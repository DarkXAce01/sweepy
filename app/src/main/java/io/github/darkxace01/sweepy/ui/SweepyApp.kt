package io.github.darkxace01.sweepy.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darkxace01.sweepy.data.MediaAccess
import io.github.darkxace01.sweepy.data.MediaPermissions
import io.github.darkxace01.sweepy.ui.deck.DeckScreen
import io.github.darkxace01.sweepy.ui.deck.DeckViewModel
import io.github.darkxace01.sweepy.ui.permission.PermissionScreen
import io.github.darkxace01.sweepy.ui.review.ReviewScreen

/** Top level of the app: permission screen → swipe deck ⇄ review bin. */
@Composable
fun SweepyApp() {
    val context = LocalContext.current
    val viewModel: DeckViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var access by remember { mutableStateOf(MediaPermissions.current(context)) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var showReview by rememberSaveable { mutableStateOf(false) }

    // The user can change permissions in Settings while the app is in the background.
    LifecycleResumeEffect(Unit) {
        access = MediaPermissions.current(context)
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        access = MediaPermissions.current(context)
        permissionDenied = access == MediaAccess.NONE
        reloadKey++ // with partial access the user may have picked different photos
    }

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onTrashResult(confirmed = result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(access, reloadKey) {
        if (access != MediaAccess.NONE) viewModel.load()
    }

    if (access == MediaAccess.NONE) {
        PermissionScreen(
            denied = permissionDenied,
            onGrant = { permissionLauncher.launch(MediaPermissions.requested) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
        )
        return
    }

    if (showReview) {
        BackHandler { showReview = false }
        ReviewScreen(
            state = state,
            onBack = { showReview = false },
            onUnmark = viewModel::unmark,
            onCommit = {
                viewModel.createTrashRequest()?.let { pendingIntent ->
                    trashLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                }
            },
        )
    } else {
        DeckScreen(
            state = state,
            partialAccess = access == MediaAccess.PARTIAL,
            onDecide = viewModel::decide,
            onUndo = viewModel::undo,
            onOpenReview = { showReview = true },
            onRequestFullAccess = { permissionLauncher.launch(MediaPermissions.requested) },
        )
    }
}
