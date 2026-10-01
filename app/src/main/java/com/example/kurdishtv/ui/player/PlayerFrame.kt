package com.example.kurdishtv.ui.player

import androidx.compose.ui.geometry.Size

/**
 * The aspect ratio assumed before the first decoded frame reports its real one.
 *
 * 16:9 is what essentially every broadcast in the catalogue is encoded as, so it is
 * the right guess for the window between "the player is on screen" and "ExoPlayer
 * has told us the dimensions" — and getting it wrong for that window is invisible,
 * because the bar is only on screen while a stream is loading anyway.
 */
const val DEFAULT_VIDEO_ASPECT = 16f / 9f

/**
 * Where the video is actually drawn inside the player container, in the same pixel
 * units as the [Size] it was computed from.
 *
 * The player fills the whole window with a black `Box` and lets `PlayerView` letterbox
 * the picture inside it. The *screen* is therefore not the *picture*: on a phone held
 * upright, a 16:9 stream occupies a horizontal band across the middle and everything
 * above and below it is black bars. The transport overlay used to be laid out against
 * the screen, which put the channel name in the top black bar, the control bar in the
 * bottom one, and a row of floating buttons across an expanse of nothing with the
 * picture nowhere near any of them.
 */
data class PlayerFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

/**
 * The rectangle the video occupies in [container] under [resizeMode].
 *
 * Centred in both axes, which is what `AspectRatioFrameLayout` does for every mode.
 *
 * FIT contains the whole picture, so the result is a band that may be narrower or
 * shorter than the window. FILL and ZOOM both *cover* the window and crop whatever
 * overflows, so in both cases the visible frame is the container itself and the
 * distinction between them is a matter of how much is cropped — which does not
 * change where a control bar belongs.
 *
 * Every branch is guarded against a zero or non-finite input, because [container]
 * comes from a measured layout and a video's aspect arrives from a decoder: both are
 * briefly zero, and dividing by the first of those would produce a frame with `NaN`
 * offsets, which Compose renders as content drawn at infinity.
 */
fun playerFrame(
    container: Size,
    videoAspect: Float,
    resizeMode: ResizeMode
): PlayerFrame {
    val containerWidth = container.width
    val containerHeight = container.height

    if (containerWidth <= 0f || containerHeight <= 0f) {
        return PlayerFrame(0f, 0f, containerWidth.coerceAtLeast(0f), containerHeight.coerceAtLeast(0f))
    }

    if (resizeMode != ResizeMode.FIT) {
        return PlayerFrame(0f, 0f, containerWidth, containerHeight)
    }

    val aspect =
        if (videoAspect.isFinite() && videoAspect > 0f) videoAspect else DEFAULT_VIDEO_ASPECT

    // The standard "contain" fit: the limiting axis is whichever of `container / 1`
    // and `container / aspect` is smaller, and the other follows from the ratio.
    val fittedWidth = minOf(containerWidth, aspect * containerHeight)
    val fittedHeight = fittedWidth / aspect

    return PlayerFrame(
        left = (containerWidth - fittedWidth) / 2f,
        top = (containerHeight - fittedHeight) / 2f,
        width = fittedWidth,
        height = fittedHeight
    )
}
