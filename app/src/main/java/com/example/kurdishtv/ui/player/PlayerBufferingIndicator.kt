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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.components.BouncingLoader
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.LocalReduceMotion
import com.example.kurdishtv.ui.motion.rememberMorphingCorners
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/** The scale the panel rests at, and the value a reduced-motion viewer sees. */
private const val RESTING_SCALE = 1f

@Composable
fun PlayerBufferingIndicator(
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val reduceMotion = LocalReduceMotion.current

    // The breath, as a `State` read in the *draw* phase.
    //
    // It used to be a `by` delegate, which is a composition read, and then handed to
    // `Modifier.scale(pulseScale)` — a second composition read, because `scale` takes
    // its value as an argument. So an 800ms loop recomposed this whole composable —
    // surface, row, loader, two text nodes — sixty times a second, on the one screen
    // the viewer is already waiting on. The sibling [BouncingLoader] documents this
    // exact trap in its own comments; this was the same mistake next door.
    //
    // Under reduced motion no transition is created at all. `rememberInfiniteTransition`
    // cannot be switched off the way a `LaunchedEffect` can, so the `if` is on the
    // composable call rather than on its result.
    val pulse: State<Float>
    if (reduceMotion) {
        pulse = remember { mutableFloatStateOf(RESTING_SCALE) }
    } else {
        val transition = rememberInfiniteTransition(label = "pulse")
        pulse = transition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    }

    // The panel's silhouette changes once, as it appears, instead of chasing the
    // breath frame by frame.
    //
    // It used to be driven by `isActive = pulseScale > 1f`, which is a *boolean*
    // derived from a value reversing twice a second, fed to a spring. That cannot
    // work: a spring retargeted mid-flight at that rate never converges, so the
    // corners spent the whole time lagging behind the square wave, opening on the
    // way up and snapping shut on the way down. It read as a glitch rather than as
    // breathing. A spring wants a target that holds still long enough to be reached;
    // this one does not have one, so it gets an entrance instead and then rests.
    //
    // The size breath above is what carries the "alive" reading from frame to frame,
    // and it is the part that can be driven continuously.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val panelShape = rememberMorphingCorners(
        rest = M3ExpressiveShapes.Corners.extraLarge,
        active = M3ExpressiveShapes.Corners.extraLarge.copy(
            topStart = 44.dp,
            topEnd = 44.dp,
            bottomEnd = 22.dp,
            bottomStart = 22.dp
        ),
        isActive = shown && !reduceMotion,
        spec = ExpressiveMotion.spatialDefault
    )

    Surface(
        shape = panelShape,
        color = colors.surface.copy(alpha = 0.90f),
        modifier = modifier
            // Read inside the layer lambda, so a frame of the breath redraws this one
            // node and recomposes nothing at all.
            .graphicsLayer {
                val s = pulse.floatValue
                scaleX = s
                scaleY = s
            }
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
