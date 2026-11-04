package com.yusufteker.healthtrack.feature.weight.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.SyncEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class WeightViewModel(
    private val database: HealthDatabase,
    private val tokenStorage: TokenStorage,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _state = MutableStateFlow(WeightState())
    val state: StateFlow<WeightState> = _state.asStateFlow()

    private val _effects = Channel<WeightEffect>(Channel.BUFFERED)
    val effects: Flow<WeightEffect> = _effects.receiveAsFlow()

    private var weightObservationJob: Job? = null

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow.collect { session ->
                if (session.isLoggedIn) {
                    observeWeights(session.userId)
                }
            }
        }
    }

    fun onIntent(intent: WeightIntent) {
        when (intent) {
            is WeightIntent.OnPeriodSelected -> {
                _state.update {
                    val filtered = filterByPeriod(it.entries, intent.period)
                    it.copy(selectedPeriod = intent.period, filteredEntries = filtered)
                }
            }
            is WeightIntent.OnInputDateChanged -> _state.update { it.copy(inputDate = intent.date) }
            is WeightIntent.OnInputWeightChanged -> _state.update { it.copy(inputWeight = intent.weight) }
            is WeightIntent.OnInputNoteChanged -> _state.update { it.copy(inputNote = intent.note) }
            is WeightIntent.OnInputTargetWeightChanged -> _state.update { it.copy(inputTargetWeight = intent.target) }
            is WeightIntent.OnSaveWeightClicked -> saveWeight()
            is WeightIntent.OnDeleteWeight -> deleteWeight(intent.id)
            is WeightIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
            is WeightIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
        }
    }

    private fun observeWeights(userId: Int) {
        weightObservationJob?.cancel()
        weightObservationJob = viewModelScope.launch {
            database.observeWeights(userId).collect { entries ->
                val sorted = entries.sortedBy { it.date }
                val initial = sorted.firstOrNull()?.weightKg
                val current = sorted.lastOrNull()?.weightKg
                val change = if (initial != null && current != null) current - initial else null
                val target = sorted.lastOrNull()?.targetWeightKg ?: _state.value.targetWeightKg

                _state.update { s ->
                    val filtered = filterByPeriod(sorted, s.selectedPeriod)
                    s.copy(
                        entries = sorted,
                        filteredEntries = filtered,
                        initialWeightKg = initial,
                        currentWeightKg = current,
                        weightChangeKg = change,
                        targetWeightKg = target,
                        inputTargetWeight = target.toString()
                    )
                }
            }
        }
    }

    private fun filterByPeriod(entries: List<WeightEntry>, period: WeightChartPeriod): List<WeightEntry> {
        if (entries.isEmpty()) return emptyList()
        return when (period) {
            WeightChartPeriod.WEEKLY -> entries.takeLast(7)
            WeightChartPeriod.MONTHLY -> entries.takeLast(30)
            WeightChartPeriod.YEARLY -> entries.takeLast(365)
            WeightChartPeriod.ALL -> entries
        }
    }

    private fun saveWeight() {
        val weight = _state.value.inputWeight.toDoubleOrNull()
        val target = _state.value.inputTargetWeight.toDoubleOrNull()
        val date = _state.value.inputDate.trim()
        val note = _state.value.inputNote.trim().ifBlank { null }
        val userId = tokenStorage.getUserId()

        if (weight == null || weight <= 0) {
            _state.update { it.copy(errorMessage = "Lütfen geçerli bir kilo değeri giriniz (Örn: 74.5).") }
            return
        }

        viewModelScope.launch {
            val now = currentTime()
            // Check if entry for date already exists
            val existing = _state.value.entries.find { it.date == date }
            val id = existing?.id ?: Uuid.random().toString()

            val entry = WeightEntry(
                id = id,
                userId = userId,
                date = date,
                weightKg = weight,
                targetWeightKg = target,
                note = note,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )

            database.upsertWeight(entry)
            _state.update { it.copy(inputWeight = "", inputNote = "", infoMessage = "Kilo kaydedildi.") }
            _effects.send(WeightEffect.ShowToast("Kilo kaydedildi."))
            launch { syncEngine.sync() }
        }
    }

    private fun deleteWeight(id: String) {
        viewModelScope.launch {
            database.deleteWeight(id)
            launch { syncEngine.sync() }
        }
    }

    private fun currentTime(): Long = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds + 1720000000000L
}
