package com.example.tuner

import android.media.tv.StreamVolumeControl
import android.media.tv.TISessionAdjacency
import android.media.tv.TvInputManager
import android.media.tv.TvInputService
import android.media.tv.TuneRequest
import android.media.tv.VideoSize
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.OptIn
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
import com.example.kurdishtv.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Publishes this app's channels to the Android TV Live TV app and plays them.
 *
 * ### What this is for
 *
 * The Tv Input Framework is how a television's *own* front end — Google TV, or an
 * OEM tuner — discovers and plays live channels. Without it the app is a
 * standalone player: a grid of channels only this app can show. Someone with a
 * satellite box next to their television had to switch inputs to watch Kurdish
 * channels at all.
 *
 * With it, "Kurdish TV Live" appears in the system's tuner list, its channels appear
 * in the system channel picker, and choosing one starts a session here that plays
 * it through the same ExoPlayer configuration the app's own player uses.
 *
 * ### How a tune becomes playback
 *
 * 1. The Live TV app reads our rows from `TvContract.Channels` and starts a session.
 * 2. It sends `onTune` with a `TuneRequest` carrying the channel number we published.
 * 3. That number is resolved through [TunerChannelStore], which is where the stream
 *    URL lives, and handed to an [ExoPlayer].
 * 4. The surface the system provides is attached, and the video size is reported
 *    back so the system can place and scale the surface itself.
 *
 * The system never sees a stream URL, and the app never has to trust it with one.
 */
@OptIn(UnstableApi::class)
class KurdishTvInputService : TvInputService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store by lazy { TunerChannelSync.store(this) }
    private val sync by lazy { TunerChannelSync(this, store) }
    private val sessions = HashMap<Int, TunerSession>()

    override fun onCreate() {
        super.onCreate()
        // The system binds this service when *its* channel list is opened, not when
        // ours is, so this is the first moment we can discover the list has gone
        // stale. Fire and forget: the picker does not block on us, and the channel
        // numbers from the last sync are already on disk for it to show.
        scope.launch { sync.sync() }
    }

    override fun onDestroy() {
        scope.cancel()
        synchronized(sessions) {
            sessions.values.forEach { it.release() }
            sessions.clear()
        }
        super.onDestroy()
    }

    /**
     * The system created a session and will send a tune to it.
     *
     * Everything expensive is deferred to [TunerSession.onTune]. The framework calls
     * this on the main thread while it opens the channel list, so any work here is
     * work added to a spinner the viewer is already looking at.
     */
    override fun onSessionRequested(sessionId: Int): TvInputService.Session {
        val session = TunerSession(this, store, sessionId)
        synchronized(sessions) { sessions[sessionId] = session }
        return session
    }

    override fun onSessionReleased(sessionId: Int) {
        val released = synchronized(sessions) { sessions.remove(sessionId) }
        released?.release()
    }
}

/**
 * One playing channel.
 *
 * A session lives from the moment the viewer opens a channel until they leave it,
 * which on a television is minutes to hours, and it owns an [ExoPlayer] for that
 * whole time. The framework guarantees `onRelease` for every session it granted, so
 * that is the one place teardown belongs.
 */
@OptIn(UnstableApi::class)
private class TunerSession(
    private val service: TvInputService,
    private val store: TunerChannelStore,
    sessionId: Int
) : TvInputService.Session(sessionId) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null
    private var surfaceCallback: SurfaceHolder.Callback? = null
    private var surface: Surface? = null
    private var ended = false

    /**
     * The viewer opened a channel.
     *
     * `request.channelId` is the `COLUMN_CHANNEL_ID` this app published — the number
     * [TunerChannelStore] assigned. The rest of the request is meaningful for a
     * hardware tuner and not for a list of URLs: a program id describes a moment in
     * a broadcast schedule, a stream id describes an input, and the adjacency hint
     * describes what the *hardware* can play next. None of it maps onto "here is a
     * URL", so only the channel number is used, and the rest is ignored on purpose.
     */
    override fun onTune(
        request: TuneRequest,
        adjacency: TISessionAdjacency,
        streamId: String?,
        params: Bundle?
    ) {
        val channel = store.find(request.channelId)
        if (channel == null) {
            // Either the system is holding a channel from a previous install, or it
            // tuned before the first sync landed. Neither is worth holding a session
            // open for: ending it tells the Live TV app to fall back, which it does
            // by returning to the picker.
            NetworkClient.logDebug("Tune for unknown channel ${request.channelId}")
            endSession(TvInputManager.SESSION_END_REASON_INTERNAL_ERROR)
            return
        }

        NetworkClient.logDebug("Tuning ${channel.name} on ${channel.number}")
        val exo = obtainPlayer()
        exo.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        exo.prepare()
        exo.play()
    }

    /**
     * The surface the system wants video drawn on.
     *
     * Called on every attach, including re-attach after the screen is switched off
     * and on again, so it is written to be idempotent rather than to fire once.
     */
    override fun onSetVideoSurface(surface: Surface?) {
        this.surface = surface
        player?.setVideoSurface(surface)

        // The callback is how the client learns whether frames are actually being
        // produced. Without the created/destroyed pair it can be left holding a
        // surface nothing draws to, which on a television shows as a frozen frame
        // rather than as an error.
        val callback = surfaceCallback
        if (surface != null) {
            callback?.surfaceCreated(surface)
        } else {
            callback?.surfaceDestroyed()
        }
    }

    override fun onSetVideoSurfaceCallback(callback: SurfaceHolder.Callback?) {
        surfaceCallback = callback
    }

    /**
     * The client is telling us the volume.
     *
     * Echoed straight back. A session that ignores this leaves the client's
     * `StreamVolumeControl` callback unfired, and the Live TV app is waiting on it —
     * so a session that never answers is a session that never reports ready.
     */
    override fun onSetStreamVolume(control: StreamVolumeControl) {
        scope.launch {
            runCatching { control.setStreamVolume(control.max, control.current) }
                .onFailure { NetworkClient.logDebug("Could not apply the system volume", it) }
        }
    }

    override fun onRelease() {
        if (ended) return
        ended = true
        scope.cancel()
        // Detach the surface before the player, so the decoder is never asked to
        // render into a surface the client has already torn down.
        surfaceCallback?.surfaceDestroyed()
        surfaceCallback = null
        surface = null
        player?.release()
        player = null
    }

    /**
     * Builds the player on first use, and only once.
     *
     * Configured exactly as the app's own player is — same data source, same buffer
     * window, same audio-focus behaviour, same network wake lock — so a channel
     * started from the system picker behaves the way it does in the app. The wake
     * lock matters most here: a live stream that lets the device drop its Wi-Fi
     * radio between segments stops for good, and the only symptom is a frozen
     * picture.
     */
    private fun obtainPlayer(): ExoPlayer {
        player?.let { return it }

        val context = service.applicationContext
        val dataSource = DefaultDataSource.Factory(
            context,
            NetworkClient.createMediaDataSourceFactory(context)
        )
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15000,
                /* maxBufferMs = */ 30000,
                /* bufferForPlaybackMs = */ 2500,
                /* bufferForPlaybackAfterRebufferMs = */ 5000
            )
            .build()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val created = ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory(dataSource)
            )
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        created.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                reportVideoSize(videoSize.width, videoSize.height)
            }

            override fun onPlayerError(error: PlaybackException) {
                // URLs on community playlists rot. Ending the session is how the
                // Live TV app is told to move on to the next channel, rather than
                // sitting on a black rectangle.
                NetworkClient.logDebug("Tuner playback failed", error)
                endSession(TvInputManager.SESSION_END_REASON_INTERNAL_ERROR)
            }
        })

        player = created
        surface?.let { created.setVideoSurface(it) }
        return created
    }

    /**
     * Tells the system how large the video is, so it can place and scale the
     * surface instead of guessing a full-screen one.
     *
     * The method only exists from Android 8, and below that the system falls back
     * to a full-screen surface, which is the right answer for a live feed anyway.
     */
    private fun reportVideoSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0 || ended) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        @Suppress("DEPRECATION")
        notifyVideoSizeChanged(VideoSize(width, height))
    }

    /**
     * Ends the session, announcing first that it is allowed to.
     *
     * From Android 10 a session has to *tell* the system it can end before it
     * ends, which is how the system gets a window in which to stop whatever is
     * drawing the surface. Skipping the announcement means the end is not honoured
     * and the session lingers.
     */
    private fun endSession(reason: Int) {
        if (ended) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notifySessionEndAvailable()
        }
        notifySessionEnd(reason)
        // The player still holds a decoder and a socket. `onRelease` will follow,
        // but the framework does not promise it happens before the next tune.
        player?.stop()
        ended = true
    }
}
