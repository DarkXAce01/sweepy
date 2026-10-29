package io.github.darkxace01.sweepy.ui.duplicates

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.duplicates.DuplicateGrouper
import io.github.darkxace01.sweepy.data.duplicates.DuplicateKind
import io.github.darkxace01.sweepy.data.duplicates.KeepReason
import io.github.darkxace01.sweepy.ui.DuplicateGroupUi
import io.github.darkxace01.sweepy.ui.DuplicatesUiState
import io.github.darkxace01.sweepy.ui.components.EmptyState
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.InkButton
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.MediaThumbnail
import io.github.darkxace01.sweepy.ui.components.OutlineButton
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.QuietIconButton
import io.github.darkxace01.sweepy.ui.components.Ruler
import io.github.darkxace01.sweepy.ui.components.ScreenTopBar
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.deck.pinchToPeek
import io.github.darkxace01.sweepy.ui.formatDate
import io.github.darkxace01.sweepy.ui.formatSize
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import java.util.Locale
import kotlin.math.roundToInt

/**
 * One duplicate group at a time. Everything starts as "bin" except the suggested keeper (and any
 * favorites); tap a photo to flip it, hold it to look closer. Binned photos go to the review bin,
 * never straight to the trash.
 */
@Composable
fun DuplicatesScreen(
    state: DuplicatesUiState,
    onBack: () -> Unit,
    onResolve: (DuplicateGroupUi, Set<Long>) -> Unit,
    onUndo: () -> Unit,
) {
    var skipped by remember { mutableStateOf(setOf<String>()) }
    var peek by remember { mutableStateOf<MediaItem?>(null) }
    val current = state.groups.firstOrNull { it.key !in skipped } ?: state.groups.firstOrNull()
    val position = current?.let { state.groups.indexOf(it) + 1 } ?: 0

    BackHandler(enabled = peek != null) { peek = null }

    Paper {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            ScreenTopBar(
                title = stringResource(R.string.deck_duplicates),
                eyebrow = when {
                    current != null -> stringResource(R.string.dup_position, formatCount(position), formatCount(state.groups.size))
                    state.scanning -> scanningLabel(state.progress)
                    else -> ""
                },
                onBack = onBack,
                actions = {
                    if (state.canUndo) {
                        QuietIconButton(stringResource(R.string.action_undo), onUndo) {
                            Icon(painterResource(R.drawable.ic_undo), contentDescription = null, tint = Sweepy.colors.ink, modifier = Modifier.size(22.dp))
                        }
                    }
                    if (current != null && state.groups.size > 1) {
                        Eyebrow(
                            stringResource(R.string.dup_skip),
                            color = Sweepy.colors.ink,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(current.key) {
                                    detectTapGestures {
                                        skipped = if (skipped.size + 1 >= state.groups.size) emptySet() else skipped + current.key
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                        )
                    }
                },
            )

            when {
                current != null -> AnimatedContent(
                    targetState = current,
                    contentKey = { it.key },
                    transitionSpec = {
                        (slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(300)) { -it / 6 } + fadeOut(tween(160)))
                    },
                    label = "group",
                    modifier = Modifier.weight(1f),
                ) { group ->
                    GroupView(group, onResolve = { keep -> onResolve(group, keep) }, onPeek = { peek = it })
                }
                state.scanning -> Scanning(state.progress, Modifier.weight(1f))
                else -> EmptyState(
                    mark = Mark.CIRCLE,
                    title = stringResource(R.string.dup_empty_title),
                    body = stringResource(R.string.dup_empty_body),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        peek?.let { item -> Peek(item, onClose = { peek = null }) }
    }
}

@Composable
private fun scanningLabel(progress: Float?): String =
    if (progress == null) stringResource(R.string.dup_scanning)
    else stringResource(R.string.dup_scanning_pct, (progress * 100).roundToInt())

@Composable
private fun Scanning(progress: Float?, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(40.dp),
    ) {
        Text(
            stringResource(R.string.dup_scanning_title),
            style = MaterialTheme.typography.headlineMedium,
            fontStyle = FontStyle.Italic,
            color = Sweepy.colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Ruler(progress ?: 0f)
        Spacer(Modifier.height(10.dp))
        Eyebrow(scanningLabel(progress), small = true)
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.dup_scanning_body),
            style = MaterialTheme.typography.bodySmall,
            color = Sweepy.colors.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GroupView(group: DuplicateGroupUi, onResolve: (Set<Long>) -> Unit, onPeek: (MediaItem) -> Unit) {
    val context = LocalContext.current
    var keep by remember(group.key) { mutableStateOf(setOf(group.suggested.id) + group.favorites) }
    val bins = group.items.filter { it.id !in keep }
    val exact = group.group.kind == DuplicateKind.EXACT
    val firstTaken = group.items.minOf { it.dateTakenMillis }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp)) {
            Text(
                stringResource(if (exact) R.string.dup_exact_title else R.string.dup_similar_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Sweepy.colors.ink,
            )
            Text(
                when {
                    exact -> pluralStringResource(R.plurals.dup_exact_body, group.items.size, group.items.size)
                    group.group.spanMillis <= DuplicateGrouper.BURST_WINDOW_MILLIS ->
                        pluralStringResource(R.plurals.dup_burst_body, group.items.size, group.items.size, formatSpan(group.group.spanMillis))
                    else -> pluralStringResource(R.plurals.dup_similar_body, group.items.size, group.items.size)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontStyle = FontStyle.Italic,
                color = Sweepy.colors.inkMuted,
            )
            Spacer(Modifier.height(10.dp))
            Eyebrow(stringResource(R.string.dup_instructions), small = true)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (group.items.size <= 4) 2 else 3),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(group.items, key = { it.id }) { item ->
                val favorite = item.id in group.favorites
                Tile(
                    item = item,
                    kept = item.id in keep,
                    suggestion = if (item.id == group.suggested.id) group.reason else if (favorite) KeepReason.FAVORITE else null,
                    caption = if (exact) item.folder.orEmpty() else formatOffset(item.dateTakenMillis - firstTaken),
                    onToggle = { if (!favorite) keep = if (item.id in keep) keep - item.id else keep + item.id },
                    onPeek = { onPeek(item) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp, top = 4.dp),
        ) {
            OutlineButton(stringResource(R.string.dup_keep_all), onClick = { onResolve(group.items.mapTo(HashSet()) { it.id }) }, modifier = Modifier.weight(1f))
            if (bins.isEmpty()) {
                InkButton(stringResource(R.string.dup_done), onClick = { onResolve(keep) }, modifier = Modifier.weight(1.4f))
            } else {
                InkButton(
                    text = stringResource(R.string.dup_bin, formatCount(bins.size)),
                    trailing = formatSize(context, bins.sumOf { it.sizeBytes }),
                    color = Sweepy.colors.pencil,
                    contentColor = Sweepy.colors.onPencil,
                    onClick = { onResolve(keep) },
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}

@Composable
private fun Tile(
    item: MediaItem,
    kept: Boolean,
    suggestion: KeepReason?,
    caption: String,
    onToggle: () -> Unit,
    onPeek: () -> Unit,
) {
    val context = LocalContext.current
    val instant = LocalInspectionMode.current
    val circle by animateFloatAsState(if (kept) 1f else 0f, if (instant) tween(0) else tween(if (kept) 420 else 160), label = "keep")
    val cross by animateFloatAsState(if (kept) 0f else 1f, if (instant) tween(0) else tween(if (kept) 160 else 380), label = "bin")
    val keepLabel = stringResource(R.string.action_keep)
    Column(
        Modifier
            .padding(4.dp)
            .semantics {
                selected = kept
                role = Role.Checkbox
                contentDescription = keepLabel + ": " + item.displayName.orEmpty()
            }
            .pointerInput(item.id) { detectTapGestures(onTap = { onToggle() }, onLongPress = { onPeek() }) },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pencilMark(Mark.CIRCLE, Sweepy.colors.keep, { circle }, strokeWidth = 0.04f),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Sweepy.colors.print)
                    .padding(3.dp),
            ) {
                MediaThumbnail(item, Modifier.fillMaxSize(), cornerRadius = 3.dp)
                // Binned photos fade back and get crossed out.
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = cross * 0.45f }.background(Sweepy.colors.paper))
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .pencilMark(Mark.CROSS, Sweepy.colors.pencil, { cross }, strokeWidth = 0.06f),
                )
            }
        }
        Column(Modifier.padding(start = 8.dp)) {
            if (suggestion != null) {
                // Why we'd keep this one, written under it like a note on the sheet.
                Eyebrow(
                    reasonLabel(suggestion),
                    small = true,
                    color = Sweepy.colors.paper,
                    modifier = Modifier
                        .padding(bottom = 3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (suggestion == KeepReason.FAVORITE) Sweepy.colors.favorite else Sweepy.colors.keep)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
            Eyebrow(formatSize(context, item.sizeBytes) + " · " + megapixels(item), small = true, color = Sweepy.colors.ink)
            Eyebrow(caption, small = true)
        }
    }
}

@Composable
private fun reasonLabel(reason: KeepReason): String = stringResource(
    when (reason) {
        KeepReason.ORIGINAL -> R.string.reason_original
        KeepReason.SHARPEST -> R.string.reason_sharpest
        KeepReason.MOST_DETAIL -> R.string.reason_detail
        KeepReason.FAVORITE -> R.string.reason_favorite
    },
)

/** Full-screen look at one photo; pinch to zoom, tap or back to close. */
@Composable
private fun Peek(item: MediaItem, onClose: () -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF012110F))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .safeDrawingPadding(),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(bottom = 56.dp).pinchToPeek(),
        )
        Text(
            listOf(item.folder.orEmpty(), formatDate(item.dateTakenMillis), formatSize(context, item.sizeBytes))
                .filter { it.isNotEmpty() }.joinToString("  ·  ").uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFF2EFE8),
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
        )
    }
}

/** "12 MP", or "0.3 MP" for small images. */
private fun megapixels(item: MediaItem): String {
    val mp = item.width.toLong() * item.height / 1_000_000.0
    return if (mp >= 1) "${mp.roundToInt()} MP" else String.format(Locale.ROOT, "%.1f MP", mp)
}

/** "+2.4 s" from the first shot; "0 s" for the first. */
private fun formatOffset(millis: Long): String = when {
    millis < 1_000 -> if (millis == 0L) "0 s" else String.format(Locale.ROOT, "+%.1f s", millis / 1000.0)
    millis < 60_000 -> String.format(Locale.ROOT, "+%.1f s", millis / 1000.0)
    millis < 3_600_000 -> "+${millis / 60_000} min"
    millis < 86_400_000 -> "+${millis / 3_600_000} h"
    else -> "+${millis / 86_400_000} d"
}

private fun formatSpan(millis: Long): String =
    if (millis < 1_000) "1 s" else String.format(Locale.ROOT, "%d s", (millis + 999) / 1000)
