package io.github.darkxace01.sweepy.ui.review

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.ReviewUiState
import io.github.darkxace01.sweepy.ui.components.BottomActionArea
import io.github.darkxace01.sweepy.ui.components.EmptyState
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.Hairline
import io.github.darkxace01.sweepy.ui.components.InkButton
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.MediaThumbnail
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.ScreenTopBar
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.Sweepy

/** Everything swiped left. Nothing is deleted until the user taps the button at the bottom. */
@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onBack: () -> Unit,
    onUnmark: (MediaItem) -> Unit,
    onUnmarkAll: () -> Unit,
    onCommit: () -> Unit,
) {
    val context = LocalContext.current
    var confirmKeepAll by remember { mutableStateOf(false) }
    Paper {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.fillMaxSize()) {
                ScreenTopBar(
                    title = stringResource(R.string.review_title),
                    eyebrow = stringResource(R.string.review_eyebrow),
                    onBack = onBack,
                    actions = {
                        if (state.count > 0) {
                            Eyebrow(
                                stringResource(R.string.review_keep_all),
                                color = Sweepy.colors.ink,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .pressable({ confirmKeepAll = true })
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                            )
                        }
                    },
                )

                if (state.count == 0) {
                    EmptyState(
                        mark = Mark.CROSS,
                        title = stringResource(R.string.review_empty_title),
                        body = stringResource(R.string.review_empty_body),
                    )
                    return@Column
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 96.dp),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 150.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "summary") {
                        Summary(state.count, formatSize(context, state.bytes), Modifier.padding(horizontal = 4.dp))
                    }
                    state.groups.forEach { group ->
                        item(span = { GridItemSpan(maxLineSpan) }, key = "header-${group.folder}") {
                            Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 8.dp)) {
                                Hairline()
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                                    Eyebrow(
                                        group.folder ?: stringResource(R.string.folder_unnamed),
                                        color = Sweepy.colors.ink,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Eyebrow(
                                        stringResource(R.string.count_and_size, formatCount(group.items.size), formatSize(context, group.bytes)),
                                        small = true,
                                    )
                                }
                            }
                        }
                        items(group.items, key = { it.id }) { item ->
                            ReviewThumbnail(item = item, onClick = { onUnmark(item) }, modifier = Modifier.animateItem())
                        }
                    }
                }
            }

            if (state.count > 0) {
                BottomActionArea(Modifier.align(Alignment.BottomCenter)) {
                    InkButton(
                        text = pluralStringResource(R.plurals.review_commit, state.count, formatCount(state.count)),
                        trailing = formatSize(context, state.bytes),
                        onClick = onCommit,
                        color = Sweepy.colors.pencil,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.review_restore_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Sweepy.colors.inkMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    if (confirmKeepAll) {
        AlertDialog(
            onDismissRequest = { confirmKeepAll = false },
            containerColor = Sweepy.colors.surface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    pluralStringResource(R.plurals.confirm_keep_all_title, state.count, formatCount(state.count)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Sweepy.colors.ink,
                )
            },
            text = {
                Text(stringResource(R.string.confirm_keep_all_body), style = MaterialTheme.typography.bodyMedium, color = Sweepy.colors.inkMuted)
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmKeepAll = false
                    onUnmarkAll()
                }) { Text(stringResource(R.string.review_keep_all), color = Sweepy.colors.ink) }
            },
            dismissButton = {
                TextButton(onClick = { confirmKeepAll = false }) { Text(stringResource(R.string.cancel), color = Sweepy.colors.ink) }
            },
        )
    }
}

@Composable
private fun Summary(count: Int, size: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(size, style = MaterialTheme.typography.displayMedium, color = Sweepy.colors.ink)
        Text(
            pluralStringResource(R.plurals.review_summary, count, formatCount(count)),
            style = MaterialTheme.typography.headlineSmall,
            fontStyle = FontStyle.Italic,
            color = Sweepy.colors.inkMuted,
        )
    }
}

/** A contact-sheet frame with a grease-pencil cross through it. Tap to keep it after all. */
@Composable
private fun ReviewThumbnail(item: MediaItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.review_unmark) + ": " + item.displayName.orEmpty()
    Box(
        modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(Sweepy.colors.print)
            .pressable(onClick, pressedScale = 0.92f)
            .padding(3.dp)
            .semantics { contentDescription = label },
    ) {
        MediaThumbnail(item, Modifier.fillMaxSize(), cornerRadius = 3.dp)
        Box(
            Modifier
                .fillMaxSize()
                .padding(10.dp)
                .pencilMark(Mark.CROSS, Sweepy.colors.pencil.copy(alpha = 0.9f), { 1f }, strokeWidth = 0.07f),
        )
    }
}
