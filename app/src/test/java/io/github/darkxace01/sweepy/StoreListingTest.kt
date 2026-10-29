package io.github.darkxace01.sweepy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.darkxace01.sweepy.data.CleanStats
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.data.ThemeChoice
import io.github.darkxace01.sweepy.data.duplicates.DuplicateGroup
import io.github.darkxace01.sweepy.data.duplicates.DuplicateKind
import io.github.darkxace01.sweepy.data.duplicates.KeepReason
import io.github.darkxace01.sweepy.data.duplicates.MediaSignature
import io.github.darkxace01.sweepy.ui.DeckSummary
import io.github.darkxace01.sweepy.ui.DeckUiState
import io.github.darkxace01.sweepy.ui.DuplicateGroupUi
import io.github.darkxace01.sweepy.ui.DuplicatesSummary
import io.github.darkxace01.sweepy.ui.DuplicatesUiState
import io.github.darkxace01.sweepy.ui.HomeUiState
import io.github.darkxace01.sweepy.ui.ReviewGroup
import io.github.darkxace01.sweepy.ui.ReviewUiState
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.MarkIcon
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.deck.DeckScreen
import io.github.darkxace01.sweepy.ui.deck.SwipeCard
import io.github.darkxace01.sweepy.ui.deck.SwipeCardState
import io.github.darkxace01.sweepy.ui.duplicates.DuplicatesScreen
import io.github.darkxace01.sweepy.ui.home.HomeScreen
import io.github.darkxace01.sweepy.ui.review.ReviewScreen
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import io.github.darkxace01.sweepy.ui.theme.SweepyTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the Play Store graphics (phone screenshots, feature graphic, hi-res icon) from the real
 * screens. Needs sample photos in docs/store/photos-raw (not in git), so it skips without them.
 * Run `./gradlew recordRoborazziPlayDebug --tests '*StoreListingTest*'`; output goes to
 * app/build/outputs/store.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-xxhdpi")
class StoreListingTest {

    private val photoDir = File("../docs/store/photos-raw")

    @Before fun needsPhotos() = assumeTrue(photoDir.isDirectory)

    // ---- Phone screenshots (1080 × 1920) -----------------------------------------------------------

    @Test fun shot1_swipe() = render("phone_1_swipe") {
        Slide("01 · Left deletes, right keeps", listOf("Swipe your", "gallery {clean}.")) {
            Phone(width = 262.dp, top = 196.dp, overlay = {
                // The top print, caught mid-swipe on its way out.
                val state = SwipeCardState(initialOffset = Offset(-660f, 90f))
                Box(Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 120.dp, bottom = 113.dp)) {
                    SwipeCard(library[0], state, onSwiped = {}, onInfo = {}, modifier = Modifier.fillMaxSize().onSizeChanged { state.size = it })
                }
            }) {
                DeckScreen(
                    source = DeckSource.Folder(0, "Camera"),
                    state = DeckUiState(loading = false, deck = library.drop(1), totalCount = 4210, markedCount = 38, markedBytes = 96_000_000, canUndo = true),
                    showHint = false, onBack = {}, onDecide = { _, _ -> }, onUndo = { null }, onOpenReview = {}, onDismissHint = {},
                )
            }
        }
    }

    @Test fun shot2_review() = render("phone_2_review") {
        Slide("02 · The review bin", listOf("Nothing goes", "until you say {so}.")) {
            Phone(width = 262.dp, top = 196.dp) {
                ReviewScreen(
                    state = ReviewUiState(
                        groups = listOf(ReviewGroup("Camera", library.drop(2).take(9)), ReviewGroup("Downloads", library.drop(14).take(5))),
                        count = 14,
                        bytes = 58_000_000,
                    ),
                    onBack = {}, onKeep = {}, onUnmarkAll = {}, onCommit = {}, initiallyCircled = setOf(library[4].id, library[9].id),
                )
            }
        }
    }

    @Test fun shot3_duplicates() = render("phone_3_duplicates") {
        Slide("03 · Bursts, copies, look-alikes", listOf("Duplicates,", "{found} for you.")) {
            Phone(width = 262.dp, top = 196.dp) {
                val shots = (0 until 6).map { i ->
                    library[0].copy(id = BURST + i, dateTakenMillis = 1_760_000_000_000L + i * 600L, width = 3000, height = 4000, sizeBytes = 3_400_000L + i * 120_000L)
                }
                val group = DuplicateGroup(DuplicateKind.SIMILAR, shots.mapIndexed { i, it -> MediaSignature(it, sharpness = if (i == 2) 90f else 40f + i) })
                DuplicatesScreen(
                    state = DuplicatesUiState(scanning = false, groups = List(4) { DuplicateGroupUi(group, shots[2], KeepReason.SHARPEST, emptySet()) }),
                    onBack = {}, onResolve = { _, _ -> }, onUndo = {},
                )
            }
        }
    }

    @Test fun shot4_home() = render("phone_4_home") {
        Slide("04 · By folder, month or size", listOf("See where your", "space {went}.")) {
            Phone(width = 262.dp, top = 196.dp) { Home() }
        }
    }

    @Test fun shot5_themes() = render("phone_5_themes") {
        Slide("05 · Three looks", listOf("Pick the one", "that feels {yours}.")) {
            Box(Modifier.fillMaxSize()) {
                Phone(width = 176.dp, top = 250.dp, rotation = -10f, x = (-104).dp, theme = ThemeChoice.RISO) { Deck() }
                Phone(width = 176.dp, top = 250.dp, rotation = 10f, x = 104.dp, dark = true) { Review() }
                Phone(width = 200.dp, top = 214.dp, theme = ThemeChoice.POP) { Home() }
            }
        }
    }

    @Test fun shot6_private() = render("phone_6_private") {
        Slide("06 · Private by design", listOf("Your photos stay", "on your {phone}.")) {
            Column(Modifier.align(Alignment.TopStart).padding(start = 30.dp, end = 24.dp, top = 196.dp)) {
                for (line in listOf("Internet access", "Ads", "Accounts or sign-ups", "Tracking or analytics")) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 9.dp)) {
                        MarkIcon(Mark.CROSS, Sweepy.colors.pencil, size = 30.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(line, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp), color = Sweepy.colors.ink)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 9.dp)) {
                    MarkIcon(Mark.CIRCLE, Sweepy.colors.keep, size = 30.dp)
                    Spacer(Modifier.width(16.dp))
                    Text("Works fully offline", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, fontStyle = FontStyle.Italic), color = Sweepy.colors.ink)
                }
                Spacer(Modifier.height(6.dp))
                Eyebrow("Deleted by mistake? 30 days to restore", small = true, modifier = Modifier.padding(start = 46.dp))
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(150.dp)) {
                Print(library[2], Modifier.align(Alignment.BottomCenter).offset((-96).dp, 78.dp), rotation = -12f, small = true)
                Print(library[4], Modifier.align(Alignment.BottomCenter).offset(96.dp, 82.dp), rotation = 10f, small = true)
                Print(library[1], Modifier.align(Alignment.BottomCenter).offset(0.dp, 64.dp), rotation = -3f, small = true, mark = Mark.CIRCLE)
            }
        }
    }

    // ---- Feature graphic (1024 × 500) and icon (512 × 512) -------------------------------------------

    @Config(qualifiers = "w512dp-h250dp-land-xhdpi")
    @Test fun feature_graphic() = render("feature_graphic") {
        Paper {
            Column(Modifier.align(Alignment.CenterStart).padding(start = 40.dp)) {
                Eyebrow("Photo & video cleaner", color = Sweepy.colors.pencil)
                Text("Sweepy", style = MaterialTheme.typography.displayLarge.copy(fontSize = 76.sp, lineHeight = 76.sp), color = Sweepy.colors.ink)
                Spacer(Modifier.height(2.dp))
                Headline("Swipe your gallery {clean}.", size = 24)
            }
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 22.dp).size(210.dp, 250.dp)) {
                Print(library[6], Modifier.align(Alignment.Center).offset((-50).dp, 6.dp), rotation = -11f)
                Print(library[3], Modifier.align(Alignment.Center).offset(50.dp, (-2).dp), rotation = 9f, mark = Mark.CIRCLE)
                Print(library[0], Modifier.align(Alignment.Center).offset(0.dp, 8.dp), rotation = -2f, mark = Mark.CROSS)
            }
        }
    }

    @Config(qualifiers = "w256dp-h256dp-xhdpi")
    @Test fun icon() = render("icon_512", withPhotos = false) {
        // Full-bleed square; Play rounds the corners itself, so leave a little more margin than a launcher does.
        Box(Modifier.fillMaxSize().background(colorResource(R.color.ic_launcher_background)), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(256.dp * 108f / 86f))
        }
    }

    // ---- Building blocks ---------------------------------------------------------------------------

    /** The page every screenshot sits on: film-edge caption, a serif headline with one word circled, then the phone. */
    @Composable
    private fun Slide(eyebrow: String, headline: List<String>, phone: @Composable BoxScope.() -> Unit) {
        Paper {
            Column(Modifier.padding(start = 28.dp, end = 20.dp, top = 40.dp)) {
                Eyebrow(eyebrow, color = Sweepy.colors.pencil)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy((-14).dp)) { headline.forEach { Headline(it, size = 46) } }
            }
            phone()
        }
    }

    /** One line of headline; the word in {braces} gets a grease-pencil circle. */
    @Composable
    private fun Headline(line: String, size: Int) {
        val style = MaterialTheme.typography.displayMedium.copy(fontSize = size.sp, lineHeight = (size * 1.08).sp)
        val before = line.substringBefore('{')
        val circled = line.substringAfter('{', "").substringBefore('}')
        val after = line.substringAfter('}', "")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(before, style = style, color = Sweepy.colors.ink)
            if (circled.isNotEmpty()) {
                Text(
                    circled,
                    style = style.copy(fontStyle = FontStyle.Italic),
                    color = Sweepy.colors.ink,
                    modifier = Modifier
                        .pencilMark(Mark.CIRCLE, Sweepy.colors.pencil, { 1f }, strokeWidth = 0.075f)
                        .padding(horizontal = (size * 0.22).dp, vertical = (size * 0.06).dp),
                )
            }
            Text(after, style = style, color = Sweepy.colors.ink, modifier = Modifier.offset(x = -(size * 0.16).dp))
        }
    }

    /**
     * A phone showing [screen] at real size (412 × 892dp), scaled down to [width]. [overlay] is drawn in the
     * same coordinates but isn't clipped to the glass, so things can fly out of the phone.
     */
    @Composable
    private fun BoxScope.Phone(
        width: Dp,
        top: Dp,
        rotation: Float = 0f,
        x: Dp = 0.dp,
        theme: ThemeChoice = ThemeChoice.CONTACT_SHEET,
        dark: Boolean = false,
        overlay: (@Composable BoxScope.() -> Unit)? = null,
        screen: @Composable () -> Unit,
    ) {
        val real = DpSize(412.dp, 892.dp)
        val bezel = width * 0.028f
        val glass = width - bezel * 2
        val scale = glass / real.width
        val height = real.height * scale + bezel * 2
        val corner = width * 0.13f
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(x, top)
                .rotate(rotation)
                .size(width, height),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .shadow(28.dp, RoundedCornerShape(corner), ambientColor = Color.Black, spotColor = Color.Black)
                    .background(Color(0xFF1A1917), RoundedCornerShape(corner))
                    .padding(bezel)
                    .clip(RoundedCornerShape(corner - bezel)),
            ) {
                Scaled(real, scale) {
                    SweepyTheme(darkTheme = dark, theme = theme) {
                        Column(Modifier.fillMaxSize().background(Sweepy.colors.paper)) {
                            StatusBar()
                            Box(Modifier.fillMaxWidth().weight(1f)) { screen() }
                        }
                    }
                }
                // Camera punch hole.
                Box(Modifier.align(Alignment.TopCenter).padding(top = 9.dp * scale * 1.6f).size(10.dp * scale * 1.6f).background(Color.Black, CircleShape))
            }
            if (overlay != null) {
                Box(Modifier.padding(bezel)) {
                    Scaled(real, scale) { SweepyTheme(darkTheme = dark, theme = theme) { Box(Modifier.fillMaxSize(), content = overlay) } }
                }
            }
        }
    }

    @Composable
    private fun Scaled(real: DpSize, scale: Float, content: @Composable () -> Unit) {
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(real)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) { content() }
    }

    @Composable
    private fun StatusBar() {
        val ink = Sweepy.colors.ink
        Row(
            Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("10:08", style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = ink)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                for (h in listOf(4, 6, 8, 10)) Box(Modifier.width(3.dp).height(h.dp).background(ink, RoundedCornerShape(1.dp)))
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(22.dp, 11.dp).background(ink.copy(alpha = 0.35f), RoundedCornerShape(3.dp))) {
                Box(Modifier.fillMaxHeight().width(15.dp).background(ink, RoundedCornerShape(3.dp)))
            }
        }
    }

    /** A loose print for the feature graphic, optionally marked in pencil. */
    @Composable
    private fun Print(item: MediaItem, modifier: Modifier, rotation: Float, mark: Mark? = null, small: Boolean = false) {
        val color = if (mark == Mark.CIRCLE) Sweepy.colors.keep else Sweepy.colors.pencil
        Box(
            modifier
                .rotate(rotation)
                .shadow(10.dp, RoundedCornerShape(3.dp))
                .background(Sweepy.colors.print, RoundedCornerShape(3.dp))
                .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 22.dp),
        ) {
            Box(Modifier.size(if (small) 112.dp else 100.dp, if (small) 140.dp else 132.dp).then(if (mark != null) Modifier.pencilMark(mark, color, { 1f }, strokeWidth = 0.07f) else Modifier)) {
                Image(photo(item.id.toInt()).asImageBitmap(), null, Modifier.fillMaxSize().clip(RoundedCornerShape(1.dp)), contentScale = ContentScale.Crop)
            }
        }
    }

    @Composable
    private fun Home() {
        val folders = listOf("Camera" to 4210, "Downloads" to 611, "Screenshots" to 1840, "Travel" to 402, "Family" to 318, "Recordings" to 120)
            .mapIndexed { i, (name, count) ->
                DeckSummary(DeckSource.Folder(i.toLong(), name), count, count * 2_400_000L, (count * (0.3 + i * 0.12)).toInt(), library.drop(i * 3).take(4))
            }
        val months = listOf(202610, 202609, 202608, 202607, 202606).mapIndexed { i, ym ->
            DeckSummary(DeckSource.Month(ym), 300 - i * 40, (300 - i * 40) * 3_100_000L, if (i == 1) 0 else 200 - i * 30, library.drop(i * 4 + 2).take(4))
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
                recent = library.take(12),
                duplicates = DuplicatesSummary(scanning = false, groupCount = 41, reclaimableBytes = 2_300_000_000, covers = library.drop(8).take(4)),
                quickDecks = listOf(
                    DeckSummary(DeckSource.Screenshots, 1840, 1_900_000_000, 1210, library.drop(12).take(4)),
                    DeckSummary(DeckSource.LargeVideos, 48, 6_700_000_000, 41, library.drop(16).take(4)),
                    DeckSummary(DeckSource.Later, 24, 120_000_000, 24, library.drop(20).take(4)),
                ),
                folders = folders,
                months = months,
            ),
            partialAccess = false,
            onOpenDeck = {}, onOpenReview = {}, onOpenTrash = {}, onOpenSettings = {}, onOpenDuplicates = {}, onRequestFullAccess = {},
        )
    }

    @Composable
    private fun Deck() = DeckScreen(
        source = DeckSource.Folder(1, "Travel"),
        state = DeckUiState(loading = false, deck = library.drop(5), totalCount = 402, markedCount = 12, markedBytes = 41_000_000, canUndo = true),
        showHint = false, onBack = {}, onDecide = { _, _ -> }, onUndo = { null }, onOpenReview = {}, onDismissHint = {},
    )

    @Composable
    private fun Review() = ReviewScreen(
        state = ReviewUiState(groups = listOf(ReviewGroup("Camera", library.drop(2).take(9)), ReviewGroup("Downloads", library.drop(14).take(5))), count = 14, bytes = 58_000_000),
        onBack = {}, onKeep = {}, onUnmarkAll = {}, onCommit = {}, initiallyCircled = setOf(library[4].id),
    )

    // ---- Fake library backed by real sample photos -------------------------------------------------

    /** Picsum/Unsplash photo ids, in the order the library shows them. The first one is the hero. */
    private val photoIds = listOf(
        237, 110, 392, 429, 164, 15, 433, 82, 292, 29, 37, 244, 152, 177, 76, 306, 188, 103, 380, 129,
        42, 200, 219, 274, 318, 10, 401, 137, 349, 334,
    )

    private val library: List<MediaItem> = photoIds.mapIndexed { i, pid ->
        val isVideo = i % 7 == 5
        MediaItem(
            id = pid.toLong(),
            uri = Uri.parse("content://media/external/images/media/$pid"),
            isVideo = isVideo,
            mimeType = if (isVideo) "video/mp4" else "image/jpeg",
            displayName = "IMG_20251${i % 10}0${i % 9}_0${i}12.jpg",
            folder = "Camera",
            bucketId = 0,
            relativePath = "DCIM/Camera/",
            sizeBytes = 2_100_000L + i * 431_000L,
            dateTakenMillis = 1_760_000_000_000 - i * 86_400_000L * 6,
            dateAddedSeconds = 1_760_000_000L - i * 86_400L,
            width = 3000,
            height = 4000,
            durationMillis = if (isVideo) 34_000L + i * 1000 else 0,
        )
    }

    private val cache = HashMap<Int, Bitmap>()

    /** Ids from [BURST] up are near-identical frames of the hero photo, each nudged a little. */
    private fun photo(id: Int): Bitmap = cache.getOrPut(id) {
        if (id >= BURST) {
            val src = photo(photoIds[0])
            val k = id - BURST.toInt()
            val inset = 18
            Bitmap.createBitmap(src, inset + (k % 3 - 1) * 9, inset + (k % 2) * 8, src.width - inset * 2 - 10, src.height - inset * 2 - 10)
        } else {
            BitmapFactory.decodeFile(File(photoDir, "p$id.jpg").path) ?: Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun render(name: String, withPhotos: Boolean = true, content: @Composable () -> Unit) {
        captureRoboImage("build/outputs/store/$name.png") {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler { request ->
                    val id = (request.data as? Uri)?.lastPathSegment?.toIntOrNull()
                    if (id == null || !withPhotos) ColorImage(0xFF444444.toInt()) else photo(id).asImage()
                },
            ) {
                SweepyTheme(darkTheme = false, theme = ThemeChoice.CONTACT_SHEET, content = content)
            }
        }
    }

    private companion object {
        const val BURST = 5_000L
    }
}
