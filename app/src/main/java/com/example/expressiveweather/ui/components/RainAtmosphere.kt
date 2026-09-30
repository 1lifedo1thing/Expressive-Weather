package com.example.expressiveweather.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.PI
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.expressiveweather.data.WeatherCondition
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
private fun buildLightningPath(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    seed: Int
): Path {
    val path = Path()
    path.moveTo(startX, startY)

    val random = Random(seed)
    val segments = 22
    val deltaX = (endX - startX) / segments
    val deltaY = (endY - startY) / segments

    for (i in 1..segments) {
        val targetX = startX + deltaX * i
        val targetY = startY + deltaY * i
        val jitterX = (random.nextFloat() - 0.5f) * 36f
        val jitterY = (random.nextFloat() - 0.5f) * 12f
        val nextX = targetX + jitterX
        val nextY = targetY + jitterY

        path.lineTo(nextX, nextY)

        if (random.nextFloat() < 0.32f && i < segments - 3) {
            var branchX = nextX
            var branchY = nextY
            val branchSign = if (random.nextBoolean()) 1f else -1f
            val branchSteps = 5 + random.nextInt(4)

            val branchPath = Path()
            branchPath.moveTo(branchX, branchY)
            for (b in 1..branchSteps) {
                branchX += branchSign * (14f + random.nextFloat() * 20f)
                branchY += 12f + random.nextFloat() * 18f
                branchPath.lineTo(branchX, branchY)
            }
            path.addPath(branchPath)
        }
    }
    return path
}

/** Layered nimbostratus deck and crisp, three-depth rainfall — pure Canvas, no bitmaps. */
@Composable
internal fun RainAtmosphere(
    condition: WeatherCondition,
    isDark: Boolean,
    tiltState: com.example.expressiveweather.ui.util.DeviceTiltState? = null,
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
    val secondsState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Disabled system animations leave a composed, static rain scene.
            val motionScale = Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            )
            if (motionScale > 0f) {
                var lastFrame = 0L
                while (true) {
                    withFrameNanos { now ->
                        if (lastFrame != 0L) {
                            secondsState.floatValue += ((now - lastFrame) / 1_000_000_000f)
                                .coerceAtMost(0.05f) / motionScale
                        }
                        lastFrame = now
                    }
                }
            }
        }
    }

    val secondsProvider = remember { { secondsState.floatValue } }
    val glass = rainGlassModifier(condition, isDark, tiltState, secondsProvider)
    Box(modifier.fillMaxSize().clipToBounds().then(glass).drawWithCache {
        val w = size.width
        val h = size.height
        val safeW = w.coerceAtLeast(1f)
        val safeH = h.coerceAtLeast(1f)
        val storm = condition == WeatherCondition.THUNDERSTORM
        // Heavy slate sky; the deck above carries most of the tone, like the
        // rain-filled shelf in the reference frame.
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
        // Nimbostratus tones: lit tops, grey body, dark bellies.
        val deckH = (safeH * stratusCoverage(condition)).coerceAtLeast(1f)
        val lit = if (isDark) Color(0xFF46535F) else Color(0xFF77838D)
        val body = if (isDark) Color(0xFF303E49) else Color(0xFF566570)
        val belly = if (isDark) Color(0xFF192731) else Color(0xFF2B3C47)
        val tones = listOf(lit, body, belly)
        val sheet = Brush.verticalGradient(
            0f to lit, 0.55f to body, 1f to body,
            startY = 0f, endY = deckH
        )
        // Eight slightly offset contours feather the underside without a hard
        // rectangular cutoff. Geometry and paint are cached, never built per frame.
        val deckPaths = List(8) { feather ->
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
        val layers = stratusLayers(condition)
        val patchPaths = layers.map { layer -> layer.puffs.map { stratusPatch(it.phase) } }
        val wisps = stratusWisps(condition)
        val wispPaths = wisps.map { stratusPatch(it.phase) }
        val margin = w * 0.65f // exceeds both the widest patch and wisp
        val span = w + margin * 2f
        val vignette = Brush.horizontalGradient(listOf(
            Color(0xFF0A1B2C).copy(alpha = 0.2f), Color.Transparent,
            Color(0xFF0A1B2C).copy(alpha = 0.18f)
        ))
        val rainColors = List(3) { layer ->
            Color(0xFFD5E8F5).copy(alpha = (if (isDark) 0.12f else 0.16f) + layer * 0.07f)
        }
        val bolt1Path = if (storm) buildLightningPath(
            startX = w * 0.72f, startY = deckH * 0.25f,
            endX = w * 0.62f, endY = h * 0.82f,
            seed = 771
        ) else null
        val bolt3Path = if (storm) buildLightningPath(
            startX = w * 0.36f, startY = deckH * 0.20f,
            endX = w * 0.24f, endY = h * 0.76f,
            seed = 993
        ) else null

        onDrawBehind {
            if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
            val time = secondsState.floatValue // Draw-phase read: no per-frame recomposition of the weather UI.
            val rawTiltX = tiltState?.tiltX ?: 0f
            val rawTiltY = tiltState?.tiltY ?: 0f
            val tiltX = if (rawTiltX.isFinite()) rawTiltX else 0f
            val tiltY = if (rawTiltY.isFinite()) rawTiltY else 0f
            drawRect(sky)
            // Silver light trapped inside the upper sheet — no visible sun.
            withTransform({ translate(w * 0.82f + tiltX * 16f, deckH * 0.30f + tiltY * 8f) }) {
                scale(1.25f, 0.70f, Offset.Zero) {
                    drawCircle(mistBrush, w * 0.70f, Offset.Zero, alpha = 0.5f)
                }
            }
            withTransform({ translate(sin(time * 0.035f) * w * 0.04f + tiltX * 14f, tiltY * 8f) }) {
                deckPaths.asReversed().forEach { drawPath(it, sheet, alpha = 0.32f) }
            }
            layers.forEachIndexed { layerIndex, layer ->
                val depthFactorX = (layerIndex + 1) * 22f
                val depthFactorY = (layerIndex + 1) * 12f
                layer.puffs.forEachIndexed { puffIndex, puff ->
                    val x = rainPhase(puff.phase + time * layer.drift) * span - margin + tiltX * depthFactorX
                    val y = deckH * puff.yFrac +
                        sin(time * layer.bobSpeed + puff.phase * 6.2832f) * h * layer.bob + tiltY * depthFactorY
                    val r = puff.radiusFrac * w
                    withTransform({ translate(x, y) }) {
                        scale(r, r * puff.squash, Offset.Zero) {
                            repeat(8) { feather ->
                                val inset = 1f - feather * 0.065f
                                scale(inset, inset, Offset.Zero) {
                                    drawPath(patchPaths[layerIndex][puffIndex], tones[puff.tone],
                                        alpha = layer.alpha * 0.10f)
                                }
                            }
                        }
                    }
                }
            }
            // Wind-combed lit streaks give the sheet its grain.
            wisps.forEachIndexed { index, wisp ->
                val x = rainPhase(wisp.phase + time * 0.02f) * span - margin + tiltX * 16f
                withTransform({ translate(x, deckH * wisp.yFrac + tiltY * 8f) }) {
                    scale(wisp.widthFrac * w, deckH * 0.05f, Offset.Zero) {
                        repeat(4) { feather ->
                            val inset = 1f - feather * 0.12f
                            scale(inset, inset, Offset.Zero) {
                                drawPath(wispPaths[index], lit, alpha = wisp.alpha * 0.3f)
                            }
                        }
                    }
                }
            }
            // Low haze resting under the rain column.
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

                    // 雲内放電グロー（落雷発生元の発光）
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFEDE7F6).copy(alpha = intensity * 0.65f), Color.Transparent),
                            center = Offset(w * 0.72f, deckH * 0.28f),
                            radius = w * 0.55f
                        )
                    )

                    // 稲妻フォーク（3層プラズマレンダリング）
                    // 1. イオン化アウラ
                    drawPath(
                        path = bolt1Path,
                        color = Color(0xFF7C4DFF).copy(alpha = intensity * 0.42f),
                        style = Stroke(width = 20f, cap = StrokeCap.Round)
                    )
                    // 2. プラズマチャンネル
                    drawPath(
                        path = bolt1Path,
                        color = Color(0xFFB388FF).copy(alpha = intensity * 0.85f),
                        style = Stroke(width = 6.5f, cap = StrokeCap.Round)
                    )
                    // 3. 白熱コア
                    drawPath(
                        path = bolt1Path,
                        color = Color.White.copy(alpha = intensity * 0.98f),
                        style = Stroke(width = 2.4f, cap = StrokeCap.Round)
                    )
                }

                // Strike 2: 雲中シートライトニング / 幕電 (5.10s - 5.65s)
                if (epoch in 5.10f..5.65f) {
                    val lt = epoch - 5.10f
                    val sheetPulse = (sin(lt / 0.55f * PI.toFloat() * 2f).coerceAtLeast(0f) * 0.6f +
                        sin(lt / 0.55f * PI.toFloat()) * 0.4f).coerceIn(0f, 1f)
                    lightningFlashAlpha = sheetPulse * 0.55f

                    // 雲の奥深くから沸き立つ紫白の発光
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFD1C4E9).copy(alpha = sheetPulse * 0.48f), Color.Transparent),
                            center = Offset(w * 0.42f, deckH * 0.40f),
                            radius = w * 0.72f
                        )
                    )
                }

                // Strike 3: 左側ダイアゴナル落雷 (7.40s - 7.76f)
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

                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFEDE7F6).copy(alpha = intensity * 0.60f), Color.Transparent),
                            center = Offset(w * 0.36f, deckH * 0.22f),
                            radius = w * 0.50f
                        )
                    )

                    drawPath(
                        path = bolt3Path,
                        color = Color(0xFF7C4DFF).copy(alpha = intensity * 0.40f),
                        style = Stroke(width = 18f, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = bolt3Path,
                        color = Color(0xFF80D8FF).copy(alpha = intensity * 0.85f),
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = bolt3Path,
                        color = Color.White.copy(alpha = intensity * 0.98f),
                        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                    )
                }

                // 全画面アンビエント閃光フラッシュ
                if (lightningFlashAlpha > 0.02f) {
                    drawRect(Color(0xFFEDE7F6).copy(alpha = lightningFlashAlpha * 0.38f))
                }
            }

            val dynamicSlant = (style.slant - tiltX * 0.85f).coerceIn(-1.30f, 1.30f)
            val lengthMult = (1f - tiltY * 0.22f).coerceIn(0.6f, 1.4f)
            val speedMult = (1f - tiltY * 0.18f).coerceIn(0.6f, 1.5f)

            drops.forEach { drop ->
                val travel = h + 80.dp.toPx()
                val y = rainPhase(drop.phase + time * style.speed * speedMult * drop.speed *
                    (0.25f + drop.depth * 0.38f)) * travel - 40.dp.toPx()
                val x = rainPhase(drop.x + y * dynamicSlant / w) * w
                val length = (8f + drop.depth * 22f).dp.toPx() * lengthMult
                val end = Offset(x + length * dynamicSlant, y + length)
                val baseColor = rainColors[(drop.depth * 2).toInt()]
                // 雷光フラッシュ時に雨粒が劇的に光を反射して輝く
                val flashBoost = lightningFlashAlpha * 0.45f
                val color = baseColor.copy(alpha = (baseColor.alpha + flashBoost).coerceAtMost(0.95f))

                drawLine(color.copy(alpha = color.alpha * 0.3f), Offset(x, y), end,
                    (0.45f + drop.depth * 0.55f).dp.toPx(), StrokeCap.Round)
                drawLine(color, Offset(x + length * dynamicSlant * 0.55f, y + length * 0.55f),
                    end, (0.45f + drop.depth * 0.55f).dp.toPx(), StrokeCap.Round)
            }
            drawRect(vignette)
        }
    })
}
