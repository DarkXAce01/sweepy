package io.github.darkxace01.sweepy

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.darkxace01.sweepy.data.DeckFilter
import io.github.darkxace01.sweepy.data.DeckOrder
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.MediaKind
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.ReelCard
import io.github.darkxace01.sweepy.data.buildReel
import io.github.darkxace01.sweepy.data.nextWaiting
import io.github.darkxace01.sweepy.data.reelPosition
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

    private val reel = library.mapIndexed { i, item -> ReelCard(item, if (i == 1) Decision.DELETE else null) }

    @Test fun `the reel opens on the first photo still waiting`() {
        assertEquals(0, reelPosition(reel, cursor = null))
        val firstTwoSwiped = reel.mapIndexed { i, card -> if (i == 0) card.copy(decision = Decision.KEEP) else card }
        assertEquals(2, reelPosition(firstTwoSwiped, cursor = null))
    }

    @Test fun `scrolling the reel back keeps every frame in place`() {
        // Moved back to the swiped photo: it shows, and nothing before or after it is dropped.
        assertEquals(1, reelPosition(reel, cursor = 2))
        assertEquals(listOf(1L, 2L, 3L, 4L), reel.map { it.item.id })
    }

    @Test fun `after a swipe the reel moves on, then wraps round to photos skipped earlier`() {
        assertEquals(3L, nextWaiting(reel, from = 0)) // the next one waiting, skipping the swiped one
        assertEquals(4L, nextWaiting(reel, from = 2))
        assertEquals(1L, nextWaiting(reel, from = 3)) // nothing after: back to the first skipped
        val allSwiped = reel.map { it.copy(decision = Decision.KEEP) }
        assertEquals(null, nextWaiting(allSwiped, from = 3))
    }

    @Test fun `the reel holds what's waiting plus what was swiped here, in order`() {
        val decided = mapOf(2L to Decision.DELETE, 3L to Decision.KEEP)
        val built = buildReel(library, { it.id !in decided }, { decided[it.id] }, swipedHere = setOf(2L))
        assertEquals(listOf(1L, 2L, 4L), built.map { it.item.id })
        assertEquals(Decision.DELETE, built[1].decision)
    }

    @Test fun `audio only ever shows up in the audio deck`() {
        val song = item(9, 4_000).copy(isAudio = true)
        assertEquals(true, DeckSource.Audio.matches(song))
        assertEquals(false, DeckSource.All.matches(song))
        assertEquals(false, DeckSource.Folder(0, "Music").matches(song))
        assertEquals(false, DeckSource.Audio.matches(library[0]))
        assertEquals(true, DeckSource.Later.matches(song))
    }

    @Test fun `the storage hogs deck starts biggest first`() {
        assertEquals(DeckOrder.LARGEST, DeckFilter.defaultFor(DeckSource.LargeVideos).order)
        assertEquals(DeckOrder.NEWEST, DeckFilter.defaultFor(DeckSource.All).order)
    }
}
