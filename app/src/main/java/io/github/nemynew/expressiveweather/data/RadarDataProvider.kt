package io.github.nemynew.expressiveweather.data

import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 雨雲レーダーのデータプロバイダー抽象インターフェース
 * 日本（気象庁ナウキャスト）、米国（NEXRAD / HRRR）、グローバル（RainViewer）を
 * 統一的に切り替え可能にする。
 */
interface RadarDataProvider {
    /** プロバイダー種別 */
    val source: RadarDataSource

    /** 表示名 */
    val displayName: String

    /** 推奨デフォルトアルファ値 (0.6f〜0.8f) */
    val defaultAlpha: Float

    /** dBZまたは降水強度凡例レベル一覧 */
    val legendLevels: List<DbzColorLevel>

    /**
     * 過去実況と未来予測が統合されたタイムラインフレーム群を取得
     *
     * @param latitude 緯度
     * @param longitude 経度
     * @param forceRefresh キャッシュを無視して最新データを取得するかどうか
     */
    suspend fun getTimelineFrames(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false
    ): Result<List<RadarTimelineFrame>>

    /**
     * WebメルカトルXYZタイルURLを生成
     *
     * @param frame 対象のレーダーフレーム
     * @param x タイルX座標
     * @param y タイルY座標
     * @param zoom ズームレベル
     */
    fun buildTileUrl(
        frame: RadarTimelineFrame,
        x: Int,
        y: Int,
        zoom: Int
    ): String
}

/**
 * 地域・設定に応じたプロバイダー決定ルーター
 */
object RadarProviderRegistry {
    private val providers = ConcurrentHashMap<RadarDataSource, RadarDataProvider>()
    @Volatile
    private var defaultsRegistered = false

    private fun ensureDefaultsRegistered() {
        if (!defaultsRegistered) {
            synchronized(this) {
                if (!defaultsRegistered) {
                    providers.putIfAbsent(RadarDataSource.US_NEXRAD_HRRR, UsRadarRepository.getInstance())
                    providers.putIfAbsent(RadarDataSource.JMA_NOWCAST, JmaRadarProvider.getInstance())
                    providers.putIfAbsent(RadarDataSource.RAIN_VIEWER, RainViewerRadarProvider.getInstance())
                    defaultsRegistered = true
                }
            }
        }
    }

    fun register(provider: RadarDataProvider) {
        providers[provider.source] = provider
    }

    fun get(source: RadarDataSource): RadarDataProvider? {
        ensureDefaultsRegistered()
        return providers[source]
    }

    /**
     * ユーザー指定モードおよび現在地座標・国情報から最適なプロバイダーを自動解決
     *
     * @param mode ユーザー設定モード (AUTO / JMA / US_NEXRAD_HRRR / RAIN_VIEWER)
     * @param latitude 緯度
     * @param longitude 経度
     * @param country 国名
     */
    fun resolveProvider(
        mode: RadarSourceMode,
        latitude: Double,
        longitude: Double,
        country: String? = null
    ): RadarDataSource {
        return when (mode) {
            RadarSourceMode.JMA -> RadarDataSource.JMA_NOWCAST
            RadarSourceMode.US_NEXRAD_HRRR -> RadarDataSource.US_NEXRAD_HRRR
            RadarSourceMode.RAIN_VIEWER -> RadarDataSource.RAIN_VIEWER
            RadarSourceMode.AUTO -> {
                if (isLocationOrCountryInJapan(country, latitude, longitude)) {
                    RadarDataSource.JMA_NOWCAST
                } else if (isLocationOrCountryInUnitedStates(country, latitude, longitude)) {
                    RadarDataSource.US_NEXRAD_HRRR
                } else {
                    RadarDataSource.RAIN_VIEWER
                }
            }
        }
    }
}

/**
 * 日本国内向け 気象庁高解像度降水ナウキャスト プロバイダー
 */
class JmaRadarProvider(
    private val jmaRepo: JmaNowcastRepository = JmaNowcastRepository.getInstance()
) : RadarDataProvider {
    override val source: RadarDataSource = RadarDataSource.JMA_NOWCAST
    override val displayName: String = "気象庁 高解像度降水ナウキャスト"
    override val defaultAlpha: Float = 0.68f
    override val legendLevels: List<DbzColorLevel> = US_NEXRAD_DBZ_LEVELS

    companion object {
        @Volatile
        private var INSTANCE: JmaRadarProvider? = null
        fun getInstance(): JmaRadarProvider = INSTANCE ?: synchronized(this) {
            INSTANCE ?: JmaRadarProvider().also { INSTANCE = it }
        }

        private val timeFormatLocal = ThreadLocal.withInitial {
            SimpleDateFormat("HH:mm", Locale.JAPAN)
        }
    }

    override suspend fun getTimelineFrames(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean
    ): Result<List<RadarTimelineFrame>> {
        return try {
            val res = jmaRepo.getRadarData(forceRefresh)
            val response = res.getOrThrow()
            val frames = mutableListOf<RadarTimelineFrame>()
            val timeFmt = timeFormatLocal.get()!!

            response.radar.past.forEach { f ->
                val timeStr = timeFmt.format(Date(f.time * 1000L))
                frames.add(
                    RadarTimelineFrame(
                        epochSeconds = f.time,
                        isForecast = false,
                        frameType = UsRadarFrameType.MRMS_BASE_REFLECTIVITY,
                        tileUrlTemplate = f.tileUrlTemplate ?: f.path,
                        timeLabel = timeStr,
                        displayLabel = "今日 $timeStr 実況",
                        description = "気象庁 降水ナウキャスト実況",
                        intervalMinutes = 5
                    )
                )
            }
            response.radar.nowcast.forEach { f ->
                val timeStr = timeFmt.format(Date(f.time * 1000L))
                frames.add(
                    RadarTimelineFrame(
                        epochSeconds = f.time,
                        isForecast = true,
                        frameType = UsRadarFrameType.HRRR_SIMULATED_REFLECTIVITY,
                        tileUrlTemplate = f.tileUrlTemplate ?: f.path,
                        timeLabel = timeStr,
                        displayLabel = "今日 $timeStr 予測",
                        description = "気象庁 降水ナウキャスト予報",
                        intervalMinutes = 5
                    )
                )
            }
            Result.success(frames)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun buildTileUrl(
        frame: RadarTimelineFrame,
        x: Int,
        y: Int,
        zoom: Int
    ): String {
        return frame.tileUrlTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
    }
}

/**
 * 全世界向け RainViewer プロバイダー
 */
class RainViewerRadarProvider(
    private val rainViewerRepo: RainViewerRepository = RainViewerRepository.getInstance()
) : RadarDataProvider {
    override val source: RadarDataSource = RadarDataSource.RAIN_VIEWER
    override val displayName: String = "RainViewer (Global)"
    override val defaultAlpha: Float = 0.60f
    override val legendLevels: List<DbzColorLevel> = US_NEXRAD_DBZ_LEVELS

    companion object {
        @Volatile
        private var INSTANCE: RainViewerRadarProvider? = null
        fun getInstance(): RainViewerRadarProvider = INSTANCE ?: synchronized(this) {
            INSTANCE ?: RainViewerRadarProvider().also { INSTANCE = it }
        }

        private val timeFormatLocal = ThreadLocal.withInitial {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        }
    }

    override suspend fun getTimelineFrames(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean
    ): Result<List<RadarTimelineFrame>> {
        return try {
            val res = rainViewerRepo.getRadarData(forceRefresh)
            val response = res.getOrThrow()
            val frames = mutableListOf<RadarTimelineFrame>()
            val timeFmt = timeFormatLocal.get()!!

            val safeHost = if (RadarUrlValidator.isValidRainViewerHost(response.host)) {
                response.host.trim().trimEnd('/')
            } else {
                "https://tilecache.rainviewer.com"
            }

            response.radar.past.forEach { f ->
                if (!RadarUrlValidator.isValidRainViewerPath(f.path)) return@forEach
                val template = "$safeHost${f.path}/256/{z}/{x}/{y}/2/1_1.png"
                val timeStr = timeFmt.format(Date(f.time * 1000L))
                frames.add(
                    RadarTimelineFrame(
                        epochSeconds = f.time,
                        isForecast = false,
                        frameType = UsRadarFrameType.MRMS_BASE_REFLECTIVITY,
                        tileUrlTemplate = template,
                        timeLabel = timeStr,
                        displayLabel = "今日 $timeStr 実況",
                        description = "RainViewer レーダー実況",
                        intervalMinutes = 10
                    )
                )
            }
            response.radar.nowcast.forEach { f ->
                if (!RadarUrlValidator.isValidRainViewerPath(f.path)) return@forEach
                val template = "$safeHost${f.path}/256/{z}/{x}/{y}/2/1_1.png"
                val timeStr = timeFmt.format(Date(f.time * 1000L))
                frames.add(
                    RadarTimelineFrame(
                        epochSeconds = f.time,
                        isForecast = true,
                        frameType = UsRadarFrameType.HRRR_SIMULATED_REFLECTIVITY,
                        tileUrlTemplate = template,
                        timeLabel = timeStr,
                        displayLabel = "今日 $timeStr 予測",
                        description = "RainViewer 予測",
                        intervalMinutes = 10
                    )
                )
            }
            Result.success(frames)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun buildTileUrl(
        frame: RadarTimelineFrame,
        x: Int,
        y: Int,
        zoom: Int
    ): String {
        return frame.tileUrlTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
    }
}
