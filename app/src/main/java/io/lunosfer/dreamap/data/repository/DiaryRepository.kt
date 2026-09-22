package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.CreateDiaryCommentRequest
import io.lunosfer.dreamap.data.model.CreateDiaryInput
import io.lunosfer.dreamap.data.model.DeleteDiaryCommentRequest
import io.lunosfer.dreamap.data.model.DiaryComment
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.data.model.DiaryLikeRequest
import io.lunosfer.dreamap.data.model.DiaryLikeResponse
import io.lunosfer.dreamap.data.model.DiaryListResponse
import io.lunosfer.dreamap.data.model.DiaryRing
import io.lunosfer.dreamap.data.model.DeleteDiaryInput
import io.lunosfer.dreamap.data.model.MarkDiarySeenInput
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.days
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
        // Bucket private oldugundan yeni yuklemeler /object/sign/ yolunu
        // uretiyor; eski kayitlar hala /object/public/. Ikisi de "zaten
        // storage'da" demek — tekrar indirip yuklemeye calismak gereksiz
        // (ve imzasi dolmus bir URL'de basarisiz) olurdu.
        if (mediaUrl.contains("supabase.co/storage/v1/object/public/") ||
            mediaUrl.contains("supabase.co/storage/v1/object/sign/")
        ) {
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
            // Mesaj dogrudan kullaniciya gosteriliyor; sabit Turkce metin
            // uygulama Ingilizce'yken de Turkce cikiyordu.
            throw Exception(DreamapApp.instance.getString(R.string.error_delete_failed))
        }
    }

    suspend fun setLiked(diaryEntryId: String, liked: Boolean): Result<DiaryLikeResponse> = runCatching {
        val request = DiaryLikeRequest(diaryEntryId = diaryEntryId)
        if (liked) api.likeDiaryEntry(request) else api.unlikeDiaryEntry(request)
    }

    suspend fun getComments(diaryEntryId: String): Result<List<DiaryComment>> = runCatching {
        api.getDiaryComments(diaryEntryId).comments
    }

    suspend fun addComment(diaryEntryId: String, content: String): Result<DiaryComment> = runCatching {
        val res = api.createDiaryComment(CreateDiaryCommentRequest(diaryEntryId = diaryEntryId, content = content))
        res.comment ?: throw Exception(res.error ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.common_error_action_failed))
    }

    suspend fun deleteComment(commentId: String): Result<Unit> = runCatching {
        val res = api.deleteDiaryComment(DeleteDiaryCommentRequest(commentId = commentId))
        if (!res.success) throw Exception(io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.common_error_action_failed))
    }

    suspend fun uploadMediaToStorage(byteArray: ByteArray, fileName: String): Result<String> = runCatching {
        // Bucket'ın gerçek adı "diary-media" — "diary" bucket'ı private ve
        // buna karşılık gelen bir storage policy'si yok, bu yüzden yükleme
        // sessizce reddediliyordu. Ayrıca "diary-media" içindeki INSERT
        // policy'si yolun ilk klasörünün auth.uid() olmasını şart koşuyor.
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException(io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.messages_error_login_required_upload))
        val bucketName = "diary-media"
        val uniquePath = "$userId/${UUID.randomUUID()}_$fileName"
        val mimeType = if (fileName.endsWith(".mp4", ignoreCase = true))
            io.ktor.http.ContentType.Video.MP4 else io.ktor.http.ContentType.Image.JPEG
        val bucket = supabaseClient.storage.from(bucketName)
        bucket.upload(uniquePath, byteArray) {
            upsert = true
            contentType = mimeType
        }
        // Bucket private: publicUrl() artik erisilemeyen bir adres uretir ve
        // composer'daki onizleme bos kalirdi. Imzali URL donuyoruz. Bu deger
        // DB'ye de yazilyor ama sorun degil — sunucu okuma aninda yoldan
        // yeni bir imza uretiyor (bkz. lib/diaryMediaUrl.js).
        bucket.createSignedUrl(uniquePath, 1.days)
    }
}
