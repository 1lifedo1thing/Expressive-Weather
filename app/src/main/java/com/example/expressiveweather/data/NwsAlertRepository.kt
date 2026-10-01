package com.example.expressiveweather.data

import android.util.Log
import com.example.expressiveweather.ui.components.WeatherWarningType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 米国国立気象局 (NOAA NWS) 公式気象警報取得リポジトリ
 *
 * エンドポイント: https://api.weather.gov/alerts/active?point={lat},{lon}
 * - Tornado Warning (竜巻警報), Flash Flood Warning (鉄砲水警報), Winter Storm Warning (暴風雪警報), Heat Advisory (高温注意報) などを取得
 */
object NwsAlertRepository {
    private const val TAG = "NwsAlertRepository"
    private const val BASE_URL = "https://api.weather.gov/alerts/active"
    private const val USER_AGENT = "ExpressiveWeather/1.0 (contact@expressiveweather.app; Android Weather App)"

    // 5分間のメモリキャッシュ
    private val alertCache = ConcurrentHashMap<String, Pair<Long, List<GlobalAlertItem>>>()
    private const val CACHE_TTL_MS = 5 * 60 * 1000L

    /**
     * 指定された都市がアメリカ合衆国（本土・アラスカ・ハワイ・海外領土）内にあるかを判定
     */
    fun isUnitedStates(city: City): Boolean {
        val country = city.country.trim()
        val cUpper = country.uppercase(Locale.US)
        if (cUpper.contains("UNITED STATES") ||
            cUpper == "USA" ||
            cUpper == "US" ||
            cUpper.contains("PUERTO RICO") ||
            cUpper.contains("GUAM") ||
            cUpper.contains("VIRGIN ISLANDS") ||
            country.contains("アメリカ") ||
            country.contains("米国")
        ) {
            return true
        }

        // カナダやメキシコなど他国が明示されている場合は米国とみなさない
        if (cUpper.contains("CANADA") || cUpper == "CA" ||
            cUpper.contains("MEXICO") || cUpper == "MX" ||
            cUpper.contains("CUBA") || cUpper.contains("BAHAMAS") ||
            country.contains("カナダ") || country.contains("メキシコ") ||
            country.contains("日本") || cUpper.contains("JAPAN")
        ) {
            return false
        }

        val lat = city.latitude
        val lon = city.longitude
        // 米国本土
        if (lat in 24.0..50.0 && lon in -125.0..-66.0) return true
        // アラスカ
        if (lat in 51.0..72.0 && (lon in -180.0..-130.0 || lon in 170.0..180.0)) return true
        // ハワイ
        if (lat in 18.0..23.0 && lon in -161.0..-154.0) return true
        // プエルトリコ
        if (lat in 17.5..18.6 && lon in -67.5..-65.0) return true

        return false
    }

    /**
     * 都市の緯度経度におけるアクティブなNWS気象警報を取得
     */
    suspend fun fetchActiveAlerts(city: City, forceFresh: Boolean = false): List<GlobalAlertItem> = withContext(Dispatchers.IO) {
        if (!isUnitedStates(city)) {
            return@withContext emptyList()
        }

        // 座標を小数点2桁に正規化してキャッシュキーとする (約1kmメッシュ)
        val latRound = String.format(Locale.US, "%.2f", city.latitude)
        val lonRound = String.format(Locale.US, "%.2f", city.longitude)
        val cacheKey = "$latRound,$lonRound"
        val now = System.currentTimeMillis()

        if (!forceFresh) {
            val cached = alertCache[cacheKey]
            if (cached != null && (now - cached.first) < CACHE_TTL_MS) {
                return@withContext cached.second
            }
        }

        try {
            val urlString = "$BASE_URL?point=$latRound,$lonRound"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/geo+json")
                connectTimeout = 6000
                readTimeout = 6000
            }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "NWS API returned HTTP $responseCode for $cacheKey")
                return@withContext emptyList()
            }

            val responseBody = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val items = parseNwsGeoJson(responseBody)
            alertCache[cacheKey] = Pair(now, items)
            Log.d(TAG, "Fetched ${items.size} active NWS alerts for ${city.name} ($cacheKey)")
            items
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch NWS alerts for ${city.name}: ${e.message}")
            emptyList()
        }
    }

    /**
     * NWS GeoJSON レスポンスのパース
     */
    private fun parseNwsGeoJson(jsonString: String): List<GlobalAlertItem> {
        val root = JSONObject(jsonString)
        val features = root.optJSONArray("features") ?: return emptyList()
        val resultList = mutableListOf<GlobalAlertItem>()

        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val props = feature.optJSONObject("properties") ?: continue

            val id = props.optString("id", props.optString("@id", "nws-$i"))
            val event = props.optString("event", "Weather Alert")
            val headline = props.optString("headline", "")
            val description = props.optString("description", "")
            val instruction = props.optString("instruction", "").takeIf { it.isNotBlank() }
            val severityStr = props.optString("severity", "Unknown")
            val urgencyStr = props.optString("urgency", "Unknown")
            val effective = props.optString("effective", "")
            val expires = props.optString("expires", "")
            val areaDesc = props.optString("areaDesc", "")
            val detailsUrl = props.optString("@id", "")

            val severity = when (severityStr.uppercase(Locale.US)) {
                "EXTREME" -> AlertSeverity.EXTREME
                "SEVERE" -> AlertSeverity.SEVERE
                "MODERATE" -> AlertSeverity.MODERATE
                "MINOR" -> AlertSeverity.MINOR
                else -> AlertSeverity.UNKNOWN
            }

            // カードタイプの判定
            // 新3色体系: 黄色：注意 (ADVISORY) / 橙色：警戒 (WARNING) / 赤色：特別警戒 (SPECIAL_WARNING)
            val isExtreme = severity == AlertSeverity.EXTREME ||
                    event.contains("Emergency", ignoreCase = true)
            val isWarning = severity == AlertSeverity.SEVERE ||
                    event.contains("Warning", ignoreCase = true)

            val type = when {
                isExtreme -> WeatherWarningType.SPECIAL_WARNING
                isWarning -> WeatherWarningType.WARNING
                else -> WeatherWarningType.ADVISORY
            }

            resultList.add(
                GlobalAlertItem(
                    id = id,
                    source = AlertSource.NWS,
                    type = type,
                    severity = severity,
                    eventName = event,
                    headline = headline,
                    description = description,
                    instruction = instruction,
                    effectiveTime = effective,
                    expiresTime = expires,
                    areaDesc = areaDesc,
                    detailsUrl = detailsUrl.ifBlank { null }
                )
            )
        }

        // 重要度順にソート (特別警戒 (赤) > 警戒 (橙) > 注意 (黄))
        return resultList.sortedWith(
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
     * テスト用のアメリカ模擬警報リストを作成
     */
    fun createSampleAlerts(): List<GlobalAlertItem> {
        return listOf(
            GlobalAlertItem(
                id = "nws-demo-tornado",
                source = AlertSource.NWS,
                type = WeatherWarningType.FOREIGN_WARNING,
                severity = AlertSeverity.EXTREME,
                eventName = "Tornado Warning",
                headline = "Tornado Warning issued September 26 at 8:45 AM CDT until 9:30 AM CDT by NWS Houston TX",
                description = "At 8:44 AM CDT, a severe thunderstorm capable of producing a tornado was located near Downtown, moving northeast at 35 mph. HAZARD: Damaging tornado and quarter size hail. Flying debris will be dangerous to those caught without shelter.",
                instruction = "TAKE COVER NOW! Move to an interior room on the lowest floor of a sturdy building. Avoid windows. If you are outdoors, in a mobile home, or in a vehicle, move to the closest substantial shelter and protect yourself from flying debris.",
                effectiveTime = "2026-09-26T08:45:00-05:00",
                expiresTime = "2026-09-26T09:30:00-05:00",
                areaDesc = "Harris County, Texas"
            ),
            GlobalAlertItem(
                id = "nws-demo-flood",
                source = AlertSource.NWS,
                type = WeatherWarningType.ADVISORY,
                severity = AlertSeverity.MODERATE,
                eventName = "Flash Flood Watch",
                headline = "Flash Flood Watch issued September 26 at 7:00 AM CDT until September 27 at 1:00 AM CDT by NWS",
                description = "Excessive runoff from heavy rainfall may result in flooding of rivers, creeks, streams, and other low-lying and flood-prone locations. Extensive street flooding is possible.",
                instruction = "You should monitor later forecasts and be prepared to take action should Flash Flood Warnings be issued.",
                effectiveTime = "2026-09-26T07:00:00-05:00",
                expiresTime = "2026-09-27T01:00:00-05:00",
                areaDesc = "Southeast Texas"
            )
        )
    }
}
