package io.github.nemynew.expressiveweather.data

import io.github.nemynew.expressiveweather.ui.components.BentoCardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoonPhaseCalculatorTest {

    private fun epochAtCycleFraction(fraction: Double): Long {
        val offsetMs = (MoonPhaseCalculator.SYNODIC_MONTH_DAYS * fraction * 86_400_000.0).toLong()
        return MoonPhaseCalculator.REFERENCE_NEW_MOON_EPOCH_MS + offsetMs
    }

    @Test
    fun testAstronomicalKeyPhasesAndIllumination() {
        val newMoon = MoonPhaseCalculator.calculate(AppLanguage.JAPANESE, epochAtCycleFraction(0.0))
        assertEquals(MoonPhaseStage.NEW_MOON, newMoon.stage)
        assertEquals("新月", newMoon.phaseName)
        assertEquals(0, newMoon.illumination)
        assertTrue(newMoon.isWaxing)

        val firstQuarter = MoonPhaseCalculator.calculate(AppLanguage.JAPANESE, epochAtCycleFraction(0.25))
        assertEquals(MoonPhaseStage.FIRST_QUARTER, firstQuarter.stage)
        assertEquals("上弦の月", firstQuarter.phaseName)
        assertEquals(50, firstQuarter.illumination)
        assertTrue(firstQuarter.isWaxing)

        val fullMoon = MoonPhaseCalculator.calculate(AppLanguage.JAPANESE, epochAtCycleFraction(0.50))
        assertEquals(MoonPhaseStage.FULL_MOON, fullMoon.stage)
        assertEquals("満月", fullMoon.phaseName)
        assertEquals(100, fullMoon.illumination)
        assertEquals(0, fullMoon.daysToFullMoon)
        assertFalse(fullMoon.isWaxing)

        val thirdQuarter = MoonPhaseCalculator.calculate(AppLanguage.JAPANESE, epochAtCycleFraction(0.75))
        assertEquals(MoonPhaseStage.THIRD_QUARTER, thirdQuarter.stage)
        assertEquals("下弦の月", thirdQuarter.phaseName)
        assertEquals(50, thirdQuarter.illumination)
        assertFalse(thirdQuarter.isWaxing)
    }

    @Test
    fun testAllEightPhasesAcrossAllEightLanguagesAreNonBlank() {
        for (stage in MoonPhaseStage.entries) {
            for (lang in AppLanguage.entries) {
                val label = MoonPhaseCalculator.getLocalizedPhaseName(stage, lang)
                assertTrue("Expected non-blank label for $stage / $lang", label.isNotBlank())
            }
        }
    }

    @Test
    fun testBentoMoonPhaseTileIncludedInDefaultOrderAndLocalized() {
        assertTrue(BentoCardType.MOON_PHASE in BentoCardType.DEFAULT_ORDER)
        assertEquals(10, BentoCardType.DEFAULT_ORDER.size)
        for (lang in AppLanguage.entries) {
            val title = BentoCardType.MOON_PHASE.getLocalizedTitle(lang.strings)
            assertTrue("Expected non-blank Bento title for $lang", title.isNotBlank())
        }
        assertEquals("月の満ち欠け", BentoCardType.MOON_PHASE.getLocalizedTitle(AppLanguage.JAPANESE.strings))
        assertEquals("Moon Phase", BentoCardType.MOON_PHASE.getLocalizedTitle(AppLanguage.ENGLISH.strings))
    }

    @Test
    fun testCalculateForFractionCoversAllEightStagesAndNormalization() {
        val expected = listOf(
            0.00f to MoonPhaseStage.NEW_MOON,
            0.125f to MoonPhaseStage.WAXING_CRESCENT,
            0.25f to MoonPhaseStage.FIRST_QUARTER,
            0.375f to MoonPhaseStage.WAXING_GIBBOUS,
            0.50f to MoonPhaseStage.FULL_MOON,
            0.625f to MoonPhaseStage.WANING_GIBBOUS,
            0.75f to MoonPhaseStage.THIRD_QUARTER,
            0.875f to MoonPhaseStage.WANING_CRESCENT
        )
        for ((fraction, stage) in expected) {
            val info = MoonPhaseCalculator.calculateForFraction(AppLanguage.JAPANESE, fraction)
            assertEquals("Stage mismatch for fraction=$fraction", stage, info.stage)
            val overrideInfo = MoonPhaseCalculator.calculate(
                language = AppLanguage.JAPANESE,
                nowMillis = epochAtCycleFraction(0.0),
                overridePhaseFraction = fraction
            )
            assertEquals(stage, overrideInfo.stage)
        }
        // Negative and >1 fractions wrap cleanly into [0, 1)
        assertEquals(MoonPhaseStage.FULL_MOON, MoonPhaseCalculator.calculateForFraction(AppLanguage.ENGLISH, 1.50f).stage)
        assertEquals(MoonPhaseStage.THIRD_QUARTER, MoonPhaseCalculator.calculateForFraction(AppLanguage.ENGLISH, -0.25f).stage)
    }

    @Test
    fun testMoonPhaseSimulatorStateLifecycle() {
        try {
            MoonPhaseSimulator.clear(null)
            assertNull(MoonPhaseSimulator.overridePhaseFraction.value)
            assertNull(MoonPhaseSimulator.overrideSouthernHemisphere.value)

            // デフォルト (override == null) は都市の実緯度に自動追従（東京=北半球、シドニー=南半球）
            assertFalse(MoonPhaseSimulator.resolveIsSouthernHemisphere(35.6762))
            assertTrue(MoonPhaseSimulator.resolveIsSouthernHemisphere(-33.8688))

            // 位相だけシミュレートしても南半球都市の緯度判定を上書きして壊さない
            MoonPhaseSimulator.setSimulatedPhase(null, 0.125f)
            assertEquals(0.125f, MoonPhaseSimulator.overridePhaseFraction.value!!, 0.0001f)
            assertTrue(MoonPhaseSimulator.resolveIsSouthernHemisphere(-33.8688))
            assertFalse(MoonPhaseSimulator.resolveIsSouthernHemisphere(35.6762))

            // 南半球トグルを ON にすると北半球都市でも南半球表示になる（位相スライダー未操作でも有効）
            MoonPhaseSimulator.setSimulatedPhase(null, null)
            MoonPhaseSimulator.setSimulatedSouthernHemisphere(null, true)
            assertEquals(true, MoonPhaseSimulator.overrideSouthernHemisphere.value)
            assertTrue(MoonPhaseSimulator.resolveIsSouthernHemisphere(35.6762))

            MoonPhaseSimulator.clear(null)
            assertNull(MoonPhaseSimulator.overridePhaseFraction.value)
            assertNull(MoonPhaseSimulator.overrideSouthernHemisphere.value)
        } finally {
            MoonPhaseSimulator.clear(null)
        }
    }

    @Test
    fun testTokyoMoonriseMoonsetAstronomicalAccuracy() {
        // 2024-09-17 (中秋の名月) 東京 (35.6762N, 139.6503E, UTC+9)
        // 国立天文台基準: 月の入り 04:39 JST前後 / 月の出 17:26 JST前後
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(2024, java.util.Calendar.SEPTEMBER, 17, 12, 0, 0) // 21:00 JST (月出後・地平線上)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val schedule = MoonPhaseCalculator.calculateMoonSchedule(
            latitude = 35.6762,
            longitude = 139.6503,
            utcOffsetSeconds = 9 * 3600,
            nowMillis = cal.timeInMillis
        )
        assertTrue("Expected moonrise on 2024-09-17 in Tokyo", schedule.moonrise != null)
        assertTrue("Expected moonset on 2024-09-17 in Tokyo", schedule.moonset != null)

        fun hmToMinutes(hm: String): Int {
            val parts = hm.split(":")
            return parts[0].toInt() * 60 + parts[1].toInt()
        }

        val riseMin = hmToMinutes(schedule.moonrise!!)
        val setMin = hmToMinutes(schedule.moonset!!)
        // 国立天文台(NAOJ) 東京 2024-09-17 基準: 月の出 17:24 JST / 月の入り 03:53 JST (±3分以内を検証)
        assertTrue("Moonrise ${schedule.moonrise} expected within ±3 min of NAOJ 17:24 JST", kotlin.math.abs(riseMin - (17 * 60 + 24)) <= 3)
        assertTrue("Moonset ${schedule.moonset} expected within ±3 min of NAOJ 03:53 JST", kotlin.math.abs(setMin - (3 * 60 + 53)) <= 3)
        assertTrue("At 21:00 JST on full moon night, moon should be above horizon", schedule.isAboveHorizon)
        assertTrue("Transit progress should be strictly in (0, 1)", schedule.transitProgress > 0.05f && schedule.transitProgress < 0.95f)
    }

    @Test
    fun testWeeklyMoonScheduleReturnsSevenDays() {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(2024, java.util.Calendar.SEPTEMBER, 17, 3, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val weekly = MoonPhaseCalculator.calculateWeeklyMoonSchedule(
            latitude = 35.6762,
            longitude = 139.6503,
            language = AppLanguage.JAPANESE,
            utcOffsetSeconds = 9 * 3600,
            nowMillis = cal.timeInMillis,
            days = 7
        )
        assertEquals(7, weekly.size)
        // 7日間のうち少なくとも6日は月の出・月の入りが存在する（朔望月中のスキップ日は最大1日）
        assertTrue(weekly.count { it.moonrise != null } >= 6)
        assertTrue(weekly.count { it.moonset != null } >= 6)
    }

    @Test
    fun testEntranceAnimationRangeHandlesNormalAndNewMoonPhases() {
        // 通常位相: 新月 (0.0) から現在位相へ満ちていく展開
        val (s1, e1) = MoonPhaseCalculator.resolveEntranceAnimationRange(0.25f)
        assertEquals(0.0f, s1, 0.0001f)
        assertEquals(0.25f, e1, 0.0001f)

        val (s2, e2) = MoonPhaseCalculator.resolveEntranceAnimationRange(0.50f)
        assertEquals(0.0f, s2, 0.0001f)
        assertEquals(0.50f, e2, 0.0001f)

        val (s3, e3) = MoonPhaseCalculator.resolveEntranceAnimationRange(0.85f)
        assertEquals(0.0f, s3, 0.0001f)
        assertEquals(0.85f, e3, 0.0001f)

        // 新月付近 (< 0.04 または > 0.96): 三日月付近 (0.18) から新月 (0.0) へ静かに収束
        val (ns1, ne1) = MoonPhaseCalculator.resolveEntranceAnimationRange(0.0f)
        assertEquals(0.18f, ns1, 0.0001f)
        assertEquals(0.0f, ne1, 0.0001f)

        val (ns2, ne2) = MoonPhaseCalculator.resolveEntranceAnimationRange(0.98f)
        assertEquals(0.18f, ns2, 0.0001f)
        assertEquals(0.0f, ne2, 0.0001f)

        // 負値・1超も正規化される
        val (ws, we) = MoonPhaseCalculator.resolveEntranceAnimationRange(1.25f)
        assertEquals(0.0f, ws, 0.0001f)
        assertEquals(0.25f, we, 0.0001f)
    }
}
