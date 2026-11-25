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
import com.yusufteker.healthtrack.core.model.FriendUserDto
import com.yusufteker.healthtrack.core.model.HealthPlatformStatus
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
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
import com.yusufteker.healthtrack.ui.components.PlanoraFindFriendsButton
import com.yusufteker.healthtrack.ui.components.PlanoraFollowListSheet
import com.yusufteker.healthtrack.ui.components.PlanoraFriendProfileSheet
import com.yusufteker.healthtrack.ui.components.PlanoraIncomingRequestsCard
import com.yusufteker.healthtrack.ui.components.PlanoraProfileHeroCard
import com.yusufteker.healthtrack.ui.components.PlanoraSearchUsersSheet
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
    onOpenGuestAuth: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onOpenWidgetPromo: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    isDeviceAuthorized: Boolean = state.hasConnectedDevice,
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
    var showHuaweiDisconnectDialog by remember { mutableStateOf(false) }
    var followListTabToOpen by remember { mutableStateOf<Int?>(null) }
    var showSearchUsersSheet by remember { mutableStateOf(false) }
    var selectedFriendForProfileSheet by remember { mutableStateOf<FriendUserDto?>(null) }
    val uriHandler = LocalUriHandler.current

    if (followListTabToOpen != null) {
        PlanoraFollowListSheet(
            initialTab = followListTabToOpen ?: 0,
            followers = state.followers,
            following = state.following,
            requests = state.followRequests,
            isLoading = state.isLoadingSocial,
            pendingActionUserIds = state.pendingActionUserIds,
            mutedWorkoutUserIds = state.mutedWorkoutFriendIds,
            onUserClick = { friend -> selectedFriendForProfileSheet = friend },
            onRemoveFollower = { userId -> onIntent(ProfileIntent.OnRemoveFollower(userId)) },
            onUnfollow = { userId -> onIntent(ProfileIntent.OnToggleFollowUser(userId)) },
            onAcceptRequest = { reqId -> onIntent(ProfileIntent.OnAcceptFollowRequest(reqId)) },
            onRejectRequest = { reqId -> onIntent(ProfileIntent.OnRejectFollowRequest(reqId)) },
            onOpenSearch = {
                followListTabToOpen = null
                showSearchUsersSheet = true
            },
            onDismiss = { followListTabToOpen = null }
        )
    }

    if (selectedFriendForProfileSheet != null) {
        val targetFriend = selectedFriendForProfileSheet!!
        PlanoraFriendProfileSheet(
            user = targetFriend,
            isWorkoutTrackingEnabled = !state.mutedWorkoutFriendIds.contains(targetFriend.id),
            onToggleWorkoutTracking = {
                onIntent(ProfileIntent.OnToggleFriendWorkoutTracking(targetFriend.id))
            },
            onUnfollow = if (targetFriend.isFollowedByMe) {
                {
                    onIntent(ProfileIntent.OnToggleFollowUser(targetFriend.id))
                    selectedFriendForProfileSheet = null
                }
            } else null,
            onDismiss = { selectedFriendForProfileSheet = null }
        )
    }

    if (showSearchUsersSheet) {
        PlanoraSearchUsersSheet(
            query = state.friendSearchQuery,
            results = state.searchResults,
            isLoading = state.isSearchingFriends,
            pendingActionUserIds = state.pendingActionUserIds,
            onQueryChanged = { query -> onIntent(ProfileIntent.OnFriendSearchQueryChanged(query)) },
            onToggleFollow = { userId -> onIntent(ProfileIntent.OnToggleFollowUser(userId)) },
            onDismiss = { showSearchUsersSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = str("Profil", "Profile"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
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

            // ── HealthTrack Premium Banner (hidden in Guest mode; Guests see the Account Upgrade card below) ──
            if (!state.isGuest) {
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
                                    "Günlük 3 AI analiz sınırını kaldır • Yemek, fotoğraf ve spor AI",
                                    "Remove the 3/day AI limit • Meal, photo & workout AI"
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
                        .height(IntrinsicSize.Max)
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
                                .fillMaxHeight()
                                .bounceClick()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = str(tab.titleTr, tab.titleEn),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        lineHeight = 13.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab == ProfileTab.PROFILE_GOALS) {
                if (state.isGuest) {
                    // ── Guest Mode Hero & Upgrade Card ──
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        elevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = str("Misafir Modu", "Guest Mode"),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = str(
                                            "Verilerin yalnızca bu cihazda tutuluyor",
                                            "Your data is stored locally on this device"
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = str(
                                    "Giriş yaparak veya hesap oluşturarak tüm öğün, kilo, antrenman ve hedef verilerini hesabına aktarabilir, bulut yedekleme ve liderlik tablosunu açabilirsin.",
                                    "Sign in or create an account to transfer all your meals, weight, workouts & goals, and unlock cloud sync and leaderboards."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = onOpenGuestAuth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = str("Giriş Yap / Verileri Hesaba Aktar", "Sign In / Transfer Data to Account"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                } else {
                    // ── Section 1: Planora 1:1 Sosyal Profil Kahraman Kartı (Fotoğraf + Seri / Takipçi / Takip Edilen) ──
                    PlanoraProfileHeroCard(
                        name = state.name,
                        username = state.username,
                        profileImageUrl = state.profileImageUrl,
                        streakDays = state.streakDays,
                        followersCount = state.followersCount,
                        followingCount = state.followingCount,
                        isPremium = state.isPremium,
                        isUploadingImage = state.isUploadingProfileImage,
                        onUploadProfileImageBytes = { bytes ->
                            onIntent(ProfileIntent.OnUploadProfileImage(bytes))
                        },
                        onFollowersClick = {
                            onIntent(ProfileIntent.OnRefreshSocial)
                            followListTabToOpen = 0
                        },
                        onFollowingClick = {
                            onIntent(ProfileIntent.OnRefreshSocial)
                            followListTabToOpen = 1
                        },
                        onStreakClick = null
                    )

                    // ── Section 1b: Arkadaş Bul & Ekle Pill Butonu ──
                    PlanoraFindFriendsButton(
                        onClick = {
                            onIntent(ProfileIntent.OnRefreshSocial)
                            showSearchUsersSheet = true
                        }
                    )

                    // ── Section 1c: Gelen Arkadaşlık İstekleri Kartı ──
                    if (state.followRequests.isNotEmpty()) {
                        PlanoraIncomingRequestsCard(
                            requests = state.followRequests,
                            onAccept = { requestId -> onIntent(ProfileIntent.OnAcceptFollowRequest(requestId)) },
                            onReject = { requestId -> onIntent(ProfileIntent.OnRejectFollowRequest(requestId)) },
                            onSeeAllClick = {
                                onIntent(ProfileIntent.OnRefreshSocial)
                                followListTabToOpen = 2
                            }
                        )
                    }
                }

                // ── Section 1d: Kompakt Fiziksel Bilgiler & BMI Şeridi ──
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    elevation = 4.dp
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
                            Text(
                                text = str("Fiziksel Bilgiler & BMI", "Body Stats & BMI"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
                                label = "BMI",
                                value = "${state.bmi}",
                                subLabel = state.localizedBmiCategory(lang),
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

                        if (!hasNotificationPermission) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = str("Bildirim İzni Kapalı", "Notifications Blocked"),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = str(
                                                "Hatırlatıcılar ve arkadaş bildirimleri için izin verin.",
                                                "Allow permission for reminders and friend alerts."
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = onRequestNotificationPermission,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = str("İzin Ver", "Allow"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

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
                                        val weightReminderTimeText = if (state.weightReminderFrequency == WeightReminderFrequency.OFF) {
                                            ""
                                        } else {
                                            val h = state.weightReminderHour.toString().padStart(2, '0')
                                            val m = state.weightReminderMinute.toString().padStart(2, '0')
                                            " · $h:$m"
                                        }
                                        Text(
                                            text = state.weightReminderFrequency.localizedTitle(lang) + weightReminderTimeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HealthColors.NutrientProtein,
                                            fontWeight = FontWeight.SemiBold
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

                        // 3. Öğün / Öğle Yemeği Bildirimi Satırı (13:30)
                        Surface(
                            onClick = { onIntent(ProfileIntent.OnFoodReminderChanged(!state.foodReminderEnabled)) },
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
                                            .background(HealthColors.Primary.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Restaurant,
                                            contentDescription = null,
                                            tint = HealthColors.Primary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = str("Öğün Kontrol Hatırlatıcısı", "Meal Check-in Reminder"),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (state.foodReminderEnabled) {
                                                str("Açık · 13:30 (öğün girilmemişse)", "Enabled · 1:30 PM (if no meal logged)")
                                            } else {
                                                str("Kapalı", "Disabled")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (state.foodReminderEnabled) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Switch(
                                    checked = state.foodReminderEnabled,
                                    onCheckedChange = { onIntent(ProfileIntent.OnFoodReminderChanged(it)) }
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = str("Akıllı Saat & Tartı", "Smart Watch & Scale"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    val (badgeText, badgeColor) = when {
                                        isDeviceAuthorized || state.hasConnectedDevice || state.isHuaweiConnected ->
                                            str("Bağlı ✓", "Connected ✓") to Color(0xFF10B981)
                                        deviceHealthBridge.status == HealthPlatformStatus.NOT_SUPPORTED ->
                                            str("Desteklenmiyor", "Unsupported") to MaterialTheme.colorScheme.onSurfaceVariant
                                        deviceHealthBridge.status == HealthPlatformStatus.NOT_INSTALLED ->
                                            str("Kurulu Değil", "Not Installed") to Color(0xFFF97316)
                                        else ->
                                            str("İzin Gerekli", "Permission Needed") to Color(0xFFF59E0B)
                                    }
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(
                                    text = state.lastDeviceSyncSummary
                                        ?: str(
                                            "${deviceHealthBridge.platformName} · Tartı ve saat verilerini eşitle",
                                            "${deviceHealthBridge.platformName} · Sync smart scale & watch data"
                                        ),
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    HuaweiCloudSyncSection(
                        isConnected = state.isHuaweiConnected,
                        needsReauth = state.isHuaweiNeedsReauth,
                        isBusy = state.isHuaweiBusy,
                        lastSyncSummary = state.lastHuaweiSyncSummary,
                        onConnect = { onIntent(ProfileIntent.OnConnectHuaweiClicked) },
                        onSyncNow = { onIntent(ProfileIntent.OnSyncHuaweiClicked) },
                        onRequestDisconnect = { showHuaweiDisconnectDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = str("Otomatik Cihaz Senkronizasyonu", "Automatic Device Sync"),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = str(
                                    "Uygulama açılışında ve ön plana gelince arka planda sessizce günceller.",
                                    "Silently syncs measurements & workouts on launch and resume."
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = state.isAutoDeviceSyncEnabled,
                            onCheckedChange = { onIntent(ProfileIntent.OnAutoDeviceSyncChanged(it)) }
                        )
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
                        text = if (state.isGuest) {
                            str(
                                "AI öğün/spor analizi ve özel API anahtarı kullanımı için ücretsiz hesap oluşturmanız gerekir. Hesap açtığınızda günde 3 ücretsiz AI hakkı tanımlanır.",
                                "A free account is required for AI meal/workout analysis and custom API keys. Signing up includes 3 free AI analyses per day."
                            )
                        } else if (maskedKey != null) {
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
                        onClick = {
                            if (state.isGuest) onOpenGuestAuth() else showCustomApiKeyDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = when {
                                state.isGuest -> str("Ücretsiz Hesap Oluştur / Giriş Yap", "Create Free Account / Sign In")
                                hasCustomKey -> str("API Anahtarını Düzenle", "Edit API Key")
                                else -> str("Özel API Key Ekle", "Add Custom API Key")
                            },
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
                            "HealthTrack yalnızca kişisel takip amaçlıdır; tıbbi tavsiye yerine geçmez.",
                            "HealthTrack is for personal tracking only and does not replace medical advice."
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Tanıtım Turu ──
            OutlinedButton(
                onClick = onReplayOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = str("Tanıtım Turunu İzle", "Replay App Tour"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // ── Section 5: Hesap Yönetimi (Çıkış & Silme) ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = str("Hesap Yönetimi", "Account Management"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.isGuest) {
                        Button(
                            onClick = onOpenGuestAuth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = str("Giriş Yap / Hesaba Aktar", "Sign In / Transfer to Account"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = if (state.isGuest) str("Misafir Modundan Çık", "Exit Guest Mode") else str("Çıkış Yap", "Sign Out"),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isGuest) str("Misafir Modundan Çık", "Exit Guest Mode") else str("Çıkış Yap", "Sign Out"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (!state.isGuest) {
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
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
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
            initialHour = state.weightReminderHour,
            initialMinute = state.weightReminderMinute,
            onDismiss = { showWeightReminderDialog = false },
            onSave = { freq, hour, minute ->
                onIntent(ProfileIntent.OnWeightReminderSettingsChanged(freq, hour, minute))
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
        CustomApiKeyDialog(
            initialKey = state.customGeminiApiKey,
            onSave = { onIntent(ProfileIntent.OnSaveCustomGeminiApiKey(it)) },
            onClear = { onIntent(ProfileIntent.OnClearCustomGeminiApiKey) },
            onDismiss = { showCustomApiKeyDialog = false }
        )
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                onIntent(ProfileIntent.OnLogoutClicked)
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountConfirmDialog(
            isDeleting = state.isDeletingAccount,
            onConfirm = {
                showDeleteAccountDialog = false
                onIntent(ProfileIntent.OnDeleteAccountConfirmed)
            },
            onDismiss = { showDeleteAccountDialog = false }
        )
    }

    if (showHuaweiDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showHuaweiDisconnectDialog = false },
            title = {
                Text(
                    text = str("Huawei Bağlantısını Kes?", "Disconnect Huawei Health?"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = str(
                        "Daha önce içe aktarılmış tartım ve antrenman verileriniz korunur, ancak yeni Huawei Sağlık verileri otomatik çekilmez.",
                        "Your previously imported weigh-ins and workouts remain saved, but new Huawei Health data will stop syncing automatically."
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHuaweiDisconnectDialog = false
                        onIntent(ProfileIntent.OnDisconnectHuaweiClicked)
                    }
                ) {
                    Text(
                        text = str("Bağlantıyı Kes", "Disconnect"),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showHuaweiDisconnectDialog = false }) {
                    Text(str("İptal", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun HuaweiCloudSyncSection(
    isConnected: Boolean,
    needsReauth: Boolean,
    isBusy: Boolean,
    lastSyncSummary: String?,
    onConnect: () -> Unit,
    onSyncNow: () -> Unit,
    onRequestDisconnect: () -> Unit
) {
    val huaweiAccent = Color(0xFFEF4444)
    val (statusLabel, statusColor) = when {
        needsReauth -> str("Yeniden Bağla", "Reconnect") to Color(0xFFF59E0B)
        isConnected -> str("Bağlı ✓", "Connected ✓") to Color(0xFF10B981)
        else -> str("Bağlı Değil", "Not Connected") to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(huaweiAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = huaweiAccent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CloudSync,
                            contentDescription = null,
                            tint = huaweiAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = str("Huawei Sağlık (Doğrudan Bulut)", "Huawei Health (Direct Cloud)"),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
        }

        Text(
            text = lastSyncSummary ?: if (isConnected && !needsReauth) {
                str(
                    "Health Sync olmadan adım, egzersiz, nabız ve kilo doğrudan çekilir.",
                    "Steps, workouts, heart rate & weight sync directly without Health Sync."
                )
            } else {
                str(
                    "Huawei Watch ve bileklik verilerini Health Sync olmadan doğrudan bağlayın.",
                    "Connect Huawei Watch & Band directly with no bridge app needed."
                )
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!isConnected || needsReauth) {
            Button(
                onClick = onConnect,
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().bounceClick(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = huaweiAccent,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isBusy -> str("Hazırlanıyor...", "Preparing...")
                        needsReauth -> str("Huawei Sağlık'ı Yeniden Bağla", "Reconnect Huawei Health")
                        else -> str("Huawei Sağlık ile Bağlan", "Connect Huawei Health")
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSyncNow,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).bounceClick(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = huaweiAccent.copy(alpha = 0.16f),
                        contentColor = huaweiAccent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBusy) str("Eşitleniyor...", "Syncing...") else str("Huawei Eşitle", "Sync Huawei"),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onRequestDisconnect,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.bounceClick(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = str("Bağlantıyı Kes", "Disconnect"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
