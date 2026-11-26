package io.github.nemynew.expressiveweather.data

import kotlinx.serialization.Serializable

/**
 * 気象庁 警戒レベル定義 (レベル1〜5 + 平常時0)
 */
@Serializable
enum class AlertLevel(
    val level: Int,
    val title: String,
    val colorHex: Long,
    val textColorHex: Long,
    val actionAdvice: String
) {
    LEVEL_5(
        level = 5,
        title = "災害切迫",
        colorHex = 0xFF3C383E, // 黒色・濃いチャコール
        textColorHex = 0xFFFFD700, // ゴールド
        actionAdvice = "何らかの災害がすでに発生している可能性が極めて高い状況です。命の危険が迫っているため直ちに身の安全を確保して下さい。"
    ),
    LEVEL_4(
        level = 4,
        title = "危険",
        colorHex = 0xFFB84FB3, // 紫色・マゼンタ
        textColorHex = 0xFFFFFFFF,
        actionAdvice = "危険な場所からの避難が必要とされる警戒レベル4に相当します。自治体からの避難指示の発令に留意するとともに、自ら避難の判断をして下さい。"
    ),
    LEVEL_3(
        level = 3,
        title = "警戒",
        colorHex = 0xFFFF5252, // 赤色・コーラル
        textColorHex = 0xFFFFFFFF,
        actionAdvice = "高齢者等は危険な場所からの避難が必要とされる警戒レベル3に相当します。普段の行動を見合わせ始めたり、避難の準備をして下さい。"
    ),
    LEVEL_2(
        level = 2,
        title = "注意",
        colorHex = 0xFFFDD835, // 明るい黄色
        textColorHex = 0xFF212121,
        actionAdvice = "避難行動の確認が必要とされる警戒レベル2に相当します。ハザードマップ等により災害が想定されている区域や避難先、避難経路を確認して下さい。"
    ),
    LEVEL_1(
        level = 1,
        title = "早期注意",
        colorHex = 0xFFE0E0E0, // 薄いグレー
        textColorHex = 0xFF212121,
        actionAdvice = "災害への心構えを高める必要があります。最新の防災気象情報等に留意して下さい。"
    ),
    LEVEL_0(
        level = 0,
        title = "平常",
        colorHex = 0xFF43A047, // 平常グリーン
        textColorHex = 0xFFFFFFFF,
        actionAdvice = "現在のところ警報・注意報は発表されていません。"
    );

    fun getTitle(language: AppLanguage): String = when (this) {
        LEVEL_5 -> when (language) {
            AppLanguage.ENGLISH -> "Emergency Safety"
            AppLanguage.KOREAN -> "재난 임박"
            AppLanguage.SIMPLIFIED_CHINESE -> "灾害切迫"
            AppLanguage.TRADITIONAL_CHINESE -> "災害切迫"
            AppLanguage.SPANISH -> "Peligro inminente"
            AppLanguage.FRENCH -> "Danger imminent"
            AppLanguage.PORTUGUESE -> "Perigo iminente"
            AppLanguage.RUSSIAN -> "Экстренная угроза"
            AppLanguage.JAPANESE -> title
        }
        LEVEL_4 -> when (language) {
            AppLanguage.ENGLISH -> "Danger"
            AppLanguage.KOREAN -> "위험"
            AppLanguage.SIMPLIFIED_CHINESE -> "危险"
            AppLanguage.TRADITIONAL_CHINESE -> "危險"
            AppLanguage.SPANISH -> "Peligro"
            AppLanguage.FRENCH -> "Danger"
            AppLanguage.PORTUGUESE -> "Perigo"
            AppLanguage.RUSSIAN -> "Опасность"
            AppLanguage.JAPANESE -> title
        }
        LEVEL_3 -> when (language) {
            AppLanguage.ENGLISH -> "Warning"
            AppLanguage.KOREAN -> "경계"
            AppLanguage.SIMPLIFIED_CHINESE -> "警戒"
            AppLanguage.TRADITIONAL_CHINESE -> "警戒"
            AppLanguage.SPANISH -> "Alerta"
            AppLanguage.FRENCH -> "Alerte"
            AppLanguage.PORTUGUESE -> "Alerta"
            AppLanguage.RUSSIAN -> "Предупреждение"
            AppLanguage.JAPANESE -> title
        }
        LEVEL_2 -> when (language) {
            AppLanguage.ENGLISH -> "Advisory"
            AppLanguage.KOREAN -> "주의"
            AppLanguage.SIMPLIFIED_CHINESE -> "注意"
            AppLanguage.TRADITIONAL_CHINESE -> "注意"
            AppLanguage.SPANISH -> "Aviso"
            AppLanguage.FRENCH -> "Vigilance"
            AppLanguage.PORTUGUESE -> "Aviso"
            AppLanguage.RUSSIAN -> "Внимание"
            AppLanguage.JAPANESE -> title
        }
        LEVEL_1 -> when (language) {
            AppLanguage.ENGLISH -> "Early Caution"
            AppLanguage.KOREAN -> "조기 주의"
            AppLanguage.SIMPLIFIED_CHINESE -> "早期注意"
            AppLanguage.TRADITIONAL_CHINESE -> "早期注意"
            AppLanguage.SPANISH -> "Precaución temprana"
            AppLanguage.FRENCH -> "Attention précoce"
            AppLanguage.PORTUGUESE -> "Atenção inicial"
            AppLanguage.RUSSIAN -> "Раннее внимание"
            AppLanguage.JAPANESE -> title
        }
        LEVEL_0 -> when (language) {
            AppLanguage.ENGLISH -> "Normal"
            AppLanguage.KOREAN -> "평상"
            AppLanguage.SIMPLIFIED_CHINESE -> "正常"
            AppLanguage.TRADITIONAL_CHINESE -> "正常"
            AppLanguage.SPANISH -> "Normal"
            AppLanguage.FRENCH -> "Normal"
            AppLanguage.PORTUGUESE -> "Normal"
            AppLanguage.RUSSIAN -> "Норма"
            AppLanguage.JAPANESE -> title
        }
    }

    companion object {
        fun fromLevel(lvl: Int): AlertLevel {
            return entries.firstOrNull { it.level == lvl } ?: LEVEL_0
        }
    }
}

/**
 * 対象災害種別（気象庁キキクル定義に完全準拠）
 */
@Serializable
enum class DisasterType(
    val id: String,
    val labelJa: String,
    val labelEn: String,
    val labelKo: String,
    val labelZhHans: String,
    val labelZhHant: String,
    val labelEs: String,
    val labelFr: String,
    val labelPt: String,
    val labelRu: String,
    val shortLabelJa: String,
    val shortLabelEn: String,
    val shortLabelKo: String,
    val shortLabelZhHans: String,
    val shortLabelZhHant: String,
    val shortLabelEs: String,
    val shortLabelFr: String,
    val shortLabelPt: String,
    val shortLabelRu: String,
    val radarLayerId: String,
    val priority: Int
) {
    HEAVY_RAIN(
        id = "heavy_rain",
        labelJa = "大雨キキクル",
        labelEn = "Heavy Rain",
        labelKo = "호우 위험도",
        labelZhHans = "大雨危险度",
        labelZhHant = "大雨危險度",
        labelEs = "Lluvia intensa",
        labelFr = "Fortes pluies",
        labelPt = "Chuva intensa",
        labelRu = "Сильный дождь",
        shortLabelJa = "大雨",
        shortLabelEn = "Rain",
        shortLabelKo = "호우",
        shortLabelZhHans = "大雨",
        shortLabelZhHant = "大雨",
        shortLabelEs = "Lluvia",
        shortLabelFr = "Pluie",
        shortLabelPt = "Chuva",
        shortLabelRu = "Дождь",
        radarLayerId = "hrpns",
        priority = 1
    ),
    LANDSLIDE(
        id = "landslide",
        labelJa = "土砂キキクル",
        labelEn = "Landslide",
        labelKo = "산사태 위험도",
        labelZhHans = "土砂灾害",
        labelZhHant = "土砂災害",
        labelEs = "Deslizamiento",
        labelFr = "Glissement de terrain",
        labelPt = "Deslizamento",
        labelRu = "Оползни",
        shortLabelJa = "土砂",
        shortLabelEn = "Landslide",
        shortLabelKo = "산사태",
        shortLabelZhHans = "土砂",
        shortLabelZhHant = "土砂",
        shortLabelEs = "Deslave",
        shortLabelFr = "Glissement",
        shortLabelPt = "Deslizamento",
        shortLabelRu = "Оползень",
        radarLayerId = "landslide",
        priority = 2
    ),
    INUNDATION(
        id = "inundation",
        labelJa = "浸水キキクル",
        labelEn = "Inundation",
        labelKo = "침수 위험도",
        labelZhHans = "浸水害",
        labelZhHant = "浸水害",
        labelEs = "Anegamiento",
        labelFr = "Inondation",
        labelPt = "Alagamento",
        labelRu = "Подтопление",
        shortLabelJa = "浸水",
        shortLabelEn = "Inundation",
        shortLabelKo = "침수",
        shortLabelZhHans = "浸水",
        shortLabelZhHant = "浸水",
        shortLabelEs = "Anegamiento",
        shortLabelFr = "Inondation",
        shortLabelPt = "Alagamento",
        shortLabelRu = "Подтопление",
        radarLayerId = "inundation",
        priority = 3
    ),
    FLOOD(
        id = "flood",
        labelJa = "洪水キキクル",
        labelEn = "Flood",
        labelKo = "홍수 위험도",
        labelZhHans = "洪水灾害",
        labelZhHant = "洪水災害",
        labelEs = "Inundación",
        labelFr = "Crue",
        labelPt = "Inundação",
        labelRu = "Наводнение",
        shortLabelJa = "洪水",
        shortLabelEn = "Flood",
        shortLabelKo = "홍수",
        shortLabelZhHans = "洪水",
        shortLabelZhHant = "洪水",
        shortLabelEs = "Inundación",
        shortLabelFr = "Crue",
        shortLabelPt = "Inundação",
        shortLabelRu = "Паводок",
        radarLayerId = "flood",
        priority = 4
    ),
    STORM_WIND(
        id = "storm_wind",
        labelJa = "暴風・強風",
        labelEn = "Storm Winds",
        labelKo = "폭풍·강풍",
        labelZhHans = "大风灾害",
        labelZhHant = "大風災害",
        labelEs = "Vientos fuertes",
        labelFr = "Vents violents",
        labelPt = "Ventos fortes",
        labelRu = "Штормовой ветер",
        shortLabelJa = "暴風",
        shortLabelEn = "Wind",
        shortLabelKo = "강풍",
        shortLabelZhHans = "暴风",
        shortLabelZhHant = "暴風",
        shortLabelEs = "Viento",
        shortLabelFr = "Vent",
        shortLabelPt = "Vento",
        shortLabelRu = "Ветер",
        radarLayerId = "hrpns",
        priority = 5
    );

    fun getLabel(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> labelJa
        AppLanguage.KOREAN -> labelKo
        AppLanguage.ENGLISH -> labelEn
        AppLanguage.SIMPLIFIED_CHINESE -> labelZhHans
        AppLanguage.TRADITIONAL_CHINESE -> labelZhHant
        AppLanguage.SPANISH -> labelEs
        AppLanguage.FRENCH -> labelFr
        AppLanguage.PORTUGUESE -> labelPt
        AppLanguage.RUSSIAN -> labelRu
    }

    fun getShortLabel(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> shortLabelJa
        AppLanguage.KOREAN -> shortLabelKo
        AppLanguage.ENGLISH -> shortLabelEn
        AppLanguage.SIMPLIFIED_CHINESE -> shortLabelZhHans
        AppLanguage.TRADITIONAL_CHINESE -> shortLabelZhHant
        AppLanguage.SPANISH -> shortLabelEs
        AppLanguage.FRENCH -> shortLabelFr
        AppLanguage.PORTUGUESE -> shortLabelPt
        AppLanguage.RUSSIAN -> shortLabelRu
    }
}

/**
 * 各キキクル災害種別ごとの詳細状況・行動指針・避難情報
 */
@Serializable
data class KikikuruDisasterDetail(
    val disasterType: DisasterType,
    val alertLevel: AlertLevel,
    val activeWarnings: List<MunicipalityWarningItem> = emptyList(),
    val headlineText: String? = null
) {
    fun getTitle(language: AppLanguage): String {
        val levelNum = alertLevel.level
        val levelTitle = alertLevel.getTitle(language)
        val typeLabel = disasterType.getLabel(language)
        val prefix = when (language) {
            AppLanguage.ENGLISH -> "Alert Level $levelNum: $levelTitle"
            AppLanguage.KOREAN -> "경계 레벨 $levelNum $levelTitle"
            AppLanguage.SIMPLIFIED_CHINESE -> "警戒等级$levelNum $levelTitle"
            AppLanguage.TRADITIONAL_CHINESE -> "警戒等級$levelNum $levelTitle"
            AppLanguage.SPANISH -> "Nivel de alerta $levelNum: $levelTitle"
            AppLanguage.FRENCH -> "Niveau d'alerte $levelNum : $levelTitle"
            AppLanguage.PORTUGUESE -> "Nível de alerta $levelNum: $levelTitle"
            AppLanguage.RUSSIAN -> "Уровень тревоги $levelNum: $levelTitle"
            AppLanguage.JAPANESE -> "警戒レベル$levelNum $levelTitle"
        }
        return "$prefix ($typeLabel)"
    }

    fun getActionAdvice(language: AppLanguage): String =
        KikikuruGuidelineContent.getActionAdvice(disasterType, alertLevel, language)

    fun getSituationExample(language: AppLanguage): String =
        KikikuruGuidelineContent.getSituationExample(disasterType, alertLevel, language)

    fun getEvacuationInfo(language: AppLanguage): String? =
        KikikuruGuidelineContent.getEvacuationInfo(alertLevel, language)

    fun getButtonText(language: AppLanguage): String =
        KikikuruGuidelineContent.getButtonLabel(disasterType, language)
}

/**
 * 対象災害フラグ集計
 */
@Serializable
data class DisasterTargetFlags(
    val landslide: Boolean = false,
    val inundation: Boolean = false,
    val flood: Boolean = false,
    val stormWind: Boolean = false,
    val heavyRain: Boolean = false
) {
    val activeTypes: List<DisasterType>
        get() {
            val list = mutableListOf<DisasterType>()
            if (heavyRain) list.add(DisasterType.HEAVY_RAIN)
            if (landslide) list.add(DisasterType.LANDSLIDE)
            if (inundation) list.add(DisasterType.INUNDATION)
            if (flood) list.add(DisasterType.FLOOD)
            if (stormWind) list.add(DisasterType.STORM_WIND)
            return list.sortedBy { it.priority }
        }

    val primaryType: DisasterType? get() = activeTypes.firstOrNull()
    val subTypes: List<DisasterType> get() = activeTypes.drop(1)
    val hasAnyDisaster: Boolean get() = heavyRain || landslide || inundation || flood || stormWind
}

/**
 * 自治体単位の警報・注意報アイテム
 */
@Serializable
data class MunicipalityWarningItem(
    val code: String,
    val name: String,
    val status: String, // "発表", "継続", "解除"
    val isWarning: Boolean,
    val isSpecial: Boolean = false
)

/**
 * 自治体単位の総合警戒レベル・キキクル情報
 */
@Serializable
data class MunicipalityAlertInfo(
    val municipalityCode: String = "",
    val municipalityName: String = "",
    val prefectureCode: String = "",
    val prefectureName: String = "",
    val alertLevel: AlertLevel = AlertLevel.LEVEL_0,
    val disasterFlags: DisasterTargetFlags = DisasterTargetFlags(),
    val activeWarnings: List<MunicipalityWarningItem> = emptyList(),
    val clearedWarnings: List<MunicipalityWarningItem> = emptyList(),
    val headlineText: String? = null,
    val publishingOffice: String? = null,
    val reportDatetime: String? = null,
    val targetDisasterPrimary: DisasterType? = disasterFlags.primaryType,
    val disasterDetails: List<KikikuruDisasterDetail> = emptyList()
) {
    val hasActiveAlert: Boolean get() = alertLevel.level >= 2
    val activeWarningNames: List<String> get() = activeWarnings.map { it.name }
    val hasActiveWarnings: Boolean get() = activeWarnings.isNotEmpty()
    val hasClearedWarnings: Boolean get() = clearedWarnings.isNotEmpty()
    val hasAnyDisasterInfo: Boolean get() = hasActiveWarnings || hasActiveAlert

    /**
     * 4大キキクル（大雨・土砂・浸水・洪水）すべての詳細を確実に取得
     */
    val allDisasterDetails: List<KikikuruDisasterDetail>
        get() {
            if (disasterDetails.isNotEmpty()) return disasterDetails
            // フォールバック: 現在の警戒レベルとフラグから4大キキクル詳細を生成
            return listOf(
                DisasterType.HEAVY_RAIN,
                DisasterType.LANDSLIDE,
                DisasterType.INUNDATION,
                DisasterType.FLOOD
            ).map { type ->
                val lvl = when (type) {
                    DisasterType.LANDSLIDE -> if (disasterFlags.landslide) alertLevel else AlertLevel.LEVEL_0
                    DisasterType.INUNDATION -> if (disasterFlags.inundation) alertLevel else AlertLevel.LEVEL_0
                    DisasterType.FLOOD -> if (disasterFlags.flood) alertLevel else AlertLevel.LEVEL_0
                    DisasterType.HEAVY_RAIN -> alertLevel
                    else -> alertLevel
                }
                KikikuruDisasterDetail(
                    disasterType = type,
                    alertLevel = lvl,
                    activeWarnings = activeWarnings
                )
            }
        }

    fun getDetailFor(type: DisasterType): KikikuruDisasterDetail {
        return allDisasterDetails.firstOrNull { it.disasterType == type }
            ?: KikikuruDisasterDetail(disasterType = type, alertLevel = alertLevel, activeWarnings = activeWarnings)
    }
}
