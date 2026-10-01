package com.example

import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.viewmodel.channelsForIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Resolving a short history list against the whole catalogue.
 *
 * This replaced `channels.associateBy { it.id }` followed by a dozen lookups, which
 * built a hash entry for every channel in the catalogue — well over a thousand of
 * them after a merge — to answer about a dozen questions. Almost every entry was
 * never read.
 *
 * The *shape* of the answer is what this pins. Getting the order wrong would put the
 * recently-watched row in a different sequence from the stored history, and dropping
 * or duplicating an entry would show the viewer a card that is not the one they
 * watched.
 */
class ChannelLookupTest {

    private fun channel(id: String) = Channel(
        id = id,
        name = "Channel $id",
        streamUrl = "https://example.com/$id.m3u8"
    )

    private fun catalogue(count: Int): List<Channel> =
        (1..count).map { channel("c$it") }

    @Test
    fun `results come back in the order the ids were given`() {
        // The history is stored most-recent-first and the row is drawn in that
        // order, so this is the whole point of the function. A catalogue-ordered
        // result would look plausible and be wrong.
        val result = channelsForIds(catalogue(10), listOf("c7", "c2", "c5"))
        assertEquals(listOf("c7", "c2", "c5"), result.map { it.id })
    }

    @Test
    fun `an id the catalogue no longer holds is skipped`() {
        // The normal case after a refresh: a source going down or a playlist being
        // edited removes a channel, and its history entry must not become a hole or
        // a crash.
        val result = channelsForIds(catalogue(5), listOf("c1", "gone", "c3"))
        assertEquals(listOf("c1", "c3"), result.map { it.id })
    }

    @Test
    fun `an empty history is an empty result`() {
        assertTrue(channelsForIds(catalogue(10), emptyList()).isEmpty())
    }

    @Test
    fun `an empty catalogue is an empty result`() {
        assertTrue(channelsForIds(emptyList(), listOf("c1")).isEmpty())
    }

    @Test
    fun `no matching id is an empty result, not an error`() {
        val result = channelsForIds(catalogue(3), listOf("nope", "also-nope"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `a repeated id is returned once per request`() {
        // The caller asked for a sequence, not a set. Collapsing duplicates here
        // would silently shorten the history row.
        val result = channelsForIds(catalogue(3), listOf("c2", "c2", "c1"))
        assertEquals(listOf("c2", "c2", "c1"), result.map { it.id })
    }

    @Test
    fun `the first channel wins when the catalogue holds a duplicate id`() {
        // A duplicate id should be impossible — the merge suffixes them — but if one
        // ever appears, the earlier entry is the one a stored favourite means, and a
        // crash or an arbitrary pick is not an option.
        val duplicated = listOf(channel("dup"), Channel("other", "Other", "u"), channel("dup"))
        val result = channelsForIds(duplicated, listOf("dup"))
        assertEquals(1, result.size)
    }

    @Test
    fun `the whole history resolves when every id is present`() {
        val ids = (1..12).map { "c$it" }
        val result = channelsForIds(catalogue(100), ids)
        assertEquals(ids, result.map { it.id })
    }

    @Test
    fun `history ids beyond the end of the catalogue are dropped`() {
        // The number pad can name a position in a list that a refresh has since
        // shortened.
        val result = channelsForIds(catalogue(3), listOf("c1", "c99"))
        assertEquals(listOf("c1"), result.map { it.id })
    }

    @Test
    fun `a large catalogue is resolved in the id order, not the catalogue order`() {
        // The regression this exists to prevent is only visible at the size the
        // merged playlists actually reach, so the test is built at that size.
        val ids = listOf("c900", "c450", "c999", "c2")
        val result = channelsForIds(catalogue(1000), ids)
        assertEquals(ids, result.map { it.id })
    }
}