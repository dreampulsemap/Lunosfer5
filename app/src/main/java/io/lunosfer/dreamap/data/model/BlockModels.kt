package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Google Play "User Generated Content" politikası (1:1 mesajlaşma/topluluk
// içeriği olan uygulamalar için ZORUNLU): kullanıcı engelleme. Backend
// tarafı pages/api/blocks/{block,unblock,status,list}.js — bkz. o
// dosyalardaki yorumlar. blockedUserId alan adı backend'in beklediği
// isimle (camelCase body) birebir aynı olmalı.

@Serializable
data class BlockUserRequest(
    val blockedUserId: String
)

@Serializable
data class BlockStatusResponse(
    val blockedByMe: Boolean = false,
    val blockedMe: Boolean = false
)

@Serializable
data class BlockedUserEntry(
    val userId: String,
    @SerialName("blockedAt") val blockedAt: String? = null,
    val profile: UserProfile? = null
)

@Serializable
data class BlockedUsersResponse(
    val blocked: List<BlockedUserEntry> = emptyList()
)
