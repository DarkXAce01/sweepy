package io.github.darkxace01.sweepy.ui.deck

import android.app.Application
import android.app.PendingIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Decision { KEEP, DELETE, LATER, FAVORITE }

data class DeckUiState(
    val loading: Boolean = true,
    /** Items not decided yet, in the order they'll be shown. */
    val deck: List<MediaItem> = emptyList(),
    /** Items swiped left, waiting in the review bin. Nothing is deleted yet. */
    val marked: List<MediaItem> = emptyList(),
    val canUndo: Boolean = false,
    /** Total size moved to the system trash during this run of the app. */
    val trashedBytes: Long = 0,
) {
    val current: MediaItem? get() = deck.firstOrNull()
    val next: MediaItem? get() = deck.getOrNull(1)
    val markedBytes: Long get() = marked.sumOf { it.sizeBytes }
}

/**
 * Holds the swipe deck and every decision made.
 *
 * TODO(next step): decisions only live in memory for now, so they're forgotten when the app closes.
 * They'll move into a Room database (the global "decision ledger") so they persist across sessions.
 */
class DeckViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaRepository(app)

    private var allMedia: List<MediaItem> = emptyList()

    // LinkedHashMap remembers insertion order, so the last entry is always the most recent swipe (for undo).
    private val decisions = LinkedHashMap<Long, Decision>()

    private var pendingTrash: List<MediaItem> = emptyList()
    private var trashedBytes = 0L

    private val _state = MutableStateFlow(DeckUiState())
    val state: StateFlow<DeckUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            allMedia = repository.loadAll()
            // Forget decisions for items that no longer exist (deleted by another app, trashed, etc.).
            val existing = allMedia.mapTo(HashSet()) { it.id }
            decisions.keys.retainAll(existing)
            publish(loading = false)
        }
    }

    fun decide(item: MediaItem, decision: Decision) {
        decisions.remove(item.id)
        decisions[item.id] = decision
        publish()
    }

    fun undo() {
        val last = decisions.keys.lastOrNull() ?: return
        decisions.remove(last)
        publish()
    }

    /** Take an item back out of the review bin. Keeps its place in the undo history. */
    fun unmark(item: MediaItem) {
        if (decisions[item.id] == Decision.DELETE) {
            decisions[item.id] = Decision.KEEP
            publish()
        }
    }

    /** The system confirmation dialog for trashing everything in the review bin, or null if it's empty. */
    fun createTrashRequest(): PendingIntent? {
        val marked = _state.value.marked
        if (marked.isEmpty()) return null
        pendingTrash = marked
        return runCatching { repository.createTrashRequest(marked) }.getOrNull()
    }

    /** Called when the user answers the system dialog. We re-read MediaStore instead of assuming success. */
    fun onTrashResult(confirmed: Boolean) {
        val requested = pendingTrash
        pendingTrash = emptyList()
        if (!confirmed || requested.isEmpty()) return

        viewModelScope.launch {
            allMedia = repository.loadAll()
            val stillThere = allMedia.mapTo(HashSet()) { it.id }
            val trashed = requested.filter { it.id !in stillThere }
            trashedBytes += trashed.sumOf { it.sizeBytes }
            trashed.forEach { decisions.remove(it.id) }
            publish(loading = false)
        }
    }

    private fun publish(loading: Boolean = _state.value.loading) {
        _state.value = DeckUiState(
            loading = loading,
            deck = allMedia.filter { it.id !in decisions },
            marked = allMedia.filter { decisions[it.id] == Decision.DELETE },
            canUndo = decisions.isNotEmpty(),
            trashedBytes = trashedBytes,
        )
    }
}
