package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.storage.storage
import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

class DreamRepository {
    private val api = NetworkModule.api

    private val imageDownloadClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun uploadDreamImage(byteArray: ByteArray, fileName: String): Result<String> = runCatching {
        val uniquePath = "${UUID.randomUUID()}_$fileName"
        val bucketName = "dreams"
        val bucket = supabaseClient.storage.from(bucketName)
        bucket.upload(uniquePath, byteArray) {
            upsert = true
        }
        bucket.publicUrl(uniquePath)
    }

    suspend fun persistImageToStorage(imageUrl: String, suggestedName: String = "dream_image"): Result<String> = runCatching {
        if (imageUrl.isBlank()) return@runCatching imageUrl
        if (imageUrl.contains("supabase.co/storage/v1/object/public/")) {
            return@runCatching imageUrl
        }
        val req = Request.Builder()
            .url(imageUrl)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .build()
        val bytes = withContext(Dispatchers.IO) {
            val response = imageDownloadClient.newCall(req).execute()
            if (!response.isSuccessful) throw Exception("Image download HTTP ${response.code}")
            response.body?.bytes() ?: throw Exception("Empty image body")
        }
        val ext = if (imageUrl.contains(".png", ignoreCase = true)) "png" else "jpg"
        val fileName = "${suggestedName}_${System.currentTimeMillis()}.$ext"
        uploadDreamImage(bytes, fileName).getOrThrow()
    }

    suspend fun getDream(id: Long): Result<DreamDetail> = runCatching {
        api.getDream(id).dream
    }

    suspend fun analyzeDream(dreamId: Long, content: String, lang: String): Result<Unit> = runCatching {
        api.analyzeDream(AnalyzeDreamRequest(dreamId, content, lang))
    }

    suspend fun likeDream(dreamId: Long, userId: String): Result<LikeResponse> = runCatching {
        try {
            api.likeDream(LikeRequest(dreamId, userId))
        } catch (e: Exception) {
            if (e.message?.contains("Already liked", ignoreCase = true) == true) {
                LikeResponse(success = true, liked = true)
            } else {
                throw e
            }
        }
    }

    suspend fun unlikeDream(dreamId: Long, userId: String): Result<LikeResponse> = runCatching {
        api.unlikeDream(LikeRequest(dreamId, userId))
    }

    suspend fun getComments(dreamId: Long): Result<List<DreamComment>> = runCatching {
        api.getComments(dreamId).comments
    }

    suspend fun createComment(dreamId: Long, userId: String, content: String): Result<DreamComment?> = runCatching {
        val res = api.createComment(CreateCommentRequest(dreamId, userId, content))
        if (!res.success && res.error != null) {
            throw Exception(res.error)
        }
        res.comment
    }

    suspend fun deleteComment(commentId: Long, userId: String): Result<Unit> = runCatching {
        val res = api.deleteComment(DeleteCommentRequest(commentId, userId))
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    suspend fun updateDream(request: UpdateDreamRequest): Result<Unit> = runCatching {
        val res = api.updateDream(request)
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    suspend fun deleteDream(dreamId: Long, userId: String, softDelete: Boolean = false): Result<Unit> = runCatching {
        val res = api.deleteDream(DeleteDreamRequest(dreamId, userId, softDelete))
        if (!res.success && !res.ok && res.error != null) {
            throw Exception(res.error)
        }
        Unit
    }

    suspend fun boostDream(dreamId: Long): Result<BoostDreamResponse> = runCatching {
        val res = api.boostDream(BoostDreamRequest(dreamId))
        if (!res.ok && res.error != null) {
            throw Exception(res.error)
        }
        res
    }

    suspend fun addBounty(dreamId: Long, bountyAmount: Int): Result<AddBountyResponse> = runCatching {
        val res = api.addBounty(AddBountyRequest(dreamId, bountyAmount))
        if (!res.ok && res.error != null) {
            throw Exception(res.error)
        }
        res
    }

    suspend fun generateDeepAnalysis(dreamId: Long): Result<GenerateDeepAnalysisResponse> = runCatching {
        val res = api.generateDeepAnalysis(GenerateDeepAnalysisRequest(dreamId = dreamId.toString()))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res
    }

    // Google Play UGC politikası: rüya şikayeti. bkz. pages/api/reports/dream.js
    suspend fun reportDream(dreamId: Long, reason: String, note: String?): Result<ContentReportResponse> = runCatching {
        val res = api.reportDream(ReportDreamRequest(dreamId = dreamId, reason = reason, note = note))
        if (!res.success && res.error != null) {
            throw Exception(res.error)
        }
        res
    }
}
