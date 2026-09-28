package com.example.kurdishtv.data

import android.content.Context
import android.content.SharedPreferences
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class CustomPlaylistStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kurdish_tv_custom_playlists_v2", Context.MODE_PRIVATE)

    fun getCustomPlaylistUrls(): Set<String> {
        return try {
            prefs.getStringSet(KEY_CUSTOM_URLS, emptySet())?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Adds a playlist link and reports whether it is now stored.
     *
     * `commit()` rather than `apply()`, and the result follows the write: a link that
     * failed to save must not be reported as added, or the UI shows a playlist the
     * next launch will never fetch.
     */
    fun addCustomPlaylistUrl(url: String): Boolean {
        val cleanUrl = normalize(url) ?: return false
        val current = getCustomPlaylistUrls().toMutableSet()
        if (current.size >= MAX_PLAYLISTS) return false
        if (!current.add(cleanUrl)) return false
        return try {
            prefs.edit().putStringSet(KEY_CUSTOM_URLS, current).commit()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Validates and canonicalizes a user-supplied playlist link.
     *
     * Rejects anything that is not plain http(s), anything with embedded credentials
     * (they would leak into logs and the on-disk channel cache), and over-long input.
     * Returns the normalized URL, or null when the value is unusable.
     */
    private fun normalize(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_URL_LENGTH) return null
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
        val parsed = trimmed.toHttpUrlOrNull() ?: return null
        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) return null
        if (parsed.host.isBlank()) return null
        return parsed.toString()
    }

    /** Removes a playlist link, reporting whether it is no longer stored. */
    fun removeCustomPlaylistUrl(url: String): Boolean {
        val cleanUrl = normalize(url) ?: url.trim()
        val current = getCustomPlaylistUrls().toMutableSet()
        if (!current.remove(cleanUrl)) return false
        return try {
            prefs.edit().putStringSet(KEY_CUSTOM_URLS, current).commit()
        } catch (_: Exception) {
            false
        }
    }

    /** Removes every custom playlist. Returns whether the write actually landed. */
    fun clear(): Boolean = try {
        prefs.edit().remove(KEY_CUSTOM_URLS).commit()
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val KEY_CUSTOM_URLS = "custom_playlist_urls"
        private const val MAX_PLAYLISTS = 20
        private const val MAX_URL_LENGTH = 2048
    }
}
