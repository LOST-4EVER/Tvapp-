package com.example

import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.ChannelFilterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Search behaviour that a viewer would describe as "search is broken".
 *
 * The parser rewrites every `-` and `_` in a playlist's raw channel name into a
 * space, so a channel shown as `NRT 1` is stored under exactly that string. These
 * tests pin the matching that makes the name a viewer *knows* still find it.
 */
class ChannelFilterEngineTest {

    private fun channel(name: String, category: String = "News") = Channel(
        id = "id_$name",
        name = name,
        streamUrl = "https://example.invalid/$name.m3u8",
        category = category
    )

    private val channels = listOf(
        channel("NRT 1"),
        channel("Kurdistan 24"),
        channel("Riya Salih TV"),
        channel("MTV HD", category = "General")
    )

    private fun search(query: String) =
        ChannelFilterEngine.filter(channels, CategoryFilter.ALL, query).map { it.name }

    @Test
    fun `an empty query returns everything`() {
        assertEquals(channels.size, search("").size)
        assertEquals(channels.size, search("   ").size)
    }

    @Test
    fun `a hyphen in the query still finds a channel stored with a space`() {
        // "NRT-1" is how the channel is written in most community playlists.
        assertEquals(listOf("NRT 1"), search("NRT-1"))
    }

    @Test
    fun `an underscore in the query still finds a channel stored with a space`() {
        assertEquals(listOf("NRT 1"), search("NRT_1"))
    }

    @Test
    fun `dropping the space entirely still finds the channel`() {
        // The commoner direction: people type "nrt1" and the stored name has a space.
        assertEquals(listOf("NRT 1"), search("nrt1"))
    }

    @Test
    fun `matching is case insensitive`() {
        assertEquals(listOf("NRT 1"), search("nRt 1"))
    }

    @Test
    fun `a plain substring still matches across a multi word name`() {
        assertEquals(listOf("Kurdistan 24"), search("kurdistan"))
    }

    @Test
    fun `the category is searchable too`() {
        assertEquals(listOf("MTV HD"), search("general"))
    }

    @Test
    fun `a query that matches nothing returns an empty list`() {
        assertTrue(search("zzzzz").isEmpty())
    }

    @Test
    fun `punctuation alone is not a query`() {
        // Normalising "---" must not leave a non-empty needle that matches everything.
        assertEquals(channels.size, search("---").size)
    }

    @Test
    fun `the category filter and the query apply together`() {
        val news = ChannelFilterEngine.filter(channels, CategoryFilter.NEWS, "nrt")
        assertEquals(listOf("NRT 1"), news.map { it.name })
    }

    @Test
    fun `a query cannot match across the join between name and category`() {
        // The search key is the normalised name and category in one string, so it
        // has to carry a separator a query can never contain — otherwise "1g" would
        // match the "1" at the end of "NRT 1" followed by the "G" of "General" and
        // hand back a channel that has nothing to do with the query.
        val joined = listOf(channel("NRT 1", category = "General"))
        assertTrue(ChannelFilterEngine.filter(joined, CategoryFilter.ALL, "1g").isEmpty())
        // ...while both halves are still individually searchable.
        assertEquals(1, ChannelFilterEngine.filter(joined, CategoryFilter.ALL, "nrt").size)
        assertEquals(1, ChannelFilterEngine.filter(joined, CategoryFilter.ALL, "general").size)
    }

    @Test
    fun `counts agree with filtering for every category`() {
        val counts = ChannelFilterEngine.countsByCategory(channels)
        for (filter in CategoryFilter.entries) {
            val shown = ChannelFilterEngine.filter(channels, filter, "").size
            assertEquals(
                "count for $filter disagreed with the filtered list",
                shown,
                counts[filter]
            )
        }
    }
}
