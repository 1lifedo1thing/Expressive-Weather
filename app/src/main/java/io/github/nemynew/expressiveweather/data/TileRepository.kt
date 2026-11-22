package io.github.nemynew.expressiveweather.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.collection.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 雨雲レーダータイルの高機能二重キャッシュリポジトリ (メモリキャッシュ + ディスクキャッシュ)
 *
 * 要件:
 * 1. メモリキャッシュ: 端末の最大ヒープ容量の15%を上限とし、LruCacheで古いBitmapタイルを自動解放
 * 2. ディスクキャッシュ: ローカルストレージに永続化し、同一時刻・座標タイルのネットワーク再取得を完全に遮断
 * 3. 即時キャンセル制御: 通り過ぎた時刻フレームの通信・デコードJobを即座にcancel
 */
class TileRepository(
    private val diskCacheDir: File?,
    maxMemoryOverrideKb: Int? = null
) {
    companion object {
        private const val TAG = "TileRepository"

        @Volatile
        private var INSTANCE: TileRepository? = null

        fun getInstance(context: Context? = null): TileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val cacheDir = context?.applicationContext?.cacheDir?.let {
                        File(it, "radar_tiles_v2").apply { mkdirs() }
                    }
                    TileRepository(cacheDir).also { INSTANCE = it }
                }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 1. メモリキャッシュ上限: 端末の最大ヒープ容量の15% (またはオーバーライド指定)
    val maxHeapKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val maxMemoryCacheKb = maxMemoryOverrideKb ?: (maxHeapKb * 0.15).toInt().coerceAtLeast(8 * 1024)

    // 統計メトリクス
    private val _hitCount = AtomicInteger(0)
    private val _missCount = AtomicInteger(0)
    private val _evictionCount = AtomicInteger(0)

    val hitCount: Int get() = _hitCount.get()
    val missCount: Int get() = _missCount.get()
    val evictionCount: Int get() = _evictionCount.get()

data class CachedTile(
    val bitmap: Bitmap?,
    val sizeKb: Int
)

    /**
     * デコード済みBitmapタイルのLRUキャッシュ (ヒープ最大15%上限)
     */
    private val memoryCache = object : LruCache<String, CachedTile>(maxMemoryCacheKb) {
        override fun sizeOf(key: String, value: CachedTile): Int {
            return value.sizeKb.coerceAtLeast(1)
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: CachedTile, newValue: CachedTile?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            if (evicted) {
                _evictionCount.incrementAndGet()
                Log.v(TAG, "Tile evicted from memory cache: $key (Current size: ${size()}KB / Max: ${maxSize()}KB)")
            }
        }
    }

    // 進行中のリクエスト管理 (タグ付けによりフレーム単位で一括キャンセル可能)
    private val activeJobsByTag = ConcurrentHashMap<String, MutableSet<Job>>()
    private val inFlightMutex = Mutex()
    private val inFlightUrls = ConcurrentHashMap<String, Job>()

    /**
     * 現在のメモリキャッシュ使用量 (KB)
     */
    fun getMemoryUsageKb(): Int = memoryCache.size()

    /**
     * メモリキャッシュ内のエントリ数
     */
    fun getMemoryEntryCount(): Int = memoryCache.snapshot().size

    /**
     * テストおよび検証用: 指定したキーとサイズ(KB)のタイルを直接メモリキャッシュに注入
     */
    fun putTileForTesting(key: String, sizeKb: Int, bitmap: Bitmap? = null) {
        memoryCache.put(key, CachedTile(bitmap, sizeKb))
    }

    /**
     * メモリキャッシュに特定のキーが存在するか確認
     */
    fun hasMemoryCache(key: String): Boolean {
        return memoryCache.get(key) != null
    }

    /**
     * メモリキャッシュから同期的にBitmapを取得（存在すれば0msで即返却）
     */
    fun getMemoryCachedBitmap(cacheKey: String): Bitmap? {
        val cached = memoryCache.get(cacheKey)
        val bmp = cached?.bitmap
        return if (bmp != null && !bmp.isRecycled) {
            _hitCount.incrementAndGet()
            bmp
        } else {
            null
        }
    }

    /**
     * ディスクキャッシュから同期的に生バイト列を取得
     */
    fun getDiskCachedBytes(cacheKey: String, url: String): ByteArray? {
        val file = getDiskCacheFile(cacheKey, url)
        return if (file != null && file.exists() && file.length() > 0) {
            try {
                file.readBytes()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * タイルBitmapの取得 (1. メモリキャッシュ -> 2. ディスクキャッシュ -> 3. ネットワーク)
     *
     * @param cacheKey キャッシュ特定キー (例: "$frameTime-$zoom-$x-$y")
     * @param url タイル取得URL
     * @param tag キャンセル制御用のフレームタグ (例: "frame_$frameTime")
     */
    suspend fun getTileBitmap(
        cacheKey: String,
        url: String,
        tag: String? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        // 1. メモリキャッシュチェック
        memoryCache.get(cacheKey)?.let { cachedTile ->
            val cachedBitmap = cachedTile.bitmap
            if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                _hitCount.incrementAndGet()
                return@withContext cachedBitmap
            } else {
                memoryCache.remove(cacheKey)
            }
        }

        _missCount.incrementAndGet()

        // 2. ディスクキャッシュチェック
        val diskFile = getDiskCacheFile(cacheKey, url)
        if (diskFile != null && diskFile.exists() && diskFile.length() > 0) {
            try {
                val bytes = diskFile.readBytes()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val sizeKb = (bitmap.byteCount / 1024).coerceAtLeast(1)
                    memoryCache.put(cacheKey, CachedTile(bitmap, sizeKb))
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                diskFile.delete() // 破損ファイルは削除
            }
        }

        // 3. ネットワークフェッチ
        val bytes = downloadTileBytesWithCancellation(url, tag) ?: return@withContext null

        // ディスクキャッシュへ保存
        saveToDisk(diskFile, bytes)

        // Bitmapデコードしてメモリキャッシュへ保存
        val bitmap = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }

        if (bitmap != null) {
            val sizeKb = (bitmap.byteCount / 1024).coerceAtLeast(1)
            memoryCache.put(cacheKey, CachedTile(bitmap, sizeKb))
        }

        bitmap
    }

    /**
     * バックグラウンド用プリフェッチ (メモリ・ディスク両方に先読み格納)
     */
    fun prefetchTile(
        cacheKey: String,
        url: String,
        tag: String
    ): Job {
        val job = scope.launch {
            try {
                getTileBitmap(cacheKey, url, tag)
            } catch (_: CancellationException) {
                // キャンセル時は正常終了
            } catch (e: Exception) {
                Log.w(TAG, "Prefetch failed for $cacheKey: ${e.message}")
            }
        }

        // タグごとにJobを登録
        val set = activeJobsByTag.computeIfAbsent(tag) { ConcurrentHashMap.newKeySet() }
        set.add(job)
        job.invokeOnCompletion { set.remove(job) }

        return job
    }

    /**
     * 特定のタグ (時刻フレームなど) に紐づく進行中リクエストを即座にキャンセル
     */
    fun cancelRequestsForTag(tag: String) {
        val jobs = activeJobsByTag.remove(tag)
        if (jobs != null && jobs.isNotEmpty()) {
            val count = jobs.size
            jobs.forEach { it.cancel() }
            Log.d(TAG, "Cancelled $count tile jobs for tag: $tag")
        }
    }

    /**
     * 指定タグ以外の古いリクエストを一括キャンセル
     */
    fun cancelRequestsExcept(keepTags: Set<String>) {
        val keysToRemove = activeJobsByTag.keys.filter { it !in keepTags }
        for (tag in keysToRemove) {
            cancelRequestsForTag(tag)
        }
    }

    /**
     * すべてのアクティブなリクエストをキャンセル
     */
    fun cancelAllRequests() {
        activeJobsByTag.values.forEach { jobs ->
            jobs.forEach { it.cancel() }
        }
        activeJobsByTag.clear()
        inFlightUrls.values.forEach { it.cancel() }
        inFlightUrls.clear()
    }

    /**
     * メモリキャッシュをクリア
     */
    fun clearMemoryCache() {
        memoryCache.evictAll()
    }

    /**
     * ディスクキャッシュを全消去
     */
    fun clearDiskCache() {
        diskCacheDir?.listFiles()?.forEach { it.delete() }
    }

    /**
     * キャッシュ統計をリセット
     */
    fun resetStats() {
        _hitCount.set(0)
        _missCount.set(0)
        _evictionCount.set(0)
    }

    private suspend fun downloadTileBytesWithCancellation(
        urlStr: String,
        tag: String?
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            io.github.nemynew.expressiveweather.ui.radar.RadarTileHttpClient.getTileBytesCancellable(urlStr)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun getDiskCacheFile(cacheKey: String, url: String): File? {
        val dir = diskCacheDir ?: return null
        val safeFileName = hashString(cacheKey.ifBlank { url }) + ".tile"
        return File(dir, safeFileName)
    }

    private fun saveToDisk(file: File?, bytes: ByteArray) {
        if (file == null) return
        val tmpFile = File(file.parentFile, "${file.name}.${System.nanoTime()}.tmp")
        try {
            tmpFile.writeBytes(bytes)
            if (!tmpFile.renameTo(file)) {
                file.writeBytes(bytes)
            }
        } catch (_: Exception) {
            // ディスク書き込み失敗時は無視
        } finally {
            if (tmpFile.exists()) tmpFile.delete()
        }
    }

    private fun hashString(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digested = md.digest(input.toByteArray())
            digested.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toString()
        }
    }
}
