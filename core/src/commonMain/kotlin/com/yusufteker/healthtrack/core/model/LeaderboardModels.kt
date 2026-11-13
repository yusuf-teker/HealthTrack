package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable

@Serializable
enum class LeaderboardPeriod(val id: String, val titleTr: String, val titleEn: String) {
    DAILY("DAILY", "Bugün", "Today"),
    WEEKLY("WEEKLY", "Bu Hafta", "This Week");

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)
}

@Serializable
enum class LeaderboardScope(val id: String, val titleTr: String, val titleEn: String) {
    GLOBAL("GLOBAL", "Genel", "Global"),
    FRIENDS("FRIENDS", "Arkadaşlar", "Friends");

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)
}

@Serializable
enum class LeaderboardCategory(val id: String, val titleTr: String, val titleEn: String) {
    DISCIPLINE_XP("DISCIPLINE_XP", "Genel Disiplin", "Discipline"),
    CALORIES_BURNED("CALORIES_BURNED", "Kalori Yakan", "Calories Burned"),
    HEALTHY_EATING("HEALTHY_EATING", "Sağlıklı Beslenen", "Healthy Eating"),
    WORKOUT_MINUTES("WORKOUT_MINUTES", "Spor Süresi", "Workout Time");

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)
}

@Serializable
data class LeaderboardEntryDto(
    val rank: Int,
    val userId: Int,
    val name: String,
    val username: String,
    val avatarId: String = "avatar_1",
    val profileImageUrl: String? = null,
    val isPremium: Boolean = false,
    val isCurrentUser: Boolean = false,
    val isFollowedByMe: Boolean = false,
    val score: Int = 0,
    val primaryValueLabel: String = "",
    val secondaryLabel: String = "",
    val streakDays: Int = 0,
    val caloriesBurned: Int = 0,
    val workoutMinutes: Int = 0,
    val healthyEatingScore: Int = 0,
    val activeDays: Int = 0
)

@Serializable
data class LeaderboardResponseDto(
    val period: String,
    val scope: String,
    val category: String,
    val startDate: String,
    val endDate: String,
    val totalParticipants: Int,
    val friendsCount: Int,
    val entries: List<LeaderboardEntryDto> = emptyList(),
    val myEntry: LeaderboardEntryDto? = null
)

@Serializable
data class FriendUserDto(
    val id: Int,
    val name: String,
    val username: String = "",
    val avatarId: String = "avatar_1",
    val profileImageUrl: String? = null,
    val isFollowedByMe: Boolean = false,
    val followRequestStatus: String? = null,
    val isPremium: Boolean = false
)

@Serializable
data class SearchUsersResponseDto(
    val users: List<FriendUserDto> = emptyList()
)

@Serializable
data class FollowRequestItemDto(
    val id: Int,
    val requesterId: Int,
    val requesterName: String,
    val requesterUsername: String = "",
    val requesterAvatarId: String = "avatar_1",
    val status: String = "PENDING",
    val requesterProfileImageUrl: String? = null
)

