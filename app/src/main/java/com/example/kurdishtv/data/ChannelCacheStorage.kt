package com.example.kurdishtv.data

import android.content.Context
import com.example.kurdishtv.model.Channel
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.nio.charset.StandardCharsets

class ChannelCacheStorage(context: Context) {
    private val cacheFile = File(context.applicationContext.filesDir, "cached_kurdish_channels.json")

    /**
     * How recently the channel list was last written, in milliseconds.
     *
     * Returns 0 when there is no usable cache at all, and [STALE_AGE_MS] — the
     * largest age there is — when the file's timestamp is *in the future*.
     *
     * That second case used to report 0 as well, on the reasoning that freshness
     * is only an optimisation so a wrong answer is harmless. It is not harmless,
     * because 0 is the youngest age there is: it says "written just now". A
     * device whose clock was set forward by a year and then corrected leaves
     * every cache file stamped in the future, and the app then serves that list
     * as brand new on every launch, forever, without ever asking the network
     * again. The channels on screen are whatever existed when the clock was
     * wrong. Reporting the opposite of stale would at least have failed safe.
     *
     * 0 therefore now means exactly one thing: nothing is cached. Callers still
     * have to read the file and confirm it is non-empty before trusting it.
     */
    fun getCacheAgeMs(): Long {
        return try {
            if (!cacheFile.exists() || cacheFile.length() == 0L) return 0L
            val age = System.currentTimeMillis() - cacheFile.lastModified()
            // Negative means the clock moved backwards relative to the file.
            if (age < 0L) STALE_AGE_MS else age
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
     * How many channels the cache holds, without building any of them.
     *
     * The merge needs this to decide whether the list it just fetched is smaller
     * than what is already on disk. It used to get it from
     * `getCachedChannels()?.size`, which reads the whole file and parses every
     * entry into a [Channel] — a JSON object, several strings and a data class
     * per channel, six hundred of them — and then throws all of it away to answer
     * a question about a single integer. That is the most expensive possible way
     * to ask, and it ran on every refresh, on the same file the cold-start path
     * had often just parsed for real.
     *
     * Counting the array's elements is a single pass over the characters and
     * allocates nothing. It is not a JSON parser and does not pretend to be: it
     * only has to find where one top-level element ends and the next begins,
     * which means tracking string and escape state so that a `}` inside a
     * channel name cannot be mistaken for the end of the object.
     *
     * Returns 0 for a missing, empty or unreadable file, which is the same
     * answer the old expression gave for all three.
     */
    fun getCachedChannelCount(): Int {
        return try {
            if (!cacheFile.exists() || cacheFile.length() == 0L) return 0
            countTopLevelElements(cacheFile.readText())
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Counts the elements of a JSON array without parsing it.
     *
     * Tracks three things: whether the cursor is inside a string, whether the
     * character before a quote is an escape, and how deep inside the array the
     * cursor is. The first `[` opens the array being counted; every bracket after
     * that belongs to an element, so an element is one whose opening bracket
     * takes the depth from 1 to 2.
     *
     * Counting the *closing* bracket of the array instead — which is the obvious
     * thing to reach for, and what this did at first — answers a different
     * question. It reports 1 for every array that has anything in it at all,
     * which would have made the cache look like it held exactly one channel and
     * silently disabled the guard that stops a failed merge from overwriting a
     * good list.
     *
     * Elements are assumed to be objects, which is the only shape this file is
     * ever written in; an array of bare scalars would not be counted.
     *
     * Internal rather than private so the unit tests can pin it against
     * hand-written JSON containing the cases that break a naive scanner — a
     * brace inside a name, an escaped quote, an escaped backslash — without
     * having to go through the file system to reach it.
     */
    internal fun countTopLevelElements(json: String): Int {
        var count = 0
        var depth = 0
        var sawArray = false
        var inString = false
        var escaped = false

        for (c in json) {
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = false
                }
                continue
            }
            if (c == '[' && !sawArray) {
                sawArray = true
                depth = 1
                continue
            }
            when (c) {
                '"' -> inString = true
                '[', '{' -> {
                    // Depth 1 to 2: this bracket opens an element of the array
                    // rather than something nested inside one.
                    if (depth == 1) count++
                    depth++
                }
                ']', '}' -> depth--
            }
        }
        return count
    }

    /**
     * Writes the list to disk.
     *
     * Written to a sibling temp file and then renamed, which is atomic on the same
     * filesystem. `writeText` straight onto the cache truncated it first, so a kill
     * or a full-disk error partway through left a half-written file; the next launch
     * then read that as "no cache" and threw away a perfectly good list.
     *
     * Streamed rather than serialised first. The obvious version builds a
     * [JSONArray], hands it to `toString()` and writes the resulting string: for a
     * merged list that is several hundred [JSONObject]s, a pile of intermediate
     * strings and then one large string that is a second full copy of all of it,
     * all alive at once on a device that may have a 32 MB heap. Writing field by
     * field through a buffered writer keeps one channel in hand at a time and
     * produces the same bytes.
     *
     * [JSONObject.quote] does the escaping, which is the part that has to be
     * exactly right — a channel name from a community playlist can contain a
     * quote, a backslash or a newline, and a hand-rolled escaper would eventually
     * get one of them wrong and leave a file that no longer parses, which reads
     * as "no cache" and throws the whole list away.
     *
     * The old cache is never destroyed to make room for the new one. The
     * delete-then-rename fallback removed the good file and *then* attempted the
     * rename, so a second failure — and rename failing twice is not far-fetched
     * on a full disk — left nothing at all: the exact outcome the atomic write
     * existed to prevent. Copying over the top cannot fail that way, because the
     * old bytes stay readable right up until the new ones land.
     */
    fun saveChannels(channels: List<Channel>) {
        if (channels.isEmpty()) return
        val tmp = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
        try {
            tmp.bufferedWriter(StandardCharsets.UTF_8).use { out ->
                writeChannels(out, channels)
                // Hand the bytes to the kernel before the rename, so a kill
                // immediately afterwards cannot leave a correctly named file that
                // is still empty. This is a flush, not an fsync: it is what makes
                // the rename meaningful for a process death, which is the case
                // that actually happens, rather than for a power cut.
                out.flush()
            }
        } catch (_: Exception) {
            // Nothing was renamed, so the previous cache is still the live one.
            tmp.delete()
            return
        }

        if (tmp.renameTo(cacheFile)) return
        // renameTo will not overwrite on every filesystem. Copy instead of
        // deleting: this writes over the old file in place, so a failure part way
        // through leaves something that no longer parses rather than something
        // that is simply gone — and a file that does not parse is recovered from
        // by the next fetch, whereas a missing one starts from nothing.
        try {
            tmp.copyTo(cacheFile, overwrite = true)
        } catch (_: Exception) {
            // The old cache is still in place and still intact; nothing to do.
        } finally {
            // A successful rename does not leave one of these behind, and a copy
            // that landed does not need it either. An orphaned temp file is a
            // second full copy of the list that nothing would ever clean up.
            tmp.delete()
        }
    }

    /** Serialises [channels] as a JSON array, one field at a time. */
    private fun writeChannels(out: BufferedWriter, channels: List<Channel>) {
        out.writeChar('[')
        for (i in channels.indices) {
            if (i > 0) out.writeChar(',')
            val ch = channels[i]
            out.writeChar('{')
            writeField(out, "id", ch.id, first = true)
            writeField(out, "name", ch.name)
            writeField(out, "streamUrl", ch.streamUrl)
            writeField(out, "logoUrl", ch.logoUrl ?: "")
            writeField(out, "category", ch.category)
            writeField(out, "quality", ch.quality)
            // Written as a real JSON boolean rather than as the string "true", so
            // the file is the same shape it always was.
            out.write(",\"isHd\":")
            out.write(ch.isHd.toString())
            // Deliberately not persisted. Favourites live in their own
            // storage and are re-applied on every read; caching them here
            // let a stale flag resurrect a favourite the user had removed,
            // because the cache short-circuits the fetch that would
            // otherwise have corrected it.
            writeField(out, "originalId", ch.originalId)
            out.writeChar('}')
        }
        out.writeChar(']')
    }

    /**
     * `java.io.Writer` has no `write(Char)`; the single-character overload takes
     * an `Int` code unit. Without this the call is a type error rather than a
     * coercion, and it is a type error at seven call sites rather than one.
     */
    private fun BufferedWriter.writeChar(c: Char) {
        write(c.code)
    }

    private fun writeField(
        out: BufferedWriter,
        name: String,
        value: String,
        first: Boolean = false
    ) {
        if (!first) out.writeChar(',')
        out.writeChar('"')
        out.write(name)
        out.write("\":")
        out.write(JSONObject.quote(value))
    }

    /**
     * Deletes the on-disk channel cache.
     *
     * `File.delete()` already returns false when it fails, so the result is passed
     * straight through. A cache that is simply absent counts as cleared — there is
     * nothing left to remove, and reporting failure there would be misleading.
     */
    fun clearCache(): Boolean = try {
        // The temp file too. It is normally gone by the time this runs, but a
        // write that died partway leaves one behind, and it is just as dead.
        File(cacheFile.parentFile, "${cacheFile.name}.tmp").delete()
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

    private companion object {
        /**
         * The largest age [getCacheAgeMs] can report.
         *
         * Any caller comparing an age against a freshness window treats this as
         * older than the window by construction, which is what "this file cannot
         * be dated" should mean.
         */
        const val STALE_AGE_MS = Long.MAX_VALUE
    }
}
