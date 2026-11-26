package io.github.nemynew.expressiveweather.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 降雨予測・接近・終了時間解析エンジン (時系列マルチティア統合スキャナー)
 *
 * 時系列に沿って異なるデータソースを走査し、雨量と確率を連続したタイムラインとして処理します。
 *
 * 1. タイムラインの構成:
 *    - 直近60分: 高解像度降水ナウキャスト 5分間隔 (予想降水量 mm/h)
 *    - 1時間後〜6時間後: 降水短時間予報 10分間隔 (予想降水量 mm/h)
 *    - 6時間後〜15時間後: 降水短時間予報 1時間間隔 (予想降水量 mm/h)
 *    - 15時間以降〜翌日: 府県天気予報 6時間ブロック (降水確率 %)
 *
 * 2. 雨量判定閾値:
 *    - 降り始め閾値: 1.0 mm/h 以上
 *    - 降り止み閾値: 0.1 mm/h 以下
 *    - 強雨判定閾値: 10.0 mm/h 以上
 *    - 激しい雨閾値: 30.0 mm/h 以上
 *    - 確定条件（ノイズ除外）: 2フレーム連続 0.8 mm/h 以上
 *
 * 3. 状態遷移とヒステリシス:
 *    - DRY: 現在雨量 < 1.0 mm/h
 *    - RAINING: 現在雨量 >= 1.0 mm/h
 */
object PrecipitationAnalyzer {

    // 雨量判定閾値 (mm/h)
    const val THRESHOLD_RAIN_START = 1.0     // 降り始め閾値 (傘が必要になる明らかな雨粒)
    const val THRESHOLD_RAIN_STOP = 0.1      // 降り止み閾値 (0.1 mm/h 以下で雨が止んだと判定)
    const val THRESHOLD_HEAVY_RAIN = 10.0    // 強雨判定閾値 (傘を差していても濡れる本降りの基準)
    const val THRESHOLD_VIOLENT_RAIN = 30.0  // 激しい雨閾値 (バケツをひっくり返したような豪雨の基準)
    const val THRESHOLD_CONFIRM_RAIN = 0.8   // 確定条件 (ノイズ除外用: 2フレーム連続 0.8 mm/h 以上)

    private val isoFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        }
    }

    enum class TimelineTier {
        TIER1_NOWCAST_5MIN,        // 0..60分: 高解像度降水ナウキャスト 5分間隔
        TIER2_SHORT_FORECAST_10MIN,// 1h..6h: 降水短時間予報 10分間隔
        TIER3_SHORT_FORECAST_1H,   // 6h..15h: 降水短時間予報 1時間間隔
        TIER4_PREFECTURAL_6H       // 15h..翌日: 府県天気予報 6時間ブロック
    }

    data class TimelineFrame(
        val timestampMillis: Long,
        val minutesFromNow: Int,
        val precipMmPerHour: Double,
        val precipProbability: Int,
        val tier: TimelineTier,
        val timeLabel: String
    )

    /**
     * マルチティア連続タイムラインを構築して総合解析
     */
    fun analyze(
        currentPrecipMm: Double,
        currentCondition: WeatherCondition,
        minutelyTimes: List<String>?,
        minutelyPrecips: List<Double>?,
        minutelyCodes: List<Int>?,
        hourlyForecast: List<HourlyForecast>,
        jmaOverview: String? = null,
        popSlots: List<PopSlot>? = null,
        nowMillis: Long = System.currentTimeMillis(),
        country: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        conditionName: String? = null,
        jmaMinutelySeries: List<Pair<Long, Double>>? = null,
        timeZone: TimeZone? = null
    ): PrecipitationNowcast {
        val isJapan = if (latitude != null && longitude != null) {
            isLocationOrCountryInJapan(country, latitude, longitude)
        } else if (jmaOverview != null) {
            true
        } else {
            val c = country?.trim() ?: ""
            c.contains("日本") || c.contains("Japan", ignoreCase = true) || c.equals("JP", ignoreCase = true) || c.equals("JPN", ignoreCase = true)
        }

        val effectiveTimeZone = timeZone ?: if (isJapan) TimeZone.getTimeZone("Asia/Tokyo") else TimeZone.getDefault()

        val isUS = if (latitude != null && longitude != null) {
            isLocationOrCountryInUnitedStates(country, latitude, longitude)
        } else {
            val c = country?.trim() ?: ""
            c.contains("アメリカ") || c.contains("米国") || c.contains("United States", ignoreCase = true) || c.equals("US", ignoreCase = true) || c.equals("USA", ignoreCase = true)
        }

        val isEurope = if (latitude != null && longitude != null) {
            isLocationOrCountryInEurope(country, latitude, longitude)
        } else {
            val c = country?.trim() ?: ""
            val lower = c.lowercase()
            listOf("france", "フランス", "germany", "ドイツ", "uk", "イギリス", "italy", "イタリア", "spain", "スペイン", "europe", "ヨーロッパ").any { lower.contains(it) || c.contains(it) }
        }

        // 日本の場合は Open-Meteo 等の外部ソース情報を完全にシャットアウト
        val effectiveMinutelyTimes = if (isJapan) null else minutelyTimes
        val effectiveMinutelyPrecips = if (isJapan) null else minutelyPrecips

        // 1. タイムラインの構築
        val timeline = buildContinuousTimeline(
            nowMillis = nowMillis,
            currentPrecipMm = currentPrecipMm,
            currentCondition = currentCondition,
            minutelyTimes = effectiveMinutelyTimes,
            minutelyPrecips = effectiveMinutelyPrecips,
            hourlyForecast = hourlyForecast,
            popSlots = popSlots,
            jmaMinutelySeries = jmaMinutelySeries,
            timeZone = effectiveTimeZone
        )

        // MinutelyRainPoint のリスト (下位互換およびUI描画用)
        val minutelyPoints = timeline.filter {
            it.tier == TimelineTier.TIER1_NOWCAST_5MIN || it.tier == TimelineTier.TIER2_SHORT_FORECAST_10MIN
        }.map {
            MinutelyRainPoint(
                timeLabel = it.timeLabel,
                timestampMillis = it.timestampMillis,
                precipMm = it.precipMmPerHour,
                probability = it.precipProbability
            )
        }

        // 2. 現在の降雨状態 (実効雨量) の算出
        val firstFramePrecip = timeline.firstOrNull()?.precipMmPerHour ?: 0.0
        val isRainByCondition = isConditionRain(currentCondition, currentPrecipMm)
        val effectiveCurrentRain = if (firstFramePrecip > 0.0 || currentPrecipMm > 0.0) {
            maxOf(currentPrecipMm.coerceAtLeast(0.0), firstFramePrecip)
        } else if (isRainByCondition && effectiveMinutelyPrecips.isNullOrEmpty() && jmaMinutelySeries.isNullOrEmpty()) {
            // 分単位データがない地域で天気が雨の場合のフォールバック
            1.5
        } else {
            0.0
        }

        // 状態 DRY vs RAINING の判定
        val isRaining = effectiveCurrentRain >= THRESHOLD_RAIN_START

        val futurePrecipFrames = timeline.filter {
            it.timestampMillis > nowMillis && it.tier != TimelineTier.TIER4_PREFECTURAL_6H
        }
        val tier4Blocks = timeline.filter { it.tier == TimelineTier.TIER4_PREFECTURAL_6H }

        return if (isRaining) {
            analyzeRainingState(
                nowMillis = nowMillis,
                effectiveCurrentRain = effectiveCurrentRain,
                futureFrames = futurePrecipFrames,
                minutelyPoints = minutelyPoints,
                isJapan = isJapan,
                isUS = isUS,
                isEurope = isEurope,
                timeZone = effectiveTimeZone
            )
        } else {
            analyzeDryState(
                nowMillis = nowMillis,
                futureFrames = futurePrecipFrames,
                tier4Blocks = tier4Blocks,
                minutelyPoints = minutelyPoints,
                jmaOverview = jmaOverview,
                isJapan = isJapan,
                isUS = isUS,
                isEurope = isEurope,
                timeZone = effectiveTimeZone
            )
        }
    }

    /**
     * 状態 DRY (現在雨が降っていない時: effectiveCurrentRain < 1.0 mm/h)
     */
    private fun analyzeDryState(
        nowMillis: Long,
        futureFrames: List<TimelineFrame>,
        tier4Blocks: List<TimelineFrame>,
        minutelyPoints: List<MinutelyRainPoint>,
        jmaOverview: String?,
        isJapan: Boolean,
        isUS: Boolean,
        isEurope: Boolean = false,
        timeZone: TimeZone = TimeZone.getDefault()
    ): PrecipitationNowcast {
        // 未来のタイムラインを先頭から順に走査し、雨量が 1.0 mm/h 以上となった最初の時刻を探索
        // ノイズ除外条件: 2フレーム連続で 0.8 mm/h 以上が続くこと
        var confirmedStartFrame: TimelineFrame? = null
        var confirmedStartIndex = -1

        for (i in futureFrames.indices) {
            val frame = futureFrames[i]
            if (frame.precipMmPerHour >= THRESHOLD_RAIN_START) {
                val nextFrame = futureFrames.getOrNull(i + 1)
                val isConfirmed = if (nextFrame != null) {
                    frame.precipMmPerHour >= THRESHOLD_CONFIRM_RAIN &&
                            nextFrame.precipMmPerHour >= THRESHOLD_CONFIRM_RAIN
                } else {
                    // Tiers 1-3の最終フレームの場合、前フレームが 0.8mm以上またはTier4に降水があること
                    (futureFrames.getOrNull(i - 1)?.precipMmPerHour ?: 0.0) >= THRESHOLD_CONFIRM_RAIN ||
                            tier4Blocks.any { it.precipProbability >= 50 }
                }

                if (isConfirmed) {
                    confirmedStartFrame = frame
                    confirmedStartIndex = i
                    break
                }
            }
        }

        // =====================================================================
        // パターン A: 15時間以内に雨が降り始める場合 (Tier 1, Tier 2, Tier 3)
        // =====================================================================
        if (confirmedStartFrame != null) {
            val startTime = confirmedStartFrame.timestampMillis
            val diffMinutes = ((startTime - nowMillis) / (60 * 1000L)).toInt().coerceAtLeast(1)

            // この降水イベントの最大予想降水量 (0.1mm/h 以下に落ちるまでのピーク)
            val maxPrecipInEvent = futureFrames.subList(confirmedStartIndex, futureFrames.size)
                .takeWhile { it.precipMmPerHour > (THRESHOLD_RAIN_STOP + 0.0001) }
                .maxOfOrNull { it.precipMmPerHour } ?: confirmedStartFrame.precipMmPerHour

            val pop = confirmedStartFrame.precipProbability.coerceAtLeast(60)

            return when {
                // 1. 直近60分以内に降り始める場合
                diffMinutes <= 60 -> {
                    val isHeavy = maxPrecipInEvent >= THRESHOLD_HEAVY_RAIN
                    val timeLabelJa = formatTo5MinTime(startTime, AppLanguage.JAPANESE, timeZone)
                    val timeLabelKo = formatTo5MinTime(startTime, AppLanguage.KOREAN, timeZone)
                    val timeLabelEn = formatTo5MinTime(startTime, AppLanguage.ENGLISH, timeZone)
                    val timeLabelZhCn = formatTo5MinTime(startTime, AppLanguage.SIMPLIFIED_CHINESE, timeZone)
                    val timeLabelZhTw = formatTo5MinTime(startTime, AppLanguage.TRADITIONAL_CHINESE, timeZone)
                    val timeLabelEs = formatTo5MinTime(startTime, AppLanguage.SPANISH, timeZone)
                    val timeLabelFr = formatTo5MinTime(startTime, AppLanguage.FRENCH, timeZone)
                    val timeLabelPt = formatTo5MinTime(startTime, AppLanguage.PORTUGUESE, timeZone)
                    val timeLabelRu = formatTo5MinTime(startTime, AppLanguage.RUSSIAN, timeZone)

                    val jaHeadline = if (diffMinutes < 15) {
                        if (isHeavy) "まもなく激しい雨が降ります" else "まもなく雨が降り始めます"
                    } else {
                        if (isHeavy) "${timeLabelJa}頃から激しい雨が降ります" else "${timeLabelJa}頃から雨が降り始めます"
                    }
                    val koHeadline = if (diffMinutes < 15) {
                        if (isHeavy) "곧 강한 비가 내릴 전망입니다" else "곧 비가 내리기 시작합니다"
                    } else {
                        if (isHeavy) "${timeLabelKo}경부터 강한 비가 내릴 전망입니다" else "${timeLabelKo}경부터 비가 내리기 시작합니다"
                    }
                    val enHeadline = if (diffMinutes < 15) {
                        if (isHeavy) "Heavy rain starting shortly" else "Rain starting shortly"
                    } else {
                        if (isHeavy) "Heavy rain starting around $timeLabelEn" else "Rain starting around $timeLabelEn"
                    }
                    val zhCnHeadline = if (diffMinutes < 15) {
                        if (isHeavy) "即将下大雨" else "即将开始下雨"
                    } else {
                        if (isHeavy) "预计 $timeLabelZhCn 左右将下大雨" else "预计 $timeLabelZhCn 左右开始下雨"
                    }
                    val zhTwHeadline = if (diffMinutes < 15) {
                        if (isHeavy) "即將下大雨" else "即將開始下雨"
                    } else {
                        if (isHeavy) "預計 $timeLabelZhTw 左右將下大雨" else "預計 $timeLabelZhTw 左右開始下雨"
                    }
                    val esHeadline = if (diffMinutes < 15) {
                        if (isHeavy) "Lluvia intensa en breve" else "Empezará a llover en breve"
                    } else {
                        if (isHeavy) "Lluvia intensa a partir de las $timeLabelEs aprox." else "Empezará a llover sobre las $timeLabelEs"
                    }
                    val frHeadline = if (diffMinutes < 15) {
                        if (isHeavy) "Forte pluie imminente" else "La pluie va bientôt commencer"
                    } else {
                        if (isHeavy) "Forte pluie prévue vers $timeLabelFr" else "Début de la pluie vers $timeLabelFr"
                    }
                    val ptHeadline = if (diffMinutes < 15) {
                        if (isHeavy) "Chuva forte começando em breve" else "Vai começar a chover em breve"
                    } else {
                        if (isHeavy) "Chuva forte começando por volta das $timeLabelPt" else "Chuva começando por volta das $timeLabelPt"
                    }
                    val ruHeadline = if (diffMinutes < 15) {
                        if (isHeavy) "Скоро начнётся сильный дождь" else "Скоро начнётся дождь"
                    } else {
                        if (isHeavy) "Сильный дождь начнётся около $timeLabelRu" else "Дождь начнётся около $timeLabelRu"
                    }

                    val jaSubText = if (isHeavy) {
                        "強い雨雲が迫っています（予想雨量 約${formatPrecipMm(maxPrecipInEvent)}mm/h）。安全な屋内へ避難してください。"
                    } else if (diffMinutes < 15) {
                        "あと約${diffMinutes}分で雨が降り始める見込みです。お出かけの際は傘をご用意ください。"
                    } else {
                        "あと約${diffMinutes}分で雨が降り始める見込みです。折りたたみ傘があると安心です。"
                    }
                    val koSubText = if (isHeavy) {
                        "강한 비구름이 다가오고 있습니다(예상 강수량 약 ${formatPrecipMm(maxPrecipInEvent)} mm/h). 안전한 실내로 대피하세요."
                    } else if (diffMinutes < 15) {
                        "약 ${diffMinutes}분 후 비가 내리기 시작할 전망입니다. 외출 시 우산을 준비하세요."
                    } else {
                        "약 ${diffMinutes}분 후 비가 내리기 시작할 전망입니다. 접이식 우산을 챙기면 좋습니다."
                    }
                    val enSubText = if (isHeavy) {
                        "Intense rain clouds approaching (est. ~${formatPrecipMm(maxPrecipInEvent)} mm/h). Seek shelter indoors."
                    } else if (diffMinutes < 15) {
                        "Rain is expected in about ${diffMinutes} min. Have an umbrella ready."
                    } else {
                        "Rain is expected in about ${diffMinutes} min. An umbrella is recommended."
                    }
                    val zhCnSubText = if (isHeavy) {
                        "强降雨云正在靠近（预计雨量约 ${formatPrecipMm(maxPrecipInEvent)} mm/h），请及时避雨。"
                    } else if (diffMinutes < 15) {
                        "预计约 ${diffMinutes} 分钟后开始下雨，出门请备好雨具。"
                    } else {
                        "距降雨约 ${diffMinutes} 分钟，出门建议携带雨具。"
                    }
                    val zhTwSubText = if (isHeavy) {
                        "強降雨雲正在靠近（預計雨量約 ${formatPrecipMm(maxPrecipInEvent)} mm/h），請及時避雨。"
                    } else if (diffMinutes < 15) {
                        "預計約 ${diffMinutes} 分鐘後開始下雨，出門請備好雨具。"
                    } else {
                        "距降雨約 ${diffMinutes} 分鐘，出門建議攜帶雨具。"
                    }
                    val esSubText = if (isHeavy) {
                        "Se acercan nubes de lluvia intensa (aprox. ${formatPrecipMm(maxPrecipInEvent)} mm/h). Busca refugio en interiores."
                    } else if (diffMinutes < 15) {
                        "Se espera lluvia en unos ${diffMinutes} min. Ten un paraguas a mano."
                    } else {
                        "Se espera lluvia en unos ${diffMinutes} min. Se recomienda llevar paraguas."
                    }
                    val frSubText = if (isHeavy) {
                        "Nuages de forte pluie en approche (env. ${formatPrecipMm(maxPrecipInEvent)} mm/h). Mettez-vous à l'abri."
                    } else if (diffMinutes < 15) {
                        "Pluie attendue dans environ ${diffMinutes} min. Préparez votre parapluie."
                    } else {
                        "Pluie attendue dans environ ${diffMinutes} min. Un parapluie est conseillé."
                    }
                    val ptSubText = if (isHeavy) {
                        "Nuvens de chuva forte se aproximando (aprox. ${formatPrecipMm(maxPrecipInEvent)} mm/h). Abrigue-se em local seguro."
                    } else if (diffMinutes < 15) {
                        "Previsão de chuva em cerca de ${diffMinutes} min. Deixe o guarda-chuva pronto."
                    } else {
                        "Previsão de chuva em cerca de ${diffMinutes} min. Recomenda-se levar guarda-chuva."
                    }
                    val ruSubText = if (isHeavy) {
                        "Приближаются тучи сильного дождя (около ${formatPrecipMm(maxPrecipInEvent)} мм/ч). Укройтесь в помещении."
                    } else if (diffMinutes < 15) {
                        "Дождь ожидается примерно через ${diffMinutes} мин. Держите зонт под рукой."
                    } else {
                        "Дождь ожидается примерно через ${diffMinutes} мин. Рекомендуется взять зонт."
                    }

                    val multilingual = createMultilingualTexts(
                        jaHeadline = jaHeadline,
                        koHeadline = koHeadline,
                        enHeadline = enHeadline,
                        zhCnHeadline = zhCnHeadline,
                        zhTwHeadline = zhTwHeadline,
                        esHeadline = esHeadline,
                        frHeadline = frHeadline,
                        ptHeadline = ptHeadline,
                        ruHeadline = ruHeadline,
                        jaSubText = jaSubText,
                        koSubText = koSubText,
                        enSubText = enSubText,
                        zhCnSubText = zhCnSubText,
                        zhTwSubText = zhTwSubText,
                        esSubText = esSubText,
                        frSubText = frSubText,
                        ptSubText = ptSubText,
                        ruSubText = ruSubText,
                        jaTimeLabel = timeLabelJa,
                        koTimeLabel = timeLabelKo,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw,
                        esTimeLabel = timeLabelEs,
                        frTimeLabel = timeLabelFr,
                        ptTimeLabel = timeLabelPt,
                        ruTimeLabel = timeLabelRu
                    )

                    buildNowcast(
                        status = RainStatus.RAIN_APPROACHING,
                        targetTimestampMillis = startTime,
                        minutesRemaining = diffMinutes,
                        currentIntensityMm = 0.0,
                        peakIntensityMm = maxPrecipInEvent,
                        maxProbability = pop,
                        minutelyPoints = minutelyPoints,
                        isLiveActive = true,
                        isPeak = false,
                        isJapan = isJapan,
                        isUS = isUS,
                        isEurope = isEurope,
                        multilingual = multilingual
                    )
                }

                // 2. 1時間後から6時間後までに降り始める場合 (10分単位の刻み)
                diffMinutes <= 360 -> {
                    val timeLabelJa = roundToNearest10Min(startTime, AppLanguage.JAPANESE, timeZone)
                    val timeLabelKo = roundToNearest10Min(startTime, AppLanguage.KOREAN, timeZone)
                    val timeLabelEn = roundToNearest10Min(startTime, AppLanguage.ENGLISH, timeZone)
                    val timeLabelZhCn = roundToNearest10Min(startTime, AppLanguage.SIMPLIFIED_CHINESE, timeZone)
                    val timeLabelZhTw = roundToNearest10Min(startTime, AppLanguage.TRADITIONAL_CHINESE, timeZone)
                    val timeLabelEs = roundToNearest10Min(startTime, AppLanguage.SPANISH, timeZone)
                    val timeLabelFr = roundToNearest10Min(startTime, AppLanguage.FRENCH, timeZone)
                    val timeLabelPt = roundToNearest10Min(startTime, AppLanguage.PORTUGUESE, timeZone)
                    val timeLabelRu = roundToNearest10Min(startTime, AppLanguage.RUSSIAN, timeZone)

                    val multilingual = createMultilingualTexts(
                        jaHeadline = "${timeLabelJa}頃から雨が降り始める見込みです",
                        koHeadline = "${timeLabelKo}경부터 비가 내릴 전망입니다",
                        enHeadline = "Rain expected around $timeLabelEn",
                        zhCnHeadline = "预计 $timeLabelZhCn 左右开始下雨",
                        zhTwHeadline = "預計 $timeLabelZhTw 左右開始下雨",
                        esHeadline = "Se espera lluvia sobre las $timeLabelEs",
                        frHeadline = "Pluie attendue vers $timeLabelFr",
                        ptHeadline = "Previsão de chuva por volta das $timeLabelPt",
                        ruHeadline = "Дождь ожидается около $timeLabelRu",
                        jaSubText = "${timeLabelJa}頃から雨が降り始める予想です（予想雨量 約${formatPrecipMm(maxPrecipInEvent)}mm/h）。お出かけの際は雨具をお持ちください。",
                        koSubText = "${timeLabelKo}경부터 비가 내릴 예상입니다(예상 강수량 약 ${formatPrecipMm(maxPrecipInEvent)} mm/h). 외출 시 우산을 챙기세요.",
                        enSubText = "Rain expected around $timeLabelEn (~${formatPrecipMm(maxPrecipInEvent)} mm/h). Bring an umbrella if heading out.",
                        zhCnSubText = "预计 $timeLabelZhCn 左右开始降雨（预计约 ${formatPrecipMm(maxPrecipInEvent)} mm/h），外出请带伞。",
                        zhTwSubText = "預計 $timeLabelZhTw 左右開始降雨（預計約 ${formatPrecipMm(maxPrecipInEvent)} mm/h），外出請帶傘。",
                        esSubText = "Se espera lluvia sobre las $timeLabelEs (aprox. ${formatPrecipMm(maxPrecipInEvent)} mm/h). Lleva paraguas si vas a salir.",
                        frSubText = "Pluie attendue vers $timeLabelFr (env. ${formatPrecipMm(maxPrecipInEvent)} mm/h). Emportez un parapluie si vous sortez.",
                        ptSubText = "Previsão de chuva por volta das $timeLabelPt (aprox. ${formatPrecipMm(maxPrecipInEvent)} mm/h). Leve guarda-chuva se for sair.",
                        ruSubText = "Дождь ожидается около $timeLabelRu (около ${formatPrecipMm(maxPrecipInEvent)} мм/ч). Возьмите зонт, если выходите.",
                        jaTimeLabel = timeLabelJa,
                        koTimeLabel = timeLabelKo,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw,
                        esTimeLabel = timeLabelEs,
                        frTimeLabel = timeLabelFr,
                        ptTimeLabel = timeLabelPt,
                        ruTimeLabel = timeLabelRu
                    )

                    buildNowcast(
                        status = RainStatus.RAIN_APPROACHING,
                        targetTimestampMillis = startTime,
                        minutesRemaining = diffMinutes,
                        currentIntensityMm = 0.0,
                        peakIntensityMm = maxPrecipInEvent,
                        maxProbability = pop,
                        minutelyPoints = minutelyPoints,
                        isLiveActive = (diffMinutes <= 120),
                        isPeak = false,
                        isJapan = isJapan,
                        isUS = isUS,
                        isEurope = isEurope,
                        multilingual = multilingual
                    )
                }

                // 3. 6時間後から15時間後までに降り始める場合 (1時間単位の丸め)
                else -> {
                    val roundedHourJa = roundToNearestHour(startTime, AppLanguage.JAPANESE, timeZone)
                    val roundedHourKo = roundToNearestHour(startTime, AppLanguage.KOREAN, timeZone)
                    val roundedHourEn = roundToNearestHour(startTime, AppLanguage.ENGLISH, timeZone)
                    val roundedHourZhCn = roundToNearestHour(startTime, AppLanguage.SIMPLIFIED_CHINESE, timeZone)
                    val roundedHourZhTw = roundToNearestHour(startTime, AppLanguage.TRADITIONAL_CHINESE, timeZone)
                    val roundedHourEs = roundToNearestHour(startTime, AppLanguage.SPANISH, timeZone)
                    val roundedHourFr = roundToNearestHour(startTime, AppLanguage.FRENCH, timeZone)
                    val roundedHourPt = roundToNearestHour(startTime, AppLanguage.PORTUGUESE, timeZone)
                    val roundedHourRu = roundToNearestHour(startTime, AppLanguage.RUSSIAN, timeZone)

                    val multilingual = createMultilingualTexts(
                        jaHeadline = "${roundedHourJa}頃から雨が降り出す見込みです",
                        koHeadline = "${roundedHourKo}경부터 비가 올 전망입니다",
                        enHeadline = "Rain expected around $roundedHourEn",
                        zhCnHeadline = "预计 $roundedHourZhCn 左右开始下雨",
                        zhTwHeadline = "預計 $roundedHourZhTw 左右開始下雨",
                        esHeadline = "Se espera lluvia sobre las $roundedHourEs",
                        frHeadline = "Pluie attendue vers $roundedHourFr",
                        ptHeadline = "Previsão de chuva por volta das $roundedHourPt",
                        ruHeadline = "Дождь ожидается около $roundedHourRu",
                        jaSubText = "今夜にかけてまとまった雨雲が広がる予想です（降水確率${pop}%）。夜間のお帰りの際は傘をお持ちください。",
                        koSubText = "오늘 밤 비구름이 확대될 전망입니다(강수확률 ${pop}%). 귀가 시 우산을 챙기세요.",
                        enSubText = "Rain clouds expected tonight (${pop}% chance). Don't forget an umbrella for tonight.",
                        zhCnSubText = "今晚可能有降雨（降水概率 ${pop}%），晚归请备好雨具。",
                        zhTwSubText = "今晚可能有降雨（降水機率 ${pop}%），晚歸請備好雨具。",
                        esSubText = "Se esperan nubes de lluvia esta noche (${pop}% de prob.). No olvides el paraguas.",
                        frSubText = "Nuages pluvieux attendus ce soir (${pop} % de risque). N'oubliez pas votre parapluie.",
                        ptSubText = "Nuvens de chuva previstas para esta noite (${pop}% de chance). Não esqueça o guarda-chuva.",
                        ruSubText = "Сегодня вечером ожидаются дождевые облака (вероятность ${pop}%). Не забудьте зонт.",
                        jaTimeLabel = "${roundedHourJa}頃",
                        koTimeLabel = "${roundedHourKo}경",
                        enTimeLabel = roundedHourEn,
                        zhCnTimeLabel = roundedHourZhCn,
                        zhTwTimeLabel = roundedHourZhTw,
                        esTimeLabel = roundedHourEs,
                        frTimeLabel = roundedHourFr,
                        ptTimeLabel = roundedHourPt,
                        ruTimeLabel = roundedHourRu
                    )

                    buildNowcast(
                        status = RainStatus.RAIN_EXPECTED_TONIGHT,
                        targetTimestampMillis = startTime,
                        minutesRemaining = null,
                        currentIntensityMm = 0.0,
                        peakIntensityMm = maxPrecipInEvent,
                        maxProbability = pop,
                        minutelyPoints = minutelyPoints,
                        isLiveActive = false,
                        isPeak = false,
                        isJapan = isJapan,
                        isUS = isUS,
                        isEurope = isEurope,
                        multilingual = multilingual
                    )
                }
            }
        }

        // =====================================================================
        // パターン B: 15時間以降〜翌日に降水確率50%以上がある場合 (Tier 4)
        // =====================================================================
        val rainyBlock = tier4Blocks.firstOrNull { it.precipProbability >= 50 }
        if (rainyBlock != null) {
            val (headlineJa, labelJa) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.JAPANESE, timeZone)
            val (headlineKo, labelKo) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.KOREAN, timeZone)
            val (headlineEn, labelEn) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.ENGLISH, timeZone)
            val (headlineZhCn, labelZhCn) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.SIMPLIFIED_CHINESE, timeZone)
            val (headlineZhTw, labelZhTw) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.TRADITIONAL_CHINESE, timeZone)
            val (headlineEs, labelEs) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.SPANISH, timeZone)
            val (headlineFr, labelFr) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.FRENCH, timeZone)
            val (headlinePt, labelPt) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.PORTUGUESE, timeZone)
            val (headlineRu, labelRu) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.RUSSIAN, timeZone)

            val pop = rainyBlock.precipProbability
            val sourceKo = when {
                isJapan -> "기상청 예보"
                isUS -> "NOAA/NWS 예보"
                isEurope -> "ECMWF 예보"
                else -> "글로벌 수치예보"
            }
            val (sourceJa, sourceEn, sourceZhCn, sourceZhTw) = when {
                isJapan -> listOf("気象庁予報", "JMA Forecast", "气象厅预报", "氣象廳預報")
                isUS -> listOf("NOAA/NWS予報", "NOAA/NWS Forecast", "NOAA/NWS预报", "NOAA/NWS預報")
                isEurope -> listOf("ECMWF予報", "ECMWF Forecast", "ECMWF预报", "ECMWF預報")
                else -> listOf("国際数値予報", "Global NWP Forecast", "国际数值预报", "國際數值預報")
            }
            val (sourceEs, sourceFr, sourcePt) = when {
                isJapan -> Triple("Pronóstico JMA", "Prévision JMA", "Previsão JMA")
                isUS -> Triple("Pronóstico NOAA/NWS", "Prévision NOAA/NWS", "Previsão NOAA/NWS")
                isEurope -> Triple("Pronóstico ECMWF", "Prévision ECMWF", "Previsão ECMWF")
                else -> Triple("Pronóstico global", "Prévision mondiale", "Previsão global")
            }
            val sourceRu = when {
                isJapan -> "Прогноз JMA"
                isUS -> "Прогноз NOAA/NWS"
                isEurope -> "Прогноз ECMWF"
                else -> "Глобальный прогноз"
            }

            val multilingual = createMultilingualTexts(
                jaHeadline = headlineJa,
                koHeadline = headlineKo,
                enHeadline = headlineEn,
                zhCnHeadline = headlineZhCn,
                zhTwHeadline = headlineZhTw,
                esHeadline = headlineEs,
                frHeadline = headlineFr,
                ptHeadline = headlinePt,
                ruHeadline = headlineRu,
                jaSubText = "${sourceJa}：降水確率${pop}%の雨が予想されています。明日の外出時は雨具をご準備ください。",
                koSubText = "${sourceKo}: 강수확률 ${pop}%의 비가 예상됩니다. 내일 외출 시 우산을 준비하세요.",
                enSubText = "${sourceEn}: ${pop}% chance of rain expected. Prepare rain gear for tomorrow.",
                zhCnSubText = "${sourceZhCn}：预计降水概率为 ${pop}%，明天出门请备好雨具。",
                zhTwSubText = "${sourceZhTw}：預計降水機率為 ${pop}%，明天出門請備好雨具。",
                esSubText = "${sourceEs}: ${pop}% de probabilidad de lluvia. Prepara ropa de lluvia para mañana.",
                frSubText = "${sourceFr} : ${pop} % de risque de pluie. Prévoyez de quoi vous protéger demain.",
                ptSubText = "${sourcePt}: ${pop}% de chance de chuva. Prepare capa ou guarda-chuva para amanhã.",
                ruSubText = "${sourceRu}: вероятность дождя ${pop}%. Подготовьте зонт на завтра.",
                jaTimeLabel = labelJa,
                koTimeLabel = labelKo,
                enTimeLabel = labelEn,
                zhCnTimeLabel = labelZhCn,
                zhTwTimeLabel = labelZhTw,
                esTimeLabel = labelEs,
                frTimeLabel = labelFr,
                ptTimeLabel = labelPt,
                ruTimeLabel = labelRu
            )

            return buildNowcast(
                status = RainStatus.RAIN_EXPECTED_LATER,
                targetTimestampMillis = rainyBlock.timestampMillis,
                minutesRemaining = null,
                currentIntensityMm = 0.0,
                peakIntensityMm = 0.0,
                maxProbability = pop,
                minutelyPoints = minutelyPoints,
                isLiveActive = false,
                isPeak = false,
                isJapan = isJapan,
                isUS = isUS,
                isEurope = isEurope,
                multilingual = multilingual
            )
        }

        // 日本国内の気象庁概況テキストに雨の言及がある場合の安全策
        if (isJapan && jmaOverview != null && (jmaOverview.contains("今夜は雨") || jmaOverview.contains("夜遅く雨"))) {
            val multilingual = createMultilingualTexts(
                jaHeadline = "今夜は雨が降るでしょう",
                koHeadline = "오늘 밤 비가 올 전망입니다",
                enHeadline = "Rain expected tonight",
                zhCnHeadline = "预计今晚有雨",
                zhTwHeadline = "預計今晚有雨",
                esHeadline = "Se espera lluvia esta noche",
                frHeadline = "Pluie attendue ce soir",
                ptHeadline = "Previsão de chuva hoje à noite",
                ruHeadline = "Сегодня вечером ожидается дождь",
                jaSubText = "気象庁予報：今夜遅くにかけて所により雨が降る見込みです。お帰りの際はご注意ください。",
                koSubText = "기상청 예보: 오늘 밤늦게 곳곳에 비가 내릴 전망입니다. 귀가 시 우산을 챙기세요.",
                enSubText = "JMA Forecast: Rain expected in some areas late tonight. Carry an umbrella when heading home.",
                zhCnSubText = "气象厅预报：今晚局部地区有雨，晚归请注意携带雨伞。",
                zhTwSubText = "氣象廳預報：今晚局部地區有雨，晚歸請注意攜帶雨傘。",
                esSubText = "Pronóstico JMA: Se esperan lluvias dispersas a última hora de la noche. Lleva paraguas al volver.",
                frSubText = "Prévision JMA : Pluies éparses attendues tard ce soir. Prévoyez un parapluie pour le retour.",
                ptSubText = "Previsão JMA: Chuva isolada prevista para o fim da noite. Leve guarda-chuva ao voltar para casa.",
                ruSubText = "Прогноз JMA: Поздно вечером местами ожидается дождь. Возьмите зонт на обратный путь.",
                jaTimeLabel = "今夜遅く",
                koTimeLabel = "오늘 밤늦게",
                enTimeLabel = "Late Tonight",
                zhCnTimeLabel = "今晚较晚",
                zhTwTimeLabel = "今晚較晚",
                esTimeLabel = "Esta noche tarde",
                frTimeLabel = "Tard ce soir",
                ptTimeLabel = "Fim da noite",
                ruTimeLabel = "Поздно вечером"
            )

            return buildNowcast(
                status = RainStatus.RAIN_EXPECTED_TONIGHT,
                targetTimestampMillis = null,
                minutesRemaining = null,
                currentIntensityMm = 0.0,
                peakIntensityMm = 0.0,
                maxProbability = 60,
                minutelyPoints = minutelyPoints,
                isLiveActive = false,
                isPeak = false,
                isJapan = isJapan,
                isUS = isUS,
                isEurope = isEurope,
                multilingual = multilingual
            )
        }

        // =====================================================================
        // パターン C: 全期間を通して降水なし (DRY)
        // =====================================================================
        val multilingual = createMultilingualTexts(
            jaHeadline = "今日いっぱいは雨の心配はありません",
            koHeadline = "오늘은 비 걱정이 없습니다",
            enHeadline = "No rain expected today",
            zhCnHeadline = "今天无需担心下雨",
            zhTwHeadline = "今天無須擔心下雨",
            esHeadline = "No se espera lluvia hoy",
            frHeadline = "Pas de pluie prévue aujourd'hui",
            ptHeadline = "Sem previsão de chuva hoje",
            ruHeadline = "Сегодня дождь не ожидается",
            jaSubText = "当面雨は降らない見込みです。お出かけやお洗濯にも安心です。",
            koSubText = "당분간 비 소식이 없습니다. 외출이나 빨래하기 좋은 날씨입니다.",
            enSubText = "Clear conditions ahead. Great weather for going out or laundry.",
            zhCnSubText = "预计近期无雨，适合外出与晾晒。",
            zhTwSubText = "預計近期無雨，適合外出與晾曬。",
            esSubText = "Sin lluvias a la vista. Buen tiempo para salir o tender la ropa.",
            frSubText = "Temps sec à venir. Idéal pour sortir ou étendre le linge.",
            ptSubText = "Sem chuva à vista. Ótimo tempo para sair ou estender roupas.",
            ruSubText = "Осадков не предвидится. Отличная погода для прогулок и сушки белья.",
            jaTimeLabel = null,
            koTimeLabel = null,
            enTimeLabel = null,
            zhCnTimeLabel = null,
            zhTwTimeLabel = null,
            esTimeLabel = null,
            frTimeLabel = null,
            ptTimeLabel = null,
            ruTimeLabel = null
        )

        return buildNowcast(
            status = RainStatus.DRY,
            targetTimestampMillis = null,
            minutesRemaining = null,
            currentIntensityMm = 0.0,
            peakIntensityMm = 0.0,
            maxProbability = 10,
            minutelyPoints = minutelyPoints,
            isLiveActive = false,
            isPeak = false,
            isJapan = isJapan,
            isUS = isUS,
            isEurope = isEurope,
            multilingual = multilingual
        )
    }

    /**
     * 状態 RAINING (現在雨が降っている時: effectiveCurrentRain >= 1.0 mm/h)
     */
    private fun analyzeRainingState(
        nowMillis: Long,
        effectiveCurrentRain: Double,
        futureFrames: List<TimelineFrame>,
        minutelyPoints: List<MinutelyRainPoint>,
        isJapan: Boolean = false,
        isUS: Boolean = false,
        isEurope: Boolean = false,
        timeZone: TimeZone = TimeZone.getDefault()
    ): PrecipitationNowcast {
        val peakIntensity = maxOf(effectiveCurrentRain, futureFrames.maxOfOrNull { it.precipMmPerHour } ?: 0.0)

        // 1. 降り止み時刻 EndTime を特定
        // 雨量が 0.1 mm/h 以下になり、かつその状態が20分以上継続する最初の時刻
        var endFrame: TimelineFrame? = null
        for (i in futureFrames.indices) {
            val frame = futureFrames[i]
            if (frame.precipMmPerHour <= (THRESHOLD_RAIN_STOP + 0.0001)) {
                val windowEndMillis = frame.timestampMillis + 20 * 60 * 1000L
                val windowFrames = futureFrames.filter {
                    it.timestampMillis >= frame.timestampMillis && it.timestampMillis <= windowEndMillis
                }
                val sustainsDry = windowFrames.all { it.precipMmPerHour <= (THRESHOLD_RAIN_STOP + 0.0001) } &&
                        (windowFrames.size >= 2 || i == futureFrames.lastIndex ||
                                futureFrames[i + 1].precipMmPerHour <= (THRESHOLD_RAIN_STOP + 0.0001))
                if (sustainsDry) {
                    endFrame = frame
                    break
                }
            }
        }

        // 2. ピーク時刻 PeakTime の探索
        // 止む前に雨量が現在の2倍以上かつ 10.0 mm/h を超える地点がある場合はピーク時刻を優先抽出
        val endBoundaryMillis = endFrame?.timestampMillis ?: (nowMillis + 6 * 3600 * 1000L)
        val framesBeforeEnd = futureFrames.filter {
            it.timestampMillis < endBoundaryMillis &&
                    it.precipMmPerHour >= (2.0 * effectiveCurrentRain) &&
                    it.precipMmPerHour > THRESHOLD_HEAVY_RAIN
        }
        val peakFrame = framesBeforeEnd.maxByOrNull { it.precipMmPerHour }

        if (peakFrame != null) {
            val peakTime = peakFrame.timestampMillis
            val diffMin = ((peakTime - nowMillis) / (60 * 1000L)).toInt().coerceAtLeast(1)
            val timeLabelJa = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.JAPANESE, timeZone) else roundToNearest10Min(peakTime, AppLanguage.JAPANESE, timeZone)
            val timeLabelKo = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.KOREAN, timeZone) else roundToNearest10Min(peakTime, AppLanguage.KOREAN, timeZone)
            val timeLabelEn = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.ENGLISH, timeZone) else roundToNearest10Min(peakTime, AppLanguage.ENGLISH, timeZone)
            val timeLabelZhCn = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.SIMPLIFIED_CHINESE, timeZone) else roundToNearest10Min(peakTime, AppLanguage.SIMPLIFIED_CHINESE, timeZone)
            val timeLabelZhTw = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.TRADITIONAL_CHINESE, timeZone) else roundToNearest10Min(peakTime, AppLanguage.TRADITIONAL_CHINESE, timeZone)
            val timeLabelEs = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.SPANISH, timeZone) else roundToNearest10Min(peakTime, AppLanguage.SPANISH, timeZone)
            val timeLabelFr = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.FRENCH, timeZone) else roundToNearest10Min(peakTime, AppLanguage.FRENCH, timeZone)
            val timeLabelPt = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.PORTUGUESE, timeZone) else roundToNearest10Min(peakTime, AppLanguage.PORTUGUESE, timeZone)
            val timeLabelRu = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.RUSSIAN, timeZone) else roundToNearest10Min(peakTime, AppLanguage.RUSSIAN, timeZone)

            val multilingual = createMultilingualTexts(
                jaHeadline = "${timeLabelJa}頃から雨がさらに強まる見込みです",
                koHeadline = "${timeLabelKo}경부터 빗줄기가 더 강해질 전망입니다",
                enHeadline = "Rain intensifying around $timeLabelEn",
                zhCnHeadline = "预计 $timeLabelZhCn 左右雨势将加剧",
                zhTwHeadline = "預計 $timeLabelZhTw 左右雨勢將加劇",
                esHeadline = "La lluvia se intensificará sobre las $timeLabelEs",
                frHeadline = "Renforcement de la pluie vers $timeLabelFr",
                ptHeadline = "Chuva ficando mais forte por volta das $timeLabelPt",
                ruHeadline = "Дождь усилится около $timeLabelRu",
                jaSubText = "現在より雨脚が強まり、激しい雨（約${formatPrecipMm(peakFrame.precipMmPerHour)}mm/h）となる恐れがあります。視界悪化にご注意ください。",
                koSubText = "지금보다 빗줄기가 강해져 강한 비(약 ${formatPrecipMm(peakFrame.precipMmPerHour)} mm/h)가 내릴 수 있습니다. 시야 악화에 주의하세요.",
                enSubText = "Rain will become heavier (est. ~${formatPrecipMm(peakFrame.precipMmPerHour)} mm/h). Watch out for poor visibility.",
                zhCnSubText = "雨势将进一步加大（约 ${formatPrecipMm(peakFrame.precipMmPerHour)} mm/h），请注意低能见度与安全。",
                zhTwSubText = "雨勢將進一步加大（約 ${formatPrecipMm(peakFrame.precipMmPerHour)} mm/h），請注意低能見度與安全。",
                esSubText = "La lluvia se intensificará (aprox. ${formatPrecipMm(peakFrame.precipMmPerHour)} mm/h). Precaución por baja visibilidad.",
                frSubText = "La pluie va s'intensifier (env. ${formatPrecipMm(peakFrame.precipMmPerHour)} mm/h). Attention à la baisse de visibilité.",
                ptSubText = "A chuva vai ficar mais forte (aprox. ${formatPrecipMm(peakFrame.precipMmPerHour)} mm/h). Atenção à baixa visibilidade.",
                ruSubText = "Дождь усилится (около ${formatPrecipMm(peakFrame.precipMmPerHour)} мм/ч). Будьте осторожны из-за плохой видимости.",
                jaTimeLabel = timeLabelJa,
                koTimeLabel = timeLabelKo,
                enTimeLabel = timeLabelEn,
                zhCnTimeLabel = timeLabelZhCn,
                zhTwTimeLabel = timeLabelZhTw,
                esTimeLabel = timeLabelEs,
                frTimeLabel = timeLabelFr,
                ptTimeLabel = timeLabelPt,
                ruTimeLabel = timeLabelRu
            )

            return buildNowcast(
                status = RainStatus.RAINING,
                targetTimestampMillis = peakTime,
                minutesRemaining = diffMin,
                currentIntensityMm = effectiveCurrentRain,
                peakIntensityMm = peakFrame.precipMmPerHour,
                maxProbability = 95,
                minutelyPoints = minutelyPoints,
                isLiveActive = true,
                isPeak = true,
                isJapan = isJapan,
                isUS = isUS,
                isEurope = isEurope,
                multilingual = multilingual
            )
        }

        // 3. 降り止み時刻が特定された場合（最大15時間まで探索）
        if (endFrame != null) {
            val endTime = endFrame.timestampMillis
            val diffMin = ((endTime - nowMillis) / (60 * 1000L)).toInt().coerceAtLeast(1)
            val timeLabelJa = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.JAPANESE, timeZone) else roundToNearest10Min(endTime, AppLanguage.JAPANESE, timeZone)
            val timeLabelKo = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.KOREAN, timeZone) else roundToNearest10Min(endTime, AppLanguage.KOREAN, timeZone)
            val timeLabelEn = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.ENGLISH, timeZone) else roundToNearest10Min(endTime, AppLanguage.ENGLISH, timeZone)
            val timeLabelZhCn = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.SIMPLIFIED_CHINESE, timeZone) else roundToNearest10Min(endTime, AppLanguage.SIMPLIFIED_CHINESE, timeZone)
            val timeLabelZhTw = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.TRADITIONAL_CHINESE, timeZone) else roundToNearest10Min(endTime, AppLanguage.TRADITIONAL_CHINESE, timeZone)
            val timeLabelEs = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.SPANISH, timeZone) else roundToNearest10Min(endTime, AppLanguage.SPANISH, timeZone)
            val timeLabelFr = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.FRENCH, timeZone) else roundToNearest10Min(endTime, AppLanguage.FRENCH, timeZone)
            val timeLabelPt = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.PORTUGUESE, timeZone) else roundToNearest10Min(endTime, AppLanguage.PORTUGUESE, timeZone)
            val timeLabelRu = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.RUSSIAN, timeZone) else roundToNearest10Min(endTime, AppLanguage.RUSSIAN, timeZone)

            val hours = diffMin / 60
            val mins = diffMin % 60
            val durationJa = when {
                hours > 0 && mins > 0 -> "約${hours}時間${mins}分"
                hours > 0 -> "約${hours}時間"
                else -> "約${mins}分"
            }
            val durationKo = when {
                hours > 0 && mins > 0 -> "약 ${hours}시간 ${mins}분"
                hours > 0 -> "약 ${hours}시간"
                else -> "약 ${mins}분"
            }
            val durationEn = when {
                hours > 0 && mins > 0 -> "about ${hours}h ${mins}m"
                hours > 0 -> "about ${hours} hour${if (hours > 1) "s" else ""}"
                else -> "about ${mins} min"
            }
            val durationZh = when {
                hours > 0 && mins > 0 -> "约${hours}小时${mins}分钟"
                hours > 0 -> "约${hours}小时"
                else -> "约${mins}分钟"
            }
            val durationEs = when {
                hours > 0 && mins > 0 -> "aprox. ${hours} h ${mins} min"
                hours > 0 -> "aprox. ${hours} hora${if (hours > 1) "s" else ""}"
                else -> "unos ${mins} min"
            }
            val durationFr = when {
                hours > 0 && mins > 0 -> "environ ${hours} h ${mins} min"
                hours > 0 -> "environ ${hours} heure${if (hours > 1) "s" else ""}"
                else -> "environ ${mins} min"
            }
            val durationPt = when {
                hours > 0 && mins > 0 -> "cerca de ${hours} h ${mins} min"
                hours > 0 -> "cerca de ${hours} hora${if (hours > 1) "s" else ""}"
                else -> "cerca de ${mins} min"
            }
            val durationRu = when {
                hours > 0 && mins > 0 -> "около ${hours} ч ${mins} мин"
                hours > 0 -> "около ${hours} ч"
                else -> "около ${mins} мин"
            }

            when {
                // A. 短時間 (90分以内): 「もうすぐ雨が止む」
                diffMin <= 90 -> {
                    val jaHeadline = if (diffMin < 15) "まもなく雨が止む見込みです" else "${timeLabelJa}頃に雨が止む見込みです"
                    val koHeadline = if (diffMin < 15) "곧 비가 그칠 전망입니다" else "${timeLabelKo}경 비가 그칠 전망입니다"
                    val enHeadline = if (diffMin < 15) "Rain stopping shortly" else "Rain stopping around $timeLabelEn"
                    val zhCnHeadline = if (diffMin < 15) "雨很快就会停" else "预计 $timeLabelZhCn 左右雨停"
                    val zhTwHeadline = if (diffMin < 15) "雨很快就會停" else "預計 $timeLabelZhTw 左右雨停"
                    val esHeadline = if (diffMin < 15) "Dejará de llover en breve" else "Dejará de llover sobre las $timeLabelEs"
                    val frHeadline = if (diffMin < 15) "La pluie va bientôt cesser" else "Fin de la pluie vers $timeLabelFr"
                    val ptHeadline = if (diffMin < 15) "A chuva deve parar em breve" else "A chuva deve parar por volta das $timeLabelPt"
                    val ruHeadline = if (diffMin < 15) "Дождь скоро закончится" else "Дождь закончится около $timeLabelRu"

                    val jaSubText = "あと${durationJa}雨が降り続く見込みです（${timeLabelJa}頃に降り止み予想）。次第に曇り空へと回復する見込みです。"
                    val koSubText = "앞으로 ${durationKo} 동안 비가 이어질 전망입니다(${timeLabelKo}경 그침 예상). 차차 흐린 날씨로 회복됩니다."
                    val enSubText = "Rain expected to continue for $durationEn (stopping around $timeLabelEn). Conditions will gradually improve."
                    val zhCnSubText = "预计降雨还将持续 $durationZh（$timeLabelZhCn 左右雨停），之后逐渐好转。"
                    val zhTwSubText = "預計降雨還將持續 $durationZh（$timeLabelZhTw 左右雨停），之後逐漸好轉。"
                    val esSubText = "Se espera lluvia durante $durationEs (parará sobre las $timeLabelEs). El tiempo mejorará gradualmente."
                    val frSubText = "Pluie prévue pendant $durationFr (fin vers $timeLabelFr). Le temps va s'améliorer progressivement."
                    val ptSubText = "Previsão de chuva por $durationPt (parando por volta das $timeLabelPt). O tempo deve melhorar gradualmente."
                    val ruSubText = "Дождь продлится $durationRu (закончится около $timeLabelRu). Погода постепенно улучшится."

                    val multilingual = createMultilingualTexts(
                        jaHeadline = jaHeadline,
                        koHeadline = koHeadline,
                        enHeadline = enHeadline,
                        zhCnHeadline = zhCnHeadline,
                        zhTwHeadline = zhTwHeadline,
                        esHeadline = esHeadline,
                        frHeadline = frHeadline,
                        ptHeadline = ptHeadline,
                        ruHeadline = ruHeadline,
                        jaSubText = jaSubText,
                        koSubText = koSubText,
                        enSubText = enSubText,
                        zhCnSubText = zhCnSubText,
                        zhTwSubText = zhTwSubText,
                        esSubText = esSubText,
                        frSubText = frSubText,
                        ptSubText = ptSubText,
                        ruSubText = ruSubText,
                        jaTimeLabel = timeLabelJa,
                        koTimeLabel = timeLabelKo,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw,
                        esTimeLabel = timeLabelEs,
                        frTimeLabel = timeLabelFr,
                        ptTimeLabel = timeLabelPt,
                        ruTimeLabel = timeLabelRu
                    )

                    return buildNowcast(
                        status = RainStatus.RAINING,
                        targetTimestampMillis = endTime,
                        minutesRemaining = diffMin,
                        currentIntensityMm = effectiveCurrentRain,
                        peakIntensityMm = peakIntensity,
                        maxProbability = 90,
                        minutelyPoints = minutelyPoints,
                        isLiveActive = true,
                        isPeak = false,
                        isJapan = isJapan,
                        isUS = isUS,
                        isEurope = isEurope,
                        multilingual = multilingual
                    )
                }

                // B. 中時間 (91分〜180分: 1.5時間〜3時間): 「XX:XX頃まで雨が続く見込み」
                diffMin in 91..180 -> {
                    val jaHeadline = "${timeLabelJa}頃まで雨が続く見込みです"
                    val koHeadline = "${timeLabelKo}경까지 비가 이어질 전망입니다"
                    val enHeadline = "Rain continuing until around $timeLabelEn"
                    val zhCnHeadline = "预计降雨将持续至 $timeLabelZhCn 左右"
                    val zhTwHeadline = "預計降雨將持續至 $timeLabelZhTw 左右"
                    val esHeadline = "La lluvia continuará hasta las $timeLabelEs aprox."
                    val frHeadline = "La pluie continue jusqu'à environ $timeLabelFr"
                    val ptHeadline = "A chuva continua até por volta das $timeLabelPt"
                    val ruHeadline = "Дождь продолжится примерно до $timeLabelRu"

                    val jaSubText = "あと${durationJa}雨が降り続く予想です。次第に雨脚が弱まる見込みです。"
                    val koSubText = "앞으로 ${durationKo} 동안 비가 이어질 예상입니다. 빗줄기가 차차 약해질 전망입니다."
                    val enSubText = "Rain expected to continue for $durationEn. Conditions will gradually ease."
                    val zhCnSubText = "预计降雨还将持续 $durationZh，之后雨势逐渐减弱。"
                    val zhTwSubText = "預計降雨還將持續 $durationZh，之後雨勢逐漸減弱。"
                    val esSubText = "Se espera que la lluvia continúe durante $durationEs y amaine gradualmente."
                    val frSubText = "La pluie devrait durer encore $durationFr avant de faiblir progressivement."
                    val ptSubText = "A chuva deve continuar por $durationPt e enfraquecer gradualmente."
                    val ruSubText = "Дождь продлится $durationRu и постепенно ослабнет."

                    val multilingual = createMultilingualTexts(
                        jaHeadline = jaHeadline,
                        koHeadline = koHeadline,
                        enHeadline = enHeadline,
                        zhCnHeadline = zhCnHeadline,
                        zhTwHeadline = zhTwHeadline,
                        esHeadline = esHeadline,
                        frHeadline = frHeadline,
                        ptHeadline = ptHeadline,
                        ruHeadline = ruHeadline,
                        jaSubText = jaSubText,
                        koSubText = koSubText,
                        enSubText = enSubText,
                        zhCnSubText = zhCnSubText,
                        zhTwSubText = zhTwSubText,
                        esSubText = esSubText,
                        frSubText = frSubText,
                        ptSubText = ptSubText,
                        ruSubText = ruSubText,
                        jaTimeLabel = timeLabelJa,
                        koTimeLabel = timeLabelKo,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw,
                        esTimeLabel = timeLabelEs,
                        frTimeLabel = timeLabelFr,
                        ptTimeLabel = timeLabelPt,
                        ruTimeLabel = timeLabelRu
                    )

                    return buildNowcast(
                        status = RainStatus.RAINING,
                        targetTimestampMillis = endTime,
                        minutesRemaining = diffMin,
                        currentIntensityMm = effectiveCurrentRain,
                        peakIntensityMm = peakIntensity,
                        maxProbability = 90,
                        minutelyPoints = minutelyPoints,
                        isLiveActive = true,
                        isPeak = false,
                        isJapan = isJapan,
                        isUS = isUS,
                        isEurope = isEurope,
                        multilingual = multilingual
                    )
                }

                // C. 長時間 (181分〜540分: 3時間〜9時間): 「雨は当面の間（今後X時間）降り続く見込み」
                diffMin in 181..540 -> {
                    val approxHours = ((diffMin + 29) / 60).coerceAtLeast(3)
                    val jaHeadline = "雨は当面の間（今後${approxHours}時間）降り続く見込みです"
                    val koHeadline = "앞으로 ${approxHours}시간 동안 비가 계속될 전망입니다"
                    val enHeadline = "Rain continuing for the next ${approxHours} hours"
                    val zhCnHeadline = "预计降雨还将持续约${approxHours}小时"
                    val zhTwHeadline = "預計降雨還將持續約${approxHours}小時"
                    val esHeadline = "La lluvia continuará durante las próximas ${approxHours} horas"
                    val frHeadline = "La pluie va continuer pendant les ${approxHours} prochaines heures"
                    val ptHeadline = "A chuva deve continuar pelas próximas ${approxHours} horas"
                    val ruHeadline = "Дождь продолжится в ближайшие ${approxHours} ч"

                    val jaSubText = "${timeLabelJa}頃まで雨が降り続く予想です。お出かけやお帰りの際は雨具を必ずお持ちください。"
                    val koSubText = "${timeLabelKo}경까지 비가 이어질 예상입니다. 외출 및 귀가 시 꼭 우산을 챙기세요."
                    val enSubText = "Rain expected to continue until around $timeLabelEn. Keep an umbrella handy."
                    val zhCnSubText = "预计降雨将持续至 $timeLabelZhCn 左右，出行请务必备好雨具。"
                    val zhTwSubText = "預計降雨將持續至 $timeLabelZhTw 左右，出行請務必備好雨具。"
                    val esSubText = "Se espera lluvia hasta las $timeLabelEs aprox. Ten siempre un paraguas a mano."
                    val frSubText = "Pluie attendue jusqu'à environ $timeLabelFr. Gardez un parapluie à portée de main."
                    val ptSubText = "Previsão de chuva até por volta das $timeLabelPt. Mantenha um guarda-chuva por perto."
                    val ruSubText = "Дождь ожидается примерно до $timeLabelRu. Держите зонт под рукой."

                    val multilingual = createMultilingualTexts(
                        jaHeadline = jaHeadline,
                        koHeadline = koHeadline,
                        enHeadline = enHeadline,
                        zhCnHeadline = zhCnHeadline,
                        zhTwHeadline = zhTwHeadline,
                        esHeadline = esHeadline,
                        frHeadline = frHeadline,
                        ptHeadline = ptHeadline,
                        ruHeadline = ruHeadline,
                        jaSubText = jaSubText,
                        koSubText = koSubText,
                        enSubText = enSubText,
                        zhCnSubText = zhCnSubText,
                        zhTwSubText = zhTwSubText,
                        esSubText = esSubText,
                        frSubText = frSubText,
                        ptSubText = ptSubText,
                        ruSubText = ruSubText,
                        jaTimeLabel = timeLabelJa,
                        koTimeLabel = timeLabelKo,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw,
                        esTimeLabel = timeLabelEs,
                        frTimeLabel = timeLabelFr,
                        ptTimeLabel = timeLabelPt,
                        ruTimeLabel = timeLabelRu
                    )

                    return buildNowcast(
                        status = RainStatus.RAINING,
                        targetTimestampMillis = endTime,
                        minutesRemaining = diffMin,
                        currentIntensityMm = effectiveCurrentRain,
                        peakIntensityMm = peakIntensity,
                        maxProbability = 90,
                        minutelyPoints = minutelyPoints,
                        isLiveActive = true,
                        isPeak = false,
                        isJapan = isJapan,
                        isUS = isUS,
                        isEurope = isEurope,
                        multilingual = multilingual
                    )
                }
            }
        }

        // 4. 先6時間ずっと降り続く場合
        val multilingual = createMultilingualTexts(
            jaHeadline = "雨は当面の間（6時間以上）降り続く見込みです",
            koHeadline = "앞으로 6시간 이상 비가 계속될 전망입니다",
            enHeadline = "Rain will continue for the next 6+ hours",
            zhCnHeadline = "预计降雨还将持续6小时以上",
            zhTwHeadline = "預計降雨還將持續6小時以上",
            esHeadline = "La lluvia continuará durante más de 6 horas",
            frHeadline = "La pluie va continuer pendant plus de 6 heures",
            ptHeadline = "A chuva deve continuar pelas próximas 6+ horas",
            ruHeadline = "Дождь продолжится более 6 часов",
            jaSubText = "今後6時間以上、まとまった雨が降り続く予想です。お足元や視界の悪化にご注意ください。",
            koSubText = "앞으로 6시간 이상 비가 계속 내릴 전망입니다. 빗길 미끄럼과 시야 악화에 주의하세요.",
            enSubText = "Steady rain expected to continue for the next 6+ hours. Take care on wet roads.",
            zhCnSubText = "预计未来6小时以上持续有雨，外出请注意路面湿滑。",
            zhTwSubText = "預計未來6小時以上持續有雨，外出請注意路面濕滑。",
            esSubText = "Se espera lluvia constante durante más de 6 horas. Extrema la precaución en calzadas mojadas.",
            frSubText = "Pluie régulière attendue pendant plus de 6 heures. Soyez prudent sur les routes glissantes.",
            ptSubText = "Chuva constante prevista pelas próximas 6+ horas. Tenha cuidado em vias molhadas.",
            ruSubText = "Устойчивый дождь ожидается более 6 часов. Будьте осторожны на мокрых дорогах.",
            jaTimeLabel = "当面の間",
            koTimeLabel = "6시간 이상",
            enTimeLabel = "6+ hours",
            zhCnTimeLabel = "6小时以上",
            zhTwTimeLabel = "6小時以上",
            esTimeLabel = "Más de 6 h",
            frTimeLabel = "Plus de 6 h",
            ptTimeLabel = "6+ horas",
            ruTimeLabel = "Более 6 ч"
        )

        return buildNowcast(
            status = RainStatus.RAINING,
            targetTimestampMillis = null,
            minutesRemaining = null,
            currentIntensityMm = effectiveCurrentRain,
            peakIntensityMm = peakIntensity,
            maxProbability = 90,
            minutelyPoints = minutelyPoints,
            isLiveActive = true,
            isPeak = false,
            isJapan = isJapan,
            isUS = isUS,
            isEurope = isEurope,
            multilingual = multilingual
        )
    }

    /**
     * 4つのデータソース（ナウキャスト5分、短時間10分、短時間1時間、府県予報6時間）を連続したタイムラインとして合成
     */
    private fun buildContinuousTimeline(
        nowMillis: Long,
        currentPrecipMm: Double,
        currentCondition: WeatherCondition,
        minutelyTimes: List<String>?,
        minutelyPrecips: List<Double>?,
        hourlyForecast: List<HourlyForecast>,
        popSlots: List<PopSlot>?,
        jmaMinutelySeries: List<Pair<Long, Double>>? = null,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<TimelineFrame> {
        val timeline = mutableListOf<TimelineFrame>()

        // 5分/15分刻みデータの事前パース (JMAナウキャスト優先・タイムスタンプ昇順)
        val parsedMinutely = mutableListOf<Pair<Long, Double>>()
        if (!jmaMinutelySeries.isNullOrEmpty()) {
            jmaMinutelySeries.forEach { (tMillis, pVal) ->
                parsedMinutely.add(Pair(tMillis, pVal.coerceAtLeast(0.0)))
            }
            parsedMinutely.sortBy { it.first }
        } else if (minutelyTimes != null && minutelyPrecips != null && minutelyTimes.size == minutelyPrecips.size) {
            for (i in minutelyTimes.indices) {
                val tMillis = parseIsoToMillis(minutelyTimes[i], timeZone) ?: continue
                val pVal = minutelyPrecips[i].coerceAtLeast(0.0)
                parsedMinutely.add(Pair(tMillis, pVal))
            }
            parsedMinutely.sortBy { it.first }
        }

        // 現在フレーム (t = 0)
        val isRainNow = isConditionRain(currentCondition, currentPrecipMm)
        val parsedMinutelyNow = parsedMinutely
            .filter { kotlin.math.abs(it.first - nowMillis) <= 10 * 60 * 1000L }
            .minByOrNull { kotlin.math.abs(it.first - nowMillis) }
            ?.second
        val nowPrecip = if (currentPrecipMm > 0.0) {
            currentPrecipMm
        } else if (parsedMinutelyNow != null) {
            parsedMinutelyNow
        } else if (isRainNow) {
            hourlyForecast.firstOrNull()?.precipitationMm?.takeIf { it > 0.0 } ?: 1.5
        } else {
            0.0
        }
        timeline.add(
            TimelineFrame(
                timestampMillis = nowMillis,
                minutesFromNow = 0,
                precipMmPerHour = nowPrecip,
                precipProbability = if (nowPrecip >= THRESHOLD_RAIN_START || isRainNow) 90 else 10,
                tier = TimelineTier.TIER1_NOWCAST_5MIN,
                timeLabel = formatToHHmm(nowMillis, timeZone)
            )
        )

        // =====================================================================
        // Tier 1: 直近60分 (高解像度降水ナウキャスト 5分間隔, 12フレーム)
        // =====================================================================
        for (offsetMin in 5..60 step 5) {
            val t = nowMillis + offsetMin * 60 * 1000L
            val pMm = samplePrecip(t, parsedMinutely, hourlyForecast, nowMillis, nowPrecip, timeZone)
            val pop = samplePop(t, popSlots, hourlyForecast, pMm, timeZone)
            timeline.add(
                TimelineFrame(
                    timestampMillis = t,
                    minutesFromNow = offsetMin,
                    precipMmPerHour = pMm,
                    precipProbability = pop,
                    tier = TimelineTier.TIER1_NOWCAST_5MIN,
                    timeLabel = formatToHHmm(t, timeZone)
                )
            )
        }

        // =====================================================================
        // Tier 2: 1時間後から6時間後まで (降水短時間予報 10分間隔, 30フレーム)
        // =====================================================================
        for (offsetMin in 70..360 step 10) {
            val t = nowMillis + offsetMin * 60 * 1000L
            val pMm = samplePrecip(t, parsedMinutely, hourlyForecast, nowMillis, nowPrecip, timeZone)
            val pop = samplePop(t, popSlots, hourlyForecast, pMm, timeZone)
            timeline.add(
                TimelineFrame(
                    timestampMillis = t,
                    minutesFromNow = offsetMin,
                    precipMmPerHour = pMm,
                    precipProbability = pop,
                    tier = TimelineTier.TIER2_SHORT_FORECAST_10MIN,
                    timeLabel = roundToNearest10Min(t, timeZone = timeZone)
                )
            )
        }

        // =====================================================================
        // Tier 3: 6時間後から15時間後まで (降水短時間予報 1時間間隔, 9フレーム)
        // =====================================================================
        for (offsetHour in 7..15) {
            val offsetMin = offsetHour * 60
            val t = nowMillis + offsetHour * 3600 * 1000L
            val pMm = samplePrecip(t, parsedMinutely, hourlyForecast, nowMillis, nowPrecip, timeZone)
            val pop = samplePop(t, popSlots, hourlyForecast, pMm, timeZone)
            timeline.add(
                TimelineFrame(
                    timestampMillis = t,
                    minutesFromNow = offsetMin,
                    precipMmPerHour = pMm,
                    precipProbability = pop,
                    tier = TimelineTier.TIER3_SHORT_FORECAST_1H,
                    timeLabel = roundToNearestHour(t, timeZone = timeZone)
                )
            )
        }

        // =====================================================================
        // Tier 4: 15時間以降から翌日 (府県天気予報 6時間ブロック)
        // =====================================================================
        val calBlock = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis + 15 * 3600 * 1000L
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val h = get(Calendar.HOUR_OF_DAY)
            set(Calendar.HOUR_OF_DAY, (h / 6) * 6)
        }

        val limitMillis = nowMillis + 48 * 3600 * 1000L
        while (calBlock.timeInMillis <= limitMillis) {
            val bStart = calBlock.timeInMillis
            val bEnd = bStart + 6 * 3600 * 1000L
            val offsetMin = ((bStart - nowMillis) / (60 * 1000L)).toInt()

            val pop = if (!popSlots.isNullOrEmpty()) {
                val matched = popSlots.lastOrNull { bStart >= it.startMillis }
                matched?.pop ?: 20
            } else if (hourlyForecast.size >= 16) {
                // フル24時間以上の時系列では実時刻インデックスで照合し、今日の降雨が翌日へ誤循環するのを防止
                val startIdx = ((bStart - nowMillis) / (3600 * 1000L)).toInt()
                val endIdx = ((bEnd - nowMillis) / (3600 * 1000L)).toInt()
                val inRange = if (startIdx < hourlyForecast.size && endIdx > 0) {
                    hourlyForecast.subList(startIdx.coerceAtLeast(0), endIdx.coerceAtMost(hourlyForecast.size))
                } else {
                    emptyList()
                }
                inRange.maxOfOrNull { it.precipProb } ?: 20
            } else {
                val startHour = calBlock.get(Calendar.HOUR_OF_DAY)
                val endHour = startHour + 6
                val inRange = hourlyForecast.filter { it.hour in startHour until endHour }
                inRange.maxOfOrNull { it.precipProb } ?: 20
            }

            timeline.add(
                TimelineFrame(
                    timestampMillis = bStart,
                    minutesFromNow = offsetMin,
                    precipMmPerHour = 0.0,
                    precipProbability = pop,
                    tier = TimelineTier.TIER4_PREFECTURAL_6H,
                    timeLabel = formatTier4TimeOfDayExpression(bStart, timeZone = timeZone).second
                )
            )

            calBlock.add(Calendar.HOUR_OF_DAY, 6)
        }

        return timeline
    }

    /**
     * 指定時刻の降水量をサンプリング / 線形補間
     */
    private fun samplePrecip(
        targetMillis: Long,
        parsedMinutely: List<Pair<Long, Double>>,
        hourlyForecast: List<HourlyForecast>,
        nowMillis: Long,
        nowPrecip: Double,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Double {
        if (parsedMinutely.isNotEmpty()) {
            val minT = parsedMinutely.first().first
            val maxT = parsedMinutely.last().first
            if (targetMillis in minT..maxT) {
                val nextIdx = parsedMinutely.indexOfFirst { it.first >= targetMillis }
                if (nextIdx == 0) return parsedMinutely[0].second
                if (nextIdx > 0) {
                    val p0 = parsedMinutely[nextIdx - 1]
                    val p1 = parsedMinutely[nextIdx]
                    val span = (p1.first - p0.first).toDouble().coerceAtLeast(1.0)
                    val frac = ((targetMillis - p0.first) / span).coerceIn(0.0, 1.0)
                    if (frac >= 1.0) return p1.second
                    if (frac <= 0.0) return p0.second
                    return p0.second + frac * (p1.second - p0.second)
                }
            }
        }

        // minutely 範囲外または未提供（日本国内等、Open-Meteo情報遮断時）の場合は
        // 直近ナウキャスト末尾 (存在すれば) または現在値と 1時間毎の予報 (hourlyForecast) を滑らかに線形補間
        if (hourlyForecast.isNotEmpty()) {
            val anchors = mutableListOf<Pair<Long, Double>>()
            val tailMinutely = parsedMinutely.lastOrNull()
            if (tailMinutely != null && tailMinutely.first > nowMillis && targetMillis >= tailMinutely.first) {
                anchors.add(tailMinutely)
            } else {
                val startPrecip = if (nowPrecip > 0.0) nowPrecip else (hourlyForecast.firstOrNull()?.precipitationMm?.takeIf { it > 0.0 } ?: 0.0)
                anchors.add(Pair(nowMillis, startPrecip.coerceAtLeast(0.0)))
            }
            for (step in 1 until hourlyForecast.size) {
                val hItem = hourlyForecast[step]
                val t = nowMillis + step * 3600 * 1000L
                if (t <= anchors.last().first) continue
                val precipVal = if (hItem.precipitationMm > 0.0) {
                    hItem.precipitationMm
                } else if (isConditionRain(hItem.condition)) {
                    if (hItem.precipProb >= 70) 2.5 else if (hItem.precipProb >= 50) 1.2 else 0.4
                } else {
                    0.0
                }
                anchors.add(Pair(t, precipVal))
            }

            if (targetMillis <= anchors.first().first) {
                return anchors.first().second
            }
            if (targetMillis >= anchors.last().first) {
                return anchors.last().second
            }

            val nextIdx = anchors.indexOfFirst { it.first >= targetMillis }
            if (nextIdx > 0) {
                val p0 = anchors[nextIdx - 1]
                val p1 = anchors[nextIdx]
                val span = (p1.first - p0.first).toDouble().coerceAtLeast(1.0)
                val frac = ((targetMillis - p0.first) / span).coerceIn(0.0, 1.0)
                if (frac >= 1.0) return p1.second
                if (frac <= 0.0) return p0.second
                return (p0.second + frac * (p1.second - p0.second)).coerceAtLeast(0.0)
            }
        }

        val diffHours = ((targetMillis - nowMillis) / (3600 * 1000L)).toInt()
        if (diffHours in hourlyForecast.indices) {
            val hItem = hourlyForecast[diffHours]
            if (hItem.precipitationMm > 0.0) return hItem.precipitationMm
            if (isConditionRain(hItem.condition)) return 1.5
        }

        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = targetMillis }
        val targetHour = cal.get(Calendar.HOUR_OF_DAY)
        val matchedHour = hourlyForecast.firstOrNull { it.hour == targetHour }
        if (matchedHour != null) {
            if (matchedHour.precipitationMm > 0.0) return matchedHour.precipitationMm
            if (isConditionRain(matchedHour.condition)) return 1.5
        }

        return if (targetMillis == nowMillis) nowPrecip.coerceAtLeast(0.0) else 0.0
    }

    /**
     * 指定時刻の降水確率をサンプリング
     */
    private fun samplePop(
        targetMillis: Long,
        popSlots: List<PopSlot>?,
        hourlyForecast: List<HourlyForecast>,
        precipMm: Double,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        if (!popSlots.isNullOrEmpty()) {
            val matched = popSlots.lastOrNull { targetMillis >= it.startMillis }
            if (matched != null) return matched.pop
        }
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = targetMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val hourlyMatched = hourlyForecast.firstOrNull { it.hour == hour }
        if (hourlyMatched != null) return hourlyMatched.precipProb

        return if (precipMm >= THRESHOLD_RAIN_START) 80 else if (precipMm > (THRESHOLD_RAIN_STOP + 0.0001)) 40 else 10
    }

    /**
     * 10分単位の丸め時刻文字列の生成 (例: "14時20分" / "14:20")
     */
    fun roundToNearest10Min(
        millis: Long,
        language: AppLanguage = AppLanguage.JAPANESE,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val minute = cal.get(Calendar.MINUTE)
        val roundedMinute = ((minute + 5) / 10 * 10)
        if (roundedMinute == 60) {
            cal.add(Calendar.HOUR_OF_DAY, 1)
            cal.set(Calendar.MINUTE, 0)
        } else {
            cal.set(Calendar.MINUTE, roundedMinute)
        }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return when (language) {
            AppLanguage.JAPANESE -> "${h}時${String.format(Locale.JAPAN, "%02d", m)}分"
            AppLanguage.FRENCH -> String.format(Locale.FRANCE, "%02dh%02d", h, m)
            else -> String.format(Locale.US, "%02d:%02d", h, m)
        }
    }

    /**
     * 1時間単位の丸め時刻文字列の生成 (例: "19時" / "19:00")
     */
    fun roundToNearestHour(
        millis: Long,
        language: AppLanguage = AppLanguage.JAPANESE,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val minute = cal.get(Calendar.MINUTE)
        if (minute >= 30) {
            cal.add(Calendar.HOUR_OF_DAY, 1)
        }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        return when (language) {
            AppLanguage.JAPANESE -> "${h}時"
            AppLanguage.KOREAN -> "${h}시"
            AppLanguage.FRENCH -> String.format(Locale.FRANCE, "%02dh00", h)
            else -> String.format(Locale.US, "%02d:00", h)
        }
    }

    /**
     * 5分単位の時刻文字列の生成 (例: "14時25分" / "14:25")
     */
    fun formatTo5MinTime(
        millis: Long,
        language: AppLanguage = AppLanguage.JAPANESE,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val minute = cal.get(Calendar.MINUTE)
        val roundedMinute = ((minute + 2) / 5 * 5)
        if (roundedMinute == 60) {
            cal.add(Calendar.HOUR_OF_DAY, 1)
            cal.set(Calendar.MINUTE, 0)
        } else {
            cal.set(Calendar.MINUTE, roundedMinute)
        }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return when (language) {
            AppLanguage.JAPANESE -> "${h}時${String.format(Locale.JAPAN, "%02d", m)}分"
            AppLanguage.FRENCH -> String.format(Locale.FRANCE, "%02dh%02d", h, m)
            else -> String.format(Locale.US, "%02d:%02d", h, m)
        }
    }

    /**
     * 翌日・15時間以降の時間帯表現 (例: "明日の朝から雨が降る見込みです")
     */
    fun formatTier4TimeOfDayExpression(
        blockStartMillis: Long,
        language: AppLanguage = AppLanguage.JAPANESE,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Pair<String, String> {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = blockStartMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when (language) {
            AppLanguage.JAPANESE -> when (hour) {
                in 0..5 -> Pair("明日の未明から雨が降る見込みです", "明日未明")
                in 6..11 -> Pair("明日の朝から雨が降る見込みです", "明日朝")
                in 12..17 -> Pair("明日の午後から雨が降る見込みです", "明日午後")
                else -> Pair("明日の夕方以降は傘が必要です", "明日夕方以降")
            }
            AppLanguage.KOREAN -> when (hour) {
                in 0..5 -> Pair("내일 새벽부터 비가 올 전망입니다", "내일 새벽")
                in 6..11 -> Pair("내일 아침부터 비가 올 전망입니다", "내일 아침")
                in 12..17 -> Pair("내일 오후부터 비가 올 전망입니다", "내일 오후")
                else -> Pair("내일 저녁부터 우산이 필요합니다", "내일 저녁 이후")
            }
            AppLanguage.ENGLISH -> when (hour) {
                in 0..5 -> Pair("Rain expected late tonight or early tomorrow", "Early Tomorrow")
                in 6..11 -> Pair("Rain expected tomorrow morning", "Tomorrow Morning")
                in 12..17 -> Pair("Rain expected tomorrow afternoon", "Tomorrow Afternoon")
                else -> Pair("Umbrella needed tomorrow evening", "Tomorrow Evening")
            }
            AppLanguage.SIMPLIFIED_CHINESE -> when (hour) {
                in 0..5 -> Pair("预计明天凌晨有雨", "明天凌晨")
                in 6..11 -> Pair("预计明天早晨有雨", "明天早晨")
                in 12..17 -> Pair("预计明天午后有雨", "明天午后")
                else -> Pair("明天傍晚起需要带伞", "明天傍晚起")
            }
            AppLanguage.TRADITIONAL_CHINESE -> when (hour) {
                in 0..5 -> Pair("預計明天凌晨有雨", "明天凌晨")
                in 6..11 -> Pair("預計明天早晨有雨", "明天早晨")
                in 12..17 -> Pair("預計明天午後有雨", "明天午後")
                else -> Pair("明天傍晚起需要帶傘", "明天傍晚起")
            }
            AppLanguage.SPANISH -> when (hour) {
                in 0..5 -> Pair("Se espera lluvia de madrugada", "Madrugada de mañana")
                in 6..11 -> Pair("Se espera lluvia mañana por la mañana", "Mañana por la mañana")
                in 12..17 -> Pair("Se espera lluvia mañana por la tarde", "Mañana por la tarde")
                else -> Pair("Necesitarás paraguas mañana al atardecer", "Mañana al atardecer")
            }
            AppLanguage.FRENCH -> when (hour) {
                in 0..5 -> Pair("Pluie attendue demain avant l'aube", "Demain avant l'aube")
                in 6..11 -> Pair("Pluie attendue demain matin", "Demain matin")
                in 12..17 -> Pair("Pluie attendue demain après-midi", "Demain après-midi")
                else -> Pair("Parapluie nécessaire demain en soirée", "Demain en soirée")
            }
            AppLanguage.PORTUGUESE -> when (hour) {
                in 0..5 -> Pair("Previsão de chuva na madrugada de amanhã", "Madrugada de amanhã")
                in 6..11 -> Pair("Previsão de chuva amanhã de manhã", "Amanhã de manhã")
                in 12..17 -> Pair("Previsão de chuva amanhã à tarde", "Amanhã à tarde")
                else -> Pair("Você vai precisar de guarda-chuva amanhã à noite", "Amanhã à noite")
            }
            AppLanguage.RUSSIAN -> when (hour) {
                in 0..5 -> Pair("Завтра на рассвете ожидается дождь", "Завтра на рассвете")
                in 6..11 -> Pair("Завтра утром ожидается дождь", "Завтра утром")
                in 12..17 -> Pair("Завтра днём ожидается дождь", "Завтра днём")
                else -> Pair("Завтра вечером понадобится зонт", "Завтра вечером")
            }
        }
    }

    private fun createMultilingualTexts(
        jaHeadline: String,
        koHeadline: String,
        enHeadline: String,
        zhCnHeadline: String,
        zhTwHeadline: String,
        esHeadline: String,
        frHeadline: String,
        ptHeadline: String,
        ruHeadline: String = enHeadline,
        jaSubText: String,
        koSubText: String,
        enSubText: String,
        zhCnSubText: String,
        zhTwSubText: String,
        esSubText: String,
        frSubText: String,
        ptSubText: String,
        ruSubText: String = enSubText,
        jaTimeLabel: String? = null,
        koTimeLabel: String? = null,
        enTimeLabel: String? = null,
        zhCnTimeLabel: String? = null,
        zhTwTimeLabel: String? = null,
        esTimeLabel: String? = null,
        frTimeLabel: String? = null,
        ptTimeLabel: String? = null,
        ruTimeLabel: String? = enTimeLabel
    ): Triple<Map<String, String>, Map<String, String>, Map<String, String>> {
        val headlines = mapOf(
            AppLanguage.JAPANESE.code to jaHeadline,
            AppLanguage.KOREAN.code to koHeadline,
            AppLanguage.ENGLISH.code to enHeadline,
            AppLanguage.SIMPLIFIED_CHINESE.code to zhCnHeadline,
            AppLanguage.TRADITIONAL_CHINESE.code to zhTwHeadline,
            AppLanguage.SPANISH.code to esHeadline,
            AppLanguage.FRENCH.code to frHeadline,
            AppLanguage.PORTUGUESE.code to ptHeadline,
            AppLanguage.RUSSIAN.code to ruHeadline
        )
        val subTexts = mapOf(
            AppLanguage.JAPANESE.code to jaSubText,
            AppLanguage.KOREAN.code to koSubText,
            AppLanguage.ENGLISH.code to enSubText,
            AppLanguage.SIMPLIFIED_CHINESE.code to zhCnSubText,
            AppLanguage.TRADITIONAL_CHINESE.code to zhTwSubText,
            AppLanguage.SPANISH.code to esSubText,
            AppLanguage.FRENCH.code to frSubText,
            AppLanguage.PORTUGUESE.code to ptSubText,
            AppLanguage.RUSSIAN.code to ruSubText
        )
        val timeLabels = mutableMapOf<String, String>()
        if (jaTimeLabel != null) timeLabels[AppLanguage.JAPANESE.code] = jaTimeLabel
        if (koTimeLabel != null) timeLabels[AppLanguage.KOREAN.code] = koTimeLabel
        if (enTimeLabel != null) timeLabels[AppLanguage.ENGLISH.code] = enTimeLabel
        if (zhCnTimeLabel != null) timeLabels[AppLanguage.SIMPLIFIED_CHINESE.code] = zhCnTimeLabel
        if (zhTwTimeLabel != null) timeLabels[AppLanguage.TRADITIONAL_CHINESE.code] = zhTwTimeLabel
        if (esTimeLabel != null) timeLabels[AppLanguage.SPANISH.code] = esTimeLabel
        if (frTimeLabel != null) timeLabels[AppLanguage.FRENCH.code] = frTimeLabel
        if (ptTimeLabel != null) timeLabels[AppLanguage.PORTUGUESE.code] = ptTimeLabel
        if (ruTimeLabel != null) timeLabels[AppLanguage.RUSSIAN.code] = ruTimeLabel

        return Triple(headlines, subTexts, timeLabels)
    }

    private fun buildNowcast(
        status: RainStatus,
        targetTimestampMillis: Long?,
        minutesRemaining: Int?,
        currentIntensityMm: Double,
        peakIntensityMm: Double,
        maxProbability: Int,
        minutelyPoints: List<MinutelyRainPoint>,
        isLiveActive: Boolean,
        isPeak: Boolean = false,
        isJapan: Boolean = false,
        isUS: Boolean = false,
        isEurope: Boolean = false,
        dataSourceName: String? = null,
        multilingual: Triple<Map<String, String>, Map<String, String>, Map<String, String>>
    ): PrecipitationNowcast {
        val (headlines, subTexts, timeLabels) = multilingual
        val jaHeadline = headlines[AppLanguage.JAPANESE.code] ?: headlines.values.firstOrNull() ?: ""
        val jaSubText = subTexts[AppLanguage.JAPANESE.code] ?: subTexts.values.firstOrNull() ?: ""
        val jaTimeLabel = timeLabels[AppLanguage.JAPANESE.code]

        val resolvedDataSource = dataSourceName ?: when {
            isJapan -> "気象庁 (JMA)"
            isUS -> "米国気象局 (NOAA/NWS)"
            isEurope -> "欧州中期予報センター (ECMWF) / 各気象機関"
            else -> "ECMWF・NOAA 国際数値気象モデル"
        }

        return PrecipitationNowcast(
            status = status,
            headline = jaHeadline,
            subText = jaSubText,
            targetTimeLabel = jaTimeLabel,
            targetTimestampMillis = targetTimestampMillis,
            minutesRemaining = minutesRemaining,
            currentIntensityMm = currentIntensityMm,
            peakIntensityMm = peakIntensityMm,
            maxProbability = maxProbability,
            minutelyPoints = minutelyPoints,
            isLiveActive = isLiveActive,
            isPeak = isPeak,
            localizedHeadlines = headlines,
            localizedSubTexts = subTexts,
            localizedTargetTimeLabels = timeLabels,
            isJapan = isJapan,
            isUS = isUS,
            isEurope = isEurope,
            dataSourceName = resolvedDataSource
        )
    }

    private fun formatToHHmm(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        return String.format(Locale.JAPAN, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun formatPrecipMm(mm: Double): String {
        return String.format(Locale.US, "%.1f", mm)
    }

    private fun parseIsoToMillis(isoStr: String, timeZone: TimeZone = TimeZone.getDefault()): Long? {
        return try {
            val clean = if (isoStr.length >= 16) isoStr.substring(0, 16) else isoStr
            val fmt = isoFormatThreadLocal.get() ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            fmt.timeZone = timeZone
            fmt.parse(clean)?.time
        } catch (_: Exception) {
            null
        }
    }

    fun isConditionRain(
        condition: WeatherCondition,
        precipMm: Double = 0.0
    ): Boolean {
        if (precipMm >= 0.5) return true
        if (condition.isPrecipitating) return true
        return condition in listOf(
            WeatherCondition.RAIN,
            WeatherCondition.HEAVY_RAIN,
            WeatherCondition.THUNDERSTORM,
            WeatherCondition.SNOW
        )
    }

    /**
     * 雨の天気かどうかの判定（現在雨が降っている地点の判定）
     */
    fun isRainWeather(weather: WeatherData): Boolean {
        return isConditionRain(weather.condition, weather.precipitationMm)
    }

    /**
     * 雨の天気の地点が選択された場合に降雨カードを表示するためのフォールバック
     */
    fun createFallbackRainNowcast(
        weather: WeatherData,
        nowMillis: Long = System.currentTimeMillis()
    ): PrecipitationNowcast {
        val isJapan = isLocationOrCountryInJapan(weather.city.country, weather.city.latitude, weather.city.longitude)
        val isUS = isLocationOrCountryInUnitedStates(weather.city.country, weather.city.latitude, weather.city.longitude)
        val isEurope = isLocationOrCountryInEurope(weather.city.country, weather.city.latitude, weather.city.longitude)
        val timeZone = weather.utcOffsetSeconds?.let { offsetSec ->
            java.util.SimpleTimeZone(offsetSec * 1000, "CityTZ")
        } ?: if (isJapan) TimeZone.getTimeZone("Asia/Tokyo") else TimeZone.getDefault()

        // 1. 今後何時間雨が降り続くか（hourlyForecastから探索）
        var stopHourIndex: Int? = null
        for (i in 1 until weather.hourlyForecast.size) {
            val h = weather.hourlyForecast[i]
            val isRain = isConditionRain(h.condition, h.precipitationMm)
            if (!isRain && h.precipProb < 40) {
                stopHourIndex = i
                break
            }
        }

        val effectivePrecip = maxOf(weather.precipitationMm, 1.5)
        val peakIntensity = maxOf(
            effectivePrecip,
            weather.hourlyForecast.take(6).maxOfOrNull { it.precipitationMm } ?: effectivePrecip
        )

        // 降雨が一定時間内に止む場合
        if (stopHourIndex != null && stopHourIndex in 1..15) {
            val diffMin = stopHourIndex * 60
            val endTime = nowMillis + diffMin * 60 * 1000L
            val timeLabelJa = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.JAPANESE, timeZone) else roundToNearest10Min(endTime, AppLanguage.JAPANESE, timeZone)
            val timeLabelKo = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.KOREAN, timeZone) else roundToNearest10Min(endTime, AppLanguage.KOREAN, timeZone)
            val timeLabelEn = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.ENGLISH, timeZone) else roundToNearest10Min(endTime, AppLanguage.ENGLISH, timeZone)
            val timeLabelZhCn = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.SIMPLIFIED_CHINESE, timeZone) else roundToNearest10Min(endTime, AppLanguage.SIMPLIFIED_CHINESE, timeZone)
            val timeLabelZhTw = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.TRADITIONAL_CHINESE, timeZone) else roundToNearest10Min(endTime, AppLanguage.TRADITIONAL_CHINESE, timeZone)
            val timeLabelEs = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.SPANISH, timeZone) else roundToNearest10Min(endTime, AppLanguage.SPANISH, timeZone)
            val timeLabelFr = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.FRENCH, timeZone) else roundToNearest10Min(endTime, AppLanguage.FRENCH, timeZone)
            val timeLabelPt = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.PORTUGUESE, timeZone) else roundToNearest10Min(endTime, AppLanguage.PORTUGUESE, timeZone)
            val timeLabelRu = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.RUSSIAN, timeZone) else roundToNearest10Min(endTime, AppLanguage.RUSSIAN, timeZone)

            val hours = diffMin / 60
            val mins = diffMin % 60
            val durationJa = when {
                hours > 0 && mins > 0 -> "約${hours}時間${mins}分"
                hours > 0 -> "約${hours}時間"
                else -> "約${mins}分"
            }
            val durationKo = when {
                hours > 0 && mins > 0 -> "약 ${hours}시간 ${mins}분"
                hours > 0 -> "약 ${hours}시간"
                else -> "약 ${mins}분"
            }
            val durationEn = when {
                hours > 0 && mins > 0 -> "about ${hours}h ${mins}m"
                hours > 0 -> "about ${hours} hour${if (hours > 1) "s" else ""}"
                else -> "about ${mins} min"
            }
            val durationZh = when {
                hours > 0 && mins > 0 -> "约${hours}小时${mins}分钟"
                hours > 0 -> "约${hours}小时"
                else -> "约${mins}分钟"
            }
            val durationEs = when {
                hours > 0 && mins > 0 -> "aprox. ${hours} h ${mins} min"
                hours > 0 -> "aprox. ${hours} hora${if (hours > 1) "s" else ""}"
                else -> "unos ${mins} min"
            }
            val durationFr = when {
                hours > 0 && mins > 0 -> "environ ${hours} h ${mins} min"
                hours > 0 -> "environ ${hours} heure${if (hours > 1) "s" else ""}"
                else -> "environ ${mins} min"
            }
            val durationPt = when {
                hours > 0 && mins > 0 -> "cerca de ${hours} h ${mins} min"
                hours > 0 -> "cerca de ${hours} hora${if (hours > 1) "s" else ""}"
                else -> "cerca de ${mins} min"
            }
            val durationRu = when {
                hours > 0 && mins > 0 -> "около ${hours} ч ${mins} мин"
                hours > 0 -> "около ${hours} ч"
                else -> "около ${mins} мин"
            }

            val multilingual = when {
                diffMin <= 90 -> {
                    val jaHeadline = if (diffMin < 15) "まもなく雨が止む見込みです" else "${timeLabelJa}頃に雨が止む見込みです"
                    val koHeadline = if (diffMin < 15) "곧 비가 그칠 전망입니다" else "${timeLabelKo}경 비가 그칠 전망입니다"
                    val enHeadline = if (diffMin < 15) "Rain stopping shortly" else "Rain stopping around $timeLabelEn"
                    val zhCnHeadline = if (diffMin < 15) "雨很快就会停" else "预计 $timeLabelZhCn 左右雨停"
                    val zhTwHeadline = if (diffMin < 15) "雨很快就會停" else "預計 $timeLabelZhTw 左右雨停"
                    val esHeadline = if (diffMin < 15) "Dejará de llover en breve" else "Dejará de llover sobre las $timeLabelEs"
                    val frHeadline = if (diffMin < 15) "La pluie va bientôt cesser" else "Fin de la pluie vers $timeLabelFr"
                    val ptHeadline = if (diffMin < 15) "A chuva deve parar em breve" else "A chuva deve parar por volta das $timeLabelPt"
                    val ruHeadline = if (diffMin < 15) "Дождь скоро закончится" else "Дождь закончится около $timeLabelRu"

                    val jaSubText = "あと${durationJa}雨が降り続く見込みです（${timeLabelJa}頃に降り止み予想）。次第に曇り空へと回復する見込みです。"
                    val koSubText = "앞으로 ${durationKo} 동안 비가 이어질 전망입니다(${timeLabelKo}경 그침 예상). 차차 흐린 날씨로 회복됩니다."
                    val enSubText = "Rain expected to continue for $durationEn (stopping around $timeLabelEn). Conditions will gradually improve."
                    val zhCnSubText = "预计降雨还将持续 $durationZh（$timeLabelZhCn 左右雨停），之后逐渐好转。"
                    val zhTwSubText = "預計降雨還將持續 $durationZh（$timeLabelZhTw 左右雨停），之後逐漸好轉。"
                    val esSubText = "Se espera lluvia durante $durationEs (parará sobre las $timeLabelEs). El tiempo mejorará gradualmente."
                    val frSubText = "Pluie prévue pendant $durationFr (fin vers $timeLabelFr). Le temps va s'améliorer progressivement."
                    val ptSubText = "Previsão de chuva por $durationPt (parando por volta das $timeLabelPt). O tempo deve melhorar gradualmente."
                    val ruSubText = "Дождь продлится $durationRu (закончится около $timeLabelRu). Погода постепенно улучшится."

                    createMultilingualTexts(
                        jaHeadline = jaHeadline,
                        koHeadline = koHeadline,
                        enHeadline = enHeadline,
                        zhCnHeadline = zhCnHeadline,
                        zhTwHeadline = zhTwHeadline,
                        esHeadline = esHeadline,
                        frHeadline = frHeadline,
                        ptHeadline = ptHeadline,
                        ruHeadline = ruHeadline,
                        jaSubText = jaSubText,
                        koSubText = koSubText,
                        enSubText = enSubText,
                        zhCnSubText = zhCnSubText,
                        zhTwSubText = zhTwSubText,
                        esSubText = esSubText,
                        frSubText = frSubText,
                        ptSubText = ptSubText,
                        ruSubText = ruSubText,
                        jaTimeLabel = timeLabelJa,
                        koTimeLabel = timeLabelKo,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw,
                        esTimeLabel = timeLabelEs,
                        frTimeLabel = timeLabelFr,
                        ptTimeLabel = timeLabelPt,
                        ruTimeLabel = timeLabelRu
                    )
                }
                diffMin in 91..180 -> {
                    createMultilingualTexts(
                        jaHeadline = "${timeLabelJa}頃まで雨が続く見込みです",
                        koHeadline = "${timeLabelKo}경까지 비가 이어질 전망입니다",
                        enHeadline = "Rain continuing until around $timeLabelEn",
                        zhCnHeadline = "预计降雨将持续至 $timeLabelZhCn 左右",
                        zhTwHeadline = "預計降雨將持續至 $timeLabelZhTw 左右",
                        esHeadline = "La lluvia continuará hasta las $timeLabelEs aprox.",
                        frHeadline = "La pluie continue jusqu'à environ $timeLabelFr",
                        ptHeadline = "A chuva continua até por volta das $timeLabelPt",
                        ruHeadline = "Дождь продолжится примерно до $timeLabelRu",
                        jaSubText = "あと${durationJa}雨が降り続く予想です。次第に雨脚が弱まる見込みです。",
                        koSubText = "앞으로 ${durationKo} 동안 비가 이어질 예상입니다. 빗줄기가 차차 약해질 전망입니다.",
                        enSubText = "Rain expected to continue for $durationEn. Conditions will gradually ease.",
                        zhCnSubText = "预计降雨还将持续 $durationZh，之后雨势逐渐减弱。",
                        zhTwSubText = "預計降雨還將持續 $durationZh，之後雨勢逐漸減弱。",
                        esSubText = "Se espera que la lluvia continúe durante $durationEs y amaine gradualmente.",
                        frSubText = "La pluie devrait durer encore $durationFr avant de faiblir progressivement.",
                        ptSubText = "A chuva deve continuar por $durationPt e enfraquecer gradualmente.",
                        ruSubText = "Дождь продлится $durationRu и постепенно ослабнет.",
                        jaTimeLabel = timeLabelJa,
                        koTimeLabel = timeLabelKo,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw,
                        esTimeLabel = timeLabelEs,
                        frTimeLabel = timeLabelFr,
                        ptTimeLabel = timeLabelPt,
                        ruTimeLabel = timeLabelRu
                    )
                }
                else -> {
                    val approxHours = ((diffMin + 29) / 60).coerceAtLeast(3)
                    createMultilingualTexts(
                        jaHeadline = "雨は当面の間（今後${approxHours}時間）降り続く見込みです",
                        koHeadline = "앞으로 ${approxHours}시간 동안 비가 계속될 전망입니다",
                        enHeadline = "Rain continuing for the next ${approxHours} hours",
                        zhCnHeadline = "预计降雨还将持续约${approxHours}小时",
                        zhTwHeadline = "預計降雨還將持續約${approxHours}小時",
                        esHeadline = "La lluvia continuará durante las próximas ${approxHours} horas",
                        frHeadline = "La pluie va continuer pendant les ${approxHours} prochaines heures",
                        ptHeadline = "A chuva deve continuar pelas próximas ${approxHours} horas",
                        ruHeadline = "Дождь продолжится в ближайшие ${approxHours} ч",
                        jaSubText = "${timeLabelJa}頃まで雨が降り続く予想です。お出かけやお帰りの際は雨具を必ずお持ちください。",
                        koSubText = "${timeLabelKo}경까지 비가 이어질 예상입니다. 외출 및 귀가 시 꼭 우산을 챙기세요.",
                        enSubText = "Rain expected to continue until around $timeLabelEn. Keep an umbrella handy.",
                        zhCnSubText = "预计降雨将持续至 $timeLabelZhCn 左右，出行请务必备好雨具。",
                        zhTwSubText = "預計降雨將持續至 $timeLabelZhTw 左右，出行請務必備好雨具。",
                        esSubText = "Se espera lluvia hasta las $timeLabelEs aprox. Ten siempre un paraguas a mano.",
                        frSubText = "Pluie attendue jusqu'à environ $timeLabelFr. Gardez un parapluie à portée de main.",
                        ptSubText = "Previsão de chuva até por volta das $timeLabelPt. Mantenha um guarda-chuva por perto.",
                        ruSubText = "Дождь ожидается примерно до $timeLabelRu. Держите зонт под рукой.",
                        jaTimeLabel = timeLabelJa,
                        koTimeLabel = timeLabelKo,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw,
                        esTimeLabel = timeLabelEs,
                        frTimeLabel = timeLabelFr,
                        ptTimeLabel = timeLabelPt,
                        ruTimeLabel = timeLabelRu
                    )
                }
            }

            // チャート用のMinutelyRainPoint (13点: 0分〜60分)
            val minutelyPoints = (0..12).map { idx ->
                val m = idx * 5
                val p = if (m >= diffMin) 0.0 else {
                    val ratio = (diffMin - m).toDouble() / diffMin.coerceAtLeast(1)
                    (effectivePrecip * ratio).coerceAtLeast(0.0)
                }
                MinutelyRainPoint(
                    timeLabel = "${m}m",
                    timestampMillis = nowMillis + m * 60 * 1000L,
                    precipMm = p,
                    probability = if (p > 0.0) 90 else 10
                )
            }

            return buildNowcast(
                status = RainStatus.RAINING,
                targetTimestampMillis = endTime,
                minutesRemaining = diffMin,
                currentIntensityMm = effectivePrecip,
                peakIntensityMm = peakIntensity,
                maxProbability = 90,
                minutelyPoints = minutelyPoints,
                isLiveActive = true,
                isPeak = false,
                isJapan = isJapan,
                isUS = isUS,
                isEurope = isEurope,
                multilingual = multilingual
            )
        } else {
            // 6時間以上降り続く場合
            val multilingual = createMultilingualTexts(
                jaHeadline = "雨は当面の間（6時間以上）降り続く見込みです",
                koHeadline = "앞으로 6시간 이상 비가 계속될 전망입니다",
                enHeadline = "Rain will continue for the next 6+ hours",
                zhCnHeadline = "预计降雨还将持续6小时以上",
                zhTwHeadline = "預計降雨還將持續6小時以上",
                esHeadline = "La lluvia continuará durante más de 6 horas",
                frHeadline = "La pluie va continuer pendant plus de 6 heures",
                ptHeadline = "A chuva deve continuar pelas próximas 6+ horas",
                jaSubText = "今後6時間以上、まとまった雨が降り続く予想です。お足元や視界の悪化にご注意ください。",
                koSubText = "앞으로 6시간 이상 비가 계속 내릴 전망입니다. 빗길 미끄럼과 시야 악화에 주의하세요.",
                enSubText = "Steady rain expected to continue for the next 6+ hours. Take care on wet roads.",
                zhCnSubText = "预计未来6小时以上持续有雨，外出请注意路面湿滑。",
                zhTwSubText = "預計未來6小時以上持續有雨，外出請注意路面濕滑。",
                esSubText = "Se espera lluvia constante durante más de 6 horas. Extrema la precaución en calzadas mojadas.",
                frSubText = "Pluie régulière attendue pendant plus de 6 heures. Soyez prudent sur les routes glissantes.",
                ptSubText = "Chuva constante prevista pelas próximas 6+ horas. Tenha cuidado em vias molhadas.",
                jaTimeLabel = "当面の間",
                koTimeLabel = "6시간 이상",
                enTimeLabel = "6+ hours",
                zhCnTimeLabel = "6小时以上",
                zhTwTimeLabel = "6小時以上",
                esTimeLabel = "Más de 6 h",
                frTimeLabel = "Plus de 6 h",
                ptTimeLabel = "6+ horas"
            )

            val minutelyPoints = (0..12).map { idx ->
                val m = idx * 5
                MinutelyRainPoint(
                    timeLabel = "${m}m",
                    timestampMillis = nowMillis + m * 60 * 1000L,
                    precipMm = effectivePrecip,
                    probability = 90
                )
            }

            return buildNowcast(
                status = RainStatus.RAINING,
                targetTimestampMillis = null,
                minutesRemaining = null,
                currentIntensityMm = effectivePrecip,
                peakIntensityMm = peakIntensity,
                maxProbability = 90,
                minutelyPoints = minutelyPoints,
                isLiveActive = true,
                isPeak = false,
                isJapan = isJapan,
                isUS = isUS,
                isEurope = isEurope,
                multilingual = multilingual
            )
        }
    }

    /**
     * デモ・テスト用のモック生成
     */
    fun createMock(status: RainStatus, nowMillis: Long = System.currentTimeMillis()): PrecipitationNowcast {
        return when (status) {
            RainStatus.RAIN_APPROACHING -> {
                val diffMin = 20
                val targetMillis = nowMillis + diffMin * 60 * 1000L
                val timeLabelJa = formatTo5MinTime(targetMillis, AppLanguage.JAPANESE)
                val timeLabelKo = formatTo5MinTime(targetMillis, AppLanguage.KOREAN)
                val timeLabelEn = formatTo5MinTime(targetMillis, AppLanguage.ENGLISH)
                val timeLabelZhCn = formatTo5MinTime(targetMillis, AppLanguage.SIMPLIFIED_CHINESE)
                val timeLabelZhTw = formatTo5MinTime(targetMillis, AppLanguage.TRADITIONAL_CHINESE)
                val timeLabelEs = formatTo5MinTime(targetMillis, AppLanguage.SPANISH)
                val timeLabelFr = formatTo5MinTime(targetMillis, AppLanguage.FRENCH)
                val timeLabelPt = formatTo5MinTime(targetMillis, AppLanguage.PORTUGUESE)
                val timeLabelRu = formatTo5MinTime(targetMillis, AppLanguage.RUSSIAN)

                val multilingual = createMultilingualTexts(
                    jaHeadline = "${timeLabelJa}頃から雨が降り始めます",
                    koHeadline = "${timeLabelKo}경부터 비가 내리기 시작합니다",
                    enHeadline = "Rain starting around $timeLabelEn",
                    zhCnHeadline = "预计 $timeLabelZhCn 左右开始下雨",
                    zhTwHeadline = "預計 $timeLabelZhTw 左右開始下雨",
                    esHeadline = "Empezará a llover sobre las $timeLabelEs",
                    frHeadline = "Début de la pluie vers $timeLabelFr",
                    ptHeadline = "Chuva começando por volta das $timeLabelPt",
                    ruHeadline = "Дождь начнётся около $timeLabelRu",
                    jaSubText = "あと約${diffMin}分で雨が降り始める見込みです。折りたたみ傘があると安心です。",
                    koSubText = "약 ${diffMin}분 후 비가 내리기 시작할 전망입니다. 접이식 우산을 챙기면 좋습니다.",
                    enSubText = "Rain is expected in about ${diffMin} min. An umbrella is recommended.",
                    zhCnSubText = "距降雨约 ${diffMin} 分钟，出门建议携带雨具。",
                    zhTwSubText = "距降雨約 ${diffMin} 分鐘，出門建議攜帶雨具。",
                    esSubText = "Se espera lluvia en unos ${diffMin} min. Se recomienda llevar paraguas.",
                    frSubText = "Pluie attendue dans environ ${diffMin} min. Un parapluie est conseillé.",
                    ptSubText = "Previsão de chuva em cerca de ${diffMin} min. Recomenda-se levar guarda-chuva.",
                    ruSubText = "Дождь ожидается примерно через ${diffMin} мин. Рекомендуется взять зонт.",
                    jaTimeLabel = timeLabelJa,
                    koTimeLabel = timeLabelKo,
                    enTimeLabel = timeLabelEn,
                    zhCnTimeLabel = timeLabelZhCn,
                    zhTwTimeLabel = timeLabelZhTw,
                    esTimeLabel = timeLabelEs,
                    frTimeLabel = timeLabelFr,
                    ptTimeLabel = timeLabelPt,
                    ruTimeLabel = timeLabelRu
                )

                buildNowcast(
                    status = RainStatus.RAIN_APPROACHING,
                    targetTimestampMillis = targetMillis,
                    minutesRemaining = diffMin,
                    currentIntensityMm = 0.0,
                    peakIntensityMm = 2.4,
                    maxProbability = 85,
                    minutelyPoints = generateMockMinutelyPoints(nowMillis, diffMin, isApproaching = true),
                    isLiveActive = true,
                    isPeak = false,
                    isJapan = true,
                    dataSourceName = "気象庁 (JMA)",
                    multilingual = multilingual
                )
            }
            RainStatus.RAINING -> {
                val diffMin = 35
                val targetMillis = nowMillis + diffMin * 60 * 1000L
                val timeLabelJa = formatTo5MinTime(targetMillis, AppLanguage.JAPANESE)
                val timeLabelKo = formatTo5MinTime(targetMillis, AppLanguage.KOREAN)
                val timeLabelEn = formatTo5MinTime(targetMillis, AppLanguage.ENGLISH)
                val timeLabelZhCn = formatTo5MinTime(targetMillis, AppLanguage.SIMPLIFIED_CHINESE)
                val timeLabelZhTw = formatTo5MinTime(targetMillis, AppLanguage.TRADITIONAL_CHINESE)
                val timeLabelEs = formatTo5MinTime(targetMillis, AppLanguage.SPANISH)
                val timeLabelFr = formatTo5MinTime(targetMillis, AppLanguage.FRENCH)
                val timeLabelPt = formatTo5MinTime(targetMillis, AppLanguage.PORTUGUESE)
                val timeLabelRu = formatTo5MinTime(targetMillis, AppLanguage.RUSSIAN)

                val multilingual = createMultilingualTexts(
                    jaHeadline = "${timeLabelJa}頃に雨が止む見込みです",
                    koHeadline = "${timeLabelKo}경 비가 그칠 전망입니다",
                    enHeadline = "Rain stopping around $timeLabelEn",
                    zhCnHeadline = "预计 $timeLabelZhCn 左右雨停",
                    zhTwHeadline = "預計 $timeLabelZhTw 左右雨停",
                    esHeadline = "Dejará de llover sobre las $timeLabelEs",
                    frHeadline = "Fin de la pluie vers $timeLabelFr",
                    ptHeadline = "A chuva deve parar por volta das $timeLabelPt",
                    ruHeadline = "Дождь закончится около $timeLabelRu",
                    jaSubText = "雨が止むまであと約${diffMin}分です。次第に曇り空へと回復する見込みです。",
                    koSubText = "비가 그치기까지 약 ${diffMin}분 남았습니다. 차차 흐린 날씨로 회복될 전망입니다.",
                    enSubText = "About ${diffMin} min until rain stops. Conditions will gradually improve.",
                    zhCnSubText = "距离雨停约 ${diffMin} 分钟，天气将逐渐好转。",
                    zhTwSubText = "距離雨停約 ${diffMin} 分鐘，天氣將逐漸好轉。",
                    esSubText = "Quedan unos ${diffMin} min para que deje de llover. El tiempo mejorará gradualmente.",
                    frSubText = "Environ ${diffMin} min avant la fin de la pluie. Le temps va s'améliorer progressivement.",
                    ptSubText = "Faltam cerca de ${diffMin} min para a chuva parar. O tempo deve melhorar gradualmente.",
                    ruSubText = "До окончания дождя около ${diffMin} мин. Погода постепенно улучшится.",
                    jaTimeLabel = timeLabelJa,
                    koTimeLabel = timeLabelKo,
                    enTimeLabel = timeLabelEn,
                    zhCnTimeLabel = timeLabelZhCn,
                    zhTwTimeLabel = timeLabelZhTw,
                    esTimeLabel = timeLabelEs,
                    frTimeLabel = timeLabelFr,
                    ptTimeLabel = timeLabelPt,
                    ruTimeLabel = timeLabelRu
                )

                buildNowcast(
                    status = RainStatus.RAINING,
                    targetTimestampMillis = targetMillis,
                    minutesRemaining = diffMin,
                    currentIntensityMm = 1.8,
                    peakIntensityMm = 3.5,
                    maxProbability = 95,
                    minutelyPoints = generateMockMinutelyPoints(nowMillis, diffMin, isApproaching = false),
                    isLiveActive = true,
                    isPeak = false,
                    isJapan = true,
                    dataSourceName = "気象庁 (JMA)",
                    multilingual = multilingual
                )
            }
            RainStatus.RAIN_EXPECTED_TONIGHT -> {
                val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                val tonightHour = if (cal.get(Calendar.HOUR_OF_DAY) >= 20) 22 else 21
                val multilingual = createMultilingualTexts(
                    jaHeadline = "${tonightHour}時頃から雨が降り出す見込みです",
                    koHeadline = "${tonightHour}시경부터 비가 올 전망입니다",
                    enHeadline = "Rain expected around ${tonightHour}:00",
                    zhCnHeadline = "预计 ${tonightHour}:00 左右开始下雨",
                    zhTwHeadline = "預計 ${tonightHour}:00 左右開始下雨",
                    esHeadline = "Se espera lluvia sobre las ${tonightHour}:00",
                    frHeadline = "Pluie attendue vers ${tonightHour}h00",
                    ptHeadline = "Previsão de chuva por volta das ${tonightHour}:00",
                    ruHeadline = "Дождь ожидается около ${tonightHour}:00",
                    jaSubText = "今夜にかけてまとまった雨雲が広がる予想です（降水確率80%）。夜間のお帰りの際は傘をお持ちください。",
                    koSubText = "오늘 밤 비구름이 확대될 전망입니다(강수확률 80%). 귀가 시 우산을 챙기세요.",
                    enSubText = "Rain clouds expected tonight (80% chance). Don't forget an umbrella for tonight.",
                    zhCnSubText = "今晚可能有降雨（降水概率 80%），晚归请备好雨具。",
                    zhTwSubText = "今晚可能有降雨（降水機率 80%），晚歸請備好雨具。",
                    esSubText = "Se esperan nubes de lluvia esta noche (80% de prob.). No olvides el paraguas.",
                    frSubText = "Nuages pluvieux attendus ce soir (80 % de risque). N'oubliez pas votre parapluie.",
                    ptSubText = "Nuvens de chuva previstas para esta noite (80% de chance). Não esqueça o guarda-chuva.",
                    ruSubText = "Сегодня вечером ожидаются дождевые облака (вероятность 80%). Не забудьте зонт.",
                    jaTimeLabel = "${tonightHour}時頃",
                    koTimeLabel = "${tonightHour}시경",
                    enTimeLabel = "${tonightHour}:00",
                    zhCnTimeLabel = "${tonightHour}:00",
                    zhTwTimeLabel = "${tonightHour}:00",
                    esTimeLabel = "${tonightHour}:00",
                    frTimeLabel = "${tonightHour}h00",
                    ptTimeLabel = "${tonightHour}:00",
                    ruTimeLabel = "${tonightHour}:00"
                )

                buildNowcast(
                    status = RainStatus.RAIN_EXPECTED_TONIGHT,
                    targetTimestampMillis = null,
                    minutesRemaining = null,
                    currentIntensityMm = 0.0,
                    peakIntensityMm = 1.5,
                    maxProbability = 80,
                    minutelyPoints = emptyList(),
                    isLiveActive = false,
                    isPeak = false,
                    isJapan = true,
                    dataSourceName = "気象庁 (JMA)",
                    multilingual = multilingual
                )
            }
            RainStatus.RAIN_EXPECTED_LATER -> {
                val multilingual = createMultilingualTexts(
                    jaHeadline = "明日の夕方以降は傘が必要です",
                    koHeadline = "내일 저녁부터 우산이 필요합니다",
                    enHeadline = "Umbrella needed tomorrow evening",
                    zhCnHeadline = "明天傍晚起需要带伞",
                    zhTwHeadline = "明天傍晚起需要帶傘",
                    esHeadline = "Necesitarás paraguas mañana al atardecer",
                    frHeadline = "Parapluie nécessaire demain en soirée",
                    ptHeadline = "Você vai precisar de guarda-chuva amanhã à noite",
                    ruHeadline = "Завтра вечером понадобится зонт",
                    jaSubText = "気象庁予報：降水確率70%の雨が予想されています。明日の外出時は雨具をご準備ください。",
                    koSubText = "기상청 예보: 강수확률 70%의 비가 예상됩니다. 내일 외출 시 우산을 준비하세요.",
                    enSubText = "JMA Forecast: 70% chance of rain expected. Prepare rain gear for tomorrow.",
                    zhCnSubText = "气象厅预报：预计降水概率为 70%，明天出门请备好雨具。",
                    zhTwSubText = "氣象廳預報：預計降水機率為 70%，明天出門請備好雨具。",
                    esSubText = "Pronóstico JMA: 70% de probabilidad de lluvia. Prepara ropa de lluvia para mañana.",
                    frSubText = "Prévision JMA : 70 % de risque de pluie. Prévoyez de quoi vous protéger demain.",
                    ptSubText = "Previsão JMA: 70% de chance de chuva. Prepare capa ou guarda-chuva para amanhã.",
                    ruSubText = "Прогноз JMA: вероятность дождя 70%. Подготовьте зонт на завтра.",
                    jaTimeLabel = "明日夕方以降",
                    koTimeLabel = "내일 저녁 이후",
                    enTimeLabel = "Tomorrow Evening",
                    zhCnTimeLabel = "明天傍晚起",
                    zhTwTimeLabel = "明天傍晚起",
                    esTimeLabel = "Mañana al atardecer",
                    frTimeLabel = "Demain en soirée",
                    ptTimeLabel = "Amanhã à noite",
                    ruTimeLabel = "Завтра вечером"
                )

                buildNowcast(
                    status = RainStatus.RAIN_EXPECTED_LATER,
                    targetTimestampMillis = null,
                    minutesRemaining = null,
                    currentIntensityMm = 0.0,
                    peakIntensityMm = 2.0,
                    maxProbability = 70,
                    minutelyPoints = emptyList(),
                    isLiveActive = false,
                    isPeak = false,
                    isJapan = true,
                    dataSourceName = "気象庁 (JMA)",
                    multilingual = multilingual
                )
            }
            RainStatus.DRY -> {
                val multilingual = createMultilingualTexts(
                    jaHeadline = "今日いっぱいは雨の心配はありません",
                    koHeadline = "오늘은 비 걱정이 없습니다",
                    enHeadline = "No rain expected today",
                    zhCnHeadline = "今天无需担心下雨",
                    zhTwHeadline = "今天無須擔心下雨",
                    esHeadline = "No se espera lluvia hoy",
                    frHeadline = "Pas de pluie prévue aujourd'hui",
                    ptHeadline = "Sem previsão de chuva hoje",
                    jaSubText = "当面雨は降らない見込みです。お出かけやお洗濯にも安心です。",
                    koSubText = "당분간 비 소식이 없습니다. 외출이나 빨래하기 좋은 날씨입니다.",
                    enSubText = "Clear conditions ahead. Great weather for going out or laundry.",
                    zhCnSubText = "预计近期无雨，适合外出与晾晒。",
                    zhTwSubText = "預計近期無雨，適合外出與晾曬。",
                    esSubText = "Sin lluvias a la vista. Buen tiempo para salir o tender la ropa.",
                    frSubText = "Temps sec à venir. Idéal pour sortir ou étendre le linge.",
                    ptSubText = "Sem chuva à vista. Ótimo tempo para sair ou estender roupas.",
                    jaTimeLabel = null,
                    koTimeLabel = null,
                    enTimeLabel = null,
                    zhCnTimeLabel = null,
                    zhTwTimeLabel = null,
                    esTimeLabel = null,
                    frTimeLabel = null,
                    ptTimeLabel = null
                )

                buildNowcast(
                    status = RainStatus.DRY,
                    targetTimestampMillis = null,
                    minutesRemaining = null,
                    currentIntensityMm = 0.0,
                    peakIntensityMm = 0.0,
                    maxProbability = 10,
                    minutelyPoints = emptyList(),
                    isLiveActive = false,
                    isPeak = false,
                    isJapan = true,
                    dataSourceName = "気象庁 (JMA)",
                    multilingual = multilingual
                )
            }
        }
    }

    private fun generateMockMinutelyPoints(nowMillis: Long, eventMinutes: Int, isApproaching: Boolean): List<MinutelyRainPoint> {
        val list = mutableListOf<MinutelyRainPoint>()
        for (i in 0..12) {
            val offsetMin = i * 5
            val t = nowMillis + offsetMin * 60 * 1000L
            val pMm = if (isApproaching) {
                if (offsetMin < eventMinutes) 0.0 else 1.8 + (i % 3) * 0.6
            } else {
                if (offsetMin < eventMinutes) 2.2 - (offsetMin.toDouble() / eventMinutes) * 2.0 else 0.0
            }
            list.add(
                MinutelyRainPoint(
                    timeLabel = formatToHHmm(t),
                    timestampMillis = t,
                    precipMm = pMm.coerceAtLeast(0.0),
                    probability = if (pMm > 0.05) 85 else 10
                )
            )
        }
        return list
    }
}

/**
 * 国名情報および座標からヨーロッパ領域かどうかを判定する
 */
fun isLocationOrCountryInEurope(country: String?, latitude: Double, longitude: Double): Boolean {
    // 日本または米国の領域であればヨーロッパではない
    if (isLocationOrCountryInJapan(country, latitude, longitude) ||
        isLocationOrCountryInUnitedStates(country, latitude, longitude)
    ) {
        return false
    }

    if (!country.isNullOrBlank()) {
        val trimmed = country.trim()
        val lower = trimmed.lowercase()
        val europeanKeywords = listOf(
            "france", "francia", "frança", "フランス", "法国", "法國",
            "germany", "alemania", "allemagne", "alemanha", "ドイツ", "德国", "德國", "deutschland",
            "united kingdom", "uk", "great britain", "reino unido", "royaume-uni", "イギリス", "英国", "英國",
            "italy", "italie", "itália", "イタリア", "意大利", "italia",
            "spain", "espagne", "espanha", "スペイン", "西班牙", "españa",
            "switzerland", "suiza", "suíça", "スイス", "瑞士", "schweiz", "suisse",
            "netherlands", "países bajos", "pays-bas", "países baixos", "オランダ", "荷兰", "荷蘭", "nederland",
            "belgium", "bélgica", "ベルギー", "比利时", "比利時", "belgique",
            "sweden", "suecia", "suède", "suécia", "スウェーデン", "瑞典", "sverige",
            "norway", "noruega", "norvège", "ノルウェー", "挪威", "norge",
            "finland", "finlandia", "finlande", "finlândia", "フィンランド", "芬兰", "芬蘭", "suomi",
            "denmark", "dinamarca", "danemark", "デンマーク", "丹麦", "丹麥", "danmark",
            "austria", "autriche", "áustria", "オーストリア", "奥地利", "奧地利", "österreich",
            "portugal", "ポルトガル", "葡萄牙",
            "ireland", "irlanda", "irlande", "アイルランド", "爱尔兰", "愛爾蘭",
            "poland", "polonia", "pologne", "polônia", "polónia", "ポーランド", "波兰", "波蘭", "polska",
            "czech", "chequia", "tchéquie", "tchéquia", "チェコ", "捷克", "česko",
            "greece", "grecia", "grèce", "grécia", "ギリシャ", "希腊", "希臘", "hellas",
            "hungary", "hungría", "hongrie", "hungria", "ハンガリー", "匈牙利", "magyarország",
            "romania", "rumanía", "rumania", "roumanie", "romênia", "roménia", "ルーマニア", "罗马尼亚", "羅馬尼亞",
            "ukraine", "ucrania", "ucrânia", "ウクライナ", "乌克兰", "烏克蘭",
            "europe", "europa", "ヨーロッパ", "欧洲", "歐洲"
        )
        if (europeanKeywords.any { lower.contains(it) || trimmed.contains(it) }) {
            return true
        }
        val isoCodes = setOf("FR", "DE", "GB", "UK", "IT", "ES", "CH", "NL", "BE", "SE", "NO", "FI", "DK", "AT", "PT", "IE", "PL", "CZ", "GR", "HU", "RO", "UA")
        if (isoCodes.contains(trimmed.uppercase())) {
            return true
        }
    }

    // ヨーロッパの緯度経度バウンディングボックス: 北緯34度〜72度, 西経25度〜東経45度
    return latitude in 34.0..72.0 && longitude in -25.0..45.0
}

