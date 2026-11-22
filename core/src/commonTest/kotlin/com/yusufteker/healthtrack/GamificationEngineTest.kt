package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.sync.GamificationEngine
import com.yusufteker.healthtrack.core.sync.GamificationManager
import com.yusufteker.healthtrack.core.utils.getTodayIso
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        assertFalse(summary.achievements.first { it.badgeId == BadgeId.DEVICE_BRIDGE }.isUnlocked)
    }

    @Test
    fun evaluate_connectedDevice_unlocksDeviceBridgeBadge() {
        val summary = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = emptyList(),
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = "2026-11-13",
            hasConnectedDevice = true
        )
        val deviceBadge = summary.achievements.first { it.badgeId == BadgeId.DEVICE_BRIDGE }
        assertTrue(deviceBadge.isUnlocked)
        assertEquals(150, deviceBadge.earnedXp)
    }

    @Test
    fun evaluate_sportBadges_tennisStagesAndStepProgress() {
        fun evalWithTennisCount(count: Int): GamificationAchievement {
            val workouts = (1..count).map { idx ->
                WorkoutLog(
                    id = "ten_$idx",
                    userId = 1,
                    date = "2026-11-${(10 + idx).toString().padStart(2, '0')}",
                    rawText = "45 dk Tenis",
                    caloriesBurned = 320,
                    durationMinutes = 45
                )
            }
            val summary = GamificationEngine.evaluate(
                foods = emptyList(),
                workouts = workouts,
                weights = emptyList(),
                waters = emptyList(),
                nutritionGoal = NutritionGoal(id = "g1", userId = 1),
                todayIso = "2026-11-16"
            )
            return summary.achievements.first { it.badgeId == BadgeId.SPORT_TENNIS }
        }

        // 1. Tenis: Sv. 1 açılır (target=1), sonraki seviye Sv. 2 için 3 antrenman gerekir (0/3)
        val t1 = evalWithTennisCount(1)
        assertTrue(t1.isUnlocked)
        assertEquals(1, t1.currentLevel)
        assertEquals(0, t1.stageStepCurrent)
        assertEquals(3, t1.stageStepTarget)
        assertEquals(0f, t1.stageProgressFraction)

        // 2. Tenis: Sv. 2 için barın 1/3'ü dolar
        val t2 = evalWithTennisCount(2)
        assertEquals(1, t2.currentLevel)
        assertEquals(1, t2.stageStepCurrent)
        assertEquals(3, t2.stageStepTarget)
        assertEquals(1f / 3f, t2.stageProgressFraction, 0.001f)

        // 3. Tenis: Sv. 2 için barın 2/3'ü dolar
        val t3 = evalWithTennisCount(3)
        assertEquals(1, t3.currentLevel)
        assertEquals(2, t3.stageStepCurrent)
        assertEquals(3, t3.stageStepTarget)
        assertEquals(2f / 3f, t3.stageProgressFraction, 0.001f)

        // 4. Tenis: Sv. 2 (Gümüş Madalya) açılır
        val t4 = evalWithTennisCount(4)
        assertEquals(2, t4.currentLevel)
        assertEquals(BadgeRank.SILVER, BadgeId.SPORT_TENNIS.rankForLevel(t4.currentLevel))
        assertEquals(0, t4.stageStepCurrent)
        assertEquals(5, t4.stageStepTarget)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun gamificationManager_tennisWorkouts_emitsUnlockAndProgressPopupsInOrder() = runTest {
        val settings = MapSettings()
        val database = HealthDatabase(settings)
        val manager = GamificationManager(database, settings)
        val today = getTodayIso()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.observeSummary(1).collect {}
        }

        // Baseline seed should not emit popups
        assertEquals(null, manager.currentCelebration.value)

        // 1. Tenis -> Sv. 1 Açılışı + Konfeti (kuyruğun başında Tenis başarımı!)
        database.upsertWorkout(
            WorkoutLog(
                id = "w_ten_1",
                userId = 1,
                date = today,
                rawText = "45 dk Tenis",
                caloriesBurned = 320,
                durationMinutes = 45
            )
        )
        val event1 = manager.currentCelebration.value
        assertNotNull(event1)
        assertEquals(CelebrationType.BADGE_UNLOCKED, event1.type)
        assertEquals(BadgeId.SPORT_TENNIS, event1.badgeId)
        assertEquals(1, event1.badgeStageLevel)
        assertTrue(event1.showConfetti)
        assertEquals(0f, event1.previousProgressFraction)
        assertEquals(1f, event1.targetProgressFraction)
        manager.clearAllCelebrations()

        // 2. Tenis -> Sv. 2 için 1/3 ilerleme barı (0f -> 1/3f)
        database.upsertWorkout(
            WorkoutLog(
                id = "w_ten_2",
                userId = 1,
                date = today,
                rawText = "45 dk Tenis",
                caloriesBurned = 320,
                durationMinutes = 45
            )
        )
        val event2 = manager.currentCelebration.value
        assertNotNull(event2)
        assertEquals(CelebrationType.SPORT_PROGRESS, event2.type)
        assertEquals(BadgeId.SPORT_TENNIS, event2.badgeId)
        assertEquals(1, event2.progressCurrent)
        assertEquals(3, event2.progressTarget)
        assertEquals(0f, event2.previousProgressFraction ?: -1f, 0.001f)
        assertEquals(1f / 3f, event2.targetProgressFraction ?: -1f, 0.001f)
        assertFalse(event2.showConfetti)
        manager.clearAllCelebrations()

        // 3. Tenis -> Sv. 2 için 2/3 ilerleme barı (1/3f -> 2/3f)
        database.upsertWorkout(
            WorkoutLog(
                id = "w_ten_3",
                userId = 1,
                date = today,
                rawText = "45 dk Tenis",
                caloriesBurned = 320,
                durationMinutes = 45
            )
        )
        val event3 = manager.currentCelebration.value
        assertNotNull(event3)
        assertEquals(CelebrationType.SPORT_PROGRESS, event3.type)
        assertEquals(2, event3.progressCurrent)
        assertEquals(3, event3.progressTarget)
        assertEquals(1f / 3f, event3.previousProgressFraction ?: -1f, 0.001f)
        assertEquals(2f / 3f, event3.targetProgressFraction ?: -1f, 0.001f)
        manager.clearAllCelebrations()

        // 4. Tenis -> Sv. 2 Gümüş Madalya Açılışı + Konfeti (2/3f -> 1f)
        database.upsertWorkout(
            WorkoutLog(
                id = "w_ten_4",
                userId = 1,
                date = today,
                rawText = "45 dk Tenis",
                caloriesBurned = 320,
                durationMinutes = 45
            )
        )
        val event4 = manager.currentCelebration.value
        assertNotNull(event4)
        assertEquals(CelebrationType.BADGE_UNLOCKED, event4.type)
        assertEquals(BadgeId.SPORT_TENNIS, event4.badgeId)
        assertEquals(2, event4.badgeStageLevel)
        assertEquals(3, event4.progressCurrent)
        assertEquals(3, event4.progressTarget)
        assertEquals(2f / 3f, event4.previousProgressFraction ?: -1f, 0.001f)
        assertEquals(1f, event4.targetProgressFraction ?: -1f, 0.001f)
        assertTrue(event4.showConfetti)

        job.cancel()
    }

    @Test
    fun evaluate_sportBadges_haveTenLevelsDifficultMaxAndUpdatedTitles() {
        assertEquals("Potanın Kralı", BadgeId.SPORT_BASKETBALL.titleTr)
        assertEquals("Gol Kralı", BadgeId.SPORT_FOOTBALL.titleTr)
        assertEquals("Kortun Efendisi", BadgeId.SPORT_TENNIS.titleTr)
        assertEquals("Demir Bilek", BadgeId.SPORT_FITNESS.titleTr)
        assertEquals("Dalga Kıran", BadgeId.SPORT_ROWING.titleTr)

        val sportBadges = BadgeId.entries.filter { it.isSportBadge }
        assertEquals(15, sportBadges.size)
        sportBadges.forEach { badge ->
            assertEquals(10, badge.maxLevel)
            assertEquals(399, badge.stageForLevel(10)?.targetCount)
            assertEquals(249, badge.stageForLevel(9)?.targetCount)
        }

        assertEquals(BadgeRank.LOCKED, BadgeId.SPORT_BASKETBALL.rankForLevel(0))
        assertEquals(BadgeRank.BRONZE, BadgeId.SPORT_BASKETBALL.rankForLevel(1))
        assertEquals(BadgeRank.SILVER, BadgeId.SPORT_BASKETBALL.rankForLevel(2))
        assertEquals(BadgeRank.GOLD, BadgeId.SPORT_BASKETBALL.rankForLevel(3))
        assertEquals(BadgeRank.PLATINUM, BadgeId.SPORT_BASKETBALL.rankForLevel(4))
        assertEquals(BadgeRank.EMERALD, BadgeId.SPORT_BASKETBALL.rankForLevel(5))
        assertEquals(BadgeRank.SAPPHIRE, BadgeId.SPORT_BASKETBALL.rankForLevel(6))
        assertEquals(BadgeRank.RUBY, BadgeId.SPORT_BASKETBALL.rankForLevel(7))
        assertEquals(BadgeRank.DIAMOND, BadgeId.SPORT_BASKETBALL.rankForLevel(8))
        assertEquals(BadgeRank.MASTER, BadgeId.SPORT_BASKETBALL.rankForLevel(9))
        assertEquals(BadgeRank.LEGEND, BadgeId.SPORT_BASKETBALL.rankForLevel(10))

        // Verify 399 workouts reaches Lv.10 Legend while 398 stays Lv.9 Master (149/150 in final stage)
        val workouts398 = (1..398).map { idx ->
            WorkoutLog(
                id = "bask_$idx",
                userId = 1,
                date = "2026-11-10",
                rawText = "45 dk Basketbol",
                caloriesBurned = 360,
                durationMinutes = 45
            )
        }
        val summary398 = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = workouts398,
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = "2026-11-16"
        )
        val ach398 = summary398.achievements.first { it.badgeId == BadgeId.SPORT_BASKETBALL }
        assertEquals(9, ach398.currentLevel)
        assertFalse(ach398.isMaxed)
        assertEquals(149, ach398.stageStepCurrent)
        assertEquals(150, ach398.stageStepTarget)

        val workouts399 = workouts398 + WorkoutLog(
            id = "bask_399",
            userId = 1,
            date = "2026-11-11",
            rawText = "45 dk Basketbol",
            caloriesBurned = 360,
            durationMinutes = 45
        )
        val summary399 = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = workouts399,
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = "2026-11-16"
        )
        val ach399 = summary399.achievements.first { it.badgeId == BadgeId.SPORT_BASKETBALL }
        assertEquals(10, ach399.currentLevel)
        assertTrue(ach399.isMaxed)
        assertEquals(BadgeRank.LEGEND, BadgeId.SPORT_BASKETBALL.rankForLevel(ach399.currentLevel))
    }

    @Test
    fun evaluate_allExpandedSports_detectAndUnlockCorrespondingBadges() {
        val sampleWorkouts = listOf(
            "30 dk Bisiklet sürüşü" to BadgeId.SPORT_CYCLING,
            "40 dk Havuzda Yüzme" to BadgeId.SPORT_SWIMMING,
            "45 dk Tempolu Yürüyüş" to BadgeId.SPORT_WALKING,
            "40 dk Yoga & Pilates" to BadgeId.SPORT_YOGA_PILATES,
            "50 dk Voleybol maçı" to BadgeId.SPORT_VOLLEYBALL,
            "45 dk Boks & Kickboks" to BadgeId.SPORT_BOXING,
            "25 dk HIIT Kardiyo" to BadgeId.SPORT_HIIT,
            "40 dk Kalistenik & Barfiks" to BadgeId.SPORT_CALISTHENICS,
            "45 dk Zumba Dans" to BadgeId.SPORT_DANCE
        ).mapIndexed { idx, (text, _) ->
            WorkoutLog(
                id = "exp_$idx",
                userId = 1,
                date = "2026-11-12",
                rawText = text,
                caloriesBurned = 300,
                durationMinutes = 40
            )
        }

        val summary = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = sampleWorkouts,
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = "2026-11-16"
        )

        val expectedBadges = listOf(
            BadgeId.SPORT_CYCLING,
            BadgeId.SPORT_SWIMMING,
            BadgeId.SPORT_WALKING,
            BadgeId.SPORT_YOGA_PILATES,
            BadgeId.SPORT_VOLLEYBALL,
            BadgeId.SPORT_BOXING,
            BadgeId.SPORT_HIIT,
            BadgeId.SPORT_CALISTHENICS,
            BadgeId.SPORT_DANCE
        )
        expectedBadges.forEach { badgeId ->
            val ach = summary.achievements.first { it.badgeId == badgeId }
            assertTrue(ach.isUnlocked, "Expected $badgeId to unlock")
            assertEquals(1, ach.currentLevel, "Expected $badgeId to be at level 1")
            assertEquals(10, ach.maxLevel, "Expected $badgeId to have 10 levels")
        }
    }

    @Test
    fun mealHealthEvaluator_healthyWholeFoodMeal_scoresAbove80AndIsCelebrationWorthy() {
        val items = listOf(
            FoodItem(
                id = "m1",
                name = "Izgara Somon",
                formattedPortion = "180g",
                calories = 340,
                proteinG = 38.0,
                carbsG = 0.0,
                fatG = 20.0,
                fiberG = 0.0,
                micronutrients = listOf(
                    Micronutrient("Vitamin D", 12.0, "µg"),
                    Micronutrient("Vitamin B12", 4.5, "µg")
                )
            ),
            FoodItem(
                id = "m2",
                name = "Zeytinyağlı Brokoli Salata",
                formattedPortion = "1 Porsiyon",
                calories = 140,
                proteinG = 4.5,
                carbsG = 10.0,
                fatG = 8.0,
                fiberG = 6.2
            )
        )

        val evaluation = MealHealthEvaluator.evaluate(
            items = items,
            totalCalories = 480,
            summary = "Izgara somon ve zeytinyağlı brokoli salatası"
        )

        assertTrue(evaluation.score >= 80, "Expected healthy meal score >= 80, got ${evaluation.score}")
        assertEquals(MealHealthTier.EXCELLENT, evaluation.tier)
        assertTrue(evaluation.isHealthyCelebrationWorthy)
        assertTrue(evaluation.highlightTagsTr.isNotEmpty())
    }

    @Test
    fun mealHealthEvaluator_ultraProcessedMeal_scoresBelow65AndDoesNotCelebrate() {
        val items = listOf(
            FoodItem(
                id = "u1",
                name = "Hamburger",
                formattedPortion = "1 Adet",
                calories = 520,
                proteinG = 22.0,
                carbsG = 45.0,
                fatG = 28.0,
                fiberG = 1.5
            ),
            FoodItem(
                id = "u2",
                name = "Patates Kızartması",
                formattedPortion = "1 Büyük Boy",
                calories = 420,
                proteinG = 4.0,
                carbsG = 54.0,
                fatG = 21.0,
                fiberG = 2.0
            ),
            FoodItem(
                id = "u3",
                name = "Kola",
                formattedPortion = "330ml",
                calories = 140,
                proteinG = 0.0,
                carbsG = 35.0,
                fatG = 0.0,
                fiberG = 0.0
            )
        )

        val evaluation = MealHealthEvaluator.evaluate(
            items = items,
            totalCalories = 1080,
            summary = "Hamburger, patates kızartması ve kola"
        )

        assertTrue(evaluation.score < 65, "Expected fast-food combo score < 65, got ${evaluation.score}")
        assertFalse(evaluation.isHealthyCelebrationWorthy)
    }

    @Test
    fun sportType_fromRawTextAndDetectAll_handlesTurkishUpperCaseAndDiacriticVariants() {
        val tennisInputs = listOf(
            "25 dk tenis",
            "25 dk Tenis",
            "25 dk TENİS",
            "25 dk tenıs",
            "25 dakika tenisi",
            "kortta 30 dk"
        )
        tennisInputs.forEach { input ->
            assertEquals(SportType.TENNIS, SportType.fromRawText(input), "Expected TENNIS for '$input'")
            assertTrue(SportType.TENNIS in SportType.detectAll(input), "Expected detectAll to include TENNIS for '$input'")
        }

        val turkishVariantPairs = listOf(
            "30 dk BİSİKLET" to SportType.CYCLING,
            "40 dk PİLATES" to SportType.YOGA_PILATES,
            "45 dk KALİSTENİK" to SportType.CALISTHENICS,
            "30 dk KOŞU" to SportType.RUNNING,
            "45 dk AĞIRLIK" to SportType.FITNESS,
            "30 dk YÜRÜYÜŞ" to SportType.WALKING,
            "30 dk YÜZME" to SportType.SWIMMING,
            "30 dk KÜREK" to SportType.ROWING,
            "30 dk DÖVÜŞ" to SportType.BOXING,
            "25 dk HIIT" to SportType.HIIT
        )
        turkishVariantPairs.forEach { (input, expected) ->
            assertEquals(expected, SportType.fromRawText(input), "Expected $expected for '$input'")
            assertTrue(expected in SportType.detectAll(input), "Expected detectAll to include $expected for '$input'")
        }
    }

    @Test
    fun sportType_fromWorkoutAndDetectAllForWorkout_fallsBackToAiAnalysisWhenRawTextLacksSport() {
        val fallbackWorkout = WorkoutLog(
            id = "w_fallback",
            userId = 1,
            date = "2026-11-16",
            rawText = "25 dk",
            caloriesBurned = 180,
            durationMinutes = 25,
            aiAnalysis = "Tenis antrenmanı için tahmini yakım"
        )
        assertEquals(SportType.TENNIS, SportType.fromWorkout(fallbackWorkout))
        assertEquals(setOf(SportType.TENNIS), SportType.detectAllForWorkout(fallbackWorkout))

        val explicitWorkout = WorkoutLog(
            id = "w_explicit",
            userId = 1,
            date = "2026-11-16",
            rawText = "25 dk TENİS",
            caloriesBurned = 180,
            durationMinutes = 25,
            aiAnalysis = "Koşu temposuna yakın kardiyo eforu"
        )
        assertEquals(SportType.TENNIS, SportType.fromWorkout(explicitWorkout))
        assertEquals(setOf(SportType.TENNIS), SportType.detectAllForWorkout(explicitWorkout))
    }

    @Test
    fun evaluate_historicalDeviceBackfill_doesNotCreate31DayStreakOnSignup() {
        val today = "2026-11-16"
        // Simulate 30 days of historical Health Connect / Apple Health workouts + daily steps synced on signup
        val backfilledWorkouts = (1..30).map { daysAgo ->
            val day = 16 - daysAgo
            val date = if (day >= 1) {
                "2026-11-${day.toString().padStart(2, '0')}"
            } else {
                "2026-10-${(31 + day).toString().padStart(2, '0')}"
            }
            WorkoutLog(
                id = "wdev_1_${date}_run_123",
                userId = 1,
                date = date,
                rawText = "30 dk Koşu",
                caloriesBurned = 260,
                durationMinutes = 30
            )
        } + WorkoutLog(
            id = "wdev_1_${today}_steps",
            userId = 1,
            date = today,
            rawText = "5000 Adım • Günlük Hareket",
            caloriesBurned = 180,
            durationMinutes = 50
        )

        val beforeAction = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = backfilledWorkouts,
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = today
        )
        assertEquals(0, beforeAction.currentStreakDays)

        // User logs 1 meal in HealthTrack today -> streak becomes 1 (not 31!)
        val afterFirstMeal = GamificationEngine.evaluate(
            foods = listOf(
                FoodLog(
                    id = "f_today",
                    userId = 1,
                    date = today,
                    rawText = "2 yumurta",
                    totalCalories = 180
                )
            ),
            workouts = backfilledWorkouts,
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = today
        )
        assertEquals(1, afterFirstMeal.currentStreakDays)
        assertTrue(afterFirstMeal.isActiveToday)
    }

    @Test
    fun mealHealthEvaluator_chocolateAndPopcorn_scoresLowEvenWithOptimisticExplicitScore() {
        val snackItems = listOf(
            FoodItem(
                id = "s1",
                name = "Çikolata",
                formattedPortion = "1 Paket (50g)",
                calories = 270,
                proteinG = 3.0,
                carbsG = 30.0,
                fatG = 15.0,
                fiberG = 1.5
            ),
            FoodItem(
                id = "s2",
                name = "Popcorn",
                formattedPortion = "1 Kase",
                calories = 200,
                proteinG = 3.0,
                carbsG = 24.0,
                fatG = 10.0,
                fiberG = 3.0
            )
        )

        val localEval = MealHealthEvaluator.evaluate(
            items = snackItems,
            totalCalories = 470,
            summary = "Çikolata ve popcorn"
        )
        assertTrue(localEval.score < 40, "Expected chocolate & popcorn score < 40, got ${localEval.score}")
        assertFalse(localEval.isHealthyCelebrationWorthy)

        val optimisticAiEval = MealHealthEvaluator.evaluate(
            items = snackItems,
            totalCalories = 470,
            summary = "Çikolata ve popcorn",
            explicitScore = 67
        )
        assertTrue(
            optimisticAiEval.score < 40,
            "Expected optimistic AI explicitScore=67 on junk snacks to be capped < 40, got ${optimisticAiEval.score}"
        )
    }
}

