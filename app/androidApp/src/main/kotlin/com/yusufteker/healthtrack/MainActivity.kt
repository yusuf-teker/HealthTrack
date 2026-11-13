package com.yusufteker.healthtrack

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
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

        val isHealthRationaleIntent = intent?.action == "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" ||
            intent?.action == "android.intent.action.VIEW_PERMISSION_USAGE"

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

        if (isHealthRationaleIntent) {
            AlertDialog.Builder(this)
                .setTitle("Health Connect Veri Kullanımı")
                .setMessage(
                    "HealthTrack; kilo, vücut yağı, kas kütlesi, adım, mesafe, egzersiz, yakılan kalori ve kalp atış hızı verilerinizi yalnızca uygulama içi sağlık analizlerinizi sunmak ve cihazlarınız arasında senkronize etmek amacıyla okur. Verileriniz üçüncü taraflara satılmaz ve reklam amacıyla kullanılmaz."
                )
                .setPositiveButton("Gizlilik Politikası") { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pulse-7b4z.onrender.com/privacy")))
                }
                .setNegativeButton("Kapat", null)
                .show()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
