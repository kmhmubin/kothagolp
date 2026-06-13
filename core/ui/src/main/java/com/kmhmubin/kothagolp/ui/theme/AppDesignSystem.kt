package com.kmhmubin.kothagolp.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Fixed spacing scale — screen margins, section gaps, component insets.
 * For density-driven grid/card spacing use [KothagolpDimensions] via [KothagolpTheme.dimensions].
 */
@Immutable
object AppSpacing {
    val xs    = 4.dp   // icon-to-text gap, badge insets
    val sm    = 8.dp   // item gap, card inner padding
    val md    = 12.dp  // card content padding, search bar vertical
    val lg    = 16.dp  // screen horizontal margin, section padding
    val xl    = 20.dp  // chip horizontal, button content padding
    val xxl   = 24.dp  // section gap, large card padding
    val xxxl  = 32.dp  // hero section gap
    val xxxxl = 48.dp  // empty state icon gap, screen top padding
}

/**
 * Canonical M3-aligned shape scale.
 * Use these instead of ad-hoc RoundedCornerShape(Xdp) throughout the app.
 */
@Immutable
object AppShape {
    /** 4dp — dense badges, micro chips */
    val extraSmall = RoundedCornerShape(4.dp)
    /** 8dp — chips, tags, secondary buttons */
    val small      = RoundedCornerShape(8.dp)
    /** 12dp — input fields, minor cards, snack content */
    val medium     = RoundedCornerShape(12.dp)
    /** 16dp — primary cards, bottom sheet content area */
    val large      = RoundedCornerShape(16.dp)
    /** 28dp — FAB, large modal sheets, prominent panels */
    val extraLarge = RoundedCornerShape(28.dp)
    /** Full pill — segment controls, counters, avatars */
    val pill       = RoundedCornerShape(50)
    /** Circle — icon buttons with background, profile images */
    val circle     = CircleShape
}

/**
 * Tonal elevation levels — use with [Surface] tonalElevation and shadowElevation.
 */
@Immutable
object AppElevation {
    val none = 0.dp
    val xs   = 1.dp
    val sm   = 2.dp
    val md   = 4.dp
    val lg   = 8.dp
    val xl   = 12.dp
}
