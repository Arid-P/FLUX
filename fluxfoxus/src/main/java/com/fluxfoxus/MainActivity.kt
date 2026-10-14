package com.fluxfoxus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.datastore.UserPreferencesRepository
import com.fluxfoxus.core.designsystem.theme.FluxFoxusTheme
import com.fluxfoxus.core.permission.PermissionChecker
import com.fluxfoxus.core.repository.InstalledAppsRepository
import com.fluxfoxus.core.repository.UsageStatsRepository
import com.fluxfoxus.navigation.FluxFoxusNavHost
import com.fluxfoxus.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce full edge-to-edge drawing
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val db = FluxFoxusDatabase.getDatabase(applicationContext)
        val userPrefs = UserPreferencesRepository(applicationContext)
        val installedAppsRepo = InstalledAppsRepository(applicationContext)
        val usageStatsRepo = UsageStatsRepository(applicationContext, db)

        // Schedule periodic midnight streak evaluation
        com.fluxfoxus.core.work.MidnightStreakWorker.schedule(applicationContext)

        // Gatekeeper check: Initial routing on launch
        val isOnboardingCompleted = runBlocking {
            try {
                userPrefs.onboardingCompleted.first()
            } catch (_: Exception) {
                false
            }
        }
        val isMandatoryGranted = PermissionChecker.areMandatoryGranted(applicationContext)

        val startDestination = if (!isOnboardingCompleted || !isMandatoryGranted) {
            Screen.Onboarding.route
        } else {
            Screen.Home.route
        }

        setContent {
            FluxFoxusTheme {
                val navController = rememberNavController()
                FluxFoxusNavHost(
                    navController = navController,
                    db = db,
                    userPrefs = userPrefs,
                    installedAppsRepo = installedAppsRepo,
                    usageStatsRepo = usageStatsRepo,
                    startDestination = startDestination
                )
            }
        }
    }
}
