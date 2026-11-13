package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.utils.calculateDaysBetween
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.getWeekBoundsIso
import com.yusufteker.healthtrack.core.utils.shiftDateIso
import kotlin.math.abs
import kotlin.math.roundToInt

data class LevelBreakdown(
    val level: Int,
    val currentLevelXp: Int,
    val nextLevelXp: Int,
    val progress: Float
)

object GamificationEngine {

    fun evaluate(
        foods: List<FoodLog>,
        workouts: List<WorkoutLog>,
        weights: List<WeightEntry>,
        waters: List<WaterEntry>,
        nutritionGoal: NutritionGoal,
        fitnessGoal: FitnessGoal = nutritionGoal.parsedFitnessGoal,
        latestWeightKg: Double? = null,
        todayIso: String = getTodayIso()
    ): GamificationSummary {
        val activeFoods = foods.filter { !it.isDeleted && it.totalCalories > 0 }
        // Exclude passive daily step entries synced from Health Connect / Apple Health so
        // 30-day historical step backfills do not fabricate fake streaks, XP, or badges.
        val nonStepWorkouts = workouts.filter {
            !it.isDeleted && !it.isDailySteps && (it.caloriesBurned > 0 || it.durationMinutes > 0)
        }
        val activeWeights = weights.filter { !it.isDeleted && it.weightKg > 0.0 }
        val activeWaters = waters.filter { it.amountMl > 0 }

        val calTarget = nutritionGoal.calories.coerceAtLeast(1000)
        val proteinTarget = if (latestWeightKg != null && latestWeightKg > 0.0) {
            calculateProteinTarget(latestWeightKg, fitnessGoal).coerceAtLeast(40.0)
        } else {
            nutritionGoal.proteinG.coerceAtLeast(40.0)
        }

        // Group by date
        val foodByDate = activeFoods.groupBy { it.date }
        val workoutByDate = nonStepWorkouts.groupBy { it.date }
        val waterByDate = activeWaters.associateBy { it.date }
        val weightDates = activeWeights.map { it.date }.toSet()

        // Active dates for streak
        val activeDates = buildSet {
            addAll(foodByDate.keys)
            addAll(workoutByDate.keys)
            activeWaters.filter { it.amountMl >= 500 }.forEach { add(it.date) }
        }

        val isActiveToday = activeDates.contains(todayIso)
        val yesterdayIso = shiftDateIso(todayIso, -1)
        val yesterdayBurned = workoutByDate[yesterdayIso].orEmpty().sumOf { it.caloriesBurned }
        val isRestDayProtected = yesterdayBurned >= WorkoutIncentiveEngine.REST_DAY_CALORIE_THRESHOLD

        val (currentStreak, longestStreak) = calculateStreaks(todayIso, activeDates)

        // Nutrition & hydration milestone counts across days
        val proteinHitDays = foodByDate.count { (_, logs) ->
            val dayProtein = logs.sumOf { log -> log.items.sumOf { it.proteinG } }
            dayProtein >= proteinTarget
        }
        val calorieBandDays = foodByDate.count { (_, logs) ->
            val dayCal = logs.sumOf { it.totalCalories }
            val ratio = dayCal.toDouble() / calTarget.toDouble()
            ratio in 0.85..1.15
        }
        val hydrationHitDays = activeWaters.count { entry ->
            val target = entry.targetMl.coerceAtLeast(1500)
            entry.amountMl >= target
        }

        val totalWorkoutSessions = nonStepWorkouts.size
        val totalCaloriesBurned = nonStepWorkouts.sumOf { it.caloriesBurned.coerceAtLeast(0) }
        val totalLogsCount = activeFoods.size + nonStepWorkouts.size + activeWeights.size

        // Achievements
        val achievements = listOf(
            achievement(BadgeId.FIRST_LOG, totalLogsCount),
            achievement(BadgeId.STREAK_3, longestStreak.coerceAtLeast(currentStreak)),
            achievement(BadgeId.STREAK_7, longestStreak.coerceAtLeast(currentStreak)),
            achievement(BadgeId.STREAK_14, longestStreak.coerceAtLeast(currentStreak)),
            achievement(BadgeId.STREAK_30, longestStreak.coerceAtLeast(currentStreak)),
            achievement(BadgeId.PROTEIN_PRO, proteinHitDays),
            achievement(BadgeId.CALORIE_SNIPER, calorieBandDays),
            achievement(BadgeId.HYDRATION_HERO, hydrationHitDays),
            achievement(BadgeId.WORKOUT_10, totalWorkoutSessions),
            achievement(BadgeId.BURN_1000, totalCaloriesBurned),
            achievement(BadgeId.BURN_5000, totalCaloriesBurned),
            achievement(BadgeId.SCALE_TRACKER, activeWeights.size)
        )

        // Today stats & Bio-Score (0..100)
        val todayFoods = foodByDate[todayIso].orEmpty()
        val todayCalories = todayFoods.sumOf { it.totalCalories }
        val todayProtein = todayFoods.sumOf { log -> log.items.sumOf { it.proteinG } }
        val todayWorkouts = workoutByDate[todayIso].orEmpty()
        val todayWater = waterByDate[todayIso]
        val todayWaterTarget = (todayWater?.targetMl ?: 2500).coerceAtLeast(1500)
        val todayWaterMl = todayWater?.amountMl ?: 0
        val hasFoodToday = todayFoods.isNotEmpty()
        val hasWorkoutToday = todayWorkouts.isNotEmpty()
        val hasWaterTargetToday = todayWaterMl >= todayWaterTarget

        val foodLoggedPts = if (hasFoodToday) 25 else 0
        val calBandPts = if (hasFoodToday) {
            val ratio = todayCalories.toDouble() / calTarget.toDouble()
            if (ratio in 0.85..1.15) 20 else (20.0 - abs(1.0 - ratio) * 35.0).roundToInt().coerceIn(5, 20)
        } else 0
        val proteinPts = ((todayProtein / proteinTarget).coerceIn(0.0, 1.0) * 25.0).roundToInt()
        val waterPts = ((todayWaterMl.toDouble() / todayWaterTarget.toDouble()).coerceIn(0.0, 1.0) * 15.0).roundToInt()
        val workoutPts = if (hasWorkoutToday || isRestDayProtected) 15 else 0
        val todayScore = (foodLoggedPts + calBandPts + proteinPts + waterPts + workoutPts).coerceIn(0, 100)

        // Daily Quests
        val dailyQuests = listOf(
            GamificationQuest(
                id = "daily_food",
                period = QuestPeriod.DAILY,
                titleTr = "Bugün öğün kaydı gir",
                titleEn = "Log a meal today",
                currentCount = if (hasFoodToday) 1 else 0,
                targetCount = 1,
                xpReward = 25,
                isCompleted = hasFoodToday
            ),
            GamificationQuest(
                id = "daily_water",
                period = QuestPeriod.DAILY,
                titleTr = "Su hedefine ulaş ($todayWaterTarget ml)",
                titleEn = "Reach water goal ($todayWaterTarget ml)",
                currentCount = todayWaterMl.coerceAtMost(todayWaterTarget),
                targetCount = todayWaterTarget,
                xpReward = 25,
                isCompleted = hasWaterTargetToday
            ),
            GamificationQuest(
                id = "daily_workout",
                period = QuestPeriod.DAILY,
                titleTr = if (isRestDayProtected && !hasWorkoutToday) {
                    "Dinlenme günü koruması aktif"
                } else {
                    "Bugün antrenman / hareket kaydet"
                },
                titleEn = if (isRestDayProtected && !hasWorkoutToday) {
                    "Rest day protection active"
                } else {
                    "Log a workout / activity today"
                },
                currentCount = if (hasWorkoutToday || isRestDayProtected) 1 else 0,
                targetCount = 1,
                xpReward = 30,
                isCompleted = hasWorkoutToday || isRestDayProtected
            )
        )

        // Weekly Quests
        val (weekMonday, weekSunday) = getWeekBoundsIso(todayIso)
        val weekFoodDays = foodByDate.keys.count { it in weekMonday..weekSunday }
        val weekWorkoutSessions = nonStepWorkouts.count {
            it.date in weekMonday..weekSunday
        }
        val weekWaterHitDays = activeWaters.count {
            it.date in weekMonday..weekSunday && it.amountMl >= it.targetMl.coerceAtLeast(1500)
        }

        val weeklyQuests = listOf(
            GamificationQuest(
                id = "weekly_workouts",
                period = QuestPeriod.WEEKLY,
                titleTr = "Bu hafta 3 antrenman tamamla",
                titleEn = "Complete 3 workouts this week",
                currentCount = weekWorkoutSessions.coerceAtMost(3),
                targetCount = 3,
                xpReward = 100,
                isCompleted = weekWorkoutSessions >= 3
            ),
            GamificationQuest(
                id = "weekly_foods",
                period = QuestPeriod.WEEKLY,
                titleTr = "Bu hafta 5 gün öğün kaydet",
                titleEn = "Log meals on 5 days this week",
                currentCount = weekFoodDays.coerceAtMost(5),
                targetCount = 5,
                xpReward = 90,
                isCompleted = weekFoodDays >= 5
            ),
            GamificationQuest(
                id = "weekly_water",
                period = QuestPeriod.WEEKLY,
                titleTr = "Bu hafta 4 gün su hedefini tut",
                titleEn = "Hit water goal on 4 days this week",
                currentCount = weekWaterHitDays.coerceAtMost(4),
                targetCount = 4,
                xpReward = 80,
                isCompleted = weekWaterHitDays >= 4
            )
        )

        // Total XP calculation (deterministic from logs + unlocked achievement rewards + weekly quest bonuses)
        val baseActionXp =
            (foodByDate.size * 20) +
                (proteinHitDays * 15) +
                (calorieBandDays * 15) +
                (hydrationHitDays * 15) +
                (workoutByDate.size * 30) +
                (weightDates.size * 10)

        val achievementXp = achievements.filter { it.isUnlocked }.sumOf { it.badgeId.xpReward }
        val weeklyQuestXp = weeklyQuests.filter { it.isCompleted }.sumOf { it.xpReward }
        val totalXp = baseActionXp + achievementXp + weeklyQuestXp

        val todayEarnedXp =
            (if (hasFoodToday) 20 else 0) +
                (if (hasFoodToday && todayProtein >= proteinTarget) 15 else 0) +
                (if (hasFoodToday && (todayCalories.toDouble() / calTarget) in 0.85..1.15) 15 else 0) +
                (if (hasWaterTargetToday) 15 else 0) +
                (if (hasWorkoutToday) 30 else 0) +
                (if (weightDates.contains(todayIso)) 10 else 0)

        val breakdown = computeLevel(totalXp)
        val tier = GamificationTier.fromLevel(breakdown.level)

        return GamificationSummary(
            totalXp = totalXp,
            level = breakdown.level,
            currentLevelXp = breakdown.currentLevelXp,
            nextLevelXp = breakdown.nextLevelXp,
            levelProgress = breakdown.progress,
            tier = tier,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            isActiveToday = isActiveToday,
            isRestDayProtected = isRestDayProtected,
            todayScore = todayScore,
            todayEarnedXp = todayEarnedXp,
            unlockedBadgesCount = achievements.count { it.isUnlocked },
            totalBadgesCount = achievements.size,
            achievements = achievements,
            dailyQuests = dailyQuests,
            weeklyQuests = weeklyQuests
        )
    }

    fun computeLevel(totalXp: Int): LevelBreakdown {
        var remaining = totalXp.coerceAtLeast(0)
        var level = 1
        while (true) {
            val needed = xpRequiredForLevel(level)
            if (remaining < needed) {
                val progress = if (needed > 0) (remaining.toFloat() / needed.toFloat()).coerceIn(0f, 1f) else 0f
                return LevelBreakdown(
                    level = level,
                    currentLevelXp = remaining,
                    nextLevelXp = needed,
                    progress = progress
                )
            }
            remaining -= needed
            level++
        }
    }

    private fun xpRequiredForLevel(level: Int): Int {
        return 150 + (level - 1) * 50
    }

    private fun achievement(badgeId: BadgeId, rawProgress: Int): GamificationAchievement {
        val clamped = rawProgress.coerceAtLeast(0)
        return GamificationAchievement(
            badgeId = badgeId,
            progressCurrent = clamped.coerceAtMost(badgeId.targetCount),
            progressTarget = badgeId.targetCount,
            isUnlocked = clamped >= badgeId.targetCount
        )
    }

    internal fun calculateStreaks(todayIso: String, activeDates: Set<String>): Pair<Int, Int> {
        if (activeDates.isEmpty()) return 0 to 0

        val sorted = activeDates.sorted()
        var longest = 1
        var run = 1
        for (i in 1 until sorted.size) {
            val diff = calculateDaysBetween(sorted[i - 1], sorted[i])
            if (diff == 1) {
                run++
                if (run > longest) longest = run
            } else if (diff > 1) {
                run = 1
            }
        }

        val yesterdayIso = shiftDateIso(todayIso, -1)
        var cursor = when {
            activeDates.contains(todayIso) -> todayIso
            activeDates.contains(yesterdayIso) -> yesterdayIso
            else -> return 0 to longest
        }

        var current = 0
        while (activeDates.contains(cursor)) {
            current++
            cursor = shiftDateIso(cursor, -1)
        }
        return current to longest.coerceAtLeast(current)
    }
}

