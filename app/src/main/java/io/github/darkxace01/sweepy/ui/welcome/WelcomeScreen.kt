package io.github.darkxace01.sweepy.ui.welcome

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.ThemeChoice
import io.github.darkxace01.sweepy.ui.components.Eyebrow
import io.github.darkxace01.sweepy.ui.components.InkButton
import io.github.darkxace01.sweepy.ui.components.Mark
import io.github.darkxace01.sweepy.ui.components.MarkIcon
import io.github.darkxace01.sweepy.ui.components.Paper
import io.github.darkxace01.sweepy.ui.components.ThemeSample
import io.github.darkxace01.sweepy.ui.components.chunky
import io.github.darkxace01.sweepy.ui.components.pressable
import io.github.darkxace01.sweepy.ui.components.reveal
import io.github.darkxace01.sweepy.ui.permission.PrintPile
import io.github.darkxace01.sweepy.ui.settings.themeName
import io.github.darkxace01.sweepy.ui.theme.Sweepy

/**
 * The very first screen after installing: pick a look. Tapping a theme switches the whole screen
 * to it straight away, and the prints at the top get marked again in that theme's hand.
 */
@Composable
fun WelcomeScreen(selected: ThemeChoice, onSelect: (ThemeChoice) -> Unit, onContinue: () -> Unit) {
    val dark = isSystemInDarkTheme()
    Paper {
        Column(
            Modifier
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
            Spacer(Modifier.height(12.dp))
            PrintPile(Modifier.align(Alignment.CenterHorizontally).reveal(1), replayKey = selected)
            Spacer(Modifier.height(24.dp))
            Eyebrow(stringResource(R.string.welcome_eyebrow), color = Sweepy.colors.pencil, modifier = Modifier.reveal(2))
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.displaySmall,
                color = Sweepy.colors.ink,
                modifier = Modifier.reveal(2),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                color = Sweepy.colors.inkMuted,
                modifier = Modifier.reveal(3),
            )
            Spacer(Modifier.height(22.dp))
            Column(Modifier.reveal(4)) {
                for (theme in ThemeChoice.entries) {
                    ThemeOption(theme, dark, selected = theme == selected, onClick = { onSelect(theme) })
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            InkButton(
                stringResource(R.string.welcome_continue, themeName(selected)),
                onContinue,
                Modifier.fillMaxWidth().reveal(5),
                trailing = "→",
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.welcome_later),
                style = MaterialTheme.typography.bodySmall,
                color = Sweepy.colors.inkMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun ThemeOption(theme: ThemeChoice, dark: Boolean, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val border by animateColorAsState(if (selected) Sweepy.colors.ink else Sweepy.colors.hairline, label = "border")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .chunky(shape, fill = Sweepy.colors.surface, offset = if (selected) 4.dp else 2.dp)
            .clip(shape)
            .border(if (selected) 1.5.dp else 1.dp, border, shape)
            .pressable(onClick, pressedScale = 0.98f, role = Role.RadioButton)
            .semantics { this.selected = selected }
            .padding(10.dp),
    ) {
        ThemeSample(
            theme,
            dark,
            Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
            markSize = 14.dp,
            padding = 6.dp,
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(themeName(theme), style = MaterialTheme.typography.titleMedium, color = Sweepy.colors.ink)
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(
                    when (theme) {
                        ThemeChoice.CONTACT_SHEET -> R.string.theme_contact_sheet_body
                        ThemeChoice.RISO -> R.string.theme_riso_body
                        ThemeChoice.POP -> R.string.theme_pop_body
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Sweepy.colors.inkMuted,
            )
        }
        if (selected) MarkIcon(Mark.CIRCLE, Sweepy.colors.keep, size = 26.dp, strokeWidth = 0.1f)
        else Spacer(Modifier.size(26.dp))
    }
}
