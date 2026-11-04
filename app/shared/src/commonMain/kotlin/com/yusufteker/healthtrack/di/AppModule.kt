package com.yusufteker.healthtrack.di

import com.yusufteker.healthtrack.core.di.coreModule
import com.yusufteker.healthtrack.feature.auth.presentation.AuthViewModel
import com.yusufteker.healthtrack.feature.food.presentation.FoodViewModel
import com.yusufteker.healthtrack.feature.health.presentation.AppointmentViewModel
import com.yusufteker.healthtrack.feature.weight.presentation.WeightViewModel
import com.yusufteker.healthtrack.feature.workout.presentation.WorkoutViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val featureModule = module {
    viewModel { AuthViewModel(get(), get(), get()) }
    viewModel { FoodViewModel(get(), get(), get(), get()) }
    viewModel { WorkoutViewModel(get(), get(), get(), get()) }
    viewModel { WeightViewModel(get(), get(), get()) }
    viewModel { AppointmentViewModel(get(), get(), get(), get()) }
}

val appModules = listOf(coreModule, featureModule)

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModules)
    }
}
