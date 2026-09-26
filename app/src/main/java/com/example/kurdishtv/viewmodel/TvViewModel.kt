package com.example.kurdishtv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.repository.TvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TvViewModel(
    private val repository: TvRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TvUiState())
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.fetchChannels()
            result.onSuccess { list ->
                val customUrls = repository.getCustomPlaylistUrls()
                val recentIds = repository.getRecentChannelIds()
                val recents = recentIds.mapNotNull { id -> list.find { it.id == id } }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        channels = list,
                        recentChannels = recents,
                        customPlaylistUrls = customUrls,
                        selectedChannel = state.selectedChannel ?: list.firstOrNull()
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to connect to channel server."
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: CategoryFilter) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onFavoriteToggled(channelId: String) {
        val isFav = repository.toggleFavorite(channelId)
        _uiState.update { state ->
            val updatedList = state.channels.map { channel ->
                if (channel.id == channelId) channel.copy(isFavorite = isFav) else channel
            }
            val updatedSelected = if (state.selectedChannel?.id == channelId) {
                state.selectedChannel.copy(isFavorite = isFav)
            } else {
                state.selectedChannel
            }
            val updatedRecents = state.recentChannels.map { channel ->
                if (channel.id == channelId) channel.copy(isFavorite = isFav) else channel
            }
            state.copy(channels = updatedList, selectedChannel = updatedSelected, recentChannels = updatedRecents)
        }
    }

    fun onChannelSelected(channel: Channel) {
        repository.addRecentChannel(channel.id)
        val recentIds = repository.getRecentChannelIds()
        val recents = recentIds.mapNotNull { id -> _uiState.value.channels.find { it.id == id } }

        _uiState.update {
            it.copy(
                selectedChannel = channel,
                recentChannels = recents
            )
        }
    }

    fun addCustomPlaylist(url: String) {
        if (repository.addCustomPlaylistUrl(url)) {
            loadChannels()
        }
    }

    fun removeCustomPlaylist(url: String) {
        if (repository.removeCustomPlaylistUrl(url)) {
            loadChannels()
        }
    }

    fun setSleepTimer(minutes: Int) {
        _uiState.update { it.copy(sleepTimerMinutes = minutes) }
    }

    fun selectNextChannel() {
        val currentList = _uiState.value.filteredChannels.ifEmpty { _uiState.value.channels }
        val current = _uiState.value.selectedChannel ?: return
        val currentIndex = currentList.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < currentList.size - 1) {
            onChannelSelected(currentList[currentIndex + 1])
        } else if (currentList.isNotEmpty()) {
            onChannelSelected(currentList.first())
        }
    }

    fun selectPreviousChannel() {
        val currentList = _uiState.value.filteredChannels.ifEmpty { _uiState.value.channels }
        val current = _uiState.value.selectedChannel ?: return
        val currentIndex = currentList.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            onChannelSelected(currentList[currentIndex - 1])
        } else if (currentList.isNotEmpty()) {
            onChannelSelected(currentList.last())
        }
    }

    class Factory(private val repository: TvRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TvViewModel::class.java)) {
                return TvViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
