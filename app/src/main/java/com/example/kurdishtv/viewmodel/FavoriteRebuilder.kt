package com.example.kurdishtv.viewmodel

import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.ChannelFilterEngine

/**
 * The four lists a heart tap changes, derived together so they cannot disagree.
 */
internal data class FavoriteUpdate(
    val channels: List<Channel>,
    val filteredChannels: List<Channel>,
    val selectedChannel: Channel?,
    val recentChannels: List<Channel>
)

internal object FavoriteRebuilder {

    /**
     * Applies one favourite flag across every list that holds a copy of a channel.
     *
     * Only the Favourites tab actually changes membership when a heart is tapped.
     * In the other eleven categories the channel is in the list before and after,
     * and what changed about it is one boolean — so the same `Channel` instance
     * with the flag flipped is substituted into the existing filtered list instead
     * of re-deriving the list from several hundred channels.
     */
    fun rebuildForFavorite(
        state: TvUiState,
        channelId: String,
        isFav: Boolean
    ): FavoriteUpdate {
        val updatedChannels = state.channels.map {
            if (it.id == channelId) it.copy(isFavorite = isFav) else it
        }
        val updatedSelected = if (state.selectedChannel?.id == channelId) {
            state.selectedChannel.copy(isFavorite = isFav)
        } else state.selectedChannel
        val updatedRecents = state.recentChannels.map {
            if (it.id == channelId) it.copy(isFavorite = isFav) else it
        }
        val filtered = if (state.selectedCategory == CategoryFilter.FAVORITES) {
            ChannelFilterEngine.filter(
                updatedChannels,
                state.selectedCategory,
                state.searchQuery
            )
        } else {
            state.filteredChannels.map {
                if (it.id == channelId) it.copy(isFavorite = isFav) else it
            }
        }
        return FavoriteUpdate(
            channels = updatedChannels,
            filteredChannels = filtered,
            selectedChannel = updatedSelected,
            recentChannels = updatedRecents
        )
    }

    /**
     * The same four lists with the favourite flag cleared everywhere.
     *
     * Lived as an inline block inside `clearFavorites`, where it was rebuilding the
     * whole catalogue *and* re-filtering the whole list on the main thread — the one
     * pass in this file that had not been moved off it. It is here now for the reason
     * [rebuildForFavorite] is: it touches four lists at once, and the only way they
     * cannot disagree is to derive them together.
     *
     * Unlike the single-channel case, clearing favourites always re-filters rather
     * than substituting into the existing list. Membership of the Favourites tab
     * changes for *every* channel at once, and on any other tab the category is not
     * the Favourites tab so the list is not derived from the flag — so the filter is
     * asked, once, rather than reasoned about per category.
     *
     * Callers must run this off the main thread: it is a whole-catalogue pass.
     */
    fun clearAllFavorites(state: TvUiState): FavoriteUpdate {
        val updatedChannels = state.channels.map { it.copy(isFavorite = false) }
        return FavoriteUpdate(
            channels = updatedChannels,
            filteredChannels = ChannelFilterEngine.filter(
                updatedChannels,
                state.selectedCategory,
                state.searchQuery
            ),
            selectedChannel = state.selectedChannel?.copy(isFavorite = false),
            recentChannels = state.recentChannels.map { it.copy(isFavorite = false) }
        )
    }
}

/**
 * [FavoriteRebuilder.clearAllFavorites], named for the call site.
 *
 * Exists so [TvViewModel.clearFavorites] reads as a sequence of steps rather than as
 * a block of list rebuilding, and so the whole-catalogue work is unmistakably
 * something to be scheduled rather than something to be done inline.
 */
internal fun stateWithFavoritesCleared(state: TvUiState): FavoriteUpdate =
    FavoriteRebuilder.clearAllFavorites(state)
