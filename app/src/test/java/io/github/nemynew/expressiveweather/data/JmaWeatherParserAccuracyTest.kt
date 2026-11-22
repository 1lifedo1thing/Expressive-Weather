package io.github.nemynew.expressiveweather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class JmaWeatherParserAccuracyTest {

    private val jst = TimeZone.getTimeZone("Asia/Tokyo")
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
        timeZone = jst
    }

    @Test
    fun testDiurnalCurvePeaksAt14AndBottomsAt05() {
        val f05 = JmaWeatherParser.computeDiurnalFraction(5)
        val f14 = JmaWeatherParser.computeDiurnalFraction(14)
        val f16 = JmaWeatherParser.computeDiurnalFraction(16)
        val f20 = JmaWeatherParser.computeDiurnalFraction(20)
        val f23 = JmaWeatherParser.computeDiurnalFraction(23)

        assertEquals(0.0, f05, 0.001)
        assertEquals(1.0, f14, 0.001)
        assertTrue("16時は20時より高い気温係数であるべき", f16 > f20)
        assertTrue("20時は23時より高い気温係数であるべき", f20 > f23)
    }

    @Test
    fun testJapanNowcastUsesJmaMinutelySeriesWhileBlockingOpenMeteo() {
        val nowMillis = isoFormat.parse("2026-11-19T16:00")!!.time

        // 1. 日本設定で Open-Meteo 側だけ雨を渡しても完全に遮断されること
        val blockedOpenMeteo = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = listOf("2026-11-19T16:00", "2026-11-19T16:15", "2026-11-19T16:30"),
            minutelyPrecips = listOf(0.0, 3.0, 5.0),
            minutelyCodes = listOf(3, 61, 63),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis,
            country = "日本",
            latitude = 35.90,
            longitude = 139.59,
            jmaMinutelySeries = null
        )
        assertEquals(RainStatus.DRY, blockedOpenMeteo.status)

        // 2. JMA 高解像度降水ナウキャスト (hrpns) の5分刻み時系列を渡せば RAIN_APPROACHING を検出すること
        val jmaSeries = listOf(
            nowMillis to 0.0,
            (nowMillis + 5 * 60_000L) to 0.0,
            (nowMillis + 10 * 60_000L) to 2.5,
            (nowMillis + 15 * 60_000L) to 7.5,
            (nowMillis + 20 * 60_000L) to 7.5
        )
        val jmaDetected = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = null,
            minutelyPrecips = null,
            minutelyCodes = null,
            hourlyForecast = emptyList(),
            nowMillis = nowMillis,
            country = "日本",
            latitude = 35.90,
            longitude = 139.59,
            jmaMinutelySeries = jmaSeries
        )
        assertEquals(RainStatus.RAIN_APPROACHING, jmaDetected.status)
        assertTrue(jmaDetected.isLiveActive)
        assertEquals(10, jmaDetected.minutesRemaining)
    }

    @Test
    fun testPop50RainForecastTriggersApproaching() {
        val nowMillis = isoFormat.parse("2026-11-19T16:00")!!.time
        val hourly = listOf(
            HourlyForecast("現在", 16, 15, WeatherCondition.CLOUDY, 20, 60, 5.0, 7, false, 0.0),
            HourlyForecast("17:00", 17, 15, WeatherCondition.CLOUDY, 30, 65, 5.0, 8, false, 0.0),
            HourlyForecast("18:00", 18, 14, WeatherCondition.RAIN, 55, 75, 6.0, 9, false, 0.0),
            HourlyForecast("19:00", 19, 13, WeatherCondition.RAIN, 60, 80, 6.0, 9, false, 0.0)
        )

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = null,
            minutelyPrecips = null,
            minutelyCodes = null,
            hourlyForecast = hourly,
            nowMillis = nowMillis,
            country = "日本"
        )

        assertEquals(RainStatus.RAIN_APPROACHING, nowcast.status)
        assertTrue(nowcast.minutelyPoints.any { it.precipMm >= PrecipitationAnalyzer.THRESHOLD_RAIN_START })
    }

    @Test
    fun testMergeWithKikikuruTileRiskUpgradesLevelAndFlags() {
        val base = MunicipalityAlertInfo(
            municipalityCode = "1110000",
            municipalityName = "さいたま市",
            prefectureCode = "110000",
            prefectureName = "埼玉県",
            alertLevel = AlertLevel.LEVEL_0
        )

        val risk = JmaPointKikikuruRisk(
            landslideLevel = 3,
            inundationLevel = 2,
            floodLevel = 0,
            sampledAtMillis = 123456789L
        )

        val merged = JmaAlertRepository.mergeWithKikikuruTileRisk(base, risk)
        assertEquals(AlertLevel.LEVEL_3, merged.alertLevel)
        assertTrue(merged.disasterFlags.landslide)
        assertTrue(merged.disasterFlags.inundation)
        assertTrue(merged.disasterFlags.heavyRain)
        assertEquals(DisasterType.LANDSLIDE, merged.targetDisasterPrimary)
        assertEquals(
            AlertLevel.LEVEL_3,
            merged.disasterDetails.first { it.disasterType == DisasterType.LANDSLIDE }.alertLevel
        )
        assertEquals(
            AlertLevel.LEVEL_2,
            merged.disasterDetails.first { it.disasterType == DisasterType.INUNDATION }.alertLevel
        )
    }

    @Test
    fun testRasrfShortRangeForecastPopulatesTier2WithoutHourlyRain() {
        val nowMillis = isoFormat.parse("2026-11-19T16:00")!!.time
        // 0..60分 (hrpns) は 0.0mm、2〜4時間先 (rasrf) で 2.5mm〜7.5mm の雨域を検出
        val combinedJmaSeries = listOf(
            nowMillis to 0.0,
            (nowMillis + 30 * 60_000L) to 0.0,
            (nowMillis + 60 * 60_000L) to 0.0,
            (nowMillis + 120 * 60_000L) to 2.5,
            (nowMillis + 180 * 60_000L) to 7.5,
            (nowMillis + 240 * 60_000L) to 2.5
        )
        // 粗い時間予報は曇り・PoP 20% のまま
        val cloudyHourly = listOf(
            HourlyForecast("現在", 16, 15, WeatherCondition.CLOUDY, 20, 60, 4.0, 7, true, 0.0),
            HourlyForecast("17:00", 17, 15, WeatherCondition.CLOUDY, 20, 62, 4.0, 7, false, 0.0),
            HourlyForecast("18:00", 18, 14, WeatherCondition.CLOUDY, 20, 65, 4.0, 7, false, 0.0)
        )

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = null,
            minutelyPrecips = null,
            minutelyCodes = null,
            hourlyForecast = cloudyHourly,
            nowMillis = nowMillis,
            country = "日本",
            latitude = 35.90,
            longitude = 139.59,
            jmaMinutelySeries = combinedJmaSeries
        )

        assertTrue("rasrf の 1〜6時間先雨域により DRY 以外になるべき", nowcast.status != RainStatus.DRY)
        assertTrue(
            "Tier2 (2〜4時間後) に 2.5mm/h 以上の点が存在するべき",
            nowcast.minutelyPoints.any { it.precipMm >= 2.5 }
        )
    }

    @Test
    fun testHrpnsTailBridgesSmoothlyPast60Minutes() {
        val nowMillis = isoFormat.parse("2026-11-19T16:00")!!.time
        // rasrf が欠損し hrpns (+60m = 3.0mm/h) までしか取れなかったケース
        val hrpnsOnly = listOf(
            nowMillis to 0.0,
            (nowMillis + 50 * 60_000L) to 1.5,
            (nowMillis + 60 * 60_000L) to 3.0
        )
        val hourly = listOf(
            HourlyForecast("現在", 16, 15, WeatherCondition.CLOUDY, 20, 60, 4.0, 7, true, 0.0),
            HourlyForecast("17:00", 17, 15, WeatherCondition.CLOUDY, 30, 65, 4.0, 8, false, 0.0),
            HourlyForecast("18:00", 18, 14, WeatherCondition.CLOUDY, 20, 65, 4.0, 7, false, 0.0)
        )

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = null,
            minutelyPrecips = null,
            minutelyCodes = null,
            hourlyForecast = hourly,
            nowMillis = nowMillis,
            country = "日本",
            jmaMinutelySeries = hrpnsOnly
        )

        val frame70m = nowcast.minutelyPoints.firstOrNull {
            kotlin.math.abs(it.timestampMillis - (nowMillis + 70 * 60_000L)) <= 2 * 60_000L
        }
        assertTrue(
            "+70分フレームが+60分の 3.0mm/h から滑らかに補間接続されるべき (actual=${frame70m?.precipMm})",
            (frame70m?.precipMm ?: 0.0) >= 2.0
        )
    }
}
