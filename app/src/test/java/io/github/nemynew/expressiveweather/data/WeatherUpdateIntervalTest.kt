package io.github.nemynew.expressiveweather.data

import io.github.nemynew.expressiveweather.ui.screens.SettingsDataHolder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

        // 未知の値はデフォルトの15分（Play安全最短周期）にフォールバック
        assertEquals(WeatherUpdateInterval.MINUTES_15, WeatherUpdateInterval.fromMinutes(45))
        assertEquals(WeatherUpdateInterval.MINUTES_15, WeatherUpdateInterval.fromMinutes(-1))
    }

    @Test
    fun testWeatherUpdateIntervalLabels() {
        val interval15 = WeatherUpdateInterval.MINUTES_15
        assertEquals("15分ごと (おすすめ)", interval15.getLabel(AppLanguage.JAPANESE))
        assertEquals("Every 15 minutes (Recommended)", interval15.getLabel(AppLanguage.ENGLISH))
        assertEquals("每15分钟 (推荐)", interval15.getLabel(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("每15分鐘 (推薦)", interval15.getLabel(AppLanguage.TRADITIONAL_CHINESE))
        assertEquals("15분마다 (권장)", interval15.getLabel(AppLanguage.KOREAN))

        val interval30 = WeatherUpdateInterval.MINUTES_30
        assertEquals("30分ごと", interval30.getLabel(AppLanguage.JAPANESE))
        assertEquals("Every 30 minutes", interval30.getLabel(AppLanguage.ENGLISH))

        val intervalOff = WeatherUpdateInterval.OFF
        assertEquals("オフ (自動更新なし)", intervalOff.getLabel(AppLanguage.JAPANESE))
        assertEquals("Off (Manual only)", intervalOff.getLabel(AppLanguage.ENGLISH))
    }

    @Test
    fun testSettingsDataHolderWeatherUpdateInterval() {
        SettingsDataHolder.weatherUpdateInterval = WeatherUpdateInterval.HOUR_1
        assertEquals(WeatherUpdateInterval.HOUR_1, SettingsDataHolder.weatherUpdateInterval)

        SettingsDataHolder.clear()
        assertEquals(WeatherUpdateInterval.MINUTES_15, SettingsDataHolder.weatherUpdateInterval)
    }

    @Test
    fun testPlaySafeAlarmDelayFloorAndWakeupBudget() {
        // Play 版では最短 15 分 (= 最大 4 回/時 <= Android Vitals 10 回/時基準) を強制
        assertEquals(
            15 * 60_000L,
            BackgroundSchedulePolicy.computeAlarmDelayMillis(5 * 60_000L, isPlayDistribution = true)
        )
        assertEquals(
            30 * 60_000L,
            BackgroundSchedulePolicy.computeAlarmDelayMillis(30 * 60_000L, isPlayDistribution = true)
        )
        // GitHub 版では 10 分間隔を許容
        assertEquals(
            10 * 60_000L,
            BackgroundSchedulePolicy.computeAlarmDelayMillis(10 * 60_000L, isPlayDistribution = false)
        )
    }

    @Test
    fun testPlayWidgetAlarmCoalescingPreventsExcessiveWakeups() {
        // Play 版: 天気が 15 分更新のときはウィジェット専用アラームを張らず統合 (合計 4 回/時)
        assertFalse(
            BackgroundSchedulePolicy.shouldScheduleDedicatedWidgetAlarm(
                isPlayDistribution = true,
                weatherInterval = WeatherUpdateInterval.MINUTES_15,
                hasActiveWidgets = true
            )
        )
        // Play 版: 天気が 30 分以上または OFF の場合はウィジェット専用 15 分アラームを有効化
        assertTrue(
            BackgroundSchedulePolicy.shouldScheduleDedicatedWidgetAlarm(
                isPlayDistribution = true,
                weatherInterval = WeatherUpdateInterval.MINUTES_30,
                hasActiveWidgets = true
            )
        )
        assertTrue(
            BackgroundSchedulePolicy.shouldScheduleDedicatedWidgetAlarm(
                isPlayDistribution = true,
                weatherInterval = WeatherUpdateInterval.OFF,
                hasActiveWidgets = true
            )
        )
        assertEquals(15L, BackgroundSchedulePolicy.resolveWidgetAlarmIntervalMinutes(isPlayDistribution = true))
        assertEquals(10L, BackgroundSchedulePolicy.resolveWidgetAlarmIntervalMinutes(isPlayDistribution = false))

        // ウィジェット未配置ならどちらの版でも無効
        assertFalse(
            BackgroundSchedulePolicy.shouldScheduleDedicatedWidgetAlarm(
                isPlayDistribution = true,
                weatherInterval = WeatherUpdateInterval.OFF,
                hasActiveWidgets = false
            )
        )
    }

    @Test
    fun testWorkManagerFlexThrottlingAndExpeditedPolicy() {
        assertEquals(15L, BackgroundSchedulePolicy.computePeriodicIntervalMinutes(WeatherUpdateInterval.MINUTES_15))
        assertEquals(5L, BackgroundSchedulePolicy.computePeriodicFlexMinutes(15L))
        assertEquals(10L, BackgroundSchedulePolicy.computePeriodicFlexMinutes(30L))
        assertEquals(15L, BackgroundSchedulePolicy.computePeriodicFlexMinutes(60L))

        // 15 分周期のスロットリング間隔は 6 分 45 秒 (重複抑止しつつ早めの OS 配信を許容)
        assertEquals(405_000L, BackgroundSchedulePolicy.computeMinThrottlingGapMillis(WeatherUpdateInterval.MINUTES_15))
        assertEquals(Long.MAX_VALUE, BackgroundSchedulePolicy.computeMinThrottlingGapMillis(WeatherUpdateInterval.OFF))

        // API 31 (Android 12) 以上のみ Expedited Work を使用 (API 30 以下の FGS 強制を回避)
        assertFalse(BackgroundSchedulePolicy.shouldUseExpeditedWork(30))
        assertTrue(BackgroundSchedulePolicy.shouldUseExpeditedWork(31))
        assertTrue(BackgroundSchedulePolicy.shouldUseExpeditedWork(36))
    }
}
