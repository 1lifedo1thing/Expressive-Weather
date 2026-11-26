package io.github.nemynew.expressiveweather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WindAndUvAccuracyTest {

    @Test
    fun testWindFlowRotationDegreesPointsInBlowingDirection() {
        // 北風 (0°: 北から南へ吹く) -> 矢印は南向き (180°)
        assertEquals(180f, windFlowRotationDegrees(0f), 0.01f)
        // 南風 (180°: 南から北へ吹く) -> 矢印は北向き (0°)
        assertEquals(0f, windFlowRotationDegrees(180f), 0.01f)
        // 東風 (90°: 東から西へ吹く) -> 矢印は西向き (270°)
        assertEquals(270f, windFlowRotationDegrees(90f), 0.01f)
        // 西風 (270°: 西から東へ吹く) -> 矢印は東向き (90°)
        assertEquals(90f, windFlowRotationDegrees(270f), 0.01f)
        // 南南東 (157.5°) -> 北北西向き (337.5°)
        assertEquals(337.5f, windFlowRotationDegrees(157.5f), 0.01f)
        // 360° / 負角 / NaN 正規化
        assertEquals(180f, windFlowRotationDegrees(360f), 0.01f)
        assertEquals(90f, windFlowRotationDegrees(-90f), 0.01f)
        assertEquals(180f, windFlowRotationDegrees(Float.NaN), 0.01f)
    }

    @Test
    fun testAmedasWindDegreeNormalization() {
        assertEquals(0f, JmaWeatherParser.normalizeAmedasWindDegree(16), 0.01f) // 北
        assertEquals(90f, JmaWeatherParser.normalizeAmedasWindDegree(4), 0.01f) // 東
        assertEquals(180f, JmaWeatherParser.normalizeAmedasWindDegree(8), 0.01f) // 南
        assertEquals(270f, JmaWeatherParser.normalizeAmedasWindDegree(12), 0.01f) // 西
        assertEquals(0f, JmaWeatherParser.normalizeAmedasWindDegree(0), 0.01f) // 静穏
    }

    @Test
    fun testJapanHourlyAndDailyWindDirectionUsesOpenMeteoSupplement() {
        val hourlyDirMap = mapOf(
            "2026-11-26T15" to 180f,
            "2026-11-26T16" to 225f,
            "2026-11-26T17" to 315f
        )
        val dailyDirMap = mapOf(
            "2026-11-26" to 160f,
            "2026-11-27" to 350f,
            "2026-11-28" to 45f
        )

        // step=0 (現在) はアメダス実測風向を優先し、step>=1 は Open-Meteo 時間別風向を採用
        assertEquals(
            157.5f,
            JmaWeatherParser.resolveHourlyWindDegree(0, "2026-11-26T15", hourlyDirMap, 157.5f, hasAmedasWindDirection = true),
            0.01f
        )
        assertEquals(
            180f,
            JmaWeatherParser.resolveHourlyWindDegree(0, "2026-11-26T15", hourlyDirMap, 157.5f, hasAmedasWindDirection = false),
            0.01f
        )
        assertEquals(
            225f,
            JmaWeatherParser.resolveHourlyWindDegree(1, "2026-11-26T16", hourlyDirMap, 157.5f, hasAmedasWindDirection = true),
            0.01f
        )
        assertEquals(
            315f,
            JmaWeatherParser.resolveHourlyWindDegree(2, "2026-11-26T17", hourlyDirMap, 157.5f, hasAmedasWindDirection = true),
            0.01f
        )
        // Open-Meteo 欠損時は現在風向へフォールバック
        assertEquals(
            157.5f,
            JmaWeatherParser.resolveHourlyWindDegree(3, "2026-11-26T18", hourlyDirMap, 157.5f, hasAmedasWindDirection = true),
            0.01f
        )

        // 週間風予報は日別の卓越風向を採用
        assertEquals(160f, JmaWeatherParser.resolveDailyWindDegree("2026-11-26", dailyDirMap, 157.5f), 0.01f)
        assertEquals(350f, JmaWeatherParser.resolveDailyWindDegree("2026-11-27", dailyDirMap, 157.5f), 0.01f)
        assertEquals(45f, JmaWeatherParser.resolveDailyWindDegree("2026-11-28", dailyDirMap, 157.5f), 0.01f)
        assertEquals(157.5f, JmaWeatherParser.resolveDailyWindDegree("2026-11-29", dailyDirMap, 157.5f), 0.01f)

        // 北跨ぎ (350° と 10°) の円周平均が 0° になること
        val northMean = JmaWeatherParser.circularMeanWindDegree(listOf(350f, 10f))!!
        assertTrue("Expected ~0° across north wrap, got $northMean", northMean < 1f || northMean > 359f)
    }

    @Test
    fun testCurrentUvResolutionVariesByApiTimeAndLatitude() {
        val jst = TimeZone.getTimeZone("Asia/Tokyo")
        val middaySummer = Calendar.getInstance(jst).apply {
            set(2026, Calendar.JULY, 15, 12, 0, 0)
        }.time
        val midnight = Calendar.getInstance(jst).apply {
            set(2026, Calendar.JULY, 15, 2, 0, 0)
        }.time
        val middayWinter = Calendar.getInstance(jst).apply {
            set(2026, Calendar.DECEMBER, 22, 12, 0, 0)
        }.time

        // 1. Open-Meteo 時間別 UV マップがある場合は固定値 4 ではなく実値を返す
        val apiMap = mapOf(
            "2026-07-15T12" to 10.4,
            "2026-07-15T02" to 0.0,
            "2026-12-22T12" to 2.2
        )
        assertEquals(
            10,
            JmaWeatherParser.resolveCurrentUvIndex(apiMap, "2026-07-15T12", true, 35.68, middaySummer, jst, WeatherCondition.CLEAR)
        )
        assertEquals(
            0,
            JmaWeatherParser.resolveCurrentUvIndex(apiMap, "2026-07-15T02", false, 35.68, midnight, jst, WeatherCondition.CLEAR)
        )
        assertEquals(
            2,
            JmaWeatherParser.resolveCurrentUvIndex(apiMap, "2026-12-22T12", true, 35.68, middayWinter, jst, WeatherCondition.CLEAR)
        )

        // 2. API 未取得時のフォールバックも夜間=0・夏昼>冬昼・低緯度>高緯度・晴>雨で変化すること
        val nightFallback = JmaWeatherParser.resolveCurrentUvIndex(emptyMap(), "2026-07-15T02", false, 35.68, midnight, jst, WeatherCondition.CLEAR)
        assertEquals(0, nightFallback)

        val summerTokyo = JmaWeatherParser.estimateSolarUv(35.68, middaySummer, jst, WeatherCondition.CLEAR)
        val winterTokyo = JmaWeatherParser.estimateSolarUv(35.68, middayWinter, jst, WeatherCondition.CLEAR)
        val winterNaha = JmaWeatherParser.estimateSolarUv(26.21, middayWinter, jst, WeatherCondition.CLEAR)
        val winterSapporo = JmaWeatherParser.estimateSolarUv(43.06, middayWinter, jst, WeatherCondition.CLEAR)
        val summerRain = JmaWeatherParser.estimateSolarUv(35.68, middaySummer, jst, WeatherCondition.RAIN)

        assertTrue("Summer midday UV ($summerTokyo) should exceed winter midday UV ($winterTokyo)", summerTokyo > winterTokyo + 3.0)
        assertTrue("Naha winter UV ($winterNaha) should exceed Sapporo winter UV ($winterSapporo)", winterNaha > winterSapporo + 1.0)
        assertTrue("Clear sky UV ($summerTokyo) should exceed rain UV ($summerRain)", summerTokyo > summerRain * 2.0)
    }
}
