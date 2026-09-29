package io.lunosfer.dreamap.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import io.lunosfer.dreamap.data.model.SendMessageRequest
import io.lunosfer.dreamap.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.lunosfer.dreamap.R

/**
 * Bildirimdeki kaydirilan cevap kutusundan gelen metni yakalar. Uygulama acilmadan
 * calisir; auth token'i (AuthInterceptor uzerinden) global supabaseClient'tan okur,
 * Application zaten calisir durumda oldugu icin ek bir oturum baslatmaya gerek yoktur.
 */
class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val senderId = intent.getStringExtra(EXTRA_SENDER_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_TEXT_REPLY)
            ?.toString()
            ?.trim()

        if (replyText.isNullOrBlank()) return

        val appContext = context.applicationContext
        // Bildirimi HEMEN kapat: ag istegi (soguk surecte oturum yukleme +
        // token yenileme dahil) birkac saniye surebiliyor, kullanici bu surede
        // cevap kutusunu "gonderiliyor" diye bekliyordu. Hata olursa asagida
        // Toast ile bildiriliyor.
        if (notificationId != -1) {
            (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationId)
        }
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Arka planda oturum yuklu/gecerli olmayabilir (bkz. SessionGuard).
                io.lunosfer.dreamap.data.network.SessionGuard.freshAccessToken()
                NetworkModule.api.sendMessage(
                    SendMessageRequest(recipientId = senderId, content = replyText)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Cevap gonderilemedi", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, R.string.notif_reply_failed, Toast.LENGTH_SHORT).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReplyReceiver"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_SENDER_ID = "extra_sender_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
