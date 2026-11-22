package com.example.axs_player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.axs_player.R

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val DMSansFont = GoogleFont("DM Sans")
private val OldStandardTTFont = GoogleFont("Old Standard TT")
private val DMSansWeights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

val DMSans = FontFamily(
    DMSansWeights.flatMap { weight ->
        listOf(FontStyle.Normal, FontStyle.Italic).map { style ->
            Font(googleFont = DMSansFont, fontProvider = GoogleFontsProvider, weight = weight, style = style)
        }
    }
)

val OldStandardTT = FontFamily(
    Font(googleFont = OldStandardTTFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(
        googleFont = OldStandardTTFont,
        fontProvider = GoogleFontsProvider,
        weight = FontWeight.Normal,
        style = FontStyle.Italic
    ),
    Font(googleFont = OldStandardTTFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold)
)

private val BaseTypography = Typography(
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

val Typography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = DMSans),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = DMSans),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = DMSans),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = DMSans),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = DMSans),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = DMSans),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = DMSans),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = DMSans),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = DMSans),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = DMSans),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = DMSans),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = DMSans),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = DMSans),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = DMSans),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = DMSans)
)
