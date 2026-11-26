package io.github.nemynew.expressiveweather.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.unit.dp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.WeatherCondition
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics
import io.github.nemynew.expressiveweather.ui.haptics.rememberExpressiveHaptics


val ExpressiveLightColorScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF5A5C7C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE2F9),
    onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF725572),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EE),
    onTertiaryContainer = Color(0xFF2E1125),
    surface = Color(0xFFFAF9FD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainer = Color(0xFFEEEDF3),
    surfaceContainerHigh = Color(0xFFE9E8EF),
    surfaceContainerHighest = Color(0xFFE3E2E6),
    onSurface = Color(0xFF1B1B1F),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    inverseSurface = Color(0xFF303034),
    inverseOnSurface = Color(0xFFF2F0F4),
    inversePrimary = Color(0xFFA8C7FA),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

val ExpressiveDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC0C2FD),
    onPrimary = Color(0xFF042C71),
    primaryContainer = Color(0xFF04409F),
    onPrimaryContainer = Color(0xFFE0E0FC),
    secondary = Color(0xFFC3C3E9),
    onSecondary = Color(0xFF2D2E4E),
    secondaryContainer = Color(0xFF424463),
    onSecondaryContainer = Color(0xFFE0E0FE),
    tertiary = Color(0xFFDFBBDE),
    onTertiary = Color(0xFF402842),
    tertiaryContainer = Color(0xFF6E334E),
    onTertiaryContainer = Color(0xFFFFD8E7),
    surface = Color(0xFF131317),
    surfaceContainerLowest = Color(0xFF0E0E12),
    surfaceContainerLow = Color(0xFF1B1B1F),
    surfaceContainer = Color(0xFF1F1F23),
    surfaceContainerHigh = Color(0xFF2A2A2E),
    surfaceContainerHighest = Color(0xFF343439),
    onSurface = Color(0xFFE2E2E8),
    onSurfaceVariant = Color(0xFFC6C5D2),
    outline = Color(0xFF90909C),
    outlineVariant = Color(0xFF464651),
    inverseSurface = Color(0xFFE2E2E8),
    inverseOnSurface = Color(0xFF303034),
    inversePrimary = Color(0xFF3757BA),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp), // M3 Expressive card is 20dp
    large = RoundedCornerShape(28.dp),  // M3 Expressive dialog is 28dp
    extraLarge = RoundedCornerShape(36.dp)
)

val LocalDarkTheme = compositionLocalOf { false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    weatherCondition: WeatherCondition? = null,
    weatherAdaptiveColor: Boolean = true,
    themePreset: ThemePreset = if (weatherAdaptiveColor) ThemePreset.WEATHER else ThemePreset.SYSTEM,
    accentColorMode: AccentColorMode = LocalAccentColorMode.current,
    language: AppLanguage = LocalAppLanguage.current,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themePreset) {
        ThemePreset.WEATHER -> {
            if (weatherAdaptiveColor && weatherCondition != null) {
                createWeatherColorScheme(weatherCondition, darkTheme)
            } else if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) {
                ExpressiveDarkColorScheme
            } else {
                ExpressiveLightColorScheme
            }
        }
        ThemePreset.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) {
                ExpressiveDarkColorScheme
            } else {
                ExpressiveLightColorScheme
            }
        }
        else -> {
            createPresetColorScheme(themePreset, darkTheme)
        }
    }

    val composeLocaleList = remember(language) {
        language.composeLocaleList
    }
    val localizedTypography = remember(composeLocaleList) {
        createLocalizedTypography(composeLocaleList)
    }

    val configuration = LocalConfiguration.current
    val localizedConfiguration = remember(configuration, language) {
        android.content.res.Configuration(configuration).apply {
            setLocale(language.locale)
            setLayoutDirection(language.locale)
        }
    }

    val expressHaptics = rememberExpressiveHaptics()
    val accentPalette = remember(colorScheme, darkTheme, accentColorMode) {
        MaterialYouHarmonizer.buildPalette(
            colorScheme = colorScheme,
            isDark = darkTheme,
            mode = accentColorMode
        )
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAccentColorMode provides accentColorMode,
        LocalMaterialYouPalette provides accentPalette,
        LocalAppLanguage provides language,
        LocalConfiguration provides localizedConfiguration,
        LocalExpressiveHaptics provides expressHaptics,
        LocalTextStyle provides TextStyle(
            fontFamily = GoogleSansFlex,
            localeList = composeLocaleList
        )
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = ExpressiveShapes,
            typography = localizedTypography
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides localizedTypography.bodyMedium
            ) {
                content()
            }
        }
    }
}
