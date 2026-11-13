package com.yusufteker.healthtrack.feature.gamification.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.theme.HealthTrackTheme

private val sampleSummary = GamificationSummary(
    totalXp = 1240,
    level = 5,
    currentLevelXp = 190,
    nextLevelXp = 350,
    levelProgress = 0.54f,
    tier = GamificationTier.CONSISTENT,
    currentStreakDays = 9,
    longestStreakDays = 14,
    isActiveToday = true,
    isRestDayProtected = false,
    todayScore = 84,
    todayEarnedXp = 65,
    unlockedBadgesCount = 5,
    totalBadgesCount = BadgeId.entries.size,
    achievements = listOf(
        GamificationAchievement(BadgeId.FIRST_LOG, 1, 1, true),
        GamificationAchievement(BadgeId.STREAK_3, 3, 3, true),
        GamificationAchievement(BadgeId.STREAK_7, 7, 7, true),
        GamificationAchievement(BadgeId.STREAK_14, 14, 14, true),
        GamificationAchievement(BadgeId.STREAK_30, 14, 30, false),
        GamificationAchievement(BadgeId.PROTEIN_PRO, 5, 5, true),
        GamificationAchievement(BadgeId.CALORIE_SNIPER, 3, 5, false),
        GamificationAchievement(BadgeId.HYDRATION_HERO, 4, 5, false),
        GamificationAchievement(BadgeId.WORKOUT_10, 7, 10, false),
        GamificationAchievement(BadgeId.BURN_1000, 1000, 1000, true),
        GamificationAchievement(BadgeId.BURN_5000, 2850, 5000, false),
        GamificationAchievement(BadgeId.SCALE_TRACKER, 3, 5, false)
    ),
    dailyQuests = listOf(
        GamificationQuest("daily_food", QuestPeriod.DAILY, "Bugün öğün kaydı gir", "Log a meal today", 1, 1, 25, true),
        GamificationQuest("daily_water", QuestPeriod.DAILY, "Su hedefine ulaş (2500 ml)", "Reach water goal (2500 ml)", 2000, 2500, 25, false),
        GamificationQuest("daily_workout", QuestPeriod.DAILY, "Bugün antrenman / hareket kaydet", "Log a workout / activity today", 1, 1, 30, true)
    ),
    weeklyQuests = listOf(
        GamificationQuest("weekly_workouts", QuestPeriod.WEEKLY, "Bu hafta 3 antrenman tamamla", "Complete 3 workouts this week", 2, 3, 100, false),
        GamificationQuest("weekly_foods", QuestPeriod.WEEKLY, "Bu hafta 5 gün öğün kaydet", "Log meals on 5 days this week", 5, 5, 90, true),
        GamificationQuest("weekly_water", QuestPeriod.WEEKLY, "Bu hafta 4 gün su hedefini tut", "Hit water goal on 4 days this week", 3, 4, 80, false)
    )
)

private val sampleLeaderboard = LeaderboardResponseDto(
    period = "WEEKLY",
    scope = "GLOBAL",
    category = "CALORIES_BURNED",
    startDate = "2026-11-09",
    endDate = "2026-11-15",
    totalParticipants = 28,
    friendsCount = 4,
    entries = listOf(
        LeaderboardEntryDto(
            rank = 1,
            userId = 10,
            name = "Mert Kaya",
            username = "mertk",
            score = 3420,
            primaryValueLabel = "3.420 kcal",
            secondaryLabel = "290 dk • 6 gün",
            streakDays = 18,
            caloriesBurned = 3420
        ),
        LeaderboardEntryDto(
            rank = 2,
            userId = 1,
            name = "Yusuf Teker",
            username = "yusufteker",
            isCurrentUser = true,
            score = 2890,
            primaryValueLabel = "2.890 kcal",
            secondaryLabel = "230 dk • 5 gün",
            streakDays = 9,
            caloriesBurned = 2890
        ),
        LeaderboardEntryDto(
            rank = 3,
            userId = 12,
            name = "Elif Yılmaz",
            username = "elify",
            score = 2410,
            primaryValueLabel = "2.410 kcal",
            secondaryLabel = "195 dk • 5 gün",
            streakDays = 11,
            caloriesBurned = 2410
        ),
        LeaderboardEntryDto(
            rank = 4,
            userId = 15,
            name = "Can Demir",
            username = "candemir",
            score = 1980,
            primaryValueLabel = "1.980 kcal",
            secondaryLabel = "150 dk • 4 gün",
            streakDays = 4,
            caloriesBurned = 1980
        )
    ),
    myEntry = LeaderboardEntryDto(
        rank = 2,
        userId = 1,
        name = "Yusuf Teker",
        username = "yusufteker",
        isCurrentUser = true,
        score = 2890,
        primaryValueLabel = "2.890 kcal",
        secondaryLabel = "230 dk • 5 gün",
        streakDays = 9,
        caloriesBurned = 2890
    )
)

private val overviewPopulatedState = GamificationState(
    userId = 1,
    userName = "Yusuf Teker",
    selectedSection = GamificationSection.OVERVIEW,
    summary = sampleSummary
)

private val leaderboardPopulatedState = GamificationState(
    userId = 1,
    userName = "Yusuf Teker",
    selectedSection = GamificationSection.LEADERBOARD,
    summary = sampleSummary,
    period = LeaderboardPeriod.WEEKLY,
    scope = LeaderboardScope.GLOBAL,
    category = LeaderboardCategory.CALORIES_BURNED,
    leaderboard = sampleLeaderboard
)

private val friendsEmptyState = GamificationState(
    userId = 1,
    userName = "Yusuf Teker",
    selectedSection = GamificationSection.LEADERBOARD,
    summary = sampleSummary,
    period = LeaderboardPeriod.DAILY,
    scope = LeaderboardScope.FRIENDS,
    category = LeaderboardCategory.HEALTHY_EATING,
    leaderboard = LeaderboardResponseDto(
        period = "DAILY",
        scope = "FRIENDS",
        category = "HEALTHY_EATING",
        startDate = "2026-11-13",
        endDate = "2026-11-13",
        totalParticipants = 1,
        friendsCount = 0,
        entries = listOf(
            LeaderboardEntryDto(
                rank = 1,
                userId = 1,
                name = "Yusuf Teker",
                username = "yusufteker",
                isCurrentUser = true,
                score = 88,
                primaryValueLabel = "88 / 100",
                secondaryLabel = "1 gün • ort %88",
                streakDays = 9
            )
        ),
        myEntry = LeaderboardEntryDto(
            rank = 1,
            userId = 1,
            name = "Yusuf Teker",
            username = "yusufteker",
            isCurrentUser = true,
            score = 88,
            primaryValueLabel = "88 / 100",
            secondaryLabel = "1 gün • ort %88",
            streakDays = 9
        )
    )
)

@Preview
@Composable
fun GamificationOverviewDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        GamificationScreen(state = overviewPopulatedState, onIntent = {}, onBack = {})
    }
}

@Preview
@Composable
fun GamificationOverviewLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        GamificationScreen(state = overviewPopulatedState, onIntent = {}, onBack = {})
    }
}

@Preview
@Composable
fun GamificationLeaderboardDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        GamificationScreen(state = leaderboardPopulatedState, onIntent = {}, onBack = {})
    }
}

@Preview
@Composable
fun GamificationLeaderboardLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        GamificationScreen(state = leaderboardPopulatedState, onIntent = {}, onBack = {})
    }
}

@Preview
@Composable
fun GamificationFriendsEmptyDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        GamificationScreen(state = friendsEmptyState, onIntent = {}, onBack = {})
    }
}

