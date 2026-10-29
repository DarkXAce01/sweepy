package io.github.darkxace01.sweepy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Swipe action colors. Same in light and dark so the meaning never changes.
val DeleteRed = Color(0xFFFF4D5E)
val KeepGreen = Color(0xFF18C088)
val LaterAmber = Color(0xFFFFAE1A)
val FavoritePink = Color(0xFFFF5FA2)

// Deck accent colors for the home screen tiles.
val ScreenshotBlue = Color(0xFF3B82F6)
val ChatGreen = Color(0xFF22C55E)
val VideoOrange = Color(0xFFFF7A1A)

// Brand
val Violet = Color(0xFF6C4BFF)
val VioletDeep = Color(0xFF4329D6)
val VioletNight = Color(0xFF241673)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B3DF5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E2FF),
    onPrimaryContainer = Color(0xFF1F0E6E),
    secondary = Color(0xFF5E5A73),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E3F0),
    onSecondaryContainer = Color(0xFF1C1A27),
    background = Color(0xFFF5F4F9),
    onBackground = Color(0xFF13121A),
    surface = Color(0xFFF5F4F9),
    onSurface = Color(0xFF13121A),
    surfaceVariant = Color(0xFFE7E5EF),
    onSurfaceVariant = Color(0xFF63617A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFAFD),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEDEBF4),
    surfaceContainerHighest = Color(0xFFE4E1EE),
    outline = Color(0xFFCFCCDB),
    outlineVariant = Color(0xFFE4E2EC),
    error = DeleteRed,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA08BFF),
    onPrimary = Color(0xFF16093F),
    primaryContainer = Color(0xFF2F2370),
    onPrimaryContainer = Color(0xFFE6DFFF),
    secondary = Color(0xFFC8C4D9),
    onSecondary = Color(0xFF2B2838),
    secondaryContainer = Color(0xFF2A2836),
    onSecondaryContainer = Color(0xFFE6E2F2),
    background = Color(0xFF0B0A10),
    onBackground = Color(0xFFF2F1F7),
    surface = Color(0xFF0B0A10),
    onSurface = Color(0xFFF2F1F7),
    surfaceVariant = Color(0xFF1C1B24),
    onSurfaceVariant = Color(0xFFA4A2B5),
    surfaceContainerLowest = Color(0xFF07060B),
    surfaceContainerLow = Color(0xFF111017),
    surfaceContainer = Color(0xFF16151D),
    surfaceContainerHigh = Color(0xFF1E1D27),
    surfaceContainerHighest = Color(0xFF282733),
    outline = Color(0xFF393845),
    outlineVariant = Color(0xFF25242E),
    error = DeleteRed,
    onError = Color.White,
)

private val SweepyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Sweepy's own colors instead of the wallpaper-based "Material You" ones, so the brand looks the
 * same on every phone. Follows the system light/dark setting.
 */
@Composable
fun SweepyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SweepyTypography,
        shapes = SweepyShapes,
        content = content,
    )
}
