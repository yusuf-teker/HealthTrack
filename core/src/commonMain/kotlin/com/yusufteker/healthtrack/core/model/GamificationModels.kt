package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable

@Serializable
enum class GamificationTier(
    val titleTr: String,
    val titleEn: String,
    val minLevel: Int,
    val colorHex: Long
) {
    ROOKIE("Başlangıç", "Starter", 1, 0xFF38BDF8),
    CONSISTENT("İstikrarlı", "Consistent", 3, 0xFF06D6A0),
    ATHLETE("Atlet", "Athlete", 6, 0xFFF59E0B),
    BIO_HACKER("Bio-Hacker", "Bio-Hacker", 10, 0xFFA855F7),
    ELITE("Elit", "Elite", 15, 0xFFEC4899);

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    companion object {
        fun fromLevel(level: Int): GamificationTier {
            return entries.lastOrNull { level >= it.minLevel } ?: ROOKIE
        }
    }
}

@Serializable
enum class BadgeCategory(val titleTr: String, val titleEn: String) {
    STREAK("Seri", "Streak"),
    NUTRITION("Beslenme", "Nutrition"),
    WORKOUT("Spor", "Workout"),
    HYDRATION("Hidrasyon", "Hydration"),
    WEIGHT("Kilo", "Weight");

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)
}

@Serializable
enum class BadgeId(
    val category: BadgeCategory,
    val titleTr: String,
    val titleEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    val targetCount: Int,
    val xpReward: Int,
    val accentColorHex: Long
) {
    FIRST_LOG(
        category = BadgeCategory.STREAK,
        titleTr = "İlk Adım",
        titleEn = "First Step",
        descriptionTr = "İlk sağlık kaydını (öğün, spor veya tartım) oluştur.",
        descriptionEn = "Create your very first health log (meal, workout, or weight).",
        targetCount = 1,
        xpReward = 50,
        accentColorHex = 0xFF38BDF8
    ),
    STREAK_3(
        category = BadgeCategory.STREAK,
        titleTr = "3 Günlük Kıvılcım",
        titleEn = "3-Day Spark",
        descriptionTr = "3 gün üst üste aktif kalarak ritmi yakala.",
        descriptionEn = "Stay active for 3 consecutive days.",
        targetCount = 3,
        xpReward = 75,
        accentColorHex = 0xFFF59E0B
    ),
    STREAK_7(
        category = BadgeCategory.STREAK,
        titleTr = "Haftalık Ateş",
        titleEn = "7-Day Flame",
        descriptionTr = "7 günlük seri ile haftayı eksiksiz tamamla.",
        descriptionEn = "Complete a full 7-day active streak.",
        targetCount = 7,
        xpReward = 150,
        accentColorHex = 0xFFFB923C
    ),
    STREAK_14(
        category = BadgeCategory.STREAK,
        titleTr = "İki Haftalık İrade",
        titleEn = "14-Day Iron Will",
        descriptionTr = "14 gün boyunca serini koru.",
        descriptionEn = "Protect your streak for 14 days.",
        targetCount = 14,
        xpReward = 300,
        accentColorHex = 0xFFA855F7
    ),
    STREAK_30(
        category = BadgeCategory.STREAK,
        titleTr = "30 Gün Efsanesi",
        titleEn = "30-Day Legend",
        descriptionTr = "30 günlük efsane seriyle alışkanlığı kalıcı kıl.",
        descriptionEn = "Forge a permanent habit with a 30-day streak.",
        targetCount = 30,
        xpReward = 600,
        accentColorHex = 0xFFEC4899
    ),
    PROTEIN_PRO(
        category = BadgeCategory.NUTRITION,
        titleTr = "Protein Ustası",
        titleEn = "Protein Pro",
        descriptionTr = "5 farklı günde günlük protein hedefine ulaş.",
        descriptionEn = "Hit your daily protein target on 5 different days.",
        targetCount = 5,
        xpReward = 120,
        accentColorHex = 0xFF06D6A0
    ),
    CALORIE_SNIPER(
        category = BadgeCategory.NUTRITION,
        titleTr = "Kalori Nişancısı",
        titleEn = "Calorie Sniper",
        descriptionTr = "5 günde kalori hedefinin ±%15 bandında kal.",
        descriptionEn = "Stay within ±15% of your calorie target for 5 days.",
        targetCount = 5,
        xpReward = 140,
        accentColorHex = 0xFF10B981
    ),
    HYDRATION_HERO(
        category = BadgeCategory.HYDRATION,
        titleTr = "Hidrasyon Kahramanı",
        titleEn = "Hydration Hero",
        descriptionTr = "5 farklı günde günlük su hedefini tamamla.",
        descriptionEn = "Reach your daily water goal on 5 different days.",
        targetCount = 5,
        xpReward = 110,
        accentColorHex = 0xFF0EA5E9
    ),
    WORKOUT_10(
        category = BadgeCategory.WORKOUT,
        titleTr = "Demir Disiplin",
        titleEn = "Iron Discipline",
        descriptionTr = "Toplam 10 antrenman seansı kaydet.",
        descriptionEn = "Log 10 workout sessions in total.",
        targetCount = 10,
        xpReward = 180,
        accentColorHex = 0xFFF97316
    ),
    BURN_1000(
        category = BadgeCategory.WORKOUT,
        titleTr = "1.000 kcal Yakıcı",
        titleEn = "1K Burner",
        descriptionTr = "Antrenmanlarla toplam 1.000 kcal yak.",
        descriptionEn = "Burn 1,000 kcal in total through workouts.",
        targetCount = 1000,
        xpReward = 100,
        accentColorHex = 0xFFEF4444
    ),
    BURN_5000(
        category = BadgeCategory.WORKOUT,
        titleTr = "5.000 kcal Motoru",
        titleEn = "5K Engine",
        descriptionTr = "Antrenmanlarla toplam 5.000 kcal yak.",
        descriptionEn = "Burn 5,000 kcal in total through workouts.",
        targetCount = 5000,
        xpReward = 350,
        accentColorHex = 0xFFE11D48
    ),
    SCALE_TRACKER(
        category = BadgeCategory.WEIGHT,
        titleTr = "Düzenli Takip",
        titleEn = "Steady Tracker",
        descriptionTr = "5 farklı tartım kaydı girerek trendini takip et.",
        descriptionEn = "Log 5 weight entries to track your trend.",
        targetCount = 5,
        xpReward = 90,
        accentColorHex = 0xFF8B5CF6
    );

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedDescription(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(descriptionTr, descriptionEn, language)
}

@Serializable
data class GamificationAchievement(
    val badgeId: BadgeId,
    val progressCurrent: Int,
    val progressTarget: Int,
    val isUnlocked: Boolean
) {
    val progressFraction: Float
        get() = if (progressTarget > 0) {
            (progressCurrent.toFloat() / progressTarget.toFloat()).coerceIn(0f, 1f)
        } else if (isUnlocked) 1f else 0f
}

@Serializable
enum class QuestPeriod {
    DAILY,
    WEEKLY
}

@Serializable
data class GamificationQuest(
    val id: String,
    val period: QuestPeriod,
    val titleTr: String,
    val titleEn: String,
    val currentCount: Int,
    val targetCount: Int,
    val xpReward: Int,
    val isCompleted: Boolean
) {
    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    val progressFraction: Float
        get() = if (targetCount > 0) {
            (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
        } else if (isCompleted) 1f else 0f
}

@Serializable
data class GamificationSummary(
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentLevelXp: Int = 0,
    val nextLevelXp: Int = 150,
    val levelProgress: Float = 0f,
    val tier: GamificationTier = GamificationTier.ROOKIE,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val isActiveToday: Boolean = false,
    val isRestDayProtected: Boolean = false,
    val todayScore: Int = 0,
    val todayEarnedXp: Int = 0,
    val unlockedBadgesCount: Int = 0,
    val totalBadgesCount: Int = BadgeId.entries.size,
    val achievements: List<GamificationAchievement> = emptyList(),
    val dailyQuests: List<GamificationQuest> = emptyList(),
    val weeklyQuests: List<GamificationQuest> = emptyList()
)

@Serializable
enum class CelebrationType {
    BADGE_UNLOCKED,
    LEVEL_UP,
    STREAK_MILESTONE,
    DAILY_GOAL_COMPLETED,
    WORKOUT_GOAL_REACHED,
    WATER_GOAL_REACHED
}

@Serializable
data class MotivationalToastEvent(
    val id: String,
    val xpEarned: Int,
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    val progressFraction: Float? = null,
    val accentColorHex: Long = 0xFF06D6A0
) {
    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedSubtitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(subtitleTr, subtitleEn, language)
}

@Serializable
data class CelebrationEvent(
    val id: String,
    val type: CelebrationType,
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    val xpBonus: Int = 0,
    val badgeId: BadgeId? = null,
    val streakDays: Int? = null,
    val newLevel: Int? = null
) {
    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedSubtitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(subtitleTr, subtitleEn, language)
}

