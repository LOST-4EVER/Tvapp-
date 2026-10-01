package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import androidx.compose.ui.graphics.Shape
import com.example.kurdishtv.ui.motion.tvClickable
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.ChannelNumber
import com.example.ui.theme.LocalAppColors

/**
 * The channel list, as a sidebar beside the grid.
 *
 * The grid is good at *finding* a channel and bad at *choosing* one. Six hundred
 * cards of artwork give no sense of what is in the list, what order it is in, or
 * what is ten rows down — so a viewer who wants NRT either scrolls looking for a
 * logo they half-remember, or types into the search field, which is a keyboard on a
 * television. The rail beside this lists categories; this lists the thing they
 * actually came for.
 *
 * It is also what makes the number pad legible. Every row is numbered, and the
 * numbers are positions in *this* list, so pressing `47` is answerable at a glance
 * by looking at row 47 rather than by counting across a grid whose order the viewer
 * cannot see.
 *
 * Focus behaviour is the reason this is built here rather than reusing `ChannelCard`:
 *
 *  - **Focusing a row selects it.** Moving the highlight up the sidebar changes the
 *    selection, so the preview pane beside it follows along. Selection and focus are
 *    then the same thing on this surface, which is what makes arrowing feel like
 *    browsing rather than like scrolling a document.
 *  - **The heart is its own stop, to the right of the name.** See the note on it
 *    below; this is the one place in the app that departs from one stop per control,
 *    and it is deliberate.
 */
@Composable
fun ChannelSidebar(
    channels: List<Channel>,
    selectedChannelId: String?,
    showLogos: Boolean,
    onChannelClick: (Channel) -> Unit,
    onChannelFocused: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()

    // Keep the selected channel on screen when the selection moves from somewhere
    // else — the grid, or the remote's number pad. Without this the sidebar can
    // show row 3 highlighted while row 200 is the one being previewed, which is
    // worse than not showing a highlight at all.
    //
    // Not memoised, and that is deliberate.
    //
    // `remember(channels, selectedChannelId)` is the obvious spelling and it cost more
    // than the thing it was caching: `remember` compares its keys with `equals`, so
    // every recomposition of this sidebar compared two Lists of several hundred
    // nine-field `Channel` data classes field by field — before running the
    // `indexOfFirst` that would have answered the question in the same pass anyway.
    // This sidebar is permanent chrome on the wide layout, so that ran on every
    // keystroke of the search field and on every selection change, to compute an
    // index.
    //
    // `remember { derivedStateOf { ... } }` is not the fix. `derivedStateOf` only
    // re-runs for reads of *snapshot state*, and `channels` and `selectedChannelId`
    // are plain function parameters — an unkeyed `remember` would capture the values
    // from the first composition and quietly go stale the moment either changed,
    // which is the same highlight-the-wrong-row bug in a harder-to-spot form.
    //
    // So it is just computed. One `indexOfFirst` is a single forward scan with an
    // early exit over a list of ids — a few hundred reference comparisons, no
    // allocation, and nothing that can be stale. Wrapping it in a cache was the
    // expensive part, not the scan.
    val selectedIndex = if (selectedChannelId == null) -1
        else channels.indexOfFirst { it.id == selectedChannelId }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            // Only when the row is genuinely off screen.
            //
            // It used to `scrollToItem` on *every* selection change, and on this list
            // that means every arrow press down the sidebar: `scrollToItem` puts its
            // target at the leading edge, so arrowing one row down scrolled the list
            // one row — then the next press scrolled another, forever. The viewer
            // watching a highlight walk down a list watches the list walk with it, and
            // the rows above the highlight are constantly being pushed out of view for
            // no reason. It was also fighting the `LazyColumn`'s own D-pad scrolling,
            // which already brings a focused row into view; two things positioning the
            // list at once is how the highlight and the list end up disagreeing.
            //
            // A row already on screen needs nothing, and on a tablet where the whole
            // list fits, that is every row there is.
            val onScreen = listState.layoutInfo.visibleItemsInfo.any { it.index == selectedIndex }
            if (!onScreen) {
                runCatching { listState.scrollToItem(selectedIndex) }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(colors.surface)
            // A divider, not a card outline. This edge separates the rail from the
            // grid beside it rather than boxing the rail in as an object of its own,
            // so it is a step lighter than the one around a card and does not glow
            // the same way.
            .border(width = 1.dp, color = colors.divider)
    ) {
        SidebarHeader(channels.size)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = channels,
                // The position *is* the channel number, so it has to be the key: two
                // rows swapping places after a refresh must not make the viewer
                // re-find where they were.
                key = { _, channel -> channel.id }
            ) { index, channel ->
                ChannelSidebarRow(
                    number = index + 1,
                    channel = channel,
                    isSelected = channel.id == selectedChannelId,
                    showLogo = showLogos,
                    onClick = { onChannelClick(channel) },
                    onFocused = { onChannelFocused(channel) },
                    onFavoriteToggle = { onFavoriteToggle(channel.id) }
                )
            }
        }
    }
}

@Composable
private fun SidebarHeader(count: Int) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)) {
        Text(
            text = "Channels",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$count in this list",
            color = colors.textTertiary,
            fontSize = 11.sp
        )
    }
}

/** One channel: its number, its logo, its name, and a heart. */
@Composable
private fun ChannelSidebarRow(
    number: Int,
    channel: Channel,
    isSelected: Boolean,
    showLogo: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val colors = LocalAppColors.current

    // One focus target for the row: the click's own. The ring observes it rather than
    // adding a second. See `expressiveFocusRing`.
    val rowFocusSource = remember(channel.id) { MutableInteractionSource() }
    // The heart is a genuinely separate stop, reached with the right arrow and left
    // with the left — a second target in a *different place* on the row, which is not
    // the stacked-two-on-one-box problem the comment below is about.
    val heartFocusSource = remember(channel.id) { MutableInteractionSource() }

    // Selected and focused are drawn differently on purpose. Focus is the accent
    // ring the viewer is looking for *right now*; selection is a quieter filled
    // plate, because it is a record of where they are rather than a prompt. Drawing
    // them identically made the sidebar's highlight and the grid's disagree whenever
    // selection was somewhere else.
    //
    // The plate is [AppColors.surfaceHigh] rather than the card's own surface, so a
    // selected row lifts off the rail instead of shifting sideways in tone.
    val plateShape: Shape = M3ExpressiveShapes.Corners.smallCard.toShape()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            // No `clip` on the row. It used to clip to the plate's shape, which cut
            // the focus ring in half — the ring is drawn outset from the element, and
            // a clip applied before it in the chain removes everything outside the
            // element's own bounds. Nothing needed it: the plate below is already
            // shape-aware, the press fill carries its own shape, and the row's
            // content sits inside its padding.
            .background(if (isSelected) colors.surfaceHigh else colors.surface, plateShape)
            // The click keeps its own focus target and the ring watches the same
            // source, so there is one stop on this row rather than two competing ones.
            // `onLongClick` is how a remote reaches the favourite, since the heart is
            // out of the tab order.
            .tvClickable(
                interactionSource = rowFocusSource,
                pressedFill = colors.primary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                pressedShape = plateShape,
                onLongClick = onFavoriteToggle
            ) { onClick() }
            .expressiveFocusRing(
                ringColor = colors.primary,
                interactionSource = rowFocusSource,
                scrim = colors.focusScrim,
                restShape = M3ExpressivePolygons.Square,
                ringShape = ShapeMorph.focusRing,
                onFocusChanged = { focused ->
                    // Only the *gain* is reported.
                    //
                    // This used to also mirror the flag into a `mutableStateOf` that
                    // nothing read — the row's plate and type are driven by
                    // `isSelected`, not by focus. So every arrow press down this list
                    // wrote state on the row that was losing focus and the row gaining
                    // it, invalidating both rows' compositions to produce a picture
                    // identical to the one already on screen. On a list of several
                    // hundred rows, arrowing is *the* interaction, so this was two
                    // wasted recompositions per keypress for the whole traversal.
                    if (focused) onFocused()
                }
            )
            .semantics {
                role = Role.Tab
                contentDescription = "$number, ${channel.name}"
            }
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        // The number is part of the row rather than a separate column so that it
        // scrolls with the channel. A fixed gutter of numbers beside a scrolling
        // list drifts out of alignment as soon as anything is filtered.
        //
        // A fixed-width face in a fixed-width slot, so row 8 and row 108 leave the
        // logo in exactly the same place and the column of numbers scans straight
        // down instead of stepping in and out.
        Text(
            text = number.toString(),
            color = if (isSelected) colors.primary else colors.textTertiary,
            style = ChannelNumber,
            maxLines = 1,
            modifier = Modifier.width(26.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))

        if (showLogo) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    // The same rounded square the rail's logo tile uses, rather than
                    // a circle: a channel's logo is a rectangle far more often than
                    // it is a roundel, and clipping it to one crops the sides off.
                    .clip(M3ExpressiveShapes.MediumCard)
            ) {
                ChannelLogo(
                    channelName = channel.name,
                    logoUrl = channel.logoUrl,
                    showLogos = true,
                    // Decode for the 34dp the row draws it at, not for the 132dp
                    // card the same channel also appears in: without this the sidebar
                    // and the grid keep two separate bitmaps for one logo.
                    size = 34.dp,
                    contentPadding = 4.dp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Text(
            text = channel.name,
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(M3ExpressiveShapes.Pill)
                // A real D-pad stop, which is the one deliberate departure from the
                // "one stop per control" rule the rest of the app follows.
                //
                // The alternative — the long-press the grid card uses for its
                // favourite — is a *pointer* gesture. Compose's `clickable` acts on
                // key-down, so holding OK fires the click immediately and there is
                // no key-down duration left to distinguish a long press from a tap;
                // a remote cannot reach it at all. Leaving the heart out of the tab
                // order would have made favouriting impossible from the sidebar,
                // which is the one place it is a primary action.
                //
                // It does not reintroduce the bug that rule was written for. That was
                // about *stacking two targets on the same box*, so arrowing down
                // stopped twice on the same channel. This is a second target in a
                // different place: the heart is to the **right** of the name, so it
                // is reached with the right arrow and left again with the left —
                // the standard list-with-a-trailing-action shape, and arrowing down
                // the list never passes through it. Only the rows on screen are
                // composed at all, so the cost is bounded by the viewport.
                .tvClickable(
                    interactionSource = heartFocusSource,
                    onClick = onFavoriteToggle
                )
                .expressiveFocusRing(
                    ringColor = colors.primary,
                    interactionSource = heartFocusSource,
                    scrim = colors.focusScrim,
                    restShape = M3ExpressivePolygons.Square,
                    ringShape = M3ExpressivePolygons.Cookie6Sided
                ),
            contentAlignment = Alignment.Center
        ) {
            SvgIcon(
                resId = if (channel.isFavorite) {
                    KurdishTvIcons.FavoriteFilledRes
                } else {
                    KurdishTvIcons.FavoriteOutline
                },
                contentDescription = if (channel.isFavorite) {
                    "Remove ${channel.name} from favourites"
                } else {
                    "Add ${channel.name} to favourites"
                },
                tint = if (channel.isFavorite) colors.liveRed else colors.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
