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

    fun addCustomPlaylistUrl(url: String): Boolean {
        val cleanUrl = normalize(url) ?: return false
        val current = getCustomPlaylistUrls().toMutableSet()
        if (current.size >= MAX_PLAYLISTS) return false
        val added = current.add(cleanUrl)
        if (added) {
            try {
                prefs.edit().putStringSet(KEY_CUSTOM_URLS, current).apply()
            } catch (_: Exception) {}
        }
        return added
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

    fun removeCustomPlaylistUrl(url: String): Boolean {
        val cleanUrl = normalize(url) ?: url.trim()
        val current = getCustomPlaylistUrls().toMutableSet()
        val removed = current.remove(cleanUrl)
        if (removed) {
            try {
                prefs.edit().putStringSet(KEY_CUSTOM_URLS, current).apply()
            } catch (_: Exception) {}
        }
        return removed
    }

    fun clear() {
        try {
            prefs.edit().remove(KEY_CUSTOM_URLS).apply()
        } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_CUSTOM_URLS = "custom_playlist_urls"
        private const val MAX_PLAYLISTS = 20
        private const val MAX_URL_LENGTH = 2048
    }
}
