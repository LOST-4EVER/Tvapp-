package com.example.kurdishtv.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.ChannelFilterEngine
import com.example.kurdishtv.model.KurdishChannelCatalog
import com.example.kurdishtv.network.NetworkMonitor
import com.example.kurdishtv.repository.TvRepository
import com.example.kurdishtv.update.AppUpdate
import com.example.kurdishtv.update.UpdateChecker
import com.example.kurdishtv.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TvViewModel(
    private val repository: TvRepository,
    private val networkMonitor: NetworkMonitor,
    private val updateChecker: UpdateChecker? = null,
    private val appContext: Context? = null
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

    /**
     * The sleep timer, on its own flow.
     *
     * It updates once a second, and it is a readout for the player and nothing else.
     * As fields of [TvUiState] it meant a running timer invalidated the main state —
     * and therefore every collector of it, which is the whole navigation graph — once
     * a second, whether or not the player was the visible screen.
     */
    private val sleepTimerManager = SleepTimerManager(viewModelScope) {
        _uiState.update { it.copy(isPlaybackPaused = true) }
    }
    val sleepTimer: StateFlow<SleepTimerState> = sleepTimerManager.sleepTimer

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    private val keypadController = ChannelKeypadController(
        coroutineScope = viewModelScope,
        getFilteredChannels = { _uiState.value.filteredChannels },
        onChannelSelected = { channel -> onChannelSelected(channel) }
    )
    val channelJump: StateFlow<ChannelJump?> = keypadController.channelJump

    private val updateController = UpdateController(viewModelScope, updateChecker, appContext)
    val updateState: StateFlow<UpdateState> = updateController.updateState
    val needsInstallPermission: StateFlow<Boolean> = updateController.needsInstallPermission

    private companion object {
        /** Long enough to coalesce a burst of keystrokes, short enough to feel live. */
        const val SEARCH_DEBOUNCE_MS = 220L
    }

    // ── Channel numbers ─────────────────────────────────────────────────────

    fun onNumericKey(digit: Int) {
        keypadController.onNumericKey(digit)
    }

    fun onNumericBackspace() {
        keypadController.onNumericBackspace()
    }

    fun commitChannelJump() {
        keypadController.commitChannelJump()
    }

    fun cancelChannelJump() {
        keypadController.cancelChannelJump()
    }

    init {
        observeNetwork()
        viewModelScope.launch {
            // A still-fresh cache short-circuits the network fetch. The instant path
            // reports whether that is what it found, because if it is then the
            // follow-up load is provably a no-op: it would read the same file, apply
            // the same favourites and filter the same list, all to arrive at exactly
            // what is already on screen. That was a second disk read, a second JSON
            // parse and a second pass over every channel on every single cold start.
            if (!loadInstantState()) {
                loadChannels(forceRefresh = false)
            }
        }
        // Check for updates in the background so a release is noticed without the
        // user hunting for it. Failures are silent here; Settings surfaces them.
        //
        // Throttled: the check is uncacheable by design, so running it on every
        // launch meant a network round trip every time the app was opened. Releases
        // do not appear and vanish, so a check every few hours finds a new build
        // just as promptly, and an explicit check from Settings always runs.
        viewModelScope.launch {
            val checker = updateChecker ?: return@launch
            if (checker.isSilentCheckDue()) {
                // Recorded before the check, not after, so a server that is
                // unreachable does not make every subsequent launch retry it.
                checker.recordSilentCheck()
                checkForUpdate(silent = true)
            }
        }
    }

    /**
     * Hydrates the UI from the on-disk cache and saved preferences. Runs on [Dispatchers.IO]
     * because it touches the file system and SharedPreferences.
     *
     * @return true when the list on screen came from an on-disk cache that is still
     *   fresh, meaning a non-forced [loadChannels] would only reproduce it.
     */
    private suspend fun loadInstantState(): Boolean {
        return try {
            val instant = withContext(Dispatchers.IO) { repository.getInstantInitialChannels() }
            if (instant.channels.isEmpty()) return false

            // One hop to IO for both preference reads rather than two: each
            // `withContext` is a dispatch and a suspension, and on a cold start this
            // is the path that decides how soon the grid has real channels in it.
            val (customUrls, recentIds) = withContext(Dispatchers.IO) {
                repository.getCustomPlaylistUrls() to repository.getRecentChannelIds()
            }

            // Filtered on a background dispatcher, like every other whole-catalogue
            // pass. `update` is a compare-and-set loop and is safe to run from any
            // thread, so the filter and the swap that publishes it stay one atomic
            // step — the query and the category cannot move between the two — without
            // that step's several hundred string comparisons landing on the main
            // thread during a cold start.
            withContext(Dispatchers.Default) {
                // One pass over the catalogue for the handful of channels the history
                // names, rather than a hash entry per channel to look them up.
                // See [channelsForIds] for why the map was the wrong shape here.
                val recents = channelsForIds(instant.channels, recentIds)
                _uiState.update { state ->
                    val filtered = ChannelFilterEngine.filter(
                        channels = instant.channels,
                        category = state.selectedCategory,
                        query = state.searchQuery
                    )
                    state.copy(
                        channels = instant.channels,
                        filteredChannels = filtered,
                        recentChannels = recents,
                        customPlaylistUrls = customUrls,
                        selectedChannel = state.selectedChannel ?: instant.channels.firstOrNull()
                    )
                }
            }
            instant.fromFreshCache
        } catch (_: Exception) {
            false
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                val wasOffline = _uiState.value.isOffline
                _uiState.update { it.copy(isOffline = !isOnline) }
                if (wasOffline && isOnline) {
                    // Coming back online is when stale data *may* need replacing —
                    // but a reconnect is not itself a reason to re-download every
                    // source. A Wi-Fi blip in the middle of a film is a two-second
                    // outage, and forcing a refresh re-fetched all four playlists and
                    // the update manifest and then overwrote a perfectly current
                    // cache with the result. The freshness check does the deciding:
                    // seconds-old data is served as-is, and only a list that is
                    // actually stale goes back to the network.
                    //
                    // It also settles the thrash. `loadChannels` refuses to start
                    // over an automatic load that is already running, so a network
                    // that flaps offline/online repeatedly can no longer cancel and
                    // restart a four-source download each time it wavers.
                    loadChannels(forceRefresh = false)
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

                val (customUrls, recentIds) = withContext(Dispatchers.IO) {
                    repository.getCustomPlaylistUrls() to repository.getRecentChannelIds()
                }

                // Same reasoning as in [loadInstantState]: the merge can hold well over
                // a thousand channels once the remote playlists land, and this pass and
                // the state swap that publishes it are one atomic step.
                withContext(Dispatchers.Default) {
                    // Resolved against the ids actually being asked about, rather
                    // than against a map of the whole catalogue.
                    //
                    // It used to be `list.associateBy { it.id }`, a hash entry for
                    // every channel in the merged list, well over a thousand of them
                    // once the remote playlists land. Its only two uses were looking
                    // up a handful of recent ids and testing one selected id, so
                    // almost every entry built was never read: roughly a thousand
                    // `Channel` keys, a thousand map insertions and a thousand hash
                    // computations to answer about a dozen questions.
                    //
                    // [channelsForIds] reads the list once and keeps only the dozen
                    // channels asked for, so the allocation is proportional to the
                    // question rather than to the catalogue. It is called from all
                    // three places that were building this map.
                    val recents = channelsForIds(list, recentIds)

                    // Whether the previously selected channel survived the refresh.
                    //
                    // A source going down, or a playlist being edited, can remove a
                    // channel between refreshes. Keeping the old reference would leave
                    // the side player and the transport controls pointing at something
                    // no longer in the list, and `selectNextChannel` would jump to the
                    // first entry instead of continuing from where the user was.
                    //
                    // Asked against the channel the `update` below actually finds, not
                    // against one captured beforehand — `update` is a compare-and-set
                    // and may merge against a state published since, and a membership
                    // test for one channel answered about a *different* channel is
                    // worse than no test at all.
                    //
                    // One sequential scan of references, comparing a hoisted string.
                    // That is cheap enough to sit inside the block even if the loop
                    // retries, and it is still enormously cheaper than the
                    // thousand-entry map this replaced.
                    _uiState.update { state ->
                        val filtered = ChannelFilterEngine.filter(
                            channels = list,
                            category = state.selectedCategory,
                            query = state.searchQuery
                        )
                        val selected = state.selectedChannel
                            ?.takeIf { current -> list.any { it.id == current.id } }
                            ?: list.firstOrNull()
                        state.copy(
                            isLoading = false,
                            channels = list,
                            filteredChannels = filtered,
                            recentChannels = recents,
                            customPlaylistUrls = customUrls,
                            selectedChannel = selected
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        // The numbers name positions in the current results, and the results are
        // changing underneath them.
        if (channelJump.value != null) cancelChannelJump()
        // Store the query immediately so the text field stays responsive, but debounce the
        // actual filtering. Re-filtering every channel on every keystroke was the main cause
        // of jank while typing, since the merged remote playlists can hold thousands of
        // entries.
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val state = _uiState.value
            if (state.searchQuery != query) return@launch
            val filtered = withContext(Dispatchers.Default) {
                ChannelFilterEngine.filter(state.channels, state.selectedCategory, query)
            }
            _uiState.update { current ->
                if (current.searchQuery != query || current.channels !== state.channels) current
                else current.copy(filteredChannels = filtered)
            }
        }
    }

    fun onCategorySelected(category: CategoryFilter) {
        // Same reason as the search: a number typed into the previous category does
        // not name the same channel in the new one.
        if (channelJump.value != null) cancelChannelJump()
        // A category switch supersedes any in-flight search debounce.
        searchJob?.cancel()
        // Already showing it, and the filtered list tracks it: nothing to compute.
        if (_uiState.value.selectedCategory == category) return
        // The selection is applied immediately so the chip row and the grid header
        // agree with what was pressed, and the filtered list follows when it is
        // computed. Publishing the selection only together with the result meant a
        // slow filter let the UI claim a category it was not yet showing.
        _uiState.update { it.copy(selectedCategory = category) }
        // Filtering the whole catalogue is a few hundred string comparisons. That is
        // not enough to be worth a thread hop on its own, but it does not need to run
        // on the main thread either, and the D-pad can fire this faster than a person
        // can read the result — arrowing across the rail re-filters on every step.
        viewModelScope.launch {
            val state = _uiState.value
            val filtered = withContext(Dispatchers.Default) {
                ChannelFilterEngine.filter(state.channels, category, state.searchQuery)
            }
            // Only publish if this is still the newest request: same category, the
            // query that was in force when this pass started, and the same channel
            // list the pass was computed from. The guard used to be inverted — it
            // published when the world HAD moved — so a slower, older pass could
            // land after a newer one and overwrite the newer category with its own,
            // leaving the grid showing the wrong tab. A query that moved is the
            // search job's to publish, not this one's, and a catalogue that moved is
            // the merge's.
            _uiState.update { current ->
                if (current.selectedCategory == category &&
                    current.searchQuery == state.searchQuery &&
                    current.channels === state.channels
                ) {
                    current.copy(filteredChannels = filtered)
                } else {
                    current
                }
            }
        }
    }

    fun onFavoriteToggled(channelId: String) {
        viewModelScope.launch {
            // SharedPreferences write: keep it off the main thread.
            val isFav = withContext(Dispatchers.IO) {
                runCatching { repository.toggleFavorite(channelId) }.getOrNull()
            } ?: return@launch

            // Three list rebuilds over a catalogue that runs to several hundred
            // channels, for one boolean. Built on a worker and only swapped in
            // here, so the tap does not spend its first frames on the thread that
            // has to draw the grid it is drawing into.
            val snapshot = _uiState.value
            val rebuilt = withContext(Dispatchers.Default) {
                FavoriteRebuilder.rebuildForFavorite(snapshot, channelId, isFav)
            }
            _uiState.update { state ->
                // A merge, a search or a second tap can land while the rebuild is
                // in flight, and then the answer describes a list that is no longer
                // on screen. Identity, not equality: a re-merged catalogue is a
                // different object even when it holds the same channels, and
                // recomputing for it is what keeps the two answers consistent.
                if (state.channels !== snapshot.channels) {
                    val fresh = FavoriteRebuilder.rebuildForFavorite(state, channelId, isFav)
                    state.copy(
                        channels = fresh.channels,
                        filteredChannels = fresh.filteredChannels,
                        selectedChannel = fresh.selectedChannel,
                        recentChannels = fresh.recentChannels
                    )
                } else {
                    state.copy(
                        channels = rebuilt.channels,
                        filteredChannels = rebuilt.filteredChannels,
                        selectedChannel = rebuilt.selectedChannel,
                        recentChannels = rebuilt.recentChannels
                    )
                }
            }
        }
    }

    fun onChannelSelected(channel: Channel) {
        // Any number on screen is now describing a different selection, and leaving
        // the readout up would claim a channel the viewer has moved off.
        if (channelJump.value != null) cancelChannelJump()
        // Selecting the channel should feel instant, so update the selection first and
        // refresh the "recently watched" row once the write has completed.
        _uiState.update { it.copy(selectedChannel = channel, isPlaybackPaused = false) }
        viewModelScope.launch {
            // The store hands back the history it just wrote, so this does not
            // read the file again to be told what it already knows. It used to:
            // build a JSON array, write it, read it straight back and parse it,
            // on every press of OK.
            val recents = withContext(Dispatchers.IO) {
                runCatching { repository.addRecentChannel(channel.id) }
                    .getOrDefault(emptyList())
            }
            if (recents.isEmpty()) return@launch
            // Deliberately does **not** write `selectedChannel` back.
            //
            // This block finishes some time after the selection was made, and on a
            // cold cache the write can take long enough for the user to have tapped
            // another channel in the meantime. Re-asserting the old value here used
            // to snap the selection back to the channel they had already moved on
            // from. The recents row is the only thing this coroutine owns.
            val channels = _uiState.value.channels
            val mappedRecents = withContext(Dispatchers.Default) {
                channelsForIds(channels, recents)
            }
            _uiState.update { state ->
                state.copy(recentChannels = mappedRecents)
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
                // The write decides the message. Reporting "Favorites cleared" after a
                // failed write was the worst version of this: the hearts vanished from
                // the grid, the confirmation appeared, and the favourites were still
                // there after a restart.
                val cleared = withContext(Dispatchers.IO) {
                    runCatching { repository.clearFavorites() }.getOrDefault(false)
                }
                if (!cleared) {
                    _uiState.update { it.copy(actionMessage = "Could not clear favourites") }
                    return@launch
                }

                // The rebuild runs on a worker, like every other whole-catalogue pass
                // in this file.
                //
                // It used to run inside the `update` block below, and `update` is a
                // compare-and-set on the main thread — so clearing favourites walked
                // every channel in the catalogue *and* re-filtered the whole list on
                // the thread that has to draw the grid it is drawing into. That is the
                // same work [onFavoriteToggled] deliberately moved off it.
                //
                // `update` may also re-run its block, which would mean doing the pass
                // more than once for one tap.
                val snapshot = _uiState.value
                val rebuilt = withContext(Dispatchers.Default) {
                    stateWithFavoritesCleared(snapshot)
                }
                _uiState.update { state ->
                    // A merge can land while the rebuild is in flight, in which case
                    // the answer describes a list that is no longer on screen. Identity,
                    // not equality — a re-merged catalogue is a different object even
                    // when it holds the same channels.
                    val fresh = if (state.channels !== snapshot.channels) {
                        stateWithFavoritesCleared(state)
                    } else {
                        rebuilt
                    }
                    state.copy(
                        channels = fresh.channels,
                        filteredChannels = fresh.filteredChannels,
                        selectedChannel = fresh.selectedChannel,
                        recentChannels = fresh.recentChannels,
                        actionMessage = "Favorites cleared"
                    )
                }
            }
        }

    fun clearRecents() {
        viewModelScope.launch {
            val cleared = withContext(Dispatchers.IO) {
                runCatching { repository.clearRecents() }.getOrDefault(false)
            }
            _uiState.update { state ->
                state.copy(
                    recentChannels = if (cleared) emptyList() else state.recentChannels,
                    actionMessage = if (cleared) "Watch history cleared" else "Could not clear watch history"
                )
            }
        }
    }

    fun clearCustomPlaylists() {
        viewModelScope.launch {
            val cleared = withContext(Dispatchers.IO) {
                runCatching { repository.clearCustomPlaylists() }.getOrDefault(false)
            }
            if (!cleared) {
                _uiState.update { it.copy(actionMessage = "Could not remove custom playlists") }
                return@launch
            }
            _uiState.update { state ->
                state.copy(customPlaylistUrls = emptySet(), actionMessage = "Custom playlists removed")
            }
            loadChannels()
        }
    }

    fun clearChannelCache() {
        viewModelScope.launch {
            val cleared = withContext(Dispatchers.IO) {
                runCatching { repository.clearChannelCache() }.getOrDefault(false)
            }
            _uiState.update { state ->
                state.copy(
                    actionMessage = if (cleared) "Channel cache cleared" else "Could not clear the channel cache"
                )
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerManager.setSleepTimer(minutes)
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun selectNextChannel() {
        val list = _uiState.value.filteredChannels.ifEmpty { _uiState.value.channels }
        if (list.isEmpty()) return
        // With nothing selected yet there is no "current" to step from, so start at
        // the top of the list rather than doing nothing. Before this, the first
        // press of the right-arrow key after a cold start appeared to be ignored.
        val current = _uiState.value.selectedChannel
        val idx = current?.let { c -> list.indexOfFirst { it.id == c.id } } ?: -1
        if (idx != -1 && idx < list.size - 1) {
            onChannelSelected(list[idx + 1])
        } else {
            onChannelSelected(list.first())
        }
    }

    fun selectPreviousChannel() {
        val list = _uiState.value.filteredChannels.ifEmpty { _uiState.value.channels }
        if (list.isEmpty()) return
        val current = _uiState.value.selectedChannel
        val idx = current?.let { c -> list.indexOfFirst { it.id == c.id } } ?: -1
        if (idx > 0) {
            onChannelSelected(list[idx - 1])
        } else {
            onChannelSelected(list.last())
        }
    }

    // ── In-app update (delegated to UpdateController) ─────────────────────────

    fun checkForUpdate(silent: Boolean = false) {
        updateController.checkForUpdate(silent)
    }

    fun downloadUpdate(update: AppUpdate) {
        updateController.downloadUpdate(update)
    }

    suspend fun installUpdate(update: AppUpdate, filePath: String): Boolean {
        return updateController.installUpdate(update, filePath)
    }

    fun requestInstallUpdate(update: AppUpdate, filePath: String) {
        updateController.requestInstallUpdate(update, filePath)
    }

    fun refreshInstallPermission() {
        updateController.refreshInstallPermission()
    }

    fun openInstallPermissionSettings() {
        updateController.openInstallPermissionSettings()
    }

    fun clearUpdateMessage() {
        updateController.clearUpdateMessage()
    }

    class Factory(
        private val repository: TvRepository,
        private val networkMonitor: NetworkMonitor,
        private val updateChecker: UpdateChecker? = null,
        private val appContext: Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TvViewModel::class.java)) {
                return TvViewModel(repository, networkMonitor, updateChecker, appContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
