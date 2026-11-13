package com.yusufteker.healthtrack.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.DailyStepTelemetry
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.model.WorkoutTelemetryHelper
import com.yusufteker.healthtrack.core.network.AiQuotaExceededException
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.network.formatActionableApiError
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.SyncEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class WorkoutViewModel(
    private val database: HealthDatabase,
    private val apiClient: HealthApiClient,
    private val tokenStorage: TokenStorage,
    private val syncEngine: SyncEngine,
    private val deviceHealthSyncManager: DeviceHealthSyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(
        WorkoutState(
            userWeightKg = tokenStorage.getUserWeight().takeIf { it > 0.0 } ?: 75.0,
            reminderFrequency = tokenStorage.getWorkoutReminderFrequency()
        )
    )
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private val _effects = Channel<WorkoutEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutEffect> = _effects.receiveAsFlow()

    private var workoutObservationJob: Job? = null
    private var allWorkoutsObservationJob: Job? = null
    private var foodObservationJob: Job? = null
    private var weightObservationJob: Job? = null

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow
                .distinctUntilChangedBy { it.isLoggedIn to it.userId }
                .collect { session ->
                    if (session.isLoggedIn && session.userId > 0) {
                        observeWorkoutsAndCalories(session.userId, _state.value.selectedDate)
                        observeAllWorkoutsHistory(session.userId)
                        observeWeight(session.userId)
                        refreshAiQuota()
                    }
                }
        }
    }

    private fun refreshAiQuota() {
        val hasCustomKey = !tokenStorage.getCustomGeminiApiKey().isNullOrBlank()
        _state.update { it.copy(hasCustomGeminiKey = hasCustomKey) }
        if (hasCustomKey) return
        viewModelScope.launch {
            apiClient.getAiQuota().onSuccess { quota ->
                _state.update { it.copy(aiQuota = quota) }
            }
        }
    }

    fun onIntent(intent: WorkoutIntent) {
        when (intent) {
            is WorkoutIntent.OnDateChanged -> {
                _state.update { it.copy(selectedDate = intent.newDate) }
                val userId = tokenStorage.getUserId()
                if (userId > 0) {
                    observeWorkoutsAndCalories(userId, intent.newDate)
                }
            }
            is WorkoutIntent.OnRawTextChanged -> _state.update { it.copy(rawText = intent.text) }
            is WorkoutIntent.OnManualCaloriesChanged -> _state.update { it.copy(manualCalories = intent.value) }
            is WorkoutIntent.OnManualDurationChanged -> _state.update { it.copy(manualDuration = intent.value) }
            is WorkoutIntent.OnTargetCaloriesChanged -> _state.update { it.copy(targetCalories = intent.value) }
            is WorkoutIntent.OnCalculateAiClicked -> calculateWithAi()
            is WorkoutIntent.OnSaveManualClicked -> saveManualWorkout()
            is WorkoutIntent.OnWatchSyncStarted -> {
                _state.update { it.copy(isSyncingWatch = true, errorMessage = null, infoMessage = null) }
            }
            is WorkoutIntent.OnWatchSnapshotReceived -> {
                viewModelScope.launch {
                    val lang = LocalizationManager.currentLanguage
                    intent.result.onSuccess { snapshot ->
                        val summary = deviceHealthSyncManager.mergeSnapshot(snapshot)
                        val msg = summary.toUserMessage(lang)
                        if (!intent.isSilent || summary.totalChanges > 0) {
                            _state.update { it.copy(isSyncingWatch = false, infoMessage = msg) }
                            _effects.send(WorkoutEffect.ShowToast(msg))
                        } else {
                            _state.update { it.copy(isSyncingWatch = false) }
                        }
                    }.onFailure { err ->
                        if (!intent.isSilent) {
                            _state.update {
                                it.copy(
                                    isSyncingWatch = false,
                                    errorMessage = err.message ?: trEn("Saat antrenman verileri alınamadı.", "Could not read watch workout data.", lang)
                                )
                            }
                        } else {
                            _state.update { it.copy(isSyncingWatch = false) }
                        }
                    }
                }
            }
            is WorkoutIntent.OnDeleteWorkout -> deleteWorkout(intent.id)
            is WorkoutIntent.OnReminderFrequencyChanged -> {
                tokenStorage.setWorkoutReminderFrequency(intent.frequency)
                _state.update { it.copy(reminderFrequency = intent.frequency, isReminderDialogOpen = false) }
                val lang = LocalizationManager.currentLanguage
                val msg = if (intent.frequency == com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency.OFF) {
                    trEn("Spora teşvik bildirimleri kapatıldı.", "Workout motivation alerts turned off.", lang)
                } else {
                    trEn(
                        "Spor hatırlatıcısı güncellendi: ${intent.frequency.localizedTitle(lang)}",
                        "Workout reminder updated: ${intent.frequency.localizedTitle(lang)}",
                        lang
                    )
                }
                _state.update { it.copy(infoMessage = msg) }
            }
            is WorkoutIntent.OnSetReminderDialogOpen -> _state.update { it.copy(isReminderDialogOpen = intent.isOpen) }
            is WorkoutIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
            is WorkoutIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
        }
    }

    private fun observeWorkoutsAndCalories(userId: Int, date: String) {
        workoutObservationJob?.cancel()
        workoutObservationJob = viewModelScope.launch {
            database.observeWorkouts(userId, date).collect { list ->
                val stepLog = list.firstOrNull { it.isDailySteps }
                val explicitWorkouts = list.filter { !it.isDailySteps }

                val explicitBurned = explicitWorkouts.sumOf { it.caloriesBurned }
                val explicitDuration = explicitWorkouts.sumOf { it.durationMinutes }

                val stepTelemetry = stepLog?.let {
                    WorkoutTelemetryHelper.parseDailyStepTelemetry(it, explicitWorkouts)
                }

                val totalBurned = explicitBurned + (stepTelemetry?.netCaloriesBurned ?: 0)
                val totalDuration = explicitDuration + (if (explicitWorkouts.isEmpty() && stepTelemetry != null) stepTelemetry.durationMinutes else 0)

                _state.update { current ->
                    current.copy(
                        workouts = explicitWorkouts,
                        dailySteps = stepTelemetry,
                        totalCaloriesBurned = totalBurned,
                        totalDurationMinutes = totalDuration,
                        netCalories = current.consumedCalories - totalBurned
                    )
                }
            }
        }

        foodObservationJob?.cancel()
        foodObservationJob = viewModelScope.launch {
            database.observeFoods(userId, date).collect { foodList ->
                val consumed = foodList.sumOf { it.totalCalories }
                _state.update { current ->
                    current.copy(
                        consumedCalories = consumed,
                        netCalories = consumed - current.totalCaloriesBurned
                    )
                }
            }
        }
    }

    private fun observeAllWorkoutsHistory(userId: Int) {
        allWorkoutsObservationJob?.cancel()
        allWorkoutsObservationJob = viewModelScope.launch {
            database.observeAllWorkouts(userId).collect { allList ->
                val sorted = allList.sortedWith(
                    compareByDescending<WorkoutLog> { it.date }
                        .thenByDescending { it.updatedAt }
                )
                _state.update { it.copy(allWorkouts = sorted) }
            }
        }
    }

    private fun observeWeight(userId: Int) {
        weightObservationJob?.cancel()
        weightObservationJob = viewModelScope.launch {
            database.observeWeights(userId).collect { weightList ->
                val latest = weightList.maxByOrNull { it.date }
                if (latest != null) {
                    _state.update { it.copy(userWeightKg = latest.weightKg) }
                } else {
                    val fallback = tokenStorage.getUserWeight()
                    if (fallback > 0.0) {
                        _state.update { it.copy(userWeightKg = fallback) }
                    }
                }
            }
        }
    }

    private fun calculateWithAi() {
        val rawText = _state.value.rawText.trim()
        val date = _state.value.selectedDate
        val userId = tokenStorage.getUserId()
        val weight = _state.value.userWeightKg

        if (rawText.isBlank()) {
            _state.update {
                it.copy(
                    errorMessage = trEn(
                        "Lütfen önce yapılan sporu yazınız (Örn: 30 dk koşu, 20 dk ağırlık).",
                        "Please enter your workout first (e.g., 30 min run, 20 min weights)."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCalculatingAi = true, errorMessage = null, infoMessage = null) }

            val result = apiClient.analyzeWorkout(rawText, weight, date)
            result.onSuccess { aiResult ->
                val now = currentTime()
                val workout = WorkoutLog(
                    id = Uuid.random().toString(),
                    userId = userId,
                    date = date,
                    rawText = rawText,
                    caloriesBurned = aiResult.caloriesBurned,
                    durationMinutes = aiResult.durationMinutes,
                    targetCalories = _state.value.targetCalories,
                    targetDurationMinutes = _state.value.targetDurationMinutes,
                    aiAnalysis = aiResult.summary,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )

                database.upsertWorkout(workout)
                _state.update {
                    it.copy(
                        isCalculatingAi = false,
                        rawText = "",
                        infoMessage = trEn(
                            "AI ile spor analizi tamamlandı: ${aiResult.caloriesBurned} kcal yakıldı.",
                            "AI workout analysis complete: ${aiResult.caloriesBurned} kcal burned."
                        )
                    )
                }
                launch { syncEngine.sync() }
                refreshAiQuota()
            }.onFailure { err ->
                if (err is AiQuotaExceededException) {
                    _state.update {
                        it.copy(
                            isCalculatingAi = false,
                            errorMessage = err.message,
                            aiQuota = err.quota ?: it.aiQuota
                        )
                    }
                    return@onFailure
                }
                println("[WorkoutViewModel] analyzeWorkout failure: ${err.message ?: "Bilinmeyen hata"}")
                val displayMsg = formatActionableApiError(
                    error = err,
                    operationTr = "AI spor analizi",
                    operationEn = "AI workout analysis"
                )

                _state.update {
                    it.copy(
                        isCalculatingAi = false,
                        errorMessage = displayMsg
                    )
                }
            }
        }
    }

    private fun saveManualWorkout() {
        val rawText = _state.value.rawText.trim()
        val cal = _state.value.manualCalories.toIntOrNull() ?: 0
        val dur = _state.value.manualDuration.toIntOrNull() ?: 0
        val date = _state.value.selectedDate
        val userId = tokenStorage.getUserId()

        if (rawText.isBlank() && cal == 0) {
            _state.update {
                it.copy(
                    errorMessage = trEn(
                        "Lütfen aktivite adı veya yakılan kaloriyi giriniz.",
                        "Please enter an activity name or calories burned."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            val now = currentTime()
            val workout = WorkoutLog(
                id = Uuid.random().toString(),
                userId = userId,
                date = date,
                rawText = if (rawText.isNotBlank()) rawText else trEn("Egzersiz ($cal kcal)", "Workout ($cal kcal)"),
                caloriesBurned = cal,
                durationMinutes = dur,
                targetCalories = _state.value.targetCalories,
                targetDurationMinutes = _state.value.targetDurationMinutes,
                aiAnalysis = trEn("Elle girildi", "Entered manually"),
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            database.upsertWorkout(workout)
            _state.update { it.copy(rawText = "", manualCalories = "", manualDuration = "") }
            _effects.send(WorkoutEffect.ShowToast(trEn("Egzersiz kaydedildi.", "Workout saved.")))
            launch { syncEngine.sync() }
        }
    }

    private fun deleteWorkout(id: String) {
        viewModelScope.launch {
            database.deleteWorkout(id)
            launch { syncEngine.sync() }
        }
    }

    private fun currentTime(): Long = com.yusufteker.healthtrack.core.utils.currentTimeMillis()
}
