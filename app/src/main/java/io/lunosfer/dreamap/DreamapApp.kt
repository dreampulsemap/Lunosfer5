package io.lunosfer.dreamap

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class DreamapApp : Application() {
    companion object {
        lateinit var instance: DreamapApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w("DreamapApp", "FirebaseApp init skipped: ${e.message}")
        }
    }
}
