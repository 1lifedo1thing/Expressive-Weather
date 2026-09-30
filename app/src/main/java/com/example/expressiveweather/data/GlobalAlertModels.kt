package com.example.expressiveweather.data

import com.example.expressiveweather.ui.components.WeatherWarningType
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * 警報・防災情報の発信元
 */
@Serializable
enum class AlertSource(val displayName: String, val shortName: String) {
    NWS("米国国立気象局 (NOAA NWS)", "NWS"),
    GDACS("全球災害警報調整システム (国連/欧州委員会 GDACS)", "GDACS"),
    JMA("気象庁 (JMA)", "JMA"),
    WMO("世界気象機関 (WMO)", "WMO")
}

/**
 * 警報の重大度
 */
@Serializable
enum class AlertSeverity {
    EXTREME,
    SEVERE,
    MODERATE,
    MINOR,
    UNKNOWN
}

/**
 * 全球・米国・国内の統一気象防災警報アイテム
 */
@Serializable
data class GlobalAlertItem(
    val id: String,
    val source: AlertSource,
    val type: WeatherWarningType,
    val severity: AlertSeverity,
    val eventName: String,
    val headline: String,
    val description: String,
    val instruction: String? = null,
    val effectiveTime: String? = null,
    val expiresTime: String? = null,
    val areaDesc: String? = null,
    val detailsUrl: String? = null,
    val eventTypeCode: String? = null // 例: "TC", "EQ", "FL", "VO", "WF", "DR" など
) {
    /**
     * 表示言語に応じたタイトルを生成
     */
    fun getLocalizedTitle(language: AppLanguage): String {
        return when (source) {
            AlertSource.NWS -> translateNwsEvent(eventName, language)
            AlertSource.GDACS -> translateGdacsEvent(eventName, eventTypeCode, language)
            AlertSource.WMO -> translateWmoEvent(eventName, language)
            AlertSource.JMA -> eventName
        }
    }

    /**
     * 表示言語に応じた要約・状況説明
     */
    fun getLocalizedDescription(language: AppLanguage): String {
        if (headline.isNotBlank()) {
            return headline
        }
        return description.take(200)
    }

    companion object {
        /**
         * NWS (米国気象局) 警報名の多言語翻訳
         */
        fun translateNwsEvent(event: String, language: AppLanguage): String {
            if (language == AppLanguage.ENGLISH) return event

            val translationsJa = mapOf(
                "Tornado Warning" to "竜巻警報",
                "Tornado Watch" to "竜巻注意報 (Watch)",
                "Severe Thunderstorm Warning" to "激しい雷雨警報",
                "Severe Thunderstorm Watch" to "激しい雷雨注意報 (Watch)",
                "Flash Flood Warning" to "鉄砲水警報",
                "Flash Flood Watch" to "鉄砲水注意報 (Watch)",
                "Flood Warning" to "洪水警報",
                "Flood Watch" to "洪水注意報",
                "Flood Advisory" to "洪水注意報 (Advisory)",
                "Winter Storm Warning" to "冬季暴風雪警報",
                "Winter Storm Watch" to "冬季暴風雪注意報",
                "Winter Weather Advisory" to "冬季気象注意報",
                "Blizzard Warning" to "猛吹雪警報 (Blizzard)",
                "Ice Storm Warning" to "着氷・アイスストーム警報",
                "High Wind Warning" to "強風警報",
                "Wind Advisory" to "強風注意報",
                "Excessive Heat Warning" to "猛暑・極度高温警報",
                "Heat Advisory" to "高温注意報",
                "Excessive Heat Watch" to "猛暑警戒情報",
                "Hurricane Warning" to "ハリケーン警報",
                "Hurricane Watch" to "ハリケーン注意報",
                "Tropical Storm Warning" to "熱帯低気圧・暴風雨警報",
                "Tropical Storm Watch" to "熱帯低気圧注意報",
                "Storm Surge Warning" to "高潮警報",
                "Storm Surge Watch" to "高潮注意報",
                "Red Flag Warning" to "山火事・火災危険警報",
                "Fire Weather Watch" to "火災気象注意報",
                "Coastal Flood Warning" to "沿岸洪水警報",
                "Dense Fog Advisory" to "濃霧注意報",
                "Frost Advisory" to "降霜注意報",
                "Freeze Warning" to "凍結警報",
                "Air Quality Alert" to "大気質注意情報 (AQI低下)"
            )

            val translationsZhHans = mapOf(
                "Tornado Warning" to "龙卷风警报",
                "Tornado Watch" to "龙卷风预警 (Watch)",
                "Severe Thunderstorm Warning" to "强雷暴警报",
                "Severe Thunderstorm Watch" to "强雷暴预警 (Watch)",
                "Flash Flood Warning" to "山洪暴发警报",
                "Flash Flood Watch" to "山洪暴发预警",
                "Flood Warning" to "洪水警报",
                "Flood Watch" to "洪水预警",
                "Flood Advisory" to "洪水提示 (Advisory)",
                "Winter Storm Warning" to "冬季暴风雪警报",
                "Winter Storm Watch" to "冬季暴风雪预警",
                "Winter Weather Advisory" to "冬季天气提示",
                "Blizzard Warning" to "特大暴风雪警报",
                "Ice Storm Warning" to "冻雨冰暴警报",
                "High Wind Warning" to "大风警报",
                "Wind Advisory" to "大风提示",
                "Excessive Heat Warning" to "极度高温警报",
                "Heat Advisory" to "高温提示",
                "Excessive Heat Watch" to "高温预警",
                "Hurricane Warning" to "飓风警报",
                "Hurricane Watch" to "飓风预警",
                "Tropical Storm Warning" to "热带风暴警报",
                "Tropical Storm Watch" to "热带风暴预警",
                "Storm Surge Warning" to "风暴潮警报",
                "Storm Surge Watch" to "风暴潮预警",
                "Red Flag Warning" to "野火火险警报",
                "Fire Weather Watch" to "火险天气预警",
                "Coastal Flood Warning" to "沿海洪水警报",
                "Dense Fog Advisory" to "大雾提示",
                "Frost Advisory" to "霜冻提示",
                "Freeze Warning" to "冰冻警报",
                "Air Quality Alert" to "空气质量预警"
            )

            val translationsZhHant = mapOf(
                "Tornado Warning" to "龍捲風警報",
                "Tornado Watch" to "龍捲風預警 (Watch)",
                "Severe Thunderstorm Warning" to "強雷雨警報",
                "Severe Thunderstorm Watch" to "強雷雨預警 (Watch)",
                "Flash Flood Warning" to "暴洪/山洪警報",
                "Flash Flood Watch" to "暴洪預警",
                "Flood Warning" to "洪水警報",
                "Flood Watch" to "洪水預警",
                "Flood Advisory" to "洪水提示 (Advisory)",
                "Winter Storm Warning" to "冬季暴風雪警報",
                "Winter Storm Watch" to "冬季暴風雪預警",
                "Winter Weather Advisory" to "冬季天氣提示",
                "Blizzard Warning" to "特大暴風雪警報",
                "Ice Storm Warning" to "凍雨冰暴警報",
                "High Wind Warning" to "強風警報",
                "Wind Advisory" to "強風提示",
                "Excessive Heat Warning" to "極度高溫警報",
                "Heat Advisory" to "高溫提示",
                "Excessive Heat Watch" to "高溫預警",
                "Hurricane Warning" to "颶風警報",
                "Hurricane Watch" to "颶風預警",
                "Tropical Storm Warning" to "熱帶風暴警報",
                "Tropical Storm Watch" to "熱帶風暴預警",
                "Storm Surge Warning" to "風暴潮警報",
                "Storm Surge Watch" to "風暴潮預警",
                "Red Flag Warning" to "野火火險警報",
                "Fire Weather Watch" to "火險天氣預警",
                "Coastal Flood Warning" to "沿海洪水警報",
                "Dense Fog Advisory" to "濃霧提示",
                "Frost Advisory" to "霜凍提示",
                "Freeze Warning" to "冰凍警報",
                "Air Quality Alert" to "空氣品質預警"
            )

            val map = when (language) {
                AppLanguage.JAPANESE -> translationsJa
                AppLanguage.SIMPLIFIED_CHINESE -> translationsZhHans
                AppLanguage.TRADITIONAL_CHINESE -> translationsZhHant
                AppLanguage.ENGLISH -> emptyMap()
            }

            return map[event] ?: run {
                // 部分一致マッチング
                when {
                    event.contains("Tornado", ignoreCase = true) -> if (language == AppLanguage.JAPANESE) "竜巻警報" else "龙卷风警报"
                    event.contains("Hurricane", ignoreCase = true) -> if (language == AppLanguage.JAPANESE) "ハリケーン警報" else "飓风警报"
                    event.contains("Thunderstorm", ignoreCase = true) -> if (language == AppLanguage.JAPANESE) "激しい雷雨警報" else "强雷暴警报"
                    event.contains("Flash Flood", ignoreCase = true) -> if (language == AppLanguage.JAPANESE) "鉄砲水警報" else "暴洪警报"
                    event.contains("Flood", ignoreCase = true) -> if (language == AppLanguage.JAPANESE) "洪水警報" else "洪水警报"
                    event.contains("Winter", ignoreCase = true) -> if (language == AppLanguage.JAPANESE) "暴風雪警報" else "暴风雪警报"
                    event.contains("Heat", ignoreCase = true) -> if (language == AppLanguage.JAPANESE) "猛暑・高温注意報" else "高温预警"
                    event.contains("Wind", ignoreCase = true) -> if (language == AppLanguage.JAPANESE) "強風注意報" else "大风预警"
                    else -> event
                }
            }
        }

        /**
         * GDACS (全球災害警報) イベントの多言語翻訳
         */
        fun translateGdacsEvent(eventName: String, typeCode: String?, language: AppLanguage): String {
            if (language == AppLanguage.ENGLISH) return eventName

            val typeNameJa = when (typeCode) {
                "TC" -> "熱帯低気圧・サイクロン/台風"
                "EQ" -> "大規模地震"
                "FL" -> "大規模洪水"
                "VO" -> "火山噴火"
                "WF" -> "大規模森林火災"
                "DR" -> "大規模干ばつ"
                else -> "自然災害"
            }

            val typeNameZh = when (typeCode) {
                "TC" -> "热带气旋/台风/飓风"
                "EQ" -> "重大地震"
                "FL" -> "特大洪水"
                "VO" -> "火山喷发"
                "WF" -> "重大森林火灾"
                "DR" -> "特大干旱"
                else -> "自然灾害"
            }

            val typeNameZhHant = when (typeCode) {
                "TC" -> "熱帶氣旋/颱風/颶風"
                "EQ" -> "重大地震"
                "FL" -> "特大洪水"
                "VO" -> "火山噴發"
                "WF" -> "重大森林火災"
                "DR" -> "特大乾旱"
                else -> "自然災害"
            }

            val prefix = when (language) {
                AppLanguage.JAPANESE -> "GDACS全球警報: $typeNameJa"
                AppLanguage.SIMPLIFIED_CHINESE -> "GDACS全球预警: $typeNameZh"
                AppLanguage.TRADITIONAL_CHINESE -> "GDACS全球預警: $typeNameZhHant"
                AppLanguage.ENGLISH -> "GDACS Alert: $eventName"
            }

            return if (eventName.isNotBlank()) "$prefix ($eventName)" else prefix
        }

        /**
         * WMO (世界気象機関) 気象警報名の多言語翻訳
         */
        fun translateWmoEvent(event: String, language: AppLanguage): String {
            if (language == AppLanguage.ENGLISH) return event

            val lower = event.lowercase(Locale.ROOT)
            return when (language) {
                AppLanguage.JAPANESE -> when {
                    lower.contains("cyclone") || lower.contains("typhoon") || lower.contains("hurricane") -> "台風・サイクロン警報"
                    lower.contains("convection") -> "強対流・雷雨警報"
                    lower.contains("thunder") -> "激しい雷雨警報"
                    lower.contains("blizzard") -> "猛吹雪・暴風雪警報"
                    lower.contains("flash flood") -> "鉄砲水警報"
                    lower.contains("flood") -> "洪水警報"
                    lower.contains("rainstorm") || (lower.contains("rain") && lower.contains("heavy")) -> "豪雨警報"
                    lower.contains("rain") -> "大雨警報"
                    lower.contains("gale") -> "暴風警報"
                    lower.contains("wind") -> "強風警報"
                    lower.contains("snow") -> "大雪警報"
                    lower.contains("heat") || lower.contains("high temperature") -> "猛暑・高温警報"
                    lower.contains("cold") || lower.contains("freeze") -> "低温・凍結警報"
                    lower.contains("frost") -> "霜注意報"
                    lower.contains("fog") -> "濃霧警報"
                    lower.contains("avalanche") -> "なだれ警報"
                    lower.contains("fire") -> "火災気象警報"
                    else -> "WMO気象警報: $event"
                }
                AppLanguage.SIMPLIFIED_CHINESE -> when {
                    lower.contains("cyclone") || lower.contains("typhoon") || lower.contains("hurricane") -> "台风/热带气旋预警"
                    lower.contains("convection") -> "强对流天气预警"
                    lower.contains("thunder") -> "强雷暴预警"
                    lower.contains("blizzard") -> "特大暴风雪预警"
                    lower.contains("flash flood") -> "山洪暴发预警"
                    lower.contains("flood") -> "洪水预警"
                    lower.contains("rainstorm") || (lower.contains("rain") && lower.contains("heavy")) -> "特大暴雨预警"
                    lower.contains("rain") -> "暴雨预警"
                    lower.contains("gale") -> "大风烈风预警"
                    lower.contains("wind") -> "大风预警"
                    lower.contains("snow") -> "暴雪预警"
                    lower.contains("heat") || lower.contains("high temperature") -> "极度高温预警"
                    lower.contains("cold") || lower.contains("freeze") -> "严寒冰冻预警"
                    lower.contains("frost") -> "霜冻提示"
                    lower.contains("fog") -> "大雾预警"
                    lower.contains("avalanche") -> "雪崩预警"
                    lower.contains("fire") -> "森林火险预警"
                    else -> "WMO预警: $event"
                }
                AppLanguage.TRADITIONAL_CHINESE -> when {
                    lower.contains("cyclone") || lower.contains("typhoon") || lower.contains("hurricane") -> "颱風/熱帶氣旋警報"
                    lower.contains("convection") -> "強對流天氣預警"
                    lower.contains("thunder") -> "強雷雨警報"
                    lower.contains("blizzard") -> "特大暴風雪警報"
                    lower.contains("flash flood") -> "暴洪/山洪警報"
                    lower.contains("flood") -> "洪水警報"
                    lower.contains("rainstorm") || (lower.contains("rain") && lower.contains("heavy")) -> "特大豪雨警報"
                    lower.contains("rain") -> "豪雨警報"
                    lower.contains("gale") -> "強烈風暴警報"
                    lower.contains("wind") -> "強風警報"
                    lower.contains("snow") -> "暴雪警報"
                    lower.contains("heat") || lower.contains("high temperature") -> "極度高溫警報"
                    lower.contains("cold") || lower.contains("freeze") -> "嚴寒冰凍警報"
                    lower.contains("frost") -> "霜凍提示"
                    lower.contains("fog") -> "濃霧警報"
                    lower.contains("avalanche") -> "雪崩警報"
                    lower.contains("fire") -> "火險警報"
                    else -> "WMO警報: $event"
                }
                AppLanguage.ENGLISH -> event
            }
        }
    }
}
