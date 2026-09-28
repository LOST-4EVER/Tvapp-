package com.example.kurdishtv.ui.theme

import androidx.compose.ui.geometry.Offset

/**
 * The Material 3 Expressive shape library, as morphable polygons.
 *
 * Google added 35 shapes to Material in the Expressive update — cookies, clovers,
 * bursts, flowers, hearts — and made them *morphable*, which is the part that matters
 * for an app: a shape is no longer a fixed outline, it is something an element can
 * transition between as its state changes.
 *
 * The definitions here are a direct port of `androidx.compose.material3.MaterialShapes`,
 * AndroidX's own copy of the library, expressed in this app's [ExpressivePolygon] so
 * they can be interpolated and rotated at will. The vertex and rounding values are
 * unchanged, so the silhouettes match the reference.
 *
 * ── Where these belong ──────────────────────────────────────────────────────
 *
 * A lobed silhouette only works where nothing has to be read off the surface. On a
 * surface carrying a glyph, a label or a touch target it produces three defects: the
 * points straddle the element's neighbour so adjacent controls merge; the bounding box
 * is uneven, so the glyph leaves the safe area; and a concave corner clips the first
 * character of a line of text.
 *
 * So the rule the app follows is:
 *
 *  - a surface that holds a glyph, a label or a touch target uses the rounded-corner
 *    scale in [M3ExpressiveShapes], animated with the corner-morph helpers in
 *    `com.example.kurdishtv.ui.motion.ShapeMorph`;
 *  - a surface that holds only an image, or only animation, uses this library.
 *
 * That is the same split Material draws: expressive silhouettes for the big, empty,
 * image-carrying moments, and the corner scale for everything you have to read.
 */
object M3ExpressivePolygons {

    // ── Geometric foundations ────────────────────────────────────────────────

    /**
     * A true circle, as a 16-gon whose corners are exactly the circle's own radius.
     *
     * Material approximates this with a 10-gon, but 16 costs almost nothing and
     * avoids the faint faceting a 10-gon shows on a large hero-sized surface.
     */
    val Circle: ExpressivePolygon =
        ExpressivePolygon.regular(sides = 16, corner = ExpressiveCorner(1f))

    /** A rounded square. */
    val Square: ExpressivePolygon = ExpressivePolygon.rect(
        1f, 1f, listOf(ExpressiveCorner(0.3f))
    )

    /** A square with opposing corners pulled apart. */
    val Slanted: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.926f, 0.970f) to ExpressiveCorner(0.189f, 0.811f),
            Offset(-0.021f, 0.967f) to ExpressiveCorner(0.187f, 0.057f)
        ),
        reps = 2
    )

    /** A dome: two full-radius top corners over two small ones. */
    val Arch: ExpressivePolygon = ExpressivePolygon.rect(
        1.414f, 1.414f,
        listOf(
            ExpressiveCorner(1f),
            ExpressiveCorner(1f),
            ExpressiveCorner(0.2f),
            ExpressiveCorner(0.2f)
        )
    )

    /** Half a pill, flat on one side. */
    val SemiCircle: ExpressivePolygon = ExpressivePolygon.rect(
        1.6f, 1f,
        listOf(
            ExpressiveCorner(0.2f),
            ExpressiveCorner(0.2f),
            ExpressiveCorner(1f),
            ExpressiveCorner(1f)
        )
    )

    /** A diagonal oval — long axis on the 45°. */
    val Oval: ExpressivePolygon = Circle.scaled(1f, 0.64f).rotated(-45f)

    /** A capsule. */
    val Pill: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.961f, 0.039f) to ExpressiveCorner(0.426f),
            Offset(1.001f, 0.428f) to ExpressiveCorner.None,
            Offset(1.000f, 0.609f) to ExpressiveCorner(1f)
        ),
        reps = 2,
        mirroring = true
    )

    // ── Directional / pointed ────────────────────────────────────────────────

    /** An upward-pointing rounded triangle. */
    val Triangle: ExpressivePolygon =
        ExpressivePolygon.regular(sides = 3, corner = ExpressiveCorner(0.2f)).rotated(-90f)

    val Arrow: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, 0.892f) to ExpressiveCorner(0.313f),
            Offset(-0.216f, 1.050f) to ExpressiveCorner(0.207f),
            Offset(0.499f, -0.160f) to ExpressiveCorner(0.215f, 1f),
            Offset(1.225f, 1.060f) to ExpressiveCorner(0.211f)
        ),
        reps = 1
    )

    val Fan: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(1.004f, 1.000f) to ExpressiveCorner(0.148f, 0.417f),
            Offset(0.000f, 1.000f) to ExpressiveCorner(0.151f),
            Offset(0.000f, -0.003f) to ExpressiveCorner(0.148f),
            Offset(0.978f, 0.020f) to ExpressiveCorner(0.803f)
        ),
        reps = 1
    )

    // ── Faceted ──────────────────────────────────────────────────────────────

    val Diamond: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, 1.096f) to ExpressiveCorner(0.151f, 0.524f),
            Offset(0.040f, 0.500f) to ExpressiveCorner(0.159f)
        ),
        reps = 2
    )

    val ClamShell: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.171f, 0.841f) to ExpressiveCorner(0.159f),
            Offset(-0.020f, 0.500f) to ExpressiveCorner(0.140f),
            Offset(0.170f, 0.159f) to ExpressiveCorner(0.159f)
        ),
        reps = 2
    )

    val Pentagon: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, -0.009f) to ExpressiveCorner(0.172f),
            Offset(1.030f, 0.365f) to ExpressiveCorner(0.164f),
            Offset(0.828f, 0.970f) to ExpressiveCorner(0.169f)
        ),
        reps = 1,
        mirroring = true
    )

    val Gem: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.499f, 1.023f) to ExpressiveCorner(0.241f, 0.778f),
            Offset(-0.005f, 0.792f) to ExpressiveCorner(0.208f),
            Offset(0.073f, 0.258f) to ExpressiveCorner(0.228f),
            Offset(0.433f, -0.000f) to ExpressiveCorner(0.491f)
        ),
        reps = 1,
        mirroring = true
    )

    // ── Lobed / radial ───────────────────────────────────────────────────────

    val Sunny: ExpressivePolygon = ExpressivePolygon.star(
        points = 8,
        innerRadius = 0.8f,
        corner = ExpressiveCorner(0.15f)
    )

    val VerySunny: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, 1.080f) to ExpressiveCorner(0.085f),
            Offset(0.358f, 0.843f) to ExpressiveCorner(0.085f)
        ),
        reps = 8
    )

    val Cookie4Sided: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(1.237f, 1.236f) to ExpressiveCorner(0.258f),
            Offset(0.500f, 0.918f) to ExpressiveCorner(0.233f)
        ),
        reps = 4
    )

    val Cookie6Sided: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.723f, 0.884f) to ExpressiveCorner(0.394f),
            Offset(0.500f, 1.099f) to ExpressiveCorner(0.398f)
        ),
        reps = 6
    )

    val Cookie7Sided: ExpressivePolygon = ExpressivePolygon.star(
        points = 7,
        innerRadius = 0.75f,
        corner = ExpressiveCorner(0.5f)
    ).rotated(-90f)

    val Cookie9Sided: ExpressivePolygon = ExpressivePolygon.star(
        points = 9,
        innerRadius = 0.8f,
        corner = ExpressiveCorner(0.5f)
    ).rotated(-90f)

    val Cookie12Sided: ExpressivePolygon = ExpressivePolygon.star(
        points = 12,
        innerRadius = 0.8f,
        corner = ExpressiveCorner(0.5f)
    ).rotated(-90f)

    val Ghostish: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, 0f) to ExpressiveCorner(1f),
            Offset(1f, 0f) to ExpressiveCorner(1f),
            Offset(1f, 1.140f) to ExpressiveCorner(0.254f, 0.106f),
            Offset(0.575f, 0.906f) to ExpressiveCorner(0.253f)
        ),
        reps = 1,
        mirroring = true
    )

    val Clover4Leaf: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, 0.074f) to ExpressiveCorner.None,
            Offset(0.725f, -0.099f) to ExpressiveCorner(0.476f)
        ),
        reps = 4,
        mirroring = true
    )

    val Clover8Leaf: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, 0.036f) to ExpressiveCorner.None,
            Offset(0.758f, -0.101f) to ExpressiveCorner(0.209f)
        ),
        reps = 8
    )

    val Burst: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, -0.006f) to ExpressiveCorner(0.006f),
            Offset(0.592f, 0.158f) to ExpressiveCorner(0.006f)
        ),
        reps = 12
    )

    val SoftBurst: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.193f, 0.277f) to ExpressiveCorner(0.053f),
            Offset(0.176f, 0.055f) to ExpressiveCorner(0.053f)
        ),
        reps = 10
    )

    val Boom: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.457f, 0.296f) to ExpressiveCorner(0.007f),
            Offset(0.500f, -0.051f) to ExpressiveCorner(0.007f)
        ),
        reps = 15
    )

    val SoftBoom: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.733f, 0.454f) to ExpressiveCorner.None,
            Offset(0.839f, 0.437f) to ExpressiveCorner(0.532f),
            Offset(0.949f, 0.449f) to ExpressiveCorner(0.439f, 1f),
            Offset(0.998f, 0.478f) to ExpressiveCorner(0.174f)
        ),
        reps = 16,
        mirroring = true
    )

    val Flower: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.370f, 0.187f) to ExpressiveCorner.None,
            Offset(0.416f, 0.049f) to ExpressiveCorner(0.381f),
            Offset(0.479f, 0.001f) to ExpressiveCorner(0.095f)
        ),
        reps = 8,
        mirroring = true
    )

    val Puffy: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, 0.053f) to ExpressiveCorner.None,
            Offset(0.545f, -0.040f) to ExpressiveCorner(0.405f),
            Offset(0.670f, -0.035f) to ExpressiveCorner(0.426f),
            Offset(0.717f, 0.066f) to ExpressiveCorner(0.574f),
            Offset(0.722f, 0.128f) to ExpressiveCorner.None,
            Offset(0.777f, 0.002f) to ExpressiveCorner(0.360f),
            Offset(0.914f, 0.149f) to ExpressiveCorner(0.660f),
            Offset(0.926f, 0.289f) to ExpressiveCorner(0.660f),
            Offset(0.881f, 0.346f) to ExpressiveCorner.None,
            Offset(0.940f, 0.344f) to ExpressiveCorner(0.126f),
            Offset(1.003f, 0.437f) to ExpressiveCorner(0.255f)
        ),
        reps = 2,
        mirroring = true
    ).scaled(1f, 0.742f)

    val PuffyDiamond: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.870f, 0.130f) to ExpressiveCorner(0.146f),
            Offset(0.818f, 0.357f) to ExpressiveCorner.None,
            Offset(1.000f, 0.332f) to ExpressiveCorner(0.853f)
        ),
        reps = 4,
        mirroring = true
    )

    // ── Pixel ────────────────────────────────────────────────────────────────

    /** A stepped circle. Reads as a rounded, low-resolution tile. */
    val PixelCircle: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, 0.000f) to ExpressiveCorner.None,
            Offset(0.704f, 0.000f) to ExpressiveCorner.None,
            Offset(0.704f, 0.065f) to ExpressiveCorner.None,
            Offset(0.843f, 0.065f) to ExpressiveCorner.None,
            Offset(0.843f, 0.148f) to ExpressiveCorner.None,
            Offset(0.926f, 0.148f) to ExpressiveCorner.None,
            Offset(0.926f, 0.296f) to ExpressiveCorner.None,
            Offset(1.000f, 0.296f) to ExpressiveCorner.None
        ),
        reps = 2,
        mirroring = true
    )

    /** A stepped triangle. */
    val PixelTriangle: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.110f, 0.500f) to ExpressiveCorner.None,
            Offset(0.113f, 0.000f) to ExpressiveCorner.None,
            Offset(0.287f, 0.000f) to ExpressiveCorner.None,
            Offset(0.287f, 0.087f) to ExpressiveCorner.None,
            Offset(0.421f, 0.087f) to ExpressiveCorner.None,
            Offset(0.421f, 0.170f) to ExpressiveCorner.None,
            Offset(0.560f, 0.170f) to ExpressiveCorner.None,
            Offset(0.560f, 0.265f) to ExpressiveCorner.None,
            Offset(0.674f, 0.265f) to ExpressiveCorner.None,
            Offset(0.675f, 0.344f) to ExpressiveCorner.None,
            Offset(0.789f, 0.344f) to ExpressiveCorner.None,
            Offset(0.789f, 0.439f) to ExpressiveCorner.None,
            Offset(0.888f, 0.439f) to ExpressiveCorner.None
        ),
        reps = 1,
        mirroring = true
    )

    val Bun: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.796f, 0.500f) to ExpressiveCorner.None,
            Offset(0.853f, 0.518f) to ExpressiveCorner(1f),
            Offset(0.992f, 0.631f) to ExpressiveCorner(1f),
            Offset(0.968f, 1.000f) to ExpressiveCorner(1f)
        ),
        reps = 2,
        mirroring = true
    )

    val Heart: ExpressivePolygon = ExpressivePolygon.fromAuthoredPoints(
        points = listOf(
            Offset(0.500f, 0.268f) to ExpressiveCorner(0.016f),
            Offset(0.792f, -0.066f) to ExpressiveCorner(0.958f),
            Offset(1.064f, 0.276f) to ExpressiveCorner(1f),
            Offset(0.501f, 0.946f) to ExpressiveCorner(0.129f)
        ),
        reps = 1,
        mirroring = true
    )

    /**
     * The seven-shape sequence the Expressive loading indicator cycles through.
     *
     * Material walks a single shape through this list continuously rather than showing
     * seven shapes at once, and so does this app — see `BouncingLoader`.
     */
    val LoadingSequence: List<ExpressivePolygon> = listOf(
        Circle,
        Cookie7Sided,
        VerySunny,
        Flower,
        PuffyDiamond,
        Cookie9Sided,
        Cookie12Sided
    )
}
