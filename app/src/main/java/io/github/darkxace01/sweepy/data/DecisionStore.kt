package io.github.darkxace01.sweepy.data

import io.github.darkxace01.sweepy.data.db.DecisionDao
import io.github.darkxace01.sweepy.data.db.DecisionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * All decisions, kept in memory for instant UI updates and written to the database in the background.
 * Writes go through a queue so they reach the database in the same order they were made.
 */
class DecisionStore(private val dao: DecisionDao, scope: CoroutineScope) {

    private val _state = MutableStateFlow<Map<Long, DecisionEntity>?>(null)

    /** null until loaded from the database. */
    val state: StateFlow<Map<Long, DecisionEntity>?> = _state.asStateFlow()

    private sealed interface Write {
        data class Put(val entities: List<DecisionEntity>) : Write
        data class Remove(val ids: List<Long>) : Write
    }

    private val writes = Channel<Write>(Channel.UNLIMITED)
    private val loadLock = Mutex()

    init {
        scope.launch {
            for (write in writes) {
                when (write) {
                    is Write.Put -> write.entities.chunked(CHUNK).forEach { dao.upsert(it) }
                    is Write.Remove -> write.ids.chunked(CHUNK).forEach { dao.delete(it) }
                }
            }
        }
    }

    suspend fun ensureLoaded() = loadLock.withLock {
        if (_state.value == null) _state.value = dao.all().associateBy { it.mediaId }
    }

    fun put(items: Collection<MediaItem>, decision: Decision) {
        if (items.isEmpty()) return
        val now = System.currentTimeMillis()
        putEntities(items.map { DecisionEntity(it.id, decision, it.fingerprint, now) })
    }

    /** Puts back exact earlier entries (used by undo). */
    fun putEntities(entities: List<DecisionEntity>) {
        if (entities.isEmpty()) return
        _state.update { current -> current.orEmpty() + entities.associateBy { it.mediaId } }
        writes.trySend(Write.Put(entities))
    }

    fun remove(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        _state.update { current -> current.orEmpty() - ids.toSet() }
        writes.trySend(Write.Remove(ids.toList()))
    }

    private companion object {
        // SQLite limits how many values one query can take.
        const val CHUNK = 500
    }
}

/** The decision for [item], ignoring stale entries whose file has changed since. */
fun Map<Long, DecisionEntity>.decisionFor(item: MediaItem): Decision? =
    get(item.id)?.takeIf { it.fingerprint == item.fingerprint }?.decision
