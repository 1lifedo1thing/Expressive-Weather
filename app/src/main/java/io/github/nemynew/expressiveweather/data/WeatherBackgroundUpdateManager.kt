package io.github.nemynew.expressiveweather.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.nemynew.expressiveweather.BuildConfig
import io.github.nemynew.expressiveweather.data.local.LocationKeyNormalizer
import io.github.nemynew.expressiveweather.ui.widget.WeatherWidgetUpdater
import io.github.nemynew.expressiveweather.ui.widget.WidgetPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 通常天気の高信頼・多重層バックグラウンド自動更新マネージャー
 * 
 * 確実なバックグラウンド更新を保証するための3層ハイブリッド設計:
 * 1. 【WorkManager層】: Android標準の永続化ワーカー。アプリプロセス終了時や端末再起動後もOSが自動実行。
 * 2. 【AlarmManager層】: Dozeモード（省電力ディープスリープ）を突破するWakeupアラーム。設定された頻度を正確に維持。
 * 3. 【フォアグラウンド/インメモリ層】: アプリ起動中やフォアグラウンド復帰時の即時更新・高応答性ループ。
 */
class WeatherBackgroundUpdateManager private constructor(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var periodicJob: Job? = null
    private val updateMutex = kotlinx.coroutines.sync.Mutex()
    private val isScheduled = java.util.concurrent.atomic.AtomicBoolean(false)
    private val locationProvider = CurrentLocationProvider(context)
    private val weatherRepository = WeatherRepository(context = context)

    companion object {
        private const val TAG = "WeatherBgUpdateMgr"
        private const val PREFS_NAME = "expressive_weather_prefs"
        private const val KEY_LAST_UPDATE_TIME = "last_bg_weather_update_time_ms"
        private const val KEY_LAST_UPDATE_CITY = "last_bg_weather_update_city"
        private const val KEY_LAST_UPDATE_LAT = "last_bg_weather_update_lat"
        private const val KEY_LAST_UPDATE_LON = "last_bg_weather_update_lon"
        private const val KEY_LAST_UPDATE_STATUS = "last_bg_weather_update_status"
        private const val REQUEST_CODE_ALARM = 4001
        private const val REQUEST_CODE_LOCATION_PENDING = 4002

        const val ACTION_LOCATION_CHANGED = "io.github.nemynew.expressiveweather.action.LOCATION_CHANGED"

        @Volatile
        private var INSTANCE: WeatherBackgroundUpdateManager? = null

        fun getInstance(context: Context): WeatherBackgroundUpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeatherBackgroundUpdateManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun init(context: Context): WeatherBackgroundUpdateManager {
            return getInstance(context).apply {
                if (isScheduled.compareAndSet(false, true)) {
                    scheduleAll()
                }
            }
        }
    }

    private fun getLocationPendingIntent(): PendingIntent {
        val intent = Intent(context, WeatherUpdateReceiver::class.java).apply {
            action = ACTION_LOCATION_CHANGED
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(context, REQUEST_CODE_LOCATION_PENDING, intent, flags)
    }

    private fun registerBackgroundLocationUpdates() {
        try {
            locationProvider.registerBackgroundLocationUpdates(getLocationPendingIntent())
            Log.d(TAG, "Registered background location change listener via PendingIntent.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register background location updates: ${e.message}")
        }
    }

    private fun unregisterBackgroundLocationUpdates() {
        try {
            val intent = Intent(context, WeatherUpdateReceiver::class.java).apply {
                action = ACTION_LOCATION_CHANGED
            }
            val flags = PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val existingPendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_LOCATION_PENDING, intent, flags)
            if (existingPendingIntent != null) {
                locationProvider.unregisterBackgroundLocationUpdates(existingPendingIntent)
                existingPendingIntent.cancel()
            }
            Log.d(TAG, "Unregistered background location change listener.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister background location updates: ${e.message}")
        }
    }

    /**
     * すべてのスケジュール機構（WorkManager, AlarmManager, バックグラウンド位置変更リスナー, In-Memoryループ）を一括設定
     */
    fun scheduleAll() {
        val interval = weatherRepository.getWeatherUpdateInterval()
        Log.d(TAG, "Initializing background update schedule for interval: ${interval.name} (${interval.minutes} min)")

        if (interval == WeatherUpdateInterval.OFF) {
            cancelAll()
            return
        }

        scheduleWorkManager(interval)
        scheduleNextAlarm()
        registerBackgroundLocationUpdates()
        startPeriodicUpdate()
    }

    /**
     * 設定変更時や即時更新が必要な際にすべてのスケジューラーを再設定
     */
    fun restart() {
        periodicJob?.cancel()
        periodicJob = null
        scheduleAll()
    }

    /**
     * すべての自動更新スケジュールをキャンセル
     */
    fun cancelAll() {
        isScheduled.set(false)
        Log.d(TAG, "Cancelling all weather background updates (Setting: OFF).")
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WeatherUpdateWorker.UNIQUE_WORK_NAME)
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling WorkManager work: ${e.message}")
        }
        cancelAlarm()
        unregisterBackgroundLocationUpdates()
        periodicJob?.cancel()
        periodicJob = null
    }

    /**
     * Layer 1: WorkManager の定期ジョブをスケジュール
     */
    private fun scheduleWorkManager(interval: WeatherUpdateInterval) {
        try {
            val intervalMinutes = interval.minutes.toLong()
            val flexMinutes = (intervalMinutes / 3).coerceAtLeast(5L)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(
                intervalMinutes, TimeUnit.MINUTES,
                flexMinutes, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WeatherUpdateWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d(TAG, "Enqueued WorkManager PeriodicWorkRequest: every ${interval.minutes}m (flex: ${flexMinutes}m)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue WorkManager periodic request: ${e.message}", e)
        }
    }

    /**
     * Layer 2: AlarmManager の Doze貫通アラームをスケジュール
     */
    fun scheduleNextAlarm(customDelayMillis: Long? = null) {
        val interval = weatherRepository.getWeatherUpdateInterval()
        if (interval == WeatherUpdateInterval.OFF) {
            cancelAlarm()
            return
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WeatherUpdateReceiver::class.java).apply {
                action = WeatherUpdateReceiver.ACTION_ALARM_WEATHER_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val delayMillis = customDelayMillis ?: (interval.minutes * 60 * 1000L)
            val triggerAtMillis = System.currentTimeMillis() + delayMillis

            AlarmManagerCompat.setAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled next AlarmManager wakeup in ${delayMillis / 1000}s (at $triggerAtMillis)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule AlarmManager: ${e.message}", e)
        }
    }

    private fun cancelAlarm() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WeatherUpdateReceiver::class.java).apply {
                action = WeatherUpdateReceiver.ACTION_ALARM_WEATHER_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ALARM,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled AlarmManager wakeup alarm.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling alarm: ${e.message}")
        }
    }

    /**
     * Layer 3: アプリプロセス生存中のインメモリループ
     */
    private fun startPeriodicUpdate() {
        if (periodicJob?.isActive == true) return

        periodicJob = scope.launch {
            Log.d(TAG, "Started in-memory weather periodic update loop.")
            while (isActive) {
                val interval = weatherRepository.getWeatherUpdateInterval()
                if (interval == WeatherUpdateInterval.OFF) {
                    delay(60_000L)
                    continue
                }

                val intervalMillis = interval.minutes * 60 * 1000L
                delay(intervalMillis)

                if (!isActive) break

                try {
                    Log.d(TAG, "Executing in-memory periodic update tick...")
                    performBackgroundWeatherUpdate(isForced = false)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "In-memory weather update error: ${e.message}")
                }
            }
        }
    }

    /**
     * アプリ起動時またはフォアグラウンド復帰時の通信トリガー
     */
    fun onAppForegrounded() {
        scope.launch {
            val interval = weatherRepository.getWeatherUpdateInterval()
            if (interval == WeatherUpdateInterval.OFF) return@launch

            val now = System.currentTimeMillis()
            val last = getLastUpdateTime()
            val staleThreshold = (interval.minutes * 60 * 1000L * 0.80).toLong().coerceAtLeast(2 * 60 * 1000L)

            if (now - last >= staleThreshold) {
                Log.d(TAG, "App foregrounded: Cache is stale (${(now - last) / 1000}s old). Triggering immediate background update.")
                try {
                    performBackgroundWeatherUpdate(isForced = true)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Immediate update on foreground failed: ${e.message}")
                }
            } else {
                Log.d(TAG, "App foregrounded: Cache is fresh (${(now - last) / 1000}s old). No immediate update needed.")
            }
        }
    }

    /**
     * 実際のバックグラウンド気象データ更新処理
     * 
     * @param isForced trueの場合、直近更新スロットリングをバイパスして強制実行
     * @return 実行結果（成功: true, 失敗: false）
     */
    suspend fun performBackgroundWeatherUpdate(isForced: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!updateMutex.tryLock()) {
            Log.d(TAG, "Background weather update already in progress; skipping concurrent invocation.")
            return@withContext true
        }
        try {
        val interval = weatherRepository.getWeatherUpdateInterval()
        if (interval == WeatherUpdateInterval.OFF) {
            Log.d(TAG, "performBackgroundWeatherUpdate called, but interval is OFF. Skipping.")
            return@withContext true
        }

        val now = System.currentTimeMillis()
        val lastUpdate = getLastUpdateTime()
        val minThrottlingGap = (interval.minutes * 60 * 1000L * 0.60).toLong().coerceAtLeast(3 * 60 * 1000L)

        if (!isForced && (now - lastUpdate) < minThrottlingGap) {
            Log.d(TAG, "Skipping redundant background update: updated recently (${(now - lastUpdate) / 1000}s ago < ${minThrottlingGap / 1000}s threshold).")
            return@withContext true
        }

        Log.d(TAG, "=== Starting Scheduled Background Weather Update ===")

        try {
            var currentGpsLocation: android.location.Location? = null
            val fallbackCity = weatherRepository.resolveFallbackCurrentCityOrNull()

            // 1. 対象の主都市（現在地、または直近で確定した位置・選択都市）を特定
            val primaryCity: City = if (locationProvider.hasLocationPermission()) {
                val loc = locationProvider.getCurrentLocation(forceFresh = true)
                currentGpsLocation = loc
                if (loc != null) {
                    val resolved = locationProvider.resolveCity(loc)
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Primary target resolved from current GPS: ${resolved.name}")
                    }
                    resolved
                } else {
                    fallbackCity ?: weatherRepository.defaultCity
                }
            } else {
                fallbackCity ?: weatherRepository.defaultCity
            }
            val hasResolvedLocationOrUserCity = currentGpsLocation != null || fallbackCity != null

            // 2. 主都市の天気予報および降雨ナウキャストを取得・Room DBへ保存
            val weatherData = weatherRepository.refresh(primaryCity)

            // 3. 降雨ナウキャストの評価とステータスバー・ライブ通知の更新（現在位置座標を伝達）
            val nowcast = weatherData.precipitationNowcast
            val isLiveNotifEnabled = WeatherRepository.isLiveAlertNotificationEnabled(context)
            if (!isLiveNotifEnabled) {
                if (!PrecipitationLiveNotificationManager.isTestActive) {
                    PrecipitationLiveNotificationManager.cancel(context)
                }
            } else if (!PrecipitationLiveNotificationManager.isTestActive) {
                if (hasResolvedLocationOrUserCity && nowcast != null && nowcast.isLiveActive && (nowcast.minutesRemaining == null || nowcast.minutesRemaining > 0)) {
                    PrecipitationLiveNotificationManager.updateRainLiveNotification(
                        context,
                        nowcast,
                        primaryCity.name,
                        currentGpsLocation
                    )
                } else if (PrecipitationLiveNotificationManager.currentActiveNowcast != null) {
                    PrecipitationLiveNotificationManager.onNowcastCleared(context, primaryCity.name)
                } else {
                    PrecipitationLiveNotificationManager.cancel(context)
                }
            }

            // 4. 気象警報・キキクル監視の更新トリガー
            try {
                KikikuruBackgroundMonitor.getInstance(context).checkCurrentAlert(forceFresh = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Kikikuru alert check error in background: ${e.message}")
            }

            // 5. 最終更新時刻・座標・ステータスを記録
            saveUpdateSuccess(primaryCity.name, currentGpsLocation?.latitude, currentGpsLocation?.longitude)

            // 6. 固定都市ウィジェットの天気も必要に応じて更新し、全ホーム画面ウィジェットを再描画
            val primaryKey = LocationKeyNormalizer.normalize(primaryCity.latitude, primaryCity.longitude)
            val pinnedCitiesToRefresh = WeatherWidgetUpdater.getActiveWidgets(context)
                .mapNotNull { entry ->
                    val cfg = WidgetPreferences.getConfig(context, entry.appWidgetId)
                    if (!cfg.useCurrentLocation) cfg.pinnedCity else null
                }
                .distinctBy { LocationKeyNormalizer.normalize(it.latitude, it.longitude) }
                .filter { LocationKeyNormalizer.normalize(it.latitude, it.longitude) != primaryKey }
            for (pinnedCity in pinnedCitiesToRefresh) {
                try {
                    weatherRepository.refresh(pinnedCity)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {}
            }
            try {
                WeatherWidgetUpdater.refreshActiveRadarWidgets(context, currentCityOverride = primaryCity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Background widget radar refresh failed: ${e.message}")
            }
            WeatherWidgetUpdater.updateAllFromCache(context, allowEnqueueRefresh = false)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Background weather update failed: ${e.message}", e)
            saveUpdateFailure(e.message ?: "Unknown error")
            false
        }
        } finally {
            updateMutex.unlock()
        }
    }

    /**
     * バックグラウンドで端末の現在位置が移動した際の高精度自動追従処理
     * 移動距離が100m以上の場合に新地点の気象データおよび降水ナウキャストを再取得
     */
    suspend fun onBackgroundLocationChanged(newLocation: android.location.Location): Boolean = withContext(Dispatchers.IO) {
        val interval = weatherRepository.getWeatherUpdateInterval()
        if (interval == WeatherUpdateInterval.OFF) {
            Log.d(TAG, "onBackgroundLocationChanged ignored: updates are OFF.")
            return@withContext true
        }

        val lastLat = getLastUpdateLat()
        val lastLon = getLastUpdateLon()

        val distance = if (lastLat != null && lastLon != null) {
            val prevLoc = android.location.Location("last").apply {
                latitude = lastLat
                longitude = lastLon
            }
            prevLoc.distanceTo(newLocation)
        } else {
            Float.MAX_VALUE
        }

        if (distance < 100f) {
            Log.d(TAG, "Background location jitter: moved ${distance.toInt()}m < 100m threshold. Skipping update.")
            return@withContext true
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Background location changed: moved ${distance.toInt()}m")
        }

        try {
            val primaryCity = locationProvider.resolveCity(newLocation)
            val weatherData = weatherRepository.refresh(primaryCity)

            val nowcast = weatherData.precipitationNowcast
            val isLiveNotifEnabled = WeatherRepository.isLiveAlertNotificationEnabled(context)
            if (!isLiveNotifEnabled) {
                if (!PrecipitationLiveNotificationManager.isTestActive) {
                    PrecipitationLiveNotificationManager.cancel(context)
                }
            } else if (!PrecipitationLiveNotificationManager.isTestActive) {
                if (nowcast != null && nowcast.isLiveActive && (nowcast.minutesRemaining == null || nowcast.minutesRemaining > 0)) {
                    PrecipitationLiveNotificationManager.updateRainLiveNotification(
                        context,
                        nowcast,
                        primaryCity.name,
                        newLocation
                    )
                } else if (PrecipitationLiveNotificationManager.currentActiveNowcast != null) {
                    PrecipitationLiveNotificationManager.onNowcastCleared(context, primaryCity.name)
                }
            }

            try {
                KikikuruBackgroundMonitor.getInstance(context).onLocationUpdated(
                    newLocation,
                    primaryCity.name,
                    primaryCity.country
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed updating Kikikuru on location shift: ${e.message}")
            }

            saveUpdateSuccess(primaryCity.name, newLocation.latitude, newLocation.longitude)
            try {
                WeatherWidgetUpdater.refreshActiveRadarWidgets(context, currentCityOverride = primaryCity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Location shift radar widget refresh failed: ${e.message}")
            }
            WeatherWidgetUpdater.updateAllFromCache(context, allowEnqueueRefresh = false)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error handling background location shift: ${e.message}", e)
            saveUpdateFailure(e.message ?: "Unknown location update error")
            false
        }
    }

    fun getLastUpdateTime(): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_UPDATE_TIME, 0L)
    }

    fun getLastUpdateCity(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_UPDATE_CITY, null)
    }

    fun getLastUpdateLat(): Double? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bitsKey = "${KEY_LAST_UPDATE_LAT}_bits"
        return when {
            prefs.contains(bitsKey) -> Double.fromBits(prefs.getLong(bitsKey, 0L))
            prefs.contains(KEY_LAST_UPDATE_LAT) -> prefs.getFloat(KEY_LAST_UPDATE_LAT, 0f).toDouble()
            else -> null
        }
    }

    fun getLastUpdateLon(): Double? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bitsKey = "${KEY_LAST_UPDATE_LON}_bits"
        return when {
            prefs.contains(bitsKey) -> Double.fromBits(prefs.getLong(bitsKey, 0L))
            prefs.contains(KEY_LAST_UPDATE_LON) -> prefs.getFloat(KEY_LAST_UPDATE_LON, 0f).toDouble()
            else -> null
        }
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun saveUpdateSuccess(cityName: String, lat: Double? = null, lon: Double? = null) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
            .putString(KEY_LAST_UPDATE_CITY, cityName)
            .putString(KEY_LAST_UPDATE_STATUS, "SUCCESS")

        if (lat != null && lon != null && lat.isFinite() && lon.isFinite()) {
            editor.putLong("${KEY_LAST_UPDATE_LAT}_bits", lat.toRawBits())
            editor.putLong("${KEY_LAST_UPDATE_LON}_bits", lon.toRawBits())
        }
        editor.apply()
    }

    private fun saveUpdateFailure(error: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_UPDATE_STATUS, "FAILED: $error")
            .apply()
    }
}
