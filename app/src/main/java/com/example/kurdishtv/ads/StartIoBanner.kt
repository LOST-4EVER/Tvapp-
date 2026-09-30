package com.example.kurdishtv.ads

import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kurdishtv.network.NetworkClient
import com.startapp.sdk.ads.banner.BannerCreator
import com.startapp.sdk.ads.banner.BannerFormat
import com.startapp.sdk.ads.banner.BannerListener
import com.startapp.sdk.ads.banner.BannerRequest

/**
 * A Start.io banner, or nothing at all.
 *
 * The SDK's banner is an Android `View`, so it is hosted through `AndroidView` rather
 * than reimplemented. Two details are load-bearing:
 *
 *  - **The request waits for [StartIoAds.isReady].** A `BannerRequest` issued before
 *    the SDK has finished initialising is dropped with no callback and no error, so a
 *    request made at composition time leaves the screen permanently blank on a cold
 *    start — the one launch where it is guaranteed not to be ready yet. Keying the
 *    effect on the readiness flag means the request fires the moment the SDK reports
 *    in, and not before.
 *
 *  - **No fill draws nothing.** The holder view is only rendered once a creator has
 *    produced one, and a failed impression clears it again. A bordered empty slot
 *    reads as a broken ad and is worse than no ad at all — and on a television, where
 *    Start.io's display inventory does not serve, that is the normal outcome rather
 *    than an error worth surfacing.
 */
@Composable
fun StartIoBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var adView by remember { mutableStateOf<View?>(null) }

    LaunchedEffect(StartIoAds.isReady) {
        if (!StartIoAds.isReady) return@LaunchedEffect

        BannerRequest(context.applicationContext)
            .setAdFormat(BannerFormat.BANNER)
            .load { creator: BannerCreator?, error: String? ->
                if (creator == null) {
                    NetworkClient.logDebug("Start.io banner unfilled: ${error ?: "no fill"}")
                    adView = null
                } else {
                    adView = creator.create(
                        context.applicationContext,
                        object : BannerListener {
                            override fun onReceiveAd(banner: View) {}

                            // The request succeeded but the banner still failed to
                            // fill. Clear it so the slot collapses instead of
                            // reserving space for an ad that is not there.
                            override fun onFailedToReceiveAd(banner: View) {
                                adView = null
                            }

                            override fun onImpression(banner: View) {}

                            override fun onClick(banner: View) {}
                        }
                    )
                }
            }
    }

    val view = adView
    if (view != null) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { view }
        )
    }
}
