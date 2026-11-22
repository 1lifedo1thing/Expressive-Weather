package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AlertSource
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.GlobalAlertItem
import io.github.nemynew.expressiveweather.data.JmaWarningMaster
import io.github.nemynew.expressiveweather.data.MunicipalityAlertInfo
import io.github.nemynew.expressiveweather.data.MunicipalityWarningItem
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import io.github.nemynew.expressiveweather.ui.components.DetailScaffold
import io.github.nemynew.expressiveweather.ui.components.GdacsIndividualAlertCard
import io.github.nemynew.expressiveweather.ui.components.JmaIndividualWarningCard
import io.github.nemynew.expressiveweather.ui.components.NwsIndividualAlertCard
import io.github.nemynew.expressiveweather.ui.components.MeteoAlarmIndividualAlertCard
import io.github.nemynew.expressiveweather.ui.components.WeatherAlertColors
import io.github.nemynew.expressiveweather.ui.components.WeatherWarningType
import io.github.nemynew.expressiveweather.ui.components.WmoIndividualAlertCard
import io.github.nemynew.expressiveweather.ui.components.getJmaOverviewLabel

/**
 * 全気象警報・防災情報専用のフルスクリーン詳細画面。
 *
 * 設計原則:
 * 1. DetailScaffold による一貫したプレミアムデザイン、フロストトップバー、ダイナミック大気キャンバス
 * 2. 全ての警報ソース（気象庁 JMA, 米国国立気象局 NWS, 世界気象機関 WMO/MeteoAlarm, 全球災害警報 GDACS）の
 *    現象・想定される被害、推奨される避難行動、警戒ポイント、詳細解説をすべて完全網羅して表示
 * 3. Android 14/15 予測型「戻る」アニメーション（Predictive Back）にネイティブ対応
 */
@Composable
fun AlertDetailScreen(
    weather: WeatherData?,
    city: City,
    language: AppLanguage,
    weatherAdaptiveThemeEnabled: Boolean = true,
    themePreset: ThemePreset = ThemePreset.WEATHER,
    themeMode: ThemeMode = ThemeMode.WEATHER,
    alertInfo: MunicipalityAlertInfo? = null,
    jmaWarnings: List<String> = emptyList(),
    jmaOverviewText: String? = null,
    globalAlerts: List<GlobalAlertItem> = emptyList(),
    initialSource: AlertSource? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenTitle = when (language) {
        AppLanguage.JAPANESE -> "気象警報・防災情報"
        AppLanguage.ENGLISH -> "Weather Alerts & Advisories"
        AppLanguage.SIMPLIFIED_CHINESE -> "气象预警与防灾信息"
        AppLanguage.TRADITIONAL_CHINESE -> "氣象警報與防災資訊"
    }

    // 1. 気象庁 (JMA) の有効な警報一覧
    val activeJmaList = alertInfo?.activeWarnings.orEmpty().filter {
        it.status != "解除" && !it.status.contains("解除") && !it.name.contains("解除") && !it.name.contains("Cancel")
    }
    val effectiveJmaList = if (activeJmaList.isNotEmpty()) {
        activeJmaList
    } else if (jmaWarnings.isNotEmpty()) {
        jmaWarnings.filter { !it.contains("解除") && !it.contains("Cancel") }.map { name ->
            MunicipalityWarningItem(
                code = "99",
                name = name,
                status = "発表",
                isWarning = name.contains("警報"),
                isSpecial = name.contains("特別")
            )
        }
    } else {
        emptyList()
    }
    val sortedJmaList = remember(effectiveJmaList) {
        JmaWarningMaster.sortWarningsByPriority(effectiveJmaList)
    }

    // 2. 米国国立気象局 (NWS)
    val nwsAlerts = remember(globalAlerts) {
        globalAlerts.filter { it.source == AlertSource.NWS }.sortedBy { it.severity.ordinal }
    }

    // 3. 欧州気象警報ネットワーク (MeteoAlarm / EUMETNET)
    val meteoAlarmAlerts = remember(globalAlerts) {
        globalAlerts.filter { it.source == AlertSource.METEOALARM }.sortedBy { it.severity.ordinal }
    }

    // 4. 世界気象機関 (WMO)
    val wmoAlerts = remember(globalAlerts) {
        globalAlerts.filter { it.source == AlertSource.WMO }.sortedBy { it.severity.ordinal }
    }

    // 5. 全球災害警報 (GDACS)
    val gdacsAlerts = remember(globalAlerts) {
        globalAlerts.filter { it.source == AlertSource.GDACS }.sortedBy { it.severity.ordinal }
    }

    val hasAnyAlerts = sortedJmaList.isNotEmpty() || nwsAlerts.isNotEmpty() || meteoAlarmAlerts.isNotEmpty() || wmoAlerts.isNotEmpty() || gdacsAlerts.isNotEmpty()

    DetailScaffold(
        title = screenTitle,
        city = city,
        weather = weather,
        language = language,
        weatherAdaptiveThemeEnabled = weatherAdaptiveThemeEnabled,
        themePreset = themePreset,
        themeMode = themeMode,
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasAnyAlerts) {
                // 警報なし時のプレースホルダー
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (language) {
                                AppLanguage.JAPANESE -> "現在、警報・注意報は発表されていません"
                                AppLanguage.ENGLISH -> "No active weather alerts at this time"
                                AppLanguage.SIMPLIFIED_CHINESE -> "当前无生效中的气象预警"
                                AppLanguage.TRADITIONAL_CHINESE -> "目前無生效中的氣象警報"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (language) {
                                AppLanguage.JAPANESE -> "気象庁および海外気象機関による最新の気象警報状況を常時監視しています。"
                                AppLanguage.ENGLISH -> "Continuously monitoring official alerts from JMA, NOAA/NWS, WMO, and GDACS."
                                AppLanguage.SIMPLIFIED_CHINESE -> "持续监测来自气象厅及全球各大气象机构的最新官方预警。"
                                AppLanguage.TRADITIONAL_CHINESE -> "持續監測來自氣象廳及全球各大氣象機構的最新官方警報。"
                            },
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ================= 1. 気象庁 (JMA) セクション =================
            if (sortedJmaList.isNotEmpty()) {
                val jmaSourceTitle = when (language) {
                    AppLanguage.JAPANESE -> "気象庁 防災気象情報"
                    AppLanguage.ENGLISH -> "Japan Meteorological Agency Alerts"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 防灾气象警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 防災氣象警報"
                }
                val jmaSourceBadge = "気象庁 (JMA)"

                AlertSourceSectionHeader(
                    title = jmaSourceTitle,
                    sourceBadge = jmaSourceBadge
                )

                // 府県気象情報・概況解説文
                if (!jmaOverviewText.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = getJmaOverviewLabel(language),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = jmaOverviewText.trim(),
                                fontSize = 13.5.sp,
                                lineHeight = 19.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // JMA 各警報カード（詳細を展開した状態で表示）
                sortedJmaList.forEach { item ->
                    JmaIndividualWarningCard(
                        item = item,
                        language = language,
                        initiallyExpanded = true
                    )
                }
            }

            // ================= 2. 米国国立気象局 (NWS) セクション =================
            if (nwsAlerts.isNotEmpty()) {
                val nwsSourceTitle = when (language) {
                    AppLanguage.JAPANESE -> "米国国立気象局 気象警報"
                    AppLanguage.ENGLISH -> "US National Weather Service Alerts"
                    AppLanguage.SIMPLIFIED_CHINESE -> "美国国家气象局 气象警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "美國國家氣象局 氣象警報"
                }
                val nwsSourceBadge = "NOAA / NWS"

                AlertSourceSectionHeader(
                    title = nwsSourceTitle,
                    sourceBadge = nwsSourceBadge
                )

                nwsAlerts.forEach { alert ->
                    NwsIndividualAlertCard(
                        alert = alert,
                        language = language,
                        initiallyExpanded = true
                    )
                }
            }

            // ================= 3. 欧州気象警報ネットワーク (MeteoAlarm) セクション =================
            if (meteoAlarmAlerts.isNotEmpty()) {
                val meteoAlarmSourceTitle = when (language) {
                    AppLanguage.JAPANESE -> "欧州気象警報ネットワーク"
                    AppLanguage.ENGLISH -> "European Weather Warnings (MeteoAlarm)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "欧洲气象警报网络 (MeteoAlarm)"
                    AppLanguage.TRADITIONAL_CHINESE -> "歐洲氣象警報網路 (MeteoAlarm)"
                }
                val meteoAlarmSourceBadge = "EUMETNET / MeteoAlarm"

                AlertSourceSectionHeader(
                    title = meteoAlarmSourceTitle,
                    sourceBadge = meteoAlarmSourceBadge
                )

                meteoAlarmAlerts.forEach { alert ->
                    MeteoAlarmIndividualAlertCard(
                        alert = alert,
                        language = language,
                        initiallyExpanded = true
                    )
                }
            }

            // ================= 4. 世界気象機関 (WMO) セクション =================
            if (wmoAlerts.isNotEmpty()) {
                val wmoSourceTitle = when (language) {
                    AppLanguage.JAPANESE -> "世界気象機関 国際気象警報"
                    AppLanguage.ENGLISH -> "World Meteorological Organization Alerts"
                    AppLanguage.SIMPLIFIED_CHINESE -> "世界气象组织 国际官方气象警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "世界氣象組織 國際官方氣象警報"
                }
                val wmoSourceBadge = "WMO (SWIC)"

                AlertSourceSectionHeader(
                    title = wmoSourceTitle,
                    sourceBadge = wmoSourceBadge
                )

                wmoAlerts.forEach { alert ->
                    WmoIndividualAlertCard(
                        alert = alert,
                        language = language,
                        initiallyExpanded = true
                    )
                }
            }

            // ================= 5. 全球災害警報 (GDACS) セクション =================
            if (gdacsAlerts.isNotEmpty()) {
                val gdacsSourceTitle = when (language) {
                    AppLanguage.JAPANESE -> "全球災害警報・調整システム"
                    AppLanguage.ENGLISH -> "Global Disaster Alert & Coordination System"
                    AppLanguage.SIMPLIFIED_CHINESE -> "全球灾害预警与协调系统"
                    AppLanguage.TRADITIONAL_CHINESE -> "全球災害預警與協調系統"
                }
                val gdacsSourceBadge = "UN / EC (GDACS)"

                AlertSourceSectionHeader(
                    title = gdacsSourceTitle,
                    sourceBadge = gdacsSourceBadge
                )

                gdacsAlerts.forEach { alert ->
                    GdacsIndividualAlertCard(
                        alert = alert,
                        language = language,
                        initiallyExpanded = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 警報発信元セクションヘッダー
 */
@Composable
private fun AlertSourceSectionHeader(
    title: String,
    sourceBadge: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ) {
            Text(
                text = sourceBadge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
