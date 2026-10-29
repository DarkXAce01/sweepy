package io.github.darkxace01.sweepy.data

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/** A photo/video sitting in Android's system trash. */
data class TrashedItem(val media: MediaItem, val expiresAtMillis: Long)

/** Reads photos/videos from Android's MediaStore and asks the system to trash, restore or delete them. */
class MediaRepository(private val context: Context) {

    private val resolver: ContentResolver get() = context.contentResolver
    private val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    /** Every photo and video we're allowed to see, newest first. Trashed/pending items are excluded. */
    suspend fun loadAll(): List<MediaItem> = withContext(Dispatchers.IO) {
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MEDIA_SELECTION)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, MEDIA_SELECTION_ARGS)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
        }
        resolver.query(collection, PROJECTION, args, null)?.use { cursor ->
            val columns = Columns(cursor)
            buildList(cursor.count) { while (cursor.moveToNext()) add(columns.read(cursor)) }
        }.orEmpty()
    }

    /** Items in the system trash, soonest-to-expire first. */
    suspend fun loadTrashed(): List<TrashedItem> = withContext(Dispatchers.IO) {
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MEDIA_SELECTION)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, MEDIA_SELECTION_ARGS)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.MediaColumns.DATE_EXPIRES} ASC")
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }
        resolver.query(collection, PROJECTION + MediaStore.MediaColumns.DATE_EXPIRES, args, null)?.use { cursor ->
            val columns = Columns(cursor)
            val expiresCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES)
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(TrashedItem(columns.read(cursor), cursor.getLong(expiresCol) * 1000))
                }
            }
        }.orEmpty()
    }

    /** System confirmation for moving [items] to the trash (restorable for about 30 days). */
    fun createTrashRequest(items: Collection<MediaItem>): PendingIntent =
        MediaStore.createTrashRequest(resolver, items.map { it.uri }, true)

    /** System confirmation for taking [items] back out of the trash. */
    fun createRestoreRequest(items: Collection<MediaItem>): PendingIntent =
        MediaStore.createTrashRequest(resolver, items.map { it.uri }, false)

    /** System confirmation for deleting [items] permanently. Cannot be undone. */
    fun createDeleteRequest(items: Collection<MediaItem>): PendingIntent =
        MediaStore.createDeleteRequest(resolver, items.map { it.uri })

    /** Emits whenever photos/videos are added, changed or removed by any app. */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(collection, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    private class Columns(cursor: Cursor) {
        val id = cursor.getColumnIndexOrThrow(BaseColumns._ID)
        val type = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val mime = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        val name = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val folder = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        val bucketId = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
        val path = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
        val size = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val taken = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
        val added = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val width = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
        val height = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
        val duration = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)

        fun read(cursor: Cursor): MediaItem {
            val id = cursor.getLong(id)
            val isVideo = cursor.getInt(type) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            val baseUri = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            // DATE_TAKEN is in milliseconds but often missing (e.g. WhatsApp images);
            // DATE_ADDED is in seconds and always present.
            val addedSeconds = cursor.getLong(added)
            val takenMillis = cursor.getLong(taken)
            return MediaItem(
                id = id,
                uri = ContentUris.withAppendedId(baseUri, id),
                isVideo = isVideo,
                mimeType = cursor.getString(mime),
                displayName = cursor.getString(name),
                folder = cursor.getString(folder),
                bucketId = cursor.getLong(bucketId),
                relativePath = cursor.getString(path),
                sizeBytes = cursor.getLong(size),
                dateTakenMillis = if (takenMillis > 0) takenMillis else addedSeconds * 1000,
                dateAddedSeconds = addedSeconds,
                width = cursor.getInt(width),
                height = cursor.getInt(height),
                durationMillis = if (isVideo) cursor.getLong(duration) else 0,
            )
        }
    }

    private companion object {
        val PROJECTION = arrayOf(
            BaseColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
        )
        const val MEDIA_SELECTION = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val MEDIA_SELECTION_ARGS = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
    }
}
