package com.example.expressiveweather.data

import android.util.Log
import com.example.expressiveweather.data.local.LocationKeyNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Photon (komoot) オープンジオコーディング API クライアント
 *
 * - APIキー不要
 * - エンドポイント: https://photon.komoot.io/api/?q={query}&lang=ja
 * - 現在地 (lat, lon) がある場合は現在地優先のバイアスパラメータを付与
 * - OpenStreetMap ベースの高精度な地名・住所検索
 */
object PhotonGeocodingService {

    private const val BASE_URL = "https://photon.komoot.io/api/"

    /**
     * Photon API を使用して地名を検索
     *
     * @param query 検索クエリ
     * @param lang 言語コード (例: "ja")
     * @param lat 端末現在地の緯度 (任意。現在地優先バイアス)
     * @param lon 端末現在地の経度 (任意。現在地優先バイアス)
     * @param limit 取得件数 (デフォルト 15)
     */
    suspend fun search(
        query: String,
        lang: String = "ja",
        lat: Double? = null,
        lon: Double? = null,
        limit: Int = 15
    ): List<City> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        // 1. 指定された lang (例: "ja") で検索を実行
        val resultsWithLang = executeQuery(trimmed, lang = lang, lat = lat, lon = lon, limit = limit)
        if (resultsWithLang.isNotEmpty()) {
            return@withContext resultsWithLang
        }

        // 2. Photon API で lang パラメータが未対応の場合のエラーフォールバック (lang パラメータなしで再実行)
        if (lang != "default") {
            val fallbackResults = executeQuery(trimmed, lang = null, lat = lat, lon = lon, limit = limit)
            if (fallbackResults.isNotEmpty()) {
                return@withContext fallbackResults
            }
        }

        emptyList()
    }

    private fun executeQuery(
        query: String,
        lang: String?,
        lat: Double?,
        lon: Double?,
        limit: Int
    ): List<City> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val sb = StringBuilder(BASE_URL)
                .append("?q=").append(encodedQuery)

            if (!lang.isNullOrBlank()) {
                sb.append("&lang=").append(lang)
            }

            if (lat != null && lon != null) {
                sb.append("&lat=").append(lat)
                sb.append("&lon=").append(lon)
            }

            sb.append("&limit=").append(limit)

            val url = URL(sb.toString())
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ExpressiveWeather/1.0 (Android; Mobile)")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 7000
            conn.readTimeout = 7000

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                conn.disconnect()
                return emptyList()
            }

            val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            conn.disconnect()

            val json = JSONObject(responseText)
            // エラーオブジェクト（言語非対応エラー等）のチェック
            if (json.has("lang") && !json.has("features")) {
                return emptyList()
            }

            val features = json.optJSONArray("features") ?: return emptyList()
            val list = mutableListOf<City>()
            val seenKeys = mutableSetOf<String>()

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val geometry = feature.optJSONObject("geometry") ?: continue
                val coordinates = geometry.optJSONArray("coordinates") ?: continue
                if (coordinates.length() < 2) continue

                val lonVal = coordinates.getDouble(0)
                val latVal = coordinates.getDouble(1)

                val key = LocationKeyNormalizer.normalize(latVal, lonVal)
                if (seenKeys.contains(key)) continue
                seenKeys.add(key)

                val properties = feature.optJSONObject("properties") ?: JSONObject()
                val name = properties.optString("name").ifBlank {
                    properties.optString("city").ifBlank { query }
                }

                val district = properties.optString("district")
                val city = properties.optString("city")
                val state = properties.optString("state")
                val country = properties.optString("country")

                val regionParts = listOf(
                    district.takeIf { it.isNotBlank() && it != name },
                    city.takeIf { it.isNotBlank() && it != name },
                    state.takeIf { it.isNotBlank() && it != name && it != city },
                    country.takeIf { it.isNotBlank() }
                ).filterNotNull().distinct()

                val regionText = regionParts.joinToString(" · ")
                val osmId = properties.optLong("osm_id", 0L)
                val cityId = if (osmId != 0L) "photon_${osmId}" else "photon_${key}"

                list.add(
                    City(
                        id = cityId,
                        name = name,
                        nameEn = properties.optString("name"),
                        country = if (regionText.isNotBlank()) regionText else country,
                        latitude = latVal,
                        longitude = lonVal
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("PhotonGeocodingService", "Photon search query error: ${e.message}")
            emptyList()
        }
    }
}
