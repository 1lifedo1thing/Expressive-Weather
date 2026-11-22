package io.github.nemynew.expressiveweather.ui.components

import android.graphics.Matrix
import android.graphics.Path
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

/**
 * セットアップ画面中央のメインビジュアル:
 * - MaterialShape はデバイスカラーを反映したぼかした半透明カラー
 * - 光沢感や影は排除し、すっきりとしたフラット＆ソフトな半透明テクスチャ
 * - 形が変わる途中の素早さは軽快なまま（約500ms）、各形状で約1.8秒静止・鑑賞できる心地よい頻度に調整
 * - Pill、Cookie12Sided、Sunny、Cookie9Sided、VerySunny が滑らかにモーフィング
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingSetupVisual(
    modifier: Modifier = Modifier,
    visualSize: Dp = 290.dp,
    iconSize: Dp = 120.dp
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "SetupShapeMorph")

    // 多彩な Material 3 Expressive シェイプ（5つの形状の連続モーフィング）
    val morphs = remember {
        listOf(
            Morph(MaterialShapes.Pill, MaterialShapes.Cookie12Sided),
            Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Sunny),
            Morph(MaterialShapes.Sunny, MaterialShapes.Cookie9Sided),
            Morph(MaterialShapes.Cookie9Sided, MaterialShapes.VerySunny),
            Morph(MaterialShapes.VerySunny, MaterialShapes.Pill)
        )
    }

    // 1ステージあたり 2400ms（約1850ms静止＋約550msの素早い変形）× 5形状 ＝ 12000ms
    val stageDurationMillis = 2400
    val morphDurationMillis = 550
    val totalDurationMillis = stageDurationMillis * 5

    val overallProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = totalDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OverallMorphProgress"
    )

    // 再利用可能な Path / Matrix オブジェクト（毎フレームのGCをゼロ化）
    val tileAndroidPath = remember { Path() }
    val tileMatrix = remember { Matrix() }

    // アプリアイコンの Bitmap 取得
    val appIconBitmap: ImageBitmap? = remember(context) {
        try {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 160
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 160
            val bitmap = android.graphics.Bitmap.createBitmap(
                width,
                height,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier.size(visualSize),
        contentAlignment = Alignment.Center
    ) {
        // ================= 1. MaterialShape（クリーンな輪郭の半透明デバイスカラー） =================
        // 影やぼかしの滲みを排除した、すっきりとしたフラット＆ソフトな半透明テクスチャ
        Canvas(
            modifier = Modifier.size(visualSize)
        ) {
            val width = size.width
            val height = size.height

            val stage = overallProgress % 5f
            val stageIndex = stage.toInt().coerceIn(0, 4)
            val stageFraction = stage - stageIndex
            val restFraction = 1f - (morphDurationMillis.toFloat() / stageDurationMillis.toFloat())

            val easedFraction = if (stageFraction < restFraction) {
                0f
            } else {
                val morphProgress = (stageFraction - restFraction) / (1f - restFraction)
                FastOutSlowInEasing.transform(morphProgress)
            }

            tileMatrix.reset()
            tileMatrix.setScale(width, height)

            tileAndroidPath.reset()
            morphs[stageIndex].toPath(easedFraction, tileAndroidPath)
            tileAndroidPath.transform(tileMatrix)
            val composeTilePath = tileAndroidPath.asComposePath()

            // 半透明のデバイスカラー（光沢エッジや影はなし）
            drawPath(
                path = composeTilePath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        colorScheme.primaryContainer.copy(alpha = 0.45f),
                        colorScheme.surfaceVariant.copy(alpha = 0.30f),
                        colorScheme.secondaryContainer.copy(alpha = 0.38f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(width, height)
                )
            )
        }

        // ================= 2. 前面・中心: アプリアイコン =================
        // 光沢ボーダーやドロップシャドウを排除した、シンプルで美しい白背景のフラットコンテナ
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            if (appIconBitmap != null) {
                Image(
                    bitmap = appIconBitmap,
                    contentDescription = "Expressive Weather App Icon",
                    modifier = Modifier.size(iconSize * 0.82f)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(iconSize * 0.58f)
                )
            }
        }
    }
}
