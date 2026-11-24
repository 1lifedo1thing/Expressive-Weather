package io.github.nemynew.expressiveweather.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class RainViewerRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    @Volatile
    private var cachedResponse: RainViewerResponse? = null
    @Volatile
    private var lastFetchTime: Long = 0L

    companion object {
        private const val TAG = "RainViewerRepo"
        private const val API_URL = "https://api.rainviewer.com/public/weather-maps.json"
        // 5分間のインメモリキャッシュ
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L

        @Volatile
        private var INSTANCE: RainViewerRepository? = null

        fun getInstance(): RainViewerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RainViewerRepository().also { INSTANCE = it }
            }
        }
    }

    suspend fun getRadarData(forceRefresh: Boolean = false): Result<RainViewerResponse> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedResponse != null && (now - lastFetchTime) < CACHE_DURATION_MS) {
            return@withContext Result.success(cachedResponse!!)
        }

        mutex.withLock {
            // 二重チェック
            val currentNow = System.currentTimeMillis()
            if (!forceRefresh && cachedResponse != null && (currentNow - lastFetchTime) < CACHE_DURATION_MS) {
                return@withContext Result.success(cachedResponse!!)
            }

            try {
                val body = AppHttpClient.getText(
                    url = API_URL,
                    headers = mapOf("User-Agent" to "ExpressiveWeather/1.0 (Android)"),
                    timeoutMs = 8_000L
                )
                val decoded = json.decodeFromString<RainViewerResponse>(body)
                val safeHost = if (RadarUrlValidator.isValidRainViewerHost(decoded.host)) {
                    decoded.host.trim().trimEnd('/')
                } else {
                    Log.w(TAG, "Rejected unexpected RainViewer host: ${decoded.host}")
                    "https://tilecache.rainviewer.com"
                }
                val safePast = decoded.radar.past.filter { RadarUrlValidator.isValidRainViewerPath(it.path) }
                val safeNowcast = decoded.radar.nowcast.filter { RadarUrlValidator.isValidRainViewerPath(it.path) }
                val response = decoded.copy(
                    host = safeHost,
                    radar = decoded.radar.copy(past = safePast, nowcast = safeNowcast)
                )
                cachedResponse = response
                lastFetchTime = System.currentTimeMillis()
                Log.d(TAG, "Fetched RainViewer data: ${response.radar.past.size} past frames")
                Result.success(response)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch RainViewer data", e)
                cachedResponse?.let {
                    Result.success(it)
                } ?: Result.failure(e)
            }
        }
    }
}
