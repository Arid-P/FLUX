package com.fluxfoxus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.datastore.UserPreferencesRepository
import com.fluxfoxus.core.designsystem.theme.FluxFoxusTheme
import com.fluxfoxus.navigation.FluxFoxusNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = FluxFoxusDatabase.getDatabase(applicationContext)
        val userPrefs = UserPreferencesRepository(applicationContext)

        // Schedule periodic midnight streak evaluation
        com.fluxfoxus.core.work.MidnightStreakWorker.schedule(applicationContext)

        setContent {
            FluxFoxusTheme {
                val navController = rememberNavController()
                FluxFoxusNavHost(
                    navController = navController,
                    db = db,
                    userPrefs = userPrefs
                )
            }
        }
    }
}
