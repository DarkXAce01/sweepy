package io.github.darkxace01.sweepy.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.AppSettings
import io.github.darkxace01.sweepy.data.Appearance
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.Hairline
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.ScreenTopBar
import io.github.darkxace01.sweepy.ui.components.Segment
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.theme.Sweepy
import java.util.Locale

private const val SOURCE_URL = "https://github.com/DarkXAce01/sweepy"

private enum class Confirm { FORGET_KEPT, FORGET_ALL }

@Composable
fun SettingsScreen(
    settings: AppSettings,
    keptCount: Int,
    onBack: () -> Unit,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onForgetKept: () -> Unit,
    onForgetEverything: () -> Unit,
) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    Paper {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            ScreenTopBar(
                title = stringResource(R.string.settings_title),
                eyebrow = stringResource(R.string.app_name) + " " + version,
                onBack = onBack,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, bottom = 40.dp),
            ) {
                Section(1, stringResource(R.string.settings_section_look))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (option in Appearance.entries) {
                        Segment(
                            text = stringResource(
                                when (option) {
                                    Appearance.SYSTEM -> R.string.appearance_system
                                    Appearance.LIGHT -> R.string.appearance_light
                                    Appearance.DARK -> R.string.appearance_dark
                                },
                            ),
                            selected = settings.appearance == option,
                            onClick = { onChange { it.copy(appearance = option) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Section(2, stringResource(R.string.settings_section_swiping))
                Toggle(
                    title = stringResource(R.string.setting_left_handed),
                    body = stringResource(R.string.setting_left_handed_body),
                    checked = settings.leftHanded,
                    onChange = { value -> onChange { it.copy(leftHanded = value) } },
                )
                Hairline()
                Toggle(
                    title = stringResource(R.string.setting_haptics),
                    body = stringResource(R.string.setting_haptics_body),
                    checked = settings.haptics,
                    onChange = { value -> onChange { it.copy(haptics = value) } },
                )
                Hairline()
                Toggle(
                    title = stringResource(R.string.setting_autoplay),
                    body = stringResource(R.string.setting_autoplay_body),
                    checked = settings.autoplayVideos,
                    onChange = { value -> onChange { it.copy(autoplayVideos = value) } },
                )

                Section(3, stringResource(R.string.settings_section_memory))
                ActionRow(
                    title = stringResource(R.string.setting_forget_kept),
                    body = pluralStringResource(R.plurals.setting_forget_kept_body, keptCount, formatCount(keptCount)),
                    enabled = keptCount > 0,
                    onClick = { confirm = Confirm.FORGET_KEPT },
                )
                Hairline()
                ActionRow(
                    title = stringResource(R.string.setting_forget_all),
                    body = stringResource(R.string.setting_forget_all_body),
                    color = Sweepy.colors.pencil,
                    onClick = { confirm = Confirm.FORGET_ALL },
                )

                Section(4, stringResource(R.string.settings_section_about))
                ActionRow(
                    title = stringResource(R.string.setting_source),
                    body = SOURCE_URL.removePrefix("https://"),
                    trailing = "↗",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // No browser installed.
                        }
                    },
                )
                Hairline()
                InfoRow(stringResource(R.string.setting_privacy), stringResource(R.string.setting_privacy_body))
                Hairline()
                InfoRow(stringResource(R.string.setting_licenses), stringResource(R.string.setting_licenses_body))

                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(R.string.settings_signoff),
                    style = MaterialTheme.typography.headlineSmall,
                    fontStyle = FontStyle.Italic,
                    color = Sweepy.colors.inkMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }

    confirm?.let { which ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Sweepy.colors.surface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    stringResource(if (which == Confirm.FORGET_KEPT) R.string.setting_forget_kept else R.string.setting_forget_all),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Sweepy.colors.ink,
                )
            },
            text = {
                Text(
                    stringResource(if (which == Confirm.FORGET_KEPT) R.string.confirm_forget_kept else R.string.confirm_forget_all),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sweepy.colors.inkMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    if (which == Confirm.FORGET_KEPT) onForgetKept() else onForgetEverything()
                }) { Text(stringResource(R.string.confirm_forget), color = Sweepy.colors.pencil) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel), color = Sweepy.colors.ink) }
            },
        )
    }
}

@Composable
private fun Section(number: Int, title: String) {
    Row(Modifier.padding(top = 30.dp, bottom = 10.dp)) {
        Eyebrow(String.format(Locale.ROOT, "%02d", number), color = Sweepy.colors.pencil)
        Eyebrow(" — $title", color = Sweepy.colors.ink)
    }
}

@Composable
private fun Toggle(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pressable({ onChange(!checked) }, pressedScale = 0.99f, role = Role.Switch)
            .padding(vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Sweepy.colors.ink)
            Spacer(Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = Sweepy.colors.inkMuted)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Sweepy.colors.ink,
                checkedThumbColor = Sweepy.colors.paper,
                checkedBorderColor = Sweepy.colors.ink,
                uncheckedTrackColor = Sweepy.colors.paper,
                uncheckedThumbColor = Sweepy.colors.inkMuted,
                uncheckedBorderColor = Sweepy.colors.hairline,
            ),
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    body: String,
    onClick: () -> Unit,
    color: Color = Sweepy.colors.ink,
    enabled: Boolean = true,
    trailing: String = "→",
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick, enabled = enabled, pressedScale = 0.99f)
            .padding(vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) color else Sweepy.colors.inkMuted)
            Spacer(Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = Sweepy.colors.inkMuted)
        }
        if (enabled) Text(trailing, style = MaterialTheme.typography.titleMedium, color = Sweepy.colors.inkMuted)
    }
}

@Composable
private fun InfoRow(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Sweepy.colors.ink)
        Spacer(Modifier.height(2.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = Sweepy.colors.inkMuted)
    }
}
