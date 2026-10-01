package com.example

import com.example.kurdishtv.ui.components.initialsOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The monogram a logo-less channel falls back to.
 *
 * This is the *fallback identity*, not a loading placeholder, so it has to be stable:
 * the same channel has to keep the same initials across refreshes, reorderings and
 * devices, and it is what a viewer reads to tell two logo-less channels apart.
 *
 * It is pure and internal so the awkward inputs — empty names, punctuation-only names,
 * names in Arabic script — can be pinned here rather than discovered on a device.
 */
class ChannelLogoTest {

    @Test
    fun `two words give the first letter of each`() {
        assertEquals("K2", initialsOf("Kurdistan 24"))
        assertEquals("N1", initialsOf("NRT 1"))
    }

    @Test
    fun `hyphens and underscores separate words`() {
        assertEquals("KT", initialsOf("Kurdish-TV"))
        assertEquals("KM", initialsOf("kurd_max"))
    }

    @Test
    fun `a single word gives its first two letters`() {
        assertEquals("RU", initialsOf("Rudaw"))
    }

    @Test
    fun `a one letter word gives one letter`() {
        assertEquals("X", initialsOf("X"))
    }

    @Test
    fun `case is normalised so the same channel reads the same twice`() {
        assertEquals(initialsOf("kurdistan 24"), initialsOf("KURDISTAN 24"))
    }

    @Test
    fun `an empty name does not crash and says so`() {
        // Reached by a channel whose name was blank after formatting, which the
        // parser will produce from a malformed playlist line.
        assertEquals("?", initialsOf(""))
        assertEquals("?", initialsOf("   "))
    }

    @Test
    fun `a name of only separators has no words to draw from`() {
        assertEquals("?", initialsOf("---"))
        assertEquals("?", initialsOf("_-_-"))
    }

    @Test
    fun `runs of separators do not become empty words`() {
        // Blank entries between delimiters must not be counted, or the initials come
        // from the wrong words.
        assertEquals("K2", initialsOf("Kurdistan   24"))
        assertEquals("K2", initialsOf("Kurdistan - 24"))
    }

    @Test
    fun `a kurdish script name yields its own letters rather than nothing`() {
        // Arabic script has no case, so `uppercase()` is a no-op — but the letters
        // must still survive, since a logo-less Kurdish channel is exactly the case
        // this fallback exists for.
        val initials = initialsOf("کوردی")
        assertEquals("کو", initials)
        assertEquals(2, initials.length)
    }

    @Test
    fun `a kurdish script two word name yields one letter from each`() {
        assertEquals("کت", initialsOf("کوردی تەلەفزیۆن"))
    }

    @Test
    fun `mixed script names work`() {
        // The Latin first letter and the Kurdish first letter, both preserved. This is
        // the shape a bilingual channel name actually has.
        assertEquals("Nک", initialsOf("NRT کوردی"))
    }

    @Test
    fun `extra words beyond the first two are ignored`() {
        assertEquals("K2", initialsOf("Kurdistan 24 News HD"))
    }
}