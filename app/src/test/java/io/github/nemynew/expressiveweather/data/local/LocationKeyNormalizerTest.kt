package io.github.nemynew.expressiveweather.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationKeyNormalizerTest {

    @Test
    fun testNormalizeStandardCoordinates() {
        // Tokyo Station: 35.6812, 139.7671 -> 35.68, 139.77
        val key = LocationKeyNormalizer.normalize(35.6812, 139.7671)
        assertEquals("35.68_139.77", key)
    }

    @Test
    fun testNormalizeCloseCoordinatesMatch() {
        // Very close coordinates (few hundred meters apart) should resolve to identical key
        val key1 = LocationKeyNormalizer.normalize(35.68123, 139.76714)
        val key2 = LocationKeyNormalizer.normalize(35.68149, 139.76698)
        assertEquals(key1, key2)
        assertEquals("35.68_139.77", key1)
    }

    @Test
    fun testNormalizeNegativeCoordinates() {
        // Sydney: -33.8688, 151.2093 -> -33.87, 151.21
        val key = LocationKeyNormalizer.normalize(-33.8688, 151.2093)
        assertEquals("-33.87_151.21", key)
    }

    @Test
    fun testNormalizeZeroAndBoundaries() {
        val key = LocationKeyNormalizer.normalize(0.004, -0.006)
        assertEquals("0.00_-0.01", key)
    }
}
