package com.example.expressiveweather.data

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 警戒レベル・キキクル Live Updates 常駐フォアグラウンドサービス
 * 
 * Android 16+ Rich Ongoing Notifications および Galaxy Now Bar と結合し、
 * スワイプで消去されない「進行中アクティビティ（Live Notification）」を提供します。
 * キキクル警戒中は3分ごとの高頻度自動ポーリングを実施します。
 */
class KikikuruLiveAlertService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null

    companion object {
        private const val TAG = "KikikuruLiveService"

        const val ACTION_START_OR_UPDATE = "com.example.expressiveweather.action.START_OR_UPDATE_LIVE_ALERT"
        const val ACTION_STOP = "com.example.expressiveweather.action.STOP_LIVE_ALERT"

        /**
         * サービスを起動して Live Notification を表示/更新
         */
        fun startOrUpdate(context: Context) {
            val intent = Intent(context, KikikuruLiveAlertService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            try {
                ContextCompat.startForegroundService(context, intent)
                Log.d(TAG, "Requested startForegroundService for Live Updates.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to startForegroundService: ${e.message}", e)
                // フォアグラウンド起動禁止例外やバックグラウンド制限時は直接通知マネージャーへフォールバック
                try {
                    val alertInfo = KikikuruNotificationManager.currentActiveAlertInfo
                    if (alertInfo != null && alertInfo.alertLevel.level >= 2) {
                        val notification = KikikuruNotificationManager.buildLiveNotification(
                            context = context,
                            alertInfo = alertInfo,
                            cityName = KikikuruNotificationManager.currentActiveCityName
                        )
                        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        nm.notify(KikikuruNotificationManager.NOTIFICATION_ID, notification)
                    }
                } catch (fallbackEx: Throwable) {
                    Log.w(TAG, "Notification fallback failed: ${fallbackEx.message}")
                }
            }
        }

        /**
         * サービスを停止し通知を破棄
         */
        fun stop(context: Context) {
            val intent = Intent(context, KikikuruLiveAlertService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
                Log.d(TAG, "Requested stop for KikikuruLiveAlertService.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send stop intent: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "KikikuruLiveAlertService created.")
        // Android 14+ / Samsung One UI 規約遵守:
        // startForegroundService 呼び出し後は数秒以内に startForeground を実行しないと
        // ForegroundServiceDidNotStartInTimeException でクラッシュするため、生成時に即時登録する
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
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_OR_UPDATE
        Log.d(TAG, "onStartCommand received action: $action")

        if (action == ACTION_STOP) {
            stopLiveService()
            return START_NOT_STICKY
        }

        val alertInfo = KikikuruNotificationManager.currentActiveAlertInfo
        val cityName = KikikuruNotificationManager.currentActiveCityName

        if (alertInfo == null || alertInfo.alertLevel.level < 2) {
            Log.d(TAG, "Alert level < 2 or no active alert. Stopping foreground service safely.")
            stopLiveService()
            return START_NOT_STICKY
        }

        // Live Notification を生成し、フォアグラウンド通知を最新化
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
            Log.d(TAG, "Foreground service successfully updated with Live Notification (Lv.${alertInfo.alertLevel.level})")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating startForeground: ${e.message}", e)
        }

        // サービス稼働中は3分ごとの高頻度バックグラウンド取得を開始
        startHighFrequencyMonitoring()

        return START_STICKY
    }

    private fun startHighFrequencyMonitoring() {
        if (pollJob?.isActive == true) return
        pollJob = serviceScope.launch {
            Log.d(TAG, "LiveAlertService high-frequency loop started (3-min interval).")
            while (isActive) {
                delay(3 * 60 * 1000L)
                try {
                    Log.d(TAG, "LiveAlertService 3-min tick: Refreshing Kikikuru alert data...")
                    KikikuruBackgroundMonitor.getInstance(this@KikikuruLiveAlertService).checkCurrentAlert(forceFresh = true)
                } catch (e: Exception) {
                    Log.w(TAG, "LiveAlertService periodic check error: ${e.message}")
                }
            }
        }
    }

    private fun stopLiveService() {
        pollJob?.cancel()
        pollJob = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error in stopForeground: ${e.message}")
        }
        stopSelf()
        Log.d(TAG, "KikikuruLiveAlertService stopped and foreground notification removed.")
    }

    override fun onDestroy() {
        super.onDestroy()
        pollJob?.cancel()
        Log.d(TAG, "KikikuruLiveAlertService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
