package io.github.darkxace01.sweepy.data.duplicates

import io.github.darkxace01.sweepy.data.MediaItem
import kotlin.math.abs

/** What the scan knows about one file. */
data class MediaSignature(
    val item: MediaItem,
    /** Hash of the file's bytes; equal hashes mean identical files. Only computed for same-size files. */
    val contentHash: String? = null,
    /** 64-bit "difference hash" of a tiny grayscale thumbnail; close hashes mean similar-looking photos. */
    val visualHash: Long? = null,
    /** Higher is sharper. Only computed for look-alikes. */
    val sharpness: Float? = null,
    /** Screenshots only ever match as exact copies: different chat screenshots look too alike. */
    val isScreenshot: Boolean = false,
)

enum class DuplicateKind {
    /** Every file in the group is byte-for-byte identical. Certain. */
    EXACT,

    /** Photos that look nearly the same (bursts, re-saves, edits). A suggestion, not a certainty. */
    SIMILAR,
}

enum class KeepReason { ORIGINAL, SHARPEST, MOST_DETAIL, FAVORITE }

data class DuplicateGroup(
    val kind: DuplicateKind,
    /** Oldest first. */
    val members: List<MediaSignature>,
) {
    val items: List<MediaItem> get() = members.map { it.item }
    val totalBytes: Long get() = items.sumOf { it.sizeBytes }

    /** Time between the first and last shot. */
    val spanMillis: Long get() = items.maxOf { it.dateTakenMillis } - items.minOf { it.dateTakenMillis }
}

/**
 * Decides which files belong together. Kept free of Android code so the rules can be unit tested.
 *
 * - **Exact copies:** identical content hash (and therefore identical size). Always grouped.
 * - **Look-alikes:** photos only (no videos, no screenshots), same shape, and either
 *   - taken within [BURST_WINDOW_MILLIS] of each other with a hash distance of at most [BURST_DISTANCE], or
 *   - taken any time apart with a distance of at most [FAR_DISTANCE] (practically the same picture).
 * - Near-blank images (almost all hash bits the same) are never look-alikes: every dark pocket shot
 *   would match every other one.
 */
object DuplicateGrouper {
    const val BURST_WINDOW_MILLIS = 60_000L
    const val BURST_DISTANCE = 10
    const val FAR_DISTANCE = 2

    /** Hashes with fewer than this many 1-bits (or 0-bits) carry too little picture to compare. */
    private const val MIN_BITS = 8

    /** Look-alike chains stop growing here, so a slow pan doesn't merge into one endless group. */
    private const val MAX_SIMILAR_GROUP = 24

    /** Width/height ratios must be within 3% of each other. */
    private const val ASPECT_TOLERANCE = 0.03

    fun group(signatures: List<MediaSignature>): List<DuplicateGroup> {
        val n = signatures.size
        val parent = IntArray(n) { it }
        val size = IntArray(n) { 1 }
        fun find(i: Int): Int {
            var x = i
            while (parent[x] != x) {
                parent[x] = parent[parent[x]]
                x = parent[x]
            }
            return x
        }
        fun union(a: Int, b: Int, limit: Int = Int.MAX_VALUE) {
            val ra = find(a)
            val rb = find(b)
            if (ra == rb || size[ra] + size[rb] > limit) return
            val (big, small) = if (size[ra] >= size[rb]) ra to rb else rb to ra
            parent[small] = big
            size[big] += size[small]
        }

        // Exact copies: same content hash.
        signatures.indices
            .filter { signatures[it].contentHash != null }
            .groupBy { signatures[it].contentHash }
            .values
            .forEach { same -> same.drop(1).forEach { union(same[0], it) } }

        // Look-alikes.
        val comparable = signatures.indices.filter { i ->
            val s = signatures[i]
            val hash = s.visualHash
            hash != null && !s.item.isVideo && !s.isScreenshot && s.item.width > 0 && s.item.height > 0 &&
                java.lang.Long.bitCount(hash) in MIN_BITS..(64 - MIN_BITS)
        }
        for (x in comparable.indices) {
            val a = signatures[comparable[x]]
            for (y in x + 1 until comparable.size) {
                val b = signatures[comparable[y]]
                if (!sameShape(a.item, b.item)) continue
                val distance = java.lang.Long.bitCount(a.visualHash!! xor b.visualHash!!)
                val close = abs(a.item.dateTakenMillis - b.item.dateTakenMillis) <= BURST_WINDOW_MILLIS
                if (distance <= FAR_DISTANCE || (close && distance <= BURST_DISTANCE)) {
                    union(comparable[x], comparable[y], MAX_SIMILAR_GROUP)
                }
            }
        }

        return signatures.indices
            .groupBy(::find)
            .values
            .filter { it.size >= 2 }
            .map { indices ->
                val members = indices.map { signatures[it] }.sortedWith(compareBy({ it.item.dateTakenMillis }, { it.item.id }))
                val hash = members.first().contentHash
                val exact = hash != null && members.all { it.contentHash == hash }
                DuplicateGroup(if (exact) DuplicateKind.EXACT else DuplicateKind.SIMILAR, members)
            }
            // Biggest savings first.
            .sortedByDescending { it.totalBytes - it.items.maxOf { item -> item.sizeBytes } }
    }

    private fun sameShape(a: MediaItem, b: MediaItem): Boolean {
        val ra = a.width.toDouble() / a.height
        val rb = b.width.toDouble() / b.height
        return abs(ra - rb) / maxOf(ra, rb) <= ASPECT_TOLERANCE
    }

    /**
     * The one we'd suggest keeping, and why. Favorites always win. For exact copies it's the
     * original (the first one saved); for look-alikes the clearly sharpest, otherwise the one with
     * the most pixels.
     */
    fun suggest(group: DuplicateGroup, isFavorite: (MediaItem) -> Boolean = { false }): Pair<MediaItem, KeepReason> {
        group.items.firstOrNull(isFavorite)?.let { return it to KeepReason.FAVORITE }
        if (group.kind == DuplicateKind.EXACT) {
            return group.items.minWith(compareBy({ it.dateAddedSeconds }, { it.id })) to KeepReason.ORIGINAL
        }
        val bySharpness = group.members.filter { it.sharpness != null }.sortedByDescending { it.sharpness }
        if (bySharpness.size == group.members.size && bySharpness.size >= 2) {
            val best = bySharpness[0].sharpness!!
            val next = bySharpness[1].sharpness!!
            // Only call it "sharpest" when the difference is clear.
            if (best >= next * 1.15f) return bySharpness[0].item to KeepReason.SHARPEST
        }
        return group.items.maxWith(compareBy({ it.width.toLong() * it.height }, { it.sizeBytes })) to KeepReason.MOST_DETAIL
    }
}
