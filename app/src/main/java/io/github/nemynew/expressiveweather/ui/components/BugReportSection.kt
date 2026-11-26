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
            AppLanguage.KOREAN -> "피드백"
            AppLanguage.SIMPLIFIED_CHINESE -> "反馈与建议"
            AppLanguage.TRADITIONAL_CHINESE -> "回饋與建議"
            AppLanguage.SPANISH -> "Comentarios"
            AppLanguage.FRENCH -> "Retours"
            AppLanguage.PORTUGUESE -> "Feedback"
            AppLanguage.RUSSIAN -> "Обратная связь"
            AppLanguage.ENGLISH -> "Feedback"
        }
    }

    val badgeText = remember(language) {
        when (language) {
            AppLanguage.JAPANESE -> "プレリリース"
            AppLanguage.KOREAN -> "프리릴리스"
            AppLanguage.SIMPLIFIED_CHINESE -> "测试版"
            AppLanguage.TRADITIONAL_CHINESE -> "測試版"
            AppLanguage.SPANISH -> "Prelanzamiento"
            AppLanguage.FRENCH -> "Préversion"
            AppLanguage.PORTUGUESE -> "Pré-lançamento"
            AppLanguage.RUSSIAN -> "Пререлиз"
            AppLanguage.ENGLISH -> "Pre-release"
        }
    }

    val subtitleText = remember(language) {
        when (language) {
            AppLanguage.JAPANESE -> "不具合の報告や機能のご要望をお知らせください"
            AppLanguage.KOREAN -> "버그 제보나 기능 제안을 남겨주세요"
            AppLanguage.SIMPLIFIED_CHINESE -> "欢迎随时反馈异常问题或功能建议"
            AppLanguage.TRADITIONAL_CHINESE -> "歡迎隨時回報異常問題或功能建議"
            AppLanguage.SPANISH -> "Envía reportes de errores o ideas de nuevas funciones"
            AppLanguage.FRENCH -> "Signalez des bugs ou partagez vos idées de fonctionnalités"
            AppLanguage.PORTUGUESE -> "Envie relatórios de bugs ou sugestões de recursos"
            AppLanguage.RUSSIAN -> "Сообщите об ошибке или предложите новую функцию"
            AppLanguage.ENGLISH -> "Share bug reports or ideas for new features"
        }
    }

    val bugButtonText = remember(language) {
        when (language) {
            AppLanguage.JAPANESE -> "不具合を報告"
            AppLanguage.KOREAN -> "버그 제보"
            AppLanguage.SIMPLIFIED_CHINESE -> "报告问题"
            AppLanguage.TRADITIONAL_CHINESE -> "回報問題"
            AppLanguage.SPANISH -> "Reportar error"
            AppLanguage.FRENCH -> "Signaler un bug"
            AppLanguage.PORTUGUESE -> "Reportar bug"
            AppLanguage.RUSSIAN -> "Сообщить об ошибке"
            AppLanguage.ENGLISH -> "Report Bug"
        }
    }

    val featureButtonText = remember(language) {
        when (language) {
            AppLanguage.JAPANESE -> "要望を送る"
            AppLanguage.KOREAN -> "기능 제안"
            AppLanguage.SIMPLIFIED_CHINESE -> "提交建议"
            AppLanguage.TRADITIONAL_CHINESE -> "提出建議"
            AppLanguage.SPANISH -> "Sugerir función"
            AppLanguage.FRENCH -> "Suggérer"
            AppLanguage.PORTUGUESE -> "Sugerir recurso"
            AppLanguage.RUSSIAN -> "Предложить идею"
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
        AppLanguage.KOREAN -> "버그 제보"
        AppLanguage.SIMPLIFIED_CHINESE -> "报告问题"
        AppLanguage.TRADITIONAL_CHINESE -> "回報問題"
        AppLanguage.SPANISH -> "Reportar error"
        AppLanguage.FRENCH -> "Signaler un bug"
        AppLanguage.PORTUGUESE -> "Reportar bug"
        AppLanguage.RUSSIAN -> "Ошибка"
        AppLanguage.ENGLISH -> "Bug Report"
    }
    val featureTabLabel = when (language) {
        AppLanguage.JAPANESE -> "要望を送る"
        AppLanguage.KOREAN -> "기능 제안"
        AppLanguage.SIMPLIFIED_CHINESE -> "功能建议"
        AppLanguage.TRADITIONAL_CHINESE -> "功能建議"
        AppLanguage.SPANISH -> "Sugerir función"
        AppLanguage.FRENCH -> "Suggestion"
        AppLanguage.PORTUGUESE -> "Sugerir recurso"
        AppLanguage.RUSSIAN -> "Идея"
        AppLanguage.ENGLISH -> "Feature Request"
    }
    val sheetSubtitle = if (isBugMode) {
        when (language) {
            AppLanguage.JAPANESE -> "入力内容と診断情報をまとめて GitHub・コピー・共有から送信できます。正確な現在地や個人情報は含まれません。"
            AppLanguage.KOREAN -> "입력한 내용과 진단 정보를 모아 GitHub, 복사 또는 공유로 전송할 수 있습니다. 정확한 위치 정보나 개인정보는 포함되지 않습니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "可将描述与诊断信息整理后通过 GitHub、复制或分享提交，不会包含精确位置或个人信息。"
            AppLanguage.TRADITIONAL_CHINESE -> "可將描述與診斷資訊整理後透過 GitHub、複製或分享提交，不會包含精確位置或個人資訊。"
            AppLanguage.SPANISH -> "Envíalo por GitHub Issues, cópialo o compártelo. Nunca se incluyen tu ubicación exacta ni datos personales."
            AppLanguage.FRENCH -> "Envoyez via GitHub Issues, copiez ou partagez. Votre position exacte et vos données personnelles ne sont jamais incluses."
            AppLanguage.PORTUGUESE -> "Envie pelo GitHub Issues, copie ou compartilhe. Localização exata e dados pessoais nunca são incluídos."
            AppLanguage.RUSSIAN -> "Отправьте через GitHub Issues, скопируйте или поделитесь. Точное местоположение и личные данные не включаются."
            AppLanguage.ENGLISH -> "Submit via GitHub Issues, copy, or share. Exact location and personal data are never included."
        }
    } else {
        when (language) {
            AppLanguage.JAPANESE -> "追加してほしい機能や改善アイデアを GitHub・コピー・共有から送れます。"
            AppLanguage.KOREAN -> "원하는 기능이나 UI·데이터 개선 아이디어를 GitHub, 복사 또는 공유로 보낼 수 있습니다."
            AppLanguage.SIMPLIFIED_CHINESE -> "可通过 GitHub、复制或分享提交新功能想法或改进建议。"
            AppLanguage.TRADITIONAL_CHINESE -> "可透過 GitHub、複製或分享提交新功能構想或改進建議。"
            AppLanguage.SPANISH -> "Envía ideas de nuevas funciones o mejoras de interfaz y datos por GitHub Issues, copia o compartir."
            AppLanguage.FRENCH -> "Envoyez vos idées de fonctionnalités ou améliorations via GitHub Issues, copier ou partager."
            AppLanguage.PORTUGUESE -> "Envie ideias de novos recursos ou melhorias de interface e dados pelo GitHub Issues, copiar ou compartilhar."
            AppLanguage.RUSSIAN -> "Отправляйте идеи новых функций и улучшений интерфейса или данных через GitHub Issues, копирование или «Поделиться»."
            AppLanguage.ENGLISH -> "Send new feature ideas or UI/data improvements via GitHub Issues, copy, or share."
        }
    }
    val categoryHeading = when (language) {
        AppLanguage.JAPANESE -> "カテゴリ"
        AppLanguage.KOREAN -> "카테고리"
        AppLanguage.SIMPLIFIED_CHINESE -> "分类"
        AppLanguage.TRADITIONAL_CHINESE -> "分類"
        AppLanguage.SPANISH -> "Categoría"
        AppLanguage.FRENCH -> "Catégorie"
        AppLanguage.PORTUGUESE -> "Categoria"
        AppLanguage.RUSSIAN -> "Категория"
        AppLanguage.ENGLISH -> "Category"
    }
    val bugDescriptionLabel = when (language) {
        AppLanguage.JAPANESE -> "起きたこと・再現手順"
        AppLanguage.KOREAN -> "발생한 문제 및 재현 방법"
        AppLanguage.SIMPLIFIED_CHINESE -> "问题描述与复现步骤"
        AppLanguage.TRADITIONAL_CHINESE -> "問題描述與重現步驟"
        AppLanguage.SPANISH -> "Qué ocurrió y pasos para reproducirlo"
        AppLanguage.FRENCH -> "Problème rencontré et étapes pour le reproduire"
        AppLanguage.PORTUGUESE -> "O que aconteceu e passos para reproduzir"
        AppLanguage.RUSSIAN -> "Что произошло и шаги для воспроизведения"
        AppLanguage.ENGLISH -> "What happened & steps to reproduce"
    }
    val bugDescriptionPlaceholder = when (language) {
        AppLanguage.JAPANESE -> "例: レーダー画面を開いたときに〜が起きる"
        AppLanguage.KOREAN -> "예: 레이더 화면을 열면 ... 현상이 발생합니다"
        AppLanguage.SIMPLIFIED_CHINESE -> "例：打开雷达页面时出现..."
        AppLanguage.TRADITIONAL_CHINESE -> "例：開啟雷達頁面時出現..."
        AppLanguage.SPANISH -> "Ej.: Al abrir la pantalla del radar ocurre..."
        AppLanguage.FRENCH -> "Ex. : Ouvrir l'écran radar provoque..."
        AppLanguage.PORTUGUESE -> "Ex.: Ao abrir a tela do radar acontece..."
        AppLanguage.RUSSIAN -> "Напр.: при открытии экрана радара происходит..."
        AppLanguage.ENGLISH -> "e.g. Opening the radar screen causes..."
    }
    val bugExpectedLabel = when (language) {
        AppLanguage.JAPANESE -> "期待していた動作（任意）"
        AppLanguage.KOREAN -> "기대했던 동작 (선택)"
        AppLanguage.SIMPLIFIED_CHINESE -> "预期行为（选填）"
        AppLanguage.TRADITIONAL_CHINESE -> "預期行為（選填）"
        AppLanguage.SPANISH -> "Comportamiento esperado (opcional)"
        AppLanguage.FRENCH -> "Comportement attendu (facultatif)"
        AppLanguage.PORTUGUESE -> "Comportamento esperado (opcional)"
        AppLanguage.RUSSIAN -> "Ожидаемое поведение (необязательно)"
        AppLanguage.ENGLISH -> "Expected behavior (optional)"
    }
    val featureProposalLabel = when (language) {
        AppLanguage.JAPANESE -> "ほしい機能・改善案"
        AppLanguage.KOREAN -> "원하는 기능 또는 개선 사항"
        AppLanguage.SIMPLIFIED_CHINESE -> "期望的新功能或改进方案"
        AppLanguage.TRADITIONAL_CHINESE -> "期望的新功能或改進方案"
        AppLanguage.SPANISH -> "Función o mejora propuesta"
        AppLanguage.FRENCH -> "Fonctionnalité ou amélioration proposée"
        AppLanguage.PORTUGUESE -> "Recurso ou melhoria proposta"
        AppLanguage.RUSSIAN -> "Предлагаемая функция или улучшение"
        AppLanguage.ENGLISH -> "Proposed feature or improvement"
    }
    val featureProposalPlaceholder = when (language) {
        AppLanguage.JAPANESE -> "例: ウィジェットに降水確率グラフも表示したい"
        AppLanguage.KOREAN -> "예: 위젯에도 강수 확률 그래프를 표시해 주세요"
        AppLanguage.SIMPLIFIED_CHINESE -> "例：希望微件也能显示降水概率趋势"
        AppLanguage.TRADITIONAL_CHINESE -> "例：希望小工具也能顯示降雨機率趨勢"
        AppLanguage.SPANISH -> "Ej.: Añadir un gráfico de probabilidad de lluvia a los widgets"
        AppLanguage.FRENCH -> "Ex. : Ajouter un graphique de probabilité de pluie aux widgets"
        AppLanguage.PORTUGUESE -> "Ex.: Adicionar um gráfico de probabilidade de chuva aos widgets"
        AppLanguage.RUSSIAN -> "Напр.: добавить график вероятности осадков в виджеты"
        AppLanguage.ENGLISH -> "e.g. Add a precipitation probability chart to widgets"
    }
    val featureProblemLabel = when (language) {
        AppLanguage.JAPANESE -> "背景・困っていること（任意）"
        AppLanguage.KOREAN -> "배경 또는 불편한 점 (선택)"
        AppLanguage.SIMPLIFIED_CHINESE -> "使用场景或痛点（选填）"
        AppLanguage.TRADITIONAL_CHINESE -> "使用情境或痛點（選填）"
        AppLanguage.SPANISH -> "Problema o motivación (opcional)"
        AppLanguage.FRENCH -> "Problème ou motivation (facultatif)"
        AppLanguage.PORTUGUESE -> "Problema ou motivação (opcional)"
        AppLanguage.RUSSIAN -> "Проблема или мотивация (необязательно)"
        AppLanguage.ENGLISH -> "Problem or motivation (optional)"
    }
    val featureAlternativesLabel = when (language) {
        AppLanguage.JAPANESE -> "補足・参考データソースなど（任意）"
        AppLanguage.KOREAN -> "추가 설명 또는 참고 데이터 소스 (선택)"
        AppLanguage.SIMPLIFIED_CHINESE -> "补充说明或参考数据源（选填）"
        AppLanguage.TRADITIONAL_CHINESE -> "補充說明或參考資料來源（選填）"
        AppLanguage.SPANISH -> "Contexto adicional o fuentes de datos (opcional)"
        AppLanguage.FRENCH -> "Contexte supplémentaire ou sources de données (facultatif)"
        AppLanguage.PORTUGUESE -> "Contexto adicional ou fontes de dados (opcional)"
        AppLanguage.RUSSIAN -> "Доп. контекст или источники данных (необязательно)"
        AppLanguage.ENGLISH -> "Additional context or data sources (optional)"
    }
    val diagnosticsHeading = when (language) {
        AppLanguage.JAPANESE -> "添付する診断情報"
        AppLanguage.KOREAN -> "첨부할 진단 정보"
        AppLanguage.SIMPLIFIED_CHINESE -> "附带的诊断信息"
        AppLanguage.TRADITIONAL_CHINESE -> "附帶的診斷資訊"
        AppLanguage.SPANISH -> "Diagnóstico adjunto"
        AppLanguage.FRENCH -> "Diagnostics joints"
        AppLanguage.PORTUGUESE -> "Diagnósticos anexados"
        AppLanguage.RUSSIAN -> "Прилагаемая диагностика"
        AppLanguage.ENGLISH -> "Attached Diagnostics"
    }
    val includeDeviceLabel = when (language) {
        AppLanguage.JAPANESE -> "端末・アプリバージョン（${snapshot.versionName}）"
        AppLanguage.KOREAN -> "기기 및 앱 버전 (${snapshot.versionName})"
        AppLanguage.SIMPLIFIED_CHINESE -> "设备与应用版本（${snapshot.versionName}）"
        AppLanguage.TRADITIONAL_CHINESE -> "裝置與應用程式版本（${snapshot.versionName}）"
        AppLanguage.SPANISH -> "Dispositivo y versión de la app (${snapshot.versionName})"
        AppLanguage.FRENCH -> "Appareil et version de l'application (${snapshot.versionName})"
        AppLanguage.PORTUGUESE -> "Dispositivo e versão do app (${snapshot.versionName})"
        AppLanguage.RUSSIAN -> "Устройство и версия приложения (${snapshot.versionName})"
        AppLanguage.ENGLISH -> "Device & app version (${snapshot.versionName})"
    }
    val includeStateLabel = when (language) {
        AppLanguage.JAPANESE -> "気象ソース・設定状態（現在地座標は除外）"
        AppLanguage.KOREAN -> "기상 소스 및 앱 설정 (정확한 좌표 제외)"
        AppLanguage.SIMPLIFIED_CHINESE -> "数据源与设置状态（不含精确坐标）"
        AppLanguage.TRADITIONAL_CHINESE -> "資料來源與設定狀態（不含精確座標）"
        AppLanguage.SPANISH -> "Proveedor y ajustes de la app (sin coordenadas)"
        AppLanguage.FRENCH -> "Fournisseur et paramètres de l'app (sans coordonnées)"
        AppLanguage.PORTUGUESE -> "Provedor e configurações do app (sem coordenadas)"
        AppLanguage.RUSSIAN -> "Источник данных и настройки (без координат)"
        AppLanguage.ENGLISH -> "Provider & app settings (no coordinates)"
    }
    val includeLogsLabel = when (language) {
        AppLanguage.JAPANESE -> "直近のアプリ内エラーログ（${snapshot.recentLogs.size}件）"
        AppLanguage.KOREAN -> "최근 앱 내 오류 로그 (${snapshot.recentLogs.size}건)"
        AppLanguage.SIMPLIFIED_CHINESE -> "近期应用内错误日志（${snapshot.recentLogs.size}条）"
        AppLanguage.TRADITIONAL_CHINESE -> "近期應用程式內錯誤記錄（${snapshot.recentLogs.size}筆）"
        AppLanguage.SPANISH -> "Registros recientes de errores (${snapshot.recentLogs.size})"
        AppLanguage.FRENCH -> "Journaux d'erreurs récents (${snapshot.recentLogs.size})"
        AppLanguage.PORTUGUESE -> "Logs de erros recentes do app (${snapshot.recentLogs.size})"
        AppLanguage.RUSSIAN -> "Недавние журналы ошибок (${snapshot.recentLogs.size})"
        AppLanguage.ENGLISH -> "Recent in-app error logs (${snapshot.recentLogs.size})"
    }
    val includeFeatureAppInfoLabel = when (language) {
        AppLanguage.JAPANESE -> "アプリ基本情報を添付（Ver ${snapshot.versionName}・言語）"
        AppLanguage.KOREAN -> "기본 앱 정보 첨부 (Ver ${snapshot.versionName} · 언어)"
        AppLanguage.SIMPLIFIED_CHINESE -> "附带应用基本信息（Ver ${snapshot.versionName}・语言）"
        AppLanguage.TRADITIONAL_CHINESE -> "附帶應用程式基本資訊（Ver ${snapshot.versionName}・語言）"
        AppLanguage.SPANISH -> "Incluir info básica de la app (Ver ${snapshot.versionName}, idioma)"
        AppLanguage.FRENCH -> "Inclure les infos de base (Ver ${snapshot.versionName}, langue)"
        AppLanguage.PORTUGUESE -> "Incluir info básica do app (Ver ${snapshot.versionName}, idioma)"
        AppLanguage.RUSSIAN -> "Приложить базовые данные (Ver ${snapshot.versionName}, язык)"
        AppLanguage.ENGLISH -> "Include basic app info (Ver ${snapshot.versionName}, language)"
    }
    val previewToggleLabel = when (language) {
        AppLanguage.JAPANESE -> "送信される診断テキストを確認"
        AppLanguage.KOREAN -> "전송될 진단 텍스트 미리보기"
        AppLanguage.SIMPLIFIED_CHINESE -> "预览附带的诊断文本"
        AppLanguage.TRADITIONAL_CHINESE -> "預覽附帶的診斷文字"
        AppLanguage.SPANISH -> "Vista previa del texto de diagnóstico"
        AppLanguage.FRENCH -> "Aperçu du texte de diagnostic"
        AppLanguage.PORTUGUESE -> "Pré-visualizar texto de diagnóstico"
        AppLanguage.RUSSIAN -> "Просмотр текста диагностики"
        AppLanguage.ENGLISH -> "Preview diagnostic text"
    }
    val openGitHubLabel = if (isBugMode) {
        when (language) {
            AppLanguage.JAPANESE -> "GitHub で報告（自動入力）"
            AppLanguage.KOREAN -> "GitHub에 제보 (자동 입력)"
            AppLanguage.SIMPLIFIED_CHINESE -> "通过 GitHub 提交（自动填充）"
            AppLanguage.TRADITIONAL_CHINESE -> "透過 GitHub 回報（自動填入）"
            AppLanguage.SPANISH -> "Reportar en GitHub (prellenado)"
            AppLanguage.FRENCH -> "Signaler sur GitHub (prérempli)"
            AppLanguage.PORTUGUESE -> "Reportar no GitHub (pré-preenchido)"
            AppLanguage.RUSSIAN -> "Отправить на GitHub (автозаполнение)"
            AppLanguage.ENGLISH -> "Report on GitHub (Pre-filled)"
        }
    } else {
        when (language) {
            AppLanguage.JAPANESE -> "GitHub で要望を送る（自動入力）"
            AppLanguage.KOREAN -> "GitHub에 기능 제안 (자동 입력)"
            AppLanguage.SIMPLIFIED_CHINESE -> "通过 GitHub 提交建议（自动填充）"
            AppLanguage.TRADITIONAL_CHINESE -> "透過 GitHub 提出建議（自動填入）"
            AppLanguage.SPANISH -> "Enviar sugerencia en GitHub"
            AppLanguage.FRENCH -> "Envoyer la suggestion sur GitHub"
            AppLanguage.PORTUGUESE -> "Enviar sugestão no GitHub"
            AppLanguage.RUSSIAN -> "Отправить идею на GitHub"
            AppLanguage.ENGLISH -> "Submit Feature Request on GitHub"
        }
    }
    val copyLabel = when (language) {
        AppLanguage.JAPANESE -> "コピー"
        AppLanguage.KOREAN -> "복사"
        AppLanguage.SIMPLIFIED_CHINESE -> "复制"
        AppLanguage.TRADITIONAL_CHINESE -> "複製"
        AppLanguage.SPANISH -> "Copiar"
        AppLanguage.FRENCH -> "Copier"
        AppLanguage.PORTUGUESE -> "Copiar"
        AppLanguage.RUSSIAN -> "Копировать"
        AppLanguage.ENGLISH -> "Copy"
    }
    val shareLabel = when (language) {
        AppLanguage.JAPANESE -> "共有"
        AppLanguage.KOREAN -> "공유"
        AppLanguage.SIMPLIFIED_CHINESE -> "分享"
        AppLanguage.TRADITIONAL_CHINESE -> "分享"
        AppLanguage.SPANISH -> "Compartir"
        AppLanguage.FRENCH -> "Partager"
        AppLanguage.PORTUGUESE -> "Compartilhar"
        AppLanguage.RUSSIAN -> "Поделиться"
        AppLanguage.ENGLISH -> "Share"
    }
    val copiedToast = if (isBugMode) {
        when (language) {
            AppLanguage.JAPANESE -> "バグレポートをコピーしました"
            AppLanguage.KOREAN -> "버그 리포트를 복사했습니다"
            AppLanguage.SIMPLIFIED_CHINESE -> "已复制问题报告"
            AppLanguage.TRADITIONAL_CHINESE -> "已複製問題回報"
            AppLanguage.SPANISH -> "Reporte de error copiado al portapapeles"
            AppLanguage.FRENCH -> "Rapport de bug copié dans le presse-papiers"
            AppLanguage.PORTUGUESE -> "Relatório de bug copiado para a área de transferência"
            AppLanguage.RUSSIAN -> "Отчёт об ошибке скопирован в буфер обмена"
            AppLanguage.ENGLISH -> "Bug report copied to clipboard"
        }
    } else {
        when (language) {
            AppLanguage.JAPANESE -> "要望テキストをコピーしました"
            AppLanguage.KOREAN -> "기능 제안을 복사했습니다"
            AppLanguage.SIMPLIFIED_CHINESE -> "已复制功能建议"
            AppLanguage.TRADITIONAL_CHINESE -> "已複製功能建議"
            AppLanguage.SPANISH -> "Sugerencia copiada al portapapeles"
            AppLanguage.FRENCH -> "Suggestion copiée dans le presse-papiers"
            AppLanguage.PORTUGUESE -> "Sugestão de recurso copiada para a área de transferência"
            AppLanguage.RUSSIAN -> "Предложение скопировано в буфер обмена"
            AppLanguage.ENGLISH -> "Feature request copied to clipboard"
        }
    }
    val openBrowserFailedToast = when (language) {
        AppLanguage.JAPANESE -> "ブラウザを開けませんでした。「コピー」をご利用ください。"
        AppLanguage.KOREAN -> "브라우저를 열 수 없습니다. '복사'를 이용해 주세요."
        AppLanguage.SIMPLIFIED_CHINESE -> "无法打开浏览器，请使用复制功能。"
        AppLanguage.TRADITIONAL_CHINESE -> "無法開啟瀏覽器，請使用複製功能。"
        AppLanguage.SPANISH -> "No se pudo abrir el navegador. Usa Copiar en su lugar."
        AppLanguage.FRENCH -> "Impossible d'ouvrir le navigateur. Veuillez utiliser Copier."
        AppLanguage.PORTUGUESE -> "Não foi possível abrir o navegador. Use Copiar."
        AppLanguage.RUSSIAN -> "Не удалось открыть браузер. Воспользуйтесь кнопкой «Копировать»."
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
