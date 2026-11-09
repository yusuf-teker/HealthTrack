package com.yusufteker.healthtrack.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.DailyStepTelemetry
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
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
        WorkoutState(userWeightKg = tokenStorage.getUserWeight().takeIf { it > 0.0 } ?: 75.0)
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
            tokenStorage.sessionFlow.collect { session ->
                if (session.isLoggedIn) {
                    observeWorkoutsAndCalories(session.userId, _state.value.selectedDate)
                    observeAllWorkoutsHistory(session.userId)
                    observeWeight(session.userId)
                }
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
                    intent.result.onSuccess { snapshot ->
                        val summary = deviceHealthSyncManager.mergeSnapshot(snapshot)
                        val msg = summary.toUserMessage()
                        _state.update { it.copy(isSyncingWatch = false, infoMessage = msg) }
                        _effects.send(WorkoutEffect.ShowToast(msg))
                    }.onFailure { err ->
                        _state.update {
                            it.copy(
                                isSyncingWatch = false,
                                errorMessage = err.message ?: "Saat antrenman verileri alınamadı."
                            )
                        }
                    }
                }
            }
            is WorkoutIntent.OnDeleteWorkout -> deleteWorkout(intent.id)
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

                // Estimate steps taken during explicit active sports to prevent double-counting
                val estimatedSportSteps = explicitWorkouts.sumOf { w ->
                    estimateSportSteps(w.rawText, w.durationMinutes)
                }

                val stepTelemetry = stepLog?.let {
                    val parsed = parseDailyStepTelemetry(it)
                    val netSteps = (parsed.stepCount - estimatedSportSteps).coerceAtLeast(0)
                    val netKcal = if (parsed.stepCount > 0) {
                        val ratio = netSteps.toDouble() / parsed.stepCount
                        (parsed.caloriesBurned * ratio).roundToInt()
                    } else 0
                    parsed.copy(netCaloriesBurned = netKcal)
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
            _state.update { it.copy(errorMessage = "Lütfen önce yapılan sporu yazınız (Örn: 30 dk koşu, 20 dk ağırlık).") }
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
                        infoMessage = "AI ile spor analizi tamamlandı: ${aiResult.caloriesBurned} kcal yakıldı."
                    )
                }
                launch { syncEngine.sync() }
            }.onFailure { err ->
                val errorMsg = err.message ?: "Bilinmeyen hata"
                println("[WorkoutViewModel] analyzeWorkout failure: $errorMsg")
                val displayMsg = when {
                    errorMsg.contains("404") ->
                        "Sunucuda AI servisi bulunamadı (404). Render üzerinde henüz yeni sürüm deploy edilmemiş olabilir."
                    errorMsg.contains("401") ->
                        "Oturum süresi doldu (401). Lütfen tekrar giriş yapınız."
                    errorMsg.contains("GEMINI_API_KEY is not configured", ignoreCase = true) ->
                        "Render sunucusunda GEMINI_API_KEY tanımlı değil."
                    errorMsg.contains("başarısız (HTTP", ignoreCase = true) -> {
                        val serverErr = if (errorMsg.contains("\"error\":")) {
                            errorMsg.substringAfter("\"error\":").substringBefore("}").trim('"', ' ', '\\')
                        } else {
                            errorMsg
                        }
                        "AI spor analizi başarısız: $serverErr"
                    }
                    errorMsg.contains("Connect", ignoreCase = true) || errorMsg.contains("timeout", ignoreCase = true) ->
                        "Sunucuya bağlanılamadı veya zaman aşımına uğradı. Lütfen internet bağlantınızı kontrol ediniz."
                    else ->
                        "AI spor analizi başarısız: $errorMsg"
                }

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
            _state.update { it.copy(errorMessage = "Lütfen aktivite adı veya yakılan kaloriyi giriniz.") }
            return
        }

        viewModelScope.launch {
            val now = currentTime()
            val workout = WorkoutLog(
                id = Uuid.random().toString(),
                userId = userId,
                date = date,
                rawText = if (rawText.isNotBlank()) rawText else "Egzersiz ($cal kcal)",
                caloriesBurned = cal,
                durationMinutes = dur,
                targetCalories = _state.value.targetCalories,
                targetDurationMinutes = _state.value.targetDurationMinutes,
                aiAnalysis = "Elle girildi",
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            database.upsertWorkout(workout)
            _state.update { it.copy(rawText = "", manualCalories = "", manualDuration = "") }
            _effects.send(WorkoutEffect.ShowToast("Egzersiz kaydedildi."))
            launch { syncEngine.sync() }
        }
    }

    private fun deleteWorkout(id: String) {
        viewModelScope.launch {
            database.deleteWorkout(id)
            launch { syncEngine.sync() }
        }
    }

    private fun parseDailyStepTelemetry(log: WorkoutLog): DailyStepTelemetry {
        val steps = Regex("""(\d+)\s*Adım""").find(log.rawText)?.groupValues?.get(1)?.toIntOrNull()
            ?: (log.durationMinutes * 100).coerceAtLeast(0)
        val analysis = log.aiAnalysis.orEmpty()
        val distanceKm = Regex("""([\d.,]+)\s*km""").find(analysis)?.groupValues?.get(1)
            ?.replace(',', '.')?.toDoubleOrNull()
            ?: ((steps * 0.75) / 1000.0)
        val source = if (analysis.contains("•")) {
            analysis.substringBefore("•").trim().removePrefix("⌚").trim()
        } else {
            "Akıllı Saat"
        }
        return DailyStepTelemetry(
            stepCount = steps,
            distanceKm = round(distanceKm * 10.0) / 10.0,
            durationMinutes = log.durationMinutes,
            caloriesBurned = log.caloriesBurned,
            netCaloriesBurned = log.caloriesBurned,
            sourceName = source.ifBlank { "Akıllı Saat" },
            stepTarget = 10_000
        )
    }

    private fun estimateSportSteps(rawText: String, durationMinutes: Int): Int {
        if (durationMinutes <= 0) return 0
        val lower = rawText.lowercase()
        val stepsPerMinute = when {
            lower.contains("koşu") || lower.contains("running") || lower.contains("jogging") -> 140
            lower.contains("tenis") || lower.contains("tennis") -> 85
            lower.contains("basket") || lower.contains("futbol") || lower.contains("soccer") -> 100
            lower.contains("yürüyüş") || lower.contains("walking") || lower.contains("trekking") -> 100
            lower.contains("hiit") || lower.contains("kardiyo") -> 90
            lower.contains("fitness") || lower.contains("ağırlık") || lower.contains("body") || lower.contains("bench") -> 25
            lower.contains("pilates") || lower.contains("yoga") -> 10
            lower.contains("bisiklet") || lower.contains("cycling") || lower.contains("yüzme") || lower.contains("swim") -> 0
            else -> 60
        }
        return stepsPerMinute * durationMinutes
    }

    private fun currentTime(): Long = com.yusufteker.healthtrack.core.utils.currentTimeMillis()
}
