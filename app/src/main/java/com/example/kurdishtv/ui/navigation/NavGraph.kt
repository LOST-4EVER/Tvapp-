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
import com.example.kurdishtv.viewmodel.TvViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Player : Screen("player")
}

@Composable
fun KurdishTvNavGraph(
    viewModel: TvViewModel,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = modifier
    ) {
        composable(Screen.Main.route) {
            MainTvScreen(
                uiState = uiState,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onCategorySelected = { viewModel.onCategorySelected(it) },
                onChannelClick = { channel ->
                    viewModel.onChannelSelected(channel)
                    navController.navigate(Screen.Player.route)
                },
                onFavoriteToggle = { id -> viewModel.onFavoriteToggled(id) },
                onAddCustomPlaylist = { url -> viewModel.addCustomPlaylist(url) },
                onRemoveCustomPlaylist = { url -> viewModel.removeCustomPlaylist(url) },
                onRetryClick = { viewModel.loadChannels() }
            )
        }

        composable(Screen.Player.route) {
            val selectedChannel = uiState.selectedChannel
            if (selectedChannel != null) {
                PlayerScreen(
                    channel = selectedChannel,
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
    }
}
