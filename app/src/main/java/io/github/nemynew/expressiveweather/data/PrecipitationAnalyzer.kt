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
        jmaMinutelySeries: List<Pair<Long, Double>>? = null
    ): PrecipitationNowcast {
        val isJapan = if (latitude != null && longitude != null) {
            isLocationOrCountryInJapan(country, latitude, longitude)
        } else if (jmaOverview != null) {
            true
        } else {
            val c = country?.trim() ?: ""
            c.contains("日本") || c.contains("Japan", ignoreCase = true) || c.equals("JP", ignoreCase = true) || c.equals("JPN", ignoreCase = true)
        }

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
            jmaMinutelySeries = jmaMinutelySeries
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
                isEurope = isEurope
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
                isEurope = isEurope
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
        isEurope: Boolean = false
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
                    val timeLabelJa = formatTo5MinTime(startTime, AppLanguage.JAPANESE)
                    val timeLabelEn = formatTo5MinTime(startTime, AppLanguage.ENGLISH)
                    val timeLabelZhCn = formatTo5MinTime(startTime, AppLanguage.SIMPLIFIED_CHINESE)
                    val timeLabelZhTw = formatTo5MinTime(startTime, AppLanguage.TRADITIONAL_CHINESE)

                    val jaHeadline = if (diffMinutes < 15) {
                        if (isHeavy) "まもなく激しい雨が降ります" else "まもなく雨が降り始めます"
                    } else {
                        if (isHeavy) "${timeLabelJa}頃から激しい雨が降ります" else "${timeLabelJa}頃から雨が降り始めます"
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

                    val jaSubText = if (isHeavy) {
                        "強い雨雲が迫っています（予想雨量 約${formatPrecipMm(maxPrecipInEvent)}mm/h）。安全な屋内へ避難してください。"
                    } else if (diffMinutes < 15) {
                        "あと約${diffMinutes}分で雨が降り始める見込みです。お出かけの際は傘をご用意ください。"
                    } else {
                        "あと約${diffMinutes}分で雨が降り始める見込みです。折りたたみ傘があると安心です。"
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

                    val multilingual = createMultilingualTexts(
                        jaHeadline = jaHeadline,
                        enHeadline = enHeadline,
                        zhCnHeadline = zhCnHeadline,
                        zhTwHeadline = zhTwHeadline,
                        jaSubText = jaSubText,
                        enSubText = enSubText,
                        zhCnSubText = zhCnSubText,
                        zhTwSubText = zhTwSubText,
                        jaTimeLabel = timeLabelJa,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw
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
                    val timeLabelJa = roundToNearest10Min(startTime, AppLanguage.JAPANESE)
                    val timeLabelEn = roundToNearest10Min(startTime, AppLanguage.ENGLISH)
                    val timeLabelZhCn = roundToNearest10Min(startTime, AppLanguage.SIMPLIFIED_CHINESE)
                    val timeLabelZhTw = roundToNearest10Min(startTime, AppLanguage.TRADITIONAL_CHINESE)

                    val multilingual = createMultilingualTexts(
                        jaHeadline = "${timeLabelJa}頃から雨が降り始める見込みです",
                        enHeadline = "Rain expected around $timeLabelEn",
                        zhCnHeadline = "预计 $timeLabelZhCn 左右开始下雨",
                        zhTwHeadline = "預計 $timeLabelZhTw 左右開始下雨",
                        jaSubText = "${timeLabelJa}頃から雨が降り始める予想です（予想雨量 約${formatPrecipMm(maxPrecipInEvent)}mm/h）。お出かけの際は雨具をお持ちください。",
                        enSubText = "Rain expected around $timeLabelEn (~${formatPrecipMm(maxPrecipInEvent)} mm/h). Bring an umbrella if heading out.",
                        zhCnSubText = "预计 $timeLabelZhCn 左右开始降雨（预计约 ${formatPrecipMm(maxPrecipInEvent)} mm/h），外出请带伞。",
                        zhTwSubText = "預計 $timeLabelZhTw 左右開始降雨（預計約 ${formatPrecipMm(maxPrecipInEvent)} mm/h），外出請帶傘。",
                        jaTimeLabel = timeLabelJa,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw
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
                    val roundedHourJa = roundToNearestHour(startTime, AppLanguage.JAPANESE)
                    val roundedHourEn = roundToNearestHour(startTime, AppLanguage.ENGLISH)
                    val roundedHourZhCn = roundToNearestHour(startTime, AppLanguage.SIMPLIFIED_CHINESE)
                    val roundedHourZhTw = roundToNearestHour(startTime, AppLanguage.TRADITIONAL_CHINESE)

                    val multilingual = createMultilingualTexts(
                        jaHeadline = "${roundedHourJa}頃から雨が降り出す見込みです",
                        enHeadline = "Rain expected around $roundedHourEn",
                        zhCnHeadline = "预计 $roundedHourZhCn 左右开始下雨",
                        zhTwHeadline = "預計 $roundedHourZhTw 左右開始下雨",
                        jaSubText = "今夜にかけてまとまった雨雲が広がる予想です（降水確率${pop}%）。夜間のお帰りの際は傘をお持ちください。",
                        enSubText = "Rain clouds expected tonight (${pop}% chance). Don't forget an umbrella for tonight.",
                        zhCnSubText = "今晚可能有降雨（降水概率 ${pop}%），晚归请备好雨具。",
                        zhTwSubText = "今晚可能有降雨（降水機率 ${pop}%），晚歸請備好雨具。",
                        jaTimeLabel = "${roundedHourJa}頃",
                        enTimeLabel = roundedHourEn,
                        zhCnTimeLabel = roundedHourZhCn,
                        zhTwTimeLabel = roundedHourZhTw
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
            val (headlineJa, labelJa) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.JAPANESE)
            val (headlineEn, labelEn) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.ENGLISH)
            val (headlineZhCn, labelZhCn) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.SIMPLIFIED_CHINESE)
            val (headlineZhTw, labelZhTw) = formatTier4TimeOfDayExpression(rainyBlock.timestampMillis, AppLanguage.TRADITIONAL_CHINESE)

            val pop = rainyBlock.precipProbability
            val (sourceJa, sourceEn, sourceZhCn, sourceZhTw) = when {
                isJapan -> listOf("気象庁予報", "JMA Forecast", "气象厅预报", "氣象廳預報")
                isUS -> listOf("NOAA/NWS予報", "NOAA/NWS Forecast", "NOAA/NWS预报", "NOAA/NWS預報")
                isEurope -> listOf("ECMWF予報", "ECMWF Forecast", "ECMWF预报", "ECMWF預報")
                else -> listOf("国際数値予報", "Global NWP Forecast", "国际数值预报", "國際數值預報")
            }

            val multilingual = createMultilingualTexts(
                jaHeadline = headlineJa,
                enHeadline = headlineEn,
                zhCnHeadline = headlineZhCn,
                zhTwHeadline = headlineZhTw,
                jaSubText = "${sourceJa}：降水確率${pop}%の雨が予想されています。明日の外出時は雨具をご準備ください。",
                enSubText = "${sourceEn}: ${pop}% chance of rain expected. Prepare rain gear for tomorrow.",
                zhCnSubText = "${sourceZhCn}：预计降水概率为 ${pop}%，明天出门请备好雨具。",
                zhTwSubText = "${sourceZhTw}：預計降水機率為 ${pop}%，明天出門請備好雨具。",
                jaTimeLabel = labelJa,
                enTimeLabel = labelEn,
                zhCnTimeLabel = labelZhCn,
                zhTwTimeLabel = labelZhTw
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
                enHeadline = "Rain expected tonight",
                zhCnHeadline = "预计今晚有雨",
                zhTwHeadline = "預計今晚有雨",
                jaSubText = "気象庁予報：今夜遅くにかけて所により雨が降る見込みです。お帰りの際はご注意ください。",
                enSubText = "JMA Forecast: Rain expected in some areas late tonight. Carry an umbrella when heading home.",
                zhCnSubText = "气象厅预报：今晚局部地区有雨，晚归请注意携带雨伞。",
                zhTwSubText = "氣象廳預報：今晚局部地區有雨，晚歸請注意攜帶雨傘。",
                jaTimeLabel = "今夜遅く",
                enTimeLabel = "Late Tonight",
                zhCnTimeLabel = "今晚较晚",
                zhTwTimeLabel = "今晚較晚"
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
            enHeadline = "No rain expected today",
            zhCnHeadline = "今天无需担心下雨",
            zhTwHeadline = "今天無須擔心下雨",
            jaSubText = "当面雨は降らない見込みです。お出かけやお洗濯にも安心です。",
            enSubText = "Clear conditions ahead. Great weather for going out or laundry.",
            zhCnSubText = "预计近期无雨，适合外出与晾晒。",
            zhTwSubText = "預計近期無雨，適合外出與晾曬。",
            jaTimeLabel = null,
            enTimeLabel = null,
            zhCnTimeLabel = null,
            zhTwTimeLabel = null
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
        isEurope: Boolean = false
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
            val timeLabelJa = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.JAPANESE) else roundToNearest10Min(peakTime, AppLanguage.JAPANESE)
            val timeLabelEn = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.ENGLISH) else roundToNearest10Min(peakTime, AppLanguage.ENGLISH)
            val timeLabelZhCn = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.SIMPLIFIED_CHINESE) else roundToNearest10Min(peakTime, AppLanguage.SIMPLIFIED_CHINESE)
            val timeLabelZhTw = if (diffMin <= 60) formatTo5MinTime(peakTime, AppLanguage.TRADITIONAL_CHINESE) else roundToNearest10Min(peakTime, AppLanguage.TRADITIONAL_CHINESE)

            val multilingual = createMultilingualTexts(
                jaHeadline = "${timeLabelJa}頃から雨がさらに強まる見込みです",
                enHeadline = "Rain intensifying around $timeLabelEn",
                zhCnHeadline = "预计 $timeLabelZhCn 左右雨势将加剧",
                zhTwHeadline = "預計 $timeLabelZhTw 左右雨勢將加劇",
                jaSubText = "現在より雨脚が強まり、激しい雨（約${formatPrecipMm(peakFrame.precipMmPerHour)}mm/h）となる恐れがあります。視界悪化にご注意ください。",
                enSubText = "Rain will become heavier (est. ~${formatPrecipMm(peakFrame.precipMmPerHour)} mm/h). Watch out for poor visibility.",
                zhCnSubText = "雨势将进一步加大（约 ${formatPrecipMm(peakFrame.precipMmPerHour)} mm/h），请注意低能见度与安全。",
                zhTwSubText = "雨勢將進一步加大（約 ${formatPrecipMm(peakFrame.precipMmPerHour)} mm/h），請注意低能見度與安全。",
                jaTimeLabel = timeLabelJa,
                enTimeLabel = timeLabelEn,
                zhCnTimeLabel = timeLabelZhCn,
                zhTwTimeLabel = timeLabelZhTw
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
            val timeLabelJa = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.JAPANESE) else roundToNearest10Min(endTime, AppLanguage.JAPANESE)
            val timeLabelEn = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.ENGLISH) else roundToNearest10Min(endTime, AppLanguage.ENGLISH)
            val timeLabelZhCn = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.SIMPLIFIED_CHINESE) else roundToNearest10Min(endTime, AppLanguage.SIMPLIFIED_CHINESE)
            val timeLabelZhTw = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.TRADITIONAL_CHINESE) else roundToNearest10Min(endTime, AppLanguage.TRADITIONAL_CHINESE)

            val hours = diffMin / 60
            val mins = diffMin % 60
            val durationJa = when {
                hours > 0 && mins > 0 -> "約${hours}時間${mins}分"
                hours > 0 -> "約${hours}時間"
                else -> "約${mins}分"
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

            when {
                // A. 短時間 (90分以内): 「もうすぐ雨が止む」
                diffMin <= 90 -> {
                    val jaHeadline = if (diffMin < 15) "まもなく雨が止む見込みです" else "${timeLabelJa}頃に雨が止む見込みです"
                    val enHeadline = if (diffMin < 15) "Rain stopping shortly" else "Rain stopping around $timeLabelEn"
                    val zhCnHeadline = if (diffMin < 15) "雨很快就会停" else "预计 $timeLabelZhCn 左右雨停"
                    val zhTwHeadline = if (diffMin < 15) "雨很快就會停" else "預計 $timeLabelZhTw 左右雨停"

                    val jaSubText = "あと${durationJa}雨が降り続く見込みです（${timeLabelJa}頃に降り止み予想）。次第に曇り空へと回復する見込みです。"
                    val enSubText = "Rain expected to continue for $durationEn (stopping around $timeLabelEn). Conditions will gradually improve."
                    val zhCnSubText = "预计降雨还将持续 $durationZh（$timeLabelZhCn 左右雨停），之后逐渐好转。"
                    val zhTwSubText = "預計降雨還將持續 $durationZh（$timeLabelZhTw 左右雨停），之後逐漸好轉。"

                    val multilingual = createMultilingualTexts(
                        jaHeadline = jaHeadline,
                        enHeadline = enHeadline,
                        zhCnHeadline = zhCnHeadline,
                        zhTwHeadline = zhTwHeadline,
                        jaSubText = jaSubText,
                        enSubText = enSubText,
                        zhCnSubText = zhCnSubText,
                        zhTwSubText = zhTwSubText,
                        jaTimeLabel = timeLabelJa,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw
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
                    val enHeadline = "Rain continuing until around $timeLabelEn"
                    val zhCnHeadline = "预计降雨将持续至 $timeLabelZhCn 左右"
                    val zhTwHeadline = "預計降雨將持續至 $timeLabelZhTw 左右"

                    val jaSubText = "あと${durationJa}雨が降り続く予想です。次第に雨脚が弱まる見込みです。"
                    val enSubText = "Rain expected to continue for $durationEn. Conditions will gradually ease."
                    val zhCnSubText = "预计降雨还将持续 $durationZh，之后雨势逐渐减弱。"
                    val zhTwSubText = "預計降雨還將持續 $durationZh，之後雨勢逐漸減弱。"

                    val multilingual = createMultilingualTexts(
                        jaHeadline = jaHeadline,
                        enHeadline = enHeadline,
                        zhCnHeadline = zhCnHeadline,
                        zhTwHeadline = zhTwHeadline,
                        jaSubText = jaSubText,
                        enSubText = enSubText,
                        zhCnSubText = zhCnSubText,
                        zhTwSubText = zhTwSubText,
                        jaTimeLabel = timeLabelJa,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw
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
                    val enHeadline = "Rain continuing for the next ${approxHours} hours"
                    val zhCnHeadline = "预计降雨还将持续约${approxHours}小时"
                    val zhTwHeadline = "預計降雨還將持續約${approxHours}小時"

                    val jaSubText = "${timeLabelJa}頃まで雨が降り続く予想です。お出かけやお帰りの際は雨具を必ずお持ちください。"
                    val enSubText = "Rain expected to continue until around $timeLabelEn. Keep an umbrella handy."
                    val zhCnSubText = "预计降雨将持续至 $timeLabelZhCn 左右，出行请务必备好雨具。"
                    val zhTwSubText = "預計降雨將持續至 $timeLabelZhTw 左右，出行請務必備好雨具。"

                    val multilingual = createMultilingualTexts(
                        jaHeadline = jaHeadline,
                        enHeadline = enHeadline,
                        zhCnHeadline = zhCnHeadline,
                        zhTwHeadline = zhTwHeadline,
                        jaSubText = jaSubText,
                        enSubText = enSubText,
                        zhCnSubText = zhCnSubText,
                        zhTwSubText = zhTwSubText,
                        jaTimeLabel = timeLabelJa,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw
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
            enHeadline = "Rain will continue for the next 6+ hours",
            zhCnHeadline = "预计降雨还将持续6小时以上",
            zhTwHeadline = "預計降雨還將持續6小時以上",
            jaSubText = "今後6時間以上、まとまった雨が降り続く予想です。お足元や視界の悪化にご注意ください。",
            enSubText = "Steady rain expected to continue for the next 6+ hours. Take care on wet roads.",
            zhCnSubText = "预计未来6小时以上持续有雨，外出请注意路面湿滑。",
            zhTwSubText = "預計未來6小時以上持續有雨，外出請注意路面濕滑。",
            jaTimeLabel = "当面の間",
            enTimeLabel = "6+ hours",
            zhCnTimeLabel = "6小时以上",
            zhTwTimeLabel = "6小時以上"
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
        jmaMinutelySeries: List<Pair<Long, Double>>? = null
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
                val tMillis = parseIsoToMillis(minutelyTimes[i]) ?: continue
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
                timeLabel = formatToHHmm(nowMillis)
            )
        )

        // =====================================================================
        // Tier 1: 直近60分 (高解像度降水ナウキャスト 5分間隔, 12フレーム)
        // =====================================================================
        for (offsetMin in 5..60 step 5) {
            val t = nowMillis + offsetMin * 60 * 1000L
            val pMm = samplePrecip(t, parsedMinutely, hourlyForecast, nowMillis, nowPrecip)
            val pop = samplePop(t, popSlots, hourlyForecast, pMm)
            timeline.add(
                TimelineFrame(
                    timestampMillis = t,
                    minutesFromNow = offsetMin,
                    precipMmPerHour = pMm,
                    precipProbability = pop,
                    tier = TimelineTier.TIER1_NOWCAST_5MIN,
                    timeLabel = formatToHHmm(t)
                )
            )
        }

        // =====================================================================
        // Tier 2: 1時間後から6時間後まで (降水短時間予報 10分間隔, 30フレーム)
        // =====================================================================
        for (offsetMin in 70..360 step 10) {
            val t = nowMillis + offsetMin * 60 * 1000L
            val pMm = samplePrecip(t, parsedMinutely, hourlyForecast, nowMillis, nowPrecip)
            val pop = samplePop(t, popSlots, hourlyForecast, pMm)
            timeline.add(
                TimelineFrame(
                    timestampMillis = t,
                    minutesFromNow = offsetMin,
                    precipMmPerHour = pMm,
                    precipProbability = pop,
                    tier = TimelineTier.TIER2_SHORT_FORECAST_10MIN,
                    timeLabel = roundToNearest10Min(t)
                )
            )
        }

        // =====================================================================
        // Tier 3: 6時間後から15時間後まで (降水短時間予報 1時間間隔, 9フレーム)
        // =====================================================================
        for (offsetHour in 7..15) {
            val offsetMin = offsetHour * 60
            val t = nowMillis + offsetHour * 3600 * 1000L
            val pMm = samplePrecip(t, parsedMinutely, hourlyForecast, nowMillis, nowPrecip)
            val pop = samplePop(t, popSlots, hourlyForecast, pMm)
            timeline.add(
                TimelineFrame(
                    timestampMillis = t,
                    minutesFromNow = offsetMin,
                    precipMmPerHour = pMm,
                    precipProbability = pop,
                    tier = TimelineTier.TIER3_SHORT_FORECAST_1H,
                    timeLabel = roundToNearestHour(t)
                )
            )
        }

        // =====================================================================
        // Tier 4: 15時間以降から翌日 (府県天気予報 6時間ブロック)
        // =====================================================================
        val calBlock = Calendar.getInstance().apply {
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
                    timeLabel = formatTier4TimeOfDayExpression(bStart).second
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
        nowPrecip: Double
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

        val cal = Calendar.getInstance().apply { timeInMillis = targetMillis }
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
        precipMm: Double
    ): Int {
        if (!popSlots.isNullOrEmpty()) {
            val matched = popSlots.lastOrNull { targetMillis >= it.startMillis }
            if (matched != null) return matched.pop
        }
        val cal = Calendar.getInstance().apply { timeInMillis = targetMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val hourlyMatched = hourlyForecast.firstOrNull { it.hour == hour }
        if (hourlyMatched != null) return hourlyMatched.precipProb

        return if (precipMm >= THRESHOLD_RAIN_START) 80 else if (precipMm > (THRESHOLD_RAIN_STOP + 0.0001)) 40 else 10
    }

    /**
     * 10分単位の丸め時刻文字列の生成 (例: "14時20分" / "14:20")
     */
    fun roundToNearest10Min(millis: Long, language: AppLanguage = AppLanguage.JAPANESE): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
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
            else -> String.format(Locale.US, "%02d:%02d", h, m)
        }
    }

    /**
     * 1時間単位の丸め時刻文字列の生成 (例: "19時" / "19:00")
     */
    fun roundToNearestHour(millis: Long, language: AppLanguage = AppLanguage.JAPANESE): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val minute = cal.get(Calendar.MINUTE)
        if (minute >= 30) {
            cal.add(Calendar.HOUR_OF_DAY, 1)
        }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        return when (language) {
            AppLanguage.JAPANESE -> "${h}時"
            else -> String.format(Locale.US, "%02d:00", h)
        }
    }

    /**
     * 5分単位の時刻文字列の生成 (例: "14時25分" / "14:25")
     */
    fun formatTo5MinTime(millis: Long, language: AppLanguage = AppLanguage.JAPANESE): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
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
            else -> String.format(Locale.US, "%02d:%02d", h, m)
        }
    }

    /**
     * 翌日・15時間以降の時間帯表現 (例: "明日の朝から雨が降る見込みです")
     */
    fun formatTier4TimeOfDayExpression(blockStartMillis: Long, language: AppLanguage = AppLanguage.JAPANESE): Pair<String, String> {
        val cal = Calendar.getInstance().apply { timeInMillis = blockStartMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when (language) {
            AppLanguage.JAPANESE -> when (hour) {
                in 0..5 -> Pair("明日の未明から雨が降る見込みです", "明日未明")
                in 6..11 -> Pair("明日の朝から雨が降る見込みです", "明日朝")
                in 12..17 -> Pair("明日の午後から雨が降る見込みです", "明日午後")
                else -> Pair("明日の夕方以降は傘が必要です", "明日夕方以降")
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
        }
    }

    private fun createMultilingualTexts(
        jaHeadline: String,
        enHeadline: String,
        zhCnHeadline: String,
        zhTwHeadline: String,
        jaSubText: String,
        enSubText: String,
        zhCnSubText: String,
        zhTwSubText: String,
        jaTimeLabel: String? = null,
        enTimeLabel: String? = null,
        zhCnTimeLabel: String? = null,
        zhTwTimeLabel: String? = null
    ): Triple<Map<String, String>, Map<String, String>, Map<String, String>> {
        val headlines = mapOf(
            AppLanguage.JAPANESE.code to jaHeadline,
            AppLanguage.ENGLISH.code to enHeadline,
            AppLanguage.SIMPLIFIED_CHINESE.code to zhCnHeadline,
            AppLanguage.TRADITIONAL_CHINESE.code to zhTwHeadline
        )
        val subTexts = mapOf(
            AppLanguage.JAPANESE.code to jaSubText,
            AppLanguage.ENGLISH.code to enSubText,
            AppLanguage.SIMPLIFIED_CHINESE.code to zhCnSubText,
            AppLanguage.TRADITIONAL_CHINESE.code to zhTwSubText
        )
        val timeLabels = mutableMapOf<String, String>()
        if (jaTimeLabel != null) timeLabels[AppLanguage.JAPANESE.code] = jaTimeLabel
        if (enTimeLabel != null) timeLabels[AppLanguage.ENGLISH.code] = enTimeLabel
        if (zhCnTimeLabel != null) timeLabels[AppLanguage.SIMPLIFIED_CHINESE.code] = zhCnTimeLabel
        if (zhTwTimeLabel != null) timeLabels[AppLanguage.TRADITIONAL_CHINESE.code] = zhTwTimeLabel

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

    private fun formatToHHmm(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(Locale.JAPAN, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun formatPrecipMm(mm: Double): String {
        return String.format(Locale.US, "%.1f", mm)
    }

    private fun parseIsoToMillis(isoStr: String): Long? {
        return try {
            val clean = if (isoStr.length >= 16) isoStr.substring(0, 16) else isoStr
            val fmt = isoFormatThreadLocal.get() ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            fmt.timeZone = TimeZone.getDefault()
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
            val timeLabelJa = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.JAPANESE) else roundToNearest10Min(endTime, AppLanguage.JAPANESE)
            val timeLabelEn = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.ENGLISH) else roundToNearest10Min(endTime, AppLanguage.ENGLISH)
            val timeLabelZhCn = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.SIMPLIFIED_CHINESE) else roundToNearest10Min(endTime, AppLanguage.SIMPLIFIED_CHINESE)
            val timeLabelZhTw = if (diffMin <= 60) formatTo5MinTime(endTime, AppLanguage.TRADITIONAL_CHINESE) else roundToNearest10Min(endTime, AppLanguage.TRADITIONAL_CHINESE)

            val hours = diffMin / 60
            val mins = diffMin % 60
            val durationJa = when {
                hours > 0 && mins > 0 -> "約${hours}時間${mins}分"
                hours > 0 -> "約${hours}時間"
                else -> "約${mins}分"
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

            val multilingual = when {
                diffMin <= 90 -> {
                    val jaHeadline = if (diffMin < 15) "まもなく雨が止む見込みです" else "${timeLabelJa}頃に雨が止む見込みです"
                    val enHeadline = if (diffMin < 15) "Rain stopping shortly" else "Rain stopping around $timeLabelEn"
                    val zhCnHeadline = if (diffMin < 15) "雨很快就会停" else "预计 $timeLabelZhCn 左右雨停"
                    val zhTwHeadline = if (diffMin < 15) "雨很快就會停" else "預計 $timeLabelZhTw 左右雨停"

                    val jaSubText = "あと${durationJa}雨が降り続く見込みです（${timeLabelJa}頃に降り止み予想）。次第に曇り空へと回復する見込みです。"
                    val enSubText = "Rain expected to continue for $durationEn (stopping around $timeLabelEn). Conditions will gradually improve."
                    val zhCnSubText = "预计降雨还将持续 $durationZh（$timeLabelZhCn 左右雨停），之后逐渐好转。"
                    val zhTwSubText = "預計降雨還將持續 $durationZh（$timeLabelZhTw 左右雨停），之後逐漸好轉。"

                    createMultilingualTexts(
                        jaHeadline = jaHeadline,
                        enHeadline = enHeadline,
                        zhCnHeadline = zhCnHeadline,
                        zhTwHeadline = zhTwHeadline,
                        jaSubText = jaSubText,
                        enSubText = enSubText,
                        zhCnSubText = zhCnSubText,
                        zhTwSubText = zhTwSubText,
                        jaTimeLabel = timeLabelJa,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw
                    )
                }
                diffMin in 91..180 -> {
                    createMultilingualTexts(
                        jaHeadline = "${timeLabelJa}頃まで雨が続く見込みです",
                        enHeadline = "Rain continuing until around $timeLabelEn",
                        zhCnHeadline = "预计降雨将持续至 $timeLabelZhCn 左右",
                        zhTwHeadline = "預計降雨將持續至 $timeLabelZhTw 左右",
                        jaSubText = "あと${durationJa}雨が降り続く予想です。次第に雨脚が弱まる見込みです。",
                        enSubText = "Rain expected to continue for $durationEn. Conditions will gradually ease.",
                        zhCnSubText = "预计降雨还将持续 $durationZh，之后雨势逐渐减弱。",
                        zhTwSubText = "預計降雨還將持續 $durationZh，之後雨勢逐漸減弱。",
                        jaTimeLabel = timeLabelJa,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw
                    )
                }
                else -> {
                    val approxHours = ((diffMin + 29) / 60).coerceAtLeast(3)
                    createMultilingualTexts(
                        jaHeadline = "雨は当面の間（今後${approxHours}時間）降り続く見込みです",
                        enHeadline = "Rain continuing for the next ${approxHours} hours",
                        zhCnHeadline = "预计降雨还将持续约${approxHours}小时",
                        zhTwHeadline = "預計降雨還將持續約${approxHours}小時",
                        jaSubText = "${timeLabelJa}頃まで雨が降り続く予想です。お出かけやお帰りの際は雨具を必ずお持ちください。",
                        enSubText = "Rain expected to continue until around $timeLabelEn. Keep an umbrella handy.",
                        zhCnSubText = "预计降雨将持续至 $timeLabelZhCn 左右，出行请务必备好雨具。",
                        zhTwSubText = "預計降雨將持續至 $timeLabelZhTw 左右，出行請務必備好雨具。",
                        jaTimeLabel = timeLabelJa,
                        enTimeLabel = timeLabelEn,
                        zhCnTimeLabel = timeLabelZhCn,
                        zhTwTimeLabel = timeLabelZhTw
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
                enHeadline = "Rain will continue for the next 6+ hours",
                zhCnHeadline = "预计降雨还将持续6小时以上",
                zhTwHeadline = "預計降雨還將持續6小時以上",
                jaSubText = "今後6時間以上、まとまった雨が降り続く予想です。お足元や視界の悪化にご注意ください。",
                enSubText = "Steady rain expected to continue for the next 6+ hours. Take care on wet roads.",
                zhCnSubText = "预计未来6小时以上持续有雨，外出请注意路面湿滑。",
                zhTwSubText = "預計未來6小時以上持續有雨，外出請注意路面濕滑。",
                jaTimeLabel = "当面の間",
                enTimeLabel = "6+ hours",
                zhCnTimeLabel = "6小时以上",
                zhTwTimeLabel = "6小時以上"
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
                val timeLabelEn = formatTo5MinTime(targetMillis, AppLanguage.ENGLISH)
                val timeLabelZhCn = formatTo5MinTime(targetMillis, AppLanguage.SIMPLIFIED_CHINESE)
                val timeLabelZhTw = formatTo5MinTime(targetMillis, AppLanguage.TRADITIONAL_CHINESE)

                val multilingual = createMultilingualTexts(
                    jaHeadline = "${timeLabelJa}頃から雨が降り始めます",
                    enHeadline = "Rain starting around $timeLabelEn",
                    zhCnHeadline = "预计 $timeLabelZhCn 左右开始下雨",
                    zhTwHeadline = "預計 $timeLabelZhTw 左右開始下雨",
                    jaSubText = "あと約${diffMin}分で雨が降り始める見込みです。折りたたみ傘があると安心です。",
                    enSubText = "Rain is expected in about ${diffMin} min. An umbrella is recommended.",
                    zhCnSubText = "距降雨约 ${diffMin} 分钟，出门建议携带雨具。",
                    zhTwSubText = "距降雨約 ${diffMin} 分鐘，出門建議攜帶雨具。",
                    jaTimeLabel = timeLabelJa,
                    enTimeLabel = timeLabelEn,
                    zhCnTimeLabel = timeLabelZhCn,
                    zhTwTimeLabel = timeLabelZhTw
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
                val timeLabelEn = formatTo5MinTime(targetMillis, AppLanguage.ENGLISH)
                val timeLabelZhCn = formatTo5MinTime(targetMillis, AppLanguage.SIMPLIFIED_CHINESE)
                val timeLabelZhTw = formatTo5MinTime(targetMillis, AppLanguage.TRADITIONAL_CHINESE)

                val multilingual = createMultilingualTexts(
                    jaHeadline = "${timeLabelJa}頃に雨が止む見込みです",
                    enHeadline = "Rain stopping around $timeLabelEn",
                    zhCnHeadline = "预计 $timeLabelZhCn 左右雨停",
                    zhTwHeadline = "預計 $timeLabelZhTw 左右雨停",
                    jaSubText = "雨が止むまであと約${diffMin}分です。次第に曇り空へと回復する見込みです。",
                    enSubText = "About ${diffMin} min until rain stops. Conditions will gradually improve.",
                    zhCnSubText = "距离雨停约 ${diffMin} 分钟，天气将逐渐好转。",
                    zhTwSubText = "距離雨停約 ${diffMin} 分鐘，天氣將逐漸好轉。",
                    jaTimeLabel = timeLabelJa,
                    enTimeLabel = timeLabelEn,
                    zhCnTimeLabel = timeLabelZhCn,
                    zhTwTimeLabel = timeLabelZhTw
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
                    enHeadline = "Rain expected around ${tonightHour}:00",
                    zhCnHeadline = "预计 ${tonightHour}:00 左右开始下雨",
                    zhTwHeadline = "預計 ${tonightHour}:00 左右開始下雨",
                    jaSubText = "今夜にかけてまとまった雨雲が広がる予想です（降水確率80%）。夜間のお帰りの際は傘をお持ちください。",
                    enSubText = "Rain clouds expected tonight (80% chance). Don't forget an umbrella for tonight.",
                    zhCnSubText = "今晚可能有降雨（降水概率 80%），晚归请备好雨具。",
                    zhTwSubText = "今晚可能有降雨（降水機率 80%），晚歸請備好雨具。",
                    jaTimeLabel = "${tonightHour}時頃",
                    enTimeLabel = "${tonightHour}:00",
                    zhCnTimeLabel = "${tonightHour}:00",
                    zhTwTimeLabel = "${tonightHour}:00"
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
                    enHeadline = "Umbrella needed tomorrow evening",
                    zhCnHeadline = "明天傍晚起需要带伞",
                    zhTwHeadline = "明天傍晚起需要帶傘",
                    jaSubText = "気象庁予報：降水確率70%の雨が予想されています。明日の外出時は雨具をご準備ください。",
                    enSubText = "JMA Forecast: 70% chance of rain expected. Prepare rain gear for tomorrow.",
                    zhCnSubText = "气象厅预报：预计降水概率为 70%，明天出门请备好雨具。",
                    zhTwSubText = "氣象廳預報：預計降水機率為 70%，明天出門請備好雨具。",
                    jaTimeLabel = "明日夕方以降",
                    enTimeLabel = "Tomorrow Evening",
                    zhCnTimeLabel = "明天傍晚起",
                    zhTwTimeLabel = "明天傍晚起"
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
                    enHeadline = "No rain expected today",
                    zhCnHeadline = "今天无需担心下雨",
                    zhTwHeadline = "今天無須擔心下雨",
                    jaSubText = "当面雨は降らない見込みです。お出かけやお洗濯にも安心です。",
                    enSubText = "Clear conditions ahead. Great weather for going out or laundry.",
                    zhCnSubText = "预计近期无雨，适合外出与晾晒。",
                    zhTwSubText = "預計近期無雨，適合外出與晾曬。",
                    jaTimeLabel = null,
                    enTimeLabel = null,
                    zhCnTimeLabel = null,
                    zhTwTimeLabel = null
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
            "france", "フランス", "法国", "法國",
            "germany", "ドイツ", "德国", "德國", "deutschland",
            "united kingdom", "uk", "great britain", "イギリス", "英国", "英國",
            "italy", "イタリア", "意大利", "italia",
            "spain", "スペイン", "西班牙", "españa",
            "switzerland", "スイス", "瑞士", "schweiz", "suisse",
            "netherlands", "オランダ", "荷兰", "荷蘭", "nederland",
            "belgium", "ベルギー", "比利时", "比利時", "belgique",
            "sweden", "スウェーデン", "瑞典", "sverige",
            "norway", "ノルウェー", "挪威", "norge",
            "finland", "フィンランド", "芬兰", "芬蘭", "suomi",
            "denmark", "デンマーク", "丹麦", "丹麥", "danmark",
            "austria", "オーストリア", "奥地利", "奧地利", "österreich",
            "portugal", "ポルトガル", "葡萄牙",
            "ireland", "アイルランド", "爱尔兰", "愛爾蘭",
            "poland", "ポーランド", "波兰", "波蘭", "polska",
            "czech", "チェコ", "捷克", "česko",
            "greece", "ギリシャ", "希腊", "希臘", "hellas",
            "hungary", "ハンガリー", "匈牙利", "magyarország",
            "romania", "ルーマニア", "罗马尼亚", "羅馬尼亞",
            "ukraine", "ウクライナ", "乌克兰", "烏克蘭",
            "europe", "ヨーロッパ", "欧洲", "歐洲"
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

