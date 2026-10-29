package io.github.darkxace01.sweepy.data.duplicates

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Size
import androidx.core.graphics.get
import androidx.core.graphics.scale
import io.github.darkxace01.sweepy.data.MediaItem
import java.io.FileInputStream
import java.security.MessageDigest

/** Reads files to produce the hashes [DuplicateGrouper] compares. Blocking: call off the main thread. */
class MediaHasher(private val resolver: ContentResolver) {

    /**
     * SHA-256 of the file. Files up to [FULL_HASH_LIMIT] are read completely; bigger ones (long
     * videos) hash their size plus the first and last [SAMPLE_BYTES], which is enough to tell
     * different recordings apart without reading gigabytes. Returns null if the file can't be read.
     */
    fun contentHash(item: MediaItem): String? = runCatching {
        resolver.openFileDescriptor(item.uri, "r")?.use { pfd ->
            val digest = MessageDigest.getInstance("SHA-256")
            val length = pfd.statSize
            digest.update(length.toString().toByteArray())
            FileInputStream(pfd.fileDescriptor).use { input ->
                val buffer = ByteArray(BUFFER)
                if (length <= FULL_HASH_LIMIT) {
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        digest.update(buffer, 0, read)
                    }
                } else {
                    val channel = input.channel
                    for (start in longArrayOf(0, length - SAMPLE_BYTES)) {
                        channel.position(start)
                        var remaining = SAMPLE_BYTES
                        while (remaining > 0) {
                            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (read < 0) break
                            digest.update(buffer, 0, read)
                            remaining -= read
                        }
                    }
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }.getOrNull()

    /**
     * A 64-bit difference hash: shrink to 9×8 grayscale and record, for each pixel, whether it's
     * brighter than its right-hand neighbour. Robust to resizing and recompression.
     */
    fun visualHash(item: MediaItem): Long? = thumbnail(item, HASH_THUMB)?.let { thumb ->
        val small = thumb.scale(9, 8)
        var hash = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                hash = hash shl 1
                if (luminance(small[x, y]) > luminance(small[x + 1, y])) hash = hash or 1L
            }
        }
        if (small !== thumb) small.recycle()
        thumb.recycle()
        hash
    }

    /** Variance of the Laplacian over a grayscale thumbnail: blurry photos have few sharp edges, so a low score. */
    fun sharpness(item: MediaItem): Float? = thumbnail(item, SHARPNESS_THUMB)?.let { thumb ->
        val w = thumb.width
        val h = thumb.height
        if (w < 3 || h < 3) return@let null
        val pixels = IntArray(w * h)
        thumb.getPixels(pixels, 0, w, 0, 0, w, h)
        thumb.recycle()
        val gray = FloatArray(pixels.size) { luminance(pixels[it]) }
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val laplacian = gray[i - 1] + gray[i + 1] + gray[i - w] + gray[i + w] - 4 * gray[i]
                sum += laplacian
                sumSq += laplacian * laplacian
                count++
            }
        }
        val mean = sum / count
        (sumSq / count - mean * mean).toFloat()
    }

    private fun thumbnail(item: MediaItem, size: Int): Bitmap? = runCatching {
        resolver.loadThumbnail(item.uri, Size(size, size), null)
    }.getOrNull()

    private fun luminance(color: Int): Float =
        0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)

    private companion object {
        const val BUFFER = 64 * 1024
        const val FULL_HASH_LIMIT = 32L * 1024 * 1024
        const val SAMPLE_BYTES = 4L * 1024 * 1024
        const val HASH_THUMB = 64
        const val SHARPNESS_THUMB = 320
    }
}
