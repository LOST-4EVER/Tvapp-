package com.example.kurdishtv.data

import android.content.Context
import android.content.SharedPreferences

class FavoriteStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kurdish_tv_favorites_v2", Context.MODE_PRIVATE)

    fun getFavoriteIds(): Set<String> {
        return try {
            prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun isFavorite(channelId: String): Boolean {
        return getFavoriteIds().contains(channelId)
    }

    fun toggleFavorite(channelId: String): Boolean {
        val current = getFavoriteIds().toMutableSet()
        val isFavNow = if (current.contains(channelId)) {
            current.remove(channelId)
            false
        } else {
            current.add(channelId)
            true
        }
        try {
            prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        } catch (_: Exception) {}
        return isFavNow
    }

    fun clear() {
        try {
            prefs.edit().remove(KEY_FAVORITES).apply()
        } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_FAVORITES = "fav_channel_ids"
    }
}
