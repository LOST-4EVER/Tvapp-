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

    private var sleepTimerJob: Job? = null
    private var searchJob: Job? = null
    private var loadJob: Job? = null
    private var updateJob: Job? = null

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /** Downloaded APKs live in cache/updates, which the system may clear. */
    private val updateDir: File?
        get() = appContext?.let { File(it.cacheDir, "updates") }

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
        // Check for updates in the background so a release is noticed without the
        // user hunting for it. Failures are silent here; Settings surfaces them.
        viewModelScope.launch { checkForUpdate(silent = true) }
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
                    // The previously selected channel can vanish between refreshes —
                    // a source going down, or a playlist being edited. Keeping the old
                    // reference would leave the side player and the transport
                    // controls pointing at something that is no longer in the list,
                    // and `selectNextChannel` would jump to the first entry instead of
                    // continuing from where the user was.
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
            _updateState.value = UpdateState.Failed(
                "Android needs permission to install this update. Tap Install again to open that setting."
            )
            return false
        }
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
     */
    fun requestInstallUpdate(update: AppUpdate, filePath: String) {
        viewModelScope.launch {
            if (!installUpdate(update, filePath) && needsInstallPermission()) {
                openInstallPermissionSettings()
            }
        }
    }

    /** True when the app still needs permission to install packages. */
    fun needsInstallPermission(): Boolean {
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
