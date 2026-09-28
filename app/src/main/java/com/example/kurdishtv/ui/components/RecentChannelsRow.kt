package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.motion.CornerScale
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.rememberMorphingCorners
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.ui.theme.LocalAppColors

@Composable
fun RecentChannelsRow(
    recentChannels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentChannels.isEmpty()) return
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primaryContainer
            ) {
                SvgIcon(
                    resId = KurdishTvIcons.History,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RECENTLY WATCHED",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        LazyRow(
            modifier = Modifier.edgeFade(listState),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = recentChannels,
                // Keyed on the channel alone. Including the index meant that
                // watching a channel shifted every other entry's key, so the whole
                // row was torn down and rebuilt on each watch instead of just
                // moving one item. The id is already unique by construction.
                key = { _, item -> item.id }
            ) { _, channel ->
                RecentChannelChip(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

/**
 * One entry in the recently-watched row.
 *
 * Both the chip and its small leading tile sit on the corner scale. The tile is only
 * 22dp, which is well below the size at which a lobed outline is still legible, and
 * because the tile is a `background` rather than a `Surface` its outline *is* its
 * clip — a polygon that bulged past its own bounds drew the tint outside the tile and
 * left the glyph behind it, which is exactly what this row was doing.
 *
 * The focus ring is still a rotating Expressive shape, so "where am I" keeps its
 * distinct look everywhere in the app.
 */
@Composable
private fun RecentChannelChip(
    channel: Channel,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    var isFocused by remember(channel.id) { mutableStateOf(false) }

    // The chip is ~54dp tall, so half its short side is 27dp. The shared
    // "cardFocused" radius is 30dp, which made this chip's own corners overlap into
    // each other the moment it took focus. 20 -> 24dp opens visibly and stays legal.
    val chipShape = rememberMorphingCorners(
        rest = CornerScale.uniform(20.dp),
        active = CornerScale.uniform(24.dp),
        isActive = isFocused,
        spec = ExpressiveMotion.spatialFast
    )
    val tileShape: Shape = remember { RoundedCornerShape(percent = 50) }

    Surface(
        shape = chipShape,
        color = colors.surfaceVariant,
        modifier = Modifier
            // The ring below owns the focus lift; see the note in `bouncyClickable`.
            .bouncyClickable(
                scaleDown = 0.92f,
                focusable = false,
                liftOnFocus = false,
                onClick = onClick
            )
            .expressiveFocusRing(
                ringColor = colors.primary,
                restShape = M3ExpressivePolygons.Square,
                ringShape = ShapeMorph.focusRing,
                focusScale = 1.04f,
                ringWidth = 2.dp,
                onFocusChanged = { isFocused = it }
            )
            .border(1.dp, colors.border, chipShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(tileShape)
                    .background(colors.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                SvgIcon(
                    resId = KurdishTvIcons.Tv,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                // A fixed width stops the row from reflowing every time a name of a
                // different length is watched, which used to shove every chip to its
                // right one place along.
                modifier = Modifier.width(96.dp)
            ) {
                Text(
                    text = channel.name,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = channel.category,
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
