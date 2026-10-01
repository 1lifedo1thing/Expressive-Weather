package com.example.expressiveweather.ui.screens

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.util.LocaleUtils

/**
 * 設定画面専用の独立した Activity。
 * ネイティブな新ページとして起動され、Android OS 標準の Activity 遷移アニメーション
 * および Android 14/15 予測型戻るジェスチャーに完全対応。
 */
class SettingsActivity : ComponentActivity() {

    private val currentLanguageState = mutableStateOf(AppLanguage.JAPANESE)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        currentLanguageState.value = SettingsDataHolder.language

        val weatherData = SettingsDataHolder.weatherData
        val temperatureUnit = SettingsDataHolder.temperatureUnit
        val windSpeedUnit = SettingsDataHolder.windSpeedUnit
        val weatherUpdateInterval = SettingsDataHolder.weatherUpdateInterval
        val weatherAdaptiveThemeEnabled = SettingsDataHolder.weatherAdaptiveThemeEnabled
        val themePreset = SettingsDataHolder.themePreset
        val themeMode = SettingsDataHolder.themeMode
        val isFrostedGlassEnabled = SettingsDataHolder.isFrostedGlassEnabled
        val isColorfulShapesEnabled = SettingsDataHolder.isColorfulShapesEnabled
        val isRadarDebugZoomEnabled = SettingsDataHolder.isRadarDebugZoomEnabled
        val isRadarDebugSourceEnabled = SettingsDataHolder.isRadarDebugSourceEnabled
        val isLiveAlertNotificationEnabled = SettingsDataHolder.isLiveAlertNotificationEnabled
        val alertNotificationThreshold = SettingsDataHolder.alertNotificationThreshold
        val isKikikuruTestEnabled = SettingsDataHolder.isKikikuruTestEnabled
        val initialSection = intent.getStringExtra("initial_section") ?: SettingsDataHolder.initialSection

        val onToggleTheme = SettingsDataHolder.onToggleWeatherAdaptiveTheme ?: {}
        val onSelectPreset = SettingsDataHolder.onSelectThemePreset ?: {}
        val onSelectMode = SettingsDataHolder.onSelectThemeMode ?: {}
        val onToggleFrosted = SettingsDataHolder.onToggleFrostedGlass ?: {}
        val onToggleColorful = SettingsDataHolder.onToggleColorfulShapes ?: {}
        val onToggleRadarZoom = SettingsDataHolder.onToggleRadarDebugZoom ?: {}
        val onToggleRadarSource = SettingsDataHolder.onToggleRadarDebugSource ?: {}
        val onToggleLiveAlert = SettingsDataHolder.onToggleLiveAlertNotification ?: {}
        val onSelectThreshold = SettingsDataHolder.onSelectAlertNotificationThreshold ?: {}
        val onToggleKikikuruTest = SettingsDataHolder.onToggleKikikuruTest ?: {}
        val isNwsTest = SettingsDataHolder.isNwsTestEnabled
        val isGdacsTest = SettingsDataHolder.isGdacsTestEnabled
        val isGdacsAlertsEnabled = SettingsDataHolder.isGdacsAlertsEnabled
        val isWmoAlertsEnabled = SettingsDataHolder.isWmoAlertsEnabled
        val isWmoAlwaysEnabled = SettingsDataHolder.isWmoAlwaysEnabled
        val isWmoTest = SettingsDataHolder.isWmoTestEnabled
        val isMeteoAlarmAlertsEnabled = SettingsDataHolder.isMeteoAlarmAlertsEnabled
        val isMeteoAlarmTest = SettingsDataHolder.isMeteoAlarmTestEnabled
        val onToggleGdacsAlerts = SettingsDataHolder.onToggleGdacsAlerts ?: {}
        val onToggleWmoAlerts = SettingsDataHolder.onToggleWmoAlerts ?: {}
        val onToggleWmoAlways = SettingsDataHolder.onToggleWmoAlways ?: {}
        val onToggleWmo = SettingsDataHolder.onToggleWmoTest ?: {}
        val onToggleMeteoAlarmAlerts = SettingsDataHolder.onToggleMeteoAlarmAlerts ?: {}
        val onToggleMeteoAlarm = SettingsDataHolder.onToggleMeteoAlarmTest ?: {}
        val onToggleNws = SettingsDataHolder.onToggleNwsTest ?: {}
        val onToggleGdacs = SettingsDataHolder.onToggleGdacsTest ?: {}
        val onCyclePrecipitationDemo = SettingsDataHolder.onCyclePrecipitationDemo ?: {}
        val onToggleUnit = SettingsDataHolder.onToggleTemperatureUnit ?: {}
        val onSelectWindUnit = SettingsDataHolder.onSelectWindSpeedUnit ?: {}
        val onSelectUpdateInterval = SettingsDataHolder.onSelectWeatherUpdateInterval ?: {}
        val onSelectLang = SettingsDataHolder.onSelectLanguage ?: {}
        val onSelectPreview = SettingsDataHolder.onSelectPreviewCondition
        val onClearCache = SettingsDataHolder.onClearCache
        val onForceRefresh = SettingsDataHolder.onForceRefresh

        setContent {
            val language by currentLanguageState
            SettingsScreen(
                weather = weatherData,
                language = language,
                temperatureUnit = temperatureUnit,
                windSpeedUnit = windSpeedUnit,
                weatherUpdateInterval = weatherUpdateInterval,
                weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
                themePreset = themePreset,
                themeMode = themeMode,
                isFrostedGlassEnabled = isFrostedGlassEnabled,
                isColorfulShapesEnabled = isColorfulShapesEnabled,
                isRadarDebugZoomEnabled = isRadarDebugZoomEnabled,
                isRadarDebugSourceEnabled = isRadarDebugSourceEnabled,
                isLiveAlertNotificationEnabled = isLiveAlertNotificationEnabled,
                alertNotificationThreshold = alertNotificationThreshold,
                isKikikuruTestEnabled = isKikikuruTestEnabled,
                isNwsTestEnabled = isNwsTest,
                isGdacsTestEnabled = isGdacsTest,
                isGdacsAlertsEnabled = isGdacsAlertsEnabled,
                isWmoAlertsEnabled = isWmoAlertsEnabled,
                isWmoAlwaysEnabled = isWmoAlwaysEnabled,
                isWmoTestEnabled = isWmoTest,
                isMeteoAlarmAlertsEnabled = isMeteoAlarmAlertsEnabled,
                isMeteoAlarmTestEnabled = isMeteoAlarmTest,
                initialSection = initialSection,
                onToggleWeatherAdaptiveTheme = onToggleTheme,
                onSelectThemePreset = onSelectPreset,
                onSelectThemeMode = onSelectMode,
                onToggleFrostedGlass = onToggleFrosted,
                onToggleColorfulShapes = onToggleColorful,
                onToggleRadarDebugZoom = onToggleRadarZoom,
                onToggleRadarDebugSource = onToggleRadarSource,
                onToggleLiveAlertNotification = onToggleLiveAlert,
                onSelectAlertNotificationThreshold = onSelectThreshold,
                onToggleKikikuruTest = onToggleKikikuruTest,
                onToggleNwsTest = onToggleNws,
                onToggleGdacsTest = onToggleGdacs,
                onToggleGdacsAlerts = onToggleGdacsAlerts,
                onToggleWmoAlerts = onToggleWmoAlerts,
                onToggleWmoAlways = onToggleWmoAlways,
                onToggleWmoTest = onToggleWmo,
                onToggleMeteoAlarmAlerts = onToggleMeteoAlarmAlerts,
                onToggleMeteoAlarmTest = onToggleMeteoAlarm,
                onCyclePrecipitationDemo = onCyclePrecipitationDemo,
                onToggleTemperatureUnit = onToggleUnit,
                onSelectWindSpeedUnit = onSelectWindUnit,
                onSelectWeatherUpdateInterval = onSelectUpdateInterval,
                onSelectLanguage = { newLang ->
                    currentLanguageState.value = newLang
                    SettingsDataHolder.language = newLang
                    onSelectLang(newLang)
                },
                onSelectPreviewCondition = onSelectPreview,
                onClearCache = onClearCache,
                onForceRefresh = onForceRefresh,
                onBack = { finish() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentLanguageState.value != SettingsDataHolder.language) {
            currentLanguageState.value = SettingsDataHolder.language
            LocaleUtils.applyLocaleToActivity(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            SettingsDataHolder.clear()
        }
    }
}
