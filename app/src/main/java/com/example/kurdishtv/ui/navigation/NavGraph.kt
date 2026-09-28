package com.example.kurdishtv.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kurdishtv.ui.screens.MainTvScreen
import com.example.kurdishtv.ui.screens.PlayerScreen
import com.example.kurdishtv.ui.screens.SettingsScreen
import com.example.kurdishtv.viewmodel.SettingsViewModel
import com.example.kurdishtv.viewmodel.TvViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Player : Screen("player")
    object Settings : Screen("settings")
}

@Composable
fun KurdishTvNavGraph(
    viewModel: TvViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val settingsLoaded by settingsViewModel.loaded.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    // The activity draws edge to edge, so screens must inset themselves out of the
    // status and navigation bars. The player is deliberately excluded: it hides the
    // system bars for fullscreen video and would be letterboxed by the padding.
    val insetModifier = modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)

    // Identity of the destination currently on top of the back stack, handed to the
    // browse screen as the token it keys its focus placement on. `NavBackStackEntry.id`
    // is a UUID string — it was being fallback-initialised with `?: 0` and passed to an
    // `Int` parameter, which does not typecheck.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val backStackEntryId = backStackEntry?.id.orEmpty()

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Main.route) {
            MainTvScreen(
                uiState = uiState,
                settings = settings,
                settingsLoaded = settingsLoaded,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onCategorySelected = { viewModel.onCategorySelected(it) },
                onChannelClick = { channel ->
                    viewModel.onChannelSelected(channel)
                    navController.navigate(Screen.Player.route)
                },
                onFavoriteToggle = { id -> viewModel.onFavoriteToggled(id) },
                onAddCustomPlaylist = { url -> viewModel.addCustomPlaylist(url) },
                onRemoveCustomPlaylist = { url -> viewModel.removeCustomPlaylist(url) },
                onRetryClick = { viewModel.loadChannels() },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                // What the grid needs to take D-pad focus back after the viewer comes
                // back from a video: a grid with nothing focused is a grid the remote
                // cannot drive.
                //
                // This is the id of whichever entry is on top, so it is the browse
                // screen's own id while the browse screen is the visible one — it does
                // not change when the viewer returns, because it is the same entry. It
                // does not have to: navigation-compose takes a destination out of the
                // composition as soon as it is covered, so returning re-runs the
                // grid's effects against the focus it saved on the way out.
                focusToken = backStackEntryId,
                modifier = insetModifier
            )
        }

        composable(Screen.Player.route) {
            val selectedChannel = uiState.selectedChannel
            // The player has nothing to render without a channel (the source was
            // removed or a refresh dropped it). Popping here would be a side
            // effect inside composition, which Compose can and does re-run, so it
            // is deferred to a coroutine keyed on the null-ness of the channel.
            LaunchedEffect(selectedChannel) {
                if (selectedChannel == null) navController.popBackStack()
            }
            if (selectedChannel != null) {
                PlayerScreen(
                    channel = selectedChannel,
                    settings = settings,
                    autoplay = settings.autoplay,
                    autoHideControls = settings.autoHideControls,
                    sleepTimerMinutes = uiState.sleepTimerMinutes,
                    sleepTimerFormattedText = uiState.sleepTimerFormattedText,
                    isPlaybackPaused = uiState.isPlaybackPaused,
                    isMuted = uiState.isMuted,
                    onToggleMute = { viewModel.toggleMute() },
                    onSetSleepTimer = { minutes -> viewModel.setSleepTimer(minutes) },
                    onNextChannel = { viewModel.selectNextChannel() },
                    onPreviousChannel = { viewModel.selectPreviousChannel() },
                    onFavoriteToggle = { id -> viewModel.onFavoriteToggled(id) },
                    onResizeModeChange = { mode ->
                        settingsViewModel.updatePlayerPreferences(resizeMode = mode)
                    },
                    onColorFilterChange = { filter ->
                        settingsViewModel.updatePlayerPreferences(videoColorFilter = filter)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                settings = settings,
                onUpdate = { transform -> settingsViewModel.update(transform) },
                onResetSettings = { settingsViewModel.resetToDefaults() },
                onClearFavorites = { viewModel.clearFavorites() },
                onClearRecents = { viewModel.clearRecents() },
                onClearCustomPlaylists = { viewModel.clearCustomPlaylists() },
                onClearCache = { viewModel.clearChannelCache() },
                message = uiState.actionMessage,
                onMessageShown = { viewModel.clearActionMessage() },
                onBack = { navController.popBackStack() },
                updateState = updateState,
                onCheckForUpdate = { viewModel.checkForUpdate() },
                onDownloadUpdate = { update -> viewModel.downloadUpdate(update) },
                onInstallUpdate = { update, path ->
                    viewModel.requestInstallUpdate(update, path)
                },
                onDismissUpdate = { viewModel.clearUpdateMessage() },
                modifier = insetModifier
            )
        }
    }
}
