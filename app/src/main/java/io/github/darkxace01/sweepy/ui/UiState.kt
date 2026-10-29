package io.github.darkxace01.sweepy.ui

import io.github.darkxace01.sweepy.data.CleanStats
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.TrashedItem
import io.github.darkxace01.sweepy.data.duplicates.DuplicateGroup
import io.github.darkxace01.sweepy.data.duplicates.KeepReason

/** Size and progress of one deck, for the home screen. */
data class DeckSummary(
    val source: DeckSource,
    val totalCount: Int,
    val totalBytes: Long,
    /** Items in this deck still waiting for a decision. */
    val remainingCount: Int,
    /** Up to four items to show as the deck's cover. */
    val covers: List<MediaItem>,
) {
    val cover: MediaItem? get() = covers.firstOrNull()
    val progress: Float get() = if (totalCount == 0) 1f else (totalCount - remainingCount) / totalCount.toFloat()
}

data class HomeUiState(
    val loading: Boolean = true,
    val totalCount: Int = 0,
    val totalBytes: Long = 0,
    val reviewedCount: Int = 0,
    val markedCount: Int = 0,
    val markedBytes: Long = 0,
    val laterCount: Int = 0,
    val stats: CleanStats = CleanStats(),
    /** Newest items still waiting, for the strip on the home screen. */
    val recent: List<MediaItem> = emptyList(),
    val quickDecks: List<DeckSummary> = emptyList(),
    val folders: List<DeckSummary> = emptyList(),
    val months: List<DeckSummary> = emptyList(),
    val duplicates: DuplicatesSummary = DuplicatesSummary(),
) {
    val progress: Float get() = if (totalCount == 0) 0f else reviewedCount / totalCount.toFloat()
}

data class DeckUiState(
    val loading: Boolean = true,
    /** Items still to swipe, in order. */
    val deck: List<MediaItem> = emptyList(),
    /** Everything in this deck, decided or not. */
    val totalCount: Int = 0,
    /** Size of the review bin (shared by all decks). */
    val markedCount: Int = 0,
    val markedBytes: Long = 0,
    val canUndo: Boolean = false,
) {
    val current: MediaItem? get() = deck.firstOrNull()
    val remainingBytes: Long get() = deck.sumOf { it.sizeBytes }
    val progress: Float get() = if (totalCount == 0) 1f else (totalCount - deck.size) / totalCount.toFloat()
}

data class ReviewGroup(val folder: String?, val items: List<MediaItem>) {
    val bytes: Long get() = items.sumOf { it.sizeBytes }
}

data class ReviewUiState(
    val groups: List<ReviewGroup> = emptyList(),
    val count: Int = 0,
    val bytes: Long = 0,
)

data class TrashUiState(
    val loading: Boolean = true,
    val items: List<TrashedItem> = emptyList(),
) {
    val totalBytes: Long get() = items.sumOf { it.media.sizeBytes }
}

/** One-off messages shown as a snackbar. */
sealed interface UiEvent {
    data class Trashed(val count: Int, val bytes: Long) : UiEvent
    data class Restored(val count: Int) : UiEvent
    data class DeletedForever(val count: Int, val bytes: Long) : UiEvent
}

/** One duplicate group ready to show, with the photo we'd suggest keeping. */
data class DuplicateGroupUi(
    val group: DuplicateGroup,
    val suggested: MediaItem,
    val reason: KeepReason,
    /** Favorites in this group; they can't be binned. */
    val favorites: Set<Long>,
) {
    val items: List<MediaItem> get() = group.items
    val key: String get() = items.joinToString(",") { it.id.toString() }

    /** Space freed by keeping only the suggestion. */
    val reclaimableBytes: Long get() = group.totalBytes - suggested.sizeBytes
}

data class DuplicatesUiState(
    val scanning: Boolean = true,
    /** 0..1 while reading files, null otherwise. */
    val progress: Float? = null,
    val groups: List<DuplicateGroupUi> = emptyList(),
    val canUndo: Boolean = false,
) {
    fun summary() = DuplicatesSummary(
        scanning = scanning,
        progress = progress,
        groupCount = groups.size,
        reclaimableBytes = groups.sumOf { it.reclaimableBytes },
        covers = groups.take(4).map { it.suggested },
    )
}

/** What the home screen shows on the duplicates tile. */
data class DuplicatesSummary(
    val scanning: Boolean = true,
    val progress: Float? = null,
    val groupCount: Int = 0,
    val reclaimableBytes: Long = 0,
    val covers: List<MediaItem> = emptyList(),
)
