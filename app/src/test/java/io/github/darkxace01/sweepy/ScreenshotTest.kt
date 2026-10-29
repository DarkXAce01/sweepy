package io.github.darkxace01.sweepy

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.darkxace01.sweepy.data.CleanStats
import io.github.darkxace01.sweepy.data.DeckFilter
import io.github.darkxace01.sweepy.data.DeckOrder
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaKind
import io.github.darkxace01.sweepy.ui.deck.FilterSheetContent
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.TrashedItem
import io.github.darkxace01.sweepy.ui.DeckSummary
import io.github.darkxace01.sweepy.ui.DeckUiState
import io.github.darkxace01.sweepy.ui.HomeUiState
import io.github.darkxace01.sweepy.ui.ReviewGroup
import io.github.darkxace01.sweepy.ui.ReviewUiState
import io.github.darkxace01.sweepy.ui.TrashUiState
import io.github.darkxace01.sweepy.ui.deck.DeckScreen
import io.github.darkxace01.sweepy.ui.deck.SwipeCard
import io.github.darkxace01.sweepy.ui.deck.SwipeCardState
import io.github.darkxace01.sweepy.ui.home.HomeScreen
import io.github.darkxace01.sweepy.ui.permission.PermissionScreen
import io.github.darkxace01.sweepy.ui.review.ReviewScreen
import io.github.darkxace01.sweepy.ui.settings.SettingsScreen
import io.github.darkxace01.sweepy.ui.LocalAppSettings
import io.github.darkxace01.sweepy.data.AppSettings
import io.github.darkxace01.sweepy.data.ThemeChoice
import io.github.darkxace01.sweepy.ui.theme.SweepyTheme
import io.github.darkxace01.sweepy.ui.trash.TrashScreen
import io.github.darkxace01.sweepy.data.duplicates.DuplicateGroup
import io.github.darkxace01.sweepy.data.duplicates.DuplicateKind
import io.github.darkxace01.sweepy.data.duplicates.KeepReason
import io.github.darkxace01.sweepy.data.duplicates.MediaSignature
import io.github.darkxace01.sweepy.ui.DuplicateGroupUi
import io.github.darkxace01.sweepy.ui.DuplicatesSummary
import io.github.darkxace01.sweepy.ui.DuplicatesUiState
import io.github.darkxace01.sweepy.ui.duplicates.DuplicatesScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every screen with a fake photo library, so the UI can be checked without a phone.
 * Run `./gradlew recordRoborazziPlayDebug`; images are written to app/build/outputs/roborazzi.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class ScreenshotTest {

    @Test fun permission_light() = capture("permission_light") { PermissionScreen(denied = false, onGrant = {}, onOpenSettings = {}) }

    @Test fun permission_dark() = capture("permission_dark", dark = true) {
        PermissionScreen(denied = false, onGrant = {}, onOpenSettings = {})
    }

    @Test fun home_light() = capture("home_light") { Home() }

    @Test fun home_dark() = capture("home_dark", dark = true) { Home() }

    @Config(qualifiers = "w412dp-h2000dp-xxhdpi")
    @Test fun home_full_light() = capture("home_full_light") { Home() }

    @Test fun home_loading() = capture("home_loading") {
        HomeScreen(HomeUiState(), partialAccess = false, onOpenDeck = {}, onOpenReview = {}, onOpenTrash = {}, onOpenSettings = {}, onOpenDuplicates = {}, onRequestFullAccess = {})
    }

    @Test fun deck_light() = capture("deck_light") { Deck() }

    @Test fun deck_dark() = capture("deck_dark", dark = true) { Deck() }

    @Test fun deck_hint() = capture("deck_hint", dark = true) { Deck(showHint = true) }

    @Test fun deck_left_handed() = capture("deck_left_handed") {
        CompositionLocalProvider(LocalAppSettings provides AppSettings(leftHanded = true)) { Deck(showHint = true) }
    }

    @Test fun riso_home() = capture("riso_home", theme = ThemeChoice.RISO) { Home() }

    @Test fun riso_deck() = capture("riso_deck", theme = ThemeChoice.RISO) { Deck() }

    @Test fun riso_night_deck() = capture("riso_night_deck", dark = true, theme = ThemeChoice.RISO) { Deck() }

    @Test fun riso_done() = capture("riso_done", theme = ThemeChoice.RISO) { DeckDone() }

    @Test fun phosphor_home() = capture("phosphor_home", dark = true, theme = ThemeChoice.PHOSPHOR) { Home() }

    @Test fun phosphor_deck() = capture("phosphor_deck", dark = true, theme = ThemeChoice.PHOSPHOR) { Deck() }

    @Test fun phosphor_review() = capture("phosphor_review", dark = true, theme = ThemeChoice.PHOSPHOR) { Review() }

    @Test fun phosphor_done() = capture("phosphor_done", dark = true, theme = ThemeChoice.PHOSPHOR) { DeckDone() }

    @Test fun settings_riso() = capture("settings_riso", theme = ThemeChoice.RISO) { Settings(ThemeChoice.RISO) }

    @Test fun settings_phosphor() = capture("settings_phosphor", dark = true, theme = ThemeChoice.PHOSPHOR) { Settings(ThemeChoice.PHOSPHOR) }

    @Test fun settings_light() = capture("settings_light") { Settings() }

    @Test fun settings_dark() = capture("settings_dark", dark = true) { Settings() }

    @Composable
    private fun Settings(theme: ThemeChoice = ThemeChoice.CONTACT_SHEET) = SettingsScreen(
        settings = AppSettings(theme = theme, leftHanded = true),
        keptCount = 1_204,
        onBack = {},
        onChange = {},
        onForgetKept = {},
        onForgetEverything = {},
    )

    @Test fun duplicates_similar() = capture("duplicates_similar") {
        val shots = library.filter { !it.isVideo }.take(5).mapIndexed { i, item ->
            item.copy(dateTakenMillis = 1_700_000_000_000L + i * 700L, width = 4000, height = 3000)
        }
        val group = DuplicateGroup(DuplicateKind.SIMILAR, shots.mapIndexed { i, it -> MediaSignature(it, sharpness = 40f + i) })
        DuplicatesScreen(
            state = DuplicatesUiState(scanning = false, groups = listOf(DuplicateGroupUi(group, shots[2], KeepReason.SHARPEST, emptySet()))),
            onBack = {}, onResolve = { _, _ -> }, onUndo = {},
        )
    }

    @Test fun duplicates_exact() = capture("duplicates_exact", dark = true) {
        val copies = List(2) { i -> library[1].copy(id = 900L + i, folder = if (i == 0) "Camera" else "WhatsApp Images") }
        val group = DuplicateGroup(DuplicateKind.EXACT, copies.map { MediaSignature(it, contentHash = "same") })
        DuplicatesScreen(
            state = DuplicatesUiState(scanning = false, groups = listOf(DuplicateGroupUi(group, copies[0], KeepReason.ORIGINAL, emptySet())), canUndo = true),
            onBack = {}, onResolve = { _, _ -> }, onUndo = {},
        )
    }

    @Test fun duplicates_scanning() = capture("duplicates_scanning") {
        DuplicatesScreen(DuplicatesUiState(scanning = true, progress = 0.42f), onBack = {}, onResolve = { _, _ -> }, onUndo = {})
    }

    @Test fun deck_done() = capture("deck_done") { DeckDone() }

    @Composable
    private fun DeckDone() {
        DeckScreen(
            source = DeckSource.Screenshots,
            state = DeckUiState(loading = false, deck = emptyList(), totalCount = 214, markedCount = 38, markedBytes = 96_000_000),
            showHint = false, onBack = {}, onDecide = { _, _ -> }, onUndo = { null }, onOpenReview = {}, onDismissHint = {},
        )
    }

    @Test fun deck_filtered_done() = capture("deck_filtered_done", dark = true) {
        DeckScreen(
            source = DeckSource.All,
            state = DeckUiState(
                loading = false, deck = emptyList(), totalCount = 412, markedCount = 12, markedBytes = 640_000_000,
                filter = DeckFilter(MediaKind.VIDEOS, DeckOrder.LARGEST), waitingPhotos = 2_318,
            ),
            showHint = false, onBack = {}, onDecide = { _, _ -> }, onUndo = { null }, onOpenReview = {}, onDismissHint = {},
        )
    }

    @Test fun filter_sheet() = capture("filter_sheet") {
        Box(Modifier.fillMaxSize().background(Sweepy.colors.paper), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().background(Sweepy.colors.surface).padding(top = 24.dp)) {
                FilterSheetContent(
                    DeckUiState(loading = false, deck = library, filter = DeckFilter(MediaKind.PHOTOS, DeckOrder.OLDEST), waitingPhotos = 2_318, waitingVideos = 412),
                    onChange = {}, onDone = {},
                )
            }
        }
    }

    @Test fun swipe_left() = captureDrag("swipe_left", Offset(-330f, 30f))

    @Test fun swipe_right() = captureDrag("swipe_right", Offset(360f, -20f))

    @Test fun swipe_up() = captureDrag("swipe_up", Offset(10f, -420f))

    @Test fun review_light() = capture("review_light") { Review() }

    @Test fun review_dark() = capture("review_dark", dark = true) { Review() }

    @Test fun trash_light() = capture("trash_light") {
        val now = 1_760_000_000_000
        val items = library.take(10).mapIndexed { i, item -> TrashedItem(item, now + (i * 3 + 1) * 86_400_000L) }
        TrashScreen(TrashUiState(loading = false, items = items), onBack = {}, onRestore = {}, onDeleteForever = {}, now = now)
    }

    @Test fun trash_empty() = capture("trash_empty", dark = true) {
        TrashScreen(TrashUiState(loading = false), onBack = {}, onRestore = {}, onDeleteForever = {})
    }

    // ---- Screens with fake data ------------------------------------------------------------------

    @Composable
    private fun Home() {
        val folders = listOf(
            "Camera" to 4210, "WhatsApp Images" to 3122, "Screenshots" to 1840, "Download" to 611,
            "Instagram" to 402, "Telegram" to 120,
        ).mapIndexed { i, (name, count) ->
            DeckSummary(DeckSource.Folder(i.toLong(), name), count, count * 2_400_000L, (count * (0.3 + i * 0.12)).toInt(), library.drop(i).take(4))
        }
        val months = listOf(202610, 202609, 202608, 202607, 202606).mapIndexed { i, ym ->
            DeckSummary(DeckSource.Month(ym), 300 - i * 40, (300 - i * 40) * 3_100_000L, if (i == 1) 0 else 200 - i * 30, library.drop(i + 6).take(4))
        }
        HomeScreen(
            state = HomeUiState(
                loading = false,
                totalCount = 18_402,
                totalBytes = 46_200_000_000,
                reviewedCount = 5_706,
                markedCount = 312,
                markedBytes = 1_420_000_000,
                laterCount = 24,
                stats = CleanStats(cleanedCount = 2_840, cleanedBytes = 7_300_000_000),
                recent = library.take(10),
                duplicates = DuplicatesSummary(scanning = false, groupCount = 41, reclaimableBytes = 2_300_000_000, covers = library.drop(5).take(4)),
                quickDecks = listOf(
                    DeckSummary(DeckSource.Screenshots, 1840, 1_900_000_000, 1210, library.drop(3).take(4)),
                    DeckSummary(DeckSource.WhatsApp, 3122, 4_100_000_000, 2650, library.drop(1).take(4)),
                    DeckSummary(DeckSource.LargeVideos, 48, 6_700_000_000, 41, library.drop(2).take(4)),
                    DeckSummary(DeckSource.Later, 24, 120_000_000, 24, library.drop(4).take(4)),
                ),
                folders = folders,
                months = months,
            ),
            partialAccess = false,
            onOpenDeck = {}, onOpenReview = {}, onOpenTrash = {}, onOpenSettings = {}, onOpenDuplicates = {}, onRequestFullAccess = {},
        )
    }

    @Composable
    private fun Deck(showHint: Boolean = false) {
        DeckScreen(
            source = DeckSource.WhatsApp,
            state = DeckUiState(
                loading = false,
                deck = library.drop(1),
                totalCount = 3122,
                markedCount = 38,
                markedBytes = 96_000_000,
                canUndo = true,
            ),
            showHint = showHint,
            onBack = {}, onDecide = { _, _ -> }, onUndo = { null }, onOpenReview = {}, onDismissHint = {},
        )
    }

    @Composable
    private fun Review() {
        ReviewScreen(
            state = ReviewUiState(
                groups = listOf(
                    ReviewGroup("WhatsApp Images", library.take(7)),
                    ReviewGroup("Screenshots", library.drop(7).take(5)),
                ),
                count = 12,
                bytes = 48_000_000,
            ),
            onBack = {}, onUnmark = {}, onUnmarkAll = {}, onCommit = {},
        )
    }

    private fun captureDrag(name: String, offset: Offset) = capture(name) {
        val state = SwipeCardState(initialOffset = offset)
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 80.dp)) {
            SwipeCard(
                item = library[1],
                state = state,
                onSwiped = {},
                onInfo = {},
                modifier = Modifier.fillMaxSize().onSizeChanged { state.size = it },
            )
        }
    }

    // ---- Rendering --------------------------------------------------------------------------------

    @OptIn(ExperimentalCoilApi::class)
    private fun capture(name: String, dark: Boolean = false, theme: ThemeChoice = ThemeChoice.CONTACT_SHEET, content: @Composable () -> Unit) {
        captureRoboImage("build/outputs/roborazzi/$name.png") {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler { request ->
                    val id = (request.data as? Uri)?.lastPathSegment?.toIntOrNull()
                    if (id == null) ColorImage(0xFF444444.toInt()) else fakePhoto(id).asImage()
                },
            ) {
                SweepyTheme(darkTheme = dark, theme = theme, content = content)
            }
        }
    }

    private val library: List<MediaItem> = (0 until 24).map { i ->
        val folder = when (i % 4) {
            0 -> "WhatsApp Images"
            1 -> "Camera"
            2 -> "WhatsApp Video"
            else -> "Screenshots"
        }
        MediaItem(
            id = i.toLong(),
            uri = Uri.parse("content://media/external/images/media/$i"),
            isVideo = i % 4 == 2,
            mimeType = if (i % 4 == 2) "video/mp4" else "image/jpeg",
            displayName = "IMG-2024031$i-WA00$i.jpg",
            folder = folder,
            bucketId = (i % 4).toLong(),
            relativePath = "Pictures/$folder/",
            sizeBytes = 180_000L + i * 731_000L,
            dateTakenMillis = 1_710_000_000_000 - i * 86_400_000L * 9,
            dateAddedSeconds = 1_710_000_000L - i * 86_400L,
            width = 1080,
            height = 1440,
            durationMillis = if (i % 4 == 2) 34_000L + i * 1000 else 0,
        )
    }

    /** A colorful stand-in "photo": a sunset landscape, a screenshot, or a greeting card. */
    private fun fakePhoto(id: Int): Bitmap {
        val w = 600
        val h = if (id % 3 == 0) 800 else if (id % 3 == 1) 450 else 760
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val palettes = listOf(
            intArrayOf(0xFFFF9A62.toInt(), 0xFFFF5E7E.toInt(), 0xFF6A3DE8.toInt()),
            intArrayOf(0xFF7DD3FC.toInt(), 0xFF38BDF8.toInt(), 0xFF0E7490.toInt()),
            intArrayOf(0xFFFDE68A.toInt(), 0xFFF59E0B.toInt(), 0xFFB45309.toInt()),
            intArrayOf(0xFFA7F3D0.toInt(), 0xFF34D399.toInt(), 0xFF047857.toInt()),
            intArrayOf(0xFFF9A8D4.toInt(), 0xFFEC4899.toInt(), 0xFF831843.toInt()),
        )
        if (id % 4 == 3) {
            // Screenshot: app UI with bars and text lines.
            canvas.drawColor(0xFFF4F4F5.toInt())
            paint.color = 0xFF2563EB.toInt()
            canvas.drawRect(0f, 0f, w.toFloat(), 90f, paint)
            paint.color = 0xFFD4D4D8.toInt()
            for (row in 0 until 9) {
                val top = 130f + row * 70f
                canvas.drawRoundRect(RectF(30f, top, 90f, top + 50f), 25f, 25f, paint)
                canvas.drawRoundRect(RectF(110f, top + 6f, 110f + (200 + (row * 47) % 260), top + 22f), 8f, 8f, paint)
                canvas.drawRoundRect(RectF(110f, top + 32f, 300f, top + 44f), 6f, 6f, paint)
            }
            return bitmap
        }
        val colors = palettes[id % palettes.size]
        paint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), colors, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
        paint.color = 0xCCFFFFFF.toInt()
        canvas.drawCircle(w * (0.3f + (id % 5) * 0.1f), h * 0.38f, 70f, paint)
        paint.color = 0x55000000
        val hills = Path().apply {
            moveTo(0f, h * 0.7f)
            quadTo(w * 0.3f, h * 0.52f, w * 0.55f, h * 0.68f)
            quadTo(w * 0.8f, h * 0.8f, w.toFloat(), h * 0.6f)
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(hills, paint)
        paint.color = 0x88000000.toInt()
        val front = Path().apply {
            moveTo(0f, h * 0.85f)
            quadTo(w * 0.5f, h * 0.72f, w.toFloat(), h * 0.88f)
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(front, paint)
        return bitmap
    }
}
