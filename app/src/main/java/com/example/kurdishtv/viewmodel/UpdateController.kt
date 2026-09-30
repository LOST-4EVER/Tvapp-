package com.example.kurdishtv.viewmodel

import android.content.Context
import com.example.BuildConfig
import com.example.kurdishtv.update.ApkInstaller
import com.example.kurdishtv.update.AppUpdate
import com.example.kurdishtv.update.DownloadState
import com.example.kurdishtv.update.UpdateChecker
import com.example.kurdishtv.update.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles the complete lifecycle of in-app APK updates.
 *
 * Encapsulates update checks against GitHub releases, progressive file downloads,
 * package installer intents, and cryptographic APK signing certificate validation.
 */
class UpdateController(
    private val scope: CoroutineScope,
    private val updateChecker: UpdateChecker?,
    private val appContext: Context?
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _needsInstallPermission = MutableStateFlow(false)
    val needsInstallPermission: StateFlow<Boolean> = _needsInstallPermission.asStateFlow()

    private var updateJob: Job? = null

    private val updateDir: File?
        get() = appContext?.cacheDir?.let { File(it, "updates") }

    /**
     * Asks GitHub whether a newer build exists.
     *
     * @param silent when true, a failed check leaves the UI untouched so the
     *   automatic background check never surfaces an error the user did not ask for.
     */
    fun checkForUpdate(silent: Boolean = false) {
        val checker = updateChecker ?: return
        if (updateJob?.isActive == true) return

        updateJob = scope.launch {
            if (!silent) _updateState.value = UpdateState.Checking

            val result = checker.checkForUpdate(BuildConfig.VERSION_CODE)
            result
                .onSuccess { update ->
                    _updateState.value = when {
                        update != null -> UpdateState.Available(update)
                        silent -> UpdateState.Idle
                        else -> UpdateState.UpToDate(BuildConfig.VERSION_NAME)
                    }
                }
                .onFailure { error ->
                    if (!silent) {
                        _updateState.value = UpdateState.Failed(
                            error.message ?: "Could not reach the update server"
                        )
                    }
                }
        }
    }

    /** Downloads an available update, reporting progress into [updateState]. */
    fun downloadUpdate(update: AppUpdate) {
        val checker = updateChecker ?: return
        val dir = updateDir ?: return
        if (updateJob?.isActive == true) return

        updateJob = scope.launch {
            _updateState.value = UpdateState.Downloading(update, DownloadState.Idle)
            checker.download(update, dir) { progress ->
                _updateState.value = UpdateState.Downloading(update, progress)
            }
                .onSuccess { file ->
                    _updateState.value = UpdateState.ReadyToInstall(update, file.absolutePath)
                }
                .onFailure { error ->
                    _updateState.value = UpdateState.Failed(
                        error.message ?: "Download failed"
                    )
                }
        }
    }

    /**
     * Opens the system installer for a downloaded APK after verifying certificate signatures.
     */
    suspend fun installUpdate(update: AppUpdate, filePath: String): Boolean {
        val context = appContext ?: return false
        val file = File(filePath)
        if (!file.exists()) {
            _updateState.value = UpdateState.Failed("The downloaded update is missing, please retry")
            return false
        }
        if (!ApkInstaller.canRequestPackageInstalls(context)) {
            _needsInstallPermission.value = true
            return false
        }
        _needsInstallPermission.value = false
        val sameSignature = withContext(Dispatchers.IO) {
            ApkInstaller.isSignedBySameCertificate(context, file)
        }
        if (!sameSignature) {
            _updateState.value = UpdateState.Failed(
                "This update is signed with a different key than the app already on this " +
                    "device, so Android cannot upgrade over it. Uninstall Kurdish TV Live, " +
                    "then install the update. Uninstalling clears your favourites and " +
                    "watch history."
            )
            return false
        }
        val started = ApkInstaller.install(context, file)
        if (!started) {
            _updateState.value =
                UpdateState.Failed("This device could not start the package installer")
        }
        return started
    }

    /**
     * Non-suspend entry point for the Install button.
     */
    fun requestInstallUpdate(update: AppUpdate, filePath: String) {
        scope.launch {
            if (!installUpdate(update, filePath) && needsInstallPermissionNow()) {
                _updateState.value = UpdateState.ReadyToInstall(update, filePath)
                openInstallPermissionSettings()
            }
        }
    }

    /** Re-reads the package install permission state. */
    fun refreshInstallPermission() {
        val context = appContext ?: return
        _needsInstallPermission.value = !ApkInstaller.canRequestPackageInstalls(context)
    }

    private fun needsInstallPermissionNow(): Boolean {
        val context = appContext ?: return false
        return !ApkInstaller.canRequestPackageInstalls(context)
    }

    fun openInstallPermissionSettings() {
        appContext?.let { ApkInstaller.openInstallPermissionSettings(it) }
    }

    fun clearUpdateMessage() {
        _updateState.value = UpdateState.Idle
    }
}
