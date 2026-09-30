package com.example.expressiveweather.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Masks
import androidx.compose.material.icons.rounded.Window
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.data.AirQuality
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.City
import com.example.expressiveweather.data.WeatherData
import com.example.expressiveweather.theme.GoogleSansFlex
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset
import com.example.expressiveweather.ui.components.DetailMetricType
import com.example.expressiveweather.ui.components.DetailScaffold

/**
 * 空気質（Air Quality / AQI）詳細画面
 * Material 3 Expressive 準拠（枠線なし、トナルサーフェス、4言語対応）
 */
@Composable
fun AirQualityDetailScreen(
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
    val airQuality = weather?.airQuality ?: AirQuality(aqi = 25, levelLabel = "Good", pm25 = 8.5, pm10 = 15.0, advice = "")
    val aqi = airQuality.aqi

    val (aqiStatus, aqiColor, aqiSummary) = getAqiInfo(aqi, language)

    val aqiHeaderLabel = when (language) {
        AppLanguage.JAPANESE -> "大気質指数 (AQI)"
        AppLanguage.SIMPLIFIED_CHINESE -> "空气质量指数 (AQI)"
        AppLanguage.TRADITIONAL_CHINESE -> "空氣品質指數 (AQI)"
        AppLanguage.ENGLISH -> "Air Quality Index (AQI)"
    }

    val pollutantsTitle = when (language) {
        AppLanguage.JAPANESE -> "主要汚染物質の測定値"
        AppLanguage.SIMPLIFIED_CHINESE -> "主要污染物浓度监测"
        AppLanguage.TRADITIONAL_CHINESE -> "主要污染物濃度監測"
        AppLanguage.ENGLISH -> "Key Pollutant Measurements"
    }

    val adviceSectionTitle = when (language) {
        AppLanguage.JAPANESE -> "空気質と健康・生活アドバイス"
        AppLanguage.SIMPLIFIED_CHINESE -> "空气质量与健康防护建议"
        AppLanguage.TRADITIONAL_CHINESE -> "空氣品質與健康防護建議"
        AppLanguage.ENGLISH -> "Air Quality Health Guidelines"
    }

    DetailScaffold(
        title = strings.airQuality,
        city = city,
        weather = weather,
        language = language,
        metricType = DetailMetricType.AIR_QUALITY,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        // ================= 1. ヒーローセクション: AQI スコア & スケールバー =================
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
                        imageVector = Icons.Rounded.Air,
                        contentDescription = null,
                        tint = aqiColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = aqiHeaderLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = aqi.toString(),
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = CircleShape,
                    color = aqiColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = aqiStatus,
                        color = aqiColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // AQI スケールバー (0 - 200+)
                val animatedProgress by animateFloatAsState(
                    targetValue = (aqi / 200f).coerceIn(0.04f, 1f),
                    animationSpec = tween(1000, easing = FastOutSlowInEasing),
                    label = "AqiProgress"
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
                                .background(aqiColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(when (language) {
                            AppLanguage.JAPANESE -> "50 (良)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "50 (优)"
                            AppLanguage.TRADITIONAL_CHINESE -> "50 (優)"
                            AppLanguage.ENGLISH -> "50 (Good)"
                        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(when (language) {
                            AppLanguage.JAPANESE -> "100 (普)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "100 (良)"
                            AppLanguage.TRADITIONAL_CHINESE -> "100 (普)"
                            AppLanguage.ENGLISH -> "100 (Mod)"
                        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(when (language) {
                            AppLanguage.JAPANESE -> "150 (注意)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "150 (敏感)"
                            AppLanguage.TRADITIONAL_CHINESE -> "150 (敏感)"
                            AppLanguage.ENGLISH -> "150 (Sens)"
                        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("200+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = aqiSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontFamily = GoogleSansFlex,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // ================= 2. 主要汚染物質グリッド =================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            ),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = pollutantsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )

                Spacer(modifier = Modifier.height(16.dp))

                val pm25Sub = when (language) {
                    AppLanguage.JAPANESE -> "微小粒子状物質"
                    AppLanguage.SIMPLIFIED_CHINESE -> "细颗粒物"
                    AppLanguage.TRADITIONAL_CHINESE -> "細懸浮微粒"
                    AppLanguage.ENGLISH -> "Fine Particles"
                }

                val pm10Sub = when (language) {
                    AppLanguage.JAPANESE -> "粒子状物質"
                    AppLanguage.SIMPLIFIED_CHINESE -> "可吸入颗粒物"
                    AppLanguage.TRADITIONAL_CHINESE -> "懸浮微粒"
                    AppLanguage.ENGLISH -> "Inhalable Particles"
                }

                val o3Sub = when (language) {
                    AppLanguage.JAPANESE -> "地表オゾン"
                    AppLanguage.SIMPLIFIED_CHINESE -> "地面臭氧"
                    AppLanguage.TRADITIONAL_CHINESE -> "地面臭氧"
                    AppLanguage.ENGLISH -> "Surface Ozone"
                }

                val no2Sub = when (language) {
                    AppLanguage.JAPANESE -> "二酸化窒素"
                    AppLanguage.SIMPLIFIED_CHINESE -> "二氧化氮"
                    AppLanguage.TRADITIONAL_CHINESE -> "二氧化氮"
                    AppLanguage.ENGLISH -> "Nitrogen Dioxide"
                }

                val goodLabel = when (language) {
                    AppLanguage.JAPANESE -> "良好"
                    AppLanguage.SIMPLIFIED_CHINESE -> "优良"
                    AppLanguage.TRADITIONAL_CHINESE -> "優良"
                    AppLanguage.ENGLISH -> "Good"
                }

                val normalLabel = when (language) {
                    AppLanguage.JAPANESE -> "基準内"
                    AppLanguage.SIMPLIFIED_CHINESE -> "达标"
                    AppLanguage.TRADITIONAL_CHINESE -> "達標"
                    AppLanguage.ENGLISH -> "Normal"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PollutantCard(
                        title = "PM2.5",
                        sub = pm25Sub,
                        value = "${airQuality.pm25} μg/m³",
                        isGood = airQuality.pm25 <= 15.0,
                        modifier = Modifier.weight(1f)
                    )
                    PollutantCard(
                        title = "PM10",
                        sub = pm10Sub,
                        value = "${airQuality.pm10} μg/m³",
                        isGood = airQuality.pm10 <= 45.0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PollutantCard(
                        title = "O₃",
                        sub = o3Sub,
                        value = goodLabel,
                        isGood = true,
                        modifier = Modifier.weight(1f)
                    )
                    PollutantCard(
                        title = "NO₂",
                        sub = no2Sub,
                        value = normalLabel,
                        isGood = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ================= 3. 健康・生活アドバイス =================
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
                        imageVector = Icons.Rounded.HealthAndSafety,
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

                val (ventTitle, ventDesc) = when (language) {
                    AppLanguage.JAPANESE -> "室内の換気" to if (aqi <= 50) "窓を開けて積極的に新鮮な外気を取り入れましょう。" else "空気清浄機を活用し、窓の開放は短時間に抑えましょう。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "室内通风" to if (aqi <= 50) "空气清新，适宜开窗通风换气。" else "建议开启空气净化器，适当减少开窗通风时间。"
                    AppLanguage.TRADITIONAL_CHINESE -> "室內通風" to if (aqi <= 50) "空氣清新，適宜開窗通風換氣。" else "建議開啟空氣清淨機，適當減少開窗通風時間。"
                    AppLanguage.ENGLISH -> "Indoor Ventilation" to if (aqi <= 50) "Air is fresh and ideal for opening windows." else "Keep windows closed and use an air purifier if needed."
                }

                val (outdoorTitle, outdoorDesc) = when (language) {
                    AppLanguage.JAPANESE -> "屋外活動" to if (aqi <= 100) "ウォーキングやランニングなど、普段通り快適に行えます。" else "激しい屋外運動は控え、屋内でのトレーニングを推奨します。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "户外运动" to if (aqi <= 100) "可正常开展户外徒步、跑步等各项体育运动。" else "敏感人群应减少剧烈户外活动，优先选择室内锻炼。"
                    AppLanguage.TRADITIONAL_CHINESE -> "戶外運動" to if (aqi <= 100) "可正常開展戶外徒步、跑步等各項體育運動。" else "敏感族群應減少劇烈戶外活動，優先選擇室內鍛鍊。"
                    AppLanguage.ENGLISH -> "Outdoor Activity" to if (aqi <= 100) "Great conditions for outdoor exercise and recreation." else "Reduce prolonged or heavy exertion outdoors."
                }

                val (maskTitle, maskDesc) = when (language) {
                    AppLanguage.JAPANESE -> "マスクの着用" to if (aqi <= 50) "マスクの着用は不要です。" else "呼吸器疾患をお持ちの方や敏感な方は、微粒子対応マスクの着用が推奨されます。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "口罩防护" to if (aqi <= 50) "空气纯净，无需佩戴口罩。" else "呼吸道敏感人群出行建议佩戴防颗粒物口罩。"
                    AppLanguage.TRADITIONAL_CHINESE -> "口罩防護" to if (aqi <= 50) "空氣純淨，無需配戴口罩。" else "呼吸道敏感族群出行建議配戴防微粒口罩。"
                    AppLanguage.ENGLISH -> "Mask Advisory" to if (aqi <= 50) "No mask required." else "Sensitive individuals should consider wearing particulate masks outdoors."
                }

                AqiAdviceRow(
                    icon = Icons.Rounded.Window,
                    title = ventTitle,
                    desc = ventDesc
                )
                AqiAdviceRow(
                    icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                    title = outdoorTitle,
                    desc = outdoorDesc
                )
                AqiAdviceRow(
                    icon = Icons.Rounded.Masks,
                    title = maskTitle,
                    desc = maskDesc
                )
            }
        }
    }
}

@Composable
private fun PollutantCard(
    title: String,
    sub: String,
    value: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    val tagColor = if (isGood) Color(0xFF4CAF50) else Color(0xFFFF9800)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tagColor)
                )
            }
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AqiAdviceRow(
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
                .size(20.dp)
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

private fun getAqiInfo(aqi: Int, language: AppLanguage): Triple<String, Color, String> {
    val color = when {
        aqi <= 50 -> Color(0xFF4CAF50)
        aqi <= 100 -> Color(0xFFFFB300)
        aqi <= 150 -> Color(0xFFFF9800)
        aqi <= 200 -> Color(0xFFE53935)
        else -> Color(0xFF8E24AA)
    }

    val label = when {
        aqi <= 50 -> when (language) {
            AppLanguage.JAPANESE -> "良い"
            AppLanguage.SIMPLIFIED_CHINESE -> "优"
            AppLanguage.TRADITIONAL_CHINESE -> "優"
            AppLanguage.ENGLISH -> "Good"
        }
        aqi <= 100 -> when (language) {
            AppLanguage.JAPANESE -> "普通"
            AppLanguage.SIMPLIFIED_CHINESE -> "良"
            AppLanguage.TRADITIONAL_CHINESE -> "普通"
            AppLanguage.ENGLISH -> "Moderate"
        }
        aqi <= 150 -> when (language) {
            AppLanguage.JAPANESE -> "敏感な人に影響"
            AppLanguage.SIMPLIFIED_CHINESE -> "轻度污染 (敏感人群)"
            AppLanguage.TRADITIONAL_CHINESE -> "輕度污染 (敏感族群)"
            AppLanguage.ENGLISH -> "Unhealthy for Sensitive Groups"
        }
        aqi <= 200 -> when (language) {
            AppLanguage.JAPANESE -> "不健康"
            AppLanguage.SIMPLIFIED_CHINESE -> "中度污染"
            AppLanguage.TRADITIONAL_CHINESE -> "中度污染"
            AppLanguage.ENGLISH -> "Unhealthy"
        }
        else -> when (language) {
            AppLanguage.JAPANESE -> "極めて不健康"
            AppLanguage.SIMPLIFIED_CHINESE -> "重度及严重污染"
            AppLanguage.TRADITIONAL_CHINESE -> "重度及嚴重污染"
            AppLanguage.ENGLISH -> "Hazardous"
        }
    }

    val advice = when {
        aqi <= 50 -> when (language) {
            AppLanguage.JAPANESE -> "大気環境は非常に清浄です。健康被害のリスクはありません。"
            AppLanguage.SIMPLIFIED_CHINESE -> "空气质量极佳，非常适合户外运动与各类活动。"
            AppLanguage.TRADITIONAL_CHINESE -> "空氣品質極佳，非常適合戶外運動與各類活動。"
            AppLanguage.ENGLISH -> "Air quality is satisfactory and poses little or no risk."
        }
        aqi <= 100 -> when (language) {
            AppLanguage.JAPANESE -> "空気質は許容範囲内です。極端に敏感な方以外は問題ありません。"
            AppLanguage.SIMPLIFIED_CHINESE -> "空气质量可接受，极少数异常敏感人群除外。"
            AppLanguage.TRADITIONAL_CHINESE -> "空氣品質可接受，極少數異常敏感族群除外。"
            AppLanguage.ENGLISH -> "Air quality is acceptable for most people."
        }
        aqi <= 150 -> when (language) {
            AppLanguage.JAPANESE -> "呼吸器系に持病のある方や高齢者・子供は長時間の外出に注意してください。"
            AppLanguage.SIMPLIFIED_CHINESE -> "易感人群症状有轻度加剧可能，建议减少户外剧烈运动。"
            AppLanguage.TRADITIONAL_CHINESE -> "易感族群症狀有輕度加劇可能，建議減少戶外劇烈運動。"
            AppLanguage.ENGLISH -> "Members of sensitive groups may experience health effects."
        }
        aqi <= 200 -> when (language) {
            AppLanguage.JAPANESE -> "すべての人が影響を感じる可能性があります。屋外での長時間の運動はお控えください。"
            AppLanguage.SIMPLIFIED_CHINESE -> "所有人群健康都可能受到影响，建议适当减少户外运动。"
            AppLanguage.TRADITIONAL_CHINESE -> "所有人群健康都可能受到影響，建議適當減少戶外運動。"
            AppLanguage.ENGLISH -> "Everyone may begin to experience health effects; limit outdoor exertion."
        }
        else -> when (language) {
            AppLanguage.JAPANESE -> "健康被害のリスクが高まっています。屋内で空気清浄機を使用して過ごしてください。"
            AppLanguage.SIMPLIFIED_CHINESE -> "健康警告：所有人应避免户外运动，室内开启空气净化设备。"
            AppLanguage.TRADITIONAL_CHINESE -> "健康警告：所有人應避免戶外運動，室內開啟空氣清淨設備。"
            AppLanguage.ENGLISH -> "Health alert: serious risk of effects for all individuals. Stay indoors."
        }
    }

    return Triple(label, color, advice)
}
