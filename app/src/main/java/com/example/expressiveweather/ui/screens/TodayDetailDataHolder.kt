package com.example.expressiveweather.ui.screens

import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.City
import com.example.expressiveweather.data.TemperatureUnit
import com.example.expressiveweather.data.WeatherData
import com.example.expressiveweather.data.WindSpeedUnit
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset

/**
 * 今日の天気詳細画面 (TodayDetailActivity) へデータを渡すためのデータホルダー。
 * プロセス内での即時かつメモリ効率の高いゼロコピーデータ受け渡しを実現。
 */
object TodayDetailDataHolder {
    var weatherData: WeatherData? = null
    var city: City? = null
    var language: AppLanguage = AppLanguage.JAPANESE
    var temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
    var windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
    var weatherAdaptiveThemeEnabled: Boolean = true
    var themePreset: ThemePreset = ThemePreset.WEATHER
    var themeMode: ThemeMode = ThemeMode.SYSTEM
    var initialSelectedDay: Int = 0
    var initialSelectedHour: Int? = null

    fun set(
        weather: WeatherData?,
        city: City?,
        language: AppLanguage,
        temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
        windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
        weatherAdaptiveThemeEnabled: Boolean = true,
        themePreset: ThemePreset = ThemePreset.WEATHER,
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        initialSelectedDay: Int = 0,
        initialSelectedHour: Int? = null
    ) {
        this.weatherData = weather
        this.city = city
        this.language = language
        this.temperatureUnit = temperatureUnit
        this.windSpeedUnit = windSpeedUnit
        this.weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled
        this.themePreset = themePreset
        this.themeMode = themeMode
        this.initialSelectedDay = initialSelectedDay
        this.initialSelectedHour = initialSelectedHour
    }

    fun clear() {
        weatherData = null
        city = null
        language = AppLanguage.JAPANESE
        temperatureUnit = TemperatureUnit.CELSIUS
        windSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
        weatherAdaptiveThemeEnabled = true
        themePreset = ThemePreset.WEATHER
        themeMode = ThemeMode.WEATHER
        initialSelectedDay = 0
        initialSelectedHour = null
    }
}
