package com.example.kurdishtv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.ChannelFilterEngine
import com.example.kurdishtv.model.KurdishChannelCatalog
import com.example.kurdishtv.network.NetworkMonitor
import com.example.kurdishtv.repository.TvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TvViewModel(
    private val repository: TvRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    // Start from the in-memory curated catalog (zero disk/network cost) and hydrate from
    // cache + preferences on a background dispatcher below. This keeps the main thread free
    // during cold start instead of reading files/prefs synchronously.
    private val _uiState = MutableStateFlow(
        TvUiState(
            channels = KurdishChannelCatalog.getDefaultChannels(),
            filteredChannels = KurdishChannelCatalog.getDefaultChannels()
        )
    )
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var searchJob: Job? = null
    private var loadJob: Job? = null

    private companion object {
        /** Long enough to coalesce a burst of keystrokes, short enough to feel live. */
        const val SEARCH_DEBOUNCE_MS = 220L
    }

    init {
        observeNetwork()
        viewModelScope.launch {
            loadInstantState()
            // Cold start: a still-fresh cache short-circuits the network fetch.
            loadChannels(forceRefresh = false)
        }
    }

    /**
     * Hydrates the UI from the on-disk cache and saved preferences. Runs on [Dispatchers.IO]
     * because it touches the file system and SharedPreferences.
     */
    private suspend fun loadInstantState() {
        try {
            val instant = withContext(Dispatchers.IO) { repository.getInstantInitialChannels() }
            if (instant.isEmpty()) return
            val byId = instant.associateBy { it.id }

            val customUrls = withContext(Dispatchers.IO) { repository.getCustomPlaylistUrls() }
            val recentIds = withContext(Dispatchers.IO) { repository.getRecentChannelIds() }
            val recents = recentIds.mapNotNull { id -> byId[id] }

            _uiState.update { state ->
                val filtered = ChannelFilterEngine.filter(
                    channels = instant,
                    category = state.selectedCategory,
                    query = state.searchQuery
                )
                state.copy(
                    channels = instant,
                    filteredChannels = filtered,
                    recentChannels = recents,
                    customPlaylistUrls = customUrls,
                    selectedChannel = state.selectedChannel ?: instant.firstOrNull()
                )
            }
        } catch (_: Exception) {}
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                val wasOffline = _uiState.value.isOffline
                _uiState.update { it.copy(isOffline = !isOnline) }
                if (wasOffline && isOnline) {
                    // Coming back online is exactly when stale data should be replaced.
                    loadChannels(forceRefresh = true)
                }
            }
        }
    }

    /**
     * Loads the merged channel list.
     *
     * @param forceRefresh when true, bypasses the fresh-cache short circuit. User-driven
     * refreshes and playlist changes must pass true so they actually see new data.
     */
    fun loadChannels(forceRefresh: Boolean = true) {
        // A refresh can be triggered by init, a reconnect, and a playlist edit in quick
        // succession. Each one fans out to every remote source, so overlapping calls only
        // waste bandwidth and fight over the UI state. An explicit user refresh still
        // preempts an in-flight automatic load.
        if (loadJob?.isActive == true && !forceRefresh) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.fetchChannels(forceRefresh)
                val list = result.getOrNull()
                if (list == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message)
                    }
                    return@launch
                }

                val customUrls = withContext(Dispatchers.IO) { repository.getCustomPlaylistUrls() }
                val recentIds = withContext(Dispatchers.IO) { repository.getRecentChannelIds() }
                val byId = list.associateBy { it.id }
                val recents = recentIds.mapNotNull { id -> byId[id] }

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
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        // Store the query immediately so the text field stays responsive, but debounce the
        // actual filtering. Re-filtering every channel on every keystroke was the main cause
        // of jank while typing, since the merged remote playlists can hold thousands of
        // entries.
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { state ->
                if (state.searchQuery != query) return@update state
                val filtered = ChannelFilterEngine.filter(state.channels, state.selectedCategory, query)
                state.copy(filteredChannels = filtered)
            }
        }
    }

    fun onCategorySelected(category: CategoryFilter) {
        // A category switch supersedes any in-flight search debounce.
        searchJob?.cancel()
        _uiState.update { state ->
            val filtered = ChannelFilterEngine.filter(state.channels, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredChannels = filtered)
        }
    }

    fun onFavoriteToggled(channelId: String) {
        viewModelScope.launch {
            // SharedPreferences write: keep it off the main thread.
            val isFav = withContext(Dispatchers.IO) {
                runCatching { repository.toggleFavorite(channelId) }.getOrNull()
            } ?: return@launch
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
        }
    }

    fun onChannelSelected(channel: Channel) {
        // Selecting the channel should feel instant, so update the selection first and
        // refresh the "recently watched" row once the write has completed.
        _uiState.update { it.copy(selectedChannel = channel, isPlaybackPaused = false) }
        viewModelScope.launch {
            val recents = withContext(Dispatchers.IO) {
                runCatching {
                    repository.addRecentChannel(channel.id)
                    repository.getRecentChannelIds()
                }.getOrNull()
            } ?: return@launch
            val channels = _uiState.value.channels
            val byId = channels.associateBy { it.id }
            _uiState.update { state ->
                state.copy(
                    selectedChannel = channel,
                    recentChannels = recents.mapNotNull { id -> byId[id] }
                )
            }
        }
    }

    fun addCustomPlaylist(url: String) {
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                runCatching { repository.addCustomPlaylistUrl(url.trim()) }.getOrDefault(false)
            }
            if (added) {
                _uiState.update { it.copy(importMessage = "Playlist added successfully! Syncing channels...") }
                loadChannels()
            } else {
                _uiState.update { it.copy(importMessage = "Invalid stream link or already added.") }
            }
        }
    }

    fun removeCustomPlaylist(url: String) {
        viewModelScope.launch {
            val removed = withContext(Dispatchers.IO) {
                runCatching { repository.removeCustomPlaylistUrl(url) }.getOrDefault(false)
            }
            if (removed) {
                loadChannels()
            }
        }
    }

    fun clearImportMessage() {
        _uiState.update { it.copy(importMessage = null) }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun clearFavorites() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { repository.clearFavorites() } }
            _uiState.update { state ->
                val updatedChannels = state.channels.map { it.copy(isFavorite = false) }
                val filtered = ChannelFilterEngine.filter(
                    updatedChannels,
                    state.selectedCategory,
                    state.searchQuery
                )
                state.copy(
                    channels = updatedChannels,
                    filteredChannels = filtered,
                    selectedChannel = state.selectedChannel?.copy(isFavorite = false),
                    recentChannels = state.recentChannels.map { it.copy(isFavorite = false) },
                    actionMessage = "Favorites cleared"
                )
            }
        }
    }

    fun clearRecents() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { repository.clearRecents() } }
            _uiState.update { state ->
                state.copy(recentChannels = emptyList(), actionMessage = "Watch history cleared")
            }
        }
    }

    fun clearCustomPlaylists() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { repository.clearCustomPlaylists() } }
            _uiState.update { state ->
                state.copy(customPlaylistUrls = emptySet(), actionMessage = "Custom playlists removed")
            }
            loadChannels()
        }
    }

    fun clearChannelCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { repository.clearChannelCache() } }
            _uiState.update { state -> state.copy(actionMessage = "Channel cache cleared") }
        }
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
