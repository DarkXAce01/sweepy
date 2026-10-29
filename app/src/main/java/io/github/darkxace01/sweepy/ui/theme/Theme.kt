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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.data.ThemeChoice

/**
 * The colors of one theme. Every theme keeps the same roles, so screens never need to know
 * which one is active: photos supply the color, the interface stays quiet.
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
    /** Text on a [pencil] button. */
    val onPencil: Color,
    val keep: Color,
    val later: Color,
    val favorite: Color,
    /** The film strip on the home screen: its base, sprocket holes and edge numbers. */
    val film: Color,
    val filmHoles: Color,
    val filmText: Color,
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
    onPencil = Color.White,
    keep = Color(0xFF4F7F5A),
    later = Color(0xFFB98322),
    favorite = Color(0xFFBF4F72),
    film = Color(0xFF1B1916),
    filmHoles = Color(0xD9F2EFE8),
    filmText = Color(0xFFE39B4B),
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
    onPencil = Color.White,
    keep = Color(0xFF7DB287),
    later = Color(0xFFD9A441),
    favorite = Color(0xFFDB7193),
    film = Color(0xFF1B1916),
    filmHoles = Color(0xD9F2EFE8),
    filmText = Color(0xFFE39B4B),
    isDark = true,
)

// Riso: a zine printed on a risograph. Cream newsprint, deep blue ink, fluorescent pink.
private val RisoCream = SweepyColors(
    paper = Color(0xFFF3EDDD),
    surface = Color(0xFFF9F4E7),
    print = Color(0xFFFFFCF4),
    ink = Color(0xFF1F2C74),
    inkMuted = Color(0xFF666A93),
    hairline = Color(0xFFE0D6BF),
    pencil = Color(0xFFF2369E),
    onPencil = Color(0xFF1F2C74),
    keep = Color(0xFF00955A),
    later = Color(0xFFE08A00),
    favorite = Color(0xFF7457B5),
    film = Color(0xFF1F2C74),
    filmHoles = Color(0xFFF3EDDD),
    filmText = Color(0xFFFF7BC4),
    isDark = false,
)

private val RisoMidnight = SweepyColors(
    paper = Color(0xFF161A38),
    surface = Color(0xFF1E2247),
    print = Color(0xFF262B55),
    ink = Color(0xFFF6EEDB),
    inkMuted = Color(0xFFA3A6C9),
    hairline = Color(0xFF323866),
    pencil = Color(0xFFFF59B6),
    onPencil = Color(0xFF161A38),
    keep = Color(0xFF39D38B),
    later = Color(0xFFFFD047),
    favorite = Color(0xFFB59BFF),
    film = Color(0xFF0D1028),
    filmHoles = Color(0xFFF6EEDB),
    filmText = Color(0xFFFFD047),
    isDark = true,
)

// Herbarium: a botanist's journal of pressed plants. Linen paper, forest ink, terracotta and moss.
private val HerbariumLinen = SweepyColors(
    paper = Color(0xFFEDEBDF),
    surface = Color(0xFFF4F2E8),
    print = Color(0xFFFBFAF4),
    ink = Color(0xFF1E3326),
    inkMuted = Color(0xFF6B7A6C),
    hairline = Color(0xFFD9D6C5),
    pencil = Color(0xFFB9512E),
    onPencil = Color.White,
    keep = Color(0xFF4F7A3A),
    later = Color(0xFFC08A2D),
    favorite = Color(0xFF9B4D6E),
    film = Color(0xFF1E3326),
    filmHoles = Color(0xFFEDEBDF),
    filmText = Color(0xFFE0A24A),
    isDark = false,
)

private val HerbariumFern = SweepyColors(
    paper = Color(0xFF101712),
    surface = Color(0xFF162019),
    print = Color(0xFF1C2820),
    ink = Color(0xFFE8E6D6),
    inkMuted = Color(0xFF93A08F),
    hairline = Color(0xFF26332A),
    pencil = Color(0xFFE0794F),
    onPencil = Color(0xFF101712),
    keep = Color(0xFF8DBF6E),
    later = Color(0xFFE2B04F),
    favorite = Color(0xFFD88BAA),
    film = Color(0xFF080D0A),
    filmHoles = Color(0xFFE8E6D6),
    filmText = Color(0xFFE2B04F),
    isDark = true,
)

/** What the page is printed on. */
enum class Texture { GRAIN, HALFTONE, LAID }

/** How grease-pencil marks are drawn. */
enum class MarkStyle {
    /** A waxy china marker. */
    PENCIL,

    /** Two inks printed slightly out of register. */
    RISO,

    /** A fine pen line over a light watercolor wash, like a botanical drawing. */
    INK,
}

/** Everything a theme decides. */
@Immutable
data class SweepyLook(
    val colors: SweepyColors,
    val texture: Texture,
    val markStyle: MarkStyle,
    /** Riso's second ink, printed under marks and headlines a little out of register. */
    val secondInk: Color = Color.Transparent,
)

val LocalSweepyLook = staticCompositionLocalOf { SweepyLook(Paper, Texture.GRAIN, MarkStyle.PENCIL) }

/** Shorthand: `Sweepy.colors.ink`. */
object Sweepy {
    val colors: SweepyColors
        @Composable @ReadOnlyComposable get() = LocalSweepyLook.current.colors

    val look: SweepyLook
        @Composable @ReadOnlyComposable get() = LocalSweepyLook.current
}

/** The look for [theme], light or dark. */
fun lookFor(theme: ThemeChoice, dark: Boolean): SweepyLook = when (theme) {
    ThemeChoice.CONTACT_SHEET -> SweepyLook(if (dark) Darkroom else Paper, Texture.GRAIN, MarkStyle.PENCIL)
    ThemeChoice.RISO -> {
        val colors = if (dark) RisoMidnight else RisoCream
        SweepyLook(colors, Texture.HALFTONE, MarkStyle.RISO, secondInk = if (dark) Color(0xFF3D7BFF) else Color(0xFF3F8FE0))
    }
    ThemeChoice.HERBARIUM -> SweepyLook(if (dark) HerbariumFern else HerbariumLinen, Texture.LAID, MarkStyle.INK)
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

/** Follows the system light/dark setting unless told otherwise. */
@Composable
fun SweepyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    theme: ThemeChoice = ThemeChoice.CONTACT_SHEET,
    content: @Composable () -> Unit,
) {
    val look = lookFor(theme, darkTheme)
    val colors = look.colors
    CompositionLocalProvider(LocalSweepyLook provides look) {
        MaterialTheme(
            colorScheme = materialColors(colors),
            typography = remember(look) { typographyFor(theme, look) },
            shapes = SweepyShapes,
            content = content,
        )
    }
}
