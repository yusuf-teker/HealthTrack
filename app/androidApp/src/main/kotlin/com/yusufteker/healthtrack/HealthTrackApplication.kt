package com.yusufteker.healthtrack

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.yusufteker.healthtrack.core.database.HealthAndroidDbContext
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.network.HealthTrackRuntimeConfig
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatformTools

/**
 * HealthTrack Android Application class.
 *
 * Keeps startup instant on the Main Thread:
 * - Registers Koin modules once (lazy definitions only)
 * - Pre-warms SQLite (`libsqliteJni.so` + single-query load) and TokenStorage off Main Thread
 * - Sets up notification channels and FCM token asynchronously
 */
class HealthTrackApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        HealthTrackRuntimeConfig.isDebugLoggingEnabled = BuildConfig.DEBUG
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }
        HealthAndroidDbContext.init(this)
        initKoin()

        appScope.launch {
            val koin = KoinPlatformTools.defaultContext().getOrNull() ?: return@launch
            runCatching { koin.get<TokenStorage>() }
            runCatching { koin.get<HealthDatabase>() }
            // Defer non-critical notification channel & GMS/FCM token refresh until after first frame
            delay(1500L)
            runCatching {
                (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.let {
                    HealthTrackFirebaseMessagingService.ensurePushChannel(it)
                }
            }
            runCatching {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        HealthTrackFirebaseMessagingService.saveFcmToken(this@HealthTrackApplication, token)
                    }
                }
            }
        }
    }
}
