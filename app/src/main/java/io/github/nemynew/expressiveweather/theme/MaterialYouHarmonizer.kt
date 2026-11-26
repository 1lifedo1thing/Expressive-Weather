package io.github.nemynew.expressiveweather.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Material Design 3 規定の 4 トーンロール（accent / onAccent / container / onContainer）。
 */
@Immutable
data class HarmonizedToneRoles(
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val onContainer: Color
)

/**
 * アプリ全体のセマンティックアクセントカラーパレット。
 *
 * [AccentColorMode]（Material You カラフル / Material You テーマ統一 / クラシック カラフル）の
 * 3 モードすべてを単一データ構造で表現し、Bento タイル・週間グラフ・レーダー・全詳細画面へ一貫供給する。
 */
@Immutable
data class MaterialYouAccentPalette(
    val mode: AccentColorMode,
    val isDark: Boolean,
    val wind: HarmonizedToneRoles,
    val uv: HarmonizedToneRoles,
    val sun: HarmonizedToneRoles,
    val sunset: HarmonizedToneRoles,
    val aqi: HarmonizedToneRoles,
    val humidity: HarmonizedToneRoles,
    val precipitation: HarmonizedToneRoles,
    val pressure: HarmonizedToneRoles,
    val visibility: HarmonizedToneRoles,
    val feelsLike: HarmonizedToneRoles,
    val moon: HarmonizedToneRoles,
    /** 5 段階の指標カラー（良い/低 -> 快適/中 -> 注意/やや高 -> 警戒/高 -> 危険/極端） */
    val severityTones: List<HarmonizedToneRoles>,
    /** 5 段階の乾燥スコアカラー（しっとり -> やや乾燥 -> 乾燥 -> カラカラ -> 超カラカラ） */
    val drynessTones: List<HarmonizedToneRoles>,
    /** 5 段階の気温カラー（寒い -> 涼しい -> 快適 -> 暑い -> 猛暑） */
    val tempTones: List<HarmonizedToneRoles>,
    /** 週間最高気温ラインの 3 点グラデーション */
    val weeklyMaxGradient: List<Color>,
    /** 週間最低気温ラインの 3 点グラデーション */
    val weeklyMinGradient: List<Color>,
    /** レーダータイムライン（実況 / 外挿 / 予報） */
    val radarObs: Color,
    val radarExtrap: Color,
    val radarForecast: Color
) {
    fun severity(tier: Int): HarmonizedToneRoles =
        severityTones[tier.coerceIn(0, severityTones.lastIndex)]

    fun dryness(ordinal: Int): HarmonizedToneRoles =
        drynessTones[ordinal.coerceIn(0, drynessTones.lastIndex)]

    fun temperature(tempC: Double): HarmonizedToneRoles = when {
        tempC >= 30.0 -> tempTones[4]
        tempC >= 25.0 -> tempTones[3]
        tempC >= 18.0 -> tempTones[2]
        tempC >= 10.0 -> tempTones[1]
        else -> tempTones[0]
    }
}

/**
 * 純粋 Kotlin による OkLab / OkLCH ベースの Material You ハーモナイゼーション＆M3 トーナルロール生成器。
 *
 * - 外部依存なし（APK サイズ増なし）
 * - Material Color Utilities の `Blend.harmonize` と同様に、各セマンティック色相を
 *   現在の `colorScheme.primary` 色相へ向かって最大 15°（回転係数 0.35）だけ引き寄せる
 * - Light（Tone 40 / 100 / 90 / 10）と Dark（Tone 80 / 20 / 30 / 90）の WCAG AA 準拠トーンを生成
 */
object MaterialYouHarmonizer {

    data class OkLch(val l: Float, val c: Float, val hDeg: Float)

    private const val HARMONIZE_FACTOR = 0.35f
    private const val MAX_HARMONIZE_DEG = 15f

    /** sRGB [Color] -> OkLCH */
    fun colorToOkLch(color: Color): OkLch {
        val r = srgbToLinear(color.red)
        val g = srgbToLinear(color.green)
        val b = srgbToLinear(color.blue)

        val l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b
        val m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b
        val s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b

        val lCbrt = cbrt(l.toDouble()).toFloat()
        val mCbrt = cbrt(m.toDouble()).toFloat()
        val sCbrt = cbrt(s.toDouble()).toFloat()

        val okL = 0.2104542553f * lCbrt + 0.7936177850f * mCbrt - 0.0040720468f * sCbrt
        val okA = 1.9779984951f * lCbrt - 2.4285922050f * mCbrt + 0.4505937099f * sCbrt
        val okB = 0.0259040371f * lCbrt + 0.7827717662f * mCbrt - 0.8086757660f * sCbrt

        val c = sqrt(okA * okA + okB * okB)
        var h = (atan2(okB.toDouble(), okA.toDouble()) * 180.0 / PI).toFloat()
        if (h < 0f) h += 360f
        return OkLch(okL.coerceIn(0f, 1f), c.coerceAtLeast(0f), h)
    }

    /**
     * OkLCH -> sRGB [Color]（sRGB ガモット外の場合は二分探索で彩度 C を滑らかに縮めて色相・明度を厳守）。
     */
    fun okLchToColor(l: Float, c: Float, hDeg: Float): Color {
        val clampedL = l.coerceIn(0f, 1f)
        val normH = ((hDeg % 360f) + 360f) % 360f
        val hRad = normH * (PI / 180.0)
        val cosH = cos(hRad).toFloat()
        val sinH = sin(hRad).toFloat()

        var lo = 0f
        var hi = c.coerceAtLeast(0f)
        if (isInSrgbGamut(clampedL, hi * cosH, hi * sinH)) {
            return okLabToSrgbColor(clampedL, hi * cosH, hi * sinH)
        }
        repeat(14) {
            val mid = (lo + hi) * 0.5f
            if (isInSrgbGamut(clampedL, mid * cosH, mid * sinH)) {
                lo = mid
            } else {
                hi = mid
            }
        }
        return okLabToSrgbColor(clampedL, lo * cosH, lo * sinH)
    }

    /**
     * 設計基準色相 [designHueDeg] をシード色相 [sourceHueDeg] 方向へ最大 [maxShiftDeg] だけ調和回転させる。
     */
    fun harmonizeHue(
        designHueDeg: Float,
        sourceHueDeg: Float,
        sourceChroma: Float = 0.1f,
        factor: Float = HARMONIZE_FACTOR,
        maxShiftDeg: Float = MAX_HARMONIZE_DEG
    ): Float {
        // 無彩色シード（グレー系テーマ等）の場合は色相回転を抑える
        if (sourceChroma < 0.015f) return ((designHueDeg % 360f) + 360f) % 360f
        val diff = ((sourceHueDeg - designHueDeg + 540f) % 360f) - 180f
        val rotation = min(abs(diff) * factor, maxShiftDeg) * if (diff >= 0f) 1f else -1f
        return ((designHueDeg + rotation) % 360f + 360f) % 360f
    }

    /**
     * 指定基準色相から、現在テーマの [sourcePrimary] に調和した M3 Expressive 4 トーンロールを生成する。
     *
     * 色相ごとの知覚最適明度（黄・アンバーは明るく澄んだトーン、青・紫は深みのあるトーン）で生成し、
     * [onAccent] は WCAG AA (>= 4.5:1) を満たす白系／濃色インクを自動選択する。
     */
    fun createHarmonizedRoles(
        designHueDeg: Float,
        sourcePrimary: Color,
        isDark: Boolean,
        chromaScale: Float = 1.0f
    ): HarmonizedToneRoles {
        val sourceLch = colorToOkLch(sourcePrimary)
        val h = harmonizeHue(designHueDeg, sourceLch.hDeg, sourceLch.c)
        return if (isDark) {
            // Material 3 Expressive Dark Tonal Roles: 明るいパステル発光 Tone 80 / 20 / 32 / 92
            val accent = okLchToColor(0.80f, 0.145f * chromaScale, h)
            val onAccent = okLchToColor(0.20f, 0.060f * chromaScale, h)
            val container = okLchToColor(0.34f, 0.095f * chromaScale, h)
            val onContainer = okLchToColor(0.93f, 0.050f * chromaScale, h)
            HarmonizedToneRoles(
                accent = accent,
                onAccent = onAccent,
                container = container,
                onContainer = onContainer
            )
        } else {
            // Material 3 Expressive Light Tonal Roles: 色相ごとの澄んだ発色（黄・シアンが泥濁りしない明度設計）
            val (accentL, accentC) = when {
                h in 48f..115f -> 0.74f to 0.170f   // アンバー・ゴールド・イエロー（明るく晴れやか）
                h in 115f..225f -> 0.63f to 0.155f  // ミント・エメラルド・シアン・スカイ（爽やか）
                h < 48f || h >= 340f -> 0.60f to 0.180f // コーラル・サンセット・レッド（鮮明）
                else -> 0.56f to 0.165f             // オーシャンブルー・インディゴ・バイオレット（クリア）
            }
            val accent = okLchToColor(accentL, accentC * chromaScale, h)
            val lightInk = okLchToColor(0.99f, 0.008f, h)
            val darkInk = okLchToColor(0.18f, 0.055f * chromaScale, h)
            val onAccent = if (contrastRatio(lightInk, accent) >= 4.5) {
                lightInk
            } else if (contrastRatio(darkInk, accent) >= 4.5) {
                darkInk
            } else {
                // どちらのインクも 4.5 未満になりそうな中間明度帯は一段だけ明度を微調整して WCAG AA を厳守
                val adjustedAccent = okLchToColor(0.51f, accentC * chromaScale, h)
                return HarmonizedToneRoles(
                    accent = adjustedAccent,
                    onAccent = lightInk,
                    container = okLchToColor(0.92f, 0.075f * chromaScale, h),
                    onContainer = okLchToColor(0.22f, 0.080f * chromaScale, h)
                )
            }
            val container = okLchToColor(0.92f, 0.075f * chromaScale, h)
            val onContainer = okLchToColor(0.22f, 0.080f * chromaScale, h)
            HarmonizedToneRoles(
                accent = accent,
                onAccent = onAccent,
                container = container,
                onContainer = onContainer
            )
        }
    }

    /**
     * WCAG 2.1 相対輝度コントラスト比（ユニットテスト＆可読性保証用）。
     */
    fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val bright = maxOf(l1, l2)
        val dark = minOf(l1, l2)
        return (bright + 0.05) / (dark + 0.05)
    }

    private fun relativeLuminance(c: Color): Double {
        val r = srgbToLinear(c.red).toDouble()
        val g = srgbToLinear(c.green).toDouble()
        val b = srgbToLinear(c.blue).toDouble()
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * 3 モードすべての [MaterialYouAccentPalette] を構築する。
     */
    fun buildPalette(
        colorScheme: ColorScheme,
        isDark: Boolean,
        mode: AccentColorMode
    ): MaterialYouAccentPalette = when (mode) {
        AccentColorMode.MATERIAL_YOU_COLORFUL -> buildMaterialYouColorful(colorScheme, isDark)
        AccentColorMode.MATERIAL_YOU_TONAL -> buildMaterialYouTonal(colorScheme, isDark)
        AccentColorMode.CLASSIC_VIVID -> buildClassicVivid(isDark)
    }

    private fun buildMaterialYouColorful(
        colorScheme: ColorScheme,
        isDark: Boolean
    ): MaterialYouAccentPalette {
        val seed = colorScheme.primary
        val wind = createHarmonizedRoles(205f, seed, isDark)
        val uv = createHarmonizedRoles(72f, seed, isDark)
        val sun = createHarmonizedRoles(64f, seed, isDark)
        val sunset = createHarmonizedRoles(36f, seed, isDark)
        val aqi = createHarmonizedRoles(156f, seed, isDark)
        val humidity = createHarmonizedRoles(246f, seed, isDark)
        val precipitation = createHarmonizedRoles(236f, seed, isDark)
        val pressure = createHarmonizedRoles(292f, seed, isDark)
        val visibility = createHarmonizedRoles(222f, seed, isDark)
        val feelsLike = createHarmonizedRoles(26f, seed, isDark)
        val moon = createHarmonizedRoles(268f, seed, isDark)

        val tierGreen = createHarmonizedRoles(148f, seed, isDark)
        val tierYellow = createHarmonizedRoles(84f, seed, isDark)
        val tierOrange = createHarmonizedRoles(54f, seed, isDark)
        val tierRed = createHarmonizedRoles(25f, seed, isDark)
        val tierPurple = createHarmonizedRoles(314f, seed, isDark)
        val tierBlue = createHarmonizedRoles(242f, seed, isDark)
        val tierSky = createHarmonizedRoles(224f, seed, isDark)

        val severityTones = listOf(tierGreen, tierYellow, tierOrange, tierRed, tierPurple)
        val drynessTones = listOf(tierBlue, tierGreen, tierYellow, tierOrange, tierRed)
        val tempTones = listOf(moon, tierSky, tierGreen, tierOrange, tierRed)

        return MaterialYouAccentPalette(
            mode = AccentColorMode.MATERIAL_YOU_COLORFUL,
            isDark = isDark,
            wind = wind,
            uv = uv,
            sun = sun,
            sunset = sunset,
            aqi = aqi,
            humidity = humidity,
            precipitation = precipitation,
            pressure = pressure,
            visibility = visibility,
            feelsLike = feelsLike,
            moon = moon,
            severityTones = severityTones,
            drynessTones = drynessTones,
            tempTones = tempTones,
            weeklyMaxGradient = listOf(tierRed.accent, tierOrange.accent, tierYellow.accent),
            weeklyMinGradient = listOf(tierSky.accent, tierBlue.accent, pressure.accent),
            radarObs = tierBlue.accent,
            radarExtrap = tierGreen.accent,
            radarForecast = pressure.accent
        )
    }

    private fun buildMaterialYouTonal(
        colorScheme: ColorScheme,
        isDark: Boolean
    ): MaterialYouAccentPalette {
        val primaryRole = HarmonizedToneRoles(
            accent = colorScheme.primary,
            onAccent = colorScheme.onPrimary,
            container = colorScheme.primaryContainer,
            onContainer = colorScheme.onPrimaryContainer
        )
        val secondaryRole = HarmonizedToneRoles(
            accent = colorScheme.secondary,
            onAccent = colorScheme.onSecondary,
            container = colorScheme.secondaryContainer,
            onContainer = colorScheme.onSecondaryContainer
        )
        val tertiaryRole = HarmonizedToneRoles(
            accent = colorScheme.tertiary,
            onAccent = colorScheme.onTertiary,
            container = colorScheme.tertiaryContainer,
            onContainer = colorScheme.onTertiaryContainer
        )
        val warmErrorRole = HarmonizedToneRoles(
            accent = lerp(colorScheme.tertiary, colorScheme.error, 0.55f),
            onAccent = colorScheme.onError,
            container = lerp(colorScheme.tertiaryContainer, colorScheme.errorContainer, 0.55f),
            onContainer = colorScheme.onErrorContainer
        )
        val errorRole = HarmonizedToneRoles(
            accent = colorScheme.error,
            onAccent = colorScheme.onError,
            container = colorScheme.errorContainer,
            onContainer = colorScheme.onErrorContainer
        )

        val severityTones = listOf(secondaryRole, primaryRole, tertiaryRole, warmErrorRole, errorRole)
        val drynessTones = listOf(primaryRole, secondaryRole, tertiaryRole, warmErrorRole, errorRole)
        val tempTones = listOf(
            HarmonizedToneRoles(
                accent = lerp(colorScheme.primary, colorScheme.secondary, 0.5f),
                onAccent = colorScheme.onPrimary,
                container = colorScheme.secondaryContainer,
                onContainer = colorScheme.onSecondaryContainer
            ),
            secondaryRole,
            primaryRole,
            warmErrorRole,
            errorRole
        )

        return MaterialYouAccentPalette(
            mode = AccentColorMode.MATERIAL_YOU_TONAL,
            isDark = isDark,
            wind = primaryRole,
            uv = tertiaryRole,
            sun = primaryRole,
            sunset = secondaryRole,
            aqi = secondaryRole,
            humidity = primaryRole,
            precipitation = primaryRole,
            pressure = primaryRole,
            visibility = primaryRole,
            feelsLike = tertiaryRole,
            moon = primaryRole,
            severityTones = severityTones,
            drynessTones = drynessTones,
            tempTones = tempTones,
            weeklyMaxGradient = listOf(colorScheme.primary, colorScheme.primary, colorScheme.primary),
            weeklyMinGradient = listOf(colorScheme.secondary, colorScheme.secondary, colorScheme.secondary),
            radarObs = colorScheme.primary,
            radarExtrap = colorScheme.tertiary,
            radarForecast = colorScheme.secondary
        )
    }

    private fun buildClassicVivid(isDark: Boolean): MaterialYouAccentPalette {
        val wind = HarmonizedToneRoles(
            accent = Color(0xFF00BCD4),
            onAccent = Color.White,
            container = if (isDark) Color(0xFF004D57) else Color(0xFFB2EBF2),
            onContainer = if (isDark) Color(0xFFE0F7FA) else Color(0xFF00363D)
        )
        val uv = HarmonizedToneRoles(
            accent = Color(0xFFFFA000),
            onAccent = Color(0xFF3E2723),
            container = if (isDark) Color(0xFF5C3A00) else Color(0xFFFFECB3),
            onContainer = if (isDark) Color(0xFFFFF8E1) else Color(0xFF3E2723)
        )
        val sun = HarmonizedToneRoles(
            accent = Color(0xFFFFA000),
            onAccent = Color(0xFF3E2723),
            container = if (isDark) Color(0xFF663D00) else Color(0xFFFFE0B2),
            onContainer = if (isDark) Color(0xFFFFF3E0) else Color(0xFF4E2600)
        )
        val sunset = HarmonizedToneRoles(
            accent = Color(0xFFFF7043),
            onAccent = Color.White,
            container = if (isDark) Color(0xFF63210E) else Color(0xFFFFCCBC),
            onContainer = if (isDark) Color(0xFFFBE9E7) else Color(0xFF4A1506)
        )
        val aqi = HarmonizedToneRoles(
            accent = Color(0xFF00BFA5),
            onAccent = Color.White,
            container = if (isDark) Color(0xFF004D40) else Color(0xFFB2DFDB),
            onContainer = if (isDark) Color(0xFFE0F2F1) else Color(0xFF00332C)
        )
        val humidity = HarmonizedToneRoles(
            accent = Color(0xFF1E88E5),
            onAccent = Color.White,
            container = if (isDark) Color(0xFF1565C0) else Color(0xFFBBDEFB),
            onContainer = if (isDark) Color(0xFFE3F2FD) else Color(0xFF0D47A1)
        )
        val precipitation = HarmonizedToneRoles(
            accent = Color(0xFF0091EA),
            onAccent = Color.White,
            container = if (isDark) Color(0xFF004A77) else Color(0xFFB3E5FC),
            onContainer = if (isDark) Color(0xFFE1F5FE) else Color(0xFF003252)
        )
        val pressure = HarmonizedToneRoles(
            accent = Color(0xFF7C4DFF),
            onAccent = Color.White,
            container = if (isDark) Color(0xFF3C1F8C) else Color(0xFFD1C4E9),
            onContainer = if (isDark) Color(0xFFEDE7F6) else Color(0xFF280B6B)
        )
        val visibility = HarmonizedToneRoles(
            accent = Color(0xFF00B0FF),
            onAccent = Color.White,
            container = if (isDark) Color(0xFF004E75) else Color(0xFFB3E5FC),
            onContainer = if (isDark) Color(0xFFE1F5FE) else Color(0xFF003452)
        )
        val feelsLike = HarmonizedToneRoles(
            accent = Color(0xFFFF5252),
            onAccent = Color.White,
            container = if (isDark) Color(0xFF7A1717) else Color(0xFFFFCDD2),
            onContainer = if (isDark) Color(0xFFFFEBEE) else Color(0xFF5F0B0B)
        )
        val moon = HarmonizedToneRoles(
            accent = if (isDark) Color(0xFF9FA8DA) else Color(0xFF3F51B5),
            onAccent = if (isDark) Color(0xFF1A237E) else Color.White,
            container = if (isDark) Color(0xFF283593) else Color(0xFFC5CAE9),
            onContainer = if (isDark) Color(0xFFE8EAF6) else Color(0xFF1A237E)
        )

        val tierGreen = HarmonizedToneRoles(Color(0xFF4CAF50), Color.White, Color(0xFFC8E6C9), Color(0xFF1B5E20))
        val tierYellow = HarmonizedToneRoles(Color(0xFFFFB300), Color(0xFF2E1C00), Color(0xFFFFECB3), Color(0xFF4E3400))
        val tierOrange = HarmonizedToneRoles(Color(0xFFFF9800), Color.White, Color(0xFFFFE0B2), Color(0xFF5D2F00))
        val tierRed = HarmonizedToneRoles(Color(0xFFE53935), Color.White, Color(0xFFFFCDD2), Color(0xFF6B0F0E))
        val tierPurple = HarmonizedToneRoles(Color(0xFF8E24AA), Color.White, Color(0xFFE1BEE7), Color(0xFF4A0E5C))
        val tierBlue = HarmonizedToneRoles(Color(0xFF0288D1), Color.White, Color(0xFFB3E5FC), Color(0xFF014361))
        val tierSky = HarmonizedToneRoles(Color(0xFF03A9F4), Color.White, Color(0xFFB3E5FC), Color(0xFF014361))
        val tierIndigo = HarmonizedToneRoles(Color(0xFF3F51B5), Color.White, Color(0xFFC5CAE9), Color(0xFF1A237E))

        return MaterialYouAccentPalette(
            mode = AccentColorMode.CLASSIC_VIVID,
            isDark = isDark,
            wind = wind,
            uv = uv,
            sun = sun,
            sunset = sunset,
            aqi = aqi,
            humidity = humidity,
            precipitation = precipitation,
            pressure = pressure,
            visibility = visibility,
            feelsLike = feelsLike,
            moon = moon,
            severityTones = listOf(tierGreen, tierYellow, tierOrange, tierRed, tierPurple),
            drynessTones = listOf(tierBlue, tierGreen, tierYellow, tierOrange, tierRed),
            tempTones = listOf(tierIndigo, tierSky, tierGreen, tierOrange, tierRed),
            weeklyMaxGradient = listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFD600)),
            weeklyMinGradient = listOf(Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFF7C4DFF)),
            radarObs = Color(0xFF2196F3),
            radarExtrap = Color(0xFF00E676),
            radarForecast = Color(0xFFB388FF)
        )
    }

    private fun srgbToLinear(channel: Float): Float {
        val c = channel.coerceIn(0f, 1f)
        return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    }

    private fun linearToSrgb(channel: Float): Float {
        val c = channel.coerceIn(0f, 1f)
        return if (c <= 0.0031308f) 12.92f * c else (1.055f * c.toDouble().pow(1.0 / 2.4).toFloat() - 0.055f).coerceIn(0f, 1f)
    }

    private fun okLabToLinearRgb(okL: Float, okA: Float, okB: Float): Triple<Float, Float, Float> {
        val l_ = okL + 0.3963377774f * okA + 0.2158037573f * okB
        val m_ = okL - 0.1055613458f * okA - 0.0638541728f * okB
        val s_ = okL - 0.0894841775f * okA - 1.2914855480f * okB

        val l = l_ * l_ * l_
        val m = m_ * m_ * m_
        val s = s_ * s_ * s_

        val r = +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
        val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
        val b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
        return Triple(r, g, b)
    }

    private fun isInSrgbGamut(okL: Float, okA: Float, okB: Float): Boolean {
        val (r, g, b) = okLabToLinearRgb(okL, okA, okB)
        val eps = -0.0005f
        return r >= eps && r <= 1.0005f && g >= eps && g <= 1.0005f && b >= eps && b <= 1.0005f
    }

    private fun okLabToSrgbColor(okL: Float, okA: Float, okB: Float): Color {
        val (r, g, b) = okLabToLinearRgb(okL, okA, okB)
        return Color(
            red = linearToSrgb(r),
            green = linearToSrgb(g),
            blue = linearToSrgb(b),
            alpha = 1f
        )
    }
}
