package io.github.darkxace01.sweepy.ui.trash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.TrashedItem
import io.github.darkxace01.sweepy.ui.TrashUiState
import io.github.darkxace01.sweepy.ui.components.BottomActionArea
import io.github.darkxace01.sweepy.ui.components.EmptyState
import io.github.darkxace01.sweepy.ui.components.IconBadge
import io.github.darkxace01.sweepy.ui.components.MediaThumbnail
import io.github.darkxace01.sweepy.ui.components.PrimaryButton
import io.github.darkxace01.sweepy.ui.components.ScreenTopBar
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.DeleteRed
import java.util.concurrent.TimeUnit

/** Items in Android's trash: restore them, or delete them for good to free the space right away. */
@Composable
fun TrashScreen(
    state: TrashUiState,
    onBack: () -> Unit,
    onRestore: (List<TrashedItem>) -> Unit,
    onDeleteForever: (List<TrashedItem>) -> Unit,
    now: Long = System.currentTimeMillis(),
) {
    val context = LocalContext.current
    var selectedIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Drop selections for items that are gone (restored/deleted).
    val present = remember(state.items) { state.items.mapTo(HashSet()) { it.media.id } }
    val selected = state.items.filter { it.media.id in selectedIds && it.media.id in present }
    val allSelected = state.items.isNotEmpty() && selected.size == state.items.size

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.fillMaxSize()) {
                ScreenTopBar(
                    title = stringResource(R.string.trash_title),
                    onBack = onBack,
                    actions = {
                        if (state.items.isNotEmpty()) {
                            TextButton(onClick = {
                                selectedIds = if (allSelected) emptySet() else state.items.mapTo(HashSet()) { it.media.id }
                            }) {
                                Text(stringResource(if (allSelected) R.string.select_none else R.string.select_all))
                            }
                        }
                    },
                )

                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    state.items.isEmpty() -> EmptyState(
                        icon = painterResource(R.drawable.ic_restore),
                        tint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.trash_empty_title),
                        body = stringResource(R.string.trash_empty_body),
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "info") {
                            InfoCard(size = formatSize(context, state.totalBytes), count = state.items.size)
                        }
                        items(state.items, key = { it.media.id }) { item ->
                            val id = item.media.id
                            TrashThumbnail(
                                item = item,
                                now = now,
                                selected = id in selectedIds,
                                onToggle = { selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }

            if (selected.isNotEmpty()) {
                BottomActionArea(Modifier.align(Alignment.BottomCenter)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(
                            text = pluralStringResource(R.plurals.trash_restore, selected.size, formatCount(selected.size)),
                            onClick = { onRestore(selected) },
                            icon = painterResource(R.drawable.ic_restore),
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryButton(
                            text = stringResource(R.string.trash_delete_forever),
                            onClick = { confirmDelete = true },
                            background = DeleteRed.copy(alpha = 0.14f),
                            contentColor = DeleteRed,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = DeleteRed) },
            title = { Text(stringResource(R.string.trash_confirm_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.trash_confirm_body,
                        selected.size,
                        formatCount(selected.size),
                        formatSize(context, selected.sumOf { it.media.sizeBytes }),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteForever(selected)
                }) { Text(stringResource(R.string.trash_delete_forever), color = DeleteRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun InfoCard(size: String, count: Int) {
    Column(
        modifier = Modifier
            .padding(start = 4.dp, end = 4.dp, bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(painterResource(R.drawable.ic_restore), MaterialTheme.colorScheme.primary, size = 52.dp)
            Column(Modifier.padding(start = 16.dp)) {
                Text(size, style = MaterialTheme.typography.headlineSmall)
                Text(
                    pluralStringResource(R.plurals.trash_summary, count, formatCount(count)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.trash_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrashThumbnail(
    item: TrashedItem,
    now: Long,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val daysLeft = TimeUnit.MILLISECONDS.toDays((item.expiresAtMillis - now).coerceAtLeast(0)).toInt()
    val scale by animateFloatAsState(if (selected) 0.88f else 1f, label = "selectScale")
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .semantics { this.selected = selected }
            .pressable(onToggle, pressedScale = 0.94f, role = Role.Checkbox),
    ) {
        MediaThumbnail(
            item.media,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(if (selected) Modifier.border(3.dp, primary, RoundedCornerShape(16.dp)) else Modifier),
            cornerRadius = 16.dp,
            showBadge = false,
        )
        Text(
            text = pluralStringResource(R.plurals.days_left, daysLeft, daysLeft),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(CircleShape)
                .background(if (daysLeft <= 3) DeleteRed else Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) primary else Color.Black.copy(alpha = 0.3f))
                .border(2.dp, Color.White, CircleShape),
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}
