package com.kmhmubin.kothagolp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor

/**
 * Large title with its actions on the same row, centred on the title line (Apple-style
 * large title), subtitle underneath. LargeFlexibleTopAppBar puts actions in a separate
 * row above the title, which reads as misaligned on the main tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlignedLargeTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column(Modifier.padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (navigationIcon != null) {
                        Box(Modifier.padding(end = 12.dp)) { navigationIcon() }
                    }
                    Box(Modifier.weight(1f)) {
                        ProvideTextStyle(MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold)) {
                            title()
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
                if (subtitle != null) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) { subtitle() }
                    }
                }
            }
        },
        expandedHeight = 96.dp,
        windowInsets = WindowInsets(0.dp),
        scrollBehavior = scrollBehavior,
    )
}
