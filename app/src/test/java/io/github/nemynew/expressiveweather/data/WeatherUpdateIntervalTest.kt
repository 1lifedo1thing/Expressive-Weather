package io.github.nemynew.expressiveweather.data

import io.github.nemynew.expressiveweather.ui.screens.SettingsDataHolder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WeatherUpdateIntervalTest {

    @After
    fun tearDown() {
        SettingsDataHolder.clear()
    }

    @Test
    fun testWeatherUpdateIntervalFromMinutes() {
        assertEquals(WeatherUpdateInterval.MINUTES_15, WeatherUpdateInterval.fromMinutes(15))
        assertEquals(WeatherUpdateInterval.MINUTES_30, WeatherUpdateInterval.fromMinutes(30))
        assertEquals(WeatherUpdateInterval.HOUR_1, WeatherUpdateInterval.fromMinutes(60))
        assertEquals(WeatherUpdateInterval.HOURS_2, WeatherUpdateInterval.fromMinutes(120))
        assertEquals(WeatherUpdateInterval.HOURS_3, WeatherUpdateInterval.fromMinutes(180))
        assertEquals(WeatherUpdateInterval.HOURS_6, WeatherUpdateInterval.fromMinutes(360))
        assertEquals(WeatherUpdateInterval.OFF, WeatherUpdateInterval.fromMinutes(0))

        // 未知の値は推奨の30分（デフォルト）に安全にフォールバック
        assertEquals(WeatherUpdateInterval.MINUTES_30, WeatherUpdateInterval.fromMinutes(45))
        assertEquals(WeatherUpdateInterval.MINUTES_30, WeatherUpdateInterval.fromMinutes(-1))
    }

    @Test
    fun testWeatherUpdateIntervalLabels() {
        val interval30 = WeatherUpdateInterval.MINUTES_30
        assertEquals("30分ごと (おすすめ)", interval30.getLabel(AppLanguage.JAPANESE))
        assertEquals("Every 30 minutes (Recommended)", interval30.getLabel(AppLanguage.ENGLISH))
        assertEquals("每30分钟 (推荐)", interval30.getLabel(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("每30分鐘 (推薦)", interval30.getLabel(AppLanguage.TRADITIONAL_CHINESE))

        val intervalOff = WeatherUpdateInterval.OFF
        assertEquals("オフ (自動更新なし)", intervalOff.getLabel(AppLanguage.JAPANESE))
        assertEquals("Off (Manual only)", intervalOff.getLabel(AppLanguage.ENGLISH))
    }

    @Test
    fun testSettingsDataHolderWeatherUpdateInterval() {
        SettingsDataHolder.weatherUpdateInterval = WeatherUpdateInterval.HOUR_1
        assertEquals(WeatherUpdateInterval.HOUR_1, SettingsDataHolder.weatherUpdateInterval)

        SettingsDataHolder.clear()
        assertEquals(WeatherUpdateInterval.MINUTES_30, SettingsDataHolder.weatherUpdateInterval)
    }
}
