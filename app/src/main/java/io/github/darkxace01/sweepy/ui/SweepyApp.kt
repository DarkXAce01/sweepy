package io.github.darkxace01.sweepy.ui

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaAccess
import io.github.darkxace01.sweepy.data.MediaPermissions
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.deck.DeckScreen
import io.github.darkxace01.sweepy.ui.duplicates.DuplicatesScreen
import io.github.darkxace01.sweepy.ui.home.HomeScreen
import io.github.darkxace01.sweepy.ui.permission.PermissionScreen
import io.github.darkxace01.sweepy.ui.review.ReviewScreen
import io.github.darkxace01.sweepy.ui.settings.SettingsScreen
import io.github.darkxace01.sweepy.ui.trash.TrashScreen
import io.github.darkxace01.sweepy.ui.welcome.WelcomeScreen
import kotlinx.coroutines.withTimeoutOrNull

/** The app's screens. Kept in a simple back stack; the last one is shown. */
sealed interface Screen {
    data object Home : Screen
    data class Deck(val source: DeckSource) : Screen
    data object Review : Screen
    data object Trash : Screen
    data object Settings : Screen
    data object Duplicates : Screen
}

@Composable
fun SweepyApp() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()

    var access by remember { mutableStateOf(MediaPermissions.current(context)) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(access, reloadKey) {
        if (access != MediaAccess.NONE) viewModel.start()
    }

    val appSettings = LocalAppSettings.current
    if (!appSettings.themePicked) {
        WelcomeScreen(
            selected = appSettings.theme,
            onSelect = { theme -> viewModel.updateSettings { it.copy(theme = theme) } },
            onContinue = { viewModel.updateSettings { it.copy(themePicked = true) } },
        )
        return
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

    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    // Keeps each screen's saved state (scroll position, expanded lists) while it's under another one.
    val screenStates = rememberSaveableStateHolder()
    var navigatingBack by remember { mutableStateOf(false) }
    fun navigate(screen: Screen) {
        navigatingBack = false
        backStack.add(screen)
    }
    fun back() {
        if (backStack.size > 1) {
            navigatingBack = true
            screenStates.removeState(backStack.removeAt(backStack.lastIndex).toString())
        }
    }
    BackHandler(enabled = backStack.size > 1) { back() }

    // System dialogs (move to trash / restore / delete forever) report back here.
    val trashLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onTrashResult(confirmed = it.resultCode == Activity.RESULT_OK)
    }
    val trashActionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onTrashActionResult(confirmed = it.resultCode == Activity.RESULT_OK)
    }
    fun PendingIntent.launchWith(launcher: ActivityResultLauncher<IntentSenderRequest>) =
        launcher.launch(IntentSenderRequest.Builder(intentSender).build())

    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is UiEvent.KeptAll) {
                // A short undo window, then the decision stands.
                val result = withTimeoutOrNull(KEEP_ALL_UNDO_MILLIS) {
                    snackbar.showSnackbar(
                        message = resources.getQuantityString(R.plurals.snack_kept_all, event.count, formatCount(event.count)),
                        actionLabel = resources.getString(R.string.action_undo),
                        duration = SnackbarDuration.Indefinite,
                    )
                }
                if (result == SnackbarResult.ActionPerformed) viewModel.undoKeepAll(event.at)
                return@collect
            }
            val message = when (event) {
                is UiEvent.Trashed -> resources.getQuantityString(
                    R.plurals.snack_trashed, event.count, formatCount(event.count), formatSize(context, event.bytes),
                )
                is UiEvent.Restored -> resources.getQuantityString(R.plurals.snack_restored, event.count, formatCount(event.count))
                is UiEvent.DeletedForever -> resources.getQuantityString(
                    R.plurals.snack_deleted, event.count, formatCount(event.count), formatSize(context, event.bytes),
                )
                is UiEvent.KeptAll -> return@collect
            }
            // After a cleanup, offer a way to the trash in case something went that shouldn't have.
            val action = if (event is UiEvent.Trashed) resources.getString(R.string.snack_view_trash) else null
            if (snackbar.showSnackbar(message, actionLabel = action) == SnackbarResult.ActionPerformed) navigate(Screen.Trash)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = backStack.last(),
            transitionSpec = {
                val direction = if (navigatingBack) -1 else 1
                (slideInHorizontally(tween(320)) { direction * it / 5 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(320)) { -direction * it / 8 } + fadeOut(tween(160)))
            },
            label = "navigation",
        ) { screen ->
            screenStates.SaveableStateProvider(screen.toString()) {
                when (screen) {
                    Screen.Home -> {
                        val state by viewModel.home.collectAsStateWithLifecycle()
                        HomeScreen(
                            state = state,
                            partialAccess = access == MediaAccess.PARTIAL,
                            onOpenDeck = { navigate(Screen.Deck(it)) },
                            onOpenReview = { navigate(Screen.Review) },
                            onOpenTrash = { navigate(Screen.Trash) },
                            onOpenSettings = { navigate(Screen.Settings) },
                            onOpenDuplicates = { navigate(Screen.Duplicates) },
                            onRequestFullAccess = { permissionLauncher.launch(MediaPermissions.requested) },
                        )
                    }
                    is Screen.Deck -> {
                        val state by viewModel.deck(screen.source).collectAsStateWithLifecycle()
                        val hintSeen by viewModel.swipeHintSeen.collectAsStateWithLifecycle()
                        DeckScreen(
                            source = screen.source,
                            state = state,
                            showHint = !hintSeen,
                            onBack = ::back,
                            onDecide = { item, decision -> viewModel.decide(screen.source, item, decision) },
                            onUndo = { viewModel.undo(screen.source) },
                            onOpenReview = { navigate(Screen.Review) },
                            onDismissHint = viewModel::dismissSwipeHint,
                            onFilterChange = { viewModel.setFilter(screen.source, it) },
                        )
                    }
                    Screen.Review -> {
                        val state by viewModel.review.collectAsStateWithLifecycle()
                        ReviewScreen(
                            state = state,
                            onBack = ::back,
                            onUnmark = viewModel::unmark,
                            onUnmarkAll = { viewModel.unmarkAll(state.groups.flatMap { it.items }) },
                            onCommit = { viewModel.createTrashRequest()?.launchWith(trashLauncher) },
                        )
                    }
                    Screen.Duplicates -> {
                        val state by viewModel.duplicates.collectAsStateWithLifecycle()
                        DuplicatesScreen(
                            state = state,
                            onBack = ::back,
                            onResolve = viewModel::resolveDuplicates,
                            onUndo = viewModel::undoDuplicates,
                        )
                    }
                    Screen.Settings -> {
                        val settings by viewModel.settings.collectAsStateWithLifecycle()
                        val keptCount by viewModel.keptCount.collectAsStateWithLifecycle()
                        SettingsScreen(
                            settings = settings,
                            keptCount = keptCount,
                            onBack = ::back,
                            onChange = viewModel::updateSettings,
                            onForgetKept = viewModel::forgetKept,
                            onForgetEverything = viewModel::forgetEverything,
                        )
                    }
                    Screen.Trash -> {
                        LaunchedEffect(Unit) { viewModel.loadTrash() }
                        val state by viewModel.trash.collectAsStateWithLifecycle()
                        TrashScreen(
                            state = state,
                            onBack = ::back,
                            onRestore = { viewModel.createRestoreRequest(it)?.launchWith(trashActionLauncher) },
                            onDeleteForever = { viewModel.createDeleteForeverRequest(it)?.launchWith(trashActionLauncher) },
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
            )
        }
    }
}

private const val KEEP_ALL_UNDO_MILLIS = 3_500L
