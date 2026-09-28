package com.example.kurdishtv.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.CornerScale
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.motion.rememberMorphingCorners
import com.example.ui.theme.LocalAppColors

/**
 * Visual treatments shared by every round icon control in the app.
 *
 * The app has three icon-button families — the channel-list header, the navigation
 * rail and the video transport controls — and all of them are the same thing: a
 * circular surface, a hairline border, a centred glyph and a springy press. Each used
 * to re-implement that inline, which is how the rail drifted to a spiked silhouette
 * and the header to a different tint. One enum, one renderer.
 */
enum class AppIconButtonStyle {
    /** Solid accent, dark glyph. Reserved for the single most important action. */
    Filled,

    /** Elevated surface, accent glyph. The default for app chrome. */
    Tonal,

    /** Translucent scrim, light glyph. For controls floating over video. */
    Glass,

    /** Transparent with a hairline ring. For secondary actions in a row. */
    Outline
}

/**
 * The app's only round icon button.
 *
 * Everything that renders an icon inside a tappable circle goes through here, so press
 * feedback, glyph size and the border treatment stay identical across screens.
 *
 * The shape is the interesting part. The button rests as a circle and squares off
 * while it is held. A circle that becomes a rounded square under a finger is the
 * clearest possible reading of "this is being pressed", and unlike a ripple it is still
 * visible from across a room and to someone who has reduced motion switched on at the
 * system level.
 *
 * It is a circle-to-squircle morph on the corner scale, not a lobed polygon. A control
 * carries a glyph, and a control's outline is also its **clip**: every one of these
 * buttons is a `Surface`, so whatever the shape is, the glyph is drawn inside that same
 * outline. That is why the Expressive library came off the controls entirely — a
 * polygon whose arc bulged across its own outline clipped its own icon away, and the
 * button rendered as an empty disc.
 */
@Composable
fun AppIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppIconButtonStyle = AppIconButtonStyle.Tonal,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    scaleDown: Float = 0.90f,
    active: Boolean = false
) {
    val colors = LocalAppColors.current

    // An "active" control (muted audio, an active sleep timer, a favourite) keeps its
    // identity but tints the surface and the glyph with the accent, so the current
    // state is visible without a menu.
    val container = when {
        active && style != AppIconButtonStyle.Filled -> colors.primary.copy(alpha = 0.28f)
        style == AppIconButtonStyle.Filled -> colors.primary
        style == AppIconButtonStyle.Tonal -> colors.surfaceElevated
        style == AppIconButtonStyle.Glass -> colors.glass
        else -> Color.Transparent
    }
    val content = when {
        style == AppIconButtonStyle.Filled -> colors.onPrimary
        // Glass floats over video, where the palette's text colours are invisible.
        style == AppIconButtonStyle.Glass -> Color.White
        else -> colors.primary
    }

    // The press state is read here rather than inside `bouncyClickable` so the outline
    // can react to it; the modifier handles the scale, this handles the silhouette.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Sized as a fraction of this button's own [size], not a fixed dp radius: the
    // same token is a circle at 40dp, at 44dp and at the rail's 36dp, and the clamp
    // in `cornerRadius` means no caller can request a radius its button is too small
    // to carry. A hard-coded 20dp was a circle on one of those and a broken shape on
    // another.
    val shape: Shape = rememberMorphingCorners(
        rest = CornerScale.uniform(ShapeMorph.cornerRadius(size, 0.5f)),
        active = CornerScale.uniform(ShapeMorph.cornerRadius(size, 0.28f)),
        isActive = isPressed,
        spec = ExpressiveMotion.spatialFast
    )

    Surface(
        shape = shape,
        color = container,
        modifier = modifier
            .size(size)
            // A filled circle needs no ring; the silhouette is already the shape.
            .then(
                if (style == AppIconButtonStyle.Filled) {
                    Modifier
                } else {
                    Modifier.border(1.dp, colors.border, shape)
                }
            )
            .bouncyClickable(
                scaleDown = scaleDown,
                interactionSource = interactionSource,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            SvgIcon(
                resId = iconRes,
                contentDescription = contentDescription,
                tint = content,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * A labelled pill that squashes under a finger.
 *
 * The other half of the app's control family. [AppIconButton] is for a glyph alone;
 * this is for a glyph *and* a word, which is why its shape stays a pill at rest — a
 * rounded square around a 12sp label looks like a mistake, not a shape. What it does
 * share is the press behaviour: the outline flattens on the way in and springs back on
 * release, so a press over video is visible even when the controls fade out.
 */
@Composable
fun SquishyPillButton(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color = Color.White,
    iconSize: Dp = 16.dp,
    fontSize: TextUnit = 12.sp,
    active: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // A percent radius rather than a dp one: this pill is ~30dp tall, and half of
    // that is 15dp, so the 20dp "active" radius it used to take made its own corners
    // overlap. A pill is a pill at every size, and this is a pill.
    val shape: Shape = ShapeMorph.pill

    Surface(
        shape = shape,
        color = containerColor,
        modifier = modifier.bouncyClickable(
            interactionSource = interactionSource,
            onClick = onClick
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SvgIcon(
                resId = iconRes,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
