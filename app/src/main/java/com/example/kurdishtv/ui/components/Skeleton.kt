package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/**
 * Placeholder blocks for content that has not arrived yet.
 *
 * ## No shimmer, and that is the point
 *
 * A skeleton is normally a shimmering gradient sweeping left to right. That is an
 * animation: a per-frame shader pass, a render node held open for as long as the
 * content takes, and a second draw of the block for its duration. This app has no
 * animation anywhere, and a placeholder that moves while the real content sits still
 * beside it would be the one thing on screen that contradicts that.
 *
 * So a skeleton here is a **static** block, and the legibility work a shimmer
 * normally does is done by contrast instead: it is one step above the page, so it
 * reads as an object sitting on the page rather than as a hole in it.
 *
 * ## Where one belongs, and where it does not
 *
 * A skeleton is a *substitute for blank space*, not a signal that a network call is
 * running. The app's grid is the obvious candidate and is deliberately not given one:
 * the bundled catalogue is on screen before any load starts and stays on screen
 * while a refresh runs behind it, so there is never blank space to stand in for.
 * Replacing a hundred real cards with a hundred grey rectangles would throw away
 * something the viewer can already use and make the screen *slower*, not faster.
 *
 * The surface that genuinely is blank is a logo tile — see [LogoSkeleton].
 */

/**
 * A static placeholder block.
 *
 * @param shape the outline. Defaults to the app's small-card radius so a bare block
 *   and a bare card do not look like they came from different apps.
 * @param hairline draws the 1dp top edge a card carries. It is what makes a group of
 *   skeletons read as cards rather than as a grey field, and it costs one line.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = M3ExpressiveShapes.SmallCard,
    hairline: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(LocalAppColors.current.surfaceHigh)
    ) {
        if (hairline) {
            // Inset by the corner radius so the line stops where the curve begins.
            TopEdgeHighlight(inset = SkeletonCorner)
        }
    }
}

/**
 * A neutral placeholder for a logo tile that is still loading.
 *
 * This is deliberately *not* the channel's monogram. The monogram is content — it is
 * what a channel with no logo is supposed to look like — so using it as a loading
 * placeholder means drawing something real and then taking it away, and on a grid of
 * several hundred that is a screen of tiles all changing their minds. See
 * [ChannelLogo] for the full argument.
 */
@Composable
fun LogoSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = M3ExpressiveShapes.LogoTile
) {
    SkeletonBlock(modifier = modifier, shape = shape, hairline = false)
}

/** [M3ExpressiveShapes.SmallCard]'s radius as a number, for [TopEdgeHighlight]. */
private val SkeletonCorner = 14.dp
