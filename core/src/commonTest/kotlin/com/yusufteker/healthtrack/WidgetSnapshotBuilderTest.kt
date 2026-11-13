package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.FoodLog
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.WidgetSnapshotBuilder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetSnapshotBuilderTest {

    @Test
    fun buildTodaySnapshot_loggedOut_returnsGuestSnapshotWithThemeAndLang() {
        val settings = MapSettings()
        settings.putBoolean("dark_mode", false)
        settings.putString("theme_accent", "emerald")
        LocalizationManager.setLanguage(AppLanguage.EN)

        val database = HealthDatabase(settings)
        val tokenStorage = TokenStorage(settings)
        val builder = WidgetSnapshotBuilder(database, tokenStorage, settings)

        val snapshot = builder.buildTodaySnapshot(userId = 0, todayIso = "2026-11-13")

        assertFalse(snapshot.isLoggedIn)
        assertEquals(0, snapshot.userId)
        assertEquals("2026-11-13", snapshot.dateIso)
        assertEquals("emerald", snapshot.themeAccentId)
        assertFalse(snapshot.isDarkTheme)
        assertEquals("en", snapshot.languageCode)
        assertEquals(0, snapshot.consumedCalories)
        assertEquals(0, snapshot.waterMl)

        LocalizationManager.setLanguage(AppLanguage.TR)
    }

    @Test
    fun buildTodaySnapshot_populated_calculatesMacrosBurnStepsWaterTargetAndStreak() = runTest {
        val settings = MapSettings()
        settings.putBoolean("dark_mode", true)
        settings.putString("theme_accent", "ocean")
        LocalizationManager.setLanguage(AppLanguage.TR)

        val database = HealthDatabase(settings)
        val tokenStorage = TokenStorage(settings)
        val userId = 42
        val today = "2026-11-13"

        tokenStorage.saveSession(
            userId = userId,
            name = "Test",
            username = "testuser",
            accessToken = "acc",
            refreshToken = "ref"
        )

        database.upsertNutritionGoal(
            NutritionGoal(
                id = "goal_$userId",
                userId = userId,
                calories = 2400,
                carbsG = 260.0,
                fatG = 75.0,
                fitnessGoal = FitnessGoal.BUILD_MUSCLE.id
            )
        )

        database.upsertWeight(
            WeightEntry(
                id = "w1",
                userId = userId,
                date = today,
                weightKg = 80.0,
                targetWeightKg = 82.0
            )
        )

        database.upsertFood(
            FoodLog(
                id = "f1",
                userId = userId,
                date = today,
                rawText = "Tavuk göğsü ve pirinç",
                totalCalories = 850,
                items = listOf(
                    FoodItem(
                        name = "Tavuk Göğsü",
                        formattedPortion = "200g",
                        calories = 330,
                        proteinG = 62.0,
                        carbsG = 0.0,
                        fatG = 7.0
                    ),
                    FoodItem(
                        name = "Pirinç Pilavı",
                        formattedPortion = "200g",
                        calories = 520,
                        proteinG = 8.0,
                        carbsG = 90.0,
                        fatG = 12.0
                    )
                )
            )
        )

        database.upsertWorkout(
            WorkoutLog(
                id = "wo_1",
                userId = userId,
                date = today,
                rawText = "Göğüs & Triceps Antrenmanı",
                caloriesBurned = 400,
                durationMinutes = 50
            )
        )

        // Daily step log: 8500 steps (220 kcal); 50m general workout estimates 50*60=3000 overlap steps -> net 5500/8500 * 220 = 142 kcal
        database.upsertWorkout(
            WorkoutLog(
                id = "${today}_steps",
                userId = userId,
                date = today,
                rawText = "8500 Adım • Günlük Hareket",
                caloriesBurned = 220,
                durationMinutes = 85
            )
        )

        database.addWater(userId = userId, date = today, deltaMl = 750)

        val builder = WidgetSnapshotBuilder(database, tokenStorage, settings)
        val snapshot = builder.buildTodaySnapshot(userId = userId, todayIso = today)

        assertTrue(snapshot.isLoggedIn)
        assertEquals(userId, snapshot.userId)
        assertEquals(850, snapshot.consumedCalories)
        assertEquals(2400, snapshot.targetCalories)
        assertEquals(70, snapshot.proteinG)
        // 80.0 kg * 1.4 (FitnessGoal.BUILD_MUSCLE) = 112 g
        assertEquals(112, snapshot.proteinTargetG)
        assertEquals(90, snapshot.carbsG)
        assertEquals(260, snapshot.carbsTargetG)
        assertEquals(19, snapshot.fatG)
        assertEquals(75, snapshot.fatTargetG)
        assertEquals(8500, snapshot.steps)
        assertEquals(10_000, snapshot.stepTarget)
        assertEquals(750, snapshot.waterMl)
        // Total burned = 400 + 220 = 620 -> (620 / 200) * 350 = 1050 extra -> 2500 + 1050 = 3550 ml
        assertEquals(3550, snapshot.waterTargetMl)
        // Effective burn = 400 + netStepBurn(142) = 542
        assertEquals(542, snapshot.burnedCalories)
        assertEquals(80.0, snapshot.latestWeightKg)
        assertEquals(82.0, snapshot.targetWeightKg)
        assertTrue(snapshot.streakDays >= 1)
        assertTrue(snapshot.isActiveToday)
    }

    @Test
    fun shouldShowDailyWidgetReminder_enforcesOncePerDayAndRespectsInstalledOrDisabled() {
        val settings = MapSettings()
        val tokenStorage = TokenStorage(settings)

        // Not logged in -> never show
        assertFalse(
            tokenStorage.shouldShowDailyWidgetReminder(
                todayIso = "2026-11-13",
                isWidgetInstalled = false
            )
        )

        tokenStorage.saveSession(
            userId = 7,
            name = "Ada",
            username = "ada",
            accessToken = "a",
            refreshToken = "r"
        )

        // First launch on 2026-11-13 with no widget -> true
        assertTrue(
            tokenStorage.shouldShowDailyWidgetReminder(
                todayIso = "2026-11-13",
                isWidgetInstalled = false
            )
        )

        // Once shown today -> false on same day
        tokenStorage.setLastWidgetReminderDate("2026-11-13")
        assertFalse(
            tokenStorage.shouldShowDailyWidgetReminder(
                todayIso = "2026-11-13",
                isWidgetInstalled = false
            )
        )

        // Next day -> true again if still not added
        assertTrue(
            tokenStorage.shouldShowDailyWidgetReminder(
                todayIso = "2026-11-14",
                isWidgetInstalled = false
            )
        )

        // If user already installed a widget -> false even on a new day
        assertFalse(
            tokenStorage.shouldShowDailyWidgetReminder(
                todayIso = "2026-11-15",
                isWidgetInstalled = true
            )
        )

        // If user opts out ("Bir daha gösterme") -> false
        tokenStorage.setWidgetReminderEnabled(false)
        assertFalse(
            tokenStorage.shouldShowDailyWidgetReminder(
                todayIso = "2026-11-16",
                isWidgetInstalled = false
            )
        )
    }
}

