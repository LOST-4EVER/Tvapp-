package com.example.kurdishtv.ui.keys

/**
 * Where a channel sits in the browse grid, as a lazy-layout **item** index.
 *
 * ## The bug this exists to prevent
 *
 * `ChannelGrid` scrolls to a channel by index, and it was doing
 * `gridState.scrollToItem(indexIntoTheChannelList)`. That is wrong, and it was wrong
 * by a different amount on every screen.
 *
 * A `LazyVerticalGrid` numbers *items*, and the home tab puts two full-width
 * non-channel items in front of the cells: the recents row and the featured hero.
 * So on the home tab, channel *n* — zero-based — is grid item `n + 2`, and the old
 * call scrolled two rows short. The number pad, which is the feature most likely to
 * ask for a specific channel, therefore landed the viewer on the wrong card and then
 * handed it D-pad focus, which is worse than not moving at all: the viewer is looking
 * at a highlighted channel that is not the one they asked for.
 *
 * A search or a category chip leaves the home tab, where the offset is zero and the
 * same arithmetic happened to be right — which is exactly why this survived as long
 * as it did. It is only wrong where the app is most used.
 *
 * Pure, so it can be pinned. See `ChannelGridIndexTest`.
 */
object RemoteGridIndex {

    /**
     * How many full-width items sit above the channel cells.
     *
     * @param hasRecentsSection whether the recents row is being shown
     * @param hasHeroSection whether the featured hero is being shown
     */
    fun leadingSectionCount(hasRecentsSection: Boolean, hasHeroSection: Boolean): Int =
        (if (hasRecentsSection) 1 else 0) + (if (hasHeroSection) 1 else 0)

    /**
     * The grid item index of the channel at [channelIndex] in the filtered list.
     *
     * @return the item index, or null when [channelIndex] is not a position in the
     *   list — a negative index, or one past the end.
     */
    fun itemIndexOf(channelIndex: Int, listSize: Int, leadingSections: Int): Int? {
        if (channelIndex < 0 || channelIndex >= listSize) return null
        return leadingSections + channelIndex
    }
}
