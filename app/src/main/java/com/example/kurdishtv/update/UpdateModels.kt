package com.example.kurdishtv.update

/** A release discovered on GitHub, reduced to what the app needs to act on it. */
data class AppUpdate(
    val versionName: String,
    val versionCode: Int,
    val releaseUrl: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val publishedAt: String?,
    val notes: String?
) {
    /**
     * Whether this release should be offered to the user.
     *
     * Compared on the numeric build code rather than the version string, because
     * "1.0.9" vs "1.0.10" compares incorrectly as text and would hide real updates.
     */
    fun isNewerThan(currentVersionCode: Int): Boolean = versionCode > currentVersionCode
}

/** Progress of an in-flight APK download. */
sealed interface DownloadState {
    data object Idle : DownloadState
    data class Running(val bytesRead: Long, val totalBytes: Long) : DownloadState {
        /** 0f..1f, or null when the server did not send a content length. */
        val fraction: Float?
            get() = if (totalBytes > 0) (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f) else null
    }
    data class Done(val filePath: String, val sizeBytes: Long) : DownloadState
    data class Failed(val message: String) : DownloadState
}

/** Everything the update UI needs to render. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState

    /** Checked and already current. */
    data class UpToDate(val currentVersion: String) : UpdateState

    data class Available(val update: AppUpdate) : UpdateState

    /** Downloading or downloaded; carries the same progress state as the download. */
    data class Downloading(val update: AppUpdate, val progress: DownloadState) : UpdateState

    /** APK is on disk and ready to hand to the package installer. */
    data class ReadyToInstall(val update: AppUpdate, val filePath: String) : UpdateState

    data class Failed(val message: String) : UpdateState
}
