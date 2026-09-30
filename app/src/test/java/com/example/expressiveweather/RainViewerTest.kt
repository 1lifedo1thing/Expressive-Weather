package com.example.expressiveweather

import com.example.expressiveweather.data.RadarColorScheme
import com.example.expressiveweather.data.RainViewerRepository
import com.example.expressiveweather.data.RainViewerResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RainViewerTest {

    @Test
    fun testRainViewerResponseParsing() {
        val sampleJson = """
            {
              "version": "2.0",
              "generated": 1789907424,
              "host": "https://tilecache.rainviewer.com",
              "radar": {
                "past": [
                  { "time": 1789900200, "path": "/v2/radar/d596f04e284a" },
                  { "time": 1789900800, "path": "/v2/radar/286e015deecf" }
                ],
                "nowcast": []
              },
              "satellite": { "infrared": [] }
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val response = json.decodeFromString<RainViewerResponse>(sampleJson)

        assertEquals("2.0", response.version)
        assertEquals("https://tilecache.rainviewer.com", response.host)
        assertEquals(2, response.radar.past.size)
        assertEquals(1789900200L, response.radar.past[0].time)
        assertEquals("/v2/radar/d596f04e284a", response.radar.past[0].path)
    }

    @Test
    fun testRadarColorSchemeDefaults() {
        assertEquals(2, RadarColorScheme.UNIVERSAL_BLUE.id)
        assertEquals(RadarColorScheme.UNIVERSAL_BLUE, RadarColorScheme.DEFAULT)
        assertTrue(RadarColorScheme.entries.size >= 4)
    }

    @Test
    fun testRainViewerRepositoryInstance() {
        val repo1 = RainViewerRepository.getInstance()
        val repo2 = RainViewerRepository.getInstance()
        assertNotNull(repo1)
        assertEquals(repo1, repo2)
    }
}
