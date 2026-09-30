package com.example.kurdishtv.ads

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.kurdishtv.network.NetworkClient

/**
 * Hardens WebViews against renderer crashes (aw_browser_terminator.cc).
 *
 * Ensures that if a WebView renderer process terminates unexpectedly,
 * onRenderProcessGone returns true to prevent the host application from being killed.
 */
object SafeWebViewHelper {

    /**
     * Recursively traverses a view hierarchy, hardening any WebView found within it.
     */
    fun hardenViewHierarchy(view: View) {
        if (view is WebView) {
            hardenWebView(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                hardenViewHierarchy(view.getChildAt(i))
            }
        }
    }

    private fun hardenWebView(webView: WebView) {
        try {
            // Apply software layer if hardware render nodes are absent
            if (AdEnvironment.isMissingRenderNode()) {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                webView.webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(
                        view: WebView,
                        detail: RenderProcessGoneDetail
                    ): Boolean {
                        val didCrash = detail.didCrash()
                        NetworkClient.logDebug(
                            "Handled onRenderProcessGone gracefully, didCrash: $didCrash"
                        )
                        try {
                            (view.parent as? ViewGroup)?.removeView(view)
                            view.destroy()
                        } catch (_: Throwable) {}
                        // Returning true signals that the app handled the crash,
                        // preventing aw_browser_terminator from killing the app process.
                        return true
                    }
                }
            }
        } catch (t: Throwable) {
            NetworkClient.logDebug("Failed to harden WebView: ${t.message}")
        }
    }
}
