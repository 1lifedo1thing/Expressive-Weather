package com.example.expressiveweather.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.expressiveweather.data.WeatherRepository
import com.example.expressiveweather.theme.ExpressiveWeatherTheme
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset
import com.example.expressiveweather.ui.radar.RadarDataHolder
import com.example.expressiveweather.ui.radar.RadarDetailScreen

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

class RadarDetailActivity : ComponentActivity() {

    private val activeLayerIdState = mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.example.expressiveweather.util.LocaleUtils.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.expressiveweather.util.LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        activeLayerIdState.value = intent.getStringExtra("initial_layer_id")

        val repo = WeatherRepository(context = applicationContext)
        val language = RadarDataHolder.language
        val defaultCurrentLocation = when (language) {
            com.example.expressiveweather.data.AppLanguage.JAPANESE -> "現在地"
            com.example.expressiveweather.data.AppLanguage.ENGLISH -> "Current Location"
            com.example.expressiveweather.data.AppLanguage.SIMPLIFIED_CHINESE -> "当前位置"
            com.example.expressiveweather.data.AppLanguage.TRADITIONAL_CHINESE -> "當前位置"
        }
        val intentCityName = intent.getStringExtra("city_name")
        val cityName = intentCityName?.takeIf { it.isNotBlank() }
            ?: RadarDataHolder.cityName.takeIf { it.isNotBlank() }
            ?: defaultCurrentLocation
        val latitude = if (RadarDataHolder.latitude != 0.0) RadarDataHolder.latitude else 35.6812
        val longitude = if (RadarDataHolder.longitude != 0.0) RadarDataHolder.longitude else 139.7671
        val country = RadarDataHolder.country ?: RadarDataHolder.city?.country ?: ""
        val weatherAdaptiveThemeEnabled = if (RadarDataHolder.city != null) RadarDataHolder.weatherAdaptiveThemeEnabled else repo.isWeatherAdaptiveThemeEnabled()
        val themePreset = if (RadarDataHolder.city != null) RadarDataHolder.themePreset else repo.getThemePreset()
        val themeMode = if (RadarDataHolder.city != null) RadarDataHolder.themeMode else repo.getThemeMode()
        val isDay = RadarDataHolder.isDay
        val showZoomBadge = RadarDataHolder.isRadarDebugZoomEnabled
        val weatherCondition = RadarDataHolder.weatherCondition

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
