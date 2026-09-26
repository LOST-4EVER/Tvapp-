package com.example.kurdishtv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kurdishtv.data.SettingsStorage
import com.example.kurdishtv.model.AppSettings
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

    init {
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { storage.load() }
            _settings.value = stored
            _loaded.value = true
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        viewModelScope.launch(Dispatchers.IO) { storage.save(next) }
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
