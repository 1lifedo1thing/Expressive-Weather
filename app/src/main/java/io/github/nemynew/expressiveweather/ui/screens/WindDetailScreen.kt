package io.github.nemynew.expressiveweather.ui.screens

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
import androidx.compose.material3.MaterialShapes
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
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.BeaufortScale
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.DailyWindItem
import io.github.nemynew.expressiveweather.data.HourlyWindItem
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.windFlowRotationDegrees
import io.github.nemynew.expressiveweather.data.getLocalizedDayLabel
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.data.getLocalizedWindDirection
import io.github.nemynew.expressiveweather.data.localizedName
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.theme.currentAccentPalette
import androidx.compose.foundation.isSystemInDarkTheme
import io.github.nemynew.expressiveweather.ui.components.DetailAtmosphereBackdrop
import io.github.nemynew.expressiveweather.ui.components.DetailDataSourceCard
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.getDetailCardColor
import io.github.nemynew.expressiveweather.ui.components.m3eBouncyClick
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
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "天気データを取得できませんでした"
                        AppLanguage.KOREAN -> "날씨 데이터를 불러올 수 없습니다"
                        AppLanguage.SIMPLIFIED_CHINESE -> "无法获取天气数据"
                        AppLanguage.TRADITIONAL_CHINESE -> "無法取得天氣資料"
                        AppLanguage.ENGLISH -> "Weather data is currently unavailable"
                        AppLanguage.SPANISH -> "Los datos meteorológicos no están disponibles"
                        AppLanguage.FRENCH -> "Les données météo ne sont pas disponibles"
                        AppLanguage.PORTUGUESE -> "Os dados do tempo não estão disponíveis"
                        AppLanguage.RUSSIAN -> "Данные о погоде временно недоступны"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.material3.TextButton(onClick = onBack) {
                    Text(
                        text = when (language) {
                            AppLanguage.JAPANESE -> "戻る"
                            AppLanguage.KOREAN -> "뒤로"
                            AppLanguage.SIMPLIFIED_CHINESE -> "返回"
                            AppLanguage.TRADITIONAL_CHINESE -> "返回"
                            AppLanguage.ENGLISH -> "Back"
                            AppLanguage.SPANISH -> "Volver"
                            AppLanguage.FRENCH -> "Retour"
                            AppLanguage.PORTUGUESE -> "Voltar"
                            AppLanguage.RUSSIAN -> "Назад"
                        }
                    )
                }
            }
        }
        return
    }

    val isDay = weather.isDay
    val isSystemDark = isSystemInDarkTheme()
    val isDark = ThemeMode.resolveIsDark(themeMode, isDay, isSystemDark)
    val condition = weather.condition
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

            Box(modifier = modifier.fillMaxSize()) {
                // 1. ライブ天候フロスト背景（空色グラデーション + 動的パーティクル + すりガラス拡散）
                DetailAtmosphereBackdrop(
                    condition = condition,
                    isDay = isDay,
                    isDark = isDark
                )

                // 2. メインコンテンツ（固定トップバー + スクロール可能な詳細データ）
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
                                    0.0f to themeSurface.copy(alpha = if (isDark) 0.68f else 0.56f),
                                    0.65f to themeSurface.copy(alpha = if (isDark) 0.42f else 0.30f),
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
                                color = getDetailCardColor(
                                    isDark = isDark,
                                    colorScheme = themeColorScheme,
                                    alpha = if (isDark) 0.78f else 0.80f
                                ),
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroWindSection(
    wind: io.github.nemynew.expressiveweather.data.WindDetails,
    language: AppLanguage,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = LocalAppStrings.current
    val accentPalette = currentAccentPalette()
    val isColorful = accentPalette.mode.isColorful
    val arrowShape = MaterialShapes.Arrow.toShape()
    val arrowColor = if (isColorful) accentPalette.wind.accent else colorScheme.primary
    val headerIconColor = if (isColorful) accentPalette.wind.accent else colorScheme.primary
    val windMs = wind.windSpeedMs
    val gustsMs = wind.windGustsMs
    val localizedDir = getLocalizedWindDirection(wind.windDegree, language)

    val animatedWindDegree by animateFloatAsState(
        targetValue = windFlowRotationDegrees(wind.windDegree),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HeroWindCompass"
    )

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = getDetailCardColor(colorScheme = colorScheme),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ヘッダー: 風タイル (WindCard) と同一のアイコン + タイトル
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_weather_squalls_24),
                    contentDescription = strings.wind,
                    tint = headerIconColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.wind,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex,
                    color = colorScheme.onSurface
                )
            }

            // 風タイル (WindCard) と同一の2層回転 Arrow シェイプ + 中央の風速・風向表示
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(188.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(180.dp)
                        .rotate(animatedWindDegree)
                ) {
                    Surface(
                        shape = arrowShape,
                        color = arrowColor.copy(alpha = if (isColorful) 0.24f else 0.18f),
                        modifier = Modifier.size(180.dp)
                    ) {}
                    Surface(
                        shape = arrowShape,
                        color = arrowColor.copy(alpha = if (isColorful) 0.50f else 0.36f),
                        modifier = Modifier.size(154.dp)
                    ) {}
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = windSpeedUnit.formatValueFromMs(windMs, language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp,
                            color = colorScheme.onSurface,
                            lineHeight = 42.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = windSpeedUnit.getDisplayUnit(language),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$localizedDir · ${wind.windDegree.toInt()}°",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = colorScheme.onSurface
                    )
                }
            }

            // 風力階級バッジ
            val beaufort = wind.beaufortScale
            val beaufortName = beaufort.getName(language)
            Surface(
                shape = CircleShape,
                color = if (isColorful) accentPalette.wind.container else colorScheme.primaryContainer.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "$beaufortName (${strings.beaufortScaleLevel(beaufort.level)})",
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isColorful) accentPalette.wind.onContainer else colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
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
                            color = accentPalette.sunset.accent
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
        color = getDetailCardColor(colorScheme = colorScheme),
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
                    val gustRole = currentAccentPalette().sunset
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = gustRole.container
                    ) {
                        Text(
                            text = "Max ${windSpeedUnit.format(peakSpeedKmH, language)} (${getLocalizedTimeLabel(peakTime, language)})",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = gustRole.onContainer,
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

                        // 風向アロー (風の吹く向きへ回転: 北風0°→南向き180°)
                        Icon(
                            imageVector = Icons.Rounded.North,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(windFlowRotationDegrees(item.windDegree))
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
        color = getDetailCardColor(colorScheme = colorScheme),
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
                        // 曜日 & 日付（改行を絶対にしないように十分な固定幅を確保 & maxLines=1, softWrap=false）
                        Column(modifier = Modifier.width(82.dp)) {
                            Text(
                                text = dayLabel,
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                tint = currentAccentPalette().sunset.accent,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(windFlowRotationDegrees(day.dominantWindDegree))
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
                                                currentAccentPalette().wind.accent,
                                                currentAccentPalette().sunset.accent.copy(alpha = 0.55f)
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
        color = getDetailCardColor(colorScheme = colorScheme),
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
                            AppLanguage.KOREAN -> "잘 마르지 않음"
                            AppLanguage.SIMPLIFIED_CHINESE -> "不易晾干"
                            AppLanguage.TRADITIONAL_CHINESE -> "不易晾乾"
                            AppLanguage.ENGLISH -> "Slow drying"
                            AppLanguage.SPANISH -> "Secado lento"
                            AppLanguage.FRENCH -> "Séchage lent"
                            AppLanguage.PORTUGUESE -> "Secagem lenta"
                            AppLanguage.RUSSIAN -> "Медленная сушка"
                        } to colorScheme.onSurfaceVariant
                        windMs < 8.0 -> when (language) {
                            AppLanguage.JAPANESE -> "外干しに最適"
                            AppLanguage.KOREAN -> "야외 건조 최적"
                            AppLanguage.SIMPLIFIED_CHINESE -> "适宜户外晾晒"
                            AppLanguage.TRADITIONAL_CHINESE -> "適宜戶外晾曬"
                            AppLanguage.ENGLISH -> "Great for drying"
                            AppLanguage.SPANISH -> "Ideal para secar"
                            AppLanguage.FRENCH -> "Idéal pour sécher"
                            AppLanguage.PORTUGUESE -> "Ótimo para secar"
                            AppLanguage.RUSSIAN -> "Отлично для сушки"
                        } to colorScheme.primary
                        windMs < 10.8 -> when (language) {
                            AppLanguage.JAPANESE -> "飛ばされないよう留める"
                            AppLanguage.KOREAN -> "날리지 않게 고정"
                            AppLanguage.SIMPLIFIED_CHINESE -> "注意固定防风吹落"
                            AppLanguage.TRADITIONAL_CHINESE -> "注意固定防風吹落"
                            AppLanguage.ENGLISH -> "Fasten securely"
                            AppLanguage.SPANISH -> "Sujetar bien"
                            AppLanguage.FRENCH -> "Bien fixer"
                            AppLanguage.PORTUGUESE -> "Prender bem"
                            AppLanguage.RUSSIAN -> "Надёжно закрепите"
                        } to colorScheme.tertiary
                        else -> when (language) {
                            AppLanguage.JAPANESE -> "部屋干し推奨"
                            AppLanguage.KOREAN -> "실내 건조 권장"
                            AppLanguage.SIMPLIFIED_CHINESE -> "建议室内晾干"
                            AppLanguage.TRADITIONAL_CHINESE -> "建議室內晾乾"
                            AppLanguage.ENGLISH -> "Dry indoors"
                            AppLanguage.SPANISH -> "Secar en interior"
                            AppLanguage.FRENCH -> "Sécher à l'intérieur"
                            AppLanguage.PORTUGUESE -> "Secar em ambiente interno"
                            AppLanguage.RUSSIAN -> "Сушите в помещении"
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
                            AppLanguage.KOREAN -> "안전하게 사용 가능"
                            AppLanguage.SIMPLIFIED_CHINESE -> "可正常撑伞"
                            AppLanguage.TRADITIONAL_CHINESE -> "可正常撐傘"
                            AppLanguage.ENGLISH -> "Safe to use"
                            AppLanguage.SPANISH -> "Uso seguro"
                            AppLanguage.FRENCH -> "Sans problème"
                            AppLanguage.PORTUGUESE -> "Uso seguro"
                            AppLanguage.RUSSIAN -> "Безопасно"
                        } to colorScheme.primary
                        windMs < 10.8 -> when (language) {
                            AppLanguage.JAPANESE -> "両手でしっかり保持"
                            AppLanguage.KOREAN -> "단단히 잡고 사용"
                            AppLanguage.SIMPLIFIED_CHINESE -> "双手握紧防翻伞"
                            AppLanguage.TRADITIONAL_CHINESE -> "雙手握緊防翻傘"
                            AppLanguage.ENGLISH -> "Hold firmly"
                            AppLanguage.SPANISH -> "Sujetar con firmeza"
                            AppLanguage.FRENCH -> "Tenir fermement"
                            AppLanguage.PORTUGUESE -> "Segurar firme"
                            AppLanguage.RUSSIAN -> "Держите крепче"
                        } to colorScheme.tertiary
                        else -> when (language) {
                            AppLanguage.JAPANESE -> "傘破損・転倒の恐れ"
                            AppLanguage.KOREAN -> "우산 파손·낙상 주의"
                            AppLanguage.SIMPLIFIED_CHINESE -> "极易损伞摔倒危险"
                            AppLanguage.TRADITIONAL_CHINESE -> "極易損傘跌倒危險"
                            AppLanguage.ENGLISH -> "Damaging winds"
                            AppLanguage.SPANISH -> "Vientos dañinos"
                            AppLanguage.FRENCH -> "Vents violents"
                            AppLanguage.PORTUGUESE -> "Ventos fortes"
                            AppLanguage.RUSSIAN -> "Опасный ветер"
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
                            AppLanguage.KOREAN -> "쾌적한 주행 가능"
                            AppLanguage.SIMPLIFIED_CHINESE -> "平稳舒适骑行"
                            AppLanguage.TRADITIONAL_CHINESE -> "平穩舒適騎行"
                            AppLanguage.ENGLISH -> "Smooth ride"
                            AppLanguage.SPANISH -> "Trayecto cómodo"
                            AppLanguage.FRENCH -> "Trajet agréable"
                            AppLanguage.PORTUGUESE -> "Pedalada tranquila"
                            AppLanguage.RUSSIAN -> "Комфортная езда"
                        } to colorScheme.primary
                        windMs < 13.9 -> when (language) {
                            AppLanguage.JAPANESE -> "横風に煽られ注意"
                            AppLanguage.KOREAN -> "측풍 주의"
                            AppLanguage.SIMPLIFIED_CHINESE -> "注意侧风摇摆"
                            AppLanguage.TRADITIONAL_CHINESE -> "注意側風搖擺"
                            AppLanguage.ENGLISH -> "Crosswind caution"
                            AppLanguage.SPANISH -> "Cuidado con viento lateral"
                            AppLanguage.FRENCH -> "Attention au vent latéral"
                            AppLanguage.PORTUGUESE -> "Atenção ao vento lateral"
                            AppLanguage.RUSSIAN -> "Осторожно: боковой ветер"
                        } to colorScheme.tertiary
                        else -> when (language) {
                            AppLanguage.JAPANESE -> "転倒の恐れあり危険"
                            AppLanguage.KOREAN -> "전도 위험·주행 주의"
                            AppLanguage.SIMPLIFIED_CHINESE -> "有吹翻危险禁止骑行"
                            AppLanguage.TRADITIONAL_CHINESE -> "有吹翻危險禁止騎行"
                            AppLanguage.ENGLISH -> "Dangerous"
                            AppLanguage.SPANISH -> "Peligroso"
                            AppLanguage.FRENCH -> "Dangereux"
                            AppLanguage.PORTUGUESE -> "Perigoso"
                            AppLanguage.RUSSIAN -> "Опасно"
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
                            AppLanguage.KOREAN -> "환기하기 좋음"
                            AppLanguage.SIMPLIFIED_CHINESE -> "开窗通风良好"
                            AppLanguage.TRADITIONAL_CHINESE -> "開窗通風良好"
                            AppLanguage.ENGLISH -> "Good ventilation"
                            AppLanguage.SPANISH -> "Buena ventilación"
                            AppLanguage.FRENCH -> "Bonne aération"
                            AppLanguage.PORTUGUESE -> "Boa ventilação"
                            AppLanguage.RUSSIAN -> "Хорошее проветривание"
                        } to colorScheme.primary
                        else -> when (language) {
                            AppLanguage.JAPANESE -> "強風のため戸締まり推奨"
                            AppLanguage.KOREAN -> "강풍으로 창문 닫기 권장"
                            AppLanguage.SIMPLIFIED_CHINESE -> "风力过大紧闭门窗"
                            AppLanguage.TRADITIONAL_CHINESE -> "風力過大緊閉門窗"
                            AppLanguage.ENGLISH -> "Keep windows shut"
                            AppLanguage.SPANISH -> "Mantén las ventanas cerradas"
                            AppLanguage.FRENCH -> "Fermez les fenêtres"
                            AppLanguage.PORTUGUESE -> "Mantenha as janelas fechadas"
                            AppLanguage.RUSSIAN -> "Держите окна закрытыми"
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
