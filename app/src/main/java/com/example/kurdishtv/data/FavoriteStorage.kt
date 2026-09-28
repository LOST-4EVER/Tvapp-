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

    /**
     * Flips a favourite and reports the state that is actually on disk.
     *
     * `commit()` rather than `apply()`, and the return value follows the write rather
     * than the intent: if the write did not land, the stored set is untouched, so the
     * honest answer is the inverse of what was attempted. Returning the intent
     * regardless put a heart on a channel the app had not actually saved, and it
     * silently disagreed with every later read.
     */
    fun toggleFavorite(channelId: String): Boolean {
        val current = getFavoriteIds().toMutableSet()
        val intended = if (current.contains(channelId)) {
            current.remove(channelId)
            false
        } else {
            current.add(channelId)
            true
        }
        val committed = try {
            prefs.edit().putStringSet(KEY_FAVORITES, current).commit()
        } catch (_: Exception) {
            false
        }
        return if (committed) intended else !intended
    }

    /**
     * Removes every favourite.
     *
     * Uses `commit()` rather than `apply()` and returns its result. The callers run
     * on a background dispatcher, so the synchronous write costs nothing there — and
     * unlike `apply()` it actually reports whether the preferences reached disk.
     * Without a real signal the UI was telling the user their favourites had been
     * cleared whether or not they had been.
     */
    fun clear(): Boolean = try {
        prefs.edit().remove(KEY_FAVORITES).commit()
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val KEY_FAVORITES = "fav_channel_ids"
    }
}
