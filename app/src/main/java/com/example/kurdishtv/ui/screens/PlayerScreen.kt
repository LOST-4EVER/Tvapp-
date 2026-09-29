package com.example.kurdishtv.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.components.KurdishTvIcons
import com.example.kurdishtv.ui.components.SleepTimerDialog
import com.example.kurdishtv.ui.components.SvgIcon
import com.example.kurdishtv.ui.motion.tapOnly
import com.example.kurdishtv.ui.player.PlayerControlsOverlay
import com.example.kurdishtv.ui.player.ResizeMode
import com.example.kurdishtv.ui.player.VideoColorFilter
import com.example.kurdishtv.ui.player.VideoPlayerView
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    channel: Channel,
    settings: AppSettings,
    autoplay: Boolean,
    autoHideControls: Boolean,
    sleepTimerMinutes: Int,
    sleepTimerFormattedText: String?,
    isPlaybackPaused: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onResizeModeChange: (ResizeMode) -> Unit,
    onColorFilterChange: (VideoColorFilter) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isUserPlaying by remember(channel.id) { mutableStateOf(autoplay) }
    // Bumped to force a re-prepare of the same URL. ExoPlayer moves to STATE_IDLE on
    // a playback error and never retries on its own, so the Retry button used to set
    // isUserPlaying = true and then nothing happened: the player was already "playing"
    // as far as it was concerned, just permanently broken. See VideoPlayerView.reloadKey.
    var retryToken by remember { mutableIntStateOf(0) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(true) }

    // How the video fills the screen, and which correction is on it, live in
    // [AppSettings] now. They used to be `rememberSaveable` here, which survived a
    // rotation and nothing else: a cold start — or returning tomorrow, or opening
    // the small picture-in-browse pane — silently reset both, so the FIT/FILL
    // button had to be pressed again every single time. The choice is about the
    // viewer's screen, not about one stream, so it is a preference like any other.
    //
    // A name that no longer resolves (an app update that renames an entry) falls
    // back to the default rather than throwing.
    val resizeMode = settings.resizeMode
    val colorFilter = settings.videoColorFilter
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

    // Auto-hide the controls while playing so the video stays unobstructed.
    LaunchedEffect(isControlsVisible, shouldPlay, autoHideControls) {
        if (autoHideControls && isControlsVisible && shouldPlay) {
            delay(4500L)
            isControlsVisible = false
        }
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
            .background(Color.Black)
            // The remote was dead while the controls were hidden.
            //
            // Auto-hide takes the transport bar out of the composition, and the bar is
            // the only focusable thing on this screen — the scrim it sits on is a
            // `tapOnly` precisely so that it is *not* a focus target. Once it is gone
            // there is no focus target left here at all, so a direction key had nothing
            // to move to and no effect key had anything to press: a viewer who let the
            // controls fade out had to press Back to leave or change channel blind.
            // On a television, which is the device with no finger to tap the screen
            // and wake them, that is a dead end.
            //
            // Any key press brings them back. It never consumes the event, so the
            // controls still receive it once they are there — this only re-opens the
            // bar, and the auto-hide effect above re-arms and hides it again if the
            // viewer is not actually watching.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (!isControlsVisible) isControlsVisible = true
                false
            }
    ) {
        VideoPlayerView(
            streamUrl = channel.streamUrl,
            isPlaying = shouldPlay,
            resizeMode = resizeMode,
            onPlaybackError = { err -> errorMessage = err },
            colorFilter = colorFilter,
            areControlsVisible = isControlsVisible,
            reloadKey = retryToken,
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
                retryToken = 0
                onNextChannel()
            },
            onPreviousChannel = {
                errorMessage = null
                retryToken = 0
                onPreviousChannel()
            },
            onFavoriteToggle = { onFavoriteToggle(channel.id) },
            onBackClick = onBackClick,
            resizeMode = resizeMode,
            onResizeModeToggle = {
                // Fill -> Zoom -> Fit, cycled from whatever is on now. The choice is
                // written through to [AppSettings], so it survives a cold start and
                // is picked up by the side player pane as well.
                onResizeModeChange(
                    when (resizeMode) {
                        ResizeMode.FILL -> ResizeMode.ZOOM
                        ResizeMode.ZOOM -> ResizeMode.FIT
                        ResizeMode.FIT -> ResizeMode.FILL
                    }
                )
            },
            isFullscreen = isFullscreen,
            onFullscreenToggle = { isFullscreen = !isFullscreen },
            isMuted = isMuted,
            onToggleMute = onToggleMute,
            colorFilter = colorFilter,
            onCycleColorFilter = { onColorFilterChange(colorFilter.next()) },
            modifier = Modifier.fillMaxSize()
        )

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    // `tapOnly`, for the same reason the transport scrim uses it: as a
                    // `clickable` this fills the screen, sits *above* the controls, and
                    // so becomes the largest and first focus target on the player. A
                    // remote user whose stream had just failed pressed OK — to retry —
                    // and the press was swallowed by an invisible scrim that only
                    // revealed the controls, leaving them pressing a second time to do
                    // what they had asked for. It cannot be marked with
                    // `focusProperties { canFocus = false }` either, for the same
                    // reason: that applies to every focus target below it, and the
                    // Retry and Next-channel buttons are below it.
                    .tapOnly { isControlsVisible = true },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = M3ExpressiveShapes.LargeCard,
                    color = colors.surface,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(26.dp)
                    ) {
                        SvgIcon(
                            resId = KurdishTvIcons.Tv,
                            contentDescription = null,
                            tint = colors.liveRed,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Channel unavailable",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The live stream for ${channel.name} is currently offline or unreachable.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    errorMessage = null
                                    isUserPlaying = true
                                    retryToken++
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
                                shape = M3ExpressiveShapes.Pill
                            ) {
                                SvgIcon(
                                    resId = KurdishTvIcons.Refresh,
                                    contentDescription = null,
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    retryToken = 0
                                    onNextChannel()
                                },
                                shape = M3ExpressiveShapes.Pill
                            ) {
                                Text("Next channel", color = colors.textPrimary)
                            }
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
