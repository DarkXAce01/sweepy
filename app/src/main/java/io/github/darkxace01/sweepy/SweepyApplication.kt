package io.github.darkxace01.sweepy

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import io.github.darkxace01.sweepy.data.AppContainer
import io.github.darkxace01.sweepy.reminders.ReminderSchedule
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SweepyApplication : Application(), SingletonImageLoader.Factory {

    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Keep the background reminder check in step with the switch in Settings.
        MainScope().launch {
            container.prefs.settings.map { it.reminders }.distinctUntilChanged().collect { on ->
                ReminderSchedule.sync(this@SweepyApplication, on)
            }
        }
    }

    // Coil (our image loader) can show a frame from a video as its thumbnail once this decoder is added.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
