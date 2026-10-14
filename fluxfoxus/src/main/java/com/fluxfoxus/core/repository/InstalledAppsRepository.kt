package com.fluxfoxus.core.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Clean domain model for an installed user application.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false
) {
    /**
     * Converts the Android Drawable icon into a Compose-ready ImageBitmap.
     */
    fun toImageBitmap(): ImageBitmap? {
        val drawable = icon ?: return null
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap.asImageBitmap()
            } else {
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Repository to query, filter, and cache launchable installed applications.
 * Eliminates manual "App Name" and "Path Name" text inputs.
 */
class InstalledAppsRepository(
    private val context: Context,
    private val packageManager: PackageManager = context.packageManager
) {
    private val mutex = Mutex()
    private var cachedApps: List<AppInfo>? = null

    /**
     * Query all launchable apps using Intent(ACTION_MAIN, CATEGORY_LAUNCHER).
     * Filters out FluxFoxus itself and internal background system services.
     */
    suspend fun getInstalledApps(
        includeSystemApps: Boolean = false,
        forceRefresh: Boolean = false
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!forceRefresh && cachedApps != null) {
                val list = cachedApps!!
                return@withContext if (includeSystemApps) list else list.filter { !it.isSystemApp }
            }

            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfoList = try {
                packageManager.queryIntentActivities(launcherIntent, 0)
            } catch (e: Exception) {
                emptyList()
            }

            val apps = resolveInfoList
                .mapNotNull { resolveInfo ->
                    val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                    val pkg = activityInfo.packageName
                    if (pkg == context.packageName) return@mapNotNull null // Don't block self

                    val appName = try {
                        resolveInfo.loadLabel(packageManager).toString()
                    } catch (_: Exception) {
                        pkg
                    }

                    val icon = try {
                        resolveInfo.loadIcon(packageManager)
                    } catch (_: Exception) {
                        null
                    }

                    val isSystem = try {
                        val appInfo = packageManager.getApplicationInfo(pkg, 0)
                        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    } catch (_: Exception) {
                        false
                    }

                    AppInfo(
                        packageName = pkg,
                        appName = appName.ifBlank { pkg },
                        icon = icon,
                        isSystemApp = isSystem
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }

            cachedApps = apps
            if (includeSystemApps) apps else apps.filter { !it.isSystemApp }
        }
    }

    /**
     * Filter list of apps by search query matching app label or package name.
     */
    fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return apps
        return apps.filter {
            it.appName.lowercase().contains(trimmed) || it.packageName.lowercase().contains(trimmed)
        }
    }

    /**
     * Look up AppInfo for a specific package.
     */
    suspend fun getAppByPackage(packageName: String): AppInfo? {
        val apps = getInstalledApps(includeSystemApps = true)
        return apps.find { it.packageName == packageName } ?: try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            AppInfo(packageName, appName, icon, isSystem)
        } catch (_: Exception) {
            null
        }
    }
}
