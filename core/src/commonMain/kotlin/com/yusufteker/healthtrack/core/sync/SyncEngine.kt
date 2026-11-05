package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun sync(): Boolean {
        val userId = tokenStorage.getUserId()
        if (userId <= 0 || tokenStorage.getAccessToken().isNullOrBlank()) {
            _syncState.value = SyncState.Error("Oturum açılmamış.")
            return false
        }

        _syncState.value = SyncState.Syncing
        Napier.d("SYNC: Senkronizasyon başlatılıyor (userId=$userId)...", tag = "SYNC_LOG")

        try {
            // ── 1. GATHER PENDING LOCAL CHANGES ──
            val pendingFoods = database.getPendingFoods(userId)
            val pendingGoal = database.getPendingNutritionGoal(userId)
            val pendingWorkouts = database.getPendingWorkouts(userId)
            val pendingWeights = database.getPendingWeights(userId)
            val pendingAppointments = database.getPendingAppointments(userId)

            val pushPayload = PushRequestPayload(
                foods = pendingFoods.map {
                    FoodSyncPayload(
                        id = it.id,
                        date = it.date,
                        rawText = it.rawText,
                        summary = it.summary,
                        totalCalories = it.totalCalories,
                        itemsJson = json.encodeToString(it.items),
                        updatedAt = it.updatedAt,
                        isDeleted = it.isDeleted
                    )
                },
                nutritionGoals = pendingGoal?.let {
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
                workouts = pendingWorkouts.map {
                    WorkoutSyncPayload(
                        id = it.id,
                        date = it.date,
                        rawText = it.rawText,
                        caloriesBurned = it.caloriesBurned,
                        durationMinutes = it.durationMinutes,
                        targetCalories = it.targetCalories,
                        targetDurationMinutes = it.targetDurationMinutes,
                        aiAnalysis = it.aiAnalysis,
                        updatedAt = it.updatedAt,
                        isDeleted = it.isDeleted
                    )
                },
                weights = pendingWeights.map {
                    WeightSyncPayload(
                        id = it.id,
                        date = it.date,
                        weightKg = it.weightKg,
                        targetWeightKg = it.targetWeightKg,
                        note = it.note,
                        updatedAt = it.updatedAt,
                        isDeleted = it.isDeleted
                    )
                },
                appointments = pendingAppointments.map {
                    AppointmentSyncPayload(
                        id = it.id,
                        dateTime = it.dateTime,
                        doctorName = it.doctorName,
                        hospitalName = it.hospitalName,
                        department = it.department,
                        notes = it.notes,
                        reminderMinutes = it.reminderMinutes,
                        planoraTaskId = it.planoraTaskId,
                        updatedAt = it.updatedAt,
                        isDeleted = it.isDeleted
                    )
                }
            )

            // ── 2. PUSH TO BACKEND ──
            val hasPending = pendingFoods.isNotEmpty() || pendingGoal != null ||
                    pendingWorkouts.isNotEmpty() || pendingWeights.isNotEmpty() || pendingAppointments.isNotEmpty()

            if (hasPending) {
                val pushResult = apiClient.pushSync(pushPayload)
                if (pushResult.isFailure) {
                    val err = pushResult.exceptionOrNull()?.message
                    Napier.e("SYNC: Push hatası: $err", pushResult.exceptionOrNull(), tag = "SYNC_LOG")
                    _syncState.value = SyncState.Error("Push hatası: $err")
                    return false
                }
                val pushResponse = pushResult.getOrThrow()
                Napier.d("SYNC: Push başarılı. Senkronlanan: foods=${pushResponse.syncedFoodIds.size}, workouts=${pushResponse.syncedWorkoutIds.size}, weights=${pushResponse.syncedWeightIds.size}", tag = "SYNC_LOG")
                database.markFoodsSynced(pushResponse.syncedFoodIds)
                if (pushResponse.syncedNutritionGoalId != null) {
                    database.markNutritionGoalSynced(userId)
                }
                database.markWorkoutsSynced(pushResponse.syncedWorkoutIds)
                database.markWeightsSynced(pushResponse.syncedWeightIds)
                database.markAppointmentsSynced(pushResponse.syncedAppointmentIds)
            }

            // ── 3. PULL FROM BACKEND ──
            val lastSyncTs = tokenStorage.getLastSyncTimestamp()
            val pullResult = apiClient.pullSync(lastSyncTs)
            if (pullResult.isFailure) {
                val err = pullResult.exceptionOrNull()?.message
                Napier.e("SYNC: Pull hatası: $err", pullResult.exceptionOrNull(), tag = "SYNC_LOG")
                _syncState.value = SyncState.Error("Pull hatası: $err")
                return false
            }

            val pullData = pullResult.getOrThrow()
            Napier.d("SYNC: Pull başarılı. Gelen: foods=${pullData.foods.size}, workouts=${pullData.workouts.size}, weights=${pullData.weights.size}, appointments=${pullData.appointments.size}", tag = "SYNC_LOG")

            // Merge Foods
            database.mergePullFoods(pullData.foods.map {
                val items = try { json.decodeFromString<List<FoodItem>>(it.itemsJson) } catch (e: Exception) { emptyList() }
                FoodLog(
                    id = it.id,
                    userId = userId,
                    date = it.date,
                    rawText = it.rawText,
                    summary = it.summary,
                    totalCalories = it.totalCalories,
                    items = items,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted,
                    syncStatus = SyncStatus.SYNCED
                )
            })

            // Merge Nutrition Goal
            pullData.nutritionGoals?.let {
                database.mergePullNutritionGoal(
                    NutritionGoal(
                        id = it.id,
                        userId = userId,
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
                        isDeleted = it.isDeleted,
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            }

            // Merge Workouts
            database.mergePullWorkouts(pullData.workouts.map {
                WorkoutLog(
                    id = it.id,
                    userId = userId,
                    date = it.date,
                    rawText = it.rawText,
                    caloriesBurned = it.caloriesBurned,
                    durationMinutes = it.durationMinutes,
                    targetCalories = it.targetCalories,
                    targetDurationMinutes = it.targetDurationMinutes,
                    aiAnalysis = it.aiAnalysis,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted,
                    syncStatus = SyncStatus.SYNCED
                )
            })

            // Merge Weights
            database.mergePullWeights(pullData.weights.map {
                WeightEntry(
                    id = it.id,
                    userId = userId,
                    date = it.date,
                    weightKg = it.weightKg,
                    targetWeightKg = it.targetWeightKg,
                    note = it.note,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted,
                    syncStatus = SyncStatus.SYNCED
                )
            })

            // Merge Appointments
            database.mergePullAppointments(pullData.appointments.map {
                AppointmentEntry(
                    id = it.id,
                    userId = userId,
                    dateTime = it.dateTime,
                    doctorName = it.doctorName,
                    hospitalName = it.hospitalName,
                    department = it.department,
                    notes = it.notes,
                    reminderMinutes = it.reminderMinutes,
                    planoraTaskId = it.planoraTaskId,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted,
                    syncStatus = SyncStatus.SYNCED
                )
            })

            if (pullData.serverTimestamp > 0) {
                tokenStorage.setLastSyncTimestamp(pullData.serverTimestamp)
            }

            _syncState.value = SyncState.Success("Veriler başarıyla senkronize edildi.")
            Napier.i("SYNC: Senkronizasyon başarıyla tamamlandı.", tag = "SYNC_LOG")
            return true
        } catch (e: Exception) {
            Napier.e("SYNC: Senkronizasyon hatası: ${e.message}", e, tag = "SYNC_LOG")
            _syncState.value = SyncState.Error("Senkronizasyon hatası: ${e.message}")
            return false
        }
    }
}
