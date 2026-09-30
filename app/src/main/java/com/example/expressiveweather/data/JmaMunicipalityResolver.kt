package com.example.expressiveweather.data

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
    private var initialized = false
    private val codeMap = HashMap<String, JmaMunicipality>(2048)
    private val nameMap = HashMap<String, MutableList<JmaMunicipality>>(2048)
    private val allList = ArrayList<JmaMunicipality>(2048)

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        try {
            val assetManager = context.applicationContext.assets
            val inputStream = assetManager.open("jma_municipalities.json")
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val jsonString = reader.use { it.readText() }
            val array = JSONArray(jsonString)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val code = obj.getString("code")
                val name = obj.getString("name")
                val kana = obj.optString("kana", "")
                val officeCode = obj.getString("officeCode")
                val officeName = obj.optString("officeName", "")
                val prefName = obj.optString("prefName", "")
                // JIS 6桁コードは JMA 7桁コードの先頭5桁 + チェックディジット（または先頭6桁）
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
                    codeMap[jisCode] = item
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

    /**
     * コード（7桁または6桁）から直接検索
     */
    fun findByCode(code: String): JmaMunicipality? {
        return codeMap[code]
    }

    /**
     * 地名（市・区・町・村）および都道府県名から自治体を解決
     */
    fun resolve(cityName: String, adminArea: String = "", officeCode: String = ""): JmaMunicipality? {
        val cleanName = cityName.trim()
        if (cleanName.isBlank()) return null

        // 1. 完全一致
        val exactMatches = nameMap[cleanName]
        if (exactMatches != null) {
            if (exactMatches.size == 1) return exactMatches[0]
            // 同名自治体（例: 府中市（東京/広島）、伊達市（北海道/福島））の場合は都道府県やオフィスで絞り込み
            val prefFiltered = exactMatches.firstOrNull {
                (adminArea.isNotBlank() && it.prefName.contains(adminArea)) ||
                (officeCode.isNotBlank() && it.officeCode == officeCode)
            }
            if (prefFiltered != null) return prefFiltered
            return exactMatches[0]
        }

        // 2. 部分一致（地名文字列の中に含まれるか、地名が自治体名を含むか）
        // 例: "東京都 渋谷区 道玄坂" -> "渋谷区"
        var bestMatch: JmaMunicipality? = null
        var maxLen = 0

        for (item in allList) {
            if (cleanName.contains(item.name) || item.name.contains(cleanName)) {
                val matchesPref = (adminArea.isBlank() || item.prefName.contains(adminArea) || cleanName.contains(item.prefName)) &&
                        (officeCode.isBlank() || item.officeCode == officeCode)
                if (matchesPref) {
                    if (item.name.length > maxLen) {
                        maxLen = item.name.length
                        bestMatch = item
                    }
                } else if (bestMatch == null) {
                    bestMatch = item
                }
            }
        }

        return bestMatch
    }

    /**
     * 座標と地名情報から最適な JmaMunicipality を解決（未マッチ時は最寄りの都道府県代表自治体へフォールバック）
     */
    fun resolveFromLocation(
        latitude: Double,
        longitude: Double,
        cityName: String,
        adminArea: String = ""
    ): JmaMunicipality {
        val areaInfo = JmaAreaResolver.resolve(latitude, longitude, cityName, adminArea)
        val matched = resolve(cityName, adminArea, areaInfo.officeCode)
        if (matched != null) return matched

        // オフィスコードから代表自治体をフォールバック
        val officeMatches = allList.filter { it.officeCode == areaInfo.officeCode }
        if (officeMatches.isNotEmpty()) {
            return officeMatches.firstOrNull { it.name.endsWith("市") || it.name.endsWith("区") } ?: officeMatches[0]
        }

        // デフォルト東京（千代田区）
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
