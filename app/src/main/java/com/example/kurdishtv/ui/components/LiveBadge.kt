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
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.rememberMorphingCorners
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/**
 * LIVE chip.
 *
 * The pulse comes from a single app-wide animation ([LocalLivePulse]), so a grid full
 * of cards runs one transition rather than one per card — which matters here, because
 * the pulse drives the dot's scale *and* its outline from the same value.
 *
 * The dot is a plain circle that flattens very slightly as it breathes. It was a
 * lobed sun before. At 8dp an eight-lobe outline is not "recognisably round", it is a
 * smudge, and it was being rebuilt on every pulse for every visible card; a corner
 * radius is the same idea for a fraction of the cost and is legible at that size.
 */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val pulse = LocalLivePulse.current

    // Map the shared 0.85→1.25 pulse onto a 0→1 fraction. Driving the shape from the
    // pulse rather than from a second transition is what keeps a grid of badges down
    // to one animation.
    val openness = ((pulse - 0.85f) / 0.40f).coerceIn(0f, 1f)
    val dotShape = rememberMorphingCorners(
        rest = ShapeMorph.liveRest,
        active = ShapeMorph.liveActive,
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
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(colors.liveRed, dotShape)
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
