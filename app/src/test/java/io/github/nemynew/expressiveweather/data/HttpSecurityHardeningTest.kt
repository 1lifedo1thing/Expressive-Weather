package io.github.nemynew.expressiveweather.data

import io.github.nemynew.expressiveweather.ui.radar.RadarTileHttpClient
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HttpSecurityHardeningTest {

    @Test
    fun testReadBytesBoundedAllowsUpToExactLimit() {
        val payload = ByteArray(1024) { (it % 127).toByte() }
        val body = payload.toResponseBody("application/octet-stream".toMediaType())

        val result = AppHttpClient.readBytesBounded(body, maxBytes = 1024L)
        assertArrayEquals(payload, result)
    }

    @Test
    fun testReadBytesBoundedRejectsDeclaredContentLengthOverLimit() {
        val payload = ByteArray(1025) { 1 }
        val body = payload.toResponseBody("application/octet-stream".toMediaType())

        try {
            AppHttpClient.readBytesBounded(body, maxBytes = 1024L)
            fail("Expected ResponseSizeLimitExceededException")
        } catch (_: ResponseSizeLimitExceededException) {
            // expected
        }
    }

    @Test
    fun testReadBytesBoundedRejectsChunkedUnknownLengthStreamOverLimit() {
        val payload = ByteArray(2048) { 2 }
        val unknownLengthBody = object : ResponseBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength(): Long = -1L
            override fun source(): BufferedSource = Buffer().write(payload)
        }

        try {
            AppHttpClient.readBytesBounded(unknownLengthBody, maxBytes = 1024L)
            fail("Expected ResponseSizeLimitExceededException for chunked body")
        } catch (_: ResponseSizeLimitExceededException) {
            // expected
        }
    }

    @Test
    fun testReadTextBoundedDecodesUtf8WithinLimit() {
        val text = """{"weather":"くもり 昼過ぎから夕方雨"}"""
        val body = text.toResponseBody("application/json; charset=utf-8".toMediaType())

        assertEquals(text, AppHttpClient.readTextBounded(body, maxBytes = 256L))
    }

    @Test
    fun testRadarUrlValidatorAllowsLegitimateHttpsRadarHosts() {
        val validUrls = listOf(
            "https://www.jma.go.jp/bosai/jmatile/data/nowc/20261124000000/none/20261124000000/surf/hrpns/10/909/403.png",
            "https://www.jma.go.jp/bosai/jmatile/",
            "https://cyberjapandata.gsi.go.jp/xyz/pale/8/227/100.png",
            "https://disaportaldata.gsi.go.jp/raster/01_flood_l2_shinsuishin/10/909/403.png",
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/ridge::USCOMP-N0Q-0/6/15/24.png",
            "https://tilecache.rainviewer.com/v2/radar/1727318400/256/7/113/50/2/1_1.png",
            "https://cdn2.rainviewer.com/v2/radar/1727318400/256/7/113/50/2/1_1.png"
        )
        validUrls.forEach { url ->
            assertTrue("Expected allowed radar URL: $url", RadarUrlValidator.isAllowedRadarUrl(url))
        }
    }

    @Test
    fun testRadarUrlValidatorRejectsCleartextSpoofedPrivateAndTraversalUrls() {
        val disallowedUrls = listOf(
            "",
            "   ",
            "http://www.jma.go.jp/bosai/jmatile/data/nowc/20261124000000/none/20261124000000/surf/hrpns/10/909/403.png",
            "http://tilecache.rainviewer.com/v2/radar/1/256/1/1/1/2/1_1.png",
            "https://jma.go.jp.evil.example/tile.png",
            "https://evil-jma.go.jp/tile.png",
            "https://rainviewer.com.attacker.net/v2/radar/1.png",
            "https://evil.example/www.jma.go.jp/tile.png",
            "https://127.0.0.1/tile.png",
            "https://192.168.1.1/tile.png",
            "https://localhost/tile.png",
            "https://[::1]/tile.png",
            "https://www.jma.go.jp@evil.example/tile.png",
            "https://user:pass@www.jma.go.jp/tile.png",
            "https://www.jma.go.jp:8443/tile.png",
            "https://www.jma.go.jp/bosai/../../secret.png",
            "https://www.jma.go.jp/bosai/%2e%2e/secret.png",
            "https://www.jma.go.jp/bosai\\tile.png",
            "file:///etc/hosts",
            "javascript:alert(1)"
        )
        disallowedUrls.forEach { url ->
            assertFalse("Expected rejected radar URL: $url", RadarUrlValidator.isAllowedRadarUrl(url))
        }
    }

    @Test
    fun testRainViewerAndJmaTokenValidators() {
        assertTrue(RadarUrlValidator.isValidRainViewerHost("https://tilecache.rainviewer.com"))
        assertTrue(RadarUrlValidator.isValidRainViewerHost("https://tilecache.rainviewer.com/"))
        assertFalse(RadarUrlValidator.isValidRainViewerHost("http://tilecache.rainviewer.com"))
        assertFalse(RadarUrlValidator.isValidRainViewerHost("https://rainviewer.com.evil.example"))
        assertFalse(RadarUrlValidator.isValidRainViewerHost("https://tilecache.rainviewer.com/extra?q=1"))

        assertTrue(RadarUrlValidator.isValidRainViewerPath("/v2/radar/1727318400"))
        assertFalse(RadarUrlValidator.isValidRainViewerPath("https://evil.example/v2"))
        assertFalse(RadarUrlValidator.isValidRainViewerPath("/v2/../secret"))
        assertFalse(RadarUrlValidator.isValidRainViewerPath("/v2//double"))

        assertTrue(RadarUrlValidator.isValidJmaTimeToken("20261124030000"))
        assertFalse(RadarUrlValidator.isValidJmaTimeToken("../20261124030000"))
        assertFalse(RadarUrlValidator.isValidJmaTimeToken("20261124"))

        assertTrue(RadarUrlValidator.isValidJmaMemberToken("none"))
        assertTrue(RadarUrlValidator.isValidJmaMemberToken("immed"))
        assertFalse(RadarUrlValidator.isValidJmaMemberToken("../etc"))
        assertFalse(RadarUrlValidator.isValidJmaMemberToken("none?foo=bar"))
    }

    @Test
    fun testRadarTileHttpClientSecurityConfigurationAndRejection() = runBlocking {
        assertEquals(10_000, RadarTileHttpClient.client.callTimeoutMillis)
        assertFalse(RadarTileHttpClient.client.followSslRedirects)
        assertTrue(RadarTileHttpClient.client.networkInterceptors.isNotEmpty())

        // Disallowed URLs must be rejected immediately without network I/O
        assertNull(RadarTileHttpClient.getTileBytes("http://127.0.0.1/test.png"))
        assertNull(RadarTileHttpClient.getTileBytes("https://jma.go.jp.evil.example/test.png"))
        assertNull(RadarTileHttpClient.getTileBytesCancellable("https://192.168.0.1/test.png"))

        // Spoofed domain must NOT receive JMA referer
        assertEquals(
            "https://jma.go.jp.evil.example/",
            RadarTileHttpClient.getRefererForUrl("https://jma.go.jp.evil.example/tile.png")
        )
    }
}
