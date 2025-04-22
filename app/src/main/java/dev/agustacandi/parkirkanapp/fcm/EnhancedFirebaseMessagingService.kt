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
class EnhancedFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "EnhancedFCM"
        private const val CHANNEL_ID = "high_priority_notifications"
        private const val CHANNEL_NAME = "Urgent Notifications"
        private const val CHANNEL_DESCRIPTION = "Important notifications that require immediate attention"
        private const val WAKELOCK_TIMEOUT = 10000L // 10 seconds
    }

    override fun onCreate() {
        super.onCreate()
        // Create notification channel as early as possible
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Acquire a wakelock to ensure notification processing completes
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ParkirkanApp:FCMWakeLock"
        )
        wakeLock.acquire(WAKELOCK_TIMEOUT)

        try {
            Log.d(TAG, "From: ${remoteMessage.from}")

            // Check for data payload
            if (remoteMessage.data.isNotEmpty()) {
                Log.d(TAG, "Message data payload: ${remoteMessage.data}")

                val title = remoteMessage.data["title"] ?: "Notification"
                val message = remoteMessage.data["message"] ?: "You have a new notification"

                // Additional data for deep linking if needed
                val deepLinkData = remoteMessage.data.filter {
                    it.key != "title" && it.key != "message"
                }

                // Show high priority notification
                showHighPriorityNotification(title, message, deepLinkData)
            }

            // Check for notification payload
            remoteMessage.notification?.let {
                Log.d(TAG, "Message Notification Body: ${it.body}")
                showHighPriorityNotification(
                    it.title ?: "Notification",
                    it.body ?: "You have a new notification"
                )
            }
        } finally {
            // Ensure wakelock is released after completion
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // Send new token to server
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Delete any existing channel to ensure fresh settings
                notificationManager.deleteNotificationChannel(CHANNEL_ID)

                // Custom sound URI
                val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm_sound}")

                // Audio attributes for alarm sound
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM) // ALARM for loud sound
                    .build()

                // Long vibration pattern
                val longVibrationPattern = longArrayOf(
                    0, 1000, 200, 1000, 200, 1000, 200, 1000, 200, 1000
                )

                // Create high priority channel
                val highPriorityChannel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    setSound(soundUri, audioAttributes)
                    vibrationPattern = longVibrationPattern
                    setBypassDnd(true) // Bypass Do Not Disturb
                    enableLights(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }

                // Register channel
                notificationManager.createNotificationChannel(highPriorityChannel)
                Log.d(TAG, "High priority notification channel created")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel", e)
            }
        }
    }

    private fun showHighPriorityNotification(
        title: String,
        message: String,
        deepLinkData: Map<String, String>? = null
    ) {
        try {
            // Ensure channel is registered
            createNotificationChannel()

            // Intent for when notification is clicked
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                action = "OPEN_FROM_NOTIFICATION" // Special action for intent filter
                putExtra("notification_opened", true)

                // Add deep link data if available
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

            // Custom sound URI from resource
            val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm_sound}")

            // Create long vibration pattern
            val vibrationPattern = longArrayOf(
                0, 1000, 200, 1000, 200, 1000, 200, 1000, 200, 1000
            )

            // Create high-priority notification
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX) // Maximum priority
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Alarm category
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(vibrationPattern)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true) // Full screen intent for heads-up

            // Trigger system vibration
            triggerSystemVibration(vibrationPattern)

            // Show notification
            val notificationManager = NotificationManagerCompat.from(this)
            try {
                // Use unique ID for each message to avoid overriding
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

    // Helper to trigger system vibration
    private fun triggerSystemVibration(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator

                // Use VibrationEffect with varying amplitudes
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