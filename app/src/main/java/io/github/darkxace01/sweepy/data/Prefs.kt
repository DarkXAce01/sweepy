package io.github.darkxace01.sweepy.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CleanStats(val cleanedCount: Long = 0, val cleanedBytes: Long = 0)

enum class Appearance { SYSTEM, LIGHT, DARK }

/** The app's whole look: colors, type, paper and marks. */
enum class ThemeChoice { CONTACT_SHEET, RISO, HERBARIUM }

/** User choices from the settings screen. */
data class AppSettings(
    val theme: ThemeChoice = ThemeChoice.CONTACT_SHEET,
    /** Set once the first-launch theme picker has been passed. */
    val themePicked: Boolean = false,
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
            theme = prefs.getString(KEY_THEME, null)
                ?.let { name -> ThemeChoice.entries.firstOrNull { it.name == name } } ?: ThemeChoice.CONTACT_SHEET,
            // People who used Sweepy before themes existed have already swiped; don't stop them.
            themePicked = prefs.getBoolean(KEY_THEME_PICKED, prefs.getBoolean(KEY_HINT_SEEN, false)),
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
            putString(KEY_THEME, updated.theme.name)
            putBoolean(KEY_THEME_PICKED, updated.themePicked)
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

    fun markSwipeHintSeen() {
        if (_swipeHintSeen.value) return
        _swipeHintSeen.value = true
        prefs.edit { putBoolean(KEY_HINT_SEEN, true) }
    }

    private companion object {
        const val KEY_CLEANED_COUNT = "cleaned_count"
        const val KEY_CLEANED_BYTES = "cleaned_bytes"
        const val KEY_HINT_SEEN = "swipe_hint_seen"
        const val KEY_THEME = "theme"
        const val KEY_THEME_PICKED = "theme_picked"
        const val KEY_APPEARANCE = "appearance"
        const val KEY_LEFT_HANDED = "left_handed"
        const val KEY_HAPTICS = "haptics"
        const val KEY_AUTOPLAY = "autoplay_videos"
    }
}
