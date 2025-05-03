package dev.agustacandi.parkirkanapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.util.Locale

/**
 * Utility to check and handle battery optimization settings
 */
object BatteryOptimizationChecker {
    /**
     * Checks if the app is ignoring battery optimizations
     * @return true if battery optimizations are ignored (good), false if they are applied (bad)
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val packageName = context.packageName
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true // On older Android versions, this isn't a concern
        }
    }

    /**
     * Creates an intent to open battery optimization settings
     */
    fun getBatteryOptimizationSettingsIntent(context: Context): Intent {
        val packageName = context.packageName

        return when {
            // For newer devices, direct to app's battery optimization settings
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
            // Fallback to general battery settings
            else -> {
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            }
        }
    }

    /**
     * Gets a device-specific intent for notification settings
     * Works for different device manufacturers
     */
    fun getNotificationSettingsIntent(context: Context): Intent {
        val packageName = context.packageName

        // Try direct notification settings first (works on most devices)
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        }

        // Add manufacturer-specific extras
        when {
            // Xiaomi/MIUI devices
            isMiui() -> {
                intent.putExtra("package", packageName)
                intent.putExtra("app_package", packageName)
            }
            // Samsung devices
            isSamsung() -> {
                intent.putExtra("packageName", packageName)
            }
            // Huawei devices
            isHuawei() -> {
                intent.putExtra("pkg_name", packageName)
            }
        }

        return intent
    }

    // Helper methods to detect manufacturer
    private fun isMiui(): Boolean = Build.MANUFACTURER.lowercase(Locale.ROOT).contains("xiaomi")
    private fun isSamsung(): Boolean = Build.MANUFACTURER.lowercase().contains("samsung")
    private fun isHuawei(): Boolean = Build.MANUFACTURER.lowercase().contains("huawei")
}