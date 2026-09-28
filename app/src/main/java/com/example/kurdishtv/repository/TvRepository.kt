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
import kotlinx.coroutines.supervisorScope
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
        return channels.map {
            it.copy(isFavorite = favIds.contains(it.id) || favIds.contains(it.originalId))
        }
    }

    /**
     * Fetches and merges every remote source.
     *
     * When [forceRefresh] is false and the on-disk cache is younger than
     * [CACHE_FRESH_MS], the cached list is returned without touching the network. Cold
     * starts then render instantly; explicit refreshes pass `true` to bypass the check.
     */
    suspend fun fetchChannels(forceRefresh: Boolean = true): Result<List<Channel>> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                val age = channelCacheStorage.getCacheAgeMs()
                // 0 is ambiguous: it means "no cache" *or* "the clock moved
                // backwards". It cannot be distinguished here, and it does not
                // need to be — the read below is what actually decides. A fresh
                // age only lets us *try* the cache; an empty or unreadable one
                // falls through to the network exactly as if it had been stale.
                if (age < CACHE_FRESH_MS) {
                    val cached = channelCacheStorage.getCachedChannels()
                    if (!cached.isNullOrEmpty()) {
                        val favIds = favoriteStorage.getFavoriteIds()
                        return@withContext Result.success(
                            cached.map {
                                it.copy(
                                    isFavorite = favIds.contains(it.id) ||
                                        favIds.contains(it.originalId)
                                )
                            }
                        )
                    }
                }
            }

            val allChannels = mutableListOf<Channel>()
            // Initialize with default fast catalog
            allChannels.addAll(KurdishChannelCatalog.getDefaultChannels())

            try {
                // supervisorScope so a single dead or malformed source cannot cancel the
                // others. A plain coroutineScope would let one bad playlist throw away the
                // channels that did load.
                supervisorScope {
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

            // Ensure absolute key uniqueness for Lazy Layouts.
            //
            // The suffix counts collisions rather than using the index, so an id is
            // derived only from the channel itself. Using the list index made the id
            // depend on position, which broke in two ways: any channel that shifted
            // position between refreshes was treated as a different channel, and a
            // favourited channel whose id gained a suffix could never match its
            // stored favourite id again, so the heart silently reset.
            val uniqueChannels = mutableListOf<Channel>()
            val seenIds = mutableSetOf<String>()
            for (ch in deduplicated) {
                var candidate = ch.id
                var suffix = 2
                while (!seenIds.add(candidate)) {
                    candidate = "${ch.id}_$suffix"
                    suffix++
                }
                uniqueChannels.add(ch.copy(id = candidate))
            }

            // Apply favorite states.
            //
            // Matched against the channel's *original* id, not the de-duplicated one.
            // A channel whose id picked up a collision suffix is still the same
            // channel, so a stored favourite must still find it.
            val favIds = favoriteStorage.getFavoriteIds()
            val channelsWithFavs = uniqueChannels.map { channel ->
                val isFav = favIds.contains(channel.id) || favIds.contains(channel.originalId)
                channel.copy(isFavorite = isFav)
            }

            // Persist to disk cache asynchronously
            channelCacheStorage.saveChannels(channelsWithFavs)

            Result.success(channelsWithFavs)
        }

    suspend fun fetchUrlContent(url: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): String? {
        return withTimeoutOrNull(timeoutMs) {
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

    fun clearFavorites(): Boolean = favoriteStorage.clear()

    fun clearRecents(): Boolean = recentStorage.clearRecents()

    fun clearCustomPlaylists(): Boolean = customPlaylistStorage.clear()

    fun clearChannelCache(): Boolean = channelCacheStorage.clearCache()

    private companion object {
        /** Per-source fetch ceiling. A dead source must not stall the whole merge. */
        const val DEFAULT_TIMEOUT_MS = 8_000L

        /**
         * A cached list this young is still considered current, so an automatic load
         * reuses it instead of re-fetching every playlist.
         */
        const val CACHE_FRESH_MS = 30 * 60 * 1000L
    }
}
