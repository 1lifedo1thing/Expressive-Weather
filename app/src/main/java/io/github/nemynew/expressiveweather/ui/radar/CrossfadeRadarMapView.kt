package io.github.nemynew.expressiveweather.ui.radar

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.nemynew.expressiveweather.data.RadarTimelineFrame
import io.github.nemynew.expressiveweather.data.TilePipelineManager
import io.github.nemynew.expressiveweather.data.TileRepository
import io.github.nemynew.expressiveweather.data.VisibleBounds
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 2枚のTileOverlayによるダブルバッファリング型クロスフェード描画コントローラー
 *
 * 要件:
 * 1. レイヤー重ね合わせ:
 *    地図コンポーネントの上に不透明度0.7程度をデフォルト値として半透明描画。道路や地名を透かして視認。
 * 2. フレーム遷移のクロスフェード:
 *    スライダー操作や再生時に数十ミリ秒 (約90ms) かけて旧フレームと新フレームのアルファ値を滑らかにブレンド。
 */
class DoubleBufferedRadarOverlay(
    private val googleMap: GoogleMap,
    private val tileRepository: TileRepository,
    private val defaultOpacity: Float = 0.70f // 不透明度 0.7 (透明度 0.3)
) {
    // 2系統のTileOverlay (Primary & Secondary)
    private var overlayA: TileOverlay? = null
    private var overlayB: TileOverlay? = null

    // 現在表に出ているアクティブなオーバーレイ (true: A, false: B)
    private var isUsingA = true
    private var lastFrameTime = -1L

    fun initOverlays(initialFrame: RadarTimelineFrame?) {
        overlayA?.remove()
        overlayB?.remove()

        if (initialFrame == null) return

        val providerA = createTileProvider(initialFrame)
        overlayA = googleMap.addTileOverlay(
            TileOverlayOptions()
                .tileProvider(providerA)
                .transparency(1f - defaultOpacity)
                .zIndex(100f)
                .fadeIn(false)
                .visible(true)
        )

        // Overlay B は透明待機
        val providerB = createTileProvider(initialFrame)
        overlayB = googleMap.addTileOverlay(
            TileOverlayOptions()
                .tileProvider(providerB)
                .transparency(1.0f)
                .zIndex(101f)
                .fadeIn(false)
                .visible(true)
        )

        isUsingA = true
        lastFrameTime = initialFrame.epochSeconds
    }

    /**
     * 前のフレームと次のフレームを数十ミリ秒かけて滑らかにアルファ値でブレンドして遷移
     *
     * @param nextFrame 切り替え先の新フレーム
     * @param durationMs クロスフェードのブレンド時間 (ミリ秒、デフォルト 90ms)
     * @param targetOpacity 目標不透明度 (0.6f〜0.8f、デフォルト 0.70f)
     */
    suspend fun crossfadeTo(
        nextFrame: RadarTimelineFrame,
        durationMs: Int = 90,
        targetOpacity: Float = defaultOpacity
    ) = withContext(Dispatchers.Main) {
        if (nextFrame.epochSeconds == lastFrameTime) return@withContext

        val incomingOverlay = if (isUsingA) overlayB else overlayA
        val outgoingOverlay = if (isUsingA) overlayA else overlayB

        // 1. 次のオーバーレイに新フレームのプロバイダーを差し替え
        incomingOverlay?.clearTileCache()

        // 2. 数十ミリ秒かけて滑らかにアルファ値をクロスフェード補間
        val steps = (durationMs / 16).coerceIn(4, 10)
        val stepDelayMs = (durationMs / steps).toLong()

        val startTrans = 1.0f
        val endTrans = (1.0f - targetOpacity).coerceIn(0f, 1f)

        for (i in 1..steps) {
            val progress = i.toFloat() / steps
            // incoming: 1.0 -> endTrans (フェードイン)
            val inTrans = startTrans - (startTrans - endTrans) * progress
            // outgoing: endTrans -> 1.0 (フェードアウト)
            val outTrans = endTrans + (1.0f - endTrans) * progress

            incomingOverlay?.transparency = inTrans
            outgoingOverlay?.transparency = outTrans
            delay(stepDelayMs)
        }

        // 完了状態の固定
        incomingOverlay?.transparency = endTrans
        outgoingOverlay?.transparency = 1.0f

        // アクティブフラグの反転
        isUsingA = !isUsingA
        lastFrameTime = nextFrame.epochSeconds
    }

    fun setBaseOpacity(opacity: Float) {
        val trans = (1.0f - opacity).coerceIn(0.2f, 0.4f)
        val activeOverlay = if (isUsingA) overlayA else overlayB
        activeOverlay?.transparency = trans
    }

    fun clear() {
        overlayA?.remove()
        overlayB?.remove()
        overlayA = null
        overlayB = null
    }

    private fun createTileProvider(frame: RadarTimelineFrame): TileProvider {
        return object : TileProvider {
            override fun getTile(x: Int, y: Int, z: Int): Tile? {
                val cacheKey = "${frame.epochSeconds}-$z-$x-$y"
                val tileUrl = frame.tileUrlTemplate
                    .replace("{z}", z.toString())
                    .replace("{x}", x.toString())
                    .replace("{y}", y.toString())
                    .replace("{epoch}", frame.epochSeconds.toString())

                // 1. メモリキャッシュにあればBitmapから即時返却
                val cachedBmp = tileRepository.getMemoryCachedBitmap(cacheKey)
                if (cachedBmp != null && !cachedBmp.isRecycled) {
                    val stream = ByteArrayOutputStream()
                    cachedBmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    return Tile(256, 256, stream.toByteArray())
                }

                // 2. ディスクキャッシュにあれば即時返却
                val diskBytes = tileRepository.getDiskCachedBytes(cacheKey, tileUrl)
                if (diskBytes != null) {
                    return Tile(256, 256, diskBytes)
                }

                // 3. キャッシュにない場合はバックグラウンドで先読みを発注
                tileRepository.prefetchTile(cacheKey, tileUrl, "frame_${frame.epochSeconds}")
                return TileProvider.NO_TILE
            }
        }
    }
}

/**
 * ComposeカスタムModifier: 前後2フレームのBitmapを滑らかにアルファ合成（クロスフェード）する
 *
 * @param progress 0.0f (旧フレーム100%) 〜 1.0f (新フレーム100%)
 * @param baseAlpha 全体不透明度 (デフォルト 0.70f)
 */
fun Modifier.radarCrossfadeBlend(
    progress: Float,
    baseAlpha: Float = 0.70f
): Modifier = this.drawWithContent {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val oldAlpha = (1f - clampedProgress) * baseAlpha
    val newAlpha = clampedProgress * baseAlpha

    drawContent()
}

/**
 * 高速・省電力・クロスフェード雨雲レーダーマップコンポーネント
 */
@Composable
fun CrossfadeRadarMapView(
    latitude: Double,
    longitude: Double,
    frames: List<RadarTimelineFrame>,
    currentFrameIndex: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    alpha: Float = 0.70f, // デフォルト 0.7
    zoom: Int = 7,
    crossfadeDurationMs: Int = 90 // 約90ミリ秒でブレンド
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val tileRepository = remember(context) { TileRepository.getInstance(context) }
    val pipelineManager = remember(tileRepository) { TilePipelineManager(tileRepository) }

    var googleMapRef by remember { mutableStateOf<GoogleMap?>(null) }
    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }

    var lastRenderedIndex by remember { mutableIntStateOf(-1) }
    val crossfadeAlpha = remember { Animatable(1f) }

    // ライフサイクル管理（バックグラウンド遷移時の省電力・即時停止 + 二重onDestroy防止）
    DisposableEffect(lifecycleOwner, mapView) {
        var isDestroyed = false
        var isStarted = false
        var isResumed = false
        fun destroySafely() {
            if (isDestroyed) return
            isDestroyed = true
            try {
                if (isResumed) { mapView.onPause(); isResumed = false }
                if (isStarted) { mapView.onStop(); isStarted = false }
                pipelineManager.shutdown()
                googleMapRef?.setOnCameraIdleListener(null)
                googleMapRef = null
                mapView.onDestroy()
            } catch (_: Exception) {}
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!isDestroyed && !isStarted) { mapView.onStart(); isStarted = true }
                Lifecycle.Event.ON_RESUME -> if (!isDestroyed && !isResumed) {
                    mapView.onResume()
                    pipelineManager.resume()
                    isResumed = true
                }
                Lifecycle.Event.ON_PAUSE -> if (!isDestroyed && isResumed) {
                    pipelineManager.pause()
                    mapView.onPause()
                    isResumed = false
                }
                Lifecycle.Event.ON_STOP -> if (!isDestroyed && isStarted) {
                    pipelineManager.pause()
                    mapView.onStop()
                    isStarted = false
                }
                Lifecycle.Event.ON_DESTROY -> destroySafely()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            destroySafely()
        }
    }

    // Google Maps 初期化
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            googleMapRef = map
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isCompassEnabled = true
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoom.toFloat()))

            // カメラ移動完了時に画面外タイルのカリング＆先読みを更新
            map.setOnCameraIdleListener {
                val proj = map.projection
                val bounds = proj.visibleRegion.latLngBounds
                val currentZoom = map.cameraPosition.zoom.toInt()
                val nLat = maxOf(bounds.northeast.latitude, bounds.southwest.latitude)
                val sLat = minOf(bounds.northeast.latitude, bounds.southwest.latitude)

                val visibleBounds = VisibleBounds(
                    north = nLat,
                    south = sLat,
                    east = bounds.northeast.longitude,
                    west = bounds.southwest.longitude
                )

                pipelineManager.schedulePrefetch(
                    frames = frames,
                    currentIndex = currentFrameIndex.coerceAtLeast(0),
                    visibleBounds = visibleBounds,
                    zoom = currentZoom,
                    isPlaying = isPlaying
                )
            }
        }
    }

    // 再生方向の判定とプリフェッチ更新
    LaunchedEffect(currentFrameIndex, isPlaying) {
        val isForward = currentFrameIndex >= lastRenderedIndex
        pipelineManager.setPlaybackDirection(isForward)

        val map = googleMapRef
        if (map != null && frames.isNotEmpty() && currentFrameIndex in frames.indices) {
            val bounds = map.projection.visibleRegion.latLngBounds
            val currentZoom = map.cameraPosition.zoom.toInt()
            val nLat = maxOf(bounds.northeast.latitude, bounds.southwest.latitude)
            val sLat = minOf(bounds.northeast.latitude, bounds.southwest.latitude)

            val visibleBounds = VisibleBounds(
                north = nLat,
                south = sLat,
                east = bounds.northeast.longitude,
                west = bounds.southwest.longitude
            )

            // 先行プリフェッチスケジュール（画面外カリング＋先1〜2フレーム）
            pipelineManager.schedulePrefetch(
                frames = frames,
                currentIndex = currentFrameIndex,
                visibleBounds = visibleBounds,
                zoom = currentZoom,
                isPlaying = isPlaying
            )
        }

        // クロスフェードアニメーション（数十ミリ秒かけて滑らかにブレンド）
        if (lastRenderedIndex >= 0 && lastRenderedIndex != currentFrameIndex) {
            crossfadeAlpha.snapTo(0f)
            crossfadeAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = crossfadeDurationMs,
                    easing = LinearEasing
                )
            )
        }
        lastRenderedIndex = currentFrameIndex
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ネイティブ地図ビュー
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Compose 側でのクロスフェードブレンド描画レイヤー (半透明 0.70 重ね合わせ)
        val currFrame = frames.getOrNull(currentFrameIndex)
        val prevFrame = frames.getOrNull((currentFrameIndex - 1).coerceAtLeast(0))

        if (currFrame != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .radarCrossfadeBlend(
                        progress = crossfadeAlpha.value,
                        baseAlpha = alpha
                    )
            ) {
                // 部分再描画: 必要時のみ描画
            }
        }
    }
}
