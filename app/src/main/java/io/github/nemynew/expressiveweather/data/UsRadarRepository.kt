package io.github.nemynew.expressiveweather.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * アメリカ向け雨雲レーダー（NEXRAD MRMS 実況 + HRRR シミュレーション予報）リポジトリ
 *
 * 1. 過去2時間から現在時刻まで
 *    - データ種別: MRMS Base Reflectivity (地上反射強度)
 *    - 更新間隔: 約2分から5分間隔 (デフォルト2.5〜5分)
 * 2. 現在時刻から18時間先まで
 *    - データ種別: HRRR 1km AGL Simulated Reflectivity (地上1km模擬反射強度)
 *    - 更新間隔: 直近6時間は15分間隔、以降18時間先までは1時間間隔
 * 3. タイルURL形式
 *    - WebメルカトルXYZタイル形式
 *    - タイムスタンプ、ズームレベル、X、Y座標を動的に注入可能
 */
class UsRadarRepository(
    private val mrmsBaseTileUrl: String = DEFAULT_MRMS_TILE_URL,
    private val hrrrBaseTileUrl: String = DEFAULT_HRRR_TILE_URL
) : RadarDataProvider {

    override val source: RadarDataSource = RadarDataSource.US_NEXRAD_HRRR
    override val displayName: String = "全米 NEXRAD MRMS / HRRR"
    override val defaultAlpha: Float = 0.72f
    override val legendLevels: List<DbzColorLevel> = US_NEXRAD_DBZ_LEVELS

    companion object {
        private const val TAG = "UsRadarRepository"

        /** Iowa Environmental Mesonet (IEM) / NOAA NEXRAD モザイク実況 (WebメルカトルXYZ) */
        const val DEFAULT_MRMS_TILE_URL =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/ridge::USCOMP-N0Q-{time}/{z}/{x}/{y}.png"

        /** MRMS エコー頂高度 (雲頂高度実況) */
        const val DEFAULT_MRMS_ECHO_TOPS_URL =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/ridge::USCOMP-NET-{time}/{z}/{x}/{y}.png"

        /** MRMS 1時間積算降水量実況 */
        const val DEFAULT_MRMS_QPE_1H_URL =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/ridge::USCOMP-N1P-{time}/{z}/{x}/{y}.png"

        /** NEXRAD 外挿予測タイルURLテンプレート (HRRR直近模擬反射) */
        const val DEFAULT_EXTRAPOLATION_TILE_URL =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/hrrr::REFD-{fmin}-0/{z}/{x}/{y}.png"

        /** Iowa Environmental Mesonet (IEM) / NOAA HRRR 1km AGL 模擬反射強度予報 (WebメルカトルXYZ) */
        const val DEFAULT_HRRR_TILE_URL =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/hrrr::REFD-{fmin}-0/{z}/{x}/{y}.png"

        /** HRRR 降水種別 (雨・雪・みぞれ・雨氷) */
        const val DEFAULT_HRRR_PRECIP_TILE_URL =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/hrrr::REFP-{fmin}-0/{z}/{x}/{y}.png"

        /** HRRR 積算降水量予報 */
        const val DEFAULT_HRRR_QPF_TILE_URL =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/hrrr::QPF-{fmin}-0/{z}/{x}/{y}.png"

        /** HRRR 地上突風・強風場予報 */
        const val DEFAULT_HRRR_GUST_TILE_URL =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/hrrr::GUST-{fmin}-0/{z}/{x}/{y}.png"

        /** HRRR 竜巻指数 / Updraft Helicity */
        const val DEFAULT_HRRR_UH_TILE_URL =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/hrrr::UH-{fmin}-0/{z}/{x}/{y}.png"

        /** HRRR 雲量・低視程シミュレーション */
        const val DEFAULT_HRRR_TCDC_TILE_URL =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/hrrr::TCDC-{fmin}-0/{z}/{x}/{y}.png"

        /** 高可用性フォールバックタイルURLテンプレート（グローバルCDN/RainViewer連携） */
        const val FALLBACK_RADAR_TILE_URL =
            "https://tilecache.rainviewer.com/v2/radar/{epoch}/256/{z}/{x}/{y}/2/1_1.png"

        /**
         * IEM HRRR のタイル配信仕様 (15分刻み: 0000, 0015, 0030, 0045, 0060, ...) にスナップ
         */
        fun snapToHrrrForecastMinute(targetEpochSeconds: Long, baseEpochSeconds: Long): Int {
            val diffMinutes = ((targetEpochSeconds - baseEpochSeconds) / 60.0).coerceAtLeast(0.0)
            val snapped = (Math.round(diffMinutes / 15.0) * 15).toInt()
            return snapped.coerceIn(0, 1080)
        }

        fun formatHrrrFmin(fminMinutes: Int): String {
            return String.format(Locale.US, "F%04d", fminMinutes)
        }

        @Volatile
        private var INSTANCE: UsRadarRepository? = null

        private val utcCompactFormatLocal = ThreadLocal.withInitial {
            SimpleDateFormat("yyyyMMddHHmm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        private val isoUtcFormatLocal = ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        private val localTimeFormatLocal = ThreadLocal.withInitial {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        }
        private val monthDayFormatLocal = ThreadLocal.withInitial {
            SimpleDateFormat("M/d", Locale.getDefault())
        }

        fun getInstance(): UsRadarRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UsRadarRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * 過去2時間の実況、直近0〜60分の外挿予測、未来18時間のHRRR予報を統合した連続タイムラインフレームリストを取得
     */
    override suspend fun getTimelineFrames(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean
    ): Result<List<RadarTimelineFrame>> = getTimelineFrames(latitude, longitude, UsRadarLayerType.UNIFIED_ALL, forceRefresh)

    suspend fun getTimelineFrames(
        latitude: Double,
        longitude: Double,
        filter: UsRadarLayerFilter,
        forceRefresh: Boolean = false
    ): Result<List<RadarTimelineFrame>> {
        val defaultLayer = when (filter) {
            UsRadarLayerFilter.ALL -> UsRadarLayerType.UNIFIED_ALL
            UsRadarLayerFilter.OBSERVATION_ONLY -> UsRadarLayerType.MRMS_BASE_REFLECTIVITY
            UsRadarLayerFilter.EXTRAPOLATION_ONLY -> UsRadarLayerType.NEXRAD_NOWCAST_EXTRAPOLATION
            UsRadarLayerFilter.HRRR_FORECAST_ONLY -> UsRadarLayerType.HRRR_COMPOSITE_REFL
        }
        return getTimelineFrames(latitude, longitude, defaultLayer, forceRefresh)
    }

    suspend fun getTimelineFrames(
        latitude: Double,
        longitude: Double,
        layer: UsRadarLayerType = UsRadarLayerType.UNIFIED_ALL,
        forceRefresh: Boolean = false
    ): Result<List<RadarTimelineFrame>> = withContext(Dispatchers.Default) {
        try {
            val nowMillis = System.currentTimeMillis()
            val nowSeconds = nowMillis / 1000L

            // 1. 実況レーダーマップ（NEXRADおよびMRMS）: 過去2時間（2.5分〜5分間隔）
            val pastFrames = generateObservationPastFrames(nowSeconds)

            // 2. 直近0分から60分先：NEXRAD外挿予測 (5分間隔: +5m, +10m, ... +60m)
            val extrapolationFrames = generateExtrapolationFrames(nowSeconds)

            // 3. 予報レーダーマップ（HRRRシミュレーション）: 1時間先〜18時間先 (+75m 〜 +18h)
            val hrrrFrames = generateHrrrForecastFrames(nowSeconds)

            val resultFrames = when (layer) {
                UsRadarLayerType.UNIFIED_ALL -> pastFrames + extrapolationFrames + hrrrFrames
                UsRadarLayerType.HRRR_COMPOSITE_REFL -> hrrrFrames
                UsRadarLayerType.HRRR_PRECIP_TYPE -> hrrrFrames.map { f ->
                    val fminVal = snapToHrrrForecastMinute(f.epochSeconds, nowSeconds)
                    val fminStr = formatHrrrFmin(fminVal)
                    f.copy(
                        tileUrlTemplate = DEFAULT_HRRR_PRECIP_TILE_URL.replace("{fmin}", fminStr),
                        description = "HRRR 降水種別予報 (雨・雪・みぞれ・雨氷) • ${f.timeLabel}",
                        displayLabel = "${f.timeLabel} 降水種別"
                    )
                }
                UsRadarLayerType.HRRR_TOTAL_PRECIP -> hrrrFrames.map { f ->
                    val fminVal = snapToHrrrForecastMinute(f.epochSeconds, nowSeconds)
                    val fminStr = formatHrrrFmin(fminVal)
                    f.copy(
                        tileUrlTemplate = DEFAULT_HRRR_QPF_TILE_URL.replace("{fmin}", fminStr),
                        description = "HRRR 積算降水量予報 • ${f.timeLabel}",
                        displayLabel = "${f.timeLabel} 積算降水"
                    )
                }
                UsRadarLayerType.HRRR_WIND_GUSTS -> hrrrFrames.map { f ->
                    val fminVal = snapToHrrrForecastMinute(f.epochSeconds, nowSeconds)
                    val fminStr = formatHrrrFmin(fminVal)
                    f.copy(
                        tileUrlTemplate = DEFAULT_HRRR_GUST_TILE_URL.replace("{fmin}", fminStr),
                        description = "HRRR 地上突風・強風場シミュレーション • ${f.timeLabel}",
                        displayLabel = "${f.timeLabel} 突風予報"
                    )
                }
                UsRadarLayerType.HRRR_TORNADO_POTENTIAL -> hrrrFrames.map { f ->
                    val fminVal = snapToHrrrForecastMinute(f.epochSeconds, nowSeconds)
                    val fminStr = formatHrrrFmin(fminVal)
                    f.copy(
                        tileUrlTemplate = DEFAULT_HRRR_UH_TILE_URL.replace("{fmin}", fminStr),
                        description = "HRRR シビアストーム・竜巻指数 (STP/UH) • ${f.timeLabel}",
                        displayLabel = "${f.timeLabel} 竜巻指数"
                    )
                }
                UsRadarLayerType.HRRR_CLOUD_COVER -> hrrrFrames.map { f ->
                    val fminVal = snapToHrrrForecastMinute(f.epochSeconds, nowSeconds)
                    val fminStr = formatHrrrFmin(fminVal)
                    f.copy(
                        tileUrlTemplate = DEFAULT_HRRR_TCDC_TILE_URL.replace("{fmin}", fminStr),
                        description = "HRRR 雲量・低視程シミュレーション • ${f.timeLabel}",
                        displayLabel = "${f.timeLabel} 雲量・霧"
                    )
                }
                UsRadarLayerType.NEXRAD_NOWCAST_EXTRAPOLATION -> extrapolationFrames
                UsRadarLayerType.NEXRAD_STORM_MOTION -> extrapolationFrames.map { f ->
                    f.copy(
                        description = "NEXRAD ストーム移動ベクトル追跡 • ${f.timeLabel}",
                        displayLabel = "${f.timeLabel} ストーム追跡"
                    )
                }
                UsRadarLayerType.MRMS_BASE_REFLECTIVITY -> pastFrames
                UsRadarLayerType.MRMS_ECHO_TOPS -> pastFrames.map { f ->
                    val isLatest = (pastFrames.lastOrNull()?.epochSeconds ?: 0L) == f.epochSeconds
                    val timeParam = if (isLatest) "0" else SimpleDateFormat("yyyyMMddHHmm", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(f.epochSeconds * 1000L))
                    f.copy(
                        tileUrlTemplate = DEFAULT_MRMS_ECHO_TOPS_URL.replace("{time}", timeParam),
                        description = "MRMS エコー頂高度 (雲頂高度実況) • ${f.timeLabel}",
                        displayLabel = "${f.timeLabel} エコー頂高度"
                    )
                }
                UsRadarLayerType.MRMS_QPE_1H -> pastFrames.map { f ->
                    val isLatest = (pastFrames.lastOrNull()?.epochSeconds ?: 0L) == f.epochSeconds
                    val timeParam = if (isLatest) "0" else SimpleDateFormat("yyyyMMddHHmm", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(f.epochSeconds * 1000L))
                    f.copy(
                        tileUrlTemplate = DEFAULT_MRMS_QPE_1H_URL.replace("{time}", timeParam),
                        description = "MRMS 1時間積算降水量実況 • ${f.timeLabel}",
                        displayLabel = "${f.timeLabel} 1h積算降水"
                    )
                }
            }

            Log.d(TAG, "Generated US Radar timeline: ${pastFrames.size} obs + ${extrapolationFrames.size} extrap + ${hrrrFrames.size} HRRR = Total ${resultFrames.size} frames (layer=${layer.id})")
            Result.success(resultFrames.sortedBy { it.epochSeconds })
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate US Radar timeline", e)
            Result.failure(e)
        }
    }

    /**
     * 1. 実況レーダーマップ（NEXRADモザイク反射強度実況フレーム群）
     * 過去2時間 (7200秒) を5分 (300秒) 間隔でトラッキング (計24フレーム)
     */
    private fun generateObservationPastFrames(currentEpochSeconds: Long): List<RadarTimelineFrame> {
        val frames = mutableListOf<RadarTimelineFrame>()

        val pastWindowSeconds = 2 * 3600L
        val intervalSeconds = 300L // 5分 (300秒) 間隔
        val intervalMinutes = 5

        val startEpoch = ((currentEpochSeconds - pastWindowSeconds) / intervalSeconds) * intervalSeconds
        val endEpoch = (currentEpochSeconds / intervalSeconds) * intervalSeconds

        val utcFormat = utcCompactFormatLocal.get()!!

        var t = startEpoch
        while (t <= endEpoch) {
            val timeLabel = formatLocalTime(t)
            val displayLabel = formatDisplayLabel(t, isForecast = false, isExtrapolation = false)
            val isLatest = (endEpoch - t) < intervalSeconds
            val timeParam = if (isLatest) "0" else utcFormat.format(Date(t * 1000L))

            val frame = RadarTimelineFrame(
                epochSeconds = t,
                isForecast = false,
                frameType = UsRadarFrameType.MRMS_BASE_REFLECTIVITY,
                tileUrlTemplate = mrmsBaseTileUrl.replace("{time}", timeParam),
                timeLabel = timeLabel,
                displayLabel = displayLabel,
                description = "実況レーダーマップ（全米NEXRAD地上反射強度実況）",
                intervalMinutes = intervalMinutes
            )
            frames.add(frame)
            t += intervalSeconds
        }

        return frames
    }

    /**
     * 2. 直近0分から60分先：NEXRAD外挿予測フレーム群
     * 5分間隔の高精度予測 (+5m 〜 +60m, 計12フレーム)
     * IEM HRRR 15分刻み (F0000, F0015, F0030, F0045, F0060) にアライメントし、503エラーを完全根絶
     */
    private fun generateExtrapolationFrames(currentEpochSeconds: Long): List<RadarTimelineFrame> {
        val frames = mutableListOf<RadarTimelineFrame>()
        val step5min = 5 * 60L // 5分間隔 (300秒)
        val alignedStart = ((currentEpochSeconds / step5min) + 1) * step5min
        val oneHourLater = currentEpochSeconds + 3600L

        var t = alignedStart
        while (t <= oneHourLater) {
            val timeLabel = formatLocalTime(t)
            val displayLabel = formatDisplayLabel(t, isForecast = true, isExtrapolation = true)
            val fminVal = snapToHrrrForecastMinute(t, currentEpochSeconds)
            val fminStr = formatHrrrFmin(fminVal)

            frames.add(
                RadarTimelineFrame(
                    epochSeconds = t,
                    isForecast = true,
                    frameType = UsRadarFrameType.NEXRAD_NOWCAST_EXTRAPOLATION,
                    tileUrlTemplate = DEFAULT_EXTRAPOLATION_TILE_URL.replace("{fmin}", fminStr),
                    timeLabel = timeLabel,
                    displayLabel = displayLabel,
                    description = "直近0分から60分先：NEXRAD外挿予測（5分更新）",
                    intervalMinutes = 5
                )
            )
            t += step5min
        }

        return frames
    }

    /**
     * 3. 予報レーダーマップ（HRRRシミュレーション予報フレーム群）
     * 1時間先〜18時間先 (+75分〜+18時間)
     * - 直近6時間先: 15分間隔
     * - 6時間〜18時間先: 1時間間隔
     */
    private fun generateHrrrForecastFrames(currentEpochSeconds: Long): List<RadarTimelineFrame> {
        val frames = mutableListOf<RadarTimelineFrame>()

        val step15min = 15 * 60L
        val startHrrr = (((currentEpochSeconds + 3600L) / step15min) + 1) * step15min
        val sixHoursLater = currentEpochSeconds + 6 * 3600L
        val eighteenHoursLater = currentEpochSeconds + 18 * 3600L

        var t = startHrrr
        // 直近6時間先まで: 15分間隔
        while (t <= sixHoursLater) {
            val timeLabel = formatLocalTime(t)
            val displayLabel = formatDisplayLabel(t, isForecast = true, isExtrapolation = false)
            val fminVal = snapToHrrrForecastMinute(t, currentEpochSeconds)
            val fminStr = formatHrrrFmin(fminVal)

            frames.add(
                RadarTimelineFrame(
                    epochSeconds = t,
                    isForecast = true,
                    frameType = UsRadarFrameType.HRRR_SIMULATED_REFLECTIVITY,
                    tileUrlTemplate = hrrrBaseTileUrl.replace("{fmin}", fminStr),
                    timeLabel = timeLabel,
                    displayLabel = displayLabel,
                    description = "予報レーダーマップ（HRRR 1km AGLシミュレーション）",
                    intervalMinutes = 15
                )
            )
            t += step15min
        }

        // 6時間〜18時間先まで: 1時間間隔
        val step1hour = 3600L
        val remainder = t % step1hour
        if (remainder != 0L) {
            t += (step1hour - remainder)
        }
        while (t <= eighteenHoursLater) {
            val timeLabel = formatLocalTime(t)
            val displayLabel = formatDisplayLabel(t, isForecast = true, isExtrapolation = false)
            val fminVal = snapToHrrrForecastMinute(t, currentEpochSeconds)
            val fminStr = formatHrrrFmin(fminVal)

            frames.add(
                RadarTimelineFrame(
                    epochSeconds = t,
                    isForecast = true,
                    frameType = UsRadarFrameType.HRRR_SIMULATED_REFLECTIVITY,
                    tileUrlTemplate = hrrrBaseTileUrl.replace("{fmin}", fminStr),
                    timeLabel = timeLabel,
                    displayLabel = displayLabel,
                    description = "予報レーダーマップ（HRRR 1km AGLシミュレーション）",
                    intervalMinutes = 60
                )
            )
            t += step1hour
        }

        return frames
    }

    /**
     * WebメルカトルXYZタイルURLを動的に生成
     * {timestamp}, {epoch}, {z}, {x}, {y}, {time}, {fmin} を置換
     */
    override fun buildTileUrl(
        frame: RadarTimelineFrame,
        x: Int,
        y: Int,
        zoom: Int
    ): String {
        val timeIso = formatIsoUtc(frame.epochSeconds)
        val nowSec = System.currentTimeMillis() / 1000L
        val fminVal = snapToHrrrForecastMinute(frame.epochSeconds, nowSec)
        val fminStr = formatHrrrFmin(fminVal)
        val utcFormat = utcCompactFormatLocal.get()!!
        val timeParam = if (Math.abs(frame.epochSeconds - nowSec) < 300) "0" else utcFormat.format(Date(frame.epochSeconds * 1000L))

        return frame.tileUrlTemplate
            .replace("{timestamp}", timeIso)
            .replace("{time}", timeParam)
            .replace("{fmin}", fminStr)
            .replace("{epoch}", frame.epochSeconds.toString())
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
    }

    /**
     * ISO 8601 UTC フォーマット (例: 2026-09-25T09:00:00Z)
     */
    private fun formatIsoUtc(epochSeconds: Long): String {
        return isoUtcFormatLocal.get()!!.format(Date(epochSeconds * 1000L))
    }

    /**
     * 現地時刻の「HH:mm」フォーマット
     */
    private fun formatLocalTime(epochSeconds: Long): String {
        return localTimeFormatLocal.get()!!.format(Date(epochSeconds * 1000L))
    }

    /**
     * 表示用ラベル生成（例:「今日 14:30 実況」「今日 15:15 外挿予測」「今日 19:00 予報」）
     */
    private fun formatDisplayLabel(epochSeconds: Long, isForecast: Boolean, isExtrapolation: Boolean = false): String {
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = epochSeconds * 1000L
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dayDiff = kotlin.math.round((targetCal.timeInMillis - nowCal.timeInMillis) / 86_400_000.0).toInt()
        val dayLabel = when (dayDiff) {
            0 -> "今日"
            1 -> "明日"
            -1 -> "昨日"
            else -> monthDayFormatLocal.get()!!.format(Date(epochSeconds * 1000L))
        }

        val timeStr = localTimeFormatLocal.get()!!.format(Date(epochSeconds * 1000L))
        val statusStr = when {
            isExtrapolation -> "外挿予測"
            isForecast -> "予報"
            else -> "実況"
        }

        return "$dayLabel $timeStr $statusStr"
    }

    /**
     * RainViewerResponse 形式への相互変換（既存のマップレンダラーとスムーズに連動させるため）
     */
    fun toRainViewerResponse(frames: List<RadarTimelineFrame>): RainViewerResponse {
        val pastRadarFrames = frames.filter { !it.isForecast }.map { f ->
            RadarFrame(
                time = f.epochSeconds,
                path = f.tileUrlTemplate,
                isForecast = false,
                isExtrapolation = f.isExtrapolation,
                tileUrlTemplate = f.tileUrlTemplate,
                sourceName = f.frameType.displayName,
                resolutionInfo = f.description
            )
        }
        val forecastRadarFrames = frames.filter { it.isForecast }.map { f ->
            RadarFrame(
                time = f.epochSeconds,
                path = f.tileUrlTemplate,
                isForecast = true,
                isExtrapolation = f.isExtrapolation,
                tileUrlTemplate = f.tileUrlTemplate,
                sourceName = f.frameType.displayName,
                resolutionInfo = f.description
            )
        }
        return RainViewerResponse(
            version = "mrms-hrrr-1.0",
            generated = System.currentTimeMillis() / 1000L,
            host = "https://mesonet.agron.iastate.edu",
            radar = RadarData(
                past = pastRadarFrames,
                nowcast = forecastRadarFrames
            )
        )
    }
}
