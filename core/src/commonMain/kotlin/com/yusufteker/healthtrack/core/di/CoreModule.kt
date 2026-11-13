package com.yusufteker.healthtrack.core.di

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.model.CheckAppVersionUseCase
import com.yusufteker.healthtrack.core.model.PendingStoreBillingClient
import com.yusufteker.healthtrack.core.model.StoreBillingClient
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DailyMealPlanEngine
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.GamificationManager
import com.yusufteker.healthtrack.core.sync.HealthReportAiGenerator
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.sync.WidgetSnapshotBuilder
import org.koin.dsl.module

val coreModule = module {
    single { Settings().also { LocalizationManager.init(it) } }
    single { TokenStorage(get()) }
    single { HealthDatabase(get()) }
    single { HealthApiClient(get()) }
    single { GamificationManager(get(), get()) }
    single { SyncEngine(get(), get(), get(), getOrNull<GamificationManager>()) }
    single { DeviceHealthSyncManager(get(), get(), get(), getOrNull<GamificationManager>()) }
    single { HealthReportAiGenerator(get()) }
    single { DailyMealPlanEngine(get()) }
    single { WidgetSnapshotBuilder(get(), get(), get()) }
    single { CheckAppVersionUseCase() }
    single<StoreBillingClient> { PendingStoreBillingClient() }
}
