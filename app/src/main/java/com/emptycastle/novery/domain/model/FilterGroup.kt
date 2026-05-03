package com.emptycastle.novery.domain.model

data class FilterGroup(
    val key: String,           // unique key for state tracking e.g. "status", "type"
    val label: String,         // display label e.g. "Status"
    val options: List<FilterOption>,
    val defaultValue: String? = null  // null = use first option's value
)