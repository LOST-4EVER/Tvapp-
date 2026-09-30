package com.example.kurdishtv.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.kurdishtv.network.NetworkClient
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener

/**
 * What the "watch an ad" action can currently do.
 *
 * Three states rather than a boolean because the two ways of not being ready are not
 * the same thing to the viewer: one of them resolves itself in a second, and the other
 * will not until they ask again.
 */
enum class FullPageAdState {
    /** Nothing is loaded and nothing is in flight. */
    Unavailable,

    /** A request is out, and an ad is expected shortly. */
    Loading,

    /** An ad is loaded and will fill the screen the moment it is asked for. */
    Ready,
}

/**
 * The Start.io full-page (interstitial) ad behind the Settings button.
 *
 * Wraps the three things that separate a button that earns from a button that looks
 * broken:
 *
 *  - **It is preloaded, not loaded on tap.** A load takes a second or more, and for
 *    every one of them the viewer is standing there watching a button do nothing. The
 *    ad is fetched as soon as the SDK reports ready and kept, so the tap is instant.
 *  - **A tap is never thrown away.** Asking while a request is still out sets
 *    `showWhenReady`, and the ad shows by itself the moment it lands. Dropping that tap
 *    would be the most expensive bug this class could have: it loses the impression and
 *    it teaches the viewer that the button does nothing.
 *  - **An ad belongs to an activity.** The SDK resolves the activity to display through,
 *    so one loaded against an activity that has since been recreated cannot be shown
 *    against the new one. A changed activity drops the loaded ad and starts over.
 */
object StartIoFullPage {

    var state by mutableStateOf(FullPageAdState.Unavailable)
        private set

    /** The loaded ad, or null when there is nothing to show. */
    private var ad: StartAppAd? = null

    /** The activity [ad] was loaded against. */
    private var owner: Activity? = null

    /** Set when the viewer asked for an ad that had not arrived yet. */
    private var showWhenReady = false

    /**
     * Fetches an ad in the background, if there is not one already.
     *
     * Idempotent, and safe to call from a `LaunchedEffect` keyed on the SDK's readiness —
     * which is how the app calls it, so on a cold start it runs once while the SDK is
     * still initialising and once more when it reports in.
     */
    fun preload(activity: Activity) {
        // Nothing can be requested before the SDK is up: the request is dropped with no
        // callback and no error, and the screen would sit on "loading" for ever.
        if (!StartIoAds.isReady) return

        // A finishing activity is not something to load an ad against — the callback
        // would land after the screen it belongs to is gone.
        if (activity.isFinishing || activity.isDestroyed) return

        if (owner !== activity) {
            // A recreated activity invalidates whatever was loaded against the old one.
            owner = activity
            ad = null
            showWhenReady = false
            state = FullPageAdState.Unavailable
        }

        if (ad != null || state == FullPageAdState.Loading) return
        load(activity)
    }

    /**
     * Shows the loaded ad, or fetches one and shows it as soon as it lands.
     *
     * Returns whether something is on screen right now. `false` means a request is in
     * flight and the ad will appear by itself, so the caller never has to decide what to
     * tell a viewer whose tap did nothing.
     */
    fun show(activity: Activity): Boolean {
        val loaded = ad
        if (loaded == null) {
            showWhenReady = true
            preload(activity)
            return false
        }

        ad = null
        state = FullPageAdState.Unavailable
        return try {
            loaded.showAd(object : AdDisplayListener {
                override fun adDisplayed(ad: Ad) {}

                override fun adClicked(ad: Ad) {}

                // The next ad is fetched as this one closes rather than when the button is
                // pressed again, so a second tap is as instant as the first. Both of these
                // can fire for one showing; `preload` is what keeps that from being two
                // requests.
                override fun adHidden(ad: Ad) = preload(activity)

                override fun adNotDisplayed(ad: Ad) = preload(activity)
            })
        } catch (t: Throwable) {
            // The same contract as the initialiser: an ad can never take the app down.
            NetworkClient.logDebug("Start.io full-page show failed: ${t.javaClass.simpleName}")
            false
        }
    }

    private fun load(activity: Activity) {
        state = FullPageAdState.Loading
        try {
            val candidate = StartAppAd(activity)
            candidate.loadAd(object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    // The activity may have been recreated while this was out. Keeping the
                    // ad would hand `show` one bound to a screen that no longer exists, so
                    // it is dropped and the new activity's own preload fetches another.
                    if (owner !== activity) return

                    this@StartIoFullPage.ad = candidate
                    state = FullPageAdState.Ready

                    // The viewer asked while this was in flight. Show it now rather than
                    // making them ask twice.
                    if (showWhenReady) {
                        showWhenReady = false
                        show(activity)
                    }
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    // The pending tap is cleared, not carried. The next successful load may
                    // be minutes away, and a full-screen ad appearing then, unprompted, is
                    // worse than the tap having done nothing.
                    showWhenReady = false
                    state = FullPageAdState.Unavailable
                    NetworkClient.logDebug(
                        "Start.io full-page unfilled: ${ad?.errorMessage ?: "no fill"}"
                    )
                }
            })
        } catch (t: Throwable) {
            showWhenReady = false
            state = FullPageAdState.Unavailable
            NetworkClient.logDebug("Start.io full-page load failed: ${t.javaClass.simpleName}")
        }
    }
}

/**
 * The [Activity] a context belongs to, or null when there is not one.
 *
 * `LocalContext` inside `setContent` *is* the activity, but it is typed as a `Context`
 * and it is not guaranteed to be one — a `ContextThemeWrapper` sits in between on some
 * devices. Unwrapping rather than casting means a miss is a null instead of a crash.
 */
internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
