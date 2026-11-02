package io.github.darkxace01.sweepy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darkxace01.sweepy.data.Appearance
import io.github.darkxace01.sweepy.data.reminders.ReminderKind
import io.github.darkxace01.sweepy.reminders.ReminderLink
import io.github.darkxace01.sweepy.ui.LocalAppSettings
import io.github.darkxace01.sweepy.ui.SweepyApp
import io.github.darkxace01.sweepy.ui.theme.SweepyTheme

class MainActivity : ComponentActivity() {
    private val prefs by lazy { (application as SweepyApplication).container.prefs }

    /** Set when a reminder was tapped: the screen it should open. */
    private var openRequest by mutableStateOf<ReminderKind?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only the first launch carries the request; after a rotation it has already been handled.
        if (savedInstanceState == null) openRequest = ReminderLink.kindOf(intent)
        setContent {
            val settings by prefs.settings.collectAsStateWithLifecycle()
            val dark = when (settings.appearance) {
                Appearance.SYSTEM -> isSystemInDarkTheme()
                Appearance.LIGHT -> false
                Appearance.DARK -> true
            }
            CompositionLocalProvider(LocalAppSettings provides settings) {
                SweepyTheme(darkTheme = dark, theme = settings.theme) {
                    SweepyApp(openRequest = openRequest, onOpenRequestHandled = { openRequest = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ReminderLink.kindOf(intent)?.let { openRequest = it }
    }

    override fun onResume() {
        super.onResume()
        prefs.markOpened()
    }
}
