package io.github.darkxace01.sweepy.ui.deck

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.formatDate
import io.github.darkxace01.sweepy.ui.formatDuration
import io.github.darkxace01.sweepy.ui.formatSize

/** Bottom sheet with details about a photo/video. EXIF camera info comes later. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(item: MediaItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val unknown = stringResource(R.string.info_unknown)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            InfoRow(stringResource(R.string.info_name), item.displayName ?: unknown)
            InfoRow(stringResource(R.string.info_folder), item.folder ?: unknown)
            InfoRow(stringResource(R.string.info_date), formatDate(item.dateTakenMillis))
            InfoRow(stringResource(R.string.info_size), formatSize(context, item.sizeBytes))
            if (item.width > 0 && item.height > 0) {
                InfoRow(stringResource(R.string.info_resolution), "${item.width} × ${item.height}")
            }
            InfoRow(stringResource(R.string.info_type), item.mimeType ?: unknown)
            if (item.isVideo) {
                InfoRow(stringResource(R.string.info_duration), formatDuration(item.durationMillis))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
