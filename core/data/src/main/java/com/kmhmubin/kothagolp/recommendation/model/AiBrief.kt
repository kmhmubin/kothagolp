package com.kmhmubin.kothagolp.recommendation.model

/** Input for the AI re-ranker: who the reader is, and the real novels it may choose from. */
data class AiBrief(
    /** Strongest library novels, "Title (Tag, Tag)" */
    val liked: List<String>,
    /** "Not interested" / dropped titles */
    val disliked: List<String>,
    val topTags: List<String>,
    val candidates: List<AiCandidate>
)

data class AiCandidate(
    /** Short stable id the model returns ("n1", "n2", …) — never a title it could misspell */
    val id: String,
    val recommendation: Recommendation,
    val tags: List<String>,
    val synopsis: String?
)
