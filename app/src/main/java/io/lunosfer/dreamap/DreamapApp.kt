package io.lunosfer.dreamap

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import io.lunosfer.dreamap.supabase.EncryptedSessionManager

class DreamapApp : Application() {
    companion object {
        lateinit var instance: DreamapApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Eski surumlerde duz metin SharedPreferences'ta birakilmis olabilecek
        // Supabase oturum jetonunu (access_token/refresh_token) diskten sil.
        // Aktif oturumu bozmaz: supabaseClient ilk erisildiginde
        // EncryptedSessionManager uzerinden kendi (sifreli) oturumunu yukler;
        // eski dosya zaten farkli bir anahtar/depoda oldugu icin bagimsizdir.
        EncryptedSessionManager.wipeLegacyPlaintextSession(this)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w("DreamapApp", "FirebaseApp init skipped: ${e.message}")
        }
    }
}
