package io.github.darkxace01.sweepy.ui.home

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.tips.TIP_NUDGE_BIG_BYTES
import io.github.darkxace01.sweepy.data.tips.Tip
import io.github.darkxace01.sweepy.ui.DeckSummary
import io.github.darkxace01.sweepy.ui.DuplicatesSummary
import io.github.darkxace01.sweepy.ui.HomeUiState
import io.github.darkxace01.sweepy.ui.components.AudioArt
import io.github.darkxace01.sweepy.ui.components.CountUp
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.InkButton
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.MarkIcon
import io.github.darkxace01.sweepy.ui.components.MediaThumbnail
import io.github.darkxace01.sweepy.ui.components.OutlineButton
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.QuietIconButton
import io.github.darkxace01.sweepy.ui.components.Ruler
import io.github.darkxace01.sweepy.ui.components.chunky
import io.github.darkxace01.sweepy.ui.components.deckTitle
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.components.reveal
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.Pixnix
import io.github.darkxace01.sweepy.ui.tips.CoffeeIcon
import io.github.darkxace01.sweepy.ui.tips.TipArt
import java.time.Month
import java.time.format.TextStyle as JavaTextStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val PAGE = 22.dp
private val GRID_GAP = 16.dp
private const val MONTH_COLUMNS = 3

/** The two ways to browse the library on the home screen. */
enum class HomeTab { FOLDERS, MONTHS }

@Composable
fun HomeScreen(
    state: HomeUiState,
    partialAccess: Boolean,
    onOpenDeck: (DeckSource) -> Unit,
    onOpenReview: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onRequestFullAccess: () -> Unit,
    /** Null hides the tip jar (builds without Google Play billing). */
    onOpenTipJar: (() -> Unit)? = null,
    showTipNudge: Boolean = false,
    onDismissTipNudge: () -> Unit = {},
    /** The one-time "want a heads-up now and then?" card. */
    showReminderAsk: Boolean = false,
    onAcceptReminders: () -> Unit = {},
    onDeclineReminders: () -> Unit = {},
    /** Whether Pixnix may see audio files. Until then the audio card asks for it. */
    audioAllowed: Boolean = true,
    onAllowAudio: () -> Unit = {},
    initialTab: HomeTab = HomeTab.FOLDERS,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    // The page settles in once. Coming back from a deck should look like it never left.
    var introPlayed by rememberSaveable { mutableStateOf(false) }
    var animateIn by remember { mutableStateOf(!introPlayed) }
    SideEffect { introPlayed = true }
    // Only the opening settles in. Cards scrolled into view later just appear.
    LaunchedEffect(Unit) {
        delay(1_200)
        animateIn = false
    }
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
            item { TopBar(state.markedCount, onOpenReview, onOpenTrash, onOpenSettings, onOpenTipJar, Modifier.reveal(0, animateIn)) }

            if (partialAccess) {
                item { PartialAccessBanner(onRequestFullAccess, Modifier.padding(top = 8.dp).reveal(1, animateIn)) }
            }

            item { Hero(state, onStart = { onOpenDeck(DeckSource.All) }, Modifier.padding(top = 24.dp).reveal(1, animateIn)) }

            if (ready) {
                if (showTipNudge && onOpenTipJar != null) {
                    item(key = "tip-nudge") {
                        TipNudge(
                            cleanedBytes = state.stats.cleanedBytes,
                            onOpen = {
                                onDismissTipNudge()
                                onOpenTipJar()
                            },
                            onDismiss = onDismissTipNudge,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }

                if (showReminderAsk) {
                    item(key = "reminder-ask") {
                        NoteCard(
                            art = {
                                Icon(
                                    painterResource(R.drawable.ic_notification),
                                    contentDescription = null,
                                    tint = Pixnix.colors.pencil,
                                    modifier = Modifier.size(40.dp),
                                )
                            },
                            title = stringResource(R.string.reminder_ask_title),
                            body = stringResource(R.string.reminder_ask_body),
                            no = stringResource(R.string.reminder_ask_no),
                            yes = stringResource(R.string.reminder_ask_yes),
                            onNo = onDeclineReminders,
                            onYes = onAcceptReminders,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }

                item(key = "tabs") { Tabs(tab, onSelect = { tab = it }, Modifier.padding(top = 36.dp, bottom = 20.dp).reveal(3, animateIn)) }

                when (tab) {
                    HomeTab.FOLDERS -> {
                        val cards = buildList<@Composable (Modifier) -> Unit> {
                            fun quick(source: DeckSource) = state.quickDecks.firstOrNull { it.source == source }?.let { deck ->
                                add { m -> DeckCard(deck, { onOpenDeck(deck.source) }, m) }
                            }
                            // Duplicates, large videos, audio, maybe later, WhatsApp, then the folders.
                            add { m -> DuplicatesCard(state.duplicates, onOpenDuplicates, m) }
                            quick(DeckSource.LargeVideos)
                            val audio = state.audio
                            when {
                                !audioAllowed -> add { m -> AudioInviteCard(onAllowAudio, m) }
                                audio != null -> add { m -> DeckCard(audio, { onOpenDeck(audio.source) }, m) }
                            }
                            quick(DeckSource.Later)
                            quick(DeckSource.WhatsApp)
                            for (folder in state.folders) add { m -> DeckCard(folder, { onOpenDeck(folder.source) }, m) }
                        }
                        val rows = cards.chunked(2)
                        items(rows.size, key = { "folders-$it" }) { index ->
                            GridRow(rows[index], columns = 2, Modifier.reveal(4 + index, animateIn))
                        }
                    }
                    HomeTab.MONTHS -> {
                        for ((year, months) in state.months.groupBy { (it.source as DeckSource.Month).yearMonth / 100 }) {
                            item(key = "year-$year") { YearHeader(year, months) }
                            val rows = months.chunked(MONTH_COLUMNS)
                            items(rows.size, key = { "months-$year-$it" }) { index ->
                                val cards = rows[index].map<DeckSummary, @Composable (Modifier) -> Unit> { month ->
                                    { m -> MonthCard(month, { onOpenDeck(month.source) }, m) }
                                }
                                GridRow(cards, columns = MONTH_COLUMNS)
                            }
                        }
                    }
                }
            }

            item { PrivacyNote(Modifier.padding(top = 28.dp)) }
        }
    }
}

/** One row of the grid; short rows keep their cards the same width as full ones. */
@Composable
private fun GridRow(cards: List<@Composable (Modifier) -> Unit>, columns: Int, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (columns > 2) 12.dp else GRID_GAP),
        modifier = modifier.fillMaxWidth().padding(bottom = 26.dp),
    ) {
        for (card in cards) card(Modifier.weight(1f))
        repeat(columns - cards.size) { Spacer(Modifier.weight(1f)) }
    }
}

// ---- Header & hero ------------------------------------------------------------------------------

@Composable
private fun TopBar(
    markedCount: Int,
    onOpenReview: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTipJar: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().height(56.dp)) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontStyle = FontStyle.Italic,
            color = Pixnix.colors.ink,
        )
        Spacer(Modifier.width(8.dp))
        MarkIcon(Mark.CROSS, Pixnix.colors.pencil, size = 14.dp, strokeWidth = 0.14f, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.weight(1f))
        if (markedCount > 0) {
            BinChip(markedCount, onOpenReview)
            Spacer(Modifier.width(4.dp))
        }
        if (onOpenTipJar != null) {
            QuietIconButton(stringResource(R.string.tips_title), onOpenTipJar) { CoffeeIcon(Pixnix.colors.ink) }
        }
        QuietIconButton(stringResource(R.string.trash_title), onOpenTrash) {
            Icon(painterResource(R.drawable.ic_restore), contentDescription = null, tint = Pixnix.colors.ink, modifier = Modifier.size(22.dp))
        }
        QuietIconButton(stringResource(R.string.settings_title), onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = null, tint = Pixnix.colors.ink, modifier = Modifier.size(22.dp))
        }
    }
}

/** The review bin, waiting: a small red tag with its count. Only there when something's in it. */
@Composable
private fun BinChip(count: Int, onClick: () -> Unit) {
    val label = stringResource(R.string.review_title)
    val shape = RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(36.dp)
            .chunky(shape, fill = Pixnix.colors.surface, offset = 3.dp)
            .clip(shape)
            .border(1.dp, Pixnix.colors.pencil.copy(alpha = 0.45f), shape)
            .pressable(onClick, pressedScale = 0.92f)
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = label },
    ) {
        MarkIcon(Mark.CROSS, Pixnix.colors.pencil, size = 12.dp, strokeWidth = 0.16f)
        Spacer(Modifier.width(7.dp))
        Eyebrow(formatCount(count), color = Pixnix.colors.ink)
    }
}

@Composable
private fun PartialAccessBanner(onRequestFullAccess: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Pixnix.colors.later.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Text(
            stringResource(R.string.partial_access),
            style = MaterialTheme.typography.bodySmall,
            color = Pixnix.colors.ink,
            modifier = Modifier.weight(1f),
        )
        Eyebrow(
            stringResource(R.string.partial_access_action),
            color = Pixnix.colors.ink,
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
            when {
                state.loading -> stringResource(R.string.home_scanning)
                state.stats.cleanedBytes > 0 -> stringResource(
                    R.string.home_library_cleaned,
                    formatSize(context, state.totalBytes),
                    formatSize(context, state.stats.cleanedBytes),
                )
                else -> stringResource(R.string.home_library_eyebrow, formatSize(context, state.totalBytes))
            },
        )
        Spacer(Modifier.height(4.dp))
        when {
            state.loading -> {
                Text("—", style = MaterialTheme.typography.displayLarge, color = Pixnix.colors.inkMuted)
            }
            state.totalCount == 0 -> {
                Text(
                    stringResource(R.string.home_empty),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Pixnix.colors.ink,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            else -> {
                CountUp(remaining.toLong(), style = MaterialTheme.typography.displayLarge, memoryKey = "home_remaining")
                Text(
                    if (remaining == 0) stringResource(R.string.home_all_sorted)
                    else pluralStringResource(R.plurals.home_left_caption, remaining),
                    style = MaterialTheme.typography.headlineSmall,
                    fontStyle = FontStyle.Italic,
                    color = Pixnix.colors.inkMuted,
                )
                Spacer(Modifier.height(20.dp))
                Ruler(state.progress)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Eyebrow(stringResource(R.string.home_sorted_pct, (state.progress * 100).roundToInt()), small = true, color = Pixnix.colors.ink)
                    Spacer(Modifier.weight(1f))
                    Eyebrow(
                        stringResource(R.string.home_sorted_of, formatCount(state.reviewedCount), formatCount(state.totalCount)),
                        small = true,
                    )
                }
                Spacer(Modifier.height(22.dp))
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

// ---- Folders / Months ---------------------------------------------------------------------------

/** Two words; the chosen one gets the grease-pencil loop drawn round it. */
@Composable
private fun Tabs(selected: HomeTab, onSelect: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        for (tab in HomeTab.entries) {
            Tab(
                text = stringResource(if (tab == HomeTab.FOLDERS) R.string.home_section_folders else R.string.home_section_months),
                selected = tab == selected,
                onClick = { onSelect(tab) },
            )
            Spacer(Modifier.width(20.dp))
        }
    }
}

@Composable
private fun Tab(text: String, selected: Boolean, onClick: () -> Unit) {
    val inspection = LocalInspectionMode.current
    val loop = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected) {
        if (!selected) loop.snapTo(0f)
        else if (inspection) loop.snapTo(1f)
        else loop.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .pressable(onClick, pressedScale = 0.95f, role = Role.Tab)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = 1.32f
                    scaleY = 1.6f
                }
                .pencilMark(Mark.CIRCLE, Pixnix.colors.pencil, { loop.value }, strokeWidth = 0.05f),
        )
        Text(
            text,
            style = MaterialTheme.typography.headlineSmall,
            fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
            color = if (selected) Pixnix.colors.ink else Pixnix.colors.inkMuted,
        )
    }
}

@Composable
private fun YearHeader(year: Int, months: List<DeckSummary>) {
    val left = months.sumOf { it.remainingCount }
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp)) {
        Text(year.toString(), style = MaterialTheme.typography.headlineMedium, color = Pixnix.colors.ink)
        Spacer(Modifier.weight(1f))
        Eyebrow(
            if (left == 0) stringResource(R.string.label_done) else stringResource(R.string.left_short, formatCount(left)),
            small = true,
            color = if (left == 0) Pixnix.colors.keep else Pixnix.colors.inkMuted,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun leftLabel(summary: DeckSummary): String =
    if (summary.remainingCount == 0) stringResource(R.string.label_done)
    else stringResource(R.string.left_short, formatCount(summary.remainingCount))

@Composable
private fun DeckCard(summary: DeckSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val done = summary.remainingCount == 0
    StackCard(
        covers = summary.covers,
        title = deckTitle(summary.source),
        detail = leftLabel(summary) + " · " + formatSize(context, summary.totalBytes),
        detailColor = if (done) Pixnix.colors.keep else Pixnix.colors.inkMuted,
        done = done,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun MonthCard(summary: DeckSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val yearMonth = (summary.source as DeckSource.Month).yearMonth
    val locale = LocalLocale.current.platformLocale
    val done = summary.remainingCount == 0
    StackCard(
        covers = summary.covers,
        title = Month.of(yearMonth % 100).getDisplayName(JavaTextStyle.SHORT, locale),
        detail = leftLabel(summary),
        detailColor = if (done) Pixnix.colors.keep else Pixnix.colors.inkMuted,
        done = done,
        compact = true,
        onClick = onClick,
        modifier = modifier,
    )
}

/** Our strength, so it leads the grid: how much the copies and look-alikes add up to. */
@Composable
private fun DuplicatesCard(summary: DuplicatesSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val none = !summary.scanning && summary.groupCount == 0
    StackCard(
        covers = summary.covers,
        title = stringResource(R.string.deck_duplicates),
        detail = when {
            summary.groupCount > 0 -> pluralStringResource(R.plurals.dup_tile_groups, summary.groupCount, formatCount(summary.groupCount)) +
                " · " + formatSize(context, summary.reclaimableBytes)
            summary.scanning -> summary.progress?.let { stringResource(R.string.dup_scanning_pct, (it * 100).roundToInt()) }
                ?: stringResource(R.string.dup_scanning)
            else -> stringResource(R.string.dup_none)
        },
        detailColor = when {
            none -> Pixnix.colors.keep
            summary.groupCount > 0 -> Pixnix.colors.pencil
            else -> Pixnix.colors.inkMuted
        },
        done = none,
        onClick = onClick,
        modifier = modifier,
        empty = {
            MarkIcon(
                if (none) Mark.CIRCLE else Mark.QUESTION,
                if (none) Pixnix.colors.keep else Pixnix.colors.inkMuted.copy(alpha = 0.5f),
                size = 44.dp,
                strokeWidth = 0.07f,
            )
        },
    )
}

/** Audio needs its own permission on Android 13+. This card asks for it, once tapped. */
@Composable
private fun AudioInviteCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    StackCard(
        covers = emptyList(),
        title = stringResource(R.string.deck_audio),
        detail = stringResource(R.string.audio_allow),
        detailColor = Pixnix.colors.ink,
        done = false,
        onClick = onClick,
        modifier = modifier,
        empty = { AudioArt(SampleAudio, Modifier.fillMaxSize(), bars = 9) },
    )
}

/** Gives the invitation card a waveform to show before there are real files. */
private val SampleAudio = MediaItem(
    id = 6, uri = Uri.EMPTY, isVideo = false, mimeType = null, displayName = null, folder = null, bucketId = 0,
    relativePath = null, sizeBytes = 0, dateTakenMillis = 0, dateAddedSeconds = 0, width = 0, height = 0,
    durationMillis = 0, isAudio = true,
)

/** How the prints underneath lean, front to back: like a handful dropped on a table. */
private val PILE = listOf(Triple(0f, 0f, 0f), Triple(-7f, -0.07f, 0.01f), Triple(6f, 0.07f, 0.02f))

/**
 * A deck shown as a small pile of its photos (up to three, the first on top), with its name and
 * how much is left underneath. A finished deck gets circled in green.
 */
@Composable
private fun StackCard(
    covers: List<MediaItem>,
    title: String,
    detail: String,
    detailColor: Color,
    done: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    empty: @Composable BoxScope.() -> Unit = {},
) {
    val shown = covers.take(PILE.size)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.pressable(onClick, pressedScale = 0.96f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            if (shown.isEmpty()) {
                Print(compact, Modifier.fillMaxSize(0.8f)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Pixnix.colors.hairline.copy(alpha = 0.5f)), content = empty)
                }
            }
            shown.withIndex().reversed().forEach { (depth, item) ->
                val (angle, dx, dy) = PILE[depth]
                Print(
                    compact,
                    Modifier
                        .fillMaxSize(0.8f)
                        .graphicsLayer {
                            rotationZ = angle
                            translationX = size.width * dx
                            translationY = size.height * dy
                        },
                ) {
                    MediaThumbnail(item, Modifier.fillMaxSize(), cornerRadius = 0.dp, showBadge = false)
                }
            }
            if (done) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pencilMark(Mark.CIRCLE, Pixnix.colors.keep.copy(alpha = 0.85f), { 1f }, strokeWidth = 0.035f),
                )
            }
        }
        Spacer(Modifier.height(if (compact) 6.dp else 8.dp))
        Text(
            title,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = Pixnix.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Eyebrow(detail, small = true, color = detailColor)
    }
}

/** One photo printed with a thin border, the way the swipe cards look. */
@Composable
private fun Print(compact: Boolean, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val corner: Dp = if (compact) 7.dp else 10.dp
    val border: Dp = if (compact) 3.dp else 4.dp
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .then(
                if (Pixnix.look.chunky) Modifier.chunky(shape, offset = 3.dp)
                else Modifier.shadow(if (compact) 4.dp else 7.dp, shape, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.3f)),
            )
            .clip(shape)
            .background(Pixnix.colors.print)
            .padding(border)
            .clip(RoundedCornerShape(corner - border / 2)),
        content = content,
    )
}

/** Shown once, after Pixnix has cleaned up a gigabyte: a friendly, skippable ask. */
@Composable
private fun TipNudge(cleanedBytes: Long, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    NoteCard(
        art = { TipArt(Tip.COFFEE, Modifier.size(44.dp)) },
        // No-break space, so "12 GB" never splits across two lines.
        title = stringResource(R.string.tips_nudge_title, formatSize(LocalContext.current, cleanedBytes).replace(' ', '\u00A0')),
        body = stringResource(if (cleanedBytes >= TIP_NUDGE_BIG_BYTES) R.string.tips_nudge_body_big else R.string.tips_nudge_body),
        no = stringResource(R.string.tips_nudge_later),
        yes = stringResource(R.string.tips_nudge_open),
        onNo = onDismiss,
        onYes = onOpen,
        modifier = modifier,
    )
}

/** A one-off card in the home feed: a small drawing, a question, and two answers. */
@Composable
private fun NoteCard(
    art: @Composable () -> Unit,
    title: String,
    body: String,
    no: String,
    yes: String,
    onNo: () -> Unit,
    onYes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .chunky(shape, fill = Pixnix.colors.surface)
            .clip(shape)
            .background(Pixnix.colors.surface)
            .border(1.dp, Pixnix.colors.hairline, shape)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            art()
            Spacer(Modifier.width(14.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = Pixnix.colors.ink,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = Pixnix.colors.inkMuted,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton(no, onNo, Modifier.weight(1f))
            InkButton(yes, onYes, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PrivacyNote(modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        MarkIcon(Mark.CIRCLE, Pixnix.colors.inkMuted.copy(alpha = 0.5f), size = 18.dp, strokeWidth = 0.1f)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.privacy_note).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Pixnix.colors.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}
