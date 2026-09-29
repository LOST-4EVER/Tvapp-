package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.kurdishtv.data.ChannelCacheStorage
import com.example.kurdishtv.data.CustomPlaylistStorage
import com.example.kurdishtv.data.FavoriteStorage
import com.example.kurdishtv.data.RecentStorage
import com.example.kurdishtv.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * The persistence layer, including the on-disk format migrations.
 *
 * Every test here is about behaviour a user would describe as data loss: a
 * favourite that comes back, a history entry that can never be reached again, a
 * playlist link that vanished because the write failed. The CHANGELOG has
 * several of these already — the comma-delimited recents format that silently
 * split every id, and the favourites write whose result was reported as the
 * intent rather than as what actually reached disk — and the fixes are only
 * worth keeping if something fails when they are undone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StorageTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    // ── Favourites ──────────────────────────────────────────────────────────

    @Test
    fun `toggling a favourite reports the state that is actually stored`() {
        val storage = FavoriteStorage(context)

        assertTrue(storage.toggleFavorite("nrt1"))
        assertTrue(storage.isFavorite("nrt1"))
        assertEquals(setOf("nrt1"), storage.getFavoriteIds())

        assertFalse(storage.toggleFavorite("nrt1"))
        assertFalse(storage.isFavorite("nrt1"))
        assertTrue(storage.getFavoriteIds().isEmpty())
    }

    @Test
    fun `favourites survive a new storage instance`() {
        FavoriteStorage(context).toggleFavorite("kurdistan24")
        // A second instance stands in for the next process: the value has to be
        // on disk, not just in the object that wrote it.
        assertTrue(FavoriteStorage(context).isFavorite("kurdistan24"))
    }

    @Test
    fun `clearing favourites empties the store`() {
        val storage = FavoriteStorage(context)
        storage.toggleFavorite("a")
        storage.toggleFavorite("b")

        assertTrue(storage.clear())
        assertTrue(storage.getFavoriteIds().isEmpty())
    }

    // ── Recently watched ────────────────────────────────────────────────────

    @Test
    fun `the most recent channel is first and a repeat moves rather than appends`() {
        val storage = RecentStorage(context)
        storage.addRecentChannel("first")
        storage.addRecentChannel("second")
        storage.addRecentChannel("first")

        assertEquals(listOf("first", "second"), storage.getRecentChannelIds())
    }

    @Test
    fun `a blank channel id is ignored`() {
        val storage = RecentStorage(context)
        storage.addRecentChannel("   ")
        assertTrue(storage.getRecentChannelIds().isEmpty())
    }

    @Test
    fun `the history is capped`() {
        val storage = RecentStorage(context)
        for (i in 1..30) storage.addRecentChannel("channel$i")

        val ids = storage.getRecentChannelIds()
        assertEquals(12, ids.size)
        // Newest first, so the last one watched is the head of the list.
        assertEquals("channel30", ids.first())
        assertEquals("channel19", ids.last())
    }

    @Test
    fun `adding a channel hands back the history that was written`() {
        // The caller that publishes the "continue watching" row used to read the
        // file straight back to get this, which meant building a JSON array,
        // writing it, reading it and parsing it again on every press of OK. If the
        // returned value ever stops matching what is stored, that optimisation has
        // quietly become a lie and the row would show something that was not
        // saved — so the two are pinned together here.
        val storage = RecentStorage(context)
        assertEquals(listOf("first"), storage.addRecentChannel("first"))
        assertEquals(listOf("second", "first"), storage.addRecentChannel("second"))
        assertEquals(
            storage.getRecentChannelIds(),
            RecentStorage(context).addRecentChannel("first")
        )
    }

    @Test
    fun `an id containing a comma survives a round trip`() {
        // The reason this is stored as JSON at all. A comma delimiter split such
        // an id in half on read, and neither half ever matched a channel again.
        val awkward = "krd_nrt1_a1,2"
        val storage = RecentStorage(context)
        storage.addRecentChannel(awkward)

        assertEquals(listOf(awkward), RecentStorage(context).getRecentChannelIds())
    }

    @Test
    fun `the legacy comma delimited format is migrated on first read`() {
        // Written the way the old release wrote it, straight into the same
        // preference the new code reads.
        val prefs = context.getSharedPreferences("kurdish_tv_recents_v3", Context.MODE_PRIVATE)
        prefs.edit().putString("recent_channel_ids", "alpha,beta,gamma").commit()

        val storage = RecentStorage(context)
        assertEquals(listOf("alpha", "beta", "gamma"), storage.getRecentChannelIds())
    }

    @Test
    fun `migration truncates before rewriting rather than after`() {
        val tooMany = (1..20).joinToString(",") { "id$it" }
        val prefs = context.getSharedPreferences("kurdish_tv_recents_v3", Context.MODE_PRIVATE)
        prefs.edit().putString("recent_channel_ids", tooMany).commit()

        val storage = RecentStorage(context)
        assertEquals(12, storage.getRecentChannelIds().size)
        // The rewrite must already be truncated, so a second read agrees with
        // the first instead of re-truncating on every single call.
        assertEquals(storage.getRecentChannelIds(), RecentStorage(context).getRecentChannelIds())
    }

    @Test
    fun `clearing the history empties the store`() {
        val storage = RecentStorage(context)
        storage.addRecentChannel("a")
        assertTrue(storage.clearRecents())
        assertTrue(RecentStorage(context).getRecentChannelIds().isEmpty())
    }

    // ── Custom playlist links ───────────────────────────────────────────────

    @Test
    fun `a valid playlist link is stored and normalised`() {
        val storage = CustomPlaylistStorage(context)
        assertTrue(storage.addCustomPlaylistUrl("  https://example.com/list.m3u  "))

        // Trailing whitespace is dropped and the value is canonicalised, so what
        // comes back is what a later `remove` will match.
        assertEquals(setOf("https://example.com/list.m3u"), storage.getCustomPlaylistUrls())
    }

    @Test
    fun `an invalid playlist link is refused`() {
        val storage = CustomPlaylistStorage(context)

        assertFalse(storage.addCustomPlaylistUrl(""))
        assertFalse(storage.addCustomPlaylistUrl("   "))
        assertFalse(storage.addCustomPlaylistUrl("not a url"))
        assertFalse(storage.addCustomPlaylistUrl("ftp://example.com/list.m3u"))
        assertFalse(storage.addCustomPlaylistUrl("file:///etc/passwd"))
        assertTrue(storage.getCustomPlaylistUrls().isEmpty())
    }

    @Test
    fun `a link carrying credentials is refused`() {
        // These would otherwise be written into preferences and into the on-disk
        // channel cache, where a shared log or a backup would expose them.
        val storage = CustomPlaylistStorage(context)

        assertFalse(storage.addCustomPlaylistUrl("https://user:pass@example.com/list.m3u"))
        assertFalse(storage.addCustomPlaylistUrl("https://user@example.com/list.m3u"))
        assertTrue(storage.getCustomPlaylistUrls().isEmpty())
    }

    @Test
    fun `an absurdly long link is refused`() {
        val storage = CustomPlaylistStorage(context)
        val tooLong = "https://example.com/" + "a".repeat(2048)
        assertFalse(storage.addCustomPlaylistUrl(tooLong))
    }

    @Test
    fun `the same link cannot be added twice`() {
        val storage = CustomPlaylistStorage(context)
        assertTrue(storage.addCustomPlaylistUrl("https://example.com/list.m3u"))
        assertFalse(storage.addCustomPlaylistUrl("https://example.com/list.m3u"))
        assertEquals(1, storage.getCustomPlaylistUrls().size)
    }

    @Test
    fun `the number of playlist links is capped`() {
        val storage = CustomPlaylistStorage(context)
        repeat(20) { i ->
            assertTrue("link $i should be accepted", storage.addCustomPlaylistUrl("https://example.com/$i.m3u"))
        }
        assertFalse(storage.addCustomPlaylistUrl("https://example.com/one-too-many.m3u"))
        assertEquals(20, storage.getCustomPlaylistUrls().size)
    }

    @Test
    fun `removing a link that is not stored reports that nothing changed`() {
        val storage = CustomPlaylistStorage(context)
        storage.addCustomPlaylistUrl("https://example.com/list.m3u")

        assertFalse(storage.removeCustomPlaylistUrl("https://example.com/absent.m3u"))
        assertTrue(storage.removeCustomPlaylistUrl("https://example.com/list.m3u"))
        assertTrue(storage.getCustomPlaylistUrls().isEmpty())
    }

    // ── The on-disk channel cache ───────────────────────────────────────────

    private fun channel(
        id: String,
        name: String = "Channel $id",
        category: String = "News",
        isHd: Boolean = true,
        isFavorite: Boolean = false
    ) = Channel(
        id = id,
        name = name,
        streamUrl = "https://example.com/$id.m3u8",
        logoUrl = "https://example.com/$id.png",
        category = category,
        quality = "HLS / 1080p",
        isFavorite = isFavorite,
        isHd = isHd,
        originalId = id
    )

    @Test
    fun `channels survive a write and a read`() {
        val storage = ChannelCacheStorage(context)
        val original = listOf(
            channel("a", name = "NRT 1"),
            channel("b", name = "Kurdistan 24", category = "Kurdish", isHd = false)
        )

        storage.saveChannels(original)
        val restored = ChannelCacheStorage(context).getCachedChannels()

        assertNotNull(restored)
        assertEquals(2, restored!!.size)
        assertEquals("NRT 1", restored[0].name)
        assertEquals("Kurdish", restored[1].category)
        assertFalse(restored[1].isHd)
    }

    @Test
    fun `the favourite flag is never written to the cache`() {
        // Favourites live in their own store and are re-applied on every read. A
        // cached flag would short-circuit the fetch that corrects it, so a heart
        // the user had removed could come back from the cache.
        val storage = ChannelCacheStorage(context)
        storage.saveChannels(listOf(channel("a", isFavorite = true)))

        assertFalse(ChannelCacheStorage(context).getCachedChannels()!!.single().isFavorite)
    }

    @Test
    fun `an empty list is not written over a good cache`() {
        val storage = ChannelCacheStorage(context)
        storage.saveChannels(listOf(channel("a")))
        storage.saveChannels(emptyList())

        assertEquals(1, ChannelCacheStorage(context).getCachedChannels()!!.size)
    }

    @Test
    fun `there is no cache before anything is written`() {
        val storage = ChannelCacheStorage(context)
        assertNull(storage.getCachedChannels())
        // Zero now means exactly one thing: nothing is cached.
        assertEquals(0L, storage.getCacheAgeMs())
    }

    @Test
    fun `a cache stamped in the future is reported as stale, not as brand new`() {
        // The failure this prevents is permanent rather than visible. A device
        // whose clock was set forward and then corrected leaves the file stamped
        // in the future, and an age of zero for that file — which is what a
        // backwards subtraction produces — means "written just now". The app then
        // serves that list on every launch, forever, and never asks again.
        val storage = ChannelCacheStorage(context)
        storage.saveChannels(listOf(channel("a")))
        val file = java.io.File(context.filesDir, "cached_kurdish_channels.json")
        assertTrue(
            "could not move the cache into the future",
            file.setLastModified(System.currentTimeMillis() + 3_600_000L)
        )

        val age = ChannelCacheStorage(context).getCacheAgeMs()
        assertTrue(
            "a future-dated cache reported an age of $age ms, which reads as fresh",
            age > TimeUnit.HOURS.toMillis(1)
        )
    }

    @Test
    fun `a freshly written cache reports itself as fresh`() {
        val storage = ChannelCacheStorage(context)
        storage.saveChannels(listOf(channel("a")))

        val age = ChannelCacheStorage(context).getCacheAgeMs()
        assertTrue("freshly written cache reported an age of $age ms", age in 0..60_000L)
    }

    @Test
    fun `clearing the cache removes it`() {
        val storage = ChannelCacheStorage(context)
        storage.saveChannels(listOf(channel("a")))

        assertTrue(storage.clearCache())
        assertNull(ChannelCacheStorage(context).getCachedChannels())
        // Already absent counts as cleared: there is nothing left to remove, and
        // reporting failure would be misleading.
        assertTrue(storage.clearCache())
    }

    @Test
    fun `a corrupt cache file is reported as no cache rather than throwing`() {
        val file = java.io.File(context.filesDir, "cached_kurdish_channels.json")
        file.writeText("{ this is not the json you are looking for")

        val storage = ChannelCacheStorage(context)
        // A truncated or corrupted file must degrade to "no cache" so the app
        // falls through to the network, not crash on every launch.
        assertNull(storage.getCachedChannels())
        // The count has to fail the same way, quietly. It is only ever used to
        // decide whether a merge shrank, and a number read out of a file that does
        // not parse would let a partial merge overwrite the good list.
        assertEquals(0, storage.getCachedChannelCount())
    }

    // ── Counting the cache without parsing it ───────────────────────────────

    @Test
    fun `the count agrees with what a full parse finds`() {
        val storage = ChannelCacheStorage(context)
        val original = (1..40).map { channel("id$it", category = if (it % 2 == 0) "News" else "Music") }
        storage.saveChannels(original)

        val storage2 = ChannelCacheStorage(context)
        assertEquals(storage2.getCachedChannels()!!.size, storage2.getCachedChannelCount())
    }

    @Test
    fun `there is no count before anything is written`() {
        assertEquals(0, ChannelCacheStorage(context).getCachedChannelCount())
    }

    @Test
    fun `the element counter is not fooled by punctuation inside a channel name`() {
        val storage = ChannelCacheStorage(context)
        // The exact shapes that break a scanner which only looks for braces and
        // commas: a closing brace in a name, an embedded quote, an escaped
        // backslash, and a newline. These are all reachable — channel names come
        // from community playlists — and each one used to be able to make the
        // counter disagree with the parser.
        val awkward = listOf(
            channel("a", name = "NRT } 1"),
            channel("b", name = "Say \"hi\" TV"),
            channel("c", name = "back\\slash"),
            channel("d", name = "line\nbreak"),
            channel("e", name = "brace } and { pair"),
            channel("f", name = "array [bracket]")
        )
        storage.saveChannels(awkward)

        val storage2 = ChannelCacheStorage(context)
        val parsed = storage2.getCachedChannels()!!
        assertEquals(awkward.size, parsed.size)
        assertEquals(parsed.size, storage2.getCachedChannelCount())
        assertEquals(listOf("NRT } 1", "Say \"hi\" TV", "back\\slash"), parsed.take(3).map { it.name })
    }

    @Test
    fun `the element counter handles the shapes a hand written file can be in`() {
        val storage = ChannelCacheStorage(context)
        assertEquals(0, storage.countTopLevelElements(""))
        assertEquals(0, storage.countTopLevelElements("[]"))
        assertEquals(0, storage.countTopLevelElements("  [  ]  "))
        assertEquals(1, storage.countTopLevelElements("""[{"a":1}]"""))
        assertEquals(3, storage.countTopLevelElements("""[{"a":1},{"b":2},{"c":3}]"""))
        // A nested array belongs to its element, and must not be counted.
        assertEquals(1, storage.countTopLevelElements("""[{"a":[1,2,3]}]"""))
        // Commas inside strings are not separators.
        assertEquals(2, storage.countTopLevelElements("""[{"a":"x,y"},{"b":"z"}]"""))
        // As are braces and escaped quotes.
        assertEquals(1, storage.countTopLevelElements("""[{"a":"}\""}]"""))
    }
}
