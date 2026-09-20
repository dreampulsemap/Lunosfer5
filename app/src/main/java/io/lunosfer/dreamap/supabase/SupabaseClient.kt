package io.lunosfer.dreamap.supabase

import io.lunosfer.dreamap.BuildConfig
import io.lunosfer.dreamap.DreamapApp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.seconds

val supabaseClient = createSupabaseClient(
    supabaseUrl = if (BuildConfig.SUPABASE_URL.isNotBlank()) BuildConfig.SUPABASE_URL else "https://placeholder.supabase.co",
    supabaseKey = if (BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) BuildConfig.SUPABASE_ANON_KEY else "placeholder-anon-key"
) {
    // Varsayilan 10 sn, yavas mobil baglantilarda yetmiyordu: profil/mana
    // sorgulari ve gorsel yuklemeleri "timed out after 10000 ms" ile sessizce
    // basarisiz oluyordu (emulatorde canli goruldu).
    requestTimeout = 30.seconds

    install(Auth) {
        scheme = "io.lunosfer.dreamap"
        host = "auth-callback"
        // Varsayilan SettingsSessionManager duz metin SharedPreferences
        // kullanir (access_token + refresh_token sifrelenmemis halde
        // diskte durur). EncryptedSessionManager, ayni jetonlari
        // Android Keystore korumali AES256-GCM ile saklar.
        sessionManager = EncryptedSessionManager(DreamapApp.instance)
    }
    install(ComposeAuth)
    install(Postgrest)
    install(Storage)
}
