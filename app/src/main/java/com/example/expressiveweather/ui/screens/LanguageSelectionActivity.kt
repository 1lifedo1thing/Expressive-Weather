package com.example.expressiveweather.ui.screens

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
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.LocalAppStrings
import com.example.expressiveweather.data.WeatherRepository
import com.example.expressiveweather.theme.ExpressiveWeatherTheme
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset
import com.example.expressiveweather.util.LocaleUtils

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

        setContent {
            val isSystemDark = isSystemInDarkTheme()
            val isDark = ThemeMode.resolveIsDark(
                themeMode = SettingsDataHolder.themeMode,
                isDay = SettingsDataHolder.weatherData?.isDay ?: true,
                isSystemDark = isSystemDark
            )
            var currentLang by remember { mutableStateOf(SettingsDataHolder.language) }

            ExpressiveWeatherTheme(
                darkTheme = isDark,
                weatherCondition = if (SettingsDataHolder.themePreset == ThemePreset.WEATHER) {
                    SettingsDataHolder.weatherData?.condition
                } else null,
                weatherAdaptiveColor = SettingsDataHolder.themePreset == ThemePreset.WEATHER,
                themePreset = SettingsDataHolder.themePreset,
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
                            SettingsDataHolder.onSelectLanguage?.invoke(newLang)
                            weatherRepo.setLanguage(newLang)
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
