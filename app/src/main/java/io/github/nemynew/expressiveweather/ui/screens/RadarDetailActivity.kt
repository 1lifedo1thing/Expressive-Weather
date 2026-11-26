package io.github.nemynew.expressiveweather.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.radar.RadarDataHolder
import io.github.nemynew.expressiveweather.ui.radar.RadarDetailScreen

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

class RadarDetailActivity : ComponentActivity() {

    private val activeLayerIdState = mutableStateOf<String?>(null)
    private val cityNameState = mutableStateOf("")
    private val latitudeState = mutableStateOf(35.6895)
    private val longitudeState = mutableStateOf(139.6917)
    private val countryState = mutableStateOf<String?>(null)

    private fun isValidCoordinate(lat: Double, lon: Double): Boolean =
        lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0

    private fun applyLocationFromIntent(intent: Intent, repo: WeatherRepository) {
        val intentCityJson = intent.getStringExtra(WeatherRepository.EXTRA_CITY_JSON)
        val decodedIntentCity: io.github.nemynew.expressiveweather.data.City? = intentCityJson?.takeIf { it.isNotBlank() }?.let { raw ->
            try {
                io.github.nemynew.expressiveweather.data.WeatherJsonSerializer.json.decodeFromString<io.github.nemynew.expressiveweather.data.City>(raw)
            } catch (_: Exception) {
                null
            }
        }?.takeIf { isValidCoordinate(it.latitude, it.longitude) }
        val hasHolder = RadarDataHolder.city != null
        val fallbackCity = decodedIntentCity
            ?: (if (hasHolder) RadarDataHolder.city else null)
            ?: repo.resolveFallbackCurrentCity()
        val language = if (hasHolder) RadarDataHolder.language else repo.getLanguage()
        val defaultCurrentLocation = when (language) {
            io.github.nemynew.expressiveweather.data.AppLanguage.JAPANESE -> "現在地"
            io.github.nemynew.expressiveweather.data.AppLanguage.KOREAN -> "현재 위치"
            io.github.nemynew.expressiveweather.data.AppLanguage.ENGLISH -> "Current Location"
            io.github.nemynew.expressiveweather.data.AppLanguage.SIMPLIFIED_CHINESE -> "当前位置"
            io.github.nemynew.expressiveweather.data.AppLanguage.TRADITIONAL_CHINESE -> "當前位置"
            io.github.nemynew.expressiveweather.data.AppLanguage.SPANISH -> "Ubicación actual"
            io.github.nemynew.expressiveweather.data.AppLanguage.FRENCH -> "Position actuelle"
            io.github.nemynew.expressiveweather.data.AppLanguage.PORTUGUESE -> "Localização atual"
        }

        val intentCityName = intent.getStringExtra("city_name")?.takeIf { it.isNotBlank() }
        cityNameState.value = intentCityName
            ?: decodedIntentCity?.name?.takeIf { it.isNotBlank() }
            ?: (if (hasHolder) RadarDataHolder.cityName.takeIf { it.isNotBlank() } else null)
            ?: fallbackCity.name.takeIf { it.isNotBlank() }
            ?: defaultCurrentLocation

        val intentHasCoords = intent.hasExtra("extra_lat") && intent.hasExtra("extra_lon")
        val intentLat = if (intentHasCoords) intent.getDoubleExtra("extra_lat", Double.NaN) else Double.NaN
        val intentLon = if (intentHasCoords) intent.getDoubleExtra("extra_lon", Double.NaN) else Double.NaN
        val (resolvedLat, resolvedLon) = when {
            isValidCoordinate(intentLat, intentLon) -> intentLat to intentLon
            decodedIntentCity != null -> decodedIntentCity.latitude to decodedIntentCity.longitude
            hasHolder && isValidCoordinate(RadarDataHolder.latitude, RadarDataHolder.longitude) ->
                RadarDataHolder.latitude to RadarDataHolder.longitude
            else -> fallbackCity.latitude to fallbackCity.longitude
        }
        latitudeState.value = resolvedLat
        longitudeState.value = resolvedLon

        val intentCountry = intent.getStringExtra("country")?.takeIf { it.isNotBlank() }
        countryState.value = intentCountry
            ?: decodedIntentCity?.country?.takeIf { it.isNotBlank() }
            ?: (if (hasHolder) RadarDataHolder.country else null)
            ?: fallbackCity.country
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(io.github.nemynew.expressiveweather.util.LocaleUtils.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        io.github.nemynew.expressiveweather.util.LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        activeLayerIdState.value = intent.getStringExtra("initial_layer_id")

        val repo = WeatherRepository(context = applicationContext)
        val hasHolder = RadarDataHolder.city != null
        applyLocationFromIntent(intent, repo)
        val fallbackCity = (if (hasHolder) RadarDataHolder.city else null)
            ?: WeatherRepository.decodeCityJsonOrDefault(
                intent.getStringExtra(WeatherRepository.EXTRA_CITY_JSON),
                repo
            )
        val language = if (hasHolder) RadarDataHolder.language else repo.getLanguage()
        val weatherAdaptiveThemeEnabled = if (hasHolder) RadarDataHolder.weatherAdaptiveThemeEnabled else repo.isWeatherAdaptiveThemeEnabled()
        val themePreset = if (hasHolder) RadarDataHolder.themePreset else repo.getThemePreset()
        val themeMode = if (hasHolder) RadarDataHolder.themeMode else repo.getThemeMode()
        val accentColorMode = repo.getAccentColorMode()
        val isDay = if (hasHolder) RadarDataHolder.isDay else (repo.getMemoryCachedWeatherOrNull(fallbackCity)?.isDay ?: true)
        val showZoomBadge = if (hasHolder) RadarDataHolder.isRadarDebugZoomEnabled else repo.getRadarDebugZoomEnabled()
        val showAllJapanLayers = if (hasHolder) RadarDataHolder.showAllJapanRadarLayers else repo.getShowAllJapanRadarLayers()
        val weatherCondition = if (hasHolder) RadarDataHolder.weatherCondition else repo.getMemoryCachedWeatherOrNull(fallbackCity)?.condition

        setContent {
            val isSystemDark = isSystemInDarkTheme()
            val isDark = ThemeMode.resolveIsDark(
                themeMode = themeMode,
                isDay = isDay,
                isSystemDark = isSystemDark
            )
            val currentLayerId by activeLayerIdState
            val currentLat by latitudeState
            val currentLon by longitudeState
            val currentCityName by cityNameState
            val currentCountry by countryState

            ExpressiveWeatherTheme(
                darkTheme = isDark,
                dynamicColor = (themePreset == ThemePreset.SYSTEM),
                weatherCondition = weatherCondition,
                weatherAdaptiveColor = weatherAdaptiveThemeEnabled,
                themePreset = themePreset,
                accentColorMode = accentColorMode,
                language = language
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RadarDetailScreen(
                        latitude = currentLat,
                        longitude = currentLon,
                        cityName = currentCityName,
                        country = currentCountry,
                        showZoomBadge = showZoomBadge,
                        showAllJapanLayers = showAllJapanLayers,
                        onBackClick = { finish() },
                        initialLayerId = currentLayerId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val repo = WeatherRepository(context = applicationContext)
        applyLocationFromIntent(intent, repo)
        val newLayerId = intent.getStringExtra("initial_layer_id")
        if (!newLayerId.isNullOrBlank()) {
            activeLayerIdState.value = newLayerId
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            RadarDataHolder.clear()
        }
    }
}
