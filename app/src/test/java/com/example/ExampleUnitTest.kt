package com.example

import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.ChannelFilterEngine
import com.example.kurdishtv.parser.KurdishTvParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testChannelFilterEngineCategories() {
        val sampleChannels = listOf(
            Channel(id = "1", name = "Rudaw TV", streamUrl = "https://example.com/1", category = "News", quality = "1080p", isHd = true),
            Channel(id = "2", name = "Vin TV", streamUrl = "https://example.com/2", category = "Music", quality = "720p", isHd = false),
            Channel(id = "3", name = "Zarok TV", streamUrl = "https://example.com/3", category = "Kids", quality = "720p", isHd = false)
        )

        val newsFiltered = ChannelFilterEngine.filter(sampleChannels, CategoryFilter.NEWS, "")
        assertEquals(1, newsFiltered.size)
        assertEquals("Rudaw TV", newsFiltered[0].name)

        val searchFiltered = ChannelFilterEngine.filter(sampleChannels, CategoryFilter.ALL, "vin")
        assertEquals(1, searchFiltered.size)
        assertEquals("Vin TV", searchFiltered[0].name)

        val hdFiltered = ChannelFilterEngine.filter(sampleChannels, CategoryFilter.HD, "")
        assertEquals(1, hdFiltered.size)
        assertEquals("Rudaw TV", hdFiltered[0].name)
    }

    @Test
    fun testParserDeduplication() {
        val list = listOf(
            Channel(id = "1", name = "Rudaw TV", streamUrl = "https://example.com/live.m3u8", category = "News", quality = "1080p", isHd = true),
            Channel(id = "2", name = "Rudaw Duplicate", streamUrl = "https://example.com/live.m3u8", category = "News", quality = "1080p", isHd = true)
        )
        val deduplicated = KurdishTvParser.deduplicate(list)
        assertEquals(1, deduplicated.size)
        assertEquals("Rudaw TV", deduplicated[0].name)
    }
}
