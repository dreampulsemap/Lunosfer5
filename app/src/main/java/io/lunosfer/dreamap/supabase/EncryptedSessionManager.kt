package io.lunosfer.dreamap.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [SessionManager] implementasyonu: Supabase access_token/refresh_token
 * ciftini duz metin SharedPreferences yerine EncryptedSharedPreferences
 * (AES256-GCM, Android Keystore korumali) icinde saklar.
 *
 * supabase-kt'nin varsayilan SettingsSessionManager'i duz SharedPreferences
 * kullanir; cihaza root/adb erisimi olan biri oturum jetonlarini dogrudan
 * okuyabilir. Bu sinif ayni SessionManager arayuzunu implemente ederek
 * SupabaseClient.kt icinde drop-in degisim saglar.
 */
class EncryptedSessionManager(context: Context) : SessionManager {

    private val json = Json { encodeDefaults = true }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Keystore bozulmus/uyumsuz olabilir (ör. cihaz yedekten geri
            // yuklendi). Sifreli depoya erisilemezse eski oturumu okumaya
            // calismak yerine temiz baslatip yeniden giris istemek daha
            // guvenli; kullanicinin verisi kaybolmaz, sadece tekrar login olur.
            Log.w(TAG, "Encrypted prefs unavailable, clearing and recreating", e)
            context.applicationContext.deleteSharedPreferences(PREFS_NAME)
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    override suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        prefs.edit().putString(SESSION_KEY, json.encodeToString(session)).apply()
    }

    override suspend fun loadSession(): UserSession? = withContext(Dispatchers.IO) {
        val raw = prefs.getString(SESSION_KEY, null) ?: return@withContext null
        try {
            json.decodeFromString(UserSession.serializer(), raw)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode stored session", e)
            null
        }
    }

    override suspend fun deleteSession() = withContext(Dispatchers.IO) {
        prefs.edit().remove(SESSION_KEY).apply()
    }

    companion object {
        private const val TAG = "EncryptedSessionMgr"
        private const val PREFS_NAME = "lunosfer_secure_session"
        private const val SESSION_KEY = "supabase_session"

        /**
         * Eski SettingsSessionManager, multiplatform-settings'in no-arg
         * Settings()'i uzerinden "<packageName>_preferences" adli duz metin
         * SharedPreferences dosyasina yaziyordu (access/refresh token dahil).
         * Sifreli depoya gecince bu dosyayi diskten siler.
         */
        fun wipeLegacyPlaintextSession(context: Context) {
            try {
                val appCtx = context.applicationContext
                val legacyName = "${appCtx.packageName}_preferences"
                // Once cerigi temizle (bazi OEM'lerde deleteSharedPreferences
                // dosyayi diskten hemen silmeyebilir), sonra kaydi kaldir.
                appCtx.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
                    .edit().clear().apply()
                appCtx.deleteSharedPreferences(legacyName)
            } catch (e: Exception) {
                Log.w(TAG, "Legacy session wipe skipped", e)
            }
        }
    }
}
