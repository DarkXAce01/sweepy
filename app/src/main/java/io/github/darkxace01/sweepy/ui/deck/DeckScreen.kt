package io.github.darkxace01.sweepy.ui.deck

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.DeleteRed
import io.github.darkxace01.sweepy.ui.theme.FavoritePink
import io.github.darkxace01.sweepy.ui.theme.KeepGreen
import io.github.darkxace01.sweepy.ui.theme.LaterAmber
import kotlinx.coroutines.launch

@Composable
fun DeckScreen(
    state: DeckUiState,
    partialAccess: Boolean,
    onDecide: (MediaItem, Decision) -> Unit,
    onUndo: () -> Unit,
    onOpenReview: () -> Unit,
    onRequestFullAccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = state.current
    // A fresh card state for every new top card.
    val cardState = remember(current?.id) { SwipeCardState() }
    var infoItem by remember { mutableStateOf<MediaItem?>(null) }

    fun swipe(direction: SwipeDirection) {
        val item = current ?: return
        if (cardState.isFlyingOut) return
        scope.launch {
            cardState.flyOut(direction)
            onDecide(item, direction.decision)
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
        ) {
            // Header: app name, items left, and the review bin button.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                    if (!state.loading) {
                        val remaining = pluralStringResource(R.plurals.deck_remaining, state.deck.size, state.deck.size)
                        val freed = stringResource(R.string.deck_freed, formatSize(context, state.trashedBytes))
                        Text(
                            text = if (state.trashedBytes > 0) "$remaining · $freed" else remaining,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                BadgedBox(badge = { if (state.marked.isNotEmpty()) Badge { Text("${state.marked.size}") } }) {
                    TextButton(onClick = onOpenReview) {
                        Text(
                            if (state.marked.isEmpty()) {
                                stringResource(R.string.deck_review)
                            } else {
                                pluralStringResource(
                                    R.plurals.deck_marked,
                                    state.marked.size,
                                    state.marked.size,
                                    formatSize(context, state.markedBytes),
                                )
                            },
                        )
                    }
                }
            }

            if (partialAccess) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            stringResource(R.string.deck_partial_access),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onRequestFullAccess) { Text(stringResource(R.string.deck_partial_access_action)) }
                    }
                }
            }

            // The card stack.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                when {
                    state.loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.deck_loading))
                    }
                    current == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.deck_empty_title), style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.deck_empty_body), textAlign = TextAlign.Center)
                    }
                    else -> {
                        state.next?.let { next ->
                            MediaCard(
                                item = next,
                                onInfo = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = 0.94f
                                        scaleY = 0.94f
                                        translationY = 24.dp.toPx()
                                    },
                            )
                        }
                        SwipeCard(
                            item = current,
                            state = cardState,
                            onSwiped = { direction -> onDecide(current, direction.decision) },
                            onInfo = { infoItem = current },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // Action buttons, an alternative to swiping (and better for accessibility).
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                val enabled = current != null
                IconButton(onClick = onUndo, enabled = state.canUndo) {
                    Icon(painterResource(R.drawable.ic_undo), contentDescription = stringResource(R.string.action_undo))
                }
                BigActionButton(enabled, DeleteRed, { swipe(SwipeDirection.LEFT) }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.action_delete), Modifier.size(32.dp))
                }
                SmallActionButton(enabled, LaterAmber, { swipe(SwipeDirection.UP) }) {
                    Icon(painterResource(R.drawable.ic_schedule), stringResource(R.string.action_later))
                }
                BigActionButton(enabled, KeepGreen, { swipe(SwipeDirection.RIGHT) }) {
                    Icon(Icons.Filled.Check, stringResource(R.string.action_keep), Modifier.size(32.dp))
                }
                SmallActionButton(enabled, FavoritePink, { swipe(SwipeDirection.DOWN) }) {
                    Icon(Icons.Filled.Favorite, stringResource(R.string.action_favorite))
                }
            }
        }
    }

    infoItem?.let { item -> InfoSheet(item = item, onDismiss = { infoItem = null }) }
}

@Composable
private fun BigActionButton(enabled: Boolean, color: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = color, contentColor = Color.White),
        modifier = Modifier.size(68.dp),
        content = content,
    )
}

@Composable
private fun SmallActionButton(
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit,
    size: Dp = 48.dp,
    content: @Composable () -> Unit,
) {
    OutlinedIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = color),
        modifier = Modifier.size(size),
        content = content,
    )
}
