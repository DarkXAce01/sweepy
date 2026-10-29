package io.github.darkxace01.sweepy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Hand-drawn grease-pencil marks, the way photographers edit a contact sheet:
 * a cross through rejects, a loop around keepers.
 */
enum class Mark { CROSS, CIRCLE, QUESTION, STAR }

/** One pencil stroke, drawn while overall progress moves through [start]..[end]. */
private class PencilStroke(val points: (Size) -> Path, val start: Float, val end: Float)

private fun strokesFor(mark: Mark): List<PencilStroke> = when (mark) {
    Mark.CROSS -> listOf(
        PencilStroke({ s ->
            Path().apply {
                moveTo(s.width * 0.20f, s.height * 0.18f)
                cubicTo(s.width * 0.42f, s.height * 0.36f, s.width * 0.60f, s.height * 0.62f, s.width * 0.83f, s.height * 0.84f)
            }
        }, 0f, 0.52f),
        PencilStroke({ s ->
            Path().apply {
                moveTo(s.width * 0.81f, s.height * 0.16f)
                cubicTo(s.width * 0.60f, s.height * 0.40f, s.width * 0.42f, s.height * 0.63f, s.width * 0.17f, s.height * 0.85f)
            }
        }, 0.48f, 1f),
    )
    Mark.CIRCLE -> listOf(PencilStroke({ s -> handLoop(s) }, 0f, 1f))
    Mark.QUESTION -> listOf(
        PencilStroke({ s ->
            Path().apply {
                moveTo(s.width * 0.30f, s.height * 0.33f)
                cubicTo(s.width * 0.30f, s.height * 0.10f, s.width * 0.72f, s.height * 0.08f, s.width * 0.70f, s.height * 0.32f)
                cubicTo(s.width * 0.69f, s.height * 0.47f, s.width * 0.50f, s.height * 0.48f, s.width * 0.51f, s.height * 0.66f)
            }
        }, 0f, 0.85f),
        PencilStroke({ s ->
            Path().apply {
                moveTo(s.width * 0.505f, s.height * 0.82f)
                lineTo(s.width * 0.515f, s.height * 0.845f)
            }
        }, 0.88f, 1f),
    )
    Mark.STAR -> listOf(PencilStroke({ s -> handStar(s) }, 0f, 1f))
}

/** A slightly lopsided oval that overshoots its start, like a quick circle drawn by hand. */
private fun handLoop(s: Size): Path = Path().apply {
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

/** A five-point star drawn in one go, with a little hand jitter. */
private fun handStar(s: Size): Path = Path().apply {
    val cx = s.width * 0.5f
    val cy = s.height * 0.53f
    val r = min(s.width, s.height) * 0.42f
    val jitter = floatArrayOf(1.0f, 0.95f, 1.04f, 0.97f, 1.02f, 1.0f)
    for (k in 0..5) {
        val a = -PI / 2 + k * 4 * PI / 5
        val px = cx + cos(a).toFloat() * r * jitter[k]
        val py = cy + sin(a).toFloat() * r * jitter[k]
        if (k == 0) moveTo(px, py) else lineTo(px, py)
    }
    // Overshoot slightly past the first point.
    lineTo(cx + cos(-PI / 2 + 0.25).toFloat() * r * 0.92f, cy + sin(-PI / 2 + 0.25).toFloat() * r * 0.92f)
}

/**
 * Draws [mark] in grease pencil, revealed up to [progress] (0..1), so it can follow a finger.
 * [strokeWidth] is a fraction of the mark's size.
 */
fun Modifier.pencilMark(
    mark: Mark,
    color: Color,
    progress: () -> Float,
    strokeWidth: Float = 0.055f,
): Modifier = drawWithCache {
    val strokes = strokesFor(mark).map { it to it.points(size) }
    val measure = PathMeasure()
    val segment = Path()
    val width = min(size.width, size.height) * strokeWidth
    onDrawWithContent {
        drawContent()
        drawPencil(strokes, measure, segment, progress(), color, width)
    }
}

private fun DrawScope.drawPencil(
    strokes: List<Pair<PencilStroke, Path>>,
    measure: PathMeasure,
    segment: Path,
    progress: Float,
    color: Color,
    width: Float,
) {
    if (progress <= 0f) return
    for ((stroke, path) in strokes) {
        val local = ((progress - stroke.start) / (stroke.end - stroke.start)).coerceIn(0f, 1f)
        if (local <= 0f) continue
        measure.setPath(path, false)
        segment.reset()
        measure.getSegment(0f, measure.length * local, segment, true)
        // A main stroke plus a faint offset one gives the waxy, uneven edge of a china marker.
        drawPath(segment, color.copy(alpha = color.alpha * 0.92f), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        translate(width * 0.18f, -width * 0.12f) {
            drawPath(segment, color.copy(alpha = color.alpha * 0.35f), style = Stroke(width * 0.55f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** A fully drawn mark used as an icon. */
@Composable
fun MarkIcon(mark: Mark, color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp, strokeWidth: Float = 0.09f) {
    Canvas(modifier.size(size).pencilMark(mark, color, { 1f }, strokeWidth)) {}
}
