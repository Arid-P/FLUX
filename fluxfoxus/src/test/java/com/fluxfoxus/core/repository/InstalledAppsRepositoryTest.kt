package com.fluxfoxus.core.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class InstalledAppsRepositoryTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var repository: InstalledAppsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        packageManager = context.packageManager
        repository = InstalledAppsRepository(context, packageManager)

        // Install dummy apps in Robolectric shadow package manager
        val shadowPm = shadowOf(packageManager)

        val app1Resolve = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.instagram.android"
                name = "com.instagram.main.MainActivity"
                applicationInfo = ApplicationInfo().apply {
                    packageName = "com.instagram.android"
                    flags = 0
                }
            }
        }
        val app1Intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage("com.instagram.android")
        }
        shadowPm.addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            app1Resolve
        )

        val selfResolve = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = context.packageName
                name = "com.fluxfoxus.MainActivity"
                applicationInfo = ApplicationInfo().apply {
                    packageName = context.packageName
                    flags = 0
                }
            }
        }
        shadowPm.addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            selfResolve
        )
    }

    @Test
    fun testGetInstalledAppsFiltersSelf() = runBlocking {
        val apps = repository.getInstalledApps(includeSystemApps = true)

        // Ensure self package is never returned in app selection
        assertFalse(apps.any { it.packageName == context.packageName })
        assertTrue(apps.any { it.packageName == "com.instagram.android" })
    }

    @Test
    fun testFilterApps() {
        val testApps = listOf(
            AppInfo("com.instagram.android", "Instagram", null, false),
            AppInfo("com.google.android.youtube", "YouTube", null, false),
            AppInfo("org.mozilla.firefox", "Firefox", null, false)
        )

        val filtered = repository.filterApps(testApps, "gram")
        assertEquals(1, filtered.size)
        assertEquals("Instagram", filtered.first().appName)

        val byPackage = repository.filterApps(testApps, "youtube")
        assertEquals(1, byPackage.size)
        assertEquals("YouTube", byPackage.first().appName)

        val emptyQuery = repository.filterApps(testApps, "   ")
        assertEquals(3, emptyQuery.size)
    }
}
