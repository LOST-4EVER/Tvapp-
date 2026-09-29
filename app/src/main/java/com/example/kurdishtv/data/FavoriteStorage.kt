package com.example.kurdishtv.data

import android.content.Context
import android.content.SharedPreferences

class FavoriteStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("kurdish_tv_favorites_v2", Context.MODE_PRIVATE)

    /**
     * Serialises the read-modify-write behind a heart.
     *
     * Toggling is read the set, copy it, add or remove one id, write it back.
     * Those are four steps and nothing makes them one. Pressing OK on the remote
     * twice in quick succession — which is what a viewer does when they heart a
     * channel and change their mind — had both toggles read the same set and both
     * write the same result, so the second press was silently lost while the UI
     * reported it had been applied. Held across the whole operation it cannot be.
     *
     * A monitor rather than a coroutine mutex on purpose: the critical section is
     * one preference read and one commit on a background dispatcher, and a monitor
     * is released by the exiting thread whether or not the coroutine that entered
     * it was cancelled.
     */
    private val writeLock = Any()

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
    fun toggleFavorite(channelId: String): Boolean = synchronized(writeLock) {
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
        if (committed) intended else !intended
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
    fun clear(): Boolean = synchronized(writeLock) {
        try {
            prefs.edit().remove(KEY_FAVORITES).commit()
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val KEY_FAVORITES = "fav_channel_ids"
    }
}
