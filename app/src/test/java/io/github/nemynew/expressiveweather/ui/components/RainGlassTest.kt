package io.github.nemynew.expressiveweather.ui.components

import io.github.nemynew.expressiveweather.data.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RainGlassTest {
    @Test
    fun strongerRainIncreasesGlassFlow() {
        assertTrue(glassRainStrength(WeatherCondition.RAIN) < glassRainStrength(WeatherCondition.HEAVY_RAIN))
        assertTrue(glassRainStrength(WeatherCondition.HEAVY_RAIN) < glassRainStrength(WeatherCondition.THUNDERSTORM))
    }

    @Test
    fun dropsStartWithoutDisplacement() {
        for (seed in 0..20) assertEquals(0f, glassTravel(0f, 30f, seed.toFloat()), 0.0001f)
    }

    @Test
    fun stickSlipNeverMovesUpwardsOrResetsAtSixSeconds() {
        for (seed in 0..10) {
            var previous = glassTravel(0f, 30f, seed.toFloat())
            for (frame in 1..3600) {
                val current = glassTravel(frame / 60f, 30f, seed.toFloat())
                assertTrue(current > previous)
                assertTrue(current - previous < 1f)
                previous = current
            }
        }
    }
}
