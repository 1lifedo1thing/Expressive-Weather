package io.github.nemynew.expressiveweather.data

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 気象庁 (JMA) 公式台風情報リポジトリ
 *
 * エンドポイント:
 * - targetTc.json: 現在アクティブな台風・熱帯低気圧の一覧
 * - {tcCode}/specifications.json: 諸元・実況・予報値（気圧、最大風速、強さ・大きさ階級、進行方向、速度）
 * - {tcCode}/forecast.json: 幾何情報（過去の経路、現在中心、予報円、暴風警戒域・強風域）
 */
object JmaTyphoonRepository {
    private const val TAG = "JmaTyphoonRepo"
    private const val BASE_URL = "https://www.jma.go.jp/bosai/typhoon/data"
    private const val USER_AGENT = "ExpressiveWeather/1.0 (Android; +https://github.com/nemy-new/Expressive-Weather)"
    private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5分キャッシュ

    @Volatile
    private var cachedTyphoons: List<JmaTyphoonItem>? = null
    @Volatile
    private var lastFetchTime: Long = 0L
    private val fetchMutex = Mutex()

    /**
     * 現在発生中の全台風の最新実況・進路予報リストを取得
     */
    suspend fun getActiveTyphoons(forceRefresh: Boolean = false): List<JmaTyphoonItem> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedTyphoons != null && (now - lastFetchTime) < CACHE_TTL_MS) {
            return cachedTyphoons ?: emptyList()
        }

        return withContext(Dispatchers.IO) {
            fetchMutex.withLock {
                val innerNow = System.currentTimeMillis()
                if (!forceRefresh && cachedTyphoons != null && (innerNow - lastFetchTime) < CACHE_TTL_MS) {
                    return@withLock cachedTyphoons ?: emptyList()
                }
                try {
                    val targetJsonStr = fetchHttp("$BASE_URL/targetTc.json") ?: return@withLock cachedTyphoons ?: emptyList()
                    val targetArray = JSONArray(targetJsonStr)
                    if (targetArray.length() == 0) {
                        cachedTyphoons = emptyList()
                        lastFetchTime = System.currentTimeMillis()
                        return@withLock emptyList()
                    }

                    val results = coroutineScope {
                        (0 until targetArray.length()).map { i ->
                            val item = targetArray.getJSONObject(i)
                            val tcCode = item.optString("tropicalCyclone")
                            val typhoonNumber = item.optString("typhoonNumber")
                            async {
                                fetchSingleTyphoon(tcCode, typhoonNumber)
                            }
                        }.awaitAll().filterNotNull()
                    }

                    cachedTyphoons = results
                    lastFetchTime = System.currentTimeMillis()
                    results
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch active typhoons from JMA", e)
                    cachedTyphoons ?: emptyList()
                }
            }
        }
    }

    private suspend fun fetchSingleTyphoon(tcCode: String, typhoonNumber: String): JmaTyphoonItem? {
        if (tcCode.isBlank()) return null
        return try {
            coroutineScope {
                val specDeferred = async { fetchHttp("$BASE_URL/$tcCode/specifications.json") }
                val forecastDeferred = async { fetchHttp("$BASE_URL/$tcCode/forecast.json") }

                val specJsonStr = specDeferred.await() ?: return@coroutineScope null
                val forecastJsonStr = forecastDeferred.await()

                parseTyphoonData(tcCode, typhoonNumber, specJsonStr, forecastJsonStr)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse single typhoon $tcCode", e)
            null
        }
    }

    private fun parseTyphoonData(
        tcCode: String,
        defaultTyphoonNumber: String,
        specJsonStr: String,
        forecastJsonStr: String?
    ): JmaTyphoonItem? {
        val specArray = JSONArray(specJsonStr)
        if (specArray.length() == 0) return null

        var rawNumber = defaultTyphoonNumber
        var nameJp = ""
        var nameEn = ""
        var issueTimeJst = ""

        val points = mutableListOf<JmaTyphoonPoint>()

        for (i in 0 until specArray.length()) {
            val item = specArray.getJSONObject(i)
            val partVal = item.opt("part")
            val partName = when (partVal) {
                is JSONObject -> partVal.optString("jp")
                is String -> partVal
                else -> ""
            }

            if (partName == "title") {
                rawNumber = item.optString("typhoonNumber", rawNumber)
                val nameObj = item.optJSONObject("name")
                nameJp = nameObj?.optString("jp") ?: ""
                nameEn = nameObj?.optString("en") ?: ""
                val issueObj = item.optJSONObject("issue")
                issueTimeJst = issueObj?.optString("JST") ?: ""
                continue
            }

            val isAnalysis = partName.contains("実況") || partName.contains("推定") || item.optInt("advancedHours", -1) == 0
            val advancedHours = item.optInt("advancedHours", 0)

            val validTimeObj = item.optJSONObject("validtime")
            val validTimeJst = validTimeObj?.optString("JST") ?: ""
            val validTimeFormatted = formatJstTime(validTimeJst)

            val posObj = item.optJSONObject("position")
            val degArr = posObj?.optJSONArray("deg") ?: continue
            val lat = degArr.optDouble(0, Double.NaN).takeIf { it.isFinite() } ?: continue
            val lon = degArr.optDouble(1, Double.NaN).takeIf { it.isFinite() } ?: continue

            val locationName = item.optString("location", "")
            val course = item.optString("course", "ほとんど停滞")

            val speedObj = item.optJSONObject("speed")
            val speedKmH = speedObj?.optString("km/h")
            val speedNote = speedObj?.optJSONObject("note")?.optString("jp")
            val speedText = when {
                !speedKmH.isNullOrBlank() -> "$speedKmH km/h"
                !speedNote.isNullOrBlank() -> speedNote
                else -> "ほとんど停滞"
            }

            val pressureStr = item.optString("pressure")
            val pressureHpa = pressureStr.toIntOrNull()

            val maxWindObj = item.optJSONObject("maximumWind")
            val sustainedObj = maxWindObj?.optJSONObject("sustained")
            val gustObj = maxWindObj?.optJSONObject("gust")
            val maxWindMs = sustainedObj?.optString("m/s")?.toIntOrNull()
            val maxWindKt = sustainedObj?.optString("kt")?.toIntOrNull()
            val gustWindMs = gustObj?.optString("m/s")?.toIntOrNull()
            val gustWindKt = gustObj?.optString("kt")?.toIntOrNull()

            val intensityStr = item.optString("intensity")
            val scaleStr = item.optString("scale")
            val intensity = JmaTyphoonIntensity.fromJmaString(intensityStr)
            val scale = JmaTyphoonScale.fromJmaString(scaleStr)

            val categoryVal = item.opt("category")
            val categoryName = when (categoryVal) {
                is JSONObject -> categoryVal.optString("jp", "台風")
                is String -> categoryVal
                else -> "台風"
            }

            val galeWarnArr = item.optJSONArray("galeWarning")
            val galeRadiusKm = galeWarnArr?.optJSONObject(0)?.optJSONObject("range")?.optFiniteDouble("km")

            val stormWarnArr = item.optJSONArray("stormWarning")
            val stormRadiusKm = stormWarnArr?.optJSONObject(0)?.optJSONObject("range")?.optFiniteDouble("km")

            val probCircleObj = item.optJSONObject("probabilityCircleRadius")
            val probRadiusKm = probCircleObj?.optFiniteDouble("km")

            val point = JmaTyphoonPoint(
                partName = partName,
                isAnalysis = isAnalysis,
                advancedHours = advancedHours,
                validTimeJst = validTimeJst,
                validTimeFormatted = validTimeFormatted,
                latitude = lat,
                longitude = lon,
                locationName = locationName,
                course = course,
                speedText = speedText,
                pressureHpa = pressureHpa,
                maxWindMs = maxWindMs,
                maxWindKt = maxWindKt,
                gustWindMs = gustWindMs,
                gustWindKt = gustWindKt,
                intensity = intensity,
                scale = scale,
                categoryName = categoryName,
                galeRadiusKm = galeRadiusKm,
                stormRadiusKm = stormRadiusKm,
                probabilityCircleRadiusKm = probRadiusKm,
                probabilityCircleRadiusMeters = probRadiusKm?.let { it * 1000.0 }
            )
            points.add(point)
        }

        val analysisPoint = points.firstOrNull { it.isAnalysis } ?: points.firstOrNull() ?: return null
        val forecastPoints = points.filter { !it.isAnalysis }

        // forecast.json からジオメトリ（過去経路、予報円接線、強風域半径など）を抽出
        val pastTracks = mutableListOf<LatLng>()
        var galeWarningMeters: Double? = null
        var stormWarningMeters: Double? = null
        val probabilityCircles = mutableListOf<JmaProbabilityCircleGeometry>()
        val stormWarningAreas = mutableListOf<JmaStormWarningAreaGeometry>()

        if (!forecastJsonStr.isNullOrBlank()) {
            try {
                val forecastArray = JSONArray(forecastJsonStr)
                for (i in 0 until forecastArray.length()) {
                    val fItem = forecastArray.getJSONObject(i)
                    val partVal = fItem.opt("part")
                    val partName = when (partVal) {
                        is JSONObject -> partVal.optString("jp")
                        is String -> partVal
                        else -> ""
                    }

                    if (partName.contains("実況") || fItem.optInt("advancedHours", -1) == 0) {
                        val trackObj = fItem.optJSONObject("track")
                        val preTrack = trackObj?.optJSONArray("preTyphoon")
                        val typhoonTrack = trackObj?.optJSONArray("typhoon")

                        if (preTrack != null) {
                            for (p in 0 until preTrack.length()) {
                                val pt = preTrack.getJSONArray(p)
                                pastTracks.add(LatLng(pt.getDouble(0), pt.getDouble(1)))
                            }
                        }
                        if (typhoonTrack != null) {
                            for (p in 0 until typhoonTrack.length()) {
                                val pt = typhoonTrack.getJSONArray(p)
                                pastTracks.add(LatLng(pt.getDouble(0), pt.getDouble(1)))
                            }
                        }

                        val galeArea = fItem.optJSONObject("galeWarningArea")
                        galeWarningMeters = galeArea?.optFiniteDouble("radius")
                        val stormArea = fItem.optJSONObject("stormWarningArea")
                        stormWarningMeters = stormArea?.optFiniteDouble("radius")
                        if (stormArea != null) {
                            val sCenterArr = stormArea.optJSONArray("center")
                            val sLat = sCenterArr?.optDouble(0, Double.NaN)?.takeIf { it.isFinite() } ?: analysisPoint.latitude
                            val sLon = sCenterArr?.optDouble(1, Double.NaN)?.takeIf { it.isFinite() } ?: analysisPoint.longitude
                            val sRad = stormWarningMeters ?: 0.0
                            if (sRad > 0.0) {
                                stormWarningAreas.add(
                                    JmaStormWarningAreaGeometry(
                                        arcs = listOf(listOf(sLat, sLon, sRad, 0.0, 360.0)),
                                        lines = emptyList()
                                    )
                                )
                            }
                        }
                    } else if (partName.contains("予報")) {
                        val centerArr = fItem.optJSONArray("center") ?: continue
                        val centerLat = centerArr.optDouble(0, Double.NaN).takeIf { it.isFinite() } ?: continue
                        val centerLon = centerArr.optDouble(1, Double.NaN).takeIf { it.isFinite() } ?: continue

                        val probCircle = fItem.optJSONObject("probabilityCircle")
                        val radiusMeters = probCircle?.optFiniteDouble("radius") ?: 0.0
                        val tangentArr = probCircle?.optJSONArray("tangent")
                        val tangents = mutableListOf<List<LatLng>>()
                        if (tangentArr != null) {
                            for (t in 0 until tangentArr.length()) {
                                val lineArr = tangentArr.getJSONArray(t)
                                val p1 = lineArr.getJSONArray(0)
                                val p2 = lineArr.getJSONArray(1)
                                tangents.add(listOf(
                                    LatLng(p1.getDouble(0), p1.getDouble(1)),
                                    LatLng(p2.getDouble(0), p2.getDouble(1))
                                ))
                            }
                        }

                        val validTimeObj = fItem.optJSONObject("validtime")
                        val vTimeJst = validTimeObj?.optString("JST") ?: ""

                        if (radiusMeters > 0.0) {
                            probabilityCircles.add(
                                JmaProbabilityCircleGeometry(
                                    center = LatLng(centerLat, centerLon),
                                    radiusMeters = radiusMeters,
                                    tangents = tangents,
                                    validTimeJst = vTimeJst,
                                    labelText = formatShortTime(vTimeJst)
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse forecast geometry for $tcCode", e)
            }
        }

        // 過去経路が取れなかった場合は実況地点を追加
        if (pastTracks.isEmpty()) {
            pastTracks.add(LatLng(analysisPoint.latitude, analysisPoint.longitude))
        }

        val formattedNumber = formatTyphoonNumber(rawNumber)
        val formattedIssueTime = formatIssueTime(issueTimeJst)

        return JmaTyphoonItem(
            id = tcCode,
            rawTyphoonNumber = rawNumber,
            formattedNumber = formattedNumber,
            nameJp = nameJp.ifBlank { "台風" },
            nameEn = nameEn.ifBlank { "Typhoon" },
            issueTimeJst = issueTimeJst,
            formattedIssueTime = formattedIssueTime,
            analysis = analysisPoint,
            forecasts = forecastPoints,
            pastTrack = pastTracks,
            galeWarningRadiusMeters = galeWarningMeters ?: analysisPoint.galeRadiusKm?.let { it * 1000.0 },
            stormWarningRadiusMeters = stormWarningMeters ?: analysisPoint.stormRadiusKm?.let { it * 1000.0 },
            probabilityCircles = probabilityCircles,
            stormWarningAreas = stormWarningAreas
        )
    }

    /**
     * "2627" -> "第27号", "2601" -> "第1号", "27" -> "第27号"
     */
    private fun formatTyphoonNumber(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "台風"
        val numPart = if (trimmed.length >= 4) trimmed.takeLast(2) else trimmed
        val intVal = numPart.toIntOrNull() ?: return "第${trimmed}号"
        return "第${intVal}号"
    }

    private fun formatJstTime(isoJst: String): String {
        // "2026-10-01T18:00:00+09:00" -> "10/01 18:00"
        return try {
            if (isoJst.length >= 16) {
                val month = isoJst.substring(5, 7)
                val day = isoJst.substring(8, 10)
                val hour = isoJst.substring(11, 13)
                val minute = isoJst.substring(14, 16)
                "$month/$day $hour:$minute"
            } else isoJst
        } catch (e: Exception) {
            isoJst
        }
    }

    private fun formatShortTime(isoJst: String): String {
        // "2026-10-02T18:00:00+09:00" -> "02日18時"
        return try {
            if (isoJst.length >= 13) {
                val day = isoJst.substring(8, 10)
                val hour = isoJst.substring(11, 13)
                "${day}日${hour}時"
            } else isoJst
        } catch (e: Exception) {
            isoJst
        }
    }

    private fun formatIssueTime(isoJst: String): String {
        return try {
            if (isoJst.length >= 16) {
                val month = isoJst.substring(5, 7)
                val day = isoJst.substring(8, 10)
                val hour = isoJst.substring(11, 13)
                val minute = isoJst.substring(14, 16)
                "$month/$day $hour:$minute 発表"
            } else "最新発表"
        } catch (e: Exception) {
            "最新発表"
        }
    }

    /**
     * 2点間の直線距離 (km) を計算 (Haversine formula)
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = (sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)).coerceIn(0.0, 1.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c * 10.0).roundToInt() / 10.0
    }

    /**
     * 2点間の方位を計算して日本語の方角（南東、南、南西等）を返す
     */
    fun calculateBearingDirection(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, language: AppLanguage): String {
        val y = sin(Math.toRadians(toLon - fromLon)) * cos(Math.toRadians(toLat))
        val x = cos(Math.toRadians(fromLat)) * sin(Math.toRadians(toLat)) -
                sin(Math.toRadians(fromLat)) * cos(Math.toRadians(toLat)) * cos(Math.toRadians(toLon - fromLon))
        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360

        val directionsJa = listOf("北", "北北東", "北東", "東北東", "東", "東南東", "南東", "南南東", "南", "南南西", "南西", "西南西", "西", "西北西", "北西", "北北西")
        val directionsEn = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")

        val index = ((bearing + 11.25) / 22.5).toInt() % 16
        return when (language) {
            AppLanguage.JAPANESE -> directionsJa[index]
            AppLanguage.ENGLISH -> directionsEn[index]
            AppLanguage.SIMPLIFIED_CHINESE -> directionsJa[index] // 漢字表記共通
            AppLanguage.TRADITIONAL_CHINESE -> directionsJa[index]
        }
    }

    private suspend fun fetchHttp(urlString: String): String? {
        return AppHttpClient.getTextOrNull(
            url = urlString,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Accept" to "application/json"
            ),
            timeoutMs = 8_000L
        )
    }

    /**
     * 台風未発生時や開発・検証用のテスト台風サンプル
     */
    fun createSampleTyphoon(): JmaTyphoonItem {
        val analysis = JmaTyphoonPoint(
            partName = "実況",
            isAnalysis = true,
            advancedHours = 0,
            validTimeJst = "2026-10-01T18:00:00+09:00",
            validTimeFormatted = "10/01 18:00",
            latitude = 22.0,
            longitude = 146.4,
            locationName = "小笠原近海",
            course = "北北西",
            speedText = "15 km/h",
            pressureHpa = 945,
            maxWindMs = 45,
            maxWindKt = 85,
            gustWindMs = 60,
            gustWindKt = 120,
            intensity = JmaTyphoonIntensity.VERY_STRONG,
            scale = JmaTyphoonScale.LARGE,
            categoryName = "台風",
            galeRadiusKm = 480.0,
            stormRadiusKm = 220.0
        )

        val f1 = JmaTyphoonPoint(
            partName = "予報　２４時間後",
            isAnalysis = false,
            advancedHours = 24,
            validTimeJst = "2026-10-02T18:00:00+09:00",
            validTimeFormatted = "10/02 18:00",
            latitude = 25.0,
            longitude = 145.1,
            locationName = "小笠原諸島近海",
            course = "北北西",
            speedText = "15 km/h",
            pressureHpa = 945,
            maxWindMs = 45,
            maxWindKt = 85,
            gustWindMs = 60,
            gustWindKt = 120,
            intensity = JmaTyphoonIntensity.VERY_STRONG,
            scale = JmaTyphoonScale.LARGE,
            categoryName = "台風",
            probabilityCircleRadiusKm = 150.0
        )

        val f2 = JmaTyphoonPoint(
            partName = "予報　４８時間後",
            isAnalysis = false,
            advancedHours = 48,
            validTimeJst = "2026-10-03T18:00:00+09:00",
            validTimeFormatted = "10/03 18:00",
            latitude = 29.5,
            longitude = 143.5,
            locationName = "日本の南",
            course = "北",
            speedText = "20 km/h",
            pressureHpa = 955,
            maxWindMs = 40,
            maxWindKt = 80,
            gustWindMs = 55,
            gustWindKt = 110,
            intensity = JmaTyphoonIntensity.STRONG,
            scale = JmaTyphoonScale.LARGE,
            categoryName = "台風",
            probabilityCircleRadiusKm = 240.0
        )

        val f3 = JmaTyphoonPoint(
            partName = "予報　７２時間後",
            isAnalysis = false,
            advancedHours = 72,
            validTimeJst = "2026-10-04T18:00:00+09:00",
            validTimeFormatted = "10/04 18:00",
            latitude = 34.0,
            longitude = 142.0,
            locationName = "関東の南東約200km",
            course = "北東",
            speedText = "35 km/h",
            pressureHpa = 970,
            maxWindMs = 35,
            maxWindKt = 70,
            gustWindMs = 50,
            gustWindKt = 100,
            intensity = JmaTyphoonIntensity.STRONG,
            scale = JmaTyphoonScale.LARGE,
            categoryName = "台風",
            probabilityCircleRadiusKm = 320.0
        )

        return JmaTyphoonItem(
            id = "TC2633_SAMPLE",
            rawTyphoonNumber = "2627",
            formattedNumber = "第27号",
            nameJp = "チョーイワン",
            nameEn = "Choi-wan",
            issueTimeJst = "2026-10-01T19:05:00+09:00",
            formattedIssueTime = "10/01 19:05 発表",
            analysis = analysis,
            forecasts = listOf(f1, f2, f3),
            pastTrack = listOf(
                LatLng(16.0, 150.8),
                LatLng(16.5, 149.5),
                LatLng(18.0, 147.5),
                LatLng(22.0, 146.4)
            ),
            galeWarningRadiusMeters = 480000.0,
            stormWarningRadiusMeters = 220000.0,
            probabilityCircles = listOf(
                JmaProbabilityCircleGeometry(
                    center = LatLng(25.0, 145.1),
                    radiusMeters = 150000.0,
                    tangents = emptyList(),
                    validTimeJst = "2026-10-02T18:00:00+09:00",
                    labelText = "02日18時"
                ),
                JmaProbabilityCircleGeometry(
                    center = LatLng(29.5, 143.5),
                    radiusMeters = 240000.0,
                    tangents = emptyList(),
                    validTimeJst = "2026-10-03T18:00:00+09:00",
                    labelText = "03日18時"
                ),
                JmaProbabilityCircleGeometry(
                    center = LatLng(34.0, 142.0),
                    radiusMeters = 320000.0,
                    tangents = emptyList(),
                    validTimeJst = "2026-10-04T18:00:00+09:00",
                    labelText = "04日18時"
                )
            )
        )
    }
}
