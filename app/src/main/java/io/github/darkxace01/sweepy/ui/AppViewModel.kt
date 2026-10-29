package io.github.darkxace01.sweepy.ui

import android.app.Application
import android.app.PendingIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.darkxace01.sweepy.SweepyApplication
import io.github.darkxace01.sweepy.data.AppSettings
import io.github.darkxace01.sweepy.data.CleanStats
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.DeckFilter
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.TrashedItem
import io.github.darkxace01.sweepy.data.UnbinRecord
import io.github.darkxace01.sweepy.data.db.DecisionEntity
import io.github.darkxace01.sweepy.data.decisionFor
import io.github.darkxace01.sweepy.data.db.DuplicateReviewEntity
import io.github.darkxace01.sweepy.data.duplicates.DuplicateGroup
import io.github.darkxace01.sweepy.data.duplicates.DuplicateGrouper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val scanner = container.duplicates
    private val duplicateDao = container.duplicateDao

    /** null while loading. */
    private val media = MutableStateFlow<List<MediaItem>?>(null)

    val swipeHintSeen: StateFlow<Boolean> = prefs.swipeHintSeen

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    // ---- Duplicates (declared first: home depends on it) -----------------------------------------

    /** Raw scan result; null until the first scan finishes. */
    private val duplicateGroups = MutableStateFlow<List<DuplicateGroup>?>(null)

    /** mediaId → fingerprint of photos already handled in the duplicates screen. */
    private val reviewed = MutableStateFlow<Map<Long, DuplicateReviewEntity>>(emptyMap())

    private data class DuplicateUndo(val items: List<MediaItem>, val previous: List<DecisionEntity>)

    private val duplicateHistory = MutableStateFlow<List<DuplicateUndo>>(emptyList())

    val duplicates: StateFlow<DuplicatesUiState> =
        combine(duplicateGroups, store.state, reviewed, scanner.progress, duplicateHistory) { groups, decisions, reviewed, progress, history ->
            DuplicatesUiState(
                scanning = groups == null || progress != null,
                progress = progress,
                groups = if (groups == null || decisions == null) emptyList() else visibleGroups(groups, decisions, reviewed),
                canUndo = history.isNotEmpty(),
            )
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, DuplicatesUiState())

    val home: StateFlow<HomeUiState> =
        combine(media, store.state, prefs.stats, duplicates) { media, decisions, stats, duplicates ->
            if (media == null || decisions == null) {
                HomeUiState(stats = stats)
            } else {
                buildHome(media, decisions, stats).copy(duplicates = duplicates.summary())
            }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    val review: StateFlow<ReviewUiState> =
        combine(media, store.state, prefs.unbin) { media, decisions, unbin ->
            if (media == null || decisions == null) return@combine ReviewUiState()
            val marked = media.filter { decisions.decisionFor(it) == Decision.DELETE }
            val unbinnedAt = unbinBatch(decisions.values, unbin)
            ReviewUiState(
                unbinned = if (unbinnedAt == null) {
                    emptyList()
                } else {
                    media.filter { item -> decisions[item.id]?.let { it.decidedAt == unbinnedAt && it.decision == Decision.KEEP } == true }
                },
                unbinnedAt = unbinnedAt ?: 0,
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
        val loaded = repository.loadAll()
        media.value = loaded
        scheduleDuplicateScan(loaded)
    }

    private var scanJob: Job? = null

    /** Rescans for duplicates a moment after the library settles (it's cached, so usually quick). */
    private fun scheduleDuplicateScan(library: List<MediaItem>) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            if (duplicateGroups.value != null) delay(3_000)
            if (reviewed.value.isEmpty()) {
                reviewed.value = duplicateDao.reviews().associateBy { it.mediaId }
            }
            duplicateGroups.value = scanner.scan(library)
        }
    }

    /**
     * Keeps the photos in [keep] and sends the rest of the group to the review bin. Favorites are
     * always kept. Keepers that already had a decision (later, kept) keep it.
     */
    fun resolveDuplicates(group: DuplicateGroupUi, keep: Set<Long>) {
        val decisions = store.state.value.orEmpty()
        val items = group.items
        val previous = items.mapNotNull { decisions[it.id] }
        val bins = items.filter { it.id !in keep && decisions.decisionFor(it) != Decision.FAVORITE }
        val newKeepers = items.filter { it.id in keep && decisions.decisionFor(it) == null }
        store.put(newKeepers, Decision.KEEP)
        store.put(bins, Decision.DELETE)
        markReviewed(items, true)
        duplicateHistory.update { (it + DuplicateUndo(items, previous)).takeLast(MAX_HISTORY) }
    }

    fun undoDuplicates() {
        val entry = duplicateHistory.value.lastOrNull() ?: return
        duplicateHistory.update { it.dropLast(1) }
        val previousIds = entry.previous.mapTo(HashSet()) { it.mediaId }
        store.remove(entry.items.map { it.id }.filter { it !in previousIds })
        store.putEntities(entry.previous)
        markReviewed(entry.items, false)
    }

    private fun markReviewed(items: List<MediaItem>, done: Boolean) {
        val ids = items.map { it.id }
        val now = System.currentTimeMillis()
        val entities = items.map { DuplicateReviewEntity(it.id, it.fingerprint, now) }
        reviewed.update { current ->
            if (done) current + entities.associateBy { it.mediaId } else current - ids.toSet()
        }
        viewModelScope.launch {
            if (done) {
                duplicateDao.upsertReviews(entities)
            } else {
                duplicateDao.deleteReviews(ids)
            }
        }
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

    /** Per-deck "show" and "order" choices. Kept while the app is open; each visit starts where you left it. */
    private val filters = MutableStateFlow<Map<DeckSource, DeckFilter>>(emptyMap())

    fun deck(source: DeckSource): StateFlow<DeckUiState> = decks.getOrPut(source) {
        combine(media, store.state, history, filters) { media, decisions, history, filters ->
            if (media == null || decisions == null) return@combine DeckUiState()
            val filter = filters[source] ?: DeckFilter.defaultFor(source)
            val inSource = media.filter(source::matches)
            val waiting = if (source == DeckSource.Later) {
                inSource.filter { decisions.decisionFor(it) == Decision.LATER }
            } else {
                inSource.filter { decisions.decisionFor(it) == null }
            }
            val deck = filter.sort(waiting.filter(filter::matches))
            val marked = media.filter { decisions.decisionFor(it) == Decision.DELETE }
            DeckUiState(
                loading = false,
                deck = deck,
                totalCount = if (source == DeckSource.Later) deck.size else inSource.count(filter::matches),
                markedCount = marked.size,
                markedBytes = marked.sumOf { it.sizeBytes },
                canUndo = history.any { it.source == source },
                filter = filter,
                defaultFilter = DeckFilter.defaultFor(source),
                waitingPhotos = waiting.count { !it.isVideo },
                waitingVideos = waiting.count { it.isVideo },
            )
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeckUiState())
    }

    fun setFilter(source: DeckSource, filter: DeckFilter) = filters.update { it + (source to filter) }

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
        duplicateHistory.value = emptyList()
        reviewed.value = emptyMap()
        viewModelScope.launch { duplicateDao.clearReviews() }
    }

    // ---- Review bin ----------------------------------------------------------------------------

    fun unmark(item: MediaItem) {
        store.put(listOf(item), Decision.KEEP)
    }

    fun unmarkAll(items: List<MediaItem>) {
        if (items.isEmpty()) return
        val at = store.put(items, Decision.KEEP)
        prefs.recordUnbin(at)
        _events.trySend(UiEvent.KeptAll(items.size, at))
    }

    /** Puts the "Keep all" batch made at [at] back in the review bin. Photos changed since then are left alone. */
    fun undoKeepAll(at: Long) {
        val decisions = store.state.value.orEmpty()
        val batch = media.value.orEmpty().filter { item ->
            decisions[item.id]?.let { it.decidedAt == at && it.decision == Decision.KEEP } == true
        }
        store.put(batch, Decision.DELETE)
        prefs.settleUnbin(at)
    }

    /** The undo window for a "Keep all" closed: the decision stands. */
    fun settleKeepAll(at: Long) = prefs.settleUnbin(at)

    fun restoreUnbinned() = undoKeepAll(review.value.unbinnedAt)

    fun dismissUnbinned() = prefs.settleUnbin(review.value.unbinnedAt)

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

/**
 * The scan result as the user should see it now: photos already in the review bin drop out,
 * groups the user has fully handled disappear, and the suggestion is recalculated for what's left.
 */
/**
 * Groups still worth showing. Once you've been through a group it stays hidden, unless a photo
 * added to the phone *after* that joins it: a genuinely new copy. Older photos joining (back from
 * the review bin, or regrouped by a rule change) don't count: you've already seen that moment.
 */
internal fun visibleGroups(
    groups: List<DuplicateGroup>,
    decisions: Map<Long, DecisionEntity>,
    reviewed: Map<Long, DuplicateReviewEntity>,
): List<DuplicateGroupUi> = groups.mapNotNull { group ->
    val members = group.members.filter { decisions.decisionFor(it.item) != Decision.DELETE }
    if (members.size < 2) return@mapNotNull null
    val reviews = members.mapNotNull { m -> reviewed[m.item.id]?.takeIf { it.fingerprint == m.item.fingerprint } }
    if (reviews.isNotEmpty()) {
        val lastReview = reviews.maxOf { it.reviewedAt }
        val newCopy = members.any { m -> reviewed[m.item.id] == null && m.item.dateAddedSeconds * 1000 > lastReview }
        if (!newCopy) return@mapNotNull null
    }
    val visible = group.copy(members = members)
    val favorites = members.map { it.item }.filter { decisions.decisionFor(it) == Decision.FAVORITE }.mapTo(HashSet()) { it.id }
    val (suggested, reason) = DuplicateGrouper.suggest(visible) { it.id in favorites }
    DuplicateGroupUi(visible, suggested, reason, favorites)
}

/**
 * The decidedAt shared by the last "Keep all" batch from the review bin, if it hasn't been dealt with.
 * Versions before 0.2 didn't record it, so fall back to the newest big batch of keeps made in one go:
 * swipes are one photo at a time, so only a "Keep all" leaves dozens with the very same timestamp.
 */
internal fun unbinBatch(decisions: Collection<DecisionEntity>, record: UnbinRecord): Long? {
    val at = record.unbinnedAt.takeIf { it > 0 } ?: decisions.asSequence()
        .filter { it.decision == Decision.KEEP }
        .groupingBy { it.decidedAt }.eachCount()
        .filter { it.value >= LEGACY_UNBIN_MIN }
        .keys.maxOrNull()
    return at?.takeIf { it > record.settledAt }
}

private const val LEGACY_UNBIN_MIN = 20
