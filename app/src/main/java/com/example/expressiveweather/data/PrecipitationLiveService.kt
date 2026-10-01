package com.example.expressiveweather.data

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.expressiveweather.ui.screens.SettingsDataHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 雨雲の接近 & 降り止み Live Updates 常駐フォアグラウンドサービス
 * 
 * Android 16+ Rich Ongoing Notifications および Samsung Galaxy Now Bar と結合し、
 * バックグラウンドでも現在位置の移動（GPS/Network）に完全追従してリアルタイム更新を継続します。
 * 
 * 1. 移動追従: ユーザーの移動（75m以上）を検知すると、即座に新地点の逆ジオコーディングとナウキャスト再解析を実行。
 * 2. 状態追従: 移動先で雨が止んだ場合は「雨が止みました」を通知して自動終了。
 * 3. 継続更新: 1分毎のカウントダウン更新に加え、3分毎に最新レーダー・ナウキャストを再検証。
 */
class PrecipitationLiveService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tickerJob: Job? = null
    private var locationStopCallback: (() -> Unit)? = null

    private lateinit var locationProvider: CurrentLocationProvider
    private lateinit var weatherRepository: WeatherRepository

    private var lastEvaluatedLocation: Location? = null
    private var isServiceRunning = false

    companion object {
        private const val TAG = "PrecipLiveService"

        const val ACTION_START_OR_UPDATE = "com.example.expressiveweather.action.START_OR_UPDATE_PRECIP_LIVE"
        const val ACTION_STOP = "com.example.expressiveweather.action.STOP_PRECIP_LIVE"

        /**
         * サービスを起動または更新
         */
        fun startOrUpdate(context: Context) {
            val intent = Intent(context, PrecipitationLiveService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            try {
                ContextCompat.startForegroundService(context, intent)
                Log.d(TAG, "Requested startForegroundService for Precipitation Live Updates.")
            } catch (e: Throwable) {
                Log.w(TAG, "startForegroundService failed, falling back to direct notification: ${e.message}")
                // バックグラウンド起動禁止等の例外時はマネージャーの直接通知＆インメモリタイカーへフォールバック
                val nowcast = PrecipitationLiveNotificationManager.currentActiveNowcast
                val cityName = PrecipitationLiveNotificationManager.currentActiveCityName
                if (nowcast != null && nowcast.isLiveActive) {
                    try {
                        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val notif = PrecipitationLiveNotificationManager.buildRainLiveNotification(context, nowcast, cityName)
                        nm.notify(PrecipitationLiveNotificationManager.NOTIFICATION_ID, notif)
                        PrecipitationLiveNotificationManager.startTickerIfNeeded(context)
                    } catch (ex: Throwable) {
                        Log.e(TAG, "Direct notification fallback failed: ${ex.message}")
                    }
                }
            }
        }

        /**
         * サービスを停止
         */
        fun stop(context: Context) {
            val intent = Intent(context, PrecipitationLiveService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
                Log.d(TAG, "Requested stop for PrecipitationLiveService.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send stop intent: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PrecipitationLiveService created.")
        locationProvider = CurrentLocationProvider(this)
        weatherRepository = WeatherRepository(context = this)

        // Android 14+ ForegroundServiceDidNotStartInTimeException 抑止用の安全な初期通知
        try {
            val safeNotification = PrecipitationLiveNotificationManager.buildSafeForegroundNotification(this)
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(PrecipitationLiveNotificationManager.NOTIFICATION_ID, safeNotification, serviceType)
            } else {
                startForeground(PrecipitationLiveNotificationManager.NOTIFICATION_ID, safeNotification)
            }
            isServiceRunning = true
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

        val nowcast = PrecipitationLiveNotificationManager.currentActiveNowcast
        val cityName = PrecipitationLiveNotificationManager.currentActiveCityName

        if (nowcast == null || !nowcast.isLiveActive || (nowcast.minutesRemaining ?: 0) <= 0) {
            Log.d(TAG, "Nowcast inactive or minutes <= 0. Stopping live service.")
            stopLiveService()
            return START_NOT_STICKY
        }

        // Live Notification の最新化
        try {
            val notification = PrecipitationLiveNotificationManager.buildRainLiveNotification(
                context = this,
                nowcast = nowcast,
                cityName = cityName
            )
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(PrecipitationLiveNotificationManager.NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(PrecipitationLiveNotificationManager.NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "Foreground notification updated for $cityName: ${nowcast.headline} (${nowcast.minutesRemaining}m)")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed updating startForeground notification: ${e.message}", e)
        }

        // 継続的な現在位置追従リスナーを開始
        startContinuousLocationTracking()

        // 1分毎のタイカー＆定期レーダー再評価ループを開始
        startPeriodicTickerLoop()

        return START_STICKY
    }

    /**
     * 現在位置の移動検知（75m以上）によるリアルタイム追従リスナー
     */
    private fun startContinuousLocationTracking() {
        if (locationStopCallback != null) return

        if (!locationProvider.hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted for continuous tracking.")
            return
        }

        lastEvaluatedLocation = PrecipitationLiveNotificationManager.currentActiveLocation

        locationStopCallback = locationProvider.startLocationUpdates(
            minTime = 5_000L,
            minDistance = 30f
        ) { newLocation ->
            val prev = lastEvaluatedLocation
            val distance = if (prev != null) prev.distanceTo(newLocation) else Float.MAX_VALUE

            if (distance >= 75f) {
                Log.d(TAG, "Significant movement detected: ${distance.toInt()}m from last evaluated position. Following new location...")
                lastEvaluatedLocation = newLocation
                serviceScope.launch {
                    handleLocationShift(newLocation)
                }
            }
        }
        Log.d(TAG, "Continuous location tracking registered for Live Updates.")
    }

    /**
     * 移動先新地点の逆ジオコーディングとナウキャスト即時再解析
     */
    private suspend fun handleLocationShift(newLocation: Location) {
        try {
            val newCity = locationProvider.resolveCity(newLocation)
            Log.d(TAG, "Resolved new location: ${newCity.name} (${newLocation.latitude}, ${newLocation.longitude})")

            val weatherData = weatherRepository.refresh(newCity)
            val newNowcast = weatherData.precipitationNowcast

            if (newNowcast != null && newNowcast.isLiveActive && (newNowcast.minutesRemaining ?: 0) > 0) {
                Log.d(TAG, "Nowcast active at new location: ${newNowcast.headline} (${newNowcast.minutesRemaining}m). Updating notification.")
                PrecipitationLiveNotificationManager.updateActiveNowcastFromBackground(
                    context = this,
                    nowcast = newNowcast,
                    cityName = newCity.name,
                    location = newLocation
                )
                // フォアグラウンド通知を新地点のデータで即時更新
                val updatedNotif = PrecipitationLiveNotificationManager.buildRainLiveNotification(
                    this,
                    newNowcast,
                    newCity.name
                )
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(PrecipitationLiveNotificationManager.NOTIFICATION_ID, updatedNotif)
            } else {
                Log.d(TAG, "Rain has stopped or no rain at new location ${newCity.name}. Finishing live countdown.")
                PrecipitationLiveNotificationManager.onRainStoppedAtNewLocation(this, newCity.name)
                stopLiveService()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update nowcast on location shift: ${e.message}")
        }
    }

    /**
     * 1分毎のカウントダウン更新および3分毎の最新レーダー同期ループ
     */
    private fun startPeriodicTickerLoop() {
        if (tickerJob?.isActive == true) return

        tickerJob = serviceScope.launch {
            var tickCount = 0
            while (isActive) {
                delay(60 * 1000L) // 1分間隔
                tickCount++

                val nowcast = PrecipitationLiveNotificationManager.currentActiveNowcast
                val cityName = PrecipitationLiveNotificationManager.currentActiveCityName

                if (nowcast == null || !nowcast.isLiveActive) {
                    stopLiveService()
                    break
                }

                // 3分毎に現在地GPSを再取得して最新気象レーダーを同期（雨雲の急激な発達・散逸に対応）
                if (tickCount % 3 == 0) {
                    try {
                        val freshLoc = locationProvider.getCurrentLocation(forceFresh = true)
                        if (freshLoc != null) {
                            val targetCity = locationProvider.resolveCity(freshLoc)
                            val refreshedWeather = weatherRepository.refresh(targetCity)
                            val refreshedNowcast = refreshedWeather.precipitationNowcast
                            if (refreshedNowcast != null && refreshedNowcast.isLiveActive && (refreshedNowcast.minutesRemaining ?: 0) > 0) {
                                PrecipitationLiveNotificationManager.updateActiveNowcastFromBackground(
                                    context = this@PrecipitationLiveService,
                                    nowcast = refreshedNowcast,
                                    cityName = targetCity.name,
                                    location = freshLoc
                                )
                                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                nm.notify(
                                    PrecipitationLiveNotificationManager.NOTIFICATION_ID,
                                    PrecipitationLiveNotificationManager.buildRainLiveNotification(
                                        this@PrecipitationLiveService,
                                        refreshedNowcast,
                                        targetCity.name
                                    )
                                )
                                continue
                            } else {
                                Log.d(TAG, "3-min radar re-check: Rain has ceased at ${targetCity.name}.")
                                PrecipitationLiveNotificationManager.onRainStoppedAtNewLocation(
                                    this@PrecipitationLiveService,
                                    targetCity.name
                                )
                                stopLiveService()
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Periodic 3-min radar sync error: ${e.message}")
                    }
                }

                // 通常の1分カウントダウン更新
                val targetMillis = nowcast.targetTimestampMillis
                val now = System.currentTimeMillis()
                val remainingMin = if (targetMillis != null) {
                    ((targetMillis - now) / 60000L).toInt()
                } else {
                    (nowcast.minutesRemaining ?: 1) - 1
                }

                if (remainingMin <= 0) {
                    Log.d(TAG, "Countdown completed. Posting completion notification and stopping service.")
                    showCompletionNotification(nowcast, cityName)
                    delay(5000L)
                    stopLiveService()
                    break
                } else {
                    val updatedNowcast = nowcast.copy(minutesRemaining = remainingMin)
                    PrecipitationLiveNotificationManager.updateActiveNowcastFromBackground(
                        context = this@PrecipitationLiveService,
                        nowcast = updatedNowcast,
                        cityName = cityName,
                        location = lastEvaluatedLocation
                    )
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(
                        PrecipitationLiveNotificationManager.NOTIFICATION_ID,
                        PrecipitationLiveNotificationManager.buildRainLiveNotification(
                            this@PrecipitationLiveService,
                            updatedNowcast,
                            cityName
                        )
                    )
                }
            }
        }
    }

    private fun showCompletionNotification(nowcast: PrecipitationNowcast, cityName: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val language = SettingsDataHolder.language
        val isStarting = nowcast.status == RainStatus.RAIN_APPROACHING

        val finishChip = when {
            isStarting -> when (language) {
                AppLanguage.ENGLISH -> "Rain has started"
                AppLanguage.SIMPLIFIED_CHINESE -> "开始下雨了"
                AppLanguage.TRADITIONAL_CHINESE -> "開始下雨了"
                AppLanguage.JAPANESE -> "雨が降り始めました"
            }
            else -> when (language) {
                AppLanguage.ENGLISH -> "Rain has stopped"
                AppLanguage.SIMPLIFIED_CHINESE -> "雨已经停了"
                AppLanguage.TRADITIONAL_CHINESE -> "雨已經停了"
                AppLanguage.JAPANESE -> "雨が止みました"
            }
        }
        val finishTitle = "$finishChip • $cityName"

        val finishNotif = NotificationCompat.Builder(this, PrecipitationLiveNotificationManager.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(finishTitle)
            .setContentText(finishChip)
            .setSubText(finishChip)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        nm.notify(PrecipitationLiveNotificationManager.NOTIFICATION_ID, finishNotif)
    }

    private fun stopLiveService() {
        tickerJob?.cancel()
        tickerJob = null

        locationStopCallback?.invoke()
        locationStopCallback = null

        isServiceRunning = false

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
        Log.d(TAG, "PrecipitationLiveService stopped and foreground notification removed.")
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        locationStopCallback?.invoke()
        Log.d(TAG, "PrecipitationLiveService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
