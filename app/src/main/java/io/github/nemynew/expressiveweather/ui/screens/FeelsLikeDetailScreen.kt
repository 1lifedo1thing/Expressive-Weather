package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.TemperatureUnit
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.WindSpeedUnit
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.theme.currentAccentPalette
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold

/**
 * 体感温度（Feels Like / Comfort）詳細画面 - Material 3 Expressive 準拠、多言語対応、枠線フリー
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            AppLanguage.KOREAN -> "실제 기온보다 +${diffDisplay}${unitSymbol} 높음"
            AppLanguage.SIMPLIFIED_CHINESE -> "较实际气温偏高 +${diffDisplay}${unitSymbol}"
            AppLanguage.TRADITIONAL_CHINESE -> "較實際氣溫偏高 +${diffDisplay}${unitSymbol}"
            AppLanguage.SPANISH -> "+${diffDisplay}${unitSymbol} más cálido que la real"
            AppLanguage.FRENCH -> "+${diffDisplay}${unitSymbol} plus chaud que le réel"
            AppLanguage.PORTUGUESE -> "+${diffDisplay}${unitSymbol} mais quente que a real"
            AppLanguage.JAPANESE -> "実気温より +${diffDisplay}${unitSymbol} 高い"
        }
        diffDisplay < 0 -> when (language) {
            AppLanguage.ENGLISH -> "${diffDisplay}${unitSymbol} cooler than actual"
            AppLanguage.KOREAN -> "실제 기온보다 ${diffDisplay}${unitSymbol} 낮음"
            AppLanguage.SIMPLIFIED_CHINESE -> "较实际气温偏低 ${diffDisplay}${unitSymbol}"
            AppLanguage.TRADITIONAL_CHINESE -> "較實際氣溫偏低 ${diffDisplay}${unitSymbol}"
            AppLanguage.SPANISH -> "${diffDisplay}${unitSymbol} más fresco que la real"
            AppLanguage.FRENCH -> "${diffDisplay}${unitSymbol} plus frais que le réel"
            AppLanguage.PORTUGUESE -> "${diffDisplay}${unitSymbol} mais frio que a real"
            AppLanguage.JAPANESE -> "実気温より ${diffDisplay}${unitSymbol} 低い"
        }
        else -> when (language) {
            AppLanguage.ENGLISH -> "Similar to actual temperature"
            AppLanguage.KOREAN -> "실제 기온과 거의 같음"
            AppLanguage.SIMPLIFIED_CHINESE -> "与实际气温基本一致"
            AppLanguage.TRADITIONAL_CHINESE -> "與實際氣溫基本一致"
            AppLanguage.SPANISH -> "Similar a la temperatura real"
            AppLanguage.FRENCH -> "Proche de la température réelle"
            AppLanguage.PORTUGUESE -> "Semelhante à temperatura real"
            AppLanguage.JAPANESE -> "実気温とほぼ同じ"
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
        val colorScheme = MaterialTheme.colorScheme
        val accentPalette = currentAccentPalette()
        val isColorful = accentPalette.mode.isColorful
        val cloverShape = MaterialShapes.Cookie4Sided.toShape()
        val cloverColor = if (isColorful) accentPalette.feelsLike.accent else colorScheme.tertiary
        val tempIconColor = if (isColorful) accentPalette.feelsLike.accent else colorScheme.onSurface

        val (comfortPillBg, comfortPillText) = remember(feelsLikeC, isColorful, colorScheme, accentPalette) {
            if (isColorful) {
                val role = accentPalette.temperature(feelsLikeC.toDouble())
                role.accent to role.onAccent
            } else {
                when {
                    feelsLikeC >= 35 -> colorScheme.error to colorScheme.onError
                    feelsLikeC >= 28 -> colorScheme.errorContainer to colorScheme.onErrorContainer
                    feelsLikeC >= 23 -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
                    feelsLikeC in 16..22 -> colorScheme.tertiary to colorScheme.onTertiary
                    feelsLikeC in 10..15 -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
                    feelsLikeC in 5..9 -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
                    else -> colorScheme.surfaceContainerHighest to colorScheme.onSurface
                }
            }
        }

        val comfortTransition = rememberInfiniteTransition(label = "DetailComfortPulse")
        val pulseScale by comfortTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DetailComfortPulseScale"
        )

        // ================= 1. ヒーローセクション: 体感温度タイルと同一の Cookie4Sided クローバー & 実気温比較 =================
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
                        painter = painterResource(R.drawable.ic_fluent_temperature_24),
                        contentDescription = null,
                        tint = tempIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Apparent Temperature (BOM Index)"
                            AppLanguage.KOREAN -> "체감 온도 (체감 지수)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "体感温度 (BOM 体感指数)"
                            AppLanguage.TRADITIONAL_CHINESE -> "體感溫度 (BOM 體感指數)"
                            AppLanguage.SPANISH -> "Sensación térmica (Índice BOM)"
                            AppLanguage.FRENCH -> "Température ressentie (Indice BOM)"
                            AppLanguage.PORTUGUESE -> "Sensação térmica (Índice BOM)"
                            AppLanguage.JAPANESE -> "体感温度 (体感指数)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 体感温度タイル (ComfortCard) と同一の2層 Cookie4Sided クローバーシェイプ + 体感温度数値
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(188.dp)
                ) {
                    Surface(
                        shape = cloverShape,
                        color = cloverColor.copy(alpha = if (isColorful) 0.22f else 0.15f),
                        modifier = Modifier
                            .fillMaxSize(0.93f)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                    ) {}
                    Surface(
                        shape = cloverShape,
                        color = cloverColor.copy(alpha = if (isColorful) 0.48f else 0.32f),
                        modifier = Modifier.fillMaxSize(0.76f)
                    ) {}

                    Text(
                        text = "${feelsLikeDisplay}°",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 46.sp,
                        lineHeight = 46.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = CircleShape,
                    color = comfortPillBg
                ) {
                    Text(
                        text = diffText,
                        color = comfortPillText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
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
                                AppLanguage.KOREAN -> "실제 기온"
                                AppLanguage.SIMPLIFIED_CHINESE -> "实际气温"
                                AppLanguage.TRADITIONAL_CHINESE -> "實際氣溫"
                                AppLanguage.SPANISH -> "Temperatura real"
                                AppLanguage.FRENCH -> "Température réelle"
                                AppLanguage.PORTUGUESE -> "Temperatura real"
                                AppLanguage.JAPANESE -> "実際の気温"
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
                                AppLanguage.KOREAN -> "체감 온도"
                                AppLanguage.SIMPLIFIED_CHINESE -> "体感温度"
                                AppLanguage.TRADITIONAL_CHINESE -> "體感溫度"
                                AppLanguage.SPANISH -> "Sensación térmica"
                                AppLanguage.FRENCH -> "Ressenti"
                                AppLanguage.PORTUGUESE -> "Sensação térmica"
                                AppLanguage.JAPANESE -> "体感温度"
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
                        AppLanguage.KOREAN -> "오늘의 체감 온도 추이"
                        AppLanguage.SIMPLIFIED_CHINESE -> "今日体感温度变化"
                        AppLanguage.TRADITIONAL_CHINESE -> "今日體感溫度變化"
                        AppLanguage.SPANISH -> "Evolución de la sensación térmica"
                        AppLanguage.FRENCH -> "Évolution du ressenti aujourd'hui"
                        AppLanguage.PORTUGUESE -> "Tendência da sensação térmica hoje"
                        AppLanguage.JAPANESE -> "本日の体感温度推移"
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
                            AppLanguage.KOREAN -> "옷차림 지수 및 추천"
                            AppLanguage.SIMPLIFIED_CHINESE -> "穿衣指数与搭配建议"
                            AppLanguage.TRADITIONAL_CHINESE -> "穿衣指數與搭配建議"
                            AppLanguage.SPANISH -> "Recomendaciones de vestimenta"
                            AppLanguage.FRENCH -> "Conseils vestimentaires"
                            AppLanguage.PORTUGUESE -> "Recomendações de vestuário"
                            AppLanguage.JAPANESE -> "服装指数とアドバイス"
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
                                AppLanguage.KOREAN -> "바람과 습도의 영향을 반영한 추천 옷차림입니다."
                                AppLanguage.SIMPLIFIED_CHINESE -> "综合考量风速与湿度后得出的最佳穿搭指南。"
                                AppLanguage.TRADITIONAL_CHINESE -> "綜合考量風速與濕度後得出的最佳穿搭指南。"
                                AppLanguage.SPANISH -> "Vestimenta recomendada según el viento, la humedad y el confort térmico."
                                AppLanguage.FRENCH -> "Tenue recommandée selon le refroidissement éolien, l'humidité et le confort thermique."
                                AppLanguage.PORTUGUESE -> "Vestuário recomendado com base no vento, umidade e conforto térmico."
                                AppLanguage.JAPANESE -> "風速や湿度の影響を考慮した推奨スタイルです。"
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
                            AppLanguage.KOREAN -> "체감 온도의 원리"
                            AppLanguage.SIMPLIFIED_CHINESE -> "体感温度原理"
                            AppLanguage.TRADITIONAL_CHINESE -> "體感溫度原理"
                            AppLanguage.SPANISH -> "Cómo se calcula la sensación térmica"
                            AppLanguage.FRENCH -> "Comprendre la température ressentie"
                            AppLanguage.PORTUGUESE -> "Entendendo a sensação térmica"
                            AppLanguage.JAPANESE -> "体感温度の仕組み"
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
                        AppLanguage.KOREAN ->
                            "체감 온도는 기온에 습도와 풍속을 종합해 산출합니다(호주 기상청 BOM / Steadman 공식). 바람이 강할수록 체열이 빠르게 빠져나가 더 서늘하게 느껴지고, 습도가 높으면 땀 증발이 억제되어 실제 기온보다 무덥게 느껴집니다."
                        AppLanguage.SIMPLIFIED_CHINESE ->
                            "体感温度综合了环境气温、相对湿度与风速等多项物理指标（依据澳大利亚气象局 BOM / Steadman 国际公式）。风速增大会加速带走体表热量，而高湿度则会阻碍汗液自然蒸发使人倍感闷热。"
                        AppLanguage.TRADITIONAL_CHINESE ->
                            "體感溫度綜合了環境氣溫、相對濕度與風速等多項物理指標（依據澳洲氣象局 BOM / Steadman 國際公式）。風速增強會加速帶走體表熱量，高濕度則會阻礙汗液自然蒸發使人倍感悶熱。"
                        AppLanguage.SPANISH ->
                            "La sensación térmica combina la temperatura ambiente, la humedad relativa y la velocidad del viento (fórmula BOM de Australia / Steadman). El viento acelera el enfriamiento corporal, mientras que la humedad alta dificulta la evaporación del sudor."
                        AppLanguage.FRENCH ->
                            "La température ressentie combine la température ambiante, l'humidité relative et la vitesse du vent (formule BOM australienne / Steadman). Le vent accélère le refroidissement, tandis qu'une forte humidité freine l'évaporation de la transpiration."
                        AppLanguage.PORTUGUESE ->
                            "A sensação térmica combina a temperatura ambiente, a umidade relativa e a velocidade do vento (fórmula BOM australiana / Steadman). O vento acelera o resfriamento do corpo, enquanto a alta umidade dificulta a evaporação do suor."
                        AppLanguage.JAPANESE ->
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

@Composable
private fun getTempColor(tempC: Int): Color =
    currentAccentPalette().temperature(tempC.toDouble()).accent

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
            AppLanguage.KOREAN -> "습도가 높아 체열 발산이 억제되면서 실제 기온보다 더 덥고 후텁지근하게 느껴집니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "湿度较高抑制体表排汗散热，体感明显比实际气温更加温热潮湿。"
            AppLanguage.TRADITIONAL_CHINESE -> "濕度較高抑制體表排汗散熱，體感明顯比實際氣溫更加悶熱。"
            AppLanguage.SPANISH -> "La humedad elevada dificulta la disipación del calor corporal, generando una sensación más cálida y bochornosa."
            AppLanguage.FRENCH -> "L'humidité élevée limite la dissipation de la chaleur corporelle, rendant l'air nettement plus chaud et lourd."
            AppLanguage.PORTUGUESE -> "A umidade elevada dificulta a dissipação do calor corporal, deixando a sensação mais quente e abafada."
            AppLanguage.JAPANESE -> "湿度が高めのため、皮膚からの熱放散が抑えられ、実際よりも暖かく（蒸し暑く）感じられます。"
        }
        diff < 0 && windSpeedKmH > 15.0 -> when (language) {
            AppLanguage.ENGLISH -> "Active airflow enhances evaporative wind chill, creating a cooler sensation than the ambient temperature."
            AppLanguage.KOREAN -> "바람의 영향으로 체열이 빠르게 식어 실제 기온보다 서늘하거나 쌀쌀하게 느껴집니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "受风力与风寒效应影响，体表散热加快，感觉明显比实际温度偏凉。"
            AppLanguage.TRADITIONAL_CHINESE -> "受風力與風寒效應影響，體表散熱加快，感覺明顯比實際溫度偏涼。"
            AppLanguage.SPANISH -> "El viento intensifica la sensación de frío, haciendo que se perciba más fresco que la temperatura real."
            AppLanguage.FRENCH -> "Le vent accentue le refroidissement éolien, donnant une sensation plus fraîche que la température réelle."
            AppLanguage.PORTUGUESE -> "O vento intensifica o resfriamento, criando uma sensação mais fria do que a temperatura ambiente."
            AppLanguage.JAPANESE -> "風が吹いているため、風冷効果（ウィンドチル）により実際の気温よりも涼しく（肌寒く）感じられます。"
        }
        else -> when (language) {
            AppLanguage.ENGLISH -> "Humidity and airflow are well-balanced; the apparent temperature closely tracks the thermometer."
            AppLanguage.KOREAN -> "습도와 풍속의 균형이 적절해 실제 기온과 거의 비슷한 쾌적한 상태입니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "湿度与风速比例适中，体感温度与实际气温非常接近。"
            AppLanguage.TRADITIONAL_CHINESE -> "濕度與風速比例適中，體感溫度與實際氣溫非常接近。"
            AppLanguage.SPANISH -> "La humedad y el viento están equilibrados; la sensación térmica coincide casi con el termómetro."
            AppLanguage.FRENCH -> "L'humidité et le vent sont équilibrés ; le ressenti correspond presque à la température affichée."
            AppLanguage.PORTUGUESE -> "A umidade e o vento estão equilibrados; a sensação térmica acompanha de perto o termômetro."
            AppLanguage.JAPANESE -> "湿度と風速のバランスが標準的で、ほぼ実際の気温に近い心地よさです。"
        }
    }

    val outfit = when {
        feelsLike >= 28 -> when (language) {
            AppLanguage.ENGLISH -> "T-shirt weather. Stay hydrated and stay cool under direct sunlight."
            AppLanguage.KOREAN -> "반팔 차림이 알맞은 무더운 날씨입니다. 온열 질환 예방과 수분 섭취에 신경 쓰세요."
            AppLanguage.SIMPLIFIED_CHINESE -> "短袖或清凉夏装适宜。请注意防暑降温与补充水分。"
            AppLanguage.TRADITIONAL_CHINESE -> "短袖或清涼夏裝適宜。請注意防暑降溫與補充水分。"
            AppLanguage.SPANISH -> "Ideal para camiseta ligera. Mantente hidratado y busca sombra bajo el sol directo."
            AppLanguage.FRENCH -> "Temps de t-shirt. Hydratez-vous bien et restez au frais au soleil."
            AppLanguage.PORTUGUESE -> "Clima para camiseta leve. Hidrate-se bem e proteja-se do sol direto."
            AppLanguage.JAPANESE -> "半袖で快適にお過ごしいただけます。熱中症対策・水分補給を心がけてください。"
        }
        feelsLike >= 23 -> when (language) {
            AppLanguage.ENGLISH -> "Short sleeves or light long sleeves are perfect for comfortable outings."
            AppLanguage.KOREAN -> "반팔이나 얇은 긴팔 셔츠가 알맞은 쾌적한 날씨입니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "短袖或薄长袖衬衫即可，体感舒适宜人。"
            AppLanguage.TRADITIONAL_CHINESE -> "短袖或薄長袖襯衫即可，體感舒適宜人。"
            AppLanguage.SPANISH -> "Manga corta o camisa ligera de manga larga son ideales para salir."
            AppLanguage.FRENCH -> "Manches courtes ou manches longues légères parfaites pour sortir."
            AppLanguage.PORTUGUESE -> "Manga curta ou manga longa leve são ideais para um passeio confortável."
            AppLanguage.JAPANESE -> "半袖または薄手の長袖シャツが快適な過ごしやすい気候です。"
        }
        feelsLike >= 17 -> when (language) {
            AppLanguage.ENGLISH -> "Long sleeves, casual sweaters, or light cardigans are ideal."
            AppLanguage.KOREAN -> "긴팔 티셔츠나 셔츠, 얇은 카디건을 챙기면 안심입니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "长袖衬衫、卫衣或薄开衫尤为适宜，早晚防风。"
            AppLanguage.TRADITIONAL_CHINESE -> "長袖襯衫、薄長T或薄開衫尤為適宜。"
            AppLanguage.SPANISH -> "Manga larga, suéter ligero o cárdigan fino son lo más adecuado."
            AppLanguage.FRENCH -> "Manches longues, pull léger ou gilet fin sont idéaux."
            AppLanguage.PORTUGUESE -> "Manga longa, suéter leve ou cardigã fino são ideais."
            AppLanguage.JAPANESE -> "長袖シャツやカットソー、薄手のカーディガンがあると安心です。"
        }
        feelsLike >= 12 -> when (language) {
            AppLanguage.ENGLISH -> "Jackets, trenches, knit sweaters, or windbreakers are recommended."
            AppLanguage.KOREAN -> "니트나 재킷, 트렌치코트 등 겉옷을 챙겨 입는 것이 좋습니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "建议穿着夹克外衣、薄毛衣或风衣防风保暖。"
            AppLanguage.TRADITIONAL_CHINESE -> "建議穿著夾克外衣、薄毛衣或風衣防風保暖。"
            AppLanguage.SPANISH -> "Se recomiendan chaquetas, gabardinas, jerséis de punto o cortavientos."
            AppLanguage.FRENCH -> "Veste, trench, pull en maille ou coupe-vent recommandés."
            AppLanguage.PORTUGUESE -> "Jaquetas, casacos leves, suéteres de tricô ou corta-ventos são recomendados."
            AppLanguage.JAPANESE -> "セーターやジャケット、トレンチコートなどの上着が必要です。"
        }
        feelsLike >= 6 -> when (language) {
            AppLanguage.ENGLISH -> "Heavier winter coats, scarves, and insulated layers are necessary."
            AppLanguage.KOREAN -> "겨울 코트와 목도리 등 든든한 방한 복장을 권장합니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "需穿着保暖大衣、羽绒服或羊毛围巾抵御寒冷。"
            AppLanguage.TRADITIONAL_CHINESE -> "需穿著保暖大衣、羽絨衣或羊毛圍巾禦寒。"
            AppLanguage.SPANISH -> "Se necesitan abrigos de invierno, bufandas y capas térmicas."
            AppLanguage.FRENCH -> "Manteau d'hiver épais, écharpe et couches isolantes nécessaires."
            AppLanguage.PORTUGUESE -> "Casacos de inverno pesados, cachecóis e camadas térmicas são necessários."
            AppLanguage.JAPANESE -> "冬物コートやマフラーなどのしっかりとした防寒着をお選びください。"
        }
        else -> when (language) {
            AppLanguage.ENGLISH -> "Heavy down coat, thermal underlayers, gloves, and knit caps essential."
            AppLanguage.KOREAN -> "패딩과 장갑, 보온 이너웨어 등 철저한 방한 대비가 필수입니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "严寒低温，请配备厚羽绒服、保暖手套及全面防寒保暖装备。"
            AppLanguage.TRADITIONAL_CHINESE -> "嚴寒低溫，請配備厚羽絨衣、保暖手套及全面防寒保暖裝備。"
            AppLanguage.SPANISH -> "Abrigo de plumas grueso, ropa térmica, guantes y gorro son imprescindibles."
            AppLanguage.FRENCH -> "Doudoune épaisse, sous-vêtements thermiques, gants et bonnet indispensables."
            AppLanguage.PORTUGUESE -> "Casaco acolchoado pesado, roupas térmicas, luvas e gorro são essenciais."
            AppLanguage.JAPANESE -> "ダウンジャケットや手袋、万全の防寒対策が必須です。"
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
