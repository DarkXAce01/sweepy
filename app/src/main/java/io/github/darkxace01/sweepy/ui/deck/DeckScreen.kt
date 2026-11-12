package io.github.darkxace01.sweepy.ui.deck

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.DeckFilter
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.ReelCard
import io.github.darkxace01.sweepy.ui.DeckUiState
import io.github.darkxace01.sweepy.ui.LocalAppSettings
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.InkButton
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.MarkIcon
import io.github.darkxace01.sweepy.ui.components.OutlineButton
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.QuietIconButton
import io.github.darkxace01.sweepy.ui.components.MediaThumbnail
import io.github.darkxace01.sweepy.ui.components.ThinProgress
import io.github.darkxace01.sweepy.ui.components.chunky
import io.github.darkxace01.sweepy.ui.components.deckTitle
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.Pixnix
import kotlinx.coroutines.launch

/** How many cards are drawn in the pile (the top one plus the ones underneath). */
private const val VISIBLE_CARDS = 3

/** A small, stable, per-photo angle so the pile looks dropped on a table rather than stacked by a machine. */
private fun pileAngle(id: Long): Float {
    val h = (id * 2654435761L) ushr 7
    val a = (h % 1000L).toFloat() / 1000f * 9f - 4.5f
    return if (a in -1.5f..1.5f) a + if (a < 0) -1.5f else 1.5f else a
}

@Composable
fun DeckScreen(
    source: DeckSource,
    state: DeckUiState,
    showHint: Boolean,
    onBack: () -> Unit,
    onDecide: (MediaItem, Decision) -> Unit,
    onUndo: () -> Decision?,
    onOpenReview: () -> Unit,
    onDismissHint: () -> Unit,
    onFilterChange: (DeckFilter) -> Unit = {},
    /** The reel under the pile was scrolled or tapped to this photo: show it on top. */
    onMoveTo: (MediaItem) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = state.current
    var stackSize by remember { mutableStateOf(IntSize.Zero) }
    // Set by undo: the next card to appear flies in from the side it was swiped to.
    var enterFrom by remember { mutableStateOf<SwipeDirection?>(null) }
    var infoItem by remember { mutableStateOf<MediaItem?>(null) }
    var showFilter by remember { mutableStateOf(false) }

    val mirrored = LocalAppSettings.current.leftHanded
    val cardState = remember(current?.id, mirrored) {
        SwipeCardState(
            initialOffset = enterFrom?.let { offscreenOffset(it, stackSize, mirrored = mirrored) } ?: Offset.Zero,
            mirrored = mirrored,
        )
    }
    LaunchedEffect(cardState) {
        if (cardState.offset.value != Offset.Zero) cardState.snapBack()
        enterFrom = null
    }

    fun swipe(direction: SwipeDirection) {
        val item = current ?: return
        if (cardState.isFlyingOut) return
        if (showHint) onDismissHint()
        scope.launch {
            cardState.flyOut(direction)
            onDecide(item, direction.decision)
        }
    }

    fun undo() {
        onUndo()?.let { enterFrom = SwipeDirection.of(it) }
    }

    val glowColors = SwipeDirection.entries.associateWith { it.color }

    Paper {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TopBar(
                title = deckTitle(source),
                eyebrow = when {
                    state.loading -> ""
                    current == null -> stringResource(R.string.deck_all_done)
                    else -> stringResource(R.string.left_and_size, formatCount(state.deck.size), formatSize(context, state.remainingBytes))
                },
                markedCount = state.markedCount,
                filterActive = state.isFiltered,
                onBack = onBack,
                onOpenFilter = { showFilter = true },
                onOpenReview = onOpenReview,
            )
            if (source != DeckSource.Later) {
                ThinProgress(state.progress, modifier = Modifier.padding(horizontal = 22.dp))
            }

            // The pile, with a soft colored light underneath while dragging.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .drawBehind {
                        val lean = cardState.lean ?: return@drawBehind
                        val radius = size.maxDimension * 0.65f
                        drawCircle(
                            Brush.radialGradient(
                                listOf(glowColors.getValue(lean.first).copy(alpha = 0.22f * lean.second), Color.Transparent),
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                        )
                    }
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 14.dp)
                    .onSizeChanged { stackSize = it },
            ) {
                when {
                    state.loading -> CircularProgressIndicator(color = Pixnix.colors.ink, strokeWidth = 1.5.dp, modifier = Modifier.size(28.dp))
                    current == null -> DoneState(source, state, onOpenReview, onBack, onShowAll = { onFilterChange(state.defaultFilter) })
                    else -> {
                        // Prints underneath, drawn back to front. They straighten up and come forward
                        // as the top one is dragged away.
                        val upcoming = state.underneath.take(VISIBLE_CARDS - 1)
                        upcoming.asReversed().forEachIndexed { reversedIndex, item ->
                            val depth = upcoming.size - reversedIndex // 1 = right under the top card
                            key(item.id) {
                                val angle = pileAngle(item.id)
                                PrintCard(
                                    item = item,
                                    onInfo = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val d = depth - cardState.dragProgress
                                            val s = 1f - 0.035f * d
                                            scaleX = s
                                            scaleY = s
                                            rotationZ = angle * d
                                            translationY = 6.dp.toPx() * d
                                        },
                                )
                            }
                        }
                        key(current.id) {
                            SwipeCard(
                                item = current,
                                state = cardState,
                                existing = state.currentDecision,
                                onSwiped = { direction ->
                                    if (showHint) onDismissHint()
                                    onDecide(current, direction.decision)
                                },
                                onInfo = { infoItem = current },
                                modifier = Modifier.fillMaxSize().onSizeChanged { cardState.size = it },
                            )
                        }
                        if (showHint) SwipeHint(mirrored, onDismiss = onDismissHint)
                    }
                }
            }

            if (current != null && state.reel.isNotEmpty()) {
                DeckReel(
                    reel = state.reel,
                    position = state.position,
                    onMoveTo = { if (!cardState.isFlyingOut) onMoveTo(it) },
                )
            }
            if (current != null || state.loading) {
                ActionBar(
                    enabled = current != null && !state.loading,
                    canUndo = state.canUndo,
                    cardState = cardState,
                    onSwipe = ::swipe,
                    onUndo = ::undo,
                )
            }
        }
    }

    infoItem?.let { item -> InfoSheet(item = item, onDismiss = { infoItem = null }) }
    if (showFilter) FilterSheet(state, onFilterChange, onDismiss = { showFilter = false })
}

@Composable
private fun TopBar(
    title: String,
    eyebrow: String,
    markedCount: Int,
    filterActive: Boolean,
    onBack: () -> Unit,
    onOpenFilter: () -> Unit,
    onOpenReview: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
    ) {
        QuietIconButton(stringResource(R.string.back), onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Pixnix.colors.ink)
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Eyebrow(eyebrow, small = true)
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = Pixnix.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FilterButton(filterActive, onOpenFilter)
        Spacer(Modifier.width(8.dp))
        BinChip(markedCount, onOpenReview)
    }
}

/** The review bin, as a small outlined tag with a running count. */
@Composable
private fun BinChip(count: Int, onClick: () -> Unit) {
    val active = count > 0
    val label = stringResource(R.string.review_title)
    val color = if (active) Pixnix.colors.pencil else Pixnix.colors.inkMuted
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .chunky(RoundedCornerShape(10.dp), fill = Pixnix.colors.surface, offset = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (active) color.copy(alpha = 0.45f) else Pixnix.colors.hairline, RoundedCornerShape(10.dp))
            .pressable(onClick, pressedScale = 0.92f)
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = label },
    ) {
        MarkIcon(Mark.CROSS, color, size = 14.dp, strokeWidth = 0.14f)
        Spacer(Modifier.width(8.dp))
        Eyebrow(stringResource(R.string.bin_short), color = color)
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            },
            label = "binCount",
        ) { value ->
            Eyebrow(formatCount(value), color = Pixnix.colors.ink, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun ActionBar(
    enabled: Boolean,
    canUndo: Boolean,
    cardState: SwipeCardState,
    onSwipe: (SwipeDirection) -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
    ) {
        // Buttons sit on the same side as their swipe, so left-handed mode swaps delete and keep.
        val (start, end) = if (cardState.mirrored) SwipeDirection.RIGHT to SwipeDirection.LEFT else SwipeDirection.LEFT to SwipeDirection.RIGHT
        UndoButton(canUndo, onUndo)
        MarkButton(start, 60.dp, enabled, cardState, onSwipe)
        MarkButton(SwipeDirection.UP, 46.dp, enabled, cardState, onSwipe)
        MarkButton(end, 60.dp, enabled, cardState, onSwipe)
        MarkButton(SwipeDirection.DOWN, 46.dp, enabled, cardState, onSwipe)
    }
}

private val FRAME_WIDTH = 44.dp
private val FRAME_HEIGHT = 58.dp

/**
 * The reel under the pile, like a film strip: every photo waiting in this deck plus the ones swiped
 * this visit (with their marks), in order. The frame in the middle is the photo on top of the pile.
 * Scroll it and the pile follows; swipe a photo and the reel moves on by itself. Tap any frame to go
 * straight there. Moving along never decides anything.
 */
@Composable
private fun DeckReel(reel: List<ReelCard>, position: Int, onMoveTo: (MediaItem) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(72.dp)) {
        val side = ((maxWidth - FRAME_WIDTH) / 2).coerceAtLeast(0.dp)
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = position.coerceAtLeast(0))
        val snapping = rememberSnapFlingBehavior(listState, SnapPosition.Center)
        val dragged by listState.interactionSource.collectIsDraggedAsState()
        // True from the moment a finger moves the reel until it comes to rest: the reel leads, the pile follows.
        var leading by remember { mutableStateOf(false) }
        LaunchedEffect(dragged) { if (dragged) leading = true }
        val currentReel by rememberUpdatedState(reel)
        val move by rememberUpdatedState(onMoveTo)
        LaunchedEffect(listState) {
            snapshotFlow { listState.centeredIndex() to listState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { (index, scrolling) ->
                    if (leading && index != null) currentReel.getOrNull(index)?.let { move(it.item) }
                    if (!scrolling && !dragged) leading = false
                }
        }
        // Otherwise the pile leads: after a swipe, undo or tap, bring that frame to the middle.
        LaunchedEffect(position) {
            if (!leading && position >= 0 && listState.centeredIndex() != position) listState.animateScrollToItem(position)
        }
        val moveLabel = stringResource(R.string.strip_jump)
        LazyRow(
            state = listState,
            flingBehavior = snapping,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = side),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(reel, key = { _, card -> card.item.id }) { index, card ->
                val onShow = index == position
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(FRAME_WIDTH, FRAME_HEIGHT)
                        .pressable({ move(card.item) }, pressedScale = 0.9f)
                        .semantics { contentDescription = moveLabel },
                ) {
                    val decided = card.decision?.let(SwipeDirection::of)
                    MediaThumbnail(
                        card.item,
                        Modifier.fillMaxSize().graphicsLayer { alpha = if (decided != null && !onShow) 0.45f else 1f },
                        cornerRadius = 6.dp,
                        showBadge = false,
                    )
                    if (decided != null) MarkIcon(decided.mark, decided.color, size = 26.dp, strokeWidth = 0.13f)
                    if (onShow) Box(Modifier.fillMaxSize().border(2.dp, Pixnix.colors.ink, RoundedCornerShape(6.dp)))
                }
            }
        }
    }
}

/** The frame nearest the middle of the reel. */
private fun LazyListState.centeredIndex(): Int? {
    val info = layoutInfo
    val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
}

@Composable
private fun UndoButton(enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.action_undo)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.3f }
            .clip(CircleShape)
            .pressable(onClick, enabled = enabled, pressedScale = 0.85f)
            .semantics { contentDescription = label },
    ) {
        Icon(painterResource(R.drawable.ic_undo), contentDescription = null, tint = Pixnix.colors.ink, modifier = Modifier.size(22.dp))
    }
}

/** A round button carrying the hand-drawn mark; it fills with the mark's color as the card leans its way. */
@Composable
private fun MarkButton(
    direction: SwipeDirection,
    size: Dp,
    enabled: Boolean,
    cardState: SwipeCardState,
    onSwipe: (SwipeDirection) -> Unit,
) {
    val color = direction.color
    val print = Pixnix.colors.print
    val hairline = Pixnix.colors.hairline
    val label = stringResource(
        when (direction) {
            SwipeDirection.LEFT -> R.string.action_delete
            SwipeDirection.RIGHT -> R.string.action_keep
            SwipeDirection.UP -> R.string.action_later
            SwipeDirection.DOWN -> R.string.action_favorite
        },
    )
    val emphasis = { cardState.lean?.takeIf { it.first == direction }?.second ?: 0f }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val e = emphasis()
                scaleX = 1f + 0.12f * e
                scaleY = 1f + 0.12f * e
                alpha = if (enabled) 1f else 0.35f
            }
            .chunky(CircleShape)
            .clip(CircleShape)
            .drawBehind {
                drawRect(print)
                drawRect(color.copy(alpha = emphasis()))
                drawCircle(hairline, radius = this.size.minDimension / 2 - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
            }
            .pressable({ onSwipe(direction) }, enabled = enabled, pressedScale = 0.88f)
            .semantics { contentDescription = label },
    ) {
        val markSize = size * 0.46f
        MarkIcon(direction.mark, color, size = markSize, strokeWidth = 0.11f, modifier = Modifier.graphicsLayer { alpha = 1f - emphasis() })
        MarkIcon(direction.mark, print, size = markSize, strokeWidth = 0.11f, modifier = Modifier.graphicsLayer { alpha = emphasis() })
    }
}

private val OnScrim = Color(0xFFF2EFE8)

/** First-run overlay: the four directions, noted at the edges of the print like grease-pencil notes. */
@Composable
private fun SwipeHint(mirrored: Boolean, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CardShape)
            .background(Color(0xFF12110F).copy(alpha = 0.8f))
            .pressable(onDismiss, pressedScale = 1f)
            .padding(18.dp),
    ) {
        HintLabel(SwipeDirection.UP, "↑", Modifier.align(Alignment.TopCenter))
        HintLabel(if (mirrored) SwipeDirection.RIGHT else SwipeDirection.LEFT, "←", Modifier.align(Alignment.CenterStart), onStart = true)
        HintLabel(if (mirrored) SwipeDirection.LEFT else SwipeDirection.RIGHT, "→", Modifier.align(Alignment.CenterEnd), onEnd = true)
        HintLabel(SwipeDirection.DOWN, "↓", Modifier.align(Alignment.BottomCenter))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 80.dp),
        ) {
            Text(
                stringResource(R.string.hint_title),
                color = OnScrim,
                style = MaterialTheme.typography.headlineLarge,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.hint_body),
                color = OnScrim.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HintLabel(direction: SwipeDirection, arrow: String, modifier: Modifier, onStart: Boolean = false, onEnd: Boolean = false) {
    val alignment = when {
        onStart -> Alignment.Start
        onEnd -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    Column(horizontalAlignment = alignment, modifier = modifier) {
        MarkIcon(direction.mark, direction.color, size = 30.dp, strokeWidth = 0.1f)
        Eyebrow(
            if (onEnd) "${direction.label()} $arrow" else "$arrow ${direction.label()}",
            color = OnScrim,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DoneState(source: DeckSource, state: DeckUiState, onOpenReview: () -> Unit, onBack: () -> Unit, onShowAll: () -> Unit) {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val draw = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(durationMillis = 900, delayMillis = 150, easing = FastOutSlowInEasing)) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(170.dp)) {
            Text(
                stringResource(R.string.done_title),
                style = MaterialTheme.typography.displaySmall,
                fontStyle = FontStyle.Italic,
                color = Pixnix.colors.ink,
                textAlign = TextAlign.Center,
            )
            // A loop drawn around the words, the way a keeper gets circled on a contact sheet.
            Box(
                Modifier
                    .fillMaxWidth(0.88f)
                    .height(150.dp)
                    .pencilMark(Mark.CIRCLE, Pixnix.colors.keep, { draw.value }, strokeWidth = 0.035f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (state.hiddenCount > 0) {
                pluralStringResource(R.plurals.done_body_filtered, state.hiddenCount, formatCount(state.hiddenCount))
            } else if (source == DeckSource.Later) {
                stringResource(R.string.done_body_later)
            } else {
                pluralStringResource(R.plurals.done_body, state.totalCount, formatCount(state.totalCount))
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Pixnix.colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(32.dp))
        if (state.hiddenCount > 0) {
            InkButton(text = stringResource(R.string.filter_show_all), onClick = onShowAll, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
        if (state.markedCount > 0) {
            InkButton(
                text = pluralStringResource(R.plurals.done_review, state.markedCount, formatCount(state.markedCount)),
                trailing = formatSize(context, state.markedBytes),
                color = Pixnix.colors.pencil,
                contentColor = Pixnix.colors.onPencil,
                onClick = onOpenReview,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
        OutlineButton(stringResource(R.string.done_back), onBack, Modifier.fillMaxWidth())
    }
}
