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
        val streakAch = summary.achievements.first { it.badgeId == BadgeId.STREAK_MASTER }
        assertTrue(streakAch.isUnlocked)
        assertEquals(1, streakAch.currentLevel)
        assertEquals(7, streakAch.progressTarget)
        assertEquals(75, streakAch.earnedXp)
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
    fun evaluate_burnStagesAndDuration_updatesMultiLevelProgressCorrectly() {
        val workouts = listOf(
            WorkoutLog(id = "w1", userId = 1, date = "2026-11-08", rawText = "Uzun Koşu", caloriesBurned = 2200, durationMinutes = 90),
            WorkoutLog(id = "w2", userId = 1, date = "2026-11-09", rawText = "Bisiklet", caloriesBurned = 3100, durationMinutes = 110),
            WorkoutLog(id = "w3", userId = 1, date = "2026-11-11", rawText = "HIIT", caloriesBurned = 4800, durationMinutes = 100)
        )
        val summary = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = workouts,
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = "2026-11-13"
        )

        val engine = summary.achievements.first { it.badgeId == BadgeId.CALORIE_ENGINE }
        assertTrue(engine.isUnlocked)
        // 10.100 kcal unlocks Sv.1 (1000) + Sv.2 (5000) + Sv.3 (10000), targeting Sv.4 (25000)
        assertEquals(3, engine.currentLevel)
        assertEquals(10100, engine.rawProgress)
        assertEquals(25000, engine.progressTarget)
        assertEquals(100 + 250 + 500, engine.earnedXp)

        val endurance = summary.achievements.first { it.badgeId == BadgeId.ENDURANCE_MINUTES }
        assertTrue(endurance.isUnlocked)
        // 300 minutes unlocks Sv.1 (150), targeting Sv.2 (500)
        assertEquals(1, endurance.currentLevel)
        assertEquals(300, endurance.rawProgress)
        assertEquals(500, endurance.progressTarget)
    }

    @Test
    fun evaluate_newBehavioralBadges_bioScoreWeekendAndFiberUnlockCorrectly() {
        // 2026-11-07 Sat + 2026-11-08 Sun, 2026-11-14 Sat + 2026-11-15 Sun -> 2 full weekends + fiber >= 30g + high bio score
        val activeDays = listOf("2026-11-07", "2026-11-08", "2026-11-14", "2026-11-15", "2026-11-16")
        val foods = activeDays.mapIndexed { idx, d ->
            FoodLog(
                id = "food_$idx",
                userId = 1,
                date = d,
                rawText = "Yulaf & Tavuk",
                totalCalories = 2000,
                items = listOf(
                    FoodItem(
                        name = "Tam Gün",
                        formattedPortion = "1 porsiyon",
                        calories = 2000,
                        proteinG = 100.0,
                        fiberG = 35.0
                    )
                )
            )
        }
        val workouts = activeDays.mapIndexed { idx, d ->
            WorkoutLog(
                id = "wk_$idx",
                userId = 1,
                date = d,
                rawText = "Koşu",
                caloriesBurned = 350,
                durationMinutes = 35
            )
        }
        val waters = activeDays.map { d ->
            WaterEntry(date = d, userId = 1, amountMl = 2600, targetMl = 2500)
        }

        val summary = GamificationEngine.evaluate(
            foods = foods,
            workouts = workouts,
            weights = emptyList(),
            waters = waters,
            nutritionGoal = NutritionGoal(id = "g1", userId = 1, calories = 2000, proteinG = 80.0, fiberG = 30.0),
            todayIso = "2026-11-16"
        )

        val weekend = summary.achievements.first { it.badgeId == BadgeId.WEEKEND_WARRIOR }
        assertTrue(weekend.isUnlocked)
        assertEquals(1, weekend.currentLevel)
        assertEquals(2, weekend.rawProgress)

        val bioElite = summary.achievements.first { it.badgeId == BadgeId.BIO_SCORE_ELITE }
        assertTrue(bioElite.isUnlocked)
        assertEquals(1, bioElite.currentLevel)
        assertEquals(5, bioElite.rawProgress)

        val fiber = summary.achievements.first { it.badgeId == BadgeId.FIBER_CHAMPION }
        assertTrue(fiber.isUnlocked)
        assertEquals(1, fiber.currentLevel)
        assertEquals(5, fiber.rawProgress)
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
        assertEquals(0, summary.unlockedTiersCount)
        assertFalse(summary.achievements.first { it.badgeId == BadgeId.STREAK_MASTER }.isUnlocked)
        assertFalse(summary.achievements.first { it.badgeId == BadgeId.CALORIE_ENGINE }.isUnlocked)
        assertFalse(summary.achievements.first { it.badgeId == BadgeId.ENDURANCE_MINUTES }.isUnlocked)
    }
}

