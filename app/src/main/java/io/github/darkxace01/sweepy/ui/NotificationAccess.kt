package io.github.darkxace01.sweepy.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.darkxace01.sweepy.reminders.ReminderCheck

/** Whether Sweepy may post notifications, and a way to ask. */
@Stable
class NotificationAccess internal constructor(
    allowed: Boolean,
    private val ask: () -> Unit,
) {
    var allowed by mutableStateOf(allowed)
        internal set

    /**
     * Shows Android's "Allow notifications?" prompt. If Android won't show it any more (the person said
     * no before and the system now remembers), opens Sweepy's notification settings instead.
     */
    fun request() = ask()
}

@Composable
fun rememberNotificationAccess(onResult: (Boolean) -> Unit = {}): NotificationAccess {
    val context = LocalContext.current
    val activity = LocalActivity.current
    lateinit var access: NotificationAccess
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        access.allowed = ReminderCheck.canNotify(context)
        onResult(granted)
    }
    access = remember {
        NotificationAccess(ReminderCheck.canNotify(context)) {
            val prefs = context.getSharedPreferences(ASKED_PREFS, Context.MODE_PRIVATE)
            val askedBefore = prefs.getBoolean(KEY_ASKED, false)
            // Already granted but switched off in Android's settings: only that page can turn it back on.
            val canPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                (!askedBefore || activity?.shouldShow() == true)
            if (canPrompt) {
                prefs.edit { putBoolean(KEY_ASKED, true) }
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
    // The person can flip it in Android's settings while Sweepy is in the background.
    LifecycleResumeEffect(Unit) {
        access.allowed = ReminderCheck.canNotify(context)
        onPauseOrDispose { }
    }
    return access
}

private fun Activity.shouldShow(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

private const val ASKED_PREFS = "sweepy"
private const val KEY_ASKED = "notification_permission_asked"
