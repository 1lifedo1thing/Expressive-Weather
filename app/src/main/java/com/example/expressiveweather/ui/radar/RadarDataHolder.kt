package com.example.expressiveweather.ui.radar

import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.City
import com.example.expressiveweather.data.RadarColorScheme
import com.example.expressiveweather.data.RainViewerResponse
import com.example.expressiveweather.data.WeatherCondition
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset

/**
 * 雨雲レーダー詳細画面 (RadarDetailActivity) へデータを渡すためのデータホルダー。
 */
object RadarDataHolder {
    var city: City? = null
    var latitude: Double = 35.6895
    var longitude: Double = 139.6917
    var cityName: String = "東京"
    var country: String? = "日本"
    var radarResponse: RainViewerResponse? = null
    var selectedColorScheme: RadarColorScheme = RadarColorScheme.UNIVERSAL_BLUE
    var language: AppLanguage = AppLanguage.JAPANESE
    var weatherAdaptiveThemeEnabled: Boolean = true
    var themePreset: ThemePreset = ThemePreset.WEATHER
    var themeMode: ThemeMode = ThemeMode.SYSTEM
    var isDay: Boolean = true
    var isRadarDebugZoomEnabled: Boolean = false
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
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        country: String? = null,
        isDay: Boolean = true,
        isRadarDebugZoomEnabled: Boolean = false,
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
        this.weatherCondition = weatherCondition
    }

    fun clear() {
        city = null
        country = null
        radarResponse = null
        selectedColorScheme = RadarColorScheme.UNIVERSAL_BLUE
        language = AppLanguage.JAPANESE
        weatherAdaptiveThemeEnabled = false
        themePreset = ThemePreset.SYSTEM
        themeMode = ThemeMode.WEATHER
        isDay = true
        isRadarDebugZoomEnabled = false
        weatherCondition = null
    }
}
