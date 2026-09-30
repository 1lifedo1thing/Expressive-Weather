package com.example.expressiveweather.ui.radar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expressiveweather.data.DbzColorLevel
import com.example.expressiveweather.data.RadarDataProvider
import com.example.expressiveweather.data.RadarDataSource
import com.example.expressiveweather.data.RadarPlaybackState
import com.example.expressiveweather.data.RadarProviderRegistry
import com.example.expressiveweather.data.RadarSourceMode
import com.example.expressiveweather.data.RadarTileCacheManager
import com.example.expressiveweather.data.RadarTimelineFrame
import com.example.expressiveweather.data.US_NEXRAD_DBZ_LEVELS
import com.example.expressiveweather.data.UsRadarFrameType
import com.example.expressiveweather.data.UsRadarRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * アメリカ向け雨雲レーダー（MRMS実況 + HRRR予報）統合ViewModel
 *
 * 1. タイムラインStateの統合:
 *    過去の実況フレーム群と未来のHRRR予測フレーム群を一つの連続したタイムラインとして管理
 * 2. 再生制御ロジック:
 *    再生・一時停止・コマ送り・任意時刻シーク・18時間後到達時の先頭2時間前への滑らかなループ巻き戻し
 * 3. タイルのプリフェッチとメモリ制御:
 *    前後2フレームの自動先読みと、LRUキャッシュによる自動メモリ開放
 * 4. 日本・米国・海外のプロバイダー自動/明示的切り替え
 */
class UsRadarViewModel(
    private val tileCacheManager: RadarTileCacheManager = RadarTileCacheManager.getInstance()
) : ViewModel() {

    companion object {
        private const val TAG = "UsRadarViewModel"
        const val DEFAULT_ALPHA = 0.72f
        const val MIN_ALPHA = 0.60f
        const val MAX_ALPHA = 0.80f
        const val DEFAULT_PLAYBACK_SPEED_MS = 650L
    }

    // タイムラインフレーム一覧
    private val _timelineFrames = MutableStateFlow<List<RadarTimelineFrame>>(emptyList())
    val timelineFrames: StateFlow<List<RadarTimelineFrame>> = _timelineFrames.asStateFlow()

    // 現在選択中のフレームインデックス
    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    // 現在選択中のフレーム詳細
    val currentFrame: StateFlow<RadarTimelineFrame?> = combine(
        _timelineFrames,
        _currentFrameIndex
    ) { frames, index ->
        frames.getOrNull(index)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 再生状態
    private val _playbackState = MutableStateFlow(
        RadarPlaybackState(
            isPlaying = false,
            currentFrameIndex = 0,
            alpha = DEFAULT_ALPHA,
            playbackSpeedMs = DEFAULT_PLAYBACK_SPEED_MS,
            isLooping = true
        )
    )
    val playbackState: StateFlow<RadarPlaybackState> = _playbackState.asStateFlow()

    // 現在時刻境界（実況ゾーンと予測ゾーンの境界インデックス）
    private val _nowDividerIndex = MutableStateFlow(0)
    val nowDividerIndex: StateFlow<Int> = _nowDividerIndex.asStateFlow()

    // アクティブなデータプロバイダー
    private val _currentDataSource = MutableStateFlow(RadarDataSource.US_NEXRAD_HRRR)
    val currentDataSource: StateFlow<RadarDataSource> = _currentDataSource.asStateFlow()

    // ローディングおよびエラー状態
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 地図の中心座標およびズーム倍率
    private val _centerCoordinate = MutableStateFlow(Pair(37.7749, -122.4194)) // デフォルト例: サンフランシスコ
    val centerCoordinate: StateFlow<Pair<Double, Double>> = _centerCoordinate.asStateFlow()

    private val _zoomLevel = MutableStateFlow(7)
    val zoomLevel: StateFlow<Int> = _zoomLevel.asStateFlow()

    // dBZ凡例リスト
    val legendLevels: List<DbzColorLevel> = US_NEXRAD_DBZ_LEVELS

    // 自動再生用Coroutine Job
    private var playbackJob: Job? = null

    /**
     * 座標および国コード、ソースモードに応じたレーダータイムラインを取得・初期化
     */
    fun loadRadarTimeline(
        latitude: Double,
        longitude: Double,
        country: String? = null,
        mode: RadarSourceMode = RadarSourceMode.AUTO,
        forceRefresh: Boolean = false
    ) {
        _centerCoordinate.value = Pair(latitude, longitude)
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // 1. プロバイダーの自動解決 (日本: JMA, 米国: NEXRAD/HRRR, その他: RainViewer)
                val targetSource = RadarProviderRegistry.resolveProvider(mode, latitude, longitude, country)
                _currentDataSource.value = targetSource

                val provider = RadarProviderRegistry.get(targetSource)
                    ?: UsRadarRepository.getInstance()

                // 2. タイムラインフレーム群の取得 (過去2時間実況 + 未来18時間HRRR予測)
                val result = provider.getTimelineFrames(latitude, longitude, forceRefresh)

                result.fold(
                    onSuccess = { frames ->
                        if (frames.isNotEmpty()) {
                            _timelineFrames.value = frames

                            // 3. 現在時刻（実況ゾーンの末尾、予測直前）の境界インデックスを計算
                            val dividerIdx = frames.indexOfLast { !it.isForecast }.coerceAtLeast(0)
                            _nowDividerIndex.value = dividerIdx

                            // 初期位置を実況最新（現在時刻）に合わせる
                            _currentFrameIndex.value = dividerIdx
                            _playbackState.value = _playbackState.value.copy(
                                currentFrameIndex = dividerIdx
                            )

                            // 4. 初期表示フレーム周辺（前後2フレーム）のタイルを先読み
                            triggerPrefetch(dividerIdx, frames, provider)
                        } else {
                            _errorMessage.value = "レーダーフレームの取得に失敗しました"
                        }
                    },
                    onFailure = { err ->
                        Log.e(TAG, "Error fetching radar timeline", err)
                        _errorMessage.value = err.localizedMessage ?: "レーダーデータの取得に失敗しました"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in loadRadarTimeline", e)
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 再生開始
     */
    fun play() {
        if (_playbackState.value.isPlaying) return
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
        startPlaybackLoop()
    }

    /**
     * 一時停止
     */
    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    /**
     * 再生/一時停止トグル
     */
    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * コマ送り（1フレーム進める）
     */
    fun stepForward() {
        pause()
        val frames = _timelineFrames.value
        if (frames.isEmpty()) return
        val next = (_currentFrameIndex.value + 1) % frames.size
        seekToIndex(next)
    }

    /**
     * コマ戻し（1フレーム戻す）
     */
    fun stepBackward() {
        pause()
        val frames = _timelineFrames.value
        if (frames.isEmpty()) return
        val prev = if (_currentFrameIndex.value - 1 < 0) frames.lastIndex else _currentFrameIndex.value - 1
        seekToIndex(prev)
    }

    /**
     * タイムラインスライダー等による任意時刻シーク
     *
     * @param index 対象フレームインデックス
     */
    fun seekToIndex(index: Int) {
        val frames = _timelineFrames.value
        if (frames.isEmpty()) return
        val clampedIndex = index.coerceIn(frames.indices)
        _currentFrameIndex.value = clampedIndex
        _playbackState.value = _playbackState.value.copy(currentFrameIndex = clampedIndex)

        // 前後2フレームの先読みを即時実行
        val provider = RadarProviderRegistry.get(_currentDataSource.value)
            ?: UsRadarRepository.getInstance()
        triggerPrefetch(clampedIndex, frames, provider)
    }

    /**
     * 現在時刻（実況ゾーンの最新フレーム）へジャンプ
     */
    fun seekToNow() {
        pause()
        seekToIndex(_nowDividerIndex.value)
    }

    /**
     * 2時間前（先頭フレーム）へジャンプ
     */
    fun seekToStart() {
        pause()
        seekToIndex(0)
    }

    /**
     * 18時間後（末尾フレーム）へジャンプ
     */
    fun seekToEnd() {
        pause()
        val frames = _timelineFrames.value
        if (frames.isNotEmpty()) {
            seekToIndex(frames.lastIndex)
        }
    }

    /**
     * レーダータイルのアルファ値調整 (0.6f〜0.8f)
     */
    fun setAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(MIN_ALPHA, MAX_ALPHA)
        _playbackState.value = _playbackState.value.copy(alpha = clamped)
    }

    /**
     * 再生速度調整（ミリ秒）
     */
    fun setPlaybackSpeed(speedMs: Long) {
        _playbackState.value = _playbackState.value.copy(playbackSpeedMs = speedMs)
        if (_playbackState.value.isPlaying) {
            // 再起動して速度を即座に反映
            startPlaybackLoop()
        }
    }

    /**
     * 地図のカメラ位置・ズーム倍率更新
     */
    fun updateMapCamera(lat: Double, lon: Double, zoom: Int) {
        _centerCoordinate.value = Pair(lat, lon)
        _zoomLevel.value = zoom
    }

    /**
     * 再生ループ処理:
     * 末尾（18時間後）に到達したあと、先頭（2時間前）へ滑らかに巻き戻る
     */
    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                val speed = _playbackState.value.playbackSpeedMs
                delay(speed)

                val frames = _timelineFrames.value
                if (frames.isNotEmpty()) {
                    val curr = _currentFrameIndex.value
                    // 末尾到達時はインデックス0（2時間前）へ滑らかにループ
                    val next = if (curr >= frames.lastIndex) 0 else curr + 1

                    _currentFrameIndex.value = next
                    _playbackState.value = _playbackState.value.copy(currentFrameIndex = next)

                    val provider = RadarProviderRegistry.get(_currentDataSource.value)
                        ?: UsRadarRepository.getInstance()
                    triggerPrefetch(next, frames, provider)
                }
            }
        }
    }

    /**
     * 前後2フレーム (index ± 2) の非同期プリフェッチをトリガー
     */
    private fun triggerPrefetch(
        index: Int,
        frames: List<RadarTimelineFrame>,
        provider: RadarDataProvider
    ) {
        val (lat, lon) = _centerCoordinate.value
        val zoom = _zoomLevel.value
        tileCacheManager.prefetchSurroundingFrames(
            frames = frames,
            currentIndex = index,
            centerLat = lat,
            centerLon = lon,
            zoom = zoom,
            provider = provider
        )
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        tileCacheManager.clearCache()
    }
}
