package io.github.nemynew.expressiveweather.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
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
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold

/**
 * 湿度（Humidity）詳細画面
 * Material 3 Expressive 準拠（枠線なし、トナルサーフェス、4言語対応）
 */
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
    val dewPointC = weather?.dewPointC ?: (weather?.currentTempC?.minus(4) ?: 15)
    val dewPointDisplay = temperatureUnit.convert(dewPointC)
    val hourlyForecast = weather?.hourlyForecast ?: emptyList()

    val (humidityLabel, humidityColor, humidityAdvice) = getHumidityInfo(humidity, language)

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
        AppLanguage.JAPANESE -> "本日の湿度推移"
        AppLanguage.KOREAN -> "오늘의 습도 추이"
        AppLanguage.SIMPLIFIED_CHINESE -> "今日湿度走势"
        AppLanguage.TRADITIONAL_CHINESE -> "今日濕度走勢"
        AppLanguage.ENGLISH -> "Hourly Humidity"
        AppLanguage.SPANISH -> "Humedad por hora"
        AppLanguage.FRENCH -> "Humidité horaire"
        AppLanguage.PORTUGUESE -> "Umidade por hora"
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
        // ================= 1. ヒーローセクション: 湿度 & 露点温度 =================
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
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = humidityColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = relativeHumidityLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "$humidity%",
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = CircleShape,
                    color = humidityColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = humidityLabel,
                        color = humidityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
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
                        style = MaterialTheme.typography.titleMedium,
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

        // ================= 2. 24時間湿度推移 =================
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
                    text = hourlyHumidityTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(hourlyForecast.take(24)) { item ->
                        val itemHumidity = item.humidity
                        val col = getHumidityColor(itemHumidity)
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
                                        .height((60 * (itemHumidity / 100f).coerceIn(0.1f, 1f)).dp)
                                        .clip(CircleShape)
                                        .background(col)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$itemHumidity%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = col
                            )
                        }
                    }
                }
            }
        }

        // ================= 3. 湿度と快適・生活アドバイス =================
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

                val (respiratoryTitle, respiratoryDesc) = when (language) {
                    AppLanguage.JAPANESE -> "呼吸器・健康管理" to if (humidity < 40) "空気が乾燥しています。加湿器等で40〜60%を保つことで粘膜を保護し、健康維持につながります。" else "理想的な湿度で、呼吸器への負担が少なく快適に過ごせます。"
                    AppLanguage.KOREAN -> "호흡기·건강 관리" to if (humidity < 40) "공기가 건조합니다. 가습기 등으로 실내 습도를 40~60%로 유지해 호흡기 점막을 보호하세요." else "쾌적한 습도로 호흡기 부담 없이 편안하게 지낼 수 있습니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "呼吸健康关怀" to if (humidity < 40) "空气干燥，适宜使用加湿设备将湿度维持在40-60%，保护呼吸道黏膜。" else "湿度维持在健康区间，呼吸舒适。"
                    AppLanguage.TRADITIONAL_CHINESE -> "呼吸健康關懷" to if (humidity < 40) "空氣乾燥，適宜使用加濕設備將濕度維持在40-60%，保護呼吸道黏膜。" else "濕度維持在健康區間，呼吸舒適。"
                    AppLanguage.ENGLISH -> "Respiratory Health" to if (humidity < 40) "Air is dry. Keeping indoor humidity between 40-60% helps protect mucosal tissues." else "Healthy humidity levels promote comfortable breathing."
                    AppLanguage.SPANISH -> "Salud respiratoria" to if (humidity < 40) "El aire está seco. Mantener la humedad interior entre 40% y 60% ayuda a proteger las mucosas." else "Niveles saludables de humedad que favorecen una respiración cómoda."
                    AppLanguage.FRENCH -> "Santé respiratoire" to if (humidity < 40) "L'air est sec. Maintenir l'humidité intérieure entre 40 et 60 % aide à protéger les muqueuses." else "Un taux d'humidité sain favorise une respiration confortable."
                    AppLanguage.PORTUGUESE -> "Saúde respiratória" to if (humidity < 40) "O ar está seco. Manter a umidade interna entre 40% e 60% ajuda a proteger as mucosas." else "Níveis saudáveis de umidade favorecem uma respiração confortável."
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
                    AppLanguage.JAPANESE -> "肌の水分バランス" to if (humidity < 40) "肌や喉が乾燥しやすいため、こまめな保湿クリームや十分な水分補給が効果的です。" else "肌に適度な潤いが保たれやすい良好な湿度です。"
                    AppLanguage.KOREAN -> "피부 보습 관리" to if (humidity < 40) "피부와 목이 건조해지기 쉬우니 보습제를 바르고 수분을 충분히 섭취하세요." else "피부에 적당한 수분이 유지되기 좋은 습도입니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "肌肤保湿护理" to if (humidity < 40) "空气湿度较低，注意补水护肤及随身饮水。" else "空气滋润，肌肤能维持良好的水油平衡。"
                    AppLanguage.TRADITIONAL_CHINESE -> "肌膚保濕護理" to if (humidity < 40) "空氣濕度較低，注意補水護膚及隨身飲水。" else "空氣滋潤，肌膚能維持良好的水油平衡。"
                    AppLanguage.ENGLISH -> "Skin Hydration" to if (humidity < 40) "Low humidity causes rapid moisture loss. Use moisturizer and stay hydrated." else "Atmospheric moisture naturally maintains skin hydration."
                    AppLanguage.SPANISH -> "Hidratación de la piel" to if (humidity < 40) "La baja humedad reseca la piel rápidamente. Usa crema hidratante y bebe agua." else "La humedad ambiental ayuda a mantener la piel naturalmente hidratada."
                    AppLanguage.FRENCH -> "Hydratation de la peau" to if (humidity < 40) "Une faible humidité assèche vite la peau. Appliquez une crème hydratante et buvez suffisamment." else "L'humidité ambiante aide à préserver naturellement l'hydratation cutanée."
                    AppLanguage.PORTUGUESE -> "Hidratação da pele" to if (humidity < 40) "A baixa umidade resseca a pele rapidamente. Use hidratante e beba bastante água." else "A umidade do ar ajuda a manter a pele naturalmente hidratada."
                }

                HumidityAdviceRow(title = respiratoryTitle, desc = respiratoryDesc)
                HumidityAdviceRow(title = moldTitle, desc = moldDesc)
                HumidityAdviceRow(title = skinTitle, desc = skinDesc)
            }
        }
    }
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

private fun getHumidityColor(humidity: Int): Color = when {
    humidity < 35 -> Color(0xFFFF9800) // 乾燥（オレンジ）
    humidity <= 60 -> Color(0xFF4CAF50) // 快適（緑）
    humidity <= 75 -> Color(0xFF03A9F4) // やや多湿（水色）
    else -> Color(0xFF3F51B5) // 多湿（深青）
}

private fun getHumidityInfo(humidity: Int, language: AppLanguage): Triple<String, Color, String> {
    val color = getHumidityColor(humidity)
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
