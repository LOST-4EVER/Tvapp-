package com.example.kurdishtv.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.components.KurdishTvIcons
import com.example.kurdishtv.ui.player.PlayerControlsOverlay
import com.example.kurdishtv.ui.player.ResizeMode
import com.example.kurdishtv.ui.player.VideoPlayerView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.KurdishRed
import com.example.ui.theme.KurdishSunGold
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    channel: Channel,
    channelsList: List<Channel>,
    sleepTimerMinutes: Int,
    onSetSleepTimer: (Int) -> Unit,
    onChannelSelect: (Channel) -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isPlaying by remember(channel.id) { mutableStateOf(true) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableStateOf(ResizeMode.FIT) }
    var isFullscreen by remember { mutableStateOf(true) }
    var errorMessage by remember(channel.id) { mutableStateOf<String?>(null) }

    // Fullscreen Immersive Mode System Bars Handler
    DisposableEffect(isFullscreen, activity) {
        val window = activity?.window
        if (window != null) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (isFullscreen) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        onDispose {
            val windowObj = activity?.window
            if (windowObj != null) {
                val insetsController = WindowCompat.getInsetsController(windowObj, windowObj.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Sleep Timer countdown effect
    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60 * 1000L)
            isPlaying = false
            onSetSleepTimer(0)
        }
    }

    BackHandler {
        onBackClick()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ExoPlayer Video Engine
        VideoPlayerView(
            streamUrl = channel.streamUrl,
            isPlaying = isPlaying,
            resizeMode = resizeMode,
            onPlaybackError = { error ->
                errorMessage = error
            },
            modifier = Modifier.fillMaxSize()
        )

        // Player Controls Overlay
        PlayerControlsOverlay(
            isVisible = isControlsVisible,
            onTapOverlay = { isControlsVisible = !isControlsVisible },
            channel = channel,
            channelsList = channelsList,
            isPlaying = isPlaying,
            sleepTimerMinutes = sleepTimerMinutes,
            onSetSleepTimer = onSetSleepTimer,
            onPlayPauseToggle = { isPlaying = !isPlaying },
            onNextChannel = {
                errorMessage = null
                onNextChannel()
            },
            onPreviousChannel = {
                errorMessage = null
                onPreviousChannel()
            },
            onChannelSelect = { selected ->
                errorMessage = null
                onChannelSelect(selected)
            },
            onFavoriteToggle = { onFavoriteToggle(channel.id) },
            onBackClick = onBackClick,
            resizeMode = resizeMode,
            onResizeModeToggle = {
                resizeMode = when (resizeMode) {
                    ResizeMode.FIT -> ResizeMode.FILL
                    ResizeMode.FILL -> ResizeMode.ZOOM
                    ResizeMode.ZOOM -> ResizeMode.FIT
                }
            },
            isFullscreen = isFullscreen,
            onFullscreenToggle = { isFullscreen = !isFullscreen },
            modifier = Modifier.fillMaxSize()
        )

        // Error State Screen (if stream fails to load)
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { isControlsVisible = true },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = KurdishTvIcons.Tv,
                        contentDescription = null,
                        tint = KurdishRed,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to Stream Channel",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The live stream for ${channel.name} is temporarily unavailable or offline.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                errorMessage = null
                                isPlaying = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KurdishSunGold),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = KurdishTvIcons.Refresh,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Retry Stream", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                onNextChannel()
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(text = "Next Channel", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
