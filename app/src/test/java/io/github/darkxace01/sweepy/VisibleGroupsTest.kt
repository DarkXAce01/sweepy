package io.github.darkxace01.sweepy

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.db.DecisionEntity
import io.github.darkxace01.sweepy.data.db.DuplicateReviewEntity
import io.github.darkxace01.sweepy.data.duplicates.DuplicateGroup
import io.github.darkxace01.sweepy.data.duplicates.DuplicateKind
import io.github.darkxace01.sweepy.data.duplicates.MediaSignature
import io.github.darkxace01.sweepy.ui.visibleGroups
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** A duplicate group you've been through must not come back, unless a genuinely new copy shows up. */
@RunWith(AndroidJUnit4::class)
class VisibleGroupsTest {

    private val reviewedAt = 2_000_000_000L // ms

    private fun item(id: Long, addedSeconds: Long = 1_000) = MediaItem(
        id = id, uri = Uri.parse("content://media/$id"), isVideo = false, mimeType = null, displayName = null,
        folder = null, bucketId = 0, relativePath = null, sizeBytes = 1_000, dateTakenMillis = 0,
        dateAddedSeconds = addedSeconds, width = 4000, height = 3000, durationMillis = 0,
    )

    private fun group(vararg items: MediaItem) = DuplicateGroup(DuplicateKind.SIMILAR, items.map { MediaSignature(it, null, 0L, null, false) })

    private fun reviewed(vararg items: MediaItem) = items.associate { it.id to DuplicateReviewEntity(it.id, it.fingerprint, reviewedAt) }

    private val keep = { item: MediaItem -> item.id to DecisionEntity(item.id, Decision.KEEP, item.fingerprint, 0) }

    @Test fun `a group you kept several of stays hidden`() {
        val a = item(1); val b = item(2); val c = item(3)
        val shown = visibleGroups(listOf(group(a, b, c)), mapOf(keep(a), keep(b), keep(c)), reviewed(a, b, c))
        assertEquals(0, shown.size)
    }

    @Test fun `an older photo joining the group doesn't bring it back`() {
        val a = item(1); val b = item(2); val back = item(3) // e.g. taken out of the review bin
        val shown = visibleGroups(listOf(group(a, b, back)), mapOf(keep(a), keep(b), keep(back)), reviewed(a, b))
        assertEquals(0, shown.size)
    }

    @Test fun `a new copy added afterwards does`() {
        val a = item(1); val b = item(2); val fresh = item(3, addedSeconds = reviewedAt / 1000 + 60)
        val shown = visibleGroups(listOf(group(a, b, fresh)), mapOf(keep(a), keep(b)), reviewed(a, b))
        assertEquals(1, shown.size)
        assertEquals(3, shown[0].items.size)
    }

    @Test fun `groups you haven't seen are shown`() {
        assertEquals(1, visibleGroups(listOf(group(item(1), item(2))), emptyMap(), emptyMap()).size)
    }
}
