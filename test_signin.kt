import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.Google
fun test(auth: Auth) {
    val url = auth.getOAuthUrl(provider = Google, redirectUrl = "io.lunosfer.dreamap://auth-callback")
}
