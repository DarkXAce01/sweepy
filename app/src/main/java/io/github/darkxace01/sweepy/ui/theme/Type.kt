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
//  - Archivo: the Pop theme's extra-wide, extra-heavy headlines

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

private fun archivo(weight: Int, width: Float) = Font(
    R.font.archivo,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
)

/** Archivo stretched as wide as it goes, for big Pop headlines. */
val Wide = FontFamily(archivo(800, 125f))

/** A little less wide, so smaller headlines like screen titles still fit. */
val WideSmall = FontFamily(archivo(750, 112f))

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

/** Pop: extra-wide, extra-heavy headlines, like a sticker sheet's header. */
private fun popTypography(): Typography {
    fun headline(family: FontFamily, size: Double, line: Double, tracking: Double) = style(family, size, line, FontWeight.ExtraBold, tracking)
    return ContactSheetTypography.copy(
        displayLarge = headline(Wide, 52.0, 56.0, -0.035),
        displayMedium = headline(Wide, 40.0, 44.0, -0.03),
        displaySmall = headline(Wide, 31.0, 36.0, -0.025),
        headlineLarge = headline(Wide, 27.0, 32.0, -0.02),
        headlineMedium = headline(Wide, 23.0, 28.0, -0.015),
        headlineSmall = headline(WideSmall, 20.0, 26.0, -0.01),
        titleMedium = style(Sans, 16.0, 22.0, FontWeight.SemiBold, -0.005),
        labelLarge = style(Sans, 15.0, 20.0, FontWeight.SemiBold, -0.005),
    )
}

fun typographyFor(theme: ThemeChoice, look: SweepyLook): Typography = when (theme) {
    ThemeChoice.CONTACT_SHEET -> ContactSheetTypography
    ThemeChoice.RISO -> risoTypography(look)
    ThemeChoice.POP -> popTypography()
}
