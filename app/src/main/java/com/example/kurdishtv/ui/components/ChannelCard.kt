package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.motion.tvFocusable
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

@Composable
fun ChannelCard(
    channel: Channel,
    showLogos: Boolean = true,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val accent = monogramAccent(channel.name)

    Card(
        modifier = modifier
            .testTag("channel_card_${channel.id}")
            .fillMaxWidth()
            .clip(M3ExpressiveShapes.LogoTile)
            .bouncyClickable(scaleDown = 0.94f) { onClick() }
            // tvFocusable owns the focus ring outright; the card no longer keeps a
            // second, competing border that switched between 1dp and 2dp of accent.
            .tvFocusable(
                ringColor = colors.primary,
                shape = M3ExpressiveShapes.LogoTile
            )
            .border(1.dp, colors.border, M3ExpressiveShapes.LogoTile),
        shape = M3ExpressiveShapes.LogoTile,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        // Depth comes from the gradient and the logo well below. A Material shadow
        // on a near-black surface is invisible, and it still costs a render pass per
        // item in a grid of several hundred cards.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // A top-lit ramp rather than a flat fill. On a near-black surface a flat
        // card reads as a hole with a border; the ramp gives it something to sit on.
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(listOf(colors.surfaceVariant, colors.surface)),
                M3ExpressiveShapes.LogoTile
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
                    showLogos = showLogos
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
 * It carries a soft glow in the channel's own accent, so a card is recognisable
 * by colour before the name is read — and a channel with no logo at all still gets
 * a designed tile rather than a gap in the grid.
 */
@Composable
private fun ChannelLogoWell(
    channel: Channel,
    accent: Color,
    showLogos: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val wellShape = M3ExpressiveShapes.MediumCard

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(wellShape)
                .background(Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                    radius = 240f
                ), wellShape)
                .background(colors.surfaceElevated, wellShape)
                .border(1.dp, colors.border, wellShape),
            contentAlignment = Alignment.Center
        ) {
            ChannelLogo(
                channelName = channel.name,
                logoUrl = channel.logoUrl,
                showLogos = showLogos,
                contentPadding = 9.dp,
                // Decode for the tile this is drawn in, not at source resolution.
                size = 84.dp
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
    Surface(
        shape = CircleShape,
        color = colors.glass,
        modifier = Modifier.size(34.dp)
    ) {
        IconButton(onClick = onClick) {
            SvgIcon(
                resId = if (isFavorite) KurdishTvIcons.FavoriteFilledRes else KurdishTvIcons.FavoriteOutline,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) colors.liveRed else colors.textPrimary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
