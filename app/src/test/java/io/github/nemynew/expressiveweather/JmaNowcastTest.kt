package io.github.nemynew.expressiveweather

import io.github.nemynew.expressiveweather.data.JmaNowcastRepository
import io.github.nemynew.expressiveweather.data.RadarDataSource
import io.github.nemynew.expressiveweather.data.RadarFrame
import io.github.nemynew.expressiveweather.data.RadarRepository
import io.github.nemynew.expressiveweather.data.isLocationInJapan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class JmaNowcastTest {

    @Test
    fun testIsLocationInJapan() {
        // 日本国内
        assertTrue("Tokyo should be in Japan", isLocationInJapan(35.6812, 139.7671))
        assertTrue("Osaka should be in Japan", isLocationInJapan(34.6937, 135.5023))
        assertTrue("Sapporo should be in Japan", isLocationInJapan(43.0618, 141.3545))
        assertTrue("Wakkanai should be in Japan", isLocationInJapan(45.4156, 141.6734))
        assertTrue("Nemuro should be in Japan", isLocationInJapan(43.3301, 145.5828))
        assertTrue("Naha should be in Japan", isLocationInJapan(26.2124, 127.6809))
        assertTrue("Fukuoka should be in Japan", isLocationInJapan(33.5904, 130.4017))
        assertTrue("Tsushima should be in Japan", isLocationInJapan(34.2045, 129.2891))
        assertTrue("Goto should be in Japan", isLocationInJapan(32.6958, 128.8415))
        assertTrue("Yonaguni should be in Japan", isLocationInJapan(24.4558, 122.9341))
        assertTrue("Oki Islands should be in Japan", isLocationInJapan(36.2083, 133.3283))
        assertTrue("Sado Island should be in Japan", isLocationInJapan(38.0167, 138.3667))
        assertTrue("Noto Peninsula should be in Japan", isLocationInJapan(37.4000, 136.9000))
        assertTrue("Minamitorishima should be in Japan", isLocationInJapan(24.2817, 153.9789))

        // 日本国外
        assertFalse("New York should not be in Japan", isLocationInJapan(40.7128, -74.0060))
        assertFalse("London should not be in Japan", isLocationInJapan(51.5074, -0.1278))
        assertFalse("Paris should not be in Japan", isLocationInJapan(48.8566, 2.3522))
        assertFalse("Sydney should not be in Japan", isLocationInJapan(-33.8688, 151.2093))
        assertFalse("Seoul should not be in Japan", isLocationInJapan(37.5665, 126.9780))
        assertFalse("Busan should not be in Japan", isLocationInJapan(35.1796, 129.0756))
        assertFalse("Ulsan should not be in Japan", isLocationInJapan(35.5384, 129.3114))
        assertFalse("Jeju should not be in Japan", isLocationInJapan(33.4996, 126.5312))
        assertFalse("Taipei should not be in Japan", isLocationInJapan(25.0330, 121.5654))
        assertFalse("Shanghai should not be in Japan", isLocationInJapan(31.2304, 121.4737))
        assertFalse("Vladivostok should not be in Japan", isLocationInJapan(43.1155, 131.8855))
    }

    @Test
    fun testIsLocationOrCountryInJapan() {
        // 国名による絶対判定
        assertTrue(io.github.nemynew.expressiveweather.data.isLocationOrCountryInJapan("日本", 35.68, 139.69))
        assertTrue(io.github.nemynew.expressiveweather.data.isLocationOrCountryInJapan("Japan", 35.68, 139.69))
        assertFalse(io.github.nemynew.expressiveweather.data.isLocationOrCountryInJapan("アメリカ合衆国", 40.71, -74.01))
        assertFalse(io.github.nemynew.expressiveweather.data.isLocationOrCountryInJapan("United States", 40.71, -74.01))
        assertFalse(io.github.nemynew.expressiveweather.data.isLocationOrCountryInJapan("韓国", 37.56, 126.97))
        assertFalse(io.github.nemynew.expressiveweather.data.isLocationOrCountryInJapan("フランス", 48.85, 2.35))

        // 国名なしの場合は座標判定
        assertTrue(io.github.nemynew.expressiveweather.data.isLocationOrCountryInJapan(null, 35.68, 139.69))
        assertFalse(io.github.nemynew.expressiveweather.data.isLocationOrCountryInJapan(null, 40.71, -74.01))
    }

    @Test
    fun testRadarFrameForecastProperties() {
        val pastFrame = RadarFrame(
            time = 1789956000L,
            path = "https://www.jma.go.jp/bosai/jmatile/data/nowc/20260921020000/none/20260921020000/surf/hrpns/{z}/{x}/{y}.png",
            isForecast = false,
            tileUrlTemplate = "https://www.jma.go.jp/bosai/jmatile/data/nowc/20260921020000/none/20260921020000/surf/hrpns/{z}/{x}/{y}.png"
        )
        assertFalse(pastFrame.isForecast)
        assertNotNull(pastFrame.tileUrlTemplate)

        val forecastFrame = RadarFrame(
            time = 1789956900L,
            path = "https://www.jma.go.jp/bosai/jmatile/data/nowc/20260921020000/none/20260921021500/surf/hrpns/{z}/{x}/{y}.png",
            isForecast = true,
            tileUrlTemplate = "https://www.jma.go.jp/bosai/jmatile/data/nowc/20260921020000/none/20260921021500/surf/hrpns/{z}/{x}/{y}.png"
        )
        assertTrue(forecastFrame.isForecast)
        assertTrue(forecastFrame.tileUrlTemplate!!.contains("20260921021500"))
    }

    @Test
    fun testJmaUtcDateParsing() {
        val utcFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = utcFormat.parse("20260921020000")
        assertNotNull(date)
        val epochSec = date!!.time / 1000L
        assertEquals(1789956000L, epochSec)
    }

    @Test
    fun testRepositoriesSingleton() {
        val jmaRepo1 = JmaNowcastRepository.getInstance()
        val jmaRepo2 = JmaNowcastRepository.getInstance()
        assertEquals(jmaRepo1, jmaRepo2)

        val radarRepo1 = RadarRepository.getInstance()
        val radarRepo2 = RadarRepository.getInstance()
        assertEquals(radarRepo1, radarRepo2)
    }

    @Test
    fun testRadarDataSourceNames() {
        assertEquals("気象庁 高解像度降水ナウキャスト", RadarDataSource.JMA_NOWCAST.displayName)
        assertEquals("RainViewer", RadarDataSource.RAIN_VIEWER.displayName)
    }
}
