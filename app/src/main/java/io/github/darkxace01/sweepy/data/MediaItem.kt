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
    val bucketId: Long,
    /** Path inside storage, e.g. "DCIM/Screenshots/". */
    val relativePath: String?,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val dateAddedSeconds: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long,
) {
    /** Year and month the photo was taken, as yyyyMM (e.g. 202403). */
    val yearMonth: Int get() = yearMonthOf(dateTakenMillis)

    /**
     * Android can reuse IDs (e.g. after a media rescan), so a saved decision only counts
     * if the file still looks the same.
     */
    val fingerprint: String get() = "$sizeBytes:$dateAddedSeconds"
}

private fun yearMonthOf(millis: Long): Int {
    val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
    return date.year * 100 + date.monthValue
}
