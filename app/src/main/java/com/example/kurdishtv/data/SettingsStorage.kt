package com.example.kurdishtv.data

import android.content.Context
import android.content.SharedPreferences
import com.example.kurdishtv.model.AccentColor
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.MotionLevel
import com.example.kurdishtv.model.ThemeMode

/** Persists [AppSettings] in SharedPreferences. */
class SettingsStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("kurdish_tv_settings_v1", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        return try {
            AppSettings(
                accent = enumOrDefault(prefs.getString(KEY_ACCENT, null), AccentColor.SUN_GOLD),
                themeMode = enumOrDefault(prefs.getString(KEY_THEME, null), ThemeMode.DARK),
                motion = enumOrDefault(prefs.getString(KEY_MOTION, null), MotionLevel.FULL),
                startCategory = enumOrDefault(prefs.getString(KEY_START_CATEGORY, null), CategoryFilter.ALL),
                autoplay = prefs.getBoolean(KEY_AUTOPLAY, true),
                showLogos = prefs.getBoolean(KEY_SHOW_LOGOS, true),
                autoHideControls = prefs.getBoolean(KEY_AUTO_HIDE, true),
                livePulse = prefs.getBoolean(KEY_LIVE_PULSE, true),
                dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
            )
        } catch (_: Exception) {
            AppSettings()
        }
    }

    fun save(settings: AppSettings) {
        try {
            prefs.edit()
                .putString(KEY_ACCENT, settings.accent.name)
                .putString(KEY_THEME, settings.themeMode.name)
                .putString(KEY_MOTION, settings.motion.name)
                .putString(KEY_START_CATEGORY, settings.startCategory.name)
                .putBoolean(KEY_AUTOPLAY, settings.autoplay)
                .putBoolean(KEY_SHOW_LOGOS, settings.showLogos)
                .putBoolean(KEY_AUTO_HIDE, settings.autoHideControls)
                .putBoolean(KEY_LIVE_PULSE, settings.livePulse)
                .putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
                .apply()
        } catch (_: Exception) {}
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        name?.let { stored -> runCatching { enumValueOf<T>(stored) }.getOrNull() } ?: default

    companion object {
        private const val KEY_ACCENT = "accent"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_MOTION = "motion"
        private const val KEY_START_CATEGORY = "start_category"
        private const val KEY_AUTOPLAY = "autoplay"
        private const val KEY_SHOW_LOGOS = "show_logos"
        private const val KEY_AUTO_HIDE = "auto_hide_controls"
        private const val KEY_LIVE_PULSE = "live_pulse"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}
