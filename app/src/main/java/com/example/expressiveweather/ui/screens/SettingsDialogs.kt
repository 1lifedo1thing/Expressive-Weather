package com.example.expressiveweather.ui.screens

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
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.WeatherCondition
import com.example.expressiveweather.theme.ThemePreset

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
                            .clickable { onSelect(item.condition, item.isDay) }
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
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                        context.startActivity(intent)
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
                            text = "GNU General Public License v3.0 (GPL-3.0)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = when (language) {
                        AppLanguage.JAPANESE -> "Expressive Weather は、ユーザーの自由を尊重するオープンソースソフトウェア（FOSS）です。\n\nGPL v3 の規定に基づき、ソースコードの閲覧、実行、改変、再配布の自由が永続的に保証されています。本ソフトウェアの改変版または派生著作物を頒布する場合も、同一の GPL v3 ライセンスに基づいてソースコードを公開する必要があります。"
                        AppLanguage.SIMPLIFIED_CHINESE -> "Expressive Weather 是一款尊重用户自由的自由与开源软件（FOSS）。\n\n根据 GPL v3 的规定，永久保障用户查看、运行、修改及重新分发源代码的自由。任何分发本软件修改版或衍生作品的行为，均须在相同的 GPL v3 许可证下公开其源代码。"
                        AppLanguage.TRADITIONAL_CHINESE -> "Expressive Weather 是一款尊重使用者自由的自由與開源軟體（FOSS）。\n\n依據 GPL v3 的規定，永久保障使用者查看、執行、修改及重新散布原始碼的自由。任何散布本軟體修改版或衍生作品的行為，均須在相同的 GPL v3 授權條款下公開其原始碼。"
                        AppLanguage.ENGLISH -> "Expressive Weather is Free and Open Source Software (FOSS).\n\nUnder the terms of the GNU General Public License v3, users have the perpetual freedom to run, study, share, and modify the software. Any modified or derivative works distributed must also be made available under the same GPL v3 license."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nemy-new/Expressive-Weather"))
                            context.startActivity(intent)
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
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("GPL v3", fontSize = 12.sp)
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


