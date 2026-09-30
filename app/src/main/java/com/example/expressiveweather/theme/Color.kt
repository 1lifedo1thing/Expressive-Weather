package com.example.expressiveweather.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.expressiveweather.data.WeatherCondition

// Primary Expressive tokens (Fallback palette when dynamic colors not available)
val ExpressivePrimary = Color(0xFF006494)
val ExpressivePrimaryContainer = Color(0xFFCBE6FF)
val ExpressiveSecondary = Color(0xFF006688)
val ExpressiveSecondaryContainer = Color(0xFFCCE8FF)
val ExpressiveTertiary = Color(0xFF8B4A61)
val ExpressiveTertiaryContainer = Color(0xFFFFD8E2)

data class WeatherAtmosphereTheme(
    val skyGradient: Brush,
    val tileSkyGradient: Brush,
    val glassBorder: Brush,
    val cardBackground: Color,
    val cardBorder: Color,
    val accentColor: Color,
    val textColorPrimary: Color,
    val textColorSecondary: Color,
    val skyTextColorPrimary: Color,
    val skyTextColorSecondary: Color,
    val pillBackground: Color,
    val particleColor: Color,
    val isDark: Boolean = false,
    val isDay: Boolean = !isDark,
    val topSkyColor: Color = Color(0xFF5CAAE6),
    val midSkyColor: Color = Color(0xFF3892D0)
)

fun getAtmosphereTheme(
    condition: WeatherCondition,
    colorScheme: ColorScheme,
    isDay: Boolean = true
): WeatherAtmosphereTheme {
    val isNight = !isDay
    val accent = when (condition) {
        WeatherCondition.CLEAR -> if (isNight) Color(0xFF64B5F6) else Color(0xFF006494)
        WeatherCondition.FEW_CLOUDS -> if (isNight) Color(0xFF81D4FA) else Color(0xFF0277BD)
        WeatherCondition.CLOUDY -> if (isNight) Color(0xFF90A4AE) else Color(0xFF455A64)
        WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> if (isNight) Color(0xFF4FC3F7) else Color(0xFF0288D1)
        WeatherCondition.THUNDERSTORM -> if (isNight) Color(0xFFB39DDB) else Color(0xFF5E35B1)
        WeatherCondition.SNOW -> if (isNight) Color(0xFF81D4FA) else Color(0xFF0288D1)
        WeatherCondition.FOG -> if (isNight) Color(0xFFB0BEC5) else Color(0xFF607D8B)
    }

    // 各天候と昼夜に応じた高品質でリアルな空色パレット
    val skyColors = if (isNight) {
        // 夜間（月・星・ダークスカイ）
        when (condition) {
            WeatherCondition.CLEAR -> listOf(
                Color(0xFF132840),
                Color(0xFF0D1C2E),
                Color(0xFF08121E),
                Color(0xFF040A12)
            )
            WeatherCondition.FEW_CLOUDS -> listOf(
                Color(0xFF14263D),
                Color(0xFF0E1C2E),
                Color(0xFF091320),
                Color(0xFF050C14)
            )
            WeatherCondition.CLOUDY -> listOf(
                Color(0xFF1A222B),
                Color(0xFF141A22),
                Color(0xFF0E1318),
                Color(0xFF090C10)
            )
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> listOf(
                Color(0xFF121D28),
                Color(0xFF0D161F),
                Color(0xFF080F16),
                Color(0xFF04080D)
            )
            WeatherCondition.THUNDERSTORM -> listOf(
                Color(0xFF151026),
                Color(0xFF0F0B1C),
                Color(0xFF0A0713),
                Color(0xFF05030A)
            )
            WeatherCondition.SNOW -> listOf(
                Color(0xFF13202C),
                Color(0xFF0E1822),
                Color(0xFF091119),
                Color(0xFF050A10)
            )
            WeatherCondition.FOG -> listOf(
                Color(0xFF161E24),
                Color(0xFF10171B),
                Color(0xFF0B1013),
                Color(0xFF06090B)
            )
        }
    } else {
        // 昼間（太陽光・青空）: 鮮やかでリアルな美しい青空と自然光
        when (condition) {
            WeatherCondition.CLEAR -> listOf(
                Color(0xFF5CAAE6), // 太陽光に照らされた明るい青空
                Color(0xFF3892D0), // 澄んだアズールブルー
                Color(0xFF2881C0), // 深い大気ブルー
                Color(0xFF2E88C8)  // 下部のなだらかな青空
            )
            WeatherCondition.FEW_CLOUDS -> listOf(
                Color(0xFF56A4E2),
                Color(0xFF3B8DC8),
                Color(0xFF2D7EBA),
                Color(0xFF3486C2)
            )
            WeatherCondition.CLOUDY -> listOf(
                Color(0xFF7B8E9F),
                Color(0xFF8FA2B3),
                Color(0xFFA5B6C6),
                Color(0xFFBDCCD8)
            )
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> listOf(
                Color(0xFF50687E),
                Color(0xFF647C92),
                Color(0xFF7A91A6),
                Color(0xFF96ABC0)
            )
            WeatherCondition.THUNDERSTORM -> listOf(
                Color(0xFF1E1B38),
                Color(0xFF2E294E),
                Color(0xFF453966),
                Color(0xFF5E497C)
            )
            WeatherCondition.SNOW -> listOf(
                Color(0xFF789DBB),
                Color(0xFF90B1CC),
                Color(0xFFAEC8DD),
                Color(0xFFCDE0EE)
            )
            WeatherCondition.FOG -> listOf(
                Color(0xFF8896A0),
                Color(0xFF9EACB6),
                Color(0xFFB5C2CB),
                Color(0xFFD0DCE3)
            )
        }
    }

    val skyGradient = Brush.verticalGradient(colors = skyColors)
    val topSkyColor = skyColors[0]
    val midSkyColor = skyColors.getOrElse(1) { skyColors[0] }

    val tileSkyGradient = skyGradient

    val glassBorder = Brush.verticalGradient(
        colors = listOf(
            Color(0x1A000000),
            Color(0x0A000000)
        )
    )

    return WeatherAtmosphereTheme(
        skyGradient = skyGradient,
        tileSkyGradient = tileSkyGradient,
        glassBorder = glassBorder,
        cardBackground = Color.White,
        cardBorder = Color(0x12000000),
        accentColor = accent,
        textColorPrimary = Color(0xFF191C1E),
        textColorSecondary = Color(0xFF44474E),
        skyTextColorPrimary = Color.White,
        skyTextColorSecondary = Color.White.copy(alpha = 0.85f),
        pillBackground = Color(0xFFE8F0FE),
        particleColor = if (isNight) Color(0xFF80D8FF) else Color(0xFF1976D2).copy(alpha = 0.5f),
        isDark = isNight,
        isDay = isDay,
        topSkyColor = topSkyColor,
        midSkyColor = midSkyColor
    )
}

/**
 * 天候（WeatherCondition）と昼夜（isDark）に基づいて、
 * アプリ全体のコンポーネント（ボタン、スライダー、Bento Grid、シート等）に適用される
 * 調和の取れた美しい Material 3 ColorScheme を動的に生成します。
 */
fun createWeatherColorScheme(condition: WeatherCondition, isDark: Boolean): ColorScheme {
    return if (isDark) {
        when (condition) {
            WeatherCondition.CLEAR -> darkColorScheme(
                primary = Color(0xFF8BCEFF),
                onPrimary = Color(0xFF00344F),
                primaryContainer = Color(0xFF004B70),
                onPrimaryContainer = Color(0xFFC9E6FF),
                secondary = Color(0xFFB7C9D9),
                onSecondary = Color(0xFF22323F),
                secondaryContainer = Color(0xFF384956),
                onSecondaryContainer = Color(0xFFD3E5F5),
                tertiary = Color(0xFFFFB77C),
                onTertiary = Color(0xFF4D2600),
                tertiaryContainer = Color(0xFF6E3900),
                onTertiaryContainer = Color(0xFFFFDCC1),
                background = Color(0xFF09131F),
                onBackground = Color(0xFFDEE3EA),
                surface = Color(0xFF0E1A29),
                onSurface = Color(0xFFDEE3EA),
                surfaceVariant = Color(0xFF1B2B3D),
                onSurfaceVariant = Color(0xFFC0C7D0),
                surfaceContainerHighest = Color(0xFF223448),
                outline = Color(0xFF8A929B),
                outlineVariant = Color(0xFF404850)
            )
            WeatherCondition.FEW_CLOUDS -> darkColorScheme(
                primary = Color(0xFF68D3FF),
                onPrimary = Color(0xFF003546),
                primaryContainer = Color(0xFF004D64),
                onPrimaryContainer = Color(0xFFBFE9FF),
                secondary = Color(0xFFB3CAD5),
                onSecondary = Color(0xFF1E333C),
                secondaryContainer = Color(0xFF354A53),
                onSecondaryContainer = Color(0xFFCEE6F2),
                tertiary = Color(0xFFC7C2EA),
                onTertiary = Color(0xFF302D4C),
                tertiaryContainer = Color(0xFF464364),
                onTertiaryContainer = Color(0xFFE4DFFF),
                background = Color(0xFF0A141E),
                onBackground = Color(0xFFDEE3E7),
                surface = Color(0xFF0F1B27),
                onSurface = Color(0xFFDEE3E7),
                surfaceVariant = Color(0xFF1C2C3A),
                onSurfaceVariant = Color(0xFFC0C8CD),
                surfaceContainerHighest = Color(0xFF233545),
                outline = Color(0xFF8A9297),
                outlineVariant = Color(0xFF40484D)
            )
            WeatherCondition.CLOUDY -> darkColorScheme(
                primary = Color(0xFFB0BEC5),
                onPrimary = Color(0xFF1E282D),
                primaryContainer = Color(0xFF37474F),
                onPrimaryContainer = Color(0xFFCFD8DC),
                secondary = Color(0xFFB0BEC5),
                onSecondary = Color(0xFF263238),
                secondaryContainer = Color(0xFF37474F),
                onSecondaryContainer = Color(0xFFECEFF1),
                tertiary = Color(0xFFCFD8DC),
                onTertiary = Color(0xFF273238),
                tertiaryContainer = Color(0xFF455A64),
                onTertiaryContainer = Color(0xFFECEFF1),
                background = Color(0xFF0E1318),
                onBackground = Color(0xFFE0E3E5),
                surface = Color(0xFF141C22),
                onSurface = Color(0xFFE0E3E5),
                surfaceVariant = Color(0xFF232C33),
                onSurfaceVariant = Color(0xFFC2C7CB),
                surfaceContainerHighest = Color(0xFF2A343C),
                outline = Color(0xFF8C9195),
                outlineVariant = Color(0xFF42474A)
            )
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> darkColorScheme(
                primary = Color(0xFF5FD4FD),
                onPrimary = Color(0xFF003545),
                primaryContainer = Color(0xFF004D63),
                onPrimaryContainer = Color(0xFFBAEAFF),
                secondary = Color(0xFFB2CAD4),
                onSecondary = Color(0xFF1D333B),
                secondaryContainer = Color(0xFF344A52),
                onSecondaryContainer = Color(0xFFCEE6F0),
                tertiary = Color(0xFF80D8FF),
                onTertiary = Color(0xFF003547),
                tertiaryContainer = Color(0xFF004D65),
                onTertiaryContainer = Color(0xFFBBE9FF),
                background = Color(0xFF08121A),
                onBackground = Color(0xFFDDE3E7),
                surface = Color(0xFF0E1A24),
                onSurface = Color(0xFFDDE3E7),
                surfaceVariant = Color(0xFF192A36),
                onSurfaceVariant = Color(0xFFBFC8CD),
                surfaceContainerHighest = Color(0xFF223442),
                outline = Color(0xFF899297),
                outlineVariant = Color(0xFF3F484D)
            )
            WeatherCondition.THUNDERSTORM -> darkColorScheme(
                primary = Color(0xFFD0BCFF),
                onPrimary = Color(0xFF381E72),
                primaryContainer = Color(0xFF4F378B),
                onPrimaryContainer = Color(0xFFE7DEFF),
                secondary = Color(0xFFCCC2DC),
                onSecondary = Color(0xFF332D41),
                secondaryContainer = Color(0xFF4A4458),
                onSecondaryContainer = Color(0xFFE8DEF8),
                tertiary = Color(0xFFFFD54F),
                onTertiary = Color(0xFF402D00),
                tertiaryContainer = Color(0xFF5C4300),
                onTertiaryContainer = Color(0xFFFFE082),
                background = Color(0xFF0C0916),
                onBackground = Color(0xFFE5E0EB),
                surface = Color(0xFF141021),
                onSurface = Color(0xFFE5E0EB),
                surfaceVariant = Color(0xFF231C33),
                onSurfaceVariant = Color(0xFFCAC4D0),
                surfaceContainerHighest = Color(0xFF2C243E),
                outline = Color(0xFF938F99),
                outlineVariant = Color(0xFF48454E)
            )
            WeatherCondition.SNOW -> darkColorScheme(
                primary = Color(0xFF86CEFF),
                onPrimary = Color(0xFF003450),
                primaryContainer = Color(0xFF004B72),
                onPrimaryContainer = Color(0xFFD0EDFF),
                secondary = Color(0xFFB6C9D8),
                onSecondary = Color(0xFF21323E),
                secondaryContainer = Color(0xFF374855),
                onSecondaryContainer = Color(0xFFD2E5F5),
                tertiary = Color(0xFF80DEEA),
                onTertiary = Color(0xFF00363B),
                tertiaryContainer = Color(0xFF004F55),
                onTertiaryContainer = Color(0xFF9EEFFD),
                background = Color(0xFF09131C),
                onBackground = Color(0xFFDEE3E8),
                surface = Color(0xFF0F1A25),
                onSurface = Color(0xFFDEE3E8),
                surfaceVariant = Color(0xFF1B2B38),
                onSurfaceVariant = Color(0xFFBFC7CD),
                surfaceContainerHighest = Color(0xFF233442),
                outline = Color(0xFF899197),
                outlineVariant = Color(0xFF3F474D)
            )
            WeatherCondition.FOG -> darkColorScheme(
                primary = Color(0xFFB0BEC5),
                onPrimary = Color(0xFF263238),
                primaryContainer = Color(0xFF37474F),
                onPrimaryContainer = Color(0xFFECEFF1),
                secondary = Color(0xFFC2C7CC),
                onSecondary = Color(0xFF2C3135),
                secondaryContainer = Color(0xFF42474C),
                onSecondaryContainer = Color(0xFFDEE3E8),
                tertiary = Color(0xFFB5C3BC),
                onTertiary = Color(0xFF20342E),
                tertiaryContainer = Color(0xFF374B44),
                onTertiaryContainer = Color(0xFFD1DFD8),
                background = Color(0xFF0E1214),
                onBackground = Color(0xFFDFE2E4),
                surface = Color(0xFF14191C),
                onSurface = Color(0xFFDFE2E4),
                surfaceVariant = Color(0xFF21282D),
                onSurfaceVariant = Color(0xFFC1C7CB),
                surfaceContainerHighest = Color(0xFF2A3137),
                outline = Color(0xFF8B9195),
                outlineVariant = Color(0xFF41474B)
            )
        }
    } else {
        when (condition) {
            WeatherCondition.CLEAR -> lightColorScheme(
                primary = Color(0xFF006492),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFC9E6FF),
                onPrimaryContainer = Color(0xFF001E30),
                secondary = Color(0xFF50606E),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFD3E5F5),
                onSecondaryContainer = Color(0xFF0C1D29),
                tertiary = Color(0xFFBA5500),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFFFDCC2),
                onTertiaryContainer = Color(0xFF3D1600),
                background = Color(0xFFF7F9FD),
                onBackground = Color(0xFF181C20),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF181C20),
                surfaceVariant = Color(0xFFDEE3EA),
                onSurfaceVariant = Color(0xFF41474D),
                surfaceContainerHighest = Color(0xFFE2E7EE),
                outline = Color(0xFF72787E),
                outlineVariant = Color(0xFFC1C7CE)
            )
            WeatherCondition.FEW_CLOUDS -> lightColorScheme(
                primary = Color(0xFF006684),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFBFE9FF),
                onPrimaryContainer = Color(0xFF001F2A),
                secondary = Color(0xFF4C616B),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFCEE5F1),
                onSecondaryContainer = Color(0xFF081E26),
                tertiary = Color(0xFF5E5A7D),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFE4DFFF),
                onTertiaryContainer = Color(0xFF1B1736),
                background = Color(0xFFF7FAFB),
                onBackground = Color(0xFF191C1E),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF191C1E),
                surfaceVariant = Color(0xFFDEE3E7),
                onSurfaceVariant = Color(0xFF41484C),
                surfaceContainerHighest = Color(0xFFE1E7EA),
                outline = Color(0xFF71787D),
                outlineVariant = Color(0xFFC1C8CC)
            )
            WeatherCondition.CLOUDY -> lightColorScheme(
                primary = Color(0xFF455A64),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFCFD8DC),
                onPrimaryContainer = Color(0xFF1A2328),
                secondary = Color(0xFF546E7A),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFDCE4E8),
                onSecondaryContainer = Color(0xFF111D23),
                tertiary = Color(0xFF5C6B73),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFE0E5E8),
                onTertiaryContainer = Color(0xFF172025),
                background = Color(0xFFF5F7F8),
                onBackground = Color(0xFF1A1C1E),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF1A1C1E),
                surfaceVariant = Color(0xFFDCE1E4),
                onSurfaceVariant = Color(0xFF43474A),
                surfaceContainerHighest = Color(0xFFDFE4E8),
                outline = Color(0xFF74787B),
                outlineVariant = Color(0xFFC3C7CA)
            )
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> lightColorScheme(
                primary = Color(0xFF006782),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFBAEAFF),
                onPrimaryContainer = Color(0xFF001F29),
                secondary = Color(0xFF4C626B),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFCEE6F0),
                onSecondaryContainer = Color(0xFF081E26),
                tertiary = Color(0xFF0288D1),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFB3E5FC),
                onTertiaryContainer = Color(0xFF001F31),
                background = Color(0xFFF5F8FA),
                onBackground = Color(0xFF181C1E),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF181C1E),
                surfaceVariant = Color(0xFFDAE2E7),
                onSurfaceVariant = Color(0xFF40484D),
                surfaceContainerHighest = Color(0xFFDEE5EA),
                outline = Color(0xFF70787D),
                outlineVariant = Color(0xFFBFC8CD)
            )
            WeatherCondition.THUNDERSTORM -> lightColorScheme(
                primary = Color(0xFF5E35B1),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE7DEFF),
                onPrimaryContainer = Color(0xFF1D0061),
                secondary = Color(0xFF625B71),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE8DEF8),
                onSecondaryContainer = Color(0xFF1E192B),
                tertiary = Color(0xFF9E6500),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFFFDF9E),
                onTertiaryContainer = Color(0xFF321D00),
                background = Color(0xFFFAF6FD),
                onBackground = Color(0xFF1D1B20),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF1D1B20),
                surfaceVariant = Color(0xFFE6E0EB),
                onSurfaceVariant = Color(0xFF48454E),
                surfaceContainerHighest = Color(0xFFE9E4EE),
                outline = Color(0xFF79757F),
                outlineVariant = Color(0xFFC9C4D0)
            )
            WeatherCondition.SNOW -> lightColorScheme(
                primary = Color(0xFF0277BD),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD0EDFF),
                onPrimaryContainer = Color(0xFF001F31),
                secondary = Color(0xFF4E606E),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFD2E4F4),
                onSecondaryContainer = Color(0xFF0B1D29),
                tertiary = Color(0xFF00838F),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFB2EBF2),
                onTertiaryContainer = Color(0xFF002024),
                background = Color(0xFFF7FAFD),
                onBackground = Color(0xFF191C1F),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF191C1F),
                surfaceVariant = Color(0xFFDFE5EB),
                onSurfaceVariant = Color(0xFF41474D),
                surfaceContainerHighest = Color(0xFFE2E7ED),
                outline = Color(0xFF71787D),
                outlineVariant = Color(0xFFC0C7CD)
            )
            WeatherCondition.FOG -> lightColorScheme(
                primary = Color(0xFF546E7A),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFCFD8DC),
                onPrimaryContainer = Color(0xFF102027),
                secondary = Color(0xFF5A6065),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFDEE3E8),
                onSecondaryContainer = Color(0xFF171D21),
                tertiary = Color(0xFF6D7973),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFE2EBE5),
                onTertiaryContainer = Color(0xFF18221D),
                background = Color(0xFFF7F8F9),
                onBackground = Color(0xFF1A1C1E),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF1A1C1E),
                surfaceVariant = Color(0xFFDFE2E4),
                onSurfaceVariant = Color(0xFF42474A),
                surfaceContainerHighest = Color(0xFFE2E5E7),
                outline = Color(0xFF73787B),
                outlineVariant = Color(0xFFC2C6C9)
            )
        }
    }
}


// ============================================================================
// 独自テーマカラー・プリセット (Fixed Theme Presets)
// ============================================================================

val SkyOceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF006492),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9E6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF50606E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E5F5),
    onSecondaryContainer = Color(0xFF0C1D29),
    tertiary = Color(0xFF006874),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9EEFFD),
    onTertiaryContainer = Color(0xFF001F24),
    background = Color(0xFFF7F9FD),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFFAF9FD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4FA),
    surfaceContainer = Color(0xFFEAF0F6),
    surfaceContainerHigh = Color(0xFFE4EAF0),
    surfaceContainerHighest = Color(0xFFDEE4EB),
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDEE3EA),
    onSurfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF72787E),
    outlineVariant = Color(0xFFC1C7CE)
)

val SkyOceanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8BCEFF),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004B70),
    onPrimaryContainer = Color(0xFFC9E6FF),
    secondary = Color(0xFFB7C9D9),
    onSecondary = Color(0xFF22323F),
    secondaryContainer = Color(0xFF384956),
    onSecondaryContainer = Color(0xFFD3E5F5),
    tertiary = Color(0xFF82D5E3),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF9EEFFD),
    background = Color(0xFF09131F),
    onBackground = Color(0xFFDEE3EA),
    surface = Color(0xFF0E1A29),
    surfaceContainerLowest = Color(0xFF09121E),
    surfaceContainerLow = Color(0xFF132030),
    surfaceContainer = Color(0xFF182638),
    surfaceContainerHigh = Color(0xFF1E2E42),
    surfaceContainerHighest = Color(0xFF26384E),
    onSurface = Color(0xFFDEE3EA),
    surfaceVariant = Color(0xFF1B2B3D),
    onSurfaceVariant = Color(0xFFC0C7D0),
    outline = Color(0xFF8A929B),
    outlineVariant = Color(0xFF404850)
)

val EmeraldForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF89F8C6),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6357),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE9D8),
    onSecondaryContainer = Color(0xFF0B1F16),
    tertiary = Color(0xFF3D6473),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC0E9FA),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF6FBF6),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF8FCF8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F6F0),
    surfaceContainer = Color(0xFFEAF0EA),
    surfaceContainerHigh = Color(0xFFE4EAE4),
    surfaceContainerHighest = Color(0xFFDEE5DE),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DE),
    onSurfaceVariant = Color(0xFF3F4943),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFBFC9C2)
)

val EmeraldForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6CDBAB),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF005138),
    onPrimaryContainer = Color(0xFF89F8C6),
    secondary = Color(0xFFB4CCBC),
    onSecondary = Color(0xFF20352A),
    secondaryContainer = Color(0xFF364B40),
    onSecondaryContainer = Color(0xFFCFE9D8),
    tertiary = Color(0xFFA5CDDE),
    onTertiary = Color(0xFF063543),
    tertiaryContainer = Color(0xFF244C5A),
    onTertiaryContainer = Color(0xFFC0E9FA),
    background = Color(0xFF0A120E),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF0F1713),
    surfaceContainerLowest = Color(0xFF0A0F0D),
    surfaceContainerLow = Color(0xFF151E19),
    surfaceContainer = Color(0xFF1B241F),
    surfaceContainerHigh = Color(0xFF222C26),
    surfaceContainerHighest = Color(0xFF2A362F),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF1C2721),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF3F4943)
)

val SunsetAmberLightColorScheme = lightColorScheme(
    primary = Color(0xFF8E4F00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF2E1500),
    secondary = Color(0xFF725A42),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEDEBF),
    onSecondaryContainer = Color(0xFF281806),
    tertiary = Color(0xFF5B6334),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E9AD),
    onTertiaryContainer = Color(0xFF181E00),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF211A14),
    surface = Color(0xFFFFF9F6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF1EA),
    surfaceContainer = Color(0xFFF4EBE3),
    surfaceContainerHigh = Color(0xFFEEE5DE),
    surfaceContainerHighest = Color(0xFFE8DFD8),
    onSurface = Color(0xFF211A14),
    surfaceVariant = Color(0xFFF2DFD1),
    onSurfaceVariant = Color(0xFF51453A),
    outline = Color(0xFF837469),
    outlineVariant = Color(0xFFD5C3B5)
)

val SunsetAmberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB87A),
    onPrimary = Color(0xFF4C2700),
    primaryContainer = Color(0xFF6C3A00),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFE1C1A4),
    onSecondary = Color(0xFF402C18),
    secondaryContainer = Color(0xFF58432C),
    onSecondaryContainer = Color(0xFFFEDEBF),
    tertiary = Color(0xFFC4CD93),
    onTertiary = Color(0xFF2E340A),
    tertiaryContainer = Color(0xFF444B1E),
    onTertiaryContainer = Color(0xFFE0E9AD),
    background = Color(0xFF130E09),
    onBackground = Color(0xFFEDE0D6),
    surface = Color(0xFF1A130D),
    surfaceContainerLowest = Color(0xFF120D08),
    surfaceContainerLow = Color(0xFF211912),
    surfaceContainer = Color(0xFF2A2018),
    surfaceContainerHigh = Color(0xFF33271E),
    surfaceContainerHighest = Color(0xFF3D2F24),
    onSurface = Color(0xFFEDE0D6),
    surfaceVariant = Color(0xFF2C2117),
    onSurfaceVariant = Color(0xFFD5C3B5),
    outline = Color(0xFF9E8E80),
    outlineVariant = Color(0xFF51453A)
)

val LavenderTwilightLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1E192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1A22),
    surface = Color(0xFFFFF8FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F1FA),
    surfaceContainer = Color(0xFFF2EBF4),
    surfaceContainerHigh = Color(0xFFECE5EE),
    surfaceContainerHighest = Color(0xFFE6DFE8),
    onSurface = Color(0xFF1D1A22),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCBC4CF)
)

val LavenderTwilightDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF0F0D14),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF15131C),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1E1B24),
    surfaceContainer = Color(0xFF25212D),
    surfaceContainerHigh = Color(0xFF2E2937),
    surfaceContainerHighest = Color(0xFF383243),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF25202D),
    onSurfaceVariant = Color(0xFFCBC4CF),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454E)
)

val SakuraBlossomLightColorScheme = lightColorScheme(
    primary = Color(0xFF9C4146),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = Color(0xFF40000A),
    secondary = Color(0xFF775656),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD9),
    onSecondaryContainer = Color(0xFF2C1516),
    tertiary = Color(0xFF755A2F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDAE),
    onTertiaryContainer = Color(0xFF281800),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF221919),
    surface = Color(0xFFFFF9F8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF1F0),
    surfaceContainer = Color(0xFFF4EBEA),
    surfaceContainerHigh = Color(0xFFEEE5E4),
    surfaceContainerHighest = Color(0xFFE8DFDE),
    onSurface = Color(0xFF221919),
    surfaceVariant = Color(0xFFF4DDDD),
    onSurfaceVariant = Color(0xFF524343),
    outline = Color(0xFF857373),
    outlineVariant = Color(0xFFD7C2C1)
)

val SakuraBlossomDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB3B4),
    onPrimary = Color(0xFF5F131C),
    primaryContainer = Color(0xFF7E2A30),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFE6BDBC),
    onSecondary = Color(0xFF44292A),
    secondaryContainer = Color(0xFF5D3F40),
    onSecondaryContainer = Color(0xFFFFDAD9),
    tertiary = Color(0xFFE5C18D),
    onTertiary = Color(0xFF422C05),
    tertiaryContainer = Color(0xFF5C431A),
    onTertiaryContainer = Color(0xFFFFDDAE),
    background = Color(0xFF130C0D),
    onBackground = Color(0xFFECE0DF),
    surface = Color(0xFF191112),
    surfaceContainerLowest = Color(0xFF130C0D),
    surfaceContainerLow = Color(0xFF221718),
    surfaceContainer = Color(0xFF2B1F20),
    surfaceContainerHigh = Color(0xFF352627),
    surfaceContainerHighest = Color(0xFF402E30),
    onSurface = Color(0xFFECE0DF),
    surfaceVariant = Color(0xFF2B1D1E),
    onSurfaceVariant = Color(0xFFD7C2C1),
    outline = Color(0xFFA08C8C),
    outlineVariant = Color(0xFF524343)
)

val MidnightSlateLightColorScheme = lightColorScheme(
    primary = Color(0xFF4F616E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E5F5),
    onPrimaryContainer = Color(0xFF0B1D29),
    secondary = Color(0xFF585F66),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE3EA),
    onSecondaryContainer = Color(0xFF151C22),
    tertiary = Color(0xFF655B6F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECDEF7),
    onTertiaryContainer = Color(0xFF20182A),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFF9FAFB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4F6),
    surfaceContainer = Color(0xFFECEEF0),
    surfaceContainerHigh = Color(0xFFE6E8EB),
    surfaceContainerHighest = Color(0xFFE0E2E5),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDFE2E6),
    onSurfaceVariant = Color(0xFF44474B),
    outline = Color(0xFF74777B),
    outlineVariant = Color(0xFFC4C7CB)
)

val MidnightSlateDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB7C9D9),
    onPrimary = Color(0xFF21323E),
    primaryContainer = Color(0xFF374955),
    onPrimaryContainer = Color(0xFFD2E5F5),
    secondary = Color(0xFFC0C7CE),
    onSecondary = Color(0xFF2A3137),
    secondaryContainer = Color(0xFF41484E),
    onSecondaryContainer = Color(0xFFDCE3EA),
    tertiary = Color(0xFFCFBEE0),
    onTertiary = Color(0xFF352D3F),
    tertiaryContainer = Color(0xFF4D4357),
    onTertiaryContainer = Color(0xFFECDEF7),
    background = Color(0xFF0B0F12),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF12161A),
    surfaceContainerLowest = Color(0xFF0B0F12),
    surfaceContainerLow = Color(0xFF181D21),
    surfaceContainer = Color(0xFF1F2429),
    surfaceContainerHigh = Color(0xFF272D33),
    surfaceContainerHighest = Color(0xFF31383F),
    onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF20262B),
    onSurfaceVariant = Color(0xFFC4C7CB),
    outline = Color(0xFF8E9195),
    outlineVariant = Color(0xFF44474B)
)

/**
 * 指定された ThemePreset と昼夜（isDark）に基づいて、固定カラーの ColorScheme を返します。
 */
fun createPresetColorScheme(preset: ThemePreset, isDark: Boolean): ColorScheme {
    return if (isDark) {
        when (preset) {
            ThemePreset.SKY_OCEAN -> SkyOceanDarkColorScheme
            ThemePreset.EMERALD_FOREST -> EmeraldForestDarkColorScheme
            ThemePreset.SUNSET_AMBER -> SunsetAmberDarkColorScheme
            ThemePreset.LAVENDER_TWILIGHT -> LavenderTwilightDarkColorScheme
            ThemePreset.SAKURA_BLOSSOM -> SakuraBlossomDarkColorScheme
            ThemePreset.MIDNIGHT_SLATE -> MidnightSlateDarkColorScheme
            ThemePreset.WEATHER, ThemePreset.SYSTEM -> ExpressiveDarkColorScheme
        }
    } else {
        when (preset) {
            ThemePreset.SKY_OCEAN -> SkyOceanLightColorScheme
            ThemePreset.EMERALD_FOREST -> EmeraldForestLightColorScheme
            ThemePreset.SUNSET_AMBER -> SunsetAmberLightColorScheme
            ThemePreset.LAVENDER_TWILIGHT -> LavenderTwilightLightColorScheme
            ThemePreset.SAKURA_BLOSSOM -> SakuraBlossomLightColorScheme
            ThemePreset.MIDNIGHT_SLATE -> MidnightSlateLightColorScheme
            ThemePreset.WEATHER, ThemePreset.SYSTEM -> ExpressiveLightColorScheme
        }
    }
}


