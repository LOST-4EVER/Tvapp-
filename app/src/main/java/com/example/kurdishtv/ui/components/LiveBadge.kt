package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.LocalLivePulse
import com.example.kurdishtv.ui.motion.rememberMorphingPolygon
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/**
 * LIVE chip.
 *
 * The pulse comes from a single app-wide animation ([LocalLivePulse]), so a grid full
 * of cards runs one transition rather than one per card — which matters here, because
 * the pulse now drives a shape morph as well as a scale.
 *
 * That is the point: the dot is not merely bigger at the peak of the pulse, it opens
 * from a circle into a very slightly lobed sun and rocks a few degrees as it does. A
 * dot that only scales reads as a heartbeat; a dot that changes outline reads as
 * *live*, and the two are indistinguishable in a still screenshot but not in
 * peripheral vision, which is how a television is actually watched.
 *
 * Both signals are derived from the same shared value, so the extra shape cost per
 * card is one polygon rebuild on an eight-lobe outline.
 */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val pulse = LocalLivePulse.current

    // Map the shared 0.85→1.25 pulse onto a 0→1 morph fraction. Driving the shape from
    // the pulse rather than from a second transition is what keeps a grid of badges
    // down to one animation.
    val openness = ((pulse - 0.85f) / 0.40f).coerceIn(0f, 1f)
    val dotShape = rememberMorphingPolygon(
        rest = M3ExpressivePolygons.Circle,
        active = M3ExpressivePolygons.VerySunny,
        isActive = openness > 0.5f,
        // Critically damped: this morphs twice a second, and an overshooting spring
        // at that cadence reads as a flicker rather than as a breath.
        spec = ExpressiveMotion.effectsFast
    )

    Surface(
        shape = M3ExpressiveShapes.BadgePill,
        color = colors.liveRed.copy(alpha = 0.20f),
        modifier = modifier.border(1.dp, colors.liveRed.copy(alpha = 0.35f), M3ExpressiveShapes.BadgePill)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    // The lobed shape is what makes this safe: a 12-lobe burst at 8dp
                    // was an unreadable blob, but an eight-lobe sun with a corner
                    // radius of 0.085 is still recognisably round at that size.
                    .background(colors.liveRed, dotShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
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
