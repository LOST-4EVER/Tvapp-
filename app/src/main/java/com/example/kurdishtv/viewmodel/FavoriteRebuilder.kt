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
}
