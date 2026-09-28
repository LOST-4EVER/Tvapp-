package com.example.kurdishtv.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/**
 * A rounded polygon: a closed ring of vertices, each with its own corner rounding.
 *
 * This is the same model Google's `androidx.graphics.shapes.RoundedPolygon` uses,
 * reimplemented here so the app keeps its dependency list short and can animate the
 * shape library without pulling in a Compose-experimental artifact. The Material 3
 * Expressive definitions in [M3ExpressivePolygons] are ported verbatim from
 * `androidx.compose.material3.MaterialShapes` and feed straight into it.
 *
 * Every vertex carries its own [ExpressiveCorner] rather than the shape carrying one
 * global radius, and that is what makes the Expressive vocabulary possible: a 7-sided
 * cookie needs seven large, individually-rounded points, a heart needs two sharp ones,
 * and a pill needs two semicircular ends joined by straight sides.
 *
 * Instances are immutable and cheap to hold. Turning one into a [Path] is the only
 * allocation-heavy step, so callers build the path once and animate only the transform.
 */
@Immutable
class ExpressivePolygon(
    val vertices: List<Offset>,
    val corners: List<ExpressiveCorner>
) {
    /** The bounding box of the vertices, after [rotationDegrees] about the centroid. */
    fun bounds(rotationDegrees: Float = 0f): Rect {
        if (vertices.isEmpty()) return Rect.Zero
        val rotated = rotateVertices(rotationDegrees)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (v in rotated) {
            if (v.x < minX) minX = v.x
            if (v.y < minY) minY = v.y
            if (v.x > maxX) maxX = v.x
            if (v.y > maxY) maxY = v.y
        }
        return Rect(minX, minY, maxX, maxY)
    }

    private fun centroid(): Offset {
        var x = 0f
        var y = 0f
        for (v in vertices) {
            x += v.x
            y += v.y
        }
        val n = vertices.size.coerceAtLeast(1)
        return Offset(x / n, y / n)
    }

    private fun rotateVertices(rotationDegrees: Float): List<Offset> {
        if (rotationDegrees == 0f || vertices.isEmpty()) return vertices
        val centre = centroid()
        return vertices.map { it.rotateDegrees(rotationDegrees, centre) }
    }

    /** Builds the outline path. */
    fun path(rotationDegrees: Float = 0f, out: Path = Path()): Path =
        buildRoundedPolygonPath(rotateVertices(rotationDegrees), corners, out)

    /** A [Shape] that draws this polygon, filling the available box. */
    fun toShape(rotationDegrees: Float = 0f): Shape = PolygonShape(this, rotationDegrees)

    /**
     * The same outline, non-uniformly stretched.
     *
     * Used to give the library its proportions: an oval is a circle squashed on one
     * axis, and the puffy shape is a wide ring flattened vertically.
     *
     * The corner radii are scaled too, by the **smaller** of the two factors. A
     * circle is a four-gon with a very large corner smoothing, so leaving the radii
     * alone would turn every flattened shape into something subtly not-an-ellipse:
     * the rounding would stay sized for the unstretched ring. Scaling by the smaller
     * factor is the only value that is guaranteed to fit on the shortened edge, and
     * it matches what `RoundedPolygon.scaled` does.
     */
    fun scaled(scaleX: Float, scaleY: Float): ExpressivePolygon {
        val r = min(abs(scaleX), abs(scaleY))
        return ExpressivePolygon(
            vertices.map { Offset(it.x * scaleX, it.y * scaleY) },
            corners.map { it.copy(radius = it.radius * r) }
        )
    }

    /** The same outline, rotated about its centroid. */
    fun rotated(degrees: Float): ExpressivePolygon {
        if (degrees == 0f) return this
        val centre = centroid()
        return ExpressivePolygon(
            vertices.map { it.rotateDegrees(degrees, centre) },
            corners
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExpressivePolygon) return false
        return vertices == other.vertices && corners == other.corners
    }

    override fun hashCode(): Int = 31 * vertices.hashCode() + corners.hashCode()

    companion object {
        /**
         * A regular n-gon inscribed in a circle of [radius], centred on the origin.
         *
         * [phaseDegrees] rotates the whole ring, which is how the odd-sided shapes are
         * aligned: a 7-sided cookie has to start at the top, not at 3 o'clock.
         */
        fun regular(
            sides: Int,
            radius: Float = 1f,
            corner: ExpressiveCorner = ExpressiveCorner(0.3f),
            phaseDegrees: Float = 0f
        ): ExpressivePolygon {
            val vertices = ArrayList<Offset>(sides)
            val corners = ArrayList<ExpressiveCorner>(sides)
            for (i in 0 until sides) {
                val angle = (phaseDegrees + i * 360f / sides).toRadians()
                vertices += Offset(cos(angle) * radius, sin(angle) * radius)
                corners += corner
            }
            return ExpressivePolygon(vertices, corners)
        }

        /**
         * A star: [points] outer vertices alternating with [points] inner ones.
         *
         * Every cookie and sunny shape in the Expressive library is one of these with a
         * high [innerRadius] and heavily rounded points — the points and the valleys are
         * barely distinguishable, which is exactly what makes them read as soft blobs
         * rather than as a spiky star.
         */
        fun star(
            points: Int,
            innerRadius: Float = 0.8f,
            radius: Float = 1f,
            corner: ExpressiveCorner = ExpressiveCorner(0.5f),
            phaseDegrees: Float = 0f
        ): ExpressivePolygon {
            val count = points * 2
            val vertices = ArrayList<Offset>(count)
            val corners = ArrayList<ExpressiveCorner>(count)
            for (i in 0 until count) {
                val r = if (i % 2 == 0) radius else radius * innerRadius
                val angle = (phaseDegrees + i * 360f / count).toRadians()
                vertices += Offset(cos(angle) * r, sin(angle) * r)
                corners += corner
            }
            return ExpressivePolygon(vertices, corners)
        }

        /** A rectangle with per-corner rounding, centred on the origin. */
        fun rect(
            width: Float = 1f,
            height: Float = 1f,
            perCorner: List<ExpressiveCorner> = listOf(ExpressiveCorner(0.3f))
        ): ExpressivePolygon {
            val halfW = width / 2f
            val halfH = height / 2f
            val vertices = listOf(
                Offset(-halfW, -halfH),
                Offset(halfW, -halfH),
                Offset(halfW, halfH),
                Offset(-halfW, halfH)
            )
            val corners = when (perCorner.size) {
                0 -> List(4) { ExpressiveCorner(0.3f) }
                1 -> List(4) { perCorner[0] }
                else -> List(4) { perCorner[it % perCorner.size] }
            }
            return ExpressivePolygon(vertices, corners)
        }

        /**
         * A shape built from one or more authored points, repeated around a centre.
         *
         * The workhorse behind the Expressive library: a designer authors the
         * interesting fraction of a shape and the rest is generated by rotating and,
         * optionally, mirroring it. [reps] is how many times the authored list repeats;
         * with [mirroring] the repetition alternates reflection, which is how a heart
         * and a pill get their left/right symmetry from half a definition.
         */
        fun fromAuthoredPoints(
            points: List<Pair<Offset, ExpressiveCorner>>,
            reps: Int,
            center: Offset = Offset(0.5f, 0.5f),
            mirroring: Boolean = false
        ): ExpressivePolygon {
            val all = repeatPoints(points, reps, center, mirroring)
            val vertices = ArrayList<Offset>(all.size)
            val corners = ArrayList<ExpressiveCorner>(all.size)
            for ((offset, corner) in all) {
                vertices += offset
                corners += corner
            }
            return ExpressivePolygon(vertices, corners)
        }

        /**
         * Repeats — and optionally mirrors — an authored point list around [center].
         *
         * A direct port of the helper Material uses to build its own shapes, so the
         * silhouettes match the reference library rather than merely approximating it.
         */
        private fun repeatPoints(
            points: List<Pair<Offset, ExpressiveCorner>>,
            reps: Int,
            center: Offset,
            mirroring: Boolean
        ): List<Pair<Offset, ExpressiveCorner>> {
            if (mirroring) {
                val angles = points.map { (it.first - center).angleDegrees() }
                val distances = points.map { (it.first - center).getDistance() }
                val actualReps = reps * 2
                val sectionAngle = 360f / actualReps
                val result = ArrayList<Pair<Offset, ExpressiveCorner>>(points.size * actualReps)
                for (rep in 0 until actualReps) {
                    for (index in points.indices) {
                        val i = if (rep % 2 == 0) index else points.lastIndex - index
                        if (i > 0 || rep % 2 == 0) {
                            val degrees = sectionAngle * rep +
                                if (rep % 2 == 0) angles[i]
                                else sectionAngle - angles[i] + 2f * angles[0]
                            val a = degrees.toRadians()
                            val finalPoint = Offset(cos(a), sin(a)) * distances[i] + center
                            result += finalPoint to points[i].second
                        }
                    }
                }
                return result
            }
            val result = ArrayList<Pair<Offset, ExpressiveCorner>>(points.size * reps)
            val np = points.size
            for (i in 0 until np * reps) {
                val point = points[i % np].first.rotateDegrees((i / np) * 360f / reps, center)
                result += point to points[i % np].second
            }
            return result
        }
    }
}

/** A corner's rounding: [radius] in the polygon's own units, [smoothing] from 0 to 1. */
@Immutable
data class ExpressiveCorner(
    val radius: Float,
    val smoothing: Float = 0f
) {
    companion object {
        /** A sharp, un-rounded corner. */
        val None = ExpressiveCorner(0f)
    }
}

/**
 * A [Shape] backed by an [ExpressivePolygon].
 *
 * The polygon is fitted to the available box the way Material does it: the *vertices*
 * are scaled to fill the box and the rounding is drawn inside them. Scaling from the
 * vertices rather than from the finished path is what keeps an oval an oval instead of
 * quietly turning it into a circle.
 *
 * The base path and its bounds are computed once, here, so drawing only ever copies and
 * transforms. That matters because this shape sits inside a grid of several hundred
 * cards that redraw on every scroll frame.
 */
class PolygonShape(
    polygon: ExpressivePolygon,
    private val rotationDegrees: Float = 0f
) : Shape {

    private val basePath: Path = polygon.path(rotationDegrees)
    private val bounds: Rect = polygon.bounds(rotationDegrees)

    /** Reused so a draw never allocates. */
    private val workPath: Path = Path()
    private val workMatrix: Matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = bounds.width
        val h = bounds.height
        if (w <= 0f || h <= 0f || size.minDimension <= 0f) {
            return Outline.Generic(Path())
        }
        workPath.rewind()
        workPath.addPath(basePath)
        fitToBox(workPath, bounds, size, workMatrix)
        return Outline.Generic(workPath)
    }
}

/**
 * A [Shape] that is somewhere between two polygons.
 *
 * The whole point of the Expressive shape library: an element is not tied to one
 * outline, it can be *between* outlines. Material calls this a built-in shape morph.
 *
 * [progress] is the fraction between the two. Because it is a plain constructor
 * argument rather than something read from a `State`, the shape is rebuilt whenever the
 * animation advances — which is what keeps the outline and the animation in step
 * without any cache invalidation of its own.
 */
class MorphingPolygonShape(
    private val morph: ExpressiveMorph,
    private val progress: Float,
    private val rotationDegrees: Float = 0f
) : Shape {

    private val workPath: Path = Path()
    private val workMatrix: Matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (size.minDimension <= 0f) return Outline.Generic(Path())
        val polygon = morph.polygonAt(progress, rotationDegrees)
        val bounds = polygon.bounds()
        if (bounds.width <= 0f || bounds.height <= 0f) return Outline.Generic(Path())

        polygon.path(out = workPath)
        fitToBox(workPath, bounds, size, workMatrix)
        return Outline.Generic(workPath)
    }
}

/**
 * Interpolates between two polygons that may have different vertex counts.
 *
 * Compose's own `Morph` does the same job, but this one puts the two shapes into
 * lockstep permanently, so evaluating it is a plain per-vertex lerp with no per-frame
 * feature matching. That is what makes it cheap enough to drive a grid of cards rather
 * than a single hero element.
 *
 * Getting the counts to match is the interesting part, and it happens in two steps:
 *
 *  1. The polygon with fewer vertices repeatedly has its longest edge split in two. A
 *     straight edge split at its midpoint is still the same edge, so nothing moves
 *     visually; it only buys the extra vertices needed to line up.
 *  2. The second polygon's vertex list is rotated until it best matches the first.
 *     Without this a square morphing into a square that happens to be drawn a quarter
 *     turn away would spin all the way round instead of staying put.
 */
@Immutable
class ExpressiveMorph(
    private val fromVertices: List<Offset>,
    private val fromCorners: List<ExpressiveCorner>,
    private val toVertices: List<Offset>,
    private val toCorners: List<ExpressiveCorner>
) {
    private val centre: Offset = run {
        var x = 0f
        var y = 0f
        for (v in fromVertices) {
            x += v.x
            y += v.y
        }
        val n = fromVertices.size.coerceAtLeast(1)
        Offset(x / n, y / n)
    }

    /**
     * The intermediate polygon at [progress], rotated by [rotationDegrees].
     *
     * This is the per-frame workhorse: one pass over a handful of vertices, producing a
     * polygon that [PolygonShape] can immediately fit to the available box.
     */
    fun polygonAt(progress: Float, rotationDegrees: Float = 0f): ExpressivePolygon {
        val t = progress.coerceIn(0f, 1f)
        val vertices = ArrayList<Offset>(fromVertices.size)
        val corners = ArrayList<ExpressiveCorner>(fromVertices.size)
        for (i in fromVertices.indices) {
            val a = fromVertices[i]
            val b = toVertices[i]
            val v = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
            vertices += if (rotationDegrees == 0f) v else v.rotateDegrees(rotationDegrees, centre)
            val ca = fromCorners[i]
            val cb = toCorners[i]
            corners += ExpressiveCorner(
                radius = ca.radius + (cb.radius - ca.radius) * t,
                smoothing = ca.smoothing + (cb.smoothing - ca.smoothing) * t
            )
        }
        return ExpressivePolygon(vertices, corners)
    }

    companion object {
        private const val MAX_SUBDIVISIONS = 96

        /** Builds a morph between two polygons, matching their vertex counts first. */
        fun between(from: ExpressivePolygon, to: ExpressivePolygon): ExpressiveMorph {
            var aVerts = from.vertices
            var aCorners = from.corners
            var bVerts = to.vertices
            var bCorners = to.corners

            var guard = 0
            while (aVerts.size < bVerts.size && guard < MAX_SUBDIVISIONS) {
                val split = splitLongestEdge(aVerts, aCorners)
                aVerts = split.first
                aCorners = split.second
                guard++
            }
            guard = 0
            while (bVerts.size < aVerts.size && guard < MAX_SUBDIVISIONS) {
                val split = splitLongestEdge(bVerts, bCorners)
                bVerts = split.first
                bCorners = split.second
                guard++
            }
            // Defensive: if a pathological pair somehow still disagrees, fall back to
            // driving the morph from whichever list is shorter.
            if (aVerts.size != bVerts.size) {
                val n = min(aVerts.size, bVerts.size)
                aVerts = aVerts.take(n)
                aCorners = aCorners.take(n)
                bVerts = bVerts.take(n)
                bCorners = bCorners.take(n)
            }

            val offset = bestRotationOffset(aVerts, bVerts)
            return ExpressiveMorph(
                aVerts,
                aCorners,
                (0 until bVerts.size).map { bVerts[(it + offset) % bVerts.size] },
                (0 until bCorners.size).map { bCorners[(it + offset) % bCorners.size] }
            )
        }

        /**
         * Inserts a sharp vertex at the midpoint of the longest edge.
         *
         * Sharp on purpose: it sits on a straight run, so rounding it would put a
         * visible dent into an edge that was previously flat.
         */
        private fun splitLongestEdge(
            vertices: List<Offset>,
            corners: List<ExpressiveCorner>
        ): Pair<List<Offset>, List<ExpressiveCorner>> {
            val n = vertices.size
            var bestIndex = 0
            var bestLength = -1f
            for (i in 0 until n) {
                val length = (vertices[(i + 1) % n] - vertices[i]).getDistance()
                if (length > bestLength) {
                    bestLength = length
                    bestIndex = i
                }
            }
            val outV = ArrayList<Offset>(n + 1)
            val outC = ArrayList<ExpressiveCorner>(n + 1)
            for (i in 0 until n) {
                outV += vertices[i]
                outC += corners[i]
                if (i == bestIndex) {
                    val next = (i + 1) % n
                    outV += Offset(
                        (vertices[i].x + vertices[next].x) / 2f,
                        (vertices[i].y + vertices[next].y) / 2f
                    )
                    outC += ExpressiveCorner.None
                }
            }
            return outV to outC
        }

        /** The shift of [b] that puts it closest to [a], vertex for vertex. */
        private fun bestRotationOffset(a: List<Offset>, b: List<Offset>): Int {
            val n = a.size
            if (n <= 1) return 0
            var best = 0
            var bestCost = Float.MAX_VALUE
            for (shift in 0 until n) {
                var cost = 0f
                for (i in 0 until n) {
                    val p = a[i]
                    val q = b[(i + shift) % n]
                    val dx = p.x - q.x
                    val dy = p.y - q.y
                    cost += dx * dx + dy * dy
                }
                if (cost < bestCost) {
                    bestCost = cost
                    best = shift
                }
            }
            return best
        }
    }
}

/**
 * The polygon's outline scaled to exactly fill [size].
 *
 * [PolygonShape] and [MorphingPolygonShape] do this internally; this is the same
 * operation exposed for callers that want to *draw* rather than clip — an animated
 * focus ring, for instance, which is stroked rather than used as a clip.
 */
fun ExpressivePolygon.fittedPath(
    size: Size,
    rotationDegrees: Float = 0f,
    out: Path = Path()
): Path {
    val bounds = bounds(rotationDegrees)
    val w = bounds.width
    val h = bounds.height
    if (w <= 0f || h <= 0f) {
        out.rewind()
        return out
    }
    path(rotationDegrees, out)
    fitToBox(out, bounds, size)
    return out
}

/** The morph's outline at [progress], scaled to exactly fill [size]. */
fun ExpressiveMorph.fittedPath(
    progress: Float,
    size: Size,
    rotationDegrees: Float = 0f,
    out: Path = Path()
): Path {
    val polygon = polygonAt(progress, rotationDegrees)
    val bounds = polygon.bounds()
    val w = bounds.width
    val h = bounds.height
    if (w <= 0f || h <= 0f) {
        out.rewind()
        return out
    }
    polygon.path(out = out)
    fitToBox(out, bounds, size)
    return out
}

/**
 * Stretches [path] so that [bounds] becomes exactly [size].
 *
 * Compose matrices post-multiply, so scaling and then translating means "move the
 * shape's top-left corner to the origin, then stretch it across the box".
 */
private fun fitToBox(path: Path, bounds: Rect, size: Size, matrix: Matrix = Matrix()) {
    matrix.reset()
    matrix.scale(size.width / bounds.width, size.height / bounds.height, 0f, 0f)
    matrix.translate(-bounds.left, -bounds.top)
    path.transform(matrix)
}

// ── Path generation ───────────────────────────────────────────────────────────

/**
 * Emits the outline of a rounded polygon into [out].
 *
 * Each corner becomes up to two cubic segments: an optional *smoothing* curve that
 * eases the straight edge into the round, and the circular arc itself. With
 * `smoothing = 0` the smoothing segment degenerates to a point and the corner is a
 * single circular-arc cubic — the standard `k = 4/3 · tan(θ/4) · r` approximation,
 * which is exact for a quarter circle.
 */
private fun buildRoundedPolygonPath(
    vertices: List<Offset>,
    corners: List<ExpressiveCorner>,
    out: Path
): Path {
    out.rewind()
    val n = vertices.size
    if (n < 3) return out

    val tangentIn = ArrayList<Offset>(n)
    val tangentOut = ArrayList<Offset>(n)
    val arcStart = ArrayList<Offset>(n)
    val arcEnd = ArrayList<Offset>(n)
    val entry = ArrayList<Offset>(n)
    val radius = FloatArray(n)

    for (i in 0 until n) {
        val centre = vertices[i]
        val toPrev = vertices[(i - 1 + n) % n] - centre
        val toNext = vertices[(i + 1) % n] - centre
        val lenIn = toPrev.getDistance()
        val lenOut = toNext.getDistance()
        val dirIn = if (lenIn > 0f) toPrev / lenIn else Offset.Zero
        val dirOut = if (lenOut > 0f) toNext / lenOut else Offset.Zero

        val corner = corners[i]
        val smoothing = corner.smoothing.coerceIn(0f, 1f)
        val cosTheta = (dirIn.x * dirOut.x + dirIn.y * dirOut.y).coerceIn(-1f, 1f)
        val theta = acos(cosTheta)
        // A corner of radius r turns through theta, and the tangent points sit
        // r·tan(theta/2) back from the vertex. Storing the arc radius separately is
        // what keeps the cubic's control length correct after the clamp below.
        val tanHalf = tan(theta / 2f)
        // The rounding and its smoothing run share the two adjacent edges. Budgeting
        // half of each edge keeps the straight run between neighbouring corners
        // non-negative even for a very round corner on a very short edge.
        val budgetIn = 0.5f * lenIn / (1f + smoothing)
        val budgetOut = 0.5f * lenOut / (1f + smoothing)
        val t = if (tanHalf < 1e-3f) {
            0f
        } else {
            min(corner.radius * tanHalf, min(budgetIn, budgetOut)).coerceAtLeast(0f)
        }
        val arcRadius = if (t > 0f && tanHalf > 1e-3f) t / tanHalf else 0f

        val a = centre + dirIn * t
        val b = centre + dirOut * t
        tangentIn += dirIn
        tangentOut += dirOut
        arcStart += a
        arcEnd += b
        entry += a - dirIn * (t * smoothing)
        radius[i] = arcRadius
    }

    out.moveTo(entry[0].x, entry[0].y)
    for (i in 0 until n) {
        val a = arcStart[i]
        val b = arcEnd[i]
        val dirIn = tangentIn[i]
        val dirOut = tangentOut[i]
        val arcRadius = radius[i]
        val cosTheta = (dirIn.x * dirOut.x + dirIn.y * dirOut.y).coerceIn(-1f, 1f)
        val kappa = 4f / 3f * tan(acos(cosTheta) / 4f) * arcRadius
        val smooth = (entry[i] - a).getDistance()

        if (smooth > 1e-4f) {
            val c1 = entry[i] + dirIn * (smooth * 0.55f)
            val c2 = a - dirIn * (smooth * 0.35f)
            out.cubicTo(c1.x, c1.y, c2.x, c2.y, a.x, a.y)
            // Arc and trailing flank share one cubic: starting at `a` and finishing at
            // the corner's exit point, tangent to the edge on both sides.
            val arcC1 = a + dirIn * kappa
            val arcC2 = b + dirOut * (smooth * 0.35f)
            val b0 = b + dirOut * smooth
            out.cubicTo(arcC1.x, arcC1.y, arcC2.x, arcC2.y, b0.x, b0.y)
        } else {
            val arcC1 = a + dirIn * kappa
            val arcC2 = b + dirOut * kappa
            out.cubicTo(arcC1.x, arcC1.y, arcC2.x, arcC2.y, b.x, b.y)
        }

        val nextEntry = entry[(i + 1) % n]
        out.lineTo(nextEntry.x, nextEntry.y)
    }
    out.close()
    return out
}

// ── Small geometry helpers ───────────────────────────────────────────────────

internal fun Float.toRadians(): Float = this / 360f * 2f * PI.toFloat()

internal fun Offset.angleDegrees(): Float = atan2(y, x) * 180f / PI.toFloat()

internal fun Offset.rotateDegrees(degrees: Float, center: Offset = Offset.Zero): Offset {
    val a = degrees.toRadians()
    val off = this - center
    return Offset(
        off.x * cos(a) - off.y * sin(a),
        off.x * sin(a) + off.y * cos(a)
    ) + center
}
