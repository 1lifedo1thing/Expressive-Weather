package io.github.nemynew.expressiveweather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JmaPointTileSamplerTest {

    @Test
    fun testLatLonToTilePixelZoom10() {
        // 東京駅 (35.6812, 139.7671) -> WebMercator z=10 (x=909, y=403)
        val tokyo = JmaPointTileSampler.latLonToTilePixel(35.6812, 139.7671, zoom = 10)
        assertEquals(10, tokyo.zoom)
        assertEquals(909, tokyo.tileX)
        assertEquals(403, tokyo.tileY)
        assertTrue(tokyo.pixelX in 0..255)
        assertTrue(tokyo.pixelY in 0..255)

        // さいたま市西区付近 (35.9006, 139.5934)
        val saitama = JmaPointTileSampler.latLonToTilePixel(35.9006, 139.5934, zoom = 10)
        assertEquals(909, saitama.tileX)
        assertTrue(saitama.tileY in 402..403)
        assertTrue(saitama.pixelX in 0..255)
        assertTrue(saitama.pixelY in 0..255)
    }

    @Test
    fun testDecodeHrpnsColorToPrecipMm() {
        assertEquals(0.0, JmaPointTileSampler.decodeHrpnsColorToPrecipMm(0x00000000), 0.01)
        assertEquals(0.0, JmaPointTileSampler.decodeHrpnsColorToPrecipMm(0xFFFFFFFF.toInt()), 0.01)
        assertEquals(2.5, JmaPointTileSampler.decodeHrpnsColorToPrecipMm(0xFFA0D2FF.toInt()), 0.01)
        assertEquals(7.5, JmaPointTileSampler.decodeHrpnsColorToPrecipMm(0xFF218CFF.toInt()), 0.01)
        assertEquals(40.0, JmaPointTileSampler.decodeHrpnsColorToPrecipMm(0xFFFF9900.toInt()), 0.01)
        assertEquals(90.0, JmaPointTileSampler.decodeHrpnsColorToPrecipMm(0xFFB40068.toInt()), 0.01)
    }

    @Test
    fun testDecodeKikikuruColorToLevel() {
        assertEquals(0, JmaPointTileSampler.decodeKikikuruColorToLevel(0x00000000))
        assertEquals(0, JmaPointTileSampler.decodeKikikuruColorToLevel(0xFFFFFFFF.toInt()))
        assertEquals(2, JmaPointTileSampler.decodeKikikuruColorToLevel(0xFFF2E700.toInt()))
        assertEquals(3, JmaPointTileSampler.decodeKikikuruColorToLevel(0xFFFF2800.toInt()))
        assertEquals(4, JmaPointTileSampler.decodeKikikuruColorToLevel(0xFFAA00AA.toInt()))
        assertEquals(5, JmaPointTileSampler.decodeKikikuruColorToLevel(0xFF0C000C.toInt()))
    }

    @Test
    fun testSampleWindowPrecipMmSuppressesSinglePixelNoise() {
        val pixels = IntArray(25) { 0x00000000 }
        // 角に1ピクセルだけのノイズを配置 (中心 (2,2) は無降水)
        pixels[0] = 0xFFA0D2FF.toInt()
        assertEquals(0.0, JmaPointTileSampler.sampleWindowPrecipMm(pixels, 5, 5, 2, 2, radiusPx = 1), 0.01)

        // (1,1) と (1,2) の2ピクセルに雨域がある場合は第2位値として拾う
        pixels[1 * 5 + 1] = 0xFF218CFF.toInt()
        pixels[1 * 5 + 2] = 0xFF218CFF.toInt()
        assertEquals(7.5, JmaPointTileSampler.sampleWindowPrecipMm(pixels, 5, 5, 2, 2, radiusPx = 1), 0.01)
    }

    @Test
    fun testSampleWindowRiskLevelFloodVsLandslide() {
        val pixels = IntArray(25) { 0x00000000 }
        // 1ピクセルだけの Lv3
        pixels[1 * 5 + 2] = 0xFFFF2800.toInt()
        assertEquals(0, JmaPointTileSampler.sampleWindowRiskLevel(pixels, 5, 5, 2, 2, radiusPx = 2, requireTwoPixels = true))
        assertEquals(3, JmaPointTileSampler.sampleWindowRiskLevel(pixels, 5, 5, 2, 2, radiusPx = 2, requireTwoPixels = false))

        // 2ピクセル目を追加すると requireTwoPixels = true でも Lv4 を検出
        pixels[1 * 5 + 2] = 0xFFAA00AA.toInt()
        pixels[1 * 5 + 3] = 0xFFAA00AA.toInt()
        assertEquals(4, JmaPointTileSampler.sampleWindowRiskLevel(pixels, 5, 5, 2, 2, radiusPx = 2, requireTwoPixels = true))
    }
}
