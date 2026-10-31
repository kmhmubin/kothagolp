package com.kmhmubin.kothagolp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kmhmubin.kothagolp.domain.model.Chapter
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Cached novel details for offline access.
 */
@Entity(tableName = "novel_details")
data class NovelDetailsEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val author: String? = null,
    val posterUrl: String? = null,
    val synopsis: String? = null,
    val tags: List<String>? = null,
    val rating: Int? = null,
    val peopleVoted: Int? = null,
    val status: String? = null,
    val views: Int? = null,
    val relatedNovelsJson: String? = null,
    val chapters: List<ChapterEntity>? = null,
    val apiName: String = "",
    val chapterCount: Int = 0,
    val cachedAt: Long = System.currentTimeMillis(),

    // ============ NEW: Custom Cover ============
    val customCoverUrl: String? = null
) {
    fun toNovelDetails(): NovelDetails {
        val relatedNovels = relatedNovelsJson?.let { json ->
            try {
                parseRelatedNovels(json)
            } catch (e: Exception) {
                null
            }
        }

        val chapterList = chapters?.map { it.toChapter() } ?: emptyList()

        return NovelDetails(
            url = url,
            name = name,
            chapters = chapterList,
            author = author,
            posterUrl = customCoverUrl ?: posterUrl,  // Prioritize custom cover
            synopsis = synopsis,
            tags = tags,
            rating = rating,
            peopleVoted = peopleVoted,
            status = status,
            views = views,
            relatedNovels = relatedNovels
        )
    }

    companion object {
        fun fromNovelDetails(
            details: NovelDetails,
            apiName: String = ""  // ADD THIS PARAMETER
        ): NovelDetailsEntity {
            return NovelDetailsEntity(
                url = details.url,
                name = details.name,
                author = details.author,
                posterUrl = details.posterUrl,
                synopsis = details.synopsis,
                tags = details.tags,
                rating = details.rating,
                peopleVoted = details.peopleVoted,
                status = details.status,
                views = details.views,
                relatedNovelsJson = details.relatedNovels?.let { serializeRelatedNovels(it) },
                chapters = details.chapters.map { ChapterEntity.fromChapter(it) },
                apiName = apiName,
                chapterCount = details.chapters.size
            )
        }

        // JSON tree API rather than Gson: no reflection, so R8 can't strip the field names.
        internal fun parseRelatedNovels(json: String): List<Novel>? =
            Json.parseToJsonElement(json).jsonArray.mapNotNull { element ->
                val o = element as? JsonObject ?: return@mapNotNull null
                val url = o["url"]?.jsonPrimitive?.contentOrNull?.ifBlank { null } ?: return@mapNotNull null
                Novel(
                    name = o["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    url = url,
                    posterUrl = o["posterUrl"]?.jsonPrimitive?.contentOrNull,
                    rating = o["rating"]?.jsonPrimitive?.intOrNull,
                    apiName = o["apiName"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )
            }

        internal fun serializeRelatedNovels(novels: List<Novel>): String =
            buildJsonArray {
                novels.forEach { n ->
                    add(buildJsonObject {
                        put("name", n.name)
                        put("url", n.url)
                        n.posterUrl?.let { put("posterUrl", it) }
                        n.rating?.let { put("rating", it) }
                        put("apiName", n.apiName)
                    })
                }
            }.toString()
    }
}

/**
 * Embedded entity for chapters
 */
data class ChapterEntity(
    val name: String,
    val url: String,
    val dateOfRelease: String? = null
) {
    fun toChapter(): Chapter {
        return Chapter(
            name = name,
            url = url,
            dateOfRelease = dateOfRelease
        )
    }

    companion object {
        fun fromChapter(chapter: Chapter): ChapterEntity {
            return ChapterEntity(
                name = chapter.name,
                url = chapter.url,
                dateOfRelease = chapter.dateOfRelease
            )
        }
    }
}

/**
 * Embedded entity for related novels
 */
data class RelatedNovelEntity(
    val name: String,
    val url: String,
    val posterUrl: String? = null,
    val apiName: String
) {
    fun toNovel(): Novel {
        return Novel(
            name = name,
            url = url,
            posterUrl = posterUrl,
            apiName = apiName
        )
    }

    companion object {
        fun fromNovel(novel: Novel): RelatedNovelEntity {
            return RelatedNovelEntity(
                name = novel.name,
                url = novel.url,
                posterUrl = novel.posterUrl,
                apiName = novel.apiName
            )
        }
    }
}