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
    val isPlaybackPaused: Boolean = false,
    val isMuted: Boolean = false,
    val importMessage: String? = null,
    val actionMessage: String? = null
)

/**
 * The sleep-timer readout.
 *
 * Kept out of [TvUiState] for one reason: it changes once a second, and a data class
 * that any field of which can change is a data class every collector of has to
 * re-read. With the countdown in the main state, a running timer invalidated the whole
 * navigation graph and every screen composed beneath it — a grid of several hundred
 * cards included — sixty times a minute, to redraw a "12:43" badge on the player that
 * was not even the visible screen. On its own flow, only the player collects it.
 */
data class SleepTimerState(
    /** The whole duration the viewer chose, so the dialog can show it as selected. */
    val minutes: Int = 0,
    /** `mm:ss` for the player, or null when no timer is running. */
    val formattedText: String? = null
) {
    val isRunning: Boolean get() = minutes > 0
}
