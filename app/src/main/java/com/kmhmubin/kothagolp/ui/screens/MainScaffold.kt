package com.kmhmubin.kothagolp.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.ui.components.KothagolpBottomNavBarWithInsets

/**
 * Main scaffold with bottom navigation only (no top bar)
 */
@Composable
fun MainScaffold(
    currentTab: String,
    onTabChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    appSettings: AppSettings,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        // No topBar - removed to maximize screen space
        bottomBar = {
            KothagolpBottomNavBarWithInsets(
                selectedRoute = currentTab,
                onItemSelected = { route ->
                    if (route == "settings") {
                        onSettingsClick()
                    } else {
                        onTabChange(route)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        content(paddingValues)
    }
}