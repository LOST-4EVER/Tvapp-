package com.example.tuner

import android.net.Uri
import android.view.Surface
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
 * OEM tuner — discovers and plays live channels. Without it this app is a
 * standalone player: a grid of channels only it can show, so anyone with a
 * satellite box next to their television had to switch inputs to watch Kurdish
 * channels at all.
 *
 * With it, "Kurdish TV Live" appears in the system's list of input sources, its
 * channels appear in the system channel picker, and choosing one starts a session
 * here that plays it through the same ExoPlayer configuration the app's own player
 * uses.
 *
 * ### The shape of the framework
 *
 * The system owns the conversation. It asks for a session, hands that session a
 * **URI** to play, and separately tells it which surface to draw on and what volume
 * to use:
 *
 * 1. `onCreateSession` — the system names the session and tells us which tuner id it
 *    considers us to be.
 * 2. `onTune(uri)` — "can you play this?" The URI is the one this app published in
 *    the channel's own row, so it carries the channel number back to
 *    [TunerChannelStore] and from there to the stream URL.
 * 3. `onSetSurface(surface)` — the surface to render on, replaceable at any time.
 *
 * There is no "what is playing" query, no video-size negotiation and no session-end
 * handshake to drive: the system owns all of that.
 */
@OptIn(UnstableApi::class)
class KurdishTvInputService : TvInputService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store by lazy { TunerChannelSync.store(this) }
    private val sync by lazy { TunerChannelSync(this, store) }
    private val sessions = HashMap<String, TunerSession>()

    /**
     * The tuner id the system assigned us.
     *
     * Learned from the first session the system asks for and remembered across
     * restarts, so the second run onwards can publish before any session exists.
     * The id belongs to the *system* — it is what the TV provider keys our channel
     * rows against, and a hand-made value that merely looks right writes channels
     * the system will never show.
     */
    @Volatile
    private var inputId: String? = null

    override fun onCreate() {
        super.onCreate()
        val known = TunerChannelSync.rememberInputId(this)
        if (known != null) {
            inputId = known
            publish()
        }
        // With no id yet — the first run on this device — publishing waits for
        // `onCreateSession`, which is the first point the system tells us what we
        // are. Rows from a previous install are still on disk, so the picker is
        // never empty in the meantime.
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
     * The system is creating a session, and has told us which tuner we are.
     *
     * The two-argument form is overridden rather than the one-argument form
     * precisely because it carries the input id; the framework's default
     * implementation just forwards to the other, so nothing is lost by handling it
     * here.
     *
     * Everything expensive is left to [TunerSession.onTune]. This is called on the
     * main thread while the channel list is opening, so work done here is work
     * added to a spinner the viewer is already watching.
     */
    override fun onCreateSession(inputId: String, sessionId: String): TvInputService.Session? {
        if (this.inputId == null) {
            this.inputId = inputId
            TunerChannelSync.rememberInputId(this, inputId)
            // The picker may already be on screen waiting for rows, and on a first
            // run there is nothing published until this fires.
            publish()
        }
        val session = TunerSession(this, store, sessionId)
        synchronized(sessions) { sessions[sessionId] = session }
        return session
    }

    /** Fire and forget: the picker does not block on us, and the channel numbers are already on disk. */
    private fun publish() {
        val id = inputId ?: return
        scope.launch { sync.sync(inputIdOverride = id) }
    }
}

/**
 * One playing channel.
 *
 * A session lives from the moment the viewer opens a channel until they leave it,
 * which on a television is minutes to hours, and it owns an [ExoPlayer] for that
 * whole time. The service holds a reference so it can hand the player back when the
 * system tears the service down, which it is free to do at any point.
 */
@OptIn(UnstableApi::class)
private class TunerSession(
    private val service: TvInputService,
    private val store: TunerChannelStore,
    sessionId: String
) : TvInputService.Session(sessionId) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null
    private var surface: Surface? = null
    private var released = false

    /**
     * "Can you play this?" — the system asking, per channel, whether we handle it.
     *
     * Returning false is a legitimate and useful answer: it tells the system to
     * treat the channel as unavailable and move on, rather than handing it a session
     * that then sits on a black rectangle. That happens when the URI names a channel
     * number this app can no longer resolve, which is exactly the case that keeping
     * the numbers and the URLs in one store is meant to prevent.
     */
    override fun onTune(uri: Uri): Boolean {
        if (released) return false
        val channel = store.find(TunerChannelSync.channelNumberOf(uri))
        if (channel == null) {
            NetworkClient.logDebug("Tune for a channel this tuner does not have: $uri")
            return false
        }

        NetworkClient.logDebug("Tuning ${channel.name} on ${channel.number}")
        val exo = obtainPlayer()
        exo.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        exo.prepare()
        exo.play()
        return true
    }

    /**
     * The surface to draw on, or null to stop drawing.
     *
     * Replaced freely — the screen going off, the system taking the surface for
     * something else, picture-in-picture — so this only ever sets what it is given
     * and assumes nothing about how many times it is called. Returning true tells
     * the system the surface was accepted.
     */
    override fun onSetSurface(surface: Surface?): Boolean {
        this.surface = surface
        player?.setVideoSurface(surface)
        return true
    }

    /** The system owns the volume; it is applied and not fought over. */
    override fun onSetStreamVolume(volume: Float) {
        player?.volume = volume.coerceIn(0f, 1f)
    }

    /**
     * There are no captions to turn on.
     *
     * A live IPTV feed carries no caption track this app can decode, so this is a
     * no-op — but the method is abstract, and an unimplemented one will not compile.
     * Answering honestly is better than pretending: the viewer pressing the caption
     * key gets nothing, not a broken overlay.
     */
    override fun onSetCaptionEnabled(enabled: Boolean) {
        if (enabled) NetworkClient.logDebug("Captions requested, but this tuner has none")
    }

    fun release() {
        if (released) return
        released = true
        scope.cancel()
        player?.release()
        player = null
        surface = null
    }

    /**
     * Builds the player on first use, and only once.
     *
     * Configured exactly as the app's own player is — same data source, same buffer
     * window, same audio attributes, same network wake lock — so a channel started
     * from the system picker behaves the way it does in the app. The wake lock
     * matters most here: a live stream that lets the device drop its Wi-Fi radio
     * between segments stops for good, and the only symptom is a frozen picture.
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
            override fun onPlayerError(error: PlaybackException) {
                // URLs on community playlists rot. Stopping hands the connection
                // and the decoder back immediately rather than leaving a failed
                // player holding both; the system decides what to show next.
                NetworkClient.logDebug("Tuner playback failed", error)
                created.stop()
            }
        })

        player = created
        surface?.let { created.setVideoSurface(it) }
        return created
    }
}
