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
data class BadgeStage(
    val level: Int,
    val targetCount: Int,
    val xpReward: Int
)

enum class BadgeRank(
    val accentHex: Long,
    val labelTr: String,
    val labelEn: String
) {
    LOCKED(0xFF64748B, "Kilitli", "Locked"),
    BRONZE(0xFFCD7F32, "Bronz", "Bronze"),
    SILVER(0xFFCBD5E1, "Gümüş", "Silver"),
    GOLD(0xFFF59E0B, "Altın", "Gold"),
    PLATINUM(0xFF38BDF8, "Platin", "Platinum"),
    DIAMOND(0xFFEC4899, "Elmas", "Diamond");

    fun localizedLabel(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(labelTr, labelEn, language)
}

@Serializable
enum class BadgeId(
    val category: BadgeCategory,
    val titleTr: String,
    val titleEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    val unitTr: String,
    val unitEn: String,
    val accentColorHex: Long,
    val stages: List<BadgeStage>
) {
    FIRST_LOG(
        category = BadgeCategory.STREAK,
        titleTr = "İlk Adım",
        titleEn = "First Step",
        descriptionTr = "İlk sağlık kaydını (öğün, spor veya tartım) oluştur.",
        descriptionEn = "Create your very first health log (meal, workout, or weight).",
        unitTr = "kayıt",
        unitEn = "logs",
        accentColorHex = 0xFF38BDF8,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 1, xpReward = 50)
        )
    ),
    STREAK_MASTER(
        category = BadgeCategory.STREAK,
        titleTr = "Seri Ateşi",
        titleEn = "Streak Flame",
        descriptionTr = "Üst üste aktif gün serisi yakalayarak alışkanlığını güçlendir.",
        descriptionEn = "Build unstoppable momentum with consecutive active days.",
        unitTr = "gün",
        unitEn = "days",
        accentColorHex = 0xFFFB923C,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 3, xpReward = 75),
            BadgeStage(level = 2, targetCount = 7, xpReward = 150),
            BadgeStage(level = 3, targetCount = 14, xpReward = 300),
            BadgeStage(level = 4, targetCount = 30, xpReward = 600),
            BadgeStage(level = 5, targetCount = 60, xpReward = 1100)
        )
    ),
    BIO_SCORE_ELITE(
        category = BadgeCategory.STREAK,
        titleTr = "Bio-Skor Ustası",
        titleEn = "Bio-Score Elite",
        descriptionTr = "Beslenme, su ve sporu dengeleyerek günlük 85+ Bio-Skor yakala.",
        descriptionEn = "Reach 85+ daily Bio-Score by balancing nutrition, water, and activity.",
        unitTr = "gün",
        unitEn = "days",
        accentColorHex = 0xFFA855F7,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 3, xpReward = 100),
            BadgeStage(level = 2, targetCount = 10, xpReward = 220),
            BadgeStage(level = 3, targetCount = 25, xpReward = 450),
            BadgeStage(level = 4, targetCount = 50, xpReward = 850)
        )
    ),
    WEEKEND_WARRIOR(
        category = BadgeCategory.STREAK,
        titleTr = "Hafta Sonu Savaşçısı",
        titleEn = "Weekend Warrior",
        descriptionTr = "Cumartesi ve Pazar günlerini birlikte aktif tamamla.",
        descriptionEn = "Stay active across both Saturday and Sunday on weekends.",
        unitTr = "hafta sonu",
        unitEn = "weekends",
        accentColorHex = 0xFFF59E0B,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 2, xpReward = 90),
            BadgeStage(level = 2, targetCount = 4, xpReward = 180),
            BadgeStage(level = 3, targetCount = 8, xpReward = 360),
            BadgeStage(level = 4, targetCount = 16, xpReward = 720)
        )
    ),
    PROTEIN_PRO(
        category = BadgeCategory.NUTRITION,
        titleTr = "Protein Ustası",
        titleEn = "Protein Pro",
        descriptionTr = "Günlük protein hedefini tutturduğun gün sayısını artır.",
        descriptionEn = "Hit your daily protein target across multiple days.",
        unitTr = "gün",
        unitEn = "days",
        accentColorHex = 0xFF06D6A0,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 5, xpReward = 120),
            BadgeStage(level = 2, targetCount = 15, xpReward = 240),
            BadgeStage(level = 3, targetCount = 30, xpReward = 480),
            BadgeStage(level = 4, targetCount = 60, xpReward = 900)
        )
    ),
    CALORIE_SNIPER(
        category = BadgeCategory.NUTRITION,
        titleTr = "Kalori Nişancısı",
        titleEn = "Calorie Sniper",
        descriptionTr = "Günlük kalori hedefinin ±%15 bandında kalarak isabet sağla.",
        descriptionEn = "Stay within ±15% of your daily calorie target.",
        unitTr = "gün",
        unitEn = "days",
        accentColorHex = 0xFF10B981,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 5, xpReward = 140),
            BadgeStage(level = 2, targetCount = 15, xpReward = 280),
            BadgeStage(level = 3, targetCount = 30, xpReward = 520),
            BadgeStage(level = 4, targetCount = 60, xpReward = 950)
        )
    ),
    FIBER_CHAMPION(
        category = BadgeCategory.NUTRITION,
        titleTr = "Lif Şampiyonu",
        titleEn = "Fiber Champion",
        descriptionTr = "Günlük lif hedefine ulaştığın dengeli günleri biriktir.",
        descriptionEn = "Reach your daily dietary fiber goal consistently.",
        unitTr = "gün",
        unitEn = "days",
        accentColorHex = 0xFF34D399,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 5, xpReward = 110),
            BadgeStage(level = 2, targetCount = 15, xpReward = 220),
            BadgeStage(level = 3, targetCount = 30, xpReward = 440),
            BadgeStage(level = 4, targetCount = 60, xpReward = 800)
        )
    ),
    HYDRATION_HERO(
        category = BadgeCategory.HYDRATION,
        titleTr = "Hidrasyon Kahramanı",
        titleEn = "Hydration Hero",
        descriptionTr = "Günlük su hedefine tam ulaştığın günleri çoğalt.",
        descriptionEn = "Complete your daily hydration goal across days.",
        unitTr = "gün",
        unitEn = "days",
        accentColorHex = 0xFF0EA5E9,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 5, xpReward = 110),
            BadgeStage(level = 2, targetCount = 15, xpReward = 230),
            BadgeStage(level = 3, targetCount = 30, xpReward = 460),
            BadgeStage(level = 4, targetCount = 60, xpReward = 850)
        )
    ),
    WORKOUT_DISCIPLINE(
        category = BadgeCategory.WORKOUT,
        titleTr = "Demir Disiplin",
        titleEn = "Iron Discipline",
        descriptionTr = "Toplam aktif antrenman seansı sayını yükselt.",
        descriptionEn = "Grow your total count of logged active workout sessions.",
        unitTr = "seans",
        unitEn = "workouts",
        accentColorHex = 0xFFF97316,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 5, xpReward = 100),
            BadgeStage(level = 2, targetCount = 15, xpReward = 220),
            BadgeStage(level = 3, targetCount = 30, xpReward = 450),
            BadgeStage(level = 4, targetCount = 60, xpReward = 900)
        )
    ),
    CALORIE_ENGINE(
        category = BadgeCategory.WORKOUT,
        titleTr = "Kalori Motoru",
        titleEn = "Calorie Engine",
        descriptionTr = "Antrenmanlarla toplam yakılan kaloriyi katla (1K → 50K kcal).",
        descriptionEn = "Multiply total calories burned via workouts (1K → 50K kcal).",
        unitTr = "kcal",
        unitEn = "kcal",
        accentColorHex = 0xFFEF4444,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 1000, xpReward = 100),
            BadgeStage(level = 2, targetCount = 5000, xpReward = 250),
            BadgeStage(level = 3, targetCount = 10000, xpReward = 500),
            BadgeStage(level = 4, targetCount = 25000, xpReward = 1000),
            BadgeStage(level = 5, targetCount = 50000, xpReward = 1800)
        )
    ),
    ENDURANCE_MINUTES(
        category = BadgeCategory.WORKOUT,
        titleTr = "Dayanıklılık Zamanı",
        titleEn = "Endurance Time",
        descriptionTr = "Antrenmanlarda harcadığın toplam aktif dakikayı büyüt.",
        descriptionEn = "Accumulate active training minutes across your workouts.",
        unitTr = "dk",
        unitEn = "min",
        accentColorHex = 0xFFFB7185,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 150, xpReward = 100),
            BadgeStage(level = 2, targetCount = 500, xpReward = 240),
            BadgeStage(level = 3, targetCount = 1500, xpReward = 520),
            BadgeStage(level = 4, targetCount = 3000, xpReward = 1000)
        )
    ),
    SCALE_TRACKER(
        category = BadgeCategory.WEIGHT,
        titleTr = "Düzenli Takip",
        titleEn = "Steady Tracker",
        descriptionTr = "Düzenli tartım kayıtlarıyla vücut trendini yakından izle.",
        descriptionEn = "Log regular weight check-ins to track your long-term trend.",
        unitTr = "tartım",
        unitEn = "weigh-ins",
        accentColorHex = 0xFF8B5CF6,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 5, xpReward = 90),
            BadgeStage(level = 2, targetCount = 15, xpReward = 180),
            BadgeStage(level = 3, targetCount = 30, xpReward = 360),
            BadgeStage(level = 4, targetCount = 60, xpReward = 720)
        )
    );

    val maxLevel: Int get() = stages.size
    val targetCount: Int get() = stages.first().targetCount
    val xpReward: Int get() = stages.first().xpReward

    fun stageForLevel(level: Int): BadgeStage? = stages.firstOrNull { it.level == level }

    fun rankForLevel(currentLevel: Int): BadgeRank = when {
        currentLevel <= 0 -> BadgeRank.LOCKED
        maxLevel == 1 -> BadgeRank.GOLD
        currentLevel >= maxLevel -> BadgeRank.DIAMOND
        currentLevel == 1 -> BadgeRank.BRONZE
        currentLevel == 2 -> BadgeRank.SILVER
        currentLevel == 3 -> BadgeRank.GOLD
        else -> BadgeRank.PLATINUM
    }

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedDescription(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(descriptionTr, descriptionEn, language)

    fun localizedUnit(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(unitTr, unitEn, language)
}

@Serializable
data class GamificationAchievement(
    val badgeId: BadgeId,
    val progressCurrent: Int,
    val progressTarget: Int,
    val isUnlocked: Boolean,
    val rawProgress: Int = progressCurrent,
    val currentLevel: Int = if (isUnlocked) 1 else 0,
    val maxLevel: Int = badgeId.maxLevel,
    val earnedXp: Int = if (isUnlocked) badgeId.xpReward else 0,
    val nextStageXpReward: Int = badgeId.xpReward
) {
    val isMaxed: Boolean
        get() = maxLevel > 0 && currentLevel >= maxLevel

    val progressFraction: Float
        get() = when {
            isMaxed -> 1f
            progressTarget > 0 -> (progressCurrent.toFloat() / progressTarget.toFloat()).coerceIn(0f, 1f)
            isUnlocked -> 1f
            else -> 0f
        }
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
    val unlockedTiersCount: Int = 0,
    val totalTiersCount: Int = BadgeId.entries.sumOf { it.maxLevel },
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
    val badgeStageLevel: Int? = null,
    val streakDays: Int? = null,
    val newLevel: Int? = null
) {
    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedSubtitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(subtitleTr, subtitleEn, language)
}
