package com.kmhmubin.kothagolp.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// KOTHAGOLP BRAND — Ink Blue
// Seed: #3D6FFF (cobalt from logo) — M3 tonal palette, WCAG AA
// ============================================

// ── Dark scheme roles ─────────────────────────
val InkPrimary                = Color(0xFFA8C7FF)   // tone-80
val InkOnPrimary              = Color(0xFF003068)   // tone-20
val InkPrimaryContainer       = Color(0xFF00448D)   // tone-30
val InkOnPrimaryContainer     = Color(0xFFD6E3FF)   // tone-90

val InkSecondary              = Color(0xFFBCC8E7)
val InkOnSecondary            = Color(0xFF263141)
val InkSecondaryContainer     = Color(0xFF3C4758)
val InkOnSecondaryContainer   = Color(0xFFD8E3FF)

val InkTertiary               = Color(0xFFD4BBFF)   // violet complement
val InkOnTertiary             = Color(0xFF3B2070)
val InkTertiaryContainer      = Color(0xFF523688)
val InkOnTertiaryContainer    = Color(0xFFEBDDFF)

val InkDarkBackground         = Color(0xFF111318)
val InkDarkOnBackground       = Color(0xFFE2E2E9)
val InkDarkSurfaceVariant     = Color(0xFF41484F)
val InkDarkOnSurfaceVariant   = Color(0xFFC1C7CE)
val InkDarkSurfaceLowest      = Color(0xFF0C0E13)
val InkDarkSurfaceLow         = Color(0xFF191C20)
val InkDarkSurface            = Color(0xFF1D2024)
val InkDarkSurfaceHigh        = Color(0xFF272A2F)
val InkDarkSurfaceHighest     = Color(0xFF323539)
val InkDarkOutline            = Color(0xFF8B9198)
val InkDarkOutlineVariant     = Color(0xFF41484F)

// ── Light scheme roles ────────────────────────
val InkLightPrimary               = Color(0xFF1B5FCC)   // tone-40
val InkLightOnPrimary             = Color(0xFFFFFFFF)
val InkLightPrimaryContainer      = Color(0xFFD6E3FF)   // tone-90
val InkLightOnPrimaryContainer    = Color(0xFF001A45)   // tone-10

val InkLightSecondary             = Color(0xFF4F5D72)
val InkLightOnSecondary           = Color(0xFFFFFFFF)
val InkLightSecondaryContainer    = Color(0xFFD3E4F5)
val InkLightOnSecondaryContainer  = Color(0xFF0C1D2C)

val InkLightTertiary              = Color(0xFF6554A4)
val InkLightOnTertiary            = Color(0xFFFFFFFF)
val InkLightTertiaryContainer     = Color(0xFFE9DDFF)
val InkLightOnTertiaryContainer   = Color(0xFF201060)

val InkLightBackground            = Color(0xFFF8F9FF)
val InkLightOnBackground          = Color(0xFF191C20)
val InkLightSurfaceVariant        = Color(0xFFDDE3EA)
val InkLightOnSurfaceVariant      = Color(0xFF41484F)
val InkLightSurfaceLowest         = Color(0xFFFFFFFF)
val InkLightSurfaceLow            = Color(0xFFF2F4FB)
val InkLightSurface               = Color(0xFFECEEF4)
val InkLightSurfaceHigh           = Color(0xFFE6E8EF)
val InkLightSurfaceHighest        = Color(0xFFE0E2E9)
val InkLightOutline               = Color(0xFF71787F)
val InkLightOutlineVariant        = Color(0xFFC1C7CE)

// Backward-compat alias — used by TTSSettingsPanel as default accent
val SunsetPrimary = Color(0xFFFF865B)

// ============================================
// NEUTRAL — Zinc (kept for reader themes and classic preset)
// ============================================
val Zinc50 = Color(0xFFFAFAFA)
val Zinc100 = Color(0xFFF4F4F5)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc500 = Color(0xFF71717A)
val Zinc600 = Color(0xFF52525B)
val Zinc700 = Color(0xFF3F3F46)
val Zinc800 = Color(0xFF27272A)
val Zinc900 = Color(0xFF18181B)
val Zinc950 = Color(0xFF09090B)

// ============================================
// LEGACY Orange (Classic preset)
// ============================================
val Orange50  = Color(0xFFFFF7ED)
val Orange100 = Color(0xFFFFEDD5)
val Orange200 = Color(0xFFFED7AA)
val Orange300 = Color(0xFFFDBA74)
val Orange400 = Color(0xFFFB923C)
val Orange500 = Color(0xFFF97316)
val Orange600 = Color(0xFFEA580C)
val Orange700 = Color(0xFFC2410C)
val Orange800 = Color(0xFF9A3412)
val Orange900 = Color(0xFF7C2D12)

// ============================================
// SEMANTIC COLORS
// ============================================
val Success = Color(0xFF22C55E)      // Green-500
val SuccessLight = Color(0xFF4ADE80) // Green-400
val Error = Color(0xFFEF4444)        // Red-500
val ErrorLight = Color(0xFFF87171)   // Red-400
val Warning = Color(0xFFF59E0B)      // Amber-500
val Info = Color(0xFF3B82F6)         // Blue-500

// ============================================
// STATUS COLORS
// ============================================
val StatusReading = Info
val StatusSpicy = Color(0xFFF97316)
val StatusCompleted = Success
val StatusOnHold = Warning
val StatusPlanToRead = Color(0xFF8B5CF6)
val StatusDROPPED = Error

val AppOrange = Color(0xFFFF6B35)     // brand orange — reader TTS accent, streak fire

// New chapters indicator
val NewChapters = Color(0xFF10B981)      // Emerald-500
val NewChaptersLight = Color(0xFF34D399) // Emerald-400

// Additional semantic accents
val AccentCyan  = Color(0xFF06B6D4)  // Cyan-500 — TTS auto-advance, feature flags
val AccentTeal  = Color(0xFF14B8A6)  // Teal-500 — secondary accents


// ============================================
// READER THEMES
// ============================================

// Dark Theme (Default)
val ReaderDarkBackground = Zinc950
val ReaderDarkText = Zinc300
val ReaderDarkSecondary = Zinc500

// Light Theme
val ReaderLightBackground = Zinc50
val ReaderLightText = Zinc900
val ReaderLightSecondary = Zinc600

// Sepia Theme
val ReaderSepiaBackground = Color(0xFFF4ECD8)
val ReaderSepiaText = Color(0xFF5B4636)
val ReaderSepiaSecondary = Color(0xFF8B7355)
