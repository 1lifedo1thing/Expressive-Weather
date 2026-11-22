package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * フロストすりガラスの透過配色を取得。
 * テーマカラー（primary / surfaceTint）を美しくブレンドし、
 * フロストぼかしが有効な状態でも選択中のテーマカラーが鮮やかに反映されるようにする。
 */
fun getFrostedCardColor(
    isFrosted: Boolean,
    isDark: Boolean,
    colorScheme: ColorScheme,
    frostedAlpha: Float? = null
): Color {
    val themeColor = colorScheme.primary
    val surfaceColor = if (isDark) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainer
    return if (isFrosted) {
        // テーマカラーをブレンドした上質なすりガラス半透明
        val tintFactor = if (isDark) 0.14f else 0.10f
        val r = surfaceColor.red * (1f - tintFactor) + themeColor.red * tintFactor
        val g = surfaceColor.green * (1f - tintFactor) + themeColor.green * tintFactor
        val b = surfaceColor.blue * (1f - tintFactor) + themeColor.blue * tintFactor
        Color(r, g, b, frostedAlpha ?: if (isDark) 0.82f else 0.86f)
    } else {
        val tintFactor = if (isDark) 0.08f else 0.06f
        val r = surfaceColor.red * (1f - tintFactor) + themeColor.red * tintFactor
        val g = surfaceColor.green * (1f - tintFactor) + themeColor.green * tintFactor
        val b = surfaceColor.blue * (1f - tintFactor) + themeColor.blue * tintFactor
        Color(r, g, b, 1.0f)
    }
}

/**
 * フロストすりガラスのボーダー（ユーザー要望により枠線は完全撤廃）
 */
fun getFrostedCardBorder(isFrosted: Boolean, isDark: Boolean): BorderStroke? = null

/**
 * Hazeによる実時間背景光学ぼかし効果を適用する Modifier
 * ぼかし強度 54.dp に加え、テーマカラーのティント（色合い）を Haze に適用して
 * すりガラスぼかしそのものにもテーマカラーが鮮明に反映されるようにする。
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.frostedGlassEffect(
    hazeState: HazeState?,
    isFrosted: Boolean,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    blurRadius: Dp = 14.dp,
    shape: Shape? = null
): Modifier {
    if (!isFrosted || hazeState == null) return this
    val isDark = isSystemInDarkTheme()
    val base = if (shape != null) this.clip(shape) else this
    return base.then(
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeMaterials.regular().copy(
                blurRadius = blurRadius,
                tints = listOf(
                    HazeTint(
                        color = tintColor.copy(alpha = if (isDark) 0.18f else 0.14f)
                    )
                )
            )
        )
    )
}

/**
 * 水平スクロールコンテナの左右見切れ部分を滑らかにフェードアウトする Modifier
 */
fun Modifier.horizontalScrollFade(
    scrollState: ScrollState,
    fadeWidth: Dp = 20.dp
): Modifier = this.then(
    Modifier
        .graphicsLayer {
            val needsFade = scrollState.maxValue > 0 && (scrollState.value > 0 || scrollState.value < scrollState.maxValue)
            compositingStrategy = if (needsFade) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        }
        .drawWithContent {
            drawContent()
            if (scrollState.maxValue <= 0) return@drawWithContent
            val fadeWidthPx = fadeWidth.toPx()
            val width = size.width
            if (width <= 0f) return@drawWithContent

            val leftAlpha = if (scrollState.value > 0) 0f else 1f
            val rightAlpha = if (scrollState.value < scrollState.maxValue) 0f else 1f

            if (leftAlpha == 0f || rightAlpha == 0f) {
                val leftFadeFraction = (fadeWidthPx / width).coerceIn(0f, 0.45f)
                val rightFadeFraction = (1f - fadeWidthPx / width).coerceIn(0.55f, 1f)

                drawRect(
                    brush = Brush.horizontalGradient(
                        0.0f to Color.Black.copy(alpha = leftAlpha),
                        leftFadeFraction to Color.Black,
                        rightFadeFraction to Color.Black,
                        1.0f to Color.Black.copy(alpha = rightAlpha)
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
        }
)
