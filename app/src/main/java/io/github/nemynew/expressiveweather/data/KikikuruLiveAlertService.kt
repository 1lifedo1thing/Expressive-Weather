package io.github.nemynew.expressiveweather.data

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 警戒レベル・キキクル Live Updates 常駐フォアグラウンドサービス
 */
class KikikuruLiveAlertService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null

    companion object {
        private const val TAG = "KikikuruLiveService"

        @Volatile
        var isActivePolling: Boolean = false
            private set

        const val ACTION_START_OR_UPDATE = "io.github.nemynew.expressiveweather.action.START_OR_UPDATE_LIVE_ALERT"
        const val ACTION_STOP = "io.github.nemynew.expressiveweather.action.STOP_LIVE_ALERT"

        fun startOrUpdate(context: Context) {
            val appContext = context.applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val intent = Intent(appContext, KikikuruLiveAlertService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            try {
                ContextCompat.startForegroundService(appContext, intent)
                Log.d(TAG, "Requested startForegroundService for Live Updates.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to startForegroundService (${e.javaClass.simpleName}): ${e.message}")
                try {
                    val alertInfo = KikikuruNotificationManager.currentActiveAlertInfo
                    if (alertInfo != null && alertInfo.alertLevel.level >= 2) {
                        val notification = KikikuruNotificationManager.buildLiveNotification(
                            context = appContext,
                            alertInfo = alertInfo,
                            cityName = KikikuruNotificationManager.currentActiveCityName
                        )
                        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(KikikuruNotificationManager.NOTIFICATION_ID, notification)
                    }
                } catch (fallbackEx: Throwable) {
                    Log.w(TAG, "Notification fallback failed: ${fallbackEx.message}")
                }
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, KikikuruLiveAlertService::class.java)
            try {
                appContext.stopService(intent)
                Log.d(TAG, "Requested stopService for KikikuruLiveAlertService.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop service: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "KikikuruLiveAlertService created.")
        KikikuruNotificationManager.restoreIfNeeded(this)
        try {
            val safeNotification = KikikuruNotificationManager.buildSafeForegroundNotification(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    KikikuruNotificationManager.NOTIFICATION_ID,
                    safeNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(KikikuruNotificationManager.NOTIFICATION_ID, safeNotification)
            }
            Log.d(TAG, "Initial safe startForeground established.")
        } catch (e: Throwable) {
            Log.e(TAG, "Initial startForeground error: ${e.message}", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_OR_UPDATE
        Log.d(TAG, "onStartCommand received action: $action")

        if (action == ACTION_STOP) {
            stopLiveService()
            return START_NOT_STICKY
        }

        KikikuruNotificationManager.restoreIfNeeded(this)
        val alertInfo = KikikuruNotificationManager.currentActiveAlertInfo
        val cityName = KikikuruNotificationManager.currentActiveCityName

        if (alertInfo == null || alertInfo.alertLevel.level < 2) {
            Log.d(TAG, "Alert level < 2 or no active alert. Stopping foreground service safely.")
            stopLiveService()
            return START_NOT_STICKY
        }

        try {
            val notification = KikikuruNotificationManager.buildLiveNotification(
                context = this,
                alertInfo = alertInfo,
                cityName = cityName
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    KikikuruNotificationManager.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(KikikuruNotificationManager.NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "Foreground service updated with Live Notification (Lv.${alertInfo.alertLevel.level})")
        } catch (e: Throwable) {
            Log.e(TAG, "Error updating startForeground: ${e.message}", e)
            stopLiveService()
            return START_NOT_STICKY
        }

        startHighFrequencyMonitoring()
        return START_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "Android 15+ FGS timeout reached (startId=$startId, type=$fgsType). Detaching notification to preserve active alert.")
        // 警報発表中のタイムアウトでは通知を消去せず DETACH して継続提示する
        stopLiveService(removeNotification = false)
    }

    private fun startHighFrequencyMonitoring() {
        if (pollJob?.isActive == true) return
        isActivePolling = true
        pollJob = serviceScope.launch {
            Log.d(TAG, "LiveAlertService high-frequency loop started (3-min interval).")
            try {
                while (isActive) {
                    delay(3 * 60 * 1000L)
                    try {
                        Log.d(TAG, "LiveAlertService 3-min tick: Refreshing Kikikuru alert data...")
                        KikikuruBackgroundMonitor.getInstance(applicationContext).checkCurrentAlert(forceFresh = true)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "LiveAlertService periodic check error: ${e.message}")
                    }
                }
            } finally {
                isActivePolling = false
            }
        }
    }

    private fun stopLiveService(removeNotification: Boolean = true) {
        isActivePolling = false
        pollJob?.cancel()
        pollJob = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(removeNotification)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error in stopForeground: ${e.message}")
        }
        stopSelf()
        Log.d(TAG, "KikikuruLiveAlertService stopped (removeNotification=$removeNotification).")
    }

    override fun onDestroy() {
        super.onDestroy()
        pollJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "KikikuruLiveAlertService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
