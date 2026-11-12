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

    companion object {
        fun defaultFor(source: DeckSource) =
            if (source.sortBySize) DeckFilter(order = DeckOrder.LARGEST) else DeckFilter()
    }
}
