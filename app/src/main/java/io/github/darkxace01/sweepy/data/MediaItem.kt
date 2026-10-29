package io.github.darkxace01.sweepy.data

import android.net.Uri

/** One photo or video on the phone, as reported by Android's MediaStore. */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val mimeType: String?,
    val displayName: String?,
    /** Folder the file lives in, e.g. "WhatsApp Images", "Screenshots", "Camera". */
    val folder: String?,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long,
)
