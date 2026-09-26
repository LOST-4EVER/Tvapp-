package com.example.kurdishtv.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.kurdishtv.network.NetworkClient

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    streamUrl: String,
    isPlaying: Boolean,
    resizeMode: ResizeMode,
    onPlaybackError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(true) }

    val exoPlayer = remember(context) {
        val httpDataSourceFactory = NetworkClient.createMediaDataSourceFactory(context)
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(defaultDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15000,
                /* maxBufferMs = */ 50000,
                /* bufferForPlaybackMs = */ 2500,
                /* bufferForPlaybackAfterRebufferMs = */ 5000
            )
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                playWhenReady = true
            }
    }

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNotBlank()) {
            try {
                isBuffering = true
                val mediaItem = MediaItem.fromUri(streamUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = isPlaying
            } catch (e: Exception) {
                isBuffering = false
                onPlaybackError("Failed to prepare channel stream: ${e.message}")
            }
        } else {
            isBuffering = false
            onPlaybackError("Stream URL is empty")
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = when (playbackState) {
                    Player.STATE_BUFFERING -> true
                    Player.STATE_READY -> false
                    Player.STATE_ENDED -> false
                    Player.STATE_IDLE -> false
                    else -> false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                onPlaybackError(error.localizedMessage ?: "Stream playback failed")
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode.mode
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode.mode
                playerView.keepScreenOn = isPlaying
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            PlayerBufferingIndicator()
        }
    }
}
