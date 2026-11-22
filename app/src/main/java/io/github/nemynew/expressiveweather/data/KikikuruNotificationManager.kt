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
import kotlinx.serialization.encodeToString

/**
 * 警戒レベル・キキクル Live Updates 通知マネージャー
 * 
 * Android 16+ Rich Ongoing Notifications および Galaxy Now Bar に完全対応した
 * 進行中アクティビティ（Live Notification）を構築・管理します。
 */
object KikikuruNotificationManager {
    private const val TAG = "KikikuruNotifManager"
    private const val PREFS_NAME = "expressive_weather_prefs"
    private const val KEY_ACTIVE_ALERT_JSON = "kikikuru_active_alert_json"
    private const val KEY_ACTIVE_CITY_NAME = "kikikuru_active_city_name"
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
            if (restored.alertLevel.level < 2) {
                persistActiveAlert(context, null, "")
                return
            }
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
            val name = "警戒レベル・キキクル Live Updates"
            val descriptionText = "市区町村で警戒レベル2以上の警報・注意報が発表された際のリアルタイム警戒通知（ライブ通知）"
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
        cityName: String = alertInfo.municipalityName
    ): Notification {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)
        val language = LocaleUtils.getSavedLanguage(appContext)

        val level = alertInfo.alertLevel
        val levelNum = level.level

        val primaryDetail = alertInfo.targetDisasterPrimary?.let { alertInfo.getDetailFor(it) }
            ?: alertInfo.allDisasterDetails.maxByOrNull { it.alertLevel.level }
            ?: alertInfo.getDetailFor(DisasterType.HEAVY_RAIN)

        val primaryDisaster = when (language) {
            AppLanguage.ENGLISH -> primaryDetail.disasterType.shortLabelEn
            AppLanguage.SIMPLIFIED_CHINESE -> primaryDetail.disasterType.shortLabelZhHans
            AppLanguage.TRADITIONAL_CHINESE -> primaryDetail.disasterType.shortLabelZhHant
            AppLanguage.JAPANESE -> primaryDetail.disasterType.shortLabelJa
        }
        val chipText = when (language) {
            AppLanguage.ENGLISH -> "$primaryDisaster Lv.$levelNum"
            AppLanguage.SIMPLIFIED_CHINESE -> "$primaryDisaster ${levelNum}级"
            AppLanguage.TRADITIONAL_CHINESE -> "$primaryDisaster ${levelNum}級"
            AppLanguage.JAPANESE -> "$primaryDisaster レベル$levelNum"
        }

        val targetCity = alertInfo.municipalityName.ifBlank { cityName }
        val title = "$chipText • $targetCity"
        val tickerText = "$chipText $targetCity"
        val evacBadge = primaryDetail.getEvacuationInfo(language)?.let { "【$it】" } ?: ""
        val levelTitle = level.getTitle(language)
        val summaryText = when (language) {
            AppLanguage.ENGLISH -> "Alert Level $levelNum [$levelTitle] $evacBadge"
            AppLanguage.SIMPLIFIED_CHINESE -> "警戒级别 $levelNum 【$levelTitle】 $evacBadge"
            AppLanguage.TRADITIONAL_CHINESE -> "警戒級別 $levelNum 【$levelTitle】 $evacBadge"
            AppLanguage.JAPANESE -> "警戒レベル$levelNum 【$levelTitle】 $evacBadge"
        }.trim()

        val disasterBadgeText = if (alertInfo.disasterFlags.activeTypes.isNotEmpty()) {
            val labels = alertInfo.disasterFlags.activeTypes.joinToString("・") {
                when (language) {
                    AppLanguage.ENGLISH -> it.labelEn
                    AppLanguage.SIMPLIFIED_CHINESE -> it.labelZhHans
                    AppLanguage.TRADITIONAL_CHINESE -> it.labelZhHant
                    AppLanguage.JAPANESE -> it.labelJa
                }
            }
            when (language) {
                AppLanguage.ENGLISH -> "[Hazards: $labels]"
                AppLanguage.SIMPLIFIED_CHINESE -> "【警戒对象: $labels】"
                AppLanguage.TRADITIONAL_CHINESE -> "【警戒對象: $labels】"
                AppLanguage.JAPANESE -> "【警戒対象: $labels】"
            }
        } else ""

        val warningsText = if (alertInfo.activeWarnings.isNotEmpty()) {
            alertInfo.activeWarnings.joinToString("、") { it.name } + when (language) {
                AppLanguage.ENGLISH -> " active"
                AppLanguage.SIMPLIFIED_CHINESE -> " 生效中"
                AppLanguage.TRADITIONAL_CHINESE -> " 生效中"
                AppLanguage.JAPANESE -> " 発表中"
            }
        } else {
            when (language) {
                AppLanguage.ENGLISH -> "Weather advisory active"
                AppLanguage.SIMPLIFIED_CHINESE -> "注意报生效中"
                AppLanguage.TRADITIONAL_CHINESE -> "注意報生效中"
                AppLanguage.JAPANESE -> "注意報発表中"
            }
        }

        val actionText = primaryDetail.getActionAdvice(language)
        val targetLayerId = primaryDetail.disasterType.radarLayerId

        val clickIntent = Intent(appContext, RadarDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("initial_layer_id", targetLayerId)
            putExtra("city_name", targetCity)
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

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setLargeIcon(badgeBitmap)
            .setContentTitle(title)
            .setContentText(actionText)
            .setSubText(chipText)
            .setTicker(tickerText)
            .setProgress(5, levelNum, false)
            .setUsesChronometer(false)
            .setShowWhen(false)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(summaryText)
                    .bigText(bigText)
            )
            .setColor(notificationColor)
            .setColorized(false)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
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
                    AppLanguage.SIMPLIFIED_CHINESE -> "打开应用"
                    AppLanguage.TRADITIONAL_CHINESE -> "開啟應用程式"
                    AppLanguage.JAPANESE -> "アプリを開く"
                },
                mainAppPendingIntent
            )

        // Galaxy One UI 7 Now Bar 互換用エクストラ設定
        builder.extras.putBoolean("com.samsung.android.app.notification.nowbar", true)
        builder.extras.putString("com.samsung.android.app.notification.nowbar.title", chipText)
        builder.extras.putString("com.samsung.android.app.notification.nowbar.text", targetCity)
        builder.extras.putString("com.samsung.android.app.notification.subtext", chipText)
        builder.extras.putString("com.samsung.android.app.notification.title", chipText)

        if (Build.VERSION.SDK_INT >= 36) {
            builder.extras.putBoolean("android.requestPromotedOngoing", true)
            builder.extras.putCharSequence("android.shortCriticalText", chipText)
            try {
                builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java).invoke(builder, chipText)
            } catch (_: Throwable) {
                try {
                    builder.javaClass.getMethod("setShortCriticalText", String::class.java).invoke(builder, chipText)
                } catch (_: Throwable) {}
            }
            try {
                builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType).invoke(builder, true)
            } catch (_: Throwable) {}
        }

        val notification = builder.build()

        // Android 16+ (API 36+) Native Live Updates (Status Bar Chip) & ProgressStyle 適用
        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
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
                    } catch (e: Throwable) {
                        Log.w(TAG, "platformBuilder.setShortCriticalText invocation failed: ${e.message}")
                    }
                    try {
                        platformBuilder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType).invoke(platformBuilder, true)
                    } catch (_: Throwable) {}

                    if (!isSamsung) {
                        try {
                            val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
                            val pointClass = Class.forName("android.app.Notification\$ProgressStyle\$Point")
                            val segmentClass = Class.forName("android.app.Notification\$ProgressStyle\$Segment")

                            val progressStyle = progressStyleClass.getDeclaredConstructor().newInstance()
                            progressStyleClass.getMethod("setProgress", Int::class.javaPrimitiveType).invoke(progressStyle, levelNum)

                            val levelColors = intArrayOf(
                                AlertLevel.LEVEL_1.colorHex.toInt(),
                                AlertLevel.LEVEL_2.colorHex.toInt(),
                                AlertLevel.LEVEL_3.colorHex.toInt(),
                                AlertLevel.LEVEL_4.colorHex.toInt(),
                                AlertLevel.LEVEL_5.colorHex.toInt()
                            )
                            val addSegmentMethod = progressStyleClass.getMethod("addProgressSegment", segmentClass)
                            for (i in 0 until 5) {
                                val segment = segmentClass.getConstructor(Int::class.javaPrimitiveType).newInstance(1)
                                try {
                                    segmentClass.getMethod("setColor", Int::class.javaPrimitiveType).invoke(segment, levelColors[i])
                                } catch (_: Throwable) {}
                                addSegmentMethod.invoke(progressStyle, segment)
                            }

                            val addPointMethod = progressStyleClass.getMethod("addProgressPoint", pointClass)
                            for (i in 1..4) {
                                try {
                                    val point = pointClass.getConstructor(Int::class.javaPrimitiveType).newInstance(i)
                                    try {
                                        pointClass.getMethod("setColor", Int::class.javaPrimitiveType).invoke(point, levelColors[(i - 1).coerceIn(0, 4)])
                                    } catch (_: Throwable) {}
                                    addPointMethod.invoke(progressStyle, point)
                                } catch (_: Throwable) {}
                            }

                            platformBuilder.javaClass.getMethod("setStyle", Notification.Style::class.java).invoke(platformBuilder, progressStyle)
                        } catch (e: Throwable) {
                            Log.w(TAG, "ProgressStyle configuration: ${e.message}")
                        }
                    }

                    return platformBuilder.build()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to apply platformBuilder modifications: ${e.message}")
            }
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
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("気象警戒情報を監視中")
            .setContentText("気象庁キキクル情報を待機しています")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
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

        currentActiveAlertInfo = alertInfo
        currentActiveCityName = cityName
        persistActiveAlert(appContext, alertInfo, cityName)

        // フォアグラウンドサービスを起動して startForeground と結合 (失敗時は内部で notify フォールバック)
        KikikuruLiveAlertService.startOrUpdate(appContext)

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
        persistActiveAlert(appContext, null, "")
        KikikuruLiveAlertService.stop(appContext)
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
