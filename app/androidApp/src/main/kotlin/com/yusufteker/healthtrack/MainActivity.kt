package com.yusufteker.healthtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        val targetTabStr = intent?.getStringExtra("open_tab")
        val initialTab = when (targetTabStr) {
            "WORKOUT" -> HealthTab.WORKOUT
            "WEIGHT" -> HealthTab.WEIGHT
            "ANALYTICS" -> HealthTab.ANALYTICS
            else -> HealthTab.FOOD
        }

        setContent {
            App(initialTab = initialTab)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}