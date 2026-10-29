package io.github.darkxace01.sweepy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.darkxace01.sweepy.R

// Bundled fonts (all SIL Open Font License, see /third_party):
//  - Instrument Serif: editorial headlines and big numbers
//  - Geist: interface text
//  - Geist Mono: film-edge metadata (sizes, dates, counts, small labels)

val Serif = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

private fun variable(res: Int, weight: FontWeight) = Font(
    res,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Sans = FontFamily(
    variable(R.font.geist, FontWeight.Normal),
    variable(R.font.geist, FontWeight.Medium),
    variable(R.font.geist, FontWeight.SemiBold),
)

val Mono = FontFamily(
    variable(R.font.geist_mono, FontWeight.Normal),
    variable(R.font.geist_mono, FontWeight.Medium),
)

private fun style(family: FontFamily, size: Double, line: Double, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) =
    TextStyle(fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em)

/**
 * Display and headline slots are the serif; titles and body are Geist; labels are Geist Mono
 * (used uppercase, as small "eyebrow" captions).
 */
val SweepyTypography = Typography(
    displayLarge = style(Serif, 72.0, 70.0, tracking = -0.025),
    displayMedium = style(Serif, 54.0, 56.0, tracking = -0.02),
    displaySmall = style(Serif, 42.0, 46.0, tracking = -0.015),
    headlineLarge = style(Serif, 36.0, 40.0, tracking = -0.01),
    headlineMedium = style(Serif, 30.0, 34.0, tracking = -0.01),
    headlineSmall = style(Serif, 25.0, 30.0, tracking = -0.005),
    titleLarge = style(Sans, 19.0, 24.0, FontWeight.SemiBold, -0.01),
    titleMedium = style(Sans, 16.0, 22.0, FontWeight.Medium, -0.005),
    titleSmall = style(Sans, 14.0, 20.0, FontWeight.Medium),
    bodyLarge = style(Sans, 16.0, 24.0),
    bodyMedium = style(Sans, 14.0, 20.0),
    bodySmall = style(Sans, 12.5, 17.0),
    labelLarge = style(Sans, 15.0, 20.0, FontWeight.Medium, -0.005),
    labelMedium = style(Mono, 11.5, 15.0, FontWeight.Medium, 0.06),
    labelSmall = style(Mono, 10.0, 13.0, FontWeight.Medium, 0.08),
)
