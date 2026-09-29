package com.example

import androidx.compose.ui.geometry.Size
import com.example.kurdishtv.ui.motion.focusRingGrowth
import com.example.kurdishtv.ui.motion.focusRingSize
import com.example.kurdishtv.ui.motion.focusScrimStrokeWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The focus ring's layout, pinned.
 *
 * This is the UI layer that #39's own note called out as having nothing covering it,
 * and the ring is where that gap had already cost something. Three separate things
 * about it were wrong at the same time, and not one of them was visible without a
 * device and a D-pad:
 *
 *  - `fittedPath` anchors a shape's **top-left** at the origin, so the ring was drawn
 *    `growth` too far down and right. Its top and left strokes landed on the element's
 *    own corner and ate into the content being marked, and only the bottom and right
 *    carried the outset.
 *  - the scrim was stroked at the ring's own width on the same path and then painted
 *    over by the ring, so it was not a subtle scrim. It was nothing at all.
 *  - the ring is drawn *outside* the element, so any `Modifier.clip` upstream of it
 *    removes it completely. That one cannot be tested here at all — it is a call-site
 *    discipline, and the reason it is written down in `TvFocus`'s KDoc.
 *
 * The first two are pure arithmetic and are pinned below. The numbers are the ones
 * `ExpressiveMotion.Focus` actually ships: 3dp stroke, 4dp outset.
 */
class FocusRingGeometryTest {

    private val stroke = 3f
    private val outset = 4f

    @Test
    fun `growth is the outset plus half the stroke`() {
        // Half the stroke falls inside the path and half outside, so the path's box
        // has to clear the element by the outset *and* by half a stroke.
        assertEquals(5.5f, focusRingGrowth(outset, stroke), 0.001f)
    }

    @Test
    fun `growth is never negative`() {
        // A negative outset would otherwise put the ring's path inside the element
        // and the stroke over its content.
        assertEquals(0f, focusRingGrowth(-20f, stroke), 0.001f)
        assertEquals(0f, focusRingGrowth(-20f, -5f), 0.001f)
    }

    @Test
    fun `a hairline ring still clears the element by the outset`() {
        // A zero-width stroke halves the growth but does not remove it. The outset is
        // the distance between the element and the ring, and it is a property of the
        // design rather than of the stroke; collapsing it to zero would put a
        // hairline ring directly on the element's edge.
        assertEquals(outset, focusRingGrowth(outset, 0f), 0.001f)
    }

    @Test
    fun `the path box is the element plus growth on every side`() {
        val element = Size(200f, 120f)
        val grown = focusRingGrowth(outset, stroke)

        val path = focusRingSize(element, grown)

        // `fittedPath` fills this box from its top-left corner, so the box is what
        // gets translated by -growth at draw time. Both dimensions, or the ring is
        // correct horizontally and off-centre vertically.
        assertEquals(element.width + grown * 2f, path.width, 0.001f)
        assertEquals(element.height + grown * 2f, path.height, 0.001f)
    }

    @Test
    fun `the gap the outset promises is the gap the ring leaves`() {
        // The regression this file exists for.
        //
        // Drawn without the translate, the path starts at the element's own top-left
        // corner. The stroke is centred on the path, so it spans -stroke/2 to
        // +stroke/2 about that corner: with the shipped numbers, the ring's top edge
        // ate 1.5dp into the content and there was no gap at all, while the bottom
        // edge sat 9.5dp away. After shifting the draw back by `growth`, the inner
        // edge of the stroke is `outset` from the element on every side.
        val growth = focusRingGrowth(outset, stroke)

        // Inner edge of the stroke, measured from the element's edge, once the draw
        // has been translated back by `growth`: growth - stroke/2.
        val gap = growth - stroke / 2f

        assertEquals(outset, gap, 0.001f)
    }

    @Test
    fun `the scrim is wider than the ring so the ring cannot cover it`() {
        val scrim = focusScrimStrokeWidth(outset, stroke)

        // The failure was the scrim being exactly the ring's width on the ring's own
        // path, which is a stroke painted precisely over another one.
        assertTrue(
            "the scrim must be wider than the ring, or the ring paints straight over it",
            scrim > stroke
        )
    }

    @Test
    fun `the scrim reaches from the element's edge out to the ring's far edge`() {
        val growth = focusRingGrowth(outset, stroke)
        val scrim = focusScrimStrokeWidth(outset, stroke)

        // The path's centre-line is `growth` from the element's edge. A stroke is
        // centred on the path, and the draw shifts the scrim back by half the outset,
        // so it spans (growth - outset/2) - scrim/2 .. (growth - outset/2) + scrim/2.
        val offset = growth - outset / 2f
        val near = offset - scrim / 2f
        val far = offset + scrim / 2f

        // It starts at the element's own edge, so it fills the gap the outset left
        // rather than floating outside it.
        assertEquals(0f, near, 0.001f)
        // And it stops at the ring's outer edge, which is at growth + stroke/2, so the
        // ring is drawn on top of the outer part of the band.
        assertEquals(growth + stroke / 2f, far, 0.001f)
    }

    @Test
    fun `the scrim does not bleed into the neighbouring card`() {
        // The bug this caught while it was being written.
        //
        // The first version centred the scrim on the ring's path at
        // `2 * outset + stroke`, which spans 0..11dp from the card's edge. The
        // channel grid's gutter is 14dp, so half of that is 7dp and an opaque band
        // reaching 11dp bites 4dp out of the *next* card along. Since only one card is
        // focused at a time this is a one-sided notch rather than two overlapping
        // bands, which is exactly the kind of thing that reads as a rendering fault.
        val growth = focusRingGrowth(outset, stroke)
        val scrim = focusScrimStrokeWidth(outset, stroke)
        val reach = growth - outset / 2f + scrim / 2f

        assertEquals(7f, reach, 0.001f)
        assertTrue(
            "the scrim must not reach past half the grid's 14dp gutter, or a focused " +
                "card paints a notch into its neighbour",
            reach <= 14f / 2f
        )
    }
}
