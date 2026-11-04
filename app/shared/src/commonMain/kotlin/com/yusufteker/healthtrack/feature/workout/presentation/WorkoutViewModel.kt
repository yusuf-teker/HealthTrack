package com.yusufteker.healthtrack.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.SyncEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class WorkoutViewModel(
    private val database: HealthDatabase,
    private val apiClient: HealthApiClient,
    private val tokenStorage: TokenStorage,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private val _effects = Channel<WorkoutEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutEffect> = _effects.receiveAsFlow()

    private var workoutObservationJob: Job? = null
    private var foodObservationJob: Job? = null
    private var weightObservationJob: Job? = null

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow.collect { session ->
                if (session.isLoggedIn) {
                    observeWorkoutsAndCalories(session.userId, _state.value.selectedDate)
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
            is WorkoutIntent.OnDeleteWorkout -> deleteWorkout(intent.id)
            is WorkoutIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
            is WorkoutIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
        }
    }

    private fun observeWorkoutsAndCalories(userId: Int, date: String) {
        workoutObservationJob?.cancel()
        workoutObservationJob = viewModelScope.launch {
            database.observeWorkouts(userId, date).collect { list ->
                val burned = list.sumOf { it.caloriesBurned }
                val duration = list.sumOf { it.durationMinutes }
                _state.update { current ->
                    current.copy(
                        workouts = list,
                        totalCaloriesBurned = burned,
                        totalDurationMinutes = duration,
                        netCalories = current.consumedCalories - burned
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

    private fun observeWeight(userId: Int) {
        weightObservationJob?.cancel()
        weightObservationJob = viewModelScope.launch {
            database.observeWeights(userId).collect { weightList ->
                val latest = weightList.maxByOrNull { it.date }
                if (latest != null) {
                    _state.update { it.copy(userWeightKg = latest.weightKg) }
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
                val displayMsg = when {
                    errorMsg.contains("404") ->
                        "Sunucuda AI servisi bulunamadı (404). Render üzerinde henüz yeni sürüm deploy edilmemiş olabilir."
                    errorMsg.contains("401") ->
                        "Oturum süresi doldu (401). Lütfen tekrar giriş yapınız."
                    errorMsg.contains("500") ->
                        "Sunucu hatası (500). Render sunucusunda GEMINI_API_KEY tanımlı olduğundan emin olunuz."
                    errorMsg.contains("Connect", ignoreCase = true) || errorMsg.contains("timeout", ignoreCase = true) ->
                        "Sunucuya bağlanılamadı. Lütfen internet bağlantınızı kontrol ediniz veya kalorinizi elle giriniz."
                    else ->
                        "AI hesaplama başarısız: $errorMsg"
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

    private fun currentTime(): Long = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds + 1720000000000L
}
