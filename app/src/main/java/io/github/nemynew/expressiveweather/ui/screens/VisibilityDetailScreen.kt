package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.components.DetailMetricType
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold

/**
 * 視程（Visibility）詳細画面 - Material 3 Expressive 準拠、多言語対応、枠線フリー
 */
@Composable
fun VisibilityDetailScreen(
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
    val visibilityKm = weather?.visibilityKm ?: 10.0

    val (visLabel, visColor, visDesc) = getVisibilityInfo(visibilityKm, language)

    DetailScaffold(
        title = strings.visibility,
        city = city,
        weather = weather,
        language = language,
        metricType = DetailMetricType.VISIBILITY,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        // ================= 1. ヒーローセクション: 同心円スコープ & 視程距離 =================
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
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = visColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Horizontal Visibility"
                            AppLanguage.SPANISH -> "Visibilidad horizontal"
                            AppLanguage.FRENCH -> "Visibilité horizontale"
                            AppLanguage.PORTUGUESE -> "Visibilidade horizontal"
                            AppLanguage.KOREAN -> "수평 가시거리"
                            AppLanguage.SIMPLIFIED_CHINESE -> "水平能见度"
                            AppLanguage.TRADITIONAL_CHINESE -> "水平能見度"
                            AppLanguage.JAPANESE -> "水平視程 (見通し距離)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // レーダースコープ風の同心円キャンバス
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(150.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val maxR = size.width / 2 - 6.dp.toPx()

                        // 同心円グリッド (2km, 5km, 10km)
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.2f),
                            radius = maxR,
                            center = center,
                            style = Stroke(1.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.25f),
                            radius = maxR * 0.65f,
                            center = center,
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                        )
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.3f),
                            radius = maxR * 0.35f,
                            center = center,
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                        )

                        // 十字ターゲットライン
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(center.x, 0f),
                            end = Offset(center.x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(0f, center.y),
                            end = Offset(size.width, center.y),
                            strokeWidth = 1.dp.toPx()
                        )

                        // 視界領域の塗り
                        val reachRatio = (visibilityKm / 10.0).coerceIn(0.1, 1.0).toFloat()
                        drawCircle(
                            color = visColor.copy(alpha = 0.18f),
                            radius = maxR * reachRatio,
                            center = center
                        )
                        drawCircle(
                            color = visColor,
                            radius = maxR * reachRatio,
                            center = center,
                            style = Stroke(2.dp.toPx())
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (visibilityKm >= 10.0) "10+" else String.format(java.util.Locale.US, "%.1f", visibilityKm),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "km",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // M3 Expressive: 枠線なしトナルサーフェス
                Surface(
                    shape = CircleShape,
                    color = visColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = visLabel,
                        color = visColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = visDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = GoogleSansFlex,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // ================= 2. 国際航空気象（ICAO）視程段階ガイド =================
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
                        AppLanguage.ENGLISH -> "International Visibility Standards (ICAO)"
                        AppLanguage.SPANISH -> "Estándares internacionales de visibilidad (OACI)"
                        AppLanguage.FRENCH -> "Normes internationales de visibilité (OACI)"
                        AppLanguage.PORTUGUESE -> "Padrões internacionais de visibilidade (OACI)"
                        AppLanguage.KOREAN -> "국제 항공 기상(ICAO) 시정 등급"
                        AppLanguage.SIMPLIFIED_CHINESE -> "国际航空气象 (ICAO) 能见度分级"
                        AppLanguage.TRADITIONAL_CHINESE -> "國際航空氣象 (ICAO) 能見度分級"
                        AppLanguage.JAPANESE -> "国際航空気象（ICAO）の視程階級"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )

                Spacer(modifier = Modifier.height(14.dp))

                VisibilityGradeRow(
                    distance = when (language) {
                        AppLanguage.ENGLISH, AppLanguage.SPANISH, AppLanguage.FRENCH, AppLanguage.PORTUGUESE -> "≥ 10 km"
                        AppLanguage.KOREAN -> "10 km 이상"
                        AppLanguage.SIMPLIFIED_CHINESE -> "10 公里以上"
                        AppLanguage.TRADITIONAL_CHINESE -> "10 公里以上"
                        AppLanguage.JAPANESE -> "10 km 以上"
                    },
                    label = when (language) {
                        AppLanguage.ENGLISH -> "Excellent (Clear)"
                        AppLanguage.SPANISH -> "Excelente (Despejado)"
                        AppLanguage.FRENCH -> "Excellente (Dégagée)"
                        AppLanguage.PORTUGUESE -> "Excelente (Límpida)"
                        AppLanguage.KOREAN -> "매우 좋음 (쾌청)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "极佳 (通透清晰)"
                        AppLanguage.TRADITIONAL_CHINESE -> "極佳 (通透清晰)"
                        AppLanguage.JAPANESE -> "極めて良好 (クリア)"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> "Distant landmarks, horizons, and terrain outlines are crystal clear."
                        AppLanguage.SPANISH -> "Los puntos de referencia lejanos, el horizonte y el relieve se ven con total nitidez."
                        AppLanguage.FRENCH -> "Les repères lointains, l'horizon et le relief sont parfaitement nets."
                        AppLanguage.PORTUGUESE -> "Marcos distantes, horizontes e contornos do relevo ficam perfeitamente nítidos."
                        AppLanguage.KOREAN -> "먼 산과 건물의 윤곽까지 또렷하게 보일 정도로 시야가 맑습니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "远景地平线与地标轮廓分明，视界极为开阔清晰。"
                        AppLanguage.TRADITIONAL_CHINESE -> "遠景地平線與地標輪廓分明，視野極為開闊清晰。"
                        AppLanguage.JAPANESE -> "遠方の山々や建造物まで輪郭がくっきりと見渡せます。"
                    },
                    isActive = visibilityKm >= 10.0
                )
                VisibilityGradeRow(
                    distance = when (language) {
                        AppLanguage.ENGLISH, AppLanguage.SPANISH, AppLanguage.FRENCH, AppLanguage.PORTUGUESE -> "4 – 10 km"
                        AppLanguage.KOREAN -> "4 ~ 10 km"
                        AppLanguage.SIMPLIFIED_CHINESE -> "4 ～ 10 公里"
                        AppLanguage.TRADITIONAL_CHINESE -> "4 ～ 10 公里"
                        AppLanguage.JAPANESE -> "4 〜 10 km"
                    },
                    label = when (language) {
                        AppLanguage.ENGLISH -> "Good (Normal)"
                        AppLanguage.SPANISH -> "Buena (Normal)"
                        AppLanguage.FRENCH -> "Bonne (Normale)"
                        AppLanguage.PORTUGUESE -> "Boa (Normal)"
                        AppLanguage.KOREAN -> "좋음 (보통)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "良好 (正常)"
                        AppLanguage.TRADITIONAL_CHINESE -> "良好 (正常)"
                        AppLanguage.JAPANESE -> "良好 (通常)"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> "Standard visibility; road travel and daily outdoor navigation are fully uninhibited."
                        AppLanguage.SPANISH -> "Visibilidad estándar; los desplazamientos por carretera y actividades cotidianas no tienen impedimentos."
                        AppLanguage.FRENCH -> "Visibilité standard ; la conduite et les déplacements quotidiens se font sans aucune gêne."
                        AppLanguage.PORTUGUESE -> "Visibilidade padrão; o tráfego rodoviário e as atividades diárias ao ar livre ocorrem sem restrições."
                        AppLanguage.KOREAN -> "일상적인 이동과 운전에 지장이 없는 표준적인 시야입니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "标准常规视界，对日常出行、通勤与道路驾驶毫无妨碍。"
                        AppLanguage.TRADITIONAL_CHINESE -> "標準常規視野，對日常出行、通勤與道路駕駛毫無妨礙。"
                        AppLanguage.JAPANESE -> "日常的な移動や運転に支障のない標準的な視界です。"
                    },
                    isActive = visibilityKm >= 4.0 && visibilityKm < 10.0
                )
                VisibilityGradeRow(
                    distance = when (language) {
                        AppLanguage.ENGLISH, AppLanguage.SPANISH, AppLanguage.FRENCH, AppLanguage.PORTUGUESE -> "1 – 4 km"
                        AppLanguage.KOREAN -> "1 ~ 4 km"
                        AppLanguage.SIMPLIFIED_CHINESE -> "1 ～ 4 公里"
                        AppLanguage.TRADITIONAL_CHINESE -> "1 ～ 4 公里"
                        AppLanguage.JAPANESE -> "1 〜 4 km"
                    },
                    label = when (language) {
                        AppLanguage.ENGLISH -> "Moderate (Haze / Rain)"
                        AppLanguage.SPANISH -> "Moderada (Bruma / Lluvia)"
                        AppLanguage.FRENCH -> "Modérée (Brume / Pluie)"
                        AppLanguage.PORTUGUESE -> "Moderada (Névoa / Chuva)"
                        AppLanguage.KOREAN -> "다소 흐림 (박무·강수)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "轻度受限 (微雾/降雨)"
                        AppLanguage.TRADITIONAL_CHINESE -> "輕度受限 (微霧/降雨)"
                        AppLanguage.JAPANESE -> "やや不良 (もや・降水)"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> "Distant scenery partially veiled by light mist, airborne particles, or rainfall."
                        AppLanguage.SPANISH -> "El paisaje lejano aparece parcialmente velado por neblina, partículas o lluvia."
                        AppLanguage.FRENCH -> "Les paysages lointains sont légèrement voilés par la brume, les particules ou la pluie."
                        AppLanguage.PORTUGUESE -> "Paisagens distantes parcialmente encobertas por névoa leve, partículas no ar ou chuva."
                        AppLanguage.KOREAN -> "비나 옅은 안개, 미세먼지 등으로 원경이 뿌옇게 보입니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> "受轻雾、微尘或降雨影响，远距离视界稍显灰白朦胧。"
                        AppLanguage.TRADITIONAL_CHINESE -> "受輕霧、微粒或降雨影響，遠距離視野稍顯灰白朦朧。"
                        AppLanguage.JAPANESE -> "雨やもや、微小粒子等により遠景がかすんで見えます。"
                    },
                    isActive = visibilityKm >= 1.0 && visibilityKm < 4.0
                )
                VisibilityGradeRow(
                    distance = when (language) {
                        AppLanguage.ENGLISH, AppLanguage.SPANISH, AppLanguage.FRENCH, AppLanguage.PORTUGUESE -> "< 1 km"
                        AppLanguage.KOREAN -> "1 km 미만"
                        AppLanguage.SIMPLIFIED_CHINESE -> "1 公里以下"
                        AppLanguage.TRADITIONAL_CHINESE -> "1 公里以下"
                        AppLanguage.JAPANESE -> "1 km 未満"
                    },
                    label = when (language) {
                        AppLanguage.ENGLISH -> "Poor (Fog / Heavy Rain)"
                        AppLanguage.SPANISH -> "Reducida (Niebla / Lluvia fuerte)"
                        AppLanguage.FRENCH -> "Faible (Brouillard / Forte pluie)"
                        AppLanguage.PORTUGUESE -> "Baixa (Neblina / Chuva forte)"
                        AppLanguage.KOREAN -> "시야 불량 (안개·짙은 안개)"
                        AppLanguage.SIMPLIFIED_CHINESE -> "视界不良 (大雾预警)"
                        AppLanguage.TRADITIONAL_CHINESE -> "視界不良 (大霧預警)"
                        AppLanguage.JAPANESE -> "視界不良 (霧・濃霧)"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> "Severely restricted forward view. Reduce speed and use low-beam headlights."
                        AppLanguage.SPANISH -> "Visibilidad frontal muy limitada. Reduce la velocidad y enciende las luces de cruce."
                        AppLanguage.FRENCH -> "Visibilité fortement réduite. Ralentissez et allumez vos feux de croisement."
                        AppLanguage.PORTUGUESE -> "Visibilidade à frente muito reduzida. Diminua a velocidade e acenda os faróis baixos."
                        AppLanguage.KOREAN -> "전방 시야가 크게 제한됩니다. 운전 시 전조등을 켜고 충분히 감속하세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> "前方可视距离受限。驾车外出务必减速慢行并开启近光灯或雾灯。"
                        AppLanguage.TRADITIONAL_CHINESE -> "前方可視距離受限。行車外出務必減速慢行並開啟近光燈或霧燈。"
                        AppLanguage.JAPANESE -> "前方見通しが悪化。車の運転時はライト点灯と減速が必要です。"
                    },
                    isActive = visibilityKm < 1.0
                )
            }
        }

        // ================= 3. 移動・活動への影響 =================
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
                        imageVector = Icons.Rounded.Landscape,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            AppLanguage.ENGLISH -> "Travel & Outdoor Activities"
                            AppLanguage.SPANISH -> "Desplazamientos y actividades al aire libre"
                            AppLanguage.FRENCH -> "Déplacements et activités extérieures"
                            AppLanguage.PORTUGUESE -> "Viagens e atividades ao ar livre"
                            AppLanguage.KOREAN -> "이동·야외 활동 영향"
                            AppLanguage.SIMPLIFIED_CHINESE -> "出行与户外活动建议"
                            AppLanguage.TRADITIONAL_CHINESE -> "出行與戶外活動建議"
                            AppLanguage.JAPANESE -> "移動・活動への影響"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                VisAdviceRow(
                    icon = Icons.Rounded.DirectionsCar,
                    title = when (language) {
                        AppLanguage.ENGLISH -> "Driving Safety"
                        AppLanguage.SPANISH -> "Seguridad vial"
                        AppLanguage.FRENCH -> "Sécurité routière"
                        AppLanguage.PORTUGUESE -> "Segurança ao dirigir"
                        AppLanguage.KOREAN -> "차량 운전"
                        AppLanguage.SIMPLIFIED_CHINESE -> "车辆驾驶与交通"
                        AppLanguage.TRADITIONAL_CHINESE -> "車輛駕駛與交通"
                        AppLanguage.JAPANESE -> "自動車の運転"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> if (visibilityKm >= 5.0)
                            "Road visibility is clear; drive with confidence under standard conditions."
                        else
                            "Reduced view: maintain generous following distance and illuminate headlights."
                        AppLanguage.SPANISH -> if (visibilityKm >= 5.0)
                            "La visibilidad en carretera es clara; conduce con normalidad y confianza."
                        else
                            "Visibilidad reducida: mantén una amplia distancia de seguridad y enciende las luces."
                        AppLanguage.FRENCH -> if (visibilityKm >= 5.0)
                            "La visibilité sur la route est bonne ; conduisez en toute sérénité."
                        else
                            "Visibilité réduite : augmentez les distances de sécurité et allumez vos phares."
                        AppLanguage.PORTUGUESE -> if (visibilityKm >= 5.0)
                            "A visibilidade nas vias está limpa; dirija com tranquilidade em condições normais."
                        else
                            "Visibilidade reduzida: mantenha distância segura e acenda os faróis."
                        AppLanguage.KOREAN -> if (visibilityKm >= 5.0)
                            "시야가 양호하여 평소처럼 안전하게 주행할 수 있습니다."
                        else
                            "안전거리를 충분히 확보하고 전조등이나 안개등을 일찍 점등하세요."
                        AppLanguage.SIMPLIFIED_CHINESE -> if (visibilityKm >= 5.0)
                            "视线良好开阔，适合安心驾驶行驶。"
                        else
                            "视程偏低，请加大车距并保持开启近光车灯，谨慎行驶。"
                        AppLanguage.TRADITIONAL_CHINESE -> if (visibilityKm >= 5.0)
                            "視野開闊，適合安心駕駛行駛。"
                        else
                            "視程偏低，請拉大車距並保持開啟近光車燈，謹慎行駛。"
                        AppLanguage.JAPANESE -> if (visibilityKm >= 5.0)
                            "視界は良好です。通常通り安心して走行いただけます。"
                        else
                            "車間距離を十分にとり、早めのヘッドライト点灯を心がけてください。"
                    }
                )
                VisAdviceRow(
                    icon = Icons.Rounded.RemoveRedEye,
                    title = when (language) {
                        AppLanguage.ENGLISH -> "Scenic & Horizon Views"
                        AppLanguage.SPANISH -> "Vistas panorámicas y horizonte"
                        AppLanguage.FRENCH -> "Panoramas et horizon"
                        AppLanguage.PORTUGUESE -> "Vistas panorâmicas e horizonte"
                        AppLanguage.KOREAN -> "전망·풍경 조망"
                        AppLanguage.SIMPLIFIED_CHINESE -> "景观与高台远眺"
                        AppLanguage.TRADITIONAL_CHINESE -> "景觀與高台遠眺"
                        AppLanguage.JAPANESE -> "展望・風景の眺望"
                    },
                    desc = when (language) {
                        AppLanguage.ENGLISH -> if (visibilityKm >= 10.0)
                            "Superb conditions for panoramic viewpoints, towers, and mountain lookouts."
                        else
                            "Distant horizons may be obscured or faded by atmospheric moisture."
                        AppLanguage.SPANISH -> if (visibilityKm >= 10.0)
                            "Condiciones magníficas para miradores, torres y vistas de montaña."
                        else
                            "El horizonte lejano puede verse difuso o empañado por la humedad ambiental."
                        AppLanguage.FRENCH -> if (visibilityKm >= 10.0)
                            "Conditions idéales pour les belvédères, tours panoramiques et points de vue en montagne."
                        else
                            "L'horizon lointain peut paraître légèrement voilé par l'humidité atmosphérique."
                        AppLanguage.PORTUGUESE -> if (visibilityKm >= 10.0)
                            "Ótimas condições para mirantes panorâmicos, torres e vistas de montanhas."
                        else
                            "O horizonte distante pode ficar encoberto ou esbranquiçado pela umidade do ar."
                        AppLanguage.KOREAN -> if (visibilityKm >= 10.0)
                            "전망이 매우 쾌청해 전망대나 높은 곳에서 탁 트인 경치를 감상하기 좋습니다."
                        else
                            "먼 풍경이 대기 중 습기나 연무로 뿌옇게 보일 수 있습니다."
                        AppLanguage.SIMPLIFIED_CHINESE -> if (visibilityKm >= 10.0)
                            "绝佳的观景远眺天气，高台与观景台视野辽阔舒展。"
                        else
                            "远景可能因水汽或微尘而呈现灰白朦胧。"
                        AppLanguage.TRADITIONAL_CHINESE -> if (visibilityKm >= 10.0)
                            "絕佳的觀景遠眺天氣，高處與觀景台視野遼闊舒展。"
                        else
                            "遠景可能因水氣或懸浮微粒而呈現灰白朦朧。"
                        AppLanguage.JAPANESE -> if (visibilityKm >= 10.0)
                            "絶好の見晴らし日和です。展望台や高台からのパノラマビューを楽しめます。"
                        else
                            "遠くの景色が白っぽくかすむ可能性があります。"
                    }
                )
            }
        }
    }
}

@Composable
private fun VisibilityGradeRow(distance: String, label: String, desc: String, isActive: Boolean) {
    // M3 Expressive: 枠線は完全禁止、アクティブ時はトナルサーフェスで表現
    val bg = if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f) else Color.Transparent

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = distance,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun VisAdviceRow(
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

private fun getVisibilityInfo(km: Double, language: AppLanguage): Triple<String, Color, String> {
    return when {
        km >= 10.0 -> Triple(
            when (language) {
                AppLanguage.ENGLISH -> "Crystal Clear"
                AppLanguage.SPANISH -> "Totalmente despejado"
                AppLanguage.FRENCH -> "Parfaitement dégagée"
                AppLanguage.PORTUGUESE -> "Cristalina"
                AppLanguage.KOREAN -> "시야 매우 좋음"
                AppLanguage.SIMPLIFIED_CHINESE -> "视野极佳"
                AppLanguage.TRADITIONAL_CHINESE -> "視野極佳"
                AppLanguage.JAPANESE -> "見通し極めて良好"
            },
            Color(0xFF4CAF50),
            when (language) {
                AppLanguage.ENGLISH -> "Completely unobstructed visibility extending to the farthest horizon."
                AppLanguage.SPANISH -> "Visibilidad completamente despejada hasta el horizonte más lejano."
                AppLanguage.FRENCH -> "Visibilité totalement dégagée s'étendant jusqu'à l'horizon."
                AppLanguage.PORTUGUESE -> "Visibilidade completamente desobstruída até o horizonte mais distante."
                AppLanguage.KOREAN -> "먼 지평선까지 가리는 것 없이 맑고 쾌청한 시야가 펼쳐져 있습니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "极度通透澄澈的视界，视野毫无遮挡直至地平线。"
                AppLanguage.TRADITIONAL_CHINESE -> "極度通透澄澈的視野，毫無遮擋直至地平線。"
                AppLanguage.JAPANESE -> "遠くの地平線まで遮るもののない、澄み切ったクリアな視界が広がっています。"
            }
        )
        km >= 4.0 -> Triple(
            when (language) {
                AppLanguage.ENGLISH -> "Good Visibility"
                AppLanguage.SPANISH -> "Buena visibilidad"
                AppLanguage.FRENCH -> "Bonne visibilité"
                AppLanguage.PORTUGUESE -> "Boa visibilidade"
                AppLanguage.KOREAN -> "좋음"
                AppLanguage.SIMPLIFIED_CHINESE -> "能见度良好"
                AppLanguage.TRADITIONAL_CHINESE -> "能見度良好"
                AppLanguage.JAPANESE -> "良好"
            },
            Color(0xFF03A9F4),
            when (language) {
                AppLanguage.ENGLISH -> "Standard clear visibility. Slight haze may appear at great distances but causes no obstruction."
                AppLanguage.SPANISH -> "Visibilidad clara normal. Puede haber una ligera bruma a gran distancia sin causar obstáculos."
                AppLanguage.FRENCH -> "Visibilité normale et claire. Une légère brume peut apparaître au loin sans gêner."
                AppLanguage.PORTUGUESE -> "Visibilidade limpa padrão. Pode haver leve névoa ao longe, sem causar obstruções."
                AppLanguage.KOREAN -> "양호한 시야입니다. 먼 곳에 약간의 연무가 있을 수 있으나 이동에 지장은 없습니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "常规良好视界。远处虽有轻微泛白，但不影响任何户外活动。"
                AppLanguage.TRADITIONAL_CHINESE -> "常規良好視野。遠處雖有輕微泛白，但不影響任何戶外活動。"
                AppLanguage.JAPANESE -> "一般的な良好な視界です。遠景にわずかな霞が見られますが移動に支障ありません。"
            }
        )
        km >= 1.0 -> Triple(
            when (language) {
                AppLanguage.ENGLISH -> "Moderate Haze"
                AppLanguage.SPANISH -> "Bruma moderada"
                AppLanguage.FRENCH -> "Brume modérée"
                AppLanguage.PORTUGUESE -> "Névoa moderada"
                AppLanguage.KOREAN -> "다소 흐림"
                AppLanguage.SIMPLIFIED_CHINESE -> "轻度朦胧"
                AppLanguage.TRADITIONAL_CHINESE -> "輕度朦朧"
                AppLanguage.JAPANESE -> "ややかすむ"
            },
            Color(0xFFFF9800),
            when (language) {
                AppLanguage.ENGLISH -> "Visibility somewhat decreased due to mist, humidity, or precipitation."
                AppLanguage.SPANISH -> "Visibilidad algo reducida debido a neblina, humedad o precipitaciones."
                AppLanguage.FRENCH -> "Visibilité légèrement réduite en raison de la brume, de l'humidité ou des précipitations."
                AppLanguage.PORTUGUESE -> "Visibilidade um pouco reduzida devido a névoa, umidade ou precipitação."
                AppLanguage.KOREAN -> "박무, 미세먼지 또는 강수의 영향으로 가시거리가 다소 떨어졌습니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "受轻雾、微粒或降雨影响，中远距离视程有所下降。"
                AppLanguage.TRADITIONAL_CHINESE -> "受輕霧、懸浮微粒或降雨影響，中遠距離視程有所下降。"
                AppLanguage.JAPANESE -> "もやや微小粒子、または降雨により見通しがやや低下しています。"
            }
        )
        else -> Triple(
            when (language) {
                AppLanguage.ENGLISH -> "Poor Visibility (Fog Alert)"
                AppLanguage.SPANISH -> "Mala visibilidad (Aviso de niebla)"
                AppLanguage.FRENCH -> "Faible visibilité (Alerte brouillard)"
                AppLanguage.PORTUGUESE -> "Baixa visibilidade (Alerta de neblina)"
                AppLanguage.KOREAN -> "시야 불량 (안개 주의)"
                AppLanguage.SIMPLIFIED_CHINESE -> "视界不良 (大雾注意)"
                AppLanguage.TRADITIONAL_CHINESE -> "視界不良 (大霧注意)"
                AppLanguage.JAPANESE -> "視界不良 (霧注意)"
            },
            Color(0xFFE53935),
            when (language) {
                AppLanguage.ENGLISH -> "Heavy fog or intense rain limits forward view to under 1 km. Exercise extreme caution."
                AppLanguage.SPANISH -> "La niebla densa o la lluvia intensa limitan la visibilidad a menos de 1 km. Extrema la precaución."
                AppLanguage.FRENCH -> "Un brouillard épais ou une forte pluie limite la visibilité à moins d'1 km. Redoublez de prudence."
                AppLanguage.PORTUGUESE -> "Neblina densa ou chuva forte limita a visão à frente a menos de 1 km. Redobre a atenção."
                AppLanguage.KOREAN -> "짙은 안개나 강한 비로 1km 앞도 보기 어려운 상태입니다. 통행에 각별히 주의하세요."
                AppLanguage.SIMPLIFIED_CHINESE -> "浓雾或暴雨导致1公里外难以辨析，出行需极度谨慎。"
                AppLanguage.TRADITIONAL_CHINESE -> "濃霧或暴雨導致1公里外難以辨認，出行需極度謹慎。"
                AppLanguage.JAPANESE -> "霧や大雨等により1km先が見えない状況です。通行には細心の注意が必要です。"
            }
        )
    }
}

