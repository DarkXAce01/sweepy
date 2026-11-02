package io.github.darkxace01.sweepy.data.reminders

/**
 * When Sweepy is allowed to send a reminder, and which one. Everything here is plain arithmetic on
 * numbers worked out on the phone, so it can be tested without Android.
 *
 * The rules, in short:
 *  - only between 10:00 and 21:00,
 *  - at most one a week,
 *  - never while someone is using the app anyway (opened in the last 3 days; 1 day when storage is tight),
 *  - after two reminders in a row go unanswered, nothing more until the app is opened again,
 *  - and only when there's something real to do, so a quiet gallery means no reminders at all.
 */
enum class ReminderKind {
    /** The phone is nearly full and big videos haven't been looked at. */
    STORAGE,

    /** Things already swiped left are still sitting in the review bin. */
    BIN,

    /** Lots of new screenshots. */
    SCREENSHOTS,

    /** Lots of new photos and videos. */
    NEW_PHOTOS,
}

data class Reminder(val kind: ReminderKind, val count: Int = 0, val bytes: Long = 0, val percentFull: Int = 0)

/** The cheap part: whether it's worth looking at the gallery at all right now. */
data class ReminderTiming(
    val now: Long,
    /** Local hour of day, 0–23. */
    val hour: Int,
    /** 0 if the app has never recorded an open (e.g. right after an update). */
    val lastOpenedAt: Long,
    val lastReminderAt: Long,
    /** Reminders sent since the app was last opened. */
    val unanswered: Int,
)

/** What's on the phone, counted from the gallery and the decisions. */
data class ReminderFacts(
    val binCount: Int,
    val binBytes: Long,
    /** When the most recent thing went into the review bin. */
    val binNewestAt: Long,
    /** Unsorted photos and videos added since the app was last opened. */
    val newSinceOpen: Int,
    /** How many of those are screenshots. */
    val newScreenshotsSinceOpen: Int,
    /** How full the phone's storage is, 0–100. */
    val percentFull: Int,
    /** Total size of big videos that haven't been sorted yet. */
    val bigVideoBytes: Long,
)

object Reminders {
    const val DAY = 24L * 60 * 60 * 1000

    const val QUIET_BEFORE_HOUR = 10
    const val QUIET_FROM_HOUR = 21
    const val MIN_GAP = 7 * DAY
    const val IDLE_BEFORE_REMINDING = 3 * DAY
    const val IDLE_BEFORE_STORAGE = 1 * DAY
    const val MAX_UNANSWERED = 2

    const val STORAGE_PERCENT = 90
    const val STORAGE_VIDEO_BYTES = 300L * 1024 * 1024
    const val BIN_MIN_COUNT = 10
    const val BIN_SETTLE = 2 * DAY
    const val SCREENSHOTS_MIN = 40
    const val NEW_PHOTOS_MIN = 150

    /** False when it's the wrong time, too soon, or the app is in regular use: then don't even look. */
    fun mayCheck(t: ReminderTiming): Boolean =
        t.hour in QUIET_BEFORE_HOUR until QUIET_FROM_HOUR &&
            t.lastOpenedAt > 0 &&
            t.unanswered < MAX_UNANSWERED &&
            t.now - t.lastReminderAt >= MIN_GAP &&
            t.now - t.lastOpenedAt >= IDLE_BEFORE_STORAGE

    /** The one reminder most worth sending, or null when there's nothing that needs doing. */
    fun pick(t: ReminderTiming, f: ReminderFacts): Reminder? {
        if (!mayCheck(t)) return null
        val idle = t.now - t.lastOpenedAt
        return when {
            f.percentFull >= STORAGE_PERCENT && f.bigVideoBytes >= STORAGE_VIDEO_BYTES ->
                Reminder(ReminderKind.STORAGE, bytes = f.bigVideoBytes, percentFull = f.percentFull)
            idle < IDLE_BEFORE_REMINDING -> null
            f.binCount >= BIN_MIN_COUNT && t.now - f.binNewestAt >= BIN_SETTLE ->
                Reminder(ReminderKind.BIN, count = f.binCount, bytes = f.binBytes)
            f.newScreenshotsSinceOpen >= SCREENSHOTS_MIN ->
                Reminder(ReminderKind.SCREENSHOTS, count = f.newScreenshotsSinceOpen)
            f.newSinceOpen >= NEW_PHOTOS_MIN ->
                Reminder(ReminderKind.NEW_PHOTOS, count = f.newSinceOpen)
            else -> null
        }
    }
}
