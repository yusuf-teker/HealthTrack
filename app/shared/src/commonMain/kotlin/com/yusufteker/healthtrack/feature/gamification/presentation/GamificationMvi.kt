package com.yusufteker.healthtrack.feature.gamification.presentation

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.utils.getTodayIso

enum class GamificationSection(val titleTr: String, val titleEn: String) {
    OVERVIEW("Rozetler & Seri", "Badges & Streak"),
    LEADERBOARD("Liderlik", "Leaderboard");

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)
}

data class GamificationState(
    val userId: Int = 0,
    val userName: String = "",
    val selectedSection: GamificationSection = GamificationSection.OVERVIEW,
    val summary: GamificationSummary = GamificationSummary(),
    val selectedBadgeCategory: BadgeCategory? = null,

    // Leaderboard
    val period: LeaderboardPeriod = LeaderboardPeriod.WEEKLY,
    val scope: LeaderboardScope = LeaderboardScope.GLOBAL,
    val category: LeaderboardCategory = LeaderboardCategory.DISCIPLINE_XP,
    val selectedDateIso: String = getTodayIso(),
    val leaderboard: LeaderboardResponseDto? = null,
    val isLoadingLeaderboard: Boolean = false,
    val leaderboardError: String? = null,

    // Friends & Follow Requests Sheet
    val isFriendsSheetOpen: Boolean = false,
    val friendSearchQuery: String = "",
    val searchResults: List<FriendUserDto> = emptyList(),
    val isSearchingFriends: Boolean = false,
    val followRequests: List<FollowRequestItemDto> = emptyList(),
    val isLoadingFollowRequests: Boolean = false,
    val pendingActionUserIds: Set<Int> = emptySet(),
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface GamificationIntent {
    data class OnSectionSelected(val section: GamificationSection) : GamificationIntent
    data class OnBadgeCategorySelected(val category: BadgeCategory?) : GamificationIntent
    data class OnPeriodSelected(val period: LeaderboardPeriod) : GamificationIntent
    data class OnScopeSelected(val scope: LeaderboardScope) : GamificationIntent
    data class OnCategorySelected(val category: LeaderboardCategory) : GamificationIntent
    data object OnRefreshLeaderboard : GamificationIntent

    data class OnSetFriendsSheetOpen(val open: Boolean) : GamificationIntent
    data class OnFriendSearchQueryChanged(val query: String) : GamificationIntent
    data class OnToggleFollowUser(val userId: Int) : GamificationIntent
    data class OnAcceptFollowRequest(val requestId: Int) : GamificationIntent
    data class OnRejectFollowRequest(val requestId: Int) : GamificationIntent

    data object OnDismissInfoMessage : GamificationIntent
    data object OnDismissErrorMessage : GamificationIntent
}

