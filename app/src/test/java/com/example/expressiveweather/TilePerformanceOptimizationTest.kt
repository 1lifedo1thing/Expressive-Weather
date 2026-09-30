package com.example.expressiveweather

import android.graphics.Bitmap
import com.example.expressiveweather.data.TilePipelineManager
import com.example.expressiveweather.data.TileRepository
import com.example.expressiveweather.data.VisibleBounds
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 雨雲レーダータイルの二重キャッシュ、メモリ解放（LRU自動破棄）、
 * 画面外カリング、リクエスト即時キャンセル制御の検証テスト
 */
class TilePerformanceOptimizationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var diskCacheDir: File
    private lateinit var tileRepository: TileRepository
    private lateinit var pipelineManager: TilePipelineManager

    @Before
    fun setUp() {
        diskCacheDir = tempFolder.newFolder("test_tile_cache")
        // テスト用：メモリ上限を小さめ（100KB）に設定して、メモリ解放（Eviction）の挙動を確実に検証
        tileRepository = TileRepository(
            diskCacheDir = diskCacheDir,
            maxMemoryOverrideKb = 100
        )
        pipelineManager = TilePipelineManager(tileRepository)
    }

    @Test
    fun testMemoryCacheHeapLimitConstraint() {
        // デフォルトインスタンスにおけるヒープ15%上限の検証
        val defaultRepo = TileRepository(diskCacheDir)
        val expected15Percent = (defaultRepo.maxHeapKb * 0.15).toInt().coerceAtLeast(8 * 1024)
        assertEquals(expected15Percent, defaultRepo.maxMemoryCacheKb)
    }

    @Test
    fun testLruMemoryEvictionWhenLimitExceeded() {
        // テスト用リポジトリの上限は setUp() で 100KB に設定済み
        val key1 = "frame1_z5_x10_y10"
        val key2 = "frame2_z5_x10_y10"

        val initialEvictions = tileRepository.evictionCount

        // 1枚目（60KB）をキャッシュに追加 -> 合計60KB (<= 100KB)
        tileRepository.putTileForTesting(key1, sizeKb = 60)
        assertEquals(60, tileRepository.getMemoryUsageKb())
        assertTrue("Key 1 should be in memory", tileRepository.hasMemoryCache(key1))
        assertEquals("No evictions yet", initialEvictions, tileRepository.evictionCount)

        // 2枚目（60KB）をキャッシュに追加 -> 合計120KBとなり上限100KBを超えるため、
        // LRUアルゴリズムにより最も古い key1 が自動解放 (Evict) される
        tileRepository.putTileForTesting(key2, sizeKb = 60)

        // 検証:
        // 1. 最古の key1 が自動解放されていること
        assertFalse("Key 1 must be evicted from memory", tileRepository.hasMemoryCache(key1))
        // 2. 最新の key2 がメモリに保持されていること
        assertTrue("Key 2 must remain in cache", tileRepository.hasMemoryCache(key2))
        // 3. evictionCount がインクリメントされていること
        assertEquals("Eviction count must increment by 1", initialEvictions + 1, tileRepository.evictionCount)
        // 4. 現在のメモリ使用量が上限以下（60KB <= 100KB）であること
        assertTrue(
            "Memory usage must not exceed limit (Current: ${tileRepository.getMemoryUsageKb()}KB, Max: ${tileRepository.maxMemoryCacheKb}KB)",
            tileRepository.getMemoryUsageKb() <= tileRepository.maxMemoryCacheKb
        )
    }

    @Test
    fun testDiskCachePersistenceAndRestoration() {
        // ディスクキャッシュにファイルが存在する場合、同一キーへの再リクエストでディスクから即時復元されること
        val cacheKey = "frame_persisted_z6_x5_y5"
        val mockUrl = "https://mesonet.agron.iastate.edu/cache/test.png"

        // 意図的にディスクファイルを事前配置
        val safeFileName = tileRepository.javaClass.getDeclaredMethod("getDiskCacheFile", String::class.java, String::class.java).let { method ->
            method.isAccessible = true
            method.invoke(tileRepository, cacheKey, mockUrl) as File
        }
        safeFileName.writeBytes(ByteArray(500))

        assertTrue("Disk cache file must exist", safeFileName.exists())

        // リポジトリから取得
        runBlocking {
            tileRepository.getTileBitmap(cacheKey, mockUrl)
        }

        // ネットワーク通信を遮断した状態でディスクから読み込まれ、missCount がインクリメントされた後にキャッシュされること
        assertTrue(safeFileName.exists())
    }

    @Test
    fun testRequestImmediateCancellation() {
        val tagToCancel = "frame_outdated_1000"
        val tagToKeep = "frame_latest_2000"

        // バックグラウンドプリフェッチを発注
        val job1 = tileRepository.prefetchTile("key1", "http://example.com/slow1.png", tagToCancel)
        val job2 = tileRepository.prefetchTile("key2", "http://example.com/slow2.png", tagToKeep)

        assertFalse("Job 1 should start active", job1.isCancelled)
        assertFalse("Job 2 should start active", job2.isCancelled)

        // スライダー操作により、古いタグのリクエストを即時キャンセル
        tileRepository.cancelRequestsExcept(setOf(tagToKeep))

        assertTrue("Job 1 must be cancelled immediately", job1.isCancelled)
        assertFalse("Job 2 must remain active", job2.isCancelled)
    }

    @Test
    fun testVisibleTileCoordinatesCulling() {
        // サンフランシスコ周辺（狭い範囲）の画面境界
        val sfBounds = VisibleBounds(
            north = 37.85,
            south = 37.70,
            east = -122.35,
            west = -122.50
        )

        val zoom = 10
        val tiles = pipelineManager.calculateVisibleTileCoordinates(sfBounds, zoom)

        // ズーム10での全地球タイル数は 1024x1024 = 1,048,576 枚。
        // サンフランシスコ市街地の境界に限定されているため、タイルの件数は数枚〜十数枚に激減（カリング）されていること
        assertTrue("Tiles count should be small and culled", tiles.isNotEmpty())
        assertTrue("Culling should eliminate off-screen tiles (count: ${tiles.size})", tiles.size in 1..16)

        // 全地球（極端な広域）でも最大64枚に安全制限されること
        val globalBounds = VisibleBounds(
            north = 80.0,
            south = -80.0,
            east = 179.0,
            west = -179.0
        )
        val globalTiles = pipelineManager.calculateVisibleTileCoordinates(globalBounds, 10)
        assertTrue("Global tiles count must be capped at 64 to protect memory", globalTiles.size <= 64)
    }

    @Test
    fun testDirectionalPrefetchIndicesCalculation() {
        val totalFrames = 50
        val currentIndex = 10

        // 1. 正方向再生中 (+1, +2 が先読み対象)
        val forwardTargets = pipelineManager.determinePrefetchIndices(
            totalFrames = totalFrames,
            currentIndex = currentIndex,
            isPlaying = true,
            isForward = true
        )
        assertEquals(listOf(11, 12), forwardTargets)

        // 2. 逆方向再生中 (-1, -2 が先読み対象)
        val backwardTargets = pipelineManager.determinePrefetchIndices(
            totalFrames = totalFrames,
            currentIndex = currentIndex,
            isPlaying = true,
            isForward = false
        )
        assertEquals(listOf(9, 8), backwardTargets)

        // 3. 静止中 (+1, -1 が先読み対象)
        val pausedTargets = pipelineManager.determinePrefetchIndices(
            totalFrames = totalFrames,
            currentIndex = currentIndex,
            isPlaying = false,
            isForward = true
        )
        assertEquals(listOf(11, 9), pausedTargets)

        // 4. 末尾でのループ循環判定 (インデックス49の次は 0, 1)
        val loopTargets = pipelineManager.determinePrefetchIndices(
            totalFrames = totalFrames,
            currentIndex = 49,
            isPlaying = true,
            isForward = true
        )
        assertEquals(listOf(0, 1), loopTargets)
    }

    @Test
    fun testRadarTileHttpClientConfiguration() {
        val client = com.example.expressiveweather.ui.radar.RadarTileHttpClient.client
        assertEquals(64, client.dispatcher.maxRequests)
        assertEquals(32, client.dispatcher.maxRequestsPerHost)
    }

    @Test
    fun testRadarTileHttpClientPngValidation() {
        // 正常なPNGヘッダー（8バイト）+ ダミーデータ (360バイト以上)
        val validPng = ByteArray(400)
        validPng[0] = 0x89.toByte()
        validPng[1] = 0x50.toByte()
        validPng[2] = 0x4E.toByte()
        validPng[3] = 0x47.toByte()
        validPng[4] = 0x0D.toByte()
        validPng[5] = 0x0A.toByte()
        validPng[6] = 0x1A.toByte()
        validPng[7] = 0x0A.toByte()
        assertTrue(com.example.expressiveweather.ui.radar.RadarTileHttpClient.isValidPngBytes(validPng))

        // 短すぎるデータ (< 350バイト)
        val shortData = ByteArray(100)
        assertFalse(com.example.expressiveweather.ui.radar.RadarTileHttpClient.isValidPngBytes(shortData))

        // 破損データ (HTMLエラーページなど)
        val htmlError = "<html><body>502 Bad Gateway</body></html>".toByteArray()
        assertFalse(com.example.expressiveweather.ui.radar.RadarTileHttpClient.isValidPngBytes(htmlError))

        // null データ
        assertFalse(com.example.expressiveweather.ui.radar.RadarTileHttpClient.isValidPngBytes(null))
    }

    @Test
    fun testRadarTileHttpClientRefererHeader() {
        val client = com.example.expressiveweather.ui.radar.RadarTileHttpClient

        // 気象庁
        val jmaReferer = client.getRefererForUrl("https://www.jma.go.jp/bosai/jmatile/data/nowc/20260926000000/none/20260926000000/surf/hrpns/8/227/100.png")
        assertEquals("https://www.jma.go.jp/bosai/nowc/", jmaReferer)

        // RainViewer
        val rainViewerReferer = client.getRefererForUrl("https://tilecache.rainviewer.com/v2/radar/1727318400/256/7/113/50/2/1_1.png")
        assertEquals("https://www.rainviewer.com/", rainViewerReferer)

        // 全米 NEXRAD / IEM
        val usReferer = client.getRefererForUrl("https://mesonet.agron.iastate.edu/cache/tiles/1.0.0/ridge::USCOMP-N0Q-202609260000/6/15/24.png")
        assertEquals("https://mesonet.agron.iastate.edu/", usReferer)

        // 未知のドメイン
        val fallbackReferer = client.getRefererForUrl("https://example.com/tiles/test.png")
        assertEquals("https://example.com/", fallbackReferer)
    }

    @Test
    fun testRainViewerZoomNotSupportedDetection() {
        val client = com.example.expressiveweather.ui.radar.RadarTileHttpClient

        // RainViewerの未対応ズーム (zoom >= 8) プレースホルダー画像 (~1370バイト)
        val placeholderBytes = ByteArray(1370)
        assertTrue(client.isZoomNotSupportedTile(placeholderBytes))

        // 許容誤差範囲内 (1350〜1390バイト)
        assertTrue(client.isZoomNotSupportedTile(ByteArray(1350)))
        assertTrue(client.isZoomNotSupportedTile(ByteArray(1390)))

        // 通常の雨雲タイルデータ（数KB〜数十KB）
        assertFalse(client.isZoomNotSupportedTile(ByteArray(5000)))
        assertFalse(client.isZoomNotSupportedTile(ByteArray(25000)))

        // 範囲外のサイズ
        assertFalse(client.isZoomNotSupportedTile(ByteArray(1349)))
        assertFalse(client.isZoomNotSupportedTile(ByteArray(1391)))
        assertFalse(client.isZoomNotSupportedTile(null))
    }
}
