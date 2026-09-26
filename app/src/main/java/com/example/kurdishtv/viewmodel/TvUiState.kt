package com.example.kurdishtv.viewmodel

import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.KurdishChannelCatalog

data class TvUiState(
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val channels: List<Channel> = KurdishChannelCatalog.getDefaultChannels(),
    val filteredChannels: List<Channel> = KurdishChannelCatalog.getDefaultChannels(),
    val recentChannels: List<Channel> = emptyList(),
    val customPlaylistUrls: Set<String> = emptySet(),
    val selectedCategory: CategoryFilter = CategoryFilter.ALL,
    val searchQuery: String = "",
    val selectedChannel: Channel? = null,
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemainingSeconds: Int = 0,
    val sleepTimerFormattedText: String? = null,
    val isPlaybackPaused: Boolean = false,
    val isMuted: Boolean = false,
    val importMessage: String? = null,
    val actionMessage: String? = null
)
