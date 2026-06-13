package com.kmhmubin.kothagolp.ui.screens.details.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.kmhmubin.kothagolp.domain.model.ReadingStatus
import com.kmhmubin.kothagolp.ui.theme.StatusCompleted
import com.kmhmubin.kothagolp.ui.theme.StatusDROPPED
import com.kmhmubin.kothagolp.ui.theme.StatusOnHold
import com.kmhmubin.kothagolp.ui.theme.StatusPlanToRead
import com.kmhmubin.kothagolp.ui.theme.StatusReading
import com.kmhmubin.kothagolp.ui.theme.StatusSpicy

/**
 * Utility functions for ReadingStatus display
 */
object StatusUtils {

    fun getStatusColor(status: ReadingStatus): Color = when (status) {
        ReadingStatus.READING -> StatusReading
        ReadingStatus.SPICY -> StatusSpicy
        ReadingStatus.COMPLETED -> StatusCompleted
        ReadingStatus.ON_HOLD -> StatusOnHold
        ReadingStatus.PLAN_TO_READ -> StatusPlanToRead
        ReadingStatus.DROPPED -> StatusDROPPED
    }

    fun getStatusIcon(status: ReadingStatus): ImageVector = when (status) {
        ReadingStatus.READING -> Icons.AutoMirrored.Filled.MenuBook
        ReadingStatus.SPICY -> Icons.Default.LocalFireDepartment
        ReadingStatus.COMPLETED -> Icons.Default.CheckCircle
        ReadingStatus.ON_HOLD -> Icons.Default.Pause
        ReadingStatus.PLAN_TO_READ -> Icons.Default.Schedule
        ReadingStatus.DROPPED -> Icons.Default.Cancel
    }

    fun getStatusDescription(status: ReadingStatus): String = when (status) {
        ReadingStatus.READING -> "Currently reading this novel"
        ReadingStatus.SPICY -> "Saved to the hidden spicy shelf"
        ReadingStatus.COMPLETED -> "Finished reading all chapters"
        ReadingStatus.ON_HOLD -> "Paused reading temporarily"
        ReadingStatus.PLAN_TO_READ -> "Added to reading list"
        ReadingStatus.DROPPED -> "Stopped reading"
    }
}

/**
 * Extension function for ReadingStatus display name
 */
fun ReadingStatus.displayName(): String = when (this) {
    ReadingStatus.READING -> "Reading"
    ReadingStatus.SPICY -> "Spicy"
    ReadingStatus.COMPLETED -> "Completed"
    ReadingStatus.ON_HOLD -> "On Hold"
    ReadingStatus.PLAN_TO_READ -> "Plan to Read"
    ReadingStatus.DROPPED -> "Dropped"
}
