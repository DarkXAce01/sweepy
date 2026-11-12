package io.github.darkxace01.sweepy.ui.deck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.components.AudioArt
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.audioTitle
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.formatDuration
import io.github.darkxace01.sweepy.ui.theme.Pixnix
import kotlinx.coroutines.delay

/**
 * An audio file as a card: where it came from, its name, a waveform and a play button. Nothing
 * plays until it's tapped. Only the top card ([interactive]) gets a player; the ones underneath
 * are just the picture.
 */
@Composable
fun AudioFace(item: MediaItem, interactive: Boolean, modifier: Modifier = Modifier) {
    val live = interactive && !LocalInspectionMode.current
    val player = if (live) rememberAudioPlayer(item) else null
    var playing by remember(item.id) { mutableStateOf(false) }
    var position by remember(item.id) { mutableLongStateOf(0L) }
    var scrub by remember(item.id) { mutableStateOf<Float?>(null) }
    val duration = item.durationMillis.takeIf { it > 0 } ?: player?.duration?.coerceAtLeast(0L) ?: 0L

    if (player != null) {
        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playing = isPlaying
                }

                override fun onPlaybackStateChanged(state: Int) {
                    // Finished: back to the start, ready to play again.
                    if (state == Player.STATE_ENDED) {
                        player.pause()
                        player.seekTo(0)
                        position = 0
                    }
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
        LaunchedEffect(player) {
            while (true) {
                if (scrub == null) position = player.currentPosition
                delay(POSITION_POLL_MILLIS)
            }
        }
    }
    val progress = scrub ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Box(modifier) {
        AudioArt(item, Modifier.fillMaxSize(), bars = 28, played = progress)
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text(
                audioTitle(item),
                style = MaterialTheme.typography.headlineSmall,
                color = Pixnix.colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.artist?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Pixnix.colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayButton(
                    playing = playing,
                    enabled = player != null,
                    onClick = {
                        val p = player ?: return@PlayButton
                        if (p.isPlaying) p.pause() else p.play()
                    },
                )
                Spacer(Modifier.width(14.dp))
                Eyebrow(
                    if (position > 0 || scrub != null) {
                        val shown = scrub?.let { (it * duration).toLong() } ?: position
                        "${formatDuration(shown)} / ${formatDuration(duration)}"
                    } else {
                        formatDuration(duration)
                    },
                    color = Pixnix.colors.ink,
                )
            }
            if (player != null && duration > 0) {
                Scrubber(
                    progress = progress,
                    active = scrub != null,
                    color = Pixnix.colors.ink,
                    onScrub = { fraction ->
                        scrub = fraction
                        player.seekTo((fraction * duration).toLong())
                    },
                    onScrubEnd = {
                        scrub?.let { position = (it * duration).toLong() }
                        scrub = null
                    },
                )
            }
        }
    }
}

/** One player for the card on top. Paused when the app goes to the background, released when the card goes. */
@Composable
private fun rememberAudioPlayer(item: MediaItem): ExoPlayer {
    val context = LocalContext.current
    val player = remember(item.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(item.uri))
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LifecycleResumeEffect(player) {
        onPauseOrDispose { player.pause() }
    }
    return player
}

@Composable
private fun PlayButton(playing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (playing) R.string.audio_pause else R.string.audio_play)
    val ink = Pixnix.colors.ink
    val paper = Pixnix.colors.print
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .pressable(onClick, enabled = enabled, pressedScale = 0.9f)
            .semantics { contentDescription = label },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(ink)
            val s = size.minDimension
            if (playing) {
                val w = s * 0.1f
                val h = s * 0.34f
                for (x in listOf(s * 0.38f, s * 0.52f)) {
                    drawRoundRect(paper, Offset(x, (s - h) / 2), Size(w, h), CornerRadius(w / 3))
                }
            } else {
                val path = Path().apply {
                    moveTo(s * 0.4f, s * 0.31f)
                    lineTo(s * 0.71f, s * 0.5f)
                    lineTo(s * 0.4f, s * 0.69f)
                    close()
                }
                drawPath(path, paper)
            }
        }
    }
}
