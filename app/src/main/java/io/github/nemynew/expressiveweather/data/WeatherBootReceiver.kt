package io.github.nemynew.expressiveweather.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.nemynew.expressiveweather.ui.widget.WeatherWidgetUpdater
import io.github.nemynew.expressiveweather.ui.widget.WidgetRefreshWorker

/**
 * 端末再起動 (BOOT_COMPLETED) およびアプリアップデート (MY_PACKAGE_REPLACED) 専用レシーバー。
 * Android 15+ の制約（BOOT_COMPLETED からの直接 FGS 起動禁止）を遵守するため、
 * FGS を直接起動せず WorkManager / AlarmManager のスケジュール再構築のみ行う。
 */
class WeatherBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        Log.d(TAG, "System event $action: Rescheduling background workers & alarms.")
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        try {
            WeatherBackgroundUpdateManager.getInstance(appContext).restart()
            WeatherWidgetUpdater.updateAllFromCacheAsync(appContext)
            if (WeatherWidgetUpdater.getActiveWidgets(appContext).isNotEmpty()) {
                WidgetRefreshWorker.ensurePeriodicSchedule(appContext)
                WidgetRefreshWorker.enqueueRefresh(appContext)
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val immediateWork = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                BOOT_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateWork
            )
        } catch (e: Exception) {
            Log.e(TAG, "Boot reschedule failed: ${e.message}", e)
        } finally {
            pendingResult.finish()
        }
    }

    companion object {
        private const val TAG = "WeatherBootReceiver"
        private const val BOOT_SYNC_WORK_NAME = "io.github.nemynew.expressiveweather.work.BOOT_WEATHER_SYNC"
    }
}
