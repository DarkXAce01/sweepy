package io.github.darkxace01.sweepy.data

/**
 * What a swipe deck shows. Decisions are stored per photo, not per deck, so every deck
 * sees the same decisions: a photo handled in one deck never shows up in another.
 */
sealed interface DeckSource {
    data object All : DeckSource
    data object Screenshots : DeckSource
    data object WhatsApp : DeckSource
    data object LargeVideos : DeckSource

    /** The "ask me later" pile. Unlike other decks, it shows items that *have* a decision (LATER). */
    data object Later : DeckSource
    data class Folder(val bucketId: Long, val name: String) : DeckSource

    /** [yearMonth] as yyyyMM, e.g. 202403. */
    data class Month(val yearMonth: Int) : DeckSource

    fun matches(item: MediaItem): Boolean = when (this) {
        All, Later -> true
        Screenshots -> item.relativePath?.contains("screenshot", ignoreCase = true) == true ||
            item.folder.equals("Screenshots", ignoreCase = true)
        WhatsApp -> item.relativePath?.contains("whatsapp", ignoreCase = true) == true
        LargeVideos -> item.isVideo && item.sizeBytes >= LARGE_VIDEO_BYTES
        is Folder -> item.bucketId == bucketId
        is Month -> item.yearMonth == yearMonth
    }

    /** Biggest first for the "storage hogs" deck, newest first everywhere else. */
    val sortBySize: Boolean get() = this == LargeVideos

    companion object {
        const val LARGE_VIDEO_BYTES = 25L * 1024 * 1024
    }
}
