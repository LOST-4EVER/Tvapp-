package com.example.kurdishtv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kurdishtv.data.SettingsStorage
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.ui.player.ResizeMode
import com.example.kurdishtv.ui.player.VideoColorFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns user preferences. Starts from defaults (instant, no disk I/O on the main
 * thread) and hydrates from [SettingsStorage] on a background dispatcher.
 */
class SettingsViewModel(
    private val storage: SettingsStorage
) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _loaded = MutableStateFlow(false)

    /** True once persisted preferences have replaced the defaults. */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /**
     * Bumped by every user edit, read only on the main thread.
     *
     * The initial load is an async disk read, and the Settings screen is reachable
     * on the very first frame. A user who toggles something in that window used to
     * have it silently reverted: [update] wrote the new value and queued the save,
     * then the pending load landed and overwrote `_settings` with the pre-edit
     * state — which was then never written back, so the change was lost outright.
     */
    private var editGeneration = 0

    init {
        viewModelScope.launch {
            val generationAtStart = editGeneration
            val stored = withContext(Dispatchers.IO) { storage.load() }
            // Only adopt the stored value if nothing has been edited since. Either
            // way the screen is now hydrated, so `loaded` flips unconditionally.
            if (editGeneration == generationAtStart) {
                _settings.value = stored
            }
            _loaded.value = true
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        editGeneration++
        _settings.value = next
        viewModelScope.launch(Dispatchers.IO) { storage.save(next) }
    }

    /**
     * Updates only the player-related settings, merging into the current state.
     *
     * The player writes its resize mode and colour filter through here rather
     * than through [update], which replaces the whole object from `_settings.value`:
     * a preferences edit landing between the player's read and write would
     * otherwise be reverted by the player's save.
     */
    fun updatePlayerPreferences(
        resizeMode: ResizeMode? = null,
        videoColorFilter: VideoColorFilter? = null
    ) {
        update { current ->
            current.copy(
                resizeMode = resizeMode ?: current.resizeMode,
                videoColorFilter = videoColorFilter ?: current.videoColorFilter
            )
        }
    }

    fun resetToDefaults() {
        update { AppSettings() }
    }

    class Factory(
        private val storage: SettingsStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(storage) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
