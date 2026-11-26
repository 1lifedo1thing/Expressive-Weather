package io.github.nemynew.expressiveweather.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan

enum class MoonPhaseStage {
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL_MOON,
    WANING_GIBBOUS,
    THIRD_QUARTER,
    WANING_CRESCENT
}

data class MoonPhaseInfo(
    val stage: MoonPhaseStage,
    val phaseName: String,
    val ageDays: String,
    val ageDaysFloat: Float,
    val phaseFraction: Float,
    val illumination: Int,
    val daysToFullMoon: Int,
    val daysToNewMoon: Int,
    val isWaxing: Boolean
)

data class MoonTransitSchedule(
    val moonrise: String?,
    val moonset: String?,
    val moonriseMillis: Long?,
    val moonsetMillis: Long?,
    val transitProgress: Float,
    val isAboveHorizon: Boolean,
    val altitudeDeg: Double,
    val transitDurationLabel: String
)

data class DailyMoonSchedule(
    val dayOffset: Int,
    val moonPhase: MoonPhaseInfo,
    val moonrise: String?,
    val moonset: String?
)

/**
 * デバッグ設定用の月相シミュレーター状態管理。
 * `overridePhaseFraction == null` のときは実際の天文時刻から算出する。
 */
object MoonPhaseSimulator {
    private const val PREFS_NAME = "expressive_weather_prefs"
    private const val KEY_DEBUG_MOON_PHASE = "debug_moon_phase_fraction"
    private const val KEY_DEBUG_SOUTHERN_HEMISPHERE = "debug_moon_southern_hemisphere"

    private val _overridePhaseFraction = MutableStateFlow<Float?>(null)
    val overridePhaseFraction: StateFlow<Float?> = _overridePhaseFraction.asStateFlow()

    // null = 都市の実緯度 (latitude < 0.0) に自動追従、true = 南半球固定、false = 北半球固定
    private val _overrideSouthernHemisphere = MutableStateFlow<Boolean?>(null)
    val overrideSouthernHemisphere: StateFlow<Boolean?> = _overrideSouthernHemisphere.asStateFlow()

    @Volatile
    private var initialized = false

    fun ensureLoaded(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean("debug_settings_unlocked", false)) {
                if (prefs.contains(KEY_DEBUG_MOON_PHASE)) {
                    val saved = prefs.getFloat(KEY_DEBUG_MOON_PHASE, -1f)
                    if (saved in 0f..1f) {
                        _overridePhaseFraction.value = saved
                    }
                }
                if (prefs.contains(KEY_DEBUG_SOUTHERN_HEMISPHERE)) {
                    _overrideSouthernHemisphere.value = prefs.getBoolean(KEY_DEBUG_SOUTHERN_HEMISPHERE, false)
                }
            }
            initialized = true
        }
    }

    fun resolveIsSouthernHemisphere(cityLatitude: Double, overrideValue: Boolean? = _overrideSouthernHemisphere.value): Boolean {
        return overrideValue ?: (cityLatitude < 0.0)
    }

    fun setSimulatedPhase(context: Context?, phaseFraction: Float?) {
        val normalized = phaseFraction?.let { ((it % 1f) + 1f) % 1f }
        _overridePhaseFraction.value = normalized
        initialized = true
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            if (normalized != null) {
                putFloat(KEY_DEBUG_MOON_PHASE, normalized)
            } else {
                remove(KEY_DEBUG_MOON_PHASE)
            }
            apply()
        }
    }

    fun setSimulatedSouthernHemisphere(context: Context?, enabled: Boolean?) {
        _overrideSouthernHemisphere.value = enabled
        initialized = true
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            if (enabled != null) {
                putBoolean(KEY_DEBUG_SOUTHERN_HEMISPHERE, enabled)
            } else {
                remove(KEY_DEBUG_SOUTHERN_HEMISPHERE)
            }
            apply()
        }
    }

    fun clear(context: Context?) {
        _overridePhaseFraction.value = null
        _overrideSouthernHemisphere.value = null
        initialized = true
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            remove(KEY_DEBUG_MOON_PHASE)
            remove(KEY_DEBUG_SOUTHERN_HEMISPHERE)
            apply()
        }
    }
}

/**
 * 国際標準天文月相計算エンジン
 * 朔望月周期 29.530588853 日 / J2000.0 新月エポック (2000-01-06T18:14:00Z) に基づき、
 * 月齢・輝面比・月相ステージ・次回満月／新月までの日数を算出する。
 */
object MoonPhaseCalculator {

    const val SYNODIC_MONTH_DAYS = 29.530588853
    const val REFERENCE_NEW_MOON_EPOCH_MS = 947_182_440_000L // 2000-01-06T18:14:00Z
    private const val MS_PER_DAY = 86_400_000.0

    fun calculate(
        language: AppLanguage,
        nowMillis: Long = System.currentTimeMillis(),
        overridePhaseFraction: Float? = null
    ): MoonPhaseInfo {
        if (overridePhaseFraction != null) {
            return calculateForFraction(language, overridePhaseFraction)
        }
        val elapsedDays = (nowMillis - REFERENCE_NEW_MOON_EPOCH_MS).toDouble() / MS_PER_DAY
        val ageDouble = ((elapsedDays % SYNODIC_MONTH_DAYS) + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS
        val phaseFraction = (ageDouble / SYNODIC_MONTH_DAYS).toFloat().coerceIn(0f, 1f)
        return calculateForFraction(language, phaseFraction)
    }

    /**
     * 読み込み・都市切替・再読み込み時の月相アニメーション開始位相と目標位相を返す。
     * 新月付近 (< 0.04 または > 0.96) は三日月付近 (0.18) から静かに欠けて新月へ収束させ、
     * それ以外は新月 (0.0) から現在相へ満ちていく展開とする。
     */
    fun resolveEntranceAnimationRange(targetPhaseFraction: Float): Pair<Float, Float> {
        val normalized = ((targetPhaseFraction % 1f) + 1f) % 1f
        return if (normalized < 0.04f || normalized > 0.96f) {
            0.18f to 0.0f
        } else {
            0.0f to normalized
        }
    }

    fun calculateForFraction(
        language: AppLanguage,
        rawPhaseFraction: Float
    ): MoonPhaseInfo {
        val phaseFraction = (((rawPhaseFraction % 1f) + 1f) % 1f).coerceIn(0f, 1f)
        val ageDouble = phaseFraction.toDouble() * SYNODIC_MONTH_DAYS

        val illuminationPct = round((1.0 - cos(2.0 * Math.PI * phaseFraction.toDouble())) * 50.0)
            .toInt()
            .coerceIn(0, 100)

        val halfCycle = SYNODIC_MONTH_DAYS / 2.0
        val daysToFull = if (ageDouble <= halfCycle) {
            round(halfCycle - ageDouble).toInt().coerceAtLeast(0)
        } else {
            round(SYNODIC_MONTH_DAYS - ageDouble + halfCycle).toInt().coerceAtLeast(0)
        }
        val daysToNew = round(SYNODIC_MONTH_DAYS - ageDouble).toInt().coerceAtLeast(0)

        val stage = when {
            phaseFraction < 0.03f || phaseFraction >= 0.97f -> MoonPhaseStage.NEW_MOON
            phaseFraction < 0.22f -> MoonPhaseStage.WAXING_CRESCENT
            phaseFraction < 0.28f -> MoonPhaseStage.FIRST_QUARTER
            phaseFraction < 0.47f -> MoonPhaseStage.WAXING_GIBBOUS
            phaseFraction < 0.53f -> MoonPhaseStage.FULL_MOON
            phaseFraction < 0.72f -> MoonPhaseStage.WANING_GIBBOUS
            phaseFraction < 0.78f -> MoonPhaseStage.THIRD_QUARTER
            else -> MoonPhaseStage.WANING_CRESCENT
        }

        val phaseName = if (language == AppLanguage.JAPANESE) {
            getJapaneseTraditionalMoonName(ageDouble)
        } else {
            getLocalizedPhaseName(stage, language)
        }

        return MoonPhaseInfo(
            stage = stage,
            phaseName = phaseName,
            ageDays = String.format(Locale.US, "%.1f", ageDouble),
            ageDaysFloat = ageDouble.toFloat(),
            phaseFraction = phaseFraction,
            illumination = illuminationPct,
            daysToFullMoon = daysToFull,
            daysToNewMoon = daysToNew,
            isWaxing = phaseFraction < 0.5f
        )
    }

    /**
     * 月齢（0.0〜29.53日）に基づく日本語の伝統的な月の呼び名を返す。
     */
    fun getJapaneseTraditionalMoonName(ageDays: Double): String {
        val normalized = ((ageDays % SYNODIC_MONTH_DAYS) + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS
        return when {
            normalized < 1.0 || normalized >= 29.0 -> "新月"
            normalized < 2.5 -> "二日月"
            normalized < 6.0 -> "三日月"
            normalized < 8.8 -> "上弦の月"
            normalized < 11.8 -> "十日夜"
            normalized < 13.5 -> "十三夜"
            normalized < 14.3 -> "小望月"
            normalized < 15.5 -> "満月"
            normalized < 16.5 -> "十六夜"
            normalized < 17.5 -> "立待月"
            normalized < 18.5 -> "居待月"
            normalized < 19.5 -> "寝待月"
            normalized < 21.0 -> "更待月"
            normalized < 23.5 -> "下弦の月"
            normalized < 25.2 -> "有明月"
            normalized < 27.2 -> "二十六夜月"
            else -> "晦"
        }
    }

    fun getLocalizedPhaseName(stage: MoonPhaseStage, language: AppLanguage): String = when (stage) {
        MoonPhaseStage.NEW_MOON -> when (language) {
            AppLanguage.JAPANESE -> "新月"
            AppLanguage.KOREAN -> "신월 (삭)"
            AppLanguage.SIMPLIFIED_CHINESE -> "新月"
            AppLanguage.TRADITIONAL_CHINESE -> "新月"
            AppLanguage.ENGLISH -> "New Moon"
            AppLanguage.SPANISH -> "Luna nueva"
            AppLanguage.FRENCH -> "Nouvelle lune"
            AppLanguage.PORTUGUESE -> "Lua nova"
            AppLanguage.RUSSIAN -> "Новолуние"
        }
        MoonPhaseStage.WAXING_CRESCENT -> when (language) {
            AppLanguage.JAPANESE -> "三日月"
            AppLanguage.KOREAN -> "초승달"
            AppLanguage.SIMPLIFIED_CHINESE -> "峨眉月"
            AppLanguage.TRADITIONAL_CHINESE -> "蛾眉月"
            AppLanguage.ENGLISH -> "Waxing Crescent"
            AppLanguage.SPANISH -> "Luna creciente"
            AppLanguage.FRENCH -> "Premier croissant"
            AppLanguage.PORTUGUESE -> "Lua crescente"
            AppLanguage.RUSSIAN -> "Растущий серп"
        }
        MoonPhaseStage.FIRST_QUARTER -> when (language) {
            AppLanguage.JAPANESE -> "上弦の月"
            AppLanguage.KOREAN -> "상현달"
            AppLanguage.SIMPLIFIED_CHINESE -> "上弦月"
            AppLanguage.TRADITIONAL_CHINESE -> "上弦月"
            AppLanguage.ENGLISH -> "First Quarter"
            AppLanguage.SPANISH -> "Cuarto creciente"
            AppLanguage.FRENCH -> "Premier quartier"
            AppLanguage.PORTUGUESE -> "Quarto crescente"
            AppLanguage.RUSSIAN -> "Первая четверть"
        }
        MoonPhaseStage.WAXING_GIBBOUS -> when (language) {
            AppLanguage.JAPANESE -> "十三夜"
            AppLanguage.KOREAN -> "차오르는 달"
            AppLanguage.SIMPLIFIED_CHINESE -> "盈凸月"
            AppLanguage.TRADITIONAL_CHINESE -> "盈凸月"
            AppLanguage.ENGLISH -> "Waxing Gibbous"
            AppLanguage.SPANISH -> "Gibosa creciente"
            AppLanguage.FRENCH -> "Gibbeuse croissante"
            AppLanguage.PORTUGUESE -> "Gibosa crescente"
            AppLanguage.RUSSIAN -> "Растущая луна"
        }
        MoonPhaseStage.FULL_MOON -> when (language) {
            AppLanguage.JAPANESE -> "満月"
            AppLanguage.KOREAN -> "보름달"
            AppLanguage.SIMPLIFIED_CHINESE -> "满月"
            AppLanguage.TRADITIONAL_CHINESE -> "滿月"
            AppLanguage.ENGLISH -> "Full Moon"
            AppLanguage.SPANISH -> "Luna llena"
            AppLanguage.FRENCH -> "Pleine lune"
            AppLanguage.PORTUGUESE -> "Lua cheia"
            AppLanguage.RUSSIAN -> "Полнолуние"
        }
        MoonPhaseStage.WANING_GIBBOUS -> when (language) {
            AppLanguage.JAPANESE -> "十六夜"
            AppLanguage.KOREAN -> "기우는 달"
            AppLanguage.SIMPLIFIED_CHINESE -> "亏凸月"
            AppLanguage.TRADITIONAL_CHINESE -> "虧凸月"
            AppLanguage.ENGLISH -> "Waning Gibbous"
            AppLanguage.SPANISH -> "Gibosa menguante"
            AppLanguage.FRENCH -> "Gibbeuse décroissante"
            AppLanguage.PORTUGUESE -> "Gibosa minguante"
            AppLanguage.RUSSIAN -> "Убывающая луна"
        }
        MoonPhaseStage.THIRD_QUARTER -> when (language) {
            AppLanguage.JAPANESE -> "下弦の月"
            AppLanguage.KOREAN -> "하현달"
            AppLanguage.SIMPLIFIED_CHINESE -> "下弦月"
            AppLanguage.TRADITIONAL_CHINESE -> "下弦月"
            AppLanguage.ENGLISH -> "Third Quarter"
            AppLanguage.SPANISH -> "Cuarto menguante"
            AppLanguage.FRENCH -> "Dernier quartier"
            AppLanguage.PORTUGUESE -> "Quarto minguante"
            AppLanguage.RUSSIAN -> "Последняя четверть"
        }
        MoonPhaseStage.WANING_CRESCENT -> when (language) {
            AppLanguage.JAPANESE -> "有明月"
            AppLanguage.KOREAN -> "그믐달"
            AppLanguage.SIMPLIFIED_CHINESE -> "残月"
            AppLanguage.TRADITIONAL_CHINESE -> "殘月"
            AppLanguage.ENGLISH -> "Waning Crescent"
            AppLanguage.SPANISH -> "Luna menguante"
            AppLanguage.FRENCH -> "Dernier croissant"
            AppLanguage.PORTUGUESE -> "Lua minguante"
            AppLanguage.RUSSIAN -> "Убывающий серп"
        }
    }

    /**
     * 指定座標・タイムゾーンにおける月の出・月の入り・現在の見かけ高度および月軌跡進行度を
     * Meeus / USNO 基準の月黄経・黄緯・地平視差・大気差補正から高精度に算出する。
     */
    fun calculateMoonSchedule(
        latitude: Double,
        longitude: Double,
        utcOffsetSeconds: Int? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): MoonTransitSchedule {
        val offsetSec = resolveUtcOffsetSeconds(latitude, longitude, utcOffsetSeconds, nowMillis)
        val dayStartMs = localDayStartMillis(nowMillis, offsetSec)
        val dayEndMs = dayStartMs + 86_400_000L

        val (todayRiseMs, todaySetMs) = findRiseSetInWindow(latitude, longitude, dayStartMs, dayEndMs)

        // 現在の見かけ高度（地平視差・視半径・大気差込み。> 0 で地平線上）
        val currentApparentRad = moonHorizonFunctionRad(latitude, longitude, nowMillis)
        val isAboveHorizon = currentApparentRad > 0.0
        val altitudeDeg = currentApparentRad * (180.0 / PI)

        // 前後24時間の月の出・月の入りを探索して、現在の運行アーク進行度と出没間隔を正確に算出
        val windowStart = nowMillis - 86_400_000L
        val windowEnd = nowMillis + 86_400_000L
        val events = findAllRiseSetEvents(latitude, longitude, windowStart, windowEnd)

        val prevRise = events.filter { it.second && it.first <= nowMillis }.maxByOrNull { it.first }?.first
        val nextSet = events.filter { !it.second && it.first >= nowMillis }.minByOrNull { it.first }?.first
        val nextRise = events.filter { it.second && it.first > nowMillis }.minByOrNull { it.first }?.first
        val setAfterNextRise = if (nextRise != null) {
            events.filter { !it.second && it.first > nextRise }.minByOrNull { it.first }?.first
        } else null

        val transitProgress = if (isAboveHorizon && prevRise != null && nextSet != null && nextSet > prevRise) {
            ((nowMillis - prevRise).toDouble() / (nextSet - prevRise).toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        val durationMs = when {
            isAboveHorizon && prevRise != null && nextSet != null && nextSet > prevRise -> nextSet - prevRise
            nextRise != null && setAfterNextRise != null && setAfterNextRise > nextRise -> setAfterNextRise - nextRise
            todayRiseMs != null && todaySetMs != null && todaySetMs > todayRiseMs -> todaySetMs - todayRiseMs
            else -> null
        }

        return MoonTransitSchedule(
            moonrise = todayRiseMs?.let { formatLocalHm(it, offsetSec) },
            moonset = todaySetMs?.let { formatLocalHm(it, offsetSec) },
            moonriseMillis = todayRiseMs,
            moonsetMillis = todaySetMs,
            transitProgress = transitProgress,
            isAboveHorizon = isAboveHorizon,
            altitudeDeg = altitudeDeg,
            transitDurationLabel = formatDurationHm(durationMs)
        )
    }

    /**
     * 今日から指定日数分（デフォルト7日間）の日別 月相・月の出・月の入り一覧を算出する。
     */
    fun calculateWeeklyMoonSchedule(
        latitude: Double,
        longitude: Double,
        language: AppLanguage,
        utcOffsetSeconds: Int? = null,
        nowMillis: Long = System.currentTimeMillis(),
        days: Int = 7
    ): List<DailyMoonSchedule> {
        val offsetSec = resolveUtcOffsetSeconds(latitude, longitude, utcOffsetSeconds, nowMillis)
        val baseDayStartMs = localDayStartMillis(nowMillis, offsetSec)
        return (0 until days).map { offset ->
            val startMs = baseDayStartMs + offset * 86_400_000L
            val endMs = startMs + 86_400_000L
            val noonMs = startMs + 43_200_000L
            val (riseMs, setMs) = findRiseSetInWindow(latitude, longitude, startMs, endMs)
            val phase = calculate(language = language, nowMillis = noonMs)
            DailyMoonSchedule(
                dayOffset = offset,
                moonPhase = phase,
                moonrise = riseMs?.let { formatLocalHm(it, offsetSec) },
                moonset = setMs?.let { formatLocalHm(it, offsetSec) }
            )
        }
    }

    private fun resolveUtcOffsetSeconds(
        latitude: Double,
        longitude: Double,
        explicitOffsetSeconds: Int?,
        nowMillis: Long
    ): Int {
        if (explicitOffsetSeconds != null) return explicitOffsetSeconds
        if (latitude in 20.0..46.5 && longitude in 122.0..154.0) {
            return 9 * 3600
        }
        return TimeZone.getDefault().getOffset(nowMillis) / 1000
    }

    private fun localDayStartMillis(nowMillis: Long, utcOffsetSeconds: Int): Long {
        val offsetMs = utcOffsetSeconds * 1000L
        val localMillis = nowMillis + offsetMs
        val localDayStart = Math.floorDiv(localMillis, 86_400_000L) * 86_400_000L
        return localDayStart - offsetMs
    }

    private fun formatLocalHm(utcMillis: Long, utcOffsetSeconds: Int): String {
        val tz = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(tz)
        // 四捨五入して最寄りの分へ丸める
        val roundedMillis = ((utcMillis + 30_000L) / 60_000L) * 60_000L
        cal.timeInMillis = roundedMillis + utcOffsetSeconds * 1000L
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    private fun formatDurationHm(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0L) return "--"
        val totalMinutes = ((durationMs + 30_000L) / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }

    private fun findRiseSetInWindow(
        latitude: Double,
        longitude: Double,
        startMs: Long,
        endMs: Long
    ): Pair<Long?, Long?> {
        val events = findAllRiseSetEvents(latitude, longitude, startMs, endMs)
        val rise = events.firstOrNull { it.second }?.first
        val set = events.firstOrNull { !it.second }?.first
        return rise to set
    }

    /**
     * Pair(eventMillis, isRise: Boolean) の一覧を時間昇順で返す。
     */
    private fun findAllRiseSetEvents(
        latitude: Double,
        longitude: Double,
        startMs: Long,
        endMs: Long
    ): List<Pair<Long, Boolean>> {
        val result = mutableListOf<Pair<Long, Boolean>>()
        val stepMs = 15L * 60_000L // 15分刻みで粗探索し二分法で秒精度へ収束
        var t0 = startMs
        var h0 = moonHorizonFunctionRad(latitude, longitude, t0)
        while (t0 < endMs) {
            val t1 = minOf(t0 + stepMs, endMs)
            val h1 = moonHorizonFunctionRad(latitude, longitude, t1)
            if (h0 <= 0.0 && h1 > 0.0) {
                result.add(refineCrossing(latitude, longitude, t0, t1, rising = true) to true)
            } else if (h0 > 0.0 && h1 <= 0.0) {
                result.add(refineCrossing(latitude, longitude, t0, t1, rising = false) to false)
            }
            t0 = t1
            h0 = h1
        }
        return result
    }

    private fun refineCrossing(
        latitude: Double,
        longitude: Double,
        tLowInit: Long,
        tHighInit: Long,
        rising: Boolean
    ): Long {
        var lo = tLowInit
        var hi = tHighInit
        repeat(14) {
            val mid = lo + (hi - lo) / 2L
            val hMid = moonHorizonFunctionRad(latitude, longitude, mid)
            if (rising) {
                if (hMid <= 0.0) lo = mid else hi = mid
            } else {
                if (hMid > 0.0) lo = mid else hi = mid
            }
        }
        return lo + (hi - lo) / 2L
    }

    /**
     * 月の上縁が地平線に接するとき 0 となる見かけ高度関数（ラジアン）。
     * Meeus 主要摂動項・地平視差・月視半径・大気差（34'）をすべて考慮。
     */
    internal fun moonHorizonFunctionRad(
        latitudeDeg: Double,
        longitudeDeg: Double,
        utcMillis: Long
    ): Double {
        val rad = PI / 180.0
        val d = (utcMillis / MS_PER_DAY) - 10_957.5 // J2000.0 (2000-01-01T12:00:00Z) からの経過日数

        val meanLon = rad * (218.316 + 13.176396 * d)
        val meanAnomaly = rad * (134.963 + 13.064993 * d)
        val meanDistNode = rad * (93.272 + 13.229350 * d)
        val meanElongation = rad * (297.850 + 12.190749 * d)
        val sunMeanAnomaly = rad * (357.529 + 0.985600 * d)

        val lonEcl = meanLon + rad * (
            6.289 * sin(meanAnomaly) +
                1.274 * sin(2.0 * meanElongation - meanAnomaly) +
                0.658 * sin(2.0 * meanElongation) +
                0.214 * sin(2.0 * meanAnomaly) -
                0.186 * sin(sunMeanAnomaly)
            )
        val latEcl = rad * (
            5.128 * sin(meanDistNode) +
                0.281 * sin(meanAnomaly + meanDistNode) +
                0.278 * sin(meanAnomaly - meanDistNode) +
                0.173 * sin(2.0 * meanElongation - meanDistNode)
            )
        val distKm = 385_001.0 -
            20_905.0 * cos(meanAnomaly) -
            3_699.0 * cos(2.0 * meanElongation - meanAnomaly) -
            2_956.0 * cos(2.0 * meanElongation)

        val obliquity = rad * 23.4397
        val sinLon = sin(lonEcl)
        val cosLon = cos(lonEcl)
        val sinLat = sin(latEcl)
        val cosLat = cos(latEcl)
        val sinEps = sin(obliquity)
        val cosEps = cos(obliquity)

        val ra = atan2(sinLon * cosEps - tan(latEcl) * sinEps, cosLon)
        val dec = asin((sinLat * cosEps + cosLat * sinEps * sinLon).coerceIn(-1.0, 1.0))

        // USNO グリニッジ平均恒星時
        val gmst = rad * (280.46061837 + 360.98564736629 * d)
        val lst = gmst + longitudeDeg * rad
        val hourAngle = lst - ra

        val latRad = latitudeDeg * rad
        val sinAltGeo = (sin(latRad) * sin(dec) + cos(latRad) * cos(dec) * cos(hourAngle)).coerceIn(-1.0, 1.0)
        val altGeo = asin(sinAltGeo)

        val horizontalParallax = asin((6_378.14 / distKm).coerceIn(-1.0, 1.0))
        val semiDiameter = asin((1_737.4 / distKm).coerceIn(-1.0, 1.0))
        val refractionHorizon = rad * (34.0 / 60.0)

        val altTopo = altGeo - horizontalParallax * cos(altGeo)
        return altTopo + semiDiameter + refractionHorizon
    }
}
