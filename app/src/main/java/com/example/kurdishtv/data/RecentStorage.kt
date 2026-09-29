package com.example.kurdishtv.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Recently-watched channel history.
 *
 * Stored as a JSON array rather than a comma-joined string. Channel ids are
 * derived from stream URLs and names, and both can legitimately contain commas
 * (query strings such as `?a=1,2` and names such as "Marjaeyat, Arabic"), so a
 * comma delimiter silently split those ids and made the entries unreachable.
 */
class RecentStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("kurdish_tv_recents_v3", Context.MODE_PRIVATE)

    /**
     * Serialises the read-modify-write that adding an entry is.
     *
     * `addRecentChannel` has to read the current list, move the channel to the
     * front and write the whole thing back. Nothing in a single method call makes
     * that atomic: the write is a separate step from the read, and the viewer can
     * press OK on one channel and then, a moment later, on another. Two of those
     * overlapping used to mean the second one read the list from before the first
     * wrote it, and the channel that was watched in between fell out of the
     * history entirely — silently, because nothing about the write failed.
     *
     * A lock held across the whole read-modify-write closes that window. It is
     * deliberately not a coroutine mutex: this class is called from a background
     * dispatcher, the critical section is one preference read and one write, and
     * a plain monitor cannot be left unlocked by a cancelled coroutine.
     */
    private val writeLock = Any()

    fun getRecentChannelIds(): List<String> {
        return try {
            val raw = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
            if (raw.isBlank()) return emptyList()

            // Migrate the old comma-delimited format once, then rewrite it.
            if (raw.trimStart().startsWith("[")) {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val id = array.optString(i)
                        if (id.isNotBlank()) add(id)
                    }
                }.take(MAX_RECENTS)
            } else {
                val legacy = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                // Rewrite in the JSON format, already truncated. Migrating the
                // untruncated list meant the very next read had to throw entries
                // away again, and the on-disk value never matched what was read.
                val trimmed = legacy.take(MAX_RECENTS)
                if (trimmed.isNotEmpty()) save(trimmed)
                trimmed
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Records a channel as the most recently watched, and returns the new history.
     *
     * Returning the list it just wrote is the point of this signature. The caller
     * — which is every channel selection, and which used to publish the row of
     * "continue watching" cards — read the history straight back afterwards to
     * get the same twelve ids, which meant building a JSON array, writing it,
     * reading it and parsing it again on every single time the viewer pressed OK.
     * The value it was after is already in hand here.
     */
    fun addRecentChannel(channelId: String): List<String> = synchronized(writeLock) {
        if (channelId.isBlank()) return@synchronized getRecentChannelIds()
        try {
            val current = getRecentChannelIds().toMutableList()
            current.remove(channelId)
            current.add(0, channelId)
            val updated = current.take(MAX_RECENTS)
            save(updated)
            updated
        } catch (_: Exception) {
            // The write did not land, so the history on disk is still the
            // truth. Reporting what is actually stored beats reporting what
            // was intended.
            getRecentChannelIds()
        }
    }

    private fun save(ids: List<String>) {
        try {
            val array = JSONArray()
            ids.forEach { array.put(it) }
            prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
        } catch (_: Exception) {}
    }

    /** Removes the watch history. Returns whether the write actually landed. */
    fun clearRecents(): Boolean = synchronized(writeLock) {
        try {
            prefs.edit().remove(KEY_RECENTS).commit()
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val KEY_RECENTS = "recent_channel_ids"
        private const val MAX_RECENTS = 12
    }
}
