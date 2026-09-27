package com.example.kurdishtv.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import kotlin.math.abs

/**
 * A pill whose silhouette animates when it becomes selected.
 *
 * Material 3 Expressive treats shape as a state channel: an element can signal
 * selection by changing its outline, not only its colour. For filter chips that
 * reads more clearly than a colour swap alone, and it costs one spring.
 *
 * The morph is done by animating the corner radius between fully-rounded and a
 * slightly pinched pill, so the chip appears to squeeze and settle. The same
 * spring used for press feedback is reused to keep the motion consistent.
 */
@Composable
fun rememberMorphingPillShape(
    selected: Boolean,
    reduceMotion: Boolean = false
): Shape {
    // 0 = resting pill, 1 = fully pinched.
    val target = if (selected) 0.42f else 0f
    val amount by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reduceMotion) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "PillShapeMorph"
    )

    // Overshoot past the target briefly squeezes harder, which is what makes the
    // shape feel like it has weight rather than just changing size.
    val overshoot = abs(amount) * 1.08f
    val radiusPercent = (50f - overshoot * 22f).coerceIn(0f, 50f)

    return RoundedCornerShape(
        topStartPercent = radiusPercent.toInt(),
        topEndPercent = radiusPercent.toInt(),
        bottomEndPercent = (radiusPercent * 0.78f).toInt(),
        bottomStartPercent = (radiusPercent * 0.78f).toInt()
    )
}
