package io.github.darkxace01.sweepy.ui.deck

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.CameraInfo
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.readCameraInfo
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.Hairline
import io.github.darkxace01.sweepy.ui.components.MediaThumbnail
import io.github.darkxace01.sweepy.ui.components.OutlineButton
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import io.github.darkxace01.sweepy.ui.formatDate
import io.github.darkxace01.sweepy.ui.formatDuration
import io.github.darkxace01.sweepy.ui.formatSize

/** Bottom sheet with details about a photo/video, including camera settings when the photo has them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(item: MediaItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val unknown = stringResource(R.string.info_unknown)
    val camera by produceState<CameraInfo?>(null, item.id) { value = readCameraInfo(context, item) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Sweepy.colors.surface,
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(width = 36.dp, height = 3.dp).background(Sweepy.colors.hairline)) },
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MediaThumbnail(item, Modifier.size(72.dp), cornerRadius = 6.dp)
                Column(Modifier.padding(start = 16.dp)) {
                    Eyebrow(item.relativePath ?: item.folder ?: unknown, small = true)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.displayName ?: unknown,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Sweepy.colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            val facts = buildList {
                add(stringResource(R.string.info_date) to formatDate(item.dateTakenMillis))
                add(stringResource(R.string.info_size) to formatSize(context, item.sizeBytes))
                if (item.width > 0 && item.height > 0) {
                    add(stringResource(R.string.info_resolution) to "${item.width} × ${item.height}")
                }
                add(stringResource(R.string.info_type) to (item.mimeType ?: unknown))
                if (item.isVideo) add(stringResource(R.string.info_duration) to formatDuration(item.durationMillis))
                add(stringResource(R.string.info_folder) to (item.folder ?: unknown))
                camera?.camera?.let { add(stringResource(R.string.info_camera) to it) }
                camera?.lens?.let { add(stringResource(R.string.info_lens) to it) }
            }
            // Exposure settings read like the data strip on the edge of a negative.
            val exposure = camera?.let { listOfNotNull(it.aperture, it.shutter, it.iso, it.focalLength) }.orEmpty()
            if (exposure.isNotEmpty()) {
                Text(
                    exposure.joinToString("   "),
                    style = MaterialTheme.typography.labelMedium,
                    color = Sweepy.colors.paper,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Sweepy.colors.ink)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
            // A spec sheet: label on the left, value on the right, hairlines between.
            facts.forEachIndexed { index, (label, value) ->
                if (index > 0) Hairline()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Eyebrow(label, small = true, modifier = Modifier.width(110.dp))
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Sweepy.colors.ink,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlineButton(
                text = stringResource(R.string.info_open) + "  ↗",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(item.uri, item.mimeType)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        // No gallery/player app installed; nothing sensible to do.
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
