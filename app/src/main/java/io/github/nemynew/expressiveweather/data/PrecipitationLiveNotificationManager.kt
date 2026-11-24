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
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.nemynew.expressiveweather.MainActivity
import io.github.nemynew.expressiveweather.ui.screens.RadarDetailActivity
import io.github.nemynew.expressiveweather.util.LocaleUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
internal data class RainNotificationDismissal(
    val normalizedCityKey: String,
    val dismissedStatusName: String,
    val dismissedTargetTimestampMillis: Long? = null,
    val dismissedAtMillis: Long,
    val suppressUntilMillis: Long
)

/**
 * 雨雲の接近 & 止むまでのカウントダウン Live Updates 通知マネージャー
 */
object PrecipitationLiveNotificationManager {

    private const val TAG = "RainLiveNotifManager"
    private const val PREFS_NAME = "expressive_weather_prefs"
    private const val KEY_ACTIVE_NOWCAST_JSON = "rain_live_active_nowcast_json"
    private const val KEY_ACTIVE_CITY_NAME = "rain_live_active_city_name"
    private const val KEY_DISMISSED_STATE_JSON = "rain_live_dismissed_state_json"
    private const val MIN_DISMISS_SUPPRESS_MS = 2 * 60 * 60 * 1000L
    private const val UNTIMED_RAIN_DISMISS_SUPPRESS_MS = 3 * 60 * 60 * 1000L
    private const val POST_TARGET_BUFFER_MS = 30 * 60 * 1000L
    private const val MAX_DISMISS_SUPPRESS_MS = 6 * 60 * 60 * 1000L
    private const val NOTIFICATION_POST_GRACE_MS = 5_000L
    const val CHANNEL_ID = "rain_live_countdown"
    const val NOTIFICATION_ID = 9120

    @Volatile
    var currentActiveNowcast: PrecipitationNowcast? = null
        private set

    @Volatile
    var currentActiveCityName: String = ""
        private set

    @Volatile
    var currentActiveLocation: android.location.Location? = null
        private set

    @Volatile
    var isTestActive: Boolean = false
        private set

    @Volatile
    private var lastUserDismissal: RainNotificationDismissal? = null

    @Volatile
    private var dismissalLoadedFromPrefs: Boolean = false

    @Volatile
    internal var lastNotificationPostedAtMillis: Long = 0L

    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickerJob: Job? = null

    internal fun normalizeCityKey(cityName: String): String {
        return cityName.trim().lowercase(java.util.Locale.ROOT)
    }

    internal fun createDismissalRecord(
        cityName: String,
        status: RainStatus,
        targetTimestampMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): RainNotificationDismissal {
        val rawSuppressUntil = if (targetTimestampMillis != null && targetTimestampMillis > 0L) {
            maxOf(nowMillis + MIN_DISMISS_SUPPRESS_MS, targetTimestampMillis + POST_TARGET_BUFFER_MS)
        } else {
            nowMillis + UNTIMED_RAIN_DISMISS_SUPPRESS_MS
        }
        val boundedSuppressUntil = rawSuppressUntil.coerceAtMost(nowMillis + MAX_DISMISS_SUPPRESS_MS)
        return RainNotificationDismissal(
            normalizedCityKey = normalizeCityKey(cityName),
            dismissedStatusName = status.name,
            dismissedTargetTimestampMillis = targetTimestampMillis,
            dismissedAtMillis = nowMillis,
            suppressUntilMillis = boundedSuppressUntil
        )
    }

    internal fun shouldSuppressNotification(
        dismissal: RainNotificationDismissal?,
        nowcast: PrecipitationNowcast,
        cityName: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (dismissal == null) return false
        if (nowMillis >= dismissal.suppressUntilMillis) return false
        if (nowcast.status != RainStatus.RAIN_APPROACHING && nowcast.status != RainStatus.RAINING) {
            return false
        }
        val incomingCityKey = normalizeCityKey(cityName)
        if (dismissal.normalizedCityKey.isNotBlank() &&
            incomingCityKey.isNotBlank() &&
            dismissal.normalizedCityKey != incomingCityKey
        ) {
            return false
        }
        return true
    }

    private fun persistDismissalState(context: Context, dismissal: RainNotificationDismissal?) {
        lastUserDismissal = dismissal
        dismissalLoadedFromPrefs = true
        try {
            val editor = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            if (dismissal == null) {
                editor.remove(KEY_DISMISSED_STATE_JSON)
            } else {
                editor.putString(KEY_DISMISSED_STATE_JSON, WeatherJsonSerializer.json.encodeToString(dismissal))
            }
            editor.apply()
        } catch (_: Exception) {}
    }

    private fun getDismissalState(context: Context, nowMillis: Long = System.currentTimeMillis()): RainNotificationDismissal? {
        if (!dismissalLoadedFromPrefs) {
            dismissalLoadedFromPrefs = true
            try {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val raw = prefs.getString(KEY_DISMISSED_STATE_JSON, null)
                if (!raw.isNullOrBlank()) {
                    lastUserDismissal = WeatherJsonSerializer.json.decodeFromString<RainNotificationDismissal>(raw)
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
        nowcast: PrecipitationNowcast,
        cityName: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val dismissal = getDismissalState(context, nowMillis)
        return shouldSuppressNotification(dismissal, nowcast, cityName, nowMillis)
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
        cityName: String = "",
        status: RainStatus? = null,
        targetTimestampMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val appContext = context.applicationContext
        val wasTest = isTestActive
        val activeNowcast = currentActiveNowcast
        val effectiveCity = cityName.ifBlank { currentActiveCityName }
        val effectiveStatus = status ?: activeNowcast?.status ?: RainStatus.RAIN_APPROACHING
        val effectiveTarget = targetTimestampMillis ?: activeNowcast?.targetTimestampMillis

        if (!wasTest && (effectiveStatus == RainStatus.RAIN_APPROACHING || effectiveStatus == RainStatus.RAINING)) {
            val record = createDismissalRecord(
                cityName = effectiveCity,
                status = effectiveStatus,
                targetTimestampMillis = effectiveTarget,
                nowMillis = nowMillis
            )
            persistDismissalState(appContext, record)
            Log.d(TAG, "Recorded user dismissal suppression for city='$effectiveCity', status=$effectiveStatus until=${record.suppressUntilMillis}")
        }
        cancel(appContext)
    }

    private fun createDismissPendingIntent(
        context: Context,
        cityName: String,
        status: RainStatus?,
        targetTimestampMillis: Long?
    ): PendingIntent {
        val appContext = context.applicationContext
        val dismissIntent = Intent(appContext, PrecipitationLiveDismissReceiver::class.java).apply {
            action = PrecipitationLiveDismissReceiver.ACTION_DISMISS_RAIN_LIVE
            putExtra(PrecipitationLiveDismissReceiver.EXTRA_CITY_NAME, cityName)
            if (status != null) {
                putExtra(PrecipitationLiveDismissReceiver.EXTRA_RAIN_STATUS, status.name)
            }
            if (targetTimestampMillis != null && targetTimestampMillis > 0L) {
                putExtra(PrecipitationLiveDismissReceiver.EXTRA_TARGET_TIMESTAMP_MS, targetTimestampMillis)
            }
        }
        return PendingIntent.getBroadcast(
            appContext,
            NOTIFICATION_ID + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun persistActiveState(context: Context, nowcast: PrecipitationNowcast?, cityName: String) {
        try {
            val editor = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            if (nowcast == null) {
                editor.remove(KEY_ACTIVE_NOWCAST_JSON).remove(KEY_ACTIVE_CITY_NAME)
            } else {
                editor.putString(KEY_ACTIVE_NOWCAST_JSON, WeatherJsonSerializer.json.encodeToString(nowcast))
                    .putString(KEY_ACTIVE_CITY_NAME, cityName)
            }
            editor.apply()
        } catch (_: Exception) {}
    }

    fun restoreIfNeeded(context: Context) {
        if (currentActiveNowcast != null) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_ACTIVE_NOWCAST_JSON, null) ?: return
            val restored = WeatherJsonSerializer.json.decodeFromString<PrecipitationNowcast>(json)
            val targetMs = restored.targetTimestampMillis
            if (targetMs != null && targetMs <= System.currentTimeMillis()) {
                persistActiveState(context, null, "")
                return
            }
            val savedCityName = prefs.getString(KEY_ACTIVE_CITY_NAME, "") ?: ""
            if (isSuppressedByUserDismissal(context, restored, savedCityName)) {
                persistActiveState(context, null, "")
                return
            }
            val resolvedLocCity = WeatherRepository.getLastResolvedLocationCity(context)
            if (savedCityName == "東京" && resolvedLocCity != null && resolvedLocCity.name != "東京") {
                currentActiveNowcast = null
                currentActiveCityName = ""
                persistActiveState(context, null, "")
                return
            }
            currentActiveNowcast = restored
            currentActiveCityName = savedCityName
        } catch (_: Exception) {}
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val language = LocaleUtils.getSavedLanguage(context.applicationContext)
            val name = when (language) {
                AppLanguage.ENGLISH -> "Rain Countdown Live Updates"
                AppLanguage.KOREAN -> "강수 카운트다운 실시간 알림"
                AppLanguage.SIMPLIFIED_CHINESE -> "降雨倒计时实时动态通知"
                AppLanguage.TRADITIONAL_CHINESE -> "降雨倒數即時動態通知"
                AppLanguage.SPANISH -> "Cuenta regresiva de lluvia en vivo"
                AppLanguage.FRENCH -> "Compte à rebours pluie en direct"
                AppLanguage.PORTUGUESE -> "Contagem regressiva de chuva ao vivo"
                AppLanguage.JAPANESE -> "雨雲接近・降雨カウントダウン Live Updates"
            }
            val descriptionText = when (language) {
                AppLanguage.ENGLISH -> "Real-time countdown notifications for approaching rain and when rain will stop"
                AppLanguage.KOREAN -> "비구름 접근 시 강수 시작 예측 및 비 그침까지 남은 시간 실시간 카운트다운 알림"
                AppLanguage.SIMPLIFIED_CHINESE -> "雨云临近及雨停剩余时间的实时倒计时通知"
                AppLanguage.TRADITIONAL_CHINESE -> "雨雲接近及雨停剩餘時間的即時倒數通知"
                AppLanguage.SPANISH -> "Notificaciones en tiempo real cuando se acerca la lluvia y cuándo dejará de llover"
                AppLanguage.FRENCH -> "Notifications en temps réel à l'approche de la pluie et jusqu'à son arrêt"
                AppLanguage.PORTUGUESE -> "Notificações em tempo real da aproximação da chuva e de quando ela vai parar"
                AppLanguage.JAPANESE -> "雨雲接近時の降り始め予測および雨が止むまでの残り時間のリアルタイム・カウントダウン通知"
            }
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = AndroidColor.CYAN
                enableVibration(true)
                // 雨雲接近を直感的に伝える軽快な雨粒リズム
                vibrationPattern = longArrayOf(0L, 24L, 55L, 28L, 55L, 42L)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildRainLiveNotification(
        context: Context,
        nowcast: PrecipitationNowcast,
        cityName: String,
        isSilentUpdate: Boolean = false
    ): Notification {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)

        val language = LocaleUtils.getSavedLanguage(appContext)
        val status = nowcast.status
        val minutesRemaining = nowcast.minutesRemaining
        val targetTime = nowcast.localizedTargetTimeLabel(language) ?: when (language) {
            AppLanguage.ENGLISH -> "shortly"
            AppLanguage.KOREAN -> "곧"
            AppLanguage.SIMPLIFIED_CHINESE -> "即将"
            AppLanguage.TRADITIONAL_CHINESE -> "即將"
            AppLanguage.SPANISH -> "en breve"
            AppLanguage.FRENCH -> "bientôt"
            AppLanguage.PORTUGUESE -> "em breve"
            AppLanguage.JAPANESE -> "まもなく"
        }
        val targetTimestamp = nowcast.targetTimestampMillis

        // ステータスバーピル / Galaxy Now Bar 用の簡潔な短縮クリティカルテキスト
        val chipText = if (minutesRemaining != null) {
            when (status) {
                RainStatus.RAIN_APPROACHING -> when (language) {
                    AppLanguage.ENGLISH -> "Rain ${minutesRemaining}m"
                    AppLanguage.KOREAN -> "${minutesRemaining}분 후 비"
                    AppLanguage.SIMPLIFIED_CHINESE -> "${minutesRemaining}分后雨"
                    AppLanguage.TRADITIONAL_CHINESE -> "${minutesRemaining}分後雨"
                    AppLanguage.SPANISH -> "Lluvia ${minutesRemaining}m"
                    AppLanguage.FRENCH -> "Pluie ${minutesRemaining}m"
                    AppLanguage.PORTUGUESE -> "Chuva ${minutesRemaining}m"
                    AppLanguage.JAPANESE -> "あと${minutesRemaining}分で雨"
                }
                RainStatus.RAINING -> when (language) {
                    AppLanguage.ENGLISH -> "Stops ${minutesRemaining}m"
                    AppLanguage.KOREAN -> "${minutesRemaining}분 후 그침"
                    AppLanguage.SIMPLIFIED_CHINESE -> "${minutesRemaining}分后停"
                    AppLanguage.TRADITIONAL_CHINESE -> "${minutesRemaining}分後停"
                    AppLanguage.SPANISH -> "Para ${minutesRemaining}m"
                    AppLanguage.FRENCH -> "Fin ${minutesRemaining}m"
                    AppLanguage.PORTUGUESE -> "Para ${minutesRemaining}m"
                    AppLanguage.JAPANESE -> "あと${minutesRemaining}分で止む"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Rain ${minutesRemaining}m"
                    AppLanguage.KOREAN -> "${minutesRemaining}분 후 비"
                    AppLanguage.SIMPLIFIED_CHINESE -> "${minutesRemaining}分后雨"
                    AppLanguage.TRADITIONAL_CHINESE -> "${minutesRemaining}分後雨"
                    AppLanguage.SPANISH -> "Lluvia ${minutesRemaining}m"
                    AppLanguage.FRENCH -> "Pluie ${minutesRemaining}m"
                    AppLanguage.PORTUGUESE -> "Chuva ${minutesRemaining}m"
                    AppLanguage.JAPANESE -> "あと${minutesRemaining}分"
                }
            }
        } else {
            when (language) {
                AppLanguage.ENGLISH -> "Steady rain"
                AppLanguage.KOREAN -> "비 계속"
                AppLanguage.SIMPLIFIED_CHINESE -> "持续降雨"
                AppLanguage.TRADITIONAL_CHINESE -> "持續降雨"
                AppLanguage.SPANISH -> "Lluvia continua"
                AppLanguage.FRENCH -> "Pluie continue"
                AppLanguage.PORTUGUESE -> "Chuva contínua"
                AppLanguage.JAPANESE -> "降雨継続中"
            }
        }

        val title = when (status) {
            RainStatus.RAIN_APPROACHING -> when (language) {
                AppLanguage.ENGLISH -> "Rain starting shortly • $cityName"
                AppLanguage.KOREAN -> "곧 비가 내리기 시작합니다 • $cityName"
                AppLanguage.SIMPLIFIED_CHINESE -> "即将开始下雨 • $cityName"
                AppLanguage.TRADITIONAL_CHINESE -> "即將開始下雨 • $cityName"
                AppLanguage.SPANISH -> "Empezará a llover en breve • $cityName"
                AppLanguage.FRENCH -> "La pluie arrive bientôt • $cityName"
                AppLanguage.PORTUGUESE -> "Vai começar a chover em breve • $cityName"
                AppLanguage.JAPANESE -> "まもなく雨が降り始めます • $cityName"
            }
            RainStatus.RAINING -> if (minutesRemaining != null) {
                when (language) {
                    AppLanguage.ENGLISH -> "Rain continuing until ~$targetTime • $cityName"
                    AppLanguage.KOREAN -> "${targetTime}경까지 비가 이어질 전망 • $cityName"
                    AppLanguage.SIMPLIFIED_CHINESE -> "预计下雨至 ${targetTime} 左右 • $cityName"
                    AppLanguage.TRADITIONAL_CHINESE -> "預計下雨至 ${targetTime} 左右 • $cityName"
                    AppLanguage.SPANISH -> "Lluvia hasta aprox. las $targetTime • $cityName"
                    AppLanguage.FRENCH -> "Pluie jusqu'à environ $targetTime • $cityName"
                    AppLanguage.PORTUGUESE -> "Chuva até por volta das $targetTime • $cityName"
                    AppLanguage.JAPANESE -> "雨は${targetTime}頃まで続く見込み • $cityName"
                }
            } else {
                when (language) {
                    AppLanguage.ENGLISH -> "Steady rain continues • $cityName"
                    AppLanguage.KOREAN -> "당분간 비가 계속될 전망 • $cityName"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降雨持续中 • $cityName"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨持續中 • $cityName"
                    AppLanguage.SPANISH -> "Continúa la lluvia • $cityName"
                    AppLanguage.FRENCH -> "La pluie continue • $cityName"
                    AppLanguage.PORTUGUESE -> "Chuva contínua persiste • $cityName"
                    AppLanguage.JAPANESE -> "雨は当面降り続く見込み • $cityName"
                }
            }
            else -> when (language) {
                AppLanguage.ENGLISH -> "Precipitation Forecast • $cityName"
                AppLanguage.KOREAN -> "강수 예보 • $cityName"
                AppLanguage.SIMPLIFIED_CHINESE -> "降水预报 • $cityName"
                AppLanguage.TRADITIONAL_CHINESE -> "降水預報 • $cityName"
                AppLanguage.SPANISH -> "Pronóstico de precipitación • $cityName"
                AppLanguage.FRENCH -> "Prévision de précipitations • $cityName"
                AppLanguage.PORTUGUESE -> "Previsão de precipitação • $cityName"
                AppLanguage.JAPANESE -> "降雨予測 • $cityName"
            }
        }

        val contentText = when (status) {
            RainStatus.RAIN_APPROACHING -> if (minutesRemaining != null) {
                when (language) {
                    AppLanguage.ENGLISH -> "Rain expected around $targetTime (in about ${minutesRemaining} min)"
                    AppLanguage.KOREAN -> "${targetTime}경 비 시작 예상 (약 ${minutesRemaining}분 후)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "预计 ${targetTime} 左右开始下雨（距现在约 ${minutesRemaining} 分钟）"
                    AppLanguage.TRADITIONAL_CHINESE -> "預計 ${targetTime} 左右開始下雨（距現在約 ${minutesRemaining} 分鐘）"
                    AppLanguage.SPANISH -> "Se espera lluvia sobre las $targetTime (en aprox. ${minutesRemaining} min)"
                    AppLanguage.FRENCH -> "Pluie attendue vers $targetTime (dans env. ${minutesRemaining} min)"
                    AppLanguage.PORTUGUESE -> "Previsão de chuva por volta das $targetTime (em aprox. ${minutesRemaining} min)"
                    AppLanguage.JAPANESE -> "${targetTime}頃から雨の予想（あと約${minutesRemaining}分）"
                }
            } else nowcast.localizedSubText(language)
            RainStatus.RAINING -> if (minutesRemaining != null) {
                when (language) {
                    AppLanguage.ENGLISH -> "About ${minutesRemaining} min until rain stops"
                    AppLanguage.KOREAN -> "비가 그치기까지 약 ${minutesRemaining}분 남았습니다"
                    AppLanguage.SIMPLIFIED_CHINESE -> "距离雨停大约还有 ${minutesRemaining} 分钟"
                    AppLanguage.TRADITIONAL_CHINESE -> "距離雨停大約還有 ${minutesRemaining} 分鐘"
                    AppLanguage.SPANISH -> "Aprox. ${minutesRemaining} min para que deje de llover"
                    AppLanguage.FRENCH -> "Environ ${minutesRemaining} min avant la fin de la pluie"
                    AppLanguage.PORTUGUESE -> "Cerca de ${minutesRemaining} min para a chuva parar"
                    AppLanguage.JAPANESE -> "雨が止むまであと約${minutesRemaining}分です"
                }
            } else nowcast.localizedSubText(language)
            else -> nowcast.localizedSubText(language)
        }

        val bigText = buildString {
            appendLine(nowcast.localizedHeadline(language))
            appendLine(nowcast.localizedSubText(language))
            if (nowcast.currentIntensityMm > 0.0) {
                append("${String.format(java.util.Locale.US, "%.1f", nowcast.currentIntensityMm)} mm/h")
            } else if (nowcast.peakIntensityMm > 0.0) {
                append("${String.format(java.util.Locale.US, "%.1f", nowcast.peakIntensityMm)} mm/h (${nowcast.maxProbability}%)")
            }
        }.trim()

        val radarIntent = Intent(appContext, RadarDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("initial_layer_id", "hrpns")
            putExtra("city_name", cityName)
        }
        val radarPendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            radarIntent,
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
        val dismissPendingIntent = createDismissPendingIntent(
            context = appContext,
            cityName = cityName,
            status = status,
            targetTimestampMillis = targetTimestamp
        )

        val badgeBitmap = createRainBadgeBitmap(appContext, status, minutesRemaining)
        val primaryColor = if (status == RainStatus.RAIN_APPROACHING) AndroidColor.parseColor("#0288D1") else AndroidColor.parseColor("#00ACC1")
        val trackColor = if (status == RainStatus.RAIN_APPROACHING) AndroidColor.parseColor("#4D0288D1") else AndroidColor.parseColor("#4D00ACC1")

        // Android 16 (API 36) ステータスバー Live Updates チップ用の簡潔なテキスト
        val statusBarChipText = if (minutesRemaining != null) {
            when (language) {
                AppLanguage.ENGLISH -> "${minutesRemaining}m"
                AppLanguage.KOREAN -> "${minutesRemaining}분"
                AppLanguage.SIMPLIFIED_CHINESE -> "${minutesRemaining}分"
                AppLanguage.TRADITIONAL_CHINESE -> "${minutesRemaining}分"
                AppLanguage.SPANISH -> "${minutesRemaining}m"
                AppLanguage.FRENCH -> "${minutesRemaining}m"
                AppLanguage.PORTUGUESE -> "${minutesRemaining}m"
                AppLanguage.JAPANESE -> "あと${minutesRemaining}分"
            }
        } else {
            when (language) {
                AppLanguage.ENGLISH -> "Rain"
                AppLanguage.KOREAN -> "비 계속"
                AppLanguage.SIMPLIFIED_CHINESE -> "降雨中"
                AppLanguage.TRADITIONAL_CHINESE -> "降雨中"
                AppLanguage.SPANISH -> "Lluvia"
                AppLanguage.FRENCH -> "Pluie"
                AppLanguage.PORTUGUESE -> "Chuva"
                AppLanguage.JAPANESE -> "降雨中"
            }
        }

        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        val progressMinutes = minutesRemaining?.takeIf { Build.VERSION.SDK_INT >= 36 && !isSamsung && it in 1..60 }

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(io.github.nemynew.expressiveweather.R.drawable.ic_fluent_weather_rain_24)
            .setLargeIcon(badgeBitmap)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(chipText)
            .setColor(primaryColor)
            .setColorized(false)
            .setGroupSummary(false)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(statusBarChipText)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(isSilentUpdate)
            .setContentIntent(radarPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setAutoCancel(false)
            .addAction(
                android.R.drawable.ic_dialog_map,
                when (language) {
                    AppLanguage.JAPANESE -> "雨雲レーダー"
                    AppLanguage.KOREAN -> "강수 레이더"
                    AppLanguage.ENGLISH -> "Radar"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达"
                    AppLanguage.TRADITIONAL_CHINESE -> "降水雷達"
                    AppLanguage.SPANISH -> "Radar"
                    AppLanguage.FRENCH -> "Radar"
                    AppLanguage.PORTUGUESE -> "Radar"
                },
                radarPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_info_details,
                when (language) {
                    AppLanguage.JAPANESE -> "アプリを開く"
                    AppLanguage.KOREAN -> "앱 열기"
                    AppLanguage.ENGLISH -> "Open App"
                    AppLanguage.SIMPLIFIED_CHINESE -> "打开应用"
                    AppLanguage.TRADITIONAL_CHINESE -> "開啟應用程式"
                    AppLanguage.SPANISH -> "Abrir app"
                    AppLanguage.FRENCH -> "Ouvrir l'app"
                    AppLanguage.PORTUGUESE -> "Abrir app"
                },
                mainAppPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                when (language) {
                    AppLanguage.JAPANESE -> "閉じる"
                    AppLanguage.KOREAN -> "닫기"
                    AppLanguage.ENGLISH -> "Dismiss"
                    AppLanguage.SIMPLIFIED_CHINESE -> "关闭"
                    AppLanguage.TRADITIONAL_CHINESE -> "關閉"
                    AppLanguage.SPANISH -> "Descartar"
                    AppLanguage.FRENCH -> "Ignorer"
                    AppLanguage.PORTUGUESE -> "Dispensar"
                },
                dismissPendingIntent
            )

        if (progressMinutes != null) {
            val totalMinutes = 60
            val elapsed = (totalMinutes - progressMinutes).coerceIn(1, totalMinutes - 1)
            val remaining = (totalMinutes - elapsed).coerceAtLeast(1)
            val progressStyle = NotificationCompat.ProgressStyle()
                .setProgress(elapsed)
                .addProgressSegment(NotificationCompat.ProgressStyle.Segment(elapsed).setColor(primaryColor))
                .addProgressSegment(NotificationCompat.ProgressStyle.Segment(remaining).setColor(trackColor))
            builder.setStyle(progressStyle)
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(chipText)
                    .bigText(bigText)
            )
        }

        // Galaxy One UI 7 Now Bar 互換用エクストラ設定
        builder.extras.putBoolean("com.samsung.android.app.notification.nowbar", true)
        builder.extras.putString("com.samsung.android.app.notification.nowbar.title", chipText)
        builder.extras.putString("com.samsung.android.app.notification.nowbar.text", cityName)
        builder.extras.putString("com.samsung.android.app.notification.subtext", chipText)
        builder.extras.putString("com.samsung.android.app.notification.title", chipText)

        if (targetTimestamp != null) {
            builder.setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetTimestamp)
        } else {
            builder.setUsesChronometer(false)
                .setShowWhen(true)
        }

        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= 36 && io.github.nemynew.expressiveweather.BuildConfig.DEBUG) {
            Log.d(TAG, "Built API 36 rain Live Update: promotable=${notification.hasPromotableCharacteristics()}, chip='$statusBarChipText'")
        }
        return notification
    }

    fun buildSafeForegroundNotification(context: Context): Notification {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)
        val language = LocaleUtils.getSavedLanguage(appContext)
        val nowcast = currentActiveNowcast
        if (nowcast != null && nowcast.isLiveActive) {
            val fallbackCity = when (language) {
                AppLanguage.ENGLISH -> "Current Location"
                AppLanguage.KOREAN -> "현재 위치"
                AppLanguage.SIMPLIFIED_CHINESE -> "当前位置"
                AppLanguage.TRADITIONAL_CHINESE -> "目前位置"
                AppLanguage.SPANISH -> "Ubicación actual"
                AppLanguage.FRENCH -> "Position actuelle"
                AppLanguage.PORTUGUESE -> "Localização atual"
                AppLanguage.JAPANESE -> "現在地"
            }
            return buildRainLiveNotification(appContext, nowcast, currentActiveCityName.ifBlank { fallbackCity })
        }
        val dismissPendingIntent = createDismissPendingIntent(
            context = appContext,
            cityName = currentActiveCityName,
            status = currentActiveNowcast?.status,
            targetTimestampMillis = currentActiveNowcast?.targetTimestampMillis
        )
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(io.github.nemynew.expressiveweather.R.drawable.ic_fluent_weather_rain_24)
            .setContentTitle(
                when (language) {
                    AppLanguage.ENGLISH -> "Monitoring precipitation"
                    AppLanguage.KOREAN -> "비구름 접근·강수 예보 감시 중"
                    AppLanguage.SIMPLIFIED_CHINESE -> "正在监测降水"
                    AppLanguage.TRADITIONAL_CHINESE -> "正在監測降水"
                    AppLanguage.SPANISH -> "Monitoreando precipitaciones"
                    AppLanguage.FRENCH -> "Surveillance des précipitations"
                    AppLanguage.PORTUGUESE -> "Monitorando precipitações"
                    AppLanguage.JAPANESE -> "雨雲接近・降雨予測を監視中"
                }
            )
            .setContentText(
                when (language) {
                    AppLanguage.ENGLISH -> "Checking radar for your current location"
                    AppLanguage.KOREAN -> "현재 위치의 최신 기상 레이더를 확인하고 있습니다"
                    AppLanguage.SIMPLIFIED_CHINESE -> "正在获取当前位置的最新雷达数据"
                    AppLanguage.TRADITIONAL_CHINESE -> "正在取得目前位置的最新雷達資料"
                    AppLanguage.SPANISH -> "Consultando el radar para tu ubicación actual"
                    AppLanguage.FRENCH -> "Consultation du radar pour votre position actuelle"
                    AppLanguage.PORTUGUESE -> "Verificando o radar para sua localização atual"
                    AppLanguage.JAPANESE -> "現在地の最新気象レーダーを受信待機しています"
                }
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setDeleteIntent(dismissPendingIntent)
            .setAutoCancel(false)
        return builder.build()
    }

    fun updateRainLiveNotification(
        context: Context,
        nowcast: PrecipitationNowcast,
        cityName: String,
        location: android.location.Location? = null
    ) {
        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!WeatherRepository.isLiveAlertNotificationEnabled(appContext)) {
            cancel(appContext)
            return
        }

        if (isTestActive && !nowcast.isLiveActive) {
            return
        }

        val isActiveState = nowcast.status == RainStatus.RAIN_APPROACHING || nowcast.status == RainStatus.RAINING
        if (!isActiveState || (nowcast.minutesRemaining != null && nowcast.minutesRemaining <= 0)) {
            onNowcastCleared(appContext, cityName)
            return
        }

        if (!canPostNotifications(appContext)) {
            return
        }

        if (!isTestActive && isSuppressedByUserDismissal(appContext, nowcast, cityName)) {
            Log.d(TAG, "Skipping rain live notification: suppressed by user dismissal for '$cityName'.")
            if (PrecipitationLiveService.isServiceRunning || tickerJob?.isActive == true) {
                cancel(appContext)
            }
            return
        }

        if (!isTestActive &&
            (PrecipitationLiveService.isServiceRunning || tickerJob?.isActive == true) &&
            hasUserDismissedActiveNotification(appContext)
        ) {
            Log.d(TAG, "Detected dismissed rain live notification during update; suppressing re-post for '$cityName'.")
            dismissByUser(
                context = appContext,
                cityName = cityName,
                status = nowcast.status,
                targetTimestampMillis = nowcast.targetTimestampMillis
            )
            return
        }

        val alreadyShowing = isNotificationCurrentlyShowing(appContext)
        val prev = currentActiveNowcast
        val isIdenticalAlreadyShowing = !isTestActive &&
            alreadyShowing &&
            prev != null &&
            normalizeCityKey(currentActiveCityName) == normalizeCityKey(cityName) &&
            prev.status == nowcast.status &&
            prev.minutesRemaining == nowcast.minutesRemaining
        if (isIdenticalAlreadyShowing) {
            currentActiveNowcast = nowcast
            currentActiveCityName = cityName
            if (location != null) {
                currentActiveLocation = location
            }
            persistActiveState(appContext, nowcast, cityName)
            return
        }

        currentActiveNowcast = nowcast
        currentActiveCityName = cityName
        if (location != null) {
            currentActiveLocation = location
        }
        persistActiveState(appContext, nowcast, cityName)

        val silentUpdate = alreadyShowing && !isTestActive
        try {
            PrecipitationLiveService.startOrUpdate(appContext, isSilentUpdate = silentUpdate)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to start PrecipitationLiveService, falling back to direct notification: ${e.message}")
            try {
                val notification = buildRainLiveNotification(appContext, nowcast, cityName, isSilentUpdate = silentUpdate)
                notificationManager.notify(NOTIFICATION_ID, notification)
                markNotificationPosted()
                startTickerIfNeeded(appContext)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed direct notification dispatch: ${ex.message}", ex)
            }
        }
    }

    fun updateActiveNowcastFromBackground(
        context: Context,
        nowcast: PrecipitationNowcast,
        cityName: String,
        location: android.location.Location? = null
    ) {
        currentActiveNowcast = nowcast
        currentActiveCityName = cityName
        if (location != null) {
            currentActiveLocation = location
        }
        persistActiveState(context.applicationContext, nowcast, cityName)
    }

    /**
     * アクティブな降水予測が終了・解消された際の遷移処理:
     * - 降雨中 (`RAINING`) だった場合のみ「雨が止みました」通知を表示してサービス停止
     * - 雨雲接近中 (`RAIN_APPROACHING`) やテスト通知の場合は誤通知を出さず静かにキャンセル
     */
    fun onNowcastCleared(context: Context, cityName: String) {
        val prev = currentActiveNowcast
        val wasTest = isTestActive
        if (!wasTest && prev?.status == RainStatus.RAINING) {
            onRainStoppedAtNewLocation(context, cityName)
        } else {
            cancel(context)
        }
    }

    /**
     * 移動先で雨が止んだことを検知した際、または降雨終了時の通知表示と完了処理
     */
    fun onRainStoppedAtNewLocation(context: Context, cityName: String) {
        val appContext = context.applicationContext
        val prev = currentActiveNowcast
        val wasTest = isTestActive
        if (wasTest || prev?.status == RainStatus.RAIN_APPROACHING) {
            cancel(appContext)
            return
        }

        val targetCity = cityName.ifBlank { currentActiveCityName }

        // 先に内部状態とサービス／タイマーを停止し、その後に完了通知を掲示する（即時消去を防止）
        isTestActive = false
        currentActiveNowcast = null
        currentActiveCityName = ""
        currentActiveLocation = null
        tickerJob?.cancel()
        tickerJob = null
        persistActiveState(appContext, null, "")
        PrecipitationLiveService.stop(appContext)

        if (!canPostNotifications(appContext) || !WeatherRepository.isLiveAlertNotificationEnabled(appContext)) return

        postCompletionNotification(appContext, isApproaching = false, cityName = targetCity)
    }

    internal fun completeLiveCountdown(
        context: Context,
        isApproaching: Boolean,
        cityName: String
    ) {
        val appContext = context.applicationContext
        val targetCity = cityName.ifBlank { currentActiveCityName }
        isTestActive = false
        currentActiveNowcast = null
        currentActiveCityName = ""
        currentActiveLocation = null
        persistActiveState(appContext, null, "")
        postCompletionNotification(appContext, isApproaching = isApproaching, cityName = targetCity)
    }

    internal fun postCompletionNotification(
        context: Context,
        isApproaching: Boolean,
        cityName: String
    ) {
        val appContext = context.applicationContext
        if (!canPostNotifications(appContext)) return

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val language = LocaleUtils.getSavedLanguage(appContext)
        val finishChip = if (isApproaching) {
            when (language) {
                AppLanguage.ENGLISH -> "Rain has started"
                AppLanguage.KOREAN -> "비가 내리기 시작했습니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "开始下雨了"
                AppLanguage.TRADITIONAL_CHINESE -> "開始下雨了"
                AppLanguage.SPANISH -> "Ha empezado a llover"
                AppLanguage.FRENCH -> "La pluie a commencé"
                AppLanguage.PORTUGUESE -> "Começou a chover"
                AppLanguage.JAPANESE -> "雨が降り始めました"
            }
        } else {
            when (language) {
                AppLanguage.ENGLISH -> "Rain has stopped"
                AppLanguage.KOREAN -> "비가 그쳤습니다"
                AppLanguage.SIMPLIFIED_CHINESE -> "雨已经停了"
                AppLanguage.TRADITIONAL_CHINESE -> "雨已經停了"
                AppLanguage.SPANISH -> "Ha dejado de llover"
                AppLanguage.FRENCH -> "La pluie s'est arrêtée"
                AppLanguage.PORTUGUESE -> "A chuva parou"
                AppLanguage.JAPANESE -> "雨が止みました"
            }
        }
        val finishTitle = if (cityName.isNotBlank()) "$finishChip • $cityName" else finishChip

        val finishNotif = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(io.github.nemynew.expressiveweather.R.drawable.ic_fluent_weather_rain_24)
            .setContentTitle(finishTitle)
            .setContentText(finishChip)
            .setSubText(finishChip)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        nm.notify(NOTIFICATION_ID, finishNotif)
    }

    internal fun stopFallbackTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun startTickerIfNeeded(context: Context) {
        val appContext = context.applicationContext
        if (tickerJob?.isActive == true) return
        tickerJob = managerScope.launch {
            val locationProvider = CurrentLocationProvider(appContext)
            var tickCount = 0

            while (isActive) {
                delay(60 * 1000L)
                tickCount++

                if (!WeatherRepository.isLiveAlertNotificationEnabled(appContext)) {
                    cancel(appContext)
                    break
                }

                val nowcast = currentActiveNowcast ?: break
                if (!isTestActive && isSuppressedByUserDismissal(appContext, nowcast, currentActiveCityName)) {
                    cancel(appContext)
                    break
                }
                if (hasUserDismissedActiveNotification(appContext)) {
                    Log.d(TAG, "Fallback ticker detected dismissed notification; stopping cleanly.")
                    dismissByUser(
                        context = appContext,
                        cityName = currentActiveCityName,
                        status = nowcast.status,
                        targetTimestampMillis = nowcast.targetTimestampMillis
                    )
                    break
                }
                val targetMillis = nowcast.targetTimestampMillis ?: continue
                val now = System.currentTimeMillis()
                val remainingMin = ((targetMillis - now) / 60000L).toInt()

                val freshLoc = locationProvider.getCurrentLocation(forceFresh = false)
                val prevLoc = currentActiveLocation
                val movedDistance = if (freshLoc != null && prevLoc != null) prevLoc.distanceTo(freshLoc) else 0f

                if (freshLoc != null && (movedDistance >= 75f || tickCount % 3 == 0)) {
                    try {
                        val newCity = locationProvider.resolveCity(freshLoc)
                        val repo = WeatherRepository(context = appContext)
                        val weatherData = repo.refresh(newCity)
                        val freshNowcast = weatherData.precipitationNowcast
                        if (freshNowcast != null && freshNowcast.isLiveActive && (freshNowcast.minutesRemaining == null || freshNowcast.minutesRemaining > 0)) {
                            if (hasUserDismissedActiveNotification(appContext)) {
                                dismissByUser(appContext, newCity.name, freshNowcast.status, freshNowcast.targetTimestampMillis)
                                break
                            }
                            currentActiveLocation = freshLoc
                            currentActiveNowcast = freshNowcast
                            currentActiveCityName = newCity.name
                            persistActiveState(appContext, freshNowcast, newCity.name)
                            if (canPostNotifications(appContext)) {
                                val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                nm.notify(NOTIFICATION_ID, buildRainLiveNotification(appContext, freshNowcast, newCity.name, isSilentUpdate = true))
                                markNotificationPosted()
                            }
                            continue
                        } else {
                            onRainStoppedAtNewLocation(appContext, newCity.name)
                            break
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Fallback ticker location follow error: ${e.message}")
                    }
                }

                if (remainingMin <= 0) {
                    val cityForCompletion = currentActiveCityName
                    val isApproaching = nowcast.status == RainStatus.RAIN_APPROACHING
                    completeLiveCountdown(appContext, isApproaching = isApproaching, cityName = cityForCompletion)
                    tickerJob = null
                    break
                } else {
                    val updatedNowcast = nowcast.copy(minutesRemaining = remainingMin)
                    currentActiveNowcast = updatedNowcast
                    persistActiveState(appContext, updatedNowcast, currentActiveCityName)
                    if (canPostNotifications(appContext)) {
                        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIFICATION_ID, buildRainLiveNotification(appContext, updatedNowcast, currentActiveCityName, isSilentUpdate = true))
                        markNotificationPosted()
                    }
                }
            }
        }
    }

    fun sendTestNotification(context: Context, status: RainStatus = RainStatus.RAIN_APPROACHING) {
        val appContext = context.applicationContext
        if (!WeatherRepository.isLiveAlertNotificationEnabled(appContext)) {
            cancel(appContext)
            return
        }
        clearDismissalSuppression(appContext)
        isTestActive = true
        val mockNowcast = PrecipitationAnalyzer.createMock(status)
        val testCityName = "${WeatherRepository.resolveTestCityName(appContext)} (テスト)"
        updateRainLiveNotification(appContext, mockNowcast, testCityName)
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        isTestActive = false
        currentActiveNowcast = null
        currentActiveCityName = ""
        currentActiveLocation = null
        lastNotificationPostedAtMillis = 0L
        tickerJob?.cancel()
        tickerJob = null
        persistActiveState(appContext, null, "")

        PrecipitationLiveService.stop(appContext)

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    private fun createRainBadgeBitmap(context: Context, status: RainStatus, minutes: Int?): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (56 * density).toInt().coerceAtLeast(56)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (status == RainStatus.RAIN_APPROACHING) AndroidColor.parseColor("#0288D1") else AndroidColor.parseColor("#00838F")
            this.style = Paint.Style.FILL
        }

        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, size * 0.35f, size * 0.35f, bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 20 * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 10 * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        if (minutes != null) {
            canvas.drawText("$minutes", size / 2f, size * 0.55f, textPaint)
            canvas.drawText("min", size / 2f, size * 0.78f, unitPaint)
        } else {
            canvas.drawText("6h+", size / 2f, size * 0.60f, textPaint)
        }

        return bitmap
    }
}
