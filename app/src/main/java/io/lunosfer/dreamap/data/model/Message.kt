package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Sunucudan hiç gelmez — yalnızca gönderim sırasında yerel (optimistic) bir
 * balonu işaretlemek için var. Backend'den dönen her mesaj varsayılan
 * (SENT) ile deserialize olur. */
enum class MessageDeliveryStatus { SENDING, SENT, FAILED }

@Serializable
data class Message(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    val content: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("attachment_type") val attachmentType: String? = null,
    @SerialName("attachment_name") val attachmentName: String? = null,
    @SerialName("attachment_mime") val attachmentMime: String? = null,
    @SerialName("attachment_size") val attachmentSize: Long? = null,
    val reaction: String? = null,
    @SerialName("shared_ref") val sharedRef: SharedRef? = null,
    @Transient val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT
)

/**
 * DM'de paylaşılan rüya / günce / vizyonun sunucuda üretilmiş anlık görüntüsü
 * (bkz. web lib/shareSnapshot.js). Metni sunucu yazar; istemci yalnızca
 * [ShareRequestRef] gönderir.
 */
@Serializable
data class SharedRef(
    val type: String,
    val id: String,
    val title: String? = null,
    val excerpt: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("owner_name") val ownerName: String? = null,
    val visibility: String? = null
)

@Serializable
data class ShareRequestRef(
    val type: String,
    val id: String
)

/** pages/api/messages/conversations.js: her satır bir kişiyle olan son durumu özetler. */
@Serializable
data class Conversation(
    val otherUser: UserProfile,
    val lastMessage: Message,
    val unreadCount: Int
)

@Serializable
data class ConversationsResponse(
    val conversations: List<Conversation>
)

@Serializable
data class ThreadResponse(
    val messages: List<Message>,
    val otherUser: UserProfile,
    val hasMore: Boolean = false
)

@Serializable
data class UnreadCountResponse(
    val unreadCount: Int
)

@Serializable
data class SendMessageRequest(
    val recipientId: String,
    val content: String? = null,
    val lang: String? = null,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null,
    val attachmentName: String? = null,
    val attachmentMime: String? = null,
    val attachmentSize: Long? = null,
    val share: ShareRequestRef? = null
)

@Serializable
data class SendMessageResponse(
    val message: Message
)

@Serializable
data class ReactMessageRequest(
    val messageId: String,
    val reaction: String
)

@Serializable
data class PushSubscriptionRequest(
    val token: String
)
