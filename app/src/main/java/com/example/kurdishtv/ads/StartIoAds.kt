package com.example.kurdishtv.ads

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.BuildConfig
import com.example.kurdishtv.network.NetworkClient
import com.startapp.sdk.adsbase.StartAppSDK
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Start.io (formerly StartApp) ad SDK lifecycle.
 *
 * Wraps the two things that are easy to get wrong about this SDK:
 *
 *  - it has to be initialised **once**, before any ad is requested, and its callback
 *    is the only reliable signal that it is ready. A banner requested earlier is
 *    dropped silently — no callback, no error — so a screen that requests at
 *    composition time is simply blank on a cold start.
 *  - the app id is not a secret and is not read from the environment. It is the
 *    Android app id from <https://portal.start.io>, and it is the only thing tying
 *    an installed build to the account that gets paid. Somebody who wanted to
 *    repoint the revenue would have to edit this file and ship a new APK, which is
 *    the right amount of friction for a value that must be identical in every build.
 *
 * Test ads are on in debug and off in release. Serving real inventory while
 * developing counts against the account and can get it flagged, which is the
 * documented reason the SDK ships the switch.
 */
object StartIoAds {

    /** The Android app id from the Start.io portal. */
    private const val APP_ID = "208672276"

    private val started = AtomicBoolean(false)

    /**
     * Whether the SDK has finished initialising.
     *
     * Compose state rather than a plain boolean, because the banner reads it during
     * composition: it is the change from `false` to `true` that gives the screen a
     * second chance to request an ad after the callback has fired.
     */
    var isReady by mutableStateOf(false)
        private set

    /**
     * Initialises the SDK at most once per process.
     *
     * Idempotent so it can be called from every activity `onCreate` — the activity
     * is recreated on a configuration change, and a second `init()` while the first
     * is still in flight is wasted work at best.
     */
    fun init(context: Context) {
        if (!started.compareAndSet(false, true)) return
        try {
            StartAppSDK.setTestAdsEnabled(BuildConfig.DEBUG)
            StartAppSDK.initParams(context.applicationContext, APP_ID)
                // The "return ad" is a full-screen ad the SDK shows on app exit. The
                // app has its own navigation and its own update flow, and a second
                // uncontrolled full-screen surface on the way out is not something
                // this product wants.
                .setReturnAdsEnabled(false)
                .setCallback { isReady = true }
                .init()
        } catch (t: Throwable) {
            // An ad SDK must never be able to take the app down. A device with no
            // Play services, a missing native library or a rejected app id all
            // surface here, and every one of them has to degrade to "no ads", not to
            // a crash on launch. The flag is released so a later attempt can retry,
            // and `isReady` simply stays false.
            started.set(false)
            NetworkClient.logDebug("Start.io init failed: ${t.javaClass.simpleName}: ${t.message}")
        }
    }
}
