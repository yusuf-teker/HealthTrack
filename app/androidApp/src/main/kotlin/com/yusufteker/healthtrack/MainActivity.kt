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
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.detectSystemLanguage
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.utils.WidgetDeepLinkBus
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

        val initialTab = resolveTabFromIntent(intent)
        dispatchDeepLinkIfPresent(intent)

        setContent {
            App(initialTab = initialTab)
        }

        if (isHealthRationaleIntent) {
            val savedCode = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
                .getString(LocalizationManager.KEY_APP_LANGUAGE, null)
            val lang = AppLanguage.fromCode(savedCode) ?: detectSystemLanguage()

            AlertDialog.Builder(this)
                .setTitle(trEn("Health Connect Veri Kullanımı", "Health Connect Data Usage", lang))
                .setMessage(
                    trEn(
                        "HealthTrack; kilo, vücut yağı, kas kütlesi, adım, mesafe, egzersiz, yakılan kalori ve kalp atış hızı verilerinizi yalnızca uygulama içi sağlık analizlerinizi sunmak ve cihazlarınız arasında senkronize etmek amacıyla okur. Verileriniz üçüncü taraflara satılmaz ve reklam amacıyla kullanılmaz.",
                        "HealthTrack reads your weight, body fat, muscle mass, steps, distance, workouts, burned calories, and heart rate data solely to provide in-app health insights and sync across your devices. Your data is never sold to third parties or used for advertising.",
                        lang
                    )
                )
                .setPositiveButton(trEn("Gizlilik Politikası", "Privacy Policy", lang)) { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pulse-7b4z.onrender.com/privacy")))
                }
                .setNegativeButton(trEn("Kapat", "Close", lang), null)
                .show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchDeepLinkIfPresent(intent)
    }

    private fun resolveTabFromIntent(intent: Intent?): HealthTab {
        val data = intent?.data
        val tabParam = data?.getQueryParameter("tab")?.uppercase()
            ?: intent?.getStringExtra("open_tab")?.uppercase()
        return when (tabParam) {
            "WORKOUT" -> HealthTab.WORKOUT
            "WEIGHT" -> HealthTab.WEIGHT
            "ANALYTICS" -> HealthTab.ANALYTICS
            else -> HealthTab.FOOD
        }
    }

    private fun dispatchDeepLinkIfPresent(intent: Intent?) {
        val data = intent?.dataString
        if (!data.isNullOrBlank() && data.startsWith("healthtrack://", ignoreCase = true)) {
            WidgetDeepLinkBus.emitFromUri(data)
            return
        }
        val tab = intent?.getStringExtra("open_tab")
        if (!tab.isNullOrBlank()) {
            WidgetDeepLinkBus.emit(tabName = tab, action = intent.getStringExtra("open_action"))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
