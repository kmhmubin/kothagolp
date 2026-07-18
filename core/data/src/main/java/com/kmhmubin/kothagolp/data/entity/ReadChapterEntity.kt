package com.kmhmubin.kothagolp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks all chapters that have been read.
 * Used for "Unread" download feature.
 */
@Entity(
    tableName = "read_chapters",
    indices = [Index(value = ["novelUrl"])]
)
data class ReadChapterEntity(
    @PrimaryKey
    val chapterUrl: String,
    val novelUrl: String,
    val readAt: Long = System.currentTimeMillis(),

    /**
     * Unread tombstone. Non-null = the chapter was marked unread at this time.
     *
     * "Unread" used to delete the row, so it was represented as absence — and a
     * sync merge (a union of both devices' read rows) simply restored it from
     * the other device's surviving copy. The row now stays with unreadAt set so
     * the unread action can win a newest-action comparison, then is filtered out
     * of the read set. read again clears it back to null.
     */
    val unreadAt: Long? = null
)