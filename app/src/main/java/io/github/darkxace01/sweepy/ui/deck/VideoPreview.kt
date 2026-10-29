package io.github.darkxace01.sweepy.ui.deck

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.formatDuration
import kotlinx.coroutines.delay

/** Remembered across cards, so turning sound on once keeps it on for the next video. */
private var soundOn by mutableStateOf(false)

/**
 * Plays [item] silently on a loop over its thumbnail, so you can tell what a video is
 * without opening it. Pauses when the app goes to the background.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPreview(item: MediaItem, modifier: Modifier = Modifier) {
    // Screenshot tests and previews have no media decoder.
    if (LocalInspectionMode.current) return
    val context = LocalContext.current
    val player = remember(item.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(item.uri))
            repeatMode = Player.REPEAT_MODE_ONE
            // Jump to the nearest keyframe while scrubbing: much faster on long videos.
            setSeekParameters(SeekParameters.CLOSEST_SYNC)
            prepare()
        }
    }
    var started by rememberSaveable(item.id) { mutableStateOf(false) }
    DisposableEffect(player) { onDispose { player.release() } }
    LifecycleResumeEffect(player) {
        player.playWhenReady = true
        started = true
        onPauseOrDispose { player.playWhenReady = false }
    }
    player.volume = if (soundOn) 1f else 0f

    // Where the video is, polled while it plays. While scrubbing, the finger decides instead.
    var position by remember(item.id) { mutableLongStateOf(0L) }
    var scrub by remember(item.id) { mutableStateOf<Float?>(null) }
    LaunchedEffect(player) {
        while (true) {
            if (scrub == null) position = player.currentPosition
            delay(POSITION_POLL_MILLIS)
        }
    }
    val duration = item.durationMillis.takeIf { it > 0 } ?: player.duration.coerceAtLeast(0L)

    Box(modifier) {
        // The thumbnail underneath stays visible until the first frame is ready.
        ContentFrame(
            player = player,
            modifier = Modifier.matchParentSize(),
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            contentScale = ContentScale.Fit,
            shutter = {},
        )
        if (started) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                    .padding(start = 10.dp, end = 10.dp, top = 24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow(
                        stringResource(if (soundOn) R.string.video_sound_on else R.string.video_sound_off),
                        small = true,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .pressable({ soundOn = !soundOn }, pressedScale = 0.9f)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    if (duration > 0) {
                        val shown = scrub?.let { (it * duration).toLong() } ?: position
                        Eyebrow(
                            "${formatDuration(shown)} / ${formatDuration(duration)}",
                            small = true,
                            color = Color.White,
                        )
                    }
                }
                if (duration > 0) {
                    Scrubber(
                        progress = scrub ?: (position.toFloat() / duration).coerceIn(0f, 1f),
                        active = scrub != null,
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
}

/**
 * A thin timeline along the bottom of the video. Touch anywhere on it and slide to jump through
 * the video; it keeps the gesture, so the card doesn't swipe while you scrub.
 */
@Composable
private fun Scrubber(progress: Float, active: Boolean, onScrub: (Float) -> Unit, onScrubEnd: () -> Unit) {
    val label = stringResource(R.string.video_seek)
    val knob by animateDpAsState(if (active) 7.dp else 4.5.dp, label = "knob")
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onScrub((down.position.x / size.width).coerceIn(0f, 1f))
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (!change.pressed) break
                        onScrub((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                    onScrubEnd()
                }
            },
    ) {
        val y = size.height / 2
        val stroke = 2.dp.toPx()
        val x = size.width * progress
        drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(0f, y), Offset(x, y), stroke, StrokeCap.Round)
        drawCircle(Color.White, knob.toPx(), Offset(x, y))
    }
}

private const val POSITION_POLL_MILLIS = 200L
