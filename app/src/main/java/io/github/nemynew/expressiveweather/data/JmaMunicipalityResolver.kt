package io.github.nemynew.expressiveweather.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

data class JmaMunicipality(
    val code: String, // 7桁 JMA コード (例: "1310100")
    val jisCode: String, // 6桁 JIS 地方公共団体コード (例: "131016")
    val name: String, // 市区町村名 (例: "千代田区", "札幌市")
    val kana: String, // かな (例: "ちよだく")
    val officeCode: String, // 気象庁オフィスコード (例: "130000")
    val officeName: String, // オフィス名 (例: "東京都")
    val prefName: String // 都道府県名 (例: "東京都")
)

/**
 * 日本全国1,805市区町村の全国地方公共団体コード・気象庁コード・管区気象台オフィスの高速リゾルバー
 */
object JmaMunicipalityResolver {
    private const val TAG = "JmaMuniResolver"

    @Volatile
    private var initialized = false
    private val lock = Any()
    private val codeMap = HashMap<String, JmaMunicipality>(2048)
    private val jisMap = HashMap<String, MutableList<JmaMunicipality>>(2048)
    private val nameMap = HashMap<String, MutableList<JmaMunicipality>>(2048)
    private val allList = ArrayList<JmaMunicipality>(2048)

    fun init(context: Context) {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            try {
                val assetManager = context.applicationContext.assets
                val inputStream = assetManager.open("jma_municipalities.json")
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val jsonString = reader.use { it.readText() }
                val array = JSONArray(jsonString)

                codeMap.clear()
                jisMap.clear()
                nameMap.clear()
                allList.clear()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val code = obj.getString("code")
                    val name = obj.getString("name")
                    val kana = obj.optNonNullString("kana")
                    val officeCode = obj.getString("officeCode")
                    val officeName = obj.optNonNullString("officeName")
                    val prefName = obj.optNonNullString("prefName")
                    val jisCode = if (code.length >= 6) code.substring(0, 6) else code

                    val item = JmaMunicipality(
                        code = code,
                        jisCode = jisCode,
                        name = name,
                        kana = kana,
                        officeCode = officeCode,
                        officeName = officeName,
                        prefName = prefName
                    )
                    codeMap[code] = item
                    if (jisCode.length == 6) {
                        jisMap.getOrPut(jisCode) { mutableListOf() }.add(item)
                    }
                    nameMap.getOrPut(name) { mutableListOf() }.add(item)
                    allList.add(item)
                }
                initialized = true
                Log.d(TAG, "Initialized ${allList.size} municipalities successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load jma_municipalities.json: ${e.message}", e)
            }
        }
    }

    fun findByCode(code: String): JmaMunicipality? {
        synchronized(lock) {
            return codeMap[code] ?: jisMap[code]?.firstOrNull()
        }
    }

    fun resolve(cityName: String, adminArea: String = "", officeCode: String = ""): JmaMunicipality? {
        val cleanName = cityName.trim()
        if (cleanName.isBlank()) return null

        val sanitizedAdmin = adminArea.trim().let { raw ->
            if (raw == "日本" || raw.equals("Japan", ignoreCase = true) || raw.equals("JP", ignoreCase = true)) {
                ""
            } else {
                raw
            }
        }

        synchronized(lock) {
            val exactMatches = nameMap[cleanName]
            if (exactMatches != null) {
                if (exactMatches.size == 1) return exactMatches[0]
                val prefFiltered = exactMatches.firstOrNull {
                    (sanitizedAdmin.isNotBlank() && (it.prefName.contains(sanitizedAdmin) || sanitizedAdmin.contains(it.prefName))) ||
                        (officeCode.isNotBlank() && it.officeCode == officeCode)
                }
                if (prefFiltered != null) return prefFiltered
                return exactMatches[0]
            }

            var bestMatch: JmaMunicipality? = null
            var maxLen = 0
            var bestMatchedPref = false

            for (item in allList) {
                if (cleanName.contains(item.name) || item.name.contains(cleanName)) {
                    val matchesPref = (sanitizedAdmin.isBlank() || item.prefName.contains(sanitizedAdmin) || sanitizedAdmin.contains(item.prefName) || cleanName.contains(item.prefName)) &&
                        (officeCode.isBlank() || item.officeCode == officeCode)
                    if (matchesPref) {
                        if (!bestMatchedPref || item.name.length > maxLen) {
                            bestMatchedPref = true
                            maxLen = item.name.length
                            bestMatch = item
                        }
                    } else if (!bestMatchedPref && item.name.length > maxLen) {
                        maxLen = item.name.length
                        bestMatch = item
                    }
                }
            }

            return bestMatch
        }
    }

    fun resolveFromLocation(
        latitude: Double,
        longitude: Double,
        cityName: String,
        adminArea: String = ""
    ): JmaMunicipality {
        val areaInfo = JmaAreaResolver.resolve(latitude, longitude, cityName, adminArea)
        val matched = resolve(cityName, adminArea, areaInfo.officeCode)
        if (matched != null) return matched

        synchronized(lock) {
            val officeMatches = allList.filter { it.officeCode == areaInfo.officeCode }
            if (officeMatches.isNotEmpty()) {
                return officeMatches.firstOrNull { it.name.endsWith("市") || it.name.endsWith("区") } ?: officeMatches[0]
            }

            return codeMap["1310100"] ?: JmaMunicipality(
                code = "1310100",
                jisCode = "131016",
                name = "千代田区",
                kana = "ちよだく",
                officeCode = "130000",
                officeName = "東京都",
                prefName = "東京都"
            )
        }
    }
}
