package io.github.nemynew.expressiveweather.data

import com.google.android.gms.maps.model.LatLng
import io.github.nemynew.expressiveweather.ui.radar.RadarDataHolder
import org.json.JSONArray
import org.json.JSONObject
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

/**
 * フルコードレビューで修正した Critical / High 不具合の再発防止回帰テスト群
 */
class CodeReviewRegressionTest {

    private var savedDefaultTimeZone: TimeZone? = null

    @Before
    fun setUp() {
        savedDefaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
    }

    @After
    fun tearDown() {
        savedDefaultTimeZone?.let { TimeZone.setDefault(it) }
        RadarDataHolder.clear()
    }

    // -------------------------------------------------------------------------
    // 1. JmaAlertRepository: 非雨系特別警報・浸水害 attentions・空エリア名フォールバック
    // -------------------------------------------------------------------------

    @Test
    fun jmaAlert_stormSpecialWarning_resolvesToStormWindLevel5() {
        val muni = JmaMunicipality("1310100", "131016", "千代田区", "ちよだく", "130000", "東京都", "東京都")
        val alert = JmaAlertRepository.evaluateParsedWarnings(
            municipality = muni,
            distinctActiveWarnings = listOf(
                MunicipalityWarningItem(code = "35", name = "暴風特別警報", status = "発表", isSpecial = true, isWarning = true)
            ),
            headlineText = "東京都では暴風に特別警戒してください。"
        )
        assertEquals(AlertLevel.LEVEL_5, alert.alertLevel)
        assertEquals(DisasterType.STORM_WIND, alert.targetDisasterPrimary)
        assertTrue(alert.disasterDetails.any { it.disasterType == DisasterType.STORM_WIND && it.alertLevel == AlertLevel.LEVEL_5 })
        assertTrue(alert.hasAnyDisasterInfo)
    }

    @Test
    fun jmaAlert_heavyRainWithInundationAttention_setsInundationLevel3() {
        val muni = JmaMunicipality("1310100", "131016", "千代田区", "ちよだく", "130000", "東京都", "東京都")
        val alert = JmaAlertRepository.evaluateParsedWarnings(
            municipality = muni,
            distinctActiveWarnings = listOf(
                MunicipalityWarningItem(code = "03", name = "大雨警報", status = "発表", isWarning = true)
            ),
            activeAttentions = setOf("浸水害"),
            headlineText = "低い土地の浸水に警戒してください。"
        )
        assertEquals(AlertLevel.LEVEL_3, alert.alertLevel)
        assertTrue(alert.disasterFlags.inundation)
        assertTrue(alert.disasterDetails.any { it.disasterType == DisasterType.INUNDATION && it.alertLevel == AlertLevel.LEVEL_3 })
    }

    @Test
    fun jmaAlert_noWarningsProducesLevel0AndNoDisasterInfo() {
        val muni = JmaMunicipality("1310100", "131016", "千代田区", "ちよだく", "130000", "東京都", "東京都")
        val alert = JmaAlertRepository.evaluateParsedWarnings(
            municipality = muni,
            distinctActiveWarnings = emptyList()
        )
        assertEquals(AlertLevel.LEVEL_0, alert.alertLevel)
        assertTrue(alert.activeWarnings.isEmpty())
        assertFalse(alert.hasAnyDisasterInfo)
    }

    // -------------------------------------------------------------------------
    // 2. MeteoAlarmRepository: 非加盟国除外 & 単語境界 severity 判定
    // -------------------------------------------------------------------------

    @Test
    fun meteoAlarm_nonMemberCountriesAreExcluded() {
        val moscow = City("ru", "モスクワ", "Moscow", "Russia", 55.7558, 37.6173)
        val istanbul = City("tr", "イスタンブール", "Istanbul", "Turkey", 41.0082, 28.9784)
        val minsk = City("by", "ミンスク", "Minsk", "Belarus", 53.9006, 27.5590)
        val cairo = City("eg", "カイロ", "Cairo", "Egypt", 30.0444, 31.2357)
        val paris = City("fr", "パリ", "Paris", "France", 48.8566, 2.3522)

        assertFalse(MeteoAlarmRepository.isSupported(moscow))
        assertFalse(MeteoAlarmRepository.isSupported(istanbul))
        assertFalse(MeteoAlarmRepository.isSupported(minsk))
        assertFalse(MeteoAlarmRepository.isSupported(cairo))
        assertTrue(MeteoAlarmRepository.isSupported(paris))
    }

    @Test
    fun meteoAlarm_substringRedDoesNotFalseEscalateToExtreme() {
        val entry = MeteoAlarmRepository.MeteoAlarmRawEntry(
            id = "test-1",
            title = "Prepared wind advisory for coastal area",
            event = "Moderate Wind Advisory",
            areaDesc = "Coastal",
            severity = "Moderate",
            onset = "",
            expires = "",
            sent = "",
            identifier = "id-1",
            capXmlUrl = null
        )
        val (_, severity) = MeteoAlarmRepository.determineTypeAndSeverity(entry)
        assertEquals(AlertSeverity.MODERATE, severity)
    }

    // -------------------------------------------------------------------------
    // 3. PrecipitationAnalyzer: 都市タイムゾーン伝播
    // -------------------------------------------------------------------------

    @Test
    fun precipitationAnalyzer_usesCityTimeZoneInsteadOfHostJst() {
        // UTC 2026-09-24 18:00 = JST 2026-09-25 03:00, EDT (UTC-4) 2026-09-24 14:00
        val nyTz = TimeZone.getTimeZone("America/New_York")
        val nyFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = nyTz }
        val nowMillis = nyFmt.parse("2026-09-24T14:00")!!.time

        val nowcast = PrecipitationAnalyzer.analyze(
            currentPrecipMm = 0.0,
            currentCondition = WeatherCondition.CLOUDY,
            minutelyTimes = listOf(
                "2026-09-24T14:00",
                "2026-09-24T14:20",
                "2026-09-24T14:25",
                "2026-09-24T14:30"
            ),
            minutelyPrecips = listOf(0.0, 0.0, 2.0, 2.5),
            minutelyCodes = listOf(3, 3, 61, 61),
            hourlyForecast = emptyList(),
            nowMillis = nowMillis,
            country = "United States",
            latitude = 40.7128,
            longitude = -74.0060,
            timeZone = nyTz
        )

        assertEquals(RainStatus.RAIN_APPROACHING, nowcast.status)
        assertEquals("14:25", nowcast.localizedTargetTimeLabel(AppLanguage.ENGLISH))
        assertTrue(nowcast.localizedHeadline(AppLanguage.ENGLISH).contains("14:25"))
    }

    // -------------------------------------------------------------------------
    // 4. RadarDataHolder: 初期状態が空/NaN（東京フォールバック禁止）
    // -------------------------------------------------------------------------

    @Test
    fun radarDataHolder_defaultAndClearedStateAreNotTokyo() {
        RadarDataHolder.clear()
        assertNull(RadarDataHolder.city)
        assertEquals("", RadarDataHolder.cityName)
        assertNull(RadarDataHolder.country)
        assertTrue(RadarDataHolder.latitude.isNaN())
        assertTrue(RadarDataHolder.longitude.isNaN())
    }

    // -------------------------------------------------------------------------
    // 5. JmaTyphoonModels: 多言語重複番号修正 & 熱帯低気圧翻訳
    // -------------------------------------------------------------------------

    @Test
    fun jmaTyphoonModels_localizedNameAndClassificationFormatCleanly() {
        val tdPoint = JmaTyphoonPoint(
            partName = "実況",
            isAnalysis = true,
            advancedHours = 0,
            validTimeJst = "2026-10-01T18:00:00+09:00",
            validTimeFormatted = "10/01 18:00",
            latitude = 20.0,
            longitude = 135.0,
            locationName = "南シナ海",
            course = "北西",
            speedText = "15 km/h",
            pressureHpa = 1002,
            maxWindMs = 15,
            maxWindKt = 30,
            gustWindMs = 23,
            gustWindKt = 45,
            intensity = JmaTyphoonIntensity.NONE,
            scale = JmaTyphoonScale.NONE,
            categoryName = "熱帯低気圧"
        )
        assertEquals("Tropical Depression", tdPoint.officialClassification(AppLanguage.ENGLISH))
        assertEquals("热带低压", tdPoint.officialClassification(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("熱帶性低氣壓", tdPoint.officialClassification(AppLanguage.TRADITIONAL_CHINESE))

        val item = JmaTyphoonItem(
            id = "TC2627",
            rawTyphoonNumber = "2627",
            formattedNumber = "第27号",
            nameJp = "チョーイワン",
            nameEn = "Choi-wan",
            issueTimeJst = "2026-10-01T19:05:00+09:00",
            formattedIssueTime = "10/01 19:05",
            analysis = tdPoint,
            forecasts = emptyList(),
            pastTrack = listOf(LatLng(19.0, 136.0), LatLng(20.0, 135.0)),
            galeWarningRadiusMeters = null,
            stormWarningRadiusMeters = null,
            probabilityCircles = emptyList()
        )
        assertEquals("Typhoon No.27 (Choi-wan)", item.localizedName(AppLanguage.ENGLISH))
        assertEquals("第27号台风 (Choi-wan)", item.localizedName(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("第27號颱風 (Choi-wan)", item.localizedName(AppLanguage.TRADITIONAL_CHINESE))
    }

    // -------------------------------------------------------------------------
    // 6. KikikuruGuidelineContent & GlobalAlertModels (繁体字 / STORM_WIND Lv.0)
    // -------------------------------------------------------------------------

    @Test
    fun guidelineAndNwsTranslation_handleStormWindLevel0AndTraditionalChinese() {
        val advice = KikikuruGuidelineContent.getActionAdvice(DisasterType.STORM_WIND, AlertLevel.LEVEL_0, AppLanguage.ENGLISH)
        assertTrue(advice.isNotBlank())
        assertFalse(advice.contains("Level 5", ignoreCase = true))

        val zhHant = GlobalAlertItem.translateNwsEvent("Coastal Tornado Alert", AppLanguage.TRADITIONAL_CHINESE)
        assertEquals("龍捲風警報", zhHant)
        assertFalse(zhHant.contains("警报"))
    }

    // -------------------------------------------------------------------------
    // 7. UsRadarModels: "usa" 部分一致 & 中国語国名除外
    // -------------------------------------------------------------------------

    @Test
    fun usRadarRegionDetection_excludesJerusalemAndChineseCanada() {
        assertFalse(isLocationOrCountryInUnitedStates("Jerusalem", 31.7683, 35.2137))
        assertFalse(isLocationOrCountryInUnitedStates("Busan", 35.1796, 129.0756))
        assertFalse(isLocationOrCountryInUnitedStates("加拿大", 43.6532, -79.3832))
        assertFalse(isLocationOrCountryInUnitedStates("墨西哥", 25.6866, -100.3161))
        assertTrue(isLocationOrCountryInUnitedStates("USA", 40.7128, -74.0060))
        assertTrue(isLocationOrCountryInUnitedStates("美国", 34.0522, -118.2437))
    }

    // -------------------------------------------------------------------------
    // 8. WeatherJsonSerializer: NaN / Infinity サニタイズ
    // -------------------------------------------------------------------------

    @Test
    fun weatherJsonSerializer_sanitizesNonFiniteFloatsWithoutThrowing() {
        val dirtyWeather = WeatherData(
            city = City("t1", "テスト", "Test", "JP", Double.NaN, Double.POSITIVE_INFINITY),
            currentTempC = 20,
            feelsLikeC = 20,
            minTempC = 15,
            maxTempC = 25,
            condition = WeatherCondition.CLEAR,
            conditionSummary = "Clear",
            humidity = 50,
            uvIndex = 3,
            uvLabel = "Moderate",
            windSpeedKmH = Double.NaN,
            windDirection = "N",
            windDegree = Float.NaN,
            pressureHpa = 1013,
            visibilityKm = Double.POSITIVE_INFINITY,
            airQuality = AirQuality(20, "Good", Double.NaN, Double.NEGATIVE_INFINITY, "", ozone = Double.NaN),
            sunSchedule = SunSchedule("06:00", "18:00", Float.NaN, "12h"),
            hourlyForecast = listOf(
                HourlyForecast("12:00", 12, 20, WeatherCondition.CLEAR, 0, 50, Double.NaN, precipitationMm = Double.POSITIVE_INFINITY)
            ),
            dailyForecast = emptyList(),
            lastUpdatedTime = "12:00",
            windGustsKmH = Double.NaN,
            precipitationMm = Double.NaN,
            precipitationSumMm = Double.NaN
        )

        val encoded = WeatherJsonSerializer.serialize(dirtyWeather)
        assertTrue(encoded.isNotBlank())
        val decoded = WeatherJsonSerializer.deserialize(encoded)
        assertNotNull(decoded)
        assertEquals(0.0, decoded!!.windSpeedKmH, 0.001)
        assertEquals(0.5f, decoded.sunSchedule.dayProgress, 0.001f)
    }
}
