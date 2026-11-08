package com.yusufteker.healthtrack.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.theme.HealthTrackTheme

private val sampleWorkouts = listOf(
    WorkoutLog(
        id = "wk_1",
        userId = 1,
        date = "2026-11-06",
        rawText = "45 dakika açık havada tempolu koşu (6.2 km)",
        caloriesBurned = 340,
        durationMinutes = 45,
        aiAnalysis = "Yüksek aerobik efor. Kardiyovasküler dayanıklılık ve yağ yakımı için ideal tempoda tamamlandı."
    ),
    WorkoutLog(
        id = "wk_2",
        userId = 1,
        date = "2026-11-06",
        rawText = "30 dakika üst vücut hipertrofi (Bench press, Dumbbell fly, Triceps pushdown)",
        caloriesBurned = 220,
        durationMinutes = 30,
        aiAnalysis = "Orta-yüksek yoğunluklu direnç antrenmanı. Kas glikojen depoları aktif kullanıldı."
    )
)

private val populatedWorkoutState = WorkoutState(
    selectedDate = "2026-11-06",
    rawText = "",
    workouts = sampleWorkouts,
    totalCaloriesBurned = 560,
    totalDurationMinutes = 75,
    targetCalories = 500,
    targetDurationMinutes = 60,
    consumedCalories = 1800,
    netCalories = 1240,
    userWeightKg = 72.4
)

@Preview
@Composable
fun WorkoutScreenDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        WorkoutScreen(
            state = populatedWorkoutState,
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun WorkoutScreenLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        WorkoutScreen(
            state = populatedWorkoutState,
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun WorkoutScreenEmptyPreview() {
    HealthTrackTheme(darkTheme = true) {
        WorkoutScreen(
            state = WorkoutState(selectedDate = "2026-11-06"),
            onIntent = {}
        )
    }
}
