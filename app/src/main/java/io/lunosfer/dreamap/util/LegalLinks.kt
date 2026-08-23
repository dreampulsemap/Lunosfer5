package io.lunosfer.dreamap.util

import io.lunosfer.dreamap.BuildConfig

/**
 * Google Play Console "App content" formunun zorunlu kıldığı Gizlilik
 * Politikası URL'i buradan tek bir yerden yönetilir. BuildConfig.APP_URL
 * (.env üzerinden enjekte edilir) hangi ortama build alındığına göre
 * otomatik doğru domain'i verir.
 *
 * Gerçek sayfalar dreamap-frontend'de (Next.js Pages Router) hazır:
 * pages/privacy.js -> /privacy
 * pages/delete-account.js -> /delete-account (Play Console "Account
 * deletion" alanına da bu URL girilecek)
 */
object LegalLinks {
    private val baseUrl: String
        get() = BuildConfig.APP_URL.trimEnd('/')

    val privacyPolicyUrl: String
        get() = "$baseUrl/privacy"

    val deleteAccountUrl: String
        get() = "$baseUrl/delete-account"
}
