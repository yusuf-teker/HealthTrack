package com.yusufteker.healthtrack.feature.weight.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.normalizeDateToIso
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
    private val syncEngine: SyncEngine,
    private val deviceHealthSyncManager: DeviceHealthSyncManager
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
            is WeightIntent.OnSetEditingTarget -> _state.update { it.copy(isEditingTarget = intent.isEditing) }
            is WeightIntent.OnSaveTargetWeight -> saveTargetWeight(intent.newTargetWeight)
            is WeightIntent.OnSaveWeightClicked -> saveWeight()
            is WeightIntent.OnScaleSyncStarted -> {
                _state.update { it.copy(isSyncingScale = true, errorMessage = null, infoMessage = null) }
            }
            is WeightIntent.OnScaleSnapshotReceived -> {
                viewModelScope.launch {
                    intent.result.onSuccess { snapshot ->
                        val summary = deviceHealthSyncManager.mergeSnapshot(snapshot)
                        val msg = summary.toUserMessage()
                        _state.update { it.copy(isSyncingScale = false, infoMessage = msg) }
                        _effects.send(WeightEffect.ShowToast(msg))
                    }.onFailure { err ->
                        _state.update {
                            it.copy(
                                isSyncingScale = false,
                                errorMessage = err.message ?: "Tartı verileri alınamadı."
                            )
                        }
                    }
                }
            }
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
                val userWeight = tokenStorage.getUserWeight().takeIf { it > 0.0 }
                val initial = sorted.firstOrNull()?.weightKg ?: userWeight
                val current = sorted.lastOrNull()?.weightKg ?: userWeight
                val change = if (initial != null && current != null && sorted.isNotEmpty()) current - initial else null
                val savedTarget = tokenStorage.getUserTargetWeight().takeIf { it > 0.0 } ?: 70.0
                val target = sorted.lastOrNull { it.targetWeightKg != null }?.targetWeightKg ?: savedTarget

                // Keep tokenStorage user weight in sync with latest recorded entry
                if (current != null && current > 0.0) {
                    tokenStorage.setUserWeight(current)
                }

                _state.update { s ->
                    val filtered = filterByPeriod(sorted, s.selectedPeriod)
                    s.copy(
                        entries = sorted,
                        filteredEntries = filtered,
                        initialWeightKg = initial,
                        currentWeightKg = current,
                        weightChangeKg = change,
                        targetWeightKg = target
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
        val target = _state.value.targetWeightKg
        val date = normalizeDateToIso(_state.value.inputDate)
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
            val id = existing?.id ?: "weight_${userId}_${date}"

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
            tokenStorage.setUserWeight(weight)
            _state.update { it.copy(inputWeight = "", inputNote = "", infoMessage = "Kilo kaydedildi.") }
            _effects.send(WeightEffect.ShowToast("Kilo kaydedildi."))
            launch { syncEngine.sync() }
        }
    }

    private fun saveTargetWeight(newTarget: Double) {
        if (newTarget <= 0 || newTarget > 350) {
            _state.update { it.copy(errorMessage = "Lütfen geçerli bir hedef kilo giriniz.") }
            return
        }
        tokenStorage.setUserTargetWeight(newTarget)
        _state.update {
            it.copy(
                targetWeightKg = newTarget,
                isEditingTarget = false,
                infoMessage = "Hedef kilo güncellendi: $newTarget kg"
            )
        }

        viewModelScope.launch {
            val latest = _state.value.entries.lastOrNull()
            if (latest != null) {
                val updated = latest.copy(
                    targetWeightKg = newTarget,
                    updatedAt = currentTime(),
                    syncStatus = SyncStatus.PENDING
                )
                database.upsertWeight(updated)
                launch { syncEngine.sync() }
            }
            _effects.send(WeightEffect.ShowToast("Hedef kilo güncellendi: $newTarget kg"))
        }
    }

    private fun deleteWeight(id: String) {
        val userId = tokenStorage.getUserId()
        viewModelScope.launch {
            database.deleteWeight(id)
            val remaining = database.getWeights(userId).sortedBy { it.date }
            val latest = remaining.lastOrNull()?.weightKg
            if (latest != null && latest > 0.0) {
                tokenStorage.setUserWeight(latest)
            }
            launch { syncEngine.sync() }
        }
    }

    private fun currentTime(): Long = currentTimeMillis()
}
