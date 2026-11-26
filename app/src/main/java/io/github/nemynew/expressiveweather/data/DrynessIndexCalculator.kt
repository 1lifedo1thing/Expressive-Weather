package io.github.nemynew.expressiveweather.data

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 乾燥スコア（0〜100）の 5 段階ランク（20 点刻み）。
 */
enum class DrynessRank(
    val minScore: Int,
    val maxScore: Int
) {
    MOIST(0, 19),
    SLIGHTLY_DRY(20, 39),
    DRY(40, 59),
    VERY_DRY(60, 79),
    EXTREME_DRY(80, 100);

    fun rangeLabel(): String = "$minScore–$maxScore"

    fun label(language: AppLanguage): String = when (this) {
        MOIST -> when (language) {
            AppLanguage.JAPANESE -> "しっとり"
            AppLanguage.KOREAN -> "촉촉함"
            AppLanguage.SIMPLIFIED_CHINESE -> "湿润"
            AppLanguage.TRADITIONAL_CHINESE -> "濕潤"
            AppLanguage.ENGLISH -> "Moist"
            AppLanguage.SPANISH -> "Húmedo"
            AppLanguage.FRENCH -> "Humide"
            AppLanguage.PORTUGUESE -> "Úmido"
        }
        SLIGHTLY_DRY -> when (language) {
            AppLanguage.JAPANESE -> "やや乾燥"
            AppLanguage.KOREAN -> "약간 건조"
            AppLanguage.SIMPLIFIED_CHINESE -> "略干"
            AppLanguage.TRADITIONAL_CHINESE -> "略乾"
            AppLanguage.ENGLISH -> "Slightly Dry"
            AppLanguage.SPANISH -> "Algo seco"
            AppLanguage.FRENCH -> "Légèrement sec"
            AppLanguage.PORTUGUESE -> "Levemente seco"
        }
        DRY -> when (language) {
            AppLanguage.JAPANESE -> "乾燥"
            AppLanguage.KOREAN -> "건조"
            AppLanguage.SIMPLIFIED_CHINESE -> "干燥"
            AppLanguage.TRADITIONAL_CHINESE -> "乾燥"
            AppLanguage.ENGLISH -> "Dry"
            AppLanguage.SPANISH -> "Seco"
            AppLanguage.FRENCH -> "Sec"
            AppLanguage.PORTUGUESE -> "Seco"
        }
        VERY_DRY -> when (language) {
            AppLanguage.JAPANESE -> "カラカラ"
            AppLanguage.KOREAN -> "매우 건조"
            AppLanguage.SIMPLIFIED_CHINESE -> "很干燥"
            AppLanguage.TRADITIONAL_CHINESE -> "很乾燥"
            AppLanguage.ENGLISH -> "Very Dry"
            AppLanguage.SPANISH -> "Muy seco"
            AppLanguage.FRENCH -> "Très sec"
            AppLanguage.PORTUGUESE -> "Muito seco"
        }
        EXTREME_DRY -> when (language) {
            AppLanguage.JAPANESE -> "超カラカラ"
            AppLanguage.KOREAN -> "극심한 건조"
            AppLanguage.SIMPLIFIED_CHINESE -> "极度干燥"
            AppLanguage.TRADITIONAL_CHINESE -> "極度乾燥"
            AppLanguage.ENGLISH -> "Extremely Dry"
            AppLanguage.SPANISH -> "Extremadamente seco"
            AppLanguage.FRENCH -> "Extrêmement sec"
            AppLanguage.PORTUGUESE -> "Extremamente seco"
        }
    }

    fun guidance(language: AppLanguage): String = when (this) {
        MOIST -> when (language) {
            AppLanguage.JAPANESE -> "十分な潤いがあり乾燥対策は不要"
            AppLanguage.KOREAN -> "충분한 수분이 있어 별도의 건조 대책이 필요 없습니다"
            AppLanguage.SIMPLIFIED_CHINESE -> "空气湿润充足，无需特别防干燥措施"
            AppLanguage.TRADITIONAL_CHINESE -> "空氣濕潤充足，無需特別防乾燥措施"
            AppLanguage.ENGLISH -> "Plenty of moisture in the air; no dryness care needed"
            AppLanguage.SPANISH -> "Humedad suficiente; no requiere medidas contra la sequedad"
            AppLanguage.FRENCH -> "Humidité suffisante ; aucune mesure anti-sécheresse nécessaire"
            AppLanguage.PORTUGUESE -> "Umidade suficiente; não é necessário cuidado contra ressecamento"
        }
        SLIGHTLY_DRY -> when (language) {
            AppLanguage.JAPANESE -> "概ね快適だが敏感肌は保湿を意識したい"
            AppLanguage.KOREAN -> "대체로 쾌적하지만 민감한 피부는 보습을 신경 쓰세요"
            AppLanguage.SIMPLIFIED_CHINESE -> "总体较舒适，但敏感肌建议适当保湿"
            AppLanguage.TRADITIONAL_CHINESE -> "整體較舒適，但敏感肌建議適當保濕"
            AppLanguage.ENGLISH -> "Mostly comfortable, though sensitive skin benefits from moisturizer"
            AppLanguage.SPANISH -> "Generalmente cómodo, aunque la piel sensible agradecerá hidratación"
            AppLanguage.FRENCH -> "Globalement confortable, mais les peaux sensibles gagnent à s'hydrater"
            AppLanguage.PORTUGUESE -> "Geralmente confortável, mas peles sensíveis se beneficiam de hidratação"
        }
        DRY -> when (language) {
            AppLanguage.JAPANESE -> "肌や喉の乾きを感じ始める標準的な注意域"
            AppLanguage.KOREAN -> "피부와 목의 건조함이 느껴지기 시작하는 주의 단계입니다"
            AppLanguage.SIMPLIFIED_CHINESE -> "皮肤和喉咙开始感到干燥，建议开始注意保湿"
            AppLanguage.TRADITIONAL_CHINESE -> "皮膚和喉嚨開始感到乾燥，建議開始注意保濕"
            AppLanguage.ENGLISH -> "Noticeable dryness for skin and throat; start moisturizing and hydrating"
            AppLanguage.SPANISH -> "Nivel de atención: la piel y la garganta comienzan a sentirse secas"
            AppLanguage.FRENCH -> "Seuil de vigilance : la peau et la gorge commencent à s'assécher"
            AppLanguage.PORTUGUESE -> "Faixa de atenção: pele e garganta começam a ressecar"
        }
        VERY_DRY -> when (language) {
            AppLanguage.JAPANESE -> "肌荒れや感染症リスクが高く加湿と保湿が必要"
            AppLanguage.KOREAN -> "피부 트러블과 감염 위험이 높아 가습과 보습이 필요합니다"
            AppLanguage.SIMPLIFIED_CHINESE -> "皮肤易干裂且呼吸道风险升高，需要加湿与保湿"
            AppLanguage.TRADITIONAL_CHINESE -> "皮膚易乾裂且呼吸道風險升高，需要加濕與保濕"
            AppLanguage.ENGLISH -> "High risk of dry skin and irritation; humidification and moisturizing needed"
            AppLanguage.SPANISH -> "Alto riesgo de irritación y piel seca; se recomienda humidificar e hidratar"
            AppLanguage.FRENCH -> "Risque élevé de sécheresse cutanée et d'irritation ; humidifiez et hydratez"
            AppLanguage.PORTUGUESE -> "Alto risco de pele ressecada e irritação; umidifique e hidrate"
        }
        EXTREME_DRY -> when (language) {
            AppLanguage.JAPANESE -> "真冬の強風・低湿などの極端な乾燥状態"
            AppLanguage.KOREAN -> "한겨울 강풍·저습 등 극심한 건조 상태입니다"
            AppLanguage.SIMPLIFIED_CHINESE -> "寒冬强风或极低湿度导致的极度干燥状态"
            AppLanguage.TRADITIONAL_CHINESE -> "寒冬強風或極低濕度導致的極度乾燥狀態"
            AppLanguage.ENGLISH -> "Extreme dryness from frigid air, strong wind, or very low humidity"
            AppLanguage.SPANISH -> "Sequedad extrema por aire frío, viento fuerte o humedad muy baja"
            AppLanguage.FRENCH -> "Sécheresse extrême liée à l'air sec, au vent fort ou à une très faible humidité"
            AppLanguage.PORTUGUESE -> "Secura extrema causada por ar seco, vento forte ou umidade muito baixa"
        }
    }

    companion object {
        fun fromScore(score: Int): DrynessRank = when {
            score < 20 -> MOIST
            score < 40 -> SLIGHTLY_DRY
            score < 60 -> DRY
            score < 80 -> VERY_DRY
            else -> EXTREME_DRY
        }
    }
}

/**
 * 乾燥スコアの算出結果と内訳。
 */
data class DrynessIndexResult(
    val score: Int,
    val rank: DrynessRank,
    val volumetricHumidityGm3: Double,
    val baseScore: Double,
    val windCorrection: Double,
    val rhCorrection: Double
)

/**
 * 容積絶対湿度（最大 80 点）＋ 風速補正 ＋ 相対湿度補正による 0〜100 乾燥スコア計算エンジン。
 *
 * 1. 飽和水蒸気圧: e_s = 6.1078 * 10^(7.5 * T / (T + 237.3))
 * 2. 容積絶対湿度: VH = (217 * e_s / (T + 273.15)) * (RH / 100) [g/m^3]
 * 3. 基礎乾燥スコア: S_base = clamp((13.0 - VH) / 11.0 * 80, 0, 80)
 * 4. 風速補正: C_wind = min(max(W - 1.0, 0), 9.0) * 2.0 * (1 - RH / 100)
 * 5. 相対湿度補正:
 *      RH < 40  -> (40 - RH) * 0.5
 *      RH > 65  -> -(RH - 65) * 1.0
 *      otherwise -> 0
 * 6. 最終スコア: round(clamp(S_base + C_wind + C_rh, 0, 100))
 */
object DrynessIndexCalculator {

    private const val VH_ZERO_SCORE = 13.0
    private const val VH_RANGE = 11.0 // 13.0 - 2.0
    private const val BASE_SCORE_MAX = 80.0
    private const val WIND_START_MS = 1.0
    private const val WIND_EFFECTIVE_MAX_MS = 9.0 // up to 10.0 m/s
    private const val WIND_COEFF = 2.0
    private const val RH_LOW_THRESHOLD = 40.0
    private const val RH_HIGH_THRESHOLD = 65.0

    fun calculate(
        tempC: Double,
        relativeHumidity: Double,
        windSpeedMs: Double
    ): DrynessIndexResult {
        val safeTempC = if (tempC.isFinite()) tempC.coerceIn(-80.0, 70.0) else 20.0
        val safeRh = if (relativeHumidity.isFinite()) relativeHumidity.coerceIn(0.0, 100.0) else 50.0
        val safeWindMs = if (windSpeedMs.isFinite()) windSpeedMs.coerceAtLeast(0.0) else 0.0

        val saturationVaporPressureHpa = 6.1078 * 10.0.pow((7.5 * safeTempC) / (safeTempC + 237.3))
        val vh = (217.0 * saturationVaporPressureHpa / (safeTempC + 273.15)) * (safeRh / 100.0)

        val baseScore = (((VH_ZERO_SCORE - vh) / VH_RANGE) * BASE_SCORE_MAX).coerceIn(0.0, BASE_SCORE_MAX)

        val effectiveWind = (safeWindMs - WIND_START_MS).coerceIn(0.0, WIND_EFFECTIVE_MAX_MS)
        val windCorrection = effectiveWind * WIND_COEFF * (1.0 - safeRh / 100.0)

        val rhCorrection = when {
            safeRh < RH_LOW_THRESHOLD -> (RH_LOW_THRESHOLD - safeRh) * 0.5
            safeRh > RH_HIGH_THRESHOLD -> -(safeRh - RH_HIGH_THRESHOLD) * 1.0
            else -> 0.0
        }

        val clampedTotal = (baseScore + windCorrection + rhCorrection).coerceIn(0.0, 100.0)
        val score = clampedTotal.roundToInt().coerceIn(0, 100)

        return DrynessIndexResult(
            score = score,
            rank = DrynessRank.fromScore(score),
            volumetricHumidityGm3 = vh,
            baseScore = baseScore,
            windCorrection = windCorrection,
            rhCorrection = rhCorrection
        )
    }

    fun calculateFromKmH(
        tempC: Double,
        relativeHumidity: Double,
        windSpeedKmH: Double
    ): DrynessIndexResult {
        val safeKmH = if (windSpeedKmH.isFinite()) windSpeedKmH.coerceAtLeast(0.0) else 0.0
        return calculate(
            tempC = tempC,
            relativeHumidity = relativeHumidity,
            windSpeedMs = safeKmH / 3.6
        )
    }
}
