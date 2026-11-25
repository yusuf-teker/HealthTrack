package com.yusufteker.healthtrack

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.huawei.agconnect.AGConnectInstance
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.push.HmsMessaging
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Runtime GMS / HMS detector and push token initializer.
 *
 * - On Google Play devices (GMS): uses Firebase Cloud Messaging (FCM).
 * - On Huawei HMS-only devices (AppGallery without GMS): uses Huawei Push Kit (HCM)
 *   when `agconnect-services.json` is bundled in assets.
 * - Never crashes if either GMS or HMS is absent or unconfigured.
 */
object MobileServicesHelper {

    fun isGmsAvailable(context: Context): Boolean = runCatching {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }.getOrDefault(false)

    fun isHmsAvailable(context: Context): Boolean = runCatching {
        HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context) == com.huawei.hms.api.ConnectionResult.SUCCESS
    }.getOrDefault(false)

    fun hasAgConnectConfig(context: Context): Boolean = runCatching {
        context.assets.open("agconnect-services.json").use { true }
    }.getOrDefault(false)

    fun initializeAgConnectIfPresent(context: Context) {
        if (!hasAgConnectConfig(context)) return
        runCatching {
            if (AGConnectInstance.getInstance() == null) {
                AGConnectInstance.initialize(context.applicationContext)
            }
        }.onFailure { err ->
            Napier.w("AGConnect init skipped: ${err.message}", tag = "HMS")
        }
    }

    fun refreshPushToken(context: Context, scope: CoroutineScope) {
        val appContext = context.applicationContext
        if (isGmsAvailable(appContext)) {
            runCatching {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        if (!token.isNullOrBlank()) {
                            HealthTrackFirebaseMessagingService.saveFcmToken(appContext, token)
                        }
                    }
                    .addOnFailureListener { err ->
                        Napier.w("FCM token fetch failed: ${err.message}", tag = "FCM")
                    }
            }
            return
        }

        if (isHmsAvailable(appContext) && hasAgConnectConfig(appContext)) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    initializeAgConnectIfPresent(appContext)
                    val options = AGConnectOptionsBuilder().build(appContext)
                    val appId = options.getString("client/app_id")
                    if (!appId.isNullOrBlank()) {
                        val hmsToken = HmsInstanceId.getInstance(appContext)
                            .getToken(appId, HmsMessaging.DEFAULT_TOKEN_SCOPE)
                        if (!hmsToken.isNullOrBlank()) {
                            Napier.d("HMS Push token fetched", tag = "HMS")
                            HealthTrackFirebaseMessagingService.saveFcmToken(appContext, hmsToken)
                        }
                    }
                }.onFailure { err ->
                    Napier.w("HMS token fetch failed: ${err.message}", tag = "HMS")
                }
            }
        }
    }
}
