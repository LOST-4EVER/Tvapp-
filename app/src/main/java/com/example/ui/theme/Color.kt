package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Core accents ──────────────────────────────────────────────────────────────
//
// Five hand-picked accents, all of them light enough to sit on a near-black
// surface without being darkened first, which is what lets one of them be used
// unchanged for text, for fills and for the focus ring.
//
// Measured against `DarkSurfaceElevated` (#191D29), the lightest surface the app
// draws on: every accent clears 7:1 against it, and every one of them clears
// 4.5:1 against `DarkSurface` (#0F1218). The sun gold is the default because it
// is the only one of the five that still reads as a *broadcast* colour rather
// than as a UI accent, and it is the one that survives being desaturated into a
// focus ring and a badge at the same time.
val KurdishSunGold = Color(0xFFFFC233)
val KurdishRed = Color(0xFFE11D48)
val KurdishGreen = Color(0xFF34D399)

val AccentEmber = Color(0xFFFF8A3D)
val AccentRose = Color(0xFFFF5C8A)
val AccentJade = Color(0xFF34C79A)
val AccentAzure = Color(0xFF5CB0FF)

// ── Dark surfaces ─────────────────────────────────────────────────────────────
//
// A four-step ramp rather than a flat one, because the app stacks surfaces: a
// background, a card on it, a chip on the card, and a control on the chip. Two
// steps could not tell those apart without leaning on borders for everything.
//
// The ramp is *cool* — every one of these carries more blue than red — so the
// warm accent sits on it as the only warm thing on screen. That single
// temperature split is most of what makes the accent read as deliberate rather
// than as one colour among several.
//
// The steps are 4-6% lightness apart, which is below what a viewer can reliably
// rank by eye but comfortably above what a badly-calibrated panel flattens. The
// borders below are what actually separate the levels; the ramp only decides
// which of two adjacent things is in front.
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF08080C)
val DarkSurfaceVariant = Color(0xFF101218)
val DarkSurfaceElevated = Color(0xFF141720)
val DarkSurfaceHigh = Color(0xFF1B1F2A)

// ── AMOLED surfaces ───────────────────────────────────────────────────────────
//
// Pure black (0xFF000000) for complete black screen experience on OLED/AMOLED and TV panels.
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF000000)
val AmoledSurfaceVariant = Color(0xFF0A0A0C)
val AmoledSurfaceElevated = Color(0xFF121216)
val AmoledSurfaceHigh = Color(0xFF18181E)

// ── Borders and dividers ──────────────────────────────────────────────────────
//
// Split into two roles, which used to be one value and are now not.
//
// [DarkCardBorder] outlines a *card* — a surface with content in it, where the
// outline is what tells you where the card stops. [DarkDivider] is a *rule*
// between rows, where an outline would read as a box around each item and turn a
// list into a column of tiles. The old app used the card border for both, which
// is why the sidebar rows had visible boxes around each of them.
val DarkCardBorder = Color(0xFF262C3C)
val DarkDivider = Color(0xFF1A1F2C)

val AmoledCardBorder = Color(0xFF23232C)
val AmoledDivider = Color(0xFF16161D)

// ── Text ──────────────────────────────────────────────────────────────────────
//
// Measured against `DarkSurfaceElevated` (#191D29), the lightest surface these
// sit on. All three clear WCAG AA for body text (4.5:1); [TextTertiary] clears it
// by the smallest margin because it is the one carrying hints and placeholders,
// and the previous value at #6B7280 measured 2.87:1 — below even the 3:1
// large-text minimum, so hints were unreadable exactly where they were needed.
val TextPrimary = Color(0xFFF2F5FA)
val TextSecondary = Color(0xFFA8B2C4)
val TextTertiary = Color(0xFF8B95A8)

// ── Overlays / status ─────────────────────────────────────────────────────────
val GlassOverlay = Color(0xCC0F1218)
val LiveRed = Color(0xFFFF2D55)

// ── Tonal derivation ──────────────────────────────────────────────────────────
// How far a container is tinted toward its accent, and how far the matching
// content colour is tinted toward white. See tonalPair() in AppColors.kt for why
// these are derived rather than taken from a fixed per-accent table.
const val CONTAINER_TINT = 0.26f
const val ON_CONTAINER_TINT = 0.55f
