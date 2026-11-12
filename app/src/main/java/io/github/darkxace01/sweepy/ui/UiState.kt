package io.github.darkxace01.sweepy.ui

import io.github.darkxace01.sweepy.data.CleanStats
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.DeckFilter
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.MediaKind
import io.github.darkxace01.sweepy.data.ReelCard
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
    /** Decks that cut across folders: WhatsApp, large videos, maybe later. */
    val quickDecks: List<DeckSummary> = emptyList(),
    /** Voice notes, recordings and music; null when there are none (or no permission to see them). */
    val audio: DeckSummary? = null,
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
    val filter: DeckFilter = DeckFilter(),
    val defaultFilter: DeckFilter = DeckFilter(),
    /** Undecided photos and videos in this deck, before [filter] narrows them. */
    val waitingPhotos: Int = 0,
    val waitingVideos: Int = 0,
    /** The strip under the pile: what's waiting plus what was swiped this visit, in order. */
    val reel: List<ReelCard> = emptyList(),
    /** The frame of [reel] on show, or -1 when everything's done. */
    val position: Int = -1,
) {
    /** The card on top of the pile. Without a reel (as in some previews), the first one waiting. */
    val current: MediaItem? get() = if (reel.isEmpty()) deck.firstOrNull() else reel.getOrNull(position)?.item

    /** How the card on top was already swiped, when the reel was moved back to it. */
    val currentDecision: Decision? get() = reel.getOrNull(position)?.decision

    /** The next couple of cards still waiting, drawn underneath the top one. */
    val underneath: List<MediaItem>
        get() = if (reel.isEmpty()) deck.drop(1).take(2)
        else reel.drop(position + 1).filter { it.decision == null }.take(2).map { it.item }
    val isFiltered: Boolean get() = filter != defaultFilter

    /** Files still waiting that the current filter hides. */
    val hiddenCount: Int get() = waitingPhotos + waitingVideos - deck.size

    /** Only offer photos/videos when the deck actually has both. */
    val canFilterKind: Boolean get() = waitingPhotos > 0 && waitingVideos > 0 || filter.kind != MediaKind.ALL
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

    /** "Keep all" emptied the review bin; offers a short undo. [at] identifies the batch. */
    data class KeptAll(val count: Int, val at: Long) : UiEvent
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
