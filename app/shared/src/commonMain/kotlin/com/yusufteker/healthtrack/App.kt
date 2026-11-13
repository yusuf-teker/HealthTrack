package com.yusufteker.healthtrack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russhwolf.settings.Settings
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.CheckAppVersionUseCase
import com.yusufteker.healthtrack.core.model.GamificationSummary
import com.yusufteker.healthtrack.core.model.UpdateStatus
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.GamificationManager
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.sync.WidgetSnapshotBuilder
import com.yusufteker.healthtrack.core.utils.ExternalApp
import com.yusufteker.healthtrack.core.utils.WidgetDeepLinkBus
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.rememberAppVersion
import com.yusufteker.healthtrack.core.utils.rememberDailyReminderNotifier
import com.yusufteker.healthtrack.core.utils.rememberDeviceHealthBridge
import com.yusufteker.healthtrack.core.utils.rememberExternalAppLauncher
import com.yusufteker.healthtrack.core.utils.rememberWeeklyReportNotifier
import com.yusufteker.healthtrack.core.utils.rememberWidgetBridge
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import com.yusufteker.healthtrack.di.appModules
import com.yusufteker.healthtrack.feature.analytics.presentation.AnalyticsScreenRoot
import com.yusufteker.healthtrack.feature.auth.presentation.AuthScreenRoot
import com.yusufteker.healthtrack.feature.food.presentation.FoodScreenRoot
import com.yusufteker.healthtrack.feature.gamification.presentation.GamificationScreenRoot
import com.yusufteker.healthtrack.feature.gamification.presentation.GamificationSection
import com.yusufteker.healthtrack.feature.onboarding.presentation.OnboardingScreenRoot
import com.yusufteker.healthtrack.feature.premium.presentation.PremiumScreenRoot
import com.yusufteker.healthtrack.feature.profile.presentation.ProfileScreenRoot
import com.yusufteker.healthtrack.feature.weight.presentation.WeightScreenRoot
import com.yusufteker.healthtrack.feature.workout.presentation.WorkoutScreenRoot
import com.yusufteker.healthtrack.theme.AppThemeColor
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.HealthTrackTheme
import com.yusufteker.healthtrack.theme.LocalBottomBarVisibility
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.ConfettiCelebrationOverlay
import com.yusufteker.healthtrack.ui.components.ForceUpdateDialog
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.HealthBrandLogo
import com.yusufteker.healthtrack.ui.components.MotivationalToastHost
import com.yusufteker.healthtrack.ui.components.ShimmeringStreakPill
import com.yusufteker.healthtrack.ui.components.WidgetPromoDialog
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class HealthTab(val titleTr: String, val titleEn: String) {
    FOOD("Yemek", "Food"),
    WORKOUT("Spor", "Workout"),
    WEIGHT("Kilo", "Weight"),
    ANALYTICS("Analiz", "Analytics");

    val title: String
        get() = localizedTitle()

    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalUuidApi::class)
@Composable
fun App(initialTab: HealthTab = HealthTab.FOOD) {
    KoinApplication(application = {
        modules(appModules)
    }) {
        val settings: Settings = koinInject()
        remember(settings) { LocalizationManager.init(settings) }
        val currentLanguage by LocalizationManager.languageFlow.collectAsState()
        var isDark by remember {
            mutableStateOf(settings.getBoolean("dark_mode", true))
        }
        var currentThemeColor by remember {
            mutableStateOf(AppThemeColor.fromId(settings.getString("theme_accent", AppThemeColor.OCEAN.id)))
        }

        HealthTrackTheme(
            darkTheme = isDark,
            themeAccent = currentThemeColor,
            language = currentLanguage
        ) {
            val tokenStorage: TokenStorage = koinInject()
            val database: HealthDatabase = koinInject()
            val syncEngine: SyncEngine = koinInject()
            val deviceHealthSyncManager: DeviceHealthSyncManager = koinInject()
            val gamificationManager: GamificationManager = koinInject()
            val apiClient: HealthApiClient = koinInject()
            val checkAppVersionUseCase: CheckAppVersionUseCase = koinInject()
            val widgetSnapshotBuilder: WidgetSnapshotBuilder = koinInject()
            val deviceHealthBridge = rememberDeviceHealthBridge()
            val widgetBridge = rememberWidgetBridge()
            val currentAppVersion = rememberAppVersion()
            val session by tokenStorage.sessionFlow.collectAsState()
            val widgetSnapshot by remember(session.isLoggedIn, session.userId, isDark, currentThemeColor, currentLanguage) {
                widgetSnapshotBuilder.observeTodaySnapshot(session.userId)
            }.collectAsState(initial = widgetSnapshotBuilder.buildTodaySnapshot(session.userId))
            var isWidgetPromoDialogVisible by remember { mutableStateOf(false) }
            var isWidgetInstalled by remember { mutableStateOf(false) }
            val isIosPlatform = remember {
                val pName = getPlatform().name
                pName.contains("iOS", ignoreCase = true) || pName.contains("iPad", ignoreCase = true)
            }
            val gamificationSummary by remember(session.isLoggedIn, session.userId) {
                if (session.isLoggedIn && session.userId > 0) {
                    gamificationManager.observeSummary(session.userId)
                } else {
                    kotlinx.coroutines.flow.flowOf(GamificationSummary())
                }
            }.collectAsState(initial = GamificationSummary())
            val activeCelebration by gamificationManager.currentCelebration.collectAsState()
            val activeMotivationalToast by gamificationManager.currentMotivationalToast.collectAsState()

            var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.UpToDate) }
            var isOptionalUpdateDismissed by remember { mutableStateOf(false) }

            LaunchedEffect(currentAppVersion) {
                apiClient.fetchAppVersionConfig().onSuccess { config ->
                    updateStatus = checkAppVersionUseCase.evaluate(currentAppVersion, config)
                }
            }

            when (val status = updateStatus) {
                is UpdateStatus.ForceUpdateRequired -> {
                    ForceUpdateDialog(config = status.config, isForceUpdate = true)
                }
                is UpdateStatus.OptionalUpdateAvailable -> {
                    if (!isOptionalUpdateDismissed) {
                        ForceUpdateDialog(
                            config = status.config,
                            isForceUpdate = false,
                            onDismiss = { isOptionalUpdateDismissed = true }
                        )
                    }
                }
                UpdateStatus.UpToDate -> {}
            }

            var isOnboardingVisible by remember { mutableStateOf(!tokenStorage.hasSeenOnboarding()) }
            var isPremiumScreenOpen by remember { mutableStateOf(false) }
            var gamificationSectionOpen by remember { mutableStateOf<GamificationSection?>(null) }
            var isProfileScreenOpen by remember(session.isLoggedIn) {
                mutableStateOf(session.isLoggedIn && !tokenStorage.isProfileConfigured() && database.getWeights(session.userId).isEmpty())
            }

            val pagerState = rememberPagerState(initialPage = initialTab.ordinal) { HealthTab.entries.size }
            val selectedTab = HealthTab.entries[pagerState.currentPage]
            val dailyReminderNotifier = rememberDailyReminderNotifier()
            val weeklyReportNotifier = rememberWeeklyReportNotifier()

            LaunchedEffect(session.isLoggedIn, session.userId) {
                if (session.isLoggedIn) {
                    if (tokenStorage.getWorkoutReminderFrequency() != WorkoutReminderFrequency.OFF) {
                        dailyReminderNotifier.scheduleEveningWorkoutReminder(
                            hour = tokenStorage.getWorkoutReminderHour(),
                            minute = tokenStorage.getWorkoutReminderMinute()
                        )
                    } else {
                        dailyReminderNotifier.cancelEveningWorkoutReminder()
                    }
                    if (tokenStorage.getWeightReminderFrequency() != WeightReminderFrequency.OFF) {
                        dailyReminderNotifier.scheduleMorningWeightReminder(hour = 9, minute = 0)
                    } else {
                        dailyReminderNotifier.cancelMorningWeightReminder()
                    }
                    if (tokenStorage.isAutoWeeklyWhatsAppEnabled()) {
                        weeklyReportNotifier.scheduleSundayEveningReminder(hour = 21, minute = 0)
                    } else {
                        weeklyReportNotifier.cancelSundayEveningReminder()
                    }
                } else {
                    dailyReminderNotifier.cancelEveningWorkoutReminder()
                    dailyReminderNotifier.cancelMorningWeightReminder()
                    weeklyReportNotifier.cancelSundayEveningReminder()
                }
            }

            val appLauncher = rememberExternalAppLauncher()
            var isEcoSwitcherOpen by remember { mutableStateOf(false) }
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val showToast: (String) -> Unit = { msg ->
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(msg)
                }
            }

            LaunchedEffect(Unit) {
                try {
                    Napier.base(DebugAntilog())
                } catch (_: Exception) {}
            }

            // Keep Home Screen Widget telemetry always in sync with local database & theme
            LaunchedEffect(widgetSnapshot, isDark, currentThemeColor, currentLanguage) {
                widgetBridge.updateSnapshot(
                    widgetSnapshot.copy(
                        isDarkTheme = isDark,
                        themeAccentId = currentThemeColor.id,
                        languageCode = currentLanguage.code
                    )
                )
            }

            // Handle deep links emitted from Home Screen Widgets (e.g. + Öğün, Spor, Tartım)
            val pendingDeepLink by WidgetDeepLinkBus.pendingTarget.collectAsState()
            LaunchedEffect(pendingDeepLink, session.isLoggedIn, isOnboardingVisible) {
                val target = pendingDeepLink ?: return@LaunchedEffect
                if (!session.isLoggedIn || isOnboardingVisible) return@LaunchedEffect
                isProfileScreenOpen = false
                isPremiumScreenOpen = false
                gamificationSectionOpen = null
                val targetTab = when (target.tabName) {
                    "WORKOUT" -> HealthTab.WORKOUT
                    "WEIGHT" -> HealthTab.WEIGHT
                    "ANALYTICS" -> HealthTab.ANALYTICS
                    else -> HealthTab.FOOD
                }
                pagerState.animateScrollToPage(targetTab.ordinal)
                if (target.action == null) {
                    WidgetDeepLinkBus.clearTarget()
                }
            }

            val evaluateWidgetReminderAndSync: () -> Unit = {
                if (session.isLoggedIn && session.userId > 0) {
                    val today = getTodayIso()
                    val pendingWaterMl = widgetBridge.consumePendingWaterDeltaMl(today)
                    if (pendingWaterMl > 0) {
                        coroutineScope.launch {
                            val currentTarget = widgetSnapshotBuilder.buildTodaySnapshot(session.userId, today).waterTargetMl
                            database.addWater(session.userId, today, pendingWaterMl, currentTarget)
                            showToast(
                                trEn(
                                    "Widget'tan +$pendingWaterMl ml su kaydedildi 💧",
                                    "+$pendingWaterMl ml water synced from widget 💧",
                                    currentLanguage
                                )
                            )
                        }
                    }
                    widgetBridge.checkWidgetInstalled { installed ->
                        isWidgetInstalled = installed
                        val canShowModal = !isOnboardingVisible &&
                            !isProfileScreenOpen &&
                            !isPremiumScreenOpen &&
                            gamificationSectionOpen == null &&
                            updateStatus == UpdateStatus.UpToDate &&
                            widgetBridge.isWidgetSupportedOnPlatform &&
                            tokenStorage.shouldShowDailyWidgetReminder(today, installed)
                        if (canShowModal) {
                            tokenStorage.setLastWidgetReminderDate(today)
                            isWidgetPromoDialogVisible = true
                        }
                    }
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, session.isLoggedIn, session.userId, isOnboardingVisible, isProfileScreenOpen) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        evaluateWidgetReminderAndSync()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(
                session.isLoggedIn,
                session.userId,
                isOnboardingVisible,
                isProfileScreenOpen,
                isPremiumScreenOpen,
                gamificationSectionOpen,
                updateStatus
            ) {
                evaluateWidgetReminderAndSync()
            }

            LaunchedEffect(session.isLoggedIn, session.userId) {
                if (session.isLoggedIn && session.userId > 0) {
                    Napier.d("SYNC: Oturum açık, uygulama açılışında otomatik sync tetikleniyor (userId=${session.userId})...")
                    apiClient.fetchSubscriptionStatus().onSuccess { subStatus ->
                        tokenStorage.setPremium(subStatus.isPremium, subStatus.premiumUntil)
                    }
                    syncEngine.sync()
                    deviceHealthBridge.syncSilentlyIfAuthorized(days = 30) { result ->
                        result.onSuccess { snapshot ->
                            coroutineScope.launch {
                                val summary = deviceHealthSyncManager.mergeSnapshot(snapshot)
                                if (summary.totalChanges > 0) {
                                    snackbarHostState.showSnackbar(summary.toUserMessage(currentLanguage))
                                }
                            }
                        }
                    }
                    if (database.getWeights(session.userId).isNotEmpty()) {
                        isProfileScreenOpen = false
                    }
                } else if (!session.isLoggedIn) {
                    Napier.d("SCREEN: AuthScreen açıldı")
                }
            }

            LaunchedEffect(selectedTab, session.isLoggedIn) {
                if (session.isLoggedIn) {
                    Napier.d("SCREEN: ${selectedTab.name} (${selectedTab.title}) açıldı")
                }
            }

            LaunchedEffect(isProfileScreenOpen) {
                if (isProfileScreenOpen) {
                    Napier.d("SCREEN: ProfileScreen açıldı")
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isOnboardingVisible) {
                    @Suppress("DEPRECATION")
                    @OptIn(ExperimentalComposeUiApi::class)
                    BackHandler(enabled = session.isLoggedIn) {
                        tokenStorage.setOnboardingSeen(true)
                        isOnboardingVisible = false
                    }
                    OnboardingScreenRoot(
                        onFinish = {
                            tokenStorage.setOnboardingSeen(true)
                            isOnboardingVisible = false
                        },
                        onLanguageSelected = { newLang ->
                            LocalizationManager.setLanguage(newLang)
                        }
                    )
                } else if (!session.isLoggedIn) {
                    AuthScreenRoot(
                        onLoginSuccess = {
                            // User logged in
                        },
                        onShowToast = showToast
                    )
                } else if (isPremiumScreenOpen) {
                    @Suppress("DEPRECATION")
                    @OptIn(ExperimentalComposeUiApi::class)
                    BackHandler(enabled = true) {
                        isPremiumScreenOpen = false
                    }
                    PremiumScreenRoot(
                        onNavigateBack = { isPremiumScreenOpen = false },
                        onShowToast = showToast
                    )
                } else if (gamificationSectionOpen != null) {
                    @Suppress("DEPRECATION")
                    @OptIn(ExperimentalComposeUiApi::class)
                    BackHandler(enabled = true) {
                        gamificationSectionOpen = null
                    }
                    GamificationScreenRoot(
                        initialSection = gamificationSectionOpen ?: GamificationSection.OVERVIEW,
                        onBack = { gamificationSectionOpen = null },
                        onShowToast = showToast
                    )
                } else if (isProfileScreenOpen) {
                    @Suppress("DEPRECATION")
                    @OptIn(ExperimentalComposeUiApi::class)
                    BackHandler(enabled = true) {
                        isProfileScreenOpen = false
                    }
                    ProfileScreenRoot(
                        onBack = { isProfileScreenOpen = false },
                        onThemeAccentChanged = { currentThemeColor = it },
                        onDarkModeChanged = { isDark = it },
                        onOpenPremium = { isPremiumScreenOpen = true },
                        onReplayOnboarding = {
                            isProfileScreenOpen = false
                            isOnboardingVisible = true
                        },
                        onOpenWidgetPromo = if (widgetBridge.isWidgetSupportedOnPlatform) {
                            {
                                tokenStorage.setWidgetReminderEnabled(true)
                                isWidgetPromoDialogVisible = true
                            }
                        } else null,
                        onLogout = {
                            isProfileScreenOpen = false
                            isPremiumScreenOpen = false
                            gamificationSectionOpen = null
                            gamificationManager.clearAllCelebrations()
                            coroutineScope.launch {
                                database.clearAll()
                                tokenStorage.clearSession()
                            }
                        },
                        onShowToast = showToast
                    )
                } else {
                @Suppress("DEPRECATION")
                @OptIn(ExperimentalComposeUiApi::class)
                BackHandler(enabled = isEcoSwitcherOpen) {
                    isEcoSwitcherOpen = false
                }
                val bottomBarVisibility = remember { mutableStateOf(true) }
                CompositionLocalProvider(
                    LocalBottomBarVisibility provides bottomBarVisibility
                ) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Brand Mark "H" (Tıklanınca Planora 'P' pod'unu açar/kapatır)
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            HealthColors.GradientPrimary.first().copy(alpha = 0.22f),
                                                            HealthColors.GradientPrimary.last().copy(alpha = 0.18f)
                                                        )
                                                    )
                                                )
                                                .bounceClick()
                                                .clickable {
                                                    isEcoSwitcherOpen = !isEcoSwitcherOpen
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            GradientText(
                                                text = "H",
                                                colors = HealthColors.GradientPrimary,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Black
                                                )
                                            )
                                        }

                                        // Ekosistem Geçişi: H'ye tıklanınca açılan Planora ("P") Pod'u
                                        AnimatedVisibility(
                                            visible = isEcoSwitcherOpen,
                                            enter = fadeIn() + expandHorizontally(),
                                            exit = fadeOut() + shrinkHorizontally()
                                        ) {
                                            val isPlanoraInstalled = remember(isEcoSwitcherOpen) {
                                                appLauncher.isAppInstalled(ExternalApp.PLANORA)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(
                                                                Color(0xFF6366F1).copy(alpha = 0.22f),
                                                                Color(0xFF8B5CF6).copy(alpha = 0.18f)
                                                            )
                                                        )
                                                    )
                                                    .bounceClick()
                                                    .clickable {
                                                        if (isPlanoraInstalled) {
                                                            appLauncher.openApp(ExternalApp.PLANORA)
                                                        } else {
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar(
                                                                    trEn(
                                                                        "Planora uygulaması cihazınızda yüklü değil.",
                                                                        "Planora app is not installed on your device.",
                                                                        currentLanguage
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                GradientText(
                                                    text = "P",
                                                    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = FontWeight.Black
                                                    )
                                                )
                                            }
                                        }

                                        Column {
                                            GradientText(
                                                text = "HealthTrack",
                                                colors = HealthColors.GradientPrimary,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = if (session.name.isNotBlank()) session.name else "@${session.username}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    // Parıldayan Seri & Seviye Hapı (Gamify Overview)
                                    ShimmeringStreakPill(
                                        streakDays = gamificationSummary.currentStreakDays,
                                        isActiveToday = gamificationSummary.isActiveToday,
                                        level = gamificationSummary.level,
                                        onClick = { gamificationSectionOpen = GamificationSection.OVERVIEW },
                                        modifier = Modifier.padding(end = 2.dp)
                                    )
                                    // Lider Tablosu (Genel & Arkadaşlar)
                                    IconButton(
                                        onClick = { gamificationSectionOpen = GamificationSection.LEADERBOARD },
                                        modifier = Modifier.bounceClick()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = str("Liderlik Tablosu", "Leaderboard"),
                                            tint = Color(0xFFFACC15)
                                        )
                                    }
                                    // Profil Butonu
                                    IconButton(
                                        onClick = { isProfileScreenOpen = true },
                                        modifier = Modifier.bounceClick()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = str("Profil & Ayarlar", "Profile & Settings"),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            val imeBottom = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
                            AnimatedVisibility(
                                visible = imeBottom == 0 && bottomBarVisibility.value,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 4.dp
                                ) {
                                    Column {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                            thickness = 0.5.dp
                                        )
                                        // Breathing room above indicator pill/icons without adding extra bottom space
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val bottomInsetPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                        NavigationBar(
                                            containerColor = Color.Transparent,
                                            tonalElevation = 0.dp,
                                            windowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                                            modifier = Modifier.height(58.dp + bottomInsetPadding)
                                        ) {
                                        NavigationBarItem(
                                            selected = selectedTab == HealthTab.FOOD,
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(HealthTab.FOOD.ordinal)
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Restaurant, contentDescription = HealthTab.FOOD.localizedTitle(currentLanguage)) },
                                            label = { Text(HealthTab.FOOD.localizedTitle(currentLanguage)) },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = selectedTab == HealthTab.WORKOUT,
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(HealthTab.WORKOUT.ordinal)
                                                }
                                            },
                                            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = HealthTab.WORKOUT.localizedTitle(currentLanguage)) },
                                            label = { Text(HealthTab.WORKOUT.localizedTitle(currentLanguage)) },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = selectedTab == HealthTab.WEIGHT,
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(HealthTab.WEIGHT.ordinal)
                                                }
                                            },
                                            icon = { Icon(Icons.Default.MonitorWeight, contentDescription = HealthTab.WEIGHT.localizedTitle(currentLanguage)) },
                                            label = { Text(HealthTab.WEIGHT.localizedTitle(currentLanguage)) },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = selectedTab == HealthTab.ANALYTICS,
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(HealthTab.ANALYTICS.ordinal)
                                                }
                                            },
                                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = str("Analiz & Rapor", "Analytics & Reports")) },
                                            label = { Text(HealthTab.ANALYTICS.localizedTitle(currentLanguage)) },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                                .imePadding()
                                .clearFocusOnTap()
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 1
                            ) { page ->
                                when (HealthTab.entries[page]) {
                                    HealthTab.FOOD -> FoodScreenRoot(
                                        onNavigateToWeight = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(HealthTab.WEIGHT.ordinal)
                                            }
                                        },
                                        onShowToast = showToast
                                    )
                                    HealthTab.WORKOUT -> WorkoutScreenRoot(onShowToast = showToast)
                                    HealthTab.WEIGHT -> WeightScreenRoot(onShowToast = showToast)
                                    HealthTab.ANALYTICS -> AnalyticsScreenRoot(onShowToast = showToast)
                                }
                            }
                        }
                    }
                }
                }

                if (!session.isLoggedIn || isPremiumScreenOpen || isProfileScreenOpen || gamificationSectionOpen != null) {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // Global Motivational Reward Toast (+XP & goal progress on every active entry)
                MotivationalToastHost(
                    event = activeMotivationalToast,
                    onDismiss = { gamificationManager.dismissMotivationalToast() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Global Confetti & Achievement Celebration Overlay
                ConfettiCelebrationOverlay(
                    event = activeCelebration,
                    onDismiss = { gamificationManager.dismissCurrentCelebration() }
                )

                // Daily Widget Reminder Popup (Max 1x/day if widget is not yet added)
                if (isWidgetPromoDialogVisible) {
                    WidgetPromoDialog(
                        snapshot = widgetSnapshot.copy(
                            isDarkTheme = isDark,
                            themeAccentId = currentThemeColor.id,
                            languageCode = currentLanguage.code
                        ),
                        supportsOneTapPinning = widgetBridge.supportsOneTapPinning,
                        isIosPlatform = isIosPlatform,
                        onAddWidgetClick = {
                            widgetBridge.requestAddWidget { success, msg ->
                                if (success) {
                                    isWidgetInstalled = true
                                    isWidgetPromoDialogVisible = false
                                    if (!msg.isNullOrBlank()) showToast(msg)
                                }
                            }
                        },
                        onDismissForToday = {
                            tokenStorage.setLastWidgetReminderDate(getTodayIso())
                            isWidgetPromoDialogVisible = false
                        },
                        onDisableDailyReminder = {
                            tokenStorage.setWidgetReminderEnabled(false)
                            tokenStorage.setLastWidgetReminderDate(getTodayIso())
                            isWidgetPromoDialogVisible = false
                            showToast(
                                trEn(
                                    "Günlük widget hatırlatması kapatıldı. Profil > Bildirim & Tema'dan tekrar açabilirsin.",
                                    "Daily widget reminder disabled. You can re-enable it in Profile > Alerts & Theme.",
                                    currentLanguage
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}
