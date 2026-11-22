package io.github.nemynew.expressiveweather.ui.radar

import android.content.Context
import io.github.nemynew.expressiveweather.data.JmaTyphoonItem
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.RadarColorScheme
import io.github.nemynew.expressiveweather.data.RadarFrame
import io.github.nemynew.expressiveweather.theme.LocalDarkTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * レーダータイルのローカルディスクキャッシュ。
 * メモリキャッシュから溢れたタイルをミリ秒単位で高速復元し、ネットワーク待ちを根絶
 */
private object TileDiskCache {
    private const val MAX_CACHE_BYTES = 64L * 1024L * 1024L
    private var cacheDir: java.io.File? = null
    private var cachedTotalBytes: Long = -1L

    @Synchronized
    fun init(context: Context) {
        if (cacheDir == null) {
            try {
                val dir = java.io.File(context.applicationContext.cacheDir, "radar_tile_cache").apply { mkdirs() }
                cacheDir = dir
                cachedTotalBytes = dir.listFiles()
                    ?.filter { it.isFile && !it.name.endsWith(".tmp") }
                    ?.sumOf { it.length() }
                    ?: 0L
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun hashKey(key: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(key.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            key.hashCode().toString()
        }
    }

    @Synchronized
    fun get(key: String): ByteArray? {
        val dir = cacheDir ?: return null
        return try {
            val file = java.io.File(dir, hashKey(key))
            if (file.exists() && file.length() > 0L) {
                file.setLastModified(System.currentTimeMillis())
                file.readBytes()
            } else {
                if (file.exists()) file.delete()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun put(key: String, bytes: ByteArray) {
        val dir = cacheDir ?: return
        if (bytes.isEmpty()) return
        try {
            val file = java.io.File(dir, hashKey(key))
            val previousSize = if (file.exists()) file.length() else 0L
            val temporaryFile = java.io.File(dir, "${file.name}.tmp")
            temporaryFile.writeBytes(bytes)
            file.delete()
            if (!temporaryFile.renameTo(file)) {
                temporaryFile.delete()
                file.writeBytes(bytes)
            }
            if (cachedTotalBytes < 0L) {
                trimToSize(dir)
            } else {
                cachedTotalBytes = (cachedTotalBytes - previousSize + bytes.size.toLong()).coerceAtLeast(0L)
                if (cachedTotalBytes > MAX_CACHE_BYTES) {
                    trimToSize(dir)
                }
            }
        } catch (e: Exception) {
            // Ignore disk write errors
        }
    }

    private fun trimToSize(dir: java.io.File) {
        val files = dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") } ?: return
        var totalBytes = files.sumOf { it.length() }
        cachedTotalBytes = totalBytes
        if (totalBytes <= MAX_CACHE_BYTES) return

        for (file in files.sortedBy { it.lastModified() }) {
            if (totalBytes <= MAX_CACHE_BYTES) break
            val fileSize = file.length()
            if (file.delete()) totalBytes -= fileSize
        }
        cachedTotalBytes = totalBytes
    }

    @Synchronized
    fun remove(key: String) {
        val dir = cacheDir ?: return
        try {
            val file = java.io.File(dir, hashKey(key))
            val len = if (file.exists()) file.length() else 0L
            if (file.delete() && cachedTotalBytes >= 0L) {
                cachedTotalBytes = (cachedTotalBytes - len).coerceAtLeast(0L)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    @Synchronized
    fun clearAll() {
        val dir = cacheDir ?: return
        try {
            dir.listFiles()?.forEach { it.delete() }
            cachedTotalBytes = 0L
        } catch (_: Exception) {}
    }
}

/**
 * ネイティブ Google Maps SDK for Android によるレーダー地図コンポーネント。
 * - Mobile Native Dynamic Maps ($0 / 完全無料・無制限) のみを使用
 * - アプリのダーク・ライトテーマに完全連動するスタイリング
 * - 気象庁降水ナウキャスト / RainViewer の雨雲タイルを TileOverlay で高品位レンダリング
 * - チラつきのない 60fps 高速フレーム切り替え＆アニメーション再生
 */
@Composable
fun RadarMapView(
    latitude: Double,
    longitude: Double,
    radarFrames: List<RadarFrame>,
    host: String = "https://tilecache.rainviewer.com",
    modifier: Modifier = Modifier,
    zoom: Int = 11,
    recenterTrigger: Int = 0,
    zoomInTrigger: Int = 0,
    zoomOutTrigger: Int = 0,
    isInteractive: Boolean = false,
    isPlaying: Boolean = false,
    currentFrameIndex: Int = -1,
    colorScheme: RadarColorScheme = RadarColorScheme.UNIVERSAL_BLUE,
    isDark: Boolean = LocalDarkTheme.current,
    showZoomBadge: Boolean = false,
    typhoons: List<JmaTyphoonItem> = emptyList(),
    selectedTyphoon: JmaTyphoonItem? = null,
    onTyphoonClick: (JmaTyphoonItem) -> Unit = {},
    onFrameChanged: (index: Int, timestamp: Long) -> Unit = { _, _ -> },
    onMapReady: () -> Unit = {},
    onMapClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentLanguage = LocalAppLanguage.current
    val m3ColorScheme = MaterialTheme.colorScheme

    // 各言語ロケールを反映した Context を生成（グローバル状態は書き換えない）
    val mapContext = remember(context, currentLanguage) {
        val targetLocale = currentLanguage.locale
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        val localizedContext = context.createConfigurationContext(config)
        try {
            com.google.android.gms.maps.MapsInitializer.initialize(localizedContext.applicationContext)
        } catch (e: Exception) {
            // MapsInitializer already initialized or fallback
        }
        localizedContext
    }

    val currentOnTyphoonClick by rememberUpdatedState(onTyphoonClick)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnFrameChanged by rememberUpdatedState(onFrameChanged)
    val currentOnMapReady by rememberUpdatedState(onMapReady)
    val currentIsDark by rememberUpdatedState(isDark)
    val currentIsInteractive by rememberUpdatedState(isInteractive)
    val currentLatitude by rememberUpdatedState(latitude)
    val currentLongitude by rememberUpdatedState(longitude)
    val currentZoom by rememberUpdatedState(zoom)

    var googleMapRef by remember { mutableStateOf<GoogleMap?>(null) }
    var markerRef by remember { mutableStateOf<Marker?>(null) }
    var isMapInitialized by remember { mutableStateOf(false) }
    // ズーム倍率をStateFlowで管理（カメラ移動中の全画面リコンポーズを完全に防止）
    val zoomLevelFlow = remember { MutableStateFlow(zoom.toFloat()) }
    var cameraIdleRevision by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val activeOverlays = remember(mapContext) { mutableMapOf<Int, TileOverlay>() }
    val activeTyphoonObjects = remember(mapContext) { mutableListOf<Any>() }

    val mapView = remember(mapContext) {
        MapView(mapContext).apply {
            onCreate(Bundle())
            setOnGenericMotionListener { _, event ->
                if (currentIsInteractive && event.action == android.view.MotionEvent.ACTION_SCROLL) {
                    val vScroll = event.getAxisValue(android.view.MotionEvent.AXIS_VSCROLL)
                    if (vScroll != 0f) {
                        val map = googleMapRef
                        if (map != null) {
                            // マウスホイール上回転（奥へスクロール / 正）: ズームイン
                            // マウスホイール下回転（手前へスクロール / 負）: ズームアウト
                            val zoomDelta = if (vScroll > 0f) 0.65f else -0.65f
                            map.animateCamera(CameraUpdateFactory.zoomBy(zoomDelta), 120, null)
                            return@setOnGenericMotionListener true
                        }
                    }
                }
                false
            }
        }
    }

    // MapView のライフサイクル管理（onDestroy 二重呼び出し防止 + オーバーレイ/リスナー解放）
    DisposableEffect(lifecycleOwner, mapView) {
        var isDestroyed = false
        var isStarted = false
        var isResumed = false
        fun destroySafely() {
            if (isDestroyed) return
            isDestroyed = true
            try {
                if (isResumed) {
                    mapView.onPause()
                    isResumed = false
                }
                if (isStarted) {
                    mapView.onStop()
                    isStarted = false
                }
                googleMapRef?.apply {
                    setOnMapClickListener(null)
                    setOnMarkerClickListener(null)
                    setOnCircleClickListener(null)
                    setOnCameraMoveListener(null)
                    setOnCameraIdleListener(null)
                }
                markerRef?.remove()
                markerRef = null
                RadarTyphoonOverlay.clearTyphoons(activeTyphoonObjects)
                activeOverlays.values.forEach { try { it.remove() } catch (_: Exception) {} }
                activeOverlays.clear()
                googleMapRef = null
                mapView.onDestroy()
            } catch (_: Exception) {
                // Ignore lifecycle cleanup errors
            }
        }

        val currentState = lifecycleOwner.lifecycle.currentState
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
            isStarted = true
        }
        if (currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
            isResumed = true
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!isDestroyed && !isStarted) {
                    mapView.onStart()
                    isStarted = true
                }
                Lifecycle.Event.ON_RESUME -> if (!isDestroyed && !isResumed) {
                    mapView.onResume()
                    isResumed = true
                }
                Lifecycle.Event.ON_PAUSE -> if (!isDestroyed && isResumed) {
                    mapView.onPause()
                    isResumed = false
                }
                Lifecycle.Event.ON_STOP -> if (!isDestroyed && isStarted) {
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

    // GoogleMap の非同期初期化
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            googleMapRef = map

            // 1. スタイリング（ダーク / ライトテーマ適用）
            try {
                val styleJson = if (currentIsDark) DARK_MAP_STYLE_JSON else LIGHT_MAP_STYLE_JSON
                map.setMapStyle(MapStyleOptions(styleJson))
            } catch (e: Exception) {
                android.util.Log.e("RadarMapView", "Failed to apply map style", e)
                try {
                    map.setMapStyle(null)
                } catch (_: Exception) {}
            }

            // 2. UI設定
            map.uiSettings.apply {
                isZoomControlsEnabled = false
                isCompassEnabled = false
                isMyLocationButtonEnabled = false
                isMapToolbarEnabled = false
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isScrollGesturesEnabled = currentIsInteractive
                isZoomGesturesEnabled = currentIsInteractive
            }

            // 3. 初期カメラ位置
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLatitude, currentLongitude), currentZoom.toFloat()))

            // 4. 現在地ピン追加
            try {
                val pinBitmap = createLocationMarkerBitmap(context)
                val descriptor = BitmapDescriptorFactory.fromBitmap(pinBitmap)
                pinBitmap.recycle()
                markerRef = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(currentLatitude, currentLongitude))
                        .anchor(0.5f, 0.5f)
                        .icon(descriptor)
                        .zIndex(200f)
                )
            } catch (e: Exception) {
                android.util.Log.e("RadarMapView", "Failed to add marker", e)
            }

            // 5. タップリスナー
            map.setOnMapClickListener {
                currentOnMapClick()
            }
            map.setOnMarkerClickListener { marker ->
                val typhoonItem = marker.tag as? JmaTyphoonItem
                if (typhoonItem != null) {
                    currentOnTyphoonClick(typhoonItem)
                    true
                } else {
                    false
                }
            }
            map.setOnCircleClickListener { circle ->
                val typhoonItem = circle.tag as? JmaTyphoonItem
                if (typhoonItem != null) {
                    currentOnTyphoonClick(typhoonItem)
                }
            }

            // 6. テスト用：カメラ移動・ズームリスナー（StateFlowを更新し、全体リコンポーズを回避）
            map.setOnCameraMoveListener {
                zoomLevelFlow.value = map.cameraPosition.zoom
            }
            map.setOnCameraIdleListener {
                cameraIdleRevision++
            }

            isMapInitialized = true
            currentOnMapReady()
        }
    }

    // アプリのダーク / ライトモード変更へのリアルタイム追従
    LaunchedEffect(isDark, isMapInitialized) {
        if (isMapInitialized) {
            val map = googleMapRef ?: return@LaunchedEffect
            try {
                val styleJson = if (isDark) DARK_MAP_STYLE_JSON else LIGHT_MAP_STYLE_JSON
                map.setMapStyle(MapStyleOptions(styleJson))
            } catch (e: Exception) {
                android.util.Log.e("RadarMapView", "Failed to update map style for theme change", e)
                try {
                    map.setMapStyle(null)
                } catch (_: Exception) {}
            }
        }
    }

    // 台風オーバレイ（過去経路・強風域・暴風域・進路予想・予報円・マーカー）の描画
    // 台風データ初期表示時のみカメラ移動
    var lastRenderedTyphoonIds by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(typhoons, isMapInitialized, isDark, currentLanguage, m3ColorScheme) {
        val map = googleMapRef
        if (isMapInitialized && map != null) {
            RadarTyphoonOverlay.renderTyphoons(
                map = map,
                context = context,
                typhoons = typhoons,
                language = currentLanguage,
                colorScheme = m3ColorScheme,
                isDark = isDark,
                activeObjects = activeTyphoonObjects
            )
            val currentIds = typhoons.map { it.id }
            if (typhoons.isNotEmpty() && currentIds != lastRenderedTyphoonIds) {
                lastRenderedTyphoonIds = currentIds
                val firstTyphoon = typhoons.first()
                val targetLatLng = LatLng(firstTyphoon.analysis.latitude, firstTyphoon.analysis.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 5.2f))
            }
        }
    }

    // インタラクティブ設定の反映
    LaunchedEffect(isInteractive, isMapInitialized) {
        if (isMapInitialized) {
            googleMapRef?.uiSettings?.apply {
                isScrollGesturesEnabled = isInteractive
                isZoomGesturesEnabled = isInteractive
            }
        }
    }

    // カメラ位置・マーカー位置の更新（座標・ズーム変更時）
    LaunchedEffect(latitude, longitude, zoom, isMapInitialized) {
        if (isMapInitialized) {
            val latLng = LatLng(latitude, longitude)
            markerRef?.position = latLng
            googleMapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom.toFloat()))
        }
    }

    // センタリング発火時
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && isMapInitialized) {
            googleMapRef?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoom.toFloat())
            )
        }
    }

    // ズームイン
    LaunchedEffect(zoomInTrigger) {
        if (zoomInTrigger > 0 && isMapInitialized) {
            googleMapRef?.animateCamera(CameraUpdateFactory.zoomIn())
        }
    }

    // ズームアウト
    LaunchedEffect(zoomOutTrigger) {
        if (zoomOutTrigger > 0 && isMapInitialized) {
            googleMapRef?.animateCamera(CameraUpdateFactory.zoomOut())
        }
    }

    val currentFrameIndexState = rememberUpdatedState(currentFrameIndex)
    val radarFramesState = rememberUpdatedState(radarFrames)

    // ホスト接続を事前確立し、レーダータイル取得での接続再利用を試みる
    LaunchedEffect(host) {
        if (host.isNotBlank()) {
            withContext(Dispatchers.IO) {
                RadarTileHttpClient.prewarm(host)
            }
        }
    }

    // 現在の表示範囲、ズーム、選択中フレームを優先してバックグラウンド先読み
    LaunchedEffect(radarFrames, latitude, longitude, currentFrameIndex, isPlaying, isMapInitialized, cameraIdleRevision) {
        if (isMapInitialized && !isPlaying && radarFrames.isNotEmpty()) {
            // スライダー操作中に古いフレームの先読みを大量に積まないよう、最後の選択を少し待つ
            delay(250L)
            val map = googleMapRef ?: return@LaunchedEffect
            val visibleBounds = try {
                map.projection.visibleRegion.latLngBounds
            } catch (_: Exception) {
                null
            }
            val cameraTarget = map.cameraPosition.target
            val cameraZoom = map.cameraPosition.zoom.toInt()
            withContext(Dispatchers.IO) {
                RadarTileProvider.prefetchNearbyTiles(
                    context = context,
                    frames = radarFrames,
                    lat = cameraTarget.latitude,
                    lon = cameraTarget.longitude,
                    host = host,
                    colorScheme = colorScheme,
                    currentFrameIndex = currentFrameIndex,
                    mapZoom = cameraZoom,
                    visibleBounds = visibleBounds
                )
            }
        }
    }

    // ヘルパー関数: 指定インデックスのTileOverlayを取得または生成
    fun getOrCreateOverlay(
        map: GoogleMap,
        index: Int,
        frames: List<RadarFrame>,
        visible: Boolean
    ): TileOverlay? {
        if (index !in frames.indices) return null
        activeOverlays[index]?.let {
            if (it.isVisible != visible) it.isVisible = visible
            return it
        }
        val frame = frames[index]
        val isJma = (frame.tileUrlTemplate?.contains("jma.go.jp") == true) ||
                (frame.path.startsWith("http") && frame.path.contains("jma.go.jp"))
        val isUs = (frame.tileUrlTemplate?.contains("mesonet.agron.iastate.edu") == true) ||
                (frame.tileUrlTemplate?.contains("mrms") == true) ||
                (frame.tileUrlTemplate?.contains("hrrr") == true)
        val transparency = if (isJma) 0.32f else if (isUs) 0.28f else 0.42f

        val provider = RadarTileProvider(frame, host, colorScheme)
        val options = TileOverlayOptions()
            .tileProvider(provider)
            .transparency(transparency)
            .zIndex(100f)
            .fadeIn(false)
            .visible(visible)

        return try {
            val overlay = map.addTileOverlay(options)
            if (overlay != null) {
                activeOverlays[index] = overlay
            }
            overlay
        } catch (e: Exception) {
            android.util.Log.e("RadarMapView", "Failed to add tile overlay for index $index", e)
            null
        }
    }

    // レーダーデータ（フレーム一覧）またはカラースキーム変更時のオーバーレイ初期化
    LaunchedEffect(isMapInitialized, radarFrames, colorScheme, host) {
        if (isMapInitialized && googleMapRef != null) {
            val map = googleMapRef ?: return@LaunchedEffect

            // 古いオーバーレイをすべて削除してリセット
            activeOverlays.values.forEach { try { it.remove() } catch (_: Exception) {} }
            activeOverlays.clear()

            if (radarFrames.isEmpty()) return@LaunchedEffect

            val initialActiveIndex = if (currentFrameIndex in radarFrames.indices) {
                currentFrameIndex
            } else {
                radarFrames.lastIndex
            }

            // 初期フレームと前後フレーム（スライディングウィンドウ: 最大3枚）のみをマップに登録
            val window = (initialActiveIndex - 1 .. initialActiveIndex + 1).filter { it in radarFrames.indices }
            for (idx in window) {
                getOrCreateOverlay(map, idx, radarFrames, visible = (idx == initialActiveIndex))
            }

            if (currentFrameIndex < 0 && initialActiveIndex in radarFrames.indices) {
                currentOnFrameChanged(initialActiveIndex, radarFrames[initialActiveIndex].time)
            }
        }
    }

    // 表示フレーム切り替え（スライディングウィンドウ方式：常に表示フレーム±1の最大3枚のみマップに存在させ、OOMを根絶）
    LaunchedEffect(currentFrameIndex, isMapInitialized, radarFrames.size) {
        if (isMapInitialized && radarFrames.isNotEmpty()) {
            val map = googleMapRef ?: return@LaunchedEffect
            val targetIndex = if (currentFrameIndex in radarFrames.indices) {
                currentFrameIndex
            } else {
                radarFrames.lastIndex
            }

            // 保持すべきウィンドウ範囲 (現在インデックス ± 1)
            val windowRange = (targetIndex - 1 .. targetIndex + 1).filter { it in radarFrames.indices }.toSet()

            // 範囲外の古いオーバーレイを安全に remove して解放
            val toRemove = activeOverlays.keys.filter { it !in windowRange }
            toRemove.forEach { oldIdx ->
                try {
                    activeOverlays.remove(oldIdx)?.remove()
                } catch (_: Exception) {}
            }

            // 目的フレームを表示
            val currentOverlay = getOrCreateOverlay(map, targetIndex, radarFrames, visible = true)
            currentOverlay?.isVisible = true

            // 前後フレームを非表示プリロード状態で準備
            for (idx in windowRange) {
                if (idx != targetIndex) {
                    val preOverlay = getOrCreateOverlay(map, idx, radarFrames, visible = false)
                    preOverlay?.isVisible = false
                }
            }
        }
    }

    // アニメーション自動再生（途中で止まらず、最新状態を参照してシームレスに周回再生）
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var localIndex = currentFrameIndexState.value
            while (isActive) {
                delay(650L)
                val frames = radarFramesState.value
                if (frames.isNotEmpty()) {
                    val externalIndex = currentFrameIndexState.value
                    val baseIndex = if (externalIndex != localIndex && externalIndex in frames.indices) {
                        externalIndex
                    } else if (localIndex in frames.indices) {
                        localIndex
                    } else {
                        frames.lastIndex
                    }
                    val nextIndex = (baseIndex + 1) % frames.size
                    localIndex = nextIndex
                    val frame = frames[nextIndex]
                    currentOnFrameChanged(nextIndex, frame.time)
                }
            }
        }
    }

    Box(modifier = modifier) {
        key(mapView) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
        }

        // デバッグ設定用：リアルタイムズーム倍率インジケーター（デフォルトはオフ）
        if (showZoomBadge) {
            ZoomLevelBadge(
                zoomFlow = zoomLevelFlow,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 64.dp)
            )
        }
    }
}

/**
 * ズーム倍率テスト用バッジ（RadarMapView の再コンポーズを発生させない独立コンポーザブル）
 */
@Composable
private fun ZoomLevelBadge(
    zoomFlow: StateFlow<Float>,
    modifier: Modifier = Modifier
) {
    val currentLanguage = LocalAppLanguage.current
    val currentZoom by zoomFlow.collectAsState()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xD90F172A),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val zoomFormat = when (currentLanguage) {
                AppLanguage.JAPANESE -> "🔍 ズーム: %.2f (Lv %d)"
                AppLanguage.ENGLISH -> "🔍 Zoom: %.2f (Lv %d)"
                AppLanguage.SIMPLIFIED_CHINESE -> "🔍 缩放: %.2f (Lv %d)"
                AppLanguage.TRADITIONAL_CHINESE -> "🔍 縮放: %.2f (Lv %d)"
            }
            Text(
                text = zoomFormat.format(currentZoom, currentZoom.toInt()),
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * 各レーダーフレームの画像タイルを取得する TileProvider。
 * - 気象庁降水ナウキャストの仕様：偶数ズーム(4, 6, 8, 10)のみ画像配信される特性に対応
 * - 奇数ズーム(3, 5, 7, 9)および高ズーム(11〜22)で親/子タイルから高速・高品位に補間生成
 * - 全ズームレベル(0〜22)で雨雲の完全連続描画を実現（3, 5, 7, 9での消失を根絶）
 * - LRUメモリキャッシュ(生タイル・親Bitmap・生成済みタイルの3層)によりRAM上で高速再利用
 * - バックグラウンド事前フェッチにより、ズーム時や時間操作時の画像空白化をゼロ化
 */
private class RadarTileProvider(
    private val frame: RadarFrame,
    private val host: String,
    private val colorScheme: RadarColorScheme
) : com.google.android.gms.maps.model.TileProvider {

    private val additionalTileProviders by lazy {
        frame.additionalTileUrlTemplates.map { tileUrl ->
            RadarTileProvider(
                frame = frame.copy(
                    path = tileUrl,
                    tileUrlTemplate = tileUrl,
                    additionalTileUrlTemplates = emptyList()
                ),
                host = host,
                colorScheme = colorScheme
            )
        }
    }

    companion object {
        // 256x256 完全透明 PNG バイト列（起動時1回のみ生成・共通利用）
        val TRANSPARENT_256_PNG_BYTES: ByteArray by lazy {
            val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val stream = java.io.ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            bmp.recycle()
            stream.toByteArray()
        }
        val TRANSPARENT_512_PNG_BYTES: ByteArray = TRANSPARENT_256_PNG_BYTES
        val TRANSPARENT_PNG_BYTES: ByteArray = TRANSPARENT_256_PNG_BYTES

        // 1. ダウンロード済み生タイルバイト列のLRUキャッシュ (最大16MB)
        private val rawBytesCache = object : android.util.LruCache<String, ByteArray>(16 * 1024) {
            override fun sizeOf(key: String, value: ByteArray): Int = (value.size / 1024).coerceAtLeast(1)
        }

        // 2. 高ズーム・奇数ズーム時の切り出し元親ビットマップのLRUキャッシュ (最大8MB)
        private val bitmapCache = object : android.util.LruCache<String, Bitmap>(8 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
        }

        // 3. 切り出し・拡大生成済み 256x256 タイルPNGのLRUキャッシュ (最大12MB)
        private val renderedTileCache = object : android.util.LruCache<String, ByteArray>(12 * 1024) {
            override fun sizeOf(key: String, value: ByteArray): Int = (value.size / 1024).coerceAtLeast(1)
        }

        // 4. JMA色加工済みPNGのキャッシュ (最大6MB)
        private val processedBytesCache = object : android.util.LruCache<String, ByteArray>(6 * 1024) {
            override fun sizeOf(key: String, value: ByteArray): Int = (value.size / 1024).coerceAtLeast(1)
        }

        // 5. 極大ズーム用 単色タイルキャッシュ（上限64色）
        private val solidColorTileCache = object : android.util.LruCache<Int, ByteArray>(64) {}

        @Volatile
        private var lastRainViewerPrefetchAtMillis = 0L

        // 同一URLへの重複・同時ダウンロードを集約する固定ストライプ構造（クリアレース・メモリリークなし）
        private val downloadLockStripes = Array(128) { Any() }

        private val isoUtcFormatLocal = ThreadLocal.withInitial {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
        }
        private val utcCompactFormatLocal = ThreadLocal.withInitial {
            java.text.SimpleDateFormat("yyyyMMddHHmm", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
        }

        fun clearMemoryCache() {
            synchronized(rawBytesCache) { rawBytesCache.evictAll() }
            synchronized(bitmapCache) { bitmapCache.evictAll() }
            synchronized(renderedTileCache) { renderedTileCache.evictAll() }
            synchronized(processedBytesCache) { processedBytesCache.evictAll() }
            synchronized(solidColorTileCache) { solidColorTileCache.evictAll() }
        }

        fun isValidImageBytes(bytes: ByteArray?): Boolean {
            return RadarTileHttpClient.isValidImageBytes(bytes)
        }

        fun isValidPngBytes(bytes: ByteArray?): Boolean {
            return RadarTileHttpClient.isValidPngBytes(bytes)
        }

        private fun getOrCreateSolidColorTile(color: Int): ByteArray {
            synchronized(solidColorTileCache) {
                solidColorTileCache.get(color)?.let { return it }
            }
            val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            return try {
                bmp.eraseColor(color)
                val stream = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val encoded = stream.toByteArray()
                synchronized(solidColorTileCache) {
                    solidColorTileCache.put(color, encoded)
                }
                encoded
            } finally {
                bmp.recycle()
            }
        }

        /**
         * バックグラウンドで表示範囲のタイルと選択フレーム近傍を先読み
         */
        fun prefetchNearbyTiles(
            context: Context,
            frames: List<RadarFrame>,
            lat: Double,
            lon: Double,
            host: String,
            colorScheme: RadarColorScheme,
            currentFrameIndex: Int,
            mapZoom: Int,
            visibleBounds: LatLngBounds?
        ) {
            TileDiskCache.init(context)
            if (frames.isEmpty()) return

            // ホスト接続の事前温存 (TLS Pre-warm)
            RadarTileHttpClient.prewarm(host)

            // 表示フレームからソースを判定し、ズームとソースの配信上限に合わせる
            val selectedIndex = currentFrameIndex.takeIf { it in frames.indices } ?: frames.lastIndex
            val selectedFrame = frames[selectedIndex]
            val sampleTemplate = selectedFrame.tileUrlTemplate ?: selectedFrame.path
            val isRainViewer = host.contains("rainviewer.com") || sampleTemplate.contains("rainviewer.com")
            val isUsRadar = host.contains("mesonet.agron.iastate.edu") ||
                    sampleTemplate.contains("mesonet.agron.iastate.edu") ||
                    sampleTemplate.contains("mrms") ||
                    sampleTemplate.contains("hrrr") ||
                    sampleTemplate.contains("ridge::")

            if (isRainViewer) {
                val now = System.currentTimeMillis()
                synchronized(this) {
                    if (now - lastRainViewerPrefetchAtMillis < 10_000L) return
                    lastRainViewerPrefetchAtMillis = now
                }
            }

            val effectiveZoom = mapZoom.coerceAtLeast(4)
            val targetZooms = when {
                isRainViewer -> when {
                    effectiveZoom >= 7 -> listOf(7, 5)
                    effectiveZoom >= 5 -> listOf(4, effectiveZoom)
                    else -> listOf(effectiveZoom)
                }
                isUsRadar -> {
                    val clamped = effectiveZoom.coerceIn(4, 14)
                    if (clamped > 4) listOf(clamped, clamped - 1) else listOf(4)
                }
                else -> listOf(8, 10).filter { it <= effectiveZoom }.ifEmpty { listOf(4) }
            }.distinct()
            val priorityFrames = if (isUsRadar) {
                listOfNotNull(selectedFrame, frames.getOrNull(selectedIndex + 1)).distinct()
            } else {
                listOf(selectedFrame)
            }
            val urlsToPrefetch = mutableListOf<String>()

            for (z in targetZooms) {
                val n = 1 shl z
                val tileCoords = visibleBounds?.let { getVisibleTileCoordinates(it, z) }
                    ?.takeIf { it.isNotEmpty() }
                    ?: getCenteredTileCoordinates(lat, lon, z)
                val centerX = (((lon + 180.0) / 360.0) * n).toInt().coerceIn(0, n - 1)
                val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
                val centerY = ((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n)
                    .toInt().coerceIn(0, n - 1)
                val prioritizedCoords = tileCoords
                    .sortedBy { (x, y) -> kotlin.math.abs(x - centerX) + kotlin.math.abs(y - centerY) }
                    .take(if (isRainViewer) 8 else 9)
                for (f in priorityFrames) {
                    val provider = RadarTileProvider(f, host, colorScheme)
                    for ((tx, ty) in prioritizedCoords) {
                        for (url in provider.buildTileUrls(tx, ty, z)) {
                            val inMem = synchronized(rawBytesCache) { rawBytesCache.get(url) != null }
                            val onDisk = !inMem && TileDiskCache.get(url) != null
                            if (!inMem && !onDisk) urlsToPrefetch.add(url)
                        }
                    }
                }
            }

            if (urlsToPrefetch.isNotEmpty()) {
                val uniqueUrls = urlsToPrefetch.distinct()
                val boundedUrls = if (isRainViewer) uniqueUrls.take(16) else uniqueUrls.take(54)
                RadarTileHttpClient.prefetchTiles(boundedUrls) { url, bytes ->
                    synchronized(rawBytesCache) {
                        rawBytesCache.put(url, bytes)
                    }
                    TileDiskCache.put(url, bytes)
                }
            }
        }

        private fun getCenteredTileCoordinates(lat: Double, lon: Double, zoom: Int): List<Pair<Int, Int>> {
            val n = 1 shl zoom
            val centerX = (((lon + 180.0) / 360.0) * n).toInt().coerceIn(0, n - 1)
            val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
            val sinhValue = Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad))
            val centerY = ((1.0 - sinhValue / Math.PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
            return buildList {
                for (dx in -1..1) for (dy in -1..1) {
                    val x = centerX + dx
                    val y = centerY + dy
                    if (x in 0 until n && y in 0 until n) add(x to y)
                }
            }
        }

        private fun getVisibleTileCoordinates(bounds: LatLngBounds, zoom: Int): List<Pair<Int, Int>> {
            val n = 1 shl zoom
            fun tileX(lon: Double): Int = (((lon + 180.0) / 360.0) * n).toInt().coerceIn(0, n - 1)
            fun tileY(lat: Double): Int {
                val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
                val sinhValue = Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad))
                return ((1.0 - sinhValue / Math.PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
            }

            val westX = tileX(bounds.southwest.longitude)
            val eastX = tileX(bounds.northeast.longitude)
            val northY = tileY(bounds.northeast.latitude)
            val southY = tileY(bounds.southwest.latitude)
            val xRange = if (westX <= eastX) westX..eastX else (westX until n) + (0..eastX)
            return buildList {
                for (x in xRange) for (y in northY..southY) add(x to y)
            }.distinct().take(32)
        }

        /**
         * 親ビットマップ内の指定領域が全ピクセル透明かどうかを超高速に判定
         * 雨雲がない領域（全体の8〜9割）でBitmap生成・PNG圧縮処理を完全スキップ
         */
        private fun isRegionTransparent(bitmap: Bitmap, startX: Int, startY: Int, size: Int): Boolean {
            if (bitmap.isRecycled) return true
            val w = bitmap.width
            val h = bitmap.height
            val safeSize = size.coerceAtLeast(1)
            val endX = (startX + safeSize).coerceAtMost(w)
            val endY = (startY + safeSize).coerceAtMost(h)
            if (startX >= endX || startY >= endY) return true

            if (safeSize <= 16) {
                for (y in startY until endY) {
                    for (x in startX until endX) {
                        if ((bitmap.getPixel(x, y) ushr 24) != 0) return false
                    }
                }
                return true
            }

            val scanWidth = endX - startX
            val buffer = IntArray(scanWidth)
            for (y in startY until endY) {
                bitmap.getPixels(buffer, 0, scanWidth, startX, y, scanWidth, 1)
                for (i in 0 until scanWidth) {
                    if ((buffer[i] ushr 24) != 0) return false
                }
            }
            return true
        }
    }

    override fun getTile(x: Int, y: Int, zoom: Int): com.google.android.gms.maps.model.Tile? {
        if (zoom < 0 || zoom > 22) {
            return com.google.android.gms.maps.model.TileProvider.NO_TILE
        }
        val maxTiles = 1 shl zoom
        if (x < 0 || x >= maxTiles || y < 0 || y >= maxTiles) {
            return com.google.android.gms.maps.model.TileProvider.NO_TILE
        }

        val template = frame.tileUrlTemplate ?: frame.path
        val isJma = template.contains("jma.go.jp")
        val isHimawari = template.contains("himawari")
        val isJmaPrecipRadar = template.contains("/hrpns/") || template.contains("/rasrf/")

        val isUsRadar = template.contains("mesonet.agron.iastate.edu") ||
                template.contains("mrms") ||
                template.contains("hrrr") ||
                template.contains("ridge::")

        if (isJma && frame.additionalTileUrlTemplates.isNotEmpty()) {
            return fetchCompositeJmaTile(x, y, zoom)
        }

        if (isUsRadar) {
            return fetchUsTileWithPyramidFallback(x, y, zoom)
        } else if (isJma && !isHimawari) {
            return fetchJmaTileWithPyramidFallback(x, y, zoom, applyLightRainAlpha = isJmaPrecipRadar)
        } else if (isHimawari) {
            val maxNativeZoom = 6
            val minNativeZoom = 3
            if (zoom < minNativeZoom) {
                return com.google.android.gms.maps.model.Tile(256, 256, TRANSPARENT_256_PNG_BYTES)
            }
            if (zoom <= maxNativeZoom) {
                return fetchDirectTile(x, y, zoom, applyJmaRainAlpha = false)
            }
            return renderFromParent(parentZoom = maxNativeZoom, x = x, y = y, zoom = zoom, applyJmaRainAlpha = false, isRadarSource = false)
        } else {
            return fetchRainViewerTileWithPyramidFallback(x, y, zoom)
        }
    }

    private fun fetchCompositeJmaTile(x: Int, y: Int, zoom: Int): com.google.android.gms.maps.model.Tile? {
        val cacheKey = buildString {
            append("jma-composite|")
            append(frame.tileUrlTemplate ?: frame.path)
            frame.additionalTileUrlTemplates.forEach { append('|').append(it) }
            append('|').append(zoom).append('|').append(x).append('|').append(y)
        }
        synchronized(renderedTileCache) {
            renderedTileCache.get(cacheKey)?.let { return com.google.android.gms.maps.model.Tile(256, 256, it) }
        }

        val sourceTiles = buildList {
            fetchJmaTileWithPyramidFallback(x, y, zoom, applyLightRainAlpha = false)?.takeIf {
                it != com.google.android.gms.maps.model.TileProvider.NO_TILE
            }?.let(::add)
            additionalTileProviders.forEach { provider ->
                provider.getTile(x, y, zoom)?.takeIf {
                    it != com.google.android.gms.maps.model.TileProvider.NO_TILE
                }?.let(::add)
            }
        }
        if (sourceTiles.isEmpty()) return null

        val sourceBitmaps = sourceTiles.mapNotNull { tile ->
            val bytes = tile.data ?: return@mapNotNull null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        if (sourceBitmaps.isEmpty()) return null

        val combined = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        return try {
            val canvas = Canvas(combined)
            val paint = Paint().apply {
                isFilterBitmap = false
                isAntiAlias = false
            }
            sourceBitmaps.forEach { bitmap -> canvas.drawBitmap(bitmap, 0f, 0f, paint) }

            val stream = java.io.ByteArrayOutputStream()
            combined.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()
            if (bytes.isEmpty()) return null
            synchronized(renderedTileCache) { renderedTileCache.put(cacheKey, bytes) }
            com.google.android.gms.maps.model.Tile(256, 256, bytes)
        } catch (e: Exception) {
            android.util.Log.w("RadarMapView", "Failed to combine JMA risk tiles", e)
            null
        } finally {
            sourceBitmaps.forEach { if (!it.isRecycled) it.recycle() }
            if (!combined.isRecycled) combined.recycle()
        }
    }

    fun buildTileUrls(x: Int, y: Int, zoom: Int): List<String> = buildList {
        buildUrl(x, y, zoom)?.let(::add)
        additionalTileProviders.forEach { provider -> provider.buildUrl(x, y, zoom)?.let(::add) }
    }

    private fun fetchJmaTileWithPyramidFallback(
        x: Int,
        y: Int,
        zoom: Int,
        applyLightRainAlpha: Boolean
    ): com.google.android.gms.maps.model.Tile? {
        if (zoom < 3) {
            return com.google.android.gms.maps.model.Tile(256, 256, TRANSPARENT_256_PNG_BYTES)
        }
        if (zoom == 3) {
            return buildZoom3JmaTile(x, y, applyLightRainAlpha)
        }

        val candidateZooms = when {
            zoom >= 10 -> listOf(10, 8, 6, 4)
            zoom in 8..9 -> listOf(8, 6, 4)
            zoom in 6..7 -> listOf(6, 4)
            else -> listOf(4)
        }

        var allKnownEmpty = true
        for (pZoom in candidateZooms) {
            val checkUrl = if (pZoom == zoom) buildUrl(x, y, zoom) else buildUrl(x shr (zoom - pZoom), y shr (zoom - pZoom), pZoom)
            val tile = if (pZoom == zoom) {
                fetchDirectTile(x, y, zoom, applyJmaRainAlpha = applyLightRainAlpha)
            } else {
                renderFromParent(parentZoom = pZoom, x = x, y = y, zoom = zoom, applyJmaRainAlpha = applyLightRainAlpha, isRadarSource = true)
            }
            if (tile != null) {
                return tile
            }
            if (checkUrl != null && !RadarTileHttpClient.isKnownEmptyTile(checkUrl)) {
                allKnownEmpty = false
            }
        }

        return if (allKnownEmpty) com.google.android.gms.maps.model.TileProvider.NO_TILE else null
    }

    private fun fetchRainViewerTileWithPyramidFallback(x: Int, y: Int, zoom: Int): com.google.android.gms.maps.model.Tile? {
        if (zoom < 3) {
            return com.google.android.gms.maps.model.Tile(256, 256, TRANSPARENT_256_PNG_BYTES)
        }

        val candidateZooms = when {
            zoom >= 7 -> listOf(7, 5, 4)
            zoom in 5..6 -> listOf(5, 4)
            else -> listOf(4)
        }

        var allKnownEmpty = true
        for (pZoom in candidateZooms) {
            val checkUrl = if (pZoom == zoom) buildUrl(x, y, zoom) else buildUrl(x shr (zoom - pZoom), y shr (zoom - pZoom), pZoom)
            val tile = if (pZoom == zoom) {
                fetchDirectTile(x, y, zoom, applyJmaRainAlpha = false)
            } else {
                renderFromParent(parentZoom = pZoom, x = x, y = y, zoom = zoom, applyJmaRainAlpha = false, isRadarSource = false)
            }
            if (tile != null) {
                return tile
            }
            if (checkUrl != null && !RadarTileHttpClient.isKnownEmptyTile(checkUrl)) {
                allKnownEmpty = false
            }
        }

        return if (allKnownEmpty) com.google.android.gms.maps.model.TileProvider.NO_TILE else null
    }

    private fun fetchUsTileWithPyramidFallback(x: Int, y: Int, zoom: Int): com.google.android.gms.maps.model.Tile? {
        if (zoom < 3) {
            return com.google.android.gms.maps.model.Tile(256, 256, TRANSPARENT_256_PNG_BYTES)
        }

        val directTile = fetchDirectTile(x, y, zoom, applyJmaRainAlpha = false)
        if (directTile != null) {
            return directTile
        }

        val directUrl = buildUrl(x, y, zoom)
        if (directUrl != null && RadarTileHttpClient.isKnownEmptyTile(directUrl) && zoom <= 14) {
            return com.google.android.gms.maps.model.TileProvider.NO_TILE
        }

        val maxFallback = minOf(zoom - 1, 12)
        val candidateZooms = (maxFallback downTo 4 step 2).take(3)
        var allKnownEmpty = directUrl != null && RadarTileHttpClient.isKnownEmptyTile(directUrl)
        for (pZoom in candidateZooms) {
            val tile = renderFromParent(parentZoom = pZoom, x = x, y = y, zoom = zoom, applyJmaRainAlpha = false, isRadarSource = true)
            if (tile != null) {
                return tile
            }
            val pUrl = buildUrl(x shr (zoom - pZoom), y shr (zoom - pZoom), pZoom)
            if (pUrl != null && !RadarTileHttpClient.isKnownEmptyTile(pUrl)) {
                allKnownEmpty = false
            }
        }

        return if (allKnownEmpty) com.google.android.gms.maps.model.TileProvider.NO_TILE else null
    }

    private fun fetchDirectTile(x: Int, y: Int, zoom: Int, applyJmaRainAlpha: Boolean): com.google.android.gms.maps.model.Tile? {
        val url = buildUrl(x, y, zoom) ?: return null
        val rawBytes = getOrDownloadBytes(url) ?: return null
        if (!isValidImageBytes(rawBytes)) return null

        val processedBytes = if (applyJmaRainAlpha) processJmaTile(url, rawBytes) else rawBytes
        return com.google.android.gms.maps.model.Tile(256, 256, processedBytes)
    }

    private fun renderFromParent(
        parentZoom: Int,
        x: Int,
        y: Int,
        zoom: Int,
        applyJmaRainAlpha: Boolean,
        isRadarSource: Boolean
    ): com.google.android.gms.maps.model.Tile? {
        val zoomDiff = zoom - parentZoom
        if (zoomDiff < 0) return null
        val parentX = x shr zoomDiff
        val parentY = y shr zoomDiff
        val parentUrl = buildUrl(parentX, parentY, parentZoom) ?: return null

        val renderCacheKey = "${parentUrl}_${zoom}_${x}_${y}"
        synchronized(renderedTileCache) {
            renderedTileCache.get(renderCacheKey)?.let {
                return com.google.android.gms.maps.model.Tile(256, 256, it)
            }
        }

        val rawBytes = getOrDownloadBytes(parentUrl) ?: return null
        if (!isValidImageBytes(rawBytes)) return null

        var parentBitmap: Bitmap? = synchronized(bitmapCache) { bitmapCache.get(parentUrl) }
        if (parentBitmap == null || parentBitmap.isRecycled) {
            val bytes = if (applyJmaRainAlpha) processJmaTile(parentUrl, rawBytes) else rawBytes
            parentBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (parentBitmap != null) {
                synchronized(bitmapCache) { bitmapCache.put(parentUrl, parentBitmap) }
            }
        }
        if (parentBitmap == null || parentBitmap.isRecycled) return null

        val pWidth = parentBitmap.width
        val pHeight = parentBitmap.height
        if (pWidth <= 0 || pHeight <= 0) return null

        return try {
            val subW = pWidth shr zoomDiff
            val subH = pHeight shr zoomDiff
            if (subW <= 1 || subH <= 1 || zoomDiff >= 8) {
                val px = ((x.toLong() and ((1L shl zoomDiff) - 1)) * pWidth.toLong() shr zoomDiff).toInt().coerceIn(0, pWidth - 1)
                val py = ((y.toLong() and ((1L shl zoomDiff) - 1)) * pHeight.toLong() shr zoomDiff).toInt().coerceIn(0, pHeight - 1)
                val pixelColor = parentBitmap.getPixel(px, py)
                if ((pixelColor ushr 24) == 0) {
                    synchronized(renderedTileCache) { renderedTileCache.put(renderCacheKey, TRANSPARENT_256_PNG_BYTES) }
                    com.google.android.gms.maps.model.Tile(256, 256, TRANSPARENT_256_PNG_BYTES)
                } else {
                    val solidBytes = getOrCreateSolidColorTile(pixelColor)
                    synchronized(renderedTileCache) { renderedTileCache.put(renderCacheKey, solidBytes) }
                    com.google.android.gms.maps.model.Tile(256, 256, solidBytes)
                }
            } else {
                val mask = (1 shl zoomDiff) - 1
                val safeSubX = ((x and mask) * subW).coerceIn(0, (pWidth - subW).coerceAtLeast(0))
                val safeSubY = ((y and mask) * subH).coerceIn(0, (pHeight - subH).coerceAtLeast(0))

                if (isRegionTransparent(parentBitmap, safeSubX, safeSubY, minOf(subW, subH))) {
                    synchronized(renderedTileCache) { renderedTileCache.put(renderCacheKey, TRANSPARENT_256_PNG_BYTES) }
                    return com.google.android.gms.maps.model.Tile(256, 256, TRANSPARENT_256_PNG_BYTES)
                }

                var cropped: Bitmap? = null
                var scaled: Bitmap? = null
                try {
                    cropped = Bitmap.createBitmap(parentBitmap, safeSubX, safeSubY, subW, subH)
                    val shouldFilter = !isRadarSource && zoomDiff >= 2
                    scaled = Bitmap.createScaledBitmap(cropped, 256, 256, shouldFilter)
                    val stream = java.io.ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val resultBytes = stream.toByteArray()
                    if (resultBytes.isNotEmpty()) {
                        synchronized(renderedTileCache) { renderedTileCache.put(renderCacheKey, resultBytes) }
                        com.google.android.gms.maps.model.Tile(256, 256, resultBytes)
                    } else {
                        null
                    }
                } finally {
                    if (scaled != null && scaled != parentBitmap && !scaled.isRecycled) {
                        scaled.recycle()
                    }
                    if (cropped != null && cropped != scaled && cropped != parentBitmap && !cropped.isRecycled) {
                        cropped.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * zoom 3 用: zoom 4 の 4枚の子タイルを縮小合成して 256x256 タイルを生成
     */
    private fun buildZoom3JmaTile(x: Int, y: Int, applyLightRainAlpha: Boolean): com.google.android.gms.maps.model.Tile? {
        val renderCacheKey = "z3_${frame.path}_${x}_${y}"
        synchronized(renderedTileCache) {
            renderedTileCache.get(renderCacheKey)?.let {
                return com.google.android.gms.maps.model.Tile(256, 256, it)
            }
        }

        val childZoom = 4
        val subTiles = mutableListOf<Pair<Pair<Int, Int>, Bitmap>>()
        var anyData = false

        for (dy in 0..1) {
            for (dx in 0..1) {
                val cx = (x shl 1) + dx
                val cy = (y shl 1) + dy
                val url = buildUrl(cx, cy, childZoom) ?: continue
                val rawBytes = getOrDownloadBytes(url) ?: continue
                if (isValidImageBytes(rawBytes)) {
                    anyData = true
                    val processed = if (applyLightRainAlpha) processJmaTile(url, rawBytes) else rawBytes
                    val bmp = BitmapFactory.decodeByteArray(processed, 0, processed.size)
                    if (bmp != null) {
                        subTiles.add(Pair(Pair(dx, dy), bmp))
                    }
                }
            }
        }

        if (!anyData || subTiles.isEmpty()) {
            subTiles.forEach { if (!it.second.isRecycled) it.second.recycle() }
            synchronized(renderedTileCache) { renderedTileCache.put(renderCacheKey, TRANSPARENT_256_PNG_BYTES) }
            return com.google.android.gms.maps.model.Tile(256, 256, TRANSPARENT_256_PNG_BYTES)
        }

        val mergedBmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        return try {
            val canvas = Canvas(mergedBmp)
            val paint = Paint().apply {
                isFilterBitmap = false
                isAntiAlias = false
            }

            for ((pos, bmp) in subTiles) {
                val (dx, dy) = pos
                val scaled = Bitmap.createScaledBitmap(bmp, 128, 128, false)
                try {
                    canvas.drawBitmap(scaled, dx * 128f, dy * 128f, paint)
                } finally {
                    if (scaled != bmp && !scaled.isRecycled) scaled.recycle()
                }
            }

            val stream = java.io.ByteArrayOutputStream()
            mergedBmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val resultBytes = stream.toByteArray()
            synchronized(renderedTileCache) {
                renderedTileCache.put(renderCacheKey, resultBytes)
            }
            com.google.android.gms.maps.model.Tile(256, 256, resultBytes)
        } catch (e: Exception) {
            null
        } finally {
            subTiles.forEach { if (!it.second.isRecycled) it.second.recycle() }
            if (!mergedBmp.isRecycled) mergedBmp.recycle()
        }
    }

    private fun buildUrl(x: Int, y: Int, z: Int): String? {
        return if (!frame.tileUrlTemplate.isNullOrBlank()) {
            val nowSec = System.currentTimeMillis() / 1000L
            val diffMin = ((frame.time - nowSec) / 60.0).coerceAtLeast(0.0)
            val snappedMin = (Math.round(diffMin / 15.0) * 15).toInt().coerceIn(0, 1080)
            val fminStr = String.format(java.util.Locale.US, "F%04d", snappedMin)

            val dateObj = java.util.Date(frame.time * 1000L)
            val timeIso = isoUtcFormatLocal.get()!!.format(dateObj)
            val utcCompact = utcCompactFormatLocal.get()!!.format(dateObj)
            val timeParam = if (Math.abs(frame.time - nowSec) < 300) "0" else utcCompact

            frame.tileUrlTemplate
                .replace("{timestamp}", timeIso)
                .replace("{time}", timeParam)
                .replace("{fmin}", fminStr)
                .replace("{epoch}", frame.time.toString())
                .replace("{z}", z.toString())
                .replace("{x}", x.toString())
                .replace("{y}", y.toString())
        } else if (frame.path.startsWith("http")) {
            val nowSec = System.currentTimeMillis() / 1000L
            val diffMin = ((frame.time - nowSec) / 60.0).coerceAtLeast(0.0)
            val snappedMin = (Math.round(diffMin / 15.0) * 15).toInt().coerceIn(0, 1080)
            val fminStr = String.format(java.util.Locale.US, "F%04d", snappedMin)

            val dateObj = java.util.Date(frame.time * 1000L)
            val timeIso = isoUtcFormatLocal.get()!!.format(dateObj)
            val utcCompact = utcCompactFormatLocal.get()!!.format(dateObj)
            val timeParam = if (Math.abs(frame.time - nowSec) < 300) "0" else utcCompact

            frame.path
                .replace("{timestamp}", timeIso)
                .replace("{time}", timeParam)
                .replace("{fmin}", fminStr)
                .replace("{epoch}", frame.time.toString())
                .replace("{z}", z.toString())
                .replace("{x}", x.toString())
                .replace("{y}", y.toString())
        } else {
            val cleanHost = host.trimEnd('/')
            val cleanPath = if (frame.path.startsWith('/')) frame.path else "/${frame.path}"
            "$cleanHost$cleanPath/256/$z/$x/$y/${colorScheme.id}/1_1.png"
        }
    }

    private fun getOrDownloadBytes(urlString: String): ByteArray? {
        synchronized(rawBytesCache) {
            rawBytesCache.get(urlString)?.let { return it }
        }

        TileDiskCache.get(urlString)?.let { diskBytes ->
            if (isValidImageBytes(diskBytes)) {
                synchronized(rawBytesCache) {
                    rawBytesCache.put(urlString, diskBytes)
                }
                return diskBytes
            } else {
                TileDiskCache.remove(urlString)
            }
        }

        val lock = downloadLockStripes[(urlString.hashCode() and 0x7fffffff) % downloadLockStripes.size]
        return synchronized(lock) {
            synchronized(rawBytesCache) {
                rawBytesCache.get(urlString)?.let { return it }
            }
            TileDiskCache.get(urlString)?.let { diskBytes ->
                if (isValidImageBytes(diskBytes)) {
                    synchronized(rawBytesCache) {
                        rawBytesCache.put(urlString, diskBytes)
                    }
                    return diskBytes
                }
            }

            val downloaded = RadarTileHttpClient.getTileBytes(urlString)
            if (downloaded != null && isValidImageBytes(downloaded)) {
                synchronized(rawBytesCache) {
                    rawBytesCache.put(urlString, downloaded)
                }
                TileDiskCache.put(urlString, downloaded)
            }
            downloaded
        }
    }

    private fun processJmaTile(url: String, bytes: ByteArray): ByteArray {
        if (bytes.size <= 350) return bytes
        synchronized(processedBytesCache) {
            processedBytesCache.get(url)?.let { return it }
        }

        var bitmap: Bitmap? = null
        var mutableBmp: Bitmap? = null
        return try {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return bytes
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            var modified = false
            for (i in pixels.indices) {
                val p = pixels[i]
                val a = (p ushr 24)
                if (a > 0) {
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    // JMA Color 2: (242, 242, 255) -> 微弱な小雨・霧雨 (0.1〜1mm/h)
                    // 背景地図が塗り潰されてしまわないよう半透明化（アルファ値 65）
                    if (r >= 235 && g >= 235 && b >= 245) {
                        pixels[i] = (65 shl 24) or (p and 0x00FFFFFF)
                        modified = true
                    }
                }
            }
            val result = if (!modified) {
                bytes
            } else {
                mutableBmp = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
                val stream = java.io.ByteArrayOutputStream()
                mutableBmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
            synchronized(processedBytesCache) {
                processedBytesCache.put(url, result)
            }
            result
        } catch (e: Exception) {
            bytes
        } finally {
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
            if (mutableBmp != null && !mutableBmp.isRecycled) mutableBmp.recycle()
        }
    }
}

/**
 * 現在地を示すシアン・ブルーの発光ドットマーカー用 Bitmap を生成
 */
private fun createLocationMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (28 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    // 1. 外側のソフトグロー（薄いシアン #38BDF8）
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(80, 56, 189, 248)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center, glowPaint)

    // 2. 白いリング
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, 9 * density, whitePaint)

    // 3. 中心のアクセントドット（シアンブルー #0284C7）
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0284C7")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, 6.5f * density, dotPaint)

    return bitmap
}

/**
 * Expressive Light テーマに調和する Google Maps スタイル JSON
 * - 地図を淡くクリーンなライトグレー/ホワイトトーンに整え、雨雲レーダーの視認性を最大化
 * - POI（店舗・施設）や公共交通機関を非表示にして雨雲の重なりを邪魔しない設計
 * - 水域（海・湖）は優しいソフトブルー、地名ラベルは高コントラストなスレートグレー
 */
private const val LIGHT_MAP_STYLE_JSON = """[
  {
    "elementType": "geometry",
    "stylers": [
      { "color": "#f8fafc" }
    ]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#334155" }
    ]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [
      { "color": "#ffffff" },
      { "weight": 2 }
    ]
  },
  {
    "featureType": "administrative.country",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#cbd5e1" },
      { "weight": 1 }
    ]
  },
  {
    "featureType": "administrative.province",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#e2e8f0" },
      { "weight": 1 }
    ]
  },
  {
    "featureType": "administrative.locality",
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#1e293b" }
    ]
  },
  {
    "featureType": "poi",
    "stylers": [
      { "visibility": "off" }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [
      { "color": "#ffffff" }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#e2e8f0" }
    ]
  },
  {
    "featureType": "road",
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#64748b" }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [
      { "color": "#f1f5f9" }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#cbd5e1" }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#334155" }
    ]
  },
  {
    "featureType": "transit",
    "stylers": [
      { "visibility": "off" }
    ]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [
      { "color": "#c8dcfa" }
    ]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#0284c7" }
    ]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.stroke",
    "stylers": [
      { "color": "#c8dcfa" }
    ]
  }
]"""

/**
 * Expressive Dark テーマに調和する Google Maps スタイル JSON
 */
private const val DARK_MAP_STYLE_JSON = """[
  {
    "elementType": "geometry",
    "stylers": [
      { "color": "#151c28" }
    ]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#8a9bb0" }
    ]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [
      { "color": "#151c28" }
    ]
  },
  {
    "featureType": "administrative.country",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#334155" },
      { "weight": 1 }
    ]
  },
  {
    "featureType": "administrative.province",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#283548" },
      { "weight": 1 }
    ]
  },
  {
    "featureType": "administrative.locality",
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#cbd5e1" }
    ]
  },
  {
    "featureType": "poi",
    "stylers": [
      { "visibility": "off" }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [
      { "color": "#242f3e" }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#1a2332" }
    ]
  },
  {
    "featureType": "road",
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#94a3b8" }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [
      { "color": "#334155" }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry.stroke",
    "stylers": [
      { "color": "#1e293b" }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#f1f5f9" }
    ]
  },
  {
    "featureType": "transit",
    "stylers": [
      { "visibility": "off" }
    ]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [
      { "color": "#0e1626" }
    ]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.fill",
    "stylers": [
      { "color": "#475569" }
    ]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.stroke",
    "stylers": [
      { "color": "#0e1626" }
    ]
  }
]"""
