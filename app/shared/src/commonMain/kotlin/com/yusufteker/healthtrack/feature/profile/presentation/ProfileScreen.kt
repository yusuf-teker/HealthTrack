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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.HealthPlatformStatus
import com.yusufteker.healthtrack.core.utils.rememberDeviceHealthBridge
import com.yusufteker.healthtrack.feature.food.presentation.components.EditGoalsDialog
import com.yusufteker.healthtrack.theme.AppThemeColor
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.LocalThemeAccent
import com.yusufteker.healthtrack.ui.components.DeviceConnectionGuideDialog
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val currentAccent = LocalThemeAccent.current
    val scrollState = rememberScrollState()
    val deviceHealthBridge = rememberDeviceHealthBridge()

    var weightInput by remember(state.weightKg) { mutableStateOf(state.weightKg.toString()) }
    var targetWeightInput by remember(state.targetWeightKg) { mutableStateOf(state.targetWeightKg.toString()) }
    var heightInput by remember(state.heightCm) { mutableStateOf(state.heightCm.toInt().toString()) }
    var ageInput by remember(state.age) { mutableStateOf(state.age.toString()) }
    var showEditGoalsDialog by remember { mutableStateOf(false) }
    var showDeviceGuideDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Profil & Ayarlar",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Kişisel ölçüleriniz ve tema tercihleri",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Brand Mark "H"
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = MaterialTheme.colorScheme.onErrorContainer)
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
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // ── Section 1: User Identity Card ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
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
                            text = if (state.name.isNotBlank()) state.name else "Kullanıcı",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "@${state.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.latestWeightDate != null) {
                            Text(
                                text = "Son Tartım: ${state.latestWeightDate}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ── Section 2: Fitness Hedefi & Besin Hedefleri (En Üst Bölüm) ──
            Text(
                text = "Fitness & Besin Hedefleri",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Hedefinize Göre Günlük Protein İhtiyacı",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Vücut ağırlığınıza ve hedefinize göre günlük alınması gereken protein miktarı otomatik hesaplanır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 3 Hedef Kartı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FitnessGoalOptionCard(
                            title = "Kilo Verme",
                            multiplier = "1.2x kg",
                            icon = Icons.Rounded.LocalFireDepartment,
                            isSelected = state.fitnessGoal == FitnessGoal.LOSE_WEIGHT,
                            accentColor = HealthColors.NutrientCarbs,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(ProfileIntent.OnFitnessGoalChanged(FitnessGoal.LOSE_WEIGHT)) }
                        )

                        FitnessGoalOptionCard(
                            title = "Sabit Kalma",
                            multiplier = "1.0x kg",
                            icon = Icons.Rounded.Scale,
                            isSelected = state.fitnessGoal == FitnessGoal.MAINTAIN,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(ProfileIntent.OnFitnessGoalChanged(FitnessGoal.MAINTAIN)) }
                        )

                        FitnessGoalOptionCard(
                            title = "Kaslanma",
                            multiplier = "1.4x kg",
                            icon = Icons.Rounded.FitnessCenter,
                            isSelected = state.fitnessGoal == FitnessGoal.BUILD_MUSCLE,
                            accentColor = HealthColors.NutrientProtein,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(ProfileIntent.OnFitnessGoalChanged(FitnessGoal.BUILD_MUSCLE)) }
                        )
                    }

                    // Dinamik Protein Telemetrisi & Bilgi Notu
                    Surface(
                        color = when (state.fitnessGoal) {
                            FitnessGoal.BUILD_MUSCLE -> HealthColors.NutrientProtein.copy(alpha = 0.12f)
                            FitnessGoal.LOSE_WEIGHT -> HealthColors.NutrientCarbs.copy(alpha = 0.12f)
                            FitnessGoal.MAINTAIN -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            when (state.fitnessGoal) {
                                FitnessGoal.BUILD_MUSCLE -> HealthColors.NutrientProtein.copy(alpha = 0.35f)
                                FitnessGoal.LOSE_WEIGHT -> HealthColors.NutrientCarbs.copy(alpha = 0.35f)
                                FitnessGoal.MAINTAIN -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Günlük Hedef Protein",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${state.targetProteinG.toInt()}",
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                            color = when (state.fitnessGoal) {
                                                FitnessGoal.BUILD_MUSCLE -> HealthColors.NutrientProtein
                                                FitnessGoal.LOSE_WEIGHT -> HealthColors.NutrientCarbs
                                                FitnessGoal.MAINTAIN -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "g / gün",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 3.dp)
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${state.weightKg} kg × ${state.fitnessGoal.proteinMultiplier}x",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Text(
                                text = when (state.fitnessGoal) {
                                    FitnessGoal.BUILD_MUSCLE -> "Kas hipertrofisi ve hacim artışı için vücut ağırlığınızın en az 1.4 katı protein almanız gerekmektedir. Günlük tartımınıza göre ana sayfada otomatik güncellenir."
                                    FitnessGoal.MAINTAIN -> "Mevcut kas dokunuzu korumak için en az vücut ağırlığınız kadar (1.0x) protein alınmalıdır. Günlük tartımınıza göre ana sayfada otomatik güncellenir."
                                    FitnessGoal.LOSE_WEIGHT -> "Yağ yakımı ve kalori açığı sürecinde kas kaybını önlemek için vücut ağırlığınızın 1.2 katı protein alınmalıdır."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Detaylı Besin & Makro Hedeflerini Düzenleme Butonu
                    OutlinedButton(
                        onClick = { showEditGoalsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tüm Besin & Makro Hedeflerini Düzenle",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // ── Section 3: Vücut Bilgileri & Ölçüler ──
            Text(
                text = "Vücut Ölçüleri",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // Güncel Kilo
            ProfileMetricCard(
                title = "Güncel Kilo",
                subtitle = state.latestWeightDate?.let { "Kilo günlüğündeki son kayıt: $it" } ?: "Kilo günlüğünden otomatik güncellenir",
                valueText = weightInput,
                unit = "kg",
                icon = Icons.Rounded.MonitorWeight,
                accentColor = MaterialTheme.colorScheme.primary,
                onValueChange = {
                    weightInput = it
                    it.toDoubleOrNull()?.let { w -> onIntent(ProfileIntent.OnWeightChanged(w)) }
                },
                onDecrement = {
                    val cur = state.weightKg
                    val next = (cur - 0.5).coerceAtLeast(30.0)
                    val rounded = kotlin.math.round(next * 10) / 10.0
                    weightInput = rounded.toString()
                    onIntent(ProfileIntent.OnWeightChanged(rounded))
                },
                onIncrement = {
                    val cur = state.weightKg
                    val next = (cur + 0.5).coerceAtMost(300.0)
                    val rounded = kotlin.math.round(next * 10) / 10.0
                    weightInput = rounded.toString()
                    onIntent(ProfileIntent.OnWeightChanged(rounded))
                }
            )

            // Hedef Kilo
            ProfileMetricCard(
                title = "Hedef Kilo",
                subtitle = "Ulaşmak istediğiniz kilo hedefi",
                valueText = targetWeightInput,
                unit = "kg",
                icon = Icons.Rounded.Flag,
                accentColor = HealthColors.Tertiary,
                onValueChange = {
                    targetWeightInput = it
                    it.toDoubleOrNull()?.let { t -> onIntent(ProfileIntent.OnTargetWeightChanged(t)) }
                },
                onDecrement = {
                    val cur = state.targetWeightKg
                    val next = (cur - 0.5).coerceAtLeast(30.0)
                    val rounded = kotlin.math.round(next * 10) / 10.0
                    targetWeightInput = rounded.toString()
                    onIntent(ProfileIntent.OnTargetWeightChanged(rounded))
                },
                onIncrement = {
                    val cur = state.targetWeightKg
                    val next = (cur + 0.5).coerceAtMost(300.0)
                    val rounded = kotlin.math.round(next * 10) / 10.0
                    targetWeightInput = rounded.toString()
                    onIntent(ProfileIntent.OnTargetWeightChanged(rounded))
                }
            )

            // Boy
            ProfileMetricCard(
                title = "Boy",
                subtitle = "BKI ve kalori hesabında kullanılır",
                valueText = heightInput,
                unit = "cm",
                icon = Icons.Rounded.Height,
                accentColor = MaterialTheme.colorScheme.secondary,
                onValueChange = {
                    heightInput = it
                    it.toDoubleOrNull()?.let { h -> onIntent(ProfileIntent.OnHeightChanged(h)) }
                },
                onDecrement = {
                    val cur = state.heightCm
                    val next = (cur - 1.0).coerceAtLeast(100.0)
                    heightInput = next.toInt().toString()
                    onIntent(ProfileIntent.OnHeightChanged(next))
                },
                onIncrement = {
                    val cur = state.heightCm
                    val next = (cur + 1.0).coerceAtMost(250.0)
                    heightInput = next.toInt().toString()
                    onIntent(ProfileIntent.OnHeightChanged(next))
                }
            )

            // Yaş
            ProfileMetricCard(
                title = "Yaş",
                subtitle = "Metabolizma ve kalori ihtiyacı için",
                valueText = ageInput,
                unit = "yaş",
                icon = Icons.Rounded.Cake,
                accentColor = HealthColors.NutrientCarbs,
                onValueChange = {
                    ageInput = it
                    it.toIntOrNull()?.let { a -> onIntent(ProfileIntent.OnAgeChanged(a)) }
                },
                onDecrement = {
                    val next = (state.age - 1).coerceAtLeast(10)
                    ageInput = next.toString()
                    onIntent(ProfileIntent.OnAgeChanged(next))
                },
                onIncrement = {
                    val next = (state.age + 1).coerceAtMost(120)
                    ageInput = next.toString()
                    onIntent(ProfileIntent.OnAgeChanged(next))
                }
            )

            // Cinsiyet
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Biyolojik Cinsiyet",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Bazal metabolizma (BMR) formülleri için gereklidir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isMale = state.gender == "MALE"
                        val isFemale = state.gender == "FEMALE"

                        GenderOptionCard(
                            title = "Erkek",
                            icon = Icons.Rounded.Male,
                            isSelected = isMale,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(ProfileIntent.OnGenderChanged("MALE")) }
                        )

                        GenderOptionCard(
                            title = "Kadın",
                            icon = Icons.Rounded.Female,
                            isSelected = isFemale,
                            accentColor = HealthColors.Secondary,
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(ProfileIntent.OnGenderChanged("FEMALE")) }
                        )
                    }
                }
            }

            // BMI (Beden Kitle İndeksi) Hesap Özeti
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Beden Kitle İndeksi (BMI)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.bmi}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "kg/m²",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = state.bmiCategory,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Section 4: Tema ve Görünüm Seçenekleri ──
            Text(
                text = "Görünüm ve Tema",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // Koyu Mod / Açık Mod Switch
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (state.isDarkMode) "Koyu Tema" else "Açık Tema",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (state.isDarkMode) "Obsidiyen gece modu aktif" else "Gündüz açık renk modu aktif",
                                style = MaterialTheme.typography.bodySmall,
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
            }

            // Tema Vurgu Rengi Paleti
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tema Rengi (Vurgu)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Uygulama butonları, grafikler ve simgeler seçilen renge bürünür",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AppThemeColor.entries.forEach { accent ->
                            val isSelected = state.themeAccent == accent
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .bounceClick()
                                    .clickable { onIntent(ProfileIntent.OnThemeAccentChanged(accent)) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(accent.gradient)
                                        )
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seçili",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = accent.title.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 4: Senkronizasyon & Veri ──
            Text(
                text = "Veri ve Senkronizasyon",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // Akıllı Saat & Tartı Köprüsü (Xiaomi Scale & Huawei Watch via Health Connect / HealthKit)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(HealthColors.TertiaryContainer.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isDeviceSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = HealthColors.TertiaryContainer
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Watch,
                                        contentDescription = null,
                                        tint = HealthColors.TertiaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Akıllı Saat & Tartı (${deviceHealthBridge.platformName})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = state.lastDeviceSyncSummary
                                        ?: "Xiaomi Tartı (Zepp Life / Mi Fitness) ve Huawei Watch verilerini çakışmasız eşitle",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (deviceHealthBridge.status != HealthPlatformStatus.NOT_SUPPORTED) {
                            IconButton(
                                onClick = { deviceHealthBridge.openHealthSettings() },
                                modifier = Modifier.size(34.dp).bounceClick()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Health Connect Ayarları",
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
                                imageVector = Icons.Rounded.MonitorWeight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isDeviceSyncing) "Okunuyor..." else "Cihazları Eşitle",
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
                                contentDescription = "Nasıl Bağlanır?",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nasıl Bağlanır?",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // ── Section 5: Otomatik Kayıt & Senkronizasyon Rozeti ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Değişiklikleriniz anında otomatik kaydedilir ve buluta eşitlenir.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // ── Section 6: Çıkış Yap (En Altta) ──
            var showLogoutDialog by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Çıkış Yap",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hesaptan Çıkış Yap",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.error
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
                            text = "Çıkış Yap",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Text(
                            text = "Hesabınızdan çıkış yapmak istediğinize emin misiniz?",
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
                            Text("Çıkış Yap", color = MaterialTheme.colorScheme.onError)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Vazgeç")
                        }
                    }
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileMetricCard(
    title: String,
    subtitle: String,
    valueText: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    onValueChange: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stepper -
                FilledIconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(44.dp).bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Azalt", tint = MaterialTheme.colorScheme.onSurface)
                }

                // Input Field
                OutlinedTextField(
                    value = valueText,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    trailingIcon = {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )

                // Stepper +
                FilledIconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(44.dp).bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Artır", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
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
                .padding(vertical = 12.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
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
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}



