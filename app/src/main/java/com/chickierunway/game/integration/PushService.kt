package com.chickierunway.game.integration

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chickierunway.game.MainActivity
import com.chickierunway.game.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM messages. A push arrives two ways, and that is the source of half the bugs here:
 *
 *  - app in foreground           -> onMessageReceived fires, we draw the notification;
 *  - app in background + a
 *    `notification` block present -> the system draws it, onMessageReceived is NOT called, and the
 *                                    data lands in the launch intent extras.
 *
 * Drawing our own notification with the link in extras funnels both paths through one Activity
 * entry (consumeIntent), instead of two branches where one is always broken.
 */
class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // The token changes on reinstall, restore and clear-data.
        IntegrationStorage.pushToken = token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = PushPayload.from(message)
        showNotification(payload)
    }

    private fun showNotification(payload: PushPayload) {
        ensureChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FROM_PUSH, true)
            payload.url?.let { putExtra(EXTRA_PUSH_URL, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            payload.url.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(payload.title ?: getString(R.string.app_name))
            .apply { payload.body?.let { setContentText(it) } }
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        // notify is a no-op without POST_NOTIFICATIONS; the token still arrives, so attribution
        // and push_token in the click do not depend on the permission.
        runCatching {
            NotificationManagerCompat.from(this).notify(payload.url.hashCode(), notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Updates", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        const val EXTRA_FROM_PUSH = "chickie_from_push"
        const val EXTRA_PUSH_URL = "chickie_push_url"
        private const val CHANNEL_ID = "offers"
    }
}

/** Title, body and the link to open — from the notification block or the data map, in that order. */
data class PushPayload(
    val title: String?,
    val body: String?,
    val url: String?,
) {
    companion object {
        fun from(message: RemoteMessage): PushPayload =
            from(message.data, message.notification?.title, message.notification?.body)

        /** Pure part of [from], so the field fallbacks are unit-testable without RemoteMessage. */
        fun from(data: Map<String, String>, title: String?, body: String?): PushPayload =
            PushPayload(
                title = title ?: data["title"],
                body = body ?: data["body"] ?: data["message"],
                url = (data["url"] ?: data["link"])?.takeIf { it.isNotBlank() },
            )
    }
}
