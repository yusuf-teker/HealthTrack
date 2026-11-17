package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.database.SqliteHealthRecordStore
import com.yusufteker.healthtrack.core.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqliteMigrationAndWaterSyncTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun legacySettingsJsonIsMigratedOnceIntoSqliteWithoutDataLoss() = runTest {
        val tempDb = File.createTempFile("ht_migration_test_", ".db").apply { deleteOnExit() }
        val settings = MapSettings()

        val legacyFood = FoodLog(
            id = "food_legacy_1",
            userId = 7,
            date = "2026-11-10",
            rawText = "2 yumurta",
            totalCalories = 155,
            syncStatus = SyncStatus.SYNCED
        )
        val legacyWeight = WeightEntry(
            id = "weight_legacy_1",
            userId = 7,
            date = "2026-11-10",
            weightKg = 74.2,
            syncStatus = SyncStatus.PENDING
        )
        val legacyWater = WaterEntry(
            date = "2026-11-10",
            userId = 7,
            amountMl = 1500,
            targetMl = 2500,
            updatedAt = 1000L,
            syncStatus = SyncStatus.PENDING
        )
        val legacyGoal = NutritionGoal(
            id = "goal_7",
            userId = 7,
            calories = 2200,
            proteinG = 150.0,
            updatedAt = 1000L,
            syncStatus = SyncStatus.SYNCED
        )

        settings.putString("ht_db_foods", json.encodeToString(listOf(legacyFood)))
        settings.putString("ht_db_weights", json.encodeToString(listOf(legacyWeight)))
        settings.putString("ht_db_water", json.encodeToString(listOf(legacyWater)))
        settings.putString("ht_db_goals", json.encodeToString(mapOf(7 to legacyGoal)))

        val store1 = SqliteHealthRecordStore(tempDb.absolutePath)
        val db1 = HealthDatabase(settings = settings, recordStore = store1)

        assertTrue(settings.getBoolean("ht_sqlite_migrated_v1", false))
        assertEquals(1, db1.getAllFoods(7).size)
        assertEquals("food_legacy_1", db1.getAllFoods(7).first().id)
        assertEquals(74.2, db1.getWeights(7).first().weightKg)
        assertEquals(1500, db1.getWaterSync(7, "2026-11-10")?.amountMl)
        assertEquals(2200, db1.getNutritionGoalForSync(7)?.calories)

        // Add a new food directly in SQLite and close
        db1.upsertFood(
            FoodLog(
                id = "food_sqlite_2",
                userId = 7,
                date = "2026-11-11",
                rawText = "Yulaf",
                totalCalories = 320,
                syncStatus = SyncStatus.PENDING
            )
        )
        store1.close()

        // Reopen with same SQLite file and cleared legacy Settings keys (simulating post-migration app restart)
        settings.remove("ht_db_foods")
        settings.remove("ht_db_weights")
        settings.remove("ht_db_water")
        settings.remove("ht_db_goals")

        val store2 = SqliteHealthRecordStore(tempDb.absolutePath)
        val db2 = HealthDatabase(settings = settings, recordStore = store2)

        assertEquals(2, db2.getAllFoods(7).size)
        assertNotNull(db2.getFoodById("food_legacy_1"))
        assertNotNull(db2.getFoodById("food_sqlite_2"))
        assertEquals(74.2, db2.getWeights(7).first().weightKg)
        assertEquals(1500, db2.getWaterSync(7, "2026-11-10")?.amountMl)
        store2.close()
    }

    @Test
    fun waterSyncMarksPendingAndMergesRemoteLastWriteWins() = runTest {
        val settings = MapSettings()
        val store = SqliteHealthRecordStore(":memory:")
        val db = HealthDatabase(settings = settings, recordStore = store)

        val added = db.addWater(userId = 3, date = "2026-11-17", deltaMl = 500, defaultTargetMl = 2600)
        assertEquals(SyncStatus.PENDING, added.syncStatus)
        assertEquals(1, db.getPendingWaters(3).size)

        db.markWatersSynced(3, listOf("2026-11-17"))
        assertEquals(0, db.getPendingWaters(3).size)
        assertEquals(SyncStatus.SYNCED, db.getWaterSync(3, "2026-11-17")?.syncStatus)

        // Older remote update should not overwrite newer local entry
        val localUpdatedAt = db.getWaterSync(3, "2026-11-17")!!.updatedAt
        db.mergePullWaters(
            listOf(
                WaterEntry(
                    date = "2026-11-17",
                    userId = 3,
                    amountMl = 250,
                    targetMl = 2600,
                    updatedAt = localUpdatedAt - 5000L,
                    syncStatus = SyncStatus.SYNCED
                ),
                WaterEntry(
                    date = "2026-11-16",
                    userId = 3,
                    amountMl = 2000,
                    targetMl = 2500,
                    updatedAt = localUpdatedAt + 1000L,
                    syncStatus = SyncStatus.SYNCED
                )
            )
        )

        assertEquals(500, db.getWaterSync(3, "2026-11-17")?.amountMl)
        assertEquals(2000, db.getWaterSync(3, "2026-11-16")?.amountMl)
        store.close()
    }
}
