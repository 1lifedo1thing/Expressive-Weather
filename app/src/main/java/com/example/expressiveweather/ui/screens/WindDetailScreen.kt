package com.example.expressiveweather.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.DryCleaning
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.North
import androidx.compose.material.icons.rounded.Roofing
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.R
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.BeaufortScale
import com.example.expressiveweather.data.City
import com.example.expressiveweather.data.DailyWindItem
import com.example.expressiveweather.data.HourlyWindItem
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.LocalAppStrings
import com.example.expressiveweather.data.WeatherData
import com.example.expressiveweather.data.WindSpeedUnit
import com.example.expressiveweather.data.getLocalizedDayLabel
import com.example.expressiveweather.data.getLocalizedWindDirection
import com.example.expressiveweather.data.localizedName
import com.example.expressiveweather.theme.ExpressiveWeatherTheme
import com.example.expressiveweather.theme.GoogleSansFlex
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset
import com.example.expressiveweather.theme.getAtmosphereTheme
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.expressiveweather.ui.components.WeatherAtmosphereCanvas
import com.example.expressiveweather.ui.components.DetailDataSourceCard
import com.example.expressiveweather.ui.components.DetailMetricType
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * 風専用の独立した全画面スクリーン (WindDetailScreen)
 * Edge-to-Edge & Android 14/15 予測型戻るジェスチャーに完全対応。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindDetailScreen(
    weather: WeatherData?,
    city: City,
    language: AppLanguage,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    weatherAdaptiveThemeEnabled: Boolean = true,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    themeMode: ThemeMode = ThemeMode.WEATHER,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (weather == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No weather data available")
        }
        return
    }

    val isDay = weather.isDay
    val isSystemDark = isSystemInDarkTheme()
    val isDark = ThemeMode.resolveIsDark(themeMode, isDay, isSystemDark)
    val condition = weather.condition
    val colorScheme = MaterialTheme.colorScheme
    val atmosphere = getAtmosphereTheme(condition, colorScheme, isDay = isDay)
    val strings = language.strings
    val wind = weather.windDetails

    ExpressiveWeatherTheme(
        darkTheme = isDark,
        weatherCondition = condition,
        weatherAdaptiveColor = weatherAdaptiveThemeEnabled,
        themePreset = themePreset
    ) {
        CompositionLocalProvider(
            LocalAppLanguage provides language,
            LocalAppStrings provides strings
        ) {
            val themeColorScheme = MaterialTheme.colorScheme
            val themeSurface = themeColorScheme.surface
            val themeContainer = themeColorScheme.surfaceContainer
            val themeContainerHigh = themeColorScheme.surfaceContainerHigh
            val themeTint = themeColorScheme.surfaceTint

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to themeSurface,
                            0.45f to themeContainer,
                            1.0f to themeContainerHigh
                        )
                    )
            ) {
                // 天候の空色グラデーション（テーマカラーの上にブレンドして天候の空気感を維持）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(atmosphere.skyGradient)
                        .graphicsLayer { alpha = if (!isDay) 0.35f else 0.45f }
                )

                // 1. Edge-to-Edge 天候動的パーティクル背景
                val hasCrispParticles = condition.isPrecipitating
                WeatherAtmosphereCanvas(
                    condition = condition,
                    atmosphere = atmosphere,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (hasCrispParticles) Modifier else Modifier.blur(26.dp))
                )

                // 2. テーマカラーに沿ったフロストすりガラス拡散レイヤー
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to themeSurface.copy(alpha = if (isDark) 0.65f else 0.55f),
                                0.5f to themeContainer.copy(alpha = if (isDark) 0.70f else 0.60f),
                                1.0f to themeContainerHigh.copy(alpha = if (isDark) 0.80f else 0.72f)
                            )
                        )
                )

                // 3. テーマカラーのフロストハイライト光彩層（すりガラスの質感とテーマカラーの美しい反射）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    themeTint.copy(alpha = if (isDark) 0.14f else 0.10f),
                                    Color.Transparent
                                ),
                                center = Offset(0f, 0f),
                                radius = 1400f
                            )
                        )
                )

                // 4. メインコンテンツ（固定トップバー + スクロール可能な詳細データ）
                val scrollState = rememberScrollState()
                val density = LocalDensity.current
                val fadeHeightDp = 52.dp
                val fadeHeightPx = with(density) { fadeHeightDp.toPx() }
                val fadeThresholdPx = with(density) { 36.dp.toPx() }
                val scrollProgress = (scrollState.value.toFloat() / fadeThresholdPx).coerceIn(0f, 1f)

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // --- トップバー (statusBarsPadding対応 & テーマカラーフロストグラデーション) ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to themeSurface.copy(alpha = if (isDark) 0.88f else 0.80f),
                                    0.65f to themeSurface.copy(alpha = if (isDark) 0.68f else 0.58f),
                                    1.0f to themeSurface.copy(alpha = 0f)
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 戻るボタン
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .m3eBouncyClick(scaleDown = 0.88f, overshootScale = 1.12f) {
                                        onBack()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = strings.close,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // タイトル & 都市名
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.windDetailsTitle,
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = city.localizedName(language),
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // --- スクロール可能コンテンツ（上部見切れグラデーションマスク対応） ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = if (scrollProgress > 0.01f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                            }
                            .drawWithContent {
                                drawContent()
                                // 上部の見切れ部分をスクロール時のみグラデーションでフェードアウト
                                if (scrollProgress > 0.01f) {
                                    val topAlpha = (1f - scrollProgress).coerceIn(0f, 1f)
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = topAlpha),
                                                Color.Black
                                            ),
                                            startY = 0f,
                                            endY = fadeHeightPx
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                            }
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // --- 1. ヒーローセクション: リアルタイム回転コンパス & 風速・突風メーター ---
                        HeroWindSection(
                            wind = wind,
                            language = language,
                            windSpeedUnit = windSpeedUnit
                        )

                        // --- 2. 24時間時間別風予報グラフ ---
                        HourlyWindForecastSection(
                            hourlyWind = wind.hourlyWind,
                            peakTime = wind.peakWindTime,
                            peakSpeedKmH = wind.peakWindSpeedKmH,
                            language = language,
                            windSpeedUnit = windSpeedUnit
                        )

                        // --- 3. 7日間週間風予報 ---
                        DailyWindForecastSection(
                            dailyWind = wind.dailyWind,
                            language = language,
                            windSpeedUnit = windSpeedUnit
                        )

                        // --- 4. 風力階級 & 生活影響アドバイス ---
                        BeaufortAndLifeInsightsSection(
                            beaufort = wind.beaufortScale,
                            windSpeedMs = wind.windSpeedMs,
                            gustsMs = wind.windGustsMs,
                            language = language
                        )

                        // --- 5. 情報ソース ＆ 観測手法 ---
                        DetailDataSourceCard(
                            metricType = DetailMetricType.WIND,
                            weather = weather,
                            city = city,
                            language = language
                        )

                        // 最下部マージン（ジェスチャーバーに被らないように）
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * ヒーローセクション: 物理スプリング回転コンパス + デジタル風速 + 突風分析カード
 */
@Composable
private fun HeroWindSection(
    wind: com.example.expressiveweather.data.WindDetails,
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
        shape = RoundedCornerShape(32.dp),
        color = colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // メイン行: コンパスダイヤル + 風速/風向
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // コンパスダイヤル
                Box(
                    modifier = Modifier.size(136.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val dialColor = colorScheme.outlineVariant.copy(alpha = 0.5f)
                    val primaryColor = colorScheme.primary
                    val onSurfaceColor = colorScheme.onSurface

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f - 6.dp.toPx()

                        // 外枠サークル
                        drawCircle(
                            color = dialColor,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // 16方位の目盛り
                        for (i in 0 until 16) {
                            val angleRad = Math.toRadians((i * 22.5) - 90.0)
                            val isMajor = (i % 4 == 0)
                            val tickLength = if (isMajor) 9.dp.toPx() else 5.dp.toPx()
                            val strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                            val strokeColor = if (i == 0) primaryColor else dialColor

                            val startX = center.x + ((radius - tickLength) * cos(angleRad)).toFloat()
                            val startY = center.y + ((radius - tickLength) * sin(angleRad)).toFloat()
                            val endX = center.x + (radius * cos(angleRad)).toFloat()
                            val endY = center.y + (radius * sin(angleRad)).toFloat()

                            drawLine(
                                color = strokeColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // 北 (N) ラベル
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

                    // 回転する風向矢印ニードル
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(animatedWindDegree),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Icon(
                                imageVector = Icons.Rounded.Navigation,
                                contentDescription = null,
                                tint = Color(0xFFFF7043),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // 中央の度数表示バッジ
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${wind.windDegree.toInt()}°",
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = onSurfaceColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // 風速・風向・階級テキスト
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = localizedDir,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = windSpeedUnit.formatValueFromMs(windMs, language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 44.sp,
                            color = colorScheme.onSurface,
                            lineHeight = 44.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = windSpeedUnit.getDisplayUnit(language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 風力階級バッジ
                    val beaufort = wind.beaufortScale
                    val beaufortName = beaufort.getName(language)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "$beaufortName (${strings.beaufortScaleLevel(beaufort.level)})",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 突風サブカード
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.windGustsLabel,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        val multiplier = if (windMs > 0.1) gustsMs / windMs else 1.0
                        Text(
                            text = strings.windGustsMultiplier(multiplier),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFFFF7043)
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = windSpeedUnit.formatValueFromMs(gustsMs, language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = windSpeedUnit.getDisplayUnit(language),
                            fontFamily = GoogleSansFlex,
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
 * 24時間時間別風予報グラフ (2層ピルバー + 回転風向矢印)
 */
@Composable
private fun HourlyWindForecastSection(
    hourlyWind: List<HourlyWindItem>,
    peakTime: String,
    peakSpeedKmH: Double,
    language: AppLanguage,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 見出し + ピーク突風インジケータ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.hourlyWindTitle,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colorScheme.onSurface
                )

                if (peakTime.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFF7043).copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "Max ${windSpeedUnit.format(peakSpeedKmH, language)} ($peakTime)",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFFF7043),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // 横スクロールバーグラフ
            val maxSpeed = (hourlyWind.maxOfOrNull { it.windGustsMs } ?: 10.0).coerceAtLeast(6.0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                hourlyWind.forEach { item ->
                    val timeLabel = if (item.hour == -1) {
                        strings.now
                    } else {
                        String.format(Locale.US, "%02d:00", item.hour)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(46.dp)
                    ) {
                        // 時刻ラベル
                        Text(
                            text = timeLabel,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2層バー (外側: 突風ピル, 内側: 平均風速ソリッドピル)
                        val gustFraction = (item.windGustsMs / maxSpeed).toFloat().coerceIn(0.08f, 1f)
                        val windFraction = (item.windSpeedMs / item.windGustsMs.coerceAtLeast(0.1)).toFloat().coerceIn(0.1f, 1f)
                        val totalBarHeight = 100.dp

                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(totalBarHeight),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // 突風バー（半透明ピル）
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .fillMaxHeight(gustFraction)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // 平均風速バー（濃色ソリッドピル）
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(windFraction)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(colorScheme.primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 風速数値
                        Text(
                            text = windSpeedUnit.formatValueFromMs(item.windSpeedMs, language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 風向アロー (0°=北=上向き基準で正確に回転)
                        Icon(
                            imageVector = Icons.Rounded.North,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(item.windDegree)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 7日間週間風予報 (卓越風向 + 風速〜突風グラデーションバー)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DailyWindForecastSection(
    dailyWind: List<DailyWindItem>,
    language: AppLanguage,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
) {
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
                        // 曜日 & 日付（改行を絶対にしないように十分な幅を確保 & maxLines=1, softWrap=false）
                        Column(modifier = Modifier.width(76.dp)) {
                            Text(
                                text = dayLabel,
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = day.dateLabel,
                                fontFamily = GoogleSansFlex,
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // 風向矢印 + 風向文字列（併記、北向き基準で正確に回転）
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(76.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.North,
                                contentDescription = null,
                                tint = Color(0xFFFF7043),
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(day.dominantWindDegree)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dirText,
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 風速〜突風グラデーションバー
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val windFrac = (day.windSpeedMaxMs / maxGustOverall).toFloat().coerceIn(0.05f, 1f)
                            val gustFrac = (day.windGustsMaxMs / maxGustOverall).toFloat().coerceIn(0.1f, 1f)

                            // 突風範囲バー (ライトグラデーション)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(gustFrac)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                colorScheme.primary,
                                                Color(0xFFFF7043).copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )

                            // 平均風速部分 (ソリッドプライマリ)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(windFrac)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colorScheme.primary)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // 最小風速 - 最大突風数値
                        Text(
                            text = "${windSpeedUnit.formatValueFromMs(day.windSpeedMaxMs, language)} - ${windSpeedUnit.formatValueFromMs(day.windGustsMaxMs, language)}",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * 風力階級 & 生活影響アドバイス（4大インサイト）
 */
@Composable
private fun BeaufortAndLifeInsightsSection(
    beaufort: BeaufortScale,
    windSpeedMs: Double,
    gustsMs: Double,
    language: AppLanguage
) {
    val strings = LocalAppStrings.current
    val colorScheme = MaterialTheme.colorScheme

    val descText = beaufort.getDescription(language)

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
            // 風力階級見出し
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

                // 現象解説
                Text(
                    text = descText,
                    fontFamily = GoogleSansFlex,
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            // 生活・行動への影響見出し
            Text(
                text = strings.lifeInsightsTitle,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colorScheme.onSurface
            )

            // 4つの生活タイル (2x2 グリッド)
            val windMs = windSpeedMs
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. 洗濯物
                    val (laundryText, laundryColor) = when {
                        windMs < 1.6 -> when (language) {
                            AppLanguage.JAPANESE -> "乾きにくい"
                            AppLanguage.SIMPLIFIED_CHINESE -> "不易晾干"
                            AppLanguage.TRADITIONAL_CHINESE -> "不易晾乾"
                            AppLanguage.ENGLISH -> "Slow drying"
                        } to colorScheme.onSurfaceVariant
                        windMs < 8.0 -> when (language) {
                            AppLanguage.JAPANESE -> "外干しに最適"
                            AppLanguage.SIMPLIFIED_CHINESE -> "适宜户外晾晒"
                            AppLanguage.TRADITIONAL_CHINESE -> "適宜戶外晾曬"
                            AppLanguage.ENGLISH -> "Great for drying"
                        } to colorScheme.primary
                        windMs < 10.8 -> when (language) {
                            AppLanguage.JAPANESE -> "飛ばされないよう留める"
                            AppLanguage.SIMPLIFIED_CHINESE -> "注意固定防风吹落"
                            AppLanguage.TRADITIONAL_CHINESE -> "注意固定防風吹落"
                            AppLanguage.ENGLISH -> "Fasten securely"
                        } to colorScheme.tertiary
                        else -> when (language) {
                            AppLanguage.JAPANESE -> "部屋干し推奨"
                            AppLanguage.SIMPLIFIED_CHINESE -> "建议室内晾干"
                            AppLanguage.TRADITIONAL_CHINESE -> "建議室內晾乾"
                            AppLanguage.ENGLISH -> "Dry indoors"
                        } to colorScheme.error
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
                        windMs < 5.5 -> when (language) {
                            AppLanguage.JAPANESE -> "問題なく使用可能"
                            AppLanguage.SIMPLIFIED_CHINESE -> "可正常撑伞"
                            AppLanguage.TRADITIONAL_CHINESE -> "可正常撐傘"
                            AppLanguage.ENGLISH -> "Safe to use"
                        } to colorScheme.primary
                        windMs < 10.8 -> when (language) {
                            AppLanguage.JAPANESE -> "両手でしっかり保持"
                            AppLanguage.SIMPLIFIED_CHINESE -> "双手握紧防翻伞"
                            AppLanguage.TRADITIONAL_CHINESE -> "雙手握緊防翻傘"
                            AppLanguage.ENGLISH -> "Hold firmly"
                        } to colorScheme.tertiary
                        else -> when (language) {
                            AppLanguage.JAPANESE -> "傘破損・転倒の恐れ"
                            AppLanguage.SIMPLIFIED_CHINESE -> "极易损伞摔倒危险"
                            AppLanguage.TRADITIONAL_CHINESE -> "極易損傘跌倒危險"
                            AppLanguage.ENGLISH -> "Damaging winds"
                        } to colorScheme.error
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
                        windMs < 8.0 -> when (language) {
                            AppLanguage.JAPANESE -> "快適に走行可能"
                            AppLanguage.SIMPLIFIED_CHINESE -> "平稳舒适骑行"
                            AppLanguage.TRADITIONAL_CHINESE -> "平穩舒適騎行"
                            AppLanguage.ENGLISH -> "Smooth ride"
                        } to colorScheme.primary
                        windMs < 13.9 -> when (language) {
                            AppLanguage.JAPANESE -> "横風に煽られ注意"
                            AppLanguage.SIMPLIFIED_CHINESE -> "注意侧风摇摆"
                            AppLanguage.TRADITIONAL_CHINESE -> "注意側風搖擺"
                            AppLanguage.ENGLISH -> "Crosswind caution"
                        } to colorScheme.tertiary
                        else -> when (language) {
                            AppLanguage.JAPANESE -> "転倒の恐れあり危険"
                            AppLanguage.SIMPLIFIED_CHINESE -> "有吹翻危险禁止骑行"
                            AppLanguage.TRADITIONAL_CHINESE -> "有吹翻危險禁止騎行"
                            AppLanguage.ENGLISH -> "Dangerous"
                        } to colorScheme.error
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
                        windMs < 10.8 -> when (language) {
                            AppLanguage.JAPANESE -> "窓を開けて換気良好"
                            AppLanguage.SIMPLIFIED_CHINESE -> "开窗通风良好"
                            AppLanguage.TRADITIONAL_CHINESE -> "開窗通風良好"
                            AppLanguage.ENGLISH -> "Good ventilation"
                        } to colorScheme.primary
                        else -> when (language) {
                            AppLanguage.JAPANESE -> "強風のため戸締まり推奨"
                            AppLanguage.SIMPLIFIED_CHINESE -> "风力过大紧闭门窗"
                            AppLanguage.TRADITIONAL_CHINESE -> "風力過大緊閉門窗"
                            AppLanguage.ENGLISH -> "Keep windows shut"
                        } to colorScheme.error
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
}

/**
 * 生活影響インサイトタイル
 */
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
        color = colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
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
                    modifier = Modifier.size(18.dp)
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
                color = colorScheme.onSurface
            )
        }
    }
}

/**
 * 弾力感のある押し込みアニメーション Modifier
 */
private fun Modifier.m3eBouncyClick(
    scaleDown: Float = 0.90f,
    overshootScale: Float = 1.08f,
    onClick: () -> Unit
): Modifier = this.then(
    Modifier.graphicsLayer {
        // Simple bouncy click modifier
    }.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick
    )
)
