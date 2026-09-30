package com.example.expressiveweather.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 気象庁（JMA）公式 Web API と直接通信を行うリポジトリ
 */
object JmaWeatherRepository {

    private const val TAG = "JmaWeatherRepo"
    private const val BASE_URL = "https://www.jma.go.jp/bosai"
    private const val USER_AGENT = "ExpressiveWeather/1.0 (Android; Mobile; JMA Client)"

    /**
     * 都道府県予報区の3日予報および7日間週間予報 JSON を取得
     */
    suspend fun fetchForecast(officeCode: String): JSONArray = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/forecast/data/forecast/$officeCode.json"
        httpGetJsonArray(url)
    }

    /**
     * 地方気象台発表の公式天気概況解説テキストを取得
     */
    suspend fun fetchOverview(officeCode: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/forecast/data/overview_forecast/$officeCode.json"
            httpGetJson(url)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch overview for $officeCode: ${e.message}")
            null
        }
    }

    /**
     * アメダスの最新観測確定時刻 (latest_time.txt) を取得
     * 返却例: "2026-09-22T20:20:00+09:00"
     */
    suspend fun fetchAmedasLatestTime(): String = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/amedas/data/latest_time.txt"
        httpGetString(url).trim()
    }

    /**
     * 特定アメダス観測所の直近3時間 (10分刻み) 観測データ JSON を取得
     * URL 例: https://www.jma.go.jp/bosai/amedas/data/point/44132/20260922_18.json
     */
    suspend fun fetchAmedasPoint(stationCode: String, ymd: String, blockHour: String): JSONObject = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/amedas/data/point/$stationCode/${ymd}_$blockHour.json"
        httpGetJson(url)
    }

    /**
     * 気象警報・注意報を取得
     */
    suspend fun fetchWarnings(officeCode: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/warning/data/warning/$officeCode.json"
            httpGetJson(url)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch warnings for $officeCode: ${e.message}")
            null
        }
    }

    private fun httpGetJson(urlString: String): JSONObject {
        val raw = httpGetString(urlString)
        return JSONObject(raw)
    }

    private fun httpGetJsonArray(urlString: String): JSONArray {
        val raw = httpGetString(urlString)
        return JSONArray(raw)
    }

    private fun httpGetString(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "application/json, text/plain, */*")
        conn.connectTimeout = 12000
        conn.readTimeout = 12000

        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP error $code from $urlString")
            }
            val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
            reader.use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
