package com.example.kurdishtv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.ui.motion.CornerScale

/**
 * The app's shape vocabulary, in two halves.
 *
 * ── The corner scale ────────────────────────────────────────────────────────
 *
 * Every named token below [ExtraLargeRounded] is a `RoundedCornerShape` on a single
 * consistent scale. These are the shapes used for anything that holds a glyph, a label
 * or a touch target, and their uniformity is the reason: a consistent rhythm of corner
 * radii is what makes adjacent surfaces read as one system, and it guarantees a label
 * inside one is never clipped by a concave corner.
 *
 * They are no longer static, either. The scale is mirrored in [CornerScale] so the
 * corners themselves can be animated — Material lists rounded corners as a spatial
 * spring property, so a card that rounds further as it takes focus is the spec's own
 * idea, not a trick.
 *
 * ── The expressive library ──────────────────────────────────────────────────
 *
 * Everything below the corner scale mirrors [M3ExpressivePolygons] and is exposed as
 * an ordinary `Shape` for call sites that want a fixed outline. For anything that
 * *changes* outline, use `rememberMorphingPolygon` in `com.example.kurdishtv.ui.motion`
 * instead — a shape built here is a still frame of the library.
 *
 * The rule for choosing between the two halves is not taste, it is legibility: a lobed
 * silhouette pushed onto a surface carrying text clips the text and overlaps its
 * neighbours. So lobed shapes go on surfaces that hold only an image, or only
 * animation, and never on a control.
 */
object M3ExpressiveShapes {

    // ── Corner scale: the shapes that hold text ──────────────────────────────

    val ExtraLargeRounded: Shape = RoundedCornerShape(32.dp)
    val LargeCard: Shape = RoundedCornerShape(26.dp)
    val MediumCard: Shape = RoundedCornerShape(20.dp)
    val SmallCard: Shape = RoundedCornerShape(14.dp)
    val Chip: Shape = RoundedCornerShape(12.dp)
    val BadgePill: Shape = RoundedCornerShape(10.dp)
    val SectionPill: Shape = RoundedCornerShape(18.dp)

    /** Rounded-square tile used by channel logos. */
    val LogoTile: Shape = RoundedCornerShape(22.dp)

    val Cookie: Shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp, bottomStart = 16.dp
    )
    val Clover: Shape = RoundedCornerShape(
        topStart = 28.dp, topEnd = 12.dp, bottomEnd = 28.dp, bottomStart = 12.dp
    )
    val SunnyCard: Shape = RoundedCornerShape(
        topStart = 32.dp, topEnd = 32.dp, bottomEnd = 8.dp, bottomStart = 32.dp
    )
    val AsymmetricHero: Shape = RoundedCornerShape(
        topStart = 28.dp, topEnd = 16.dp, bottomEnd = 28.dp, bottomStart = 20.dp
    )

    /**
     * The same scale, as animatable corner radii.
     *
     * `rememberMorphingCorners` takes two of these and springs between them, so a
     * surface can round further as it gains focus without a second shape being
     * authored for it.
     */
    object Corners {
        val extraLarge = CornerScale.uniform(32.dp)
        val largeCard = CornerScale.uniform(26.dp)
        val mediumCard = CornerScale.uniform(20.dp)
        val smallCard = CornerScale.uniform(14.dp)
        val chip = CornerScale.uniform(12.dp)
        val logoTile = CornerScale.uniform(22.dp)

        /** A card that opens up as it takes focus. */
        val cardFocused = CornerScale.uniform(30.dp)

        /** A control pressed flat, then released back. */
        val pressed = CornerScale.uniform(10.dp)
    }

    // ── Geometric foundations ────────────────────────────────────────────────

    val Circle: Shape = M3ExpressivePolygons.Circle.toShape()
    val Square: Shape = M3ExpressivePolygons.Square.toShape()
    val Slanted: Shape = M3ExpressivePolygons.Slanted.toShape()
    val Arch: Shape = M3ExpressivePolygons.Arch.toShape()
    val SemiCircle: Shape = M3ExpressivePolygons.SemiCircle.toShape()
    val Oval: Shape = M3ExpressivePolygons.Oval.toShape()
    val Pill: Shape = M3ExpressivePolygons.Pill.toShape()

    /** A pill with a pinch in it, for selection states that should not change hue. */
    val MorphingPill: Shape = M3ExpressivePolygons.Pill.scaled(1f, 0.82f).toShape()

    // ── Directional / pointed ────────────────────────────────────────────────

    val Triangle: Shape = M3ExpressivePolygons.Triangle.toShape()
    val Arrow: Shape = M3ExpressivePolygons.Arrow.toShape()
    val Fan: Shape = M3ExpressivePolygons.Fan.toShape()

    // ── Faceted ──────────────────────────────────────────────────────────────

    val Diamond: Shape = M3ExpressivePolygons.Diamond.toShape()
    val Clamshell: Shape = M3ExpressivePolygons.ClamShell.toShape()
    val Pentagon: Shape = M3ExpressivePolygons.Pentagon.toShape()
    val Gem: Shape = M3ExpressivePolygons.Gem.toShape()

    // ── Lobed / radial ───────────────────────────────────────────────────────

    val VerySunny: Shape = M3ExpressivePolygons.VerySunny.toShape()
    val Sunny: Shape = M3ExpressivePolygons.Sunny.toShape()
    val FourSidedCookie: Shape = M3ExpressivePolygons.Cookie4Sided.toShape()
    val SixSidedCookie: Shape = M3ExpressivePolygons.Cookie6Sided.toShape()
    val SevenSidedCookie: Shape = M3ExpressivePolygons.Cookie7Sided.toShape()
    val NineSidedCookie: Shape = M3ExpressivePolygons.Cookie9Sided.toShape()
    val TwelveSidedCookie: Shape = M3ExpressivePolygons.Cookie12Sided.toShape()
    val FourLeafClover: Shape = M3ExpressivePolygons.Clover4Leaf.toShape()
    val EightLeafClover: Shape = M3ExpressivePolygons.Clover8Leaf.toShape()
    val Burst: Shape = M3ExpressivePolygons.Burst.toShape()
    val SoftBurst: Shape = M3ExpressivePolygons.SoftBurst.toShape()
    val Boom: Shape = M3ExpressivePolygons.Boom.toShape()
    val SoftBoom: Shape = M3ExpressivePolygons.SoftBoom.toShape()
    val Flower: Shape = M3ExpressivePolygons.Flower.toShape()
    val Puffy: Shape = M3ExpressivePolygons.Puffy.toShape()
    val PuffyDiamond: Shape = M3ExpressivePolygons.PuffyDiamond.toShape()
    val Ghostish: Shape = M3ExpressivePolygons.Ghostish.toShape()
    val Bun: Shape = M3ExpressivePolygons.Bun.toShape()

    // ── Pixel ────────────────────────────────────────────────────────────────

    val PixelCircle: Shape = M3ExpressivePolygons.PixelCircle.toShape()
    val PixelTriangle: Shape = M3ExpressivePolygons.PixelTriangle.toShape()
    val Heart: Shape = M3ExpressivePolygons.Heart.toShape()
}

/**
 * MaterialTheme shapes mapping (extraSmall → extraLarge).
 *
 * Named `AppShapes` rather than `Shapes` so it cannot shadow the
 * `androidx.compose.material3.Shapes` type it is built from, and so call sites in
 * other packages have to import it explicitly.
 *
 * The scale is deliberately gentle. The Expressive update pushed Material's default
 * radii much higher, but this app puts a 13sp label inside most of its buttons, and a
 * 28dp radius on a 36dp-tall control leaves barely anything of the glyph. So the
 * expressive half of the system lives in the shape library and in animated corners,
 * where it can be applied to surfaces that can carry it.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
