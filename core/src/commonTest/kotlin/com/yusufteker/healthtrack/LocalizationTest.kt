package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.DeviceExerciseCategory
import com.yusufteker.healthtrack.core.model.DeviceSyncSummary
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.FoodLog
import com.yusufteker.healthtrack.core.model.MealSlotType
import com.yusufteker.healthtrack.core.model.Micronutrient
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DailyMealPlanEngine
import com.yusufteker.healthtrack.core.sync.HealthReportAiGenerator
import com.yusufteker.healthtrack.core.sync.SmartMealEngine
import com.yusufteker.healthtrack.core.sync.WorkoutIncentiveEngine
import com.yusufteker.healthtrack.core.utils.HealthReminderLogic
import com.yusufteker.healthtrack.core.utils.formatDayMonthYear
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.core.utils.formatMonthYearHeader
import com.yusufteker.healthtrack.core.utils.formatShortDateTr
import com.yusufteker.healthtrack.core.utils.formatWeekRangeTr
import com.yusufteker.healthtrack.core.utils.getWeekDaysForDate
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalizationTest {

    @AfterTest
    fun resetLanguage() {
        LocalizationManager.setLanguage(AppLanguage.TR)
    }

    @Test
    fun testLocalizationManagerPersistenceAndSwitching() {
        val settings = MapSettings()
        LocalizationManager.setLanguage(AppLanguage.EN, settings)
        assertEquals("en", settings.getStringOrNull(LocalizationManager.KEY_APP_LANGUAGE))
        assertEquals(AppLanguage.EN, LocalizationManager.currentLanguage)

        // Reset in-memory flow to TR, then re-init from settings and verify persistence wins
        LocalizationManager.setLanguage(AppLanguage.TR, settings = null)
        assertEquals(AppLanguage.TR, LocalizationManager.currentLanguage)
        val restored = LocalizationManager.init(settings)
        assertEquals(AppLanguage.EN, restored)
        assertEquals(AppLanguage.EN, LocalizationManager.currentLanguage)

        assertEquals(AppLanguage.TR, AppLanguage.fromCode("tr-TR"))
        assertEquals(AppLanguage.EN, AppLanguage.fromCode("en-US"))
        assertEquals("Merhaba", trEn("Merhaba", "Hello", AppLanguage.TR))
        assertEquals("Hello", trEn("Merhaba", "Hello", AppLanguage.EN))
    }

    @Test
    fun testDateUtilsBilingualFormatting() {
        val date = "2026-11-04"

        assertEquals("Kasım 2026", formatMonthYearHeader(date, AppLanguage.TR))
        assertEquals("November 2026", formatMonthYearHeader(date, AppLanguage.EN))

        assertEquals("04 Kasım 2026", formatDisplayDateTr(date, AppLanguage.TR))
        assertEquals("04 November 2026", formatDisplayDateTr(date, AppLanguage.EN))

        assertEquals("04.11.2026", formatShortDateTr(date, AppLanguage.TR))
        assertEquals("11/04/2026", formatShortDateTr(date, AppLanguage.EN))

        assertEquals("04 Kas 2026", formatDayMonthYear(date, AppLanguage.TR))
        assertEquals("04 Nov 2026", formatDayMonthYear(date, AppLanguage.EN))

        assertEquals("02 - 08 Kas 2026", formatWeekRangeTr("2026-11-02", "2026-11-08", AppLanguage.TR))
        assertEquals("02 - 08 Nov 2026", formatWeekRangeTr("2026-11-02", "2026-11-08", AppLanguage.EN))

        val trWeek = getWeekDaysForDate(date, AppLanguage.TR)
        val enWeek = getWeekDaysForDate(date, AppLanguage.EN)
        assertEquals(listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"), trWeek.map { it.dayName })
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), enWeek.map { it.dayName })
    }

    @Test
    fun testReminderAndWorkoutNotificationLocalization() {
        val (weightTitleTr, weightBodyTr) = HealthReminderLogic.buildWeightReminderContent(
            freq = WeightReminderFrequency.DAILY,
            daysSinceLastWeight = 3,
            language = AppLanguage.TR
        )
        val (weightTitleEn, weightBodyEn) = HealthReminderLogic.buildWeightReminderContent(
            freq = WeightReminderFrequency.DAILY,
            daysSinceLastWeight = 3,
            language = AppLanguage.EN
        )
        assertTrue(weightTitleTr.contains("Tartıldın"))
        assertTrue(weightBodyTr.contains("3 gündür"))
        assertTrue(weightTitleEn.contains("Weigh In"))
        assertTrue(weightBodyEn.contains("3 days"))

        val (foodTitleTr, foodBodyTr) = HealthReminderLogic.buildFoodReminderContent(AppLanguage.TR)
        val (foodTitleEn, foodBodyEn) = HealthReminderLogic.buildFoodReminderContent(AppLanguage.EN)
        assertTrue(foodTitleTr.contains("Bugün Ne Yedin"))
        assertTrue(foodBodyTr.contains("öğün"))
        assertTrue(foodTitleEn.contains("What Did You Eat"))
        assertTrue(foodBodyEn.contains("logged a meal"))

        val workoutTr = WorkoutIncentiveEngine.buildWorkoutReminderPayload("koşu", "Yusuf", AppLanguage.TR)
        val workoutEn = WorkoutIncentiveEngine.buildWorkoutReminderPayload("koşu", "Yusuf", AppLanguage.EN)
        assertTrue(workoutTr.title.contains("Koşu"))
        assertTrue(workoutTr.body.startsWith("Hey Yusuf!"))
        assertTrue(workoutEn.title.contains("Running"))
        assertTrue(workoutEn.body.contains("energy"))
    }

    @Test
    fun testDeviceHealthModelsLocalization() {
        assertEquals("Koşu", DeviceExerciseCategory.RUNNING.localizedTitle(AppLanguage.TR))
        assertEquals("Running", DeviceExerciseCategory.RUNNING.localizedTitle(AppLanguage.EN))

        val summary = DeviceSyncSummary(
            importedWeights = 2,
            importedWorkouts = 1,
            latestWorkoutDate = "2026-11-12"
        )
        val trMsg = summary.toUserMessage(AppLanguage.TR)
        val enMsg = summary.toUserMessage(AppLanguage.EN)
        assertTrue(trMsg.contains("2 yeni tartım eklendi"))
        assertTrue(trMsg.contains("1 yeni antrenman eklendi"))
        assertTrue(enMsg.contains("2 new weigh-ins added"))
        assertTrue(enMsg.contains("1 new workouts added"))
    }

    @Test
    fun testAiEnginesAndSmartMealsSupportEnglishAndTurkish() = runTest {
        val settings = MapSettings()
        val apiClient = HealthApiClient(TokenStorage(settings), baseUrl = "http://localhost")
        val mealPlanEngine = DailyMealPlanEngine(apiClient)
        val reportGenerator = HealthReportAiGenerator(apiClient)
        val goal = NutritionGoal(id = "g1", userId = 1, calories = 2200, proteinG = 120.0, fiberG = 30.0)

        val foodLogs = listOf(
            FoodLog(
                id = "f1",
                userId = 1,
                date = "2026-11-11",
                rawText = "2 boiled eggs and oats",
                totalCalories = 450,
                items = listOf(
                    FoodItem(
                        name = "Spinach Lentil Bowl",
                        formattedPortion = "1 bowl",
                        calories = 450,
                        proteinG = 22.0,
                        carbsG = 50.0,
                        fatG = 14.0,
                        fiberG = 8.0,
                        micronutrients = listOf(Micronutrient("Iron", 4.0, "mg"))
                    )
                )
            )
        )
        val workoutLogs = listOf(
            WorkoutLog(id = "w1", userId = 1, date = "2026-11-12", rawText = "30m run", durationMinutes = 30, caloriesBurned = 320)
        )
        val weights = listOf(
            WeightEntry(id = "wt1", userId = 1, date = "2026-11-10", weightKg = 78.0),
            WeightEntry(id = "wt2", userId = 1, date = "2026-11-12", weightKg = 77.2)
        )

        val planEn = mealPlanEngine.generateDailyPlan(
            userId = 1,
            dateIso = "2026-11-12",
            recentDaysFoodLogs = foodLogs,
            goal = goal,
            fitnessGoal = FitnessGoal.LOSE_WEIGHT,
            userWeightKg = 77.2,
            language = AppLanguage.EN
        )
        assertTrue(planEn.breakfast.title.isNotBlank())
        assertEquals("Breakfast", MealSlotType.BREAKFAST.localizedLabel(AppLanguage.EN))
        assertFalse(planEn.headline.contains("Günlük"), "English meal plan headline should be English: ${planEn.headline}")

        val planTr = mealPlanEngine.generateDailyPlan(
            userId = 1,
            dateIso = "2026-11-12",
            recentDaysFoodLogs = foodLogs,
            goal = goal,
            fitnessGoal = FitnessGoal.LOSE_WEIGHT,
            userWeightKg = 77.2,
            language = AppLanguage.TR
        )
        assertTrue(planTr.deficiencySummary.isNotBlank())
        assertEquals("Kahvaltı", MealSlotType.BREAKFAST.localizedLabel(AppLanguage.TR))

        val dailyReportEn = reportGenerator.generateDailyReport(
            userId = 1,
            dateIso = "2026-11-12",
            foodLogs = foodLogs,
            workoutLogs = workoutLogs,
            weights = weights,
            goal = goal,
            fitnessGoal = FitnessGoal.MAINTAIN,
            userWeightKg = 77.2,
            targetWeightKg = 75.0,
            language = AppLanguage.EN
        )
        assertEquals("12 November 2026", dailyReportEn.periodLabel)
        assertTrue(dailyReportEn.headline.isNotBlank())

        val weeklyReportEn = reportGenerator.generateWeeklyReport(
            userId = 1,
            mondayIso = "2026-11-09",
            sundayIso = "2026-11-15",
            foodLogs = foodLogs,
            workoutLogs = workoutLogs,
            weights = weights,
            goal = goal,
            fitnessGoal = FitnessGoal.MAINTAIN,
            userWeightKg = 77.2,
            targetWeightKg = 75.0,
            language = AppLanguage.EN
        )
        assertEquals("09 - 15 Nov 2026", weeklyReportEn.periodLabel)
        assertTrue(weeklyReportEn.summary.isNotBlank())

        val smartEn = SmartMealEngine.matchSmartMeals(
            remainingCalories = 600,
            missingProteinG = 35.0,
            nutrientBars = listOf(NutrientProgress(key = "iron", label = "Iron", current = 2.0, target = 14.0, unit = "mg", colorHex = "#EF4444")),
            recent3DaysFoodNames = emptySet(),
            language = AppLanguage.EN
        )
        assertTrue(smartEn.isNotEmpty())
        assertTrue(smartEn.first().title.isNotBlank())

        val tipsEn = SmartMealEngine.detectSynergyTips(
            todayItems = foodLogs.first().items,
            nutrientBars = emptyList(),
            waterCurrentMl = 500,
            language = AppLanguage.EN
        )
        assertTrue(tipsEn.isNotEmpty())
        assertTrue(tipsEn.any { it.title.contains("Iron Absorption", ignoreCase = true) && it.highlightNutrient == "Iron + Vitamin C" })
    }
}
