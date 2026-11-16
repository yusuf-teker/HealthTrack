package com.yusufteker.healthtrack.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.HealthPlatformStatus
import com.yusufteker.healthtrack.core.utils.rememberDailyReminderNotifier
import com.yusufteker.healthtrack.core.utils.rememberDeviceHealthBridge
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import kotlinx.coroutines.Dispatchers
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InitialProfileSetupScreenRoot(
    onComplete: () -> Unit,
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    val dailyReminderNotifier = rememberDailyReminderNotifier()
    val deviceHealthBridge = rememberDeviceHealthBridge()
    var hasNotifPermission by remember { mutableStateOf(dailyReminderNotifier.hasPermission) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.NavigateBack -> onComplete()
                is ProfileEffect.ShowToast -> onShowToast(effect.message)
                is ProfileEffect.LoggedOut -> Unit
            }
        }
    }

    InitialProfileSetupScreen(
        state = state,
        hasNotificationPermission = hasNotifPermission,
        platformStatus = deviceHealthBridge.status,
        platformName = deviceHealthBridge.platformName,
        onIntent = viewModel::onIntent,
        onRequestNotificationPermission = {
            dailyReminderNotifier.requestPermission { granted ->
                hasNotifPermission = granted
            }
        },
        onSyncDevice = {
            viewModel.onIntent(ProfileIntent.OnDeviceSyncStarted)
            deviceHealthBridge.syncLastDays(days = 30) { result ->
                viewModel.onIntent(ProfileIntent.OnDeviceSnapshotReceived(result))
            }
        },
        onSkipForLater = onComplete,
        modifier = modifier
    )
}

@Composable
fun InitialProfileSetupScreen(
    state: ProfileState,
    hasNotificationPermission: Boolean,
    platformStatus: HealthPlatformStatus,
    platformName: String,
    onIntent: (ProfileIntent) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSyncDevice: () -> Unit,
    onSkipForLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = appLang()
    var weightText by remember {
        mutableStateOf(if (state.weightKg > 0.0) state.weightKg.toString() else "72.0")
    }
    var targetWeightText by remember {
        mutableStateOf(if (state.targetWeightKg > 0.0) state.targetWeightKg.toString() else "70.0")
    }
    var heightText by remember {
        mutableStateOf(if (state.heightCm > 0.0) state.heightCm.toInt().toString() else "175")
    }
    var ageText by remember {
        mutableStateOf(if (state.age > 0) state.age.toString() else "26")
    }

    LaunchedEffect(state.hasConnectedDevice, state.isDeviceSyncing) {
        if (!state.isDeviceSyncing && state.hasConnectedDevice && state.weightKg > 0.0) {
            val currentParsed = weightText.replace(',', '.').toDoubleOrNull()
            if (currentParsed != state.weightKg) {
                weightText = state.weightKg.toString()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .clearFocusOnTap()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        HealthColors.GradientPrimary.first().copy(alpha = 0.22f),
                                        HealthColors.GradientPrimary.last().copy(alpha = 0.16f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        GradientText(
                            text = str("Hızlı Başlangıç Kurulumu", "Quick Start Setup"),
                            colors = HealthColors.GradientPrimary,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = str(
                                "Hedef kalorini ve analizlerini kişiselleştirelim.",
                                "Let's personalize your calorie targets and analytics."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Core Body Telemetry Card ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = str("Vücut Bilgilerin", "Body Metrics"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { raw ->
                                weightText = raw
                                raw.replace(',', '.').toDoubleOrNull()?.takeIf { it in 25.0..300.0 }?.let {
                                    onIntent(ProfileIntent.OnWeightChanged(it))
                                }
                            },
                            label = { Text(str("Kilo (kg)", "Weight (kg)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetWeightText,
                            onValueChange = { raw ->
                                targetWeightText = raw
                                raw.replace(',', '.').toDoubleOrNull()?.takeIf { it in 25.0..300.0 }?.let {
                                    onIntent(ProfileIntent.OnTargetWeightChanged(it))
                                }
                            },
                            label = { Text(str("Hedef (kg)", "Target (kg)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { raw ->
                                heightText = raw
                                raw.replace(',', '.').toDoubleOrNull()?.takeIf { it in 100.0..250.0 }?.let {
                                    onIntent(ProfileIntent.OnHeightChanged(it))
                                }
                            },
                            label = { Text(str("Boy (cm)", "Height (cm)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { raw ->
                                ageText = raw
                                raw.toIntOrNull()?.takeIf { it in 12..110 }?.let {
                                    onIntent(ProfileIntent.OnAgeChanged(it))
                                }
                            },
                            label = { Text(str("Yaş", "Age")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Gender selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "MALE" to str("Erkek", "Male"),
                            "FEMALE" to str("Kadın", "Female")
                        ).forEach { (code, label) ->
                            val selected = state.gender == code
                            Surface(
                                onClick = { onIntent(ProfileIntent.OnGenderChanged(code)) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) HealthColors.Primary.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) HealthColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f).bounceClick()
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Primary Fitness Goal ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = str("Ana Hedefin Nedir?", "What Is Your Primary Goal?"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FitnessGoal.entries.forEach { goal ->
                        val isSelected = state.fitnessGoal == goal
                        Surface(
                            onClick = { onIntent(ProfileIntent.OnFitnessGoalChanged(goal)) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) HealthColors.Primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth().bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = goal.localizedTitle(lang),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = HealthColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Optional Quick Permissions / Device Sync ──
            if (!hasNotificationPermission || platformStatus != HealthPlatformStatus.NOT_SUPPORTED) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = str("İsteğe Bağlı Kolaylıklar", "Optional Boosters"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!hasNotificationPermission) {
                            OutlinedButton(
                                onClick = onRequestNotificationPermission,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().bounceClick()
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(str("Akıllı Hatırlatıcılara İzin Ver", "Allow Smart Reminders"))
                            }
                        }
                        if (platformStatus != HealthPlatformStatus.NOT_SUPPORTED) {
                            OutlinedButton(
                                onClick = onSyncDevice,
                                enabled = !state.isDeviceSyncing,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().bounceClick()
                            ) {
                                Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.hasConnectedDevice) {
                                        str("$platformName Bağlandı ✓", "$platformName Connected ✓")
                                    } else {
                                        str("Akıllı Saat & Tartı Bağla ($platformName)", "Connect Watch & Scale ($platformName)")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onIntent(ProfileIntent.OnSaveProfileClicked) },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = str("Profilimi Kaydet & Başla", "Save Profile & Start"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            TextButton(
                onClick = onSkipForLater,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = str("Şimdi Geç, Ana Ekrana Git", "Skip for Now"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

