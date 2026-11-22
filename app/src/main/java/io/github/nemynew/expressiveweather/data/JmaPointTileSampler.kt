package io.github.nemynew.expressiveweather.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.tan

data class TilePixelCoord(
    val zoom: Int,
    val tileX: Int,
    val tileY: Int,
    val pixelX: Int,
    val pixelY: Int
)

data class JmaPointNowcastSeries(
    val points: List<Pair<Long, Double>>,
    val latestObservedMm: Double
)

data class JmaPointKikikuruRisk(
    val landslideLevel: Int,
    val inundationLevel: Int,
    val floodLevel: Int,
    val sampledAtMillis: Long
)

/**
 * 気象庁 (JMA) 公式タイル PNG（高解像度降水ナウキャスト hrpns・キキクル risk）から
 * 任意座標の5分刻み降水量時系列および災害危険度を直接サンプリングするエンジン。
 */
object JmaPointTileSampler {

    private const val TAG = "JmaPointTileSampler"
    private const val TILE_SIZE = 256
    private const val TILE_CACHE_TTL_MS = 3 * 60 * 1000L
    private const val MAX_TILE_CACHE_ENTRIES = 128

    private data class CachedTilePixels(
        val fetchedAtMillis: Long,
        val width: Int,
        val height: Int,
        val pixels: IntArray
    )

    private val tilePixelCache = ConcurrentHashMap<String, CachedTilePixels>()

    private data class PaletteEntry(
        val r: Int,
        val g: Int,
        val b: Int,
        val value: Double
    )

    // 気象庁 高解像度降水ナウキャスト (hrpns) 公式カラーパレット
    private val HRPNS_PALETTE = listOf(
        PaletteEntry(242, 242, 255, 0.5),  // < 1 mm/h (微弱・霧雨)
        PaletteEntry(212, 227, 245, 0.5),  // < 1 mm/h バリアント
        PaletteEntry(160, 210, 255, 2.5),  // 1 - 5 mm/h
        PaletteEntry(33, 140, 255, 7.5),   // 5 - 10 mm/h
        PaletteEntry(0, 65, 255, 15.0),    // 10 - 20 mm/h
        PaletteEntry(250, 245, 0, 25.0),   // 20 - 30 mm/h
        PaletteEntry(255, 153, 0, 40.0),   // 30 - 50 mm/h
        PaletteEntry(255, 40, 0, 65.0),    // 50 - 80 mm/h
        PaletteEntry(200, 0, 255, 65.0),   // 50 - 80 mm/h 代替パレット
        PaletteEntry(180, 0, 104, 90.0),   // >= 80 mm/h
        PaletteEntry(120, 0, 60, 95.0)     // 猛烈な雨極大トーン
    )

    // 気象庁 キキクル (risk/land, risk/inund, risk/flood) 公式警戒レベル配色
    private val KIKIKURU_PALETTE = listOf(
        PaletteEntry(12, 0, 12, 5.0),      // Lv5 災害切迫 (#0C000C)
        PaletteEntry(60, 56, 62, 5.0),     // Lv5 チャコールバリアント (#3C383E)
        PaletteEntry(170, 0, 170, 4.0),    // Lv4 危険 (#AA00AA)
        PaletteEntry(184, 79, 179, 4.0),   // Lv4 マゼンタバリアント (#B84FB3)
        PaletteEntry(255, 40, 0, 3.0),     // Lv3 警戒 (#FF2800)
        PaletteEntry(255, 82, 82, 3.0),    // Lv3 バリアント (#FF5252)
        PaletteEntry(242, 231, 0, 2.0),    // Lv2 注意 (#F2E700)
        PaletteEntry(253, 216, 53, 2.0)    // Lv2 バリアント (#FDD835)
    )

    /**
     * 緯度経度から Webメルカトル タイル座標および 256x256 内ピクセル位置を算出
     */
    fun latLonToTilePixel(
        latitude: Double,
        longitude: Double,
        zoom: Int = 10
    ): TilePixelCoord {
        val clampedLat = latitude.coerceIn(-85.05112878, 85.05112878)
        val n = (1 shl zoom).toDouble()
        val xFloat = ((longitude + 180.0) / 360.0) * n
        val latRad = Math.toRadians(clampedLat)
        val yFloat = (1.0 - asinh(tan(latRad)) / PI) / 2.0 * n

        val maxTileIdx = (1 shl zoom) - 1
        val tileX = floor(xFloat).toInt().coerceIn(0, maxTileIdx)
        val tileY = floor(yFloat).toInt().coerceIn(0, maxTileIdx)

        val pixelX = floor((xFloat - tileX) * TILE_SIZE).toInt().coerceIn(0, TILE_SIZE - 1)
        val pixelY = floor((yFloat - tileY) * TILE_SIZE).toInt().coerceIn(0, TILE_SIZE - 1)

        return TilePixelCoord(
            zoom = zoom,
            tileX = tileX,
            tileY = tileY,
            pixelX = pixelX,
            pixelY = pixelY
        )
    }

    /**
     * hrpns PNG ピクセル (ARGB) から降水量 (mm/h) へデコード
     */
    fun decodeHrpnsColorToPrecipMm(argb: Int): Double {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha < 48) return 0.0

        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF

        // 純白に近い背景透過/無降水ピクセルを除外
        if (r > 246 && g > 246 && b > 246) return 0.0

        var bestMatch: PaletteEntry? = null
        var bestDistSq = Int.MAX_VALUE
        for (entry in HRPNS_PALETTE) {
            val dr = r - entry.r
            val dg = g - entry.g
            val db = b - entry.b
            val distSq = dr * dr + dg * dg + db * db
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestMatch = entry
            }
        }
        // 最大許容色差 (~55 RGBユークリッド距離) 以内のみ雨域として採用
        return if (bestMatch != null && bestDistSq <= 3025) bestMatch.value else 0.0
    }

    /**
     * キキクル PNG ピクセル (ARGB) から警戒レベル相当 (0, 2..5) へデコード
     */
    fun decodeKikikuruColorToLevel(argb: Int): Int {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha < 100) return 0

        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF

        // 白 (Lv1以下 / 背景)・無彩色グレー (警戒対象外) は 0 とする
        if (r > 220 && g > 220 && b > 220) return 0

        var bestMatch: PaletteEntry? = null
        var bestDistSq = Int.MAX_VALUE
        for (entry in KIKIKURU_PALETTE) {
            val dr = r - entry.r
            val dg = g - entry.g
            val db = b - entry.b
            val distSq = dr * dr + dg * dg + db * db
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestMatch = entry
            }
        }
        // 境界線やアンチエイリアスの誤反応を防ぐため厳しめの色距離 (<= 45) に制限
        return if (bestMatch != null && bestDistSq <= 2025) bestMatch.value.toInt() else 0
    }

    /**
     * 中心ピクセルと近傍ウィンドウから単発スパイクノイズを除去した代表降水量を算出
     */
    fun sampleWindowPrecipMm(
        pixels: IntArray,
        width: Int,
        height: Int,
        centerX: Int,
        centerY: Int,
        radiusPx: Int = 1
    ): Double {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return 0.0
        val cx = centerX.coerceIn(0, width - 1)
        val cy = centerY.coerceIn(0, height - 1)
        val centerMm = decodeHrpnsColorToPrecipMm(pixels[cy * width + cx])

        val values = mutableListOf<Double>()
        for (dy in -radiusPx..radiusPx) {
            val y = (cy + dy).coerceIn(0, height - 1)
            for (dx in -radiusPx..radiusPx) {
                val x = (cx + dx).coerceIn(0, width - 1)
                values.add(decodeHrpnsColorToPrecipMm(pixels[y * width + x]))
            }
        }
        val sortedDesc = values.sortedDescending()
        val secondHighest = sortedDesc.getOrElse(1) { 0.0 }
        // 中心が降っていれば中心値、近傍複数ピクセルに雨雲が掛かっていれば第2位値を採用（1pxノイズ除外）
        return max(centerMm, secondHighest)
    }

    /**
     * 中心ピクセルと近傍ウィンドウからキキクル危険度を算出
     * requireTwoPixels=true のときは2px以上一致を要求（洪水ライン用には false で1px検出を許可）
     */
    fun sampleWindowRiskLevel(
        pixels: IntArray,
        width: Int,
        height: Int,
        centerX: Int,
        centerY: Int,
        radiusPx: Int = 2,
        requireTwoPixels: Boolean = true
    ): Int {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return 0
        val cx = centerX.coerceIn(0, width - 1)
        val cy = centerY.coerceIn(0, height - 1)
        val centerLevel = decodeKikikuruColorToLevel(pixels[cy * width + cx])

        val levels = mutableListOf<Int>()
        for (dy in -radiusPx..radiusPx) {
            val y = (cy + dy).coerceIn(0, height - 1)
            for (dx in -radiusPx..radiusPx) {
                val x = (cx + dx).coerceIn(0, width - 1)
                levels.add(decodeKikikuruColorToLevel(pixels[y * width + x]))
            }
        }
        val sortedDesc = levels.sortedDescending()
        if (!requireTwoPixels) {
            return sortedDesc.firstOrNull() ?: 0
        }
        val secondHighest = sortedDesc.getOrElse(1) { 0 }
        return max(centerLevel, secondHighest)
    }

    /**
     * 指定座標の気象庁 高解像度降水ナウキャスト (hrpns: 実況+60分先) および
     * 降水短時間予報 (rasrf: 1〜8時間先) タイルをサンプリングして降水量時系列を取得
     */
    suspend fun sampleHrpnsTimeline(
        latitude: Double,
        longitude: Double
    ): JmaPointNowcastSeries? = withContext(Dispatchers.IO) {
        if (!isLocationInJapan(latitude, longitude)) return@withContext null
        try {
            val jmaRepo = JmaNowcastRepository.getInstance()
            val radarResult = jmaRepo.getLayerData(JmaLayerType.HRPNS, forceRefresh = false)
            val response = radarResult.getOrNull() ?: return@withContext null
            if (response.version.contains("SYNTHETIC", ignoreCase = true)) {
                return@withContext null
            }

            val latestObs = response.radar.past.lastOrNull {
                it.tileUrlTemplate?.contains("surf/hrpns") == true
            }
            val nowcastHrpns = response.radar.nowcast.filter {
                it.tileUrlTemplate?.contains("surf/hrpns") == true
            }.take(12)
            val rasrfFrames = response.radar.nowcast.filter {
                it.tileUrlTemplate?.contains("surf/rasrf") == true
            }.take(8)

            val targetFrames = (listOfNotNull(latestObs) + nowcastHrpns + rasrfFrames).distinctBy { it.time }
            if (targetFrames.isEmpty()) return@withContext null

            val coordZ10 = latLonToTilePixel(latitude, longitude, zoom = 10)
            val coordZ8 = latLonToTilePixel(latitude, longitude, zoom = 8)
            val sampledPoints = coroutineScope {
                targetFrames.map { frame ->
                    async {
                        val template = frame.tileUrlTemplate ?: return@async null
                        val isRasrf = template.contains("surf/rasrf")
                        var activeCoord = coordZ10
                        var cachedTile = fetchAndDecodeTilePixels(buildConcreteTileUrl(template, coordZ10))
                        // 降水短時間予報 (rasrf) が z=10 で未配信の領域は z=8 にフォールバック
                        if (cachedTile == null && isRasrf) {
                            activeCoord = coordZ8
                            cachedTile = fetchAndDecodeTilePixels(buildConcreteTileUrl(template, coordZ8))
                        }
                        cachedTile ?: return@async null
                        val mm = sampleWindowPrecipMm(
                            pixels = cachedTile.pixels,
                            width = cachedTile.width,
                            height = cachedTile.height,
                            centerX = activeCoord.pixelX,
                            centerY = activeCoord.pixelY,
                            radiusPx = 1
                        )
                        (frame.time * 1000L) to mm
                    }
                }.awaitAll().filterNotNull().sortedBy { it.first }
            }

            if (sampledPoints.isEmpty()) return@withContext null

            val obsMm = if (latestObs != null) {
                val obsMillis = latestObs.time * 1000L
                sampledPoints.firstOrNull { it.first == obsMillis }?.second ?: sampledPoints.first().second
            } else {
                0.0
            }

            Log.d(
                TAG,
                "Sampled JMA precip timeline (${sampledPoints.size} frames: hrpns=${nowcastHrpns.size}, rasrf=${rasrfFrames.size}): obs=${obsMm}mm/h, peak=${sampledPoints.maxOfOrNull { it.second } ?: 0.0}mm/h"
            )
            JmaPointNowcastSeries(
                points = sampledPoints,
                latestObservedMm = obsMm
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed sampling HRPNS timeline: ${e.message}")
            null
        }
    }

    /**
     * 指定座標の気象庁 キキクル（土砂・浸水・洪水危険度分布）実測レベルを取得
     */
    suspend fun sampleKikikuruRisk(
        latitude: Double,
        longitude: Double
    ): JmaPointKikikuruRisk? = withContext(Dispatchers.IO) {
        if (!isLocationInJapan(latitude, longitude)) return@withContext null
        try {
            val jmaRepo = JmaNowcastRepository.getInstance()
            val coord = latLonToTilePixel(latitude, longitude, zoom = 10)

            coroutineScope {
                val landDef = async {
                    sampleLatestLayerRisk(jmaRepo, JmaLayerType.LANDSLIDE, coord, radiusPx = 2, requireTwo = true)
                }
                val inundDef = async {
                    sampleLatestLayerRisk(jmaRepo, JmaLayerType.INUNDATION, coord, radiusPx = 2, requireTwo = true)
                }
                val floodDef = async {
                    sampleLatestLayerRisk(jmaRepo, JmaLayerType.FLOOD, coord, radiusPx = 4, requireTwo = false)
                }

                val landLvl = landDef.await()
                val inundLvl = inundDef.await()
                val floodLvl = floodDef.await()

                if (landLvl == null && inundLvl == null && floodLvl == null) {
                    return@coroutineScope null
                }

                Log.d(
                    TAG,
                    "Sampled Kikikuru risk: land=${landLvl ?: 0}, inund=${inundLvl ?: 0}, flood=${floodLvl ?: 0}"
                )
                JmaPointKikikuruRisk(
                    landslideLevel = landLvl ?: 0,
                    inundationLevel = inundLvl ?: 0,
                    floodLevel = floodLvl ?: 0,
                    sampledAtMillis = System.currentTimeMillis()
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed sampling Kikikuru risk tiles: ${e.message}")
            null
        }
    }

    private suspend fun sampleLatestLayerRisk(
        jmaRepo: JmaNowcastRepository,
        layer: JmaLayerType,
        coord: TilePixelCoord,
        radiusPx: Int,
        requireTwo: Boolean
    ): Int? {
        val result = jmaRepo.getLayerData(layer, forceRefresh = false)
        val resp = result.getOrNull() ?: return null
        val frame = resp.radar.past.lastOrNull() ?: resp.radar.nowcast.firstOrNull() ?: return null
        val template = frame.tileUrlTemplate ?: return null
        val tileUrl = buildConcreteTileUrl(template, coord)
        val cachedTile = fetchAndDecodeTilePixels(tileUrl) ?: return null
        return sampleWindowRiskLevel(
            pixels = cachedTile.pixels,
            width = cachedTile.width,
            height = cachedTile.height,
            centerX = coord.pixelX,
            centerY = coord.pixelY,
            radiusPx = radiusPx,
            requireTwoPixels = requireTwo
        )
    }

    private fun buildConcreteTileUrl(template: String, coord: TilePixelCoord): String {
        return template
            .replace("{z}", coord.zoom.toString())
            .replace("{x}", coord.tileX.toString())
            .replace("{y}", coord.tileY.toString())
    }

    private suspend fun fetchAndDecodeTilePixels(tileUrl: String): CachedTilePixels? {
        val now = System.currentTimeMillis()
        val cached = tilePixelCache[tileUrl]
        if (cached != null && (now - cached.fetchedAtMillis) < TILE_CACHE_TTL_MS) {
            return cached
        }

        val bytes = AppHttpClient.getBytesOrNull(
            url = tileUrl,
            headers = mapOf("Referer" to "https://www.jma.go.jp/"),
            timeoutMs = 4_500L
        ) ?: return null

        val bitmap: Bitmap? = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Throwable) {
            null
        }
        if (bitmap == null) return null

        return try {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return null
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val entry = CachedTilePixels(now, w, h, pixels)
            if (tilePixelCache.size >= MAX_TILE_CACHE_ENTRIES) {
                tilePixelCache.entries
                    .filter { (now - it.value.fetchedAtMillis) >= TILE_CACHE_TTL_MS }
                    .forEach { tilePixelCache.remove(it.key) }
                if (tilePixelCache.size >= MAX_TILE_CACHE_ENTRIES) {
                    tilePixelCache.clear()
                }
            }
            tilePixelCache[tileUrl] = entry
            entry
        } finally {
            bitmap.recycle()
        }
    }
}
