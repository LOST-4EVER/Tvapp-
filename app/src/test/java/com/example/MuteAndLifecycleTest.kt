package com.example

import com.example.kurdishtv.ads.FullPageAdState
import com.example.kurdishtv.ads.StartIoFullPage
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.viewmodel.ChannelJump
import com.example.kurdishtv.viewmodel.SleepTimerState
import com.example.kurdishtv.viewmodel.TvUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MuteAndLifecycleTest {

    @Test
    fun testUiStateMuteDefaultAndToggle() {
        val initial = TvUiState()
        assertFalse("isMuted should be false initially", initial.isMuted)

        val muted = initial.copy(isMuted = true)
        assertTrue("isMuted should be true after toggle", muted.isMuted)

        val unmuted = muted.copy(isMuted = false)
        assertFalse("isMuted should be false after untoggle", unmuted.isMuted)
    }

    @Test
    fun testSleepTimerState() {
        val timer = SleepTimerState(minutes = 15, formattedText = "15:00")
        assertEquals(15, timer.minutes)
        assertEquals("15:00", timer.formattedText)

        val idle = SleepTimerState()
        assertEquals(0, idle.minutes)
        assertNull(idle.formattedText)
    }

    @Test
    fun testChannelJumpModel() {
        val channel = Channel(
            id = "ch_test",
            name = "Test TV",
            streamUrl = "https://example.com/stream.m3u8"
        )
        val jump = ChannelJump(digits = "42", target = channel)
        assertEquals("42", jump.digits)
        assertEquals("Test TV", jump.target?.name)
    }

    @Test
    fun testStartIoFullPageRelease() {
        StartIoFullPage.release()
        assertEquals(FullPageAdState.Unavailable, StartIoFullPage.state)
    }
}
