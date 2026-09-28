package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.rememberMorphingCorners
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

    // The hero's corners open up when it takes focus, and its logo tile — which holds
    // an image and no text — becomes properly expressive. The silhouette of the card
    // itself stays on the corner scale, because it carries the channel's name.
    //
    // Fixed dp radii are safe here and nowhere else in the app: the hero is a
    // full-width card roughly 180dp tall on every layout, so even the widest radius
    // in play stays well inside half its short side.
    val heroShape = rememberMorphingCorners(
        rest = M3ExpressiveShapes.Corners.extraLarge,
        active = M3ExpressiveShapes.Corners.extraLarge.copy(
            topStart = 42.dp, topEnd = 22.dp, bottomEnd = 42.dp, bottomStart = 22.dp
        ),
        isActive = isFocused,
        spec = ExpressiveMotion.spatialDefault
    )
    val logoShape = rememberMorphingCorners(
        rest = ShapeMorph.logoRest,
        active = ShapeMorph.logoActive,
        isActive = isFocused,
        spec = ExpressiveMotion.spatialDefault
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            // The ring below owns the focus lift; see the note in `bouncyClickable`.
            .bouncyClickable(
                scaleDown = 0.97f,
                focusable = false,
                liftOnFocus = false
            ) { onWatchClick(channel) }
            .expressiveFocusRing(
                ringColor = colors.primary,
                restShape = M3ExpressivePolygons.Square,
                // The same ring the cards and the rail use. The hero had its own
                // `SoftBurst`, so the two surfaces you are most likely to move
                // between answered "you are here" in two different shapes, and a
                // burst is a wide, spiky silhouette — the wrong one to lay over the
                // largest surface in the app, where it crosses the LIVE badge and
                // the logo.
                ringShape = ShapeMorph.focusRing,
                focusScale = 1.02f,
                ringWidth = 3.dp,
                onFocusChanged = { isFocused = it }
            )
            .border(1.dp, colors.border, heroShape),
        shape = heroShape,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // A wide accent wash from the leading edge, so the hero reads as the
        // channel's own surface rather than a generic dark panel.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.16f),
                            colors.surfaceVariant.copy(alpha = 0.0f),
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
                            shape = RoundedCornerShape(percent = 50),
                            color = colors.primaryContainer
                        ) {
                            Text(
                                text = "FEATURED",
                                color = colors.onPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
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
                        Button(
                            onClick = { onWatchClick(channel) },
                            shape = RoundedCornerShape(percent = 50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            // No extra clickable here: the whole card is already a
                            // click target, so a second one would fire it twice.
                            modifier = Modifier.align(Alignment.Start)
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
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
