package io.github.nemynew.expressiveweather.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.nemynew.expressiveweather.data.CurrentLocationProvider
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.local.WeatherDatabase
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset

/**
 * セットアップ画面（SetupScreen）を独立して起動するための Activity。
 * 設定画面の「初期セットアップを再表示」や、外部Intentからの直接プレビューに使用。
 */
class SetupActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(io.github.nemynew.expressiveweather.util.LocaleUtils.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        io.github.nemynew.expressiveweather.util.LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        val database = WeatherDatabase.getInstance(applicationContext)
        val repository = WeatherRepository(
            weatherDao = database.weatherDao(),
            savedCityDao = database.savedCityDao(),
            context = applicationContext
        )
        val locationProvider = CurrentLocationProvider(applicationContext)
        val initialLanguage = repository.getLanguage()
        val themeMode = repository.getThemeMode()
        val themePreset = repository.getThemePreset()
        val weatherAdaptiveThemeEnabled = repository.isWeatherAdaptiveThemeEnabled()
        val lastCity = repository.resolveFallbackCurrentCity()
        val cachedWeather = repository.getMemoryCachedWeatherOrNull(lastCity)

        setContent {
            var activeLanguage by remember { mutableStateOf(initialLanguage) }
            val isSystemDark = isSystemInDarkTheme()
            val isDark = ThemeMode.resolveIsDark(
                themeMode = themeMode,
                isDay = cachedWeather?.isDay ?: !isSystemDark,
                isSystemDark = isSystemDark
            )

            ExpressiveWeatherTheme(
                darkTheme = isDark,
                dynamicColor = (themePreset == ThemePreset.SYSTEM),
                weatherCondition = cachedWeather?.condition,
                weatherAdaptiveColor = weatherAdaptiveThemeEnabled,
                themePreset = themePreset,
                language = activeLanguage
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SetupScreen(
                        repository = repository,
                        locationProvider = locationProvider,
                        isRerun = true,
                        onDismiss = { finish() },
                        onLanguageChanged = { newLang ->
                            activeLanguage = newLang
                            io.github.nemynew.expressiveweather.util.LocaleUtils.applyLocaleToActivity(this@SetupActivity)
                        },
                        onSetupComplete = { city ->
                            WeatherRepository.notifyExternalCitySelected(city)
                            setResult(RESULT_OK)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
