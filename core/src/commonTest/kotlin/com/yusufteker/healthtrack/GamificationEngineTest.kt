package com.yusufteker.healthtrack

import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.sync.GamificationEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GamificationEngineTest {

    @Test
    fun evaluate_emptyLogs_returnsLevel1ZeroStreak() {
        val summary = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = emptyList(),
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = "2026-11-13"
        )
        assertEquals(1, summary.level)
        assertEquals(0, summary.totalXp)
        assertEquals(0, summary.currentStreakDays)
        assertEquals(0, summary.unlockedBadgesCount)
        assertFalse(summary.isActiveToday)
    }

    @Test
    fun evaluate_threeConsecutiveDays_unlocksFirstLogAndStreak3() {
        val foods = listOf("2026-11-11", "2026-11-12", "2026-11-13").mapIndexed { idx, date ->
            FoodLog(
                id = "f$idx",
                userId = 1,
                date = date,
                rawText = "Tavuk salata",
                totalCalories = 1950,
                items = listOf(
                    FoodItem(
                        id = "i$idx",
                        name = "Tavuk",
                        formattedPortion = "300g",
                        calories = 1950,
                        proteinG = 110.0
                    )
                )
            )
        }
        val summary = GamificationEngine.evaluate(
            foods = foods,
            workouts = emptyList(),
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1, calories = 2000, proteinG = 80.0),
            todayIso = "2026-11-13"
        )

        assertEquals(3, summary.currentStreakDays)
        assertEquals(3, summary.longestStreakDays)
        assertTrue(summary.isActiveToday)
        assertTrue(summary.achievements.first { it.badgeId == BadgeId.FIRST_LOG }.isUnlocked)
        assertTrue(summary.achievements.first { it.badgeId == BadgeId.STREAK_3 }.isUnlocked)
        assertFalse(summary.achievements.first { it.badgeId == BadgeId.STREAK_7 }.isUnlocked)
    }

    @Test
    fun evaluate_yesterdayHardWorkout_activatesRestDayProtection() {
        val workouts = listOf(
            WorkoutLog(
                id = "w1",
                userId = 1,
                date = "2026-11-12",
                rawText = "45 dk koşu",
                caloriesBurned = 520,
                durationMinutes = 45
            )
        )
        val summary = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = workouts,
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = "2026-11-13"
        )

        assertEquals(1, summary.currentStreakDays)
        assertTrue(summary.isRestDayProtected)
        assertFalse(summary.isActiveToday)
        assertTrue(summary.dailyQuests.first { it.id == "daily_workout" }.isCompleted)
    }

    @Test
    fun evaluate_burn1000AndWorkouts_updatesProgressCorrectly() {
        val workouts = listOf(
            WorkoutLog(id = "w1", userId = 1, date = "2026-11-11", rawText = "Koşu", caloriesBurned = 650, durationMinutes = 50),
            WorkoutLog(id = "w2", userId = 1, date = "2026-11-12", rawText = "Bisiklet", caloriesBurned = 450, durationMinutes = 40)
        )
        val summary = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = workouts,
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = "2026-11-13"
        )

        assertTrue(summary.achievements.first { it.badgeId == BadgeId.BURN_1000 }.isUnlocked)
        assertFalse(summary.achievements.first { it.badgeId == BadgeId.BURN_5000 }.isUnlocked)
        assertEquals(1100, summary.achievements.first { it.badgeId == BadgeId.BURN_5000 }.progressCurrent)
    }

    @Test
    fun computeLevel_advancesSmoothlyAcrossThresholds() {
        val lv1 = GamificationEngine.computeLevel(0)
        assertEquals(1, lv1.level)
        assertEquals(150, lv1.nextLevelXp)

        val lv2 = GamificationEngine.computeLevel(150)
        assertEquals(2, lv2.level)
        assertEquals(0, lv2.currentLevelXp)
        assertEquals(200, lv2.nextLevelXp)

        val lv3 = GamificationEngine.computeLevel(360)
        assertEquals(3, lv3.level)
        assertEquals(10, lv3.currentLevelXp)
    }

    @Test
    fun evaluate_passiveDailyStepsFor35Days_doesNotGrantFakeStreakOrBadges() {
        val passiveStepWorkouts = (0 until 35).map { offset ->
            val day = 1 + (offset % 28)
            val month = if (offset < 28) 10 else 11
            val date = "2026-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            WorkoutLog(
                id = "wdev_1_${date}_steps",
                userId = 1,
                date = date,
                rawText = "DAILY_STEPS 8500",
                caloriesBurned = 320,
                durationMinutes = 40
            )
        }

        val summary = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = passiveStepWorkouts,
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = "2026-11-13"
        )

        assertEquals(0, summary.currentStreakDays)
        assertEquals(0, summary.longestStreakDays)
        assertEquals(0, summary.totalXp)
        assertEquals(0, summary.unlockedBadgesCount)
        assertFalse(summary.achievements.first { it.badgeId == BadgeId.STREAK_30 }.isUnlocked)
        assertFalse(summary.achievements.first { it.badgeId == BadgeId.BURN_5000 }.isUnlocked)
    }
}

