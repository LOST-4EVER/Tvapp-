package com.example.tuner

import android.content.ContentResolver
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
 * Publishes the app's channels into the system Live TV database.
 *
 * ### Where the channel list comes from
 *
 * From [TvRepository], unchanged — the same object the app's own grid is fed by. It
 * already fetches the remote playlists this app depends on, merges them, dedupes
 * them and caches the result for half an hour, and it already fails soft when a
 * source is down. A tuner that fetched and parsed the same GitHub URLs with its own
 * OkHttp call would be a second implementation of the merge, the dedupe, the retry
 * and the cache, free to drift from the first one. In particular the two would
 * disagree about what a channel is called and which of two feeds is "the" feed,
 * and the system picker would show names the app does not.
 *
 * That is also why this is OkHttp and not Retrofit: OkHttp is what the project
 * already uses, through [NetworkClient], which owns the connection pool, the
 * dispatcher and the HTTP cache. Adding a second HTTP stack to save a few lines of
 * `suspend fun` would mean a second connection pool and a second cache to keep
 * warm on a device with little memory to spare.
 *
 * ### What is written
 *
 * Presentation only — name, number, category, artwork. The stream URL is
 * deliberately *not* published. The system Live TV app has no business holding a
 * URL it will never use, and the session resolves the number back to a URL through
 * [TunerChannelStore] when playback actually starts. That also means a stream URL
 * can change — which they do, constantly, on community playlists — without
 * anything in the system database needing to be rewritten.
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
        val inputId = inputId(app) ?: return TunerSyncResult(0, 0, "No TvInputManager")

        val channels = try {
            repository(app)
                .fetchChannels(forceRefresh)
                .getOrThrow()
                .take(MAX_PUBLISHED_CHANNELS)
        } catch (e: Exception) {
            // A failed fetch is not a failure to publish. The numbers from the last
            // good sync are still on disk, so the picker keeps working offline —
            // which is the whole point of persisting them separately from the
            // system rows, which the system is free to prune.
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

        // Only remove what we actually published as ours and no longer have. Rows
        // belonging to other tuners are not in `existing` and are never touched.
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
     * The system-assigned identity of this app's tuner.
     *
     * Read from the platform rather than derived from the package name, because the
     * system is what decides it, and a hand-rolled id that merely *looks* right
     * writes channels the system will never associate with the tuner it asked for.
     */
    private fun inputId(context: Context): String? = try {
        context.getSystemService(TvInputManager::class.java)?.tunerUuid?.toString()
    } catch (e: Exception) {
        NetworkClient.logDebug("Could not read the tuner id", e)
        null
    }

    /**
     * The system rows already published for this tuner, as `channel number -> row id`.
     *
     * Needed because inserting the same channel twice is not free of consequence:
     * the provider is keyed on the pair, but the app still has to decide between an
     * insert and an update, and the row id is what an update or a delete needs.
     */
    private fun existingRows(context: Context, inputId: String): Map<Long, Long> {
        val result = HashMap<Long, Long>()
        try {
            context.contentResolver.query(
                TvContract.Channels.CONTENT_URI,
                arrayOf(TvContract.Channels._ID, TvContract.Channels.COLUMN_CHANNEL_ID),
                "${TvContract.Channels.COLUMN_INPUT_ID} = ?",
                arrayOf(inputId),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(TvContract.Channels._ID)
                val numberIndex =
                    cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_CHANNEL_ID)
                while (cursor.moveToNext()) {
                    result[cursor.getLong(numberIndex)] = cursor.getLong(idIndex)
                }
            }
        } catch (e: Exception) {
            NetworkClient.logDebug("Could not read the existing tuner channels", e)
        }
        return result
    }

    /**
     * Builds the presentation row for one channel.
     *
     * Two columns are deliberately left alone on every update:
     *
     *  - `COLUMN_HIDDEN`, which is the viewer's own decision to hide a channel from
     *    the picker. Writing the whole row back wholesale would resurrect every
     *    channel the viewer had hidden, on every sync, which is the sort of thing
     *    that makes people uninstall a TV app.
     *  - `COLUMN_NUMBER`, for the same reason: the number the viewer assigned is
     *    theirs, not ours.
     *
     * `COLUMN_LOGO` is the app's own icon rather than the channel's remote artwork.
     * The column is specified as a content or file URI; a remote `http` URL is
     * resolved by the Live TV app with no guarantee at all, and a broken tile in the
     * channel list looks like a fault. The per-channel artwork the playlists carry
     * is already on screen in the app's own grid, where it is loaded properly.
     */
    private fun rowValues(
        context: Context,
        inputId: String,
        channel: TunerChannel
    ): ContentValues = ContentValues().apply {
        put(TvContract.Channels.COLUMN_CHANNEL_ID, channel.number)
        put(TvContract.Channels.COLUMN_INPUT_ID, inputId)
        put(TvContract.Channels.COLUMN_NAME, channel.name)
        put(TvContract.Channels.COLUMN_DESCRIPTION, channel.category)
        put(TvContract.Channels.COLUMN_LOGO, appLogoUri(context))
        // Per-channel custom data, the documented slot for it. Not how the session
        // resolves a stream — that goes through the channel number — but it round
        // trips in `TuneRequest.getAdapterData()` on some framework paths, so it is
        // a free second way back to the same answer.
        put(
            TvContract.Channels.COLUMN_INTERNAL_PRESENTATION_DATA,
            Uri.parse("$PRESENTATION_SCHEME://channel/${channel.number}")
        )
    }

    private fun appLogoUri(context: Context): Uri = Uri.parse(
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.mipmap.ic_launcher}"
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
         * How many channels are published to the system.
         *
         * The merge can run to a few thousand entries once community playlists are
         * imported, and every one of them is a row the system has to index, hold and
         * render in a picker that has to stay responsive on a television. The list
         * keeps the source's own order, so this takes the first N — the curated and
         * primary sources, which are the channels a viewer would actually want at
         * the top of a list they scroll with a remote.
         */
        const val MAX_PUBLISHED_CHANNELS = 800

        private const val PRESENTATION_SCHEME = "kurdishtv"

        /** The file the tuner's channel numbers live in. */
        fun store(context: Context): TunerChannelStore = TunerChannelStore(
            File(context.applicationContext.filesDir, "tuner_channels.json")
        )
    }
}
