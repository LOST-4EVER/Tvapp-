package com.example.tuner

import android.content.ContentValues
import android.content.Context
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
import com.example.R
import com.example.kurdishtv.data.ChannelCacheStorage
import com.example.kurdishtv.data.CustomPlaylistStorage
import com.example.kurdishtv.data.FavoriteStorage
import com.example.kurdishtv.data.RecentStorage
import com.example.kurdishtv.network.NetworkClient
import com.example.kurdishtv.repository.TvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** What a [TunerChannelSync.sync] call managed to publish. */
data class TunerSyncResult(
    val published: Int,
    val removed: Int,
    val failedReason: String? = null
)

/**
 * The TV provider's channel-table columns.
 *
 * Written out as strings rather than referenced as `TvContract.Channels.COLUMN_*`
 * because the current public SDK no longer exposes several of the presentation
 * columns this needs, and a reference to one that is not there does not compile.
 * The provider's schema is a stable, documented contract and these are its column
 * names; `_ID` is still referenced through `BaseColumns` because that one is
 * genuinely part of the public API.
 */
private object ChannelColumns {
    const val CHANNEL_ID = "channel_id"
    const val INPUT_ID = "input_id"
    const val NAME = "name"
    const val DESCRIPTION = "description"
    const val LOGO = "logo"
    const val HIDDEN = "hidden"

    /**
     * Per-channel data belonging to the TV input service that published it.
     *
     * This is the column the whole design turns on: whatever URI is published here
     * is the URI the system hands back to
     * [android.media.tv.TvInputService.Session.onTune], which is how a channel
     * number finds its way back to a stream URL.
     */
    const val INTERNAL_PROVIDER_DATA = "internal_provider_data"
}

/**
 * Publishes the app's channels into the system Live TV database.
 *
 * ### Where the channel list comes from
 *
 * From [TvRepository], unchanged — the same object the app's own grid is fed by. It
 * already fetches the remote playlists, merges them, dedupes them, caches the result
 * for half an hour and fails soft when a source is down. A tuner that fetched and
 * parsed the same GitHub URLs with its own HTTP call would be a second implementation
 * of all of that, free to drift from the first. The two would then disagree about
 * what a channel is called and which of two feeds is "the" feed, and the system
 * picker would show names the app does not.
 *
 * That is also why this is OkHttp and not Retrofit: OkHttp is what the project
 * already uses, through [NetworkClient], which owns the connection pool, the
 * dispatcher and the HTTP cache. A second HTTP stack would mean a second connection
 * pool and a second cache to keep warm on a device with little memory to spare.
 *
 * ### What is written
 *
 * Presentation plus one URI. The stream URL is deliberately **not** published: the
 * URI published in [ChannelColumns.INTERNAL_PROVIDER_DATA] carries only the channel
 * number, and the session resolves that back to a URL through [TunerChannelStore]
 * when playback actually starts. So a stream URL can change — as community playlist
 * URLs constantly do — without anything in the system database being rewritten.
 */
class TunerChannelSync(
    private val context: Context,
    private val store: TunerChannelStore
) {
    /**
     * One sync at a time.
     *
     * The system can ask for a tune before the first sync has finished, and a
     * refresh in the app can overlap a reconnect. Without this the two would each
     * fetch every playlist and each renumber the list, and whichever finished last
     * would win a race the viewer never sees.
     */
    private val mutex = Mutex()

    suspend fun sync(forceRefresh: Boolean = false): TunerSyncResult =
        mutex.withLock { withContext(Dispatchers.IO) { syncLocked(forceRefresh) } }

    private suspend fun syncLocked(forceRefresh: Boolean): TunerSyncResult {
        val app = context.applicationContext
        val inputId = tunerId(app)
        if (inputId == null) {
            return TunerSyncResult(0, 0, "The system has not given this app a tuner id")
        }

        val channels = try {
            repository(app).fetchChannels(forceRefresh).getOrThrow().take(MAX_PUBLISHED_CHANNELS)
        } catch (e: Exception) {
            // A failed fetch is not a failure to publish. The numbers from the last
            // good sync are still on disk, so the picker keeps working offline,
            // which is the whole reason they are persisted separately from the
            // system rows — the system is free to prune those.
            NetworkClient.logDebug("Tuner channel sync could not reach a source", e)
            return TunerSyncResult(0, 0, e.message ?: "Could not reach the channel sources")
        }

        val tuners = store.reconcile(channels)
        val existing = existingRows(app, inputId)
        val liveNumbers = tuners.mapTo(HashSet()) { it.number }

        var published = 0
        for (tuner in tuners) {
            val values = rowValues(app, inputId, tuner)
            val rowId = existing[tuner.number]
            val ok = if (rowId == null) {
                app.contentResolver.insert(TvContract.Channels.CONTENT_URI, values) != null
            } else {
                app.contentResolver.update(
                    Uri.withAppendedPath(TvContract.Channels.CONTENT_URI, rowId.toString()),
                    values,
                    null,
                    null
                ) > 0
            }
            if (ok) published++
        }

        // Remove only rows this tuner published and no longer has. Rows belonging
        // to other tuners are not in `existing` and are never touched.
        var removed = 0
        for ((number, rowId) in existing) {
            if (number in liveNumbers) continue
            removed += app.contentResolver.delete(
                Uri.withAppendedPath(TvContract.Channels.CONTENT_URI, rowId.toString()),
                null,
                null
            )
        }

        return TunerSyncResult(published = published, removed = removed)
    }

    /**
     * The id the system assigned this app's tuner, or null if it has not.
     *
     * Read from the platform rather than derived from the package name, because the
     * system is what decides it. A hand-made id that merely *looks* right publishes
     * channels the system will never associate with the tuner it asked for, and the
     * symptom is a tuner that appears in the list with nothing behind it.
     */
    private fun tunerId(context: Context): String? = try {
        context.getSystemService(TvInputManager::class.java)
            ?.tunerUuid
            ?.toString()
            ?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        NetworkClient.logDebug("Could not read this app's tuner id", e)
        null
    }

    /**
     * The system rows already published for this tuner, as `channel number -> row id`.
     *
     * Needed to decide between an insert and an update, and because the row id is
     * what an update or a delete needs.
     */
    private fun existingRows(context: Context, inputId: String): Map<Long, Long> {
        val result = HashMap<Long, Long>()
        try {
            context.contentResolver.query(
                TvContract.Channels.CONTENT_URI,
                arrayOf(TvContract.Channels._ID, ChannelColumns.CHANNEL_ID),
                "${ChannelColumns.INPUT_ID} = ?",
                arrayOf(inputId),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(TvContract.Channels._ID)
                val numberIndex = cursor.getColumnIndex(ChannelColumns.CHANNEL_ID)
                if (numberIndex >= 0) {
                    while (cursor.moveToNext()) {
                        result[cursor.getLong(numberIndex)] = cursor.getLong(idIndex)
                    }
                }
            }
        } catch (e: Exception) {
            NetworkClient.logDebug("Could not read the existing tuner channels", e)
        }
        return result
    }

    /**
     * Builds the row for one channel.
     *
     * `hidden` is never written, and neither is the viewer's own channel number:
     * writing whole rows back wholesale would resurrect every channel the viewer
     * had hidden or renumbered in the system UI, on every sync. That is the sort of
     * thing that makes people uninstall a TV app.
     *
     * The logo is the app's own icon rather than the channel's remote artwork. The
     * column is specified for a content or file URI, and a remote `http` URL is
     * resolved by the Live TV app with no guarantee at all — a broken tile in the
     * channel list reads as a fault rather than as a missing image. The per-channel
     * artwork the playlists carry is already on screen in the app's own grid, where
     * it is loaded properly.
     */
    private fun rowValues(
        context: Context,
        inputId: String,
        channel: TunerChannel
    ): ContentValues = ContentValues().apply {
        put(ChannelColumns.CHANNEL_ID, channel.number)
        put(ChannelColumns.INPUT_ID, inputId)
        put(ChannelColumns.NAME, channel.name)
        put(ChannelColumns.DESCRIPTION, channel.category)
        // `ContentValues` has no `Uri` overload — the provider stores both of these
        // as text, and the framework parses them back on the way out.
        put(ChannelColumns.LOGO, appLogoUri(context).toString())
        put(ChannelColumns.INTERNAL_PROVIDER_DATA, channelUri(channel.number).toString())
    }

    private fun appLogoUri(context: Context): Uri = Uri.parse(
        "android.resource://${context.packageName}/${R.mipmap.ic_launcher}"
    )

    /**
     * The same wiring `MainActivity` builds, over the same shared OkHttp client and
     * the same on-disk caches.
     */
    private fun repository(context: Context): TvRepository = TvRepository(
        favoriteStorage = FavoriteStorage(context),
        recentStorage = RecentStorage(context),
        customPlaylistStorage = CustomPlaylistStorage(context),
        channelCacheStorage = ChannelCacheStorage(context),
        okHttpClient = NetworkClient.getOkHttpClient(context)
    )

    companion object {
        /**
         * How many channels are published.
         *
         * The merge runs to a few thousand entries once community playlists are
         * imported, and each one is a row the system has to index, hold and render
         * in a picker that has to stay responsive on a television. The list keeps
         * the source's own order, so this takes the first N — the curated and
         * primary sources, which are the channels a viewer scrolling with a remote
         * would want at the top anyway.
         */
        const val MAX_PUBLISHED_CHANNELS = 800

        private const val CHANNEL_URI_SCHEME = "kurdishtv"

        /** The per-channel URI the system hands back to `Session.onTune`. */
        fun channelUri(number: Long): Uri = Uri.parse("$CHANNEL_URI_SCHEME://channel/$number")

        /** The channel number a tune URI names, or 0 when it names nothing we own. */
        fun channelNumberOf(uri: Uri): Long {
            if (!CHANNEL_URI_SCHEME.equals(uri.scheme, ignoreCase = true)) return 0L
            return uri.lastPathSegment?.toLongOrNull() ?: 0L
        }

        /** The file the tuner's channel numbers live in. */
        fun store(context: Context): TunerChannelStore = TunerChannelStore(
            File(context.applicationContext.filesDir, "tuner_channels.json")
        )
    }
}
