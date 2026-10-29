package io.github.darkxace01.sweepy.ui

import android.app.Application
import android.app.PendingIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.darkxace01.sweepy.SweepyApplication
import io.github.darkxace01.sweepy.data.AppSettings
import io.github.darkxace01.sweepy.data.CleanStats
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.TrashedItem
import io.github.darkxace01.sweepy.data.db.DecisionEntity
import io.github.darkxace01.sweepy.data.decisionFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The app's single source of truth: the photo library (from MediaStore) combined with the
 * decision ledger (from the database). Every screen's state is derived from those two.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as SweepyApplication).container
    private val repository = container.media
    private val store = container.decisions
    private val prefs = container.prefs

    /** null while loading. */
    private val media = MutableStateFlow<List<MediaItem>?>(null)

    val swipeHintSeen: StateFlow<Boolean> = prefs.swipeHintSeen

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    val home: StateFlow<HomeUiState> =
        combine(media, store.state, prefs.stats) { media, decisions, stats ->
            if (media == null || decisions == null) HomeUiState(stats = stats) else buildHome(media, decisions, stats)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    val review: StateFlow<ReviewUiState> =
        combine(media, store.state) { media, decisions ->
            if (media == null || decisions == null) return@combine ReviewUiState()
            val marked = media.filter { decisions.decisionFor(it) == Decision.DELETE }
            ReviewUiState(
                groups = marked.groupBy { it.folder }
                    .map { (folder, items) -> ReviewGroup(folder, items) }
                    .sortedByDescending { it.items.size },
                count = marked.size,
                bytes = marked.sumOf { it.sizeBytes },
            )
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, ReviewUiState())

    private val _trash = MutableStateFlow(TrashUiState())
    val trash: StateFlow<TrashUiState> = _trash.asStateFlow()

    // ---- Loading -------------------------------------------------------------------------------

    private var started = false

    /** Call once media permission is granted. Safe to call again (e.g. after access changes). */
    @OptIn(FlowPreview::class)
    fun start() {
        viewModelScope.launch {
            store.ensureLoaded()
            refresh()
        }
        if (!started) {
            started = true
            // Another app (camera, WhatsApp, gallery…) changed something: reload, at most about once a second.
            viewModelScope.launch { repository.changes().debounce(800).collect { refresh() } }
        }
    }

    private suspend fun refresh() {
        media.value = repository.loadAll()
    }

    // ---- Decks ---------------------------------------------------------------------------------

    private data class HistoryEntry(
        val source: DeckSource,
        val itemId: Long,
        val previous: DecisionEntity?,
        val decision: Decision,
    )

    /** Undo history, newest last. Each deck undoes only its own swipes. */
    private val history = MutableStateFlow<List<HistoryEntry>>(emptyList())

    private val decks = HashMap<DeckSource, StateFlow<DeckUiState>>()

    fun deck(source: DeckSource): StateFlow<DeckUiState> = decks.getOrPut(source) {
        combine(media, store.state, history) { media, decisions, history ->
            if (media == null || decisions == null) return@combine DeckUiState()
            val inSource = media.filter(source::matches)
            var deck = if (source == DeckSource.Later) {
                inSource.filter { decisions.decisionFor(it) == Decision.LATER }
            } else {
                inSource.filter { decisions.decisionFor(it) == null }
            }
            if (source.sortBySize) deck = deck.sortedByDescending { it.sizeBytes }
            val marked = media.filter { decisions.decisionFor(it) == Decision.DELETE }
            DeckUiState(
                loading = false,
                deck = deck,
                totalCount = if (source == DeckSource.Later) deck.size else inSource.size,
                markedCount = marked.size,
                markedBytes = marked.sumOf { it.sizeBytes },
                canUndo = history.any { it.source == source },
            )
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeckUiState())
    }

    fun decide(source: DeckSource, item: MediaItem, decision: Decision) {
        val previous = store.state.value?.get(item.id)
        history.update { (it + HistoryEntry(source, item.id, previous, decision)).takeLast(MAX_HISTORY) }
        store.put(listOf(item), decision)
        prefs.markSwipeHintSeen()
    }

    /** Reverts the last swipe in [source]. Returns what that swipe was, so the card can fly back in. */
    fun undo(source: DeckSource): Decision? {
        val entry = history.value.lastOrNull { it.source == source } ?: return null
        history.update { list -> list.filterNot { it === entry } }
        if (entry.previous != null) store.putEntities(listOf(entry.previous)) else store.remove(listOf(entry.itemId))
        return entry.decision
    }

    fun dismissSwipeHint() = prefs.markSwipeHintSeen()

    // ---- Settings ------------------------------------------------------------------------------

    val settings: StateFlow<AppSettings> = prefs.settings

    fun updateSettings(transform: (AppSettings) -> AppSettings) = prefs.updateSettings(transform)

    /** How many items are remembered as kept (so they never show up again). */
    val keptCount: StateFlow<Int> = store.state
        .map { decisions -> decisions?.values?.count { it.decision == Decision.KEEP } ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Lets kept photos show up in decks again. Marked, later and favorites are untouched. */
    fun forgetKept() {
        val ids = store.state.value?.values?.filter { it.decision == Decision.KEEP }?.map { it.mediaId }.orEmpty()
        store.remove(ids)
        history.value = emptyList()
    }

    /** Forgets every decision, including the review bin. Nothing on the phone is deleted. */
    fun forgetEverything() {
        store.remove(store.state.value?.keys.orEmpty())
        history.value = emptyList()
    }

    // ---- Review bin ----------------------------------------------------------------------------

    fun unmark(item: MediaItem) = store.put(listOf(item), Decision.KEEP)

    fun unmarkAll(items: List<MediaItem>) = store.put(items, Decision.KEEP)

    private var pendingTrash: List<MediaItem> = emptyList()

    /** The system "Move to trash?" dialog for the review bin, or null if it's empty. */
    fun createTrashRequest(): PendingIntent? {
        val marked = review.value.groups.flatMap { it.items }.take(MAX_BATCH)
        if (marked.isEmpty()) return null
        pendingTrash = marked
        return runCatching { repository.createTrashRequest(marked) }.getOrNull()
    }

    /** We re-read MediaStore instead of trusting the dialog's answer, and only count what actually went. */
    fun onTrashResult(confirmed: Boolean) {
        val requested = pendingTrash
        pendingTrash = emptyList()
        if (!confirmed || requested.isEmpty()) return
        viewModelScope.launch {
            refresh()
            val present = media.value.orEmpty().mapTo(HashSet()) { it.id }
            val trashed = requested.filter { it.id !in present }
            if (trashed.isEmpty()) return@launch
            val ids = trashed.mapTo(HashSet()) { it.id }
            store.remove(ids)
            history.update { list -> list.filterNot { it.itemId in ids } }
            val bytes = trashed.sumOf { it.sizeBytes }
            prefs.addCleaned(trashed.size, bytes)
            _events.send(UiEvent.Trashed(trashed.size, bytes))
        }
    }

    // ---- Recently cleaned (system trash) -------------------------------------------------------

    fun loadTrash() {
        viewModelScope.launch {
            _trash.value = TrashUiState(loading = false, items = repository.loadTrashed())
        }
    }

    private enum class TrashAction { RESTORE, DELETE_FOREVER }

    private var pendingTrashAction: Pair<TrashAction, List<TrashedItem>>? = null

    fun createRestoreRequest(items: List<TrashedItem>): PendingIntent? =
        trashActionRequest(TrashAction.RESTORE, items) { repository.createRestoreRequest(it) }

    fun createDeleteForeverRequest(items: List<TrashedItem>): PendingIntent? =
        trashActionRequest(TrashAction.DELETE_FOREVER, items) { repository.createDeleteRequest(it) }

    private inline fun trashActionRequest(
        action: TrashAction,
        items: List<TrashedItem>,
        build: (List<MediaItem>) -> PendingIntent,
    ): PendingIntent? {
        val batch = items.take(MAX_BATCH)
        if (batch.isEmpty()) return null
        pendingTrashAction = action to batch
        return runCatching { build(batch.map { it.media }) }.getOrNull()
    }

    fun onTrashActionResult(confirmed: Boolean) {
        val (action, requested) = pendingTrashAction ?: return
        pendingTrashAction = null
        if (!confirmed) return
        viewModelScope.launch {
            val stillTrashed = repository.loadTrashed()
            _trash.value = TrashUiState(loading = false, items = stillTrashed)
            val remainingIds = stillTrashed.mapTo(HashSet()) { it.media.id }
            val done = requested.filter { it.media.id !in remainingIds }
            if (done.isEmpty()) return@launch
            when (action) {
                TrashAction.RESTORE -> {
                    refresh()
                    // The user deliberately brought these back, so don't ask about them again.
                    val restored = media.value.orEmpty().filter { item -> done.any { it.media.id == item.id } }
                    store.put(restored, Decision.KEEP)
                    _events.send(UiEvent.Restored(done.size))
                }
                TrashAction.DELETE_FOREVER ->
                    _events.send(UiEvent.DeletedForever(done.size, done.sumOf { it.media.sizeBytes }))
            }
        }
    }

    private companion object {
        const val MAX_HISTORY = 500

        /** Keep each system request a reasonable size; the user can simply tap again for the rest. */
        const val MAX_BATCH = 1000
    }
}

/** Groups the whole library into deck summaries in a single pass. */
private fun buildHome(media: List<MediaItem>, decisions: Map<Long, DecisionEntity>, stats: CleanStats): HomeUiState {
    class Acc(val source: DeckSource) {
        var total = 0
        var bytes = 0L
        var remaining = 0
        // Covers prefer photos still waiting, so a finished deck doesn't look unfinished.
        val waitingCovers = ArrayList<MediaItem>(COVER_COUNT)
        val decidedCovers = ArrayList<MediaItem>(COVER_COUNT)

        fun add(item: MediaItem, undecided: Boolean) {
            total++
            bytes += item.sizeBytes
            if (undecided) remaining++
            val covers = if (undecided) waitingCovers else decidedCovers
            if (covers.size < COVER_COUNT) covers += item
        }

        fun summary() = DeckSummary(source, total, bytes, remaining, (waitingCovers + decidedCovers).take(COVER_COUNT))
    }

    val screenshots = Acc(DeckSource.Screenshots)
    val whatsApp = Acc(DeckSource.WhatsApp)
    val largeVideos = Acc(DeckSource.LargeVideos)
    val later = Acc(DeckSource.Later)
    val folders = LinkedHashMap<Long, Acc>()
    val months = HashMap<Int, Acc>()

    var totalBytes = 0L
    var reviewed = 0
    var markedCount = 0
    var markedBytes = 0L
    val recent = ArrayList<MediaItem>(RECENT_COUNT)

    for (item in media) {
        val decision = decisions.decisionFor(item)
        val undecided = decision == null
        if (undecided && recent.size < RECENT_COUNT) recent += item
        totalBytes += item.sizeBytes
        if (!undecided) reviewed++
        if (decision == Decision.DELETE) {
            markedCount++
            markedBytes += item.sizeBytes
        }
        if (decision == Decision.LATER) later.add(item, undecided = true)

        for (quick in arrayOf(screenshots, whatsApp, largeVideos)) {
            if (quick.source.matches(item)) quick.add(item, undecided)
        }
        folders.getOrPut(item.bucketId) { Acc(DeckSource.Folder(item.bucketId, item.folder.orEmpty())) }
            .add(item, undecided)
        months.getOrPut(item.yearMonth) { Acc(DeckSource.Month(item.yearMonth)) }.add(item, undecided)
    }

    return HomeUiState(
        loading = false,
        totalCount = media.size,
        totalBytes = totalBytes,
        reviewedCount = reviewed,
        markedCount = markedCount,
        markedBytes = markedBytes,
        laterCount = later.total,
        stats = stats,
        recent = recent,
        quickDecks = listOf(screenshots, whatsApp, largeVideos, later).filter { it.total > 0 }.map { it.summary() },
        folders = folders.values.sortedByDescending { it.total }.map { it.summary() },
        months = months.values.sortedByDescending { (it.source as DeckSource.Month).yearMonth }.map { it.summary() },
    )
}

private const val COVER_COUNT = 4
private const val RECENT_COUNT = 10
