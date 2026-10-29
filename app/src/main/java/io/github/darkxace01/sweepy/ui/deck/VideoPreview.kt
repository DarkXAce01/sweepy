package io.github.darkxace01.sweepy.ui.deck

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.pressable

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
            Eyebrow(
                stringResource(if (soundOn) R.string.video_sound_on else R.string.video_sound_off),
                small = true,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .pressable({ soundOn = !soundOn }, pressedScale = 0.9f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}
