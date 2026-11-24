package io.github.nemynew.expressiveweather.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.transformed
import io.github.nemynew.expressiveweather.MainActivity
import io.github.nemynew.expressiveweather.ui.screens.RadarDetailActivity
import io.github.nemynew.expressiveweather.util.LocaleUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
internal data class KikikuruAlertDismissal(
    val municipalityCode: String,
    val dismissedLevel: Int,
    val dismissedSignature: String,
    val dismissedAtMillis: Long,
    val suppressUntilMillis: Long
)

/**
 * 警戒レベル・キキクル Live Updates 通知マネージャー
 *
 * Android 16+ Native Live Updates (Promoted Ongoing + ProgressStyle) および Galaxy Now Bar に完全対応した
 * 進行中アクティビティ（Live Notification）を構築・管理します。
 */
object KikikuruNotificationManager {
    private const val TAG = "KikikuruNotifManager"
    private const val PREFS_NAME = "expressive_weather_prefs"
    private const val KEY_ACTIVE_ALERT_JSON = "kikikuru_active_alert_json"
    private const val KEY_ACTIVE_CITY_NAME = "kikikuru_active_city_name"
    private const val KEY_LAST_ALERTED_LEVEL = "kikikuru_last_alerted_level"
    private const val KEY_LAST_POSTED_SIGNATURE = "kikikuru_last_posted_signature"
    private const val KEY_DISMISSED_ALERT_JSON = "kikikuru_dismissed_alert_json"
    private const val ALERT_DISMISS_SUPPRESS_MS = 3 * 60 * 60 * 1000L
    private const val NOTIFICATION_POST_GRACE_MS = 5_000L
    const val CHANNEL_ID = "kikikuru_live_updates"
    const val NOTIFICATION_ID = 9110

    // 現在アクティブな警戒情報
    @Volatile
    var currentActiveAlertInfo: MunicipalityAlertInfo? = null
        private set

    @Volatile
    var currentActiveCityName: String = ""
        private set

    @Volatile
    var isTestModeActive: Boolean = false
        private set

    // 直近で音・バイブ通知を発火した警戒レベル（SharedPreferences にも永続化しプロセス再起動時の再鳴動を防止）
    @Volatile
    private var lastAlertedLevel: Int = 0

    @Volatile
    private var lastAlertedLevelLoaded: Boolean = false

    @Volatile
    private var lastPostedSignature: String = ""

    @Volatile
    private var lastUserDismissal: KikikuruAlertDismissal? = null

    @Volatile
    private var dismissalLoadedFromPrefs: Boolean = false

    @Volatile
    internal var lastNotificationPostedAtMillis: Long = 0L

    internal fun buildAlertSignature(alertInfo: MunicipalityAlertInfo): String {
        val warningsPart = alertInfo.activeWarnings
            .map { "${it.code}:${it.name}" }
            .sorted()
            .joinToString(",")
        val flagsPart = alertInfo.disasterFlags.activeTypes
            .map { it.name }
            .sorted()
            .joinToString(",")
        val muniKey = alertInfo.municipalityCode.ifBlank { alertInfo.municipalityName.trim() }
        return "$muniKey|lv=${alertInfo.alertLevel.level}|primary=${alertInfo.targetDisasterPrimary?.name.orEmpty()}|w=$warningsPart|f=$flagsPart"
    }

    internal fun shouldSuppressAlert(
        dismissal: KikikuruAlertDismissal?,
        alertInfo: MunicipalityAlertInfo,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (dismissal == null) return false
        if (nowMillis >= dismissal.suppressUntilMillis) return false
        val incomingMuni = alertInfo.municipalityCode.ifBlank { alertInfo.municipalityName.trim() }
        if (dismissal.municipalityCode.isNotBlank() &&
            incomingMuni.isNotBlank() &&
            dismissal.municipalityCode != incomingMuni
        ) {
            return false
        }
        // 警戒レベルが消去時より引き上がった場合（例: レベル2 → レベル3/4）は安全のため必ず再通知する
        if (alertInfo.alertLevel.level > dismissal.dismissedLevel) {
            return false
        }
        return true
    }

    private fun loadLastAlertedLevelIfNeeded(context: Context): Int {
        if (!lastAlertedLevelLoaded) {
            lastAlertedLevelLoaded = true
            try {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                lastAlertedLevel = prefs.getInt(KEY_LAST_ALERTED_LEVEL, 0)
                lastPostedSignature = prefs.getString(KEY_LAST_POSTED_SIGNATURE, "") ?: ""
            } catch (_: Exception) {}
        }
        return lastAlertedLevel
    }

    private fun persistAlertTracking(context: Context, level: Int, signature: String) {
        lastAlertedLevel = level
        lastAlertedLevelLoaded = true
        lastPostedSignature = signature
        try {
            val editor = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            if (level <= 0 && signature.isBlank()) {
                editor.remove(KEY_LAST_ALERTED_LEVEL).remove(KEY_LAST_POSTED_SIGNATURE)
            } else {
                editor.putInt(KEY_LAST_ALERTED_LEVEL, level)
                    .putString(KEY_LAST_POSTED_SIGNATURE, signature)
            }
            editor.apply()
        } catch (_: Exception) {}
    }

    private fun persistDismissalState(context: Context, dismissal: KikikuruAlertDismissal?) {
        lastUserDismissal = dismissal
        dismissalLoadedFromPrefs = true
        try {
            val editor = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            if (dismissal == null) {
                editor.remove(KEY_DISMISSED_ALERT_JSON)
            } else {
                editor.putString(KEY_DISMISSED_ALERT_JSON, WeatherJsonSerializer.json.encodeToString(dismissal))
            }
            editor.apply()
        } catch (_: Exception) {}
    }

    private fun getDismissalState(context: Context, nowMillis: Long = System.currentTimeMillis()): KikikuruAlertDismissal? {
        if (!dismissalLoadedFromPrefs) {
            dismissalLoadedFromPrefs = true
            try {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val raw = prefs.getString(KEY_DISMISSED_ALERT_JSON, null)
                if (!raw.isNullOrBlank()) {
                    lastUserDismissal = WeatherJsonSerializer.json.decodeFromString<KikikuruAlertDismissal>(raw)
                }
            } catch (_: Exception) {
                lastUserDismissal = null
            }
        }
        val current = lastUserDismissal ?: return null
        if (nowMillis >= current.suppressUntilMillis) {
            persistDismissalState(context, null)
            return null
        }
        return current
    }

    fun isSuppressedByUserDismissal(
        context: Context,
        alertInfo: MunicipalityAlertInfo,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val dismissal = getDismissalState(context, nowMillis)
        return shouldSuppressAlert(dismissal, alertInfo, nowMillis)
    }

    fun clearDismissalSuppression(context: Context) {
        persistDismissalState(context.applicationContext, null)
    }

    internal fun markNotificationPosted(nowMillis: Long = System.currentTimeMillis()) {
        lastNotificationPostedAtMillis = nowMillis
    }

    internal fun isNotificationCurrentlyShowing(context: Context): Boolean {
        return try {
            val nm = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return true
            nm.activeNotifications.any { it.id == NOTIFICATION_ID }
        } catch (_: Throwable) {
            true
        }
    }

    internal fun hasUserDismissedActiveNotification(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val postedAt = lastNotificationPostedAtMillis
        if (postedAt <= 0L || nowMillis - postedAt < NOTIFICATION_POST_GRACE_MS) {
            return false
        }
        return !isNotificationCurrentlyShowing(context)
    }

    fun dismissByUser(
        context: Context,
        municipalityCode: String = "",
        dismissedLevel: Int = 0,
        signature: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val appContext = context.applicationContext
        val wasTest = isTestModeActive
        val active = currentActiveAlertInfo
        val effectiveMuni = municipalityCode.ifBlank {
            active?.municipalityCode?.ifBlank { active.municipalityName.trim() } ?: currentActiveCityName.trim()
        }
        val effectiveLevel = if (dismissedLevel > 0) dismissedLevel else (active?.alertLevel?.level ?: 2)
        val effectiveSig = signature.ifBlank { active?.let { buildAlertSignature(it) } ?: lastPostedSignature }

        if (!wasTest && effectiveLevel >= 2) {
            val record = KikikuruAlertDismissal(
                municipalityCode = effectiveMuni,
                dismissedLevel = effectiveLevel,
                dismissedSignature = effectiveSig,
                dismissedAtMillis = nowMillis,
                suppressUntilMillis = nowMillis + ALERT_DISMISS_SUPPRESS_MS
            )
            persistDismissalState(appContext, record)
            Log.d(TAG, "Recorded Kikikuru alert dismissal suppression (muni='$effectiveMuni', lv=$effectiveLevel)")
        }
        cancelAll(appContext)
    }

    private fun createDismissPendingIntent(
        context: Context,
        municipalityCode: String,
        level: Int,
        signature: String
    ): PendingIntent {
        val appContext = context.applicationContext
        val dismissIntent = Intent(appContext, KikikuruAlertDismissReceiver::class.java).apply {
            action = KikikuruAlertDismissReceiver.ACTION_DISMISS_KIKIKURU_ALERT
            putExtra(KikikuruAlertDismissReceiver.EXTRA_MUNICIPALITY_CODE, municipalityCode)
            putExtra(KikikuruAlertDismissReceiver.EXTRA_ALERT_LEVEL, level)
            putExtra(KikikuruAlertDismissReceiver.EXTRA_ALERT_SIGNATURE, signature)
        }
        return PendingIntent.getBroadcast(
            appContext,
            NOTIFICATION_ID + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun persistActiveAlert(context: Context, alertInfo: MunicipalityAlertInfo?, cityName: String) {
        try {
            val editor = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            if (alertInfo == null) {
                editor.remove(KEY_ACTIVE_ALERT_JSON).remove(KEY_ACTIVE_CITY_NAME)
            } else {
                editor.putString(KEY_ACTIVE_ALERT_JSON, WeatherJsonSerializer.json.encodeToString(alertInfo))
                    .putString(KEY_ACTIVE_CITY_NAME, cityName)
            }
            editor.apply()
        } catch (_: Exception) {}
    }

    fun restoreIfNeeded(context: Context) {
        if (currentActiveAlertInfo != null) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_ACTIVE_ALERT_JSON, null) ?: return
            val restored = WeatherJsonSerializer.json.decodeFromString<MunicipalityAlertInfo>(json)
            if (restored.alertLevel.level < 2 || isSuppressedByUserDismissal(context, restored)) {
                persistActiveAlert(context, null, "")
                return
            }
            loadLastAlertedLevelIfNeeded(context)
            currentActiveAlertInfo = restored
            currentActiveCityName = prefs.getString(KEY_ACTIVE_CITY_NAME, "") ?: restored.municipalityName
        } catch (_: Exception) {}
    }

    /**
     * 通知チャンネルの作成（Android 8.0+ 必須）
     * ステータスバーピル表示およびロック画面最上部掲示のため、重要度 HIGH を設定
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val language = LocaleUtils.getSavedLanguage(context.applicationContext)
            val name = when (language) {
                AppLanguage.ENGLISH -> "Disaster Alert Live Updates"
                AppLanguage.KOREAN -> "재난 경보 실시간 알림"
                AppLanguage.SIMPLIFIED_CHINESE -> "灾害预警实时动态通知"
                AppLanguage.TRADITIONAL_CHINESE -> "災害警報即時動態通知"
                AppLanguage.SPANISH -> "Alertas de desastre en tiempo real"
                AppLanguage.FRENCH -> "Alertes catastrophe en temps réel"
                AppLanguage.PORTUGUESE -> "Alertas de desastre em tempo real"
                AppLanguage.JAPANESE -> "警戒レベル・キキクル Live Updates"
            }
            val descriptionText = when (language) {
                AppLanguage.ENGLISH -> "Real-time ongoing alerts when Level 2+ weather or disaster warnings are active"
                AppLanguage.KOREAN -> "해당 지역에 경계 레벨 2 이상의 기상·재난 특보가 발표될 때 표시되는 실시간 알림"
                AppLanguage.SIMPLIFIED_CHINESE -> "当所在地区发布2级以上气象或灾害预警时的实时常驻通知"
                AppLanguage.TRADITIONAL_CHINESE -> "當所在地區發布2級以上氣象或災害警報時的即時常駐通知"
                AppLanguage.SPANISH -> "Notificaciones continuas en tiempo real cuando hay alertas meteorológicas o de desastre de nivel 2 o superior"
                AppLanguage.FRENCH -> "Notifications continues en temps réel lorsque des alertes météo ou catastrophe de niveau 2 ou plus sont actives"
                AppLanguage.PORTUGUESE -> "Notificações contínuas em tempo real quando houver alertas meteorológicos ou de desastre de nível 2 ou superior"
                AppLanguage.JAPANESE -> "市区町村で警戒レベル2以上の警報・注意報が発表された際のリアルタイム警戒通知（ライブ通知）"
            }
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                // 警戒レベル通知向けの重厚で明確な緊急リズム
                vibrationPattern = longArrayOf(0L, 65L, 70L, 120L, 90L, 180L)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Android 16 Rich Ongoing Notifications & Galaxy Now Bar 準拠の
     * 進行中ライブ通知 (Notification) をビルド
     */
    fun buildLiveNotification(
        context: Context,
        alertInfo: MunicipalityAlertInfo,
        cityName: String = alertInfo.municipalityName,
        isSilentUpdate: Boolean = false
    ): Notification {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)
        val language = LocaleUtils.getSavedLanguage(appContext)

        val level = alertInfo.alertLevel
        val levelNum = level.level

        val rawPrimaryDetail = alertInfo.targetDisasterPrimary?.let { alertInfo.getDetailFor(it) }
            ?: alertInfo.allDisasterDetails.maxByOrNull { it.alertLevel.level }
            ?: alertInfo.getDetailFor(DisasterType.HEAVY_RAIN)
        // primaryDetail のレベルが総合警戒レベルを下回る場合（非大雨特別警報等）は総合レベルを引き継ぐ
        val primaryDetail = if (rawPrimaryDetail.alertLevel.level < levelNum) {
            rawPrimaryDetail.copy(alertLevel = level)
        } else {
            rawPrimaryDetail
        }

        val primaryDisaster = when (language) {
            AppLanguage.ENGLISH -> primaryDetail.disasterType.shortLabelEn
            AppLanguage.KOREAN -> primaryDetail.disasterType.shortLabelKo
            AppLanguage.SIMPLIFIED_CHINESE -> primaryDetail.disasterType.shortLabelZhHans
            AppLanguage.TRADITIONAL_CHINESE -> primaryDetail.disasterType.shortLabelZhHant
            AppLanguage.SPANISH -> primaryDetail.disasterType.shortLabelEs
            AppLanguage.FRENCH -> primaryDetail.disasterType.shortLabelFr
            AppLanguage.PORTUGUESE -> primaryDetail.disasterType.shortLabelPt
            AppLanguage.JAPANESE -> primaryDetail.disasterType.shortLabelJa
        }
        val chipText = when (language) {
            AppLanguage.ENGLISH -> "$primaryDisaster Lv.$levelNum"
            AppLanguage.KOREAN -> "$primaryDisaster 레벨 $levelNum"
            AppLanguage.SIMPLIFIED_CHINESE -> "$primaryDisaster ${levelNum}级"
            AppLanguage.TRADITIONAL_CHINESE -> "$primaryDisaster ${levelNum}級"
            AppLanguage.SPANISH -> "$primaryDisaster Nv.$levelNum"
            AppLanguage.FRENCH -> "$primaryDisaster Niv.$levelNum"
            AppLanguage.PORTUGUESE -> "$primaryDisaster Nv.$levelNum"
            AppLanguage.JAPANESE -> "$primaryDisaster レベル$levelNum"
        }

        val targetCity = alertInfo.municipalityName.ifBlank { cityName }
        val title = "$chipText • $targetCity"
        val tickerText = "$chipText $targetCity"
        val evacBadge = primaryDetail.getEvacuationInfo(language)?.let { "【$it】" } ?: ""
        val levelTitle = level.getTitle(language)
        val summaryText = when (language) {
            AppLanguage.ENGLISH -> "Alert Level $levelNum [$levelTitle] $evacBadge"
            AppLanguage.KOREAN -> "경계 레벨 $levelNum 【$levelTitle】 $evacBadge"
            AppLanguage.SIMPLIFIED_CHINESE -> "警戒级别 $levelNum 【$levelTitle】 $evacBadge"
            AppLanguage.TRADITIONAL_CHINESE -> "警戒級別 $levelNum 【$levelTitle】 $evacBadge"
            AppLanguage.SPANISH -> "Nivel de alerta $levelNum [$levelTitle] $evacBadge"
            AppLanguage.FRENCH -> "Niveau d'alerte $levelNum [$levelTitle] $evacBadge"
            AppLanguage.PORTUGUESE -> "Nível de alerta $levelNum [$levelTitle] $evacBadge"
            AppLanguage.JAPANESE -> "警戒レベル$levelNum 【$levelTitle】 $evacBadge"
        }.trim()

        val disasterBadgeText = if (alertInfo.disasterFlags.activeTypes.isNotEmpty()) {
            val labels = alertInfo.disasterFlags.activeTypes.joinToString("・") {
                when (language) {
                    AppLanguage.ENGLISH -> it.labelEn
                    AppLanguage.KOREAN -> it.labelKo
                    AppLanguage.SIMPLIFIED_CHINESE -> it.labelZhHans
                    AppLanguage.TRADITIONAL_CHINESE -> it.labelZhHant
                    AppLanguage.SPANISH -> it.labelEs
                    AppLanguage.FRENCH -> it.labelFr
                    AppLanguage.PORTUGUESE -> it.labelPt
                    AppLanguage.JAPANESE -> it.labelJa
                }
            }
            when (language) {
                AppLanguage.ENGLISH -> "[Hazards: $labels]"
                AppLanguage.KOREAN -> "【경계 대상: $labels】"
                AppLanguage.SIMPLIFIED_CHINESE -> "【警戒对象: $labels】"
                AppLanguage.TRADITIONAL_CHINESE -> "【警戒對象: $labels】"
                AppLanguage.SPANISH -> "[Riesgos: $labels]"
                AppLanguage.FRENCH -> "[Risques : $labels]"
                AppLanguage.PORTUGUESE -> "[Riscos: $labels]"
                AppLanguage.JAPANESE -> "【警戒対象: $labels】"
            }
        } else ""

        val warningsText = if (alertInfo.activeWarnings.isNotEmpty()) {
            val separator = when (language) {
                AppLanguage.JAPANESE, AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.TRADITIONAL_CHINESE -> "、"
                AppLanguage.ENGLISH, AppLanguage.KOREAN, AppLanguage.SPANISH, AppLanguage.FRENCH, AppLanguage.PORTUGUESE -> ", "
            }
            alertInfo.activeWarnings.joinToString(separator) {
                JmaWarningMaster.getLocalizedWarningName(it.name, language)
            } + when (language) {
                AppLanguage.ENGLISH -> " active"
                AppLanguage.KOREAN -> " 발효 중"
                AppLanguage.SIMPLIFIED_CHINESE -> " 生效中"
                AppLanguage.TRADITIONAL_CHINESE -> " 生效中"
                AppLanguage.SPANISH -> " en vigor"
                AppLanguage.FRENCH -> " en vigueur"
                AppLanguage.PORTUGUESE -> " em vigor"
                AppLanguage.JAPANESE -> " 発表中"
            }
        } else {
            when (language) {
                AppLanguage.ENGLISH -> "Weather advisory active"
                AppLanguage.KOREAN -> "기상 주의보 발효 중"
                AppLanguage.SIMPLIFIED_CHINESE -> "注意报生效中"
                AppLanguage.TRADITIONAL_CHINESE -> "注意報生效中"
                AppLanguage.SPANISH -> "Aviso meteorológico en vigor"
                AppLanguage.FRENCH -> "Vigilance météo en vigueur"
                AppLanguage.PORTUGUESE -> "Aviso meteorológico em vigor"
                AppLanguage.JAPANESE -> "注意報発表中"
            }
        }

        val actionText = primaryDetail.getActionAdvice(language)
        val targetLayerId = primaryDetail.disasterType.radarLayerId

        val lastResolvedCity = WeatherRepository.getLastResolvedLocationCity(appContext)
        val clickIntent = Intent(appContext, RadarDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("initial_layer_id", targetLayerId)
            putExtra("city_name", targetCity)
            if (lastResolvedCity != null) {
                putExtra("latitude", lastResolvedCity.latitude)
                putExtra("longitude", lastResolvedCity.longitude)
                putExtra("country", lastResolvedCity.country)
                runCatching {
                    putExtra(WeatherRepository.EXTRA_CITY_JSON, WeatherJsonSerializer.json.encodeToString(lastResolvedCity))
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainAppPendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID + 1,
            mainAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = buildString {
            appendLine("$summaryText $targetCity".trim())
            if (disasterBadgeText.isNotBlank()) {
                appendLine(disasterBadgeText)
            }
            appendLine()
            appendLine(actionText)
            appendLine()
            append(warningsText)
        }

        val notificationColor = level.colorHex.toInt()
        val badgeBitmap = createLevelBadgeBitmap(appContext, level)
        val alertSignature = buildAlertSignature(alertInfo)
        val muniKey = alertInfo.municipalityCode.ifBlank { targetCity }
        val dismissPendingIntent = createDismissPendingIntent(
            context = appContext,
            municipalityCode = muniKey,
            level = levelNum,
            signature = alertSignature
        )

        val prevAlertedLevel = loadLastAlertedLevelIfNeeded(appContext)
        val isEscalated = !isSilentUpdate && levelNum > prevAlertedLevel
        if (isEscalated) {
            persistAlertTracking(appContext, levelNum, alertSignature)
        } else if (lastPostedSignature != alertSignature) {
            persistAlertTracking(appContext, maxOf(prevAlertedLevel, levelNum), alertSignature)
        }

        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        val useProgressStyle = Build.VERSION.SDK_INT >= 36 && !isSamsung

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setLargeIcon(badgeBitmap)
            .setContentTitle(title)
            .setContentText(actionText)
            .setSubText(chipText)
            .setTicker(tickerText)
            .setUsesChronometer(false)
            .setShowWhen(true)
            .setColor(notificationColor)
            .setColorized(false)
            .setGroupSummary(false)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(chipText)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(!isEscalated)
            .setSilent(isSilentUpdate && !isEscalated)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setAutoCancel(false)
            .addAction(
                android.R.drawable.ic_dialog_map,
                primaryDetail.getButtonText(language),
                pendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_info_details,
                when (language) {
                    AppLanguage.ENGLISH -> "Open App"
                    AppLanguage.KOREAN -> "앱 열기"
                    AppLanguage.SIMPLIFIED_CHINESE -> "打开应用"
                    AppLanguage.TRADITIONAL_CHINESE -> "開啟應用程式"
                    AppLanguage.SPANISH -> "Abrir app"
                    AppLanguage.FRENCH -> "Ouvrir l'app"
                    AppLanguage.PORTUGUESE -> "Abrir app"
                    AppLanguage.JAPANESE -> "アプリを開く"
                },
                mainAppPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                when (language) {
                    AppLanguage.ENGLISH -> "Dismiss"
                    AppLanguage.KOREAN -> "닫기"
                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                    AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                    AppLanguage.SPANISH -> "Descartar"
                    AppLanguage.FRENCH -> "Fermer"
                    AppLanguage.PORTUGUESE -> "Dispensar"
                    AppLanguage.JAPANESE -> "閉じる"
                },
                dismissPendingIntent
            )

        if (useProgressStyle) {
            val levelColors = intArrayOf(
                AlertLevel.LEVEL_1.colorHex.toInt(),
                AlertLevel.LEVEL_2.colorHex.toInt(),
                AlertLevel.LEVEL_3.colorHex.toInt(),
                AlertLevel.LEVEL_4.colorHex.toInt(),
                AlertLevel.LEVEL_5.colorHex.toInt()
            )
            val progressStyle = NotificationCompat.ProgressStyle()
                .setProgress(levelNum)
            for (i in 0 until 5) {
                progressStyle.addProgressSegment(
                    NotificationCompat.ProgressStyle.Segment(1).setColor(levelColors[i])
                )
            }
            for (i in 1..4) {
                progressStyle.addProgressPoint(
                    NotificationCompat.ProgressStyle.Point(i).setColor(levelColors[(i - 1).coerceIn(0, 4)])
                )
            }
            builder.setStyle(progressStyle)
        } else {
            builder.setProgress(5, levelNum, false)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .setSummaryText(summaryText)
                        .bigText(bigText)
                )
        }

        // Galaxy One UI 7 Now Bar 互換用エクストラ設定
        builder.extras.putBoolean("com.samsung.android.app.notification.nowbar", true)
        builder.extras.putString("com.samsung.android.app.notification.nowbar.title", chipText)
        builder.extras.putString("com.samsung.android.app.notification.nowbar.text", targetCity)
        builder.extras.putString("com.samsung.android.app.notification.subtext", chipText)
        builder.extras.putString("com.samsung.android.app.notification.title", chipText)

        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= 36 && io.github.nemynew.expressiveweather.BuildConfig.DEBUG) {
            Log.d(TAG, "Built API 36 alert notification: promotable=${notification.hasPromotableCharacteristics()}")
        }
        return notification
    }

    /**
     * フォアグラウンドサービス起動直後に OS へ提示する安全な初期通知
     * ForegroundServiceDidNotStartInTimeException を 100% 抑止するために使用
     */
    fun buildSafeForegroundNotification(context: Context): Notification {
        createNotificationChannel(context)
        val alertInfo = currentActiveAlertInfo
        if (alertInfo != null && alertInfo.alertLevel.level >= 2) {
            return buildLiveNotification(context, alertInfo, currentActiveCityName)
        }
        val language = LocaleUtils.getSavedLanguage(context.applicationContext)
        val fgTitle = when (language) {
            AppLanguage.ENGLISH -> "Monitoring weather alerts"
            AppLanguage.KOREAN -> "기상 경계 정보 감시 중"
            AppLanguage.SIMPLIFIED_CHINESE -> "正在监测气象预警信息"
            AppLanguage.TRADITIONAL_CHINESE -> "正在監測氣象警報資訊"
            AppLanguage.SPANISH -> "Supervisando alertas meteorológicas"
            AppLanguage.FRENCH -> "Surveillance des alertes météo"
            AppLanguage.PORTUGUESE -> "Monitorando alertas meteorológicos"
            AppLanguage.JAPANESE -> "気象警戒情報を監視中"
        }
        val fgText = when (language) {
            AppLanguage.ENGLISH -> "Waiting for disaster risk updates"
            AppLanguage.KOREAN -> "재난 위험도 정보를 대기하고 있습니다"
            AppLanguage.SIMPLIFIED_CHINESE -> "正在等待灾害风险信息更新"
            AppLanguage.TRADITIONAL_CHINESE -> "正在等待災害風險資訊更新"
            AppLanguage.SPANISH -> "Esperando actualizaciones de riesgo de desastre"
            AppLanguage.FRENCH -> "En attente des mises à jour des risques"
            AppLanguage.PORTUGUESE -> "Aguardando atualizações de risco de desastre"
            AppLanguage.JAPANESE -> "気象庁キキクル情報を待機しています"
        }
        val dismissPendingIntent = createDismissPendingIntent(
            context = context,
            municipalityCode = currentActiveAlertInfo?.municipalityCode.orEmpty(),
            level = currentActiveAlertInfo?.alertLevel?.level ?: 2,
            signature = lastPostedSignature
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(fgTitle)
            .setContentText(fgText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setDeleteIntent(dismissPendingIntent)
            .setAutoCancel(false)
        return builder.build()
    }

    /**
     * 判定結果に応じた Live Updates 通知の更新または破棄
     * - レベル2以上: KikikuruLiveAlertService（フォアグラウンドサービス）を起動し通知と常駐プロセスを結合
     * - レベル1以下: サービス停止および通知破棄
     */
    fun updateLiveAlertNotification(
        context: Context,
        alertInfo: MunicipalityAlertInfo,
        cityName: String = alertInfo.municipalityName
    ) {
        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 通知マスター設定チェック
        if (!WeatherRepository.isLiveAlertNotificationEnabled(appContext)) {
            currentActiveAlertInfo = null
            currentActiveCityName = ""
            isTestModeActive = false
            persistActiveAlert(appContext, null, "")
            KikikuruLiveAlertService.stop(appContext)
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d(TAG, "Live alert notification is disabled by user.")
            return
        }

        val threshold = WeatherRepository.getAlertNotificationThreshold(appContext)
        if (threshold == AlertNotificationThreshold.OFF) {
            currentActiveAlertInfo = null
            currentActiveCityName = ""
            isTestModeActive = false
            persistActiveAlert(appContext, null, "")
            KikikuruLiveAlertService.stop(appContext)
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d(TAG, "Alert notification threshold is OFF.")
            return
        }

        // テスト通知稼働中にバックグラウンドの平常データ(Lv < 2)が届いてもテスト表示を即時消去しないよう保護
        if (isTestModeActive && alertInfo.alertLevel.level < 2) {
            Log.d(TAG, "Active test notification preserved; ignoring background Lv.${alertInfo.alertLevel.level} update.")
            return
        }

        // 終了条件
        if (alertInfo.alertLevel.level < 2 || (!isTestModeActive && alertInfo.alertLevel.level < threshold.minLevel)) {
            currentActiveAlertInfo = null
            currentActiveCityName = ""
            isTestModeActive = false
            lastNotificationPostedAtMillis = 0L
            persistAlertTracking(appContext, 0, "")
            persistActiveAlert(appContext, null, "")
            KikikuruLiveAlertService.stop(appContext)
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d(TAG, "Alert level (${alertInfo.alertLevel.level}) below threshold (${threshold.minLevel}). Stopped live service.")
            return
        }

        // POST_NOTIFICATIONS 権限チェック (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skip notification.")
                return
            }
        }

        val incomingSignature = buildAlertSignature(alertInfo)
        loadLastAlertedLevelIfNeeded(appContext)

        // ユーザーがスワイプ消去した同一レベル以下の警報は再通知しない（レベル昇格時のみ再通知）
        if (!isTestModeActive && isSuppressedByUserDismissal(appContext, alertInfo)) {
            Log.d(TAG, "Skipping Kikikuru alert notification: suppressed by user dismissal (${alertInfo.municipalityName} Lv.${alertInfo.alertLevel.level})")
            if (KikikuruLiveAlertService.isActivePolling) {
                KikikuruLiveAlertService.stop(appContext)
            }
            return
        }

        // 既に表示中の通知をユーザーがスワイプ消去していた場合は再送せず抑止記録して終了
        if (!isTestModeActive &&
            KikikuruLiveAlertService.isActivePolling &&
            hasUserDismissedActiveNotification(appContext)
        ) {
            Log.d(TAG, "Detected swipe-dismissed Kikikuru notification during update; suppressing re-post.")
            dismissByUser(
                context = appContext,
                municipalityCode = alertInfo.municipalityCode.ifBlank { alertInfo.municipalityName },
                dismissedLevel = alertInfo.alertLevel.level,
                signature = incomingSignature
            )
            return
        }

        // 全く同一内容の警報が既に通知欄へ掲示済みなら重複再投稿をスキップ（1分毎・3分毎のスパムを完全抑止）
        val isIdenticalAlreadyShowing = !isTestModeActive &&
            incomingSignature == lastPostedSignature &&
            isNotificationCurrentlyShowing(appContext) &&
            currentActiveAlertInfo != null
        if (isIdenticalAlreadyShowing) {
            currentActiveAlertInfo = alertInfo
            currentActiveCityName = cityName
            persistActiveAlert(appContext, alertInfo, cityName)
            Log.d(TAG, "Identical Kikikuru alert already active in shade; skipping redundant notify ($incomingSignature).")
            return
        }

        val isAlreadyShowing = isNotificationCurrentlyShowing(appContext)
        currentActiveAlertInfo = alertInfo
        currentActiveCityName = cityName
        persistActiveAlert(appContext, alertInfo, cityName)

        // フォアグラウンドサービスを起動して startForeground と結合 (失敗時は内部で notify フォールバック)
        KikikuruLiveAlertService.startOrUpdate(appContext, isSilentUpdate = isAlreadyShowing && !isTestModeActive)

        Log.d(TAG, "Dispatched Live Updates Foreground Service: ${alertInfo.municipalityName} Lv.${alertInfo.alertLevel.level}")
    }

    /**
     * 警戒レベルのカラー 12-sided cookie (MaterialShape) バッジビットマップを動的生成
     */
    private fun createLevelBadgeBitmap(context: Context, level: AlertLevel): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (96 * density).toInt().coerceAtLeast(96)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Material 3 Expressive 12-sided cookie シェイプの描画 (中央に大きく描画)
        val cookiePath = get12SidedCookiePath(size.toFloat())
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = level.colorHex.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawPath(cookiePath, bgPaint)

        // レベル5（黒）およびレベル1（薄グレー）の場合は通知背景に埋もれないよう外枠線を描画
        if (level == AlertLevel.LEVEL_5 || level == AlertLevel.LEVEL_1) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (level == AlertLevel.LEVEL_5) android.graphics.Color.WHITE else android.graphics.Color.GRAY
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * density
            }
            canvas.drawPath(cookiePath, strokePaint)
        }

        // "Lv.X" テキスト描画 (高コントラストフォント色、中央配置)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = level.textColorHex.toInt()
            textSize = size * 0.40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("Lv.${level.level}", size / 2f, yPos, textPaint)

        return bitmap
    }

    /**
     * Material 3 Expressive の 12-sided cookie (Cookie12Sided) パスを取得
     * [0, 1]x[0, 1] の正規化座標系をビットマップ全域へ適切に拡大・センタリング
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun get12SidedCookiePath(size: Float): android.graphics.Path {
        return try {
            val padding = size * 0.04f
            val drawSize = size - padding * 2f
            val matrix = Matrix().apply {
                setScale(drawSize, drawSize)
                postTranslate(padding, padding)
            }
            MaterialShapes.Cookie12Sided.transformed(matrix).toPath()
        } catch (_: Throwable) {
            createGeometric12SidedCookiePath(size)
        }
    }

    /**
     * Compose環境外（ユニットテスト・特殊プロセス等）でも確実に動作する幾何学12花弁（12-lobed rounded scallop）クッキーパス
     */
    private fun createGeometric12SidedCookiePath(size: Float): android.graphics.Path {
        val path = android.graphics.Path()
        val cx = size / 2f
        val cy = size / 2f
        val rOut = size * 0.46f
        val rIn = size * 0.38f
        val lobes = 12
        val step = (2.0 * Math.PI / lobes).toFloat()
        val halfStep = step / 2f

        for (i in 0 until lobes) {
            val anglePeak = i * step - (Math.PI / 2.0).toFloat()
            val angleValley1 = anglePeak - halfStep
            val angleValley2 = anglePeak + halfStep

            val xV1 = (cx + rIn * Math.cos(angleValley1.toDouble())).toFloat()
            val yV1 = (cy + rIn * Math.sin(angleValley1.toDouble())).toFloat()

            val xPeak = (cx + rOut * Math.cos(anglePeak.toDouble())).toFloat()
            val yPeak = (cy + rOut * Math.sin(anglePeak.toDouble())).toFloat()

            val xV2 = (cx + rIn * Math.cos(angleValley2.toDouble())).toFloat()
            val yV2 = (cy + rIn * Math.sin(angleValley2.toDouble())).toFloat()

            val ctrl1X = (cx + (rOut * 1.05f) * Math.cos(anglePeak - halfStep * 0.5)).toFloat()
            val ctrl1Y = (cy + (rOut * 1.05f) * Math.sin(anglePeak - halfStep * 0.5)).toFloat()

            val ctrl2X = (cx + (rOut * 1.05f) * Math.cos(anglePeak + halfStep * 0.5)).toFloat()
            val ctrl2Y = (cy + (rOut * 1.05f) * Math.sin(anglePeak + halfStep * 0.5)).toFloat()

            if (i == 0) {
                path.moveTo(xV1, yV1)
            }
            path.cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, xV2, yV2)
        }
        path.close()
        return path
    }

    /**
     * 設定画面やデバッグ画面からの動作確認用テスト通知メソッド
     */
    fun sendTestNotification(context: Context, testLevel: AlertLevel = AlertLevel.LEVEL_4) {
        clearDismissalSuppression(context)
        isTestModeActive = true
        val testCity = WeatherRepository.resolveTestCityName(context)
        val dummyAlert = MunicipalityAlertInfo(
            municipalityCode = "1310100",
            municipalityName = "$testCity (テスト)",
            prefectureCode = "130000",
            prefectureName = "東京都",
            alertLevel = testLevel,
            disasterFlags = DisasterTargetFlags(
                heavyRain = true,
                landslide = true,
                inundation = true,
                flood = true
            ),
            activeWarnings = listOf(
                MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true),
                MunicipalityWarningItem("04", "洪水警報", "発表", isWarning = true),
                MunicipalityWarningItem("14", "雷注意報", "継続", isWarning = false)
            ),
            targetDisasterPrimary = DisasterType.HEAVY_RAIN,
            disasterDetails = listOf(
                KikikuruDisasterDetail(
                    disasterType = DisasterType.HEAVY_RAIN,
                    alertLevel = testLevel,
                    activeWarnings = listOf(MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true))
                ),
                KikikuruDisasterDetail(
                    disasterType = DisasterType.LANDSLIDE,
                    alertLevel = testLevel,
                    activeWarnings = listOf(MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true))
                ),
                KikikuruDisasterDetail(
                    disasterType = DisasterType.INUNDATION,
                    alertLevel = if (testLevel.level >= 3) AlertLevel.LEVEL_3 else testLevel,
                    activeWarnings = listOf(MunicipalityWarningItem("03", "大雨警報", "発表", isWarning = true))
                ),
                KikikuruDisasterDetail(
                    disasterType = DisasterType.FLOOD,
                    alertLevel = if (testLevel.level >= 2) AlertLevel.LEVEL_2 else testLevel,
                    activeWarnings = listOf(MunicipalityWarningItem("04", "洪水注意報", "発表", isWarning = false))
                )
            ),
            headlineText = "大雨警報・洪水警報が発表されました。命を守るため危険な場所から避難してください。"
        )
        updateLiveAlertNotification(context, dummyAlert)
    }

    /**
     * 通知の完全消去および常駐サービス停止
     */
    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        isTestModeActive = false
        currentActiveAlertInfo = null
        currentActiveCityName = ""
        lastNotificationPostedAtMillis = 0L
        persistAlertTracking(appContext, 0, "")
        persistActiveAlert(appContext, null, "")
        KikikuruLiveAlertService.stop(appContext)
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
