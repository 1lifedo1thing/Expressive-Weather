package io.github.nemynew.expressiveweather.ui.radar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.JmaTyphoonItem
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

/**
 * Material 3 Expressive 準拠の台風マップオーバレイ描画ヘルパー
 * 全ての描画要素（経路・強風域・暴風域・予報円・マーカー）に Compose の Material 3 ColorScheme を適用
 */
object RadarTyphoonOverlay {

    private val dotMarkerCache = object : android.util.LruCache<String, BitmapDescriptor>(16) {}

    fun clearCache() {
        synchronized(dotMarkerCache) {
            dotMarkerCache.evictAll()
        }
    }

    /**
     * GoogleMap上に台風の過去経路・強風域・暴風域・進路予想・予報円・M3マーカーを描画
     */
    fun renderTyphoons(
        map: GoogleMap,
        context: Context,
        typhoons: List<JmaTyphoonItem>,
        language: AppLanguage,
        colorScheme: ColorScheme,
        isDark: Boolean,
        activeObjects: MutableList<Any>,
        selectedTyphoonId: String? = null
    ) {
        // 既存の台風描画オブジェクトを全削除
        clearTyphoons(activeObjects)

        if (typhoons.isEmpty()) return

        for (typhoon in typhoons) {
            val analysis = typhoon.analysis

            // 1. 過去の経路ライン (過去 -> 現在中心: M3 Primary)
            if (typhoon.pastTrack.size >= 2) {
                val pastTrackColor = colorScheme.primary.copy(alpha = if (isDark) 0.90f else 0.85f).toArgb()
                val polyline = map.addPolyline(
                    PolylineOptions()
                        .addAll(typhoon.pastTrack)
                        .color(pastTrackColor)
                        .width(6.5f)
                        .geodesic(true)
                        .zIndex(60f)
                )
                activeObjects.add(polyline)
            }

            // 2. 強風域 (風速15m/s以上: M3 Tertiary 半透明塗り)
            val galeRadius = typhoon.galeWarningRadiusMeters
            if (galeRadius != null && galeRadius > 0.0) {
                val galeFill = colorScheme.tertiary.copy(alpha = if (isDark) 0.20f else 0.16f).toArgb()
                val galeStroke = colorScheme.tertiary.copy(alpha = if (isDark) 0.85f else 0.80f).toArgb()
                val galeCircle = map.addCircle(
                    CircleOptions()
                        .center(LatLng(analysis.latitude, analysis.longitude))
                        .radius(galeRadius)
                        .fillColor(galeFill)
                        .strokeColor(galeStroke)
                        .strokeWidth(3f)
                        .clickable(true)
                        .zIndex(61f)
                )
                galeCircle.tag = typhoon
                activeObjects.add(galeCircle)
            }

            // 3. 暴風域 (風速25m/s以上: M3 Error 半透明塗り)
            val stormRadius = typhoon.stormWarningRadiusMeters
            if (stormRadius != null && stormRadius > 0.0) {
                val stormFill = colorScheme.error.copy(alpha = if (isDark) 0.25f else 0.20f).toArgb()
                val stormStroke = colorScheme.error.copy(alpha = if (isDark) 0.90f else 0.85f).toArgb()
                val stormCircle = map.addCircle(
                    CircleOptions()
                        .center(LatLng(analysis.latitude, analysis.longitude))
                        .radius(stormRadius)
                        .fillColor(stormFill)
                        .strokeColor(stormStroke)
                        .strokeWidth(3.5f)
                        .clickable(true)
                        .zIndex(62f)
                )
                stormCircle.tag = typhoon
                activeObjects.add(stormCircle)
            }

            // 4. 進路予想線 (現在位置 -> 予報円の中心を結ぶ破線: M3 Outline)
            if (typhoon.probabilityCircles.isNotEmpty()) {
                val forecastPath = listOf(LatLng(analysis.latitude, analysis.longitude)) +
                        typhoon.probabilityCircles.map { it.center }
                val forecastLineColor = colorScheme.outline.copy(alpha = if (isDark) 0.80f else 0.65f).toArgb()
                val forecastLine = map.addPolyline(
                    PolylineOptions()
                        .addAll(forecastPath)
                        .color(forecastLineColor)
                        .width(4f)
                        .pattern(listOf(Dash(20f), Gap(12f)))
                        .geodesic(true)
                        .zIndex(63f)
                )
                activeObjects.add(forecastLine)
            }

            // 5. 予報円 & 接線 & 日時ラベルチップ
            val probFill = colorScheme.onSurface.copy(alpha = if (isDark) 0.10f else 0.06f).toArgb()
            val probStroke = colorScheme.outline.copy(alpha = if (isDark) 0.85f else 0.75f).toArgb()
            val tanStroke = colorScheme.outlineVariant.copy(alpha = if (isDark) 0.65f else 0.50f).toArgb()

            for (prob in typhoon.probabilityCircles) {
                // 予報円（半径 0 以下の不正値による地図描画例外を防止）
                if (prob.radiusMeters.isFinite() && prob.radiusMeters > 0.0) {
                    val probCircle = map.addCircle(
                        CircleOptions()
                            .center(prob.center)
                            .radius(prob.radiusMeters)
                            .fillColor(probFill)
                            .strokeColor(probStroke)
                            .strokeWidth(2.5f)
                            .strokePattern(listOf(Dash(16f), Gap(10f)))
                            .zIndex(64f)
                    )
                    activeObjects.add(probCircle)
                }

                // 予報円を結ぶ接線
                for (tangent in prob.tangents) {
                    if (tangent.size >= 2) {
                        val tanLine = map.addPolyline(
                            PolylineOptions()
                                .addAll(tangent)
                                .color(tanStroke)
                                .width(2.5f)
                                .zIndex(63f)
                        )
                        activeObjects.add(tanLine)
                    }
                }

                // 予報日時チップ (例: 02日18時: M3 SurfaceContainerHighest)
                val labelBitmap = createForecastTimeChipBitmap(context, prob.labelText, colorScheme, isDark)
                val labelDescriptor = try {
                    BitmapDescriptorFactory.fromBitmap(labelBitmap)
                } finally {
                    if (!labelBitmap.isRecycled) labelBitmap.recycle()
                }
                val labelMarker = map.addMarker(
                    MarkerOptions()
                        .position(prob.center)
                        .anchor(0.5f, 0.5f)
                        .icon(labelDescriptor)
                        .zIndex(65f)
                )
                if (labelMarker != null) activeObjects.add(labelMarker)
            }

            // 5.5 暴風警戒域 (JMA stormWarningAreas)
            if (typhoon.stormWarningAreas.isNotEmpty()) {
                val warnStroke = colorScheme.error.copy(alpha = if (isDark) 0.72f else 0.65f).toArgb()
                for (area in typhoon.stormWarningAreas) {
                    for (line in area.lines) {
                        if (line.size >= 2) {
                            val warnLine = map.addPolyline(
                                PolylineOptions()
                                    .addAll(line)
                                    .color(warnStroke)
                                    .width(3f)
                                    .pattern(listOf(Dash(14f), Gap(8f)))
                                    .geodesic(true)
                                    .zIndex(63.5f)
                            )
                            activeObjects.add(warnLine)
                        }
                    }
                    for (arc in area.arcs) {
                        if (arc.size >= 3) {
                            val lat = arc[0]
                            val lon = arc[1]
                            val radiusM = arc[2]
                            if (lat.isFinite() && lon.isFinite() && radiusM.isFinite() && radiusM > 0.0) {
                                val warnCircle = map.addCircle(
                                    CircleOptions()
                                        .center(LatLng(lat, lon))
                                        .radius(radiusM)
                                        .fillColor(android.graphics.Color.TRANSPARENT)
                                        .strokeColor(warnStroke)
                                        .strokeWidth(2.5f)
                                        .strokePattern(listOf(Dash(14f), Gap(8f)))
                                        .zIndex(63.5f)
                                )
                                activeObjects.add(warnCircle)
                            }
                        }
                    }
                }
            }

            // 6. 中心マーカー (アイコン不要・大きめのドット＋半透明の円・波動アニメーションなしの静的表示)
            // タップ後も変化せず、右側の詳細ポップアップのみ展開
            val dotMarkerDescriptor = getDotMarkerDescriptor(context, colorScheme, isDark)
            val markerOptions = MarkerOptions()
                .position(LatLng(analysis.latitude, analysis.longitude))
                .anchor(0.5f, 0.5f)
                .title(typhoon.simpleName(language))
                .icon(dotMarkerDescriptor)
                .zIndex(70f)
            val centerMarker = map.addMarker(markerOptions)
            if (centerMarker != null) {
                centerMarker.tag = typhoon
                activeObjects.add(centerMarker)
            }
        }
    }

    /**
     * 描画した台風オブジェクトをクリア
     */
    fun clearTyphoons(activeObjects: MutableList<Any>) {
        for (obj in activeObjects) {
            when (obj) {
                is Polyline -> try { obj.remove() } catch (_: Exception) {}
                is Circle -> try { obj.remove() } catch (_: Exception) {}
                is Marker -> try { obj.remove() } catch (_: Exception) {}
            }
        }
        activeObjects.clear()
    }

    /**
     * 台風中心マーカー（ドット＋半透明の円）の BitmapDescriptor を取得
     * - 外側：M3 Primaryの柔らかな半透明円（半径18dp）
     * - 内側：M3 Primaryのソリッドドット（半径7.5dp）＋白枠線（2dp）＋微細ドロップシャドウ
     * - アイコンなし・文字なし・アニメーションなしのシンプルかつ洗練された静的デザイン
     */
    fun getDotMarkerDescriptor(
        context: Context,
        colorScheme: ColorScheme,
        isDark: Boolean
    ): BitmapDescriptor {
        val density = context.resources.displayMetrics.density
        val primaryArgb = colorScheme.primary.toArgb()
        val cacheKey = "${primaryArgb}_${isDark}_$density"
        synchronized(dotMarkerCache) {
            dotMarkerCache.get(cacheKey)?.let { return it }
        }

        val canvasDp = 48f
        val canvasSize = (canvasDp * density).toInt()
        val centerX = canvasSize / 2f
        val centerY = canvasSize / 2f

        val haloRadius = 18f * density
        val dotRadius = 7.5f * density

        val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. 半透明の円 (外側のオーラ / ハロー)
        val haloAlpha = if (isDark) 0.24f else 0.20f
        val haloColor = colorScheme.primary.copy(alpha = haloAlpha).toArgb()
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = haloColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, haloRadius, haloPaint)

        // 半透明円の輪郭（繊細な境界線で視認性を強化）
        val haloStrokeColor = colorScheme.primary.copy(alpha = if (isDark) 0.40f else 0.32f).toArgb()
        val haloStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = haloStrokeColor
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
        }
        canvas.drawCircle(centerX, centerY, haloRadius - 0.5f * density, haloStrokePaint)

        // 2. ドットのドロップシャドウ (地図上でくっきり浮き立たせる)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(80, 0, 0, 0)
            style = Paint.Style.FILL
            setShadowLayer(3.5f * density, 0f, 1.5f * density, android.graphics.Color.argb(120, 0, 0, 0))
        }
        canvas.drawCircle(centerX, centerY, dotRadius, shadowPaint)

        // 3. ドットのコア (M3 Primary)
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryArgb
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, dotRadius, corePaint)

        // 4. 高コントラストな白枠（2dp、どのようなマップタイルや気象レイヤー上でも埋もれない）
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        canvas.drawCircle(centerX, centerY, dotRadius - 1f * density, borderPaint)

        val descriptor = try {
            BitmapDescriptorFactory.fromBitmap(bitmap)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        synchronized(dotMarkerCache) {
            dotMarkerCache.put(cacheKey, descriptor)
        }
        return descriptor
    }

    /**
     * 予報日時の極小タイムスタンプチップビットマップ（M3 SurfaceContainerHighest + OnSurfaceVariant）
     */
    fun createForecastTimeChipBitmap(
        context: Context,
        labelText: String,
        colorScheme: ColorScheme,
        isDark: Boolean
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.5f * density
            color = colorScheme.onSurfaceVariant.toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textWidth = textPaint.measureText(labelText)
        val padH = 8f * density
        val height = 20f * density
        val width = textWidth + padH * 2

        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val rect = RectF(0f, 0f, width, height)
        val corner = height / 2f

        val bgColor = colorScheme.surfaceContainerHighest.copy(alpha = if (isDark) 0.92f else 0.88f).toArgb()
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
            setShadowLayer(2f * density, 0f, 1f * density, android.graphics.Color.argb(50, 0, 0, 0))
        }
        canvas.drawRoundRect(rect, corner, corner, bgPaint)

        canvas.drawText(labelText, padH, height / 2f + (3.5f * density), textPaint)
        return bitmap
    }
}
