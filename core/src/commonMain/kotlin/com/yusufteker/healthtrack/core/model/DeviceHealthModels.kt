package com.yusufteker.healthtrack.core.model

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
    val defaultMet: Double
) {
    RUNNING("Koşu", 8.5),
    WALKING("Yürüyüş", 3.8),
    CYCLING("Bisiklet", 7.0),
    SWIMMING("Yüzme", 7.5),
    STRENGTH("Ağırlık Antrenmanı", 5.0),
    HIIT("HIIT Kardiyo", 8.0),
    YOGA("Yoga & Esneme", 3.0),
    PILATES("Pilates", 3.5),
    ROWING("Kürek Antrenmanı", 7.0),
    ELLIPTICAL("Eliptik Bisiklet", 5.5),
    HIKING("Doğa Yürüyüşü (Hiking)", 6.0),
    BASKETBALL("Basketbol", 7.5),
    FOOTBALL("Futbol", 8.0),
    TENNIS("Tenis", 7.0),
    MARTIAL_ARTS("Dövüş Sporları / Boks", 7.8),
    DAILY_STEPS("Günlük Adım & Hareket", 3.5),
    OTHER("Serbest Antrenman", 5.0)
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
    val sourceName: String = "Akıllı Tartı"
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
    val sourceName: String = "Akıllı Saat"
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

    fun toUserMessage(): String {
        val weightPart = when {
            importedWeights > 0 && updatedWeights > 0 -> "$importedWeights yeni tartım eklendi, $updatedWeights tartım güncellendi"
            importedWeights > 0 -> "$importedWeights yeni tartım eklendi"
            updatedWeights > 0 -> "$updatedWeights tartım güncellendi"
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
            importedWorkouts > 0 && updatedWorkouts > 0 -> "$importedWorkouts yeni antrenman eklendi, $updatedWorkouts antrenman güncellendi$dateRangeSuffix"
            importedWorkouts > 0 -> "$importedWorkouts yeni antrenman eklendi$dateRangeSuffix"
            updatedWorkouts > 0 -> "$updatedWorkouts antrenman güncellendi$dateRangeSuffix"
            else -> null
        }

        return when {
            weightPart != null && workoutPart != null -> "$platformName: $weightPart ve $workoutPart."
            weightPart != null -> "$platformName: $weightPart."
            workoutPart != null -> "$platformName: $workoutPart."
            skippedWeights > 0 || skippedWorkouts > 0 -> "$platformName verileri zaten güncel (çakışma yok, ${skippedWeights + skippedWorkouts} kayıt kontrol edildi)."
            else -> "$platformName üzerinde son 30 güne ait yeni tartım, adım veya antrenman kaydı bulunamadı. Lütfen Huawei Health / Health Sync veya Zepp Life uygulamasının $platformName'e veri yazdığından emin olun."
        }
    }
}

