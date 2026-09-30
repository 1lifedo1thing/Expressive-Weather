package com.example.expressiveweather.data

import com.example.expressiveweather.ui.components.WeatherWarningType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WmoAlertRepositoryTest {

    @Test
    fun wmoGuard_blocksJapanAndUs_unlessAlwaysEnabled() = runBlocking {
        val tokyo = City(id = "tokyo", name = "東京", nameEn = "Tokyo", country = "日本", latitude = 35.6895, longitude = 139.6917)
        val newYork = City(id = "ny", name = "ニューヨーク", nameEn = "New York", country = "United States", latitude = 40.7128, longitude = -74.0060)

        // alwaysEnabled = false (デフォルト) -> 空リスト返却 (直接取得を完全保護)
        val alertsTokyo = WmoAlertRepository.fetchAlertsForCity(tokyo, alwaysEnabled = false)
        assertTrue(alertsTokyo.isEmpty())

        val alertsNy = WmoAlertRepository.fetchAlertsForCity(newYork, alwaysEnabled = false)
        assertTrue(alertsNy.isEmpty())
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
