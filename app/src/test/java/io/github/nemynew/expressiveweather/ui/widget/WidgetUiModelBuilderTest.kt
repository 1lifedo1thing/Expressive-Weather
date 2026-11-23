package io.github.nemynew.expressiveweather.ui.widget

import io.github.nemynew.expressiveweather.data.AirQuality
import io.github.nemynew.expressiveweather.data.AlertLevel
import io.github.nemynew.expressiveweather.data.AlertSeverity
import io.github.nemynew.expressiveweather.data.AlertSource
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.DailyForecast
import io.github.nemynew.expressiveweather.data.GlobalAlertItem
import io.github.nemynew.expressiveweather.data.HourlyForecast
import io.github.nemynew.expressiveweather.data.MinutelyRainPoint
import io.github.nemynew.expressiveweather.data.MunicipalityAlertInfo
import io.github.nemynew.expressiveweather.data.MunicipalityWarningItem
import io.github.nemynew.expressiveweather.data.PrecipitationNowcast
import io.github.nemynew.expressiveweather.data.RainStatus
import io.github.nemynew.expressiveweather.data.SunSchedule
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.ui.components.WeatherWarningType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetUiModelBuilderTest {

    private val tokyo = City("tokyo", "東京", "Tokyo", "日本", 35.6895, 139.6917)
    private val osaka = City("osaka", "大阪", "Osaka", "日本", 34.6937, 135.5023)

    private fun sampleWeather(
        city: City = tokyo,
        alertInfo: MunicipalityAlertInfo? = null,
        globalAlerts: List<GlobalAlertItem> = emptyList(),
        nowcast: PrecipitationNowcast? = null
    ): WeatherData {
        return WeatherData(
            city = city,
            currentTempC = 20,
            feelsLikeC = 19,
            minTempC = 14,
            maxTempC = 25,
            condition = WeatherCondition.FEW_CLOUDS,
            conditionSummary = "晴れ時々曇り",
            humidity = 55,
            uvIndex = 4,
            uvLabel = "中",
            windSpeedKmH = 10.8,
            windDirection = "南東",
            windDegree = 135f,
            pressureHpa = 1015,
            visibilityKm = 12.0,
            airQuality = AirQuality(22, "良い", 5.0, 10.0, "快適"),
            sunSchedule = SunSchedule("06:10", "17:00", 0.5f, "10h 50m"),
            hourlyForecast = listOf(
                HourlyForecast("現在", 13, 20, WeatherCondition.FEW_CLOUDS, 10, 55, 10.8, isDay = true),
                HourlyForecast("14:00", 14, 22, WeatherCondition.CLEAR, 0, 50, 9.0, isDay = true),
                HourlyForecast("15:00", 15, 21, WeatherCondition.RAIN, 60, 68, 12.0, isDay = true)
            ),
            dailyForecast = listOf(
                DailyForecast("今日", "11/21", WeatherCondition.FEW_CLOUDS, 14, 25, 20),
                DailyForecast("明日", "11/22", WeatherCondition.RAIN, 12, 18, 80)
            ),
            lastUpdatedTime = "13:30",
            isDay = true,
            alertInfo = alertInfo,
            globalAlerts = globalAlerts,
            precipitationNowcast = nowcast
        )
    }

    @Test
    fun sizeTier_mapsResponsiveBreakpointsCorrectly() {
        // 通常の 2x2 コンパクトウィジェットはサイズが広がっても時間別予報レイアウトに昇格しない
        assertEquals(WidgetSizeTier.COMPACT_WIDE, WidgetSizeTier.fromSizeDp(140f, 60f, WidgetSizeTier.COMPACT))
        assertEquals(WidgetSizeTier.COMPACT_WIDE, WidgetSizeTier.fromSizeDp(467f, 89f, WidgetSizeTier.COMPACT))
        assertEquals(WidgetSizeTier.COMPACT, WidgetSizeTier.fromSizeDp(140f, 130f, WidgetSizeTier.COMPACT))
        assertEquals(WidgetSizeTier.COMPACT, WidgetSizeTier.fromSizeDp(280f, 130f, WidgetSizeTier.COMPACT))
        assertEquals(WidgetSizeTier.COMPACT, WidgetSizeTier.fromSizeDp(390f, 260f, WidgetSizeTier.COMPACT))

        // 中・大ウィジェットは従来通りレスポンシブにレイアウトを切り替える
        assertEquals(WidgetSizeTier.WIDE_HOURLY, WidgetSizeTier.fromSizeDp(630f, 105f, WidgetSizeTier.MEDIUM))
        assertEquals(WidgetSizeTier.WIDE_HOURLY, WidgetSizeTier.fromSizeDp(520f, 128f, WidgetSizeTier.MEDIUM))
        assertEquals(WidgetSizeTier.COMPACT, WidgetSizeTier.fromSizeDp(140f, 130f, WidgetSizeTier.MEDIUM))
        assertEquals(WidgetSizeTier.MEDIUM, WidgetSizeTier.fromSizeDp(280f, 130f, WidgetSizeTier.MEDIUM))
        assertEquals(WidgetSizeTier.LARGE, WidgetSizeTier.fromSizeDp(280f, 240f, WidgetSizeTier.MEDIUM))
        assertEquals(WidgetSizeTier.LARGE, WidgetSizeTier.fromSizeDp(280f, 320f, WidgetSizeTier.LARGE))
        assertEquals(WidgetSizeTier.LARGE, WidgetSizeTier.fromSizeDp(0f, 0f, WidgetSizeTier.LARGE))
    }

    @Test
    fun resolveTargetCity_usesPinnedCityWhenConfigured() {
        val currentCfg = WidgetConfig(appWidgetId = 1, useCurrentLocation = true, pinnedCity = osaka)
        val pinnedCfg = WidgetConfig(appWidgetId = 2, useCurrentLocation = false, pinnedCity = osaka)

        assertEquals(tokyo, WidgetUiModelBuilder.resolveTargetCity(currentCfg, tokyo, tokyo))
        assertEquals(osaka, WidgetUiModelBuilder.resolveTargetCity(pinnedCfg, tokyo, tokyo))
    }

    @Test
    fun build_nullWeather_producesLocalizedPlaceholder() {
        val cfg = WidgetConfig(appWidgetId = 10, useCurrentLocation = false, pinnedCity = osaka)
        val modelJa = WidgetUiModelBuilder.build(
            config = cfg,
            weather = null,
            language = AppLanguage.JAPANESE,
            temperatureUnit = TemperatureUnit.CELSIUS,
            windSpeedUnit = WindSpeedUnit.MS,
            fallbackCity = tokyo,
            isRefreshing = false
        )
        assertFalse(modelJa.hasData)
        assertEquals("大阪", modelJa.cityName)
        assertEquals("--°", modelJa.currentTempText)
        assertEquals("タップして天気を取得", modelJa.conditionLabel)

        val modelEn = WidgetUiModelBuilder.build(
            config = cfg,
            weather = null,
            language = AppLanguage.ENGLISH,
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            windSpeedUnit = WindSpeedUnit.MPH,
            fallbackCity = tokyo,
            isRefreshing = true
        )
        assertEquals("Osaka", modelEn.cityName)
        assertEquals("Updating…", modelEn.conditionLabel)
    }

    @Test
    fun build_convertsTemperatureUnitsAndLocalizesForecasts() {
        val cfg = WidgetConfig(appWidgetId = 11, useCurrentLocation = true)
        val weather = sampleWeather()

        val sampleCal = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.NOVEMBER, 21, 13, 30) }
        val modelF = WidgetUiModelBuilder.build(
            config = cfg,
            weather = weather,
            language = AppLanguage.ENGLISH,
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            windSpeedUnit = WindSpeedUnit.KMH,
            fallbackCity = tokyo,
            nowMillis = sampleCal.timeInMillis
        )

        assertTrue(modelF.hasData)
        assertEquals("Tokyo", modelF.cityName)
        assertEquals("68°", modelF.currentTempText) // 20C = 68F
        assertEquals("↑77° ↓57°", modelF.highLowText)
        assertEquals(3, modelF.hourlyItems.size)
        assertEquals("Now", modelF.hourlyItems[0].timeLabel)
        assertEquals("14:00", modelF.hourlyItems[1].timeLabel)
        assertEquals("60%", modelF.hourlyItems[2].precipProbText)
        assertEquals(2, modelF.dailyItems.size)
        assertEquals("Today", modelF.dailyItems[0].dayLabel)
        assertEquals("Tomorrow", modelF.dailyItems[1].dayLabel)
    }

    @Test
    fun build_omitsKikikuruAndDisasterAlertBannersFromWidgets() {
        val cfg = WidgetConfig(appWidgetId = 12, showAlertsAndNowcast = true)
        val nowcast = PrecipitationNowcast(
            status = RainStatus.RAIN_APPROACHING,
            headline = "あと15分で雨が降り出します",
            subText = "傘をお持ちください",
            minutelyPoints = listOf(
                MinutelyRainPoint("13:35", 0L, 0.2),
                MinutelyRainPoint("13:45", 0L, 1.5),
                MinutelyRainPoint("13:55", 0L, 3.0)
            )
        )
        val jmaAlert = MunicipalityAlertInfo(
            municipalityName = "千代田区",
            alertLevel = AlertLevel.LEVEL_3,
            activeWarnings = listOf(MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true))
        )
        val model = WidgetUiModelBuilder.build(
            config = cfg,
            weather = sampleWeather(
                alertInfo = jmaAlert,
                globalAlerts = listOf(
                    GlobalAlertItem(
                        id = "nws1",
                        source = AlertSource.NWS,
                        type = WeatherWarningType.WARNING,
                        severity = AlertSeverity.SEVERE,
                        eventName = "Severe Thunderstorm Warning",
                        headline = "Storm incoming",
                        description = "Details"
                    )
                ),
                nowcast = nowcast
            ),
            language = AppLanguage.JAPANESE,
            temperatureUnit = TemperatureUnit.CELSIUS,
            windSpeedUnit = WindSpeedUnit.MS,
            fallbackCity = tokyo
        )
        assertNull(model.alertBanner)
    }

    @Test
    fun widgetConfig_defaultsToMaterialYouStyle() {
        val defaultCfg = WidgetConfig(appWidgetId = 101)
        assertEquals(WidgetStyle.MATERIAL_YOU, defaultCfg.style)
        assertEquals(WidgetStyle.MATERIAL_YOU, WidgetStyle.fromId(null))
        assertEquals(WidgetStyle.MATERIAL_YOU, WidgetStyle.fromId("unknown_style"))
        assertEquals(WidgetStyle.MATERIAL_YOU, WidgetStyle.entries.first())
    }

    @Test
    fun widgetConfig_deserializesLegacyJsonWithoutShapeFields() {
        val legacyJson = """{"appWidgetId":42,"useCurrentLocation":true,"style":"MATERIAL_YOU"}"""
        val decoded = io.github.nemynew.expressiveweather.data.WeatherJsonSerializer.json.decodeFromString<WidgetConfig>(legacyJson)
        assertEquals(42, decoded.appWidgetId)
        assertEquals(WidgetStyle.MATERIAL_YOU, decoded.style)
        assertEquals(MaterialWidgetShape.PILL, decoded.shapeType)
        assertFalse(decoded.showCityOnShape)
        assertEquals(0.72f, decoded.glassOpacity, 0.001f)

        val frostedCfg = WidgetConfig(
            appWidgetId = 77,
            style = WidgetStyle.FROSTED_GLASS,
            glassOpacity = 0.42f
        )
        val encoded = io.github.nemynew.expressiveweather.data.WeatherJsonSerializer.json.encodeToString(WidgetConfig.serializer(), frostedCfg)
        val reDecoded = io.github.nemynew.expressiveweather.data.WeatherJsonSerializer.json.decodeFromString<WidgetConfig>(encoded)
        assertEquals(WidgetStyle.FROSTED_GLASS, reDecoded.style)
        assertEquals(0.42f, reDecoded.glassOpacity, 0.001f)
        assertFalse(WidgetStyle.FROSTED_GLASS.getDisplayName(AppLanguage.JAPANESE).contains("ガラス"))
        assertFalse(WidgetStyle.FROSTED_GLASS.getDescription(AppLanguage.JAPANESE).contains("ガラス"))
    }

    @Test
    fun build_showsAll7DaysAndAdvancesDayLabelsWhenDateRollsOver() {
        val sevenDays = listOf(
            DailyForecast("今日", "11/21", WeatherCondition.CLEAR, 8, 15, 0),
            DailyForecast("明日", "11/22", WeatherCondition.FEW_CLOUDS, 9, 16, 10),
            DailyForecast("月", "11/23", WeatherCondition.CLOUDY, 10, 14, 20),
            DailyForecast("火", "11/24", WeatherCondition.RAIN, 7, 13, 60),
            DailyForecast("水", "11/25", WeatherCondition.CLEAR, 6, 15, 0),
            DailyForecast("木", "11/26", WeatherCondition.FEW_CLOUDS, 8, 17, 10),
            DailyForecast("金", "11/27", WeatherCondition.CLEAR, 9, 18, 0)
        )
        val weather = sampleWeather().copy(dailyForecast = sevenDays)
        val cfg = WidgetConfig(appWidgetId = 55)

        val nov21Cal = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.NOVEMBER, 21, 10, 0) }
        val sameDayModel = WidgetUiModelBuilder.build(
            config = cfg,
            weather = weather,
            language = AppLanguage.JAPANESE,
            temperatureUnit = TemperatureUnit.CELSIUS,
            windSpeedUnit = WindSpeedUnit.MS,
            fallbackCity = tokyo,
            nowMillis = nov21Cal.timeInMillis
        )
        assertEquals(listOf("今日", "明日", "月", "火", "水", "木", "金"), sameDayModel.dailyItems.map { it.dayLabel })

        // 日付が 11/22（日曜）へ進んだ場合はキャッシュ上の 11/21 を除外し、曜日を正しく再計算する
        val nov22Cal = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.NOVEMBER, 22, 0, 5) }
        val nextDayModel = WidgetUiModelBuilder.build(
            config = cfg,
            weather = weather,
            language = AppLanguage.JAPANESE,
            temperatureUnit = TemperatureUnit.CELSIUS,
            windSpeedUnit = WindSpeedUnit.MS,
            fallbackCity = tokyo,
            nowMillis = nov22Cal.timeInMillis
        )
        assertEquals(listOf("今日", "明日", "火", "水", "木", "金"), nextDayModel.dailyItems.map { it.dayLabel })
    }

    @Test
    fun isTallRadarSize_detectsTabletPortraitAndLandscapeBuckets() {
        assertFalse(WeatherWidgetUpdater.isTallRadarSize(360f, 160f))
        assertFalse(WeatherWidgetUpdater.isTallRadarSize(420f, 220f))
        assertTrue(WeatherWidgetUpdater.isTallRadarSize(240f, 240f))
        assertTrue(WeatherWidgetUpdater.isTallRadarSize(260f, 320f))
        assertTrue(WeatherWidgetUpdater.isTallRadarSize(0f, 220f))
    }

    @Test
    fun refineRadarOverlayPixel_softensDarkModeJmaPaleBorderOnly() {
        val jmaPaleMist = (255 shl 24) or (242 shl 16) or (242 shl 8) or 255
        val jmaModerateBlue = (255 shl 24) or (33 shl 16) or (140 shl 8) or 255

        assertEquals(0, WidgetRadarMapRenderer.refineRadarOverlayPixel(0x00FFFFFF, isSystemDark = true))
        assertEquals(jmaPaleMist, WidgetRadarMapRenderer.refineRadarOverlayPixel(jmaPaleMist, isSystemDark = false))

        val darkMist = WidgetRadarMapRenderer.refineRadarOverlayPixel(jmaPaleMist, isSystemDark = true)
        val darkMistAlpha = (darkMist ushr 24) and 0xFF
        assertTrue(darkMistAlpha in 90..130)

        assertEquals(jmaModerateBlue, WidgetRadarMapRenderer.refineRadarOverlayPixel(jmaModerateBlue, isSystemDark = true))
    }
}
