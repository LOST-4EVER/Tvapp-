package com.example

import com.example.kurdishtv.ui.keys.RemoteKeyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The remote's auto-repeat policy, pinned.
 *
 * This is the part of the D-pad that is pure arithmetic, and it is the part that was
 * wrong. The browse screen and the player both filtered `KeyEventType.KeyUp` on the
 * reasoning that a held key is "the same physical press reported again" — but
 * auto-repeat arrives as *extra Key Down events* with a growing `repeatCount`, so that
 * filter never saw one. Holding `4` entered `4444`.
 *
 * Two rules are therefore load-bearing, and they are opposites:
 *
 *  - a key that **builds a value** must ignore repeats entirely, or one press becomes
 *    four digits and the viewer lands on a channel they never typed;
 *  - a key that **moves a selection** must *accept* repeats, because holding CH+ is
 *    how anyone scrolls a list they cannot see — but rate-limited, since the platform
 *    repeats around thirty times a second.
 */
class RemoteKeyPolicyTest {

    @Test
    fun `only the initial press is not a repeat`() {
        assertFalse(RemoteKeyPolicy.isAutoRepeat(0))
        assertTrue(RemoteKeyPolicy.isAutoRepeat(1))
        assertTrue(RemoteKeyPolicy.isAutoRepeat(2))
        // Auto-repeat counts up for as long as the key is held. A remote held down
        // for a minute gets into the hundreds, so this must not be a `== 1` check.
        assertTrue(RemoteKeyPolicy.isAutoRepeat(347))
    }

    @Test
    fun `the first step of a held key is always accepted`() {
        // No previous step has been taken, so there is nothing to be too soon after.
        assertTrue(RemoteKeyPolicy.acceptsChannelStep(nowMs = 0L, lastStepAtMs = null))
        assertTrue(RemoteKeyPolicy.acceptsChannelStep(nowMs = 12_345L, lastStepAtMs = null))
    }

    @Test
    fun `a repeat inside the interval is dropped`() {
        // Android's repeat rate is far faster than the step interval, so without the
        // limit a held CH+ crosses the whole catalogue before the viewer reacts.
        assertFalse(RemoteKeyPolicy.acceptsChannelStep(nowMs = 40L, lastStepAtMs = 0L))
        assertFalse(
            RemoteKeyPolicy.acceptsChannelStep(
                nowMs = RemoteKeyPolicy.CHANNEL_STEP_MIN_INTERVAL_MS - 1L,
                lastStepAtMs = 0L
            )
        )
    }

    @Test
    fun `a repeat at or past the interval is accepted`() {
        assertTrue(
            RemoteKeyPolicy.acceptsChannelStep(
                nowMs = RemoteKeyPolicy.CHANNEL_STEP_MIN_INTERVAL_MS,
                lastStepAtMs = 0L
            )
        )
        assertTrue(
            RemoteKeyPolicy.acceptsChannelStep(
                nowMs = 5_000L,
                lastStepAtMs = 1_000L
            )
        )
    }

    @Test
    fun `the step interval steps about four channels a second`() {
        // Roughly what a television set does when CH+ is held. Faster and the viewer
        // cannot see a channel go by; slower and walking a screenful is a chore.
        val stepsPerSecond = 1_000L / RemoteKeyPolicy.CHANNEL_STEP_MIN_INTERVAL_MS
        assertEquals(4L, stepsPerSecond)
    }

    @Test
    fun `a clock that goes backwards does not lock the key out`() {
        // `SystemClock.uptimeMillis` is monotonic within a boot, so this should not
        // arise — but the failure modes are not symmetric. Refusing here would wedge
        // CH+ until the key was released and pressed again, and the viewer would have
        // no way to know why the key they are holding had stopped working.
        assertTrue(RemoteKeyPolicy.acceptsChannelStep(nowMs = 100L, lastStepAtMs = 500L))
        assertTrue(RemoteKeyPolicy.acceptsChannelStep(nowMs = 500L, lastStepAtMs = 500L))
    }
}
