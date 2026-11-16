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
    EMERALD(0xFF10B981, "Zümrüt", "Emerald"),
    SAPPHIRE(0xFF3B82F6, "Safir", "Sapphire"),
    RUBY(0xFFEF4444, "Yakut", "Ruby"),
    DIAMOND(0xFFEC4899, "Elmas", "Diamond"),
    MASTER(0xFFA855F7, "Usta", "Master"),
    LEGEND(0xFFFACC15, "Efsane", "Legend");

    fun localizedLabel(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(labelTr, labelEn, language)
}

private val sportBadgeStages: List<BadgeStage> = listOf(
    BadgeStage(level = 1, targetCount = 1, xpReward = 75),
    BadgeStage(level = 2, targetCount = 4, xpReward = 150),
    BadgeStage(level = 3, targetCount = 9, xpReward = 300),
    BadgeStage(level = 4, targetCount = 19, xpReward = 600),
    BadgeStage(level = 5, targetCount = 34, xpReward = 1000),
    BadgeStage(level = 6, targetCount = 59, xpReward = 1500),
    BadgeStage(level = 7, targetCount = 99, xpReward = 2200),
    BadgeStage(level = 8, targetCount = 159, xpReward = 3200),
    BadgeStage(level = 9, targetCount = 249, xpReward = 4500),
    BadgeStage(level = 10, targetCount = 399, xpReward = 6500)
)

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
    ),
    DEVICE_BRIDGE(
        category = BadgeCategory.WEIGHT,
        titleTr = "Cihaz Köprüsü",
        titleEn = "Device Bridge",
        descriptionTr = "En az 1 akıllı tartı veya akıllı saat bağlayıp verilerini eşitle.",
        descriptionEn = "Connect and sync at least 1 smart scale or smartwatch.",
        unitTr = "cihaz",
        unitEn = "device",
        accentColorHex = 0xFF06B6D4,
        stages = listOf(
            BadgeStage(level = 1, targetCount = 1, xpReward = 150)
        )
    ),
    SPORT_TENNIS(
        category = BadgeCategory.WORKOUT,
        titleTr = "Kortun Efendisi",
        titleEn = "Tennis Ace",
        descriptionTr = "Kortta yaptığın tenis antrenmanlarıyla branş madalyanı yükselt.",
        descriptionEn = "Level up your tennis medal with every session on court.",
        unitTr = "tenis",
        unitEn = "tennis",
        accentColorHex = 0xFF10B981,
        stages = sportBadgeStages
    ),
    SPORT_FITNESS(
        category = BadgeCategory.WORKOUT,
        titleTr = "Demir Bilek",
        titleEn = "Iron Titan",
        descriptionTr = "Ağırlık ve fitness antrenmanlarıyla güç madalyanı parlat.",
        descriptionEn = "Polish your strength medal through fitness and weight sessions.",
        unitTr = "fitness",
        unitEn = "fitness",
        accentColorHex = 0xFFF59E0B,
        stages = sportBadgeStages
    ),
    SPORT_RUNNING(
        category = BadgeCategory.WORKOUT,
        titleTr = "Koşu Fırtınası",
        titleEn = "Running Storm",
        descriptionTr = "Koşu ve koşu bandı seanslarını artırarak madalyanı yükselt.",
        descriptionEn = "Upgrade your medal by stacking running and treadmill sessions.",
        unitTr = "koşu",
        unitEn = "runs",
        accentColorHex = 0xFFFB7185,
        stages = sportBadgeStages
    ),
    SPORT_ROWING(
        category = BadgeCategory.WORKOUT,
        titleTr = "Dalga Kıran",
        titleEn = "Rowing Master",
        descriptionTr = "Kürek antrenmanlarıyla üst düzey dayanıklılık madalyası kazan.",
        descriptionEn = "Earn endurance medals from every rowing workout.",
        unitTr = "kürek",
        unitEn = "rows",
        accentColorHex = 0xFF38BDF8,
        stages = sportBadgeStages
    ),
    SPORT_BASKETBALL(
        category = BadgeCategory.WORKOUT,
        titleTr = "Potanın Kralı",
        titleEn = "Hoops King",
        descriptionTr = "Basketbol antrenmanları ve maçlarıyla pota madalyanı zirveye taşı.",
        descriptionEn = "Unlock your hoops medal with basketball training and games.",
        unitTr = "basketbol",
        unitEn = "basketball",
        accentColorHex = 0xFFF97316,
        stages = sportBadgeStages
    ),
    SPORT_FOOTBALL(
        category = BadgeCategory.WORKOUT,
        titleTr = "Gol Kralı",
        titleEn = "Pitch Striker",
        descriptionTr = "Futbol ve halı saha maçlarıyla yeşil sahalardaki madalyanı geliştir.",
        descriptionEn = "Level up your striker medal with football and pitch matches.",
        unitTr = "futbol",
        unitEn = "football",
        accentColorHex = 0xFF22C55E,
        stages = sportBadgeStages
    ),
    SPORT_CYCLING(
        category = BadgeCategory.WORKOUT,
        titleTr = "Pedal Ustası",
        titleEn = "Pedal Master",
        descriptionTr = "Bisiklet, spinning ve eliptik sürüşleriyle yolların tozunu attır.",
        descriptionEn = "Conquer the road with cycling, spinning, and elliptical rides.",
        unitTr = "sürüş",
        unitEn = "rides",
        accentColorHex = 0xFF06D6A0,
        stages = sportBadgeStages
    ),
    SPORT_SWIMMING(
        category = BadgeCategory.WORKOUT,
        titleTr = "Kulaç Ustası",
        titleEn = "Aqua Master",
        descriptionTr = "Havuz ve açık su yüzme antrenmanlarıyla su madalyanı yükselt.",
        descriptionEn = "Level up your aqua medal with pool and open-water swim sessions.",
        unitTr = "yüzme",
        unitEn = "swims",
        accentColorHex = 0xFF0EA5E9,
        stages = sportBadgeStages
    ),
    SPORT_WALKING(
        category = BadgeCategory.WORKOUT,
        titleTr = "Yolların Fatihi",
        titleEn = "Trail Blazer",
        descriptionTr = "Tempolu yürüyüş ve doğa yürüyüşleriyle aktif adımlarını taçlandır.",
        descriptionEn = "Crown your active steps with brisk walks, trekking, and hikes.",
        unitTr = "yürüyüş",
        unitEn = "walks",
        accentColorHex = 0xFF34D399,
        stages = sportBadgeStages
    ),
    SPORT_YOGA_PILATES(
        category = BadgeCategory.WORKOUT,
        titleTr = "Zen & Denge",
        titleEn = "Zen Master",
        descriptionTr = "Yoga, pilates ve esneme seanslarıyla esneklik ve denge ustası ol.",
        descriptionEn = "Master flexibility, core control, and balance with yoga and pilates.",
        unitTr = "seans",
        unitEn = "sessions",
        accentColorHex = 0xFFA855F7,
        stages = sportBadgeStages
    ),
    SPORT_VOLLEYBALL(
        category = BadgeCategory.WORKOUT,
        titleTr = "Filenin Sultanı",
        titleEn = "Net Monarch",
        descriptionTr = "Voleybol maçları ve antrenmanlarıyla file üstü hakimiyetini kanıtla.",
        descriptionEn = "Rule the net with volleyball matches, blocks, and spikes.",
        unitTr = "voleybol",
        unitEn = "volleyball",
        accentColorHex = 0xFFEC4899,
        stages = sportBadgeStages
    ),
    SPORT_BOXING(
        category = BadgeCategory.WORKOUT,
        titleTr = "Ringlerin Hakimi",
        titleEn = "Ring Champion",
        descriptionTr = "Boks, kickboks ve dövüş antrenmanlarıyla yıkılmaz kondisyon inşa et.",
        descriptionEn = "Build unstoppable conditioning with boxing, kickboxing, and combat sessions.",
        unitTr = "raunt",
        unitEn = "sessions",
        accentColorHex = 0xFFEF4444,
        stages = sportBadgeStages
    ),
    SPORT_HIIT(
        category = BadgeCategory.WORKOUT,
        titleTr = "Nabız Avcısı",
        titleEn = "HIIT Dynamo",
        descriptionTr = "HIIT, CrossFit ve yüksek tempolu kardiyo ile sınırlarını zorla.",
        descriptionEn = "Push your limits with explosive HIIT, CrossFit, and circuit training.",
        unitTr = "kardiyo",
        unitEn = "HIIT",
        accentColorHex = 0xFFFACC15,
        stages = sportBadgeStages
    ),
    SPORT_CALISTHENICS(
        category = BadgeCategory.WORKOUT,
        titleTr = "Barların Efendisi",
        titleEn = "Calisthenics Pro",
        descriptionTr = "Barfiks, şınav, dips ve kalistenik egzersizlerle kendi ağırlığına hükmet.",
        descriptionEn = "Master your own bodyweight with pull-ups, push-ups, dips, and calisthenics.",
        unitTr = "kalistenik",
        unitEn = "calisthenics",
        accentColorHex = 0xFF6366F1,
        stages = sportBadgeStages
    ),
    SPORT_DANCE(
        category = BadgeCategory.WORKOUT,
        titleTr = "Ritim Ustası",
        titleEn = "Rhythm Master",
        descriptionTr = "Dans, zumba ve aerobik seanslarıyla ritmi yüksek kalori yakımına dönüştür.",
        descriptionEn = "Turn rhythm into high-energy burn with dance, Zumba, and aerobics.",
        unitTr = "dans",
        unitEn = "dance",
        accentColorHex = 0xFFF43F5E,
        stages = sportBadgeStages
    );

    val maxLevel: Int get() = stages.size
    val targetCount: Int get() = stages.first().targetCount
    val xpReward: Int get() = stages.first().xpReward

    fun stageForLevel(level: Int): BadgeStage? = stages.firstOrNull { it.level == level }

    fun rankForLevel(currentLevel: Int): BadgeRank = when {
        currentLevel <= 0 -> BadgeRank.LOCKED
        maxLevel == 1 -> BadgeRank.GOLD
        maxLevel <= 5 && currentLevel >= maxLevel -> BadgeRank.DIAMOND
        currentLevel >= 10 -> BadgeRank.LEGEND
        currentLevel == 1 -> BadgeRank.BRONZE
        currentLevel == 2 -> BadgeRank.SILVER
        currentLevel == 3 -> BadgeRank.GOLD
        currentLevel == 4 -> BadgeRank.PLATINUM
        currentLevel == 5 -> BadgeRank.EMERALD
        currentLevel == 6 -> BadgeRank.SAPPHIRE
        currentLevel == 7 -> BadgeRank.RUBY
        currentLevel == 8 -> BadgeRank.DIAMOND
        currentLevel == 9 -> BadgeRank.MASTER
        else -> BadgeRank.LEGEND
    }

    fun toSportTypeOrNull(): SportType? = when (this) {
        SPORT_TENNIS -> SportType.TENNIS
        SPORT_FITNESS -> SportType.FITNESS
        SPORT_RUNNING -> SportType.RUNNING
        SPORT_ROWING -> SportType.ROWING
        SPORT_BASKETBALL -> SportType.BASKETBALL
        SPORT_FOOTBALL -> SportType.FOOTBALL
        SPORT_CYCLING -> SportType.CYCLING
        SPORT_SWIMMING -> SportType.SWIMMING
        SPORT_WALKING -> SportType.WALKING
        SPORT_YOGA_PILATES -> SportType.YOGA_PILATES
        SPORT_VOLLEYBALL -> SportType.VOLLEYBALL
        SPORT_BOXING -> SportType.BOXING
        SPORT_HIIT -> SportType.HIIT
        SPORT_CALISTHENICS -> SportType.CALISTHENICS
        SPORT_DANCE -> SportType.DANCE
        else -> null
    }

    val isSportBadge: Boolean get() = toSportTypeOrNull() != null

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedDescription(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(descriptionTr, descriptionEn, language)

    fun localizedUnit(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(unitTr, unitEn, language)

    companion object {
        fun fromSportType(sportType: SportType): BadgeId? = when (sportType) {
            SportType.TENNIS -> SPORT_TENNIS
            SportType.FITNESS -> SPORT_FITNESS
            SportType.RUNNING -> SPORT_RUNNING
            SportType.ROWING -> SPORT_ROWING
            SportType.BASKETBALL -> SPORT_BASKETBALL
            SportType.FOOTBALL -> SPORT_FOOTBALL
            SportType.CYCLING -> SPORT_CYCLING
            SportType.SWIMMING -> SPORT_SWIMMING
            SportType.WALKING -> SPORT_WALKING
            SportType.YOGA_PILATES -> SPORT_YOGA_PILATES
            SportType.VOLLEYBALL -> SPORT_VOLLEYBALL
            SportType.BOXING -> SPORT_BOXING
            SportType.HIIT -> SPORT_HIIT
            SportType.CALISTHENICS -> SPORT_CALISTHENICS
            SportType.DANCE -> SPORT_DANCE
            else -> null
        }
    }
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

    val previousStageTarget: Int
        get() = if (currentLevel <= 0) 0 else badgeId.stageForLevel(currentLevel)?.targetCount ?: 0

    val stageStepTarget: Int
        get() = if (isMaxed) {
            val prev = badgeId.stageForLevel(maxLevel - 1)?.targetCount ?: 0
            (progressTarget - prev).coerceAtLeast(1)
        } else {
            (progressTarget - previousStageTarget).coerceAtLeast(1)
        }

    val stageStepCurrent: Int
        get() = if (isMaxed) {
            stageStepTarget
        } else {
            (rawProgress - previousStageTarget).coerceIn(0, stageStepTarget)
        }

    val stageProgressFraction: Float
        get() = if (isMaxed) {
            1f
        } else {
            (stageStepCurrent.toFloat() / stageStepTarget.toFloat()).coerceIn(0f, 1f)
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
    SPORT_PROGRESS,
    LEVEL_UP,
    STREAK_MILESTONE,
    DAILY_GOAL_COMPLETED,
    WORKOUT_GOAL_REACHED,
    WATER_GOAL_REACHED,
    HEALTHY_MEAL_LOGGED
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
    val newLevel: Int? = null,
    val progressCurrent: Int? = null,
    val progressTarget: Int? = null,
    val previousProgressFraction: Float? = null,
    val targetProgressFraction: Float? = null,
    val nextStageLevel: Int? = null,
    val showConfetti: Boolean = true,
    val autoDismissMs: Long? = null,
    val healthScore: Int? = null
) {
    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedSubtitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(subtitleTr, subtitleEn, language)
}
