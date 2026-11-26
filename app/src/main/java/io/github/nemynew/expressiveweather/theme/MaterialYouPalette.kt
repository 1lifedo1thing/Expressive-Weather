package io.github.nemynew.expressiveweather.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.nemynew.expressiveweather.data.DrynessRank

/**
 * 現在適用中の [AccentColorMode]（Material You カラフル / テーマ統一 / クラシック カラフル）。
 * [ExpressiveWeatherTheme] が提供し、ネストされた [ExpressiveWeatherTheme] でも自動継承される。
 */
val LocalAccentColorMode = compositionLocalOf { AccentColorMode.CLASSIC_VIVID }

/**
 * 現在適用中の [MaterialYouAccentPalette]。
 * [ExpressiveWeatherTheme] 内で `colorScheme`・`darkTheme`・`accentColorMode` から自動構築される。
 */
val LocalMaterialYouPalette = compositionLocalOf<MaterialYouAccentPalette?> { null }

/**
 * 現在の [MaterialYouAccentPalette] を取得する。
 * 万一 [ExpressiveWeatherTheme] 外で呼ばれても安全にフォールバック構築する。
 */
@Composable
fun currentAccentPalette(): MaterialYouAccentPalette {
    LocalMaterialYouPalette.current?.let { return it }
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalDarkTheme.current
    val mode = LocalAccentColorMode.current
    return MaterialYouHarmonizer.buildPalette(colorScheme, isDark, mode)
}

/**
 * Pure Material You (Material 3 ColorScheme) semantic color helpers (fallback / tonal mode).
 */
fun ColorScheme.severityColor(tier: Int): Color = when (tier.coerceIn(0, 4)) {
    0 -> secondary
    1 -> primary
    2 -> tertiary
    3 -> lerp(tertiary, error, 0.55f)
    else -> error
}

fun ColorScheme.severityContainerColors(tier: Int): Pair<Color, Color> = when (tier.coerceIn(0, 4)) {
    0 -> secondaryContainer to onSecondaryContainer
    1 -> primaryContainer to onPrimaryContainer
    2 -> tertiaryContainer to onTertiaryContainer
    3 -> lerp(tertiaryContainer, errorContainer, 0.55f) to onErrorContainer
    else -> errorContainer to onErrorContainer
}

fun ColorScheme.severitySolidColors(tier: Int): Pair<Color, Color> = when (tier.coerceIn(0, 4)) {
    0 -> secondary to onSecondary
    1 -> primary to onPrimary
    2 -> tertiary to onTertiary
    3 -> lerp(tertiary, error, 0.5f) to onError
    else -> error to onError
}

fun ColorScheme.severityGradientStops(): List<Color> = listOf(
    secondary,
    primary,
    tertiary,
    lerp(tertiary, error, 0.55f),
    error
)

fun ColorScheme.temperatureColor(tempC: Double): Color = when {
    tempC >= 30.0 -> error
    tempC >= 25.0 -> lerp(tertiary, error, 0.5f)
    tempC >= 18.0 -> primary
    tempC >= 10.0 -> secondary
    else -> lerp(primary, secondary, 0.5f)
}

/**
 * [MaterialYouAccentPalette] に基づく [DrynessRank] のカラー。
 */
fun DrynessRank.accentColor(palette: MaterialYouAccentPalette): Color =
    palette.dryness(ordinal).accent

/**
 * 後方互換: 単一 [ColorScheme] ベースの [DrynessRank] カラー。
 */
fun DrynessRank.materialYouColor(colorScheme: ColorScheme): Color = when (this) {
    DrynessRank.MOIST -> colorScheme.primary
    DrynessRank.SLIGHTLY_DRY -> colorScheme.secondary
    DrynessRank.DRY -> colorScheme.tertiary
    DrynessRank.VERY_DRY -> lerp(colorScheme.tertiary, colorScheme.error, 0.55f)
    DrynessRank.EXTREME_DRY -> colorScheme.error
}
