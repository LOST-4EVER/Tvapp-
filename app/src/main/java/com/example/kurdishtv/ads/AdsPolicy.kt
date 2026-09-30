package com.example.kurdishtv.ads

/**
 * Whether an ad may be requested at all.
 *
 * Both halves are load-bearing, and the second one is the dangerous one. Somebody who
 * has switched ads off has been promised that nothing is requested — no banner, no
 * full-page ad, no non-personalised fallback — and this comparison, not the hidden
 * button, is what keeps that promise: every request site in the app goes through it.
 *
 * `adsAllowed && sdkReady` written as just `sdkReady` still draws no ads on the screen
 * that hides them, so nothing visible changes. What changes is that the app starts
 * fetching ads for every viewer who declined them. That is why the rule lives here on
 * its own, away from the SDK, and why it has a test.
 */
internal fun adsMayBeRequested(adsAllowed: Boolean, sdkReady: Boolean): Boolean =
    adsAllowed && sdkReady
