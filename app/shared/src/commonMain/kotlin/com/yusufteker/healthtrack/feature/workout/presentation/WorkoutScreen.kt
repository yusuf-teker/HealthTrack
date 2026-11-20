@file:OptIn(ExperimentalLayoutApi::class)

package com.yusufteker.healthtrack.feature.workout.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.DailyStepTelemetry
import com.yusufteker.healthtrack.core.model.HealthPlatformStatus
import com.yusufteker.healthtrack.core.model.SportType
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
import com.yusufteker.healthtrack.ui.components.ShimmeringAiButton
import com.yusufteker.healthtrack.ui.components.SportIcons
import com.yusufteker.healthtrack.ui.components.SpotlightTargetId
import com.yusufteker.healthtrack.ui.components.ToastType
import com.yusufteker.healthtrack.ui.components.accentColor
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import com.yusufteker.healthtrack.ui.components.keepVisibleAboveIme
import com.yusufteker.healthtrack.ui.components.rememberKeyboardDismisser
import com.yusufteker.healthtrack.ui.components.iconVector
import com.yusufteker.healthtrack.ui.components.spotlightTarget
import kotlinx.coroutines.launch

@Composable
fun WorkoutScreen(
    state: WorkoutState,
    onIntent: (WorkoutIntent) -> Unit,
    onOpenPremiumOffer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val speechLauncher = rememberSpeechToTextLauncher()
    val deviceHealthBridge = rememberDeviceHealthBridge()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var showManualFields by remember { mutableStateOf(false) }
    var showDeviceGuideDialog by remember { mutableStateOf(false) }
    var workoutPendingDelete by remember { mutableStateOf<WorkoutLog?>(null) }
    var rawTextInput by remember { mutableStateOf(state.rawText) }
    var manualCaloriesInput by remember { mutableStateOf(state.manualCalories) }
    var manualDurationInput by remember { mutableStateOf(state.manualDuration) }
    val dismissKeyboard = rememberKeyboardDismisser()
    LaunchedEffect(state.rawText) {
        if (rawTextInput != state.rawText) {
            // Antrenman kaydedildi / metin sıfırlandı -> klavyeyi otomatik kapat
            if (state.rawText.isBlank() && rawTextInput.isNotBlank()) dismissKeyboard()
            rawTextInput = state.rawText
        }
    }
    LaunchedEffect(state.isCalculatingAi) {
        if (state.isCalculatingAi) dismissKeyboard()
    }
    LaunchedEffect(state.manualCalories) {
        if (manualCaloriesInput != state.manualCalories) manualCaloriesInput = state.manualCalories
    }
    LaunchedEffect(state.manualDuration) {
        if (manualDurationInput != state.manualDuration) manualDurationInput = state.manualDuration
    }

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

    workoutPendingDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { workoutPendingDelete = null },
            title = {
                Text(
                    text = str("Antrenmanı Sil?", "Delete Workout?"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = str(
                        "\"${workout.rawText}\" (${workout.caloriesBurned} kcal) kaydını silmek istediğinize emin misiniz?",
                        "Are you sure you want to delete \"${workout.rawText}\" (${workout.caloriesBurned} kcal)?"
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(WorkoutIntent.OnDeleteWorkout(workout.id))
                        workoutPendingDelete = null
                    }
                ) {
                    Text(
                        str("Sil", "Delete"),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutPendingDelete = null }) {
                    Text(str("İptal", "Cancel"))
                }
            }
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
                onOpenReminderSettings = { onIntent(WorkoutIntent.OnSetReminderDialogOpen(true)) },
                modifier = Modifier.spotlightTarget(SpotlightTargetId.WORKOUT_HERO_GAUGE)
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

                // Minimalist Sport Category Icon Selector
                val detectedSports = remember(rawTextInput) {
                    if (rawTextInput.isBlank()) emptySet() else SportType.detectAll(rawTextInput)
                }
                val selectedSports = remember(detectedSports, state.selectedSport) {
                    val selected = state.selectedSport
                    if (selected != null && selected != SportType.OTHER) {
                        detectedSports + selected
                    } else {
                        detectedSports
                    }
                }
                val durationUnit = str("dk", "min")
                SportCategorySelectorRow(
                    selectedSports = selectedSports,
                    onSportClick = { sport ->
                        val template = sport.localizedTemplate(lang)
                        val trimmed = rawTextInput.trim()
                        val isExactTemplate = trimmed.equals(template, ignoreCase = true)
                        if (sport == state.selectedSport && (trimmed.isEmpty() || isExactTemplate)) {
                            rawTextInput = ""
                            onIntent(WorkoutIntent.OnRawTextChanged(""))
                            onIntent(WorkoutIntent.OnSportSelected(null))
                        } else {
                            val isAnyTemplate = SportIcons.SELECTABLE_SPORTS.any {
                                trimmed.equals(it.localizedTemplate(lang), ignoreCase = true)
                            }
                            val durationOnlyMatch = Regex("""^(\d+)\s*(?:dk|dakika|min|m)?$""", RegexOption.IGNORE_CASE)
                                .matchEntire(trimmed)
                            val durationPrefixMatch = Regex("""^(\d+)\s*(?:dk|dakika|min)\s+.+$""", RegexOption.IGNORE_CASE)
                                .matchEntire(trimmed)

                            val updated = when {
                                durationOnlyMatch != null -> {
                                    val dur = durationOnlyMatch.groupValues[1]
                                    "$dur $durationUnit ${sport.localizedTitle(lang)}"
                                }
                                trimmed.isEmpty() || isAnyTemplate -> template
                                sport in detectedSports -> trimmed
                                durationPrefixMatch != null && detectedSports.size <= 1 -> {
                                    val dur = durationPrefixMatch.groupValues[1]
                                    "$dur $durationUnit ${sport.localizedTitle(lang)}"
                                }
                                else -> "$trimmed, $template"
                            }
                            rawTextInput = updated
                            onIntent(WorkoutIntent.OnRawTextChanged(updated))
                            onIntent(WorkoutIntent.OnSportSelected(sport))
                        }
                    }
                )

                val workoutAiQuota = state.aiQuota?.takeIf { !state.hasCustomGeminiKey }
                val isWorkoutQuotaExhausted = workoutAiQuota != null && workoutAiQuota.dailyRemaining <= 0
                val workoutQuotaBadge = workoutAiQuota?.let { "${it.dailyRemaining}/${it.dailyLimit}" }

                // Input + chips + "AI ile Hesapla" klavye açıkken birlikte görünür kalsın
                Column(
                    modifier = Modifier.fillMaxWidth().keepVisibleAboveIme(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                // Compact Expandable Text Input Well
                OutlinedTextField(
                    value = rawTextInput,
                    onValueChange = {
                        rawTextInput = it
                        onIntent(WorkoutIntent.OnRawTextChanged(it))
                    },
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
                            if (rawTextInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        rawTextInput = ""
                                        onIntent(WorkoutIntent.OnRawTextChanged(""))
                                    },
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
                                            val current = rawTextInput.trim()
                                            val updated = if (current.isBlank()) spokenText else "$current, $spokenText"
                                            rawTextInput = updated
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .spotlightTarget(SpotlightTargetId.WORKOUT_INPUT_FIELD),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = {
                        dismissKeyboard()
                        val canCalculate = !state.isCalculatingAi && (
                            rawTextInput.isNotBlank() ||
                                (state.selectedSport != null && state.selectedSport != SportType.OTHER)
                            )
                        if (canCalculate) onIntent(WorkoutIntent.OnCalculateAiClicked)
                    }),
                    minLines = 1,
                    maxLines = 3
                )

                // Quick Logging Chips (Most recent workouts first, filled up to 4 with smart defaults)
                val defaultChips = listOf(
                    str("+ 30 dk Koşu", "+ 30 min Run") to str("30 dk koşu", "30 min run"),
                    str("+ 45 dk Ağırlık", "+ 45 min Weights") to str("45 dk ağırlık antrenmanı", "45 min weight training"),
                    str("+ 20 dk HIIT", "+ 20 min HIIT") to str("20 dk HIIT kardiyo", "20 min HIIT cardio"),
                    str("+ 40 dk Yürüyüş", "+ 40 min Walk") to str("40 dk tempolu yürüyüş", "40 min brisk walk")
                )
                val recentChips = remember(state.allWorkouts) {
                    state.allWorkouts
                        .filter { !it.isDeleted && !it.isDailySteps && it.rawText.isNotBlank() }
                        .sortedWith(compareByDescending<WorkoutLog> { it.date }.thenByDescending { it.updatedAt })
                        .map { it.rawText.trim() }
                        .distinctBy { it.lowercase() }
                        .take(4)
                        .map { raw ->
                            val short = if (raw.length > 22) "${raw.take(22)}…" else raw
                            "+ $short" to raw
                        }
                }
                val combinedChips = remember(recentChips, defaultChips) {
                    val seen = recentChips.map { it.second.lowercase() }.toMutableSet()
                    val result = recentChips.toMutableList()
                    defaultChips.forEach { (label, prompt) ->
                        if (result.size < 4 && seen.add(prompt.lowercase())) {
                            result.add(label to prompt)
                        }
                    }
                    result
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    combinedChips.forEach { (label, promptText) ->
                        val chipSport = SportType.fromRawText(promptText)
                        WorkoutQuickChip(
                            label = label,
                            icon = if (chipSport != SportType.OTHER) chipSport.iconVector() else null,
                            iconTint = chipSport.accentColor()
                        ) {
                            val current = if (rawTextInput.isNotBlank()) "$rawTextInput, $promptText" else promptText
                            rawTextInput = current
                            onIntent(WorkoutIntent.OnRawTextChanged(current))
                        }
                    }
                }

                // AI Calculate Button
                ShimmeringAiButton(
                    text = str(
                        "AI ile Hesapla (${state.userWeightKg} kg bazlı)",
                        "Calculate with AI (${state.userWeightKg} kg)"
                    ),
                    badgeText = workoutQuotaBadge,
                    isBadgeAlert = isWorkoutQuotaExhausted,
                    isLoading = state.isCalculatingAi,
                    enabled = !state.isCalculatingAi && (
                        rawTextInput.isNotBlank() ||
                            (state.selectedSport != null && state.selectedSport != SportType.OTHER)
                        ),
                    onClick = {
                        dismissKeyboard()
                        onIntent(WorkoutIntent.OnCalculateAiClicked)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .spotlightTarget(SpotlightTargetId.WORKOUT_CALCULATE_BUTTON),
                    height = 44.dp
                )
                }

                if (isWorkoutQuotaExhausted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPremiumOffer?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = str("🔥 Günlük Ücretsiz AI Kotan Doldu", "🔥 Daily Free AI Quota Reached"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                                Text(
                                    text = str("Sana özel fırsatla sınırsıza geç", "Upgrade with your exclusive offer"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = str("249.99 ₺ >", "249.99 ₺ >"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }

                // Manual Fields Section (Expanded via Header Pill)
                if (showManualFields) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .keepVisibleAboveIme()
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
                                value = manualCaloriesInput,
                                onValueChange = {
                                    manualCaloriesInput = it
                                    onIntent(WorkoutIntent.OnManualCaloriesChanged(it))
                                },
                                label = { Text(str("Kalori (kcal)", "Calories (kcal)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = manualDurationInput,
                                onValueChange = {
                                    manualDurationInput = it
                                    onIntent(WorkoutIntent.OnManualDurationChanged(it))
                                },
                                label = { Text(str("Süre (dk)", "Duration (min)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                dismissKeyboard()
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
                            imageVector = SportIcons.FitnessDumbbellIcon,
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
                        onDelete = { workoutPendingDelete = w }
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
    onOpenReminderSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val progress = state.calorieProgress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800)
    )
    val percentage = (progress * 100).toInt()

    GlassCard(
        modifier = modifier.fillMaxWidth(),
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

                    val isGoalReached = state.targetCalories > 0 && state.totalCaloriesBurned >= state.targetCalories
                    val goalPillBg = if (isGoalReached) {
                        HealthColors.Primary.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    }
                    val goalPillTextColor = if (isGoalReached) {
                        if (isDark) Color(0xFF34D399) else HealthColors.Primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(goalPillBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isGoalReached) {
                                str("✓ HEDEF DOLDU ($percentage%)", "✓ GOAL HIT ($percentage%)")
                            } else {
                                str("%$percentage Hedef", "$percentage% Goal")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = goalPillTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Glowing Radial Progress Ring
            val isGoalReached = state.targetCalories > 0 && state.totalCaloriesBurned >= state.targetCalories
            val goalPulse = if (isGoalReached) {
                val transition = rememberInfiniteTransition(label = "workout_goal_ring")
                val scale by transition.animateFloat(
                    initialValue = 0.97f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ring_halo_pulse"
                )
                scale
            } else {
                1f
            }

            Box(
                modifier = Modifier.size(196.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isGoalReached) {
                    Box(
                        modifier = Modifier
                            .size(192.dp)
                            .graphicsLayer {
                                scaleX = goalPulse
                                scaleY = goalPulse
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        HealthColors.PrimaryGlow.copy(alpha = 0.24f),
                                        Color(0xFFFACC15).copy(alpha = 0.10f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                val ringBackground = if (isDark) HealthColors.SurfaceContainerHighest.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight
                val flameGradient = if (isGoalReached) {
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFACC15),
                            HealthColors.PrimaryGlow,
                            Color(0xFF06D6A0),
                            Color(0xFFF97316),
                            Color(0xFFFACC15)
                        )
                    )
                } else {
                    Brush.sweepGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiary,
                            Color(0xFFFF7043),
                            HealthColors.Primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                }

                Canvas(modifier = Modifier.size(186.dp)) {
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
                        text = if (isGoalReached) str("HEDEF TAMAMLANDI", "GOAL CRUSHED") else str("YAKILAN", "BURNED"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isGoalReached) {
                            if (isDark) Color(0xFF34D399) else HealthColors.Primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
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
                        text = if (isGoalReached) {
                            str("🔥 Harika enerji!", "🔥 Unstoppable!")
                        } else {
                            str("kcal hedefi", "kcal goal")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isGoalReached) FontWeight.Bold else FontWeight.Normal,
                        color = if (isGoalReached) {
                            if (isDark) Color(0xFFFDE047) else Color(0xFFD97706)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
                        text = if (state.totalCaloriesBurned > 0) "-${state.totalCaloriesBurned}" else "0",
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
            icon = SportIcons.WalkingIcon,
            color = HealthColors.NutrientFiber
        )
    }

    val sport = SportType.fromWorkout(workout)
    return WorkoutVisual(
        icon = sport.iconVector(),
        color = sport.accentColor()
    )
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    WorkoutTelemetrySection(
                        analysis = workout.aiAnalysis,
                        durationMinutes = workout.durationMinutes
                    )
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
private fun SportCategorySelectorRow(
    selectedSports: Set<SportType>,
    onSportClick: (SportType) -> Unit
) {
    val lang = appLang()
    val isDark = LocalIsDarkTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SportIcons.SELECTABLE_SPORTS.forEach { sport ->
            val isSelected = sport in selectedSports
            val accent = sport.accentColor()
            val containerColor = when {
                isSelected -> accent.copy(alpha = 0.20f)
                isDark -> HealthColors.SurfaceContainerHigh.copy(alpha = 0.72f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = containerColor,
                border = if (isSelected) {
                    BorderStroke(1.25.dp, accent.copy(alpha = 0.7f))
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSportClick(sport) }
                    .bounceClick()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = if (isSelected) 0.24f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = sport.iconVector(),
                            contentDescription = sport.localizedTitle(lang),
                            tint = if (isSelected) accent else accent.copy(alpha = 0.88f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = sport.localizedTitle(lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutQuickChip(
    label: String,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
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
                        imageVector = SportIcons.WalkingIcon,
                        contentDescription = null,
                        tint = HealthColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
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
                            color = HealthColors.Primary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = str("~${dailySteps.durationMinutes} dk hareket", "~${dailySteps.durationMinutes} min active"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
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
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
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

@Composable
private fun WorkoutTelemetrySection(
    analysis: String?,
    durationMinutes: Int
) {
    val trimmedAnalysis = analysis?.trim().orEmpty()
    val isStructuredTelemetry = trimmedAnalysis.isNotEmpty() && (
        trimmedAnalysis.contains("•") || trimmedAnalysis.startsWith("⌚")
    )
    val telemetryTokens = if (isStructuredTelemetry) {
        trimmedAnalysis.split("•").map { it.trim() }.filter { it.isNotEmpty() }
    } else {
        emptyList()
    }

    if (trimmedAnalysis.isNotEmpty() && !isStructuredTelemetry) {
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = trimmedAnalysis,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (telemetryTokens.isNotEmpty() || durationMinutes > 0) {
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (durationMinutes > 0) {
                WorkoutTelemetryBadge(
                    text = str("⏱️ $durationMinutes dk", "⏱️ $durationMinutes min"),
                    tint = Color(0xFFF59E0B)
                )
            }
            val fallbackTint = MaterialTheme.colorScheme.onSurfaceVariant
            telemetryTokens.forEach { token ->
                val (badgeText, badgeTint) = classifyTelemetryBadge(token, fallbackTint)
                WorkoutTelemetryBadge(text = badgeText, tint = badgeTint)
            }
        }
    }
}

private fun classifyTelemetryBadge(token: String, defaultColor: Color): Pair<String, Color> {
    val lower = token.lowercase()
    return when {
        lower.contains("bpm") || lower.contains("❤️") || lower.contains("ort.") || lower.contains("avg") || lower.contains("kalp") || lower.contains("heart") -> {
            val label = if (token.startsWith("❤️")) token else "❤️ $token"
            label to Color(0xFFEF4444)
        }
        lower.contains("km") || lower.endsWith(" m") || lower.contains("metre") || lower.contains("meter") || lower.contains("📍") -> {
            val label = if (token.startsWith("📍")) token else "📍 $token"
            label to Color(0xFF3B82F6)
        }
        lower.contains("adım") || lower.contains("adim") || lower.contains("step") || lower.contains("👟") -> {
            val label = if (token.startsWith("👟")) token else "👟 $token"
            label to HealthColors.Primary
        }
        token.startsWith("⌚") || lower.contains("watch") || lower.contains("health") || lower.contains("garmin") || lower.contains("huawei") || lower.contains("fitbit") || lower.contains("samsung") -> {
            val label = if (token.startsWith("⌚")) token else "⌚ $token"
            label to Color(0xFF8B5CF6)
        }
        else -> token to defaultColor
    }
}

@Composable
private fun WorkoutTelemetryBadge(
    text: String,
    tint: Color
) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.30f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
