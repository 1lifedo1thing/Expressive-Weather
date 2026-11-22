package io.github.nemynew.expressiveweather.data

import io.github.nemynew.expressiveweather.data.local.LocationKeyNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * データモデル・正規化・境界値・並行安全性の包括的回帰テスト
 */
class CoreDomainRegressionTest {

    @Test
    fun beaufortScale_handlesGapAndEdgeSpeedsContinuously() {
        assertEquals(0, BeaufortScale.fromSpeedMs(Double.NaN).level)
        assertEquals(0, BeaufortScale.fromSpeedMs(-1.0).level)
        assertEquals(0, BeaufortScale.fromSpeedMs(0.0).level)
        assertEquals(0, BeaufortScale.fromSpeedMs(0.25).level)
        assertEquals(1, BeaufortScale.fromSpeedMs(0.3).level)
        assertEquals(1, BeaufortScale.fromSpeedMs(1.55).level)
        assertEquals(2, BeaufortScale.fromSpeedMs(1.6).level)
        assertEquals(2, BeaufortScale.fromSpeedMs(3.35).level)
        assertEquals(3, BeaufortScale.fromSpeedMs(3.4).level)
        assertEquals(3, BeaufortScale.fromSpeedMs(5.45).level)
        assertEquals(4, BeaufortScale.fromSpeedMs(5.5).level)
        assertEquals(5, BeaufortScale.fromSpeedMs(8.0).level)
        assertEquals(6, BeaufortScale.fromSpeedMs(10.8).level)
        assertEquals(7, BeaufortScale.fromSpeedMs(13.9).level)
        assertEquals(8, BeaufortScale.fromSpeedMs(17.2).level)
        assertEquals(9, BeaufortScale.fromSpeedMs(20.8).level)
        assertEquals(10, BeaufortScale.fromSpeedMs(24.5).level)
        assertEquals(11, BeaufortScale.fromSpeedMs(28.5).level)
        assertEquals(12, BeaufortScale.fromSpeedMs(32.7).level)
        assertEquals(12, BeaufortScale.fromSpeedMs(65.0).level)
    }

    @Test
    fun locationKeyNormalizer_eliminatesNegativeZeroAndNormalizesTwoDecimals() {
        assertEquals("0.00_0.00", LocationKeyNormalizer.normalize(0.0, 0.0))
        assertEquals("0.00_0.00", LocationKeyNormalizer.normalize(-0.0, -0.0))
        assertEquals("0.00_0.00", LocationKeyNormalizer.normalize(-0.001, -0.004))
        assertEquals("35.69_139.69", LocationKeyNormalizer.normalize(35.6895, 139.6917))
        assertEquals("-33.87_151.21", LocationKeyNormalizer.normalize(-33.8688, 151.2093))
    }

    @Test
    fun jmaAreaResolver_doesNotMisclassifySubstringCitiesAsMieTsu() {
        // 三重県津市 -> 240000 (津地方気象台)
        val tsu = JmaAreaResolver.resolve(34.730, 136.508, "津市", "日本")
        assertEquals("240000", tsu.officeCode)

        // 滋賀県大津市 -> 250000 (彦根地方気象台)
        val otsu = JmaAreaResolver.resolve(35.017, 135.854, "大津市", "日本")
        assertEquals("250000", otsu.officeCode)

        // 大分県中津市 -> 440000 (大分地方気象台) ※「津」に誤爆しないこと
        val nakatsu = JmaAreaResolver.resolve(33.598, 131.188, "中津市", "日本")
        assertFalse("Nakatsu should not resolve to Mie (240000)", nakatsu.officeCode == "240000")

        // 佐賀県唐津市 -> 410000 (佐賀地方気象台)
        val karatsu = JmaAreaResolver.resolve(33.449, 129.968, "唐津市", "日本")
        assertEquals("410000", karatsu.officeCode)

        // 愛媛県松山市 ("Matsuyama") -> 380000 (松山地方気象台) ※"Tsu"に誤爆しないこと
        val matsuyama = JmaAreaResolver.resolve(33.839, 132.765, "Matsuyama", "Japan")
        assertEquals("380000", matsuyama.officeCode)
    }

    @Test
    fun japanAndUsLocationDetection_respectsExplicitForeignCountriesInBoundingBox() {
        // 東京 -> 日本 true
        assertTrue(isLocationOrCountryInJapan("日本", 35.6895, 139.6917))
        assertTrue(isLocationOrCountryInJapan("Japan", 35.6895, 139.6917))
        assertTrue(isLocationOrCountryInJapan(null, 35.6895, 139.6917))

        // 韓国・釜山やロシア・ウラジオストクは日本の外枠矩形に近くても国名が明示されていれば false
        assertFalse(isLocationOrCountryInJapan("South Korea", 35.1796, 129.0756))
        assertFalse(isLocationOrCountryInJapan("韓国", 35.1796, 129.0756))
        assertFalse(isLocationOrCountryInJapan("Russia", 43.1155, 131.8855))
        assertFalse(isLocationOrCountryInJapan("Taiwan", 25.0330, 121.5654))
    }

    @Test
    fun radarProviderRegistry_concurrentAccessIsThreadSafe() = runBlocking {
        val jobs = (1..32).map { idx ->
            async(Dispatchers.Default) {
                val source = when (idx % 3) {
                    0 -> RadarProviderRegistry.resolveProvider(RadarSourceMode.AUTO, 35.6895, 139.6917, "日本")
                    1 -> RadarProviderRegistry.resolveProvider(RadarSourceMode.AUTO, 40.7128, -74.0060, "United States")
                    else -> RadarProviderRegistry.resolveProvider(RadarSourceMode.AUTO, 48.8566, 2.3522, "France")
                }
                assertNotNull(RadarProviderRegistry.get(source))
                source
            }
        }
        val results = jobs.awaitAll()
        assertEquals(32, results.size)
    }

    @Test
    fun windSpeedUnit_formatsCorrectlyAcrossLanguagesAndUnits() {
        // 10.0 m/s = 36.0 km/h
        assertEquals("10.0 m/s", WindSpeedUnit.MS.formatFromMs(10.0, AppLanguage.JAPANESE))
        assertEquals("36 km/h", WindSpeedUnit.KMH.format(36.0, AppLanguage.ENGLISH))
        assertEquals("10.0 m/s", WindSpeedUnit.FOLLOW_LANGUAGE.formatFromMs(10.0, AppLanguage.JAPANESE))
        assertEquals("36 km/h", WindSpeedUnit.FOLLOW_LANGUAGE.format(36.0, AppLanguage.ENGLISH))
        assertEquals("22 mph", WindSpeedUnit.MPH.format(36.0, AppLanguage.ENGLISH))
    }
}
