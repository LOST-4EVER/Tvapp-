package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Core accents ──────────────────────────────────────────────────────────────
val KurdishSunGold = Color(0xFFFFB703)
val KurdishRed = Color(0xFFD90429)
val KurdishGreen = Color(0xFF2EC4B6)

// Accent hues used by the selectable palettes in Settings
val AccentEmber = Color(0xFFFF7A00)
val AccentRose = Color(0xFFFF4D6D)
val AccentJade = Color(0xFF21C08B)
val AccentAzure = Color(0xFF4DA3FF)

// ── Dark surfaces ─────────────────────────────────────────────────────────────
val DarkBackground = Color(0xFF090A0F)
val DarkSurface = Color(0xFF13151F)
val DarkSurfaceVariant = Color(0xFF1D2130)
val DarkSurfaceElevated = Color(0xFF262C3E)
val DarkCardBorder = Color(0xFF2D3347)

// ── AMOLED surfaces ───────────────────────────────────────────────────────────
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0A0D)
val AmoledSurfaceVariant = Color(0xFF141419)
val AmoledSurfaceElevated = Color(0xFF1D1D24)
val AmoledCardBorder = Color(0xFF26262E)

// ── Text ──────────────────────────────────────────────────────────────────────
// Contrast measured against the lightest surface these sit on (DarkSurfaceElevated,
// #262C3E), which is where the app draws its highest-contrast cards. Tertiary text
// is used for hints, placeholders and inactive segment labels, so it has to clear
// the WCAG AA 4.5:1 body-text threshold rather than the 3:1 large-text one.
val TextPrimary = Color(0xFFF8F9FA)
val TextSecondary = Color(0xFFA2A9B8)

// Was #6B7280, which measured 2.87:1 on DarkSurfaceElevated — below even the 3:1
// large-text minimum, so hints and placeholders were effectively unreadable on the
// surfaces the app uses most. #8D94A2 measures 4.56:1 on the worst surface and
// 5.25:1 or better everywhere else.
val TextTertiary = Color(0xFF8D94A2)

// ── Overlays / status ─────────────────────────────────────────────────────────
val GlassOverlay = Color(0xB313151F)
val LiveRed = Color(0xFFFF1744)

// ── Tonal derivation ──────────────────────────────────────────────────────────
// How far a container is tinted toward its accent, and how far the matching
// content colour is tinted toward white. See tonalPair() in AppColors.kt for why
// these are derived rather than taken from a fixed per-accent table.
const val CONTAINER_TINT = 0.30f
const val ON_CONTAINER_TINT = 0.45f
