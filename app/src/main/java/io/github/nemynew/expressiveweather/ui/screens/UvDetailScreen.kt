package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WbSunny
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
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.data.getLocalizedDayLabel
import io.github.nemynew.expressiveweather.data.getLocalizedTimeLabel
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.MaterialYouAccentPalette
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.theme.currentAccentPalette
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold
import io.github.nemynew.expressiveweather.ui.components.getDetailCardColor

/**
 * 紫外線（UV Index）詳細画面
 * Material 3 Expressive 準拠（枠線なし、トナルサーフェス、4言語対応）
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UvDetailScreen(
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
    val uvIndex = weather?.uvIndex ?: 0
    val hourlyForecast = weather?.hourlyForecast ?: emptyList()
    val dailyForecast = weather?.dailyForecast ?: emptyList()

    val currentUvLabel = when (language) {
        AppLanguage.JAPANESE -> "現在の紫外線指数"
        AppLanguage.KOREAN -> "현재 자외선 지수"
        AppLanguage.SIMPLIFIED_CHINESE -> "当前紫外线指数"
        AppLanguage.TRADITIONAL_CHINESE -> "當前紫外線指數"
        AppLanguage.ENGLISH -> "Current UV Index"
        AppLanguage.SPANISH -> "Índice UV actual"
        AppLanguage.FRENCH -> "Indice UV actuel"
        AppLanguage.PORTUGUESE -> "Índice UV atual"
        AppLanguage.RUSSIAN -> "Текущий УФ-индекс"
    }

    val hourlyUvTitle = when (language) {
        AppLanguage.JAPANESE -> "本日の紫外線推移"
        AppLanguage.KOREAN -> "오늘의 자외선 추이"
        AppLanguage.SIMPLIFIED_CHINESE -> "今日紫外线走势"
        AppLanguage.TRADITIONAL_CHINESE -> "今日紫外線走勢"
        AppLanguage.ENGLISH -> "Hourly UV Index"
        AppLanguage.SPANISH -> "Índice UV por hora"
        AppLanguage.FRENCH -> "Indice UV horaire"
        AppLanguage.PORTUGUESE -> "Índice UV por hora"
        AppLanguage.RUSSIAN -> "УФ-индекс по часам"
    }

    val dailyUvTitle = when (language) {
        AppLanguage.JAPANESE -> "7日間の紫外線予報"
        AppLanguage.KOREAN -> "7일 자외선 예보"
        AppLanguage.SIMPLIFIED_CHINESE -> "7天紫外线预报"
        AppLanguage.TRADITIONAL_CHINESE -> "7天紫外線預報"
        AppLanguage.ENGLISH -> "7-Day UV Forecast"
        AppLanguage.SPANISH -> "Previsión UV a 7 días"
        AppLanguage.FRENCH -> "Prévisions UV sur 7 jours"
        AppLanguage.PORTUGUESE -> "Previsão UV para 7 dias"
        AppLanguage.RUSSIAN -> "Прогноз УФ на 7 дней"
    }

    val guideTitle = when (language) {
        AppLanguage.JAPANESE -> "紫外線対策ガイド (WHO推奨)"
        AppLanguage.KOREAN -> "자외선 차단 가이드 (WHO 권장)"
        AppLanguage.SIMPLIFIED_CHINESE -> "防晒防护指南 (WHO标准)"
        AppLanguage.TRADITIONAL_CHINESE -> "防曬防護指南 (WHO標準)"
        AppLanguage.ENGLISH -> "Sun Protection Guide (WHO)"
        AppLanguage.SPANISH -> "Guía de protección solar (OMS)"
        AppLanguage.FRENCH -> "Guide de protection solaire (OMS)"
        AppLanguage.PORTUGUESE -> "Guia de proteção solar (OMS)"
        AppLanguage.RUSSIAN -> "Защита от солнца (ВОЗ)"
    }

    DetailScaffold(
        title = strings.uvIndex,
        city = city,
        weather = weather,
        language = language,
        metricType = DetailMetricType.UV,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val accentPalette = currentAccentPalette()
        val isColorful = accentPalette.mode.isColorful
        val (uvCategory, uvColor, uvAdvice) = getUvInfo(uvIndex, language, accentPalette)
        val sunnyShape = MaterialShapes.VerySunny.toShape()
        val sunnyColor = if (isColorful) accentPalette.uv.accent else colorScheme.tertiaryContainer
        val onSunnyColor = if (isColorful) accentPalette.uv.onAccent else colorScheme.onTertiaryContainer
        val uvIconColor = if (isColorful) accentPalette.uv.accent else colorScheme.secondary

        val (uvPillBg, uvPillText) = remember(uvIndex, isColorful, colorScheme, accentPalette) {
            if (isColorful) {
                val tier = when {
                    uvIndex <= 2 -> 0
                    uvIndex <= 5 -> 1
                    uvIndex <= 7 -> 2
                    uvIndex <= 10 -> 3
                    else -> 4
                }
                val role = accentPalette.severity(tier)
                role.accent to role.onAccent
            } else {
                when {
                    uvIndex <= 2 -> colorScheme.surfaceContainerHighest to colorScheme.onSurface
                    uvIndex <= 5 -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
                    uvIndex <= 7 -> colorScheme.tertiary to colorScheme.onTertiary
                    uvIndex <= 10 -> colorScheme.errorContainer to colorScheme.onErrorContainer
                    else -> colorScheme.error to colorScheme.onError
                }
            }
        }

        val uvTransition = rememberInfiniteTransition(label = "DetailUvAnimation")
        val sunnyRotation by uvTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 26000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "DetailUvRotate"
        )
        val sunnyPulse by uvTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DetailUvScale"
        )

        // ================= 1. ヒーローセクション: 紫外線タイルと同一の VerySunny シェイプ & UV指数 =================
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
                        painter = painterResource(R.drawable.ic_fluent_weather_sunny_high_24),
                        contentDescription = null,
                        tint = uvIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentUvLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 紫外線タイル (UvCard) と同一の回転・パルス VerySunny シェイプ + UV指数 & レベルバッジ
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(188.dp)
                ) {
                    Surface(
                        shape = sunnyShape,
                        color = sunnyColor,
                        modifier = Modifier
                            .size(176.dp)
                            .graphicsLayer {
                                rotationZ = sunnyRotation
                                scaleX = sunnyPulse
                                scaleY = sunnyPulse
                            }
                    ) {}

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uvIndex.toString(),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 46.sp,
                            lineHeight = 46.sp,
                            color = onSunnyColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = uvPillBg
                        ) {
                            Text(
                                text = uvCategory,
                                color = uvPillText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = uvAdvice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontFamily = GoogleSansFlex,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // ================= 2. 24時間紫外線予報グラフ =================
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
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = uvColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = hourlyUvTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val maxUv = maxOf(
                        dailyForecast.firstOrNull()?.uvMax?.let { kotlin.math.round(it).toInt() } ?: (weather?.uvIndex ?: 0),
                        weather?.uvIndex ?: 0
                    )
                    items(hourlyForecast.take(24)) { item ->
                        val itemUv = item.uvIndex?.let { kotlin.math.round(it).toInt().coerceAtLeast(0) }
                            ?: if (item.hour in 6..18) {
                                val factor = kotlin.math.sin(Math.PI * (item.hour - 6) / 12.0)
                                kotlin.math.round(maxUv * factor).toInt().coerceAtLeast(0)
                            } else {
                                0
                            }
                        val itemColor = getUvColor(itemUv)
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
                                        .height((60 * (itemUv / 11f).coerceIn(0.08f, 1f)).dp)
                                        .clip(CircleShape)
                                        .background(itemColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = itemUv.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (itemUv > 0) itemColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ================= 3. 週間紫外線予報 =================
        if (dailyForecast.isNotEmpty()) {
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
                        text = dailyUvTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    dailyForecast.take(7).forEach { day ->
                        val dayUv = kotlin.math.round(day.uvMax).toInt()
                        val dayColor = getUvColor(dayUv)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getLocalizedDayLabel(day.dayLabel, language),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.width(82.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // 水平プログレス
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((dayUv / 12f).coerceIn(0.05f, 1f))
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(dayColor)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "UV $dayUv",
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = dayColor,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.width(52.dp)
                            )
                        }
                    }
                }
            }
        }

        // ================= 4. 紫外線対策・健康アドバイス =================
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
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = guideTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val (sunscreenTitle, sunscreenDesc) = when (language) {
                    AppLanguage.JAPANESE -> "日焼け止めの使用" to if (uvIndex >= 6) "SPF50+ / PA++++ の日焼け止めを2時間おきに塗り直してください。" else "日常的な外出には SPF30 程度で十分です。"
                    AppLanguage.KOREAN -> "자외선 차단제 사용" to if (uvIndex >= 6) "SPF50+ / PA++++ 자외선 차단제를 2시간마다 덧발라 주세요." else "일상적인 외출에는 SPF30 정도로 충분합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "防晒霜涂抹" to if (uvIndex >= 6) "建议涂抹高倍数防晒霜(SPF50+)，且每2小时补涂一次。" else "日常出行选择SPF30左右防晒霜即可。"
                    AppLanguage.TRADITIONAL_CHINESE -> "防曬乳塗抹" to if (uvIndex >= 6) "建議塗抹高倍數防曬乳(SPF50+)，且每2小時補塗一次。" else "日常出行選擇SPF30左右防曬乳即可。"
                    AppLanguage.ENGLISH -> "Sunscreen Application" to if (uvIndex >= 6) "Apply broad-spectrum SPF 50+ sunscreen every 2 hours." else "SPF 30 is generally sufficient for regular outdoor activities."
                    AppLanguage.SPANISH -> "Aplicación de protector solar" to if (uvIndex >= 6) "Aplica protector solar de amplio espectro FPS 50+ cada 2 horas." else "Un FPS 30 suele ser suficiente para actividades habituales al aire libre."
                    AppLanguage.FRENCH -> "Application de crème solaire" to if (uvIndex >= 6) "Appliquez une crème solaire large spectre SPF 50+ toutes les 2 heures." else "Un SPF 30 suffit généralement pour les activités extérieures courantes."
                    AppLanguage.PORTUGUESE -> "Uso de protetor solar" to if (uvIndex >= 6) "Aplique protetor solar de amplo espectro FPS 50+ a cada 2 horas." else "FPS 30 geralmente é suficiente para atividades rotineiras ao ar livre."
                    AppLanguage.RUSSIAN -> "Солнцезащитный крем" to if (uvIndex >= 6) "Наносите крем широкого спектра SPF 50+ каждые 2 часа." else "Для обычных прогулок обычно достаточно SPF 30."
                }

                val (sunglassTitle, sunglassDesc) = when (language) {
                    AppLanguage.JAPANESE -> "サングラスと帽子" to "目や頭皮への紫外線ダメージを防ぐため、UVカットサングラスや広つばの帽子の着用を推奨します。"
                    AppLanguage.KOREAN -> "선글라스와 모자" to "눈과 두피를 자외선으로부터 보호하기 위해 UV 차단 선글라스와 챙이 넓은 모자 착용을 권장합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "太阳镜与遮阳帽" to "佩戴UV400防紫外线太阳镜与宽檐遮阳帽，保护眼部和头皮免受辐射损伤。"
                    AppLanguage.TRADITIONAL_CHINESE -> "太陽眼鏡與遮陽帽" to "配戴UV400防紫外線太陽眼鏡與寬簷遮陽帽，保護眼部和頭皮免受輻射損傷。"
                    AppLanguage.ENGLISH -> "Sunglasses & Hat" to "Wear UV-blocking sunglasses and a wide-brimmed hat to protect eyes and scalp."
                    AppLanguage.SPANISH -> "Gafas de sol y sombrero" to "Usa gafas de sol con filtro UV y un sombrero de ala ancha para proteger los ojos y el cuero cabelludo."
                    AppLanguage.FRENCH -> "Lunettes de soleil et chapeau" to "Portez des lunettes de soleil anti-UV et un chapeau à larges bords pour protéger vos yeux et votre cuir chevelu."
                    AppLanguage.PORTUGUESE -> "Óculos de sol e chapéu" to "Use óculos de sol com proteção UV e chapéu de aba larga para proteger os olhos e o couro cabeludo."
                    AppLanguage.RUSSIAN -> "Очки и головной убор" to "Носите очки с УФ-фильтром и шляпу с широкими полями для защиты глаз и кожи головы."
                }

                val (shadeTitle, shadeDesc) = when (language) {
                    AppLanguage.JAPANESE -> "日陰の活用" to "10:00〜14:00の間は紫外線が最も強くなります。可能な限り日陰や屋内でお過ごしください。"
                    AppLanguage.KOREAN -> "그늘 활용" to "오전 10시~오후 2시는 자외선이 가장 강한 시간대입니다. 가급적 그늘이나 실내에서 활동하세요."
                    AppLanguage.SIMPLIFIED_CHINESE -> "寻找阴凉处" to "上午10点至下午2点紫外线最强烈，尽量在林荫下或室内活动。"
                    AppLanguage.TRADITIONAL_CHINESE -> "尋找陰涼處" to "上午10點至下午2點紫外線最為強烈，盡量在樹蔭下或室內活動。"
                    AppLanguage.ENGLISH -> "Seek Shade" to "UV radiation peaks between 10:00 AM and 2:00 PM; seek shade whenever possible."
                    AppLanguage.SPANISH -> "Busca la sombra" to "La radiación UV alcanza su pico entre las 10:00 y las 14:00; busca sombra siempre que puedas."
                    AppLanguage.FRENCH -> "Restez à l'ombre" to "Les rayons UV atteignent leur pic entre 10 h et 14 h ; privilégiez l'ombre dès que possible."
                    AppLanguage.PORTUGUESE -> "Procure sombra" to "A radiação UV atinge o pico entre 10h e 14h; procure sombra sempre que possível."
                    AppLanguage.RUSSIAN -> "Держитесь в тени" to "Пик УФ-излучения приходится на 10:00–14:00 — по возможности оставайтесь в тени."
                }

                UvProtectionItem(title = sunscreenTitle, desc = sunscreenDesc)
                UvProtectionItem(title = sunglassTitle, desc = sunglassDesc)
                UvProtectionItem(title = shadeTitle, desc = shadeDesc)
            }
        }
    }
}

@Composable
private fun UvProtectionItem(title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
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

@Composable
private fun getUvColor(uv: Int): Color = getUvColor(uv, currentAccentPalette())

private fun getUvColor(uv: Int, palette: MaterialYouAccentPalette): Color = when {
    uv <= 2 -> palette.severity(0).accent
    uv <= 5 -> palette.severity(1).accent
    uv <= 7 -> palette.severity(2).accent
    uv <= 10 -> palette.severity(3).accent
    else -> palette.severity(4).accent
}

private fun getUvInfo(
    uv: Int,
    language: AppLanguage,
    palette: MaterialYouAccentPalette
): Triple<String, Color, String> {
    val color = getUvColor(uv, palette)
    val label = when {
        uv <= 2 -> when (language) {
            AppLanguage.JAPANESE -> "弱い"
            AppLanguage.KOREAN -> "낮음"
            AppLanguage.SIMPLIFIED_CHINESE -> "低"
            AppLanguage.TRADITIONAL_CHINESE -> "低"
            AppLanguage.ENGLISH -> "Low"
            AppLanguage.SPANISH -> "Bajo"
            AppLanguage.FRENCH -> "Faible"
            AppLanguage.PORTUGUESE -> "Baixo"
            AppLanguage.RUSSIAN -> "Низкий"
        }
        uv <= 5 -> when (language) {
            AppLanguage.JAPANESE -> "中程度"
            AppLanguage.KOREAN -> "보통"
            AppLanguage.SIMPLIFIED_CHINESE -> "中等"
            AppLanguage.TRADITIONAL_CHINESE -> "中等"
            AppLanguage.ENGLISH -> "Moderate"
            AppLanguage.SPANISH -> "Moderado"
            AppLanguage.FRENCH -> "Modéré"
            AppLanguage.PORTUGUESE -> "Moderado"
            AppLanguage.RUSSIAN -> "Умеренный"
        }
        uv <= 7 -> when (language) {
            AppLanguage.JAPANESE -> "強い"
            AppLanguage.KOREAN -> "높음"
            AppLanguage.SIMPLIFIED_CHINESE -> "高"
            AppLanguage.TRADITIONAL_CHINESE -> "高"
            AppLanguage.ENGLISH -> "High"
            AppLanguage.SPANISH -> "Alto"
            AppLanguage.FRENCH -> "Élevé"
            AppLanguage.PORTUGUESE -> "Alto"
            AppLanguage.RUSSIAN -> "Высокий"
        }
        uv <= 10 -> when (language) {
            AppLanguage.JAPANESE -> "非常に強い"
            AppLanguage.KOREAN -> "매우 높음"
            AppLanguage.SIMPLIFIED_CHINESE -> "极高"
            AppLanguage.TRADITIONAL_CHINESE -> "極高"
            AppLanguage.ENGLISH -> "Very High"
            AppLanguage.SPANISH -> "Muy alto"
            AppLanguage.FRENCH -> "Très élevé"
            AppLanguage.PORTUGUESE -> "Muito alto"
            AppLanguage.RUSSIAN -> "Очень высокий"
        }
        else -> when (language) {
            AppLanguage.JAPANESE -> "極端に強い"
            AppLanguage.KOREAN -> "위험"
            AppLanguage.SIMPLIFIED_CHINESE -> "危险强"
            AppLanguage.TRADITIONAL_CHINESE -> "危險強"
            AppLanguage.ENGLISH -> "Extreme"
            AppLanguage.SPANISH -> "Extremo"
            AppLanguage.FRENCH -> "Extrême"
            AppLanguage.PORTUGUESE -> "Extremo"
            AppLanguage.RUSSIAN -> "Экстремальный"
        }
    }

    val advice = when {
        uv <= 2 -> when (language) {
            AppLanguage.JAPANESE -> "肌へのダメージはごくわずかです。安心して屋外で活動できます。"
            AppLanguage.KOREAN -> "피부 손상 위험이 매우 낮습니다. 안심하고 야외 활동을 즐기세요."
            AppLanguage.SIMPLIFIED_CHINESE -> "紫外线微弱，对人体伤害极小，可放心进行户外活动。"
            AppLanguage.TRADITIONAL_CHINESE -> "紫外線微弱，對人體傷害極小，可放心進行戶外活動。"
            AppLanguage.ENGLISH -> "Minimal sun risk. Safe to stay outside with standard precautions."
            AppLanguage.SPANISH -> "Riesgo solar mínimo. Es seguro estar al aire libre con precauciones básicas."
            AppLanguage.FRENCH -> "Risque solaire minimal. Vous pouvez profiter de l'extérieur avec des précautions habituelles."
            AppLanguage.PORTUGUESE -> "Risco solar mínimo. É seguro ficar ao ar livre com precauções básicas."
            AppLanguage.RUSSIAN -> "Минимальный риск. Можно спокойно находиться на улице с обычными мерами."
        }
        uv <= 5 -> when (language) {
            AppLanguage.JAPANESE -> "日中は日焼け止めや帽子などを着用して対策を行いましょう。"
            AppLanguage.KOREAN -> "낮 시간대에는 자외선 차단제와 모자 등으로 가볍게 대비하세요."
            AppLanguage.SIMPLIFIED_CHINESE -> "日间出行建议涂抹防晒霜并佩戴遮阳帽。"
            AppLanguage.TRADITIONAL_CHINESE -> "日間出行建議塗抹防曬乳並配戴遮陽帽。"
            AppLanguage.ENGLISH -> "Moderate sun risk. Wear sunglasses, a hat, and sunscreen."
            AppLanguage.SPANISH -> "Riesgo solar moderado. Usa gafas de sol, sombrero y protector solar."
            AppLanguage.FRENCH -> "Risque solaire modéré. Portez lunettes de soleil, chapeau et crème solaire."
            AppLanguage.PORTUGUESE -> "Risco solar moderado. Use óculos de sol, chapéu e protetor solar."
            AppLanguage.RUSSIAN -> "Умеренный риск. Используйте очки, головной убор и солнцезащитный крем."
        }
        uv <= 7 -> when (language) {
            AppLanguage.JAPANESE -> "肌への影響が強くなります。長袖の着用、日傘や日陰の利用を推奨します。"
            AppLanguage.KOREAN -> "자외선이 강합니다. 긴소매 옷, 양산, 그늘 활용을 권장합니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "紫外辐射较强，建议着长袖衣物，多在阴凉处活动并打遮阳伞。"
            AppLanguage.TRADITIONAL_CHINESE -> "紫外輻射較強，建議著長袖衣物，多在陰涼處活動並撐遮陽傘。"
            AppLanguage.ENGLISH -> "High risk of harm. Cover up, wear a hat, and seek shade during midday."
            AppLanguage.SPANISH -> "Alto riesgo de daño solar. Cúbrete, lleva sombrero y busca sombra al mediodía."
            AppLanguage.FRENCH -> "Risque élevé. Couvrez-vous, portez un chapeau et restez à l'ombre à la mi-journée."
            AppLanguage.PORTUGUESE -> "Alto risco solar. Cubra-se, use chapéu e procure sombra ao meio-dia."
            AppLanguage.RUSSIAN -> "Высокий риск. Прикрывайте кожу, носите головной убор и ищите тень днём."
        }
        uv <= 10 -> when (language) {
            AppLanguage.JAPANESE -> "極力直射日光を避け、外出時は徹底したUV対策を行ってください。"
            AppLanguage.KOREAN -> "가급적 직사광선을 피하고 외출 시 철저히 자외선을 차단하세요."
            AppLanguage.SIMPLIFIED_CHINESE -> "紫外辐射极强，尽量避免中午暴晒，外出须做好全方位防护。"
            AppLanguage.TRADITIONAL_CHINESE -> "紫外輻射極強，盡量避免中午暴曬，外出須做好全方位防護。"
            AppLanguage.ENGLISH -> "Very high risk. Minimize sun exposure between 10 AM and 4 PM."
            AppLanguage.SPANISH -> "Riesgo muy alto. Reduce al mínimo la exposición solar entre las 10:00 y las 16:00."
            AppLanguage.FRENCH -> "Risque très élevé. Limitez l'exposition au soleil entre 10 h et 16 h."
            AppLanguage.PORTUGUESE -> "Risco muito alto. Minimize a exposição ao sol entre 10h e 16h."
            AppLanguage.RUSSIAN -> "Очень высокий риск. Ограничьте пребывание на солнце с 10:00 до 16:00."
        }
        else -> when (language) {
            AppLanguage.JAPANESE -> "危険なレベルです。日中の不要不急な屋外活動はできるだけ控えてください。"
            AppLanguage.KOREAN -> "위험 수준입니다. 한낮의 불필요한 야외 활동은 가급적 자제하세요."
            AppLanguage.SIMPLIFIED_CHINESE -> "紫外辐射达到危险等级，请尽量避免在烈日下进行户外活动。"
            AppLanguage.TRADITIONAL_CHINESE -> "紫外輻射達到危險等級，請盡量避免在烈日下進行戶外活動。"
            AppLanguage.ENGLISH -> "Extreme risk. Take all precautions; unprotected skin can burn in minutes."
            AppLanguage.SPANISH -> "Riesgo extremo. Extrema las precauciones; la piel sin protección puede quemarse en minutos."
            AppLanguage.FRENCH -> "Risque extrême. Prenez toutes les précautions ; la peau non protégée peut brûler en quelques minutes."
            AppLanguage.PORTUGUESE -> "Risco extremo. Tome todas as precauções; a pele desprotegida pode queimar em minutos."
            AppLanguage.RUSSIAN -> "Экстремальный риск. Примите все меры защиты: незащищённая кожа обгорает за минуты."
        }
    }

    return Triple(label, color, advice)
}
