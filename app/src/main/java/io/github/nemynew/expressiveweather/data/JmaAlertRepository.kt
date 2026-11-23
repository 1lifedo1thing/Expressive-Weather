package io.github.nemynew.expressiveweather.data

import android.content.Context
import android.util.Log
import io.github.nemynew.expressiveweather.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * 市区町村単位の警戒レベル・キキクル情報・警報注意報の判定・管理リポジトリ
 */
object JmaAlertRepository {
    private const val TAG = "JmaAlertRepository"
    private const val BASE_URL = "https://www.jma.go.jp/bosai/warning/data/warning"
    private const val USER_AGENT = "ExpressiveWeather/1.0 (Android; +https://github.com/nemy-new/Expressive-Weather)"

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
     * 緯度・経度が指定された場合は JMA キキクル危険度分布タイル（土砂・浸水・洪水）も併せてサンプリングし、
     * 高解像度メッシュ危険度で警戒レベルを補完・引き上げ判定する。
     */
    suspend fun fetchAlertForMunicipality(
        municipality: JmaMunicipality,
        forceFresh: Boolean = false,
        latitude: Double? = null,
        longitude: Double? = null
    ): MunicipalityAlertInfo = withContext(Dispatchers.IO) {
        val cacheKey = if (latitude != null && longitude != null) {
            "${municipality.code}_${(latitude * 100).toInt()}_${(longitude * 100).toInt()}"
        } else {
            municipality.code
        }
        val now = System.currentTimeMillis()
        if (!forceFresh) {
            val cached = alertCache[cacheKey]
            if (cached != null && (now - cached.first) < 120_000L) {
                return@withContext cached.second
            }
        }

        try {
            val warningDeferred = async { fetchOfficeWarningJson(municipality.officeCode) }
            val tileRiskDeferred = async {
                if (latitude != null && longitude != null) {
                    JmaPointTileSampler.sampleKikikuruRisk(latitude, longitude)
                } else {
                    null
                }
            }
            val warningJson = warningDeferred.await()
            val tileRisk = tileRiskDeferred.await()
            val baseAlert = evaluateAlert(warningJson, municipality)
            val alertInfo = mergeWithKikikuruTileRisk(baseAlert, tileRisk)
            alertCache[cacheKey] = Pair(now, alertInfo)
            alertInfo
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to fetch alerts for ${municipality.name} (${municipality.code}): ${e.message}")
            } else {
                Log.w(TAG, "Failed to fetch alerts: ${e.javaClass.simpleName}")
            }
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

        val reportDatetime = warningJson.optNullableString("reportDatetime")
        val publishingOffice = warningJson.optNullableString("publishingOffice")
        val headlineText = warningJson.optNullableString("headlineText")

        val activeWarnings = mutableListOf<MunicipalityWarningItem>()
        val clearedWarnings = mutableListOf<MunicipalityWarningItem>()
        val activeAttentions = mutableSetOf<String>()
        var foundMunicipality = false

        // 1. 完全一致する市区町村コードを最優先探索
        val areaTypes = warningJson.optJSONArray("areaTypes")
        if (areaTypes != null) {
            for (i in 0 until areaTypes.length()) {
                val at = areaTypes.optJSONObject(i) ?: continue
                val areas = at.optJSONArray("areas") ?: continue
                for (j in 0 until areas.length()) {
                    val area = areas.optJSONObject(j) ?: continue
                    val areaCode = area.optNonNullString("code")
                    if (areaCode == municipality.code) {
                        foundMunicipality = true
                        extractWarningsInto(area, activeWarnings, clearedWarnings, activeAttentions)
                    }
                }
            }
            // 完全一致がない場合のみ JIS 6桁または5桁+自治体名一致で探索
            if (!foundMunicipality) {
                for (i in 0 until areaTypes.length()) {
                    val at = areaTypes.optJSONObject(i) ?: continue
                    val areas = at.optJSONArray("areas") ?: continue
                    for (j in 0 until areas.length()) {
                        val area = areas.optJSONObject(j) ?: continue
                        val areaCode = area.optNonNullString("code")
                        val areaName = area.optNonNullString("name")
                        if ((municipality.jisCode.isNotBlank() && areaCode == municipality.jisCode) ||
                            (municipality.code.length >= 5 && areaCode.startsWith(municipality.code.take(5)) &&
                                (areaName.isBlank() || areaName.contains(municipality.name) || municipality.name.contains(areaName)))
                        ) {
                            foundMunicipality = true
                            extractWarningsInto(area, activeWarnings, clearedWarnings, activeAttentions)
                        }
                    }
                }
            }
        }

        // 2. 自治体単位で見つからない場合のみ、該当オフィス/都道府県に一致する一次細分区域を安全フォールバック
        if (!foundMunicipality && areaTypes != null && areaTypes.length() > 0) {
            val at0 = areaTypes.optJSONObject(0)
            val areas0 = at0?.optJSONArray("areas")
            if (areas0 != null) {
                for (j in 0 until areas0.length()) {
                    val area = areas0.optJSONObject(j) ?: continue
                    val areaCode = area.optNonNullString("code")
                    val areaName = area.optNonNullString("name")
                    val matchesCode = areaCode.isNotBlank() && areaCode == municipality.officeCode
                    val matchesName = areaName.isNotBlank() && (
                        (municipality.officeName.isNotBlank() && (areaName.contains(municipality.officeName) || municipality.officeName.contains(areaName))) ||
                            (municipality.prefName.isNotBlank() && (areaName.contains(municipality.prefName) || municipality.prefName.contains(areaName)))
                        )
                    if (areas0.length() == 1 || matchesCode || matchesName) {
                        extractWarningsInto(area, activeWarnings, clearedWarnings, activeAttentions)
                    }
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

        // ==========================================
        // 4大キキクル（大雨・土砂・浸水・洪水）個別警戒レベル判定
        // ==========================================
        val landslideTs = extractTimeSeriesMaxRiskLevel(warningJson, municipality.code, listOf("土砂"))
        val inundationTs = extractTimeSeriesMaxRiskLevel(warningJson, municipality.code, listOf("浸水"))
        val floodTs = extractTimeSeriesMaxRiskLevel(warningJson, municipality.code, listOf("洪水"))
        val rainTs = extractTimeSeriesMaxRiskLevel(warningJson, municipality.code, listOf("雨", "大雨"))

        return evaluateParsedWarnings(
            municipality = municipality,
            distinctActiveWarnings = distinctActiveWarnings,
            distinctClearedWarnings = distinctClearedWarnings,
            activeAttentions = activeAttentions,
            headlineText = headlineText,
            publishingOffice = publishingOffice,
            reportDatetime = reportDatetime,
            landslideTs = landslideTs,
            inundationTs = inundationTs,
            floodTs = floodTs,
            rainTs = rainTs
        )
    }

    internal fun evaluateParsedWarnings(
        municipality: JmaMunicipality,
        distinctActiveWarnings: List<MunicipalityWarningItem>,
        distinctClearedWarnings: List<MunicipalityWarningItem> = emptyList(),
        activeAttentions: Set<String> = emptySet(),
        headlineText: String? = null,
        publishingOffice: String? = null,
        reportDatetime: String? = null,
        landslideTs: AlertLevel = AlertLevel.LEVEL_0,
        inundationTs: AlertLevel = AlertLevel.LEVEL_0,
        floodTs: AlertLevel = AlertLevel.LEVEL_0,
        rainTs: AlertLevel = AlertLevel.LEVEL_0
    ): MunicipalityAlertInfo {
        val warningCodes = distinctActiveWarnings.map { it.code }.toSet()
        val muniText = distinctActiveWarnings.joinToString { it.name }
        val attentionText = activeAttentions.joinToString(" ")

        val hasAnySpecial = distinctActiveWarnings.any { it.isSpecial }
        val hasAnyWarning = distinctActiveWarnings.any { it.isWarning }
        val hasSpecialRain = warningCodes.contains("33")

        val hasInundationAttention = attentionText.contains("浸水")
        val hasLandslideAttention = attentionText.contains("土砂")
        val isInundationOnly = hasInundationAttention && !hasLandslideAttention

        // 1. 土砂キキクル判定
        val hasLandslideAlert = (headlineText != null && headlineText.contains("土砂災害警戒情報") && headlineText.contains(municipality.name))
        val hasLandslideWarning = (warningCodes.contains("03") && !isInundationOnly && !muniText.contains("浸水害のみ")) ||
                hasLandslideAttention ||
                (headlineText != null && headlineText.contains("土砂災害") && headlineText.contains(municipality.name))
        val hasRainAdvisory = warningCodes.contains("10")
        val landslideLevelNum = maxOf(
            landslideTs.level,
            if (hasSpecialRain) 5 else if (hasLandslideAlert) 4 else if (hasLandslideWarning) 3 else if (hasRainAdvisory) 2 else 0
        )
        val landslideLevel = AlertLevel.fromLevel(landslideLevelNum)

        // 2. 浸水キキクル判定（attentions/condition の「浸水害」も判定）
        val hasInundationWarning = hasInundationAttention ||
                (warningCodes.contains("03") && (muniText.contains("浸水") || (headlineText != null && headlineText.contains("浸水") && headlineText.contains(municipality.name)))) ||
                muniText.contains("浸水")
        val inundationLevelNum = maxOf(
            inundationTs.level,
            if (hasSpecialRain) 5 else if (hasInundationWarning) 3 else if (hasRainAdvisory) 2 else 0
        )
        val inundationLevel = AlertLevel.fromLevel(inundationLevelNum)

        // 3. 洪水キキクル判定 (都道府県headlineへの反応時は当該自治体名の記載を確認)
        val headlineMentionsMuni = headlineText != null && headlineText.contains(municipality.name)
        val hasFloodSpecial = headlineMentionsMuni && headlineText.contains("氾濫発生情報")
        val hasFloodLevel4 = headlineMentionsMuni && headlineText.contains("氾濫危険情報")
        val hasFloodWarning = warningCodes.contains("04") || (headlineMentionsMuni && headlineText.contains("氾濫警戒情報"))
        val hasFloodAdvisory = warningCodes.contains("18") || (headlineMentionsMuni && headlineText.contains("氾濫注意情報"))
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

        // 5. 暴風・高潮・大雪・波浪など非大雨系警報の警戒レベル判定
        val nonRainSpecialCodes = setOf("32", "35", "36", "37", "38")
        val nonRainWarningCodes = setOf("02", "05", "06", "07", "08")
        val stormAdvisoryCodes = setOf("13", "15")
        val hasNonRainSpecial = distinctActiveWarnings.any { it.isSpecial && it.code in nonRainSpecialCodes }
        val hasNonRainWarning = distinctActiveWarnings.any { it.isWarning && it.code in nonRainWarningCodes }
        val hasStormAdvisory = distinctActiveWarnings.any { it.code in stormAdvisoryCodes }
        val stormWindLevelNum = when {
            hasNonRainSpecial -> 5
            hasNonRainWarning -> 3
            hasStormAdvisory -> 2
            else -> 0
        }
        val stormWindLevel = AlertLevel.fromLevel(stormWindLevelNum)

        // 総合警戒レベル (暴風・大雪・波浪・高潮等の警報/特別警報も含める)
        val overallLevelNum = maxOf(
            heavyRainLevel.level,
            landslideLevel.level,
            inundationLevel.level,
            floodLevel.level,
            stormWindLevel.level,
            if (hasAnySpecial) 5 else if (hasAnyWarning) 3 else 0
        )
        val overallLevel = AlertLevel.fromLevel(overallLevelNum)

        val flagStormWind = stormWindLevel.level >= 2 ||
                warningCodes.contains("05") ||
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

        val nonRainWarnings = distinctActiveWarnings.filter {
            it.code in nonRainSpecialCodes || it.code in nonRainWarningCodes || it.code in stormAdvisoryCodes
        }

        val disasterDetails = buildList {
            add(
                KikikuruDisasterDetail(
                    disasterType = DisasterType.HEAVY_RAIN,
                    alertLevel = heavyRainLevel,
                    activeWarnings = distinctActiveWarnings.filter { it.code in setOf("33", "03", "10") },
                    headlineText = headlineText
                )
            )
            add(
                KikikuruDisasterDetail(
                    disasterType = DisasterType.LANDSLIDE,
                    alertLevel = landslideLevel,
                    activeWarnings = distinctActiveWarnings.filter { it.code in setOf("33", "03", "10") || it.name.contains("土砂") },
                    headlineText = headlineText
                )
            )
            add(
                KikikuruDisasterDetail(
                    disasterType = DisasterType.INUNDATION,
                    alertLevel = inundationLevel,
                    activeWarnings = distinctActiveWarnings.filter { it.code in setOf("33", "03", "10") || it.name.contains("浸水") },
                    headlineText = headlineText
                )
            )
            add(
                KikikuruDisasterDetail(
                    disasterType = DisasterType.FLOOD,
                    alertLevel = floodLevel,
                    activeWarnings = distinctActiveWarnings.filter { it.code in setOf("04", "18") || it.name.contains("洪水") },
                    headlineText = headlineText
                )
            )
            if (stormWindLevel.level >= 2 || nonRainWarnings.isNotEmpty()) {
                add(
                    KikikuruDisasterDetail(
                        disasterType = DisasterType.STORM_WIND,
                        alertLevel = stormWindLevel,
                        activeWarnings = nonRainWarnings,
                        headlineText = headlineText
                    )
                )
            }
        }

        val primaryType = disasterDetails
            .filter { it.alertLevel.level > 0 }
            .maxByOrNull { it.alertLevel.level }?.disasterType
            ?: DisasterType.HEAVY_RAIN

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

    /**
     * 警報JSON評価結果に JMA キキクル危険度分布（タイルPNGサンプリング）結果をマージする。
     * いずれかの災害種別でメッシュ危険度が既存の警報・注意報レベルを上回った場合、警戒レベルを引き上げる。
     */
    fun mergeWithKikikuruTileRisk(
        base: MunicipalityAlertInfo,
        risk: JmaPointKikikuruRisk?
    ): MunicipalityAlertInfo {
        if (risk == null) return base

        val baseHeavyRain = base.disasterDetails.firstOrNull { it.disasterType == DisasterType.HEAVY_RAIN }?.alertLevel?.level ?: 0
        val baseLandslide = base.disasterDetails.firstOrNull { it.disasterType == DisasterType.LANDSLIDE }?.alertLevel?.level ?: 0
        val baseInundation = base.disasterDetails.firstOrNull { it.disasterType == DisasterType.INUNDATION }?.alertLevel?.level ?: 0
        val baseFlood = base.disasterDetails.firstOrNull { it.disasterType == DisasterType.FLOOD }?.alertLevel?.level ?: 0

        val upgradedFromTile =
            risk.landslideLevel > baseLandslide ||
                risk.inundationLevel > baseInundation ||
                risk.floodLevel > baseFlood

        if (!upgradedFromTile) {
            return base
        }

        val newLandslide = AlertLevel.fromLevel(maxOf(baseLandslide, risk.landslideLevel))
        val newInundation = AlertLevel.fromLevel(maxOf(baseInundation, risk.inundationLevel))
        val newFlood = AlertLevel.fromLevel(maxOf(baseFlood, risk.floodLevel))
        val newHeavyRain = AlertLevel.fromLevel(maxOf(baseHeavyRain, newLandslide.level, newInundation.level, newFlood.level))
        val newOverall = AlertLevel.fromLevel(maxOf(base.alertLevel.level, newHeavyRain.level))

        val updatedDetails = listOf(
            KikikuruDisasterDetail(
                disasterType = DisasterType.HEAVY_RAIN,
                alertLevel = newHeavyRain,
                activeWarnings = base.disasterDetails.firstOrNull { it.disasterType == DisasterType.HEAVY_RAIN }?.activeWarnings
                    ?: base.activeWarnings.filter { it.code in setOf("33", "03", "10") },
                headlineText = base.headlineText
            ),
            KikikuruDisasterDetail(
                disasterType = DisasterType.LANDSLIDE,
                alertLevel = newLandslide,
                activeWarnings = base.disasterDetails.firstOrNull { it.disasterType == DisasterType.LANDSLIDE }?.activeWarnings
                    ?: base.activeWarnings.filter { it.code in setOf("33", "03", "10") || it.name.contains("土砂") },
                headlineText = base.headlineText
            ),
            KikikuruDisasterDetail(
                disasterType = DisasterType.INUNDATION,
                alertLevel = newInundation,
                activeWarnings = base.disasterDetails.firstOrNull { it.disasterType == DisasterType.INUNDATION }?.activeWarnings
                    ?: base.activeWarnings.filter { it.code in setOf("33", "03", "10") || it.name.contains("浸水") },
                headlineText = base.headlineText
            ),
            KikikuruDisasterDetail(
                disasterType = DisasterType.FLOOD,
                alertLevel = newFlood,
                activeWarnings = base.disasterDetails.firstOrNull { it.disasterType == DisasterType.FLOOD }?.activeWarnings
                    ?: base.activeWarnings.filter { it.code in setOf("04", "18") || it.name.contains("洪水") },
                headlineText = base.headlineText
            )
        )

        val newFlags = base.disasterFlags.copy(
            heavyRain = newHeavyRain.level >= 2,
            landslide = newLandslide.level >= 2,
            inundation = newInundation.level >= 2,
            flood = newFlood.level >= 2
        )
        val newPrimary = listOf(
            DisasterType.LANDSLIDE to newLandslide.level,
            DisasterType.INUNDATION to newInundation.level,
            DisasterType.FLOOD to newFlood.level,
            DisasterType.HEAVY_RAIN to newHeavyRain.level
        ).maxByOrNull { it.second }?.first ?: base.targetDisasterPrimary
        val updatedHeadline = if (newOverall.level >= 2) {
            val suffix = "現在地周辺のキキクル危険度分布（警戒レベル${newOverall.level}相当）を検知"
            if (base.headlineText.isNullOrBlank()) suffix else "${base.headlineText}\n$suffix"
        } else {
            base.headlineText
        }

        return base.copy(
            alertLevel = newOverall,
            disasterFlags = newFlags,
            headlineText = updatedHeadline,
            targetDisasterPrimary = newPrimary,
            disasterDetails = updatedDetails
        )
    }

    private fun extractWarningsInto(
        areaObj: JSONObject,
        activeDestination: MutableList<MunicipalityWarningItem>,
        clearedDestination: MutableList<MunicipalityWarningItem>,
        activeAttentions: MutableSet<String> = mutableSetOf()
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
                val attentions = w.optJSONArray("attentions")
                if (attentions != null) {
                    for (aIdx in 0 until attentions.length()) {
                        val att = attentions.optString(aIdx).trim()
                        if (att.isNotBlank()) activeAttentions.add(att)
                    }
                }
                val cond = w.optNonNullString("condition").trim()
                if (cond.isNotBlank()) activeAttentions.add(cond)
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

    private suspend fun fetchOfficeWarningJson(officeCode: String): JSONObject? {
        val url = "$BASE_URL/$officeCode.json"
        val raw = AppHttpClient.getTextOrNull(
            url = url,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Accept" to "application/json, text/plain, */*"
            ),
            timeoutMs = 8_000L
        ) ?: return null
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            null
        }
    }
}
