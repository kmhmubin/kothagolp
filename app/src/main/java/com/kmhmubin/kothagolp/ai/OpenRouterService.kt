package com.kmhmubin.kothagolp.ai

import com.kmhmubin.kothagolp.recommendation.model.AiBrief
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.ForYouCopy
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenRouter calls for For You. The AI never invents the list on its own:
 * [rerank] chooses among real novels the engine already found, and
 * [suggestTitles] only proposes titles that the caller then verifies on the sources.
 */
private const val TAG = "OpenRouter"

class OpenRouterService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
        /** OpenRouter's free router: resolves to whichever free model is live. */
        const val DEFAULT_MODEL = "openrouter/free"

        internal fun errorMessage(code: Int, detail: String): String = when (code) {
            429 -> if (detail.contains("day", ignoreCase = true)) ForYouCopy.AI_DAILY_LIMIT else ForYouCopy.AI_BUSY
            401, 403 -> ForYouCopy.AI_BAD_KEY
            402, 503 -> ForYouCopy.AI_BUSY
            else -> ForYouCopy.AI_UNREADABLE
        }
    }

    data class Pick(val id: String, val reason: String)
    data class Suggestion(val title: String, val author: String?, val reason: String)

    /** Pick and explain the best [count] of the brief's candidates for this reader. */
    suspend fun rerank(apiKey: String, brief: AiBrief, model: String, count: Int = 8): Result<List<Pick>> {
        val candidates = brief.candidates.joinToString("\n") { c ->
            buildString {
                append("${c.id} | ${c.recommendation.novel.name}")
                if (c.tags.isNotEmpty()) append(" | ${c.tags.joinToString()}")
                c.recommendation.novel.rating?.let { append(" | %.1f★".format(it / 200f)) }
                c.synopsis?.let { append(" | $it") }
            }
        }
        val user = """${readerProfile(brief)}

Candidate novels (id | title | genres | rating | synopsis):
$candidates

Choose the $count candidates this reader is most likely to love and finish. Prefer clear fits with their favourites; include at most one bolder pick. For each, write one specific sentence (max 20 words) on why it fits THIS reader, naming a favourite or trait when you can. Use only ids from the list."""
        val schema = picksSchema(
            "picks",
            mapOf("id" to "Candidate id from the list", "reason" to "Why it fits this reader")
        )
        return chatJson(apiKey, model, user, schema).mapCatching { json ->
            val ids = brief.candidates.map { it.id }.toSet()
            json.optJSONArray("picks").objects()
                .mapNotNull { o ->
                    val id = o.optString("id").trim()
                    if (id !in ids) null else Pick(id, o.optString("reason").trim().ifBlank { ForYouCopy.PICKED_BY_AI })
                }
                .distinctBy { it.id }
                .ifEmpty { throw Exception(ForYouCopy.AI_UNREADABLE) }
        }
    }

    /** Ask for well-known novels outside the candidate pool; caller must verify each on the sources. */
    suspend fun suggestTitles(
        apiKey: String, brief: AiBrief, exclude: List<String>, model: String, count: Int = 10
    ): Result<List<Suggestion>> {
        val user = """${readerProfile(brief)}

Already known to the reader (do NOT suggest): ${exclude.take(60).joinToString("; ")}

Suggest $count real, well-known web novels or light novels (translated Chinese/Korean/Japanese, or English originals like Royal Road) that this reader would love. Use the exact English title readers search for. Only suggest novels you are sure exist. For each, one sentence (max 20 words) on why it fits this reader."""
        val schema = picksSchema(
            "suggestions",
            mapOf("title" to "Exact English title", "author" to "Author, or empty if unsure", "reason" to "Why it fits this reader")
        )
        return chatJson(apiKey, model, user, schema).mapCatching { json ->
            json.optJSONArray("suggestions").objects().mapNotNull { o ->
                val title = o.optString("title").trim().ifBlank { return@mapNotNull null }
                val author = o.optString("author").trim()
                    .takeUnless { it.isBlank() || it.lowercase() in setOf("author", "unknown", "n/a", "none") }
                Suggestion(title, author, o.optString("reason").trim())
            }.ifEmpty { throw Exception(ForYouCopy.AI_UNREADABLE) }
        }
    }

    private fun readerProfile(brief: AiBrief) = """Reader profile:
Favourite novels (strongest first): ${brief.liked.joinToString("; ").ifBlank { "none yet" }}
Top genres: ${brief.topTags.joinToString().ifBlank { "unknown" }}
Not interested in: ${brief.disliked.joinToString("; ").ifBlank { "nothing noted" }}"""

    /** {"<key>": [ {field: string, ...} ]} with every field required. */
    private fun picksSchema(key: String, fields: Map<String, String>): JSONObject {
        val props = JSONObject().apply {
            fields.forEach { (name, desc) -> put(name, JSONObject().put("type", "string").put("description", desc)) }
        }
        val item = JSONObject()
            .put("type", "object")
            .put("properties", props)
            .put("required", JSONArray(fields.keys.toList()))
            .put("additionalProperties", false)
        return JSONObject()
            .put("type", "object")
            .put("properties", JSONObject().put(key, JSONObject().put("type", "array").put("items", item)))
            .put("required", JSONArray(listOf(key)))
            .put("additionalProperties", false)
    }

    /**
     * One chat call that must come back as a JSON object.
     * Up to 3 attempts: falls back to the free router if the chosen model is gone, drops
     * the schema if the model rejects it (then parses leniently), and retries empty answers.
     */
    private suspend fun chatJson(
        apiKey: String, model: String, user: String, schema: JSONObject
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        var currentModel = model
        var useSchema = true
        repeat(3) {
            val body = JSONObject().apply {
                put("model", currentModel)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content",
                        "You are a web novel recommendation expert. Reply with JSON only, matching the requested shape."))
                    put(JSONObject().put("role", "user").put("content",
                        if (useSchema) user else "$user\n\nReply with ONLY this JSON shape: ${schema}"))
                })
                put("temperature", 0.4)
                // The free router often lands on reasoning models, which spend max_tokens on
                // thinking and return empty content: cap the thinking, leave room for the answer.
                put("max_tokens", 6000)
                put("reasoning", JSONObject().put("max_tokens", 1024).put("exclude", true))
                if (useSchema) {
                    put("response_format", JSONObject()
                        .put("type", "json_schema")
                        .put("json_schema", JSONObject().put("name", "result").put("strict", true).put("schema", schema)))
                    // Only route to endpoints that actually honour the schema.
                    put("provider", JSONObject().put("require_parameters", true))
                }
            }.toString()
            val request = Request.Builder()
                .url(BASE_URL)
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://github.com/kmhmubin/kothagolp")
                .addHeader("X-Title", "Kothagolp")
                .build()

            val started = System.currentTimeMillis()
            val (code, text) = try {
                client.newCall(request).execute().use { it.code to (it.body?.string() ?: "") }
            } catch (e: Exception) {
                Log.w(TAG, "$currentModel failed after ${System.currentTimeMillis() - started}ms", e)
                return@withContext Result.failure(Exception(ForYouCopy.AI_NETWORK))
            }
            Log.d(TAG, "$currentModel schema=$useSchema -> $code in ${System.currentTimeMillis() - started}ms" +
                    if (code !in 200..299) ": ${text.take(200)}" else "")
            if (code in 200..299) {
                parseContent(text)?.let { return@withContext Result.success(it) }
                runCatching {
                    val root = JSONObject(text)
                    val choice = root.optJSONArray("choices")?.optJSONObject(0)
                    Log.w(TAG, "Unreadable answer from ${root.optString("model")}, finish=${choice?.optString("finish_reason")}: " +
                            choice?.optJSONObject("message")?.toString()?.take(400))
                }
                return@repeat // empty/cut-off answer: try again (the free router picks another model)
            }
            val detail = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
            when {
                (code == 400 || code == 404) && currentModel != DEFAULT_MODEL &&
                    Regex("model|endpoint", RegexOption.IGNORE_CASE).containsMatchIn(detail) &&
                    !Regex("response_format|structured|json", RegexOption.IGNORE_CASE).containsMatchIn(detail) ->
                    currentModel = DEFAULT_MODEL
                code == 400 && useSchema -> useSchema = false
                else -> return@withContext Result.failure(Exception(errorMessage(code, detail)))
            }
        }
        Result.failure(Exception(ForYouCopy.AI_UNREADABLE))
    }

    private fun parseContent(responseText: String): JSONObject? = runCatching {
        val content = JSONObject(responseText).optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content").orEmpty()
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) null else JSONObject(content.substring(start, end + 1))
    }.getOrNull()

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
