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
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.agustacandi.parkirkanapp.MainActivity
import dev.agustacandi.parkirkanapp.NavDestination
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
class ParkirkanMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "ParkirkanFCM"
        private const val CHANNEL_ID = "parkirkan_alert_channel"
        private const val CHANNEL_NAME = "Parking Alerts"
        private const val CHANNEL_DESCRIPTION = "Important alerts about your vehicle"
        private const val WAKELOCK_TIMEOUT = 20000L // 20 seconds
    }

    override fun onCreate() {
        super.onCreate()
        // Create channels early
        createNotificationChannels()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Data: ${remoteMessage.data}")
        Log.d(TAG, "Notification: ${remoteMessage.notification}")

        // Acquire a wakelock to ensure processing completes
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ParkirkanApp:FCMWakeLock"
        ).apply {
            acquire(WAKELOCK_TIMEOUT)
        }

        try {
            // Process the notification
            val notificationType = remoteMessage.data["notification_type"] ?: "default"
            val title = remoteMessage.notification?.title
                ?: remoteMessage.data["title"]
                ?: "Perhatian!"
            val message = remoteMessage.notification?.body
                ?: remoteMessage.data["message"]
                ?: "Ada yang bawa motor kamu nih!"

            // For alert notifications, we want to use a special handling path
            if (notificationType == "alert" || remoteMessage.data["click_action"] == "OPEN_NOTIFICATION") {
                showAlertNotification(title, message)
            } else {
                // Regular notification
                showNormalNotification(title, message)
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
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

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Try to delete existing channel to ensure fresh settings
            try {
                notificationManager.deleteNotificationChannel(CHANNEL_ID)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting existing channel", e)
            }

            // Sound URI - use raw resource
            val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm_sound}")

            // Create audio attributes
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM) // ALARM for loud sound
                .build()

            // Create the notification channel with maximum importance
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION

                // Set sound
                setSound(soundUri, audioAttributes)

                // Set vibration pattern
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)

                // Other settings
                enableLights(true)
                lightColor = ContextCompat.getColor(this@ParkirkanMessagingService, R.color.colorAccent)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun showAlertNotification(title: String, message: String) {
        try {
            // Intent to open the Alert screen directly
            val intent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

                // Important extras to direct to Alert screen
                putExtra("notification_opened", true)
                putExtra("notification_type", "alert")
                putExtra("target_route", NavDestination.Alert.route)
                putExtra("timestamp", System.currentTimeMillis())
            }

            // Create PendingIntent with unique request code
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val requestCode = System.currentTimeMillis().toInt()
            val pendingIntent = PendingIntent.getActivity(
                this, requestCode, intent, pendingIntentFlags
            )

            // Sound URI
            val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm_sound}")

            // Create notification with highest priority
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
                .setSound(soundUri)
                .setLights(ContextCompat.getColor(this, R.color.colorAccent), 1000, 500)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                // Full screen intent for heads-up notification
                .setFullScreenIntent(pendingIntent, true)

            // Show the notification
            val notificationManager = NotificationManagerCompat.from(this)
            try {
                val notificationId = requestCode // Use same value as request code
                notificationManager.notify(notificationId, notificationBuilder.build())
                Log.d(TAG, "Alert notification shown with ID: $notificationId")
            } catch (e: SecurityException) {
                Log.e(TAG, "No notification permission: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing alert notification", e)
        }
    }

    private fun showNormalNotification(title: String, message: String) {
        try {
            // Regular notification intent
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(
                this, System.currentTimeMillis().toInt(), intent, pendingIntentFlags
            )

            // Default notification sound
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // Create standard notification
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(defaultSoundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            // Show the notification
            val notificationManager = NotificationManagerCompat.from(this)
            try {
                val notificationId = System.currentTimeMillis().toInt()
                notificationManager.notify(notificationId, notificationBuilder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "No notification permission: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing normal notification", e)
        }
    }
}