package com.yusufteker.healthtrack.feature.onboarding.presentation

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.AnalysisPeriodType
import com.yusufteker.healthtrack.core.model.DailyStepTelemetry
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.HealthAiReport
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.WaterEntry
import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.network.FoodAiResultDto
import com.yusufteker.healthtrack.feature.analytics.presentation.AnalyticsIntent
import com.yusufteker.healthtrack.feature.analytics.presentation.AnalyticsState
import com.yusufteker.healthtrack.feature.food.presentation.FoodIntent
import com.yusufteker.healthtrack.feature.food.presentation.FoodState
import com.yusufteker.healthtrack.feature.weight.presentation.WeightIntent
import com.yusufteker.healthtrack.feature.weight.presentation.WeightState
import com.yusufteker.healthtrack.feature.workout.presentation.WorkoutIntent
import com.yusufteker.healthtrack.feature.workout.presentation.WorkoutState
import com.yusufteker.healthtrack.ui.components.SpotlightTargetId

enum class OnboardingStep(
    val badgeTr: String,
    val badgeEn: String
) {
    FOOD_AI_DEMO("1/4 • BESLENME", "1/4 • NUTRITION"),
    WORKOUT_SYNC_DEMO("2/4 • SPOR & DENGE", "2/4 • WORKOUT & BALANCE"),
    WEIGHT_TREND_DEMO("3/4 • AKILLI KİLO", "3/4 • SMART WEIGHT"),
    ANALYTICS_AI_DEMO("4/4 • AI KOÇ & RAPOR", "4/4 • AI COACH & REPORT"),
    READY_TO_START("HAZIRSIN", "YOU'RE READY");

    fun badge(lang: AppLanguage): String = trEn(badgeTr, badgeEn, lang)
}

enum class OnboardingMicroStep(
    val parentStep: OnboardingStep,
    val targetId: SpotlightTargetId?,
    val preferTopScroll: Boolean = false
) {
    // 1. Beslenme (Food)
    FOOD_INPUT(OnboardingStep.FOOD_AI_DEMO, SpotlightTargetId.FOOD_INPUT_FIELD),
    FOOD_CALCULATE(OnboardingStep.FOOD_AI_DEMO, SpotlightTargetId.FOOD_CALCULATE_BUTTON),
    FOOD_REVIEW_SHEET(OnboardingStep.FOOD_AI_DEMO, SpotlightTargetId.FOOD_REVIEW_CONFIRM),
    FOOD_RESULT_GAUGE(OnboardingStep.FOOD_AI_DEMO, SpotlightTargetId.FOOD_CALORIE_GAUGE, preferTopScroll = true),

    // 2. Egzersiz (Workout)
    WORKOUT_INPUT(OnboardingStep.WORKOUT_SYNC_DEMO, SpotlightTargetId.WORKOUT_INPUT_FIELD),
    WORKOUT_CALCULATE(OnboardingStep.WORKOUT_SYNC_DEMO, SpotlightTargetId.WORKOUT_CALCULATE_BUTTON),
    WORKOUT_RESULT_GAUGE(OnboardingStep.WORKOUT_SYNC_DEMO, SpotlightTargetId.WORKOUT_HERO_GAUGE, preferTopScroll = true),

    // 3. Kilo (Weight)
    WEIGHT_ADD(OnboardingStep.WEIGHT_TREND_DEMO, SpotlightTargetId.WEIGHT_ADD_BUTTON, preferTopScroll = true),
    WEIGHT_RESULT_CHART(OnboardingStep.WEIGHT_TREND_DEMO, SpotlightTargetId.WEIGHT_CHART_CARD),

    // 4. Analiz & Rapor (Analytics)
    ANALYTICS_GENERATE(OnboardingStep.ANALYTICS_AI_DEMO, SpotlightTargetId.ANALYTICS_GENERATE_BUTTON),
    ANALYTICS_RESULT_REPORT(OnboardingStep.ANALYTICS_AI_DEMO, SpotlightTargetId.ANALYTICS_REPORT_CARD, preferTopScroll = true),

    // 5. Hazırsın
    READY_TO_START(OnboardingStep.READY_TO_START, null);

    companion object {
        fun firstForStep(step: OnboardingStep): OnboardingMicroStep = when (step) {
            OnboardingStep.FOOD_AI_DEMO -> FOOD_INPUT
            OnboardingStep.WORKOUT_SYNC_DEMO -> WORKOUT_INPUT
            OnboardingStep.WEIGHT_TREND_DEMO -> WEIGHT_ADD
            OnboardingStep.ANALYTICS_AI_DEMO -> ANALYTICS_GENERATE
            OnboardingStep.READY_TO_START -> READY_TO_START
        }
    }
}

enum class FoodDemoPhase {
    IDLE_PROMPT,
    TYPING,
    READY_TO_ANALYZE,
    REVIEW_SHEET,
    ANALYZED
}

enum class WorkoutDemoPhase {
    IDLE_PROMPT,
    TYPING,
    READY_TO_ANALYZE,
    ANALYZED
}

enum class WeightDemoPhase {
    IDLE_PROMPT,
    LOGGED
}

enum class AnalyticsDemoPhase {
    IDLE_PROMPT,
    REPORT_READY
}

data class OnboardingState(
    val microStep: OnboardingMicroStep = OnboardingMicroStep.FOOD_INPUT,
    val foodPhase: FoodDemoPhase = FoodDemoPhase.IDLE_PROMPT,
    val foodState: FoodState = createInitialDemoFoodState(AppLanguage.TR),
    val workoutPhase: WorkoutDemoPhase = WorkoutDemoPhase.IDLE_PROMPT,
    val workoutState: WorkoutState = createInitialDemoWorkoutState(AppLanguage.TR),
    val weightPhase: WeightDemoPhase = WeightDemoPhase.IDLE_PROMPT,
    val weightState: WeightState = createInitialDemoWeightState(AppLanguage.TR),
    val analyticsPhase: AnalyticsDemoPhase = AnalyticsDemoPhase.IDLE_PROMPT,
    val analyticsState: AnalyticsState = createInitialDemoAnalyticsState(AppLanguage.TR),
    val demoFeedbackPill: String? = null
) {
    val currentStep: OnboardingStep
        get() = microStep.parentStep
}

sealed interface OnboardingIntent {
    data object OnNextStep : OnboardingIntent
    data object OnPreviousStep : OnboardingIntent
    data class OnGoToStep(val step: OnboardingStep) : OnboardingIntent
    data object OnPrimaryBubbleAction : OnboardingIntent
    data object OnSkip : OnboardingIntent
    data object OnFinish : OnboardingIntent

    // Food interactive demo
    data object OnTriggerFoodTypeSample : OnboardingIntent
    data object OnTriggerFoodAnalyze : OnboardingIntent
    data class OnFoodIntent(val intent: FoodIntent) : OnboardingIntent

    // Workout interactive demo
    data object OnTriggerWorkoutTypeSample : OnboardingIntent
    data object OnTriggerWorkoutAnalyze : OnboardingIntent
    data class OnWorkoutIntent(val intent: WorkoutIntent) : OnboardingIntent

    // Weight interactive demo
    data object OnTriggerWeightAddSample : OnboardingIntent
    data class OnWeightIntent(val intent: WeightIntent) : OnboardingIntent

    // Analytics interactive demo
    data object OnTriggerAnalyticsGenerate : OnboardingIntent
    data class OnAnalyticsIntent(val intent: AnalyticsIntent) : OnboardingIntent
}

fun demoFoodSamplePrompt(lang: AppLanguage): String = trEn(
    "2 haşlanmış yumurta, 1 dilim tam buğday ekmeği, 5 zeytin",
    "2 boiled eggs, 1 slice whole wheat bread, 5 olives",
    lang
)

fun demoFoodAnalyzedItems(lang: AppLanguage): List<FoodItem> = listOf(
    FoodItem(
        id = "demo_food_egg",
        name = trEn("Haşlanmış Yumurta", "Boiled Eggs", lang),
        formattedPortion = trEn("2 Adet", "2 Eggs", lang),
        calories = 155,
        proteinG = 13.0,
        carbsG = 1.1,
        fatG = 10.6,
        fiberG = 0.0
    ),
    FoodItem(
        id = "demo_food_bread",
        name = trEn("Tam Buğday Ekmeği", "Whole Wheat Bread", lang),
        formattedPortion = trEn("1 Dilim (35g)", "1 Slice (35g)", lang),
        calories = 85,
        proteinG = 3.2,
        carbsG = 15.8,
        fatG = 1.1,
        fiberG = 2.4
    ),
    FoodItem(
        id = "demo_food_olives",
        name = trEn("Siyah Zeytin", "Black Olives", lang),
        formattedPortion = trEn("5 Adet", "5 Pieces", lang),
        calories = 45,
        proteinG = 0.4,
        carbsG = 0.8,
        fatG = 4.5,
        fiberG = 0.6
    )
)

fun demoFoodAiResult(lang: AppLanguage): FoodAiResultDto {
    val items = demoFoodAnalyzedItems(lang)
    return FoodAiResultDto(
        summary = trEn(
            "Kahvaltı öğünün 3 besin halinde analiz edildi: dengeli protein ve sağlıklı yağ.",
            "Breakfast analyzed into 3 items: balanced protein and healthy fats.",
            lang
        ),
        totalCalories = items.sumOf { it.calories },
        items = items
    )
}

fun demoWorkoutSamplePrompt(lang: AppLanguage): String = trEn(
    "45 dk açık havada tempolu koşu (6.2 km)",
    "45 min outdoor tempo run (6.2 km)",
    lang
)

fun createInitialDemoFoodState(lang: AppLanguage): FoodState {
    val initialItems = listOf(
        FoodItem(
            id = "demo_init_1",
            name = trEn("Yulaf Ezmesi & Muz", "Oatmeal & Banana", lang),
            formattedPortion = trEn("1 Kase (80g)", "1 Bowl (80g)", lang),
            calories = 380,
            proteinG = 12.5,
            carbsG = 68.0,
            fatG = 6.0,
            fiberG = 9.4
        )
    )
    return FoodState(
        selectedDate = "2026-11-13",
        rawText = "",
        items = initialItems,
        totalCalories = 380,
        summary = trEn(
            "Güne harika bir başlangıç! Öğünlerini yazarak halkayı doldur.",
            "Great start to the day! Log meals to fill your ring.",
            lang
        ),
        nutritionGoal = NutritionGoal(
            id = "demo_goal",
            userId = 1,
            calories = 2100,
            proteinG = 140.0,
            carbsG = 220.0,
            fatG = 65.0,
            fiberG = 30.0
        ),
        fitnessGoal = FitnessGoal.BUILD_MUSCLE,
        selectedDateWeightKg = 72.4,
        isWeightExactForDate = true,
        effectiveProteinTarget = 140.0,
        nutrientBars = buildDemoNutrientBars(
            protein = 12.5,
            carbs = 68.0,
            fat = 6.0,
            fiber = 9.4,
            lang = lang
        ),
        latestWeightKg = 72.4,
        weightDifferenceKg = -0.4,
        recentWeights = listOf(73.4, 73.1, 72.8, 72.6, 72.4),
        frequentFoods = initialItems,
        waterEntry = WaterEntry(date = "2026-11-13", userId = 1, amountMl = 1250, targetMl = 2500),
        dynamicWaterTargetMl = 2500
    )
}

fun buildDemoNutrientBars(
    protein: Double,
    carbs: Double,
    fat: Double,
    fiber: Double,
    lang: AppLanguage
): List<NutrientProgress> = listOf(
    NutrientProgress(key = "protein", label = trEn("Protein", "Protein", lang), current = protein, target = 140.0, unit = "g", colorHex = "#0df2a6"),
    NutrientProgress(key = "carbs", label = trEn("Karbonhidrat", "Carbs", lang), current = carbs, target = 220.0, unit = "g", colorHex = "#06b6d4"),
    NutrientProgress(key = "fat", label = trEn("Yağ", "Fat", lang), current = fat, target = 65.0, unit = "g", colorHex = "#f59e0b"),
    NutrientProgress(key = "fiber", label = trEn("Lif", "Fiber", lang), current = fiber, target = 30.0, unit = "g", colorHex = "#10b981")
)

fun createInitialDemoWorkoutState(lang: AppLanguage): WorkoutState {
    val initialWorkout = WorkoutLog(
        id = "demo_wk_steps",
        userId = 1,
        date = "2026-11-13",
        rawText = trEn("6,420 Adım • Günlük Hareket (~62 dk)", "6,420 Steps • Daily Activity (~62 min)", lang),
        caloriesBurned = 210,
        durationMinutes = 62,
        aiAnalysis = trEn("Akıllı Saat Senkronu • MET kalori hesabı", "Smartwatch Sync • MET calorie calculation", lang)
    )
    return WorkoutState(
        selectedDate = "2026-11-13",
        rawText = "",
        workouts = listOf(initialWorkout),
        dailySteps = DailyStepTelemetry(
            stepCount = 6420,
            distanceKm = 4.8,
            durationMinutes = 62,
            caloriesBurned = 210,
            netCaloriesBurned = 95,
            sourceName = "Health Connect",
            stepTarget = 10_000
        ),
        allWorkouts = listOf(initialWorkout),
        totalCaloriesBurned = 210,
        totalDurationMinutes = 62,
        targetCalories = 500,
        targetDurationMinutes = 60,
        consumedCalories = 665,
        netCalories = 455,
        userWeightKg = 72.4
    )
}

fun createInitialDemoWeightState(lang: AppLanguage): WeightState {
    val entries = listOf(
        WeightEntry(id = "dw1", userId = 1, date = "2026-11-08", weightKg = 73.6, targetWeightKg = 70.0),
        WeightEntry(id = "dw2", userId = 1, date = "2026-11-09", weightKg = 73.3, targetWeightKg = 70.0),
        WeightEntry(id = "dw3", userId = 1, date = "2026-11-10", weightKg = 73.0, targetWeightKg = 70.0),
        WeightEntry(id = "dw4", userId = 1, date = "2026-11-11", weightKg = 72.7, targetWeightKg = 70.0),
        WeightEntry(id = "dw5", userId = 1, date = "2026-11-12", weightKg = 72.4, targetWeightKg = 70.0)
    )
    return WeightState(
        entries = entries,
        filteredEntries = entries,
        selectedPeriod = WeightChartPeriod.WEEKLY,
        currentWeightKg = 72.4,
        initialWeightKg = 73.6,
        weightChangeKg = -1.2,
        targetWeightKg = 70.0,
        inputWeight = "72.1",
        inputDate = "13.11.2026",
        inputNote = trEn("Sabah ölçümü", "Morning weigh-in", lang)
    )
}

fun demoSampleWeeklyReport(lang: AppLanguage): HealthAiReport = HealthAiReport(
    id = "demo_report_1",
    userId = 1,
    periodType = AnalysisPeriodType.WEEKLY,
    startDate = "2026-11-07",
    endDate = "2026-11-13",
    periodLabel = trEn("07 - 13 Kas 2026", "Nov 07 - 13, 2026", lang),
    score = 88,
    headline = trEn("Şampiyon Haftası! Hedeflerle Tam Uyum 🏆", "Champion Week! On Track With Goals 🏆", lang),
    summary = trEn(
        "Bu hafta 6 gün düzenli beslenme ve 4 antrenman kaydedildi. Günlük ortalama 2080 kcal alındı, 1420 kcal yakıldı. Kilo trendi: 73.6 kg → 72.1 kg (-1.5 kg).",
        "Logged 6 days of nutrition and 4 workouts this week. Daily avg 2080 kcal intake, 1420 kcal burned. Weight trend: 73.6 kg → 72.1 kg (-1.5 kg).",
        lang
    ),
    highlights = listOf(
        trEn("Haftalık protein ortalaman (138g/gün) kas koruma hedefinle %98 uyumlu.", "Weekly avg protein (138g/day) is 98% aligned with your muscle goal.", lang),
        trEn("4 antrenman ile toplam 185 dk aktif kalarak 1420 kcal yaktın.", "Completed 4 workouts (185 min total), burning 1420 kcal.", lang)
    ),
    warnings = listOf(
        trEn("Lif alımı bazı günlerde 22g seviyesinde kaldı.", "Fiber intake stayed around 22g on a couple of days.", lang)
    ),
    recommendations = listOf(
        trEn("Akşam öğünlerine 1 porsiyon yeşillik ekleyerek lif hedefini 30g'a tamamla.", "Add a side of greens to dinner to reach your 30g fiber goal.", lang)
    ),
    avgCaloriesIntake = 2080,
    targetCalories = 2100,
    totalCaloriesBurned = 1420,
    totalWorkoutMinutes = 185,
    workoutCount = 4,
    avgProteinG = 138.0,
    targetProteinG = 140.0,
    avgCarbsG = 210.0,
    avgFatG = 64.0,
    avgFiberG = 24.0,
    startWeightKg = 73.6,
    endWeightKg = 72.1,
    targetWeightKg = 70.0,
    loggedDaysCount = 6,
    isAutoGenerated = true
)

fun createInitialDemoAnalyticsState(lang: AppLanguage): AnalyticsState = AnalyticsState(
    userName = trEn("Demo Kullanıcı", "Demo User", lang),
    periodType = AnalysisPeriodType.WEEKLY,
    selectedDate = "2026-11-13",
    selectedWeekMonday = "2026-11-07",
    selectedWeekSunday = "2026-11-13",
    currentReport = null,
    recentReports = emptyList(),
    whatsAppPhone = "",
    autoWeeklyWhatsApp = true,
    previewCaloriesIntake = 2080,
    previewTargetCalories = 2100,
    previewCaloriesBurned = 1420,
    previewWorkoutMinutes = 185,
    previewProteinG = 138,
    previewTargetProteinG = 140,
    previewWeightKg = 72.1,
    previewLoggedDays = 6
)

fun createInitialOnboardingState(lang: AppLanguage): OnboardingState = OnboardingState(
    microStep = OnboardingMicroStep.FOOD_INPUT,
    foodState = createInitialDemoFoodState(lang),
    workoutState = createInitialDemoWorkoutState(lang),
    weightState = createInitialDemoWeightState(lang),
    analyticsState = createInitialDemoAnalyticsState(lang)
)
