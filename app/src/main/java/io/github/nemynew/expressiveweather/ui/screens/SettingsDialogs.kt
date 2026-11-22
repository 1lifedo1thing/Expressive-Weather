package io.github.nemynew.expressiveweather.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.BuildConfig
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.HapticCapabilitiesSnapshot
import io.github.nemynew.expressiveweather.ui.haptics.HapticDeviceProfile
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics

@Composable
fun <T> SingleChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    cancelText: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { (option, label) ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    onOptionSelected(option)
                                    onDismiss()
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun DataProviderInfoDialog(
    title: String,
    desc: String,
    futureNotice: String,
    onDismiss: () -> Unit,
    closeText: String
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Cloud,
                contentDescription = null,
                tint = colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = futureNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"))
                    context.startActivity(intent)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("open-meteo.com")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(closeText)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

@Composable
fun ClearCacheConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

private data class SimulationWeatherItem(
    val nameJa: String,
    val nameEn: String,
    val nameZhHans: String,
    val nameZhHant: String,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val icon: ImageVector
) {
    fun getName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> nameJa
        AppLanguage.SIMPLIFIED_CHINESE -> nameZhHans
        AppLanguage.TRADITIONAL_CHINESE -> nameZhHant
        AppLanguage.ENGLISH -> nameEn
    }
}

@Composable
fun WeatherSimulationDialog(
    language: AppLanguage,
    onSelect: (WeatherCondition?, Boolean?) -> Unit,
    onDismiss: () -> Unit,
    cancelText: String,
    resetText: String
) {
    val expressHaptics = LocalExpressiveHaptics.current
    val items = remember {
        listOf(
            SimulationWeatherItem("快晴 (昼)", "Clear (Day)", "晴朗 (白天)", "晴朗 (白天)", WeatherCondition.CLEAR, true, Icons.Rounded.WbSunny),
            SimulationWeatherItem("快晴 (夜)", "Clear (Night)", "晴朗 (夜间)", "晴朗 (夜間)", WeatherCondition.CLEAR, false, Icons.Rounded.NightsStay),
            SimulationWeatherItem("晴れ時々曇り", "Few Clouds", "多云", "多雲", WeatherCondition.FEW_CLOUDS, true, Icons.Rounded.CloudQueue),
            SimulationWeatherItem("曇り", "Cloudy", "阴天", "陰天", WeatherCondition.CLOUDY, true, Icons.Rounded.Cloud),
            SimulationWeatherItem("雨", "Rain", "降雨", "降雨", WeatherCondition.RAIN, true, Icons.Rounded.WaterDrop),
            SimulationWeatherItem("雷雨", "Thunderstorm", "雷阵雨", "雷陣雨", WeatherCondition.THUNDERSTORM, false, Icons.Rounded.Thunderstorm),
            SimulationWeatherItem("雪", "Snow", "降雪", "降雪", WeatherCondition.SNOW, true, Icons.Rounded.AcUnit),
            SimulationWeatherItem("霧", "Fog", "有雾", "有霧", WeatherCondition.FOG, true, Icons.Rounded.Dehaze)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "天候アニメーションのプレビュー"
                    AppLanguage.SIMPLIFIED_CHINESE -> "天气动态效果预览"
                    AppLanguage.TRADITIONAL_CHINESE -> "天氣動態效果預覽"
                    AppLanguage.ENGLISH -> "Atmosphere Simulation"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expressHaptics.playWeatherSignature(item.condition)
                                onSelect(item.condition, item.isDay)
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = item.getName(language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(null, null) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = resetText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}


@Composable
fun ThemePresetDialog(
    currentPreset: ThemePreset,
    language: AppLanguage,
    onPresetSelected: (ThemePreset) -> Unit,
    onDismiss: () -> Unit,
    cancelText: String
) {
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "テーマカラーの選択"
                        AppLanguage.SIMPLIFIED_CHINESE -> "选择主题配色"
                        AppLanguage.TRADITIONAL_CHINESE -> "選擇主題配色"
                        AppLanguage.ENGLISH -> "Select Theme Color"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "好みのカラーを選択して固定、または天候連動に設定できます"
                        AppLanguage.SIMPLIFIED_CHINESE -> "选择喜爱的调色板固定显示，或设置为根据天气自动变换"
                        AppLanguage.TRADITIONAL_CHINESE -> "選擇喜愛的調色盤固定顯示，或設定為隨天氣自動變化"
                        AppLanguage.ENGLISH -> "Choose a color palette to fix or adapt to weather"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemePreset.entries.forEach { preset ->
                    val isSelected = preset == currentPreset
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onPresetSelected(preset)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.displayName(language),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = preset.description(language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // 単一の代表色スウォッチ（天候連動時は4分割タイル）
                            if (preset == ThemePreset.WEATHER) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                                        .padding(2.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
                                        Row(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEDEDED)))
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF9E9E9E)))
                                        }
                                        Row(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF262626)))
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF5E5E5E)))
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(preset.previewPrimary)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

private data class DataSourceCreditEntry(
    val name: String,
    val category: String,
    val usage: String,
    val attribution: String,
    val url: String
)

@Composable
fun DataSourcesCreditsDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val items = remember(language) {
        listOf(
            DataSourceCreditEntry(
                name = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA)"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA)"
                    AppLanguage.ENGLISH -> "Japan Meteorological Agency (JMA)"
                },
                category = when (language) {
                    AppLanguage.JAPANESE -> "国内高精度実況・防災"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本高分辨率降水・防灾预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本高解析度降水・防災預警"
                    AppLanguage.ENGLISH -> "Japan Radar & Disaster Alerts"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "高解像度降水ナウキャスト(250m)、降水短時間予報(+15h)、キキクル(危険度分布)、気象衛星ひまわり、特別警報・警報・注意報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "高分辨率降水临近预报(250m)、短时降水预报(+15h)、危险度分布(Kikikuru)、向日葵气象卫星、气象警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "高解析度降水臨近預報(250m)、短時降水預報(+15h)、危險度分佈(Kikikuru)、向日葵氣象衛星、氣象警報"
                    AppLanguage.ENGLISH -> "High-Resolution Nowcast (250m), Precipitation Forecast (+15h), Kikikuru, Himawari Satellite, Weather Warnings"
                },
                attribution = when (language) {
                    AppLanguage.JAPANESE -> "出典：気象庁（政府標準利用規約2.0準拠）"
                    AppLanguage.SIMPLIFIED_CHINESE -> "来源：日本气象厅"
                    AppLanguage.TRADITIONAL_CHINESE -> "來源：日本氣象廳"
                    AppLanguage.ENGLISH -> "Source: Japan Meteorological Agency"
                },
                url = "https://www.jma.go.jp/"
            ),
            DataSourceCreditEntry(
                name = "Open-Meteo",
                category = when (language) {
                    AppLanguage.JAPANESE -> "全世界気象予報・大気質"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球天气预报・空气质量"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球天氣預報・空氣品質"
                    AppLanguage.ENGLISH -> "Global Forecast & Air Quality"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "週間・時間別天気予報、気温、降水量、湿度、風向風速、気圧、紫外線指数(UV)、大気質指数(AQI)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "逐小时与周天气预报、气温、降水、湿度、风力、气压、紫外线(UV)、空气质量(AQI)"
                    AppLanguage.TRADITIONAL_CHINESE -> "逐小時與週天氣預報、氣溫、降水、濕度、風力、氣壓、紫外線(UV)、空氣品質(AQI)"
                    AppLanguage.ENGLISH -> "Weekly & Hourly Forecast, Temperature, Precipitation, Humidity, Wind, Pressure, UV, AQI"
                },
                attribution = "Weather data by Open-Meteo.com (CC BY 4.0)",
                url = "https://open-meteo.com/"
            ),
            DataSourceCreditEntry(
                name = "RainViewer",
                category = when (language) {
                    AppLanguage.JAPANESE -> "世界複合気象レーダー"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球复合气象雷达"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球複合氣象雷達"
                    AppLanguage.ENGLISH -> "Global Radar Network"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "世界各国の気象レーダー観測網を統合したシームレスな実況レーダータイルおよびタイムライン"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球多国气象雷达整合实况切片与时间轴"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球多國氣象雷達整合實況圖磚與時間軸"
                    AppLanguage.ENGLISH -> "Composite global weather radar observation tiles and timeline"
                },
                attribution = "Weather data provided by RainViewer",
                url = "https://www.rainviewer.com/api.html"
            ),
            DataSourceCreditEntry(
                name = "Photon / OpenStreetMap",
                category = when (language) {
                    AppLanguage.JAPANESE -> "都市検索・ジオコーディング"
                    AppLanguage.SIMPLIFIED_CHINESE -> "城市搜索・地理编码"
                    AppLanguage.TRADITIONAL_CHINESE -> "城市搜尋・地理編碼"
                    AppLanguage.ENGLISH -> "Geocoding & Location Search"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "全世界の都市・地名検索およびGPS座標からの逆ジオコーディング"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球城市与地点检索及经纬度反向地理编码"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球城市與地點檢索及經緯度反向地理編碼"
                    AppLanguage.ENGLISH -> "Global city search and reverse geocoding from coordinates"
                },
                attribution = "© OpenStreetMap contributors (ODbL) / Photon",
                url = "https://www.openstreetmap.org/copyright"
            ),
            DataSourceCreditEntry(
                name = "NOAA / National Weather Service",
                category = when (language) {
                    AppLanguage.JAPANESE -> "全米レーダー & HRRR予報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全美雷达与 HRRR 预报"
                    AppLanguage.TRADITIONAL_CHINESE -> "全美雷達與 HRRR 預報"
                    AppLanguage.ENGLISH -> "US Radar & HRRR Forecast"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "全米NEXRAD地上反射強度実況、MRMSモザイク、HRRR数値予報シミュレーション(+18h)、NWS気象警報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全美 NEXRAD 地表反射强度、MRMS 复合切片、HRRR 数值预报(+18h)、NWS 警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "全美 NEXRAD 地表反射強度、MRMS 複合圖磚、HRRR 數值預報(+18h)、NWS 警報"
                    AppLanguage.ENGLISH -> "US NEXRAD Reflectivity, MRMS Composite, HRRR Simulation (+18h), NWS Weather Alerts"
                },
                attribution = "Data provided by NOAA / National Weather Service (Public Domain)",
                url = "https://www.weather.gov/"
            ),
            DataSourceCreditEntry(
                name = "GDACS",
                category = when (language) {
                    AppLanguage.JAPANESE -> "国際自然災害アラート"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球自然灾害预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球自然災害預警"
                    AppLanguage.ENGLISH -> "Global Disaster Alerts"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "熱帯低気圧（台風・ハリケーン）、地震、津波、火山活動、洪水警報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "热带气旋、地震、海啸、火山爆发与洪水警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "熱帶氣旋、地震、海嘯、火山爆發與洪水警報"
                    AppLanguage.ENGLISH -> "Tropical Cyclones, Earthquakes, Tsunamis, Volcanic Eruptions, Floods"
                },
                attribution = "Disaster alerts provided by GDACS (UN & European Commission)",
                url = "https://www.gdacs.org/"
            ),
            DataSourceCreditEntry(
                name = "WMO",
                category = when (language) {
                    AppLanguage.JAPANESE -> "世界気象機関公式警報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "世界气象组织官方预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "世界氣象組織官方預警"
                    AppLanguage.ENGLISH -> "Official Weather Warnings"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "SWIC（Severe Weather Information Centre）を通じた各国気象当局の公式警報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "通过 SWIC 汇聚的各国气象部门官方权威警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "透過 SWIC 彙聚的各國氣象部門官方權威警報"
                    AppLanguage.ENGLISH -> "Official severe weather warnings from National Meteorological Services worldwide"
                },
                attribution = "Official weather warnings provided by WMO",
                url = "https://severeweather.wmo.int/"
            ),
            DataSourceCreditEntry(
                name = "MeteoAlarm (EUMETNET)",
                category = when (language) {
                    AppLanguage.JAPANESE -> "欧州公式気象警報"
                    AppLanguage.SIMPLIFIED_CHINESE -> "欧洲官方气象预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "歐洲官方氣象警報"
                    AppLanguage.ENGLISH -> "European Weather Warnings"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "欧州39カ国・地域の公式気象警報（CAP / Atomフィード）"
                    AppLanguage.SIMPLIFIED_CHINESE -> "欧洲39个国家与地区的官方气象警报（CAP / Atom）"
                    AppLanguage.TRADITIONAL_CHINESE -> "歐洲39個國家與地區的官方氣象警報（CAP / Atom）"
                    AppLanguage.ENGLISH -> "Official weather warnings across 39 European countries (CAP / Atom)"
                },
                attribution = "Alerts provided by MeteoAlarm / EUMETNET",
                url = "https://meteoalarm.org/"
            ),
            DataSourceCreditEntry(
                name = "Iowa Environmental Mesonet (IEM)",
                category = when (language) {
                    AppLanguage.JAPANESE -> "全米レーダータイル配信"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全美雷达切片服务"
                    AppLanguage.TRADITIONAL_CHINESE -> "全美雷達圖磚服務"
                    AppLanguage.ENGLISH -> "US Radar Tile Service"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "NEXRAD / MRMS 合成レーダーおよび HRRR 数値予報タイル配信"
                    AppLanguage.SIMPLIFIED_CHINESE -> "NEXRAD / MRMS 合成雷达与 HRRR 数值预报切片"
                    AppLanguage.TRADITIONAL_CHINESE -> "NEXRAD / MRMS 合成雷達與 HRRR 數值預報圖磚"
                    AppLanguage.ENGLISH -> "NEXRAD / MRMS composite radar and HRRR model map tiles"
                },
                attribution = "Tiles hosted by Iowa Environmental Mesonet (Iowa State University)",
                url = "https://mesonet.agron.iastate.edu/"
            ),
            DataSourceCreditEntry(
                name = when (language) {
                    AppLanguage.JAPANESE -> "国土地理院 (GSI)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本国土地理院 (GSI)"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本國土地理院 (GSI)"
                    AppLanguage.ENGLISH -> "Geospatial Information Authority of Japan (GSI)"
                },
                category = when (language) {
                    AppLanguage.JAPANESE -> "背景地図・ハザードマップ"
                    AppLanguage.SIMPLIFIED_CHINESE -> "底图与灾害地图"
                    AppLanguage.TRADITIONAL_CHINESE -> "底圖與災害地圖"
                    AppLanguage.ENGLISH -> "Base Maps & Hazard Overlays"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "日本域ウィジェット背景地図（淡色・白地図）および浸水想定区域オーバーレイ"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本地区小组件底图及洪水风险图层"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本地區小工具底圖及淹水潛勢圖層"
                    AppLanguage.ENGLISH -> "Japan widget base tiles (pale/blank) and flood hazard overlays"
                },
                attribution = when (language) {
                    AppLanguage.JAPANESE -> "出典：国土地理院（地理院タイル）"
                    AppLanguage.SIMPLIFIED_CHINESE -> "来源：日本国土地理院（地理院地图）"
                    AppLanguage.TRADITIONAL_CHINESE -> "來源：日本國土地理院（地理院圖磚）"
                    AppLanguage.ENGLISH -> "Source: Geospatial Information Authority of Japan (GSI Tiles)"
                },
                url = "https://maps.gsi.go.jp/development/ichiran.html"
            ),
            DataSourceCreditEntry(
                name = "Google Maps Platform",
                category = when (language) {
                    AppLanguage.JAPANESE -> "インタラクティブ地図"
                    AppLanguage.SIMPLIFIED_CHINESE -> "交互式地图"
                    AppLanguage.TRADITIONAL_CHINESE -> "互動式地圖"
                    AppLanguage.ENGLISH -> "Interactive Maps"
                },
                usage = when (language) {
                    AppLanguage.JAPANESE -> "雨雲レーダー・キキクル・台風進路表示用のベースマップ描画"
                    AppLanguage.SIMPLIFIED_CHINESE -> "用于降水雷达、危险度分布及台风路径的交互底图"
                    AppLanguage.TRADITIONAL_CHINESE -> "用於降雨雷達、危險度分佈及颱風路徑的互動底圖"
                    AppLanguage.ENGLISH -> "Interactive base map rendering for weather radar, Kikikuru, and typhoon tracks"
                },
                attribution = "Map data © Google",
                url = "https://www.google.com/help/terms_maps/"
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Public,
                contentDescription = null,
                tint = colorScheme.primary
            )
        },
        title = {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "データソースと帰属表記"
                    AppLanguage.SIMPLIFIED_CHINESE -> "数据来源与致谢"
                    AppLanguage.TRADITIONAL_CHINESE -> "資料來源與致謝"
                    AppLanguage.ENGLISH -> "Data Sources & Credits"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSecondaryContainer,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.usage,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.attribution,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (language) {
                                            AppLanguage.JAPANESE -> "公式"
                                            AppLanguage.SIMPLIFIED_CHINESE -> "官网"
                                            AppLanguage.TRADITIONAL_CHINESE -> "官網"
                                            AppLanguage.ENGLISH -> "Website"
                                        },
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (language) {
                            AppLanguage.JAPANESE -> "※本アプリは各気象機関および公的機関の公式アプリではありません。提供される情報は教育・参考用であり、緊急時は自治体や気象庁等の公式発表をご確認ください。"
                            AppLanguage.SIMPLIFIED_CHINESE -> "※本应用非各气象部门或官方机构的官方应用。所有信息仅供参考，紧急情况下请务必以官方权威发布为准。"
                            AppLanguage.TRADITIONAL_CHINESE -> "※本應用程式非各氣象部門或官方機構的官方應用程式。所有資訊僅供參考，緊急情況下請務必以官方權威發布為準。"
                            AppLanguage.ENGLISH -> "Note: This is an unofficial application and not affiliated with any official meteorological agency. In emergencies, please consult official government announcements."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onTertiaryContainer,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "閉じる"
                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                        AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                        AppLanguage.ENGLISH -> "Close"
                    }
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

@Composable
fun OpenSourceLicenseDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Policy,
                contentDescription = null,
                tint = colorScheme.primary
            )
        },
        title = {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "オープンソースライセンス"
                    AppLanguage.SIMPLIFIED_CHINESE -> "开源许可证"
                    AppLanguage.TRADITIONAL_CHINESE -> "開源許可證"
                    AppLanguage.ENGLISH -> "Open Source License"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apache License 2.0",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather は Apache License 2.0 の下で公開されているオープンソースソフトウェアです。本ソフトウェアは「現状有姿（AS IS）」で提供され、明示・黙示を問わず一切の保証を行いません。"
                        AppLanguage.SIMPLIFIED_CHINESE -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather 基于 Apache License 2.0 开源协议发布。本软件按「原样（AS IS）」提供，不提供任何明示或暗示的担保。"
                        AppLanguage.TRADITIONAL_CHINESE -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather 依據 Apache License 2.0 開源授權條款發布。本軟體依「現況（AS IS）」提供，不提供任何明示或默示擔保。"
                        AppLanguage.ENGLISH -> "Copyright © Expressive Weather Contributors\n\nExpressive Weather is open-source software licensed under the Apache License, Version 2.0. This software is provided \"AS IS\", WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (language) {
                                AppLanguage.JAPANESE -> "サードパーティ OSS・アセットライセンス"
                                AppLanguage.SIMPLIFIED_CHINESE -> "第三方开源组件与资源许可"
                                AppLanguage.TRADITIONAL_CHINESE -> "第三方開源元件與資源授權"
                                AppLanguage.ENGLISH -> "Third-Party OSS & Asset Licenses"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        val thirdPartyLines = listOf(
                            "• Google Sans Flex Font — SIL Open Font License 1.1 (Copyright Google LLC)",
                            "• Fluent System Icons — MIT License (Copyright Microsoft Corp.)",
                            "• AndroidX / Jetpack Compose / Room / WorkManager / Navigation3 — Apache-2.0",
                            "• Kotlin / Coroutines / kotlinx.serialization — Apache-2.0",
                            "• OkHttp — Apache-2.0 (Square, Inc.)",
                            "• Haze — Apache-2.0 (Chris Banes)",
                            "• Google Play Services Maps SDK — Google APIs Terms of Service"
                        )
                        thirdPartyLines.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nemy-new/Expressive-Weather"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GitHub", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.apache.org/licenses/LICENSE-2.0"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Apache 2.0", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (BuildConfig.ENABLE_EXTERNAL_DONATION_LINKS) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/nemynew"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorScheme.secondaryContainer,
                            contentColor = colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalCafe,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (language) {
                                AppLanguage.JAPANESE -> "Buy Me a Coffee で支援する"
                                AppLanguage.SIMPLIFIED_CHINESE -> "在 Buy Me a Coffee 支持作者"
                                AppLanguage.TRADITIONAL_CHINESE -> "在 Buy Me a Coffee 支持作者"
                                AppLanguage.ENGLISH -> "Buy Me a Coffee"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "閉じる"
                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                        AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                        AppLanguage.ENGLISH -> "Close"
                    }
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

/**
 * Google Play バックグラウンド位置情報ポリシー準拠の顕著な開示（Prominent Disclosure）ダイアログ
 */
@Composable
fun BackgroundLocationDisclosureDialog(
    language: AppLanguage,
    onConfirmProceed: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = colorScheme.primary
            )
        },
        title = {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "バックグラウンド位置情報の利用について"
                    AppLanguage.SIMPLIFIED_CHINESE -> "关于后台位置信息的使用说明"
                    AppLanguage.TRADITIONAL_CHINESE -> "關於後台位置資訊的使用說明"
                    AppLanguage.ENGLISH -> "Background Location Disclosure"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "Expressive Weather は、アプリを閉じている間や使用していないときにも位置情報データを収集し、以下の機能を有効にします："
                        AppLanguage.SIMPLIFIED_CHINESE -> "Expressive Weather 会在应用关闭或未使用时收集位置数据，以支持以下功能："
                        AppLanguage.TRADITIONAL_CHINESE -> "Expressive Weather 會在應用程式關閉或未使用時收集位置資料，以支援以下功能："
                        AppLanguage.ENGLISH -> "Expressive Weather collects location data to enable the following features even when the app is closed or not in use:"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val bullets = when (language) {
                            AppLanguage.JAPANESE -> listOf(
                                "• 移動先に応じた気象警報・特別警報・キキクル（危険度分布）の自動監視と通知",
                                "• 雨雲の接近・降り止みリアルタイムカウントダウン通知の現在地追従",
                                "• ホーム画面ウィジェットおよび天気予報のバックグラウンド自動更新"
                            )
                            AppLanguage.SIMPLIFIED_CHINESE -> listOf(
                                "• 根据当前位置变化自动监测并推送灾害预警与危险度分布",
                                "• 降雨临近与雨停倒计时实时通知的位置跟随",
                                "• 主屏幕小组件与天气预报的后台自动刷新"
                            )
                            AppLanguage.TRADITIONAL_CHINESE -> listOf(
                                "• 依據目前位置變化自動監測並推播災害警報與危險度分佈",
                                "• 降雨接近與雨停倒數即時通知的位置追蹤",
                                "• 主畫面小工具與天氣預報的後台自動更新"
                            )
                            AppLanguage.ENGLISH -> listOf(
                                "• Automatic severe weather & disaster alert monitoring as you travel",
                                "• Live precipitation approach/stop countdown notifications following your location",
                                "• Background weather and home-screen widget updates for your current area"
                            )
                        }
                        bullets.forEach { bullet ->
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "位置座標は天気・警報データ取得のためのみに各気象機関・天気APIへ送信され、広告配信や第三者への販売には一切使用されません。許可は任意であり、Androidの設定からいつでも変更・撤回できます。"
                        AppLanguage.SIMPLIFIED_CHINESE -> "位置坐标仅用于向气象数据服务请求天气与预警信息，绝不用于广告或出售给第三方。该权限为可选项，您可随时在系统设置中更改或撤销。"
                        AppLanguage.TRADITIONAL_CHINESE -> "位置座標僅用於向氣象資料服務請求天氣與警報資訊，絕不用於廣告或出售給第三方。此權限為可選項，您可隨時在系統設定中變更或撤銷。"
                        AppLanguage.ENGLISH -> "Your coordinates are sent solely to weather and alert providers to fetch local forecasts and warnings. They are never sold or used for advertising. This permission is optional and can be revoked at any time in Android Settings."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onConfirmProceed) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "同意して設定を開く"
                        AppLanguage.SIMPLIFIED_CHINESE -> "同意并前往设置"
                        AppLanguage.TRADITIONAL_CHINESE -> "同意並前往設定"
                        AppLanguage.ENGLISH -> "Continue to Settings"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "キャンセル"
                        AppLanguage.SIMPLIFIED_CHINESE -> "取消"
                        AppLanguage.TRADITIONAL_CHINESE -> "取消"
                        AppLanguage.ENGLISH -> "Not Now"
                    }
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

/**
 * プライバシーポリシー要約＆全文リンクダイアログ
 */
@Composable
fun PrivacyPolicyDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val privacyPolicyUrl = "https://github.com/nemy-new/Expressive-Weather/blob/main/PRIVACY_POLICY.md"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.PrivacyTip,
                contentDescription = null,
                tint = colorScheme.primary
            )
        },
        title = {
            Text(
                text = when (language) {
                    AppLanguage.JAPANESE -> "プライバシーポリシー"
                    AppLanguage.SIMPLIFIED_CHINESE -> "隐私政策"
                    AppLanguage.TRADITIONAL_CHINESE -> "隱私權政策"
                    AppLanguage.ENGLISH -> "Privacy Policy"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "Expressive Weather は開発者独自の収集サーバーを持たず、アカウント登録や広告トラッキングも行いません。"
                        AppLanguage.SIMPLIFIED_CHINESE -> "Expressive Weather 不运营开发者自建收集服务器，不要求账号注册，也不进行广告追踪。"
                        AppLanguage.TRADITIONAL_CHINESE -> "Expressive Weather 不營運開發者自建收集伺服器，不要求帳號註冊，也不進行廣告追蹤。"
                        AppLanguage.ENGLISH -> "Expressive Weather operates no developer-owned analytics or tracking servers, requires no account, and displays no ads."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val items = when (language) {
                            AppLanguage.JAPANESE -> listOf(
                                "• 位置情報（任意）：天気予報・降水ナウキャスト・災害警報・ジオコーディングの取得のみに使用。許可した場合のみバックグラウンドでも更新されます。",
                                "• 検索クエリ：都市検索のため Photon (OpenStreetMap) API へ送信されます。",
                                "• 外部通信先：Open-Meteo、気象庁(JMA)、国土地理院(GSI)、NOAA/NWS、IEM、MeteoAlarm、WMO、GDACS、RainViewer、Photon、Google Maps SDK。",
                                "• 端末内保存：保存都市・天気キャッシュ・設定のみを端末内に保存します（アプリ削除またはキャッシュ消去で削除可能）。"
                            )
                            AppLanguage.SIMPLIFIED_CHINESE -> listOf(
                                "• 位置信息（可选）：仅用于获取天气预报、降水临近预报、灾害预警与地理编码。仅在您授权后于后台更新。",
                                "• 搜索词：用于通过 Photon (OpenStreetMap) 检索城市名称。",
                                "• 第三方接口：Open-Meteo、JMA、GSI、NOAA/NWS、IEM、MeteoAlarm、WMO、GDACS、RainViewer、Photon、Google Maps SDK。",
                                "• 本地存储：仅在设备本地保存收藏城市、天气缓存与偏好设置。"
                            )
                            AppLanguage.TRADITIONAL_CHINESE -> listOf(
                                "• 位置資訊（可選）：僅用於取得天氣預報、降水臨近預報、災害警報與地理編碼。僅在您授權後於後台更新。",
                                "• 搜尋詞：用於透過 Photon (OpenStreetMap) 檢索城市名稱。",
                                "• 第三方介面：Open-Meteo、JMA、GSI、NOAA/NWS、IEM、MeteoAlarm、WMO、GDACS、RainViewer、Photon、Google Maps SDK。",
                                "• 本機儲存：僅在裝置本機儲存收藏城市、天氣快取與偏好設定。"
                            )
                            AppLanguage.ENGLISH -> listOf(
                                "• Location (Optional): Used solely to fetch forecasts, nowcasts, disaster alerts, and reverse geocoding. Background access occurs only if explicitly enabled by you.",
                                "• Search Queries: Sent to Photon (OpenStreetMap) for city lookup.",
                                "• Third-Party Providers: Open-Meteo, JMA, GSI, NOAA/NWS, IEM, MeteoAlarm, WMO, GDACS, RainViewer, Photon, and Google Maps SDK.",
                                "• On-Device Storage: Saved cities, weather caches, and preferences stay locally on your device."
                            )
                        }
                        items.forEach { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (language) {
                            AppLanguage.JAPANESE -> "プライバシーポリシー全文を開く"
                            AppLanguage.SIMPLIFIED_CHINESE -> "查看完整隐私政策"
                            AppLanguage.TRADITIONAL_CHINESE -> "查看完整隱私權政策"
                            AppLanguage.ENGLISH -> "Read Full Privacy Policy"
                        },
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "閉じる"
                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                        AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                        AppLanguage.ENGLISH -> "Close"
                    }
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}

private data class HapticPreviewItem(
    val pattern: ExpressiveHapticPattern,
    val labelJa: String,
    val labelEn: String,
    val labelZhHans: String,
    val labelZhHant: String,
    val icon: ImageVector
) {
    fun label(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> labelJa
        AppLanguage.SIMPLIFIED_CHINESE -> labelZhHans
        AppLanguage.TRADITIONAL_CHINESE -> labelZhHant
        AppLanguage.ENGLISH -> labelEn
    }
}

/**
 * 端末別ハプティクスプロファイル診断 & 全パターン体験ダイアログ
 */
@Composable
fun HapticShowcaseDialog(
    language: AppLanguage,
    currentIntensity: io.github.nemynew.expressiveweather.data.HapticIntensity,
    capabilities: HapticCapabilitiesSnapshot?,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val expressHaptics = LocalExpressiveHaptics.current

    val uiPatterns = remember {
        listOf(
            HapticPreviewItem(
                ExpressiveHapticPattern.EXPRESSIVE_BOUNCE,
                "弾性タップ", "Bouncy Tap", "弹性点击", "彈性點擊",
                Icons.Rounded.TouchApp
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.SWITCH_ON,
                "スイッチ ON", "Switch Snap", "开关切换", "開關切換",
                Icons.Rounded.ToggleOn
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.DETENT_BOUNDARY,
                "端点デテント", "Detent Edge", "边界刻度", "邊界刻度",
                Icons.Rounded.LinearScale
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.DRAG_PICKUP,
                "ドラッグ掴み", "Grab & Lift", "抓取抬起", "抓取抬起",
                Icons.Rounded.DragIndicator
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.REFRESH_SUCCESS,
                "更新完了", "Sync Done", "同步完成", "同步完成",
                Icons.Rounded.CheckCircle
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.CELEBRATE_UNLOCK,
                "達成・解除", "Celebrate", "解锁庆祝", "解鎖慶祝",
                Icons.Rounded.AutoAwesome
            )
        )
    }

    val weatherPatterns = remember {
        listOf(
            HapticPreviewItem(
                ExpressiveHapticPattern.WEATHER_RAINDROP_PATTER,
                "雨粒パター", "Raindrop Patter", "雨滴轻敲", "雨滴輕敲",
                Icons.Rounded.WaterDrop
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.WEATHER_THUNDER_RUMBLE,
                "雷鳴ランブル", "Thunder Rumble", "雷鸣轰隆", "雷鳴轟隆",
                Icons.Rounded.Thunderstorm
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.WEATHER_SNOW_CRISP,
                "粉雪クリスプ", "Crisp Snow", "降雪轻触", "降雪輕觸",
                Icons.Rounded.AcUnit
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.WEATHER_WIND_GUST,
                "強風ガスト", "Wind Gust", "阵风拂动", "陣風拂動",
                Icons.Rounded.Air
            ),
            HapticPreviewItem(
                ExpressiveHapticPattern.ALERT_CRITICAL_PULSE,
                "緊急警報パルス", "Critical Alert", "紧急警报脉冲", "緊急警報脈衝",
                Icons.Rounded.Warning
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Vibration,
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "ハプティクス診断 & 体験"
                        AppLanguage.SIMPLIFIED_CHINESE -> "触感诊断与体验"
                        AppLanguage.TRADITIONAL_CHINESE -> "觸感診斷與體驗"
                        AppLanguage.ENGLISH -> "Haptics Studio & Profile"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val profileLabel = capabilities?.profile?.getLabel(language)
                            ?: HapticDeviceProfile.LEGACY_TIMED_ERM.getLabel(language)
                        Text(
                            text = profileLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        if (capabilities != null) {
                            Text(
                                text = "${capabilities.manufacturer} ${capabilities.model} • API ${capabilities.sdkInt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            val yes = when (language) {
                                AppLanguage.JAPANESE -> "対応"
                                AppLanguage.SIMPLIFIED_CHINESE -> "支持"
                                AppLanguage.TRADITIONAL_CHINESE -> "支援"
                                AppLanguage.ENGLISH -> "Yes"
                            }
                            val no = when (language) {
                                AppLanguage.JAPANESE -> "非対応"
                                AppLanguage.SIMPLIFIED_CHINESE -> "不支持"
                                AppLanguage.TRADITIONAL_CHINESE -> "不支援"
                                AppLanguage.ENGLISH -> "No"
                            }
                            Text(
                                text = when (language) {
                                    AppLanguage.JAPANESE -> "振幅制御: ${if (capabilities.hasAmplitudeControl) yes else no} / プリミティブ数: ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.SIMPLIFIED_CHINESE -> "振幅控制: ${if (capabilities.hasAmplitudeControl) yes else no} / 原语数: ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.TRADITIONAL_CHINESE -> "振幅控制: ${if (capabilities.hasAmplitudeControl) yes else no} / 原語數: ${capabilities.supportedPrimitivesCount}"
                                    AppLanguage.ENGLISH -> "Amplitude: ${if (capabilities.hasAmplitudeControl) yes else no} • Primitives: ${capabilities.supportedPrimitivesCount}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = when (language) {
                                AppLanguage.JAPANESE -> "現在の強度: ${currentIntensity.getLabel(language)}"
                                AppLanguage.SIMPLIFIED_CHINESE -> "当前强度: ${currentIntensity.getLabel(language)}"
                                AppLanguage.TRADITIONAL_CHINESE -> "目前強度: ${currentIntensity.getLabel(language)}"
                                AppLanguage.ENGLISH -> "Active Intensity: ${currentIntensity.getLabel(language)}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (currentIntensity == io.github.nemynew.expressiveweather.data.HapticIntensity.OFF) {
                                colorScheme.error
                            } else {
                                colorScheme.onSurface
                            }
                        )
                    }
                }

                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "UI インタラクション"
                        AppLanguage.SIMPLIFIED_CHINESE -> "UI 交互触感"
                        AppLanguage.TRADITIONAL_CHINESE -> "UI 互動觸感"
                        AppLanguage.ENGLISH -> "UI Interactions"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )

                uiPatterns.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            FilledTonalButton(
                                onClick = { expressHaptics.play(item.pattern, bypassThrottle = true) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label(language),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "天候・防災シグネチャ"
                        AppLanguage.SIMPLIFIED_CHINESE -> "天气与灾害警报特征"
                        AppLanguage.TRADITIONAL_CHINESE -> "天氣與災害警報特徵"
                        AppLanguage.ENGLISH -> "Weather & Alert Signatures"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )

                weatherPatterns.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            OutlinedButton(
                                onClick = { expressHaptics.play(item.pattern, bypassThrottle = true) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (item.pattern == ExpressiveHapticPattern.ALERT_CRITICAL_PULSE) {
                                        colorScheme.error
                                    } else {
                                        colorScheme.primary
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label(language),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (language) {
                        AppLanguage.JAPANESE -> "閉じる"
                        AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                        AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                        AppLanguage.ENGLISH -> "Close"
                    }
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}


