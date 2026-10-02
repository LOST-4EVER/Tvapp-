package com.example

import com.example.kurdishtv.ui.player.VideoColorFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The colour filters, pinned.
 *
 * The player draws the picture through a `Canvas.saveLayer` carrying one of these
 * matrices, and [VideoPlayerView] keys its view on whether a filter is active at
 * all — because a filter applied to a `SurfaceView` is silently a no-op, the view
 * has to be a `TextureView` and the two decisions have to agree.
 *
 * That agreement is the whole contract here, and it rests on two properties of
 * [VideoColorFilter] that no compiler checks and that nothing else in the app
 * would notice breaking:
 *
 *  - [isActive] is true for exactly the presets that produce a matrix. If a preset
 *    ever gained a matrix without setting the flag, or set the flag without one,
 *    the player would pick the wrong view and the correction would either vanish or
 *    be spent re-creating a `PlayerView` for no reason.
 *  - [toMatrix] is null only for [VideoColorFilter.None], so the default path is
 *    never handed a matrix and never enters a layer.
 *
 * The `next()` cycle is checked too, because the filter is a single cycling
 * button: a cycle that skipped a preset, or that looped back to `None` early,
 * would leave one of the six unreachable rather than wrong-looking.
 */
class VideoColorFilterTest {

    @Test
    fun `only Normal has no matrix`() {
        assertNull(
            "Normal must leave the unmodified rendering path alone",
            VideoColorFilter.None.toMatrix()
        )
        for (preset in VideoColorFilter.entries) {
            if (preset == VideoColorFilter.None) continue
            assertNotNull("${preset.name} must produce a matrix", preset.toMatrix())
        }
    }

    @Test
    fun `isActive agrees with whether there is a matrix to apply`() {
        // The player decides SurfaceView vs TextureView from `isActive`, and the
        // draw path decides whether to open a layer from the matrix. If these two
        // ever disagree the picture is silently uncorrected.
        for (preset in VideoColorFilter.entries) {
            assertEquals(
                "isActive disagrees with toMatrix for ${preset.name}",
                preset.toMatrix() != null,
                preset.isActive
            )
        }
    }

    @Test
    fun `every matrix is a well formed 4 by 5`() {
        // Android's ColorMatrix is column-major with four rows of five: the last
        // row is the alpha row and must be 0 0 0 1 0. A matrix of any other length
        // is rejected outright by ColorMatrixColorFilter, and one with a wrong alpha
        // row turns the video transparent rather than tinted.
        for (preset in VideoColorFilter.entries) {
            val matrix = preset.toMatrix() ?: continue
            assertEquals("${preset.name} must have 20 entries", 20, matrix.size)
            assertEquals("${preset.name} alpha offset must be 0", 0f, matrix[15], EPS)
            assertEquals("${preset.name} alpha scale must be 1", 1f, matrix[18], EPS)
            assertEquals("${preset.name} alpha bias must be 0", 0f, matrix[19], EPS)
        }
    }

    @Test
    fun `cycling reaches every preset and returns to Normal`() {
        var preset = VideoColorFilter.None
        val seen = mutableListOf(preset)
        repeat(VideoColorFilter.entries.size - 1) {
            preset = preset.next()
            assertTrue(
                "cycling revisited ${preset.name} before covering every preset",
                preset !in seen
            )
            seen.add(preset)
        }

        assertEquals(
            "one press of the button must reach every preset exactly once",
            VideoColorFilter.entries.toList(),
            seen
        )
        assertEquals(
            "the cycle must close back onto Normal",
            VideoColorFilter.None,
            preset.next()
        )
    }

    @Test
    fun `the Compose conversion agrees with the matrix`() {
        // Both wrap [toMatrix], so neither may disagree about which presets exist.
        //
        // Only the Compose conversion is checked here. The framework one builds an
        // `android.graphics.ColorMatrixColorFilter`, which is a real Android class
        // and cannot be constructed under plain JUnit — this test is deliberately not
        // a Robolectric one so it stays in the fast unit suite.
        for (preset in VideoColorFilter.entries) {
            assertEquals(
                "Compose conversion disagrees for ${preset.name}",
                preset.toMatrix() != null,
                preset.toColorFilter() != null
            )
        }
    }

    @Test
    fun `Normal is the only preset that is not active`() {
        assertFalse(VideoColorFilter.None.isActive)
        for (preset in VideoColorFilter.entries) {
            if (preset != VideoColorFilter.None) {
                assertTrue("${preset.name} should be active", preset.isActive)
            }
        }
    }

    private companion object {
        const val EPS = 1e-6f
    }
}