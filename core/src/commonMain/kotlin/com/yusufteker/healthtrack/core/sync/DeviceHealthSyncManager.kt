package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.utils.BodyCompositionCalculator
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Conflict-free merger that reconciles smart scale (Xiaomi / Zepp Life / Mi Fitness)
 * and smartwatch (Huawei Watch / Apple Watch / WearOS) telemetry into [HealthDatabase].
 *
 * Conflict Prevention Rules:
 * 1. Tombstone Guard: Never resurrects a WeightEntry or WorkoutLog that the user manually deleted in the app.
 * 2. Same-Day Weight Deduplication: Groups multiple scale measurements on the same day and selects the latest timestamp.
 * 3. Manual Override Protection: If the user manually entered a weight in the app *after* the scale measurement timestamp,
 *    the user's manual weight is preserved; if the scale measurement is newer, it updates the day's entry cleanly.
 * 4. Cross-Bridge Workout Deduplication: Uses a 5-minute start-time bucket + exercise category fingerprint so if both
 *    Huawei Health and Mi Fitness / Google Fit write the same workout session to Health Connect, calories are never double-counted.
 * 5. Manual/AI Workout Overlap Detection: Skips importing a device workout if an active manual/AI workout on the same day
 *    already matches the activity title/category and has similar duration/calories.
 * 6. MET Calorie Fallback: If a smartwatch logs an exercise session with 0 kcal, automatically estimates burned calories
 *    using the exercise's MET factor, duration, and the user's current weight.
 */
class DeviceHealthSyncManager(
    private val database: HealthDatabase,
    private val tokenStorage: TokenStorage,
    private val syncEngine: SyncEngine,
    private val gamificationManager: GamificationManager? = null
) {
    private val mutex = Mutex()
    private val isoDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

    suspend fun mergeSnapshot(
        snapshot: DeviceHealthSnapshot,
        syncToCloud: Boolean = true
    ): DeviceSyncSummary = performMergeSnapshot(snapshot, syncToCloud)

    private suspend fun performMergeSnapshot(
        snapshot: DeviceHealthSnapshot,
        syncToCloud: Boolean
    ): DeviceSyncSummary = mutex.withLock {
        val userId = tokenStorage.getUserId().takeIf { it > 0 } ?: 1
        val now = currentTimeMillis()

        var importedWeights = 0
        var updatedWeights = 0
        var skippedWeights = 0

        var importedWorkouts = 0
        var updatedWorkouts = 0
        var skippedWorkouts = 0

        var latestWeightEntry: WeightEntry? = null
        val touchedWorkoutDates = mutableSetOf<String>()

        val applyDeviceData: suspend () -> Unit = {
        // ─────────────────────────────────────────────────────────────
        // 1. MERGE SMART SCALE WEIGHTS & BODY COMPOSITION (FAT + MUSCLE)
        // ─────────────────────────────────────────────────────────────
        val allLocalWeights = database.getAllWeightsForSync(userId)
        val localWeightsByDate = allLocalWeights.groupBy { it.date }
        val defaultTargetWeight = tokenStorage.getUserTargetWeight().takeIf { it > 0.0 } ?: 70.0
        val isMale = !tokenStorage.getUserGender().equals("FEMALE", ignoreCase = true)

        // Group incoming scale records by YYYY-MM-DD, pick the latest measurement per day, and carry body comp
        val validDeviceWeightsByDate = snapshot.weights
            .filter { isoDateRegex.matches(it.date) && it.weightKg in 20.0..350.0 }
            .groupBy { it.date }
            .mapValues { (_, records) ->
                val latest = records.maxByOrNull { it.measuredAtMillis } ?: records.first()
                val roundedWeight = roundOneDecimal(latest.weightKg)
                val bodyFat = latest.bodyFatPercent ?: records.mapNotNull { it.bodyFatPercent }.lastOrNull()
                val leanMass = latest.leanBodyMassKg ?: records.mapNotNull { it.leanBodyMassKg }.lastOrNull()
                val boneMass = latest.boneMassKg ?: records.mapNotNull { it.boneMassKg }.lastOrNull()
                val muscleMass = latest.muscleMassKg ?: records.mapNotNull { it.muscleMassKg }.lastOrNull()
                val musclePct = latest.musclePercent ?: records.mapNotNull { it.musclePercent }.lastOrNull()

                val resolved = BodyCompositionCalculator.resolveFromDevice(
                    weightKg = roundedWeight,
                    bodyFatPercent = bodyFat,
                    leanBodyMassKg = leanMass,
                    boneMassKg = boneMass,
                    muscleMassKg = muscleMass,
                    musclePercent = musclePct,
                    isMale = isMale
                )

                latest.copy(
                    weightKg = roundedWeight,
                    bodyFatPercent = resolved.bodyFatPercent,
                    leanBodyMassKg = resolved.leanBodyMassKg,
                    boneMassKg = resolved.boneMassKg,
                    muscleMassKg = resolved.muscleMassKg,
                    musclePercent = resolved.musclePercent
                )
            }

        for ((date, deviceRecord) in validDeviceWeightsByDate) {
            val existingForDate = localWeightsByDate[date].orEmpty()
            val activeLocal = existingForDate.filter { !it.isDeleted }.maxByOrNull { it.updatedAt }
            val deletedTombstone = existingForDate.filter { it.isDeleted }.maxByOrNull { it.updatedAt }

            // Rule 1.1: Tombstone Guard — if user deleted this day's weight after the scale measurement, respect deletion
            if (activeLocal == null && deletedTombstone != null && deletedTombstone.updatedAt >= deviceRecord.measuredAtMillis) {
                skippedWeights++
                continue
            }

            val deviceNote = buildScaleNote(deviceRecord)

            if (activeLocal == null) {
                // Brand new day entry from smart scale
                val entry = WeightEntry(
                    id = "weight_${userId}_${date}",
                    userId = userId,
                    date = date,
                    weightKg = deviceRecord.weightKg,
                    targetWeightKg = defaultTargetWeight,
                    bodyFatPercent = deviceRecord.bodyFatPercent,
                    muscleMassKg = deviceRecord.muscleMassKg,
                    musclePercent = deviceRecord.musclePercent,
                    note = deviceNote,
                    createdAt = deviceRecord.measuredAtMillis.takeIf { it > 0L } ?: now,
                    updatedAt = maxOf(deviceRecord.measuredAtMillis, now),
                    isDeleted = false,
                    syncStatus = SyncStatus.PENDING
                )
                database.upsertWeight(entry)
                importedWeights++
            } else {
                val legacyBodyComp = BodyCompositionCalculator.parseLegacyBodyCompositionFromNote(
                    note = activeLocal.note,
                    weightKg = activeLocal.weightKg,
                    isMale = isMale
                )
                val currentFat = activeLocal.bodyFatPercent ?: legacyBodyComp?.bodyFatPercent
                val currentMuscleKg = activeLocal.muscleMassKg ?: legacyBodyComp?.muscleMassKg
                val currentMusclePct = activeLocal.musclePercent ?: legacyBodyComp?.musclePercent

                val isSameWeight = abs(activeLocal.weightKg - deviceRecord.weightKg) < 0.05
                val targetFat = deviceRecord.bodyFatPercent ?: currentFat
                val targetMuscleKg = deviceRecord.muscleMassKg ?: currentMuscleKg
                val targetMusclePct = deviceRecord.musclePercent ?: currentMusclePct

                val hasNewBodyCompInfo =
                    (targetFat != null && (activeLocal.bodyFatPercent == null || abs(activeLocal.bodyFatPercent - targetFat) >= 0.05)) ||
                    (targetMuscleKg != null && (activeLocal.muscleMassKg == null || abs(activeLocal.muscleMassKg - targetMuscleKg) >= 0.05)) ||
                    (targetMusclePct != null && (activeLocal.musclePercent == null || abs(activeLocal.musclePercent - targetMusclePct) >= 0.05))

                if (isSameWeight && !hasNewBodyCompInfo) {
                    // Already identical -> skip without dirtying syncStatus
                    skippedWeights++
                } else if (deviceRecord.measuredAtMillis > activeLocal.updatedAt || isAutoGeneratedWeightNote(activeLocal.note)) {
                    // Scale reading is newer than local entry (or local entry was an auto-generated placeholder)
                    val cleanedExistingNote = BodyCompositionCalculator.stripLegacyBodyCompFromNote(activeLocal.note)
                    val mergedNote = mergeWeightNotes(cleanedExistingNote, deviceNote)
                    val updatedEntry = activeLocal.copy(
                        weightKg = deviceRecord.weightKg,
                        targetWeightKg = activeLocal.targetWeightKg ?: defaultTargetWeight,
                        bodyFatPercent = targetFat,
                        muscleMassKg = targetMuscleKg,
                        musclePercent = targetMusclePct,
                        note = mergedNote,
                        updatedAt = maxOf(deviceRecord.measuredAtMillis, now),
                        syncStatus = SyncStatus.PENDING
                    )
                    database.upsertWeight(updatedEntry)
                    updatedWeights++
                } else if (hasNewBodyCompInfo) {
                    // User entered weight manually later in the day: keep manual weightKg & note, enrich missing composition
                    val cleanedExistingNote = BodyCompositionCalculator.stripLegacyBodyCompFromNote(activeLocal.note)
                    val mergedNote = mergeWeightNotes(cleanedExistingNote, deviceNote)
                    val updatedEntry = activeLocal.copy(
                        bodyFatPercent = activeLocal.bodyFatPercent ?: targetFat,
                        muscleMassKg = activeLocal.muscleMassKg ?: targetMuscleKg,
                        musclePercent = activeLocal.musclePercent ?: targetMusclePct,
                        note = mergedNote,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING
                    )
                    database.upsertWeight(updatedEntry)
                    updatedWeights++
                } else {
                    // User's manual weight is newer -> keep user's entry
                    skippedWeights++
                }
            }
        }

        // Update current user weight & dynamic protein goal if weights changed
        val activeWeightsAfterMerge = database.getWeights(userId).sortedBy { it.date }
        val resolvedLatestWeight = activeWeightsAfterMerge.lastOrNull()
        latestWeightEntry = resolvedLatestWeight
        if (resolvedLatestWeight != null && (importedWeights > 0 || updatedWeights > 0)) {
            tokenStorage.setUserWeight(resolvedLatestWeight.weightKg)
            val currentGoal = database.observeNutritionGoal(userId).firstOrNull()
            if (currentGoal != null) {
                val fitnessGoal = tokenStorage.getUserFitnessGoal()
                val newProtein = calculateProteinTarget(resolvedLatestWeight.weightKg, fitnessGoal)
                if (abs(currentGoal.proteinG - newProtein) >= 0.5) {
                    database.upsertNutritionGoal(
                        currentGoal.copy(
                            proteinG = newProtein,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                    )
                }
            }
        }

        val effectiveUserWeightKg = resolvedLatestWeight?.weightKg
            ?: tokenStorage.getUserWeight().takeIf { it > 0.0 }
            ?: 75.0

        // ─────────────────────────────────────────────────────────────
        // 2. MERGE SMARTWATCH WORKOUTS & CALORIES
        // ─────────────────────────────────────────────────────────────
        database.deduplicateAndSanitizeLocalData(userId, isoDateRegex)
        val allLocalWorkouts = database.getAllWorkoutsForSync(userId)
        val localWorkoutsById = allLocalWorkouts.associateBy { it.id }.toMutableMap()
        val activeWorkoutsByDate = allLocalWorkouts
            .filter { !it.isDeleted }
            .groupBy { it.date }
            .mapValues { it.value.toMutableList() }
            .toMutableMap()

        // Deduplicate incoming device workouts that share the same 5-minute start bucket & category
        // (e.g. when both Huawei Health and Zepp/Google Fit report the exact same watch session)
        val deduplicatedDeviceWorkouts = snapshot.workouts
            .filter {
                isoDateRegex.matches(it.date) &&
                    (it.durationMinutes > 0 || it.caloriesBurned > 0 || (it.steps ?: 0) >= 20)
            }
            .groupBy { record ->
                if (record.category == DeviceExerciseCategory.DAILY_STEPS) {
                    "${record.date}_DAILY_STEPS"
                } else {
                    val fiveMinBucket = record.startTimeMillis / 300_000L
                    "${record.date}_${record.category.name}_$fiveMinBucket"
                }
            }
            .map { (_, candidates) ->
                // Pick the candidate with the richest telemetry (highest steps for DAILY_STEPS, or caloriesBurned/heartRate/distance)
                candidates.maxWithOrNull(
                    compareBy<DeviceWorkoutRecord> {
                        if (it.category == DeviceExerciseCategory.DAILY_STEPS) (it.steps ?: 0) else it.caloriesBurned
                    }
                        .thenBy { it.caloriesBurned }
                        .thenBy { it.avgHeartRateBpm ?: 0 }
                        .thenBy { it.distanceMeters ?: 0.0 }
                        .thenBy { it.durationMinutes }
                ) ?: candidates.first()
            }

        for (deviceWorkout in deduplicatedDeviceWorkouts) {
            val deterministicId = buildDeterministicWorkoutId(userId, deviceWorkout)
            val existingById = localWorkoutsById[deterministicId]

            // Rule 2.1: Tombstone Guard — if user deleted this synced workout in HealthTrack, never re-import it
            if (existingById != null && existingById.isDeleted) {
                skippedWorkouts++
                continue
            }

            val resolvedDuration = when {
                deviceWorkout.durationMinutes > 0 -> deviceWorkout.durationMinutes
                (deviceWorkout.steps ?: 0) > 0 -> (deviceWorkout.steps!! / 100).coerceAtLeast(1)
                else -> 1
            }
            val resolvedCalories = when {
                deviceWorkout.caloriesBurned > 0 -> deviceWorkout.caloriesBurned
                deviceWorkout.category == DeviceExerciseCategory.DAILY_STEPS && (deviceWorkout.steps ?: 0) > 0 -> {
                    (deviceWorkout.steps!! * 0.00055 * effectiveUserWeightKg).roundToInt().coerceAtLeast(5)
                }
                else -> {
                    // MET fallback calculation using user's weight
                    val hours = resolvedDuration / 60.0
                    (deviceWorkout.category.defaultMet * effectiveUserWeightKg * hours).roundToInt().coerceAtLeast(15)
                }
            }

            val rawTitle = buildWorkoutTitle(deviceWorkout, resolvedDuration)
            val telemetrySummary = buildWorkoutTelemetrySummary(
                record = deviceWorkout,
                usedMetFallback = deviceWorkout.caloriesBurned <= 0
            )

            if (existingById != null) {
                // Already synced before with this deterministic ID: check if watch updated duration or calories
                val changed = existingById.caloriesBurned != resolvedCalories ||
                    existingById.durationMinutes != resolvedDuration ||
                    existingById.aiAnalysis != telemetrySummary
                if (!changed) {
                    skippedWorkouts++
                } else {
                    val updated = existingById.copy(
                        rawText = rawTitle,
                        caloriesBurned = resolvedCalories,
                        durationMinutes = resolvedDuration,
                        aiAnalysis = telemetrySummary,
                        updatedAt = maxOf(deviceWorkout.endTimeMillis, now),
                        syncStatus = SyncStatus.PENDING
                    )
                    database.upsertWorkout(updated)
                    localWorkoutsById[deterministicId] = updated
                    touchedWorkoutDates.add(deviceWorkout.date)
                    updatedWorkouts++
                }
                continue
            }

            // Rule 2.2: Check if user manually/AI logged an overlapping equivalent workout on the same date
            val sameDayActive = activeWorkoutsByDate[deviceWorkout.date].orEmpty()
            val manualDuplicate = sameDayActive.firstOrNull { local ->
                isEquivalentManualWorkout(
                    local = local,
                    deviceCategory = deviceWorkout.category,
                    deviceDuration = resolvedDuration,
                    deviceCalories = resolvedCalories
                )
            }

            if (manualDuplicate != null) {
                skippedWorkouts++
                continue
            }

            // Insert new device workout
            val newWorkout = WorkoutLog(
                id = deterministicId,
                userId = userId,
                date = deviceWorkout.date,
                rawText = rawTitle,
                caloriesBurned = resolvedCalories,
                durationMinutes = resolvedDuration,
                targetCalories = 300,
                targetDurationMinutes = 45,
                aiAnalysis = telemetrySummary,
                createdAt = now,
                updatedAt = maxOf(deviceWorkout.endTimeMillis, now),
                isDeleted = false,
                syncStatus = SyncStatus.PENDING
            )
            database.upsertWorkout(newWorkout)
            localWorkoutsById[deterministicId] = newWorkout
            activeWorkoutsByDate.getOrPut(deviceWorkout.date) { mutableListOf() }.add(newWorkout)
            touchedWorkoutDates.add(deviceWorkout.date)
            importedWorkouts++
        }
        } // end applyDeviceData

        if (snapshot.weights.isNotEmpty() || snapshot.workouts.isNotEmpty()) {
            gamificationManager?.runQuietly { applyDeviceData() } ?: applyDeviceData()
        } else {
            val activeWeightsAfterMerge = database.getWeights(userId).sortedBy { it.date }
            latestWeightEntry = activeWeightsAfterMerge.lastOrNull()
        }

        val sortedTouchedDates = touchedWorkoutDates.sorted()

        val summary = DeviceSyncSummary(
            importedWeights = importedWeights,
            updatedWeights = updatedWeights,
            skippedWeights = skippedWeights,
            importedWorkouts = importedWorkouts,
            updatedWorkouts = updatedWorkouts,
            skippedWorkouts = skippedWorkouts,
            latestWeightKg = latestWeightEntry?.weightKg,
            earliestWorkoutDate = sortedTouchedDates.firstOrNull(),
            latestWorkoutDate = sortedTouchedDates.lastOrNull(),
            platformName = snapshot.platformName
        )

        tokenStorage.setHasConnectedDevice(true)
        tokenStorage.setLastDeviceSyncMs(now)
        tokenStorage.setLastDeviceSyncSummary(summary.toUserMessage())

        Napier.i(
            "DEVICE_SYNC: ${summary.toUserMessage()} (weights: +$importedWeights/~$updatedWeights/=$skippedWeights, workouts: +$importedWorkouts/~$updatedWorkouts/=$skippedWorkouts)",
            tag = "DEVICE_SYNC"
        )

        if (syncToCloud) {
            val hasPendingLocal = summary.totalChanges > 0 || database.getPendingWorkouts(userId).isNotEmpty() || database.getPendingWeights(userId).isNotEmpty()
            if (hasPendingLocal) {
                try {
                    syncEngine.sync()
                } catch (e: Exception) {
                    Napier.w("DEVICE_SYNC: Bulut senkronizasyonu ertelendi: ${e.message}", tag = "DEVICE_SYNC")
                }
            }
        }

        return@withLock summary
    }

    /**
     * Generates a deterministic primary key strictly <= 36 characters to fit PostgreSQL VARCHAR(36)
     * on `health_workouts.id`.
     */
    private fun buildDeterministicWorkoutId(userId: Int, record: DeviceWorkoutRecord): String {
        if (record.category == DeviceExerciseCategory.DAILY_STEPS) {
            return "wdev_${userId}_${record.date}_steps".take(36)
        }
        val catCode = record.category.name.lowercase().take(4)
        val fiveMinBucket = (record.startTimeMillis / 300_000L) % 10_000_000L
        return "wdev_${userId}_${record.date}_${catCode}_$fiveMinBucket".take(36)
    }

    private fun buildScaleNote(
        record: DeviceWeightRecord
    ): String {
        return "⚖️ ${record.sourceName}"
    }

    private fun isAutoGeneratedWeightNote(note: String?): Boolean {
        if (note.isNullOrBlank()) return true
        return note == "Profil Kilo Kaydı" ||
            note == "Profile Weight Entry" ||
            note.startsWith("⚖️")
    }

    private fun mergeWeightNotes(existingNote: String?, scaleNote: String): String {
        if (existingNote.isNullOrBlank() || isAutoGeneratedWeightNote(existingNote)) {
            return scaleNote
        }
        // Preserve user's custom note (e.g. "Aç karna") and append scale telemetry if not already present
        return if (existingNote.contains(scaleNote)) {
            existingNote
        } else {
            "$existingNote ($scaleNote)"
        }
    }

    private fun buildWorkoutTitle(
        record: DeviceWorkoutRecord,
        durationMinutes: Int,
        language: AppLanguage = LocalizationManager.currentLanguage
    ): String {
        val minUnit = trEn("dk", "min", language)
        if (record.category == DeviceExerciseCategory.DAILY_STEPS && (record.steps ?: 0) > 0) {
            return trEn(
                "${record.steps} Adım • Günlük Hareket (~$durationMinutes dk)",
                "${record.steps} Steps • Daily Activity (~$durationMinutes min)",
                language
            )
        }
        val rawCustomTitle = record.customTitle
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("Workout", ignoreCase = true) && !it.equals("Exercise", ignoreCase = true) }
        val baseTitle = when {
            rawCustomTitle == null -> record.category.localizedTitle(language)
            record.category != DeviceExerciseCategory.OTHER &&
                record.category != DeviceExerciseCategory.DAILY_STEPS &&
                SportType.detectAll(rawCustomTitle).isEmpty() ->
                "$rawCustomTitle (${record.category.localizedTitle(language)})"
            else -> rawCustomTitle
        }
        return if (baseTitle.contains("dk", ignoreCase = true) || baseTitle.contains("min", ignoreCase = true)) {
            baseTitle
        } else {
            "$durationMinutes $minUnit $baseTitle"
        }
    }

    private fun buildWorkoutTelemetrySummary(
        record: DeviceWorkoutRecord,
        usedMetFallback: Boolean,
        language: AppLanguage = LocalizationManager.currentLanguage
    ): String {
        val badges = mutableListOf<String>()
        badges.add("⌚ ${record.sourceName}")
        record.distanceMeters?.takeIf { it >= 50.0 }?.let { meters ->
            val km = roundOneDecimal(meters / 1000.0)
            badges.add("$km km")
        }
        record.steps?.takeIf { it > 0 }?.let { steps ->
            badges.add(trEn("$steps adım", "$steps steps", language))
        }
        record.avgHeartRateBpm?.takeIf { it in 40..230 }?.let { bpm ->
            badges.add(trEn("Ort. $bpm bpm", "Avg. $bpm bpm", language))
        }
        if (usedMetFallback) {
            badges.add(trEn("MET kalori hesabı", "MET calorie estimate", language))
        }
        return badges.joinToString(" • ")
    }

    private fun isEquivalentManualWorkout(
        local: WorkoutLog,
        deviceCategory: DeviceExerciseCategory,
        deviceDuration: Int,
        deviceCalories: Int
    ): Boolean {
        // Only compare against non-device entries (manual or AI)
        if (local.id.startsWith("workout_dev_") || local.id.startsWith("wdev_")) return false
        val textNormalized = local.rawText.lowercase()
        val trKeyword = deviceCategory.titleTr.lowercase().substringBefore(" ")
        val enKeyword = deviceCategory.titleEn.lowercase().substringBefore(" ")
        val sameCategory = textNormalized.contains(trKeyword) || textNormalized.contains(enKeyword)
        val similarDuration = abs(local.durationMinutes - deviceDuration) <= 10
        val similarCalories = abs(local.caloriesBurned - deviceCalories) <= maxOf(50, (deviceCalories * 0.25).toInt())
        return sameCategory && (similarDuration || similarCalories)
    }

    private fun roundOneDecimal(value: Double): Double {
        return (value * 10.0).roundToInt() / 10.0
    }
}

