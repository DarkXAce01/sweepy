package io.github.darkxace01.sweepy.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CleanStats(val cleanedCount: Long = 0, val cleanedBytes: Long = 0)

/** Small settings and lifetime stats. */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("sweepy", Context.MODE_PRIVATE)

    private val _stats = MutableStateFlow(
        CleanStats(prefs.getLong(KEY_CLEANED_COUNT, 0), prefs.getLong(KEY_CLEANED_BYTES, 0)),
    )
    val stats: StateFlow<CleanStats> = _stats.asStateFlow()

    private val _swipeHintSeen = MutableStateFlow(prefs.getBoolean(KEY_HINT_SEEN, false))
    val swipeHintSeen: StateFlow<Boolean> = _swipeHintSeen.asStateFlow()

    fun addCleaned(count: Int, bytes: Long) {
        val updated = CleanStats(_stats.value.cleanedCount + count, _stats.value.cleanedBytes + bytes)
        _stats.value = updated
        prefs.edit {
            putLong(KEY_CLEANED_COUNT, updated.cleanedCount)
            putLong(KEY_CLEANED_BYTES, updated.cleanedBytes)
        }
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
    }
}
