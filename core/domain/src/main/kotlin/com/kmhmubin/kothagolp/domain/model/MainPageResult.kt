package com.kmhmubin.kothagolp.domain.model

/**
 * Result from loading main catalog page.
 */
data class MainPageResult(
    val url: String,
    val novels: List<Novel>,
    val hasNextPage: Boolean = true
)