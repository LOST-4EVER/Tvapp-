package com.example.kurdishtv.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
