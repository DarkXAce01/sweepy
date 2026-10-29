package io.github.darkxace01.sweepy.data

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.provider.BaseColumns
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads photos/videos from Android's MediaStore and asks the system to trash them. */
class MediaRepository(private val context: Context) {

    /** Every photo and video we're allowed to see, newest first. Trashed/pending items are excluded by Android. */
    suspend fun loadAll(): List<MediaItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            BaseColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val items = ArrayList<MediaItem>()
        context.contentResolver.query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val folderCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val isVideo = cursor.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val baseUri = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                // DATE_TAKEN is in milliseconds but often missing (e.g. WhatsApp images);
                // DATE_ADDED is in seconds and always present.
                val taken = cursor.getLong(takenCol)
                items += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(baseUri, id),
                    isVideo = isVideo,
                    mimeType = cursor.getString(mimeCol),
                    displayName = cursor.getString(nameCol),
                    folder = cursor.getString(folderCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    dateTakenMillis = if (taken > 0) taken else cursor.getLong(addedCol) * 1000,
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    durationMillis = if (isVideo) cursor.getLong(durationCol) else 0,
                )
            }
        }
        items
    }

    /**
     * Builds the system "Move to trash?" confirmation for [items].
     * Trashed items can be restored for about 30 days.
     */
    fun createTrashRequest(items: Collection<MediaItem>): PendingIntent =
        MediaStore.createTrashRequest(context.contentResolver, items.map { it.uri }, true)
}
