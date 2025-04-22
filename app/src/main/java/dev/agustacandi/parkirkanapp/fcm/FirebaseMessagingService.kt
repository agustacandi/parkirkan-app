package dev.agustacandi.parkirkanapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.agustacandi.parkirkanapp.MainActivity
import dev.agustacandi.parkirkanapp.R
import dev.agustacandi.parkirkanapp.domain.auth.repository.AuthRepository
import dev.agustacandi.parkirkanapp.fcm.FirebaseMessagingServiceAAA.Companion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "fcm_notification_channel_v2"
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


        // Buat pola vibrasi panjang
        val vibrationPattern = longArrayOf(
            0, 1000, 200, 1000, 200, 1000, 200, 1000, 200, 1000
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(vibrationPattern)


        triggerSystemVibration(vibrationPattern)

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

        // Custom sound URI
        val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm_sound}")

        // Audio attributes untuk sound alarm
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM) // ALARM untuk suara keras
            .build()

        // Pola vibrasi panjang
        val longVibrationPattern = longArrayOf(
            0, 1000, 200, 1000, 200, 1000, 200, 1000, 200, 1000
        )
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
            setSound(soundUri, audioAttributes)
            vibrationPattern = longVibrationPattern
            enableVibration(true)
            setBypassDnd(true)
            enableLights(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setShowBadge(true)
        }

        // Register channel with system
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun triggerSystemVibration(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator

                // Gunakan VibrationEffect dengan amplitudo yang berubah-ubah
                val amplitudes = IntArray(pattern.size) { i ->
                    if (i % 2 == 1) VibrationEffect.DEFAULT_AMPLITUDE else 0
                }
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }
    }
}