package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Mental Wall ---
@Serializable
data class MentalWallRequest(
    val lang: String = "tr"
)

@Serializable
data class MentalWallResponse(
    val ok: Boolean? = true,
    val image: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val url: String? = null,
    val narrative: String? = null,
    val summary: String? = null,
    val archetypes: List<String>? = emptyList(),
    val error: String? = null
) {
    val displayImage: String? get() = image ?: imageUrl ?: url
    val displayText: String? get() = narrative ?: summary
}

// --- Psyche Map ---
@Serializable
data class PsycheMapResponse(
    val ok: Boolean? = true,
    @SerialName("dominant_archetype") val dominantArchetype: String? = null,
    @SerialName("psychic_score") val psychicScore: Int? = null,
    val summary: String? = null,
    val archetypes: List<PsycheArchetypeItem>? = emptyList(),
    val nodes: List<PsycheNode>? = emptyList(),
    val error: String? = null
)

@Serializable
data class PsycheArchetypeItem(
    // Backend (pages/api/psyche-map.js) "name"/"percentage"/"description" değil,
    // "label"/"share" (0-1 arası oran) alanlarını döndürüyor — bu alan adları
    // eskiden hiç ulaşılamayan bu ekranla birlikte hiç test edilmemişti.
    val label: String,
    val count: Int? = 0,
    val share: Double? = 0.0
) {
    val name: String get() = label
    val percentage: Int get() = ((share ?: 0.0) * 100).toInt()
    val description: String? get() = null
}

@Serializable
data class PsycheNode(
    val id: String,
    val label: String,
    val type: String? = null,
    val weight: Float? = 1f
)

// --- Prophet ---
/** [mode]: "general" = ruya/vizyonlardan kehanet, "ask" = yazilan soruya cevap. */
@Serializable
data class ProphetRequest(
    val mode: String = MODE_GENERAL,
    val question: String? = null,
    val lang: String = "tr"
) {
    companion object {
        const val MODE_GENERAL = "general"
        const val MODE_ASK = "ask"
    }
}

@Serializable
data class ProphetResponse(
    val ok: Boolean? = true,
    val success: Boolean? = true,
    val prophecy: String? = null,
    val answer: String? = null,
    val card: String? = null,
    val guidance: String? = null,
    val mode: String? = null,
    val isPremium: Boolean = false,
    /** Premium Claude uretimi mi (uzun/gerekceli), yoksa kisa ucretsiz metin mi. */
    val detailed: Boolean = false,
    /** Ucretsiz gunluk hak bitti — arayuz premium teklifini gosterir. */
    val limitReached: Boolean = false,
    /** Kullanicinin hic ruyasi/vizyonu yok — once icerik eklemesi gerek. */
    val needsContent: Boolean = false,
    val remaining: Int? = null,
    val dailyLimit: Int? = null,
    val error: String? = null
) {
    val resultText: String? get() = prophecy ?: answer ?: guidance
}
