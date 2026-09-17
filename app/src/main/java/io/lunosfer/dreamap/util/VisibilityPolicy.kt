package io.lunosfer.dreamap.util

/**
 * Profil gizliliği ile paylaşım (Rüya/Vizyon/Günlük) gizliliği arasındaki
 * ilişkiyi tanımlayan tek kaynak. Tüm oluşturma/düzenleme ekranları bu
 * politikayı kullanmalı ki kural her yerde tutarlı uygulansın:
 *
 * - Profil "private" (tamamen gizli)  -> paylaşım SADECE "private" olabilir.
 * - Profil "friends" (sadece arkadaşlar) -> paylaşım "friends" veya "private" olabilir
 *   ("public" seçeneği sunulmaz).
 * - Profil "public" (herkese açık) veya bilinmiyorsa -> paylaşım "public",
 *   "friends" veya "private" olabilir (kısıtlama yok).
 *
 * Not: Aynı kural backend'de de uygulanıyor — 013_profile_visibility_and_post_clamp.sql
 * migration'ındaki DB trigger'ı (dreams/goals/diary_entries) ve ilgili API
 * route'ları (goals/create, goals/update-visibility, diary/create,
 * update-dream, submit-dream) nihai güvence. Buradaki istemci tarafı kısıtlama
 * sadece kullanıcıya doğru seçenekleri baştan göstermek içindir.
 */
object VisibilityPolicy {
    const val PUBLIC = "public"
    const val FRIENDS = "friends"
    const val PRIVATE = "private"

    /** Verilen profil gizliliğine göre seçilebilir paylaşım gizliliklerini, en açıktan en kapalıya sırayla döner. */
    fun allowedOptions(profileVisibility: String?): List<String> = when (profileVisibility) {
        PRIVATE -> listOf(PRIVATE)
        FRIENDS -> listOf(FRIENDS, PRIVATE)
        else -> listOf(PUBLIC, FRIENDS, PRIVATE)
    }

    /** Profile göre izin verilen en açık (varsayılan) paylaşım gizliliği. */
    fun defaultFor(profileVisibility: String?): String = allowedOptions(profileVisibility).first()

    /** İstenen bir gizlilik değerini profil kısıtına göre güvenli bir değere sıkıştırır (savunma amaçlı). */
    fun clamp(requested: String, profileVisibility: String?): String {
        val allowed = allowedOptions(profileVisibility)
        return if (requested in allowed) requested else allowed.first()
    }
}
