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

class OpenRouterService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
        // Stable, always-available free model as default
        const val DEFAULT_MODEL = "meta-llama/llama-3.1-8b-instruct:free"
    }

    suspend fun getRecommendations(
        apiKey: String,
        readHistory: List<ReadHistoryItem>,
        likedGenres: List<String>,
        dislikedGenres: List<String>,
        model: String = DEFAULT_MODEL
    ): Result<List<AiRecommendedNovel>> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(readHistory, likedGenres, dislikedGenres)

            val requestJson = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.8)
                put("max_tokens", 2048)
            }.toString()

            val body = requestJson.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BASE_URL)
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://github.com/kmhmubin/kothagolp")
                .addHeader("X-Title", "Kothagolp")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val message = when (response.code) {
                        429 -> {
                            val detail = try {
                                JSONObject(errorBody).optJSONObject("error")?.optString("message") ?: ""
                            } catch (_: Exception) { "" }
                            when {
                                detail.contains("daily", ignoreCase = true) ||
                                detail.contains("day", ignoreCase = true) ->
                                    "Daily free limit reached for this model. Switch to a different model or try tomorrow."
                                detail.contains("RPM", ignoreCase = true) ||
                                detail.contains("per minute", ignoreCase = true) ||
                                detail.contains("minute", ignoreCase = true) ->
                                    "Too many requests per minute. Wait 60 seconds and retry."
                                else -> "Rate limit reached. Switch to a different model or wait a moment."
                            }
                        }
                        401 -> "Invalid API key. Check your OpenRouter key in Settings → For You."
                        403 -> "Access denied. Ensure your OpenRouter key has the right permissions."
                        400 -> "Bad request — the model may be unavailable. Try again."
                        503 -> "OpenRouter service temporarily unavailable. Try again shortly."
                        else -> "OpenRouter error ${response.code}: ${errorBody.take(120)}"
                    }
                    return@withContext Result.failure(Exception(message))
                }

                val responseText = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response from OpenRouter"))

                val novels = parseResponse(responseText)
                if (novels.isEmpty()) {
                    Result.failure(Exception("No recommendations returned. Try again."))
                } else {
                    Result.success(novels)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrendingRecommendations(
        apiKey: String,
        likedGenres: List<String>,
        model: String = DEFAULT_MODEL
    ): Result<List<AiRecommendedNovel>> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildTrendingPrompt(likedGenres)
            val requestJson = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 2048)
            }.toString()

            val body = requestJson.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BASE_URL)
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://github.com/kmhmubin/kothagolp")
                .addHeader("X-Title", "Kothagolp")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val message = when (response.code) {
                        429 -> {
                            val detail = try {
                                JSONObject(errorBody).optJSONObject("error")?.optString("message") ?: ""
                            } catch (_: Exception) { "" }
                            when {
                                detail.contains("daily", ignoreCase = true) || detail.contains("day", ignoreCase = true) ->
                                    "Daily free limit reached. Switch model or try tomorrow."
                                detail.contains("RPM", ignoreCase = true) || detail.contains("minute", ignoreCase = true) ->
                                    "Too many requests per minute. Wait 60s and retry."
                                else -> "Rate limit reached. Switch model or wait a moment."
                            }
                        }
                        401 -> "Invalid API key. Check your OpenRouter key in Settings → For You."
                        else -> "OpenRouter error ${response.code}"
                    }
                    return@withContext Result.failure(Exception(message))
                }
                val responseText = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response from OpenRouter"))
                val novels = parseResponse(responseText)
                if (novels.isEmpty()) Result.failure(Exception("No trending results. Try again."))
                else Result.success(novels)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildTrendingPrompt(likedGenres: List<String>): String {
        val genresText = if (likedGenres.isEmpty()) "any genre" else likedGenres.take(5).joinToString(", ")
        return """You are a web novel recommendation expert. Recommend 8 currently popular and trending web novels that readers worldwide love right now. Mix subgenres for variety.

User's preferred genres (match some): $genresText

Return ONLY a valid JSON array with exactly 8 objects, no other text. Each object must have:
- "title": string
- "author": string or null
- "reason": string (1-2 sentences why it's popular/trending)
- "genres": array of 2-4 genre strings

Example: [{"title":"Novel Title","author":"Author Name","reason":"Trending for its unique magic system.","genres":["Fantasy","Adventure"]}]"""
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

Return ONLY a valid JSON array with exactly 8 objects, no other text. Each object must have:
- "title": string
- "author": string or null
- "reason": string (1-2 sentences why it matches their taste)
- "genres": array of 2-4 genre strings

Example: [{"title":"Novel Title","author":"Author Name","reason":"Matches your love of action.","genres":["Action","Fantasy"]}]"""
    }

    private fun parseResponse(responseText: String): List<AiRecommendedNovel> {
        return try {
            val root = JSONObject(responseText)
            val choices = root.optJSONArray("choices") ?: return emptyList()
            val firstChoice = choices.optJSONObject(0) ?: return emptyList()
            val message = firstChoice.optJSONObject("message") ?: return emptyList()
            val text = message.optString("content").takeIf { it.isNotBlank() } ?: return emptyList()

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
