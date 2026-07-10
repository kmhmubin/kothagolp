package com.kmhmubin.kothagolp.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// KOTHAGOLP BRAND — Ink Blue Expressive
// Seed: #3D6FFF — high-chroma M3 expressive palette, WCAG AA
// ============================================

// ── Dark scheme roles ─────────────────────────
val InkPrimary                = Color(0xFF5B9BFF)   // vivid cobalt — ~6.9:1 on bg
val InkOnPrimary              = Color(0xFF001A6E)
val InkPrimaryContainer       = Color(0xFF1247C0)   // deep vivid blue
val InkOnPrimaryContainer     = Color(0xFFD6E3FF)

val InkSecondary              = Color(0xFF8CAFD8)   // richer blue-grey
val InkOnSecondary            = Color(0xFF0D2035)
val InkSecondaryContainer     = Color(0xFF1E3A56)
val InkOnSecondaryContainer   = Color(0xFFD0E4F7)

val InkTertiary               = Color(0xFFB99EFF)   // vivid violet
val InkOnTertiary             = Color(0xFF25006B)
val InkTertiaryContainer      = Color(0xFF4527A0)   // deep violet
val InkOnTertiaryContainer    = Color(0xFFE8DDFF)

val InkDarkBackground         = Color(0xFF0D1117)   // true ink — richer than grey
val InkDarkOnBackground       = Color(0xFFDEE3F0)
val InkDarkSurfaceVariant     = Color(0xFF2E3542)
val InkDarkOnSurfaceVariant   = Color(0xFFB0BAC8)
val InkDarkSurfaceLowest      = Color(0xFF080B10)
val InkDarkSurfaceLow         = Color(0xFF141820)
val InkDarkSurface            = Color(0xFF191D26)
val InkDarkSurfaceHigh        = Color(0xFF222733)
val InkDarkSurfaceHighest     = Color(0xFF2C3140)
val InkDarkOutline            = Color(0xFF7A8494)
val InkDarkOutlineVariant     = Color(0xFF2E3542)

// ── Light scheme roles ────────────────────────
val InkLightPrimary               = Color(0xFF0B47C5)   // deep expressive blue — 8.1:1 on bg
val InkLightOnPrimary             = Color(0xFFFFFFFF)
val InkLightPrimaryContainer      = Color(0xFFC8D9FF)   // vivid blue-tinted
val InkLightOnPrimaryContainer    = Color(0xFF001551)

val InkLightSecondary             = Color(0xFF3A5A80)   // rich slate-blue
val InkLightOnSecondary           = Color(0xFFFFFFFF)
val InkLightSecondaryContainer    = Color(0xFFBDD6F0)
val InkLightOnSecondaryContainer  = Color(0xFF071929)

val InkLightTertiary              = Color(0xFF5236A0)   // rich violet
val InkLightOnTertiary            = Color(0xFFFFFFFF)
val InkLightTertiaryContainer     = Color(0xFFDDD4FF)
val InkLightOnTertiaryContainer   = Color(0xFF170060)

val InkLightBackground            = Color(0xFFF5F7FF)   // blue-white
val InkLightOnBackground          = Color(0xFF0E1320)
val InkLightSurfaceVariant        = Color(0xFFD4DCE8)
val InkLightOnSurfaceVariant      = Color(0xFF3A4250)
val InkLightSurfaceLowest         = Color(0xFFFFFFFF)
val InkLightSurfaceLow            = Color(0xFFEDF1FB)
val InkLightSurface               = Color(0xFFE7EBF6)
val InkLightSurfaceHigh           = Color(0xFFDFE4F0)
val InkLightSurfaceHighest        = Color(0xFFD8DEEC)
val InkLightOutline               = Color(0xFF5D6778)
val InkLightOutlineVariant        = Color(0xFFB8C2D0)

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
val StatusDownloaded = Color(0xFF06B6D4) // Cyan-500 — matches Novery's downloaded shelf

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
