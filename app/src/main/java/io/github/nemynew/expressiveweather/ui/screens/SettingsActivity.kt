package io.github.nemynew.expressiveweather.ui.screens

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.GdacsAlertRepository
import io.github.nemynew.expressiveweather.data.GlobalAlertItem
import io.github.nemynew.expressiveweather.data.KikikuruNotificationManager
import io.github.nemynew.expressiveweather.data.MeteoAlarmRepository
import io.github.nemynew.expressiveweather.data.NwsAlertRepository
import io.github.nemynew.expressiveweather.data.PrecipitationAnalyzer
import io.github.nemynew.expressiveweather.data.PrecipitationLiveNotificationManager
import io.github.nemynew.expressiveweather.data.RainStatus
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherBackgroundUpdateManager
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.WmoAlertRepository
import io.github.nemynew.expressiveweather.data.local.WeatherDatabase
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.main.MainScreenViewModel
import io.github.nemynew.expressiveweather.util.LocaleUtils
import kotlinx.coroutines.launch

/**
 * 設定画面専用の独立した Activity。
 * ネイティブな新ページとして起動され、Android OS 標準の Activity 遷移アニメーション
 * および Android 14/15 予測型戻るジェスチャーに完全対応。
 * Process Death 後も WeatherRepository (SharedPreferences + Room) から完全復元される。
 */
class SettingsActivity : ComponentActivity() {

    private lateinit var repo: WeatherRepository
    private val currentLanguageState = mutableStateOf(AppLanguage.JAPANESE)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.applyLocaleToContext(newBase))
    }

    private fun rebuildGlobalAlertOverrides(
        nwsOn: Boolean,
        gdacsOn: Boolean,
        wmoOn: Boolean,
        meteoAlarmOn: Boolean
    ): List<GlobalAlertItem>? {
        val overrides = mutableListOf<GlobalAlertItem>()
        if (nwsOn) overrides.addAll(NwsAlertRepository.createSampleAlerts())
        if (gdacsOn) overrides.addAll(GdacsAlertRepository.createSampleAlerts())
        if (wmoOn) overrides.addAll(WmoAlertRepository.createSampleAlerts())
        if (meteoAlarmOn) overrides.addAll(MeteoAlarmRepository.createSampleAlerts())
        return if (overrides.isEmpty()) null else overrides
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleUtils.applyLocaleToActivity(this)
        enableEdgeToEdge()

        val db = WeatherDatabase.getInstance(applicationContext)
        repo = WeatherRepository(
            weatherDao = db.weatherDao(),
            savedCityDao = db.savedCityDao(),
            context = applicationContext
        )

        currentLanguageState.value = repo.getLanguage()
        val initialSection = intent.getStringExtra("initial_section") ?: SettingsDataHolder.initialSection
        val lastCity = repo.getLastSelectedCity()
        val initialWeather = SettingsDataHolder.weatherData ?: repo.getMemoryCachedWeatherOrNull(lastCity)

        setContent {
            val scope = rememberCoroutineScope()
            val settingsVersion by WeatherRepository.settingsVersion.collectAsState()
            var weatherData by remember { mutableStateOf(initialWeather) }
            LaunchedEffect(lastCity) {
                if (weatherData == null) {
                    weatherData = repo.getCachedWeatherOrNull(lastCity)
                }
            }

            val language = remember(settingsVersion, currentLanguageState.value) { repo.getLanguage() }
            val temperatureUnit = remember(settingsVersion) { repo.getTemperatureUnit() }
            val windSpeedUnit = remember(settingsVersion) { repo.getWindSpeedUnit() }
            val weatherUpdateInterval = remember(settingsVersion) { repo.getWeatherUpdateInterval() }
            val themePreset = remember(settingsVersion) { repo.getThemePreset() }
            val weatherAdaptiveThemeEnabled = themePreset == ThemePreset.WEATHER
            val themeMode = remember(settingsVersion) { repo.getThemeMode() }
            val isFrostedGlassEnabled = remember(settingsVersion) { repo.getFrostedGlassEnabled() }
            val accentColorMode = remember(settingsVersion) { repo.getAccentColorMode() }
            val isColorfulShapesEnabled = accentColorMode.isColorful
            val hapticIntensity = remember(settingsVersion) { repo.getHapticIntensity() }
            val isWeatherHapticsEnabled = remember(settingsVersion) { repo.isWeatherHapticsEnabled() }
            val isRadarDebugZoomEnabled = remember(settingsVersion) { repo.getRadarDebugZoomEnabled() }
            val isRadarDebugSourceEnabled = remember(settingsVersion) { repo.getRadarDebugSourceEnabled() }
            val isShowAllJapanRadarLayersEnabled = remember(settingsVersion) { repo.getShowAllJapanRadarLayers() }
            val isLiveAlertNotificationEnabled = remember(settingsVersion) { repo.isLiveAlertNotificationEnabled() }
            val alertNotificationThreshold = remember(settingsVersion) { repo.getAlertNotificationThreshold() }
            val isKikikuruTestEnabled = remember(settingsVersion) { repo.getKikikuruTestEnabled() }
            val isNwsTest = remember(settingsVersion) { repo.getNwsTestEnabled() }
            val isGdacsTest = remember(settingsVersion) { repo.getGdacsTestEnabled() }
            val isGdacsAlertsEnabled = remember(settingsVersion) { repo.getGdacsAlertsEnabled() }
            val isWmoAlertsEnabled = remember(settingsVersion) { repo.getWmoAlertsEnabled() }
            val isWmoAlwaysEnabled = remember(settingsVersion) { repo.isWmoAlwaysEnabled() }
            val isWmoTest = remember(settingsVersion) { repo.getWmoTestEnabled() }
            val isMeteoAlarmAlertsEnabled = remember(settingsVersion) { repo.getMeteoAlarmAlertsEnabled() }
            val isMeteoAlarmTest = remember(settingsVersion) { repo.getMeteoAlarmTestEnabled() }
            val nowcastOverride by MainScreenViewModel.staticNowcastOverride.collectAsState()

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
                accentColorMode = accentColorMode,
                isColorfulShapesEnabled = isColorfulShapesEnabled,
                hapticIntensity = hapticIntensity,
                isWeatherHapticsEnabled = isWeatherHapticsEnabled,
                isRadarDebugZoomEnabled = isRadarDebugZoomEnabled,
                isRadarDebugSourceEnabled = isRadarDebugSourceEnabled,
                isShowAllJapanRadarLayersEnabled = isShowAllJapanRadarLayersEnabled,
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
                precipitationDemoStatus = nowcastOverride?.status,
                initialSection = initialSection,
                onToggleWeatherAdaptiveTheme = {
                    val next = if (repo.getThemePreset() == ThemePreset.WEATHER) ThemePreset.SKY_OCEAN else ThemePreset.WEATHER
                    repo.setThemePreset(next)
                    SettingsDataHolder.onToggleWeatherAdaptiveTheme?.invoke()
                },
                onSelectThemePreset = {
                    repo.setThemePreset(it)
                    SettingsDataHolder.onSelectThemePreset?.invoke(it)
                },
                onSelectThemeMode = {
                    repo.setThemeMode(it)
                    SettingsDataHolder.onSelectThemeMode?.invoke(it)
                },
                onToggleFrostedGlass = {
                    repo.setFrostedGlassEnabled(it)
                    SettingsDataHolder.onToggleFrostedGlass?.invoke(it)
                },
                onSelectAccentColorMode = { mode ->
                    repo.setAccentColorMode(mode)
                    SettingsDataHolder.onSelectAccentColorMode?.invoke(mode)
                },
                onToggleColorfulShapes = {
                    repo.setColorfulShapesEnabled(it)
                    SettingsDataHolder.onToggleColorfulShapes?.invoke(it)
                },
                onSelectHapticIntensity = {
                    repo.setHapticIntensity(it)
                },
                onToggleWeatherHaptics = {
                    repo.setWeatherHapticsEnabled(it)
                },
                onToggleRadarDebugZoom = {
                    repo.setRadarDebugZoomEnabled(it)
                    SettingsDataHolder.onToggleRadarDebugZoom?.invoke(it)
                },
                onToggleRadarDebugSource = {
                    repo.setRadarDebugSourceEnabled(it)
                    SettingsDataHolder.onToggleRadarDebugSource?.invoke(it)
                },
                onToggleShowAllJapanRadarLayers = {
                    repo.setShowAllJapanRadarLayers(it)
                    SettingsDataHolder.onToggleShowAllJapanRadarLayers?.invoke(it)
                },
                onToggleLiveAlertNotification = { enabled ->
                    repo.setLiveAlertNotificationEnabled(enabled)
                    if (!enabled) {
                        KikikuruNotificationManager.cancelAll(applicationContext)
                        PrecipitationLiveNotificationManager.cancel(applicationContext)
                    }
                    SettingsDataHolder.onToggleLiveAlertNotification?.invoke(enabled)
                },
                onSelectAlertNotificationThreshold = {
                    repo.setAlertNotificationThreshold(it)
                    SettingsDataHolder.onSelectAlertNotificationThreshold?.invoke(it)
                },
                onToggleKikikuruTest = {
                    repo.setKikikuruTestEnabled(it)
                    SettingsDataHolder.isKikikuruTestEnabled = it
                    SettingsDataHolder.onToggleKikikuruTest?.invoke(it)
                },
                onToggleNwsTest = { enabled ->
                    repo.setNwsTestEnabled(enabled)
                    SettingsDataHolder.isNwsTestEnabled = enabled
                    MainScreenViewModel.staticGlobalAlertsOverride.value = rebuildGlobalAlertOverrides(
                        enabled, repo.getGdacsTestEnabled(), repo.getWmoTestEnabled(), repo.getMeteoAlarmTestEnabled()
                    )
                    SettingsDataHolder.onToggleNwsTest?.invoke(enabled)
                },
                onToggleGdacsTest = { enabled ->
                    repo.setGdacsTestEnabled(enabled)
                    SettingsDataHolder.isGdacsTestEnabled = enabled
                    MainScreenViewModel.staticGlobalAlertsOverride.value = rebuildGlobalAlertOverrides(
                        repo.getNwsTestEnabled(), enabled, repo.getWmoTestEnabled(), repo.getMeteoAlarmTestEnabled()
                    )
                    SettingsDataHolder.onToggleGdacsTest?.invoke(enabled)
                },
                onToggleGdacsAlerts = {
                    repo.setGdacsAlertsEnabled(it)
                    if (it) WeatherRepository.requestForceRefresh()
                    SettingsDataHolder.onToggleGdacsAlerts?.invoke(it)
                },
                onToggleWmoAlerts = {
                    repo.setWmoAlertsEnabled(it)
                    if (it) WeatherRepository.requestForceRefresh()
                    SettingsDataHolder.onToggleWmoAlerts?.invoke(it)
                },
                onToggleWmoAlways = {
                    repo.setWmoAlwaysEnabled(it)
                    WeatherRepository.requestForceRefresh()
                    SettingsDataHolder.onToggleWmoAlways?.invoke(it)
                },
                onToggleWmoTest = { enabled ->
                    repo.setWmoTestEnabled(enabled)
                    SettingsDataHolder.isWmoTestEnabled = enabled
                    MainScreenViewModel.staticGlobalAlertsOverride.value = rebuildGlobalAlertOverrides(
                        repo.getNwsTestEnabled(), repo.getGdacsTestEnabled(), enabled, repo.getMeteoAlarmTestEnabled()
                    )
                    SettingsDataHolder.onToggleWmoTest?.invoke(enabled)
                },
                onToggleMeteoAlarmAlerts = {
                    repo.setMeteoAlarmAlertsEnabled(it)
                    if (it) WeatherRepository.requestForceRefresh()
                    SettingsDataHolder.onToggleMeteoAlarmAlerts?.invoke(it)
                },
                onToggleMeteoAlarmTest = { enabled ->
                    repo.setMeteoAlarmTestEnabled(enabled)
                    SettingsDataHolder.isMeteoAlarmTestEnabled = enabled
                    MainScreenViewModel.staticGlobalAlertsOverride.value = rebuildGlobalAlertOverrides(
                        repo.getNwsTestEnabled(), repo.getGdacsTestEnabled(), repo.getWmoTestEnabled(), enabled
                    )
                    SettingsDataHolder.onToggleMeteoAlarmTest?.invoke(enabled)
                },
                onCyclePrecipitationDemo = {
                    val currentOverride = MainScreenViewModel.staticNowcastOverride.value
                    val nextStatus = when (currentOverride?.status) {
                        null -> RainStatus.RAIN_EXPECTED_TONIGHT
                        RainStatus.RAIN_EXPECTED_TONIGHT -> RainStatus.RAIN_APPROACHING
                        RainStatus.RAIN_APPROACHING -> RainStatus.RAINING
                        RainStatus.RAINING -> null
                        else -> null
                    }
                    val newOverride = nextStatus?.let { PrecipitationAnalyzer.createMock(it) }
                    MainScreenViewModel.staticNowcastOverride.value = newOverride
                    if (repo.isLiveAlertNotificationEnabled()) {
                        if (newOverride != null && (newOverride.status == RainStatus.RAIN_APPROACHING || newOverride.status == RainStatus.RAINING)) {
                            PrecipitationLiveNotificationManager.sendTestNotification(applicationContext, newOverride.status)
                        } else {
                            PrecipitationLiveNotificationManager.cancel(applicationContext)
                        }
                    }
                    SettingsDataHolder.onCyclePrecipitationDemo?.invoke()
                },
                onClearPrecipitationDemo = {
                    MainScreenViewModel.staticNowcastOverride.value = null
                    PrecipitationLiveNotificationManager.cancel(applicationContext)
                },
                onToggleTemperatureUnit = {
                    val next = if (repo.getTemperatureUnit() == TemperatureUnit.CELSIUS) {
                        TemperatureUnit.FAHRENHEIT
                    } else {
                        TemperatureUnit.CELSIUS
                    }
                    repo.setTemperatureUnit(next)
                    SettingsDataHolder.onToggleTemperatureUnit?.invoke()
                },
                onSelectWindSpeedUnit = {
                    repo.setWindSpeedUnit(it)
                    SettingsDataHolder.onSelectWindSpeedUnit?.invoke(it)
                },
                onSelectWeatherUpdateInterval = {
                    repo.setWeatherUpdateInterval(it)
                    WeatherBackgroundUpdateManager.getInstance(applicationContext).restart()
                    SettingsDataHolder.onSelectWeatherUpdateInterval?.invoke(it)
                },
                onSelectLanguage = { newLang ->
                    currentLanguageState.value = newLang
                    SettingsDataHolder.language = newLang
                    repo.setLanguage(newLang)
                    SettingsDataHolder.onSelectLanguage?.invoke(newLang)
                },
                onSelectPreviewCondition = { cond, isD ->
                    WeatherRepository.previewConditionOverride.value = cond to isD
                    SettingsDataHolder.onSelectPreviewCondition?.invoke(cond, isD)
                },
                onClearCache = {
                    MainScreenViewModel.staticNowcastOverride.value = null
                    scope.launch {
                        repo.clearCache()
                        WeatherRepository.requestForceRefresh()
                    }
                    SettingsDataHolder.onClearCache?.invoke()
                },
                onForceRefresh = {
                    MainScreenViewModel.staticNowcastOverride.value = null
                    WeatherRepository.requestForceRefresh()
                    SettingsDataHolder.onForceRefresh?.invoke()
                },
                onBack = { finish() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val savedLang = repo.getLanguage()
        if (currentLanguageState.value != savedLang) {
            currentLanguageState.value = savedLang
            SettingsDataHolder.language = savedLang
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
