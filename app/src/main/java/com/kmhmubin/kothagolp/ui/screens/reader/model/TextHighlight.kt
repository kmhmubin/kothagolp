package com.kmhmubin.kothagolp.ui.screens.reader.model

data class TextHighlight(
    val id: Long = 0,
    val segmentId: String,
    val startOffset: Int,
    val endOffset: Int,
    val color: String,
    val text: String,
    val userNote: String? = null
)

data class WordSelection(
    val word: String,
    val startOffset: Int,
    val endOffset: Int,
    val segmentId: String,
    val segmentIndex: Int,
    val existingHighlightId: Long? = null,
    val existingNote: String? = null
)
