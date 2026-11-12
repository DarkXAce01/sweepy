package io.github.darkxace01.sweepy.data

/** Which kinds of files a deck shows. */
enum class MediaKind { ALL, PHOTOS, VIDEOS }

/** The order cards come off the pile. */
enum class DeckOrder { NEWEST, OLDEST, LARGEST }

/** How the user narrowed and sorted one deck. Never changes decisions, only what's shown and when. */
data class DeckFilter(val kind: MediaKind = MediaKind.ALL, val order: DeckOrder = DeckOrder.NEWEST) {

    fun matches(item: MediaItem): Boolean = when (kind) {
        MediaKind.ALL -> true
        MediaKind.PHOTOS -> !item.isVideo
        MediaKind.VIDEOS -> item.isVideo
    }

    /** [items] arrive newest first, the way the library is loaded. */
    fun sort(items: List<MediaItem>): List<MediaItem> = when (order) {
        DeckOrder.NEWEST -> items
        DeckOrder.OLDEST -> items.reversed()
        DeckOrder.LARGEST -> items.sortedByDescending { it.sizeBytes }
    }

    /**
     * [items] sorted, starting from [anchor] (a card picked from the strip, or one brought back by
     * undo). Cards that come before it move to the end, so nothing is skipped for good. The anchor
     * may already be decided and gone; the pile then starts where it used to be.
     */
    fun arrange(items: List<MediaItem>, anchor: MediaItem?): List<MediaItem> {
        val sorted = sort(items)
        if (anchor == null) return sorted
        var start = sorted.indexOfFirst { it.id == anchor.id }
        if (start < 0) start = sorted.indexOfFirst { !comesBefore(it, anchor) }.takeIf { it >= 0 } ?: 0
        return if (start == 0) sorted else sorted.subList(start, sorted.size) + sorted.subList(0, start)
    }

    /** Whether [item] sits strictly before [other] in this order. */
    private fun comesBefore(item: MediaItem, other: MediaItem): Boolean = when (order) {
        DeckOrder.NEWEST -> item.dateAddedSeconds > other.dateAddedSeconds
        DeckOrder.OLDEST -> item.dateAddedSeconds < other.dateAddedSeconds
        DeckOrder.LARGEST -> item.sizeBytes > other.sizeBytes
    }

    companion object {
        fun defaultFor(source: DeckSource) =
            if (source.sortBySize) DeckFilter(order = DeckOrder.LARGEST) else DeckFilter()
    }
}
