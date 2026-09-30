package com.example.expressiveweather.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 地図の表示境界矩形 (WGS84 緯度経度)
 */
data class VisibleBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
) {
    init {
        require(north >= south) { "North must be >= South" }
    }
}

/**
 * 画面内外タイルのカリング判定と、再生方向に応じた先読み（プリフェッチ）を司るパイプラインマネージャー
 *
 * 要件:
 * 1. 再生方向に応じた先読み:
 *    正方向再生中は +1, +2 先のフレームを先行取得。逆方向再生中は -1, -2 を先行取得。
 * 2. 画面外タイルのカリング:
 *    現在画面に表示されている地図の境界座標とズームレベルに合致するタイルのみをフェッチ対象とし、画面外を除外。
 * 3. 即時キャンセル制御:
 *    スライダー操作時に通り過ぎた過去のフレームに対するJobを即座にcancelし、最新位置を最優先実行。
 * 4. 省電力:
 *    バックグラウンド遷移時に全処理を即座に停止。
 */
class TilePipelineManager(
    private val tileRepository: TileRepository = TileRepository.getInstance()
) {
    companion object {
        private const val TAG = "TilePipelineManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var prefetchPipelineJob: Job? = null
    private val isPaused = AtomicBoolean(false)

    // 現在の再生方向: true=正方向 (+1), false=逆方向 (-1)
    private var isPlayingForward = true

    /**
     * 再生方向の更新 (再生開始時や順方向/逆方向切り替え時に呼び出し)
     */
    fun setPlaybackDirection(isForward: Boolean) {
        isPlayingForward = isForward
    }

    /**
     * 画面が一時停止/バックグラウンドに入った際の即時省電力停止
     */
    fun pause() {
        isPaused.set(true)
        prefetchPipelineJob?.cancel()
        tileRepository.cancelAllRequests()
        Log.d(TAG, "TilePipelineManager paused, all in-flight jobs cancelled.")
    }

    /**
     * 画面復帰時の再開
     */
    fun resume() {
        isPaused.set(false)
        Log.d(TAG, "TilePipelineManager resumed.")
    }

    /**
     * 画面内外判定（カリング）を行い、再生方向に応じた先1〜2フレームの可視タイルを先読み
     *
     * @param frames 全タイムラインフレーム
     * @param currentIndex 現在表示中のフレームインデックス
     * @param visibleBounds 現在画面に表示されている地図の緯度経度境界
     * @param zoom 現在のズームレベル
     * @param isPlaying 自動再生中かどうか
     */
    fun schedulePrefetch(
        frames: List<RadarTimelineFrame>,
        currentIndex: Int,
        visibleBounds: VisibleBounds,
        zoom: Int,
        isPlaying: Boolean = false
    ) {
        if (isPaused.get() || frames.isEmpty()) return

        // 1. 直前のプリフェッチJobをキャンセル（素早いスライダー操作への即応）
        prefetchPipelineJob?.cancel()

        // 2. 先読み対象フレームのインデックスを決定
        val targetIndices = determinePrefetchIndices(
            totalFrames = frames.size,
            currentIndex = currentIndex,
            isPlaying = isPlaying,
            isForward = isPlayingForward
        )

        if (targetIndices.isEmpty()) return

        // 保持すべきタグ一覧（現在のフレーム ＋ 先読み対象フレーム）
        val neededTags = (listOf(currentIndex) + targetIndices)
            .map { "frame_${frames[it].epochSeconds}" }
            .toSet()

        // 3. 通り過ぎた不要なフレームの通信・デコードJobを即座にキャンセル
        tileRepository.cancelRequestsExcept(neededTags)

        // 4. 画面内の可視タイル座標矩形を算出 (カリング)
        val visibleTileCoords = calculateVisibleTileCoordinates(visibleBounds, zoom)
        if (visibleTileCoords.isEmpty()) return

        // 5. バックグラウンドコルーチンで先行取得・デコードを開始
        prefetchPipelineJob = scope.launch {
            for (targetIdx in targetIndices) {
                if (isPaused.get()) break
                val frame = frames[targetIdx]
                val tag = "frame_${frame.epochSeconds}"

                for ((tx, ty) in visibleTileCoords) {
                    if (isPaused.get()) break
                    val tileUrl = frame.tileUrlTemplate
                        .replace("{z}", zoom.toString())
                        .replace("{x}", tx.toString())
                        .replace("{y}", ty.toString())
                        .replace("{epoch}", frame.epochSeconds.toString())

                    val cacheKey = "${frame.epochSeconds}-$zoom-$tx-$ty"
                    tileRepository.prefetchTile(cacheKey, tileUrl, tag)
                }
            }
        }
    }

    /**
     * 再生状態および方向に応じた先読み対象フレームの算出
     * - 再生中: 再生方向に応じて先1〜2フレーム (+1, +2 または -1, -2)
     * - 停止中: 直近の前後フレーム (+1, -1)
     */
    fun determinePrefetchIndices(
        totalFrames: Int,
        currentIndex: Int,
        isPlaying: Boolean,
        isForward: Boolean
    ): List<Int> {
        if (totalFrames <= 1) return emptyList()

        val offsets = if (isPlaying) {
            if (isForward) listOf(1, 2) else listOf(-1, -2)
        } else {
            listOf(1, -1)
        }

        val result = mutableListOf<Int>()
        for (offset in offsets) {
            val candidate = (currentIndex + offset).mod(totalFrames)
            if (candidate != currentIndex && candidate !in result) {
                result.add(candidate)
            }
        }
        return result
    }

    /**
     * 表示中の地図境界とズームレベルから、可視領域（画面内）のタイル座標リストを計算 (画面外カリング)
     */
    fun calculateVisibleTileCoordinates(
        bounds: VisibleBounds,
        zoom: Int
    ): List<Pair<Int, Int>> {
        val n = 1 shl zoom

        val minX = lonToTileX(bounds.west, zoom)
        val maxX = lonToTileX(bounds.east, zoom)
        val minY = latToTileY(bounds.north, zoom) // 北側の方がYが小さい
        val maxY = latToTileY(bounds.south, zoom) // 南側の方がYが大きい

        val coords = mutableListOf<Pair<Int, Int>>()

        val xRange = if (minX <= maxX) {
            (minX..maxX).toList()
        } else {
            // 経度180度（日付変更線）またぎ
            (minX until n) + (0..maxX)
        }

        val yMinClamped = minY.coerceIn(0, n - 1)
        val yMaxClamped = maxY.coerceIn(0, n - 1)

        for (x in xRange) {
            for (y in yMinClamped..yMaxClamped) {
                coords.add(Pair(x, y))
            }
        }

        // 過剰なタイル数（極小ズームで全地球など）の場合は最大64タイルに制限し安全性を確保
        return coords.take(64)
    }

    private fun lonToTileX(lon: Double, zoom: Int): Int {
        val n = 1 shl zoom
        val normLon = ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        return ((normLon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    }

    private fun latToTileY(lat: Double, zoom: Int): Int {
        val n = 1 shl zoom
        val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
        val latRad = Math.toRadians(clampedLat)
        val safeTan = Math.tan(latRad)
        val safeCos = Math.cos(latRad)
        val secant = if (safeCos != 0.0) 1.0 / safeCos else 1.0
        val sinhVal = Math.log((safeTan + secant).coerceAtLeast(1e-5))
        return ((1.0 - sinhVal / Math.PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
    }
}
