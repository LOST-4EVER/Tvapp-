package com.example.kurdishtv.model

import com.example.kurdishtv.ui.player.ResizeMode
import com.example.kurdishtv.ui.player.VideoColorFilter

/** Accent palettes offered in Settings. Mapped to real colors in the UI theme layer. */
enum class AccentColor(val displayName: String) {
    SUN_GOLD("Sun Gold"),
    EMBER("Ember"),
    ROSE("Rose"),
    JADE("Jade"),
    AZURE("Azure")
}

enum class ThemeMode(val displayName: String) {
    DARK("Dim"),
    AMOLED("AMOLED Black")
}

enum class MotionLevel(val displayName: String) {
    FULL("Expressive"),
    REDUCED("Reduced")
}

/**
 * User preferences. Immutable so the UI can safely diff it, and persisted by
 * [com.example.kurdishtv.data.SettingsStorage].
 */
data class AppSettings(
    val accent: AccentColor = AccentColor.SUN_GOLD,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val motion: MotionLevel = MotionLevel.FULL,
    val startCategory: CategoryFilter = CategoryFilter.ALL,
    val autoplay: Boolean = true,
    val showLogos: Boolean = true,
    val autoHideControls: Boolean = true,
    val livePulse: Boolean = true,
    /**
     * Material You: derive accent colors from the device wallpaper palette.
     * Requires Android 12+ (API 31); ignored on older releases.
     */
    val dynamicColor: Boolean = true,
    /**
     * How the video fills the screen: the FIT/FILL/ZOOM toggle in the player.
     *
     * Persisted because the choice is about the viewer's screen, not about a
     * stream: someone who wants FILL in portrait wants it tomorrow as well, and
     * an app that reset it on every cold start was asking them to set it again
     * each time. The side player pane draws the same picture, so it reads this
     * too rather than hard-coding its own idea of correctness.
     */
    val resizeMode: ResizeMode = ResizeMode.FILL,
    /** The video colour correction last chosen in the player, for the same reason. */
    val videoColorFilter: VideoColorFilter = VideoColorFilter.None
) {
    val reduceMotion: Boolean get() = motion == MotionLevel.REDUCED
    val isAmoled: Boolean get() = themeMode == ThemeMode.AMOLED
}
