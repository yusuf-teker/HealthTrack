package com.yusufteker.healthtrack.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.yusufteker.healthtrack.core.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

private val REQUIRED_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(BodyFatRecord::class),
    HealthPermission.getReadPermission(LeanBodyMassRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class)
)

private val CORE_READ_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class)
)

@Composable
actual fun rememberDeviceHealthBridge(): DeviceHealthBridge {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pendingDays by remember { mutableStateOf(30) }
    var pendingCallback by remember { mutableStateOf<((Result<DeviceHealthSnapshot>) -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        val callback = pendingCallback
        pendingCallback = null
        if (callback == null) return@rememberLauncherForActivityResult

        val hasAnyCorePermission = grantedPermissions.any { it in CORE_READ_PERMISSIONS }
        if (!hasAnyCorePermission) {
            callback(
                Result.failure(
                    IllegalStateException(
                        "Health Connect okuma izni verilmedi. Lütfen Kilo veya Egzersiz okuma izinlerini onaylayın."
                    )
                )
            )
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            val result = readHealthConnectData(context, pendingDays, grantedPermissions)
            callback(result)
        }
    }

    return remember(context, permissionLauncher, coroutineScope) {
        object : DeviceHealthBridge {
            override val platformName: String = "Health Connect"

            override val status: HealthPlatformStatus
                get() = when (HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)) {
                    HealthConnectClient.SDK_AVAILABLE -> HealthPlatformStatus.AVAILABLE
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthPlatformStatus.NOT_INSTALLED
                    else -> HealthPlatformStatus.NOT_SUPPORTED
                }

            override fun syncLastDays(
                days: Int,
                onResult: (Result<DeviceHealthSnapshot>) -> Unit
            ) {
                val currentStatus = status
                if (currentStatus == HealthPlatformStatus.NOT_INSTALLED) {
                    openHealthConnectStorePage(context)
                    onResult(
                        Result.failure(
                            IllegalStateException(
                                "Health Connect uygulaması yüklü veya güncel değil. Play Store sayfası açıldı."
                            )
                        )
                    )
                    return
                }
                if (currentStatus == HealthPlatformStatus.NOT_SUPPORTED) {
                    onResult(
                        Result.failure(
                            IllegalStateException("Cihazınız Android Health Connect özelliğini desteklemiyor.")
                        )
                    )
                    return
                }

                coroutineScope.launch {
                    try {
                        val client = HealthConnectClient.getOrCreate(context)
                        val granted = client.permissionController.getGrantedPermissions()
                        val hasAllCore = granted.containsAll(CORE_READ_PERMISSIONS)

                        if (hasAllCore) {
                            val result = readHealthConnectData(context, days, granted)
                            onResult(result)
                        } else {
                            pendingDays = days
                            pendingCallback = onResult
                            permissionLauncher.launch(REQUIRED_PERMISSIONS)
                        }
                    } catch (e: Exception) {
                        Napier.e("Health Connect başlatma hatası: ${e.message}", e, tag = "DEVICE_SYNC")
                        onResult(Result.failure(e))
                    }
                }
            }

            override fun syncSilentlyIfAuthorized(
                days: Int,
                onResult: (Result<DeviceHealthSnapshot>) -> Unit
            ) {
                if (status != HealthPlatformStatus.AVAILABLE) return
                coroutineScope.launch {
                    try {
                        val client = HealthConnectClient.getOrCreate(context)
                        val granted = client.permissionController.getGrantedPermissions()
                        val hasAnyCore = granted.any { it in CORE_READ_PERMISSIONS }
                        if (hasAnyCore) {
                            val result = readHealthConnectData(context, days, granted)
                            onResult(result)
                        }
                    } catch (e: Exception) {
                        Napier.w("Silent Health Connect sync skipped: ${e.message}", e, tag = "DEVICE_SYNC")
                    }
                }
            }

            override fun openHealthSettings() {
                try {
                    val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    openHealthConnectStorePage(context)
                }
            }
        }
    }
}

private fun openHealthConnectStorePage(context: Context) {
    try {
        val uri = Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.android.vending")
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {}
    }
}

private suspend fun readHealthConnectData(
    context: Context,
    days: Int,
    grantedPermissions: Set<String>
): Result<DeviceHealthSnapshot> = withContext(Dispatchers.IO) {
    runCatching {
        val client = HealthConnectClient.getOrCreate(context)
        val endInstant = Instant.now()
        val startInstant = endInstant.minus(days.toLong().coerceIn(1L, 90L), ChronoUnit.DAYS)
        val timeRange = TimeRangeFilter.between(startInstant, endInstant)
        val zoneId = ZoneId.systemDefault()
        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        // ── 1. Read Weight, BodyFat & LeanBodyMass (Xiaomi Scale / Zepp Life / Mi Fitness) ──
        val weightRecords = if (grantedPermissions.contains(HealthPermission.getReadPermission(WeightRecord::class))) {
            runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = timeRange,
                        ascendingOrder = false,
                        pageSize = 1000
                    )
                ).records
            }.getOrDefault(emptyList())
        } else emptyList()

        val bodyFatRecords = if (grantedPermissions.contains(HealthPermission.getReadPermission(BodyFatRecord::class))) {
            runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = BodyFatRecord::class,
                        timeRangeFilter = timeRange,
                        ascendingOrder = false,
                        pageSize = 1000
                    )
                ).records
            }.getOrDefault(emptyList())
        } else emptyList()

        val leanMassRecords = if (grantedPermissions.contains(HealthPermission.getReadPermission(LeanBodyMassRecord::class))) {
            runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = LeanBodyMassRecord::class,
                        timeRangeFilter = timeRange,
                        ascendingOrder = false,
                        pageSize = 1000
                    )
                ).records
            }.getOrDefault(emptyList())
        } else emptyList()

        val mappedWeights = weightRecords.map { record ->
            val recordMillis = record.time.toEpochMilli()
            val localDate = record.time.atZone(zoneId).toLocalDate().format(isoFormatter)
            val pkg = record.metadata.dataOrigin.packageName

            // Match closest BodyFatRecord & LeanBodyMassRecord within ±15 minutes (900_000 ms)
            val matchedBodyFat = bodyFatRecords
                .filter { abs(it.time.toEpochMilli() - recordMillis) <= 900_000L }
                .minByOrNull { abs(it.time.toEpochMilli() - recordMillis) }
                ?.percentage?.value

            val matchedLeanMass = leanMassRecords
                .filter { abs(it.time.toEpochMilli() - recordMillis) <= 900_000L }
                .minByOrNull { abs(it.time.toEpochMilli() - recordMillis) }
                ?.mass?.inKilograms

            DeviceWeightRecord(
                externalId = record.metadata.id.ifBlank { "hc_w_$recordMillis" },
                date = localDate,
                weightKg = record.weight.inKilograms,
                bodyFatPercent = matchedBodyFat,
                leanBodyMassKg = matchedLeanMass,
                measuredAtMillis = recordMillis,
                sourcePackage = pkg,
                sourceName = resolveFriendlySourceName(pkg, isScale = true)
            )
        }

        // ── 2. Read Exercise Sessions & Associated Telemetry (Huawei Watch / Mi Fitness / WearOS) ──
        val exerciseSessions = if (grantedPermissions.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))) {
            runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = timeRange,
                        ascendingOrder = false,
                        pageSize = 1000
                    )
                ).records
            }.getOrDefault(emptyList())
        } else emptyList()

        val mappedWorkouts = exerciseSessions.mapNotNull { session ->
            val startMillis = session.startTime.toEpochMilli()
            val endMillis = session.endTime.toEpochMilli()
            val durationMinutes = Duration.between(session.startTime, session.endTime).toMinutes().toInt()
            if (durationMinutes <= 0) return@mapNotNull null

            val localDate = session.startTime.atZone(zoneId).toLocalDate().format(isoFormatter)
            val pkg = session.metadata.dataOrigin.packageName
            val sessionRange = TimeRangeFilter.between(session.startTime, session.endTime)

            // Aggregate active or total calories burned during this workout window
            val activeKcal = runCatching {
                val agg = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = sessionRange
                    )
                )
                agg[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.roundToInt()
            }.getOrNull() ?: 0

            val totalKcal = if (activeKcal <= 0) {
                runCatching {
                    val agg = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                            timeRangeFilter = sessionRange
                        )
                    )
                    agg[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.roundToInt()
                }.getOrNull() ?: 0
            } else 0

            val distanceMeters = runCatching {
                val agg = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = sessionRange
                    )
                )
                agg[DistanceRecord.DISTANCE_TOTAL]?.inMeters
            }.getOrNull()

            val stepsCount = runCatching {
                val agg = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = sessionRange
                    )
                )
                agg[StepsRecord.COUNT_TOTAL]?.toInt()
            }.getOrNull()

            val avgBpm = runCatching {
                val agg = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(HeartRateRecord.BPM_AVG),
                        timeRangeFilter = sessionRange
                    )
                )
                agg[HeartRateRecord.BPM_AVG]?.toInt()
            }.getOrNull()

            DeviceWorkoutRecord(
                externalId = session.metadata.id.ifBlank { "hc_ex_$startMillis" },
                date = localDate,
                category = mapHealthConnectExerciseType(session.exerciseType),
                customTitle = session.title?.takeIf { it.isNotBlank() },
                durationMinutes = durationMinutes,
                caloriesBurned = if (activeKcal > 0) activeKcal else totalKcal,
                distanceMeters = distanceMeters,
                steps = stepsCount,
                avgHeartRateBpm = avgBpm,
                startTimeMillis = startMillis,
                endTimeMillis = endMillis,
                sourcePackage = pkg,
                sourceName = resolveFriendlySourceName(pkg, isScale = false)
            )
        }

        // ── 3. Read Daily Steps & Movement Day-by-Day (Today Backwards — Prevents 1000-Record Page Truncation) ──
        val todayLocalDate = endInstant.atZone(zoneId).toLocalDate()
        val maxDays = days.coerceIn(1, 90)
        val dailyStepWorkouts = if (grantedPermissions.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
            (0..maxDays).mapNotNull { dayOffset ->
                val localDate = todayLocalDate.minusDays(dayOffset.toLong())
                val isoDate = localDate.format(isoFormatter)
                val dayStartInstant = localDate.atStartOfDay(zoneId).toInstant()
                val dayEndInstant = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().coerceAtMost(endInstant)
                if (!dayEndInstant.isAfter(dayStartInstant)) return@mapNotNull null
                val dayRange = TimeRangeFilter.between(dayStartInstant, dayEndInstant)

                val dayStepRecords = runCatching {
                    client.readRecords(
                        ReadRecordsRequest(
                            recordType = StepsRecord::class,
                            timeRangeFilter = dayRange,
                            ascendingOrder = false,
                            pageSize = 5000
                        )
                    ).records
                }.getOrDefault(emptyList())

                // Use Health Connect's deduplicated daily total if available, else sum records by preferred source
                val aggregatedSteps = runCatching {
                    val agg = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = dayRange
                        )
                    )
                    agg[StepsRecord.COUNT_TOTAL]?.toInt()
                }.getOrNull() ?: 0

                if (dayStepRecords.isEmpty() && aggregatedSteps <= 0) return@mapNotNull null

                val preferredSourcePkg = dayStepRecords
                    .map { it.metadata.dataOrigin.packageName }
                    .maxByOrNull { pkg ->
                        val p = pkg.lowercase()
                        when {
                            p.contains("huawei") || p.contains("healthsync") -> 100
                            p.contains("xiaomi") || p.contains("zepp") || p.contains("huami") -> 90
                            p.contains("shealth") || p.contains("garmin") || p.contains("fitbit") -> 80
                            else -> 10
                        }
                    }.orEmpty()

                val rawStepsForPreferredSource = dayStepRecords
                    .filter { it.metadata.dataOrigin.packageName == preferredSourcePkg }
                    .sumOf { it.count }
                    .toInt()

                val totalDaySteps = if (aggregatedSteps > 0) aggregatedSteps else rawStepsForPreferredSource

                // Subtract steps already counted inside explicit ExerciseSessionRecords on the same date
                val exerciseStepsOnDay = mappedWorkouts
                    .filter { it.date == isoDate }
                    .sumOf { it.steps ?: 0 }

                val remainingSteps = (totalDaySteps - exerciseStepsOnDay).coerceAtLeast(0)
                if (remainingSteps < 20) return@mapNotNull null

                // Distance for the day (minus explicit exercise session distances)
                val totalDayDistance = runCatching {
                    val agg = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                            timeRangeFilter = dayRange
                        )
                    )
                    agg[DistanceRecord.DISTANCE_TOTAL]?.inMeters
                }.getOrNull()

                val exerciseDistanceOnDay = mappedWorkouts
                    .filter { it.date == isoDate }
                    .sumOf { it.distanceMeters ?: 0.0 }

                val remainingDistanceMeters = totalDayDistance
                    ?.let { (it - exerciseDistanceOnDay).coerceAtLeast(0.0) }
                    ?.takeIf { it >= 15.0 }
                    ?: (remainingSteps * 0.75)

                // Active calories for the day (minus explicit exercise session calories)
                val totalDayActiveKcal = runCatching {
                    val agg = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                            timeRangeFilter = dayRange
                        )
                    )
                    agg[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.roundToInt()
                }.getOrNull() ?: 0

                val exerciseKcalOnDay = mappedWorkouts
                    .filter { it.date == isoDate }
                    .sumOf { it.caloriesBurned }

                val remainingActiveKcal = (totalDayActiveKcal - exerciseKcalOnDay)
                    .takeIf { it in 5..2500 } ?: 0

                val estimatedDurationMinutes = (remainingSteps / 100).coerceAtLeast(1)
                val lastRecordEndMillis = dayStepRecords.maxOfOrNull { it.endTime.toEpochMilli() }
                    ?: dayEndInstant.toEpochMilli()

                DeviceWorkoutRecord(
                    externalId = "hc_steps_$isoDate",
                    date = isoDate,
                    category = DeviceExerciseCategory.DAILY_STEPS,
                    customTitle = "$remainingSteps Adım • Günlük Hareket",
                    durationMinutes = estimatedDurationMinutes,
                    caloriesBurned = remainingActiveKcal, // 0 triggers weight-adjusted step calorie calculation in DeviceHealthSyncManager
                    distanceMeters = remainingDistanceMeters,
                    steps = remainingSteps,
                    avgHeartRateBpm = null,
                    startTimeMillis = dayStartInstant.toEpochMilli(),
                    endTimeMillis = lastRecordEndMillis,
                    sourcePackage = preferredSourcePkg,
                    sourceName = resolveFriendlySourceName(preferredSourcePkg, isScale = false)
                )
            }
        } else emptyList()

        DeviceHealthSnapshot(
            weights = mappedWeights,
            workouts = mappedWorkouts + dailyStepWorkouts,
            platformName = "Health Connect"
        )
    }
}

private fun resolveFriendlySourceName(packageName: String, isScale: Boolean): String {
    val pkg = packageName.lowercase()
    return when {
        pkg.contains("xiaomi.hm.health") || pkg.contains("huami") || pkg.contains("zepp") ->
            if (isScale) "Zepp Life (Xiaomi Tartı)" else "Zepp Life (Xiaomi)"
        pkg.contains("xiaomi.wearable") || pkg.contains("mi.health") || pkg.contains("mifitness") ->
            if (isScale) "Mi Fitness (Xiaomi Tartı)" else "Mi Fitness (Xiaomi)"
        pkg.contains("huawei.health") || pkg.contains("huawei") ->
            "Huawei Health"
        pkg.contains("healthsync") || pkg.contains("appyhapps") ->
            "Health Sync"
        pkg.contains("shealth") || pkg.contains("samsung") ->
            "Samsung Health"
        pkg.contains("google.android.apps.fitness") ->
            "Google Fit"
        pkg.contains("garmin") ->
            "Garmin Connect"
        pkg.contains("withings") ->
            "Withings"
        pkg.contains("fitbit") ->
            "Fitbit"
        else -> if (isScale) "Health Connect Tartı" else "Health Connect Saat"
    }
}

private fun mapHealthConnectExerciseType(type: Int): DeviceExerciseCategory {
    return when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> DeviceExerciseCategory.RUNNING

        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> DeviceExerciseCategory.WALKING

        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> DeviceExerciseCategory.CYCLING

        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> DeviceExerciseCategory.SWIMMING

        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> DeviceExerciseCategory.STRENGTH

        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> DeviceExerciseCategory.HIIT

        ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
        ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> DeviceExerciseCategory.YOGA

        ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> DeviceExerciseCategory.PILATES

        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> DeviceExerciseCategory.ROWING

        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> DeviceExerciseCategory.ELLIPTICAL

        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> DeviceExerciseCategory.HIKING

        ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> DeviceExerciseCategory.BASKETBALL

        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER,
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> DeviceExerciseCategory.FOOTBALL

        ExerciseSessionRecord.EXERCISE_TYPE_TENNIS,
        ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON,
        ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> DeviceExerciseCategory.TENNIS

        ExerciseSessionRecord.EXERCISE_TYPE_BOXING,
        ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> DeviceExerciseCategory.MARTIAL_ARTS

        else -> DeviceExerciseCategory.OTHER
    }
}

