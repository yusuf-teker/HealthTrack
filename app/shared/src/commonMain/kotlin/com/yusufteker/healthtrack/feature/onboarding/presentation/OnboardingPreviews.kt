package com.yusufteker.healthtrack.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.theme.HealthTrackTheme

@Preview
@Composable
fun OnboardingFoodInputDarkPreview() {
    HealthTrackTheme(darkTheme = true, language = AppLanguage.TR) {
        OnboardingScreen(
            state = createInitialOnboardingState(AppLanguage.TR),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun OnboardingFoodInputLightPreview() {
    HealthTrackTheme(darkTheme = false, language = AppLanguage.TR) {
        OnboardingScreen(
            state = createInitialOnboardingState(AppLanguage.TR),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun OnboardingFoodResultDarkPreview() {
    val initial = createInitialOnboardingState(AppLanguage.TR)
    val added = demoFoodAnalyzedItems(AppLanguage.TR)
    val merged = added + initial.foodState.items
    HealthTrackTheme(darkTheme = true, language = AppLanguage.TR) {
        OnboardingScreen(
            state = initial.copy(
                microStep = OnboardingMicroStep.FOOD_RESULT_GAUGE,
                foodPhase = FoodDemoPhase.ANALYZED,
                demoFeedbackPill = "+285 kcal • +16.6g Protein",
                foodState = initial.foodState.copy(
                    items = merged,
                    totalCalories = merged.sumOf { it.calories },
                    nutrientBars = buildDemoNutrientBars(
                        protein = merged.sumOf { it.proteinG },
                        carbs = merged.sumOf { it.carbsG },
                        fat = merged.sumOf { it.fatG },
                        fiber = merged.sumOf { it.fiberG },
                        lang = AppLanguage.TR
                    )
                )
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun OnboardingWorkoutStepDarkPreview() {
    HealthTrackTheme(darkTheme = true, language = AppLanguage.TR) {
        OnboardingScreen(
            state = createInitialOnboardingState(AppLanguage.TR).copy(
                microStep = OnboardingMicroStep.WORKOUT_INPUT
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun OnboardingReadyStepDarkPreview() {
    HealthTrackTheme(darkTheme = true, language = AppLanguage.TR) {
        OnboardingScreen(
            state = createInitialOnboardingState(AppLanguage.TR).copy(
                microStep = OnboardingMicroStep.READY_TO_START
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun OnboardingReadyStepLightPreview() {
    HealthTrackTheme(darkTheme = false, language = AppLanguage.EN) {
        OnboardingScreen(
            state = createInitialOnboardingState(AppLanguage.EN).copy(
                microStep = OnboardingMicroStep.READY_TO_START
            ),
            onIntent = {}
        )
    }
}
