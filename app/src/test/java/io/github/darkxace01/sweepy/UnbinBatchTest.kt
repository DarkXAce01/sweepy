package io.github.darkxace01.sweepy

import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.UnbinRecord
import io.github.darkxace01.sweepy.data.db.DecisionEntity
import io.github.darkxace01.sweepy.ui.unbinBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Finding the photos a "Keep all" took out of the review bin, so they can be put back. */
class UnbinBatchTest {

    private fun keeps(count: Int, at: Long, from: Long = 0) = List(count) { DecisionEntity(from + it, Decision.KEEP, "f", at) }

    /** Ordinary swipes: one keep at a time, each with its own timestamp. */
    private val swipes = List(300) { DecisionEntity(10_000L + it, Decision.KEEP, "f", 1_000L + it) }

    @Test fun `a recorded Keep all is offered until it's settled`() {
        val decisions = swipes + keeps(1_000, at = 5_000)
        assertEquals(5_000L, unbinBatch(decisions, UnbinRecord(unbinnedAt = 5_000)))
        assertNull(unbinBatch(decisions, UnbinRecord(unbinnedAt = 5_000, settledAt = 5_000)))
    }

    @Test fun `an unrecorded Keep all from an older version is found by its shared timestamp`() {
        assertEquals(5_000L, unbinBatch(swipes + keeps(1_000, at = 5_000), UnbinRecord()))
    }

    @Test fun `ordinary swipes and small batches are never mistaken for a Keep all`() {
        assertNull(unbinBatch(swipes + keeps(3, at = 5_000), UnbinRecord()))
    }

    @Test fun `once a newer Keep all is settled, the old unrecorded one isn't offered again`() {
        val decisions = swipes + keeps(1_000, at = 5_000) + keeps(40, at = 9_000, from = 50_000)
        assertNull(unbinBatch(decisions, UnbinRecord(unbinnedAt = 9_000, settledAt = 9_000)))
    }
}
