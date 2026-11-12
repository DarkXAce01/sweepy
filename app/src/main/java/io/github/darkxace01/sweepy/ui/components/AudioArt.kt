package io.github.darkxace01.sweepy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.theme.Sweepy

/**
 * Stand-in artwork for an audio file: a waveform printed in one of the theme's inks. The shape
 * comes from the file's id, so each recording keeps the same look everywhere it appears.
 * [played] (0..1) inks the bars up to that point, for the player.
 */
@Composable
fun AudioArt(item: MediaItem, modifier: Modifier = Modifier, bars: Int = 18, played: Float? = null) {
    val colors = Sweepy.colors
    val tint = listOf(colors.keep, colors.later, colors.favorite, colors.pencil)[(item.id % 4).toInt()]
    val ink = colors.ink
    Box(modifier.background(colors.print)) {
        Canvas(Modifier.fillMaxSize().background(tint.copy(alpha = if (colors.isDark) 0.22f else 0.16f))) {
            val heights = waveform(item.id, bars)
            val slot = size.width * 0.8f / bars
            val barWidth = slot * 0.55f
            val left = size.width * 0.1f
            val mid = size.height / 2
            heights.forEachIndexed { i, h ->
                val barHeight = size.height * 0.5f * h
                val x = left + i * slot + (slot - barWidth) / 2
                val done = played != null && (i + 0.5f) / bars <= played
                val color = if (done) ink else tint
                drawRoundRect(
                    color,
                    Offset(x, mid - barHeight / 2),
                    Size(barWidth, barHeight),
                    CornerRadius(barWidth / 2),
                )
            }
        }
    }
}

/** Bar heights (0.15..1) that look like speech or music: loud and quiet patches, never flat. */
private fun waveform(seed: Long, count: Int): List<Float> {
    var h = seed * 2654435761L + 1013904223L
    return List(count) { i ->
        h = h * 6364136223846793005L + 1442695040888963407L
        val noise = ((h ushr 33) % 1000L).toFloat() / 1000f
        val swell = 0.55f + 0.45f * kotlin.math.sin(i * 0.7f + (seed % 7))
        (0.15f + 0.85f * noise * swell).coerceIn(0.15f, 1f)
    }
}

/** Where an audio file came from, in words: "WhatsApp voice note", "Recording", "Music"… */
@Composable
fun audioSource(item: MediaItem): String {
    val path = item.relativePath.orEmpty().lowercase()
    return when {
        "voice notes" in path -> stringResource(R.string.audio_whatsapp_voice)
        "whatsapp" in path -> stringResource(R.string.audio_whatsapp)
        "call" in path -> stringResource(R.string.audio_call)
        "record" in path || "sounds" in path -> stringResource(R.string.audio_recording)
        "telegram" in path -> stringResource(R.string.audio_telegram)
        path.startsWith("download") -> stringResource(R.string.audio_download)
        path.startsWith("music") || item.artist != null -> stringResource(R.string.audio_music)
        else -> item.folder ?: stringResource(R.string.info_unknown)
    }
}

/** The file name without its extension, for a title. */
fun audioTitle(item: MediaItem): String =
    item.displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: ""
