package com.example.kurdishtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.CornerScale
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.motion.rememberMorphingCorners
import com.example.ui.theme.LocalAppColors

@Composable
fun TopHeaderBar(
    channelCount: Int,
    isLoading: Boolean = false,
    onOpenImport: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Squeeze the row for a short window — a phone on its side, where every dp of
     * vertical space the header takes is a dp of grid that is not on show.
     */
    compact: Boolean = false
) {
    val colors = LocalAppColors.current

    // While a refresh is in flight the logo gently breathes, so the header reads as
    // alive without needing a blocking spinner over the grid.
    val logoScale by animateFloatAsState(
        targetValue = if (isLoading) 1.06f else 1f,
        animationSpec = ExpressiveMotion.spatialDefault,
        label = "HeaderLogoScale"
    )

    // The brand tile holds a glyph, so it stays on the corner scale. Its resting
    // radius matches the logo tiles in the grid below it, which is what ties the
    // header and the content together; while a refresh is in flight the corners open
    // a little, so the header says "working" without a spinner over the grid.
    val brandShape = rememberMorphingCorners(
        // Sized off the tile, not off a shared token: the tile is 40-46dp, so half
        // its short side is 20-23dp and the 22dp "logo tile" radius exceeded it on the
        // narrow layout. Opening 30% -> 44% of the tile keeps it a rounded square at
        // both sizes and still reads as a change when a refresh is in flight.
        rest = CornerScale.uniform(ShapeMorph.cornerRadius(46.dp, 0.30f)),
        active = CornerScale.uniform(ShapeMorph.cornerRadius(46.dp, 0.44f)),
        isActive = isLoading,
        spec = ExpressiveMotion.spatialDefault
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // On narrow screens the title, the action row and the channel count cannot all
        // fit on one line, so the title is capped and the secondary line is dropped
        // rather than letting the text slide under the buttons.
        val isNarrow = maxWidth < 420.dp
        val titleMaxWidth = if (isNarrow) 132.dp else 220.dp
        val gutter = if (isNarrow) 12.dp else 20.dp
        val logoSize = if (isNarrow) 40.dp else 46.dp
        val verticalPadding = if (compact) 8.dp else 14.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = gutter, vertical = verticalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // `weight(1f)` and not `weight(1f, fill = false)`.
            //
            // With `fill = false` the left cluster is laid out at whatever width it
            // asks for and `SpaceBetween` absorbs the difference — but when the
            // cluster's own width plus the three 40dp action buttons plus the gutters
            // exceeds the row, the row does not shrink the cluster, it overflows, and
            // the overflow goes off the *left* edge because the cluster is first. The
            // brand tile is the leading child, so the brand tile is what got pushed
            // half off the screen on a narrow phone.
            //
            // Filling the weight pins the cluster to exactly the space the action
            // buttons leave, and the title column below takes the slack and
            // ellipsizes, so the one thing that must stay fully visible — the brand
            // tile and the three actions — always is.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // The brand tile is the one place a bold shape still reads correctly:
                // a rounded square, matching the logo tiles in the grid below it, so
                // the header and the content share a geometry. While a refresh is in
                // flight it opens into a very-sunny shape and breathes, so the header
                // says "working" without a blocking spinner over the grid.
                Surface(
                    shape = brandShape,
                    color = colors.primary,
                    modifier = Modifier
                        .size(logoSize)
                        .border(1.dp, colors.primary.copy(alpha = 0.5f), brandShape)
                        // Attached only while the logo is actually breathing. A
                        // `graphicsLayer` is a render node with its own display list,
                        // and this one otherwise sat at scale 1 for the entire life of
                        // the screen, multiplying by one.
                        .then(
                            if (logoScale == 1f) {
                                Modifier
                            } else {
                                Modifier.graphicsLayer {
                                    scaleX = logoScale
                                    scaleY = logoScale
                                }
                            }
                        )
                        .bouncyClickable { onRefresh() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SvgIcon(
                            resId = KurdishTvIcons.Tv,
                            // Names the *action*, not the app. This tile is a refresh
                            // button — it runs [onRefresh] — and it announced itself as
                            // the app's title, so a screen reader read out "Kurdish TV
                            // Live" where the viewer needed to hear what pressing it
                            // would do. The brand is already the window title and the
                            // text beside this tile.
                            contentDescription = "Refresh channels",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(if (isNarrow) 22.dp else 25.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(if (isNarrow) 8.dp else 12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kurdish TV Live",
                        color = colors.textPrimary,
                        fontSize = if (isNarrow) 17.sp else 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = titleMaxWidth)
                    )
                    Text(
                        text = buildString {
                            append(channelCount)
                            append(if (channelCount == 1) " channel" else " channels")
                            if (!isNarrow) append(" • تەلەفزیۆنی کوردی")
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = titleMaxWidth)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconButton(
                    iconRes = KurdishTvIcons.Settings,
                    contentDescription = "Settings",
                    onClick = onOpenSettings,
                    style = AppIconButtonStyle.Tonal
                )
                AppIconButton(
                    iconRes = KurdishTvIcons.AddLink,
                    contentDescription = "Import IPTV playlist",
                    onClick = onOpenImport,
                    style = AppIconButtonStyle.Tonal
                )
                AppIconButton(
                    iconRes = KurdishTvIcons.Refresh,
                    contentDescription = "Reload channels",
                    onClick = onRefresh,
                    style = AppIconButtonStyle.Tonal
                )
                if (isLoading) {
                    BouncingLoader(size = 26.dp)
                }
            }
        }
    }
}
