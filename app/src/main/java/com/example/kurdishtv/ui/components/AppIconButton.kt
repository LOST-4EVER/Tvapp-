package com.example.kurdishtv.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.ui.theme.LocalAppColors

/**
 * Visual treatments shared by every round icon control in the app.
 *
 * The app has three icon-button families — the channel-list header, the
 * navigation rail and the video transport controls — and all of them are the same
 * thing: a circular surface, a hairline border, a centred glyph and a springy
 * press. Each used to re-implement that inline, which is how the rail drifted to a
 * spiked silhouette and the header to a different tint. One enum, one renderer.
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
 * Everything that renders an icon inside a tappable circle should go through here
 * so press feedback, glyph size and the border treatment stay identical across
 * screens. Passing [size] is enough — the glyph is centred automatically.
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

    // An "active" control (muted audio, an active sleep timer, a favourite) keeps
    // its identity but tints the surface and the glyph with the accent, so the
    // current state is visible without a menu.
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

    Surface(
        // Always a circle. The procedural lobed shapes (Burst, Sunny, the cookies)
        // are reserved for decoration with no glyph and no touch target — on a
        // control they read as a cog behind the icon and the lobes overlap the
        // neighbouring buttons.
        shape = CircleShape,
        color = container,
        modifier = modifier
            .size(size)
            // A filled circle needs no ring; the silhouette is already the shape.
            .then(
                if (style == AppIconButtonStyle.Filled) {
                    Modifier
                } else {
                    Modifier.border(1.dp, colors.border, CircleShape)
                }
            )
            .bouncyClickable(scaleDown = scaleDown, onClick = onClick)
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
