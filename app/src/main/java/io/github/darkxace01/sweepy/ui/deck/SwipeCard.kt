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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.QuietIconButton
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.LocalAppSettings
import io.github.darkxace01.sweepy.ui.formatDate
import io.github.darkxace01.sweepy.ui.formatDuration
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

enum class SwipeDirection(val decision: Decision, val mark: Mark) {
    LEFT(Decision.DELETE, Mark.CROSS),
    RIGHT(Decision.KEEP, Mark.CIRCLE),
    UP(Decision.LATER, Mark.QUESTION),
    DOWN(Decision.FAVORITE, Mark.STAR),
    ;

    companion object {
        fun of(decision: Decision): SwipeDirection = entries.first { it.decision == decision }
    }
}

/** The grease-pencil color for each decision. */
val SwipeDirection.color: Color
    @Composable @ReadOnlyComposable get() = when (this) {
        SwipeDirection.LEFT -> Sweepy.colors.pencil
        SwipeDirection.RIGHT -> Sweepy.colors.keep
        SwipeDirection.UP -> Sweepy.colors.later
        SwipeDirection.DOWN -> Sweepy.colors.favorite
    }

@Composable
fun SwipeDirection.label(): String = stringResource(
    when (this) {
        SwipeDirection.LEFT -> R.string.label_delete
        SwipeDirection.RIGHT -> R.string.label_keep
        SwipeDirection.UP -> R.string.label_later
        SwipeDirection.DOWN -> R.string.label_favorite
    },
)

/** How far the card must travel (as a fraction of its size) before letting go counts as a swipe. */
private const val HORIZONTAL_THRESHOLD = 0.3f
private const val VERTICAL_THRESHOLD = 0.2f

/** A quick flick counts as a swipe even if the card hasn't travelled far. In px per second. */
private const val FLING_VELOCITY = 1800f

val CardShape = RoundedCornerShape(14.dp)
private val PhotoShape = RoundedCornerShape(5.dp)

/**
 * Position and animation of the top card. Shared with the buttons so they can "throw" the card too.
 * [initialOffset] lets a card start off-screen and fly back in (used by undo).
 * [mirrored] is left-handed mode: dragging right means [SwipeDirection.LEFT] (delete) and vice versa.
 */
@Stable
class SwipeCardState(initialOffset: Offset = Offset.Zero, val mirrored: Boolean = false) {
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
                val direction = if ((x > 0) != mirrored) SwipeDirection.RIGHT else SwipeDirection.LEFT
                direction to min(1f, abs(x) / (size.width * HORIZONTAL_THRESHOLD))
            } else {
                val direction = if (y > 0) SwipeDirection.DOWN else SwipeDirection.UP
                direction to min(1f, abs(y) / (size.height * VERTICAL_THRESHOLD))
            }
        }

    /** How far the mark for [direction] is drawn (0..1). It stays complete while the card flies away. */
    fun markProgress(direction: SwipeDirection): Float {
        if (isFlyingOut) return if (flownDirection == direction) 1f else 0f
        return lean?.takeIf { it.first == direction }?.second ?: 0f
    }

    private var flownDirection: SwipeDirection? = null

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
                if ((vx > 0) != mirrored) SwipeDirection.RIGHT else SwipeDirection.LEFT
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
        flownDirection = direction
        isFlyingOut = true
        offset.animateTo(offscreenOffset(direction, size, offset.value, mirrored), tween(durationMillis = 260, easing = FastOutLinearInEasing))
    }
}

/** Where a card ends up after being swiped in [direction]. */
fun offscreenOffset(direction: SwipeDirection, size: IntSize, from: Offset = Offset.Zero, mirrored: Boolean = false): Offset {
    val w = size.width * 1.4f * if (mirrored) -1f else 1f
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
    val hapticsOn = LocalAppSettings.current.haptics
    LaunchedEffect(committed) {
        if (committed != null && hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val (x, y) = state.offset.value
                val w = state.size.width.coerceAtLeast(1)
                val h = state.size.height.coerceAtLeast(1)
                translationX = x
                translationY = y
                rotationZ = x / w * 10f
                // A slight tilt in depth, like a print lifted off the table by one corner.
                rotationY = -x / w * 9f
                rotationX = y / h * 7f
                cameraDistance = 14f * density
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
        PrintCard(item = item, onInfo = onInfo, playVideo = LocalAppSettings.current.autoplayVideos, zoomable = true, modifier = Modifier.fillMaxSize()) {
            PencilOverlay(state)
        }
    }
}

/**
 * The grease-pencil mark drawn over the photo while it's dragged: it draws itself in step with
 * the finger, so letting go halfway leaves half a cross. Reads drag state only while drawing.
 */
@Composable
private fun PencilOverlay(state: SwipeCardState) {
    val colors = SwipeDirection.entries.associateWith { it.color }
    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                // A faint wash of the mark's color, like light through a gel.
                val lean = state.lean ?: return@drawWithContent
                drawRect(colors.getValue(lean.first).copy(alpha = 0.16f * lean.second))
            },
    ) {
        // Cross and circle span the whole frame, like marks on a contact sheet.
        for (direction in listOf(SwipeDirection.LEFT, SwipeDirection.RIGHT)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .pencilMark(direction.mark, colors.getValue(direction), { state.markProgress(direction) }, strokeWidth = 0.032f),
            )
        }
        // The question mark and star sit in the middle.
        for (direction in listOf(SwipeDirection.UP, SwipeDirection.DOWN)) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.5f)
                    .aspectRatio(1f)
                    .pencilMark(direction.mark, colors.getValue(direction), { state.markProgress(direction) }, strokeWidth = 0.075f),
            )
        }
        for (direction in SwipeDirection.entries) {
            // The label sits on the side the card is moving away from.
            val alignment = when (direction) {
                SwipeDirection.LEFT -> if (state.mirrored) Alignment.TopStart else Alignment.TopEnd
                SwipeDirection.RIGHT -> if (state.mirrored) Alignment.TopEnd else Alignment.TopStart
                SwipeDirection.UP -> Alignment.BottomCenter
                SwipeDirection.DOWN -> Alignment.TopCenter
            }
            val tilt = when (direction) {
                SwipeDirection.LEFT -> 6f
                SwipeDirection.RIGHT -> -6f
                SwipeDirection.UP -> -3f
                SwipeDirection.DOWN -> 3f
            }
            Text(
                direction.label(),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, letterSpacing = 1.5.sp),
                color = Sweepy.colors.print,
                modifier = Modifier
                    .align(alignment)
                    .padding(20.dp)
                    .graphicsLayer {
                        val p = state.markProgress(direction)
                        alpha = ((p - 0.25f) / 0.5f).coerceIn(0f, 1f)
                        rotationZ = tilt
                        val s = 0.85f + 0.15f * p
                        scaleX = s
                        scaleY = s
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.getValue(direction))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/**
 * A photo presented as a print: matte border with a caption "chin" carrying its folder, date and
 * size. Also used for the cards waiting underneath. [overlay] is drawn on top of the photo only.
 */
@Composable
fun PrintCard(
    item: MediaItem,
    onInfo: (() -> Unit)?,
    modifier: Modifier = Modifier,
    playVideo: Boolean = false,
    zoomable: Boolean = false,
    overlay: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val colors = Sweepy.colors
    Column(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = CardShape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = if (colors.isDark) 0.6f else 0.28f),
            )
            .clip(CardShape)
            .background(colors.print)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(PhotoShape)
                .background(Color(0xFF14130F)),
        ) {
            // A blurred, zoomed copy fills the space around portrait/landscape photos.
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.45f }
                    .blur(40.dp),
            )
            Box(if (zoomable) Modifier.fillMaxSize().pinchToPeek() else Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                if (item.isVideo && playVideo) VideoPreview(item, Modifier.fillMaxSize())
            }
            overlay()
        }

        // The caption chin.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Eyebrow(item.folder ?: stringResource(R.string.info_unknown), small = true)
                Spacer(Modifier.height(1.dp))
                Text(
                    formatDate(item.dateTakenMillis),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp, lineHeight = 24.sp),
                    fontStyle = FontStyle.Italic,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Eyebrow(
                    if (item.isVideo) "▶ " + formatDuration(item.durationMillis) else fileType(item),
                    small = true,
                    color = colors.ink,
                )
                Spacer(Modifier.height(3.dp))
                Eyebrow(formatSize(context, item.sizeBytes), small = true)
            }
            if (onInfo != null) {
                Spacer(Modifier.width(2.dp))
                QuietIconButton(stringResource(R.string.action_info), onInfo) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

/** "JPG", "PNG", "HEIC"… from the MIME type. */
private fun fileType(item: MediaItem): String =
    item.mimeType?.substringAfter('/')?.substringBefore(';')?.let {
        when (it.lowercase()) {
            "jpeg" -> "JPG"
            "quicktime" -> "MOV"
            "x-matroska" -> "MKV"
            else -> it.uppercase()
        }
    } ?: ""
