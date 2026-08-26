package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.storage.storage
import io.lunosfer.dreamap.data.model.CreateDiaryInput
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.data.model.DiaryListResponse
import io.lunosfer.dreamap.data.model.DiaryRing
import io.lunosfer.dreamap.data.model.DeleteDiaryInput
import io.lunosfer.dreamap.data.model.MarkDiarySeenInput
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

class DiaryRepository {
    private val api = NetworkModule.api

    private val imageDownloadClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun persistMediaToStorage(mediaUrl: String): Result<String> = runCatching {
        if (mediaUrl.isBlank()) return@runCatching mediaUrl
        if (mediaUrl.contains("supabase.co/storage/v1/object/public/")) {
            return@runCatching mediaUrl
        }
        val req = Request.Builder()
            .url(mediaUrl)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .build()
        val bytes = withContext(Dispatchers.IO) {
            val response = imageDownloadClient.newCall(req).execute()
            if (!response.isSuccessful) throw Exception("Media download HTTP ${response.code}")
            response.body?.bytes() ?: throw Exception("Empty media body")
        }
        val ext = if (mediaUrl.contains(".mp4", ignoreCase = true)) "mp4" else "jpg"
        val fileName = "diary_${System.currentTimeMillis()}.$ext"
        uploadMediaToStorage(bytes, fileName).getOrThrow()
    }

    suspend fun getFeed(): Result<List<DiaryRing>> = runCatching {
        api.getDiaryFeed().rings
    }

    suspend fun getEntriesForUser(userId: String): Result<DiaryListResponse> = runCatching {
        api.getDiaryListForUser(userId)
    }

    suspend fun markSeen(ownerId: String): Result<Unit> = runCatching {
        api.markDiarySeen(MarkDiarySeenInput(ownerId = ownerId))
    }

    suspend fun createEntry(input: CreateDiaryInput): Result<DiaryEntry> = runCatching {
        val res = api.createDiaryEntry(input)
        res.entry ?: throw Exception(res.error ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.diary_error_entry_create_failed))
    }

    suspend fun deleteEntry(entryId: String): Result<Unit> = runCatching {
        val res = api.deleteDiaryEntry(DeleteDiaryInput(entryId = entryId))
        if (!res.success) {
            throw Exception("Girdi silinemedi")
        }
    }

    suspend fun uploadMediaToStorage(byteArray: ByteArray, fileName: String): Result<String> = runCatching {
        val bucketName = "diary"
        val uniquePath = "${UUID.randomUUID()}_$fileName"
        try {
            val bucket = supabaseClient.storage.from(bucketName)
            bucket.upload(uniquePath, byteArray) {
                upsert = true
            }
            bucket.publicUrl(uniquePath)
        } catch (e: Exception) {
            // Try fallback bucket "public" or "dreams" if "diary" bucket doesn't exist
            try {
                val bucket = supabaseClient.storage.from("dreams")
                bucket.upload(uniquePath, byteArray) {
                    upsert = true
                }
                bucket.publicUrl(uniquePath)
            } catch (_: Exception) {
                throw e
            }
        }
    }
}
