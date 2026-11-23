package io.github.nemynew.expressiveweather.data

import android.util.Log
import io.github.nemynew.expressiveweather.ui.components.WeatherWarningType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import kotlin.math.*

/**
 * 全球災害警報・調整システム (国連/欧州委員会 GDACS) 公式災害警報取得リポジトリ
 *
 * エンドポイント: https://www.gdacs.org/gdacsapi/api/events/geteventlist/SEARCH?alertlevel=red;orange&pagesize=100
 * - 熱帯低気圧・サイクロン・台風 (TC), 地震 (EQ), 洪水 (FL), 火山噴火 (VO), 森林火災 (WF), 干ばつ (DR) の Red/Orange 警報を全球監視
 */
object GdacsAlertRepository {
    private const val TAG = "GdacsAlertRepository"
    private const val SEARCH_URL = "https://www.gdacs.org/gdacsapi/api/events/geteventlist/SEARCH?alertlevel=red;orange&pagesize=100"

    // 15分間のグローバルメモリキャッシュ
    @Volatile
    private var cachedEvents: List<GdacsRawEvent> = emptyList()
    @Volatile
    private var lastFetchTimeMillis: Long = 0L
    private const val CACHE_TTL_MS = 15 * 60 * 1000L
    private val fetchMutex = Mutex()

    data class GdacsRawEvent(
        val eventId: String,
        val eventType: String, // "TC", "EQ", "FL", "VO", "WF", "DR"
        val eventName: String,
        val name: String,
        val description: String,
        val alertLevel: String, // "Red", "Orange", "Green"
        val alertScore: Double,
        val latitude: Double,
        val longitude: Double,
        val countries: List<String>,
        val isoCodes: List<String> = emptyList(),
        val fromDate: String,
        val toDate: String,
        val reportUrl: String?,
        val severityText: String?
    )

    /**
     * 都市の緯度経度・国名に基づき、該当するGDACS災害警報を抽出
     */
    suspend fun fetchAlertsForCity(city: City, forceFresh: Boolean = false): List<GlobalAlertItem> = withContext(Dispatchers.IO) {
        val allEvents = getAllGdacsEvents(forceFresh)
        if (allEvents.isEmpty()) return@withContext emptyList()

        val matchingItems = mutableListOf<GlobalAlertItem>()

        for (event in allEvents) {
            val isMatching = isEventAffectingCity(event, city)
            if (isMatching) {
                val isRed = event.alertLevel.equals("Red", ignoreCase = true)
                val isOrange = event.alertLevel.equals("Orange", ignoreCase = true)

                // 新3色体系: 黄色：注意 (ADVISORY) / 橙色：警戒 (WARNING) / 赤色：特別警戒 (SPECIAL_WARNING)
                val type = when {
                    isRed -> WeatherWarningType.SPECIAL_WARNING
                    isOrange -> WeatherWarningType.WARNING
                    else -> WeatherWarningType.ADVISORY
                }
                val severity = when {
                    isRed -> AlertSeverity.EXTREME
                    isOrange -> AlertSeverity.SEVERE
                    else -> AlertSeverity.MODERATE
                }

                val headline = if (!event.severityText.isNullOrBlank()) {
                    "${event.name}: ${event.severityText}"
                } else {
                    event.name
                }

                matchingItems.add(
                    GlobalAlertItem(
                        id = "gdacs-${event.eventType}-${event.eventId}",
                        source = AlertSource.GDACS,
                        type = type,
                        severity = severity,
                        eventName = event.eventName.ifBlank { event.name },
                        headline = headline,
                        description = event.description.ifBlank { event.name },
                        instruction = null,
                        effectiveTime = event.fromDate,
                        expiresTime = event.toDate,
                        areaDesc = event.countries.joinToString(", ").take(100),
                        detailsUrl = event.reportUrl,
                        eventTypeCode = event.eventType
                    )
                )
            }
        }

        // 特別警戒 (赤) > 警戒 (橙) > 注意 (黄) の順でソート
        matchingItems.sortedWith(
            compareBy(
                {
                    when (it.type) {
                        WeatherWarningType.SPECIAL_WARNING -> 0
                        WeatherWarningType.WARNING -> 1
                        WeatherWarningType.ADVISORY -> 2
                        else -> 3
                    }
                },
                { it.severity.ordinal }
            )
        )
    }

    /**
     * GDACSから全世界のアクティブなRed/Orange災害一覧を一括取得
     */
    private suspend fun getAllGdacsEvents(forceFresh: Boolean): List<GdacsRawEvent> {
        val now = System.currentTimeMillis()
        if (!forceFresh && lastFetchTimeMillis > 0L && (now - lastFetchTimeMillis) < CACHE_TTL_MS) {
            return cachedEvents
        }

        return fetchMutex.withLock {
            val innerNow = System.currentTimeMillis()
            if (!forceFresh && lastFetchTimeMillis > 0L && (innerNow - lastFetchTimeMillis) < CACHE_TTL_MS) {
                return cachedEvents
            }

            try {
                val body = AppHttpClient.getTextOrNull(
                    url = SEARCH_URL,
                    headers = mapOf(
                        "User-Agent" to "ExpressiveWeather/1.0 (Android; +https://github.com/nemy-new/Expressive-Weather)",
                        "Accept" to "application/json"
                    ),
                    timeoutMs = 7_000L
                ) ?: return cachedEvents

                val parsed = parseGdacsJson(body)
                cachedEvents = parsed
                lastFetchTimeMillis = System.currentTimeMillis()
                Log.d(TAG, "Successfully fetched ${parsed.size} global disaster events from GDACS")
                parsed
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch GDACS disaster list: ${e.message}")
                cachedEvents
            }
        }
    }

    /**
     * GDACS GeoJSONのパース
     */
    private fun parseGdacsJson(jsonString: String): List<GdacsRawEvent> {
        val root = JSONObject(jsonString)
        val features = root.optJSONArray("features") ?: return emptyList()
        val list = mutableListOf<GdacsRawEvent>()

        for (i in 0 until features.length()) {
            val feat = features.optJSONObject(i) ?: continue
            val geom = feat.optJSONObject("geometry")
            val coords = geom?.optJSONArray("coordinates")
            val lon = coords?.optDouble(0, Double.NaN) ?: Double.NaN
            val lat = coords?.optDouble(1, Double.NaN) ?: Double.NaN

            val props = feat.optJSONObject("properties") ?: continue
            val eventId = props.optNonNullString("eventid")
            val eventType = props.optNonNullString("eventtype")
            val eventName = props.optNonNullString("eventname")
            val name = props.optNonNullString("name")
            val description = props.optNonNullString("description")
            val alertLevel = props.optNonNullString("alertlevel", "Orange")
            val alertScore = props.optFiniteDouble("alertscore") ?: 1.0
            val countryStr = props.optNonNullString("country")
            val fromDate = props.optNonNullString("fromdate")
            val toDate = props.optNonNullString("todate")

            val urlObj = props.optJSONObject("url")
            val reportUrl = urlObj?.optNullableString("report")

            val severityObj = props.optJSONObject("severitydata")
            val severityText = severityObj?.optNullableString("severitytext")

            val countriesList = mutableListOf<String>()
            val isoCodesList = mutableListOf<String>()
            if (countryStr.isNotBlank()) {
                countriesList.addAll(countryStr.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }

            val affectedArr = props.optJSONArray("affectedcountries")
            if (affectedArr != null) {
                for (j in 0 until affectedArr.length()) {
                    val cObj = affectedArr.optJSONObject(j) ?: continue
                    val cName = cObj.optNonNullString("countryname").trim()
                    val iso2 = cObj.optNonNullString("iso2").trim()
                    val iso3 = cObj.optNonNullString("iso3").trim()
                    if (cName.isNotBlank() && !countriesList.contains(cName)) countriesList.add(cName)
                    if (iso2.isNotBlank() && !isoCodesList.contains(iso2)) isoCodesList.add(iso2)
                    if (iso3.isNotBlank() && !isoCodesList.contains(iso3)) isoCodesList.add(iso3)
                }
            }

            list.add(
                GdacsRawEvent(
                    eventId = eventId,
                    eventType = eventType,
                    eventName = eventName,
                    name = name,
                    description = description,
                    alertLevel = alertLevel,
                    alertScore = alertScore,
                    latitude = lat,
                    longitude = lon,
                    countries = countriesList,
                    isoCodes = isoCodesList,
                    fromDate = fromDate,
                    toDate = toDate,
                    reportUrl = reportUrl,
                    severityText = severityText
                )
            )
        }

        return list
    }

    /**
     * 災害イベントが指定都市に影響を与えているかを空間・地理判定
     */
    private fun isEventAffectingCity(event: GdacsRawEvent, city: City): Boolean {
        // 1. 同一国判定 (2/3文字ISOコードによる部分一致誤爆を防止)
        val cityCountry = city.country.trim().lowercase(Locale.ROOT)
        val countryTokens = cityCountry.split(" ", ",", "·", "/", "(", ")").map { it.trim() }.filter { it.isNotEmpty() }
        val isSameCountry = if (cityCountry.isNotBlank()) {
            event.countries.any { c ->
                val cLower = c.trim().lowercase(Locale.ROOT)
                cLower.isNotBlank() && (
                    cityCountry == cLower ||
                    (cLower.length >= 4 && cityCountry.contains(cLower)) ||
                    (cityCountry.length >= 4 && cLower.contains(cityCountry))
                )
            } || event.isoCodes.any { code ->
                val cLower = code.trim().lowercase(Locale.ROOT)
                cLower.isNotBlank() && (
                    countryTokens.contains(cLower) ||
                    (cityCountry.contains("japan") || cityCountry.contains("日本")) && (cLower == "jp" || cLower == "jpn") ||
                    (cityCountry.contains("united states") || cityCountry.contains("アメリカ") || cityCountry.contains("米国")) && (cLower == "us" || cLower == "usa")
                )
            }
        } else {
            false
        }

        // 2. 距離計算 (座標欠損時は無限大として扱い、(0,0) 誤マッチを防止)
        val distanceKm = if (event.latitude.isFinite() && event.longitude.isFinite()) {
            calculateDistanceKm(city.latitude, city.longitude, event.latitude, event.longitude)
        } else {
            Double.POSITIVE_INFINITY
        }

        // 3. 災害種別ごとの影響半径判定
        return when (event.eventType.uppercase(Locale.ROOT)) {
            "TC" -> distanceKm <= 1000.0 || (isSameCountry && distanceKm <= 1500.0) // 熱帯低気圧・台風
            "EQ" -> distanceKm <= 600.0 // 大規模地震
            "FL" -> distanceKm <= 400.0 || (isSameCountry && distanceKm <= 800.0) // 洪水
            "VO" -> distanceKm <= 350.0 // 火山噴火
            "WF" -> distanceKm <= 250.0 || (isSameCountry && distanceKm <= 500.0) // 森林火災
            "DR" -> isSameCountry || distanceKm <= 500.0 // 干ばつ
            else -> distanceKm <= 300.0
        }
    }

    /**
     * 2地点間の大圏距離を計算 (Haversine Formula)
     */
    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = (sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)).coerceIn(0.0, 1.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    /**
     * テスト用のGDACS模擬警報リストを作成
     */
    fun createSampleAlerts(): List<GlobalAlertItem> {
        return listOf(
            GlobalAlertItem(
                id = "gdacs-demo-tc",
                source = AlertSource.GDACS,
                type = WeatherWarningType.SPECIAL_WARNING,
                severity = AlertSeverity.EXTREME,
                eventName = "Super Typhoon SHANSHAN (Category 4)",
                headline = "Tropical Cyclone SHANSHAN: Red Alert with wind gusts up to 215 km/h",
                description = "Tropical Cyclone SHANSHAN is currently active with maximum sustained winds of 175 km/h and gusts over 215 km/h. High potential for severe storm surge, extreme rainfall, and flash flooding in the coastal region.",
                instruction = "Stay away from low-lying coastal areas. Secure emergency kits and follow official evacuation orders from local authorities.",
                effectiveTime = "2026-09-26T00:00:00Z",
                expiresTime = "2026-09-28T00:00:00Z",
                areaDesc = "Western Pacific Basin",
                eventTypeCode = "TC"
            ),
            GlobalAlertItem(
                id = "gdacs-demo-eq",
                source = AlertSource.GDACS,
                type = WeatherWarningType.WARNING,
                severity = AlertSeverity.SEVERE,
                eventName = "M 6.8 Major Earthquake",
                headline = "Major Earthquake M6.8 at depth of 15 km: Orange Alert for humanitarian impact",
                description = "A strong earthquake of magnitude 6.8 has occurred. Potentially damaging ground shaking within 100 km radius. Moderate risk of secondary landslides in mountainous terrain.",
                instruction = "Check for gas leaks and structural damage. Prepare for possible aftershocks.",
                effectiveTime = "2026-09-26T06:12:00Z",
                expiresTime = "2026-09-27T06:12:00Z",
                areaDesc = "Regional Seismic Zone",
                eventTypeCode = "EQ"
            )
        )
    }
}
