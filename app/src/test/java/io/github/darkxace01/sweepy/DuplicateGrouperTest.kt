package io.github.darkxace01.sweepy

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.duplicates.DuplicateGrouper
import io.github.darkxace01.sweepy.data.duplicates.DuplicateKind
import io.github.darkxace01.sweepy.data.duplicates.KeepReason
import io.github.darkxace01.sweepy.data.duplicates.MediaSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The rules that decide what counts as a duplicate. Each test is one real-world situation. */
@RunWith(AndroidJUnit4::class)
class DuplicateGrouperTest {

    private val minute = 60_000L
    private val day = 24 * 60 * minute

    /** A hash with a healthy mix of bits, like a normal photo. */
    private val base = 0x5A3C_96F0_0F69_C3A5L

    private fun item(id: Long, takenAt: Long = 0, width: Int = 4000, height: Int = 3000, size: Long = 3_000_000L, video: Boolean = false, addedAt: Long = id) =
        MediaItem(
            id = id, uri = Uri.parse("content://media/$id"), isVideo = video, mimeType = "image/jpeg",
            displayName = "IMG_$id.jpg", folder = "Camera", bucketId = 1, relativePath = "DCIM/Camera/",
            sizeBytes = size, dateTakenMillis = takenAt, dateAddedSeconds = addedAt, width = width, height = height,
            durationMillis = 0,
        )

    /** [base] with [bits] low bits flipped: a picture that differs by that much. */
    private fun near(bits: Int): Long = base xor ((1L shl bits) - 1)

    private fun sig(item: MediaItem, visual: Long? = base, content: String? = null, sharpness: Float? = null, screenshot: Boolean = false) =
        MediaSignature(item, content, visual, sharpness, screenshot)

    @Test fun `identical files are exact copies, even screenshots and videos`() {
        val groups = DuplicateGrouper.group(
            listOf(
                sig(item(1, video = true), visual = null, content = "abc"),
                sig(item(2, video = true, takenAt = 5 * day), visual = null, content = "abc"),
                sig(item(3), visual = near(30), content = "shot", screenshot = true),
                sig(item(4, takenAt = 9 * day), visual = near(40), content = "shot", screenshot = true),
            ),
        )
        assertEquals(2, groups.size)
        assertTrue(groups.all { it.kind == DuplicateKind.EXACT })
    }

    @Test fun `a burst of slightly different shots is one look-alike group`() {
        val groups = DuplicateGrouper.group(
            listOf(sig(item(1, takenAt = 0)), sig(item(2, takenAt = 800), near(6)), sig(item(3, takenAt = 1_900), near(9))),
        )
        assertEquals(1, groups.size)
        assertEquals(DuplicateKind.SIMILAR, groups[0].kind)
        assertEquals(3, groups[0].items.size)
    }

    @Test fun `similar shots taken far apart only match when practically identical`() {
        val loose = DuplicateGrouper.group(listOf(sig(item(1, takenAt = 0)), sig(item(2, takenAt = 30 * day), near(6))))
        assertEquals("a near-match months apart is a different moment", 0, loose.size)

        val identical = DuplicateGrouper.group(listOf(sig(item(1, takenAt = 0)), sig(item(2, takenAt = 30 * day), near(2))))
        assertEquals("a re-saved copy matches even months later", 1, identical.size)
    }

    @Test fun `different-looking photos never group, even in the same second`() {
        val groups = DuplicateGrouper.group(listOf(sig(item(1)), sig(item(2, takenAt = 500), near(20))))
        assertEquals(0, groups.size)
    }

    @Test fun `look-alike screenshots are not grouped (only identical ones are)`() {
        val groups = DuplicateGrouper.group(
            listOf(sig(item(1), screenshot = true), sig(item(2, takenAt = 1_000), near(1), screenshot = true)),
        )
        assertEquals(0, groups.size)
    }

    @Test fun `near-blank pictures are not compared`() {
        val black = 0L
        val almostBlack = 0b111L
        val groups = DuplicateGrouper.group(listOf(sig(item(1), black), sig(item(2, takenAt = 1_000), almostBlack)))
        assertEquals(0, groups.size)
    }

    @Test fun `portrait and landscape versions are not look-alikes`() {
        val groups = DuplicateGrouper.group(listOf(sig(item(1)), sig(item(2, takenAt = 1_000, width = 3000, height = 4000))))
        assertEquals(0, groups.size)
    }

    @Test fun `videos only match as exact copies`() {
        val groups = DuplicateGrouper.group(listOf(sig(item(1, video = true)), sig(item(2, video = true, takenAt = 1_000))))
        assertEquals(0, groups.size)
    }

    @Test fun `exact copies suggest keeping the original`() {
        val group = DuplicateGrouper.group(
            listOf(sig(item(1, addedAt = 900), content = "x"), sig(item(2, addedAt = 100), content = "x"), sig(item(3, addedAt = 500), content = "x")),
        ).single()
        val (keep, reason) = DuplicateGrouper.suggest(group)
        assertEquals(2L, keep.id)
        assertEquals(KeepReason.ORIGINAL, reason)
    }

    @Test fun `a burst suggests the clearly sharpest shot`() {
        val group = DuplicateGrouper.group(
            listOf(sig(item(1), sharpness = 40f), sig(item(2, takenAt = 500), near(4), sharpness = 95f), sig(item(3, takenAt = 900), near(5), sharpness = 50f)),
        ).single()
        assertEquals(2L to KeepReason.SHARPEST, DuplicateGrouper.suggest(group).let { it.first.id to it.second })
    }

    @Test fun `when sharpness is too close to call, it falls back to most detail`() {
        val group = DuplicateGrouper.group(
            listOf(sig(item(1, size = 2_000_000), sharpness = 50f), sig(item(2, takenAt = 500, size = 4_000_000), near(4), sharpness = 52f)),
        ).single()
        val (keep, reason) = DuplicateGrouper.suggest(group)
        assertEquals(KeepReason.MOST_DETAIL, reason)
        assertEquals(2L, keep.id)
    }

    @Test fun `favorites always win the suggestion`() {
        val group = DuplicateGrouper.group(
            listOf(sig(item(1), sharpness = 90f), sig(item(2, takenAt = 500), near(4), sharpness = 10f)),
        ).single()
        val (keep, reason) = DuplicateGrouper.suggest(group) { it.id == 2L }
        assertEquals(2L, keep.id)
        assertEquals(KeepReason.FAVORITE, reason)
    }
}
