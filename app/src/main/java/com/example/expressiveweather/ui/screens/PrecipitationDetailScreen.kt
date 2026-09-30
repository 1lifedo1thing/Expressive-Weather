package com.example.expressiveweather.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
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
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.City
import com.example.expressiveweather.data.WeatherData
import com.example.expressiveweather.data.getLocalizedDayLabel
import com.example.expressiveweather.data.getLocalizedTimeLabel
import com.example.expressiveweather.theme.GoogleSansFlex
import com.example.expressiveweather.theme.ThemeMode
import com.example.expressiveweather.theme.ThemePreset
import com.example.expressiveweather.ui.components.DetailMetricType
import com.example.expressiveweather.ui.components.DetailScaffold

/**
 * 降水量（Precipitation）詳細画面 - Material 3 Expressive 準拠、多言語対応、枠線フリー
 */
@Composable
fun PrecipitationDetailScreen(
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
    val currentPrecip = weather?.precipitationMm ?: 0.0
    val totalPrecipSum = weather?.precipitationSumMm ?: (weather?.dailyForecast?.firstOrNull()?.precipitationSumMm ?: 0.0)
    val maxProb = weather?.precipitationProbabilityMax ?: (weather?.dailyForecast?.firstOrNull()?.precipProb ?: 0)
    val hourlyForecast = weather?.hourlyForecast ?: emptyList()
    val dailyForecast = weather?.dailyForecast ?: emptyList()

    val isRaining = currentPrecip > 0.0 || (weather?.condition?.isPrecipitating == true)
    val statusColor = if (isRaining) Color(0xFF1E88E5) else if (maxProb >= 50) Color(0xFF0288D1) else Color(0xFF4CAF50)

    DetailScaffold(
        title = strings.precipitation,
        city = city,
        weather = weather,
        language = language,
        metricType = DetailMetricType.PRECIPITATION,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        // ================= 1. ヒーローセクション: 降水量 & 降水確率 =================
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
                        imageVector = Icons.Rounded.Umbrella,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Precipitation & Clouds"
                            AppLanguage.SIMPLIFIED_CHINESE -> "降水量与云雨状态"
                            AppLanguage.TRADITIONAL_CHINESE -> "降水量與雲雨狀態"
                            else -> "降水量・雨雲状況"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 現在の雨量
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (language) {
                                AppLanguage.ENGLISH -> "Current Rate"
                                AppLanguage.SIMPLIFIED_CHINESE -> "当前速率"
                                AppLanguage.TRADITIONAL_CHINESE -> "當前速率"
                                else -> "現在の降水量"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currentPrecip",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "mm / h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // 今日の予想総雨量
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (language) {
                                AppLanguage.ENGLISH -> "Expected Total"
                                AppLanguage.SIMPLIFIED_CHINESE -> "预计总雨量"
                                AppLanguage.TRADITIONAL_CHINESE -> "預計總雨量"
                                else -> "本日の予想総雨量"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalPrecipSum",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "mm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // 本日の降水確率
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (language) {
                                AppLanguage.ENGLISH -> "Max Rain Chance"
                                AppLanguage.SIMPLIFIED_CHINESE -> "最高降水概率"
                                AppLanguage.TRADITIONAL_CHINESE -> "最高降水機率"
                                else -> "最大降水確率"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$maxProb%",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = statusColor
                        )
                        Text(
                            text = when (language) {
                                AppLanguage.ENGLISH -> "Chance"
                                AppLanguage.SIMPLIFIED_CHINESE -> "概率"
                                AppLanguage.TRADITIONAL_CHINESE -> "機率"
                                else -> "確率"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // M3 Expressive: 枠線なしトナルサーフェス
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (isRaining) {
                            when (language) {
                                AppLanguage.ENGLISH -> "Rain Falling"
                                AppLanguage.SIMPLIFIED_CHINESE -> "降雨中"
                                AppLanguage.TRADITIONAL_CHINESE -> "降雨中"
                                else -> "降雨中"
                            }
                        } else if (maxProb >= 50) {
                            when (language) {
                                AppLanguage.ENGLISH -> "Rain Expected"
                                AppLanguage.SIMPLIFIED_CHINESE -> "有降水可能"
                                AppLanguage.TRADITIONAL_CHINESE -> "有降雨可能"
                                else -> "雨の可能性あり"
                            }
                        } else {
                            when (language) {
                                AppLanguage.ENGLISH -> "No Rain Expected"
                                AppLanguage.SIMPLIFIED_CHINESE -> "无降水担忧"
                                AppLanguage.TRADITIONAL_CHINESE -> "無降雨之虞"
                                else -> "降水の心配なし"
                            }
                        },
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isRaining) {
                        when (language) {
                            AppLanguage.ENGLISH -> "Rain is currently detected in the area. Remember your umbrella when heading out."
                            AppLanguage.SIMPLIFIED_CHINESE -> "当前区域正在降水，外出请备好雨具。"
                            AppLanguage.TRADITIONAL_CHINESE -> "當前區域正在降雨，外出請攜帶雨具。"
                            else -> "現在雨が観測されています。外出時は雨具を携帯してください。"
                        }
                    } else if (maxProb >= 50) {
                        when (language) {
                            AppLanguage.ENGLISH -> "Elevated chance of precipitation today. Carrying a compact umbrella is recommended."
                            AppLanguage.SIMPLIFIED_CHINESE -> "今日有较高降水概率，建议随身携带折叠雨伞以备阵雨。"
                            AppLanguage.TRADITIONAL_CHINESE -> "今日有較高降雨機率，建議隨身攜帶摺疊雨傘以防陣雨。"
                            else -> "午後に雨が降りやすくなります。折りたたみ傘の携行をおすすめします。"
                        }
                    } else {
                        when (language) {
                            AppLanguage.ENGLISH -> "Conditions are stable with minimal rain probability throughout the day."
                            AppLanguage.SIMPLIFIED_CHINESE -> "今日天气平稳干燥，无需担心降水或携带雨具。"
                            AppLanguage.TRADITIONAL_CHINESE -> "今日天氣平穩乾燥，無需攜帶雨具即可輕鬆外出。"
                            else -> "今日は安定した天気で、雨具の心配はほとんどありません。"
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = GoogleSansFlex,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // ================= 2. 24時間降水確率・降水量推移 =================
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
                        AppLanguage.ENGLISH -> "Hourly Precipitation Chance"
                        AppLanguage.SIMPLIFIED_CHINESE -> "逐小时降水概率变化"
                        AppLanguage.TRADITIONAL_CHINESE -> "逐小時降水機率變化"
                        else -> "時間別の降水確率推移"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(hourlyForecast.take(24)) { item ->
                        val prob = item.precipProb
                        val probColor = if (prob >= 60) Color(0xFF1976D2) else if (prob >= 30) Color(0xFF03A9F4) else Color.LightGray
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
                                        .height((60 * (prob / 100f).coerceIn(0.08f, 1f)).dp)
                                        .clip(CircleShape)
                                        .background(probColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$prob%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (prob >= 30) probColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ================= 3. 7日間週間降水予報 =================
        if (dailyForecast.isNotEmpty()) {
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
                            AppLanguage.ENGLISH -> "7-Day Precipitation Outlook"
                            AppLanguage.SIMPLIFIED_CHINESE -> "7天降水趋势预报"
                            AppLanguage.TRADITIONAL_CHINESE -> "7天降水趨勢預報"
                            else -> "7日間の降水予報"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    dailyForecast.take(7).forEach { day ->
                        val prob = day.precipProb
                        val pSum = day.precipitationSumMm
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
                                modifier = Modifier.widthIn(min = 68.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // 降水確率バー
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((prob / 100f).coerceIn(0.05f, 1f))
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(if (prob >= 50) Color(0xFF1E88E5) else Color(0xFF03A9F4))
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "$prob%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (prob >= 50) Color(0xFF1E88E5) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(44.dp)
                            )

                            Text(
                                text = "${pSum}mm",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(52.dp)
                            )
                        }
                    }
                }
            }
        }

        // ================= 4. 雨具・お出かけアドバイス =================
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
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Rainwear & Travel Advice"
                            AppLanguage.SIMPLIFIED_CHINESE -> "出行与雨具携带建议"
                            AppLanguage.TRADITIONAL_CHINESE -> "出行與雨具攜帶建議"
                            else -> "雨具・お出かけアドバイス"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                PrecipAdviceRow(
                    icon = Icons.Rounded.Umbrella,
                    title = when (language) {
                        AppLanguage.ENGLISH -> "Umbrella Recommendation"
                        AppLanguage.SIMPLIFIED_CHINESE -> "雨伞携带建议"
                        AppLanguage.TRADITIONAL_CHINESE -> "雨傘攜帶建議"
                        else -> "傘の必要性"
                    },
                    desc = if (isRaining || maxProb >= 70) {
                        when (language) {
                            AppLanguage.ENGLISH -> "A sturdy full-size umbrella or waterproof rain gear is strongly advised."
                            AppLanguage.SIMPLIFIED_CHINESE -> "降水明显，建议随身携带坚固的长柄雨伞或防风防雨外套。"
                            AppLanguage.TRADITIONAL_CHINESE -> "降雨明顯，建議隨身攜帶長柄雨傘或防風防雨外套。"
                            else -> "しっかりとした長傘の持参が推奨されます。"
                        }
                    } else if (maxProb >= 40) {
                        when (language) {
                            AppLanguage.ENGLISH -> "A compact folding umbrella is recommended in case of scattered showers."
                            AppLanguage.SIMPLIFIED_CHINESE -> "可能出现局地小阵雨，随身备一把轻便折叠伞即可安心出行。"
                            AppLanguage.TRADITIONAL_CHINESE -> "可能出現陣雨，隨身備一把輕便摺疊傘即可安心出行。"
                            else -> "折りたたみ傘があると急な雨でも安心です。"
                        }
                    } else {
                        when (language) {
                            AppLanguage.ENGLISH -> "No umbrella is needed; clear and dry conditions prevail."
                            AppLanguage.SIMPLIFIED_CHINESE -> "无须携带雨伞，可轻装自如出行。"
                            AppLanguage.TRADITIONAL_CHINESE -> "無需攜帶雨傘，可輕裝自如出行。"
                            else -> "傘の持参は不要です。"
                        }
                    }
                )

                PrecipAdviceRow(
                    icon = Icons.Rounded.WaterDrop,
                    title = when (language) {
                        AppLanguage.ENGLISH -> "Footwear & Outfits"
                        AppLanguage.SIMPLIFIED_CHINESE -> "鞋履与穿着搭配"
                        AppLanguage.TRADITIONAL_CHINESE -> "鞋履與穿著搭配"
                        else -> "足元・服装"
                    },
                    desc = if (totalPrecipSum >= 10.0) {
                        when (language) {
                            AppLanguage.ENGLISH -> "Watch for wet puddles and road splashes. Waterproof boots and raincoats are best suited."
                            AppLanguage.SIMPLIFIED_CHINESE -> "积水量较大，请注意道路积水与溅泥，建议搭配防水鞋靴或雨衣。"
                            AppLanguage.TRADITIONAL_CHINESE -> "積水量較大，請注意道路積水，建議穿著防水鞋靴或雨衣。"
                            else -> "路面冠水や泥はねにご注意ください。防水靴やレインコートが適しています。"
                        }
                    } else {
                        when (language) {
                            AppLanguage.ENGLISH -> "Standard daily footwear and normal clothing are completely fine."
                            AppLanguage.SIMPLIFIED_CHINESE -> "日常便服与常规鞋履即可舒适轻松应对。"
                            AppLanguage.TRADITIONAL_CHINESE -> "日常便服與常規鞋履即可舒適輕鬆應對。"
                            else -> "普段通りの服装・靴で快適にお過ごしいただけます。"
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PrecipAdviceRow(
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
