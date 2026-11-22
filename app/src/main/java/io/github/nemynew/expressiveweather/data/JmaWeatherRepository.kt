package io.github.nemynew.expressiveweather.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 気象庁（JMA）公式 Web API と直接通信を行うリポジトリ
 */
object JmaWeatherRepository {

    private const val TAG = "JmaWeatherRepo"
    private const val BASE_URL = "https://www.jma.go.jp/bosai"
    private val jmaHeaders = mapOf(
        "User-Agent" to "ExpressiveWeather/1.0 (Android; Mobile; JMA Client)",
        "Accept" to "application/json, text/plain, */*"
    )

    suspend fun fetchForecast(officeCode: String): JSONArray = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/forecast/data/forecast/$officeCode.json"
        JSONArray(AppHttpClient.getText(url, headers = jmaHeaders, timeoutMs = 12_000L))
    }

    suspend fun fetchOverview(officeCode: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/forecast/data/overview_forecast/$officeCode.json"
            JSONObject(AppHttpClient.getText(url, headers = jmaHeaders, timeoutMs = 12_000L))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch overview for $officeCode: ${e.message}")
            null
        }
    }

    suspend fun fetchAmedasLatestTime(): String = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/amedas/data/latest_time.txt"
        AppHttpClient.getText(url, headers = jmaHeaders, timeoutMs = 12_000L).trim()
    }

    suspend fun fetchAmedasPoint(stationCode: String, ymd: String, blockHour: String): JSONObject = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/amedas/data/point/$stationCode/${ymd}_$blockHour.json"
        JSONObject(AppHttpClient.getText(url, headers = jmaHeaders, timeoutMs = 12_000L))
    }

    suspend fun fetchWarnings(officeCode: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/warning/data/warning/$officeCode.json"
            JSONObject(AppHttpClient.getText(url, headers = jmaHeaders, timeoutMs = 12_000L))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch warnings for $officeCode: ${e.message}")
            null
        }
    }
}
