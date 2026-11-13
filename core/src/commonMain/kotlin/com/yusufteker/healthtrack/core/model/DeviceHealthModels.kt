package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable

/**
 * Availability status of the platform health hub (Android Health Connect / Apple HealthKit).
 */
enum class HealthPlatformStatus {
    AVAILABLE,
    NOT_INSTALLED, // e.g. Health Connect APK needs installation/update on older Android
    NOT_SUPPORTED
}

/**
 * Standardized Exercise Category mapped from Health Connect / HealthKit workout types.
 */
enum class DeviceExerciseCategory(
    val titleTr: String,
    val titleEn: String,
    val defaultMet: Double
) {
    RUNNING("Koşu", "Running", 8.5),
    WALKING("Yürüyüş", "Walking", 3.8),
    CYCLING("Bisiklet", "Cycling", 7.0),
    SWIMMING("Yüzme", "Swimming", 7.5),
    STRENGTH("Ağırlık Antrenmanı", "Strength Training", 5.0),
    HIIT("HIIT Kardiyo", "HIIT Cardio", 8.0),
    YOGA("Yoga & Esneme", "Yoga & Stretching", 3.0),
    PILATES("Pilates", "Pilates", 3.5),
    ROWING("Kürek Antrenmanı", "Rowing", 7.0),
    ELLIPTICAL("Eliptik Bisiklet", "Elliptical", 5.5),
    HIKING("Doğa Yürüyüşü (Hiking)", "Hiking", 6.0),
    BASKETBALL("Basketbol", "Basketball", 7.5),
    FOOTBALL("Futbol", "Football / Soccer", 8.0),
    TENNIS("Tenis", "Tennis", 7.0),
    MARTIAL_ARTS("Dövüş Sporları / Boks", "Martial Arts / Boxing", 7.8),
    DAILY_STEPS("Günlük Adım & Hareket", "Daily Steps & Activity", 3.5),
    OTHER("Serbest Antrenman", "Open Workout", 5.0);

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)
}

/**
 * Normalized weight & body composition measurement read from a smart scale
 * (e.g., Xiaomi Mi Body Composition Scale via Zepp Life / Mi Fitness -> Health Connect / HealthKit).
 */
@Serializable
data class DeviceWeightRecord(
    val externalId: String,
    val date: String, // YYYY-MM-DD in local timezone
    val weightKg: Double,
    val bodyFatPercent: Double? = null,
    val leanBodyMassKg: Double? = null,
    val measuredAtMillis: Long,
    val sourcePackage: String = "",
    val sourceName: String = trEn("Akıllı Tartı", "Smart Scale")
)

/**
 * Normalized workout / exercise session read from a smartwatch or fitness band
 * (e.g., Huawei Watch via Huawei Health, Xiaomi Smart Band via Mi Fitness -> Health Connect / HealthKit).
 */
@Serializable
data class DeviceWorkoutRecord(
    val externalId: String,
    val date: String, // YYYY-MM-DD in local timezone
    val category: DeviceExerciseCategory = DeviceExerciseCategory.OTHER,
    val customTitle: String? = null,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val distanceMeters: Double? = null,
    val steps: Int? = null,
    val avgHeartRateBpm: Int? = null,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val sourcePackage: String = "",
    val sourceName: String = trEn("Akıllı Saat", "Smartwatch")
)

/**
 * Aggregated payload returned by the platform health bridge for a requested time window.
 */
data class DeviceHealthSnapshot(
    val weights: List<DeviceWeightRecord> = emptyList(),
    val workouts: List<DeviceWorkoutRecord> = emptyList(),
    val platformName: String = "Health Connect"
)

/**
 * Summary of a conflict-free device sync operation.
 */
data class DeviceSyncSummary(
    val importedWeights: Int = 0,
    val updatedWeights: Int = 0,
    val skippedWeights: Int = 0,
    val importedWorkouts: Int = 0,
    val updatedWorkouts: Int = 0,
    val skippedWorkouts: Int = 0,
    val latestWeightKg: Double? = null,
    val earliestWorkoutDate: String? = null,
    val latestWorkoutDate: String? = null,
    val platformName: String = "Health Connect"
) {
    val totalChanges: Int
        get() = importedWeights + updatedWeights + importedWorkouts + updatedWorkouts

    fun toUserMessage(language: AppLanguage = LocalizationManager.currentLanguage): String {
        val weightPart = when {
            importedWeights > 0 && updatedWeights > 0 -> trEn(
                "$importedWeights yeni tartım eklendi, $updatedWeights tartım güncellendi",
                "$importedWeights new weigh-ins added, $updatedWeights weigh-ins updated",
                language
            )
            importedWeights > 0 -> trEn(
                "$importedWeights yeni tartım eklendi",
                "$importedWeights new weigh-ins added",
                language
            )
            updatedWeights > 0 -> trEn(
                "$updatedWeights tartım güncellendi",
                "$updatedWeights weigh-ins updated",
                language
            )
            else -> null
        }
        val dateRangeSuffix = when {
            earliestWorkoutDate != null && latestWorkoutDate != null && earliestWorkoutDate != latestWorkoutDate ->
                " ($earliestWorkoutDate .. $latestWorkoutDate)"
            latestWorkoutDate != null ->
                " ($latestWorkoutDate)"
            else -> ""
        }
        val workoutPart = when {
            importedWorkouts > 0 && updatedWorkouts > 0 -> trEn(
                "$importedWorkouts yeni antrenman eklendi, $updatedWorkouts antrenman güncellendi$dateRangeSuffix",
                "$importedWorkouts new workouts added, $updatedWorkouts workouts updated$dateRangeSuffix",
                language
            )
            importedWorkouts > 0 -> trEn(
                "$importedWorkouts yeni antrenman eklendi$dateRangeSuffix",
                "$importedWorkouts new workouts added$dateRangeSuffix",
                language
            )
            updatedWorkouts > 0 -> trEn(
                "$updatedWorkouts antrenman güncellendi$dateRangeSuffix",
                "$updatedWorkouts workouts updated$dateRangeSuffix",
                language
            )
            else -> null
        }

        return when {
            weightPart != null && workoutPart != null -> trEn(
                "$platformName: $weightPart ve $workoutPart.",
                "$platformName: $weightPart and $workoutPart.",
                language
            )
            weightPart != null -> "$platformName: $weightPart."
            workoutPart != null -> "$platformName: $workoutPart."
            skippedWeights > 0 || skippedWorkouts > 0 -> trEn(
                "$platformName verileri zaten güncel (çakışma yok, ${skippedWeights + skippedWorkouts} kayıt kontrol edildi).",
                "$platformName data is already up to date (no conflicts, ${skippedWeights + skippedWorkouts} records checked).",
                language
            )
            else -> trEn(
                "$platformName üzerinde son 30 güne ait yeni tartım, adım veya antrenman kaydı bulunamadı. Lütfen Huawei Health / Health Sync veya Zepp Life uygulamasının $platformName'e veri yazdığından emin olun.",
                "No new weigh-ins, steps, or workouts found on $platformName in the last 30 days. Please make sure Huawei Health / Health Sync or Zepp Life is writing data to $platformName.",
                language
            )
        }
    }
}
