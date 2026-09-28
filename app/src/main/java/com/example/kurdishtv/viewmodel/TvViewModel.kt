package com.example.kurdishtv.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.ChannelFilterEngine
import com.example.kurdishtv.model.KurdishChannelCatalog
import com.example.kurdishtv.network.NetworkMonitor
import com.example.kurdishtv.repository.TvRepository
import com.example.kurdishtv.update.ApkInstaller
import com.example.kurdishtv.update.AppUpdate
import com.example.kurdishtv.update.DownloadState
import com.example.kurdishtv.update.UpdateChecker
import com.example.kurdishtv.update.UpdateState
import java.io.File
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
    private val _sleepTimer = MutableStateFlow(SleepTimerState())
    val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var searchJob: Job? = null
    private var loadJob: Job? = null
    private var updateJob: Job? = null

    // ── Channel numbers on the remote keypad ────────────────────────────────
    //
    // A television remote has a number pad, and this app ignored every key on it.
    // The numbers are the fastest way to reach a channel on a screen with six
    // hundred of them, and they are what a viewer who knows "NRT is 47" reaches
    // for without thinking about it.
    private val _channelJump = MutableStateFlow<ChannelJump?>(null)
    val channelJump: StateFlow<ChannelJump?> = _channelJump.asStateFlow()

    private var jumpCommitJob: Job? = null

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /** Downloaded APKs live in cache/updates, which the system may clear. */
    private val updateDir: File?
        get() = appContext?.let { File(it.cacheDir, "updates") }

    private companion object {
        /** Long enough to coalesce a burst of keystrokes, short enough to feel live. */
        const val SEARCH_DEBOUNCE_MS = 220L

        /**
         * How long a typed number waits for another digit before it is acted on.
         *
         * A viewer typing "47" has to be given time to type the 7, and a viewer who
         * stops at "4" has to be given time to notice that nothing happened. Just
         * over a second is the usual compromise on a television, where the distance
         * to the remote makes fine motor timing less reliable than it is on a
         * keyboard.
         */
        const val JUMP_COMMIT_DELAY_MS = 1_400L

        /**
         * The most digits a channel number can have.
         *
         * Not cosmetic: a remote key that sticks, or a viewer holding a digit down
         * through auto-repeat, would otherwise append forever and the number could
         * never match anything again. Four digits is beyond any list this app holds.
         */
        const val MAX_JUMP_DIGITS = 4
    }

    // ── Channel numbers ─────────────────────────────────────────────────────

    /**
     * Adds a digit to the number being typed.
     *
     * `digit` is 0-9. Anything else is ignored rather than clamped, because a key
     * that is not a number reaching here means the caller is wrong, and guessing
     * would be worse than doing nothing.
     */
    fun onNumericKey(digit: Int) {
        if (digit !in 0..9) return
        val current = _channelJump.value?.digits.orEmpty()
        setJump((current + digit).takeLast(MAX_JUMP_DIGITS))
    }

    /** Removes the last digit, or cancels the whole entry if it was the only one. */
    fun onNumericBackspace() {
        val shorter = _channelJump.value?.digits?.dropLast(1) ?: return
        if (shorter.isEmpty()) cancelChannelJump() else setJump(shorter)
    }

    /**
     * Publishes a number and restarts the idle timer that acts on it.
     *
     * One place, because the target has to be recomputed and the timer restarted on
     * every change to the digits — and doing the two separately is how the readout
     * ends up describing a number the viewer has already edited past.
     */
    private fun setJump(digits: String) {
        _channelJump.value = ChannelJump(digits = digits, target = jumpTargetFor(digits))
        jumpCommitJob?.cancel()
        jumpCommitJob = viewModelScope.launch {
            delay(JUMP_COMMIT_DELAY_MS)
            commitChannelJump()
        }
    }

    /**
     * Acts on the number being typed and clears the readout.
     *
     * Selects rather than plays. Typing a number is a way of *moving* through the
     * list — the grid scrolls the channel into view and takes D-pad focus, and the
     * preview pane follows — and the viewer presses OK for the channel they want,
     * exactly as they would if they had arced to it. Jumping straight to fullscreen
     * video from a number key makes every mistyped digit a stream the app then has
     * to start, buffer and tear down.
     */
    fun commitChannelJump() {
        jumpCommitJob?.cancel()
        val target = _channelJump.value?.target
        _channelJump.value = null
        if (target != null) onChannelSelected(target)
    }

    /** Throws the number away without acting on it. */
    fun cancelChannelJump() {
        jumpCommitJob?.cancel()
        _channelJump.value = null
    }

    /**
     * The channel a number names, in the list the viewer is actually looking at.
     *
     * The *filtered* list, not the whole catalogue: the numbers are the ones shown
     * in the sidebar and implied by the grid, so a number that only resolved
     * against channels the viewer has filtered out, searched away or never scrolled
     * to would tune a channel they cannot see and cannot recognise.
     *
     * One-based, because that is how a television numbers its channels and how the
     * sidebar labels them. Zero is not a channel number, so it never resolves —
     * which is also what makes it a safe digit to *start* a number with.
     */
    private fun jumpTargetFor(digits: String): Channel? {
        val number = digits.toIntOrNull() ?: return null
        if (number < 1) return null
        return _uiState.value.filteredChannels.getOrNull(number - 1)
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
            val byId = instant.channels.associateBy { it.id }

            val customUrls = withContext(Dispatchers.IO) { repository.getCustomPlaylistUrls() }
            val recentIds = withContext(Dispatchers.IO) { repository.getRecentChannelIds() }
            val recents = recentIds.mapNotNull { id -> byId[id] }

            // Filtered on a background dispatcher, like every other whole-catalogue
            // pass. `update` is a compare-and-set loop and is safe to run from any
            // thread, so the filter and the swap that publishes it stay one atomic
            // step — the query and the category cannot move between the two — without
            // that step's several hundred string comparisons landing on the main
            // thread during a cold start.
            withContext(Dispatchers.Default) {
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

                val customUrls = withContext(Dispatchers.IO) { repository.getCustomPlaylistUrls() }
                val recentIds = withContext(Dispatchers.IO) { repository.getRecentChannelIds() }
                val byId = list.associateBy { it.id }
                val recents = recentIds.mapNotNull { id -> byId[id] }
                // Built here rather than inside the `update` below, which is a
                // compare-and-set loop that can run more than once: the membership
                // test was a linear `list.any { it.id == ... }` over the whole merged
                // catalogue, and paying for that on every retry of a contended
                // compare-and-set is a scan of a thousand channels to answer one
                // yes/no question.
                val ids = byId.keys

                // Same reasoning as in [loadInstantState]: the merge can hold well over
                // a thousand channels once the remote playlists land, and this pass and
                // the state swap that publishes it are one atomic step.
                withContext(Dispatchers.Default) {
                    _uiState.update { state ->
                        val filtered = ChannelFilterEngine.filter(
                            channels = list,
                            category = state.selectedCategory,
                            query = state.searchQuery
                        )
                        // The previously selected channel can vanish between
                        // refreshes — a source going down, or a playlist being edited.
                        // Keeping the old reference would leave the side player and
                        // the transport controls pointing at something that is no
                        // longer in the list, and `selectNextChannel` would jump to
                        // the first entry instead of continuing from where the user
                        // was.
                        val selected = state.selectedChannel
                            ?.takeIf { current -> ids.contains(current.id) }
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
        if (_channelJump.value != null) cancelChannelJump()
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
                if (current.searchQuery != query) current
                else current.copy(filteredChannels = filtered)
            }
        }
    }

    fun onCategorySelected(category: CategoryFilter) {
        // Same reason as the search: a number typed into the previous category does
        // not name the same channel in the new one.
        if (_channelJump.value != null) cancelChannelJump()
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
            // Only publish if this is still the newest request: same category, and
            // the query that was in force when this pass started. The guard used to
            // be inverted — it published when the world HAD moved — so a slower,
            // older pass could land after a newer one and overwrite the newer
            // category with its own, leaving the grid showing the wrong tab. A query
            // that moved is the search job's to publish, not this one's.
            _uiState.update { current ->
                if (current.selectedCategory == category &&
                    current.searchQuery == state.searchQuery
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

                // Only the Favourites tab actually changes membership when a heart is
                // tapped. In the other eleven categories the channel is in the list
                // before and after, and what changed about it is one boolean — so
                // the same `Channel` instance with the flag flipped is substituted
                // into the existing filtered list instead of re-deriving the list
                // from several hundred channels.
                //
                // This is the action a viewer performs most often after watching
                // something, and it is on the main thread, so the difference between
                // "replace one element" and "refilter everything" is the difference
                // between instant and a visible stall on a large merged playlist.
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
        // Any number on screen is now describing a different selection, and leaving
        // the readout up would claim a channel the viewer has moved off.
        if (_channelJump.value != null) cancelChannelJump()
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
            // Deliberately does **not** write `selectedChannel` back.
            //
            // This block finishes some time after the selection was made, and on a
            // cold cache the write can take long enough for the user to have tapped
            // another channel in the meantime. Re-asserting the old value here used
            // to snap the selection back to the channel they had already moved on
            // from. The recents row is the only thing this coroutine owns.
            val byId = _uiState.value.channels.associateBy { it.id }
            _uiState.update { state ->
                state.copy(recentChannels = recents.mapNotNull { id -> byId[id] })
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
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimer.value = SleepTimerState()
            return
        }

        val totalSeconds = minutes * 60
        _sleepTimer.value = SleepTimerState(
            minutes = minutes,
            formattedText = formatRemainingTime(totalSeconds)
        )

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                // Only the timer's own flow moves. See [sleepTimer].
                _sleepTimer.value = SleepTimerState(
                    minutes = minutes,
                    formattedText = formatRemainingTime(remaining)
                )
            }
            // Sleep timer finished: pause playback cleanly
            _sleepTimer.value = SleepTimerState()
            _uiState.update { it.copy(isPlaybackPaused = true) }
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

    // ── In-app update ─────────────────────────────────────────────────────────

    /**
     * Asks GitHub whether a newer build exists.
     *
     * @param silent when true, a failed check leaves the UI untouched so the
     *   automatic background check never surfaces an error the user did not ask for.
     */
    fun checkForUpdate(silent: Boolean = false) {
        val checker = updateChecker ?: return
        if (updateJob?.isActive == true) return

        updateJob = viewModelScope.launch {
            if (!silent) _updateState.value = UpdateState.Checking

            val result = checker.checkForUpdate(BuildConfig.VERSION_CODE)
            result
                .onSuccess { update ->
                    _updateState.value = when {
                        update != null -> UpdateState.Available(update)
                        silent -> UpdateState.Idle
                        else -> UpdateState.UpToDate(BuildConfig.VERSION_NAME)
                    }
                }
                .onFailure { error ->
                    // A silent check must not show a failure the user did not request.
                    if (!silent) {
                        _updateState.value = UpdateState.Failed(
                            error.message ?: "Could not reach the update server"
                        )
                    }
                }
        }
    }

    /** Downloads an available update, reporting progress into [updateState]. */
    fun downloadUpdate(update: AppUpdate) {
        val checker = updateChecker ?: return
        val dir = updateDir ?: return
        if (updateJob?.isActive == true) return

        updateJob = viewModelScope.launch {
            _updateState.value = UpdateState.Downloading(update, DownloadState.Idle)
            checker.download(update, dir) { progress ->
                _updateState.value = UpdateState.Downloading(update, progress)
            }
                .onSuccess { file ->
                    _updateState.value = UpdateState.ReadyToInstall(update, file.absolutePath)
                }
                .onFailure { error ->
                    _updateState.value = UpdateState.Failed(
                        error.message ?: "Download failed"
                    )
                }
        }
    }

    /**
     * Opens the system installer for a downloaded APK.
     *
     * Suspends, because it is not a cheap call: comparing signing certificates
     * parses the downloaded archive's manifest and certificate block, which on a
     * multi-megabyte APK is real disk I/O and crypto. It used to run synchronously
     * from the Install button's click handler, putting all of that on the main
     * thread for exactly as long as it takes.
     *
     * Returns false when the install could not be started. Each failure gets its own
     * message because the fix is different in every case: a missing file needs a
     * re-download, an ungranted "allow from this source" needs the settings screen,
     * and a changed signing key needs an uninstall.
     */
    suspend fun installUpdate(update: AppUpdate, filePath: String): Boolean {
        val context = appContext ?: return false
        val file = File(filePath)
        if (!file.exists()) {
            _updateState.value = UpdateState.Failed("The downloaded update is missing, please retry")
            return false
        }
        if (!ApkInstaller.canRequestPackageInstalls(context)) {
            _needsInstallPermission.value = true
            // Deliberately not a failure state: the download is still good, the
            // button must survive, and the caller turns this into a trip to Settings.
            // See [requestInstallUpdate].
            return false
        }
        _needsInstallPermission.value = false
        val sameSignature = withContext(Dispatchers.IO) {
            ApkInstaller.isSignedBySameCertificate(context, file)
        }
        if (!sameSignature) {
            // Android reports this only from inside its own installer, as
            // "package conflicts with an existing package", which reads like a
            // corrupt download. It is actually a different signing key, which
            // only an uninstall can clear.
            _updateState.value = UpdateState.Failed(
                "This update is signed with a different key than the app already on this " +
                    "device, so Android cannot upgrade over it. Uninstall Kurdish TV Live, " +
                    "then install the update. Uninstalling clears your favourites and " +
                    "watch history."
            )
            return false
        }
        val started = ApkInstaller.install(context, file)
        if (!started) {
            _updateState.value =
                UpdateState.Failed("This device could not start the package installer")
        }
        return started
    }

    /**
     * Non-suspend entry point for the Install button.
     *
     * Only the "allow from this source" case can be fixed by sending the user to
     * settings. Every other failure (missing file, changed signing key, no
     * installer) is already reported in the update card, and bouncing the user
     * into a settings screen that cannot help is worse than saying what went
     * wrong.
     *
     * Critically, [installUpdate] must be left in [UpdateState.ReadyToInstall] when
     * it stops for want of that permission. It used to overwrite the state with
     * [UpdateState.Failed], and because the card renders its actions *from* the
     * state, that removed the Install button and replaced it with "Retry / Dismiss" —
     * which re-runs the update *check*, not the install. So the flow was: tap Install,
     * get bounced to Settings, grant the permission, come back, and find that the
     * multi-megabyte APK that had already been downloaded could no longer be
     * installed from the app at all. The only way out was a second full download.
     */
    fun requestInstallUpdate(update: AppUpdate, filePath: String) {
        viewModelScope.launch {
            if (!installUpdate(update, filePath) && needsInstallPermissionNow()) {
                // Put the card back where it was before bouncing to Settings, so the
                // Install button is still there on the way back in.
                _updateState.value = UpdateState.ReadyToInstall(update, filePath)
                openInstallPermissionSettings()
            }
        }
    }

    /**
     * Whether Android is still refusing to let this app install packages.
     *
     * Exposed as state rather than as a function because the update card reads it
     * during composition, and the underlying check goes to `PackageManager`. Reading
     * it on every recomposition of the navigation graph meant a binder round trip
     * per frame of whatever animation was running; the answer only ever changes when
     * the user leaves and re-enters the system settings screen, so it is re-read
     * when Settings is opened and when an install is attempted.
     */
    private val _needsInstallPermission = MutableStateFlow(false)
    val needsInstallPermission: StateFlow<Boolean> = _needsInstallPermission.asStateFlow()

    /** Re-reads the permission. Cheap, and only called at the two moments it can change. */
    fun refreshInstallPermission() {
        val context = appContext ?: return
        _needsInstallPermission.value = !ApkInstaller.canRequestPackageInstalls(context)
    }

    /** True when the app still needs permission to install packages. */
    private fun needsInstallPermissionNow(): Boolean {
        val context = appContext ?: return false
        return !ApkInstaller.canRequestPackageInstalls(context)
    }

    fun openInstallPermissionSettings() {
        appContext?.let { ApkInstaller.openInstallPermissionSettings(it) }
    }

    fun clearUpdateMessage() {
        _updateState.value = UpdateState.Idle
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
