package io.github.darkxace01.sweepy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darkxace01.sweepy.data.Appearance
import io.github.darkxace01.sweepy.ui.LocalAppSettings
import io.github.darkxace01.sweepy.ui.SweepyApp
import io.github.darkxace01.sweepy.ui.theme.SweepyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = (application as SweepyApplication).container.prefs
        setContent {
            val settings by prefs.settings.collectAsStateWithLifecycle()
            val dark = when (settings.appearance) {
                Appearance.SYSTEM -> isSystemInDarkTheme()
                Appearance.LIGHT -> false
                Appearance.DARK -> true
            }
            CompositionLocalProvider(LocalAppSettings provides settings) {
                SweepyTheme(darkTheme = dark, theme = settings.theme) {
                    SweepyApp()
                }
            }
        }
    }
}
