package io.github.darkxace01.sweepy

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.darkxace01.sweepy.data.DeckFilter
import io.github.darkxace01.sweepy.data.DeckOrder
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Sorting and narrowing a deck. */
@RunWith(AndroidJUnit4::class)
class DeckFilterTest {

    private fun item(id: Long, size: Long, video: Boolean = false) = MediaItem(
        id = id, uri = Uri.parse("content://media/$id"), isVideo = video, mimeType = null, displayName = null,
        folder = null, bucketId = 0, relativePath = null, sizeBytes = size, dateTakenMillis = 0,
        dateAddedSeconds = 0, width = 0, height = 0, durationMillis = 0,
    )

    /** Newest first, the way the library is loaded. */
    private val library = listOf(item(1, 300), item(2, 900, video = true), item(3, 100), item(4, 500, video = true))

    private fun DeckFilter.apply() = sort(library.filter(::matches)).map { it.id }

    @Test fun `the default shows everything, newest first`() {
        assertEquals(listOf(1L, 2L, 3L, 4L), DeckFilter().apply())
    }

    @Test fun `photos only and videos only`() {
        assertEquals(listOf(1L, 3L), DeckFilter(kind = MediaKind.PHOTOS).apply())
        assertEquals(listOf(2L, 4L), DeckFilter(kind = MediaKind.VIDEOS).apply())
    }

    @Test fun `oldest first and largest first`() {
        assertEquals(listOf(4L, 3L, 2L, 1L), DeckFilter(order = DeckOrder.OLDEST).apply())
        assertEquals(listOf(2L, 4L, 1L, 3L), DeckFilter(order = DeckOrder.LARGEST).apply())
        assertEquals(listOf(1L, 3L), DeckFilter(MediaKind.PHOTOS, DeckOrder.LARGEST).apply())
    }

    @Test fun `the storage hogs deck starts biggest first`() {
        assertEquals(DeckOrder.LARGEST, DeckFilter.defaultFor(DeckSource.LargeVideos).order)
        assertEquals(DeckOrder.NEWEST, DeckFilter.defaultFor(DeckSource.All).order)
    }
}
