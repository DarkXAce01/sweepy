package io.github.darkxace01.sweepy.ui.trash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.TrashedItem
import io.github.darkxace01.sweepy.ui.TrashUiState
import io.github.darkxace01.sweepy.ui.components.BottomActionArea
import io.github.darkxace01.sweepy.ui.components.EmptyState
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.InkButton
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.MediaThumbnail
import io.github.darkxace01.sweepy.ui.components.OutlineButton
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.ScreenTopBar
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import java.util.concurrent.TimeUnit

/** Android's own trash: what Sweepy (or anything else) recently deleted, restorable until it expires. */
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

    Paper {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.fillMaxSize()) {
                ScreenTopBar(
                    title = stringResource(R.string.trash_title),
                    eyebrow = stringResource(R.string.trash_eyebrow),
                    onBack = onBack,
                    actions = {
                        if (state.items.isNotEmpty()) {
                            Eyebrow(
                                stringResource(if (allSelected) R.string.select_none else R.string.select_all),
                                color = Sweepy.colors.ink,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .pressable({
                                        selectedIds = if (allSelected) emptySet() else state.items.mapTo(HashSet()) { it.media.id }
                                    })
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                            )
                        }
                    },
                )

                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Sweepy.colors.ink, strokeWidth = 1.5.dp)
                    }
                    state.items.isEmpty() -> EmptyState(
                        mark = Mark.CIRCLE,
                        title = stringResource(R.string.trash_empty_title),
                        body = stringResource(R.string.trash_empty_body),
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 150.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "info") {
                            Summary(size = formatSize(context, state.totalBytes), count = state.items.size)
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
                        InkButton(
                            text = pluralStringResource(R.plurals.trash_restore, selected.size, formatCount(selected.size)),
                            onClick = { onRestore(selected) },
                            modifier = Modifier.weight(1f),
                        )
                        OutlineButton(
                            text = stringResource(R.string.trash_delete_forever),
                            onClick = { confirmDelete = true },
                            contentColor = Sweepy.colors.pencil,
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
            containerColor = Sweepy.colors.surface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    stringResource(R.string.trash_confirm_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Sweepy.colors.ink,
                )
            },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.trash_confirm_body,
                        selected.size,
                        formatCount(selected.size),
                        formatSize(context, selected.sumOf { it.media.sizeBytes }),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sweepy.colors.inkMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteForever(selected)
                }) { Text(stringResource(R.string.trash_delete_forever), color = Sweepy.colors.pencil) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel), color = Sweepy.colors.ink) }
            },
        )
    }
}

@Composable
private fun Summary(size: String, count: Int) {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 18.dp)) {
        Text(size, style = MaterialTheme.typography.displayMedium, color = Sweepy.colors.ink)
        Text(
            pluralStringResource(R.plurals.trash_summary, count, formatCount(count)),
            style = MaterialTheme.typography.headlineSmall,
            fontStyle = FontStyle.Italic,
            color = Sweepy.colors.inkMuted,
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.trash_explainer), style = MaterialTheme.typography.bodySmall, color = Sweepy.colors.inkMuted)
    }
}

/** A frame you can circle (select). Shows how long until Android deletes it for good. */
@Composable
private fun TrashThumbnail(
    item: TrashedItem,
    now: Long,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val daysLeft = TimeUnit.MILLISECONDS.toDays((item.expiresAtMillis - now).coerceAtLeast(0)).toInt()
    val inspection = LocalInspectionMode.current
    val circle by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (inspection) tween(0) else tween(durationMillis = if (selected) 380 else 160),
        label = "circle",
    )
    val urgent = daysLeft <= 3
    Box(
        modifier
            .aspectRatio(1f)
            .semantics { this.selected = selected }
            .pressable(onToggle, pressedScale = 0.94f, role = Role.Checkbox)
            .pencilMark(Mark.CIRCLE, Sweepy.colors.keep, { circle }, strokeWidth = 0.045f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(9.dp)
                .graphicsLayer {
                    val s = 1f - 0.08f * circle
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(6.dp))
                .background(Sweepy.colors.print)
                .padding(3.dp),
        ) {
            MediaThumbnail(item.media, Modifier.fillMaxSize(), cornerRadius = 3.dp, showBadge = false)
            Eyebrow(
                pluralStringResource(R.plurals.days_left, daysLeft, daysLeft),
                small = true,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (urgent) Sweepy.colors.pencil else Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}
