package io.github.nemynew.expressiveweather.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.BugReportBuilder
import io.github.nemynew.expressiveweather.data.BugReportCategory
import io.github.nemynew.expressiveweather.data.BugReportSnapshot
import io.github.nemynew.expressiveweather.data.FeatureRequestCategory
import io.github.nemynew.expressiveweather.data.FeedbackMode
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics

/**
 * 正式リリース前（1.0.0 未満）限定のフィードバックカード（不具合報告 ＆ 機能要望）。
 * メイン画面のすべてのタイルの最下部に表示し、1 タップで不具合報告または要望シートを開く。
 */
@Composable
fun BugReportCardSection(
    snapshotProvider: () -> BugReportSnapshot,
    modifier: Modifier = Modifier,
    condition: WeatherCondition? = null,
    isDark: Boolean = false,
    isFrostedGlassEnabled: Boolean = true,
    hazeState: HazeState? = null
) {
    val language = LocalAppLanguage.current
    val expressHaptics = LocalExpressiveHaptics.current
    val colorScheme = MaterialTheme.colorScheme
    val isSnowing = condition == WeatherCondition.SNOW
    val cardColor = getFrostedCardColor(isFrostedGlassEnabled, isDark, colorScheme)
    val cardBorder = getFrostedCardBorder(isFrostedGlassEnabled, isDark)

    var activeSheetMode by rememberSaveable { mutableStateOf<FeedbackMode?>(null) }

    val cardTitle = remember(language) {
        when (language) {
            AppLanguage.JAPANESE -> "フィードバック"
            AppLanguage.SIMPLIFIED_CHINESE -> "反馈与建议"
            AppLanguage.TRADITIONAL_CHINESE -> "回饋與建議"
            AppLanguage.ENGLISH -> "Feedback"
        }
    }

    val badgeText = remember(language) {
        when (language) {
            AppLanguage.JAPANESE -> "プレリリース"
            AppLanguage.SIMPLIFIED_CHINESE -> "测试版"
            AppLanguage.TRADITIONAL_CHINESE -> "測試版"
            AppLanguage.ENGLISH -> "Pre-release"
        }
    }

    val subtitleText = remember(language) {
        when (language) {
            AppLanguage.JAPANESE -> "不具合の報告や機能のご要望をお知らせください"
            AppLanguage.SIMPLIFIED_CHINESE -> "欢迎随时反馈异常问题或功能建议"
            AppLanguage.TRADITIONAL_CHINESE -> "歡迎隨時回報異常問題或功能建議"
            AppLanguage.ENGLISH -> "Share bug reports or ideas for new features"
        }
    }

    val bugButtonText = remember(language) {
        when (language) {
            AppLanguage.JAPANESE -> "不具合を報告"
            AppLanguage.SIMPLIFIED_CHINESE -> "报告问题"
            AppLanguage.TRADITIONAL_CHINESE -> "回報問題"
            AppLanguage.ENGLISH -> "Report Bug"
        }
    }

    val featureButtonText = remember(language) {
        when (language) {
            AppLanguage.JAPANESE -> "要望を送る"
            AppLanguage.SIMPLIFIED_CHINESE -> "提交建议"
            AppLanguage.TRADITIONAL_CHINESE -> "提出建議"
            AppLanguage.ENGLISH -> "Request Feature"
        }
    }

    val cardShape = RoundedCornerShape(32.dp)
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .frostedGlassEffect(hazeState, isFrostedGlassEnabled, shape = cardShape)
            .snowTileAccumulation(
                isSnowing = isSnowing,
                isDark = isDark,
                shape = SnowTileShape.ROUNDED_RECT,
                cornerRadiusDp = 32.dp,
                seed = 109
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 左: 不具合を報告
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colorScheme.tertiaryContainer.copy(alpha = 0.82f),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(role = Role.Button, onClickLabel = bugButtonText) {
                            expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                            activeSheetMode = FeedbackMode.BUG_REPORT
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BugReport,
                            contentDescription = null,
                            tint = colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bugButtonText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 右: 要望を送る
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(role = Role.Button, onClickLabel = featureButtonText) {
                            expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                            activeSheetMode = FeedbackMode.FEATURE_REQUEST
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = featureButtonText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    activeSheetMode?.let { initialMode ->
        val snapshot = remember { snapshotProvider() }
        FeedbackBottomSheet(
            initialMode = initialMode,
            snapshot = snapshot,
            language = language,
            onDismiss = { activeSheetMode = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FeedbackBottomSheet(
    initialMode: FeedbackMode,
    snapshot: BugReportSnapshot,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val expressHaptics = LocalExpressiveHaptics.current
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var mode by rememberSaveable(initialMode) { mutableStateOf(initialMode) }

    // 不具合報告フィールド
    var selectedBugCategory by rememberSaveable { mutableStateOf(BugReportCategory.WEATHER_DATA) }
    var bugDescription by rememberSaveable { mutableStateOf("") }
    var bugExpected by rememberSaveable { mutableStateOf("") }
    var includeDeviceInfo by rememberSaveable { mutableStateOf(true) }
    var includeAppState by rememberSaveable { mutableStateOf(true) }
    var includeRecentLogs by rememberSaveable { mutableStateOf(false) }
    var showDiagnosticsPreview by rememberSaveable { mutableStateOf(false) }

    // 機能要望フィールド
    var selectedFeatureCategory by rememberSaveable { mutableStateOf(FeatureRequestCategory.UI_DESIGN) }
    var featureProposal by rememberSaveable { mutableStateOf("") }
    var featureProblem by rememberSaveable { mutableStateOf("") }
    var featureAlternatives by rememberSaveable { mutableStateOf("") }
    var includeFeatureAppInfo by rememberSaveable { mutableStateOf(true) }

    val diagnosticsPreview = remember(snapshot, includeDeviceInfo, includeAppState, includeRecentLogs) {
        BugReportBuilder.formatDiagnosticSummary(
            snapshot = snapshot,
            includeDeviceInfo = includeDeviceInfo,
            includeAppState = includeAppState,
            includeRecentLogs = includeRecentLogs
        )
    }

    val isBugMode = mode == FeedbackMode.BUG_REPORT

    val bugTabLabel = when (language) {
        AppLanguage.JAPANESE -> "不具合を報告"
        AppLanguage.SIMPLIFIED_CHINESE -> "报告问题"
        AppLanguage.TRADITIONAL_CHINESE -> "回報問題"
        AppLanguage.ENGLISH -> "Bug Report"
    }
    val featureTabLabel = when (language) {
        AppLanguage.JAPANESE -> "要望を送る"
        AppLanguage.SIMPLIFIED_CHINESE -> "功能建议"
        AppLanguage.TRADITIONAL_CHINESE -> "功能建議"
        AppLanguage.ENGLISH -> "Feature Request"
    }
    val sheetSubtitle = if (isBugMode) {
        when (language) {
            AppLanguage.JAPANESE -> "入力内容と診断情報をまとめて GitHub・コピー・共有から送信できます。正確な現在地や個人情報は含まれません。"
            AppLanguage.SIMPLIFIED_CHINESE -> "可将描述与诊断信息整理后通过 GitHub、复制或分享提交，不会包含精确位置或个人信息。"
            AppLanguage.TRADITIONAL_CHINESE -> "可將描述與診斷資訊整理後透過 GitHub、複製或分享提交，不會包含精確位置或個人資訊。"
            AppLanguage.ENGLISH -> "Submit via GitHub Issues, copy, or share. Exact location and personal data are never included."
        }
    } else {
        when (language) {
            AppLanguage.JAPANESE -> "追加してほしい機能や改善アイデアを GitHub・コピー・共有から送れます。"
            AppLanguage.SIMPLIFIED_CHINESE -> "可通过 GitHub、复制或分享提交新功能想法或改进建议。"
            AppLanguage.TRADITIONAL_CHINESE -> "可透過 GitHub、複製或分享提交新功能構想或改進建議。"
            AppLanguage.ENGLISH -> "Send new feature ideas or UI/data improvements via GitHub Issues, copy, or share."
        }
    }
    val categoryHeading = when (language) {
        AppLanguage.JAPANESE -> "カテゴリ"
        AppLanguage.SIMPLIFIED_CHINESE -> "分类"
        AppLanguage.TRADITIONAL_CHINESE -> "分類"
        AppLanguage.ENGLISH -> "Category"
    }
    val bugDescriptionLabel = when (language) {
        AppLanguage.JAPANESE -> "起きたこと・再現手順"
        AppLanguage.SIMPLIFIED_CHINESE -> "问题描述与复现步骤"
        AppLanguage.TRADITIONAL_CHINESE -> "問題描述與重現步驟"
        AppLanguage.ENGLISH -> "What happened & steps to reproduce"
    }
    val bugDescriptionPlaceholder = when (language) {
        AppLanguage.JAPANESE -> "例: レーダー画面を開いたときに〜が起きる"
        AppLanguage.SIMPLIFIED_CHINESE -> "例：打开雷达页面时出现..."
        AppLanguage.TRADITIONAL_CHINESE -> "例：開啟雷達頁面時出現..."
        AppLanguage.ENGLISH -> "e.g. Opening the radar screen causes..."
    }
    val bugExpectedLabel = when (language) {
        AppLanguage.JAPANESE -> "期待していた動作（任意）"
        AppLanguage.SIMPLIFIED_CHINESE -> "预期行为（选填）"
        AppLanguage.TRADITIONAL_CHINESE -> "預期行為（選填）"
        AppLanguage.ENGLISH -> "Expected behavior (optional)"
    }
    val featureProposalLabel = when (language) {
        AppLanguage.JAPANESE -> "ほしい機能・改善案"
        AppLanguage.SIMPLIFIED_CHINESE -> "期望的新功能或改进方案"
        AppLanguage.TRADITIONAL_CHINESE -> "期望的新功能或改進方案"
        AppLanguage.ENGLISH -> "Proposed feature or improvement"
    }
    val featureProposalPlaceholder = when (language) {
        AppLanguage.JAPANESE -> "例: ウィジェットに降水確率グラフも表示したい"
        AppLanguage.SIMPLIFIED_CHINESE -> "例：希望微件也能显示降水概率趋势"
        AppLanguage.TRADITIONAL_CHINESE -> "例：希望小工具也能顯示降雨機率趨勢"
        AppLanguage.ENGLISH -> "e.g. Add a precipitation probability chart to widgets"
    }
    val featureProblemLabel = when (language) {
        AppLanguage.JAPANESE -> "背景・困っていること（任意）"
        AppLanguage.SIMPLIFIED_CHINESE -> "使用场景或痛点（选填）"
        AppLanguage.TRADITIONAL_CHINESE -> "使用情境或痛點（選填）"
        AppLanguage.ENGLISH -> "Problem or motivation (optional)"
    }
    val featureAlternativesLabel = when (language) {
        AppLanguage.JAPANESE -> "補足・参考データソースなど（任意）"
        AppLanguage.SIMPLIFIED_CHINESE -> "补充说明或参考数据源（选填）"
        AppLanguage.TRADITIONAL_CHINESE -> "補充說明或參考資料來源（選填）"
        AppLanguage.ENGLISH -> "Additional context or data sources (optional)"
    }
    val diagnosticsHeading = when (language) {
        AppLanguage.JAPANESE -> "添付する診断情報"
        AppLanguage.SIMPLIFIED_CHINESE -> "附带的诊断信息"
        AppLanguage.TRADITIONAL_CHINESE -> "附帶的診斷資訊"
        AppLanguage.ENGLISH -> "Attached Diagnostics"
    }
    val includeDeviceLabel = when (language) {
        AppLanguage.JAPANESE -> "端末・アプリバージョン（${snapshot.versionName}）"
        AppLanguage.SIMPLIFIED_CHINESE -> "设备与应用版本（${snapshot.versionName}）"
        AppLanguage.TRADITIONAL_CHINESE -> "裝置與應用程式版本（${snapshot.versionName}）"
        AppLanguage.ENGLISH -> "Device & app version (${snapshot.versionName})"
    }
    val includeStateLabel = when (language) {
        AppLanguage.JAPANESE -> "気象ソース・設定状態（現在地座標は除外）"
        AppLanguage.SIMPLIFIED_CHINESE -> "数据源与设置状态（不含精确坐标）"
        AppLanguage.TRADITIONAL_CHINESE -> "資料來源與設定狀態（不含精確座標）"
        AppLanguage.ENGLISH -> "Provider & app settings (no coordinates)"
    }
    val includeLogsLabel = when (language) {
        AppLanguage.JAPANESE -> "直近のアプリ内エラーログ（${snapshot.recentLogs.size}件）"
        AppLanguage.SIMPLIFIED_CHINESE -> "近期应用内错误日志（${snapshot.recentLogs.size}条）"
        AppLanguage.TRADITIONAL_CHINESE -> "近期應用程式內錯誤記錄（${snapshot.recentLogs.size}筆）"
        AppLanguage.ENGLISH -> "Recent in-app error logs (${snapshot.recentLogs.size})"
    }
    val includeFeatureAppInfoLabel = when (language) {
        AppLanguage.JAPANESE -> "アプリ基本情報を添付（Ver ${snapshot.versionName}・言語）"
        AppLanguage.SIMPLIFIED_CHINESE -> "附带应用基本信息（Ver ${snapshot.versionName}・语言）"
        AppLanguage.TRADITIONAL_CHINESE -> "附帶應用程式基本資訊（Ver ${snapshot.versionName}・語言）"
        AppLanguage.ENGLISH -> "Include basic app info (Ver ${snapshot.versionName}, language)"
    }
    val previewToggleLabel = when (language) {
        AppLanguage.JAPANESE -> "送信される診断テキストを確認"
        AppLanguage.SIMPLIFIED_CHINESE -> "预览附带的诊断文本"
        AppLanguage.TRADITIONAL_CHINESE -> "預覽附帶的診斷文字"
        AppLanguage.ENGLISH -> "Preview diagnostic text"
    }
    val openGitHubLabel = if (isBugMode) {
        when (language) {
            AppLanguage.JAPANESE -> "GitHub で報告（自動入力）"
            AppLanguage.SIMPLIFIED_CHINESE -> "通过 GitHub 提交（自动填充）"
            AppLanguage.TRADITIONAL_CHINESE -> "透過 GitHub 回報（自動填入）"
            AppLanguage.ENGLISH -> "Report on GitHub (Pre-filled)"
        }
    } else {
        when (language) {
            AppLanguage.JAPANESE -> "GitHub で要望を送る（自動入力）"
            AppLanguage.SIMPLIFIED_CHINESE -> "通过 GitHub 提交建议（自动填充）"
            AppLanguage.TRADITIONAL_CHINESE -> "透過 GitHub 提出建議（自動填入）"
            AppLanguage.ENGLISH -> "Submit Feature Request on GitHub"
        }
    }
    val copyLabel = when (language) {
        AppLanguage.JAPANESE -> "コピー"
        AppLanguage.SIMPLIFIED_CHINESE -> "复制"
        AppLanguage.TRADITIONAL_CHINESE -> "複製"
        AppLanguage.ENGLISH -> "Copy"
    }
    val shareLabel = when (language) {
        AppLanguage.JAPANESE -> "共有"
        AppLanguage.SIMPLIFIED_CHINESE -> "分享"
        AppLanguage.TRADITIONAL_CHINESE -> "分享"
        AppLanguage.ENGLISH -> "Share"
    }
    val copiedToast = if (isBugMode) {
        when (language) {
            AppLanguage.JAPANESE -> "バグレポートをコピーしました"
            AppLanguage.SIMPLIFIED_CHINESE -> "已复制问题报告"
            AppLanguage.TRADITIONAL_CHINESE -> "已複製問題回報"
            AppLanguage.ENGLISH -> "Bug report copied to clipboard"
        }
    } else {
        when (language) {
            AppLanguage.JAPANESE -> "要望テキストをコピーしました"
            AppLanguage.SIMPLIFIED_CHINESE -> "已复制功能建议"
            AppLanguage.TRADITIONAL_CHINESE -> "已複製功能建議"
            AppLanguage.ENGLISH -> "Feature request copied to clipboard"
        }
    }
    val openBrowserFailedToast = when (language) {
        AppLanguage.JAPANESE -> "ブラウザを開けませんでした。「コピー」をご利用ください。"
        AppLanguage.SIMPLIFIED_CHINESE -> "无法打开浏览器，请使用复制功能。"
        AppLanguage.TRADITIONAL_CHINESE -> "無法開啟瀏覽器，請使用複製功能。"
        AppLanguage.ENGLISH -> "Could not open browser. Please use Copy instead."
    }

    fun buildCurrentPlainText(): String {
        return if (isBugMode) {
            BugReportBuilder.formatPlainTextReport(
                category = selectedBugCategory,
                description = bugDescription,
                expected = bugExpected,
                snapshot = snapshot,
                includeDeviceInfo = includeDeviceInfo,
                includeAppState = includeAppState,
                includeRecentLogs = includeRecentLogs
            )
        } else {
            BugReportBuilder.formatPlainTextFeatureRequest(
                category = selectedFeatureCategory,
                proposal = featureProposal,
                problem = featureProblem,
                alternatives = featureAlternatives,
                snapshot = snapshot,
                includeAppInfo = includeFeatureAppInfo
            )
        }
    }

    fun buildCurrentGitHubUrl(): String {
        return if (isBugMode) {
            BugReportBuilder.buildGitHubIssueUrl(
                category = selectedBugCategory,
                description = bugDescription,
                expected = bugExpected,
                snapshot = snapshot,
                includeDeviceInfo = includeDeviceInfo,
                includeAppState = includeAppState,
                includeRecentLogs = includeRecentLogs
            )
        } else {
            BugReportBuilder.buildGitHubFeatureRequestUrl(
                category = selectedFeatureCategory,
                proposal = featureProposal,
                problem = featureProblem,
                alternatives = featureAlternatives,
                snapshot = snapshot,
                includeAppInfo = includeFeatureAppInfo
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ヘッダー
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isBugMode) colorScheme.tertiaryContainer else colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBugMode) Icons.Rounded.BugReport else Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = if (isBugMode) colorScheme.onTertiaryContainer else colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isBugMode) bugTabLabel else featureTabLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }

            // モード切替（不具合を報告 / 要望を送る）
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isBugMode,
                    onClick = {
                        expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                        mode = FeedbackMode.BUG_REPORT
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = isBugMode) {
                            Icon(
                                imageVector = Icons.Rounded.BugReport,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                ) {
                    Text(
                        text = bugTabLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SegmentedButton(
                    selected = !isBugMode,
                    onClick = {
                        expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                        mode = FeedbackMode.FEATURE_REQUEST
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = !isBugMode) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                ) {
                    Text(
                        text = featureTabLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = sheetSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            if (isBugMode) {
                // ================= 不具合報告フォーム =================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = categoryHeading,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BugReportCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedBugCategory == category,
                                onClick = {
                                    expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                                    selectedBugCategory = category
                                },
                                label = {
                                    Text(
                                        text = category.label(language),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = bugDescription,
                    onValueChange = { bugDescription = it },
                    label = { Text(bugDescriptionLabel) },
                    placeholder = { Text(bugDescriptionPlaceholder) },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bugExpected,
                    onValueChange = { bugExpected = it },
                    label = { Text(bugExpectedLabel) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = diagnosticsHeading,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )

                        DiagnosticCheckboxRow(
                            label = includeDeviceLabel,
                            checked = includeDeviceInfo,
                            onCheckedChange = { includeDeviceInfo = it }
                        )
                        DiagnosticCheckboxRow(
                            label = includeStateLabel,
                            checked = includeAppState,
                            onCheckedChange = { includeAppState = it }
                        )
                        if (snapshot.recentLogs.isNotEmpty()) {
                            DiagnosticCheckboxRow(
                                label = includeLogsLabel,
                                checked = includeRecentLogs,
                                onCheckedChange = { includeRecentLogs = it }
                            )
                        }

                        if (diagnosticsPreview.isNotBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                            val arrowRotation by animateFloatAsState(
                                targetValue = if (showDiagnosticsPreview) 180f else 0f,
                                label = "DiagnosticsPreviewArrow"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showDiagnosticsPreview = !showDiagnosticsPreview }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = previewToggleLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .rotate(arrowRotation)
                                )
                            }

                            AnimatedVisibility(
                                visible = showDiagnosticsPreview,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colorScheme.surfaceContainerLowest,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = diagnosticsPreview,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ================= 機能要望フォーム =================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = categoryHeading,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FeatureRequestCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedFeatureCategory == category,
                                onClick = {
                                    expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                                    selectedFeatureCategory = category
                                },
                                label = {
                                    Text(
                                        text = category.label(language),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = featureProposal,
                    onValueChange = { featureProposal = it },
                    label = { Text(featureProposalLabel) },
                    placeholder = { Text(featureProposalPlaceholder) },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = featureProblem,
                    onValueChange = { featureProblem = it },
                    label = { Text(featureProblemLabel) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = featureAlternatives,
                    onValueChange = { featureAlternatives = it },
                    label = { Text(featureAlternativesLabel) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        DiagnosticCheckboxRow(
                            label = includeFeatureAppInfoLabel,
                            checked = includeFeatureAppInfo,
                            onCheckedChange = { includeFeatureAppInfo = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // プライマリ送信ボタン: GitHub Issues（既存テンプレート自動入力）
            Button(
                onClick = {
                    expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                    val issueUrl = buildCurrentGitHubUrl()
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(issueUrl))
                        context.startActivity(intent)
                        onDismiss()
                    } catch (_: Exception) {
                        Toast.makeText(context, openBrowserFailedToast, Toast.LENGTH_LONG).show()
                    }
                },
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = openGitHubLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // セカンダリボタン: コピー / 他アプリで共有
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                        val text = buildCurrentPlainText()
                        val label = if (isBugMode) "Expressive Weather Bug Report" else "Expressive Weather Feature Request"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
                        Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = copyLabel, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        expressHaptics.play(ExpressiveHapticPattern.SOFT_TAP)
                        val text = buildCurrentPlainText()
                        val subject = if (isBugMode) {
                            "[Expressive Weather] ${selectedBugCategory.issueTag}"
                        } else {
                            "[Expressive Weather Request] ${selectedFeatureCategory.issueTag}"
                        }
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, subject)
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, shareLabel))
                        } catch (_: Exception) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText(subject, text))
                            Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = shareLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
