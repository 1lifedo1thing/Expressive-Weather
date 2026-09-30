package com.example.expressiveweather.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * 市区町村単位の警戒レベル・キキクル情報・警報注意報の判定・管理リポジトリ
 */
object JmaAlertRepository {
    private const val TAG = "JmaAlertRepository"
    private const val BASE_URL = "https://www.jma.go.jp/bosai/warning/data/warning"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; ExpressiveWeather) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    // 直近の取得結果キャッシュ（短期間の重複通信を抑制: 有効期間2分）
    private val alertCache = ConcurrentHashMap<String, Pair<Long, MunicipalityAlertInfo>>()

    // JMA 警報コードマッピング辞書
    val WARNING_CODE_MAP = mapOf(
        // 特別警報
        "32" to Triple("暴風雪特別警報", true, true),
        "33" to Triple("大雨特別警報", true, true),
        "35" to Triple("暴風特別警報", true, true),
        "36" to Triple("大雪特別警報", true, true),
        "37" to Triple("波浪特別警報", true, true),
        "38" to Triple("高潮特別警報", true, true),
        // 警報
        "02" to Triple("暴風雪警報", true, false),
        "03" to Triple("大雨警報", true, false),
        "04" to Triple("洪水警報", true, false),
        "05" to Triple("暴風警報", true, false),
        "06" to Triple("大雪警報", true, false),
        "07" to Triple("波浪警報", true, false),
        "08" to Triple("高潮警報", true, false),
        // 注意報
        "10" to Triple("大雨注意報", false, false),
        "12" to Triple("大雪注意報", false, false),
        "13" to Triple("風雪注意報", false, false),
        "14" to Triple("雷注意報", false, false),
        "15" to Triple("強風注意報", false, false),
        "16" to Triple("波浪注意報", false, false),
        "17" to Triple("融雪注意報", false, false),
        "18" to Triple("洪水注意報", false, false),
        "19" to Triple("高潮注意報", false, false),
        "20" to Triple("濃霧注意報", false, false),
        "21" to Triple("乾燥注意報", false, false),
        "22" to Triple("なだれ注意報", false, false),
        "23" to Triple("低温注意報", false, false),
        "24" to Triple("霜注意報", false, false),
        "25" to Triple("着氷注意報", false, false),
        "26" to Triple("着雪注意報", false, false)
    )

    /**
     * 市区町村の警報・キキクル警戒レベル情報を取得・判定
     */
    suspend fun fetchAlertForMunicipality(
        municipality: JmaMunicipality,
        forceFresh: Boolean = false
    ): MunicipalityAlertInfo = withContext(Dispatchers.IO) {
        val cacheKey = municipality.code
        val now = System.currentTimeMillis()
        if (!forceFresh) {
            val cached = alertCache[cacheKey]
            if (cached != null && (now - cached.first) < 120_000L) {
                return@withContext cached.second
            }
        }

        try {
            val warningJson = fetchOfficeWarningJson(municipality.officeCode)
            val alertInfo = evaluateAlert(warningJson, municipality)
            alertCache[cacheKey] = Pair(now, alertInfo)
            alertInfo
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch alerts for ${municipality.name} (${municipality.code}): ${e.message}")
            MunicipalityAlertInfo(
                municipalityCode = municipality.code,
                municipalityName = municipality.name,
                prefectureCode = municipality.officeCode,
                prefectureName = municipality.prefName,
                alertLevel = AlertLevel.LEVEL_0
            )
        }
    }

    /**
     * 複数登録地点（保存地点）の中で最も高い警戒レベルの情報を特定
     */
    suspend fun evaluateAlertsForSavedCities(
        context: Context,
        cities: List<City>
    ): MunicipalityAlertInfo? = withContext(Dispatchers.IO) {
        if (cities.isEmpty()) return@withContext null
        JmaMunicipalityResolver.init(context)

        coroutineScope {
            val alerts = cities.map { city ->
                async {
                    val muni = JmaMunicipalityResolver.resolveFromLocation(
                        city.latitude,
                        city.longitude,
                        city.name,
                        city.country
                    )
                    fetchAlertForMunicipality(muni)
                }
            }.awaitAll()

            // 警戒レベル降順（レベル5 > 4 > 3 > 2）で最も危険な自治体を返却
            alerts.filter { it.alertLevel.level >= 2 }
                .maxByOrNull { it.alertLevel.level }
        }
    }

    /**
     * 気象庁オフィス Warning JSON から自治体コード配下の警報・危険度フラグ・警戒レベルを精密判定
     */
    fun evaluateAlert(
        warningJson: JSONObject?,
        municipality: JmaMunicipality
    ): MunicipalityAlertInfo {
        if (warningJson == null) {
            return MunicipalityAlertInfo(
                municipalityCode = municipality.code,
                municipalityName = municipality.name,
                prefectureCode = municipality.officeCode,
                prefectureName = municipality.prefName,
                alertLevel = AlertLevel.LEVEL_0
            )
        }

        val reportDatetime = warningJson.optString("reportDatetime").ifBlank { null }
        val publishingOffice = warningJson.optString("publishingOffice").ifBlank { null }
        val headlineText = warningJson.optString("headlineText").trim().ifBlank { null }

        val activeWarnings = mutableListOf<MunicipalityWarningItem>()
        val clearedWarnings = mutableListOf<MunicipalityWarningItem>()
        var foundMunicipality = false

        // 1. areaTypes[1] (市区町村単位) を最優先探索
        val areaTypes = warningJson.optJSONArray("areaTypes")
        if (areaTypes != null) {
            for (i in 0 until areaTypes.length()) {
                val at = areaTypes.getJSONObject(i)
                val areas = at.optJSONArray("areas") ?: continue
                for (j in 0 until areas.length()) {
                    val area = areas.getJSONObject(j)
                    val areaCode = area.optString("code")
                    if (areaCode == municipality.code ||
                        (municipality.jisCode.isNotBlank() && areaCode.startsWith(municipality.jisCode)) ||
                        (municipality.code.length >= 5 && areaCode.startsWith(municipality.code.take(5)))
                    ) {
                        foundMunicipality = true
                        extractWarningsInto(area, activeWarnings, clearedWarnings)
                    }
                }
            }
        }

        // 2. 自治体単位で見つからない場合のみ二次細分区域（areaTypes[0]）を安全フォールバック
        if (!foundMunicipality && areaTypes != null && areaTypes.length() > 0) {
            val at0 = areaTypes.getJSONObject(0)
            val areas0 = at0.optJSONArray("areas")
            if (areas0 != null) {
                for (j in 0 until areas0.length()) {
                    val area = areas0.getJSONObject(j)
                    extractWarningsInto(area, activeWarnings, clearedWarnings)
                }
            }
        }

        // 重複を除外した有効な警報一覧および解除一覧
        val distinctActiveWarnings = activeWarnings
            .filter { it.status != "解除" && it.name.isNotBlank() }
            .distinctBy { it.code }

        val distinctClearedWarnings = clearedWarnings
            .filter { it.name.isNotBlank() }
            .distinctBy { it.code }

        val warningCodes = distinctActiveWarnings.map { it.code }.toSet()
        val muniText = distinctActiveWarnings.joinToString { it.name }

        // ==========================================
        // 4大キキクル（大雨・土砂・浸水・洪水）個別警戒レベル判定
        // ==========================================
        val landslideTs = extractTimeSeriesMaxRiskLevel(warningJson, municipality.code, listOf("土砂"))
        val inundationTs = extractTimeSeriesMaxRiskLevel(warningJson, municipality.code, listOf("浸水"))
        val floodTs = extractTimeSeriesMaxRiskLevel(warningJson, municipality.code, listOf("洪水"))
        val rainTs = extractTimeSeriesMaxRiskLevel(warningJson, municipality.code, listOf("雨", "大雨"))

        val hasAnySpecial = distinctActiveWarnings.any { it.isSpecial }
        val hasSpecialRain = warningCodes.contains("33") || hasAnySpecial

        // 1. 土砂キキクル判定
        val hasLandslideAlert = (headlineText != null && headlineText.contains("土砂災害警戒情報") && headlineText.contains(municipality.name))
        val hasLandslideWarning = (warningCodes.contains("03") && !muniText.contains("浸水害のみ")) ||
                (headlineText != null && headlineText.contains("土砂災害") && headlineText.contains(municipality.name))
        val hasRainAdvisory = warningCodes.contains("10")
        val landslideLevelNum = maxOf(
            landslideTs.level,
            if (hasSpecialRain) 5 else if (hasLandslideAlert) 4 else if (hasLandslideWarning) 3 else if (hasRainAdvisory) 2 else 0
        )
        val landslideLevel = AlertLevel.fromLevel(landslideLevelNum)

        // 2. 浸水キキクル判定
        val hasInundationWarning = (warningCodes.contains("03") && muniText.contains("浸水")) || muniText.contains("浸水")
        val inundationLevelNum = maxOf(
            inundationTs.level,
            if (hasSpecialRain) 5 else if (hasInundationWarning) 3 else if (hasRainAdvisory) 2 else 0
        )
        val inundationLevel = AlertLevel.fromLevel(inundationLevelNum)

        // 3. 洪水キキクル判定
        val hasFloodSpecial = headlineText?.contains("氾濫発生情報") == true
        val hasFloodLevel4 = headlineText?.contains("氾濫危険情報") == true
        val hasFloodWarning = warningCodes.contains("04") || headlineText?.contains("氾濫警戒情報") == true
        val hasFloodAdvisory = warningCodes.contains("18") || headlineText?.contains("氾濫注意情報") == true
        val floodLevelNum = maxOf(
            floodTs.level,
            if (hasFloodSpecial) 5 else if (hasFloodLevel4) 4 else if (hasFloodWarning) 3 else if (hasFloodAdvisory) 2 else 0
        )
        val floodLevel = AlertLevel.fromLevel(floodLevelNum)

        // 4. 大雨キキクル判定
        val heavyRainLevelNum = maxOf(
            rainTs.level,
            landslideLevel.level,
            inundationLevel.level,
            floodLevel.level,
            if (hasSpecialRain) 5 else if (warningCodes.contains("03")) 3 else if (warningCodes.contains("10")) 2 else 0
        )
        val heavyRainLevel = AlertLevel.fromLevel(heavyRainLevelNum)

        // 総合警戒レベル
        val overallLevelNum = maxOf(heavyRainLevel.level, landslideLevel.level, inundationLevel.level, floodLevel.level)
        val overallLevel = AlertLevel.fromLevel(overallLevelNum)

        val flagStormWind = warningCodes.contains("05") ||
                warningCodes.contains("02") ||
                warningCodes.contains("15") ||
                warningCodes.contains("13") ||
                muniText.contains("暴風") ||
                muniText.contains("強風")

        val disasterFlags = DisasterTargetFlags(
            heavyRain = heavyRainLevel.level >= 2,
            landslide = landslideLevel.level >= 2,
            inundation = inundationLevel.level >= 2,
            flood = floodLevel.level >= 2,
            stormWind = flagStormWind
        )

        val disasterDetails = listOf(
            KikikuruDisasterDetail(
                disasterType = DisasterType.HEAVY_RAIN,
                alertLevel = heavyRainLevel,
                activeWarnings = distinctActiveWarnings.filter { it.code in setOf("33", "03", "10") },
                headlineText = headlineText
            ),
            KikikuruDisasterDetail(
                disasterType = DisasterType.LANDSLIDE,
                alertLevel = landslideLevel,
                activeWarnings = distinctActiveWarnings.filter { it.code in setOf("33", "03", "10") || it.name.contains("土砂") },
                headlineText = headlineText
            ),
            KikikuruDisasterDetail(
                disasterType = DisasterType.INUNDATION,
                alertLevel = inundationLevel,
                activeWarnings = distinctActiveWarnings.filter { it.code in setOf("33", "03", "10") || it.name.contains("浸水") },
                headlineText = headlineText
            ),
            KikikuruDisasterDetail(
                disasterType = DisasterType.FLOOD,
                alertLevel = floodLevel,
                activeWarnings = distinctActiveWarnings.filter { it.code in setOf("04", "18") || it.name.contains("洪水") },
                headlineText = headlineText
            )
        )

        val primaryType = disasterDetails.maxByOrNull { it.alertLevel.level }?.disasterType ?: DisasterType.HEAVY_RAIN

        return MunicipalityAlertInfo(
            municipalityCode = municipality.code,
            municipalityName = municipality.name,
            prefectureCode = municipality.officeCode,
            prefectureName = municipality.prefName,
            alertLevel = overallLevel,
            disasterFlags = disasterFlags,
            activeWarnings = distinctActiveWarnings,
            clearedWarnings = distinctClearedWarnings,
            headlineText = headlineText,
            publishingOffice = publishingOffice,
            reportDatetime = reportDatetime,
            targetDisasterPrimary = primaryType,
            disasterDetails = disasterDetails
        )
    }

    private fun extractWarningsInto(
        areaObj: JSONObject,
        activeDestination: MutableList<MunicipalityWarningItem>,
        clearedDestination: MutableList<MunicipalityWarningItem>
    ) {
        val warningsArr = areaObj.optJSONArray("warnings") ?: return
        for (k in 0 until warningsArr.length()) {
            val w = warningsArr.getJSONObject(k)
            val code = w.optString("code")
            val status = w.optString("status", "発表")
            if (code.isBlank() || code == "00" || status == "発表警報・注意報はなし") continue

            val mapping = WARNING_CODE_MAP[code]
            val name = mapping?.first ?: "気象情報 ($code)"
            val isWarning = mapping?.second ?: false
            val isSpecial = mapping?.third ?: false

            val item = MunicipalityWarningItem(
                code = code,
                name = name,
                status = status,
                isWarning = isWarning,
                isSpecial = isSpecial
            )

            if (status == "解除") {
                clearedDestination.add(item)
            } else {
                activeDestination.add(item)
            }
        }
    }

    /**
     * timeSeries 内の指定災害危険度コード ("40" = 災害切迫 Lv5, "30" = 危険 Lv4, "20" = 警戒 Lv3, "10" = 注意 Lv2) を抽出
     */
    fun extractTimeSeriesMaxRiskLevel(
        warningJson: JSONObject?,
        municipalityCode: String,
        riskTypeKeywords: List<String>
    ): AlertLevel {
        if (warningJson == null) return AlertLevel.LEVEL_0
        val timeSeries = warningJson.optJSONArray("timeSeries") ?: return AlertLevel.LEVEL_0
        var maxLvl = 0
        for (i in 0 until timeSeries.length()) {
            val ts = timeSeries.getJSONObject(i)
            val areaTypes = ts.optJSONArray("areaTypes") ?: continue
            for (j in 0 until areaTypes.length()) {
                val at = areaTypes.getJSONObject(j)
                val areas = at.optJSONArray("areas") ?: continue
                for (k in 0 until areas.length()) {
                    val a = areas.getJSONObject(k)
                    val code = a.optString("code")
                    if (code == municipalityCode || (municipalityCode.length >= 5 && code.startsWith(municipalityCode.take(5)))) {
                        val warnings = a.optJSONArray("warnings") ?: continue
                        for (wIdx in 0 until warnings.length()) {
                            val w = warnings.getJSONObject(wIdx)
                            val levels = w.optJSONArray("levels") ?: continue
                            for (lIdx in 0 until levels.length()) {
                                val lvl = levels.getJSONObject(lIdx)
                                val type = lvl.optString("type")
                                if (riskTypeKeywords.any { type.contains(it) }) {
                                    val localAreas = lvl.optJSONArray("localAreas") ?: continue
                                    for (laIdx in 0 until localAreas.length()) {
                                        val la = localAreas.getJSONObject(laIdx)
                                        val vals = la.optJSONArray("values") ?: continue
                                        for (vIdx in 0 until vals.length()) {
                                            when (vals.optString(vIdx)) {
                                                "40" -> maxLvl = maxOf(maxLvl, 5)
                                                "30" -> maxLvl = maxOf(maxLvl, 4)
                                                "20" -> maxLvl = maxOf(maxLvl, 3)
                                                "10" -> maxLvl = maxOf(maxLvl, 2)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return AlertLevel.fromLevel(maxLvl)
    }

    private fun fetchOfficeWarningJson(officeCode: String): JSONObject? {
        val url = "$BASE_URL/$officeCode.json"
        val raw = httpGetString(url) ?: return null
        return JSONObject(raw)
    }

    private fun httpGetString(urlString: String): String? {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "application/json, text/plain, */*")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000

        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.w(TAG, "HTTP error $code from $urlString")
                return null
            }
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to GET $urlString: ${e.message}")
            null
        } finally {
            conn.disconnect()
        }
    }
}
