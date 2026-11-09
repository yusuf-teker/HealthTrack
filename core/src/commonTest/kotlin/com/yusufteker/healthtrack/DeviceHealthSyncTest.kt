package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.SyncEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeviceHealthSyncTest {

    private fun createTestFixture(): Triple<HealthDatabase, TokenStorage, DeviceHealthSyncManager> {
        val settings = MapSettings()
        val tokenStorage = TokenStorage(settings).apply {
            saveSession(
                userId = 1,
                name = "Yusuf",
                username = "yusuf",
                accessToken = "test_token",
                refreshToken = "test_refresh"
            )
            setUserWeight(75.0)
            setUserTargetWeight(70.0)
            setUserFitnessGoal(FitnessGoal.BUILD_MUSCLE)
        }
        val database = HealthDatabase(settings)
        val apiClient = HealthApiClient(tokenStorage)
        val syncEngine = SyncEngine(database, apiClient, tokenStorage)
        val manager = DeviceHealthSyncManager(database, tokenStorage, syncEngine)
        return Triple(database, tokenStorage, manager)
    }

    @Test
    fun `same day multiple Xiaomi scale readings picks latest and carries body fat`() = runTest {
        val (database, tokenStorage, manager) = createTestFixture()

        val snapshot = DeviceHealthSnapshot(
            weights = listOf(
                DeviceWeightRecord(
                    externalId = "w1",
                    date = "2026-11-08",
                    weightKg = 74.8,
                    bodyFatPercent = 16.2,
                    measuredAtMillis = 1_700_000_000_000L,
                    sourceName = "Zepp Life (Xiaomi Tartı)"
                ),
                DeviceWeightRecord(
                    externalId = "w2",
                    date = "2026-11-08",
                    weightKg = 74.4,
                    bodyFatPercent = null, // Second quick weigh-in without impedance
                    measuredAtMillis = 1_700_000_060_000L,
                    sourceName = "Zepp Life (Xiaomi Tartı)"
                )
            ),
            platformName = "Health Connect"
        )

        val summary = manager.mergeSnapshot(snapshot, syncToCloud = false)
        assertEquals(1, summary.importedWeights)

        val savedWeights = database.getWeights(1)
        assertEquals(1, savedWeights.size)
        val entry = savedWeights.first()
        assertEquals(74.4, entry.weightKg)
        assertEquals(74.4, tokenStorage.getUserWeight())
        assertNotNull(entry.note)
        assertTrue(entry.note!!.contains("%16.2 Yağ"))
    }

    @Test
    fun `manual weight entered after scale measurement is not overwritten`() = runTest {
        val (database, _, manager) = createTestFixture()

        // User manually entered 73.5 kg at t = 2_000_000_000_000L
        database.upsertWeight(
            WeightEntry(
                id = "weight_1_2026-11-08",
                userId = 1,
                date = "2026-11-08",
                weightKg = 73.5,
                targetWeightKg = 70.0,
                note = "Aç karna",
                createdAt = 2_000_000_000_000L,
                updatedAt = 2_000_000_000_000L,
                syncStatus = SyncStatus.PENDING
            )
        )

        // Older scale measurement at t = 1_500_000_000_000L
        val snapshot = DeviceHealthSnapshot(
            weights = listOf(
                DeviceWeightRecord(
                    externalId = "w_old",
                    date = "2026-11-08",
                    weightKg = 74.9,
                    bodyFatPercent = 15.8,
                    measuredAtMillis = 1_500_000_000_000L,
                    sourceName = "Zepp Life (Xiaomi Tartı)"
                )
            )
        )

        manager.mergeSnapshot(snapshot, syncToCloud = false)

        val entry = database.getWeights(1).first()
        // Manual weight 73.5 is preserved, while note is enriched with body fat
        assertEquals(73.5, entry.weightKg)
        assertTrue(entry.note!!.contains("Aç karna"))
        assertTrue(entry.note!!.contains("%15.8 Yağ"))
    }

    @Test
    fun `Huawei Watch and Zepp duplicate workout sessions in same 5 min bucket are deduplicated`() = runTest {
        val (database, _, manager) = createTestFixture()

        val baseTime = 1_700_000_000_000L
        val snapshot = DeviceHealthSnapshot(
            workouts = listOf(
                // From Huawei Health with full heart rate & calories
                DeviceWorkoutRecord(
                    externalId = "hw_1",
                    date = "2026-11-08",
                    category = DeviceExerciseCategory.RUNNING,
                    durationMinutes = 40,
                    caloriesBurned = 385,
                    distanceMeters = 5200.0,
                    avgHeartRateBpm = 148,
                    startTimeMillis = baseTime,
                    endTimeMillis = baseTime + 40 * 60_000L,
                    sourceName = "Huawei Health"
                ),
                // Duplicate bridged record 30 seconds later with fewer metrics
                DeviceWorkoutRecord(
                    externalId = "fit_1",
                    date = "2026-11-08",
                    category = DeviceExerciseCategory.RUNNING,
                    durationMinutes = 40,
                    caloriesBurned = 360,
                    distanceMeters = null,
                    avgHeartRateBpm = null,
                    startTimeMillis = baseTime + 30_000L,
                    endTimeMillis = baseTime + 40 * 60_000L,
                    sourceName = "Google Fit"
                )
            )
        )

        val summary = manager.mergeSnapshot(snapshot, syncToCloud = false)
        assertEquals(1, summary.importedWorkouts)

        val workouts = database.getAllWorkoutsForSync(1).filter { !it.isDeleted }
        assertEquals(1, workouts.size)
        val saved = workouts.first()
        assertEquals(385, saved.caloriesBurned)
        assertTrue(saved.aiAnalysis!!.contains("Huawei Health"))
        assertTrue(saved.aiAnalysis!!.contains("5.2 km"))
        assertTrue(saved.aiAnalysis!!.contains("148 bpm"))

        // Second sync of exact same snapshot is idempotent (0 new, 1 skipped)
        val secondSummary = manager.mergeSnapshot(snapshot, syncToCloud = false)
        assertEquals(0, secondSummary.importedWorkouts)
        assertEquals(0, secondSummary.updatedWorkouts)
        assertEquals(1, secondSummary.skippedWorkouts)
    }

    @Test
    fun `deleted device workout tombstone prevents re-importing`() = runTest {
        val (database, _, manager) = createTestFixture()

        val baseTime = 1_700_000_000_000L
        val snapshot = DeviceHealthSnapshot(
            workouts = listOf(
                DeviceWorkoutRecord(
                    externalId = "hw_del",
                    date = "2026-11-08",
                    category = DeviceExerciseCategory.CYCLING,
                    durationMinutes = 30,
                    caloriesBurned = 250,
                    startTimeMillis = baseTime,
                    endTimeMillis = baseTime + 30 * 60_000L,
                    sourceName = "Huawei Health"
                )
            )
        )

        manager.mergeSnapshot(snapshot, syncToCloud = false)
        val created = database.getAllWorkoutsForSync(1).first()
        database.deleteWorkout(created.id)

        // Sync again: should respect user's deletion and skip
        val summaryAfterDelete = manager.mergeSnapshot(snapshot, syncToCloud = false)
        assertEquals(0, summaryAfterDelete.importedWorkouts)
        assertEquals(1, summaryAfterDelete.skippedWorkouts)
        assertTrue(database.getAllWorkoutsForSync(1).none { !it.isDeleted })
    }
}

