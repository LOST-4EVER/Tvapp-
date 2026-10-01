package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.LocalAppColors

@Composable
fun FeaturedHeroCard(
    channel: Channel,
    showLogos: Boolean = true,
    onWatchClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val accent = monogramAccent(channel.name)
    var isFocused by remember(channel.id) { mutableStateOf(false) }
    // The click keeps the focus target; the ring observes it. See `expressiveFocusRing`.
    val focusSource = remember { MutableInteractionSource() }

    // A fixed radius, at the widest step of the card scale. The hero's outline used
    // to squash on the vertical axis as it took focus — 42dp at the ends, 22dp at
    // the top and bottom — which read as a lens rather than a card, and it is a
    // full-width surface roughly 180dp tall, so every radius in play stays well
    // inside half its short side.
    val heroShape: Shape = M3ExpressiveShapes.Corners.extraLarge.toShape()
    val logoShape: Shape = ShapeMorph.logoRest.toShape()

    Card(
        modifier = modifier
            .fillMaxWidth()
            // One focus target: the click's own, watched by the ring below.
            .tvClickable(
                interactionSource = focusSource,
                pressedFill = colors.primary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                pressedShape = heroShape,
                onClick = { onWatchClick(channel) }
            )
            .expressiveFocusRing(
                ringColor = colors.primary,
                interactionSource = focusSource,
                scrim = colors.focusScrim,
                restShape = M3ExpressivePolygons.Square,
                // The same ring the cards and the rail use. The hero had its own
                // `SoftBurst`, so the two surfaces you are most likely to move
                // between answered "you are here" in two different shapes, and a
                // burst is a wide, spiky silhouette — the wrong one to lay over the
                // largest surface in the app, where it crosses the LIVE badge and
                // the logo.
                ringShape = ShapeMorph.focusRing,
                onFocusChanged = { isFocused = it }
            )
            .border(
                width = if (isFocused) 0.dp else 1.dp,
                color = colors.border,
                shape = heroShape
            ),
        shape = heroShape,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // A wide accent wash from the leading edge, so the hero reads as the
        // channel's own surface rather than a generic dark panel.
        //
        // The grid card dropped its ramp for a flat fill and a 1dp top hairline, and
        // this keeps a gradient on purpose. The cards pay for theirs several hundred
        // times over and sit shoulder to shoulder, where a tint per card turns the
        // grid into stripes; the hero is one surface on the page, and the accent is
        // what tells the viewer this is a channel rather than a section header.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    ),
                    heroShape
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveBadge()
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = M3ExpressiveShapes.Pill,
                            color = colors.primaryContainer
                        ) {
                            Text(
                                // The one all-caps, Latin-only, widely-tracked style
                                // in the type scale. See the note on `labelSmall`.
                                text = "FEATURED",
                                color = colors.onPrimaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }

                    CardFavoriteButton(
                        isFavorite = channel.isFavorite,
                        onClick = { onFavoriteToggle(channel.id) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = logoShape,
                        color = colors.surfaceElevated,
                        modifier = Modifier
                            .size(72.dp)
                            .border(1.dp, colors.border, logoShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ChannelLogo(
                                channelName = channel.name,
                                logoUrl = channel.logoUrl,
                                showLogos = showLogos,
                                contentPadding = 8.dp,
                                size = 72.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${channel.category} • ${channel.quality}",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // The primary action sits with the channel's name rather than
                        // on a row of its own. It used to be a full-width band under
                        // three stacked rows, which made the hero so tall that on a
                        // phone you scrolled past it before reaching a single
                        // channel — the hero was eating the screen it was meant to
                        // introduce.
                        Surface(
                            shape = M3ExpressiveShapes.Pill,
                            color = colors.primary,
                            contentColor = colors.onPrimary,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .tvClickable(
                                    focusable = false,
                                    pressedFill = colors.onPrimary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                                    pressedShape = M3ExpressiveShapes.Pill,
                                    onClick = { onWatchClick(channel) }
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SvgIcon(
                                    resId = KurdishTvIcons.PlayRes,
                                    contentDescription = null,
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = "Watch live now",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
