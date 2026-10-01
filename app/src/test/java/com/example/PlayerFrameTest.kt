package com.example

import androidx.compose.ui.geometry.Size
import com.example.kurdishtv.ui.player.DEFAULT_VIDEO_ASPECT
import com.example.kurdishtv.ui.player.ResizeMode
import com.example.kurdishtv.ui.player.playerFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where the picture sits, pinned.
 *
 * The transport overlay is laid out against the video's frame rather than against the
 * screen, because on a phone held upright those are not the same rectangle: a 16:9
 * stream is a band across the middle of a tall window, and pinning the control bar to
 * the bottom of the *window* stranded it in the black bar below the picture with the
 * video nowhere near it.
 *
 * That is pure arithmetic on three numbers, and it is the kind of arithmetic that is
 * wrong in a way no compiler catches — an inverted divide, an off-by-half in the
 * centring, or a guard that lets a zero aspect through and puts the whole overlay at
 * `NaN` offsets.
 */
class PlayerFrameTest {

    private val hd = Size(1080f, 720f)   // a 3:2 box, i.e. wider than 16:9
    private val tall = Size(1080f, 1920f) // a phone held upright

    @Test
    fun `fit centres a narrower picture as horizontal bars`() {
        val frame = playerFrame(tall, DEFAULT_VIDEO_ASPECT, ResizeMode.FIT)

        // 16:9 inside 1080x1920 is limited by width, so it is full width...
        assertEquals(1080f, frame.width, 0.01f)
        assertEquals(607.5f, frame.height, 0.01f)
        // ...and split into equal bars above and below.
        assertEquals((1920f - 607.5f) / 2f, frame.top, 0.01f)
        assertEquals(0f, frame.left, 0.01f)
    }

    @Test
    fun `fit centres a taller picture as side bars`() {
        val frame = playerFrame(hd, 3f / 4f, ResizeMode.FIT)

        // A 3:4 stream inside a 3:2 window is limited by height.
        assertEquals(720f, frame.height, 0.01f)
        assertEquals(540f, frame.width, 0.01f)
        assertEquals(0f, frame.top, 0.01f)
        assertEquals((1080f - 540f) / 2f, frame.left, 0.01f)
    }

    @Test
    fun `fill and zoom both cover the whole container`() {
        for (mode in listOf(ResizeMode.FILL, ResizeMode.ZOOM)) {
            val frame = playerFrame(tall, DEFAULT_VIDEO_ASPECT, mode)
            assertEquals("left for $mode", 0f, frame.left, 0.01f)
            assertEquals("top for $mode", 0f, frame.top, 0.01f)
            assertEquals("width for $mode", 1080f, frame.width, 0.01f)
            assertEquals("height for $mode", 1920f, frame.height, 0.01f)
        }
    }

    @Test
    fun `a square picture in a square window is not inset at all`() {
        val frame = playerFrame(Size(1000f, 1000f), 1f, ResizeMode.FIT)
        assertEquals(0f, frame.top, 0.01f)
        assertEquals(1000f, frame.width, 0.01f)
    }

    @Test
    fun `an unknown or nonsense aspect falls back to 16 by 9`() {
        // Before the first frame is decoded, and on a decoder that reports a
        // degenerate size. Neither may divide by zero or produce NaN offsets, which
        // Compose would draw at infinity.
        for (aspect in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val frame = playerFrame(tall, aspect, ResizeMode.FIT)
            val expected = playerFrame(tall, DEFAULT_VIDEO_ASPECT, ResizeMode.FIT)
            assertEquals("top for aspect=$aspect", expected.top, frame.top, 0.01f)
            assertEquals("height for aspect=$aspect", expected.height, frame.height, 0.01f)
        }
    }

    @Test
    fun `a not-yet-measured container does not divide by zero`() {
        val frame = playerFrame(Size(0f, 0f), DEFAULT_VIDEO_ASPECT, ResizeMode.FIT)
        assertEquals(0f, frame.top, 0.01f)
        assertEquals(0f, frame.width, 0.01f)
        assertEquals(0f, frame.height, 0.01f)
    }

    @Test
    fun `the picture never overflows the container it is fitted into`() {
        for (aspect in listOf(0.5f, 1f, 1.777f, 2.39f, 4f)) {
            val frame = playerFrame(tall, aspect, ResizeMode.FIT)
            assertTrue("left for aspect=$aspect", frame.left >= -0.01f)
            assertTrue("top for aspect=$aspect", frame.top >= -0.01f)
            assertTrue("width for aspect=$aspect", frame.left + frame.width <= tall.width + 0.01f)
            assertTrue("height for aspect=$aspect", frame.top + frame.height <= tall.height + 0.01f)
        }
    }
}
