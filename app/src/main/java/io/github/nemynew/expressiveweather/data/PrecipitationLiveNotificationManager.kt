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
import kotlinx.serialization.encodeToString

/**
 * 雨雲の接近 & 止むまでのカウントダウン Live Updates 通知マネージャー
 */
object PrecipitationLiveNotificationManager {

    private const val TAG = "RainLiveNotifManager"
    private const val PREFS_NAME = "expressive_weather_prefs"
    private const val KEY_ACTIVE_NOWCAST_JSON = "rain_live_active_nowcast_json"
    private const val KEY_ACTIVE_CITY_NAME = "rain_live_active_city_name"
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

    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickerJob: Job? = null

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
            currentActiveNowcast = restored
            val savedCityName = prefs.getString(KEY_ACTIVE_CITY_NAME, "") ?: ""
            val resolvedLocCity = WeatherRepository.getLastResolvedLocationCity(context)
            if (savedCityName == "東京" && resolvedLocCity != null && resolvedLocCity.name != "東京") {
                currentActiveNowcast = null
                currentActiveCityName = ""
                persistActiveState(context, null, "")
                return
            }
            currentActiveCityName = savedCityName
        } catch (_: Exception) {}
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val language = LocaleUtils.getSavedLanguage(context.applicationContext)
            val name = when (language) {
                AppLanguage.ENGLISH -> "Rain Countdown Live Updates"
                AppLanguage.SIMPLIFIED_CHINESE -> "降雨倒计时实时动态通知"
                AppLanguage.TRADITIONAL_CHINESE -> "降雨倒數即時動態通知"
                AppLanguage.JAPANESE -> "雨雲接近・降雨カウントダウン Live Updates"
            }
            val descriptionText = when (language) {
                AppLanguage.ENGLISH -> "Real-time countdown notifications for approaching rain and when rain will stop"
                AppLanguage.SIMPLIFIED_CHINESE -> "雨云临近及雨停剩余时间的实时倒计时通知"
                AppLanguage.TRADITIONAL_CHINESE -> "雨雲接近及雨停剩餘時間的即時倒數通知"
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
        cityName: String
    ): Notification {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)

        val language = LocaleUtils.getSavedLanguage(appContext)
        val status = nowcast.status
        val minutesRemaining = nowcast.minutesRemaining
        val targetTime = nowcast.localizedTargetTimeLabel(language) ?: when (language) {
            AppLanguage.ENGLISH -> "shortly"
            AppLanguage.SIMPLIFIED_CHINESE -> "即将"
            AppLanguage.TRADITIONAL_CHINESE -> "即將"
            AppLanguage.JAPANESE -> "まもなく"
        }
        val targetTimestamp = nowcast.targetTimestampMillis

        // ステータスバーピル / Galaxy Now Bar 用の簡潔な短縮クリティカルテキスト
        val chipText = if (minutesRemaining != null) {
            when (status) {
                RainStatus.RAIN_APPROACHING -> when (language) {
                    AppLanguage.ENGLISH -> "Rain ${minutesRemaining}m"
                    AppLanguage.SIMPLIFIED_CHINESE -> "${minutesRemaining}分后雨"
                    AppLanguage.TRADITIONAL_CHINESE -> "${minutesRemaining}分後雨"
                    AppLanguage.JAPANESE -> "あと${minutesRemaining}分で雨"
                }
                RainStatus.RAINING -> when (language) {
                    AppLanguage.ENGLISH -> "Stops ${minutesRemaining}m"
                    AppLanguage.SIMPLIFIED_CHINESE -> "${minutesRemaining}分后停"
                    AppLanguage.TRADITIONAL_CHINESE -> "${minutesRemaining}分後停"
                    AppLanguage.JAPANESE -> "あと${minutesRemaining}分で止む"
                }
                else -> when (language) {
                    AppLanguage.ENGLISH -> "Rain ${minutesRemaining}m"
                    AppLanguage.SIMPLIFIED_CHINESE -> "${minutesRemaining}分后雨"
                    AppLanguage.TRADITIONAL_CHINESE -> "${minutesRemaining}分後雨"
                    AppLanguage.JAPANESE -> "あと${minutesRemaining}分"
                }
            }
        } else {
            when (language) {
                AppLanguage.ENGLISH -> "Steady rain"
                AppLanguage.SIMPLIFIED_CHINESE -> "持续降雨"
                AppLanguage.TRADITIONAL_CHINESE -> "持續降雨"
                AppLanguage.JAPANESE -> "降雨継続中"
            }
        }

        val title = when (status) {
            RainStatus.RAIN_APPROACHING -> when (language) {
                AppLanguage.ENGLISH -> "Rain starting shortly • $cityName"
                AppLanguage.SIMPLIFIED_CHINESE -> "即将开始下雨 • $cityName"
                AppLanguage.TRADITIONAL_CHINESE -> "即將開始下雨 • $cityName"
                AppLanguage.JAPANESE -> "まもなく雨が降り始めます • $cityName"
            }
            RainStatus.RAINING -> if (minutesRemaining != null) {
                when (language) {
                    AppLanguage.ENGLISH -> "Rain continuing until ~$targetTime • $cityName"
                    AppLanguage.SIMPLIFIED_CHINESE -> "预计下雨至 ${targetTime} 左右 • $cityName"
                    AppLanguage.TRADITIONAL_CHINESE -> "預計下雨至 ${targetTime} 左右 • $cityName"
                    AppLanguage.JAPANESE -> "雨は${targetTime}頃まで続く見込み • $cityName"
                }
            } else {
                when (language) {
                    AppLanguage.ENGLISH -> "Steady rain continues • $cityName"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降雨持续中 • $cityName"
                    AppLanguage.TRADITIONAL_CHINESE -> "降雨持續中 • $cityName"
                    AppLanguage.JAPANESE -> "雨は当面降り続く見込み • $cityName"
                }
            }
            else -> when (language) {
                AppLanguage.ENGLISH -> "Precipitation Forecast • $cityName"
                AppLanguage.SIMPLIFIED_CHINESE -> "降水预报 • $cityName"
                AppLanguage.TRADITIONAL_CHINESE -> "降水預報 • $cityName"
                AppLanguage.JAPANESE -> "降雨予測 • $cityName"
            }
        }

        val contentText = when (status) {
            RainStatus.RAIN_APPROACHING -> if (minutesRemaining != null) {
                when (language) {
                    AppLanguage.ENGLISH -> "Rain expected around $targetTime (in about ${minutesRemaining} min)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "预计 ${targetTime} 左右开始下雨（距现在约 ${minutesRemaining} 分钟）"
                    AppLanguage.TRADITIONAL_CHINESE -> "預計 ${targetTime} 左右開始下雨（距現在約 ${minutesRemaining} 分鐘）"
                    AppLanguage.JAPANESE -> "${targetTime}頃から雨の予想（あと約${minutesRemaining}分）"
                }
            } else nowcast.localizedSubText(language)
            RainStatus.RAINING -> if (minutesRemaining != null) {
                when (language) {
                    AppLanguage.ENGLISH -> "About ${minutesRemaining} min until rain stops"
                    AppLanguage.SIMPLIFIED_CHINESE -> "距离雨停大约还有 ${minutesRemaining} 分钟"
                    AppLanguage.TRADITIONAL_CHINESE -> "距離雨停大約還有 ${minutesRemaining} 分鐘"
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

        val badgeBitmap = createRainBadgeBitmap(appContext, status, minutesRemaining)
        val primaryColor = if (status == RainStatus.RAIN_APPROACHING) AndroidColor.parseColor("#0288D1") else AndroidColor.parseColor("#00ACC1")

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setLargeIcon(badgeBitmap)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(chipText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(chipText)
                    .bigText(bigText)
            )
            .setColor(primaryColor)
            .setColorized(false)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentIntent(radarPendingIntent)
            .setAutoCancel(false)
            .addAction(
                android.R.drawable.ic_dialog_map,
                when (language) {
                    AppLanguage.JAPANESE -> "雨雲レーダー"
                    AppLanguage.ENGLISH -> "Radar"
                    AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达"
                    AppLanguage.TRADITIONAL_CHINESE -> "降水雷達"
                },
                radarPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_info_details,
                when (language) {
                    AppLanguage.JAPANESE -> "アプリを開く"
                    AppLanguage.ENGLISH -> "Open App"
                    AppLanguage.SIMPLIFIED_CHINESE -> "打开应用"
                    AppLanguage.TRADITIONAL_CHINESE -> "開啟應用程式"
                },
                mainAppPendingIntent
            )

        // Galaxy One UI 7 Now Bar 互換用エクストラ設定
        builder.extras.putBoolean("com.samsung.android.app.notification.nowbar", true)
        builder.extras.putString("com.samsung.android.app.notification.nowbar.title", chipText)
        builder.extras.putString("com.samsung.android.app.notification.nowbar.text", cityName)
        builder.extras.putString("com.samsung.android.app.notification.subtext", chipText)
        builder.extras.putString("com.samsung.android.app.notification.title", chipText)

        if (targetTimestamp != null) {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetTimestamp)
        } else {
            builder.setUsesChronometer(false).setShowWhen(false)
        }

        // Android 16 (API 36+) Rich Ongoing Notifications
        if (Build.VERSION.SDK_INT >= 36) {
            builder.extras.putBoolean("android.requestPromotedOngoing", true)
            builder.extras.putCharSequence("android.shortCriticalText", chipText)
            try {
                val method = builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
                method.invoke(builder, chipText)
            } catch (_: Throwable) {
                try {
                    val method = builder.javaClass.getMethod("setShortCriticalText", String::class.java)
                    method.invoke(builder, chipText)
                } catch (_: Throwable) {}
            }
            try {
                val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                method.invoke(builder, true)
            } catch (_: Throwable) {}
        }

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= 36) {
            try {
                val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val canPromote = try {
                    nm.javaClass.getMethod("canPostPromotedNotifications").invoke(nm) as? Boolean ?: true
                } catch (_: Throwable) {
                    true
                }
                if (canPromote) {
                    val platformBuilder = Notification.Builder.recoverBuilder(appContext, notification)
                    try {
                        platformBuilder.javaClass.getMethod("setShortCriticalText", String::class.java).invoke(platformBuilder, chipText)
                    } catch (_: Throwable) {}
                    try {
                        platformBuilder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType).invoke(platformBuilder, true)
                    } catch (_: Throwable) {}
                    return platformBuilder.build()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to apply API 36 promoted notification builder: ${e.message}")
            }
        }

        return notification
    }

    fun buildSafeForegroundNotification(context: Context): Notification {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)
        val nowcast = currentActiveNowcast
        if (nowcast != null && nowcast.isLiveActive) {
            return buildRainLiveNotification(appContext, nowcast, currentActiveCityName.ifBlank { "現在地" })
        }
        val language = LocaleUtils.getSavedLanguage(appContext)
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(
                when (language) {
                    AppLanguage.ENGLISH -> "Monitoring precipitation"
                    AppLanguage.SIMPLIFIED_CHINESE -> "正在监测降水"
                    AppLanguage.TRADITIONAL_CHINESE -> "正在監測降水"
                    AppLanguage.JAPANESE -> "雨雲接近・降雨予測を監視中"
                }
            )
            .setContentText(
                when (language) {
                    AppLanguage.ENGLISH -> "Checking radar for your current location"
                    AppLanguage.SIMPLIFIED_CHINESE -> "正在获取当前位置的最新雷达数据"
                    AppLanguage.TRADITIONAL_CHINESE -> "正在取得目前位置的最新雷達資料"
                    AppLanguage.JAPANESE -> "現在地の最新気象レーダーを受信待機しています"
                }
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
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

        currentActiveNowcast = nowcast
        currentActiveCityName = cityName
        if (location != null) {
            currentActiveLocation = location
        }
        persistActiveState(appContext, nowcast, cityName)

        try {
            PrecipitationLiveService.startOrUpdate(appContext)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to start PrecipitationLiveService, falling back to direct notification: ${e.message}")
            try {
                val notification = buildRainLiveNotification(appContext, nowcast, cityName)
                notificationManager.notify(NOTIFICATION_ID, notification)
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
                AppLanguage.SIMPLIFIED_CHINESE -> "开始下雨了"
                AppLanguage.TRADITIONAL_CHINESE -> "開始下雨了"
                AppLanguage.JAPANESE -> "雨が降り始めました"
            }
        } else {
            when (language) {
                AppLanguage.ENGLISH -> "Rain has stopped"
                AppLanguage.SIMPLIFIED_CHINESE -> "雨已经停了"
                AppLanguage.TRADITIONAL_CHINESE -> "雨已經停了"
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

    fun startTickerIfNeeded(context: Context) {
        val appContext = context.applicationContext
        if (tickerJob?.isActive == true) return
        tickerJob = managerScope.launch {
            val locationProvider = CurrentLocationProvider(appContext)
            var tickCount = 0

            while (isActive) {
                delay(60 * 1000L)
                tickCount++

                val nowcast = currentActiveNowcast ?: break
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
                            currentActiveLocation = freshLoc
                            currentActiveNowcast = freshNowcast
                            currentActiveCityName = newCity.name
                            persistActiveState(appContext, freshNowcast, newCity.name)
                            if (canPostNotifications(appContext)) {
                                val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                nm.notify(NOTIFICATION_ID, buildRainLiveNotification(appContext, freshNowcast, newCity.name))
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
                        nm.notify(NOTIFICATION_ID, buildRainLiveNotification(appContext, updatedNowcast, currentActiveCityName))
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
