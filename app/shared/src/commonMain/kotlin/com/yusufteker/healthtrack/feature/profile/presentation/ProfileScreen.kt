package com.yusufteker.healthtrack.feature.profile.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.HealthPlatformStatus
import com.yusufteker.healthtrack.core.utils.rememberDeviceHealthBridge
import com.yusufteker.healthtrack.feature.food.presentation.components.EditGoalsDialog
import com.yusufteker.healthtrack.feature.weight.presentation.components.WeightReminderDialog
import com.yusufteker.healthtrack.feature.workout.presentation.components.WorkoutReminderDialog
import com.yusufteker.healthtrack.theme.AppThemeColor
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.DeviceConnectionGuideDialog
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.LanguageSelectorPill
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

private enum class ProfileTab(val titleTr: String, val titleEn: String, val icon: ImageVector) {
    PROFILE_GOALS("Profil & Hedefler", "Profile & Goals", Icons.Rounded.Person),
    NOTIFICATIONS_THEME("Bildirim & Tema", "Alerts & Theme", Icons.Rounded.Notifications),
    DEVICES_ACCOUNT("Cihaz & Hesap", "Devices & Account", Icons.Rounded.Watch)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    onBack: () -> Unit,
    onOpenPremium: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onOpenWidgetPromo: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val deviceHealthBridge = rememberDeviceHealthBridge()
    val isDark = LocalIsDarkTheme.current
    val lang = appLang()

    var selectedTab by remember { mutableStateOf(ProfileTab.PROFILE_GOALS) }
    var showEditBodyDialog by remember { mutableStateOf(false) }
    var showEditGoalsDialog by remember { mutableStateOf(false) }
    var showWorkoutReminderDialog by remember { mutableStateOf(false) }
    var showWeightReminderDialog by remember { mutableStateOf(false) }
    var showDeviceGuideDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showCustomApiKeyDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = str("Profil & Ayarlar", "Profile & Settings"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = str("Geri", "Back"),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        GradientText(
                            text = "H",
                            colors = HealthColors.GradientPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .clearFocusOnTap()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Feedback Banner Messages ──
            AnimatedVisibility(visible = state.errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onIntent(ProfileIntent.OnDismissErrorMessage) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = str("Kapat", "Close"), tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.infoMessage != null, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.infoMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onIntent(ProfileIntent.OnDismissInfoMessage) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = str("Kapat", "Close"), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // ── HealthTrack Premium Banner ──
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick()
                    .clickable(onClick = onOpenPremium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isPremium) {
                                str("HealthTrack Premium Aktif", "HealthTrack Premium Active")
                            } else {
                                str("HealthTrack Premium'a Geç", "Upgrade to HealthTrack Premium")
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.isPremium) {
                                val until = state.premiumUntil?.take(10)
                                if (until != null) str("Bitiş: $until", "Expires: $until") else str("Süresiz / Aktif Üyelik", "Lifetime / Active Membership")
                            } else {
                                str(
                                    "Sınırsız AI analizi, öğün planlayıcı ve özel temalar",
                                    "Unlimited AI analysis, meal planner & custom themes"
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Segmented Tab Selector ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProfileTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab
                        Surface(
                            onClick = { selectedTab = tab },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.15f)
                            } else {
                                Color.Transparent
                            },
                            border = if (isSelected) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            } else null,
                            modifier = Modifier
                                .weight(1f)
                                .bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = str(tab.titleTr, tab.titleEn),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab == ProfileTab.PROFILE_GOALS) {
                // ── Section 1: Kimlik & Fiziksel Profil Özeti (Tek Kompakt Kart) ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.name.firstOrNull()?.uppercase() ?: state.username.firstOrNull()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.name.isNotBlank()) state.name else str("Kullanıcı", "User"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "@${state.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            onClick = { showEditBodyDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = str("Fiziksel Bilgileri Düzenle", "Edit Body Stats"),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = str("Düzenle", "Edit"),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))

                    // Salt-okunur (Read-Only) Kompakt Vücut Özeti Şeridi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showEditBodyDialog = true }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStatPill(
                            label = str("Güncel / Hedef", "Current / Goal"),
                            value = "${state.weightKg} / ${state.targetWeightKg} kg",
                            modifier = Modifier.weight(1.25f)
                        )
                        ProfileStatPill(
                            label = str("Boy & Yaş", "Height & Age"),
                            value = "${state.heightCm.toInt()} cm • ${state.age}",
                            modifier = Modifier.weight(1.1f)
                        )
                        ProfileStatPill(
                            label = str("Cinsiyet", "Gender"),
                            value = if (state.gender == "FEMALE") str("Kadın", "Female") else str("Erkek", "Male"),
                            modifier = Modifier.weight(0.8f)
                        )
                        ProfileStatPill(
                            label = "BMI (${state.localizedBmiCategory(lang)})",
                            value = "${state.bmi}",
                            highlight = true,
                            modifier = Modifier.weight(0.95f)
                        )
                    }
                }
            }

            // ── Section 2: Fitness & Besin Hedefleri ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = str("Fitness & Besin Hedefleri", "Fitness & Nutrition Goals"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${state.nutritionGoal.calories} kcal • ${state.targetProteinG.toInt()}g P",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 3 Hedef Kartı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FitnessGoalOptionCard(
                            title = str("Kilo Verme", "Lose Weight"),
                            multiplier = "1.2x kg",
                            icon = Icons.Rounded.LocalFireDepartment,
                            isSelected = state.fitnessGoal == FitnessGoal.LOSE_WEIGHT,
                            accentColor = HealthColors.NutrientCarbs,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(ProfileIntent.OnFitnessGoalChanged(FitnessGoal.LOSE_WEIGHT)) }
                        )

                        FitnessGoalOptionCard(
                            title = str("Sabit Kalma", "Maintain"),
                            multiplier = "1.0x kg",
                            icon = Icons.Rounded.Scale,
                            isSelected = state.fitnessGoal == FitnessGoal.MAINTAIN,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(ProfileIntent.OnFitnessGoalChanged(FitnessGoal.MAINTAIN)) }
                        )

                        FitnessGoalOptionCard(
                            title = str("Kaslanma", "Build Muscle"),
                            multiplier = "1.4x kg",
                            icon = Icons.Rounded.FitnessCenter,
                            isSelected = state.fitnessGoal == FitnessGoal.BUILD_MUSCLE,
                            accentColor = HealthColors.NutrientProtein,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(ProfileIntent.OnFitnessGoalChanged(FitnessGoal.BUILD_MUSCLE)) }
                        )
                    }

                    // Kompakt Özet & Makro Düzenleme Satırı
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = str(
                                        "Günlük Protein: ${state.targetProteinG.toInt()}g (${state.weightKg}kg × ${state.fitnessGoal.proteinMultiplier}x)",
                                        "Daily Protein: ${state.targetProteinG.toInt()}g (${state.weightKg}kg × ${state.fitnessGoal.proteinMultiplier}x)"
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = str(
                                        "Karb: ${state.nutritionGoal.carbsG.toInt()}g • Yağ: ${state.nutritionGoal.fatG.toInt()}g • Lif: ${state.nutritionGoal.fiberG.toInt()}g",
                                        "Carbs: ${state.nutritionGoal.carbsG.toInt()}g • Fat: ${state.nutritionGoal.fatG.toInt()}g • Fiber: ${state.nutritionGoal.fiberG.toInt()}g"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            FilledTonalButton(
                                onClick = { showEditGoalsDialog = true },
                                modifier = Modifier.bounceClick(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = str("Makrolar", "Macros"),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
            } else if (selectedTab == ProfileTab.NOTIFICATIONS_THEME) {
                // ── Section: Hatırlatıcılar & Bildirim Sıklıkları (Özel Kontrol Kartı) ──
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HealthColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = HealthColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = str("Hatırlatıcılar & Bildirim Sıklıkları", "Reminders & Notification Cadence"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = str("Akıllı teşvik ve tartılma ritmi ayarları", "Smart workout & weigh-in rhythm settings"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                        // 1. Antrenman / Spora Teşvik Bildirimi Satırı
                        Surface(
                            onClick = { showWorkoutReminderDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(HealthColors.Primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FitnessCenter,
                                            contentDescription = null,
                                            tint = HealthColors.Primary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = str("Spora Teşvik Bildirimi", "Workout Motivation Alerts"),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = state.workoutReminderFrequency.localizedTitle(lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HealthColors.Primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = state.workoutReminderFrequency.localizedDescription(lang),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = str("Düzenle", "Edit"),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 2. Kilo / Tartılma Bildirimi Satırı
                        Surface(
                            onClick = { showWeightReminderDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(HealthColors.NutrientProtein.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Scale,
                                            contentDescription = null,
                                            tint = HealthColors.NutrientProtein,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = str("Tartılma Hatırlatıcısı", "Weigh-in Reminder"),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = state.weightReminderFrequency.localizedTitle(lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HealthColors.NutrientProtein,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = str(
                                                "Her sabah 08:30'da güncel vücut ağırlığını kaydetmen için uyarır.",
                                                "Reminds you every morning at 08:30 to log your current weight."
                                            ),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = str("Düzenle", "Edit"),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // ── Home Screen Widget Card ──
                if (onOpenWidgetPromo != null) {
                    GlassCard(
                        onClick = onOpenWidgetPromo,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Widgets,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = str("Ana Ekran Widget'ı", "Home Screen Widget"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = str(
                                            "Canlı kalori halkası, makrolar ve tek dokunuşla +250ml su",
                                            "Live calorie ring, macros & one-tap +250ml hydration"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Language Selector Card ──
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = str("Uygulama Dili", "App Language"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = state.language.nativeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        LanguageSelectorPill(
                            selectedLanguage = state.language,
                            onLanguageSelected = { onIntent(ProfileIntent.OnLanguageChanged(it)) },
                            showNativeNames = true
                        )
                    }
                }

                // ── Section 3: Görünüm & Tema (Tek Birleşik Kart) ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (state.isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = str("Görünüm & Tema", "Appearance & Theme"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (state.isDarkMode) str("Koyu tema aktif", "Dark theme active") else str("Açık tema aktif", "Light theme active"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = state.isDarkMode,
                            onCheckedChange = { onIntent(ProfileIntent.OnThemeModeChanged(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppThemeColor.entries.forEach { accent ->
                            val isSelected = state.themeAccent == accent
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .bounceClick()
                                    .clickable { onIntent(ProfileIntent.OnThemeAccentChanged(accent)) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(accent.gradient))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = str("Seçili", "Selected"),
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = accent.localizedTitle(lang).substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            } else if (selectedTab == ProfileTab.DEVICES_ACCOUNT) {
                // ── Section 4: Akıllı Saat & Tartı Köprüsü ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isDeviceSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Watch,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = str("Akıllı Saat & Tartı (${deviceHealthBridge.platformName})", "Smart Watch & Scale (${deviceHealthBridge.platformName})"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = state.lastDeviceSyncSummary
                                        ?: str("Tartı ve saat verilerini eşitle", "Sync smart scale and watch data"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (deviceHealthBridge.status != HealthPlatformStatus.NOT_SUPPORTED) {
                            IconButton(
                                onClick = { deviceHealthBridge.openHealthSettings() },
                                modifier = Modifier.size(32.dp).bounceClick()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = str("Health Connect Ayarları", "Health Settings"),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                onIntent(ProfileIntent.OnDeviceSyncStarted)
                                deviceHealthBridge.syncLastDays(days = 30) { result ->
                                    onIntent(ProfileIntent.OnDeviceSnapshotReceived(result))
                                }
                            },
                            enabled = !state.isDeviceSyncing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).bounceClick(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isDeviceSyncing) str("Okunuyor...", "Reading...") else str("Cihazları Eşitle", "Sync Devices"),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = { showDeviceGuideDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.bounceClick(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = str("Nasıl Bağlanır?", "How to Connect?"),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = str("Rehber", "Guide"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // ── Yapay Zeka (Özel Gemini API Key) ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                val hasCustomKey = state.customGeminiApiKey.isNotBlank()
                val maskedKey = if (hasCustomKey) {
                    val k = state.customGeminiApiKey
                    if (k.length > 8) "${k.take(4)}••••${k.takeLast(4)}" else "••••••••"
                } else {
                    null
                }
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = str("Yapay Zeka (Özel API Key)", "AI (Custom API Key)"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = if (hasCustomKey) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (hasCustomKey) str("Aktif", "Active") else str("Varsayılan", "Default"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (hasCustomKey) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = if (maskedKey != null) {
                            str(
                                "Kayıtlı anahtar: $maskedKey • Yemek ve egzersiz analizleri kendi Gemini kotanızla çalışır.",
                                "Saved key: $maskedKey • Food and workout analyses use your own Gemini quota."
                            )
                        } else {
                            val quotaSuffix = state.aiQuota?.let { q ->
                                if (q.isPremium) {
                                    str(
                                        "\nGünlük Premium AI hakkınız: ${q.dailyRemaining}/${q.dailyLimit}",
                                        "\nDaily Premium AI quota left: ${q.dailyRemaining}/${q.dailyLimit}"
                                    )
                                } else {
                                    str(
                                        "\nGünlük ücretsiz AI hakkınız: ${q.dailyRemaining}/${q.dailyLimit}",
                                        "\nDaily free AI quota left: ${q.dailyRemaining}/${q.dailyLimit}"
                                    )
                                }
                            }.orEmpty()
                            str(
                                "İsterseniz kendi Google Gemini API anahtarınızı (aistudio.google.com) girerek AI analizlerini kendi kotanızla kullanabilirsiniz.",
                                "Optionally enter your own Google Gemini API key (aistudio.google.com) to run AI analyses on your own quota."
                            ) + quotaSuffix
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showCustomApiKeyDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = if (hasCustomKey) str("API Anahtarını Düzenle", "Edit API Key") else str("Özel API Key Ekle", "Add Custom API Key"),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Yasal Bilgiler & Tıbbi Uyarı ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = str("Yasal Bilgiler ve Gizlilik", "Legal & Privacy"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { uriHandler.openUri("https://pulse-7b4z.onrender.com/privacy") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = str("Gizlilik Politikası", "Privacy Policy"),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { uriHandler.openUri("https://pulse-7b4z.onrender.com/terms") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = str("Kullanım Koşulları", "Terms of Service"),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = str(
                            "Tıbbi Uyarı: HealthTrack yalnızca kişisel takip amaçlıdır; tıbbi tavsiye, teşhis veya tedavi yerine geçmez. Sağlık kararlarınızda mutlaka hekiminize danışınız.",
                            "Medical Disclaimer: HealthTrack is for personal tracking only and does not replace medical advice, diagnosis, or treatment. Always consult your physician for health decisions."
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── İnteraktif Tanıtım Turunu Tekrar İzle ──
            OutlinedButton(
                onClick = onReplayOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = str("İnteraktif Tanıtım Turunu İzle", "Replay Interactive App Tour"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // ── Section 5: Çıkış Yap & Hesap Silme ──
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = str("Çıkış Yap", "Sign Out"),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = str("Hesaptan Çıkış Yap", "Sign Out"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            TextButton(
                onClick = { showDeleteAccountDialog = true },
                enabled = !state.isDeletingAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = str("Hesabı Sil", "Delete Account"),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.isDeletingAccount) str("Hesap Siliniyor...", "Deleting Account...") else str("Hesabı Kalıcı Olarak Sil", "Permanently Delete Account"),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                )
            }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Hatırlatıcı Düzenleme Diyalogları ──
    if (showWorkoutReminderDialog) {
        WorkoutReminderDialog(
            currentFrequency = state.workoutReminderFrequency,
            onDismiss = { showWorkoutReminderDialog = false },
            onSave = {
                onIntent(ProfileIntent.OnWorkoutReminderFrequencyChanged(it))
                showWorkoutReminderDialog = false
            }
        )
    }

    if (showWeightReminderDialog) {
        WeightReminderDialog(
            currentFrequency = state.weightReminderFrequency,
            onDismiss = { showWeightReminderDialog = false },
            onSave = {
                onIntent(ProfileIntent.OnWeightReminderFrequencyChanged(it))
                showWeightReminderDialog = false
            }
        )
    }

    // ── Fiziksel Profil Düzenleme Diyaloğu (Güncel Kilo Hariç: Hedef Kilo, Boy, Yaş, Cinsiyet) ──
    if (showEditBodyDialog) {
        EditBodyProfileDialog(
            state = state,
            onIntent = onIntent,
            onDismiss = { showEditBodyDialog = false }
        )
    }

    // ── Detaylı Besin Hedefleri Düzenleme Diyaloğu ──
    if (showEditGoalsDialog) {
        EditGoalsDialog(
            currentGoal = state.nutritionGoal,
            currentWeight = state.weightKg,
            fitnessGoal = state.fitnessGoal,
            onDismiss = { showEditGoalsDialog = false },
            onSave = { updatedGoal, updatedFitnessGoal ->
                onIntent(ProfileIntent.OnSaveNutritionGoal(updatedGoal, updatedFitnessGoal))
                showEditGoalsDialog = false
            }
        )
    }

    if (showDeviceGuideDialog) {
        DeviceConnectionGuideDialog(
            platformName = deviceHealthBridge.platformName,
            onOpenHealthSettings = { deviceHealthBridge.openHealthSettings() },
            onSyncNow = {
                onIntent(ProfileIntent.OnDeviceSyncStarted)
                deviceHealthBridge.syncLastDays(days = 30) { result ->
                    onIntent(ProfileIntent.OnDeviceSnapshotReceived(result))
                }
            },
            onDismiss = { showDeviceGuideDialog = false }
        )
    }

    if (showCustomApiKeyDialog) {
        var keyInput by remember(state.customGeminiApiKey) { mutableStateOf(state.customGeminiApiKey) }
        var showKey by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCustomApiKeyDialog = false },
            modifier = Modifier.imePadding(),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = str("Özel Gemini API Key", "Custom Gemini API Key"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = str(
                            "Kendi Google Gemini API anahtarınızı (aistudio.google.com) girerek yemek ve spor AI analizlerini kendi kotanızla çalıştırabilirsiniz. Anahtarınız yalnızca cihazınızda saklanır.",
                            "Enter your own Google Gemini API key (aistudio.google.com) to run meal and workout AI analyses with your own quota. Your key is stored only on this device."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIza...") },
                        singleLine = true,
                        visualTransformation = if (showKey) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onIntent(ProfileIntent.OnSaveCustomGeminiApiKey(keyInput))
                        showCustomApiKeyDialog = false
                    },
                    enabled = keyInput.trim().isNotBlank()
                ) {
                    Text(str("Kaydet", "Save"))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.customGeminiApiKey.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onIntent(ProfileIntent.OnClearCustomGeminiApiKey)
                                showCustomApiKeyDialog = false
                            }
                        ) {
                            Text(str("Kaldır", "Remove"), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showCustomApiKeyDialog = false }) {
                        Text(str("Vazgeç", "Cancel"))
                    }
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            modifier = Modifier.imePadding(),
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = str("Çıkış Yap", "Sign Out"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = str(
                        "Hesabınızdan çıkış yapmak istediğinize emin misiniz?",
                        "Are you sure you want to sign out of your account?"
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onIntent(ProfileIntent.OnLogoutClicked)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(str("Çıkış Yap", "Sign Out"), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(str("Vazgeç", "Cancel"))
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isDeletingAccount) showDeleteAccountDialog = false },
            modifier = Modifier.imePadding(),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = str("Hesabı Kalıcı Olarak Sil", "Permanently Delete Account"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = str(
                        "Hesabınızı sildiğinizde tüm HealthTrack ve Planora verileriniz (beslenme, antrenman, kilo, randevular, görevler ve profil) kalıcı olarak silinir. Bu işlem geri alınamaz.",
                        "Deleting your account will permanently erase all your HealthTrack and Planora data (nutrition, workouts, weight, appointments, tasks, and profile). This action cannot be undone."
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onIntent(ProfileIntent.OnDeleteAccountConfirmed)
                    },
                    enabled = !state.isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(str("Evet, Hesabımı Sil", "Yes, Delete My Account"), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !state.isDeletingAccount
                ) {
                    Text(str("Vazgeç", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun ProfileStatPill(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun EditBodyProfileDialog(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    onDismiss: () -> Unit
) {
    var targetWeightInput by remember(state.targetWeightKg) { mutableStateOf(state.targetWeightKg.toString()) }
    var heightInput by remember(state.heightCm) { mutableStateOf(state.heightCm.toInt().toString()) }
    var ageInput by remember(state.age) { mutableStateOf(state.age.toString()) }
    var selectedGender by remember(state.gender) { mutableStateOf(state.gender) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = str("Fiziksel Bilgileri Düzenle", "Edit Body Profile"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = str("Kapat", "Close"), modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearFocusOnTap()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = str(
                        "Güncel kilonuz (${state.weightKg} kg) Kilo sekmesinden otomatik güncellenir.",
                        "Your current weight (${state.weightKg} kg) updates automatically from the Weight tab."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Hedef Kilo
                OutlinedTextField(
                    value = targetWeightInput,
                    onValueChange = { targetWeightInput = it },
                    label = { Text(str("Hedef Kilo (kg)", "Target Weight (kg)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Boy & Yaş Yan Yana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text(str("Boy (cm)", "Height (cm)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it },
                        label = { Text(str("Yaş", "Age")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Biyolojik Cinsiyet Seçimi
                Text(
                    text = str("Biyolojik Cinsiyet", "Biological Sex"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GenderOptionCard(
                        title = str("Erkek", "Male"),
                        icon = Icons.Rounded.Male,
                        isSelected = selectedGender == "MALE",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedGender = "MALE" }
                    )

                    GenderOptionCard(
                        title = str("Kadın", "Female"),
                        icon = Icons.Rounded.Female,
                        isSelected = selectedGender == "FEMALE",
                        accentColor = HealthColors.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedGender = "FEMALE" }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    targetWeightInput.replace(',', '.').toDoubleOrNull()?.let {
                        onIntent(ProfileIntent.OnTargetWeightChanged(it))
                    }
                    heightInput.replace(',', '.').toDoubleOrNull()?.let {
                        onIntent(ProfileIntent.OnHeightChanged(it))
                    }
                    ageInput.toIntOrNull()?.let {
                        onIntent(ProfileIntent.OnAgeChanged(it))
                    }
                    if (selectedGender != state.gender) {
                        onIntent(ProfileIntent.OnGenderChanged(selectedGender))
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(str("Kaydet", "Save"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(str("İptal", "Cancel"))
            }
        }
    )
}

@Composable
private fun FitnessGoalOptionCard(
    title: String,
    multiplier: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.15f) else if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.5f)

    Surface(
        modifier = modifier
            .bounceClick()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.5.sp
                ),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = multiplier,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GenderOptionCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.15f) else if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.5f)

    Surface(
        modifier = modifier
            .bounceClick()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
