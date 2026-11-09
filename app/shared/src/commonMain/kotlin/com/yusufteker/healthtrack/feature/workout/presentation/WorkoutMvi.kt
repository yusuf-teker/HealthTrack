package com.yusufteker.healthtrack.feature.workout.presentation

import com.yusufteker.healthtrack.core.model.DailyStepTelemetry
import com.yusufteker.healthtrack.core.model.DeviceHealthSnapshot
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.utils.getTodayIso

data class WorkoutState(
    val selectedDate: String = getTodayIso(),
    val rawText: String = "",
    val manualCalories: String = "",
    val manualDuration: String = "",
    val workouts: List<WorkoutLog> = emptyList(),
    val dailySteps: DailyStepTelemetry? = null,
    val allWorkouts: List<WorkoutLog> = emptyList(),
    val totalCaloriesBurned: Int = 0,
    val totalDurationMinutes: Int = 0,
    val targetCalories: Int = 300,
    val targetDurationMinutes: Int = 45,
    val consumedCalories: Int = 0, // From Food
    val netCalories: Int = 0, // consumed - burned
    val isCalculatingAi: Boolean = false,
    val isSyncingWatch: Boolean = false,
    val userWeightKg: Double = 75.0,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    val calorieProgress: Float
        get() = if (targetCalories > 0) (totalCaloriesBurned.toFloat() / targetCalories).coerceIn(0f, 1f) else 0f

    val durationProgress: Float
        get() = if (targetDurationMinutes > 0) (totalDurationMinutes.toFloat() / targetDurationMinutes).coerceIn(0f, 1f) else 0f
}

sealed interface WorkoutIntent {
    data class OnDateChanged(val newDate: String) : WorkoutIntent
    data class OnRawTextChanged(val text: String) : WorkoutIntent
    data class OnManualCaloriesChanged(val value: String) : WorkoutIntent
    data class OnManualDurationChanged(val value: String) : WorkoutIntent
    data class OnTargetCaloriesChanged(val value: Int) : WorkoutIntent
    data object OnCalculateAiClicked : WorkoutIntent
    data object OnSaveManualClicked : WorkoutIntent
    data object OnWatchSyncStarted : WorkoutIntent
    data class OnWatchSnapshotReceived(val result: Result<DeviceHealthSnapshot>) : WorkoutIntent
    data class OnDeleteWorkout(val id: String) : WorkoutIntent
    data object OnDismissErrorMessage : WorkoutIntent
    data object OnDismissInfoMessage : WorkoutIntent
}

sealed interface WorkoutEffect {
    data class ShowToast(val message: String) : WorkoutEffect
}
