package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.database.SettingsHealthRecordStore
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.TokenStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuestModeMigrationTest {

    @Test
    fun testGuestSessionStartsAndUpgradesWithPreservedPreferences() {
        val settings = MapSettings()
        val storage = TokenStorage(settings)

        storage.startGuestSession("Misafir")
        assertTrue(storage.isGuest())
        assertTrue(storage.hasActiveSession())
        assertFalse(storage.isLoggedIn())
        assertFalse(storage.isAuthenticated())
        assertEquals(UserSession.GUEST_USER_ID, storage.getUserId())

        storage.setUserWeight(78.5)
        storage.setUserTargetWeight(72.0)
        storage.setUserHeight(180.0)
        storage.setUserFitnessGoal(FitnessGoal.LOSE_WEIGHT)
        storage.hideFrequentFoodName(UserSession.GUEST_USER_ID, "ekmek")

        // Reloading TokenStorage should restore guest session
        val reloaded = TokenStorage(settings)
        assertTrue(reloaded.isGuest())
        assertTrue(reloaded.sessionFlow.value.hasActiveSession)
        assertFalse(reloaded.sessionFlow.value.isAuthenticated)

        // Upgrading guest -> real user preserving guest preferences
        reloaded.saveSession(
            userId = 42,
            name = "Yusuf",
            username = "yusuf",
            accessToken = "real-token",
            refreshToken = "real-refresh",
            preserveGuestPreferences = true
        )

        assertFalse(reloaded.isGuest())
        assertTrue(reloaded.isAuthenticated())
        assertTrue(reloaded.isLoggedIn())
        assertEquals(42, reloaded.getUserId())
        assertEquals(78.5, reloaded.getUserWeight())
        assertEquals(72.0, reloaded.getUserTargetWeight())
        assertEquals(180.0, reloaded.getUserHeight())
        assertEquals(FitnessGoal.LOSE_WEIGHT, reloaded.getUserFitnessGoal())
        assertTrue(reloaded.getHiddenFrequentFoodNames(42).contains("ekmek"))
        assertTrue(reloaded.getHiddenFrequentFoodNames(UserSession.GUEST_USER_ID).isEmpty())
    }

    @Test
    fun testMigrateGuestDataToUserTransfersAllRecordsAsPending() = runTest {
        val settings = MapSettings()
        val db = HealthDatabase(
            settings = settings,
            recordStore = SettingsHealthRecordStore(settings)
        )
        val guestId = UserSession.GUEST_USER_ID
        val targetId = 99

        db.upsertFood(
            FoodLog(
                id = "food_guest_1",
                userId = guestId,
                date = "2026-11-19",
                rawText = "2 yumurta",
                totalCalories = 160,
                syncStatus = SyncStatus.SYNCED
            )
        )
        db.upsertWorkout(
            WorkoutLog(
                id = "workout_guest_1",
                userId = guestId,
                date = "2026-11-19",
                rawText = "30 dk koşu",
                durationMinutes = 30,
                caloriesBurned = 300,
                syncStatus = SyncStatus.SYNCED
            )
        )
        db.upsertWeight(
            WeightEntry(
                id = "weight_guest_1",
                userId = guestId,
                date = "2026-11-19",
                weightKg = 77.4,
                targetWeightKg = 72.0,
                syncStatus = SyncStatus.SYNCED
            )
        )
        db.addWater(guestId, "2026-11-19", 750, 2500)
        db.upsertNutritionGoal(
            NutritionGoal(
                id = "goal_$guestId",
                userId = guestId,
                calories = 2100,
                proteinG = 155.0,
                fitnessGoal = FitnessGoal.LOSE_WEIGHT.id,
                syncStatus = SyncStatus.SYNCED
            )
        )
        db.upsertCustomFood(
            CustomFoodItem(
                id = "custom_guest_1",
                userId = guestId,
                name = "Ev Yapımı Granola",
                portionUnit = "100g",
                calories = 420
            )
        )
        db.toggleFavoriteFood(guestId, "fav_oatmeal")

        assertTrue(db.hasDataForUser(guestId))
        assertFalse(db.hasDataForUser(targetId))

        val summary = db.migrateGuestDataToUser(
            guestUserId = guestId,
            targetUserId = targetId,
            preferGuestGoal = true
        )

        assertEquals(1, summary.foodsMigrated)
        assertEquals(1, summary.workoutsMigrated)
        assertEquals(1, summary.weightsMigrated)
        assertEquals(1, summary.watersMigrated)
        assertEquals(1, summary.customFoodsMigrated)
        assertTrue(summary.goalMigrated)
        assertEquals(1, summary.favoritesMigrated)

        assertFalse(db.hasDataForUser(guestId))
        assertTrue(db.hasDataForUser(targetId))

        // Verify all syncable migrated entities are marked PENDING for SyncEngine push
        assertEquals(1, db.getPendingFoods(targetId).size)
        assertEquals(1, db.getPendingWorkouts(targetId).size)
        assertEquals(1, db.getPendingWeights(targetId).size)
        assertEquals(1, db.getPendingWaters(targetId).size)
        assertEquals(750, db.getWaterSync(targetId, "2026-11-19")?.amountMl)
        assertEquals(2100, db.getPendingNutritionGoal(targetId)?.calories)
    }
}
