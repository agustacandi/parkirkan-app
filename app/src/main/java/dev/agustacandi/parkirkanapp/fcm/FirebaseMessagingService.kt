package dev.agustacandi.parkirkanapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.agustacandi.parkirkanapp.MainActivity
import dev.agustacandi.parkirkanapp.R
import dev.agustacandi.parkirkanapp.domain.auth.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "fcm_notification_channel"
        private const val CHANNEL_NAME = "FCM Notifications"
        private const val CHANNEL_DESCRIPTION = "Receive notifications from server"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Periksa jika pesan berisi data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            val title = remoteMessage.data["title"] ?: "Notification"
            val message = remoteMessage.data["message"] ?: "You have a new notification"

            // Tambahan data untuk deep linking jika diperlukan
            val deepLinkData = remoteMessage.data.filter {
                it.key != "title" && it.key != "message"
            }

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(applicationContext, alarmSound)
            ringtone.play()

            // Tampilkan notifikasi
            showNotification(title, message, deepLinkData)
        }

        // Periksa jika pesan berisi notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title ?: "Notification", it.body ?: "You have a new notification")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // Kirim token baru ke server
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        authRepository.updateFcmToken(token)
            .onEach { result ->
                if (result.isSuccess) {
                    Log.d(TAG, "FCM token updated successfully")
                } else {
                    Log.e(TAG, "Failed to update FCM token", result.exceptionOrNull())
                }
            }
            .catch { e ->
                Log.e(TAG, "Error updating FCM token", e)
            }
            .launchIn(serviceScope)
    }

    private fun showNotification(
        title: String,
        message: String,
        deepLinkData: Map<String, String>? = null
    ) {
        // Buat channel notifikasi (untuk Android Oreo/API 26+)
        createNotificationChannel()

        // Intent yang akan dibuka ketika notifikasi di-tap
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            // Tambahkan data untuk deep linking jika ada
            deepLinkData?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Buat notification builder
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))

        // Tampilkan notifikasi
        with(NotificationManagerCompat.from(this)) {
            try {
                notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "No notification permission", e)
                // Izin notifikasi tidak diberikan
            }
        }
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
        }

        // Register channel with system
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}