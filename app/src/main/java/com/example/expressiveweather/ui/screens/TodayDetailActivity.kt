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

/**
 * 今日の天気詳細専用の独立した Activity。
 * ネイティブな新ページとして起動され、Android OS 標準の Activity 遷移アニメーション
 * および Android 14/15 予測型戻るジェスチャーに完全対応。
 */
class TodayDetailActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.example.expressiveweather.util.LocaleUtils.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.expressiveweather.util.LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        val weatherData = TodayDetailDataHolder.weatherData
        val city = TodayDetailDataHolder.city ?: WeatherRepository().defaultCity
        val language = TodayDetailDataHolder.language
        val temperatureUnit = TodayDetailDataHolder.temperatureUnit
        val windSpeedUnit = TodayDetailDataHolder.windSpeedUnit
        val initialSelectedDay = TodayDetailDataHolder.initialSelectedDay
        val initialSelectedHour = TodayDetailDataHolder.initialSelectedHour
        val weatherAdaptiveThemeEnabled = TodayDetailDataHolder.weatherAdaptiveThemeEnabled
        val themePreset = TodayDetailDataHolder.themePreset
        val themeMode = TodayDetailDataHolder.themeMode

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
                    TodayDetailScreen(
                        weather = weatherData,
                        city = city,
                        language = language,
                        temperatureUnit = temperatureUnit,
                        windSpeedUnit = windSpeedUnit,
                        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                        themePreset = themePreset,
                        themeMode = themeMode,
                        onBack = {
                            finish()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            TodayDetailDataHolder.clear()
        }
    }
}
