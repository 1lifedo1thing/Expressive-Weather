package com.example.expressiveweather.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

@Serializable
private data class JmaTargetTime(
    val basetime: String = "",
    val validtime: String = "",
    val member: String = "none",
    val elements: List<String> = emptyList()
)

/**
 * 気象庁（JMA）気象タイルリポジトリ
 * 雨雲高解像度ナウキャスト（HR-PNS）、雷・竜巻ナウキャスト、キキクル（危険度分布）、
 * 気象衛星ひまわり（トゥルーカラー/赤外/水蒸気/雲頂）、今後の雨、雪情報、
 * 天気予報分布、海上分布予報、および各種背景地図タイルに対応。
 */
class JmaNowcastRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    // レイヤー別キャッシュ (キャッシュ有効期限: 3分)
    private val cacheMap = ConcurrentHashMap<JmaLayerType, Pair<Long, RainViewerResponse>>()

    companion object {
        private const val TAG = "JmaNowcastRepo"
        private const val CACHE_DURATION_MS = 3 * 60 * 1000L // 3分キャッシュ

        @Volatile
        private var INSTANCE: JmaNowcastRepository? = null

        fun getInstance(): JmaNowcastRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JmaNowcastRepository().also { INSTANCE = it }
            }
        }
    }

    private val utcDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parseUtcToEpochSeconds(timeStr: String): Long {
        return try {
            val date = synchronized(utcDateFormat) {
                utcDateFormat.parse(timeStr)
            }
            (date?.time ?: 0L) / 1000L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 従来の互換メソッド（雨雲ナウキャスト HR-PNS + 降水短時間予報 統合データを取得）
     */
    suspend fun getRadarData(forceRefresh: Boolean = false): Result<RainViewerResponse> {
        return getLayerData(JmaLayerType.HRPNS, forceRefresh)
    }

    /**
     * 雨雲レーダー（高解像度降水ナウキャスト + 降水短時間予報 統合ハイブリッド）の合成フォールバック
     */
    fun generateSyntheticHrpnsAndRasrfData(): RainViewerResponse {
        val now = System.currentTimeMillis()
        val fiveMinMs = 5 * 60 * 1000L
        val safeBaseTimeMs = (now - 3 * 60 * 1000L) / fiveMinMs * fiveMinMs
        val latestBasetimeStr = synchronized(utcDateFormat) {
            utcDateFormat.format(Date(safeBaseTimeMs))
        }

        // 過去2時間（-120分〜0分、5分刻み：25フレーム）
        val pastFrames = (24 downTo 0).map { i ->
            val frameTimeMs = safeBaseTimeMs - (i * fiveMinMs)
            val timeStr = synchronized(utcDateFormat) {
                utcDateFormat.format(Date(frameTimeMs))
            }
            val tileUrl = JmaLayerType.HRPNS.buildTileUrl(timeStr, timeStr, "none")
            val isLive = (i == 0)
            RadarFrame(
                time = frameTimeMs / 1000L,
                path = tileUrl,
                isForecast = false,
                tileUrlTemplate = tileUrl,
                sourceName = "高解像度降水ナウキャスト",
                resolutionInfo = if (isLive) "実況" else "過去観測"
            )
        }

        // 予測60分（+5分〜+60分、5分刻み：12フレーム）
        val nowcastFrames = (1..12).map { i ->
            val frameTimeMs = safeBaseTimeMs + (i * fiveMinMs)
            val validTimeStr = synchronized(utcDateFormat) {
                utcDateFormat.format(Date(frameTimeMs))
            }
            val tileUrl = JmaLayerType.HRPNS.buildTileUrl(latestBasetimeStr, validTimeStr, "none")
            val resInfo = "ナウキャスト予測"
            RadarFrame(
                time = frameTimeMs / 1000L,
                path = tileUrl,
                isForecast = true,
                tileUrlTemplate = tileUrl,
                sourceName = "高解像度降水ナウキャスト",
                resolutionInfo = resInfo
            )
        }

        // 降水短時間予報（+2時間〜+15時間、1時間刻み：14フレーム）
        val oneHourMs = 60 * 60 * 1000L
        val rasrfFrames = (2..15).map { h ->
            val frameTimeMs = safeBaseTimeMs + (h * oneHourMs)
            val validTimeStr = synchronized(utcDateFormat) {
                utcDateFormat.format(Date(frameTimeMs))
            }
            val member = if (h <= 6) "immed" else "none"
            val tileUrl = "https://www.jma.go.jp/bosai/jmatile/data/rasrf/$latestBasetimeStr/$member/$validTimeStr/surf/rasrf/{z}/{x}/{y}.png"
            val resInfo = "降水短時間予報"
            RadarFrame(
                time = frameTimeMs / 1000L,
                path = tileUrl,
                isForecast = true,
                tileUrlTemplate = tileUrl,
                sourceName = "降水短時間予報",
                resolutionInfo = resInfo
            )
        }

        return RainViewerResponse(
            version = "JMA-SYNTHETIC-HRPNS-RASRF",
            generated = now / 1000L,
            host = "https://www.jma.go.jp",
            radar = RadarData(
                past = pastFrames,
                nowcast = nowcastFrames + rasrfFrames
            )
        )
    }

    /**
     * ネットワークまたはDNS一時不通時の自動合成フレーム生成
     */
    fun generateSyntheticRadarData(layerType: JmaLayerType): RainViewerResponse {
        if (layerType == JmaLayerType.HRPNS) {
            return generateSyntheticHrpnsAndRasrfData()
        }
        val now = System.currentTimeMillis()
        val fiveMinMs = 5 * 60 * 1000L
        // 配信ラグ（通常約2〜3分）を考慮して最新観測基準時刻を算出
        val safeBaseTimeMs = (now - 3 * 60 * 1000L) / fiveMinMs * fiveMinMs
        val latestBasetimeStr = synchronized(utcDateFormat) {
            utcDateFormat.format(Date(safeBaseTimeMs))
        }

        // 過去13フレーム（-60分〜0分）
        val pastFrames = (12 downTo 0).map { i ->
            val frameTimeMs = safeBaseTimeMs - (i * fiveMinMs)
            val timeStr = synchronized(utcDateFormat) {
                utcDateFormat.format(Date(frameTimeMs))
            }
            val tileUrl = layerType.buildTileUrl(timeStr, timeStr, "none")
            RadarFrame(
                time = frameTimeMs / 1000L,
                path = tileUrl,
                isForecast = false,
                tileUrlTemplate = tileUrl
            )
        }

        // 予測11フレーム（+5分〜+55分）
        val forecastFrames = (1..11).map { i ->
            val frameTimeMs = safeBaseTimeMs + (i * fiveMinMs)
            val validTimeStr = synchronized(utcDateFormat) {
                utcDateFormat.format(Date(frameTimeMs))
            }
            val tileUrl = layerType.buildTileUrl(latestBasetimeStr, validTimeStr, "none")
            RadarFrame(
                time = frameTimeMs / 1000L,
                path = tileUrl,
                isForecast = true,
                tileUrlTemplate = tileUrl
            )
        }

        return RainViewerResponse(
            version = "JMA-SYNTHETIC-${layerType.id.uppercase()}",
            generated = now / 1000L,
            host = "https://www.jma.go.jp",
            radar = RadarData(
                past = pastFrames,
                nowcast = forecastFrames
            )
        )
    }

    /**
     * 雨雲レーダー（高解像度降水ナウキャスト + 降水短時間予報 統合ハイブリッド）
     * - 過去2時間〜現在：高解像度降水ナウキャスト（10分刻み・13フレーム、空間解像度1km）
     * - 未来60分：高解像度降水ナウキャスト（10分刻み・6フレーム、空間解像度1km）
     * - 未来60分超〜15時間後：降水短時間予報（1時間刻み・14フレーム、空間解像度1km〜5km広域数値予報モデル）
     */
    private suspend fun getCombinedHrpnsAndRasrfData(forceRefresh: Boolean): Result<RainViewerResponse> {
        val now = System.currentTimeMillis()
        val cached = cacheMap[JmaLayerType.HRPNS]
        if (!forceRefresh && cached != null && (now - cached.first) < CACHE_DURATION_MS) {
            return Result.success(cached.second)
        }

        return mutex.withLock {
            val innerNow = System.currentTimeMillis()
            val innerCached = cacheMap[JmaLayerType.HRPNS]
            if (!forceRefresh && innerCached != null && (innerNow - innerCached.first) < CACHE_DURATION_MS) {
                return@withLock Result.success(innerCached.second)
            }

            try {
                // 1. N1（過去実況ナウキャスト）
                val n1Url = "https://www.jma.go.jp/bosai/jmatile/data/nowc/targetTimes_N1.json"
                val n1Json = fetchJsonWithRetry(n1Url, maxRetries = 2)
                val rawN1 = json.decodeFromString<List<JmaTargetTime>>(n1Json)
                val n1Entries = rawN1
                    .filter { it.elements.isEmpty() || it.elements.contains("hrpns") }
                    .distinctBy { it.validtime }
                    .sortedBy { it.validtime }

                if (n1Entries.isEmpty()) {
                    throw Exception("N1 target times is empty")
                }

                val latestN1 = n1Entries.last()
                val latestEpochSec = parseUtcToEpochSeconds(latestN1.validtime)

                // 過去2時間（-120分〜0分、5分刻み：25フレーム）
                val pastFrames = (24 downTo 0).mapNotNull { step ->
                    val targetSec = latestEpochSec - (step * 300L)
                    val closest = n1Entries.minByOrNull { Math.abs(parseUtcToEpochSeconds(it.validtime) - targetSec) }
                    if (closest != null && Math.abs(parseUtcToEpochSeconds(closest.validtime) - targetSec) <= 120L) {
                        val epochSec = parseUtcToEpochSeconds(closest.validtime)
                        val tileUrl = JmaLayerType.HRPNS.buildTileUrl(closest.basetime, closest.validtime, "none")
                        val isLive = (step == 0)
                        RadarFrame(
                            time = epochSec,
                            path = tileUrl,
                            isForecast = false,
                            tileUrlTemplate = tileUrl,
                            sourceName = "高解像度降水ナウキャスト",
                            resolutionInfo = if (isLive) "実況" else "過去観測"
                        )
                    } else null
                }.distinctBy { it.time }

                // 2. N2（未来60分ナウキャスト、5分刻み：12フレーム）
                val n2Url = "https://www.jma.go.jp/bosai/jmatile/data/nowc/targetTimes_N2.json"
                val n2Json = fetchJsonWithRetry(n2Url, maxRetries = 2)
                val rawN2 = json.decodeFromString<List<JmaTargetTime>>(n2Json)
                val n2Entries = rawN2
                    .filter { it.elements.isEmpty() || it.elements.contains("hrpns") }
                    .distinctBy { it.validtime }
                    .sortedBy { it.validtime }

                val nowcastFrames = (1..12).mapNotNull { step ->
                    val targetSec = latestEpochSec + (step * 300L)
                    val closest = n2Entries.minByOrNull { Math.abs(parseUtcToEpochSeconds(it.validtime) - targetSec) }
                    if (closest != null && Math.abs(parseUtcToEpochSeconds(closest.validtime) - targetSec) <= 120L) {
                        val epochSec = parseUtcToEpochSeconds(closest.validtime)
                        val tileUrl = JmaLayerType.HRPNS.buildTileUrl(closest.basetime, closest.validtime, "none")
                        val resInfo = "ナウキャスト予測"
                        RadarFrame(
                            time = epochSec,
                            path = tileUrl,
                            isForecast = true,
                            tileUrlTemplate = tileUrl,
                            sourceName = "高解像度降水ナウキャスト",
                            resolutionInfo = resInfo
                        )
                    } else null
                }.distinctBy { it.time }

                // 3. 降水短時間予報（未来60分超〜15時間後、1時間刻み）
                val rasrfUrl = "https://www.jma.go.jp/bosai/jmatile/data/rasrf/targetTimes.json"
                val rasrfFrames = try {
                    val rasrfJson = fetchJsonWithRetry(rasrfUrl, maxRetries = 2)
                    val rawRasrf = json.decodeFromString<List<JmaTargetTime>>(rasrfJson)
                    val cutOffSec = latestEpochSec + 3600L - 300L // 60分超
                    val maxFutureSec = latestEpochSec + 15 * 3600L + 1800L // 最大15時間後

                    val filteredRasrf = rawRasrf
                        .filter { it.elements.contains("rasrf") }
                        .filter {
                            val epoch = parseUtcToEpochSeconds(it.validtime)
                            epoch > cutOffSec && epoch <= maxFutureSec
                        }
                        // 同一validtimeに対してbasetimeが最新のものを優先
                        .groupBy { it.validtime }
                        .mapValues { (_, list) -> list.maxByOrNull { it.basetime }!! }
                        .values
                        .sortedBy { it.validtime }

                    filteredRasrf.map { item ->
                        val epochSec = parseUtcToEpochSeconds(item.validtime)
                        val member = if (item.member.isNotBlank()) item.member else "none"
                        val tileUrl = "https://www.jma.go.jp/bosai/jmatile/data/rasrf/${item.basetime}/$member/${item.validtime}/surf/rasrf/{z}/{x}/{y}.png"
                        val resInfo = "降水短時間予報"
                        RadarFrame(
                            time = epochSec,
                            path = tileUrl,
                            isForecast = true,
                            tileUrlTemplate = tileUrl,
                            sourceName = "降水短時間予報",
                            resolutionInfo = resInfo
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch rasrf short forecast, continuing with nowcast only: ${e.message}")
                    emptyList()
                }

                val finalPast = if (pastFrames.isNotEmpty()) pastFrames else listOf(
                    RadarFrame(
                        time = latestEpochSec,
                        path = JmaLayerType.HRPNS.buildTileUrl(latestN1.basetime, latestN1.validtime, "none"),
                        isForecast = false,
                        tileUrlTemplate = JmaLayerType.HRPNS.buildTileUrl(latestN1.basetime, latestN1.validtime, "none"),
                        sourceName = "高解像度降水ナウキャスト",
                        resolutionInfo = "実況"
                    )
                )
                val finalForecast = nowcastFrames + rasrfFrames

                val response = RainViewerResponse(
                    version = "JMA-HRPNS-RASRF-COMBINED",
                    generated = System.currentTimeMillis() / 1000L,
                    host = "https://www.jma.go.jp",
                    radar = RadarData(
                        past = finalPast,
                        nowcast = finalForecast
                    )
                )

                cacheMap[JmaLayerType.HRPNS] = Pair(System.currentTimeMillis(), response)
                Log.d(TAG, "JMA Combined Radar loaded: ${finalPast.size} past frames (-2h〜0h), ${nowcastFrames.size} nowcast (+60m), ${rasrfFrames.size} rasrf (+15h)")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch combined HRPNS/RASRF data, falling back to synthetic", e)
                val fallback = cacheMap[JmaLayerType.HRPNS]?.second ?: generateSyntheticHrpnsAndRasrfData()
                cacheMap[JmaLayerType.HRPNS] = Pair(System.currentTimeMillis(), fallback)
                Result.success(fallback)
            }
        }
    }

    /**
     * 指定された気象庁レイヤーのタイルデータ（時間軸フレーム群）を取得
     */
    suspend fun getLayerData(
        layerType: JmaLayerType,
        forceRefresh: Boolean = false
    ): Result<RainViewerResponse> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 雨雲ナウキャスト（HRPNS）は高解像度ナウキャスト（-2h〜+60m）と降水短時間予報（+60m〜+15h）をシームレス統合
        if (layerType == JmaLayerType.HRPNS) {
            return@withContext getCombinedHrpnsAndRasrfData(forceRefresh)
        }

        // 1. 静的レイヤー（国土地理院淡色地図、気象庁標準地図、洪水浸水想定区域など）
        if (layerType.isStatic) {
            val staticUrl = layerType.staticUrl ?: ""
            val staticFrame = RadarFrame(
                time = now / 1000L,
                path = staticUrl,
                isForecast = false,
                tileUrlTemplate = staticUrl
            )
            return@withContext Result.success(
                RainViewerResponse(
                    version = "JMA-STATIC-${layerType.id}",
                    generated = now / 1000L,
                    host = "https://www.jma.go.jp",
                    radar = RadarData(
                        past = listOf(staticFrame),
                        nowcast = emptyList()
                    )
                )
            )
        }

        // 2. キャッシュチェック
        val cached = cacheMap[layerType]
        if (!forceRefresh && cached != null && (now - cached.first) < CACHE_DURATION_MS) {
            return@withContext Result.success(cached.second)
        }

        mutex.withLock {
            val currentNow = System.currentTimeMillis()
            val innerCached = cacheMap[layerType]
            if (!forceRefresh && innerCached != null && (currentNow - innerCached.first) < CACHE_DURATION_MS) {
                return@withContext Result.success(innerCached.second)
            }

            try {
                // 3. 各 targetTimesUrl からエントリを収集（リトライ付き）
                val rawEntries = mutableListOf<JmaTargetTime>()
                for (targetUrl in layerType.targetTimesUrls) {
                    try {
                        val jsonStr = fetchJsonWithRetry(targetUrl, maxRetries = 2)
                        val list = json.decodeFromString<List<JmaTargetTime>>(jsonStr)
                        rawEntries.addAll(list)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to fetch $targetUrl for ${layerType.displayName}: ${e.message}")
                    }
                }

                if (rawEntries.isEmpty()) {
                    // 災害危険度を架空フレームで表示しない。取得できない場合は既存キャッシュのみ返す。
                    val cachedFallback = cacheMap[layerType]?.second
                    if (cachedFallback != null) {
                        Log.i(TAG, "No targetTimes data received; using cached ${layerType.id} data")
                        return@withContext Result.success(cachedFallback)
                    }
                    if (layerType.category == JmaLayerCategory.RISK) {
                        return@withContext Result.failure(IllegalStateException("JMA risk timeline is unavailable for ${layerType.id}"))
                    }
                    val fallback = generateSyntheticRadarData(layerType)
                    cacheMap[layerType] = Pair(System.currentTimeMillis(), fallback)
                    return@withContext Result.success(fallback)
                }

                // 4. elementKey によるフィルタリング（elements が指定されている場合）
                val filteredEntries = if (layerType.elementKey.isNotBlank() && !layerType.elementKey.contains("/")) {
                    rawEntries.filter { item ->
                        item.elements.isEmpty() || layerType.elementKeys.any(item.elements::contains)
                    }
                } else {
                    rawEntries
                }

                // 重複排除（validtimeキー）
                val distinctEntries = filteredEntries
                    .distinctBy { it.validtime }
                    .sortedBy { it.validtime } // 昇順（過去から未来へ）

                // 5. 過去と未来の仕分け
                // 最新の basetime を基準に過去と予測を判定
                val latestBasetime = distinctEntries.mapNotNull { it.basetime.takeIf { b -> b.isNotBlank() } }.maxOrNull() ?: ""

                val pastList = mutableListOf<JmaTargetTime>()
                val forecastList = mutableListOf<JmaTargetTime>()

                for (item in distinctEntries) {
                    val isForecast = if (latestBasetime.isNotBlank()) {
                        item.validtime > latestBasetime
                    } else {
                        item.validtime > item.basetime
                    }

                    if (isForecast) {
                        forecastList.add(item)
                    } else {
                        pastList.add(item)
                    }
                }

                // ナウキャスト等で過去フレーム数が膨大な場合は直近13フレーム（約1時間）に制限
                val trimmedPast = if (pastList.size > 13) pastList.takeLast(13) else pastList

                // 6. RadarFrame の構築
                val pastFrames = trimmedPast.map { item ->
                    val epochSec = parseUtcToEpochSeconds(item.validtime)
                    val tileUrl = layerType.buildTileUrl(item.basetime, item.validtime, item.member)
                    RadarFrame(
                        time = epochSec,
                        path = tileUrl,
                        isForecast = false,
                        tileUrlTemplate = tileUrl,
                        additionalTileUrlTemplates = layerType.additionalPathPatterns.map { pattern ->
                            layerType.buildTileUrl(item.basetime, item.validtime, item.member, pattern)
                        }
                    )
                }

                val forecastFrames = forecastList.map { item ->
                    val epochSec = parseUtcToEpochSeconds(item.validtime)
                    val tileUrl = layerType.buildTileUrl(item.basetime, item.validtime, item.member)
                    RadarFrame(
                        time = epochSec,
                        path = tileUrl,
                        isForecast = true,
                        tileUrlTemplate = tileUrl,
                        additionalTileUrlTemplates = layerType.additionalPathPatterns.map { pattern ->
                            layerType.buildTileUrl(item.basetime, item.validtime, item.member, pattern)
                        }
                    )
                }

                // 全て予報の場合は最初の1枚をpastとして扱い、再生・スライダーを正常動作させる
                val finalPastFrames: List<RadarFrame>
                val finalForecastFrames: List<RadarFrame>
                if (pastFrames.isEmpty() && forecastFrames.isNotEmpty()) {
                    finalPastFrames = listOf(forecastFrames.first().copy(isForecast = false))
                    finalForecastFrames = forecastFrames.drop(1)
                } else {
                    finalPastFrames = pastFrames
                    finalForecastFrames = forecastFrames
                }

                val response = RainViewerResponse(
                    version = "JMA-${layerType.id.uppercase()}",
                    generated = System.currentTimeMillis() / 1000L,
                    host = "https://www.jma.go.jp",
                    radar = RadarData(
                        past = finalPastFrames,
                        nowcast = finalForecastFrames
                    )
                )

                cacheMap[layerType] = Pair(System.currentTimeMillis(), response)
                Log.d(TAG, "JMA Layer [${layerType.displayName}] loaded: ${finalPastFrames.size} past, ${finalForecastFrames.size} forecast frames")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch JMA Layer data for ${layerType.displayName}", e)
                val cachedFallback = cacheMap[layerType]?.second
                if (cachedFallback != null) {
                    Result.success(cachedFallback)
                } else if (layerType.category == JmaLayerCategory.RISK) {
                    Result.failure(e)
                } else {
                    val fallback = generateSyntheticRadarData(layerType)
                    cacheMap[layerType] = Pair(System.currentTimeMillis(), fallback)
                    Result.success(fallback)
                }
            }
        }
    }

    private suspend fun fetchJsonWithRetry(urlStr: String, maxRetries: Int = 2): String {
        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                return fetchJson(urlStr)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delay(attempt * 400L)
                }
            }
        }
        throw lastException ?: Exception("Failed to fetch $urlStr after $maxRetries attempts")
    }

    private fun fetchJson(urlStr: String): String {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; ExpressiveWeather) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            setRequestProperty("Accept", "application/json, text/plain, */*")
        }
        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("HTTP ${conn.responseCode} from $urlStr")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }
}
