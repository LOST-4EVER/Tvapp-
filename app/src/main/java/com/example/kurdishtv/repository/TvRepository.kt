package com.example.kurdishtv.repository

import com.example.kurdishtv.data.CustomPlaylistStorage
import com.example.kurdishtv.data.FavoriteStorage
import com.example.kurdishtv.data.RecentStorage
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.parser.KurdishTvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TvRepository(
    private val favoriteStorage: FavoriteStorage,
    private val recentStorage: RecentStorage,
    private val customPlaylistStorage: CustomPlaylistStorage,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    private val gistChannelsUrl = "https://gist.githubusercontent.com/RekoPlany/58da74c17bd9fdccda3033c77710a7f0/raw/channels.json"
    private val primaryEndpointUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/canal.txt"
    private val secondaryEndpointUrl = "https://iptv-org.github.io/iptv/languages/kur.m3u"

    suspend fun fetchChannels(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        val allChannels = mutableListOf<Channel>()

        coroutineScope {
            // 1. Concurrent Gist JSON fetch (600+ channels)
            val gistDeferred = async {
                fetchUrlContent(gistChannelsUrl)?.let { body ->
                    KurdishTvParser.parseJson(body)
                } ?: emptyList()
            }

            // 2. Concurrent Custom User Playlists fetch
            val customUrls = customPlaylistStorage.getCustomPlaylistUrls()
            val customDeferreds = customUrls.map { customUrl ->
                async {
                    fetchUrlContent(customUrl)?.let { body ->
                        KurdishTvParser.parse(body)
                    } ?: emptyList()
                }
            }

            // 3. Concurrent Primary canal.txt fetch
            val primaryDeferred = async {
                fetchUrlContent(primaryEndpointUrl)?.let { body ->
                    KurdishTvParser.parse(body)
                } ?: emptyList()
            }

            // 4. Concurrent Tertiary iptv-org Kurdish list fetch
            val secondaryDeferred = async {
                fetchUrlContent(secondaryEndpointUrl)?.let { body ->
                    KurdishTvParser.parse(body)
                } ?: emptyList()
            }

            // Await all concurrent network calls
            allChannels.addAll(gistDeferred.await())
            primaryDeferred.await().let { allChannels.addAll(it) }
            secondaryDeferred.await().let { allChannels.addAll(it) }
            for (customDef in customDeferreds) {
                allChannels.addAll(customDef.await())
            }
        }

        // 5. Fallback to default channels if network fails completely
        val finalChannels = if (allChannels.isNotEmpty()) {
            allChannels.distinctBy { it.streamUrl }
        } else {
            KurdishTvParser.getFallbackChannels()
        }

        // Apply persisted user favorites
        val favIds = favoriteStorage.getFavoriteIds()
        val channelsWithFavs = finalChannels.map { channel ->
            channel.copy(isFavorite = favIds.contains(channel.id))
        }

        Result.success(channelsWithFavs)
    }

    private suspend fun fetchUrlContent(url: String): String? {
        return withTimeoutOrNull(6000L) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "KurdishTV-Android/2.0")
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()
                    } else null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun toggleFavorite(channelId: String): Boolean {
        return favoriteStorage.toggleFavorite(channelId)
    }

    fun addRecentChannel(channelId: String) {
        recentStorage.addRecentChannel(channelId)
    }

    fun getRecentChannelIds(): List<String> {
        return recentStorage.getRecentChannelIds()
    }

    fun addCustomPlaylistUrl(url: String): Boolean {
        return customPlaylistStorage.addCustomPlaylistUrl(url)
    }

    fun getCustomPlaylistUrls(): Set<String> {
        return customPlaylistStorage.getCustomPlaylistUrls()
    }

    fun removeCustomPlaylistUrl(url: String): Boolean {
        return customPlaylistStorage.removeCustomPlaylistUrl(url)
    }
}
