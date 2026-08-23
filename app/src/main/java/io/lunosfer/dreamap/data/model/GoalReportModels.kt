package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Bir vizyonu bildirme (rapor etme). SlidesViewer/VisionVideoPlayer/
 * VisionReels ekranlarındaki "..." menüsünden erişilir.
 * pages/api/goals/report.js ve 009_goal_saves_and_reports.sql migration'ı
 * ile BİREBİR eşleşir — reason değerleri ve "note" alan adı backend'deki
 * check constraint'e sadık kalınarak seçildi, uydurulmadı:
 *   reason text check (reason in ('spam','inappropriate','harassment',
 *     'misinformation','hate_speech','other'))
 * goal_reports tablosunda (goal_id, reporter_id) üzerinde unique constraint
 * var — aynı kişi aynı vizyonu ikinci kez bildirmeye çalışırsa backend
 * "already_reported" hatası döner, repository bunu kullanıcıya normal bir
 * hata mesajı olarak iletir.
 */
enum class GoalReportReason(val apiValue: String) {
    SPAM("spam"),
    INAPPROPRIATE("inappropriate"),
    HARASSMENT("harassment"),
    MISINFORMATION("misinformation"),
    HATE_SPEECH("hate_speech"),
    OTHER("other")
}

@Serializable
data class ReportGoalRequest(
    val goalId: String,
    val reason: String,
    val note: String? = null
)

@Serializable
data class ReportGoalResponse(
    val success: Boolean = false,
    // Backend aynı kişi aynı vizyonu ikinci kez bildirmeye çalıştığında
    // (unique constraint) hata değil, success:true + bu bayrakla nazikçe
    // "zaten bildirilmiş" bilgisini döner — UI bunu ayrı bir mesajla
    // gösterebilir, hata olarak ele almaz.
    @SerialName("already_reported") val alreadyReported: Boolean = false,
    val error: String? = null
)
