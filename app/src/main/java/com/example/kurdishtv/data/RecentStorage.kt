package com.example.kurdishtv.data

import android.content.Context
import android.content.SharedPreferences

class RecentStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kurdish_tv_recents", Context.MODE_PRIVATE)

    fun getRecentChannelIds(): List<String> {
        val raw = prefs.getString(KEY_RECENTS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",")
    }

    fun addRecentChannel(channelId: String) {
        val current = getRecentChannelIds().toMutableList()
        current.remove(channelId)
        current.add(0, channelId)
        val trimmed = current.take(15)
        prefs.edit().putString(KEY_RECENTS, trimmed.joinToString(",")).apply()
    }

    fun clearRecents() {
        prefs.edit().remove(KEY_RECENTS).apply()
    }

    companion object {
        private const val KEY_RECENTS = "recent_channel_ids"
    }
}
