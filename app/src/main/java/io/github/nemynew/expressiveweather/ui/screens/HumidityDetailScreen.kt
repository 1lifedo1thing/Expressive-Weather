package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.DrynessIndexCalculator
import io.github.nemynew.expressiveweather.data.DrynessIndexResult
import io.github.nemynew.expressiveweather.data.DrynessRank
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.MaterialYouAccentPalette
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.theme.accentColor
import io.github.nemynew.expressiveweather.theme.currentAccentPalette
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 湿度（Humidity）詳細画面
 * Material 3 Expressive 準拠（枠線なし、トナルサーフェス、8言語対応）
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HumidityDetailScreen(
    weather: WeatherData?,
    city: City,
    language: AppLanguage,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    weatherAdaptiveThemeEnabled: Boolean = true,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    themeMode: ThemeMode = ThemeMode.WEATHER,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = language.strings
    val humidity = weather?.humidity ?: 50
    val currentTempC = weather?.currentTempC ?: 20
    val currentWindKmH = weather?.windSpeedKmH ?: 0.0
    val dewPointC = weather?.dewPointC ?: (weather?.currentTempC?.minus(4) ?: 15)
    val dewPointDisplay = temperatureUnit.convert(dewPointC)
    val hourlyForecast = weather?.hourlyForecast ?: emptyList()

    val currentDryness = remember(currentTempC, humidity, currentWindKmH) {
        DrynessIndexCalculator.calculateFromKmH(
            tempC = currentTempC.toDouble(),
            relativeHumidity = humidity.toDouble(),
            windSpeedKmH = currentWindKmH
        )
    }
    val hourlyDrynessList = remember(hourlyForecast) {
        hourlyForecast.take(24).map { item ->
            item to DrynessIndexCalculator.calculateFromKmH(
                tempC = item.tempC.toDouble(),
                relativeHumidity = item.humidity.toDouble(),
                windSpeedKmH = item.windSpeedKmH
            )
        }
    }

    val relativeHumidityLabel = when (language) {
        AppLanguage.JAPANESE -> "相対湿度"
        AppLanguage.KOREAN -> "상대 습도"
        AppLanguage.SIMPLIFIED_CHINESE -> "相对湿度"
        AppLanguage.TRADITIONAL_CHINESE -> "相對濕度"
        AppLanguage.ENGLISH -> "Relative Humidity"
        AppLanguage.SPANISH -> "Humedad relativa"
        AppLanguage.FRENCH -> "Humidité relative"
        AppLanguage.PORTUGUESE -> "Umidade relativa"
    }

    val dewPointLabel = when (language) {
        AppLanguage.JAPANESE -> "現在の露点温度"
        AppLanguage.KOREAN -> "현재 이슬점 온도"
        AppLanguage.SIMPLIFIED_CHINESE -> "当前露点温度"
        AppLanguage.TRADITIONAL_CHINESE -> "當前露點溫度"
        AppLanguage.ENGLISH -> "Current Dew Point"
        AppLanguage.SPANISH -> "Punto de rocío actual"
        AppLanguage.FRENCH -> "Point de rosée actuel"
        AppLanguage.PORTUGUESE -> "Ponto de orvalho atual"
    }

    val hourlyHumidityTitle = when (language) {
        AppLanguage.JAPANESE -> "本日の湿度・乾燥推移"
        AppLanguage.KOREAN -> "오늘의 습도·건조 추이"
        AppLanguage.SIMPLIFIED_CHINESE -> "今日湿度与干燥走势"
        AppLanguage.TRADITIONAL_CHINESE -> "今日濕度與乾燥走勢"
        AppLanguage.ENGLISH -> "Hourly Humidity & Dryness"
        AppLanguage.SPANISH -> "Humedad y sequedad por hora"
        AppLanguage.FRENCH -> "Humidité et sécheresse horaire"
        AppLanguage.PORTUGUESE -> "Umidade e secura por hora"
    }

    val adviceSectionTitle = when (language) {
        AppLanguage.JAPANESE -> "室内環境・快適性ガイド"
        AppLanguage.KOREAN -> "실내 환경·쾌적도 가이드"
        AppLanguage.SIMPLIFIED_CHINESE -> "室内环境与舒适度指南"
        AppLanguage.TRADITIONAL_CHINESE -> "室內環境與舒適度指南"
        AppLanguage.ENGLISH -> "Indoor Comfort & Wellness Guide"
        AppLanguage.SPANISH -> "Guía de confort y bienestar interior"
        AppLanguage.FRENCH -> "Guide confort et bien-être intérieur"
        AppLanguage.PORTUGUESE -> "Guia de conforto e bem-estar interno"
    }

    DetailScaffold(
        title = strings.humidity,
        city = city,
        weather = weather,
        language = language,
        metricType = DetailMetricType.HUMIDITY,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        val expressHaptics = LocalExpressiveHaptics.current
        val colorScheme = MaterialTheme.colorScheme
        val accentPalette = currentAccentPalette()
        val isColorful = accentPalette.mode.isColorful
        val (humidityLabel, humidityColor, humidityAdvice) = getHumidityInfo(humidity, language, accentPalette)

        val waveShape = MaterialShapes.Cookie9Sided.toShape()
        val rippleColor = if (isColorful) accentPalette.humidity.accent else colorScheme.secondary
        val rippleContainerColor = if (isColorful) accentPalette.humidity.container else colorScheme.secondaryContainer
        val onRippleColor = if (isColorful) accentPalette.humidity.onContainer else colorScheme.onSecondaryContainer
        val humidityIconColor = if (isColorful) accentPalette.humidity.accent else colorScheme.secondary

        val (humidityPillBg, humidityPillText) = remember(humidity, isColorful, colorScheme, accentPalette) {
            if (isColorful) {
                val role = when {
                    humidity < 30 -> accentPalette.dryness(4)
                    humidity < 40 -> accentPalette.dryness(2)
                    humidity <= 60 -> accentPalette.dryness(1)
                    humidity <= 75 -> accentPalette.visibility
                    else -> accentPalette.humidity
                }
                role.accent to role.onAccent
            } else {
                when {
                    humidity < 30 -> colorScheme.errorContainer to colorScheme.onErrorContainer
                    humidity < 40 -> colorScheme.surfaceContainerHighest to colorScheme.onSurface
                    humidity <= 60 -> colorScheme.tertiary to colorScheme.onTertiary
                    humidity <= 75 -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
                    else -> colorScheme.primary to colorScheme.onPrimary
                }
            }
        }

        val rippleTransition = rememberInfiniteTransition(label = "DetailHumidityRipple")
        val outerScale by rippleTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DetailHumidityOuterScale"
        )
        val midScale by rippleTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DetailHumidityMidScale"
        )
        val innerScale by rippleTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DetailHumidityInnerScale"
        )
        val wobbleAngle by rippleTransition.animateFloat(
            initialValue = -2.5f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DetailHumidityWobble"
        )

        // ================= 1. ヒーローセクション: 湿度タイルと同一の多層リップル & 露点温度 =================
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fluent_weather_humidity_24),
                        contentDescription = null,
                        tint = humidityIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = relativeHumidityLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 湿度タイル (HumidityCard) と同一の3層 Cookie9Sided リップルシェイプ + 湿度数値
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(188.dp)
                ) {
                    Surface(
                        shape = waveShape,
                        color = rippleColor.copy(alpha = if (isColorful) 0.22f else 0.20f),
                        modifier = Modifier
                            .size(180.dp)
                            .graphicsLayer {
                                rotationZ = wobbleAngle * 0.7f
                                scaleX = outerScale
                                scaleY = outerScale
                            }
                    ) {}
                    Surface(
                        shape = waveShape,
                        color = rippleColor.copy(alpha = if (isColorful) 0.44f else 0.40f),
                        modifier = Modifier
                            .size(156.dp)
                            .graphicsLayer {
                                rotationZ = -wobbleAngle * 0.8f
                                scaleX = midScale
                                scaleY = midScale
                            }
                    ) {}
                    Surface(
                        shape = waveShape,
                        color = rippleContainerColor,
                        modifier = Modifier
                            .size(132.dp)
                            .graphicsLayer {
                                rotationZ = wobbleAngle
                                scaleX = innerScale
                                scaleY = innerScale
                            }
                    ) {}
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$humidity",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 44.sp,
                            lineHeight = 44.sp,
                            color = onRippleColor,
                            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum")
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "%",
                            fontFamily = GoogleSansFlex,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = onRippleColor.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = CircleShape,
                    color = humidityPillBg
                ) {
                    Text(
                        text = humidityLabel,
                        color = humidityPillText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 湿度プログレスバー
                val animatedProgress by animateFloatAsState(
                    targetValue = (humidity / 100f).coerceIn(0.05f, 1f),
                    animationSpec = tween(1000, easing = FastOutSlowInEasing),
                    label = "HumidityProgress"
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(humidityColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(when (language) {
                            AppLanguage.JAPANESE -> "0% (乾燥)"
                            AppLanguage.KOREAN -> "0% (건조)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "0% (干燥)"
                            AppLanguage.TRADITIONAL_CHINESE -> "0% (乾燥)"
                            AppLanguage.ENGLISH -> "0% (Dry)"
                            AppLanguage.SPANISH -> "0% (Seco)"
                            AppLanguage.FRENCH -> "0% (Sec)"
                            AppLanguage.PORTUGUESE -> "0% (Seco)"
                        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(when (language) {
                            AppLanguage.JAPANESE -> "40-60% (快適ゾーン)"
                            AppLanguage.KOREAN -> "40-60% (쾌적 구간)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "40-60% (适宜舒适区)"
                            AppLanguage.TRADITIONAL_CHINESE -> "40-60% (適宜舒適區)"
                            AppLanguage.ENGLISH -> "40-60% (Comfort Zone)"
                            AppLanguage.SPANISH -> "40-60% (Zona óptima)"
                            AppLanguage.FRENCH -> "40-60% (Zone confort)"
                            AppLanguage.PORTUGUESE -> "40-60% (Zona ideal)"
                        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(when (language) {
                            AppLanguage.JAPANESE -> "100% (多湿)"
                            AppLanguage.KOREAN -> "100% (다습)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "100% (潮湿)"
                            AppLanguage.TRADITIONAL_CHINESE -> "100% (潮濕)"
                            AppLanguage.ENGLISH -> "100% (Humid)"
                            AppLanguage.SPANISH -> "100% (Húmedo)"
                            AppLanguage.FRENCH -> "100% (Humide)"
                            AppLanguage.PORTUGUESE -> "100% (Úmido)"
                        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 露点温度サブタイル
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Thermostat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dewPointLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${dewPointDisplay}${temperatureUnit.symbol}",
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = humidityAdvice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontFamily = GoogleSansFlex,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // ================= 2. 乾燥スコア（容積絶対湿度＋風速・湿度補正） =================
        DrynessScoreCard(
            result = currentDryness,
            language = language
        )

        // ================= 3. 24時間 湿度・乾燥スコア推移 =================
        var showDrynessHourly by rememberSaveable { mutableStateOf(false) }
        val peakDrynessEntry = remember(hourlyDrynessList) {
            hourlyDrynessList.maxByOrNull { it.second.score }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
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
                    Text(
                        text = hourlyHumidityTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HourlyMetricToggle(
                        showDryness = showDrynessHourly,
                        language = language,
                        onSelectDryness = { selected ->
                            if (showDrynessHourly != selected) {
                                expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                                showDrynessHourly = selected
                            }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = showDrynessHourly && peakDrynessEntry != null,
                    enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(180, easing = FastOutLinearInEasing)) + fadeOut(tween(120))
                ) {
                    peakDrynessEntry?.let { (peakItem, peakDryness) ->
                        val peakColor = peakDryness.rank.accentColor(accentPalette)
                        val peakTime = getLocalizedTimeLabel(peakItem.timeLabel, language)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(peakColor.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (language) {
                                    AppLanguage.JAPANESE -> "本日の最大乾燥 ($peakTime)"
                                    AppLanguage.KOREAN -> "오늘 최대 건조 ($peakTime)"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "今日最干燥时段 ($peakTime)"
                                    AppLanguage.TRADITIONAL_CHINESE -> "今日最乾燥時段 ($peakTime)"
                                    AppLanguage.ENGLISH -> "Today's Peak Dryness ($peakTime)"
                                    AppLanguage.SPANISH -> "Máxima sequedad hoy ($peakTime)"
                                    AppLanguage.FRENCH -> "Pic de sécheresse ($peakTime)"
                                    AppLanguage.PORTUGUESE -> "Pico de secura hoje ($peakTime)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${peakDryness.score} · ${peakDryness.rank.label(language)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = peakColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(hourlyDrynessList) { (item, itemDryness) ->
                        val valueFraction = if (showDrynessHourly) {
                            (itemDryness.score / 100f).coerceIn(0.08f, 1f)
                        } else {
                            (item.humidity / 100f).coerceIn(0.1f, 1f)
                        }
                        val col = if (showDrynessHourly) {
                            itemDryness.rank.accentColor(accentPalette)
                        } else {
                            getHumidityColor(item.humidity, accentPalette)
                        }
                        val valueText = if (showDrynessHourly) "${itemDryness.score}" else "${item.humidity}%"

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(44.dp)
                        ) {
                            Text(
                                text = getLocalizedTimeLabel(item.timeLabel, language),
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // 縦バー
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((60 * valueFraction).dp)
                                        .clip(CircleShape)
                                        .background(col)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = valueText,
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = col,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ================= 4. 湿度と快適・生活アドバイス =================
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
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = adviceSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val isHighDryness = currentDryness.score >= 60
                val isModerateDryness = currentDryness.score >= 40 || humidity < 40

                val (respiratoryTitle, respiratoryDesc) = when (language) {
                    AppLanguage.JAPANESE -> "呼吸器・健康管理" to when {
                        isHighDryness -> "乾燥スコアが高い状態です（${currentDryness.score}点）。粘膜が乾きやすく感染症リスクが高まるため、加湿器で室内40〜60%を保ち、マスクやこまめな水分補給を心がけましょう。"
                        isModerateDryness -> "空気が乾燥し始めています。加湿器等で室内湿度を40〜60%に保つことで粘膜を保護し、喉の不調を予防できます。"
                        else -> "理想的な潤いがあり、呼吸器への負担が少なく快適に過ごせます。"
                    }
                    AppLanguage.KOREAN -> "호흡기·건강 관리" to when {
                        isHighDryness -> "건조 지수가 높습니다 (${currentDryness.score}점). 호흡기 점막이 쉽게 건조해지므로 가습기로 실내 습도 40~60%를 유지하고 수분을 자주 섭취하세요."
                        isModerateDryness -> "공기가 건조해지고 있습니다. 가습기 등으로 실내 습도를 40~60%로 유지해 호흡기 점막을 보호하세요."
                        else -> "쾌적한 습도로 호흡기 부담 없이 편안하게 지낼 수 있습니다."
                    }
                    AppLanguage.SIMPLIFIED_CHINESE -> "呼吸健康关怀" to when {
                        isHighDryness -> "当前干燥指数偏高（${currentDryness.score}分），呼吸道黏膜易干燥，建议使用加湿器将室内湿度保持在40–60%并多饮水。"
                        isModerateDryness -> "空气开始干燥，适宜使用加湿设备将湿度维持在40–60%，保护呼吸道黏膜。"
                        else -> "空气湿润度适宜，呼吸舒适。"
                    }
                    AppLanguage.TRADITIONAL_CHINESE -> "呼吸健康關懷" to when {
                        isHighDryness -> "目前乾燥指數偏高（${currentDryness.score}分），呼吸道黏膜易乾燥，建議使用加濕器將室內濕度維持在40–60%並多補充水分。"
                        isModerateDryness -> "空氣開始乾燥，適宜使用加濕設備將濕度維持在40–60%，保護呼吸道黏膜。"
                        else -> "空氣濕潤度適宜，呼吸舒適。"
                    }
                    AppLanguage.ENGLISH -> "Respiratory Health" to when {
                        isHighDryness -> "High dryness score (${currentDryness.score}/100). Air holds very little moisture—use a humidifier to keep indoor RH at 40–60% and stay hydrated."
                        isModerateDryness -> "Air is getting dry. Keeping indoor humidity between 40–60% helps protect mucosal tissues and throat comfort."
                        else -> "Healthy moisture levels promote comfortable breathing."
                    }
                    AppLanguage.SPANISH -> "Salud respiratoria" to when {
                        isHighDryness -> "Índice de sequedad alto (${currentDryness.score}/100). Usa humidificador para mantener la humedad interior entre 40% y 60% e hidrátate con frecuencia."
                        isModerateDryness -> "El aire comienza a estar seco. Mantener la humedad interior entre 40% y 60% ayuda a proteger las mucosas."
                        else -> "Niveles saludables de humedad que favorecen una respiración cómoda."
                    }
                    AppLanguage.FRENCH -> "Santé respiratoire" to when {
                        isHighDryness -> "Indice de sécheresse élevé (${currentDryness.score}/100). Utilisez un humidificateur pour maintenir 40 à 60 % d'humidité intérieure et hydratez-vous souvent."
                        isModerateDryness -> "L'air devient sec. Maintenir l'humidité intérieure entre 40 et 60 % aide à protéger les muqueuses."
                        else -> "Un taux d'humidité sain favorise une respiration confortable."
                    }
                    AppLanguage.PORTUGUESE -> "Saúde respiratória" to when {
                        isHighDryness -> "Índice de secura alto (${currentDryness.score}/100). Use umidificador para manter o ambiente entre 40% e 60% e beba água com frequência."
                        isModerateDryness -> "O ar está ficando seco. Manter a umidade interna entre 40% e 60% ajuda a proteger as mucosas."
                        else -> "Níveis saudáveis de umidade favorecem uma respiração confortável."
                    }
                }

                val (moldTitle, moldDesc) = when (language) {
                    AppLanguage.JAPANESE -> "カビ・結露の防止" to if (humidity > 65) "湿度が高めです。部屋の換気やエアコン除湿を活用し、カビの発生を抑えましょう。" else "カビや結露の心配は少ない良好な状態です。"
                    AppLanguage.KOREAN -> "곰팡이·결로 예방" to if (humidity > 65) "습도가 높습니다. 실내 환기나 제습기를 활용해 곰팡이와 결로를 예방하세요." else "곰팡이나 결로 걱정이 적은 양호한 상태입니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "防潮防霉管理" to if (humidity > 65) "湿度偏高，注意开窗通风或使用抽湿机，以减少霉菌滋生。" else "湿度适宜，无需担心过度潮湿与霉菌风险。"
                    AppLanguage.TRADITIONAL_CHINESE -> "防潮防霉管理" to if (humidity > 65) "濕度偏高，注意開窗通風或使用抽濕機，以減少黴菌滋生。" else "濕度適宜，無需擔心過度潮濕與黴菌風險。"
                    AppLanguage.ENGLISH -> "Moisture & Mold Prevention" to if (humidity > 65) "High humidity. Maintain proper ventilation or use dehumidifiers to prevent mold." else "Low risk of mold and condensation."
                    AppLanguage.SPANISH -> "Prevención de moho y humedad" to if (humidity > 65) "Humedad elevada. Ventila bien o usa un deshumidificador para evitar el moho." else "Bajo riesgo de moho y condensación."
                    AppLanguage.FRENCH -> "Prévention moisissures et condensation" to if (humidity > 65) "Humidité élevée. Aérez régulièrement ou utilisez un déshumidificateur contre les moisissures." else "Faible risque de moisissures et de condensation."
                    AppLanguage.PORTUGUESE -> "Prevenção de mofo e umidade" to if (humidity > 65) "Umidade alta. Mantenha o ambiente ventilado ou use desumidificador para evitar mofo." else "Baixo risco de mofo e condensação."
                }

                val (skinTitle, skinDesc) = when (language) {
                    AppLanguage.JAPANESE -> "肌の水分バランス" to when {
                        isHighDryness -> "冷気や風により肌の水分が急速に奪われやすい状態です。外出前の保湿クリームやリップケアをしっかり行いましょう。"
                        isModerateDryness -> "肌や唇が乾燥しやすいため、こまめな保湿ケアや十分な水分補給が効果的です。"
                        else -> "肌に適度な潤いが保たれやすい良好な状態です。"
                    }
                    AppLanguage.KOREAN -> "피부 보습 관리" to when {
                        isHighDryness -> "찬 공기와 바람으로 피부 수분이 빠르게 증발합니다. 외출 전 보습 크림과 립밤으로 꼼꼼히 관리하세요."
                        isModerateDryness -> "피부와 입술이 건조해지기 쉬우니 보습제를 바르고 수분을 충분히 섭취하세요."
                        else -> "피부에 적당한 수분이 유지되기 좋은 상태입니다."
                    }
                    AppLanguage.SIMPLIFIED_CHINESE -> "肌肤保湿护理" to when {
                        isHighDryness -> "低湿或强风会加速肌肤水分流失，外出前请做好面霜与护唇保湿。"
                        isModerateDryness -> "空气水分偏低，注意补水护肤及随身饮水。"
                        else -> "空气滋润，肌肤能维持良好的水油平衡。"
                    }
                    AppLanguage.TRADITIONAL_CHINESE -> "肌膚保濕護理" to when {
                        isHighDryness -> "低濕或強風會加速肌膚水分流失，外出前請做好乳霜與護唇保濕。"
                        isModerateDryness -> "空氣水分偏低，注意補水護膚及隨身飲水。"
                        else -> "空氣滋潤，肌膚能維持良好的水油平衡。"
                    }
                    AppLanguage.ENGLISH -> "Skin Hydration" to when {
                        isHighDryness -> "Dry air and wind strip moisture quickly. Apply rich moisturizer and lip balm before heading outdoors."
                        isModerateDryness -> "Skin and lips may dry out faster than usual. Moisturize regularly and stay hydrated."
                        else -> "Atmospheric moisture naturally maintains skin hydration."
                    }
                    AppLanguage.SPANISH -> "Hidratación de la piel" to when {
                        isHighDryness -> "El aire seco y el viento deshidratan la piel rápidamente. Aplica crema hidratante y bálsamo labial antes de salir."
                        isModerateDryness -> "La piel y los labios pueden resecarse con facilidad. Usa crema hidratante y bebe agua."
                        else -> "La humedad ambiental ayuda a mantener la piel naturalmente hidratada."
                    }
                    AppLanguage.FRENCH -> "Hydratation de la peau" to when {
                        isHighDryness -> "L'air sec et le vent déshydratent vite la peau. Appliquez une crème riche et un baume à lèvres avant de sortir."
                        isModerateDryness -> "La peau et les lèvres peuvent s'assécher. Hydratez régulièrement et buvez suffisamment."
                        else -> "L'humidité ambiante aide à préserver naturellement l'hydratation cutanée."
                    }
                    AppLanguage.PORTUGUESE -> "Hidratação da pele" to when {
                        isHighDryness -> "O ar seco e o vento retiram a umidade da pele rapidamente. Aplique hidratante e protetor labial antes de sair."
                        isModerateDryness -> "A pele e os lábios podem ressecar com facilidade. Use hidratante e beba bastante água."
                        else -> "A umidade do ar ajuda a manter a pele naturalmente hidratada."
                    }
                }

                HumidityAdviceRow(title = respiratoryTitle, desc = respiratoryDesc)
                HumidityAdviceRow(title = moldTitle, desc = moldDesc)
                HumidityAdviceRow(title = skinTitle, desc = skinDesc)
            }
        }
    }
}

@Composable
private fun DrynessScoreCard(
    result: DrynessIndexResult,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentPalette = currentAccentPalette()
    val expressHaptics = LocalExpressiveHaptics.current
    val rankColor = result.rank.accentColor(accentPalette)
    var showRankGuide by rememberSaveable { mutableStateOf(false) }
    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasEntered = true }

    val animatedScoreFraction by animateFloatAsState(
        targetValue = if (hasEntered) (result.score / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "DrynessScoreProgress"
    )
    val animatedDisplayScore = (animatedScoreFraction * 100f).roundToInt().coerceIn(0, 100)

    val cardTitle = when (language) {
        AppLanguage.JAPANESE -> "乾燥スコア"
        AppLanguage.KOREAN -> "건조 지수"
        AppLanguage.SIMPLIFIED_CHINESE -> "干燥指数"
        AppLanguage.TRADITIONAL_CHINESE -> "乾燥指數"
        AppLanguage.ENGLISH -> "Dryness Score"
        AppLanguage.SPANISH -> "Índice de sequedad"
        AppLanguage.FRENCH -> "Indice de sécheresse"
        AppLanguage.PORTUGUESE -> "Índice de secura"
    }

    val vhLabel = when (language) {
        AppLanguage.JAPANESE -> "容積絶対湿度"
        AppLanguage.KOREAN -> "용적 절대습도"
        AppLanguage.SIMPLIFIED_CHINESE -> "容积绝对湿度"
        AppLanguage.TRADITIONAL_CHINESE -> "容積絕對濕度"
        AppLanguage.ENGLISH -> "Absolute Humidity"
        AppLanguage.SPANISH -> "Humedad absoluta"
        AppLanguage.FRENCH -> "Humidité absolue"
        AppLanguage.PORTUGUESE -> "Umidade absoluta"
    }

    val baseLabel = when (language) {
        AppLanguage.JAPANESE -> "基礎点"
        AppLanguage.KOREAN -> "기초 점수"
        AppLanguage.SIMPLIFIED_CHINESE -> "基础分"
        AppLanguage.TRADITIONAL_CHINESE -> "基礎分"
        AppLanguage.ENGLISH -> "Base (VH)"
        AppLanguage.SPANISH -> "Base (HA)"
        AppLanguage.FRENCH -> "Base (HA)"
        AppLanguage.PORTUGUESE -> "Base (UA)"
    }

    val windCorrLabel = when (language) {
        AppLanguage.JAPANESE -> "風速補正"
        AppLanguage.KOREAN -> "풍속 보정"
        AppLanguage.SIMPLIFIED_CHINESE -> "风速修正"
        AppLanguage.TRADITIONAL_CHINESE -> "風速修正"
        AppLanguage.ENGLISH -> "Wind Adj."
        AppLanguage.SPANISH -> "Aj. viento"
        AppLanguage.FRENCH -> "Corr. vent"
        AppLanguage.PORTUGUESE -> "Aj. vento"
    }

    val rhCorrLabel = when (language) {
        AppLanguage.JAPANESE -> "湿度補正"
        AppLanguage.KOREAN -> "습도 보정"
        AppLanguage.SIMPLIFIED_CHINESE -> "湿度修正"
        AppLanguage.TRADITIONAL_CHINESE -> "濕度修正"
        AppLanguage.ENGLISH -> "RH Adj."
        AppLanguage.SPANISH -> "Aj. HR"
        AppLanguage.FRENCH -> "Corr. HR"
        AppLanguage.PORTUGUESE -> "Aj. UR"
    }

    val rankGuideTitle = when (language) {
        AppLanguage.JAPANESE -> "5段階ランクの目安"
        AppLanguage.KOREAN -> "5단계 등급 안내"
        AppLanguage.SIMPLIFIED_CHINESE -> "5级干燥等级说明"
        AppLanguage.TRADITIONAL_CHINESE -> "5級乾燥等級說明"
        AppLanguage.ENGLISH -> "5-Tier Dryness Scale"
        AppLanguage.SPANISH -> "Escala de sequedad (5 niveles)"
        AppLanguage.FRENCH -> "Échelle de sécheresse (5 niveaux)"
        AppLanguage.PORTUGUESE -> "Escala de secura (5 níveis)"
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ヘッダー：アイコン・タイトル・ランクピル
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Air,
                        contentDescription = null,
                        tint = rankColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex,
                        color = colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = rankColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = result.rank.label(language),
                        color = rankColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // メインスコア（0〜100）＆ 状態目安
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$animatedDisplayScore",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 46.sp,
                        lineHeight = 48.sp,
                        color = colorScheme.onSurface,
                        style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // 容積絶対湿度チップ
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = vhLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f g/m³", result.volumetricHumidityGm3),
                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = result.rank.guidance(language),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface,
                fontFamily = GoogleSansFlex
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5段階セグメントゲージ（20点刻み）
            DrynessSegmentedGauge(
                scoreFraction = animatedScoreFraction,
                activeColor = rankColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 算出内訳（基礎点・風速補正・湿度補正）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DrynessFactorPill(
                    label = baseLabel,
                    value = String.format(Locale.US, "%.1f", result.baseScore),
                    valueColor = colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                DrynessFactorPill(
                    label = windCorrLabel,
                    value = formatSignedDelta(result.windCorrection),
                    valueColor = if (result.windCorrection >= 0.05) accentPalette.dryness(3).accent else colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                DrynessFactorPill(
                    label = rhCorrLabel,
                    value = formatSignedDelta(result.rhCorrection),
                    valueColor = when {
                        result.rhCorrection >= 0.05 -> accentPalette.dryness(4).accent
                        result.rhCorrection <= -0.05 -> accentPalette.dryness(0).accent
                        else -> colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5段階ランク目安（折りたたみ）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                            showRankGuide = !showRankGuide
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rankGuideTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (showRankGuide) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(
                    visible = showRankGuide,
                    enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(180, easing = FastOutLinearInEasing)) + fadeOut(tween(120))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DrynessRank.entries.forEach { rank ->
                            val isCurrent = rank == result.rank
                            val tierColor = rank.accentColor(accentPalette)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) tierColor.copy(alpha = 0.14f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(tierColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = rank.rangeLabel(),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) tierColor else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(52.dp),
                                    maxLines = 1
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rank.label(language),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) tierColor else colorScheme.onSurface
                                    )
                                    Text(
                                        text = rank.guidance(language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrynessSegmentedGauge(
    scoreFraction: Float,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentPalette = currentAccentPalette()
    val surfaceColor = colorScheme.surfaceContainerHigh
    val markerCenterColor = colorScheme.surface
    val ranks = DrynessRank.entries
    val segmentColors = ranks.map { it.accentColor(accentPalette) }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
        ) {
            val barHeight = 10.dp.toPx()
            val barTop = (size.height - barHeight) / 2f
            val gap = 3.dp.toPx()
            val segCount = ranks.size
            val totalGap = gap * (segCount - 1)
            val segWidth = (size.width - totalGap) / segCount
            val radius = CornerRadius(barHeight / 2f, barHeight / 2f)

            segmentColors.forEachIndexed { index, segColor ->
                val left = index * (segWidth + gap)
                drawRoundRect(
                    color = segColor.copy(alpha = 0.85f),
                    topLeft = Offset(left, barTop),
                    size = Size(segWidth, barHeight),
                    cornerRadius = radius
                )
            }

            // 現在スコアのインジケータードット
            val markerRadius = 8.dp.toPx()
            val markerX = (scoreFraction.coerceIn(0f, 1f) * (size.width - markerRadius * 2)) + markerRadius
            val centerY = size.height / 2f

            drawCircle(
                color = surfaceColor,
                radius = markerRadius + 2.5.dp.toPx(),
                center = Offset(markerX, centerY)
            )
            drawCircle(
                color = activeColor,
                radius = markerRadius,
                center = Offset(markerX, centerY)
            )
            drawCircle(
                color = markerCenterColor,
                radius = 3.dp.toPx(),
                center = Offset(markerX, centerY)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("0", "20", "40", "60", "80", "100").forEach { tick ->
                Text(
                    text = tick,
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DrynessFactorPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HourlyMetricToggle(
    showDryness: Boolean,
    language: AppLanguage,
    onSelectDryness: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val humidityTabLabel = when (language) {
        AppLanguage.JAPANESE -> "湿度"
        AppLanguage.KOREAN -> "습도"
        AppLanguage.SIMPLIFIED_CHINESE -> "湿度"
        AppLanguage.TRADITIONAL_CHINESE -> "濕度"
        AppLanguage.ENGLISH -> "RH %"
        AppLanguage.SPANISH -> "HR %"
        AppLanguage.FRENCH -> "HR %"
        AppLanguage.PORTUGUESE -> "UR %"
    }
    val drynessTabLabel = when (language) {
        AppLanguage.JAPANESE -> "乾燥"
        AppLanguage.KOREAN -> "건조"
        AppLanguage.SIMPLIFIED_CHINESE -> "干燥"
        AppLanguage.TRADITIONAL_CHINESE -> "乾燥"
        AppLanguage.ENGLISH -> "Dryness"
        AppLanguage.SPANISH -> "Sequedad"
        AppLanguage.FRENCH -> "Sécher."
        AppLanguage.PORTUGUESE -> "Secura"
    }

    Surface(
        shape = CircleShape,
        color = colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(false to humidityTabLabel, true to drynessTabLabel).forEach { (isDrynessTab, label) ->
                val selected = showDryness == isDrynessTab
                Surface(
                    shape = CircleShape,
                    color = if (selected) colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSelectDryness(isDrynessTab) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun formatSignedDelta(value: Double): String {
    if (abs(value) < 0.05) return "±0.0"
    val sign = if (value > 0) "+" else "-"
    return String.format(Locale.US, "%s%.1f", sign, abs(value))
}

@Composable
private fun HumidityAdviceRow(title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(
            imageVector = Icons.Rounded.Opacity,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getHumidityColor(
    humidity: Int,
    palette: MaterialYouAccentPalette
): Color = when {
    humidity < 35 -> palette.dryness(3).accent
    humidity <= 60 -> palette.dryness(1).accent
    humidity <= 75 -> palette.visibility.accent
    else -> palette.humidity.accent
}

private fun getHumidityInfo(
    humidity: Int,
    language: AppLanguage,
    palette: MaterialYouAccentPalette
): Triple<String, Color, String> {
    val color = getHumidityColor(humidity, palette)
    val label = when {
        humidity < 35 -> when (language) {
            AppLanguage.JAPANESE -> "乾燥気味"
            AppLanguage.KOREAN -> "건조"
            AppLanguage.SIMPLIFIED_CHINESE -> "干燥"
            AppLanguage.TRADITIONAL_CHINESE -> "乾燥"
            AppLanguage.ENGLISH -> "Dry"
            AppLanguage.SPANISH -> "Seco"
            AppLanguage.FRENCH -> "Sec"
            AppLanguage.PORTUGUESE -> "Seco"
        }
        humidity <= 60 -> when (language) {
            AppLanguage.JAPANESE -> "快適"
            AppLanguage.KOREAN -> "쾌적"
            AppLanguage.SIMPLIFIED_CHINESE -> "舒适"
            AppLanguage.TRADITIONAL_CHINESE -> "舒適"
            AppLanguage.ENGLISH -> "Comfortable"
            AppLanguage.SPANISH -> "Agradable"
            AppLanguage.FRENCH -> "Confortable"
            AppLanguage.PORTUGUESE -> "Confortável"
        }
        humidity <= 75 -> when (language) {
            AppLanguage.JAPANESE -> "やや多湿"
            AppLanguage.KOREAN -> "다소 습함"
            AppLanguage.SIMPLIFIED_CHINESE -> "微潮"
            AppLanguage.TRADITIONAL_CHINESE -> "微潮"
            AppLanguage.ENGLISH -> "Humid"
            AppLanguage.SPANISH -> "Húmedo"
            AppLanguage.FRENCH -> "Humide"
            AppLanguage.PORTUGUESE -> "Úmido"
        }
        else -> when (language) {
            AppLanguage.JAPANESE -> "高湿度"
            AppLanguage.KOREAN -> "매우 습함"
            AppLanguage.SIMPLIFIED_CHINESE -> "高湿度"
            AppLanguage.TRADITIONAL_CHINESE -> "高濕度"
            AppLanguage.ENGLISH -> "Very Humid"
            AppLanguage.SPANISH -> "Muy húmedo"
            AppLanguage.FRENCH -> "Très humide"
            AppLanguage.PORTUGUESE -> "Muito úmido"
        }
    }

    val advice = when {
        humidity < 35 -> when (language) {
            AppLanguage.JAPANESE -> "空気が乾燥しています。水分補給や室内の加湿を心がけましょう。"
            AppLanguage.KOREAN -> "공기가 건조합니다. 수분을 충분히 섭취하고 실내 가습에 신경 쓰세요."
            AppLanguage.SIMPLIFIED_CHINESE -> "空气湿度偏低，注意多饮水并适当进行室内加湿。"
            AppLanguage.TRADITIONAL_CHINESE -> "空氣濕度偏低，注意多飲水並適當進行室內加濕。"
            AppLanguage.ENGLISH -> "Air is dry. Stay hydrated and consider using a humidifier indoors."
            AppLanguage.SPANISH -> "El aire está seco. Mantente hidratado y considera usar un humidificador en interiores."
            AppLanguage.FRENCH -> "L'air est sec. Hydratez-vous bien et envisagez un humidificateur à l'intérieur."
            AppLanguage.PORTUGUESE -> "O ar está seco. Hidrate-se bem e considere usar um umidificador em ambientes internos."
        }
        humidity <= 60 -> when (language) {
            AppLanguage.JAPANESE -> "理想的な湿度環境です。肌や喉にも優しく過ごしやすい空気です。"
            AppLanguage.KOREAN -> "이상적인 습도입니다. 피부와 호흡기 모두 편안한 쾌적한 공기입니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "湿度非常适宜，体感舒适宜人。"
            AppLanguage.TRADITIONAL_CHINESE -> "濕度非常適宜，體感舒適宜人。"
            AppLanguage.ENGLISH -> "Ideal humidity levels. Comfortable for respiratory tract and skin."
            AppLanguage.SPANISH -> "Niveles de humedad ideales. Cómodo para la piel y las vías respiratorias."
            AppLanguage.FRENCH -> "Taux d'humidité idéal. Confortable pour la peau et les voies respiratoires."
            AppLanguage.PORTUGUESE -> "Níveis ideais de umidade. Confortável para a pele e as vias respiratórias."
        }
        humidity <= 75 -> when (language) {
            AppLanguage.JAPANESE -> "少し湿気を感じる状態です。除湿や風通しを良くするとより快適です。"
            AppLanguage.KOREAN -> "약간 습기가 느껴집니다. 환기하거나 제습 모드를 켜면 더욱 쾌적합니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "略感潮热，开启通风或除湿模式可提升体感舒适度。"
            AppLanguage.TRADITIONAL_CHINESE -> "略感潮熱，開啟通風或除濕模式可提升體感舒適度。"
            AppLanguage.ENGLISH -> "Noticeable moisture in the air. Good ventilation keeps things fresh."
            AppLanguage.SPANISH -> "Se nota humedad en el ambiente. Una buena ventilación mantiene el aire fresco."
            AppLanguage.FRENCH -> "Humidité perceptible dans l'air. Une bonne aération garde l'intérieur frais."
            AppLanguage.PORTUGUESE -> "Umidade perceptível no ar. Uma boa ventilação mantém o ambiente agradável."
        }
        else -> when (language) {
            AppLanguage.JAPANESE -> "湿度が非常に高い状態です。蒸し暑さやカビの発生にご注意ください。"
            AppLanguage.KOREAN -> "습도가 매우 높습니다. 불쾌지수 상승과 실내 곰팡이·결로에 주의하세요."
            AppLanguage.SIMPLIFIED_CHINESE -> "空气极为潮湿，注意防霉防潮及闷热防护。"
            AppLanguage.TRADITIONAL_CHINESE -> "空氣極為潮濕，注意防黴防潮及悶熱防護。"
            AppLanguage.ENGLISH -> "High relative humidity. Watch out for mugginess and indoor condensation."
            AppLanguage.SPANISH -> "Humedad relativa muy alta. Precaución ante el bochorno y la condensación interior."
            AppLanguage.FRENCH -> "Humidité relative très élevée. Attention à la lourdeur de l'air et à la condensation."
            AppLanguage.PORTUGUESE -> "Umidade relativa muito alta. Atenção ao calor abafado e à condensação interna."
        }
    }

    return Triple(label, color, advice)
}
