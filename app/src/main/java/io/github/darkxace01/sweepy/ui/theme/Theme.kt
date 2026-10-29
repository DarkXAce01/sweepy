package io.github.darkxace01.sweepy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Sweepy's look is a photographer's contact sheet: warm paper, ink, and a red grease pencil.
 * Photos supply the color; the interface stays quiet.
 */
@Immutable
data class SweepyColors(
    /** Page background. */
    val paper: Color,
    /** Slightly raised surfaces: tiles, sheets. */
    val surface: Color,
    /** The photo "print" cards. */
    val print: Color,
    /** Main text and primary buttons. */
    val ink: Color,
    /** Secondary text, captions. */
    val inkMuted: Color,
    /** Thin dividers and outlines. */
    val hairline: Color,
    /** Grease-pencil red: delete marks and destructive actions. */
    val pencil: Color,
    val keep: Color,
    val later: Color,
    val favorite: Color,
    val isDark: Boolean,
)

private val Paper = SweepyColors(
    paper = Color(0xFFF2EFE8),
    surface = Color(0xFFF8F6F1),
    print = Color(0xFFFDFCF9),
    ink = Color(0xFF1C1A17),
    inkMuted = Color(0xFF746E63),
    hairline = Color(0xFFDDD7CB),
    pencil = Color(0xFFD4432A),
    keep = Color(0xFF4F7F5A),
    later = Color(0xFFB98322),
    favorite = Color(0xFFBF4F72),
    isDark = false,
)

private val Darkroom = SweepyColors(
    paper = Color(0xFF12110F),
    surface = Color(0xFF1A1916),
    print = Color(0xFF201E1B),
    ink = Color(0xFFEDE8DE),
    inkMuted = Color(0xFF9A9488),
    hairline = Color(0xFF2E2B27),
    pencil = Color(0xFFE5553A),
    keep = Color(0xFF7DB287),
    later = Color(0xFFD9A441),
    favorite = Color(0xFFDB7193),
    isDark = true,
)

val LocalSweepyColors = staticCompositionLocalOf { Paper }

/** Shorthand: `Sweepy.colors.ink`. */
object Sweepy {
    val colors: SweepyColors
        @Composable @ReadOnlyComposable get() = LocalSweepyColors.current
}

private fun materialColors(c: SweepyColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.ink, onPrimary = c.paper,
        primaryContainer = c.surface, onPrimaryContainer = c.ink,
        secondary = c.inkMuted, onSecondary = c.paper,
        background = c.paper, onBackground = c.ink,
        surface = c.paper, onSurface = c.ink,
        surfaceVariant = c.surface, onSurfaceVariant = c.inkMuted,
        surfaceContainerLowest = c.paper, surfaceContainerLow = c.surface,
        surfaceContainer = c.surface, surfaceContainerHigh = c.print, surfaceContainerHighest = c.hairline,
        inverseSurface = c.ink, inverseOnSurface = c.paper,
        outline = c.hairline, outlineVariant = c.hairline,
        error = c.pencil, onError = Color.White,
    )
} else {
    lightColorScheme(
        primary = c.ink, onPrimary = c.paper,
        primaryContainer = c.surface, onPrimaryContainer = c.ink,
        secondary = c.inkMuted, onSecondary = c.paper,
        background = c.paper, onBackground = c.ink,
        surface = c.paper, onSurface = c.ink,
        surfaceVariant = c.surface, onSurfaceVariant = c.inkMuted,
        surfaceContainerLowest = c.print, surfaceContainerLow = c.surface,
        surfaceContainer = c.surface, surfaceContainerHigh = c.print, surfaceContainerHighest = c.hairline,
        inverseSurface = c.ink, inverseOnSurface = c.paper,
        outline = c.hairline, outlineVariant = c.hairline,
        error = c.pencil, onError = Color.White,
    )
}

private val SweepyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Follows the system light/dark setting. */
@Composable
fun SweepyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) Darkroom else Paper
    CompositionLocalProvider(LocalSweepyColors provides colors) {
        MaterialTheme(
            colorScheme = materialColors(colors),
            typography = SweepyTypography,
            shapes = SweepyShapes,
            content = content,
        )
    }
}
