package io.github.darkxace01.sweepy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.darkxace01.sweepy.ui.components.StudioLockup
import io.github.darkxace01.sweepy.ui.components.StudioMonogram
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Draws Grease Pencil Studio's logo files into app/build/outputs/studio. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w400dp-h400dp-xxhdpi")
class StudioBrandTest {
    private val paper = Color(0xFFF2EFE8)
    private val ink = Color(0xFF1C1A17)

    /** Play developer page icon: 512 × 512. */
    @Config(qualifiers = "w256dp-h256dp-xhdpi")
    @Test fun icon_light() = render("studio_icon_light", dark = false) { StudioMonogram(it, size = 200.dp) }

    @Config(qualifiers = "w256dp-h256dp-xhdpi")
    @Test fun icon_dark() = render("studio_icon_dark", dark = true) { StudioMonogram(it, size = 200.dp) }

    /** Play developer page header: 4096 × 2304. */
    @Config(qualifiers = "w2048dp-h1152dp-xhdpi")
    @Test fun header_light() = render("studio_header_light", dark = false) { StudioLockup(it, height = 220.dp) }

    @Config(qualifiers = "w2048dp-h1152dp-xhdpi")
    @Test fun header_dark() = render("studio_header_dark", dark = true) { StudioLockup(it, height = 220.dp) }

    /** The logo on its own, for anywhere else: 2000 × 640. */
    @Config(qualifiers = "w1000dp-h320dp-xhdpi")
    @Test fun lockup_light() = render("studio_logo_light", dark = false) { StudioLockup(it, height = 160.dp) }

    @Config(qualifiers = "w1000dp-h320dp-xhdpi")
    @Test fun lockup_dark() = render("studio_logo_dark", dark = true) { StudioLockup(it, height = 160.dp) }

    private fun render(name: String, dark: Boolean, content: @Composable (Color) -> Unit) =
        captureRoboImage("build/outputs/studio/$name.png") {
            Box(Modifier.fillMaxSize().background(if (dark) ink else paper), contentAlignment = Alignment.Center) {
                content(if (dark) paper else ink)
            }
        }
}
