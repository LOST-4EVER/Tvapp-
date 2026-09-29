package com.example.kurdishtv.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.tvClickable
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
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
 * The outline is a circle and stays a circle. It used to square off into a rounded
 * rectangle while held, on a spring — a circle that becomes a squircle is a good
 * reading of "pressed", and it is gone now, so the press is carried by a flat tint
 * laid over the same circle instead. Same instant acknowledgement, no animation, and
 * the outline never changes shape under a glyph.
 *
 * The glyph is drawn into a plain `Box` that the shape only clips — never owns. That
 * separation is the point: a control's outline is also its clip, and a shape that is
 * even slightly wrong then deletes the icon along with the outline. See the comment
 * on the `Box` below.
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
    active: Boolean = false
) {
    val colors = LocalAppColors.current

    // An "active" control (muted audio, an active sleep timer, a favourite) keeps its
    // identity but tints the surface and the glyph with the accent, so the current
    // state is visible without a menu.
    val container = when {
        active && style != AppIconButtonStyle.Filled -> colors.primary.copy(alpha = 0.28f)
        style == AppIconButtonStyle.Filled -> colors.primary
        style == AppIconButtonStyle.Tonal -> colors.surfaceHigh
        style == AppIconButtonStyle.Glass -> colors.glass
        else -> Color.Transparent
    }
    val content = when {
        style == AppIconButtonStyle.Filled -> colors.onPrimary
        // Glass floats over video, where the palette's text colours are invisible.
        style == AppIconButtonStyle.Glass -> Color.White
        else -> colors.primary
    }

    // The press state is read here rather than inside the click modifier so the
    // container can tint along with it.
    val interactionSource = remember { MutableInteractionSource() }

    // Sized as a fraction of this button's own [size], not a fixed dp radius: the
    // same token is a circle at 40dp, at 44dp and at the rail's 36dp, and the clamp
    // in `cornerRadius` means no caller can request a radius its button is too small
    // to carry. A hard-coded 20dp was a circle on one of those and a broken shape on
    // another.
    // The outline is a circle, and stays a circle whether the button is held or not.
    // It used to flatten from 0.5 to 0.28 of the button's short side on a spring.
    val shape: Shape = CornerScale.uniform(ShapeMorph.cornerRadius(size, 0.5f)).toShape()

    // The background is drawn with `background(color, shape)` and the glyph is a
    // plain child of the Box.
    //
    // `Surface(shape, color) { glyph }` was the previous arrangement, and it is the
    // one arrangement that cannot be got right by reasoning: `Surface` clips its
    // content to the shape it is given, so the glyph lives inside the same path as
    // the fill. Any error in that path — a radius that exceeded half the control's
    // short side, a polygon arc that swung across the outline — does not merely make
    // the button look wrong, it deletes the icon and leaves a bare disc. That is
    // what the three header buttons and the card hearts were rendering as.
    //
    // Here the two are independent: the background follows the shape and the glyph
    // is centred inside it. A wrong shape now costs a wrong outline and nothing
    // else.
    Box(
        modifier = modifier
            .size(size)
            // Deliberately **not** clipped. The focus ring below is drawn outset
            // from this Box, and a `clip` applied before it in the chain removes
            // everything outside the element's own bounds — which for a ring is all
            // of it. The glyph does not need the clip either: it is 16-19dp inside a
            // 36-44dp circle, so it is well within the inscribed square at every size
            // this is called with.
            .background(container, shape)
            // A filled circle needs no ring; the silhouette is already the shape.
            .then(
                if (style == AppIconButtonStyle.Filled) {
                    Modifier
                } else {
                    Modifier.border(1.dp, colors.border, shape)
                }
            )
            .tvClickable(
                interactionSource = interactionSource,
                pressedFill = content.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                pressedShape = shape,
                onClick = onClick
            )
            // This control had **no focus indication at all** for a while. The
            // springy click used to scale it 4% larger when the D-pad landed on it,
            // so removing the scale removed the only thing that said "you are here"
            // — on the player's transport controls, which are reached by remote and
            // sit over video where a 1dp border is invisible. The ring is what
            // carries focus here now.
            //
            // It is a circle, because the control is one, and it is drawn in the
            // glyph's own colour: a `Glass` button over video gets a white ring,
            // which is the only thing about it that stays legible against whatever
            // is playing.
            .expressiveFocusRing(
                ringColor = content,
                interactionSource = interactionSource,
                scrim = colors.focusScrim,
                restShape = M3ExpressivePolygons.Circle,
                ringShape = M3ExpressivePolygons.Circle
            ),
        contentAlignment = Alignment.Center
    ) {
        SvgIcon(
            resId = iconRes,
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * A labelled pill.
 *
 * The other half of the app's control family. [AppIconButton] is for a glyph alone;
 * this is for a glyph *and* a word, which is why its shape is a pill — a rounded
 * square around a 12sp label looks like a mistake, not a shape. What it shares is the
 * press behaviour, so a press over video is still visible.
 *
 * Passing a blank [label] is supported and deliberate: the control then degrades to a
 * fixed-size round icon button rather than to a pill with a hole where its text
 * should be.
 */
@Composable
fun LabelPillButton(
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
    // A percent radius rather than a dp one: this pill is ~30dp tall, and half of
    // that is 15dp, so the 20dp "active" radius it used to take made its own corners
    // overlap. A pill is a pill at every size, and this is a pill.
    val shape: Shape = ShapeMorph.pill
    val colors = LocalAppColors.current

    // A blank label used to render as `icon + 5dp gap + an empty Text` inside a 50%
    // radius, which is a circle roughly one glyph wide: a bare disc sitting in a
    // control bar next to a labelled pill and an icon button, reading as a rendering
    // fault rather than as a control. It reached the player as a filled accent
    // circle, because that is exactly the colour the "a colour filter is active"
    // state uses.
    //
    // A label is what makes this a pill at all, so without one the honest thing to
    // render is a fixed-size round icon button: same glyph, same press, an actual
    // hit target, and nothing that looks like a control that failed to render its
    // text.
    val hasLabel = label.isNotBlank()

    // Shared by the click and the ring, so the two cannot disagree about where the
    // viewer is.
    val pillSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .then(
                if (hasLabel) {
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                } else {
                    Modifier.size(40.dp)
                }
            )
            .background(containerColor, shape)
            .tvClickable(
                interactionSource = pillSource,
                pressedFill = contentColor.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                pressedShape = shape,
                onClick = onClick
            )
            // See the note on the same ring in [AppIconButton]. These are the
            // labelled controls on the player — reached by remote, over video — and
            // the press fill alone is not a focus mark.
            //
            // No `clip` on this Row either, for the same reason: it would take the
            // ring with it. The label and glyph sit inside the pill's own padding.
            .expressiveFocusRing(
                ringColor = contentColor,
                interactionSource = pillSource,
                scrim = colors.focusScrim,
                restShape = M3ExpressivePolygons.Square,
                ringShape = ShapeMorph.focusRing
            ),
        // Centred when there is no label. A fixed-size Row defaults to
        // `Arrangement.Start`, which would pin a lone glyph against the leading edge
        // of its own circle instead of in the middle of it.
        horizontalArrangement =
            if (hasLabel) Arrangement.spacedBy(5.dp) else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SvgIcon(
            resId = iconRes,
            // These controls are always inside a bar that already names them in
            // text, so the glyph itself is decorative.
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
        // Always composed, never conditionally: a composable called from inside an
        // `if` changes the Row's slot count and Compose cannot reconcile the two
        // states. An empty string draws nothing and measures to nothing, which is
        // the same result the branch would have produced.
        Text(
            text = label,
            color = contentColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
