package io.github.nemynew.expressiveweather.ui.screens

import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset

/**
 * 設定画面 (SettingsActivity) へデータを渡すためのデータホルダー。
 * プロセス内での即時かつコールバック付きのデータ受け渡しを実現。
 */
object SettingsDataHolder {
    var weatherData: WeatherData? = null
    var language: AppLanguage = AppLanguage.JAPANESE
    var temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
    var windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
    var weatherUpdateInterval: io.github.nemynew.expressiveweather.data.WeatherUpdateInterval = io.github.nemynew.expressiveweather.data.WeatherUpdateInterval.MINUTES_30
    var weatherAdaptiveThemeEnabled: Boolean = true
    var themePreset: ThemePreset = ThemePreset.WEATHER
    var themeMode: ThemeMode = ThemeMode.WEATHER
    var isFrostedGlassEnabled: Boolean = true
    var isColorfulShapesEnabled: Boolean = true
    var isRadarDebugZoomEnabled: Boolean = false
    var isRadarDebugSourceEnabled: Boolean = true
    var isShowAllJapanRadarLayersEnabled: Boolean = false
    var isLiveAlertNotificationEnabled: Boolean = true
    var alertNotificationThreshold: io.github.nemynew.expressiveweather.data.AlertNotificationThreshold = io.github.nemynew.expressiveweather.data.AlertNotificationThreshold.DEFAULT
    var isKikikuruTestEnabled: Boolean = false
    var isNwsTestEnabled: Boolean = false
    var isGdacsTestEnabled: Boolean = false
    var isGdacsAlertsEnabled: Boolean = false
    var isWmoAlertsEnabled: Boolean = true
    var isWmoAlwaysEnabled: Boolean = false
    var isWmoTestEnabled: Boolean = false
    var isMeteoAlarmAlertsEnabled: Boolean = true
    var isMeteoAlarmTestEnabled: Boolean = false
    var initialSection: String = SECTION_ALL

    // コールバック
    var onToggleWeatherAdaptiveTheme: (() -> Unit)? = null
    var onSelectThemePreset: ((ThemePreset) -> Unit)? = null
    var onSelectThemeMode: ((ThemeMode) -> Unit)? = null
    var onToggleFrostedGlass: ((Boolean) -> Unit)? = null
    var onToggleColorfulShapes: ((Boolean) -> Unit)? = null
    var onToggleRadarDebugZoom: ((Boolean) -> Unit)? = null
    var onToggleRadarDebugSource: ((Boolean) -> Unit)? = null
    var onToggleShowAllJapanRadarLayers: ((Boolean) -> Unit)? = null
    var onToggleLiveAlertNotification: ((Boolean) -> Unit)? = null
    var onSelectAlertNotificationThreshold: ((io.github.nemynew.expressiveweather.data.AlertNotificationThreshold) -> Unit)? = null
    var onToggleKikikuruTest: ((Boolean) -> Unit)? = null
    var onToggleNwsTest: ((Boolean) -> Unit)? = null
    var onToggleGdacsTest: ((Boolean) -> Unit)? = null
    var onToggleGdacsAlerts: ((Boolean) -> Unit)? = null
    var onToggleWmoAlerts: ((Boolean) -> Unit)? = null
    var onToggleWmoAlways: ((Boolean) -> Unit)? = null
    var onToggleWmoTest: ((Boolean) -> Unit)? = null
    var onToggleMeteoAlarmAlerts: ((Boolean) -> Unit)? = null
    var onToggleMeteoAlarmTest: ((Boolean) -> Unit)? = null
    var onCyclePrecipitationDemo: (() -> Unit)? = null
    var onToggleTemperatureUnit: (() -> Unit)? = null
    var onSelectWindSpeedUnit: ((WindSpeedUnit) -> Unit)? = null
    var onSelectWeatherUpdateInterval: ((io.github.nemynew.expressiveweather.data.WeatherUpdateInterval) -> Unit)? = null
    var onSelectLanguage: ((AppLanguage) -> Unit)? = null
    var onSelectPreviewCondition: ((WeatherCondition?, Boolean?) -> Unit)? = null
    var onClearCache: (() -> Unit)? = null
    var onForceRefresh: (() -> Unit)? = null

    const val SECTION_ALL = "all"
    const val SECTION_APPEARANCE = "appearance"
    const val SECTION_GENERAL = "general"
    const val SECTION_LANGUAGE = "language"
    const val SECTION_SOURCES = "sources"
    const val SECTION_DEBUG = "debug"

    fun set(
        weather: WeatherData?,
        language: AppLanguage,
        temperatureUnit: TemperatureUnit,
        windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
        weatherUpdateInterval: io.github.nemynew.expressiveweather.data.WeatherUpdateInterval = io.github.nemynew.expressiveweather.data.WeatherUpdateInterval.MINUTES_30,
        weatherAdaptiveThemeEnabled: Boolean = true,
        themePreset: ThemePreset = ThemePreset.WEATHER,
        themeMode: ThemeMode = ThemeMode.WEATHER,
        isFrostedGlassEnabled: Boolean = true,
        isColorfulShapesEnabled: Boolean = true,
        isRadarDebugZoomEnabled: Boolean = false,
        isRadarDebugSourceEnabled: Boolean = true,
        isShowAllJapanRadarLayersEnabled: Boolean = false,
        isLiveAlertNotificationEnabled: Boolean = true,
        alertNotificationThreshold: io.github.nemynew.expressiveweather.data.AlertNotificationThreshold = io.github.nemynew.expressiveweather.data.AlertNotificationThreshold.DEFAULT,
        isKikikuruTestEnabled: Boolean = false,
        isNwsTestEnabled: Boolean = false,
        isGdacsTestEnabled: Boolean = false,
        isGdacsAlertsEnabled: Boolean = false,
        isWmoAlertsEnabled: Boolean = true,
        isWmoAlwaysEnabled: Boolean = false,
        isWmoTestEnabled: Boolean = false,
        isMeteoAlarmAlertsEnabled: Boolean = true,
        isMeteoAlarmTestEnabled: Boolean = false,
        initialSection: String = SECTION_ALL,
        onToggleWeatherAdaptiveTheme: (() -> Unit)? = null,
        onSelectThemePreset: ((ThemePreset) -> Unit)? = null,
        onSelectThemeMode: ((ThemeMode) -> Unit)? = null,
        onToggleFrostedGlass: ((Boolean) -> Unit)? = null,
        onToggleColorfulShapes: ((Boolean) -> Unit)? = null,
        onToggleRadarDebugZoom: ((Boolean) -> Unit)? = null,
        onToggleRadarDebugSource: ((Boolean) -> Unit)? = null,
        onToggleShowAllJapanRadarLayers: ((Boolean) -> Unit)? = null,
        onToggleLiveAlertNotification: ((Boolean) -> Unit)? = null,
        onSelectAlertNotificationThreshold: ((io.github.nemynew.expressiveweather.data.AlertNotificationThreshold) -> Unit)? = null,
        onToggleKikikuruTest: ((Boolean) -> Unit)? = null,
        onToggleNwsTest: ((Boolean) -> Unit)? = null,
        onToggleGdacsTest: ((Boolean) -> Unit)? = null,
        onToggleGdacsAlerts: ((Boolean) -> Unit)? = null,
        onToggleWmoAlerts: ((Boolean) -> Unit)? = null,
        onToggleWmoAlways: ((Boolean) -> Unit)? = null,
        onToggleWmoTest: ((Boolean) -> Unit)? = null,
        onToggleMeteoAlarmAlerts: ((Boolean) -> Unit)? = null,
        onToggleMeteoAlarmTest: ((Boolean) -> Unit)? = null,
        onCyclePrecipitationDemo: (() -> Unit)? = null,
        onToggleTemperatureUnit: (() -> Unit)? = null,
        onSelectWindSpeedUnit: ((WindSpeedUnit) -> Unit)? = null,
        onSelectWeatherUpdateInterval: ((io.github.nemynew.expressiveweather.data.WeatherUpdateInterval) -> Unit)? = null,
        onSelectLanguage: ((AppLanguage) -> Unit)? = null,
        onSelectPreviewCondition: ((WeatherCondition?, Boolean?) -> Unit)? = null,
        onClearCache: (() -> Unit)? = null,
        onForceRefresh: (() -> Unit)? = null
    ) {
        this.weatherData = weather
        this.language = language
        this.temperatureUnit = temperatureUnit
        this.windSpeedUnit = windSpeedUnit
        this.weatherUpdateInterval = weatherUpdateInterval
        this.weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled
        this.themePreset = themePreset
        this.themeMode = themeMode
        this.isFrostedGlassEnabled = isFrostedGlassEnabled
        this.isColorfulShapesEnabled = isColorfulShapesEnabled
        this.isRadarDebugZoomEnabled = isRadarDebugZoomEnabled
        this.isRadarDebugSourceEnabled = isRadarDebugSourceEnabled
        this.isShowAllJapanRadarLayersEnabled = isShowAllJapanRadarLayersEnabled
        this.isLiveAlertNotificationEnabled = isLiveAlertNotificationEnabled
        this.alertNotificationThreshold = alertNotificationThreshold
        this.isKikikuruTestEnabled = isKikikuruTestEnabled
        this.isNwsTestEnabled = isNwsTestEnabled
        this.isGdacsTestEnabled = isGdacsTestEnabled
        this.isGdacsAlertsEnabled = isGdacsAlertsEnabled
        this.isWmoAlertsEnabled = isWmoAlertsEnabled
        this.isWmoAlwaysEnabled = isWmoAlwaysEnabled
        this.isWmoTestEnabled = isWmoTestEnabled
        this.isMeteoAlarmAlertsEnabled = isMeteoAlarmAlertsEnabled
        this.isMeteoAlarmTestEnabled = isMeteoAlarmTestEnabled
        this.initialSection = initialSection
        this.onToggleWeatherAdaptiveTheme = onToggleWeatherAdaptiveTheme
        this.onSelectThemePreset = onSelectThemePreset
        this.onSelectThemeMode = onSelectThemeMode
        this.onToggleFrostedGlass = onToggleFrostedGlass
        this.onToggleColorfulShapes = onToggleColorfulShapes
        this.onToggleRadarDebugZoom = onToggleRadarDebugZoom
        this.onToggleRadarDebugSource = onToggleRadarDebugSource
        this.onToggleShowAllJapanRadarLayers = onToggleShowAllJapanRadarLayers
        this.onToggleLiveAlertNotification = onToggleLiveAlertNotification
        this.onSelectAlertNotificationThreshold = onSelectAlertNotificationThreshold
        this.onToggleKikikuruTest = onToggleKikikuruTest
        this.onToggleNwsTest = onToggleNwsTest
        this.onToggleGdacsTest = onToggleGdacsTest
        this.onToggleGdacsAlerts = onToggleGdacsAlerts
        this.onToggleWmoAlerts = onToggleWmoAlerts
        this.onToggleWmoAlways = onToggleWmoAlways
        this.onToggleWmoTest = onToggleWmoTest
        this.onToggleMeteoAlarmAlerts = onToggleMeteoAlarmAlerts
        this.onToggleMeteoAlarmTest = onToggleMeteoAlarmTest
        this.onCyclePrecipitationDemo = onCyclePrecipitationDemo
        this.onToggleTemperatureUnit = onToggleTemperatureUnit
        this.onSelectWindSpeedUnit = onSelectWindSpeedUnit
        this.onSelectWeatherUpdateInterval = onSelectWeatherUpdateInterval
        this.onSelectLanguage = onSelectLanguage
        this.onSelectPreviewCondition = onSelectPreviewCondition
        this.onClearCache = onClearCache
        this.onForceRefresh = onForceRefresh
    }

    fun clear() {
        weatherData = null
        language = AppLanguage.JAPANESE
        temperatureUnit = TemperatureUnit.CELSIUS
        windSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
        weatherUpdateInterval = io.github.nemynew.expressiveweather.data.WeatherUpdateInterval.MINUTES_30
        weatherAdaptiveThemeEnabled = true
        themePreset = ThemePreset.WEATHER
        themeMode = ThemeMode.WEATHER
        isFrostedGlassEnabled = true
        isColorfulShapesEnabled = true
        isRadarDebugZoomEnabled = false
        isRadarDebugSourceEnabled = true
        isShowAllJapanRadarLayersEnabled = false
        isLiveAlertNotificationEnabled = true
        alertNotificationThreshold = io.github.nemynew.expressiveweather.data.AlertNotificationThreshold.DEFAULT
        isKikikuruTestEnabled = false
        isNwsTestEnabled = false
        isGdacsTestEnabled = false
        isGdacsAlertsEnabled = false
        isWmoAlertsEnabled = true
        isWmoAlwaysEnabled = false
        isWmoTestEnabled = false
        isMeteoAlarmAlertsEnabled = true
        isMeteoAlarmTestEnabled = false
        initialSection = SECTION_ALL
        onToggleWeatherAdaptiveTheme = null
        onSelectThemePreset = null
        onSelectThemeMode = null
        onToggleFrostedGlass = null
        onToggleColorfulShapes = null
        onToggleRadarDebugZoom = null
        onToggleRadarDebugSource = null
        onToggleShowAllJapanRadarLayers = null
        onToggleLiveAlertNotification = null
        onSelectAlertNotificationThreshold = null
        onToggleKikikuruTest = null
        onToggleNwsTest = null
        onToggleGdacsTest = null
        onToggleGdacsAlerts = null
        onToggleWmoAlerts = null
        onToggleWmoAlways = null
        onToggleWmoTest = null
        onToggleMeteoAlarmAlerts = null
        onToggleMeteoAlarmTest = null
        onCyclePrecipitationDemo = null
        onToggleTemperatureUnit = null
        onSelectWindSpeedUnit = null
        onSelectWeatherUpdateInterval = null
        onSelectLanguage = null
        onSelectPreviewCondition = null
        onClearCache = null
        onForceRefresh = null
    }
}
