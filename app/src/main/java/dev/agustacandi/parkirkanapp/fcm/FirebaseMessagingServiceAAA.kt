package dev.agustacandi.parkirkanapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.PowerManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessagingServiceAAA : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "parkirkan_high_priority_channel"
        private const val CHANNEL_NAME = "Pemberitahuan Penting"
        private const val CHANNEL_DESCRIPTION = "Notifikasi penting dengan suara dan getaran kustom"
        private const val WAKELOCK_TIMEOUT = 10000L // 10 detik
    }

    override fun onCreate() {
        super.onCreate()
        // Buat channel saat service dibuat untuk memastikan teregister lebih awal
        createNotificationChannels()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Aktifkan wakelock untuk memastikan proses berjalan tuntas
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ParkirkanApp:FCMWakeLock"
        )
        wakeLock.acquire(WAKELOCK_TIMEOUT)

        try {
            Log.d(TAG, "From: ${remoteMessage.from}")

            // Periksa jika pesan berisi data payload
            if (remoteMessage.data.isNotEmpty()) {
                Log.d(TAG, "Message data payload: ${remoteMessage.data}")

                val title = remoteMessage.data["title"] ?: "Notifikasi"
                val message = remoteMessage.data["message"] ?: "Ada pemberitahuan baru"

                // Tambahan data untuk deep linking jika diperlukan
                val deepLinkData = remoteMessage.data.filter {
                    it.key != "title" && it.key != "message"
                }

                // Tampilkan notifikasi dengan HIGH_PRIORITY
                showHighPriorityNotification(title, message, deepLinkData)
            }

            // Periksa jika pesan berisi notification payload
            remoteMessage.notification?.let {
                Log.d(TAG, "Message Notification Body: ${it.body}")
                showHighPriorityNotification(
                    it.title ?: "Notifikasi",
                    it.body ?: "Ada pemberitahuan baru"
                )
            }
        } finally {
            // Pastikan wakelock dilepas setelah selesai
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
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

    // Membuat semua channel notifikasi yang diperlukan
    private fun createNotificationChannels() {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Hapus channel lama jika ada untuk memastikan pengaturan terbaru
                notificationManager.deleteNotificationChannel(CHANNEL_ID)

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

                // Buat channel high priority
                val highPriorityChannel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // Menggunakan HIGH untuk kompatibilitas lebih baik
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    setSound(soundUri, audioAttributes)
                    vibrationPattern = longVibrationPattern
                    setBypassDnd(true) // Lewati Do Not Disturb
                    enableLights(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }

                // Register channel
                notificationManager.createNotificationChannel(highPriorityChannel)
                Log.d(TAG, "High priority notification channel created during service onCreate")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channels", e)
            }
    }

    private fun showHighPriorityNotification(
        title: String,
        message: String,
        deepLinkData: Map<String, String>? = null
    ) {
        try {
            // Pastikan channel terdaftar
            createNotificationChannels()

            // Intent untuk membuka activity saat notifikasi diklik
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                action = "OPEN_FROM_NOTIFICATION" // Action khusus untuk intent filter
                putExtra("notification_opened", true)

                // Tambahkan data deep link jika ada
                deepLinkData?.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(
                this, System.currentTimeMillis().toInt(), intent, pendingIntentFlags
            )

            // URI suara kustom dari resource
            val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm_sound}")

            // Buat pola vibrasi panjang
            val vibrationPattern = longArrayOf(
                0, 1000, 200, 1000, 200, 1000, 200, 1000, 200, 1000
            )

            // Create notifikasi dengan prioritas tinggi
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX) // Prioritas maksimum
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Kategori alarm
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(vibrationPattern)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true) // Full screen intent untuk heads-up

            // Eksekusi vibrasi melalui sistem
            triggerSystemVibration(vibrationPattern)

            // Tampilkan notifikasi
            val notificationManager = NotificationManagerCompat.from(this)
            try {
                // Gunakan ID unik untuk setiap pesan untuk menghindari overriding
                val notificationId = System.currentTimeMillis().toInt()
                notificationManager.notify(notificationId, notificationBuilder.build())
                Log.d(TAG, "High priority notification shown with ID: $notificationId")
            } catch (e: SecurityException) {
                Log.e(TAG, "No notification permission: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    // Helper untuk memicu vibrasi melalui sistem
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