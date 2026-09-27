package com.example.kurdishtv.ui.player

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Colour treatments for the video surface.
 *
 * Broadcast streams are frequently over-saturated or washed out, and some are
 * encoded brighter than the display can show. These presets let the viewer correct
 * that without leaving the app.
 *
 * Each is expressed as a 4x5 colour matrix, which is applied to the decoded video
 * frame rather than to a screenshot, so it costs no extra decode work.
 */
enum class VideoColorFilter(val label: String) {
    /** No correction. */
    None("Normal"),

    /** Lifts contrast and saturation, which most compressed streams need. */
    Vivid("Vivid"),

    /** Warms the image and lifts the shadows. Comfortable at night. */
    Warm("Warm"),

    /** Cools the image and reduces saturation. Useful for a bright room. */
    Cool("Cool"),

    /** Full desaturation. */
    Mono("Mono"),

    /** Boosts contrast and drops brightness; best for very dark sources. */
    Night("Night");

    /** The next preset in the cycle, for a single-button control. */
    fun next(): VideoColorFilter = entries[(ordinal + 1) % entries.size]

    val isActive: Boolean get() = this != None

    /**
     * The colour matrix for this preset, or null when no correction is needed so
     * the default rendering path is left completely untouched.
     */
    fun toColorFilter(): ColorFilter? {
        val matrix = when (this) {
            None -> return null
            // Saturate about 1.45x and add a touch of contrast.
            Vivid -> floatArrayOf(
                1.22f, -0.11f, -0.11f, 0f, 0f,
                -0.08f, 1.16f, -0.08f, 0f, 0f,
                -0.08f, -0.08f, 1.16f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            // Warm: push red up, blue down.
            Warm -> floatArrayOf(
                1.14f, 0.02f, -0.04f, 0f, 6f,
                0.03f, 1.02f, -0.02f, 0f, 2f,
                -0.06f, -0.02f, 0.92f, 0f, -4f,
                0f, 0f, 0f, 1f, 0f
            )
            // Cool: the opposite, with saturation pulled back a little.
            Cool -> floatArrayOf(
                0.94f, 0f, 0.04f, 0f, -2f,
                0f, 1.0f, 0.03f, 0f, 0f,
                0.02f, 0.04f, 1.12f, 0f, 4f,
                0f, 0f, 0f, 1f, 0f
            )
            // Mono uses the standard luminance weights.
            Mono -> floatArrayOf(
                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            // Night: more contrast, less overall brightness.
            Night -> floatArrayOf(
                1.18f, -0.09f, -0.09f, 0f, -14f,
                -0.09f, 1.18f, -0.09f, 0f, -14f,
                -0.09f, -0.09f, 1.18f, 0f, -14f,
                0f, 0f, 0f, 1f, 0f
            )
        }
        return ColorFilter.colorMatrix(ColorMatrix(matrix))
    }
}
