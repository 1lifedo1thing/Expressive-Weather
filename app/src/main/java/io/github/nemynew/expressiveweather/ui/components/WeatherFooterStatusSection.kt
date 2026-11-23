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
            weather.city.country.equals("Japan", ignoreCase = true) ||
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
                    contentDescription = "再読み込み",
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
                            AppLanguage.SIMPLIFIED_CHINESE -> "所有显示数据提供方"
                            AppLanguage.TRADITIONAL_CHINESE -> "所有顯示資料提供方"
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
                    contentDescription = if (isSourcesExpanded) "折りたたむ" else "展開する",
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
    return if (isJapan) {
        listOf(
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "気象実況 (気温/湿度/気圧/風/雨)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "气象实况 (气温/湿度/气压/风速/雨量)"
                    AppLanguage.TRADITIONAL_CHINESE -> "氣象實況 (氣溫/濕度/氣壓/風速/雨量)"
                    AppLanguage.ENGLISH -> "Observations (Temp/Humidity/Wind/Rain)"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) アメダス実測"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) AMeDAS实测"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) AMeDAS實測"
                    AppLanguage.ENGLISH -> "JMA AMeDAS (Direct)"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "全国約1,300地点のアメダス観測所から最寄り実測値（気温・湿度・気圧・風向風速・降水量・最高最低気温）を直接リアルタイム取得しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接实时获取自全国约1,300个AMeDAS气象观测站实测数据（气温、湿度、气压、风向风速、降水量、最高最低温度）。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接即時擷取自全國約1,300個AMeDAS氣象觀測站實測資料（氣溫、濕度、氣壓、風向風速、降水量、最高最低溫度）。"
                    AppLanguage.ENGLISH -> "Real-time direct measurements (temperature, humidity, pressure, wind, rain, highs/lows) from ~1,300 AMeDAS stations nationwide."
                },
                icon = Icons.Rounded.Thermostat,
                isDirectJma = true
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "天気予報 (24時間・週間7日)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "天气预报 (24小时・7天预报)"
                    AppLanguage.TRADITIONAL_CHINESE -> "天氣預報 (24小時・7天預報)"
                    AppLanguage.ENGLISH -> "Forecasts (Hourly & 7-Day)"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) 府県・週間予報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) 区域・周预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) 區域・週預報"
                    AppLanguage.ENGLISH -> "JMA Official Forecasts"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "各管区気象台発表の府県天気予報、6時間区切り降水確率、週間天気予報コード及び予報信頼度を直接取得しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接获取各管区气象台发布的区域天气预报、6小时降水概率、一周预报代码与可靠度评级。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接擷取各管區氣象台發布的區域天氣預報、6小時降水機率、一週預報代碼與可靠度評級。"
                    AppLanguage.ENGLISH -> "Direct regional prefectural forecasts, 6-hour precipitation probabilities, and 7-day outlook codes with reliability ratings."
                },
                icon = Icons.Rounded.CalendarMonth,
                isDirectJma = true
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "警報・注意報 & 天気概況解説"
                    AppLanguage.SIMPLIFIED_CHINESE -> "预警信息 & 天气概况解说"
                    AppLanguage.TRADITIONAL_CHINESE -> "警報資訊 & 天氣概況解說"
                    AppLanguage.ENGLISH -> "Warnings & Weather Synopsis"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) 防災気象情報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) 防灾信息"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) 防災資訊"
                    AppLanguage.ENGLISH -> "JMA Disaster Alerts"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "気象台予報官執筆の天気概況解説文、市区町村別特別警報・警報・注意報の発表状況を直接取得しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接获取预报员撰写的天气概况解说文，以及各市区町村特别警报・警报・注意报。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接擷取預報員撰寫的天氣概況解說文，以及各市區町村特別警報・警報・注意報。"
                    AppLanguage.ENGLISH -> "Direct forecaster synoptic overviews and municipality-level emergency, warning, and advisory alerts."
                },
                icon = Icons.Rounded.WarningAmber,
                isDirectJma = true
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "広域大規模自然災害警報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "广域自然灾害应急预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "廣域自然災害應急預警"
                    AppLanguage.ENGLISH -> "Global Disaster Alerts"
                },
                source = "国連/欧州委員会 GDACS",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "巨大サイクロン・大規模地震・広域洪水等の発生時に、国連および欧州委員会(GDACS)の全球自然災害警報を直接監視・連携表示します。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "在特大台风、特大地震、严重洪水发生时，联动监控联合国与欧盟GDACS全球自然灾害应急预警。"
                    AppLanguage.TRADITIONAL_CHINESE -> "在特大颱風、特大地震、嚴重洪水發生時，聯動監控聯合國與歐盟GDACS全球自然災害應急預警。"
                    AppLanguage.ENGLISH -> "Direct integration with UN/EC Global Disaster Alert and Coordination System for major cyclones, earthquakes, and floods."
                },
                icon = Icons.Rounded.Public,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨雲レーダー・キキクル・衛星"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达・危险度分布・气象卫星"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨雷達・危險度分布・氣象衛星"
                    AppLanguage.ENGLISH -> "Radar, Kikikuru & Satellite"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) 高解像度ナウキャスト"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) 高清Nowcast"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) 高畫質Nowcast"
                    AppLanguage.ENGLISH -> "JMA High-Res Nowcast"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "高解像度降水ナウキャスト(HR-PNS 250mメッシュ雨雲)、土砂浸水洪水キキクル危険度分布、気象衛星ひまわりタイルを公式配信サーバーから直接取得しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接从官方服务获取250m网格高清降水Nowcast雷达、土砂浸水洪水Kikikuru危险度分布及向日葵卫星瓦片。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接從官方服務擷取250m網格高畫質降水Nowcast雷達、土砂浸水洪水Kikikuru危險度分布及向日葵衛星圖磚。"
                    AppLanguage.ENGLISH -> "Direct JMA high-resolution precipitation nowcast (250m mesh), Kikikuru hazard distributions, and Himawari satellite imagery."
                },
                icon = Icons.Rounded.Radar,
                isDirectJma = true
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "大気質 (AQI / PM2.5 / PM10)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "空气质量 (AQI / PM2.5 / PM10)"
                    AppLanguage.TRADITIONAL_CHINESE -> "空氣品質 (AQI / PM2.5 / PM10)"
                    AppLanguage.ENGLISH -> "Air Quality (AQI / PM2.5 / PM10)"
                },
                source = "Open-Meteo Air Quality",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "気象庁APIにはリアルタイム全国大気質APIがないため、Open-Meteoの大気化学モデルAPIからUS AQI・PM2.5・PM10・NO2・O3を補完取得しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "由于气象厅未提供实时全国空气质量接口，通过Open-Meteo全球大气化学模型API补充获取US AQI、PM2.5、PM10与各类气体指标。"
                    AppLanguage.TRADITIONAL_CHINESE -> "由於氣象廳未提供即時全國空氣品質介面，透過Open-Meteo全球大氣化學模型API補充擷取US AQI、PM2.5、PM10與各類氣體指標。"
                    AppLanguage.ENGLISH -> "Supplemented via Open-Meteo global atmospheric chemistry models for US AQI, PM2.5, PM10, NO2, and Ozone."
                },
                icon = Icons.Rounded.Air,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨の降り始め・止み予測"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水起止分析预测"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨起止分析預測"
                    AppLanguage.ENGLISH -> "Precipitation Nowcast Prediction"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) ＆ Open-Meteo"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 & Open-Meteo"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 & Open-Meteo"
                    AppLanguage.ENGLISH -> "JMA & Open-Meteo (Hybrid)"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "気象台公式概況の降雨判定と、Open-Meteoの15分刻み超短期雨量予測を統合し、アプリ内で雨の降り始め・止み時刻をハイブリッド解析しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "综合气象厅天气概况降雨判定与Open-Meteo 15分钟高分辨率雨量预报，在应用内混合计算降雨起止时刻。"
                    AppLanguage.TRADITIONAL_CHINESE -> "綜合氣象廳天氣概況降雨判定與Open-Meteo 15分鐘高解析度雨量預報，在應用程式內混合計算降雨起止時刻。"
                    AppLanguage.ENGLISH -> "Hybrid analysis combining JMA synoptic weather bulletins and Open-Meteo 15-minute precipitation steps."
                },
                icon = Icons.Rounded.Umbrella,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "日の出・日の入り・太陽軌道"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日出日落・太阳轨迹"
                    AppLanguage.TRADITIONAL_CHINESE -> "日出日落・太陽軌跡"
                    AppLanguage.ENGLISH -> "Sunrise / Sunset & Solar Transit"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "NOAA天文学算式 (端末独自計算)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "NOAA天文学公式 (设备端计算)"
                    AppLanguage.TRADITIONAL_CHINESE -> "NOAA天文學公式 (裝置端計算)"
                    AppLanguage.ENGLISH -> "NOAA Algorithm (On-device)"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米国海洋大気庁(NOAA)の天文学数式アルゴリズムに基づき、端末CPUで緯度・経度から秒単位の日の出・日の入り・日長・南中進捗を完全オフライン独自計算しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "基于美国国家海洋和大气管理局(NOAA)天文学公式，由设备端CPU依据经纬度全离线精确计算日出、日落、昼长与中天轨迹。"
                    AppLanguage.TRADITIONAL_CHINESE -> "基於美國國家海洋暨大氣總署(NOAA)天文學公式，由裝置端CPU依據經緯度全離線精確計算日出、日落、晝長與中天軌跡。"
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
                    AppLanguage.SIMPLIFIED_CHINESE -> "气象实况与天气预报 (24h/周预报)"
                    AppLanguage.TRADITIONAL_CHINESE -> "氣象實況與天氣預報 (24h/週預報)"
                    AppLanguage.ENGLISH -> "Weather Observations & Forecasts"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteo 全球数値モデル"
                    AppLanguage.SIMPLIFIED_CHINESE -> "Open-Meteo 全球数值预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "Open-Meteo 全球數值預報"
                    AppLanguage.ENGLISH -> "Open-Meteo Global NWP"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米NOAA GFS、HRRR、欧州ECMWF等の最高峰全球・局地モデルをシームレスに統合した高精度気象データです。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "融合美国NOAA GFS、HRRR及欧洲ECMWF等高分辨率数值预报模型的实时高精度气象数据。"
                    AppLanguage.TRADITIONAL_CHINESE -> "融合美國NOAA GFS、HRRR及歐洲ECMWF等高解析度數值預報模型的即時高精度氣象資料。"
                    AppLanguage.ENGLISH -> "High-resolution seamless weather blending NOAA GFS, HRRR, and ECMWF models."
                },
                icon = Icons.Rounded.Thermostat,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "気象警報・注意報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "气象预警・提示信息"
                    AppLanguage.TRADITIONAL_CHINESE -> "氣象警報・提示資訊"
                    AppLanguage.ENGLISH -> "Weather Warnings & Watches"
                },
                source = "米国気象局 (NOAA NWS) 公式",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米国国立気象局(NWS)公式API(api.weather.gov)から、竜巻警報・鉄砲水警報・暴風雪警報・熱波注意報等のアクティブ警報を直接リアルタイム取得しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接实时获取自美国国家气象局(NWS)官方接口的活动预警（龙卷风警报、山洪警报、暴风雪警报、高温预警等）。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接即時擷取自美國國家氣象局(NWS)官方介面的活動預警（龍捲風警報、暴洪警報、暴風雪警報、高溫預警等）。"
                    AppLanguage.ENGLISH -> "Direct real-time active weather alerts (Tornado, Flash Flood, Winter Storm, Heat) from NOAA National Weather Service API."
                },
                icon = Icons.Rounded.CrisisAlert,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "全球広域自然災害警報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球自然灾害应急预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球自然災害應急預警"
                    AppLanguage.ENGLISH -> "Global Disaster Alerts"
                },
                source = "国連/欧州委員会 GDACS",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "国連および欧州委員会の全球自然災害警報システム(GDACS)から、ハリケーン・大地震・火山等の重大災害警報(Red/Orange Alert)を取得・判定しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "实时获取联合国与欧盟全球灾害预警系统(GDACS)发布的重大飓风、地震、火山等Red/Orange级紧急灾害预警。"
                    AppLanguage.TRADITIONAL_CHINESE -> "即時擷取聯合國與歐盟全球災害預警系統(GDACS)發布的重大颶風、地震、火山等Red/Orange級緊急災害預警。"
                    AppLanguage.ENGLISH -> "Global disaster alerts (Red/Orange) for severe hurricanes, earthquakes, and floods via UN/EC GDACS."
                },
                icon = Icons.Rounded.Public,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨雲レーダー"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨雷達"
                    AppLanguage.ENGLISH -> "Precipitation Radar"
                },
                source = "NOAA MRMS / RainViewer",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米NOAA地上連動マルチレーダー(MRMS 1km)および全球合成レーダー(RainViewer)から高精度レーダー画像を配信しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "通过美国NOAA地面高分辨率雷达系统(MRMS)与全球合成雷达瓦片(RainViewer)提供。"
                    AppLanguage.TRADITIONAL_CHINESE -> "透過美國NOAA地面高解析度雷達系統(MRMS)與全球合成雷達圖磚(RainViewer)提供。"
                    AppLanguage.ENGLISH -> "High-resolution radar via NOAA Multi-Radar Multi-Sensor (MRMS) and global composite RainViewer."
                },
                icon = Icons.Rounded.Radar,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "大気質 (AQI / PM2.5 / PM10)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "空气质量 (AQI / PM2.5 / PM10)"
                    AppLanguage.TRADITIONAL_CHINESE -> "空氣品質 (AQI / PM2.5 / PM10)"
                    AppLanguage.ENGLISH -> "Air Quality (AQI / PM2.5 / PM10)"
                },
                source = "Open-Meteo Air Quality",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "全球大気質・化学輸送モデルにより、US AQI・PM2.5・PM10等のリアルタイム大気汚染指標を提供しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "基于全球大气质量与化学传输模型，提供实时的US AQI、PM2.5、PM10等综合指数。"
                    AppLanguage.TRADITIONAL_CHINESE -> "基於全球大氣品質與化學傳輸模型，提供即時的US AQI、PM2.5、PM10等綜合指數。"
                    AppLanguage.ENGLISH -> "Global atmospheric chemistry models delivering real-time US AQI, PM2.5, and particulate tracking."
                },
                icon = Icons.Rounded.Air,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨の降り始め予測"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水短时临近预测"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨短時即時預測"
                    AppLanguage.ENGLISH -> "Precipitation Nowcast"
                },
                source = "Open-Meteo 15-Min Forecast",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteoの15分間隔高精度降水モデルにより、雨の降り始めや止み時刻を予測しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "依托 Open-Meteo 15分钟高分辨率步进降水模型，精准分析预测降雨起止时刻。"
                    AppLanguage.TRADITIONAL_CHINESE -> "依託 Open-Meteo 15分鐘高解析度步進降水模型，精準分析預測降雨起止時刻。"
                    AppLanguage.ENGLISH -> "Precision 15-minute interval precipitation model forecasting exact rain start and stop times."
                },
                icon = Icons.Rounded.Umbrella,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "日の出・日の入り・太陽軌道"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日出日落・太阳轨迹"
                    AppLanguage.TRADITIONAL_CHINESE -> "日出日落・太陽軌跡"
                    AppLanguage.ENGLISH -> "Sunrise / Sunset & Solar Transit"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "NOAA天文学算式 (端末独自計算)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "NOAA天文学公式 (设备端计算)"
                    AppLanguage.TRADITIONAL_CHINESE -> "NOAA天文學公式 (裝置端計算)"
                    AppLanguage.ENGLISH -> "NOAA Algorithm (On-device)"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米国海洋大気庁(NOAA)の天文学数式アルゴリズムに基づき、端末CPUで緯度・経度から秒単位の日の出・日の入り・日長・南中進捗を完全オフライン独自計算しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "基于美国国家海洋和大气管理局(NOAA)天文学公式，由设备端CPU依据经纬度全离线精确计算日出、日落、昼长与中天轨迹。"
                    AppLanguage.TRADITIONAL_CHINESE -> "基於美國國家海洋暨大氣總署(NOAA)天文學公式，由裝置端CPU依據經緯度全離線精確計算日出、日落、晝長與中天軌跡。"
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
                    AppLanguage.SIMPLIFIED_CHINESE -> "气象实况与天气预报 (24h/周预报)"
                    AppLanguage.TRADITIONAL_CHINESE -> "氣象實況與天氣預報 (24h/週預報)"
                    AppLanguage.ENGLISH -> "Weather Observations & Forecasts"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteo 全球数値モデル"
                    AppLanguage.SIMPLIFIED_CHINESE -> "Open-Meteo 全球数值预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "Open-Meteo 全球數值預報"
                    AppLanguage.ENGLISH -> "Open-Meteo Global NWP"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "欧州中期予報センター(ECMWF)、米NOAA GFS、独気象局ICON等の各国の最高峰全球モデルをシームレスに統合した高精度気象データです。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "融合欧洲中心(ECMWF)、美国GFS及德国ICON等全球顶级数值预报模型的高精度无缝气象数据。"
                    AppLanguage.TRADITIONAL_CHINESE -> "融合歐洲中心(ECMWF)、美國GFS及德國ICON等全球頂級數值預報模型的高精度無縫氣象資料。"
                    AppLanguage.ENGLISH -> "High-resolution seamless weather blending ECMWF, NOAA GFS, and DWD ICON global models."
                },
                icon = Icons.Rounded.Thermostat,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "全球広域自然災害警報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球自然灾害应急预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球自然災害應急預警"
                    AppLanguage.ENGLISH -> "Global Disaster Alerts"
                },
                source = "国連/欧州委員会 GDACS",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "国連および欧州委員会の全球自然災害警報システム(GDACS)から、サイクロン・台風・地震・洪水・火山噴火・森林火災の重大災害警報(Red/Orange Alert)を取得・判定しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "实时获取联合国与欧盟全球灾害预警系统(GDACS)发布的重大热带气旋、地震、洪水、火山喷发等Red/Orange级紧急灾害预警。"
                    AppLanguage.TRADITIONAL_CHINESE -> "即時擷取聯合國與歐盟全球災害預警系統(GDACS)發布的重大熱帶氣旋、地震、洪水、火山噴發等Red/Orange級緊急災害預警。"
                    AppLanguage.ENGLISH -> "Real-time global disaster alerts for severe cyclones, earthquakes, and floods via UN/EC GDACS."
                },
                icon = Icons.Rounded.Public,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "大気質 (AQI / PM2.5 / PM10)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "空气质量 (AQI / PM2.5 / PM10)"
                    AppLanguage.TRADITIONAL_CHINESE -> "空氣品質 (AQI / PM2.5 / PM10)"
                    AppLanguage.ENGLISH -> "Air Quality (AQI / PM2.5 / PM10)"
                },
                source = "Open-Meteo Air Quality",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "全球大気質・化学輸送モデルにより、US AQI・European AQI・PM2.5・PM10等のリアルタイム大気汚染指標を提供しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "基于全球大气质量与化学传输模型，提供实时的US AQI、European AQI、PM2.5、PM10等综合指数。"
                    AppLanguage.TRADITIONAL_CHINESE -> "基於全球大氣品質與化學傳輸模型，提供即時的US AQI、European AQI、PM2.5、PM10等綜合指數。"
                    AppLanguage.ENGLISH -> "Global atmospheric chemistry models delivering real-time US AQI, European AQI, PM2.5, and particulate tracking."
                },
                icon = Icons.Rounded.Air,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨雲レーダー"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨雷達"
                    AppLanguage.ENGLISH -> "Precipitation Radar"
                },
                source = "RainViewer レーダー合成",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "全球気象レーダー合成タイル (RainViewer) から、各国の気象観測レーダーを統合した降水データを配信しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "通过 RainViewer 全球雷达综合瓦片提供多国气象雷达无缝观测数据。"
                    AppLanguage.TRADITIONAL_CHINESE -> "透過 RainViewer 全球雷達綜合圖磚提供多國氣象雷達無縫觀測資料。"
                    AppLanguage.ENGLISH -> "Global composite precipitation radar imagery via RainViewer multi-radar tiles."
                },
                icon = Icons.Rounded.Radar,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "雨の降り始め予測"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水短时临近预测"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨短時即時預測"
                    AppLanguage.ENGLISH -> "Precipitation Nowcast"
                },
                source = "Open-Meteo 15-Min Forecast",
                detail = when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteoの15分間隔高精度降水モデルにより、雨の降り始めや止み時刻を予測しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "依托 Open-Meteo 15分钟高分辨率步进降水模型，精准分析预测降雨起止时刻。"
                    AppLanguage.TRADITIONAL_CHINESE -> "依託 Open-Meteo 15分鐘高解析度步進降水模型，精準分析預測降雨起止時刻。"
                    AppLanguage.ENGLISH -> "Precision 15-minute interval precipitation model forecasting exact rain start and stop times."
                },
                icon = Icons.Rounded.Umbrella,
                isDirectJma = false
            ),
            WeatherDataSourceEntry(
                category = when (language) {
                    AppLanguage.JAPANESE -> "日の出・日の入り・太陽軌道"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日出日落・太阳轨迹"
                    AppLanguage.TRADITIONAL_CHINESE -> "日出日落・太陽軌跡"
                    AppLanguage.ENGLISH -> "Sunrise / Sunset & Solar Transit"
                },
                source = when (language) {
                    AppLanguage.JAPANESE -> "NOAA天文学算式 (端末独自計算)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "NOAA天文学公式 (设备端计算)"
                    AppLanguage.TRADITIONAL_CHINESE -> "NOAA天文學公式 (裝置端計算)"
                    AppLanguage.ENGLISH -> "NOAA Algorithm (On-device)"
                },
                detail = when (language) {
                    AppLanguage.JAPANESE -> "米国海洋大気庁(NOAA)の天文学数式アルゴリズムに基づき、端末CPUで緯度・経度から秒単位の日の出・日の入り・日長・南中進捗を完全オフライン独自計算しています。"
                    AppLanguage.SIMPLIFIED_CHINESE -> "基于美国国家海洋和大气管理局(NOAA)天文学公式，由设备端CPU依据经纬度全离线精确计算日出、日落、昼长与中天轨迹。"
                    AppLanguage.TRADITIONAL_CHINESE -> "基於美國國家海洋暨大氣總署(NOAA)天文學公式，由裝置端CPU依據經緯度全離線精確計算日出、日落、晝長與中天軌跡。"
                    AppLanguage.ENGLISH -> "Calculated entirely on-device via NOAA Solar Calculation astronomical formulas for precision sunrise, sunset, and solar arc."
                },
                icon = Icons.Rounded.WbSunny,
                isDirectJma = false
            )
        )
    }
}

