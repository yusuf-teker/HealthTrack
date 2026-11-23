package com.yusufteker.healthtrack.feature.weight.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.DeviceHealthSnapshot
import com.yusufteker.healthtrack.core.model.DeviceWeightRecord
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.model.WeightChartPeriod
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.utils.BodyCompositionCalculator
import com.yusufteker.healthtrack.core.utils.FullBodyCompositionAnalytics
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.normalizeDateToIso
import com.yusufteker.healthtrack.core.utils.toLocaleDoubleOrNull
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

    private val _state = MutableStateFlow(
        WeightState(
            userHeightCm = tokenStorage.getUserHeight().takeIf { h -> h > 50.0 } ?: 175.0,
            userAge = tokenStorage.getUserAge().takeIf { a -> a > 0 } ?: 26,
            userIsMale = !tokenStorage.getUserGender().equals("FEMALE", ignoreCase = true),
            reminderFrequency = tokenStorage.getWeightReminderFrequency(),
            reminderHour = tokenStorage.getWeightReminderHour(),
            reminderMinute = tokenStorage.getWeightReminderMinute()
        )
    )
    val state: StateFlow<WeightState> = _state.asStateFlow()

    private val _effects = Channel<WeightEffect>(Channel.BUFFERED)
    val effects: Flow<WeightEffect> = _effects.receiveAsFlow()

    private var weightObservationJob: Job? = null

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow
                .distinctUntilChangedBy { it.hasActiveSession to it.userId }
                .collect { session ->
                    if (session.hasActiveSession && session.userId != 0) {
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
            is WeightIntent.OnMetricSelected -> _state.update { it.copy(selectedMetric = intent.metric) }
            is WeightIntent.OnInputDateChanged -> _state.update { it.copy(inputDate = intent.date) }
            is WeightIntent.OnInputWeightChanged -> _state.update { it.copy(inputWeight = intent.weight, errorMessage = null) }
            is WeightIntent.OnInputBodyFatChanged -> _state.update { it.copy(inputBodyFat = intent.bodyFat, errorMessage = null) }
            is WeightIntent.OnInputMuscleValueChanged -> _state.update { it.copy(inputMuscleValue = intent.muscleValue, errorMessage = null) }
            is WeightIntent.OnToggleInputMuscleUnit -> _state.update { it.copy(inputMuscleIsPercent = !it.inputMuscleIsPercent, errorMessage = null) }
            is WeightIntent.OnInputNoteChanged -> _state.update { it.copy(inputNote = intent.note) }
            is WeightIntent.OnSetAddDialogOpen -> _state.update {
                it.copy(isAddDialogOpen = intent.isOpen, errorMessage = if (intent.isOpen) null else it.errorMessage)
            }
            is WeightIntent.OnSetBleScaleSheetOpen -> _state.update {
                it.copy(
                    isBleScaleSheetOpen = intent.isOpen,
                    userHeightCm = tokenStorage.getUserHeight().takeIf { h -> h > 50.0 } ?: 175.0,
                    userAge = tokenStorage.getUserAge().takeIf { a -> a > 0 } ?: 26,
                    userIsMale = !tokenStorage.getUserGender().equals("FEMALE", ignoreCase = true)
                )
            }
            is WeightIntent.OnSaveBleScaleReading -> saveBleScaleReading(intent.reading)
            is WeightIntent.OnSetEditingTarget -> _state.update { it.copy(isEditingTarget = intent.isEditing) }
            is WeightIntent.OnSaveTargetWeight -> saveTargetWeight(intent.newTargetWeight)
            is WeightIntent.OnSaveWeightClicked -> saveWeight()
            is WeightIntent.OnScaleSyncStarted -> {
                _state.update { it.copy(isSyncingScale = true, errorMessage = null, infoMessage = null) }
            }
            is WeightIntent.OnScaleSnapshotReceived -> {
                viewModelScope.launch {
                    val lang = LocalizationManager.currentLanguage
                    intent.result.onSuccess { snapshot ->
                        val summary = deviceHealthSyncManager.mergeSnapshot(snapshot)
                        if (!intent.isSilent) {
                            val msg = summary.toUserMessage(lang)
                            _state.update { it.copy(isSyncingScale = false, infoMessage = msg) }
                        } else {
                            _state.update { it.copy(isSyncingScale = false) }
                        }
                    }.onFailure { err ->
                        if (!intent.isSilent) {
                            _state.update {
                                it.copy(
                                    isSyncingScale = false,
                                    errorMessage = err.message ?: trEn("Tartı verileri alınamadı.", "Could not read scale data.", lang)
                                )
                            }
                        } else {
                            _state.update { it.copy(isSyncingScale = false) }
                        }
                    }
                }
            }
            is WeightIntent.OnDeleteWeight -> deleteWeight(intent.id)
            is WeightIntent.OnReminderFrequencyChanged -> {
                onIntent(WeightIntent.OnReminderSettingsChanged(intent.frequency, state.value.reminderHour, state.value.reminderMinute))
            }
            is WeightIntent.OnReminderSettingsChanged -> {
                tokenStorage.setWeightReminderFrequency(intent.frequency)
                tokenStorage.setWeightReminderHour(intent.hour)
                tokenStorage.setWeightReminderMinute(intent.minute)
                _state.update {
                    it.copy(
                        reminderFrequency = intent.frequency,
                        reminderHour = intent.hour,
                        reminderMinute = intent.minute,
                        isReminderDialogOpen = false
                    )
                }
                val lang = LocalizationManager.currentLanguage
                val timeStr = "${intent.hour.toString().padStart(2, '0')}:${intent.minute.toString().padStart(2, '0')}"
                val msg = if (intent.frequency == com.yusufteker.healthtrack.core.model.WeightReminderFrequency.OFF) {
                    trEn("Tartı hatırlatıcısı kapatıldı.", "Weigh-in reminder turned off.", lang)
                } else {
                    trEn(
                        "Tartı hatırlatıcısı güncellendi: ${intent.frequency.localizedTitle(lang)} ($timeStr)",
                        "Weigh-in reminder updated: ${intent.frequency.localizedTitle(lang)} ($timeStr)",
                        lang
                    )
                }
                _state.update { it.copy(infoMessage = msg) }
            }
            is WeightIntent.OnSetReminderDialogOpen -> _state.update { it.copy(isReminderDialogOpen = intent.isOpen) }
            is WeightIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
            is WeightIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
        }
    }

    private fun observeWeights(userId: Int) {
        weightObservationJob?.cancel()
        weightObservationJob = viewModelScope.launch {
            database.observeWeights(userId).collect { entries ->
                val userHeightCm = tokenStorage.getUserHeight().takeIf { it > 50.0 } ?: 175.0
                val userAge = tokenStorage.getUserAge().takeIf { it > 0 } ?: 26
                val isMale = !tokenStorage.getUserGender().equals("FEMALE", ignoreCase = true)

                val sorted = entries.sortedBy { it.date }.map { entry ->
                    if (entry.bodyFatPercent == null || entry.musclePercent == null || entry.muscleMassKg == null) {
                        val resolved = if (entry.bodyFatPercent != null) {
                            BodyCompositionCalculator.resolveFromDevice(
                                weightKg = entry.weightKg,
                                bodyFatPercent = entry.bodyFatPercent,
                                muscleMassKg = entry.muscleMassKg,
                                musclePercent = entry.musclePercent,
                                isMale = isMale
                            )
                        } else {
                            BodyCompositionCalculator.parseLegacyBodyCompositionFromNote(
                                note = entry.note,
                                weightKg = entry.weightKg,
                                isMale = isMale
                            )
                        }
                        if (resolved != null) {
                            entry.copy(
                                bodyFatPercent = entry.bodyFatPercent ?: resolved.bodyFatPercent,
                                muscleMassKg = entry.muscleMassKg ?: resolved.muscleMassKg,
                                musclePercent = entry.musclePercent ?: resolved.musclePercent
                            )
                        } else {
                            entry
                        }
                    } else {
                        entry
                    }
                }

                val userWeight = tokenStorage.getUserWeight().takeIf { it > 0.0 }
                val initial = sorted.firstOrNull()?.weightKg ?: userWeight
                val current = sorted.lastOrNull()?.weightKg ?: userWeight
                val change = if (initial != null && current != null && sorted.isNotEmpty()) current - initial else null
                val savedTarget = tokenStorage.getUserTargetWeight().takeIf { it > 0.0 } ?: 70.0
                val target = sorted.lastOrNull { it.targetWeightKg != null }?.targetWeightKg ?: savedTarget

                val fatEntries = sorted.filter { it.bodyFatPercent != null }
                val currentFat = fatEntries.lastOrNull()?.bodyFatPercent
                val firstFat = fatEntries.firstOrNull()?.bodyFatPercent
                val fatChange = if (fatEntries.size >= 2 && firstFat != null && currentFat != null) {
                    BodyCompositionCalculator.round1(currentFat - firstFat)
                } else null

                val muscleEntries = sorted.filter { it.musclePercent != null }
                val currentMusclePct = muscleEntries.lastOrNull()?.musclePercent
                val currentMuscleKg = muscleEntries.lastOrNull()?.muscleMassKg
                val firstMusclePct = muscleEntries.firstOrNull()?.musclePercent
                val musclePctChange = if (muscleEntries.size >= 2 && firstMusclePct != null && currentMusclePct != null) {
                    BodyCompositionCalculator.round1(currentMusclePct - firstMusclePct)
                } else null

                val currentImpedance = sorted.lastOrNull { it.impedanceOhm != null }?.impedanceOhm
                val report = current?.let { w ->
                    FullBodyCompositionAnalytics.calculate(
                        weightKg = w,
                        heightCm = userHeightCm,
                        age = userAge,
                        isMale = isMale,
                        bodyFatPercent = currentFat,
                        muscleMassKg = currentMuscleKg,
                        musclePercent = currentMusclePct,
                        impedanceOhm = currentImpedance
                    )
                }

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
                        targetWeightKg = target,
                        currentBodyFatPercent = currentFat,
                        bodyFatChangePercent = fatChange,
                        currentMuscleMassKg = currentMuscleKg,
                        currentMusclePercent = currentMusclePct,
                        musclePercentChange = musclePctChange,
                        bodyCompositionReport = report,
                        userHeightCm = userHeightCm,
                        userAge = userAge,
                        userIsMale = isMale
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
        val weight = _state.value.inputWeight.toLocaleDoubleOrNull()
        val target = _state.value.targetWeightKg
        val date = normalizeDateToIso(_state.value.inputDate)
        val note = _state.value.inputNote.trim().ifBlank { null }
        val userId = tokenStorage.getUserId()

        val lang = LocalizationManager.currentLanguage
        if (weight == null || weight < 25.0 || weight > 250.0) {
            _state.update {
                it.copy(
                    errorMessage = trEn(
                        "Lütfen 25 ile 250 kg arasında geçerli bir kilo giriniz (Örn: 74.5).",
                        "Please enter a valid weight between 25 and 250 kg (e.g. 74.5).",
                        lang
                    )
                )
            }
            return
        }

        val rawBodyFat = _state.value.inputBodyFat.trim()
        val parsedBodyFat = if (rawBodyFat.isEmpty()) {
            null
        } else {
            val v = rawBodyFat.toLocaleDoubleOrNull()
            if (v == null || v !in 3.0..75.0) {
                _state.update {
                    it.copy(
                        errorMessage = trEn(
                            "Lütfen %3 ile %75 arasında geçerli bir yağ oranı giriniz.",
                            "Please enter a valid body fat percentage between 3 and 75.",
                            lang
                        )
                    )
                }
                return
            }
            v
        }

        val rawMuscle = _state.value.inputMuscleValue.trim()
        val isMusclePercent = _state.value.inputMuscleIsPercent
        val parsedMuscle = if (rawMuscle.isEmpty()) {
            null
        } else {
            val v = rawMuscle.toLocaleDoubleOrNull()
            val isValid = if (v == null) false else if (isMusclePercent) v in 15.0..90.0 else v in 10.0..weight && v <= 120.0
            if (!isValid) {
                _state.update {
                    it.copy(
                        errorMessage = if (isMusclePercent) {
                            trEn(
                                "Lütfen %15 ile %90 arasında geçerli bir kas oranı giriniz.",
                                "Please enter a valid muscle percentage between 15 and 90.",
                                lang
                            )
                        } else {
                            trEn(
                                "Lütfen 10 kg ile kilonuz ($weight kg) arasında geçerli bir kas kütlesi giriniz.",
                                "Please enter a valid muscle mass between 10 kg and your weight ($weight kg).",
                                lang
                            )
                        }
                    )
                }
                return
            }
            v
        }

        val manualComp = BodyCompositionCalculator.resolveFromManualInput(
            weightKg = weight,
            bodyFatPercent = parsedBodyFat,
            muscleValue = parsedMuscle,
            isMuscleUnitPercent = isMusclePercent
        )

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
                bodyFatPercent = manualComp.bodyFatPercent ?: existing?.bodyFatPercent,
                muscleMassKg = manualComp.muscleMassKg ?: existing?.muscleMassKg,
                musclePercent = manualComp.musclePercent ?: existing?.musclePercent,
                note = note,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )

            database.upsertWeight(entry)
            tokenStorage.setUserWeight(weight)
            val savedMsg = trEn("Kilo kaydedildi.", "Weight saved.", lang)
            _state.update {
                it.copy(
                    inputWeight = "",
                    inputBodyFat = "",
                    inputMuscleValue = "",
                    inputNote = "",
                    isAddDialogOpen = false,
                    errorMessage = null,
                    infoMessage = savedMsg
                )
            }
            launch { syncEngine.sync() }
        }
    }

    private fun saveBleScaleReading(reading: com.yusufteker.healthtrack.core.utils.BleScaleReading) {
        viewModelScope.launch {
            val lang = LocalizationManager.currentLanguage
            val now = currentTime()
            val todayIso = getTodayIso()
            val snapshot = DeviceHealthSnapshot(
                weights = listOf(
                    DeviceWeightRecord(
                        externalId = "ble_$now",
                        date = todayIso,
                        weightKg = reading.weightKg,
                        bodyFatPercent = reading.bodyFatPercent,
                        leanBodyMassKg = reading.leanBodyMassKg,
                        boneMassKg = reading.boneMassKg,
                        muscleMassKg = reading.muscleMassKg,
                        musclePercent = reading.musclePercent,
                        impedanceOhm = reading.impedanceOhm,
                        measuredAtMillis = now,
                        sourcePackage = "ble.scale",
                        sourceName = reading.scaleModel
                    )
                ),
                platformName = "Bluetooth (${reading.scaleModel})"
            )

            val summary = deviceHealthSyncManager.mergeSnapshot(snapshot)
            val fatSuffix = reading.bodyFatPercent?.let { " • %$it ${trEn("Yağ", "Fat", lang)}" }.orEmpty()
            val muscleSuffix = reading.musclePercent?.let { " • %$it ${trEn("Kas", "Muscle", lang)}" }.orEmpty()
            val msg = if (summary.totalChanges > 0) {
                trEn(
                    "${reading.scaleModel} ölçümü kaydedildi: ${reading.weightKg} kg$fatSuffix$muscleSuffix",
                    "${reading.scaleModel} reading saved: ${reading.weightKg} kg$fatSuffix$muscleSuffix",
                    lang
                )
            } else {
                summary.toUserMessage(lang)
            }

            _state.update {
                it.copy(
                    isBleScaleSheetOpen = false,
                    infoMessage = msg
                )
            }
        }
    }

    private fun saveTargetWeight(newTarget: Double) {
        val lang = LocalizationManager.currentLanguage
        if (newTarget <= 0 || newTarget > 350) {
            _state.update {
                it.copy(
                    errorMessage = trEn("Lütfen geçerli bir hedef kilo giriniz.", "Please enter a valid target weight.", lang)
                )
            }
            return
        }
        tokenStorage.setUserTargetWeight(newTarget)
        val updatedMsg = trEn("Hedef kilo güncellendi: $newTarget kg", "Target weight updated: $newTarget kg", lang)
        _state.update {
            it.copy(
                targetWeightKg = newTarget,
                isEditingTarget = false,
                infoMessage = updatedMsg
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
