package com.example

import com.example.kurdishtv.network.FastCachingDns
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The DNS cache's freshness rule.
 *
 * This is the part that was wrong in a way nothing at the call site could reveal. The
 * cache stamped entries with the wall clock, and a wall clock that moves backwards —
 * which NITZ does on a TV box, and a user does from the system settings — makes every
 * cached entry compute a *negative* age. A negative age passes any `age < ttl` test,
 * so the whole cache pins itself as fresh until the clock catches up, which on a box
 * whose time arrives after boot can be most of a session.
 *
 * The rule is a free function so both directions can be pinned without a network, a
 * socket or a real clock.
 */
class DnsEntryTest {

    private val ttl = 5_000L

    @Test
    fun `an entry just stamped is fresh`() {
        assertTrue(FastCachingDns.isEntryFresh(nowNanos = 1_000L, storedAtNanos = 1_000L, ttlNanos = ttl))
    }

    @Test
    fun `an entry within the ttl is still fresh`() {
        assertTrue(FastCachingDns.isEntryFresh(nowNanos = 1_000L + ttl - 1, storedAtNanos = 1_000L, ttlNanos = ttl))
    }

    @Test
    fun `an entry exactly at the ttl has expired`() {
        // The boundary is exclusive. An entry is usable for strictly less than the
        // ttl, so a lookup landing exactly on the boundary refetches rather than
        // serving an address that is precisely of age.
        assertFalse(FastCachingDns.isEntryFresh(nowNanos = 1_000L + ttl, storedAtNanos = 1_000L, ttlNanos = ttl))
    }

    @Test
    fun `an entry well past the ttl has expired`() {
        assertFalse(FastCachingDns.isEntryFresh(nowNanos = 1_000L + ttl * 100, storedAtNanos = 1_000L, ttlNanos = ttl))
    }

    @Test
    fun `an entry that claims to be from the future is stale, not fresh`() {
        // This is the regression the monotonic stamp exists to prevent. Stored 10s
        // *after* now, which is what a wall clock moving backwards produces.
        assertFalse(FastCachingDns.isEntryFresh(nowNanos = 1_000L, storedAtNanos = 1_000L + 10_000L, ttlNanos = ttl))
    }

    @Test
    fun `a clock correction does not resurrect an arbitrarily old entry`() {
        // A large backward correction must not be able to make a genuinely ancient
        // record look fresh, however it is arranged.
        val veryOld = 1L
        val corrected = veryOld + 3_600L * 1_000_000_000L // an hour of wall clock moved backwards
        assertFalse(FastCachingDns.isEntryFresh(nowNanos = veryOld, storedAtNanos = corrected, ttlNanos = ttl))
    }

    @Test
    fun `a non-positive ttl disables caching entirely`() {
        // A legitimate configuration, and the safe reading of it: every entry is
        // treated as expired rather than as permanently fresh.
        assertFalse(FastCachingDns.isEntryFresh(nowNanos = 1_000L, storedAtNanos = 1_000L, ttlNanos = 0L))
        assertFalse(FastCachingDns.isEntryFresh(nowNanos = 1_000L, storedAtNanos = 1_000L, ttlNanos = -1L))
    }

    @Test
    fun `the rule survives the extremes of the clock`() {
        // nanoTime is monotonic and unbounded in practice, but the arithmetic is
        // plain Long subtraction, so the edges are worth pinning.
        assertTrue(
            FastCachingDns.isEntryFresh(
                nowNanos = Long.MAX_VALUE,
                storedAtNanos = Long.MAX_VALUE - 1L,
                ttlNanos = ttl
            )
        )
        assertFalse(
            FastCachingDns.isEntryFresh(
                nowNanos = Long.MIN_VALUE,
                storedAtNanos = 0L,
                ttlNanos = ttl
            )
        )
    }
}