package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import io.github.nemynew.expressiveweather.ui.components.horizontalScrollFade
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.HourlyForecast
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.getLocalizedLabel
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.data.localizedName
import io.github.nemynew.expressiveweather.theme.ExpressiveWeatherTheme
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.GoogleSansFlexHeroTemp
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.theme.getAtmosphereTheme
import androidx.compose.foundation.isSystemInDarkTheme
import io.github.nemynew.expressiveweather.ui.components.WeatherAtmosphereCanvas
import io.github.nemynew.expressiveweather.ui.components.WeatherIconHelper
import io.github.nemynew.expressiveweather.ui.components.m3eBouncyClick
import java.util.Locale

/**
 * 24時間詳細画面の表示メトリクス種別
 */
enum class HourlyChartMetric(
    val labelJa: String,
    val labelEn: String,
    val labelZhHans: String,
    val labelZhHant: String
) {
    TEMPERATURE("気温", "Temperature", "气温", "氣溫"),
    PRECIPITATION("降水", "Precipitation", "降水", "降水"),
    WIND("風速", "Wind Speed", "风速", "風速");

    fun getLabel(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> labelJa
        AppLanguage.SIMPLIFIED_CHINESE -> labelZhHans
        AppLanguage.TRADITIONAL_CHINESE -> labelZhHant
        AppLanguage.ENGLISH -> labelEn
    }
}

/**
 * メイン画面の「24時間の天気」パネルをタップして開く、24時間天気に特化した独立詳細画面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HourlyDetailScreen(
    weather: WeatherData?,
    city: City,
    language: AppLanguage,
    temperatureUnit: TemperatureUnit,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    weatherAdaptiveThemeEnabled: Boolean = true,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    themeMode: ThemeMode = ThemeMode.WEATHER,
    initialSelectedHour: Int?,
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
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "天気データを読み込み中..."
                    AppLanguage.SIMPLIFIED_CHINESE -> "正在加载天气数据..."
                    AppLanguage.TRADITIONAL_CHINESE -> "正在載入天氣資料..."
                    AppLanguage.ENGLISH -> "Loading weather data..."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

    val hourlyList = remember(weather) { weather.hourlyForecast.take(24) }

    // 初期選択時間のインデックス解決
    val initialIdx = remember(initialSelectedHour, hourlyList) {
        if (initialSelectedHour != null) {
            val idx = hourlyList.indexOfFirst { it.hour == initialSelectedHour }
            if (idx >= 0) idx else 0
        } else {
            0
        }
    }

    var selectedIndex by remember { mutableIntStateOf(initialIdx) }
    val selectedItem = hourlyList.getOrNull(selectedIndex) ?: hourlyList.firstOrNull()

    // グラフの表示モード（気温 / 降水 / 風速）
    var selectedMetric by remember { mutableStateOf(HourlyChartMetric.TEMPERATURE) }

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
            val expressHaptics = LocalExpressiveHaptics.current
            val selectHourWithHaptic: (Int) -> Unit = { newIndex ->
                if (newIndex != selectedIndex && hourlyList.isNotEmpty()) {
                    val lastIdx = hourlyList.lastIndex
                    if (newIndex == 0 || newIndex == lastIdx) {
                        expressHaptics.play(ExpressiveHapticPattern.DETENT_BOUNDARY)
                    } else {
                        expressHaptics.play(ExpressiveHapticPattern.SCRUB_TICK)
                    }
                }
                selectedIndex = newIndex
            }
            val selectMetricWithHaptic: (HourlyChartMetric) -> Unit = { newMetric ->
                if (newMetric != selectedMetric) {
                    expressHaptics.play(ExpressiveHapticPattern.SWITCH_ON)
                }
                selectedMetric = newMetric
            }

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
                        HourlyTopBar(
                            cityName = city.localizedName(language),
                            language = language,
                            condition = condition,
                            isDay = isDay,
                            onBack = onBack,
                            modifier = Modifier.statusBarsPadding()
                        )
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
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 36.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 選択時間の大型ステータスカード
                        if (selectedItem != null) {
                            SelectedHourHeroCard(
                                item = selectedItem,
                                temperatureUnit = temperatureUnit,
                                windSpeedUnit = windSpeedUnit,
                                language = language
                            )
                        }

                        // 24時間タイムラインチャート（メトリクス切り替え付き）
                        HourlyInteractiveTimelineCard(
                            hourlyList = hourlyList,
                            selectedIndex = selectedIndex,
                            onSelectIndex = selectHourWithHaptic,
                            selectedMetric = selectedMetric,
                            onMetricChanged = selectMetricWithHaptic,
                            temperatureUnit = temperatureUnit,
                            windSpeedUnit = windSpeedUnit,
                            sunrise = weather.sunSchedule.sunrise,
                            sunset = weather.sunSchedule.sunset,
                            language = language
                        )

                        // 24時間スマートインサイト要約
                        HourlyInsightsSection(
                            hourlyList = hourlyList,
                            temperatureUnit = temperatureUnit,
                            windSpeedUnit = windSpeedUnit,
                            sunrise = weather.sunSchedule.sunrise,
                            sunset = weather.sunSchedule.sunset,
                            daylightHours = weather.sunSchedule.daylightHours,
                            language = language
                        )

                        // 24時間全時間帯ブレークダウンリスト
                        HourlyBreakdownListCard(
                            hourlyList = hourlyList,
                            selectedIndex = selectedIndex,
                            onSelectIndex = selectHourWithHaptic,
                            temperatureUnit = temperatureUnit,
                            windSpeedUnit = windSpeedUnit,
                            language = language
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * 1. トップバー (WindDetailScreen準拠のプレミアムデザイン)
 */
@Composable
private fun HourlyTopBar(
    cityName: String,
    language: AppLanguage,
    condition: WeatherCondition,
    isDay: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 戻るボタン
        Surface(
            shape = CircleShape,
            color = colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
            modifier = Modifier
                .size(44.dp)
                .m3eBouncyClick(scaleDown = 0.88f, overshootScale = 1.12f) {
                    onBack()
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = when (language) {
                        AppLanguage.JAPANESE -> "戻る"
                        AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.TRADITIONAL_CHINESE -> "返回"
                        AppLanguage.ENGLISH -> "Back"
                    },
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // タイトル & 都市名
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "24時間の天気"
                    AppLanguage.SIMPLIFIED_CHINESE -> "24小时天气预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "24小時天氣預報"
                    AppLanguage.ENGLISH -> "24-Hour Forecast"
                },
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colorScheme.onSurface
            )
            Text(
                text = cityName,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 2. 選択中時間の大型ステータスカード
 */
@Composable
private fun SelectedHourHeroCard(
    item: HourlyForecast,
    temperatureUnit: TemperatureUnit,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    language: AppLanguage
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.90f)

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ヘッダー行: 時刻ラベル & 昼夜バッジ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.primaryContainer.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = getLocalizedTimeLabel(item.timeLabel, language),
                            fontFamily = GoogleSansFlex,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = item.condition.getLocalizedLabel(language),
                        fontFamily = GoogleSansFlex,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isDay) Icons.Rounded.WbSunny else Icons.Rounded.WbTwilight,
                            contentDescription = null,
                            tint = if (item.isDay) Color(0xFFF9A825) else colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (item.isDay) {
                                when (language) {
                                    AppLanguage.JAPANESE -> "昼間"
                                    AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.TRADITIONAL_CHINESE -> "白天"
                                    AppLanguage.ENGLISH -> "Day"
                                }
                            } else {
                                when (language) {
                                    AppLanguage.JAPANESE -> "夜間"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "夜间"
                                    AppLanguage.TRADITIONAL_CHINESE -> "夜間"
                                    AppLanguage.ENGLISH -> "Night"
                                }
                            },
                            fontFamily = GoogleSansFlex,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // メイン行: アイコン + 超大型気温
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    painter = painterResource(
                        WeatherIconHelper.getConditionDrawable(item.condition, item.isDay)
                    ),
                    contentDescription = null,
                    tint = WeatherIconHelper.getConditionIconTint(item.condition),
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "${temperatureUnit.convert(item.tempC)}°",
                    fontFamily = GoogleSansFlexHeroTemp,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    lineHeight = 58.sp
                )
            }

            // 3分割メトリクスチップ (湿度・風速・降水確率)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    icon = Icons.Rounded.Opacity,
                    label = when (language) {
                        AppLanguage.JAPANESE -> "湿度"
                        AppLanguage.SIMPLIFIED_CHINESE -> "湿度"
                        AppLanguage.TRADITIONAL_CHINESE -> "濕度"
                        AppLanguage.ENGLISH -> "Humidity"
                    },
                    value = "${item.humidity}%",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    icon = Icons.Rounded.Air,
                    label = when (language) {
                        AppLanguage.JAPANESE -> "風速"
                        AppLanguage.SIMPLIFIED_CHINESE -> "风速"
                        AppLanguage.TRADITIONAL_CHINESE -> "風速"
                        AppLanguage.ENGLISH -> "Wind"
                    },
                    value = windSpeedUnit.formatCompact(item.windSpeedKmH, language),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    icon = Icons.Rounded.WaterDrop,
                    label = when (language) {
                        AppLanguage.JAPANESE -> "降水確率"
                        AppLanguage.SIMPLIFIED_CHINESE -> "降水概率"
                        AppLanguage.TRADITIONAL_CHINESE -> "降水機率"
                        AppLanguage.ENGLISH -> "Precip"
                    },
                    value = "${item.precipProb}%",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = label,
                    fontFamily = GoogleSansFlex,
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = value,
                    fontFamily = GoogleSansFlex,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 3. 24時間インタラクティブ・タイムラインカード
 */
@Composable
private fun HourlyInteractiveTimelineCard(
    hourlyList: List<HourlyForecast>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    selectedMetric: HourlyChartMetric,
    onMetricChanged: (HourlyChartMetric) -> Unit,
    temperatureUnit: TemperatureUnit,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    sunrise: String,
    sunset: String,
    language: AppLanguage
) {
    if (hourlyList.isEmpty()) return

    val colorScheme = MaterialTheme.colorScheme
    val cardColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 見出し + メトリクス切り替えピル
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "タイムライン"
                        AppLanguage.SIMPLIFIED_CHINESE -> "逐小时时间线"
                        AppLanguage.TRADITIONAL_CHINESE -> "逐小時時間軸"
                        AppLanguage.ENGLISH -> "Timeline"
                    },
                    fontFamily = GoogleSansFlex,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                // メトリクス切り替えピル行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HourlyChartMetric.entries.forEach { metric ->
                        val isSelected = (metric == selectedMetric)
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                            label = "metricPillBg"
                        )
                        val pillText = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = pillBg,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onMetricChanged(metric) }
                                )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = metric.getLabel(language),
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = pillText
                                )
                            }
                        }
                    }
                }
            }

            // チャート本体（横スクロールコンテナ）
            val chartScrollState = rememberScrollState()
            val columnWidthDp = 58.dp
            val density = LocalDensity.current
            val columnWidthPx = with(density) { columnWidthDp.toPx() }
            val totalWidthDp = columnWidthDp * hourlyList.size

            val minTemp = hourlyList.minOfOrNull { it.tempC } ?: 10
            val maxTemp = hourlyList.maxOfOrNull { it.tempC } ?: 25
            val tempRange = (maxTemp - minTemp).coerceAtLeast(1)

            // 風速の固定基準スケール（最大風速または40km/h）
            val maxWind = maxOf(40.0, hourlyList.maxOfOrNull { it.windSpeedKmH } ?: 30.0)

            val primaryColor = colorScheme.primary
            val surfaceColor = colorScheme.surface
            val onSurface = colorScheme.onSurface
            val onSurfaceVariant = colorScheme.onSurfaceVariant

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScrollFade(chartScrollState, fadeWidth = 24.dp)
                    .horizontalScroll(chartScrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(totalWidthDp)
                        .height(210.dp)
                        .pointerInput(hourlyList) {
                            detectTapGestures { offset ->
                                if (hourlyList.isNotEmpty()) {
                                    val clickedIdx = (offset.x / columnWidthPx).toInt().coerceIn(0, hourlyList.size - 1)
                                    onSelectIndex(clickedIdx)
                                }
                            }
                        }
                ) {
                    // Canvas 描画（折れ線・塗りつぶし・バー・スクラバー線・目盛りガイドライン）
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val chartHeight = 125.dp.toPx()
                        val topPadding = 24.dp.toPx()
                        val bottomPadding = 20.dp.toPx()
                        val usableHeight = chartHeight - topPadding - bottomPadding

                        fun yForValue(fraction: Float): Float {
                            return chartHeight - bottomPadding - (fraction.coerceIn(0f, 1f) * usableHeight)
                        }

                        // 水平目盛りガイドラインの描画（破線）
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                        val guideLineColor = onSurfaceVariant.copy(alpha = 0.18f)

                        when (selectedMetric) {
                            HourlyChartMetric.PRECIPITATION -> {
                                // 常時 0% 〜 100% の基準ガイドライン（100%, 50%, 0%）
                                val y100 = yForValue(1.0f)
                                val y50 = yForValue(0.5f)
                                val y0 = yForValue(0.0f)

                                drawLine(guideLineColor, Offset(0f, y100), Offset(size.width, y100), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                                drawLine(guideLineColor, Offset(0f, y50), Offset(size.width, y50), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                                drawLine(guideLineColor, Offset(0f, y0), Offset(size.width, y0), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                            }
                            HourlyChartMetric.WIND -> {
                                // 風速基準ガイドライン（30 km/h, 15 km/h, 0 km/h）
                                val y30 = yForValue((30.0 / maxWind).toFloat())
                                val y15 = yForValue((15.0 / maxWind).toFloat())
                                val y0 = yForValue(0.0f)

                                drawLine(guideLineColor, Offset(0f, y30), Offset(size.width, y30), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                                drawLine(guideLineColor, Offset(0f, y15), Offset(size.width, y15), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                                drawLine(guideLineColor, Offset(0f, y0), Offset(size.width, y0), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                            }
                            HourlyChartMetric.TEMPERATURE -> {
                                val yMid = yForValue(0.5f)
                                drawLine(guideLineColor, Offset(0f, yMid), Offset(size.width, yMid), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                            }
                        }

                        // 各ポイントのY座標計算
                        val points = hourlyList.mapIndexed { i, item ->
                            val frac = when (selectedMetric) {
                                HourlyChartMetric.TEMPERATURE -> (item.tempC - minTemp).toFloat() / tempRange
                                // 降水確率は常に 0%〜100% 固定 (0f〜1f)
                                HourlyChartMetric.PRECIPITATION -> (item.precipProb / 100f).coerceIn(0f, 1f)
                                // 風速は固定スケール maxWind に対する割合
                                HourlyChartMetric.WIND -> (item.windSpeedKmH / maxWind).toFloat().coerceIn(0f, 1f)
                            }
                            Offset(i * columnWidthPx + columnWidthPx / 2f, yForValue(frac))
                        }

                        // ベジェ曲線パス
                        val linePath = Path().apply {
                            if (points.isNotEmpty()) {
                                moveTo(points[0].x, points[0].y)
                                for (i in 0 until points.size - 1) {
                                    val p0 = points[i]
                                    val p1 = points[i + 1]
                                    val cx = (p0.x + p1.x) / 2f
                                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                }
                            }
                        }

                        // グラデーション塗りつぶし
                        val fillPath = Path().apply {
                            addPath(linePath)
                            if (points.isNotEmpty()) {
                                lineTo(points.last().x, chartHeight)
                                lineTo(points.first().x, chartHeight)
                                close()
                            }
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    primaryColor.copy(alpha = 0.02f)
                                ),
                                startY = 0f,
                                endY = chartHeight
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 降水バーエリア（下部）
                        val precipAreaTop = chartHeight + 10.dp.toPx()
                        val maxBarHeight = size.height - precipAreaTop - 26.dp.toPx()

                        hourlyList.forEachIndexed { i, item ->
                            val xCenter = i * columnWidthPx + columnWidthPx / 2f
                            val isSelected = (i == selectedIndex)

                            // チャート上のドット
                            drawCircle(
                                color = if (isSelected) primaryColor else surfaceColor,
                                radius = if (isSelected) 5.5.dp.toPx() else 3.5.dp.toPx(),
                                center = points[i]
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = surfaceColor,
                                    radius = 2.5.dp.toPx(),
                                    center = points[i]
                                )
                            }

                            // 降水バー（降水メトリクス選択時のみ描画し、気温モード下部からは消去）
                            if (selectedMetric == HourlyChartMetric.PRECIPITATION && item.precipProb > 0) {
                                val barH = (item.precipProb / 100f) * maxBarHeight
                                val barW = 16.dp.toPx()
                                drawRoundRect(
                                    color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.45f),
                                    topLeft = Offset(xCenter - barW / 2f, size.height - 26.dp.toPx() - barH),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }

                        // 選択インジケーター線（点線スクラバー）
                        if (selectedIndex in points.indices) {
                            val selectedX = points[selectedIndex].x
                            drawLine(
                                color = primaryColor.copy(alpha = 0.8f),
                                start = Offset(selectedX, 6.dp.toPx()),
                                end = Offset(selectedX, size.height - 20.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }
                    }

                    // カラムごとのテキスト & タップエリア
                    Row(modifier = Modifier.fillMaxSize()) {
                        hourlyList.forEachIndexed { index, item ->
                            val isSelected = (index == selectedIndex)

                            Column(
                                modifier = Modifier
                                    .width(columnWidthDp)
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onSelectIndex(index)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 時刻
                                Text(
                                    text = getLocalizedTimeLabel(item.timeLabel, language),
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) primaryColor else onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // 値表示（選択メトリクス連動）
                                when (selectedMetric) {
                                    HourlyChartMetric.TEMPERATURE -> {
                                        Text(
                                            text = "${temperatureUnit.convert(item.tempC)}°",
                                            fontFamily = GoogleSansFlex,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) primaryColor else onSurface
                                        )
                                    }
                                    HourlyChartMetric.PRECIPITATION -> {
                                        // 降水確率（0%〜100%）
                                        Text(
                                            text = "${item.precipProb}%",
                                            fontFamily = GoogleSansFlex,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) primaryColor else (if (item.precipProb > 0) primaryColor else onSurface)
                                        )
                                        Spacer(modifier = Modifier.height(30.dp))
                                        // 降水量表示 (mm)
                                        Text(
                                            text = if (item.precipitationMm > 0) String.format(Locale.US, "%.1fmm", item.precipitationMm) else "-",
                                            fontFamily = GoogleSansFlex,
                                            fontSize = 10.sp,
                                            color = onSurfaceVariant.copy(alpha = 0.75f)
                                        )
                                    }
                                    HourlyChartMetric.WIND -> {
                                        Text(
                                            text = windSpeedUnit.formatCompact(item.windSpeedKmH, language),
                                            fontFamily = GoogleSansFlex,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) primaryColor else onSurface
                                        )
                                        Spacer(modifier = Modifier.height(30.dp))
                                        // 風の強さレベル（何を示しているかを直感的に明示）
                                        val windSpeed = item.windSpeedKmH
                                        val windStrengthLabel = when {
                                            windSpeed < 10.0 -> when (language) {
                                                AppLanguage.JAPANESE -> "穏やか"
                                                AppLanguage.SIMPLIFIED_CHINESE -> "微风"
                                                AppLanguage.TRADITIONAL_CHINESE -> "微風"
                                                AppLanguage.ENGLISH -> "Calm"
                                            }
                                            windSpeed < 20.0 -> when (language) {
                                                AppLanguage.JAPANESE -> "微風"
                                                AppLanguage.SIMPLIFIED_CHINESE -> "软风"
                                                AppLanguage.TRADITIONAL_CHINESE -> "軟風"
                                                AppLanguage.ENGLISH -> "Light"
                                            }
                                            windSpeed < 32.0 -> when (language) {
                                                AppLanguage.JAPANESE -> "和風"
                                                AppLanguage.SIMPLIFIED_CHINESE -> "清风"
                                                AppLanguage.TRADITIONAL_CHINESE -> "清風"
                                                AppLanguage.ENGLISH -> "Breeze"
                                            }
                                            else -> when (language) {
                                                AppLanguage.JAPANESE -> "強風"
                                                AppLanguage.SIMPLIFIED_CHINESE -> "强风"
                                                AppLanguage.TRADITIONAL_CHINESE -> "強風"
                                                AppLanguage.ENGLISH -> "Strong"
                                            }
                                        }
                                        Text(
                                            text = windStrengthLabel,
                                            fontFamily = GoogleSansFlex,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) primaryColor else onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. 24時間スマートインサイト要約
 */
@Composable
private fun HourlyInsightsSection(
    hourlyList: List<HourlyForecast>,
    temperatureUnit: TemperatureUnit,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    sunrise: String,
    sunset: String,
    daylightHours: String,
    language: AppLanguage
) {
    val colorScheme = MaterialTheme.colorScheme

    val maxTempItem = hourlyList.maxByOrNull { it.tempC }
    val minTempItem = hourlyList.minByOrNull { it.tempC }
    val maxPrecipItem = hourlyList.maxByOrNull { it.precipProb }
    val maxWindItem = hourlyList.maxByOrNull { it.windSpeedKmH }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = when (language) {
                AppLanguage.JAPANESE -> "24時間の要約インサイト"
                AppLanguage.SIMPLIFIED_CHINESE -> "24小时精选看点"
                AppLanguage.TRADITIONAL_CHINESE -> "24小時精選摘要"
                AppLanguage.ENGLISH -> "24-Hour Highlights"
            },
            fontFamily = GoogleSansFlex,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 気温レンジ
            InsightCard(
                title = when (language) {
                    AppLanguage.JAPANESE, AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.TRADITIONAL_CHINESE -> "最高 / 最低"
                    AppLanguage.ENGLISH -> "High / Low"
                },
                mainText = if (maxTempItem != null && minTempItem != null) {
                    "${temperatureUnit.convert(maxTempItem.tempC)}° / ${temperatureUnit.convert(minTempItem.tempC)}°"
                } else "--",
                subText = if (maxTempItem != null) {
                    when (language) {
                        AppLanguage.JAPANESE -> "最高 ${maxTempItem.timeLabel}"
                        AppLanguage.SIMPLIFIED_CHINESE -> "最高温出现在 ${maxTempItem.timeLabel}"
                        AppLanguage.TRADITIONAL_CHINESE -> "最高溫出現在 ${maxTempItem.timeLabel}"
                        AppLanguage.ENGLISH -> "Peak at ${maxTempItem.timeLabel}"
                    }
                } else "",
                modifier = Modifier.weight(1f)
            )

            // 降水ピーク
            InsightCard(
                title = when (language) {
                    AppLanguage.JAPANESE -> "降水ピーク"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水高峰"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨高峰"
                    AppLanguage.ENGLISH -> "Peak Rain"
                },
                mainText = if (maxPrecipItem != null) "${maxPrecipItem.precipProb}%" else "0%",
                subText = if (maxPrecipItem != null && maxPrecipItem.precipProb > 0) {
                    when (language) {
                        AppLanguage.JAPANESE -> "${maxPrecipItem.timeLabel} 前後"
                        AppLanguage.SIMPLIFIED_CHINESE -> "${maxPrecipItem.timeLabel} 前后"
                        AppLanguage.TRADITIONAL_CHINESE -> "${maxPrecipItem.timeLabel} 前後"
                        AppLanguage.ENGLISH -> "Around ${maxPrecipItem.timeLabel}"
                    }
                } else {
                    when (language) {
                        AppLanguage.JAPANESE -> "雨の心配なし"
                        AppLanguage.SIMPLIFIED_CHINESE -> "无需担心降雨"
                        AppLanguage.TRADITIONAL_CHINESE -> "無需擔心降雨"
                        AppLanguage.ENGLISH -> "No rain expected"
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 最大風速
            InsightCard(
                title = when (language) {
                    AppLanguage.JAPANESE -> "最大風速"
                    AppLanguage.SIMPLIFIED_CHINESE -> "最大阵风"
                    AppLanguage.TRADITIONAL_CHINESE -> "最大陣風"
                    AppLanguage.ENGLISH -> "Max Wind"
                },
                mainText = if (maxWindItem != null) {
                    windSpeedUnit.format(maxWindItem.windSpeedKmH, language)
                } else "--",
                subText = if (maxWindItem != null) {
                    when (language) {
                        AppLanguage.JAPANESE -> "${maxWindItem.timeLabel} に最大"
                        AppLanguage.SIMPLIFIED_CHINESE -> "${maxWindItem.timeLabel} 达最大"
                        AppLanguage.TRADITIONAL_CHINESE -> "${maxWindItem.timeLabel} 達最大"
                        AppLanguage.ENGLISH -> "Peak at ${maxWindItem.timeLabel}"
                    }
                } else "",
                modifier = Modifier.weight(1f)
            )

            // 日の出・日の入り
            InsightCard(
                title = when (language) {
                    AppLanguage.JAPANESE -> "太陽の運行"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日出与日落"
                    AppLanguage.TRADITIONAL_CHINESE -> "日出與日落"
                    AppLanguage.ENGLISH -> "Sun Schedule"
                },
                mainText = "$sunrise / $sunset",
                subText = when (language) {
                    AppLanguage.JAPANESE -> "日照時間 $daylightHours"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日照时长 $daylightHours"
                    AppLanguage.TRADITIONAL_CHINESE -> "日照時長 $daylightHours"
                    AppLanguage.ENGLISH -> "$daylightHours daylight"
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InsightCard(
    title: String,
    mainText: String,
    subText: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontFamily = GoogleSansFlex,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = mainText,
                fontFamily = GoogleSansFlex,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            if (subText.isNotBlank()) {
                Text(
                    text = subText,
                    fontFamily = GoogleSansFlex,
                    fontSize = 11.sp,
                    color = colorScheme.primary
                )
            }
        }
    }
}

/**
 * 5. 24時間全時間帯ブレークダウンリスト
 */
@Composable
private fun HourlyBreakdownListCard(
    hourlyList: List<HourlyForecast>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    temperatureUnit: TemperatureUnit,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    language: AppLanguage
) {
    if (hourlyList.isEmpty()) return

    val colorScheme = MaterialTheme.colorScheme
    val cardColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "時間別の全詳細リスト"
                        AppLanguage.SIMPLIFIED_CHINESE -> "逐小时详细预报"
                        AppLanguage.TRADITIONAL_CHINESE -> "逐小時詳細預報"
                        AppLanguage.ENGLISH -> "Hourly Breakdown"
                    },
                    fontFamily = GoogleSansFlex,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "24時間分"
                        AppLanguage.SIMPLIFIED_CHINESE -> "未来24小时"
                        AppLanguage.TRADITIONAL_CHINESE -> "未來24小時"
                        AppLanguage.ENGLISH -> "24 Hours"
                    },
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                hourlyList.forEachIndexed { index, item ->
                    val isSelected = (index == selectedIndex)
                    val rowBg by animateColorAsState(
                        targetValue = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
                        label = "hourlyRowBg"
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = rowBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectIndex(index) }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 時間
                            Text(
                                text = getLocalizedTimeLabel(item.timeLabel, language),
                                fontFamily = GoogleSansFlex,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                modifier = Modifier.width(48.dp)
                            )

                            // アイコン + 天候名
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        WeatherIconHelper.getConditionDrawable(item.condition, item.isDay)
                                    ),
                                    contentDescription = null,
                                    tint = WeatherIconHelper.getConditionIconTint(item.condition),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = item.condition.getLocalizedLabel(language),
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 12.sp,
                                    color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                )
                            }

                            // 風速
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.width(58.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Air,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = windSpeedUnit.formatCompact(item.windSpeedKmH, language),
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 11.sp,
                                    color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                )
                            }

                            // 降水確率（0%でも明示表示）
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.width(50.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_fluent_drop_24),
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${item.precipProb}%",
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 11.sp,
                                    fontWeight = if (item.precipProb > 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (item.precipProb > 0) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            // 気温
                            Text(
                                text = "${temperatureUnit.convert(item.tempC)}°",
                                fontFamily = GoogleSansFlex,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                modifier = Modifier.width(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
