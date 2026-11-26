package io.github.nemynew.expressiveweather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrynessIndexCalculatorTest {

    @Test
    fun simulation_earlySummerSunny_matchesExpected() {
        // 初夏の晴れ: 22℃, 60%, 2 m/s -> VH 11.7, base 9.7, wind +0.8, rh 0.0 -> 11・しっとり
        val result = DrynessIndexCalculator.calculate(tempC = 22.0, relativeHumidity = 60.0, windSpeedMs = 2.0)
        assertEquals(11.7, result.volumetricHumidityGm3, 0.1)
        assertEquals(9.7, result.baseScore, 0.1)
        assertEquals(0.8, result.windCorrection, 0.05)
        assertEquals(0.0, result.rhCorrection, 0.01)
        assertEquals(11, result.score)
        assertEquals(DrynessRank.MOIST, result.rank)
    }

    @Test
    fun simulation_winterColdRain_matchesExpected() {
        // 冬の冷たい雨: 5℃, 90%, 2 m/s -> VH 6.1, base 50.0, wind +0.2, rh -25.0 -> 25・やや乾燥
        val result = DrynessIndexCalculator.calculate(tempC = 5.0, relativeHumidity = 90.0, windSpeedMs = 2.0)
        assertEquals(6.1, result.volumetricHumidityGm3, 0.1)
        assertEquals(50.0, result.baseScore, 0.1)
        assertEquals(0.2, result.windCorrection, 0.05)
        assertEquals(-25.0, result.rhCorrection, 0.01)
        assertEquals(25, result.score)
        assertEquals(DrynessRank.SLIGHTLY_DRY, result.rank)
    }

    @Test
    fun simulation_autumnSunny_matchesExpected() {
        // 秋の晴れ: 15℃, 45%, 3 m/s -> VH 5.8, base 52.5, wind +2.2, rh 0.0 -> 55・乾燥
        val result = DrynessIndexCalculator.calculate(tempC = 15.0, relativeHumidity = 45.0, windSpeedMs = 3.0)
        assertEquals(5.8, result.volumetricHumidityGm3, 0.1)
        assertEquals(52.5, result.baseScore, 0.1)
        assertEquals(2.2, result.windCorrection, 0.05)
        assertEquals(0.0, result.rhCorrection, 0.01)
        assertEquals(55, result.score)
        assertEquals(DrynessRank.DRY, result.rank)
    }

    @Test
    fun simulation_winterSunny_matchesExpected() {
        // 冬の普通の晴れ: 10℃, 40%, 2 m/s -> VH 3.8, base 67.2, wind +1.2, rh 0.0 -> 68・カラカラ
        val result = DrynessIndexCalculator.calculate(tempC = 10.0, relativeHumidity = 40.0, windSpeedMs = 2.0)
        assertEquals(3.8, result.volumetricHumidityGm3, 0.1)
        assertEquals(67.2, result.baseScore, 0.1)
        assertEquals(1.2, result.windCorrection, 0.05)
        assertEquals(0.0, result.rhCorrection, 0.01)
        assertEquals(68, result.score)
        assertEquals(DrynessRank.VERY_DRY, result.rank)
    }

    @Test
    fun simulation_midwinterDryWind_matchesExpected() {
        // 真冬の空っ風: 6℃, 25%, 8 m/s -> VH 1.8, base 80.0, wind +10.5, rh +7.5 -> 98・超カラカラ
        val result = DrynessIndexCalculator.calculate(tempC = 6.0, relativeHumidity = 25.0, windSpeedMs = 8.0)
        assertEquals(1.8, result.volumetricHumidityGm3, 0.1)
        assertEquals(80.0, result.baseScore, 0.01)
        assertEquals(10.5, result.windCorrection, 0.05)
        assertEquals(7.5, result.rhCorrection, 0.01)
        assertEquals(98, result.score)
        assertEquals(DrynessRank.EXTREME_DRY, result.rank)
    }

    @Test
    fun boundaries_clampingAndWindCaps_behaveCorrectly() {
        // 高温多湿・強風でも 0 にクランプ、基礎点 0、風速補正は 100% 湿度で 0
        val humid = DrynessIndexCalculator.calculate(tempC = 32.0, relativeHumidity = 100.0, windSpeedMs = 15.0)
        assertEquals(0.0, humid.baseScore, 0.001)
        assertEquals(0.0, humid.windCorrection, 0.001)
        assertEquals(-35.0, humid.rhCorrection, 0.001)
        assertEquals(0, humid.score)
        assertEquals(DrynessRank.MOIST, humid.rank)

        // 風速 1.0 m/s 以下は風速補正 0
        val calm = DrynessIndexCalculator.calculate(tempC = 10.0, relativeHumidity = 40.0, windSpeedMs = 1.0)
        assertEquals(0.0, calm.windCorrection, 0.001)

        // 風速 10.0 m/s 超は 9.0 m/s 分で頭打ち
        val wind10 = DrynessIndexCalculator.calculate(tempC = 0.0, relativeHumidity = 20.0, windSpeedMs = 10.0)
        val wind25 = DrynessIndexCalculator.calculate(tempC = 0.0, relativeHumidity = 20.0, windSpeedMs = 25.0)
        assertEquals(wind10.windCorrection, wind25.windCorrection, 0.001)
        assertEquals(100, wind25.score)
        assertEquals(DrynessRank.EXTREME_DRY, wind25.rank)

        // RH 40% と 65% の境界は RH 補正 0
        assertEquals(0.0, DrynessIndexCalculator.calculate(15.0, 40.0, 1.0).rhCorrection, 0.001)
        assertEquals(0.0, DrynessIndexCalculator.calculate(15.0, 65.0, 1.0).rhCorrection, 0.001)
    }

    @Test
    fun rankThresholds_every20Points_matchSpec() {
        assertEquals(DrynessRank.MOIST, DrynessRank.fromScore(0))
        assertEquals(DrynessRank.MOIST, DrynessRank.fromScore(19))
        assertEquals(DrynessRank.SLIGHTLY_DRY, DrynessRank.fromScore(20))
        assertEquals(DrynessRank.SLIGHTLY_DRY, DrynessRank.fromScore(39))
        assertEquals(DrynessRank.DRY, DrynessRank.fromScore(40))
        assertEquals(DrynessRank.DRY, DrynessRank.fromScore(59))
        assertEquals(DrynessRank.VERY_DRY, DrynessRank.fromScore(60))
        assertEquals(DrynessRank.VERY_DRY, DrynessRank.fromScore(79))
        assertEquals(DrynessRank.EXTREME_DRY, DrynessRank.fromScore(80))
        assertEquals(DrynessRank.EXTREME_DRY, DrynessRank.fromScore(100))
    }

    @Test
    fun kmhConversionAndInvalidInputs_areSafe() {
        val fromMs = DrynessIndexCalculator.calculate(tempC = 6.0, relativeHumidity = 25.0, windSpeedMs = 8.0)
        val fromKmH = DrynessIndexCalculator.calculateFromKmH(tempC = 6.0, relativeHumidity = 25.0, windSpeedKmH = 28.8)
        assertEquals(fromMs.score, fromKmH.score)
        assertEquals(fromMs.windCorrection, fromKmH.windCorrection, 0.001)

        val nanSafe = DrynessIndexCalculator.calculate(Double.NaN, Double.POSITIVE_INFINITY, -5.0)
        assertTrue(nanSafe.score in 0..100)
        assertTrue(nanSafe.volumetricHumidityGm3.isFinite())
    }
}
