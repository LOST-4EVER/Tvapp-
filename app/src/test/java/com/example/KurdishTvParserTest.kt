package com.example

import com.example.kurdishtv.parser.KurdishTvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KurdishTvParserTest {

    @Test
    fun testParseLineByLineFormat() {
        val rawInput = """
            rudaw-tv;https://svs.itworkscdn.net/rudawlive/rudawlive.smil/playlist.m3u8
            kurdistan24-hd;https://live.kurdistan24.net/k24live/playlist.m3u8
        """.trimIndent()

        val channels = KurdishTvParser.parse(rawInput)

        assertEquals(2, channels.size)
        assertEquals("Rudaw TV", channels[0].name)
        assertEquals("https://svs.itworkscdn.net/rudawlive/rudawlive.smil/playlist.m3u8", channels[0].streamUrl)
        assertEquals("Kurdistan 24 HD", channels[1].name)
        assertEquals("News", channels[0].category)
    }

    @Test
    fun testParseM3uFormat() {
        val m3uInput = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://rudaw.net/logo.png" group-title="News", Rudaw TV
            https://svs.itworkscdn.net/rudawlive/rudawlive.smil/playlist.m3u8
        """.trimIndent()

        val channels = KurdishTvParser.parse(m3uInput)

        assertEquals(1, channels.size)
        assertEquals("Rudaw TV", channels[0].name)
        assertEquals("https://rudaw.net/logo.png", channels[0].logoUrl)
        assertEquals("News", channels[0].category)
    }

    @Test
    fun testParseJsonFormat() {
        val jsonInput = """
            [
              {
                "id": "ava_tv",
                "name": "AVA TV",
                "category": "KRD News",
                "url": "https://ava2.store/upload/ava.m3u8",
                "logo": "https://ava.store/logo.png"
              }
            ]
        """.trimIndent()

        val channels = KurdishTvParser.parseJson(jsonInput)

        assertEquals(1, channels.size)
        assertEquals("AVA TV", channels[0].name)
        assertEquals("https://ava2.store/upload/ava.m3u8", channels[0].streamUrl)
        assertEquals("News", channels[0].category)
        assertEquals("https://ava.store/logo.png", channels[0].logoUrl)
    }

    @Test
    fun testExtNameSplitsOnFirstUnquotedComma() {
        // A group title containing a comma. The old parser split on the *last*
        // comma, so this produced an empty display name and fell back to
        // "Kurdish Channel".
        val withCommaInGroup = """
            #EXTM3U
            #EXTINF:-1 group-title="News, Culture",NRT Arabic
            https://example.com/nrt.m3u8
        """.trimIndent()

        val channels = KurdishTvParser.parse(withCommaInGroup)
        assertEquals(1, channels.size)
        assertEquals("NRT Arabic", channels[0].name)
        // The category is still read out of the quoted group title.
        assertEquals("News", channels[0].category)
    }

    @Test
    fun testExtNameKeepsCommasInsideTheDisplayName() {
        // A display name containing a comma. Splitting on the last comma reduced
        // this to "HD".
        val withCommaInName = """
            #EXTM3U
            #EXTINF:-1 group-title="Kurdish",Kurdistan, HD
            https://example.com/kurdistan.m3u8
        """.trimIndent()

        val channels = KurdishTvParser.parse(withCommaInName)
        assertEquals(1, channels.size)
        assertEquals("Kurdistan, HD", channels[0].name)
        assertTrue(channels[0].isHd)
    }

    @Test
    fun testSemicolonLineDoesNotLeakTheNextExtinfName() {
        // A "name;url" entry used to leave the pending #EXTINF in place, so the
        // bare URL on the following line inherited the previous entry's name and
        // logo.
        val mixed = """
            #EXTM3U
            #EXTINF:-1 group-title="News",KurdMax Show
            first;https://example.com/first.m3u8
            https://example.com/second.m3u8
        """.trimIndent()

        val channels = KurdishTvParser.parse(mixed)
        assertEquals(2, channels.size)
        assertEquals("First", channels[0].name)
        assertEquals("Kurdish Channel 2", channels[1].name)
    }

    @Test
    fun testDynamicNameFormatting() {
        assertEquals("Rudaw TV", KurdishTvParser.formatChannelName("rudaw-tv"))
        assertEquals("Kurdistan 24 HD", KurdishTvParser.formatChannelName("kurdistan24-hd"))
        assertEquals("KurdSat News", KurdishTvParser.formatChannelName("kurdsat_news"))
        assertEquals("KurdMax Show", KurdishTvParser.formatChannelName("kurdmax-show"))
    }

    @Test
    fun testFallbackChannels() {
        val fallbacks = KurdishTvParser.getFallbackChannels()
        assertTrue(fallbacks.isNotEmpty())
        assertNotNull(fallbacks.find { it.name == "Rudaw TV" })
    }
}
