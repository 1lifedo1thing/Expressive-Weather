package io.github.nemynew.expressiveweather.data

import android.util.Log
import io.github.nemynew.expressiveweather.BuildConfig
import io.github.nemynew.expressiveweather.data.local.LocationKeyNormalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Photon (komoot) オープンジオコーディング API クライアント
 */
object PhotonGeocodingService {

    private const val BASE_URL = "https://photon.komoot.io/api/"

    suspend fun search(
        query: String,
        lang: String = "ja",
        lat: Double? = null,
        lon: Double? = null,
        limit: Int = 15
    ): List<City> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val resultsWithLang = executeQuery(trimmed, lang = lang, lat = lat, lon = lon, limit = limit)
        if (resultsWithLang.isNotEmpty()) {
            return@withContext resultsWithLang
        }

        if (lang != "default") {
            val fallbackResults = executeQuery(trimmed, lang = null, lat = lat, lon = lon, limit = limit)
            if (fallbackResults.isNotEmpty()) {
                return@withContext fallbackResults
            }
        }

        emptyList()
    }

    private suspend fun executeQuery(
        query: String,
        lang: String?,
        lat: Double?,
        lon: Double?,
        limit: Int
    ): List<City> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val safeLimit = limit.coerceIn(1, 50)
            val sb = StringBuilder(BASE_URL)
                .append("?q=").append(encodedQuery)

            if (!lang.isNullOrBlank()) {
                sb.append("&lang=").append(URLEncoder.encode(lang.trim(), "UTF-8"))
            }

            if (lat != null && lon != null && lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) {
                sb.append("&lat=").append(lat)
                sb.append("&lon=").append(lon)
            }

            sb.append("&limit=").append(safeLimit)

            val responseText = AppHttpClient.getTextOrNull(sb.toString(), timeoutMs = 7_000L) ?: return emptyList()
            val json = JSONObject(responseText)
            if (json.has("lang") && !json.has("features")) {
                return emptyList()
            }

            val features = json.optJSONArray("features") ?: return emptyList()
            val list = mutableListOf<City>()
            val seenKeys = mutableSetOf<String>()

            for (i in 0 until features.length()) {
                try {
                    val feature = features.optJSONObject(i) ?: continue
                    val geometry = feature.optJSONObject("geometry") ?: continue
                    val coordinates = geometry.optJSONArray("coordinates") ?: continue
                    if (coordinates.length() < 2) continue

                    val lonVal = coordinates.optDouble(0, Double.NaN)
                    val latVal = coordinates.optDouble(1, Double.NaN)
                    if (!lonVal.isFinite() || !latVal.isFinite()) continue

                    val key = LocationKeyNormalizer.normalize(latVal, lonVal)
                    if (!seenKeys.add(key)) continue

                    val properties = feature.optJSONObject("properties") ?: JSONObject()
                    val name = properties.optNonNullString("name").ifBlank {
                        properties.optNonNullString("city").ifBlank { query }
                    }

                    val district = properties.optNonNullString("district")
                    val city = properties.optNonNullString("city")
                    val state = properties.optNonNullString("state")
                    val country = properties.optNonNullString("country")

                    val regionParts = listOfNotNull(
                        district.takeIf { it.isNotBlank() && it != name },
                        city.takeIf { it.isNotBlank() && it != name },
                        state.takeIf { it.isNotBlank() && it != name && it != city },
                        country.takeIf { it.isNotBlank() }
                    ).distinct()

                    val regionText = regionParts.joinToString(" · ")
                    val osmId = properties.optLong("osm_id", 0L)
                    val cityId = if (osmId != 0L) "photon_${osmId}" else "photon_${key}"

                    val englishOrOsmName = properties.optNonNullString("name:en")
                        .ifBlank { properties.optNonNullString("int_name") }
                        .ifBlank { name }

                    list.add(
                        City(
                            id = cityId,
                            name = name,
                            nameEn = englishOrOsmName,
                            country = if (regionText.isNotBlank()) regionText else country,
                            latitude = latVal,
                            longitude = lonVal
                        )
                    )
                } catch (_: Exception) {
                    // Skip malformed feature without discarding entire list
                }
            }
            list
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w("PhotonGeocodingService", "Photon search query error: ${e.message}")
            }
            emptyList()
        }
    }
}
