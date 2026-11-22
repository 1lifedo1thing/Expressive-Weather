package io.github.nemynew.expressiveweather.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnowEffectsPhysicsTest {

    @Test
    fun snowPhase_wrapsPositiveAndNegativeIntoUnitInterval() {
        assertEquals(0.25f, snowPhase(1.25f), 0.0001f)
        assertEquals(0.75f, snowPhase(-0.25f), 0.0001f)
        assertEquals(0.0f, snowPhase(3.0f), 0.0001f)
    }

    @Test
    fun computeSnowHorizontalVelocity_respondsToTiltDirectionAndHandlesNonFinite() {
        // 右傾き (tiltX < 0) で画面右(+X)へ流れる
        val rightTiltVel = computeSnowHorizontalVelocity(
            tiltX = -0.8f,
            baseBreeze = 0f,
            gustDirection = 0f,
            gustEnergy = 0f
        )
        assertTrue(rightTiltVel > 0.2f)

        // 左傾き (tiltX > 0) で画面左(-X)へ流れる
        val leftTiltVel = computeSnowHorizontalVelocity(
            tiltX = 0.8f,
            baseBreeze = 0f,
            gustDirection = 0f,
            gustEnergy = 0f
        )
        assertTrue(leftTiltVel < -0.2f)

        // NaN 耐性
        val safeVel = computeSnowHorizontalVelocity(
            tiltX = Float.NaN,
            baseBreeze = 0.02f,
            gustDirection = 0f,
            gustEnergy = 0f
        )
        assertEquals(0.02f, safeVel, 0.0001f)
    }

    @Test
    fun computeSnowFallMultiplier_loftReducesFallDuringGustAndStaysBounded() {
        val calm = computeSnowFallMultiplier(tiltY = 0f, gustEnergy = 0f)
        val gusting = computeSnowFallMultiplier(tiltY = 0f, gustEnergy = 1.0f)
        assertEquals(1.0f, calm, 0.0001f)
        assertTrue(gusting < calm)
        assertTrue(computeSnowFallMultiplier(tiltY = -1.5f, gustEnergy = 0f) <= 1.85f)
        assertTrue(computeSnowFallMultiplier(tiltY = 1.5f, gustEnergy = 1.0f) >= 0.35f)
    }

    @Test
    fun computeOrganicSnowProfile_tapersToZeroAtEndsAndShiftsTowardTiltSide() {
        val (leftEdgeCrest, leftEdgeLip) = computeOrganicSnowProfile(frac = 0f, seed = 101, tiltX = 0f)
        val (rightEdgeCrest, rightEdgeLip) = computeOrganicSnowProfile(frac = 1f, seed = 101, tiltX = 0f)
        assertEquals(0f, leftEdgeCrest, 0.0001f)
        assertEquals(0f, leftEdgeLip, 0.0001f)
        assertEquals(0f, rightEdgeCrest, 0.0001f)
        assertEquals(0f, rightEdgeLip, 0.0001f)

        // 右傾き (tiltX < 0) 時は右側 (frac=0.8) の厚みが左側 (frac=0.2) より増す
        val (neutralLeft, _) = computeOrganicSnowProfile(frac = 0.2f, seed = 101, tiltX = 0f)
        val (neutralRight, _) = computeOrganicSnowProfile(frac = 0.8f, seed = 101, tiltX = 0f)
        val (tiltedRightSideLeft, _) = computeOrganicSnowProfile(frac = 0.2f, seed = 101, tiltX = -0.9f)
        val (tiltedRightSideRight, _) = computeOrganicSnowProfile(frac = 0.8f, seed = 101, tiltX = -0.9f)

        assertTrue(tiltedRightSideRight > neutralRight)
        assertTrue(tiltedRightSideLeft < neutralLeft)
    }

    @Test
    fun computeCircleApexAngleRad_slidesAlongArcWithTiltAndClamps() {
        assertEquals(0f, computeCircleApexAngleRad(0f), 0.0001f)
        assertTrue(computeCircleApexAngleRad(-0.8f) > 0.3f)
        assertTrue(computeCircleApexAngleRad(0.8f) < -0.3f)
        assertEquals(0.54f, computeCircleApexAngleRad(-2.0f), 0.0001f)
        assertEquals(-0.54f, computeCircleApexAngleRad(2.0f), 0.0001f)
    }

    @Test
    fun computeFacetGlintAlpha_peaksWhenTiltMatchesFacetNormal() {
        val aligned = computeFacetGlintAlpha(
            tiltX = 0.4f,
            tiltY = -0.2f,
            facetNormalX = 0.4f,
            facetNormalY = -0.2f,
            ambientPhase = 0f
        )
        val misaligned = computeFacetGlintAlpha(
            tiltX = -0.8f,
            tiltY = 0.6f,
            facetNormalX = 0.4f,
            facetNormalY = -0.2f,
            ambientPhase = 0f
        )
        assertTrue(aligned > misaligned)
        assertTrue(aligned in 0f..1f)
        assertTrue(misaligned in 0f..1f)
    }

    @Test
    fun computeCombinedHorizontalBias_combinesTiltAndTileInertia() {
        // タイルを左へ動かすと inertiaX > 0 (雪が右へ遅れて寄る) ＋ 右傾き(tiltX < 0)で加算
        val additive = computeCombinedHorizontalBias(tiltX = -0.5f, inertiaX = 0.4f)
        assertTrue(additive > 0.7f)

        // クランプ検証
        assertEquals(1.35f, computeCombinedHorizontalBias(tiltX = -2.0f, inertiaX = 1.0f), 0.0001f)
        assertEquals(-1.35f, computeCombinedHorizontalBias(tiltX = 2.0f, inertiaX = -1.0f), 0.0001f)
    }

    @Test
    fun stepSnowInertia_lagsOppositeToMovementAndDampsBackToZero() {
        // 右(+X)へタイル移動すると雪の慣性は左(-X)へ振れる
        val pushed = stepSnowInertia(currentInertia = 0f, deltaMoveDp = 12f, dt = 0.016f)
        assertTrue(pushed < -0.3f)

        // 停止後は 0 に向かって減衰する
        val decayed = stepSnowInertia(currentInertia = pushed, deltaMoveDp = 0f, dt = 0.05f)
        assertTrue(kotlin.math.abs(decayed) < kotlin.math.abs(pushed))
    }

    @Test
    fun stepSnowCover_shedsWhenMovedFastAndRegrowsWhenCalm() {
        val afterFastDrag = stepSnowCover(
            currentCover = 1.0f,
            motionSpeedDpPerSec = 500f,
            tiltImpulse = 0f,
            dt = 0.05f
        )
        assertTrue(afterFastDrag < 1.0f)

        val regrown = stepSnowCover(
            currentCover = afterFastDrag,
            motionSpeedDpPerSec = 0f,
            tiltImpulse = 0f,
            dt = 0.05f
        )
        assertTrue(regrown > afterFastDrag)

        // 最小残存量 0.16f 下限維持
        val minimumBound = stepSnowCover(
            currentCover = 0.16f,
            motionSpeedDpPerSec = 2000f,
            tiltImpulse = 1.0f,
            dt = 0.05f
        )
        assertEquals(0.16f, minimumBound, 0.0001f)
    }
}
