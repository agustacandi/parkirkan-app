package dev.agustacandi.parkirkanapp.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

object MiuiHelper {
    private const val TAG = "MiuiHelper"

    /**
     * Check if device is running MIUI
     */
    fun isMiuiDevice(): Boolean {
        return try {
            val prop = System.getProperty("ro.miui.ui.version.name")
            !prop.isNullOrEmpty()
        } catch (e: Exception) {
            try {
                Build.MANUFACTURER.toLowerCase().contains("xiaomi")
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Open MIUI's special battery settings for the app
     */
    fun openMiuiBatterySettings(context: Context) {
        try {
            // Try MIUI security center first
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening MIUI autostart settings", e)
        }

        try {
            // Try alternative MIUI security center
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.powercenter.PowerSettings"
                )
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening MIUI power settings", e)
        }

        // Fallback to regular battery settings
        try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening battery settings", e)
        }
    }

    /**
     * Open MIUI's notification settings
     */
    fun openMiuiNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$NotificationSettingsActivity"
                )
                putExtra("package", context.packageName)
                putExtra("app_package", context.packageName)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening MIUI notification settings", e)
        }

        // Fallback to app details settings
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app details", e)
        }
    }
}