package com.example.kurdishtv.data

import android.content.Context
import android.content.SharedPreferences

class RecentStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kurdish_tv_recents_v2", Context.MODE_PRIVATE)

    fun getRecentChannelIds(): List<String> {
        return try {
            val raw = prefs.getString(KEY_RECENTS, "") ?: ""
            if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecentChannel(channelId: String) {
        try {
            val current = getRecentChannelIds().toMutableList()
            current.remove(channelId)
            current.add(0, channelId)
            val trimmed = current.take(12)
            prefs.edit().putString(KEY_RECENTS, trimmed.joinToString(",")).apply()
        } catch (_: Exception) {}
    }

    fun clearRecents() {
        try {
            prefs.edit().remove(KEY_RECENTS).apply()
        } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_RECENTS = "recent_channel_ids"
    }
}
