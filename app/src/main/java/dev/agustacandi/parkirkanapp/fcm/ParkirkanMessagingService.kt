package dev.agustacandi.parkirkanapp.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import dev.agustacandi.parkirkanapp.app.MainApp
import dev.agustacandi.parkirkanapp.domain.auth.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Firebase Messaging Service for handling push notifications
 * This implementation ensures consistent notification behavior across all Android versions
 */
@AndroidEntryPoint
class ParkirkanMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "ParkirkanFCM"
        private const val WAKELOCK_TIMEOUT = 20000L // 20 seconds
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
                ?: remoteMessage.data["notification_title"]
                ?: remoteMessage.data["title"]
                ?: "Attention!"
            val message = remoteMessage.notification?.body
                ?: remoteMessage.data["notification_body"]
                ?: remoteMessage.data["message"]
                ?: "Someone is taking your vehicle!"

            // Log detailed information for debugging
            Log.d(TAG, "=== FCM Message Details ===")
            Log.d(TAG, "Notification Type: $notificationType")
            Log.d(TAG, "Title: $title")
            Log.d(TAG, "Message: $message")
            Log.d(TAG, "Has notification payload: ${remoteMessage.notification != null}")
            Log.d(TAG, "All Data Keys: ${remoteMessage.data.keys}")
            remoteMessage.data.forEach { (key, value) ->
                Log.d(TAG, "Data[$key] = $value")
            }
            Log.d(TAG, "========================")

            // Additional data for deep linking if needed
            val deepLinkData = remoteMessage.data.filter {
                it.key != "title" && it.key != "message" && it.key != "notification_type" &&
                        it.key != "notification_title" && it.key != "notification_body"
            }

            // For alert notifications, we want to use special handling
            if (notificationType == "alert" || remoteMessage.data["click_action"] == "OPEN_NOTIFICATION") {
                Log.d(TAG, "Routing to ALERT notification")
                showAlertNotification(title, message, deepLinkData)
            } else {
                Log.d(TAG, "Routing to NORMAL notification (default)")
                // Regular notification
                showNormalNotification(title, message, deepLinkData)
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

    private fun showAlertNotification(
        title: String,
        message: String,
        deepLinkData: Map<String, String>? = null
    ) {
        try {
            // Intent to open the Alert screen directly
            val intent = Intent(this, MainActivity::class.java).apply {
                action = "OPEN_NOTIFICATION"
                addCategory(Intent.CATEGORY_LAUNCHER)
                // Use stronger flags to ensure the activity is brought to front
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )

                // Important extras to direct to Alert screen
                putExtra("notification_opened", true)
                putExtra("notification_type", "alert")
                putExtra("target_route", NavDestination.Alert.route)
                putExtra("force_navigation", true) // New flag to force navigation
                putExtra("timestamp", System.currentTimeMillis())

                // Add deep link data if available
                deepLinkData?.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }

            // Create stronger PendingIntent with unique request code
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            // Create additional deep link intent as backup approach
            val deepLinkUri = Uri.parse("parkirkanapp://alert")
            val deepLinkIntent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
                putExtras(intent) // Copy all extras from the main intent
            }

            // Ensure we have a unique request code for each notification
            val requestCode = System.currentTimeMillis().toInt()

            // Log intent details for debugging
            Log.d(
                TAG,
                "Creating PendingIntent for alert notification with action: ${intent.action}"
            )
            Log.d(
                TAG,
                "Intent extras: notification_opened=${
                    intent.getBooleanExtra(
                        "notification_opened",
                        false
                    )
                }, " +
                        "notification_type=${intent.getStringExtra("notification_type")}, " +
                        "target_route=${intent.getStringExtra("target_route")}, " +
                        "force_navigation=${intent.getBooleanExtra("force_navigation", false)}"
            )

            // Try to use the main intent first, but create a backup with the deep link
            val pendingIntent = PendingIntent.getActivity(
                this, requestCode, intent, pendingIntentFlags
            )

            // Create backup PendingIntent with deep link
            val backupPendingIntent = PendingIntent.getActivity(
                this, requestCode + 1, deepLinkIntent, pendingIntentFlags
            )

            // Sound URI
            val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm_sound}")

            // Create long vibration pattern for urgent alerts
            val vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)

            // Create notification with highest priority and multiple action paths
            val notificationBuilder = NotificationCompat.Builder(this, MainApp.ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(vibrationPattern)
                .setSound(soundUri)
                .setLights(ContextCompat.getColor(this, R.color.colorAccent), 1000, 500)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_notifications, "View Alert", pendingIntent)
                // Add backup action with deep link
                .addAction(R.drawable.ic_notifications, "Open", backupPendingIntent)
                // Ensure notification appears as heads-up
                .setFullScreenIntent(pendingIntent, true)
                .setOnlyAlertOnce(false) // Allow sound every time

            // Make notification display even when app is in foreground on Android 13+
            if (Build.VERSION.SDK_INT >= 33) {
                notificationBuilder.setForegroundServiceBehavior(
                    NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
                )
            }

            // Trigger system vibration separately to ensure it works
            triggerSystemVibration(vibrationPattern)

            // Show the notification
            val notificationManager = NotificationManagerCompat.from(this)
            try {
                val notificationId = requestCode // Use same value as request code
                notificationManager.notify(notificationId, notificationBuilder.build())
                Log.d(TAG, "Alert notification shown with ID: $notificationId")

                // Ensure notification works in background
                wakeUpForNotification()
            } catch (e: SecurityException) {
                Log.e(TAG, "No notification permission: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing alert notification", e)
        }
    }

    private fun showNormalNotification(
        title: String,
        message: String,
        deepLinkData: Map<String, String>? = null
    ) {
        try {
            // Regular notification intent
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

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

            // Create standard notification
            val notificationBuilder =
                NotificationCompat.Builder(this, MainApp.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVibrate(longArrayOf(0, 300, 200, 300))
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

    // Helper method to trigger system vibration
    private fun triggerSystemVibration(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator

                // Use VibrationEffect with varying amplitudes
                val amplitudes = IntArray(pattern.size) { i ->
                    if (i % 2 == 1) VibrationEffect.DEFAULT_AMPLITUDE else 0
                }
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }
    }

    // Additional method to help ensure notification works in background
    private fun wakeUpForNotification() {
        try {
            // Try to wake up screen (if allowed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isInteractive) {
                    // If screen is off, try to raise priority
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notificationChannels.find { it.id == MainApp.ALERT_CHANNEL_ID }
                        ?.let { channel ->
                            if (channel.importance < NotificationManager.IMPORTANCE_HIGH) {
                                Log.d(TAG, "Upgrading channel importance")
                                // Can't directly change importance, so recreate channel
                                nm.deleteNotificationChannel(MainApp.ALERT_CHANNEL_ID)

                                // Recreate with same importance from MainApp
                                (application as? MainApp)?.createNotificationChannels()
                            }
                        }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error waking up device", e)
        }
    }
}