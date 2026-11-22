package io.github.nemynew.expressiveweather.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.nemynew.expressiveweather.ui.widget.WeatherWidgetUpdater
import io.github.nemynew.expressiveweather.ui.widget.WidgetRefreshWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * AlarmManager および位置情報変更 (PendingIntent) の内部専用ブロードキャストレシーバー (exported="false")
 */
class WeatherUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val appContext = context.applicationContext
        Log.d(TAG, "WeatherUpdateReceiver received action: $action")

        when (action) {
            ACTION_ALARM_WEATHER_UPDATE -> {
                val pendingResult = goAsync()
                val isForced = intent.getBooleanExtra("force", false)
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val manager = WeatherBackgroundUpdateManager.getInstance(appContext)
                        withTimeoutOrNull(8_500L) {
                            manager.performBackgroundWeatherUpdate(isForced = isForced)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Alarm weather update failed: ${e.message}", e)
                    } finally {
                        // 例外やタイムアウト発生時も次回アラームの連鎖を途切れさせない
                        try {
                            WeatherBackgroundUpdateManager.getInstance(appContext).scheduleNextAlarm()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to schedule next alarm: ${e.message}", e)
                        }
                        pendingResult.finish()
                    }
                }
            }

            ACTION_ALARM_WIDGET_UPDATE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val completed = withTimeoutOrNull(8_500L) {
                            WeatherWidgetUpdater.refreshWidgetsFromNetwork(appContext)
                            true
                        }
                        if (completed == null) {
                            WidgetRefreshWorker.enqueueRefresh(appContext)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Alarm widget update failed: ${e.message}", e)
                        WidgetRefreshWorker.enqueueRefresh(appContext)
                    } finally {
                        WidgetRefreshWorker.scheduleNextWidgetAlarm(appContext)
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
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        try {
                            withTimeoutOrNull(8_500L) {
                                WeatherBackgroundUpdateManager.getInstance(appContext).onBackgroundLocationChanged(location)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed handling background location change: ${e.message}", e)
                        } finally {
                            pendingResult.finish()
                        }
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
