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
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.local.WeatherDatabase
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.ThemeMode

/**
 * 風詳細専用の独立した Activity。
 * ネイティブな新ページとして起動され、Android OS 標準の Activity 遷移アニメーション
 * および Android 14/15 予測型戻るジェスチャーに完全対応。
 */
class WindDetailActivity : ComponentActivity() {

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
        val hasHolder = WindDetailDataHolder.weatherData != null || WindDetailDataHolder.city != null
        val city = WindDetailDataHolder.city ?: WeatherRepository.decodeCityJsonOrDefault(
            intent.getStringExtra(WeatherRepository.EXTRA_CITY_JSON),
            repo
        )
        val language = if (hasHolder) WindDetailDataHolder.language else repo.getLanguage()
        val windSpeedUnit = if (hasHolder) WindDetailDataHolder.windSpeedUnit else repo.getWindSpeedUnit()
        val weatherAdaptiveThemeEnabled = if (hasHolder) WindDetailDataHolder.weatherAdaptiveThemeEnabled else repo.isWeatherAdaptiveThemeEnabled()
        val themePreset = if (hasHolder) WindDetailDataHolder.themePreset else repo.getThemePreset()
        val themeMode = if (hasHolder) WindDetailDataHolder.themeMode else repo.getThemeMode()
        val initialWeather = WindDetailDataHolder.weatherData ?: repo.getMemoryCachedWeatherOrNull(city)

        setContent {
            var weatherData by remember { mutableStateOf(initialWeather) }
            LaunchedEffect(city) {
                if (weatherData == null) {
                    weatherData = repo.getCachedWeatherOrNull(city) ?: try {
                        repo.getWeatherData(city)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
            }
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
                    WindDetailScreen(
                        weather = weatherData,
                        city = city,
                        language = language,
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
        // 画面破棄時に不要になったメモリ参照をクリア（必要に応じて）
        if (isFinishing) {
            WindDetailDataHolder.clear()
        }
    }
}
