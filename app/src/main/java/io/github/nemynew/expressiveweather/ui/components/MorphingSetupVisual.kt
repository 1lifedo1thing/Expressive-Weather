package io.github.nemynew.expressiveweather.ui.components

import android.graphics.Matrix
import android.graphics.Path
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.theme.LocalDarkTheme

/**
 * セットアップ画面中央のメインビジュアル:
 * - 背景の MaterialShapes は存在感のある高不透明度（85〜92%）のデバイスカラーグラデーション
 * - 前面はランチャーのアプリアイコンとは完全に独立した、セットアップ画面専用の完全な円形アイコン
 * - 形が変わる途中の素早さは軽快なまま（約550ms）、各形状で約1.85秒静止・鑑賞できる心地よい頻度に調整
 * - OS の「アニメーション無効化」設定時は静止表示へフォールバック
 * - Pill、Cookie12Sided、Sunny、Cookie9Sided、VerySunny が滑らかにモーフィング
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingSetupVisual(
    modifier: Modifier = Modifier,
    visualSize: Dp = 290.dp,
    iconSize: Dp = 120.dp,
    iconContentDescription: String? = null
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalDarkTheme.current

    // OS のアニメーションスケール設定を尊重（0f の場合は静止表示）
    val animationsEnabled = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        } catch (_: Exception) {
            true
        }
    }

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

    val overallProgress: Float = if (animationsEnabled) {
        val infiniteTransition = rememberInfiniteTransition(label = "SetupShapeMorph")
        val animatedProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = totalDurationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "OverallMorphProgress"
        )
        animatedProgress
    } else {
        // アニメーション無効時は視認性の高い Sunny シェイプで静止
        2f
    }

    // 再利用可能な Path / Matrix オブジェクト（毎フレームのGCをゼロ化）
    val tileAndroidPath = remember { Path() }
    val tileMatrix = remember { Matrix() }

    Box(
        modifier = modifier.size(visualSize),
        contentAlignment = Alignment.Center
    ) {
        // ================= 1. MaterialShape（輪郭がくっきり映える高不透明度のデバイスカラー） =================
        Canvas(
            modifier = Modifier.size(visualSize)
        ) {
            val width = size.width
            val height = size.height

            val stage = overallProgress % 5f
            val stageIndex = stage.toInt().coerceIn(0, 4)
            val stageFraction = stage - stageIndex
            val restFraction = 1f - (morphDurationMillis.toFloat() / stageDurationMillis.toFloat())

            val easedFraction = if (!animationsEnabled || stageFraction < restFraction) {
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

            val shapeColors = if (isDark) {
                listOf(
                    colorScheme.primaryContainer.copy(alpha = 0.90f),
                    colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    colorScheme.secondaryContainer.copy(alpha = 0.86f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.92f),
                    colorScheme.primaryContainer.copy(alpha = 0.88f),
                    colorScheme.secondaryContainer.copy(alpha = 0.86f)
                )
            }

            drawPath(
                path = composeTilePath,
                brush = Brush.linearGradient(
                    colors = shapeColors,
                    start = Offset(0f, 0f),
                    end = Offset(width, height)
                )
            )
        }

        // ================= 2. 前面・中心: セットアップ画面専用の完全円形アイコン =================
        Image(
            painter = painterResource(id = R.drawable.setup_circle_app_icon),
            contentDescription = iconContentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
        )
    }
}
