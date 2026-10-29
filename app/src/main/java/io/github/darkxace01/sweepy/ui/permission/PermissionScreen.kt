package io.github.darkxace01.sweepy.ui.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.ui.components.IconBadge
import io.github.darkxace01.sweepy.ui.components.PrimaryButton
import io.github.darkxace01.sweepy.ui.theme.ChatGreen
import io.github.darkxace01.sweepy.ui.theme.DeleteRed
import io.github.darkxace01.sweepy.ui.theme.KeepGreen
import io.github.darkxace01.sweepy.ui.theme.LaterAmber
import io.github.darkxace01.sweepy.ui.theme.ScreenshotBlue
import io.github.darkxace01.sweepy.ui.theme.Violet
import io.github.darkxace01.sweepy.ui.theme.VioletDeep

@Composable
fun PermissionScreen(
    denied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            CardFan()
            Spacer(Modifier.height(36.dp))
            Text(
                stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.permission_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(18.dp),
            ) {
                Promise(painterResource(R.drawable.ic_shield), KeepGreen, R.string.promise_private_title, R.string.promise_private_body)
                Promise(painterResource(R.drawable.ic_restore), Violet, R.string.promise_safe_title, R.string.promise_safe_body)
                Promise(painterResource(R.drawable.ic_sparkle), LaterAmber, R.string.promise_free_title, R.string.promise_free_body)
            }

            Spacer(Modifier.height(28.dp))
            if (denied) {
                Text(
                    stringResource(R.string.permission_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                PrimaryButton(
                    text = stringResource(R.string.permission_open_settings),
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.permission_grant),
                    onClick = onGrant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Promise(icon: Painter, tint: Color, title: Int, body: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, tint, size = 42.dp)
        Column(Modifier.padding(start = 14.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Three fanned-out photo cards, the front one getting a "keep" tick. */
@Composable
private fun CardFan() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(210.dp)) {
        FanCard(listOf(ScreenshotBlue, Color(0xFF1D4ED8)), painterResource(R.drawable.ic_smartphone), rotation = -16f, offsetX = -78f)
        FanCard(listOf(ChatGreen, Color(0xFF15803D)), painterResource(R.drawable.ic_chat), rotation = 16f, offsetX = 78f)
        FanCard(listOf(Violet, VioletDeep), painterResource(R.drawable.ic_image), rotation = 0f, offsetX = 0f, elevated = true)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer {
                    translationX = 52.dp.toPx()
                    translationY = (-78).dp.toPx()
                }
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .background(KeepGreen),
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer {
                    translationX = (-128).dp.toPx()
                    translationY = 64.dp.toPx()
                }
                .size(34.dp)
                .clip(RoundedCornerShape(50))
                .background(DeleteRed),
        ) {
            Text("×", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun FanCard(colors: List<Color>, icon: Painter, rotation: Float, offsetX: Float, elevated: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                rotationZ = rotation
                translationX = offsetX.dp.toPx()
                translationY = if (elevated) 0f else 14.dp.toPx()
                val s = if (elevated) 1f else 0.9f
                scaleX = s
                scaleY = s
                shadowElevation = if (elevated) 24.dp.toPx() else 8.dp.toPx()
                shape = RoundedCornerShape(26.dp)
                clip = true
            }
            .size(width = 132.dp, height = 176.dp)
            .background(Brush.linearGradient(colors)),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(52.dp))
    }
}
