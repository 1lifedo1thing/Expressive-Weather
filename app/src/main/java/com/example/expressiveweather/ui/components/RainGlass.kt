package com.example.expressiveweather.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.example.expressiveweather.R
import com.example.expressiveweather.data.WeatherCondition
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
    tiltState: com.example.expressiveweather.ui.util.DeviceTiltState? = null,
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
    tiltState: com.example.expressiveweather.ui.util.DeviceTiltState? = null,
    seconds: () -> Float
): Modifier =
    Modifier.drawWithCache {
        val random = Random(4301)
        val unit = density
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)

        // 1. 静止水滴（ビーズ）
        val beadCount = ((w / unit) * (h / unit) / 2400f).toInt().coerceIn(30, 140)
        val beads = List(beadCount) {
            WindowDrop(
                random.nextFloat() * w,
                random.nextFloat() * h,
                (1.2f + random.nextFloat() * 2.4f) * unit,
                random.nextFloat() * 6.28f
            )
        }

        // 2. したたる滑落水滴（ランナー）- 画面幅に均等配置し、上品な本数に制御
        val runnerCount = (2 + (strength * 3.5f).toInt()).coerceIn(2, 6)
        val laneWidth = w / runnerCount
        val runners = List(runnerCount) { index ->
            val laneX = (index + 0.30f + random.nextFloat() * 0.40f) * laneWidth
            WindowDrop(
                laneX,
                random.nextFloat(),
                (2.8f + random.nextFloat() * 1.8f) * unit,
                random.nextFloat() * 6.28f
            )
        }

        // レンズ外周メニスカス（屈折暗環）
        val beadBaseBrush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = if (isDark) 0.04f else 0.07f),
            0.60f to Color(0xFF8BB5D0).copy(alpha = if (isDark) 0.10f else 0.15f),
            0.85f to Color(0xFF142B3E).copy(alpha = if (isDark) 0.30f else 0.18f),
            1.0f to Color.Transparent,
            center = Offset.Zero, radius = 1f
        )

        val runnerHeadBrush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = 0.12f),
            0.55f to Color(0xFFA0C8E8).copy(alpha = 0.18f),
            0.85f to Color(0xFF0E2235).copy(alpha = 0.32f),
            1.0f to Color.Transparent,
            center = Offset.Zero, radius = 1f
        )

        // 窓の微小な空気感（結露フロスト）
        val frost = Brush.verticalGradient(
            listOf(
                Color(0xFFD6E4EA).copy(alpha = if (isDark) 0.02f else 0.04f),
                Color(0xFFCBDDE6).copy(alpha = 0.012f),
                Color(0xFF849CAA).copy(alpha = if (isDark) 0.035f else 0.05f)
            )
        )

        val path = Path()

        onDrawWithContent {
            drawContent()
            if (size.width <= 1f || size.height <= 1f) return@onDrawWithContent
            drawRect(frost)

            val time = seconds()
            val rawTiltX = tiltState?.tiltX ?: 0f
            val rawTiltY = tiltState?.tiltY ?: 0f
            val tiltX = if (rawTiltX.isFinite()) rawTiltX else 0f
            val tiltY = if (rawTiltY.isFinite()) rawTiltY else 0f

            // A. 静止水滴（ビーズ）の描画
            val beadStretch = 1.05f + kotlin.math.abs(tiltX) * 0.08f
            beads.forEach { drop ->
                val bx = drop.x + tiltX * 3f
                val by = drop.y + tiltY * 2f
                val r = drop.radius * (0.93f + 0.07f * sin(time * 0.15f + drop.seed))

                withTransform({
                    translate(bx, by)
                    scale(r, r * beadStretch, Offset.Zero)
                }) {
                    drawCircle(beadBaseBrush, 1f, Offset.Zero)
                    // 上端の白色スペキュラハイライト（光のきらめき）
                    drawCircle(
                        Color.White.copy(alpha = 0.70f),
                        0.25f,
                        Offset(-0.28f, -0.32f)
                    )
                    // 底部の優しいコースティクス集光
                    drawCircle(
                        Color(0xFFE1F5FE).copy(alpha = 0.35f),
                        0.30f,
                        Offset(0.18f, 0.34f)
                    )
                }
            }

            // B. したたる滑落水滴（ランナー）と有機的な軌跡
            val travel = size.height + 200f * unit
            val slideSpeed = (16f + strength * 12f) * (1f - tiltY * 0.18f).coerceIn(0.6f, 1.8f) * unit

            runners.forEach { drop ->
                val currentTravel = glassTravel(time, slideSpeed, drop.seed)
                val rawY = rainPhase(drop.y + currentTravel / travel) * travel - 70f * unit

                // 蛇行関数（Meandering）：デフォルト垂直落下 ＋ 微小な有機的揺らぎ ＋ 端末傾き連動
                fun meanderX(yPos: Float): Float {
                    val microWander = sin((yPos / unit) * 0.024f + drop.seed * 19f) * 1.4f * unit +
                        sin((yPos / unit) * 0.055f + drop.seed * 37f) * 0.6f * unit
                    val gyroSlant = (yPos / h.coerceAtLeast(1f)) * (tiltX * 36f * unit)
                    return drop.x + microWander + gyroSlant
                }

                val headX = meanderX(rawY)
                val headY = rawY

                // 中央テキスト領域の視認性配慮（中央気温表示エリア付近はわずかに控えめに）
                val distFromCenterX = kotlin.math.abs(headX - w * 0.5f) / (w * 0.5f)
                val distFromCenterY = kotlin.math.abs(headY - h * 0.26f) / (h * 0.18f)
                val inCenterText = distFromCenterX < 0.65f && distFromCenterY < 1.0f
                val alphaMult = if (inCenterText) 0.48f else 1.0f

                // 速度に応じた縦長ティアドロップ伸長率
                val stretchY = 1.28f + kotlin.math.abs(tiltY) * 0.12f

                // 1. テーパリングする濡れた軌跡（Trail）と残留微小水滴
                val trailLength = (75f + strength * 35f) * unit
                val trailTopY = (headY - trailLength).coerceAtLeast(0f)
                val trailBottomY = headY.coerceAtLeast(trailTopY + 1f)

                path.reset()
                path.moveTo(headX, headY)

                val trailSteps = 14
                for (step in 1..trailSteps) {
                    val stepFrac = step.toFloat() / trailSteps
                    val trailY = headY - stepFrac * trailLength
                    val trailX = meanderX(trailY)
                    path.lineTo(trailX, trailY)

                    // 軌跡上に時折残る微小な残留水滴（毛細管ビード）
                    if (step == 4) {
                        val residueR = drop.radius * 0.24f
                        val fade = (1f - stepFrac) * alphaMult
                        drawCircle(
                            Color.White.copy(alpha = 0.45f * fade),
                            residueR,
                            Offset(trailX, trailY)
                        )
                    }
                }

                // 軌跡のグラデーション描画：水滴直後は太く鮮明、上に向かって自然に乾燥・細くフェード
                val trailGradient = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.50f to Color(0xFFB0D2E8).copy(alpha = 0.06f * alphaMult),
                    1.0f to Color.White.copy(alpha = 0.20f * alphaMult),
                    startY = trailTopY,
                    endY = trailBottomY
                )
                drawPath(
                    path = path,
                    brush = trailGradient,
                    style = Stroke(
                        width = drop.radius * 0.48f,
                        cap = StrokeCap.Round
                    )
                )

                // 軌跡中心のハイライト芯線（水膜の光反射）
                val trailCoreGradient = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.65f to Color.White.copy(alpha = 0.10f * alphaMult),
                    1.0f to Color.White.copy(alpha = 0.38f * alphaMult),
                    startY = trailTopY,
                    endY = trailBottomY
                )
                drawPath(
                    path = path,
                    brush = trailCoreGradient,
                    style = Stroke(
                        width = drop.radius * 0.16f,
                        cap = StrokeCap.Round
                    )
                )

                // 2. 滑落水滴ヘッド（ティアドロップ立体レンズ）
                withTransform({
                    translate(headX, headY)
                    scale(drop.radius, drop.radius * stretchY, Offset.Zero)
                }) {
                    drawCircle(runnerHeadBrush, 1f, Offset.Zero, alpha = alphaMult)
                    // 上端の白色スペキュラハイライト（空の反射光）
                    drawCircle(
                        Color.White.copy(alpha = 0.85f * alphaMult),
                        0.25f,
                        Offset(-0.25f, -0.32f)
                    )
                    // 底部のコースティクス集光（三日月状の輝き）
                    drawCircle(
                        Color(0xFFE1F5FE).copy(alpha = 0.42f * alphaMult),
                        0.32f,
                        Offset(0.14f, 0.38f)
                    )
                }
            }
        }
    }

