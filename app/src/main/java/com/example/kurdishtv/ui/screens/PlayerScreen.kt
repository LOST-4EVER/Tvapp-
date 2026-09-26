package com.example.kurdishtv.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.kurdishtv.ui.components.SleepTimerDialog
import com.example.kurdishtv.ui.components.SvgIcon
import com.example.kurdishtv.ui.player.PlayerControlsOverlay
import com.example.kurdishtv.ui.player.ResizeMode
import com.example.kurdishtv.ui.player.VideoPlayerView
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.KurdishRed
import com.example.ui.theme.KurdishSunGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlayerScreen(
    channel: Channel,
    sleepTimerMinutes: Int,
    sleepTimerFormattedText: String?,
    isPlaybackPaused: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isUserPlaying by remember(channel.id) { mutableStateOf(true) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableStateOf(ResizeMode.FIT) }
    var isFullscreen by remember { mutableStateOf(true) }
    var errorMessage by remember(channel.id) { mutableStateOf<String?>(null) }
    var showSleepDialog by remember { mutableStateOf(false) }

    val shouldPlay = isUserPlaying && !isPlaybackPaused

    DisposableEffect(isFullscreen, activity) {
        val window = activity?.window
        if (window != null) {
            try {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                if (isFullscreen) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            } catch (_: Exception) {}
        }
        onDispose {
            val win = activity?.window
            if (win != null) {
                try {
                    val controller = WindowCompat.getInsetsController(win, win.decorView)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                } catch (_: Exception) {}
            }
        }
    }

    BackHandler {
        onBackClick()
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            currentMinutes = sleepTimerMinutes,
            onSelectMinutes = onSetSleepTimer,
            onDismiss = { showSleepDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        VideoPlayerView(
            streamUrl = channel.streamUrl,
            isPlaying = shouldPlay,
            resizeMode = resizeMode,
            onPlaybackError = { err -> errorMessage = err },
            modifier = Modifier.fillMaxSize()
        )

        PlayerControlsOverlay(
            isVisible = isControlsVisible,
            onTapOverlay = { isControlsVisible = !isControlsVisible },
            channel = channel,
            isPlaying = shouldPlay,
            sleepTimerRemainingText = sleepTimerFormattedText,
            onOpenSleepTimer = { showSleepDialog = true },
            onPlayPauseToggle = { isUserPlaying = !isUserPlaying },
            onNextChannel = {
                errorMessage = null
                onNextChannel()
            },
            onPreviousChannel = {
                errorMessage = null
                onPreviousChannel()
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
            isMuted = isMuted,
            onToggleMute = onToggleMute,
            modifier = Modifier.fillMaxSize()
        )

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.93f))
                    .clickable { isControlsVisible = true },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(28.dp)
                ) {
                    SvgIcon(
                        resId = KurdishTvIcons.Tv,
                        contentDescription = null,
                        tint = KurdishRed,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Channel Stream Unavailable",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The live stream for ${channel.name} is currently offline or unreachable.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                errorMessage = null
                                isUserPlaying = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KurdishSunGold),
                            shape = M3ExpressiveShapes.Pill
                        ) {
                            SvgIcon(
                                resId = KurdishTvIcons.Refresh,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                onNextChannel()
                            },
                            shape = M3ExpressiveShapes.Pill
                        ) {
                            Text("Next Channel", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
