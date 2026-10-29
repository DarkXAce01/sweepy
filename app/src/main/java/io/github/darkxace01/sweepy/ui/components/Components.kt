package io.github.darkxace01.sweepy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaItem
import io.github.darkxace01.sweepy.ui.formatDuration
import io.github.darkxace01.sweepy.ui.theme.ChatGreen
import io.github.darkxace01.sweepy.ui.theme.LaterAmber
import io.github.darkxace01.sweepy.ui.theme.ScreenshotBlue
import io.github.darkxace01.sweepy.ui.theme.VideoOrange
import io.github.darkxace01.sweepy.ui.theme.Violet
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Clickable that also shrinks slightly while pressed, for a tactile feel. */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    role: Role = Role.Button,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = role, onClick = onClick)
}

/** A rounded progress bar that animates when the value changes. */
@Composable
fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    height: Dp = 6.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress",
    )
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** An icon inside a softly tinted circle. */
@Composable
fun IconBadge(icon: Painter, tint: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f)),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Round icon button on a subtle surface, used in top bars. */
@Composable
fun CircleIconButton(icon: Painter, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .pressable(onClick, pressedScale = 0.9f)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
    }
}

/** Square-cropped thumbnail with a duration badge for videos. */
@Composable
fun MediaThumbnail(item: MediaItem, modifier: Modifier = Modifier, cornerRadius: Dp = 14.dp, showBadge: Boolean = true) {
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (showBadge && item.isVideo) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_videocam),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    " " + formatDuration(item.durationMillis),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

// ---- Deck presentation --------------------------------------------------------------------------

@Composable
fun deckTitle(source: DeckSource): String = when (source) {
    DeckSource.All -> stringResource(R.string.deck_all)
    DeckSource.Screenshots -> stringResource(R.string.deck_screenshots)
    DeckSource.WhatsApp -> stringResource(R.string.deck_whatsapp)
    DeckSource.LargeVideos -> stringResource(R.string.deck_large_videos)
    DeckSource.Later -> stringResource(R.string.deck_later)
    is DeckSource.Folder -> source.name.ifBlank { stringResource(R.string.folder_unnamed) }
    is DeckSource.Month -> formatYearMonth(source.yearMonth)
}

@DrawableRes
fun deckIcon(source: DeckSource): Int = when (source) {
    DeckSource.All -> R.drawable.ic_image
    DeckSource.Screenshots -> R.drawable.ic_smartphone
    DeckSource.WhatsApp -> R.drawable.ic_chat
    DeckSource.LargeVideos -> R.drawable.ic_videocam
    DeckSource.Later -> R.drawable.ic_schedule
    is DeckSource.Folder -> R.drawable.ic_folder
    is DeckSource.Month -> R.drawable.ic_calendar
}

fun deckColor(source: DeckSource): Color = when (source) {
    DeckSource.Screenshots -> ScreenshotBlue
    DeckSource.WhatsApp -> ChatGreen
    DeckSource.LargeVideos -> VideoOrange
    DeckSource.Later -> LaterAmber
    else -> Violet
}

fun formatYearMonth(yearMonth: Int): String {
    val ym = YearMonth.of(yearMonth / 100, yearMonth % 100)
    return ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.year
}

fun formatCount(count: Number): String = NumberFormat.getIntegerInstance().format(count)

// ---- Screen scaffolding -------------------------------------------------------------------------

/** Back button, title and optional actions, used by the secondary screens. */
@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        CircleIconButton(
            icon = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        actions()
    }
}

/** Area pinned to the bottom of a screen that fades into the content above it. */
@Composable
fun BottomActionArea(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val background = MaterialTheme.colorScheme.background
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to background.copy(alpha = 0f), 0.25f to background))
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        content = content,
    )
}

/** Big rounded button used for the main action of a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: Painter? = null,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .pressable(onClick, enabled = enabled)
            .padding(horizontal = 20.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Centered illustration + message for empty screens. */
@Composable
fun EmptyState(icon: Painter, tint: Color, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(40.dp),
    ) {
        IconBadge(icon, tint, size = 96.dp)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
