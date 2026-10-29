package io.github.darkxace01.sweepy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.ThemeChoice

// Bundled fonts (all SIL Open Font License, see /third_party):
//  - Instrument Serif: editorial headlines and big numbers
//  - Geist: interface text
//  - Geist Mono: film-edge metadata (sizes, dates, counts, small labels)
//  - Bricolage Grotesque: the Riso theme's heavy, condensed headlines

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

/** Heavy and narrow, set at display optical size: a zine masthead. */
val Zine = FontFamily(
    Font(
        R.font.bricolage_grotesque,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(78f),
            FontVariation.Setting("opsz", 72f),
        ),
    ),
)

private fun style(family: FontFamily, size: Double, line: Double, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) =
    TextStyle(fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em)

/**
 * Display and headline slots are the serif; titles and body are Geist; labels are Geist Mono
 * (used uppercase, as small "eyebrow" captions).
 */
val ContactSheetTypography = Typography(
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

/** Riso: zine headlines with a fluorescent ink printed a hair out of register underneath. */
private fun risoTypography(look: SweepyLook): Typography {
    val offset = Shadow(color = look.colors.pencil.copy(alpha = 0.55f), offset = Offset(3.5f, 3f), blurRadius = 0f)
    fun headline(size: Double, line: Double, tracking: Double) = style(Zine, size, line, FontWeight.ExtraBold, tracking).copy(shadow = offset)
    return ContactSheetTypography.copy(
        displayLarge = headline(66.0, 66.0, -0.03),
        displayMedium = headline(50.0, 52.0, -0.025),
        displaySmall = headline(38.0, 42.0, -0.02),
        headlineLarge = headline(32.0, 36.0, -0.015),
        headlineMedium = headline(27.0, 31.0, -0.01),
        headlineSmall = style(Zine, 23.0, 28.0, FontWeight.ExtraBold, -0.005),
    )
}

/** Phosphor: monospace everywhere, with big type glowing faintly like a CRT. */
private fun phosphorTypography(look: SweepyLook): Typography {
    val glow = Shadow(color = look.colors.ink.copy(alpha = 0.6f), offset = Offset.Zero, blurRadius = 16f)
    fun glowing(size: Double, line: Double, tracking: Double) = style(Mono, size, line, FontWeight.Medium, tracking).copy(shadow = glow)
    return Typography(
        displayLarge = glowing(54.0, 60.0, -0.04),
        displayMedium = glowing(40.0, 46.0, -0.035),
        displaySmall = glowing(30.0, 36.0, -0.03),
        headlineLarge = glowing(27.0, 32.0, -0.025),
        headlineMedium = glowing(23.0, 28.0, -0.02),
        headlineSmall = style(Mono, 20.0, 26.0, FontWeight.Medium, -0.02),
        titleLarge = style(Mono, 17.0, 23.0, FontWeight.Medium, -0.02),
        titleMedium = style(Mono, 14.5, 21.0, FontWeight.Medium, -0.015),
        titleSmall = style(Mono, 13.0, 19.0, FontWeight.Medium, -0.01),
        bodyLarge = style(Mono, 14.5, 22.0, tracking = -0.015),
        bodyMedium = style(Mono, 13.0, 19.0, tracking = -0.01),
        bodySmall = style(Mono, 11.5, 16.0, tracking = -0.005),
        labelLarge = style(Mono, 14.0, 20.0, FontWeight.Medium, -0.01),
        labelMedium = style(Mono, 11.5, 15.0, FontWeight.Medium, 0.06),
        labelSmall = style(Mono, 10.0, 13.0, FontWeight.Medium, 0.08),
    )
}

fun typographyFor(theme: ThemeChoice, look: SweepyLook): Typography = when (theme) {
    ThemeChoice.CONTACT_SHEET -> ContactSheetTypography
    ThemeChoice.RISO -> risoTypography(look)
    ThemeChoice.PHOSPHOR -> phosphorTypography(look)
}
