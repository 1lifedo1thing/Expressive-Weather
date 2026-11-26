package io.github.nemynew.expressiveweather.data

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * WorkManager を使用した天気・防災警報・ウィジェットの永続的バックグラウンド更新ワーカー。
 * - 定期実行 (PeriodicWorkRequest) とアラーム/再起動/現在地移動からの即時実行 (OneTimeWorkRequest) の両方を担当
 * - Android 12+ (API 31+) の即時ジョブは Expedited (OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) で
 *   FGS や通知権限を追加せずに App Standby 遅延を回避
 * - ネットワーク接続待ち (NetworkType.CONNECTED) と 30秒起点の指数バックオフで一時的な通信断から迅速に自動回復
 */
class WeatherUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val triggerSource = inputData.getString(KEY_TRIGGER_SOURCE) ?: SOURCE_PERIODIC
        val isForced = inputData.getBoolean(KEY_IS_FORCED, false)
        val hasLocationPayload = inputData.getBoolean(KEY_HAS_LOCATION, false)
        Log.d(
            TAG,
            "WeatherUpdateWorker started (source=$triggerSource, forced=$isForced, hasLoc=$hasLocationPayload, attempt=$runAttemptCount)."
        )

        val updateManager = WeatherBackgroundUpdateManager.getInstance(applicationContext)
        var succeeded = false
        return try {
            val success = if (hasLocationPayload) {
                val lat = inputData.getDouble(KEY_LOCATION_LAT, Double.NaN)
                val lon = inputData.getDouble(KEY_LOCATION_LON, Double.NaN)
                if (lat.isFinite() && lon.isFinite()) {
                    val loc = Location("bg_shift").apply {
                        latitude = lat
                        longitude = lon
                        val acc = inputData.getFloat(KEY_LOCATION_ACCURACY, -1f)
                        if (acc >= 0f) accuracy = acc
                        val timeMs = inputData.getLong(KEY_LOCATION_TIME, 0L)
                        if (timeMs > 0L) time = timeMs
                    }
                    updateManager.onBackgroundLocationChanged(loc)
                } else {
                    updateManager.performBackgroundWeatherUpdate(isForced = isForced)
                }
            } else {
                updateManager.performBackgroundWeatherUpdate(isForced = isForced)
            }

            succeeded = success
            if (success) {
                Log.d(TAG, "WeatherUpdateWorker completed successfully (source=$triggerSource).")
                Result.success()
            } else {
                Log.w(TAG, "WeatherUpdateWorker did not complete update (attempt=$runAttemptCount), scheduling retry.")
                if (runAttemptCount < BackgroundSchedulePolicy.WEATHER_WORKER_MAX_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "WeatherUpdateWorker error during execution: ${e.message}", e)
            if (runAttemptCount < BackgroundSchedulePolicy.WEATHER_WORKER_MAX_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        } finally {
            // WorkManager と AlarmManager の相互自己修復:
            // 成功時は次回アラームを「現在 + 設定間隔」に再アンカーし重複起床を防止しつつ連鎖を維持、
            // 失敗時は既存アラームが欠損している場合のみ補完する。
            try {
                val interval = WeatherRepository.getWeatherUpdateInterval(applicationContext)
                if (interval != WeatherUpdateInterval.OFF) {
                    if (succeeded && !hasLocationPayload) {
                        updateManager.scheduleNextAlarm()
                    } else {
                        updateManager.ensureAlarmScheduled()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Post-work alarm maintenance failed: ${e.message}")
            }
        }
    }

    companion object {
        const val TAG = "WeatherUpdateWorker"
        const val UNIQUE_WORK_NAME = "io.github.nemynew.expressiveweather.work.WEATHER_UPDATE_PERIODIC"
        const val IMMEDIATE_SYNC_WORK_NAME = "io.github.nemynew.expressiveweather.work.WEATHER_UPDATE_IMMEDIATE"
        const val LOCATION_SYNC_WORK_NAME = "io.github.nemynew.expressiveweather.work.WEATHER_UPDATE_LOCATION_SHIFT"

        private const val KEY_IS_FORCED = "key_is_forced"
        private const val KEY_TRIGGER_SOURCE = "key_trigger_source"
        private const val KEY_HAS_LOCATION = "key_has_location"
        private const val KEY_LOCATION_LAT = "key_location_lat"
        private const val KEY_LOCATION_LON = "key_location_lon"
        private const val KEY_LOCATION_ACCURACY = "key_location_accuracy"
        private const val KEY_LOCATION_TIME = "key_location_time"

        const val SOURCE_PERIODIC = "periodic"
        const val SOURCE_ALARM = "alarm"
        const val SOURCE_BOOT = "boot"
        const val SOURCE_LOCATION = "location_shift"

        /**
         * アラーム起床・端末再起動時などに即時同期ワーカーを投入する。
         * BroadcastReceiver の 10 秒 ANR 制限や Doze 復帰直後の通信不安定を回避し確実に完走させる。
         */
        fun enqueueImmediateSync(
            context: Context,
            isForced: Boolean = false,
            source: String = SOURCE_ALARM
        ) {
            val appCtx = context.applicationContext
            val input = Data.Builder()
                .putBoolean(KEY_IS_FORCED, isForced)
                .putString(KEY_TRIGGER_SOURCE, source)
                .build()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val builder = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
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
            WorkManager.getInstance(appCtx).enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                builder.build()
            )
            Log.d(TAG, "Enqueued immediate WeatherUpdateWorker (source=$source, forced=$isForced, expedited=${BackgroundSchedulePolicy.shouldUseExpeditedWork()}).")
        }

        /**
         * バックグラウンド位置移動検知時の追従更新ワーカーを投入する。
         * 移動中に圏外（地下鉄・トンネル等）であっても回線復帰後に自動で新地点の天気・警報を取得する。
         */
        fun enqueueLocationShiftSync(context: Context, location: Location) {
            val appCtx = context.applicationContext
            val input = Data.Builder()
                .putBoolean(KEY_HAS_LOCATION, true)
                .putString(KEY_TRIGGER_SOURCE, SOURCE_LOCATION)
                .putDouble(KEY_LOCATION_LAT, location.latitude)
                .putDouble(KEY_LOCATION_LON, location.longitude)
                .putFloat(KEY_LOCATION_ACCURACY, location.accuracy)
                .putLong(KEY_LOCATION_TIME, location.time)
                .build()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val builder = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
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
            WorkManager.getInstance(appCtx).enqueueUniqueWork(
                LOCATION_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                builder.build()
            )
            Log.d(TAG, "Enqueued location-shift WeatherUpdateWorker (expedited=${BackgroundSchedulePolicy.shouldUseExpeditedWork()}).")
        }
    }
}
