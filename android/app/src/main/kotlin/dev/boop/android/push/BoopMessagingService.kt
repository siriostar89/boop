package dev.boop.android.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.boop.android.BoopApp
import dev.boop.android.MainActivity
import dev.boop.android.R
import dev.boop.android.data.model.EventAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Receives FCM pushes, shows them as notifications and keeps the token registered. */
class BoopMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val app = application as? BoopApp ?: return
        scope.launch { app.container.pushRegistrar.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        showNotification(
            title = notification.title.orEmpty(),
            body = notification.body.orEmpty(),
            eventId = message.data[KEY_EVENT_ID].orEmpty(),
            actions = message.data[KEY_ACTIONS]?.let { runCatching { actionsJson.decodeFromString(ListSerializer(EventAction.serializer()), it) }.getOrDefault(emptyList()) } ?: emptyList(),
        )
    }

    private fun showNotification(title: String, body: String, eventId: String, actions: List<EventAction>) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val tap = PendingIntent.getActivity(
            this,
            eventId.hashCode(),
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_EVENT_ID, eventId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.ifBlank { getString(R.string.app_name) })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        actions.take(MAX_ACTIONS).forEachIndexed { index, action ->
            val actionIntent = PendingIntent.getActivity(
                this,
                (eventId + action.url).hashCode() + index,
                Intent(Intent.ACTION_VIEW, Uri.parse(action.url)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, action.label, actionIntent)
        }

        manager.notify(eventId.hashCode(), builder.build())
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Matches the FCM data keys sent by the server (see server/internal/fcm Payload). */
        const val KEY_EVENT_ID = "event_id"
        const val KEY_ACTIONS = "actions"
        const val CHANNEL_ID = "boop_events"
        private const val MAX_ACTIONS = 3
        private val actionsJson = Json { ignoreUnknownKeys = true }
    }
}
