package io.github.nemynew.expressiveweather.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.nemynew.expressiveweather.BuildConfig
import io.github.nemynew.expressiveweather.data.BackgroundSchedulePolicy
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.WeatherUpdateReceiver
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * ウィジェットの即時ネットワーク更新・再描画を行う WorkManager ワーカー
 */
class WidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val targetWidgetId = inputData.getInt(KEY_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        var succeeded = false
        return try {
            val specificId = if (targetWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) targetWidgetId else null
            WeatherWidgetUpdater.refreshWidgetsFromNetwork(applicationContext, specificWidgetId = specificId)
            succeeded = true
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "WidgetRefreshWorker failed: ${e.message}", e)
            WeatherWidgetUpdater.updateAllFromCache(applicationContext)
            if (runAttemptCount < BackgroundSchedulePolicy.WIDGET_WORKER_MAX_ATTEMPTS) Result.retry() else Result.failure()
        } finally {
            if (WeatherWidgetUpdater.getActiveWidgets(applicationContext).isNotEmpty()) {
                if (succeeded) {
                    scheduleNextWidgetAlarm(applicationContext)
                } else {
                    ensureNextWidgetAlarmScheduled(applicationContext)
                }
            } else {
                cancelWidgetAlarm(applicationContext)
            }
        }
    }

    companion object {
        private const val TAG = "WidgetRefreshWorker"
        private const val KEY_APP_WIDGET_ID = "key_app_widget_id"
        private const val UNIQUE_WORK_PREFIX = "io.github.nemynew.expressiveweather.widget.REFRESH_"
        private const val PERIODIC_WIDGET_WORK_NAME = "io.github.nemynew.expressiveweather.widget.PERIODIC_REFRESH"
        private const val REQUEST_CODE_WIDGET_ALARM = 4003
        const val WIDGET_REFRESH_INTERVAL_MINUTES = BackgroundSchedulePolicy.GITHUB_WIDGET_ALARM_INTERVAL_MINUTES

        fun ensurePeriodicSchedule(context: Context) {
            val appCtx = context.applicationContext
            if (WeatherWidgetUpdater.getActiveWidgets(appCtx).isEmpty()) {
                cancelPeriodicSchedule(appCtx)
                return
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            // WorkManager 最短 15 分周期 (flex 5 分) を永続バックアップ層として維持
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                15L, TimeUnit.MINUTES,
                5L, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BackgroundSchedulePolicy.WORKER_BACKOFF_INITIAL_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()
            WorkManager.getInstance(appCtx).enqueueUniquePeriodicWork(
                PERIODIC_WIDGET_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            syncWidgetAlarmWithWeatherSchedule(appCtx)
        }

        /**
         * 天気定期更新の頻度と連動し、Play 版では 15 分天気アラームと統合（重複起床ゼロ）、
         * 天気更新が 30 分以上/OFF の場合のみウィジェット専用 15 分アラームを維持する。
         */
        fun syncWidgetAlarmWithWeatherSchedule(context: Context) {
            val appCtx = context.applicationContext
            val hasWidgets = WeatherWidgetUpdater.getActiveWidgets(appCtx).isNotEmpty()
            val weatherInterval = WeatherRepository.getWeatherUpdateInterval(appCtx)
            val shouldRunDedicatedAlarm = BackgroundSchedulePolicy.shouldScheduleDedicatedWidgetAlarm(
                isPlayDistribution = BuildConfig.IS_PLAY_DISTRIBUTION,
                weatherInterval = weatherInterval,
                hasActiveWidgets = hasWidgets
            )
            if (shouldRunDedicatedAlarm) {
                ensureNextWidgetAlarmScheduled(appCtx)
            } else {
                cancelWidgetAlarm(appCtx)
            }
        }

        private fun createWidgetAlarmPendingIntent(context: Context, flags: Int): PendingIntent? {
            val intent = Intent(context, WeatherUpdateReceiver::class.java).apply {
                action = WeatherUpdateReceiver.ACTION_ALARM_WIDGET_UPDATE
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_WIDGET_ALARM,
                intent,
                flags or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun ensureNextWidgetAlarmScheduled(context: Context) {
            val appCtx = context.applicationContext
            if (createWidgetAlarmPendingIntent(appCtx, PendingIntent.FLAG_NO_CREATE) == null) {
                scheduleNextWidgetAlarm(appCtx)
            }
        }

        fun scheduleNextWidgetAlarm(context: Context) {
            val appCtx = context.applicationContext
            val hasWidgets = WeatherWidgetUpdater.getActiveWidgets(appCtx).isNotEmpty()
            val weatherInterval = WeatherRepository.getWeatherUpdateInterval(appCtx)
            if (!BackgroundSchedulePolicy.shouldScheduleDedicatedWidgetAlarm(
                    isPlayDistribution = BuildConfig.IS_PLAY_DISTRIBUTION,
                    weatherInterval = weatherInterval,
                    hasActiveWidgets = hasWidgets
                )
            ) {
                cancelWidgetAlarm(appCtx)
                return
            }
            try {
                val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val pendingIntent = createWidgetAlarmPendingIntent(appCtx, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
                val intervalMinutes = BackgroundSchedulePolicy.resolveWidgetAlarmIntervalMinutes(BuildConfig.IS_PLAY_DISTRIBUTION)
                val delayMillis = BackgroundSchedulePolicy.computeAlarmDelayMillis(
                    requestedDelayMillis = intervalMinutes * 60_000L,
                    isPlayDistribution = BuildConfig.IS_PLAY_DISTRIBUTION
                )
                val triggerAtMillis = System.currentTimeMillis() + delayMillis
                AlarmManagerCompat.setAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled widget wakeup alarm in ${delayMillis / 1000}s (play=${BuildConfig.IS_PLAY_DISTRIBUTION})")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule widget alarm: ${e.message}")
            }
        }

        fun cancelWidgetAlarm(context: Context) {
            try {
                val appCtx = context.applicationContext
                val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val pendingIntent = createWidgetAlarmPendingIntent(appCtx, PendingIntent.FLAG_NO_CREATE)
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling widget alarm: ${e.message}")
            }
        }

        fun cancelPeriodicSchedule(context: Context) {
            val appCtx = context.applicationContext
            WorkManager.getInstance(appCtx)
                .cancelUniqueWork(PERIODIC_WIDGET_WORK_NAME)
            cancelWidgetAlarm(appCtx)
        }

        fun enqueueRefresh(context: Context, appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID) {
            val appCtx = context.applicationContext
            val input = Data.Builder()
                .putInt(KEY_APP_WIDGET_ID, appWidgetId)
                .build()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val builder = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInputData(input)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BackgroundSchedulePolicy.WORKER_BACKOFF_INITIAL_SECONDS,
                    TimeUnit.SECONDS
                )
            if (BackgroundSchedulePolicy.shouldUseExpeditedWork()) {
                builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
            val workName = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                "$UNIQUE_WORK_PREFIX$appWidgetId"
            } else {
                "${UNIQUE_WORK_PREFIX}ALL"
            }
            WorkManager.getInstance(appCtx).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                builder.build()
            )
        }
    }
}
