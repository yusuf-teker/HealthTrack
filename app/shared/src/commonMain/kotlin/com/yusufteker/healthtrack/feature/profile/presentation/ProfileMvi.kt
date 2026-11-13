package com.yusufteker.healthtrack.feature.profile.presentation

import com.yusufteker.healthtrack.core.model.DeviceHealthSnapshot
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency
import com.yusufteker.healthtrack.core.model.calculateProteinTarget
import com.yusufteker.healthtrack.theme.AppThemeColor

data class ProfileState(
    val userId: Int = 0,
    val name: String = "",
    val username: String = "",
    val weightKg: Double = 72.0,
    val targetWeightKg: Double = 70.0,
    val heightCm: Double = 175.0,
    val age: Int = 26,
    val gender: String = "MALE", // "MALE" or "FEMALE"
    val fitnessGoal: FitnessGoal = FitnessGoal.MAINTAIN,
    val nutritionGoal: NutritionGoal = NutritionGoal(id = "default", userId = 0),
    val workoutReminderFrequency: WorkoutReminderFrequency = WorkoutReminderFrequency.SMART_CADENCE,
    val weightReminderFrequency: WeightReminderFrequency = WeightReminderFrequency.EVERY_3_DAYS,
    val isDarkMode: Boolean = true,
    val themeAccent: AppThemeColor = AppThemeColor.OCEAN,
    val customGeminiApiKey: String = "",
    val latestWeightDate: String? = null,
    val isSaving: Boolean = false,
    val isSyncing: Boolean = false,
    val isDeviceSyncing: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val lastDeviceSyncSummary: String? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null
) {
    val targetProteinG: Double
        get() = calculateProteinTarget(weightKg, fitnessGoal)

    val bmi: Double
        get() {
            val hM = heightCm / 100.0
            return if (hM > 0 && weightKg > 0) {
                kotlin.math.round((weightKg / (hM * hM)) * 10) / 10.0
            } else 0.0
        }

    val bmiCategory: String
        get() = when {
            bmi <= 0.0 -> "-"
            bmi < 18.5 -> "Zayıf"
            bmi < 25.0 -> "İdeal / Sağlıklı"
            bmi < 30.0 -> "Fazla Kilolu"
            else -> "Obezite"
        }
}

sealed interface ProfileIntent {
    data class OnWeightChanged(val weight: Double) : ProfileIntent
    data class OnTargetWeightChanged(val target: Double) : ProfileIntent
    data class OnHeightChanged(val height: Double) : ProfileIntent
    data class OnAgeChanged(val age: Int) : ProfileIntent
    data class OnGenderChanged(val gender: String) : ProfileIntent
    data class OnFitnessGoalChanged(val goal: FitnessGoal) : ProfileIntent
    data class OnSaveNutritionGoal(val goal: NutritionGoal, val fitnessGoal: FitnessGoal) : ProfileIntent
    data class OnWorkoutReminderFrequencyChanged(val frequency: WorkoutReminderFrequency) : ProfileIntent
    data class OnWeightReminderFrequencyChanged(val frequency: WeightReminderFrequency) : ProfileIntent
    data class OnThemeModeChanged(val isDark: Boolean) : ProfileIntent
    data class OnThemeAccentChanged(val accent: AppThemeColor) : ProfileIntent
    data class OnSaveCustomGeminiApiKey(val apiKey: String) : ProfileIntent
    data object OnClearCustomGeminiApiKey : ProfileIntent
    data object OnSyncClicked : ProfileIntent
    data object OnDeviceSyncStarted : ProfileIntent
    data class OnDeviceSnapshotReceived(val result: Result<DeviceHealthSnapshot>) : ProfileIntent
    data object OnLogoutClicked : ProfileIntent
    data object OnDeleteAccountConfirmed : ProfileIntent
    data object OnSaveProfileClicked : ProfileIntent
    data object OnScreenAppeared : ProfileIntent
    data object OnDismissInfoMessage : ProfileIntent
    data object OnDismissErrorMessage : ProfileIntent
}

sealed interface ProfileEffect {
    data class ShowToast(val message: String) : ProfileEffect
    data object NavigateBack : ProfileEffect
    data object LoggedOut : ProfileEffect
}

