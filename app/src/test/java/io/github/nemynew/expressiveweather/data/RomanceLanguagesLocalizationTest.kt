package io.github.nemynew.expressiveweather.data

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

class RomanceLanguagesLocalizationTest {

    private val romanceLanguages = listOf(
        AppLanguage.SPANISH,
        AppLanguage.FRENCH,
        AppLanguage.PORTUGUESE
    )

    @Test
    fun appLanguage_resolvesCodesLocalesAndDisplayNames() {
        // Spanish
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromCode("es"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromCode("es-ES"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromCode("es-MX"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromCode("ES"))
        assertEquals("Español", AppLanguage.SPANISH.displayName)
        assertEquals(Locale("es", "ES"), AppLanguage.SPANISH.locale)

        // French
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromCode("fr"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromCode("fr-FR"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromCode("fr-CA"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromCode("FR"))
        assertEquals("Français", AppLanguage.FRENCH.displayName)
        assertEquals(Locale.FRANCE, AppLanguage.FRENCH.locale)

        // Portuguese
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromCode("pt"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromCode("pt-BR"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromCode("pt-PT"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromCode("PT"))
        assertEquals("Português", AppLanguage.PORTUGUESE.displayName)
        assertEquals(Locale("pt", "BR"), AppLanguage.PORTUGUESE.locale)

        // Full 8x8 getDisplayNameIn matrix non-blank & accurate spot-checks
        for (target in AppLanguage.entries) {
            for (displayLang in AppLanguage.entries) {
                val name = target.getDisplayNameIn(displayLang)
                assertTrue(
                    "Display name of $target in $displayLang should not be blank",
                    name.isNotBlank()
                )
            }
        }
        assertEquals("Spanish", AppLanguage.SPANISH.getDisplayNameIn(AppLanguage.ENGLISH))
        assertEquals("French", AppLanguage.FRENCH.getDisplayNameIn(AppLanguage.ENGLISH))
        assertEquals("Portuguese", AppLanguage.PORTUGUESE.getDisplayNameIn(AppLanguage.ENGLISH))
        assertEquals("スペイン語", AppLanguage.SPANISH.getDisplayNameIn(AppLanguage.JAPANESE))
        assertEquals("フランス語", AppLanguage.FRENCH.getDisplayNameIn(AppLanguage.JAPANESE))
        assertEquals("ポルトガル語", AppLanguage.PORTUGUESE.getDisplayNameIn(AppLanguage.JAPANESE))
        assertEquals("Español", AppLanguage.SPANISH.getDisplayNameIn(AppLanguage.SPANISH))
        assertEquals("Français", AppLanguage.FRENCH.getDisplayNameIn(AppLanguage.FRENCH))
        assertEquals("Português", AppLanguage.PORTUGUESE.getDisplayNameIn(AppLanguage.PORTUGUESE))
    }

    @Test
    fun weatherConditions_localizeLabelsAndTaglines() {
        for (lang in romanceLanguages) {
            for (cond in WeatherCondition.entries) {
                val label = cond.getLocalizedLabel(lang)
                val tagline = cond.getLocalizedTagline(lang)
                val enLabel = cond.getLocalizedLabel(AppLanguage.ENGLISH)
                val enTagline = cond.getLocalizedTagline(AppLanguage.ENGLISH)
                assertTrue("$cond label in $lang should not be blank", label.isNotBlank())
                assertTrue("$cond tagline in $lang should not be blank", tagline.isNotBlank())
                assertNotEquals("$cond label in $lang should not fall back to English", enLabel, label)
                assertNotEquals("$cond tagline in $lang should not fall back to English", enTagline, tagline)
            }
        }
    }

    @Test
    fun windDirections_all16CompassPointsLocalizeToRomanceLanguages() {
        val expected = listOf(
            "北" to "N",
            "北北東" to "NNE",
            "北東" to "NE",
            "東北東" to "ENE",
            "東" to "E",
            "東南東" to "ESE",
            "南東" to "SE",
            "南南東" to "SSE",
            "南" to "S",
            "南南西" to "SSO",
            "南西" to "SO",
            "西南西" to "OSO",
            "西" to "O",
            "西北西" to "ONO",
            "北西" to "NO",
            "北北西" to "NNO"
        )
        for (lang in romanceLanguages) {
            for ((jaDir, expectedDir) in expected) {
                assertEquals(
                    "Direction $jaDir in $lang",
                    expectedDir,
                    getLocalizedWindDirection(jaDir, lang)
                )
            }
        }
    }

    @Test
    fun dayAndTimeLabels_roundTripAcrossEsFrPt() {
        val dayMatrix = listOf(
            listOf("Today", "今日", "Hoy", "Aujourd'hui", "Hoje"),
            listOf("Tomorrow", "明日", "Mañana", "Demain", "Amanhã"),
            listOf("Mon", "月", "Lun", "Lun", "Seg"),
            listOf("Tue", "火", "Mar", "Mar", "Ter"),
            listOf("Wed", "水", "Mié", "Mer", "Qua"),
            listOf("Thu", "木", "Jue", "Jeu", "Qui"),
            listOf("Fri", "金", "Vie", "Ven", "Sex"),
            listOf("Sat", "土", "Sáb", "Sam", "Sáb"),
            listOf("Sun", "日", "Dom", "Dim", "Dom")
        )
        for ((en, ja, es, fr, pt) in dayMatrix) {
            assertEquals(es, getLocalizedDayLabel(en, AppLanguage.SPANISH))
            assertEquals(fr, getLocalizedDayLabel(en, AppLanguage.FRENCH))
            assertEquals(pt, getLocalizedDayLabel(en, AppLanguage.PORTUGUESE))

            assertEquals(es, getLocalizedDayLabel(ja, AppLanguage.SPANISH))
            assertEquals(fr, getLocalizedDayLabel(ja, AppLanguage.FRENCH))
            assertEquals(pt, getLocalizedDayLabel(ja, AppLanguage.PORTUGUESE))

            // Round-trip back to English and Japanese
            assertEquals(en, getLocalizedDayLabel(es, AppLanguage.ENGLISH))
            assertEquals(en, getLocalizedDayLabel(fr, AppLanguage.ENGLISH))
            assertEquals(en, getLocalizedDayLabel(pt, AppLanguage.ENGLISH))
            assertEquals(ja, getLocalizedDayLabel(es, AppLanguage.JAPANESE))
            assertEquals(ja, getLocalizedDayLabel(fr, AppLanguage.JAPANESE))
            assertEquals(ja, getLocalizedDayLabel(pt, AppLanguage.JAPANESE))
        }

        // Time label round-trip
        assertEquals("Ahora", getLocalizedTimeLabel("Now", AppLanguage.SPANISH))
        assertEquals("Maintenant", getLocalizedTimeLabel("Now", AppLanguage.FRENCH))
        assertEquals("Agora", getLocalizedTimeLabel("Now", AppLanguage.PORTUGUESE))
        assertEquals("Ahora", getLocalizedTimeLabel("現在", AppLanguage.SPANISH))
        assertEquals("Maintenant", getLocalizedTimeLabel("現在", AppLanguage.FRENCH))
        assertEquals("Agora", getLocalizedTimeLabel("現在", AppLanguage.PORTUGUESE))

        assertEquals("Now", getLocalizedTimeLabel("Ahora", AppLanguage.ENGLISH))
        assertEquals("Now", getLocalizedTimeLabel("Maintenant", AppLanguage.ENGLISH))
        assertEquals("Now", getLocalizedTimeLabel("Agora", AppLanguage.ENGLISH))
        assertEquals("現在", getLocalizedTimeLabel("Ahora", AppLanguage.JAPANESE))
        assertEquals("現在", getLocalizedTimeLabel("Maintenant", AppLanguage.JAPANESE))
        assertEquals("現在", getLocalizedTimeLabel("Agora", AppLanguage.JAPANESE))
    }

    @Test
    fun cityLocalizationAndMasterSearch_supportRomanceCitiesAndTokyo() {
        val tokyo = City("tokyo", "東京都", "Tokyo", "日本", 35.6895, 139.6917)
        val newYork = City("ny", "ニューヨーク", "New York", "アメリカ合衆国", 40.7128, -74.0060)
        val london = City("london", "ロンドン", "London", "イギリス", 51.5074, -0.1278)
        val madrid = City("madrid", "Madrid", "Madrid", "España", 40.4168, -3.7038)
        val paris = City("paris", "Paris", "Paris", "France", 48.8566, 2.3522)
        val saoPaulo = City("saopaulo", "São Paulo", "São Paulo", "Brasil", -23.5505, -46.6333)

        assertEquals("Tokio", tokyo.localizedName(AppLanguage.SPANISH))
        assertEquals("Tokyo", tokyo.localizedName(AppLanguage.FRENCH))
        assertEquals("Tóquio", tokyo.localizedName(AppLanguage.PORTUGUESE))

        assertEquals("Nueva York", newYork.localizedName(AppLanguage.SPANISH))
        assertEquals("New York", newYork.localizedName(AppLanguage.FRENCH))
        assertEquals("Nova York", newYork.localizedName(AppLanguage.PORTUGUESE))

        assertEquals("Londres", london.localizedName(AppLanguage.SPANISH))
        assertEquals("Londres", london.localizedName(AppLanguage.FRENCH))
        assertEquals("Londres", london.localizedName(AppLanguage.PORTUGUESE))

        assertEquals("Madrid", madrid.localizedName(AppLanguage.SPANISH))
        assertEquals("Paris", paris.localizedName(AppLanguage.FRENCH))
        assertEquals("São Paulo", saoPaulo.localizedName(AppLanguage.PORTUGUESE))

        // Master dictionary searches
        val madridResults = CitySearchEngine.searchLocalMaster("Madrid", AppLanguage.SPANISH)
        assertTrue("Master search should find Madrid", madridResults.isNotEmpty())
        assertEquals("Madrid", madridResults.first().localizedName(AppLanguage.SPANISH))
        assertFalse(isLocationOrCountryInJapan(madridResults.first().country, madridResults.first().latitude, madridResults.first().longitude))

        val parisResults = CitySearchEngine.searchLocalMaster("Paris", AppLanguage.FRENCH)
        assertTrue("Master search should find Paris", parisResults.isNotEmpty())
        assertEquals("Paris", parisResults.first().localizedName(AppLanguage.FRENCH))
        assertFalse(isLocationOrCountryInJapan(parisResults.first().country, parisResults.first().latitude, parisResults.first().longitude))

        val saoPauloResults = CitySearchEngine.searchLocalMaster("São Paulo", AppLanguage.PORTUGUESE)
        assertTrue("Master search should find São Paulo", saoPauloResults.isNotEmpty())
        assertEquals("São Paulo", saoPauloResults.first().localizedName(AppLanguage.PORTUGUESE))

        val lisboaResults = CitySearchEngine.searchLocalMaster("Lisboa", AppLanguage.PORTUGUESE)
        assertTrue("Master search should find Lisboa", lisboaResults.isNotEmpty())
        assertTrue(
            "Lisboa localized name should contain Lisboa",
            lisboaResults.first().localizedName(AppLanguage.PORTUGUESE).contains("Lisboa", ignoreCase = true)
        )

        val tokyoEsResults = CitySearchEngine.searchLocalMaster("Tokio", AppLanguage.SPANISH)
        assertTrue("Master search should find Tokyo for 'Tokio'", tokyoEsResults.isNotEmpty())
        assertEquals("Tokio", tokyoEsResults.first().localizedName(AppLanguage.SPANISH))

        val tokyoFrResults = CitySearchEngine.searchLocalMaster("Tokyo", AppLanguage.FRENCH)
        assertTrue("Master search should find Tokyo for 'Tokyo' in FR", tokyoFrResults.isNotEmpty())
        assertEquals("Tokyo", tokyoFrResults.first().localizedName(AppLanguage.FRENCH))

        val tokyoPtResults = CitySearchEngine.searchLocalMaster("Tóquio", AppLanguage.PORTUGUESE)
        assertTrue("Master search should find Tokyo for 'Tóquio' in PT", tokyoPtResults.isNotEmpty())
        assertEquals("Tóquio", tokyoPtResults.first().localizedName(AppLanguage.PORTUGUESE))
    }

    @Test
    fun windUnitThemesWidgetsAlertsAndFeedback_localizeToEsFrPt() {
        // Wind speed unit follows km/h standard for ES, FR, PT
        for (lang in romanceLanguages) {
            assertEquals(
                "WindSpeedUnit.FOLLOW_LANGUAGE should be km/h for $lang",
                "km/h",
                WindSpeedUnit.FOLLOW_LANGUAGE.getDisplayUnit(lang)
            )
        }

        // ThemeMode & ThemePreset
        for (lang in romanceLanguages) {
            for (mode in ThemeMode.entries) {
                val name = mode.displayName(lang)
                assertTrue("ThemeMode.$mode in $lang should not be blank", name.isNotBlank())
                assertNotEquals(mode.displayName(AppLanguage.JAPANESE), name)
            }
            for (preset in ThemePreset.entries) {
                val name = preset.displayName(lang)
                assertTrue("ThemePreset.$preset in $lang should not be blank", name.isNotBlank())
                assertNotEquals(preset.displayName(AppLanguage.JAPANESE), name)
            }
        }

        // WidgetStyle, MaterialWidgetShape, WidgetUiModelBuilder
        val cities = mapOf(
            AppLanguage.SPANISH to City("madrid", "Madrid", "Madrid", "España", 40.4168, -3.7038),
            AppLanguage.FRENCH to City("paris", "Paris", "Paris", "France", 48.8566, 2.3522),
            AppLanguage.PORTUGUESE to City("saopaulo", "São Paulo", "São Paulo", "Brasil", -23.5505, -46.6333)
        )
        for (lang in romanceLanguages) {
            for (style in WidgetStyle.entries) {
                val display = style.getDisplayName(lang)
                val desc = style.getDescription(lang)
                assertTrue("WidgetStyle.$style displayName in $lang", display.isNotBlank())
                assertTrue("WidgetStyle.$style description in $lang", desc.isNotBlank())
                assertNotEquals(style.getDescription(AppLanguage.ENGLISH), desc)
            }
            for (shape in MaterialWidgetShape.entries) {
                val name = shape.getDisplayName(lang)
                assertTrue("MaterialWidgetShape.$shape in $lang", name.isNotBlank())
            }
            assertNotEquals(
                MaterialWidgetShape.PILL.getDisplayName(AppLanguage.JAPANESE),
                MaterialWidgetShape.PILL.getDisplayName(lang)
            )

            val city = cities.getValue(lang)
            val placeholder = WidgetUiModelBuilder.build(
                config = WidgetConfig(appWidgetId = 1000 + lang.ordinal),
                weather = null,
                language = lang,
                temperatureUnit = TemperatureUnit.CELSIUS,
                windSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
                fallbackCity = city,
                isRefreshing = false
            )
            assertEquals(city.localizedName(lang), placeholder.cityName)
            assertTrue("Widget loading label in $lang should not be blank", placeholder.conditionLabel.isNotBlank())
            assertNotEquals("Loading...", placeholder.conditionLabel)
        }

        // JmaWarningMaster
        for (lang in romanceLanguages) {
            val heavyRain = JmaWarningMaster.getLocalizedWarningName("大雨警報", lang)
            assertTrue("JMA 大雨警報 in $lang should not be blank", heavyRain.isNotBlank())
            assertNotEquals("大雨警報", heavyRain)
            assertNotEquals(JmaWarningMaster.getLocalizedWarningName("大雨警報", AppLanguage.ENGLISH), heavyRain)

            val phenomenon = JmaWarningMaster.getLocalizedPhenomenon("大雨警報", WarningCategory.WARNING, lang, "")
            assertTrue("JMA phenomenon in $lang should not be blank", phenomenon.isNotBlank())
            assertNotEquals(
                JmaWarningMaster.getLocalizedPhenomenon("大雨警報", WarningCategory.WARNING, AppLanguage.ENGLISH, ""),
                phenomenon
            )
        }

        // KikikuruModels
        for (lang in romanceLanguages) {
            for (level in AlertLevel.entries) {
                val title = level.getTitle(lang)
                assertTrue("AlertLevel.$level in $lang should not be blank", title.isNotBlank())
                assertNotEquals(level.getTitle(AppLanguage.JAPANESE), title)
            }
            assertNotEquals(AlertLevel.LEVEL_5.getTitle(AppLanguage.ENGLISH), AlertLevel.LEVEL_5.getTitle(lang))
            assertNotEquals(AlertLevel.LEVEL_3.getTitle(AppLanguage.ENGLISH), AlertLevel.LEVEL_3.getTitle(lang))
            for (type in DisasterType.entries) {
                val label = type.getLabel(lang)
                val shortLabel = type.getShortLabel(lang)
                assertTrue("DisasterType.$type label in $lang", label.isNotBlank())
                assertTrue("DisasterType.$type shortLabel in $lang", shortLabel.isNotBlank())
                assertNotEquals(type.getLabel(AppLanguage.ENGLISH), label)
            }
        }

        // BugReportCategory & FeatureRequestCategory
        for (lang in romanceLanguages) {
            for (cat in BugReportCategory.entries) {
                val label = cat.label(lang)
                assertTrue("BugReportCategory.$cat in $lang", label.isNotBlank())
                assertNotEquals(cat.label(AppLanguage.JAPANESE), label)
            }
            assertNotEquals(BugReportCategory.OTHER.label(AppLanguage.ENGLISH), BugReportCategory.OTHER.label(lang))
            for (cat in FeatureRequestCategory.entries) {
                val label = cat.label(lang)
                assertTrue("FeatureRequestCategory.$cat in $lang", label.isNotBlank())
                assertNotEquals(cat.label(AppLanguage.JAPANESE), label)
            }
            assertNotEquals(FeatureRequestCategory.RADAR.label(AppLanguage.ENGLISH), FeatureRequestCategory.RADAR.label(lang))
        }

        // AppStrings spot checks
        assertTrue(SpanishStrings.settingsTitle.isNotBlank())
        assertNotEquals(EnglishStrings.settingsTitle, SpanishStrings.settingsTitle)
        assertTrue(FrenchStrings.settingsTitle.isNotBlank())
        assertNotEquals(EnglishStrings.settingsTitle, FrenchStrings.settingsTitle)
        assertTrue(PortugueseStrings.settingsTitle.isNotBlank())
        assertNotEquals(EnglishStrings.settingsTitle, PortugueseStrings.settingsTitle)
    }
}
