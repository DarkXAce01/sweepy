package io.github.darkxace01.sweepy.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CleanStats(val cleanedCount: Long = 0, val cleanedBytes: Long = 0)

enum class Appearance { SYSTEM, LIGHT, DARK }

/** Timestamps (decidedAt) of the last "Keep all" in the review bin, and of the last batch already dealt with. */
data class UnbinRecord(val unbinnedAt: Long = 0, val settledAt: Long = 0)

/** User choices from the settings screen. */
data class AppSettings(
    val appearance: Appearance = Appearance.SYSTEM,
    /** Swaps the sides: swipe right to delete, left to keep. */
    val leftHanded: Boolean = false,
    val haptics: Boolean = true,
    val autoplayVideos: Boolean = true,
)

/** Small settings and lifetime stats. */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("sweepy", Context.MODE_PRIVATE)

    private val _stats = MutableStateFlow(
        CleanStats(prefs.getLong(KEY_CLEANED_COUNT, 0), prefs.getLong(KEY_CLEANED_BYTES, 0)),
    )
    val stats: StateFlow<CleanStats> = _stats.asStateFlow()

    private val _swipeHintSeen = MutableStateFlow(prefs.getBoolean(KEY_HINT_SEEN, false))
    val swipeHintSeen: StateFlow<Boolean> = _swipeHintSeen.asStateFlow()

    private val _settings = MutableStateFlow(
        AppSettings(
            appearance = prefs.getString(KEY_APPEARANCE, null)
                ?.let { name -> Appearance.entries.firstOrNull { it.name == name } } ?: Appearance.SYSTEM,
            leftHanded = prefs.getBoolean(KEY_LEFT_HANDED, false),
            haptics = prefs.getBoolean(KEY_HAPTICS, true),
            autoplayVideos = prefs.getBoolean(KEY_AUTOPLAY, true),
        ),
    )
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.edit {
            putString(KEY_APPEARANCE, updated.appearance.name)
            putBoolean(KEY_LEFT_HANDED, updated.leftHanded)
            putBoolean(KEY_HAPTICS, updated.haptics)
            putBoolean(KEY_AUTOPLAY, updated.autoplayVideos)
        }
    }

    fun addCleaned(count: Int, bytes: Long) {
        val updated = CleanStats(_stats.value.cleanedCount + count, _stats.value.cleanedBytes + bytes)
        _stats.value = updated
        prefs.edit {
            putLong(KEY_CLEANED_COUNT, updated.cleanedCount)
            putLong(KEY_CLEANED_BYTES, updated.cleanedBytes)
        }
    }

    private val _unbin = MutableStateFlow(UnbinRecord(prefs.getLong(KEY_UNBINNED_AT, 0), prefs.getLong(KEY_UNBIN_SETTLED_AT, 0)))

    /** When "Keep all" last emptied the review bin, so it can be put back. */
    val unbin: StateFlow<UnbinRecord> = _unbin.asStateFlow()

    fun recordUnbin(at: Long) {
        _unbin.value = _unbin.value.copy(unbinnedAt = at)
        prefs.edit { putLong(KEY_UNBINNED_AT, at) }
    }

    /** The batch from [at] was put back or deliberately left kept; stop offering it. */
    fun settleUnbin(at: Long) {
        _unbin.value = _unbin.value.copy(settledAt = at)
        prefs.edit { putLong(KEY_UNBIN_SETTLED_AT, at) }
    }

    fun markSwipeHintSeen() {
        if (_swipeHintSeen.value) return
        _swipeHintSeen.value = true
        prefs.edit { putBoolean(KEY_HINT_SEEN, true) }
    }

    private companion object {
        const val KEY_CLEANED_COUNT = "cleaned_count"
        const val KEY_CLEANED_BYTES = "cleaned_bytes"
        const val KEY_HINT_SEEN = "swipe_hint_seen"
        const val KEY_APPEARANCE = "appearance"
        const val KEY_LEFT_HANDED = "left_handed"
        const val KEY_HAPTICS = "haptics"
        const val KEY_AUTOPLAY = "autoplay_videos"
        const val KEY_UNBINNED_AT = "unbinned_at"
        const val KEY_UNBIN_SETTLED_AT = "unbin_settled_at"
    }
}
