package com.example.expressiveweather.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
                val url = URL(API_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "ExpressiveWeather/1.0 (Android)")
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val response = json.decodeFromString<RainViewerResponse>(body)
                    cachedResponse = response
                    lastFetchTime = System.currentTimeMillis()
                    Log.d(TAG, "Fetched RainViewer data: ${response.radar.past.size} past frames")
                    Result.success(response)
                } else {
                    Log.w(TAG, "RainViewer API error: ${conn.responseCode}")
                    cachedResponse?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("RainViewer API returned code ${conn.responseCode}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch RainViewer data", e)
                cachedResponse?.let {
                    Result.success(it)
                } ?: Result.failure(e)
            }
        }
    }
}
