package io.github.darkxace01.sweepy

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.darkxace01.sweepy.data.ThemeChoice
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.MarkIcon
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import io.github.darkxace01.sweepy.ui.theme.SweepyTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The banner at the top of the GitHub README, in light and dark: 1280 × 640, which is also the size
 * GitHub wants for a repository's social preview. Output goes to app/build/outputs/readme.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w640dp-h320dp-xhdpi")
class ReadmeArtTest {
    @Test fun banner_light() = banner("banner_light", dark = false)

    @Test fun banner_dark() = banner("banner_dark", dark = true)

    private fun banner(name: String, dark: Boolean) = captureRoboImage("build/outputs/readme/$name.png") {
        SweepyTheme(darkTheme = dark, theme = ThemeChoice.CONTACT_SHEET) {
            Paper {
                Row(Modifier.align(Alignment.Center).offset(x = (-8).dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(Modifier.size(176.dp))
                    Spacer(Modifier.width(40.dp))
                    Column {
                        Eyebrow("Photo & video cleaner for Android", color = Sweepy.colors.pencil)
                        Text(
                            "Sweepy",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp, lineHeight = 88.sp),
                            color = Sweepy.colors.ink,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Line("Swipe your gallery")
                            Circled("clean")
                            Line(".", Modifier.offset(x = (-11).dp))
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for ((i, promise) in listOf("No internet", "No ads", "Open source").withIndex()) {
                                if (i > 0) Spacer(Modifier.width(14.dp))
                                MarkIcon(Mark.CIRCLE, Sweepy.colors.keep, size = 12.dp, strokeWidth = 0.16f)
                                Spacer(Modifier.width(6.dp))
                                Eyebrow(promise, color = Sweepy.colors.ink, small = true)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Line(text: String, modifier: Modifier = Modifier, italic: Boolean = false) = Text(
        text,
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        ),
        color = Sweepy.colors.inkMuted,
        modifier = modifier,
    )

    /** A word with the grease-pencil loop drawn behind it, wide enough to clear the letters. */
    @Composable
    private fun Circled(word: String) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.25f
                        scaleY = 1.45f
                    }
                    .pencilMark(Mark.CIRCLE, Sweepy.colors.pencil, { 1f }, strokeWidth = 0.06f),
            )
            Line(word, italic = true)
        }
    }

    /** The launcher icon as it looks on a phone: the print on its paper square, rounded. */
    @Composable
    private fun AppIcon(modifier: Modifier) {
        val shape = RoundedCornerShape(26)
        Box(
            modifier
                .shadow(18.dp, shape)
                .clip(shape)
                .background(colorResource(R.color.ic_launcher_background)),
            contentAlignment = Alignment.Center,
        ) {
            // The foreground is drawn for a 108dp canvas of which the middle 72dp shows.
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(176.dp * 108f / 72f))
        }
    }
}
