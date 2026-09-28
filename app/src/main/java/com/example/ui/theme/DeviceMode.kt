package com.example.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the app is running on a television.
 *
 * Android reports this in the configuration's `uiMode`, and it is the only reliable
 * signal: a TV box and a tablet report comparable sizes and both declare the leanback
 * feature, so every layout decision made from width and height alone quietly put TV
 * boxes into a phone/tablet branch tuned for a screen held at arm's length.
 *
 * A `compositionLocalOf` rather than a `staticCompositionLocalOf` because the value
 * is not fixed for the process. `MainActivity` declares `uiMode` in its
 * `configChanges`, so the system does not recreate the activity when a device is
 * docked or undocked — Compose is handed the new configuration in place, and this
 * local has to follow it. The tracking variant recomposes only the handful of
 * readers rather than the whole tree.
 */
val LocalIsTv = compositionLocalOf { false }

/** Whether [this] configuration describes a television rather than a handheld. */
fun Configuration.isTvMode(): Boolean =
    (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
