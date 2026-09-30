package com.example.expressiveweather.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.R

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 500f),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 600f),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 700f),
            FontVariation.Setting("ROND", 100f)
        )
    )
)

/**
 * Dedicated Google Sans Flex instance for the Hero Temperature display:
 * Weight: 563, Round: 100
 */
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexHeroTemp = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 563f),
            FontVariation.Setting("ROND", 100f)
        )
    )
)

/**
 * Dedicated Google Sans Flex instance for the Setup / Welcome title display:
 * Weight: 750, Round: 100 (Max Roundness for Expressive Welcome)
 */
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexWelcome = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 750f),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 500f),
            FontVariation.Setting("ROND", 100f)
        )
    )
)

fun createLocalizedTypography(localeList: androidx.compose.ui.text.intl.LocaleList): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
            localeList = localeList
        ),
        displayMedium = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
            localeList = localeList
        ),
        displaySmall = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
            localeList = localeList
        ),
        headlineLarge = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
            localeList = localeList
        ),
        headlineMedium = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
            localeList = localeList
        ),
        headlineSmall = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
            localeList = localeList
        ),
        titleLarge = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
            localeList = localeList
        ),
        titleMedium = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
            localeList = localeList
        ),
        titleSmall = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            localeList = localeList
        ),
        bodyLarge = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
            localeList = localeList
        ),
        bodyMedium = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
            localeList = localeList
        ),
        bodySmall = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
            localeList = localeList
        ),
        labelLarge = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            localeList = localeList
        ),
        labelMedium = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            localeList = localeList
        ),
        labelSmall = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            localeList = localeList
        )
    )
}

val defaultJaLocaleList = androidx.compose.ui.text.intl.LocaleList(
    androidx.compose.ui.text.intl.Locale("ja")
)

val Typography: Typography = createLocalizedTypography(defaultJaLocaleList)

