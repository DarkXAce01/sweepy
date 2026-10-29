package io.github.darkxace01.sweepy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.darkxace01.sweepy.R

// Plus Jakarta Sans (SIL Open Font License) is bundled in the app, so no download is needed.
// It's a variable font: one file covers every weight.
private fun jakarta(weight: FontWeight) = Font(
    R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Jakarta = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Jakarta,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

val SweepyTypography = Typography(
    displayLarge = style(56, 60, FontWeight.ExtraBold, -0.03),
    displayMedium = style(44, 48, FontWeight.ExtraBold, -0.03),
    displaySmall = style(36, 40, FontWeight.ExtraBold, -0.025),
    headlineLarge = style(30, 36, FontWeight.ExtraBold, -0.02),
    headlineMedium = style(26, 32, FontWeight.ExtraBold, -0.02),
    headlineSmall = style(22, 28, FontWeight.Bold, -0.015),
    titleLarge = style(20, 26, FontWeight.Bold, -0.01),
    titleMedium = style(16, 22, FontWeight.Bold, -0.005),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Medium),
    labelLarge = style(15, 20, FontWeight.Bold),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.01),
    labelSmall = style(11, 14, FontWeight.Bold, 0.04),
)
