package com.example.kurdishtv.viewmodel

import com.example.kurdishtv.model.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the digit entry state for TV remote number pad channel switching.
 */
class ChannelKeypadController(
    private val coroutineScope: CoroutineScope,
    private val getFilteredChannels: () -> List<Channel>,
    private val onChannelSelected: (Channel) -> Unit
) {
    private val _channelJump = MutableStateFlow<ChannelJump?>(null)
    val channelJump: StateFlow<ChannelJump?> = _channelJump.asStateFlow()

    private var jumpCommitJob: Job? = null

    companion object {
        const val JUMP_COMMIT_DELAY_MS = 1_400L
        const val MAX_JUMP_DIGITS = 4
    }

    fun onNumericKey(digit: Int) {
        if (digit !in 0..9) return
        val current = _channelJump.value?.digits.orEmpty()
        if (current.length >= MAX_JUMP_DIGITS) return
        setJump(current + digit)
    }

    fun onNumericBackspace() {
        val shorter = _channelJump.value?.digits?.dropLast(1) ?: return
        if (shorter.isEmpty()) cancelChannelJump() else setJump(shorter)
    }

    private fun setJump(digits: String) {
        _channelJump.value = ChannelJump(digits = digits, target = jumpTargetFor(digits))
        jumpCommitJob?.cancel()
        jumpCommitJob = coroutineScope.launch {
            delay(JUMP_COMMIT_DELAY_MS)
            commitChannelJump()
        }
    }

    fun commitChannelJump() {
        jumpCommitJob?.cancel()
        val target = _channelJump.value?.target
        _channelJump.value = null
        if (target != null) onChannelSelected(target)
    }

    fun cancelChannelJump() {
        jumpCommitJob?.cancel()
        _channelJump.value = null
    }

    private fun jumpTargetFor(digits: String): Channel? {
        val number = digits.toIntOrNull() ?: return null
        if (number < 1) return null
        return getFilteredChannels().getOrNull(number - 1)
    }
}
