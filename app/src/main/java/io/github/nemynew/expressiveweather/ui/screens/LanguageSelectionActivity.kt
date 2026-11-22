package io.github.nemynew.expressiveweather.ui.screens

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.util.LocaleUtils

/**
 * 言語選択専用の独立した Activity。
 * ネイティブな新画面として起動され、Android OS 標準の右からのスライドイン画面遷移、
 * および Android 14/15 の予測型戻る（Predictive Back）ジェスチャーアニメーションに対応。
 */
class LanguageSelectionActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        val weatherRepo = WeatherRepository(context = applicationContext)
        val initialThemeMode = weatherRepo.getThemeMode()
        val initialThemePreset = weatherRepo.getThemePreset()
        val initialWeather = SettingsDataHolder.weatherData ?: weatherRepo.getMemoryCachedWeatherOrNull(weatherRepo.getLastSelectedCity())

        setContent {
            val isSystemDark = isSystemInDarkTheme()
            val isDark = ThemeMode.resolveIsDark(
                themeMode = initialThemeMode,
                isDay = initialWeather?.isDay ?: true,
                isSystemDark = isSystemDark
            )
            var currentLang by remember { mutableStateOf(weatherRepo.getLanguage()) }

            ExpressiveWeatherTheme(
                darkTheme = isDark,
                weatherCondition = if (initialThemePreset == ThemePreset.WEATHER) {
                    initialWeather?.condition
                } else null,
                weatherAdaptiveColor = initialThemePreset == ThemePreset.WEATHER,
                themePreset = initialThemePreset,
                language = currentLang
            ) {
                CompositionLocalProvider(
                    LocalAppLanguage provides currentLang,
                    LocalAppStrings provides currentLang.strings
                ) {
                    LanguageSelectionScreen(
                        currentLanguage = currentLang,
                        onSelectLanguage = { newLang ->
                            currentLang = newLang
                            SettingsDataHolder.language = newLang
                            weatherRepo.setLanguage(newLang)
                            SettingsDataHolder.onSelectLanguage?.invoke(newLang)
                            LocaleUtils.applyLocaleToActivity(this@LanguageSelectionActivity)
                        },
                        onBack = { finish() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
