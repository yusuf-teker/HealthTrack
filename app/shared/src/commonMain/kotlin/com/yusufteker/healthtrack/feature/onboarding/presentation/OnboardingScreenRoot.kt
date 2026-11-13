package com.yusufteker.healthtrack.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.utils.toLocaleDoubleOrNull
import com.yusufteker.healthtrack.feature.analytics.presentation.AnalyticsIntent
import com.yusufteker.healthtrack.feature.food.presentation.FoodIntent
import com.yusufteker.healthtrack.feature.weight.presentation.WeightIntent
import com.yusufteker.healthtrack.feature.workout.presentation.WorkoutIntent
import com.yusufteker.healthtrack.theme.appLang
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreenRoot(
    onFinish: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = appLang()
    val coroutineScope = rememberCoroutineScope()
    var state by remember(lang) { mutableStateOf(createInitialOnboardingState(lang)) }
    var activeDemoJob by remember { mutableStateOf<Job?>(null) }

    fun triggerFoodTypeSample() {
        activeDemoJob?.cancel()
        activeDemoJob = coroutineScope.launch {
            val fullText = demoFoodSamplePrompt(lang)
            state = state.copy(
                microStep = OnboardingMicroStep.FOOD_INPUT,
                foodPhase = FoodDemoPhase.TYPING,
                demoFeedbackPill = null,
                foodState = state.foodState.copy(rawText = "")
            )
            for (i in 1..fullText.length) {
                delay(18L)
                state = state.copy(
                    foodState = state.foodState.copy(rawText = fullText.substring(0, i))
                )
            }
            delay(180L)
            state = state.copy(
                foodPhase = FoodDemoPhase.READY_TO_ANALYZE,
                microStep = OnboardingMicroStep.FOOD_CALCULATE
            )
        }
    }

    fun triggerFoodAnalyze() {
        if (state.foodState.isCalculatingAi) return
        if (state.foodPhase == FoodDemoPhase.ANALYZED) {
            state = state.copy(microStep = OnboardingMicroStep.FOOD_RESULT_GAUGE)
            return
        }
        activeDemoJob?.cancel()
        activeDemoJob = coroutineScope.launch {
            state = state.copy(
                foodState = state.foodState.copy(isCalculatingAi = true)
            )
            delay(650L)
            val aiResult = demoFoodAiResult(lang)
            state = state.copy(
                foodPhase = FoodDemoPhase.REVIEW_SHEET,
                microStep = OnboardingMicroStep.FOOD_REVIEW_SHEET,
                foodState = state.foodState.copy(
                    isCalculatingAi = false,
                    isAiReviewSheetOpen = true,
                    pendingAiResult = aiResult
                )
            )
        }
    }

    fun confirmFoodReview() {
        val pending = state.foodState.pendingAiResult ?: demoFoodAiResult(lang)
        val existing = state.foodState.items
        val merged = (pending.items + existing).distinctBy { it.id }
        val totalCal = merged.sumOf { it.calories }
        val totalProtein = merged.sumOf { it.proteinG }
        val totalCarbs = merged.sumOf { it.carbsG }
        val totalFat = merged.sumOf { it.fatG }
        val totalFiber = merged.sumOf { it.fiberG }
        state = state.copy(
            foodPhase = FoodDemoPhase.ANALYZED,
            microStep = OnboardingMicroStep.FOOD_RESULT_GAUGE,
            demoFeedbackPill = trEn(
                "+285 kcal • +16.6g Protein",
                "+285 kcal • +16.6g Protein",
                lang
            ),
            foodState = state.foodState.copy(
                isCalculatingAi = false,
                isAiReviewSheetOpen = false,
                pendingAiResult = null,
                rawText = "",
                items = merged,
                totalCalories = totalCal,
                nutrientBars = buildDemoNutrientBars(
                    protein = totalProtein,
                    carbs = totalCarbs,
                    fat = totalFat,
                    fiber = totalFiber,
                    lang = lang
                ),
                summary = trEn(
                    "Gemini 3 besini otomatik analiz etti ve günlük hedeflerine işledi.",
                    "Gemini analyzed 3 items and updated your daily targets.",
                    lang
                )
            )
        )
    }

    fun triggerWorkoutTypeSample() {
        activeDemoJob?.cancel()
        activeDemoJob = coroutineScope.launch {
            val fullText = demoWorkoutSamplePrompt(lang)
            state = state.copy(
                microStep = OnboardingMicroStep.WORKOUT_INPUT,
                workoutPhase = WorkoutDemoPhase.TYPING,
                demoFeedbackPill = null,
                workoutState = state.workoutState.copy(rawText = "")
            )
            for (i in 1..fullText.length) {
                delay(18L)
                state = state.copy(
                    workoutState = state.workoutState.copy(rawText = fullText.substring(0, i))
                )
            }
            delay(180L)
            state = state.copy(
                workoutPhase = WorkoutDemoPhase.READY_TO_ANALYZE,
                microStep = OnboardingMicroStep.WORKOUT_CALCULATE
            )
        }
    }

    fun triggerWorkoutAnalyze() {
        if (state.workoutState.isCalculatingAi) return
        if (state.workoutPhase == WorkoutDemoPhase.ANALYZED) {
            state = state.copy(microStep = OnboardingMicroStep.WORKOUT_RESULT_GAUGE)
            return
        }
        activeDemoJob?.cancel()
        activeDemoJob = coroutineScope.launch {
            state = state.copy(
                workoutState = state.workoutState.copy(isCalculatingAi = true)
            )
            delay(650L)
            val newWorkout = WorkoutLog(
                id = "demo_wk_run",
                userId = 1,
                date = "2026-11-13",
                rawText = state.workoutState.rawText.ifBlank { demoWorkoutSamplePrompt(lang) },
                caloriesBurned = 360,
                durationMinutes = 45,
                aiAnalysis = trEn(
                    "Yüksek aerobik efor • Kalori bütçene +360 kcal alan açıldı.",
                    "High aerobic effort • +360 kcal added to your daily budget.",
                    lang
                )
            )
            val merged = (listOf(newWorkout) + state.workoutState.workouts).distinctBy { it.id }
            val totalBurned = merged.sumOf { it.caloriesBurned }
            val totalMinutes = merged.sumOf { it.durationMinutes }
            state = state.copy(
                workoutPhase = WorkoutDemoPhase.ANALYZED,
                microStep = OnboardingMicroStep.WORKOUT_RESULT_GAUGE,
                demoFeedbackPill = trEn(
                    "+360 kcal yakıldı • Hedef %114!",
                    "+360 kcal burned • 114% Goal!",
                    lang
                ),
                workoutState = state.workoutState.copy(
                    isCalculatingAi = false,
                    rawText = "",
                    workouts = merged,
                    allWorkouts = merged,
                    totalCaloriesBurned = totalBurned,
                    totalDurationMinutes = totalMinutes,
                    netCalories = state.workoutState.consumedCalories - totalBurned
                )
            )
        }
    }

    fun triggerWeightAddSample(weightOverrideKg: Double? = null) {
        val loggedKg = weightOverrideKg ?: 72.1
        val initialKg = state.weightState.initialWeightKg ?: 73.6
        val diff = ((loggedKg - initialKg) * 10).toInt() / 10.0
        val newEntry = WeightEntry(
            id = "dw6",
            userId = 1,
            date = "2026-11-13",
            weightKg = loggedKg,
            targetWeightKg = state.weightState.targetWeightKg,
            note = state.weightState.inputNote.ifBlank { trEn("Sabah ölçümü", "Morning weigh-in", lang) }
        )
        val merged = (state.weightState.entries.filterNot { it.id == "dw6" } + newEntry)
        state = state.copy(
            weightPhase = WeightDemoPhase.LOGGED,
            microStep = OnboardingMicroStep.WEIGHT_RESULT_CHART,
            demoFeedbackPill = trEn(
                "$loggedKg kg kaydedildi • $diff kg",
                "$loggedKg kg logged • $diff kg",
                lang
            ),
            weightState = state.weightState.copy(
                isAddDialogOpen = false,
                errorMessage = null,
                entries = merged,
                filteredEntries = merged,
                currentWeightKg = loggedKg,
                weightChangeKg = diff
            )
        )
    }

    fun triggerAnalyticsGenerate() {
        if (state.analyticsState.isGenerating) return
        if (state.analyticsPhase == AnalyticsDemoPhase.REPORT_READY) {
            state = state.copy(microStep = OnboardingMicroStep.ANALYTICS_RESULT_REPORT)
            return
        }
        activeDemoJob?.cancel()
        activeDemoJob = coroutineScope.launch {
            state = state.copy(
                analyticsState = state.analyticsState.copy(isGenerating = true)
            )
            delay(650L)
            val report = demoSampleWeeklyReport(lang)
            state = state.copy(
                analyticsPhase = AnalyticsDemoPhase.REPORT_READY,
                microStep = OnboardingMicroStep.ANALYTICS_RESULT_REPORT,
                demoFeedbackPill = trEn(
                    "Haftalık Skor: 88/100 🏆",
                    "Weekly Score: 88/100 🏆",
                    lang
                ),
                analyticsState = state.analyticsState.copy(
                    isGenerating = false,
                    currentReport = report,
                    recentReports = listOf(report)
                )
            )
        }
    }

    fun moveToNextMicroStep() {
        activeDemoJob?.cancel()
        val all = OnboardingMicroStep.entries
        val nextIdx = (state.microStep.ordinal + 1).coerceAtMost(all.lastIndex)
        val nextMicro = all[nextIdx]
        state = state.copy(
            microStep = nextMicro,
            demoFeedbackPill = null,
            foodState = state.foodState.copy(isAiReviewSheetOpen = false),
            weightState = state.weightState.copy(isAddDialogOpen = false)
        )
    }

    fun moveToPrevMicroStep() {
        activeDemoJob?.cancel()
        val all = OnboardingMicroStep.entries
        val prevIdx = (state.microStep.ordinal - 1).coerceAtLeast(0)
        val prevMicro = all[prevIdx]
        // Skip REVIEW_SHEET when going backward so we don't pop an unexpected empty sheet
        val resolvedMicro = if (prevMicro == OnboardingMicroStep.FOOD_REVIEW_SHEET) {
            OnboardingMicroStep.FOOD_CALCULATE
        } else {
            prevMicro
        }
        state = state.copy(
            microStep = resolvedMicro,
            demoFeedbackPill = null,
            foodState = state.foodState.copy(isAiReviewSheetOpen = false),
            weightState = state.weightState.copy(isAddDialogOpen = false)
        )
    }

    OnboardingScreen(
        state = state,
        onLanguageSelected = onLanguageSelected,
        onIntent = { intent ->
            when (intent) {
                is OnboardingIntent.OnNextStep -> moveToNextMicroStep()
                is OnboardingIntent.OnPreviousStep -> moveToPrevMicroStep()
                is OnboardingIntent.OnGoToStep -> {
                    activeDemoJob?.cancel()
                    state = state.copy(
                        microStep = OnboardingMicroStep.firstForStep(intent.step),
                        demoFeedbackPill = null,
                        foodState = state.foodState.copy(isAiReviewSheetOpen = false),
                        weightState = state.weightState.copy(isAddDialogOpen = false)
                    )
                }
                is OnboardingIntent.OnPrimaryBubbleAction -> {
                    when (state.microStep) {
                        OnboardingMicroStep.FOOD_INPUT -> {
                            if (state.foodState.rawText.isBlank()) {
                                triggerFoodTypeSample()
                            } else {
                                state = state.copy(
                                    foodPhase = FoodDemoPhase.READY_TO_ANALYZE,
                                    microStep = OnboardingMicroStep.FOOD_CALCULATE
                                )
                            }
                        }
                        OnboardingMicroStep.FOOD_CALCULATE -> triggerFoodAnalyze()
                        OnboardingMicroStep.FOOD_REVIEW_SHEET -> confirmFoodReview()
                        OnboardingMicroStep.FOOD_RESULT_GAUGE -> {
                            state = state.copy(
                                microStep = OnboardingMicroStep.WORKOUT_INPUT,
                                demoFeedbackPill = null
                            )
                        }
                        OnboardingMicroStep.WORKOUT_INPUT -> {
                            if (state.workoutState.rawText.isBlank()) {
                                triggerWorkoutTypeSample()
                            } else {
                                state = state.copy(
                                    workoutPhase = WorkoutDemoPhase.READY_TO_ANALYZE,
                                    microStep = OnboardingMicroStep.WORKOUT_CALCULATE
                                )
                            }
                        }
                        OnboardingMicroStep.WORKOUT_CALCULATE -> triggerWorkoutAnalyze()
                        OnboardingMicroStep.WORKOUT_RESULT_GAUGE -> {
                            state = state.copy(
                                microStep = OnboardingMicroStep.WEIGHT_ADD,
                                demoFeedbackPill = null
                            )
                        }
                        OnboardingMicroStep.WEIGHT_ADD -> triggerWeightAddSample()
                        OnboardingMicroStep.WEIGHT_RESULT_CHART -> {
                            state = state.copy(
                                microStep = OnboardingMicroStep.ANALYTICS_GENERATE,
                                demoFeedbackPill = null
                            )
                        }
                        OnboardingMicroStep.ANALYTICS_GENERATE -> triggerAnalyticsGenerate()
                        OnboardingMicroStep.ANALYTICS_RESULT_REPORT -> {
                            state = state.copy(
                                microStep = OnboardingMicroStep.READY_TO_START,
                                demoFeedbackPill = null
                            )
                        }
                        OnboardingMicroStep.READY_TO_START -> onFinish()
                    }
                }
                is OnboardingIntent.OnSkip,
                is OnboardingIntent.OnFinish -> {
                    activeDemoJob?.cancel()
                    onFinish()
                }
                is OnboardingIntent.OnTriggerFoodTypeSample -> triggerFoodTypeSample()
                is OnboardingIntent.OnTriggerFoodAnalyze -> triggerFoodAnalyze()
                is OnboardingIntent.OnFoodIntent -> {
                    when (val fi = intent.intent) {
                        is FoodIntent.OnRawTextChanged -> {
                            state = state.copy(
                                foodState = state.foodState.copy(rawText = fi.text),
                                foodPhase = if (fi.text.isNotBlank() && state.foodPhase != FoodDemoPhase.ANALYZED) {
                                    FoodDemoPhase.READY_TO_ANALYZE
                                } else if (fi.text.isBlank()) {
                                    FoodDemoPhase.IDLE_PROMPT
                                } else {
                                    state.foodPhase
                                }
                            )
                        }
                        is FoodIntent.OnCalculateAiClicked -> triggerFoodAnalyze()
                        is FoodIntent.OnConfirmAiReview -> confirmFoodReview()
                        is FoodIntent.OnDismissAiReview -> {
                            state = state.copy(
                                foodPhase = FoodDemoPhase.READY_TO_ANALYZE,
                                microStep = OnboardingMicroStep.FOOD_CALCULATE,
                                foodState = state.foodState.copy(
                                    isAiReviewSheetOpen = false,
                                    pendingAiResult = null
                                )
                            )
                        }
                        is FoodIntent.OnAddWater -> {
                            val current = state.foodState.waterEntry
                            if (current != null) {
                                val nextMl = (current.amountMl + fi.deltaMl).coerceAtLeast(0)
                                state = state.copy(
                                    foodState = state.foodState.copy(
                                        waterEntry = current.copy(amountMl = nextMl)
                                    )
                                )
                            }
                        }
                        is FoodIntent.OnRemoveItem -> {
                            val updated = state.foodState.items.filterNot { it.id == fi.itemId }
                            state = state.copy(
                                foodState = state.foodState.copy(
                                    items = updated,
                                    totalCalories = updated.sumOf { it.calories },
                                    nutrientBars = buildDemoNutrientBars(
                                        protein = updated.sumOf { it.proteinG },
                                        carbs = updated.sumOf { it.carbsG },
                                        fat = updated.sumOf { it.fatG },
                                        fiber = updated.sumOf { it.fiberG },
                                        lang = lang
                                    )
                                )
                            )
                        }
                        else -> Unit
                    }
                }
                is OnboardingIntent.OnTriggerWorkoutTypeSample -> triggerWorkoutTypeSample()
                is OnboardingIntent.OnTriggerWorkoutAnalyze -> triggerWorkoutAnalyze()
                is OnboardingIntent.OnWorkoutIntent -> {
                    when (val wi = intent.intent) {
                        is WorkoutIntent.OnRawTextChanged -> {
                            state = state.copy(
                                workoutState = state.workoutState.copy(rawText = wi.text),
                                workoutPhase = if (wi.text.isNotBlank() && state.workoutPhase != WorkoutDemoPhase.ANALYZED) {
                                    WorkoutDemoPhase.READY_TO_ANALYZE
                                } else if (wi.text.isBlank()) {
                                    WorkoutDemoPhase.IDLE_PROMPT
                                } else {
                                    state.workoutPhase
                                }
                            )
                        }
                        is WorkoutIntent.OnCalculateAiClicked -> triggerWorkoutAnalyze()
                        else -> Unit
                    }
                }
                is OnboardingIntent.OnTriggerWeightAddSample -> triggerWeightAddSample()
                is OnboardingIntent.OnWeightIntent -> {
                    when (val wi = intent.intent) {
                        is WeightIntent.OnSetAddDialogOpen -> {
                            state = state.copy(
                                weightState = state.weightState.copy(
                                    isAddDialogOpen = wi.isOpen,
                                    inputWeight = if (wi.isOpen && state.weightState.inputWeight.isBlank()) "72.1" else state.weightState.inputWeight
                                )
                            )
                        }
                        is WeightIntent.OnInputWeightChanged -> {
                            state = state.copy(
                                weightState = state.weightState.copy(inputWeight = wi.weight, errorMessage = null)
                            )
                        }
                        is WeightIntent.OnInputDateChanged -> {
                            state = state.copy(
                                weightState = state.weightState.copy(inputDate = wi.date)
                            )
                        }
                        is WeightIntent.OnInputNoteChanged -> {
                            state = state.copy(
                                weightState = state.weightState.copy(inputNote = wi.note)
                            )
                        }
                        is WeightIntent.OnPeriodSelected -> {
                            state = state.copy(
                                weightState = state.weightState.copy(selectedPeriod = wi.period)
                            )
                        }
                        is WeightIntent.OnSaveWeightClicked -> {
                            val parsed = state.weightState.inputWeight.toLocaleDoubleOrNull()
                            if (parsed != null && parsed in 20.0..350.0) {
                                triggerWeightAddSample(weightOverrideKg = parsed)
                            } else {
                                triggerWeightAddSample(weightOverrideKg = 72.1)
                            }
                        }
                        else -> Unit
                    }
                }
                is OnboardingIntent.OnTriggerAnalyticsGenerate -> triggerAnalyticsGenerate()
                is OnboardingIntent.OnAnalyticsIntent -> {
                    when (val ai = intent.intent) {
                        is AnalyticsIntent.OnPeriodTypeChanged -> {
                            state = state.copy(
                                analyticsState = state.analyticsState.copy(periodType = ai.periodType)
                            )
                        }
                        is AnalyticsIntent.OnGenerateReportClicked -> triggerAnalyticsGenerate()
                        else -> Unit
                    }
                }
            }
        },
        modifier = modifier
    )
}
