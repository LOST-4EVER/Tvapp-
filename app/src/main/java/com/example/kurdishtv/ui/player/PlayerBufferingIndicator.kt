package com.example.kurdishtv.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.components.BouncingLoader
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.rememberMorphingCorners
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

@Composable
fun PlayerBufferingIndicator(
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // The panel breathes in silhouette as well as in size. Shape is a state channel
    // in Expressive, and a buffering notice that changes outline reads as *alive*
    // where one that merely pulses reads as a blinking light. It stays on the corner
    // scale, because the panel carries two lines of text.
    val panelShape = rememberMorphingCorners(
        rest = M3ExpressiveShapes.Corners.extraLarge,
        active = M3ExpressiveShapes.Corners.extraLarge.copy(
            topStart = 44.dp,
            topEnd = 44.dp,
            bottomEnd = 22.dp,
            bottomStart = 22.dp
        ),
        isActive = pulseScale > 1f,
        spec = ExpressiveMotion.spatialDefault
    )

    Surface(
        shape = panelShape,
        color = colors.surface.copy(alpha = 0.90f),
        modifier = modifier
            .scale(pulseScale)
            .border(1.dp, colors.border, panelShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            BouncingLoader(size = 22.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Connecting stream…",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Live HLS buffer",
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
