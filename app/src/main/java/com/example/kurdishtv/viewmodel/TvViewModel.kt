package com.example.kurdishtv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.ChannelFilterEngine
import com.example.kurdishtv.network.NetworkMonitor
import com.example.kurdishtv.repository.TvRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class TvViewModel(
    private val repository: TvRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TvUiState(
            channels = repository.getInstantInitialChannels(),
            filteredChannels = repository.getInstantInitialChannels()
        )
    )
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

    private var sleepTimerJob: Job? = null

    init {
        loadSavedRecentsAndPlaylists()
        observeNetwork()
        loadChannels()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                val wasOffline = _uiState.value.isOffline
                _uiState.update { it.copy(isOffline = !isOnline) }
                if (wasOffline && isOnline) {
                    loadChannels()
                }
            }
        }
    }

    private fun loadSavedRecentsAndPlaylists() {
        try {
            val customUrls = repository.getCustomPlaylistUrls()
            val recentIds = repository.getRecentChannelIds()
            val currentChannels = _uiState.value.channels
            val recents = recentIds.mapNotNull { id -> currentChannels.find { it.id == id } }

            _uiState.update { state ->
                val filtered = ChannelFilterEngine.filter(
                    channels = state.channels,
                    category = state.selectedCategory,
                    query = state.searchQuery
                )
                state.copy(
                    customPlaylistUrls = customUrls,
                    recentChannels = recents,
                    filteredChannels = filtered,
                    selectedChannel = state.selectedChannel ?: currentChannels.firstOrNull()
                )
            }
        } catch (_: Exception) {}
    }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.fetchChannels()
                result.onSuccess { list ->
                    val customUrls = repository.getCustomPlaylistUrls()
                    val recentIds = repository.getRecentChannelIds()
                    val recents = recentIds.mapNotNull { id -> list.find { it.id == id } }

                    _uiState.update { state ->
                        val filtered = ChannelFilterEngine.filter(
                            channels = list,
                            category = state.selectedCategory,
                            query = state.searchQuery
                        )
                        state.copy(
                            isLoading = false,
                            channels = list,
                            filteredChannels = filtered,
                            recentChannels = recents,
                            customPlaylistUrls = customUrls,
                            selectedChannel = state.selectedChannel ?: list.firstOrNull()
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = ChannelFilterEngine.filter(state.channels, state.selectedCategory, query)
            state.copy(searchQuery = query, filteredChannels = filtered)
        }
    }

    fun onCategorySelected(category: CategoryFilter) {
        _uiState.update { state ->
            val filtered = ChannelFilterEngine.filter(state.channels, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredChannels = filtered)
        }
    }

    fun onFavoriteToggled(channelId: String) {
        try {
            val isFav = repository.toggleFavorite(channelId)
            _uiState.update { state ->
                val updatedChannels = state.channels.map {
                    if (it.id == channelId) it.copy(isFavorite = isFav) else it
                }
                val updatedSelected = if (state.selectedChannel?.id == channelId) {
                    state.selectedChannel.copy(isFavorite = isFav)
                } else state.selectedChannel

                val updatedRecents = state.recentChannels.map {
                    if (it.id == channelId) it.copy(isFavorite = isFav) else it
                }
                val filtered = ChannelFilterEngine.filter(
                    updatedChannels,
                    state.selectedCategory,
                    state.searchQuery
                )
                state.copy(
                    channels = updatedChannels,
                    filteredChannels = filtered,
                    selectedChannel = updatedSelected,
                    recentChannels = updatedRecents
                )
            }
        } catch (_: Exception) {}
    }

    fun onChannelSelected(channel: Channel) {
        try {
            repository.addRecentChannel(channel.id)
            val recentIds = repository.getRecentChannelIds()
            val recents = recentIds.mapNotNull { id -> _uiState.value.channels.find { it.id == id } }

            _uiState.update {
                it.copy(
                    selectedChannel = channel,
                    recentChannels = recents,
                    isPlaybackPaused = false
                )
            }
        } catch (_: Exception) {}
    }

    fun addCustomPlaylist(url: String) {
        val cleanUrl = url.trim()
        if (repository.addCustomPlaylistUrl(cleanUrl)) {
            _uiState.update { it.copy(importMessage = "Playlist added successfully! Syncing channels...") }
            loadChannels()
        } else {
            _uiState.update { it.copy(importMessage = "Invalid stream link or already added.") }
        }
    }

    fun removeCustomPlaylist(url: String) {
        if (repository.removeCustomPlaylistUrl(url)) {
            loadChannels()
        }
    }

    fun clearImportMessage() {
        _uiState.update { it.copy(importMessage = null) }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.update {
                it.copy(
                    sleepTimerMinutes = 0,
                    sleepTimerRemainingSeconds = 0,
                    sleepTimerFormattedText = null
                )
            }
            return
        }

        val totalSeconds = minutes * 60
        _uiState.update {
            it.copy(
                sleepTimerMinutes = minutes,
                sleepTimerRemainingSeconds = totalSeconds,
                sleepTimerFormattedText = formatRemainingTime(totalSeconds)
            )
        }

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                val formatted = formatRemainingTime(remaining)
                _uiState.update {
                    it.copy(
                        sleepTimerRemainingSeconds = remaining,
                        sleepTimerFormattedText = formatted
                    )
                }
            }
            // Sleep timer finished: pause playback cleanly
            _uiState.update {
                it.copy(
                    sleepTimerMinutes = 0,
                    sleepTimerRemainingSeconds = 0,
                    sleepTimerFormattedText = null,
                    isPlaybackPaused = true
                )
            }
        }
    }

    private fun formatRemainingTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun selectNextChannel() {
        val list = _uiState.value.filteredChannels.ifEmpty { _uiState.value.channels }
        val current = _uiState.value.selectedChannel ?: return
        val idx = list.indexOfFirst { it.id == current.id }
        if (idx != -1 && idx < list.size - 1) {
            onChannelSelected(list[idx + 1])
        } else if (list.isNotEmpty()) {
            onChannelSelected(list.first())
        }
    }

    fun selectPreviousChannel() {
        val list = _uiState.value.filteredChannels.ifEmpty { _uiState.value.channels }
        val current = _uiState.value.selectedChannel ?: return
        val idx = list.indexOfFirst { it.id == current.id }
        if (idx > 0) {
            onChannelSelected(list[idx - 1])
        } else if (list.isNotEmpty()) {
            onChannelSelected(list.last())
        }
    }

    class Factory(
        private val repository: TvRepository,
        private val networkMonitor: NetworkMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TvViewModel::class.java)) {
                return TvViewModel(repository, networkMonitor) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
