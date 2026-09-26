package com.example.kurdishtv.data

import android.content.Context
import com.example.kurdishtv.model.Channel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChannelCacheStorage(context: Context) {
    private val cacheFile = File(context.applicationContext.filesDir, "cached_kurdish_channels.json")

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

    fun saveChannels(channels: List<Channel>) {
        if (channels.isEmpty()) return
        try {
            val jsonArray = JSONArray()
            for (ch in channels) {
                val obj = JSONObject().apply {
                    put("id", ch.id)
                    put("name", ch.name)
                    put("streamUrl", ch.streamUrl)
                    put("logoUrl", ch.logoUrl ?: "")
                    put("category", ch.category)
                    put("quality", ch.quality)
                    put("isHd", ch.isHd)
                    put("isFavorite", ch.isFavorite)
                }
                jsonArray.put(obj)
            }
            cacheFile.writeText(jsonArray.toString())
        } catch (_: Exception) {}
    }

    fun clearCache() {
        try {
            if (cacheFile.exists()) cacheFile.delete()
        } catch (_: Exception) {}
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
                        isFavorite = obj.optBoolean("isFavorite", false)
                    )
                )
            }
        }
        return list
    }
}
