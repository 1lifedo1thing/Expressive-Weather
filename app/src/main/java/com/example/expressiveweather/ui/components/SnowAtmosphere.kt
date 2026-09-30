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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// 画面両端をスムーズに循環ラップさせる関数
internal fun snowPhase(value: Float): Float = value - floor(value)

private data class SnowParticle(
    val x: Float,
    val phase: Float,
    val speed: Float,
    val radius: Float,
    val wobbleSpeed: Float,
    val wobbleAmp: Float,
    val spinSpeed: Float,
    val rotOffset: Float,
    val alpha: Float
)

@Composable
internal fun SnowAtmosphere(
    isDark: Boolean,
    tiltState: com.example.expressiveweather.ui.util.DeviceTiltState? = null,
    modifier: Modifier = Modifier
) {
    // 3層構造の物理降雪パーティクル
    val fgFlakes = remember {
        val rng = Random(101)
        List(24) {
            SnowParticle(
                x = rng.nextFloat(),
                phase = rng.nextFloat(),
                speed = 0.45f + rng.nextFloat() * 0.35f,
                radius = 5.0f + rng.nextFloat() * 3.5f,
                wobbleSpeed = 0.8f + rng.nextFloat() * 0.7f,
                wobbleAmp = 18f + rng.nextFloat() * 24f,
                spinSpeed = 25f + rng.nextFloat() * 40f,
                rotOffset = rng.nextFloat() * 360f,
                alpha = 0.65f + rng.nextFloat() * 0.30f
            )
        }
    }

    val mgFlakes = remember {
        val rng = Random(202)
        List(60) {
            SnowParticle(
                x = rng.nextFloat(),
                phase = rng.nextFloat(),
                speed = 0.65f + rng.nextFloat() * 0.45f,
                radius = 2.6f + rng.nextFloat() * 2.2f,
                wobbleSpeed = 1.2f + rng.nextFloat() * 1.0f,
                wobbleAmp = 12f + rng.nextFloat() * 16f,
                spinSpeed = 40f + rng.nextFloat() * 60f,
                rotOffset = rng.nextFloat() * 360f,
                alpha = 0.70f + rng.nextFloat() * 0.28f
            )
        }
    }

    val bgPowder = remember {
        val rng = Random(303)
        List(110) {
            SnowParticle(
                x = rng.nextFloat(),
                phase = rng.nextFloat(),
                speed = 0.85f + rng.nextFloat() * 0.55f,
                radius = 1.0f + rng.nextFloat() * 1.2f,
                wobbleSpeed = 0.6f + rng.nextFloat() * 0.6f,
                wobbleAmp = 6f + rng.nextFloat() * 8f,
                spinSpeed = 0f,
                rotOffset = 0f,
                alpha = 0.30f + rng.nextFloat() * 0.35f
            )
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    var seconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val motionScale = Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            )
            if (motionScale > 0f) {
                var lastFrame = 0L
                while (true) {
                    withFrameNanos { now ->
                        if (lastFrame != 0L) {
                            seconds += ((now - lastFrame) / 1_000_000_000f)
                                .coerceAtMost(0.05f) / motionScale
                        }
                        lastFrame = now
                    }
                }
            }
        }
    }

    Box(modifier.fillMaxSize().clipToBounds().drawWithCache {
        val w = size.width
        val h = size.height
        val safeW = w.coerceAtLeast(1f)
        val safeH = h.coerceAtLeast(1f)

        val skyTop = if (isDark) Color(0xFF0C1824) else Color(0xFF6B8FA8)
        val skyMid = if (isDark) Color(0xFF132232) else Color(0xFF88A5BC)
        val skyBottom = if (isDark) Color(0xFF081018) else Color(0xFFB4C8D8)
        val skyBrush = Brush.verticalGradient(listOf(skyTop, skyMid, skyBottom))

        val mistColor = if (isDark) Color(0xFF5D7B93) else Color(0xFFD4E5F2)
        val mistBrush = Brush.radialGradient(
            0f to mistColor.copy(alpha = if (isDark) 0.18f else 0.28f),
            0.6f to mistColor.copy(alpha = if (isDark) 0.08f else 0.14f),
            1f to Color.Transparent,
            center = Offset(safeW * 0.5f, safeH * 0.25f),
            radius = (safeW * 0.75f).coerceAtLeast(1f)
        )

        // 6方向の樹状突起を持つ美しい六角形雪結晶パス（Midground用）
        val crystalPath = Path().apply {
            for (i in 0 until 6) {
                val angle = i * PI.toFloat() / 3f
                val dx = cos(angle)
                val dy = sin(angle)
                moveTo(0f, 0f)
                lineTo(dx, dy)

                val barbLength = 0.38f
                val barbAngle1 = angle + PI.toFloat() * 0.3f
                val barbAngle2 = angle - PI.toFloat() * 0.3f
                val px = dx * 0.62f
                val py = dy * 0.62f
                moveTo(px, py)
                lineTo(px + cos(barbAngle1) * barbLength, py + sin(barbAngle1) * barbLength)
                moveTo(px, py)
                lineTo(px + cos(barbAngle2) * barbLength, py + sin(barbAngle2) * barbLength)
            }
        }

        onDrawBehind {
            if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
            val time = seconds
            val rawTiltX = tiltState?.tiltX ?: 0f
            val rawTiltY = tiltState?.tiltY ?: 0f
            val tiltX = if (rawTiltX.isFinite()) rawTiltX else 0f
            val tiltY = if (rawTiltY.isFinite()) rawTiltY else 0f

            // 1. 冬の空グラデーションと冷気ミスト
            drawRect(skyBrush)
            drawRect(mistBrush)

            // 全体的な冬の風（穏やかな揺らぎ）
            val windOffset = sin(time * 0.4f) * 14.dp.toPx()

            // 2. 最奥層 (Background Powder): 無数の細かい粉雪 (遠景パララックス)
            val tiltOffsetBg = tiltX * 22.dp.toPx()
            val speedMultBg = (1f - tiltY * 0.18f).coerceIn(0.5f, 1.8f)
            bgPowder.forEach { p ->
                val travel = h + 20.dp.toPx()
                val y = snowPhase(p.phase + time * 0.14f * p.speed * speedMultBg) * travel - 10.dp.toPx()
                val wobble = sin(time * p.wobbleSpeed + p.x * 20f) * p.wobbleAmp
                val x = snowPhase(p.x + (windOffset * 0.3f + wobble + tiltOffsetBg) / w) * w
                drawCircle(
                    color = Color.White.copy(alpha = p.alpha * 0.75f),
                    radius = p.radius.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // 3. 中層 (Midground Crystals): 六角結晶構造を持つ舞い落ちる雪片 (中景パララックス)
            val tiltOffsetMg = tiltX * 52.dp.toPx()
            val speedMultMg = (1f - tiltY * 0.24f).coerceIn(0.5f, 1.8f)
            mgFlakes.forEach { p ->
                val travel = h + 30.dp.toPx()
                val y = snowPhase(p.phase + time * 0.18f * p.speed * speedMultMg) * travel - 15.dp.toPx()
                val wobble = sin(time * p.wobbleSpeed + p.x * 12f) * p.wobbleAmp
                val x = snowPhase(p.x + (windOffset * 0.6f + wobble + tiltOffsetMg) / w) * w
                val currentRot = time * p.spinSpeed + p.rotOffset + tiltX * 45f
                val radPx = p.radius.dp.toPx()

                withTransform({
                    translate(x, y)
                    rotate(currentRot)
                    scale(radPx, radPx, Offset.Zero)
                }) {
                    drawPath(
                        path = crystalPath,
                        color = Color.White.copy(alpha = p.alpha),
                        style = Stroke(width = 0.35f, cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha * 0.9f),
                        radius = 0.28f,
                        center = Offset.Zero
                    )
                }
            }

            // 4. 前景層 (Foreground Fluffy Flakes): カメラ近くをふわふわと漂う大きなぼかし雪片 (近景ダイナミックパララックス)
            val tiltOffsetFg = tiltX * 95.dp.toPx()
            val speedMultFg = (1f - tiltY * 0.30f).coerceIn(0.5f, 2.0f)
            fgFlakes.forEach { p ->
                val travel = h + 50.dp.toPx()
                val y = snowPhase(p.phase + time * 0.22f * p.speed * speedMultFg) * travel - 25.dp.toPx()
                val wobble = sin(time * p.wobbleSpeed + p.x * 8f) * p.wobbleAmp
                val x = snowPhase(p.x + (windOffset + wobble + tiltOffsetFg) / w) * w
                val radPx = p.radius.dp.toPx()

                // 外側ソフトボケオーラ
                drawCircle(
                    color = Color.White.copy(alpha = p.alpha * 0.35f),
                    radius = radPx * 1.55f,
                    center = Offset(x, y)
                )
                // メインボディ
                drawCircle(
                    color = Color.White.copy(alpha = p.alpha * 0.85f),
                    radius = radPx,
                    center = Offset(x, y)
                )
                // 芯（高輝度コア）
                drawCircle(
                    color = Color.White.copy(alpha = p.alpha),
                    radius = radPx * 0.45f,
                    center = Offset(x, y)
                )
            }
        }
    })
}
