package io.github.nemynew.expressiveweather.data

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.nemynew.expressiveweather.util.LocaleUtils
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
 * 雨雲接近・降り止みリアルタイムカウントダウン Live Updates 常駐フォアグラウンドサービス
 */
class PrecipitationLiveService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tickerJob: Job? = null
    private var locationShiftJob: Job? = null
    private var locationStopCallback: (() -> Unit)? = null

    private lateinit var locationProvider: CurrentLocationProvider
    private lateinit var weatherRepository: WeatherRepository

    private var lastEvaluatedLocation: Location? = null

    companion object {
        private const val TAG = "PrecipitationLiveSvc"

        const val ACTION_START_OR_UPDATE = "io.github.nemynew.expressiveweather.action.START_OR_UPDATE_RAIN_LIVE"
        const val ACTION_STOP = "io.github.nemynew.expressiveweather.action.STOP_RAIN_LIVE"

        @Volatile
        var isServiceRunning = false
            private set

        fun startOrUpdate(context: Context) {
            val appContext = context.applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val intent = Intent(appContext, PrecipitationLiveService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            try {
                ContextCompat.startForegroundService(appContext, intent)
                Log.d(TAG, "Requested startForegroundService for Precipitation Live Updates.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to startForegroundService (${e.javaClass.simpleName}): ${e.message}")
                val nowcast = PrecipitationLiveNotificationManager.currentActiveNowcast
                if (nowcast != null && nowcast.isLiveActive) {
                    val notification = PrecipitationLiveNotificationManager.buildRainLiveNotification(
                        context = appContext,
                        nowcast = nowcast,
                        cityName = PrecipitationLiveNotificationManager.currentActiveCityName
                    )
                    val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(PrecipitationLiveNotificationManager.NOTIFICATION_ID, notification)
                    PrecipitationLiveNotificationManager.startTickerIfNeeded(appContext)
                }
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, PrecipitationLiveService::class.java)
            try {
                appContext.stopService(intent)
                Log.d(TAG, "Requested stopService for PrecipitationLiveService.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop service: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PrecipitationLiveService created.")
        locationProvider = CurrentLocationProvider(applicationContext)
        weatherRepository = WeatherRepository(context = applicationContext)
        PrecipitationLiveNotificationManager.restoreIfNeeded(this)

        val safeNotification = PrecipitationLiveNotificationManager.buildSafeForegroundNotification(this)
        if (!promoteForegroundSafely(safeNotification)) {
            stopSelf()
            return
        }
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_OR_UPDATE
        Log.d(TAG, "onStartCommand received action: $action")

        if (action == ACTION_STOP) {
            stopLiveService()
            return START_NOT_STICKY
        }

        PrecipitationLiveNotificationManager.restoreIfNeeded(this)
        val nowcast = PrecipitationLiveNotificationManager.currentActiveNowcast
        val cityName = PrecipitationLiveNotificationManager.currentActiveCityName

        if (nowcast == null || !nowcast.isLiveActive || (nowcast.minutesRemaining != null && nowcast.minutesRemaining <= 0)) {
            Log.d(TAG, "Nowcast inactive or minutes <= 0. Stopping live service.")
            stopLiveService()
            return START_NOT_STICKY
        }

        val notification = PrecipitationLiveNotificationManager.buildRainLiveNotification(
            context = this,
            nowcast = nowcast,
            cityName = cityName
        )
        if (!promoteForegroundSafely(notification)) {
            stopLiveService()
            return START_NOT_STICKY
        }

        startContinuousLocationTracking()
        startPeriodicTickerLoop()
        return START_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "Android 15+ FGS timeout reached (startId=$startId, type=$fgsType). Stopping service cleanly.")
        stopLiveService()
    }

    private fun promoteForegroundSafely(notification: Notification): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return try {
                startForeground(PrecipitationLiveNotificationManager.NOTIFICATION_ID, notification)
                true
            } catch (e: Throwable) {
                Log.e(TAG, "startForeground failed: ${e.message}", e)
                false
            }
        }

        val canUseLocationType = locationProvider.hasLocationPermission()
        val preferredType = if (canUseLocationType) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }

        return try {
            startForeground(PrecipitationLiveNotificationManager.NOTIFICATION_ID, notification, preferredType)
            true
        } catch (secEx: SecurityException) {
            Log.w(TAG, "Location FGS type denied (${secEx.message}); retrying with DATA_SYNC only.")
            try {
                startForeground(
                    PrecipitationLiveNotificationManager.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
                true
            } catch (fallbackEx: Throwable) {
                Log.e(TAG, "DATA_SYNC startForeground failed: ${fallbackEx.message}", fallbackEx)
                false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "startForeground failed: ${e.message}", e)
            false
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

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
                lastEvaluatedLocation = newLocation
                locationShiftJob?.cancel()
                locationShiftJob = serviceScope.launch {
                    handleLocationShift(newLocation)
                }
            }
        }
    }

    private suspend fun handleLocationShift(newLocation: Location) {
        try {
            val newCity = locationProvider.resolveCity(newLocation)
            val weatherData = weatherRepository.refresh(newCity)
            val newNowcast = weatherData.precipitationNowcast

            if (newNowcast != null && newNowcast.isLiveActive && (newNowcast.minutesRemaining == null || newNowcast.minutesRemaining > 0)) {
                PrecipitationLiveNotificationManager.updateActiveNowcastFromBackground(
                    context = this,
                    nowcast = newNowcast,
                    cityName = newCity.name,
                    location = newLocation
                )
                if (canPostNotifications()) {
                    val updatedNotif = PrecipitationLiveNotificationManager.buildRainLiveNotification(
                        this,
                        newNowcast,
                        newCity.name
                    )
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(PrecipitationLiveNotificationManager.NOTIFICATION_ID, updatedNotif)
                }
            } else {
                PrecipitationLiveNotificationManager.onNowcastCleared(this, newCity.name)
                stopLiveService(removeNotification = false)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update nowcast on location shift: ${e.message}")
        }
    }

    private fun startPeriodicTickerLoop() {
        if (tickerJob?.isActive == true) return

        tickerJob = serviceScope.launch {
            var tickCount = 0
            while (isActive) {
                delay(60 * 1000L)
                tickCount++

                if (!WeatherRepository.isLiveAlertNotificationEnabled(this@PrecipitationLiveService)) {
                    PrecipitationLiveNotificationManager.cancel(this@PrecipitationLiveService)
                    stopLiveService()
                    break
                }

                val nowcast = PrecipitationLiveNotificationManager.currentActiveNowcast
                val cityName = PrecipitationLiveNotificationManager.currentActiveCityName

                if (nowcast == null || !nowcast.isLiveActive) {
                    stopLiveService()
                    break
                }

                if (tickCount % 3 == 0 && !PrecipitationLiveNotificationManager.isTestActive) {
                    try {
                        val freshLoc = locationProvider.getCurrentLocation(forceFresh = true)
                        if (freshLoc != null) {
                            val targetCity = locationProvider.resolveCity(freshLoc)
                            val refreshedWeather = weatherRepository.refresh(targetCity)
                            val refreshedNowcast = refreshedWeather.precipitationNowcast
                            if (refreshedNowcast != null && refreshedNowcast.isLiveActive && (refreshedNowcast.minutesRemaining == null || refreshedNowcast.minutesRemaining > 0)) {
                                PrecipitationLiveNotificationManager.updateActiveNowcastFromBackground(
                                    context = this@PrecipitationLiveService,
                                    nowcast = refreshedNowcast,
                                    cityName = targetCity.name,
                                    location = freshLoc
                                )
                                if (canPostNotifications()) {
                                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    nm.notify(
                                        PrecipitationLiveNotificationManager.NOTIFICATION_ID,
                                        PrecipitationLiveNotificationManager.buildRainLiveNotification(
                                            this@PrecipitationLiveService,
                                            refreshedNowcast,
                                            targetCity.name
                                        )
                                    )
                                }
                                continue
                            } else {
                                PrecipitationLiveNotificationManager.onNowcastCleared(
                                    this@PrecipitationLiveService,
                                    targetCity.name
                                )
                                stopLiveService(removeNotification = false)
                                break
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Periodic 3-min radar sync error: ${e.message}")
                    }
                }

                val targetMillis = nowcast.targetTimestampMillis
                val remainingMin: Int? = when {
                    targetMillis != null -> ((targetMillis - System.currentTimeMillis()) / 60000L).toInt()
                    nowcast.minutesRemaining != null -> nowcast.minutesRemaining - 1
                    else -> null
                }

                if (remainingMin != null && remainingMin <= 0) {
                    val isApproaching = nowcast.status == RainStatus.RAIN_APPROACHING
                    PrecipitationLiveNotificationManager.completeLiveCountdown(
                        context = this@PrecipitationLiveService,
                        isApproaching = isApproaching,
                        cityName = cityName
                    )
                    delay(5000L)
                    stopLiveService(removeNotification = false)
                    break
                } else {
                    val updatedNowcast = nowcast.copy(minutesRemaining = remainingMin)
                    PrecipitationLiveNotificationManager.updateActiveNowcastFromBackground(
                        context = this@PrecipitationLiveService,
                        nowcast = updatedNowcast,
                        cityName = cityName,
                        location = lastEvaluatedLocation
                    )
                    if (canPostNotifications()) {
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
    }

    private fun stopLiveService(removeNotification: Boolean = true) {
        tickerJob?.cancel()
        tickerJob = null
        locationShiftJob?.cancel()
        locationShiftJob = null

        locationStopCallback?.invoke()
        locationStopCallback = null

        isServiceRunning = false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(removeNotification)
            }
            if (removeNotification) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(PrecipitationLiveNotificationManager.NOTIFICATION_ID)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error in stopForeground: ${e.message}")
        }
        stopSelf()
        Log.d(TAG, "PrecipitationLiveService stopped (removeNotification=$removeNotification).")
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        locationShiftJob?.cancel()
        locationStopCallback?.invoke()
        locationStopCallback = null
        serviceScope.cancel()
        isServiceRunning = false
        Log.d(TAG, "PrecipitationLiveService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
