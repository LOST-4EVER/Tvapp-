package com.example

import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.ChannelFilterEngine
import com.example.kurdishtv.model.foldForSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The script folding behind every Kurdish search.
 *
 * This is the failure that reads, from a sofa, as "search is broken": a channel is
 * plainly on screen with its name in Arabic script, the viewer types what they think
 * it says, and the result grid comes back empty. Nothing errors, nothing looks wrong,
 * and there is no way for the viewer to tell that the two strings they were comparing
 * are the same word written with a different letter.
 *
 * `foldForSearch` is internal and free precisely so that it can be pinned here rather
 * than only checked by typing at it on a device.
 */
class SearchFoldingTest {

    @Test
    fun `ascii names are untouched apart from case and separators`() {
        // The fast path every Latin playlist takes. It has to keep behaving exactly
        // as it did before the folding was added, or this fix breaks the search that
        // already worked.
        assertEquals("nrt1", foldForSearch("NRT 1"))
        assertEquals("nrt1", foldForSearch("nrt-1"))
        assertEquals("nrt1", foldForSearch("NRT_1"))
        assertEquals("kurdsat", foldForSearch("KurdSat"))
    }

    @Test
    fun `kurdish letters fold onto the plain letter they are written with`() {
        // U+0695 is "reh with small v"; U+0631 is a plain reh. One word, two
        // keyboards, and a viewer who cannot find the channel because of it.
        assertEquals(foldForSearch("ر"), foldForSearch("ڕ"))
        assertEquals(foldForSearch("ل"), foldForSearch("ڵ"))
        assertEquals(foldForSearch("و"), foldForSearch("ۆ"))
        assertEquals(foldForSearch("ی"), foldForSearch("ێ"))
        assertEquals(foldForSearch("ه"), foldForSearch("ە"))

        // Peh, tcheh and veh onto be/jeh/fe, and gaf onto kaf.
        assertEquals(foldForSearch("ب"), foldForSearch("پ"))
        assertEquals(foldForSearch("ج"), foldForSearch("چ"))
        assertEquals(foldForSearch("ف"), foldForSearch("ڤ"))
        assertEquals(foldForSearch("ک"), foldForSearch("گ"))
    }

    @Test
    fun `a name typed without the special letters still finds the channel`() {
        // The channel as a playlist stores it: with the small-v reh, the lam with
        // small v, and Arabic-Indic digits.
        val stored = listOf(
            Channel(id = "k1", name = "کوردستان ڕستی ١٢", category = "General")
        )
        // Typed with plain Arabic letters and ASCII digits, as a viewer holding a
        // stock Android keyboard would.
        assertEquals(
            "a Kurdish name has to be findable without its special letters",
            1,
            ChannelFilterEngine.filter(stored, CategoryFilter.ALL, "کوردستان رستی 12").size
        )
    }

    @Test
    fun `arabic indic digits match ascii digits`() {
        // A Kurdish playlist writes a channel number as ١٢ far more often than as 12,
        // and a remote's number pad can only ever produce the ASCII form.
        assertEquals("12", foldForSearch("١٢"))
        // Extended Arabic-Indic, which is what a Kurdish keyboard produces.
        assertEquals("12", foldForSearch("۱۲"))
    }

    @Test
    fun `alef variants and yeh forms collapse together`() {
        assertEquals(foldForSearch("ا"), foldForSearch("أ"))
        assertEquals(foldForSearch("ا"), foldForSearch("إ"))
        assertEquals(foldForSearch("ا"), foldForSearch("آ"))
        assertEquals(foldForSearch("ی"), foldForSearch("ى"))
        assertEquals(foldForSearch("ه"), foldForSearch("ة"))
    }

    @Test
    fun `presentation forms are folded to their base letters`() {
        // U+FEDD and U+FEE9 are ARABIC LETTER LAM INITIAL FORM and HEH FINAL FORM —
        // combining marks, not letters. `isLetterOrDigit` said no to both, so a name
        // written in shaped form lost exactly the letters that made it
        // distinguishable, and matched nothing at all.
        val shaped = "ﻝﻩ"
        assertEquals(foldForSearch("له"), foldForSearch(shaped))
        assertNotEquals("", foldForSearch(shaped))
    }

    @Test
    fun `zero width and tatweel characters are dropped rather than indexed`() {
        // A name pasted from a web page can carry a ZWNJ, a ZWJ or a soft hyphen
        // between two letters. If those survived into the key, one channel would have
        // two keys and neither typed form would match the other.
        assertEquals(foldForSearch("nrt"), foldForSearch("n‌rt"))
        assertEquals(foldForSearch("nrt"), foldForSearch("n‍rt"))
        assertEquals(foldForSearch("nrt"), foldForSearch("n­rt"))
        assertEquals(foldForSearch("nrt"), foldForSearch("n﻿rt"))
        // U+0640, tatweel, is what stretches a word for justification and is
        // routinely left behind in scraped Arabic text.
        assertEquals(foldForSearch("كرد"), foldForSearch("كـرد"))
    }

    @Test
    fun `latin accents are deliberately not folded`() {
        // The one thing the folding does *not* do, and the reason it is a script
        // fold rather than a general one. A Kurdish playlist writes its Latin names
        // without diacritics, so "ç" and "c" are two different channels here, not two
        // spellings of one. Folding them would make a real channel unfindable by its
        // own name — a different bug, in the opposite direction, and just as silent.
        assertNotEquals(foldForSearch("c"), foldForSearch("ç"))
    }
}
