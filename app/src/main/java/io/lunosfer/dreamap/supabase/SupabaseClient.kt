package io.lunosfer.dreamap.supabase

import io.lunosfer.dreamap.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

val supabaseClient = createSupabaseClient(
    supabaseUrl = if (BuildConfig.SUPABASE_URL.isNotBlank()) BuildConfig.SUPABASE_URL else "https://placeholder.supabase.co",
    supabaseKey = if (BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) BuildConfig.SUPABASE_ANON_KEY else "placeholder-anon-key"
) {
    install(Auth) {
        scheme = "io.lunosfer.dreamap"
        host = "auth-callback"
    }
    install(ComposeAuth)
    install(Postgrest)
    install(Storage)
}
