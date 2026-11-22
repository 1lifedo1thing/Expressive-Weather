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
        val fallbackCity = RadarDataHolder.city ?: WeatherRepository.decodeCityJsonOrDefault(
            intent.getStringExtra(WeatherRepository.EXTRA_CITY_JSON),
            repo
        )
        val language = if (hasHolder) RadarDataHolder.language else repo.getLanguage()
        val defaultCurrentLocation = when (language) {
            io.github.nemynew.expressiveweather.data.AppLanguage.JAPANESE -> "現在地"
            io.github.nemynew.expressiveweather.data.AppLanguage.ENGLISH -> "Current Location"
            io.github.nemynew.expressiveweather.data.AppLanguage.SIMPLIFIED_CHINESE -> "当前位置"
            io.github.nemynew.expressiveweather.data.AppLanguage.TRADITIONAL_CHINESE -> "當前位置"
        }
        val intentCityName = intent.getStringExtra("city_name")
        val cityName = intentCityName?.takeIf { it.isNotBlank() }
            ?: RadarDataHolder.cityName.takeIf { it.isNotBlank() }
            ?: fallbackCity.name.takeIf { it.isNotBlank() }
            ?: defaultCurrentLocation
        val latitude = when {
            intent.hasExtra("extra_lat") -> intent.getDoubleExtra("extra_lat", fallbackCity.latitude)
            hasHolder && RadarDataHolder.latitude.isFinite() -> RadarDataHolder.latitude
            else -> fallbackCity.latitude
        }
        val longitude = when {
            intent.hasExtra("extra_lon") -> intent.getDoubleExtra("extra_lon", fallbackCity.longitude)
            hasHolder && RadarDataHolder.longitude.isFinite() -> RadarDataHolder.longitude
            else -> fallbackCity.longitude
        }
        val country = RadarDataHolder.country ?: fallbackCity.country
        val weatherAdaptiveThemeEnabled = if (hasHolder) RadarDataHolder.weatherAdaptiveThemeEnabled else repo.isWeatherAdaptiveThemeEnabled()
        val themePreset = if (hasHolder) RadarDataHolder.themePreset else repo.getThemePreset()
        val themeMode = if (hasHolder) RadarDataHolder.themeMode else repo.getThemeMode()
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

            ExpressiveWeatherTheme(
                darkTheme = isDark,
                dynamicColor = (themePreset == ThemePreset.SYSTEM),
                weatherCondition = weatherCondition,
                weatherAdaptiveColor = weatherAdaptiveThemeEnabled,
                themePreset = themePreset,
                language = language
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RadarDetailScreen(
                        latitude = latitude,
                        longitude = longitude,
                        cityName = cityName,
                        country = country,
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
