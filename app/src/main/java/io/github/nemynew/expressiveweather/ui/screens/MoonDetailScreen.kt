package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.MoonPhaseCalculator
import io.github.nemynew.expressiveweather.data.MoonPhaseSimulator
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.getLocalizedDayLabel
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.theme.currentAccentPalette
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold
import io.github.nemynew.expressiveweather.ui.components.MoonPhaseDisc
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 月の満ち欠け・月の出／月の入り専用詳細画面。
 * 月相・月の出没アーク・7日間の月相＆出没予定を表示する。
 */
@Composable
fun MoonDetailScreen(
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
    val context = LocalContext.current
    val isDark = ThemeMode.resolveIsDark(themeMode, weather?.isDay ?: true, isSystemInDarkTheme())

    LaunchedEffect(Unit) {
        MoonPhaseSimulator.ensureLoaded(context)
    }
    val simulatedPhase by MoonPhaseSimulator.overridePhaseFraction.collectAsState()
    val simulatedSouthern by MoonPhaseSimulator.overrideSouthernHemisphere.collectAsState()

    val nowMillis = remember(weather?.fetchedAtMillis) { System.currentTimeMillis() }
    val moonInfo = remember(language, simulatedPhase, nowMillis) {
        MoonPhaseCalculator.calculate(
            language = language,
            nowMillis = nowMillis,
            overridePhaseFraction = simulatedPhase
        )
    }
    val isSouthernHemisphere = MoonPhaseSimulator.resolveIsSouthernHemisphere(
        cityLatitude = city.latitude,
        overrideValue = simulatedSouthern
    )

    val heroInitialRange = remember(moonInfo.phaseFraction) {
        MoonPhaseCalculator.resolveEntranceAnimationRange(moonInfo.phaseFraction)
    }
    val heroAnimPhase = remember { Animatable(heroInitialRange.first) }
    val heroMoonScale = remember { Animatable(0.74f) }
    val heroMoonTilt = remember { Animatable(-10f) }

    LaunchedEffect(city.id, city.latitude, weather?.fetchedAtMillis, isSouthernHemisphere, simulatedPhase) {
        val (startPhase, endPhase) = MoonPhaseCalculator.resolveEntranceAnimationRange(moonInfo.phaseFraction)
        val moonSpring = spring<Float>(dampingRatio = 0.64f, stiffness = 250f)

        heroAnimPhase.snapTo(startPhase)
        heroMoonScale.snapTo(0.74f)
        heroMoonTilt.snapTo(if (isSouthernHemisphere) 10f else -10f)

        launch {
            heroAnimPhase.animateTo(
                targetValue = endPhase,
                animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
            )
        }
        launch { heroMoonScale.animateTo(1f, moonSpring) }
        launch { heroMoonTilt.animateTo(0f, moonSpring) }
    }

    val moonSchedule = remember(city.latitude, city.longitude, weather?.utcOffsetSeconds, nowMillis) {
        MoonPhaseCalculator.calculateMoonSchedule(
            latitude = city.latitude,
            longitude = city.longitude,
            utcOffsetSeconds = weather?.utcOffsetSeconds,
            nowMillis = nowMillis
        )
    }

    val weeklyMoonSchedule = remember(city.latitude, city.longitude, language, weather?.utcOffsetSeconds, nowMillis) {
        MoonPhaseCalculator.calculateWeeklyMoonSchedule(
            latitude = city.latitude,
            longitude = city.longitude,
            language = language,
            utcOffsetSeconds = weather?.utcOffsetSeconds,
            nowMillis = nowMillis,
            days = 7
        )
    }

    val dailyForecast = weather?.dailyForecast ?: emptyList()

    val moonPhaseTitle = when (language) {
        AppLanguage.JAPANESE -> "月の満ち欠け"
        AppLanguage.KOREAN -> "달의 위상"
        AppLanguage.SIMPLIFIED_CHINESE -> "月相盈亏"
        AppLanguage.TRADITIONAL_CHINESE -> "月相盈虧"
        AppLanguage.ENGLISH -> "Moon Phase"
        AppLanguage.SPANISH -> "Fase lunar"
        AppLanguage.FRENCH -> "Phase de la lune"
        AppLanguage.PORTUGUESE -> "Fase da lua"
    }

    val lunarTrackTitle = when (language) {
        AppLanguage.JAPANESE -> "月の出・月の入り"
        AppLanguage.KOREAN -> "월출 · 월몰"
        AppLanguage.SIMPLIFIED_CHINESE -> "月出 · 月落"
        AppLanguage.TRADITIONAL_CHINESE -> "月出 · 月沒"
        AppLanguage.ENGLISH -> "Moonrise & Moonset"
        AppLanguage.SPANISH -> "Salida y puesta lunar"
        AppLanguage.FRENCH -> "Lever et coucher de lune"
        AppLanguage.PORTUGUESE -> "Nascer e pôr da lua"
    }

    val southernBadgeLabel = when (language) {
        AppLanguage.JAPANESE -> "南半球表示"
        AppLanguage.KOREAN -> "남반구 기준"
        AppLanguage.SIMPLIFIED_CHINESE -> "南半球视角"
        AppLanguage.TRADITIONAL_CHINESE -> "南半球視角"
        AppLanguage.ENGLISH -> "Southern Hemisphere"
        AppLanguage.SPANISH -> "Hemisferio sur"
        AppLanguage.FRENCH -> "Hémisphère sud"
        AppLanguage.PORTUGUESE -> "Hemisfério sul"
    }

    val moonriseLabel = when (language) {
        AppLanguage.JAPANESE -> "月の出"
        AppLanguage.KOREAN -> "월출"
        AppLanguage.SIMPLIFIED_CHINESE -> "月出"
        AppLanguage.TRADITIONAL_CHINESE -> "月出"
        AppLanguage.ENGLISH -> "Moonrise"
        AppLanguage.SPANISH -> "Salida lunar"
        AppLanguage.FRENCH -> "Lever de lune"
        AppLanguage.PORTUGUESE -> "Nascer da lua"
    }

    val moonsetLabel = when (language) {
        AppLanguage.JAPANESE -> "月の入り"
        AppLanguage.KOREAN -> "월몰"
        AppLanguage.SIMPLIFIED_CHINESE -> "月落"
        AppLanguage.TRADITIONAL_CHINESE -> "月沒"
        AppLanguage.ENGLISH -> "Moonset"
        AppLanguage.SPANISH -> "Puesta lunar"
        AppLanguage.FRENCH -> "Coucher de lune"
        AppLanguage.PORTUGUESE -> "Pôr da lua"
    }

    val horizonStatusText = if (moonSchedule.isAboveHorizon) {
        val altRounded = moonSchedule.altitudeDeg.roundToInt()
        when (language) {
            AppLanguage.JAPANESE -> "地平線上 · 高度 ${altRounded}°"
            AppLanguage.KOREAN -> "지평선 위 · 고도 ${altRounded}°"
            AppLanguage.SIMPLIFIED_CHINESE -> "地平线上 · 高度 ${altRounded}°"
            AppLanguage.TRADITIONAL_CHINESE -> "地平線上 · 高度 ${altRounded}°"
            AppLanguage.ENGLISH -> "Above horizon · ${altRounded}°"
            AppLanguage.SPANISH -> "Sobre el horizonte · ${altRounded}°"
            AppLanguage.FRENCH -> "Au-dessus · ${altRounded}°"
            AppLanguage.PORTUGUESE -> "Acima do horizonte · ${altRounded}°"
        }
    } else {
        when (language) {
            AppLanguage.JAPANESE -> "地平線下"
            AppLanguage.KOREAN -> "지평선 아래"
            AppLanguage.SIMPLIFIED_CHINESE -> "地平线下"
            AppLanguage.TRADITIONAL_CHINESE -> "地平線下"
            AppLanguage.ENGLISH -> "Below horizon"
            AppLanguage.SPANISH -> "Bajo el horizonte"
            AppLanguage.FRENCH -> "Sous l'horizon"
            AppLanguage.PORTUGUESE -> "Abaixo do horizonte"
        }
    }

    val weeklyMoonTitle = when (language) {
        AppLanguage.JAPANESE -> "週間 月の出・月の入り・月相"
        AppLanguage.KOREAN -> "주간 월출·월몰 및 달 위상"
        AppLanguage.SIMPLIFIED_CHINESE -> "未来7天月出月落与月相"
        AppLanguage.TRADITIONAL_CHINESE -> "未來7天月出月沒與月相"
        AppLanguage.ENGLISH -> "7-Day Moon Schedule"
        AppLanguage.SPANISH -> "Calendario lunar (7 días)"
        AppLanguage.FRENCH -> "Calendrier lunaire (7 j)"
        AppLanguage.PORTUGUESE -> "Calendário lunar (7 dias)"
    }

    val noEventNote = when (language) {
        AppLanguage.JAPANESE -> "※月の周期は約24時間50分のため、暦日によっては月の出または月の入りがない日（--:--）があります。"
        AppLanguage.KOREAN -> "※ 달의 공전 주기는 약 24시간 50분이므로 날짜에 따라 월출 또는 월몰이 없는 날(--:--)이 있습니다."
        AppLanguage.SIMPLIFIED_CHINESE -> "※ 月球公转周期约为24小时50分，因此部分日期可能没有月出或月落（--:--）。"
        AppLanguage.TRADITIONAL_CHINESE -> "※ 月球公轉週期約為24小時50分，因此部分日期可能沒有月出或月沒（--:--）。"
        AppLanguage.ENGLISH -> "Because a lunar day is ~24h 50m, some calendar dates have no moonrise or moonset (--:--)."
        AppLanguage.SPANISH -> "Dado que el día lunar dura ~24h 50m, algunos días no tienen salida o puesta lunar (--:--)."
        AppLanguage.FRENCH -> "Un jour lunaire durant ~24h 50m, certains jours n'ont pas de lever ou de coucher (--:--)."
        AppLanguage.PORTUGUESE -> "Como o dia lunar dura ~24h 50m, alguns dias não têm nascer ou pôr da lua (--:--)."
    }

    DetailScaffold(
        title = strings.moonPhase,
        city = city,
        weather = weather,
        language = language,
        metricType = DetailMetricType.MOON,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val accentPalette = currentAccentPalette()
        val moonAccent = accentPalette.moon.accent
        val moonContainer = accentPalette.moon.container
        val onMoonContainer = accentPalette.moon.onContainer
        val moonriseAccent = accentPalette.sun.accent
        // ================= 1. ヒーロー: 月の満ち欠け =================
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            ),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.NightsStay,
                            contentDescription = null,
                            tint = moonAccent,
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
                    if (isSouthernHemisphere) {
                        Surface(
                            shape = CircleShape,
                            color = moonContainer
                        ) {
                            Text(
                                text = southernBadgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = onMoonContainer,
                                fontFamily = GoogleSansFlex,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoonPhaseDisc(
                        phaseFraction = heroAnimPhase.value,
                        isSouthernHemisphere = isSouthernHemisphere,
                        isColorful = true,
                        isDark = isDark,
                        modifier = Modifier
                            .size(88.dp)
                            .graphicsLayer {
                                scaleX = heroMoonScale.value
                                scaleY = heroMoonScale.value
                                rotationZ = heroMoonTilt.value
                            }
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    val ageLabel = when (language) {
                        AppLanguage.JAPANESE -> "月齢: 約 ${moonInfo.ageDays} 日（輝面比 ${moonInfo.illumination}%）"
                        AppLanguage.KOREAN -> "월령: 약 ${moonInfo.ageDays}일 (조도 ${moonInfo.illumination}%)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "月龄: 约 ${moonInfo.ageDays} 天（受光比 ${moonInfo.illumination}%）"
                        AppLanguage.TRADITIONAL_CHINESE -> "月齡: 約 ${moonInfo.ageDays} 天（受光比 ${moonInfo.illumination}%）"
                        AppLanguage.ENGLISH -> "Moon Age: ~${moonInfo.ageDays}d (${moonInfo.illumination}% lit)"
                        AppLanguage.SPANISH -> "Edad lunar: ~${moonInfo.ageDays} d (${moonInfo.illumination}% ilum.)"
                        AppLanguage.FRENCH -> "Âge lunaire : ~${moonInfo.ageDays} j (${moonInfo.illumination} %)"
                        AppLanguage.PORTUGUESE -> "Idade lunar: ~${moonInfo.ageDays} d (${moonInfo.illumination}%)"
                    }

                    val nextFullMoonLabel = when (language) {
                        AppLanguage.JAPANESE -> "次回満月まで: 約 ${moonInfo.daysToFullMoon} 日"
                        AppLanguage.KOREAN -> "다음 보름달까지: 약 ${moonInfo.daysToFullMoon}일"
                        AppLanguage.SIMPLIFIED_CHINESE -> "距下次满月: 约 ${moonInfo.daysToFullMoon} 天"
                        AppLanguage.TRADITIONAL_CHINESE -> "距下次滿月: 約 ${moonInfo.daysToFullMoon} 天"
                        AppLanguage.ENGLISH -> "Next Full Moon: in ~${moonInfo.daysToFullMoon}d"
                        AppLanguage.SPANISH -> "Próxima luna llena: en ~${moonInfo.daysToFullMoon} d"
                        AppLanguage.FRENCH -> "Prochaine pleine lune : ~${moonInfo.daysToFullMoon} j"
                        AppLanguage.PORTUGUESE -> "Próxima lua cheia: em ~${moonInfo.daysToFullMoon} d"
                    }

                    val nextNewMoonLabel = when (language) {
                        AppLanguage.JAPANESE -> "次回新月まで: 約 ${moonInfo.daysToNewMoon} 日"
                        AppLanguage.KOREAN -> "다음 신월까지: 약 ${moonInfo.daysToNewMoon}일"
                        AppLanguage.SIMPLIFIED_CHINESE -> "距下次新月: 约 ${moonInfo.daysToNewMoon} 天"
                        AppLanguage.TRADITIONAL_CHINESE -> "距下次新月: 約 ${moonInfo.daysToNewMoon} 天"
                        AppLanguage.ENGLISH -> "Next New Moon: in ~${moonInfo.daysToNewMoon}d"
                        AppLanguage.SPANISH -> "Próxima luna nueva: en ~${moonInfo.daysToNewMoon} d"
                        AppLanguage.FRENCH -> "Prochaine nouvelle lune : ~${moonInfo.daysToNewMoon} j"
                        AppLanguage.PORTUGUESE -> "Próxima lua nova: em ~${moonInfo.daysToNewMoon} d"
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = moonInfo.phaseName,
                            style = MaterialTheme.typography.titleLarge,
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nextFullMoonLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = GoogleSansFlex,
                            color = moonAccent
                        )
                        Text(
                            text = nextNewMoonLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = GoogleSansFlex,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ================= 2. 月の出・月の入りアーク =================
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NightsStay,
                            contentDescription = null,
                            tint = moonAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lunarTrackTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSansFlex
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (moonSchedule.isAboveHorizon) {
                            moonContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    ) {
                        Text(
                            text = horizonStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (moonSchedule.isAboveHorizon) onMoonContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = GoogleSansFlex,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 月の軌跡キャンバス（半円アーク + 月タイルと同一の Clover8Leaf + MoonPhaseDisc マーカー）
                val targetArcProgress = if (moonSchedule.isAboveHorizon) moonSchedule.transitProgress.coerceIn(0f, 1f) else 0f
                val moonArcAnim = remember { Animatable(0f) }
                LaunchedEffect(moonSchedule.isAboveHorizon, moonSchedule.transitProgress) {
                    moonArcAnim.snapTo(0f)
                    if (moonSchedule.isAboveHorizon) {
                        moonArcAnim.animateTo(
                            targetValue = targetArcProgress,
                            animationSpec = spring(dampingRatio = 0.68f, stiffness = 170f)
                        )
                    }
                }
                val animatedProgress = moonArcAnim.value

                val arcTrackColor = colorScheme.outlineVariant.copy(alpha = 0.65f)
                val arcActiveColor = moonAccent
                val horizonColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                val density = LocalDensity.current
                val canvasWidthDp = 280.dp
                val canvasHeightDp = 128.dp
                val markerAuraSizeDp = 36.dp

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
                        val centerY = heightPx - with(density) { 14.dp.toPx() }
                        val auraSizePx = with(density) { markerAuraSizeDp.toPx() }

                        val currentAngleRad = PI - (PI * animatedProgress)
                        val moonX = if (moonSchedule.isAboveHorizon) {
                            (centerX + arcRadiusX * cos(currentAngleRad)).toFloat()
                        } else {
                            centerX - arcRadiusX
                        }
                        val moonY = if (moonSchedule.isAboveHorizon) {
                            (centerY - arcRadiusY * sin(currentAngleRad)).toFloat()
                        } else {
                            centerY + with(density) { 2.dp.toPx() }
                        }

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

                            // 月の全軌跡（点線アーク）
                            drawArc(
                                color = arcTrackColor,
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(centerX - arcRadiusX, centerY - arcRadiusY),
                                size = Size(arcRadiusX * 2f, arcRadiusY * 2f),
                                style = Stroke(
                                    width = strokeWidth,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                                )
                            )

                            if (moonSchedule.isAboveHorizon) {
                                // 通過済みアーク（Material Indigo）
                                drawArc(
                                    color = arcActiveColor,
                                    startAngle = 180f,
                                    sweepAngle = 180f * animatedProgress,
                                    useCenter = false,
                                    topLeft = Offset(centerX - arcRadiusX, centerY - arcRadiusY),
                                    size = Size(arcRadiusX * 2f, arcRadiusY * 2f),
                                    style = Stroke(width = strokeWidth + 1.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        // 月タイルと同一の MoonPhaseDisc 位置マーカー（MaterialShape 背景なしのすっきり表示）
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = moonX - auraSizePx / 2f
                                    translationY = moonY - auraSizePx / 2f
                                    alpha = if (moonSchedule.isAboveHorizon) 1f else 0.72f
                                }
                                .size(markerAuraSizeDp),
                            contentAlignment = Alignment.Center
                        ) {
                            MoonPhaseDisc(
                                phaseFraction = heroAnimPhase.value,
                                isSouthernHemisphere = isSouthernHemisphere,
                                isColorful = true,
                                isDark = isDark,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 今日の月の出・月の入り時刻
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                tint = moonriseAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = moonriseLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = GoogleSansFlex
                            )
                        }
                        Text(
                            text = moonSchedule.moonrise ?: "--:--",
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = GoogleSansFlex
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = moonAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = moonsetLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = GoogleSansFlex
                            )
                        }
                        Text(
                            text = moonSchedule.moonset ?: "--:--",
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = GoogleSansFlex
                        )
                    }
                }
            }
        }

        // ================= 3. 7日間 月の出・月の入り・月相カレンダー =================
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
                        tint = moonAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = weeklyMoonTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                weeklyMoonSchedule.forEachIndexed { index, item ->
                    val dayText = dailyForecast.getOrNull(index)?.dayLabel?.takeIf { it.isNotBlank() }?.let {
                        getLocalizedDayLabel(it, language)
                    } ?: fallbackDayLabel(index, language, nowMillis, weather?.utcOffsetSeconds)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = GoogleSansFlex,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.width(76.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(62.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(moonContainer.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                MoonPhaseDisc(
                                    phaseFraction = item.moonPhase.phaseFraction,
                                    isSouthernHemisphere = isSouthernHemisphere,
                                    isColorful = true,
                                    isDark = isDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.moonPhase.illumination}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = GoogleSansFlex,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.width(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.width(66.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = moonriseLabel,
                                tint = moonriseAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = item.moonrise ?: "--:--",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Medium,
                                fontFamily = GoogleSansFlex,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.width(66.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = moonsetLabel,
                                tint = moonAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = item.moonset ?: "--:--",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Medium,
                                fontFamily = GoogleSansFlex,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (weeklyMoonSchedule.any { it.moonrise == null || it.moonset == null }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = noEventNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = GoogleSansFlex
                    )
                }
            }
        }
    }
}

private fun fallbackDayLabel(
    dayOffset: Int,
    language: AppLanguage,
    nowMillis: Long,
    utcOffsetSeconds: Int?
): String {
    if (dayOffset == 0) return getLocalizedDayLabel("今日", language)
    if (dayOffset == 1) return getLocalizedDayLabel("明日", language)
    val offsetMs = (utcOffsetSeconds ?: (TimeZone.getDefault().getOffset(nowMillis) / 1000)) * 1000L
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = nowMillis + offsetMs + dayOffset * 86_400_000L
    }
    val dow = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "日"
        Calendar.MONDAY -> "月"
        Calendar.TUESDAY -> "火"
        Calendar.WEDNESDAY -> "水"
        Calendar.THURSDAY -> "木"
        Calendar.FRIDAY -> "金"
        else -> "土"
    }
    return getLocalizedDayLabel(dow, language)
}
