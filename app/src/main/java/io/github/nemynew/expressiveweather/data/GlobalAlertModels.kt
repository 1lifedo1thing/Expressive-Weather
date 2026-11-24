package io.github.nemynew.expressiveweather.data

import io.github.nemynew.expressiveweather.ui.components.WeatherWarningType
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
    WMO("世界気象機関 (WMO)", "WMO"),
    METEOALARM("欧州気象警報ネットワーク (MeteoAlarm)", "MeteoAlarm")
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
 * 全球・米国・欧州・国内の統一気象防災警報アイテム
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
            AlertSource.METEOALARM -> translateMeteoAlarmEvent(eventName, language)
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

            val translationsKo = mapOf(
                "Tornado Warning" to "토네이도 경보",
                "Tornado Watch" to "토네이도 주의보 (Watch)",
                "Severe Thunderstorm Warning" to "강한 뇌우 경보",
                "Severe Thunderstorm Watch" to "강한 뇌우 주의보 (Watch)",
                "Flash Flood Warning" to "돌발 홍수 경보",
                "Flash Flood Watch" to "돌발 홍수 주의보 (Watch)",
                "Flood Warning" to "홍수 경보",
                "Flood Watch" to "홍수 주의보",
                "Flood Advisory" to "홍수 주의보 (Advisory)",
                "Winter Storm Warning" to "겨울 폭풍 경보",
                "Winter Storm Watch" to "겨울 폭풍 주의보",
                "Winter Weather Advisory" to "겨울 기상 주의보",
                "Blizzard Warning" to "눈보라 경보 (Blizzard)",
                "Ice Storm Warning" to "결빙 폭풍 경보",
                "High Wind Warning" to "강풍 경보",
                "Wind Advisory" to "강풍 주의보",
                "Excessive Heat Warning" to "폭염 경보",
                "Heat Advisory" to "폭염 주의보",
                "Excessive Heat Watch" to "폭염 예비특보",
                "Hurricane Warning" to "허리케인 경보",
                "Hurricane Watch" to "허리케인 주의보",
                "Tropical Storm Warning" to "열대폭풍 경보",
                "Tropical Storm Watch" to "열대폭풍 주의보",
                "Storm Surge Warning" to "폭풍해일 경보",
                "Storm Surge Watch" to "폭풍해일 주의보",
                "Red Flag Warning" to "산불·화재 위험 경보",
                "Fire Weather Watch" to "화재 기상 주의보",
                "Coastal Flood Warning" to "해안 홍수 경보",
                "Dense Fog Advisory" to "짙은 안개 주의보",
                "Frost Advisory" to "서리 주의보",
                "Freeze Warning" to "한파·결빙 경보",
                "Air Quality Alert" to "대기질 주의보 (AQI 악화)"
            )

            val translationsEs = mapOf(
                "Tornado Warning" to "Aviso de tornado",
                "Tornado Watch" to "Vigilancia de tornado",
                "Severe Thunderstorm Warning" to "Aviso de tormenta severa",
                "Severe Thunderstorm Watch" to "Vigilancia de tormenta severa",
                "Flash Flood Warning" to "Aviso de inundación repentina",
                "Flash Flood Watch" to "Vigilancia de inundación repentina",
                "Flood Warning" to "Aviso de inundación",
                "Flood Watch" to "Vigilancia de inundación",
                "Flood Advisory" to "Advertencia de inundación",
                "Winter Storm Warning" to "Aviso de tormenta invernal",
                "Winter Storm Watch" to "Vigilancia de tormenta invernal",
                "Winter Weather Advisory" to "Advertencia de tiempo invernal",
                "Blizzard Warning" to "Aviso de ventisca",
                "Ice Storm Warning" to "Aviso de tormenta de hielo",
                "High Wind Warning" to "Aviso de vientos fuertes",
                "Wind Advisory" to "Advertencia de viento",
                "Excessive Heat Warning" to "Aviso de calor extremo",
                "Heat Advisory" to "Advertencia de calor",
                "Excessive Heat Watch" to "Vigilancia de calor extremo",
                "Hurricane Warning" to "Aviso de huracán",
                "Hurricane Watch" to "Vigilancia de huracán",
                "Tropical Storm Warning" to "Aviso de tormenta tropical",
                "Tropical Storm Watch" to "Vigilancia de tormenta tropical",
                "Storm Surge Warning" to "Aviso de marejada ciclónica",
                "Storm Surge Watch" to "Vigilancia de marejada ciclónica",
                "Red Flag Warning" to "Aviso de bandera roja (riesgo de incendio)",
                "Fire Weather Watch" to "Vigilancia meteorológica de incendios",
                "Coastal Flood Warning" to "Aviso de inundación costera",
                "Dense Fog Advisory" to "Advertencia de niebla densa",
                "Frost Advisory" to "Advertencia de helada",
                "Freeze Warning" to "Aviso de congelación",
                "Air Quality Alert" to "Alerta de calidad del aire"
            )

            val translationsFr = mapOf(
                "Tornado Warning" to "Alerte de tornade",
                "Tornado Watch" to "Veille de tornade",
                "Severe Thunderstorm Warning" to "Alerte d'orage violent",
                "Severe Thunderstorm Watch" to "Veille d'orage violent",
                "Flash Flood Warning" to "Alerte de crue éclair",
                "Flash Flood Watch" to "Veille de crue éclair",
                "Flood Warning" to "Alerte d'inondation",
                "Flood Watch" to "Veille d'inondation",
                "Flood Advisory" to "Avis d'inondation",
                "Winter Storm Warning" to "Alerte de tempête hivernale",
                "Winter Storm Watch" to "Veille de tempête hivernale",
                "Winter Weather Advisory" to "Avis de conditions hivernales",
                "Blizzard Warning" to "Alerte de blizzard",
                "Ice Storm Warning" to "Alerte de tempête de verglas",
                "High Wind Warning" to "Alerte de vents violents",
                "Wind Advisory" to "Avis de vent fort",
                "Excessive Heat Warning" to "Alerte de chaleur extrême",
                "Heat Advisory" to "Avis de chaleur",
                "Excessive Heat Watch" to "Veille de chaleur extrême",
                "Hurricane Warning" to "Alerte d'ouragan",
                "Hurricane Watch" to "Veille d'ouragan",
                "Tropical Storm Warning" to "Alerte de tempête tropicale",
                "Tropical Storm Watch" to "Veille de tempête tropicale",
                "Storm Surge Warning" to "Alerte d'onde de tempête",
                "Storm Surge Watch" to "Veille d'onde de tempête",
                "Red Flag Warning" to "Alerte rouge incendie",
                "Fire Weather Watch" to "Veille météo incendie",
                "Coastal Flood Warning" to "Alerte d'inondation côtière",
                "Dense Fog Advisory" to "Avis de brouillard dense",
                "Frost Advisory" to "Avis de gelée",
                "Freeze Warning" to "Alerte de gel",
                "Air Quality Alert" to "Alerte qualité de l'air"
            )

            val translationsPt = mapOf(
                "Tornado Warning" to "Aviso de tornado",
                "Tornado Watch" to "Vigilância de tornado",
                "Severe Thunderstorm Warning" to "Aviso de tempestade severa",
                "Severe Thunderstorm Watch" to "Vigilância de tempestade severa",
                "Flash Flood Warning" to "Aviso de enchente relâmpago",
                "Flash Flood Watch" to "Vigilância de enchente relâmpago",
                "Flood Warning" to "Aviso de inundação",
                "Flood Watch" to "Vigilância de inundação",
                "Flood Advisory" to "Alerta de inundação",
                "Winter Storm Warning" to "Aviso de tempestade de inverno",
                "Winter Storm Watch" to "Vigilância de tempestade de inverno",
                "Winter Weather Advisory" to "Alerta de tempo invernal",
                "Blizzard Warning" to "Aviso de nevasca intensa",
                "Ice Storm Warning" to "Aviso de tempestade de gelo",
                "High Wind Warning" to "Aviso de ventos fortes",
                "Wind Advisory" to "Alerta de vento",
                "Excessive Heat Warning" to "Aviso de calor extremo",
                "Heat Advisory" to "Alerta de calor",
                "Excessive Heat Watch" to "Vigilância de calor extremo",
                "Hurricane Warning" to "Aviso de furacão",
                "Hurricane Watch" to "Vigilância de furacão",
                "Tropical Storm Warning" to "Aviso de tempestade tropical",
                "Tropical Storm Watch" to "Vigilância de tempestade tropical",
                "Storm Surge Warning" to "Aviso de ressaca ciclônica",
                "Storm Surge Watch" to "Vigilância de ressaca ciclônica",
                "Red Flag Warning" to "Aviso de bandeira vermelha (risco de incêndio)",
                "Fire Weather Watch" to "Vigilância meteorológica de incêndios",
                "Coastal Flood Warning" to "Aviso de inundação costeira",
                "Dense Fog Advisory" to "Alerta de nevoeiro denso",
                "Frost Advisory" to "Alerta de geada",
                "Freeze Warning" to "Aviso de congelamento",
                "Air Quality Alert" to "Alerta de qualidade do ar"
            )

            val map = when (language) {
                AppLanguage.JAPANESE -> translationsJa
                AppLanguage.KOREAN -> translationsKo
                AppLanguage.SIMPLIFIED_CHINESE -> translationsZhHans
                AppLanguage.TRADITIONAL_CHINESE -> translationsZhHant
                AppLanguage.SPANISH -> translationsEs
                AppLanguage.FRENCH -> translationsFr
                AppLanguage.PORTUGUESE -> translationsPt
                AppLanguage.ENGLISH -> emptyMap()
            }

            return map[event] ?: run {
                // 部分一致マッチング
                when {
                    event.contains("Tornado", ignoreCase = true) -> when (language) {
                        AppLanguage.JAPANESE -> "竜巻警報"
                        AppLanguage.KOREAN -> "토네이도 경보"
                        AppLanguage.TRADITIONAL_CHINESE -> "龍捲風警報"
                        AppLanguage.SIMPLIFIED_CHINESE -> "龙卷风警报"
                        AppLanguage.SPANISH -> "Aviso de tornado"
                        AppLanguage.FRENCH -> "Alerte de tornade"
                        AppLanguage.PORTUGUESE -> "Aviso de tornado"
                        AppLanguage.ENGLISH -> event
                    }
                    event.contains("Hurricane", ignoreCase = true) -> when (language) {
                        AppLanguage.JAPANESE -> "ハリケーン警報"
                        AppLanguage.KOREAN -> "허리케인 경보"
                        AppLanguage.TRADITIONAL_CHINESE -> "颶風警報"
                        AppLanguage.SIMPLIFIED_CHINESE -> "飓风警报"
                        AppLanguage.SPANISH -> "Aviso de huracán"
                        AppLanguage.FRENCH -> "Alerte d'ouragan"
                        AppLanguage.PORTUGUESE -> "Aviso de furacão"
                        AppLanguage.ENGLISH -> event
                    }
                    event.contains("Thunderstorm", ignoreCase = true) -> when (language) {
                        AppLanguage.JAPANESE -> "激しい雷雨警報"
                        AppLanguage.KOREAN -> "강한 뇌우 경보"
                        AppLanguage.TRADITIONAL_CHINESE -> "強雷暴警報"
                        AppLanguage.SIMPLIFIED_CHINESE -> "强雷暴警报"
                        AppLanguage.SPANISH -> "Aviso de tormenta severa"
                        AppLanguage.FRENCH -> "Alerte d'orage violent"
                        AppLanguage.PORTUGUESE -> "Aviso de tempestade severa"
                        AppLanguage.ENGLISH -> event
                    }
                    event.contains("Flash Flood", ignoreCase = true) -> when (language) {
                        AppLanguage.JAPANESE -> "鉄砲水警報"
                        AppLanguage.KOREAN -> "돌발 홍수 경보"
                        AppLanguage.TRADITIONAL_CHINESE -> "暴洪警報"
                        AppLanguage.SIMPLIFIED_CHINESE -> "暴洪警报"
                        AppLanguage.SPANISH -> "Aviso de inundación repentina"
                        AppLanguage.FRENCH -> "Alerte de crue éclair"
                        AppLanguage.PORTUGUESE -> "Aviso de enchente relâmpago"
                        AppLanguage.ENGLISH -> event
                    }
                    event.contains("Flood", ignoreCase = true) -> when (language) {
                        AppLanguage.JAPANESE -> "洪水警報"
                        AppLanguage.KOREAN -> "홍수 경보"
                        AppLanguage.TRADITIONAL_CHINESE -> "洪水警報"
                        AppLanguage.SIMPLIFIED_CHINESE -> "洪水警报"
                        AppLanguage.SPANISH -> "Aviso de inundación"
                        AppLanguage.FRENCH -> "Alerte d'inondation"
                        AppLanguage.PORTUGUESE -> "Aviso de inundação"
                        AppLanguage.ENGLISH -> event
                    }
                    event.contains("Winter", ignoreCase = true) -> when (language) {
                        AppLanguage.JAPANESE -> "暴風雪警報"
                        AppLanguage.KOREAN -> "겨울 폭풍 경보"
                        AppLanguage.TRADITIONAL_CHINESE -> "暴風雪警報"
                        AppLanguage.SIMPLIFIED_CHINESE -> "暴风雪警报"
                        AppLanguage.SPANISH -> "Aviso de tormenta invernal"
                        AppLanguage.FRENCH -> "Alerte de tempête hivernale"
                        AppLanguage.PORTUGUESE -> "Aviso de tempestade de inverno"
                        AppLanguage.ENGLISH -> event
                    }
                    event.contains("Heat", ignoreCase = true) -> when (language) {
                        AppLanguage.JAPANESE -> "猛暑・高温注意報"
                        AppLanguage.KOREAN -> "폭염 주의보"
                        AppLanguage.TRADITIONAL_CHINESE -> "高溫預警"
                        AppLanguage.SIMPLIFIED_CHINESE -> "高温预警"
                        AppLanguage.SPANISH -> "Advertencia de calor"
                        AppLanguage.FRENCH -> "Avis de chaleur"
                        AppLanguage.PORTUGUESE -> "Alerta de calor"
                        AppLanguage.ENGLISH -> event
                    }
                    event.contains("Wind", ignoreCase = true) -> when (language) {
                        AppLanguage.JAPANESE -> "強風注意報"
                        AppLanguage.KOREAN -> "강풍 주의보"
                        AppLanguage.TRADITIONAL_CHINESE -> "強風預警"
                        AppLanguage.SIMPLIFIED_CHINESE -> "大风预警"
                        AppLanguage.SPANISH -> "Advertencia de viento"
                        AppLanguage.FRENCH -> "Avis de vent fort"
                        AppLanguage.PORTUGUESE -> "Alerta de vento"
                        AppLanguage.ENGLISH -> event
                    }
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

            val typeNameKo = when (typeCode) {
                "TC" -> "열대저기압·태풍/사이클론"
                "EQ" -> "대규모 지진"
                "FL" -> "대규모 홍수"
                "VO" -> "화산 분화"
                "WF" -> "대규모 산불"
                "DR" -> "대규모 가뭄"
                else -> "자연재해"
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

            val typeNameEs = when (typeCode) {
                "TC" -> "Ciclón tropical / Tifón / Huracán"
                "EQ" -> "Terremoto importante"
                "FL" -> "Inundación grave"
                "VO" -> "Erupción volcánica"
                "WF" -> "Incendio forestal grave"
                "DR" -> "Sequía severa"
                else -> "Desastre natural"
            }

            val typeNameFr = when (typeCode) {
                "TC" -> "Cyclone tropical / Typhon / Ouragan"
                "EQ" -> "Séisme majeur"
                "FL" -> "Inondation majeure"
                "VO" -> "Éruption volcanique"
                "WF" -> "Feu de forêt majeur"
                "DR" -> "Sécheresse sévère"
                else -> "Catastrophe naturelle"
            }

            val typeNamePt = when (typeCode) {
                "TC" -> "Ciclone tropical / Tufão / Furacão"
                "EQ" -> "Terremoto de grande magnitude"
                "FL" -> "Inundação grave"
                "VO" -> "Erupção vulcânica"
                "WF" -> "Incêndio florestal grave"
                "DR" -> "Seca severa"
                else -> "Desastre natural"
            }

            val prefix = when (language) {
                AppLanguage.JAPANESE -> "GDACS全球警報: $typeNameJa"
                AppLanguage.KOREAN -> "GDACS 글로벌 경보: $typeNameKo"
                AppLanguage.SIMPLIFIED_CHINESE -> "GDACS全球预警: $typeNameZh"
                AppLanguage.TRADITIONAL_CHINESE -> "GDACS全球預警: $typeNameZhHant"
                AppLanguage.SPANISH -> "Alerta global GDACS: $typeNameEs"
                AppLanguage.FRENCH -> "Alerte mondiale GDACS : $typeNameFr"
                AppLanguage.PORTUGUESE -> "Alerta global GDACS: $typeNamePt"
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
                AppLanguage.KOREAN -> when {
                    lower.contains("cyclone") || lower.contains("typhoon") || lower.contains("hurricane") -> "태풍·열대저기압 경보"
                    lower.contains("convection") -> "강한 대류·뇌우 경보"
                    lower.contains("thunder") -> "강한 뇌우 경보"
                    lower.contains("blizzard") -> "눈보라·폭설 경보"
                    lower.contains("flash flood") -> "돌발 홍수 경보"
                    lower.contains("flood") -> "홍수 경보"
                    lower.contains("rainstorm") || (lower.contains("rain") && lower.contains("heavy")) -> "집중호우 경보"
                    lower.contains("rain") -> "호우 경보"
                    lower.contains("gale") -> "폭풍 경보"
                    lower.contains("wind") -> "강풍 경보"
                    lower.contains("snow") -> "대설 경보"
                    lower.contains("heat") || lower.contains("high temperature") -> "폭염 경보"
                    lower.contains("cold") || lower.contains("freeze") -> "한파·결빙 경보"
                    lower.contains("frost") -> "서리 주의보"
                    lower.contains("fog") -> "짙은 안개 경보"
                    lower.contains("avalanche") -> "눈사태 경보"
                    lower.contains("fire") -> "산불 기상 경보"
                    else -> "WMO 기상특보: $event"
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
                AppLanguage.SPANISH -> when {
                    lower.contains("cyclone") || lower.contains("typhoon") || lower.contains("hurricane") -> "Alerta de ciclón tropical"
                    lower.contains("convection") -> "Alerta de convección severa"
                    lower.contains("thunder") -> "Alerta de tormenta eléctrica"
                    lower.contains("blizzard") -> "Alerta de ventisca"
                    lower.contains("flash flood") -> "Alerta de inundación repentina"
                    lower.contains("flood") -> "Alerta de inundación"
                    lower.contains("rainstorm") || (lower.contains("rain") && lower.contains("heavy")) -> "Alerta de lluvia torrencial"
                    lower.contains("rain") -> "Alerta de lluvia intensa"
                    lower.contains("gale") -> "Alerta de vendaval"
                    lower.contains("wind") -> "Alerta de viento fuerte"
                    lower.contains("snow") -> "Alerta de nevada intensa"
                    lower.contains("heat") || lower.contains("high temperature") -> "Alerta de calor extremo"
                    lower.contains("cold") || lower.contains("freeze") -> "Alerta de frío extremo y heladas"
                    lower.contains("frost") -> "Aviso de helada"
                    lower.contains("fog") -> "Alerta de niebla densa"
                    lower.contains("avalanche") -> "Alerta de avalancha"
                    lower.contains("fire") -> "Alerta de incendio forestal"
                    else -> "Alerta OMM: $event"
                }
                AppLanguage.FRENCH -> when {
                    lower.contains("cyclone") || lower.contains("typhoon") || lower.contains("hurricane") -> "Alerte cyclone tropical"
                    lower.contains("convection") -> "Alerte convection violente"
                    lower.contains("thunder") -> "Alerte orages violents"
                    lower.contains("blizzard") -> "Alerte blizzard"
                    lower.contains("flash flood") -> "Alerte crue éclair"
                    lower.contains("flood") -> "Alerte inondation"
                    lower.contains("rainstorm") || (lower.contains("rain") && lower.contains("heavy")) -> "Alerte pluies torrentielles"
                    lower.contains("rain") -> "Alerte fortes pluies"
                    lower.contains("gale") -> "Alerte coup de vent"
                    lower.contains("wind") -> "Alerte vent violent"
                    lower.contains("snow") -> "Alerte fortes chutes de neige"
                    lower.contains("heat") || lower.contains("high temperature") -> "Alerte canicule"
                    lower.contains("cold") || lower.contains("freeze") -> "Alerte grand froid"
                    lower.contains("frost") -> "Avis de gelée"
                    lower.contains("fog") -> "Alerte brouillard dense"
                    lower.contains("avalanche") -> "Alerte avalanche"
                    lower.contains("fire") -> "Alerte feu de forêt"
                    else -> "Alerte OMM : $event"
                }
                AppLanguage.PORTUGUESE -> when {
                    lower.contains("cyclone") || lower.contains("typhoon") || lower.contains("hurricane") -> "Alerta de ciclone tropical"
                    lower.contains("convection") -> "Alerta de convecção severa"
                    lower.contains("thunder") -> "Alerta de tempestade elétrica"
                    lower.contains("blizzard") -> "Alerta de nevasca intensa"
                    lower.contains("flash flood") -> "Alerta de enchente relâmpago"
                    lower.contains("flood") -> "Alerta de inundação"
                    lower.contains("rainstorm") || (lower.contains("rain") && lower.contains("heavy")) -> "Alerta de chuva torrencial"
                    lower.contains("rain") -> "Alerta de chuva forte"
                    lower.contains("gale") -> "Alerta de vendaval"
                    lower.contains("wind") -> "Alerta de vento forte"
                    lower.contains("snow") -> "Alerta de nevasca"
                    lower.contains("heat") || lower.contains("high temperature") -> "Alerta de calor extremo"
                    lower.contains("cold") || lower.contains("freeze") -> "Alerta de frio extremo"
                    lower.contains("frost") -> "Aviso de geada"
                    lower.contains("fog") -> "Alerta de nevoeiro denso"
                    lower.contains("avalanche") -> "Alerta de avalanche"
                    lower.contains("fire") -> "Alerta de incêndio florestal"
                    else -> "Alerta OMM: $event"
                }
                AppLanguage.ENGLISH -> event
            }
        }

        /**
         * 欧州気象警報ネットワーク (MeteoAlarm / EUMETNET) 警報名の多言語翻訳
         * 11種の公式危険種別 (Awareness Types) を完全サポート
         */
        fun translateMeteoAlarmEvent(event: String, language: AppLanguage): String {
            if (language == AppLanguage.ENGLISH) return event

            val lower = event.lowercase(Locale.ROOT)
            return when (language) {
                AppLanguage.JAPANESE -> when {
                    lower.contains("coastal") -> "沿岸高波・波浪警報"
                    lower.contains("avalanche") || lower.contains("lawine") -> "雪崩警報"
                    lower.contains("forest") || lower.contains("fire") || lower.contains("waldbrand") || lower.contains("incendie") -> "森林火災警報"
                    lower.contains("thunder") || lower.contains("gewitter") || lower.contains("orage") || lower.contains("tormenta") -> "雷雨・落雷警報"
                    lower.contains("snow") || lower.contains("ice") || lower.contains("schnee") || lower.contains("neige") || lower.contains("nieve") -> "大雪・路面凍結警報"
                    lower.contains("flash flood") -> "鉄砲水警報"
                    lower.contains("flood") || lower.contains("hochwasser") || lower.contains("inondation") -> "洪水警報"
                    lower.contains("rain") || lower.contains("regen") || lower.contains("pluie") || lower.contains("lluvia") -> "大雨・豪雨警報"
                    lower.contains("gale") || lower.contains("storm") -> "暴風警報"
                    lower.contains("wind") || lower.contains("böen") || lower.contains("vent") || lower.contains("viento") -> "強風・暴風警報"
                    lower.contains("high-temperature") || lower.contains("high temp") || lower.contains("heat") || lower.contains("hitze") || lower.contains("canicule") -> "猛暑・極度高温警報"
                    lower.contains("low-temperature") || lower.contains("low temp") || lower.contains("cold") || lower.contains("freeze") || lower.contains("kälte") || lower.contains("froid") -> "低温・極度寒波警報"
                    lower.contains("fog") || lower.contains("nebel") || lower.contains("brouillard") || lower.contains("niebla") -> "濃霧警報"
                    else -> "欧州気象警報: $event"
                }
                AppLanguage.KOREAN -> when {
                    lower.contains("coastal") -> "연안 풍랑·너울 경보"
                    lower.contains("avalanche") || lower.contains("lawine") -> "눈사태 경보"
                    lower.contains("forest") || lower.contains("fire") || lower.contains("waldbrand") || lower.contains("incendie") -> "산불 경보"
                    lower.contains("thunder") || lower.contains("gewitter") || lower.contains("orage") || lower.contains("tormenta") -> "뇌우·낙뢰 경보"
                    lower.contains("snow") || lower.contains("ice") || lower.contains("schnee") || lower.contains("neige") || lower.contains("nieve") -> "대설·도로 결빙 경보"
                    lower.contains("flash flood") -> "돌발 홍수 경보"
                    lower.contains("flood") || lower.contains("hochwasser") || lower.contains("inondation") -> "홍수 경보"
                    lower.contains("rain") || lower.contains("regen") || lower.contains("pluie") || lower.contains("lluvia") -> "호우 경보"
                    lower.contains("gale") || lower.contains("storm") -> "폭풍 경보"
                    lower.contains("wind") || lower.contains("böen") || lower.contains("vent") || lower.contains("viento") -> "강풍 경보"
                    lower.contains("high-temperature") || lower.contains("high temp") || lower.contains("heat") || lower.contains("hitze") || lower.contains("canicule") -> "폭염 경보"
                    lower.contains("low-temperature") || lower.contains("low temp") || lower.contains("cold") || lower.contains("freeze") || lower.contains("kälte") || lower.contains("froid") -> "한파 경보"
                    lower.contains("fog") || lower.contains("nebel") || lower.contains("brouillard") || lower.contains("niebla") -> "짙은 안개 경보"
                    else -> "유럽 기상경보: $event"
                }
                AppLanguage.SIMPLIFIED_CHINESE -> when {
                    lower.contains("coastal") -> "沿海风浪海啸预警"
                    lower.contains("avalanche") || lower.contains("lawine") -> "雪崩预警"
                    lower.contains("forest") || lower.contains("fire") || lower.contains("waldbrand") || lower.contains("incendie") -> "森林火险预警"
                    lower.contains("thunder") || lower.contains("gewitter") || lower.contains("orage") || lower.contains("tormenta") -> "强雷暴对流预警"
                    lower.contains("snow") || lower.contains("ice") || lower.contains("schnee") || lower.contains("neige") || lower.contains("nieve") -> "暴雪道路结冰预警"
                    lower.contains("flash flood") -> "山洪暴发预警"
                    lower.contains("flood") || lower.contains("hochwasser") || lower.contains("inondation") -> "洪水预警"
                    lower.contains("rain") || lower.contains("regen") || lower.contains("pluie") || lower.contains("lluvia") -> "暴雨预警"
                    lower.contains("gale") || lower.contains("storm") -> "烈风风暴预警"
                    lower.contains("wind") || lower.contains("böen") || lower.contains("vent") || lower.contains("viento") -> "大风强风预警"
                    lower.contains("high-temperature") || lower.contains("high temp") || lower.contains("heat") || lower.contains("hitze") || lower.contains("canicule") -> "极度高温热浪预警"
                    lower.contains("low-temperature") || lower.contains("low temp") || lower.contains("cold") || lower.contains("freeze") || lower.contains("kälte") || lower.contains("froid") -> "严寒寒潮预警"
                    lower.contains("fog") || lower.contains("nebel") || lower.contains("brouillard") || lower.contains("niebla") -> "大雾预警"
                    else -> "欧洲气象预警: $event"
                }
                AppLanguage.TRADITIONAL_CHINESE -> when {
                    lower.contains("coastal") -> "沿海風浪海嘯警報"
                    lower.contains("avalanche") || lower.contains("lawine") -> "雪崩警報"
                    lower.contains("forest") || lower.contains("fire") || lower.contains("waldbrand") || lower.contains("incendie") -> "森林火險警報"
                    lower.contains("thunder") || lower.contains("gewitter") || lower.contains("orage") || lower.contains("tormenta") -> "強雷雨對流警報"
                    lower.contains("snow") || lower.contains("ice") || lower.contains("schnee") || lower.contains("neige") || lower.contains("nieve") -> "暴雪道路結冰警報"
                    lower.contains("flash flood") -> "暴洪/山洪警報"
                    lower.contains("flood") || lower.contains("hochwasser") || lower.contains("inondation") -> "洪水警報"
                    lower.contains("rain") || lower.contains("regen") || lower.contains("pluie") || lower.contains("lluvia") -> "暴雨/豪雨警報"
                    lower.contains("gale") || lower.contains("storm") -> "烈風風暴警報"
                    lower.contains("wind") || lower.contains("böen") || lower.contains("vent") || lower.contains("viento") -> "強風大風警報"
                    lower.contains("high-temperature") || lower.contains("high temp") || lower.contains("heat") || lower.contains("hitze") || lower.contains("canicule") -> "極度高溫熱浪警報"
                    lower.contains("low-temperature") || lower.contains("low temp") || lower.contains("cold") || lower.contains("freeze") || lower.contains("kälte") || lower.contains("froid") -> "嚴寒寒潮警報"
                    lower.contains("fog") || lower.contains("nebel") || lower.contains("brouillard") || lower.contains("niebla") -> "濃霧警報"
                    else -> "歐洲氣象警報: $event"
                }
                AppLanguage.SPANISH -> when {
                    lower.contains("coastal") || lower.contains("costeir") -> "Alerta costera y de oleaje"
                    lower.contains("avalanche") || lower.contains("lawine") -> "Alerta de avalancha"
                    lower.contains("forest") || lower.contains("fire") || lower.contains("waldbrand") || lower.contains("incendie") || lower.contains("incêndio") || lower.contains("incendio") -> "Alerta de incendio forestal"
                    lower.contains("thunder") || lower.contains("gewitter") || lower.contains("orage") || lower.contains("tormenta") || lower.contains("trovoada") -> "Alerta de tormenta eléctrica"
                    lower.contains("snow") || lower.contains("ice") || lower.contains("schnee") || lower.contains("neige") || lower.contains("nieve") || lower.contains("neve") || lower.contains("gelo") -> "Alerta de nieve y hielo"
                    lower.contains("flash flood") -> "Alerta de inundación repentina"
                    lower.contains("flood") || lower.contains("hochwasser") || lower.contains("inondation") || lower.contains("inundaci") || lower.contains("cheia") -> "Alerta de inundación"
                    lower.contains("rain") || lower.contains("regen") || lower.contains("pluie") || lower.contains("lluvia") || lower.contains("chuva") -> "Alerta de lluvia intensa"
                    lower.contains("gale") || lower.contains("storm") || lower.contains("tempestade") -> "Alerta de temporal"
                    lower.contains("wind") || lower.contains("böen") || lower.contains("vent") || lower.contains("viento") || lower.contains("vento") -> "Alerta de viento fuerte"
                    lower.contains("high-temperature") || lower.contains("high temp") || lower.contains("heat") || lower.contains("hitze") || lower.contains("canicule") || lower.contains("calor") -> "Alerta de calor extremo"
                    lower.contains("low-temperature") || lower.contains("low temp") || lower.contains("cold") || lower.contains("freeze") || lower.contains("kälte") || lower.contains("froid") || lower.contains("frio") || lower.contains("frío") -> "Alerta de frío extremo"
                    lower.contains("fog") || lower.contains("nebel") || lower.contains("brouillard") || lower.contains("niebla") || lower.contains("nevoeiro") -> "Alerta de niebla densa"
                    else -> "Alerta MeteoAlarm: $event"
                }
                AppLanguage.FRENCH -> when {
                    lower.contains("coastal") || lower.contains("costeir") -> "Alerte vagues-submersion"
                    lower.contains("avalanche") || lower.contains("lawine") -> "Alerte avalanche"
                    lower.contains("forest") || lower.contains("fire") || lower.contains("waldbrand") || lower.contains("incendie") || lower.contains("incêndio") || lower.contains("incendio") -> "Alerte feu de forêt"
                    lower.contains("thunder") || lower.contains("gewitter") || lower.contains("orage") || lower.contains("tormenta") || lower.contains("trovoada") -> "Alerte orages"
                    lower.contains("snow") || lower.contains("ice") || lower.contains("schnee") || lower.contains("neige") || lower.contains("nieve") || lower.contains("neve") || lower.contains("gelo") -> "Alerte neige-verglas"
                    lower.contains("flash flood") -> "Alerte crue éclair"
                    lower.contains("flood") || lower.contains("hochwasser") || lower.contains("inondation") || lower.contains("inundaci") || lower.contains("cheia") -> "Alerte inondation"
                    lower.contains("rain") || lower.contains("regen") || lower.contains("pluie") || lower.contains("lluvia") || lower.contains("chuva") -> "Alerte fortes pluies"
                    lower.contains("gale") || lower.contains("storm") || lower.contains("tempestade") -> "Alerte tempête"
                    lower.contains("wind") || lower.contains("böen") || lower.contains("vent") || lower.contains("viento") || lower.contains("vento") -> "Alerte vent violent"
                    lower.contains("high-temperature") || lower.contains("high temp") || lower.contains("heat") || lower.contains("hitze") || lower.contains("canicule") || lower.contains("calor") -> "Alerte canicule"
                    lower.contains("low-temperature") || lower.contains("low temp") || lower.contains("cold") || lower.contains("freeze") || lower.contains("kälte") || lower.contains("froid") || lower.contains("frio") || lower.contains("frío") -> "Alerte grand froid"
                    lower.contains("fog") || lower.contains("nebel") || lower.contains("brouillard") || lower.contains("niebla") || lower.contains("nevoeiro") -> "Alerte brouillard dense"
                    else -> "Alerte MeteoAlarm : $event"
                }
                AppLanguage.PORTUGUESE -> when {
                    lower.contains("coastal") || lower.contains("costeir") -> "Alerta de agitação marítima"
                    lower.contains("avalanche") || lower.contains("lawine") -> "Alerta de avalanche"
                    lower.contains("forest") || lower.contains("fire") || lower.contains("waldbrand") || lower.contains("incendie") || lower.contains("incêndio") || lower.contains("incendio") -> "Alerta de incêndio florestal"
                    lower.contains("thunder") || lower.contains("gewitter") || lower.contains("orage") || lower.contains("tormenta") || lower.contains("trovoada") -> "Alerta de trovoada"
                    lower.contains("snow") || lower.contains("ice") || lower.contains("schnee") || lower.contains("neige") || lower.contains("nieve") || lower.contains("neve") || lower.contains("gelo") -> "Alerta de neve e gelo"
                    lower.contains("flash flood") -> "Alerta de enchente relâmpago"
                    lower.contains("flood") || lower.contains("hochwasser") || lower.contains("inondation") || lower.contains("inundaci") || lower.contains("cheia") -> "Alerta de inundação"
                    lower.contains("rain") || lower.contains("regen") || lower.contains("pluie") || lower.contains("lluvia") || lower.contains("chuva") -> "Alerta de chuva forte"
                    lower.contains("gale") || lower.contains("storm") || lower.contains("tempestade") -> "Alerta de tempestade"
                    lower.contains("wind") || lower.contains("böen") || lower.contains("vent") || lower.contains("viento") || lower.contains("vento") -> "Alerta de vento forte"
                    lower.contains("high-temperature") || lower.contains("high temp") || lower.contains("heat") || lower.contains("hitze") || lower.contains("canicule") || lower.contains("calor") -> "Alerta de calor extremo"
                    lower.contains("low-temperature") || lower.contains("low temp") || lower.contains("cold") || lower.contains("freeze") || lower.contains("kälte") || lower.contains("froid") || lower.contains("frio") || lower.contains("frío") -> "Alerta de frio extremo"
                    lower.contains("fog") || lower.contains("nebel") || lower.contains("brouillard") || lower.contains("niebla") || lower.contains("nevoeiro") -> "Alerta de nevoeiro denso"
                    else -> "Alerta MeteoAlarm: $event"
                }
                AppLanguage.ENGLISH -> event
            }
        }
    }
}
