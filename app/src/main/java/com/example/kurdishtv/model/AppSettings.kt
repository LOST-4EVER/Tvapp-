package com.example.kurdishtv.model

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
    val dynamicColor: Boolean = true
) {
    val reduceMotion: Boolean get() = motion == MotionLevel.REDUCED
    val isAmoled: Boolean get() = themeMode == ThemeMode.AMOLED
}
