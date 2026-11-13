package com.yusufteker.healthtrack.core.sync

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import com.yusufteker.healthtrack.core.model.WorkoutTelemetryHelper
import com.yusufteker.healthtrack.core.model.calculateProteinTarget
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.getTodayIso
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlin.math.roundToInt

/**
 * Computes a unified [WidgetTelemetrySnapshot] from local HealthDatabase + TokenStorage + Settings,
 * ensuring Android Widgets, iOS WidgetKit, and the in-app Live Widget Preview display identical metrics.
 */
class WidgetSnapshotBuilder(
    private val database: HealthDatabase,
    private val tokenStorage: TokenStorage,
    private val settings: Settings
) {
    fun buildTodaySnapshot(
        userId: Int = tokenStorage.getUserId(),
        todayIso: String = getTodayIso()
    ): WidgetTelemetrySnapshot {
        val isDark = settings.getBoolean("dark_mode", true)
        val accentId = settings.getString("theme_accent", "ocean")
        val langCode = LocalizationManager.currentLanguage.code

        if (!tokenStorage.isLoggedIn() || userId <= 0) {
            return WidgetTelemetrySnapshot(
                userId = 0,
                dateIso = todayIso,
                isLoggedIn = false,
                themeAccentId = accentId,
                isDarkTheme = isDark,
                languageCode = langCode,
                updatedAtMs = currentTimeMillis()
            )
        }

        val foods = database.getFoods(userId, todayIso)
        val allFoods = database.getAllFoods(userId)
        val workoutsToday = database.getWorkouts(userId, todayIso)
        val allWorkouts = database.getAllWorkouts(userId)
        val weights = database.getWeights(userId)
        val waterToday = database.getWaterSync(userId, todayIso)
        val allWaters = database.getAllWaterSync(userId)
        val goal = database.getNutritionGoalForSync(userId) ?: NutritionGoal(
            id = "goal_$userId",
            userId = userId
        )

        val fitnessGoal = if (goal.fitnessGoal.isNotBlank() && goal.fitnessGoal != FitnessGoal.MAINTAIN.id) {
            goal.parsedFitnessGoal
        } else {
            tokenStorage.getUserFitnessGoal()
        }

        val todayItems = foods.flatMap { it.items }
        val consumedCalories = foods.sumOf { it.totalCalories }
        val proteinG = todayItems.sumOf { it.proteinG }.roundToInt()
        val carbsG = todayItems.sumOf { it.carbsG }.roundToInt()
        val fatG = todayItems.sumOf { it.fatG }.roundToInt()

        val activeWeights = weights.filter { !it.isDeleted && it.weightKg > 0.0 }
        val latestWeight = activeWeights.maxByOrNull { it.date }?.weightKg
            ?: tokenStorage.getUserWeight().takeIf { it > 0.0 }
        val targetWeight = activeWeights.maxByOrNull { it.date }?.targetWeightKg
            ?: tokenStorage.getUserTargetWeight().takeIf { it > 0.0 }

        val effectiveWeightForProtein = latestWeight ?: 70.0
        val proteinTargetG = calculateProteinTarget(effectiveWeightForProtein, fitnessGoal).roundToInt().coerceAtLeast(40)

        val totalBurnedAll = workoutsToday.filter { !it.isDeleted }.sumOf { it.caloriesBurned }
        val explicitWorkouts = workoutsToday.filter { !it.isDeleted && !it.isDailySteps }
        val stepLog = workoutsToday.firstOrNull { !it.isDeleted && it.isDailySteps }
        val effectiveBurned = WorkoutTelemetryHelper.calculateEffectiveDailyBurn(explicitWorkouts, stepLog)
        val stepTelemetry = stepLog?.let { WorkoutTelemetryHelper.parseDailyStepTelemetry(it, explicitWorkouts) }

        val extraWater = (totalBurnedAll / 200) * 350
        val dynamicWaterTarget = (2500 + extraWater).coerceIn(2000, 4500)
        val currentWaterMl = waterToday?.amountMl ?: 0

        val gamification = GamificationEngine.evaluate(
            foods = allFoods,
            workouts = allWorkouts,
            weights = activeWeights,
            waters = allWaters,
            nutritionGoal = goal,
            fitnessGoal = fitnessGoal,
            latestWeightKg = latestWeight,
            todayIso = todayIso
        )

        return WidgetTelemetrySnapshot(
            userId = userId,
            dateIso = todayIso,
            isLoggedIn = true,
            consumedCalories = consumedCalories,
            targetCalories = goal.calories.coerceAtLeast(1000),
            burnedCalories = effectiveBurned,
            proteinG = proteinG,
            proteinTargetG = proteinTargetG,
            carbsG = carbsG,
            carbsTargetG = goal.carbsG.roundToInt().coerceAtLeast(50),
            fatG = fatG,
            fatTargetG = goal.fatG.roundToInt().coerceAtLeast(25),
            waterMl = currentWaterMl,
            waterTargetMl = dynamicWaterTarget,
            steps = stepTelemetry?.stepCount ?: 0,
            stepTarget = stepTelemetry?.stepTarget ?: 10_000,
            latestWeightKg = latestWeight,
            targetWeightKg = targetWeight,
            streakDays = gamification.currentStreakDays,
            isActiveToday = gamification.isActiveToday,
            themeAccentId = accentId,
            isDarkTheme = isDark,
            languageCode = langCode,
            updatedAtMs = currentTimeMillis()
        )
    }

    fun observeTodaySnapshot(userId: Int): Flow<WidgetTelemetrySnapshot> {
        if (userId <= 0) {
            return flowOf(buildTodaySnapshot(userId = 0))
        }
        val dataFlow = combine(
            database.observeAllFoods(userId),
            database.observeAllWorkouts(userId),
            database.observeWeights(userId),
            database.observeAllWater(userId),
            database.observeNutritionGoal(userId)
        ) { _, _, _, _, _ -> Unit }

        return combine(
            dataFlow,
            LocalizationManager.languageFlow
        ) { _, _ ->
            buildTodaySnapshot(userId = userId, todayIso = getTodayIso())
        }
    }
}
