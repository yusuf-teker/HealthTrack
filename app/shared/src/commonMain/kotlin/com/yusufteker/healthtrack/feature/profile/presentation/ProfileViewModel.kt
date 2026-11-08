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
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.sync.SyncState
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.theme.AppThemeColor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileViewModel(
    private val database: HealthDatabase,
    private val tokenStorage: TokenStorage,
    private val syncEngine: SyncEngine,
    private val settings: Settings
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
        val isDark = settings.getBoolean("dark_mode", false)
        val accent = AppThemeColor.fromId(settings.getString("theme_accent", AppThemeColor.TEAL.id))

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
            isDarkMode = isDark,
            themeAccent = accent
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

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.OnWeightChanged -> {
                hasUserModifiedWeight = true
                _state.update { it.copy(weightKg = kotlin.math.round(intent.weight * 10) / 10.0) }
            }
            is ProfileIntent.OnTargetWeightChanged -> {
                _state.update { it.copy(targetWeightKg = kotlin.math.round(intent.target * 10) / 10.0) }
            }
            is ProfileIntent.OnHeightChanged -> {
                _state.update { it.copy(heightCm = kotlin.math.round(intent.height * 10) / 10.0) }
            }
            is ProfileIntent.OnAgeChanged -> {
                _state.update { it.copy(age = intent.age.coerceIn(10, 120)) }
            }
            is ProfileIntent.OnGenderChanged -> {
                _state.update { it.copy(gender = intent.gender) }
            }
            is ProfileIntent.OnFitnessGoalChanged -> {
                tokenStorage.setUserFitnessGoal(intent.goal)
                _state.update { it.copy(fitnessGoal = intent.goal) }
                viewModelScope.launch {
                    val userId = tokenStorage.getUserId()
                    if (userId > 0) {
                        val currentGoal = database.observeNutritionGoal(userId).firstOrNull() ?: NutritionGoal(id = "goal_$userId", userId = userId)
                        val calculatedProtein = calculateProteinTarget(_state.value.weightKg, intent.goal)
                        val updated = currentGoal.copy(
                            proteinG = calculatedProtein,
                            fitnessGoal = intent.goal.id,
                            updatedAt = currentTimeMillis(),
                            syncStatus = SyncStatus.PENDING
                        )
                        database.upsertNutritionGoal(updated)
                        launch { syncEngine.sync() }
                    }
                }
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
            is ProfileIntent.OnThemeModeChanged -> {
                settings.putBoolean("dark_mode", intent.isDark)
                _state.update { it.copy(isDarkMode = intent.isDark) }
            }
            is ProfileIntent.OnThemeAccentChanged -> {
                settings.putString("theme_accent", intent.accent.id)
                _state.update { it.copy(themeAccent = intent.accent) }
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
            ProfileIntent.OnLogoutClicked -> {
                viewModelScope.launch {
                    database.clearAll()
                    tokenStorage.clearSession()
                    _effects.send(ProfileEffect.LoggedOut)
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

    private fun saveProfile() {
        val s = _state.value
        val userId = s.userId
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                // Update storage
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
                launch { syncEngine.sync() }

                _state.update { it.copy(isSaving = false, infoMessage = "Profil ve tercihleriniz kaydedildi.") }
                _effects.send(ProfileEffect.ShowToast("Profil başarıyla kaydedildi."))
                _effects.send(ProfileEffect.NavigateBack)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "Kayıt sırasında hata: ${e.message}") }
            }
        }
    }
}
