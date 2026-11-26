package io.github.nemynew.expressiveweather.theme

import androidx.compose.ui.graphics.Color
import io.github.nemynew.expressiveweather.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MaterialYouHarmonizerTest {

    @Test
    fun okLchRoundTripPreservesSrgbColorsWithinTolerance() {
        val samples = listOf(
            Color(0xFF006492),
            Color(0xFF6750A4),
            Color(0xFF006C4C),
            Color(0xFF8E4F00),
            Color(0xFF9C4146),
            Color(0xFF4F616E),
            Color(0xFFE53935),
            Color(0xFF1E88E5)
        )
        samples.forEach { original ->
            val lch = MaterialYouHarmonizer.colorToOkLch(original)
            val reconstructed = MaterialYouHarmonizer.okLchToColor(lch.l, lch.c, lch.hDeg)
            assertTrue("Red mismatch for $original", abs(original.red - reconstructed.red) < 0.01f)
            assertTrue("Green mismatch for $original", abs(original.green - reconstructed.green) < 0.01f)
            assertTrue("Blue mismatch for $original", abs(original.blue - reconstructed.blue) < 0.01f)
        }
    }

    @Test
    fun harmonizeHuePullsTowardSeedWithinMax15Degrees() {
        val shifted = MaterialYouHarmonizer.harmonizeHue(
            designHueDeg = 60f,
            sourceHueDeg = 200f,
            sourceChroma = 0.14f
        )
        // Diff = +140°, 140 * 0.35 = 49° -> clamped to +15° => 75°
        assertEquals(75f, shifted, 0.01f)

        val smallShift = MaterialYouHarmonizer.harmonizeHue(
            designHueDeg = 200f,
            sourceHueDeg = 220f,
            sourceChroma = 0.14f
        )
        // Diff = +20°, 20 * 0.35 = 7° => 207°
        assertEquals(207f, smallShift, 0.01f)

        val wrapAround = MaterialYouHarmonizer.harmonizeHue(
            designHueDeg = 355f,
            sourceHueDeg = 15f,
            sourceChroma = 0.14f
        )
        // Shortest arc is +20°, 20 * 0.35 = +7° => 2°
        assertEquals(2f, wrapAround, 0.01f)

        val neutralSeed = MaterialYouHarmonizer.harmonizeHue(
            designHueDeg = 120f,
            sourceHueDeg = 260f,
            sourceChroma = 0.005f
        )
        assertEquals(120f, neutralSeed, 0.01f)
    }

    @Test
    fun materialYouColorfulRolesMeetWcagAaContrastInLightAndDark() {
        val schemes = listOf(
            SkyOceanLightColorScheme to false,
            SkyOceanDarkColorScheme to true,
            EmeraldForestLightColorScheme to false,
            EmeraldForestDarkColorScheme to true,
            SunsetAmberLightColorScheme to false,
            SunsetAmberDarkColorScheme to true,
            LavenderTwilightLightColorScheme to false,
            LavenderTwilightDarkColorScheme to true,
            SakuraBlossomLightColorScheme to false,
            SakuraBlossomDarkColorScheme to true
        )

        schemes.forEach { (scheme, isDark) ->
            val palette = MaterialYouHarmonizer.buildPalette(
                colorScheme = scheme,
                isDark = isDark,
                mode = AccentColorMode.MATERIAL_YOU_COLORFUL
            )
            val allRoles = listOf(
                palette.wind,
                palette.uv,
                palette.sun,
                palette.sunset,
                palette.aqi,
                palette.humidity,
                palette.precipitation,
                palette.pressure,
                palette.visibility,
                palette.feelsLike,
                palette.moon
            ) + palette.severityTones + palette.drynessTones + palette.tempTones

            allRoles.forEach { role ->
                val accentContrast = MaterialYouHarmonizer.contrastRatio(role.onAccent, role.accent)
                val containerContrast = MaterialYouHarmonizer.contrastRatio(role.onContainer, role.container)
                assertTrue(
                    "Accent contrast too low ($accentContrast) for isDark=$isDark",
                    accentContrast >= 4.5
                )
                assertTrue(
                    "Container contrast too low ($containerContrast) for isDark=$isDark",
                    containerContrast >= 4.5
                )
            }
        }
    }

    @Test
    fun threeAccentModesProduceDistinctPalettesAndHarmonizeToSeed() {
        val colorfulBlue = MaterialYouHarmonizer.buildPalette(
            SkyOceanLightColorScheme, isDark = false, mode = AccentColorMode.MATERIAL_YOU_COLORFUL
        )
        val colorfulAmber = MaterialYouHarmonizer.buildPalette(
            SunsetAmberLightColorScheme, isDark = false, mode = AccentColorMode.MATERIAL_YOU_COLORFUL
        )
        val tonal = MaterialYouHarmonizer.buildPalette(
            SkyOceanLightColorScheme, isDark = false, mode = AccentColorMode.MATERIAL_YOU_TONAL
        )
        val classic = MaterialYouHarmonizer.buildPalette(
            SkyOceanLightColorScheme, isDark = false, mode = AccentColorMode.CLASSIC_VIVID
        )

        // Material You カラフルはシードテーマに応じて色相が実際に調和変化する
        assertNotEquals(colorfulBlue.aqi.accent, colorfulAmber.aqi.accent)
        // 3モードが互いに異なるパレットを返す
        assertNotEquals(colorfulBlue.sun.accent, tonal.sun.accent)
        assertNotEquals(colorfulBlue.sun.accent, classic.sun.accent)
        // Tonal モードは完全に colorScheme のトークンのみを使用
        assertEquals(SkyOceanLightColorScheme.primary, tonal.wind.accent)
        assertEquals(SkyOceanLightColorScheme.tertiary, tonal.uv.accent)
        // 全 8 言語のタイトル・説明が空でない
        AccentColorMode.entries.forEach { mode ->
            assertEquals(mode, AccentColorMode.fromId(mode.id))
            AppLanguage.entries.forEach { lang ->
                assertTrue(mode.title(lang).isNotBlank())
                assertTrue(mode.subtitle(lang).isNotBlank())
            }
        }
    }
}
