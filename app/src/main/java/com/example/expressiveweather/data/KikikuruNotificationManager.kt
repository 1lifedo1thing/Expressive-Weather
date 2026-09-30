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
import com.example.expressiveweather.MainActivity
import com.example.expressiveweather.ui.screens.RadarDetailActivity

/**
 * 警戒レベル・キキクル Live Updates 通知マネージャー
 * 
 * Android 16+ Rich Ongoing Notifications および Galaxy Now Bar に完全対応した
 * 進行中アクティビティ（Live Notification）を構築・管理します。
 */
object KikikuruNotificationManager {
    private const val TAG = "KikikuruNotifManager"
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
        createNotificationChannel(context)

        val level = alertInfo.alertLevel
        val levelNum = level.level

        val primaryDetail = alertInfo.targetDisasterPrimary?.let { alertInfo.getDetailFor(it) }
            ?: alertInfo.allDisasterDetails.maxByOrNull { it.alertLevel.level }
            ?: alertInfo.getDetailFor(DisasterType.HEAVY_RAIN)

        val primaryDisaster = primaryDetail.disasterType.shortLabelJa
        val chipText = "$primaryDisaster レベル$levelNum"

        val targetCity = alertInfo.municipalityName.ifBlank { cityName }
        // Samsung Now Bar の狭いピル領域でも「千代田区」などの地名だけにならず「大雨 レベル4」が最初に表示されるよう先頭に配置
        val title = "$chipText • $targetCity"
        val tickerText = "$chipText $targetCity"
        val evacBadge = primaryDetail.getEvacuationInfo(AppLanguage.JAPANESE)?.let { "【$it】" } ?: ""
        val summaryText = "警戒レベル$levelNum 【${level.title}】 $evacBadge".trim()

        // 対象災害種別バッジ（四角記号文字は一切使用しない）
        val disasterBadgeText = if (alertInfo.disasterFlags.activeTypes.isNotEmpty()) {
            "【警戒対象: " + alertInfo.disasterFlags.activeTypes.joinToString("・") { it.labelJa } + "】"
        } else ""

        // 発表警報名一覧
        val warningsText = if (alertInfo.activeWarnings.isNotEmpty()) {
            alertInfo.activeWarnings.joinToString("、") { it.name } + " 発表中"
        } else {
            "注意報発表中"
        }

        val actionText = primaryDetail.getActionAdvice(AppLanguage.JAPANESE)
        val targetLayerId = primaryDetail.disasterType.radarLayerId

        val clickIntent = Intent(context, RadarDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("initial_layer_id", targetLayerId)
            putExtra("city_name", targetCity)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            clickIntent,
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

        // 展開カードの本文組み立て（■や□などの記号文字は一切含めない）
        val bigText = buildString {
            appendLine("【警戒レベル$levelNum ${level.title}】 $targetCity $evacBadge".trim())
            if (disasterBadgeText.isNotBlank()) {
                appendLine(disasterBadgeText)
            }
            appendLine()
            appendLine("【行動指針】")
            appendLine(actionText)
            appendLine()
            append(warningsText)
        }

        val notificationColor = level.colorHex.toInt()
        val badgeBitmap = createLevelBadgeBitmap(context, level)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setLargeIcon(badgeBitmap)
            .setContentTitle(title)
            .setContentText(actionText)
            .setSubText(chipText) // ステータスバーピル / Galaxy Now Bar 専用テキスト（例: "洪水 レベル4"）
            .setTicker(tickerText)
            // 進行中アクティビティ・Galaxy Now Bar / Android 16 のトリガーとなるプログレスバー (max: 5, progress: levelNum)
            .setProgress(5, levelNum, false)
            // タイマー表示（クロノメーター）は抑止し、ステータスバーピルに "洪水 レベル4" を明瞭表示
            .setUsesChronometer(false)
            .setShowWhen(false)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(summaryText)
                    .bigText(bigText)
            )
            .setColor(notificationColor)
            .setColorized(false) // Android 16 の Live Updates プロモーション要件: setColorized(true) を避ける
            .setOngoing(true) // スワイプ消去不可・進行中フラグ
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Android 12+ 10秒表示遅延を抑止
            .setCategory(NotificationCompat.CATEGORY_STATUS) // 進行中ステータスカテゴリ (Live Activity)
            .setPriority(NotificationCompat.PRIORITY_MAX) // 最優先表示 (Heads-Up & Status Bar Chip)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // ロック画面常時表示
            .setOnlyAlertOnce(true) // 定期更新時の連続鳴動を抑制しつつピルは更新
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            // アクションボタン
            .addAction(
                android.R.drawable.ic_dialog_map,
                primaryDetail.getButtonText(AppLanguage.JAPANESE),
                pendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_info_details,
                "アプリを開く",
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
        try {
            val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
            method.invoke(builder, true)
        } catch (_: Throwable) {
            // AndroidX / OSバージョン差異吸収
        }

        // Galaxy One UI 7 Now Bar 互換用エクストラ設定
        builder.extras.putBoolean("com.samsung.android.app.notification.nowbar", true)
        builder.extras.putString("com.samsung.android.app.notification.nowbar.title", chipText)
        builder.extras.putString("com.samsung.android.app.notification.nowbar.text", targetCity)
        builder.extras.putString("com.samsung.android.app.notification.subtext", chipText)
        builder.extras.putString("com.samsung.android.app.notification.title", chipText)

        val notification = builder.build()

        // Android 16+ (API 35/36/37) Native Live Updates (Status Bar Chip) & ProgressStyle 適用
        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        if (Build.VERSION.SDK_INT >= 35) {
            try {
                val platformBuilder = Notification.Builder.recoverBuilder(context, notification)
                try {
                    val setShortCriticalTextMethod = platformBuilder.javaClass.getMethod("setShortCriticalText", String::class.java)
                    setShortCriticalTextMethod.invoke(platformBuilder, chipText)
                } catch (e: Throwable) {
                    Log.w(TAG, "platformBuilder.setShortCriticalText invocation failed: ${e.message}")
                }
                try {
                    val promotedMethod = platformBuilder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    promotedMethod.invoke(platformBuilder, true)
                } catch (_: Throwable) {}

                // Samsung One UI SystemUI は ProgressStyle の android.progressSegments が ArrayList である場合に
                // ClassCastException を起こすため、Samsung端末では ProgressStyle 注入を行わず、
                // 標準の setProgress(5, levelNum, false) と Now Bar エクストラで安定動作させる
                if (!isSamsung) {
                    try {
                        val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
                        val pointClass = Class.forName("android.app.Notification\$ProgressStyle\$Point")
                        val segmentClass = Class.forName("android.app.Notification\$ProgressStyle\$Segment")

                        val progressStyle = progressStyleClass.getDeclaredConstructor().newInstance()

                        // 現在の進捗を設定 (1〜5)
                        val setProgressMethod = progressStyleClass.getMethod("setProgress", Int::class.javaPrimitiveType)
                        setProgressMethod.invoke(progressStyle, levelNum)

                        // 5段階のレベルセグメント（各長さ1）を追加
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
                                val setColorMethod = segmentClass.getMethod("setColor", Int::class.javaPrimitiveType)
                                setColorMethod.invoke(segment, levelColors[i])
                            } catch (_: Throwable) {}
                            addSegmentMethod.invoke(progressStyle, segment)
                        }

                        // Android 16 MAX_POINTS は 4 であるため、境界ポイント (1..4) を追加
                        val addPointMethod = progressStyleClass.getMethod("addProgressPoint", pointClass)
                        for (i in 1..4) {
                            try {
                                val point = pointClass.getConstructor(Int::class.javaPrimitiveType).newInstance(i)
                                try {
                                    val setColorMethod = pointClass.getMethod("setColor", Int::class.javaPrimitiveType)
                                    setColorMethod.invoke(point, levelColors[(i - 1).coerceIn(0, 4)])
                                } catch (_: Throwable) {}
                                addPointMethod.invoke(progressStyle, point)
                            } catch (_: Throwable) {}
                        }

                        val setStyleMethod = platformBuilder.javaClass.getMethod("setStyle", Notification.Style::class.java)
                        setStyleMethod.invoke(platformBuilder, progressStyle)
                        Log.d(TAG, "Applied Android 16 ProgressStyle with 5 level segments and 4 boundary points")
                    } catch (e: Throwable) {
                        Log.w(TAG, "ProgressStyle configuration: ${e.message}")
                    }
                }

                val finalNotif = platformBuilder.build()
                finalNotif.flags = finalNotif.flags or 262144 // Notification.FLAG_PROMOTED_ONGOING
                return finalNotif
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
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 通知マスター設定チェック
        if (!WeatherRepository.isLiveAlertNotificationEnabled(context)) {
            currentActiveAlertInfo = null
            currentActiveCityName = ""
            isTestModeActive = false
            KikikuruLiveAlertService.stop(context)
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d(TAG, "Live alert notification is disabled by user.")
            return
        }

        // ユーザー設定の重要度閾値を取得 (注意報以上: Lv.2, 警報以上: Lv.3, 特別警報のみ: Lv.4, オフ: OFF)
        val threshold = WeatherRepository.getAlertNotificationThreshold(context)
        if (threshold == AlertNotificationThreshold.OFF) {
            currentActiveAlertInfo = null
            currentActiveCityName = ""
            isTestModeActive = false
            KikikuruLiveAlertService.stop(context)
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d(TAG, "Alert notification threshold is OFF.")
            return
        }

        // テスト通知稼働中にバックグラウンドの平常データ(Lv < 2)が届いてもテスト表示を即時消去しないよう保護
        if (isTestModeActive && alertInfo.municipalityCode != "1310100" && alertInfo.alertLevel.level < 2) {
            Log.d(TAG, "Active test notification preserved; ignoring background Lv.${alertInfo.alertLevel.level} update.")
            return
        }

        // 終了条件: レベル1または平常時へ戻った、もしくはユーザー設定の閾値未満の場合はサービス停止と通知破棄
        if (alertInfo.alertLevel.level < 2 || (!isTestModeActive && alertInfo.alertLevel.level < threshold.minLevel)) {
            currentActiveAlertInfo = null
            currentActiveCityName = ""
            isTestModeActive = false
            KikikuruLiveAlertService.stop(context)
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d(TAG, "Alert level (${alertInfo.alertLevel.level}) below threshold (${threshold.minLevel}). Stopped live service.")
            return
        }

        // POST_NOTIFICATIONS 権限チェック (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skip notification.")
                return
            }
        }

        currentActiveAlertInfo = alertInfo
        currentActiveCityName = cityName

        // フォアグラウンドサービスを起動して startForeground と結合
        KikikuruLiveAlertService.startOrUpdate(context)

        // サービスが既に稼働中の場合、最新の通知内容を即時同期反映
        try {
            val notification = buildLiveNotification(context, alertInfo, cityName)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "NotificationManager.notify fallback error: ${e.message}")
        }

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
        val dummyAlert = MunicipalityAlertInfo(
            municipalityCode = "1310100",
            municipalityName = "千代田区 (テスト)",
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
        isTestModeActive = false
        currentActiveAlertInfo = null
        currentActiveCityName = ""
        KikikuruLiveAlertService.stop(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
