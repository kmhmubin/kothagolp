package com.kmhmubin.kothagolp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity for reading history.
 * Equivalent to HistoryEntry in React.
 */
@Entity(
    tableName = "history",
    // History is listed/observed ordered by timestamp DESC; without this,
    // every history-tab load is a full table scan.
    indices = [Index(value = ["timestamp"])]
)
data class HistoryEntity(
    @PrimaryKey
    val novelUrl: String,
    val novelName: String,
    val posterUrl: String? = null,
    val chapterName: String,
    val chapterUrl: String,
    val apiName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val customCoverUrl: String? = null
)