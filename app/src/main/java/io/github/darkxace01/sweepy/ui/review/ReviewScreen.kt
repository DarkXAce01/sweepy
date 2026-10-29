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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.ReviewUiState
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
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.fillMaxSize()) {
                ScreenTopBar(
                    title = stringResource(R.string.review_title),
                    onBack = onBack,
                    actions = {
                        if (state.count > 0) {
                            TextButton(onClick = onUnmarkAll) { Text(stringResource(R.string.review_keep_all)) }
                        }
                    },
                )

                if (state.count == 0) {
                    EmptyState(
                        icon = rememberVectorPainter(Icons.Filled.Delete),
                        tint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.review_empty_title),
                        body = stringResource(R.string.review_empty_body),
                    )
                    return@Column
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "summary") {
                        SummaryCard(
                            count = state.count,
                            size = formatSize(context, state.bytes),
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                        )
                    }
                    state.groups.forEach { group ->
                        item(span = { GridItemSpan(maxLineSpan) }, key = "header-${group.folder}") {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 8.dp),
                            ) {
                                Text(
                                    group.folder ?: stringResource(R.string.folder_unnamed),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    stringResource(
                                        R.string.count_and_size,
                                        formatCount(group.items.size),
                                        formatSize(context, group.bytes),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(group.items, key = { it.id }) { item ->
                            ReviewThumbnail(
                                item = item,
                                onClick = { onUnmark(item) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }

            if (state.count > 0) {
                BottomActionArea(Modifier.align(Alignment.BottomCenter)) {
                    PrimaryButton(
                        text = pluralStringResource(
                            R.plurals.review_commit,
                            state.count,
                            formatCount(state.count),
                            formatSize(context, state.bytes),
                        ),
                        onClick = onCommit,
                        background = DeleteRed,
                        contentColor = Color.White,
                        icon = rememberVectorPainter(Icons.Filled.Delete),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.review_restore_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(count: Int, size: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(18.dp),
    ) {
        IconBadge(rememberVectorPainter(Icons.Filled.Delete), DeleteRed, size = 52.dp)
        Column(Modifier.padding(start = 16.dp)) {
            Text(size, style = MaterialTheme.typography.headlineSmall)
            Text(
                pluralStringResource(R.plurals.review_summary, count, formatCount(count)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReviewThumbnail(item: MediaItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.review_unmark) + ": " + item.displayName.orEmpty()
    Box(
        modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .pressable(onClick, pressedScale = 0.92f)
            .semantics { contentDescription = label },
    ) {
        MediaThumbnail(item, Modifier.fillMaxSize(), cornerRadius = 16.dp)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}
