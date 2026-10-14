package com.fluxfoxus.core.permission

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.fluxfoxus.core.service.FocusAccessibilityService

/**
 * Focus Shields permission status model.
 */
data class ShieldStatus(
    val hasNotifications: Boolean = false,
    val hasUsageStats: Boolean = false,
    val hasOverlay: Boolean = false,
    val hasAccessibility: Boolean = false,
    val hasBatteryOptimizationIgnored: Boolean = false
) {
    /**
     * Mandatory shields: Notifications (if Android 13+), Usage Stats, Overlay, and Accessibility Interceptor.
     */
    val areMandatoryReady: Boolean
        get() = hasNotifications && hasUsageStats && hasOverlay && hasAccessibility
}

/**
 * System permission validation engine.
 */
object PermissionChecker {

    fun isNotificationGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun isUsageStatsGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isOverlayGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isAccessibilityGranted(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, FocusAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true) ||
                componentName.contains(context.packageName)
            ) {
                return true
            }
        }
        return false
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun getShieldStatus(context: Context): ShieldStatus {
        return ShieldStatus(
            hasNotifications = isNotificationGranted(context),
            hasUsageStats = isUsageStatsGranted(context),
            hasOverlay = isOverlayGranted(context),
            hasAccessibility = isAccessibilityGranted(context),
            hasBatteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
        )
    }

    fun areMandatoryGranted(context: Context): Boolean {
        return getShieldStatus(context).areMandatoryReady
    }
}
