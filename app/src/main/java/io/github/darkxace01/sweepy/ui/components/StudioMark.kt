package io.github.darkxace01.sweepy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.darkxace01.sweepy.ui.theme.Mono
import io.github.darkxace01.sweepy.ui.theme.Serif
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Grease Pencil Studio's mark: the studio's initials in an italic serif, circled in red grease
 * pencil. It stays the same in every app and every theme, so its red is the studio's own pencil red,
 * not the app theme's accent.
 */
object Studio {
    val Red = Color(0xFFD4432A)
    val RedOnDark = Color(0xFFE5553A)

    /** The right red for text of color [ink]: light ink means a dark background. */
    fun redFor(ink: Color) = if (ink.luminance() > 0.5f) RedOnDark else Red
}

/** The monogram: initials with the loop round them. [size] is the square it fills. */
@Composable
fun StudioMonogram(
    ink: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    letters: String = "gp",
    red: Color = Studio.redFor(ink),
) {
    val fontSize = with(LocalDensity.current) { (size * 0.46f).toSp() }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size).studioLoop(red, strokeWidth = 0.062f)) {}
        Text(
            letters,
            style = TextStyle(
                fontFamily = Serif,
                fontStyle = FontStyle.Italic,
                fontSize = fontSize,
                lineHeight = 1.em,
                letterSpacing = (-0.02).em,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
            color = ink,
            // Italic letters lean right and "g" hangs low; nudge them back to the optical centre.
            modifier = Modifier.offset(x = -size * 0.02f, y = -size * 0.035f),
        )
    }
}

/** Monogram plus the name, side by side: "Grease Pencil" over a small "STUDIO". */
@Composable
fun StudioLockup(ink: Color, modifier: Modifier = Modifier, height: Dp = 40.dp, muted: Color = ink.copy(alpha = 0.6f)) {
    val density = LocalDensity.current
    // Read out as one name, not "gp, Grease Pencil, STUDIO".
    Row(modifier.clearAndSetSemantics { contentDescription = "Grease Pencil Studio" }, verticalAlignment = Alignment.CenterVertically) {
        StudioMonogram(ink, size = height)
        Spacer(Modifier.width(height * 0.28f))
        Column {
            Text(
                "Grease Pencil",
                style = TextStyle(
                    fontFamily = Serif,
                    fontSize = with(density) { (height * 0.5f).toSp() },
                    lineHeight = 1.em,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
                color = ink,
            )
            Spacer(Modifier.size(height * 0.08f))
            Text(
                "STUDIO",
                style = TextStyle(
                    fontFamily = Mono,
                    fontSize = with(density) { (height * 0.2f).toSp() },
                    letterSpacing = 0.32.em,
                    lineHeight = 1.em,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
                color = muted,
            )
        }
    }
}

/**
 * The studio's loop, always in the plain grease-pencil hand whatever the app's theme: a slightly
 * lopsided oval that overshoots where it started, with a faint second stroke for the waxy edge.
 */
private fun Modifier.studioLoop(color: Color, strokeWidth: Float): Modifier = drawWithCache {
    val path = loopPath(size)
    val width = min(size.width, size.height) * strokeWidth
    val segment = Path().apply {
        val measure = PathMeasure().apply { setPath(path, false) }
        measure.getSegment(0f, measure.length, this, true)
    }
    onDrawBehind {
        fun line(c: Color, w: Float) = drawPath(segment, c, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        line(color.copy(alpha = color.alpha * 0.92f), width)
        translate(width * 0.18f, -width * 0.12f) { line(color.copy(alpha = color.alpha * 0.35f), width * 0.55f) }
    }
}

private fun loopPath(s: Size): Path = Path().apply {
    val cx = s.width * 0.5f
    val cy = s.height * 0.5f
    val rx = s.width * 0.41f
    val ry = s.height * 0.37f
    val tilt = -0.14f
    val steps = 80
    val sweep = 2.0 * PI * 1.12
    val startAngle = -PI * 0.62
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val a = startAngle + sweep * t
        val wobble = 1f + 0.035f * sin(2.0 * a + 0.7).toFloat() + 0.07f * t
        val x = cos(a).toFloat() * rx * wobble
        val y = sin(a).toFloat() * ry * wobble
        val px = cx + x * cos(tilt) - y * sin(tilt)
        val py = cy + x * sin(tilt) + y * cos(tilt)
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
}
