package io.github.darkxace01.sweepy.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.ui.DeckSummary
import io.github.darkxace01.sweepy.ui.HomeUiState
import io.github.darkxace01.sweepy.ui.components.CircleIconButton
import io.github.darkxace01.sweepy.ui.components.IconBadge
import io.github.darkxace01.sweepy.ui.components.MediaThumbnail
import io.github.darkxace01.sweepy.ui.components.ProgressTrack
import io.github.darkxace01.sweepy.ui.components.deckColor
import io.github.darkxace01.sweepy.ui.components.deckIcon
import io.github.darkxace01.sweepy.ui.components.deckTitle
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.DeleteRed
import io.github.darkxace01.sweepy.ui.theme.KeepGreen
import io.github.darkxace01.sweepy.ui.theme.Violet
import io.github.darkxace01.sweepy.ui.theme.VioletDeep
import io.github.darkxace01.sweepy.ui.theme.VioletNight
import kotlin.math.roundToInt

private const val COLLAPSED_FOLDERS = 5
private const val COLLAPSED_MONTHS = 4

@Composable
fun HomeScreen(
    state: HomeUiState,
    partialAccess: Boolean,
    onOpenDeck: (DeckSource) -> Unit,
    onOpenReview: () -> Unit,
    onOpenTrash: () -> Unit,
    onRequestFullAccess: () -> Unit,
) {
    var showAllFolders by rememberSaveable { mutableStateOf(false) }
    var showAllMonths by rememberSaveable { mutableStateOf(false) }
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp + insets.calculateStartPadding(layoutDirection),
                end = 20.dp + insets.calculateEndPadding(layoutDirection),
                top = insets.calculateTopPadding() + 8.dp,
                bottom = insets.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TopBar(onOpenTrash) }

            if (partialAccess) {
                item { PartialAccessBanner(onRequestFullAccess) }
            }

            item { HeroCard(state, onStart = { onOpenDeck(DeckSource.All) }) }

            if (!state.loading && state.totalCount > 0) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            icon = rememberVectorPainter(Icons.Filled.Delete),
                            tint = DeleteRed,
                            value = formatCount(state.markedCount),
                            label = stringResource(R.string.home_stat_bin),
                            detail = formatSize(LocalContext.current, state.markedBytes),
                            onClick = onOpenReview,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            icon = painterResource(R.drawable.ic_sparkle),
                            tint = KeepGreen,
                            value = formatSize(LocalContext.current, state.stats.cleanedBytes),
                            label = stringResource(R.string.home_stat_cleaned),
                            detail = pluralStringResource(
                                R.plurals.items_count,
                                state.stats.cleanedCount.toInt(),
                                formatCount(state.stats.cleanedCount),
                            ),
                            onClick = onOpenTrash,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (state.quickDecks.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.home_section_quick)) }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            // Let tiles scroll edge to edge while staying aligned with the page.
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .layoutBleed(20.dp),
                        ) {
                            items(state.quickDecks, key = { it.source.toString() }) { summary ->
                                QuickDeckTile(summary, onClick = { onOpenDeck(summary.source) })
                            }
                        }
                    }
                }

                if (state.folders.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.home_section_folders),
                            action = if (state.folders.size > COLLAPSED_FOLDERS) {
                                if (showAllFolders) stringResource(R.string.show_less) else stringResource(R.string.show_all, state.folders.size)
                            } else {
                                null
                            },
                            onAction = { showAllFolders = !showAllFolders },
                        )
                    }
                    val folders = if (showAllFolders) state.folders else state.folders.take(COLLAPSED_FOLDERS)
                    items(folders, key = { "folder-" + it.source.toString() }) { summary ->
                        DeckRow(summary, onClick = { onOpenDeck(summary.source) })
                    }
                }

                if (state.months.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.home_section_months),
                            action = if (state.months.size > COLLAPSED_MONTHS) {
                                if (showAllMonths) stringResource(R.string.show_less) else stringResource(R.string.show_all, state.months.size)
                            } else {
                                null
                            },
                            onAction = { showAllMonths = !showAllMonths },
                        )
                    }
                    val months = if (showAllMonths) state.months else state.months.take(COLLAPSED_MONTHS)
                    items(months, key = { "month-" + it.source.toString() }) { summary ->
                        DeckRow(summary, onClick = { onOpenDeck(summary.source) })
                    }
                }

                item { Spacer(Modifier.height(4.dp)) }
                item { RecentlyCleanedRow(onOpenTrash) }
            }

            item { PrivacyNote() }
        }
    }
}

/** Lets a horizontally scrolling row extend into the page's side padding. */
private fun Modifier.layoutBleed(bleed: Dp): Modifier = this.layout { measurable, constraints ->
    val extra = bleed.roundToPx() * 2
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.minWidth + extra))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

@Composable
private fun TopBar(onOpenTrash: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(Violet, VioletDeep))),
        ) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 10.dp).weight(1f),
        )
        CircleIconButton(
            icon = painterResource(R.drawable.ic_restore),
            contentDescription = stringResource(R.string.trash_title),
            onClick = onOpenTrash,
        )
    }
}

@Composable
private fun PartialAccessBanner(onRequestFullAccess: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Text(
            stringResource(R.string.partial_access),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRequestFullAccess) { Text(stringResource(R.string.partial_access_action)) }
    }
}

@Composable
private fun HeroCard(state: HomeUiState, onStart: () -> Unit) {
    val context = LocalContext.current
    val onHero = Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(Violet, VioletDeep, VioletNight), start = Offset.Zero, end = Offset.Infinite))
            .drawBehind {
                // Soft decorative circles.
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.width * 0.42f, center = Offset(size.width * 0.95f, size.height * 0.05f))
                drawCircle(Color.White.copy(alpha = 0.05f), radius = size.width * 0.3f, center = Offset(size.width * 0.8f, size.height * 1.05f))
            }
            .padding(24.dp),
    ) {
        if (state.loading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp)) {
                CircularProgressIndicator(color = onHero, trackColor = onHero.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.home_scanning), color = onHero.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            }
            return@Box
        }

        Column {
            Text(stringResource(R.string.home_your_gallery), color = onHero.copy(alpha = 0.75f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatCount(state.totalCount), color = onHero, style = MaterialTheme.typography.displayMedium)
                Text(
                    " " + pluralStringResource(R.plurals.items_word, state.totalCount),
                    color = onHero.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
            Text(
                stringResource(R.string.home_on_phone, formatSize(context, state.totalBytes)),
                color = onHero.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium,
            )

            if (state.totalCount > 0) {
                Spacer(Modifier.height(22.dp))
                ProgressTrack(
                    progress = state.progress,
                    color = onHero,
                    trackColor = onHero.copy(alpha = 0.2f),
                    height = 8.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Text(
                        stringResource(R.string.home_reviewed_pct, (state.progress * 100).roundToInt()),
                        color = onHero,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.home_to_go, formatCount(state.totalCount - state.reviewedCount)),
                        color = onHero.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.height(22.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(onHero)
                        .pressable(onStart),
                ) {
                    Text(
                        stringResource(if (state.reviewedCount == 0) R.string.home_start else R.string.home_continue),
                        color = VioletDeep,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = VioletDeep, modifier = Modifier.size(20.dp))
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.home_empty), color = onHero, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: Painter,
    tint: Color,
    value: String,
    label: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .pressable(onClick)
            .padding(16.dp),
    ) {
        IconBadge(icon, tint, size = 36.dp)
        Spacer(Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 2.dp).height(36.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = onAction) { Text(action, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun QuickDeckTile(summary: DeckSummary, onClick: () -> Unit) {
    val context = LocalContext.current
    val color = deckColor(summary.source)
    val isLater = summary.source == DeckSource.Later
    Column(
        modifier = Modifier
            .width(158.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .pressable(onClick)
            .padding(16.dp),
    ) {
        IconBadge(painterResource(deckIcon(summary.source)), color, size = 42.dp)
        Spacer(Modifier.height(14.dp))
        Text(deckTitle(summary.source), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(
            text = when {
                summary.remainingCount == 0 -> stringResource(R.string.deck_all_done)
                isLater -> pluralStringResource(R.plurals.waiting_count, summary.remainingCount, formatCount(summary.remainingCount))
                else -> stringResource(
                    R.string.left_and_size,
                    formatCount(summary.remainingCount),
                    formatSize(context, summary.totalBytes),
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(14.dp))
        ProgressTrack(
            progress = if (isLater) 0f else summary.progress,
            color = color,
            height = 5.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DeckRow(summary: DeckSummary, onClick: () -> Unit) {
    val context = LocalContext.current
    val done = summary.remainingCount == 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .pressable(onClick, pressedScale = 0.98f)
            .padding(10.dp),
    ) {
        val cover = summary.cover
        if (cover != null) {
            MediaThumbnail(cover, Modifier.size(58.dp), cornerRadius = 16.dp, showBadge = false)
        } else {
            IconBadge(painterResource(deckIcon(summary.source)), deckColor(summary.source), size = 58.dp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(deckTitle(summary.source), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(
                    R.string.count_and_size,
                    pluralStringResource(R.plurals.items_count, summary.totalCount, formatCount(summary.totalCount)),
                    formatSize(context, summary.totalBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))
            ProgressTrack(
                progress = summary.progress,
                color = if (done) KeepGreen else MaterialTheme.colorScheme.primary,
                height = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = if (done) "✓" else "${(summary.progress * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = if (done) KeepGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp).padding(end = 4.dp),
        )
    }
}

@Composable
private fun RecentlyCleanedRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .pressable(onClick, pressedScale = 0.98f)
            .padding(16.dp),
    ) {
        IconBadge(painterResource(R.drawable.ic_restore), MaterialTheme.colorScheme.primary, size = 42.dp)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(stringResource(R.string.trash_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.home_trash_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_shield),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}
