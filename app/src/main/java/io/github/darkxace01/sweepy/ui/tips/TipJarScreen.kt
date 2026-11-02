package io.github.darkxace01.sweepy.ui.tips

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.tips.Tip
import io.github.darkxace01.sweepy.data.tips.TipJarState
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.InkButton
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.MarkIcon
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.ScreenTopBar
import io.github.darkxace01.sweepy.ui.components.chunky
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.theme.Sweepy

/** What to say under the tips after a purchase that didn't end in a thank-you. */
enum class TipNote { PENDING, FAILED }

@Composable
fun TipJarScreen(
    state: TipJarState,
    tipsGiven: Int,
    thanked: Tip?,
    note: TipNote?,
    onBuy: (Tip) -> Unit,
    onDismissThanks: () -> Unit,
    onBack: () -> Unit,
) {
    Paper {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            ScreenTopBar(
                title = stringResource(R.string.tips_title),
                eyebrow = stringResource(R.string.tips_eyebrow),
                onBack = onBack,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 40.dp),
            ) {
                Text(
                    stringResource(R.string.tips_headline),
                    style = MaterialTheme.typography.displaySmall,
                    color = Sweepy.colors.ink,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.tips_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Sweepy.colors.inkMuted,
                )
                Spacer(Modifier.height(28.dp))

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (tip in Tip.entries) TipCard(tip, state, onBuy)
                }

                val message = when {
                    note == TipNote.PENDING -> stringResource(R.string.tips_pending)
                    note == TipNote.FAILED -> stringResource(R.string.tips_failed)
                    state == TipJarState.Unavailable -> stringResource(R.string.tips_unavailable)
                    else -> null
                }
                if (message != null) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = if (note == TipNote.PENDING) Sweepy.colors.keep else Sweepy.colors.pencil,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }

                if (tipsGiven > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 26.dp)) {
                        MarkIcon(Mark.STAR, Sweepy.colors.favorite, size = 22.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            pluralStringResource(R.plurals.tips_given, tipsGiven, tipsGiven),
                            style = MaterialTheme.typography.titleMedium,
                            color = Sweepy.colors.ink,
                        )
                    }
                }

                Spacer(Modifier.height(30.dp))
                Text(
                    stringResource(R.string.tips_fine_print),
                    style = MaterialTheme.typography.bodySmall,
                    color = Sweepy.colors.inkMuted,
                )
            }
        }

        // Remember the last tip so the picture stays put while the thank-you fades out.
        val last = remember { arrayOf(Tip.COFFEE) }
        if (thanked != null) last[0] = thanked
        AnimatedVisibility(thanked != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
            ThankYou(last[0], onDismissThanks)
        }
    }
}

@Composable
private fun TipCard(tip: Tip, state: TipJarState, onBuy: (Tip) -> Unit) {
    val price = (state as? TipJarState.Ready)?.prices?.get(tip)
    val enabled = price != null
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (state == TipJarState.Unavailable) 0.55f else 1f)
            .chunky(shape, fill = Sweepy.colors.surface, offset = 4.dp)
            .clip(shape)
            .background(Sweepy.colors.surface)
            .border(1.dp, Sweepy.colors.hairline, shape)
            .pressable({ onBuy(tip) }, enabled = enabled, pressedScale = 0.97f, role = Role.Button)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Sweepy.colors.paper),
        ) {
            TipArt(tip, Modifier.size(48.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(tipName(tip), style = MaterialTheme.typography.headlineSmall, color = Sweepy.colors.ink)
            Spacer(Modifier.height(2.dp))
            Text(tipLine(tip), style = MaterialTheme.typography.bodySmall, color = Sweepy.colors.inkMuted)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            price ?: if (state == TipJarState.Loading) "···" else "–",
            style = MaterialTheme.typography.labelLarge,
            color = Sweepy.colors.paper,
            modifier = Modifier
                .widthIn(min = 64.dp)
                .clip(CircleShape)
                .background(if (enabled) Sweepy.colors.ink else Sweepy.colors.inkMuted)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ThankYou(tip: Tip, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val circle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        circle.animateTo(1f, tween(900, delayMillis = 250))
    }
    Paper(Modifier.pressable(onDismiss, pressedScale = 1f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center).safeDrawingPadding().padding(32.dp),
        ) {
            TipArt(tip, Modifier.size(132.dp).rotate(-6f))
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.tips_thanks_title),
                style = MaterialTheme.typography.displayMedium,
                fontStyle = FontStyle.Italic,
                color = Sweepy.colors.ink,
                modifier = Modifier
                    .pencilMark(Mark.CIRCLE, Sweepy.colors.pencil, { circle.value }, strokeWidth = 0.06f)
                    .padding(horizontal = 26.dp, vertical = 14.dp),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.tips_thanks_body, tipName(tip).lowercase()),
                style = MaterialTheme.typography.bodyLarge,
                color = Sweepy.colors.inkMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            InkButton(stringResource(R.string.tips_thanks_done), onDismiss)
        }
    }
}

@Composable
fun tipName(tip: Tip): String = stringResource(
    when (tip) {
        Tip.COFFEE -> R.string.tip_coffee
        Tip.LUNCH -> R.string.tip_lunch
        Tip.DINNER -> R.string.tip_dinner
    },
)

@Composable
private fun tipLine(tip: Tip): String = stringResource(
    when (tip) {
        Tip.COFFEE -> R.string.tip_coffee_line
        Tip.LUNCH -> R.string.tip_lunch_line
        Tip.DINNER -> R.string.tip_dinner_line
    },
)

// ---- Drawings -----------------------------------------------------------------------------------

/** A small hand-drawn picture of each tip, in the theme's inks. */
@Composable
fun TipArt(tip: Tip, modifier: Modifier = Modifier) {
    val ink = Sweepy.colors.ink
    val fill = Sweepy.colors.print
    val accent = Sweepy.colors.pencil
    val green = Sweepy.colors.keep
    val yellow = Sweepy.colors.later
    Canvas(modifier) {
        val line = Stroke(size.minDimension * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (tip) {
            Tip.COFFEE -> drawCoffee(ink, fill, accent, line)
            Tip.LUNCH -> drawBurger(ink, fill, green, yellow, accent, line)
            Tip.DINNER -> drawPizza(ink, yellow, accent, line)
        }
    }
}

/** The coffee cup alone, sized for a top-bar icon. */
@Composable
fun CoffeeIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val line = Stroke(this.size.minDimension * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCoffee(color, Color.Transparent, color, line, steamLines = 2)
    }
}

private fun DrawScope.drawCoffee(ink: Color, fill: Color, steam: Color, line: Stroke, steamLines: Int = 3) {
    val w = size.width
    val h = size.height
    // Cup: wider at the rim, rounded at the bottom.
    val cup = Path().apply {
        moveTo(w * 0.16f, h * 0.42f)
        lineTo(w * 0.70f, h * 0.42f)
        lineTo(w * 0.65f, h * 0.78f)
        quadraticTo(w * 0.63f, h * 0.88f, w * 0.53f, h * 0.88f)
        lineTo(w * 0.33f, h * 0.88f)
        quadraticTo(w * 0.23f, h * 0.88f, w * 0.21f, h * 0.78f)
        close()
    }
    drawPath(cup, fill)
    drawPath(cup, ink, style = line)
    // Handle.
    val handle = Path().apply {
        moveTo(w * 0.69f, h * 0.50f)
        cubicTo(w * 0.90f, h * 0.48f, w * 0.90f, h * 0.72f, w * 0.66f, h * 0.70f)
    }
    drawPath(handle, ink, style = line)
    // Steam.
    val xs = listOf(0.30f, 0.43f, 0.56f).take(steamLines).let { if (steamLines == 2) listOf(0.34f, 0.52f) else it }
    for ((i, x) in xs.withIndex()) {
        val top = h * (0.10f + (i % 2) * 0.04f)
        val wisp = Path().apply {
            moveTo(w * x, h * 0.34f)
            cubicTo(w * (x - 0.07f), h * 0.27f, w * (x + 0.07f), h * 0.21f, w * x, top)
        }
        drawPath(wisp, steam, style = line)
    }
}

private fun DrawScope.drawBurger(ink: Color, fill: Color, lettuce: Color, cheese: Color, tomato: Color, line: Stroke) {
    val w = size.width
    val h = size.height
    // Top bun.
    val top = Path().apply {
        moveTo(w * 0.12f, h * 0.48f)
        cubicTo(w * 0.12f, h * 0.16f, w * 0.88f, h * 0.16f, w * 0.88f, h * 0.48f)
        close()
    }
    drawPath(top, fill)
    drawPath(top, ink, style = line)
    for ((x, y) in listOf(0.38f to 0.32f, 0.52f to 0.27f, 0.64f to 0.35f)) {
        drawLine(ink, Offset(w * x, h * y), Offset(w * (x + 0.035f), h * (y - 0.015f)), line.width * 0.9f, StrokeCap.Round)
    }
    // Lettuce, tomato, cheese.
    val leaf = Path().apply {
        moveTo(w * 0.10f, h * 0.53f)
        for (i in 0 until 8) {
            val x0 = 0.10f + i * 0.1f
            quadraticTo(w * (x0 + 0.05f), h * 0.61f, w * (x0 + 0.1f), h * 0.53f)
        }
    }
    drawPath(leaf, lettuce, style = line)
    drawRoundRect(tomato, Offset(w * 0.15f, h * 0.58f), Size(w * 0.70f, h * 0.07f), CornerRadius(h * 0.03f))
    drawRoundRect(cheese, Offset(w * 0.13f, h * 0.645f), Size(w * 0.74f, h * 0.05f), CornerRadius(h * 0.02f))
    // Patty and bottom bun.
    drawRoundRect(ink, Offset(w * 0.14f, h * 0.69f), Size(w * 0.72f, h * 0.09f), CornerRadius(h * 0.045f))
    val bottom = Rect(w * 0.14f, h * 0.80f, w * 0.86f, h * 0.90f)
    drawRoundRect(fill, bottom.topLeft, bottom.size, CornerRadius(h * 0.05f))
    drawRoundRect(ink, bottom.topLeft, bottom.size, CornerRadius(h * 0.05f), style = line)
}

private fun DrawScope.drawPizza(ink: Color, cheese: Color, pepperoni: Color, line: Stroke) {
    val w = size.width
    val h = size.height
    // A slice, point down, with the crust along the top.
    val slice = Path().apply {
        moveTo(w * 0.14f, h * 0.24f)
        quadraticTo(w * 0.50f, h * 0.10f, w * 0.86f, h * 0.24f)
        lineTo(w * 0.52f, h * 0.92f)
        close()
    }
    drawPath(slice, cheese)
    drawPath(slice, ink, style = line)
    val crust = Path().apply {
        moveTo(w * 0.14f, h * 0.24f)
        quadraticTo(w * 0.50f, h * 0.10f, w * 0.86f, h * 0.24f)
        lineTo(w * 0.81f, h * 0.33f)
        quadraticTo(w * 0.50f, h * 0.21f, w * 0.19f, h * 0.33f)
        close()
    }
    drawPath(crust, ink)
    for ((x, y, r) in listOf(Triple(0.38f, 0.44f, 0.07f), Triple(0.60f, 0.48f, 0.065f), Triple(0.49f, 0.66f, 0.06f))) {
        drawCircle(pepperoni, w * r, Offset(w * x, h * y))
    }
    // A drip of cheese.
    drawLine(ink, Offset(w * 0.30f, h * 0.52f), Offset(w * 0.33f, h * 0.58f), line.width * 0.8f, StrokeCap.Round)
}
