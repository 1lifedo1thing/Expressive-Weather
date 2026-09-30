package com.example.expressiveweather.data

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
    private val locationProvider = CurrentLocationProvider(context)
    private val weatherRepository = WeatherRepository(context = context)

    companion object {
        private const val TAG = "WeatherBgUpdateMgr"
        private const val PREFS_NAME = "expressive_weather_prefs"
        private const val KEY_LAST_UPDATE_TIME = "last_bg_weather_update_time_ms"
        private const val KEY_LAST_UPDATE_CITY = "last_bg_weather_update_city"
        private const val KEY_LAST_UPDATE_STATUS = "last_bg_weather_update_status"
        private const val REQUEST_CODE_ALARM = 4001

        @Volatile
        private var INSTANCE: WeatherBackgroundUpdateManager? = null

        fun getInstance(context: Context): WeatherBackgroundUpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeatherBackgroundUpdateManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun init(context: Context): WeatherBackgroundUpdateManager {
            return getInstance(context).apply {
                scheduleAll()
            }
        }
    }

    /**
     * すべてのスケジュール機構（WorkManager, AlarmManager, In-Memoryループ）を一括設定
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
        Log.d(TAG, "Cancelling all weather background updates (Setting: OFF).")
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WeatherUpdateWorker.UNIQUE_WORK_NAME)
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling WorkManager work: ${e.message}")
        }
        cancelAlarm()
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
            // 1. 対象の主都市（現在地、または最後に選択された都市/デフォルト都市）を特定
            val primaryCity: City = if (locationProvider.hasLocationPermission()) {
                val loc = locationProvider.getCurrentLocation(forceFresh = false)
                if (loc != null) {
                    val resolved = locationProvider.resolveCity(loc)
                    Log.d(TAG, "Primary target resolved from current GPS: ${resolved.name} (${resolved.latitude}, ${resolved.longitude})")
                    resolved
                } else {
                    val defaultCity = weatherRepository.defaultCity
                    Log.d(TAG, "GPS location unavailable, using default city: ${defaultCity.name}")
                    defaultCity
                }
            } else {
                val defaultCity = weatherRepository.defaultCity
                Log.d(TAG, "Location permission not granted, using default city: ${defaultCity.name}")
                defaultCity
            }

            // 2. 主都市の天気予報および降雨ナウキャストを取得・Room DBへ保存
            Log.d(TAG, "Fetching & caching weather data for ${primaryCity.name}...")
            val weatherData = weatherRepository.refresh(primaryCity)
            Log.d(TAG, "Weather data successfully cached in Room for ${primaryCity.name}: ${weatherData.currentTempC}°C, ${weatherData.condition}")

            // 3. 降雨ナウキャストの評価とステータスバー・ライブ通知の更新
            val nowcast = weatherData.precipitationNowcast
            if (nowcast != null) {
                Log.d(TAG, "Precipitation nowcast active for ${primaryCity.name}: ${nowcast.headline} (${nowcast.minutesRemaining} min). Updating live notification.")
                PrecipitationLiveNotificationManager.updateRainLiveNotification(
                    context,
                    nowcast,
                    primaryCity.name
                )
            } else {
                PrecipitationLiveNotificationManager.cancel(context)
            }

            // 4. 気象警報・キキクル監視の更新トリガー
            try {
                KikikuruBackgroundMonitor.getInstance(context).checkCurrentAlert(forceFresh = true)
            } catch (e: Exception) {
                Log.w(TAG, "Kikikuru alert check error in background: ${e.message}")
            }

            // 5. 最終更新時刻とステータスを記録（現在地のみを確実に更新）
            saveUpdateSuccess(primaryCity.name)
            Log.d(TAG, "=== Background Weather Update Completed Successfully for ${primaryCity.name} (Current Location Only) ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Background weather update failed: ${e.message}", e)
            saveUpdateFailure(e.message ?: "Unknown error")
            false
        }
    }

    /**
     * 過去の最終更新ミリ秒を取得
     */
    fun getLastUpdateTime(): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_UPDATE_TIME, 0L)
    }

    /**
     * 最終更新された都市名を取得
     */
    fun getLastUpdateCity(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_UPDATE_CITY, null)
    }

    /**
     * バッテリー最適化除外（制限なし）状態かどうか
     */
    fun isBatteryOptimizationIgnored(): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun saveUpdateSuccess(cityName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
            .putString(KEY_LAST_UPDATE_CITY, cityName)
            .putString(KEY_LAST_UPDATE_STATUS, "SUCCESS")
            .apply()
    }

    private fun saveUpdateFailure(error: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_UPDATE_STATUS, "FAILED: $error")
            .apply()
    }
}
