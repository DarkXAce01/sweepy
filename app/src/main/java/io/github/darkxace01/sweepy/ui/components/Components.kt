package io.github.darkxace01.sweepy.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.formatDuration
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import io.github.darkxace01.sweepy.ui.theme.Texture
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import java.util.Random
import kotlinx.coroutines.delay

// ---- Surfaces -----------------------------------------------------------------------------------

/** Fine, static film grain. Generated once; tiles across any size. */
private val grainBitmap: ImageBitmap by lazy {
    val n = 192
    val random = Random(1957)
    val pixels = IntArray(n * n) {
        val alpha = random.nextInt(48)
        val rgb = if (random.nextBoolean()) 0xFFFFFF else 0x000000
        (alpha shl 24) or rgb
    }
    Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Draws film grain behind this element's content. */
fun Modifier.grain(strength: Float = 0.55f): Modifier = drawWithCache {
    val brush = ShaderBrush(ImageShader(grainBitmap, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind { drawRect(brush, alpha = strength) }
}

/** Halftone screen: a dot every 16 px on a 45° grid, the way a risograph lays down tone. */
private val halftoneBitmap: ImageBitmap by lazy {
    val n = 16
    val centers = listOf(0f to 0f, 8f to 8f, 16f to 0f, 0f to 16f, 16f to 16f)
    val pixels = IntArray(n * n) { i ->
        val x = i % n + 0.5f
        val y = i / n + 0.5f
        val d = centers.minOf { (cx, cy) -> kotlin.math.hypot(x - cx, y - cy) }
        val alpha = ((2.4f - d).coerceIn(0f, 1f) * 255).toInt()
        (alpha shl 24) or 0xFFFFFF
    }
    Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Dot-grid paper: one soft dot every 44 px, like a designer's notebook. */
private val dotGridBitmap: ImageBitmap by lazy {
    val n = 44
    val c = n / 2f
    val pixels = IntArray(n * n) { i ->
        val d = kotlin.math.hypot(i % n + 0.5f - c, i / n + 0.5f - c)
        val alpha = ((2.2f - d).coerceIn(0f, 1f) * 255).toInt()
        (alpha shl 24) or 0xFFFFFF
    }
    Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Tiles a white [pattern] across the element, tinted with [color]. */
private fun Modifier.pattern(pattern: ImageBitmap, color: Color, alpha: Float): Modifier = drawWithCache {
    val brush = ShaderBrush(ImageShader(pattern, TileMode.Repeated, TileMode.Repeated))
    val tint = ColorFilter.tint(color, BlendMode.SrcIn)
    onDrawBehind { drawRect(brush, alpha = alpha, colorFilter = tint) }
}

/** The page every screen sits on, with the active theme's texture: film grain, halftone or a dot grid. */
@Composable
fun Paper(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val look = Sweepy.look
    val texture = when (look.texture) {
        Texture.GRAIN -> Modifier.grain()
        Texture.HALFTONE -> Modifier.grain(0.3f).pattern(halftoneBitmap, look.colors.ink, if (look.colors.isDark) 0.07f else 0.05f)
        Texture.DOT_GRID -> Modifier.pattern(dotGridBitmap, look.colors.ink, if (look.colors.isDark) 0.22f else 0.2f)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(look.colors.paper)
            .then(texture),
        content = content,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Sweepy.colors.hairline))
}

// ---- Text ---------------------------------------------------------------------------------------

/** Small uppercase monospace caption, like the edge print on film. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Sweepy.colors.inkMuted, small: Boolean = false) {
    Text(
        text.uppercase(),
        style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** The last value each [CountUp] showed, while the app is running. */
private val countUpShown = HashMap<String, Long>()

/**
 * A number that counts to [value]. With a [memoryKey] it counts up from zero only the first time
 * it's shown after the app starts; after that it stays put when scrolled back into view, and only
 * animates when the number actually changes (from the old number to the new one).
 */
@Composable
fun CountUp(
    value: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Sweepy.colors.ink,
    memoryKey: String? = null,
    format: (Long) -> String = ::formatCount,
) {
    val inspection = LocalInspectionMode.current
    val start = when {
        inspection -> value
        memoryKey != null -> countUpShown[memoryKey] ?: 0L
        else -> 0L
    }
    val animated = remember { Animatable(start.toFloat()) }
    LaunchedEffect(value) {
        animated.animateTo(value.toFloat(), tween(durationMillis = 1100, easing = FastOutSlowInEasing))
        if (memoryKey != null) countUpShown[memoryKey] = value
    }
    Text(format(animated.value.toLong()), style = style, color = color, modifier = modifier, maxLines = 1)
}

/**
 * Fades and lifts content in when it first appears, [index] steps after the screen opens,
 * so a page settles in from top to bottom instead of popping.
 */
@Composable
fun Modifier.reveal(index: Int, enabled: Boolean = true): Modifier {
    if (!enabled || LocalInspectionMode.current) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(60L * index.coerceAtMost(8))
        progress.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 14.dp.toPx()
    }
}

/**
 * Pop's sticker treatment: a thick outline and a hard shadow offset down and right. Does nothing
 * in other themes. Put it before `clip` so the shadow can sit outside the shape; [fill] paints
 * under elements that are otherwise see-through, so the shadow doesn't show through them.
 */
@Composable
fun Modifier.chunky(shape: Shape, fill: Color? = null, offset: Dp = 4.dp, shadow: Color? = null): Modifier {
    val look = Sweepy.look
    if (!look.chunky) return this
    val shadowColor = shadow ?: look.shadow
    return drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        val o = offset.toPx()
        translate(o, o) { drawOutline(outline, shadowColor) }
        if (fill != null) drawOutline(outline, fill)
    }.border(2.dp, look.outline, shape)
}

// ---- Progress -----------------------------------------------------------------------------------

/** A progress indicator drawn like a ruler: tick marks with an ink line filling along the edge. */
@Composable
fun Ruler(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Sweepy.colors.ink,
    tickColor: Color = Sweepy.colors.inkMuted.copy(alpha = 0.45f),
    ticks: Int = 40,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessVeryLow), label = "ruler")
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val stroke = 1.dp.toPx()
        val y = size.height - stroke
        for (i in 0..ticks) {
            val x = (size.width - stroke) * i / ticks + stroke / 2
            val h = when {
                i % 10 == 0 -> 9.dp.toPx()
                i % 5 == 0 -> 6.dp.toPx()
                else -> 3.5.dp.toPx()
            }
            drawLine(tickColor, Offset(x, y), Offset(x, y - h), stroke)
        }
        drawLine(tickColor, Offset(0f, y), Offset(size.width, y), stroke)
        if (animated > 0f) {
            drawLine(color, Offset(0f, y), Offset(size.width * animated, y), 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/** A thin, simple progress line for small places. */
@Composable
fun ThinProgress(progress: Float, modifier: Modifier = Modifier, color: Color = Sweepy.colors.ink) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessLow), label = "thin")
    val track = Sweepy.colors.hairline
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        drawLine(track, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height)
        if (animated > 0f) drawLine(color, Offset(0f, size.height / 2), Offset(size.width * animated, size.height / 2), size.height)
    }
}

// ---- Interaction --------------------------------------------------------------------------------

/** Clickable that also sinks slightly while pressed, for a tactile feel. */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    role: Role = Role.Button,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = role, onClick = onClick)
}

/** Primary button: solid ink (or [color]) with paper text. */
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Sweepy.colors.ink,
    contentColor: Color = Sweepy.colors.paper,
    enabled: Boolean = true,
    trailing: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            // An ink button's own shadow would blend into it, so it sits on a yellow slab instead.
            .chunky(RoundedCornerShape(14.dp), shadow = if (color == Sweepy.colors.ink) Sweepy.colors.later else null)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .pressable(onClick, enabled = enabled)
            .padding(horizontal = 20.dp),
    ) {
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            Text(trailing, color = contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** Secondary button: hairline outline, ink text. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, contentColor: Color = Sweepy.colors.ink) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(56.dp)
            .chunky(RoundedCornerShape(14.dp), fill = Sweepy.colors.surface)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Sweepy.colors.hairline, RoundedCornerShape(14.dp))
            .pressable(onClick)
            .padding(horizontal = 20.dp),
    ) {
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** A plain icon button with a generous touch target and no background. */
/** One choice in a row of mutually exclusive options; the chosen one is inked in. */
@Composable
fun Segment(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .chunky(shape, fill = Sweepy.colors.surface, offset = if (selected) 4.dp else 2.dp)
            .clip(shape)
            .background(if (selected) Sweepy.colors.ink else Color.Transparent)
            .border(1.dp, if (selected) Sweepy.colors.ink else Sweepy.colors.hairline, shape)
            .pressable(onClick, role = Role.RadioButton),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Sweepy.colors.paper else Sweepy.colors.ink,
        )
    }
}

@Composable
fun QuietIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .pressable(onClick, pressedScale = 0.85f)
            .semantics { this.contentDescription = contentDescription },
    ) { content() }
}

// ---- Media --------------------------------------------------------------------------------------

/** Cropped thumbnail with a small duration label for videos. */
@Composable
fun MediaThumbnail(item: MediaItem, modifier: Modifier = Modifier, cornerRadius: Dp = 8.dp, showBadge: Boolean = true) {
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Sweepy.colors.hairline),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (showBadge && item.isVideo) {
            Text(
                "▶ " + formatDuration(item.durationMillis),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

/** Up to four photos in a 2×2 grid: a deck's "cover". */
@Composable
fun Mosaic(items: List<MediaItem>, modifier: Modifier = Modifier, gap: Dp = 2.dp, cornerRadius: Dp = 10.dp) {
    Column(
        verticalArrangement = Arrangement.spacedBy(gap),
        modifier = modifier
            .aspectRatio(1f)
            .chunky(RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(Sweepy.colors.hairline),
    ) {
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.weight(1f)) {
                for (col in 0 until 2) {
                    val item = items.getOrNull(row * 2 + col)
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        if (item != null) MediaThumbnail(item, Modifier.fillMaxSize(), cornerRadius = 0.dp, showBadge = false)
                    }
                }
            }
        }
    }
}

// ---- Screen scaffolding -------------------------------------------------------------------------

/** Back button, eyebrow + serif title, and optional actions, used by the secondary screens. */
@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        QuietIconButton(stringResource(R.string.back), onBack) {
            Icon(rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack), contentDescription = null, tint = Sweepy.colors.ink)
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (eyebrow != null) Eyebrow(eyebrow, small = true)
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = Sweepy.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

/** Area pinned to the bottom of a screen that fades into the content above it. */
@Composable
fun BottomActionArea(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val paper = Sweepy.colors.paper
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to paper.copy(alpha = 0f), 0.3f to paper))
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 14.dp),
        content = content,
    )
}

/** Centered mark + message for empty screens. */
@Composable
fun EmptyState(mark: Mark, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(40.dp),
    ) {
        MarkIcon(mark, Sweepy.colors.inkMuted.copy(alpha = 0.6f), size = 88.dp, strokeWidth = 0.05f)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Sweepy.colors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Sweepy.colors.inkMuted, textAlign = TextAlign.Center)
    }
}

// ---- Deck presentation --------------------------------------------------------------------------

@Composable
fun deckTitle(source: DeckSource): String = when (source) {
    DeckSource.All -> stringResource(R.string.deck_all)
    DeckSource.Screenshots -> stringResource(R.string.deck_screenshots)
    DeckSource.WhatsApp -> stringResource(R.string.deck_whatsapp)
    DeckSource.LargeVideos -> stringResource(R.string.deck_large_videos)
    DeckSource.Later -> stringResource(R.string.deck_later)
    is DeckSource.Folder -> source.name.ifBlank { stringResource(R.string.folder_unnamed) }
    is DeckSource.Month -> formatYearMonth(source.yearMonth)
}

fun formatYearMonth(yearMonth: Int, short: Boolean = false): String {
    val ym = YearMonth.of(yearMonth / 100, yearMonth % 100)
    val style = if (short) JavaTextStyle.SHORT else JavaTextStyle.FULL
    return ym.month.getDisplayName(style, Locale.getDefault()) + " " + ym.year
}

fun formatCount(count: Number): String = NumberFormat.getIntegerInstance().format(count)
