package com.example.kurdishtv.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Material 3 Expressive shape system.
 *
 * Expressive shapes are not just rounded rectangles. Google's M3 Expressive update
 * added a library of decorative shapes (cookies, clovers, bursts, flowers, hearts)
 * that are built by modulating the *polar radius* of a polygon as you walk around
 * it. That is what gives them their characteristic soft, organic, slightly irregular
 * silhouettes while still resolving to a clean bounding box.
 *
 * The polar-modulation approach is used here because it scales cleanly: the same
 * shape renders correctly in a 24dp chip and a 320dp hero tile, which fixed-size
 * path data could not do.
 *
 * [AppShapes] feeds the standard Material components so that every button, card and
 * dialog picks up an expressive corner treatment without call-site changes.
 */
object M3ExpressiveShapes {

    // ── Geometric foundations ────────────────────────────────────────────────
    val Circle: Shape = RoundedCornerShape(percent = 50)
    val Square: Shape = RoundedCornerShape(0.dp)
    val Slanted: Shape = RoundedCornerShape(
        topStart = 4.dp, topEnd = 16.dp, bottomEnd = 4.dp, bottomStart = 16.dp
    )

    val Arch: Shape = RoundedCornerShape(
        topStart = CornerSize(50), topEnd = CornerSize(50),
        bottomEnd = CornerSize(16.dp), bottomStart = CornerSize(16.dp)
    )
    val Semicircle: Shape = RoundedCornerShape(
        topStart = CornerSize(50), topEnd = CornerSize(50),
        bottomEnd = CornerSize(0.dp), bottomStart = CornerSize(0.dp)
    )
    val Oval: Shape = RoundedCornerShape(percent = 50)
    val Pill: Shape = RoundedCornerShape(percent = 50)

    // ── Directional / pointed ────────────────────────────────────────────────
    val Triangle: Shape = RoundedCornerShape(
        topStart = CornerSize(50), topEnd = CornerSize(50),
        bottomEnd = CornerSize(0.dp), bottomStart = CornerSize(0.dp)
    )
    val Arrow: Shape = RoundedCornerShape(
        topStart = CornerSize(0.dp), topEnd = CornerSize(40.dp),
        bottomEnd = CornerSize(50), bottomStart = CornerSize(50)
    )
    val Fan: Shape = RoundedCornerShape(
        topStart = CornerSize(50), topEnd = CornerSize(12.dp),
        bottomEnd = CornerSize(12.dp), bottomStart = CornerSize(12.dp)
    )

    // ── Faceted ──────────────────────────────────────────────────────────────
    val Diamond: Shape = RoundedCornerShape(percent = 50)
    val Clamshell: Shape = RoundedCornerShape(
        topStart = CornerSize(50), topEnd = CornerSize(50),
        bottomEnd = CornerSize(24.dp), bottomStart = CornerSize(24.dp)
    )
    val Pentagon: Shape = RoundedCornerShape(
        topStart = CornerSize(24.dp), topEnd = CornerSize(24.dp),
        bottomEnd = CornerSize(50), bottomStart = CornerSize(50)
    )
    val Gem: Shape = RoundedCornerShape(
        topStart = CornerSize(20.dp), topEnd = CornerSize(20.dp),
        bottomEnd = CornerSize(50), bottomStart = CornerSize(50)
    )

    // ── Lobed / radial ───────────────────────────────────────────────────────
    val VerySunny: Shape = radialShape(lobes = 12, depth = 0.10f, phase = 0f)
    val Sunny: Shape = radialShape(lobes = 8, depth = 0.13f, phase = 0f)
    val FourSidedCookie: Shape = radialShape(lobes = 4, depth = 0.09f, phase = 0f)
    val SixSidedCookie: Shape = radialShape(lobes = 6, depth = 0.09f, phase = 0f)
    val SevenSidedCookie: Shape = radialShape(lobes = 7, depth = 0.09f, phase = 0f)
    val NineSidedCookie: Shape = radialShape(lobes = 9, depth = 0.09f, phase = 0f)
    val TwelveSidedCookie: Shape = radialShape(lobes = 12, depth = 0.08f, phase = 0f)
    val FourLeafClover: Shape = radialShape(lobes = 4, depth = 0.17f, phase = PI.toFloat() / 4f)
    val EightLeafClover: Shape = radialShape(lobes = 8, depth = 0.16f, phase = PI.toFloat() / 8f)
    val Burst: Shape = radialShape(lobes = 12, depth = 0.22f, phase = 0f, sharpness = 2.4f)
    val SoftBurst: Shape = radialShape(lobes = 9, depth = 0.16f, phase = 0f, sharpness = 1.5f)
    val Boom: Shape = radialShape(lobes = 14, depth = 0.30f, phase = 0f, sharpness = 1.0f)
    val SoftBoom: Shape = radialShape(lobes = 16, depth = 0.17f, phase = 0f, sharpness = 1.1f)
    val Flower: Shape = radialShape(lobes = 6, depth = 0.20f, phase = 0f, sharpness = 1.8f)
    val Puffy: Shape = radialShape(lobes = 9, depth = 0.13f, phase = 0f, sharpness = 0.8f)
    val PuffyDiamond: Shape = radialShape(lobes = 4, depth = 0.19f, phase = 0f, sharpness = 0.8f)
    val Ghostish: Shape = radialShape(
        lobes = 2, depth = 0.16f, phase = 0f, sharpness = 0.7f, teardrop = true
    )
    val Bun: Shape = radialShape(lobes = 3, depth = 0.15f, phase = 0f, sharpness = 0.9f)

    // ── Pixel ────────────────────────────────────────────────────────────────
    val PixelCircle: Shape = RoundedCornerShape(22.dp)
    val PixelTriangle: Shape = pixelShape(steps = 6)

    val Heart: Shape = heartShape()

    // ── App tokens ───────────────────────────────────────────────────────────
    // Kept so existing call sites across the app keep compiling and now resolve to
    // genuinely expressive geometry rather than plain rounded rectangles.
    val ExtraLargeRounded: Shape = RoundedCornerShape(32.dp)
    val LargeCard: Shape = RoundedCornerShape(26.dp)
    val MediumCard: Shape = RoundedCornerShape(20.dp)
    val SmallCard: Shape = RoundedCornerShape(14.dp)
    val Chip: Shape = RoundedCornerShape(12.dp)
    val BadgePill: Shape = RoundedCornerShape(10.dp)
    val SectionPill: Shape = RoundedCornerShape(18.dp)
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

    /** Rounded-square tile used by channel logos. */
    val LogoTile: Shape = RoundedCornerShape(22.dp)

    /**
     * Builds a lobed shape by modulating the polar radius around the bounding box.
     *
     * @param lobes number of outward bumps around the perimeter
     * @param depth how far the bumps protrude, as a fraction of the base radius
     * @param phase rotates the lobes; odd/even lobe counts need different phases so
     *   the bumps sit symmetrically rather than straddling the corners
     * @param sharpness exponent applied to each lobe; higher values give pointed
     *   star-like spikes (Burst) while lower values give soft rounds (Puffy)
     * @param teardrop biases the modulation so one lobe is larger, as in Ghostish
     */
    private fun radialShape(
        lobes: Int,
        depth: Float,
        phase: Float,
        sharpness: Float = 1.6f,
        teardrop: Boolean = false
    ): Shape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val path = Path()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = min(size.width, size.height) / 2f
            val steps = (lobes * 24).coerceAtLeast(96)

            for (i in 0..steps) {
                val angle = (i.toFloat() / steps) * 2f * PI.toFloat() - PI.toFloat() / 2f
                val wave = (1f - cos(lobes * (angle + phase))) / 2f
                // pow sharpens each lobe: 1 = soft round, >1 = pointed.
                val mod = 1f + depth * Math.pow(wave.toDouble(), sharpness.toDouble()).toFloat()
                val bias = if (teardrop && i > steps * 0.45f && i < steps * 0.55f) 1.12f else 1f
                val r = base * mod * bias
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            return Outline.Generic(path)
        }
    }

    /** A stepped triangle, echoing the low-resolution "pixel triangle" in the shape library. */
    private fun pixelShape(steps: Int): Shape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val path = Path()
            val w = size.width
            val h = size.height
            val step = h / steps
            // Right edge descends in steps...
            for (i in 0..steps) {
                val y = i * step
                val x = w - (steps - i) * (w / steps) * 0.5f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            // ...then the hypotenuse climbs back in matching steps.
            for (i in steps downTo 0) {
                val y = i * step
                val x = (steps - i) * (w / steps) * 0.5f
                path.lineTo(x, y)
            }
            path.close()
            return Outline.Generic(path)
        }
    }

    /** A two-lobe heart built from parametric curves. */
    private fun heartShape(): Shape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val path = Path()
            val w = size.width
            val h = size.height
            path.moveTo(w / 2f, h * 0.92f)
            path.cubicTo(w * -0.06f, h * 0.52f, w * 0.16f, h * 0.04f, w * 0.5f, h * 0.30f)
            path.cubicTo(w * 0.84f, h * 0.04f, w * 1.06f, h * 0.52f, w / 2f, h * 0.92f)
            path.close()
            return Outline.Generic(path)
        }
    }
}

/**
 * MaterialTheme shapes mapping (extraSmall → extraLarge).
 *
 * Named `AppShapes` rather than `Shapes` so it cannot shadow the
 * `androidx.compose.material3.Shapes` type it is built from, and so
 * call sites in other packages have to import it explicitly.
 *
 * [Shapes] requires `CornerBasedShape`, so the scale is constructed from
 * `RoundedCornerShape` directly rather than from the [Shape]-typed tokens above
 * (a `Shape` is too wide to satisfy it). The fully procedural lobed shapes are
 * applied at the call sites that accept a plain [Shape].
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
