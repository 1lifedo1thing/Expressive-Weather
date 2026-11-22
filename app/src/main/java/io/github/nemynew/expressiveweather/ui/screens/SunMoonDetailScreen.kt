package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.SunSchedule
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.getLocalizedDayLabel
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * 太陽・月（満ち欠け / 日の出・日の入り）詳細画面
 * Material 3 Expressive 準拠（枠線なし、トナルサーフェス、4言語対応）
 */
@Composable
fun SunMoonDetailScreen(
    weather: WeatherData?,
    city: City,
    language: AppLanguage,
    weatherAdaptiveThemeEnabled: Boolean = true,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    themeMode: ThemeMode = ThemeMode.WEATHER,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = language.strings
    val sunSchedule = weather?.sunSchedule ?: SunSchedule(
        sunrise = "05:32",
        sunset = "17:48",
        dayProgress = 0.5f,
        daylightHours = "12h 16m"
    )
    val dailyForecast = weather?.dailyForecast ?: emptyList()

    val moonInfo = calculateMoonPhase(language, weather?.utcOffsetSeconds)

    val solarTrackTitle = when (language) {
        AppLanguage.JAPANESE -> "太陽の運行と日照"
        AppLanguage.SIMPLIFIED_CHINESE -> "太阳运行与日照轨迹"
        AppLanguage.TRADITIONAL_CHINESE -> "太陽運行與日照軌跡"
        AppLanguage.ENGLISH -> "Solar Movement & Daylight"
    }

    val daylightLabel = when (language) {
        AppLanguage.JAPANESE -> "昼の長さ: ${sunSchedule.daylightHours}"
        AppLanguage.SIMPLIFIED_CHINESE -> "昼长: ${sunSchedule.daylightHours}"
        AppLanguage.TRADITIONAL_CHINESE -> "晝長: ${sunSchedule.daylightHours}"
        AppLanguage.ENGLISH -> "Daylight: ${sunSchedule.daylightHours}"
    }

    val sunriseLabel = strings.sunrise
    val sunsetLabel = strings.sunset

    val moonPhaseTitle = when (language) {
        AppLanguage.JAPANESE -> "月の満ち欠け"
        AppLanguage.SIMPLIFIED_CHINESE -> "月相盈亏"
        AppLanguage.TRADITIONAL_CHINESE -> "月相盈虧"
        AppLanguage.ENGLISH -> "Moon Phases"
    }

    val weeklySunTitle = when (language) {
        AppLanguage.JAPANESE -> "週間 日の出・日の入り"
        AppLanguage.SIMPLIFIED_CHINESE -> "未来7天日出日落"
        AppLanguage.TRADITIONAL_CHINESE -> "未來7天日出日落"
        AppLanguage.ENGLISH -> "7-Day Sun Schedule"
    }

    DetailScaffold(
        title = strings.sun,
        city = city,
        weather = weather,
        language = language,
        metricType = DetailMetricType.SUN_MOON,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        // ================= 1. ヒーローセクション: 太陽の軌跡 (Solar Arc) =================
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            ),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = solarTrackTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSansFlex
                        )
                    }

                    Text(
                        text = daylightLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 太陽軌跡キャンバス (半円アークと地平線)
                val animatedProgress by animateFloatAsState(
                    targetValue = sunSchedule.dayProgress.coerceIn(0f, 1f),
                    animationSpec = tween(1200, easing = FastOutSlowInEasing),
                    label = "SunArcProgress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(280.dp, 120.dp)) {
                        val strokeWidth = 3.dp.toPx()
                        val arcRadiusX = size.width / 2 - 20.dp.toPx()
                        val arcRadiusY = size.height - 20.dp.toPx()
                        val centerX = size.width / 2
                        val centerY = size.height - 10.dp.toPx()

                        // 地平線
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.35f),
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )

                        // 太陽の軌跡（点線アーク）
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.45f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(centerX - arcRadiusX, centerY - arcRadiusY),
                            size = Size(arcRadiusX * 2, arcRadiusY * 2),
                            style = Stroke(width = strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                        )

                        // 昼間進行度アーク（金色）
                        val currentAngleRad = Math.PI - (Math.PI * animatedProgress)
                        drawArc(
                            color = Color(0xFFFFB300),
                            startAngle = 180f,
                            sweepAngle = 180f * animatedProgress,
                            useCenter = false,
                            topLeft = Offset(centerX - arcRadiusX, centerY - arcRadiusY),
                            size = Size(arcRadiusX * 2, arcRadiusY * 2),
                            style = Stroke(width = strokeWidth + 1.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 太陽の位置マーカー
                        val sunX = (centerX + arcRadiusX * cos(currentAngleRad)).toFloat()
                        val sunY = (centerY - arcRadiusY * sin(currentAngleRad)).toFloat()

                        drawCircle(
                            color = Color(0xFFFFB300).copy(alpha = 0.28f),
                            radius = 16.dp.toPx(),
                            center = Offset(sunX, sunY)
                        )
                        drawCircle(
                            color = Color(0xFFFFA000),
                            radius = 8.dp.toPx(),
                            center = Offset(sunX, sunY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(sunX, sunY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 日の出・日の入り時刻
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = sunriseLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = GoogleSansFlex
                        )
                        Text(
                            text = sunSchedule.sunrise,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = GoogleSansFlex
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = sunsetLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = GoogleSansFlex
                        )
                        Text(
                            text = sunSchedule.sunset,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = GoogleSansFlex
                        )
                    }
                }
            }
        }

        // ================= 2. 月の満ち欠け (Moon Phase) セクション =================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            ),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.NightsStay,
                        contentDescription = null,
                        tint = Color(0xFF7E57C2),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = moonPhaseTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 月のグラフィック
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF263238)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = null,
                            tint = Color(0xFFFFE082),
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    val ageLabel = when (language) {
                        AppLanguage.JAPANESE -> "月齢: 約 ${moonInfo.ageDays} 日 (輝面比 ${moonInfo.illumination}%)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "月龄: 约 ${moonInfo.ageDays} 天 (受光比 ${moonInfo.illumination}%)"
                        AppLanguage.TRADITIONAL_CHINESE -> "月齡: 約 ${moonInfo.ageDays} 天 (受光比 ${moonInfo.illumination}%)"
                        AppLanguage.ENGLISH -> "Moon Age: ~${moonInfo.ageDays} days (${moonInfo.illumination}% lit)"
                    }

                    val nextFullMoonLabel = when (language) {
                        AppLanguage.JAPANESE -> "次回満月まで: 約 ${moonInfo.daysToFullMoon} 日"
                        AppLanguage.SIMPLIFIED_CHINESE -> "距离下个满月: 约 ${moonInfo.daysToFullMoon} 天"
                        AppLanguage.TRADITIONAL_CHINESE -> "距離下個滿月: 約 ${moonInfo.daysToFullMoon} 天"
                        AppLanguage.ENGLISH -> "Next Full Moon: in ~${moonInfo.daysToFullMoon} days"
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = moonInfo.phaseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSansFlex,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ageLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = GoogleSansFlex,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nextFullMoonLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = GoogleSansFlex,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ================= 3. 7日間 日の出・日の入り予報 =================
        if (dailyForecast.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = weeklySunTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSansFlex
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    dailyForecast.take(7).forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getLocalizedDayLabel(day.dayLabel, language),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = GoogleSansFlex,
                                modifier = Modifier.widthIn(min = 68.dp)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.WbSunny,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = day.sunrise.ifBlank { sunSchedule.sunrise },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = GoogleSansFlex
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.WbTwilight,
                                    contentDescription = null,
                                    tint = Color(0xFFFF7043),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = day.sunset.ifBlank { sunSchedule.sunset },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = GoogleSansFlex
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class MoonPhaseInfo(
    val phaseName: String,
    val ageDays: String,
    val illumination: Int,
    val daysToFullMoon: Int
)

/**
 * 国際標準天文月相計算（29.530588853日 朔望月周期 / J2000.0 新月エポック 2000-01-06 18:14 UTC）
 */
private fun calculateMoonPhase(language: AppLanguage, utcOffsetSeconds: Int? = null): MoonPhaseInfo {
    val synodicMonthDays = 29.530588853
    val referenceNewMoonEpochMs = 947182440_000L // 2000-01-06T18:14:00Z
    val nowMs = System.currentTimeMillis()
    val elapsedDays = (nowMs - referenceNewMoonEpochMs).toDouble() / 86_400_000.0
    val ageFloat = ((elapsedDays % synodicMonthDays) + synodicMonthDays) % synodicMonthDays
    val phaseFraction = ageFloat / synodicMonthDays
    val illuminationPct = ((1.0 - cos(2.0 * Math.PI * phaseFraction)) * 50.0)
        .let { kotlin.math.round(it).toInt().coerceIn(0, 100) }
    val halfCycle = synodicMonthDays / 2.0
    val daysToFull = if (ageFloat <= halfCycle) {
        kotlin.math.round(halfCycle - ageFloat).toInt().coerceAtLeast(0)
    } else {
        kotlin.math.round(synodicMonthDays - ageFloat + halfCycle).toInt()
    }
    val age = kotlin.math.round(ageFloat).toInt() % 30

    val (nameJa, nameZhHans, nameZhHant, nameEn) = when {
        age in 0..1 || age >= 29 -> Tuple4("新月", "新月", "新月", "New Moon")
        age in 2..6 -> Tuple4("三日月 (夕月)", "峨眉月", "蛾眉月", "Waxing Crescent")
        age in 7..8 -> Tuple4("上弦の月", "上弦月", "上弦月", "First Quarter")
        age in 9..13 -> Tuple4("満ちていく月", "盈凸月", "盈凸月", "Waxing Gibbous")
        age in 14..16 -> Tuple4("満月", "满月", "滿月", "Full Moon")
        age in 17..21 -> Tuple4("欠けていく月", "亏凸月", "虧凸月", "Waning Gibbous")
        age in 22..23 -> Tuple4("下弦の月", "下弦月", "下弦月", "Third Quarter")
        else -> Tuple4("明け方の三日月", "残月", "殘月", "Waning Crescent")
    }

    val localizedName = when (language) {
        AppLanguage.JAPANESE -> nameJa
        AppLanguage.SIMPLIFIED_CHINESE -> nameZhHans
        AppLanguage.TRADITIONAL_CHINESE -> nameZhHant
        AppLanguage.ENGLISH -> nameEn
    }

    return MoonPhaseInfo(
        phaseName = localizedName,
        ageDays = String.format(Locale.US, "%.1f", ageFloat),
        illumination = illuminationPct,
        daysToFullMoon = daysToFull
    )
}

private data class Tuple4<A, B, C, D>(
    val a: A, val b: B, val c: C, val d: D
)
