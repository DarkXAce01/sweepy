package io.github.darkxace01.sweepy.ui.deck

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.components.MediaThumbnail
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.formatDate
import io.github.darkxace01.sweepy.ui.formatDuration
import io.github.darkxace01.sweepy.ui.formatSize

/** Bottom sheet with details about a photo/video. EXIF camera info comes later. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(item: MediaItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val unknown = stringResource(R.string.info_unknown)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MediaThumbnail(item, Modifier.size(64.dp), cornerRadius = 16.dp)
                Column(Modifier.padding(start = 16.dp)) {
                    Text(
                        item.displayName ?: unknown,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.relativePath ?: item.folder ?: unknown,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            val facts = buildList {
                add(stringResource(R.string.info_date) to formatDate(item.dateTakenMillis))
                add(stringResource(R.string.info_size) to formatSize(context, item.sizeBytes))
                if (item.width > 0 && item.height > 0) {
                    add(stringResource(R.string.info_resolution) to "${item.width} × ${item.height}")
                }
                add(stringResource(R.string.info_type) to (item.mimeType?.substringAfter('/')?.uppercase() ?: unknown))
                if (item.isVideo) add(stringResource(R.string.info_duration) to formatDuration(item.durationMillis))
                add(stringResource(R.string.info_folder) to (item.folder ?: unknown))
            }
            facts.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    row.forEach { (label, value) -> FactTile(label, value, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .pressable({
                        val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(item.uri, item.mimeType)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            // No gallery/player app installed; nothing sensible to do.
                        }
                    }),
            ) {
                Icon(
                    painterResource(R.drawable.ic_open_external),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.info_open),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun FactTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
