package io.github.nemynew.expressiveweather.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PrecipitationAnalyzerTest {

    private var savedDefaultTimeZone: TimeZone? = null

    private val jstTimeZone = TimeZone.getTimeZone("Asia/Tokyo")

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
        timeZone = jstTimeZone
    }

    @Before
    fun setUp() {
        savedDefaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(jstTimeZone)
    }

    @After
    fun tearDown() {
        savedDefaultTimeZone?.let { TimeZone.setDefault(it) }
    }

    // =========================================================================
    // 1. 状態 DRY: 直近60分以内の降り始め判定
    // =========================================================================

    @Test
    fun testDryToRainWithin60MinUnder15Min() {
        val nowMillis = isoFormat.parse("2026-09-24T14:00")!!.time

        // 14:00〜14:05は0mm、14:10から雨 (1.5mm, 1.8mm)
        val times = listOf(
            "2026-09-24T14:00",
            "2026-09-24T14:10",
            "2026-09-24T14:15",
            "2026-09-24T14:30"
        )
        val precips = listOf(0.0, 1.5, 1.8, 1.2)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(3, 61, 61, 61),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAIN_APPROACHING, nowcast.status)
        assertTrue(nowcast.isLiveActive)
        assertEquals("まもなく雨が降り始めます", nowcast.headline)
        assertTrue(nowcast.minutesRemaining!! < 15)
    }

    @Test
    fun testDryToHeavyRainWithin60MinUnder15Min() {
        val nowMillis = isoFormat.parse("2026-09-24T14:00")!!.time

        // 14:10から激しい雨 (12.0mm, 15.0mm >= 10.0mm/h)
        val times = listOf(
            "2026-09-24T14:00",
            "2026-09-24T14:10",
            "2026-09-24T14:15"
        )
        val precips = listOf(0.0, 12.0, 15.0)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(3, 65, 65),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAIN_APPROACHING, nowcast.status)
        assertTrue(nowcast.isLiveActive)
        assertEquals("まもなく激しい雨が降ります", nowcast.headline)
    }

    @Test
    fun testDryToRainWithin60MinOver15Min() {
        val nowMillis = isoFormat.parse("2026-09-24T14:00")!!.time

        // 14:25から雨 (1.5mm, 1.8mm)
        val times = listOf(
            "2026-09-24T14:00",
            "2026-09-24T14:15",
            "2026-09-24T14:25",
            "2026-09-24T14:30"
        )
        val precips = listOf(0.0, 0.0, 1.5, 1.8)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(3, 3, 61, 61),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAIN_APPROACHING, nowcast.status)
        assertTrue(nowcast.isLiveActive)
        assertEquals(25, nowcast.minutesRemaining)
        assertTrue(nowcast.headline.contains("14時25分頃から雨が降り始めます"))
    }

    @Test
    fun testDryToHeavyRainWithin60MinOver15Min() {
        val nowMillis = isoFormat.parse("2026-09-24T14:00")!!.time

        // 14:25から強雨 (11.0mm, 12.0mm >= 10.0mm/h)
        val times = listOf(
            "2026-09-24T14:00",
            "2026-09-24T14:20",
            "2026-09-24T14:25",
            "2026-09-24T14:30"
        )
        val precips = listOf(0.0, 0.0, 11.0, 12.0)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(3, 3, 65, 65),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAIN_APPROACHING, nowcast.status)
        assertTrue(nowcast.isLiveActive)
        assertTrue(nowcast.headline.contains("14時25分頃から激しい雨が降ります"))
    }

    // =========================================================================
    // 2. ノイズ除外ロジック (1フレーム単独スパイク vs 2フレーム連続確定)
    // =========================================================================

    @Test
    fun testNoiseFilterRejectsSingleFrameSpike() {
        val nowMillis = isoFormat.parse("2026-09-24T14:00")!!.time

        // 14:20に単発で1.2mm/hだが、前後が0.0mm/hの孤立メッシュ
        val times = listOf(
            "2026-09-24T14:00",
            "2026-09-24T14:15",
            "2026-09-24T14:20",
            "2026-09-24T14:25",
            "2026-09-24T14:30"
        )
        val precips = listOf(0.0, 0.0, 1.2, 0.0, 0.0)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(3, 3, 61, 3, 3),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        // 2フレーム連続 0.8mm以上の確定条件を満たさないためノイズとして除外され DRY と判定される
        assertEquals(RainStatus.DRY, nowcast.status)
        assertEquals("今日いっぱいは雨の心配はありません", nowcast.headline)
    }

    @Test
    fun testNoiseFilterAcceptsTwoConsecutiveFrames() {
        val nowMillis = isoFormat.parse("2026-09-24T14:00")!!.time

        // 14:20に1.2mm/h、14:25に0.9mm/h（2フレーム連続で0.8mm/h以上）
        val times = listOf(
            "2026-09-24T14:00",
            "2026-09-24T14:15",
            "2026-09-24T14:20",
            "2026-09-24T14:25",
            "2026-09-24T14:30"
        )
        val precips = listOf(0.0, 0.0, 1.2, 0.9, 0.5)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(3, 3, 61, 61, 61),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAIN_APPROACHING, nowcast.status)
        assertEquals(20, nowcast.minutesRemaining)
    }

    // =========================================================================
    // 3. 状態 DRY: 1時間後〜6時間後 (10分単位の刻み表現)
    // =========================================================================

    @Test
    fun testDryToRainBetween1hAnd6h() {
        val nowMillis = isoFormat.parse("2026-09-24T12:00")!!.time

        // 14:20頃（2時間20分後 = 140分後）から雨
        val targetMillis = nowMillis + 140 * 60 * 1000L
        val targetIso = isoFormat.format(java.util.Date(targetMillis))
        val targetIsoBefore = isoFormat.format(java.util.Date(targetMillis - 10 * 60 * 1000L))
        val targetIsoNext = isoFormat.format(java.util.Date(targetMillis + 10 * 60 * 1000L))

        val times = listOf("2026-09-24T12:00", targetIsoBefore, targetIso, targetIsoNext)
        val precips = listOf(0.0, 0.0, 2.0, 2.5)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLEAR,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(0, 0, 61, 61),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAIN_APPROACHING, nowcast.status)
        assertTrue(nowcast.headline.contains("14時20分頃から雨が降り始める見込みです"))
    }

    // =========================================================================
    // 4. 状態 DRY: 6時間後〜15時間後 (1時間単位の丸め表現)
    // =========================================================================

    @Test
    fun testDryToRainBetween6hAnd15h() {
        val nowMillis = isoFormat.parse("2026-09-24T12:00")!!.time

        // 19:00（7時間後）から雨
        val hourly = listOf(
            HourlyForecast("12:00", 12, 24, WeatherCondition.CLEAR, 10, 50, 8.0),
            HourlyForecast("13:00", 13, 25, WeatherCondition.CLEAR, 10, 48, 8.0),
            HourlyForecast("14:00", 14, 26, WeatherCondition.CLEAR, 10, 45, 9.0),
            HourlyForecast("15:00", 15, 26, WeatherCondition.CLEAR, 10, 45, 9.0),
            HourlyForecast("16:00", 16, 25, WeatherCondition.CLEAR, 10, 48, 8.0),
            HourlyForecast("17:00", 17, 24, WeatherCondition.CLOUDY, 20, 55, 7.0),
            HourlyForecast("18:00", 18, 22, WeatherCondition.CLOUDY, 30, 60, 6.0),
            HourlyForecast("19:00", 19, 20, WeatherCondition.RAIN, 70, 75, 10.0, precipitationMm = 2.5),
            HourlyForecast("20:00", 20, 19, WeatherCondition.RAIN, 80, 80, 12.0, precipitationMm = 3.0)
        )

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLEAR,
            minutelyTimes = emptyList(),
            minutelyPrecips = emptyList(),
            minutelyCodes = emptyList(),
            hourlyForecast = hourly,
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAIN_EXPECTED_TONIGHT, nowcast.status)
        assertNull(nowcast.minutesRemaining)
        assertTrue(nowcast.headline.contains("19時頃から雨が降り出す見込みです"))
    }

    // =========================================================================
    // 5. 状態 DRY: 15時間以降〜翌日 (時間帯表現: 明日の朝/夕方以降)
    // =========================================================================

    @Test
    fun testDryToRainTier4Pop50() {
        val nowMillis = isoFormat.parse("2026-09-24T06:00")!!.time

        // 翌日夕方 (18:00〜) に降水確率70%
        val calTomorrowEvening = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 18)
            set(java.util.Calendar.MINUTE, 0)
        }

        val popSlots = listOf(
            PopSlot(nowMillis, 10),
            PopSlot(nowMillis + 6 * 3600 * 1000L, 10),
            PopSlot(calTomorrowEvening.timeInMillis, 70)
        )

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLEAR,
            minutelyTimes = emptyList(),
            minutelyPrecips = emptyList(),
            minutelyCodes = emptyList(),
            hourlyForecast = emptyList(),
            popSlots = popSlots,
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAIN_EXPECTED_LATER, nowcast.status)
        assertEquals("明日の夕方以降は傘が必要です", nowcast.headline)
        assertEquals(70, nowcast.maxProbability)
    }

    // =========================================================================
    // 6. 全期間を通して降水なし (DRY)
    // =========================================================================

    @Test
    fun testDryEntirePeriodNoRain() {
        val nowMillis = isoFormat.parse("2026-09-24T10:00")!!.time

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLEAR,
            minutelyTimes = listOf("2026-09-24T10:00", "2026-09-24T10:15"),
            minutelyPrecips = listOf(0.0, 0.0),
            minutelyCodes = listOf(0, 0),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.DRY, nowcast.status)
        assertEquals("今日いっぱいは雨の心配はありません", nowcast.headline)
        assertTrue(nowcast.subText.contains("当面雨は降らない見込みです"))
    }

    // =========================================================================
    // 7. 状態 RAINING: ピーク時刻の優先抽出 (雨脚の強まり)
    // =========================================================================

    @Test
    fun testRainingPeakTimePrioritized() {
        val nowMillis = isoFormat.parse("2026-09-24T15:00")!!.time

        // 現在雨量 1.5mm/h。15:30に14.0mm/hの激しい雨ピーク（現在の2倍以上かつ > 10.0mm/h）
        val times = listOf(
            "2026-09-24T15:00",
            "2026-09-24T15:15",
            "2026-09-24T15:30",
            "2026-09-24T15:45",
            "2026-09-24T16:00"
        )
        val precips = listOf(1.5, 2.0, 14.0, 1.0, 0.0)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 1.5,
            currentCondition = WeatherCondition.RAIN,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(61, 61, 65, 61, 0),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAINING, nowcast.status)
        assertTrue(nowcast.isPeak)
        assertTrue(nowcast.headline.contains("15時30分頃から雨がさらに強まる見込みです"))
    }

    // =========================================================================
    // 8. 状態 RAINING: 降り止み時刻 (20分以上継続して 0.1mm/h 以下)
    // =========================================================================

    @Test
    fun testRainingEndTimeSustained20Min() {
        val nowMillis = isoFormat.parse("2026-09-24T15:00")!!.time

        // 15:00〜15:30は雨。15:45に0.0mmになり、16:00, 16:15も0.0mm（20分以上継続）
        val times = listOf(
            "2026-09-24T15:00",
            "2026-09-24T15:15",
            "2026-09-24T15:30",
            "2026-09-24T15:45",
            "2026-09-24T16:00",
            "2026-09-24T16:15"
        )
        val precips = listOf(2.5, 1.8, 1.0, 0.0, 0.0, 0.0)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 2.5,
            currentCondition = WeatherCondition.RAIN,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(61, 61, 61, 0, 0, 0),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAINING, nowcast.status)
        assertFalse(nowcast.isPeak)
        assertEquals(45, nowcast.minutesRemaining)
        assertTrue(nowcast.headline.contains("15時45分頃に雨が止む見込みです"))
    }

    // =========================================================================
    // 9. 状態 RAINING: 先6時間ずっと降り続く場合
    // =========================================================================

    @Test
    fun testRainingContinuous6Hours() {
        val nowMillis = isoFormat.parse("2026-09-24T12:00")!!.time

        // 今後6時間以上ずっと 2.0mm/h の雨が続く
        val hourly = (0..8).map { step ->
            val h = (12 + step) % 24
            HourlyForecast(
                timeLabel = "$h:00",
                hour = h,
                tempC = 20,
                condition = WeatherCondition.RAIN,
                precipProb = 90,
                humidity = 90,
                windSpeedKmH = 15.0,
                precipitationMm = 2.0
            )
        }

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 2.0,
            currentCondition = WeatherCondition.RAIN,
            minutelyTimes = emptyList(),
            minutelyPrecips = emptyList(),
            minutelyCodes = emptyList(),
            hourlyForecast = hourly,
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAINING, nowcast.status)
        assertNull(nowcast.minutesRemaining)
        assertEquals("雨は当面の間（6時間以上）降り続く見込みです", nowcast.headline)
    }

    // =========================================================================
    // 10. 多言語対応 (日本語・英語・簡体中文・繁体中文)
    // =========================================================================

    @Test
    fun testMultilingualSupportTier4() {
        val nowMillis = isoFormat.parse("2026-09-24T06:00")!!.time

        // 翌日夕方 (18:00〜) に降水確率50%
        val calTomorrowEvening = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 18)
            set(java.util.Calendar.MINUTE, 0)
        }

        val popSlots = listOf(
            PopSlot(nowMillis, 10),
            PopSlot(calTomorrowEvening.timeInMillis, 50)
        )

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLEAR,
            minutelyTimes = emptyList(),
            minutelyPrecips = emptyList(),
            minutelyCodes = emptyList(),
            hourlyForecast = emptyList(),
            popSlots = popSlots,
            nowMillis = nowMillis,
            country = "JP"
        )

        // 日本語
        assertEquals("明日の夕方以降は傘が必要です", nowcast.localizedHeadline(AppLanguage.JAPANESE))
        assertTrue(nowcast.localizedSubText(AppLanguage.JAPANESE).contains("気象庁予報：降水確率50%の雨が予想されています。明日の外出時は雨具をご準備ください。"))

        // 英語
        assertEquals("Umbrella needed tomorrow evening", nowcast.localizedHeadline(AppLanguage.ENGLISH))
        assertTrue(nowcast.localizedSubText(AppLanguage.ENGLISH).contains("JMA Forecast: 50% chance of rain expected. Prepare rain gear for tomorrow."))

        // 簡体中文
        assertEquals("明天傍晚起需要带伞", nowcast.localizedHeadline(AppLanguage.SIMPLIFIED_CHINESE))
        assertTrue(nowcast.localizedSubText(AppLanguage.SIMPLIFIED_CHINESE).contains("气象厅预报：预计降水概率为 50%，明天出门请备好雨具。"))

        // 繁體中文
        assertEquals("明天傍晚起需要帶傘", nowcast.localizedHeadline(AppLanguage.TRADITIONAL_CHINESE))
        assertTrue(nowcast.localizedSubText(AppLanguage.TRADITIONAL_CHINESE).contains("氣象廳預報：預計降水機率為 50%，明天出門請備好雨具。"))

        // 米国の場合: NOAA/NWS予報 となることを検証
        val usNowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLEAR,
            minutelyTimes = emptyList(),
            minutelyPrecips = emptyList(),
            minutelyCodes = emptyList(),
            hourlyForecast = emptyList(),
            popSlots = popSlots,
            nowMillis = nowMillis,
            country = "United States"
        )
        assertTrue(usNowcast.localizedSubText(AppLanguage.JAPANESE).contains("NOAA/NWS予報：降水確率50%の雨が予想されています。明日の外出時は雨具をご準備ください。"))
        assertTrue(usNowcast.localizedSubText(AppLanguage.ENGLISH).contains("NOAA/NWS Forecast: 50% chance of rain expected. Prepare rain gear for tomorrow."))
    }

    @Test
    fun testMultilingualSupportApproaching() {
        val nowMillis = isoFormat.parse("2026-09-24T14:00")!!.time

        val times = listOf(
            "2026-09-24T14:00",
            "2026-09-24T14:15",
            "2026-09-24T14:25",
            "2026-09-24T14:30"
        )
        val precips = listOf(0.0, 0.0, 1.5, 1.8)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(3, 3, 61, 61),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals("14時25分頃から雨が降り始めます", nowcast.localizedHeadline(AppLanguage.JAPANESE))
        assertEquals("Rain starting around 14:25", nowcast.localizedHeadline(AppLanguage.ENGLISH))
        assertEquals("预计 14:25 左右开始下雨", nowcast.localizedHeadline(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("預計 14:25 左右開始下雨", nowcast.localizedHeadline(AppLanguage.TRADITIONAL_CHINESE))

        assertTrue(nowcast.localizedSubText(AppLanguage.ENGLISH).contains("Rain is expected in about 25 min."))
    }

    @Test
    fun testLegacyCacheFallbackMultilingual() {
        // レガシーキャッシュ（localizedHeadlines 等が空マップの古いオブジェクト）のテスト
        val legacyNowcast = PrecipitationNowcast(
            status = RainStatus.RAIN_EXPECTED_LATER,
            headline = "明日の夕方以降は傘が必要です",
            subText = "気象庁予報：降水確率50%の雨が予想されています。明日の外出時は雨具をご準備ください。",
            targetTimeLabel = "明日の夕方以降",
            targetTimestampMillis = 1790000000000L,
            maxProbability = 50,
            localizedHeadlines = emptyMap(),
            localizedSubTexts = emptyMap(),
            localizedTargetTimeLabels = emptyMap()
        )

        // 英語
        assertEquals("Umbrella needed tomorrow evening", legacyNowcast.localizedHeadline(AppLanguage.ENGLISH))
        assertEquals("JMA Forecast: 50% chance of rain expected. Prepare rain gear for tomorrow.", legacyNowcast.localizedSubText(AppLanguage.ENGLISH))
        assertEquals("Tomorrow evening", legacyNowcast.localizedTargetTimeLabel(AppLanguage.ENGLISH))

        // 簡体字中国語
        assertEquals("明天傍晚起需要带伞", legacyNowcast.localizedHeadline(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("气象厅预报：预计降水概率为 50%，明天出门请备好雨具。", legacyNowcast.localizedSubText(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("明天傍晚", legacyNowcast.localizedTargetTimeLabel(AppLanguage.SIMPLIFIED_CHINESE))

        // 繁体字中国語
        assertEquals("明天傍晚起需要帶傘", legacyNowcast.localizedHeadline(AppLanguage.TRADITIONAL_CHINESE))
        assertEquals("氣象廳預報：預計降水機率為 50%，明天出門請備好雨具。", legacyNowcast.localizedSubText(AppLanguage.TRADITIONAL_CHINESE))
        assertEquals("明天傍晚", legacyNowcast.localizedTargetTimeLabel(AppLanguage.TRADITIONAL_CHINESE))

        // 日本語
        assertEquals("明日の夕方以降は傘が必要です", legacyNowcast.localizedHeadline(AppLanguage.JAPANESE))
        assertEquals("気象庁予報：降水確率50%の雨が予想されています。明日の外出時は雨具をご準備ください。", legacyNowcast.localizedSubText(AppLanguage.JAPANESE))
    }

    // =========================================================================
    // 14. 降り止みの基準が 0.1mm/h 以下であることの検証
    // =========================================================================

    @Test
    fun testRainStoppingThresholdAt0Point1Mm() {
        val nowMillis = isoFormat.parse("2026-09-24T15:00")!!.time

        // 15:00は2.0mm。15:20にちょうど0.1mm/hになり、以降0.05mm, 0.0mmと継続
        val times = listOf(
            "2026-09-24T15:00",
            "2026-09-24T15:20",
            "2026-09-24T15:35",
            "2026-09-24T15:50"
        )
        val precips = listOf(2.0, 0.1, 0.05, 0.0)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 2.0,
            currentCondition = WeatherCondition.RAIN,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(61, 0, 0, 0),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAINING, nowcast.status)
        // 0.1mm/h 以下の地点（15:20 = 20分後）で降り止み判定されること
        assertEquals(20, nowcast.minutesRemaining)
        assertTrue(nowcast.headline.contains("15時20分頃に雨が止む見込みです"))
    }

    // =========================================================================
    // 15. 日本国内の場合にOpen-Meteoデータがシャットアウトされ気象庁ソースとなることの検証
    // =========================================================================

    @Test
    fun testJapanShutsOutOpenMeteoMinutelyData() {
        val nowMillis = isoFormat.parse("2026-09-24T15:00")!!.time

        // Open-Meteoから渡されたとする分単位データ (ダミー値 10.0mm/h)
        val openMeteoTimes = listOf(
            "2026-09-24T15:00",
            "2026-09-24T15:15",
            "2026-09-24T15:30"
        )
        val openMeteoPrecips = listOf(10.0, 10.0, 10.0)

        // 日本（東京）の座標とCountry
        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = openMeteoTimes,
            minutelyPrecips = openMeteoPrecips,
            minutelyCodes = listOf(65, 65, 65),
            hourlyForecast = listOf(
                HourlyForecast(
                    timeLabel = "現在",
                    hour = 15,
                    tempC = 20,
                    condition = WeatherCondition.CLOUDY,
                    precipProb = 10,
                    humidity = 60,
                    windSpeedKmH = 5.0,
                    dewPointC = 12,
                    isDay = true,
                    precipitationMm = 0.0
                )
            ),
            country = "Japan",
            latitude = 35.6895,
            longitude = 139.6917,
            nowMillis = nowMillis
        )

        // 日本フラグが有効
        assertTrue(nowcast.isJapan)
        assertFalse(nowcast.isUS)

        // データソースにOpen-Meteoが含まれず、気象庁 (JMA) となっていること
        assertEquals("気象庁 (JMA)", nowcast.dataSourceName)

        // Open-Meteoの豪雨データ (10.0mm/h) がシャットアウトされ、DRY状態であること
        assertEquals(RainStatus.DRY, nowcast.status)
        assertEquals(0.0, nowcast.currentIntensityMm, 0.001)
    }

    // =========================================================================
    // 16. ヨーロッパ地域の判定とECMWFデータソース帰属の検証
    // =========================================================================

    @Test
    fun testEuropeLocationDetection() {
        // ヨーロッパ主要都市
        assertTrue(isLocationOrCountryInEurope("France", 48.8566, 2.3522))
        assertTrue(isLocationOrCountryInEurope("フランス", 48.8566, 2.3522))
        assertTrue(isLocationOrCountryInEurope("FR", 48.8566, 2.3522))
        assertTrue(isLocationOrCountryInEurope("United Kingdom", 51.5074, -0.1278))
        assertTrue(isLocationOrCountryInEurope("Germany", 52.5200, 13.4050))
        assertTrue(isLocationOrCountryInEurope("Italy", 41.9028, 12.4964))

        // ヨーロッパ外
        assertFalse(isLocationOrCountryInEurope("Japan", 35.6895, 139.6917))
        assertFalse(isLocationOrCountryInEurope("日本", 35.6895, 139.6917))
        assertFalse(isLocationOrCountryInEurope("United States", 40.7128, -74.0060))
        assertFalse(isLocationOrCountryInEurope("Australia", -33.8688, 151.2093))
    }

    @Test
    fun testEuropeAttributionEcmwf() {
        val nowMillis = isoFormat.parse("2026-09-24T15:00")!!.time
        val times = listOf("2026-09-24T15:00", "2026-09-24T15:30")
        val precips = listOf(0.0, 0.0)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(3, 3),
            hourlyForecast = listOf(
                HourlyForecast(
                    timeLabel = "16:00",
                    hour = 16,
                    tempC = 18,
                    condition = WeatherCondition.RAIN,
                    precipProb = 60,
                    humidity = 70,
                    windSpeedKmH = 10.0,
                    dewPointC = 12,
                    isDay = true,
                    precipitationMm = 0.0
                )
            ),
            country = "France",
            latitude = 48.8566,
            longitude = 2.3522,
            nowMillis = nowMillis
        )

        assertTrue(nowcast.isEurope)
        assertFalse(nowcast.isJapan)
        assertFalse(nowcast.isUS)
        assertEquals("欧州中期予報センター (ECMWF) / 各気象機関", nowcast.dataSourceName)
        assertTrue(nowcast.subText.contains("ECMWF予報"))
    }

    // =========================================================================
    // 17. 海外における幽霊降水（Fake Rain: 5分で止むループ）の防止検証
    // =========================================================================

    @Test
    fun testOverseasNoFakeRainWhenZeroPrecip() {
        val nowMillis = isoFormat.parse("2026-09-24T15:00")!!.time
        // 天気コードはSHOWERSやRAINだが、実際の降水量は0.0、分単位データもすべて0.0
        val times = listOf(
            "2026-09-24T15:00",
            "2026-09-24T15:05",
            "2026-09-24T15:10",
            "2026-09-24T15:15"
        )
        val precips = listOf(0.0, 0.0, 0.0, 0.0)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.RAIN,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(80, 80, 80, 80),
            hourlyForecast = emptyList(),
            country = "France",
            latitude = 48.8566,
            longitude = 2.3522,
            nowMillis = nowMillis
        )

        // 降水量0.0の分単位データがある場合、人工的な1.5mm/hが注入されず、RAINING（あと5分で止む）にならないこと
        assertEquals(RainStatus.DRY, nowcast.status)
        assertFalse(nowcast.headline.contains("雨が止む見込み"))
    }

    // =========================================================================
    // 18. 降水継続時間に応じた段階的表現（Tiered Duration）の検証
    // =========================================================================

    @Test
    fun testTieredRainDurationUnder90Min() {
        val nowMillis = isoFormat.parse("2026-09-24T15:00")!!.time
        // 15:00から雨、15:35（35分後）に0.0mmで止む
        val times = listOf(
            "2026-09-24T15:00",
            "2026-09-24T15:20",
            "2026-09-24T15:35",
            "2026-09-24T15:45"
        )
        val precips = listOf(3.0, 2.0, 0.0, 0.0)

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 3.0,
            currentCondition = WeatherCondition.RAIN,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(61, 61, 0, 0),
            hourlyForecast = emptyList(),
            country = "France",
            latitude = 48.8566,
            longitude = 2.3522,
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAINING, nowcast.status)
        assertEquals(35, nowcast.minutesRemaining)
        // 90分以内: 「15時35分頃に雨が止む見込みです」
        assertTrue(nowcast.headline.contains("15時35分頃に雨が止む見込みです"))
    }

    @Test
    fun testTieredRainDurationBetween90And180Min() {
        val nowMillis = isoFormat.parse("2026-09-24T15:00")!!.time
        // 15:00は雨。分単位は60分後まで雨が続く
        val times = listOf(
            "2026-09-24T15:00",
            "2026-09-24T15:30",
            "2026-09-24T16:00"
        )
        val precips = listOf(2.5, 2.0, 1.5)

        // 時間予報で17:00まで雨、18:00に止む (17:00 = 120分後)
        val hourly = listOf(
            HourlyForecast(
                timeLabel = "15:00",
                hour = 15,
                tempC = 15,
                condition = WeatherCondition.RAIN,
                precipProb = 80,
                humidity = 85,
                windSpeedKmH = 10.0,
                dewPointC = 12,
                isDay = true,
                precipitationMm = 2.5
            ),
            HourlyForecast(
                timeLabel = "16:00",
                hour = 16,
                tempC = 15,
                condition = WeatherCondition.RAIN,
                precipProb = 70,
                humidity = 85,
                windSpeedKmH = 10.0,
                dewPointC = 12,
                isDay = true,
                precipitationMm = 2.0
            ),
            HourlyForecast(
                timeLabel = "17:00",
                hour = 17,
                tempC = 14,
                condition = WeatherCondition.RAIN,
                precipProb = 60,
                humidity = 80,
                windSpeedKmH = 8.0,
                dewPointC = 11,
                isDay = true,
                precipitationMm = 1.0
            ),
            HourlyForecast(
                timeLabel = "18:00",
                hour = 18,
                tempC = 14,
                condition = WeatherCondition.CLOUDY,
                precipProb = 10,
                humidity = 70,
                windSpeedKmH = 5.0,
                dewPointC = 9,
                isDay = true,
                precipitationMm = 0.0
            )
        )

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 2.5,
            currentCondition = WeatherCondition.RAIN,
            minutelyTimes = times,
            minutelyPrecips = precips,
            minutelyCodes = listOf(61, 61, 61),
            hourlyForecast = hourly,
            country = "France",
            latitude = 48.8566,
            longitude = 2.3522,
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAINING, nowcast.status)
        val mins = nowcast.minutesRemaining ?: 0
        assertTrue("minutesRemaining should be between 91 and 180, was $mins", mins in 91..180)
        // 91〜180分: 「〜頃まで雨が続く見込みです」（「止む」ではなく「続く」）
        assertTrue(nowcast.headline.contains("頃まで雨が続く見込みです"))
    }

    @Test
    fun testTieredRainDurationOver180Min() {
        val nowMillis = isoFormat.parse("2026-09-24T15:00")!!.time
        // 15:00〜19:00 (4時間以上) 降り続く
        val hourly = listOf(
            HourlyForecast("15:00", 15, 15, WeatherCondition.RAIN, 90, 90, 10.0, 12, true, 3.0),
            HourlyForecast("16:00", 16, 15, WeatherCondition.RAIN, 90, 90, 10.0, 12, true, 3.0),
            HourlyForecast("17:00", 17, 15, WeatherCondition.RAIN, 90, 90, 10.0, 12, true, 2.5),
            HourlyForecast("18:00", 18, 14, WeatherCondition.RAIN, 80, 85, 10.0, 12, true, 2.0),
            HourlyForecast("19:00", 19, 14, WeatherCondition.RAIN, 70, 80, 10.0, 11, true, 1.5),
            HourlyForecast("20:00", 20, 13, WeatherCondition.CLOUDY, 20, 70, 5.0, 9, false, 0.0)
        )

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 3.0,
            currentCondition = WeatherCondition.RAIN,
            minutelyTimes = listOf("2026-09-24T15:00", "2026-09-24T16:00"),
            minutelyPrecips = listOf(3.0, 3.0),
            minutelyCodes = listOf(61, 61),
            hourlyForecast = hourly,
            country = "United Kingdom",
            latitude = 51.5074,
            longitude = -0.1278,
            nowMillis = nowMillis
        )

        assertEquals(RainStatus.RAINING, nowcast.status)
        val mins = nowcast.minutesRemaining ?: 0
        assertTrue("minutesRemaining should be > 180, was $mins", mins > 180)
        // 181分以上: 「雨は当面の間（今後X時間）降り続く見込みです」
        assertTrue(nowcast.headline.contains("雨は当面の間"))
        assertTrue(nowcast.headline.contains("降り続く見込みです"))
    }
}


