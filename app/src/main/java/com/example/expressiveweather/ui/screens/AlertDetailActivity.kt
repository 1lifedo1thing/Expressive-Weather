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
import com.example.expressiveweather.data.AlertSource
import com.example.expressiveweather.data.WeatherRepository
import com.example.expressiveweather.theme.ExpressiveWeatherTheme
import com.example.expressiveweather.theme.ThemeMode

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
        super.attachBaseContext(com.example.expressiveweather.util.LocaleUtils.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.expressiveweather.util.LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        val sourceParam = intent.getStringExtra(EXTRA_SOURCE)?.let {
            runCatching { AlertSource.valueOf(it) }.getOrNull()
        } ?: AlertDetailDataHolder.targetSource

        val weatherData = AlertDetailDataHolder.weatherData
        val city = AlertDetailDataHolder.city ?: WeatherRepository().defaultCity
        val language = AlertDetailDataHolder.language
        val weatherAdaptiveThemeEnabled = AlertDetailDataHolder.weatherAdaptiveThemeEnabled
        val themePreset = AlertDetailDataHolder.themePreset
        val themeMode = AlertDetailDataHolder.themeMode
        val alertInfo = AlertDetailDataHolder.alertInfo
        val jmaWarnings = AlertDetailDataHolder.jmaWarnings
        val jmaOverviewText = AlertDetailDataHolder.jmaOverviewText
        val globalAlerts = AlertDetailDataHolder.globalAlerts

        setContent {
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
