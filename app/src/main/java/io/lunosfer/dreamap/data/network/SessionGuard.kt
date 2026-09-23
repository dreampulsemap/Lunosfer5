package io.lunosfer.dreamap.data.network

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Uygulama on planda degilken (bildirimden cevap/okundu, FCM token kaydi)
 * supabase-kt oturumu diskten YUKLEMIYOR ve otomatik yenilemiyor: bunlar
 * ProcessLifecycle ON_START olayina bagli. Sonuc: istek token'siz ya da
 * suresi dolmus token'la gidip 401 aliyordu (canli loglarda
 * POST /api/messages/send 401). Istekten once oturumu diskten yukler ve
 * suresi dolmak uzereyse yeniler.
 */
object SessionGuard {
    private const val TAG = "SessionGuard"
    private const val REFRESH_MARGIN_MS = 60_000L

    /** Gecerli bir access token dondurur; oturum yoksa null. */
    suspend fun freshAccessToken(): String? {
        val auth = supabaseClient.auth
        withTimeoutOrNull(5_000) { auth.sessionStatus.first { it !is SessionStatus.Initializing } }

        if (auth.currentSessionOrNull() == null) {
            runCatching { auth.loadFromStorage(autoRefresh = false) }
                .onFailure { Log.w(TAG, "Oturum diskten yuklenemedi", it) }
        }
        val session = auth.currentSessionOrNull() ?: return null

        if (session.expiresAt.toEpochMilliseconds() - System.currentTimeMillis() < REFRESH_MARGIN_MS) {
            runCatching { auth.refreshCurrentSession() }
                .onFailure { Log.w(TAG, "Oturum yenilenemedi", it) }
        }
        return auth.currentSessionOrNull()?.accessToken
    }
}
