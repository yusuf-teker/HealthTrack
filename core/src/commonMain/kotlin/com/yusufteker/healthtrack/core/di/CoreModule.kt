package com.yusufteker.healthtrack.core.di

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.HealthReportAiGenerator
import com.yusufteker.healthtrack.core.sync.SyncEngine
import org.koin.dsl.module

val coreModule = module {
    single { Settings() }
    single { TokenStorage(get()) }
    single { HealthDatabase(get()) }
    single { HealthApiClient(get()) }
    single { SyncEngine(get(), get(), get()) }
    single { DeviceHealthSyncManager(get(), get(), get()) }
    single { HealthReportAiGenerator(get()) }
}
