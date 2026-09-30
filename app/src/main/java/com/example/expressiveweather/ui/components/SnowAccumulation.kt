package com.example.expressiveweather.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 雪が積もるタイルの形状種別
 */
enum class SnowTileShape {
    ROUNDED_RECT, // 角丸長方形 / 正方形 (HourlyForecast, Bentoカード, WeeklyForecastなど)
    CIRCLE        // 正円 (WindCard, SunCardなど)
}

/**
 * タイルやボックスの輪郭・形状に完全に沿って、うっすらと上品に積もる雪のエフェクトを提供する Modifier。
 */
@Composable
fun Modifier.snowTileAccumulation(
    isSnowing: Boolean,
    isDark: Boolean = false,
    shape: SnowTileShape = SnowTileShape.ROUNDED_RECT,
    cornerRadiusDp: Dp = 44.dp,
    snowDepthDp: Dp = 9.dp,
    seed: Int = 0
): Modifier {
    if (!isSnowing) return this

    val infiniteTransition = rememberInfiniteTransition(label = "SnowSparkle_$seed")
    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000 + (seed % 5) * 350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SparklePhase_$seed"
    )

    return this.then(
        Modifier.drawWithContent {
            // タイル本体を通常描画
            drawContent()

            val cornerRadiusPx = cornerRadiusDp.toPx()
            val snowDepthPx = snowDepthDp.toPx()

            when (shape) {
                SnowTileShape.ROUNDED_RECT -> drawRoundedRectSnowDusting(
                    isDark = isDark,
                    cornerRadiusPx = cornerRadiusPx,
                    snowDepthPx = snowDepthPx,
                    sparklePhase = sparklePhase,
                    seed = seed
                )
                SnowTileShape.CIRCLE -> drawCircleSnowDusting(
                    isDark = isDark,
                    snowDepthPx = snowDepthPx,
                    sparklePhase = sparklePhase,
                    seed = seed
                )
            }
        }
    )
}

/**
 * 角丸タイルの上縁に沿って自然な立体感と柔らかな起伏を持って積もる雪
 */
private fun DrawScope.drawRoundedRectSnowDusting(
    isDark: Boolean,
    cornerRadiusPx: Float,
    snowDepthPx: Float,
    sparklePhase: Float,
    seed: Int
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val r = cornerRadiusPx.coerceAtMost(w * 0.5f).coerceAtMost(h * 0.5f)
    val crestHighlightPath = Path()
    val fullSnowPath = Path()
    val shadowPath = Path()

    val steps = 54
    val sparklePositions = mutableListOf<Offset>()

    // 1. 上部稜線（Top Crest）: タイル上辺と角丸に沿い、わずかに柔らかなマウンド（約2.5dp）を盛り上げる
    var first = true
    for (i in 0..steps) {
        val frac = i.toFloat() / steps
        val x = frac * w

        val tileTopY = when {
            x < r -> {
                val dx = r - x
                r - kotlin.math.sqrt((r * r - dx * dx).coerceAtLeast(0f))
            }
            x > w - r -> {
                val dx = x - (w - r)
                r - kotlin.math.sqrt((r * r - dx * dx).coerceAtLeast(0f))
            }
            else -> 0f
        }

        // 左右端で0に収束するため、カードの横幅(w)を絶対に飛び出さない
        val edgeFade = sin(frac * PI.toFloat()).coerceIn(0f, 1f)
        val mound = 1f + sin(frac * PI.toFloat() * 5f + seed) * 0.28f
        val crestRise = (snowDepthPx * 0.32f * mound) * edgeFade
        val crestY = tileTopY - crestRise

        if (first) {
            fullSnowPath.moveTo(x, crestY)
            crestHighlightPath.moveTo(x, crestY)
            first = false
        } else {
            fullSnowPath.lineTo(x, crestY)
            crestHighlightPath.lineTo(x, crestY)
        }

        if (i in listOf(14, 30, 42) && edgeFade > 0.5f) {
            sparklePositions.add(Offset(x, crestY + 1.5f))
        }
    }

    // 2. 下部リップ（Bottom Lip）: タイル表面に約5.5dp自然に垂れ下がる波状のふくらみ
    for (i in steps downTo 0) {
        val frac = i.toFloat() / steps
        val x = frac * w

        val tileTopY = when {
            x < r -> {
                val dx = r - x
                r - kotlin.math.sqrt((r * r - dx * dx).coerceAtLeast(0f))
            }
            x > w - r -> {
                val dx = x - (w - r)
                r - kotlin.math.sqrt((r * r - dx * dx).coerceAtLeast(0f))
            }
            else -> 0f
        }

        val edgeFade = sin(frac * PI.toFloat()).coerceIn(0f, 1f)
        val scallop = 1f + cos(frac * PI.toFloat() * 6f + seed) * 0.22f
        val lipDrape = (snowDepthPx * 0.68f * scallop) * edgeFade
        val lipY = tileTopY + lipDrape

        fullSnowPath.lineTo(x, lipY)

        if (i == steps) {
            shadowPath.moveTo(x, lipY + 2f)
        } else {
            shadowPath.lineTo(x, lipY + 2f)
        }
    }
    fullSnowPath.close()

    // --- レンダリング ---
    // A. タイル表面への柔らかな接触影（雪の存在感と立体感を際立たせる）
    val shadowColor = if (isDark) Color(0x35000000) else Color(0x280A1828)
    drawPath(
        path = shadowPath,
        color = shadowColor,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )

    // B. 雪の本体グラデーション（底部のアイストーンから頂部の純白まで）
    val topColor = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.94f) else Color(0xFFFFFFFF)
    val midColor = if (isDark) Color(0xFFD8E5F2).copy(alpha = 0.82f) else Color(0xFFF7FAFD).copy(alpha = 0.90f)
    val bottomColor = if (isDark) Color(0xFF8BA5BC).copy(alpha = 0.55f) else Color(0xFFCADBE8).copy(alpha = 0.65f)

    val snowBrush = Brush.verticalGradient(
        colors = listOf(topColor, midColor, bottomColor),
        startY = -snowDepthPx * 0.4f,
        endY = snowDepthPx * 0.8f
    )
    drawPath(fullSnowPath, snowBrush)

    // C. 頂部稜線のきらめく白ハイライト輪郭
    val rimColor = Color.White.copy(alpha = if (isDark) 0.88f else 0.96f)
    drawPath(
        path = crestHighlightPath,
        color = rimColor,
        style = Stroke(width = 1.6f, cap = StrokeCap.Round)
    )

    // D. 氷結晶の煌めき（適度な2〜3点のスター）
    sparklePositions.forEachIndexed { idx, pt ->
        val phase = sparklePhase + idx * 2.1f
        val alpha = ((sin(phase) + 1f) * 0.5f).coerceIn(0f, 1f)
        if (alpha > 0.25f) {
            drawIceSparkle(center = pt, size = 3.0f, alpha = alpha * (if (isDark) 0.75f else 0.90f))
        }
    }
}

private fun DrawScope.drawCircleSnowDusting(
    isDark: Boolean,
    snowDepthPx: Float,
    sparklePhase: Float,
    seed: Int
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val cx = w * 0.5f
    val cy = h * 0.5f
    val radius = w * 0.5f

    // 積雪する円弧範囲: 頂点（真上）を中心に左右約44度（-0.24π 〜 +0.24π）
    val halfArc = 0.24f * PI.toFloat()
    val steps = 36

    val crestHighlightPath = Path()
    val fullSnowPath = Path()
    val shadowPath = Path()

    // 1. 上部稜線（わずかに約2.5dp上にふっくら盛り上がる）
    var first = true
    for (i in 0..steps) {
        val t = -halfArc + (i.toFloat() / steps) * (halfArc * 2f)
        val taper = cos(t / halfArc * (PI.toFloat() * 0.5f)).coerceAtLeast(0f)
        val mound = 1f + sin(t * 10f + seed) * 0.20f
        val rOuter = radius + (snowDepthPx * 0.30f * mound) * taper

        val x = cx + rOuter * sin(t)
        val y = cy - rOuter * cos(t)

        if (first) {
            fullSnowPath.moveTo(x, y)
            crestHighlightPath.moveTo(x, y)
            first = false
        } else {
            fullSnowPath.lineTo(x, y)
            crestHighlightPath.lineTo(x, y)
        }
    }

    // 2. 下部リップ（円の内側へ約5.5dp自然に垂れ下がる）
    for (i in steps downTo 0) {
        val t = -halfArc + (i.toFloat() / steps) * (halfArc * 2f)
        val taper = cos(t / halfArc * (PI.toFloat() * 0.5f)).coerceAtLeast(0f)
        val scallop = 1f + cos(t * 12f + seed) * 0.18f
        val rInner = radius - (snowDepthPx * 0.65f * scallop) * taper

        val x = cx + rInner * sin(t)
        val y = cy - rInner * cos(t)
        fullSnowPath.lineTo(x, y)

        if (i == steps) {
            shadowPath.moveTo(x, y + 1.8f)
        } else {
            shadowPath.lineTo(x, y + 1.8f)
        }
    }
    fullSnowPath.close()

    // --- レンダリング ---
    // A. 接触影
    val shadowColor = if (isDark) Color(0x35000000) else Color(0x280A1828)
    drawPath(
        path = shadowPath,
        color = shadowColor,
        style = Stroke(width = 3.2f, cap = StrokeCap.Round)
    )

    // B. 雪グラデーション
    val topColor = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.94f) else Color(0xFFFFFFFF)
    val midColor = if (isDark) Color(0xFFD8E5F2).copy(alpha = 0.82f) else Color(0xFFF7FAFD).copy(alpha = 0.90f)
    val bottomColor = if (isDark) Color(0xFF8BA5BC).copy(alpha = 0.55f) else Color(0xFFCADBE8).copy(alpha = 0.65f)

    val snowBrush = Brush.radialGradient(
        colors = listOf(topColor, midColor, bottomColor),
        center = Offset(cx, cy - radius),
        radius = snowDepthPx * 1.6f
    )
    drawPath(fullSnowPath, snowBrush)

    // C. 稜線ハイライト
    val rimColor = Color.White.copy(alpha = if (isDark) 0.88f else 0.96f)
    drawPath(
        path = crestHighlightPath,
        color = rimColor,
        style = Stroke(width = 1.6f, cap = StrokeCap.Round)
    )

    // D. 頂点付近の煌めき
    val apexSparkle = Offset(cx, cy - radius - 1f)
    val alpha = ((sin(sparklePhase) + 1f) * 0.5f).coerceIn(0f, 1f)
    if (alpha > 0.25f) {
        drawIceSparkle(center = apexSparkle, size = 3.0f, alpha = alpha * (if (isDark) 0.75f else 0.90f))
    }
}

private fun DrawScope.drawIceSparkle(center: Offset, size: Float, alpha: Float) {
    val glintColor = Color.White.copy(alpha = alpha)
    drawLine(
        color = glintColor,
        start = Offset(center.x - size, center.y),
        end = Offset(center.x + size, center.y),
        strokeWidth = 1.0f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = glintColor,
        start = Offset(center.x, center.y - size),
        end = Offset(center.x, center.y + size),
        strokeWidth = 1.0f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = size * 0.35f,
        center = center
    )
}
