package com.example

import com.example.kurdishtv.network.retryDelayMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The retry curve.
 *
 * Retry code is easy to write and easy to get quietly wrong in the two
 * directions that matter: a backoff that collapses to nothing defeats the
 * purpose of backing off, and one that does not cap turns a struggling source
 * into a request that is still running long after the viewer has given up.
 *
 * This is a pure function precisely so both can be pinned without a network, a
 * clock, or a real multi-second wait in the test suite.
 */
class RetryPolicyTest {

    @Test
    fun `the delay grows between attempts`() {
        val first = retryDelayMs(attempt = 1, jitter = 0f)
        val second = retryDelayMs(attempt = 2, jitter = 0f)
        val third = retryDelayMs(attempt = 3, jitter = 0f)

        assertTrue("expected growth: $first -> $second -> $third",
            first < second && second < third)
    }

    @Test
    fun `the first retry is never free`() {
        // Jitter is the usual way backoff gets defeated: a pure
        // random(0, delay) can draw almost nothing, and several sources
        // failing together would then all retry at once. Equal jitter
        // guarantees a real wait while still de-correlating the sources.
        for (attempt in 1..6) {
            val lowest = retryDelayMs(attempt, jitter = 0f)
            assertTrue(
                "attempt $attempt backed off by only ${lowest}ms at zero jitter",
                lowest > 0
            )
        }
    }

    @Test
    fun `jitter only ever adds time, and never more than the full delay`() {
        for (attempt in 1..6) {
            val low = retryDelayMs(attempt, jitter = 0f)
            val high = retryDelayMs(attempt, jitter = 1f)

            assertTrue("jitter must not shorten: $low vs $high", low <= high)
            // With equal jitter the range is [delay/2, delay], so the
            // spread is exactly half the delay.
            assertTrue(
                "jitter spread too wide for attempt $attempt: $low..$high",
                high - low <= (high + 1) / 2
            )
        }
    }

    @Test
    fun `jitter is clamped rather than trusted`() {
        // The value comes from Random.nextFloat(), which is already in
        // range, but a policy function should not depend on its caller
        // having behaved.
        val atZero = retryDelayMs(2, jitter = 0f)
        val negative = retryDelayMs(2, jitter = -5f)
        val overOne = retryDelayMs(2, jitter = 7f)

        assertEquals(atZero, negative)
        assertEquals(retryDelayMs(2, jitter = 1f), overOne)
    }

    @Test
    fun `the delay is capped so a struggling source cannot stall the merge`() {
        // A viewer waiting on a merge is waiting on every source at once.
        // An uncapped exponential would let one bad source hold the whole
        // refresh open long past the point of being useful.
        val capped = retryDelayMs(attempt = 50, jitter = 1f)
        val alsoCapped = retryDelayMs(attempt = 12, jitter = 1f)

        assertTrue("a very late attempt waited ${capped}ms", capped <= 8_000L)
        assertEquals("the cap must actually be reached, not approached", capped, alsoCapped)
    }

    @Test
    fun `an absurd attempt number cannot overflow into a negative delay`() {
        // The attempt count is bounded by the caller's MAX_FETCH_ATTEMPTS,
        // but the shift count is not — Int.MIN_VALUE or a huge value would
        // wrap the shift and produce nonsense.
        for (attempt in listOf(0, -1, 31, 32, 64, Int.MAX_VALUE)) {
            val delay = retryDelayMs(attempt, jitter = 1f)
            assertTrue("attempt $attempt produced $delay", delay in 0..8_000L)
        }
    }
}
