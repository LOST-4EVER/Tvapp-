package com.example

import com.example.kurdishtv.ads.FullPageAdState
import com.example.kurdishtv.ui.screens.adActionSubtitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The line under the "Watch an ad" button.
 *
 * The button is the app's most valuable surface and its most fragile one: it is only
 * worth anything if it gets pressed, and nothing about a button says whether it can be
 * served at this moment. The line under it is the only thing telling the viewer whether
 * to tap or to wait, so the cases have to stay distinguishable from one another.
 */
class FullPageAdTest {

    @Test
    fun `a ready ad reads as an invitation`() {
        assertEquals(
            "Plays a short video ad, and keeps the app free",
            adActionSubtitle(sdkReady = true, adState = FullPageAdState.Ready)
        )
    }

    @Test
    fun `an ad still being fetched does not read as a refusal`() {
        assertEquals(
            "Preparing an ad…",
            adActionSubtitle(sdkReady = true, adState = FullPageAdState.Loading)
        )
    }

    @Test
    fun `an sdk that has not reported in yet does not read as a refusal either`() {
        // The cold start. The SDK callback has not fired, nothing has been requested, and
        // so there is no ad by definition. Saying so would be true and useless, and would
        // make every single launch look like it had tried and failed.
        assertEquals(
            "Getting the ad service ready…",
            adActionSubtitle(sdkReady = false, adState = FullPageAdState.Unavailable)
        )
    }

    @Test
    fun `no two states share a line`() {
        // This is the part that gets "tidied". One "no ad" line covers all four cases
        // below and every other assertion in this file still passes, while the button now
        // claims to be broken for the second or two it is merely working.
        val lines = listOf(
            adActionSubtitle(sdkReady = true, adState = FullPageAdState.Ready),
            adActionSubtitle(sdkReady = true, adState = FullPageAdState.Loading),
            adActionSubtitle(sdkReady = true, adState = FullPageAdState.Unavailable),
            adActionSubtitle(sdkReady = false, adState = FullPageAdState.Unavailable)
        )
        assertEquals("two of these read the same: $lines", lines.size, lines.toSet().size)
    }

    @Test
    fun `readiness beats the ad state`() {
        // While the SDK is still coming up its state is meaningless: it is `Unavailable`
        // because nothing could be requested yet, not because the network said no. The two
        // are different sentences to a viewer and are pinned as different here.
        assertNotEquals(
            adActionSubtitle(sdkReady = false, adState = FullPageAdState.Unavailable),
            adActionSubtitle(sdkReady = true, adState = FullPageAdState.Unavailable)
        )
    }

    @Test
    fun `every state has a line, so none of them is nameless`() {
        for (state in FullPageAdState.entries) {
            for (ready in listOf(true, false)) {
                assertTrue(
                    "$state with sdkReady=$ready has no line",
                    adActionSubtitle(ready, state).isNotBlank()
                )
            }
        }
    }
}
