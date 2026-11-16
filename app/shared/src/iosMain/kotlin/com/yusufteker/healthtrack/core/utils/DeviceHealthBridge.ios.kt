package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.*
import platform.HealthKit.*
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MAX_SYNC_DAYS = 90
private const val WEIGHT_MATCH_WINDOW_MS = 900_000L // ±15 minutes
private const val STEP_BUCKET_MS = 900_000L // 15-minute deduplication window

private data class HealthKitSampleTypes(
    val bodyMass: HKQuantityType? = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBodyMass),
    val bodyFat: HKQuantityType? = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBodyFatPercentage),
    val leanMass: HKQuantityType? = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierLeanBodyMass),
    val stepCount: HKQuantityType? = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierStepCount),
    val distance: HKQuantityType? = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierDistanceWalkingRunning),
    val activeEnergy: HKQuantityType? = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierActiveEnergyBurned),
    val heartRate: HKQuantityType? = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierHeartRate),
    val workout: HKWorkoutType = HKObjectType.workoutType()
) {
    fun toReadSet(): Set<HKObjectType> = buildSet {
        bodyMass?.let(::add)
        bodyFat?.let(::add)
        leanMass?.let(::add)
        stepCount?.let(::add)
        distance?.let(::add)
        activeEnergy?.let(::add)
        heartRate?.let(::add)
        add(workout)
    }
}

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
                            IllegalStateException(
                                trEn(
                                    "Cihazınızda Apple HealthKit desteği bulunmuyor.",
                                    "Apple HealthKit is not available on this device."
                                )
                            )
                        )
                    )
                    return
                }

                val types = HealthKitSampleTypes()
                val readTypes = types.toReadSet()

                store.requestAuthorizationToShareTypes(
                    typesToShare = null,
                    readTypes = readTypes
                ) { success, error ->
                    if (!success || error != null) {
                        val msg = error?.localizedDescription ?: trEn(
                            "Apple Sağlık okuma izni reddedildi.",
                            "Apple Health read permission was denied."
                        )
                        onResult(Result.failure(IllegalStateException(msg)))
                        return@requestAuthorizationToShareTypes
                    }

                    executeHealthKitSync(
                        store = store,
                        days = days,
                        types = types,
                        onResult = onResult
                    )
                }
            }

            override fun syncSilentlyIfAuthorized(
                days: Int,
                onResult: (Result<DeviceHealthSnapshot>) -> Unit
            ) {
                val store = healthStore ?: return
                if (status != HealthPlatformStatus.AVAILABLE) return

                val types = HealthKitSampleTypes()
                val readTypes = types.toReadSet()

                store.getRequestStatusForAuthorizationToShareTypes(
                    typesToShare = emptySet<HKSampleType>(),
                    readTypes = readTypes
                ) { requestStatus, error ->
                    if (error == null && requestStatus == HKAuthorizationRequestStatusUnnecessary) {
                        executeHealthKitSync(
                            store = store,
                            days = days,
                            types = types,
                            onResult = onResult
                        )
                    }
                }
            }

            override suspend fun isAuthorized(): Boolean {
                val store = healthStore ?: return false
                if (status != HealthPlatformStatus.AVAILABLE) return false
                val readTypes = HealthKitSampleTypes().toReadSet()
                return suspendCancellableCoroutine { continuation ->
                    store.getRequestStatusForAuthorizationToShareTypes(
                        typesToShare = emptySet<HKSampleType>(),
                        readTypes = readTypes
                    ) { requestStatus, error ->
                        if (continuation.isActive) {
                            continuation.resume(error == null && requestStatus == HKAuthorizationRequestStatusUnnecessary)
                        }
                    }
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

private fun executeHealthKitSync(
    store: HKHealthStore,
    days: Int,
    types: HealthKitSampleTypes,
    onResult: (Result<DeviceHealthSnapshot>) -> Unit
) {
    val nowDate = NSDate()
    val secondsBack = -(days.coerceIn(1, MAX_SYNC_DAYS).toDouble() * 86_400.0)
    val startDate = nowDate.dateByAddingTimeInterval(secondsBack)
    val predicate = HKQuery.predicateForSamplesWithStartDate(
        startDate = startDate,
        endDate = nowDate,
        options = HKQueryOptionNone
    )

    queryQuantitySamples(store, types.bodyMass, predicate, 1000u) { weightSamples ->
        queryQuantitySamples(store, types.bodyFat, predicate, 1000u) { fatSamples ->
            queryQuantitySamples(store, types.leanMass, predicate, 1000u) { leanMassSamples ->
                queryQuantitySamples(store, types.stepCount, predicate, 10000u) { stepSamples ->
                    queryQuantitySamples(store, types.distance, predicate, 5000u) { distanceSamples ->
                        queryQuantitySamples(store, types.activeEnergy, predicate, 5000u) { activeEnergySamples ->
                            queryQuantitySamples(store, types.heartRate, predicate, 5000u) { heartRateSamples ->
                                queryWorkoutSamples(store, types.workout, predicate, 1000u) { workoutSamples ->
                                    val mappedWeights = mapWeightSamples(
                                        weightSamples = weightSamples,
                                        fatSamples = fatSamples,
                                        leanMassSamples = leanMassSamples
                                    )
                                    val mappedWorkouts = mapWorkoutSamples(
                                        workoutSamples = workoutSamples,
                                        stepSamples = stepSamples,
                                        distanceSamples = distanceSamples,
                                        activeEnergySamples = activeEnergySamples,
                                        heartRateSamples = heartRateSamples
                                    )
                                    val dailyStepWorkouts = mapDailyStepWorkouts(
                                        stepSamples = stepSamples,
                                        distanceSamples = distanceSamples,
                                        activeEnergySamples = activeEnergySamples,
                                        heartRateSamples = heartRateSamples,
                                        mappedWorkouts = mappedWorkouts
                                    )

                                    onResult(
                                        Result.success(
                                            DeviceHealthSnapshot(
                                                weights = mappedWeights,
                                                workouts = mappedWorkouts + dailyStepWorkouts,
                                                platformName = "Apple Health"
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun queryQuantitySamples(
    store: HKHealthStore,
    quantityType: HKQuantityType?,
    predicate: NSPredicate?,
    limit: ULong,
    onComplete: (List<HKQuantitySample>) -> Unit
) {
    if (quantityType == null) {
        onComplete(emptyList())
        return
    }
    val sortDescriptors = listOf(NSSortDescriptor(key = HKSampleSortIdentifierStartDate, ascending = false))
    val query = HKSampleQuery(
        sampleType = quantityType,
        predicate = predicate,
        limit = limit,
        sortDescriptors = sortDescriptors
    ) { _, samples, _ ->
        onComplete(samples?.filterIsInstance<HKQuantitySample>().orEmpty())
    }
    store.executeQuery(query)
}

private fun queryWorkoutSamples(
    store: HKHealthStore,
    workoutType: HKWorkoutType,
    predicate: NSPredicate?,
    limit: ULong,
    onComplete: (List<HKWorkout>) -> Unit
) {
    val sortDescriptors = listOf(NSSortDescriptor(key = HKSampleSortIdentifierStartDate, ascending = false))
    val query = HKSampleQuery(
        sampleType = workoutType,
        predicate = predicate,
        limit = limit,
        sortDescriptors = sortDescriptors
    ) { _, samples, _ ->
        onComplete(samples?.filterIsInstance<HKWorkout>().orEmpty())
    }
    store.executeQuery(query)
}

@OptIn(ExperimentalForeignApi::class)
private fun mapWeightSamples(
    weightSamples: List<HKQuantitySample>,
    fatSamples: List<HKQuantitySample>,
    leanMassSamples: List<HKQuantitySample>
): List<DeviceWeightRecord> {
    if (weightSamples.isEmpty()) return emptyList()
    val kgUnit = HKUnit.gramUnitWithMetricPrefix(HKMetricPrefixKilo)
    val percentUnit = HKUnit.percentUnit()

    return weightSamples.map { sample ->
        val millis = sample.startMillis()
        val isoDate = epochMillisToLocalIsoDate(millis)
        val kg = sample.quantity.doubleValueForUnit(kgUnit)

        val matchedFat = findClosestQuantitySample(fatSamples, millis)
            ?.quantity?.doubleValueForUnit(percentUnit)
            ?.let { raw -> if (raw <= 1.0) raw * 100.0 else raw }

        val matchedLeanMass = findClosestQuantitySample(leanMassSamples, millis)
            ?.quantity?.doubleValueForUnit(kgUnit)

        val bundleId = sample.sourceRevision.source.bundleIdentifier
        val rawName = sample.sourceRevision.source.name
        val productType = sample.sourceRevision.productType

        DeviceWeightRecord(
            externalId = sample.UUID.UUIDString,
            date = isoDate,
            weightKg = kg,
            bodyFatPercent = matchedFat,
            leanBodyMassKg = matchedLeanMass,
            measuredAtMillis = millis,
            sourcePackage = bundleId,
            sourceName = resolveIosFriendlySourceName(bundleId, rawName, productType, isScale = true)
        )
    }
}

private fun findClosestQuantitySample(
    samples: List<HKQuantitySample>,
    targetMillis: Long
): HKQuantitySample? {
    return samples
        .filter { abs(it.startMillis() - targetMillis) <= WEIGHT_MATCH_WINDOW_MS }
        .minByOrNull { abs(it.startMillis() - targetMillis) }
}

private fun mapWorkoutSamples(
    workoutSamples: List<HKWorkout>,
    stepSamples: List<HKQuantitySample>,
    distanceSamples: List<HKQuantitySample>,
    activeEnergySamples: List<HKQuantitySample>,
    heartRateSamples: List<HKQuantitySample>
): List<DeviceWorkoutRecord> {
    if (workoutSamples.isEmpty()) return emptyList()

    val kcalUnit = HKUnit.kilocalorieUnit()
    val meterUnit = HKUnit.meterUnit()
    val countUnit = HKUnit.countUnit()
    val bpmUnit = HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit())

    return workoutSamples.mapNotNull { workout ->
        val startMillis = workout.startMillis()
        val endMillis = workout.endMillis()
        val durationMinutes = (workout.duration / 60.0).roundToInt()
        if (durationMinutes <= 0) return@mapNotNull null

        val isoDate = epochMillisToLocalIsoDate(startMillis)
        val bundleId = workout.sourceRevision.source.bundleIdentifier
        val rawName = workout.sourceRevision.source.name
        val productType = workout.sourceRevision.productType
        val friendlySourceName = resolveIosFriendlySourceName(bundleId, rawName, productType, isScale = false)

        // Calories: prefer HKWorkout.totalEnergyBurned, fallback to overlapping activeEnergySamples
        val directKcal = workout.totalEnergyBurned?.doubleValueForUnit(kcalUnit)?.roundToInt() ?: 0
        val kcal = if (directKcal > 0) {
            directKcal
        } else {
            val overlappingEnergy = filterSamplesInWindow(activeEnergySamples, startMillis, endMillis, bundleId)
            overlappingEnergy.sumOf { it.quantity.doubleValueForUnit(kcalUnit) }.roundToInt()
        }

        // Distance: prefer HKWorkout.totalDistance, fallback to overlapping distanceSamples
        val directMeters = workout.totalDistance?.doubleValueForUnit(meterUnit)?.takeIf { it > 0.0 }
        val meters = directMeters ?: filterSamplesInWindow(distanceSamples, startMillis, endMillis, bundleId)
            .takeIf { it.isNotEmpty() }
            ?.sumOf { it.quantity.doubleValueForUnit(meterUnit) }
            ?.takeIf { it > 0.0 }

        // Steps attributed to this explicit workout session
        val workoutSteps = filterSamplesInWindow(stepSamples, startMillis, endMillis, bundleId)
            .sumOf { it.quantity.doubleValueForUnit(countUnit) }
            .roundToInt()
            .takeIf { it > 0 }

        // Average Heart Rate during this workout session
        val hrValues = filterSamplesInWindow(heartRateSamples, startMillis, endMillis, bundleId)
            .map { it.quantity.doubleValueForUnit(bpmUnit) }
            .filter { it in 35.0..240.0 }
        val avgBpm = hrValues.takeIf { it.isNotEmpty() }?.average()?.roundToInt()

        val brandTitle = (workout.metadata?.get(HKMetadataKeyWorkoutBrandName) as? String)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        DeviceWorkoutRecord(
            externalId = workout.UUID.UUIDString,
            date = isoDate,
            category = mapHealthKitWorkoutType(workout.workoutActivityType),
            customTitle = brandTitle,
            durationMinutes = durationMinutes,
            caloriesBurned = kcal,
            distanceMeters = meters,
            steps = workoutSteps,
            avgHeartRateBpm = avgBpm,
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            sourcePackage = bundleId,
            sourceName = friendlySourceName
        )
    }
}

private fun filterSamplesInWindow(
    samples: List<HKQuantitySample>,
    startMillis: Long,
    endMillis: Long,
    preferredBundleId: String
): List<HKQuantitySample> {
    val overlapping = samples.filter { sample ->
        val sStart = sample.startMillis()
        val sEnd = sample.endMillis()
        sEnd >= startMillis && sStart <= endMillis
    }
    if (overlapping.isEmpty()) return emptyList()

    val matchingBundle = overlapping.filter { it.sourceRevision.source.bundleIdentifier == preferredBundleId }
    if (matchingBundle.isNotEmpty()) return matchingBundle

    val bestBundle = pickPreferredBundleId(overlapping)
    return overlapping.filter { it.sourceRevision.source.bundleIdentifier == bestBundle }
}

private fun mapDailyStepWorkouts(
    stepSamples: List<HKQuantitySample>,
    distanceSamples: List<HKQuantitySample>,
    activeEnergySamples: List<HKQuantitySample>,
    heartRateSamples: List<HKQuantitySample>,
    mappedWorkouts: List<DeviceWorkoutRecord>
): List<DeviceWorkoutRecord> {
    if (stepSamples.isEmpty()) return emptyList()

    val countUnit = HKUnit.countUnit()
    val meterUnit = HKUnit.meterUnit()
    val kcalUnit = HKUnit.kilocalorieUnit()
    val bpmUnit = HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit())

    val stepsByDate = stepSamples.groupBy { epochMillisToLocalIsoDate(it.startMillis()) }
    val distanceByDate = distanceSamples.groupBy { epochMillisToLocalIsoDate(it.startMillis()) }
    val energyByDate = activeEnergySamples.groupBy { epochMillisToLocalIsoDate(it.startMillis()) }
    val heartRateByDate = heartRateSamples.groupBy { epochMillisToLocalIsoDate(it.startMillis()) }

    return stepsByDate.entries
        .sortedByDescending { it.key }
        .mapNotNull { (isoDate, dayStepSamples) ->
            val preferredBundleId = pickPreferredBundleId(dayStepSamples)
            val totalDaySteps = aggregateDeduplicatedQuantity(
                samples = dayStepSamples,
                preferredBundleId = preferredBundleId,
                unit = countUnit
            ).roundToInt()

            val exerciseStepsOnDay = mappedWorkouts
                .filter { it.date == isoDate }
                .sumOf { it.steps ?: 0 }

            val remainingSteps = (totalDaySteps - exerciseStepsOnDay).coerceAtLeast(0)
            if (remainingSteps < 20) return@mapNotNull null

            val dayDistanceSamples = distanceByDate[isoDate].orEmpty()
            val totalDayDistance = if (dayDistanceSamples.isNotEmpty()) {
                val distBundle = pickPreferredBundleId(dayDistanceSamples, preferredBundleId)
                aggregateDeduplicatedQuantity(dayDistanceSamples, distBundle, meterUnit)
            } else null

            val exerciseDistanceOnDay = mappedWorkouts
                .filter { it.date == isoDate }
                .sumOf { it.distanceMeters ?: 0.0 }

            val remainingDistanceMeters = totalDayDistance
                ?.let { (it - exerciseDistanceOnDay).coerceAtLeast(0.0) }
                ?.takeIf { it >= 15.0 }
                ?: (remainingSteps * 0.75)

            val dayEnergySamples = energyByDate[isoDate].orEmpty()
            val totalDayActiveKcal = if (dayEnergySamples.isNotEmpty()) {
                val energyBundle = pickPreferredBundleId(dayEnergySamples, preferredBundleId)
                aggregateDeduplicatedQuantity(dayEnergySamples, energyBundle, kcalUnit).roundToInt()
            } else 0

            val exerciseKcalOnDay = mappedWorkouts
                .filter { it.date == isoDate }
                .sumOf { it.caloriesBurned }

            val remainingActiveKcal = (totalDayActiveKcal - exerciseKcalOnDay)
                .takeIf { it in 5..2500 } ?: 0

            val dayHrSamples = heartRateByDate[isoDate].orEmpty()
            val dayHrBundle = if (dayHrSamples.isNotEmpty()) pickPreferredBundleId(dayHrSamples, preferredBundleId) else ""
            val dayAvgBpm = dayHrSamples
                .filter { dayHrBundle.isBlank() || it.sourceRevision.source.bundleIdentifier == dayHrBundle }
                .map { it.quantity.doubleValueForUnit(bpmUnit) }
                .filter { it in 35.0..240.0 }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.roundToInt()

            val estimatedDurationMinutes = (remainingSteps / 100).coerceAtLeast(1)
            val dayStartMillis = dayStepSamples.minOf { it.startMillis() }
            val dayEndMillis = dayStepSamples.maxOf { it.endMillis() }
            val representativeSample = dayStepSamples.firstOrNull {
                it.sourceRevision.source.bundleIdentifier == preferredBundleId
            } ?: dayStepSamples.first()

            DeviceWorkoutRecord(
                externalId = "hk_steps_$isoDate",
                date = isoDate,
                category = DeviceExerciseCategory.DAILY_STEPS,
                customTitle = trEn(
                    "$remainingSteps Adım • Günlük Hareket",
                    "$remainingSteps Steps • Daily Movement"
                ),
                durationMinutes = estimatedDurationMinutes,
                caloriesBurned = remainingActiveKcal, // 0 triggers weight-adjusted step calorie calculation in DeviceHealthSyncManager
                distanceMeters = remainingDistanceMeters,
                steps = remainingSteps,
                avgHeartRateBpm = dayAvgBpm,
                startTimeMillis = dayStartMillis,
                endTimeMillis = dayEndMillis,
                sourcePackage = preferredBundleId,
                sourceName = resolveIosFriendlySourceName(
                    bundleId = preferredBundleId,
                    rawSourceName = representativeSample.sourceRevision.source.name,
                    productType = representativeSample.sourceRevision.productType,
                    isScale = false
                )
            )
        }
}

private fun aggregateDeduplicatedQuantity(
    samples: List<HKQuantitySample>,
    preferredBundleId: String,
    unit: HKUnit
): Double {
    if (samples.isEmpty()) return 0.0

    val preferredTotal = samples
        .filter { it.sourceRevision.source.bundleIdentifier == preferredBundleId }
        .sumOf { it.quantity.doubleValueForUnit(unit) }

    // Also compute 15-minute bucket deduplicated sum so daytime iPhone steps can fill
    // gaps when the watch is charging, without ever double-counting overlapping buckets.
    val bucketedTotal = samples
        .groupBy { it.startMillis() / STEP_BUCKET_MS }
        .values
        .sumOf { bucketSamples ->
            val winningBundle = pickPreferredBundleId(bucketSamples, preferredBundleId)
            bucketSamples
                .filter { it.sourceRevision.source.bundleIdentifier == winningBundle }
                .sumOf { it.quantity.doubleValueForUnit(unit) }
        }

    return maxOf(preferredTotal, bucketedTotal)
}

private fun pickPreferredBundleId(
    samples: List<HKQuantitySample>,
    hintBundleId: String? = null
): String {
    if (hintBundleId != null && samples.any { it.sourceRevision.source.bundleIdentifier == hintBundleId }) {
        val hintScore = samples
            .first { it.sourceRevision.source.bundleIdentifier == hintBundleId }
            .let { scoreIosHealthSource(it.sourceRevision.source.bundleIdentifier, it.sourceRevision.source.name, it.sourceRevision.productType) }
        val maxOtherScore = samples.maxOf {
            scoreIosHealthSource(it.sourceRevision.source.bundleIdentifier, it.sourceRevision.source.name, it.sourceRevision.productType)
        }
        if (hintScore >= maxOtherScore) return hintBundleId
    }

    return samples
        .maxByOrNull { sample ->
            scoreIosHealthSource(
                bundleId = sample.sourceRevision.source.bundleIdentifier,
                rawSourceName = sample.sourceRevision.source.name,
                productType = sample.sourceRevision.productType
            )
        }
        ?.sourceRevision?.source?.bundleIdentifier
        .orEmpty()
}

private fun scoreIosHealthSource(
    bundleId: String,
    rawSourceName: String,
    productType: String?
): Int {
    val pkg = bundleId.lowercase()
    val name = rawSourceName.lowercase()
    val hw = productType.orEmpty().lowercase()

    return when {
        pkg.contains("huawei") || name.contains("huawei") -> 100
        hw.contains("watch") || name.contains("apple watch") -> 95
        pkg.contains("xiaomi") || pkg.contains("huami") || pkg.contains("zepp") || pkg.contains("mifitness") ||
            name.contains("zepp") || name.contains("xiaomi") || name.contains("mi fitness") -> 90
        pkg.contains("garmin") || pkg.contains("samsung") || pkg.contains("fitbit") || pkg.contains("polar") ||
            pkg.contains("withings") || pkg.contains("suunto") || pkg.contains("coros") || pkg.contains("ouraring") ||
            name.contains("garmin") || name.contains("fitbit") || name.contains("polar") || name.contains("withings") -> 85
        !pkg.startsWith("com.apple.") -> 60
        else -> 10 // Built-in iPhone pedometer / generic Apple Health fallback
    }
}

private fun resolveIosFriendlySourceName(
    bundleId: String,
    rawSourceName: String,
    productType: String?,
    isScale: Boolean
): String {
    val pkg = bundleId.lowercase()
    val name = rawSourceName.lowercase()
    val hw = productType.orEmpty().lowercase()

    return when {
        pkg.contains("huawei") || name.contains("huawei") ->
            "Huawei Health"
        pkg.contains("huami") || pkg.contains("zepp") || name.contains("zepp") ->
            if (isScale) trEn("Zepp Life (Xiaomi Tartı)", "Zepp Life (Xiaomi Scale)") else "Zepp Life (Xiaomi)"
        pkg.contains("xiaomi") || pkg.contains("mifitness") || pkg.contains("mihealth") || name.contains("mi fitness") || name.contains("xiaomi") ->
            if (isScale) trEn("Mi Fitness (Xiaomi Tartı)", "Mi Fitness (Xiaomi Scale)") else "Mi Fitness (Xiaomi)"
        pkg.contains("garmin") || name.contains("garmin") ->
            "Garmin Connect"
        pkg.contains("samsung") || name.contains("samsung") ->
            "Samsung Health"
        pkg.contains("withings") || name.contains("withings") ->
            "Withings"
        pkg.contains("fitbit") || name.contains("fitbit") ->
            "Fitbit"
        pkg.contains("polar") || name.contains("polar") ->
            "Polar Flow"
        pkg.contains("suunto") || name.contains("suunto") ->
            "Suunto"
        pkg.contains("eufy") || name.contains("eufy") ->
            "EufyLife"
        pkg.contains("renpho") || name.contains("renpho") ->
            "Renpho"
        hw.contains("watch") || name.contains("apple watch") ->
            "Apple Watch"
        rawSourceName.isNotBlank() ->
            rawSourceName
        else ->
            if (isScale) trEn("Apple Health Tartı", "Apple Health Scale") else trEn("Apple Health Saat", "Apple Health Watch")
    }
}

private fun HKSample.startMillis(): Long = (startDate.timeIntervalSince1970 * 1000.0).toLong()
private fun HKSample.endMillis(): Long = (endDate.timeIntervalSince1970 * 1000.0).toLong()

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
        HKWorkoutActivityTypeRunning,
        HKWorkoutActivityTypeTrackAndField -> DeviceExerciseCategory.RUNNING

        HKWorkoutActivityTypeWalking -> DeviceExerciseCategory.WALKING

        HKWorkoutActivityTypeCycling,
        HKWorkoutActivityTypeHandCycling -> DeviceExerciseCategory.CYCLING

        HKWorkoutActivityTypeSwimming,
        HKWorkoutActivityTypeWaterFitness,
        HKWorkoutActivityTypeWaterPolo,
        HKWorkoutActivityTypeSurfingSports -> DeviceExerciseCategory.SWIMMING

        HKWorkoutActivityTypeTraditionalStrengthTraining,
        HKWorkoutActivityTypeFunctionalStrengthTraining,
        HKWorkoutActivityTypeCoreTraining -> DeviceExerciseCategory.STRENGTH

        HKWorkoutActivityTypeGymnastics -> DeviceExerciseCategory.CALISTHENICS

        HKWorkoutActivityTypeHighIntensityIntervalTraining,
        HKWorkoutActivityTypeCrossTraining,
        HKWorkoutActivityTypeMixedCardio,
        HKWorkoutActivityTypeJumpRope,
        HKWorkoutActivityTypeStairClimbing,
        HKWorkoutActivityTypeStepTraining,
        HKWorkoutActivityTypeFitnessGaming -> DeviceExerciseCategory.HIIT

        HKWorkoutActivityTypeYoga,
        HKWorkoutActivityTypeMindAndBody,
        HKWorkoutActivityTypeFlexibility,
        HKWorkoutActivityTypePreparationAndRecovery,
        HKWorkoutActivityTypeCooldown,
        HKWorkoutActivityTypeTaiChi -> DeviceExerciseCategory.YOGA

        HKWorkoutActivityTypePilates,
        HKWorkoutActivityTypeBarre -> DeviceExerciseCategory.PILATES

        HKWorkoutActivityTypeRowing,
        HKWorkoutActivityTypePaddleSports -> DeviceExerciseCategory.ROWING

        HKWorkoutActivityTypeElliptical -> DeviceExerciseCategory.ELLIPTICAL

        HKWorkoutActivityTypeHiking,
        HKWorkoutActivityTypeClimbing -> DeviceExerciseCategory.HIKING

        HKWorkoutActivityTypeBasketball,
        HKWorkoutActivityTypeHandball -> DeviceExerciseCategory.BASKETBALL

        HKWorkoutActivityTypeVolleyball -> DeviceExerciseCategory.VOLLEYBALL

        HKWorkoutActivityTypeSoccer,
        HKWorkoutActivityTypeAmericanFootball,
        HKWorkoutActivityTypeAustralianFootball,
        HKWorkoutActivityTypeRugby -> DeviceExerciseCategory.FOOTBALL

        HKWorkoutActivityTypeTennis,
        HKWorkoutActivityTypeBadminton,
        HKWorkoutActivityTypeTableTennis,
        HKWorkoutActivityTypeRacquetball,
        HKWorkoutActivityTypeSquash,
        HKWorkoutActivityTypePickleball -> DeviceExerciseCategory.TENNIS

        HKWorkoutActivityTypeCardioDance,
        HKWorkoutActivityTypeSocialDance -> DeviceExerciseCategory.DANCE

        HKWorkoutActivityTypeBoxing,
        HKWorkoutActivityTypeMartialArts,
        HKWorkoutActivityTypeKickboxing,
        HKWorkoutActivityTypeWrestling,
        HKWorkoutActivityTypeFencing -> DeviceExerciseCategory.MARTIAL_ARTS

        else -> DeviceExerciseCategory.OTHER
    }
}
