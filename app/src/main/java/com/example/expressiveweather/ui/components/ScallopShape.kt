package com.example.expressiveweather.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive の 12SidedShape (Cookie12Sided) スカロップ形状
 *
 * 気象庁キキクル警戒レベルバッジで用いられる公式の12面スカロップ（花びら・クッキー型）Shape。
 */
val TwelveSidedScallopShape: Shape
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    get() = MaterialShapes.Cookie12Sided.toShape()

/**
 * 非 Composable 文脈でも即座にインスタンス化可能な12面スカロップ Shape
 */
val DefaultScallop12Shape: Shape = CustomScallopShape(lobes = 12, depth = 0.12f)

/**
 * スカロップ（波状・花形12面）形状をベジェ曲線で描画するカスタム Shape 実装。
 *
 * 12個のなめらかな山（lobe）を円周上に均等配置し、
 * クッキー・スカロップのような丸みを帯びた輪郭を数学的に構築します。
 *
 * @param lobes 突起の数（デフォルト 12）
 * @param depth 谷の深さ比率（0.0f〜0.3f、デフォルト 0.12f）
 */
class CustomScallopShape(
    private val lobes: Int = 12,
    private val depth: Float = 0.12f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = minOf(centerX, centerY)
        val outerRadius = baseRadius
        val innerRadius = baseRadius * (1f - depth.coerceIn(0.01f, 0.4f))

        val totalPoints = lobes * 2
        val step = (2.0 * PI / totalPoints).toFloat()

        // 始点（最外突起の頂点）
        val startX = centerX + outerRadius * cos(0f)
        val startY = centerY + outerRadius * sin(0f)
        path.moveTo(startX, startY)

        for (i in 0 until lobes) {
            val peakAngle = i * 2 * step
            val valleyAngle = peakAngle + step
            val nextPeakAngle = (i + 1) * 2 * step

            val p0x = centerX + outerRadius * cos(peakAngle)
            val p0y = centerY + outerRadius * sin(peakAngle)

            val vx = centerX + innerRadius * cos(valleyAngle)
            val vy = centerY + innerRadius * sin(valleyAngle)

            val p1x = centerX + outerRadius * cos(nextPeakAngle)
            val p1y = centerY + outerRadius * sin(nextPeakAngle)

            // なめらかな波形をつなぐ3次ベジェ曲線
            val cp1Angle = peakAngle + step * 0.5f
            val cp1Radius = (outerRadius + innerRadius) / 2f
            val cp1x = centerX + cp1Radius * cos(cp1Angle)
            val cp1y = centerY + cp1Radius * sin(cp1Angle)

            val cp2Angle = valleyAngle + step * 0.5f
            val cp2Radius = (outerRadius + innerRadius) / 2f
            val cp2x = centerX + cp2Radius * cos(cp2Angle)
            val cp2y = centerY + cp2Radius * sin(cp2Angle)

            path.quadraticTo(cp1x, cp1y, vx, vy)
            path.quadraticTo(cp2x, cp2y, p1x, p1y)
        }
        path.close()

        return Outline.Generic(path)
    }
}
