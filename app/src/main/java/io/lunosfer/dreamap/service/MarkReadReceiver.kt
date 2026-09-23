package io.lunosfer.dreamap.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bildirimdeki "okundu isaretle" aksiyonu. Backend'de mesajlara ozel hafif bir
 * mark-read ucu yok; GET /api/messages/thread, o kisiden gelen okunmamis mesajlari
 * yan etki olarak okundu isaretliyor (bkz. pages/api/messages/thread.js) — ayni
 * davranisi tetiklemek icin onu cagiriyoruz.
 */
class MarkReadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val senderId = intent.getStringExtra(EXTRA_SENDER_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        // SEMANTIC_ACTION_MARK_AS_READ sistemi otomatik kapatmiyor; kullaniciya
        // aninda geri bildirim icin bildirimi hemen, agdan bagimsiz kapatiyoruz.
        if (notificationId != -1) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationId)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Arka planda oturum yuklu/gecerli olmayabilir (bkz. SessionGuard).
                io.lunosfer.dreamap.data.network.SessionGuard.freshAccessToken()
                io.lunosfer.dreamap.data.network.NetworkModule.api.getThread(
                    otherUserId = senderId,
                    before = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Okundu isaretleme basarisiz", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "MarkReadReceiver"
        const val EXTRA_SENDER_ID = "extra_sender_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
