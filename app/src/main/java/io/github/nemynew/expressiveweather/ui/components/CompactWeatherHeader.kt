package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import io.github.nemynew.expressiveweather.theme.WeatherAtmosphereTheme
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.GoogleSansFlexHeroTemp

/**
 * 下にスクロールして巨大な天気が見切れる際に表示される折りたたみコンパクトヘッダー
 * - 添付画像に完全一致：
 *   左側：上段に地名（東京）、下段に天気（晴れ）
 *   右側：大きな気温（27°）
 * - 背景：Haze Backdrop Blur による背後カードの本物のすりガラスぼかし ＋ フロスト光グラデーション
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun CompactWeatherHeader(
    cityName: String,
    conditionName: String,
    currentTemp: Int,
    atmosphere: WeatherAtmosphereTheme,
    isDark: Boolean,
    visible: Boolean,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                slideInVertically(
                    initialOffsetY = { -it / 3 },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                slideOutVertically(
                    targetOffsetY = { -it / 3 },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // 背景レイヤー：フロストすりガラス（ぼかし）グラデーション
            val skyColor = if (isDark) Color(0xFF0F1E30) else Color(0xFF6BA2E0)
            val frostLight = if (isDark) Color(0xFF22354E) else Color(0xFFEBF3FB)

            // 1. すりガラスぼかしレイヤー（Haze Backdrop Blur + グラデーション透過マスク）
            // 背後を通過するカードの文字やグラフを光学的にすりガラス状にぼかし、下部へ向かって自然にグラデーション消失
            if (hazeState != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.65f to Color.Black,
                                    0.85f to Color.Black.copy(alpha = 0.5f),
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                        .hazeEffect(
                            state = hazeState,
                            style = HazeMaterials.ultraThin().copy(
                                blurRadius = 32.dp,
                                tints = listOf(
                                    dev.chrisbanes.haze.HazeTint(
                                        color = skyColor.copy(alpha = if (isDark) 0.60f else 0.45f)
                                    )
                                )
                            )
                        )
                )
            }

            // 2. フロスト光彩＆スカイグラデーション層（添付画像のすりガラスの白い光沢と空色のグラデーション）
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to skyColor.copy(alpha = if (isDark) 0.85f else 0.78f),
                            0.50f to skyColor.copy(alpha = if (isDark) 0.70f else 0.60f),
                            0.75f to skyColor.copy(alpha = if (isDark) 0.35f else 0.28f),
                            0.90f to skyColor.copy(alpha = if (isDark) 0.10f else 0.08f),
                            1.0f to Color.Transparent
                        )
                    )
            )

            // 3. フロストハイライト層（光の反射・すりガラスの質感）
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to frostLight.copy(alpha = if (isDark) 0.18f else 0.45f),
                            0.50f to frostLight.copy(alpha = if (isDark) 0.08f else 0.25f),
                            0.80f to Color.Transparent
                        )
                    )
            )

            // テキストシャドウ（文字の視認性と立体感を最大化）
            val textShadow = androidx.compose.ui.graphics.Shadow(
                color = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF1E3A5F).copy(alpha = 0.25f),
                offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                blurRadius = 8f
            )

            // コンテンツレイヤー（ステータスバー余白直下に近接配置 + 添付画像のレイアウト）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 0.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左側：地名（上） ＋ 天気（下）
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = cityName,
                            fontFamily = GoogleSansFlex,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.95f),
                            letterSpacing = 0.3.sp,
                            style = androidx.compose.ui.text.TextStyle(shadow = textShadow)
                        )
                        Text(
                            text = conditionName,
                            fontFamily = GoogleSansFlex,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp,
                            style = androidx.compose.ui.text.TextStyle(shadow = textShadow)
                        )
                    }

                    // 右側：気温（°付き）
                    Text(
                        text = "${currentTemp}°",
                        fontFamily = GoogleSansFlexHeroTemp,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-1).sp,
                        style = androidx.compose.ui.text.TextStyle(shadow = textShadow)
                    )
                }

                // 文字位置を変えずに背景領域を下方向へ自然に拡張
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
