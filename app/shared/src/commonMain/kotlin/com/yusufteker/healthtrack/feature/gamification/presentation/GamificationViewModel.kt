package com.yusufteker.healthtrack.feature.gamification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.LeaderboardCategory
import com.yusufteker.healthtrack.core.model.LeaderboardEntryDto
import com.yusufteker.healthtrack.core.model.LeaderboardPeriod
import com.yusufteker.healthtrack.core.model.LeaderboardResponseDto
import com.yusufteker.healthtrack.core.model.LeaderboardScope
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.network.formatActionableApiError
import com.yusufteker.healthtrack.core.sync.GamificationManager
import com.yusufteker.healthtrack.core.utils.getTodayIso
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GamificationViewModel(
    private val gamificationManager: GamificationManager,
    private val apiClient: HealthApiClient,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _state = MutableStateFlow(GamificationState())
    val state: StateFlow<GamificationState> = _state.asStateFlow()

    private var summaryJob: Job? = null
    private var leaderboardJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow.collectLatest { session ->
                _state.update {
                    it.copy(
                        userId = session.userId,
                        userName = if (session.name.isNotBlank()) session.name else session.username
                    )
                }
                summaryJob?.cancel()
                if (session.userId > 0) {
                    summaryJob = viewModelScope.launch {
                        gamificationManager.observeSummary(session.userId).collect { summary ->
                            _state.update { it.copy(summary = summary) }
                        }
                    }
                    loadFollowRequests()
                    if (_state.value.selectedSection == GamificationSection.LEADERBOARD) {
                        loadLeaderboard()
                    }
                }
            }
        }
    }

    fun setInitialSection(section: GamificationSection) {
        if (_state.value.selectedSection != section) {
            _state.update { it.copy(selectedSection = section) }
        }
        if (section == GamificationSection.LEADERBOARD && _state.value.leaderboard == null) {
            loadLeaderboard()
        }
    }

    fun onIntent(intent: GamificationIntent) {
        when (intent) {
            is GamificationIntent.OnSectionSelected -> {
                _state.update { it.copy(selectedSection = intent.section) }
                if (intent.section == GamificationSection.LEADERBOARD && _state.value.leaderboard == null) {
                    loadLeaderboard()
                }
            }
            is GamificationIntent.OnBadgeCategorySelected -> {
                _state.update { it.copy(selectedBadgeCategory = intent.category) }
            }
            is GamificationIntent.OnPeriodSelected -> {
                _state.update { it.copy(period = intent.period) }
                loadLeaderboard()
            }
            is GamificationIntent.OnScopeSelected -> {
                _state.update { it.copy(scope = intent.scope) }
                loadLeaderboard()
            }
            is GamificationIntent.OnCategorySelected -> {
                _state.update { it.copy(category = intent.category) }
                loadLeaderboard()
            }
            GamificationIntent.OnRefreshLeaderboard -> {
                loadLeaderboard()
                loadSocialData()
            }
            is GamificationIntent.OnSetFriendsSheetOpen -> {
                val targetTab = if (intent.open) {
                    if (_state.value.followRequests.isNotEmpty()) 2 else 0
                } else null
                _state.update {
                    it.copy(
                        isFriendsSheetOpen = intent.open,
                        followListTabToOpen = targetTab
                    )
                }
                if (intent.open) {
                    loadSocialData()
                }
            }
            is GamificationIntent.OnSetFollowListTab -> {
                _state.update {
                    it.copy(
                        followListTabToOpen = intent.tab,
                        isFriendsSheetOpen = intent.tab != null
                    )
                }
                if (intent.tab != null) {
                    loadSocialData()
                }
            }
            is GamificationIntent.OnSetSearchSheetOpen -> {
                _state.update { it.copy(isSearchSheetOpen = intent.open) }
            }
            is GamificationIntent.OnFriendSearchQueryChanged -> {
                _state.update { it.copy(friendSearchQuery = intent.query) }
                searchUsersDebounced(intent.query)
            }
            is GamificationIntent.OnToggleFollowUser -> toggleFollowUser(intent.userId)
            is GamificationIntent.OnRemoveFollower -> removeFollower(intent.userId)
            is GamificationIntent.OnAcceptFollowRequest -> acceptFollowRequest(intent.requestId)
            is GamificationIntent.OnRejectFollowRequest -> rejectFollowRequest(intent.requestId)
            GamificationIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
            GamificationIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadLeaderboard(
        period: LeaderboardPeriod = _state.value.period,
        scope: LeaderboardScope = _state.value.scope,
        category: LeaderboardCategory = _state.value.category
    ) {
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingLeaderboard = true, leaderboardError = null) }
            val todayIso = getTodayIso()
            val (result, friendsResult) = coroutineScope {
                val primaryDeferred = async {
                    apiClient.getLeaderboard(
                        period = period,
                        scope = scope,
                        category = category,
                        dateIso = todayIso
                    )
                }
                val friendsDeferred = if (scope == LeaderboardScope.GLOBAL) {
                    async {
                        apiClient.getLeaderboard(
                            period = period,
                            scope = LeaderboardScope.FRIENDS,
                            category = category,
                            dateIso = todayIso
                        )
                    }
                } else {
                    null
                }
                primaryDeferred.await() to friendsDeferred?.await()
            }
            result.onSuccess { dto ->
                val effectiveDto = if (scope == LeaderboardScope.GLOBAL) {
                    mergeGlobalWithFriends(dto, friendsResult?.getOrNull())
                } else {
                    dto
                }
                _state.update {
                    it.copy(
                        leaderboard = effectiveDto,
                        isLoadingLeaderboard = false,
                        leaderboardError = null
                    )
                }
            }.onFailure { err ->
                val friendly = formatActionableApiError(err, "Lider tablosu", "Leaderboard")
                _state.update {
                    it.copy(
                        isLoadingLeaderboard = false,
                        leaderboardError = friendly
                    )
                }
            }
        }
    }

    private fun mergeGlobalWithFriends(
        global: LeaderboardResponseDto,
        friends: LeaderboardResponseDto?
    ): LeaderboardResponseDto {
        if (friends == null) return global
        val existingIds = global.entries.mapTo(mutableSetOf()) { it.userId }
        val missingFriends = friends.entries.filter { it.userId !in existingIds }
        val effectiveFriendsCount = maxOf(global.friendsCount, friends.friendsCount)
        if (missingFriends.isEmpty()) {
            return if (effectiveFriendsCount == global.friendsCount) {
                global
            } else {
                global.copy(friendsCount = effectiveFriendsCount)
            }
        }
        val combined = (global.entries + missingFriends)
            .sortedWith(
                compareByDescending<LeaderboardEntryDto> { it.score }
                    .thenByDescending { it.streakDays }
                    .thenByDescending { it.activeDays }
                    .thenByDescending { it.isCurrentUser }
                    .thenBy { it.name.lowercase() }
            )
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }

        val updatedMyEntry = combined.firstOrNull { it.isCurrentUser } ?: global.myEntry
        val updatedTotal = maxOf(global.totalParticipants + missingFriends.size, combined.size)
        return global.copy(
            totalParticipants = updatedTotal,
            friendsCount = effectiveFriendsCount,
            entries = combined,
            myEntry = updatedMyEntry
        )
    }

    private fun loadFollowRequests() {
        loadSocialData()
    }

    private fun loadSocialData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingFollowRequests = true, isLoadingSocialLists = true) }
            coroutineScope {
                val requestsDeferred = async { apiClient.getFollowRequests() }
                val followersDeferred = async { apiClient.getFollowers() }
                val followingDeferred = async { apiClient.getFollowing() }

                val requests = requestsDeferred.await().getOrNull()
                val followers = followersDeferred.await().getOrNull()
                val following = followingDeferred.await().getOrNull()

                _state.update { current ->
                    current.copy(
                        followRequests = requests ?: current.followRequests,
                        followers = followers ?: current.followers,
                        following = following ?: current.following,
                        isLoadingFollowRequests = false,
                        isLoadingSocialLists = false
                    )
                }
            }
        }
    }

    private fun searchUsersDebounced(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _state.update { it.copy(searchResults = emptyList(), isSearchingFriends = false) }
            return
        }
        searchJob = viewModelScope.launch {
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
            _state.update { it.copy(pendingActionUserIds = it.pendingActionUserIds + targetUserId) }
            apiClient.toggleFollowUser(targetUserId)
                .onSuccess {
                    // Refresh search results, social lists & friends leaderboard
                    val currentQuery = _state.value.friendSearchQuery.trim()
                    if (currentQuery.length >= 2) {
                        apiClient.searchUsers(currentQuery).onSuccess { users ->
                            _state.update { it.copy(searchResults = users) }
                        }
                    }
                    loadSocialData()
                    if (_state.value.scope == LeaderboardScope.FRIENDS) {
                        loadLeaderboard()
                    }
                    _state.update {
                        it.copy(
                            pendingActionUserIds = it.pendingActionUserIds - targetUserId,
                            infoMessage = trEn("Arkadaşlık durumu güncellendi.", "Friend status updated.")
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            pendingActionUserIds = it.pendingActionUserIds - targetUserId,
                            errorMessage = formatActionableApiError(err, "Takip işlemi", "Follow action")
                        )
                    }
                }
        }
    }

    private fun removeFollower(followerUserId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(pendingActionUserIds = it.pendingActionUserIds + followerUserId) }
            apiClient.removeFollower(followerUserId)
                .onSuccess {
                    _state.update { current ->
                        current.copy(
                            followers = current.followers.filterNot { it.id == followerUserId },
                            pendingActionUserIds = current.pendingActionUserIds - followerUserId,
                            infoMessage = trEn("Takipçi çıkarıldı.", "Follower removed.")
                        )
                    }
                    loadSocialData()
                    if (_state.value.scope == LeaderboardScope.FRIENDS) {
                        loadLeaderboard()
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            pendingActionUserIds = it.pendingActionUserIds - followerUserId,
                            errorMessage = formatActionableApiError(err, "Takipçi çıkarma", "Remove follower")
                        )
                    }
                }
        }
    }

    private fun acceptFollowRequest(requestId: Int) {
        viewModelScope.launch {
            apiClient.acceptFollowRequest(requestId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            followRequests = it.followRequests.filterNot { r -> r.id == requestId },
                            infoMessage = trEn("Arkadaşlık isteği kabul edildi.", "Friend request accepted.")
                        )
                    }
                    loadSocialData()
                    if (_state.value.selectedSection == GamificationSection.LEADERBOARD) {
                        loadLeaderboard()
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(errorMessage = formatActionableApiError(err, "İstek kabulü", "Accept request"))
                    }
                }
        }
    }

    private fun rejectFollowRequest(requestId: Int) {
        viewModelScope.launch {
            apiClient.rejectFollowRequest(requestId)
                .onSuccess {
                    _state.update {
                        it.copy(followRequests = it.followRequests.filterNot { r -> r.id == requestId })
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(errorMessage = formatActionableApiError(err, "İstek reddi", "Reject request"))
                    }
                }
        }
    }
}

