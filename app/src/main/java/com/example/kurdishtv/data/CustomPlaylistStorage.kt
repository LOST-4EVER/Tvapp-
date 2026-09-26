package com.example.kurdishtv.data

import android.content.Context
import android.content.SharedPreferences

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
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://"))) {
            return false
        }
        val current = getCustomPlaylistUrls().toMutableSet()
        val added = current.add(cleanUrl)
        if (added) {
            try {
                prefs.edit().putStringSet(KEY_CUSTOM_URLS, current).apply()
            } catch (_: Exception) {}
        }
        return added
    }

    fun removeCustomPlaylistUrl(url: String): Boolean {
        val cleanUrl = url.trim()
        val current = getCustomPlaylistUrls().toMutableSet()
        val removed = current.remove(cleanUrl)
        if (removed) {
            try {
                prefs.edit().putStringSet(KEY_CUSTOM_URLS, current).apply()
            } catch (_: Exception) {}
        }
        return removed
    }

    companion object {
        private const val KEY_CUSTOM_URLS = "custom_playlist_urls"
    }
}
