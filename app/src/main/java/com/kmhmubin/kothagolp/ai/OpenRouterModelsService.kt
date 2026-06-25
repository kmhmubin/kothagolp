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

    // Models hand-picked for novel recommendation quality
    val RECOMMENDED_IDS = setOf(
        "meta-llama/llama-3.3-70b-instruct:free",
        "google/gemini-2.0-flash-exp:free",
        "deepseek/deepseek-r1:free",
        "deepseek/deepseek-chat-v3-0324:free",
        "qwen/qwen-2.5-72b-instruct:free",
        // paid but affordable
        "google/gemini-flash-1.5",
        "anthropic/claude-3-haiku",
        "openai/gpt-4o-mini",
    )

    private val RECOMMEND_REASONS = mapOf(
        "meta-llama/llama-3.3-70b-instruct:free" to "Best free model for creative tasks. Fast + high quality.",
        "google/gemini-2.0-flash-exp:free"        to "Huge 1M context, great taste for stories. Free.",
        "deepseek/deepseek-r1:free"               to "Strong reasoning. Picks niche novels humans miss.",
        "deepseek/deepseek-chat-v3-0324:free"     to "Fast DeepSeek V3. Balanced quality and speed.",
        "qwen/qwen-2.5-72b-instruct:free"         to "Excellent at Asian light novels and web fiction.",
        "google/gemini-flash-1.5"                 to "Fast, cheap, very good at genre matching.",
        "anthropic/claude-3-haiku"                to "Snappy and precise. Great recommendation reasoning.",
        "openai/gpt-4o-mini"                      to "Reliable GPT-4 quality at mini price.",
    )

    // Curated free models shown before API loads. Rate-limited but $0 cost.
    val FALLBACK_FREE_MODELS = listOf(
        AiModel("google/gemini-2.0-flash-exp:free",        "Gemini 2.0 Flash Exp",         true, 0.0, 0.0, 1048576, "Google",     true, RECOMMEND_REASONS["google/gemini-2.0-flash-exp:free"]!!),
        AiModel("meta-llama/llama-3.3-70b-instruct:free",  "Llama 3.3 70B Instruct",       true, 0.0, 0.0, 131072,  "Meta",       true, RECOMMEND_REASONS["meta-llama/llama-3.3-70b-instruct:free"]!!),
        AiModel("deepseek/deepseek-r1:free",               "DeepSeek R1",                  true, 0.0, 0.0, 163840,  "DeepSeek",   true, RECOMMEND_REASONS["deepseek/deepseek-r1:free"]!!),
        AiModel("deepseek/deepseek-chat-v3-0324:free",     "DeepSeek V3 0324",             true, 0.0, 0.0, 131072,  "DeepSeek",   true, RECOMMEND_REASONS["deepseek/deepseek-chat-v3-0324:free"]!!),
        AiModel("qwen/qwen-2.5-72b-instruct:free",         "Qwen 2.5 72B Instruct",        true, 0.0, 0.0, 131072,  "Alibaba",    true, RECOMMEND_REASONS["qwen/qwen-2.5-72b-instruct:free"]!!),
        AiModel("google/gemini-flash-1.5-8b-exp:free",     "Gemini 1.5 Flash 8B Exp",      true, 0.0, 0.0, 1000000, "Google"),
        AiModel("google/gemma-2-9b-it:free",               "Gemma 2 9B",                   true, 0.0, 0.0, 8192,    "Google"),
        AiModel("meta-llama/llama-3.1-8b-instruct:free",   "Llama 3.1 8B Instruct",        true, 0.0, 0.0, 131072,  "Meta"),
        AiModel("mistralai/mistral-7b-instruct:free",      "Mistral 7B Instruct",          true, 0.0, 0.0, 32768,   "Mistral"),
        AiModel("microsoft/phi-3-mini-128k-instruct:free", "Phi-3 Mini 128K",              true, 0.0, 0.0, 128000,  "Microsoft"),
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
