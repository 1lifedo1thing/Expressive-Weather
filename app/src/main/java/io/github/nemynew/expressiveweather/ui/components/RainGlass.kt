package io.github.nemynew.expressiveweather.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.WeatherCondition
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

internal fun glassRainStrength(condition: WeatherCondition): Float = when (condition) {
    WeatherCondition.HEAVY_RAIN -> 0.9f
    WeatherCondition.THUNDERSTORM -> 1.4f
    WeatherCondition.RAIN -> 0.35f
    else -> 0f
}

/** Continuous stick/slip motion; velocity remains positive during the slow phase. */
internal fun glassTravel(seconds: Float, speed: Float, seed: Float): Float {
    val w1 = 1.25f
    val w2 = 2.50f
    val s1 = sin(seconds * w1 + seed) - sin(seed)
    val s2 = sin(seconds * w2 + seed * 1.7f) - sin(seed * 1.7f)
    return speed * (seconds + 0.54f * s1 + 0.08f * s2)
}

@Composable
internal fun rainGlassModifier(
    condition: WeatherCondition,
    isDark: Boolean,
    tiltState: io.github.nemynew.expressiveweather.ui.util.DeviceTiltState? = null,
    seconds: () -> Float
): Modifier {
    val strength = glassRainStrength(condition)
    if (strength <= 0f) return Modifier
    return remember(strength, isDark, tiltState, seconds) { canvasRainGlass(strength, isDark, tiltState, seconds) }
}

/** Experimental; deliberately not selected by rainGlassModifier. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun refractingRainGlass(strength: Float, isDark: Boolean, seconds: () -> Float): Modifier {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    // Compile once, not per frame. Keep a fallback for shader compilation failures.
    val shader = remember(context) {
        runCatching {
            RuntimeShader(context.resources.openRawResource(R.raw.rain_window)
                .bufferedReader().use { it.readText() })
        }.onFailure { Log.w("RainGlass", "Using Canvas rain fallback", it) }.getOrNull()
    }
    if (shader == null) return remember(strength, isDark, seconds) {
        canvasRainGlass(strength, isDark, null, seconds)
    }
    val effect = remember(shader) {
        RenderEffect.createRuntimeShaderEffect(shader, "scene").asComposeRenderEffect()
    }
    return Modifier.graphicsLayer {
        shader.setFloatUniform("resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
        shader.setFloatUniform("density", density)
        shader.setFloatUniform("time", seconds())
        shader.setFloatUniform("strength", strength)
        shader.setFloatUniform("night", if (isDark) 1f else 0f)
        renderEffect = effect
        clip = true
    }
}

private data class WindowDrop(val x: Float, val y: Float, val radius: Float, val seed: Float)

/** API 24+: simulated glass using cached lens gradients and reusable paths, no bitmaps. */
internal fun canvasRainGlass(
    strength: Float,
    isDark: Boolean,
    tiltState: io.github.nemynew.expressiveweather.ui.util.DeviceTiltState? = null,
    seconds: () -> Float
): Modifier =
    Modifier.drawWithCache {
        val random = Random(4301)
        val unit = density
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)

        // 1. 静止水滴（ビーズ）— 大画面での GPU フィルレート負荷を抑えるため上限 72 に最適化
        val beadCount = ((w / unit) * (h / unit) / 2800f).toInt().coerceIn(28, 72)
        val beads = List(beadCount) {
            WindowDrop(
                random.nextFloat() * w,
                random.nextFloat() * h,
                (1.2f + random.nextFloat() * 2.2f) * unit,
                random.nextFloat() * 6.28f
            )
        }

        // 2. したたる滑落水滴（ランナー）
        val runnerCount = (2 + (strength * 3.5f).toInt()).coerceIn(2, 6)
        val laneWidth = w / runnerCount
        val runners = List(runnerCount) { index ->
            val laneX = (index + 0.30f + random.nextFloat() * 0.40f) * laneWidth
            WindowDrop(
                laneX,
                random.nextFloat(),
                (2.8f + random.nextFloat() * 1.6f) * unit,
                random.nextFloat() * 6.28f
            )
        }

        // ビーズ・滑落ヘッド用レンズを小型オフスクリーン ImageBitmap へ事前ベイク（毎フレームの RadialGradient 切替を撤廃）
        val spriteSize = 56
        fun bakeDropSprite(highlightAlpha: Float, causticAlpha: Float, baseBrush: Brush): ImageBitmap {
            val bmp = ImageBitmap(spriteSize, spriteSize)
            val offCanvas = Canvas(bmp)
            val offScope = CanvasDrawScope()
            val half = spriteSize / 2f
            offScope.draw(
                density = this,
                layoutDirection = layoutDirection,
                canvas = offCanvas,
                size = Size(spriteSize.toFloat(), spriteSize.toFloat())
            ) {
                drawCircle(baseBrush, radius = half, center = Offset(half, half))
                drawCircle(
                    Color.White.copy(alpha = highlightAlpha),
                    radius = half * 0.25f,
                    center = Offset(half - half * 0.28f, half - half * 0.32f)
                )
                drawCircle(
                    Color(0xFFE1F5FE).copy(alpha = causticAlpha),
                    radius = half * 0.30f,
                    center = Offset(half + half * 0.16f, half + half * 0.36f)
                )
            }
            return bmp
        }

        val beadBaseBrush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = if (isDark) 0.04f else 0.07f),
            0.60f to Color(0xFF8BB5D0).copy(alpha = if (isDark) 0.10f else 0.15f),
            0.85f to Color(0xFF142B3E).copy(alpha = if (isDark) 0.30f else 0.18f),
            1.0f to Color.Transparent,
            center = Offset(spriteSize / 2f, spriteSize / 2f),
            radius = spriteSize / 2f
        )
        val runnerHeadBrush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = 0.12f),
            0.55f to Color(0xFFA0C8E8).copy(alpha = 0.18f),
            0.85f to Color(0xFF0E2235).copy(alpha = 0.32f),
            1.0f to Color.Transparent,
            center = Offset(spriteSize / 2f, spriteSize / 2f),
            radius = spriteSize / 2f
        )
        val beadSprite = bakeDropSprite(highlightAlpha = 0.70f, causticAlpha = 0.35f, baseBrush = beadBaseBrush)
        val runnerSprite = bakeDropSprite(highlightAlpha = 0.85f, causticAlpha = 0.42f, baseBrush = runnerHeadBrush)

        val frost = Brush.verticalGradient(
            listOf(
                Color(0xFFD6E4EA).copy(alpha = if (isDark) 0.02f else 0.04f),
                Color(0xFFCBDDE6).copy(alpha = 0.012f),
                Color(0xFF849CAA).copy(alpha = if (isDark) 0.035f else 0.05f)
            )
        )

        val tailFadePath = Path()
        val tailBrightPath = Path()
        val trailSteps = 10
        val trailColorUpper = Color(0xFFB0D2E8)
        val trailColorBright = Color.White
        val travelTotal = h + 200f * unit
        val trailLengthPx = (75f + strength * 35f) * unit

        onDrawWithContent {
            drawContent()
            if (size.width <= 1f || size.height <= 1f) return@onDrawWithContent
            drawRect(frost)

            val time = seconds()
            val rawTiltX = tiltState?.tiltX ?: 0f
            val rawTiltY = tiltState?.tiltY ?: 0f
            val tiltX = if (rawTiltX.isFinite()) rawTiltX else 0f
            val tiltY = if (rawTiltY.isFinite()) rawTiltY else 0f

            // A. 静止水滴（事前ベイク済みスプライトの単一 drawImage で高速描画）
            val beadStretch = 1.05f + kotlin.math.abs(tiltX) * 0.08f
            val beadOffsetX = tiltX * 3f
            val beadOffsetY = tiltY * 2f
            beads.forEach { drop ->
                val r = drop.radius * (0.93f + 0.07f * sin(time * 0.15f + drop.seed))
                val rx = r.roundToInt().coerceAtLeast(1)
                val ry = (r * beadStretch).roundToInt().coerceAtLeast(1)
                drawImage(
                    image = beadSprite,
                    dstOffset = IntOffset((drop.x + beadOffsetX - rx).roundToInt(), (drop.y + beadOffsetY - ry).roundToInt()),
                    dstSize = IntSize(rx * 2, ry * 2)
                )
            }

            // B. したたる滑落水滴（ランナー）と軌跡（毎フレーム Brush アロケーションゼロ）
            val slideSpeed = (16f + strength * 12f) * (1f - tiltY * 0.18f).coerceIn(0.6f, 1.8f) * unit
            val stretchY = 1.28f + kotlin.math.abs(tiltY) * 0.12f

            runners.forEach { drop ->
                val currentTravel = glassTravel(time, slideSpeed, drop.seed)
                val rawY = rainPhase(drop.y + currentTravel / travelTotal) * travelTotal - 70f * unit

                fun meanderX(yPos: Float): Float {
                    val microWander = sin((yPos / unit) * 0.024f + drop.seed * 19f) * 1.4f * unit +
                        sin((yPos / unit) * 0.055f + drop.seed * 37f) * 0.6f * unit
                    val gyroSlant = (yPos / h) * (tiltX * 36f * unit)
                    return drop.x + microWander + gyroSlant
                }

                val headX = meanderX(rawY)
                val headY = rawY

                val distFromCenterX = kotlin.math.abs(headX - w * 0.5f) / (w * 0.5f)
                val distFromCenterY = kotlin.math.abs(headY - h * 0.26f) / (h * 0.18f)
                val inCenterText = distFromCenterX < 0.65f && distFromCenterY < 1.0f
                val alphaMult = if (inCenterText) 0.48f else 1.0f

                tailBrightPath.reset()
                tailFadePath.reset()
                tailBrightPath.moveTo(headX, headY)

                val midSplit = trailSteps / 2
                for (step in 1..trailSteps) {
                    val stepFrac = step.toFloat() / trailSteps
                    val trailY = headY - stepFrac * trailLengthPx
                    val trailX = meanderX(trailY)
                    if (step <= midSplit) {
                        tailBrightPath.lineTo(trailX, trailY)
                        if (step == midSplit) {
                            tailFadePath.moveTo(trailX, trailY)
                        }
                    } else {
                        tailFadePath.lineTo(trailX, trailY)
                    }
                    if (step == 3) {
                        drawCircle(
                            Color.White.copy(alpha = 0.34f * alphaMult),
                            drop.radius * 0.22f,
                            Offset(trailX, trailY)
                        )
                    }
                }

                drawPath(
                    path = tailFadePath,
                    color = trailColorUpper.copy(alpha = 0.07f * alphaMult),
                    style = Stroke(width = drop.radius * 0.42f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = tailBrightPath,
                    color = trailColorBright.copy(alpha = 0.18f * alphaMult),
                    style = Stroke(width = drop.radius * 0.46f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = tailBrightPath,
                    color = trailColorBright.copy(alpha = 0.34f * alphaMult),
                    style = Stroke(width = drop.radius * 0.15f, cap = StrokeCap.Round)
                )

                val rx = drop.radius.roundToInt().coerceAtLeast(1)
                val ry = (drop.radius * stretchY).roundToInt().coerceAtLeast(1)
                drawImage(
                    image = runnerSprite,
                    dstOffset = IntOffset((headX - rx).roundToInt(), (headY - ry).roundToInt()),
                    dstSize = IntSize(rx * 2, ry * 2),
                    alpha = alphaMult
                )
            }
        }
    }

