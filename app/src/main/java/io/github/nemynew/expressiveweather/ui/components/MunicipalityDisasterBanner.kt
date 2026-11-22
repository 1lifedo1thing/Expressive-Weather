package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AlertSource
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.GlobalAlertItem
import io.github.nemynew.expressiveweather.data.JmaWarningMaster
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.MunicipalityAlertInfo
import io.github.nemynew.expressiveweather.data.MunicipalityWarningItem

/**
 * 気象庁 (JMA)、米国気象局 (NWS)、全球災害警報 (GDACS) の各系統を集約表示するアコーディオン防災バナー
 *
 * 厳格なデザイン仕様:
 * - 警報・注意報カードの厳密な背景色（cardBg）と文字色（contentColor）を展開後も完全に踏襲
 * - 不要な枠線（border）は一切排除
 * - まとめや発表概況などの不要なテキストを排除し、純粋に「警報のカード一覧」のみを展開
 */
@Composable
fun MunicipalityDisasterBanner(
    alertInfo: MunicipalityAlertInfo?,
    jmaWarnings: List<String> = emptyList(),
    jmaOverviewText: String? = null,
    globalAlerts: List<GlobalAlertItem> = emptyList(),
    onAlertClick: ((AlertSource) -> Unit)? = null,
    onBannerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val language = LocalAppLanguage.current

    // 1. 気象庁 (JMA) の警報リスト（解除されたものは徹底的に除外）
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

    // 2. 米国気象局 (NWS) の警報リスト
    val nwsAlerts = remember(globalAlerts) {
        globalAlerts.filter { it.source == AlertSource.NWS }
            .sortedBy { it.severity.ordinal }
    }

    // 3. 世界気象機関 (WMO) の警報リスト (日本・米国以外の海外地域専用、または常にオン時)
    val wmoAlerts = remember(globalAlerts) {
        globalAlerts.filter { it.source == AlertSource.WMO }
            .sortedBy { it.severity.ordinal }
    }

    // 4. 全球災害警報 (GDACS) の警報リスト
    val gdacsAlerts = remember(globalAlerts) {
        globalAlerts.filter { it.source == AlertSource.GDACS }
            .sortedBy { it.severity.ordinal }
    }

    // 5. 欧州気象警報ネットワーク (MeteoAlarm) の警報リスト
    val meteoAlarmAlerts = remember(globalAlerts) {
        globalAlerts.filter { it.source == AlertSource.METEOALARM }
            .sortedBy { it.severity.ordinal }
    }

    val hasAnyAlerts = sortedJmaList.isNotEmpty() || nwsAlerts.isNotEmpty() || meteoAlarmAlerts.isNotEmpty() || wmoAlerts.isNotEmpty() || gdacsAlerts.isNotEmpty()
    if (!hasAnyAlerts) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ================= A. 気象庁 (JMA) バナーカード =================
        if (sortedJmaList.isNotEmpty()) {
            val hasSpecial = sortedJmaList.any { it.isSpecial }
            val hasWarning = sortedJmaList.any { it.isWarning }
            val warningType = when {
                hasSpecial -> WeatherWarningType.SPECIAL_WARNING
                hasWarning -> WeatherWarningType.WARNING
                else -> WeatherWarningType.ADVISORY
            }
            val aggregatedTitle = JmaWarningMaster.formatAggregatedTitle(sortedJmaList, language)
            val rawTitle = sortedJmaList.joinToString("・") { it.name }
            val description = if (language == AppLanguage.JAPANESE) {
                alertInfo?.headlineText?.trim()?.ifBlank { null } ?: formatWarningDescription(
                    rawTitle = rawTitle,
                    headlineText = null,
                    language = language
                )
            } else {
                formatWarningDescription(
                    rawTitle = rawTitle,
                    headlineText = null,
                    language = language
                )
            }

            DisasterAlertNavigationCard(
                type = warningType,
                title = aggregatedTitle,
                description = description,
                onClick = { onAlertClick?.invoke(AlertSource.JMA) ?: onBannerClick?.invoke() }
            )
        }

        // ================= B. 米国気象局 (NWS) バナーカード =================
        if (nwsAlerts.isNotEmpty()) {
            val topAlert = nwsAlerts.first()
            val hasSpecial = nwsAlerts.any { it.type == WeatherWarningType.SPECIAL_WARNING }
            val hasWarning = nwsAlerts.any { it.type == WeatherWarningType.WARNING }
            val warningType = when {
                hasSpecial -> WeatherWarningType.SPECIAL_WARNING
                hasWarning -> WeatherWarningType.WARNING
                else -> WeatherWarningType.ADVISORY
            }
            val aggregatedTitle = if (nwsAlerts.size == 1) {
                topAlert.getLocalizedTitle(language)
            } else {
                when (language) {
                    AppLanguage.ENGLISH -> "${topAlert.getLocalizedTitle(language)} & ${nwsAlerts.size - 1} more"
                    AppLanguage.SIMPLIFIED_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其余${nwsAlerts.size - 1}项预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其餘${nwsAlerts.size - 1}項預警"
                    AppLanguage.JAPANESE -> "${topAlert.getLocalizedTitle(language)} ほか${nwsAlerts.size - 1}件の警報・注意報"
                }
            }
            val headline = if (language == AppLanguage.ENGLISH) {
                topAlert.headline.trim().ifBlank { topAlert.getLocalizedDescription(language) }
            } else {
                topAlert.getLocalizedDescription(language)
            }

            DisasterAlertNavigationCard(
                type = warningType,
                title = aggregatedTitle,
                description = headline,
                onClick = { onAlertClick?.invoke(AlertSource.NWS) ?: onBannerClick?.invoke() }
            )
        }

        // ================= C. 世界気象機関 (WMO) バナーカード =================
        if (wmoAlerts.isNotEmpty()) {
            val topAlert = wmoAlerts.first()
            val hasSpecial = wmoAlerts.any { it.type == WeatherWarningType.SPECIAL_WARNING }
            val hasWarning = wmoAlerts.any { it.type == WeatherWarningType.WARNING }
            val warningType = when {
                hasSpecial -> WeatherWarningType.SPECIAL_WARNING
                hasWarning -> WeatherWarningType.WARNING
                else -> WeatherWarningType.ADVISORY
            }
            val aggregatedTitle = if (wmoAlerts.size == 1) {
                topAlert.getLocalizedTitle(language)
            } else {
                when (language) {
                    AppLanguage.ENGLISH -> "${topAlert.getLocalizedTitle(language)} & ${wmoAlerts.size - 1} more"
                    AppLanguage.SIMPLIFIED_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其余${wmoAlerts.size - 1}项WMO预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其餘${wmoAlerts.size - 1}項WMO預警"
                    AppLanguage.JAPANESE -> "${topAlert.getLocalizedTitle(language)} ほか${wmoAlerts.size - 1}件のWMO世界警報"
                }
            }
            val detailedText = if (topAlert.description.isNotBlank() && topAlert.description.trim() != topAlert.eventName.trim()) {
                topAlert.description.trim().take(160)
            } else if (topAlert.headline.isNotBlank() && topAlert.headline.trim() != topAlert.eventName.trim()) {
                topAlert.headline.trim()
            } else {
                topAlert.getLocalizedDescription(language)
            }

            DisasterAlertNavigationCard(
                type = warningType,
                title = aggregatedTitle,
                description = detailedText,
                onClick = { onAlertClick?.invoke(AlertSource.WMO) ?: onBannerClick?.invoke() }
            )
        }

        // ================= D. 欧州気象警報ネットワーク (MeteoAlarm) バナーカード =================
        if (meteoAlarmAlerts.isNotEmpty()) {
            val topAlert = meteoAlarmAlerts.first()
            val hasSpecial = meteoAlarmAlerts.any { it.type == WeatherWarningType.SPECIAL_WARNING }
            val hasWarning = meteoAlarmAlerts.any { it.type == WeatherWarningType.WARNING }
            val warningType = when {
                hasSpecial -> WeatherWarningType.SPECIAL_WARNING
                hasWarning -> WeatherWarningType.WARNING
                else -> WeatherWarningType.ADVISORY
            }
            val aggregatedTitle = if (meteoAlarmAlerts.size == 1) {
                topAlert.getLocalizedTitle(language)
            } else {
                when (language) {
                    AppLanguage.ENGLISH -> "${topAlert.getLocalizedTitle(language)} & ${meteoAlarmAlerts.size - 1} more"
                    AppLanguage.SIMPLIFIED_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其余${meteoAlarmAlerts.size - 1}项欧洲预警"
                    AppLanguage.TRADITIONAL_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其餘${meteoAlarmAlerts.size - 1}項歐洲警報"
                    AppLanguage.JAPANESE -> "${topAlert.getLocalizedTitle(language)} ほか${meteoAlarmAlerts.size - 1}件の欧州気象警報"
                }
            }
            val detailedText = if (topAlert.description.isNotBlank() && topAlert.description.trim() != topAlert.eventName.trim()) {
                topAlert.description.trim().take(160)
            } else if (topAlert.headline.isNotBlank() && topAlert.headline.trim() != topAlert.eventName.trim()) {
                topAlert.headline.trim()
            } else {
                topAlert.getLocalizedDescription(language)
            }

            DisasterAlertNavigationCard(
                type = warningType,
                title = aggregatedTitle,
                description = detailedText,
                onClick = { onAlertClick?.invoke(AlertSource.METEOALARM) ?: onBannerClick?.invoke() }
            )
        }

        // ================= E. 全球災害警報 (GDACS) バナーカード =================
        if (gdacsAlerts.isNotEmpty()) {
            val topAlert = gdacsAlerts.first()
            val hasSpecial = gdacsAlerts.any { it.type == WeatherWarningType.SPECIAL_WARNING }
            val hasWarning = gdacsAlerts.any { it.type == WeatherWarningType.WARNING }
            val warningType = when {
                hasSpecial -> WeatherWarningType.SPECIAL_WARNING
                hasWarning -> WeatherWarningType.WARNING
                else -> WeatherWarningType.ADVISORY
            }
            val aggregatedTitle = if (gdacsAlerts.size == 1) {
                topAlert.getLocalizedTitle(language)
            } else {
                when (language) {
                    AppLanguage.ENGLISH -> "${topAlert.getLocalizedTitle(language)} & ${gdacsAlerts.size - 1} more"
                    AppLanguage.SIMPLIFIED_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其余${gdacsAlerts.size - 1}项全球警报"
                    AppLanguage.TRADITIONAL_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其餘${gdacsAlerts.size - 1}項全球警報"
                    AppLanguage.JAPANESE -> "${topAlert.getLocalizedTitle(language)} ほか${gdacsAlerts.size - 1}件の全球警報"
                }
            }
            val titleText = topAlert.getLocalizedTitle(language).ifBlank { topAlert.eventName.trim() }

            DisasterAlertNavigationCard(
                type = warningType,
                title = aggregatedTitle,
                description = titleText,
                onClick = { onAlertClick?.invoke(AlertSource.GDACS) ?: onBannerClick?.invoke() }
            )
        }
    }
}

/**
 * 警報ナビゲーションカード
 *
 * 警報アイコン + タイトル + 詳細文章 + 「＞」アイコンを配置し、タップで独立した警報詳細 Activity へ遷移
 */
@Composable
fun DisasterAlertNavigationCard(
    type: WeatherWarningType,
    title: String,
    description: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WeatherAlertColors.forWarningType(type)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBg,
            contentColor = colors.contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colors.contentColor,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = title,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.contentColor,
                        lineHeight = 22.sp
                    )
                }

                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.5.sp,
                        color = colors.contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ＞ アイコン (詳細ページへの遷移を示す)
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.contentColor.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * JMA 個別警報カード
 * 警報・注意報の厳格な背景色カラーコード・文字色カラーコードを踏襲した独立カード。
 * タップで現象・避難行動・警戒ポイントがインライン展開されます（多言語対応）。
 */
@Composable
internal fun JmaIndividualWarningCard(
    item: MunicipalityWarningItem,
    language: AppLanguage,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isDetailOpen by remember { mutableStateOf(initiallyExpanded) }
    val detail = remember(item.name) { JmaWarningMaster.getDetailByName(item.name) }
    val localizedName = remember(item.name, language) {
        JmaWarningMaster.getLocalizedWarningName(item.name, language)
    }

    val warningType = when {
        item.isSpecial -> WeatherWarningType.SPECIAL_WARNING // 赤色: 特別警戒 (#FFB4AB / #690005)
        item.isWarning -> WeatherWarningType.WARNING         // 橙色: 警戒 (#FFDBC9 / #341100)
        else -> WeatherWarningType.ADVISORY                  // 黄色: 注意 (#FFCA00 / #624E00)
    }
    val colors = WeatherAlertColors.forWarningType(warningType)

    val arrowRotation by animateFloatAsState(
        targetValue = if (isDetailOpen) 180f else 0f,
        label = "IndividualCardArrow"
    )

    val localizedPhenomenon = detail.getLocalizedPhenomenon(language)
    val localizedActionGuide = detail.getLocalizedActionGuide(language)
    val localizedCautionPoints = detail.getLocalizedCautionPoints(language)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { isDetailOpen = !isDetailOpen }
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBg,
            contentColor = colors.contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ヘッダー行: アイコン + 多言語対応警報名 + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colors.contentColor,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = localizedName,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.contentColor
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = getExpandCollapseDescription(isDetailOpen, language),
                    tint = colors.contentColor,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(arrowRotation)
                )
            }

            // タップで展開される個別詳細（現象・避難指針・警戒ポイント）
            AnimatedVisibility(
                visible = isDetailOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 想定される現象・被害
                    if (localizedPhenomenon.isNotBlank()) {
                        Column {
                            Text(
                                text = getPhenomenonLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = localizedPhenomenon,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.contentColor
                            )
                        }
                    }

                    // 推奨される避難・安全確保行動
                    if (localizedActionGuide.isNotBlank()) {
                        Column {
                            Text(
                                text = getActionGuideLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = localizedActionGuide,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.contentColor
                            )
                        }
                    }

                    // 警戒ポイント
                    if (localizedCautionPoints.isNotEmpty()) {
                        Column {
                            Text(
                                text = getCautionPointsLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            localizedCautionPoints.forEach { point ->
                                Text(
                                    text = "• $point",
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
                                    color = colors.contentColor
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
 * B. 米国気象局 (NWS) 集約アコーディオンカード
 */
@Composable
private fun NwsAccordionCard(
    alerts: List<GlobalAlertItem>,
    language: AppLanguage
) {
    var isExpanded by remember { mutableStateOf(false) }

    val topAlert = alerts.firstOrNull() ?: return
    val hasSpecial = alerts.any { it.type == WeatherWarningType.SPECIAL_WARNING }
    val hasWarning = alerts.any { it.type == WeatherWarningType.WARNING }
    val warningType = when {
        hasSpecial -> WeatherWarningType.SPECIAL_WARNING // 赤色: 特別警戒 (#FFB4AB / #690005)
        hasWarning -> WeatherWarningType.WARNING         // 橙色: 警戒 (#FFDBC9 / #341100)
        else -> WeatherWarningType.ADVISORY              // 黄色: 注意 (#FFCA00 / #624E00)
    }

    val aggregatedTitle = if (alerts.size == 1) {
        topAlert.getLocalizedTitle(language)
    } else {
        when (language) {
            AppLanguage.ENGLISH -> "${topAlert.getLocalizedTitle(language)} & ${alerts.size - 1} more"
            AppLanguage.SIMPLIFIED_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其余${alerts.size - 1}项预警"
            AppLanguage.TRADITIONAL_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其餘${alerts.size - 1}項預警"
            AppLanguage.JAPANESE -> "${topAlert.getLocalizedTitle(language)} ほか${alerts.size - 1}件の警報・注意報"
        }
    }

    // 展開前: 英語は取得した headline を優先、他言語はローカライズされた説明文を優先
    val headline = if (language == AppLanguage.ENGLISH) {
        topAlert.headline.trim().ifBlank { topAlert.getLocalizedDescription(language) }
    } else {
        topAlert.getLocalizedDescription(language)
    }

    AccordionDisasterAlertCard(
        type = warningType,
        title = aggregatedTitle,
        description = headline,
        isExpanded = isExpanded,
        onToggleExpand = { isExpanded = !isExpanded }
    ) {
        // 展開後: 各NWS警報カード一覧（タップで instruction と description が表示）
        alerts.forEach { alert ->
            NwsIndividualAlertCard(
                alert = alert,
                language = language
            )
        }
    }
}

/**
 * NWS 個別警報カード
 */
@Composable
internal fun NwsIndividualAlertCard(
    alert: GlobalAlertItem,
    language: AppLanguage,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isDetailOpen by remember { mutableStateOf(initiallyExpanded) }
    val colors = WeatherAlertColors.forWarningType(alert.type)

    val arrowRotation by animateFloatAsState(
        targetValue = if (isDetailOpen) 180f else 0f,
        label = "NwsCardArrow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { isDetailOpen = !isDetailOpen }
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBg,
            contentColor = colors.contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colors.contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = alert.getLocalizedTitle(language),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.contentColor
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = getExpandCollapseDescription(isDetailOpen, language),
                    tint = colors.contentColor,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(arrowRotation)
                )
            }

            AnimatedVisibility(
                visible = isDetailOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 展開後: instruction (避難・安全確保行動)
                    if (!alert.instruction.isNullOrBlank()) {
                        Column {
                            Text(
                                text = getNwsInstructionLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.instruction.trim(),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.contentColor
                            )
                        }
                    }

                    // 展開後: description (詳細解説)
                    if (alert.description.isNotBlank()) {
                        Column {
                            Text(
                                text = getNwsDescriptionLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.description.trim(),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * C. 世界気象機関 (WMO) 集約アコーディオンカード
 * 日本・米国以外の世界各国専用（「常にオン」時は日本・米国でも並行表示）
 */
@Composable
private fun WmoAccordionCard(
    alerts: List<GlobalAlertItem>,
    language: AppLanguage
) {
    var isExpanded by remember { mutableStateOf(false) }

    val topAlert = alerts.firstOrNull() ?: return
    val hasSpecial = alerts.any { it.type == WeatherWarningType.SPECIAL_WARNING }
    val hasWarning = alerts.any { it.type == WeatherWarningType.WARNING }
    val warningType = when {
        hasSpecial -> WeatherWarningType.SPECIAL_WARNING // 赤色: 特別警戒 (#FFB4AB / #690005)
        hasWarning -> WeatherWarningType.WARNING         // 橙色: 警戒 (#FFDBC9 / #341100)
        else -> WeatherWarningType.ADVISORY              // 黄色: 注意 (#FFCA00 / #624E00)
    }

    val aggregatedTitle = if (alerts.size == 1) {
        topAlert.getLocalizedTitle(language)
    } else {
        when (language) {
            AppLanguage.ENGLISH -> "${topAlert.getLocalizedTitle(language)} & ${alerts.size - 1} more"
            AppLanguage.SIMPLIFIED_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其余${alerts.size - 1}项WMO预警"
            AppLanguage.TRADITIONAL_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其餘${alerts.size - 1}項WMO預警"
            AppLanguage.JAPANESE -> "${topAlert.getLocalizedTitle(language)} ほか${alerts.size - 1}件のWMO世界警報"
        }
    }

    // 展開前: 取得した多言語対応 Title または headline を優先表示
    val titleText = topAlert.getLocalizedTitle(language).ifBlank { topAlert.headline.trim() }

    AccordionDisasterAlertCard(
        type = warningType,
        title = aggregatedTitle,
        description = titleText,
        isExpanded = isExpanded,
        onToggleExpand = { isExpanded = !isExpanded }
    ) {
        // 展開後: 各WMO警報カード一覧（タップで詳細・対象地域が表示）
        alerts.forEach { alert ->
            WmoIndividualAlertCard(
                alert = alert,
                language = language
            )
        }
    }
}

/**
 * WMO 個別警報カード
 */
@Composable
internal fun WmoIndividualAlertCard(
    alert: GlobalAlertItem,
    language: AppLanguage,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isDetailOpen by remember { mutableStateOf(initiallyExpanded) }
    val colors = WeatherAlertColors.forWarningType(alert.type)

    val arrowRotation by animateFloatAsState(
        targetValue = if (isDetailOpen) 180f else 0f,
        label = "WmoCardArrow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { isDetailOpen = !isDetailOpen }
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBg,
            contentColor = colors.contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colors.contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = alert.getLocalizedTitle(language),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.contentColor
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = getExpandCollapseDescription(isDetailOpen, language),
                    tint = colors.contentColor,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(arrowRotation)
                )
            }

            AnimatedVisibility(
                visible = isDetailOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 展開後: description (災害概要・解説本文)
                    if (alert.description.isNotBlank()) {
                        Column {
                            Text(
                                text = getWmoOverviewLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.description.trim(),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.contentColor
                            )
                        }
                    }

                    // 展開後: areaDesc (対象エリア情報)
                    if (!alert.areaDesc.isNullOrBlank()) {
                        Column {
                            Text(
                                text = getWmoAreaLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.areaDesc.trim(),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 欧州気象警報ネットワーク (MeteoAlarm) 個別警報カード
 */
@Composable
fun MeteoAlarmIndividualAlertCard(
    alert: GlobalAlertItem,
    language: AppLanguage,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isDetailOpen by remember { mutableStateOf(initiallyExpanded) }
    val colors = WeatherAlertColors.forWarningType(alert.type)

    val arrowRotation by animateFloatAsState(
        targetValue = if (isDetailOpen) 180f else 0f,
        label = "MeteoAlarmCardArrow"
    )

    val severityBadgeText = when (alert.type) {
        WeatherWarningType.SPECIAL_WARNING -> when (language) {
            AppLanguage.JAPANESE -> "特別警戒 (Red: Extreme)"
            AppLanguage.ENGLISH -> "Red Warning (Extreme Hazard)"
            AppLanguage.SIMPLIFIED_CHINESE -> "红色特别警报 (Extreme)"
            AppLanguage.TRADITIONAL_CHINESE -> "紅色特別警報 (Extreme)"
        }
        WeatherWarningType.WARNING -> when (language) {
            AppLanguage.JAPANESE -> "警戒 (Orange: Severe)"
            AppLanguage.ENGLISH -> "Orange Warning (Severe Hazard)"
            AppLanguage.SIMPLIFIED_CHINESE -> "橙色警报 (Severe)"
            AppLanguage.TRADITIONAL_CHINESE -> "橙色警報 (Severe)"
        }
        else -> when (language) {
            AppLanguage.JAPANESE -> "注意 (Yellow: Moderate)"
            AppLanguage.ENGLISH -> "Yellow Warning (Moderate Hazard)"
            AppLanguage.SIMPLIFIED_CHINESE -> "黄色注意预警 (Moderate)"
            AppLanguage.TRADITIONAL_CHINESE -> "黃色注意預警 (Moderate)"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { isDetailOpen = !isDetailOpen }
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBg,
            contentColor = colors.contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colors.contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = alert.getLocalizedTitle(language),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.contentColor
                        )
                        Text(
                            text = severityBadgeText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.contentColor.copy(alpha = 0.85f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = getExpandCollapseDescription(isDetailOpen, language),
                    tint = colors.contentColor,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(arrowRotation)
                )
            }

            AnimatedVisibility(
                visible = isDetailOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 展開後: 概要・解説本文
                    if (alert.description.isNotBlank()) {
                        Column {
                            Text(
                                text = getMeteoAlarmOverviewLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.description.trim(),
                                fontSize = 13.sp,
                                lineHeight = 18.5.sp,
                                color = colors.contentColor
                            )
                        }
                    }

                    // 展開後: 防災指示・行動指針
                    if (!alert.instruction.isNullOrBlank()) {
                        Column {
                            Text(
                                text = getMeteoAlarmInstructionLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.instruction.trim(),
                                fontSize = 13.sp,
                                lineHeight = 18.5.sp,
                                color = colors.contentColor
                            )
                        }
                    }

                    // 展開後: 対象エリア
                    if (!alert.areaDesc.isNullOrBlank()) {
                        Column {
                            Text(
                                text = getMeteoAlarmAreaLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.areaDesc.trim(),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.contentColor
                            )
                        }
                    }

                    // 展開後: 有効期間
                    if (!alert.effectiveTime.isNullOrBlank() || !alert.expiresTime.isNullOrBlank()) {
                        val immediateLabel = when (language) {
                            AppLanguage.JAPANESE -> "即時"
                            AppLanguage.ENGLISH -> "Immediate"
                            AppLanguage.SIMPLIFIED_CHINESE -> "即时"
                            AppLanguage.TRADITIONAL_CHINESE -> "即時"
                        }
                        val tbdLabel = when (language) {
                            AppLanguage.JAPANESE -> "未定"
                            AppLanguage.ENGLISH -> "TBD"
                            AppLanguage.SIMPLIFIED_CHINESE -> "待定"
                            AppLanguage.TRADITIONAL_CHINESE -> "未定"
                        }
                        val onsetStr = formatIsoAlertTimestamp(alert.effectiveTime) ?: immediateLabel
                        val expiresStr = formatIsoAlertTimestamp(alert.expiresTime) ?: tbdLabel
                        val periodLabel = when (language) {
                            AppLanguage.JAPANESE -> "【有効期間】"
                            AppLanguage.ENGLISH -> "【Valid Period】"
                            AppLanguage.SIMPLIFIED_CHINESE -> "【生效时间】"
                            AppLanguage.TRADITIONAL_CHINESE -> "【生效時間】"
                        }
                        Column {
                            Text(
                                text = periodLabel,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$onsetStr 〜 $expiresStr",
                                fontSize = 12.5.sp,
                                color = colors.contentColor.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // 発信元クレジット
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.contentColor.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val creditText = when (language) {
                            AppLanguage.JAPANESE -> "提供元: EUMETNET / MeteoAlarm ＆ 欧州各国気象機関 (NMHS)"
                            AppLanguage.ENGLISH -> "Source: EUMETNET / MeteoAlarm & European NMHSs"
                            AppLanguage.SIMPLIFIED_CHINESE -> "来源: EUMETNET / MeteoAlarm 及欧洲各国气象机构 (NMHS)"
                            AppLanguage.TRADITIONAL_CHINESE -> "來源: EUMETNET / MeteoAlarm 及歐洲各國氣象機構 (NMHS)"
                        }
                        Text(
                            text = creditText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.contentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * D. 全球災害警報 (GDACS) 集約アコーディオンカード
 */
@Composable
private fun GdacsAccordionCard(
    alerts: List<GlobalAlertItem>,
    language: AppLanguage
) {
    var isExpanded by remember { mutableStateOf(false) }

    val topAlert = alerts.firstOrNull() ?: return
    val hasSpecial = alerts.any { it.type == WeatherWarningType.SPECIAL_WARNING }
    val hasWarning = alerts.any { it.type == WeatherWarningType.WARNING }
    val warningType = when {
        hasSpecial -> WeatherWarningType.SPECIAL_WARNING // 赤色: 特別警戒 (#FFB4AB / #690005)
        hasWarning -> WeatherWarningType.WARNING         // 橙色: 警戒 (#FFDBC9 / #341100)
        else -> WeatherWarningType.ADVISORY              // 黄色: 注意 (#FFCA00 / #624E00)
    }

    val aggregatedTitle = if (alerts.size == 1) {
        topAlert.getLocalizedTitle(language)
    } else {
        when (language) {
            AppLanguage.ENGLISH -> "${topAlert.getLocalizedTitle(language)} & ${alerts.size - 1} more"
            AppLanguage.SIMPLIFIED_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其余${alerts.size - 1}项全球警报"
            AppLanguage.TRADITIONAL_CHINESE -> "${topAlert.getLocalizedTitle(language)} 及其餘${alerts.size - 1}項全球警報"
            AppLanguage.JAPANESE -> "${topAlert.getLocalizedTitle(language)} ほか${alerts.size - 1}件の全球警報"
        }
    }

    // 展開前: 取得した多言語対応 Title を最優先表示
    val titleText = topAlert.getLocalizedTitle(language).ifBlank { topAlert.eventName.trim() }

    AccordionDisasterAlertCard(
        type = warningType,
        title = aggregatedTitle,
        description = titleText,
        isExpanded = isExpanded,
        onToggleExpand = { isExpanded = !isExpanded }
    ) {
        // 展開後: 各GDACS警報カード一覧（タップで description が表示）
        alerts.forEach { alert ->
            GdacsIndividualAlertCard(
                alert = alert,
                language = language
            )
        }
    }
}


/**
 * GDACS 個別警報カード
 */
@Composable
internal fun GdacsIndividualAlertCard(
    alert: GlobalAlertItem,
    language: AppLanguage,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isDetailOpen by remember { mutableStateOf(initiallyExpanded) }
    val colors = WeatherAlertColors.forWarningType(alert.type)

    val arrowRotation by animateFloatAsState(
        targetValue = if (isDetailOpen) 180f else 0f,
        label = "GdacsCardArrow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { isDetailOpen = !isDetailOpen }
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBg,
            contentColor = colors.contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colors.contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = alert.getLocalizedTitle(language),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.contentColor
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = getExpandCollapseDescription(isDetailOpen, language),
                    tint = colors.contentColor,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(arrowRotation)
                )
            }

            AnimatedVisibility(
                visible = isDetailOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 展開後: description (災害概要・解説本文)
                    if (alert.description.isNotBlank()) {
                        Column {
                            Text(
                                text = getGdacsDescriptionLabel(language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.description.trim(),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= 多言語ヘルパー関数 =================

internal fun getJmaOverviewLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【府県気象情報】"
    AppLanguage.ENGLISH -> "【Regional Weather Commentary】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【县级气象信息】"
    AppLanguage.TRADITIONAL_CHINESE -> "【縣級氣象資訊】"
}

internal fun getPhenomenonLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【現象と想定される被害】"
    AppLanguage.ENGLISH -> "【Expected Hazards & Impacts】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【预期灾害与影响】"
    AppLanguage.TRADITIONAL_CHINESE -> "【預期災害與影響】"
}

internal fun getActionGuideLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【推奨される避難・安全行動】"
    AppLanguage.ENGLISH -> "【Recommended Safety Actions】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【推荐避险与安全行动】"
    AppLanguage.TRADITIONAL_CHINESE -> "【推薦避難與安全行動】"
}

internal fun getCautionPointsLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【警戒ポイント】"
    AppLanguage.ENGLISH -> "【Key Watch Points】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【警戒重点】"
    AppLanguage.TRADITIONAL_CHINESE -> "【警戒重點】"
}

internal fun getNwsInstructionLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【避難・安全確保行動】"
    AppLanguage.ENGLISH -> "【Recommended Actions】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【避险与安全防护行动】"
    AppLanguage.TRADITIONAL_CHINESE -> "【避難與安全防護行動】"
}

internal fun getNwsDescriptionLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【詳細解説】"
    AppLanguage.ENGLISH -> "【Detailed Description】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【详细说明】"
    AppLanguage.TRADITIONAL_CHINESE -> "【詳細說明】"
}

internal fun getGdacsDescriptionLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【災害概要】"
    AppLanguage.ENGLISH -> "【Disaster Summary】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【灾害概要】"
    AppLanguage.TRADITIONAL_CHINESE -> "【災害概要】"
}

internal fun getWmoOverviewLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【災害概要・警報状況】"
    AppLanguage.ENGLISH -> "【Disaster Overview & Status】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【灾害概要与预警情况】"
    AppLanguage.TRADITIONAL_CHINESE -> "【災害概要與預警情況】"
}

internal fun getWmoAreaLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【対象地域】"
    AppLanguage.ENGLISH -> "【Affected Area】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【影响区域】"
    AppLanguage.TRADITIONAL_CHINESE -> "【受影響區域】"
}

internal fun getMeteoAlarmOverviewLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【気象警報状況・解説】"
    AppLanguage.ENGLISH -> "【Weather Warning & Details】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【气象预警情况与详情】"
    AppLanguage.TRADITIONAL_CHINESE -> "【氣象警報情況與詳情】"
}

internal fun getMeteoAlarmInstructionLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【防災行動・指示】"
    AppLanguage.ENGLISH -> "【Recommended Safety Actions】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【防灾避险与应对行动】"
    AppLanguage.TRADITIONAL_CHINESE -> "【防災避險與應對行動】"
}

internal fun getMeteoAlarmAreaLabel(language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> "【対象地域】"
    AppLanguage.ENGLISH -> "【Affected Region】"
    AppLanguage.SIMPLIFIED_CHINESE -> "【影响区域】"
    AppLanguage.TRADITIONAL_CHINESE -> "【受影響區域】"
}

internal fun getExpandCollapseDescription(isExpanded: Boolean, language: AppLanguage): String = when (language) {
    AppLanguage.JAPANESE -> if (isExpanded) "折りたたむ" else "詳細を見る"
    AppLanguage.ENGLISH -> if (isExpanded) "Collapse" else "View details"
    AppLanguage.SIMPLIFIED_CHINESE -> if (isExpanded) "折叠" else "查看详情"
    AppLanguage.TRADITIONAL_CHINESE -> if (isExpanded) "收合" else "檢視詳情"
}

/**
 * 警報カードの説明文整形（多言語対応）
 */
internal fun formatWarningDescription(
    rawTitle: String,
    headlineText: String?,
    language: AppLanguage
): String {
    if (!headlineText.isNullOrBlank()) {
        return headlineText
    }
    return when (language) {
        AppLanguage.JAPANESE -> when {
            rawTitle.contains("特別") -> "命に危険が及ぶ極めて危険な状況です。直ちに身の安全を確保してください。"
            rawTitle.contains("大雨") && rawTitle.contains("洪水") -> "大雨および河川の増水・氾濫に警戒してください。"
            rawTitle.contains("大雨") -> "土砂災害や低い土地の浸水に警戒してください。"
            rawTitle.contains("洪水") -> "河川の増水や氾濫に警戒してください。"
            rawTitle.contains("暴風雪") -> "猛吹雪や吹きだまりによる交通障害に厳重に警戒してください。"
            rawTitle.contains("暴風") || rawTitle.contains("強風") -> "暴風や強風、高波に警戒してください。"
            rawTitle.contains("大雪") -> "大雪や路面凍結による交通障害に警戒してください。"
            rawTitle.contains("高潮") || rawTitle.contains("波浪") -> "高潮や高波に厳重に警戒してください。"
            rawTitle.contains("雷") -> "落雷や急な強い雨、突風に注意してください。"
            rawTitle.contains("濃霧") -> "濃霧による視程障害に注意してください。"
            rawTitle.contains("乾燥") -> "空気の乾燥による火災に注意してください。"
            rawTitle.contains("なだれ") -> "なだれに注意してください。"
            rawTitle.contains("融雪") -> "融雪による浸水や土砂災害に注意してください。"
            rawTitle.contains("着氷") || rawTitle.contains("着雪") -> "電線や船体等への着氷・着雪に注意してください。"
            rawTitle.contains("霜") -> "農作物の霜害に注意してください。"
            rawTitle.contains("低温") -> "低温による農作物や水道管の凍結に注意してください。"
            else -> "気象庁からの警報・注意報が発表されています。最新の気象情報に留意してください。"
        }
        AppLanguage.ENGLISH -> when {
            rawTitle.contains("特別") -> "Life-threatening conditions are imminent. Take immediate shelter."
            rawTitle.contains("大雨") && rawTitle.contains("洪水") -> "Flash flooding and dangerous river conditions are expected."
            rawTitle.contains("大雨") -> "Stay alert for landslides and localized flooding in low-lying areas."
            rawTitle.contains("洪水") -> "Stay alert for rising river water levels and potential flooding."
            rawTitle.contains("暴風雪") -> "Severe blizzard conditions and heavy drifting snow expected."
            rawTitle.contains("暴風") || rawTitle.contains("強風") -> "Dangerous winds and severe conditions are expected. Stay indoors."
            rawTitle.contains("大雪") -> "Heavy snow and hazardous road conditions are expected."
            rawTitle.contains("高潮") || rawTitle.contains("波浪") -> "High waves and coastal storm surge expected. Keep away from shores."
            rawTitle.contains("雷") -> "Watch out for lightning, sudden downpours, and gusty winds."
            rawTitle.contains("濃霧") -> "Dense fog warning. Be cautious of severely reduced visibility."
            rawTitle.contains("乾燥") -> "Dry weather conditions. Take extra precautions against fires."
            rawTitle.contains("なだれ") -> "Avalanche hazard in mountainous terrain. Avoid steep slopes."
            rawTitle.contains("融雪") -> "Snowmelt risk. Beware of localized flooding and slope failures."
            rawTitle.contains("着氷") || rawTitle.contains("着雪") -> "Freezing rain and snow accretion hazard."
            rawTitle.contains("霜") -> "Frost conditions expected. Protect sensitive crops."
            rawTitle.contains("低温") -> "Freezing temperatures. Beware of frozen water pipes and crop damage."
            else -> "Severe weather warnings/advisories in effect. Please take necessary precautions."
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when {
            rawTitle.contains("特別") -> "情况极其危险，危及生命安全。请立即采取应急避险措施。"
            rawTitle.contains("大雨") && rawTitle.contains("洪水") -> "大雨以及河流暴涨、泛滥风险高，请注意警戒。"
            rawTitle.contains("大雨") -> "请警惕滑坡地质灾害和低洼地区内涝。"
            rawTitle.contains("洪水") -> "请警惕河流暴涨和泛滥风险。"
            rawTitle.contains("暴風雪") -> "强暴风雪可能造成严重交通受阻，请严加防范。"
            rawTitle.contains("暴風") || rawTitle.contains("強風") -> "狂风风暴天气，请注意防风避险并留在安全室内。"
            rawTitle.contains("大雪") -> "降雪量大，道路结冰，请注意出行交通安全。"
            rawTitle.contains("高潮") || rawTitle.contains("波浪") -> "风暴潮巨浪来袭，严禁靠近沿海与岸边。"
            rawTitle.contains("雷") -> "雷电交加且可能有短时强降水与阵风，请在室内躲避。"
            rawTitle.contains("濃霧") -> "大雾能见度极低，请注意交通出行安全。"
            rawTitle.contains("乾燥") -> "空气干燥火险等级高，请严格注意用火安全。"
            rawTitle.contains("なだれ") -> "山区有雪崩危险，请远离积雪陡坡。"
            rawTitle.contains("融雪") -> "积雪融化可能引发低洼积水与滑坡，请注意防范。"
            rawTitle.contains("着氷") || rawTitle.contains("着雪") -> "可能出现电线与设施结冰结雪，注意防护。"
            rawTitle.contains("霜") -> "地面可能出现霜冻，请注意农作物防冻保暖。"
            rawTitle.contains("低温") -> "低温严寒天气，注意防寒保暖及水管防冻裂。"
            else -> "气象部门已发布预警信息，请密切关注天气动态做好防范。"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when {
            rawTitle.contains("特別") -> "情況極其危險，危及生命安全。請立即採取緊急避難措施。"
            rawTitle.contains("大雨") && rawTitle.contains("洪水") -> "大雨以及河川暴漲、氾濫風險高，請嚴加戒備。"
            rawTitle.contains("大雨") -> "請戒備土石流災害及低窪地區積淹水。"
            rawTitle.contains("洪水") -> "請戒備河川水位暴漲與氾濫。"
            rawTitle.contains("暴風雪") -> "強暴風雪恐導致嚴重交通受阻，請嚴加戒備。"
            rawTitle.contains("暴風") || rawTitle.contains("強風") -> "強烈風暴天氣，請留在室內並嚴加防範。"
            rawTitle.contains("大雪") -> "降雪劇烈且路面結冰，請注意交通安全。"
            rawTitle.contains("高潮") || rawTitle.contains("波浪") -> "暴潮巨浪來襲，嚴禁前往海邊與岸邊。"
            rawTitle.contains("雷") -> "雷擊劇烈並可能伴隨瞬間強陣風，請於室內避難。"
            rawTitle.contains("濃霧") -> "濃霧導致能見度極低，請特別注意交通安全。"
            rawTitle.contains("乾燥") -> "空氣乾燥火災風險高，請注意用火安全。"
            rawTitle.contains("なだれ") -> "山區有雪崩風險，請遠離積雪陡坡。"
            rawTitle.contains("融雪") -> "積雪融化可能引發淹水與土石崩塌，請注意防範。"
            rawTitle.contains("着氷") || rawTitle.contains("着雪") -> "電線與設施有結冰結雪之虞，請注意防範。"
            rawTitle.contains("霜") -> "地面可能出現霜凍，請注意農作物防霜害。"
            rawTitle.contains("低温") -> "低溫嚴寒天氣，請注意保暖及水管防凍破。"
            else -> "氣象廳已發布警報或注意報，請密切留意最新氣象資訊。"
        }
    }
}

private fun formatIsoAlertTimestamp(iso: String?): String? {
    val trimmed = iso?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return try {
        val instant = java.time.OffsetDateTime.parse(trimmed).toInstant()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", java.util.Locale.US)
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        try {
            val instant = java.time.Instant.parse(trimmed)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", java.util.Locale.US)
                .withZone(java.time.ZoneId.systemDefault())
            formatter.format(instant)
        } catch (_: Exception) {
            trimmed.take(16).replace("T", " ").ifBlank { null }
        }
    }
}

