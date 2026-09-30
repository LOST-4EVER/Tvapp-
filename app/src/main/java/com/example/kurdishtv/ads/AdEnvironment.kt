package com.example.kurdishtv.ads

import android.os.Build
import java.io.File

/**
 * Checks whether the current runtime environment can safely initialize and render ads.
 *
 * Emulators and headless cloud containers lack GPU hardware render nodes (/dev/dri/renderD*),
 * causing Mesa OpenGL to fail ("Failed to open rendernode: No such file or directory")
 * and Chromium's WebView renderer process to crash (aw_browser_terminator.cc).
 * Furthermore, Start.io's publisher policy restricts serving ads on emulated devices.
 */
object AdEnvironment {

    /**
     * Determines whether the app is executing inside an Android emulator or virtualized container.
     */
    fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT?.lowercase().orEmpty()
        val model = Build.MODEL?.lowercase().orEmpty()
        val manufacturer = Build.MANUFACTURER?.lowercase().orEmpty()
        val brand = Build.BRAND?.lowercase().orEmpty()
        val device = Build.DEVICE?.lowercase().orEmpty()
        val product = Build.PRODUCT?.lowercase().orEmpty()
        val hardware = Build.HARDWARE?.lowercase().orEmpty()
        val board = Build.BOARD?.lowercase().orEmpty()

        return fingerprint.startsWith("generic")
            || fingerprint.startsWith("unknown")
            || model.contains("google_sdk")
            || model.contains("emulator")
            || model.contains("android sdk built for")
            || manufacturer.contains("genymotion")
            || (brand.startsWith("generic") && device.startsWith("generic"))
            || product.contains("google_sdk")
            || product.contains("sdk_gphone")
            || product.contains("sdk_google")
            || product.contains("vbox")
            || product.contains("emulator")
            || product.contains("simulator")
            || hardware.contains("goldfish")
            || hardware.contains("ranchu")
            || hardware.contains("cutf")
            || hardware.contains("cuttlefish")
            || board.contains("goldfish")
            || board.contains("ranchu")
            || board.contains("cutf")
    }

    /**
     * Checks if the Linux Direct Rendering Infrastructure (DRI) render node is missing.
     * When /dev/dri/renderD* is missing, Mesa fails with "Failed to open rendernode"
     * and Chromium WebView renderer crashes with code -1.
     */
    fun isMissingRenderNode(): Boolean {
        return try {
            val driDir = File("/dev/dri")
            if (!driDir.exists() || !driDir.isDirectory) {
                true
            } else {
                val nodes = driDir.listFiles()
                nodes == null || nodes.none { it.name.startsWith("renderD") }
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Returns true only when running on a physical Android device with valid hardware rendering.
     */
    fun isAdRenderingSupported(): Boolean {
        if (isEmulator()) return false
        if (isMissingRenderNode()) return false
        return true
    }
}
