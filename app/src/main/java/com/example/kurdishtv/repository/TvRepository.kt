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

/**
 * What [TvRepository.getInstantInitialChannels] produced, and where it came from.
 *
 * A named pair rather than a bare list because the caller has to act on the second
 * half: only a list that came from a *fresh* cache makes a follow-up fetch
 * redundant, and inferring that from the list itself is not possible.
 */
data class InitialChannels(
    val channels: List<Channel>,
    val fromFreshCache: Boolean
)

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
     * The list to show before any network call, and where it came from.
     *
     * [fromFreshCache] is the part callers act on. It is true only when the list is
     * the on-disk cache *and* that cache is young enough that a fetch would serve
     * the same bytes back. When it is true, a subsequent non-forced
     * [fetchChannels] is a no-op by construction, so the caller can skip it rather
     * than re-reading the file and re-filtering the list it already has on screen.
     */
    fun getInstantInitialChannels(): InitialChannels {
        val cached = freshCachedChannels()
        val channels = cached ?: KurdishChannelCatalog.getDefaultChannels()
        return InitialChannels(
            channels = applyFavorites(channels),
            fromFreshCache = cached != null
        )
    }

    /**
     * Stamps the stored favourites onto [channels].
     *
     * One definition of what "is this channel a favourite" means, used by both the
     * cold-start path and the post-merge path. The test is deliberately against the
     * channel's *original* id as well as its current one: [Channel.originalId] is
     * the id before duplicate resolution, so a channel whose id picked up a
     * collision suffix is still the same channel to someone who hearted it before
     * the merge, and a lookup on the suffixed id alone silently dropped the heart.
     *
     * Favourites are re-applied on every read rather than cached alongside the
     * channels, which is why the on-disk cache never persists the flag.
     */
    private fun applyFavorites(channels: List<Channel>): List<Channel> {
        val favIds = favoriteStorage.getFavoriteIds()
        if (favIds.isEmpty()) return channels
        return channels.map { channel ->
            val isFav = favIds.contains(channel.id) || favIds.contains(channel.originalId)
            if (channel.isFavorite == isFav) channel else channel.copy(isFavorite = isFav)
        }
    }

    /**
     * The on-disk list, but only while it is still fresh enough to be served without
     * going to the network. Null when there is no cache, when it is empty, or when
     * it is older than [CACHE_FRESH_MS].
     *
     * The single place that decides this. [fetchChannels] used to inline the same
     * age-then-read-then-check-empty sequence, and the cold-start path ran its own
     * looser version beside it — so the two could disagree about whether the cache
     * was usable, and a list could be read and filtered twice in the same launch.
     */
    private fun freshCachedChannels(): List<Channel>? {
        if (channelCacheStorage.getCacheAgeMs() >= CACHE_FRESH_MS) return null
        return channelCacheStorage.getCachedChannels()?.takeIf { it.isNotEmpty() }
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
                // 0 is ambiguous: it means "no cache" *or* "the clock moved
                // backwards". It cannot be distinguished here, and it does not
                // need to be — the read below is what actually decides. A fresh
                // age only lets us *try* the cache; an empty or unreadable one
                // falls through to the network exactly as if it had been stale.
                val cached = freshCachedChannels()
                if (cached != null) {
                    return@withContext Result.success(applyFavorites(cached))
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

            // Apply favorite states. See [applyFavorites] — the merge path and the
            // cold-start path must agree on what a favourite *is*, or the same
            // channel reads as a favourite on one launch and not on the next.
            val channelsWithFavs = applyFavorites(uniqueChannels)

            // Persisted to the on-disk cache, still on `Dispatchers.IO` so the
            // serialise-and-write is off the main thread.
            //
            // Deliberately *not* fired at a detached coroutine: the obvious version
            // of this — `scope.launch { save }` built from the current context — is
            // cancelled the instant `withContext` returns, because the Job in
            // `currentCoroutineContext()` there is the `withContext` block's own.
            // The write has to finish inside this block, or be given a real owner.
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
