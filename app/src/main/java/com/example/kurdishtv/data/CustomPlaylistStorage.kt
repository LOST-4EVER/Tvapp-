package com.example.kurdishtv.data

import android.content.Context
import android.content.SharedPreferences

class CustomPlaylistStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kurdish_tv_custom_playlists", Context.MODE_PRIVATE)

    fun getCustomPlaylistUrls(): Set<String> {
        return prefs.getStringSet(KEY_CUSTOM_URLS, emptySet()) ?: emptySet()
    }

    fun addCustomPlaylistUrl(url: String): Boolean {
        val current = getCustomPlaylistUrls().toMutableSet()
        val added = current.add(url.trim())
        if (added) {
            prefs.edit().putStringSet(KEY_CUSTOM_URLS, current).apply()
        }
        return added
    }

    fun removeCustomPlaylistUrl(url: String): Boolean {
        val current = getCustomPlaylistUrls().toMutableSet()
        val removed = current.remove(url.trim())
        if (removed) {
            prefs.edit().putStringSet(KEY_CUSTOM_URLS, current).apply()
        }
        return removed
    }

    companion object {
        private const val KEY_CUSTOM_URLS = "custom_playlist_urls"
    }
}
