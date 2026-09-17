package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * goal_slides tablosu satırı ("Vizyon Slaytları" — eski çoklu-slayt
 * gösterisi). Ürün artık tek "Vizyon Videosu" (vision_video_url,
 * VideoEditorScreen) yönüne geçti; bu model ve SlidesViewerScreen SADECE
 * henüz video'ya dönüştürülmemiş eski vizyonlar için okuma amaçlı fallback
 * olarak kullanılıyor — GoalDetailScreen'deki "Vizyonu İzle" butonu
 * goal.visionVideoUrl null ise buraya düşer. Yeni slayt OLUŞTURMA akışı
 * yok (web tarafında da kaldırıldı, bkz. components/GoalDetailModal.jsx).
 * pages/api/goals/slides/list.js ve components/SlidesViewer.jsx ile eşleşir.
 */
@Serializable
data class GoalSlide(
    val id: String,
    @SerialName("goal_id") val goalId: String,
    @SerialName("image_url") val imageUrl: String,
    val caption: String? = null,
    @SerialName("caption_font") val captionFont: String? = null, // "sans" | "serif" | "mono"
    @SerialName("caption_x") val captionX: Float? = null,        // yüzde, varsayılan 50
    @SerialName("caption_y") val captionY: Float? = null,        // yüzde, varsayılan 85
    @SerialName("caption_color") val captionColor: String? = null, // hex, varsayılan #ffffff
    @SerialName("caption_size") val captionSize: Float? = null,  // çarpan, varsayılan 1
    @SerialName("duration_seconds") val durationSeconds: Int? = 4,
    @SerialName("order_index") val orderIndex: Int? = null,
    @SerialName("saves_count") val savesCount: Int? = 0,
    @SerialName("has_saved") val hasSaved: Boolean? = false,
    @SerialName("created_at") val createdAt: String? = null
) {
    /** components/SlidesViewer.jsx'teki isVideoUrl(url) ile aynı mantık. */
    val isVideo: Boolean
        get() = imageUrl.contains("/pixabay-video/") || imageUrl.substringBefore("?").endsWith(".mp4")
}

@Serializable
data class GoalSlidesResponse(
    val slides: List<GoalSlide> = emptyList(),
    val owner: UserProfile? = null
)

@Serializable
data class DeleteSlideRequest(
    @SerialName("slideId") val slideId: String
)

@Serializable
data class SaveSlideRequest(
    @SerialName("slideId") val slideId: String
)

// --- Slayt OLUŞTURMA/DÜZENLEME (Android'e özgü — web'de artık UI'da yok,
// ama backend endpoint'leri ve goal_slides tablosu tam çalışır durumda,
// bkz. pages/api/goals/slides/{create,update,reorder}.js). ---

@Serializable
data class CreateSlideRequest(
    val goalId: String,
    val imageUrl: String,
    val caption: String? = null,
    val durationSeconds: Int? = null
)

@Serializable
data class CreateSlideResponse(
    val slide: GoalSlide? = null,
    val error: String? = null
)

@Serializable
data class UpdateSlideRequest(
    val slideId: String,
    val caption: String? = null,
    val durationSeconds: Int? = null,
    val captionFont: String? = null,
    val captionColor: String? = null,
    val captionX: Float? = null,
    val captionY: Float? = null,
    val captionSize: Float? = null
)

@Serializable
data class UpdateSlideResponse(
    val slide: GoalSlide? = null,
    val error: String? = null
)

@Serializable
data class ReorderSlidesRequest(
    val goalId: String,
    val orderedSlideIds: List<String>
)
