package io.github.darkxace01.sweepy.data

/**
 * One frame of a deck's reel: the strip under the pile. [decision] is null while the photo is still
 * waiting; photos swiped in this visit stay on the reel, in place, carrying their mark.
 */
data class ReelCard(val item: MediaItem, val decision: Decision?)

/**
 * The deck as a reel, in the deck's order: everything still waiting, plus what was swiped in this
 * visit ([swipedHere]), so the reel never shifts under your finger.
 */
fun buildReel(
    sorted: List<MediaItem>,
    isWaiting: (MediaItem) -> Boolean,
    decisionOf: (MediaItem) -> Decision?,
    swipedHere: Set<Long>,
): List<ReelCard> = sorted.mapNotNull { item ->
    when {
        isWaiting(item) -> ReelCard(item, null)
        item.id in swipedHere -> ReelCard(item, decisionOf(item))
        else -> null
    }
}

/**
 * Which frame is on show: the one the reel was moved to ([cursor]), or else the first photo still
 * waiting. -1 when there's nothing left to show.
 */
fun reelPosition(reel: List<ReelCard>, cursor: Long?): Int {
    if (cursor != null) {
        val at = reel.indexOfFirst { it.item.id == cursor }
        if (at >= 0) return at
    }
    return reel.indexOfFirst { it.decision == null }
}

/**
 * Where the reel goes after the frame at [from] is swiped: the next photo still waiting after it,
 * or, at the end, the first one skipped earlier. Null when every photo has been seen to.
 */
fun nextWaiting(reel: List<ReelCard>, from: Int): Long? {
    val swiped = reel.getOrNull(from)?.item?.id
    fun waiting(card: ReelCard) = card.decision == null && card.item.id != swiped
    return (reel.drop(from + 1).firstOrNull(::waiting) ?: reel.take(from.coerceAtLeast(0)).firstOrNull(::waiting))?.item?.id
}
