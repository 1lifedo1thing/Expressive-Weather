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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold
import kotlin.math.cos
import kotlin.math.sin

/**
 * 気圧（Pressure）詳細画面 - Material 3 Expressive 準拠、多言語対応、枠線フリー
 */
@Composable
fun PressureDetailScreen(
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
    val pressure = weather?.pressureHpa ?: 1013
    val hourlyForecast = weather?.hourlyForecast ?: emptyList()

    val (pressureState, stateColor, trendDesc, isWarning) = getPressureAnalysis(pressure, language)

    DetailScaffold(
        title = strings.pressure,
        city = city,
        weather = weather,
        language = language,
        metricType = DetailMetricType.PRESSURE,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        // ================= 1. ヒーローセクション: バロメーターダイヤル & 現在気圧 =================
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Surface Pressure (Barometer)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "地面气压 (气压计)"
                            AppLanguage.TRADITIONAL_CHINESE -> "地面氣壓 (氣壓計)"
                            else -> "現地気圧 (バロメーター)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 円形ダイヤルメーター
                Box(
                    modifier = Modifier.size(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 960hPa〜1050hPa をスケールとしてマッピング
                    val normalizedProgress = ((pressure - 960f) / (1050f - 960f)).coerceIn(0f, 1f)
                    val animatedAngle by animateFloatAsState(
                        targetValue = 135f + (270f * normalizedProgress),
                        animationSpec = tween(1100, easing = FastOutSlowInEasing),
                        label = "BarometerGauge"
                    )

                    Canvas(modifier = Modifier.size(160.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        // 背景アーク (135度から270度分のスイープ)
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.25f),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 進捗アーク
                        drawArc(
                            color = stateColor,
                            startAngle = 135f,
                            sweepAngle = (animatedAngle - 135f).coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 指針の先端マーカー
                        val angleRad = Math.toRadians(animatedAngle.toDouble())
                        val r = size.width / 2 - 18.dp.toPx()
                        val pointerX = (size.width / 2 + r * cos(angleRad)).toFloat()
                        val pointerY = (size.height / 2 + r * sin(angleRad)).toFloat()

                        drawCircle(
                            color = stateColor,
                            radius = 6.dp.toPx(),
                            center = Offset(pointerX, pointerY)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$pressure",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "hPa",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // M3 Expressive: 枠線を撤廃しトナルサーフェスで表現
                Surface(
                    shape = CircleShape,
                    color = stateColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = pressureState,
                        color = stateColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = trendDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = GoogleSansFlex,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // ================= 2. 24時間気圧推移 =================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = when (language) {
                        AppLanguage.ENGLISH -> "24-Hour Pressure Trend"
                        AppLanguage.SIMPLIFIED_CHINESE -> "24小时气压变化"
                        AppLanguage.TRADITIONAL_CHINESE -> "24小時氣壓變化"
                        else -> "24時間の気圧推移"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(hourlyForecast.take(24)) { item ->
                        val hourAngle = Math.PI * 2 * (item.hour - 9) / 12.0
                        val tidalVariation = kotlin.math.round(1.5 * kotlin.math.cos(hourAngle)).toInt()
                        val itemPressure = (weather?.pressureHpa ?: 1013) + tidalVariation
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(48.dp)
                        ) {
                            Text(
                                text = getLocalizedTimeLabel(item.timeLabel, language),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // 縦バー
                            val barHeight = ((itemPressure - 980) / 40f).coerceIn(0.15f, 1f) * 60
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
                                        .height(barHeight.dp)
                                        .clip(CircleShape)
                                        .background(if (itemPressure >= 1013) Color(0xFF4CAF50) else Color(0xFFFF9800))
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$itemPressure",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // ================= 3. 国際標準健康・気圧インサイト =================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.HealthAndSafety,
                        contentDescription = null,
                        tint = if (isWarning) Color(0xFFFF9800) else Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Barometric Sensitivity & Wellness"
                            AppLanguage.SIMPLIFIED_CHINESE -> "气压变化与健康关怀"
                            AppLanguage.TRADITIONAL_CHINESE -> "氣壓變化與健康關懷"
                            else -> "気圧変化と健康アドバイス"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                PressureAdviceRow(
                    icon = if (isWarning) Icons.AutoMirrored.Rounded.TrendingDown else Icons.AutoMirrored.Rounded.TrendingFlat,
                    title = when (language) {
                        AppLanguage.ENGLISH -> if (isWarning) "Low Pressure Alert" else "Stable Barometric Trend"
                        AppLanguage.SIMPLIFIED_CHINESE -> if (isWarning) "低气压不适提醒" else "气压平稳正常"
                        AppLanguage.TRADITIONAL_CHINESE -> if (isWarning) "低氣壓不適提醒" else "氣壓平穩正常"
                        else -> if (isWarning) "気圧低下に伴う体調への配慮" else "気圧は安定推移"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> if (isWarning)
                            "Decreased barometric pressure may induce headaches, fatigue, or joint sensitivity. Keep well-hydrated and rest as needed."
                        else
                            "Standard atmospheric pressure is maintained; environmental effects on physical comfort remain minimal."
                        AppLanguage.SIMPLIFIED_CHINESE -> if (isWarning)
                            "气压呈下降或偏低趋势，容易引发植物神经紊乱、头痛或倦怠。建议保持放松并适度休息。"
                        else
                            "处于标准舒适气压区间，气压变化对身体状态基本无影响。"
                        AppLanguage.TRADITIONAL_CHINESE -> if (isWarning)
                            "氣壓呈下降或偏低趨勢，容易引發自律神經失調、頭痛或倦怠。建議保持放鬆並適度休息。"
                        else
                            "處於標準舒適氣壓區間，氣壓變化對身體狀態基本無影響。"
                        else -> if (isWarning)
                            "気圧が低めまたは下降傾向です。自律神経の乱れ、頭痛やだるさを感じやすい時間帯です。無理のないペースでお過ごしください。"
                        else
                            "標準的な気圧が維持されており、気圧変化による体調への影響は少ない見込みです。"
                    }
                )

                PressureAdviceRow(
                    icon = Icons.Rounded.Compress,
                    title = when (language) {
                        AppLanguage.ENGLISH -> "Standard Atmospheric Baseline"
                        AppLanguage.SIMPLIFIED_CHINESE -> "标准大气压基准"
                        AppLanguage.TRADITIONAL_CHINESE -> "標準大氣壓基準"
                        else -> "標準大気圧との関係"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH ->
                            "Mean sea-level atmospheric pressure is 1013.25 hPa. Higher values generally bring clear and stable skies, while lower pressure indicates passing troughs or fronts."
                        AppLanguage.SIMPLIFIED_CHINESE ->
                            "海平面标准大气压为 1013.25 hPa。高于此值通常受高气压影响而晴好，低于此值则易受低气压或天气锋面影响。"
                        AppLanguage.TRADITIONAL_CHINESE ->
                            "海平面標準大氣壓為 1013.25 hPa。高於此數值通常代表受高氣壓籠罩而晴朗，低於此數值則易伴隨陰雨及鋒面系統。"
                        else ->
                            "標準大気圧（1気圧）は 1013.25 hPa です。これより高い場合は高気圧で晴れやすく、低い場合は低気圧や前線が接近しやすくなります。"
                    }
                )
            }
        }
    }
}

@Composable
private fun PressureAdviceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(
            imageVector = icon,
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

private fun getPressureAnalysis(pressure: Int, language: AppLanguage): Quadruple<String, Color, String, Boolean> {
    return when {
        pressure >= 1018 -> Quadruple(
            when (language) {
                AppLanguage.ENGLISH -> "High Pressure System"
                AppLanguage.SIMPLIFIED_CHINESE -> "受高气压控制"
                AppLanguage.TRADITIONAL_CHINESE -> "受高氣壓控制"
                else -> "高気圧に覆われています"
            },
            Color(0xFF4CAF50),
            when (language) {
                AppLanguage.ENGLISH -> "Atmospheric pressure is high and stable. Expect sunny weather with no pressure-induced discomfort."
                AppLanguage.SIMPLIFIED_CHINESE -> "气压较高且稳定。晴好天气居多，气压对身体几乎无不良影响。"
                AppLanguage.TRADITIONAL_CHINESE -> "氣壓較高且穩定。晴朗天氣居多，氣壓對身體幾乎無不良影響。"
                else -> "気圧が高く安定しています。晴天が続きやすく、気圧による体調への影響はほとんどありません。"
            },
            false
        )
        pressure >= 1010 -> Quadruple(
            when (language) {
                AppLanguage.ENGLISH -> "Normal & Stable Pressure"
                AppLanguage.SIMPLIFIED_CHINESE -> "处于标准气压附近"
                AppLanguage.TRADITIONAL_CHINESE -> "處於標準氣壓附近"
                else -> "標準気圧付近で安定"
            },
            Color(0xFF03A9F4),
            when (language) {
                AppLanguage.ENGLISH -> "Fluctuating around normal pressure (1013 hPa); minimal bodily stress expected."
                AppLanguage.SIMPLIFIED_CHINESE -> "气压在标准大气压(1013hPa)附近波动，对身体无明显额外负担。"
                AppLanguage.TRADITIONAL_CHINESE -> "氣壓在標準大氣壓(1013hPa)附近波動，對身體無明顯額外負擔。"
                else -> "標準大気圧(1013hPa)前後で推移しており、身体への急激な負荷は少ない環境です。"
            },
            false
        )
        pressure >= 1002 -> Quadruple(
            when (language) {
                AppLanguage.ENGLISH -> "Slightly Low Pressure"
                AppLanguage.SIMPLIFIED_CHINESE -> "气压略微偏低"
                AppLanguage.TRADITIONAL_CHINESE -> "氣壓略微偏低"
                else -> "気圧やや低め"
            },
            Color(0xFFFF9800),
            when (language) {
                AppLanguage.ENGLISH -> "Pressure is slightly depressed. Sensitive individuals may experience mild headaches or fatigue."
                AppLanguage.SIMPLIFIED_CHINESE -> "气压略微偏低。敏感体质者可能会感到轻微头痛或颈肩酸胀。"
                AppLanguage.TRADITIONAL_CHINESE -> "氣壓略微偏低。敏感體質者可能會感到輕微頭痛或頸肩緊繃。"
                else -> "気圧がやや低下しています。敏感な方は軽い頭痛や首肩のこりを感じる場合があります。"
            },
            true
        )
        else -> Quadruple(
            when (language) {
                AppLanguage.ENGLISH -> "Low Pressure System (Caution)"
                AppLanguage.SIMPLIFIED_CHINESE -> "低气压接近 (注意)"
                AppLanguage.TRADITIONAL_CHINESE -> "低氣壓接近 (注意)"
                else -> "低気圧接近中 (警戒)"
            },
            Color(0xFFE53935),
            when (language) {
                AppLanguage.ENGLISH -> "Pressure is significantly reduced. May trigger headaches, joint stiffness, and lethargy; prioritize rest."
                AppLanguage.SIMPLIFIED_CHINESE -> "气压明显偏低。容易引起偏头痛、关节不适与倦怠感，请注意劳逸结合与充足休息。"
                AppLanguage.TRADITIONAL_CHINESE -> "氣壓明顯偏低。容易引起偏頭痛、關節不適與倦怠感，請注意適度休息。"
                else -> "気圧がかなり低くなっています。偏頭痛、関節痛、倦怠感が生じやすいため、十分な休息をお取りください。"
            },
            true
        )
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

