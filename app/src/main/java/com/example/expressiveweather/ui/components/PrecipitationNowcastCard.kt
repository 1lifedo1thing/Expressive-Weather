package com.example.expressiveweather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.R
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.MinutelyRainPoint
import com.example.expressiveweather.data.PrecipitationNowcast
import com.example.expressiveweather.data.RainStatus
import com.example.expressiveweather.data.WeatherCondition
import dev.chrisbanes.haze.HazeState
import java.util.Locale

/**
 * 降水予測・接近＆終了カウントダウンボックス (Material 3 Expressive 完全準拠)
 *
 * 設計原則:
 * 1. 他の主要セクション (HourlyForecastSection, WeeklyForecastSection) と同一のフロストすりガラス・角丸28dpデザイン
 * 2. 「雨の心配はありません」状態は一切表示しない（降水イベント時のみスマートに出現）
 * 3. アイコンはすべて Microsoft Fluent UI System Icons (ic_fluent_*) で統一
 * 4. 全文表示: ヘッドラインおよび補足テキストを途中で「...」と省略せず全文表示
 * 5. 多言語完全対応: 日本語・英語・簡体字中国語・繁体字中国語に対応
 * 6. カード全体のタップで雨雲レーダーにシームレスに遷移
 */
@Composable
fun PrecipitationNowcastCard(
    nowcast: PrecipitationNowcast?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    condition: WeatherCondition? = null,
    hazeState: HazeState? = null,
    isFrostedGlassEnabled: Boolean = true,
    onCardClick: () -> Unit = {},
    onRadarClick: () -> Unit = {},
    onCycleTestMode: () -> Unit = {}
) {
    // 雨の心配がない状態（DRY）またはnullの場合はカードを描画しない
    if (nowcast == null || nowcast.status == RainStatus.DRY) return

    val language = LocalAppLanguage.current
    val status = nowcast.status
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)
    val isSnowing = condition == WeatherCondition.SNOW

    // Microsoft Fluent UI System Icons のリソース決定（傘・雨雲・雨滴）
    val iconRes = when (status) {
        RainStatus.RAINING -> if (nowcast.currentIntensityMm >= 4.0) R.drawable.ic_fluent_weather_rain_24 else R.drawable.ic_fluent_umbrella_24
        RainStatus.RAIN_APPROACHING -> R.drawable.ic_fluent_umbrella_24
        RainStatus.RAIN_EXPECTED_TONIGHT -> R.drawable.ic_fluent_umbrella_24
        RainStatus.RAIN_EXPECTED_LATER -> R.drawable.ic_fluent_umbrella_24
        RainStatus.DRY -> R.drawable.ic_fluent_weather_sunny_24
    }

    val headlineText = nowcast.localizedHeadline(language)
    val subText = nowcast.localizedSubText(language)
    val radarContentDescription = when (language) {
        AppLanguage.ENGLISH -> "Open precipitation radar"
        AppLanguage.SIMPLIFIED_CHINESE -> "打开降水雷达"
        AppLanguage.TRADITIONAL_CHINESE -> "開啟降水雷達"
        AppLanguage.JAPANESE -> "雨雲レーダーを開く"
    }

    val cardShape = RoundedCornerShape(28.dp)
    Card(
        onClick = onCardClick,
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        interactionSource = remember { MutableInteractionSource() },
        modifier = modifier
            .fillMaxWidth()
            .frostedGlassEffect(hazeState, isFrostedGlassEnabled, shape = cardShape)
            .snowTileAccumulation(
                isSnowing = isSnowing,
                isDark = isDark,
                shape = SnowTileShape.ROUNDED_RECT,
                cornerRadiusDp = 28.dp,
                seed = 103
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp)
        ) {
            // 上段: ヘッダー情報（アイコン・見出し・残り時間/確率）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. M3 Expressive アイコンコンテナ
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.8f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(13.dp))

                // 2. 中央: 自然言語ヘッドラインと補足テキスト (全文を一切省略せず全て表示)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = headlineText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3. 右側: カウントダウン / 降水確率のスマートメトリクス + シェブロン矢印
                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (nowcast.minutesRemaining != null) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val mins = nowcast.minutesRemaining
                            val labelText = when {
                                status == RainStatus.RAIN_APPROACHING -> when (language) {
                                    AppLanguage.ENGLISH -> "Rain starts"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "开始降雨"
                                    AppLanguage.TRADITIONAL_CHINESE -> "開始降雨"
                                    AppLanguage.JAPANESE -> "降り始め"
                                }
                                nowcast.isPeak -> when (language) {
                                    AppLanguage.ENGLISH -> "Peak"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "最强峰值"
                                    AppLanguage.TRADITIONAL_CHINESE -> "最強峰值"
                                    AppLanguage.JAPANESE -> "ピーク"
                                }
                                mins > 90 -> when (language) {
                                    AppLanguage.ENGLISH -> "Duration"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "持续时间"
                                    AppLanguage.TRADITIONAL_CHINESE -> "持續時間"
                                    AppLanguage.JAPANESE -> "継続予想"
                                }
                                else -> when (language) {
                                    AppLanguage.ENGLISH -> "Rain stops"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "预计雨停"
                                    AppLanguage.TRADITIONAL_CHINESE -> "預計雨停"
                                    AppLanguage.JAPANESE -> "降り止み"
                                }
                            }
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val prefixText = if (mins > 90) {
                                    when (language) {
                                        AppLanguage.ENGLISH -> "~"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "约"
                                        AppLanguage.TRADITIONAL_CHINESE -> "約"
                                        AppLanguage.JAPANESE -> "約"
                                    }
                                } else {
                                    when (language) {
                                        AppLanguage.ENGLISH -> "in "
                                        AppLanguage.SIMPLIFIED_CHINESE -> "剩"
                                        AppLanguage.TRADITIONAL_CHINESE -> "剩"
                                        AppLanguage.JAPANESE -> "あと"
                                    }
                                }

                                Text(
                                    text = prefixText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 2.dp, end = 1.dp)
                                )

                                if (mins >= 60) {
                                    val hours = (mins + 29) / 60
                                    val displayVal = if (mins > 180) "$hours+" else "$hours"
                                    val suffixText = when (language) {
                                        AppLanguage.ENGLISH -> "h"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "小时"
                                        AppLanguage.TRADITIONAL_CHINESE -> "小時"
                                        AppLanguage.JAPANESE -> "時間"
                                    }
                                    Text(
                                        text = displayVal,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 21.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = suffixText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 2.dp, start = 1.dp)
                                    )
                                } else {
                                    val suffixText = when (language) {
                                        AppLanguage.ENGLISH -> "m"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "分"
                                        AppLanguage.TRADITIONAL_CHINESE -> "分"
                                        AppLanguage.JAPANESE -> "分"
                                    }
                                    Text(
                                        text = "$mins",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 21.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = suffixText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 2.dp, start = 1.dp)
                                    )
                                }
                            }
                        }
                    } else if (status == RainStatus.RAINING) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val labelText = when (language) {
                                AppLanguage.ENGLISH -> "Duration"
                                AppLanguage.SIMPLIFIED_CHINESE -> "持续时间"
                                AppLanguage.TRADITIONAL_CHINESE -> "持續時間"
                                AppLanguage.JAPANESE -> "継続予想"
                            }
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            val valueText = when (language) {
                                AppLanguage.ENGLISH -> "6h+"
                                AppLanguage.SIMPLIFIED_CHINESE -> "6小时+"
                                AppLanguage.TRADITIONAL_CHINESE -> "6小時+"
                                AppLanguage.JAPANESE -> "6時間+"
                            }
                            Text(
                                text = valueText,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (nowcast.maxProbability > 0) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val probLabel = when (language) {
                                AppLanguage.ENGLISH -> "Rain chance"
                                AppLanguage.SIMPLIFIED_CHINESE -> "降水概率"
                                AppLanguage.TRADITIONAL_CHINESE -> "降水機率"
                                AppLanguage.JAPANESE -> "降水確率"
                            }
                            Text(
                                text = probLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "${nowcast.maxProbability}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 21.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 2.dp, start = 1.dp)
                                )
                            }
                        }
                    }

                    // 右横のアイコン: Microsoft Fluent UI Chevron Right (ic_fluent_chevron_right_24)
                    Icon(
                        painter = painterResource(R.drawable.ic_fluent_chevron_right_24),
                        contentDescription = radarContentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 下段: 降水タイムラインミニチャート（雨が直近60分以内に降る・現在降っている場合にのみ表示）
            // 「明日の午後に雨」などの先々の予報（RAIN_EXPECTED_LATER, RAIN_EXPECTED_TONIGHT, DRY）では60分推移グラフは絶対に表示しない
            val showGraph = (status == RainStatus.RAINING || (status == RainStatus.RAIN_APPROACHING && (nowcast.minutesRemaining ?: 999) <= 60)) &&
                    (nowcast.minutelyPoints.any { it.precipMm > 0.05 } || nowcast.currentIntensityMm > 0.05)

            if (showGraph) {
                Spacer(modifier = Modifier.height(12.dp))
                PrecipitationTimelineChart(
                    nowcast = nowcast,
                    language = language,
                    isDark = isDark
                )
            }
        }
    }
}

/**
 * 降水タイムラインミニ棒グラフ (今後60分間の降水量推移)
 * 各5分間隔の降水量を角丸バー（Bar）で直感的に表示し、降り始め・降り止み・ピーク位置をハイライト
 */
@Composable
fun PrecipitationTimelineChart(
    nowcast: PrecipitationNowcast,
    language: AppLanguage,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val points = remember(nowcast) {
        val validPoints = nowcast.minutelyPoints.filter { it.precipMm >= 0.0 }
        if (validPoints.size >= 5 && validPoints.any { it.precipMm > 0.05 }) {
            validPoints.take(13) // 5分毎 13点 (0分〜60分)
        } else {
            generateFallbackMinutelyPoints(
                status = nowcast.status,
                minutesRemaining = nowcast.minutesRemaining ?: (if (nowcast.status == RainStatus.RAINING) 60 else 25),
                currentIntensity = nowcast.currentIntensityMm,
                peakIntensity = maxOf(nowcast.peakIntensityMm, if (nowcast.status == RainStatus.RAINING) 1.5 else 2.5)
            )
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.5f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val peakPrecip = remember(points, nowcast) {
        maxOf(nowcast.peakIntensityMm, nowcast.currentIntensityMm, points.maxOfOrNull { it.precipMm } ?: 0.0)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // 1. チャート上部ヘッダー（Fluent UI水滴アイコン ＆ タイトル ＆ ピーク降水量バッジ）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chartTitle = when (language) {
                AppLanguage.ENGLISH -> "60-Min Rain Trend"
                AppLanguage.SIMPLIFIED_CHINESE -> "未来60分钟降水趋势"
                AppLanguage.TRADITIONAL_CHINESE -> "未來60分鐘降水趨勢"
                AppLanguage.JAPANESE -> "今後60分間の雨雲推移"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_drop_24),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = chartTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            if (peakPrecip > 0.05) {
                val peakText = when (language) {
                    AppLanguage.ENGLISH -> "Peak ~${String.format(Locale.US, "%.1f", peakPrecip)} mm/h"
                    AppLanguage.SIMPLIFIED_CHINESE -> "最大约 ${String.format(Locale.US, "%.1f", peakPrecip)} mm/h"
                    AppLanguage.TRADITIONAL_CHINESE -> "最大約 ${String.format(Locale.US, "%.1f", peakPrecip)} mm/h"
                    AppLanguage.JAPANESE -> "最大 約${String.format(Locale.US, "%.1f", peakPrecip)} mm/h"
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = primaryColor.copy(alpha = if (isDark) 0.22f else 0.14f),
                    modifier = Modifier.height(18.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = peakText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val textMeasurer = rememberTextMeasurer()

        // 2. 降水量棒グラフ (Canvas)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
        ) {
            val w = size.width
            val h = size.height
            if (points.isEmpty()) return@Canvas

            val maxPrecipScale = maxOf(peakPrecip, 2.5) // 最低スケール 2.5 mm/h
            val count = points.size // 13スロット (0分, 5分, ..., 60分)
            val slotWidth = w / count
            val barWidth = (slotWidth * 0.58f).coerceAtLeast(3.dp.toPx())
            val topPadding = 15.dp.toPx() // てっぺんの降水量数値表示用マージン
            val yBase = h - 3.dp.toPx()
            val availableHeight = h - topPadding - 4.dp.toPx()

            // 1.0 mm/h の傘が必要な基準線 (薄い点線)
            if (maxPrecipScale > 1.0) {
                val y1mm = yBase - (1.0 / maxPrecipScale * availableHeight).toFloat()
                drawLine(
                    color = outlineColor.copy(alpha = 0.35f),
                    start = Offset(0f, y1mm),
                    end = Offset(w, y1mm),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }

            // ベースライン (0 mm/h)
            drawLine(
                color = outlineColor.copy(alpha = 0.45f),
                start = Offset(0f, yBase),
                end = Offset(w, yBase),
                strokeWidth = 1.dp.toPx()
            )

            // イベント発生スロット（降り始め / 降り止みの目標時刻）のインデックス判定
            val minRem = nowcast.minutesRemaining
            val targetIndex = if (minRem != null && minRem in 0..60) {
                (minRem / 5).coerceIn(0, count - 1)
            } else null

            // 各棒（バー）を描画
            points.forEachIndexed { index, point ->
                val centerX = (index + 0.5f) * slotWidth
                val left = centerX - barWidth / 2f
                val clampedPrecip = point.precipMm.coerceIn(0.0, maxPrecipScale)
                val isTargetEvent = index == targetIndex

                if (clampedPrecip > 0.05) {
                    // 降水ありのバー (角丸ピル形状)
                    val barHeight = maxOf(
                        3.5.dp.toPx(),
                        ((clampedPrecip / maxPrecipScale) * availableHeight).toFloat()
                    )
                    val top = yBase - barHeight

                    val barBrush = if (isTargetEvent) {
                        Brush.verticalGradient(
                            colors = listOf(
                                tertiaryColor,
                                primaryColor
                            ),
                            startY = top,
                            endY = yBase
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                primaryColor,
                                primaryColor.copy(alpha = 0.60f)
                            ),
                            startY = top,
                            endY = yBase
                        )
                    }

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // 棒のてっぺんに降水量（mm/h）の数値を描画
                    val labelText = if (point.precipMm >= 10.0) {
                        String.format(Locale.US, "%.0f", point.precipMm)
                    } else {
                        String.format(Locale.US, "%.1f", point.precipMm)
                    }

                    val textLayout = textMeasurer.measure(
                        text = labelText,
                        style = TextStyle(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTargetEvent) tertiaryColor else primaryColor
                        )
                    )
                    val textX = centerX - textLayout.size.width / 2f
                    val indicatorOffset = if (isTargetEvent) 7.dp.toPx() else 2.dp.toPx()
                    val textY = (top - textLayout.size.height - indicatorOffset).coerceAtLeast(0f)

                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(textX, textY)
                    )

                    // イベント対象バーの上部に小さなアクセントインジケーター
                    if (isTargetEvent) {
                        drawCircle(
                            color = tertiaryColor.copy(alpha = 0.45f),
                            radius = 3.5.dp.toPx(),
                            center = Offset(centerX, top - 3.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.8.dp.toPx(),
                            center = Offset(centerX, top - 3.5.dp.toPx())
                        )
                    }
                } else {
                    // 降水なしのバー (極小ピルで時間軸スロットを整然と表示)
                    val pillHeight = 2.dp.toPx()
                    val top = yBase - pillHeight
                    val inactiveColor = if (isTargetEvent) {
                        tertiaryColor.copy(alpha = 0.7f)
                    } else {
                        outlineColor.copy(alpha = if (isDark) 0.25f else 0.35f)
                    }

                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, pillHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // 0mmの棒のてっぺんにも控えめに「0」を表示
                    val zeroTextLayout = textMeasurer.measure(
                        text = "0",
                        style = TextStyle(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal,
                            color = onSurfaceVariant.copy(alpha = if (isDark) 0.38f else 0.48f)
                        )
                    )
                    drawText(
                        textLayoutResult = zeroTextLayout,
                        topLeft = Offset(
                            centerX - zeroTextLayout.size.width / 2f,
                            (top - zeroTextLayout.size.height - 2.dp.toPx()).coerceAtLeast(0f)
                        )
                    )

                    if (isTargetEvent) {
                        drawCircle(
                            color = tertiaryColor,
                            radius = 2.5.dp.toPx(),
                            center = Offset(centerX, top - 4.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. X軸時間ラベル (現在, 15分, 30分, 45分, 60分)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val labelNow = when (language) {
                AppLanguage.ENGLISH -> "Now"
                AppLanguage.SIMPLIFIED_CHINESE -> "现在"
                AppLanguage.TRADITIONAL_CHINESE -> "現在"
                AppLanguage.JAPANESE -> "現在"
            }
            val labelSuffix = when (language) {
                AppLanguage.ENGLISH -> "m"
                AppLanguage.SIMPLIFIED_CHINESE -> "分"
                AppLanguage.TRADITIONAL_CHINESE -> "分"
                AppLanguage.JAPANESE -> "分"
            }
            val timeLabels = listOf(labelNow, "15$labelSuffix", "30$labelSuffix", "45$labelSuffix", "60$labelSuffix")
            timeLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. データソース（情報源）表示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = null,
                tint = primaryColor.copy(alpha = 0.75f),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            val sourceText = when {
                nowcast.isJapan || nowcast.dataSourceName?.contains("JMA") == true || nowcast.dataSourceName?.contains("気象庁") == true -> {
                    when (language) {
                        AppLanguage.ENGLISH -> "Source: Japan Meteorological Agency (JMA)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "数据来源: 日本气象厅 (JMA)"
                        AppLanguage.TRADITIONAL_CHINESE -> "資料來源: 日本氣象廳 (JMA)"
                        AppLanguage.JAPANESE -> "情報源: 気象庁 高解像度降水ナウキャスト"
                    }
                }
                nowcast.isUS || nowcast.dataSourceName?.contains("NOAA") == true || nowcast.dataSourceName?.contains("NWS") == true -> {
                    when (language) {
                        AppLanguage.ENGLISH -> "Source: National Weather Service (NOAA/NWS)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "数据来源: 美国国家气象局 (NOAA/NWS)"
                        AppLanguage.TRADITIONAL_CHINESE -> "資料來源: 美國國家氣象局 (NOAA/NWS)"
                        AppLanguage.JAPANESE -> "情報源: 米国気象局 高解像度数値予報 (NOAA/NWS)"
                    }
                }
                nowcast.isEurope || nowcast.dataSourceName?.contains("ECMWF") == true || nowcast.dataSourceName?.contains("欧州") == true || nowcast.dataSourceName?.contains("ヨーロッパ") == true -> {
                    when (language) {
                        AppLanguage.ENGLISH -> "Source: ECMWF / European Met Services (via Open-Meteo)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "数据来源: 欧洲中期天气预报中心 (ECMWF) / 欧洲各气象局"
                        AppLanguage.TRADITIONAL_CHINESE -> "資料來源: 歐洲中期天氣預報中心 (ECMWF) / 歐洲各氣象局"
                        AppLanguage.JAPANESE -> "情報源: 欧州中期予報センター (ECMWF) / 各気象機関"
                    }
                }
                else -> {
                    when (language) {
                        AppLanguage.ENGLISH -> "Source: ECMWF & NOAA Numerical Models (via Open-Meteo)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "数据来源: ECMWF・NOAA 国际数值气象预报"
                        AppLanguage.TRADITIONAL_CHINESE -> "資料來源: ECMWF・NOAA 國際數值氣象預報"
                        AppLanguage.JAPANESE -> "情報源: ECMWF・NOAA 国際数値気象モデル"
                    }
                }
            }
            Text(
                text = sourceText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    }
}

/**
 * フォールバック用の滑らかな60分間降水プロファイル生成
 */
private fun generateFallbackMinutelyPoints(
    status: RainStatus,
    minutesRemaining: Int,
    currentIntensity: Double,
    peakIntensity: Double
): List<MinutelyRainPoint> {
    val count = 13 // 0, 5, 10, ... 60分
    val result = mutableListOf<MinutelyRainPoint>()
    val now = System.currentTimeMillis()
    val safePeak = maxOf(peakIntensity, currentIntensity, 1.8)

    for (i in 0 until count) {
        val min = i * 5
        val precip = when (status) {
            RainStatus.RAIN_APPROACHING -> {
                if (min < minutesRemaining) {
                    0.0
                } else if (min == minutesRemaining) {
                    1.0
                } else {
                    val afterStart = min - minutesRemaining
                    val progress = (afterStart / 25.0).coerceIn(0.0, 1.0)
                    1.0 + (safePeak - 1.0).coerceAtLeast(0.0) * Math.sin(progress * Math.PI / 2)
                }
            }
            RainStatus.RAINING -> {
                if (min >= minutesRemaining) {
                    0.0
                } else {
                    val remainingRatio = (minutesRemaining - min).toDouble() / minutesRemaining.coerceAtLeast(1)
                    val base = maxOf(currentIntensity, 1.2)
                    (base * remainingRatio).coerceAtLeast(0.0)
                }
            }
            else -> 0.0
        }
        result.add(
            MinutelyRainPoint(
                timeLabel = "${min}m",
                timestampMillis = now + min * 60 * 1000L,
                precipMm = precip
            )
        )
    }
    return result
}
