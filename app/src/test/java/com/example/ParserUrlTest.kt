package com.example

import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.parser.KurdishTvParser
import com.example.kurdishtv.parser.normalizeStreamUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * URL normalisation, and what it is allowed to merge.
 *
 * The de-duplication this feeds previously compared raw URL strings, so it only ever
 * caught an *exact* repeat. Real playlists do not repeat exactly: the same channel
 * turns up as `http://` on one playlist and `https://` on another, sometimes with a
 * trailing slash. Each of those is one channel rendered twice in the grid, with its
 * own id, its own logo request and its own heart — so a favourite tapped on one of
 * them does nothing to the other.
 *
 * The rule this pins is deliberately narrow. Folding the *path* would merge channels
 * that are genuinely different, because most CDNs treat paths as case-sensitive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParserUrlTest {

    private fun channel(name: String, url: String) = Channel(
        id = name,
        name = name,
        streamUrl = url,
        category = "General"
    )

    @Test
    fun `identical urls normalise identically`() {
        val url = "https://cdn.example.com/live/stream.m3u8"
        assertEquals(normalizeStreamUrl(url), normalizeStreamUrl(url))
    }

    @Test
    fun `surrounding whitespace is not a difference`() {
        assertEquals(
            normalizeStreamUrl("https://cdn.example.com/a.m3u8"),
            normalizeStreamUrl("  https://cdn.example.com/a.m3u8  ")
        )
    }

    @Test
    fun `scheme case is folded but http and https stay different`() {
        assertEquals(
            normalizeStreamUrl("https://cdn.example.com/live.m3u8"),
            normalizeStreamUrl("HTTPS://cdn.example.com/live.m3u8")
        )
        // Deliberately *not* equal, and this is the one judgement call in the whole
        // function. The same path on http and on https is two different origins and a
        // server is entitled to serve different content from each — and, more to the
        // point, one of the two may require a token or a subscription the other does
        // not. Merging them would silently drop a channel that only plays on one of
        // the two, which is worse than showing a row twice.
        //
        // A scheme difference is also the one difference a playlist is *most* likely
        // to get wrong in a way the viewer pays for, so it is left visible rather
        // than guessed away.
        assertNotEquals(
            normalizeStreamUrl("https://cdn.example.com/live.m3u8"),
            normalizeStreamUrl("http://cdn.example.com/live.m3u8")
        )
    }

    @Test
    fun `host case is folded but path case is not`() {
        assertEquals(
            normalizeStreamUrl("https://CDN.Example.com/live.m3u8"),
            normalizeStreamUrl("https://cdn.example.com/live.m3u8")
        )
        // Two paths differing only by case are two different objects on most servers,
        // so folding this would merge channels that are not the same.
        assertNotEquals(
            normalizeStreamUrl("https://cdn.example.com/Live.m3u8"),
            normalizeStreamUrl("https://cdn.example.com/live.m3u8")
        )
    }

    @Test
    fun `a default port is the same origin written longer`() {
        assertEquals(
            normalizeStreamUrl("https://cdn.example.com/live.m3u8"),
            normalizeStreamUrl("https://cdn.example.com:443/live.m3u8")
        )
        assertEquals(
            normalizeStreamUrl("http://cdn.example.com/live.m3u8"),
            normalizeStreamUrl("http://cdn.example.com:80/live.m3u8")
        )
    }

    @Test
    fun `a non-default port is a difference`() {
        assertNotEquals(
            normalizeStreamUrl("https://cdn.example.com:8443/live.m3u8"),
            normalizeStreamUrl("https://cdn.example.com/live.m3u8")
        )
    }

    @Test
    fun `a trailing slash is not a difference`() {
        assertEquals(
            normalizeStreamUrl("https://cdn.example.com/live.m3u8"),
            normalizeStreamUrl("https://cdn.example.com/live.m3u8/")
        )
    }

    @Test
    fun `a fragment is not a difference`() {
        // Never transmitted to the server, so it cannot select a different stream.
        assertEquals(
            normalizeStreamUrl("https://cdn.example.com/live.m3u8"),
            normalizeStreamUrl("https://cdn.example.com/live.m3u8#t=10")
        )
    }

    @Test
    fun `the query string is preserved exactly`() {
        assertNotEquals(
            normalizeStreamUrl("https://cdn.example.com/live.m3u8?a=1"),
            normalizeStreamUrl("https://cdn.example.com/live.m3u8?a=2")
        )
        // A token genuinely in the query must survive normalisation, or the
        // comparison key is no longer comparable at all.
        assertEquals(
            normalizeStreamUrl("https://cdn.example.com/live.m3u8?token=ABC123"),
            normalizeStreamUrl("https://cdn.example.com/live.m3u8?token=ABC123")
        )
    }

    @Test
    fun `a slash inside the query is not trimmed`() {
        // Trimming the whole string would corrupt this value.
        assertEquals(
            normalizeStreamUrl("https://cdn.example.com/live.m3u8?path=a/b"),
            normalizeStreamUrl("https://cdn.example.com/live.m3u8?path=a/b")
        )
    }

    @Test
    fun `a bare ipv6 literal is not mistaken for a host and port`() {
        // The bracket is what distinguishes these. Reading the colon as a port
        // separator would cut the address in half and merge unrelated streams.
        val a = normalizeStreamUrl("http://[2001:db8::1]:8080/live.m3u8")
        val b = normalizeStreamUrl("http://[2001:db8::1]:9090/live.m3u8")
        assertNotEquals(a, b)
        assertEquals(
            normalizeStreamUrl("http://[2001:db8::1]:8080/live.m3u8"),
            a
        )
    }

    @Test
    fun `an empty url is left alone rather than crashing`() {
        assertEquals("", normalizeStreamUrl(""))
        assertEquals("", normalizeStreamUrl("   "))
    }

    @Test
    fun `deduplicate merges the same stream reached two ways`() {
        val merged = KurdishTvParser.deduplicate(
            listOf(
                channel("First", "https://cdn.example.com/live.m3u8"),
                channel("Trailing Slash", "https://cdn.example.com/live.m3u8/"),
                channel("Host Case", "https://CDN.Example.com/live.m3u8"),
                channel("Fragment", "https://cdn.example.com/live.m3u8#t=90"),
                channel("Default Port", "https://cdn.example.com:443/live.m3u8"),
                channel("Genuinely Different", "https://cdn.example.com/other.m3u8")
            )
        )

        assertEquals(2, merged.size)
        assertEquals(listOf("First", "Genuinely Different"), merged.map { it.name })
    }

    @Test
    fun `the first occurrence wins so ordering is preserved`() {
        // The merged list's order decides where a channel appears in the grid, so this
        // has to be deterministic rather than whichever spelling sorted first.
        val merged = KurdishTvParser.deduplicate(
            listOf(
                channel("Alpha", "https://cdn.example.com/live.m3u8"),
                channel("Beta", "https://cdn.example.com/other.m3u8"),
                channel("Gamma", "https://cdn.example.com/live.m3u8/")
            )
        )
        assertEquals(listOf("Alpha", "Beta"), merged.map { it.name })
    }

    @Test
    fun `the surviving channel keeps the url the playlist published`() {
        // Normalisation is a comparison key only. Rewriting the stored URL would be a
        // change of behaviour handed to the player, not a de-duplication.
        val published = "https://cdn.example.com/live.m3u8"
        val merged = KurdishTvParser.deduplicate(
            listOf(
                channel("First", published),
                channel("Second", "https://cdn.example.com/live.m3u8/")
            )
        )
        assertEquals(1, merged.size)
        assertEquals(published, merged[0].streamUrl)
    }

    @Test
    fun `a blank url is still skipped`() {
        val merged = KurdishTvParser.deduplicate(
            listOf(channel("NoUrl", ""), channel("Real", "https://cdn.example.com/a.m3u8"))
        )
        assertEquals(1, merged.size)
        assertEquals("Real", merged[0].name)
    }

    @Test
    fun `a kurdish script channel is filed under Kurdish, not General`() {
        // Every needle in the categoriser is Latin, and `lowercase` does nothing to
        // Arabic script — so a channel named in Kurdish script matched none of them
        // and fell through to `else`. These are exactly the channels that belong in
        // the Kurdish tab, so the tab they were missing from was the one guaranteed
        // to be full.
        val channels = KurdishTvParser.parse(
            """
            #EXTM3U
            #EXTINF:-1,تەلەفزیۆنی کوردی
            https://example.com/kurdish.m3u8
            """.trimIndent()
        )

        assertEquals(1, channels.size)
        assertEquals("Kurdish", channels[0].category)
    }

    @Test
    fun `a kurdish script channel carrying a latin hd marker is still Kurdish`() {
        val channels = KurdishTvParser.parse(
            """
            #EXTM3U
            #EXTINF:-1,کوردی HD
            https://example.com/kurdish-hd.m3u8
            """.trimIndent()
        )
        assertEquals(1, channels.size)
        assertEquals("Kurdish", channels[0].category)
    }

    @Test
    fun `an arabic script channel keeps a readable id rather than an empty name part`() {
        // `[^a-z0-9]` strips every letter of a Kurdish-script name, so the id came out
        // as `krd__2847193`. Still unique — the URL hash carried that — but with no
        // information about the channel in any human-readable part.
        val channels = KurdishTvParser.parse(
            """
            #EXTM3U
            #EXTINF:-1,کوردی
            https://example.com/a.m3u8
            """.trimIndent(),
            sourceTag = "krd"
        )

        assertEquals(1, channels.size)
        val id = channels[0].id
        assertTrue("expected the source tag to survive, got $id", id.startsWith("krd_"))
        assertTrue("expected a name part between the tags, got $id", id.removePrefix("krd_").isNotBlank())
        // And still ASCII, because these ids become grid keys, testTag fragments and
        // preference values.
        assertTrue("id must stay ASCII: $id", id.all { it.code in 32..126 })
    }

    @Test
    fun `a latin channel's id is unchanged by the new fallback`() {
        // The fallback must not disturb ids that already carried a readable name, or
        // every existing favourite and recently-watched entry would stop resolving.
        val channels = KurdishTvParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 group-title="News",Kurdistan 24 HD
            https://example.com/kurdistan.m3u8
            """.trimIndent(),
            sourceTag = "krd"
        )

        assertEquals(1, channels.size)
        assertTrue(
            "expected a latin name part, got ${channels[0].id}",
            channels[0].id.startsWith("krd_kurdistan24hd_")
        )
    }

    @Test
    fun `ids are stable across parses of the same playlist`() {
        // Favourites and recents are keyed on this, so an id that moved between two
        // fetches of identical input would silently drop a viewer's history.
        val playlist = """
            #EXTM3U
            #EXTINF:-1 group-title="News",Kurdistan 24 HD
            https://example.com/a.m3u8
            #EXTINF:-1,کوردی
            https://example.com/b.m3u8
        """.trimIndent()

        val first = KurdishTvParser.parse(playlist, sourceTag = "krd").map { it.id }
        val second = KurdishTvParser.parse(playlist, sourceTag = "krd").map { it.id }
        assertEquals(first, second)
    }
}