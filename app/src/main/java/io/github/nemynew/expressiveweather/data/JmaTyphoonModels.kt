package io.github.nemynew.expressiveweather.data

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng

/**
 * 気象庁公式の強さの階級 (Intensity)
 * 最大風速基準:
 * - 猛烈な (Violent): 54 m/s (105 kt) 以上
 * - 非常に強い (Very Strong): 44 m/s (85 kt) 〜 54 m/s 未満
 * - 強い (Strong): 33 m/s (64 kt) 〜 44 m/s 未満
 * - なし (None / -): 33 m/s 未満 (台風 / 熱帯低気圧)
 */
enum class JmaTyphoonIntensity(
    val id: String,
    val jaName: String,
    val enName: String,
    val koName: String,
    val zhCnName: String,
    val zhTwName: String,
    val esName: String,
    val frName: String,
    val ptName: String,
    val ruName: String,
    val badgeColor: Color,
    val badgeContentColor: Color
) {
    VIOLENT(
        id = "violent",
        jaName = "猛烈な",
        enName = "Violent",
        koName = "초강력",
        zhCnName = "超强台风",
        zhTwName = "強烈颱風",
        esName = "Violento",
        frName = "Violent",
        ptName = "Violento",
        ruName = "Сверхсильный",
        badgeColor = Color(0xFF9C27B0), // Purple 500
        badgeContentColor = Color.White
    ),
    VERY_STRONG(
        id = "very_strong",
        jaName = "非常に強い",
        enName = "Very Strong",
        koName = "매우 강",
        zhCnName = "强台风",
        zhTwName = "中度颱風(強)",
        esName = "Muy fuerte",
        frName = "Très fort",
        ptName = "Muito forte",
        ruName = "Очень сильный",
        badgeColor = Color(0xFFD32F2F), // Red 700
        badgeContentColor = Color.White
    ),
    STRONG(
        id = "strong",
        jaName = "強い",
        enName = "Strong",
        koName = "강",
        zhCnName = "台风",
        zhTwName = "中度颱風",
        esName = "Fuerte",
        frName = "Fort",
        ptName = "Forte",
        ruName = "Сильный",
        badgeColor = Color(0xFFFF9800), // Orange 500
        badgeContentColor = Color(0xFF2E1500)
    ),
    NONE(
        id = "none",
        jaName = "",
        enName = "",
        koName = "",
        zhCnName = "",
        zhTwName = "",
        esName = "",
        frName = "",
        ptName = "",
        ruName = "",
        badgeColor = Color(0xFF0288D1), // Light Blue 700
        badgeContentColor = Color.White
    );

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> jaName
        AppLanguage.KOREAN -> koName
        AppLanguage.ENGLISH -> enName
        AppLanguage.SIMPLIFIED_CHINESE -> zhCnName
        AppLanguage.TRADITIONAL_CHINESE -> zhTwName
        AppLanguage.SPANISH -> esName
        AppLanguage.FRENCH -> frName
        AppLanguage.PORTUGUESE -> ptName
        AppLanguage.RUSSIAN -> ruName
    }

    companion object {
        fun fromJmaString(intensity: String?): JmaTyphoonIntensity {
            if (intensity.isNullOrBlank()) return NONE
            return when {
                intensity.contains("猛烈") -> VIOLENT
                intensity.contains("非常に強い") -> VERY_STRONG
                intensity.contains("強い") -> STRONG
                else -> NONE
            }
        }
    }
}

/**
 * 気象庁公式の大きさの階級 (Scale)
 * 強風域 (風速15m/s以上) の半径基準:
 * - 超大型 (Very Large): 800 km 以上
 * - 大型 (Large): 500 km 〜 800 km 未満
 * - なし (None / -): 500 km 未満
 */
enum class JmaTyphoonScale(
    val id: String,
    val jaName: String,
    val enName: String,
    val koName: String,
    val zhCnName: String,
    val zhTwName: String,
    val esName: String,
    val frName: String,
    val ptName: String,
    val ruName: String
) {
    VERY_LARGE(
        id = "very_large",
        jaName = "超大型",
        enName = "Very Large",
        koName = "초대형",
        zhCnName = "超大型",
        zhTwName = "超大型",
        esName = "Muy grande",
        frName = "Très vaste",
        ptName = "Muito grande",
        ruName = "Очень крупный"
    ),
    LARGE(
        id = "large",
        jaName = "大型",
        enName = "Large",
        koName = "대형",
        zhCnName = "大型",
        zhTwName = "大型",
        esName = "Grande",
        frName = "Vaste",
        ptName = "Grande",
        ruName = "Крупный"
    ),
    NONE(
        id = "none",
        jaName = "",
        enName = "",
        koName = "",
        zhCnName = "",
        zhTwName = "",
        esName = "",
        frName = "",
        ptName = "",
        ruName = ""
    );

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> jaName
        AppLanguage.KOREAN -> koName
        AppLanguage.ENGLISH -> enName
        AppLanguage.SIMPLIFIED_CHINESE -> zhCnName
        AppLanguage.TRADITIONAL_CHINESE -> zhTwName
        AppLanguage.SPANISH -> esName
        AppLanguage.FRENCH -> frName
        AppLanguage.PORTUGUESE -> ptName
        AppLanguage.RUSSIAN -> ruName
    }

    companion object {
        fun fromJmaString(scale: String?): JmaTyphoonScale {
            if (scale.isNullOrBlank()) return NONE
            return when {
                scale.contains("超大型") -> VERY_LARGE
                scale.contains("大型") -> LARGE
                else -> NONE
            }
        }
    }
}

/**
 * 過去から現在、各予測時刻における台風の位置・諸元データ
 */
data class JmaTyphoonPoint(
    val partName: String, // "実況", "予報　２４時間後", etc.
    val isAnalysis: Boolean, // true = 実況, false = 予報
    val advancedHours: Int, // 0, 12, 24, 45, 69, 93, 117
    val validTimeJst: String, // "2026-10-01T18:00:00+09:00"
    val validTimeFormatted: String, // "10/01 18:00" or "02日18時"
    val latitude: Double,
    val longitude: Double,
    val locationName: String, // "マリアナ諸島", "銚子市の東約280km"
    val course: String, // "北北西", "北東", "ほとんど停滞"
    val speedText: String, // "15 km/h", "ゆっくり", "ほとんど停滞"
    val pressureHpa: Int?, // 945
    val maxWindMs: Int?, // 45 (中心付近最大風速)
    val maxWindKt: Int?, // 85
    val gustWindMs: Int?, // 60 (最大瞬間風速)
    val gustWindKt: Int?, // 120
    val intensity: JmaTyphoonIntensity,
    val scale: JmaTyphoonScale,
    val categoryName: String, // "台風", "熱帯低気圧", "温帯低気圧"
    val galeRadiusKm: Double? = null, // 強風域半径 (km)
    val stormRadiusKm: Double? = null, // 暴風域半径 (km)
    val probabilityCircleRadiusKm: Double? = null, // 予報円半径 (km)
    val probabilityCircleRadiusMeters: Double? = null // 予報円半径 (meters)
) {
    /**
     * 気象庁公式の総合勢力区分呼称を生成
     * 例: 「大型で非常に強い台風」「超大型で猛烈な台風」「非常に強い台風」「大型の台風」「台風」
     */
    fun officialClassification(language: AppLanguage): String {
        val hasScale = scale != JmaTyphoonScale.NONE
        val hasIntensity = intensity != JmaTyphoonIntensity.NONE

        return when (language) {
            AppLanguage.JAPANESE -> {
                when {
                    hasScale && hasIntensity -> "${scale.jaName}で${intensity.jaName}$categoryName"
                    hasScale -> "${scale.jaName}の$categoryName"
                    hasIntensity -> "${intensity.jaName}$categoryName"
                    else -> categoryName
                }
            }
            AppLanguage.KOREAN -> {
                val base = when {
                    categoryName.contains("温帯") -> "온대저기압"
                    categoryName.contains("熱帯低気圧") -> "열대저압부"
                    else -> "태풍"
                }
                val intensityPhrase = when (intensity) {
                    JmaTyphoonIntensity.VIOLENT -> "초강력"
                    JmaTyphoonIntensity.VERY_STRONG -> "매우 강한"
                    JmaTyphoonIntensity.STRONG -> "강한"
                    JmaTyphoonIntensity.NONE -> ""
                }
                when {
                    hasScale && hasIntensity -> "${scale.koName} $intensityPhrase $base"
                    hasScale -> "${scale.koName} $base"
                    hasIntensity -> "$intensityPhrase $base"
                    else -> base
                }
            }
            AppLanguage.ENGLISH -> {
                val base = when {
                    categoryName.contains("温帯") -> "Extratropical Low"
                    categoryName.contains("熱帯低気圧") -> "Tropical Depression"
                    else -> "Typhoon"
                }
                when {
                    hasScale && hasIntensity -> "${scale.enName} & ${intensity.enName} $base"
                    hasScale -> "${scale.enName} $base"
                    hasIntensity -> "${intensity.enName} $base"
                    else -> base
                }
            }
            AppLanguage.SIMPLIFIED_CHINESE -> {
                val base = when {
                    categoryName.contains("温帯") -> "温带气旋"
                    categoryName.contains("熱帯低気圧") -> "热带低压"
                    else -> "台风"
                }
                when {
                    hasScale && hasIntensity -> "${scale.zhCnName}${intensity.zhCnName}"
                    hasScale -> "${scale.zhCnName}$base"
                    hasIntensity -> intensity.zhCnName
                    else -> base
                }
            }
            AppLanguage.TRADITIONAL_CHINESE -> {
                val base = when {
                    categoryName.contains("温帯") -> "溫帶氣旋"
                    categoryName.contains("熱帯低気圧") -> "熱帶性低氣壓"
                    else -> "颱風"
                }
                when {
                    hasScale && hasIntensity -> "${scale.zhTwName}${intensity.zhTwName}"
                    hasScale -> "${scale.zhTwName}$base"
                    hasIntensity -> intensity.zhTwName
                    else -> base
                }
            }
            AppLanguage.SPANISH -> {
                val isFeminine = categoryName.contains("熱帯低気圧")
                val base = when {
                    categoryName.contains("温帯") -> "Ciclón extratropical"
                    isFeminine -> "Depresión tropical"
                    else -> "Tifón"
                }
                val scaleAdj = when (scale) {
                    JmaTyphoonScale.VERY_LARGE -> "muy grande"
                    JmaTyphoonScale.LARGE -> "grande"
                    JmaTyphoonScale.NONE -> ""
                }
                val intensityAdj = when (intensity) {
                    JmaTyphoonIntensity.VIOLENT -> if (isFeminine) "violenta" else "violento"
                    JmaTyphoonIntensity.VERY_STRONG -> "muy fuerte"
                    JmaTyphoonIntensity.STRONG -> "fuerte"
                    JmaTyphoonIntensity.NONE -> ""
                }
                when {
                    hasScale && hasIntensity -> "$base $scaleAdj y $intensityAdj"
                    hasScale -> "$base $scaleAdj"
                    hasIntensity -> "$base $intensityAdj"
                    else -> base
                }
            }
            AppLanguage.FRENCH -> {
                val isFeminine = categoryName.contains("温帯") || categoryName.contains("熱帯低気圧")
                val base = when {
                    categoryName.contains("温帯") -> "Dépression extratropicale"
                    categoryName.contains("熱帯低気圧") -> "Dépression tropicale"
                    else -> "Typhon"
                }
                val scaleAdj = when (scale) {
                    JmaTyphoonScale.VERY_LARGE -> "très vaste"
                    JmaTyphoonScale.LARGE -> "vaste"
                    JmaTyphoonScale.NONE -> ""
                }
                val intensityAdj = when (intensity) {
                    JmaTyphoonIntensity.VIOLENT -> if (isFeminine) "violente" else "violent"
                    JmaTyphoonIntensity.VERY_STRONG -> if (isFeminine) "très forte" else "très fort"
                    JmaTyphoonIntensity.STRONG -> if (isFeminine) "forte" else "fort"
                    JmaTyphoonIntensity.NONE -> ""
                }
                when {
                    hasScale && hasIntensity -> "$base $scaleAdj et $intensityAdj"
                    hasScale -> "$base $scaleAdj"
                    hasIntensity -> "$base $intensityAdj"
                    else -> base
                }
            }
            AppLanguage.PORTUGUESE -> {
                val isFeminine = categoryName.contains("熱帯低気圧")
                val base = when {
                    categoryName.contains("温帯") -> "Ciclone extratropical"
                    isFeminine -> "Depressão tropical"
                    else -> "Tufão"
                }
                val scaleAdj = when (scale) {
                    JmaTyphoonScale.VERY_LARGE -> "muito grande"
                    JmaTyphoonScale.LARGE -> "grande"
                    JmaTyphoonScale.NONE -> ""
                }
                val intensityAdj = when (intensity) {
                    JmaTyphoonIntensity.VIOLENT -> if (isFeminine) "violenta" else "violento"
                    JmaTyphoonIntensity.VERY_STRONG -> "muito forte"
                    JmaTyphoonIntensity.STRONG -> "forte"
                    JmaTyphoonIntensity.NONE -> ""
                }
                when {
                    hasScale && hasIntensity -> "$base $scaleAdj e $intensityAdj"
                    hasScale -> "$base $scaleAdj"
                    hasIntensity -> "$base $intensityAdj"
                    else -> base
                }
            }
            AppLanguage.RUSSIAN -> {
                val isFeminine = categoryName.contains("熱帯低気圧")
                val base = when {
                    categoryName.contains("温帯") -> "Внетропический циклон"
                    isFeminine -> "Тропическая депрессия"
                    else -> "Тайфун"
                }
                val baseLower = base.replaceFirstChar { it.lowercase() }
                val scaleAdj = when (scale) {
                    JmaTyphoonScale.VERY_LARGE -> if (isFeminine) "очень крупная" else "очень крупный"
                    JmaTyphoonScale.LARGE -> if (isFeminine) "крупная" else "крупный"
                    JmaTyphoonScale.NONE -> ""
                }
                val intensityAdj = when (intensity) {
                    JmaTyphoonIntensity.VIOLENT -> if (isFeminine) "сверхсильная" else "сверхсильный"
                    JmaTyphoonIntensity.VERY_STRONG -> if (isFeminine) "очень сильная" else "очень сильный"
                    JmaTyphoonIntensity.STRONG -> if (isFeminine) "сильная" else "сильный"
                    JmaTyphoonIntensity.NONE -> ""
                }
                when {
                    hasScale && hasIntensity -> "${scaleAdj.replaceFirstChar { it.uppercase() }} и $intensityAdj $baseLower"
                    hasScale -> "${scaleAdj.replaceFirstChar { it.uppercase() }} $baseLower"
                    hasIntensity -> "${intensityAdj.replaceFirstChar { it.uppercase() }} $baseLower"
                    else -> base
                }
            }
        }
    }
}

/**
 * 予報円および進路予想の幾何情報 (地図描画用)
 */
data class JmaProbabilityCircleGeometry(
    val center: LatLng,
    val radiusMeters: Double,
    val tangents: List<List<LatLng>>, // 予報円を結ぶ接線
    val validTimeJst: String,
    val labelText: String
)

/**
 * 暴風警戒域幾何情報 (地図描画用)
 */
data class JmaStormWarningAreaGeometry(
    val arcs: List<List<Double>>, // [lat, lon, radiusMeters, startAngle, endAngle]
    val lines: List<List<LatLng>> // 結線
)

/**
 * 1つの台風の全体情報（実況＋予報タイムライン＋幾何情報）
 */
data class JmaTyphoonItem(
    val id: String, // "TC2633"
    val rawTyphoonNumber: String, // "2627"
    val formattedNumber: String, // "第27号" / "No.27"
    val nameJp: String, // "チョーイワン"
    val nameEn: String, // "Choi-wan"
    val issueTimeJst: String, // "2026-10-01T19:05:00+09:00"
    val formattedIssueTime: String, // "10/01 19:05 発表"
    val analysis: JmaTyphoonPoint, // 実況
    val forecasts: List<JmaTyphoonPoint>, // 今後の予報 (12h, 24h, 45h, 69h, 93h, 117h)
    val pastTrack: List<LatLng>, // 過去の経路座標 (古い順 -> 現在中心)
    val galeWarningRadiusMeters: Double?, // 強風域半径 (m)
    val stormWarningRadiusMeters: Double?, // 暴風域半径 (m)
    val probabilityCircles: List<JmaProbabilityCircleGeometry>, // 予報円
    val stormWarningAreas: List<JmaStormWarningAreaGeometry> = emptyList() // 暴風警戒域
) {
    private fun numericOrder(): Int? = Regex("(\\d+)").find(formattedNumber)?.groupValues?.get(1)?.toIntOrNull()
        ?: rawTyphoonNumber.takeLast(2).toIntOrNull()

    fun localizedName(language: AppLanguage): String {
        val num = numericOrder()
        return when (language) {
            AppLanguage.JAPANESE -> "$formattedNumber ($nameJp)"
            AppLanguage.KOREAN -> if (num != null) "제${num}호 태풍 ($nameEn)" else "태풍 $formattedNumber ($nameEn)"
            AppLanguage.ENGLISH -> if (num != null) "Typhoon No.$num ($nameEn)" else "Typhoon $formattedNumber ($nameEn)"
            AppLanguage.SIMPLIFIED_CHINESE -> if (num != null) "第${num}号台风 ($nameEn)" else "$formattedNumber ($nameEn)"
            AppLanguage.TRADITIONAL_CHINESE -> if (num != null) "第${num}號颱風 ($nameEn)" else "$formattedNumber ($nameEn)"
            AppLanguage.SPANISH -> if (num != null) "Tifón n.º $num ($nameEn)" else "Tifón $formattedNumber ($nameEn)"
            AppLanguage.FRENCH -> if (num != null) "Typhon n° $num ($nameEn)" else "Typhon $formattedNumber ($nameEn)"
            AppLanguage.PORTUGUESE -> if (num != null) "Tufão nº $num ($nameEn)" else "Tufão $formattedNumber ($nameEn)"
            AppLanguage.RUSSIAN -> if (num != null) "Тайфун № $num ($nameEn)" else "Тайфун $formattedNumber ($nameEn)"
        }
    }

    fun simpleName(language: AppLanguage): String {
        val num = numericOrder()
        return when (language) {
            AppLanguage.JAPANESE -> "$formattedNumber $nameJp"
            AppLanguage.KOREAN -> if (num != null) "제${num}호 $nameEn" else "$formattedNumber $nameEn"
            AppLanguage.ENGLISH -> if (num != null) "No.$num $nameEn" else "$formattedNumber $nameEn"
            AppLanguage.SIMPLIFIED_CHINESE -> if (num != null) "第${num}号 $nameEn" else "$formattedNumber $nameEn"
            AppLanguage.TRADITIONAL_CHINESE -> if (num != null) "第${num}號 $nameEn" else "$formattedNumber $nameEn"
            AppLanguage.SPANISH -> if (num != null) "N.º $num $nameEn" else "$formattedNumber $nameEn"
            AppLanguage.FRENCH -> if (num != null) "N° $num $nameEn" else "$formattedNumber $nameEn"
            AppLanguage.PORTUGUESE -> if (num != null) "Nº $num $nameEn" else "$formattedNumber $nameEn"
            AppLanguage.RUSSIAN -> if (num != null) "№ $num $nameEn" else "$formattedNumber $nameEn"
        }
    }
}
