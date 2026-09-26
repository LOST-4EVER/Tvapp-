package com.example.kurdishtv.repository

import com.example.kurdishtv.data.ChannelCacheStorage
import com.example.kurdishtv.data.CustomPlaylistStorage
import com.example.kurdishtv.data.FavoriteStorage
import com.example.kurdishtv.data.RecentStorage
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.KurdishChannelCatalog
import com.example.kurdishtv.network.NetworkClient.fetchString
import com.example.kurdishtv.parser.KurdishTvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

class TvRepository(
    private val favoriteStorage: FavoriteStorage,
    private val recentStorage: RecentStorage,
    private val customPlaylistStorage: CustomPlaylistStorage,
    private val channelCacheStorage: ChannelCacheStorage,
    private val okHttpClient: OkHttpClient
) {
    private val gistChannelsUrl =
        "https://gist.githubusercontent.com/RekoPlany/58da74c17bd9fdccda3033c77710a7f0/raw/channels.json"
    private val primaryEndpointUrl =
        "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/canal.txt"
    private val secondaryEndpointUrl =
        "https://iptv-org.github.io/iptv/languages/kur.m3u"
    private val fallbackGistUrl =
        "https://gist.githubusercontent.com/nevzatokcu/e0fc485c276b509bcbe452ce798606ff/raw/liste.m3u"

    /**
     * Instantly loads channels from disk cache or catalog with zero network latency.
     */
    fun getInstantInitialChannels(): List<Channel> {
        val cached = channelCacheStorage.getCachedChannels()
        val channels = if (!cached.isNullOrEmpty()) cached else KurdishChannelCatalog.getDefaultChannels()
        val favIds = favoriteStorage.getFavoriteIds()
        return channels.map { it.copy(isFavorite = favIds.contains(it.id)) }
    }

    suspend fun fetchChannels(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        val allChannels = mutableListOf<Channel>()
        // Initialize with default fast catalog
        allChannels.addAll(KurdishChannelCatalog.getDefaultChannels())

        try {
            coroutineScope {
                val gistDeferred = async {
                    fetchUrlContent(gistChannelsUrl)?.let {
                        KurdishTvParser.parseJson(it, "gist")
                    } ?: emptyList()
                }

                val primaryDeferred = async {
                    fetchUrlContent(primaryEndpointUrl)?.let {
                        KurdishTvParser.parse(it, "krd")
                    } ?: emptyList()
                }

                val secondaryDeferred = async {
                    fetchUrlContent(secondaryEndpointUrl)?.let {
                        KurdishTvParser.parse(it, "iptv")
                    } ?: emptyList()
                }

                val fallbackDeferred = async {
                    fetchUrlContent(fallbackGistUrl)?.let {
                        KurdishTvParser.parse(it, "fbk")
                    } ?: emptyList()
                }

                val customUrls = customPlaylistStorage.getCustomPlaylistUrls()
                val customDeferreds = customUrls.mapIndexed { idx, url ->
                    async {
                        fetchUrlContent(url)?.let {
                            KurdishTvParser.parse(it, "usr$idx")
                        } ?: emptyList()
                    }
                }

                allChannels.addAll(gistDeferred.await())
                allChannels.addAll(primaryDeferred.await())
                allChannels.addAll(secondaryDeferred.await())
                allChannels.addAll(fallbackDeferred.await())
                for (customDef in customDeferreds) {
                    allChannels.addAll(customDef.await())
                }
            }
        } catch (_: Exception) {
            // Safe fallback to accumulated channels
        }

        // Deduplicate streams
        val deduplicated = KurdishTvParser.deduplicate(allChannels)

        // Ensure absolute key uniqueness for Lazy Layouts
        val uniqueChannels = mutableListOf<Channel>()
        val seenIds = mutableSetOf<String>()
        for ((idx, ch) in deduplicated.withIndex()) {
            val uniqueId = if (seenIds.add(ch.id)) ch.id else "${ch.id}_$idx"
            seenIds.add(uniqueId)
            uniqueChannels.add(ch.copy(id = uniqueId))
        }

        // Apply favorite states
        val favIds = favoriteStorage.getFavoriteIds()
        val channelsWithFavs = uniqueChannels.map { channel ->
            channel.copy(isFavorite = favIds.contains(channel.id))
        }

        // Persist to disk cache asynchronously
        channelCacheStorage.saveChannels(channelsWithFavs)

        Result.success(channelsWithFavs)
    }

    suspend fun fetchUrlContent(url: String): String? {
        return withTimeoutOrNull(8000L) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "KurdishTV-Android/3.5")
                    .addHeader("Accept", "*/*")
                    .build()
                okHttpClient.fetchString(request)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun toggleFavorite(channelId: String): Boolean = favoriteStorage.toggleFavorite(channelId)

    fun addRecentChannel(channelId: String) = recentStorage.addRecentChannel(channelId)

    fun getRecentChannelIds(): List<String> = recentStorage.getRecentChannelIds()

    fun addCustomPlaylistUrl(url: String): Boolean = customPlaylistStorage.addCustomPlaylistUrl(url)

    fun getCustomPlaylistUrls(): Set<String> = customPlaylistStorage.getCustomPlaylistUrls()

    fun removeCustomPlaylistUrl(url: String): Boolean = customPlaylistStorage.removeCustomPlaylistUrl(url)

    fun clearFavorites() = favoriteStorage.clear()

    fun clearRecents() = recentStorage.clearRecents()

    fun clearCustomPlaylists() = customPlaylistStorage.clear()

    fun clearChannelCache() = channelCacheStorage.clearCache()
}
