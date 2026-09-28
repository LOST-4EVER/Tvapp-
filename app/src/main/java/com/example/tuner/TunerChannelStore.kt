package com.example.tuner

import com.example.kurdishtv.model.Channel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * One channel as the system Live TV app sees it.
 *
 * [number] is the identity handed to `TvContract.Channels.COLUMN_CHANNEL_ID` — a
 * number the *user* would recognise as a channel number, and, more importantly, the
 * only stable key the framework hands back to a session in
 * [android.media.tv.TuneRequest.getChannelId]. It is therefore the join between the
 * system picker and the stream URL, which is why it has to survive refreshes
 * untouched.
 *
 * [sourceId] is the parser's own channel id, which is stable across refreshes by
 * design. It is what "the same channel" is decided by.
 */
data class TunerChannel(
    val number: Long,
    val sourceId: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val category: String
)

/**
 * Remembers which number belongs to which channel, across refreshes and restarts.
 *
 * A tuner cannot simply re-number the list on every sync. The system Live TV app
 * records the viewer's current channel, their favourites, their guide offsets and
 * their playback position against these numbers, so a channel that moved from 340
 * to 12 is a channel that looks deleted, with something else invented in its place.
 * Numbers are therefore assigned once and never reused while the channel lives, and
 * a channel that returns after a refresh dropped it gets its old number back.
 *
 * Persisted rather than held in memory because the system creates and destroys the
 * service around its own schedule, and a number that changed across a restart would
 * be just as confusing as one that changed across a refresh.
 *
 * Reads are served from a cache: [find] is called from `onTune`, which the framework
 * delivers on the main thread, and re-reading and re-parsing a few hundred rows of
 * JSON on the main thread while the viewer waits on a channel to appear is exactly
 * the kind of stall this file is otherwise trying to avoid.
 */
class TunerChannelStore(private val cacheFile: File) {

    /**
     * The last list read or written.
     *
     * A single `@Volatile` field holding the whole immutable list, so there is no
     * window in which a reader can see a new index with the old array or the other
     * way round. A reader that loses a race with a sync simply gets the previous
     * list, which is correct: it is the list the channel numbers in the system
     * database were written from.
     */
    @Volatile
    private var cached: List<TunerChannel>? = null

    /** Every channel we have published and not yet dropped. */
    fun load(): List<TunerChannel> {
        cached?.let { return it }
        val parsed = try {
            if (!cacheFile.exists()) {
                emptyList()
            } else {
                parse(cacheFile.readText())
            }
        } catch (_: Exception) {
            emptyList()
        }
        cached = parsed
        return parsed
    }

    /** The channel behind a number the framework handed us, or null. */
    fun find(number: Long): TunerChannel? = load().firstOrNull { it.number == number }

    /**
     * Assigns a number to every channel, reuses the numbers already held, and
     * forgets channels that have disappeared from the source.
     *
     * Returns the surviving channels in the order the source listed them, which is
     * the order the system picker shows them in. A tuner whose list reshuffles on
     * every refresh is disorienting in a way a grid of cards is not.
     */
    fun reconcile(channels: List<Channel>): List<TunerChannel> {
        val previous = load().associateBy { it.sourceId }
        var nextNumber = previous.values.maxOfOrNull { it.number }?.plus(1)
            ?: FIRST_CHANNEL_NUMBER

        val result = ArrayList<TunerChannel>(channels.size)
        val seen = HashSet<String>(channels.size)

        for (channel in channels) {
            if (channel.streamUrl.isBlank()) continue
            // A duplicate number would put two tiles in the system picker that both
            // resolve to the same stream. The parser dedupes on stream URL, but a
            // playlist the viewer imported is not guaranteed to.
            if (!seen.add(channel.id)) continue

            val existing = previous[channel.id]
            result += TunerChannel(
                number = existing?.number ?: nextNumber++,
                sourceId = channel.id,
                name = channel.name,
                streamUrl = channel.streamUrl,
                logoUrl = channel.logoUrl,
                category = channel.category
            )
        }

        persist(result)
        return result
    }

    private fun parse(json: String): List<TunerChannel> {
        val array = JSONArray(json)
        val result = ArrayList<TunerChannel>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val number = o.optLong("number", 0L)
            val sourceId = o.optString("sourceId")
            val streamUrl = o.optString("streamUrl")
            if (number <= 0L || sourceId.isBlank() || streamUrl.isBlank()) continue
            result += TunerChannel(
                number = number,
                sourceId = sourceId,
                name = o.optString("name"),
                streamUrl = streamUrl,
                logoUrl = o.optString("logoUrl").ifBlank { null },
                category = o.optString("category")
            )
        }
        return result
    }

    /**
     * Writes to a sibling temp file and renames, which is atomic on the same
     * filesystem — the same reason `ChannelCacheStorage` does it. Writing straight
     * onto the file truncates it first, so a kill partway through would leave a
     * half-written list and lose every number in it, which is the one thing this
     * file exists to preserve.
     */
    private fun persist(channels: List<TunerChannel>) {
        val array = JSONArray()
        for (c in channels) {
            array.put(
                JSONObject().apply {
                    put("number", c.number)
                    put("sourceId", c.sourceId)
                    put("name", c.name)
                    put("streamUrl", c.streamUrl)
                    put("logoUrl", c.logoUrl.orEmpty())
                    put("category", c.category)
                }
            )
        }
        try {
            val tmp = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
            tmp.writeText(array.toString())
            if (!tmp.renameTo(cacheFile)) {
                if (!cacheFile.delete() && cacheFile.exists()) return
                tmp.renameTo(cacheFile)
            }
        } catch (_: Exception) {
            // A failed write costs the channel numbers and nothing else. The next
            // successful sync reassigns them and the system rows are rewritten.
            return
        }
        // Only published once the bytes are on disk, so a reader can never be handed
        // a list that a crash would then take away.
        cached = channels
    }

    companion object {
        /**
         * Where numbering starts.
         *
         * High enough that a future LCN-style mapping — where the viewer types the
         * number they know from the satellite box — can be layered on without
         * renumbering everything that came before it.
         */
        const val FIRST_CHANNEL_NUMBER = 1000L
    }
}
