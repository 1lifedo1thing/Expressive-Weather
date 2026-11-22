package io.github.nemynew.expressiveweather.ui.screens

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
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold

/**
 * 体感温度（Feels Like / Comfort）詳細画面 - Material 3 Expressive 準拠、多言語対応、枠線フリー
 */
@Composable
fun FeelsLikeDetailScreen(
    weather: WeatherData?,
    city: City,
    language: AppLanguage,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    windSpeedUnit: WindSpeedUnit = WindSpeedUnit.FOLLOW_LANGUAGE,
    weatherAdaptiveThemeEnabled: Boolean = true,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    themeMode: ThemeMode = ThemeMode.WEATHER,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = language.strings
    val currentTempC = weather?.currentTempC ?: 20
    val feelsLikeC = weather?.feelsLikeC ?: 20
    val currentTempDisplay = temperatureUnit.convert(currentTempC)
    val feelsLikeDisplay = temperatureUnit.convert(feelsLikeC)
    val unitSymbol = temperatureUnit.symbol
    val humidity = weather?.humidity ?: 50
    val windSpeedKmH = weather?.windSpeedKmH ?: 10.0
    val hourlyForecast = weather?.hourlyForecast ?: emptyList()

    val diffC = feelsLikeC - currentTempC
    val diffDisplay = feelsLikeDisplay - currentTempDisplay
    val diffText = when {
        diffDisplay > 0 -> when (language) {
            AppLanguage.ENGLISH -> "+${diffDisplay}${unitSymbol} warmer than actual"
            AppLanguage.SIMPLIFIED_CHINESE -> "较实际气温偏高 +${diffDisplay}${unitSymbol}"
            AppLanguage.TRADITIONAL_CHINESE -> "較實際氣溫偏高 +${diffDisplay}${unitSymbol}"
            else -> "実気温より +${diffDisplay}${unitSymbol} 高い"
        }
        diffDisplay < 0 -> when (language) {
            AppLanguage.ENGLISH -> "${diffDisplay}${unitSymbol} cooler than actual"
            AppLanguage.SIMPLIFIED_CHINESE -> "较实际气温偏低 ${diffDisplay}${unitSymbol}"
            AppLanguage.TRADITIONAL_CHINESE -> "較實際氣溫偏低 ${diffDisplay}${unitSymbol}"
            else -> "実気温より ${diffDisplay}${unitSymbol} 低い"
        }
        else -> when (language) {
            AppLanguage.ENGLISH -> "Similar to actual temperature"
            AppLanguage.SIMPLIFIED_CHINESE -> "与实际气温基本一致"
            AppLanguage.TRADITIONAL_CHINESE -> "與實際氣溫基本一致"
            else -> "実気温とほぼ同じ"
        }
    }

    val (factorDesc, outfitAdvice) = getFeelsLikeAnalysis(feelsLikeC, diffC, humidity, windSpeedKmH, language)

    DetailScaffold(
        title = strings.feelsLikeCard,
        city = city,
        weather = weather,
        language = language,
        metricType = DetailMetricType.FEELS_LIKE,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        // ================= 1. ヒーローセクション: 体感温度 & 実気温比較 =================
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
                        imageVector = Icons.Rounded.Thermostat,
                        contentDescription = null,
                        tint = getTempColor(feelsLikeC),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Apparent Temperature (BOM Index)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "体感温度 (BOM 体感指数)"
                            AppLanguage.TRADITIONAL_CHINESE -> "體感溫度 (BOM 體感指數)"
                            else -> "体感温度 (体感指数)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "${feelsLikeDisplay}°",
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 54.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // M3 Expressive: 枠線なしトナルサーフェス
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = diffText,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 実気温 vs 体感温度の比較カード
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = when (language) {
                                AppLanguage.ENGLISH -> "Actual Temperature"
                                AppLanguage.SIMPLIFIED_CHINESE -> "实际气温"
                                AppLanguage.TRADITIONAL_CHINESE -> "實際氣溫"
                                else -> "実際の気温"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currentTempDisplay}${unitSymbol}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = when (language) {
                                AppLanguage.ENGLISH -> "Feels Like"
                                AppLanguage.SIMPLIFIED_CHINESE -> "体感温度"
                                AppLanguage.TRADITIONAL_CHINESE -> "體感溫度"
                                else -> "体感温度"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${feelsLikeDisplay}${unitSymbol}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = getTempColor(feelsLikeC)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = factorDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = GoogleSansFlex,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // ================= 2. 24時間体感温度推移 =================
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
                        AppLanguage.ENGLISH -> "Today's Apparent Temperature Trend"
                        AppLanguage.SIMPLIFIED_CHINESE -> "今日体感温度变化"
                        AppLanguage.TRADITIONAL_CHINESE -> "今日體感溫度變化"
                        else -> "本日の体感温度推移"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(hourlyForecast.take(24)) { item ->
                        val tempC = calculateApparentTemperature(item.tempC, item.humidity, item.windSpeedKmH)
                        val tempDisplay = temperatureUnit.convert(tempC)
                        val col = getTempColor(tempC)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(44.dp)
                        ) {
                            Text(
                                text = getLocalizedTimeLabel(item.timeLabel, language),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // 縦バー
                            val barHeight = ((tempC + 5) / 40f).coerceIn(0.12f, 1f) * 60
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
                                        .background(col)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${tempDisplay}°",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = col
                            )
                        }
                    }
                }
            }
        }

        // ================= 3. 服装指数・おすすめコーディネート =================
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
                        imageVector = Icons.Rounded.Checkroom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Clothing & Outfit Recommendations"
                            AppLanguage.SIMPLIFIED_CHINESE -> "穿衣指数与搭配建议"
                            AppLanguage.TRADITIONAL_CHINESE -> "穿衣指數與搭配建議"
                            else -> "服装指数とアドバイス"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Checkroom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = outfitAdvice,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (language) {
                                AppLanguage.ENGLISH -> "Recommended dressing based on wind chill, humidity, and thermal comfort."
                                AppLanguage.SIMPLIFIED_CHINESE -> "综合考量风速与湿度后得出的最佳穿搭指南。"
                                AppLanguage.TRADITIONAL_CHINESE -> "綜合考量風速與濕度後得出的最佳穿搭指南。"
                                else -> "風速や湿度の影響を考慮した推奨スタイルです。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ================= 4. 体感温度のメカニズム解説 =================
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Understanding Apparent Temperature"
                            AppLanguage.SIMPLIFIED_CHINESE -> "体感温度原理"
                            AppLanguage.TRADITIONAL_CHINESE -> "體感溫度原理"
                            else -> "体感温度の仕組み"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (language) {
                        AppLanguage.ENGLISH ->
                            "Apparent temperature computes thermal sensation by synthesizing ambient temperature, relative humidity, and wind speed (Australian BOM / Steadman formula). Wind accelerates evaporative cooling, whereas high humidity restricts transpiration."
                        AppLanguage.SIMPLIFIED_CHINESE ->
                            "体感温度综合了环境气温、相对湿度与风速等多项物理指标（依据澳大利亚气象局 BOM / Steadman 国际公式）。风速增大会加速带走体表热量，而高湿度则会阻碍汗液自然蒸发使人倍感闷热。"
                        AppLanguage.TRADITIONAL_CHINESE ->
                            "體感溫度綜合了環境氣溫、相對濕度與風速等多項物理指標（依據澳洲氣象局 BOM / Steadman 國際公式）。風速增強會加速帶走體表熱量，高濕度則會阻礙汗液自然蒸發使人倍感悶熱。"
                        else ->
                            "体感温度は気温に加え、「湿度」と「風速」を総合して算出されます（オーストラリア気象局/Steadman基準）。風速が強まると風冷効果で体温が奪われ、湿度が高まると発汗による気化熱冷却が阻害されて蒸し暑く感じられます。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun getTempColor(tempC: Int): Color = when {
    tempC >= 30 -> Color(0xFFE53935) // 猛暑・酷暑 (赤)
    tempC >= 25 -> Color(0xFFFF9800) // 夏日 (橙)
    tempC >= 18 -> Color(0xFF4CAF50) // 快適 (緑)
    tempC >= 10 -> Color(0xFF03A9F4) // 涼しい (水色)
    else -> Color(0xFF3F51B5)        // 寒い (青)
}

private fun getFeelsLikeAnalysis(
    feelsLike: Int,
    diff: Int,
    humidity: Int,
    windSpeedKmH: Double,
    language: AppLanguage
): Pair<String, String> {
    val factor = when {
        diff > 0 && humidity > 60 -> when (language) {
            AppLanguage.ENGLISH -> "Elevated humidity suppresses body heat dissipation, making it feel noticeably warmer and muggy."
            AppLanguage.SIMPLIFIED_CHINESE -> "湿度较高抑制体表排汗散热，体感明显比实际气温更加温热潮湿。"
            AppLanguage.TRADITIONAL_CHINESE -> "濕度較高抑制體表排汗散熱，體感明顯比實際氣溫更加悶熱。"
            else -> "湿度が高めのため、皮膚からの熱放散が抑えられ、実際よりも暖かく（蒸し暑く）感じられます。"
        }
        diff < 0 && windSpeedKmH > 15.0 -> when (language) {
            AppLanguage.ENGLISH -> "Active airflow enhances evaporative wind chill, creating a cooler sensation than the ambient temperature."
            AppLanguage.SIMPLIFIED_CHINESE -> "受风力与风寒效应影响，体表散热加快，感觉明显比实际温度偏凉。"
            AppLanguage.TRADITIONAL_CHINESE -> "受風力與風寒效應影響，體表散熱加快，感覺明顯比實際溫度偏涼。"
            else -> "風が吹いているため、風冷効果（ウィンドチル）により実際の気温よりも涼しく（肌寒く）感じられます。"
        }
        else -> when (language) {
            AppLanguage.ENGLISH -> "Humidity and airflow are well-balanced; the apparent temperature closely tracks the thermometer."
            AppLanguage.SIMPLIFIED_CHINESE -> "湿度与风速比例适中，体感温度与实际气温非常接近。"
            AppLanguage.TRADITIONAL_CHINESE -> "濕度與風速比例適中，體感溫度與實際氣溫非常接近。"
            else -> "湿度と風速のバランスが標準的で、ほぼ実際の気温に近い心地よさです。"
        }
    }

    val outfit = when {
        feelsLike >= 28 -> when (language) {
            AppLanguage.ENGLISH -> "T-shirt weather. Stay hydrated and stay cool under direct sunlight."
            AppLanguage.SIMPLIFIED_CHINESE -> "短袖或清凉夏装适宜。请注意防暑降温与补充水分。"
            AppLanguage.TRADITIONAL_CHINESE -> "短袖或清涼夏裝適宜。請注意防暑降溫與補充水分。"
            else -> "半袖で快適にお過ごしいただけます。熱中症対策・水分補給を心がけてください。"
        }
        feelsLike >= 23 -> when (language) {
            AppLanguage.ENGLISH -> "Short sleeves or light long sleeves are perfect for comfortable outings."
            AppLanguage.SIMPLIFIED_CHINESE -> "短袖或薄长袖衬衫即可，体感舒适宜人。"
            AppLanguage.TRADITIONAL_CHINESE -> "短袖或薄長袖襯衫即可，體感舒適宜人。"
            else -> "半袖または薄手の長袖シャツが快適な過ごしやすい気候です。"
        }
        feelsLike >= 17 -> when (language) {
            AppLanguage.ENGLISH -> "Long sleeves, casual sweaters, or light cardigans are ideal."
            AppLanguage.SIMPLIFIED_CHINESE -> "长袖衬衫、卫衣或薄开衫尤为适宜，早晚防风。"
            AppLanguage.TRADITIONAL_CHINESE -> "長袖襯衫、薄長T或薄開衫尤為適宜。"
            else -> "長袖シャツやカットソー、薄手のカーディガンがあると安心です。"
        }
        feelsLike >= 12 -> when (language) {
            AppLanguage.ENGLISH -> "Jackets, trenches, knit sweaters, or windbreakers are recommended."
            AppLanguage.SIMPLIFIED_CHINESE -> "建议穿着夹克外衣、薄毛衣或风衣防风保暖。"
            AppLanguage.TRADITIONAL_CHINESE -> "建議穿著夾克外衣、薄毛衣或風衣防風保暖。"
            else -> "セーターやジャケット、トレンチコートなどの上着が必要です。"
        }
        feelsLike >= 6 -> when (language) {
            AppLanguage.ENGLISH -> "Heavier winter coats, scarves, and insulated layers are necessary."
            AppLanguage.SIMPLIFIED_CHINESE -> "需穿着保暖大衣、羽绒服或羊毛围巾抵御寒冷。"
            AppLanguage.TRADITIONAL_CHINESE -> "需穿著保暖大衣、羽絨衣或羊毛圍巾禦寒。"
            else -> "冬物コートやマフラーなどのしっかりとした防寒着をお選びください。"
        }
        else -> when (language) {
            AppLanguage.ENGLISH -> "Heavy down coat, thermal underlayers, gloves, and knit caps essential."
            AppLanguage.SIMPLIFIED_CHINESE -> "严寒低温，请配备厚羽绒服、保暖手套及全面防寒保暖装备。"
            AppLanguage.TRADITIONAL_CHINESE -> "嚴寒低溫，請配備厚羽絨衣、保暖手套及全面防寒保暖裝備。"
            else -> "ダウンジャケットや手袋、万全の防寒対策が必須です。"
        }
    }

    return Pair(factor, outfit)
}

private fun calculateApparentTemperature(tempC: Int, humidity: Int, windSpeedKmH: Double): Int {
    val windSpeedMs = windSpeedKmH / 3.6
    val e = (humidity / 100.0) * 6.105 * kotlin.math.exp((17.27 * tempC) / (237.7 + tempC))
    val at = tempC + 0.33 * e - 0.70 * windSpeedMs - 4.0
    return kotlin.math.round(at).toInt()
}
