package com.yusufteker.healthtrack.di

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.di.coreModule
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.HealthReportAiGenerator
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.feature.analytics.presentation.AnalyticsViewModel
import com.yusufteker.healthtrack.feature.auth.presentation.AuthViewModel
import com.yusufteker.healthtrack.feature.food.presentation.FoodViewModel
import com.yusufteker.healthtrack.feature.profile.presentation.ProfileViewModel
import com.yusufteker.healthtrack.feature.weight.presentation.WeightViewModel
import com.yusufteker.healthtrack.feature.workout.presentation.WorkoutViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val featureModule = module {
    viewModel { AuthViewModel(get(), get(), get(), get()) }
    viewModel { FoodViewModel(get(), get(), get(), get()) }
    viewModel { WorkoutViewModel(get(), get(), get(), get(), get()) }
    viewModel { WeightViewModel(get(), get(), get(), get()) }
    viewModel { AnalyticsViewModel(get(), get(), get<HealthReportAiGenerator>()) }
    viewModel {
        ProfileViewModel(
            get<HealthDatabase>(),
            get<TokenStorage>(),
            get<SyncEngine>(),
            get<Settings>(),
            get<DeviceHealthSyncManager>()
        )
    }
}

val appModules = listOf(coreModule, featureModule)

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModules)
    }
}
