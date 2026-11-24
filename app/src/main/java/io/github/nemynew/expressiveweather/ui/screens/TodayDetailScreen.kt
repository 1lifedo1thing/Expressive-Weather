package io.github.nemynew.expressiveweather.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DryCleaning
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.nemynew.expressiveweather.data.DailyForecast
import io.github.nemynew.expressiveweather.data.HourlyForecast
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.getLocalizedDayLabel
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.data.getLocalizedLabel
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
 * 7日間の日別天気詳細スクリーン (TodayDetailScreen / DailyForecastDetailScreen)
 * Material 3 Expressive ガイドラインに完全準拠。
 * 画面上部の7日間セレクターで日付を切り替え可能。
 * 各日において、24時間インタラクティブ・スクラバーグラフ、朝・昼・夕・夜のステージ分析、
 * 生活＆行動アシスタント、詳細気象メトリクスを同等の極上デザインで表示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayDetailScreen(
    weather: WeatherData?,
    city: City,
    language: AppLanguage,
    temperatureUnit: TemperatureUnit,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    weatherAdaptiveThemeEnabled: Boolean = true,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    themeMode: ThemeMode = ThemeMode.WEATHER,
    initialSelectedDay: Int = 0,
    initialSelectedHour: Int? = null,
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
                        }
                    )
                }
            }
        }
        return
    }

    // 7日間の中から選択されている日 (0 = 今日, 1 = 明日, ...)
    val dailyList = weather.dailyForecast
    var selectedDayIndex by remember {
        mutableIntStateOf(initialSelectedDay.coerceIn(0, (dailyList.size - 1).coerceAtLeast(0)))
    }
    val currentSelectedDay = dailyList.getOrNull(selectedDayIndex) ?: dailyList.firstOrNull()

    val isToday = (selectedDayIndex == 0)
    val dayCondition = currentSelectedDay?.condition ?: weather.condition
    val isDay = weather.isDay
    val isSystemDark = isSystemInDarkTheme()
    val isDark = ThemeMode.resolveIsDark(themeMode, isDay, isSystemDark)
    val colorScheme = MaterialTheme.colorScheme
    val atmosphere = getAtmosphereTheme(dayCondition, colorScheme, isDay = isDay)
    val strings = language.strings

    // その日専用の時間別予報
    val dayHourlyList = if (isToday) {
        weather.hourlyForecast
    } else {
        currentSelectedDay?.hourlyForecast?.ifEmpty { weather.hourlyForecast } ?: weather.hourlyForecast
    }

    ExpressiveWeatherTheme(
        darkTheme = isDark,
        weatherCondition = dayCondition,
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
                // 天候の空色グラデーション
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(atmosphere.skyGradient)
                        .graphicsLayer { alpha = if (!isDay) 0.35f else 0.45f }
                )

                // 1. Edge-to-Edge 動的パーティクル背景
                val hasCrispParticles = dayCondition.isPrecipitating
                WeatherAtmosphereCanvas(
                    condition = dayCondition,
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

                // 3. テーマカラーのフロストハイライト光彩層
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

                // 4. メインコンテンツ
                val scrollState = rememberScrollState()
                val density = LocalDensity.current
                val fadeHeightDp = 52.dp
                val fadeHeightPx = with(density) { fadeHeightDp.toPx() }
                val fadeThresholdPx = with(density) { 36.dp.toPx() }
                val scrollProgress = (scrollState.value.toFloat() / fadeThresholdPx).coerceIn(0f, 1f)

                Column(modifier = Modifier.fillMaxSize()) {
                    // --- トップバー ---
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
                                    text = when (language) {
                                        AppLanguage.JAPANESE -> "１週間の天気詳細"
                                        AppLanguage.KOREAN -> "주간 날씨 상세"
                                        AppLanguage.SIMPLIFIED_CHINESE -> "未来7天天气详情"
                                        AppLanguage.TRADITIONAL_CHINESE -> "未來7天天氣詳情"
                                        AppLanguage.ENGLISH -> "7-Day Weather Details"
                                        AppLanguage.SPANISH -> "Detalles del tiempo a 7 días"
                                        AppLanguage.FRENCH -> "Détails météo sur 7 jours"
                                        AppLanguage.PORTUGUESE -> "Detalhes do tempo para 7 dias"
                                    },
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
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

                            // リアルタイム/代表天候バッジ
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            WeatherIconHelper.getConditionDrawable(dayCondition, isDay)
                                        ),
                                        contentDescription = null,
                                        tint = WeatherIconHelper.getConditionIconTint(dayCondition),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = dayCondition.getLocalizedLabel(language),
                                        fontFamily = GoogleSansFlex,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // --- スクロール可能コンテンツ ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = if (scrollProgress > 0.01f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                            }
                            .drawWithContent {
                                drawContent()
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // --- 0. 7日間 日別セレクターカプセル行 ---
                        DaySelectorCapsuleRow(
                            dailyList = dailyList,
                            selectedIndex = selectedDayIndex,
                            temperatureUnit = temperatureUnit,
                            language = language,
                            onDaySelected = { selectedDayIndex = it }
                        )

                        // --- 1. ヒーロー・サマリーカード ＆ 天気ダイジェスト解説 ＆ 太陽軌道 ---
                        HeroDaySection(
                            weather = weather,
                            selectedDay = currentSelectedDay,
                            isToday = isToday,
                            temperatureUnit = temperatureUnit,
                            language = language
                        )

                        // --- 2. 24時間インタラクティブ・スクラバーグラフ ---
                        InteractiveHourlyWeatherChart(
                            hourlyList = dayHourlyList,
                            temperatureUnit = temperatureUnit,
                            windSpeedUnit = windSpeedUnit,
                            initialSelectedHour = if (isToday) initialSelectedHour else null,
                            language = language
                        )

                        // --- 3. 詳細情報メトリクス・グリッド（生活指数の上に配置） ---
                        DetailedWeatherMetricsSection(
                            weather = weather,
                            selectedDay = currentSelectedDay,
                            isToday = isToday,
                            temperatureUnit = temperatureUnit,
                            language = language,
                            windSpeedUnit = windSpeedUnit
                        )

                        // --- 4. 生活指数 ---
                        LifestyleAdvisorSection(
                            weather = weather,
                            selectedDay = currentSelectedDay,
                            isToday = isToday,
                            temperatureUnit = temperatureUnit,
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
 * 0. 7日間 日付セレクター行（カプセルタブ）
 */
@Composable
private fun DaySelectorCapsuleRow(
    dailyList: List<DailyForecast>,
    selectedIndex: Int,
    temperatureUnit: TemperatureUnit,
    language: AppLanguage,
    onDaySelected: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScrollFade(scrollState, fadeWidth = 20.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dailyList.forEachIndexed { index, day ->
            val isSelected = (index == selectedIndex)
            val dayText = if (index == 0) {
                language.strings.today
            } else {
                getLocalizedDayLabel(day.dayLabel, language)
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                modifier = Modifier
                    .m3eBouncyClick(scaleDown = 0.94f) {
                        onDaySelected(index)
                    }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = dayText,
                        fontFamily = GoogleSansFlex,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                    )
                    Text(
                        text = day.dateLabel,
                        fontFamily = GoogleSansFlex,
                        fontSize = 10.sp,
                        color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Icon(
                        painter = painterResource(
                            WeatherIconHelper.getConditionDrawable(day.condition, isDay = true)
                        ),
                        contentDescription = null,
                        tint = WeatherIconHelper.getConditionIconTint(day.condition),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${temperatureUnit.convert(day.maxTempC)}°",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                    )
                    Text(
                        text = "${temperatureUnit.convert(day.minTempC)}°",
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 1. ヒーロー・サマリーセクション
 */
@Composable
private fun HeroDaySection(
    weather: WeatherData,
    selectedDay: DailyForecast?,
    isToday: Boolean,
    temperatureUnit: TemperatureUnit,
    language: AppLanguage
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = language.strings

    val day = selectedDay ?: weather.dailyForecast.firstOrNull() ?: return
    val condition = if (isToday) weather.condition else day.condition
    val maxTemp = temperatureUnit.convert(day.maxTempC)
    val minTemp = temperatureUnit.convert(day.minTempC)
    val displayMainTemp = if (isToday) temperatureUnit.convert(weather.currentTempC) else maxTemp
    val feelsLike = if (isToday) temperatureUnit.convert(weather.feelsLikeC) else maxTemp

    // アイコン浮遊アニメーション
    val infiniteTransition = rememberInfiniteTransition(label = "HeroDayMotion")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeroDayFloat"
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 上段: 気温 & 天候アイコン
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "$displayMainTemp",
                            fontFamily = GoogleSansFlexHeroTemp,
                            fontSize = 76.sp,
                            lineHeight = 76.sp,
                            fontWeight = FontWeight.Normal,
                            color = colorScheme.onSurface,
                            letterSpacing = (-1.5).sp
                        )
                        Text(
                            text = "°",
                            fontFamily = GoogleSansFlexHeroTemp,
                            fontSize = 38.sp,
                            lineHeight = 38.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isToday) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = strings.feelsLike(feelsLike),
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            val precipLabel = when (language) {
                                AppLanguage.JAPANESE -> "降水確率 ${day.precipProb}%"
                                AppLanguage.KOREAN -> "강수확률 ${day.precipProb}%"
                                AppLanguage.SIMPLIFIED_CHINESE -> "降水概率 ${day.precipProb}%"
                                AppLanguage.TRADITIONAL_CHINESE -> "降雨機率 ${day.precipProb}%"
                                AppLanguage.ENGLISH -> "Precip ${day.precipProb}%"
                                AppLanguage.SPANISH -> "Lluvia ${day.precipProb}%"
                                AppLanguage.FRENCH -> "Pluie ${day.precipProb} %"
                                AppLanguage.PORTUGUESE -> "Chuva ${day.precipProb}%"
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = precipLabel,
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "${strings.high(maxTemp)}  ${strings.low(minTemp)}",
                            fontFamily = GoogleSansFlex,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3D調天候アイコン
                Surface(
                    shape = CircleShape,
                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(92.dp)
                        .graphicsLayer { translationY = floatOffset }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(
                                WeatherIconHelper.getConditionDrawable(condition, isDay = if (isToday) weather.isDay else true)
                            ),
                            contentDescription = null,
                            tint = WeatherIconHelper.getConditionIconTint(condition),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            // 中段: 気象状況ダイジェスト解説
            val smartSummary = remember(day, isToday, language) {
                generateDaySmartSummary(day, isToday, weather, language)
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LightMode,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = smartSummary,
                        fontFamily = GoogleSansFlex,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp,
                        color = colorScheme.onSurface
                    )
                }
            }

            // 下段: 日出没サン・トラッカー
            val sunriseTime = if (isToday) weather.sunSchedule.sunrise else day.sunrise
            val sunsetTime = if (isToday) weather.sunSchedule.sunset else day.sunset
            val dayProgress = if (isToday) weather.sunSchedule.dayProgress else 0.5f
            val daylightHours = if (isToday) weather.sunSchedule.daylightHours else day.daylightHours

            SunProgressMiniTracker(
                sunrise = sunriseTime,
                sunset = sunsetTime,
                dayProgress = dayProgress,
                daylightHours = daylightHours,
                language = language
            )
        }
    }
}

/**
 * 日の出・日の入りのミニトラッカー
 */
@Composable
private fun SunProgressMiniTracker(
    sunrise: String,
    sunset: String,
    dayProgress: Float,
    daylightHours: String,
    language: AppLanguage
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = language.strings

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WbTwilight,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.sunScheduleTitle,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${strings.daylightLabel}: $daylightHours",
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // アーチ線 Canvas
        val outlineColor = colorScheme.outlineVariant.copy(alpha = 0.4f)
        val sunColor = Color(0xFFFFB74D)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val startX = 14.dp.toPx()
                val endX = w - 14.dp.toPx()
                val baseLineY = h - 6.dp.toPx()
                val archPeakY = 6.dp.toPx()

                drawLine(
                    color = outlineColor,
                    start = Offset(startX, baseLineY),
                    end = Offset(endX, baseLineY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )

                val arcPath = Path().apply {
                    moveTo(startX, baseLineY)
                    quadraticTo(
                        (startX + endX) / 2f,
                        archPeakY,
                        endX,
                        baseLineY
                    )
                }

                drawPath(
                    path = arcPath,
                    color = outlineColor,
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )

                val progress = dayProgress.coerceIn(0f, 1f)
                val t = progress
                val sunX = (1 - t) * (1 - t) * startX + 2 * (1 - t) * t * ((startX + endX) / 2f) + t * t * endX
                val sunY = (1 - t) * (1 - t) * baseLineY + 2 * (1 - t) * t * archPeakY + t * t * baseLineY

                drawCircle(
                    color = sunColor.copy(alpha = 0.35f),
                    radius = 8.dp.toPx(),
                    center = Offset(sunX, sunY)
                )
                drawCircle(
                    color = sunColor,
                    radius = 4.5.dp.toPx(),
                    center = Offset(sunX, sunY)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${strings.sunrise} $sunrise",
                fontFamily = GoogleSansFlex,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = "${strings.sunset} $sunset",
                fontFamily = GoogleSansFlex,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 2. 24時間インタラクティブ・スクラバーグラフ
 */
@Composable
private fun InteractiveHourlyWeatherChart(
    hourlyList: List<HourlyForecast>,
    temperatureUnit: TemperatureUnit,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    initialSelectedHour: Int? = null,
    language: AppLanguage
) {
    if (hourlyList.isEmpty()) return

    val colorScheme = MaterialTheme.colorScheme
    val strings = language.strings

    var selectedIndex by remember(hourlyList) {
        val initialIdx = if (initialSelectedHour != null) {
            hourlyList.indexOfFirst { it.hour == initialSelectedHour }.coerceAtLeast(0)
        } else {
            0
        }
        mutableIntStateOf(initialIdx)
    }

    val currentItem = hourlyList.getOrNull(selectedIndex) ?: hourlyList.first()
    val expressHaptics = LocalExpressiveHaptics.current
    val maxScrubIndex = (hourlyList.size.coerceAtMost(24) - 1).coerceAtLeast(0)
    val selectHourWithHaptic: (Int) -> Unit = { newIndex ->
        if (newIndex != selectedIndex) {
            if (newIndex == 0 || newIndex == maxScrubIndex) {
                expressHaptics.play(ExpressiveHapticPattern.DETENT_BOUNDARY)
            } else {
                expressHaptics.play(ExpressiveHapticPattern.SCRUB_TICK)
            }
        }
        selectedIndex = newIndex
    }

    Surface(
        shape = RoundedCornerShape(32.dp),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.interactive24hChartTitle,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = getLocalizedTimeLabel(currentItem.timeLabel, language),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = currentItem,
                transitionSpec = {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                },
                label = "ScrubberDetailPreview"
            ) { item ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    WeatherIconHelper.getConditionDrawable(item.condition, item.isDay)
                                ),
                                contentDescription = null,
                                tint = WeatherIconHelper.getConditionIconTint(item.condition),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "${temperatureUnit.convert(item.tempC)}°",
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = item.condition.getLocalizedLabel(language),
                                fontFamily = GoogleSansFlex,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_fluent_drop_24),
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${item.precipProb}%",
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Opacity,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${item.humidity}%",
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Air,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = windSpeedUnit.formatCompact(item.windSpeedKmH, language),
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            val displayItems = hourlyList.take(24)
            val chartScrollState = rememberScrollState()
            val columnWidthDp = 52.dp
            val density = LocalDensity.current
            val columnWidthPx = with(density) { columnWidthDp.toPx() }
            val totalWidthDp = columnWidthDp * displayItems.size

            val minTemp = displayItems.minOfOrNull { it.tempC } ?: 10
            val maxTemp = displayItems.maxOfOrNull { it.tempC } ?: 25
            val tempRange = (maxTemp - minTemp).coerceAtLeast(1)

            val primaryColor = colorScheme.primary
            val onSurface = colorScheme.onSurface
            val onSurfaceVariant = colorScheme.onSurfaceVariant
            val surfaceColor = colorScheme.surface
            val scrubberColor = colorScheme.primary

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chartScrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(totalWidthDp)
                        .height(180.dp)
                        .pointerInput(displayItems) {
                            detectTapGestures { offset ->
                                val clickedIdx = (offset.x / columnWidthPx).toInt().coerceIn(0, displayItems.size - 1)
                                selectHourWithHaptic(clickedIdx)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val chartHeight = 110.dp.toPx()
                        val topPadding = 26.dp.toPx()
                        val bottomPadding = 16.dp.toPx()
                        val usableHeight = chartHeight - topPadding - bottomPadding

                        fun tempY(temp: Int): Float {
                            val fraction = (temp - minTemp).toFloat() / tempRange
                            return chartHeight - bottomPadding - (fraction * usableHeight)
                        }

                        val points = displayItems.mapIndexed { i, item ->
                            Offset(i * columnWidthPx + columnWidthPx / 2f, tempY(item.tempC))
                        }

                        val tempPath = Path().apply {
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

                        val fillPath = Path().apply {
                            addPath(tempPath)
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
                            path = tempPath,
                            color = primaryColor,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )

                        val precipAreaTop = chartHeight + 10.dp.toPx()
                        val maxBarHeight = size.height - precipAreaTop - 24.dp.toPx()

                        displayItems.forEachIndexed { i, item ->
                            val xCenter = i * columnWidthPx + columnWidthPx / 2f
                            val isSelected = (i == selectedIndex)

                            drawCircle(
                                color = if (isSelected) primaryColor else surfaceColor,
                                radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                                center = points[i]
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = surfaceColor,
                                    radius = 2.5.dp.toPx(),
                                    center = points[i]
                                )
                            }

                            if (item.precipProb > 0) {
                                val barH = (item.precipProb / 100f) * maxBarHeight
                                val barW = 14.dp.toPx()
                                drawRoundRect(
                                    color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.45f),
                                    topLeft = Offset(xCenter - barW / 2f, size.height - 24.dp.toPx() - barH),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }

                        if (selectedIndex in points.indices) {
                            val selectedX = points[selectedIndex].x
                            drawLine(
                                color = scrubberColor.copy(alpha = 0.7f),
                                start = Offset(selectedX, 8.dp.toPx()),
                                end = Offset(selectedX, size.height - 18.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        displayItems.forEachIndexed { index, item ->
                            val isSelected = (index == selectedIndex)
                            Column(
                                modifier = Modifier
                                    .width(columnWidthDp)
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        selectHourWithHaptic(index)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = getLocalizedTimeLabel(item.timeLabel, language),
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) primaryColor else onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = "${temperatureUnit.convert(item.tempC)}°",
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) primaryColor else onSurface
                                )

                                Spacer(modifier = Modifier.height(30.dp))

                                Text(
                                    text = "${item.precipProb}%",
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 10.sp,
                                    fontWeight = if (item.precipProb > 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (item.precipProb > 0) primaryColor else onSurfaceVariant.copy(alpha = 0.65f)
                                )

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
 * 4. 生活指数（服装・傘・洗濯・UV・換気）
 */
@Composable
private fun LifestyleAdvisorSection(
    weather: WeatherData,
    selectedDay: DailyForecast?,
    isToday: Boolean,
    temperatureUnit: TemperatureUnit,
    language: AppLanguage
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = language.strings

    val day = selectedDay ?: weather.dailyForecast.firstOrNull() ?: return
    val tempForClothing = if (isToday) weather.currentTempC else day.maxTempC
    val precipProb = if (isToday) weather.precipitationProbabilityMax else day.precipProb
    val precipMm = if (isToday) weather.precipitationSumMm else day.precipitationSumMm
    val humidity = if (isToday) weather.humidity else (day.humidityAvg?.takeIf { day.humidityAvgSource == "Open-Meteo" } ?: weather.humidity)
    val uv = if (isToday) weather.uvIndex else day.uvMax.toInt()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = strings.lifestyleAdvisorTitle,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LifestyleAdviceCard(
                title = strings.clothingTitle,
                advice = strings.clothingTip(tempForClothing),
                icon = Icons.Rounded.Checkroom,
                accentColor = colorScheme.primary
            )

            LifestyleAdviceCard(
                title = strings.umbrellaAdviceTitle,
                advice = strings.umbrellaTip(precipProb, precipMm),
                icon = Icons.Rounded.Umbrella,
                accentColor = Color(0xFF42A5F5)
            )

            LifestyleAdviceCard(
                title = strings.laundryAdviceTitle,
                advice = strings.laundryTip(humidity, precipProb),
                icon = Icons.Rounded.DryCleaning,
                accentColor = Color(0xFF26A69A)
            )

            LifestyleAdviceCard(
                title = strings.uvAdviceTitle,
                advice = strings.uvTip(uv),
                icon = Icons.Rounded.LightMode,
                accentColor = Color(0xFFFFB300)
            )

            LifestyleAdviceCard(
                title = strings.ventilationAdviceTitle,
                advice = strings.ventilationTip(humidity, weather.airQuality.aqi),
                icon = Icons.Rounded.Air,
                accentColor = Color(0xFF7E57C2)
            )
        }
    }
}

@Composable
private fun LifestyleAdviceCard(
    title: String,
    advice: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.16f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = advice,
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 5. 詳細気象メトリクス・グリッド
 */
@Composable
private fun DetailedWeatherMetricsSection(
    weather: WeatherData,
    selectedDay: DailyForecast?,
    isToday: Boolean,
    temperatureUnit: TemperatureUnit,
    language: AppLanguage,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = language.strings

    val day = selectedDay ?: weather.dailyForecast.firstOrNull() ?: return
    val humidity = if (isToday) weather.humidity else (day.humidityAvg?.takeIf { day.humidityAvgSource == "Open-Meteo" } ?: weather.humidity)
    val dewPointC = if (isToday) weather.dewPointC else (day.minTempC - 3)
    val dewPointFormatted = temperatureUnit.convert(dewPointC)
    val pressure = if (isToday) weather.pressureHpa else day.pressureHpa
    val windMs = if (isToday) weather.windSpeedKmH / 3.6 else day.windSpeedMaxKmH / 3.6
    val gustsMs = if (isToday) weather.windGustsKmH / 3.6 else (day.windSpeedMaxKmH * 1.35) / 3.6
    val precipSum = if (isToday) weather.precipitationSumMm else day.precipitationSumMm
    val precipProb = if (isToday) weather.precipitationProbabilityMax else day.precipProb
    val aqiValue = if (weather.airQuality.isAvailable) "AQI ${weather.airQuality.aqi}" else "--"
    val aqiSub = if (weather.airQuality.isAvailable) {
        weather.airQuality.levelLabel
    } else {
        when (language) {
            AppLanguage.JAPANESE -> "データなし"
            AppLanguage.KOREAN -> "데이터 없음"
            AppLanguage.SIMPLIFIED_CHINESE -> "暂无数据"
            AppLanguage.TRADITIONAL_CHINESE -> "暫無資料"
            AppLanguage.ENGLISH -> "No Data"
            AppLanguage.SPANISH -> "Sin datos"
            AppLanguage.FRENCH -> "Aucune donnée"
            AppLanguage.PORTUGUESE -> "Sem dados"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = strings.detailedMetricsTitle,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricItemCard(
                    modifier = Modifier.weight(1f),
                    title = strings.humidity,
                    value = "$humidity%",
                    sub = "${strings.dewPointLabel}: ${dewPointFormatted}${temperatureUnit.symbol}",
                    icon = Icons.Rounded.WaterDrop
                )
                MetricItemCard(
                    modifier = Modifier.weight(1f),
                    title = strings.pressure,
                    value = "$pressure hPa",
                    sub = if (pressure >= 1013) strings.pressureNormal else strings.pressureLow,
                    icon = Icons.Rounded.Compress
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricItemCard(
                    modifier = Modifier.weight(1f),
                    title = strings.visibility,
                    value = String.format(Locale.US, "%.1f km", weather.visibilityKm),
                    sub = if (weather.visibilityKm >= 10.0) strings.visibilityClear else strings.visibilityGood,
                    icon = Icons.Rounded.Visibility
                )
                MetricItemCard(
                    modifier = Modifier.weight(1f),
                    title = strings.airQuality,
                    value = aqiValue,
                    sub = aqiSub,
                    icon = Icons.Rounded.Air
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val windSub = if (isToday) {
                    "${weather.windDirection} • ${strings.maxGusts} ${windSpeedUnit.formatFromMs(gustsMs, language)}"
                } else {
                    "${strings.maxGusts} ${windSpeedUnit.formatFromMs(gustsMs, language)}"
                }
                MetricItemCard(
                    modifier = Modifier.weight(1f),
                    title = strings.wind,
                    value = windSpeedUnit.formatFromMs(windMs, language),
                    sub = windSub,
                    icon = Icons.Rounded.NearMe
                )
                MetricItemCard(
                    modifier = Modifier.weight(1f),
                    title = strings.dailyPrecipitationSum,
                    value = String.format(Locale.US, "%.1f mm", precipSum),
                    sub = "${strings.todayTotalPrecip} • ${strings.maxPrecipChance(precipProb)}",
                    icon = Icons.Rounded.Opacity
                )
            }
        }
    }
}

@Composable
private fun MetricItemCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                fontFamily = GoogleSansFlex,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                text = sub,
                fontFamily = GoogleSansFlex,
                fontSize = 11.5.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 日別の気象状況からインテリジェントなサマリー解説文を生成する
 */
private fun generateDaySmartSummary(
    day: DailyForecast,
    isToday: Boolean,
    weather: WeatherData,
    language: AppLanguage
): String {
    val maxProb = day.precipProb
    val sumMm = day.precipitationSumMm
    val maxTemp = day.maxTempC

    if (isToday && !weather.jmaOverviewText.isNullOrBlank() && language == AppLanguage.JAPANESE) {
        return weather.jmaOverviewText.replace("\n\n", " ").replace("\n", " ").trim()
    }

    return when (language) {
        AppLanguage.JAPANESE -> when {
            maxProb >= 60 || sumMm >= 5.0 ->
                "まとまった雨が予想されます。外出時は傘や雨具を必ず携帯してください。"
            maxProb in 25..59 ->
                "一時的な通り雨やにわか雨の可能性があります。折りたたみ傘があると安心です。"
            maxTemp >= 28 ->
                "日中は気温が上昇し夏日となる見込みです。こまめな水分補給と熱中症対策をお心がけください。"
            maxTemp <= 12 ->
                "一日を通して冷え込みが強まります。厚手の上着やマフラーで十分な防寒を行ってください。"
            else ->
                "穏やかで過ごしやすい天候の予報です。お出かけやお洗濯にも適した心地よい一日となりそうです。"
        }
        AppLanguage.KOREAN -> when {
            maxProb >= 60 || sumMm >= 5.0 ->
                "제법 많은 비가 예상됩니다. 외출 시 우산이나 우비를 꼭 챙기세요."
            maxProb in 25..59 ->
                "한때 소나기가 지날 가능성이 있습니다. 접이식 우산을 챙기면 안심입니다."
            maxTemp >= 28 ->
                "낮 동안 기온이 크게 올라 더울 전망입니다. 수분을 자주 섭취하고 더위에 대비하세요."
            maxTemp <= 12 ->
                "하루 종일 쌀쌀하겠습니다. 두꺼운 겉옷과 목도리로 따뜻하게 입으세요."
            else ->
                "온화하고 쾌적한 날씨가 예상됩니다. 외출이나 빨래하기 좋은 하루가 되겠습니다."
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when {
            maxProb >= 60 || sumMm >= 5.0 ->
                "预计有较强降雨，外出请务必携带雨具。"
            maxProb in 25..59 ->
                "可能出现局部短暂阵雨，建议随身携带折叠伞。"
            maxTemp >= 28 ->
                "白天气温较高，请注意防晒并适时补充水分。"
            maxTemp <= 12 ->
                "全天气温偏低偏凉，请穿厚外套做好防寒保暖。"
            else ->
                "天气温和舒适，非常适合出行和户外活动。"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when {
            maxProb >= 60 || sumMm >= 5.0 ->
                "預計有較大雨勢，外出請務必攜帶雨具。"
            maxProb in 25..59 ->
                "可能出現局部短暫陣雨，建議隨身攜帶折疊傘。"
            maxTemp >= 28 ->
                "白天氣溫較高，請注意防曬並適時補充水分。"
            maxTemp <= 12 ->
                "全天氣溫偏低偏涼，請穿厚外套做好防寒保暖。"
            else ->
                "天氣溫和舒適，非常適合外出與戶外活動。"
        }
        AppLanguage.ENGLISH -> when {
            maxProb >= 60 || sumMm >= 5.0 ->
                "Periods of rain expected. Be sure to carry an umbrella or rain gear."
            maxProb in 25..59 ->
                "Scattered passing showers possible. A compact umbrella is recommended."
            maxTemp >= 28 ->
                "Warm conditions with high daytime temperatures. Stay hydrated and mind the sun."
            maxTemp <= 12 ->
                "Chilly conditions throughout the day. Layer up with a warm coat or jacket."
            else ->
                "Mild and pleasant conditions forecast. A great day for outdoor plans and activities."
        }
        AppLanguage.SPANISH -> when {
            maxProb >= 60 || sumMm >= 5.0 ->
                "Se esperan lluvias frecuentes. No olvides llevar paraguas o ropa impermeable."
            maxProb in 25..59 ->
                "Posibles chubascos aislados. Se recomienda llevar un paraguas plegable."
            maxTemp >= 28 ->
                "Jornada calurosa con temperaturas altas durante el día. Hidrátate bien y protégete del sol."
            maxTemp <= 12 ->
                "Ambiente frío durante todo el día. Abrígate bien con un abrigo o chaqueta cálida."
            else ->
                "Se prevé un tiempo templado y agradable. Un gran día para planes y actividades al aire libre."
        }
        AppLanguage.FRENCH -> when {
            maxProb >= 60 || sumMm >= 5.0 ->
                "Des épisodes de pluie sont attendus. Pensez à prendre un parapluie ou un vêtement imperméable."
            maxProb in 25..59 ->
                "Averses passagères possibles. Un parapluie pliable est recommandé."
            maxTemp >= 28 ->
                "Temps chaud avec des températures élevées en journée. Hydratez-vous régulièrement et protégez-vous du soleil."
            maxTemp <= 12 ->
                "Temps frais tout au long de la journée. Couvrez-vous bien avec un manteau ou une veste chaude."
            else ->
                "Temps doux et agréable prévu. Une belle journée pour vos sorties et activités extérieures."
        }
        AppLanguage.PORTUGUESE -> when {
            maxProb >= 60 || sumMm >= 5.0 ->
                "Previsão de períodos de chuva. Não deixe de levar guarda-chuva ou capa de chuva."
            maxProb in 25..59 ->
                "Possibilidade de pancadas isoladas. Recomenda-se levar um guarda-chuva compacto."
            maxTemp >= 28 ->
                "Tempo quente com temperaturas elevadas ao longo do dia. Hidrate-se bem e proteja-se do sol."
            maxTemp <= 12 ->
                "Tempo frio durante todo o dia. Agasalhe-se bem com casaco ou jaqueta quente."
            else ->
                "Previsão de tempo ameno e agradável. Um ótimo dia para passeios e atividades ao ar livre."
        }
    }
}
