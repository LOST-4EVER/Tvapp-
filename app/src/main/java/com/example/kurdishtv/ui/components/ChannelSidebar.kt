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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val selectedIndex = remember(channels, selectedChannelId) {
        val id = selectedChannelId
        if (id == null) -1 else channels.indexOfFirst { it.id == id }
    }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            // `scrollToItem`, not `animateScrollToItem`. The row is highlighted the
            // instant it is selected, and a glide that takes a third of a second
            // lands the viewer looking at a list that has not caught up with the
            // highlight they just watched appear.
            runCatching { listState.scrollToItem(selectedIndex) }
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
    var isFocused by remember(channel.id) { mutableStateOf(false) }

    // One focus target for the row: the click's own. The ring observes it rather than
    // adding a second. See `expressiveFocusRing`.
    val rowFocusSource = remember { MutableInteractionSource() }
    // The heart is a genuinely separate stop, reached with the right arrow and left
    // with the left — a second target in a *different place* on the row, which is not
    // the stacked-two-on-one-box problem the comment below is about.
    val heartFocusSource = remember { MutableInteractionSource() }

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
            .clip(plateShape)
            .background(if (isSelected) colors.surfaceHigh else colors.surface)
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
                    isFocused = focused
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
