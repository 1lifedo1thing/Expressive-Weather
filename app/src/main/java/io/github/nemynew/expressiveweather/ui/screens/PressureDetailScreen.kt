package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.MaterialYouAccentPalette
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.theme.currentAccentPalette
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold
import java.util.Locale

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
        val colorScheme = MaterialTheme.colorScheme
        val accentPalette = currentAccentPalette()
        val (pressureState, stateColor, trendDesc, isWarning) = getPressureAnalysis(pressure, language, accentPalette)
        val pressureColor = accentPalette.pressure.accent
        val gaugeTrackColor = pressureColor.copy(alpha = if (accentPalette.mode.isColorful) 0.26f else 0.32f)
        // ================= 1. ヒーローセクション: 気圧タイルと同一のバロメーターゲージ & 現在気圧 =================
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
                        painter = painterResource(R.drawable.ic_atmospheric_pressure),
                        contentDescription = null,
                        tint = pressureColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Surface Pressure (Barometer)"
                            AppLanguage.SPANISH -> "Presión en superficie (Barómetro)"
                            AppLanguage.FRENCH -> "Pression de surface (Baromètre)"
                            AppLanguage.PORTUGUESE -> "Pressão na superfície (Barômetro)"
                            AppLanguage.KOREAN -> "현지 기압 (기압계)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "地面气压 (气压计)"
                            AppLanguage.TRADITIONAL_CHINESE -> "地面氣壓 (氣壓計)"
                            AppLanguage.JAPANESE -> "現地気圧 (バロメーター)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 気圧タイル (PressureCard) と同一の円形アークメーター (970〜1050 hPa・135°〜270°・微小パルス)
                Box(
                    modifier = Modifier.size(184.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val minPressure = 970f
                    val maxPressure = 1050f
                    val targetProgress = ((pressure - minPressure) / (maxPressure - minPressure)).coerceIn(0.04f, 1f)
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                        label = "BarometerGaugeProgress"
                    )
                    val infiniteTransition = rememberInfiniteTransition(label = "DetailPressurePulseAnimation")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.996f,
                        targetValue = 1.004f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "DetailPressurePulseScale"
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                    ) {
                        val strokeWidthPx = 13.dp.toPx()
                        val halfStroke = strokeWidthPx / 2f
                        val arcSize = Size(
                            width = size.width - strokeWidthPx,
                            height = size.height - strokeWidthPx
                        )
                        val topLeft = Offset(halfStroke, halfStroke)
                        val startAngle = 135f
                        val totalSweep = 270f

                        // 背景トラック（タイルと同一）
                        drawArc(
                            color = gaugeTrackColor,
                            startAngle = startAngle,
                            sweepAngle = totalSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )

                        // アクティブプログレス（タイルと同一）
                        drawArc(
                            color = pressureColor,
                            startAngle = startAngle,
                            sweepAngle = totalSweep * animatedProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )
                    }

                    val formattedPressure = remember(pressure) {
                        String.format(Locale.US, "%,d", pressure)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formattedPressure,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "hPa",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
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
                        AppLanguage.ENGLISH -> "Reference Sea-Level Pressure"
                        AppLanguage.SPANISH -> "Presión de referencia al nivel del mar"
                        AppLanguage.FRENCH -> "Pression de référence au niveau de la mer"
                        AppLanguage.PORTUGUESE -> "Pressão de referência ao nível do mar"
                        AppLanguage.KOREAN -> "기준 해면 기압 참고치"
                        AppLanguage.SIMPLIFIED_CHINESE -> "参考海平面气压"
                        AppLanguage.TRADITIONAL_CHINESE -> "參考海平面氣壓"
                        AppLanguage.JAPANESE -> "基準海面気圧の目安"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (language) {
                        AppLanguage.ENGLISH -> "Hourly pressure forecast series is not provided by the current data source; the latest observed sea-level pressure is shown as a reference."
                        AppLanguage.SPANISH -> "La fuente de datos actual no ofrece previsión horaria de presión; se muestra la última presión observada al nivel del mar como referencia."
                        AppLanguage.FRENCH -> "La source de données actuelle ne fournit pas de prévision horaire de pression ; la dernière pression observée au niveau de la mer est affichée à titre indicatif."
                        AppLanguage.PORTUGUESE -> "A fonte de dados atual não fornece previsão horária de pressão; a última pressão observada ao nível do mar é exibida como referência."
                        AppLanguage.KOREAN -> "현재 데이터 제공처에서 시간별 기압 예보를 제공하지 않아 최신 관측 해면 기압을 기준치로 표시합니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "当前数据源未提供逐小时气压预测序列，以下以最新实测海平面气压作为参考基准。"
                        AppLanguage.TRADITIONAL_CHINESE -> "目前資料來源未提供逐小時氣壓預測序列，以下以最新實測海平面氣壓作為參考基準。"
                        AppLanguage.JAPANESE -> "現在のデータソースでは時間別気圧予測が提供されていないため、最新の実測海面気圧を基準値として表示しています。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                val observedPressure = weather?.pressureHpa ?: 1013
                val barHeight = ((observedPressure - 980) / 40f).coerceIn(0.15f, 1f) * 60
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(hourlyForecast.take(24)) { item ->
                        val itemPressure = observedPressure
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
                                        .background(if (itemPressure >= 1013) accentPalette.severity(0).accent else accentPalette.severity(2).accent)
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
                        tint = if (isWarning) accentPalette.severity(2).accent else accentPalette.severity(0).accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Barometric Sensitivity & Wellness"
                            AppLanguage.SPANISH -> "Sensibilidad barométrica y bienestar"
                            AppLanguage.FRENCH -> "Sensibilité barométrique et bien-être"
                            AppLanguage.PORTUGUESE -> "Sensibilidade barométrica e bem-estar"
                            AppLanguage.KOREAN -> "기압 변화와 건강 가이드"
                            AppLanguage.SIMPLIFIED_CHINESE -> "气压变化与健康关怀"
                            AppLanguage.TRADITIONAL_CHINESE -> "氣壓變化與健康關懷"
                            AppLanguage.JAPANESE -> "気圧変化と健康アドバイス"
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
                        AppLanguage.SPANISH -> if (isWarning) "Aviso de baja presión" else "Tendencia barométrica estable"
                        AppLanguage.FRENCH -> if (isWarning) "Alerte basse pression" else "Tendance barométrique stable"
                        AppLanguage.PORTUGUESE -> if (isWarning) "Alerta de baixa pressão" else "Tendência barométrica estável"
                        AppLanguage.KOREAN -> if (isWarning) "저기압 컨디션 주의" else "기압 안정세"
                        AppLanguage.SIMPLIFIED_CHINESE -> if (isWarning) "低气压不适提醒" else "气压平稳正常"
                        AppLanguage.TRADITIONAL_CHINESE -> if (isWarning) "低氣壓不適提醒" else "氣壓平穩正常"
                        AppLanguage.JAPANESE -> if (isWarning) "気圧低下に伴う体調への配慮" else "気圧は安定推移"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> if (isWarning)
                            "Decreased barometric pressure may induce headaches, fatigue, or joint sensitivity. Keep well-hydrated and rest as needed."
                        else
                            "Standard atmospheric pressure is maintained; environmental effects on physical comfort remain minimal."
                        AppLanguage.SPANISH -> if (isWarning)
                            "La baja presión barométrica puede provocar dolor de cabeza, fatiga o molestias articulares. Mantente bien hidratado y descansa si lo necesitas."
                        else
                            "Se mantiene una presión atmosférica estándar; los efectos en el bienestar físico son mínimos."
                        AppLanguage.FRENCH -> if (isWarning)
                            "Une baisse de la pression barométrique peut provoquer maux de tête, fatigue ou douleurs articulaires. Hydratez-vous bien et reposez-vous si nécessaire."
                        else
                            "La pression atmosphérique reste dans la normale ; son impact sur le bien-être physique est minimal."
                        AppLanguage.PORTUGUESE -> if (isWarning)
                            "A queda na pressão barométrica pode causar dor de cabeça, fadiga ou desconforto articular. Hidrate-se bem e descanse quando necessário."
                        else
                            "A pressão atmosférica permanece no padrão normal, com impacto mínimo no bem-estar físico."
                        AppLanguage.KOREAN -> if (isWarning)
                            "기압이 낮거나 하강하는 추세입니다. 두통, 피로감, 관절 불편감을 느끼기 쉬우니 무리하지 말고 충분히 휴식하세요."
                        else
                            "표준 기압 범위가 유지되고 있어 기압 변화로 인한 컨디션 영향은 적을 전망입니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> if (isWarning)
                            "气压呈下降或偏低趋势，容易引发植物神经紊乱、头痛或倦怠。建议保持放松并适度休息。"
                        else
                            "处于标准舒适气压区间，气压变化对身体状态基本无影响。"
                        AppLanguage.TRADITIONAL_CHINESE -> if (isWarning)
                            "氣壓呈下降或偏低趨勢，容易引發自律神經失調、頭痛或倦怠。建議保持放鬆並適度休息。"
                        else
                            "處於標準舒適氣壓區間，氣壓變化對身體狀態基本無影響。"
                        AppLanguage.JAPANESE -> if (isWarning)
                            "気圧が低めまたは下降傾向です。自律神経の乱れ、頭痛やだるさを感じやすい時間帯です。無理のないペースでお過ごしください。"
                        else
                            "標準的な気圧が維持されており、気圧変化による体調への影響は少ない見込みです。"
                    }
                )

                PressureAdviceRow(
                    icon = Icons.Rounded.Compress,
                    title = when (language) {
                        AppLanguage.ENGLISH -> "Standard Atmospheric Baseline"
                        AppLanguage.SPANISH -> "Referencia atmosférica estándar"
                        AppLanguage.FRENCH -> "Référence atmosphérique standard"
                        AppLanguage.PORTUGUESE -> "Referência atmosférica padrão"
                        AppLanguage.KOREAN -> "표준 대기압과의 관계"
                        AppLanguage.SIMPLIFIED_CHINESE -> "标准大气压基准"
                        AppLanguage.TRADITIONAL_CHINESE -> "標準大氣壓基準"
                        AppLanguage.JAPANESE -> "標準大気圧との関係"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH ->
                            "Mean sea-level atmospheric pressure is 1013.25 hPa. Higher values generally bring clear and stable skies, while lower pressure indicates passing troughs or fronts."
                        AppLanguage.SPANISH ->
                            "La presión atmosférica media al nivel del mar es de 1013,25 hPa. Valores más altos suelen traer cielos despejados y estables; valores más bajos indican el paso de borrascas o frentes."
                        AppLanguage.FRENCH ->
                            "La pression atmosphérique moyenne au niveau de la mer est de 1013,25 hPa. Des valeurs plus élevées apportent généralement un temps calme et dégagé, tandis qu'une baisse annonce le passage de dépressions ou de fronts."
                        AppLanguage.PORTUGUESE ->
                            "A pressão atmosférica média ao nível do mar é de 1013,25 hPa. Valores mais altos costumam trazer céu limpo e tempo estável, enquanto valores mais baixos indicam a aproximação de frentes ou áreas de baixa pressão."
                        AppLanguage.KOREAN ->
                            "표준 대기압(1기압)은 1013.25 hPa입니다. 이보다 높으면 고기압 영향으로 맑고 안정적인 날씨가 이어지며, 낮으면 저기압이나 기압골·전선이 다가오기 쉽습니다."
                        AppLanguage.SIMPLIFIED_CHINESE ->
                            "海平面标准大气压为 1013.25 hPa。高于此值通常受高气压影响而晴好，低于此值则易受低气压或天气锋面影响。"
                        AppLanguage.TRADITIONAL_CHINESE ->
                            "海平面標準大氣壓為 1013.25 hPa。高於此數值通常代表受高氣壓籠罩而晴朗，低於此數值則易伴隨陰雨及鋒面系統。"
                        AppLanguage.JAPANESE ->
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

private fun getPressureAnalysis(
    pressure: Int,
    language: AppLanguage,
    palette: MaterialYouAccentPalette
): Quadruple<String, Color, String, Boolean> {
    return when {
        pressure >= 1018 -> Quadruple(
            when (language) {
                AppLanguage.ENGLISH -> "High Pressure System"
                AppLanguage.SPANISH -> "Sistema de alta presión"
                AppLanguage.FRENCH -> "Système anticyclonique"
                AppLanguage.PORTUGUESE -> "Sistema de alta pressão"
                AppLanguage.KOREAN -> "고기압 영향권"
                AppLanguage.SIMPLIFIED_CHINESE -> "受高气压控制"
                AppLanguage.TRADITIONAL_CHINESE -> "受高氣壓控制"
                AppLanguage.JAPANESE -> "高気圧に覆われています"
            },
            palette.severity(0).accent,
            when (language) {
                AppLanguage.ENGLISH -> "Atmospheric pressure is high and stable. Expect sunny weather with no pressure-induced discomfort."
                AppLanguage.SPANISH -> "La presión atmosférica es alta y estable. Se espera tiempo soleado sin molestias por presión."
                AppLanguage.FRENCH -> "La pression atmosphérique est élevée et stable. Temps ensoleillé attendu sans gêne liée à la pression."
                AppLanguage.PORTUGUESE -> "A pressão atmosférica está alta e estável. Espera-se tempo ensolarado sem desconforto causado pela pressão."
                AppLanguage.KOREAN -> "기압이 높고 안정적입니다. 맑은 날씨가 이어지기 쉬우며 기압에 의한 컨디션 영향은 거의 없습니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "气压较高且稳定。晴好天气居多，气压对身体几乎无不良影响。"
                AppLanguage.TRADITIONAL_CHINESE -> "氣壓較高且穩定。晴朗天氣居多，氣壓對身體幾乎無不良影響。"
                AppLanguage.JAPANESE -> "気圧が高く安定しています。晴天が続きやすく、気圧による体調への影響はほとんどありません。"
            },
            false
        )
        pressure >= 1010 -> Quadruple(
            when (language) {
                AppLanguage.ENGLISH -> "Normal & Stable Pressure"
                AppLanguage.SPANISH -> "Presión normal y estable"
                AppLanguage.FRENCH -> "Pression normale et stable"
                AppLanguage.PORTUGUESE -> "Pressão normal e estável"
                AppLanguage.KOREAN -> "표준 기압 부근 안정"
                AppLanguage.SIMPLIFIED_CHINESE -> "处于标准气压附近"
                AppLanguage.TRADITIONAL_CHINESE -> "處於標準氣壓附近"
                AppLanguage.JAPANESE -> "標準気圧付近で安定"
            },
            palette.pressure.accent,
            when (language) {
                AppLanguage.ENGLISH -> "Fluctuating around normal pressure (1013 hPa); minimal bodily stress expected."
                AppLanguage.SPANISH -> "Oscila en torno a la presión normal (1013 hPa); se espera un impacto mínimo en el organismo."
                AppLanguage.FRENCH -> "Pression proche de la normale (1013 hPa) ; impact minimal sur l'organisme."
                AppLanguage.PORTUGUESE -> "Oscilando em torno da pressão normal (1013 hPa); impacto mínimo esperado no organismo."
                AppLanguage.KOREAN -> "표준 대기압(1013 hPa) 전후로 유지되어 신체에 미치는 부담이 적은 상태입니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "气压在标准大气压(1013hPa)附近波动，对身体无明显额外负担。"
                AppLanguage.TRADITIONAL_CHINESE -> "氣壓在標準大氣壓(1013hPa)附近波動，對身體無明顯額外負擔。"
                AppLanguage.JAPANESE -> "標準大気圧(1013hPa)前後で推移しており、身体への急激な負荷は少ない環境です。"
            },
            false
        )
        pressure >= 1002 -> Quadruple(
            when (language) {
                AppLanguage.ENGLISH -> "Slightly Low Pressure"
                AppLanguage.SPANISH -> "Presión ligeramente baja"
                AppLanguage.FRENCH -> "Pression légèrement basse"
                AppLanguage.PORTUGUESE -> "Pressão ligeiramente baixa"
                AppLanguage.KOREAN -> "다소 낮은 기압"
                AppLanguage.SIMPLIFIED_CHINESE -> "气压略微偏低"
                AppLanguage.TRADITIONAL_CHINESE -> "氣壓略微偏低"
                AppLanguage.JAPANESE -> "気圧やや低め"
            },
            palette.severity(2).accent,
            when (language) {
                AppLanguage.ENGLISH -> "Pressure is slightly depressed. Sensitive individuals may experience mild headaches or fatigue."
                AppLanguage.SPANISH -> "La presión está algo baja. Las personas sensibles pueden notar ligeros dolores de cabeza o cansancio."
                AppLanguage.FRENCH -> "La pression est légèrement basse. Les personnes sensibles peuvent ressentir de légers maux de tête ou de la fatigue."
                AppLanguage.PORTUGUESE -> "A pressão está ligeiramente baixa. Pessoas sensíveis podem sentir leves dores de cabeça ou cansaço."
                AppLanguage.KOREAN -> "기압이 다소 낮은 편입니다. 민감한 경우 가벼운 두통이나 목·어깨 뻐근함을 느낄 수 있습니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "气压略微偏低。敏感体质者可能会感到轻微头痛或颈肩酸胀。"
                AppLanguage.TRADITIONAL_CHINESE -> "氣壓略微偏低。敏感體質者可能會感到輕微頭痛或頸肩緊繃。"
                AppLanguage.JAPANESE -> "気圧がやや低下しています。敏感な方は軽い頭痛や首肩のこりを感じる場合があります。"
            },
            true
        )
        else -> Quadruple(
            when (language) {
                AppLanguage.ENGLISH -> "Low Pressure System (Caution)"
                AppLanguage.SPANISH -> "Sistema de baja presión (Precaución)"
                AppLanguage.FRENCH -> "Dépression (Prudence)"
                AppLanguage.PORTUGUESE -> "Sistema de baixa pressão (Atenção)"
                AppLanguage.KOREAN -> "저기압 접근 (주의)"
                AppLanguage.SIMPLIFIED_CHINESE -> "低气压接近 (注意)"
                AppLanguage.TRADITIONAL_CHINESE -> "低氣壓接近 (注意)"
                AppLanguage.JAPANESE -> "低気圧接近中 (警戒)"
            },
            palette.severity(3).accent,
            when (language) {
                AppLanguage.ENGLISH -> "Pressure is significantly reduced. May trigger headaches, joint stiffness, and lethargy; prioritize rest."
                AppLanguage.SPANISH -> "La presión es significativamente baja. Puede provocar dolor de cabeza, rigidez articular y decaimiento; prioriza el descanso."
                AppLanguage.FRENCH -> "La pression est nettement basse. Elle peut déclencher maux de tête, raideurs articulaires et fatigue ; privilégiez le repos."
                AppLanguage.PORTUGUESE -> "A pressão está significativamente baixa. Pode causar dor de cabeça, rigidez articular e cansaço; priorize o descanso."
                AppLanguage.KOREAN -> "기압이 크게 낮아진 상태입니다. 편두통, 관절통, 나른함이 나타나기 쉬우니 충분히 휴식하세요."
                AppLanguage.SIMPLIFIED_CHINESE -> "气压明显偏低。容易引起偏头痛、关节不适与倦怠感，请注意劳逸结合与充足休息。"
                AppLanguage.TRADITIONAL_CHINESE -> "氣壓明顯偏低。容易引起偏頭痛、關節不適與倦怠感，請注意適度休息。"
                AppLanguage.JAPANESE -> "気圧がかなり低くなっています。偏頭痛、関節痛、倦怠感が生じやすいため、十分な休息をお取りください。"
            },
            true
        )
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

