package io.github.nemynew.expressiveweather.ui.screens

import io.github.nemynew.expressiveweather.data.AlertSource
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.GlobalAlertItem
import io.github.nemynew.expressiveweather.data.MunicipalityAlertInfo
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset

/**
 * 警報・防災情報詳細画面 (AlertDetailActivity) へのデータ受け渡しホルダー
 */
object AlertDetailDataHolder {
    var weatherData: WeatherData? = null
    var city: City? = null
    var language: AppLanguage = AppLanguage.JAPANESE
    var weatherAdaptiveThemeEnabled: Boolean = true
    var themePreset: ThemePreset = ThemePreset.WEATHER
    var themeMode: ThemeMode = ThemeMode.WEATHER
    var alertInfo: MunicipalityAlertInfo? = null
    var jmaWarnings: List<String> = emptyList()
    var jmaOverviewText: String? = null
    var globalAlerts: List<GlobalAlertItem> = emptyList()
    var targetSource: AlertSource? = null

    fun set(
        weather: WeatherData?,
        city: City?,
        language: AppLanguage,
        weatherAdaptiveThemeEnabled: Boolean,
        themePreset: ThemePreset,
        themeMode: ThemeMode,
        alertInfo: MunicipalityAlertInfo?,
        jmaWarnings: List<String>,
        jmaOverviewText: String?,
        globalAlerts: List<GlobalAlertItem>,
        targetSource: AlertSource? = null
    ) {
        this.weatherData = weather
        this.city = city
        this.language = language
        this.weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled
        this.themePreset = themePreset
        this.themeMode = themeMode
        this.alertInfo = alertInfo
        this.jmaWarnings = jmaWarnings
        this.jmaOverviewText = jmaOverviewText
        this.globalAlerts = globalAlerts
        this.targetSource = targetSource
    }

    fun clear() {
        weatherData = null
        city = null
        language = AppLanguage.JAPANESE
        weatherAdaptiveThemeEnabled = true
        themePreset = ThemePreset.WEATHER
        themeMode = ThemeMode.WEATHER
        alertInfo = null
        jmaWarnings = emptyList()
        jmaOverviewText = null
        globalAlerts = emptyList()
        targetSource = null
    }
}
