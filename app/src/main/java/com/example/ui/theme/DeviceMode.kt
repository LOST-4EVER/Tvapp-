package com.example.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.compositionLocalOf
import com.example.kurdishtv.model.DeviceMode

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
 *
 * It is also not fixed by the *setting*: a viewer on a box that misreports its
 * `uiMode`, or on a tablet driven by a keyboard, can override the detected answer
 * from Settings. See [DeviceMode.resolveIsTv], which is what turns the preference and
 * the configuration into the value published here.
 */
val LocalIsTv = compositionLocalOf { false }

/** Whether [this] configuration describes a television rather than a handheld. */
fun Configuration.isTvMode(): Boolean =
    (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

/**
 * Resolves a [DeviceMode] preference against what the device actually reports.
 *
 * `detected` is the configuration's answer and is consulted only for
 * [DeviceMode.AUTO]. An override has to win outright: the whole point of forcing a
 * mode is that the device is reporting something the user has told us to ignore, so
 * letting `uiMode` second-guess them would put the setting back where it started and
 * read as a control that does nothing.
 */
fun DeviceMode.resolveIsTv(detected: Boolean): Boolean = when (this) {
    DeviceMode.AUTO -> detected
    DeviceMode.TV -> true
    DeviceMode.MOBILE -> false
}
