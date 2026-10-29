package io.github.darkxace01.sweepy.data

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/** Camera settings from a photo's EXIF data. Every field is optional; many photos have none. */
data class CameraInfo(
    val camera: String?,
    val lens: String?,
    val aperture: String?,
    val shutter: String?,
    val iso: String?,
    val focalLength: String?,
) {
    val isEmpty: Boolean
        get() = listOf(camera, lens, aperture, shutter, iso, focalLength).all { it == null }
}

/** Reads EXIF from [item]. Returns null for videos, unreadable files, or photos without camera data. */
suspend fun readCameraInfo(context: Context, item: MediaItem): CameraInfo? {
    if (item.isVideo) return null
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                CameraInfo(
                    camera = cameraName(exif.getAttribute(ExifInterface.TAG_MAKE), exif.getAttribute(ExifInterface.TAG_MODEL)),
                    lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()?.takeIf { it.isNotEmpty() },
                    aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                        .takeIf { it > 0 }?.let { "ƒ/" + String.format(Locale.ROOT, "%.1f", it).removeSuffix(".0") },
                    shutter = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                        .takeIf { it > 0 }?.let(::formatShutter),
                    iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                        .takeIf { it > 0 }?.let { "ISO $it" },
                    focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                        .takeIf { it > 0 }?.let { "${it.roundToInt()} mm" },
                ).takeUnless { it.isEmpty }
            }
        } catch (_: Exception) {
            // Corrupt or unsupported file: just show no camera details.
            null
        }
    }
}

/** "samsung" + "SM-S721B" → "Samsung SM-S721B"; avoids "Apple Apple iPhone" style repeats. */
private fun cameraName(make: String?, model: String?): String? {
    val m = make?.trim().orEmpty().replaceFirstChar { it.titlecase(Locale.ROOT) }
    val mo = model?.trim().orEmpty()
    return when {
        mo.isEmpty() -> m.ifEmpty { null }
        m.isEmpty() || mo.startsWith(m, ignoreCase = true) -> mo
        else -> "$m $mo"
    }
}

private fun formatShutter(seconds: Double): String =
    if (seconds >= 1) String.format(Locale.ROOT, "%.1f s", seconds).replace(".0 s", " s")
    else "1/${(1 / seconds).roundToInt()} s"

