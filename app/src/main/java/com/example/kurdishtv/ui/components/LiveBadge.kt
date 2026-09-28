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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
 */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val pulse = LocalLivePulse.current

    // The dot breathes by *scaling*, not by changing shape.
    //
    // It used to flatten its corner radii with the pulse, which meant each badge held
    // its own `animateFloatAsState` and allocated a fresh `RoundedCornerShape` on every
    // frame of a twice-a-second animation — per card, across a grid that holds several
    // hundred of them. A transform on a circle is the same breath for one matrix and
    // no allocation, and at 8dp the two are indistinguishable.
    val dotScale = pulse

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
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
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
