package com.yusufteker.healthtrack.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.calculateProteinTarget
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.sync.SyncState
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.theme.AppThemeColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileViewModel(
    private val database: HealthDatabase,
    private val tokenStorage: TokenStorage,
    private val syncEngine: SyncEngine,
    private val settings: Settings,
    private val deviceHealthSyncManager: DeviceHealthSyncManager,
    private val apiClient: HealthApiClient
) : ViewModel() {

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var hasUserModifiedWeight = false

    init {
        observeLatestWeight()
        observeSyncState()
        observeNutritionGoal()
        refreshAiQuota()
        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            tokenStorage.isPremiumFlow.collect { isPrem ->
                _state.update { it.copy(isPremium = isPrem) }
            }
        }
        viewModelScope.launch {
            tokenStorage.premiumUntilFlow.collect { until ->
                _state.update { it.copy(premiumUntil = until) }
            }
        }
    }

    private fun refreshAiQuota() {
        if (!tokenStorage.getCustomGeminiApiKey().isNullOrBlank()) return
        viewModelScope.launch {
            apiClient.getAiQuota().onSuccess { quota ->
                _state.update { it.copy(aiQuota = quota) }
            }
        }
    }

    private fun observeNutritionGoal() {
        val userId = tokenStorage.getUserId()
        if (userId <= 0) return
        viewModelScope.launch {
            database.observeNutritionGoal(userId).collect { goal ->
                val resolvedFitnessGoal = if (goal.fitnessGoal.isNotBlank() && goal.fitnessGoal != FitnessGoal.MAINTAIN.id) {
                    goal.parsedFitnessGoal
                } else {
                    tokenStorage.getUserFitnessGoal()
                }
                _state.update {
                    it.copy(
                        nutritionGoal = goal,
                        fitnessGoal = resolvedFitnessGoal
                    )
                }
            }
        }
    }

    private fun createInitialState(): ProfileState {
        val userId = tokenStorage.getUserId()
        val name = tokenStorage.getName()
        val username = tokenStorage.getUsername()
        val height = tokenStorage.getUserHeight().takeIf { it > 0.0 } ?: 175.0
        val targetWeight = tokenStorage.getUserTargetWeight().takeIf { it > 0.0 } ?: 70.0
        val weight = tokenStorage.getUserWeight().takeIf { it > 0.0 } ?: 72.0
        val age = tokenStorage.getUserAge().takeIf { it > 0 } ?: 26
        val gender = tokenStorage.getUserGender().takeIf { it.isNotBlank() } ?: "MALE"
        val fitnessGoal = tokenStorage.getUserFitnessGoal()
        val workoutReminder = tokenStorage.getWorkoutReminderFrequency()
        val weightReminder = tokenStorage.getWeightReminderFrequency()
        val isDark = settings.getBoolean("dark_mode", true)
        val accent = AppThemeColor.fromId(settings.getString("theme_accent", AppThemeColor.OCEAN.id))
        val customGeminiApiKey = tokenStorage.getCustomGeminiApiKey().orEmpty()

        return ProfileState(
            userId = userId,
            name = name,
            username = username,
            weightKg = weight,
            targetWeightKg = targetWeight,
            heightCm = height,
            age = age,
            gender = gender,
            fitnessGoal = fitnessGoal,
            workoutReminderFrequency = workoutReminder,
            weightReminderFrequency = weightReminder,
            isDarkMode = isDark,
            themeAccent = accent,
            customGeminiApiKey = customGeminiApiKey
        )
    }

    private fun observeLatestWeight() {
        val userId = tokenStorage.getUserId()
        if (userId <= 0) return

        viewModelScope.launch {
            database.observeWeights(userId).collect { entries ->
                val sorted = entries.sortedBy { it.date }
                val latest = sorted.lastOrNull()
                if (latest != null) {
                    _state.update { current ->
                        current.copy(
                            weightKg = if (!hasUserModifiedWeight) latest.weightKg else current.weightKg,
                            latestWeightDate = latest.date,
                            targetWeightKg = latest.targetWeightKg ?: current.targetWeightKg
                        )
                    }
                }
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            syncEngine.syncState.collect { syncState ->
                _state.update { it.copy(isSyncing = syncState is SyncState.Syncing) }
            }
        }
    }

    private var autoSaveJob: Job? = null

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.OnWeightChanged -> {
                hasUserModifiedWeight = true
                val w = kotlin.math.round(intent.weight * 10) / 10.0
                _state.update { it.copy(weightKg = w) }
                scheduleAutoSave(debounceMs = 350L)
            }
            is ProfileIntent.OnTargetWeightChanged -> {
                val t = kotlin.math.round(intent.target * 10) / 10.0
                _state.update { it.copy(targetWeightKg = t) }
                scheduleAutoSave(debounceMs = 350L)
            }
            is ProfileIntent.OnHeightChanged -> {
                val h = kotlin.math.round(intent.height * 10) / 10.0
                _state.update { it.copy(heightCm = h) }
                scheduleAutoSave(debounceMs = 350L)
            }
            is ProfileIntent.OnAgeChanged -> {
                val a = intent.age.coerceIn(10, 120)
                _state.update { it.copy(age = a) }
                scheduleAutoSave(debounceMs = 350L)
            }
            is ProfileIntent.OnGenderChanged -> {
                _state.update { it.copy(gender = intent.gender) }
                scheduleAutoSave(debounceMs = 0L)
            }
            is ProfileIntent.OnFitnessGoalChanged -> {
                tokenStorage.setUserFitnessGoal(intent.goal)
                _state.update { it.copy(fitnessGoal = intent.goal) }
                scheduleAutoSave(debounceMs = 0L)
            }
            is ProfileIntent.OnSaveNutritionGoal -> {
                viewModelScope.launch {
                    val userId = tokenStorage.getUserId()
                    val updated = intent.goal.copy(
                        userId = userId,
                        fitnessGoal = intent.fitnessGoal.id,
                        updatedAt = currentTimeMillis(),
                        syncStatus = SyncStatus.PENDING
                    )
                    tokenStorage.setUserFitnessGoal(intent.fitnessGoal)
                    database.upsertNutritionGoal(updated)
                    _state.update { it.copy(nutritionGoal = updated, fitnessGoal = intent.fitnessGoal) }
                    _effects.send(ProfileEffect.ShowToast("${intent.fitnessGoal.title} ve besin hedefleri güncellendi."))
                    launch { syncEngine.sync() }
                }
            }
            is ProfileIntent.OnWorkoutReminderFrequencyChanged -> {
                tokenStorage.setWorkoutReminderFrequency(intent.frequency)
                _state.update { it.copy(workoutReminderFrequency = intent.frequency) }
                val msg = if (intent.frequency == com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency.OFF) {
                    "Spora teşvik bildirimi kapatıldı."
                } else {
                    "Spora teşvik sıklığı: ${intent.frequency.title} olarak ayarlandı."
                }
                viewModelScope.launch {
                    _effects.send(ProfileEffect.ShowToast(msg))
                }
            }
            is ProfileIntent.OnWeightReminderFrequencyChanged -> {
                tokenStorage.setWeightReminderFrequency(intent.frequency)
                _state.update { it.copy(weightReminderFrequency = intent.frequency) }
                val msg = if (intent.frequency == com.yusufteker.healthtrack.core.model.WeightReminderFrequency.OFF) {
                    "Tartılma hatırlatıcısı kapatıldı."
                } else {
                    "Tartılma hatırlatıcısı: ${intent.frequency.title} olarak ayarlandı."
                }
                viewModelScope.launch {
                    _effects.send(ProfileEffect.ShowToast(msg))
                }
            }
            is ProfileIntent.OnThemeModeChanged -> {
                settings.putBoolean("dark_mode", intent.isDark)
                _state.update { it.copy(isDarkMode = intent.isDark) }
            }
            is ProfileIntent.OnThemeAccentChanged -> {
                settings.putString("theme_accent", intent.accent.id)
                _state.update { it.copy(themeAccent = intent.accent) }
            }
            is ProfileIntent.OnSaveCustomGeminiApiKey -> {
                val trimmed = intent.apiKey.trim()
                tokenStorage.setCustomGeminiApiKey(trimmed)
                _state.update { it.copy(customGeminiApiKey = trimmed) }
                viewModelScope.launch {
                    _effects.send(ProfileEffect.ShowToast("Özel Gemini API anahtarı kaydedildi."))
                }
            }
            ProfileIntent.OnClearCustomGeminiApiKey -> {
                tokenStorage.setCustomGeminiApiKey(null)
                _state.update { it.copy(customGeminiApiKey = "") }
                viewModelScope.launch {
                    _effects.send(ProfileEffect.ShowToast("Özel API anahtarı kaldırıldı, varsayılan AI kullanılacak."))
                }
            }
            ProfileIntent.OnSyncClicked -> {
                viewModelScope.launch {
                    val success = syncEngine.sync(forceFullPull = true)
                    if (success) {
                        _effects.send(ProfileEffect.ShowToast("Senkronizasyon tamamlandı."))
                    } else {
                        _effects.send(ProfileEffect.ShowToast("Senkronizasyon tamamlanamadı veya çevrimdışı."))
                    }
                }
            }
            ProfileIntent.OnDeviceSyncStarted -> {
                _state.update { it.copy(isDeviceSyncing = true, errorMessage = null, infoMessage = null) }
            }
            is ProfileIntent.OnDeviceSnapshotReceived -> {
                viewModelScope.launch {
                    intent.result.onSuccess { snapshot ->
                        val summary = deviceHealthSyncManager.mergeSnapshot(snapshot)
                        val msg = summary.toUserMessage()
                        _state.update {
                            it.copy(
                                isDeviceSyncing = false,
                                lastDeviceSyncSummary = msg,
                                infoMessage = msg
                            )
                        }
                        _effects.send(ProfileEffect.ShowToast(msg))
                    }.onFailure { err ->
                        val msg = err.message ?: "Cihaz senkronizasyonu başarısız oldu."
                        _state.update {
                            it.copy(
                                isDeviceSyncing = false,
                                errorMessage = msg
                            )
                        }
                    }
                }
            }
            ProfileIntent.OnLogoutClicked -> {
                viewModelScope.launch {
                    database.clearAll()
                    tokenStorage.clearSession()
                    _effects.send(ProfileEffect.LoggedOut)
                }
            }
            ProfileIntent.OnDeleteAccountConfirmed -> {
                viewModelScope.launch {
                    _state.update { it.copy(isDeletingAccount = true, errorMessage = null) }
                    apiClient.deleteAccount()
                        .onSuccess {
                            database.clearAll()
                            tokenStorage.clearSession()
                            _state.update { it.copy(isDeletingAccount = false) }
                            _effects.send(ProfileEffect.LoggedOut)
                        }
                        .onFailure { error ->
                            _state.update {
                                it.copy(
                                    isDeletingAccount = false,
                                    errorMessage = "Hesap silinemedi: ${error.message ?: "Bilinmeyen hata"}"
                                )
                            }
                        }
                }
            }
            ProfileIntent.OnSaveProfileClicked -> saveProfile()
            ProfileIntent.OnScreenAppeared -> {
                val currentFitnessGoal = tokenStorage.getUserFitnessGoal()
                _state.update { current ->
                    current.copy(fitnessGoal = currentFitnessGoal)
                }
            }
            ProfileIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
            ProfileIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun scheduleAutoSave(debounceMs: Long) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            persistProfileChanges()
        }
    }

    private suspend fun persistProfileChanges() {
        val s = _state.value
        val userId = s.userId
        if (userId <= 0) return

        try {
            // Update persistent token storage
            tokenStorage.setUserWeight(s.weightKg)
            tokenStorage.setUserTargetWeight(s.targetWeightKg)
            tokenStorage.setUserHeight(s.heightCm)
            tokenStorage.setUserAge(s.age)
            tokenStorage.setUserGender(s.gender)
            tokenStorage.setUserFitnessGoal(s.fitnessGoal)
            settings.putBoolean("dark_mode", s.isDarkMode)
            settings.putString("theme_accent", s.themeAccent.id)

            // Update Nutrition Goal with recalculated dynamic protein
            val calculatedProtein = calculateProteinTarget(s.weightKg, s.fitnessGoal)
            val currentGoal = database.observeNutritionGoal(userId).firstOrNull() ?: NutritionGoal(id = "goal_$userId", userId = userId)
            database.upsertNutritionGoal(
                currentGoal.copy(
                    proteinG = calculatedProtein,
                    fitnessGoal = s.fitnessGoal.id,
                    updatedAt = currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING
                )
            )

            // Kilo ve hedef kilo kaydı kontrolü:
            val today = getTodayIso()
            val existingWeights = database.getWeights(userId).sortedBy { it.date }
            val todayEntry = existingWeights.find { it.date == today }
            val now = currentTimeMillis()

            if (todayEntry == null || hasUserModifiedWeight) {
                val entry = WeightEntry(
                    id = todayEntry?.id ?: "weight_${userId}_${today}",
                    userId = userId,
                    date = today,
                    weightKg = s.weightKg,
                    targetWeightKg = s.targetWeightKg,
                    note = todayEntry?.note ?: "Profil Kilo Kaydı",
                    createdAt = todayEntry?.createdAt ?: now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
                database.upsertWeight(entry)
                hasUserModifiedWeight = false
            } else {
                val latest = existingWeights.lastOrNull()
                if (latest != null && latest.targetWeightKg != s.targetWeightKg) {
                    database.upsertWeight(
                        latest.copy(
                            targetWeightKg = s.targetWeightKg,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING
                        )
                    )
                }
            }

            // Cloud sync in background
            syncEngine.sync()
        } catch (_: Exception) {
            // Quietly retain offline SSOT
        }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                persistProfileChanges()
                _state.update { it.copy(isSaving = false, infoMessage = "Değişiklikler kaydedildi.") }
                _effects.send(ProfileEffect.ShowToast("Değişiklikler kaydedildi."))
                _effects.send(ProfileEffect.NavigateBack)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "Kayıt sırasında hata: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val s = _state.value
        if (s.userId > 0) {
            tokenStorage.setUserWeight(s.weightKg)
            tokenStorage.setUserTargetWeight(s.targetWeightKg)
            tokenStorage.setUserHeight(s.heightCm)
            tokenStorage.setUserAge(s.age)
            tokenStorage.setUserGender(s.gender)
            tokenStorage.setUserFitnessGoal(s.fitnessGoal)
        }
    }
}
