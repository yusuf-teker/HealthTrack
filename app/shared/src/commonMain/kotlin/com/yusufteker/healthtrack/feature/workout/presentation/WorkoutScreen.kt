package com.yusufteker.healthtrack.feature.workout.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.DailyStepTelemetry
import com.yusufteker.healthtrack.core.model.HealthPlatformStatus
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.utils.formatDayMonthYear
import com.yusufteker.healthtrack.core.utils.formatDisplayDateTr
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.rememberDeviceHealthBridge
import com.yusufteker.healthtrack.core.utils.rememberSpeechToTextLauncher
import com.yusufteker.healthtrack.core.utils.shiftDateIso
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.CrossedBarbellPlusIcon
import com.yusufteker.healthtrack.ui.components.DaysStripSelector
import com.yusufteker.healthtrack.ui.components.DeviceConnectionGuideDialog
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.HealthToastHost
import com.yusufteker.healthtrack.ui.components.ToastType
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import kotlinx.coroutines.launch

@Composable
fun WorkoutScreen(
    state: WorkoutState,
    onIntent: (WorkoutIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val speechLauncher = rememberSpeechToTextLauncher()
    val deviceHealthBridge = rememberDeviceHealthBridge()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var showManualFields by remember { mutableStateOf(false) }
    var showDeviceGuideDialog by remember { mutableStateOf(false) }

    val todayIso = remember { getTodayIso() }
    val isSelectedToday = state.selectedDate == todayIso
    val lang = appLang()
    val markedWorkoutDates = remember(state.allWorkouts) {
        state.allWorkouts.map { it.date }.toSet()
    }

    // Auto-sync watch & workouts silently on screen entry if permissions are already authorized
    LaunchedEffect(Unit) {
        deviceHealthBridge.syncSilentlyIfAuthorized(days = 30) { result ->
            onIntent(WorkoutIntent.OnWatchSnapshotReceived(result = result, isSilent = true))
        }
    }

    if (showDeviceGuideDialog) {
        DeviceConnectionGuideDialog(
            platformName = deviceHealthBridge.platformName,
            onOpenHealthSettings = { deviceHealthBridge.openHealthSettings() },
            onSyncNow = {
                onIntent(WorkoutIntent.OnWatchSyncStarted)
                deviceHealthBridge.syncLastDays(days = 30) { result ->
                    onIntent(WorkoutIntent.OnWatchSnapshotReceived(result))
                }
            },
            onDismiss = { showDeviceGuideDialog = false }
        )
    }

    if (state.isReminderDialogOpen) {
        com.yusufteker.healthtrack.feature.workout.presentation.components.WorkoutReminderDialog(
            currentFrequency = state.reminderFrequency,
            onDismiss = { onIntent(WorkoutIntent.OnSetReminderDialogOpen(false)) },
            onSave = { onIntent(WorkoutIntent.OnReminderFrequencyChanged(it)) }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Stitch Horizontal Days Strip with Week Badges ──
            DaysStripSelector(
                selectedDate = state.selectedDate,
                onDateSelected = { newDate ->
                    onIntent(WorkoutIntent.OnDateChanged(newDate))
                },
                markedDates = markedWorkoutDates
            )

            // ── Radial Strain & Calorie Burn Gauge (Hero Card) ──
            WorkoutStrainHeroCard(
                state = state,
                onOpenReminderSettings = { onIntent(WorkoutIntent.OnSetReminderDialogOpen(true)) }
            )

            // ── Ambient Daily Steps & Movement Telemetry Widget (Option 1) ──
            state.dailySteps?.let { steps ->
                if (steps.stepCount > 0) {
                    DailyStepsTelemetryCard(dailySteps = steps)
                }
            }

        // ── AI & Manual Workout Telemetry Logger Card ──
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = str("Egzersiz Ekle (AI)", "Add Workout (AI)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        onClick = { showManualFields = !showManualFields },
                        shape = RoundedCornerShape(999.dp),
                        color = if (showManualFields) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.bounceClick()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = if (showManualFields) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (showManualFields) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (showManualFields) str("Manuel Gizle", "Hide Manual") else str("Manuel Giriş", "Manual Entry"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (showManualFields) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Compact Expandable Text Input Well
                OutlinedTextField(
                    value = state.rawText,
                    onValueChange = { onIntent(WorkoutIntent.OnRawTextChanged(it)) },
                    placeholder = {
                        Text(
                            str(
                                "Hangi sporu yaptınız? Örn: 45 dk koşu, ağırlık...",
                                "What workout did you do? e.g. 45 min run, weights..."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            if (state.rawText.isNotBlank()) {
                                IconButton(
                                    onClick = { onIntent(WorkoutIntent.OnRawTextChanged("")) },
                                    modifier = Modifier.size(28.dp).bounceClick()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = str("Temizle", "Clear"),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (speechLauncher.isAvailable) {
                                val voicePrompt = str(
                                    "Hangi antrenmanı yaptınız? Sesinizle söyleyin...",
                                    "What workout did you do? Speak now..."
                                )
                                IconButton(
                                    onClick = {
                                        speechLauncher.launch(voicePrompt) { spokenText ->
                                            val current = state.rawText.trim()
                                            val updated = if (current.isBlank()) spokenText else "$current, $spokenText"
                                            onIntent(WorkoutIntent.OnRawTextChanged(updated))
                                        }
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(HealthColors.Primary.copy(alpha = 0.15f))
                                        .bounceClick()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = str("Sesle Söyle", "Voice Input"),
                                        tint = HealthColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)
                    ),
                    minLines = 1,
                    maxLines = 3
                )

                // Quick Logging Chips (No duplicate mic chip)
                val runChipText = str("30 dk koşu", "30 min run")
                val weightsChipText = str("45 dk ağırlık antrenmanı", "45 min weight training")
                val hiitChipText = str("20 dk HIIT kardiyo", "20 min HIIT cardio")
                val walkChipText = str("40 dk tempolu yürüyüş", "40 min brisk walk")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkoutQuickChip(label = str("+ 30 dk Koşu", "+ 30 min Run")) {
                        val current = if (state.rawText.isNotBlank()) "${state.rawText}, $runChipText" else runChipText
                        onIntent(WorkoutIntent.OnRawTextChanged(current))
                    }
                    WorkoutQuickChip(label = str("+ 45 dk Ağırlık", "+ 45 min Weights")) {
                        val current = if (state.rawText.isNotBlank()) "${state.rawText}, $weightsChipText" else weightsChipText
                        onIntent(WorkoutIntent.OnRawTextChanged(current))
                    }
                    WorkoutQuickChip(label = str("+ 20 dk HIIT", "+ 20 min HIIT")) {
                        val current = if (state.rawText.isNotBlank()) "${state.rawText}, $hiitChipText" else hiitChipText
                        onIntent(WorkoutIntent.OnRawTextChanged(current))
                    }
                    WorkoutQuickChip(label = str("+ 40 dk Yürüyüş", "+ 40 min Walk")) {
                        val current = if (state.rawText.isNotBlank()) "${state.rawText}, $walkChipText" else walkChipText
                        onIntent(WorkoutIntent.OnRawTextChanged(current))
                    }
                }

                val workoutAiQuota = state.aiQuota
                if (!state.hasCustomGeminiKey && workoutAiQuota != null) {
                    val isExhausted = workoutAiQuota.dailyRemaining <= 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (workoutAiQuota.isPremium) {
                                str(
                                    "Premium AI Hakkı: ${workoutAiQuota.dailyRemaining}/${workoutAiQuota.dailyLimit}",
                                    "Premium AI Quota: ${workoutAiQuota.dailyRemaining}/${workoutAiQuota.dailyLimit}"
                                )
                            } else {
                                str(
                                    "Günlük AI Hakkı: ${workoutAiQuota.dailyRemaining}/${workoutAiQuota.dailyLimit}",
                                    "Daily AI Quota: ${workoutAiQuota.dailyRemaining}/${workoutAiQuota.dailyLimit}"
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isExhausted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isExhausted) {
                            Text(
                                text = str("Profil > Özel API Key ile sınırsız", "Unlimited via Profile > Custom API Key"),
                                style = MaterialTheme.typography.labelSmall,
                                color = HealthColors.Primary
                            )
                        }
                    }
                }

                // AI Calculate Button
                Button(
                    onClick = { onIntent(WorkoutIntent.OnCalculateAiClicked) },
                    enabled = !state.isCalculatingAi && state.rawText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        disabledContentColor = Color.White
                    )
                ) {
                    if (state.isCalculatingAi) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            str("Yakılan Kalori Hesaplanıyor...", "Calculating Calories Burned..."),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            str(
                                "AI ile Hesapla (${state.userWeightKg} kg bazlı)",
                                "Calculate with AI (${state.userWeightKg} kg)"
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                // Manual Fields Section (Expanded via Header Pill)
                if (showManualFields) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = state.manualCalories,
                                onValueChange = { onIntent(WorkoutIntent.OnManualCaloriesChanged(it)) },
                                label = { Text(str("Kalori (kcal)", "Calories (kcal)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.manualDuration,
                                onValueChange = { onIntent(WorkoutIntent.OnManualDurationChanged(it)) },
                                label = { Text(str("Süre (dk)", "Duration (min)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                onIntent(WorkoutIntent.OnSaveManualClicked)
                                showManualFields = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(str("Elle Kaydet", "Save Manually"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ── Selected Day's Logged Workouts Feed ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isSelectedToday) {
                        str("Bugünkü Aktiviteler", "Today's Activities")
                    } else {
                        str("${formatDisplayDateTr(state.selectedDate, lang)} Aktiviteleri", "${formatDisplayDateTr(state.selectedDate, lang)} Activities")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = str("${state.workouts.size} Kayıt", "${state.workouts.size} Logged"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (deviceHealthBridge.status != HealthPlatformStatus.NOT_SUPPORTED) {
                    Surface(
                        onClick = {
                            if (!state.isSyncingWatch) {
                                onIntent(WorkoutIntent.OnWatchSyncStarted)
                                deviceHealthBridge.syncLastDays(days = 30) { result ->
                                    onIntent(WorkoutIntent.OnWatchSnapshotReceived(result))
                                }
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                        modifier = Modifier.bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (state.isSyncingWatch) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Text(
                                text = if (state.isSyncingWatch) str("Saat Okunuyor...", "Reading Watch...") else str("⌚ Saatten Çek", "⌚ Sync Watch"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showDeviceGuideDialog = true },
                    modifier = Modifier.size(28.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = str("Saat Nasıl Bağlanır?", "How to Connect Watch?"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (state.workouts.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = if (isSelectedToday) {
                                str("Bugün için henüz egzersiz kaydedilmedi.", "No workouts logged for today yet.")
                            } else {
                                str(
                                    "${formatDisplayDateTr(state.selectedDate, lang)} için egzersiz kaydı yok.",
                                    "No workouts logged for ${formatDisplayDateTr(state.selectedDate, lang)}."
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.workouts.forEach { w ->
                    LuxuryWorkoutItem(
                        workout = w,
                        onDelete = { onIntent(WorkoutIntent.OnDeleteWorkout(w.id)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ── Floating Toast Notifications (Auto-dismissing) ──
    HealthToastHost(
        message = state.errorMessage,
        type = ToastType.ERROR,
        onDismiss = { onIntent(WorkoutIntent.OnDismissErrorMessage) }
    )
    HealthToastHost(
        message = state.infoMessage,
        type = ToastType.SUCCESS,
        onDismiss = { onIntent(WorkoutIntent.OnDismissInfoMessage) }
    )
}
}

/**
 * Concentric Circular Radial Strain & Burn Gauge with Quick Metrics Ribbon matching Stitch.
 */
@Composable
private fun WorkoutStrainHeroCard(
    state: WorkoutState,
    onOpenReminderSettings: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val progress = state.calorieProgress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800)
    )
    val percentage = (progress * 100).toInt()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = str("Egzersiz & Efor", "Exercise & Effort"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Reminder Frequency Pill Button
                    val isReminderOff = state.reminderFrequency == com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency.OFF
                    val reminderLabel = when (state.reminderFrequency) {
                        com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency.OFF -> str("Kapalı", "Off")
                        com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency.SMART_CADENCE -> str("Akıllı", "Smart")
                        com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency.EVERY_2_DAYS -> str("2 Günde 1", "Every 2 Days")
                        com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency.THREE_DAYS_WEEK -> str("Haftada 3", "3x / Week")
                        com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency.EVERY_DAY -> str("Her Gün", "Daily")
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isReminderOff) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else HealthColors.Primary.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (isReminderOff) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f) else HealthColors.Primary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onOpenReminderSettings() }
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = str("Hatırlatıcı Ayarları", "Reminder Settings"),
                                tint = if (isReminderOff) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                } else {
                                    HealthColors.Primary
                                },
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = reminderLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isReminderOff) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                } else {
                                    HealthColors.Primary
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = str("%$percentage Hedef", "$percentage% Goal"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Glowing Radial Progress Ring
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                val ringBackground = if (isDark) HealthColors.SurfaceContainerHighest.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight
                val flameGradient = Brush.sweepGradient(
                    listOf(
                        MaterialTheme.colorScheme.tertiary,
                        Color(0xFFFF7043),
                        HealthColors.Primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    // Background Track
                    drawArc(
                        color = ringBackground,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active Burn Arc
                    if (animatedProgress > 0f) {
                        drawArc(
                            brush = flameGradient,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Centered Telemetry
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = str("YAKILAN", "BURNED"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${state.totalCaloriesBurned}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "/ ${state.targetCalories}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = str("kcal hedefi", "kcal goal"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Metrics Ribbon (3 Columns: Active Time, Net Intake, Burned)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) HealthColors.SurfaceContainerLowest.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Time
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = str("AKTİF SÜRE", "ACTIVE TIME"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = str("${state.totalDurationMinutes} dk", "${state.totalDurationMinutes} min"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = str("hedef ${state.targetDurationMinutes} dk", "goal ${state.targetDurationMinutes} min"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )

                // Net Calorie
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = str("NET KALORİ", "NET CALORIES"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${state.netCalories} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.netCalories <= 0) HealthColors.Primary else MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = str("alınan − yakılan", "intake − burned"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )

                // Total Burned
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = str("TOPLAM YAKILAN", "TOTAL BURNED"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "-${state.totalCaloriesBurned}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "kcal",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private data class WorkoutVisual(
    val icon: ImageVector,
    val color: Color
)

private fun resolveWorkoutVisual(workout: WorkoutLog): WorkoutVisual {
    if (workout.isDailySteps) {
        return WorkoutVisual(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            color = HealthColors.NutrientFiber
        )
    }

    val text = workout.rawText.lowercase()
    return when {
        text.contains("eliptik") || text.contains("elliptical") ||
            text.contains("bisiklet") || text.contains("cycling") ||
            text.contains("pedal") || text.contains("spin") -> {
            WorkoutVisual(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                color = HealthColors.Primary
            )
        }
        text.contains("kürek") || text.contains("kurek") || text.contains("rowing") -> {
            WorkoutVisual(
                icon = Icons.Default.Rowing,
                color = HealthColors.Secondary
            )
        }
        text.contains("yürüyüş") || text.contains("yuruyus") || text.contains("walking") ||
            text.contains("yürü") || text.contains("adım") -> {
            WorkoutVisual(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                color = HealthColors.NutrientFiber
            )
        }
        text.contains("yüzme") || text.contains("yuzme") || text.contains("swimming") || text.contains("havuz") -> {
            WorkoutVisual(
                icon = Icons.Default.Pool,
                color = HealthColors.NutrientVitD
            )
        }
        text.contains("ağırlık") || text.contains("agirlik") || text.contains("fitness") ||
            text.contains("bodybuilding") || text.contains("gym") || text.contains("dumbell") ||
            text.contains("dambıl") || text.contains("bench") || text.contains("squat") || text.contains("deadlift") -> {
            WorkoutVisual(
                icon = Icons.Default.FitnessCenter,
                color = HealthColors.NutrientCarbs
            )
        }
        text.contains("yoga") || text.contains("pilates") || text.contains("esneme") || text.contains("stretching") -> {
            WorkoutVisual(
                icon = Icons.Default.SelfImprovement,
                color = HealthColors.SecondaryAccent
            )
        }
        text.contains("basketbol") || text.contains("basketball") -> {
            WorkoutVisual(
                icon = Icons.Default.SportsBasketball,
                color = HealthColors.NutrientCarbs
            )
        }
        text.contains("futbol") || text.contains("football") || text.contains("soccer") -> {
            WorkoutVisual(
                icon = Icons.Default.SportsSoccer,
                color = HealthColors.NutrientFiber
            )
        }
        text.contains("tenis") || text.contains("tennis") -> {
            WorkoutVisual(
                icon = Icons.Default.SportsTennis,
                color = HealthColors.Secondary
            )
        }
        text.contains("boks") || text.contains("boxing") || text.contains("mma") || text.contains("kickboks") || text.contains("dövüş") -> {
            WorkoutVisual(
                icon = Icons.Default.SportsMma,
                color = HealthColors.NutrientIron
            )
        }
        text.contains("hiking") || text.contains("tırmanış") || text.contains("dağ") -> {
            WorkoutVisual(
                icon = Icons.Default.Hiking,
                color = HealthColors.Tertiary
            )
        }
        else -> {
            WorkoutVisual(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                color = HealthColors.Tertiary
            )
        }
    }
}

/**
 * Luxury Workout Activity Item Card with glowing circular icon container and burned kcal badge.
 */
@Composable
private fun LuxuryWorkoutItem(
    workout: WorkoutLog,
    onDelete: () -> Unit
) {
    val visual = resolveWorkoutVisual(workout)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Colored Icon Pod
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(visual.color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.rawText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val analysis = workout.aiAnalysis
                    if (!analysis.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = analysis,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (workout.durationMinutes > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = str("${workout.durationMinutes} dakika", "${workout.durationMinutes} min"),
                                style = MaterialTheme.typography.labelSmall,
                                color = HealthColors.Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Calorie pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "-${workout.caloriesBurned} kcal",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = str("Sil", "Delete"),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutQuickChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .bounceClick()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Stitch Luxury Ambient Daily Steps & Movement Telemetry Card (Option 1).
 * Displays ambient watch steps cleanly without crowding the workout list or double-counting active sport calories.
 */
@Composable
private fun DailyStepsTelemetryCard(dailySteps: DailyStepTelemetry) {
    val isDark = LocalIsDarkTheme.current
    val progress = dailySteps.progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600)
    )
    val percentage = (progress * 100).toInt()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(HealthColors.Primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${dailySteps.stepCount}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = str("adım", "steps"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${dailySteps.distanceKm} km",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = HealthColors.Primary
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = str("~${dailySteps.durationMinutes} dk hareket", "~${dailySteps.durationMinutes} min active"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (dailySteps.netCaloriesBurned > 0) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "${dailySteps.netCaloriesBurned} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

                // Source Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isDark) HealthColors.SurfaceContainerHigh
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⌚ ${dailySteps.sourceName}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Target Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = str("Hedef ${dailySteps.stepTarget} adım", "Goal ${dailySteps.stepTarget} steps"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = str("%$percentage", "$percentage%"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = HealthColors.Primary
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = HealthColors.Primary,
                    trackColor = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
