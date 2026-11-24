package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.nemynew.expressiveweather.data.*
import io.github.nemynew.expressiveweather.ui.util.rememberRelativeTime

/**
 * メイン画面で表示している情報のデータソース定義
 */
data class WeatherDataSourceEntry(
    val category: String,
    val source: String,
    val detail: String,
    val icon: ImageVector,
    val isDirectJma: Boolean = false
)

/**
 * メイン画面最下部のステータスピル
 * メイン画面に表示しているすべての情報のソースを漏れなく明記
 */
@Composable
fun WeatherFooterStatusSection(
    weather: WeatherData,
    isFromCache: Boolean,
    isStale: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    condition: io.github.nemynew.expressiveweather.data.WeatherCondition? = null,
    isDark: Boolean = false,
    isFrostedGlassEnabled: Boolean = true,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val relativeTime = rememberRelativeTime(weather.fetchedAtMillis, strings)

    var isSourcesExpanded by rememberSaveable { mutableStateOf(false) }

    val updatedText = if (isFromCache) {
        strings.cachedDataRelative(weather.lastUpdatedTime, relativeTime)
    } else {
        strings.lastUpdatedRelative(weather.lastUpdatedTime, relativeTime)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rotation"
    )

    val isSnowing = condition == io.github.nemynew.expressiveweather.data.WeatherCondition.SNOW
    val colorScheme = MaterialTheme.colorScheme
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)

    // 日本国内かどうかの厳密判定
    val isJapan = weather.city.country.contains("日本") ||
            weather.city.country.contains("일본") ||
            weather.city.country.equals("Japan", ignoreCase = true) ||
            weather.city.country.equals("Japón", ignoreCase = true) ||
            weather.city.country.equals("Japon", ignoreCase = true) ||
            weather.city.country.equals("Japão", ignoreCase = true) ||
            weather.jmaOverviewText != null ||
            (weather.city.latitude in 20.0..46.0 && weather.city.longitude in 122.0..154.0)

    val isUs = !isJapan && NwsAlertRepository.isUnitedStates(weather.city)

    val dataSources = remember(isJapan, isUs, currentLang) {
        getDataSourcesList(isJapan, isUs, currentLang)
    }

    val cardShape = RoundedCornerShape(32.dp)
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .frostedGlassEffect(hazeState, isFrostedGlassEnabled, shape = cardShape)
            .snowTileAccumulation(
                isSnowing = isSnowing,
                isDark = isDark,
                shape = SnowTileShape.ROUNDED_RECT,
                cornerRadiusDp = 32.dp,
                seed = 103
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ================= 1. 更新時刻 =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRefresh
                    )
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isFromCache) Icons.Rounded.CloudQueue else Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = if (isStale) colorScheme.tertiary else colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = updatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = when (currentLang) {
                        AppLanguage.JAPANESE -> "再読み込み"
                        AppLanguage.KOREAN -> "새로고침"
                        AppLanguage.SIMPLIFIED_CHINESE -> "刷新"
                        AppLanguage.TRADITIONAL_CHINESE -> "重新整理"
                        AppLanguage.SPANISH -> "Actualizar"
                        AppLanguage.FRENCH -> "Actualiser"
                        AppLanguage.PORTUGUESE -> "Atualizar"
                        AppLanguage.ENGLISH -> "Refresh"
                    },
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier
                        .size(15.dp)
                        .then(if (isRefreshing) Modifier.rotate(rotation) else Modifier)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // ================= 2. データソースヘッダー（折りたたみトグル付き） =================
            val arrowRotation by animateFloatAsState(
                targetValue = if (isSourcesExpanded) 180f else 0f,
                label = "SourceAccordionArrow"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isSourcesExpanded = !isSourcesExpanded }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (currentLang) {
                            AppLanguage.JAPANESE -> "表示情報の全データ提供元"
                            AppLanguage.KOREAN -> "표시된 정보의 전체 데이터 출처"
                            AppLanguage.SIMPLIFIED_CHINESE -> "所有显示数据提供方"
                            AppLanguage.TRADITIONAL_CHINESE -> "所有顯示資料提供方"
                            AppLanguage.SPANISH -> "Todas las fuentes de datos"
                            AppLanguage.FRENCH -> "Toutes les sources de données"
                            AppLanguage.PORTUGUESE -> "Todas as fontes de dados"
                            AppLanguage.ENGLISH -> "All Data Sources & Attribution"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    )
                }

                // 開閉アイコン
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = when (currentLang) {
                        AppLanguage.JAPANESE -> if (isSourcesExpanded) "折りたたむ" else "展開する"
                        AppLanguage.KOREAN -> if (isSourcesExpanded) "접기" else "펼치기"
                        AppLanguage.SIMPLIFIED_CHINESE -> if (isSourcesExpanded) "收起" else "展开"
                        AppLanguage.TRADITIONAL_CHINESE -> if (isSourcesExpanded) "收合" else "展開"
                        AppLanguage.SPANISH -> if (isSourcesExpanded) "Contraer" else "Expandir"
                        AppLanguage.FRENCH -> if (isSourcesExpanded) "Réduire" else "Développer"
                        AppLanguage.PORTUGUESE -> if (isSourcesExpanded) "Recolher" else "Expandir"
                        AppLanguage.ENGLISH -> if (isSourcesExpanded) "Collapse" else "Expand"
                    },
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(arrowRotation)
                )
            }

            // ================= 3. 全ソースの完全一覧 (折りたたみ対応・デフォルト非表示) =================
            AnimatedVisibility(
                visible = isSourcesExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceContainer.copy(alpha = 0.35f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    dataSources.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item.isDirectJma) colorScheme.primary.copy(alpha = 0.12f)
                                            else colorScheme.secondary.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (item.isDirectJma) colorScheme.primary else colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onSurface.copy(alpha = 0.85f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    item.isDirectJma -> colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    item.source.contains("Open-Meteo") -> colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    else -> colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                }
                            ) {
                                Text(
                                    text = item.source,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            item.isDirectJma -> colorScheme.onPrimaryContainer
                                            item.source.contains("Open-Meteo") -> colorScheme.onSecondaryContainer
                                            else -> colorScheme.onTertiaryContainer
                                        }
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

/**
 * 日本国内および海外都市の全情報ソース定義（絶対に漏れのない全項目）
 */
private fun getDataSourcesList(isJapan: Boolean, isUs: Boolean, language: AppLanguage): List<WeatherDataSourceEntry> {
    val gdacsSourceLabel = when (language) {
        AppLanguage.JAPANESE -> "国連/欧州委員会 GDACS"
        AppLanguage.KOREAN -> "UN/유럽위원회 GDACS"
        AppLanguage.SIMPLIFIED_CHINESE -> "联合国/欧盟委员会 GDACS"
        AppLanguage.TRADITIONAL_CHINESE -> "聯合國/歐盟委員會 GDACS"
        AppLanguage.SPANISH -> "ONU / CE GDACS"
        AppLanguage.FRENCH -> "ONU / CE GDACS"
        AppLanguage.PORTUGUESE -> "ONU / CE GDACS"
        AppLanguage.ENGLISH -> "UN / EC GDACS"
    }
    return if (isJapan) {
        listOf(
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "気象実況 (気温/湿度/気圧/風/雨)"
                    AppLanguage.KOREAN -> "기상 실황 (기온/습도/기압/바람/강수)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "气象实况 (气温/湿度/气压/风速/雨量)"
                    AppLanguage.TRADITIONAL_CHINESE -> "氣象實況 (氣溫/濕度/氣壓/風速/雨量)"
                    AppLanguage.SPANISH -> "Observaciones (Temp./Hum./Viento/Lluvia)"
                    AppLanguage.FRENCH -> "Observations (Temp./Hum./Vent/Pluie)"
                    AppLanguage.PORTUGUESE -> "Observações (Temp./Umid./Vento/Chuva)"
                    AppLanguage.ENGLISH -> "Observations (Temp/Humidity/Wind/Rain)"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) アメダス実測"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA) AMeDAS 실측"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) AMeDAS实测"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) AMeDAS實測"
                    AppLanguage.SPANISH -> "JMA AMeDAS (Directo)"
                    AppLanguage.FRENCH -> "JMA AMeDAS (Direct)"
                    AppLanguage.PORTUGUESE -> "JMA AMeDAS (Direto)"
                    AppLanguage.ENGLISH -> "JMA AMeDAS (Direct)"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "全国約1,300地点のアメダス観測所から最寄り実測値（気温・湿度・気圧・風向風速・降水量・最高最低気温）を直接リアルタイム取得しています。"
                    AppLanguage.KOREAN -> "일본 전역 약 1,300개 AMeDAS 관측소에서 가장 가까운 실측값(기온, 습도, 기압, 풍향·풍속, 강수량, 최고·최저기온)을 실시간으로 직접 수신합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接实时获取自全国约1,300个AMeDAS气象观测站实测数据（气温、湿度、气压、风向风速、降水量、最高最低温度）。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接即時擷取自全國約1,300個AMeDAS氣象觀測站實測資料（氣溫、濕度、氣壓、風向風速、降水量、最高最低溫度）。"
                    AppLanguage.SPANISH -> "Mediciones directas en tiempo real (temperatura, humedad, presión, viento, lluvia, máximas/mínimas) de ~1.300 estaciones AMeDAS en todo Japón."
                    AppLanguage.FRENCH -> "Mesures directes en temps réel (température, humidité, pression, vent, pluie, min./max.) issues d'environ 1 300 stations AMeDAS au Japon."
                    AppLanguage.PORTUGUESE -> "Medições diretas em tempo real (temperatura, umidade, pressão, vento, chuva, máximas/mínimas) de ~1.300 estações AMeDAS no Japão."
                    AppLanguage.ENGLISH -> "Real-time direct measurements (temperature, humidity, pressure, wind, rain, highs/lows) from ~1,300 AMeDAS stations nationwide."
                },
                icon = Icons.Rounded.Thermostat,
                isDirectJma = true
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "天気予報 (24時間・週間7日)"
                    AppLanguage.KOREAN -> "일기예보 (24시간 · 주간 7일)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "天气预报 (24小时・7天预报)"
                    AppLanguage.TRADITIONAL_CHINESE -> "天氣預報 (24小時・7天預報)"
                    AppLanguage.SPANISH -> "Pronósticos (Por hora y 7 días)"
                    AppLanguage.FRENCH -> "Prévisions (Horaires et 7 jours)"
                    AppLanguage.PORTUGUESE -> "Previsões (Horária e 7 dias)"
                    AppLanguage.ENGLISH -> "Forecasts (Hourly & 7-Day)"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) 府県・週間予報"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA) 지역·주간 예보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) 区域・周预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) 區域・週預報"
                    AppLanguage.SPANISH -> "Pronósticos oficiales JMA"
                    AppLanguage.FRENCH -> "Prévisions officielles JMA"
                    AppLanguage.PORTUGUESE -> "Previsões oficiais JMA"
                    AppLanguage.ENGLISH -> "JMA Official Forecasts"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "各管区気象台発表の府県天気予報、6時間区切り降水確率、週間天気予報コード及び予報信頼度を直接取得しています。"
                    AppLanguage.KOREAN -> "각 지방기상대가 발표하는 지역 일기예보, 6시간 단위 강수 확률, 주간 예보 코드 및 신뢰도 등급을 직접 수신합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接获取各管区气象台发布的区域天气预报、6小时降水概率、一周预报代码与可靠度评级。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接擷取各管區氣象台發布的區域天氣預報、6小時降水機率、一週預報代碼與可靠度評級。"
                    AppLanguage.SPANISH -> "Pronósticos regionales por prefectura, probabilidades de precipitación cada 6 horas y códigos de pronóstico a 7 días con índices de fiabilidad."
                    AppLanguage.FRENCH -> "Prévisions régionales par préfecture, probabilités de précipitations sur 6 heures et tendances à 7 jours avec indices de fiabilité."
                    AppLanguage.PORTUGUESE -> "Previsões regionais por província, probabilidades de chuva a cada 6 horas e previsão para 7 dias com índices de confiabilidade."
                    AppLanguage.ENGLISH -> "Direct regional prefectural forecasts, 6-hour precipitation probabilities, and 7-day outlook codes with reliability ratings."
                },
                icon = Icons.Rounded.CalendarMonth,
                isDirectJma = true
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "警報・注意報 & 天気概況解説"
                    AppLanguage.KOREAN -> "기상 특보 및 날씨 개황 해설"
                    AppLanguage.SIMPLIFIED_CHINESE -> "预警信息 & 天气概况解说"
                    AppLanguage.TRADITIONAL_CHINESE -> "警報資訊 & 天氣概況解說"
                    AppLanguage.SPANISH -> "Alertas y sinopsis meteorológica"
                    AppLanguage.FRENCH -> "Alertes et bulletin météo"
                    AppLanguage.PORTUGUESE -> "Alertas e sinopse meteorológica"
                    AppLanguage.ENGLISH -> "Warnings & Weather Synopsis"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) 防災気象情報"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA) 방재 기상 정보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) 防灾信息"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) 防災資訊"
                    AppLanguage.SPANISH -> "Alertas de desastres JMA"
                    AppLanguage.FRENCH -> "Alertes catastrophes JMA"
                    AppLanguage.PORTUGUESE -> "Alertas de desastres JMA"
                    AppLanguage.ENGLISH -> "JMA Disaster Alerts"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "気象台予報官執筆の天気概況解説文、市区町村別特別警報・警報・注意報の発表状況を直接取得しています。"
                    AppLanguage.KOREAN -> "기상대 예보관이 작성한 날씨 개황 해설문과 시구정촌별 특별경보·경보·주의보 발령 현황을 직접 수신합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接获取预报员撰写的天气概况解说文，以及各市区町村特别警报・警报・注意报。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接擷取預報員撰寫的天氣概況解說文，以及各市區町村特別警報・警報・注意報。"
                    AppLanguage.SPANISH -> "Resúmenes sinópticos de meteorólogos y alertas de emergencia, advertencias y avisos a nivel municipal."
                    AppLanguage.FRENCH -> "Bulletins synoptiques des prévisionnistes et alertes d'urgence, vigilances et avis au niveau municipal."
                    AppLanguage.PORTUGUESE -> "Resumos sinóticos de meteorologistas e alertas de emergência, avisos e advertências em nível municipal."
                    AppLanguage.ENGLISH -> "Direct forecaster synoptic overviews and municipality-level emergency, warning, and advisory alerts."
                },
                icon = Icons.Rounded.WarningAmber,
                isDirectJma = true
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "広域大規模自然災害警報"
                    AppLanguage.KOREAN -> "광역 대규모 자연재난 경보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "广域自然灾害应急预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "廣域自然災害應急預警"
                    AppLanguage.SPANISH -> "Alertas globales de desastres"
                    AppLanguage.FRENCH -> "Alertes mondiales de catastrophes"
                    AppLanguage.PORTUGUESE -> "Alertas globais de desastres"
                    AppLanguage.ENGLISH -> "Global Disaster Alerts"
                },
                source = gdacsSourceLabel,
                detail = when (language) {
                    AppLanguage.JAPANESE -> "巨大サイクロン・大規模地震・広域洪水等の発生時に、国連および欧州委員会(GDACS)の全球自然災害警報を直接監視・連携表示します。"
                    AppLanguage.KOREAN -> "초대형 태풍, 대규모 지진, 광역 홍수 등이 발생할 때 유엔 및 유럽위원회 (GDACS)의 글로벌 자연재난 경보를 연동해 표시합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "在特大台风、特大地震、严重洪水发生时，联动监控联合国与欧盟GDACS全球自然灾害应急预警。"
                    AppLanguage.TRADITIONAL_CHINESE -> "在特大颱風、特大地震、嚴重洪水發生時，聯動監控聯合國與歐盟GDACS全球自然災害應急預警。"
                    AppLanguage.SPANISH -> "Integración directa con el Sistema Global de Alerta y Coordinación de Desastres de la ONU/CE para ciclones, terremotos e inundaciones graves."
                    AppLanguage.FRENCH -> "Intégration directe avec le système mondial d'alerte aux catastrophes ONU/CE pour les cyclones, séismes et inondations majeurs."
                    AppLanguage.PORTUGUESE -> "Integração direta com o Sistema Global de Alerta de Desastres da ONU/CE para ciclones, terremotos e enchentes graves."
                    AppLanguage.ENGLISH -> "Direct integration with UN/EC Global Disaster Alert and Coordination System for major cyclones, earthquakes, and floods."
                },
                icon = Icons.Rounded.Public,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨雲レーダー・キキクル・衛星"
                    AppLanguage.KOREAN -> "강수 레이더 · 키키쿠루 · 위성"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达・危险度分布・气象卫星"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨雷達・危險度分布・氣象衛星"
                    AppLanguage.SPANISH -> "Radar, Kikikuru y satélite"
                    AppLanguage.FRENCH -> "Radar, Kikikuru et satellite"
                    AppLanguage.PORTUGUESE -> "Radar, Kikikuru e satélite"
                    AppLanguage.ENGLISH -> "Radar, Kikikuru & Satellite"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) 高解像度ナウキャスト"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA) 고해상도 나우캐스트"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) 高清Nowcast"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) 高畫質Nowcast"
                    AppLanguage.SPANISH -> "Nowcast alta resolución JMA"
                    AppLanguage.FRENCH -> "Nowcast haute résolution JMA"
                    AppLanguage.PORTUGUESE -> "Nowcast alta resolução JMA"
                    AppLanguage.ENGLISH -> "JMA High-Res Nowcast"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "高解像度降水ナウキャスト(HR-PNS 250mメッシュ雨雲)、土砂浸水洪水キキクル危険度分布、気象衛星ひまわりタイルを公式配信サーバーから直接取得しています。"
                    AppLanguage.KOREAN -> "고해상도 강수 나우캐스트 (250m 격자 비구름), 산사태·침수·홍수 키키쿠루 위험도 분포, 히마와리 기상위성 타일을 공식 서버에서 직접 수신합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接从官方服务获取250m网格高清降水Nowcast雷达、土砂浸水洪水Kikikuru危险度分布及向日葵卫星瓦片。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接從官方服務擷取250m網格高畫質降水Nowcast雷達、土砂浸水洪水Kikikuru危險度分布及向日葵衛星圖磚。"
                    AppLanguage.SPANISH -> "Nowcast de precipitación de alta resolución de JMA (malla de 250 m), mapas de riesgo Kikikuru e imágenes del satélite Himawari."
                    AppLanguage.FRENCH -> "Nowcast de précipitations haute résolution de JMA (maille 250 m), cartes de risques Kikikuru et imagerie satellite Himawari."
                    AppLanguage.PORTUGUESE -> "Nowcast de precipitação de alta resolução da JMA (malha de 250 m), mapas de risco Kikikuru e imagens do satélite Himawari."
                    AppLanguage.ENGLISH -> "Direct JMA high-resolution precipitation nowcast (250m mesh), Kikikuru hazard distributions, and Himawari satellite imagery."
                },
                icon = Icons.Rounded.Radar,
                isDirectJma = true
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "大気質 (AQI / PM2.5 / PM10)"
                    AppLanguage.KOREAN -> "대기질 (AQI / PM2.5 / PM10)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "空气质量 (AQI / PM2.5 / PM10)"
                    AppLanguage.TRADITIONAL_CHINESE -> "空氣品質 (AQI / PM2.5 / PM10)"
                    AppLanguage.SPANISH -> "Calidad del aire (AQI / PM2.5 / PM10)"
                    AppLanguage.FRENCH -> "Qualité de l'air (AQI / PM2.5 / PM10)"
                    AppLanguage.PORTUGUESE -> "Qualidade do ar (AQI / PM2.5 / PM10)"
                    AppLanguage.ENGLISH -> "Air Quality (AQI / PM2.5 / PM10)"
                },
                source = "Open-Meteo Air Quality",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "気象庁APIにはリアルタイム全国大気質APIがないため、Open-Meteoの大気化学モデルAPIからUS AQI・PM2.5・PM10・NO2・O3を補完取得しています。"
                    AppLanguage.KOREAN -> "Open-Meteo 전 지구 대기화학 모델 API를 통해 US AQI, 초미세먼지 (PM2.5), 미세먼지 (PM10), NO2, 오존 수치를 보완 제공합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "由于气象厅未提供实时全国空气质量接口，通过Open-Meteo全球大气化学模型API补充获取US AQI、PM2.5、PM10与各类气体指标。"
                    AppLanguage.TRADITIONAL_CHINESE -> "由於氣象廳未提供即時全國空氣品質介面，透過Open-Meteo全球大氣化學模型API補充擷取US AQI、PM2.5、PM10與各類氣體指標。"
                    AppLanguage.SPANISH -> "Complementado mediante los modelos globales de química atmosférica de Open-Meteo para US AQI, PM2.5, PM10, NO2 y ozono."
                    AppLanguage.FRENCH -> "Complété via les modèles mondiaux de chimie atmosphérique d'Open-Meteo pour US AQI, PM2.5, PM10, NO2 et ozone."
                    AppLanguage.PORTUGUESE -> "Complementado pelos modelos globais de química atmosférica da Open-Meteo para US AQI, PM2.5, PM10, NO2 e ozônio."
                    AppLanguage.ENGLISH -> "Supplemented via Open-Meteo global atmospheric chemistry models for US AQI, PM2.5, PM10, NO2, and Ozone."
                },
                icon = Icons.Rounded.Air,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨の降り始め・止み予測"
                    AppLanguage.KOREAN -> "강수 시작 · 종료 예측"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水起止分析预测"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨起止分析預測"
                    AppLanguage.SPANISH -> "Predicción inmediata de lluvia"
                    AppLanguage.FRENCH -> "Prévision immédiate de pluie"
                    AppLanguage.PORTUGUESE -> "Previsão imediata de chuva"
                    AppLanguage.ENGLISH -> "Precipitation Nowcast Prediction"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) ＆ Open-Meteo"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA) & Open-Meteo"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 & Open-Meteo"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 & Open-Meteo"
                    AppLanguage.SPANISH -> "JMA y Open-Meteo (Híbrido)"
                    AppLanguage.FRENCH -> "JMA et Open-Meteo (Hybride)"
                    AppLanguage.PORTUGUESE -> "JMA e Open-Meteo (Híbrido)"
                    AppLanguage.ENGLISH -> "JMA & Open-Meteo (Hybrid)"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "気象台公式概況の降雨判定と、Open-Meteoの15分刻み超短期雨量予測を統合し、アプリ内で雨の降り始め・止み時刻をハイブリッド解析しています。"
                    AppLanguage.KOREAN -> "기상대 공식 개황의 강수 판정과 Open-Meteo 15분 단위 초단기 강수 예보를 통합해 비가 시작되고 그치는 시각을 분석합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "综合气象厅天气概况降雨判定与Open-Meteo 15分钟高分辨率雨量预报，在应用内混合计算降雨起止时刻。"
                    AppLanguage.TRADITIONAL_CHINESE -> "綜合氣象廳天氣概況降雨判定與Open-Meteo 15分鐘高解析度雨量預報，在應用程式內混合計算降雨起止時刻。"
                    AppLanguage.SPANISH -> "Análisis híbrido que combina boletines sinópticos de JMA con pasos de precipitación cada 15 minutos de Open-Meteo."
                    AppLanguage.FRENCH -> "Analyse hybride combinant les bulletins synoptiques de JMA et les prévisions de précipitations par pas de 15 minutes d'Open-Meteo."
                    AppLanguage.PORTUGUESE -> "Análise híbrida combinando boletins sinóticos da JMA e previsões de precipitação a cada 15 minutos da Open-Meteo."
                    AppLanguage.ENGLISH -> "Hybrid analysis combining JMA synoptic weather bulletins and Open-Meteo 15-minute precipitation steps."
                },
                icon = Icons.Rounded.Umbrella,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "日の出・日の入り・太陽軌道"
                    AppLanguage.KOREAN -> "일출 · 일몰 및 태양 궤적"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日出日落・太阳轨迹"
                    AppLanguage.TRADITIONAL_CHINESE -> "日出日落・太陽軌跡"
                    AppLanguage.SPANISH -> "Amanecer / Atardecer y arco solar"
                    AppLanguage.FRENCH -> "Lever / Coucher et course du soleil"
                    AppLanguage.PORTUGUESE -> "Nascer / Pôr do sol e arco solar"
                    AppLanguage.ENGLISH -> "Sunrise / Sunset & Solar Transit"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "NOAA天文学算式 (端末独自計算)"
                    AppLanguage.KOREAN -> "NOAA 천문 계산식 (기기 내 연산)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "NOAA天文学公式 (设备端计算)"
                    AppLanguage.TRADITIONAL_CHINESE -> "NOAA天文學公式 (裝置端計算)"
                    AppLanguage.SPANISH -> "Algoritmo NOAA (En el dispositivo)"
                    AppLanguage.FRENCH -> "Algorithme NOAA (Local)"
                    AppLanguage.PORTUGUESE -> "Algoritmo NOAA (No dispositivo)"
                    AppLanguage.ENGLISH -> "NOAA Algorithm (On-device)"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米国海洋大気庁(NOAA)の天文学数式アルゴリズムに基づき、端末CPUで緯度・経度から秒単位の日の出・日の入り・日長・南中進捗を完全オフライン独自計算しています。"
                    AppLanguage.KOREAN -> "미국 해양대기청 (NOAA) 천문 계산 알고리즘을 기반으로 기기에서 위도·경도를 통해 일출, 일몰, 낮 길이, 남중 진행도를 오프라인으로 정밀 계산합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "基于美国国家海洋和大气管理局(NOAA)天文学公式，由设备端CPU依据经纬度全离线精确计算日出、日落、昼长与中天轨迹。"
                    AppLanguage.TRADITIONAL_CHINESE -> "基於美國國家海洋暨大氣總署(NOAA)天文學公式，由裝置端CPU依據經緯度全離線精確計算日出、日落、晝長與中天軌跡。"
                    AppLanguage.SPANISH -> "Calculado íntegramente en el dispositivo mediante fórmulas astronómicas solares de la NOAA para amanecer, atardecer y arco solar precisos."
                    AppLanguage.FRENCH -> "Calculé entièrement sur l'appareil via les formules astronomiques solaires de la NOAA pour le lever, le coucher et la course du soleil."
                    AppLanguage.PORTUGUESE -> "Calculado inteiramente no dispositivo pelas fórmulas astronômicas solares da NOAA para nascer, pôr do sol e arco solar precisos."
                    AppLanguage.ENGLISH -> "Calculated entirely on-device via NOAA Solar Calculation astronomical formulas for precision sunrise, sunset, and solar arc."
                },
                icon = Icons.Rounded.WbSunny,
                isDirectJma = false
            )
        )
    } else if (isUs) {
        listOf(
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "気象実況 & 天気予報 (24h/週間)"
                    AppLanguage.KOREAN -> "기상 실황 및 일기예보 (24시간/주간)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "气象实况与天气预报 (24h/周预报)"
                    AppLanguage.TRADITIONAL_CHINESE -> "氣象實況與天氣預報 (24h/週預報)"
                    AppLanguage.SPANISH -> "Observaciones y pronósticos"
                    AppLanguage.FRENCH -> "Observations et prévisions"
                    AppLanguage.PORTUGUESE -> "Observações e previsões"
                    AppLanguage.ENGLISH -> "Weather Observations & Forecasts"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteo 全球数値モデル"
                    AppLanguage.KOREAN -> "Open-Meteo 전 지구 수치 모델"
                    AppLanguage.SIMPLIFIED_CHINESE -> "Open-Meteo 全球数值预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "Open-Meteo 全球數值預報"
                    AppLanguage.SPANISH -> "Open-Meteo NWP Global"
                    AppLanguage.FRENCH -> "Open-Meteo NWP Mondial"
                    AppLanguage.PORTUGUESE -> "Open-Meteo NWP Global"
                    AppLanguage.ENGLISH -> "Open-Meteo Global NWP"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米NOAA GFS、HRRR、欧州ECMWF等の最高峰全球・局地モデルをシームレスに統合した高精度気象データです。"
                    AppLanguage.KOREAN -> "미국 NOAA GFS, HRRR 및 유럽 ECMWF 등 고해상도 전 지구·국지 수치 예보 모델을 매끄럽게 통합한 정밀 기상 데이터입니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "融合美国NOAA GFS、HRRR及欧洲ECMWF等高分辨率数值预报模型的实时高精度气象数据。"
                    AppLanguage.TRADITIONAL_CHINESE -> "融合美國NOAA GFS、HRRR及歐洲ECMWF等高解析度數值預報模型的即時高精度氣象資料。"
                    AppLanguage.SPANISH -> "Datos meteorológicos de alta resolución que combinan los modelos NOAA GFS, HRRR y ECMWF."
                    AppLanguage.FRENCH -> "Données météo haute résolution combinant les modèles NOAA GFS, HRRR et ECMWF."
                    AppLanguage.PORTUGUESE -> "Dados meteorológicos de alta resolução combinando os modelos NOAA GFS, HRRR e ECMWF."
                    AppLanguage.ENGLISH -> "High-resolution seamless weather blending NOAA GFS, HRRR, and ECMWF models."
                },
                icon = Icons.Rounded.Thermostat,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "気象警報・注意報"
                    AppLanguage.KOREAN -> "기상 경보 · 주의보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "气象预警・提示信息"
                    AppLanguage.TRADITIONAL_CHINESE -> "氣象警報・提示資訊"
                    AppLanguage.SPANISH -> "Alertas y avisos meteorológicos"
                    AppLanguage.FRENCH -> "Alertes et vigilances météo"
                    AppLanguage.PORTUGUESE -> "Alertas e avisos meteorológicos"
                    AppLanguage.ENGLISH -> "Weather Warnings & Watches"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "米国気象局 (NOAA NWS) 公式"
                    AppLanguage.KOREAN -> "미국 국립기상청 (NOAA NWS) 공식"
                    AppLanguage.SIMPLIFIED_CHINESE -> "美国国家气象局 (NOAA NWS) 官方"
                    AppLanguage.TRADITIONAL_CHINESE -> "美國國家氣象局 (NOAA NWS) 官方"
                    AppLanguage.SPANISH -> "NOAA NWS Oficial"
                    AppLanguage.FRENCH -> "NOAA NWS Officiel"
                    AppLanguage.PORTUGUESE -> "NOAA NWS Oficial"
                    AppLanguage.ENGLISH -> "NOAA NWS Official"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米国国立気象局(NWS)公式API(api.weather.gov)から、竜巻警報・鉄砲水警報・暴風雪警報・熱波注意報等のアクティブ警報を直接リアルタイム取得しています。"
                    AppLanguage.KOREAN -> "미국 국립기상청 (NWS) 공식 API (api.weather.gov)에서 토네이도 경보, 돌발 홍수 경보, 눈폭풍 경보, 폭염 주의보 등 발령 중인 특보를 실시간으로 직접 수신합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接实时获取自美国国家气象局(NWS)官方接口的活动预警（龙卷风警报、山洪警报、暴风雪警报、高温预警等）。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接即時擷取自美國國家氣象局(NWS)官方介面的活動預警（龍捲風警報、暴洪警報、暴風雪警報、高溫預警等）。"
                    AppLanguage.SPANISH -> "Alertas activas en tiempo real (tornado, inundación repentina, tormenta invernal, calor) desde la API oficial del NOAA NWS."
                    AppLanguage.FRENCH -> "Alertes actives en temps réel (tornade, crue éclair, tempête hivernale, canicule) via l'API officielle du NOAA NWS."
                    AppLanguage.PORTUGUESE -> "Alertas ativos em tempo real (tornado, enxurrada, tempestade de inverno, calor) da API oficial do NOAA NWS."
                    AppLanguage.ENGLISH -> "Direct real-time active weather alerts (Tornado, Flash Flood, Winter Storm, Heat) from NOAA National Weather Service API."
                },
                icon = Icons.Rounded.CrisisAlert,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "全球広域自然災害警報"
                    AppLanguage.KOREAN -> "글로벌 광역 자연재난 경보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球自然灾害应急预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球自然災害應急預警"
                    AppLanguage.SPANISH -> "Alertas globales de desastres"
                    AppLanguage.FRENCH -> "Alertes mondiales de catastrophes"
                    AppLanguage.PORTUGUESE -> "Alertas globais de desastres"
                    AppLanguage.ENGLISH -> "Global Disaster Alerts"
                },
                source = gdacsSourceLabel,
                detail = when (language) {
                    AppLanguage.JAPANESE -> "国連および欧州委員会の全球自然災害警報システム(GDACS)から、ハリケーン・大地震・火山等の重大災害警報(Red/Orange Alert)を取得・判定しています。"
                    AppLanguage.KOREAN -> "유엔 및 유럽위원회의 글로벌 자연재난 경보 시스템 (GDACS)에서 허리케인, 대지진, 화산 등 중대 재난 경보 (Red/Orange Alert)를 수신·판정합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "实时获取联合国与欧盟全球灾害预警系统(GDACS)发布的重大飓风、地震、火山等Red/Orange级紧急灾害预警。"
                    AppLanguage.TRADITIONAL_CHINESE -> "即時擷取聯合國與歐盟全球災害預警系統(GDACS)發布的重大颶風、地震、火山等Red/Orange級緊急災害預警。"
                    AppLanguage.SPANISH -> "Alertas globales de desastres (rojo/naranja) para huracanes, terremotos e inundaciones graves mediante ONU/CE GDACS."
                    AppLanguage.FRENCH -> "Alertes mondiales de catastrophes (rouge/orange) pour ouragans, séismes et inondations graves via ONU/CE GDACS."
                    AppLanguage.PORTUGUESE -> "Alertas globais de desastres (vermelho/laranja) para furacões, terremotos e enchentes graves via ONU/CE GDACS."
                    AppLanguage.ENGLISH -> "Global disaster alerts (Red/Orange) for severe hurricanes, earthquakes, and floods via UN/EC GDACS."
                },
                icon = Icons.Rounded.Public,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨雲レーダー"
                    AppLanguage.KOREAN -> "강수 레이더"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨雷達"
                    AppLanguage.SPANISH -> "Radar de precipitación"
                    AppLanguage.FRENCH -> "Radar de précipitations"
                    AppLanguage.PORTUGUESE -> "Radar de precipitação"
                    AppLanguage.ENGLISH -> "Precipitation Radar"
                },
                source = "NOAA MRMS / RainViewer",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米NOAA地上連動マルチレーダー(MRMS 1km)および全球合成レーダー(RainViewer)から高精度レーダー画像を配信しています。"
                    AppLanguage.KOREAN -> "미국 NOAA 지상 연동 다중 레이더 (MRMS 1km)와 글로벌 합성 레이더 (RainViewer)의 고해상도 레이더 영상을 제공합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "通过美国NOAA地面高分辨率雷达系统(MRMS)与全球合成雷达瓦片(RainViewer)提供。"
                    AppLanguage.TRADITIONAL_CHINESE -> "透過美國NOAA地面高解析度雷達系統(MRMS)與全球合成雷達圖磚(RainViewer)提供。"
                    AppLanguage.SPANISH -> "Radar de alta resolución mediante NOAA Multi-Radar Multi-Sensor (MRMS) y el compuesto global RainViewer."
                    AppLanguage.FRENCH -> "Radar haute résolution via NOAA Multi-Radar Multi-Sensor (MRMS) et la mosaïque mondiale RainViewer."
                    AppLanguage.PORTUGUESE -> "Radar de alta resolução via NOAA Multi-Radar Multi-Sensor (MRMS) e composto global RainViewer."
                    AppLanguage.ENGLISH -> "High-resolution radar via NOAA Multi-Radar Multi-Sensor (MRMS) and global composite RainViewer."
                },
                icon = Icons.Rounded.Radar,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "大気質 (AQI / PM2.5 / PM10)"
                    AppLanguage.KOREAN -> "대기질 (AQI / PM2.5 / PM10)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "空气质量 (AQI / PM2.5 / PM10)"
                    AppLanguage.TRADITIONAL_CHINESE -> "空氣品質 (AQI / PM2.5 / PM10)"
                    AppLanguage.SPANISH -> "Calidad del aire (AQI / PM2.5 / PM10)"
                    AppLanguage.FRENCH -> "Qualité de l'air (AQI / PM2.5 / PM10)"
                    AppLanguage.PORTUGUESE -> "Qualidade do ar (AQI / PM2.5 / PM10)"
                    AppLanguage.ENGLISH -> "Air Quality (AQI / PM2.5 / PM10)"
                },
                source = "Open-Meteo Air Quality",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "全球大気質・化学輸送モデルにより、US AQI・PM2.5・PM10等のリアルタイム大気汚染指標を提供しています。"
                    AppLanguage.KOREAN -> "전 지구 대기질·화학 수송 모델을 통해 US AQI, 초미세먼지 (PM2.5), 미세먼지 (PM10) 등 실시간 대기오염 지표를 제공합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "基于全球大气质量与化学传输模型，提供实时的US AQI、PM2.5、PM10等综合指数。"
                    AppLanguage.TRADITIONAL_CHINESE -> "基於全球大氣品質與化學傳輸模型，提供即時的US AQI、PM2.5、PM10等綜合指數。"
                    AppLanguage.SPANISH -> "Modelos globales de química atmosférica que ofrecen US AQI, PM2.5 y partículas en tiempo real."
                    AppLanguage.FRENCH -> "Modèles mondiaux de chimie atmosphérique fournissant US AQI, PM2.5 et particules en temps réel."
                    AppLanguage.PORTUGUESE -> "Modelos globais de química atmosférica fornecendo US AQI, PM2.5 e partículas em tempo real."
                    AppLanguage.ENGLISH -> "Global atmospheric chemistry models delivering real-time US AQI, PM2.5, and particulate tracking."
                },
                icon = Icons.Rounded.Air,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨の降り始め予測"
                    AppLanguage.KOREAN -> "강수 시작 예측"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水短时临近预测"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨短時即時預測"
                    AppLanguage.SPANISH -> "Nowcast de precipitación"
                    AppLanguage.FRENCH -> "Nowcast de précipitations"
                    AppLanguage.PORTUGUESE -> "Nowcast de precipitação"
                    AppLanguage.ENGLISH -> "Precipitation Nowcast"
                },
                source = "Open-Meteo 15-Min Forecast",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteoの15分間隔高精度降水モデルにより、雨の降り始めや止み時刻を予測しています。"
                    AppLanguage.KOREAN -> "Open-Meteo 15분 간격 고정밀 강수 모델로 비가 시작되거나 그치는 시각을 예측합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "依托 Open-Meteo 15分钟高分辨率步进降水模型，精准分析预测降雨起止时刻。"
                    AppLanguage.TRADITIONAL_CHINESE -> "依託 Open-Meteo 15分鐘高解析度步進降水模型，精準分析預測降雨起止時刻。"
                    AppLanguage.SPANISH -> "Modelo de precipitación en intervalos de 15 minutos que predice con precisión el inicio y fin de la lluvia."
                    AppLanguage.FRENCH -> "Modèle de précipitations par intervalles de 15 minutes prédisant précisément le début et la fin de la pluie."
                    AppLanguage.PORTUGUESE -> "Modelo de precipitação em intervalos de 15 minutos prevendo com precisão o início e o fim da chuva."
                    AppLanguage.ENGLISH -> "Precision 15-minute interval precipitation model forecasting exact rain start and stop times."
                },
                icon = Icons.Rounded.Umbrella,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "日の出・日の入り・太陽軌道"
                    AppLanguage.KOREAN -> "일출 · 일몰 및 태양 궤적"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日出日落・太阳轨迹"
                    AppLanguage.TRADITIONAL_CHINESE -> "日出日落・太陽軌跡"
                    AppLanguage.SPANISH -> "Amanecer / Atardecer y arco solar"
                    AppLanguage.FRENCH -> "Lever / Coucher et course du soleil"
                    AppLanguage.PORTUGUESE -> "Nascer / Pôr do sol e arco solar"
                    AppLanguage.ENGLISH -> "Sunrise / Sunset & Solar Transit"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "NOAA天文学算式 (端末独自計算)"
                    AppLanguage.KOREAN -> "NOAA 천문 계산식 (기기 내 연산)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "NOAA天文学公式 (设备端计算)"
                    AppLanguage.TRADITIONAL_CHINESE -> "NOAA天文學公式 (裝置端計算)"
                    AppLanguage.SPANISH -> "Algoritmo NOAA (En el dispositivo)"
                    AppLanguage.FRENCH -> "Algorithme NOAA (Local)"
                    AppLanguage.PORTUGUESE -> "Algoritmo NOAA (No dispositivo)"
                    AppLanguage.ENGLISH -> "NOAA Algorithm (On-device)"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米国海洋大気庁(NOAA)の天文学数式アルゴリズムに基づき、端末CPUで緯度・経度から秒単位の日の出・日の入り・日長・南中進捗を完全オフライン独自計算しています。"
                    AppLanguage.KOREAN -> "미국 해양대기청 (NOAA) 천문 계산 알고리즘을 기반으로 기기에서 위도·경도를 통해 일출, 일몰, 낮 길이, 남중 진행도를 오프라인으로 정밀 계산합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "基于美国国家海洋和大气管理局(NOAA)天文学公式，由设备端CPU依据经纬度全离线精确计算日出、日落、昼长与中天轨迹。"
                    AppLanguage.TRADITIONAL_CHINESE -> "基於美國國家海洋暨大氣總署(NOAA)天文學公式，由裝置端CPU依據經緯度全離線精確計算日出、日落、晝長與中天軌跡。"
                    AppLanguage.SPANISH -> "Calculado íntegramente en el dispositivo mediante fórmulas astronómicas solares de la NOAA para amanecer, atardecer y arco solar precisos."
                    AppLanguage.FRENCH -> "Calculé entièrement sur l'appareil via les formules astronomiques solaires de la NOAA pour le lever, le coucher et la course du soleil."
                    AppLanguage.PORTUGUESE -> "Calculado inteiramente no dispositivo pelas fórmulas astronômicas solares da NOAA para nascer, pôr do sol e arco solar precisos."
                    AppLanguage.ENGLISH -> "Calculated entirely on-device via NOAA Solar Calculation astronomical formulas for precision sunrise, sunset, and solar arc."
                },
                icon = Icons.Rounded.WbSunny,
                isDirectJma = false
            )
        )
    } else {
        listOf(
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "気象実況 & 天気予報 (24h/週間)"
                    AppLanguage.KOREAN -> "기상 실황 및 일기예보 (24시간/주간)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "气象实况与天气预报 (24h/周预报)"
                    AppLanguage.TRADITIONAL_CHINESE -> "氣象實況與天氣預報 (24h/週預報)"
                    AppLanguage.SPANISH -> "Observaciones y pronósticos"
                    AppLanguage.FRENCH -> "Observations et prévisions"
                    AppLanguage.PORTUGUESE -> "Observações e previsões"
                    AppLanguage.ENGLISH -> "Weather Observations & Forecasts"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteo 全球数値モデル"
                    AppLanguage.KOREAN -> "Open-Meteo 전 지구 수치 모델"
                    AppLanguage.SIMPLIFIED_CHINESE -> "Open-Meteo 全球数值预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "Open-Meteo 全球數值預報"
                    AppLanguage.SPANISH -> "Open-Meteo NWP Global"
                    AppLanguage.FRENCH -> "Open-Meteo NWP Mondial"
                    AppLanguage.PORTUGUESE -> "Open-Meteo NWP Global"
                    AppLanguage.ENGLISH -> "Open-Meteo Global NWP"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "欧州中期予報センター(ECMWF)、米NOAA GFS、独気象局ICON等の各国の最高峰全球モデルをシームレスに統合した高精度気象データです。"
                    AppLanguage.KOREAN -> "유럽중기예보센터 (ECMWF), 미국 NOAA GFS, 독일 기상청 ICON 등 주요 글로벌 수치 예보 모델을 통합한 고정밀 기상 데이터입니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "融合欧洲中心(ECMWF)、美国GFS及德国ICON等全球顶级数值预报模型的高精度无缝气象数据。"
                    AppLanguage.TRADITIONAL_CHINESE -> "融合歐洲中心(ECMWF)、美國GFS及德國ICON等全球頂級數值預報模型的高精度無縫氣象資料。"
                    AppLanguage.SPANISH -> "Datos meteorológicos de alta resolución que combinan los modelos globales ECMWF, NOAA GFS y DWD ICON."
                    AppLanguage.FRENCH -> "Données météo haute résolution combinant les modèles mondiaux ECMWF, NOAA GFS et DWD ICON."
                    AppLanguage.PORTUGUESE -> "Dados meteorológicos de alta resolução combinando os modelos globais ECMWF, NOAA GFS e DWD ICON."
                    AppLanguage.ENGLISH -> "High-resolution seamless weather blending ECMWF, NOAA GFS, and DWD ICON global models."
                },
                icon = Icons.Rounded.Thermostat,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "全球広域自然災害警報"
                    AppLanguage.KOREAN -> "글로벌 광역 자연재난 경보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球自然灾害应急预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球自然災害應急預警"
                    AppLanguage.SPANISH -> "Alertas globales de desastres"
                    AppLanguage.FRENCH -> "Alertes mondiales de catastrophes"
                    AppLanguage.PORTUGUESE -> "Alertas globais de desastres"
                    AppLanguage.ENGLISH -> "Global Disaster Alerts"
                },
                source = gdacsSourceLabel,
                detail = when (language) {
                    AppLanguage.JAPANESE -> "国連および欧州委員会の全球自然災害警報システム(GDACS)から、サイクロン・台風・地震・洪水・火山噴火・森林火災の重大災害警報(Red/Orange Alert)を取得・判定しています。"
                    AppLanguage.KOREAN -> "유엔 및 유럽위원회의 글로벌 자연재난 경보 시스템 (GDACS)에서 사이클론, 태풍, 지진, 홍수, 화산 분화 등 중대 재난 경보 (Red/Orange Alert)를 수신·판정합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "实时获取联合国与欧盟全球灾害预警系统(GDACS)发布的重大热带气旋、地震、洪水、火山喷发等Red/Orange级紧急灾害预警。"
                    AppLanguage.TRADITIONAL_CHINESE -> "即時擷取聯合國與歐盟全球災害預警系統(GDACS)發布的重大熱帶氣旋、地震、洪水、火山噴發等Red/Orange級緊急災害預警。"
                    AppLanguage.SPANISH -> "Alertas globales de desastres en tiempo real para ciclones, terremotos e inundaciones graves mediante ONU/CE GDACS."
                    AppLanguage.FRENCH -> "Alertes mondiales en temps réel pour les cyclones, séismes et inondations graves via ONU/CE GDACS."
                    AppLanguage.PORTUGUESE -> "Alertas globais de desastres em tempo real para ciclones, terremotos e enchentes graves via ONU/CE GDACS."
                    AppLanguage.ENGLISH -> "Real-time global disaster alerts for severe cyclones, earthquakes, and floods via UN/EC GDACS."
                },
                icon = Icons.Rounded.Public,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "大気質 (AQI / PM2.5 / PM10)"
                    AppLanguage.KOREAN -> "대기질 (AQI / PM2.5 / PM10)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "空气质量 (AQI / PM2.5 / PM10)"
                    AppLanguage.TRADITIONAL_CHINESE -> "空氣品質 (AQI / PM2.5 / PM10)"
                    AppLanguage.SPANISH -> "Calidad del aire (AQI / PM2.5 / PM10)"
                    AppLanguage.FRENCH -> "Qualité de l'air (AQI / PM2.5 / PM10)"
                    AppLanguage.PORTUGUESE -> "Qualidade do ar (AQI / PM2.5 / PM10)"
                    AppLanguage.ENGLISH -> "Air Quality (AQI / PM2.5 / PM10)"
                },
                source = "Open-Meteo Air Quality",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "全球大気質・化学輸送モデルにより、US AQI・European AQI・PM2.5・PM10等のリアルタイム大気汚染指標を提供しています。"
                    AppLanguage.KOREAN -> "전 지구 대기질·화학 수송 모델을 통해 US AQI, 유럽 AQI, 초미세먼지 (PM2.5), 미세먼지 (PM10) 등 실시간 대기오염 지표를 제공합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "基于全球大气质量与化学传输模型，提供实时的US AQI、European AQI、PM2.5、PM10等综合指数。"
                    AppLanguage.TRADITIONAL_CHINESE -> "基於全球大氣品質與化學傳輸模型，提供即時的US AQI、European AQI、PM2.5、PM10等綜合指數。"
                    AppLanguage.SPANISH -> "Modelos globales de química atmosférica que ofrecen US AQI, AQI europeo, PM2.5 y partículas en tiempo real."
                    AppLanguage.FRENCH -> "Modèles mondiaux de chimie atmosphérique fournissant US AQI, AQI européen, PM2.5 et particules en temps réel."
                    AppLanguage.PORTUGUESE -> "Modelos globais de química atmosférica fornecendo US AQI, AQI europeu, PM2.5 e partículas em tempo real."
                    AppLanguage.ENGLISH -> "Global atmospheric chemistry models delivering real-time US AQI, European AQI, PM2.5, and particulate tracking."
                },
                icon = Icons.Rounded.Air,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨雲レーダー"
                    AppLanguage.KOREAN -> "강수 레이더"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨雷達"
                    AppLanguage.SPANISH -> "Radar de precipitación"
                    AppLanguage.FRENCH -> "Radar de précipitations"
                    AppLanguage.PORTUGUESE -> "Radar de precipitação"
                    AppLanguage.ENGLISH -> "Precipitation Radar"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "RainViewer レーダー合成"
                    AppLanguage.KOREAN -> "RainViewer 레이더 합성"
                    AppLanguage.SIMPLIFIED_CHINESE -> "RainViewer 雷达合成"
                    AppLanguage.TRADITIONAL_CHINESE -> "RainViewer 雷達合成"
                    AppLanguage.SPANISH -> "Radar compuesto RainViewer"
                    AppLanguage.FRENCH -> "Radar composite RainViewer"
                    AppLanguage.PORTUGUESE -> "Radar composto RainViewer"
                    AppLanguage.ENGLISH -> "RainViewer Composite Radar"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "全球気象レーダー合成タイル (RainViewer) から、各国の気象観測レーダーを統合した降水データを配信しています。"
                    AppLanguage.KOREAN -> "글로벌 기상 레이더 합성 타일 (RainViewer)을 통해 각국 기상 관측 레이더를 통합한 강수 데이터를 제공합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "通过 RainViewer 全球雷达综合瓦片提供多国气象雷达无缝观测数据。"
                    AppLanguage.TRADITIONAL_CHINESE -> "透過 RainViewer 全球雷達綜合圖磚提供多國氣象雷達無縫觀測資料。"
                    AppLanguage.SPANISH -> "Imágenes compuestas globales de radar de precipitación mediante mosaicos multirradar de RainViewer."
                    AppLanguage.FRENCH -> "Imagerie radar composite mondiale des précipitations via les tuiles multi-radars RainViewer."
                    AppLanguage.PORTUGUESE -> "Imagens compostas globais de radar de precipitação via blocos multirradar RainViewer."
                    AppLanguage.ENGLISH -> "Global composite precipitation radar imagery via RainViewer multi-radar tiles."
                },
                icon = Icons.Rounded.Radar,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨の降り始め予測"
                    AppLanguage.KOREAN -> "강수 시작 예측"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水短时临近预测"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨短時即時預測"
                    AppLanguage.SPANISH -> "Nowcast de precipitación"
                    AppLanguage.FRENCH -> "Nowcast de précipitations"
                    AppLanguage.PORTUGUESE -> "Nowcast de precipitação"
                    AppLanguage.ENGLISH -> "Precipitation Nowcast"
                },
                source = "Open-Meteo 15-Min Forecast",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteoの15分間隔高精度降水モデルにより、雨の降り始めや止み時刻を予測しています。"
                    AppLanguage.KOREAN -> "Open-Meteo 15분 간격 고정밀 강수 모델로 비가 시작되거나 그치는 시각을 예측합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "依托 Open-Meteo 15分钟高分辨率步进降水模型，精准分析预测降雨起止时刻。"
                    AppLanguage.TRADITIONAL_CHINESE -> "依託 Open-Meteo 15分鐘高解析度步進降水模型，精準分析預測降雨起止時刻。"
                    AppLanguage.SPANISH -> "Modelo de precipitación en intervalos de 15 minutos que predice con precisión el inicio y fin de la lluvia."
                    AppLanguage.FRENCH -> "Modèle de précipitations par intervalles de 15 minutes prédisant précisément le début et la fin de la pluie."
                    AppLanguage.PORTUGUESE -> "Modelo de precipitação em intervalos de 15 minutos prevendo com precisão o início e o fim da chuva."
                    AppLanguage.ENGLISH -> "Precision 15-minute interval precipitation model forecasting exact rain start and stop times."
                },
                icon = Icons.Rounded.Umbrella,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "日の出・日の入り・太陽軌道"
                    AppLanguage.KOREAN -> "일출 · 일몰 및 태양 궤적"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日出日落・太阳轨迹"
                    AppLanguage.TRADITIONAL_CHINESE -> "日出日落・太陽軌跡"
                    AppLanguage.SPANISH -> "Amanecer / Atardecer y arco solar"
                    AppLanguage.FRENCH -> "Lever / Coucher et course du soleil"
                    AppLanguage.PORTUGUESE -> "Nascer / Pôr do sol e arco solar"
                    AppLanguage.ENGLISH -> "Sunrise / Sunset & Solar Transit"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "NOAA天文学算式 (端末独自計算)"
                    AppLanguage.KOREAN -> "NOAA 천문 계산식 (기기 내 연산)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "NOAA天文学公式 (设备端计算)"
                    AppLanguage.TRADITIONAL_CHINESE -> "NOAA天文學公式 (裝置端計算)"
                    AppLanguage.SPANISH -> "Algoritmo NOAA (En el dispositivo)"
                    AppLanguage.FRENCH -> "Algorithme NOAA (Local)"
                    AppLanguage.PORTUGUESE -> "Algoritmo NOAA (No dispositivo)"
                    AppLanguage.ENGLISH -> "NOAA Algorithm (On-device)"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米国海洋大気庁(NOAA)の天文学数式アルゴリズムに基づき、端末CPUで緯度・経度から秒単位の日の出・日の入り・日長・南中進捗を完全オフライン独自計算しています。"
                    AppLanguage.KOREAN -> "미국 해양대기청 (NOAA) 천문 계산 알고리즘을 기반으로 기기에서 위도·경도를 통해 일출, 일몰, 낮 길이, 남중 진행도를 오프라인으로 정밀 계산합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "基于美国国家海洋和大气管理局(NOAA)天文学公式，由设备端CPU依据经纬度全离线精确计算日出、日落、昼长与中天轨迹。"
                    AppLanguage.TRADITIONAL_CHINESE -> "基於美國國家海洋暨大氣總署(NOAA)天文學公式，由裝置端CPU依據經緯度全離線精確計算日出、日落、晝長與中天軌跡。"
                    AppLanguage.SPANISH -> "Calculado íntegramente en el dispositivo mediante fórmulas astronómicas solares de la NOAA para amanecer, atardecer y arco solar precisos."
                    AppLanguage.FRENCH -> "Calculé entièrement sur l'appareil via les formules astronomiques solaires de la NOAA pour le lever, le coucher et la course du soleil."
                    AppLanguage.PORTUGUESE -> "Calculado inteiramente no dispositivo pelas fórmulas astronômicas solares da NOAA para nascer, pôr do sol e arco solar precisos."
                    AppLanguage.ENGLISH -> "Calculated entirely on-device via NOAA Solar Calculation astronomical formulas for precision sunrise, sunset, and solar arc."
                },
                icon = Icons.Rounded.WbSunny,
                isDirectJma = false
            )
        )
    }
}

