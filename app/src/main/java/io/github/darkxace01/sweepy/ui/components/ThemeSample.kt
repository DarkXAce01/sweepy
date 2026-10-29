package io.github.darkxace01.sweepy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.data.ThemeChoice
import io.github.darkxace01.sweepy.ui.theme.LocalSweepyLook
import io.github.darkxace01.sweepy.ui.theme.lookFor
import io.github.darkxace01.sweepy.ui.theme.typographyFor

/**
 * A little swatch of [theme], drawn in that theme itself whatever the app currently uses:
 * its paper, its headline type and its three marks.
 */
@Composable
fun ThemeSample(theme: ThemeChoice, dark: Boolean, modifier: Modifier = Modifier, markSize: Dp = 20.dp, padding: Dp = 12.dp) {
    val look = lookFor(theme, dark)
    CompositionLocalProvider(LocalSweepyLook provides look) {
        MaterialTheme(typography = typographyFor(theme, look)) {
            Paper(modifier) {
                // Pinned top and bottom, so a tall font can't squash the marks in a small swatch.
                Box(Modifier.fillMaxSize().padding(padding)) {
                    Text("Aa", style = MaterialTheme.typography.headlineLarge, color = look.colors.ink, modifier = Modifier.align(Alignment.TopStart))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.align(Alignment.BottomStart)) {
                        MarkIcon(Mark.CROSS, look.colors.pencil, size = markSize, strokeWidth = 0.12f)
                        MarkIcon(Mark.CIRCLE, look.colors.keep, size = markSize, strokeWidth = 0.1f)
                        MarkIcon(Mark.STAR, look.colors.favorite, size = markSize, strokeWidth = 0.1f)
                    }
                }
            }
        }
    }
}
