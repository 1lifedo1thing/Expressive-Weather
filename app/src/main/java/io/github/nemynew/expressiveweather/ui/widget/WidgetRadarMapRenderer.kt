package io.github.nemynew.expressiveweather.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import io.github.nemynew.expressiveweather.R
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.RadarDataSource
import io.github.nemynew.expressiveweather.data.RadarProviderRegistry
import io.github.nemynew.expressiveweather.data.RadarRepository
import io.github.nemynew.expressiveweather.data.isLocationOrCountryInJapan
import io.github.nemynew.expressiveweather.data.local.LocationKeyNormalizer
import io.github.nemynew.expressiveweather.ui.radar.RadarTileHttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * Material You スタイル 2×4 雨雲レーダーウィジェット専用の地図＆レーダー合成レンダラー
 * - Web Mercator XYZ ベースマップ（Esri World Light Gray + 日本域 GSI / 海外 Esri Reference 境界・地名オーバーレイ）を取得
 * - 輝度 LUT により実地図をデバイスの Material You ダイナミックカラー（ダーク/ライト連動）へ調色
 * - RadarRepository (JMA / US NEXRAD / RainViewer) の最新実況レーダータイルを高精度合成
 */
object WidgetRadarMapRenderer {
    private const val TAG = "WidgetRadarMapRenderer"
    private const val CACHE_DIR_NAME = "widget_radar_cache"
    private const val BASE_TILE_TTL_MS = 14L * 24 * 60 * 60 * 1000L
    private const val COMPOSITE_STALE_FALLBACK_MAX_AGE_MS = 3L * 60 * 60 * 1000L
    private const val TILE_SIZE = 256.0

    @Volatile
    private var cachedBadgeFont: Typeface? = null

    private fun getBadgeTypeface(context: Context): Typeface {
        cachedBadgeFont?.let { return it }
        val resolved = runCatching {
            ResourcesCompat.getFont(context, R.font.google_sans_flex)
        }.getOrNull() ?: Typeface.SANS_SERIF
        cachedBadgeFont = resolved
        return resolved
    }

    /**
     * キャッシュ済みの合成済みレーダー画像があれば即座に返し、無ければ Material You 調のオフライン地図グリッドを生成する。
     */
    fun loadCachedOrFallbackBitmap(
        context: Context,
        city: City,
        myPalette: WidgetMaterialYouPalette,
        isSystemDark: Boolean,
        language: AppLanguage,
        widthPx: Int = 300,
        heightPx: Int = 240
    ): Bitmap {
        val file = getCompositeCacheFile(context, city, isSystemDark, widthPx, heightPx)
        if (file.exists()) {
            val decoded = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            if (decoded != null) return decoded
        }
        val altWidth = if (widthPx == 420) 300 else 420
        val altHeight = if (widthPx == 420) 240 else 220
        val altSizeFile = getCompositeCacheFile(context, city, isSystemDark, altWidth, altHeight)
        if (altSizeFile.exists()) {
            val decoded = runCatching { BitmapFactory.decodeFile(altSizeFile.absolutePath) }.getOrNull()
            if (decoded != null) return decoded
        }
        return renderFallbackMapBitmap(
            context = context,
            myPalette = myPalette,
            isSystemDark = isSystemDark,
            language = language,
            widthPx = widthPx,
            heightPx = heightPx
        )
    }

    fun hasCachedComposite(
        context: Context,
        city: City,
        isSystemDark: Boolean,
        widthPx: Int = 300,
        heightPx: Int = 240
    ): Boolean {
        val file = getCompositeCacheFile(context, city, isSystemDark, widthPx, heightPx)
        return file.exists() && file.length() > 0L
    }

    /**
     * ベース地図タイル + 最新雨雲レーダータイルを取得し、Material You 調色を施してディスクキャッシュへ保存する。
     */
    suspend fun fetchAndRenderAndCache(
        context: Context,
        city: City,
        myPalette: WidgetMaterialYouPalette,
        isSystemDark: Boolean,
        language: AppLanguage,
        forceRefresh: Boolean = true,
        widthPx: Int = 300,
        heightPx: Int = 240
    ): Bitmap = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val lat = city.latitude
        val lon = city.longitude

        val radarRepo = RadarRepository.getInstance(appCtx)
        val mode = radarRepo.getSourceMode()
        val resolvedSource = RadarProviderRegistry.resolveProvider(mode, lat, lon, city.country)
        val zoom = when (resolvedSource) {
            RadarDataSource.JMA_NOWCAST, RadarDataSource.US_NEXRAD_HRRR -> 8
            RadarDataSource.RAIN_VIEWER -> 7
        }

        val (radarResult, actualSource) = try {
            radarRepo.getRadarData(lat, lon, city.country, forceRefresh = forceRefresh)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure<io.github.nemynew.expressiveweather.data.RainViewerResponse>(e) to resolvedSource
        }

        val provider = RadarProviderRegistry.get(actualSource)
        val frames = try {
            provider?.getTimelineFrames(lat, lon, forceRefresh = false)?.getOrNull() ?: emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
        val observedCandidates = frames
            .filter { !it.isForecast }
            .asReversed()
            .ifEmpty { listOfNotNull(frames.lastOrNull()) }

        val worldTiles = 1 shl zoom
        val centerTileX = ((lon + 180.0) / 360.0) * worldTiles
        val latRad = Math.toRadians(lat.coerceIn(-85.0, 85.0))
        val centerTileY = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * worldTiles

        val centerPxX = centerTileX * TILE_SIZE
        val centerPxY = centerTileY * TILE_SIZE
        val leftPx = centerPxX - widthPx / 2.0
        val topPx = centerPxY - heightPx / 2.0
        val rightPx = leftPx + widthPx
        val bottomPx = topPx + heightPx

        val minTileX = floor(leftPx / TILE_SIZE).toInt()
        val maxTileX = floor((rightPx - 1.0) / TILE_SIZE).toInt()
        val minTileY = floor(topPx / TILE_SIZE).toInt().coerceIn(0, worldTiles - 1)
        val maxTileY = floor((bottomPx - 1.0) / TILE_SIZE).toInt().coerceIn(0, worldTiles - 1)

        val tileCoords = buildList {
            for (ty in minTileY..maxTileY) {
                for (txRaw in minTileX..maxTileX) {
                    val tx = ((txRaw % worldTiles) + worldTiles) % worldTiles
                    add(Triple(txRaw, tx, ty))
                }
            }
        }

        val inJapan = isLocationOrCountryInJapan(city.country, lat, lon)
        val (baseTiles, overlayTiles) = coroutineScope {
            val baseDeferred = tileCoords.map { (txRaw, tx, ty) ->
                async {
                    (txRaw to ty) to fetchBaseTile(appCtx, zoom, tx, ty, inJapan)
                }
            }
            val overlayDeferred = tileCoords.map { (txRaw, tx, ty) ->
                async {
                    (txRaw to ty) to fetchLabelOverlayTile(appCtx, zoom, tx, ty, inJapan)
                }
            }
            baseDeferred.awaitAll().toMap() to overlayDeferred.awaitAll().toMap()
        }

        var selectedFrame = observedCandidates.firstOrNull()
        var radarTiles: Map<Pair<Int, Int>, Bitmap?> = emptyMap()
        var radarHttpSuccessCount = 0
        if (provider != null && observedCandidates.isNotEmpty()) {
            // 最新フレームが配信直後/通信瞬断で全滅した場合は1つ前の実況フレームへフォールバック
            for (candidate in observedCandidates.take(2)) {
                val results = coroutineScope {
                    tileCoords.map { (txRaw, tx, ty) ->
                        async {
                            val url = provider.buildTileUrl(candidate, tx, ty, zoom)
                            val bytes = RadarTileHttpClient.getTileBytes(url)
                            val bmp = bytes?.let {
                                runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull()
                            }
                            val ok = bmp != null || RadarTileHttpClient.isKnownEmptyTile(url)
                            Triple(txRaw to ty, bmp, ok)
                        }
                    }.awaitAll()
                }
                val okCount = results.count { it.third }
                selectedFrame = candidate
                radarTiles = results.associate { it.first to it.second }
                radarHttpSuccessCount = okCount
                if (okCount > 0) break
                results.forEach { it.second?.recycle() }
            }
        }

        val existingFile = getCompositeCacheFile(appCtx, city, isSystemDark, widthPx, heightPx)
        val hasRadarUpdateSuccess = selectedFrame != null && radarHttpSuccessCount > 0
        if (!hasRadarUpdateSuccess && existingFile.exists()) {
            val existingAgeMs = System.currentTimeMillis() - existingFile.lastModified()
            if (existingAgeMs < COMPOSITE_STALE_FALLBACK_MAX_AGE_MS) {
                val existingBmp = runCatching { BitmapFactory.decodeFile(existingFile.absolutePath) }.getOrNull()
                if (existingBmp != null) {
                    baseTiles.values.forEach { it?.recycle() }
                    overlayTiles.values.forEach { it?.recycle() }
                    radarTiles.values.forEach { it?.recycle() }
                    return@withContext existingBmp
                }
            }
        }

        val rawMapBitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rawMapBitmap)

        // 1. ベース地図を描画＆Material You カラートーンへ LUT 変換
        val baseLayerBitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val baseCanvas = Canvas(baseLayerBitmap)
        var anyBaseTileDrawn = false
        val tilePaint = Paint(Paint.FILTER_BITMAP_FLAG)
        for ((txRaw, _, ty) in tileCoords) {
            val bmp = baseTiles[txRaw to ty] ?: continue
            val dx = (txRaw * TILE_SIZE - leftPx).toFloat()
            val dy = (ty * TILE_SIZE - topPx).toFloat()
            baseCanvas.drawBitmap(bmp, dx, dy, tilePaint)
            bmp.recycle()
            anyBaseTileDrawn = true
        }

        if (anyBaseTileDrawn) {
            applyMaterialYouMapLut(baseLayerBitmap, myPalette, isSystemDark)
            canvas.drawBitmap(baseLayerBitmap, 0f, 0f, null)
            baseLayerBitmap.recycle()
        } else {
            baseLayerBitmap.recycle()
            drawFallbackGridBase(canvas, widthPx.toFloat(), heightPx.toFloat(), myPalette, isSystemDark)
        }

        // 1.5 日本域（GSI 白地図）/ 海外域（Esri Reference）の境界線・地名ラベルを Material You トーンで薄く重畳
        if (overlayTiles.values.any { it != null }) {
            val lineLayer = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val lineCanvas = Canvas(lineLayer)
            var drewAnyLine = false
            for ((txRaw, _, ty) in tileCoords) {
                val oBmp = overlayTiles[txRaw to ty] ?: continue
                val dx = (txRaw * TILE_SIZE - leftPx).toFloat()
                val dy = (ty * TILE_SIZE - topPx).toFloat()
                lineCanvas.drawBitmap(oBmp, dx, dy, tilePaint)
                oBmp.recycle()
                drewAnyLine = true
            }
            if (drewAnyLine) {
                tintBorderOverlay(lineLayer, myPalette.secondaryTextColor, isSystemDark)
                canvas.drawBitmap(lineLayer, 0f, 0f, null)
            }
            lineLayer.recycle()
        }

        // 2. 雨雲レーダータイルを Web Mercator 座標へ正確に合成（ダークモード時は極淡白フチの白浮きを抑制）
        val radarPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            alpha = if (isSystemDark) 216 else 226
        }
        for ((txRaw, _, ty) in tileCoords) {
            val rBmp = radarTiles[txRaw to ty] ?: continue
            val refined = prepareRadarTileForOverlay(rBmp, isSystemDark)
            val dx = (txRaw * TILE_SIZE - leftPx).toFloat()
            val dy = (ty * TILE_SIZE - topPx).toFloat()
            val dest = RectF(dx, dy, dx + TILE_SIZE.toFloat(), dy + TILE_SIZE.toFloat())
            canvas.drawBitmap(refined, null, dest, radarPaint)
            if (refined !== rBmp) {
                refined.recycle()
            }
            rBmp.recycle()
        }

        // 3. 中心ピン描画＋時刻ラベル保存（バッジ自体は ImageView オーバーレイとして描画し centerCrop 切れを防止）
        drawCenterLocationPin(canvas, widthPx / 2f, heightPx / 2f, myPalette)
        if (selectedFrame != null && radarHttpSuccessCount > 0) {
            saveBadgeTimeLabel(appCtx, city, selectedFrame.timeLabel)
            // 4. 有効なレーダー取得時のみディスクキャッシュへ保存（通信失敗時の無雲キャッシュ上書きを防止）
            saveCompositeCache(appCtx, city, isSystemDark, widthPx, heightPx, rawMapBitmap)
        } else if (!existingFile.exists() && anyBaseTileDrawn) {
            saveCompositeCache(appCtx, city, isSystemDark, widthPx, heightPx, rawMapBitmap)
        }
        rawMapBitmap
    }

    fun renderFallbackMapBitmap(
        context: Context,
        myPalette: WidgetMaterialYouPalette,
        isSystemDark: Boolean,
        language: AppLanguage,
        widthPx: Int = 300,
        heightPx: Int = 240
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawFallbackGridBase(canvas, widthPx.toFloat(), heightPx.toFloat(), myPalette, isSystemDark)
        drawCenterLocationPin(canvas, widthPx / 2f, heightPx / 2f, myPalette)
        return bitmap
    }

    private fun drawFallbackGridBase(
        canvas: Canvas,
        w: Float,
        h: Float,
        myPalette: WidgetMaterialYouPalette,
        isSystemDark: Boolean
    ) {
        val mapBg = if (isSystemDark) {
            WidgetCanvasRenderer.blendColors(myPalette.cardTopColor, myPalette.accentColor, 0.12f)
        } else {
            WidgetCanvasRenderer.blendColors(myPalette.cardTopColor, myPalette.accentColor, 0.14f)
        }
        canvas.drawColor(mapBg)

        val ringColor = WidgetCanvasRenderer.withAlpha(myPalette.accentColor, if (isSystemDark) 42 else 36)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            color = ringColor
        }
        val cx = w / 2f
        val cy = h / 2f
        val step = w.coerceAtMost(h) * 0.24f
        for (i in 1..4) {
            canvas.drawCircle(cx, cy, step * i, ringPaint)
        }
        canvas.drawLine(cx, 0f, cx, h, ringPaint)
        canvas.drawLine(0f, cy, w, cy, ringPaint)
    }

    private fun applyMaterialYouMapLut(
        bitmap: Bitmap,
        myPalette: WidgetMaterialYouPalette,
        isSystemDark: Boolean
    ) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val landColor = if (isSystemDark) {
            WidgetCanvasRenderer.blendColors(myPalette.cardTopColor, myPalette.accentColor, 0.08f)
        } else {
            WidgetCanvasRenderer.blendColors(myPalette.cardTopColor, Color.WHITE, 0.42f)
        }
        val urbanTone = WidgetCanvasRenderer.blendColors(landColor, myPalette.accentColor, if (isSystemDark) 0.16f else 0.12f)
        val waterColor = if (isSystemDark) {
            WidgetCanvasRenderer.blendColors(myPalette.cardTopColor, Color.BLACK, 0.36f)
        } else {
            WidgetCanvasRenderer.blendColors(myPalette.primaryContainerColor, myPalette.accentColor, 0.32f)
        }
        val lineColor = WidgetCanvasRenderer.blendColors(
            waterColor,
            myPalette.secondaryTextColor,
            if (isSystemDark) 0.32f else 0.35f
        )

        val lut = IntArray(256)
        for (lum in 0..255) {
            lut[lum] = when {
                lum >= 242 -> landColor
                lum >= 229 -> {
                    val t = (242 - lum) / 13f
                    WidgetCanvasRenderer.blendColors(landColor, urbanTone, t)
                }
                lum >= 198 -> {
                    val t = (lum - 198) / 31f
                    WidgetCanvasRenderer.blendColors(waterColor, urbanTone, t)
                }
                else -> {
                    val t = (lum / 198f).coerceIn(0f, 1f)
                    WidgetCanvasRenderer.blendColors(lineColor, waterColor, t)
                }
            }
        }

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val lum = ((r * 77 + g * 150 + b * 29) shr 8).coerceIn(0, 255)
            pixels[i] = lut[lum]
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun drawCenterLocationPin(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        myPalette: WidgetMaterialYouPalette
    ) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WidgetCanvasRenderer.withAlpha(myPalette.accentColor, 72)
        }
        canvas.drawCircle(cx, cy, 16f, haloPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawCircle(cx, cy, 7.5f, ringPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = myPalette.accentColor
        }
        canvas.drawCircle(cx, cy, 5f, dotPaint)
    }

    fun getCachedBadgeText(
        context: Context,
        city: City,
        language: AppLanguage
    ): String {
        val file = getBadgeLabelFile(context, city)
        val rawTime = if (file.exists()) runCatching { file.readText().trim() }.getOrNull() else null
        return formatBadgeLabel(rawTime, language)
    }

    fun renderRadarBadgePillBitmap(
        context: Context,
        text: String,
        myPalette: WidgetMaterialYouPalette
    ): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = myPalette.onPrimaryContainerColor
            textSize = 21f
            typeface = getBadgeTypeface(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                fontVariationSettings = "'wght' 650, 'ROND' 100"
            }
        }
        val textW = paint.measureText(text)
        val fm = paint.fontMetrics
        val textH = fm.descent - fm.ascent
        val padH = 16f
        val padV = 7f
        val w = ceil((textW + padH * 2f).toDouble()).toInt().coerceAtLeast(24)
        val h = ceil((textH + padV * 2f).toDouble()).toInt().coerceAtLeast(20)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            density = android.util.DisplayMetrics.DENSITY_XHIGH
        }
        val canvas = Canvas(bmp)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WidgetCanvasRenderer.withAlpha(myPalette.primaryContainerColor, 236)
        }
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, bgPaint)
        canvas.drawText(text, padH, padV - fm.ascent, paint)
        return bmp
    }

    private fun saveBadgeTimeLabel(context: Context, city: City, timeLabel: String?) {
        val t = timeLabel?.trim().orEmpty()
        val file = getBadgeLabelFile(context, city)
        if (t.isEmpty() && file.exists() && file.length() > 0L) return
        runCatching {
            file.writeText(t)
        }
    }

    private fun prepareRadarTileForOverlay(source: Bitmap, isSystemDark: Boolean): Bitmap {
        if (!isSystemDark) return source
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        var modified = false
        for (i in pixels.indices) {
            val orig = pixels[i]
            val refined = refineRadarOverlayPixel(orig, isSystemDark = true)
            if (refined != orig) {
                pixels[i] = refined
                modified = true
            }
        }
        if (!modified) return source
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    internal fun refineRadarOverlayPixel(argb: Int, isSystemDark: Boolean): Int {
        val a = (argb ushr 24) and 0xFF
        if (a == 0) return 0
        if (!isSystemDark) return argb
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        // 気象庁 0.1〜1 mm/h 帯 (#F2F2FF) はダーク背景上で白く浮くため、柔らかな透過スカイブルーへ調色
        if (r >= 225 && g >= 225 && b >= 240 && (b - r) <= 32) {
            val softAlpha = (a * 0.44f).toInt().coerceIn(0, 255)
            return (softAlpha shl 24) or (156 shl 16) or (200 shl 8) or 255
        }
        return argb
    }

    private fun getBadgeLabelFile(context: Context, city: City): File {
        val dir = File(context.applicationContext.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
        val key = LocationKeyNormalizer.normalize(city.latitude, city.longitude)
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return File(dir, "badge_v1_${key}.txt")
    }

    private fun formatBadgeLabel(timeLabel: String?, language: AppLanguage): String {
        val live = when (language) {
            AppLanguage.JAPANESE -> "雨雲レーダー"
            AppLanguage.ENGLISH -> "Radar"
            AppLanguage.SIMPLIFIED_CHINESE -> "降水雷达"
            AppLanguage.TRADITIONAL_CHINESE -> "降雨雷達"
        }
        val t = timeLabel?.trim()?.takeIf { it.isNotBlank() } ?: return live
        return when (language) {
            AppLanguage.JAPANESE -> "$t 実況"
            AppLanguage.ENGLISH -> "$t Live"
            AppLanguage.SIMPLIFIED_CHINESE -> "$t 实况"
            AppLanguage.TRADITIONAL_CHINESE -> "$t 實況"
        }
    }

    private fun tintBorderOverlay(
        bitmap: Bitmap,
        lineRgbColor: Int,
        isSystemDark: Boolean
    ) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val targetR = Color.red(lineRgbColor)
        val targetG = Color.green(lineRgbColor)
        val targetB = Color.blue(lineRgbColor)
        val maxAlpha = if (isSystemDark) 118 else 128
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            if (a < 32) {
                pixels[i] = 0
                continue
            }
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val lum = (r * 77 + g * 150 + b * 29) shr 8
            if (lum >= 220) {
                pixels[i] = 0
            } else {
                val strength = ((220 - lum) / 140f).coerceIn(0f, 1f)
                val outA = ((a / 255f) * strength * maxAlpha).toInt().coerceIn(0, maxAlpha)
                pixels[i] = (outA shl 24) or (targetR shl 16) or (targetG shl 8) or targetB
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun fetchBaseTile(
        context: Context,
        zoom: Int,
        tx: Int,
        ty: Int,
        inJapan: Boolean
    ): Bitmap? {
        // 日本域は国土地理院タイル (出典: 国土地理院) を使用。
        // 海外域は Esri / OSM 標準タイルの直接取得を避け、Material You レーダースコープ背景へフォールバックする。
        if (!inJapan) return null
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
        val file = File(cacheDir, "base_v3_jp_${zoom}_${tx}_${ty}.img")
        val now = System.currentTimeMillis()
        if (file.exists() && (now - file.lastModified()) < BASE_TILE_TTL_MS) {
            runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()?.let { return it }
        }

        val candidates = listOf(
            "https://cyberjapandata.gsi.go.jp/xyz/pale/$zoom/$tx/$ty.png",
            "https://www.jma.go.jp/tile/gsi/pale/$zoom/$tx/$ty.png"
        )
        for (url in candidates) {
            val bytes = RadarTileHttpClient.getTileBytes(url) ?: continue
            val decoded = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull() ?: continue
            runCatching { file.writeBytes(bytes) }
            return decoded
        }
        return if (file.exists()) {
            runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        } else null
    }

    private fun fetchLabelOverlayTile(
        context: Context,
        zoom: Int,
        tx: Int,
        ty: Int,
        inJapan: Boolean
    ): Bitmap? {
        if (!inJapan) return null
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
        val file = File(cacheDir, "jp_blank_v2_${zoom}_${tx}_${ty}.png")
        val now = System.currentTimeMillis()
        if (file.exists() && (now - file.lastModified()) < BASE_TILE_TTL_MS) {
            runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()?.let { return it }
        }
        val url = "https://cyberjapandata.gsi.go.jp/xyz/blank/$zoom/$tx/$ty.png"
        val bytes = RadarTileHttpClient.getTileBytes(url)
        if (bytes != null) {
            val decoded = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
            if (decoded != null) {
                runCatching { file.writeBytes(bytes) }
                return decoded
            }
        }
        return if (file.exists()) {
            runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        } else null
    }

    private fun getCompositeCacheFile(
        context: Context,
        city: City,
        isSystemDark: Boolean,
        widthPx: Int,
        heightPx: Int
    ): File {
        val dir = File(context.applicationContext.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
        val key = LocationKeyNormalizer.normalize(city.latitude, city.longitude)
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return File(dir, "comp_v4_${key}_${isSystemDark}_${widthPx}x${heightPx}.png")
    }

    private fun saveCompositeCache(
        context: Context,
        city: City,
        isSystemDark: Boolean,
        widthPx: Int,
        heightPx: Int,
        bitmap: Bitmap
    ) {
        try {
            val file = getCompositeCacheFile(context, city, isSystemDark, widthPx, heightPx)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving radar widget composite cache: ${e.message}")
        }
    }
}
