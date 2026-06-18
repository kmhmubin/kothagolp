package com.kmhmubin.kothagolp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    NavItem("library", "Library", Icons.Filled.LibraryBooks, Icons.Outlined.LibraryBooks),
    NavItem("browse", "Browse", Icons.Filled.Explore, Icons.Outlined.Explore),
    NavItem("foryou", "For You", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    NavItem("history", "History", Icons.Filled.History, Icons.Outlined.History),
    NavItem("more", "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
)

@Composable
fun KothagolpBottomNavBar(
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedRoute = selectedRoute.removePrefix("tab_")
    NavigationBar(modifier = modifier) {
        bottomNavItems.forEach { item ->
            val selected = item.route == normalizedRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
fun KothagolpBottomNavBarWithInsets(
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    KothagolpBottomNavBar(
        selectedRoute = selectedRoute,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}

@Composable
fun KothagolpNavigationRail(
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedRoute = selectedRoute.removePrefix("tab_")
    NavigationRail(modifier = modifier) {
        bottomNavItems.forEach { item ->
            val selected = item.route == normalizedRoute || selectedRoute.contains(item.route)
            NavigationRailItem(
                selected = selected,
                onClick = { onItemSelected(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}
