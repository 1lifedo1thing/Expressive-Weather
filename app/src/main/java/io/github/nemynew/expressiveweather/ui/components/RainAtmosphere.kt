package io.github.nemynew.expressiveweather.ui.components

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import kotlin.math.sin
import kotlin.random.Random

internal val WeatherCondition.hasRainAtmosphere: Boolean
    get() = this == WeatherCondition.RAIN || this == WeatherCondition.HEAVY_RAIN ||
        this == WeatherCondition.THUNDERSTORM

internal data class RainStyle(val dropCount: Int, val speed: Float, val slant: Float)

internal fun rainStyle(condition: WeatherCondition): RainStyle = when (condition) {
    WeatherCondition.HEAVY_RAIN -> RainStyle(185, 1.40f, 0.0f)
    WeatherCondition.THUNDERSTORM -> RainStyle(230, 1.70f, 0.0f)
    else -> RainStyle(120, 0.95f, 0.0f)
}

// Positive wrapping also works for wind-driven particles beyond the left edge.
internal fun rainPhase(value: Float): Float = value - kotlin.math.floor(value)

private data class RainParticle(
    val x: Float, val phase: Float, val depth: Float, val speed: Float
)

/** tone 0 = lit top sheet, 1 = body grey, 2 = dark belly / hanging chunk. */
internal data class StratusPuff(
    val phase: Float,      // horizontal position, wraps across both screen edges
    val yFrac: Float,      // vertical center as a fraction of the deck height
    val radiusFrac: Float, // radius as a fraction of the screen width
    val squash: Float,     // vertical squash: flat, sheet-like masses
    val tone: Int
)

internal data class StratusLayer(
    val puffs: List<StratusPuff>,
    val drift: Float,      // widths per second; slow parallax scroll
    val bob: Float,        // vertical sway amplitude as a fraction of height
    val bobSpeed: Float,
    val alpha: Float
)

internal data class Wisp(val phase: Float, val yFrac: Float, val widthFrac: Float, val alpha: Float)

/** Fraction of the screen height hidden by the cloud deck; heavier rain sits lower. */
internal fun stratusCoverage(condition: WeatherCondition): Float = when (condition) {
    WeatherCondition.THUNDERSTORM -> 0.72f
    WeatherCondition.HEAVY_RAIN -> 0.62f
    else -> 0.56f
}

/**
 * Layered nimbostratus deck: broad soft masses with darker rain-loaded bellies
 * and a ragged, hanging underside — never isolated circular gradients.
 * Deterministic from a fixed seed, so layout is stable across recomposition.
 */
internal fun stratusLayers(condition: WeatherCondition): List<StratusLayer> {
    val storm = condition == WeatherCondition.THUNDERSTORM
    val random = Random(20260917)
    fun puffs(
        count: Int, yMin: Float, yMax: Float, rMin: Float, rMax: Float,
        squashMin: Float, squashMax: Float, litChance: Float, bellyChance: Float
    ): List<StratusPuff> = List(count) { i ->
        val tone = when {
            random.nextFloat() < litChance -> 0
            random.nextFloat() < bellyChance -> 2
            else -> 1
        }
        StratusPuff(
            phase = (i + random.nextFloat()) / count,
            yFrac = yMin + random.nextFloat() * (yMax - yMin),
            radiusFrac = rMin + random.nextFloat() * (rMax - rMin),
            squash = squashMin + random.nextFloat() * (squashMax - squashMin),
            tone = tone
        )
    }
    val back = StratusLayer(
        puffs(if (storm) 10 else 9, -0.18f, 0.28f, 0.26f, 0.38f, 0.30f, 0.44f, 0.55f, 0.05f),
        drift = 0.006f, bob = 0.010f, bobSpeed = 0.045f, alpha = 0.55f
    )
    val mid = StratusLayer(
        puffs(if (storm) 13 else 12, 0.10f, 0.62f, 0.20f, 0.30f, 0.34f, 0.50f, 0.18f, 0.35f),
        drift = 0.010f, bob = 0.014f, bobSpeed = 0.060f, alpha = 0.80f
    )
    val stormShelf = if (storm) listOf(
        StratusLayer(
            puffs(8, 0.30f, 0.80f, 0.18f, 0.28f, 0.30f, 0.45f, 0.00f, 0.85f),
            drift = 0.012f, bob = 0.012f, bobSpeed = 0.070f, alpha = 0.85f
        )
    ) else emptyList()
    val front = StratusLayer(
        puffs(if (storm) 13 else 12, 0.50f, 0.95f, 0.17f, 0.26f, 0.36f, 0.55f, 0.05f, 0.70f) +
            puffs(if (storm) 6 else 4, 0.92f, 1.22f, 0.12f, 0.20f, 0.20f, 0.30f, 0.00f, 0.90f),
        drift = 0.016f, bob = 0.016f, bobSpeed = 0.075f, alpha = 0.92f
    )
    return listOf(back, mid) + stormShelf + listOf(front)
}

/** Thin lit streaks combed along the sheet — the visible grain of the deck. */
internal fun stratusWisps(condition: WeatherCondition): List<Wisp> {
    val random = Random(771)
    val count = if (condition == WeatherCondition.THUNDERSTORM) 8 else 6
    return List(count) {
        Wisp(
            phase = random.nextFloat(),
            yFrac = 0.06f + random.nextFloat() * 0.70f,
            widthFrac = 0.30f + random.nextFloat() * 0.30f,
            alpha = 0.05f + random.nextFloat() * 0.06f
        )
    }
}

/** Multi-scale, continuous underside; no repeated circular scallops. */
internal fun stratusEdge(x: Float): Float = 0.94f +
    0.035f * sin(x * 9.3f + 0.7f) + 0.022f * sin(x * 23.1f) +
    0.012f * sin(x * 57.7f + 1.4f) + 0.006f * sin(x * 113.3f)

/** Cached irregular contours, feathered by nested low-opacity fills when drawn. */
private fun stratusPatch(phase: Float): Path = Path().apply {
    repeat(65) { i ->
        val angle = i / 64f * (2f * PI.toFloat())
        val radius = 0.80f + 0.09f * sin(angle * 3f + phase * 17f) +
            0.06f * cos(angle * 7f + phase * 31f) + 0.035f * sin(angle * 13f)
        val x = cos(angle) * radius
        val y = sin(angle) * radius
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** リアルなジグザグと複数の分岐（サブブランチ）を持つ稲妻パスを生成 */
internal fun buildLightningPath(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    seed: Int,
    jitterScalePx: Float = 36f
): Path {
    val path = Path()
    val branchPaths = mutableListOf<Path>()
    path.moveTo(startX, startY)

    val random = Random(seed)
    val segments = 22
    val deltaX = (endX - startX) / segments
    val deltaY = (endY - startY) / segments
    val isHorizontal = kotlin.math.abs(endX - startX) > kotlin.math.abs(endY - startY) * 1.4f

    for (i in 1..segments) {
        val targetX = startX + deltaX * i
        val targetY = startY + deltaY * i
        val perpJitter = (random.nextFloat() - 0.5f) * jitterScalePx
        val axisJitter = (random.nextFloat() - 0.5f) * (jitterScalePx * 0.34f)
        val nextX = targetX + if (isHorizontal) axisJitter else perpJitter
        val nextY = targetY + if (isHorizontal) perpJitter else axisJitter

        path.lineTo(nextX, nextY)

        if (random.nextFloat() < 0.34f && i in 2..(segments - 3)) {
            var branchX = nextX
            var branchY = nextY
            val branchSign = if (random.nextBoolean()) 1f else -1f
            val branchSteps = 5 + random.nextInt(4)
            val branchStepLen = jitterScalePx * 0.52f

            val branchPath = Path()
            branchPath.moveTo(branchX, branchY)
            for (b in 1..branchSteps) {
                branchX += if (isHorizontal) {
                    (random.nextFloat() * 0.6f + 0.4f) * branchStepLen * kotlin.math.sign(deltaX).let { if (it == 0f) 1f else it }
                } else {
                    branchSign * (0.4f + random.nextFloat() * 0.65f) * branchStepLen
                }
                branchY += if (isHorizontal) {
                    (0.35f + random.nextFloat() * 0.65f) * branchStepLen
                } else {
                    (0.35f + random.nextFloat() * 0.55f) * branchStepLen
                }
                branchPath.lineTo(branchX, branchY)
            }
            branchPaths.add(branchPath)
        }
    }
    // 主幹を描き終えてからブランチを結合し、前枝先端から次の主幹頂点への誤接続を防止
    branchPaths.forEach { path.addPath(it) }
    return path
}

/** Layered nimbostratus deck and crisp, three-depth rainfall — pure Canvas, no bitmaps. */
@Composable
internal fun RainAtmosphere(
    condition: WeatherCondition,
    isDark: Boolean,
    tiltState: io.github.nemynew.expressiveweather.ui.util.DeviceTiltState? = null,
    modifier: Modifier = Modifier
) {
    val style = remember(condition) { rainStyle(condition) }
    val drops = remember(style) {
        val random = Random(917)
        List(style.dropCount) { index ->
            RainParticle(random.nextFloat(), random.nextFloat(),
                (index % 3) / 2f, 0.8f + random.nextFloat() * 0.4f)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val expressHaptics = LocalExpressiveHaptics.current
    val secondsState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(lifecycle, condition, expressHaptics) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Disabled system animations leave a composed, static rain scene.
            val motionScale = Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            )
            if (motionScale > 0f) {
                var lastFrame = 0L
                var lastCycleIndex = -1
                var strike1Fired = false
                var strike3Fired = false
                while (true) {
                    withFrameNanos { now ->
                        if (lastFrame != 0L) {
                            secondsState.floatValue += ((now - lastFrame) / 1_000_000_000f)
                                .coerceAtMost(0.05f) / motionScale

                            if (condition == WeatherCondition.THUNDERSTORM &&
                                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                            ) {
                                val t = secondsState.floatValue
                                val cycleIndex = (t / 9.2f).toInt()
                                val epoch = t % 9.2f
                                if (cycleIndex != lastCycleIndex) {
                                    lastCycleIndex = cycleIndex
                                    strike1Fired = false
                                    strike3Fired = false
                                }
                                if (epoch >= 2.10f && !strike1Fired) {
                                    strike1Fired = true
                                    expressHaptics.play(ExpressiveHapticPattern.WEATHER_THUNDER_RUMBLE)
                                }
                                if (epoch >= 7.40f && !strike3Fired) {
                                    strike3Fired = true
                                    expressHaptics.play(ExpressiveHapticPattern.WEATHER_THUNDER_RUMBLE)
                                }
                            }
                        }
                        lastFrame = now
                    }
                }
            }
        }
    }

    val secondsProvider = remember { { secondsState.floatValue } }
    val glass = rainGlassModifier(condition, isDark, tiltState, secondsProvider)
    key(condition, isDark) {
        Box(modifier.fillMaxSize().clipToBounds().then(glass).drawWithCache {
            val w = size.width
            val h = size.height
            val safeW = w.coerceAtLeast(1f)
            val safeH = h.coerceAtLeast(1f)
            val isWideTablet = safeW > safeH * 1.15f
            val storm = condition == WeatherCondition.THUNDERSTORM

            val top = if (isDark) Color(0xFF0C1A2A) else Color(0xFF3D5468)
            val middle = if (isDark) Color(0xFF18293B) else Color(0xFF4E687D)
            val bottom = if (isDark) Color(0xFF081221) else Color(0xFF1F3A50)
            val sky = Brush.verticalGradient(listOf(top, middle, bottom))
            val mistColor = if (isDark) Color(0xFF6E8CA6) else Color(0xFFB4C6D4)
            val mistBrush = Brush.radialGradient(
                0f to mistColor.copy(alpha = 0.22f),
                0.5f to mistColor.copy(alpha = 0.11f),
                1f to mistColor.copy(alpha = 0f),
                center = Offset.Zero, radius = (safeW * 0.85f).coerceAtLeast(1f)
            )
            val deckH = (safeH * stratusCoverage(condition)).coerceAtLeast(1f)
            val lit = if (isDark) Color(0xFF46535F) else Color(0xFF77838D)
            val body = if (isDark) Color(0xFF303E49) else Color(0xFF566570)
            val belly = if (isDark) Color(0xFF192731) else Color(0xFF2B3C47)
            val tones = listOf(lit, body, belly)
            val sheet = Brush.verticalGradient(
                0f to lit, 0.55f to body, 1f to body,
                startY = 0f, endY = deckH
            )

            // デッキ下端の8層フェザリングを低解像度オフスクリーン Bitmap へ1回だけベイク（タブレット上の過描画を大幅削減）
            val deckCacheScale = (720f / (safeW * 1.4f)).coerceAtMost(0.45f).coerceAtLeast(0.18f)
            val deckBmpW = (safeW * 1.4f * deckCacheScale).roundToInt().coerceAtLeast(32)
            val deckBmpH = (deckH * 1.25f * deckCacheScale).roundToInt().coerceAtLeast(32)
            val bakedDeckBitmap = ImageBitmap(deckBmpW, deckBmpH)
            run {
                val offCanvas = Canvas(bakedDeckBitmap)
                val offScope = CanvasDrawScope()
                val rawDeckPaths = List(8) { feather ->
                    Path().apply {
                        moveTo(-w * 0.2f, -h * 0.1f)
                        lineTo(w * 1.2f, -h * 0.1f)
                        for (i in 168 downTo 0) {
                            val x = -0.2f + i / 120f
                            lineTo(x * w, deckH * (stratusEdge(x) + feather * 0.0025f))
                        }
                        close()
                    }
                }
                offScope.draw(
                    density = this,
                    layoutDirection = layoutDirection,
                    canvas = offCanvas,
                    size = Size(deckBmpW.toFloat(), deckBmpH.toFloat())
                ) {
                    withTransform({
                        scale(deckCacheScale, deckCacheScale, Offset.Zero)
                        translate(w * 0.2f, h * 0.1f)
                    }) {
                        rawDeckPaths.asReversed().forEach { drawPath(it, sheet, alpha = 0.32f) }
                    }
                }
            }
            val deckDstW = (safeW * 1.4f).roundToInt().coerceAtLeast(1)
            val deckDstH = (deckH * 1.25f).roundToInt().coerceAtLeast(1)

            // 層雲パフとウィスプも 8層/4層 フェザリングをスプライトへ事前ベイクし、毎フレーム 300〜400 回の Path 再計算・過描画を解消
            val layers = stratusLayers(condition)
            val puffVariants = 4
            val puffSpriteSize = 96
            val puffSpritesByTone = List(tones.size) { toneIdx ->
                val toneColor = tones[toneIdx]
                List(puffVariants) { variantIdx ->
                    val bmp = ImageBitmap(puffSpriteSize, puffSpriteSize)
                    val path = stratusPatch(variantIdx / puffVariants.toFloat())
                    val offCanvas = Canvas(bmp)
                    val offScope = CanvasDrawScope()
                    val half = puffSpriteSize / 2f
                    offScope.draw(
                        density = this,
                        layoutDirection = layoutDirection,
                        canvas = offCanvas,
                        size = Size(puffSpriteSize.toFloat(), puffSpriteSize.toFloat())
                    ) {
                        withTransform({
                            translate(half, half)
                            scale(half * 0.96f, half * 0.96f, Offset.Zero)
                        }) {
                            repeat(8) { feather ->
                                val inset = 1f - feather * 0.065f
                                scale(inset, inset, Offset.Zero) {
                                    drawPath(path, toneColor, alpha = 0.10f)
                                }
                            }
                        }
                    }
                    bmp
                }
            }

            val wisps = stratusWisps(condition)
            val wispBmpW = 128
            val wispBmpH = 32
            val bakedWispSprite = ImageBitmap(wispBmpW, wispBmpH).also { bmp ->
                val path = stratusPatch(0.37f)
                val offCanvas = Canvas(bmp)
                val offScope = CanvasDrawScope()
                offScope.draw(
                    density = this,
                    layoutDirection = layoutDirection,
                    canvas = offCanvas,
                    size = Size(wispBmpW.toFloat(), wispBmpH.toFloat())
                ) {
                    withTransform({
                        translate(wispBmpW / 2f, wispBmpH / 2f)
                        scale(wispBmpW * 0.48f, wispBmpH * 0.48f, Offset.Zero)
                    }) {
                        repeat(4) { feather ->
                            val inset = 1f - feather * 0.12f
                            scale(inset, inset, Offset.Zero) {
                                drawPath(path, lit, alpha = 0.12f)
                            }
                        }
                    }
                }
            }

            val margin = w * 0.65f
            val span = w + margin * 2f
            val vignette = Brush.horizontalGradient(listOf(
                Color(0xFF0A1B2C).copy(alpha = 0.2f), Color.Transparent,
                Color(0xFF0A1B2C).copy(alpha = 0.18f)
            ))
            val rainColors = List(3) { layer ->
                Color(0xFFD5E8F5).copy(alpha = (if (isDark) 0.12f else 0.16f) + layer * 0.07f)
            }

            // タブレット横長時は中央のカード群で隠れない左右サイド空域＋上部雲間に落雷・幕電を配置
            val boltJitterPx = (safeW.coerceAtMost(safeH) * 0.042f).coerceAtLeast(16.dp.toPx())
            val bolt1Center = if (isWideTablet) Offset(w * 0.89f, deckH * 0.16f) else Offset(w * 0.72f, deckH * 0.25f)
            val bolt1Path = if (storm) buildLightningPath(
                startX = bolt1Center.x,
                startY = bolt1Center.y,
                endX = if (isWideTablet) w * 0.82f else w * 0.62f,
                endY = if (isWideTablet) h * 0.86f else h * 0.82f,
                seed = 771,
                jitterScalePx = boltJitterPx
            ) else null
            val bolt2AnvilPath = if (storm) buildLightningPath(
                startX = w * 0.18f,
                startY = deckH * 0.24f,
                endX = w * 0.84f,
                endY = deckH * 0.30f,
                seed = 853,
                jitterScalePx = boltJitterPx * 0.9f
            ) else null
            val bolt3Center = if (isWideTablet) Offset(w * 0.13f, deckH * 0.18f) else Offset(w * 0.36f, deckH * 0.20f)
            val bolt3Path = if (storm) buildLightningPath(
                startX = bolt3Center.x,
                startY = bolt3Center.y,
                endX = if (isWideTablet) w * 0.20f else w * 0.24f,
                endY = if (isWideTablet) h * 0.82f else h * 0.76f,
                seed = 993,
                jitterScalePx = boltJitterPx
            ) else null

            val flash1Brush = Brush.radialGradient(
                listOf(Color(0xFFEDE7F6).copy(alpha = 0.65f), Color.Transparent),
                center = bolt1Center,
                radius = (safeW * 0.55f).coerceAtLeast(1f)
            )
            val flash2Center = if (isWideTablet) Offset(w * 0.50f, deckH * 0.22f) else Offset(w * 0.42f, deckH * 0.40f)
            val flash2Brush = Brush.radialGradient(
                listOf(Color(0xFFD1C4E9).copy(alpha = 0.52f), Color.Transparent),
                center = flash2Center,
                radius = (safeW * 0.72f).coerceAtLeast(1f)
            )
            val flash3Brush = Brush.radialGradient(
                listOf(Color(0xFFEDE7F6).copy(alpha = 0.60f), Color.Transparent),
                center = bolt3Center,
                radius = (safeW * 0.50f).coerceAtLeast(1f)
            )

            val dropTravelPx = h + 80.dp.toPx()
            val dropTopOffsetPx = 40.dp.toPx()
            val dropLenBasePx = 8.dp.toPx()
            val dropLenDepthPx = 22.dp.toPx()
            val dropStrokeBasePx = 0.45.dp.toPx()
            val dropStrokeDepthPx = 0.55.dp.toPx()
            val auraStroke1 = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            val plasmaStroke1 = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            val coreStroke1 = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            val auraStroke3 = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val plasmaStroke3 = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
            val coreStroke3 = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)

            onDrawBehind {
                if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
                val time = secondsState.floatValue
                val rawTiltX = tiltState?.tiltX ?: 0f
                val rawTiltY = tiltState?.tiltY ?: 0f
                val tiltX = if (rawTiltX.isFinite()) rawTiltX else 0f
                val tiltY = if (rawTiltY.isFinite()) rawTiltY else 0f
                drawRect(sky)
                withTransform({ translate(w * 0.82f + tiltX * 16f, deckH * 0.30f + tiltY * 8f) }) {
                    scale(1.25f, 0.70f, Offset.Zero) {
                        drawCircle(mistBrush, w * 0.70f, Offset.Zero, alpha = 0.5f)
                    }
                }
                drawImage(
                    image = bakedDeckBitmap,
                    dstOffset = IntOffset(
                        (-w * 0.2f + sin(time * 0.035f) * w * 0.04f + tiltX * 14f).roundToInt(),
                        (-h * 0.1f + tiltY * 8f).roundToInt()
                    ),
                    dstSize = IntSize(deckDstW, deckDstH)
                )
                layers.forEachIndexed { layerIndex, layer ->
                    val depthFactorX = (layerIndex + 1) * 22f
                    val depthFactorY = (layerIndex + 1) * 12f
                    val spritesForTone = puffSpritesByTone
                    layer.puffs.forEachIndexed { puffIndex, puff ->
                        val x = rainPhase(puff.phase + time * layer.drift) * span - margin + tiltX * depthFactorX
                        val y = deckH * puff.yFrac +
                            sin(time * layer.bobSpeed + puff.phase * 6.2832f) * h * layer.bob + tiltY * depthFactorY
                        val rx = (puff.radiusFrac * w).roundToInt().coerceAtLeast(1)
                        val ry = (puff.radiusFrac * w * puff.squash).roundToInt().coerceAtLeast(1)
                        drawImage(
                            image = spritesForTone[puff.tone][(layerIndex + puffIndex) % puffVariants],
                            dstOffset = IntOffset((x - rx).roundToInt(), (y - ry).roundToInt()),
                            dstSize = IntSize(rx * 2, ry * 2),
                            alpha = layer.alpha
                        )
                    }
                }
                wisps.forEach { wisp ->
                    val x = rainPhase(wisp.phase + time * 0.02f) * span - margin + tiltX * 16f
                    val y = deckH * wisp.yFrac + tiltY * 8f
                    val rx = (wisp.widthFrac * w).roundToInt().coerceAtLeast(1)
                    val ry = (deckH * 0.05f).roundToInt().coerceAtLeast(1)
                    drawImage(
                        image = bakedWispSprite,
                        dstOffset = IntOffset((x - rx).roundToInt(), (y - ry).roundToInt()),
                        dstSize = IntSize(rx * 2, ry * 2),
                        alpha = (wisp.alpha * 2.5f).coerceAtMost(1f)
                    )
                }
                withTransform({ translate(w * 0.5f + tiltX * 28f, h * 0.88f + tiltY * 16f) }) {
                    scale(1.7f, 0.30f, Offset.Zero) {
                        drawCircle(mistBrush, w * 0.75f, Offset.Zero, alpha = 0.4f)
                    }
                }
                var lightningFlashAlpha = 0f
                if (storm) {
                    drawRect(Color(0xFF15132D).copy(alpha = 0.18f))

                    val epoch = time % 9.2f

                    // Strike 1: 右側クラウド・トゥ・グラウンド落雷 (2.10s - 2.48s)
                    if (epoch in 2.10f..2.48f && bolt1Path != null) {
                        val lt = epoch - 2.10f
                        val intensity = when {
                            lt < 0.05f -> lt / 0.05f * 0.45f
                            lt < 0.15f -> 0.95f
                            lt < 0.20f -> 0.30f
                            lt < 0.28f -> 0.75f
                            else -> ((0.38f - lt) / 0.10f).coerceAtLeast(0f) * 0.45f
                        }
                        lightningFlashAlpha = intensity

                        drawCircle(brush = flash1Brush, alpha = intensity)
                        drawPath(bolt1Path, Color(0xFF7C4DFF).copy(alpha = intensity * 0.44f), style = auraStroke1)
                        drawPath(bolt1Path, Color(0xFFB388FF).copy(alpha = intensity * 0.88f), style = plasmaStroke1)
                        drawPath(bolt1Path, Color.White.copy(alpha = intensity * 0.98f), style = coreStroke1)
                    }

                    // Strike 2: 雲中シートライトニング / アンビルクローラー雲間放電 (5.10s - 5.65s)
                    if (epoch in 5.10f..5.65f) {
                        val lt = epoch - 5.10f
                        val sheetPulse = (sin(lt / 0.55f * PI.toFloat() * 2f).coerceAtLeast(0f) * 0.6f +
                            sin(lt / 0.55f * PI.toFloat()) * 0.4f).coerceIn(0f, 1f)
                        lightningFlashAlpha = sheetPulse * 0.58f

                        drawCircle(brush = flash2Brush, alpha = sheetPulse)
                        if (bolt2AnvilPath != null && sheetPulse > 0.25f) {
                            drawPath(bolt2AnvilPath, Color(0xFF9575CD).copy(alpha = sheetPulse * 0.45f), style = auraStroke3)
                            drawPath(bolt2AnvilPath, Color(0xFFEDE7F6).copy(alpha = sheetPulse * 0.82f), style = coreStroke3)
                        }
                    }

                    // Strike 3: 左側ダイアゴナル落雷 (7.40s - 7.76s)
                    if (epoch in 7.40f..7.76f && bolt3Path != null) {
                        val lt = epoch - 7.40f
                        val intensity = when {
                            lt < 0.04f -> lt / 0.04f * 0.40f
                            lt < 0.13f -> 0.92f
                            lt < 0.18f -> 0.25f
                            lt < 0.26f -> 0.68f
                            else -> ((0.36f - lt) / 0.10f).coerceAtLeast(0f) * 0.40f
                        }
                        lightningFlashAlpha = intensity

                        drawCircle(brush = flash3Brush, alpha = intensity)
                        drawPath(bolt3Path, Color(0xFF7C4DFF).copy(alpha = intensity * 0.42f), style = auraStroke3)
                        drawPath(bolt3Path, Color(0xFF80D8FF).copy(alpha = intensity * 0.86f), style = plasmaStroke3)
                        drawPath(bolt3Path, Color.White.copy(alpha = intensity * 0.98f), style = coreStroke3)
                    }

                    if (lightningFlashAlpha > 0.02f) {
                        drawRect(Color(0xFFEDE7F6).copy(alpha = lightningFlashAlpha * 0.36f))
                    }
                }

                val dynamicSlant = (style.slant - tiltX * 0.85f).coerceIn(-1.30f, 1.30f)
                val lengthMult = (1f - tiltY * 0.22f).coerceIn(0.6f, 1.4f)
                val speedMult = (1f - tiltY * 0.18f).coerceIn(0.6f, 1.5f)
                val speedFactor = time * style.speed * speedMult
                val flashBoost = lightningFlashAlpha * 0.45f

                drops.forEach { drop ->
                    val y = rainPhase(drop.phase + speedFactor * drop.speed *
                        (0.25f + drop.depth * 0.38f)) * dropTravelPx - dropTopOffsetPx
                    val x = rainPhase(drop.x + y * dynamicSlant / w) * w
                    val length = (dropLenBasePx + drop.depth * dropLenDepthPx) * lengthMult
                    val endX = x + length * dynamicSlant
                    val endY = y + length
                    val baseColor = rainColors[(drop.depth * 2).toInt()]
                    val finalAlpha = (baseColor.alpha + flashBoost).coerceAtMost(0.95f)
                    val strokeW = dropStrokeBasePx + drop.depth * dropStrokeDepthPx

                    drawLine(
                        baseColor.copy(alpha = finalAlpha * 0.3f),
                        Offset(x, y),
                        Offset(endX, endY),
                        strokeW,
                        StrokeCap.Round
                    )
                    drawLine(
                        baseColor.copy(alpha = finalAlpha),
                        Offset(x + length * dynamicSlant * 0.55f, y + length * 0.55f),
                        Offset(endX, endY),
                        strokeW,
                        StrokeCap.Round
                    )
                }
                drawRect(vignette)
            }
        })
    }
}
