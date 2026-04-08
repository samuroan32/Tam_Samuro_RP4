package com.twinscalev4.notification

import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.twinscalev4.MainActivity
import com.twinscalev4.R
import com.twinscalev4.TwinScaleApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TwinScaleMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = TokenSyncManager.userId ?: return
        val roomId = TokenSyncManager.roomId

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                FirebaseDatabase.getInstance().reference
                    .child("users").child(userId).child("fcmToken").setValue(token).await()

                if (!roomId.isNullOrBlank()) {
                    FirebaseDatabase.getInstance().reference
                        .child("rooms").child(roomId)
                        .child("users").child(userId)
                        .child("fcmToken").setValue(token).await()
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val roomId = message.data["roomId"]
        val senderName = message.data["senderName"] ?: "Партнёр"
        val text = message.data["text"] ?: "Новое сообщение"

        val shouldSuppress = ChatPresenceManager.isAppInForeground &&
            ChatPresenceManager.isChatVisible &&
            ChatPresenceManager.activeRoomId == roomId

        if (shouldSuppress) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("roomId", roomId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(this, TwinScaleApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$senderName написал(а)")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}
