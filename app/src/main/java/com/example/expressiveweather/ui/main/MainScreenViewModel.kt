package com.example.expressiveweather.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expressiveweather.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.location.Location
import kotlinx.coroutines.launch
import com.example.expressiveweather.data.local.LocationKeyNormalizer
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset
import com.example.expressiveweather.ui.components.BentoCardType
import kotlinx.coroutines.Job

data class WeatherUiState(
    val weatherData: WeatherData? = null,
    val defaultCity: City,
    val selectedCity: City,
    val savedCities: List<City> = emptyList(),
    val recentSearches: List<City> = emptyList(),
    val availableCities: List<City> = emptyList(),
    val searchResults: List<City> = emptyList(),
    val isSearching: Boolean = false,
    val selectedHour: HourlyForecast? = null,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    val weatherUpdateInterval: WeatherUpdateInterval = WeatherUpdateInterval.MINUTES_30,
    val language: AppLanguage = AppLanguage.JAPANESE,
    val weatherAdaptiveThemeEnabled: Boolean = true,
    val themePreset: ThemePreset = ThemePreset.WEATHER,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isFrostedGlassEnabled: Boolean = true,
    val isColorfulShapesEnabled: Boolean = true,
    val isRadarDebugZoomEnabled: Boolean = false,
    val isRadarDebugSourceEnabled: Boolean = false,
    val isLiveAlertNotificationEnabled: Boolean = true,
    val alertNotificationThreshold: AlertNotificationThreshold = AlertNotificationThreshold.DEFAULT,
    val isKikikuruTestEnabled: Boolean = false,
    val isNwsTestEnabled: Boolean = false,
    val isGdacsTestEnabled: Boolean = false,
    val isGdacsAlertsEnabled: Boolean = false,
    val isWmoAlertsEnabled: Boolean = true,
    val isWmoAlwaysEnabled: Boolean = false,
    val isWmoTestEnabled: Boolean = false,
    val isMeteoAlarmAlertsEnabled: Boolean = true,
    val isMeteoAlarmTestEnabled: Boolean = false,
    val globalAlertsOverride: List<GlobalAlertItem>? = null,
    val precipitationNowcastOverride: PrecipitationNowcast? = null,
    val currentAlert: MunicipalityAlertInfo? = null,
    val savedCitiesMaxAlert: MunicipalityAlertInfo? = null,
    val bentoCardOrder: List<BentoCardType> = BentoCardType.DEFAULT_ORDER,
    val selectedDetailCard: BentoCardType? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isFromCache: Boolean = false,
    val isStale: Boolean = false,
    val showCitySheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val errorMessage: String? = null
)

class MainScreenViewModel(
    private val repository: WeatherRepository = WeatherRepository(),
    private val locationProvider: CurrentLocationProvider? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WeatherUiState(
            defaultCity = repository.defaultCity,
            selectedCity = repository.getLastSelectedCity(),
            savedCities = emptyList(),
            recentSearches = emptyList(),
            availableCities = emptyList(),
            language = repository.getLanguage(),
            windSpeedUnit = repository.getWindSpeedUnit(),
            weatherUpdateInterval = repository.getWeatherUpdateInterval(),
            weatherAdaptiveThemeEnabled = repository.isWeatherAdaptiveThemeEnabled(),
            themePreset = repository.getThemePreset(),
            themeMode = repository.getThemeMode(),
            isFrostedGlassEnabled = repository.getFrostedGlassEnabled(),
            isColorfulShapesEnabled = repository.getColorfulShapesEnabled(),
            isRadarDebugZoomEnabled = repository.getRadarDebugZoomEnabled(),
            isRadarDebugSourceEnabled = repository.getRadarDebugSourceEnabled(),
            isLiveAlertNotificationEnabled = repository.isLiveAlertNotificationEnabled(),
            alertNotificationThreshold = repository.getAlertNotificationThreshold(),
            isGdacsAlertsEnabled = repository.getGdacsAlertsEnabled(),
            isWmoAlertsEnabled = repository.getWmoAlertsEnabled(),
            isWmoAlwaysEnabled = repository.isWmoAlwaysEnabled(),
            isWmoTestEnabled = false,
            isMeteoAlarmAlertsEnabled = repository.getMeteoAlarmAlertsEnabled(),
            isMeteoAlarmTestEnabled = false,
            isKikikuruTestEnabled = repository.getKikikuruTestEnabled()
        )
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var weatherStreamJob: Job? = null

    init {
        observeWeatherForCity(_uiState.value.selectedCity)
        refreshCurrentLocation()
        observeSavedCities()
        val ctx = repository.context
        if (ctx != null) {
            WeatherBackgroundUpdateManager.init(ctx)
        }
        viewModelScope.launch {
            staticNowcastOverride.collect { override ->
                _uiState.update { it.copy(precipitationNowcastOverride = override) }
            }
        }
        viewModelScope.launch {
            staticGlobalAlertsOverride.collect { override ->
                _uiState.update { it.copy(globalAlertsOverride = override) }
            }
        }
    }

    private fun observeSavedCities() {
        viewModelScope.launch {
            repository.getSavedCitiesStream().collect { cities ->
                _uiState.value = _uiState.value.copy(savedCities = cities)
                val ctx = repository.context
                if (ctx != null) {
                    val maxAlert = JmaAlertRepository.evaluateAlertsForSavedCities(ctx, cities)
                    _uiState.value = _uiState.value.copy(savedCitiesMaxAlert = maxAlert)
                }
            }
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun selectCity(city: City, fromSearch: Boolean = false) {
        val current = _uiState.value.selectedCity
        val defaultCity = _uiState.value.defaultCity
        val currentRecent = _uiState.value.recentSearches

        val isDefault = city.id == defaultCity.id ||
                (city.latitude == defaultCity.latitude && city.longitude == defaultCity.longitude)

        // ユーザーリクエスト：+ボタン以外では地名登録されないようにする（savedCitiesは変更しない）
        // 検索から選ばれた場合、または現在地以外の都市が選ばれた場合は最近検索した項目（最大2件）にのみ追加
        val updatedRecent = if (!isDefault) {
            (listOf(city) + currentRecent.filterNot { it.id == city.id || (it.name == city.name && it.country == city.country) }).take(2)
        } else {
            currentRecent
        }

        if (current.id != city.id || current.latitude != city.latitude || current.longitude != city.longitude) {
            _uiState.value = _uiState.value.copy(
                selectedCity = city,
                recentSearches = updatedRecent,
                selectedHour = null,
                showCitySheet = false,
                errorMessage = null
            )
            repository.setLastSelectedCity(city)
            observeWeatherForCity(city)
        } else {
            _uiState.value = _uiState.value.copy(
                recentSearches = updatedRecent,
                showCitySheet = false
            )
        }
    }

    /**
     * +マーク等による明示的な地名登録/解除トグル（Roomデータベースに永続化）
     */
    fun toggleSaveCity(city: City) {
        val defaultCity = _uiState.value.defaultCity
        val isDefault = city.id == defaultCity.id ||
                (city.latitude == defaultCity.latitude && city.longitude == defaultCity.longitude)
        if (isDefault) return // 現在地は固定

        viewModelScope.launch {
            repository.toggleSaveCity(city)
        }
    }

    fun saveCity(city: City) {
        val defaultCity = _uiState.value.defaultCity
        val isDefault = city.id == defaultCity.id ||
                (city.latitude == defaultCity.latitude && city.longitude == defaultCity.longitude)
        if (isDefault) return

        viewModelScope.launch {
            repository.saveCity(city)
        }
    }

    fun removeSavedCity(city: City) {
        viewModelScope.launch {
            repository.removeSavedCity(city)
        }
    }

    fun searchCities(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        _uiState.value = _uiState.value.copy(isSearching = true)
        val currentLanguage = _uiState.value.language
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(150)
            val results = repository.searchCities(trimmed, currentLanguage)
            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isSearching = false
            )
        }
    }

    fun toggleTemperatureUnit() {
        val nextUnit = if (_uiState.value.temperatureUnit == TemperatureUnit.CELSIUS) {
            TemperatureUnit.FAHRENHEIT
        } else {
            TemperatureUnit.CELSIUS
        }
        _uiState.value = _uiState.value.copy(temperatureUnit = nextUnit)
    }

    fun setLanguage(language: AppLanguage) {
        repository.setLanguage(language)
        _uiState.value = _uiState.value.copy(language = language)
        java.util.Locale.setDefault(language.locale)
        repository.context?.let { ctx ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val localeManager = ctx.getSystemService(android.app.LocaleManager::class.java)
                localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(language.code)
            }
            val config = ctx.resources.configuration
            config.setLocale(language.locale)
            config.setLayoutDirection(language.locale)
            @Suppress("DEPRECATION")
            ctx.resources.updateConfiguration(config, ctx.resources.displayMetrics)
        }
    }

    fun toggleLanguage() {
        val nextLang = if (_uiState.value.language == AppLanguage.JAPANESE) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.JAPANESE
        }
        setLanguage(nextLang)
    }

    fun toggleWeatherAdaptiveTheme() {
        val currentPreset = _uiState.value.themePreset
        val nextPreset = if (currentPreset == ThemePreset.WEATHER) {
            ThemePreset.SKY_OCEAN
        } else {
            ThemePreset.WEATHER
        }
        setThemePreset(nextPreset)
    }

    fun setWeatherAdaptiveTheme(enabled: Boolean) {
        if (enabled && _uiState.value.themePreset != ThemePreset.WEATHER) {
            setThemePreset(ThemePreset.WEATHER)
        } else if (!enabled && _uiState.value.themePreset == ThemePreset.WEATHER) {
            setThemePreset(ThemePreset.SKY_OCEAN)
        }
    }

    fun setThemePreset(preset: ThemePreset) {
        if (_uiState.value.themePreset != preset) {
            _uiState.value = _uiState.value.copy(
                themePreset = preset,
                weatherAdaptiveThemeEnabled = preset == ThemePreset.WEATHER
            )
            repository.setThemePreset(preset)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        if (_uiState.value.themeMode != mode) {
            _uiState.value = _uiState.value.copy(themeMode = mode)
            repository.setThemeMode(mode)
        }
    }

    fun setFrostedGlassEnabled(enabled: Boolean) {
        if (_uiState.value.isFrostedGlassEnabled != enabled) {
            _uiState.update { it.copy(isFrostedGlassEnabled = enabled) }
            repository.setFrostedGlassEnabled(enabled)
        }
    }

    fun toggleFrostedGlassEnabled() {
        val next = !_uiState.value.isFrostedGlassEnabled
        setFrostedGlassEnabled(next)
    }

    fun setColorfulShapesEnabled(enabled: Boolean) {
        if (_uiState.value.isColorfulShapesEnabled != enabled) {
            _uiState.update { it.copy(isColorfulShapesEnabled = enabled) }
            repository.setColorfulShapesEnabled(enabled)
        }
    }

    fun toggleColorfulShapesEnabled() {
        val next = !_uiState.value.isColorfulShapesEnabled
        setColorfulShapesEnabled(next)
    }

    fun setRadarDebugZoomEnabled(enabled: Boolean) {
        if (_uiState.value.isRadarDebugZoomEnabled != enabled) {
            _uiState.value = _uiState.value.copy(isRadarDebugZoomEnabled = enabled)
            repository.setRadarDebugZoomEnabled(enabled)
        }
    }

    fun toggleRadarDebugZoom(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.isRadarDebugZoomEnabled
        setRadarDebugZoomEnabled(next)
    }

    fun setRadarDebugSourceEnabled(enabled: Boolean) {
        if (_uiState.value.isRadarDebugSourceEnabled != enabled) {
            _uiState.value = _uiState.value.copy(isRadarDebugSourceEnabled = enabled)
            repository.setRadarDebugSourceEnabled(enabled)
        }
    }

    fun toggleRadarDebugSource(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.isRadarDebugSourceEnabled
        setRadarDebugSourceEnabled(next)
    }

    fun setLiveAlertNotificationEnabled(enabled: Boolean) {
        if (_uiState.value.isLiveAlertNotificationEnabled != enabled) {
            _uiState.value = _uiState.value.copy(isLiveAlertNotificationEnabled = enabled)
            repository.setLiveAlertNotificationEnabled(enabled)
            val ctx = repository.context
            if (ctx != null) {
                if (!enabled) {
                    KikikuruNotificationManager.cancelAll(ctx)
                    PrecipitationLiveNotificationManager.cancel(ctx)
                } else {
                    val defaultCity = _uiState.value.defaultCity
                    val isCurrentLocation = _uiState.value.selectedCity.id == defaultCity.id ||
                            (_uiState.value.selectedCity.latitude == defaultCity.latitude && _uiState.value.selectedCity.longitude == defaultCity.longitude)
                    if (isCurrentLocation) {
                        _uiState.value.currentAlert?.let { alert ->
                            KikikuruNotificationManager.updateLiveAlertNotification(ctx, alert)
                        }
                        val nowcast = _uiState.value.precipitationNowcastOverride ?: _uiState.value.weatherData?.precipitationNowcast
                        if (nowcast != null) {
                            PrecipitationLiveNotificationManager.updateRainLiveNotification(ctx, nowcast, _uiState.value.selectedCity.name)
                        }
                    }
                }
            }
        }
    }

    fun toggleLiveAlertNotification(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.isLiveAlertNotificationEnabled
        setLiveAlertNotificationEnabled(next)
    }

    fun selectAlertNotificationThreshold(threshold: AlertNotificationThreshold) {
        if (_uiState.value.alertNotificationThreshold != threshold) {
            _uiState.value = _uiState.value.copy(alertNotificationThreshold = threshold)
            repository.setAlertNotificationThreshold(threshold)
            val ctx = repository.context
            if (ctx != null && _uiState.value.isLiveAlertNotificationEnabled) {
                val defaultCity = _uiState.value.defaultCity
                val isCurrentLocation = _uiState.value.selectedCity.id == defaultCity.id ||
                        (_uiState.value.selectedCity.latitude == defaultCity.latitude && _uiState.value.selectedCity.longitude == defaultCity.longitude)
                if (isCurrentLocation) {
                    _uiState.value.currentAlert?.let { alert ->
                        KikikuruNotificationManager.updateLiveAlertNotification(ctx, alert)
                    }
                }
            }
        }
    }

    fun setGdacsAlertsEnabled(enabled: Boolean) {
        if (_uiState.value.isGdacsAlertsEnabled == enabled) return
        repository.setGdacsAlertsEnabled(enabled)
        val currentWeather = _uiState.value.weatherData
        _uiState.value = _uiState.value.copy(
            isGdacsAlertsEnabled = enabled,
            weatherData = if (enabled || currentWeather == null) currentWeather else {
                currentWeather.copy(globalAlerts = currentWeather.globalAlerts.filterNot { it.source == AlertSource.GDACS })
            }
        )
        if (enabled) refreshWeather()
    }

    fun toggleGdacsAlerts(enabled: Boolean? = null) {
        setGdacsAlertsEnabled(enabled ?: !_uiState.value.isGdacsAlertsEnabled)
    }

    /**
     * WMO (世界気象機関) 公式気象警報の有効化/無効化
     */
    fun setWmoAlertsEnabled(enabled: Boolean) {
        if (_uiState.value.isWmoAlertsEnabled == enabled) return
        repository.setWmoAlertsEnabled(enabled)
        val currentWeather = _uiState.value.weatherData
        _uiState.value = _uiState.value.copy(
            isWmoAlertsEnabled = enabled,
            weatherData = if (enabled || currentWeather == null) currentWeather else {
                currentWeather.copy(globalAlerts = currentWeather.globalAlerts.filterNot { it.source == AlertSource.WMO })
            }
        )
        if (enabled) refreshWeather()
    }

    fun toggleWmoAlerts(enabled: Boolean? = null) {
        setWmoAlertsEnabled(enabled ?: !_uiState.value.isWmoAlertsEnabled)
    }

    /**
     * 「常にWMOを有効化（日本・米国でも有効化）」のトグル設定
     */
    fun setWmoAlwaysEnabled(enabled: Boolean) {
        if (_uiState.value.isWmoAlwaysEnabled == enabled) return
        repository.setWmoAlwaysEnabled(enabled)
        _uiState.value = _uiState.value.copy(isWmoAlwaysEnabled = enabled)
        refreshWeather()
    }

    fun toggleWmoAlways(enabled: Boolean? = null) {
        setWmoAlwaysEnabled(enabled ?: !_uiState.value.isWmoAlwaysEnabled)
    }

    fun setMeteoAlarmAlertsEnabled(enabled: Boolean) {
        if (_uiState.value.isMeteoAlarmAlertsEnabled == enabled) return
        repository.setMeteoAlarmAlertsEnabled(enabled)
        val currentWeather = _uiState.value.weatherData
        _uiState.value = _uiState.value.copy(
            isMeteoAlarmAlertsEnabled = enabled,
            weatherData = if (enabled || currentWeather == null) currentWeather else {
                currentWeather.copy(globalAlerts = currentWeather.globalAlerts.filterNot { it.source == AlertSource.METEOALARM })
            }
        )
        if (enabled) refreshWeather()
    }

    fun toggleMeteoAlarmAlerts(enabled: Boolean? = null) {
        setMeteoAlarmAlertsEnabled(enabled ?: !_uiState.value.isMeteoAlarmAlertsEnabled)
    }

    private fun rebuildGlobalAlertOverrides(nwsOn: Boolean, gdacsOn: Boolean, wmoOn: Boolean, meteoAlarmOn: Boolean): List<GlobalAlertItem>? {
        val overrides = mutableListOf<GlobalAlertItem>()
        if (nwsOn) overrides.addAll(NwsAlertRepository.createSampleAlerts())
        if (gdacsOn) overrides.addAll(GdacsAlertRepository.createSampleAlerts())
        if (wmoOn) overrides.addAll(WmoAlertRepository.createSampleAlerts())
        if (meteoAlarmOn) overrides.addAll(MeteoAlarmRepository.createSampleAlerts())
        return if (overrides.isEmpty()) null else overrides
    }

    /**
     * NWS (米国気象局) 警報テスト
     */
    fun setNwsTestEnabled(enabled: Boolean) {
        val next = enabled
        val gdacsOn = _uiState.value.isGdacsTestEnabled
        val wmoOn = _uiState.value.isWmoTestEnabled
        val meteoAlarmOn = _uiState.value.isMeteoAlarmTestEnabled
        val finalOverrides = rebuildGlobalAlertOverrides(next, gdacsOn, wmoOn, meteoAlarmOn)
        com.example.expressiveweather.ui.screens.SettingsDataHolder.isNwsTestEnabled = next
        staticGlobalAlertsOverride.value = finalOverrides
        _uiState.value = _uiState.value.copy(
            isNwsTestEnabled = next,
            globalAlertsOverride = finalOverrides
        )
    }

    fun toggleNwsTest(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.isNwsTestEnabled
        setNwsTestEnabled(next)
    }

    /**
     * GDACS (全球災害警報) テスト
     */
    fun setGdacsTestEnabled(enabled: Boolean) {
        val next = enabled
        val nwsOn = _uiState.value.isNwsTestEnabled
        val wmoOn = _uiState.value.isWmoTestEnabled
        val meteoAlarmOn = _uiState.value.isMeteoAlarmTestEnabled
        val finalOverrides = rebuildGlobalAlertOverrides(nwsOn, next, wmoOn, meteoAlarmOn)
        com.example.expressiveweather.ui.screens.SettingsDataHolder.isGdacsTestEnabled = next
        staticGlobalAlertsOverride.value = finalOverrides
        _uiState.value = _uiState.value.copy(
            isGdacsTestEnabled = next,
            globalAlertsOverride = finalOverrides
        )
    }

    fun toggleGdacsTest(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.isGdacsTestEnabled
        setGdacsTestEnabled(next)
    }

    /**
     * WMO (世界気象機関) 警報テスト (赤: 特別警戒 / 橙: 警戒 / 黄: 注意)
     */
    fun setWmoTestEnabled(enabled: Boolean) {
        val next = enabled
        val nwsOn = _uiState.value.isNwsTestEnabled
        val gdacsOn = _uiState.value.isGdacsTestEnabled
        val meteoAlarmOn = _uiState.value.isMeteoAlarmTestEnabled
        val finalOverrides = rebuildGlobalAlertOverrides(nwsOn, gdacsOn, next, meteoAlarmOn)
        com.example.expressiveweather.ui.screens.SettingsDataHolder.isWmoTestEnabled = next
        staticGlobalAlertsOverride.value = finalOverrides
        _uiState.value = _uiState.value.copy(
            isWmoTestEnabled = next,
            globalAlertsOverride = finalOverrides
        )
    }

    fun toggleWmoTest(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.isWmoTestEnabled
        setWmoTestEnabled(next)
    }

    /**
     * MeteoAlarm (欧州気象警報ネットワーク) 警報テスト (赤: 特別警戒 / 橙: 警戒 / 黄: 注意)
     */
    fun setMeteoAlarmTestEnabled(enabled: Boolean) {
        val next = enabled
        val nwsOn = _uiState.value.isNwsTestEnabled
        val gdacsOn = _uiState.value.isGdacsTestEnabled
        val wmoOn = _uiState.value.isWmoTestEnabled
        val finalOverrides = rebuildGlobalAlertOverrides(nwsOn, gdacsOn, wmoOn, next)
        com.example.expressiveweather.ui.screens.SettingsDataHolder.isMeteoAlarmTestEnabled = next
        staticGlobalAlertsOverride.value = finalOverrides
        _uiState.value = _uiState.value.copy(
            isMeteoAlarmTestEnabled = next,
            globalAlertsOverride = finalOverrides
        )
    }

    fun toggleMeteoAlarmTest(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.isMeteoAlarmTestEnabled
        setMeteoAlarmTestEnabled(next)
    }

    /**
     * 雨雲予測・カウントダウンのテスト状態を順繰りに切り替え
     * (今夜は雨が降るでしょう → 雨雲接近中(あと20分) → 降雨中(あと35分) → 雨雲なし → 実際の予報)
     */
    fun cyclePrecipitationDemoState() {
        val currentOverride = _uiState.value.precipitationNowcastOverride
        val nextStatus = when (currentOverride?.status) {
            null -> RainStatus.RAIN_EXPECTED_TONIGHT
            RainStatus.RAIN_EXPECTED_TONIGHT -> RainStatus.RAIN_APPROACHING
            RainStatus.RAIN_APPROACHING -> RainStatus.RAINING
            RainStatus.RAINING -> null
            else -> null
        }
        val newOverride = if (nextStatus != null) {
            PrecipitationAnalyzer.createMock(nextStatus)
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(precipitationNowcastOverride = newOverride)
        staticNowcastOverride.value = newOverride

        val ctx = repository.context
        if (ctx != null && _uiState.value.isLiveAlertNotificationEnabled) {
            if (newOverride != null && (newOverride.status == RainStatus.RAIN_APPROACHING || newOverride.status == RainStatus.RAINING)) {
                PrecipitationLiveNotificationManager.sendTestNotification(ctx, newOverride.status)
            } else if (newOverride == null) {
                val actual = _uiState.value.weatherData?.precipitationNowcast
                if (actual != null) {
                    PrecipitationLiveNotificationManager.updateRainLiveNotification(ctx, actual, _uiState.value.selectedCity.name)
                } else {
                    PrecipitationLiveNotificationManager.cancel(ctx)
                }
            } else {
                PrecipitationLiveNotificationManager.cancel(ctx)
            }
        }
    }

    fun setPrecipitationDemoState(status: RainStatus?) {
        val newOverride = if (status != null) PrecipitationAnalyzer.createMock(status) else null
        _uiState.value = _uiState.value.copy(precipitationNowcastOverride = newOverride)
        val ctx = repository.context
        if (ctx != null && _uiState.value.isLiveAlertNotificationEnabled) {
            if (newOverride != null && (newOverride.status == RainStatus.RAIN_APPROACHING || newOverride.status == RainStatus.RAINING)) {
                PrecipitationLiveNotificationManager.sendTestNotification(ctx, newOverride.status)
            } else {
                PrecipitationLiveNotificationManager.cancel(ctx)
            }
        }
    }

    fun setKikikuruTestEnabled(enabled: Boolean) {
        if (_uiState.value.isKikikuruTestEnabled != enabled) {
            _uiState.value = _uiState.value.copy(isKikikuruTestEnabled = enabled)
            repository.setKikikuruTestEnabled(enabled)
        }
    }

    fun toggleKikikuruTest(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.isKikikuruTestEnabled
        setKikikuruTestEnabled(next)
    }

    fun setWindSpeedUnit(unit: WindSpeedUnit) {
        if (_uiState.value.windSpeedUnit != unit) {
            _uiState.value = _uiState.value.copy(windSpeedUnit = unit)
            repository.setWindSpeedUnit(unit)
        }
    }

    fun setWeatherUpdateInterval(interval: WeatherUpdateInterval) {
        if (_uiState.value.weatherUpdateInterval != interval) {
            _uiState.value = _uiState.value.copy(weatherUpdateInterval = interval)
            repository.setWeatherUpdateInterval(interval)
            val ctx = repository.context
            if (ctx != null) {
                WeatherBackgroundUpdateManager.getInstance(ctx).restart()
            }
        }
    }

    fun clearAppCache() {
        viewModelScope.launch {
            repository.clearCache()
            refreshWeather()
        }
    }

    fun selectHourlyForecast(hourly: HourlyForecast) {
        val currentSelected = _uiState.value.selectedHour
        _uiState.value = if (currentSelected?.hour == hourly.hour) {
            _uiState.value.copy(selectedHour = null)
        } else {
            _uiState.value.copy(selectedHour = hourly)
        }
    }

    fun openCitySheet(initialQuery: String = "") {
        _uiState.value = _uiState.value.copy(showCitySheet = true)
        if (initialQuery.isNotBlank()) {
            searchCities(initialQuery)
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    fun closeCitySheet() {
        _uiState.value = _uiState.value.copy(showCitySheet = false, searchResults = emptyList())
    }

    fun openSettingsSheet() {
        _uiState.value = _uiState.value.copy(showSettingsSheet = true)
    }

    fun closeSettingsSheet() {
        _uiState.value = _uiState.value.copy(showSettingsSheet = false)
    }

    private var locationUpdatesStop: (() -> Unit)? = null

    /**
     * 端末の移動を検知した際の現在地更新・天候再取得ハンドラ
     */
    private suspend fun handleNewLocation(loc: Location) {
        val provider = locationProvider ?: return
        val city = provider.resolveCity(loc)
        val currentCity = _uiState.value.selectedCity
        val defaultCity = _uiState.value.defaultCity

        val isViewingCurrent = currentCity.id == "current" ||
                (currentCity.latitude == defaultCity.latitude && currentCity.longitude == defaultCity.longitude)

        val distanceMeters = FloatArray(1).also {
            Location.distanceBetween(
                defaultCity.latitude, defaultCity.longitude,
                city.latitude, city.longitude,
                it
            )
        }[0]

        // 50m以上の移動、または地名（区・市町村）が変わった場合は新現在地として認識
        val hasMovedSignificantly = distanceMeters >= 50f || defaultCity.name != city.name

        _uiState.value = _uiState.value.copy(
            defaultCity = city,
            selectedCity = if (isViewingCurrent) city else currentCity
        )

        // 現在地画面を表示中かつ位置が変化した（または未ロード）場合は即座に天候を再取得
        if (isViewingCurrent && (hasMovedSignificantly || _uiState.value.weatherData == null)) {
            observeWeatherForCity(city)
        } else if (hasMovedSignificantly && PrecipitationLiveNotificationManager.currentActiveNowcast != null) {
            // 他画面選択中であってもLive Updates通知がアクティブな場合は、現在地移動に伴い即座にナウキャストを再取得して追従
            viewModelScope.launch {
                try {
                    val weather = repository.refresh(city)
                    val nowcast = weather.precipitationNowcast
                    repository.context?.let { ctx ->
                        if (nowcast != null && nowcast.isLiveActive && (nowcast.minutesRemaining ?: 0) > 0) {
                            PrecipitationLiveNotificationManager.updateRainLiveNotification(ctx, nowcast, city.name, loc)
                        } else {
                            PrecipitationLiveNotificationManager.onRainStoppedAtNewLocation(ctx, city.name)
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 位置移動（市区町村変更）検知時のキキクル警戒情報即時再判定トリガー
        repository.context?.let { ctx ->
            KikikuruBackgroundMonitor.getInstance(ctx).onLocationUpdated(loc, city.name, city.country)
        }
    }

    /**
     * 移動（GPS/ネットワーク）に連動する継続的な位置情報リスナーを開始
     */
    fun startContinuousLocationUpdates() {
        val provider = locationProvider ?: return
        if (!provider.hasLocationPermission()) return
        locationUpdatesStop?.invoke()
        locationUpdatesStop = provider.startLocationUpdates { loc ->
            viewModelScope.launch {
                handleNewLocation(loc)
            }
        }
    }

    /**
     * バックグラウンド移行時等に位置情報の監視を一時停止
     */
    fun stopContinuousLocationUpdates() {
        locationUpdatesStop?.invoke()
        locationUpdatesStop = null
    }

    override fun onCleared() {
        super.onCleared()
        locationUpdatesStop?.invoke()
    }

    fun refreshCurrentLocation(forceFresh: Boolean = false) {
        val provider = locationProvider ?: return
        if (!provider.hasLocationPermission()) return
        startContinuousLocationUpdates()
        viewModelScope.launch {
            val loc = provider.getCurrentLocation(forceFresh = forceFresh)
            if (loc != null) {
                handleNewLocation(loc)
            }
        }
    }

    fun selectCurrentLocation() {
        val defaultCity = _uiState.value.defaultCity
        if (_uiState.value.selectedCity != defaultCity) {
            selectCity(defaultCity)
        }
        refreshCurrentLocation(forceFresh = true)
    }

    fun refreshWeather() {
        val currentCity = _uiState.value.selectedCity
        val defaultCity = _uiState.value.defaultCity
        val isViewingCurrent = currentCity.id == "current" ||
                (currentCity.latitude == defaultCity.latitude && currentCity.longitude == defaultCity.longitude)

        _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
        viewModelScope.launch {
            try {
                // 現在地画面でのプルダウン更新時は、まず最新のGPS位置を強制再取得して地点を更新
                val targetCity = if (isViewingCurrent) {
                    val provider = locationProvider
                    if (provider != null && provider.hasLocationPermission()) {
                        val loc = provider.getCurrentLocation(forceFresh = true)
                        if (loc != null) {
                            val resolved = provider.resolveCity(loc)
                            _uiState.value = _uiState.value.copy(
                                defaultCity = resolved,
                                selectedCity = resolved
                            )
                            resolved
                        } else currentCity
                    } else currentCity
                } else currentCity

                val data = repository.refresh(targetCity)
                _uiState.value = _uiState.value.copy(
                    weatherData = data,
                    isRefreshing = false,
                    isLoading = false,
                    isFromCache = false,
                    isStale = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                val hasData = _uiState.value.weatherData != null
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    isLoading = false,
                    isFromCache = hasData,
                    isStale = hasData,
                    errorMessage = if (hasData) null else "気象データの更新に失敗しました: ${e.localizedMessage ?: "通信エラー"}"
                )
            }
        }
    }

    private fun observeWeatherForCity(city: City) {
        weatherStreamJob?.cancel()
        weatherStreamJob = viewModelScope.launch {
            repository.getWeatherStream(city).collect { result ->
                when (result) {
                    is WeatherStreamResult.Loading -> {
                        _uiState.update { current ->
                            val currentData = current.weatherData
                            val isSameLocation = currentData != null &&
                                    LocationKeyNormalizer.normalize(currentData.city.latitude, currentData.city.longitude) ==
                                    LocationKeyNormalizer.normalize(city.latitude, city.longitude)
                            if (!isSameLocation) {
                                current.copy(
                                    weatherData = null,
                                    isLoading = true,
                                    errorMessage = null
                                )
                            } else {
                                current
                            }
                        }
                    }
                    is WeatherStreamResult.Success -> {
                        val alert = result.data.alertInfo
                        _uiState.update { current ->
                            current.copy(
                                weatherData = result.data,
                                currentAlert = alert,
                                isLoading = false,
                                isRefreshing = false,
                                isFromCache = result.isFromCache,
                                isStale = result.isStale,
                                errorMessage = null
                            )
                        }
                        if (_uiState.value.isLiveAlertNotificationEnabled) {
                            val defaultCity = _uiState.value.defaultCity
                            val isCurrentLocation = city.id == defaultCity.id ||
                                    (city.latitude == defaultCity.latitude && city.longitude == defaultCity.longitude)
                            if (isCurrentLocation) {
                                repository.context?.let { ctx ->
                                    if (alert != null) {
                                        KikikuruNotificationManager.updateLiveAlertNotification(ctx, alert, city.name)
                                    }
                                    val nowcast = _uiState.value.precipitationNowcastOverride ?: result.data.precipitationNowcast
                                    if (nowcast != null && nowcast.isLiveActive && (nowcast.minutesRemaining ?: 0) > 0) {
                                        val loc = locationProvider?.getCurrentLocation(forceFresh = false)
                                        PrecipitationLiveNotificationManager.updateRainLiveNotification(ctx, nowcast, city.name, loc)
                                    } else if (PrecipitationLiveNotificationManager.currentActiveNowcast != null) {
                                        PrecipitationLiveNotificationManager.onRainStoppedAtNewLocation(ctx, city.name)
                                    }
                                }
                            }
                        }
                    }
                    is WeatherStreamResult.Error -> {
                        _uiState.update { current ->
                            val hasCurrentData = current.weatherData != null
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                isFromCache = hasCurrentData,
                                isStale = hasCurrentData,
                                errorMessage = if (hasCurrentData) null else result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadWeatherForCity(city: City) {
        observeWeatherForCity(city)
    }

    /**
     * ベントグリッドのタイル並び順を更新
     */
    fun updateBentoOrder(newOrder: List<BentoCardType>) {
        _uiState.update { it.copy(bentoCardOrder = newOrder) }
    }

    /**
     * タイル詳細画面（ボトムシート）を開く
     */
    fun openCardDetail(card: BentoCardType) {
        _uiState.value = _uiState.value.copy(selectedDetailCard = card)
    }

    /**
     * タイル詳細画面を閉じる
     */
    fun closeCardDetail() {
        _uiState.value = _uiState.value.copy(selectedDetailCard = null)
    }

    companion object {
        val staticNowcastOverride = MutableStateFlow<com.example.expressiveweather.data.PrecipitationNowcast?>(null)
        val staticGlobalAlertsOverride = MutableStateFlow<List<GlobalAlertItem>?>(null)
    }
}
