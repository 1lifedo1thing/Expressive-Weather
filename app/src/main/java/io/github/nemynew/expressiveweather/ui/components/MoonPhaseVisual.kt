package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import io.github.nemynew.expressiveweather.theme.AccentColorMode
import io.github.nemynew.expressiveweather.theme.currentAccentPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * 天文位相 (`phaseFraction`: 0.0=新月, 0.25=上弦, 0.5=満月, 0.75=下弦) に基づき、
 * Material 3 の `colorScheme`（天気・壁紙テーマ連動カラー）を用いた
 * 端正で記号的なフラット 2 トーン月相シンボルを描画する。
 */
@Composable
fun MoonPhaseDisc(
    phaseFraction: Float,
    modifier: Modifier = Modifier,
    isSouthernHemisphere: Boolean = false,
    isColorful: Boolean = true,
    isDark: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val useDarkTokens = isDark || colorScheme.surface.luminance() < 0.5f
    val normalizedPhase = ((phaseFraction % 1f) + 1f) % 1f
    val cosPhase = cos(2.0 * PI * normalizedPhase.toDouble()).toFloat()
    val illuminationFactor = ((1f - cosPhase) / 2f).coerceIn(0f, 1f)

    val accentPalette = currentAccentPalette()
    val useColorfulMoon = isColorful && accentPalette.mode != AccentColorMode.MATERIAL_YOU_TONAL

    // 選択アクセントモード連動パレット（Material You 調和 / テーマ統一 / クラシック）
    val litColor = if (useColorfulMoon) {
        if (useDarkTokens) accentPalette.sun.accent else accentPalette.sun.container
    } else {
        if (useDarkTokens) colorScheme.primary else colorScheme.primaryContainer
    }
    val shadowFill = if (useColorfulMoon) {
        if (useDarkTokens) {
            accentPalette.moon.container.copy(alpha = 0.78f)
        } else {
            accentPalette.moon.accent.copy(alpha = 0.36f)
        }
    } else {
        colorScheme.primary.copy(alpha = if (useDarkTokens) 0.28f else 0.22f)
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerRadius = size.minDimension / 2f
        val radius = outerRadius * 0.86f
        val litOnRight = if (isSouthernHemisphere) normalizedPhase >= 0.5f else normalizedPhase < 0.5f

        // 1. ごく控えめな M3 月光リフト
        if (illuminationFactor > 0.04f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to litColor.copy(alpha = 0.16f * illuminationFactor),
                        0.72f to litColor.copy(alpha = 0.06f * illuminationFactor),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = Offset(cx, cy)
            )
        }

        val circleRect = Rect(cx - radius, cy - radius, cx + radius, cy + radius)

        // 2. 陰面ディスク（アウトラインなしのフラットな M3 トーン円）
        drawCircle(
            color = shadowFill,
            radius = radius,
            center = Offset(cx, cy)
        )

        // 3. 光面シルエット（正確な天文ターミネーターによるフラット M3 カラー描画）
        if (illuminationFactor > 0.008f) {
            val rx = radius * abs(cosPhase)
            val isGibbous = cosPhase < 0f
            val litPath = buildLitMoonPath(
                cx = cx,
                cy = cy,
                radius = radius,
                circleRect = circleRect,
                litOnRight = litOnRight,
                rx = rx,
                isGibbous = isGibbous,
                isFull = illuminationFactor >= 0.99f
            )

            drawPath(
                path = litPath,
                color = litColor
            )
        }
    }
}

private fun buildLitMoonPath(
    cx: Float,
    cy: Float,
    radius: Float,
    circleRect: Rect,
    litOnRight: Boolean,
    rx: Float,
    isGibbous: Boolean,
    isFull: Boolean
): Path {
    val path = Path()
    if (isFull) {
        path.addOval(circleRect)
        return path
    }
    if (litOnRight) {
        path.arcTo(
            rect = circleRect,
            startAngleDegrees = -90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = true
        )
        if (rx < 0.5f) {
            path.lineTo(cx, cy - radius)
        } else {
            val terminatorRect = Rect(cx - rx, cy - radius, cx + rx, cy + radius)
            path.arcTo(
                rect = terminatorRect,
                startAngleDegrees = 90f,
                sweepAngleDegrees = if (isGibbous) 180f else -180f,
                forceMoveTo = false
            )
        }
        path.close()
    } else {
        path.arcTo(
            rect = circleRect,
            startAngleDegrees = 90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = true
        )
        if (rx < 0.5f) {
            path.lineTo(cx, cy + radius)
        } else {
            val terminatorRect = Rect(cx - rx, cy - radius, cx + rx, cy + radius)
            path.arcTo(
                rect = terminatorRect,
                startAngleDegrees = -90f,
                sweepAngleDegrees = if (isGibbous) 180f else -180f,
                forceMoveTo = false
            )
        }
        path.close()
    }
    return path
}
