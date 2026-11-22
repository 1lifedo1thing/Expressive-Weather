package io.github.nemynew.expressiveweather.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.nemynew.expressiveweather.data.AlertSource
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.local.WeatherDatabase
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.ThemeMode

/**
 * 全気象警報・防災情報専用の独立した Activity。
 * ネイティブな新ページとして起動され、Android OS 標準の Activity 遷移アニメーション
 * および Android 14/15 予測型戻るジェスチャー（Predictive Back）に完全対応。
 */
class AlertDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SOURCE = "extra_alert_source"
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(io.github.nemynew.expressiveweather.util.LocaleUtils.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        io.github.nemynew.expressiveweather.util.LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        val db = WeatherDatabase.getInstance(applicationContext)
        val repo = WeatherRepository(
            weatherDao = db.weatherDao(),
            savedCityDao = db.savedCityDao(),
            context = applicationContext
        )
        val hasHolder = AlertDetailDataHolder.weatherData != null
        val sourceParam = intent.getStringExtra(EXTRA_SOURCE)?.let {
            runCatching { AlertSource.valueOf(it) }.getOrNull()
        } ?: AlertDetailDataHolder.targetSource

        val city = AlertDetailDataHolder.city ?: WeatherRepository.decodeCityJsonOrDefault(
            intent.getStringExtra(WeatherRepository.EXTRA_CITY_JSON),
            repo
        )
        val language = if (hasHolder) AlertDetailDataHolder.language else repo.getLanguage()
        val weatherAdaptiveThemeEnabled = if (hasHolder) AlertDetailDataHolder.weatherAdaptiveThemeEnabled else repo.isWeatherAdaptiveThemeEnabled()
        val themePreset = if (hasHolder) AlertDetailDataHolder.themePreset else repo.getThemePreset()
        val themeMode = if (hasHolder) AlertDetailDataHolder.themeMode else repo.getThemeMode()
        val initialWeather = AlertDetailDataHolder.weatherData ?: repo.getMemoryCachedWeatherOrNull(city)
        val holderAlertInfo = AlertDetailDataHolder.alertInfo
        val holderJmaWarnings = AlertDetailDataHolder.jmaWarnings
        val holderJmaOverviewText = AlertDetailDataHolder.jmaOverviewText
        val holderGlobalAlerts = AlertDetailDataHolder.globalAlerts

        setContent {
            var weatherData by remember { mutableStateOf(initialWeather) }
            LaunchedEffect(city) {
                if (weatherData == null) {
                    weatherData = repo.getCachedWeatherOrNull(city)
                        ?: runCatching { repo.getWeatherData(city) }.getOrNull()
                }
            }
            val alertInfo = holderAlertInfo ?: weatherData?.alertInfo
            val jmaWarnings = holderJmaWarnings.ifEmpty { weatherData?.jmaWarnings.orEmpty() }
            val jmaOverviewText = holderJmaOverviewText ?: weatherData?.jmaOverviewText
            val globalAlerts = holderGlobalAlerts.ifEmpty { weatherData?.globalAlerts.orEmpty() }
            val isSystemDark = isSystemInDarkTheme()
            val isDark = ThemeMode.resolveIsDark(themeMode, weatherData?.isDay ?: true, isSystemDark)

            ExpressiveWeatherTheme(
                darkTheme = isDark,
                weatherCondition = weatherData?.condition,
                weatherAdaptiveColor = weatherAdaptiveThemeEnabled,
                themePreset = themePreset,
                language = language
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlertDetailScreen(
                        weather = weatherData,
                        city = city,
                        language = language,
                        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                        themePreset = themePreset,
                        themeMode = themeMode,
                        alertInfo = alertInfo,
                        jmaWarnings = jmaWarnings,
                        jmaOverviewText = jmaOverviewText,
                        globalAlerts = globalAlerts,
                        initialSource = sourceParam,
                        onBack = { finish() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            AlertDetailDataHolder.clear()
        }
    }
}
