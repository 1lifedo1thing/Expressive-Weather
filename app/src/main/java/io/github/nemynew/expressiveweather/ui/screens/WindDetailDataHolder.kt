package io.github.nemynew.expressiveweather.ui.screens

import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset

/**
 * 風詳細画面 (WindDetailActivity) へデータを渡すためのデータホルダー。
 * プロセス内での即時かつメモリ効率の高いゼロコピーデータ受け渡しを実現。
 */
object WindDetailDataHolder {
    var weatherData: WeatherData? = null
    var city: City? = null
    var language: AppLanguage = AppLanguage.JAPANESE
    var windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
    var weatherAdaptiveThemeEnabled: Boolean = true
    var themePreset: ThemePreset = ThemePreset.WEATHER
    var themeMode: ThemeMode = ThemeMode.WEATHER

    fun set(
        weather: WeatherData?,
        city: City?,
        language: AppLanguage,
        windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
        weatherAdaptiveThemeEnabled: Boolean = true,
        themePreset: ThemePreset = ThemePreset.WEATHER,
        themeMode: ThemeMode = ThemeMode.WEATHER
    ) {
        this.weatherData = weather
        this.city = city
        this.language = language
        this.windSpeedUnit = windSpeedUnit
        this.weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled
        this.themePreset = themePreset
        this.themeMode = themeMode
    }

    fun clear() {
        weatherData = null
        city = null
        language = AppLanguage.JAPANESE
        windSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
        weatherAdaptiveThemeEnabled = true
        themePreset = ThemePreset.WEATHER
        themeMode = ThemeMode.WEATHER
    }
}
