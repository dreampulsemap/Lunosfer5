package io.lunosfer.dreamap.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.lunosfer.dreamap.MainActivity
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.PushSubscriptionRequest
import io.lunosfer.dreamap.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LunosferMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        sendTokenToServer(token)
    }

    // Backend artik SADECE data-payload gonderiyor (bkz. dreamap-frontend/lib/webPush.js).
    // Neden: notification+data birlikte gonderilirse, uygulama arka plandayken/kapaliyken
    // sistem bildirimi KENDI gosterir ve onMessageReceived HIC cagrilmaz — bu servisin
    // ozel MessagingStyle/inline-reply/mark-as-read mantigi calismaz. Data-only mesajlar
    // ise uygulama durumundan bagimsiz her zaman burayi tetikler.
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title ?: data["title"] ?: "Lunosfer"
        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: ""

        val senderId = data["senderId"]
        if (data["type"] == "message" && !senderId.isNullOrBlank()) {
            if (senderId == activeThreadUserId) return
            val senderName = data["senderName"]?.takeIf { it.isNotBlank() } ?: title
            showMessageNotification(senderId, senderName, body)
        } else {
            showNotification(title, body, data)
        }
    }

    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        val targetRoute = parseTargetRoute(data)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!targetRoute.isNullOrBlank()) {
                putExtra("target_route", targetRoute)
            }
        }

        // Ayni "tag" (ornegin ayni goal'e tekrar yorum) tekrar geldiginde eski
        // bildirimi UST USTE yeni bir tane olarak degil, guncelleyerek gostersin.
        // Bu alan backend'den (webPush.js) her zaman gonderiliyordu ama daha once
        // hic okunmuyordu; her push rastgele yeni bir ID aliyordu.
        val notificationId = data["tag"]?.takeIf { it.isNotBlank() }?.hashCode()
            ?: System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(notificationManager, CHANNEL_ID, R.string.notif_channel_name, R.string.notif_channel_description, NotificationManager.IMPORTANCE_DEFAULT)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lunosfer)
            .setColor(NOTIFICATION_ACCENT)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    // WhatsApp tarzi: kisi basina TEK bildirim kutusu. Ayni gonderenden yeni
    // mesaj geldiginde onceki bildirim kapatilmiyor, MessagingStyle'a yeni
    // satir olarak ekleniyor (aktif bildirimden style'i geri okuyarak).
    private fun showMessageNotification(senderId: String, senderName: String, messageText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(notificationManager, MESSAGE_CHANNEL_ID, R.string.notif_message_channel_name, R.string.notif_message_channel_description, NotificationManager.IMPORTANCE_HIGH)

        val notificationId = conversationNotificationId(senderId)
        val me = Person.Builder().setName(getString(R.string.notif_me)).setKey(ME_PERSON_KEY).build()
        val sender = Person.Builder().setName(senderName).setKey(senderId).build()

        val existingStyle = notificationManager.activeNotifications
            .firstOrNull { it.id == notificationId }
            ?.notification
            ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }

        val style: NotificationCompat.MessagingStyle = existingStyle ?: NotificationCompat.MessagingStyle(me)
        style.addMessage(messageText, System.currentTimeMillis(), sender)

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("target_route", "thread/$senderId")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, notificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val replyLabel = getString(R.string.notif_action_reply)
        val remoteInput = RemoteInput.Builder(ReplyReceiver.KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        val replyIntent = Intent(this, ReplyReceiver::class.java).apply {
            putExtra(ReplyReceiver.EXTRA_SENDER_ID, senderId)
            putExtra(ReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        // RemoteInput ile kullanilan PendingIntent MUTABLE olmak ZORUNDA —
        // sistem cevap metnini bu Intent'in extra'larina yazarak gonderiyor;
        // IMMUTABLE verilirse RemoteInput.getResultsFromIntent() hep null doner.
        val replyPendingIntent = PendingIntent.getBroadcast(
            this, notificationId, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_reply, replyLabel, replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .build()

        val markReadIntent = Intent(this, MarkReadReceiver::class.java).apply {
            putExtra(MarkReadReceiver.EXTRA_SENDER_ID, senderId)
            putExtra(MarkReadReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            this, notificationId, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_check, getString(R.string.notif_action_mark_read), markReadPendingIntent
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()

        val notification = NotificationCompat.Builder(this, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lunosfer)
            .setColor(NOTIFICATION_ACCENT)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(replyAction)
            .addAction(markReadAction)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun ensureChannel(
        notificationManager: NotificationManager,
        channelId: String,
        nameRes: Int,
        descriptionRes: Int,
        importance: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, getString(nameRes), importance).apply {
                description = getString(descriptionRes)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "LunosferFCM"
        const val CHANNEL_ID = "lunosfer_notifications"
        const val MESSAGE_CHANNEL_ID = "lunosfer_messages"
        const val ME_PERSON_KEY = "me"
        // Marka altin rengi (launcher ikonundaki #D9B166).
        const val NOTIFICATION_ACCENT = 0xFFD9B166.toInt()

        // Ayni gonderenin bildirimi her zaman AYNI notification ID'ye dusmeli ki
        // yeni mesaj eskisini kapatip yeni satir acmak yerine mevcut kutuya eklensin.
        // "msg_" namespace'i, genel showNotification() tarafindan kullanilan tag
        // hash'leriyle ID cakismasini engeller.
        fun conversationNotificationId(senderId: String): Int = ("msg_$senderId").hashCode()

        // Sohbet ekrani acikken (ThreadScreen) o kisiden gelen push'u gostermemek
        // ve ekran acilinca bildirimini kapatmak icin.
        @Volatile
        var activeThreadUserId: String? = null

        // Oncelik type+id: backend "url"i web Service Worker icin web yoluyla
        // gonderiyor (/u/.., /messages?with=..), native rotalarla eslesmiyor.
        // Eski payload'lar icin url de native rotaya cevriliyor; taninmayan
        // rota null doner (uygulama ana ekranda acilir).
        fun parseTargetRoute(data: Map<String, String>): String? {
            val type = data["type"]?.lowercase()
            val id = (data["id"] ?: data["target_id"] ?: data["entity_id"])?.takeIf { it.isNotBlank() }
            if (!type.isNullOrBlank()) {
                val route = when (type) {
                    "dream", "dream_detail" -> id?.let { "dream/$it" }
                    "thread", "message", "chat" -> id?.let { "thread/$it" }
                    "goal", "vision", "goal_detail" -> id?.let { "goal/$it" }
                    "user", "profile" -> id?.let { "public_profile/$it" }
                    "diary", "diary_comment" -> id?.let { "diary_journal/$it" }
                    "notification", "notifications" -> "notifications"
                    else -> null
                }
                if (route != null) return route
            }

            val rawUrl = (data["url"] ?: data["target_route"])?.trim()?.removePrefix("/")
            if (rawUrl.isNullOrBlank()) return null
            val path = rawUrl.substringBefore('?')
            val query = rawUrl.substringAfter('?', "")
            val segments = path.split('/').filter { it.isNotBlank() }
            val first = segments.firstOrNull() ?: return null
            val second = segments.getOrNull(1)
            return when (first) {
                "u", "public_profile" -> second?.let { "public_profile/$it" }
                "thread" -> second?.let { "thread/$it" }
                "messages" -> query.split('&')
                    .firstOrNull { it.startsWith("with=") }
                    ?.removePrefix("with=")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "thread/$it" }
                    ?: "messages"
                "dream" -> second?.takeIf { it.toLongOrNull() != null }?.let { "dream/$it" }
                "goal" -> second?.let { "goal/$it" }
                "diary_journal" -> second?.let { "diary_journal/$it" }
                "notifications" -> "notifications"
                else -> null
            }
        }

        fun sendTokenToServer(token: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    NetworkModule.api.subscribePush(PushSubscriptionRequest(token = token))
                    Log.d(TAG, "FCM token successfully registered to server")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send FCM token to server (silently ignored)", e)
                }
            }
        }

        fun registerCurrentFcmToken() {
            try {
                val ctx = runCatching { io.lunosfer.dreamap.DreamapApp.instance }.getOrNull()
                val apps = if (ctx != null) {
                    try {
                        if (com.google.firebase.FirebaseApp.getApps(ctx).isEmpty()) {
                            com.google.firebase.FirebaseApp.initializeApp(ctx)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "FirebaseApp.initializeApp failed: ${e.message}")
                    }
                    com.google.firebase.FirebaseApp.getApps(ctx)
                } else {
                    emptyList()
                }

                if (apps.isNotEmpty()) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            if (!token.isNullOrEmpty()) {
                                sendTokenToServer(token)
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "FirebaseApp not initialized; skipping FCM token registration")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Could not fetch FCM token", e)
            }
        }
    }
}
