package com.example.expressiveweather.ui.components

import com.example.expressiveweather.data.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnowAtmosphereTest {
    @Test
    fun snowConditionAttributes() {
        assertTrue(WeatherCondition.SNOW.isPrecipitating)
        assertFalse(WeatherCondition.SNOW.hasRainAtmosphere)
        assertTrue(WeatherCondition.THUNDERSTORM.isPrecipitating)
        assertTrue(WeatherCondition.THUNDERSTORM.hasRainAtmosphere)
    }

    @Test
    fun snowPhaseWrapsCorrectly() {
        assertEquals(0.75f, snowPhase(-0.25f), 0.0001f)
        assertEquals(0.25f, snowPhase(1.25f), 0.0001f)
        assertEquals(0f, snowPhase(1f), 0.0001f)
        for (i in -1000..1000) {
            val phase = snowPhase(i / 19f)
            assertTrue(phase >= 0f && phase < 1f)
        }
    }
}
