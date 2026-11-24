package io.github.nemynew.expressiveweather.data

import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.widget.MaterialWidgetShape
import io.github.nemynew.expressiveweather.ui.widget.WidgetConfig
import io.github.nemynew.expressiveweather.ui.widget.WidgetStyle
import io.github.nemynew.expressiveweather.ui.widget.WidgetUiModelBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class KoreanLocalizationTest {

    private fun containsHangul(text: String): Boolean {
        return text.any { ch -> ch in '\uAC00'..'\uD7A3' }
    }

    @Test
    fun appLanguage_resolvesKoreanCodesAndLocale() {
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromCode("ko"))
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromCode("ko-KR"))
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromCode("KO"))
        assertEquals("한국어", AppLanguage.KOREAN.displayName)
        assertEquals(Locale.KOREA, AppLanguage.KOREAN.locale)
        assertEquals("韓国語", AppLanguage.KOREAN.getDisplayNameIn(AppLanguage.JAPANESE))
        assertEquals("Korean", AppLanguage.KOREAN.getDisplayNameIn(AppLanguage.ENGLISH))
        assertEquals("한국어", AppLanguage.KOREAN.getDisplayNameIn(AppLanguage.KOREAN))
        assertEquals("일본어", AppLanguage.JAPANESE.getDisplayNameIn(AppLanguage.KOREAN))
    }

    @Test
    fun weatherConditionsDirectionsDaysAndTime_localizeToKorean() {
        WeatherCondition.entries.forEach { cond ->
            val label = cond.getLocalizedLabel(AppLanguage.KOREAN)
            val tagline = cond.getLocalizedTagline(AppLanguage.KOREAN)
            assertTrue("Condition $cond label should contain Hangul: $label", containsHangul(label))
            assertTrue("Condition $cond tagline should contain Hangul: $tagline", containsHangul(tagline))
        }

        assertEquals("북", getLocalizedWindDirection("北", AppLanguage.KOREAN))
        assertEquals("남동", getLocalizedWindDirection("南東", AppLanguage.KOREAN))
        assertEquals("북서", getLocalizedWindDirection("北西", AppLanguage.KOREAN))
        assertEquals("동북동", getLocalizedWindDirection("東北東", AppLanguage.KOREAN))

        assertEquals("오늘", getLocalizedDayLabel("Today", AppLanguage.KOREAN))
        assertEquals("내일", getLocalizedDayLabel("明日", AppLanguage.KOREAN))
        assertEquals("월", getLocalizedDayLabel("Mon", AppLanguage.KOREAN))
        assertEquals("일", getLocalizedDayLabel("日", AppLanguage.KOREAN))
        assertEquals("Today", getLocalizedDayLabel("오늘", AppLanguage.ENGLISH))
        assertEquals("今日", getLocalizedDayLabel("오늘", AppLanguage.JAPANESE))
        assertEquals("현재", getLocalizedTimeLabel("現在", AppLanguage.KOREAN))
        assertEquals("현재", getLocalizedTimeLabel("Now", AppLanguage.KOREAN))
        assertEquals("Now", getLocalizedTimeLabel("현재", AppLanguage.ENGLISH))
    }

    @Test
    fun cityLocalizationAndMasterDictionary_supportKoreanCitiesAndAliases() {
        val tokyo = City("tokyo", "東京都", "Tokyo", "日本", 35.6895, 139.6917)
        val seoul = City("seoul", "ソウル", "Seoul", "韓国", 37.5665, 126.9780)
        val busanNative = City("busan", "부산", "Busan", "대한민국", 35.1796, 129.0756)

        assertEquals("도쿄", tokyo.localizedName(AppLanguage.KOREAN))
        assertEquals("서울", seoul.localizedName(AppLanguage.KOREAN))
        assertEquals("부산", busanNative.localizedName(AppLanguage.KOREAN))

        val seoulResults = CitySearchEngine.searchLocalMaster("서울", AppLanguage.KOREAN)
        assertTrue("Master search should find Seoul for '서울'", seoulResults.isNotEmpty())
        assertEquals("서울", seoulResults.first().localizedName(AppLanguage.KOREAN))
        assertTrue(seoulResults.first().country.contains("대한민국"))

        val busanResults = CitySearchEngine.searchLocalMaster("부산", AppLanguage.KOREAN)
        assertTrue("Master search should find Busan for '부산'", busanResults.isNotEmpty())

        val jejuResults = CitySearchEngine.searchLocalMaster("제주", AppLanguage.KOREAN)
        assertTrue("Master search should find Jeju for '제주'", jejuResults.isNotEmpty())

        val tokyoKoResults = CitySearchEngine.searchLocalMaster("도쿄", AppLanguage.KOREAN)
        assertTrue("Master search should find Tokyo for '도쿄'", tokyoKoResults.isNotEmpty())
        assertEquals("도쿄", tokyoKoResults.first().localizedName(AppLanguage.KOREAN))
    }

    @Test
    fun unitsThemeWidgetsAlertsAndFeedback_localizeToKorean() {
        // Wind speed unit follows South Korean m/s standard
        assertEquals("m/s", WindSpeedUnit.FOLLOW_LANGUAGE.getDisplayUnit(AppLanguage.KOREAN))

        // Theme & presets
        ThemeMode.entries.forEach { mode ->
            assertTrue(containsHangul(mode.displayName(AppLanguage.KOREAN)))
        }
        ThemePreset.entries.forEach { preset ->
            assertTrue(containsHangul(preset.displayName(AppLanguage.KOREAN)))
        }

        // Widget styles & model
        WidgetStyle.entries.forEach { style ->
            assertTrue(containsHangul(style.getDescription(AppLanguage.KOREAN)))
        }
        assertTrue(containsHangul(MaterialWidgetShape.PILL.getDisplayName(AppLanguage.KOREAN)))

        val seoul = City("seoul", "서울", "Seoul", "대한민국", 37.5665, 126.9780)
        val placeholder = WidgetUiModelBuilder.build(
            config = WidgetConfig(appWidgetId = 999),
            weather = null,
            language = AppLanguage.KOREAN,
            temperatureUnit = TemperatureUnit.CELSIUS,
            windSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
            fallbackCity = seoul,
            isRefreshing = false
        )
        assertEquals("서울", placeholder.cityName)
        assertTrue(containsHangul(placeholder.conditionLabel))

        // JMA warnings, Kikikuru, Global alerts, and Bug Report categories
        val heavyRain = JmaWarningMaster.getLocalizedWarningName("大雨警報", AppLanguage.KOREAN)
        assertTrue("Expected Korean JMA warning translation, got: $heavyRain", containsHangul(heavyRain))

        assertTrue(containsHangul(AlertLevel.LEVEL_4.getTitle(AppLanguage.KOREAN)))
        assertTrue(containsHangul(DisasterType.LANDSLIDE.getLabel(AppLanguage.KOREAN)))

        BugReportCategory.entries.forEach { cat ->
            assertTrue(containsHangul(cat.label(AppLanguage.KOREAN)))
        }
        FeatureRequestCategory.entries.forEach { cat ->
            assertTrue(containsHangul(cat.label(AppLanguage.KOREAN)))
        }

        // Korean city should NOT be classified as Japan region bucket
        assertFalse(isLocationOrCountryInJapan(seoul.country, seoul.latitude, seoul.longitude))
        assertNotNull(KoreanStrings.settingsTitle)
        assertTrue(containsHangul(KoreanStrings.settingsTitle))
    }
}
