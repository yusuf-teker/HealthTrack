package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yusufteker.healthtrack.core.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.*
import platform.HealthKit.*
import platform.UIKit.UIApplication
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
actual fun rememberDeviceHealthBridge(): DeviceHealthBridge {
    return remember {
        object : DeviceHealthBridge {
            private val healthStore: HKHealthStore? =
                if (HKHealthStore.isHealthDataAvailable()) HKHealthStore() else null

            override val platformName: String = "Apple Health"

            override val status: HealthPlatformStatus
                get() = if (HKHealthStore.isHealthDataAvailable()) {
                    HealthPlatformStatus.AVAILABLE
                } else {
                    HealthPlatformStatus.NOT_SUPPORTED
                }

            override fun syncLastDays(
                days: Int,
                onResult: (Result<DeviceHealthSnapshot>) -> Unit
            ) {
                val store = healthStore
                if (store == null) {
                    onResult(
                        Result.failure(
                            IllegalStateException("Cihazınızda Apple HealthKit desteği bulunmuyor.")
                        )
                    )
                    return
                }

                val bodyMassType = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBodyMass)
                val bodyFatType = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBodyFatPercentage)
                val leanMassType = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierLeanBodyMass)
                val workoutType = HKObjectType.workoutType()

                val readTypes = buildSet<HKObjectType> {
                    if (bodyMassType != null) add(bodyMassType)
                    if (bodyFatType != null) add(bodyFatType)
                    if (leanMassType != null) add(leanMassType)
                    add(workoutType)
                }

                store.requestAuthorizationToShareTypes(
                    typesToShare = null,
                    readTypes = readTypes
                ) { success, error ->
                    if (!success || error != null) {
                        val msg = error?.localizedDescription ?: "Apple Health okuma izni reddedildi."
                        onResult(Result.failure(IllegalStateException(msg)))
                        return@requestAuthorizationToShareTypes
                    }

                    val nowDate = NSDate()
                    val secondsBack = -(days.coerceIn(1, 90).toDouble() * 86_400.0)
                    val startDate = nowDate.dateByAddingTimeInterval(secondsBack)
                    val predicate = HKQuery.predicateForSamplesWithStartDate(
                        startDate = startDate,
                        endDate = nowDate,
                        options = HKQueryOptionNone
                    )

                    queryHealthKitSamples(
                        store = store,
                        predicate = predicate,
                        bodyMassType = bodyMassType,
                        bodyFatType = bodyFatType,
                        leanMassType = leanMassType,
                        workoutType = workoutType,
                        onResult = onResult
                    )
                }
            }

            override fun openHealthSettings() {
                val url = NSURL.URLWithString("x-apple-health://")
                if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
                    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
                }
            }
        }
    }
}

private fun queryHealthKitSamples(
    store: HKHealthStore,
    predicate: NSPredicate?,
    bodyMassType: HKQuantityType?,
    bodyFatType: HKQuantityType?,
    leanMassType: HKQuantityType?,
    workoutType: HKWorkoutType,
    onResult: (Result<DeviceHealthSnapshot>) -> Unit
) {
    if (bodyMassType == null) {
        onResult(Result.success(DeviceHealthSnapshot(platformName = "Apple Health")))
        return
    }

    val weightQuery = HKSampleQuery(
        sampleType = bodyMassType,
        predicate = predicate,
        limit = 500u,
        sortDescriptors = null
    ) { _, weightSamples, _ ->
        val kgUnit = HKUnit.gramUnitWithMetricPrefix(HKMetricPrefixKilo)
        val rawWeights = weightSamples?.filterIsInstance<HKQuantitySample>().orEmpty()

        val fatQuery = if (bodyFatType != null) {
            HKSampleQuery(
                sampleType = bodyFatType,
                predicate = predicate,
                limit = 500u,
                sortDescriptors = null
            ) { _, fatSamples, _ ->
                val percentUnit = HKUnit.percentUnit()
                val rawFats = fatSamples?.filterIsInstance<HKQuantitySample>().orEmpty()

                val mappedWeights = rawWeights.map { sample ->
                    val millis = (sample.startDate.timeIntervalSince1970 * 1000.0).toLong()
                    val isoDate = epochMillisToLocalIsoDate(millis)
                    val kg = sample.quantity.doubleValueForUnit(kgUnit)
                    val sourceName = sample.sourceRevision.source.name.ifBlank { "Apple Health Tartı" }

                    val matchedFat = rawFats
                        .filter {
                            val fatMillis = (it.startDate.timeIntervalSince1970 * 1000.0).toLong()
                            abs(fatMillis - millis) <= 900_000L
                        }
                        .minByOrNull {
                            val fatMillis = (it.startDate.timeIntervalSince1970 * 1000.0).toLong()
                            abs(fatMillis - millis)
                        }
                        ?.quantity?.doubleValueForUnit(percentUnit)?.let { v ->
                            if (v <= 1.0) v * 100.0 else v
                        }

                    DeviceWeightRecord(
                        externalId = sample.UUID.UUIDString,
                        date = isoDate,
                        weightKg = kg,
                        bodyFatPercent = matchedFat,
                        leanBodyMassKg = null,
                        measuredAtMillis = millis,
                        sourcePackage = sample.sourceRevision.source.bundleIdentifier,
                        sourceName = sourceName
                    )
                }

                queryWorkouts(
                    store = store,
                    predicate = predicate,
                    workoutType = workoutType,
                    mappedWeights = mappedWeights,
                    onResult = onResult
                )
            }
        } else null

        if (fatQuery != null) {
            store.executeQuery(fatQuery)
        } else {
            val mappedWeights = rawWeights.map { sample ->
                val millis = (sample.startDate.timeIntervalSince1970 * 1000.0).toLong()
                val isoDate = epochMillisToLocalIsoDate(millis)
                val kg = sample.quantity.doubleValueForUnit(kgUnit)
                DeviceWeightRecord(
                    externalId = sample.UUID.UUIDString,
                    date = isoDate,
                    weightKg = kg,
                    measuredAtMillis = millis,
                    sourcePackage = sample.sourceRevision.source.bundleIdentifier,
                    sourceName = sample.sourceRevision.source.name.ifBlank { "Apple Health Tartı" }
                )
            }
            queryWorkouts(
                store = store,
                predicate = predicate,
                workoutType = workoutType,
                mappedWeights = mappedWeights,
                onResult = onResult
            )
        }
    }

    store.executeQuery(weightQuery)
}

private fun queryWorkouts(
    store: HKHealthStore,
    predicate: NSPredicate?,
    workoutType: HKWorkoutType,
    mappedWeights: List<DeviceWeightRecord>,
    onResult: (Result<DeviceHealthSnapshot>) -> Unit
) {
    val workoutQuery = HKSampleQuery(
        sampleType = workoutType,
        predicate = predicate,
        limit = 500u,
        sortDescriptors = null
    ) { _, samples, _ ->
        val kcalUnit = HKUnit.kilocalorieUnit()
        val meterUnit = HKUnit.meterUnit()
        val rawWorkouts = samples?.filterIsInstance<HKWorkout>().orEmpty()

        val mappedWorkouts = rawWorkouts.mapNotNull { workout ->
            val startMillis = (workout.startDate.timeIntervalSince1970 * 1000.0).toLong()
            val endMillis = (workout.endDate.timeIntervalSince1970 * 1000.0).toLong()
            val durationMinutes = (workout.duration / 60.0).roundToInt()
            if (durationMinutes <= 0) return@mapNotNull null

            val isoDate = epochMillisToLocalIsoDate(startMillis)
            val kcal = workout.totalEnergyBurned?.doubleValueForUnit(kcalUnit)?.roundToInt() ?: 0
            val meters = workout.totalDistance?.doubleValueForUnit(meterUnit)
            val sourceName = workout.sourceRevision.source.name.ifBlank { "Apple Health Saat" }

            DeviceWorkoutRecord(
                externalId = workout.UUID.UUIDString,
                date = isoDate,
                category = mapHealthKitWorkoutType(workout.workoutActivityType),
                customTitle = null,
                durationMinutes = durationMinutes,
                caloriesBurned = kcal,
                distanceMeters = meters,
                steps = null,
                avgHeartRateBpm = null,
                startTimeMillis = startMillis,
                endTimeMillis = endMillis,
                sourcePackage = workout.sourceRevision.source.bundleIdentifier,
                sourceName = sourceName
            )
        }

        onResult(
            Result.success(
                DeviceHealthSnapshot(
                    weights = mappedWeights,
                    workouts = mappedWorkouts,
                    platformName = "Apple Health"
                )
            )
        )
    }

    store.executeQuery(workoutQuery)
}

private fun epochMillisToLocalIsoDate(epochMillis: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        instant.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    } catch (_: Throwable) {
        getTodayIso()
    }
}

private fun mapHealthKitWorkoutType(type: HKWorkoutActivityType): DeviceExerciseCategory {
    return when (type) {
        HKWorkoutActivityTypeRunning -> DeviceExerciseCategory.RUNNING
        HKWorkoutActivityTypeWalking -> DeviceExerciseCategory.WALKING
        HKWorkoutActivityTypeCycling -> DeviceExerciseCategory.CYCLING
        HKWorkoutActivityTypeSwimming -> DeviceExerciseCategory.SWIMMING
        HKWorkoutActivityTypeTraditionalStrengthTraining,
        HKWorkoutActivityTypeFunctionalStrengthTraining,
        HKWorkoutActivityTypeCoreTraining -> DeviceExerciseCategory.STRENGTH
        HKWorkoutActivityTypeHighIntensityIntervalTraining,
        HKWorkoutActivityTypeCrossTraining -> DeviceExerciseCategory.HIIT
        HKWorkoutActivityTypeYoga,
        HKWorkoutActivityTypeMindAndBody -> DeviceExerciseCategory.YOGA
        HKWorkoutActivityTypePilates -> DeviceExerciseCategory.PILATES
        HKWorkoutActivityTypeRowing -> DeviceExerciseCategory.ROWING
        HKWorkoutActivityTypeElliptical -> DeviceExerciseCategory.ELLIPTICAL
        HKWorkoutActivityTypeHiking -> DeviceExerciseCategory.HIKING
        HKWorkoutActivityTypeBasketball -> DeviceExerciseCategory.BASKETBALL
        HKWorkoutActivityTypeSoccer -> DeviceExerciseCategory.FOOTBALL
        HKWorkoutActivityTypeTennis,
        HKWorkoutActivityTypeBadminton,
        HKWorkoutActivityTypeTableTennis -> DeviceExerciseCategory.TENNIS
        HKWorkoutActivityTypeBoxing,
        HKWorkoutActivityTypeMartialArts,
        HKWorkoutActivityTypeKickboxing -> DeviceExerciseCategory.MARTIAL_ARTS
        else -> DeviceExerciseCategory.OTHER
    }
}

