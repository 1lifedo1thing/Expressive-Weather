package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DryCleaning
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.North
import androidx.compose.material.icons.rounded.Roofing
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.BeaufortScale
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.DailyWindItem
import io.github.nemynew.expressiveweather.data.HourlyWindItem
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.getLocalizedDayLabel
import io.github.nemynew.expressiveweather.data.getLocalizedWindDirection
import io.github.nemynew.expressiveweather.data.localizedName
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.ui.components.m3eBouncyClick
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * 風の詳細情報モーダルシート (WindDetailSheet)
 * Open-Meteoから取得可能な地上10mの風情報を網羅し、Material 3 Expressiveで美しく可視化
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindDetailSheet(
    weather: WeatherData,
    city: City,
    onDismiss: () -> Unit,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val colorScheme = MaterialTheme.colorScheme
    val wind = weather.windDetails

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = colorScheme.surfaceContainerLow,
        tonalElevation = 8.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.5.dp)
                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.35f), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- 1. ヘッダー (タイトル + 都市名 + 閉じるボタン) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Air,
                                contentDescription = null,
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = strings.windDetailsTitle,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = city.localizedName(language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(38.dp)
                        .m3eBouncyClick(scaleDown = 0.85f, overshootScale = 1.15f) {
                            onDismiss()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = strings.close,
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // --- 2. ヒーローセクション: リアルタイム回転コンパス & 風速・突風メーター ---
            WindHeroCompassCard(
                wind = wind,
                language = language,
                windSpeedUnit = windSpeedUnit
            )

            // --- 3. 24時間 時間別 風速・突風・風向タイムライングラフ ---
            HourlyWindGraphCard(
                hourlyWind = wind.hourlyWind,
                peakTime = wind.peakWindTime,
                peakSpeedKmH = wind.peakWindSpeedKmH,
                language = language,
                windSpeedUnit = windSpeedUnit
            )

            // --- 4. 7日間 週間風予報 ---
            DailyWindForecastCard(
                dailyWind = wind.dailyWind,
                language = language,
                windSpeedUnit = windSpeedUnit
            )

            // --- 5. ビューフォート風力階級 & 生活影響アドバイス ---
            BeaufortAndLivingInsightsCard(
                beaufort = wind.beaufortScale,
                windMs = wind.windSpeedMs,
                gustsMs = wind.windGustsMs,
                language = language
            )
        }
    }
}

/**
 * ヒーローセクション：回転方位コンパスと特大デジタル風速・突風
 */
@Composable
private fun WindHeroCompassCard(
    wind: io.github.nemynew.expressiveweather.data.WindDetails,
    language: AppLanguage,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = LocalAppStrings.current
    val windMs = wind.windSpeedMs
    val gustsMs = wind.windGustsMs
    val localizedDir = getLocalizedWindDirection(wind.windDegree, language)

    val animatedWindDegree by animateFloatAsState(
        targetValue = wind.windDegree,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HeroWindCompass"
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左側: 回転コンパス
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 外周のコンパスリング
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.5.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2f
                        drawCircle(
                            color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                            radius = radius,
                            style = Stroke(width = strokeWidth)
                        )

                        // 4方位の小マーカー (N, E, S, W)
                        val cardinalAngles = listOf(0f, 90f, 180f, 270f)
                        val markerLen = 8.dp.toPx()
                        cardinalAngles.forEach { deg ->
                            val rad = Math.toRadians(deg.toDouble() - 90.0)
                            val startX = (center.x + (radius - markerLen) * cos(rad)).toFloat()
                            val startY = (center.y + (radius - markerLen) * sin(rad)).toFloat()
                            val endX = (center.x + radius * cos(rad)).toFloat()
                            val endY = (center.y + radius * sin(rad)).toFloat()
                            drawLine(
                                color = if (deg == 0f) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = if (deg == 0f) 3.dp.toPx() else 1.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // 北ラベル
                    Text(
                        text = "N",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    )

                    // 回転する方位針
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(animatedWindDegree),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Navigation,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                        )
                    }

                    // 中央に角度度数
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${wind.windDegree.toInt()}°",
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }

                // 右側: 特大デジタル風速 + 階級バッジ
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = localizedDir,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = windSpeedUnit.formatValueFromMs(windMs, language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 44.sp,
                            lineHeight = 44.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = windSpeedUnit.getDisplayUnit(language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // ビューフォート階級名バッジ
                    val beaufortName = if (language == AppLanguage.ENGLISH) wind.beaufortScale.nameEn else wind.beaufortScale.nameJa
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "$beaufortName (${strings.beaufortScaleLevel(wind.beaufortScale.level)})",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // 下部: 突風（最大瞬間風速）分析バッジ
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.windGustsLabel,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        val multiplier = if (windMs > 0.1) gustsMs / windMs else 1.3
                        Text(
                            text = strings.windGustsMultiplier(multiplier),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = windSpeedUnit.formatValueFromMs(gustsMs, language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = windSpeedUnit.getDisplayUnit(language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 24時間 時間別 風速・突風・風向グラフ
 */
@Composable
private fun HourlyWindGraphCard(
    hourlyWind: List<HourlyWindItem>,
    peakTime: String,
    peakSpeedKmH: Double,
    language: AppLanguage,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
) {
    if (hourlyWind.isEmpty()) return

    val strings = LocalAppStrings.current
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.hourlyWindTitle,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colorScheme.onSurface
                )

                if (peakTime.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colorScheme.errorContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Max ${windSpeedUnit.format(peakSpeedKmH, language)} ($peakTime)",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 横スクロール可能な24時間グラフ & 風向アイコン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                val maxSpeed = (hourlyWind.maxOfOrNull { it.windGustsMs } ?: 10.0).coerceAtLeast(6.0)

                hourlyWind.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.width(48.dp)
                    ) {
                        // 時刻ラベル
                        Text(
                            text = item.timeLabel,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )

                        // 風速バー（下部が平均風速、上部が突風の2層ピル）
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(110.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // 突風バー（薄いアクセント）
                            val gustFraction = (item.windGustsMs / maxSpeed).toFloat().coerceIn(0.08f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(gustFraction)
                                    .background(colorScheme.primary.copy(alpha = 0.35f))
                            )

                            // 平均風速バー（濃いプライマリ）
                            val avgFraction = (item.windSpeedMs / maxSpeed).toFloat().coerceIn(0.05f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(avgFraction)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(colorScheme.primary)
                            )
                        }

                        // 風速数値
                        Text(
                            text = windSpeedUnit.formatValueFromMs(item.windSpeedMs, language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = colorScheme.onSurface
                        )

                        // 風向矢印（その時間の角度に回転）
                        Icon(
                            imageVector = Icons.Rounded.North,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(item.windDegree)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 7日間 週間風予報カード
 */
@Composable
private fun DailyWindForecastCard(
    dailyWind: List<DailyWindItem>,
    language: AppLanguage,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
) {
    if (dailyWind.isEmpty()) return

    val strings = LocalAppStrings.current
    val colorScheme = MaterialTheme.colorScheme
    val maxGustOverall = (dailyWind.maxOfOrNull { it.windGustsMaxMs } ?: 12.0).coerceAtLeast(6.0)

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.dailyWindTitle,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = colorScheme.onSurface
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                dailyWind.forEach { day ->
                    val dayLabel = getLocalizedDayLabel(day.dayLabel, language)
                    val dirText = getLocalizedWindDirection(day.dominantWindDegree, language)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 曜日 & 日付
                        Column(modifier = Modifier.width(60.dp)) {
                            Text(
                                text = dayLabel,
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = day.dateLabel,
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        // 卓越風向矢印 + テキスト
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.width(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.North,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(day.dominantWindDegree)
                            )
                            Text(
                                text = dirText,
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 風速バー（最大風速〜最大突風）
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(colorScheme.surfaceContainerHighest)
                        ) {
                            val gustFrac = (day.windGustsMaxMs / maxGustOverall).toFloat().coerceIn(0.1f, 1f)
                            val speedFrac = (day.windSpeedMaxMs / maxGustOverall).toFloat().coerceIn(0.05f, gustFrac)

                            // 突風バー
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(gustFrac)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.35f))
                            )

                            // 最大風速バー
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(speedFrac)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(colorScheme.primary)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // 数値表示
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.width(68.dp)
                        ) {
                            Text(
                                text = "${windSpeedUnit.formatValueFromMs(day.windSpeedMaxMs, language)} - ${windSpeedUnit.formatValueFromMs(day.windGustsMaxMs, language)}",
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ビューフォート風力階級 & 生活影響アドバイス（4大インサイト）
 */
@Composable
private fun BeaufortAndLivingInsightsCard(
    beaufort: BeaufortScale,
    windMs: Double,
    gustsMs: Double,
    language: AppLanguage
) {
    val strings = LocalAppStrings.current
    val colorScheme = MaterialTheme.colorScheme

    val descText = if (language == AppLanguage.ENGLISH) beaufort.descEn else beaufort.descJa

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ビューフォート階級見出し
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = strings.beaufortScaleTitle,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colorScheme.onSurface
                )

                // 0〜12のゲージプログレスバー
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorScheme.surfaceContainerHighest)
                ) {
                    val progress = (beaufort.level / 12f).coerceIn(0.05f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        colorScheme.primary,
                                        if (beaufort.level >= 7) colorScheme.error else colorScheme.primary
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = descText,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 4大生活影響インサイト
            Text(
                text = strings.lifeInsightsTitle,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colorScheme.onSurface
            )

            // 洗濯物・傘・自転車・換気の4枚カード（2x2グリッド）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. 洗濯物
                val (laundryText, laundryColor) = when {
                    windMs < 5.5 -> (if (language == AppLanguage.ENGLISH) "Safe to hang dry" else "外干しに最適") to colorScheme.primary
                    windMs < 10.8 -> (if (language == AppLanguage.ENGLISH) "Secure with clips" else "飛ばされないよう固定") to colorScheme.tertiary
                    else -> (if (language == AppLanguage.ENGLISH) "Dry indoors" else "部屋干し推奨") to colorScheme.error
                }
                InsightTile(
                    title = strings.laundryTitle,
                    status = laundryText,
                    icon = Icons.Rounded.DryCleaning,
                    accentColor = laundryColor,
                    modifier = Modifier.weight(1f)
                )

                // 2. 傘
                val (umbrellaText, umbrellaColor) = when {
                    windMs < 8.0 -> (if (language == AppLanguage.ENGLISH) "Normal use" else "問題なく使用可能") to colorScheme.primary
                    windMs < 10.8 -> (if (language == AppLanguage.ENGLISH) "Caution of inside-out" else "おちょこに注意") to colorScheme.tertiary
                    else -> (if (language == AppLanguage.ENGLISH) "Raincoat needed" else "傘の破損注意・カッパ推奨") to colorScheme.error
                }
                InsightTile(
                    title = strings.umbrellaTitle,
                    status = umbrellaText,
                    icon = Icons.Rounded.Umbrella,
                    accentColor = umbrellaColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 3. 自転車・バイク
                val (cyclingText, cyclingColor) = when {
                    windMs < 8.0 -> (if (language == AppLanguage.ENGLISH) "Smooth ride" else "快適に走行可能") to colorScheme.primary
                    windMs < 13.9 -> (if (language == AppLanguage.ENGLISH) "Crosswind caution" else "横風に煽られ注意") to colorScheme.tertiary
                    else -> (if (language == AppLanguage.ENGLISH) "Dangerous" else "転倒の恐れあり危険") to colorScheme.error
                }
                InsightTile(
                    title = strings.cyclingTitle,
                    status = cyclingText,
                    icon = Icons.AutoMirrored.Rounded.DirectionsBike,
                    accentColor = cyclingColor,
                    modifier = Modifier.weight(1f)
                )

                // 4. 窓・換気
                val (outdoorText, outdoorColor) = when {
                    windMs < 10.8 -> (if (language == AppLanguage.ENGLISH) "Good ventilation" else "窓を開けて換気良好") to colorScheme.primary
                    else -> (if (language == AppLanguage.ENGLISH) "Keep windows shut" else "強風のため戸締まり推奨") to colorScheme.error
                }
                InsightTile(
                    title = strings.outdoorTitle,
                    status = outdoorText,
                    icon = Icons.Rounded.Roofing,
                    accentColor = outdoorColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InsightTile(
    title: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceContainerHighest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = status,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = colorScheme.onSurface,
                lineHeight = 17.sp
            )
        }
    }
}
