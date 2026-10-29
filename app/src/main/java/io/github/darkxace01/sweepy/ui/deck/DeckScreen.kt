package io.github.darkxace01.sweepy.ui.deck

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.DeckUiState
import io.github.darkxace01.sweepy.ui.components.CircleIconButton
import io.github.darkxace01.sweepy.ui.components.ProgressTrack
import io.github.darkxace01.sweepy.ui.components.deckTitle
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.DeleteRed
import io.github.darkxace01.sweepy.ui.theme.FavoritePink
import io.github.darkxace01.sweepy.ui.theme.KeepGreen
import io.github.darkxace01.sweepy.ui.theme.LaterAmber
import kotlinx.coroutines.launch

/** How many cards are drawn in the stack (the top one plus the ones peeking out underneath). */
private const val VISIBLE_CARDS = 3

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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = state.current
    var stackSize by remember { mutableStateOf(IntSize.Zero) }
    // Set by undo: the next card to appear flies in from the side it was swiped to.
    var enterFrom by remember { mutableStateOf<SwipeDirection?>(null) }
    var infoItem by remember { mutableStateOf<MediaItem?>(null) }

    val cardState = remember(current?.id) {
        SwipeCardState(initialOffset = enterFrom?.let { offscreenOffset(it, stackSize) } ?: Offset.Zero)
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

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TopBar(
                title = deckTitle(source),
                subtitle = when {
                    state.loading -> ""
                    current == null -> stringResource(R.string.deck_all_done)
                    else -> stringResource(
                        R.string.left_and_size,
                        formatCount(state.deck.size),
                        formatSize(context, state.remainingBytes),
                    )
                },
                markedCount = state.markedCount,
                onBack = onBack,
                onOpenReview = onOpenReview,
            )
            if (source != DeckSource.Later) {
                ProgressTrack(
                    progress = state.progress,
                    height = 4.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )
            }

            // The card stack, with a colored glow behind it while dragging.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .drawBehind {
                        val lean = cardState.lean ?: return@drawBehind
                        drawCircle(
                            Brush.radialGradient(
                                listOf(lean.first.color.copy(alpha = 0.32f * lean.second), Color.Transparent),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.maxDimension * 0.7f,
                            ),
                            radius = size.maxDimension * 0.7f,
                        )
                    }
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 28.dp)
                    .onSizeChanged { stackSize = it },
            ) {
                when {
                    state.loading -> CircularProgressIndicator()
                    current == null -> DoneState(source, state, onOpenReview, onBack)
                    else -> {
                        // Cards underneath, drawn back to front. They move up as the top card is dragged away.
                        val upcoming = state.deck.subList(1, minOf(state.deck.size, VISIBLE_CARDS))
                        upcoming.asReversed().forEachIndexed { reversedIndex, item ->
                            val depth = upcoming.size - reversedIndex // 1 = right under the top card
                            key(item.id) {
                                MediaCard(
                                    item = item,
                                    onInfo = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val p = cardState.dragProgress
                                            val d = depth - p
                                            val s = 1f - 0.05f * d
                                            scaleX = s
                                            scaleY = s
                                            // Undo the shrink at the bottom edge, then peek out a little further per level.
                                            translationY = size.height * (1f - s) / 2f + 12.dp.toPx() * d
                                        },
                                )
                            }
                        }
                        key(current.id) {
                            SwipeCard(
                                item = current,
                                state = cardState,
                                onSwiped = { direction ->
                                    if (showHint) onDismissHint()
                                    onDecide(current, direction.decision)
                                },
                                onInfo = { infoItem = current },
                                modifier = Modifier.fillMaxSize().onSizeChanged { cardState.size = it },
                            )
                        }
                        if (showHint) SwipeHint(onDismiss = onDismissHint)
                    }
                }
            }

            if (current != null || state.loading) ActionBar(
                enabled = current != null && !state.loading,
                canUndo = state.canUndo,
                cardState = cardState,
                onSwipe = ::swipe,
                onUndo = ::undo,
            )
        }
    }

    infoItem?.let { item -> InfoSheet(item = item, onDismiss = { infoItem = null }) }
}

@Composable
private fun TopBar(
    title: String,
    subtitle: String,
    markedCount: Int,
    onBack: () -> Unit,
    onOpenReview: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        CircleIconButton(
            icon = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        BinButton(markedCount, onOpenReview)
    }
}

@Composable
private fun BinButton(count: Int, onClick: () -> Unit) {
    val active = count > 0
    val label = stringResource(R.string.review_title)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(if (active) DeleteRed.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .pressable(onClick, pressedScale = 0.92f)
            .padding(horizontal = 14.dp)
            .semantics { contentDescription = label },
    ) {
        Icon(
            Icons.Filled.Delete,
            contentDescription = null,
            tint = if (active) DeleteRed else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            },
            label = "binCount",
        ) { value ->
            Text(
                formatCount(value),
                style = MaterialTheme.typography.labelLarge,
                color = if (active) DeleteRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
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
    // The button matching the drag direction lights up as the card is dragged.
    fun emphasis(direction: SwipeDirection): () -> Float = {
        cardState.lean?.takeIf { it.first == direction }?.second ?: 0f
    }
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
    ) {
        ActionButton(
            icon = painterResource(R.drawable.ic_undo),
            label = stringResource(R.string.action_undo),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 50.dp,
            enabled = canUndo,
            emphasis = { 0f },
            onClick = onUndo,
        )
        ActionButton(
            icon = rememberVectorPainter(Icons.Filled.Close),
            label = stringResource(R.string.action_delete),
            color = DeleteRed,
            size = 72.dp,
            enabled = enabled,
            emphasis = emphasis(SwipeDirection.LEFT),
            onClick = { onSwipe(SwipeDirection.LEFT) },
        )
        ActionButton(
            icon = painterResource(R.drawable.ic_schedule),
            label = stringResource(R.string.action_later),
            color = LaterAmber,
            size = 50.dp,
            enabled = enabled,
            emphasis = emphasis(SwipeDirection.UP),
            onClick = { onSwipe(SwipeDirection.UP) },
        )
        ActionButton(
            icon = rememberVectorPainter(Icons.Filled.Check),
            label = stringResource(R.string.action_keep),
            color = KeepGreen,
            size = 72.dp,
            enabled = enabled,
            emphasis = emphasis(SwipeDirection.RIGHT),
            onClick = { onSwipe(SwipeDirection.RIGHT) },
        )
        ActionButton(
            icon = rememberVectorPainter(Icons.Filled.Favorite),
            label = stringResource(R.string.action_favorite),
            color = FavoritePink,
            size = 50.dp,
            enabled = enabled,
            emphasis = emphasis(SwipeDirection.DOWN),
            onClick = { onSwipe(SwipeDirection.DOWN) },
        )
    }
}

@Composable
private fun ActionButton(
    icon: Painter,
    label: String,
    color: Color,
    size: Dp,
    enabled: Boolean,
    emphasis: () -> Float,
    onClick: () -> Unit,
) {
    val container = MaterialTheme.colorScheme.surfaceContainer
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val e = emphasis()
                scaleX = 1f + 0.14f * e
                scaleY = 1f + 0.14f * e
                alpha = if (enabled) 1f else 0.35f
            }
            .clip(CircleShape)
            .drawBehind {
                drawRect(container)
                drawRect(color.copy(alpha = 0.12f + 0.88f * emphasis()))
            }
            .border(1.5.dp, color.copy(alpha = 0.3f), CircleShape)
            .pressable(onClick, enabled = enabled, pressedScale = 0.88f)
            .semantics { contentDescription = label },
    ) {
        val iconSize = size * 0.42f
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize).graphicsLayer { alpha = 1f - emphasis() })
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize).graphicsLayer { alpha = emphasis() })
    }
}

/** First-run overlay explaining the four swipe directions. */
@Composable
private fun SwipeHint(onDismiss: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(CardShape)
            .background(Color.Black.copy(alpha = 0.62f))
            .pressable(onDismiss, pressedScale = 1f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            HintChip("↑  " + stringResource(R.string.action_later), LaterAmber)
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HintChip("←  " + stringResource(R.string.action_delete), DeleteRed)
                Spacer(Modifier.width(16.dp))
                HintChip(stringResource(R.string.action_keep) + "  →", KeepGreen)
            }
            Spacer(Modifier.height(20.dp))
            HintChip("↓  " + stringResource(R.string.action_favorite), FavoritePink)
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.hint_title), color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.hint_body),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HintChip(text: String, color: Color) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun DoneState(source: DeckSource, state: DeckUiState, onOpenReview: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(visible, enter = fadeIn() + scaleIn(initialScale = 0.85f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(KeepGreen, Color(0xFF0E9E6E)))),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.done_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (source == DeckSource.Later) {
                    stringResource(R.string.done_body_later)
                } else {
                    pluralStringResource(R.plurals.done_body, state.totalCount, formatCount(state.totalCount))
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            if (state.markedCount > 0) {
                WideButton(
                    text = pluralStringResource(
                        R.plurals.done_review,
                        state.markedCount,
                        formatCount(state.markedCount),
                        formatSize(context, state.markedBytes),
                    ),
                    background = DeleteRed,
                    content = Color.White,
                    onClick = onOpenReview,
                )
                Spacer(Modifier.height(10.dp))
            }
            WideButton(
                text = stringResource(R.string.done_back),
                background = MaterialTheme.colorScheme.surfaceContainerHigh,
                content = MaterialTheme.colorScheme.onSurface,
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun WideButton(text: String, background: Color, content: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .pressable(onClick),
    ) {
        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}
