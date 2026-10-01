package com.example.expressiveweather.data

import android.util.Log
import com.example.expressiveweather.ui.components.WeatherWarningType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * 世界気象機関 (WMO: World Meteorological Organization) 公式気象警報取得リポジトリ
 *
 * エンドポイント: https://severeweather.wmo.int/json/wmo_all.json
 * - 世界190カ国以上の国家気象水文機関 (NMHS) の公式CAP気象警報をリアルタイム集約
 * - 基本原則:
 *   1. 日本: 気象庁 (JMA / キキクル) 直接取得を最優先
 *   2. 米国: NOAA / NWS 直接取得を最優先
 *   3. 欧州・加盟39カ国: MeteoAlarm (EUMETNET) 直接取得を最優先
 *   4. 上記以外の全世界 (カナダ、豪州、ニュージーランド、東アジア、東南アジア、中南米、アフリカ等): WMO公式警報を使用
 *   ただし「常にWMOをオンにする (alwaysEnabled)」が有効な場合は日本・米国・MeteoAlarm対象国でも並行取得可能。
 * - 新カラー体系: 黄色：注意 (ADVISORY) / 橙色：警戒 (WARNING) / 赤色：特別警戒 (SPECIAL_WARNING)
 */
object WmoAlertRepository {
    private const val TAG = "WmoAlertRepository"
    private const val WMO_ALL_URL = "https://severeweather.wmo.int/json/wmo_all.json"
    private const val USER_AGENT = "ExpressiveWeather/1.0 (contact@expressiveweather.app; Android Weather App)"

    // 15分間のグローバルメモリキャッシュ
    private var cachedItems: List<WmoRawItem> = emptyList()
    private var lastFetchTimeMillis: Long = 0L
    private const val CACHE_TTL_MS = 15 * 60 * 1000L
    private val fetchMutex = Mutex()

    /**
     * WMOの生警報データ
     */
    data class WmoRawItem(
        val id: String,
        val event: String,
        val headline: String,
        val areaDesc: String,
        val mid: String,
        val severityNumber: Int, // 1=Minor, 2=Moderate, 3=Severe, 4=Extreme
        val sent: String,
        val expires: String,
        val effective: String,
        val url: String
    )

    /**
     * 日本・米国・MeteoAlarm対象国など、直接取得が優先される地域かどうかを判定
     * alwaysEnabled = true の場合は除外されない（falseを返却）
     */
    fun isCityExcludedFromWmo(city: City, alwaysEnabled: Boolean = false): Boolean {
        if (alwaysEnabled) return false
        val inJapan = isLocationOrCountryInJapan(city.country, city.latitude, city.longitude)
        val inUs = NwsAlertRepository.isUnitedStates(city)
        val inMeteoAlarm = MeteoAlarmRepository.isSupported(city)
        return inJapan || inUs || inMeteoAlarm
    }

    /**
     * 都市の国名・緯度経度に基づき、該当するWMO公式気象警報を取得
     *
     * @param city 対象都市
     * @param forceFresh 強制再取得フラグ
     * @param alwaysEnabled 日本・米国・MeteoAlarm対象国でも常にWMOを取得するかどうか（デフォルト: false）
     */
    suspend fun fetchAlertsForCity(
        city: City,
        forceFresh: Boolean = false,
        alwaysEnabled: Boolean = false
    ): List<GlobalAlertItem> = withContext(Dispatchers.IO) {
        // 日本・米国・MeteoAlarm対象地域ガード: alwaysEnabled でない限り、各地域の直接取得を尊重しWMOは完全スキップ
        if (isCityExcludedFromWmo(city, alwaysEnabled)) {
            return@withContext emptyList()
        }

        val allItems = getAllWmoItems(forceFresh)
        if (allItems.isEmpty()) return@withContext emptyList()

        val matchingItems = mutableListOf<GlobalAlertItem>()

        for (item in allItems) {
            if (isEventAffectingCity(item, city)) {
                // 新3色体系: 黄色：注意 (ADVISORY) / 橙色：警戒 (WARNING) / 赤色：特別警戒 (SPECIAL_WARNING)
                val (type, severity) = determineTypeAndSeverity(item)

                val effectiveTime = item.effective.ifBlank { item.sent }
                val headlineText = item.headline.ifBlank { item.event }
                val desc = if (item.areaDesc.isNotBlank()) {
                    "$headlineText (${item.areaDesc})"
                } else {
                    headlineText
                }

                matchingItems.add(
                    GlobalAlertItem(
                        id = "wmo-${item.id.hashCode()}",
                        source = AlertSource.WMO,
                        type = type,
                        severity = severity,
                        eventName = item.event.ifBlank { "Weather Alert" },
                        headline = headlineText,
                        description = desc,
                        instruction = null,
                        effectiveTime = effectiveTime,
                        expiresTime = item.expires.ifBlank { null },
                        areaDesc = item.areaDesc.take(120),
                        detailsUrl = if (item.url.isNotBlank()) "https://severeweather.wmo.int/v2/${item.url}" else null,
                        eventTypeCode = null
                    )
                )
            }
        }

        // 特別警戒 (赤色) > 警戒 (橙色) > 注意 (黄色) の順でソート
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
     * 重大度とカード色（赤・橙・黄）の判定
     * - 赤色: SPECIAL_WARNING (特別警戒)
     * - 橙色: WARNING (警戒)
     * - 黄色: ADVISORY (注意)
     */
    private fun determineTypeAndSeverity(item: WmoRawItem): Pair<WeatherWarningType, AlertSeverity> {
        val s = item.severityNumber
        val combinedText = "${item.event} ${item.headline}".lowercase(Locale.ROOT)

        val isExtreme = s >= 4 ||
                combinedText.contains("extreme") ||
                combinedText.contains("emergency") ||
                combinedText.contains("red warning") ||
                combinedText.contains("red alert") ||
                combinedText.contains("特别重大") ||
                combinedText.contains("特別")

        if (isExtreme) {
            return Pair(WeatherWarningType.SPECIAL_WARNING, AlertSeverity.EXTREME)
        }

        val isSevere = s == 3 ||
                combinedText.contains("severe") ||
                combinedText.contains("warning") ||
                combinedText.contains("orange warning") ||
                combinedText.contains("orange alert") ||
                combinedText.contains("gale") ||
                combinedText.contains("blizzard") ||
                combinedText.contains("flash flood") ||
                combinedText.contains("警報") ||
                combinedText.contains("预警")

        if (isSevere) {
            return Pair(WeatherWarningType.WARNING, AlertSeverity.SEVERE)
        }

        return Pair(WeatherWarningType.ADVISORY, AlertSeverity.MODERATE)
    }

    /**
     * 都市と警報の照合ロジック
     */
    private fun isEventAffectingCity(item: WmoRawItem, city: City): Boolean {
        val cityNameLower = city.name.lowercase(Locale.ROOT).trim()
        val cityNameEnLower = city.nameEn.lowercase(Locale.ROOT).trim()
        val countryNameLower = city.country.lowercase(Locale.ROOT).trim()
        val areaLower = item.areaDesc.lowercase(Locale.ROOT)
        val headlineLower = item.headline.lowercase(Locale.ROOT)
        val urlLower = item.url.lowercase(Locale.ROOT)

        // 1. URLプレフィックスから国コードを判定
        val countryMatches = isCountryMatching(urlLower, item.mid, countryNameLower)

        if (!countryMatches) {
            // 国が一致しない場合でも、areaDescやheadlineに国名が明示されているかチェック
            if (!areaLower.contains(countryNameLower) && !headlineLower.contains(countryNameLower)) {
                return false
            }
        }

        // 2. 都市名（日本語名または英語名）が areaDesc または headline に含まれる場合 -> 即一致
        if (cityNameLower.length >= 3 && (areaLower.contains(cityNameLower) || headlineLower.contains(cityNameLower))) {
            return true
        }
        if (cityNameEnLower.length >= 3 && (areaLower.contains(cityNameEnLower) || headlineLower.contains(cityNameEnLower))) {
            return true
        }

        // 3. 国が一致しており、かつ広域警報（全国または全州・全管区）の場合
        if (countryMatches) {
            // 国全体が対象の警報、またはareaDescが空の場合
            if (areaLower.isBlank() || areaLower == countryNameLower || areaLower.contains("entire") || areaLower.contains("nationwide")) {
                return true
            }
        }

        return false
    }

    /**
     * URLやmidコードから国が一致しているかを判定
     */
    private fun isCountryMatching(urlLower: String, mid: String, countryLower: String): Boolean {
        val prefixMap = mapOf(
            "de-dwd" to listOf("germany", "deutschland", "ドイツ"),
            "ca-msc" to listOf("canada", "カナダ"),
            "au-bom" to listOf("australia", "オーストラリア"),
            "cn-cma" to listOf("china", "中国"),
            "gb-met" to listOf("united kingdom", "uk", "great britain", "england", "scotland", "wales", "イギリス", "英国"),
            "uk-met" to listOf("united kingdom", "uk", "great britain", "england", "scotland", "wales", "イギリス", "英国"),
            "fr-met" to listOf("france", "フランス"),
            "it-met" to listOf("italy", "italia", "イタリア"),
            "es-aem" to listOf("spain", "españa", "スペイン"),
            "kr-kma" to listOf("south korea", "korea", "韓国"),
            "in-imd" to listOf("india", "インド"),
            "br-inm" to listOf("brazil", "brasil", "ブラジル"),
            "za-saw" to listOf("south africa", "南アフリカ"),
            "ph-pag" to listOf("philippines", "フィリピン"),
            "th-tmd" to listOf("thailand", "タイ"),
            "id-bmk" to listOf("indonesia", "インドネシア"),
            "id-ina" to listOf("indonesia", "インドネシア"),
            "nz-met" to listOf("new zealand", "ニュージーランド"),
            "ch-met" to listOf("switzerland", "schweiz", "suisse", "スイス"),
            "at-zam" to listOf("austria", "österreich", "オーストリア"),
            "nl-knm" to listOf("netherlands", "holland", "オランダ"),
            "be-rmi" to listOf("belgium", "belgique", "ベルギー"),
            "se-smh" to listOf("sweden", "sverige", "スウェーデン"),
            "no-met" to listOf("norway", "norge", "ノルウェー"),
            "fi-fmi" to listOf("finland", "suomi", "フィンランド"),
            "dk-dmi" to listOf("denmark", "danmark", "デンマーク"),
            "ie-met" to listOf("ireland", "アイルランド"),
            "pl-img" to listOf("poland", "polska", "ポーランド"),
            "pt-ipm" to listOf("portugal", "ポルトガル"),
            "gr-hnm" to listOf("greece", "ギリシャ"),
            "kg-met" to listOf("kyrgyzstan", "キルギス"),
            "us-noa" to listOf("united states", "usa", "us", "america", "アメリカ", "米国"),
            "jp-jma" to listOf("japan", "日本")
        )

        for ((prefix, countryKeywords) in prefixMap) {
            if (urlLower.contains(prefix)) {
                if (countryKeywords.any { countryLower.contains(it) || it.contains(countryLower) }) {
                    return true
                }
            }
        }

        // mid (WMO国コード) による判定
        if (mid == "001" && (countryLower.contains("china") || countryLower.contains("中国"))) return true
        if (mid == "016" && (countryLower.contains("germany") || countryLower.contains("ドイツ"))) return true
        if (mid == "056" && (countryLower.contains("canada") || countryLower.contains("カナダ"))) return true
        if (mid == "185" && (countryLower.contains("australia") || countryLower.contains("オーストラリア"))) return true
        if (mid == "043" && (countryLower.contains("indonesia") || countryLower.contains("インドネシア"))) return true
        if (mid == "177" && (countryLower.contains("kyrgyz") || countryLower.contains("キルギス"))) return true
        if (mid == "093" && (countryLower.contains("united states") || countryLower.contains("usa") || countryLower.contains("米国"))) return true

        return false
    }

    /**
     * WMO SWIC 3.0 から全警報一覧を取得（15分キャッシュ）
     */
    private suspend fun getAllWmoItems(forceFresh: Boolean = false): List<WmoRawItem> = fetchMutex.withLock {
        val now = System.currentTimeMillis()
        if (!forceFresh && cachedItems.isNotEmpty() && (now - lastFetchTimeMillis) < CACHE_TTL_MS) {
            return cachedItems
        }

        try {
            val url = URL(WMO_ALL_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "WMO SWIC API returned HTTP ${conn.responseCode}")
                return cachedItems
            }

            val jsonText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val root = JSONObject(jsonText)
            val itemsArray = root.optJSONArray("items") ?: return cachedItems

            val resultList = mutableListOf<WmoRawItem>()
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val event = obj.optString("event", "")
                val headline = obj.optString("headline", "")
                val areaDesc = obj.optString("areaDesc", "")
                val mid = obj.optString("mid", "")
                val severityNum = obj.optInt("s", 2)
                val sent = obj.optString("sent", "")
                val expires = obj.optString("expires", "")
                val effective = obj.optString("effective", "")
                val capUrl = obj.optString("capURL", "").ifBlank { obj.optString("url", "") }

                resultList.add(
                    WmoRawItem(
                        id = id,
                        event = event,
                        headline = headline,
                        areaDesc = areaDesc,
                        mid = mid,
                        severityNumber = severityNum,
                        sent = sent,
                        expires = expires,
                        effective = effective,
                        url = capUrl
                    )
                )
            }

            cachedItems = resultList
            lastFetchTimeMillis = now
            Log.d(TAG, "Fetched ${resultList.size} WMO items from SWIC")
            resultList
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch WMO alerts: ${e.message}")
            cachedItems
        }
    }

    /**
     * テスト用のWMO模擬警報リストを作成 (赤色: 特別警戒 / 橙色: 警戒 / 黄色: 注意)
     */
    fun createSampleAlerts(): List<GlobalAlertItem> {
        return listOf(
            GlobalAlertItem(
                id = "wmo-demo-blizzard",
                source = AlertSource.WMO,
                type = WeatherWarningType.SPECIAL_WARNING,
                severity = AlertSeverity.EXTREME,
                eventName = "Severe Blizzard",
                headline = "Severe Blizzard Alert issued by National Meteorological Service",
                description = "Extreme snowfall exceeding 50cm with wind gusts over 120 km/h. Near zero visibility and severe whiteout conditions expected.",
                instruction = "Stay indoors. Travel is extremely dangerous and strictly not recommended.",
                effectiveTime = "2026-09-29T10:00:00Z",
                expiresTime = "2026-09-30T00:00:00Z",
                areaDesc = "Greater Metropolitan Area",
                detailsUrl = "https://severeweather.wmo.int"
            ),
            GlobalAlertItem(
                id = "wmo-demo-gale",
                source = AlertSource.WMO,
                type = WeatherWarningType.WARNING,
                severity = AlertSeverity.SEVERE,
                eventName = "Gale Warning",
                headline = "Gale Warning issued for coastal and inland regions",
                description = "Sustained winds of 70-85 km/h with localized damaging gusts. Loose outdoor objects may be blown away.",
                instruction = "Secure outdoor furniture and take precautions against falling branches.",
                effectiveTime = "2026-09-29T12:00:00Z",
                expiresTime = "2026-09-29T21:00:00Z",
                areaDesc = "Coastal & Valley Districts",
                detailsUrl = "https://severeweather.wmo.int"
            ),
            GlobalAlertItem(
                id = "wmo-demo-thunder",
                source = AlertSource.WMO,
                type = WeatherWarningType.ADVISORY,
                severity = AlertSeverity.MODERATE,
                eventName = "Thunderstorm Advisory",
                headline = "Thunderstorm Advisory in effect for this afternoon",
                description = "Scattered thunderstorms capable of producing lightning, brief heavy downpours, and pea-sized hail.",
                instruction = "Seek shelter in a sturdy building if thunder roars.",
                effectiveTime = "2026-09-29T14:00:00Z",
                expiresTime = "2026-09-29T19:00:00Z",
                areaDesc = "Central District",
                detailsUrl = "https://severeweather.wmo.int"
            )
        )
    }
}
