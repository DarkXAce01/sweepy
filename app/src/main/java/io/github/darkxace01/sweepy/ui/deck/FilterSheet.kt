package io.github.darkxace01.sweepy.ui.deck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.DeckFilter
import io.github.darkxace01.sweepy.data.DeckOrder
import io.github.darkxace01.sweepy.data.MediaKind
import io.github.darkxace01.sweepy.ui.DeckUiState
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.OutlineButton
import io.github.darkxace01.sweepy.ui.components.Segment
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.theme.Sweepy

/** Square button in the deck's top bar: two sliders, with a pencil dot when the deck is narrowed or re-sorted. */
@Composable
fun FilterButton(active: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.filter_title)
    val color = if (active) Sweepy.colors.pencil else Sweepy.colors.ink
    val paper = Sweepy.colors.paper
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (active) color.copy(alpha = 0.45f) else Sweepy.colors.hairline, RoundedCornerShape(10.dp))
            .pressable(onClick, pressedScale = 0.92f)
            .semantics { contentDescription = label },
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = 1.4.dp.toPx()
            val knob = 2.6.dp.toPx()
            for ((y, x) in listOf(0.28f to 0.68f, 0.72f to 0.32f)) {
                val cy = size.height * y
                drawLine(color, Offset(0f, cy), Offset(size.width, cy), stroke, StrokeCap.Round)
                drawCircle(color, knob, Offset(size.width * x, cy))
                drawCircle(paper, knob - stroke, Offset(size.width * x, cy))
            }
        }
        if (active) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(5.dp)
                    .background(Sweepy.colors.pencil, CircleShape),
            )
        }
    }
}

/** Choose what a deck shows and in which order. Changes apply right away, under the sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(state: DeckUiState, onChange: (DeckFilter) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Sweepy.colors.surface,
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(width = 36.dp, height = 3.dp).background(Sweepy.colors.hairline)) },
    ) {
        FilterSheetContent(state, onChange, onDismiss)
    }
}

@Composable
internal fun FilterSheetContent(state: DeckUiState, onChange: (DeckFilter) -> Unit, onDone: () -> Unit) {
    val filter = state.filter
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
        Eyebrow(stringResource(R.string.filter_eyebrow), small = true)
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.filter_title), style = MaterialTheme.typography.headlineSmall, color = Sweepy.colors.ink)

        if (state.canFilterKind) {
            Label(stringResource(R.string.filter_show))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (kind in MediaKind.entries) {
                    Segment(
                        text = when (kind) {
                            MediaKind.ALL -> stringResource(R.string.filter_kind_all)
                            MediaKind.PHOTOS -> stringResource(R.string.filter_kind_photos, formatCount(state.waitingPhotos))
                            MediaKind.VIDEOS -> stringResource(R.string.filter_kind_videos, formatCount(state.waitingVideos))
                        },
                        selected = filter.kind == kind,
                        onClick = { onChange(filter.copy(kind = kind)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Label(stringResource(R.string.filter_order))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (order in DeckOrder.entries) {
                Segment(
                    text = stringResource(
                        when (order) {
                            DeckOrder.NEWEST -> R.string.filter_order_newest
                            DeckOrder.OLDEST -> R.string.filter_order_oldest
                            DeckOrder.LARGEST -> R.string.filter_order_largest
                        },
                    ),
                    selected = filter.order == order,
                    onClick = { onChange(filter.copy(order = order)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.isFiltered) {
                OutlineButton(stringResource(R.string.filter_reset), { onChange(state.defaultFilter) }, Modifier.weight(1f))
            }
            OutlineButton(stringResource(R.string.filter_done), onDone, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Label(text: String) {
    Eyebrow(text, color = Sweepy.colors.ink, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
}
