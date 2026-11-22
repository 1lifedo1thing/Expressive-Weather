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
import io.github.nemynew.expressiveweather.ui.components.BentoCardType

/**
 * ベントグリッドの各天候カード（紫外線、空気質、湿度、太陽、気圧、視程、降水量、体感温度）
 * の詳細専用 Activity。
 * ネイティブな新ページとして起動され、Android OS 標準の Activity 遷移アニメーション
 * および Android 14/15 予測型戻るジェスチャーに完全対応。
 */
class BentoDetailActivity : ComponentActivity() {

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
        val hasHolder = BentoDetailDataHolder.weatherData != null
        val extraCardName = intent.getStringExtra(EXTRA_CARD_TYPE)
        val cardType = extraCardName?.let { runCatching { BentoCardType.valueOf(it) }.getOrNull() }
            ?: BentoDetailDataHolder.cardType
        val city = BentoDetailDataHolder.city ?: WeatherRepository.decodeCityJsonOrDefault(
            intent.getStringExtra(WeatherRepository.EXTRA_CITY_JSON),
            repo
        )
        val language = if (hasHolder) BentoDetailDataHolder.language else repo.getLanguage()
        val temperatureUnit = if (hasHolder) BentoDetailDataHolder.temperatureUnit else repo.getTemperatureUnit()
        val windSpeedUnit = if (hasHolder) BentoDetailDataHolder.windSpeedUnit else repo.getWindSpeedUnit()
        val weatherAdaptiveThemeEnabled = if (hasHolder) BentoDetailDataHolder.weatherAdaptiveThemeEnabled else repo.isWeatherAdaptiveThemeEnabled()
        val themePreset = if (hasHolder) BentoDetailDataHolder.themePreset else repo.getThemePreset()
        val themeMode = if (hasHolder) BentoDetailDataHolder.themeMode else repo.getThemeMode()
        val initialWeather = BentoDetailDataHolder.weatherData ?: repo.getMemoryCachedWeatherOrNull(city)

        setContent {
            var weatherData by remember { mutableStateOf(initialWeather) }
            LaunchedEffect(city) {
                if (weatherData == null) {
                    weatherData = repo.getCachedWeatherOrNull(city)
                        ?: runCatching { repo.getWeatherData(city) }.getOrNull()
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
                    when (cardType) {
                        BentoCardType.UV -> UvDetailScreen(
                            weather = weatherData,
                            city = city,
                            language = language,
                            weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                            themePreset = themePreset,
                            themeMode = themeMode,
                            onBack = { finish() }
                        )

                        BentoCardType.AQI -> AirQualityDetailScreen(
                            weather = weatherData,
                            city = city,
                            language = language,
                            weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                            themePreset = themePreset,
                            themeMode = themeMode,
                            onBack = { finish() }
                        )

                        BentoCardType.HUMIDITY -> HumidityDetailScreen(
                            weather = weatherData,
                            city = city,
                            language = language,
                            temperatureUnit = temperatureUnit,
                            weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                            themePreset = themePreset,
                            themeMode = themeMode,
                            onBack = { finish() }
                        )

                        BentoCardType.SUN_CYCLE -> SunMoonDetailScreen(
                            weather = weatherData,
                            city = city,
                            language = language,
                            weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                            themePreset = themePreset,
                            themeMode = themeMode,
                            onBack = { finish() }
                        )

                        BentoCardType.PRESSURE -> PressureDetailScreen(
                            weather = weatherData,
                            city = city,
                            language = language,
                            weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                            themePreset = themePreset,
                            themeMode = themeMode,
                            onBack = { finish() }
                        )

                        BentoCardType.VISIBILITY -> VisibilityDetailScreen(
                            weather = weatherData,
                            city = city,
                            language = language,
                            weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                            themePreset = themePreset,
                            themeMode = themeMode,
                            onBack = { finish() }
                        )

                        BentoCardType.PRECIPITATION -> PrecipitationDetailScreen(
                            weather = weatherData,
                            city = city,
                            language = language,
                            weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                            themePreset = themePreset,
                            themeMode = themeMode,
                            onBack = { finish() }
                        )

                        BentoCardType.COMFORT -> FeelsLikeDetailScreen(
                            weather = weatherData,
                            city = city,
                            language = language,
                            temperatureUnit = temperatureUnit,
                            windSpeedUnit = windSpeedUnit,
                            weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                            themePreset = themePreset,
                            themeMode = themeMode,
                            onBack = { finish() }
                        )

                        BentoCardType.WIND -> {
                            // 風は既存の WindDetailScreen を呼び出し
                            WindDetailScreen(
                                weather = weatherData,
                                city = city,
                                language = language,
                                windSpeedUnit = windSpeedUnit,
                                weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                                themePreset = themePreset,
                                themeMode = themeMode,
                                onBack = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            BentoDetailDataHolder.clear()
        }
    }

    companion object {
        const val EXTRA_CARD_TYPE = "extra_card_type"
    }
}
