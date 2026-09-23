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
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                NetworkModule.api.sendMessage(
                    SendMessageRequest(recipientId = senderId, content = replyText)
                )
                if (notificationId != -1) {
                    appendSentMessageToNotification(appContext, notificationId, replyText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cevap gonderilemedi", e)
                // Bildirim yeniden post edilmezse cevap kutusu sonsuza dek "gonderiliyor"
                // spinner'inda kalir; ayni icerikle yeniden gosterip kullaniciyi uyariyoruz.
                if (notificationId != -1) repostNotification(appContext, notificationId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, R.string.notif_reply_failed, Toast.LENGTH_SHORT).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    // Cevap basariyla gonderildikten sonra bildirimi KAPATMIYORUZ — WhatsApp gibi,
    // gonderilen mesaji da ayni MessagingStyle gecmisine ekleyip acik birakiyoruz.
    private fun appendSentMessageToNotification(context: Context, notificationId: Int, replyText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val active = notificationManager.activeNotifications.firstOrNull { it.id == notificationId } ?: return
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(active.notification) ?: return

        style.addMessage(replyText, System.currentTimeMillis(), style.user)

        // setOnlyAlertOnce: kendi cevabimiz icin tekrar ses/titresim olmasin.
        val rebuilt = NotificationCompat.Builder(context, active.notification)
            .setStyle(style)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(notificationId, rebuilt)
    }

    private fun repostNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val active = notificationManager.activeNotifications.firstOrNull { it.id == notificationId } ?: return
        val rebuilt = NotificationCompat.Builder(context, active.notification)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(notificationId, rebuilt)
    }

    companion object {
        private const val TAG = "ReplyReceiver"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_SENDER_ID = "extra_sender_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
