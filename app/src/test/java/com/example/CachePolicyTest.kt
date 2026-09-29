package com.example

import com.example.kurdishtv.repository.shouldReplaceCache
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule that stops a bad refresh from becoming a long one.
 *
 * The failure this prevents is invisible and persistent at once: a refresh that
 * loses a source writes the smaller list to disk, the next cold start reads that
 * cache as fresh, and nothing prompts a refetch — so a two-second network blip
 * becomes thirty minutes of missing channels that the viewer has no way to
 * diagnose.
 *
 * The rule is one comparison, which is exactly why it needs a test. A predicate
 * this small gets "tidied" — inverted, flipped to `>`, changed to compare the
 * wrong pair — and nothing else in the build would notice. Its correctness is
 * not visible in any screenshot.
 */
class CachePolicyTest {

    @Test
    fun `a partial merge does not replace a richer cache`() {
        // The case the rule exists for. 1000 channels on disk, this refresh
        // managed 200 because two of the four sources failed.
        assertFalse(
            "a merge that lost channels must not overwrite the cache",
            shouldReplaceCache(mergedSize = 200, cachedSize = 1000)
        )
    }

    @Test
    fun `a full or better merge replaces the cache`() {
        assertTrue("a larger merge is a real update", shouldReplaceCache(1200, 1000))
    }

    @Test
    fun `an identical merge replaces the cache`() {
        // Deliberately allowed. The channels may well be different even at the
        // same count, and a tie is not evidence of a regression.
        assertTrue(shouldReplaceCache(1000, 1000))
    }

    @Test
    fun `an empty cache is always replaced`() {
        // First run: nothing on disk, so there is nothing to protect.
        assertTrue(shouldReplaceCache(mergedSize = 0, cachedSize = 0))
        assertTrue(shouldReplaceCache(mergedSize = 1, cachedSize = 0))
    }

    @Test
    fun `the bundled catalogue alone never shrinks a merged cache`() {
        // A total loss of every remote source still yields the offline
        // catalogue, so the floor is well above zero and the guard engages
        // rather than being silently bypassed.
        val catalogue = 98
        assertFalse(
            "losing every source must not shrink the cache",
            shouldReplaceCache(mergedSize = catalogue, cachedSize = 1000)
        )
    }

    @Test
    fun `the comparison is not inverted`() {
        // A single assertion that fails loudly if someone ever swaps the two
        // arguments' meaning, which is the realistic way this goes wrong.
        assertTrue(shouldReplaceCache(10, 9))
        assertFalse(shouldReplaceCache(9, 10))
    }
}
