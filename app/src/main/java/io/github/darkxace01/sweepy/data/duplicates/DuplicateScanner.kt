package io.github.darkxace01.sweepy.data.duplicates

import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.db.DuplicateDao
import io.github.darkxace01.sweepy.data.db.MediaHashEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Scan progress, 0..1, or null when not scanning. */
typealias ScanProgress = Float?

/**
 * Finds duplicate groups across the library. Every hash is cached in the database keyed by the
 * file's fingerprint, so after the first scan only new or changed files are read.
 */
class DuplicateScanner(private val dao: DuplicateDao, private val hasher: MediaHasher) {

    private val _progress = MutableStateFlow<ScanProgress>(null)
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    private val lock = Mutex()

    suspend fun scan(media: List<MediaItem>): List<DuplicateGroup> = lock.withLock {
        withContext(Dispatchers.IO) {
            try {
                scanLocked(media)
            } finally {
                _progress.value = null
            }
        }
    }

    private suspend fun scanLocked(media: List<MediaItem>): List<DuplicateGroup> {
        val cache = HashMap<Long, MediaHashEntity>()
        dao.hashes().forEach { cache[it.mediaId] = it }
        fun cached(item: MediaItem) = cache[item.id]?.takeIf { it.fingerprint == item.fingerprint }

        // 1. A visual hash for every photo. This is the slow part on the first run.
        val needVisual = media.filter { !it.isVideo && cached(it)?.visualHash == null }
        // 2. A content hash only for files that share their exact size with another file.
        val needContent = media.filter { it.sizeBytes > 0 }
            .groupBy { it.sizeBytes }.values.filter { it.size >= 2 }.flatten()
            .filter { cached(it)?.contentHash == null }
        val total = (needVisual.size + needContent.size).coerceAtLeast(1)
        var done = 0
        if (needVisual.isNotEmpty() || needContent.isNotEmpty()) _progress.value = 0f

        suspend fun compute(items: List<MediaItem>, update: (MediaItem, MediaHashEntity?) -> MediaHashEntity) {
            for (batch in items.chunked(BATCH)) {
                coroutineContext.ensureActive()
                val results = coroutineScope {
                    batch.chunked((batch.size + PARALLELISM - 1) / PARALLELISM).map { part ->
                        async { part.map { item -> update(item, cached(item)) } }
                    }.awaitAll().flatten()
                }
                results.forEach { cache[it.mediaId] = it }
                dao.upsertHashes(results)
                done += batch.size
                _progress.value = done.toFloat() / total
            }
        }

        compute(needVisual) { item, old ->
            (old ?: empty(item)).copy(visualHash = hasher.visualHash(item) ?: NO_HASH)
        }
        compute(needContent) { item, old ->
            (old ?: empty(item)).copy(contentHash = hasher.contentHash(item) ?: "")
        }

        fun signature(item: MediaItem): MediaSignature {
            val row = cached(item)
            return MediaSignature(
                item = item,
                contentHash = row?.contentHash?.takeIf { it.isNotEmpty() },
                visualHash = row?.visualHash?.takeIf { it != NO_HASH },
                sharpness = row?.sharpness,
                isScreenshot = DeckSource.Screenshots.matches(item),
            )
        }

        var groups = withContext(Dispatchers.Default) { DuplicateGrouper.group(media.map(::signature)) }

        // 3. Sharpness, only for look-alikes, so we can suggest the clearest shot.
        val needSharpness = groups.filter { it.kind == DuplicateKind.SIMILAR }
            .flatMap { it.items }
            .filter { cached(it)?.sharpness == null }
        if (needSharpness.isNotEmpty()) {
            val results = coroutineScope {
                needSharpness.chunked((needSharpness.size + PARALLELISM - 1) / PARALLELISM).map { part ->
                    async { part.map { (cached(it) ?: empty(it)).copy(sharpness = hasher.sharpness(it) ?: 0f) } }
                }.awaitAll().flatten()
            }
            results.forEach { cache[it.mediaId] = it }
            dao.upsertHashes(results)
            groups = groups.map { group -> group.copy(members = group.members.map { signature(it.item) }) }
        }
        return groups
    }

    private fun empty(item: MediaItem) = MediaHashEntity(item.id, item.fingerprint, null, null, null)

    private companion object {
        const val BATCH = 200
        const val PARALLELISM = 4

        /** Stored when a photo couldn't be read, so we don't retry it on every scan. */
        const val NO_HASH = Long.MIN_VALUE
    }
}
