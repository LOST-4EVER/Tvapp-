package com.example

import com.example.kurdishtv.ads.adsMayBeRequested
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.ui.screens.adsConsentHint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ads switch, and the promise under it.
 *
 * The switch is a privacy control, and the part of it that matters is invisible: the
 * placements disappearing is easy to see, but a viewer who has switched ads off has
 * also been told that nothing is requested. That half is kept by one comparison which
 * every request site goes through, and it fails silently — rewriting it to look only at
 * the SDK still draws no ads on the screen that hides them, and starts fetching for
 * everybody who declined.
 */
class AdsConsentTest {

    @Test
    fun `a viewer who switched ads off is never asked for one`() {
        assertFalse(adsMayBeRequested(adsAllowed = false, sdkReady = true))
        assertFalse(adsMayBeRequested(adsAllowed = false, sdkReady = false))
    }

    @Test
    fun `an ad may be requested only when the viewer agreed and the sdk is up`() {
        assertTrue(adsMayBeRequested(adsAllowed = true, sdkReady = true))
        assertFalse(adsMayBeRequested(adsAllowed = true, sdkReady = false))
    }

    @Test
    fun `the two halves are not interchangeable`() {
        // The dangerous edit is `adsAllowed && sdkReady` reduced to `sdkReady`. Reading
        // only the SDK half has to give a different answer for a viewer who declined, or
        // the switch has quietly become decoration.
        assertNotEquals(
            adsMayBeRequested(adsAllowed = false, sdkReady = true),
            adsMayBeRequested(adsAllowed = true, sdkReady = true)
        )
    }

    @Test
    fun `ads are on for an install that has never opened settings`() {
        // The switch arrived after the ads did. Defaulting to off would have silently
        // removed the app's only revenue for every install that never opened the screen.
        assertTrue(AppSettings().adsEnabled)
    }

    @Test
    fun `the switch reads differently in each position`() {
        val on = adsConsentHint(adsEnabled = true)
        val off = adsConsentHint(adsEnabled = false)
        assertNotEquals(on, off)
        assertTrue("the off line should say it is off: $off", off.startsWith("Off"))
        assertFalse("the on line should not read as off: $on", on.startsWith("Off"))
    }

    @Test
    fun `the off line promises only what the code does`() {
        // It claims two things, and both are enforced elsewhere: the request sites ask
        // `adsMayBeRequested`, and StartIoAdsHost does not start the SDK at all when ads
        // are off. Copy that promises more than that would be a lie on a settings screen.
        val off = adsConsentHint(adsEnabled = false)
        assertTrue("should say nothing is requested: $off", off.contains("no ads are requested"))
        assertTrue("should say the service is not started: $off", off.contains("not started"))
    }
}
