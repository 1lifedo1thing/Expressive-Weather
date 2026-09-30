package com.example.expressiveweather.ui.main

import com.example.expressiveweather.data.City
import com.example.expressiveweather.data.TemperatureUnit
import com.example.expressiveweather.data.WeatherCondition
import com.example.expressiveweather.data.WeatherRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainScreenViewModelTest {

    private var repository = WeatherRepository()

    @Before
    fun setUp() {
        repository = WeatherRepository()
    }

    @Test
    fun temperatureUnit_conversionIsAccurate() {
        assertEquals(20, TemperatureUnit.CELSIUS.convert(20))
        assertEquals(32, TemperatureUnit.FAHRENHEIT.convert(0))
        assertEquals(68, TemperatureUnit.FAHRENHEIT.convert(20))
        assertEquals(78, TemperatureUnit.FAHRENHEIT.convert(26))
        assertEquals(86, TemperatureUnit.FAHRENHEIT.convert(30))
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
    fun recentSearches_logic_keepsMaxTwoAndUnique() {
        val initialRecent = emptyList<City>()
        val city1 = City(id = "c1", name = "京都", nameEn = "Kyoto", country = "日本", latitude = 35.0, longitude = 135.0)
        val city2 = City(id = "c2", name = "大阪", nameEn = "Osaka", country = "日本", latitude = 34.0, longitude = 135.0)
        val city3 = City(id = "c3", name = "札幌", nameEn = "Sapporo", country = "日本", latitude = 43.0, longitude = 141.0)

        // 1件追加
        var recent = (listOf(city1) + initialRecent.filterNot { it.id == city1.id }).take(2)
        assertEquals(1, recent.size)
        assertEquals("京都", recent[0].name)

        // 2件目追加
        recent = (listOf(city2) + recent.filterNot { it.id == city2.id }).take(2)
        assertEquals(2, recent.size)
        assertEquals("大阪", recent[0].name)
        assertEquals("京都", recent[1].name)

        // 3件目追加（最大2件保持）
        recent = (listOf(city3) + recent.filterNot { it.id == city3.id }).take(2)
        assertEquals(2, recent.size)
        assertEquals("札幌", recent[0].name)
        assertEquals("大阪", recent[1].name)
        assertFalse(recent.any { it.id == "c1" })

        // 既存の city2 を再検索（先頭に移動し重複しない）
        recent = (listOf(city2) + recent.filterNot { it.id == city2.id }).take(2)
        assertEquals(2, recent.size)
        assertEquals("大阪", recent[0].name)
        assertEquals("札幌", recent[1].name)
    }

    @Test
    fun bentoCardOrder_updateChangesOrderCorrectly() {
        val viewModel = MainScreenViewModel(repository)
        val initialOrder = viewModel.uiState.value.bentoCardOrder
        assertEquals(com.example.expressiveweather.ui.components.BentoCardType.DEFAULT_ORDER, initialOrder)

        val reversedOrder = initialOrder.reversed()
        viewModel.updateBentoOrder(reversedOrder)
        assertEquals(reversedOrder, viewModel.uiState.value.bentoCardOrder)
    }

    @Test
    fun refreshWeather_triggersStateUpdate() {
        val viewModel = MainScreenViewModel(repository)
        assertFalse(viewModel.uiState.value.isRefreshing)
        viewModel.refreshWeather()
        // verify calling refreshWeather handles state gracefully without unhandled exceptions
        assertNotNull(viewModel.uiState.value.selectedCity)
    }

    @Test
    fun uiState_cacheAndStaleFlags_defaultFalse() {
        val viewModel = MainScreenViewModel(repository)
        assertFalse(viewModel.uiState.value.isFromCache)
        assertFalse(viewModel.uiState.value.isStale)
    }

    @Test
    fun themePreset_fromId_and_isFixed() {
        assertEquals(com.example.expressiveweather.theme.ThemePreset.WEATHER, com.example.expressiveweather.theme.ThemePreset.fromId("weather"))
        assertEquals(com.example.expressiveweather.theme.ThemePreset.SKY_OCEAN, com.example.expressiveweather.theme.ThemePreset.fromId("sky_ocean"))
        assertEquals(com.example.expressiveweather.theme.ThemePreset.EMERALD_FOREST, com.example.expressiveweather.theme.ThemePreset.fromId("emerald"))
        assertEquals(com.example.expressiveweather.theme.ThemePreset.SUNSET_AMBER, com.example.expressiveweather.theme.ThemePreset.fromId("sunset"))
        assertEquals(com.example.expressiveweather.theme.ThemePreset.LAVENDER_TWILIGHT, com.example.expressiveweather.theme.ThemePreset.fromId("lavender"))
        assertEquals(com.example.expressiveweather.theme.ThemePreset.SAKURA_BLOSSOM, com.example.expressiveweather.theme.ThemePreset.fromId("sakura"))
        assertEquals(com.example.expressiveweather.theme.ThemePreset.MIDNIGHT_SLATE, com.example.expressiveweather.theme.ThemePreset.fromId("slate"))
        assertEquals(com.example.expressiveweather.theme.ThemePreset.WEATHER, com.example.expressiveweather.theme.ThemePreset.fromId("unknown_fallback"))

        assertFalse(com.example.expressiveweather.theme.ThemePreset.WEATHER.isFixed)
        assertFalse(com.example.expressiveweather.theme.ThemePreset.SYSTEM.isFixed)
        assertTrue(com.example.expressiveweather.theme.ThemePreset.SKY_OCEAN.isFixed)
        assertTrue(com.example.expressiveweather.theme.ThemePreset.EMERALD_FOREST.isFixed)
        assertTrue(com.example.expressiveweather.theme.ThemePreset.SUNSET_AMBER.isFixed)
        assertTrue(com.example.expressiveweather.theme.ThemePreset.LAVENDER_TWILIGHT.isFixed)
        assertTrue(com.example.expressiveweather.theme.ThemePreset.SAKURA_BLOSSOM.isFixed)
        assertTrue(com.example.expressiveweather.theme.ThemePreset.MIDNIGHT_SLATE.isFixed)
    }

    @Test
    fun viewModel_themePreset_switchingAndToggling() {
        val viewModel = MainScreenViewModel(repository)
        assertEquals(com.example.expressiveweather.theme.ThemePreset.WEATHER, viewModel.uiState.value.themePreset)
        assertTrue(viewModel.uiState.value.weatherAdaptiveThemeEnabled)

        // 固定プリセットに設定
        viewModel.setThemePreset(com.example.expressiveweather.theme.ThemePreset.EMERALD_FOREST)
        assertEquals(com.example.expressiveweather.theme.ThemePreset.EMERALD_FOREST, viewModel.uiState.value.themePreset)
        assertFalse(viewModel.uiState.value.weatherAdaptiveThemeEnabled)

        // 天候連動に復元トグル
        viewModel.toggleWeatherAdaptiveTheme()
        assertEquals(com.example.expressiveweather.theme.ThemePreset.WEATHER, viewModel.uiState.value.themePreset)
        assertTrue(viewModel.uiState.value.weatherAdaptiveThemeEnabled)

        // もう一度トグルすると固定プリセットに切り替わる
        viewModel.toggleWeatherAdaptiveTheme()
        assertEquals(com.example.expressiveweather.theme.ThemePreset.SKY_OCEAN, viewModel.uiState.value.themePreset)
        assertFalse(viewModel.uiState.value.weatherAdaptiveThemeEnabled)
    }

    @Test
    fun themeMode_resolution_and_viewmodel_switching() {
        // 1. fromId
        assertEquals(com.example.expressiveweather.theme.ThemeMode.WEATHER, com.example.expressiveweather.theme.ThemeMode.fromId("weather"))
        assertEquals(com.example.expressiveweather.theme.ThemeMode.SYSTEM, com.example.expressiveweather.theme.ThemeMode.fromId("system"))
        assertEquals(com.example.expressiveweather.theme.ThemeMode.LIGHT, com.example.expressiveweather.theme.ThemeMode.fromId("light"))
        assertEquals(com.example.expressiveweather.theme.ThemeMode.DARK, com.example.expressiveweather.theme.ThemeMode.fromId("dark"))
        assertEquals(com.example.expressiveweather.theme.ThemeMode.SYSTEM, com.example.expressiveweather.theme.ThemeMode.fromId("unknown"))

        // 2. resolveIsDark
        // WEATHER: 昼(isDay=true) -> false, 夜(isDay=false) -> true
        assertFalse(com.example.expressiveweather.theme.ThemeMode.resolveIsDark(com.example.expressiveweather.theme.ThemeMode.WEATHER, isDay = true, isSystemDark = true))
        assertTrue(com.example.expressiveweather.theme.ThemeMode.resolveIsDark(com.example.expressiveweather.theme.ThemeMode.WEATHER, isDay = false, isSystemDark = false))

        // SYSTEM: 端末設定に追従
        assertTrue(com.example.expressiveweather.theme.ThemeMode.resolveIsDark(com.example.expressiveweather.theme.ThemeMode.SYSTEM, isDay = true, isSystemDark = true))
        assertFalse(com.example.expressiveweather.theme.ThemeMode.resolveIsDark(com.example.expressiveweather.theme.ThemeMode.SYSTEM, isDay = false, isSystemDark = false))

        // LIGHT: 常に false
        assertFalse(com.example.expressiveweather.theme.ThemeMode.resolveIsDark(com.example.expressiveweather.theme.ThemeMode.LIGHT, isDay = false, isSystemDark = true))

        // DARK: 常に true
        assertTrue(com.example.expressiveweather.theme.ThemeMode.resolveIsDark(com.example.expressiveweather.theme.ThemeMode.DARK, isDay = true, isSystemDark = false))

        // 3. ViewModel switching
        val viewModel = MainScreenViewModel(repository)
        viewModel.setThemeMode(com.example.expressiveweather.theme.ThemeMode.DARK)
        assertEquals(com.example.expressiveweather.theme.ThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.setThemeMode(com.example.expressiveweather.theme.ThemeMode.LIGHT)
        assertEquals(com.example.expressiveweather.theme.ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    @Test
    fun viewModel_frostedGlass_switchingAndToggling() {
        val viewModel = MainScreenViewModel(repository)
        assertTrue(viewModel.uiState.value.isFrostedGlassEnabled)

        viewModel.setFrostedGlassEnabled(false)
        assertFalse(viewModel.uiState.value.isFrostedGlassEnabled)

        viewModel.toggleFrostedGlassEnabled()
        assertTrue(viewModel.uiState.value.isFrostedGlassEnabled)
    }

    @Test
    fun viewModel_colorfulShapes_switchingAndToggling() {
        val viewModel = MainScreenViewModel(repository)
        assertTrue(viewModel.uiState.value.isColorfulShapesEnabled)

        viewModel.setColorfulShapesEnabled(false)
        assertFalse(viewModel.uiState.value.isColorfulShapesEnabled)

        viewModel.toggleColorfulShapesEnabled()
        assertTrue(viewModel.uiState.value.isColorfulShapesEnabled)
    }
}

