package io.github.nemynew.expressiveweather.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * RainViewer weather-maps.json API レスポンスモデル
 */
@Serializable
data class RainViewerResponse(
    val version: String = "2.0",
    val generated: Long = 0L,
    val host: String = "https://tilecache.rainviewer.com",
    val radar: RadarData = RadarData()
)

@Serializable
data class RadarData(
    val past: List<RadarFrame> = emptyList(),
    val nowcast: List<RadarFrame> = emptyList()
)

@Serializable
data class RadarFrame(
    val time: Long,
    val path: String = "",
    val isForecast: Boolean = false,
    val isExtrapolation: Boolean = false,
    val tileUrlTemplate: String? = null,
    val sourceName: String = "",
    val resolutionInfo: String = "",
    val additionalTileUrlTemplates: List<String> = emptyList()
)

/**
 * レーダーデータプロバイダー種別
 */
enum class RadarDataSource(val displayName: String) {
    JMA_NOWCAST("気象庁 高解像度降水ナウキャスト"),
    US_NEXRAD_HRRR("全米 NEXRAD / HRRR"),
    RAIN_VIEWER("RainViewer");
}

/**
 * 日本の気象庁高解像度降水ナウキャストの提供領域内かどうかを精密に判定する
 * 南西諸島（沖縄・与那国島・奄美）、小笠原・伊豆諸島・南鳥島、五島列島・対馬、本州・四国・九州・北海道、隠岐諸島を完全カバーし、
 * 韓国（釜山・ソウル・済州島・蔚山・浦項）、ロシア極東（ウラジオストク等）、台湾、中国を確実に除外します。
 */
fun isLocationInJapan(latitude: Double, longitude: Double): Boolean {
    // 緯度・経度の日本全体包括境界（日本領土の最遠点を含む外接矩形）
    if (latitude !in 20.0..45.8 || longitude !in 122.5..154.0) return false

    // 周辺国の陸地を精密に除外
    // 1. ロシア沿海地方・極東（ウラジオストク等：北緯40度以北で東経139度未満）
    if (latitude >= 40.0 && longitude < 139.0) return false
    // 2. 日本海北部・朝鮮半島東部（北緯36〜40度で東経131度未満。※隠岐諸島133°E・能登半島136.9°Eは安全に包含）
    if (latitude in 36.0..40.0 && longitude < 131.0) return false
    // 3. 韓国本土（釜山・蔚山・大邱等：北緯34.75〜36度で対馬北端以北の東経130度未満）
    if (latitude in 34.75..36.0 && longitude < 130.0) return false
    // 4. 対馬海峡西側・韓国南部（北緯33〜34.75度で対馬以西の東経129.14度未満。※対馬129.16°E〜129.5°Eは包含）
    if (latitude in 33.0..34.75 && longitude < 129.14) return false
    // 5. 済州島（韓国：北緯31〜33度で五島列島以西の東経128.0度未満。※五島列島128.8°Eは包含）
    if (latitude in 31.0..33.0 && longitude < 128.0) return false
    // 6. 台湾・中国大陸東部（東経122.5度未満は外接矩形により除外済み。※与那国島122.93°Eは包含）

    return true
}

/**
 * 国名情報および座標から日本国内かどうかを絶対確実に判定する
 */
fun isLocationOrCountryInJapan(country: String?, latitude: Double, longitude: Double): Boolean {
    if (!latitude.isFinite() || !longitude.isFinite()) return false
    // 座標が明らかに日本領域外（例: 西経のアメリカ大陸、ヨーロッパ、南半球など）の場合は絶対に日本ではない
    if (longitude < 100.0 || longitude > 165.0 || latitude < 18.0 || latitude > 50.0) {
        return false
    }

    // 1. 国名が明示されている場合はまず国名と整合性を確認（サハリン・台湾・韓国等の誤判定防止）
    if (!country.isNullOrBlank()) {
        val trimmed = country.trim()
        val lower = trimmed.lowercase()
        val mentionsJapan = trimmed.contains("日本") ||
            trimmed.contains("일본") ||
            lower.contains("japan") ||
            lower.contains("japón") ||
            lower.contains("japon") ||
            lower.contains("japão") ||
            lower.contains("япони") ||
            trimmed.equals("JP", ignoreCase = true) ||
            trimmed.equals("JPN", ignoreCase = true)
        return mentionsJapan && isLocationInJapan(latitude, longitude)
    }
    // 2. 国名未指定時は座標による精密領域判定（日本海・韓国・ロシア・台湾の除外を含む）
    return isLocationInJapan(latitude, longitude)
}

/**
 * レーダータイルのカラースキーム定義
 * @property id RainViewerのカラーコード（0〜8）
 * @property displayName 表示名
 */
enum class RadarColorScheme(val id: Int, val displayName: String) {
    UNIVERSAL_BLUE(2, "Universal Blue"),
    RAINBOW(6, "Rainbow"),
    DARK_SKY(7, "Dark Sky"),
    TITAN(3, "TITAN"),
    BLACK_WHITE(0, "Black & White");

    companion object {
        val DEFAULT = UNIVERSAL_BLUE
    }
}

/**
 * RainViewer レーダー反射強度 (dBZ) カラーレベルモデル
 */
data class RainViewerColorLevel(
    val dbzValue: Int,
    val label: String,
    val intensityName: String,
    val color: Color,
    val description: String
) {
    fun intensityName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> intensityName
        AppLanguage.KOREAN -> when (dbzValue) {
            15 -> "매우 약한 비·이슬비"
            20 -> "약한 비"
            25 -> "보통 비"
            35 -> "강한 비"
            45 -> "매우 강한 비"
            50 -> "폭우"
            else -> "극심한 뇌우·호우"
        }
        AppLanguage.ENGLISH -> when (dbzValue) {
            15 -> "Faint / Drizzle"
            20 -> "Light Rain"
            25 -> "Moderate Rain"
            35 -> "Heavy Rain"
            45 -> "Very Heavy Rain"
            50 -> "Intense Downpour"
            else -> "Severe Thunderstorm"
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when (dbzValue) {
            15 -> "微弱/小雨"
            20 -> "小雨"
            25 -> "中雨"
            35 -> "大雨"
            45 -> "暴雨"
            50 -> "大暴雨"
            else -> "特大雷雨/风暴"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (dbzValue) {
            15 -> "微弱/小雨"
            20 -> "小雨"
            25 -> "中雨"
            35 -> "大雨"
            45 -> "暴雨"
            50 -> "大暴雨"
            else -> "特大雷雨/風暴"
        }
        AppLanguage.SPANISH -> when (dbzValue) {
            15 -> "Débil / Llovizna"
            20 -> "Lluvia débil"
            25 -> "Lluvia moderada"
            35 -> "Lluvia fuerte"
            45 -> "Lluvia muy fuerte"
            50 -> "Aguacero intenso"
            else -> "Tormenta severa"
        }
        AppLanguage.FRENCH -> when (dbzValue) {
            15 -> "Faible / Bruine"
            20 -> "Pluie faible"
            25 -> "Pluie modérée"
            35 -> "Pluie forte"
            45 -> "Pluie très forte"
            50 -> "Averse torrentielle"
            else -> "Orage violent"
        }
        AppLanguage.PORTUGUESE -> when (dbzValue) {
            15 -> "Fraca / Garoa"
            20 -> "Chuva fraca"
            25 -> "Chuva moderada"
            35 -> "Chuva forte"
            45 -> "Chuva muito forte"
            50 -> "Aguaceiro intenso"
            else -> "Tempestade severa"
        }
        AppLanguage.RUSSIAN -> when (dbzValue) {
            15 -> "Слабый / Морось"
            20 -> "Небольшой дождь"
            25 -> "Умеренный дождь"
            35 -> "Сильный дождь"
            45 -> "Очень сильный дождь"
            50 -> "Сильный ливень"
            else -> "Сильная гроза"
        }
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> description
        AppLanguage.KOREAN -> when (dbzValue) {
            15 -> "빗방울이 떨어지거나 매우 약한 이슬비 (0.1~1 mm/h)"
            20 -> "우산이 필요한 약한 비 (1~2 mm/h)"
            25 -> "꾸준히 내리며 발이 젖는 보통 비 (2~5 mm/h)"
            35 -> "빗소리가 들리며 이어지는 강한 비 (5~15 mm/h)"
            45 -> "양동이로 퍼붓듯 쏟아지는 매우 강한 비 (15~30 mm/h)"
            50 -> "폭우 수준의 강수, 도로 침수 주의 (30~60 mm/h)"
            else -> "낙뢰·돌풍·우박 위험이 있는 최대급 강수 강도 (60+ mm/h)"
        }
        AppLanguage.ENGLISH -> when (dbzValue) {
            15 -> "Very faint rain or drizzle (0.1-1 mm/h)"
            20 -> "Light rain requiring an umbrella (1-2 mm/h)"
            25 -> "Steady rain, feet get wet (2-5 mm/h)"
            35 -> "Rustling continuous rain (5-15 mm/h)"
            45 -> "Pours like bucketing down (15-30 mm/h)"
            50 -> "Violent downpour, risk of street flooding (30-60 mm/h)"
            else -> "Maximum intensity with risk of severe storms & hail (60+ mm/h)"
        }
        AppLanguage.SIMPLIFIED_CHINESE -> when (dbzValue) {
            15 -> "几乎不影响出行的微弱毛毛雨 (0.1~1 mm/h)"
            20 -> "淅淅沥沥的细雨，需撑伞 (1~2 mm/h)"
            25 -> "中等雨量，路面湿润 (2~5 mm/h)"
            35 -> "哗哗作响的持续降水 (5~15 mm/h)"
            45 -> "犹如倒水倾盆而下的急雨 (15~30 mm/h)"
            50 -> "暴雨如注，警惕低洼积水 (30~60 mm/h)"
            else -> "极强降水，存在强雷暴及短时大风风险 (60+ mm/h)"
        }
        AppLanguage.TRADITIONAL_CHINESE -> when (dbzValue) {
            15 -> "幾乎不影響出行的微弱毛毛雨 (0.1~1 mm/h)"
            20 -> "淅淅瀝瀝的細雨，需撐傘 (1~2 mm/h)"
            25 -> "中等雨量，路面濕潤 (2~5 mm/h)"
            35 -> "嘩嘩作響的持續降水 (5~15 mm/h)"
            45 -> "猶如倒水傾盆而下的急雨 (15~30 mm/h)"
            50 -> "暴雨如注，警惕低窪積水 (30~60 mm/h)"
            else -> "極強降水，存在強雷暴及短時大風風險 (60+ mm/h)"
        }
        AppLanguage.SPANISH -> when (dbzValue) {
            15 -> "Lluvia muy débil o llovizna (0,1-1 mm/h)"
            20 -> "Lluvia ligera, se recomienda paraguas (1-2 mm/h)"
            25 -> "Lluvia constante que moja el calzado (2-5 mm/h)"
            35 -> "Lluvia continua y audible (5-15 mm/h)"
            45 -> "Lluvia torrencial, cae a cántaros (15-30 mm/h)"
            50 -> "Aguacero violento, riesgo de anegamiento urbano (30-60 mm/h)"
            else -> "Intensidad máxima con riesgo de tormentas severas y granizo (60+ mm/h)"
        }
        AppLanguage.FRENCH -> when (dbzValue) {
            15 -> "Pluie très faible ou bruine (0,1-1 mm/h)"
            20 -> "Pluie fine nécessitant un parapluie (1-2 mm/h)"
            25 -> "Pluie soutenue, sol bien mouillé (2-5 mm/h)"
            35 -> "Pluie continue et sonore (5-15 mm/h)"
            45 -> "Pluie battante, averses intenses (15-30 mm/h)"
            50 -> "Averse violente, risque de ruissellement urbain (30-60 mm/h)"
            else -> "Intensité maximale avec risque d'orages violents et de grêle (60+ mm/h)"
        }
        AppLanguage.PORTUGUESE -> when (dbzValue) {
            15 -> "Chuva muito fraca ou garoa (0,1-1 mm/h)"
            20 -> "Chuva leve, recomenda-se guarda-chuva (1-2 mm/h)"
            25 -> "Chuva constante, piso encharcado (2-5 mm/h)"
            35 -> "Chuva forte e contínua (5-15 mm/h)"
            45 -> "Chuva torrencial, cai com força (15-30 mm/h)"
            50 -> "Aguaceiro violento, risco de alagamentos (30-60 mm/h)"
            else -> "Intensidade máxima com risco de tempestades severas e granizo (60+ mm/h)"
        }
        AppLanguage.RUSSIAN -> when (dbzValue) {
            15 -> "Очень слабый дождь или морось (0,1–1 мм/ч)"
            20 -> "Небольшой дождь, нужен зонт (1–2 мм/ч)"
            25 -> "Устойчивый дождь, промокает обувь (2–5 мм/ч)"
            35 -> "Шумный продолжительный дождь (5–15 мм/ч)"
            45 -> "Льёт как из ведра (15–30 мм/ч)"
            50 -> "Мощный ливень, риск подтопления улиц (30–60 мм/ч)"
            else -> "Максимальная интенсивность, риск грозы и града (60+ мм/ч)"
        }
    }
}

/**
 * RainViewer Universal Blue (ID: 2) 標準 dBZ カラースケール
 * 薄い水色 (15 dBZ) から マゼンタ (60+ dBZ) までの公式カラースケール
 */
val RAIN_VIEWER_DBZ_LEVELS = listOf(
    RainViewerColorLevel(
        dbzValue = 15,
        label = "15",
        intensityName = "微弱・霧雨",
        color = Color(0xFF88DDEE), // 薄い水色
        description = "パラつく程度の非常に弱い雨・霧雨 (0.1〜1 mm/h)"
    ),
    RainViewerColorLevel(
        dbzValue = 20,
        label = "20",
        intensityName = "弱い雨",
        color = Color(0xFF00A3E0), // 鮮やかな水色
        description = "しとしとと降る雨、傘が必要 (1〜2 mm/h)"
    ),
    RainViewerColorLevel(
        dbzValue = 25,
        label = "25",
        intensityName = "並の雨",
        color = Color(0xFF0077AA), // 青
        description = "本降りの雨、足元が濡れる (2〜5 mm/h)"
    ),
    RainViewerColorLevel(
        dbzValue = 35,
        label = "35",
        intensityName = "強い雨",
        color = Color(0xFFFFEE00), // 黄色
        description = "ザーザーと音を立てて降るまとまった雨 (5〜15 mm/h)"
    ),
    RainViewerColorLevel(
        dbzValue = 45,
        label = "45",
        intensityName = "激しい雨",
        color = Color(0xFFFF4400), // オレンジ
        description = "バケツをひっくり返したような激しい雨 (15〜30 mm/h)"
    ),
    RainViewerColorLevel(
        dbzValue = 50,
        label = "50",
        intensityName = "非常に激しい雨",
        color = Color(0xFFC10000), // 濃い赤
        description = "土砂降りの豪雨、冠水に警戒 (30〜60 mm/h)"
    ),
    RainViewerColorLevel(
        dbzValue = 60,
        label = "60+",
        intensityName = "猛烈な雷雨・豪雨",
        color = Color(0xFFFF77FF), // マゼンタ
        description = "竜巻・雷・大雨の危険がある最大級の降水強度 (60+ mm/h)"
    )
)

