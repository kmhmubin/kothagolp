package com.kmhmubin.kothagolp.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiModel(
    val id: String,
    val name: String,
    val isFree: Boolean,
    val inputPricePerMToken: Double,   // USD per 1M input tokens (0.0 if free)
    val outputPricePerMToken: Double,  // USD per 1M output tokens (0.0 if free)
    val contextLength: Int,
    val provider: String,              // e.g. "Google", "Meta", "Mistral"
    val isRecommended: Boolean = false,
    val recommendReason: String = ""   // short blurb shown in the picker
) {
    val displayPrice: String get() = when {
        isFree -> "FREE"
        inputPricePerMToken < 0.01 -> "<$0.01/1M"
        else -> "$${"%.2f".format(inputPricePerMToken)}/1M"
    }
}

object OpenRouterModelsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Session-level cache — fresh on every app start
    @Volatile private var cachedModels: List<AiModel>? = null

    // Models hand-picked for novel recommendation quality. OpenRouter retires free
    // models often, so the default is its free router, which always resolves to a live one.
    val RECOMMENDED_IDS = setOf(
        OpenRouterService.DEFAULT_MODEL,
        "google/gemma-4-31b-it:free",
        "nvidia/nemotron-3.5-super-vl:free",
        // paid but cheap
        "deepseek/deepseek-v4.1-flash",
        "openai/gpt-5-nano",
        "anthropic/claude-haiku-5.5",
    )

    private val RECOMMEND_REASONS = mapOf(
        OpenRouterService.DEFAULT_MODEL       to "Always works: routes to whichever free model is up.",
        "google/gemma-4-31b-it:free"          to "Strong free model, good at genre and story matching.",
        "nvidia/nemotron-3.5-super-vl:free"   to "Large free model with reliable JSON output.",
        "deepseek/deepseek-v4.1-flash"        to "Very cheap, fast, great at web novel knowledge.",
        "openai/gpt-5-nano"                   to "Cheap and precise at following instructions.",
        "anthropic/claude-haiku-5.5"          to "Best explanations of why a novel fits you.",
    )

    // Shown before the live list loads (or when it can't). Rate-limited but $0 cost.
    val FALLBACK_FREE_MODELS = listOf(
        AiModel(OpenRouterService.DEFAULT_MODEL,  "Free Models Router", true, 0.0, 0.0, 200000,  "OpenRouter", true, RECOMMEND_REASONS.getValue(OpenRouterService.DEFAULT_MODEL)),
        AiModel("google/gemma-4-31b-it:free",        "Gemma 4 31B",        true, 0.0, 0.0, 262144,  "Google",     true, RECOMMEND_REASONS.getValue("google/gemma-4-31b-it:free")),
        AiModel("nvidia/nemotron-3.5-super-vl:free", "Nemotron 3.5 Super", true, 0.0, 0.0, 1000000, "NVIDIA",     true, RECOMMEND_REASONS.getValue("nvidia/nemotron-3.5-super-vl:free")),
    )

    suspend fun getModels(apiKey: String?): Result<List<AiModel>> = withContext(Dispatchers.IO) {
        cachedModels?.let { return@withContext Result.success(it) }

        try {
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/models")
                .apply { if (!apiKey.isNullOrBlank()) addHeader("Authorization", "Bearer $apiKey") }
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.success(FALLBACK_FREE_MODELS)
                }

                val body = response.body?.string() ?: return@withContext Result.success(FALLBACK_FREE_MODELS)
                val root = JSONObject(body)
                val data = root.optJSONArray("data") ?: return@withContext Result.success(FALLBACK_FREE_MODELS)

                val models = mutableListOf<AiModel>()
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue

                    // Keep models that output text (includes multimodal like Gemini: "text+image->text")
                    val modality = obj.optJSONObject("architecture")?.optString("modality") ?: ""
                    if (modality.isNotBlank() && !modality.endsWith("->text")) continue

                    val pricing = obj.optJSONObject("pricing")
                    val promptPrice = pricing?.optString("prompt", "0")?.toDoubleOrNull() ?: 0.0
                    val completionPrice = pricing?.optString("completion", "0")?.toDoubleOrNull() ?: 0.0
                    // :free suffix = free tier with daily/monthly rate limits; price=0 = always free
                    val isFree = id.endsWith(":free") || (promptPrice == 0.0 && completionPrice == 0.0)

                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: id
                    val contextLength = obj.optInt("context_length", 4096)
                    val provider = id.substringBefore("/").replaceFirstChar { it.uppercaseChar() }

                    val isRec = id in RECOMMENDED_IDS
                    models.add(AiModel(
                        id = id,
                        name = name,
                        isFree = isFree,
                        inputPricePerMToken = promptPrice * 1_000_000,
                        outputPricePerMToken = completionPrice * 1_000_000,
                        contextLength = contextLength,
                        provider = provider,
                        isRecommended = isRec,
                        recommendReason = if (isRec) RECOMMEND_REASONS[id] ?: "" else ""
                    ))
                }

                // Sort: free first, then by name
                val sorted = models.sortedWith(compareByDescending<AiModel> { it.isFree }.thenBy { it.name })
                cachedModels = sorted
                Result.success(sorted)
            }
        } catch (_: Exception) {
            Result.success(FALLBACK_FREE_MODELS)
        }
    }

    fun invalidateCache() { cachedModels = null }
}
