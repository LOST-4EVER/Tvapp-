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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
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
import com.example.kurdishtv.ui.motion.rememberMorphingPolygon
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
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
 * The chip carries a name, so its own silhouette stays on the corner scale — but the
 * small leading tile holds only a glyph, which is what earns it an Expressive outline
 * that opens when the chip takes focus. The focus ring around the chip is the same
 * rotating shape the grid uses, so "where am I" looks identical everywhere in the app.
 */
@Composable
private fun RecentChannelChip(
    channel: Channel,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    var isFocused by remember(channel.id) { mutableStateOf(false) }

    val chipShape = rememberMorphingCorners(
        rest = M3ExpressiveShapes.Corners.mediumCard,
        active = M3ExpressiveShapes.Corners.cardFocused,
        isActive = isFocused,
        spec = ExpressiveMotion.spatialFast
    )
    val tileShape: Shape = rememberMorphingPolygon(
        rest = ShapeMorph.buttonRest,
        active = ShapeMorph.indicatorActive,
        isActive = isFocused,
        spec = ExpressiveMotion.spatialFast,
        rotationWhileActive = 45f
    )

    Surface(
        shape = chipShape,
        color = colors.surfaceVariant,
        modifier = Modifier
            .background(colors.surfaceVariant, chipShape)
            .bouncyClickable(scaleDown = 0.92f, focusable = false, onClick = onClick)
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
                    .size(22.dp)
                    .background(colors.primary.copy(alpha = 0.18f), tileShape),
                contentAlignment = Alignment.Center
            ) {
                SvgIcon(
                    resId = KurdishTvIcons.Tv,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = channel.name,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = channel.category,
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
