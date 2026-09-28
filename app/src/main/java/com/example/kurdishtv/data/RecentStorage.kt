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
        context.getSharedPreferences("kurdish_tv_recents_v3", Context.MODE_PRIVATE)

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

    fun addRecentChannel(channelId: String) {
        if (channelId.isBlank()) return
        try {
            val current = getRecentChannelIds().toMutableList()
            current.remove(channelId)
            current.add(0, channelId)
            save(current.take(MAX_RECENTS))
        } catch (_: Exception) {}
    }

    private fun save(ids: List<String>) {
        try {
            val array = JSONArray()
            ids.forEach { array.put(it) }
            prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
        } catch (_: Exception) {}
    }

    /** Removes the watch history. Returns whether the write actually landed. */
    fun clearRecents(): Boolean = try {
        prefs.edit().remove(KEY_RECENTS).commit()
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val KEY_RECENTS = "recent_channel_ids"
        private const val MAX_RECENTS = 12
    }
}
