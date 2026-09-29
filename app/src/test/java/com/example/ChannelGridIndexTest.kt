package com.example

import com.example.kurdishtv.ui.keys.RemoteGridIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Where a channel sits in the browse grid, pinned.
 *
 * `ChannelGrid` scrolls to a channel and then hands it D-pad focus, and it was doing
 * `scrollToItem(indexIntoTheChannelList)`. A `LazyVerticalGrid` numbers *items*, and
 * the home tab puts two full-width non-channel items in front of the cells — the
 * recents row and the featured hero. So on the home tab the number pad scrolled two
 * rows short, and then focused whatever it had landed on: the viewer typed a channel
 * number, the highlight moved, and it was on the wrong channel. The same arithmetic was
 * correct on a search or a category, which is why it survived.
 */
class ChannelGridIndexTest {

    @Test
    fun `no sections means the channel index is the item index`() {
        // Search results and category tabs: the cells are the only items.
        val leading = RemoteGridIndex.leadingSectionCount(
            hasRecentsSection = false,
            hasHeroSection = false
        )
        assertEquals(0, leading)
        assertEquals(0, RemoteGridIndex.itemIndexOf(0, listSize = 500, leadingSections = leading))
        assertEquals(299, RemoteGridIndex.itemIndexOf(299, listSize = 500, leadingSections = leading))
    }

    @Test
    fun `the home tab offsets by the recents row and the hero`() {
        // This is the case that was wrong. Both sections are showing, so channel 0 is
        // the third *item* in the grid even though it is the first channel.
        val leading = RemoteGridIndex.leadingSectionCount(
            hasRecentsSection = true,
            hasHeroSection = true
        )
        assertEquals(2, leading)
        assertEquals(2, RemoteGridIndex.itemIndexOf(0, listSize = 500, leadingSections = leading))
        assertEquals(301, RemoteGridIndex.itemIndexOf(299, listSize = 500, leadingSections = leading))
    }

    @Test
    fun `each section is counted once and only when present`() {
        // The hero is skipped once the viewer has recents *and* is browsing, so the
        // offset genuinely varies rather than being a constant.
        assertEquals(
            1,
            RemoteGridIndex.leadingSectionCount(hasRecentsSection = true, hasHeroSection = false)
        )
        assertEquals(
            1,
            RemoteGridIndex.leadingSectionCount(hasRecentsSection = false, hasHeroSection = true)
        )
    }

    @Test
    fun `an index that is not in the list has no item index`() {
        // `scrollToItem` with an out-of-range index throws rather than returning, and
        // a channel that has just been filtered out is the ordinary way to get here.
        val leading = 2
        assertNull(RemoteGridIndex.itemIndexOf(-1, listSize = 500, leadingSections = leading))
        assertNull(RemoteGridIndex.itemIndexOf(500, listSize = 500, leadingSections = leading))
        // Empty list: nothing to scroll to at all.
        assertNull(RemoteGridIndex.itemIndexOf(0, listSize = 0, leadingSections = leading))
    }

    @Test
    fun `the last channel is the last item, offset included`() {
        val size = 3
        val leading = 2
        assertEquals(
            leading + size - 1,
            RemoteGridIndex.itemIndexOf(size - 1, listSize = size, leadingSections = leading)
        )
    }
}
