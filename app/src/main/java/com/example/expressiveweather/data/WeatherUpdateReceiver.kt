package com.example.expressiveweather.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AlarmManager および端末再起動時のブロードキャストレシーバー
 * - Dozeモード（省電力待機状態）でもAlarmManagerのWakeupアラームを受信
 * - 端末再起動（BOOT_COMPLETED）およびアプリアップデート（MY_PACKAGE_REPLACED）時に
 *   WorkManagerとAlarmManagerのスケジュールを自動復元
 */
class WeatherUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "WeatherUpdateReceiver received action: $action")

        when (action) {
            ACTION_ALARM_WEATHER_UPDATE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val manager = WeatherBackgroundUpdateManager.getInstance(context)
                        val isForced = intent.getBooleanExtra("force", false)
                        manager.performBackgroundWeatherUpdate(isForced = isForced)
                        // 次のアラームを連鎖登録
                        manager.scheduleNextAlarm()
                    } catch (e: Exception) {
                        Log.e(TAG, "Alarm weather update failed: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            WeatherBackgroundUpdateManager.ACTION_LOCATION_CHANGED -> {
                val location: android.location.Location? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(android.location.LocationManager.KEY_LOCATION_CHANGED, android.location.Location::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(android.location.LocationManager.KEY_LOCATION_CHANGED)
                } ?: run {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra("locations", android.location.Location::class.java)?.lastOrNull()
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra<android.location.Location>("locations")?.lastOrNull()
                    }
                }

                if (location != null) {
                    Log.d(TAG, "Location changed broadcast received: (${location.latitude}, ${location.longitude})")
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            WeatherBackgroundUpdateManager.getInstance(context).onBackgroundLocationChanged(location)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed handling background location change: ${e.message}", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "System event $action: Rescheduling all background weather workers & alarms.")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        WeatherBackgroundUpdateManager.getInstance(context).restart()
                        KikikuruBackgroundMonitor.getInstance(context).checkCurrentAlert(forceFresh = true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Boot reschedule failed: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WeatherUpdateReceiver"
        const val ACTION_ALARM_WEATHER_UPDATE = "com.example.expressiveweather.action.ALARM_WEATHER_UPDATE"
    }
}
