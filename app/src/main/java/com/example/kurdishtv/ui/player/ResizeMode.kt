package com.example.kurdishtv.ui.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

@OptIn(UnstableApi::class)
enum class ResizeMode(val mode: Int, val label: String) {
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit"),
    FILL(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Fill"),
    ZOOM(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom")
}
