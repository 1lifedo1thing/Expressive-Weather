package io.github.nemynew.expressiveweather.data

import android.os.Build
import io.github.nemynew.expressiveweather.BuildConfig
import io.github.nemynew.expressiveweather.theme.ThemeMode
import io.github.nemynew.expressiveweather.theme.ThemePreset
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * フィードバック送信モード（不具合報告 / 機能要望）
 */
enum class FeedbackMode {
    BUG_REPORT,
    FEATURE_REQUEST
}

/**
 * 機能要望（Feature Request）のカテゴリ定義
 */
enum class FeatureRequestCategory(
    val issueTag: String,
    val labelJa: String,
    val labelEn: String,
    val labelKo: String,
    val labelZhHans: String,
    val labelZhHant: String,
    val labelEs: String,
    val labelFr: String,
    val labelPt: String,
    val labelRu: String
) {
    UI_DESIGN(
        issueTag = "UI / Design",
        labelJa = "UI・デザイン",
        labelEn = "UI & Design",
        labelKo = "UI·디자인",
        labelZhHans = "界面与设计",
        labelZhHant = "介面與設計",
        labelEs = "UI y diseño",
        labelFr = "UI et design",
        labelPt = "UI e design",
        labelRu = "Интерфейс и дизайн"
    ),
    RADAR(
        issueTag = "Radar",
        labelJa = "雨雲レーダー",
        labelEn = "Rain Radar",
        labelKo = "강수 레이더",
        labelZhHans = "降水雷达",
        labelZhHant = "降雨雷達",
        labelEs = "Radar de lluvia",
        labelFr = "Radar de pluie",
        labelPt = "Radar de chuva",
        labelRu = "Радар осадков"
    ),
    WIDGET(
        issueTag = "Widget",
        labelJa = "ウィジェット",
        labelEn = "Widget",
        labelKo = "위젯",
        labelZhHans = "桌面微件",
        labelZhHant = "桌面小工具",
        labelEs = "Widget",
        labelFr = "Widget",
        labelPt = "Widget",
        labelRu = "Виджет"
    ),
    ALERTS_NOTIFICATIONS(
        issueTag = "Alerts / Notifications",
        labelJa = "警報・通知",
        labelEn = "Alerts & Notifications",
        labelKo = "특보·알림",
        labelZhHans = "预警与通知",
        labelZhHant = "警報與通知",
        labelEs = "Alertas y notificaciones",
        labelFr = "Alertes et notifications",
        labelPt = "Alertas e notificações",
        labelRu = "Оповещения и уведомления"
    ),
    WEATHER_DATA_REGION(
        issueTag = "Weather Data / Region",
        labelJa = "天気データ・新地域",
        labelEn = "Data & Regions",
        labelKo = "날씨 데이터·지역",
        labelZhHans = "气象数据与地区",
        labelZhHant = "氣象資料與地區",
        labelEs = "Datos y regiones",
        labelFr = "Données et régions",
        labelPt = "Dados e regiões",
        labelRu = "Данные и регионы"
    ),
    SETTINGS_USABILITY(
        issueTag = "Settings / UX",
        labelJa = "設定・操作性",
        labelEn = "Settings & UX",
        labelKo = "설정·사용성",
        labelZhHans = "设置与体验",
        labelZhHant = "設定與體驗",
        labelEs = "Ajustes y experiencia",
        labelFr = "Paramètres et UX",
        labelPt = "Configurações e UX",
        labelRu = "Настройки и удобство"
    ),
    OTHER(
        issueTag = "Feature",
        labelJa = "その他",
        labelEn = "Other",
        labelKo = "기타",
        labelZhHans = "其他",
        labelZhHant = "其他",
        labelEs = "Otro",
        labelFr = "Autre",
        labelPt = "Outro",
        labelRu = "Другое"
    );

    fun label(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> labelJa
        AppLanguage.KOREAN -> labelKo
        AppLanguage.SIMPLIFIED_CHINESE -> labelZhHans
        AppLanguage.TRADITIONAL_CHINESE -> labelZhHant
        AppLanguage.ENGLISH -> labelEn
        AppLanguage.SPANISH -> labelEs
        AppLanguage.FRENCH -> labelFr
        AppLanguage.PORTUGUESE -> labelPt
        AppLanguage.RUSSIAN -> labelRu
    }
}

/**
 * バグレポートのカテゴリ定義
 */
enum class BugReportCategory(
    val issueTag: String,
    val labelJa: String,
    val labelEn: String,
    val labelKo: String,
    val labelZhHans: String,
    val labelZhHant: String,
    val labelEs: String,
    val labelFr: String,
    val labelPt: String,
    val labelRu: String
) {
    WEATHER_DATA(
        issueTag = "Weather Data",
        labelJa = "天気データ・数値",
        labelEn = "Weather Data",
        labelKo = "날씨 데이터·수치",
        labelZhHans = "天气数据",
        labelZhHant = "天氣資料",
        labelEs = "Datos meteorológicos",
        labelFr = "Données météo",
        labelPt = "Dados meteorológicos",
        labelRu = "Данные о погоде"
    ),
    RADAR(
        issueTag = "Radar",
        labelJa = "雨雲レーダー",
        labelEn = "Rain Radar",
        labelKo = "강수 레이더",
        labelZhHans = "降水雷达",
        labelZhHant = "降雨雷達",
        labelEs = "Radar de lluvia",
        labelFr = "Radar de pluie",
        labelPt = "Radar de chuva",
        labelRu = "Радар осадков"
    ),
    ALERTS_NOTIFICATIONS(
        issueTag = "Alerts / Notifications",
        labelJa = "警報・通知",
        labelEn = "Alerts & Notifications",
        labelKo = "특보·알림",
        labelZhHans = "预警与通知",
        labelZhHant = "警報與通知",
        labelEs = "Alertas y notificaciones",
        labelFr = "Alertes et notifications",
        labelPt = "Alertas e notificações",
        labelRu = "Оповещения и уведомления"
    ),
    WIDGET(
        issueTag = "Widget",
        labelJa = "ウィジェット",
        labelEn = "Widget",
        labelKo = "위젯",
        labelZhHans = "桌面微件",
        labelZhHant = "桌面小工具",
        labelEs = "Widget",
        labelFr = "Widget",
        labelPt = "Widget",
        labelRu = "Виджет"
    ),
    UI_LAYOUT(
        issueTag = "UI / Layout",
        labelJa = "UI・表示崩れ",
        labelEn = "UI & Layout",
        labelKo = "UI·화면 깨짐",
        labelZhHans = "界面显示",
        labelZhHant = "介面顯示",
        labelEs = "UI y diseño",
        labelFr = "UI et mise en page",
        labelPt = "UI e layout",
        labelRu = "Интерфейс и вёрстка"
    ),
    CRASH_PERFORMANCE(
        issueTag = "Crash / Freeze",
        labelJa = "クラッシュ・重い",
        labelEn = "Crash / Slow",
        labelKo = "강제 종료·느림",
        labelZhHans = "崩溃 / 卡顿",
        labelZhHant = "當機 / 卡頓",
        labelEs = "Cierre / Lentitud",
        labelFr = "Plantage / Lenteur",
        labelPt = "Travamento / Lentidão",
        labelRu = "Сбои / тормоза"
    ),
    OTHER(
        issueTag = "Other",
        labelJa = "その他",
        labelEn = "Other",
        labelKo = "기타",
        labelZhHans = "其他",
        labelZhHant = "其他",
        labelEs = "Otro",
        labelFr = "Autre",
        labelPt = "Outro",
        labelRu = "Другое"
    );

    fun label(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> labelJa
        AppLanguage.KOREAN -> labelKo
        AppLanguage.SIMPLIFIED_CHINESE -> labelZhHans
        AppLanguage.TRADITIONAL_CHINESE -> labelZhHant
        AppLanguage.ENGLISH -> labelEn
        AppLanguage.SPANISH -> labelEs
        AppLanguage.FRENCH -> labelFr
        AppLanguage.PORTUGUESE -> labelPt
        AppLanguage.RUSSIAN -> labelRu
    }
}

/**
 * バグレポートに含める非個人特定・サニタイズ済み診断スナップショット
 */
data class BugReportSnapshot(
    val versionName: String,
    val versionCode: Int,
    val isPlayDistribution: Boolean,
    val deviceInfo: String,
    val language: AppLanguage,
    val themeMode: ThemeMode,
    val themePreset: ThemePreset,
    val temperatureUnit: TemperatureUnit,
    val windSpeedUnit: WindSpeedUnit,
    val radarSourceMode: RadarSourceMode,
    val isFrostedGlassEnabled: Boolean,
    val isFromCache: Boolean,
    val isStale: Boolean,
    val regionBucket: String,
    val recentLogs: List<String> = emptyList()
) {
    val flavorOption: String
        get() = if (isPlayDistribution) "Google Play (play)" else "GitHub Release (github)"

    val versionDisplay: String
        get() = "$versionName ($versionCode)"

    companion object {
        fun buildDeviceInfo(): String {
            val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
            val model = Build.MODEL?.trim().orEmpty()
            val deviceName = when {
                manufacturer.isBlank() -> model.ifBlank { "Android Device" }
                model.startsWith(manufacturer, ignoreCase = true) -> model
                else -> "$manufacturer $model"
            }
            val release = Build.VERSION.RELEASE?.trim().orEmpty().ifBlank { "?" }
            return "$deviceName, Android $release (API ${Build.VERSION.SDK_INT})"
        }

        fun resolveCoarseRegionBucket(weather: WeatherData?): String {
            if (weather == null) return "Unknown"
            val city = weather.city
            val isJapan = weather.jmaOverviewText != null ||
                isLocationOrCountryInJapan(city.country, city.latitude, city.longitude)
            if (isJapan) return "Japan (JMA)"
            if (NwsAlertRepository.isUnitedStates(city)) return "United States (NWS / Open-Meteo)"
            return "Global (Open-Meteo)"
        }

        fun fromCurrentState(
            weather: WeatherData?,
            language: AppLanguage,
            themeMode: ThemeMode,
            themePreset: ThemePreset,
            temperatureUnit: TemperatureUnit,
            windSpeedUnit: WindSpeedUnit,
            radarSourceMode: RadarSourceMode,
            isFrostedGlassEnabled: Boolean,
            isFromCache: Boolean,
            isStale: Boolean
        ): BugReportSnapshot {
            return BugReportSnapshot(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                isPlayDistribution = BuildConfig.IS_PLAY_DISTRIBUTION,
                deviceInfo = buildDeviceInfo(),
                language = language,
                themeMode = themeMode,
                themePreset = themePreset,
                temperatureUnit = temperatureUnit,
                windSpeedUnit = windSpeedUnit,
                radarSourceMode = radarSourceMode,
                isFrostedGlassEnabled = isFrostedGlassEnabled,
                isFromCache = isFromCache,
                isStale = isStale,
                regionBucket = resolveCoarseRegionBucket(weather),
                recentLogs = BugReportLogBuffer.snapshot()
            )
        }
    }
}

/**
 * 機密情報・座標などの個人情報（PII）を診断テキストから自動除去するサニタイザ
 */
internal object BugReportSanitizer {
    private val googleApiKeyRegex = Regex("AIza[0-9A-Za-z_\\-]{20,}")
    private val secretParamRegex = Regex("(?i)(bearer\\s+|api[_-]?key[=:]\\s*|token[=:]\\s*|key[=:]\\s*)(?!\\[REDACTED)([^\\s,&;\"']+)")
    private val latLonQueryRegex = Regex("(?i)(lat(?:itude)?|lon(?:gitude)?|lng)\\s*[=:]\\s*-?\\d{1,3}\\.\\d+")
    private val coordinatePairRegex = Regex("(?<!\\d)-?(?:90(?:\\.0+)?|[1-8]?\\d\\.\\d{2,})\\s*,\\s*-?(?:180(?:\\.0+)?|(?:1[0-7]\\d|[1-9]?\\d)\\.\\d{2,})(?!\\d)")

    fun sanitize(raw: String, maxLength: Int = 240): String {
        if (raw.isBlank()) return ""
        val collapsed = raw
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace(googleApiKeyRegex, "[REDACTED_KEY]")
            .replace(secretParamRegex) { match -> "${match.groupValues[1]}[REDACTED]" }
            .replace(latLonQueryRegex) { match -> "${match.groupValues[1]}=[REDACTED_COORD]" }
            .replace(coordinatePairRegex, "[REDACTED_COORDS]")
            .trim()
        return if (collapsed.length > maxLength) {
            collapsed.take(maxLength - 3) + "..."
        } else {
            collapsed
        }
    }
}

/**
 * アプリ内の軽量な直近エラーリングバッファ（最大20件・メモリ内のみ・自動送信なし）
 */
object BugReportLogBuffer {
    internal const val MAX_ENTRIES = 20
    private val lock = Any()
    private val entries = ArrayDeque<String>(MAX_ENTRIES)
    private val utcFormat = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun record(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = utcFormat.get()?.format(Date()) ?: "00:00:00Z"
        val safeTag = BugReportSanitizer.sanitize(tag, maxLength = 32).ifBlank { "App" }
        val safeMsg = BugReportSanitizer.sanitize(message, maxLength = 160)
        val errSuffix = throwable?.let {
            val cls = it::class.java.simpleName.ifBlank { "Exception" }
            val detail = BugReportSanitizer.sanitize(it.message.orEmpty(), maxLength = 100)
            if (detail.isNotBlank()) " ($cls: $detail)" else " ($cls)"
        }.orEmpty()
        val line = "[$timestamp] $safeTag: $safeMsg$errSuffix".trim()

        synchronized(lock) {
            while (entries.size >= MAX_ENTRIES) {
                entries.removeFirst()
            }
            entries.addLast(line)
        }
    }

    fun snapshot(): List<String> = synchronized(lock) { entries.toList() }

    fun clear() = synchronized(lock) { entries.clear() }
}

/**
 * バグレポート本文および GitHub Issues 自動入力 URL のビルダー
 */
object BugReportBuilder {
    const val GITHUB_ISSUE_BASE_URL = "https://github.com/nemy-new/Expressive-Weather/issues/new"
    private const val BUG_ISSUE_TEMPLATE_NAME = "bug_report.yml"
    private const val FEATURE_ISSUE_TEMPLATE_NAME = "feature_request.yml"
    private const val MAX_GITHUB_URL_LENGTH = 5500

    /**
     * バージョン 1.0.0 未満（および -alpha / -beta / -rc 付き）はすべて非正式（プレ）リリースと判定する。
     */
    fun isPreReleaseVersion(versionName: String): Boolean {
        val normalized = versionName.trim().removePrefix("v").removePrefix("V")
        val major = normalized.substringBefore('.').substringBefore('-').toIntOrNull()
        if (major != null && major < 1) return true
        return normalized.contains("-alpha", ignoreCase = true) ||
            normalized.contains("-beta", ignoreCase = true) ||
            normalized.contains("-rc", ignoreCase = true)
    }

    fun formatDiagnosticSummary(
        snapshot: BugReportSnapshot,
        includeDeviceInfo: Boolean,
        includeAppState: Boolean,
        includeRecentLogs: Boolean
    ): String = buildString {
        if (includeDeviceInfo) {
            appendLine("- Flavor: ${snapshot.flavorOption}")
            appendLine("- App Version: ${snapshot.versionDisplay}")
            appendLine("- Device: ${BugReportSanitizer.sanitize(snapshot.deviceInfo, maxLength = 120)}")
        }
        if (includeAppState) {
            appendLine("- Region / Provider: ${snapshot.regionBucket}")
            appendLine("- Language: ${snapshot.language.code}")
            appendLine("- Theme: mode=${snapshot.themeMode.id}, preset=${snapshot.themePreset.id}, frosted=${snapshot.isFrostedGlassEnabled}")
            appendLine("- Units: temp=${snapshot.temperatureUnit.symbol}, wind=${snapshot.windSpeedUnit.id}, radar=${snapshot.radarSourceMode.id}")
            appendLine("- Cache State: fromCache=${snapshot.isFromCache}, stale=${snapshot.isStale}")
        }
        if (includeRecentLogs && snapshot.recentLogs.isNotEmpty()) {
            appendLine()
            appendLine("Recent in-app diagnostics (sanitized):")
            snapshot.recentLogs.takeLast(10).forEach { logLine ->
                appendLine("  $logLine")
            }
        }
    }.trimEnd()

    fun formatPlainTextReport(
        category: BugReportCategory,
        description: String,
        expected: String,
        snapshot: BugReportSnapshot,
        includeDeviceInfo: Boolean,
        includeAppState: Boolean,
        includeRecentLogs: Boolean
    ): String = buildString {
        appendLine("[Expressive Weather Bug Report]")
        appendLine("Category: ${category.issueTag}")
        appendLine()
        appendLine("## What happened")
        appendLine(description.trim().ifBlank { "(No description entered)" })
        if (expected.isNotBlank()) {
            appendLine()
            appendLine("## Expected behavior")
            appendLine(expected.trim())
        }
        val diagnostics = formatDiagnosticSummary(
            snapshot = snapshot,
            includeDeviceInfo = includeDeviceInfo,
            includeAppState = includeAppState,
            includeRecentLogs = includeRecentLogs
        )
        if (diagnostics.isNotBlank()) {
            appendLine()
            appendLine("## Diagnostics")
            appendLine(diagnostics)
        }
    }.trimEnd()

    fun buildGitHubIssueUrl(
        category: BugReportCategory,
        description: String,
        expected: String,
        snapshot: BugReportSnapshot,
        includeDeviceInfo: Boolean,
        includeAppState: Boolean,
        includeRecentLogs: Boolean
    ): String {
        val summaryTitle = description.trim()
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.take(60)
            ?.let { "[${category.issueTag}] $it" }
            ?: "[${category.issueTag}] Bug report"

        val diagnosticsBlock = formatDiagnosticSummary(
            snapshot = snapshot,
            includeDeviceInfo = false, // flavor / app_version / device は専用欄に分離
            includeAppState = includeAppState,
            includeRecentLogs = includeRecentLogs
        )
        val combinedDescription = buildString {
            append(description.trim())
            if (diagnosticsBlock.isNotBlank()) {
                if (isNotEmpty()) appendLine().appendLine()
                appendLine("### App State / Diagnostics")
                append(diagnosticsBlock)
            }
        }

        val params = linkedMapOf(
            "template" to BUG_ISSUE_TEMPLATE_NAME,
            "title" to summaryTitle,
            "flavor" to snapshot.flavorOption,
            "app_version" to snapshot.versionDisplay
        )
        if (includeDeviceInfo) {
            params["device"] = BugReportSanitizer.sanitize(snapshot.deviceInfo, maxLength = 120)
        }
        if (includeAppState) {
            params["region"] = snapshot.regionBucket
        }
        if (combinedDescription.isNotBlank()) {
            params["description"] = combinedDescription
        }
        if (expected.isNotBlank()) {
            params["expected"] = expected.trim()
        }

        return encodeBoundedGitHubUrl(params, trimKey = "description")
    }

    fun formatFeatureAppInfo(snapshot: BugReportSnapshot): String = buildString {
        appendLine("- Flavor: ${snapshot.flavorOption}")
        appendLine("- App Version: ${snapshot.versionDisplay}")
        appendLine("- Language: ${snapshot.language.code}")
    }.trimEnd()

    fun formatPlainTextFeatureRequest(
        category: FeatureRequestCategory,
        proposal: String,
        problem: String,
        alternatives: String,
        snapshot: BugReportSnapshot,
        includeAppInfo: Boolean
    ): String = buildString {
        appendLine("[Expressive Weather Feature Request]")
        appendLine("Category: ${category.issueTag}")
        appendLine()
        appendLine("## Proposed Solution")
        appendLine(proposal.trim().ifBlank { "(No proposal entered)" })
        if (problem.isNotBlank()) {
            appendLine()
            appendLine("## Problem or Motivation")
            appendLine(problem.trim())
        }
        if (alternatives.isNotBlank()) {
            appendLine()
            appendLine("## Additional Context / Data Sources")
            appendLine(alternatives.trim())
        }
        if (includeAppInfo) {
            appendLine()
            appendLine("## App Info")
            appendLine(formatFeatureAppInfo(snapshot))
        }
    }.trimEnd()

    fun buildGitHubFeatureRequestUrl(
        category: FeatureRequestCategory,
        proposal: String,
        problem: String,
        alternatives: String,
        snapshot: BugReportSnapshot,
        includeAppInfo: Boolean
    ): String {
        val summaryLine = proposal.trim()
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?: problem.trim().lineSequence().firstOrNull { it.isNotBlank() }
        val summaryTitle = summaryLine
            ?.take(60)
            ?.let { "[${category.issueTag}] $it" }
            ?: "[${category.issueTag}] Feature request"

        val combinedAlternatives = buildString {
            val cleanAlt = alternatives.trim()
            if (cleanAlt.isNotBlank()) append(cleanAlt)
            if (includeAppInfo) {
                if (isNotEmpty()) appendLine().appendLine()
                appendLine("### App Info")
                append(formatFeatureAppInfo(snapshot))
            }
        }

        val params = linkedMapOf(
            "template" to FEATURE_ISSUE_TEMPLATE_NAME,
            "title" to summaryTitle
        )
        if (problem.isNotBlank()) {
            params["problem"] = problem.trim()
        }
        if (proposal.isNotBlank()) {
            params["proposal"] = proposal.trim()
        }
        if (combinedAlternatives.isNotBlank()) {
            params["alternatives"] = combinedAlternatives
        }

        return encodeBoundedGitHubUrl(params, trimKey = "proposal")
    }

    private fun encodeBoundedGitHubUrl(
        params: LinkedHashMap<String, String>,
        trimKey: String
    ): String {
        fun encode(map: Map<String, String>): String {
            val query = map.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            return "$GITHUB_ISSUE_BASE_URL?$query"
        }

        var url = encode(params)
        if (url.length > MAX_GITHUB_URL_LENGTH && params.containsKey(trimKey)) {
            val overflow = url.length - MAX_GITHUB_URL_LENGTH + 48
            val currentText = params[trimKey].orEmpty()
            val trimmedLen = (currentText.length - overflow).coerceAtLeast(120)
            params[trimKey] = currentText.take(trimmedLen) + "\n...(truncated)"
            url = encode(params)
        }
        return url
    }
}
