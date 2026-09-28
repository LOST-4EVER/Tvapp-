package com.example.kurdishtv.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a downloaded APK to Android's package installer.
 *
 * A `file://` URI cannot be used since Android 7 (it throws
 * `FileUriExposedException`), so the APK is exposed through the app's
 * [FileProvider] as a `content://` URI instead. That is also what allows the
 * installer to read the file without granting it world-readable permissions.
 */
object ApkInstaller {

    private const val TAG = "ApkInstaller"
    private const val AUTHORITY = "com.aistudio.kurdishtv.live.fileprovider"

    /**
     * Whether [apk] is signed by the same certificate as the copy of this app
     * currently installed on the device.
     *
     * Android refuses to upgrade an app whose signing key changed, and it reports
     * that only inside the system installer's own dialog — as "App not installed
     * as package conflicts with an existing package", with no hint that an
     * uninstall is what is actually needed. Checking the certificates up front
     * lets the app say the useful thing instead.
     *
     * Returns true when the question cannot be answered (the archive is unreadable,
     * the package is not installed, or the platform will not expose signatures),
     * so an inconclusive check never blocks an install that would have worked.
     */
    fun isSignedBySameCertificate(context: Context, apk: File): Boolean {
        if (!apk.exists()) return true
        val installed = signingInfo(context, context.packageName) ?: return true
        val candidate = signingInfo(context, apk.absolutePath) ?: return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val a = installed.signingInfo ?: return true
            val b = candidate.signingInfo ?: return true
            // apkContentsSigners is the *current* signer set, so a key that was
            // rotated through the v3 lineage still compares equal to itself.
            return a.apkContentsSigners.contentEquals(b.apkContentsSigners)
        }

        @Suppress("DEPRECATION")
        val a = installed.signatures ?: return true
        @Suppress("DEPRECATION")
        val b = candidate.signatures ?: return true
        return a.size == b.size && a.contentEquals(b)
    }

    /**
     * Reads a package's signing info, or null when it cannot be read.
     *
     * Catches [Exception] rather than only `NameNotFoundException` on purpose. The
     * archive path can fail in several other ways on a real device — an OEM package
     * manager throwing on a malformed APK, a SecurityException from a restricted
     * profile — and this runs while the user is tapping Install. Letting any of
     * those escape took the whole app down instead of falling through to the
     * "cannot tell, so let Android decide" behaviour every other failure uses.
     */
    @Suppress("DEPRECATION")
    private fun signingInfo(context: Context, source: String): PackageInfo? = try {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        if (source == context.packageName) {
            context.packageManager.getPackageInfo(source, flags)
        } else {
            context.packageManager.getPackageArchiveInfo(source, flags)
        }
    } catch (e: Exception) {
        Log.w(TAG, "signingInfo: could not read signing info for $source", e)
        null
    }

    /**
     * Launches the install prompt for [apk].
     *
     * @return true when the installer was started. Returns false when the device
     *   cannot install the package, so the caller can show a useful message
     *   instead of the user tapping a button that silently does nothing.
     */
    fun install(context: Context, apk: File): Boolean {
        if (!apk.exists()) {
            Log.w(TAG, "install: APK missing at ${apk.absolutePath}")
            return false
        }

        val uri: Uri = try {
            FileProvider.getUriForFile(context, AUTHORITY, apk)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "install: FileProvider could not expose the APK", e)
            return false
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "install: no package installer available", e)
            false
        }
    }

    /**
     * Whether the user has allowed this app to install packages.
     *
     * On Android 8+ each app must be granted this permission separately, so the
     * first install attempt fails unless we have been to the settings screen.
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    /**
     * Opens the "allow from this source" screen, which is a per-app toggle rather
     * than a runtime permission dialog.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Some OEM builds do not expose the per-app screen; fall back to the
            // list of apps that can install unknown sources.
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure {
                Log.e(TAG, "openInstallPermissionSettings: no settings screen found", it)
            }
        }
    }

    private const val APK_MIME = "application/vnd.android.package-archive"
}
