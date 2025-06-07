package dev.agustacandi.parkirkanapp.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import dev.agustacandi.parkirkanapp.R

/**
 * Main Application class for ParkirkanApp
 * Handles initialization of Firebase and notification channels
 */
@HiltAndroidApp
class MainApp: Application() {
    
    companion object {
        const val ALERT_CHANNEL_ID = "parkirkan_alert_channel"
        const val NOTIFICATION_CHANNEL_ID = "parkirkan_notification_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Create notification channels
        createNotificationChannels()
    }
    
    /**
     * Creates notification channels for different priority levels
     * - Alert channel: High priority for urgent notifications (vehicle alerts)
     * - Regular channel: Default priority for regular app notifications
     */
    fun createNotificationChannels() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Try to delete existing channels to ensure fresh settings
        try {
            notificationManager.deleteNotificationChannel(ALERT_CHANNEL_ID)
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
        } catch (e: Exception) {
            // Ignore
        }

        // Alert Channel (High Priority)
        val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm_sound}")
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM) // ALARM for loud sound
            .build()

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Parking Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important alerts about your vehicle"
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            enableLights(true)
            lightColor = ContextCompat.getColor(this@MainApp, R.color.colorAccent)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setBypassDnd(true)
            setShowBadge(true)

            // Set importance again to ensure it's applied
            importance = NotificationManager.IMPORTANCE_HIGH
        }
        notificationManager.createNotificationChannel(alertChannel)

        // Regular Notification Channel
        val normalChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "App Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Regular app notifications"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            enableLights(true)
            lightColor = ContextCompat.getColor(this@MainApp, R.color.colorAccent)

            // Set importance again to ensure it's applied
            importance = NotificationManager.IMPORTANCE_HIGH
        }
        notificationManager.createNotificationChannel(normalChannel)
    }
}