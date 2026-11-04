package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.FoodLog
import com.yusufteker.healthtrack.core.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncEngineLogicTest {

    @Test
    fun testOfflineFirstInsertAndSyncStatus() = runTest {
        val memorySettings = MapSettings()
        val database = HealthDatabase(memorySettings)

        val food = FoodLog(
            id = "offline_food_uuid_1",
            userId = 1,
            date = "2026-11-04",
            rawText = "1 elma",
            totalCalories = 80,
            updatedAt = 1000L,
            syncStatus = SyncStatus.PENDING
        )

        database.upsertFood(food)

        val pending = database.getPendingFoods(1)
        assertEquals(1, pending.size)
        assertEquals("offline_food_uuid_1", pending[0].id)
        assertEquals(SyncStatus.PENDING, pending[0].syncStatus)

        // After successful sync push:
        database.markFoodsSynced(listOf("offline_food_uuid_1"))

        val pendingAfterSync = database.getPendingFoods(1)
        assertTrue(pendingAfterSync.isEmpty())

        val foodsObserved = database.observeFoods(1, "2026-11-04").first()
        assertEquals(1, foodsObserved.size)
        assertEquals(SyncStatus.SYNCED, foodsObserved[0].syncStatus)
    }

    @Test
    fun testLastWriteWinsConflictResolution() = runTest {
        val memorySettings = MapSettings()
        val database = HealthDatabase(memorySettings)

        val localFood = FoodLog(
            id = "conflict_food_1",
            userId = 1,
            date = "2026-11-04",
            rawText = "Yerel değişiklik",
            totalCalories = 100,
            updatedAt = 2000L,
            syncStatus = SyncStatus.PENDING
        )
        database.upsertFood(localFood)

        // Case 1: Remote update is older (updatedAt = 1500L < 2000L). Local must win!
        val olderRemoteFood = localFood.copy(
            rawText = "Eski sunucu verisi",
            totalCalories = 90,
            updatedAt = 1500L
        )
        database.mergePullFoods(listOf(olderRemoteFood))

        var current = database.getFoodById("conflict_food_1")
        assertEquals("Yerel değişiklik", current?.rawText)
        assertEquals(100, current?.totalCalories)

        // Case 2: Remote update is newer (updatedAt = 3000L > 2000L). Remote must win!
        val newerRemoteFood = localFood.copy(
            rawText = "Daha yeni sunucu verisi",
            totalCalories = 250,
            updatedAt = 3000L
        )
        database.mergePullFoods(listOf(newerRemoteFood))

        current = database.getFoodById("conflict_food_1")
        assertEquals("Daha yeni sunucu verisi", current?.rawText)
        assertEquals(250, current?.totalCalories)
        assertEquals(SyncStatus.SYNCED, current?.syncStatus)
    }
}
