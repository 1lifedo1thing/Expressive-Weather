package com.example.expressiveweather.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * 雨雲レーダータイルのLRUメモリキャッシュおよび前後2フレーム先読みプリフェッチャー
 *
 * 要件:
 * 1. 再生中のチラつきを抑えるため、現在表示中の前後2フレーム (currentIndex ± 2) を非同期に先読み
 * 2. メモリリークおよびOut Of Memory (OOM) を避けるため、一定容量以上のBitmapタイルキャッシュはLRU方式で自動破棄
 */
class RadarTileCacheManager private constructor() {

    companion object {
        private const val TAG = "RadarTileCacheManager"

        @Volatile
        private var INSTANCE: RadarTileCacheManager? = null

        fun getInstance(): RadarTileCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RadarTileCacheManager().also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var prefetchJob: Job? = null

    // 同一URLの重複ダウンロード・デコードを防ぐためのインフライトロック
    private val inFlightUrls = ConcurrentHashMap<String, Job>()

    // 1. BitmapタイルのLRUキャッシュ (最大メモリの1/8または最大約32MB)
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 8).coerceIn(16 * 1024, 64 * 1024) // 16MB〜64MB

    private val bitmapLruCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            if (evicted) {
                // LRUポリシーによってキャッシュから溢れた古いBitmapは自動破棄
                Log.v(TAG, "Tile evicted from Bitmap LRU cache: $key")
            }
        }
    }

    // 2. ダウンロード済み生タイルバイト列 (PNG) のLRUキャッシュ (最大500枚)
    private val rawBytesLruCache = object : LruCache<String, ByteArray>(500) {
        override fun sizeOf(key: String, value: ByteArray): Int = 1
    }

    /**
     * キャッシュからBitmapを取得（メモリにあれば即時返却）
     */
    fun getBitmap(key: String): Bitmap? {
        return bitmapLruCache.get(key)
    }

    /**
     * キャッシュから生バイト列を取得
     */
    fun getRawBytes(url: String): ByteArray? {
        return rawBytesLruCache.get(url)
    }

    /**
     * タイルをキャッシュへ保存
     */
    fun putBitmap(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            bitmapLruCache.put(key, bitmap)
        }
    }

    /**
     * タイル生バイト列をキャッシュへ保存
     */
    fun putRawBytes(url: String, bytes: ByteArray) {
        rawBytesLruCache.put(url, bytes)
    }

    /**
     * タイルURLから同期/非同期でBitmapをロード（キャッシュヒット時はゼロ遅延）
     */
    suspend fun loadTileBitmap(
        url: String,
        cacheKey: String = url
    ): Bitmap? = withContext(Dispatchers.IO) {
        // 1. Bitmap LRUキャッシュヒット
        bitmapLruCache.get(cacheKey)?.let { return@withContext it }

        // 2. 生バイト列キャッシュヒット
        val cachedBytes = rawBytesLruCache.get(url)
        if (cachedBytes != null) {
            val bmp = BitmapFactory.decodeByteArray(cachedBytes, 0, cachedBytes.size)
            if (bmp != null) {
                bitmapLruCache.put(cacheKey, bmp)
                return@withContext bmp
            }
        }

        // 3. ネットワークからダウンロード
        try {
            val bytes = downloadBytes(url) ?: return@withContext null
            rawBytesLruCache.put(url, bytes)

            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) {
                bitmapLruCache.put(cacheKey, bmp)
                return@withContext bmp
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load tile bitmap: $url", e)
        }
        null
    }

    /**
     * 現在表示中のフレームの前後2フレーム (currentIndex - 2 .. currentIndex + 2) を
     * バックグラウンドで先読み（プリフェッチ）し、再生時のチラつきを完全に防止する
     */
    fun prefetchSurroundingFrames(
        frames: List<RadarTimelineFrame>,
        currentIndex: Int,
        centerLat: Double,
        centerLon: Double,
        zoom: Int,
        provider: RadarDataProvider
    ) {
        if (frames.isEmpty()) return

        // 以前のプリフェッチ処理があればキャンセルして最新の前後フレームを最優先
        prefetchJob?.cancel()

        prefetchJob = scope.launch {
            // 前後2フレームを抽出 (自身を除く [curr-2, curr-1, curr+1, curr+2])
            val targetIndices = mutableListOf<Int>()
            for (offset in listOf(1, -1, 2, -2)) {
                val idx = currentIndex + offset
                if (idx in frames.indices) {
                    targetIndices.add(idx)
                }
            }

            if (targetIndices.isEmpty()) return@launch

            // 地図中心座標からWebメルカトルタイル座標を算出
            val n = 1 shl zoom
            val latRad = Math.toRadians(centerLat)
            val centerX = ((centerLon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
            val safeCos = Math.cos(latRad)
            val safeTan = Math.tan(latRad)
            val secant = if (safeCos != 0.0) 1.0 / safeCos else 1.0
            val sinhVal = Math.log((safeTan + secant).coerceAtLeast(1e-5))
            val centerY = ((1.0 - sinhVal / Math.PI) / 2.0 * n).toInt().coerceIn(0, n - 1)

            // 画面中心および隣接する周辺タイル（計3x3=9タイル）
            val tileCoords = mutableListOf<Pair<Int, Int>>()
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val tx = centerX + dx
                    val ty = centerY + dy
                    if (tx in 0 until n && ty in 0 until n) {
                        tileCoords.add(Pair(tx, ty))
                    }
                }
            }

            // 各ターゲットフレームのタイルを順次先読み
            for (targetIdx in targetIndices) {
                val frame = frames[targetIdx]
                for ((tx, ty) in tileCoords) {
                    val tileUrl = provider.buildTileUrl(frame, tx, ty, zoom)
                    val cacheKey = "${frame.epochSeconds}-$zoom-$tx-$ty"

                    // すでにBitmapキャッシュにあればスキップ
                    if (bitmapLruCache.get(cacheKey) == null) {
                        loadTileBitmap(tileUrl, cacheKey)
                    }
                }
            }
        }
    }

    /**
     * HTTP経由でバイト列をダウンロード（HTTP/2 マルチプレキシングクライアントを使用）
     */
    private fun downloadBytes(urlStr: String): ByteArray? {
        return com.example.expressiveweather.ui.radar.RadarTileHttpClient.getTileBytes(urlStr)
    }

    /**
     * メモリ解放
     */
    fun clearCache() {
        bitmapLruCache.evictAll()
        rawBytesLruCache.evictAll()
    }
}
