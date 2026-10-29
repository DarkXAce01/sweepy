package io.github.darkxace01.sweepy.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
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

enum class SwipeDirection(val decision: Decision) {
    LEFT(Decision.DELETE),
    RIGHT(Decision.KEEP),
    UP(Decision.LATER),
    DOWN(Decision.FAVORITE),
}

/** How far the card must travel (as a fraction of its size) before letting go counts as a swipe. */
private const val HORIZONTAL_THRESHOLD = 0.28f
private const val VERTICAL_THRESHOLD = 0.18f

/** Position and animation of the top card. Shared with the buttons so they can "throw" the card too. */
@Stable
class SwipeCardState {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    var size by mutableStateOf(IntSize.Zero)
    var isFlyingOut by mutableStateOf(false)
        private set

    /** The direction the card is leaning, and how close it is to the threshold (0..1). */
    val lean: Pair<SwipeDirection, Float>?
        get() {
            if (size == IntSize.Zero) return null
            val (x, y) = offset.value
            if (x == 0f && y == 0f) return null
            return if (abs(x) >= abs(y)) {
                val dir = if (x > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                dir to min(1f, abs(x) / (size.width * HORIZONTAL_THRESHOLD))
            } else {
                val dir = if (y > 0) SwipeDirection.DOWN else SwipeDirection.UP
                dir to min(1f, abs(y) / (size.height * VERTICAL_THRESHOLD))
            }
        }

    /** The direction the card would be swiped if released now, or null if it would snap back. */
    val committedDirection: SwipeDirection?
        get() = lean?.takeIf { it.second >= 1f }?.first

    suspend fun dragBy(delta: Offset) = offset.snapTo(offset.value + delta)

    suspend fun snapBack() = offset.animateTo(
        Offset.Zero,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    )

    suspend fun flyOut(direction: SwipeDirection) {
        isFlyingOut = true
        val w = size.width * 1.5f
        val h = size.height * 1.5f
        val current = offset.value
        val target = when (direction) {
            SwipeDirection.LEFT -> Offset(-w, current.y)
            SwipeDirection.RIGHT -> Offset(w, current.y)
            SwipeDirection.UP -> Offset(current.x, -h)
            SwipeDirection.DOWN -> Offset(current.x, h)
        }
        offset.animateTo(target, tween(durationMillis = 220))
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
        if (committed != null && !state.isFlyingOut) {
            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { state.size = it }
            .graphicsLayer {
                translationX = state.offset.value.x
                translationY = state.offset.value.y
                rotationZ = if (state.size.width > 0) state.offset.value.x / state.size.width * 12f else 0f
            }
            .pointerInput(state) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { state.dragBy(dragAmount) }
                    },
                    onDragEnd = {
                        val direction = state.committedDirection
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
            },
    ) {
        MediaCard(item = item, onInfo = onInfo, modifier = Modifier.fillMaxSize())
        state.lean?.let { (direction, progress) -> SwipeLabel(direction, progress) }
    }
}

/** The photo/video with its folder, date and size. Also used for the card peeking out underneath. */
@Composable
fun MediaCard(item: MediaItem, onInfo: (() -> Unit)?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        if (item.isVideo) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(formatDuration(item.durationMillis), color = Color.White, fontSize = 13.sp)
            }
        }

        if (onInfo != null) {
            FilledTonalIconButton(
                onClick = onInfo,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.action_info))
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp),
        ) {
            Text(
                text = item.folder ?: stringResource(R.string.info_unknown),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${formatDate(item.dateTakenMillis)} · ${formatSize(context, item.sizeBytes)}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SwipeLabel(direction: SwipeDirection, progress: Float) {
    val (textRes, color, alignment) = when (direction) {
        SwipeDirection.LEFT -> Triple(R.string.label_delete, DeleteRed, Alignment.TopEnd)
        SwipeDirection.RIGHT -> Triple(R.string.label_keep, KeepGreen, Alignment.TopStart)
        SwipeDirection.UP -> Triple(R.string.label_later, LaterAmber, Alignment.BottomCenter)
        SwipeDirection.DOWN -> Triple(R.string.label_favorite, FavoritePink, Alignment.TopCenter)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = progress }
            .clip(RoundedCornerShape(24.dp))
            .background(color.copy(alpha = 0.25f))
            .padding(32.dp),
    ) {
        Text(
            text = stringResource(textRes),
            color = color,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(alignment)
                .graphicsLayer { rotationZ = if (direction == SwipeDirection.LEFT) 12f else -12f }
                .border(4.dp, color, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
