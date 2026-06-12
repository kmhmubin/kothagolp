package com.kmhmubin.kothagolp.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

private const val TABLET_WIDTH_DP = 720

@Composable
fun isTabletUi(): Boolean = LocalConfiguration.current.screenWidthDp >= TABLET_WIDTH_DP

@Composable
fun isCompactUi(): Boolean = LocalConfiguration.current.screenWidthDp < 480
