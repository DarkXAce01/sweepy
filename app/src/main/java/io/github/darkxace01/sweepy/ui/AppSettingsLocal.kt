package io.github.darkxace01.sweepy.ui

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.darkxace01.sweepy.data.AppSettings

/** The user's settings, available anywhere in the UI without passing them down by hand. */
val LocalAppSettings = staticCompositionLocalOf { AppSettings() }
