package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.BlockStatusResponse
import io.lunosfer.dreamap.data.model.BlockUserRequest
import io.lunosfer.dreamap.data.model.BlockedUserEntry
import io.lunosfer.dreamap.data.network.NetworkModule

/**
 * Google Play "User Generated Content" politikasının zorunlu kıldığı
 * kullanıcı engelleme akışı. Backend: pages/api/blocks/*.js
 * (dreamap-frontend) — user_blocks tablosu, RLS açık, canlıda doğrulandı.
 */
class BlockRepository {
    private val api = NetworkModule.api

    suspend fun blockUser(targetUserId: String): Result<Unit> = runCatching {
        val res = api.blockUser(BlockUserRequest(blockedUserId = targetUserId))
        if (!res.success && !res.ok) {
            throw Exception(res.error ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.block_user_error))
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> = runCatching {
        val res = api.unblockUser(BlockUserRequest(blockedUserId = targetUserId))
        if (!res.success && !res.ok) {
            throw Exception(res.error ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.unblock_user_error))
        }
    }

    suspend fun getBlockStatus(targetUserId: String): Result<BlockStatusResponse> = runCatching {
        api.getBlockStatus(targetUserId = targetUserId)
    }

    suspend fun getBlockedUsers(): Result<List<BlockedUserEntry>> = runCatching {
        api.getBlockedUsers().blocked
    }
}
