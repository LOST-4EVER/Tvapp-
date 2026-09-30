package com.example.kurdishtv.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Manages countdown logic for the sleep timer feature.
 * Automatically triggers [onTimerFinished] when countdown reaches 0.
 */
class SleepTimerManager(
    private val scope: CoroutineScope,
    private val onTimerFinished: () -> Unit
) {
    private val _sleepTimer = MutableStateFlow(SleepTimerState())
    val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()

    private var timerJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        timerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimer.value = SleepTimerState()
            return
        }

        val totalSeconds = minutes * 60
        _sleepTimer.value = SleepTimerState(
            minutes = minutes,
            formattedText = formatRemainingTime(totalSeconds)
        )

        timerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _sleepTimer.value = SleepTimerState(
                    minutes = minutes,
                    formattedText = formatRemainingTime(remaining)
                )
            }
            _sleepTimer.value = SleepTimerState()
            onTimerFinished()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        _sleepTimer.value = SleepTimerState()
    }

    private fun formatRemainingTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
