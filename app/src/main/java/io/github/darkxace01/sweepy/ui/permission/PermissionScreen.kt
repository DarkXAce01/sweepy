package io.github.darkxace01.sweepy.ui.permission

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.Hairline
import io.github.darkxace01.sweepy.ui.components.InkButton
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.pencilMark
import io.github.darkxace01.sweepy.ui.components.reveal
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import kotlinx.coroutines.delay

@Composable
fun PermissionScreen(
    denied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Paper {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontStyle = FontStyle.Italic,
                color = Sweepy.colors.ink,
                modifier = Modifier.reveal(0),
            )
            Spacer(Modifier.height(20.dp))
            PrintPile(Modifier.align(Alignment.CenterHorizontally).reveal(1))
            Spacer(Modifier.height(36.dp))
            Eyebrow(stringResource(R.string.permission_eyebrow), color = Sweepy.colors.pencil, modifier = Modifier.reveal(2))
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.permission_title),
                style = MaterialTheme.typography.displaySmall,
                color = Sweepy.colors.ink,
                modifier = Modifier.reveal(2),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.permission_body),
                style = MaterialTheme.typography.bodyLarge,
                color = Sweepy.colors.inkMuted,
                modifier = Modifier.reveal(3),
            )
            Spacer(Modifier.height(28.dp))

            Column(Modifier.reveal(4)) {
                Hairline()
                Promise(1, R.string.promise_private_title, R.string.promise_private_body)
                Hairline()
                Promise(2, R.string.promise_safe_title, R.string.promise_safe_body)
                Hairline()
                Promise(3, R.string.promise_free_title, R.string.promise_free_body)
                Hairline()
            }

            Spacer(Modifier.height(28.dp))
            if (denied) {
                Text(
                    stringResource(R.string.permission_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sweepy.colors.pencil,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                InkButton(stringResource(R.string.permission_open_settings), onOpenSettings, Modifier.fillMaxWidth(), trailing = "→")
            } else {
                InkButton(stringResource(R.string.permission_grant), onGrant, Modifier.fillMaxWidth().reveal(5), trailing = "→")
            }
        }
    }
}

@Composable
private fun Promise(number: Int, title: Int, body: Int) {
    Row(Modifier.padding(vertical = 14.dp)) {
        Eyebrow("0$number", color = Sweepy.colors.pencil, modifier = Modifier.width(36.dp).padding(top = 2.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = Sweepy.colors.ink)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = Sweepy.colors.inkMuted)
        }
    }
}

/**
 * Three little prints dropped on the table, each getting marked in turn: crossed out, circled,
 * starred. It shows what the app does before a single photo is loaded.
 */
@Composable
private fun PrintPile(modifier: Modifier = Modifier) {
    val inspection = LocalInspectionMode.current
    val marks = remember { List(3) { Animatable(if (inspection) 1f else 0f) } }
    LaunchedEffect(Unit) {
        delay(450)
        for (mark in marks) {
            mark.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
            delay(120)
        }
    }
    Box(modifier.size(width = 340.dp, height = 210.dp)) {
        MiniPrint(
            scene = Scene.Dusk,
            mark = Mark.CROSS,
            markColor = Sweepy.colors.pencil,
            progress = { marks[0].value },
            angle = -9f,
            modifier = Modifier.align(Alignment.CenterStart).offset(x = 0.dp, y = 14.dp),
        )
        MiniPrint(
            scene = Scene.Sea,
            mark = Mark.STAR,
            markColor = Sweepy.colors.favorite,
            progress = { marks[2].value },
            angle = 8f,
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 0.dp, y = 18.dp),
        )
        MiniPrint(
            scene = Scene.Hills,
            mark = Mark.CIRCLE,
            markColor = Sweepy.colors.keep,
            progress = { marks[1].value },
            angle = -1.5f,
            size = 140.dp,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

private enum class Scene { Dusk, Hills, Sea }

@Composable
private fun MiniPrint(
    scene: Scene,
    mark: Mark,
    markColor: Color,
    progress: () -> Float,
    angle: Float,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
) {
    Column(
        modifier
            .rotate(angle)
            .width(size)
            .shadow(10.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(8.dp))
            .background(Sweepy.colors.print)
            .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 22.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(size * 0.82f).clip(RoundedCornerShape(3.dp))) {
            SceneArt(scene, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().padding(8.dp).pencilMark(mark, markColor, progress, strokeWidth = 0.065f))
        }
    }
}

/** Simple painted scenes in muted film tones, standing in for photos. */
@Composable
private fun SceneArt(scene: Scene, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (scene) {
            Scene.Dusk -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3B3F5C), Color(0xFFB9785E), Color(0xFFE2B07A))))
                drawCircle(Color(0xFFF3D9A4), radius = w * 0.11f, center = Offset(w * 0.62f, h * 0.62f))
                drawRect(Color(0xFF2A2730), topLeft = Offset(0f, h * 0.72f))
            }
            Scene.Hills -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFA9C3C9), Color(0xFFE6E0CC))))
                drawCircle(Color(0xFFF6EBCB), radius = w * 0.09f, center = Offset(w * 0.74f, h * 0.27f))
                drawPath(
                    Path().apply {
                        moveTo(0f, h * 0.68f)
                        cubicTo(w * 0.25f, h * 0.48f, w * 0.45f, h * 0.55f, w * 0.62f, h * 0.64f)
                        cubicTo(w * 0.78f, h * 0.72f, w * 0.9f, h * 0.58f, w, h * 0.6f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    },
                    Color(0xFF6E8A63),
                )
                drawPath(
                    Path().apply {
                        moveTo(0f, h * 0.84f)
                        cubicTo(w * 0.3f, h * 0.72f, w * 0.6f, h * 0.9f, w, h * 0.78f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    },
                    Color(0xFF4C6448),
                )
            }
            Scene.Sea -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFD7DCD8), Color(0xFF9DB4BB))))
                drawRect(Brush.verticalGradient(listOf(Color(0xFF4F7480), Color(0xFF2F4E5A)), startY = h * 0.55f), topLeft = Offset(0f, h * 0.55f))
                drawRect(Color(0xFFE9DEC4), topLeft = Offset(0f, h * 0.86f))
            }
        }
    }
}
