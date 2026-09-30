package com.example.kurdishtv.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.components.ChannelCard
import com.example.kurdishtv.ui.components.FeaturedHeroCard
import com.example.kurdishtv.ui.components.RecentChannelsRow
import com.example.kurdishtv.ui.components.verticalEdgeFade
import com.example.kurdishtv.ui.keys.RemoteGridIndex
import com.example.kurdishtv.ui.motion.rememberTvFocusRequester
import com.example.kurdishtv.viewmodel.TvUiState
import com.example.ui.theme.LocalAppColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChannelGrid(
    filtered: List<Channel>,
    uiState: TvUiState,
    showLogos: Boolean,
    isHome: Boolean,
    minCellSize: Dp,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    focusToken: String,
    /**
     * A channel to move D-pad focus to, or null.
     *
     * Set by the remote's number pad. The grid owns the focus requester and the
     * anchor card, so this is the only way the number pad can move the highlight —
     * and the grid is the only thing that knows how to bring a card into a lazy
     * layout and then hand focus to it.
     */
    focusChannelId: String? = null,
    compact: Boolean = false
) {
    // A television is driven by a D-pad, so focus is a primary state rather than a
    // detail: a screen where nothing holds focus is a screen where the remote does
    // nothing, and where the first press of any direction key lands somewhere
    // arbitrary because focus is being picked for the first time.
    val gridState = rememberLazyGridState()
    val gridFocus = rememberTvFocusRequester()
    // Read here rather than passed in: the only thing the grid needs it for is the
    // background the edge fade dissolves into, and that is the page background.
    val colors = LocalAppColors.current

    // Where the viewer's focus was the last time this screen was on show.
    //
    // Saved rather than merely remembered, because the navigator composes one
    // destination at a time: the browse screen is disposed while the player is on top
    // of it, so coming back from a video is a *fresh* composition of this screen and
    // nothing in it survives. Without a record of where the viewer was, they came back
    // to a grid scrolled to their place but with nothing focused, and had to hunt.
    var lastFocusedId by rememberSaveable { mutableStateOf<String?>(null) }

    // The card the [FocusRequester] is currently attached to. Deliberately kept
    // separate from `lastFocusedId`, which is rewritten on every arrow press: binding
    // the requester to that value would rebind it — and so recompose every card on
    // screen — each time the viewer moved. This one changes only when focus has to be
    // placed somewhere new.
    var focusAnchorId by rememberSaveable { mutableStateOf<String?>(null) }

    // The card in this grid that holds focus right now, or null when focus is somewhere
    // else on the screen.
    //
    // Kept because "the results changed" and "the viewer wants focus moved" are not the
    // same event. Typing into the search field changes the results on every keystroke,
    // and a grid that took focus back on the first of them would pull the keyboard out
    // from under the viewer and end the search one letter in.
    var activeCardId by remember { mutableStateOf<String?>(null) }

    // Whether this visit has already handed focus to the grid. Plain state on purpose:
    // it resets when the screen leaves composition, which is exactly the case that has
    // to place focus — the viewer coming back from a video.
    var focusPlaced by remember { mutableStateOf(false) }

    val firstChannelId = filtered.firstOrNull()?.id

    // The channel the hero card shows, and how many full-width sections sit above the
    // channel cells.
    //
    // Hoisted out of the grid body because the scroll arithmetic below needs them: a
    // `LazyVerticalGrid` counts *items*, and on the home tab the first two items are
    // the recents row and the hero — not channels. So channel *n* is grid item
    // `n + leadingSectionCount`, and a `scrollToItem` that used the index into
    // `filtered` directly scrolled to the wrong place by one or two rows.
    val heroChannel = uiState.selectedChannel ?: filtered.firstOrNull()
    val leadingSectionCount = RemoteGridIndex.leadingSectionCount(
        hasRecentsSection = isHome && uiState.recentChannels.isNotEmpty(),
        hasHeroSection = isHome && heroChannel != null
    )

    // The grid item index of a channel id, or null when it is not in the list on show.
    fun gridIndexOf(channelId: String): Int? {
        val index = filtered.indexOfFirst { it.id == channelId }
        return RemoteGridIndex.itemIndexOf(index, filtered.size, leadingSectionCount)
    }

    LaunchedEffect(focusToken, firstChannelId) {
        if (firstChannelId == null) return@LaunchedEffect
        val restored = lastFocusedId?.takeIf { id -> filtered.any { it.id == id } }
        // Nothing here has ever been driven by a remote and the grid is scrolled, which
        // means the viewer has been scrolling with a finger. Leave their place alone
        // rather than yanking the list back to the top under them.
        if (restored == null && lastFocusedId == null && gridState.firstVisibleItemIndex > 0) {
            return@LaunchedEffect
        }
        // The fallback is also the recovery path: it fires when the anchored card has
        // left the list entirely — a favourite removed while the Favourites tab is on
        // screen — which is the one case where focus genuinely has nowhere to go.
        //
        // The anchor only moves when the grid is where focus already is, or when this
        // visit has not placed it yet. A card removed out from under a focused grid
        // leaves `activeCardId` naming a card that no longer exists, which still reads
        // as "the grid is in charge" and is exactly the case that has to recover.
        val anchor = restored ?: firstChannelId
        if (!focusPlaced || activeCardId != null) {
            focusAnchorId = anchor
        }
        focusPlaced = true
    }

    // Requested after a frame rather than during composition: the card is not in the
    // tree until the grid has laid it out, and asking for focus before it is attached
    // does nothing at all.
    LaunchedEffect(focusAnchorId) {
        val anchor = focusAnchorId ?: return@LaunchedEffect
        // Getting the anchor into the tree is this effect's problem, not the focus
        // system's: a lazy grid does not compose a card that is scrolled a long way off
        // screen, and `requestFocus` can only target a node that exists — failing
        // *silently*, with no error and no effect, when it does not.
        //
        // So the anchor is scrolled to whenever it is not already on screen, rather
        // than only in the one case where it happens to be the first channel while the
        // list is scrolled. That narrower check missed the case that actually happens
        // most: `rememberLazyGridState` is not saved state, so a grid scrolled down to
        // channel 300 comes back from the player at the top, and the restored anchor is
        // nowhere near the viewport. `requestFocus` then quietly did nothing, and the
        // viewer returned to a screen where the remote did nothing at all — the exact
        // dead end this whole arrangement exists to prevent.
        //
        // Asked of the laid-out items rather than of the scroll position, because that
        // is the thing that decides whether the request can succeed.
        val target = gridIndexOf(anchor)
        if (target != null) {
            val onScreen = gridState.layoutInfo.visibleItemsInfo.any { it.key == anchor }
            if (!onScreen) {
                runCatching { gridState.scrollToItem(target) }
            }
        }
        withFrameNanos { }
        runCatching { gridFocus.requestFocus() }
    }

    // The remote's number pad, landing here as a channel id.
    //
    // Focus cannot be requested until the card exists: a lazy grid does not compose
    // a row that is scrolled off screen, and `requestFocus` against a node that was
    // never composed does nothing at all — silently. So the order is scroll, wait a
    // frame for the card to be laid out, then re-anchor, and the existing
    // `focusAnchorId` effect does the requesting.
    LaunchedEffect(focusChannelId) {
        val id = focusChannelId ?: return@LaunchedEffect
        val target = gridIndexOf(id) ?: return@LaunchedEffect
        lastFocusedId = id
        // This grid is taking focus deliberately, so it must not be treated as a
        // screen that has never placed it and re-anchored somewhere else.
        focusPlaced = true
        runCatching { gridState.scrollToItem(target) }
        withFrameNanos { }
        focusAnchorId = id
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCellSize),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            // The grid clips a card wherever the viewport ends, and a card clipped
            // through its logo reads as a rendering fault rather than as a list that
            // continues — a phone screenshot caught exactly that, a row reduced to a
            // floating category chip with the rest of the card sliced off above it.
            // Fading the clipping edge turns the same pixels into "there is more".
            .verticalEdgeFade(
                gridState = gridState,
                background = colors.background
            )
    ) {
        // Recents come first when they exist.
        //
        // The hero was unconditionally above everything, which meant that for anyone
        // who had actually watched something, the first screenful of the app was an
        // advert for a channel they had not asked for, and the thing they *had* asked
        // for — pick up where you left off — was below the fold. Resume is the more
        // likely intent than browse, so it gets the better position; the hero follows
        // as the catalogue's anchor, and is skipped entirely once someone has recents
        // and is browsing, rather than competing with them for the first glance.
        if (isHome && uiState.recentChannels.isNotEmpty()) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "recent_channels_section",
                contentType = "section"
            ) {
                RecentChannelsRow(
                    recentChannels = uiState.recentChannels,
                    onChannelClick = onChannelClick
                )
            }
        }

        if (isHome && heroChannel != null) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "featured_hero_section",
                contentType = "section"
            ) {
                FeaturedHeroCard(
                    channel = heroChannel,
                    showLogos = showLogos,
                    onWatchClick = onChannelClick,
                    onFavoriteToggle = onFavoriteToggle
                )
            }
        }

        itemsIndexed(
            items = filtered,
            key = { _, ch -> ch.id },
            contentType = { _, _ -> "channel" }
        ) { _, channel ->
            ChannelCard(
                channel = channel,
                showLogos = showLogos,
                onClick = { onChannelClick(channel) },
                onFavoriteToggle = { onFavoriteToggle(channel.id) },
                // Exactly one card carries the focus handle: the anchored one. Handing
                // it to every card would put a requester registration on each of
                // several hundred rows for one job, for an outcome that needs exactly
                // one owner.
                focusRequester = if (channel.id == focusAnchorId) gridFocus else null,
                // Recorded so the next visit can put focus back where the viewer left
                // it, and so the grid can tell whether focus is still inside it. Writing
                // to plain state that nothing reads during composition, so moving
                // between cards costs one write and nothing else.
                //
                // The loss is checked against the card that gained it, because a card
                // also reports `false` the first time it is composed — which happens
                // every time one scrolls into view — and that would otherwise clear a
                // flag another card is still holding.
                onFocusChanged = { focused ->
                    if (focused) {
                        lastFocusedId = channel.id
                        activeCardId = channel.id
                    } else if (activeCardId == channel.id) {
                        activeCardId = null
                    }
                },
                compact = compact,
                modifier = Modifier
            )
        }
    }
}
