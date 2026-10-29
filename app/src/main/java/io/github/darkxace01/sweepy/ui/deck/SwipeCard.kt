package io.github.darkxace01.sweepy.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.formatDate
import io.github.darkxace01.sweepy.ui.formatDuration
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.DeleteRed
import io.github.darkxace01.sweepy.ui.theme.FavoritePink
import io.github.darkxace01.sweepy.ui.theme.KeepGreen
import io.github.darkxace01.sweepy.ui.theme.LaterAmber
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

enum class SwipeDirection(val decision: Decision, val color: Color) {
    LEFT(Decision.DELETE, DeleteRed),
    RIGHT(Decision.KEEP, KeepGreen),
    UP(Decision.LATER, LaterAmber),
    DOWN(Decision.FAVORITE, FavoritePink),
    ;

    companion object {
        fun of(decision: Decision): SwipeDirection = entries.first { it.decision == decision }
    }
}

/** How far the card must travel (as a fraction of its size) before letting go counts as a swipe. */
private const val HORIZONTAL_THRESHOLD = 0.3f
private const val VERTICAL_THRESHOLD = 0.2f

/** A quick flick counts as a swipe even if the card hasn't travelled far. In px per second. */
private const val FLING_VELOCITY = 1800f

val CardShape = RoundedCornerShape(30.dp)

/**
 * Position and animation of the top card. Shared with the buttons so they can "throw" the card too.
 * [initialOffset] lets a card start off-screen and fly back in (used by undo).
 */
@Stable
class SwipeCardState(initialOffset: Offset = Offset.Zero) {
    val offset = Animatable(initialOffset, Offset.VectorConverter)
    var size by mutableStateOf(IntSize.Zero)
    var isFlyingOut by mutableStateOf(false)
        private set

    /** The direction the card is leaning, and how close it is to the threshold (0..1). */
    val lean: Pair<SwipeDirection, Float>?
        get() {
            if (size == IntSize.Zero || isFlyingOut) return null
            val (x, y) = offset.value
            if (x == 0f && y == 0f) return null
            return if (abs(x) >= abs(y)) {
                val direction = if (x > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                direction to min(1f, abs(x) / (size.width * HORIZONTAL_THRESHOLD))
            } else {
                val direction = if (y > 0) SwipeDirection.DOWN else SwipeDirection.UP
                direction to min(1f, abs(y) / (size.height * VERTICAL_THRESHOLD))
            }
        }

    /** 0..1: how far the top card has moved, used to bring the cards underneath forward. */
    val dragProgress: Float
        get() = if (isFlyingOut) 1f else lean?.second ?: 0f

    /** The direction the card would be swiped if released now, or null if it would snap back. */
    val committedDirection: SwipeDirection?
        get() = lean?.takeIf { it.second >= 1f }?.first

    fun directionForRelease(velocity: Offset): SwipeDirection? {
        committedDirection?.let { return it }
        val (vx, vy) = velocity
        val (x, y) = offset.value
        return when {
            abs(vx) > abs(vy) && abs(vx) > FLING_VELOCITY && vx * x > 0 ->
                if (vx > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
            abs(vy) > abs(vx) && abs(vy) > FLING_VELOCITY && vy * y > 0 ->
                if (vy > 0) SwipeDirection.DOWN else SwipeDirection.UP
            else -> null
        }
    }

    suspend fun dragBy(delta: Offset) = offset.snapTo(offset.value + delta)

    suspend fun snapBack() = offset.animateTo(
        Offset.Zero,
        spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
    )

    suspend fun flyOut(direction: SwipeDirection) {
        isFlyingOut = true
        offset.animateTo(offscreenOffset(direction, size, offset.value), tween(durationMillis = 240, easing = FastOutLinearInEasing))
    }
}

/** Where a card ends up after being swiped in [direction]. */
fun offscreenOffset(direction: SwipeDirection, size: IntSize, from: Offset = Offset.Zero): Offset {
    val w = size.width * 1.4f
    val h = size.height * 1.3f
    return when (direction) {
        SwipeDirection.LEFT -> Offset(-w, from.y + size.height * 0.05f)
        SwipeDirection.RIGHT -> Offset(w, from.y + size.height * 0.05f)
        SwipeDirection.UP -> Offset(from.x, -h)
        SwipeDirection.DOWN -> Offset(from.x, h)
    }
}

@Composable
fun SwipeCard(
    item: MediaItem,
    state: SwipeCardState,
    onSwiped: (SwipeDirection) -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Buzz once when the card crosses the "let go to swipe" threshold.
    val committed by remember(state) { derivedStateOf { state.committedDirection } }
    LaunchedEffect(committed) {
        if (committed != null) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = state.offset.value.x
                translationY = state.offset.value.y
                rotationZ = if (state.size.width > 0) state.offset.value.x / state.size.width * 14f else 0f
            }
            .pointerInput(state) {
                val tracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = { tracker.resetTracking() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        scope.launch { state.dragBy(dragAmount) }
                    },
                    onDragEnd = {
                        val velocity = tracker.calculateVelocity()
                        val direction = state.directionForRelease(Offset(velocity.x, velocity.y))
                        scope.launch {
                            if (direction != null) {
                                state.flyOut(direction)
                                onSwiped(direction)
                            } else {
                                state.snapBack()
                            }
                        }
                    },
                    onDragCancel = { scope.launch { state.snapBack() } },
                )
            }
            .pointerInput(item.id) { detectTapGestures(onDoubleTap = { onInfo() }) },
    ) {
        MediaCard(item = item, onInfo = onInfo, modifier = Modifier.fillMaxSize())
        SwipeOverlay(state)
    }
}

/** Color wash and "stamp" that appear as the card is dragged. Reads state only while drawing. */
@Composable
private fun SwipeOverlay(state: SwipeCardState) {
    val lean = state.lean ?: return
    val (direction, progress) = lean
    val (label, icon) = when (direction) {
        SwipeDirection.LEFT -> stringResource(R.string.label_delete) to rememberVectorPainter(Icons.Filled.Close)
        SwipeDirection.RIGHT -> stringResource(R.string.label_keep) to rememberVectorPainter(Icons.Filled.Check)
        SwipeDirection.UP -> stringResource(R.string.label_later) to painterResource(R.drawable.ic_schedule)
        SwipeDirection.DOWN -> stringResource(R.string.label_favorite) to rememberVectorPainter(Icons.Filled.Favorite)
    }
    val alignment = when (direction) {
        SwipeDirection.LEFT -> Alignment.TopEnd
        SwipeDirection.RIGHT -> Alignment.TopStart
        SwipeDirection.UP -> Alignment.BottomCenter
        SwipeDirection.DOWN -> Alignment.TopCenter
    }
    val tilt = when (direction) {
        SwipeDirection.LEFT -> 14f
        SwipeDirection.RIGHT -> -14f
        else -> 0f
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(CardShape)
            .background(
                Brush.radialGradient(
                    listOf(direction.color.copy(alpha = 0.45f * progress), direction.color.copy(alpha = 0.12f * progress)),
                ),
            )
            .padding(28.dp),
    ) {
        Stamp(
            label = label,
            icon = icon,
            color = direction.color,
            modifier = Modifier
                .align(alignment)
                .padding(top = if (direction == SwipeDirection.DOWN) 0.dp else 8.dp, bottom = if (direction == SwipeDirection.UP) 60.dp else 0.dp)
                .graphicsLayer {
                    alpha = min(1f, progress * 1.4f)
                    val s = 0.7f + 0.3f * progress
                    scaleX = s
                    scaleY = s
                    rotationZ = tilt
                },
        )
    }
}

@Composable
private fun Stamp(label: String, icon: Painter, color: Color, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.headlineSmall)
    }
}

/** The photo/video with its folder, date and size. Also used for the cards waiting underneath. */
@Composable
fun MediaCard(item: MediaItem, onInfo: (() -> Unit)?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(Color(0xFF0E0D13)),
    ) {
        // Blurred, zoomed copy of the photo fills the empty space around portrait/landscape images.
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.55f }
                .blur(48.dp),
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        // Bottom info with a gradient so text is readable on any photo.
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    icon = painterResource(if (item.isVideo) R.drawable.ic_videocam else R.drawable.ic_folder),
                    text = item.folder ?: stringResource(R.string.info_unknown),
                )
                if (item.isVideo) {
                    Spacer(Modifier.width(6.dp))
                    Pill(icon = null, text = formatDuration(item.durationMillis))
                }
            }
            Text(
                text = "${formatDate(item.dateTakenMillis)}  ·  ${formatSize(context, item.sizeBytes)}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (onInfo != null) {
            val infoLabel = stringResource(R.string.action_info)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pressable(onInfo, pressedScale = 0.88f)
                    .semantics { contentDescription = infoLabel },
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun Pill(icon: Painter?, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
