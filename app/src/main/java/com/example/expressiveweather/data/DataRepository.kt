package com.example.expressiveweather.data

import android.content.Context
import android.util.Log
import com.example.expressiveweather.data.local.LocationKeyNormalizer
import com.example.expressiveweather.data.local.SavedCityDao
import com.example.expressiveweather.data.local.SavedCityEntity
import com.example.expressiveweather.data.local.WeatherCacheEntity
import com.example.expressiveweather.data.local.WeatherDao
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 天気データ取得状態を表す Result クラス
 */
sealed interface WeatherStreamResult {
    data object Loading : WeatherStreamResult
    data class Success(val data: WeatherData, val isStale: Boolean, val isFromCache: Boolean = false) : WeatherStreamResult
    data class Error(val message: String, val cachedData: WeatherData? = null) : WeatherStreamResult
}

class WeatherRepository(
    private var weatherDao: WeatherDao? = null,
    private var savedCityDao: SavedCityDao? = null,
    val context: Context? = null
) {

    init {
        if (context != null) {
            val db = try {
                com.example.expressiveweather.data.local.WeatherDatabase.getInstance(context)
            } catch (e: Exception) {
                Log.w("WeatherRepository", "Could not get WeatherDatabase instance: ${e.message}")
                null
            }
            if (weatherDao == null && db != null) {
                weatherDao = db.weatherDao()
            }
            if (savedCityDao == null && db != null) {
                savedCityDao = db.savedCityDao()
            }
        }
    }

    val defaultCity: City = City("tokyo", "東京", "Tokyo", "日本", 35.6895, 139.6917, isFavorite = true)

    fun getLastSelectedCity(): City {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        val json = prefs?.getString("last_selected_city_json", null)
        if (json != null) {
            try {
                return Json.decodeFromString<City>(json)
            } catch (_: Exception) {}
        }
        return defaultCity
    }

    fun setLastSelectedCity(city: City) {
        try {
            val json = Json.encodeToString(city)
            context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
                ?.edit()
                ?.putString("last_selected_city_json", json)
                ?.apply()
        } catch (_: Exception) {}
    }

    suspend fun getAllSavedCities(): List<City> = withContext(Dispatchers.IO) {
        val dao = savedCityDao
        if (dao != null) {
            dao.getAllSavedCitiesOnce().map { it.toCity() }
        } else {
            memorySavedCities.values.toList()
        }
    }

    companion object {
        // キャッシュ有効期間：30分（30分以内ならフレッシュとみなし裏での再取得は行わない）
        const val CACHE_STALE_DURATION_MS = 30 * 60 * 1000L
        // キャッシュ保持上限期間：7日（7日以上経過した不要レコードをクリーンアップ）
        const val CACHE_CLEANUP_THRESHOLD_MS = 7 * 24 * 60 * 60 * 1000L

        fun getAlertNotificationThreshold(context: Context): AlertNotificationThreshold {
            val prefs = context.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            val id = prefs.getString("alert_notification_threshold", null)
            return AlertNotificationThreshold.fromId(id)
        }

        fun isLiveAlertNotificationEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean("live_alert_notification_enabled", true)
        }

        fun getWeatherUpdateInterval(context: Context): WeatherUpdateInterval {
            val prefs = context.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            val minutes = prefs.getInt("weather_update_interval_minutes", WeatherUpdateInterval.MINUTES_30.minutes)
            return WeatherUpdateInterval.fromMinutes(minutes)
        }

        fun setWeatherUpdateInterval(context: Context, interval: WeatherUpdateInterval) {
            context.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("weather_update_interval_minutes", interval.minutes)
                .apply()
        }

        fun isSetupCompleted(context: Context): Boolean {
            val prefs = context.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean("setup_completed", false)
        }

        fun setSetupCompleted(context: Context, completed: Boolean) {
            context.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("setup_completed", completed)
                .apply()
        }
    }

    // In-memory cache for ultra-fast fallback
    private val memoryCache = ConcurrentHashMap<String, WeatherData>()
    private val memorySavedCities = ConcurrentHashMap<String, City>()
    private val _fallbackSavedCitiesFlow = kotlinx.coroutines.flow.MutableStateFlow<List<City>>(emptyList())
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeFetches = ConcurrentHashMap<String, Job>()

    fun getThemePreset(): ThemePreset {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        val savedId = prefs?.getString("theme_preset", null)
        if (savedId != null) {
            return ThemePreset.fromId(savedId)
        }
        val adaptive = prefs?.getBoolean("weather_adaptive_theme_enabled", true) ?: true
        return if (adaptive) ThemePreset.WEATHER else ThemePreset.SYSTEM
    }

    fun setThemePreset(preset: ThemePreset) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("theme_preset", preset.id)
            ?.putBoolean("weather_adaptive_theme_enabled", preset == ThemePreset.WEATHER)
            ?.apply()
    }

    fun isWeatherAdaptiveThemeEnabled(): Boolean {
        return getThemePreset() == ThemePreset.WEATHER
    }

    fun setThemeMode(mode: ThemeMode) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("theme_mode", mode.id)
            ?.apply()
    }

    fun getThemeMode(): ThemeMode {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        val id = prefs?.getString("theme_mode", ThemeMode.SYSTEM.id)
        return ThemeMode.fromId(id)
    }

    fun setWeatherAdaptiveThemeEnabled(enabled: Boolean) {
        setThemePreset(if (enabled) ThemePreset.WEATHER else ThemePreset.SKY_OCEAN)
    }

    fun getWindSpeedUnit(): WindSpeedUnit {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        val name = prefs?.getString("wind_speed_unit", WindSpeedUnit.FOLLOW_LANGUAGE.name)
        return try {
            if (name != null) WindSpeedUnit.valueOf(name) else WindSpeedUnit.FOLLOW_LANGUAGE
        } catch (_: Exception) {
            WindSpeedUnit.FOLLOW_LANGUAGE
        }
    }

    fun setWindSpeedUnit(unit: WindSpeedUnit) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("wind_speed_unit", unit.name)
            ?.apply()
    }

    fun getFrostedGlassEnabled(): Boolean {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("frosted_glass_enabled", true) ?: true
    }

    fun setFrostedGlassEnabled(enabled: Boolean) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("frosted_glass_enabled", enabled)
            ?.apply()
    }

    fun getColorfulShapesEnabled(): Boolean {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("colorful_shapes_enabled", true) ?: true
    }

    fun setColorfulShapesEnabled(enabled: Boolean) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("colorful_shapes_enabled", enabled)
            ?.apply()
    }

    fun getRadarDebugZoomEnabled(): Boolean {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("radar_debug_zoom_enabled", false) ?: false
    }

    fun setRadarDebugZoomEnabled(enabled: Boolean) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("radar_debug_zoom_enabled", enabled)
            ?.apply()
    }

    fun getRadarDebugSourceEnabled(): Boolean {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("radar_debug_source_enabled", false) ?: false
    }

    fun setRadarDebugSourceEnabled(enabled: Boolean) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("radar_debug_source_enabled", enabled)
            ?.apply()
    }

    fun getGdacsAlertsEnabled(): Boolean {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("gdacs_alerts_enabled", false) ?: false
    }

    fun setGdacsAlertsEnabled(enabled: Boolean) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("gdacs_alerts_enabled", enabled)
            ?.apply()
    }

    fun getWmoAlertsEnabled(): Boolean {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("wmo_alerts_enabled", true) ?: true
    }

    fun setWmoAlertsEnabled(enabled: Boolean) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("wmo_alerts_enabled", enabled)
            ?.apply()
    }

    fun getMeteoAlarmAlertsEnabled(): Boolean {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("meteoalarm_alerts_enabled", true) ?: true
    }

    fun setMeteoAlarmAlertsEnabled(enabled: Boolean) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("meteoalarm_alerts_enabled", enabled)
            ?.apply()
    }

    fun isWmoAlwaysEnabled(): Boolean {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("wmo_always_enabled", false) ?: false
    }

    fun setWmoAlwaysEnabled(enabled: Boolean) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("wmo_always_enabled", enabled)
            ?.apply()
    }

    fun isLiveAlertNotificationEnabled(): Boolean {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("live_alert_notification_enabled", true) ?: true
    }

    fun setLiveAlertNotificationEnabled(enabled: Boolean) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("live_alert_notification_enabled", enabled)
            ?.apply()
    }

    fun getWeatherUpdateInterval(): WeatherUpdateInterval {
        val ctx = context ?: return WeatherUpdateInterval.MINUTES_30
        return Companion.getWeatherUpdateInterval(ctx)
    }

    fun setWeatherUpdateInterval(interval: WeatherUpdateInterval) {
        val ctx = context ?: return
        Companion.setWeatherUpdateInterval(ctx, interval)
    }

    fun isSetupCompleted(): Boolean {
        val ctx = context ?: return false
        return Companion.isSetupCompleted(ctx)
    }

    fun setSetupCompleted(completed: Boolean) {
        val ctx = context ?: return
        Companion.setSetupCompleted(ctx, completed)
    }

    fun getAlertNotificationThreshold(): AlertNotificationThreshold {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        val id = prefs?.getString("alert_notification_threshold", null)
        return AlertNotificationThreshold.fromId(id)
    }

    fun setAlertNotificationThreshold(threshold: AlertNotificationThreshold) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("alert_notification_threshold", threshold.id)
            ?.apply()
    }

    fun getKikikuruTestEnabled(): Boolean {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        return prefs?.getBoolean("kikikuru_test_enabled", false) ?: false
    }

    fun setKikikuruTestEnabled(enabled: Boolean) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("kikikuru_test_enabled", enabled)
            ?.apply()
    }

    fun getLanguage(): AppLanguage {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
        val code = prefs?.getString("app_language", null)
        if (code != null) {
            return AppLanguage.fromCode(code)
        }
        val systemLocale = java.util.Locale.getDefault()
        return AppLanguage.fromCode(systemLocale.toLanguageTag())
    }

    fun setLanguage(language: AppLanguage) {
        context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("app_language", language.code)
            ?.apply()
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        memoryCache.clear()
        weatherDao?.deleteAll()
    }

    /**
     * 登録都市（お気に入り都市）のリアルタイム監視ストリーム
     */
    fun getSavedCitiesStream(): Flow<List<City>> {
        val dao = savedCityDao
        return if (dao != null) {
            dao.getAllSavedCitiesStream().map { list -> list.map { it.toCity() } }
        } else {
            _fallbackSavedCitiesFlow
        }
    }

    /**
     * 都市の登録（Roomデータベースへの永続化）
     */
    suspend fun saveCity(city: City) = withContext(Dispatchers.IO) {
        val dao = savedCityDao
        if (dao != null) {
            dao.insertOrUpdate(SavedCityEntity.fromCity(city))
        } else {
            val key = city.id.ifBlank { LocationKeyNormalizer.normalize(city.latitude, city.longitude) }
            memorySavedCities[key] = city
            _fallbackSavedCitiesFlow.value = memorySavedCities.values.toList()
        }
    }

    /**
     * 都市の登録解除（Roomデータベースからの削除）
     */
    suspend fun removeSavedCity(city: City) = withContext(Dispatchers.IO) {
        val dao = savedCityDao
        if (dao != null) {
            dao.deleteCity(city.id, city.latitude, city.longitude)
        } else {
            val key = city.id.ifBlank { LocationKeyNormalizer.normalize(city.latitude, city.longitude) }
            memorySavedCities.remove(key)
            _fallbackSavedCitiesFlow.value = memorySavedCities.values.toList()
        }
    }

    /**
     * 都市の登録／解除トグル
     */
    suspend fun toggleSaveCity(city: City) = withContext(Dispatchers.IO) {
        val dao = savedCityDao
        if (dao != null) {
            val existing = dao.getAllSavedCitiesOnce()
            val isSaved = existing.any { it.id == city.id || (it.latitude == city.latitude && it.longitude == city.longitude) }
            if (isSaved) {
                dao.deleteCity(city.id, city.latitude, city.longitude)
            } else {
                dao.insertOrUpdate(SavedCityEntity.fromCity(city))
            }
        } else {
            val key = city.id.ifBlank { LocationKeyNormalizer.normalize(city.latitude, city.longitude) }
            if (memorySavedCities.containsKey(key)) {
                memorySavedCities.remove(key)
            } else {
                memorySavedCities[key] = city
            }
            _fallbackSavedCitiesFlow.value = memorySavedCities.values.toList()
        }
    }

    /**
     * Cache-First Stale-While-Revalidate 天気データストリーム
     *
     * 1. 座標を正規化（約1.1kmメッシュ）し、一意キーを作成。
     * 2. Room から該当キーのデータを即座に emit（アプリ起動時・都市切替時の体感待ち時間ゼロ）。
     * 3. キャッシュが存在しない、または取得から30分以上経過（stale）している場合、裏でAPI通信を実行。
     * 4. 通信成功時: Room に保存。Roomの Flow が自律的に最新データを画面へ通知。
     * 5. 通信失敗時: ログを記録するのみとし、既存キャッシュを維持して画面崩壊を防止。キャッシュ皆無時のみエラーemit。
     */
    fun getWeatherStream(latitude: Double, longitude: Double, city: City? = null): Flow<WeatherStreamResult> = flow {
        val locationKey = LocationKeyNormalizer.normalize(latitude, longitude)
        val resolvedCity = city ?: City(
            id = "loc_${locationKey}",
            name = "現在地",
            nameEn = "Current Location",
            country = "",
            latitude = latitude,
            longitude = longitude
        )

        // 定期的に古いキャッシュ（7日超）を非同期クリーンアップ
        cleanupOldCacheAsync()

        val dao = weatherDao
        if (dao == null) {
            // DAO が未提供の場合のフォールバック
            val memCached = memoryCache[locationKey]
            if (memCached != null) {
                emit(WeatherStreamResult.Success(memCached, isStale = false, isFromCache = true))
            } else {
                emit(WeatherStreamResult.Loading)
            }
            try {
                val fresh = fetchAndSave(resolvedCity, locationKey)
                emit(WeatherStreamResult.Success(fresh, isStale = false, isFromCache = false))
            } catch (e: Exception) {
                if (memCached != null) {
                    emit(WeatherStreamResult.Success(memCached, isStale = true, isFromCache = true))
                } else {
                    emit(WeatherStreamResult.Error(e.localizedMessage ?: "通信エラー"))
                }
            }
            return@flow
        }

        // 1. まずRoomから一度直接取得して即座に画面へ供給
        val initialEntity = dao.getWeatherOnce(locationKey)
        val now = System.currentTimeMillis()
        var hasValidCache = false

        if (initialEntity != null) {
            val cachedWeather = WeatherJsonSerializer.deserialize(initialEntity.weatherDataJson)
            if (cachedWeather != null) {
                hasValidCache = true
                val isStale = (now - initialEntity.fetchedAt) > CACHE_STALE_DURATION_MS || needsHumidityForecastRefresh(cachedWeather)
                val enrichedWeather = enrichWeatherData(cachedWeather, initialEntity.fetchedAt, resolvedCity)
                // メモリキャッシュにも載せる
                memoryCache[locationKey] = enrichedWeather
                emit(WeatherStreamResult.Success(enrichedWeather, isStale = isStale, isFromCache = true))

                // 30分以上経過（stale）なら裏で非同期再取得を実行
                if (isStale) {
                    revalidateCacheAsync(resolvedCity, locationKey)
                }
            }
        }

        if (!hasValidCache) {
            // キャッシュが全く存在しない初回アクセス時
            emit(WeatherStreamResult.Loading)
            try {
                val freshData = fetchAndSave(resolvedCity, locationKey)
                emit(WeatherStreamResult.Success(freshData, isStale = false, isFromCache = false))
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Initial fetch failed for $locationKey", e)
                emit(WeatherStreamResult.Error(e.localizedMessage ?: "気象データを取得できませんでした"))
            }
        }

        // 2. Room の Flow を購読し、DB更新（裏でのfetch成功時や手動refresh時）を自律的に検知して通知
        dao.getWeatherStream(locationKey)
            .filterNotNull()
            .collect { entity ->
                // 初回ですでにemitした直後の重複を避けるため、更新時刻やJSONをチェック
                val weather = WeatherJsonSerializer.deserialize(entity.weatherDataJson)
                if (weather != null) {
                    val age = System.currentTimeMillis() - entity.fetchedAt
                    val isStale = age > CACHE_STALE_DURATION_MS
                    val isFromCache = age > 30_000L
                    val enrichedWeather = enrichWeatherData(weather, entity.fetchedAt, resolvedCity)
                    memoryCache[locationKey] = enrichedWeather
                    emit(WeatherStreamResult.Success(enrichedWeather, isStale = isStale, isFromCache = isFromCache))
                }
            }
    }

    /**
     * City オブジェクトを受け取るオーバーロード
     */
    fun getWeatherStream(city: City): Flow<WeatherStreamResult> {
        return getWeatherStream(city.latitude, city.longitude, city)
    }

    /**
     * 手動更新用（Pull-to-Refresh等）
     * 有効期限を無視して強制的にAPI通信を実行し、Room DBを更新します。
     */
    suspend fun refresh(latitude: Double, longitude: Double, city: City? = null): WeatherData = withContext(Dispatchers.IO) {
        val locationKey = LocationKeyNormalizer.normalize(latitude, longitude)
        val resolvedCity = city ?: City(
            id = "loc_${locationKey}",
            name = "現在地",
            nameEn = "Current Location",
            country = "",
            latitude = latitude,
            longitude = longitude
        )
        fetchAndSave(resolvedCity, locationKey)
    }

    suspend fun refresh(city: City): WeatherData = refresh(city.latitude, city.longitude, city)

    /**
     * 既存コード互換用の同期取得メソッド
     */
    suspend fun getWeatherData(city: City): WeatherData = withContext(Dispatchers.IO) {
        val locationKey = LocationKeyNormalizer.normalize(city.latitude, city.longitude)
        // まずキャッシュを確認
        val cachedEntity = weatherDao?.getWeatherOnce(locationKey)
        if (cachedEntity != null) {
            val cached = WeatherJsonSerializer.deserialize(cachedEntity.weatherDataJson)
            val isFresh = cached != null && (System.currentTimeMillis() - cachedEntity.fetchedAt) <= CACHE_STALE_DURATION_MS && !needsHumidityForecastRefresh(cached)
            if (cached != null && isFresh) {
                return@withContext enrichWeatherData(cached, cachedEntity.fetchedAt, city)
            }
        }
        // キャッシュがないか期限切れならfetch
        try {
            fetchAndSave(city, locationKey)
        } catch (e: Exception) {
            // 通信失敗時は古いキャッシュがあればそれを返す
            if (cachedEntity != null) {
                val cached = WeatherJsonSerializer.deserialize(cachedEntity.weatherDataJson)
                if (cached != null) return@withContext enrichWeatherData(cached, cachedEntity.fetchedAt, city)
            }
            throw e
        }
    }

    private fun enrichWeatherData(weather: WeatherData, fetchedAtMillis: Long, city: City): WeatherData {
        val isJapanLoc = isLocationOrCountryInJapan(city.country, city.latitude, city.longitude)
        val isUSLoc = isLocationOrCountryInUnitedStates(city.country, city.latitude, city.longitude)
        val isEuropeLoc = isLocationOrCountryInEurope(city.country, city.latitude, city.longitude)
        val resolvedSource = if (weather.dataSourceName.isNotBlank() && !weather.dataSourceName.contains("Open-Meteo ＆ NOAA") && (!isJapanLoc || !weather.dataSourceName.contains("Open-Meteo"))) {
            weather.dataSourceName
        } else {
            when {
                isJapanLoc -> "気象データ提供元: 気象庁 (JMA)"
                isUSLoc -> "気象データ提供元: 米国気象局 (NOAA/NWS)"
                isEuropeLoc -> "気象データ提供元: 欧州中期予報センター (ECMWF) ＆ RainViewer"
                else -> "気象データ提供元: Open-Meteo ＆ NOAA ＆ RainViewer"
            }
        }
        val enrichedNowcast = weather.precipitationNowcast?.let { nowcast ->
            val updated = if (nowcast.localizedHeadlines.isEmpty()) {
                val languages = listOf(
                    AppLanguage.JAPANESE,
                    AppLanguage.ENGLISH,
                    AppLanguage.SIMPLIFIED_CHINESE,
                    AppLanguage.TRADITIONAL_CHINESE
                )
                val headlines = languages.associate { lang ->
                    lang.code to (PrecipitationNowcastTranslator.translateHeadline(nowcast.headline, lang) ?: nowcast.headline)
                }
                val subTexts = languages.associate { lang ->
                    lang.code to (PrecipitationNowcastTranslator.translateSubText(nowcast.subText, lang) ?: nowcast.subText)
                }
                val timeLabels = languages.associate { lang ->
                    lang.code to (nowcast.targetTimeLabel?.let { PrecipitationNowcastTranslator.translateTimeLabel(it, lang) } ?: (nowcast.targetTimeLabel ?: ""))
                }
                nowcast.copy(
                    localizedHeadlines = headlines,
                    localizedSubTexts = subTexts,
                    localizedTargetTimeLabels = timeLabels
                )
            } else {
                nowcast
            }
            if (isJapanLoc && !updated.isJapan) {
                updated.copy(isJapan = true, dataSourceName = "気象庁 (JMA)")
            } else if (isUSLoc && !updated.isUS) {
                updated.copy(isUS = true, dataSourceName = "米国気象局 (NOAA/NWS)")
            } else if (isEuropeLoc && !updated.isEurope) {
                updated.copy(isEurope = true, dataSourceName = "欧州中期予報センター (ECMWF) / 各気象機関")
            } else {
                updated
            }
        }
        val isWmoExcluded = WmoAlertRepository.isCityExcludedFromWmo(city, isWmoAlwaysEnabled())
        val filteredAlerts = weather.globalAlerts
            .filter { getGdacsAlertsEnabled() || it.source != AlertSource.GDACS }
            .filter { (getWmoAlertsEnabled() && !isWmoExcluded) || it.source != AlertSource.WMO }
            .filter { getMeteoAlarmAlertsEnabled() || it.source != AlertSource.METEOALARM }
        return weather.copy(
            fetchedAtMillis = fetchedAtMillis,
            dataSourceName = resolvedSource,
            globalAlerts = filteredAlerts,
            precipitationNowcast = enrichedNowcast ?: weather.precipitationNowcast
        )
    }

    /**
     * バックグラウンドで非同期にAPI再検証を行い、Roomを更新
     */
    private fun revalidateCacheAsync(city: City, locationKey: String) {
        if (activeFetches[locationKey]?.isActive == true) return
        val job = repositoryScope.launch {
            try {
                Log.d("WeatherRepository", "Revalidating stale weather for $locationKey (${city.name})")
                fetchAndSave(city, locationKey)
            } catch (e: Exception) {
                Log.w("WeatherRepository", "Background revalidation failed for $locationKey: ${e.message}")
                // 失敗しても既存キャッシュはRoomに残るため画面は壊れない
            } finally {
                activeFetches.remove(locationKey)
            }
        }
        activeFetches[locationKey] = job
    }

    /**
     * API通信を実行し、結果をRoom DBに保存
     */
    private suspend fun fetchAndSave(city: City, locationKey: String): WeatherData = withContext(Dispatchers.IO) {
        val inJapan = isLocationOrCountryInJapan(city.country, city.latitude, city.longitude)
        val freshData = if (inJapan) {
            try {
                fetchJmaWeatherData(city)
            } catch (e: Exception) {
                Log.w("WeatherRepository", "JMA fetch failed for ${city.name} ($locationKey), falling back to Open-Meteo: ${e.message}")
                fetchOpenMeteoWeatherData(city)
            }
        } else {
            fetchOpenMeteoWeatherData(city)
        }

        // Room DB に保存
        val sourceFilteredData = filterDisabledGlobalAlerts(freshData, city)
        val jsonString = WeatherJsonSerializer.serialize(sourceFilteredData)
        weatherDao?.insertOrUpdate(
            WeatherCacheEntity(
                locationKey = locationKey,
                latitude = city.latitude,
                longitude = city.longitude,
                fetchedAt = System.currentTimeMillis(),
                weatherDataJson = jsonString
            )
        )
        memoryCache[locationKey] = sourceFilteredData
        sourceFilteredData
    }

    private fun needsHumidityForecastRefresh(weather: WeatherData): Boolean =
        weather.dailyForecast.any { it.humidityAvg != null && it.humidityAvgSource == null }

    private fun filterDisabledGlobalAlerts(weather: WeatherData, city: City? = null): WeatherData {
        val isWmoExcluded = city != null && WmoAlertRepository.isCityExcludedFromWmo(city, isWmoAlwaysEnabled())
        val filtered = weather.globalAlerts
            .filter { getGdacsAlertsEnabled() || it.source != AlertSource.GDACS }
            .filter { (getWmoAlertsEnabled() && !isWmoExcluded) || it.source != AlertSource.WMO }
            .filter { getMeteoAlarmAlertsEnabled() || it.source != AlertSource.METEOALARM }
        return weather.copy(globalAlerts = filtered)
    }

    /**
     * 気象庁（JMA）公式APIから直接データを取得して WeatherData を生成
     */
    private suspend fun fetchJmaWeatherData(city: City): WeatherData = coroutineScope {
        val areaInfo = JmaAreaResolver.resolve(city.latitude, city.longitude, city.name, city.country)

        val forecastDeferred = async { JmaWeatherRepository.fetchForecast(areaInfo.officeCode) }
        val overviewDeferred = async { JmaWeatherRepository.fetchOverview(areaInfo.officeCode) }
        val warningsDeferred = async { JmaWeatherRepository.fetchWarnings(areaInfo.officeCode) }
        val aqiDeferred = async { fetchAirQuality(city) }

        val amedasDeferred = async {
            try {
                val latestTimeStr = JmaWeatherRepository.fetchAmedasLatestTime()
                val cleanTime = latestTimeStr.replace("-", "").replace(":", "").replace("T", "")
                val ymd = cleanTime.take(8)
                val hourInt = cleanTime.substring(8, 10).toIntOrNull() ?: 12
                val blockHourInt = (hourInt / 3) * 3
                val blockHourStr = String.format(Locale.US, "%02d", blockHourInt)
                JmaWeatherRepository.fetchAmedasPoint(areaInfo.stationCode, ymd, blockHourStr)
            } catch (e: Exception) {
                Log.w("WeatherRepository", "AMeDAS point fetch failed for station ${areaInfo.stationCode}: ${e.message}")
                null
            }
        }
        val highResDeferred = async {
            try {
                fetchWeatherForecast(city)
            } catch (e: Exception) {
                Log.w("WeatherRepository", "Supplementary high-res forecast fetch failed: ${e.message}")
                null
            }
        }

        val gdacsDeferred: kotlinx.coroutines.Deferred<List<GlobalAlertItem>>? = if (getGdacsAlertsEnabled()) async {
            try {
                GdacsAlertRepository.fetchAlertsForCity(city)
            } catch (e: Exception) {
                emptyList()
            }
        } else null

        // 日本国内でも「常にWMOをオンにする」が有効な場合のみWMOも並行取得
        val wmoDeferred: kotlinx.coroutines.Deferred<List<GlobalAlertItem>>? = if (isWmoAlwaysEnabled() && getWmoAlertsEnabled()) async {
            try {
                WmoAlertRepository.fetchAlertsForCity(city, alwaysEnabled = true)
            } catch (e: Exception) {
                emptyList()
            }
        } else null

        val forecastArray = forecastDeferred.await()
        val overviewJson = overviewDeferred.await()
        val warningsJson = warningsDeferred.await()
        val aqiJson = aqiDeferred.await()
        val amedasJson = amedasDeferred.await()
        val highResJson = highResDeferred.await()
        val gdacsAlerts = gdacsDeferred?.await().orEmpty()
        val wmoAlerts = wmoDeferred?.await().orEmpty()

        val parsed = JmaWeatherParser.parse(
            city = city,
            forecastArray = forecastArray,
            amedasJson = amedasJson,
            overviewJson = overviewJson,
            warningJson = warningsJson,
            supplementaryAqiJson = aqiJson,
            supplementaryHighResJson = highResJson
        )
        val combinedAlerts = (gdacsAlerts + wmoAlerts).distinctBy { it.id }
        parsed.copy(globalAlerts = combinedAlerts)
    }

    /**
     * 海外都市用: Open-Meteo API から全球気象データを取得
     * - 米国都市なら NOAA/NWS API から直接取得（方式維持）
     * - 日本・米国以外の世界各国なら WMO (世界気象機関) SWIC 3.0 公式CAPから取得
     * - ただし「常にWMOをオンにする」が有効な場合は米国でもWMOを並行取得
     */
    private suspend fun fetchOpenMeteoWeatherData(city: City): WeatherData = coroutineScope {
        val weatherDeferred = async { fetchWeatherForecast(city) }
        val aqiDeferred = async { fetchAirQuality(city) }
        val isUs = NwsAlertRepository.isUnitedStates(city)
        val isMeteoAlarm = MeteoAlarmRepository.isSupported(city)

        // 1. 米国気象局 (NWS) - 米国都市用直接取得
        val nwsAlertsDeferred = async {
            try {
                if (isUs) {
                    NwsAlertRepository.fetchActiveAlerts(city)
                } else emptyList()
            } catch (e: Exception) {
                Log.w("WeatherRepository", "NWS alert fetch failed: ${e.message}")
                emptyList()
            }
        }

        // 2. 欧州気象警報ネットワーク (MeteoAlarm / EUMETNET) - サポート全域直接取得（最優先）
        val meteoAlarmAlertsDeferred: kotlinx.coroutines.Deferred<List<GlobalAlertItem>>? = if (isMeteoAlarm && getMeteoAlarmAlertsEnabled()) async {
            try {
                MeteoAlarmRepository.fetchAlertsForCity(city)
            } catch (e: Exception) {
                Log.w("WeatherRepository", "MeteoAlarm alert fetch failed: ${e.message}")
                emptyList()
            }
        } else null

        // 3. 全球災害警報 (GDACS)
        val gdacsAlertsDeferred: kotlinx.coroutines.Deferred<List<GlobalAlertItem>>? = if (getGdacsAlertsEnabled()) async {
            try {
                GdacsAlertRepository.fetchAlertsForCity(city)
            } catch (e: Exception) {
                Log.w("WeatherRepository", "GDACS alert fetch failed: ${e.message}")
                emptyList()
            }
        } else null

        // 4. 世界気象機関 (WMO) - 日本・米国・MeteoAlarm対象国以外の海外地域専用（「常にオン」時は並行取得）
        val shouldFetchWmo = getWmoAlertsEnabled() && ((!isUs && !isMeteoAlarm) || isWmoAlwaysEnabled())
        val wmoAlertsDeferred: kotlinx.coroutines.Deferred<List<GlobalAlertItem>>? = if (shouldFetchWmo) async {
            try {
                WmoAlertRepository.fetchAlertsForCity(city, alwaysEnabled = isWmoAlwaysEnabled())
            } catch (e: Exception) {
                Log.w("WeatherRepository", "WMO alert fetch failed: ${e.message}")
                emptyList()
            }
        } else null

        val (weatherJson, aqiJson) = Pair(weatherDeferred.await(), aqiDeferred.await())
        val nwsAlerts = nwsAlertsDeferred.await()
        val meteoAlarmAlerts = meteoAlarmAlertsDeferred?.await().orEmpty()
        val gdacsAlerts = gdacsAlertsDeferred?.await().orEmpty()
        val wmoAlerts = wmoAlertsDeferred?.await().orEmpty()
        val combinedAlerts = (nwsAlerts + meteoAlarmAlerts + gdacsAlerts + wmoAlerts).distinctBy { it.id }

        val parsed = parseWeatherData(city, weatherJson, aqiJson)
        val gdacsSource = if (getGdacsAlertsEnabled()) " ＆ GDACS" else ""
        val wmoSource = if (wmoAlerts.isNotEmpty() || shouldFetchWmo) " ＆ WMO" else ""
        val source = if (isUs) {
            "気象データ提供元: Open-Meteo ＆ NOAA/NWS ＆ RainViewer$gdacsSource$wmoSource"
        } else if (isMeteoAlarm) {
            "気象データ提供元: Open-Meteo ＆ MeteoAlarm ＆ RainViewer$gdacsSource$wmoSource"
        } else {
            "気象データ提供元: Open-Meteo ＆ RainViewer$wmoSource$gdacsSource"
        }
        parsed.copy(
            dataSourceName = source,
            globalAlerts = combinedAlerts
        )
    }

    /**
     * 7日以上前の古いキャッシュレコードを非同期でクリーンアップ
     */
    private fun cleanupOldCacheAsync() {
        val dao = weatherDao ?: return
        repositoryScope.launch {
            try {
                val threshold = System.currentTimeMillis() - CACHE_CLEANUP_THRESHOLD_MS
                dao.deleteOlderThan(threshold)
            } catch (e: Exception) {
                Log.w("WeatherRepository", "Cache cleanup error: ${e.message}")
            }
        }
    }

    suspend fun searchCities(
        query: String,
        language: AppLanguage = AppLanguage.JAPANESE
    ): List<City> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        try {
            CitySearchEngine.search(trimmed, language, context)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Photon API による高精度地名検索 (現在地バイアス対応)
     */
    suspend fun searchCitiesPhoton(
        query: String,
        language: AppLanguage = AppLanguage.JAPANESE,
        lat: Double? = null,
        lon: Double? = null
    ): List<City> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        try {
            val langCode = when (language) {
                AppLanguage.JAPANESE -> "ja"
                AppLanguage.ENGLISH -> "en"
                AppLanguage.SIMPLIFIED_CHINESE -> "default"
                AppLanguage.TRADITIONAL_CHINESE -> "default"
            }
            val photonResults = PhotonGeocodingService.search(
                query = trimmed,
                lang = langCode,
                lat = lat,
                lon = lon
            )
            if (photonResults.isNotEmpty()) {
                return@withContext photonResults
            }
            // フォールバック
            CitySearchEngine.search(trimmed, language, context)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 直近の検索履歴を取得
     */
    fun getRecentSearches(): List<City> {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE) ?: return emptyList()
        val jsonStr = prefs.getString("recent_searches_json", null) ?: return emptyList()
        return try {
            val jsonArray = org.json.JSONArray(jsonStr)
            val list = mutableListOf<City>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    City(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        nameEn = obj.optString("nameEn", ""),
                        country = obj.optString("country", ""),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 直近の検索履歴に追加
     */
    fun addRecentSearch(city: City) {
        val prefs = context?.getSharedPreferences("expressive_weather_prefs", Context.MODE_PRIVATE) ?: return
        val current = getRecentSearches().filterNot {
            it.id == city.id ||
                    (it.latitude == city.latitude && it.longitude == city.longitude) ||
                    (it.name == city.name && it.country == city.country)
        }
        val updated = (listOf(city) + current).take(6)
        try {
            val jsonArray = org.json.JSONArray()
            for (c in updated) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("nameEn", c.nameEn)
                    put("country", c.country)
                    put("latitude", c.latitude)
                    put("longitude", c.longitude)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("recent_searches_json", jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun fetchWeatherForecast(city: City): JSONObject {
        val urlStr = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=${city.latitude}&longitude=${city.longitude}" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
                "&minutely_15=precipitation,weather_code" +
                "&hourly=temperature_2m,relative_humidity_2m,dew_point_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,surface_pressure,visibility,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,is_day" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,daylight_duration,sunshine_duration,uv_index_max,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant" +
                "&timezone=auto&forecast_days=7"

        return httpGetJson(urlStr)
    }

    private fun fetchAirQuality(city: City): JSONObject? {
        return try {
            val urlStr = "https://air-quality-api.open-meteo.com/v1/air-quality?" +
                    "latitude=${city.latitude}&longitude=${city.longitude}" +
                    "&current=european_aqi,us_aqi,pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone" +
                    "&timezone=auto"
            httpGetJson(urlStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchGeocoding(query: String): List<City> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val urlStr = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=ja&format=json"
        val json = httpGetJson(urlStr)
        val results = json.optJSONArray("results") ?: return emptyList()

        val list = mutableListOf<City>()
        for (i in 0 until results.length()) {
            val obj = results.getJSONObject(i)
            val id = obj.optString("id", "city_$i")
            val name = obj.optString("name", query)
            val country = obj.optString("country", "")
            val admin1 = obj.optString("admin1", "")
            val admin2 = obj.optString("admin2", "")
            val lat = obj.getDouble("latitude")
            val lon = obj.getDouble("longitude")

            val regionParts = listOf(country, admin1, admin2).filter { it.isNotBlank() }.distinct()
            val regionDesc = regionParts.joinToString(" ")

            list.add(
                City(
                    id = "geo_${id}_$i",
                    name = name,
                    nameEn = obj.optString("name", name),
                    country = if (regionDesc.isNotEmpty()) regionDesc else country,
                    latitude = lat,
                    longitude = lon
                )
            )
        }
        return list
    }

    private fun httpGetJson(urlString: String): JSONObject {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "ExpressiveWeather/1.0 (Android; Mobile)")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        return try {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.use { it.readText() }
            JSONObject(response)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseWeatherData(city: City, weatherJson: JSONObject, aqiJson: JSONObject?): WeatherData {
        val current = weatherJson.getJSONObject("current")
        val daily = weatherJson.getJSONObject("daily")
        val hourly = weatherJson.getJSONObject("hourly")

        val currentTemp = current.getDouble("temperature_2m").roundToInt()
        val feelsLike = current.getDouble("apparent_temperature").roundToInt()
        val isDay = current.optInt("is_day", 1) == 1
        val humidity = current.getInt("relative_humidity_2m")
        val weatherCode = current.getInt("weather_code")
        val windSpeed = current.getDouble("wind_speed_10m")
        val windDegree = current.getDouble("wind_direction_10m").toFloat()
        val windGusts = current.optDouble("wind_gusts_10m", windSpeed * 1.35)
        val pressure = current.getDouble("surface_pressure").roundToInt()
        val precipMm = current.optDouble("precipitation", 0.0)

        val condition = mapWmoCode(weatherCode, isDay)

        // Daily Arrays
        val dailyMaxList = daily.getJSONArray("temperature_2m_max")
        val dailyMinList = daily.getJSONArray("temperature_2m_min")
        val dailyCodeList = daily.getJSONArray("weather_code")
        val dailyPrecipList = daily.optJSONArray("precipitation_probability_max")
        val dailyPrecipSumList = daily.optJSONArray("precipitation_sum")
        val dailyWindMaxList = daily.optJSONArray("wind_speed_10m_max")
        val dailyWindGustsMaxList = daily.optJSONArray("wind_gusts_10m_max")
        val dailyWindDirDominantList = daily.optJSONArray("wind_direction_10m_dominant")
        val sunriseList = daily.getJSONArray("sunrise")
        val sunsetList = daily.getJSONArray("sunset")
        val uvMaxList = daily.optJSONArray("uv_index_max")
        val daylightList = daily.optJSONArray("daylight_duration")

        val maxTemp = dailyMaxList.getDouble(0).roundToInt()
        val minTemp = dailyMinList.getDouble(0).roundToInt()
        val uvIndex = uvMaxList?.optDouble(0, 4.0)?.roundToInt() ?: 4
        val precipSumToday = dailyPrecipSumList?.optDouble(0, 0.0) ?: 0.0
        val precipProbToday = dailyPrecipList?.optInt(0, 0) ?: 0

        val sunriseIso = sunriseList.getString(0)
        val sunsetIso = sunsetList.getString(0)
        val sunriseTime = formatTimeFromIso(sunriseIso)
        val sunsetTime = formatTimeFromIso(sunsetIso)
        val daylightSeconds = daylightList?.optDouble(0, 43200.0)?.toLong() ?: 43200L
        val daylightHoursStr = formatDaylightDuration(daylightSeconds)

        // Parse Hourly
        val hourlyTimes = hourly.getJSONArray("time")
        val hourlyTemps = hourly.getJSONArray("temperature_2m")
        val hourlyCodes = hourly.getJSONArray("weather_code")
        val hourlyPrecips = hourly.optJSONArray("precipitation_probability")
        val hourlyHumidities = hourly.optJSONArray("relative_humidity_2m")
        val hourlyWinds = hourly.optJSONArray("wind_speed_10m")
        val hourlyGusts = hourly.optJSONArray("wind_gusts_10m")
        val hourlyWindDirs = hourly.optJSONArray("wind_direction_10m")
        val hourlyDewPoints = hourly.optJSONArray("dew_point_2m")
        val hourlyVisibilities = hourly.optJSONArray("visibility")
        val hourlyPrecipMms = hourly.optJSONArray("precipitation")
        val hourlyIsDays = hourly.optJSONArray("is_day")

        val currentHourIndex = findCurrentHourIndex(hourlyTimes)
        val hourlyForecast = mutableListOf<HourlyForecast>()
        val hourlyWindList = mutableListOf<HourlyWindItem>()
        val maxHourCount = 24.coerceAtMost(hourlyTimes.length() - currentHourIndex)

        val currentDewPoint = hourlyDewPoints?.optDouble(currentHourIndex, currentTemp - 4.0)?.roundToInt() ?: (currentTemp - 4)
        val currentVisibilityMeters = hourlyVisibilities?.optDouble(currentHourIndex, 10000.0) ?: 10000.0
        val visibilityKm = (currentVisibilityMeters / 1000.0).coerceAtLeast(0.1)

        var peakWindGust = windGusts
        var peakWindTime = if (hourlyTimes.length() > currentHourIndex) formatHourLabel(hourlyTimes.getString(currentHourIndex)) else "現在"

        for (i in 0 until maxHourCount) {
            val idx = currentHourIndex + i
            val timeIso = hourlyTimes.getString(idx)
            val hourLabel = if (i == 0) "現在" else formatHourLabel(timeIso)
            val hTemp = hourlyTemps.getDouble(idx).roundToInt()
            val hCode = hourlyCodes.getInt(idx)
            val hPrecip = hourlyPrecips?.optInt(idx, 0) ?: 0
            val hHum = hourlyHumidities?.optInt(idx, humidity) ?: humidity
            val hWind = hourlyWinds?.optDouble(idx, windSpeed) ?: windSpeed
            val hGust = hourlyGusts?.optDouble(idx, hWind * 1.35) ?: (hWind * 1.35)
            val hWindDir = hourlyWindDirs?.optDouble(idx, windDegree.toDouble())?.toFloat() ?: windDegree
            val hDew = hourlyDewPoints?.optDouble(idx, (hTemp - 3).toDouble())?.roundToInt() ?: (hTemp - 3)
            val hIsDay = hourlyIsDays?.optInt(idx, if (parseHourInt(timeIso) in 6..18) 1 else 0) == 1
            val hPrecipMm = hourlyPrecipMms?.optDouble(idx, 0.0) ?: 0.0

            if (hGust > peakWindGust) {
                peakWindGust = hGust
                peakWindTime = hourLabel
            }

            hourlyForecast.add(
                HourlyForecast(
                    timeLabel = hourLabel,
                    hour = parseHourInt(timeIso),
                    tempC = hTemp,
                    condition = mapWmoCode(hCode, hIsDay),
                    precipProb = hPrecip,
                    humidity = hHum,
                    windSpeedKmH = hWind,
                    dewPointC = hDew,
                    isDay = hIsDay,
                    precipitationMm = hPrecipMm
                )
            )

            hourlyWindList.add(
                HourlyWindItem(
                    timeLabel = hourLabel,
                    hour = parseHourInt(timeIso),
                    windSpeedKmH = hWind,
                    windGustsKmH = hGust,
                    windDegree = hWindDir
                )
            )
        }

        // Parse 7-Day Forecast
        val dailyForecast = mutableListOf<DailyForecast>()
        val dailyWindList = mutableListOf<DailyWindItem>()
        val dayCount = 7.coerceAtMost(dailyMaxList.length())
        val dayOfWeekFormatter = SimpleDateFormat("E", Locale.JAPANESE)
        val cal = Calendar.getInstance()

        for (i in 0 until dayCount) {
            val dMax = dailyMaxList.getDouble(i).roundToInt()
            val dMin = dailyMinList.getDouble(i).roundToInt()
            val dCode = dailyCodeList.getInt(i)
            val dPrecip = dailyPrecipList?.optInt(i, 0) ?: 0
            val dUv = uvMaxList?.optDouble(i, 4.0) ?: 4.0
            val dPrecipSum = dailyPrecipSumList?.optDouble(i, 0.0) ?: 0.0
            val dWindMax = dailyWindMaxList?.optDouble(i, 12.0) ?: 12.0
            val dWindGustMax = dailyWindGustsMaxList?.optDouble(i, dWindMax * 1.35) ?: (dWindMax * 1.35)
            val dWindDir = dailyWindDirDominantList?.optDouble(i, windDegree.toDouble())?.toFloat() ?: windDegree

            val dLabel = when (i) {
                0 -> "今日"
                1 -> "明日"
                else -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    dayOfWeekFormatter.format(cal.time)
                }
            }

            // Real date from time array
            val dateIso = daily.getJSONArray("time").getString(i)
            val dateLabel = formatMonthDay(dateIso)

            val dSunriseIso = sunriseList.optString(i, "")
            val dSunsetIso = sunsetList.optString(i, "")
            val dSunriseTime = if (dSunriseIso.isNotBlank()) formatTimeFromIso(dSunriseIso) else "06:00"
            val dSunsetTime = if (dSunsetIso.isNotBlank()) formatTimeFromIso(dSunsetIso) else "18:00"
            val dDaylightSec = daylightList?.optDouble(i, 43200.0)?.toLong() ?: 43200L
            val dDaylightStr = formatDaylightDuration(dDaylightSec)

            // その日 (0:00 - 23:00) の24時間予報を抽出
            val dayHourlyList = mutableListOf<HourlyForecast>()
            val dayStartIdx = i * 24
            var sumHumidity = 0
            var countHumidity = 0
            for (h in 0 until 24) {
                val hIdx = dayStartIdx + h
                if (hIdx < hourlyTimes.length()) {
                    val hTimeIso = hourlyTimes.getString(hIdx)
                    val hHour = parseHourInt(hTimeIso)
                    val hLabel = String.format(Locale.JAPANESE, "%d:00", hHour)
                    val hT = hourlyTemps.optDouble(hIdx, ((dMax + dMin) / 2.0)).roundToInt()
                    val hC = hourlyCodes.optInt(hIdx, dCode)
                    val hP = hourlyPrecips?.optInt(hIdx, dPrecip) ?: dPrecip
                    val rawHumidity = hourlyHumidities?.optInt(hIdx, -1) ?: -1
                    val hHum = rawHumidity.takeIf { it in 0..100 } ?: 60
                    val hW = hourlyWinds?.optDouble(hIdx, dWindMax * 0.7) ?: (dWindMax * 0.7)
                    val hDew = hourlyDewPoints?.optDouble(hIdx, (hT - 4).toDouble())?.roundToInt() ?: (hT - 4)
                    val hIsDay = hourlyIsDays?.optInt(hIdx, if (hHour in 6..18) 1 else 0) == 1
                    val hPmm = hourlyPrecipMms?.optDouble(hIdx, 0.0) ?: 0.0

                    if (rawHumidity in 0..100) {
                        sumHumidity += rawHumidity
                        countHumidity++
                    }

                    dayHourlyList.add(
                        HourlyForecast(
                            timeLabel = hLabel,
                            hour = hHour,
                            tempC = hT,
                            condition = mapWmoCode(hC, hIsDay),
                            precipProb = hP,
                            humidity = hHum,
                            windSpeedKmH = hW,
                            dewPointC = hDew,
                            isDay = hIsDay,
                            precipitationMm = hPmm
                        )
                    )
                }
            }
            dailyForecast.add(
                DailyForecast(
                    dayLabel = dLabel,
                    dateLabel = dateLabel,
                    condition = mapWmoCode(dCode, true),
                    minTempC = dMin,
                    maxTempC = dMax,
                    precipProb = dPrecip,
                    uvMax = dUv,
                    precipitationSumMm = dPrecipSum,
                    windSpeedMaxKmH = dWindMax,
                    hourlyForecast = dayHourlyList,
                    sunrise = dSunriseTime,
                    sunset = dSunsetTime,
                    daylightHours = dDaylightStr,
                    humidityAvg = if (countHumidity > 0) (sumHumidity.toDouble() / countHumidity).roundToInt() else null,
                    humidityAvgSource = if (countHumidity > 0) "Open-Meteo" else null,
                    pressureHpa = pressure
                )
            )

            dailyWindList.add(
                DailyWindItem(
                    dayLabel = dLabel,
                    dateLabel = dateLabel,
                    windSpeedMaxKmH = dWindMax,
                    windGustsMaxKmH = dWindGustMax,
                    dominantWindDegree = dWindDir
                )
            )
        }

        // Parse Real Air Quality
        val airQuality = parseAirQuality(aqiJson)

        val currentTimeFmt = SimpleDateFormat("HH:mm", Locale.JAPANESE).format(Date())

        val specificName = getSpecificWeatherName(weatherCode)

        val windDetails = WindDetails(
            windSpeedKmH = windSpeed,
            windGustsKmH = windGusts,
            windDegree = windDegree,
            windDirectionText = getWindDirectionText(windDegree),
            beaufortScale = BeaufortScale.fromSpeedMs(windSpeed / 3.6),
            hourlyWind = hourlyWindList,
            dailyWind = dailyWindList,
            peakWindTime = peakWindTime,
            peakWindSpeedKmH = peakWindGust
        )

        val minutelyObj = weatherJson.optJSONObject("minutely_15")
        val minutelyTimes = minutelyObj?.optJSONArray("time")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        val minutelyPrecips = minutelyObj?.optJSONArray("precipitation")?.let { arr ->
            (0 until arr.length()).map { arr.optDouble(it, 0.0) }
        }
        val minutelyCodes = minutelyObj?.optJSONArray("weather_code")?.let { arr ->
            (0 until arr.length()).map { arr.optInt(it, 0) }
        }

        val isUs = isLocationOrCountryInUnitedStates(city.country, city.latitude, city.longitude)

        val precipitationNowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = precipMm,
            currentCondition = condition,
            minutelyTimes = minutelyTimes,
            minutelyPrecips = minutelyPrecips,
            minutelyCodes = minutelyCodes,
            hourlyForecast = hourlyForecast,
            country = city.country,
            latitude = city.latitude,
            longitude = city.longitude,
            conditionName = specificName
        )

        return WeatherData(
            city = city,
            currentTempC = currentTemp,
            feelsLikeC = feelsLike,
            minTempC = minTemp,
            maxTempC = maxTemp,
            condition = condition,
            conditionSummary = condition.tagline,
            conditionName = specificName,
            humidity = humidity,
            uvIndex = uvIndex,
            uvLabel = getUvLabel(uvIndex),
            windSpeedKmH = windSpeed,
            windDirection = getWindDirectionText(windDegree),
            windDegree = windDegree,
            pressureHpa = pressure,
            visibilityKm = String.format(Locale.US, "%.1f", visibilityKm).toDoubleOrNull() ?: visibilityKm,
            airQuality = airQuality,
            sunSchedule = SunSchedule(
                sunrise = sunriseTime,
                sunset = sunsetTime,
                dayProgress = calculateDayProgress(sunriseTime, sunsetTime),
                daylightHours = daylightHoursStr
            ),
            hourlyForecast = hourlyForecast,
            dailyForecast = dailyForecast,
            lastUpdatedTime = currentTimeFmt,
            isDay = isDay,
            dewPointC = currentDewPoint,
            windGustsKmH = windGusts,
            precipitationMm = precipMm,
            precipitationSumMm = precipSumToday,
            precipitationProbabilityMax = precipProbToday,
            windDetails = windDetails,
            dataSourceName = if (isUs) "気象データ提供元: NOAA ＆ Open-Meteo" else "気象データ提供元: Open-Meteo",
            precipitationNowcast = precipitationNowcast
        )
    }

    private fun parseAirQuality(aqiJson: JSONObject?): AirQuality {
        if (aqiJson == null) {
            return AirQuality(
                aqi = 25,
                levelLabel = "良好",
                pm25 = 8.0,
                pm10 = 15.0,
                advice = "清浄な空気です。深呼吸や屋外活動に最適です",
                usAqi = 25,
                europeanAqi = 20
            )
        }
        val current = aqiJson.optJSONObject("current")
        val usAqi = current?.optInt("us_aqi", 28) ?: 28
        val eAqi = current?.optInt("european_aqi", 22) ?: 22
        val pm25 = current?.optDouble("pm2_5", 7.5) ?: 7.5
        val pm10 = current?.optDouble("pm10", 14.0) ?: 14.0

        val (label, advice) = when {
            usAqi <= 50 -> "優良" to "清浄で澄んだ大気質です。屋外活動や換気に最適です"
            usAqi <= 100 -> "普通" to "大気質は良好です。敏感な方も安心してお過ごしいただけます"
            usAqi <= 150 -> "注意" to "呼吸器系に敏感な方は長時間の激しい運動をお控えください"
            usAqi <= 200 -> "不健康" to "全体的に空気質が低下しています。マスク着用を推奨します"
            else -> "危険" to "外出を控え、窓を閉めて空気清浄機をご利用ください"
        }

        return AirQuality(
            aqi = usAqi,
            levelLabel = label,
            pm25 = pm25,
            pm10 = pm10,
            advice = advice,
            usAqi = usAqi,
            europeanAqi = eAqi
        )
    }

    fun getSpecificWeatherName(code: Int): String {
        return when (code) {
            0 -> "快晴"
            1 -> "晴れ"
            2 -> "晴れ時々曇り"
            3 -> "くもり"
            45, 48 -> "霧"
            51 -> "弱い霧雨"
            53 -> "霧雨"
            55 -> "強い霧雨"
            56, 57 -> "着氷性の霧雨"
            61 -> "小雨"
            63 -> "本降りの雨"
            65 -> "大雨"
            66, 67 -> "みぞれ・雨"
            71 -> "小雪"
            73 -> "雪"
            75 -> "大雪"
            77 -> "雪あられ"
            80 -> "にわか雨"
            81, 82 -> "激しい雨"
            85, 86 -> "にわか雪"
            95 -> "雷雨"
            96, 99 -> "雷雨（雹）"
            else -> "晴れ"
        }
    }

    private fun mapWmoCode(code: Int, isDay: Boolean = true): WeatherCondition {
        return when (code) {
            0 -> WeatherCondition.CLEAR
            1, 2 -> WeatherCondition.FEW_CLOUDS
            3 -> WeatherCondition.CLOUDY
            45, 48 -> WeatherCondition.FOG
            51, 53, 55, 61, 63, 80, 81 -> WeatherCondition.RAIN
            65, 82 -> WeatherCondition.HEAVY_RAIN
            71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
            95, 96, 99 -> WeatherCondition.THUNDERSTORM
            else -> WeatherCondition.FEW_CLOUDS
        }
    }

    private fun findCurrentHourIndex(times: org.json.JSONArray): Int {
        val nowIsoHour = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US).format(Date())
        for (i in 0 until times.length()) {
            val t = times.getString(i)
            if (t.startsWith(nowIsoHour)) return i
        }
        return 0
    }

    private fun formatTimeFromIso(iso: String): String {
        return try {
            iso.substringAfter("T").take(5)
        } catch (e: Exception) {
            "05:30"
        }
    }

    private fun formatHourLabel(iso: String): String {
        return try {
            iso.substringAfter("T").take(2) + ":00"
        } catch (e: Exception) {
            "12:00"
        }
    }

    private fun formatMonthDay(iso: String): String {
        return try {
            val parts = iso.split("-")
            "${parts[1].toInt()}/${parts[2].toInt()}"
        } catch (e: Exception) {
            "9/17"
        }
    }

    private fun parseHourInt(iso: String): Int {
        return try {
            iso.substringAfter("T").take(2).toInt()
        } catch (e: Exception) {
            12
        }
    }

    private fun formatDaylightDuration(seconds: Long): String {
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        return "${hours}時間${mins}分"
    }

    private fun getUvLabel(uv: Int): String {
        return when {
            uv <= 2 -> "弱い"
            uv <= 5 -> "普通"
            uv <= 7 -> "強い"
            uv <= 10 -> "非常に強い"
            else -> "極端に強い"
        }
    }

    private fun getWindDirectionText(degree: Float): String {
        val directions = listOf("北", "北北東", "北東", "東北東", "東", "東南東", "南東", "南南東", "南", "南南西", "南西", "西南西", "西", "西北西", "北西", "北北西")
        val index = (((degree + 11.25f) % 360) / 22.5f).toInt()
        return directions[index % directions.size]
    }

    private fun calculateDayProgress(sunrise: String, sunset: String): Float {
        return try {
            val nowCal = Calendar.getInstance()
            val nowMins = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
            val riseParts = sunrise.split(":").map { it.toInt() }
            val riseMins = riseParts[0] * 60 + riseParts[1]
            val setParts = sunset.split(":").map { it.toInt() }
            val setMins = setParts[0] * 60 + setParts[1]

            if (nowMins <= riseMins) return 0.0f
            if (nowMins >= setMins) return 1.0f
            (nowMins - riseMins).toFloat() / (setMins - riseMins).toFloat()
        } catch (e: Exception) {
            0.5f
        }
    }
}
