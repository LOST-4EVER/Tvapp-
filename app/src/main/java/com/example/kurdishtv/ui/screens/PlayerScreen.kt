package com.example.kurdishtv.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
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
import com.example.kurdishtv.ui.components.ChannelNumberOverlay
import com.example.kurdishtv.ui.components.KurdishTvIcons
import com.example.kurdishtv.ui.components.SleepTimerDialog
import com.example.kurdishtv.ui.components.SvgIcon
import com.example.kurdishtv.ui.keys.RemoteKeyPolicy.acceptsChannelStep
import com.example.kurdishtv.ui.keys.RemoteKeyPolicy.isAutoRepeat
import com.example.kurdishtv.ui.motion.tapOnly
import com.example.kurdishtv.ui.player.PlayerControlsOverlay
import com.example.kurdishtv.ui.player.ResizeMode
import com.example.kurdishtv.ui.player.VideoColorFilter
import com.example.kurdishtv.ui.player.VideoPlayerView
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.kurdishtv.viewmodel.ChannelJump
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
    /**
     * The channel number being typed on the remote, or null when nothing is.
     *
     * The number pad works here as well as on the browse screen, and it is *more*
     * useful here: the viewer is watching something and the fastest way to leave it
     * is to type the next channel's number rather than press Back and hunt for it.
     * One implementation — the view model's, and the overlay's — drives both screens.
     */
    channelJump: ChannelJump? = null,
    onNumericKey: (Int) -> Unit = {},
    onNumericCommit: () -> Unit = {},
    onNumericBackspace: () -> Unit = {},
    onNumericCancel: () -> Unit = {},
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

    // When CH+ or CH- last changed channel, for the held-key rate limit; null until it
    // has. See `RemoteKeyPolicy`; remembered rather than saved, because it describes
    // this visit and a restored timestamp would swallow the first press of the next.
    var lastChannelStepAt by remember { mutableStateOf<Long?>(null) }

    val shouldPlay = isUserPlaying && !isPlaybackPaused
    val playerFocusRequester = remember { FocusRequester() }

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
        // A number being typed is a smaller commitment than leaving the video, so it
        // is what Back gives back first. Without this, one accidental press while
        // typing a channel number threw the viewer out of the stream they were on.
        if (channelJump != null) onNumericCancel() else onBackClick()
    }

    // Auto-hide the controls while playing so the video stays unobstructed.
    LaunchedEffect(isControlsVisible, shouldPlay, autoHideControls) {
        if (autoHideControls && isControlsVisible && shouldPlay) {
            delay(4500L)
            isControlsVisible = false
        }
    }

    // When transport controls fade out, transfer focus to the player Box so remote hardware
    // keys (digits, channel stepping, play/pause) continue to be intercepted reliably.
    LaunchedEffect(isControlsVisible) {
        if (!isControlsVisible) {
            runCatching { playerFocusRequester.requestFocus() }
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
            .focusRequester(playerFocusRequester)
            .focusable()
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
            // Any key press brings them back, and the keys that mean something on a
            // television — the number pad, CHANNEL UP/DOWN, the transport media keys —
            // are then acted on here rather than being woken-and-ignored. That last
            // part was the real gap: a viewer watching live TV with the controls faded
            // out pressed CH+ and nothing happened at all, because the only handlers
            // for those keys lived on the browse screen behind them.
            //
            // Anything not listed returns false, so the controls still receive the
            // event once they are there — this only re-opens the bar, and the auto-hide
            // effect above re-arms and hides it again if the viewer is not watching.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                // A dialog is a separate window with its own key handling, but the
                // number pad is answered here and "a dialog is open" is a more honest
                // guard than relying on window boundaries that differ by platform.
                if (showSleepDialog) return@onKeyEvent false
                if (!isControlsVisible) isControlsVisible = true
                // `repeatCount` is the platform's own auto-repeat counter: 0 on the
                // initial press, growing for as long as the key is held.
                val isRepeat = isAutoRepeat(event.nativeKeyEvent.repeatCount)

                // Stepping by channel, rate-limited while the key is held. See
                // `RemoteKeyPolicy` for why repeats are allowed here and refused
                // everywhere else on this screen. Returns whether the step was taken,
                // so a dropped repeat is still consumed rather than falling through
                // to whatever the controls would have done with it.
                fun stepChannel(delta: Int): Boolean {
                    val now = SystemClock.uptimeMillis()
                    if (isRepeat && !acceptsChannelStep(now, lastChannelStepAt)) return false
                    lastChannelStepAt = now
                    // Never change the channel out from under a number still being
                    // typed: the idle timer would commit it a second and a half later
                    // and land on whatever the viewer had already stepped past.
                    if (channelJump != null) onNumericCancel()
                    errorMessage = null
                    retryToken = 0
                    if (delta > 0) onNextChannel() else onPreviousChannel()
                    return true
                }

                when (event.key) {
                    // Digits build a channel number, so a held key must not build one
                    // the viewer did not type. Auto-repeat arrives as extra Key Down
                    // events, so the Key Up filter above does not cover this.
                    Key.Zero, Key.NumPad0 -> { if (!isRepeat) onNumericKey(0); true }
                    Key.One, Key.NumPad1 -> { if (!isRepeat) onNumericKey(1); true }
                    Key.Two, Key.NumPad2 -> { if (!isRepeat) onNumericKey(2); true }
                    Key.Three, Key.NumPad3 -> { if (!isRepeat) onNumericKey(3); true }
                    Key.Four, Key.NumPad4 -> { if (!isRepeat) onNumericKey(4); true }
                    Key.Five, Key.NumPad5 -> { if (!isRepeat) onNumericKey(5); true }
                    Key.Six, Key.NumPad6 -> { if (!isRepeat) onNumericKey(6); true }
                    Key.Seven, Key.NumPad7 -> { if (!isRepeat) onNumericKey(7); true }
                    Key.Eight, Key.NumPad8 -> { if (!isRepeat) onNumericKey(8); true }
                    Key.Nine, Key.NumPad9 -> { if (!isRepeat) onNumericKey(9); true }

                    // CHANNEL UP/DOWN, and the media keys that mean the same thing on
                    // a remote with a transport row. Every one of these was dead here.
                    Key.ChannelUp, Key.MediaNext -> stepChannel(1)
                    Key.ChannelDown, Key.MediaPrevious -> stepChannel(-1)
                    // Play/pause is the single most-pressed key on a television remote
                    // and it did nothing on this screen: the only way to pause was to
                    // wake the controls and find the button. The directional variants
                    // are honoured as what they say rather than folded into a toggle,
                    // because a remote that has a distinct PLAY key means it.
                    Key.MediaPlayPause -> {
                        if (!isRepeat) isUserPlaying = !isUserPlaying
                        true
                    }
                    Key.MediaPlay -> {
                        if (!isRepeat) isUserPlaying = true
                        true
                    }
                    Key.MediaPause -> {
                        if (!isRepeat) isUserPlaying = false
                        true
                    }

                    // Commit the number early rather than waiting out the idle timer.
                    // Only while one is up, so OK still reaches whatever control has
                    // focus, and only on the first press, so leaning on OK does not
                    // commit a number that is still being typed.
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        if (channelJump != null) {
                            if (!isRepeat) onNumericCommit()
                            true
                        } else {
                            if (!isControlsVisible) {
                                isControlsVisible = true
                                true
                            } else {
                                if (!isRepeat) isUserPlaying = !isUserPlaying
                                true
                            }
                        }
                    }

                    Key.DirectionUp -> {
                        if (!isControlsVisible) {
                            stepChannel(1)
                        } else false
                    }
                    Key.DirectionDown -> {
                        if (!isControlsVisible) {
                            stepChannel(-1)
                        } else false
                    }
                    Key.DirectionLeft -> {
                        if (!isControlsVisible) {
                            stepChannel(-1)
                        } else false
                    }
                    Key.DirectionRight -> {
                        if (!isControlsVisible) {
                            stepChannel(1)
                        } else false
                    }

                    // One press, one digit. A held backspace used to clear the lot.
                    Key.Backspace ->
                        if (channelJump != null) {
                            if (!isRepeat) onNumericBackspace()
                            true
                        } else false
                    // Back clears the number. The `BackHandler` above leaves the
                    // screen, and is reached only because this returns false once
                    // there is nothing left to clear.
                    Key.Back, Key.Escape ->
                        if (channelJump != null) {
                            if (!isRepeat) onNumericCancel()
                            true
                        } else false

                    Key.Menu, Key.Info, Key.Guide -> {
                        if (!isRepeat) isControlsVisible = !isControlsVisible
                        true
                    }

                    else -> false
                }
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
            isMuted = isMuted,
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
            onBackClick = {
                if (channelJump != null) onNumericCancel() else onBackClick()
            },
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

        // The number pad's readout, above the video.
        //
        // The same component the browse screen uses, deliberately: a viewer who has
        // learned where the digits appear on one screen should not have to learn it
        // again on the other, and both are answering the same question with the same
        // number in the same list.
        //
        // Centred horizontally and lifted above the middle, because the two places it
        // could collide are the top bar's channel name and the transport row along the
        // bottom — and digits read against either of those look like part of the
        // player's own furniture rather than something the remote just did.
        channelJump?.let { jump ->
            ChannelNumberOverlay(
                jump = jump,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 96.dp)
            )
        }

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
