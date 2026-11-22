package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 高級感のあるシマー（光彩移動）アニメーションブラシを生成
 */
@Composable
fun rememberShimmerBrush(
    isDark: Boolean = isSystemInDarkTheme()
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val baseAlpha = if (isDark) 0.12f else 0.22f
    val highlightAlpha = if (isDark) 0.32f else 0.48f
    val color = Color.White

    return Brush.linearGradient(
        colors = listOf(
            color.copy(alpha = baseAlpha),
            color.copy(alpha = highlightAlpha),
            color.copy(alpha = baseAlpha)
        ),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 400f, translateAnim + 400f)
    )
}

fun Modifier.shimmerElement(
    shimmerBrush: Brush,
    shape: Shape = RoundedCornerShape(12.dp)
): Modifier = this
    .clip(shape)
    .background(shimmerBrush)

/**
 * 実画面（Hero、時間ごとカプセル、週間予報、ベントグリッド）の構造と1:1で呼応する
 * リッチな天気ダッシュボード スケルトンスクリーン
 */
@Composable
fun WeatherDashboardSkeleton(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val shimmerBrush = rememberShimmerBrush(isDark = isDark)
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Hero Weather Section Skeleton
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 地名ピル
            Box(
                modifier = Modifier
                    .size(width = 150.dp, height = 32.dp)
                    .shimmerElement(shimmerBrush, shape = RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 天気コンディション名バー
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 24.dp)
                    .shimmerElement(shimmerBrush, shape = RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 巨大な気温の数字ブロック（実際の128sp相当）
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 96.dp)
                    .shimmerElement(shimmerBrush, shape = RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 体感温度バー
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 18.dp)
                    .shimmerElement(shimmerBrush, shape = RoundedCornerShape(9.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 最高 / 最低温度バー
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 18.dp)
                    .shimmerElement(shimmerBrush, shape = RoundedCornerShape(9.dp))
            )
        }

        // 2. Hourly Forecast Section Skeleton (Capsule Shape)
        Card(
            shape = RoundedCornerShape(44.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(54.dp)
                            .height(96.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = if (isDark) 0.06f else 0.12f))
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 時間
                        Box(
                            modifier = Modifier
                                .size(width = 30.dp, height = 10.dp)
                                .shimmerElement(shimmerBrush, shape = RoundedCornerShape(5.dp))
                        )
                        // 天気アイコンサークル
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .shimmerElement(shimmerBrush, shape = CircleShape)
                        )
                        // 気温
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 12.dp)
                                .shimmerElement(shimmerBrush, shape = RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }

        // 3. Weekly 7-Day Forecast Section Skeleton
        Card(
            shape = RoundedCornerShape(44.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                repeat(7) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 曜日ラベル
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 14.dp)
                                .shimmerElement(shimmerBrush, shape = RoundedCornerShape(7.dp))
                        )

                        // 天気アイコン
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .shimmerElement(shimmerBrush, shape = CircleShape)
                        )

                        // 最低気温
                        Box(
                            modifier = Modifier
                                .size(width = 22.dp, height = 12.dp)
                                .shimmerElement(shimmerBrush, shape = RoundedCornerShape(6.dp))
                        )

                        // 気温バー（スライダーバー）
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .padding(horizontal = 12.dp)
                                .shimmerElement(shimmerBrush, shape = RoundedCornerShape(3.dp))
                        )

                        // 最高気温
                        Box(
                            modifier = Modifier
                                .size(width = 22.dp, height = 12.dp)
                                .shimmerElement(shimmerBrush, shape = RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }

        // 4. Weather Bento Grid Skeleton (2 Columns x 4 Rows)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 左カラムカード
                    Card(
                        shape = RoundedCornerShape(44.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 14.dp)
                                        .shimmerElement(shimmerBrush, shape = RoundedCornerShape(7.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .shimmerElement(shimmerBrush, shape = CircleShape)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(width = 72.dp, height = 36.dp)
                                    .shimmerElement(shimmerBrush, shape = RoundedCornerShape(12.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(12.dp)
                                    .shimmerElement(shimmerBrush, shape = RoundedCornerShape(6.dp))
                            )
                        }
                    }

                    // 右カラムカード
                    Card(
                        shape = RoundedCornerShape(44.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 14.dp)
                                        .shimmerElement(shimmerBrush, shape = RoundedCornerShape(7.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .shimmerElement(shimmerBrush, shape = CircleShape)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(width = 72.dp, height = 36.dp)
                                    .shimmerElement(shimmerBrush, shape = RoundedCornerShape(12.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(12.dp)
                                    .shimmerElement(shimmerBrush, shape = RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
