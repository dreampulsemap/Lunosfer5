package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.lunosfer.dreamap.data.model.FullUserProfile
import io.lunosfer.dreamap.data.model.PremiumStatusResponse
import io.lunosfer.dreamap.data.model.UpdateProfileRequest
import io.lunosfer.dreamap.data.model.UpdateProfileResponse
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import java.util.UUID
import java.util.concurrent.TimeUnit

class ProfileRepository {
    private val api = NetworkModule.api
    private val json = Json { ignoreUnknownKeys = true }

    private val imageDownloadClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun uploadAvatar(byteArray: ByteArray, fileName: String): Result<String> = runCatching {
        val uniquePath = "${UUID.randomUUID()}_$fileName"
        try {
            val bucket = supabaseClient.storage.from("avatars")
            bucket.upload(uniquePath, byteArray) { upsert = true }
            bucket.publicUrl(uniquePath)
        } catch (e: Exception) {
            try {
                val bucket = supabaseClient.storage.from("dreams")
                bucket.upload(uniquePath, byteArray) { upsert = true }
                bucket.publicUrl(uniquePath)
            } catch (_: Exception) {
                throw e
            }
        }
    }

    suspend fun persistAvatarToStorage(avatarUrl: String): Result<String> = runCatching {
        if (avatarUrl.isBlank()) return@runCatching avatarUrl
        if (avatarUrl.contains("supabase.co/storage/v1/object/public/")) {
            return@runCatching avatarUrl
        }
        val req = Request.Builder()
            .url(avatarUrl)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .build()
        val bytes = withContext(Dispatchers.IO) {
            val response = imageDownloadClient.newCall(req).execute()
            if (!response.isSuccessful) throw Exception("Avatar download HTTP ${response.code}")
            response.body?.bytes() ?: throw Exception("Empty image body")
        }
        val ext = if (avatarUrl.contains(".png", ignoreCase = true)) "png" else "jpg"
        val fileName = "avatar_${System.currentTimeMillis()}.$ext"
        uploadAvatar(bytes, fileName).getOrThrow()
    }

    suspend fun getUserProfile(userId: String): Result<FullUserProfile> = runCatching {
        try {
            val list = supabaseClient.postgrest["user_profiles"]
                .select { filter { eq("id", userId) } }
                .decodeList<FullUserProfile>()
            if (list.isNotEmpty()) {
                return@runCatching list.first()
            }
        } catch (_: Exception) {}

        // Fallback to public-profile API
        val res = api.getPublicProfile(userId = userId, page = 0)
        val p = res.profile
            ?: throw Exception(io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.profile_error_not_found))
        FullUserProfile(
            id = p.id,
            username = p.username,
            displayName = p.displayName,
            avatarUrl = p.avatarUrl,
            bio = p.bio
        )
    }

    suspend fun getUserDreams(userId: String): Result<List<io.lunosfer.dreamap.data.model.Dream>> = runCatching {
        val res = api.getPublicProfile(userId = userId, page = 0)
        res.dreams
    }

    suspend fun getUserVisions(): Result<List<io.lunosfer.dreamap.data.model.Goal>> = runCatching {
        val res = api.getGoalsFeed(mode = "own", page = 0, status = null)
        res.goals
    }

    suspend fun getSavedVisions(): Result<List<io.lunosfer.dreamap.data.model.Goal>> = runCatching {
        val res = api.getGoalsFeed(mode = "saved", page = 0, status = null)
        res.goals
    }

    suspend fun getUserDiary(userId: String): Result<List<io.lunosfer.dreamap.data.model.DiaryEntry>> = runCatching {
        val res = api.getDiaryListForUser(userId)
        res.entries
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Result<FullUserProfile> = runCatching {
        try {
            val res = api.updateProfile(request)
            if (!res.success) {
                throw Exception(res.error ?: res.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.profile_error_update_failed))
            }
            res.profile ?: FullUserProfile(id = request.userId)
        } catch (e: HttpException) {
            val parsedError = parseHttpError(e)
            throw Exception(parsedError)
        }
    }

    suspend fun getPremiumStatus(): Result<PremiumStatusResponse> = runCatching {
        api.getPremiumStatus()
    }

    suspend fun getProfileStats(): Result<io.lunosfer.dreamap.data.model.ProfileStatsResponse> = runCatching {
        api.getProfileStats()
    }

    // Hesabı ve ilişkili tüm verileri kalıcı olarak siler (bkz. LunosferApi.deleteAccount).
    // Geri alınamaz — çağıran taraf (ViewModel) kullanıcıdan önce onay almalı.
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val res = api.deleteAccount()
        if (!res.success && !res.ok) {
            throw Exception(res.error ?: "Hesap silinemedi")
        }
    }

    private fun parseHttpError(e: HttpException): String {
        val errorBody = e.response()?.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            try {
                val obj = json.parseToJsonElement(errorBody).jsonObject
                val err = obj["error"]?.jsonPrimitive?.content
                    ?: obj["message"]?.jsonPrimitive?.content
                if (!err.isNullOrBlank()) {
                    return err
                }
            } catch (_: Exception) {}
        }
        return when (e.code()) {
            409 -> io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.profile_error_username_taken)
            400 -> io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.profile_error_invalid_data)
            else -> io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.profile_error_update_generic).format(e.code())
        }
    }
}
