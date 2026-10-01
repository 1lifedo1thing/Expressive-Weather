package com.example.expressiveweather.data

import com.example.expressiveweather.ui.components.WeatherWarningType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WmoAlertRepositoryTest {

    @Test
    fun wmoGuard_blocksJapanUsAndMeteoAlarmCoverage_unlessAlwaysEnabled() = runBlocking {
        val tokyo = City(id = "tokyo", name = "東京", nameEn = "Tokyo", country = "日本", latitude = 35.6895, longitude = 139.6917)
        val newYork = City(id = "ny", name = "ニューヨーク", nameEn = "New York", country = "United States", latitude = 40.7128, longitude = -74.0060)
        val paris = City(id = "paris", name = "パリ", nameEn = "Paris", country = "France", latitude = 48.8566, longitude = 2.3522)
        val berlin = City(id = "berlin", name = "ベルリン", nameEn = "Berlin", country = "Germany", latitude = 52.5200, longitude = 13.4050)
        val kyiv = City(id = "kyiv", name = "キーウ", nameEn = "Kyiv", country = "Ukraine", latitude = 50.4501, longitude = 30.5234)
        val telAviv = City(id = "telaviv", name = "テルアビブ", nameEn = "Tel Aviv", country = "Israel", latitude = 32.0853, longitude = 34.7818)

        // 1. alwaysEnabled = false (デフォルト):
        // 日本（気象庁直接）、米国（NWS直接）、欧州39カ国（MeteoAlarm直接）はWMO対象外
        assertTrue(WmoAlertRepository.isCityExcludedFromWmo(tokyo, alwaysEnabled = false))
        assertTrue(WmoAlertRepository.isCityExcludedFromWmo(newYork, alwaysEnabled = false))
        assertTrue(WmoAlertRepository.isCityExcludedFromWmo(paris, alwaysEnabled = false))
        assertTrue(WmoAlertRepository.isCityExcludedFromWmo(berlin, alwaysEnabled = false))
        assertTrue(WmoAlertRepository.isCityExcludedFromWmo(kyiv, alwaysEnabled = false))
        assertTrue(WmoAlertRepository.isCityExcludedFromWmo(telAviv, alwaysEnabled = false))

        assertTrue(WmoAlertRepository.fetchAlertsForCity(tokyo, alwaysEnabled = false).isEmpty())
        assertTrue(WmoAlertRepository.fetchAlertsForCity(newYork, alwaysEnabled = false).isEmpty())
        assertTrue(WmoAlertRepository.fetchAlertsForCity(paris, alwaysEnabled = false).isEmpty())
        assertTrue(WmoAlertRepository.fetchAlertsForCity(kyiv, alwaysEnabled = false).isEmpty())
        assertTrue(WmoAlertRepository.fetchAlertsForCity(telAviv, alwaysEnabled = false).isEmpty())

        // 2. 日本・米国・MeteoAlarmサポート範囲以外の世界各国:
        // WMOガードにブロックされず、WMOから公式警報を取得する
        val sydney = City(id = "sydney", name = "シドニー", nameEn = "Sydney", country = "Australia", latitude = -33.8688, longitude = 151.2093)
        val toronto = City(id = "toronto", name = "トロント", nameEn = "Toronto", country = "Canada", latitude = 43.6532, longitude = -79.3832)
        val seoul = City(id = "seoul", name = "ソウル", nameEn = "Seoul", country = "South Korea", latitude = 37.5665, longitude = 126.9780)
        val beijing = City(id = "beijing", name = "北京", nameEn = "Beijing", country = "China", latitude = 39.9042, longitude = 116.4074)
        val saoPaulo = City(id = "saopaulo", name = "サンパウロ", nameEn = "São Paulo", country = "Brazil", latitude = -23.5505, longitude = -46.6333)

        org.junit.Assert.assertFalse(WmoAlertRepository.isCityExcludedFromWmo(sydney, alwaysEnabled = false))
        org.junit.Assert.assertFalse(WmoAlertRepository.isCityExcludedFromWmo(toronto, alwaysEnabled = false))
        org.junit.Assert.assertFalse(WmoAlertRepository.isCityExcludedFromWmo(seoul, alwaysEnabled = false))
        org.junit.Assert.assertFalse(WmoAlertRepository.isCityExcludedFromWmo(beijing, alwaysEnabled = false))
        org.junit.Assert.assertFalse(WmoAlertRepository.isCityExcludedFromWmo(saoPaulo, alwaysEnabled = false))

        // 3. alwaysEnabled = true: 日本・米国・MeteoAlarm対象国でもWMO取得が許可される
        org.junit.Assert.assertFalse(WmoAlertRepository.isCityExcludedFromWmo(tokyo, alwaysEnabled = true))
        org.junit.Assert.assertFalse(WmoAlertRepository.isCityExcludedFromWmo(newYork, alwaysEnabled = true))
        org.junit.Assert.assertFalse(WmoAlertRepository.isCityExcludedFromWmo(paris, alwaysEnabled = true))
        org.junit.Assert.assertFalse(WmoAlertRepository.isCityExcludedFromWmo(kyiv, alwaysEnabled = true))
    }

    @Test
    fun sampleAlerts_containThreeDistinctSeverities() {
        val samples = WmoAlertRepository.createSampleAlerts()
        assertEquals(3, samples.size)

        // 1. 赤色: 特別警戒 (SPECIAL_WARNING)
        val specialWarning = samples.find { it.type == WeatherWarningType.SPECIAL_WARNING }
        assertNotNull(specialWarning)
        assertEquals(AlertSeverity.EXTREME, specialWarning?.severity)

        // 2. 橙色: 警戒 (WARNING)
        val warning = samples.find { it.type == WeatherWarningType.WARNING }
        assertNotNull(warning)
        assertEquals(AlertSeverity.SEVERE, warning?.severity)

        // 3. 黄色: 注意 (ADVISORY)
        val advisory = samples.find { it.type == WeatherWarningType.ADVISORY }
        assertNotNull(advisory)
        assertEquals(AlertSeverity.MODERATE, advisory?.severity)
    }

    @Test
    fun wmoEventTranslation_supportsFourLanguages() {
        // Rainstorm
        assertEquals("豪雨警報", GlobalAlertItem.translateWmoEvent("Rainstorm", AppLanguage.JAPANESE))
        assertEquals("Rainstorm", GlobalAlertItem.translateWmoEvent("Rainstorm", AppLanguage.ENGLISH))
        assertEquals("特大暴雨预警", GlobalAlertItem.translateWmoEvent("Rainstorm", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("特大豪雨警報", GlobalAlertItem.translateWmoEvent("Rainstorm", AppLanguage.TRADITIONAL_CHINESE))

        // Gale
        assertEquals("暴風警報", GlobalAlertItem.translateWmoEvent("Gale", AppLanguage.JAPANESE))
        assertEquals("Gale", GlobalAlertItem.translateWmoEvent("Gale", AppLanguage.ENGLISH))
        assertEquals("大风烈风预警", GlobalAlertItem.translateWmoEvent("Gale", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("強烈風暴警報", GlobalAlertItem.translateWmoEvent("Gale", AppLanguage.TRADITIONAL_CHINESE))

        // Blizzard
        assertEquals("猛吹雪・暴風雪警報", GlobalAlertItem.translateWmoEvent("Blizzard", AppLanguage.JAPANESE))
        assertEquals("Blizzard", GlobalAlertItem.translateWmoEvent("Blizzard", AppLanguage.ENGLISH))
        assertEquals("特大暴风雪预警", GlobalAlertItem.translateWmoEvent("Blizzard", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("特大暴風雪警報", GlobalAlertItem.translateWmoEvent("Blizzard", AppLanguage.TRADITIONAL_CHINESE))
    }
}
