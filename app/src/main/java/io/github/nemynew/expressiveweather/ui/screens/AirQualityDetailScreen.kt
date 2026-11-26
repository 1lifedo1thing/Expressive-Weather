package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.AirQuality
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.MaterialYouAccentPalette
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.theme.currentAccentPalette
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold
import io.github.nemynew.expressiveweather.ui.components.getDetailCardColor

/**
 * 空気質（Air Quality / AQI）詳細画面
 * Material 3 Expressive 準拠（枠線なし、トナルサーフェス、4言語対応）
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val airQuality = weather?.airQuality ?: AirQuality(
        aqi = 0,
        levelLabel = "--",
        pm25 = 0.0,
        pm10 = 0.0,
        advice = "",
        isAvailable = false
    )
    val isAvailable = airQuality.isAvailable
    val aqi = airQuality.aqi

    val aqiHeaderLabel = when (language) {
        AppLanguage.JAPANESE -> "大気質指数 (AQI)"
        AppLanguage.KOREAN -> "대기질 지수 (AQI)"
        AppLanguage.SIMPLIFIED_CHINESE -> "空气质量指数 (AQI)"
        AppLanguage.TRADITIONAL_CHINESE -> "空氣品質指數 (AQI)"
        AppLanguage.ENGLISH -> "Air Quality Index (AQI)"
        AppLanguage.SPANISH -> "Índice de calidad del aire (ICA)"
        AppLanguage.FRENCH -> "Indice de qualité de l'air (IQA)"
        AppLanguage.PORTUGUESE -> "Índice de qualidade do ar (IQA)"
        AppLanguage.RUSSIAN -> "Индекс качества воздуха (AQI)"
    }

    val pollutantsTitle = when (language) {
        AppLanguage.JAPANESE -> "主要汚染物質の測定値"
        AppLanguage.KOREAN -> "주요 대기오염물질 측정치"
        AppLanguage.SIMPLIFIED_CHINESE -> "主要污染物浓度监测"
        AppLanguage.TRADITIONAL_CHINESE -> "主要污染物濃度監測"
        AppLanguage.ENGLISH -> "Key Pollutant Measurements"
        AppLanguage.SPANISH -> "Contaminantes principales"
        AppLanguage.FRENCH -> "Principaux polluants mesurés"
        AppLanguage.PORTUGUESE -> "Principais poluentes medidos"
        AppLanguage.RUSSIAN -> "Основные загрязнители"
    }

    val adviceSectionTitle = when (language) {
        AppLanguage.JAPANESE -> "空気質と健康・生活アドバイス"
        AppLanguage.KOREAN -> "대기질 및 건강·생활 가이드"
        AppLanguage.SIMPLIFIED_CHINESE -> "空气质量与健康防护建议"
        AppLanguage.TRADITIONAL_CHINESE -> "空氣品質與健康防護建議"
        AppLanguage.ENGLISH -> "Air Quality Health Guidelines"
        AppLanguage.SPANISH -> "Recomendaciones de salud y aire"
        AppLanguage.FRENCH -> "Conseils santé et qualité de l'air"
        AppLanguage.PORTUGUESE -> "Recomendações de saúde e ar"
        AppLanguage.RUSSIAN -> "Рекомендации по здоровью"
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
        val colorScheme = MaterialTheme.colorScheme
        val accentPalette = currentAccentPalette()
        val isColorful = accentPalette.mode.isColorful
        val puffyShape = MaterialShapes.Puffy.toShape()
        val puffyColor = if (isColorful) accentPalette.aqi.accent else colorScheme.secondary
        val aqiIconColor = if (isColorful) accentPalette.aqi.accent else colorScheme.secondary

        val (aqiStatus, aqiColor, aqiSummary) = if (isAvailable) {
            getAqiInfo(aqi, language, accentPalette)
        } else {
            Triple(
                when (language) {
                    AppLanguage.JAPANESE -> "データなし"
                    AppLanguage.KOREAN -> "데이터 없음"
                    AppLanguage.SIMPLIFIED_CHINESE -> "暂无数据"
                    AppLanguage.TRADITIONAL_CHINESE -> "暫無資料"
                    AppLanguage.ENGLISH -> "Unavailable"
                    AppLanguage.SPANISH -> "No disponible"
                    AppLanguage.FRENCH -> "Indisponible"
                    AppLanguage.PORTUGUESE -> "Indisponível"
                    AppLanguage.RUSSIAN -> "Нет данных"
                },
                colorScheme.outline,
                when (language) {
                    AppLanguage.JAPANESE -> "現在この地域の大気質観測データは取得できません。"
                    AppLanguage.KOREAN -> "현재 이 지역의 대기질 관측 데이터를 불러올 수 없습니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "当前暂无法获取该地区的空气质量监测数据。"
                    AppLanguage.TRADITIONAL_CHINESE -> "目前暫無法取得該地區的空氣品質監測資料。"
                    AppLanguage.ENGLISH -> "Air quality observation data is currently unavailable for this location."
                    AppLanguage.SPANISH -> "Los datos de calidad del aire no están disponibles actualmente para esta ubicación."
                    AppLanguage.FRENCH -> "Les données de qualité de l'air sont actuellement indisponibles pour ce lieu."
                    AppLanguage.PORTUGUESE -> "Os dados de qualidade do ar não estão disponíveis no momento para este local."
                    AppLanguage.RUSSIAN -> "Данные о качестве воздуха для этого места сейчас недоступны."
                }
            )
        }

        val (aqiPillBg, aqiPillText) = remember(aqi, isAvailable, isColorful, colorScheme, accentPalette) {
            if (!isAvailable) {
                colorScheme.surfaceContainerHighest to colorScheme.onSurfaceVariant
            } else if (isColorful) {
                val tier = when {
                    aqi <= 50 -> 0
                    aqi <= 100 -> 1
                    aqi <= 150 -> 2
                    aqi <= 200 -> 3
                    else -> 4
                }
                val role = accentPalette.severity(tier)
                role.accent to role.onAccent
            } else {
                when {
                    aqi <= 50 -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
                    aqi <= 100 -> colorScheme.secondary to colorScheme.onSecondary
                    aqi <= 150 -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
                    aqi <= 200 -> colorScheme.errorContainer to colorScheme.onErrorContainer
                    else -> colorScheme.error to colorScheme.onError
                }
            }
        }

        val puffyTransition = rememberInfiniteTransition(label = "DetailAqiAnimation")
        val floatY by puffyTransition.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DetailAqiFloatY"
        )
        val aqiScale by puffyTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DetailAqiScale"
        )

        // ================= 1. ヒーローセクション: 空気質タイルと同一の Puffy シェイプ & スケールバー =================
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = getDetailCardColor()
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
                        painter = painterResource(R.drawable.ic_fluent_weather_duststorm_24),
                        contentDescription = null,
                        tint = aqiIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = aqiHeaderLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 空気質タイル (AqiCard) と同一の2層浮遊・脈動 Puffy シェイプ + AQI数値
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(188.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(180.dp)
                            .graphicsLayer {
                                translationY = floatY * 2.5f
                                scaleX = aqiScale
                                scaleY = aqiScale
                            }
                    ) {
                        Surface(
                            shape = puffyShape,
                            color = puffyColor.copy(alpha = if (isColorful) 0.24f else 0.18f),
                            modifier = Modifier.size(180.dp)
                        ) {}
                        Surface(
                            shape = puffyShape,
                            color = puffyColor.copy(alpha = if (isColorful) 0.50f else 0.35f),
                            modifier = Modifier.size(156.dp)
                        ) {}
                    }

                    Text(
                        text = if (isAvailable) aqi.toString() else "--",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 46.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = CircleShape,
                    color = aqiPillBg
                ) {
                    Text(
                        text = aqiStatus,
                        color = aqiPillText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
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
                            AppLanguage.KOREAN -> "50 (좋음)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "50 (优)"
                            AppLanguage.TRADITIONAL_CHINESE -> "50 (優)"
                            AppLanguage.ENGLISH -> "50 (Good)"
                            AppLanguage.SPANISH -> "50 (Buena)"
                            AppLanguage.FRENCH -> "50 (Bon)"
                            AppLanguage.PORTUGUESE -> "50 (Bom)"
                            AppLanguage.RUSSIAN -> "50 (Хор.)"
                        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(when (language) {
                            AppLanguage.JAPANESE -> "100 (普)"
                            AppLanguage.KOREAN -> "100 (보통)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "100 (良)"
                            AppLanguage.TRADITIONAL_CHINESE -> "100 (普)"
                            AppLanguage.ENGLISH -> "100 (Mod)"
                            AppLanguage.SPANISH -> "100 (Mod)"
                            AppLanguage.FRENCH -> "100 (Mod)"
                            AppLanguage.PORTUGUESE -> "100 (Mod)"
                            AppLanguage.RUSSIAN -> "100 (Умер.)"
                        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(when (language) {
                            AppLanguage.JAPANESE -> "150 (注意)"
                            AppLanguage.KOREAN -> "150 (민감)"
                            AppLanguage.SIMPLIFIED_CHINESE -> "150 (敏感)"
                            AppLanguage.TRADITIONAL_CHINESE -> "150 (敏感)"
                            AppLanguage.ENGLISH -> "150 (Sens)"
                            AppLanguage.SPANISH -> "150 (Sens)"
                            AppLanguage.FRENCH -> "150 (Sens)"
                            AppLanguage.PORTUGUESE -> "150 (Sens)"
                            AppLanguage.RUSSIAN -> "150 (Чувств.)"
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
                containerColor = getDetailCardColor()
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
                    AppLanguage.KOREAN -> "초미세먼지"
                    AppLanguage.SIMPLIFIED_CHINESE -> "细颗粒物"
                    AppLanguage.TRADITIONAL_CHINESE -> "細懸浮微粒"
                    AppLanguage.ENGLISH -> "Fine Particles"
                    AppLanguage.SPANISH -> "Partículas finas"
                    AppLanguage.FRENCH -> "Particules fines"
                    AppLanguage.PORTUGUESE -> "Partículas finas"
                    AppLanguage.RUSSIAN -> "Мелкие частицы"
                }

                val pm10Sub = when (language) {
                    AppLanguage.JAPANESE -> "粒子状物質"
                    AppLanguage.KOREAN -> "미세먼지"
                    AppLanguage.SIMPLIFIED_CHINESE -> "可吸入颗粒物"
                    AppLanguage.TRADITIONAL_CHINESE -> "懸浮微粒"
                    AppLanguage.ENGLISH -> "Inhalable Particles"
                    AppLanguage.SPANISH -> "Partículas inhalables"
                    AppLanguage.FRENCH -> "Particules inhalables"
                    AppLanguage.PORTUGUESE -> "Partículas inaláveis"
                    AppLanguage.RUSSIAN -> "Крупные частицы"
                }

                val o3Sub = when (language) {
                    AppLanguage.JAPANESE -> "地表オゾン"
                    AppLanguage.KOREAN -> "지표 오존"
                    AppLanguage.SIMPLIFIED_CHINESE -> "地面臭氧"
                    AppLanguage.TRADITIONAL_CHINESE -> "地面臭氧"
                    AppLanguage.ENGLISH -> "Surface Ozone"
                    AppLanguage.SPANISH -> "Ozono troposférico"
                    AppLanguage.FRENCH -> "Ozone de surface"
                    AppLanguage.PORTUGUESE -> "Ozônio superficial"
                    AppLanguage.RUSSIAN -> "Приземный озон"
                }

                val no2Sub = when (language) {
                    AppLanguage.JAPANESE -> "二酸化窒素"
                    AppLanguage.KOREAN -> "이산화질소"
                    AppLanguage.SIMPLIFIED_CHINESE -> "二氧化氮"
                    AppLanguage.TRADITIONAL_CHINESE -> "二氧化氮"
                    AppLanguage.ENGLISH -> "Nitrogen Dioxide"
                    AppLanguage.SPANISH -> "Dióxido de nitrógeno"
                    AppLanguage.FRENCH -> "Dioxyde d'azote"
                    AppLanguage.PORTUGUESE -> "Dióxido de nitrogênio"
                    AppLanguage.RUSSIAN -> "Диоксид азота"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PollutantCard(
                        title = "PM2.5",
                        sub = pm25Sub,
                        value = if (isAvailable) "${airQuality.pm25} μg/m³" else "--",
                        isGood = isAvailable && airQuality.pm25 <= 15.0,
                        modifier = Modifier.weight(1f)
                    )
                    PollutantCard(
                        title = "PM10",
                        sub = pm10Sub,
                        value = if (isAvailable) "${airQuality.pm10} μg/m³" else "--",
                        isGood = isAvailable && airQuality.pm10 <= 45.0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val o3Val = airQuality.ozone
                    val no2Val = airQuality.nitrogenDioxide
                    PollutantCard(
                        title = "O₃",
                        sub = o3Sub,
                        value = when {
                            !isAvailable -> "--"
                            o3Val != null -> "${o3Val} μg/m³"
                            else -> "--"
                        },
                        isGood = isAvailable && (o3Val == null || o3Val <= 100.0),
                        modifier = Modifier.weight(1f)
                    )
                    PollutantCard(
                        title = "NO₂",
                        sub = no2Sub,
                        value = when {
                            !isAvailable -> "--"
                            no2Val != null -> "${no2Val} μg/m³"
                            else -> "--"
                        },
                        isGood = isAvailable && (no2Val == null || no2Val <= 40.0),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ================= 3. 健康・生活アドバイス =================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = getDetailCardColor()
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

                if (!isAvailable) {
                    val unavailableAdvice = when (language) {
                        AppLanguage.JAPANESE -> "現在この地域の大気質データが取得できないため、換気・屋外活動・マスクに関する具体的なガイダンスを表示できません。"
                        AppLanguage.KOREAN -> "현재 이 지역의 대기질 데이터를 불러올 수 없어 환기·야외 활동·마스크 착용 가이드를 표시할 수 없습니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "当前暂未获取到该地区的空气质量数据，无法提供具体的通风、户外活动或口罩防护建议。"
                        AppLanguage.TRADITIONAL_CHINESE -> "目前暫未取得該地區的空氣品質資料，無法提供具體的通風、戶外活動或口罩防護建議。"
                        AppLanguage.ENGLISH -> "Air quality data is currently unavailable for this location, so ventilation, outdoor activity, and mask guidance cannot be determined."
                        AppLanguage.SPANISH -> "No hay datos de calidad del aire disponibles para esta ubicación, por lo que no es posible mostrar recomendaciones de ventilación, actividad exterior o mascarilla."
                        AppLanguage.FRENCH -> "Les données de qualité de l'air étant indisponibles pour ce lieu, il est impossible d'afficher des conseils sur l'aération, les activités extérieures ou le masque."
                        AppLanguage.PORTUGUESE -> "Os dados de qualidade do ar não estão disponíveis para este local, portanto não é possível exibir recomendações de ventilação, atividades ao ar livre ou uso de máscara."
                        AppLanguage.RUSSIAN -> "Данные о качестве воздуха для этого места недоступны, поэтому рекомендации по проветриванию, прогулкам и маскам не могут быть показаны."
                    }
                    Text(
                        text = unavailableAdvice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val (ventTitle, ventDesc) = when (language) {
                        AppLanguage.JAPANESE -> "室内の換気" to if (aqi <= 50) "窓を開けて積極的に新鮮な外気を取り入れましょう。" else "空気清浄機を活用し、窓の開放は短時間に抑えましょう。"
                        AppLanguage.KOREAN -> "실내 환기" to if (aqi <= 50) "공기가 쾌적해 창문을 열고 충분히 환기하기 좋습니다." else "공기청정기를 사용하고 환기는 짧게 하는 것이 좋습니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "室内通风" to if (aqi <= 50) "空气清新，适宜开窗通风换气。" else "建议开启空气净化器，适当减少开窗通风时间。"
                        AppLanguage.TRADITIONAL_CHINESE -> "室內通風" to if (aqi <= 50) "空氣清新，適宜開窗通風換氣。" else "建議開啟空氣清淨機，適當減少開窗通風時間。"
                        AppLanguage.ENGLISH -> "Indoor Ventilation" to if (aqi <= 50) "Air is fresh and ideal for opening windows." else "Keep windows closed and use an air purifier if needed."
                        AppLanguage.SPANISH -> "Ventilación interior" to if (aqi <= 50) "El aire es fresco e ideal para abrir las ventanas y ventilar." else "Mantén las ventanas cerradas y usa un purificador de aire si es posible."
                        AppLanguage.FRENCH -> "Aération intérieure" to if (aqi <= 50) "L'air est frais et idéal pour ouvrir les fenêtres." else "Gardez les fenêtres fermées et utilisez un purificateur d'air si nécessaire."
                        AppLanguage.PORTUGUESE -> "Ventilação interna" to if (aqi <= 50) "O ar está limpo e ideal para abrir as janelas." else "Mantenha as janelas fechadas e use um purificador de ar se necessário."
                        AppLanguage.RUSSIAN -> "Проветривание" to if (aqi <= 50) "Воздух чистый — отлично подходит для проветривания." else "Держите окна закрытыми и при возможности используйте очиститель воздуха."
                    }

                    val (outdoorTitle, outdoorDesc) = when (language) {
                        AppLanguage.JAPANESE -> "屋外活動" to if (aqi <= 100) "ウォーキングやランニングなど、普段通り快適に行えます。" else "激しい屋外運動は控え、屋内でのトレーニングを推奨します。"
                        AppLanguage.KOREAN -> "야외 활동" to if (aqi <= 100) "산책이나 조깅 등 평소처럼 쾌적하게 야외 활동을 즐길 수 있습니다." else "격렬한 야외 운동은 자제하고 실내 운동을 권장합니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "户外运动" to if (aqi <= 100) "可正常开展户外徒步、跑步等各项体育运动。" else "敏感人群应减少剧烈户外活动，优先选择室内锻炼。"
                        AppLanguage.TRADITIONAL_CHINESE -> "戶外運動" to if (aqi <= 100) "可正常開展戶外徒步、跑步等各項體育運動。" else "敏感族群應減少劇烈戶外活動，優先選擇室內鍛鍊。"
                        AppLanguage.ENGLISH -> "Outdoor Activity" to if (aqi <= 100) "Great conditions for outdoor exercise and recreation." else "Reduce prolonged or heavy exertion outdoors."
                        AppLanguage.SPANISH -> "Actividad exterior" to if (aqi <= 100) "Excelentes condiciones para hacer ejercicio y actividades al aire libre." else "Reduce el esfuerzo intenso o prolongado al aire libre."
                        AppLanguage.FRENCH -> "Activité extérieure" to if (aqi <= 100) "Conditions idéales pour le sport et les loisirs en plein air." else "Limitez les efforts intenses ou prolongés en extérieur."
                        AppLanguage.PORTUGUESE -> "Atividades ao ar livre" to if (aqi <= 100) "Ótimas condições para exercícios e lazer ao ar livre." else "Reduza esforços intensos ou prolongados ao ar livre."
                        AppLanguage.RUSSIAN -> "Активность на улице" to if (aqi <= 100) "Отличные условия для прогулок и спорта на свежем воздухе." else "Ограничьте длительные и интенсивные нагрузки на улице."
                    }

                    val (maskTitle, maskDesc) = when (language) {
                        AppLanguage.JAPANESE -> "マスクの着用" to if (aqi <= 50) "マスクの着用は不要です。" else "呼吸器疾患をお持ちの方や敏感な方は、微粒子対応マスクの着用が推奨されます。"
                        AppLanguage.KOREAN -> "마스크 착용" to if (aqi <= 50) "마스크를 착용하지 않아도 괜찮습니다." else "호흡기 질환자나 민감군은 KF80/KF94 등 보건용 마스크 착용을 권장합니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "口罩防护" to if (aqi <= 50) "空气纯净，无需佩戴口罩。" else "呼吸道敏感人群出行建议佩戴防颗粒物口罩。"
                        AppLanguage.TRADITIONAL_CHINESE -> "口罩防護" to if (aqi <= 50) "空氣純淨，無需配戴口罩。" else "呼吸道敏感族群出行建議配戴防微粒口罩。"
                        AppLanguage.ENGLISH -> "Mask Advisory" to if (aqi <= 50) "No mask required." else "Sensitive individuals should consider wearing particulate masks outdoors."
                        AppLanguage.SPANISH -> "Uso de mascarilla" to if (aqi <= 50) "No es necesario usar mascarilla." else "Se recomienda mascarilla filtrante para personas sensibles al salir."
                        AppLanguage.FRENCH -> "Port du masque" to if (aqi <= 50) "Le port du masque n'est pas nécessaire." else "Les personnes sensibles devraient envisager un masque filtrant à l'extérieur."
                        AppLanguage.PORTUGUESE -> "Uso de máscara" to if (aqi <= 50) "Não é necessário usar máscara." else "Pessoas sensíveis devem considerar usar máscara com filtro ao sair."
                        AppLanguage.RUSSIAN -> "Защитная маска" to if (aqi <= 50) "Маска не требуется." else "Чувствительным людям стоит использовать защитную маску на улице."
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
}

@Composable
private fun PollutantCard(
    title: String,
    sub: String,
    value: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    val accentPalette = currentAccentPalette()
    val tagColor = if (isGood) accentPalette.severity(0).accent else accentPalette.severity(2).accent
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

private fun getAqiInfo(
    aqi: Int,
    language: AppLanguage,
    palette: MaterialYouAccentPalette
): Triple<String, Color, String> {
    val color = when {
        aqi <= 50 -> palette.severity(0).accent
        aqi <= 100 -> palette.severity(1).accent
        aqi <= 150 -> palette.severity(2).accent
        aqi <= 200 -> palette.severity(3).accent
        else -> palette.severity(4).accent
    }

    val label = when {
        aqi <= 50 -> when (language) {
            AppLanguage.JAPANESE -> "良い"
            AppLanguage.KOREAN -> "좋음"
            AppLanguage.SIMPLIFIED_CHINESE -> "优"
            AppLanguage.TRADITIONAL_CHINESE -> "優"
            AppLanguage.ENGLISH -> "Good"
            AppLanguage.SPANISH -> "Buena"
            AppLanguage.FRENCH -> "Bonne"
            AppLanguage.PORTUGUESE -> "Boa"
            AppLanguage.RUSSIAN -> "Хорошее"
        }
        aqi <= 100 -> when (language) {
            AppLanguage.JAPANESE -> "普通"
            AppLanguage.KOREAN -> "보통"
            AppLanguage.SIMPLIFIED_CHINESE -> "良"
            AppLanguage.TRADITIONAL_CHINESE -> "普通"
            AppLanguage.ENGLISH -> "Moderate"
            AppLanguage.SPANISH -> "Moderada"
            AppLanguage.FRENCH -> "Modérée"
            AppLanguage.PORTUGUESE -> "Moderada"
            AppLanguage.RUSSIAN -> "Умеренное"
        }
        aqi <= 150 -> when (language) {
            AppLanguage.JAPANESE -> "敏感な人に影響"
            AppLanguage.KOREAN -> "민감군 나쁨"
            AppLanguage.SIMPLIFIED_CHINESE -> "轻度污染 (敏感人群)"
            AppLanguage.TRADITIONAL_CHINESE -> "輕度污染 (敏感族群)"
            AppLanguage.ENGLISH -> "Unhealthy for Sensitive Groups"
            AppLanguage.SPANISH -> "Perjudicial para grupos sensibles"
            AppLanguage.FRENCH -> "Mauvaise pour les personnes sensibles"
            AppLanguage.PORTUGUESE -> "Ruim para grupos sensíveis"
            AppLanguage.RUSSIAN -> "Вредно для чувствительных"
        }
        aqi <= 200 -> when (language) {
            AppLanguage.JAPANESE -> "不健康"
            AppLanguage.KOREAN -> "나쁨"
            AppLanguage.SIMPLIFIED_CHINESE -> "中度污染"
            AppLanguage.TRADITIONAL_CHINESE -> "中度污染"
            AppLanguage.ENGLISH -> "Unhealthy"
            AppLanguage.SPANISH -> "Insalubre"
            AppLanguage.FRENCH -> "Mauvaise"
            AppLanguage.PORTUGUESE -> "Insalubre"
            AppLanguage.RUSSIAN -> "Вредное"
        }
        else -> when (language) {
            AppLanguage.JAPANESE -> "極めて不健康"
            AppLanguage.KOREAN -> "매우 나쁨"
            AppLanguage.SIMPLIFIED_CHINESE -> "重度及严重污染"
            AppLanguage.TRADITIONAL_CHINESE -> "重度及嚴重污染"
            AppLanguage.ENGLISH -> "Hazardous"
            AppLanguage.SPANISH -> "Peligrosa"
            AppLanguage.FRENCH -> "Dangereuse"
            AppLanguage.PORTUGUESE -> "Perigosa"
            AppLanguage.RUSSIAN -> "Опасное"
        }
    }

    val advice = when {
        aqi <= 50 -> when (language) {
            AppLanguage.JAPANESE -> "大気環境は非常に清浄です。健康被害のリスクはありません。"
            AppLanguage.KOREAN -> "대기 상태가 매우 쾌적합니다. 건강상 우려 없이 야외 활동하기 좋습니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "空气质量极佳，非常适合户外运动与各类活动。"
            AppLanguage.TRADITIONAL_CHINESE -> "空氣品質極佳，非常適合戶外運動與各類活動。"
            AppLanguage.ENGLISH -> "Air quality is satisfactory and poses little or no risk."
            AppLanguage.SPANISH -> "La calidad del aire es satisfactoria y presenta poco o ningún riesgo."
            AppLanguage.FRENCH -> "La qualité de l'air est satisfaisante et présente peu ou pas de risque."
            AppLanguage.PORTUGUESE -> "A qualidade do ar é satisfatória e oferece pouco ou nenhum risco."
            AppLanguage.RUSSIAN -> "Качество воздуха отличное, риска для здоровья практически нет."
        }
        aqi <= 100 -> when (language) {
            AppLanguage.JAPANESE -> "空気質は許容範囲内です。極端に敏感な方以外は問題ありません。"
            AppLanguage.KOREAN -> "대기질은 무난한 수준입니다. 특히 민감한 경우가 아니라면 일상 활동에 무리가 없습니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "空气质量可接受，极少数异常敏感人群除外。"
            AppLanguage.TRADITIONAL_CHINESE -> "空氣品質可接受，極少數異常敏感族群除外。"
            AppLanguage.ENGLISH -> "Air quality is acceptable for most people."
            AppLanguage.SPANISH -> "La calidad del aire es aceptable para la mayoría de las personas."
            AppLanguage.FRENCH -> "La qualité de l'air est acceptable pour la plupart des personnes."
            AppLanguage.PORTUGUESE -> "A qualidade do ar é aceitável para a maioria das pessoas."
            AppLanguage.RUSSIAN -> "Качество воздуха приемлемое для большинства людей."
        }
        aqi <= 150 -> when (language) {
            AppLanguage.JAPANESE -> "呼吸器系に持病のある方や高齢者・子供は長時間の外出に注意してください。"
            AppLanguage.KOREAN -> "호흡기 질환자, 노약자, 어린이는 장시간 실외 활동 시 주의하세요."
            AppLanguage.SIMPLIFIED_CHINESE -> "易感人群症状有轻度加剧可能，建议减少户外剧烈运动。"
            AppLanguage.TRADITIONAL_CHINESE -> "易感族群症狀有輕度加劇可能，建議減少戶外劇烈運動。"
            AppLanguage.ENGLISH -> "Members of sensitive groups may experience health effects."
            AppLanguage.SPANISH -> "Los grupos sensibles pueden experimentar efectos en la salud."
            AppLanguage.FRENCH -> "Les personnes sensibles peuvent ressentir des effets sur leur santé."
            AppLanguage.PORTUGUESE -> "Pessoas de grupos sensíveis podem sentir efeitos na saúde."
            AppLanguage.RUSSIAN -> "Чувствительные группы могут испытывать дискомфорт."
        }
        aqi <= 200 -> when (language) {
            AppLanguage.JAPANESE -> "すべての人が影響を感じる可能性があります。屋外での長時間の運動はお控えください。"
            AppLanguage.KOREAN -> "모든 사람에게 건강상 영향이 나타날 수 있습니다. 장시간 야외 운동을 자제하세요."
            AppLanguage.SIMPLIFIED_CHINESE -> "所有人群健康都可能受到影响，建议适当减少户外运动。"
            AppLanguage.TRADITIONAL_CHINESE -> "所有人群健康都可能受到影響，建議適當減少戶外運動。"
            AppLanguage.ENGLISH -> "Everyone may begin to experience health effects; limit outdoor exertion."
            AppLanguage.SPANISH -> "Toda la población puede verse afectada; limita el esfuerzo al aire libre."
            AppLanguage.FRENCH -> "Tout le monde peut ressentir des effets ; limitez les efforts en extérieur."
            AppLanguage.PORTUGUESE -> "Todos podem sentir efeitos na saúde; limite esforços ao ar livre."
            AppLanguage.RUSSIAN -> "Возможно влияние на здоровье у всех; ограничьте нагрузки на улице."
        }
        else -> when (language) {
            AppLanguage.JAPANESE -> "健康被害のリスクが高まっています。屋内で空気清浄機を使用して過ごしてください。"
            AppLanguage.KOREAN -> "건강 위해 위험이 높습니다. 가급적 실내에 머무르며 공기청정기를 가동하세요."
            AppLanguage.SIMPLIFIED_CHINESE -> "健康警告：所有人应避免户外运动，室内开启空气净化设备。"
            AppLanguage.TRADITIONAL_CHINESE -> "健康警告：所有人應避免戶外運動，室內開啟空氣清淨設備。"
            AppLanguage.ENGLISH -> "Health alert: serious risk of effects for all individuals. Stay indoors."
            AppLanguage.SPANISH -> "Alerta sanitaria: riesgo grave para toda la población. Permanece en interiores."
            AppLanguage.FRENCH -> "Alerte sanitaire : risque grave pour tous. Restez à l'intérieur."
            AppLanguage.PORTUGUESE -> "Alerta de saúde: risco grave para todos. Permaneça em ambientes fechados."
            AppLanguage.RUSSIAN -> "Опасно для здоровья: серьёзный риск для всех. Оставайтесь в помещении."
        }
    }

    return Triple(label, color, advice)
}
