package io.github.nemynew.expressiveweather.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.JmaLayerType
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.WeatherRepository
import io.github.nemynew.expressiveweather.data.isAllowedJapanRadarLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private var repository = WeatherRepository()
    private lateinit var viewModelStore: ViewModelStore

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModelStore = ViewModelStore()
        MainScreenViewModel.staticGlobalAlertsOverride.value = null
        MainScreenViewModel.staticNowcastOverride.value = null
        repository = WeatherRepository()
        repository.setThemePreset(io.github.nemynew.expressiveweather.theme.ThemePreset.WEATHER)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        MainScreenViewModel.staticGlobalAlertsOverride.value = null
        MainScreenViewModel.staticNowcastOverride.value = null
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MainScreenViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainScreenViewModel(repository) as T
            }
        }
        return ViewModelProvider(viewModelStore, factory)[MainScreenViewModel::class.java]
    }

    @Test
    fun temperatureUnit_conversionIsAccurate() {
        assertEquals(20, TemperatureUnit.CELSIUS.convert(20))
        assertEquals(32, TemperatureUnit.FAHRENHEIT.convert(0))
        assertEquals(68, TemperatureUnit.FAHRENHEIT.convert(20))
        assertEquals(79, TemperatureUnit.FAHRENHEIT.convert(26)) // 26 * 9/5 + 32 = 78.8 -> roundToInt() = 79
        assertEquals(86, TemperatureUnit.FAHRENHEIT.convert(30))
        assertEquals(23, TemperatureUnit.FAHRENHEIT.convert(-5)) // -5 * 1.8 + 32 = 23.0
        assertEquals(18, TemperatureUnit.FAHRENHEIT.convert(-8)) // -8 * 1.8 + 32 = 17.6 -> 18
    }

    @Test
    fun defaultCity_isTokyo() {
        val defaultCity = repository.defaultCity
        assertNotNull(defaultCity)
        assertEquals("東京", defaultCity.name)
        assertEquals("日本", defaultCity.country)
    }

    @Test
    fun weatherCondition_attributesAreValid() {
        assertTrue(WeatherCondition.RAIN.isPrecipitating)
        assertTrue(WeatherCondition.HEAVY_RAIN.isPrecipitating)
        assertTrue(WeatherCondition.THUNDERSTORM.isPrecipitating)
        assertTrue(!WeatherCondition.CLEAR.isPrecipitating)
        assertTrue(WeatherCondition.CLEAR.label.isNotEmpty())
    }

    @Test
    fun recentSearches_selectCity_keepsMaxTwoAndUnique() {
        val city1 = City(id = "c1", name = "京都", nameEn = "Kyoto", country = "日本", latitude = 35.0, longitude = 135.0)
        val city2 = City(id = "c2", name = "大阪", nameEn = "Osaka", country = "日本", latitude = 34.0, longitude = 135.0)
        val city3 = City(id = "c3", name = "札幌", nameEn = "Sapporo", country = "日本", latitude = 43.0, longitude = 141.0)

        val viewModel = createViewModel()
        viewModel.selectCity(city1, fromSearch = true)
        assertEquals(1, viewModel.uiState.value.recentSearches.size)
        assertEquals("京都", viewModel.uiState.value.recentSearches[0].name)

        viewModel.selectCity(city2, fromSearch = true)
        assertEquals(2, viewModel.uiState.value.recentSearches.size)
        assertEquals("大阪", viewModel.uiState.value.recentSearches[0].name)
        assertEquals("京都", viewModel.uiState.value.recentSearches[1].name)

        viewModel.selectCity(city3, fromSearch = true)
        assertEquals(2, viewModel.uiState.value.recentSearches.size)
        assertEquals("札幌", viewModel.uiState.value.recentSearches[0].name)
        assertEquals("大阪", viewModel.uiState.value.recentSearches[1].name)
        assertFalse(viewModel.uiState.value.recentSearches.any { it.id == "c1" })

        viewModel.selectCity(city2, fromSearch = true)
        assertEquals(2, viewModel.uiState.value.recentSearches.size)
        assertEquals("大阪", viewModel.uiState.value.recentSearches[0].name)
        assertEquals("札幌", viewModel.uiState.value.recentSearches[1].name)
    }

    @Test
    fun bentoCardOrder_updateChangesOrderCorrectly() {
        val viewModel = createViewModel()
        val initialOrder = viewModel.uiState.value.bentoCardOrder
        assertEquals(io.github.nemynew.expressiveweather.ui.components.BentoCardType.DEFAULT_ORDER, initialOrder)

        val reversedOrder = initialOrder.reversed()
        viewModel.updateBentoOrder(reversedOrder)
        assertEquals(reversedOrder, viewModel.uiState.value.bentoCardOrder)
    }

    @Test
    fun cyclePrecipitationDemoState_cyclesThroughExpectedStates() {
        val viewModel = createViewModel()
        assertEquals(null, viewModel.uiState.value.precipitationNowcastOverride)

        viewModel.cyclePrecipitationDemoState()
        assertEquals(
            io.github.nemynew.expressiveweather.data.RainStatus.RAIN_EXPECTED_TONIGHT,
            viewModel.uiState.value.precipitationNowcastOverride?.status
        )

        viewModel.cyclePrecipitationDemoState()
        assertEquals(
            io.github.nemynew.expressiveweather.data.RainStatus.RAIN_APPROACHING,
            viewModel.uiState.value.precipitationNowcastOverride?.status
        )

        viewModel.cyclePrecipitationDemoState()
        assertEquals(
            io.github.nemynew.expressiveweather.data.RainStatus.RAINING,
            viewModel.uiState.value.precipitationNowcastOverride?.status
        )

        viewModel.cyclePrecipitationDemoState()
        assertEquals(null, viewModel.uiState.value.precipitationNowcastOverride)
    }

    @Test
    fun selectCity_andRefresh_clearPrecipitationDemoOverride() {
        val viewModel = createViewModel()
        viewModel.cyclePrecipitationDemoState()
        assertEquals(
            io.github.nemynew.expressiveweather.data.RainStatus.RAIN_EXPECTED_TONIGHT,
            viewModel.uiState.value.precipitationNowcastOverride?.status
        )

        val osaka = City(id = "osaka", name = "大阪", nameEn = "Osaka", country = "日本", latitude = 34.6937, longitude = 135.5023)
        viewModel.selectCity(osaka)
        assertEquals(null, viewModel.uiState.value.precipitationNowcastOverride)
        assertEquals(null, MainScreenViewModel.staticNowcastOverride.value)

        viewModel.cyclePrecipitationDemoState()
        assertNotNull(viewModel.uiState.value.precipitationNowcastOverride)
        viewModel.refreshWeather()
        assertEquals(null, viewModel.uiState.value.precipitationNowcastOverride)
        assertEquals(null, MainScreenViewModel.staticNowcastOverride.value)

        viewModel.cyclePrecipitationDemoState()
        assertNotNull(viewModel.uiState.value.precipitationNowcastOverride)
        viewModel.clearAppCache()
        assertEquals(null, viewModel.uiState.value.precipitationNowcastOverride)
        assertEquals(null, MainScreenViewModel.staticNowcastOverride.value)
    }

    @Test
    fun uiState_cacheAndStaleFlags_defaultFalse() {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isFromCache)
        assertFalse(viewModel.uiState.value.isStale)
    }

    @Test
    fun themePreset_fromId_and_isFixed() {
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.WEATHER, io.github.nemynew.expressiveweather.theme.ThemePreset.fromId("weather"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.SKY_OCEAN, io.github.nemynew.expressiveweather.theme.ThemePreset.fromId("sky_ocean"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.EMERALD_FOREST, io.github.nemynew.expressiveweather.theme.ThemePreset.fromId("emerald"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.SUNSET_AMBER, io.github.nemynew.expressiveweather.theme.ThemePreset.fromId("sunset"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.LAVENDER_TWILIGHT, io.github.nemynew.expressiveweather.theme.ThemePreset.fromId("lavender"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.SAKURA_BLOSSOM, io.github.nemynew.expressiveweather.theme.ThemePreset.fromId("sakura"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.MIDNIGHT_SLATE, io.github.nemynew.expressiveweather.theme.ThemePreset.fromId("slate"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.WEATHER, io.github.nemynew.expressiveweather.theme.ThemePreset.fromId("unknown_fallback"))

        assertFalse(io.github.nemynew.expressiveweather.theme.ThemePreset.WEATHER.isFixed)
        assertFalse(io.github.nemynew.expressiveweather.theme.ThemePreset.SYSTEM.isFixed)
        assertTrue(io.github.nemynew.expressiveweather.theme.ThemePreset.SKY_OCEAN.isFixed)
        assertTrue(io.github.nemynew.expressiveweather.theme.ThemePreset.EMERALD_FOREST.isFixed)
        assertTrue(io.github.nemynew.expressiveweather.theme.ThemePreset.SUNSET_AMBER.isFixed)
        assertTrue(io.github.nemynew.expressiveweather.theme.ThemePreset.LAVENDER_TWILIGHT.isFixed)
        assertTrue(io.github.nemynew.expressiveweather.theme.ThemePreset.SAKURA_BLOSSOM.isFixed)
        assertTrue(io.github.nemynew.expressiveweather.theme.ThemePreset.MIDNIGHT_SLATE.isFixed)
    }

    @Test
    fun viewModel_themePreset_switchingAndToggling() {
        val viewModel = createViewModel()
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.WEATHER, viewModel.uiState.value.themePreset)
        assertTrue(viewModel.uiState.value.weatherAdaptiveThemeEnabled)

        // 固定プリセットに設定
        viewModel.setThemePreset(io.github.nemynew.expressiveweather.theme.ThemePreset.EMERALD_FOREST)
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.EMERALD_FOREST, viewModel.uiState.value.themePreset)
        assertFalse(viewModel.uiState.value.weatherAdaptiveThemeEnabled)

        // 天候連動に復元トグル
        viewModel.toggleWeatherAdaptiveTheme()
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.WEATHER, viewModel.uiState.value.themePreset)
        assertTrue(viewModel.uiState.value.weatherAdaptiveThemeEnabled)

        // もう一度トグルすると固定プリセットに切り替わる
        viewModel.toggleWeatherAdaptiveTheme()
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemePreset.SKY_OCEAN, viewModel.uiState.value.themePreset)
        assertFalse(viewModel.uiState.value.weatherAdaptiveThemeEnabled)
    }

    @Test
    fun themeMode_resolution_and_viewmodel_switching() {
        // 1. fromId
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemeMode.WEATHER, io.github.nemynew.expressiveweather.theme.ThemeMode.fromId("weather"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemeMode.SYSTEM, io.github.nemynew.expressiveweather.theme.ThemeMode.fromId("system"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemeMode.LIGHT, io.github.nemynew.expressiveweather.theme.ThemeMode.fromId("light"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemeMode.DARK, io.github.nemynew.expressiveweather.theme.ThemeMode.fromId("dark"))
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemeMode.WEATHER, io.github.nemynew.expressiveweather.theme.ThemeMode.fromId("unknown"))

        // 2. resolveIsDark
        // WEATHER: 昼(isDay=true) -> false, 夜(isDay=false) -> true
        assertFalse(io.github.nemynew.expressiveweather.theme.ThemeMode.resolveIsDark(io.github.nemynew.expressiveweather.theme.ThemeMode.WEATHER, isDay = true, isSystemDark = true))
        assertTrue(io.github.nemynew.expressiveweather.theme.ThemeMode.resolveIsDark(io.github.nemynew.expressiveweather.theme.ThemeMode.WEATHER, isDay = false, isSystemDark = false))

        // SYSTEM: 端末設定に追従
        assertTrue(io.github.nemynew.expressiveweather.theme.ThemeMode.resolveIsDark(io.github.nemynew.expressiveweather.theme.ThemeMode.SYSTEM, isDay = true, isSystemDark = true))
        assertFalse(io.github.nemynew.expressiveweather.theme.ThemeMode.resolveIsDark(io.github.nemynew.expressiveweather.theme.ThemeMode.SYSTEM, isDay = false, isSystemDark = false))

        // LIGHT: 常に false
        assertFalse(io.github.nemynew.expressiveweather.theme.ThemeMode.resolveIsDark(io.github.nemynew.expressiveweather.theme.ThemeMode.LIGHT, isDay = false, isSystemDark = true))

        // DARK: 常に true
        assertTrue(io.github.nemynew.expressiveweather.theme.ThemeMode.resolveIsDark(io.github.nemynew.expressiveweather.theme.ThemeMode.DARK, isDay = true, isSystemDark = false))

        // 3. ViewModel switching
        val viewModel = createViewModel()
        viewModel.setThemeMode(io.github.nemynew.expressiveweather.theme.ThemeMode.DARK)
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.setThemeMode(io.github.nemynew.expressiveweather.theme.ThemeMode.LIGHT)
        assertEquals(io.github.nemynew.expressiveweather.theme.ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    @Test
    fun viewModel_frostedGlass_switchingAndToggling() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isFrostedGlassEnabled)

        viewModel.setFrostedGlassEnabled(false)
        assertFalse(viewModel.uiState.value.isFrostedGlassEnabled)

        viewModel.toggleFrostedGlassEnabled()
        assertTrue(viewModel.uiState.value.isFrostedGlassEnabled)
    }

    @Test
    fun viewModel_colorfulShapes_switchingAndToggling() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isColorfulShapesEnabled)

        viewModel.setColorfulShapesEnabled(false)
        assertFalse(viewModel.uiState.value.isColorfulShapesEnabled)

        viewModel.toggleColorfulShapesEnabled()
        assertTrue(viewModel.uiState.value.isColorfulShapesEnabled)
    }

    @Test
    fun isAllowedJapanRadarLayer_filtersCorrectly() {
        // Allowed by default: 雨雲ナウキャスト, 雷ナウキャスト, キキクル危険度, 雪降雪量情報
        assertTrue(isAllowedJapanRadarLayer(JmaLayerType.HRPNS))
        assertTrue(isAllowedJapanRadarLayer(JmaLayerType.THNS))
        assertTrue(isAllowedJapanRadarLayer(JmaLayerType.LANDSLIDE))
        assertTrue(isAllowedJapanRadarLayer(JmaLayerType.HEAVY_RAIN))
        assertTrue(isAllowedJapanRadarLayer(JmaLayerType.INUNDATION))
        assertTrue(isAllowedJapanRadarLayer(JmaLayerType.FLOOD))
        assertTrue(isAllowedJapanRadarLayer(JmaLayerType.SNOW_DEPTH))
        assertTrue(isAllowedJapanRadarLayer(JmaLayerType.SNOWFALL_3H))
        assertTrue(isAllowedJapanRadarLayer(JmaLayerType.SNOWFALL_24H))

        // Disallowed / hidden unless debug is enabled
        assertFalse(isAllowedJapanRadarLayer(JmaLayerType.TRNS))
        assertFalse(isAllowedJapanRadarLayer(JmaLayerType.HIMAWARI_TRUECOLOR))
        assertFalse(isAllowedJapanRadarLayer(JmaLayerType.HIMAWARI_INFRARED))
        assertFalse(isAllowedJapanRadarLayer(JmaLayerType.WEATHER_DIST))
        assertFalse(isAllowedJapanRadarLayer(JmaLayerType.MAX_TEMP))
        assertFalse(isAllowedJapanRadarLayer(JmaLayerType.WAVE_HEIGHT))
        assertFalse(isAllowedJapanRadarLayer(JmaLayerType.GSI_PALE))
        assertFalse(isAllowedJapanRadarLayer(JmaLayerType.RASRF_1H))
    }

    @Test
    fun viewModel_showAllJapanRadarLayers_switchingAndToggling() {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.showAllJapanRadarLayers)

        viewModel.setShowAllJapanRadarLayers(true)
        assertTrue(viewModel.uiState.value.showAllJapanRadarLayers)

        viewModel.toggleShowAllJapanRadarLayers()
        assertFalse(viewModel.uiState.value.showAllJapanRadarLayers)

        viewModel.toggleShowAllJapanRadarLayers()
        assertTrue(viewModel.uiState.value.showAllJapanRadarLayers)
    }
}

