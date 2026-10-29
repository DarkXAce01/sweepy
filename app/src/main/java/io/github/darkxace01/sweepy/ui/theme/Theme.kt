package io.github.darkxace01.sweepy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val DeleteRed = Color(0xFFE5484D)
val KeepGreen = Color(0xFF30A46C)
val LaterAmber = Color(0xFFF5A524)
val FavoritePink = Color(0xFFE93D82)

private val LightColors = lightColorScheme(primary = Color(0xFF5B3DF5))
private val DarkColors = darkColorScheme(primary = Color(0xFFC4B8FF))

@Composable
fun SweepyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    // Android 12+ can match the user's wallpaper colors ("Material You").
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
