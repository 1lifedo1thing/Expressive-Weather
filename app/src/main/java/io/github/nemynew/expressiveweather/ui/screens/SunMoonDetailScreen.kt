package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.SunSchedule
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.getLocalizedDayLabel
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.theme.currentAccentPalette
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold
import io.github.nemynew.expressiveweather.ui.components.getDetailCardColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * 太陽（日の出・日の入り・昼の長さ・週間スケジュール）詳細画面
 * Material 3 Expressive 準拠（枠線なし、トナルサーフェス、8言語対応）
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    val solarTrackTitle = when (language) {
        AppLanguage.JAPANESE -> "日の出・日の入り"
        AppLanguage.KOREAN -> "일출 · 일몰"
        AppLanguage.SIMPLIFIED_CHINESE -> "日出 · 日落"
        AppLanguage.TRADITIONAL_CHINESE -> "日出 · 日落"
        AppLanguage.ENGLISH -> "Sunrise & Sunset"
        AppLanguage.SPANISH -> "Amanecer y atardecer"
        AppLanguage.FRENCH -> "Lever et coucher du soleil"
        AppLanguage.PORTUGUESE -> "Nascer e pôr do sol"
        AppLanguage.RUSSIAN -> "Восход и закат"
    }

    val daylightLabel = when (language) {
        AppLanguage.JAPANESE -> "昼の長さ: ${sunSchedule.daylightHours}"
        AppLanguage.KOREAN -> "낮 길이: ${sunSchedule.daylightHours}"
        AppLanguage.SIMPLIFIED_CHINESE -> "昼长: ${sunSchedule.daylightHours}"
        AppLanguage.TRADITIONAL_CHINESE -> "晝長: ${sunSchedule.daylightHours}"
        AppLanguage.ENGLISH -> "Daylight: ${sunSchedule.daylightHours}"
        AppLanguage.SPANISH -> "Luz diurna: ${sunSchedule.daylightHours}"
        AppLanguage.FRENCH -> "Jour : ${sunSchedule.daylightHours}"
        AppLanguage.PORTUGUESE -> "Luz do dia: ${sunSchedule.daylightHours}"
        AppLanguage.RUSSIAN -> "Световой день: ${sunSchedule.daylightHours}"
    }

    val sunriseLabel = strings.sunrise
    val sunsetLabel = strings.sunset

    val weeklySunTitle = when (language) {
        AppLanguage.JAPANESE -> "週間 日の出・日の入り"
        AppLanguage.KOREAN -> "주간 일출·일몰"
        AppLanguage.SIMPLIFIED_CHINESE -> "未来7天日出日落"
        AppLanguage.TRADITIONAL_CHINESE -> "未來7天日出日落"
        AppLanguage.ENGLISH -> "7-Day Sun Schedule"
        AppLanguage.SPANISH -> "Salida y puesta del sol (7 días)"
        AppLanguage.FRENCH -> "Lever et coucher du soleil (7 j)"
        AppLanguage.PORTUGUESE -> "Nascer e pôr do sol (7 dias)"
        AppLanguage.RUSSIAN -> "Восход и закат (7 дней)"
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
        val accentPalette = currentAccentPalette()
        val sunAccent = accentPalette.sun.accent
        val sunInnerDot = accentPalette.sun.container
        val sunsetAccent = accentPalette.sunset.accent

        // ================= 1. ヒーローセクション: 太陽の軌跡 (Solar Arc) =================
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = getDetailCardColor()
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
                            tint = sunAccent,
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

                // 太陽軌跡キャンバス (半円アーク + 太陽タイルと同一の MaterialShapes.Sunny マーカー)
                val targetProgress = sunSchedule.dayProgress.coerceIn(0f, 1f)
                val animProgress = remember { Animatable(0f) }
                val animRotation = remember { Animatable(0f) }
                LaunchedEffect(sunSchedule.dayProgress, sunSchedule.sunrise, sunSchedule.sunset) {
                    animProgress.snapTo(0f)
                    animRotation.snapTo(0f)
                    launch {
                        animProgress.animateTo(
                            targetValue = targetProgress,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f)
                        )
                    }
                    launch {
                        animRotation.animateTo(
                            targetValue = 720f * targetProgress.coerceAtLeast(0.4f),
                            animationSpec = spring(dampingRatio = 0.70f, stiffness = 180f)
                        )
                    }
                }
                val sunnyShape = MaterialShapes.Sunny.toShape()
                val density = LocalDensity.current
                val canvasWidthDp = 280.dp
                val canvasHeightDp = 128.dp
                val markerSizeDp = 26.dp
                val auraSizeDp = 38.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(canvasWidthDp, canvasHeightDp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        val widthPx = with(density) { canvasWidthDp.toPx() }
                        val heightPx = with(density) { canvasHeightDp.toPx() }
                        val arcRadiusX = widthPx / 2f - with(density) { 22.dp.toPx() }
                        val arcRadiusY = heightPx - with(density) { 30.dp.toPx() }
                        val centerX = widthPx / 2f
                        val centerY = heightPx - with(density) { 12.dp.toPx() }
                        val markerSizePx = with(density) { markerSizeDp.toPx() }
                        val auraSizePx = with(density) { auraSizeDp.toPx() }

                        val animatedProgress = animProgress.value
                        val currentAngleRad = Math.PI - (Math.PI * animatedProgress)
                        val sunX = (centerX + arcRadiusX * cos(currentAngleRad)).toFloat()
                        val sunY = (centerY - arcRadiusY * sin(currentAngleRad)).toFloat()

                        val horizonColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                        val dashedArcColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        val activeSunArcColor = sunAccent

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 3.dp.toPx()

                            // 地平線
                            drawLine(
                                color = horizonColor,
                                start = Offset(0f, centerY),
                                end = Offset(size.width, centerY),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                            )

                            // 太陽の軌跡（点線アーク）
                            drawArc(
                                color = dashedArcColor,
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(centerX - arcRadiusX, centerY - arcRadiusY),
                                size = Size(arcRadiusX * 2f, arcRadiusY * 2f),
                                style = Stroke(width = strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                            )

                            // 昼間進行度アーク（Material You）
                            drawArc(
                                color = activeSunArcColor,
                                startAngle = 180f,
                                sweepAngle = 180f * animatedProgress,
                                useCenter = false,
                                topLeft = Offset(centerX - arcRadiusX, centerY - arcRadiusY),
                                size = Size(arcRadiusX * 2f, arcRadiusY * 2f),
                                style = Stroke(width = strokeWidth + 1.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // ふんわり光るオーラ（SunCard タイルと同一）
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = sunX - auraSizePx / 2f
                                    translationY = sunY - auraSizePx / 2f
                                }
                                .size(auraSizeDp)
                            .background(
                                color = sunAccent.copy(alpha = 0.24f),
                                shape = CircleShape
                            )
                        )

                        // MaterialShapes.Sunny 太陽マーカー（SunCard タイルと同一・自転付き）
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = sunX - markerSizePx / 2f
                                    translationY = sunY - markerSizePx / 2f
                                    rotationZ = animRotation.value
                                }
                                .size(markerSizeDp)
                        ) {
                            Surface(
                                shape = sunnyShape,
                                color = sunAccent,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = CircleShape,
                                        color = sunInnerDot,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 日の出・日の入り時刻
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WbSunny,
                                contentDescription = null,
                                tint = sunAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sunriseLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = GoogleSansFlex
                            )
                        }
                        Text(
                            text = sunSchedule.sunrise,
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = GoogleSansFlex
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WbTwilight,
                                contentDescription = null,
                                tint = sunsetAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sunsetLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = GoogleSansFlex
                            )
                        }
                        Text(
                            text = sunSchedule.sunset,
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = GoogleSansFlex
                        )
                    }
                }
            }
        }

        // ================= 2. 7日間 日の出・日の入り予報 =================
        if (dailyForecast.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = getDetailCardColor()
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getLocalizedDayLabel(day.dayLabel, language),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = GoogleSansFlex,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.width(84.dp)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.width(74.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WbSunny,
                                    contentDescription = null,
                                    tint = sunAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = day.sunrise.ifBlank { sunSchedule.sunrise },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = GoogleSansFlex,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.width(74.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WbTwilight,
                                    contentDescription = null,
                                    tint = sunsetAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = day.sunset.ifBlank { sunSchedule.sunset },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = GoogleSansFlex,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
