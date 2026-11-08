package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Success(val message: String) : SyncState
    data class Error(val message: String) : SyncState
}

class SyncEngine(
    private val database: HealthDatabase,
    private val apiClient: HealthApiClient,
    private val tokenStorage: TokenStorage
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val isoDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun sync(forceFullPull: Boolean = false): Boolean = syncMutex.withLock {
        val userId = tokenStorage.getUserId()
        if (userId <= 0 || tokenStorage.getAccessToken().isNullOrBlank()) {
            _syncState.value = SyncState.Error("Oturum açılmamış.")
            return@withLock false
        }

        _syncState.value = SyncState.Syncing
        Napier.d("SYNC: Senkronizasyon başlatılıyor (userId=$userId, forceFullPull=$forceFullPull)...", tag = "SYNC_LOG")

        try {
            // ── 0. SANITIZE & DEDUPLICATE LOCAL DATA ──
            database.deduplicateAndSanitizeLocalData(userId, isoDateRegex)

            // ── 1. GATHER LOCAL CHANGES TO PUSH ──
            val foodsToPush = (if (forceFullPull) database.getAllFoodsForSync(userId) else database.getPendingFoods(userId))
                .filter { isoDateRegex.matches(it.date) }
            val goalToPush = if (forceFullPull) database.getNutritionGoalForSync(userId) else database.getPendingNutritionGoal(userId)
            val workoutsToPush = (if (forceFullPull) database.getAllWorkoutsForSync(userId) else database.getPendingWorkouts(userId))
                .filter { isoDateRegex.matches(it.date) }
            val weightsToPush = (if (forceFullPull) database.getAllWeightsForSync(userId) else database.getPendingWeights(userId))
                .filter { isoDateRegex.matches(it.date) }

            val pushPayload = buildPushPayload(
                foods = foodsToPush,
                goal = goalToPush,
                workouts = workoutsToPush,
                weights = weightsToPush
            )

            // ── 2. PUSH TO BACKEND ──
            val hasPending = foodsToPush.isNotEmpty() || goalToPush != null ||
                    workoutsToPush.isNotEmpty() || weightsToPush.isNotEmpty()

            var pushError: String? = null
            if (hasPending) {
                val pushResult = apiClient.pushSync(pushPayload)
                if (pushResult.isFailure) {
                    val err = pushResult.exceptionOrNull()?.message
                    Napier.e("SYNC: Push hatası: $err", pushResult.exceptionOrNull(), tag = "SYNC_LOG")
                    pushError = err
                } else {
                    val pushResponse = pushResult.getOrThrow()
                    Napier.d("SYNC: Push başarılı. Senkronlanan: foods=${pushResponse.syncedFoodIds.size}, workouts=${pushResponse.syncedWorkoutIds.size}, weights=${pushResponse.syncedWeightIds.size}", tag = "SYNC_LOG")
                    database.markFoodsSynced(pushResponse.syncedFoodIds)
                    if (pushResponse.syncedNutritionGoalId != null) {
                        database.markNutritionGoalSynced(userId)
                    }
                    database.markWorkoutsSynced(pushResponse.syncedWorkoutIds)
                    database.markWeightsSynced(pushResponse.syncedWeightIds)
                }
            }

            // ── 3. PULL FULL STATE FROM BACKEND (Planora-style 0L pull avoids clock-skew/offline timestamp misses) ──
            val pullResult = apiClient.pullSync(0L)
            if (pullResult.isFailure) {
                val err = pullResult.exceptionOrNull()?.message
                Napier.e("SYNC: Pull hatası: $err", pullResult.exceptionOrNull(), tag = "SYNC_LOG")
                _syncState.value = SyncState.Error(pushError?.let { "Push: $it, Pull: $err" } ?: "Pull hatası: $err")
                return@withLock false
            }

            val pullData = pullResult.getOrThrow()
            Napier.d("SYNC: Pull başarılı. Gelen: foods=${pullData.foods.size}, workouts=${pullData.workouts.size}, weights=${pullData.weights.size}", tag = "SYNC_LOG")

            // Merge Foods
            database.mergePullFoods(pullData.foods.filter { isoDateRegex.matches(it.date) }.map {
                val items = if (it.isDeleted) emptyList() else try { json.decodeFromString<List<FoodItem>>(it.itemsJson) } catch (e: Exception) { emptyList() }
                FoodLog(
                    id = it.id,
                    userId = userId,
                    date = it.date,
                    rawText = if (it.isDeleted) "" else it.rawText,
                    summary = if (it.isDeleted) null else it.summary,
                    totalCalories = if (it.isDeleted) 0 else it.totalCalories,
                    items = items,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted,
                    syncStatus = SyncStatus.SYNCED
                )
            })

            // Merge Weights
            database.mergePullWeights(pullData.weights.filter { isoDateRegex.matches(it.date) }.map {
                WeightEntry(
                    id = it.id,
                    userId = userId,
                    date = it.date,
                    weightKg = it.weightKg,
                    targetWeightKg = it.targetWeightKg,
                    note = if (it.isDeleted) null else it.note,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted,
                    syncStatus = SyncStatus.SYNCED
                )
            })

            // Update TokenStorage weight & targetWeight from merged local DB
            val mergedWeights = database.getWeights(userId)
            val latestWeight = mergedWeights.maxByOrNull { it.date }
            if (latestWeight != null) {
                tokenStorage.setUserWeight(latestWeight.weightKg)
                val latestTarget = mergedWeights.filter { it.targetWeightKg != null }.maxByOrNull { it.date }?.targetWeightKg
                    ?: latestWeight.targetWeightKg
                if (latestTarget != null && latestTarget > 0.0) {
                    tokenStorage.setUserTargetWeight(latestTarget)
                }
                if (tokenStorage.getUserHeight() <= 0.0) {
                    tokenStorage.setUserHeight(175.0)
                }
                if (tokenStorage.getUserAge() <= 0) {
                    tokenStorage.setUserAge(26)
                }
            }

            // Merge Nutrition Goal & Restore Fitness Goal in TokenStorage
            pullData.nutritionGoals?.let { goalDto ->
                val localGoal = database.getNutritionGoalForSync(userId)
                val shouldApplyRemoteGoal = localGoal == null ||
                    (localGoal.syncStatus != SyncStatus.PENDING && goalDto.updatedAt >= localGoal.updatedAt)

                val currentWeight = latestWeight?.weightKg
                    ?: tokenStorage.getUserWeight().takeIf { it > 0.0 }
                    ?: 0.0

                val inferredGoal = if (currentWeight > 0.0) {
                    val ratio = goalDto.proteinG / currentWeight
                    when {
                        ratio in 1.10..1.30 -> FitnessGoal.LOSE_WEIGHT
                        ratio >= 1.35 -> FitnessGoal.BUILD_MUSCLE
                        else -> FitnessGoal.MAINTAIN
                    }
                } else {
                    tokenStorage.getUserFitnessGoal()
                }

                if (shouldApplyRemoteGoal) {
                    tokenStorage.setUserFitnessGoal(inferredGoal)
                }

                database.mergePullNutritionGoal(
                    NutritionGoal(
                        id = goalDto.id,
                        userId = userId,
                        calories = goalDto.calories,
                        proteinG = goalDto.proteinG,
                        carbsG = goalDto.carbsG,
                        fatG = goalDto.fatG,
                        fiberG = goalDto.fiberG,
                        vitaminCMg = goalDto.vitaminCMg,
                        vitaminDUg = goalDto.vitaminDUg,
                        vitaminB12Ug = goalDto.vitaminB12Ug,
                        ironMg = goalDto.ironMg,
                        calciumMg = goalDto.calciumMg,
                        magnesiumMg = goalDto.magnesiumMg,
                        fitnessGoal = if (shouldApplyRemoteGoal) inferredGoal.id else (localGoal?.fitnessGoal ?: inferredGoal.id),
                        updatedAt = goalDto.updatedAt,
                        isDeleted = goalDto.isDeleted,
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            }

            // Merge Workouts
            database.mergePullWorkouts(pullData.workouts.filter { isoDateRegex.matches(it.date) }.map {
                WorkoutLog(
                    id = it.id,
                    userId = userId,
                    date = it.date,
                    rawText = if (it.isDeleted) "" else it.rawText,
                    caloriesBurned = if (it.isDeleted) 0 else it.caloriesBurned,
                    durationMinutes = if (it.isDeleted) 0 else it.durationMinutes,
                    targetCalories = it.targetCalories,
                    targetDurationMinutes = it.targetDurationMinutes,
                    aiAnalysis = if (it.isDeleted) null else it.aiAnalysis,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted,
                    syncStatus = SyncStatus.SYNCED
                )
            })

            // ── 4. POST-MERGE RECONCILIATION & CLEANUP PUSH ──
            // Ensure any local active items missing/outdated on server OR newly generated duplicate-cleanup tombstones are pushed immediately
            val remoteFoodMap = pullData.foods.associateBy { it.id }
            val remoteWeightMap = pullData.weights.associateBy { it.id }
            val remoteWorkoutMap = pullData.workouts.associateBy { it.id }

            val reconcileFoods = database.getAllFoodsForSync(userId).filter { local ->
                isoDateRegex.matches(local.date) && (
                    local.syncStatus == SyncStatus.PENDING ||
                    (!local.isDeleted && (remoteFoodMap[local.id] == null || local.updatedAt > (remoteFoodMap[local.id]?.updatedAt ?: 0L)))
                )
            } + pullData.foods.filter { remote ->
                isoDateRegex.matches(remote.date) && remote.isDeleted &&
                    (remote.rawText.isNotBlank() || (remote.itemsJson.isNotBlank() && remote.itemsJson != "[]"))
            }.map { remote ->
                FoodLog(
                    id = remote.id,
                    userId = userId,
                    date = remote.date,
                    rawText = "",
                    summary = null,
                    totalCalories = 0,
                    items = emptyList(),
                    updatedAt = remote.updatedAt + 1,
                    isDeleted = true,
                    syncStatus = SyncStatus.PENDING
                )
            }
            val reconcileWeights = database.getAllWeightsForSync(userId).filter { local ->
                isoDateRegex.matches(local.date) && (
                    local.syncStatus == SyncStatus.PENDING ||
                    (!local.isDeleted && (remoteWeightMap[local.id] == null || local.updatedAt > (remoteWeightMap[local.id]?.updatedAt ?: 0L)))
                )
            }
            val reconcileWorkouts = database.getAllWorkoutsForSync(userId).filter { local ->
                isoDateRegex.matches(local.date) && (
                    local.syncStatus == SyncStatus.PENDING ||
                    (!local.isDeleted && (remoteWorkoutMap[local.id] == null || local.updatedAt > (remoteWorkoutMap[local.id]?.updatedAt ?: 0L)))
                )
            }
            val localGoalAfterMerge = database.getNutritionGoalForSync(userId)
            val reconcileGoal = localGoalAfterMerge?.takeIf { local ->
                local.syncStatus == SyncStatus.PENDING ||
                pullData.nutritionGoals == null ||
                local.updatedAt > pullData.nutritionGoals.updatedAt
            }

            if (reconcileFoods.isNotEmpty() || reconcileWeights.isNotEmpty() || reconcileWorkouts.isNotEmpty() || reconcileGoal != null) {
                Napier.d("SYNC: Post-merge uzlaştırma push çalıştırılıyor (foods=${reconcileFoods.size}, weights=${reconcileWeights.size}, workouts=${reconcileWorkouts.size}, goal=${reconcileGoal != null})", tag = "SYNC_LOG")
                val cleanupPayload = buildPushPayload(
                    foods = reconcileFoods,
                    goal = reconcileGoal,
                    workouts = reconcileWorkouts,
                    weights = reconcileWeights
                )
                val cleanupResult = apiClient.pushSync(cleanupPayload)
                if (cleanupResult.isSuccess) {
                    val resp = cleanupResult.getOrThrow()
                    database.markFoodsSynced(resp.syncedFoodIds)
                    if (resp.syncedNutritionGoalId != null) {
                        database.markNutritionGoalSynced(userId)
                    }
                    database.markWorkoutsSynced(resp.syncedWorkoutIds)
                    database.markWeightsSynced(resp.syncedWeightIds)
                    pushError = null
                }
            }

            if (pullData.serverTimestamp > 0) {
                tokenStorage.setLastSyncTimestamp(pullData.serverTimestamp)
            }

            if (pushError != null) {
                _syncState.value = SyncState.Error("Yerel değişiklikler iletilemedi ($pushError) ancak sunucu güncellemeleri alındı.")
                return@withLock false
            } else {
                _syncState.value = SyncState.Success("Veriler başarıyla senkronize edildi.")
                Napier.i("SYNC: Senkronizasyon başarıyla tamamlandı.", tag = "SYNC_LOG")
                return@withLock true
            }
        } catch (e: Exception) {
            Napier.e("SYNC: Senkronizasyon hatası: ${e.message}", e, tag = "SYNC_LOG")
            _syncState.value = SyncState.Error("Senkronizasyon hatası: ${e.message}")
            return@withLock false
        }
    }

    private fun buildPushPayload(
        foods: List<FoodLog>,
        goal: NutritionGoal?,
        workouts: List<WorkoutLog>,
        weights: List<WeightEntry>
    ): PushRequestPayload {
        return PushRequestPayload(
            foods = foods.map {
                FoodSyncPayload(
                    id = it.id,
                    date = it.date,
                    rawText = if (it.isDeleted) "" else it.rawText,
                    summary = if (it.isDeleted) null else it.summary,
                    totalCalories = if (it.isDeleted) 0 else it.totalCalories,
                    itemsJson = if (it.isDeleted) "[]" else json.encodeToString(it.items),
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            },
            nutritionGoals = goal?.let {
                NutritionGoalSyncPayload(
                    id = it.id,
                    calories = it.calories,
                    proteinG = it.proteinG,
                    carbsG = it.carbsG,
                    fatG = it.fatG,
                    fiberG = it.fiberG,
                    vitaminCMg = it.vitaminCMg,
                    vitaminDUg = it.vitaminDUg,
                    vitaminB12Ug = it.vitaminB12Ug,
                    ironMg = it.ironMg,
                    calciumMg = it.calciumMg,
                    magnesiumMg = it.magnesiumMg,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            },
            workouts = workouts.map {
                WorkoutSyncPayload(
                    id = it.id,
                    date = it.date,
                    rawText = if (it.isDeleted) "" else it.rawText,
                    caloriesBurned = if (it.isDeleted) 0 else it.caloriesBurned,
                    durationMinutes = if (it.isDeleted) 0 else it.durationMinutes,
                    targetCalories = if (it.isDeleted) 0 else it.targetCalories,
                    targetDurationMinutes = if (it.isDeleted) 0 else it.targetDurationMinutes,
                    aiAnalysis = if (it.isDeleted) null else it.aiAnalysis,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            },
            weights = weights.map {
                WeightSyncPayload(
                    id = it.id,
                    date = it.date,
                    weightKg = it.weightKg,
                    targetWeightKg = it.targetWeightKg,
                    note = if (it.isDeleted) null else it.note,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
        )
    }
}
