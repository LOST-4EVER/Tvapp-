package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.kurdishtv.parser.KurdishTvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
