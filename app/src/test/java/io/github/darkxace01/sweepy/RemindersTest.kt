package io.github.darkxace01.sweepy

import io.github.darkxace01.sweepy.data.reminders.ReminderFacts
import io.github.darkxace01.sweepy.data.reminders.ReminderKind
import io.github.darkxace01.sweepy.data.reminders.ReminderTiming
import io.github.darkxace01.sweepy.data.reminders.Reminders
import io.github.darkxace01.sweepy.data.reminders.Reminders.DAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemindersTest {
    private val now = 1_000 * DAY

    /** Away for 5 days, last reminder long ago, mid-afternoon: a reminder is allowed. */
    private val idle = ReminderTiming(now = now, hour = 15, lastOpenedAt = now - 5 * DAY, lastReminderAt = 0, unanswered = 0)

    private val nothing = ReminderFacts(
        binCount = 0, binBytes = 0, binNewestAt = 0, newSinceOpen = 0, newScreenshotsSinceOpen = 0,
        percentFull = 50, bigVideoBytes = 0,
    )
    private val screenshots = nothing.copy(newSinceOpen = 60, newScreenshotsSinceOpen = 45)

    @Test fun quietGalleryMeansNoReminder() = assertNull(Reminders.pick(idle, nothing))

    @Test fun screenshotsPilingUp() = assertEquals(ReminderKind.SCREENSHOTS, Reminders.pick(idle, screenshots)?.kind)

    @Test fun lotsOfNewPhotos() =
        assertEquals(ReminderKind.NEW_PHOTOS, Reminders.pick(idle, nothing.copy(newSinceOpen = 200))?.kind)

    @Test fun neverAtNight() {
        for (hour in listOf(0, 6, 9, 21, 23)) assertNull("hour $hour", Reminders.pick(idle.copy(hour = hour), screenshots))
        for (hour in listOf(10, 20)) assertEquals(ReminderKind.SCREENSHOTS, Reminders.pick(idle.copy(hour = hour), screenshots)?.kind)
    }

    @Test fun atMostOnceAWeek() {
        assertNull(Reminders.pick(idle.copy(lastReminderAt = now - 6 * DAY), screenshots))
        assertEquals(ReminderKind.SCREENSHOTS, Reminders.pick(idle.copy(lastReminderAt = now - 7 * DAY), screenshots)?.kind)
    }

    @Test fun notWhileTheAppIsInUse() = assertNull(Reminders.pick(idle.copy(lastOpenedAt = now - 2 * DAY), screenshots))

    @Test fun stopsAfterTwoIgnored() {
        assertEquals(ReminderKind.SCREENSHOTS, Reminders.pick(idle.copy(unanswered = 1), screenshots)?.kind)
        assertNull(Reminders.pick(idle.copy(unanswered = 2), screenshots))
    }

    @Test fun neverBeforeTheAppHasBeenOpened() = assertNull(Reminders.pick(idle.copy(lastOpenedAt = 0), screenshots))

    @Test fun fullBinWinsOverNewPhotos() {
        val bin = screenshots.copy(binCount = 30, binBytes = 90_000_000, binNewestAt = now - 4 * DAY)
        val reminder = Reminders.pick(idle, bin)
        assertEquals(ReminderKind.BIN, reminder?.kind)
        assertEquals(30, reminder?.count)
    }

    @Test fun binLeftAloneJustAfterSwiping() {
        val bin = nothing.copy(binCount = 30, binNewestAt = now - 1 * DAY)
        assertNull(Reminders.pick(idle, bin))
    }

    @Test fun lowStorageComesFirstAndSooner() {
        val full = screenshots.copy(percentFull = 94, bigVideoBytes = 2_000_000_000)
        assertEquals(ReminderKind.STORAGE, Reminders.pick(idle, full)?.kind)
        // Opened yesterday: too soon for anything else, but a nearly full phone is worth a word.
        assertEquals(ReminderKind.STORAGE, Reminders.pick(idle.copy(lastOpenedAt = now - DAY), full)?.kind)
        // Full, but not because of videos Sweepy could help with.
        assertEquals(ReminderKind.SCREENSHOTS, Reminders.pick(idle, full.copy(bigVideoBytes = 0))?.kind)
    }
}
