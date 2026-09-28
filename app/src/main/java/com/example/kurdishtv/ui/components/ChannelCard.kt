package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.LocalReduceMotion
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.rememberMorphingCorners
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/**
 * A channel in the grid.
 *
 * The card is the app's densest surface — a name, a category, a quality badge, a logo
 * and two controls — and it is the surface a person on a sofa scans from two metres
 * away. So it carries two shape animations, chosen for different jobs:
 *
 *  - **The card's own corners** open up when it takes D-pad focus. A rounded
 *    rectangle that springs from 22dp to 30dp is a much quieter signal than a colour
 *    change, and unlike a colour change it survives being looked at obliquely.
 *  - **The logo well** morphs from a rounded square into a puffy blob, because the
 *    well holds an image and nothing else. This is the one part of the card free to be
 *    properly expressive, and it is why the grid reads as having depth rather than as
 *    a flat list of rectangles.
 *
 * The focus ring itself is an Expressive shape that turns slowly — see
 * [expressiveFocusRing]. Everything the card does is a spatial spring, so the surface
 * arrives with a small overshoot rather than easing in.
 */
@Composable
fun ChannelCard(
    channel: Channel,
    showLogos: Boolean = true,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Lets the grid decide which single card holds programmatic focus.
     *
     * Only one card is ever given one. Handing every card its own requester would
     * mean several hundred focus handles the grid has to keep in step, for an
     * outcome — "the first card, or wherever the viewer was" — that needs exactly
     * one owner.
     */
    focusRequester: FocusRequester? = null,
    /**
     * Reports D-pad focus back to the grid, so the grid can remember which card the
     * viewer was on and put focus back there the next time this screen is shown.
     */
    onFocusChanged: (isFocused: Boolean) -> Unit = {}
) {
    val colors = LocalAppColors.current
    // The accent is derived from the channel's name, so it is the same for the whole
    // life of the card. Recomputing it on every recomposition meant a hash and a
    // modulo per card per frame for as long as the grid was on show; it is also what
    // makes `logoWell` below a stable value, so the tile is not re-measured either.
    val accent = remember(channel.name) { monogramAccent(channel.name) }
    val reduceMotion = LocalReduceMotion.current

    // Focus is tracked here as well as inside the modifier, because the logo well and
    // the card's corners animate in response to it and the modifier owns the state.
    var isFocused by remember(channel.id) { mutableStateOf(false) }

    val cardShape = rememberMorphingCorners(
        rest = M3ExpressiveShapes.Corners.logoTile,
        active = M3ExpressiveShapes.Corners.cardFocused,
        isActive = isFocused,
        spec = ExpressiveMotion.spatialDefault
    )

    Card(
        modifier = modifier
            .testTag("channel_card_${channel.id}")
            .fillMaxWidth()
            // `liftOnFocus = false`: `expressiveFocusRing` below already lifts this
            // node by 1.05 while it is focused. Two lifts on one node multiply, so
            // the card was jumping 10% instead of the 5% that was asked for — and
            // the jump was large enough to be visible as a pop every time the D-pad
            // moved.
            //
            // `onLongClick` is how a remote user reaches the favourite, now that the
            // heart is out of the tab order. See [CardFavoriteButton].
            .bouncyClickable(
                scaleDown = 0.94f,
                focusable = false,
                liftOnFocus = false,
                onLongClick = onFavoriteToggle
            ) { onClick() }
            // The requester sits *after* the click for a reason.
            //
            // `Modifier.clickable` brings a focus target of its own, and a
            // `focusRequester` binds to the first focus target below it in the chain.
            // Placed before the click — where it used to be — it therefore bound to the
            // click's target, which is not the target that owns the visible focus
            // state: `expressiveFocusRing` supplies its own. Asking it for focus lit
            // nothing up. After the click, the only target below it is the ring's.
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .expressiveFocusRing(
                ringColor = colors.primary,
                restShape = M3ExpressivePolygons.Square,
                ringShape = ShapeMorph.focusRing,
                focusScale = 1.05f,
                onFocusChanged = { focused ->
                    isFocused = focused
                    onFocusChanged(focused)
                }
            )
            .border(1.dp, colors.border, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        // Depth comes from the gradient and the logo well below. A Material shadow on a
        // near-black surface is invisible, and it still costs a render pass per item in
        // a grid of several hundred cards.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // A top-lit ramp rather than a flat fill. On a near-black surface a flat card
        // reads as a hole with a border; the ramp gives it something to sit on.
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(listOf(colors.surfaceVariant, colors.surface)),
                cardShape
            )
        ) {
            Column {
                // The status row is its own band rather than an overlay on the
                // artwork. Floating the badge and the heart on top of the logo meant
                // every wide logo was partly hidden behind them, and the only way to
                // avoid it was to inset the artwork by an arbitrary amount that only
                // worked for one logo aspect ratio.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 9.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveBadge()

                    CardFavoriteButton(
                        isFavorite = channel.isFavorite,
                        onClick = onFavoriteToggle
                    )
                }

                ChannelLogoWell(
                    channel = channel,
                    accent = accent,
                    showLogos = showLogos,
                    isActive = isFocused,
                    allowAnimation = !reduceMotion
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 11.dp)
                ) {
                    Text(
                        text = channel.name,
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = M3ExpressiveShapes.BadgePill,
                            color = colors.surfaceElevated
                        ) {
                            Text(
                                text = channel.category,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SvgIcon(
                                resId = KurdishTvIcons.Signal,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                // Prefer the label the source published over the
                                // boolean: a couple of channels are flagged HD but
                                // publish 720p, and the card contradicted itself.
                                text = channel.quality.substringAfterLast('/').trim()
                                    .ifBlank { if (channel.isHd) "1080p" else "720p" },
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The raised tile a channel's logo sits in.
 *
 * It carries a soft glow in the channel's own accent, so a card is recognisable by
 * colour before the name is read — and a channel with no logo at all still gets a
 * designed tile rather than a gap in the grid.
 *
 * When the card takes focus the well's corners open up. It used to open into a puffy
 * lobed blob; a lobed outline on the card's largest element made the tile's silhouette
 * fight the card's own rounded rectangle right above it, and the logo inside was no
 * longer reliably inside *anything*. A corner that opens 20dp → 28dp says the same
 * thing and leaves the logo exactly where it was.
 */
@Composable
private fun ChannelLogoWell(
    channel: Channel,
    accent: Color,
    showLogos: Boolean,
    isActive: Boolean,
    allowAnimation: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    // Built unconditionally and gated on the flag, rather than called from inside a
    // branch: the composition has to keep the same shape whatever the setting is.
    val animatedWell = rememberMorphingCorners(
        rest = ShapeMorph.logoRest,
        active = ShapeMorph.logoActive,
        isActive = isActive && allowAnimation,
        spec = ExpressiveMotion.spatialDefault
    )
    val stillWell = M3ExpressiveShapes.LogoTile
    val wellShape: Shape = if (allowAnimation) animatedWell else stillWell

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                // The well fills the card instead of sitting in the middle of it as a
                // small tile. It was a fixed 84dp square inside a cell that runs from
                // roughly 120dp on a phone to 200dp on a television, so on most of the
                // grid well over half of every card was empty on both sides of the
                // logo — and because those two narrow columns of undersized tiles are
                // what the eye lands on, the dead space between them read as a hole
                // running down the middle of the grid rather than as card padding.
                //
                // `widthIn` is applied *before* `fillMaxWidth` on purpose: a width
                // constraint after a fill is a no-op, because the fill has already
                // pinned min and max to the parent's width, and the cap would then be
                // coerced straight back to that same width. Ordered this way the cap
                // really does bound the well, and a wide television cell gets a
                // larger logo instead of a billboard.
                .widthIn(max = 132.dp)
                .fillMaxWidth()
                // Square, and therefore the same height at every cell width, which is
                // what keeps each row of the grid the same rhythm.
                .aspectRatio(1f)
                .clip(wellShape)
                .background(colors.surfaceElevated, wellShape)
                // Glow on top of the tile, not under it. These two used to be in
                // the other order, and because `background` draws in modifier order
                // the opaque surfaceElevated was painted straight over the gradient
                // — the per-channel accent this tile exists to show was never visible
                // on any card in the app.
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                        radius = 240f
                    ),
                    wellShape
                )
                .border(1.dp, colors.border, wellShape),
            contentAlignment = Alignment.Center
        ) {
            ChannelLogo(
                channelName = channel.name,
                logoUrl = channel.logoUrl,
                showLogos = showLogos,
                contentPadding = 9.dp,
                // Decode for the largest tile this can be drawn in, not at source
                // resolution. Slightly over the draw size on a narrow cell, which is
                // the safe direction to err in; decoding under it is what makes a
                // logo look soft on a television.
                size = 132.dp
            )
        }
    }
}

@Composable
internal fun CardFavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    // The heart *glyph* already says "favourite"; the silhouette does not need to say
    // it a second time, and a heart outline around a heart glyph puts the two slightly
    // out of register. The button stays a circle and signals state by fill and tint,
    // which is also the only signal that survives being glanced at obliquely.
    val shape = remember { RoundedCornerShape(percent = 50) }
    Surface(
        shape = shape,
        color = if (isFavorite) colors.liveRed.copy(alpha = 0.18f) else colors.glass,
        modifier = Modifier
            .size(34.dp)
            .border(1.dp, colors.border, shape)
            // The click lives on the Surface, not on an `IconButton` inside it.
            // `IconButton` enforces a 48dp minimum touch target, so nesting one in a
            // 34dp surface measured 48dp of content in a 34dp box: the glyph was
            // clipped away by the surface outline and the oversized hit area spilled
            // over the card's own edges, stealing taps from the channel behind it.
            //
            // This heart sits inside the card's own bounds, so a focusable heart is the
            // nearest target to the right of every card: one press of the right arrow
            // lands on it and the next press is needed to reach the next channel, which
            // across a grid of several hundred halves the speed of the most-used
            // direction in the app.
            //
            // `focusable = false` is what takes it out of the tab order — see the note
            // on that parameter, which is the part that is easy to get wrong. The
            // action is not lost: it is a long press on the card, the idiom every TV
            // player uses for a list row's secondary action, and the heart remains a
            // tap target that announces itself and exposes a click action for touch and
            // for a screen reader.
            .bouncyClickable(focusable = false, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            SvgIcon(
                resId = if (isFavorite) KurdishTvIcons.FavoriteFilledRes else KurdishTvIcons.FavoriteOutline,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) colors.liveRed else colors.textPrimary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
