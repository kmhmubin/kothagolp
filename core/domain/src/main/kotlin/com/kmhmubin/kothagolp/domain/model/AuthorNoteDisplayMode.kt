package com.kmhmubin.kothagolp.domain.model

enum class AuthorNoteDisplayMode(val id: String) {
    EXPANDED("expanded"),
    COLLAPSED("collapsed"),
    HIDDEN("hidden");

    companion object {
        fun fromId(id: String): AuthorNoteDisplayMode =
            entries.find { it.id == id } ?: COLLAPSED
    }
}
