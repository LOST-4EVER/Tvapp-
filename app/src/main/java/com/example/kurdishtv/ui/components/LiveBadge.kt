package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.LocalLivePulse
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/**
 * LIVE chip.
 *
 * The pulse comes from a single app-wide animation ([LocalLivePulse]), so a grid full
 * of cards runs one transition rather than one per card — which matters here, because
 * the pulse is read on every frame by every badge on screen.
 *
 * The dot is a plain circle that scales very slightly as it breathes. It was a lobed
 * sun before. At 8dp an eight-lobe outline is not "recognisably round", it is a
 * smudge, and it was being rebuilt on every pulse for every visible card; a scale is
 * the same idea for a fraction of the cost and is legible at that size.
 *
 * The breathe is the only thing in this composable that changes between frames, and it
 * is applied entirely in the draw phase, so a badge never recomposes as it pulses —
 * which matters here more than anywhere else in the app, because there is one on every
 * card in the grid.
 */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current

    // A `MutableFloatState`, not a `Float`: reading it here instead of in the draw
    // lambda below would recompose this whole composable — surface, row, text and all —
    // on every frame of the pulse, for every badge on screen.
    val pulse = LocalLivePulse.current

    Surface(
        shape = M3ExpressiveShapes.BadgePill,
        color = colors.liveRed.copy(alpha = 0.20f),
        modifier = modifier.border(1.dp, colors.liveRed.copy(alpha = 0.35f), M3ExpressiveShapes.BadgePill)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    // The breath, as a canvas transform applied while drawing.
                    //
                    // `Modifier.graphicsLayer` would need a render node for every
                    // badge on screen — one per card, in a grid of several hundred.
                    // The pulse is read in the draw phase either way, so no badge
                    // recomposes as it breathes; this version also costs no render
                    // node, just a matrix around one 8dp circle.
                    .drawWithContent {
                        val breath = pulse.floatValue
                        scale(breath, breath, center) { this@drawWithContent.drawContent() }
                    }
                    .background(colors.liveRed, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
        }
    }
}
