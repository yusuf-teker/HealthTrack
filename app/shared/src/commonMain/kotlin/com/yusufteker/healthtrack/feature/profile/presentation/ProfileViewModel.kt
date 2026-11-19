package com.yusufteker.healthtrack.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.calculateProteinTarget
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.network.formatActionableApiError
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.GamificationManager
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.sync.SyncState
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.theme.AppThemeColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
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
    private val apiClient: HealthApiClient,
    private val gamificationManager: GamificationManager? = null
) : ViewModel() {

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var hasUserModifiedWeight = false
    private var weightObservationJob: Job? = null
    private var goalObservationJob: Job? = null
    private var streakObservationJob: Job? = null
    private var socialSearchJob: Job? = null

    init {
        observeSession()
        observeSyncState()
        observePremiumStatus()
        viewModelScope.launch {
            LocalizationManager.languageFlow.collect { lang ->
                _state.update { it.copy(language = lang) }
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            tokenStorage.sessionFlow
                .distinctUntilChangedBy { it.hasActiveSession to it.userId }
                .collect { session ->
                    if (session.hasActiveSession && session.userId != 0) {
                        hasUserModifiedWeight = false
                        _state.value = createInitialState()
                        observeLatestWeight(session.userId)
                        observeNutritionGoal(session.userId)
                        observeStreak(session.userId)
                        if (session.isAuthenticated) {
                            refreshAiQuota()
                            refreshSocialData()
                        }
                    } else {
                        weightObservationJob?.cancel()
                        goalObservationJob?.cancel()
                        streakObservationJob?.cancel()
                        hasUserModifiedWeight = false
                        _state.value = createInitialState()
                    }
                }
        }
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
        if (!tokenStorage.getCustomGeminiApiKey().isNullOrBlank() || !tokenStorage.isAuthenticated()) return
        viewModelScope.launch {
            apiClient.getAiQuota().onSuccess { quota ->
                _state.update { it.copy(aiQuota = quota) }
            }
        }
    }

    private fun observeNutritionGoal(userId: Int) {
        if (userId == 0) return
        goalObservationJob?.cancel()
        goalObservationJob = viewModelScope.launch {
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
        val profileImageUrl = tokenStorage.getProfileImageUrl()
        val height = tokenStorage.getUserHeight().takeIf { it > 0.0 } ?: 175.0
        val targetWeight = tokenStorage.getUserTargetWeight().takeIf { it > 0.0 } ?: 70.0
        val weight = tokenStorage.getUserWeight().takeIf { it > 0.0 } ?: 72.0
        val age = tokenStorage.getUserAge().takeIf { it > 0 } ?: 26
        val gender = tokenStorage.getUserGender().takeIf { it.isNotBlank() } ?: "MALE"
        val fitnessGoal = tokenStorage.getUserFitnessGoal()
        val workoutReminder = tokenStorage.getWorkoutReminderFrequency()
        val weightReminder = tokenStorage.getWeightReminderFrequency()
        val weightReminderHour = tokenStorage.getWeightReminderHour()
        val weightReminderMinute = tokenStorage.getWeightReminderMinute()
        val foodReminder = tokenStorage.isFoodReminderEnabled()
        val autoDeviceSync = tokenStorage.isAutoDeviceSyncEnabled()
        val hasConnectedDevice = tokenStorage.hasConnectedDevice()
        val lastDeviceSyncSummary = tokenStorage.getLastDeviceSyncSummary()
        val isDark = settings.getBoolean("dark_mode", true)
        val accent = AppThemeColor.fromId(settings.getString("theme_accent", AppThemeColor.OCEAN.id))
        val customGeminiApiKey = tokenStorage.getCustomGeminiApiKey().orEmpty()

        return ProfileState(
            userId = userId,
            isGuest = tokenStorage.isGuest(),
            name = name,
            username = username,
            profileImageUrl = profileImageUrl,
            weightKg = weight,
            targetWeightKg = targetWeight,
            heightCm = height,
            age = age,
            gender = gender,
            fitnessGoal = fitnessGoal,
            workoutReminderFrequency = workoutReminder,
            weightReminderFrequency = weightReminder,
            weightReminderHour = weightReminderHour,
            weightReminderMinute = weightReminderMinute,
            foodReminderEnabled = foodReminder,
            isAutoDeviceSyncEnabled = autoDeviceSync,
            hasConnectedDevice = hasConnectedDevice,
            lastDeviceSyncSummary = lastDeviceSyncSummary,
            isDarkMode = isDark,
            themeAccent = accent,
            language = LocalizationManager.currentLanguage,
            customGeminiApiKey = customGeminiApiKey,
            mutedWorkoutFriendIds = tokenStorage.getMutedWorkoutFriendIds(userId)
        )
    }

    private fun observeLatestWeight(userId: Int) {
        if (userId == 0) return

        weightObservationJob?.cancel()
        weightObservationJob = viewModelScope.launch {
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
                    val lang = LocalizationManager.currentLanguage
                    _state.update { it.copy(nutritionGoal = updated, fitnessGoal = intent.fitnessGoal) }
                    _effects.send(
                        ProfileEffect.ShowToast(
                            trEn(
                                "${intent.fitnessGoal.localizedTitle(lang)} ve besin hedefleri güncellendi.",
                                "${intent.fitnessGoal.localizedTitle(lang)} and nutrition targets updated.",
                                lang
                            )
                        )
                    )
                    launch { syncEngine.sync() }
                }
            }
            is ProfileIntent.OnWorkoutReminderFrequencyChanged -> {
                tokenStorage.setWorkoutReminderFrequency(intent.frequency)
                _state.update { it.copy(workoutReminderFrequency = intent.frequency) }
                val lang = LocalizationManager.currentLanguage
                val msg = if (intent.frequency == com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency.OFF) {
                    trEn("Spora teşvik bildirimi kapatıldı.", "Workout reminder notifications turned off.", lang)
                } else {
                    trEn(
                        "Spora teşvik sıklığı: ${intent.frequency.localizedTitle(lang)} olarak ayarlandı.",
                        "Workout reminder frequency set to: ${intent.frequency.localizedTitle(lang)}.",
                        lang
                    )
                }
                viewModelScope.launch {
                    _effects.send(ProfileEffect.ShowToast(msg))
                }
            }
            is ProfileIntent.OnWeightReminderFrequencyChanged -> {
                onIntent(ProfileIntent.OnWeightReminderSettingsChanged(intent.frequency, state.value.weightReminderHour, state.value.weightReminderMinute))
            }
            is ProfileIntent.OnWeightReminderSettingsChanged -> {
                tokenStorage.setWeightReminderFrequency(intent.frequency)
                tokenStorage.setWeightReminderHour(intent.hour)
                tokenStorage.setWeightReminderMinute(intent.minute)
                _state.update {
                    it.copy(
                        weightReminderFrequency = intent.frequency,
                        weightReminderHour = intent.hour,
                        weightReminderMinute = intent.minute
                    )
                }
                val lang = LocalizationManager.currentLanguage
                val timeStr = "${intent.hour.toString().padStart(2, '0')}:${intent.minute.toString().padStart(2, '0')}"
                val msg = if (intent.frequency == com.yusufteker.healthtrack.core.model.WeightReminderFrequency.OFF) {
                    trEn("Tartılma hatırlatıcısı kapatıldı.", "Weigh-in reminder turned off.", lang)
                } else {
                    trEn(
                        "Tartılma hatırlatıcısı: ${intent.frequency.localizedTitle(lang)} ($timeStr) olarak ayarlandı.",
                        "Weigh-in reminder set to: ${intent.frequency.localizedTitle(lang)} ($timeStr).",
                        lang
                    )
                }
                viewModelScope.launch {
                    _effects.send(ProfileEffect.ShowToast(msg))
                }
            }
            is ProfileIntent.OnFoodReminderChanged -> {
                tokenStorage.setFoodReminderEnabled(intent.enabled)
                _state.update { it.copy(foodReminderEnabled = intent.enabled) }
                val lang = LocalizationManager.currentLanguage
                val msg = if (intent.enabled) {
                    trEn("13:30 öğün hatırlatıcısı açıldı.", "1:30 PM meal reminder enabled.", lang)
                } else {
                    trEn("Öğün hatırlatıcısı kapatıldı.", "Meal reminder turned off.", lang)
                }
                viewModelScope.launch {
                    _effects.send(ProfileEffect.ShowToast(msg))
                }
            }
            is ProfileIntent.OnAutoDeviceSyncChanged -> {
                tokenStorage.setAutoDeviceSyncEnabled(intent.enabled)
                _state.update { it.copy(isAutoDeviceSyncEnabled = intent.enabled) }
            }
            is ProfileIntent.OnThemeModeChanged -> {
                settings.putBoolean("dark_mode", intent.isDark)
                _state.update { it.copy(isDarkMode = intent.isDark) }
            }
            is ProfileIntent.OnThemeAccentChanged -> {
                settings.putString("theme_accent", intent.accent.id)
                _state.update { it.copy(themeAccent = intent.accent) }
            }
            is ProfileIntent.OnLanguageChanged -> {
                LocalizationManager.setLanguage(intent.language, settings)
                _state.update { it.copy(language = intent.language) }
            }
            is ProfileIntent.OnSaveCustomGeminiApiKey -> {
                val trimmed = intent.apiKey.trim()
                tokenStorage.setCustomGeminiApiKey(trimmed)
                _state.update { it.copy(customGeminiApiKey = trimmed) }
                val lang = LocalizationManager.currentLanguage
                viewModelScope.launch {
                    _effects.send(
                        ProfileEffect.ShowToast(
                            trEn("Özel Gemini API anahtarı kaydedildi.", "Custom Gemini API key saved.", lang)
                        )
                    )
                }
            }
            ProfileIntent.OnClearCustomGeminiApiKey -> {
                tokenStorage.setCustomGeminiApiKey(null)
                _state.update { it.copy(customGeminiApiKey = "") }
                val lang = LocalizationManager.currentLanguage
                viewModelScope.launch {
                    _effects.send(
                        ProfileEffect.ShowToast(
                            trEn(
                                "Özel API anahtarı kaldırıldı, varsayılan AI kullanılacak.",
                                "Custom API key removed; default AI will be used.",
                                lang
                            )
                        )
                    )
                }
            }
            ProfileIntent.OnSyncClicked -> {
                viewModelScope.launch {
                    val lang = LocalizationManager.currentLanguage
                    val success = syncEngine.sync(forceFullPull = true)
                    if (success) {
                        _effects.send(ProfileEffect.ShowToast(trEn("Senkronizasyon tamamlandı.", "Sync completed.", lang)))
                    } else {
                        _effects.send(
                            ProfileEffect.ShowToast(
                                trEn(
                                    "Senkronizasyon tamamlanamadı veya çevrimdışı.",
                                    "Sync could not finish or you are offline.",
                                    lang
                                )
                            )
                        )
                    }
                }
            }
            ProfileIntent.OnDeviceSyncStarted -> {
                _state.update { it.copy(isDeviceSyncing = true, errorMessage = null, infoMessage = null) }
            }
            is ProfileIntent.OnDeviceSnapshotReceived -> {
                viewModelScope.launch {
                    val lang = LocalizationManager.currentLanguage
                    intent.result.onSuccess { snapshot ->
                        val summary = deviceHealthSyncManager.mergeSnapshot(snapshot)
                        val msg = summary.toUserMessage(lang)
                        _state.update {
                            it.copy(
                                isDeviceSyncing = false,
                                hasConnectedDevice = true,
                                lastDeviceSyncSummary = msg,
                                infoMessage = msg
                            )
                        }
                        _effects.send(ProfileEffect.ShowToast(msg))
                    }.onFailure { err ->
                        val msg = err.message ?: trEn("Cihaz senkronizasyonu başarısız oldu.", "Device sync failed.", lang)
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
                            val lang = LocalizationManager.currentLanguage
                            _state.update {
                                it.copy(
                                    isDeletingAccount = false,
                                    errorMessage = formatActionableApiError(
                                        error = error,
                                        operationTr = "Hesap silme",
                                        operationEn = "Account deletion",
                                        language = lang
                                    )
                                )
                            }
                        }
                }
            }
            ProfileIntent.OnSaveProfileClicked -> saveProfile()
            ProfileIntent.OnScreenAppeared -> {
                val currentFitnessGoal = tokenStorage.getUserFitnessGoal()
                _state.update { current ->
                    current.copy(
                        fitnessGoal = currentFitnessGoal,
                        profileImageUrl = tokenStorage.getProfileImageUrl()
                    )
                }
                refreshSocialData()
            }
            is ProfileIntent.OnUploadProfileImage -> uploadProfileImage(intent.imageBytes)
            ProfileIntent.OnRefreshSocial -> refreshSocialData()
            is ProfileIntent.OnFriendSearchQueryChanged -> {
                _state.update { it.copy(friendSearchQuery = intent.query) }
                searchUsersDebounced(intent.query)
            }
            is ProfileIntent.OnToggleFollowUser -> toggleFollowUser(intent.userId)
            is ProfileIntent.OnRemoveFollower -> removeFollower(intent.userId)
            is ProfileIntent.OnToggleFriendWorkoutTracking -> toggleFriendWorkoutTracking(intent.friendUserId)
            is ProfileIntent.OnAcceptFollowRequest -> acceptFollowRequest(intent.requestId)
            is ProfileIntent.OnRejectFollowRequest -> rejectFollowRequest(intent.requestId)
            ProfileIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
            ProfileIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun observeStreak(userId: Int) {
        val manager = gamificationManager ?: return
        streakObservationJob?.cancel()
        streakObservationJob = viewModelScope.launch {
            manager.observeSummary(userId).collect { summary ->
                _state.update { it.copy(streakDays = summary.currentStreakDays) }
            }
        }
    }

    private fun refreshSocialData() {
        if (!tokenStorage.isLoggedIn()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSocial = true) }
            try {
                coroutineScope {
                    val profileDeferred = async { apiClient.getMyProfile() }
                    val followersDeferred = async { apiClient.getFollowers() }
                    val followingDeferred = async { apiClient.getFollowing() }
                    val requestsDeferred = async { apiClient.getFollowRequests() }
                    val mutedDeferred = async { apiClient.getMutedWorkoutFriendIds() }

                    val profileRes = profileDeferred.await()
                    val followersRes = followersDeferred.await()
                    val followingRes = followingDeferred.await()
                    val requestsRes = requestsDeferred.await()
                    val mutedRes = mutedDeferred.await()

                    mutedRes.onSuccess { mutedList ->
                        tokenStorage.syncMutedWorkoutFriendIds(mutedList)
                    }

                    profileRes.onSuccess { p ->
                        if (p.profileImageUrl != tokenStorage.getProfileImageUrl()) {
                            tokenStorage.setProfileImageUrl(p.profileImageUrl)
                        }
                        _state.update {
                            it.copy(
                                name = p.name.ifBlank { it.name },
                                username = p.username.ifBlank { it.username },
                                profileImageUrl = p.profileImageUrl ?: it.profileImageUrl,
                                followersCount = p.followersCount,
                                followingCount = p.followingCount,
                                mutedWorkoutFriendIds = tokenStorage.getMutedWorkoutFriendIds()
                            )
                        }
                    }
                    followersRes.onSuccess { list ->
                        _state.update {
                            it.copy(
                                followers = list,
                                followersCount = maxOf(it.followersCount, list.size)
                            )
                        }
                    }
                    followingRes.onSuccess { list ->
                        _state.update {
                            it.copy(
                                following = list,
                                followingCount = maxOf(it.followingCount, list.size)
                            )
                        }
                    }
                    requestsRes.onSuccess { list ->
                        _state.update { it.copy(followRequests = list) }
                    }
                }
            } finally {
                _state.update { it.copy(isLoadingSocial = false) }
            }
        }
    }

    private fun toggleFriendWorkoutTracking(friendUserId: Int) {
        if (friendUserId <= 0) return
        val currentMuted = _state.value.mutedWorkoutFriendIds
        val isCurrentlyMuted = currentMuted.contains(friendUserId)
        val willBeEnabled = isCurrentlyMuted
        tokenStorage.setFriendWorkoutTrackingEnabled(friendUserId, willBeEnabled)
        val updatedMuted = if (willBeEnabled) currentMuted - friendUserId else currentMuted + friendUserId
        val lang = LocalizationManager.currentLanguage
        val message = if (willBeEnabled) {
            trEn("Arkadaşının spor takibi ve bildirimleri açıldı.", "Workout tracking & alerts enabled for this friend.", lang)
        } else {
            trEn("Arkadaşının spor bildirimleri sessize alındı.", "Workout notifications muted for this friend.", lang)
        }
        _state.update {
            it.copy(mutedWorkoutFriendIds = updatedMuted, infoMessage = message)
        }
        viewModelScope.launch {
            apiClient.toggleFriendWorkoutNotifications(friendUserId)
        }
    }

    private fun uploadProfileImage(imageBytes: ByteArray) {
        if (imageBytes.isEmpty()) return
        viewModelScope.launch {
            val lang = LocalizationManager.currentLanguage
            _state.update { it.copy(isUploadingProfileImage = true, errorMessage = null) }
            apiClient.uploadProfileImage(imageBytes)
                .onSuccess { imageUrl ->
                    tokenStorage.setProfileImageUrl(imageUrl)
                    _state.update {
                        it.copy(
                            isUploadingProfileImage = false,
                            profileImageUrl = imageUrl,
                            infoMessage = trEn("Profil fotoğrafı güncellendi.", "Profile photo updated.", lang)
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isUploadingProfileImage = false,
                            errorMessage = formatActionableApiError(
                                error = err,
                                operationTr = "Fotoğraf yükleme",
                                operationEn = "Photo upload",
                                language = lang
                            )
                        )
                    }
                }
        }
    }

    private fun searchUsersDebounced(query: String) {
        socialSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _state.update { it.copy(searchResults = emptyList(), isSearchingFriends = false) }
            return
        }
        socialSearchJob = viewModelScope.launch {
            delay(320)
            _state.update { it.copy(isSearchingFriends = true) }
            apiClient.searchUsers(trimmed)
                .onSuccess { users ->
                    _state.update { it.copy(searchResults = users, isSearchingFriends = false) }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isSearchingFriends = false,
                            errorMessage = formatActionableApiError(err, "Kullanıcı arama", "User search")
                        )
                    }
                }
        }
    }

    private fun toggleFollowUser(targetUserId: Int) {
        viewModelScope.launch {
            val lang = LocalizationManager.currentLanguage
            _state.update { it.copy(pendingActionUserIds = it.pendingActionUserIds + targetUserId) }
            apiClient.toggleFollowUser(targetUserId)
                .onSuccess {
                    val currentQuery = _state.value.friendSearchQuery.trim()
                    if (currentQuery.length >= 2) {
                        apiClient.searchUsers(currentQuery).onSuccess { users ->
                            _state.update { it.copy(searchResults = users) }
                        }
                    }
                    refreshSocialData()
                    _state.update {
                        it.copy(
                            pendingActionUserIds = it.pendingActionUserIds - targetUserId,
                            infoMessage = trEn("Arkadaşlık durumu güncellendi.", "Friend status updated.", lang)
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            pendingActionUserIds = it.pendingActionUserIds - targetUserId,
                            errorMessage = formatActionableApiError(err, "Takip işlemi", "Follow action", language = lang)
                        )
                    }
                }
        }
    }

    private fun removeFollower(followerUserId: Int) {
        viewModelScope.launch {
            val lang = LocalizationManager.currentLanguage
            _state.update { it.copy(pendingActionUserIds = it.pendingActionUserIds + followerUserId) }
            apiClient.removeFollower(followerUserId)
                .onSuccess {
                    _state.update { current ->
                        val updatedFollowers = current.followers.filterNot { it.id == followerUserId }
                        current.copy(
                            followers = updatedFollowers,
                            followersCount = updatedFollowers.size,
                            pendingActionUserIds = current.pendingActionUserIds - followerUserId,
                            infoMessage = trEn("Takipçi çıkarıldı.", "Follower removed.", lang)
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            pendingActionUserIds = it.pendingActionUserIds - followerUserId,
                            errorMessage = formatActionableApiError(err, "Takipçi çıkarma", "Remove follower", language = lang)
                        )
                    }
                }
        }
    }

    private fun acceptFollowRequest(requestId: Int) {
        viewModelScope.launch {
            val lang = LocalizationManager.currentLanguage
            apiClient.acceptFollowRequest(requestId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            followRequests = it.followRequests.filterNot { r -> r.id == requestId },
                            infoMessage = trEn("Arkadaşlık isteği kabul edildi.", "Friend request accepted.", lang)
                        )
                    }
                    refreshSocialData()
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(errorMessage = formatActionableApiError(err, "İstek kabulü", "Accept request", language = lang))
                    }
                }
        }
    }

    private fun rejectFollowRequest(requestId: Int) {
        viewModelScope.launch {
            val lang = LocalizationManager.currentLanguage
            apiClient.rejectFollowRequest(requestId)
                .onSuccess {
                    _state.update {
                        it.copy(followRequests = it.followRequests.filterNot { r -> r.id == requestId })
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(errorMessage = formatActionableApiError(err, "İstek reddi", "Reject request", language = lang))
                    }
                }
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
        if (userId == 0) return

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
                    bodyFatPercent = todayEntry?.bodyFatPercent,
                    muscleMassKg = todayEntry?.muscleMassKg,
                    musclePercent = todayEntry?.musclePercent,
                    note = todayEntry?.note ?: trEn("Profil Kilo Kaydı", "Profile Weight Entry"),
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
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Quietly retain offline SSOT
        }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            val lang = LocalizationManager.currentLanguage
            _state.update { it.copy(isSaving = true) }
            try {
                persistProfileChanges()
                val savedMsg = trEn("Değişiklikler kaydedildi.", "Changes saved.", lang)
                _state.update { it.copy(isSaving = false, infoMessage = savedMsg) }
                _effects.send(ProfileEffect.ShowToast(savedMsg))
                _effects.send(ProfileEffect.NavigateBack)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = formatActionableApiError(
                            error = e,
                            operationTr = "Profil kaydı",
                            operationEn = "Saving profile",
                            language = lang
                        )
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val s = _state.value
        if (s.userId != 0 && tokenStorage.hasActiveSession() && tokenStorage.getUserId() == s.userId) {
            tokenStorage.setUserWeight(s.weightKg)
            tokenStorage.setUserTargetWeight(s.targetWeightKg)
            tokenStorage.setUserHeight(s.heightCm)
            tokenStorage.setUserAge(s.age)
            tokenStorage.setUserGender(s.gender)
            tokenStorage.setUserFitnessGoal(s.fitnessGoal)
        }
    }
}
