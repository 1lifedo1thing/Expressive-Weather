package io.github.nemynew.expressiveweather.ui.radar

import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.RadarColorScheme
import io.github.nemynew.expressiveweather.data.RainViewerResponse
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset

/**
 * 雨雲レーダー詳細画面 (RadarDetailActivity) へデータを渡すためのデータホルダー。
 */
object RadarDataHolder {
    var city: City? = null
    var latitude: Double = Double.NaN
    var longitude: Double = Double.NaN
    var cityName: String = ""
    var country: String? = null
    var radarResponse: RainViewerResponse? = null
    var selectedColorScheme: RadarColorScheme = RadarColorScheme.UNIVERSAL_BLUE
    var language: AppLanguage = AppLanguage.JAPANESE
    var weatherAdaptiveThemeEnabled: Boolean = true
    var themePreset: ThemePreset = ThemePreset.WEATHER
    var themeMode: ThemeMode = ThemeMode.WEATHER
    var isDay: Boolean = true
    var isRadarDebugZoomEnabled: Boolean = false
    var showAllJapanRadarLayers: Boolean = false
    var weatherCondition: WeatherCondition? = null

    fun set(
        city: City?,
        latitude: Double,
        longitude: Double,
        cityName: String,
        radarResponse: RainViewerResponse?,
        selectedColorScheme: RadarColorScheme = RadarColorScheme.UNIVERSAL_BLUE,
        language: AppLanguage = AppLanguage.JAPANESE,
        weatherAdaptiveThemeEnabled: Boolean = true,
        themePreset: ThemePreset = ThemePreset.WEATHER,
        themeMode: ThemeMode = ThemeMode.WEATHER,
        country: String? = null,
        isDay: Boolean = true,
        isRadarDebugZoomEnabled: Boolean = false,
        showAllJapanRadarLayers: Boolean = false,
        weatherCondition: WeatherCondition? = null
    ) {
        this.city = city
        this.latitude = latitude
        this.longitude = longitude
        this.cityName = cityName
        this.country = country ?: city?.country
        this.radarResponse = radarResponse
        this.selectedColorScheme = selectedColorScheme
        this.language = language
        this.weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled
        this.themePreset = themePreset
        this.themeMode = themeMode
        this.isDay = isDay
        this.isRadarDebugZoomEnabled = isRadarDebugZoomEnabled
        this.showAllJapanRadarLayers = showAllJapanRadarLayers
        this.weatherCondition = weatherCondition
    }

    fun clear() {
        city = null
        latitude = Double.NaN
        longitude = Double.NaN
        cityName = ""
        country = null
        radarResponse = null
        selectedColorScheme = RadarColorScheme.UNIVERSAL_BLUE
        language = AppLanguage.JAPANESE
        weatherAdaptiveThemeEnabled = true
        themePreset = ThemePreset.WEATHER
        themeMode = ThemeMode.WEATHER
        isDay = true
        isRadarDebugZoomEnabled = false
        showAllJapanRadarLayers = false
        weatherCondition = null
    }
}
