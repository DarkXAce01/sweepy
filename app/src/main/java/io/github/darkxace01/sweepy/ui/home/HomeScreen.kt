package io.github.darkxace01.sweepy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.DeckSummary
import io.github.darkxace01.sweepy.ui.HomeUiState
import io.github.darkxace01.sweepy.ui.components.CountUp
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.Hairline
import io.github.darkxace01.sweepy.ui.components.InkButton
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.MarkIcon
import io.github.darkxace01.sweepy.ui.components.MediaThumbnail
import io.github.darkxace01.sweepy.ui.components.Mosaic
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.QuietIconButton
import io.github.darkxace01.sweepy.ui.components.Ruler
import io.github.darkxace01.sweepy.ui.components.ThinProgress
import io.github.darkxace01.sweepy.ui.components.deckTitle
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.components.reveal
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val COLLAPSED_FOLDERS = 5
private const val COLLAPSED_MONTHS = 6
private const val MONTH_COLUMNS = 3
private val PAGE = 22.dp

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
    val ready = !state.loading && state.totalCount > 0

    Paper {
        LazyColumn(
            contentPadding = PaddingValues(
                start = PAGE + insets.calculateStartPadding(layoutDirection),
                end = PAGE + insets.calculateEndPadding(layoutDirection),
                top = insets.calculateTopPadding() + 4.dp,
                bottom = insets.calculateBottomPadding() + 28.dp,
            ),
        ) {
            item { TopBar(onOpenTrash, Modifier.reveal(0)) }

            if (partialAccess) {
                item { PartialAccessBanner(onRequestFullAccess, Modifier.padding(top = 8.dp).reveal(1)) }
            }

            item { Hero(state, onStart = { onOpenDeck(DeckSource.All) }, Modifier.padding(top = 28.dp).reveal(1)) }

            if (ready) {
                if (state.recent.isNotEmpty()) {
                    item { SectionHeader(1, stringResource(R.string.home_section_recent), Modifier.reveal(3)) }
                    item { FilmStrip(state.recent, onClick = { onOpenDeck(DeckSource.All) }, Modifier.reveal(3)) }
                }

                item { Stats(state, onOpenReview, onOpenTrash, Modifier.padding(top = 28.dp).reveal(4)) }

                if (state.quickDecks.isNotEmpty()) {
                    item { SectionHeader(2, stringResource(R.string.home_section_quick), Modifier.reveal(5)) }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = PAGE),
                            modifier = Modifier.fillMaxWidth().bleed(PAGE).reveal(5),
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
                            number = 3,
                            title = stringResource(R.string.home_section_folders),
                            action = toggleLabel(state.folders.size, COLLAPSED_FOLDERS, showAllFolders),
                            onAction = { showAllFolders = !showAllFolders },
                        )
                    }
                    val folders = if (showAllFolders) state.folders else state.folders.take(COLLAPSED_FOLDERS)
                    itemsIndexed(folders, key = { _, it -> "folder-" + it.source.toString() }) { index, summary ->
                        if (index > 0) Hairline()
                        FolderRow(summary, onClick = { onOpenDeck(summary.source) })
                    }
                }

                if (state.months.isNotEmpty()) {
                    item {
                        SectionHeader(
                            number = 4,
                            title = stringResource(R.string.home_section_months),
                            action = toggleLabel(state.months.size, COLLAPSED_MONTHS, showAllMonths),
                            onAction = { showAllMonths = !showAllMonths },
                        )
                    }
                    val months = if (showAllMonths) state.months else state.months.take(COLLAPSED_MONTHS)
                    items(months.chunked(MONTH_COLUMNS), key = { row -> "months-" + row.first().source.toString() }) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                            for (summary in row) MonthChip(summary, onClick = { onOpenDeck(summary.source) }, Modifier.weight(1f))
                            repeat(MONTH_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                item { SectionHeader(5, stringResource(R.string.home_section_archive)) }
                item { RecentlyCleanedRow(onOpenTrash) }
            }

            item { PrivacyNote(Modifier.padding(top = 36.dp)) }
        }
    }
}

@Composable
private fun toggleLabel(size: Int, collapsed: Int, expanded: Boolean): String? = when {
    size <= collapsed -> null
    expanded -> stringResource(R.string.show_less)
    else -> stringResource(R.string.show_all, size)
}

/** Lets a horizontally scrolling row run under the page's side margins, edge to edge. */
private fun Modifier.bleed(bleed: Dp): Modifier = this.layout { measurable, constraints ->
    val extra = bleed.roundToPx() * 2
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.minWidth + extra))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

// ---- Header & hero ------------------------------------------------------------------------------

@Composable
private fun TopBar(onOpenTrash: () -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().height(56.dp)) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontStyle = FontStyle.Italic,
            color = Sweepy.colors.ink,
        )
        Spacer(Modifier.width(8.dp))
        MarkIcon(Mark.CROSS, Sweepy.colors.pencil, size = 14.dp, strokeWidth = 0.14f, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.weight(1f))
        QuietIconButton(stringResource(R.string.trash_title), onOpenTrash, Modifier.padding(end = 0.dp)) {
            Icon(painterResource(R.drawable.ic_restore), contentDescription = null, tint = Sweepy.colors.ink, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun PartialAccessBanner(onRequestFullAccess: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Sweepy.colors.later.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Text(
            stringResource(R.string.partial_access),
            style = MaterialTheme.typography.bodySmall,
            color = Sweepy.colors.ink,
            modifier = Modifier.weight(1f),
        )
        Eyebrow(
            stringResource(R.string.partial_access_action),
            color = Sweepy.colors.ink,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .pressable(onRequestFullAccess)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun Hero(state: HomeUiState, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val remaining = state.totalCount - state.reviewedCount
    Column(modifier.fillMaxWidth()) {
        Eyebrow(
            if (state.loading) stringResource(R.string.home_scanning)
            else stringResource(R.string.home_library_eyebrow, formatSize(context, state.totalBytes)),
        )
        Spacer(Modifier.height(4.dp))
        when {
            state.loading -> {
                Text("—", style = MaterialTheme.typography.displayLarge, color = Sweepy.colors.inkMuted)
            }
            state.totalCount == 0 -> {
                Text(
                    stringResource(R.string.home_empty),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Sweepy.colors.ink,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            else -> {
                CountUp(remaining.toLong(), style = MaterialTheme.typography.displayLarge)
                Text(
                    if (remaining == 0) stringResource(R.string.home_all_sorted)
                    else pluralStringResource(R.plurals.home_left_caption, remaining),
                    style = MaterialTheme.typography.headlineSmall,
                    fontStyle = FontStyle.Italic,
                    color = Sweepy.colors.inkMuted,
                )
                Spacer(Modifier.height(24.dp))
                Ruler(state.progress)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Eyebrow(stringResource(R.string.home_sorted_pct, (state.progress * 100).roundToInt()), small = true, color = Sweepy.colors.ink)
                    Spacer(Modifier.weight(1f))
                    Eyebrow(
                        stringResource(R.string.home_sorted_of, formatCount(state.reviewedCount), formatCount(state.totalCount)),
                        small = true,
                    )
                }
                Spacer(Modifier.height(24.dp))
                InkButton(
                    text = stringResource(if (state.reviewedCount == 0) R.string.home_start else R.string.home_continue),
                    trailing = "→",
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---- Film strip ---------------------------------------------------------------------------------

private val FilmBase = Color(0xFF1B1916)
private val FilmEdgeText = Color(0xFFE39B4B)
private val FRAME = 74.dp
private val FRAME_GAP = 8.dp

/** The newest photos still waiting, shown as a strip of negative film with sprocket holes. */
@Composable
private fun FilmStrip(items: List<MediaItem>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .bleed(PAGE)
            .background(FilmBase)
            .pressable(onClick, pressedScale = 0.99f),
    ) {
        item { FilmFrame(null, null, leader = true) }
        itemsIndexed(items, key = { _, it -> it.id }) { index, item -> FilmFrame(item, index + 1) }
        item { FilmFrame(null, null, leader = true) }
    }
}

@Composable
private fun FilmFrame(item: MediaItem?, number: Int?, leader: Boolean = false) {
    val width = if (leader) PAGE else FRAME + FRAME_GAP
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width)
            .drawBehind {
                // Sprocket holes along both edges.
                val hole = Size(5.dp.toPx(), 3.5.dp.toPx())
                val step = 11.dp.toPx()
                val color = Color(0xFFF2EFE8).copy(alpha = 0.85f)
                var x = step / 2 - hole.width / 2
                while (x < size.width) {
                    drawRoundRect(color, Offset(x, 5.dp.toPx()), hole, CornerRadius(1.dp.toPx()))
                    drawRoundRect(color, Offset(x, size.height - 5.dp.toPx() - hole.height), hole, CornerRadius(1.dp.toPx()))
                    x += step
                }
            }
            .padding(top = 16.dp, bottom = 6.dp),
    ) {
        Box(Modifier.size(FRAME)) {
            if (item != null) MediaThumbnail(item, Modifier.fillMaxSize(), cornerRadius = 3.dp, showBadge = false)
        }
        Text(
            if (number != null) "$number ▸" else " ",
            style = MaterialTheme.typography.labelSmall,
            color = FilmEdgeText,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
    }
}

// ---- Stats --------------------------------------------------------------------------------------

@Composable
private fun Stats(state: HomeUiState, onOpenReview: () -> Unit, onOpenTrash: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth()) {
        Hairline()
        Row(Modifier.height(IntrinsicSize.Min)) {
            Stat(
                mark = Mark.CROSS,
                markColor = Sweepy.colors.pencil,
                label = stringResource(R.string.home_stat_bin),
                value = formatCount(state.markedCount),
                detail = formatSize(context, state.markedBytes),
                onClick = onOpenReview,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(Sweepy.colors.hairline))
            Stat(
                mark = Mark.CIRCLE,
                markColor = Sweepy.colors.keep,
                label = stringResource(R.string.home_stat_cleaned),
                value = formatSize(context, state.stats.cleanedBytes),
                detail = pluralStringResource(R.plurals.items_count, state.stats.cleanedCount.toInt(), formatCount(state.stats.cleanedCount)),
                onClick = onOpenTrash,
                modifier = Modifier.weight(1f).padding(start = 18.dp),
            )
        }
        Hairline()
    }
}

@Composable
private fun Stat(
    mark: Mark,
    markColor: Color,
    label: String,
    value: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.pressable(onClick, pressedScale = 0.98f).padding(vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MarkIcon(mark, markColor, size = 13.dp, strokeWidth = 0.14f)
            Spacer(Modifier.width(7.dp))
            Eyebrow(label, small = true)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineLarge, color = Sweepy.colors.ink, maxLines = 1)
        Eyebrow(detail, small = true)
    }
}

// ---- Sections -----------------------------------------------------------------------------------

@Composable
private fun SectionHeader(number: Int, title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(top = 36.dp, bottom = 12.dp),
    ) {
        Eyebrow(String.format(Locale.ROOT, "%02d", number), color = Sweepy.colors.pencil)
        Eyebrow(" — $title", color = Sweepy.colors.ink, modifier = Modifier.weight(1f))
        if (action != null) {
            Eyebrow(
                action,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .pressable(onAction)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun leftLabel(summary: DeckSummary): String =
    if (summary.remainingCount == 0) stringResource(R.string.label_done)
    else stringResource(R.string.left_short, formatCount(summary.remainingCount))

@Composable
private fun QuickDeckTile(summary: DeckSummary, onClick: () -> Unit) {
    val context = LocalContext.current
    val done = summary.remainingCount == 0
    Column(Modifier.width(150.dp).pressable(onClick)) {
        Box {
            Mosaic(summary.covers, Modifier.fillMaxWidth())
            if (done) {
                MarkIcon(
                    Mark.CIRCLE,
                    Sweepy.colors.keep,
                    size = 150.dp,
                    strokeWidth = 0.03f,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(deckTitle(summary.source), style = MaterialTheme.typography.titleMedium, color = Sweepy.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Eyebrow(leftLabel(summary) + " · " + formatSize(context, summary.totalBytes), small = true, color = if (done) Sweepy.colors.keep else Sweepy.colors.inkMuted)
        Spacer(Modifier.height(8.dp))
        ThinProgress(summary.progress, color = if (done) Sweepy.colors.keep else Sweepy.colors.ink)
    }
}

@Composable
private fun FolderRow(summary: DeckSummary, onClick: () -> Unit) {
    val context = LocalContext.current
    val done = summary.remainingCount == 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().pressable(onClick, pressedScale = 0.985f).padding(vertical = 12.dp),
    ) {
        Box(Modifier.size(52.dp)) {
            val cover = summary.cover
            if (cover != null) MediaThumbnail(cover, Modifier.fillMaxSize(), cornerRadius = 6.dp, showBadge = false)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(deckTitle(summary.source), style = MaterialTheme.typography.titleMedium, color = Sweepy.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Eyebrow(leftLabel(summary) + " · " + formatSize(context, summary.totalBytes), small = true, color = if (done) Sweepy.colors.keep else Sweepy.colors.inkMuted)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "${(summary.progress * 100).roundToInt()}%",
            style = MaterialTheme.typography.headlineSmall,
            color = if (done) Sweepy.colors.keep else Sweepy.colors.ink,
        )
    }
}

@Composable
private fun MonthChip(summary: DeckSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ym = (summary.source as DeckSource.Month).yearMonth
    val month = YearMonth.of(ym / 100, ym % 100)
    val done = summary.remainingCount == 0
    val locale = LocalLocale.current.platformLocale
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Sweepy.colors.hairline, RoundedCornerShape(12.dp))
            .pressable(onClick),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    month.month.getDisplayName(JavaTextStyle.SHORT, locale),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Sweepy.colors.ink,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
                Eyebrow("’" + (month.year % 100).toString().padStart(2, '0'), small = true, modifier = Modifier.padding(bottom = 5.dp))
            }
            Spacer(Modifier.height(6.dp))
            Eyebrow(leftLabel(summary), small = true, color = if (done) Sweepy.colors.keep else Sweepy.colors.inkMuted)
        }
        if (done) {
            // Finished months get circled, like a keeper on a contact sheet.
            Box(
                Modifier
                    .matchParentSize()
                    .padding(3.dp)
                    .pencilMark(Mark.CIRCLE, Sweepy.colors.keep.copy(alpha = 0.75f), { 1f }, strokeWidth = 0.04f),
            )
        }
    }
}

@Composable
private fun RecentlyCleanedRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Sweepy.colors.surface)
            .border(1.dp, Sweepy.colors.hairline, RoundedCornerShape(14.dp))
            .pressable(onClick, pressedScale = 0.985f)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Icon(painterResource(R.drawable.ic_restore), contentDescription = null, tint = Sweepy.colors.ink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.trash_title), style = MaterialTheme.typography.titleMedium, color = Sweepy.colors.ink)
            Text(stringResource(R.string.home_trash_subtitle), style = MaterialTheme.typography.bodySmall, color = Sweepy.colors.inkMuted)
        }
        Text("→", style = MaterialTheme.typography.titleMedium, color = Sweepy.colors.inkMuted)
    }
}

@Composable
private fun PrivacyNote(modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        MarkIcon(Mark.CIRCLE, Sweepy.colors.inkMuted.copy(alpha = 0.5f), size = 18.dp, strokeWidth = 0.1f)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.privacy_note).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Sweepy.colors.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}
