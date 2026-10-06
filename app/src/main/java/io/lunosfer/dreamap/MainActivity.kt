package io.lunosfer.dreamap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import io.lunosfer.dreamap.service.LunosferMessagingService
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.screens.MainScreen
import io.lunosfer.dreamap.ui.theme.MyApplicationTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.parseSessionFromFragment
import io.github.jan.supabase.auth.status.SessionSource
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : AppCompatActivity() {
    private val pendingRouteState = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        handleAuthDeeplink(intent)
        handleShareDeeplink(intent)

        intent.getStringExtra("target_route")?.let { route ->
            if (route.isNotBlank()) {
                pendingRouteState.value = route
            }
        }

        requestNotificationPermission()
        initFcmToken()
        io.lunosfer.dreamap.util.GlobalContentPicker.register(this)
        io.lunosfer.dreamap.util.GlobalCameraCapture.register(this)

        setContent {
            MyApplicationTheme {
                MainScreen(
                    pendingRoute = pendingRouteState.value,
                    onRouteHandled = { pendingRouteState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeeplink(intent)
        handleShareDeeplink(intent)

        val route = intent.getStringExtra("target_route")
        if (!route.isNullOrBlank()) {
            pendingRouteState.value = route
        }
    }

    /**
     * Sadece gercek bir auth-callback deep link'i geldiginde Supabase'e
     * iletir. islendikten sonra intent'in data'sini temizler (setIntent ile
     * geri yazar): aksi halde surec dusup Android bu Activity'yi AYNI eski
     * intent ile yeniden olusturdugunda (kullanici uygulamayi tekrar
     * actiginda), suresi dolmus access/refresh token tekrar tekrar oturuma
     * yazilmaya calisiliyor, refresh basarisiz oluyor ve kullanici surekli
     * login ekranina dusuyordu ("uygulamadan atiyor").
     */
    private fun handleAuthDeeplink(intent: Intent) {
        val data = intent.data
        if (data != null && data.scheme == "io.lunosfer.dreamap" && data.host == "auth-callback") {
            val fragment = data.fragment
            intent.data = null
            setIntent(intent)
            // supabase handleDeeplinks() kullanicıyı kendi authScope'unda
            // dogruluyordu; orada atilan AuthRestException yakalanamiyor ve
            // uygulamayi cokertiyordu (Sentry, 1.5.4+27, Play on-lansman robotu).
            // Iptal edilen/hatali OAuth donusunde (#error=...) token yok.
            if (fragment.isNullOrBlank() || "access_token=" !in fragment) {
                if (fragment?.contains("error") == true || data.getQueryParameter("error") != null) {
                    Toast.makeText(this, R.string.common_error_unknown, Toast.LENGTH_LONG).show()
                }
                return
            }
            lifecycleScope.launch {
                try {
                    val auth = supabaseClient.auth
                    val session = auth.parseSessionFromFragment(fragment)
                    val user = auth.retrieveUser(session.accessToken)
                    auth.importSession(session.copy(user = user), source = SessionSource.External)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("MainActivity", "OAuth donusu islenemedi", e)
                    Toast.makeText(this@MainActivity, R.string.common_error_unknown, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Paylaşılan rüya / günce / vizyon bağlantısı; giriş yapılınca MainScreen o ekrana gider. */
    private fun handleShareDeeplink(intent: Intent) {
        val route = io.lunosfer.dreamap.util.shareRouteFromUri(intent.data) ?: return
        pendingRouteState.value = route
        intent.data = null
        setIntent(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun initFcmToken() {
        try {
            try {
                if (FirebaseApp.getApps(this).isEmpty()) {
                    FirebaseApp.initializeApp(applicationContext)
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "FirebaseApp initializeApp skipped: ${e.message}")
            }

            if (FirebaseApp.getApps(this).isNotEmpty()) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        if (!token.isNull_or_blank()) {
                            Log.d("MainActivity", "FCM Token: $token")
                            LunosferMessagingService.sendTokenToServer(token)
                        }
                    } else {
                        Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                    }
                }
            } else {
                Log.d("MainActivity", "FirebaseApp not initialized (google-services.json missing or incomplete)")
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "FCM token initialization error", e)
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}

