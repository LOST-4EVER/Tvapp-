package com.example.kurdishtv.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first

/**
 * The ad SDK's whole lifecycle, driven off one boolean: the viewer's own preference.
 *
 * Rendered once from the app's root and deliberately draws nothing. Everything that
 * follows from "the viewer said yes" or "the viewer said no" happens here, so no screen
 * has to remember to start the SDK, fetch an ad, or let go of one:
 *
 *  - **Off means the SDK is never started.** Not started and then ignored — the
 *    initialiser is not called at all, so a viewer who declined ads is not paying for a
 *    remote-config fetch, its handlers and its threads on every launch.
 *  - **Off means a fetched ad is dropped.** [StartIoFullPage.release] lets go of it, so
 *    nothing holding a reference can still put it on screen.
 *  - **On waits for readiness on a snapshot flow rather than as part of composition.**
 *    The SDK reports in from its own thread some way into a launch, and reading the flag
 *    during composition would recompose whatever this sits under — which is the whole
 *    app, because it is rendered at the root. The parameters are safe to read because
 *    this draws nothing and has no children to invalidate.
 */
@Composable
fun StartIoAdsHost(enabled: Boolean) {
    val context = LocalContext.current

    LaunchedEffect(enabled) {
        StartIoAds.setAllowed(enabled)

        if (!enabled) {
            StartIoFullPage.release()
            return@LaunchedEffect
        }

        StartIoAds.init(context.applicationContext)

        snapshotFlow { StartIoAds.isReady }.first { it }

        // The app's one activity, and the window the SDK resolves its display target
        // through. Null means there is nothing to show an ad in, so nothing is fetched
        // either.
        val activity = context.findActivity() ?: return@LaunchedEffect
        StartIoFullPage.preload(activity)
    }
}
