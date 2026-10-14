package com.fluxfoxus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fluxfoxus.core.designsystem.theme.FluxFoxusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FluxFoxusTheme {
                // Application root entry point
            }
        }
    }
}
