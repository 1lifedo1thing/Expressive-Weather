package com.example.expressiveweather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.data.DailyForecast
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.LocalAppStrings
import com.example.expressiveweather.data.TemperatureUnit
import com.example.expressiveweather.data.getLocalizedDayLabel
import com.example.expressiveweather.data.getLocalizedLabel
import com.example.expressiveweather.theme.WeatherAtmosphereTheme

/**
 * 週間天気予報セクション (WeeklyForecastSection):
 * - 枠線（border）を完全排除。
 * - 外枠カード、テキスト、折れ線グラフ、ドット、アイコン等の全配色を端末のデバイステーマカラー (MaterialTheme.colorScheme) に完全対応。
 * - 大画面時は全幅に7日間を均等配分し、空白を解消。
 */
@Composable
fun WeeklyForecastSection(
    dailyList: List<DailyForecast>,
    temperatureUnit: TemperatureUnit,
    atmosphere: WeatherAtmosphereTheme,
    onDayClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    condition: com.example.expressiveweather.data.WeatherCondition? = null,
    isFrostedGlassEnabled: Boolean = true,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    isColorful: Boolean = true
) {
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, atmosphere.isDark, MaterialTheme.colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, atmosphere.isDark)
    val isSnowing = condition == com.example.expressiveweather.data.WeatherCondition.SNOW

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .frostedGlassEffect(hazeState, isFrostedGlassEnabled)
            .snowTileAccumulation(
                isSnowing = isSnowing,
                isDark = atmosphere.isDark,
                shape = SnowTileShape.ROUNDED_RECT,
                cornerRadiusDp = 32.dp,
                seed = 102
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            DailyChartContent(
                dailyList = dailyList,
                temperatureUnit = temperatureUnit,
                onDayClick = onDayClick,
                isColorful = isColorful
            )
        }
    }
}

@Composable
private fun DailyChartContent(
    dailyList: List<DailyForecast>,
    temperatureUnit: TemperatureUnit,
    onDayClick: (Int) -> Unit = {},
    isColorful: Boolean = true
) {
    if (dailyList.isEmpty()) return

    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    // デバイステーマカラー
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val highTempColor = MaterialTheme.colorScheme.primary
    val lowTempColor = MaterialTheme.colorScheme.secondary
    val guideColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val dotSurfaceColor = MaterialTheme.colorScheme.surface

    val overallMaxTemp = dailyList.maxOfOrNull { it.maxTempC } ?: 30
    val overallMinTemp = dailyList.minOfOrNull { it.minTempC } ?: 10
    val tempDiff = (overallMaxTemp - overallMinTemp).coerceAtLeast(1)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val totalAvailableWidth = maxWidth
        val horizontalPaddingDp = 16.dp
        val innerWidth = (totalAvailableWidth - horizontalPaddingDp * 2).coerceAtLeast(0.dp)
        val n = dailyList.size
        val minColWidth = 58.dp
        val isSpacious = (innerWidth / n) >= minColWidth
        val columnWidthDp = if (isSpacious) (innerWidth / n) else minColWidth
        val columnWidthPx = with(density) { columnWidthDp.toPx() }
        val chartHeightDp = if (isSpacious) 58.dp else 50.dp

        val dayFontSize = if (isSpacious) 13.5.sp else 12.5.sp
        val dateFontSize = if (isSpacious) 12.sp else 11.sp
        val dayIconSize = if (isSpacious) 32.dp else 28.dp
        val maxTempFontSize = if (isSpacious) 16.5.sp else 15.sp
        val minTempFontSize = if (isSpacious) 15.5.sp else 14.sp
        val rainTextFontSize = if (isSpacious) 12.sp else 11.sp
        val nightIconSize = if (isSpacious) 26.dp else 24.dp

        val scrollContainerModifier = if (isSpacious) {
            Modifier.fillMaxWidth()
        } else {
            Modifier
                .fillMaxWidth()
                .horizontalScrollFade(scrollState, fadeWidth = 24.dp)
                .horizontalScroll(scrollState)
        }

        Row(modifier = scrollContainerModifier) {
            Spacer(modifier = Modifier.width(horizontalPaddingDp))
            Column(modifier = Modifier.width(columnWidthDp * n)) {
                // 行 1: 曜日、日付、昼天気アイコン、最高気温
                Row(modifier = Modifier.fillMaxWidth()) {
                    dailyList.forEachIndexed { index, day ->
                        Column(
                            modifier = Modifier
                                .width(columnWidthDp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onDayClick(index) }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val dayText = if (index == 0) {
                                strings.today
                            } else {
                                getLocalizedDayLabel(day.dayLabel, language)
                            }
                            Text(
                                text = dayText,
                                fontSize = dayFontSize,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium,
                                color = onSurface
                            )
                            Text(
                                text = day.dateLabel,
                                fontSize = dateFontSize,
                                fontWeight = FontWeight.Normal,
                                color = onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(
                                    WeatherIconHelper.getConditionDrawable(day.condition, isDay = true)
                                ),
                                contentDescription = day.condition.getLocalizedLabel(language),
                                tint = WeatherIconHelper.getConditionIconTint(day.condition),
                                modifier = Modifier.size(dayIconSize)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${temperatureUnit.convert(day.maxTempC)}°",
                                fontSize = maxTempFontSize,
                                fontWeight = FontWeight.Bold,
                                color = if (isColorful) Color(0xFFFF5722) else onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 行 2: 連続温度変化 折れ線グラフ (Canvas)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeightDp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeightDp)
                    ) {
                        val nPoints = dailyList.size
                        if (nPoints < 2) return@Canvas

                        val paddingVertical = 14.dp.toPx()
                        val usableHeight = size.height - paddingVertical * 2

                        fun tempToY(tempC: Int): Float {
                            val fraction = (tempC - overallMinTemp).toFloat() / tempDiff
                            return size.height - paddingVertical - (fraction * usableHeight)
                        }

                        val maxPoints = dailyList.mapIndexed { index, day ->
                            Offset(
                                x = index * columnWidthPx + columnWidthPx / 2f,
                                y = tempToY(day.maxTempC)
                            )
                        }
                        val minPoints = dailyList.mapIndexed { index, day ->
                            Offset(
                                x = index * columnWidthPx + columnWidthPx / 2f,
                                y = tempToY(day.minTempC)
                            )
                        }

                        val maxPath = Path().apply {
                            moveTo(maxPoints[0].x, maxPoints[0].y)
                            for (i in 0 until nPoints - 1) {
                                val p0 = maxPoints[i]
                                val p1 = maxPoints[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }

                        val minPath = Path().apply {
                            moveTo(minPoints[0].x, minPoints[0].y)
                            for (i in 0 until nPoints - 1) {
                                val p0 = minPoints[i]
                                val p1 = minPoints[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }

                        // 温度帯エリアの塗りつぶし（テーマカラーグラデーション）
                        val areaPath = Path().apply {
                            moveTo(maxPoints[0].x, maxPoints[0].y)
                            for (i in 0 until nPoints - 1) {
                                val p0 = maxPoints[i]
                                val p1 = maxPoints[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                            lineTo(minPoints[nPoints - 1].x, minPoints[nPoints - 1].y)
                            for (i in nPoints - 1 downTo 1) {
                                val p0 = minPoints[i]
                                val p1 = minPoints[i - 1]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                            close()
                        }

                        val areaBrush = if (isColorful) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF7043).copy(alpha = 0.30f),
                                    Color(0xFF00B0FF).copy(alpha = 0.12f)
                                ),
                                startY = 0f,
                                endY = size.height
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    highTempColor.copy(alpha = 0.20f),
                                    lowTempColor.copy(alpha = 0.05f)
                                ),
                                startY = 0f,
                                endY = size.height
                            )
                        }
                        drawPath(path = areaPath, brush = areaBrush)

                        // 垂直破線ガイド
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        for (i in 0 until nPoints) {
                            drawLine(
                                color = guideColor,
                                start = maxPoints[i],
                                end = minPoints[i],
                                strokeWidth = 1.2.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }

                        // 最高気温ライン（カラフル時: 鮮烈な暖色サンバーストグラデーション）
                        val maxBrush = if (isColorful) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF3D00),
                                    Color(0xFFFF9100),
                                    Color(0xFFFFD600)
                                )
                            )
                        } else {
                            androidx.compose.ui.graphics.SolidColor(highTempColor)
                        }
                        drawPath(
                            path = maxPath,
                            brush = maxBrush,
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 最低気温ライン（カラフル時: 鮮烈なクールアクアグラデーション）
                        val minBrush = if (isColorful) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF2979FF),
                                    Color(0xFF7C4DFF)
                                )
                            )
                        } else {
                            androidx.compose.ui.graphics.SolidColor(lowTempColor)
                        }
                        drawPath(
                            path = minPath,
                            brush = minBrush,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // データポイントのドット
                        for (i in 0 until nPoints) {
                            val maxDotColor = if (isColorful) Color(0xFFFF5722) else highTempColor
                            val minDotColor = if (isColorful) Color(0xFF00B0FF) else lowTempColor

                            // 最高気温ドット
                            drawCircle(
                                color = dotSurfaceColor,
                                radius = 4.5.dp.toPx(),
                                center = maxPoints[i]
                            )
                            drawCircle(
                                color = maxDotColor,
                                radius = 3.dp.toPx(),
                                center = maxPoints[i]
                            )

                            // 最低気温ドット
                            drawCircle(
                                color = dotSurfaceColor,
                                radius = 4.dp.toPx(),
                                center = minPoints[i]
                            )
                            drawCircle(
                                color = minDotColor,
                                radius = 2.5.dp.toPx(),
                                center = minPoints[i]
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 行 3: 最低気温、降水確率、夜間天気アイコン
                Row(modifier = Modifier.fillMaxWidth()) {
                    dailyList.forEachIndexed { index, day ->
                        Column(
                            modifier = Modifier
                                .width(columnWidthDp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onDayClick(index) }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${temperatureUnit.convert(day.minTempC)}°",
                                fontSize = minTempFontSize,
                                fontWeight = FontWeight.Medium,
                                color = if (isColorful) Color(0xFF0288D1) else onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (day.precipProb > 0) {
                                val precipColor = if (isColorful) Color(0xFF00B0FF) else highTempColor
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(com.example.expressiveweather.R.drawable.ic_fluent_drop_24),
                                        contentDescription = strings.precipitation,
                                        tint = precipColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(1.dp))
                                    Text(
                                        text = "${day.precipProb}%",
                                        fontSize = rainTextFontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = precipColor
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Icon(
                                painter = androidx.compose.ui.res.painterResource(
                                    WeatherIconHelper.getConditionDrawable(day.condition, isDay = false)
                                ),
                                contentDescription = day.condition.getLocalizedLabel(language),
                                tint = onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.size(nightIconSize)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(horizontalPaddingDp))
        }
    }
}
