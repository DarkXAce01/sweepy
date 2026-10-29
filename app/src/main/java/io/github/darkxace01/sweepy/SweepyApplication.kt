package io.github.darkxace01.sweepy

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder

class SweepyApplication : Application(), SingletonImageLoader.Factory {

    // Coil (our image loader) can show a frame from a video as its thumbnail once this decoder is added.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
