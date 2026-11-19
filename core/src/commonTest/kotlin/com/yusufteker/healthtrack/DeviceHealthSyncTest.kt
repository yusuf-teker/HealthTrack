package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.GamificationEngine
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
        assertEquals(16.2, entry.bodyFatPercent)
        assertNotNull(entry.muscleMassKg)
        assertNotNull(entry.musclePercent)
        assertTrue(entry.musclePercent > 50.0)
        assertEquals(74.4, tokenStorage.getUserWeight())
        assertNotNull(entry.note)
        assertTrue(entry.note.contains("Zepp Life"))
    }

    @Test
    fun `exact leanBodyMass minus boneMass derives exact muscle mass and percentage`() = runTest {
        val (database, _, manager) = createTestFixture()

        val snapshot = DeviceHealthSnapshot(
            weights = listOf(
                DeviceWeightRecord(
                    externalId = "hc_exact_1",
                    date = "2026-11-09",
                    weightKg = 80.0,
                    bodyFatPercent = 20.0,
                    leanBodyMassKg = 64.0,
                    boneMassKg = 3.2,
                    measuredAtMillis = 1_700_050_000_000L,
                    sourceName = "Health Connect Tartı"
                )
            ),
            platformName = "Health Connect"
        )

        val summary = manager.mergeSnapshot(snapshot, syncToCloud = false)
        assertEquals(1, summary.importedWeights)

        val entry = database.getWeights(1).first()
        assertEquals(80.0, entry.weightKg)
        assertEquals(20.0, entry.bodyFatPercent)
        assertEquals(60.8, entry.muscleMassKg)
        assertEquals(76.0, entry.musclePercent)
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
        // Manual weight 73.5 is preserved, while structured body fat & muscle are enriched
        assertEquals(73.5, entry.weightKg)
        assertTrue(entry.note!!.contains("Aç karna"))
        assertEquals(15.8, entry.bodyFatPercent)
        assertNotNull(entry.muscleMassKg)
        assertNotNull(entry.musclePercent)
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

    @Test
    fun `Apple Health Huawei Watch tennis yoga and daily steps sync preserves telemetry and updates idempotently`() = runTest {
        val (database, _, manager) = createTestFixture()

        val baseTime = 1_700_000_000_000L
        val tennis = DeviceWorkoutRecord(
            externalId = "hk_tennis_1",
            date = "2026-11-15",
            category = DeviceExerciseCategory.TENNIS,
            durationMinutes = 60,
            caloriesBurned = 460,
            distanceMeters = 3100.0,
            steps = 4150,
            avgHeartRateBpm = 144,
            startTimeMillis = baseTime,
            endTimeMillis = baseTime + 60 * 60_000L,
            sourcePackage = "com.huawei.health",
            sourceName = "Huawei Health"
        )
        val yoga = DeviceWorkoutRecord(
            externalId = "hk_yoga_1",
            date = "2026-11-15",
            category = DeviceExerciseCategory.YOGA,
            durationMinutes = 45,
            caloriesBurned = 155,
            avgHeartRateBpm = 102,
            startTimeMillis = baseTime + 3 * 3_600_000L,
            endTimeMillis = baseTime + 3 * 3_600_000L + 45 * 60_000L,
            sourcePackage = "com.huawei.health",
            sourceName = "Huawei Health"
        )
        val dailySteps = DeviceWorkoutRecord(
            externalId = "hk_steps_2026-11-15",
            date = "2026-11-15",
            category = DeviceExerciseCategory.DAILY_STEPS,
            customTitle = "6200 Adım • Günlük Hareket",
            durationMinutes = 62,
            caloriesBurned = 210,
            distanceMeters = 4650.0,
            steps = 6200,
            avgHeartRateBpm = 86,
            startTimeMillis = baseTime + 6 * 3_600_000L,
            endTimeMillis = baseTime + 12 * 3_600_000L,
            sourcePackage = "com.huawei.health",
            sourceName = "Huawei Health"
        )

        val initialSnapshot = DeviceHealthSnapshot(
            workouts = listOf(tennis, yoga, dailySteps),
            platformName = "Apple Health"
        )

        val firstSummary = manager.mergeSnapshot(initialSnapshot, syncToCloud = false)
        assertEquals(3, firstSummary.importedWorkouts)
        assertEquals(0, firstSummary.updatedWorkouts)
        assertEquals("Apple Health", firstSummary.platformName)

        val savedWorkouts = database.getAllWorkoutsForSync(1).filter { !it.isDeleted }
        assertEquals(3, savedWorkouts.size)

        val savedTennis = savedWorkouts.first { it.rawText.contains("Tenis", ignoreCase = true) || it.rawText.contains("Tennis", ignoreCase = true) }
        assertEquals(460, savedTennis.caloriesBurned)
        assertTrue(savedTennis.aiAnalysis!!.contains("Huawei Health"))
        assertTrue(savedTennis.aiAnalysis!!.contains("3.1 km"))
        assertTrue(savedTennis.aiAnalysis!!.contains("4150"))
        assertTrue(savedTennis.aiAnalysis!!.contains("144 bpm"))

        val savedYoga = savedWorkouts.first { it.rawText.contains("Yoga", ignoreCase = true) }
        assertEquals(155, savedYoga.caloriesBurned)
        assertTrue(savedYoga.aiAnalysis!!.contains("102 bpm"))

        val savedSteps = savedWorkouts.first { it.id.endsWith("_steps") }
        assertEquals(210, savedSteps.caloriesBurned)
        assertTrue(savedSteps.aiAnalysis!!.contains("6200"))

        // Repeat exact same snapshot -> 0 imported, 0 updated, 3 skipped
        val repeatSummary = manager.mergeSnapshot(initialSnapshot, syncToCloud = false)
        assertEquals(0, repeatSummary.importedWorkouts)
        assertEquals(0, repeatSummary.updatedWorkouts)
        assertEquals(3, repeatSummary.skippedWorkouts)

        // Watch syncs more steps later that day -> updates existing DAILY_STEPS record cleanly
        val laterSteps = dailySteps.copy(
            customTitle = "8400 Adım • Günlük Hareket",
            durationMinutes = 84,
            caloriesBurned = 290,
            distanceMeters = 6300.0,
            steps = 8400
        )
        val updateSummary = manager.mergeSnapshot(
            DeviceHealthSnapshot(
                workouts = listOf(tennis, yoga, laterSteps),
                platformName = "Apple Health"
            ),
            syncToCloud = false
        )
        assertEquals(0, updateSummary.importedWorkouts)
        assertEquals(1, updateSummary.updatedWorkouts)
        assertEquals(2, updateSummary.skippedWorkouts)

        val updatedSteps = database.getAllWorkoutsForSync(1).first { it.id.endsWith("_steps") }
        assertEquals(290, updatedSteps.caloriesBurned)
        assertEquals(84, updatedSteps.durationMinutes)
        assertTrue(updatedSteps.aiAnalysis!!.contains("8400"))
    }

    @Test
    fun `smartwatch workouts across categories and generic custom titles unlock corresponding sport badges`() = runTest {
        val (database, _, manager) = createTestFixture()
        val baseTime = 1_700_000_000_000L

        val categoriesToBadges = listOf(
            DeviceExerciseCategory.VOLLEYBALL to BadgeId.SPORT_VOLLEYBALL,
            DeviceExerciseCategory.CALISTHENICS to BadgeId.SPORT_CALISTHENICS,
            DeviceExerciseCategory.DANCE to BadgeId.SPORT_DANCE,
            DeviceExerciseCategory.TENNIS to BadgeId.SPORT_TENNIS,
            DeviceExerciseCategory.BASKETBALL to BadgeId.SPORT_BASKETBALL,
            DeviceExerciseCategory.FOOTBALL to BadgeId.SPORT_FOOTBALL,
            DeviceExerciseCategory.MARTIAL_ARTS to BadgeId.SPORT_BOXING,
            DeviceExerciseCategory.HIIT to BadgeId.SPORT_HIIT,
            DeviceExerciseCategory.CYCLING to BadgeId.SPORT_CYCLING,
            DeviceExerciseCategory.SWIMMING to BadgeId.SPORT_SWIMMING,
            DeviceExerciseCategory.WALKING to BadgeId.SPORT_WALKING,
            DeviceExerciseCategory.YOGA to BadgeId.SPORT_YOGA_PILATES,
            DeviceExerciseCategory.ROWING to BadgeId.SPORT_ROWING,
            DeviceExerciseCategory.STRENGTH to BadgeId.SPORT_FITNESS,
            DeviceExerciseCategory.RUNNING to BadgeId.SPORT_RUNNING
        )

        val watchWorkouts = categoriesToBadges.mapIndexed { idx, (category, _) ->
            DeviceWorkoutRecord(
                externalId = "watch_cat_$idx",
                date = "2026-11-16",
                category = category,
                // Even if watch app assigns a generic custom title, category suffix preserves sport detection
                customTitle = if (category == DeviceExerciseCategory.VOLLEYBALL) "Sabah Seansı" else null,
                durationMinutes = 40,
                caloriesBurned = 300,
                startTimeMillis = baseTime + idx * 3_600_000L,
                endTimeMillis = baseTime + idx * 3_600_000L + 40 * 60_000L,
                sourceName = "Huawei Health"
            )
        }

        val syncSummary = manager.mergeSnapshot(
            DeviceHealthSnapshot(workouts = watchWorkouts, platformName = "Health Connect"),
            syncToCloud = false
        )
        assertEquals(15, syncSummary.importedWorkouts)

        val savedWorkouts = database.getAllWorkoutsForSync(1).filter { !it.isDeleted }
        val gamification = GamificationEngine.evaluate(
            foods = emptyList(),
            workouts = savedWorkouts,
            weights = emptyList(),
            waters = emptyList(),
            nutritionGoal = NutritionGoal(id = "g1", userId = 1),
            todayIso = "2026-11-16",
            hasConnectedDevice = true
        )

        categoriesToBadges.forEach { (category, expectedBadge) ->
            val ach = gamification.achievements.first { it.badgeId == expectedBadge }
            assertTrue(ach.isUnlocked, "Expected $category from watch to unlock $expectedBadge")
            assertEquals(1, ach.currentLevel)
        }
    }
}

