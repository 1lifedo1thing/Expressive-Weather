package io.github.nemynew.expressiveweather.data

import io.github.nemynew.expressiveweather.theme.AccentColorMode
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.widget.MaterialWidgetShape
import io.github.nemynew.expressiveweather.ui.widget.WidgetConfig
import io.github.nemynew.expressiveweather.ui.widget.WidgetStyle
import io.github.nemynew.expressiveweather.ui.widget.WidgetUiModelBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class RussianLocalizationTest {

    private fun containsCyrillic(text: String): Boolean {
        return text.any { ch -> ch in '\u0400'..'\u04FF' }
    }

    @Test
    fun appLanguage_resolvesRussianCodesLocaleAndDisplayNames() {
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromCode("ru"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromCode("ru-RU"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromCode("RU"))
        assertEquals("Русский", AppLanguage.RUSSIAN.displayName)
        assertEquals(Locale.forLanguageTag("ru-RU"), AppLanguage.RUSSIAN.locale)

        // Full 9x9 getDisplayNameIn matrix
        for (target in AppLanguage.entries) {
            for (displayLang in AppLanguage.entries) {
                val name = target.getDisplayNameIn(displayLang)
                assertTrue(
                    "Display name of $target in $displayLang should not be blank",
                    name.isNotBlank()
                )
            }
        }
        assertEquals("Russian", AppLanguage.RUSSIAN.getDisplayNameIn(AppLanguage.ENGLISH))
        assertEquals("ロシア語", AppLanguage.RUSSIAN.getDisplayNameIn(AppLanguage.JAPANESE))
        assertEquals("Русский", AppLanguage.RUSSIAN.getDisplayNameIn(AppLanguage.RUSSIAN))
        assertEquals("Японский", AppLanguage.JAPANESE.getDisplayNameIn(AppLanguage.RUSSIAN))
    }

    @Test
    fun weatherConditionsDirectionsDaysAndTime_localizeToRussian() {
        WeatherCondition.entries.forEach { cond ->
            val label = cond.getLocalizedLabel(AppLanguage.RUSSIAN)
            val tagline = cond.getLocalizedTagline(AppLanguage.RUSSIAN)
            assertTrue("Condition $cond label should contain Cyrillic: $label", containsCyrillic(label))
            assertTrue("Condition $cond tagline should contain Cyrillic: $tagline", containsCyrillic(tagline))
        }

        // All 16 compass points in Cyrillic
        val expectedDirections = listOf(
            "北" to "С",
            "北北東" to "ССВ",
            "北東" to "СВ",
            "東北東" to "ВСВ",
            "東" to "В",
            "東南東" to "ВЮВ",
            "南東" to "ЮВ",
            "南南東" to "ЮЮВ",
            "南" to "Ю",
            "南南西" to "ЮЮЗ",
            "南西" to "ЮЗ",
            "西南西" to "ЗЮЗ",
            "西" to "З",
            "西北西" to "ЗСЗ",
            "北西" to "СЗ",
            "北北西" to "ССЗ"
        )
        for ((jaDir, ruDir) in expectedDirections) {
            assertEquals("Wind direction $jaDir", ruDir, getLocalizedWindDirection(jaDir, AppLanguage.RUSSIAN))
        }

        // Day labels round-trip
        val dayPairs = listOf(
            Triple("Today", "今日", "Сегодня"),
            Triple("Tomorrow", "明日", "Завтра"),
            Triple("Mon", "月", "Пн"),
            Triple("Tue", "火", "Вт"),
            Triple("Wed", "水", "Ср"),
            Triple("Thu", "木", "Чт"),
            Triple("Fri", "金", "Пт"),
            Triple("Sat", "土", "Сб"),
            Triple("Sun", "日", "Вс")
        )
        for ((en, ja, ru) in dayPairs) {
            assertEquals(ru, getLocalizedDayLabel(en, AppLanguage.RUSSIAN))
            assertEquals(ru, getLocalizedDayLabel(ja, AppLanguage.RUSSIAN))
            assertEquals(en, getLocalizedDayLabel(ru, AppLanguage.ENGLISH))
            assertEquals(ja, getLocalizedDayLabel(ru, AppLanguage.JAPANESE))
        }

        assertEquals("Сейчас", getLocalizedTimeLabel("Now", AppLanguage.RUSSIAN))
        assertEquals("Сейчас", getLocalizedTimeLabel("現在", AppLanguage.RUSSIAN))
        assertEquals("Now", getLocalizedTimeLabel("Сейчас", AppLanguage.ENGLISH))
        assertEquals("現在", getLocalizedTimeLabel("Сейчас", AppLanguage.JAPANESE))
    }

    @Test
    fun cityLocalizationAndMasterSearch_supportRussianCitiesAndTokyo() {
        val tokyo = City("tokyo", "東京都", "Tokyo", "日本", 35.6895, 139.6917)
        val moscow = City("moscow", "モスクワ", "Moscow", "ロシア", 55.7558, 37.6173)
        val spb = City("spb", "サンクトペテルブルク", "Saint Petersburg", "ロシア", 59.9343, 30.3351)

        assertEquals("Токио", tokyo.localizedName(AppLanguage.RUSSIAN))
        assertEquals("Москва", moscow.localizedName(AppLanguage.RUSSIAN))
        assertEquals("Санкт-Петербург", spb.localizedName(AppLanguage.RUSSIAN))

        val moscowResults = CitySearchEngine.searchLocalMaster("Москва", AppLanguage.RUSSIAN)
        assertTrue("Master search should find Moscow for 'Москва'", moscowResults.isNotEmpty())
        assertEquals("Москва", moscowResults.first().localizedName(AppLanguage.RUSSIAN))
        assertEquals("Россия", moscowResults.first().country)
        assertFalse(isLocationOrCountryInJapan(moscowResults.first().country, moscowResults.first().latitude, moscowResults.first().longitude))

        val spbResults = CitySearchEngine.searchLocalMaster("Санкт-Петербург", AppLanguage.RUSSIAN)
        assertTrue("Master search should find Saint Petersburg", spbResults.isNotEmpty())
        assertEquals("Санкт-Петербург", spbResults.first().localizedName(AppLanguage.RUSSIAN))
    }

    @Test
    fun unitsThemesWidgetsAlertsMoonDrynessAndNowcast_localizeToRussian() {
        // Russia standard wind unit is m/s
        assertEquals("m/s", WindSpeedUnit.FOLLOW_LANGUAGE.getDisplayUnit(AppLanguage.RUSSIAN))

        // Update intervals & haptics
        WeatherUpdateInterval.entries.forEach { interval ->
            assertTrue(containsCyrillic(interval.getLabel(AppLanguage.RUSSIAN)))
        }
        HapticIntensity.entries.forEach { haptic ->
            assertTrue(containsCyrillic(haptic.getLabel(AppLanguage.RUSSIAN)))
        }

        // Theme modes, presets, accent color modes
        ThemeMode.entries.forEach { mode ->
            assertTrue(containsCyrillic(mode.displayName(AppLanguage.RUSSIAN)))
        }
        ThemePreset.entries.forEach { preset ->
            assertTrue(containsCyrillic(preset.displayName(AppLanguage.RUSSIAN)))
        }
        AccentColorMode.entries.forEach { mode ->
            assertTrue(containsCyrillic(mode.title(AppLanguage.RUSSIAN)))
        }

        // Widgets
        WidgetStyle.entries.forEach { style ->
            assertTrue(containsCyrillic(style.getDescription(AppLanguage.RUSSIAN)))
        }
        assertTrue(containsCyrillic(MaterialWidgetShape.PILL.getDisplayName(AppLanguage.RUSSIAN)))

        val moscow = City("moscow", "Москва", "Moscow", "Россия", 55.7558, 37.6173)
        val placeholder = WidgetUiModelBuilder.build(
            config = WidgetConfig(appWidgetId = 777),
            weather = null,
            language = AppLanguage.RUSSIAN,
            temperatureUnit = TemperatureUnit.CELSIUS,
            windSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
            fallbackCity = moscow,
            isRefreshing = false
        )
        assertEquals("Москва", placeholder.cityName)
        assertTrue(containsCyrillic(placeholder.conditionLabel))

        // JMA Warnings, Kikikuru, Global Alerts, Bug/Feature categories
        val heavyRain = JmaWarningMaster.getLocalizedWarningName("大雨警報", AppLanguage.RUSSIAN)
        assertTrue("Expected Russian JMA warning, got: $heavyRain", containsCyrillic(heavyRain))
        assertTrue(containsCyrillic(AlertLevel.LEVEL_4.getTitle(AppLanguage.RUSSIAN)))
        assertTrue(containsCyrillic(DisasterType.LANDSLIDE.getLabel(AppLanguage.RUSSIAN)))
        assertTrue(containsCyrillic(GlobalAlertItem.translateNwsEvent("Tornado Warning", AppLanguage.RUSSIAN)))
        assertTrue(containsCyrillic(GlobalAlertItem.translateGdacsEvent("Test", "TC", AppLanguage.RUSSIAN)))
        assertTrue(containsCyrillic(GlobalAlertItem.translateWmoEvent("Heavy Rain", AppLanguage.RUSSIAN)))
        assertTrue(containsCyrillic(GlobalAlertItem.translateMeteoAlarmEvent("Wind", AppLanguage.RUSSIAN)))

        BugReportCategory.entries.forEach { cat ->
            assertTrue(containsCyrillic(cat.label(AppLanguage.RUSSIAN)))
        }
        FeatureRequestCategory.entries.forEach { cat ->
            assertTrue(containsCyrillic(cat.label(AppLanguage.RUSSIAN)))
        }

        // Moon phase & Dryness index & Precipitation nowcast
        val moon = MoonPhaseCalculator.calculate(AppLanguage.RUSSIAN)
        assertTrue(containsCyrillic(moon.phaseName))

        DrynessRank.entries.forEach { rank ->
            assertTrue(containsCyrillic(rank.label(AppLanguage.RUSSIAN)))
            assertTrue(containsCyrillic(rank.guidance(AppLanguage.RUSSIAN)))
        }

        val mockNowcast = PrecipitationAnalyzer.createMock(RainStatus.RAIN_APPROACHING)
        val ruHeadline = mockNowcast.localizedHeadlines[AppLanguage.RUSSIAN.code].orEmpty()
        assertTrue("Expected Russian nowcast headline, got: $ruHeadline", containsCyrillic(ruHeadline))
        assertNotEquals(mockNowcast.localizedHeadlines[AppLanguage.ENGLISH.code], ruHeadline)

        // RussianStrings spot-checks
        assertTrue(containsCyrillic(RussianStrings.settingsTitle))
        assertTrue(containsCyrillic(RussianStrings.dataSourcesTitle))
        assertTrue(containsCyrillic(RussianStrings.changeLanguageTitle))
    }
}
