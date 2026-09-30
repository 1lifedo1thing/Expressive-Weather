package com.example.expressiveweather.data

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
import com.example.expressiveweather.MainActivity
import com.example.expressiveweather.ui.screens.RadarDetailActivity
import com.example.expressiveweather.ui.screens.SettingsDataHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 雨雲の接近 & 止むまでのカウントダウン Live Updates 通知マネージャー
 * 
 * Android 16+ Rich Ongoing Notifications および Samsung Galaxy Now Bar に完全対応。
 * ステータスバーピルに「雨雲接近 あと20分」「雨止むまで あと35分」を掲示し、
 * ロック画面や通知シェードではネイティブクロノメーターによる秒単位カウントダウンを提供します。
 */
object PrecipitationLiveNotificationManager {

    private const val TAG = "RainLiveNotifManager"
    const val CHANNEL_ID = "rain_live_countdown"
    const val NOTIFICATION_ID = 9120

    @Volatile
    var currentActiveNowcast: PrecipitationNowcast? = null
        private set

    @Volatile
    var currentActiveCityName: String = ""
        private set

    @Volatile
    var isTestActive: Boolean = false
        private set

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null

    /**
     * 通知チャンネルの作成（Android 8.0+ 必須）
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "雨雲接近・降雨カウントダウン Live Updates"
            val descriptionText = "雨雲接近時の降り始め予測および雨が止むまでの残り時間のリアルタイム・カウントダウン通知"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = AndroidColor.CYAN
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Android 16 Rich Ongoing Notifications & Galaxy Now Bar 準拠の
     * 雨雲カウントダウン Live Notification をビルド
     */
    fun buildRainLiveNotification(
        context: Context,
        nowcast: PrecipitationNowcast,
        cityName: String
    ): Notification {
        createNotificationChannel(context)

        val language = SettingsDataHolder.language
        val status = nowcast.status
        val minutesRemaining = nowcast.minutesRemaining ?: 15
        val targetTime = nowcast.localizedTargetTimeLabel(language) ?: when (language) {
            AppLanguage.ENGLISH -> "shortly"
            AppLanguage.SIMPLIFIED_CHINESE -> "即将"
            AppLanguage.TRADITIONAL_CHINESE -> "即將"
            AppLanguage.JAPANESE -> "まもなく"
        }
        val targetTimestamp = nowcast.targetTimestampMillis ?: (System.currentTimeMillis() + minutesRemaining * 60 * 1000L)

        // ステータスバーピル / Galaxy Now Bar 専用の短縮クリティカルテキスト (10文字以内推奨)
        val chipText = when (status) {
            RainStatus.RAIN_APPROACHING -> when (language) {
                AppLanguage.ENGLISH -> "Rain in ${minutesRemaining}m"
                AppLanguage.SIMPLIFIED_CHINESE -> "降雨还有${minutesRemaining}分"
                AppLanguage.TRADITIONAL_CHINESE -> "降雨還有${minutesRemaining}分"
                AppLanguage.JAPANESE -> "雨雲接近 あと${minutesRemaining}分"
            }
            RainStatus.RAINING -> when (language) {
                AppLanguage.ENGLISH -> "Stops in ${minutesRemaining}m"
                AppLanguage.SIMPLIFIED_CHINESE -> "距雨停${minutesRemaining}分"
                AppLanguage.TRADITIONAL_CHINESE -> "距雨停${minutesRemaining}分"
                AppLanguage.JAPANESE -> "雨止むまで あと${minutesRemaining}分"
            }
            else -> when (language) {
                AppLanguage.ENGLISH -> "Rain in ${minutesRemaining}m"
                AppLanguage.SIMPLIFIED_CHINESE -> "降雨还有${minutesRemaining}分"
                AppLanguage.TRADITIONAL_CHINESE -> "降雨還有${minutesRemaining}分"
                AppLanguage.JAPANESE -> "雨予報 あと${minutesRemaining}分"
            }
        }

        // 通知展開タイトル・本文
        val title = when (status) {
            RainStatus.RAIN_APPROACHING -> when (language) {
                AppLanguage.ENGLISH -> "Rain starting shortly • $cityName"
                AppLanguage.SIMPLIFIED_CHINESE -> "即将开始下雨 • $cityName"
                AppLanguage.TRADITIONAL_CHINESE -> "即將開始下雨 • $cityName"
                AppLanguage.JAPANESE -> "まもなく雨が降り始めます • $cityName"
            }
            RainStatus.RAINING -> when (language) {
                AppLanguage.ENGLISH -> "Rain continuing until ~$targetTime • $cityName"
                AppLanguage.SIMPLIFIED_CHINESE -> "预计下雨至 ${targetTime} 左右 • $cityName"
                AppLanguage.TRADITIONAL_CHINESE -> "預計下雨至 ${targetTime} 左右 • $cityName"
                AppLanguage.JAPANESE -> "雨は${targetTime}頃まで続く見込み • $cityName"
            }
            else -> when (language) {
                AppLanguage.ENGLISH -> "Precipitation Forecast • $cityName"
                AppLanguage.SIMPLIFIED_CHINESE -> "降水预报 • $cityName"
                AppLanguage.TRADITIONAL_CHINESE -> "降水預報 • $cityName"
                AppLanguage.JAPANESE -> "降雨予測 • $cityName"
            }
        }

        val contentText = when (status) {
            RainStatus.RAIN_APPROACHING -> when (language) {
                AppLanguage.ENGLISH -> "Rain expected around $targetTime (in about ${minutesRemaining} min)"
                AppLanguage.SIMPLIFIED_CHINESE -> "预计 ${targetTime} 左右开始下雨（距现在约 ${minutesRemaining} 分钟）"
                AppLanguage.TRADITIONAL_CHINESE -> "預計 ${targetTime} 左右開始下雨（距現在約 ${minutesRemaining} 分鐘）"
                AppLanguage.JAPANESE -> "${targetTime}頃から雨の予想（あと約${minutesRemaining}分）"
            }
            RainStatus.RAINING -> when (language) {
                AppLanguage.ENGLISH -> "About ${minutesRemaining} min until rain stops"
                AppLanguage.SIMPLIFIED_CHINESE -> "距离雨停大约还有 ${minutesRemaining} 分钟"
                AppLanguage.TRADITIONAL_CHINESE -> "距離雨停大約還有 ${minutesRemaining} 分鐘"
                AppLanguage.JAPANESE -> "雨が止むまであと約${minutesRemaining}分です"
            }
            else -> nowcast.localizedSubText(language)
        }

        val bigText = buildString {
            if (status == RainStatus.RAIN_APPROACHING) {
                appendLine("【雨雲接近中】あと約${minutesRemaining}分で雨が降り始める見込みです (${targetTime}頃〜)")
                if (nowcast.peakIntensityMm > 0.0) {
                    appendLine("予想降水量: 約${String.format(java.util.Locale.US, "%.1f", nowcast.peakIntensityMm)}mm/h")
                }
                appendLine("降水確率: ${nowcast.maxProbability}%")
                appendLine()
                append("お出かけの際は折りたたみ傘をお持ちください。")
            } else {
                appendLine("【降雨中】雨は${targetTime}頃まで降り続く見込みです")
                appendLine("雨が止むまであと約${minutesRemaining}分です。")
                if (nowcast.currentIntensityMm > 0.0) {
                    appendLine("現在の雨脚: 約${String.format(java.util.Locale.US, "%.1f", nowcast.currentIntensityMm)}mm/h")
                }
                appendLine()
                append("雨雲が通過した後は次第に曇りへと回復する予想です。")
            }
        }

        // タップ時に雨雲レーダー画面へ遷移するディープリンク
        val radarIntent = Intent(context, RadarDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("initial_layer_id", "hrpns") // 雨雲高解像度レーダー
            putExtra("city_name", cityName)
        }
        val radarPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            radarIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // アプリ本体起動インテント
        val mainAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainAppPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID + 1,
            mainAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val badgeBitmap = createRainBadgeBitmap(context, status, minutesRemaining)
        val primaryColor = if (status == RainStatus.RAIN_APPROACHING) AndroidColor.parseColor("#0288D1") else AndroidColor.parseColor("#00ACC1")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setLargeIcon(badgeBitmap)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(chipText) // ステータスバーピル・Now Bar用
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(chipText)
                    .bigText(bigText)
            )
            .setColor(primaryColor)
            .setColorized(false)
            .setOngoing(true) // 進行中ライブアクティビティ
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentIntent(radarPendingIntent)
            .setAutoCancel(false)
            // ネイティブクロノメーターによる秒単位カウントダウン
            .setUsesChronometer(true)
            .setWhen(targetTimestamp)
            // アクションボタン
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

        // Android 16 (API 36+) Rich Ongoing Notifications (Live Updates) プロモーション要求 & チップ文字列設定
        builder.extras.putBoolean("android.requestPromotedOngoing", true)
        builder.extras.putCharSequence("android.shortCriticalText", chipText)
        builder.extras.putString("android.shortCriticalText", chipText)
        builder.extras.putString("android.extra.SHORT_CRITICAL_TEXT", chipText)
        builder.extras.putCharSequence("android.subText", chipText)
        try {
            val method = builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
            method.invoke(builder, chipText)
        } catch (_: Throwable) {
            try {
                val method = builder.javaClass.getMethod("setShortCriticalText", String::class.java)
                method.invoke(builder, chipText)
            } catch (_: Throwable) {}
        }

        return builder.build()
    }

    /**
     * 降雨予測に基づく Live Updates 通知の更新または終了
     */
    fun updateRainLiveNotification(
        context: Context,
        nowcast: PrecipitationNowcast,
        cityName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // テスト稼働中に平常データが届いてもテスト表示を即消去しない
        if (isTestActive && !nowcast.isLiveActive) {
            return
        }

        // 雨雲接近中または降雨中でない場合は通知を終了
        val isActiveState = nowcast.status == RainStatus.RAIN_APPROACHING || nowcast.status == RainStatus.RAINING
        if (!isActiveState || (nowcast.minutesRemaining ?: 0) <= 0) {
            cancel(context)
            return
        }

        // 通知権限チェック (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        currentActiveNowcast = nowcast
        currentActiveCityName = cityName

        try {
            val notification = buildRainLiveNotification(context, nowcast, cityName)
            notificationManager.notify(NOTIFICATION_ID, notification)
            startTickerIfNeeded(context)
            Log.d(TAG, "Dispatched Rain Live Countdown Notification: ${nowcast.headline} (${nowcast.minutesRemaining}m)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch rain live notification: ${e.message}", e)
        }
    }

    /**
     * 1分毎に残り分数を更新するリアルタイムタイカー
     */
    private fun startTickerIfNeeded(context: Context) {
        if (tickerJob?.isActive == true) return
        tickerJob = managerScope.launch {
            while (isActive) {
                delay(60 * 1000L) // 1分間隔
                val nowcast = currentActiveNowcast ?: break
                val targetMillis = nowcast.targetTimestampMillis ?: break
                val now = System.currentTimeMillis()
                val remainingMin = ((targetMillis - now) / 60000L).toInt()

                if (remainingMin <= 0) {
                    // カウントダウン完了
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val finishChip = if (nowcast.status == RainStatus.RAIN_APPROACHING) "雨が降り始めました" else "雨が止みました"
                    val finishTitle = if (nowcast.status == RainStatus.RAIN_APPROACHING) "雨が降り始めました • $currentActiveCityName" else "雨が止みました • $currentActiveCityName"
                    val finishNotif = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setContentTitle(finishTitle)
                        .setContentText(finishChip)
                        .setSubText(finishChip)
                        .setAutoCancel(true)
                        .setOngoing(false)
                        .build()
                    nm.notify(NOTIFICATION_ID, finishNotif)
                    delay(5000L)
                    cancel(context)
                    break
                } else {
                    val updatedNowcast = nowcast.copy(minutesRemaining = remainingMin)
                    currentActiveNowcast = updatedNowcast
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, buildRainLiveNotification(context, updatedNowcast, currentActiveCityName))
                }
            }
        }
    }

    /**
     * テスト通知の即時送信
     */
    fun sendTestNotification(context: Context, status: RainStatus = RainStatus.RAIN_APPROACHING) {
        isTestActive = true
        val mockNowcast = PrecipitationAnalyzer.createMock(status)
        updateRainLiveNotification(context, mockNowcast, "千代田区 (テスト)")
    }

    /**
     * 通知の消去およびタイカー停止
     */
    fun cancel(context: Context) {
        isTestActive = false
        currentActiveNowcast = null
        currentActiveCityName = ""
        tickerJob?.cancel()
        tickerJob = null
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    /**
     * 通知用ダイナミックバッジ画像（残り分数入り）の生成
     */
    private fun createRainBadgeBitmap(context: Context, status: RainStatus, minutes: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (56 * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (status == RainStatus.RAIN_APPROACHING) AndroidColor.parseColor("#0288D1") else AndroidColor.parseColor("#00838F")
            this.style = Paint.Style.FILL
        }

        // 角丸スクワークル
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, size * 0.35f, size * 0.35f, bgPaint)

        // 文字描画（残り分数）
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

        val yPos = size * 0.55f
        canvas.drawText("$minutes", size / 2f, yPos, textPaint)
        canvas.drawText("min", size / 2f, size * 0.78f, unitPaint)

        return bitmap
    }
}
