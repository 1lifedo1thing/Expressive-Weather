package io.github.nemynew.expressiveweather.ui.haptics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.HapticIntensity
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.data.WeatherRepository
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * 端末バイブレータのハードウェア特性に応じた最適化プロファイル
 */
enum class HapticDeviceProfile(
    val id: String,
    val labelJa: String,
    val labelKo: String,
    val labelZhCn: String,
    val labelZhTw: String,
    val labelEs: String,
    val labelFr: String,
    val labelPt: String,
    val labelEn: String
) {
    /** Google Pixel 等のプリミティブ完全対応 LRA (線形共振アクチュエータ) */
    HIGH_DEF_LRA(
        "high_def_lra",
        "高精細 LRA (プリミティブ合成)",
        "고정밀 LRA (프리미티브 합성)",
        "高精度 LRA（基元合成）",
        "高精細 LRA（基元合成）",
        "LRA de alta definición (primitivas)",
        "LRA haute définition (primitives)",
        "LRA de alta definição (primitivas)",
        "High-Def LRA (Composition Primitives)"
    ),

    /** Samsung Galaxy 系 X軸リニアモーター最適化 (タイトなトランジェント・サチュレーション抑制) */
    SAMSUNG_HD_LRA(
        "samsung_hd_lra",
        "Galaxy HD リニアモーター最適化",
        "Galaxy HD 리니어 모터 최적화",
        "Galaxy HD 线性马达优化",
        "Galaxy HD 線性馬達最佳化",
        "LRA HD Samsung Galaxy",
        "LRA HD Samsung Galaxy",
        "LRA HD Samsung Galaxy",
        "Samsung Galaxy HD LRA"
    ),

    /** 振幅制御対応モーター (API 26+ Waveform Envelope) */
    AMPLITUDE_WAVEFORM(
        "amplitude_waveform",
        "振幅制御ウェーブフォーム",
        "진폭 제어 웨이브폼",
        "振幅控制波形",
        "振幅控制波形",
        "Onda con control de amplitud",
        "Forme d'onde à amplitude contrôlée",
        "Forma de onda com controle de amplitude",
        "Amplitude-Controlled Waveform"
    ),

    /** 従来型 ERM / 時間制御モーター (スピンアップ補正付き) */
    LEGACY_TIMED_ERM(
        "legacy_timed_erm",
        "標準タイミング制御 (ERM)",
        "표준 타이밍 제어 (ERM)",
        "标准时序控制 (ERM)",
        "標準時序控制 (ERM)",
        "Pulso temporizado (ERM)",
        "Impulsion temporisée (ERM)",
        "Pulso temporizado (ERM)",
        "Timed Pulse (ERM)"
    ),

    /** バイブレータ非搭載 (タブレット等) */
    NONE(
        "none",
        "ハードウェア非搭載 (システム委譲)",
        "진동 하드웨어 없음 (시스템 위임)",
        "无振动硬件（系统托管）",
        "無震動硬體（系統委派）",
        "Sin hardware de vibración",
        "Aucun matériel de vibration",
        "Sem hardware de vibração",
        "No Vibrator Hardware"
    )
    ;

    fun getLabel(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> labelJa
        AppLanguage.KOREAN -> labelKo
        AppLanguage.SIMPLIFIED_CHINESE -> labelZhCn
        AppLanguage.TRADITIONAL_CHINESE -> labelZhTw
        AppLanguage.SPANISH -> labelEs
        AppLanguage.FRENCH -> labelFr
        AppLanguage.PORTUGUESE -> labelPt
        AppLanguage.ENGLISH -> labelEn
    }
}

/**
 * 拡張ハプティクスパターン定義（Material 3 Expressive UI + 気象シグネチャ）
 */
enum class ExpressiveHapticPattern(
    val isWeatherPattern: Boolean = false,
    val isUrgentAlert: Boolean = false,
    val minIntervalMs: Long = 24L
) {
    // --- UI 操作・モーション連動パターン ---
    SOFT_TAP(minIntervalMs = 20L),
    EXPRESSIVE_BOUNCE(minIntervalMs = 35L),
    SWITCH_ON(minIntervalMs = 30L),
    SWITCH_OFF(minIntervalMs = 30L),
    SCRUB_TICK(minIntervalMs = 28L),
    DETENT_BOUNDARY(minIntervalMs = 45L),
    DRAG_PICKUP(minIntervalMs = 60L),
    DRAG_SWAP(minIntervalMs = 45L),
    DRAG_DROP(minIntervalMs = 60L),
    PULL_REFRESH_TRIGGER(minIntervalMs = 120L),
    REFRESH_SUCCESS(minIntervalMs = 150L),
    ERROR_REJECT(minIntervalMs = 120L),
    CELEBRATE_UNLOCK(minIntervalMs = 200L),

    // --- 天候・防災アラート シグネチャパターン ---
    WEATHER_RAINDROP_PATTER(isWeatherPattern = true, minIntervalMs = 400L),
    WEATHER_THUNDER_RUMBLE(isWeatherPattern = true, minIntervalMs = 500L),
    WEATHER_SNOW_CRISP(isWeatherPattern = true, minIntervalMs = 400L),
    WEATHER_WIND_GUST(isWeatherPattern = true, minIntervalMs = 450L),
    ALERT_CRITICAL_PULSE(isUrgentAlert = true, minIntervalMs = 500L);

    companion object {
        fun forWeatherCondition(condition: WeatherCondition): ExpressiveHapticPattern? = when (condition) {
            WeatherCondition.THUNDERSTORM -> WEATHER_THUNDER_RUMBLE
            WeatherCondition.HEAVY_RAIN, WeatherCondition.RAIN -> WEATHER_RAINDROP_PATTER
            WeatherCondition.SNOW -> WEATHER_SNOW_CRISP
            WeatherCondition.FOG, WeatherCondition.CLOUDY, WeatherCondition.FEW_CLOUDS, WeatherCondition.CLEAR -> null
        }
    }
}

data class HapticWaveformSpec(
    val timingsMs: LongArray,
    val amplitudes: IntArray
) {
    init {
        require(timingsMs.size == amplitudes.size) { "timingsMs and amplitudes must have identical length" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HapticWaveformSpec) return false
        return timingsMs.contentEquals(other.timingsMs) && amplitudes.contentEquals(other.amplitudes)
    }

    override fun hashCode(): Int {
        var result = timingsMs.contentHashCode()
        result = 31 * result + amplitudes.contentHashCode()
        return result
    }
}

data class HapticPrimitiveStep(
    val primitiveId: Int,
    val scale: Float,
    val delayMs: Int = 0
)

data class HapticCapabilitiesSnapshot(
    val manufacturer: String,
    val model: String,
    val sdkInt: Int,
    val hasVibrator: Boolean,
    val hasAmplitudeControl: Boolean,
    val supportedPrimitivesCount: Int,
    val profile: HapticDeviceProfile
)

/**
 * ハードウェア判別とパターン波形計算の純粋ロジック（単体テスト可能）
 */
object ExpressiveHapticProfiles {

    fun detectDeviceProfile(
        manufacturer: String,
        sdkInt: Int,
        hasVibrator: Boolean,
        hasAmplitudeControl: Boolean,
        supportedPrimitivesCount: Int
    ): HapticDeviceProfile {
        if (!hasVibrator) return HapticDeviceProfile.NONE
        val isSamsung = manufacturer.equals("samsung", ignoreCase = true)
        return when {
            isSamsung && sdkInt >= Build.VERSION_CODES.Q -> HapticDeviceProfile.SAMSUNG_HD_LRA
            sdkInt >= Build.VERSION_CODES.R && supportedPrimitivesCount >= 4 -> HapticDeviceProfile.HIGH_DEF_LRA
            sdkInt >= Build.VERSION_CODES.O && hasAmplitudeControl -> HapticDeviceProfile.AMPLITUDE_WAVEFORM
            else -> HapticDeviceProfile.LEGACY_TIMED_ERM
        }
    }

    fun scaleAmplitude(baseAmplitude: Int, intensity: HapticIntensity, profile: HapticDeviceProfile): Int {
        if (baseAmplitude <= 0 || intensity == HapticIntensity.OFF) return 0
        val profileCap = when (profile) {
            // Samsung X-axis LRA は215以上の振幅で飽和・金属音化しやすいため上品な上限に調整
            HapticDeviceProfile.SAMSUNG_HD_LRA -> 215
            HapticDeviceProfile.HIGH_DEF_LRA -> 255
            else -> 240
        }
        val scaled = (baseAmplitude * intensity.scale).roundToInt()
        return scaled.coerceIn(14, profileCap)
    }

    fun scalePrimitive(baseScale: Float, intensity: HapticIntensity): Float {
        if (intensity == HapticIntensity.OFF) return 0f
        return (baseScale * intensity.scale).coerceIn(0.12f, 1.0f)
    }

    /**
     * API 30+ VibrationEffect.Composition 用ステップ列を生成
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun buildPrimitiveSteps(
        pattern: ExpressiveHapticPattern,
        intensity: HapticIntensity,
        supportedPrimitives: Set<Int>
    ): List<HapticPrimitiveStep> {
        fun pick(preferred: Int, fallback: Int = VibrationEffect.Composition.PRIMITIVE_CLICK): Int {
            return if (preferred in supportedPrimitives) preferred else fallback
        }

        val s = intensity.scale
        fun sc(v: Float): Float = (v * s).coerceIn(0.12f, 1.0f)

        val steps = when (pattern) {
            ExpressiveHapticPattern.SOFT_TAP -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_TICK), sc(0.45f), 0)
            )
            ExpressiveHapticPattern.EXPRESSIVE_BOUNCE -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.62f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_LOW_TICK), sc(0.28f), 32)
            )
            ExpressiveHapticPattern.SWITCH_ON -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_LOW_TICK), sc(0.35f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.85f), 14)
            )
            ExpressiveHapticPattern.SWITCH_OFF -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.58f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_LOW_TICK), sc(0.24f), 12)
            )
            ExpressiveHapticPattern.SCRUB_TICK -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_LOW_TICK), sc(0.34f), 0)
            )
            ExpressiveHapticPattern.DETENT_BOUNDARY -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_THUD, VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.70f), 0)
            )
            ExpressiveHapticPattern.DRAG_PICKUP -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE), sc(0.60f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.88f), 10)
            )
            ExpressiveHapticPattern.DRAG_SWAP -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_LOW_TICK), sc(0.52f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_TICK), sc(0.56f), 8)
            )
            ExpressiveHapticPattern.DRAG_DROP -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_THUD, VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.76f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_LOW_TICK), sc(0.30f), 22)
            )
            ExpressiveHapticPattern.PULL_REFRESH_TRIGGER -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE), sc(0.55f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.85f), 18)
            )
            ExpressiveHapticPattern.REFRESH_SUCCESS -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.55f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE), sc(0.72f), 50)
            )
            ExpressiveHapticPattern.ERROR_REJECT -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.78f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_THUD, VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.82f), 45)
            )
            ExpressiveHapticPattern.CELEBRATE_UNLOCK -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_LOW_TICK), sc(0.45f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.70f), 38),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE), sc(0.92f), 42)
            )
            ExpressiveHapticPattern.WEATHER_RAINDROP_PATTER -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.76f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.92f), 115),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.74f), 155),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.88f), 100),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.80f), 135)
            )
            ExpressiveHapticPattern.WEATHER_THUNDER_RUMBLE -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.95f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, VibrationEffect.Composition.PRIMITIVE_QUICK_RISE), sc(0.78f), 30),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_THUD, VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.92f), 20),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, VibrationEffect.Composition.PRIMITIVE_LOW_TICK), sc(0.62f), 35)
            )
            ExpressiveHapticPattern.WEATHER_SNOW_CRISP -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_TICK, VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.75f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.55f), 110),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_TICK, VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.70f), 95)
            )
            ExpressiveHapticPattern.WEATHER_WIND_GUST -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, VibrationEffect.Composition.PRIMITIVE_QUICK_RISE), sc(0.70f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, VibrationEffect.Composition.PRIMITIVE_LOW_TICK), sc(0.48f), 28)
            )
            ExpressiveHapticPattern.ALERT_CRITICAL_PULSE -> listOf(
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_THUD, VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.92f), 0),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.85f), 60),
                HapticPrimitiveStep(pick(VibrationEffect.Composition.PRIMITIVE_THUD, VibrationEffect.Composition.PRIMITIVE_CLICK), sc(0.95f), 80)
            )
        }
        return steps.filter { it.primitiveId in supportedPrimitives }
    }

    /**
     * 全端末・全パターン対応の振幅エンベロープ波形生成（Samsung LRA / Amplitude / ERM に最適化）
     */
    fun buildWaveformSpec(
        pattern: ExpressiveHapticPattern,
        intensity: HapticIntensity,
        profile: HapticDeviceProfile
    ): HapticWaveformSpec {
        // Samsung LRA は極めて応答が速いためタイトなパルス幅 (10-16ms)、ERM はスピンアップ補償 (18-32ms)
        val isTightMotor = profile == HapticDeviceProfile.SAMSUNG_HD_LRA || profile == HapticDeviceProfile.HIGH_DEF_LRA
        val rawTimings: LongArray
        val rawAmps: IntArray

        when (pattern) {
            ExpressiveHapticPattern.SOFT_TAP -> {
                rawTimings = longArrayOf(if (isTightMotor) 10L else 18L)
                rawAmps = intArrayOf(90)
            }
            ExpressiveHapticPattern.EXPRESSIVE_BOUNCE -> {
                rawTimings = longArrayOf(if (isTightMotor) 13L else 22L, 32L, if (isTightMotor) 8L else 14L)
                rawAmps = intArrayOf(155, 0, 70)
            }
            ExpressiveHapticPattern.SWITCH_ON -> {
                rawTimings = longArrayOf(10L, 14L, if (isTightMotor) 14L else 22L)
                rawAmps = intArrayOf(75, 0, 195)
            }
            ExpressiveHapticPattern.SWITCH_OFF -> {
                rawTimings = longArrayOf(if (isTightMotor) 12L else 20L, 12L, 9L)
                rawAmps = intArrayOf(145, 0, 60)
            }
            ExpressiveHapticPattern.SCRUB_TICK -> {
                rawTimings = longArrayOf(if (isTightMotor) 8L else 15L)
                rawAmps = intArrayOf(75)
            }
            ExpressiveHapticPattern.DETENT_BOUNDARY -> {
                rawTimings = longArrayOf(if (isTightMotor) 18L else 28L)
                rawAmps = intArrayOf(180)
            }
            ExpressiveHapticPattern.DRAG_PICKUP -> {
                rawTimings = longArrayOf(12L, 14L, if (isTightMotor) 16L else 26L)
                rawAmps = intArrayOf(85, 140, 210)
            }
            ExpressiveHapticPattern.DRAG_SWAP -> {
                rawTimings = longArrayOf(if (isTightMotor) 11L else 18L, 10L, if (isTightMotor) 10L else 16L)
                rawAmps = intArrayOf(125, 0, 145)
            }
            ExpressiveHapticPattern.DRAG_DROP -> {
                rawTimings = longArrayOf(if (isTightMotor) 18L else 28L, 22L, 10L)
                rawAmps = intArrayOf(185, 0, 75)
            }
            ExpressiveHapticPattern.PULL_REFRESH_TRIGGER -> {
                rawTimings = longArrayOf(14L, 18L, if (isTightMotor) 16L else 24L)
                rawAmps = intArrayOf(95, 0, 195)
            }
            ExpressiveHapticPattern.REFRESH_SUCCESS -> {
                rawTimings = longArrayOf(if (isTightMotor) 12L else 20L, 50L, if (isTightMotor) 16L else 26L)
                rawAmps = intArrayOf(130, 0, 185)
            }
            ExpressiveHapticPattern.ERROR_REJECT -> {
                rawTimings = longArrayOf(if (isTightMotor) 16L else 26L, 42L, if (isTightMotor) 20L else 34L)
                rawAmps = intArrayOf(185, 0, 200)
            }
            ExpressiveHapticPattern.CELEBRATE_UNLOCK -> {
                rawTimings = longArrayOf(10L, 38L, 12L, 42L, if (isTightMotor) 18L else 28L)
                rawAmps = intArrayOf(95, 0, 150, 0, 210)
            }
            ExpressiveHapticPattern.WEATHER_RAINDROP_PATTER -> {
                rawTimings = longArrayOf(24L, 115L, 28L, 155L, 24L, 100L, 26L, 135L, 24L)
                rawAmps = intArrayOf(175, 0, 210, 0, 170, 0, 200, 0, 185)
            }
            ExpressiveHapticPattern.WEATHER_THUNDER_RUMBLE -> {
                rawTimings = longArrayOf(22L, 26L, 40L, 48L, 60L, 55L)
                rawAmps = intArrayOf(230, 0, 150, 210, 135, 70)
            }
            ExpressiveHapticPattern.WEATHER_SNOW_CRISP -> {
                rawTimings = longArrayOf(20L, 110L, 22L, 95L, 18L)
                rawAmps = intArrayOf(135, 0, 160, 0, 125)
            }
            ExpressiveHapticPattern.WEATHER_WIND_GUST -> {
                rawTimings = longArrayOf(28L, 35L, 40L, 35L, 28L)
                rawAmps = intArrayOf(50, 105, 165, 115, 55)
            }
            ExpressiveHapticPattern.ALERT_CRITICAL_PULSE -> {
                rawTimings = longArrayOf(24L, 55L, 24L, 75L, 32L)
                rawAmps = intArrayOf(210, 0, 195, 0, 220)
            }
        }

        val scaledAmps = IntArray(rawAmps.size) { i ->
            scaleAmplitude(rawAmps[i], intensity, profile)
        }
        return HapticWaveformSpec(rawTimings, scaledAmps)
    }

    /**
     * API < 26 または振幅制御非対応デバイス用 ON/OFF タイミングパターン
     */
    fun toLegacyOnOffPattern(spec: HapticWaveformSpec): LongArray {
        val result = ArrayList<Long>(spec.timingsMs.size + 1)
        result.add(0L) // initial delay
        for (i in spec.timingsMs.indices) {
            result.add(spec.timingsMs[i].coerceAtLeast(1L))
        }
        return result.toLongArray()
    }
}

/**
 * アプリ全体のハプティクス制御エンジン（デバイスプロファイル自動最適化＋レート制限＋設定連動）
 */
class ExpressiveHapticController(context: Context) {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    @Volatile
    var intensity: HapticIntensity = HapticIntensity.STANDARD

    @Volatile
    var weatherHapticsEnabled: Boolean = true

    private val lastTriggeredMap = ConcurrentHashMap<ExpressiveHapticPattern, Long>()

    private val supportedPrimitiveSet: Set<Int> by lazy {
        val v = vibrator ?: return@lazy emptySet()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !v.hasVibrator()) return@lazy emptySet()
        val candidates = intArrayOf(
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
            VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
            VibrationEffect.Composition.PRIMITIVE_THUD,
            VibrationEffect.Composition.PRIMITIVE_SPIN
        )
        try {
            val supported = v.arePrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
                VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
                VibrationEffect.Composition.PRIMITIVE_THUD,
                VibrationEffect.Composition.PRIMITIVE_SPIN
            )
            val result = LinkedHashSet<Int>()
            for (i in candidates.indices) {
                if (i < supported.size && supported[i]) {
                    result.add(candidates[i])
                }
            }
            result
        } catch (_: Exception) {
            emptySet()
        }
    }

    val capabilities: HapticCapabilitiesSnapshot by lazy {
        val v = vibrator
        val hasVib = try { v?.hasVibrator() == true } catch (_: Exception) { false }
        val hasAmp = try {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && v?.hasAmplitudeControl() == true
        } catch (_: Exception) { false }
        val primCount = supportedPrimitiveSet.size
        val profile = ExpressiveHapticProfiles.detectDeviceProfile(
            manufacturer = Build.MANUFACTURER ?: "",
            sdkInt = Build.VERSION.SDK_INT,
            hasVibrator = hasVib,
            hasAmplitudeControl = hasAmp,
            supportedPrimitivesCount = primCount
        )
        HapticCapabilitiesSnapshot(
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            model = Build.MODEL ?: "Android",
            sdkInt = Build.VERSION.SDK_INT,
            hasVibrator = hasVib,
            hasAmplitudeControl = hasAmp,
            supportedPrimitivesCount = primCount,
            profile = profile
        )
    }

    @SuppressLint("MissingPermission")
    fun triggerRaindropPulse(
        strength: Float,
        view: View? = null,
        composeFallback: HapticFeedback? = null
    ) {
        val currentIntensity = intensity
        if (currentIntensity == HapticIntensity.OFF || !weatherHapticsEnabled) return

        val caps = capabilities
        val v = vibrator
        if (caps.hasVibrator && v != null) {
            val handled = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    VibrationEffect.Composition.PRIMITIVE_CLICK in supportedPrimitiveSet
                ) {
                    val comp = VibrationEffect.startComposition()
                    comp.addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_CLICK,
                        ExpressiveHapticProfiles.scalePrimitive(strength, currentIntensity),
                        0
                    )
                    vibrateWithAttributes(v, comp.compose(), isUrgentAlert = false)
                    true
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val baseAmp = (strength.coerceIn(0.2f, 1.0f) * 215).toInt()
                    val amp = ExpressiveHapticProfiles.scaleAmplitude(baseAmp, currentIntensity, caps.profile)
                    val effect = if (caps.hasAmplitudeControl) {
                        VibrationEffect.createOneShot(24L, amp)
                    } else {
                        VibrationEffect.createOneShot(24L, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    vibrateWithAttributes(v, effect, isUrgentAlert = false)
                    true
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(24L)
                    true
                }
            } catch (_: SecurityException) {
                false
            } catch (_: Exception) {
                false
            }
            if (handled) return
        }
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            ?: composeFallback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @SuppressLint("MissingPermission")
    fun trigger(
        pattern: ExpressiveHapticPattern,
        view: View? = null,
        composeFallback: HapticFeedback? = null,
        bypassThrottle: Boolean = false
    ) {
        val currentIntensity = intensity
        if (currentIntensity == HapticIntensity.OFF) return
        if (pattern.isWeatherPattern && !weatherHapticsEnabled && !bypassThrottle) return

        val now = SystemClock.elapsedRealtime()
        if (!bypassThrottle) {
            val prev = lastTriggeredMap[pattern] ?: 0L
            if (now - prev < pattern.minIntervalMs) return
        }
        lastTriggeredMap[pattern] = now

        val caps = capabilities
        val v = vibrator

        if (caps.hasVibrator && v != null) {
            if (tryVibrateHardware(v, pattern, currentIntensity, caps, bypassThrottle)) {
                return
            }
        }

        // フォールバック: View または Compose HapticFeedback（タブレット等のシステムフィードバック委譲用）
        if (view != null && tryPerformViewFeedback(view, pattern)) {
            return
        }
        composeFallback?.let { fb ->
            val type = when (pattern) {
                ExpressiveHapticPattern.DRAG_PICKUP,
                ExpressiveHapticPattern.ALERT_CRITICAL_PULSE,
                ExpressiveHapticPattern.CELEBRATE_UNLOCK,
                ExpressiveHapticPattern.ERROR_REJECT -> HapticFeedbackType.LongPress
                else -> HapticFeedbackType.TextHandleMove
            }
            fb.performHapticFeedback(type)
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryVibrateHardware(
        v: Vibrator,
        pattern: ExpressiveHapticPattern,
        intensity: HapticIntensity,
        caps: HapticCapabilitiesSnapshot,
        isManualPreview: Boolean = false
    ): Boolean {
        return try {
            // 1. Composition プリミティブ合成 (HIGH_DEF_LRA または Samsung が十分なプリミティブ対応時)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportedPrimitiveSet.isNotEmpty()) {
                val preferComposition = caps.profile == HapticDeviceProfile.HIGH_DEF_LRA ||
                    (caps.profile == HapticDeviceProfile.SAMSUNG_HD_LRA &&
                        VibrationEffect.Composition.PRIMITIVE_CLICK in supportedPrimitiveSet)
                if (preferComposition) {
                    val steps = ExpressiveHapticProfiles.buildPrimitiveSteps(pattern, intensity, supportedPrimitiveSet)
                    if (steps.isNotEmpty()) {
                        val comp = VibrationEffect.startComposition()
                        for (step in steps) {
                            comp.addPrimitive(step.primitiveId, step.scale, step.delayMs)
                        }
                        vibrateWithAttributes(v, comp.compose(), pattern.isUrgentAlert && !isManualPreview)
                        return true
                    }
                }
            }

            // 2. Android O+ Amplitude Waveform (全端末で滑らかなエンベロープを描画)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val spec = ExpressiveHapticProfiles.buildWaveformSpec(pattern, intensity, caps.profile)
                val effect = if (caps.hasAmplitudeControl) {
                    VibrationEffect.createWaveform(spec.timingsMs, spec.amplitudes, -1)
                } else {
                    VibrationEffect.createWaveform(ExpressiveHapticProfiles.toLegacyOnOffPattern(spec), -1)
                }
                vibrateWithAttributes(v, effect, pattern.isUrgentAlert && !isManualPreview)
                return true
            }

            // 3. API 24-25 レガシー
            val spec = ExpressiveHapticProfiles.buildWaveformSpec(pattern, intensity, caps.profile)
            @Suppress("DEPRECATION")
            v.vibrate(ExpressiveHapticProfiles.toLegacyOnOffPattern(spec), -1)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    private fun vibrateWithAttributes(v: Vibrator, effect: VibrationEffect, isUrgentAlert: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val usage = if (isUrgentAlert) {
                VibrationAttributes.USAGE_NOTIFICATION
            } else {
                VibrationAttributes.USAGE_TOUCH
            }
            val attrs = VibrationAttributes.Builder().setUsage(usage).build()
            v.vibrate(effect, attrs)
        } else {
            // API 26–32 ではシステム設定の「タッチ操作時のバイブレーション」を尊重する（緊急警報除く）
            if (!isUrgentAlert) {
                val systemTouchHapticsEnabled = try {
                    @Suppress("DEPRECATION")
                    val key = android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED
                    android.provider.Settings.System.getInt(
                        appContext.contentResolver,
                        key,
                        1
                    ) != 0
                } catch (_: Exception) {
                    true
                }
                if (!systemTouchHapticsEnabled) return
            }
            @Suppress("DEPRECATION")
            v.vibrate(effect)
        }
    }

    private fun tryPerformViewFeedback(view: View, pattern: ExpressiveHapticPattern): Boolean {
        val constant = when (pattern) {
            ExpressiveHapticPattern.SOFT_TAP -> HapticFeedbackConstants.CLOCK_TICK
            ExpressiveHapticPattern.EXPRESSIVE_BOUNCE -> HapticFeedbackConstants.KEYBOARD_TAP
            ExpressiveHapticPattern.SWITCH_ON -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.TOGGLE_ON
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            ExpressiveHapticPattern.SWITCH_OFF -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.TOGGLE_OFF
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            ExpressiveHapticPattern.SCRUB_TICK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            ExpressiveHapticPattern.DETENT_BOUNDARY,
            ExpressiveHapticPattern.DRAG_SWAP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.SEGMENT_TICK
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            ExpressiveHapticPattern.DRAG_PICKUP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.DRAG_START
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            ExpressiveHapticPattern.DRAG_DROP,
            ExpressiveHapticPattern.PULL_REFRESH_TRIGGER -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.GESTURE_END
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            ExpressiveHapticPattern.REFRESH_SUCCESS,
            ExpressiveHapticPattern.CELEBRATE_UNLOCK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.CONTEXT_CLICK
            }
            ExpressiveHapticPattern.ERROR_REJECT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            else -> HapticFeedbackConstants.CONTEXT_CLICK
        }
        return view.performHapticFeedback(constant)
    }

    companion object {
        @Volatile
        private var INSTANCE: ExpressiveHapticController? = null

        fun getInstance(context: Context): ExpressiveHapticController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExpressiveHapticController(context).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Compose UI から参照するための統合ハプティクスハンドル
 */
class ComposeExpressiveHaptics(
    val controller: ExpressiveHapticController?,
    private val view: View?,
    private val composeFallback: HapticFeedback?
) {
    fun play(
        pattern: ExpressiveHapticPattern,
        bypassThrottle: Boolean = false
    ) {
        val ctrl = controller
        if (ctrl != null) {
            ctrl.trigger(
                pattern = pattern,
                view = view,
                composeFallback = composeFallback,
                bypassThrottle = bypassThrottle
            )
        } else {
            val fallbackType = when (pattern) {
                ExpressiveHapticPattern.DRAG_PICKUP,
                ExpressiveHapticPattern.ALERT_CRITICAL_PULSE -> HapticFeedbackType.LongPress
                else -> HapticFeedbackType.TextHandleMove
            }
            composeFallback?.performHapticFeedback(fallbackType)
        }
    }

    fun toggle(checked: Boolean) {
        play(if (checked) ExpressiveHapticPattern.SWITCH_ON else ExpressiveHapticPattern.SWITCH_OFF)
    }

    fun playWeatherSignature(condition: WeatherCondition) {
        val pattern = ExpressiveHapticPattern.forWeatherCondition(condition) ?: return
        play(pattern)
    }

    fun playRaindropPulse(strength: Float) {
        controller?.triggerRaindropPulse(
            strength = strength,
            view = view,
            composeFallback = composeFallback
        ) ?: composeFallback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

val LocalExpressiveHaptics = staticCompositionLocalOf {
    ComposeExpressiveHaptics(controller = null, view = null, composeFallback = null)
}

@Composable
fun rememberExpressiveHaptics(): ComposeExpressiveHaptics {
    val context = LocalContext.current
    val view = LocalView.current
    val composeHaptic = LocalHapticFeedback.current
    val controller = remember(context) { ExpressiveHapticController.getInstance(context) }
    val settingsVer by WeatherRepository.settingsVersion.collectAsState()

    LaunchedEffect(controller, settingsVer) {
        val repo = WeatherRepository(context = context)
        controller.intensity = repo.getHapticIntensity()
        controller.weatherHapticsEnabled = repo.isWeatherHapticsEnabled()
    }

    return remember(controller, view, composeHaptic) {
        ComposeExpressiveHaptics(
            controller = controller,
            view = view,
            composeFallback = composeHaptic
        )
    }
}
