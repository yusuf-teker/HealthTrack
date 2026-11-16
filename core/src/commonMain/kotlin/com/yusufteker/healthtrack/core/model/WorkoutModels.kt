package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable
import kotlin.math.round
import kotlin.math.roundToInt

@Serializable
enum class SportType(
    val titleTr: String,
    val titleEn: String,
    val templateTr: String,
    val templateEn: String,
    val defaultDurationMinutes: Int,
    val defaultCalories: Int,
    val emoji: String = "💪",
    val actionPhraseTr: String = "antrenman yaptı",
    val actionPhraseEn: String = "completed a workout"
) {
    TENNIS(
        titleTr = "Tenis",
        titleEn = "Tennis",
        templateTr = "45 dk Tenis",
        templateEn = "45 min Tennis",
        defaultDurationMinutes = 45,
        defaultCalories = 320,
        emoji = "🎾",
        actionPhraseTr = "tenis oynadı",
        actionPhraseEn = "played tennis"
    ),
    FITNESS(
        titleTr = "Fitness",
        titleEn = "Fitness",
        templateTr = "45 dk Fitness & Ağırlık",
        templateEn = "45 min Fitness & Weights",
        defaultDurationMinutes = 45,
        defaultCalories = 260,
        emoji = "🏋️",
        actionPhraseTr = "fitness yaptı",
        actionPhraseEn = "did a fitness workout"
    ),
    RUNNING(
        titleTr = "Koşu",
        titleEn = "Running",
        templateTr = "30 dk Koşu",
        templateEn = "30 min Run",
        defaultDurationMinutes = 30,
        defaultCalories = 310,
        emoji = "🏃",
        actionPhraseTr = "koşu yaptı",
        actionPhraseEn = "went for a run"
    ),
    ROWING(
        titleTr = "Kürek",
        titleEn = "Rowing",
        templateTr = "25 dk Kürek Makinesi",
        templateEn = "25 min Rowing Machine",
        defaultDurationMinutes = 25,
        defaultCalories = 240,
        emoji = "🚣",
        actionPhraseTr = "kürek çekti",
        actionPhraseEn = "did a rowing session"
    ),
    BASKETBALL(
        titleTr = "Basketbol",
        titleEn = "Basketball",
        templateTr = "45 dk Basketbol",
        templateEn = "45 min Basketball",
        defaultDurationMinutes = 45,
        defaultCalories = 360,
        emoji = "🏀",
        actionPhraseTr = "basketbol oynadı",
        actionPhraseEn = "played basketball"
    ),
    FOOTBALL(
        titleTr = "Futbol",
        titleEn = "Football",
        templateTr = "60 dk Futbol",
        templateEn = "60 min Football",
        defaultDurationMinutes = 60,
        defaultCalories = 450,
        emoji = "⚽",
        actionPhraseTr = "futbol oynadı",
        actionPhraseEn = "played football"
    ),
    CYCLING(
        titleTr = "Bisiklet",
        titleEn = "Cycling",
        templateTr = "30 dk Bisiklet",
        templateEn = "30 min Cycling",
        defaultDurationMinutes = 30,
        defaultCalories = 250,
        emoji = "🚴",
        actionPhraseTr = "bisiklet sürdü",
        actionPhraseEn = "went cycling"
    ),
    SWIMMING(
        titleTr = "Yüzme",
        titleEn = "Swimming",
        templateTr = "30 dk Yüzme",
        templateEn = "30 min Swimming",
        defaultDurationMinutes = 30,
        defaultCalories = 280,
        emoji = "🏊",
        actionPhraseTr = "yüzdü",
        actionPhraseEn = "went swimming"
    ),
    WALKING(
        titleTr = "Yürüyüş",
        titleEn = "Walking",
        templateTr = "40 dk Tempolu Yürüyüş",
        templateEn = "40 min Brisk Walk",
        defaultDurationMinutes = 40,
        defaultCalories = 180,
        emoji = "🚶",
        actionPhraseTr = "yürüyüş yaptı",
        actionPhraseEn = "went for a walk"
    ),
    YOGA_PILATES(
        titleTr = "Yoga / Pilates",
        titleEn = "Yoga / Pilates",
        templateTr = "40 dk Yoga & Pilates",
        templateEn = "40 min Yoga & Pilates",
        defaultDurationMinutes = 40,
        defaultCalories = 160,
        emoji = "🧘",
        actionPhraseTr = "yoga & pilates yaptı",
        actionPhraseEn = "did yoga & pilates"
    ),
    VOLLEYBALL(
        titleTr = "Voleybol",
        titleEn = "Volleyball",
        templateTr = "45 dk Voleybol",
        templateEn = "45 min Volleyball",
        defaultDurationMinutes = 45,
        defaultCalories = 300,
        emoji = "🏐",
        actionPhraseTr = "voleybol oynadı",
        actionPhraseEn = "played volleyball"
    ),
    BOXING(
        titleTr = "Boks / Dövüş",
        titleEn = "Boxing / MMA",
        templateTr = "45 dk Boks & Kickboks",
        templateEn = "45 min Boxing & Kickboxing",
        defaultDurationMinutes = 45,
        defaultCalories = 420,
        emoji = "🥊",
        actionPhraseTr = "boks yaptı",
        actionPhraseEn = "did a boxing session"
    ),
    HIIT(
        titleTr = "HIIT / Kardiyo",
        titleEn = "HIIT / Cardio",
        templateTr = "25 dk HIIT Kardiyo",
        templateEn = "25 min HIIT Cardio",
        defaultDurationMinutes = 25,
        defaultCalories = 310,
        emoji = "⚡",
        actionPhraseTr = "HIIT antrenmanı yaptı",
        actionPhraseEn = "did a HIIT workout"
    ),
    CALISTHENICS(
        titleTr = "Kalistenik",
        titleEn = "Calisthenics",
        templateTr = "40 dk Kalistenik & Barfiks",
        templateEn = "40 min Calisthenics & Pull-ups",
        defaultDurationMinutes = 40,
        defaultCalories = 270,
        emoji = "🤸",
        actionPhraseTr = "kalistenik yaptı",
        actionPhraseEn = "did calisthenics"
    ),
    DANCE(
        titleTr = "Dans / Zumba",
        titleEn = "Dance / Zumba",
        templateTr = "45 dk Dans & Zumba",
        templateEn = "45 min Dance & Zumba",
        defaultDurationMinutes = 45,
        defaultCalories = 310,
        emoji = "💃",
        actionPhraseTr = "dans antrenmanı yaptı",
        actionPhraseEn = "did a dance workout"
    ),
    OTHER(
        titleTr = "Antrenman",
        titleEn = "Workout",
        templateTr = "30 dk Antrenman",
        templateEn = "30 min Workout",
        defaultDurationMinutes = 30,
        defaultCalories = 200,
        emoji = "💪",
        actionPhraseTr = "antrenman yaptı",
        actionPhraseEn = "completed a workout"
    );

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedTemplate(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(templateTr, templateEn, language)

    companion object {
        private val kortRegex = Regex("""\bkort(ta|da)?\b""")
        private val potaRegex = Regex("""\bpota(ya|da)?\b""")
        private val kaleRegex = Regex("""\bkale(de|si|ye)?\b""")
        private val gymRegex = Regex("""\bgym\b""")
        private val runWordRegex = Regex("""\b(run|runs|kosu|kosma)\b""")
        private val runningStemRegex = Regex("""\bkos""")

        internal fun normalizeForSportMatch(rawText: String): String {
            if (rawText.isEmpty()) return ""
            val sb = StringBuilder(rawText.length)
            for (ch in rawText) {
                when (ch) {
                    in '\u0300'..'\u036F' -> continue
                    'İ', 'I', 'ı', 'i' -> sb.append('i')
                    'Ğ', 'ğ' -> sb.append('g')
                    'Ü', 'ü' -> sb.append('u')
                    'Ş', 'ş' -> sb.append('s')
                    'Ö', 'ö' -> sb.append('o')
                    'Ç', 'ç' -> sb.append('c')
                    else -> sb.append(ch.lowercaseChar())
                }
            }
            return sb.toString()
        }

        private fun matchesTennis(lower: String): Boolean =
            lower.contains("tenis") || lower.contains("tennis") || lower.contains("raket") ||
                lower.contains("padel") || lower.contains("squash") || lower.contains("badminton") ||
                kortRegex.containsMatchIn(lower)

        private fun matchesFitness(lower: String): Boolean =
            lower.contains("fitness") || lower.contains("agirlik") ||
                lower.contains("dambil") || lower.contains("dumbell") ||
                lower.contains("dumbbell") || lower.contains("bodybuilding") || lower.contains("hipertrofi") ||
                lower.contains("strength") || lower.contains("weight training") || lower.contains("weights") ||
                gymRegex.containsMatchIn(lower) || lower.contains("bench") || lower.contains("squat") ||
                lower.contains("deadlift")

        private fun matchesRunning(lower: String): Boolean =
            lower.contains("kosu") || lower.contains("running") || lower.contains("jogging") ||
                lower.contains("treadmill") || lower.contains("kosu bandi") ||
                runWordRegex.containsMatchIn(lower) || runningStemRegex.containsMatchIn(lower)

        private fun matchesRowing(lower: String): Boolean =
            lower.contains("kurek") || lower.contains("rowing") ||
                lower.contains("ergometre") || lower.contains("ergometer")

        private fun matchesBasketball(lower: String): Boolean =
            lower.contains("basketbol") || lower.contains("basketball") || lower.contains("basket") ||
                potaRegex.containsMatchIn(lower)

        private fun matchesFootball(lower: String): Boolean =
            lower.contains("futbol") || lower.contains("football") || lower.contains("soccer") ||
                lower.contains("hali saha") || kaleRegex.containsMatchIn(lower)

        private fun matchesCycling(lower: String): Boolean =
            lower.contains("bisiklet") || lower.contains("cycling") || lower.contains("eliptik") ||
                lower.contains("elliptical") || lower.contains("pedal") || lower.contains("spin")

        private fun matchesSwimming(lower: String): Boolean =
            lower.contains("yuzme") || lower.contains("swimming") ||
                lower.contains("swim") || lower.contains("havuz")

        private fun matchesWalking(lower: String): Boolean =
            lower.contains("yuruyus") || lower.contains("walking") ||
                lower.contains("yuru") || lower.contains("trekking") ||
                lower.contains("hiking")

        private fun matchesYogaPilates(lower: String): Boolean =
            lower.contains("yoga") || lower.contains("pilates") || lower.contains("esneme") ||
                lower.contains("stretching")

        private fun matchesVolleyball(lower: String): Boolean =
            lower.contains("voleybol") || lower.contains("volleyball") ||
                lower.contains("plaj voleybolu") || lower.contains("smac")

        private fun matchesBoxing(lower: String): Boolean =
            lower.contains("boks") || lower.contains("boxing") || lower.contains("kickboks") ||
                lower.contains("kickbox") || lower.contains("mma") || lower.contains("muay thai") ||
                lower.contains("dovus") || lower.contains("karate") ||
                lower.contains("taekwondo") || lower.contains("tekvando") || lower.contains("judo") ||
                lower.contains("gures")

        private fun matchesHiit(lower: String): Boolean =
            lower.contains("hiit") || lower.contains("crossfit") || lower.contains("tabata") ||
                lower.contains("kardiyo") || lower.contains("cardio") || lower.contains("circuit") ||
                lower.contains("ip atlama") || lower.contains("jump rope") || lower.contains("burpee")

        private fun matchesCalisthenics(lower: String): Boolean =
            lower.contains("kalistenik") || lower.contains("calisthenics") ||
                lower.contains("street workout") || lower.contains("barfiks") ||
                lower.contains("pull-up") || lower.contains("pullup") ||
                lower.contains("sinav") ||
                lower.contains("push-up") || lower.contains("pushup") ||
                lower.contains("dips") || lower.contains("plank") ||
                lower.contains("cimnastik") || lower.contains("gymnastics")

        private fun matchesDance(lower: String): Boolean =
            lower.contains("dans") || lower.contains("dance") || lower.contains("zumba") ||
                lower.contains("aerobik") || lower.contains("aerobics") ||
                lower.contains("step aerobik") || lower.contains("salsa") ||
                lower.contains("hip hop")

        fun fromRawText(rawText: String): SportType {
            val lower = normalizeForSportMatch(rawText)
            return when {
                matchesTennis(lower) -> TENNIS
                matchesBasketball(lower) -> BASKETBALL
                matchesFootball(lower) -> FOOTBALL
                matchesVolleyball(lower) -> VOLLEYBALL
                matchesBoxing(lower) -> BOXING
                matchesRowing(lower) -> ROWING
                matchesHiit(lower) -> HIIT
                matchesCalisthenics(lower) -> CALISTHENICS
                matchesDance(lower) -> DANCE
                matchesFitness(lower) -> FITNESS
                matchesRunning(lower) -> RUNNING
                matchesCycling(lower) -> CYCLING
                matchesSwimming(lower) -> SWIMMING
                matchesWalking(lower) -> WALKING
                matchesYogaPilates(lower) -> YOGA_PILATES
                else -> OTHER
            }
        }

        fun detectAll(rawText: String): Set<SportType> {
            val lower = normalizeForSportMatch(rawText)
            return buildSet {
                if (matchesTennis(lower)) add(TENNIS)
                if (matchesFitness(lower)) add(FITNESS)
                if (matchesRunning(lower)) add(RUNNING)
                if (matchesRowing(lower)) add(ROWING)
                if (matchesBasketball(lower)) add(BASKETBALL)
                if (matchesFootball(lower)) add(FOOTBALL)
                if (matchesCycling(lower)) add(CYCLING)
                if (matchesSwimming(lower)) add(SWIMMING)
                if (matchesWalking(lower)) add(WALKING)
                if (matchesYogaPilates(lower)) add(YOGA_PILATES)
                if (matchesVolleyball(lower)) add(VOLLEYBALL)
                if (matchesBoxing(lower)) add(BOXING)
                if (matchesHiit(lower)) add(HIIT)
                if (matchesCalisthenics(lower)) add(CALISTHENICS)
                if (matchesDance(lower)) add(DANCE)
            }
        }

        fun fromWorkout(workout: WorkoutLog): SportType {
            val fromRaw = fromRawText(workout.rawText)
            if (fromRaw != OTHER) return fromRaw
            val analysis = workout.aiAnalysis?.takeIf { it.isNotBlank() } ?: return OTHER
            return fromRawText(analysis)
        }

        fun detectAllForWorkout(workout: WorkoutLog): Set<SportType> {
            val fromRaw = detectAll(workout.rawText)
            if (fromRaw.isNotEmpty()) return fromRaw
            val analysis = workout.aiAnalysis?.takeIf { it.isNotBlank() } ?: return emptySet()
            return detectAll(analysis)
        }

        fun fromWire(raw: String?): SportType? {
            val cleaned = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
            return entries.firstOrNull { it.name.equals(cleaned, ignoreCase = true) }
        }

        fun formatFriendWorkoutNotification(
            actorFullName: String,
            rawText: String,
            durationMinutes: Int,
            caloriesBurned: Int,
            language: AppLanguage = LocalizationManager.currentLanguage
        ): Pair<String, String> {
            val nickname = actorFullName.trim().substringBefore(" ").ifBlank {
                trEn("Arkadaşın", "Your friend", language)
            }
            val sport = fromRawText(rawText)
            val phrase = trEn(sport.actionPhraseTr, sport.actionPhraseEn, language)
            val minUnit = trEn("dk", "min", language)
            val title = if (durationMinutes > 0) {
                "${sport.emoji} $nickname $durationMinutes $minUnit $phrase"
            } else {
                "${sport.emoji} $nickname $phrase"
            }
            val body = if (caloriesBurned > 0) {
                trEn(
                    "🔥 $caloriesBurned kcal yaktı • Sen de serini koru!",
                    "🔥 Burned $caloriesBurned kcal • Keep your streak alive!",
                    language
                )
            } else {
                trEn(
                    "HealthTrack'te yeni antrenman kaydı!",
                    "Logged a new workout on HealthTrack!",
                    language
                )
            }
            return title to body
        }
    }
}

@Serializable
data class WorkoutLog(
    val id: String,
    val userId: Int,
    val date: String, // YYYY-MM-DD
    val rawText: String,
    val caloriesBurned: Int = 0,
    val durationMinutes: Int = 0,
    val targetCalories: Int = 300,
    val targetDurationMinutes: Int = 45,
    val aiAnalysis: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) {
    val isDailySteps: Boolean
        get() = id.endsWith("_steps") ||
            id.contains("daily_steps") ||
            rawText.contains("Adım • Günlük Hareket", ignoreCase = true) ||
            rawText.contains("Steps • Daily Activity", ignoreCase = true)
}

@Serializable
data class DailyStepTelemetry(
    val stepCount: Int = 0,
    val distanceKm: Double = 0.0,
    val durationMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    val netCaloriesBurned: Int = 0,
    val sourceName: String = trEn("Akıllı Saat", "Smartwatch"),
    val stepTarget: Int = 10_000
) {
    val progress: Float
        get() = if (stepTarget > 0) (stepCount.toFloat() / stepTarget).coerceIn(0f, 1f) else 0f
}

/**
 * Shared telemetry helpers used by both WorkoutViewModel and GamificationManager
 * so daily calorie burn rings and goal-reached celebrations always match 1:1.
 */
object WorkoutTelemetryHelper {
    fun estimateSportSteps(rawText: String, durationMinutes: Int): Int {
        if (durationMinutes <= 0) return 0
        val lower = rawText.lowercase()
        val stepsPerMinute = when {
            lower.contains("koşu") || lower.contains("running") || lower.contains("jogging") -> 140
            lower.contains("hiit") || lower.contains("kardiyo") || lower.contains("cardio") ||
                lower.contains("crossfit") || lower.contains("tabata") -> 120
            lower.contains("dans") || lower.contains("dance") || lower.contains("zumba") ||
                lower.contains("aerobik") -> 115
            lower.contains("boks") || lower.contains("boxing") || lower.contains("kickboks") ||
                lower.contains("mma") || lower.contains("dövüş") -> 110
            lower.contains("basket") || lower.contains("futbol") || lower.contains("soccer") -> 100
            lower.contains("yürüyüş") || lower.contains("walking") || lower.contains("trekking") -> 100
            lower.contains("voleybol") || lower.contains("volleyball") -> 95
            lower.contains("tenis") || lower.contains("tennis") -> 85
            lower.contains("kalistenik") || lower.contains("calisthenics") ||
                lower.contains("barfiks") || lower.contains("şınav") -> 35
            lower.contains("fitness") || lower.contains("ağırlık") || lower.contains("body") || lower.contains("bench") -> 25
            lower.contains("pilates") || lower.contains("yoga") -> 10
            lower.contains("bisiklet") || lower.contains("cycling") || lower.contains("yüzme") || lower.contains("swim") -> 0
            else -> 60
        }
        return stepsPerMinute * durationMinutes
    }

    fun parseDailyStepTelemetry(
        stepLog: WorkoutLog,
        explicitWorkouts: List<WorkoutLog> = emptyList()
    ): DailyStepTelemetry {
        val steps = Regex("""(\d+)\s*(?:Adım|Steps)""", RegexOption.IGNORE_CASE)
            .find(stepLog.rawText)?.groupValues?.get(1)?.toIntOrNull()
            ?: (stepLog.durationMinutes * 100).coerceAtLeast(0)
        val analysis = stepLog.aiAnalysis.orEmpty()
        val distanceKm = Regex("""([\d.,]+)\s*km""").find(analysis)?.groupValues?.get(1)
            ?.replace(',', '.')?.toDoubleOrNull()
            ?: ((steps * 0.75) / 1000.0)
        val defaultWatchName = trEn("Akıllı Saat", "Smart Watch")
        val source = if (analysis.contains("•")) {
            analysis.substringBefore("•").trim().removePrefix("⌚").trim()
        } else {
            defaultWatchName
        }
        val estimatedSportSteps = explicitWorkouts.sumOf { estimateSportSteps(it.rawText, it.durationMinutes) }
        val netSteps = (steps - estimatedSportSteps).coerceAtLeast(0)
        val netKcal = if (steps > 0) {
            val ratio = netSteps.toDouble() / steps
            (stepLog.caloriesBurned * ratio).roundToInt()
        } else 0

        return DailyStepTelemetry(
            stepCount = steps,
            distanceKm = round(distanceKm * 10.0) / 10.0,
            durationMinutes = stepLog.durationMinutes,
            caloriesBurned = stepLog.caloriesBurned,
            netCaloriesBurned = netKcal,
            sourceName = source.ifBlank { defaultWatchName },
            stepTarget = 10_000
        )
    }

    fun calculateEffectiveDailyBurn(
        explicitWorkouts: List<WorkoutLog>,
        stepLog: WorkoutLog?
    ): Int {
        val explicitBurned = explicitWorkouts.sumOf { it.caloriesBurned.coerceAtLeast(0) }
        val netStepBurned = stepLog?.let { parseDailyStepTelemetry(it, explicitWorkouts).netCaloriesBurned } ?: 0
        return explicitBurned + netStepBurned
    }
}
