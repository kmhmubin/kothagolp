package com.kmhmubin.kothagolp.util

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.domain.model.GridColumns

/**
 * Calculate the number of grid columns based on settings and screen width
 */
@Composable
fun calculateGridColumns(
    gridColumns: GridColumns,
    minColumnWidth: Int = 150
): Int {
    return when (gridColumns) {
        is GridColumns.Auto -> {
            val screenWidth = LocalConfiguration.current.screenWidthDp
            (screenWidth / minColumnWidth).coerceIn(2, 5)
        }
        is GridColumns.Fixed -> gridColumns.count
    }
}

/**
 * Returns GridCells for a LazyVerticalGrid: Adaptive for Auto mode, Fixed for user-chosen counts.
 */
@Composable
fun gridCellsFor(columns: GridColumns, minSize: Dp = 160.dp): GridCells {
    return when (columns) {
        is GridColumns.Auto -> GridCells.Adaptive(minSize = minSize)
        is GridColumns.Fixed -> GridCells.Fixed(columns.count)
    }
}