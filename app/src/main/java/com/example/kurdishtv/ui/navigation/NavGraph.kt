package com.example.kurdishtv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = modifier
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
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Player.route) {
            val selectedChannel = uiState.selectedChannel
            if (selectedChannel != null) {
                PlayerScreen(
                    channel = selectedChannel,
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
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}
