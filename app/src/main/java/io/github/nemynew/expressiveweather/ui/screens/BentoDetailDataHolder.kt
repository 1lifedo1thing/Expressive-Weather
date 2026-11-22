package io.github.nemynew.expressiveweather.ui.screens

import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.components.BentoCardType

/**
 * ベントグリッドの各カード（紫外線、空気質、湿度、太陽、気圧、視程、降水量、体感温度）の詳細画面へ
 * データを引き渡すための共有データホルダー。
 */
object BentoDetailDataHolder {
    var weatherData: WeatherData? = null
    var city: City? = null
    var language: AppLanguage = AppLanguage.JAPANESE
    var cardType: BentoCardType = BentoCardType.UV
    var temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
    var windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
    var weatherAdaptiveThemeEnabled: Boolean = true
    var themePreset: ThemePreset = ThemePreset.WEATHER
    var themeMode: ThemeMode = ThemeMode.WEATHER

    fun set(
        weather: WeatherData?,
        city: City,
        language: AppLanguage,
        cardType: BentoCardType,
        temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
        windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
        weatherAdaptiveThemeEnabled: Boolean = true,
        themePreset: ThemePreset = ThemePreset.WEATHER,
        themeMode: ThemeMode = ThemeMode.WEATHER
    ) {
        this.weatherData = weather
        this.city = city
        this.language = language
        this.cardType = cardType
        this.temperatureUnit = temperatureUnit
        this.windSpeedUnit = windSpeedUnit
        this.weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled
        this.themePreset = themePreset
        this.themeMode = themeMode
    }

    fun clear() {
        weatherData = null
        city = null
        language = AppLanguage.JAPANESE
        cardType = BentoCardType.UV
        temperatureUnit = TemperatureUnit.CELSIUS
        windSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
        weatherAdaptiveThemeEnabled = true
        themePreset = ThemePreset.WEATHER
        themeMode = ThemeMode.WEATHER
    }
}
