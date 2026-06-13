package com.kmhmubin.kothagolp.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// KOTHAGOLP BRAND — DaisyUI Sunset
// ============================================
val SunsetBase100         = Color(0xFF121C22)  // background / surface
val SunsetBase200         = Color(0xFF0E171E)  // surfaceContainerLow
val SunsetBase300         = Color(0xFF091319)  // surfaceContainerLowest
val SunsetContent         = Color(0xFF9FB9D0)  // onBackground / onSurface
val SunsetPrimary         = Color(0xFFFF865B)  // coral-orange
val SunsetPrimaryContent  = Color(0xFF160603)
val SunsetSecondary       = Color(0xFFFD6F9C)  // pink
val SunsetSecondaryContent= Color(0xFF160409)
val SunsetAccent          = Color(0xFFB387FA)  // purple
val SunsetAccentContent   = Color(0xFF0C0615)
val SunsetNeutral         = Color(0xFF1B262C)  // surfaceContainer / variant
val SunsetNeutralContent  = Color(0xFF94A0A9)
val SunsetInfo            = Color(0xFF89E0EB)
val SunsetInfoContent     = Color(0xFF071213)
val SunsetSuccessColor    = Color(0xFFADDFAD)
val SunsetSuccessContent  = Color(0xFF0B120B)
val SunsetWarningColor    = Color(0xFFF1C892)
val SunsetWarningContent  = Color(0xFF140F08)
val SunsetErrorColor      = Color(0xFFFFBBBD)
val SunsetErrorContent    = Color(0xFF160D0D)

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

// New chapters indicator
val NewChapters = Color(0xFF10B981)      // Emerald-500
val NewChaptersLight = Color(0xFF34D399) // Emerald-400


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
