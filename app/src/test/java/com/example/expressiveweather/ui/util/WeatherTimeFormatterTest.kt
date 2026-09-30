package com.example.expressiveweather.ui.util

import com.example.expressiveweather.data.EnglishStrings
import com.example.expressiveweather.data.JapaneseStrings
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherTimeFormatterTest {

    @Test
    fun formatRelativeTime_japanese_returnsExpectedValues() {
        val now = 1_000_000_000_000L

        // 30秒前 (< 1分)
        val justNow = WeatherTimeFormatter.formatRelativeTime(now - 30_000L, now, JapaneseStrings)
        assertEquals("たった今", justNow)

        // 5分前
        val fiveMinsAgo = WeatherTimeFormatter.formatRelativeTime(now - 5 * 60_000L, now, JapaneseStrings)
        assertEquals("5分前", fiveMinsAgo)

        // 2時間前
        val twoHoursAgo = WeatherTimeFormatter.formatRelativeTime(now - 2 * 3600_000L, now, JapaneseStrings)
        assertEquals("2時間前", twoHoursAgo)

        // 2日前
        val twoDaysAgo = WeatherTimeFormatter.formatRelativeTime(now - 2 * 86400_000L, now, JapaneseStrings)
        assertEquals("2日前", twoDaysAgo)
    }

    @Test
    fun formatRelativeTime_english_returnsExpectedValues() {
        val now = 1_000_000_000_000L

        // 30 seconds ago (< 1m)
        val justNow = WeatherTimeFormatter.formatRelativeTime(now - 30_000L, now, EnglishStrings)
        assertEquals("Just now", justNow)

        // 15m ago
        val fifteenMinsAgo = WeatherTimeFormatter.formatRelativeTime(now - 15 * 60_000L, now, EnglishStrings)
        assertEquals("15m ago", fifteenMinsAgo)

        // 3h ago
        val threeHoursAgo = WeatherTimeFormatter.formatRelativeTime(now - 3 * 3600_000L, now, EnglishStrings)
        assertEquals("3h ago", threeHoursAgo)

        // 1d ago
        val oneDayAgo = WeatherTimeFormatter.formatRelativeTime(now - 86400_000L, now, EnglishStrings)
        assertEquals("1d ago", oneDayAgo)
    }

    @Test
    fun formatLastUpdated_freshAndCached_returnsExpectedStrings() {
        val now = 1_000_000_000_000L
        val fiveMinsAgo = now - 5 * 60_000L

        // Japanese fresh
        val jaFresh = WeatherTimeFormatter.formatLastUpdated("14:30", fiveMinsAgo, isFromCache = false, nowMillis = now, strings = JapaneseStrings)
        assertEquals("更新: 14:30（5分前）", jaFresh)

        // Japanese cached
        val jaCached = WeatherTimeFormatter.formatLastUpdated("12:00", fiveMinsAgo, isFromCache = true, nowMillis = now, strings = JapaneseStrings)
        assertEquals("キャッシュ: 12:00（5分前）", jaCached)

        // English fresh
        val enFresh = WeatherTimeFormatter.formatLastUpdated("14:30", fiveMinsAgo, isFromCache = false, nowMillis = now, strings = EnglishStrings)
        assertEquals("Updated: 14:30 (5m ago)", enFresh)

        // English cached
        val enCached = WeatherTimeFormatter.formatLastUpdated("12:00", fiveMinsAgo, isFromCache = true, nowMillis = now, strings = EnglishStrings)
        assertEquals("Cached: 12:00 (5m ago)", enCached)
    }
}
