package io.github.nemynew.expressiveweather.ui.components

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import io.github.nemynew.expressiveweather.ui.util.DeviceTiltState
import io.github.nemynew.expressiveweather.ui.util.LocalDeviceTiltState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** 画面両端をスムーズに循環ラップさせる正規化位相関数 */
internal fun snowPhase(value: Float): Float = value - floor(value)

/** 端末の傾きと突風エネルギーから水平風速（画面幅/秒の基準係数）を算出 */
internal fun computeSnowHorizontalVelocity(
    tiltX: Float,
    baseBreeze: Float,
    gustDirection: Float,
    gustEnergy: Float
): Float {
    val safeTiltX = if (tiltX.isFinite()) tiltX.coerceIn(-1.5f, 1.5f) else 0f
    // Android重力センサーでは右傾き時に tiltX < 0 となるため、-tiltX が画面右(+X)の下り坂方向
    val gravityDrift = -safeTiltX * 0.28f
    val gustPush = gustDirection * gustEnergy * 0.22f
    return baseBreeze + gravityDrift + gustPush
}

/** 端末の前後傾きと突風エネルギーから落下速度係数を算出 */
internal fun computeSnowFallMultiplier(tiltY: Float, gustEnergy: Float): Float {
    val safeTiltY = if (tiltY.isFinite()) tiltY.coerceIn(-1.5f, 1.5f) else 0f
    val pitchFactor = (1f - safeTiltY * 0.26f).coerceIn(0.55f, 1.85f)
    // 急激に傾けた瞬間のスノードーム的なふわりとした舞い上がり減速
    val loftReduction = (1f - gustEnergy * 0.25f).coerceIn(0.65f, 1f)
    return pitchFactor * loftReduction
}

private data class SnowParticle(
    val x: Float,
    val phase: Float,
    val speed: Float,
    val radiusDp: Float,
    val wobbleSpeed: Float,
    val wobbleAmpDp: Float,
    val tumbleSpeed: Float,
    val tumbleOffset: Float,
    val spinSpeed: Float,
    val rotOffset: Float,
    val alpha: Float,
    val depthFactor: Float
)

private data class WinterCloudPuff(
    val phase: Float,
    val yFrac: Float,
    val radiusFrac: Float,
    val squash: Float,
    val alpha: Float,
    val isLit: Boolean
)

private data class SnowWispStreak(
    val x: Float,
    val yFrac: Float,
    val widthFrac: Float,
    val thicknessDp: Float,
    val speed: Float,
    val alpha: Float
)

private class SnowMotionClock {
    var seconds by mutableFloatStateOf(0f)
    var windTravel by mutableFloatStateOf(0f)
    var fallTravel by mutableFloatStateOf(0f)
    var gustEnergy by mutableFloatStateOf(0f)
    var gustDirection by mutableFloatStateOf(0f)
}

@Composable
private fun rememberSnowMotionClock(
    tiltState: DeviceTiltState?,
    enableTiltHaptics: Boolean = false
): SnowMotionClock {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val expressHaptics = LocalExpressiveHaptics.current
    val clock = remember { SnowMotionClock() }

    LaunchedEffect(lifecycle, tiltState, enableTiltHaptics, expressHaptics) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val motionScale = Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            )
            if (motionScale > 0f) {
                var lastFrame = 0L
                var prevTiltX = tiltState?.tiltX?.takeIf { it.isFinite() } ?: 0f
                var prevTiltY = tiltState?.tiltY?.takeIf { it.isFinite() } ?: 0f
                var smoothedVelX = 0f
                var lastHapticSec = -10f

                while (true) {
                    withFrameNanos { now ->
                        if (lastFrame != 0L) {
                            val dt = ((now - lastFrame) / 1_000_000_000f)
                                .coerceAtMost(0.05f) / motionScale
                            clock.seconds += dt

                            val rawTx = tiltState?.tiltX ?: 0f
                            val rawTy = tiltState?.tiltY ?: 0f
                            val tx = if (rawTx.isFinite()) rawTx.coerceIn(-1.5f, 1.5f) else 0f
                            val ty = if (rawTy.isFinite()) rawTy.coerceIn(-1.5f, 1.5f) else 0f

                            val dTx = tx - prevTiltX
                            val dTy = ty - prevTiltY
                            prevTiltX = tx
                            prevTiltY = ty

                            val tiltImpulse = abs(dTx) + abs(dTy) * 0.7f
                            if (tiltImpulse > 0.015f) {
                                clock.gustEnergy = (clock.gustEnergy + tiltImpulse * 2.8f).coerceAtMost(1.35f)
                                if (abs(dTx) > 0.008f) {
                                    clock.gustDirection = (-dTx).coerceIn(-1f, 1f)
                                }
                                if (enableTiltHaptics &&
                                    tiltImpulse > 0.14f &&
                                    (clock.seconds - lastHapticSec) >= 1.4f &&
                                    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                                ) {
                                    lastHapticSec = clock.seconds
                                    expressHaptics.play(ExpressiveHapticPattern.WEATHER_SNOW_CRISP)
                                }
                            } else {
                                clock.gustEnergy *= exp(-dt * 2.1f)
                                if (clock.gustEnergy < 0.005f) clock.gustEnergy = 0f
                            }

                            val ambientBreeze = sin(clock.seconds * 0.27f) * 0.032f +
                                sin(clock.seconds * 0.61f) * 0.014f
                            val targetVelX = computeSnowHorizontalVelocity(
                                tiltX = tx,
                                baseBreeze = ambientBreeze,
                                gustDirection = clock.gustDirection,
                                gustEnergy = clock.gustEnergy
                            )
                            smoothedVelX += (targetVelX - smoothedVelX) * (dt * 7.5f).coerceIn(0f, 1f)
                            clock.windTravel += smoothedVelX * dt

                            val fallMult = computeSnowFallMultiplier(ty, clock.gustEnergy)
                            clock.fallTravel += fallMult * dt
                        }
                        lastFrame = now
                    }
                }
            }
        }
    }
    return clock
}

/**
 * 冬空の多層ソフトクラウド＋速度積分＆傾き（重力・風・舞い上がり）対応の高品質降雪レイヤー。
 * - 硬い棒線結晶や同心円ドットを排し、ソフトボケ＋擬似3Dタンブリング雪片で空気感を表現。
 */
@Composable
internal fun SnowAtmosphere(
    isDark: Boolean,
    tiltState: DeviceTiltState? = null,
    modifier: Modifier = Modifier
) {
    val activeTilt = tiltState ?: LocalDeviceTiltState.current
    val clock = rememberSnowMotionClock(activeTilt, enableTiltHaptics = true)

    // 1. 遠景粉雪 (Distant Powder Mist)
    val bgPowder = remember {
        val rng = Random(3031)
        List(120) {
            SnowParticle(
                x = rng.nextFloat(),
                phase = rng.nextFloat(),
                speed = 0.075f + rng.nextFloat() * 0.045f,
                radiusDp = 0.9f + rng.nextFloat() * 1.1f,
                wobbleSpeed = 0.55f + rng.nextFloat() * 0.55f,
                wobbleAmpDp = 5f + rng.nextFloat() * 7f,
                tumbleSpeed = 0f,
                tumbleOffset = 0f,
                spinSpeed = 0f,
                rotOffset = 0f,
                alpha = 0.18f + rng.nextFloat() * 0.26f,
                depthFactor = 0.38f
            )
        }
    }

    // 2. 中景の舞い落ちる雪片 (Midground Tumbling Flakes)
    val mgFlakes = remember {
        val rng = Random(2029)
        List(66) {
            SnowParticle(
                x = rng.nextFloat(),
                phase = rng.nextFloat(),
                speed = 0.105f + rng.nextFloat() * 0.065f,
                radiusDp = 2.2f + rng.nextFloat() * 2.1f,
                wobbleSpeed = 0.95f + rng.nextFloat() * 0.85f,
                wobbleAmpDp = 11f + rng.nextFloat() * 15f,
                tumbleSpeed = 1.6f + rng.nextFloat() * 2.2f,
                tumbleOffset = rng.nextFloat() * 6.283f,
                spinSpeed = (if (rng.nextBoolean()) 1f else -1f) * (18f + rng.nextFloat() * 34f),
                rotOffset = rng.nextFloat() * 360f,
                alpha = 0.48f + rng.nextFloat() * 0.34f,
                depthFactor = 0.78f
            )
        }
    }

    // 3. 近景のソフトボケ雪片 (Foreground Soft Bokeh Flakes)
    val fgFlakes = remember {
        val rng = Random(1017)
        List(22) {
            SnowParticle(
                x = rng.nextFloat(),
                phase = rng.nextFloat(),
                speed = 0.145f + rng.nextFloat() * 0.075f,
                radiusDp = 4.8f + rng.nextFloat() * 4.4f,
                wobbleSpeed = 0.75f + rng.nextFloat() * 0.65f,
                wobbleAmpDp = 16f + rng.nextFloat() * 22f,
                tumbleSpeed = 1.1f + rng.nextFloat() * 1.4f,
                tumbleOffset = rng.nextFloat() * 6.283f,
                spinSpeed = (if (rng.nextBoolean()) 1f else -1f) * (10f + rng.nextFloat() * 20f),
                rotOffset = rng.nextFloat() * 360f,
                alpha = 0.36f + rng.nextFloat() * 0.34f,
                depthFactor = 1.25f
            )
        }
    }

    // 上空を流れる柔らかな冬雲層
    val cloudPuffs = remember {
        val rng = Random(8801)
        List(16) { idx ->
            WinterCloudPuff(
                phase = (idx + rng.nextFloat() * 0.7f) / 16f,
                yFrac = 0.02f + rng.nextFloat() * 0.34f,
                radiusFrac = 0.22f + rng.nextFloat() * 0.18f,
                squash = 0.28f + rng.nextFloat() * 0.18f,
                alpha = 0.18f + rng.nextFloat() * 0.18f,
                isLit = idx % 3 == 0
            )
        }
    }

    // 風の流れを感じる微細な吹雪ストリーク
    val wisps = remember {
        val rng = Random(5512)
        List(6) {
            SnowWispStreak(
                x = rng.nextFloat(),
                yFrac = 0.15f + rng.nextFloat() * 0.75f,
                widthFrac = 0.18f + rng.nextFloat() * 0.22f,
                thicknessDp = 1.2f + rng.nextFloat() * 1.4f,
                speed = 0.09f + rng.nextFloat() * 0.06f,
                alpha = 0.03f + rng.nextFloat() * 0.035f
            )
        }
    }

    Box(modifier.fillMaxSize().clipToBounds().drawWithCache {
        val w = size.width
        val h = size.height
        val safeW = w.coerceAtLeast(1f)
        val safeH = h.coerceAtLeast(1f)

        // 背景スカイの明暗を自然になじませる冬の大気ベール（下地のテーマグラデーションも活かす）
        val skyVeilBrush = Brush.verticalGradient(
            0.0f to (if (isDark) Color(0xFF0B1623) else Color(0xFF6A8AA4)).copy(alpha = if (isDark) 0.72f else 0.58f),
            0.48f to (if (isDark) Color(0xFF122336) else Color(0xFF8AA8C0)).copy(alpha = if (isDark) 0.52f else 0.40f),
            1.0f to (if (isDark) Color(0xFF0A1420) else Color(0xFFB7CCDC)).copy(alpha = if (isDark) 0.75f else 0.52f)
        )

        val upperColdGlowBrush = Brush.radialGradient(
            0.0f to (if (isDark) Color(0xFF426380) else Color(0xFFE6F0F8)).copy(alpha = if (isDark) 0.20f else 0.34f),
            0.65f to (if (isDark) Color(0xFF28435C) else Color(0xFFC4DAE9)).copy(alpha = if (isDark) 0.08f else 0.14f),
            1.0f to Color.Transparent,
            center = Offset(safeW * 0.5f, safeH * 0.16f),
            radius = safeW * 0.85f
        )

        val groundMistBrush = Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.55f to (if (isDark) Color(0xFF21384D) else Color(0xFFDDEAF4)).copy(alpha = if (isDark) 0.10f else 0.16f),
            1.0f to (if (isDark) Color(0xFF2B465F) else Color(0xFFEBF3F9)).copy(alpha = if (isDark) 0.24f else 0.32f),
            startY = safeH * 0.62f,
            endY = safeH
        )

        val cloudLitBrush = Brush.radialGradient(
            0.0f to (if (isDark) Color(0xFF53708A) else Color(0xFFF3F8FC)).copy(alpha = if (isDark) 0.26f else 0.42f),
            0.55f to (if (isDark) Color(0xFF3A546B) else Color(0xFFD5E3EE)).copy(alpha = if (isDark) 0.14f else 0.20f),
            1.0f to Color.Transparent,
            center = Offset.Zero,
            radius = 1f
        )
        val cloudBodyBrush = Brush.radialGradient(
            0.0f to (if (isDark) Color(0xFF293F54) else Color(0xFFAEC3D4)).copy(alpha = if (isDark) 0.30f else 0.34f),
            0.60f to (if (isDark) Color(0xFF1E3042) else Color(0xFF9BB4C8)).copy(alpha = if (isDark) 0.14f else 0.16f),
            1.0f to Color.Transparent,
            center = Offset.Zero,
            radius = 1f
        )

        // 滑らかなガウシアン風フォールオフを持つ雪片ブラシ群（すべて unit radius = 1f でスケーリング再利用）
        val powderBrush = Brush.radialGradient(
            0.0f to Color.White,
            0.55f to Color(0xFFF0F7FF).copy(alpha = 0.72f),
            1.0f to Color.Transparent,
            center = Offset.Zero,
            radius = 1f
        )
        val flakeBrush = Brush.radialGradient(
            0.0f to Color.White,
            0.42f to Color(0xFFF5FAFF).copy(alpha = 0.90f),
            0.75f to Color(0xFFDCEBF8).copy(alpha = 0.35f),
            1.0f to Color.Transparent,
            center = Offset.Zero,
            radius = 1f
        )
        val bokehBrush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = 0.92f),
            0.35f to Color(0xFFF0F7FF).copy(alpha = 0.65f),
            0.72f to Color(0xFFD7E8F7).copy(alpha = 0.22f),
            1.0f to Color.Transparent,
            center = Offset.Zero,
            radius = 1f
        )

        val cloudPath = Path().apply {
            val count = 48
            for (i in 0 until count) {
                val a = (i.toFloat() / count) * 2f * PI.toFloat()
                val r = 0.85f + 0.08f * sin(a * 3f + 0.5f) + 0.05f * cos(a * 5f + 1.2f)
                val px = cos(a) * r
                val py = sin(a) * r
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }

        onDrawBehind {
            if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
            val time = clock.seconds
            val windTravel = clock.windTravel
            val fallTravel = clock.fallTravel
            val gust = clock.gustEnergy
            val rawTx = activeTilt?.tiltX ?: 0f
            val rawTy = activeTilt?.tiltY ?: 0f
            val tx = if (rawTx.isFinite()) rawTx.coerceIn(-1.5f, 1.5f) else 0f
            val ty = if (rawTy.isFinite()) rawTy.coerceIn(-1.5f, 1.5f) else 0f

            // 1. 冬の大気ベール＆冷気グロー
            drawRect(skyVeilBrush)
            drawRect(upperColdGlowBrush)

            // 2. ゆったり漂う冬雲層（奥行きパララックス付き）
            val cloudSpan = w * 1.45f
            val cloudMargin = w * 0.225f
            cloudPuffs.forEach { puff ->
                val cx = snowPhase(puff.phase + time * 0.008f) * cloudSpan - cloudMargin + tx * 24.dp.toPx()
                val cy = h * puff.yFrac + sin(time * 0.14f + puff.phase * 6.28f) * 6.dp.toPx() + ty * 12.dp.toPx()
                val rx = w * puff.radiusFrac
                val ry = rx * puff.squash
                withTransform({
                    translate(cx, cy)
                    scale(rx, ry, Offset.Zero)
                }) {
                    drawPath(
                        path = cloudPath,
                        brush = if (puff.isLit) cloudLitBrush else cloudBodyBrush,
                        alpha = puff.alpha
                    )
                }
            }

            // 3. 遠景層: 細かく柔らかな粉雪
            drawSnowLayer(
                particles = bgPowder,
                brush = powderBrush,
                w = w,
                h = h,
                time = time,
                windTravel = windTravel,
                fallTravel = fallTravel,
                gust = gust,
                tiltX = tx,
                tiltY = ty,
                parallaxDp = 12f,
                enableTumble = false
            )

            // 4. 風が流れているときの繊細な雪煙ストリーク
            val windActivity = (abs(tx) * 0.85f + gust * 0.9f).coerceIn(0f, 1.4f)
            if (windActivity > 0.08f) {
                val slantRad = (-tx * 0.35f).coerceIn(-0.45f, 0.45f)
                val slantDeg = slantRad * (180f / PI.toFloat())
                wisps.forEach { wisp ->
                    val wx = snowPhase(wisp.x + windTravel * 1.3f + time * wisp.speed * (if (tx <= 0f) 1f else -1f)) * (w * 1.3f) - w * 0.15f
                    val wy = h * wisp.yFrac + sin(time * 0.5f + wisp.x * 8f) * 14.dp.toPx()
                    val len = w * wisp.widthFrac
                    val thick = wisp.thicknessDp.dp.toPx()
                    withTransform({
                        translate(wx, wy)
                        rotate(slantDeg, Offset.Zero)
                        scale(len * 0.5f, thick, Offset.Zero)
                    }) {
                        drawCircle(
                            brush = powderBrush,
                            radius = 1f,
                            center = Offset.Zero,
                            alpha = (wisp.alpha * windActivity).coerceAtMost(0.11f)
                        )
                    }
                }
            }

            // 5. 中景層: 3Dタンブリング（ひらひら回転）する柔らかな雪片
            drawSnowLayer(
                particles = mgFlakes,
                brush = flakeBrush,
                w = w,
                h = h,
                time = time,
                windTravel = windTravel,
                fallTravel = fallTravel,
                gust = gust,
                tiltX = tx,
                tiltY = ty,
                parallaxDp = 28f,
                enableTumble = true
            )

            // 6. 近景層: 柔らかな光学ボケを持つ雪片
            drawSnowLayer(
                particles = fgFlakes,
                brush = bokehBrush,
                w = w,
                h = h,
                time = time,
                windTravel = windTravel,
                fallTravel = fallTravel,
                gust = gust,
                tiltX = tx,
                tiltY = ty,
                parallaxDp = 46f,
                enableTumble = true
            )

            // 7. 地平線付近のやわらかな雪明かり・冷気ミスト
            drawRect(groundMistBrush)
        }
    })
}

/**
 * カード群の前面（最前面・タッチ完全透過）を極薄く横切る近景粉雪レイヤー。
 * ガラスカードの後ろと前を雪が通ることで、立体的なスノードーム奥行きを演出します。
 */
@Composable
internal fun SnowForegroundOverlay(
    isDark: Boolean,
    tiltState: DeviceTiltState? = null,
    modifier: Modifier = Modifier
) {
    val activeTilt = tiltState ?: LocalDeviceTiltState.current
    val clock = rememberSnowMotionClock(activeTilt, enableTiltHaptics = false)

    val nearFlakes = remember {
        val rng = Random(9941)
        List(15) { idx ->
            val isMicroMote = idx % 3 != 0
            SnowParticle(
                x = rng.nextFloat(),
                phase = rng.nextFloat(),
                speed = if (isMicroMote) 0.16f + rng.nextFloat() * 0.08f else 0.13f + rng.nextFloat() * 0.06f,
                radiusDp = if (isMicroMote) 1.5f + rng.nextFloat() * 1.4f else 4.2f + rng.nextFloat() * 3.4f,
                wobbleSpeed = 0.8f + rng.nextFloat() * 0.7f,
                wobbleAmpDp = 14f + rng.nextFloat() * 20f,
                tumbleSpeed = 1.3f + rng.nextFloat() * 1.6f,
                tumbleOffset = rng.nextFloat() * 6.283f,
                spinSpeed = (if (rng.nextBoolean()) 1f else -1f) * (14f + rng.nextFloat() * 22f),
                rotOffset = rng.nextFloat() * 360f,
                alpha = if (isMicroMote) 0.24f + rng.nextFloat() * 0.22f else 0.14f + rng.nextFloat() * 0.16f,
                depthFactor = if (isMicroMote) 1.35f else 1.55f
            )
        }
    }

    Box(modifier.fillMaxSize().clipToBounds().drawWithCache {
        val bokehBrush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = if (isDark) 0.85f else 0.92f),
            0.40f to Color(0xFFF2F8FF).copy(alpha = if (isDark) 0.50f else 0.62f),
            0.75f to Color(0xFFD8EBF9).copy(alpha = 0.16f),
            1.0f to Color.Transparent,
            center = Offset.Zero,
            radius = 1f
        )

        onDrawBehind {
            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f) return@onDrawBehind
            val rawTx = activeTilt?.tiltX ?: 0f
            val rawTy = activeTilt?.tiltY ?: 0f
            val tx = if (rawTx.isFinite()) rawTx.coerceIn(-1.5f, 1.5f) else 0f
            val ty = if (rawTy.isFinite()) rawTy.coerceIn(-1.5f, 1.5f) else 0f

            drawSnowLayer(
                particles = nearFlakes,
                brush = bokehBrush,
                w = w,
                h = h,
                time = clock.seconds,
                windTravel = clock.windTravel,
                fallTravel = clock.fallTravel,
                gust = clock.gustEnergy,
                tiltX = tx,
                tiltY = ty,
                parallaxDp = 56f,
                enableTumble = true
            )
        }
    })
}

private fun DrawScope.drawSnowLayer(
    particles: List<SnowParticle>,
    brush: Brush,
    w: Float,
    h: Float,
    time: Float,
    windTravel: Float,
    fallTravel: Float,
    gust: Float,
    tiltX: Float,
    tiltY: Float,
    parallaxDp: Float,
    enableTumble: Boolean
) {
    val marginY = 28.dp.toPx()
    val travelH = h + marginY * 2f
    val marginX = 36.dp.toPx()
    val travelW = w + marginX * 2f
    val parallaxX = -tiltX * parallaxDp.dp.toPx()
    val parallaxY = -tiltY * (parallaxDp * 0.4f).dp.toPx()
    val slantTiltDeg = (-tiltX * 18f).coerceIn(-28f, 28f)

    particles.forEach { p ->
        val y = snowPhase(p.phase + fallTravel * p.speed + parallaxY / travelH) * travelH - marginY
        val wobblePx = (sin(time * p.wobbleSpeed + p.x * 19f) +
            0.45f * sin(time * p.wobbleSpeed * 1.73f + p.phase * 11f)) * p.wobbleAmpDp.dp.toPx()
        val gustFlutter = if (gust > 0.01f) {
            sin(time * 5.2f + p.phase * 23f) * gust * 14.dp.toPx() * p.depthFactor
        } else 0f

        val x = snowPhase(
            p.x + windTravel * p.depthFactor + (wobblePx + gustFlutter + parallaxX) / travelW
        ) * travelW - marginX

        val radPx = p.radiusDp.dp.toPx()

        if (!enableTumble) {
            withTransform({
                translate(x, y)
                scale(radPx, radPx, Offset.Zero)
            }) {
                drawCircle(
                    brush = brush,
                    radius = 1f,
                    center = Offset.Zero,
                    alpha = p.alpha
                )
            }
        } else {
            val tumble = 0.46f + 0.54f * abs(cos(time * p.tumbleSpeed + p.tumbleOffset))
            val squashY = 0.86f + 0.14f * sin(time * p.tumbleSpeed * 0.8f + p.tumbleOffset)
            val rot = time * p.spinSpeed + p.rotOffset + slantTiltDeg

            withTransform({
                translate(x, y)
                rotate(rot, Offset.Zero)
                scale(radPx * tumble, radPx * squashY, Offset.Zero)
            }) {
                drawCircle(
                    brush = brush,
                    radius = 1f,
                    center = Offset.Zero,
                    alpha = p.alpha
                )
            }
        }
    }
}
