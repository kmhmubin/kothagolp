package com.kmhmubin.kothagolp.ai

import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.AiRecommendedNovel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRecommendationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    }

    suspend fun getRecommendations(
        apiKey: String,
        readHistory: List<ReadHistoryItem>,
        likedGenres: List<String>,
        dislikedGenres: List<String>
    ): Result<List<AiRecommendedNovel>> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(readHistory, likedGenres, dislikedGenres)
            val requestJson = buildRequestJson(prompt)
            val url = "$BASE_URL?key=$apiKey"

            val body = requestJson.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val message = when (response.code) {
                        429 -> "Rate limit reached. Free tier allows 15 calls/min. Wait a moment and try again."
                        401, 403 -> "Invalid API key. Check your Gemini key in Settings → For You."
                        400 -> "Bad request. Try refreshing."
                        503 -> "Gemini service temporarily unavailable. Try again in a few seconds."
                        else -> "Gemini error ${response.code}: ${errorBody.take(120)}"
                    }
                    return@withContext Result.failure(Exception(message))
                }

                val responseText = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))

                val novels = parseResponse(responseText)
                Result.success(novels)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        history: List<ReadHistoryItem>,
        likedGenres: List<String>,
        dislikedGenres: List<String>
    ): String {
        val historyText = if (history.isEmpty()) {
            "No reading history yet."
        } else {
            history.take(20).joinToString("\n") { "- ${it.title}" +
                if (it.genres.isNotEmpty()) " (${it.genres.take(3).joinToString(", ")})" else ""
            }
        }

        val likedText = if (likedGenres.isEmpty()) "not specified" else likedGenres.joinToString(", ")
        val dislikedText = if (dislikedGenres.isEmpty()) "none" else dislikedGenres.joinToString(", ")

        return """You are a web novel recommendation expert. Based on the user's reading history and preferences, recommend exactly 8 web novels or light novels they would enjoy.

User's reading history:
$historyText

Liked genres: $likedText
Disliked genres: $dislikedText

Return ONLY a valid JSON array with exactly 8 objects, no other text. Each object must have these fields:
- "title": string (the novel title)
- "author": string or null (author name if known)
- "reason": string (1-2 sentences explaining why this matches their taste)
- "genres": array of strings (2-4 genre tags)

Example format:
[{"title":"Novel Title","author":"Author Name","reason":"This matches your love of action and fantasy.","genres":["Action","Fantasy"]},...]"""
    }

    private fun buildRequestJson(prompt: String): String {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.8)
                put("maxOutputTokens", 2048)
            })
        }.toString()
    }

    private fun parseResponse(responseText: String): List<AiRecommendedNovel> {
        return try {
            val root = JSONObject(responseText)
            val candidates = root.optJSONArray("candidates") ?: return emptyList()
            val firstCandidate = candidates.optJSONObject(0) ?: return emptyList()
            val content = firstCandidate.optJSONObject("content") ?: return emptyList()
            val parts = content.optJSONArray("parts") ?: return emptyList()
            val text = parts.optJSONObject(0)?.optString("text") ?: return emptyList()

            // Extract JSON array from text (handle markdown code blocks if present)
            val jsonText = text.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val startIdx = jsonText.indexOf('[')
            val endIdx = jsonText.lastIndexOf(']')
            if (startIdx < 0 || endIdx < 0) return emptyList()

            val jsonArray = JSONArray(jsonText.substring(startIdx, endIdx + 1))
            (0 until jsonArray.length()).mapNotNull { i ->
                val obj = jsonArray.optJSONObject(i) ?: return@mapNotNull null
                val title = obj.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val genresArr = obj.optJSONArray("genres")
                val genres = if (genresArr != null) {
                    (0 until genresArr.length()).map { genresArr.optString(it) }.filter { it.isNotBlank() }
                } else emptyList()

                AiRecommendedNovel(
                    title = title,
                    author = obj.optString("author").takeIf { it.isNotBlank() },
                    reason = obj.optString("reason").takeIf { it.isNotBlank() } ?: "Recommended for you",
                    genres = genres
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    data class ReadHistoryItem(
        val title: String,
        val genres: List<String> = emptyList()
    )
}
