package io.github.nemynew.expressiveweather.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.github.nemynew.expressiveweather.ui.widget.WidgetRefreshWorker

/**
 * AlarmManager および位置情報変更 (PendingIntent) の内部専用ブロードキャストレシーバー (exported="false")。
 *
 * 10秒の BroadcastReceiver ANR 制限および Doze 復帰直後の通信途中キャンセルを完全に回避するため、
 * ネットワーク取得・位置解決・ウィジェット再描画は即座に WorkManager (API 31+ では Expedited) へ委譲し、
 * 次回アラームと定期 WorkManager の連鎖を確実に維持する。
 */
class WeatherUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val appContext = context.applicationContext
        Log.d(TAG, "WeatherUpdateReceiver received action: $action")

        when (action) {
            ACTION_ALARM_WEATHER_UPDATE -> {
                val isForced = intent.getBooleanExtra("force", false)
                try {
                    val manager = WeatherBackgroundUpdateManager.getInstance(appContext)
                    val interval = WeatherRepository.getWeatherUpdateInterval(appContext)
                    if (interval == WeatherUpdateInterval.OFF) {
                        manager.cancelAll()
                        return
                    }
                    // WorkManager 層が OEM 省電力等で解除されていた場合もここで自動修復
                    manager.ensureWorkManagerScheduled()
                    // 通信・警報評価・ウィジェット更新は WorkManager (API 31+ Expedited + 自動リトライ) へ委譲
                    WeatherUpdateWorker.enqueueImmediateSync(
                        context = appContext,
                        isForced = isForced,
                        source = WeatherUpdateWorker.SOURCE_ALARM
                    )
                    // 連鎖を途切れさせないよう次回 Doze 貫通アラームを即時予約
                    manager.scheduleNextAlarm()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed handling weather alarm broadcast: ${e.message}", e)
                }
            }

            ACTION_ALARM_WIDGET_UPDATE -> {
                try {
                    WidgetRefreshWorker.enqueueRefresh(appContext)
                    WidgetRefreshWorker.scheduleNextWidgetAlarm(appContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed handling widget alarm broadcast: ${e.message}", e)
                }
            }

            WeatherBackgroundUpdateManager.ACTION_LOCATION_CHANGED -> {
                val location: android.location.Location? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(android.location.LocationManager.KEY_LOCATION_CHANGED, android.location.Location::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(android.location.LocationManager.KEY_LOCATION_CHANGED)
                } ?: run {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra("locations", android.location.Location::class.java)?.lastOrNull()
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra<android.location.Location>("locations")?.lastOrNull()
                    }
                }

                if (location != null) {
                    try {
                        val interval = WeatherRepository.getWeatherUpdateInterval(appContext)
                        if (interval != WeatherUpdateInterval.OFF) {
                            WeatherUpdateWorker.enqueueLocationShiftSync(appContext, location)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed enqueueing location shift sync: ${e.message}", e)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WeatherUpdateReceiver"
        const val ACTION_ALARM_WEATHER_UPDATE = "io.github.nemynew.expressiveweather.action.ALARM_WEATHER_UPDATE"
        const val ACTION_ALARM_WIDGET_UPDATE = "io.github.nemynew.expressiveweather.action.ALARM_WIDGET_UPDATE"
    }
}
