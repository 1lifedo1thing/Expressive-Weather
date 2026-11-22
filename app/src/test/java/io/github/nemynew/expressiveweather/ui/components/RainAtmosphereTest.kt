package io.github.nemynew.expressiveweather.ui.components

import io.github.nemynew.expressiveweather.data.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RainAtmosphereTest {
    @Test
    fun onlyRainConditionsUseRainRenderer() {
        val rainConditions = setOf(WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN,
            WeatherCondition.THUNDERSTORM)
        WeatherCondition.entries.forEach { condition ->
            assertEquals(condition.name, condition in rainConditions, condition.hasRainAtmosphere)
        }
        assertFalse(WeatherCondition.SNOW.hasRainAtmosphere)
    }

    @Test
    fun strongerRainIncreasesDensitySpeedAndWind() {
        val rain = rainStyle(WeatherCondition.RAIN)
        val heavy = rainStyle(WeatherCondition.HEAVY_RAIN)
        val storm = rainStyle(WeatherCondition.THUNDERSTORM)
        assertTrue(rain.dropCount < heavy.dropCount && heavy.dropCount < storm.dropCount)
        assertTrue(rain.speed < heavy.speed && heavy.speed < storm.speed)
        assertEquals(0f, rain.slant, 0.0001f)
        assertEquals(0f, heavy.slant, 0.0001f)
        assertEquals(0f, storm.slant, 0.0001f)
        assertTrue(storm.dropCount <= 240)
    }

    @Test
    fun particlesWrapAcrossBothScreenEdges() {
        assertEquals(0.75f, rainPhase(-0.25f), 0.0001f)
        assertEquals(0.25f, rainPhase(1.25f), 0.0001f)
        assertEquals(0f, rainPhase(1f), 0.0001f)
        for (i in -10000..10000) {
            val phase = rainPhase(i / 37f)
            assertTrue(phase >= 0f && phase < 1f)
        }
    }

    @Test
    fun particleMotionDoesNotResetAtTheOldSixSecondBoundary() {
        val speed = rainStyle(WeatherCondition.RAIN).speed
        val before = rainPhase(0.2f + 5.999f * speed * 0.25f)
        val after = rainPhase(0.2f + 6.001f * speed * 0.25f)
        assertTrue(after > before)
        assertTrue(after - before < 0.001f)
    }

    @Test
    fun stratusLayoutIsDeterministic() {
        assertEquals(stratusLayers(WeatherCondition.RAIN), stratusLayers(WeatherCondition.RAIN))
        assertEquals(stratusWisps(WeatherCondition.RAIN), stratusWisps(WeatherCondition.RAIN))
        assertEquals(stratusLayers(WeatherCondition.THUNDERSTORM),
            stratusLayers(WeatherCondition.THUNDERSTORM))
    }

    @Test
    fun stratusPuffsStayInsideLayoutBounds() {
        WeatherCondition.entries.filter { it.hasRainAtmosphere }.forEach { condition ->
            stratusLayers(condition).forEach { layer ->
                assertTrue(layer.drift > 0f && layer.alpha in 0.4f..1f)
                layer.puffs.forEach { puff ->
                    assertTrue(puff.phase in 0f..1f)
                    assertTrue(puff.yFrac > -0.5f && puff.yFrac < 1.3f)
                    assertTrue(puff.radiusFrac in 0.10f..0.40f)
                    assertTrue(puff.squash in 0.15f..0.6f)
                    assertTrue(puff.tone in 0..2)
                }
            }
            stratusWisps(condition).forEach { wisp ->
                assertTrue(wisp.phase in 0f..1f && wisp.widthFrac in 0.2f..0.7f)
                assertTrue(wisp.alpha > 0f && wisp.alpha < 0.2f)
            }
        }
    }

    @Test
    fun cloudDeckStacksFromLitTopToDarkRaggedUnderside() {
        val layers = stratusLayers(WeatherCondition.RAIN)
        assertEquals(3, layers.size)
        val avgTone = layers.map { layer -> layer.puffs.map { it.tone }.average() }
        assertTrue("tones ${avgTone}", avgTone[0] < avgTone[1] && avgTone[1] < avgTone[2])
        val avgY = layers.map { layer -> layer.puffs.map { it.yFrac }.average() }
        assertTrue(avgY[0] < avgY[2])
        assertTrue(layers[2].puffs.any { it.yFrac > 1f }) // chunks hang past the deck line
    }

    @Test
    fun puffsWrapAroundBothEdgesWithoutPopping() {
        stratusLayers(WeatherCondition.THUNDERSTORM).forEach { layer ->
            for (t in intArrayOf(0, 30, 300, 3600)) {
                layer.puffs.forEach { puff ->
                    val wrapped = rainPhase(puff.phase + t * layer.drift)
                    assertTrue(wrapped >= 0f && wrapped < 1f)
                }
            }
        }
    }

    @Test
    fun heavierRainLowersTheDeckAndAddsDensity() {
        val rain = stratusCoverage(WeatherCondition.RAIN)
        val heavy = stratusCoverage(WeatherCondition.HEAVY_RAIN)
        val storm = stratusCoverage(WeatherCondition.THUNDERSTORM)
        assertTrue(rain < heavy && heavy < storm)
        val rainPuffs = stratusLayers(WeatherCondition.RAIN).sumOf { it.puffs.size }
        val stormPuffs = stratusLayers(WeatherCondition.THUNDERSTORM).sumOf { it.puffs.size }
        assertTrue(stormPuffs > rainPuffs)
        assertTrue(stratusWisps(WeatherCondition.THUNDERSTORM).size >
            stratusWisps(WeatherCondition.RAIN).size)
    }
}
