package com.example.kurdishtv.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.kurdishtv.network.NetworkClient

/**
 * How far ahead of the playhead the loader is allowed to read.
 *
 * Fifty seconds of a live stream is a lot of decoded-adjacent memory and, on a
 * metered connection, a lot of data held for a rebuffer that may never come — for a
 * stream that is already live, so anything past a few seconds cannot be played "late"
 * anyway. Thirty seconds is still far more than the fifteen-second floor absorbs
 * before playback starts, which is what actually has to be covered.
 */
private const val MAX_BUFFER_MS = 30000

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    streamUrl: String,
    isPlaying: Boolean,
    resizeMode: ResizeMode,
    onPlaybackError: (String) -> Unit,
    colorFilter: VideoColorFilter = VideoColorFilter.None,
    /**
     * Whether the transport controls are on screen.
     *
     * The buffering notice and the transport row are both centred, so when the
     * controls were visible the "Connecting stream…" pill was drawn straight
     * through the play button. The controls already communicate playback state, so
     * the notice is only needed once they auto-hide.
     */
    areControlsVisible: Boolean = true,
    /**
     * Change this to force a re-prepare of the same [streamUrl].
     *
     * ExoPlayer moves to STATE_IDLE on a playback error and does not retry on
     * its own, so toggling play/pause is not enough to recover a stream that has
     * just come back online. Bumping this re-runs the prepare effect.
     */
    reloadKey: Int = 0,
    /**
     * Defer loading until playback is actually asked for.
     *
     * A prepared-but-paused ExoPlayer is not an idle one. It has opened the
     * connection, and it goes on filling its buffer to [DEFAULT_BUFFER_MS] whether
     * or not a single frame is ever shown. On the browse screen's preview pane — which
     * is paused by design, because autoplaying a live stream with sound on every visit
     * to the grid would be both surprising and ruinous — that meant arriving at the
     * screen downloaded a whole live stream to a black box.
     *
     * The fullscreen player leaves this off: there the viewer *is* watching, and
     * pre-loading is exactly what makes the first frames appear quickly.
     */
    loadOnlyWhenPlaying: Boolean = false,
    /** Whether audio playback is muted. */
    isMuted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(true) }

    // Applied with drawWithContent rather than graphicsLayer: in Compose 1.7
    // GraphicsLayerScope has no colorFilter property, so graphicsLayer cannot
    // carry the matrix. Drawing the content into a saveLayer whose Paint owns
    // the filter applies it to the decoded frame itself, with no extra decode.
    //
    // Returns null for the Normal preset, in which case no layer is created at
    // all and rendering takes the unmodified path.
    val filterPaint = remember(colorFilter) {
        val filter = colorFilter.toColorFilter()
        if (filter == null) {
            null
        } else {
            // Built by hand rather than with `Paint().apply { colorFilter = ... }`:
            // inside apply, the composable's own `colorFilter` parameter shadows
            // Paint.colorFilter, so the assignment targets a val of the wrong type.
            Paint().also { it.colorFilter = filter }
        }
    }

    val exoPlayer = remember(context) {
        val httpDataSourceFactory = NetworkClient.createMediaDataSourceFactory(context)
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

        val mediaSourceFactory = DefaultMediaSourceFactory(defaultDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 6000,
                /* maxBufferMs = */ MAX_BUFFER_MS,
                /* bufferForPlaybackMs = */ 800,
                /* bufferForPlaybackAfterRebufferMs = */ 1500
            )
            .setBackBuffer(0, false)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                // Not `true`. Playback is now driven entirely by the load effect
                // above, and starting out ready to play meant a preview pane began
                // fetching before it had decided it was allowed to.
                playWhenReady = false
            }
    }

    // One effect for the whole load/play decision, keyed on everything that can change
    // it. Split across two effects these two flags could disagree — `isPlaying` turning
    // true would call `play()` on a player that had not been prepared yet, which is a
    // no-op that leaves the stream silently dead until something else happened to
    // re-trigger the load.
    LaunchedEffect(streamUrl, reloadKey, isPlaying, loadOnlyWhenPlaying) {
        if (streamUrl.isBlank()) {
            isBuffering = false
            onPlaybackError("Stream URL is empty")
            return@LaunchedEffect
        }

        if (!isPlaying) {
            exoPlayer.pause()
            if (loadOnlyWhenPlaying) {
                // `stop()` rather than `pause()`: pause leaves the player buffering in
                // the background, and on a live stream there is no position worth
                // keeping, so releasing the connection outright is both correct and the
                // whole point. It is safe to call on a player with no media item.
                exoPlayer.stop()
                isBuffering = false
            }
            return@LaunchedEffect
        }

        try {
            // Already prepared and holding this item — this is a resume, not a first
            // load, so there is nothing to re-fetch and nothing to re-buffer.
            val alreadyLoaded = exoPlayer.playbackState != Player.STATE_IDLE &&
                exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString() == streamUrl
            if (!alreadyLoaded) {
                isBuffering = true
                val liveConfig = MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .setMinPlaybackSpeed(0.98f)
                    .build()
                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setLiveConfiguration(liveConfig)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }
            exoPlayer.play()
        } catch (e: Exception) {
            isBuffering = false
            onPlaybackError("Failed to prepare channel stream: ${e.message}")
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
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
                val view = LayoutInflater.from(ctx).inflate(R.layout.exo_player_texture_view, null) as PlayerView
                view.apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode.mode
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
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
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val paint = filterPaint
                    if (paint == null) {
                        drawContent()
                    } else {
                        drawIntoCanvas { canvas ->
                            canvas.saveLayer(
                                Rect(Offset.Zero, size),
                                paint
                            )
                            drawContent()
                            canvas.restore()
                        }
                    }
                }
        )

        if (isBuffering && !areControlsVisible) {
            PlayerBufferingIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
        }
    }
}
