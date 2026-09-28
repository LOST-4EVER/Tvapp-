package com.example.kurdishtv.data

import android.content.Context
import com.example.kurdishtv.model.Channel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChannelCacheStorage(context: Context) {
    private val cacheFile = File(context.applicationContext.filesDir, "cached_kurdish_channels.json")

    /**
     * How recently the channel list was last written.
     *
     * Lets a cold start skip the network round-trip entirely when the list is still
     * fresh, which is the common case when the app is reopened during normal viewing.
     *
     * Returns 0 when there is no usable cache *and* when the device clock has
     * moved backwards (timezone change or NTP correction), so the caller cannot
     * tell the two apart. That is safe because freshness is only ever used as an
     * optimisation: the caller still has to read the cache and confirm it is
     * non-empty before trusting it.
     */
    fun getCacheAgeMs(): Long {
        return try {
            if (!cacheFile.exists() || cacheFile.length() == 0L) return 0L
            val age = System.currentTimeMillis() - cacheFile.lastModified()
            // Negative means the clock jumped backwards. Report 0 rather than a
            // negative number so the caller treats the cache as fresh instead of
            // refetching on every single launch.
            if (age < 0L) 0L else age
        } catch (_: Exception) {
            0L
        }
    }

    fun getCachedChannels(): List<Channel>? {
        return try {
            if (!cacheFile.exists()) return null
            val content = cacheFile.readText()
            if (content.isBlank()) return null
            parseChannels(content)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Writes the list to disk.
     *
     * Written to a sibling temp file and then renamed, which is atomic on the same
     * filesystem. `writeText` straight onto the cache truncated it first, so a kill
     * or a full-disk error partway through left a half-written file; the next launch
     * then read that as "no cache" and threw away a perfectly good list. A failed
     * rename leaves the old cache intact, which is the behaviour that matters.
     */
    fun saveChannels(channels: List<Channel>) {
        if (channels.isEmpty()) return
        try {
            val jsonArray = JSONArray()
            for (ch in channels) {
                val obj = JSONObject().apply {
                    put("id", ch.id)
                    put("name", ch.name)
                    put("streamUrl", ch.streamUrl)
                    put("logoUrl", ch.logoUrl ?: "")
                    put("category", ch.category)
                    put("quality", ch.quality)
                    put("isHd", ch.isHd)
                    // Deliberately not persisted. Favourites live in their own
                    // storage and are re-applied on every read; caching them here
                    // let a stale flag resurrect a favourite the user had removed,
                    // because the cache short-circuits the fetch that would
                    // otherwise have corrected it.
                    put("originalId", ch.originalId)
                }
                jsonArray.put(obj)
            }
            val tmp = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
            tmp.writeText(jsonArray.toString())
            if (!tmp.renameTo(cacheFile)) {
                // renameTo will not overwrite on every filesystem; fall back to a
                // delete-then-rename, still leaving the old file intact if the
                // write itself is what failed.
                if (!cacheFile.delete() && cacheFile.exists()) return
                tmp.renameTo(cacheFile)
            }
        } catch (_: Exception) {}
    }

    /**
     * Deletes the on-disk channel cache.
     *
     * `File.delete()` already returns false when it fails, so the result is passed
     * straight through. A cache that is simply absent counts as cleared — there is
     * nothing left to remove, and reporting failure there would be misleading.
     */
    fun clearCache(): Boolean = try {
        !cacheFile.exists() || cacheFile.delete()
    } catch (_: Exception) {
        false
    }

    private fun parseChannels(jsonString: String): List<Channel> {
        val list = mutableListOf<Channel>()
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            val name = obj.optString("name")
            val url = obj.optString("streamUrl")
            if (id.isNotBlank() && url.isNotBlank()) {
                list.add(
                    Channel(
                        id = id,
                        name = name,
                        streamUrl = url,
                        logoUrl = obj.optString("logoUrl").takeIf { it.isNotBlank() },
                        category = obj.optString("category", "General"),
                        quality = obj.optString("quality", "HLS / 720p"),
                        isHd = obj.optBoolean("isHd", false),
                        originalId = obj.optString("originalId").ifBlank { id }
                    )
                )
            }
        }
        return list
    }
}
