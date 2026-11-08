package com.yusufteker.healthtrack.feature.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.theme.AppThemeColor
import com.yusufteker.healthtrack.theme.HealthTrackTheme

private val populatedProfileState = ProfileState(
    userId = 1,
    name = "Yusuf Teker",
    username = "yusufteker",
    weightKg = 72.4,
    targetWeightKg = 70.0,
    heightCm = 178.0,
    age = 26,
    gender = "MALE",
    fitnessGoal = FitnessGoal.BUILD_MUSCLE,
    nutritionGoal = NutritionGoal(
        id = "goal_1",
        userId = 1,
        calories = 2200,
        proteinG = 145.0,
        carbsG = 230.0,
        fatG = 65.0,
        fiberG = 30.0
    ),
    isDarkMode = true,
    themeAccent = AppThemeColor.TEAL,
    latestWeightDate = "06.11.2026"
)

@Preview
@Composable
fun ProfileScreenDarkPreview() {
    HealthTrackTheme(darkTheme = true, themeAccent = AppThemeColor.TEAL) {
        ProfileScreen(
            state = populatedProfileState,
            onIntent = {},
            onBack = {},
            onLogout = {}
        )
    }
}

@Preview
@Composable
fun ProfileScreenLightPreview() {
    HealthTrackTheme(darkTheme = false, themeAccent = AppThemeColor.TEAL) {
        ProfileScreen(
            state = populatedProfileState.copy(isDarkMode = false),
            onIntent = {},
            onBack = {},
            onLogout = {}
        )
    }
}

@Preview
@Composable
fun ProfileScreenEmeraldPreview() {
    HealthTrackTheme(darkTheme = true, themeAccent = AppThemeColor.EMERALD) {
        ProfileScreen(
            state = populatedProfileState.copy(themeAccent = AppThemeColor.EMERALD),
            onIntent = {},
            onBack = {},
            onLogout = {}
        )
    }
}
