package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.DeepAnalysis
import io.lunosfer.dreamap.data.model.DeepAnalysisRequest
import io.lunosfer.dreamap.data.model.DeepAnalysisResponse
import io.lunosfer.dreamap.data.network.LunosferApi
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.util.ApiErrors
import retrofit2.HttpException

/** Sunucunun anlamli hata kodlari; arayuz bunlara gore ayri metin gosterir. */
sealed class DeepAnalysisError(message: String) : Exception(message) {
    /** Bakiye yetmiyor — [cost] Aura gerekiyor, kullanicida [auras] var. */
    class InsufficientAuras(val cost: Int, val auras: Int) : DeepAnalysisError("insufficient_auras")
    /** Analiz icin en az [minimum] ruya gerekiyor. */
    class NotEnoughDreams(val minimum: Int) : DeepAnalysisError("not_enough_dreams")
    /** Saatlik deneme hakki doldu. */
    class RateLimited(val retryAfterMinutes: Int) : DeepAnalysisError("rate_limited")
    /** Uretim basarisiz oldu; [refunded] true ise Aura geri verildi. */
    class GenerationFailed(val refunded: Boolean) : DeepAnalysisError("generation_failed")
}

class DeepAnalysisRepository(
    private val api: LunosferApi = NetworkModule.api
) {

    suspend fun generate(lang: String, dreamIds: List<Long>? = null): Result<DeepAnalysis> =
        runCatching {
            val res = api.generateDeepAnalysis(DeepAnalysisRequest(dreamIds, lang))
            res.analysis ?: throw mapBody(res)
        }.recoverCatching { throw mapThrowable(it) }

    suspend fun history(): Result<List<DeepAnalysis>> = runCatching {
        api.listDeepAnalyses().analyses
    }

    // 402/429/400/502 govdesi Retrofit tarafindan HttpException'a sarildigi
    // icin hicbir yerde okunamiyordu; kullanici "Aura yetmedi" ile "saatlik
    // limit doldu" arasindaki farki goremiyordu. Govdeyi burada acip tipli
    // hataya ceviriyoruz.
    private fun mapThrowable(error: Throwable): Throwable {
        val http = error as? HttpException ?: return error
        val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
        val json = body?.let { runCatching { org.json.JSONObject(it) }.getOrNull() }
            ?: return ApiErrors.translate(error)

        return when (json.optString("error")) {
            "insufficient_auras" -> DeepAnalysisError.InsufficientAuras(
                json.optInt("cost", 10),
                json.optInt("auras", 0)
            )
            "not_enough_dreams" -> DeepAnalysisError.NotEnoughDreams(json.optInt("minimum", 3))
            "rate_limited" -> DeepAnalysisError.RateLimited(json.optInt("retryAfterMinutes", 60))
            "generation_failed", "claude_refusal", "claude_truncated",
            "invalid_json_from_model", "anthropic_key_missing" ->
                DeepAnalysisError.GenerationFailed(json.optBoolean("refunded", false))
            else -> ApiErrors.translate(error)
        }
    }

    private fun mapBody(res: DeepAnalysisResponse): Throwable = when (res.error) {
        "insufficient_auras" -> DeepAnalysisError.InsufficientAuras(res.cost ?: 10, res.auras ?: 0)
        "not_enough_dreams" -> DeepAnalysisError.NotEnoughDreams(res.minimum ?: 3)
        "rate_limited" -> DeepAnalysisError.RateLimited(res.retryAfterMinutes ?: 60)
        else -> DeepAnalysisError.GenerationFailed(res.refunded)
    }
}
