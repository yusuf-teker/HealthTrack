package com.yusufteker.healthtrack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.yusufteker.healthtrack.core.model.DeviceHealthSnapshot
import com.yusufteker.healthtrack.core.model.DeviceWeightRecord
import com.yusufteker.healthtrack.core.model.GamificationSummary
import com.yusufteker.healthtrack.core.model.PushTokenBus
import com.yusufteker.healthtrack.core.model.UpdateStatus
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency
import com.yusufteker.healthtrack.core.model.toDeviceHealthSnapshot
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.DeviceHealthSyncManager
import com.yusufteker.healthtrack.core.sync.FriendActivityMonitor
import com.yusufteker.healthtrack.core.sync.GamificationManager
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.sync.WidgetSnapshotBuilder
import com.yusufteker.healthtrack.core.utils.BleScaleScanState
import com.yusufteker.healthtrack.core.utils.ExternalApp
import com.yusufteker.healthtrack.core.utils.HuaweiDeepLinkBus
import com.yusufteker.healthtrack.core.utils.WidgetDeepLinkBus
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.rememberAppVersion
import com.yusufteker.healthtrack.core.utils.rememberBleScaleBridge
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
import com.yusufteker.healthtrack.feature.profile.presentation.InitialProfileSetupScreenRoot
import com.yusufteker.healthtrack.feature.profile.presentation.ProfileScreenRoot
import com.yusufteker.healthtrack.feature.weight.presentation.WeightScreenRoot
import com.yusufteker.healthtrack.feature.weight.presentation.components.BleScaleMeasureDialogContent
import com.yusufteker.healthtrack.feature.weight.presentation.components.BleScaleSheetCoordinator
import com.yusufteker.healthtrack.feature.workout.presentation.WorkoutScreenRoot
import com.yusufteker.healthtrack.theme.AppThemeColor
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.HealthTrackTheme
import com.yusufteker.healthtrack.theme.LocalBottomBarVisibility
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.ConfettiCelebrationOverlay
import com.yusufteker.healthtrack.ui.components.ForceUpdateDialog
import com.yusufteker.healthtrack.ui.components.FriendActivityBannerHost
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.HealthBrandLogo
import com.yusufteker.healthtrack.ui.components.HealthSnackbarHost
import com.yusufteker.healthtrack.ui.components.MotivationalToastHost
import com.yusufteker.healthtrack.ui.components.NotificationPermissionDialog
import com.yusufteker.healthtrack.ui.components.ShimmeringStreakPill
import com.yusufteker.healthtrack.ui.components.WidgetPromoDialog
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatformTools
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

@Composable
fun App(initialTab: HealthTab = HealthTab.FOOD) {
    val isKoinStarted = remember { KoinPlatformTools.defaultContext().getOrNull() != null }
    if (isKoinStarted) {
        AppContent(initialTab = initialTab)
    } else {
        KoinApplication(application = {
            modules(appModules)
        }) {
            AppContent(initialTab = initialTab)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalUuidApi::class)
@Composable
private fun AppContent(initialTab: HealthTab) {
    run {
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
            val friendActivityMonitor: FriendActivityMonitor = koinInject()
            val apiClient: HealthApiClient = koinInject()
            val checkAppVersionUseCase: CheckAppVersionUseCase = koinInject()
            val widgetSnapshotBuilder: WidgetSnapshotBuilder = koinInject()
            val deviceHealthBridge = rememberDeviceHealthBridge()
            val passiveBleScaleBridge = rememberBleScaleBridge()
            val passiveBleState by passiveBleScaleBridge.state.collectAsState()
            val widgetBridge = rememberWidgetBridge()
            var isAutoBlePopupOpen by remember { mutableStateOf(false) }
            var lastAutoBleDismissOrSaveMs by remember { mutableStateOf(0L) }
            var autoScaleSuccessMessage by remember { mutableStateOf<String?>(null) }
            val currentAppVersion = rememberAppVersion()
            val session by tokenStorage.sessionFlow.collectAsState()
            val widgetSnapshot by remember(session.hasActiveSession, session.userId) {
                widgetSnapshotBuilder.observeTodaySnapshot(session.userId)
            }.collectAsState(initial = null)
            var isWidgetPromoDialogVisible by remember { mutableStateOf(false) }
            var isWidgetInstalled by remember { mutableStateOf(false) }
            val isIosPlatform = remember {
                val pName = getPlatform().name
                pName.contains("iOS", ignoreCase = true) || pName.contains("iPad", ignoreCase = true)
            }
            val gamificationSummary by remember(session.hasActiveSession, session.userId) {
                if (session.hasActiveSession && session.userId != 0) {
                    gamificationManager.observeSummary(session.userId).flowOn(Dispatchers.Default)
                } else {
                    kotlinx.coroutines.flow.flowOf(GamificationSummary())
                }
            }.collectAsState(initial = GamificationSummary())
            val activeCelebration by gamificationManager.currentCelebration.collectAsState()
            val activeMotivationalToast by gamificationManager.currentMotivationalToast.collectAsState()
            val activeFriendBanner by friendActivityMonitor.currentBanner.collectAsState()
            val observedPushToken by PushTokenBus.tokenFlow.collectAsState()

            var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.UpToDate) }
            var isOptionalUpdateDismissed by remember { mutableStateOf(false) }

            LaunchedEffect(currentAppVersion) {
                delay(1500L)
                val evaluated = withContext(Dispatchers.Default) {
                    apiClient.fetchAppVersionConfig().getOrNull()?.let { config ->
                        checkAppVersionUseCase.evaluate(currentAppVersion, config)
                    }
                }
                if (evaluated != null) {
                    updateStatus = evaluated
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
            var premiumOfferType by remember { mutableStateOf<String?>(null) }
            var gamificationSectionOpen by remember { mutableStateOf<GamificationSection?>(null) }
            var isGuestAuthOpen by remember(session.isAuthenticated) { mutableStateOf(false) }
            var isInitialSetupVisible by remember(session.hasActiveSession, session.userId) {
                mutableStateOf(
                    session.hasActiveSession &&
                        !tokenStorage.isProfileConfigured() &&
                        database.getWeights(session.userId).isEmpty()
                )
            }
            var isProfileScreenOpen by remember(session.hasActiveSession) {
                mutableStateOf(false)
            }
            var isNotifPermDialogVisible by remember { mutableStateOf(false) }

            val pagerState = rememberPagerState(initialPage = initialTab.ordinal) { HealthTab.entries.size }
            val selectedTab = HealthTab.entries[pagerState.currentPage]
            val dailyReminderNotifier = rememberDailyReminderNotifier()
            val weeklyReportNotifier = rememberWeeklyReportNotifier()

            LaunchedEffect(session.hasActiveSession, session.userId) {
                withContext(Dispatchers.Default) {
                    if (session.hasActiveSession) {
                        if (tokenStorage.getWorkoutReminderFrequency() != WorkoutReminderFrequency.OFF) {
                            dailyReminderNotifier.scheduleEveningWorkoutReminder(
                                hour = tokenStorage.getWorkoutReminderHour(),
                                minute = tokenStorage.getWorkoutReminderMinute()
                            )
                        } else {
                            dailyReminderNotifier.cancelEveningWorkoutReminder()
                        }
                        val weightFreq = tokenStorage.getWeightReminderFrequency()
                        if (weightFreq != WeightReminderFrequency.OFF) {
                            dailyReminderNotifier.scheduleMorningWeightReminder(
                                hour = tokenStorage.getWeightReminderHour(),
                                minute = tokenStorage.getWeightReminderMinute(),
                                frequency = weightFreq
                            )
                        } else {
                            dailyReminderNotifier.cancelMorningWeightReminder()
                        }
                        if (tokenStorage.isFoodReminderEnabled()) {
                            dailyReminderNotifier.scheduleMiddayFoodReminder(hour = 13, minute = 30)
                        } else {
                            dailyReminderNotifier.cancelMiddayFoodReminder()
                        }
                        if (tokenStorage.isAutoWeeklyWhatsAppEnabled()) {
                            weeklyReportNotifier.scheduleSundayEveningReminder(hour = 21, minute = 0)
                        } else {
                            weeklyReportNotifier.cancelSundayEveningReminder()
                        }
                    } else {
                        dailyReminderNotifier.cancelEveningWorkoutReminder()
                        dailyReminderNotifier.cancelMorningWeightReminder()
                        dailyReminderNotifier.cancelMiddayFoodReminder()
                        weeklyReportNotifier.cancelSundayEveningReminder()
                    }
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

            // Collect incoming live pushes (FCM / APNs) immediately while app is open
            LaunchedEffect(Unit) {
                friendActivityMonitor.observeLivePushBus()
            }

            var lastRegisteredFcmKey by remember { mutableStateOf<String?>(null) }
            var inFlightFcmKey by remember { mutableStateOf<String?>(null) }
            val rawApnsHexRegex = remember { Regex("^[0-9a-fA-F]{64}$") }

            suspend fun syncFcmTokenIfNeeded(force: Boolean = false) {
                val currentSession = tokenStorage.sessionFlow.value
                if (!currentSession.isAuthenticated || currentSession.userId <= 0) {
                    lastRegisteredFcmKey = null
                    inFlightFcmKey = null
                    return
                }
                val candidate = (PushTokenBus.tokenFlow.value ?: tokenStorage.getFcmToken())?.trim()
                if (candidate.isNullOrBlank() || (isIosPlatform && rawApnsHexRegex.matches(candidate))) {
                    if (isIosPlatform && tokenStorage.getFcmToken()?.let { rawApnsHexRegex.matches(it) } == true) {
                        tokenStorage.setFcmToken(null)
                    }
                    return
                }
                val key = "${currentSession.userId}:$candidate"
                if (!force && (key == lastRegisteredFcmKey || key == inFlightFcmKey)) return
                inFlightFcmKey = key

                val platformLabel = if (isIosPlatform) "HEALTHTRACK_IOS" else "HEALTHTRACK_ANDROID"
                val result = withContext(Dispatchers.Default) {
                    apiClient.registerFcmToken(candidate, platformLabel)
                }
                result
                    .onSuccess {
                        lastRegisteredFcmKey = key
                        if (inFlightFcmKey == key) inFlightFcmKey = null
                        Napier.d(
                            "FCM token registered for userId=${currentSession.userId} ($platformLabel)",
                            tag = "FCM"
                        )
                    }
                    .onFailure { err ->
                        if (inFlightFcmKey == key) inFlightFcmKey = null
                        Napier.w(
                            "FCM token register failed, will retry on resume: ${err.message}",
                            tag = "FCM"
                        )
                    }
            }

            // Register latest push token whenever user signs in or token refreshes
            LaunchedEffect(session.isAuthenticated, session.userId, observedPushToken) {
                if (session.isAuthenticated && session.userId > 0) {
                    syncFcmTokenIfNeeded()
                } else {
                    lastRegisteredFcmKey = null
                }
            }

            suspend fun checkFriendUpdatesAndNotify(force: Boolean = false) {
                val currentSession = tokenStorage.sessionFlow.value
                if (!currentSession.isAuthenticated || currentSession.userId <= 0) return
                val newlyDetected = withContext(Dispatchers.Default) {
                    friendActivityMonitor.checkForUpdates(
                        userId = currentSession.userId,
                        force = force
                    )
                }
                newlyDetected.forEach { event ->
                    dailyReminderNotifier.sendFriendActivityNotification(
                        id = event.id,
                        title = event.localizedTitle(currentLanguage),
                        body = event.localizedMessage(currentLanguage),
                        targetTab = "LEADERBOARD"
                    )
                }
            }

            // On app launch / active session, immediately check friend updates & pending pushes
            LaunchedEffect(session.isAuthenticated, session.userId) {
                if (session.isAuthenticated && session.userId > 0) {
                    checkFriendUpdatesAndNotify(force = true)
                } else {
                    friendActivityMonitor.clearAll()
                }
            }

            // Every time the main dashboard becomes visible (after closing Profile/Leaderboard/etc.), check friend activity
            val isDashboardVisible = session.isAuthenticated &&
                session.userId > 0 &&
                !isOnboardingVisible &&
                !isInitialSetupVisible &&
                !isProfileScreenOpen &&
                !isPremiumScreenOpen &&
                !isGuestAuthOpen &&
                gamificationSectionOpen == null
            LaunchedEffect(isDashboardVisible, session.userId) {
                if (isDashboardVisible) {
                    checkFriendUpdatesAndNotify(force = false)
                }
            }

            // Keep Home Screen Widget telemetry always in sync with local database & theme
            LaunchedEffect(widgetSnapshot, isDark, currentThemeColor, currentLanguage) {
                val snapshot = widgetSnapshot ?: return@LaunchedEffect
                widgetBridge.updateSnapshot(
                    snapshot.copy(
                        isDarkTheme = isDark,
                        themeAccentId = currentThemeColor.id,
                        languageCode = currentLanguage.code
                    )
                )
            }

            // Handle deep links emitted from Home Screen Widgets (e.g. + Öğün, Spor, Tartım)
            val pendingDeepLink by WidgetDeepLinkBus.pendingTarget.collectAsState()
            LaunchedEffect(pendingDeepLink, session.hasActiveSession, isOnboardingVisible) {
                val target = pendingDeepLink ?: return@LaunchedEffect
                if (!session.hasActiveSession || isOnboardingVisible) return@LaunchedEffect
                isProfileScreenOpen = false
                isPremiumScreenOpen = false
                isGuestAuthOpen = false
                if (target.tabName == "LEADERBOARD") {
                    gamificationSectionOpen = GamificationSection.LEADERBOARD
                    WidgetDeepLinkBus.clearTarget()
                    return@LaunchedEffect
                }
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

            suspend fun syncHuaweiCloudIfReady(showFeedbackAlways: Boolean = false) {
                if (!tokenStorage.isAuthenticated()) return
                apiClient.syncHuaweiHealth(days = 7).onSuccess { dto ->
                    tokenStorage.setHuaweiConnected(dto.connected)
                    if (dto.connected && !dto.needsReauth) {
                        deviceHealthSyncManager.mergeSnapshot(
                            dto.toDeviceHealthSnapshot(),
                            syncToCloud = true
                        )
                        tokenStorage.setLastHuaweiSyncMs(currentTimeMillis())
                        if (showFeedbackAlways) {
                            showToast(
                                trEn(
                                    "Huawei Sağlık bağlandı ✓",
                                    "Huawei Health connected ✓",
                                    currentLanguage
                                )
                            )
                        }
                    }
                }
            }

            val pendingHuaweiCallback by HuaweiDeepLinkBus.pendingEvent.collectAsState()
            LaunchedEffect(pendingHuaweiCallback, isProfileScreenOpen, session.isAuthenticated) {
                val event = pendingHuaweiCallback ?: return@LaunchedEffect
                if (isProfileScreenOpen || !session.isAuthenticated) return@LaunchedEffect
                HuaweiDeepLinkBus.consume()
                if (event.status.equals("connected", ignoreCase = true)) {
                    tokenStorage.setHuaweiConnected(true)
                    syncHuaweiCloudIfReady(showFeedbackAlways = true)
                } else {
                    val detail = event.reason?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                    showToast(
                        trEn(
                            "Huawei Sağlık bağlantısı tamamlanamadı$detail.",
                            "Could not connect Huawei Health$detail.",
                            currentLanguage
                        )
                    )
                }
            }

            val evaluateWidgetReminderAndSync: () -> Unit = {
                if (session.hasActiveSession && session.userId != 0) {
                    val today = getTodayIso()
                    val pendingWaterMl = widgetBridge.consumePendingWaterDeltaMl(today)
                    if (pendingWaterMl > 0) {
                        coroutineScope.launch {
                            val currentTarget = widgetSnapshotBuilder.buildTodaySnapshot(session.userId, today).waterTargetMl
                            database.addWater(session.userId, today, pendingWaterMl, currentTarget)
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
            // Automatic passive foreground BLE smart scale listener (zero prompt, stops on ON_PAUSE)
            val startPassiveScaleIfReady: () -> Unit = {
                if (session.hasActiveSession && session.userId != 0 && passiveBleScaleBridge.isSupported) {
                    val h = tokenStorage.getUserHeight().takeIf { it > 50.0 } ?: 175.0
                    val a = tokenStorage.getUserAge().takeIf { it > 0 } ?: 26
                    val isMale = !tokenStorage.getUserGender().equals("FEMALE", ignoreCase = true)
                    passiveBleScaleBridge.startPassiveScanIfPermitted(h, a, isMale)
                }
            }

            LaunchedEffect(session.hasActiveSession, session.userId) {
                if (session.hasActiveSession && session.userId != 0) {
                    startPassiveScaleIfReady()
                } else {
                    passiveBleScaleBridge.stopScan()
                }
            }

            // Open the full interactive BLE Scale popup when the user steps on the scale on any screen
            val passiveReadingDetected = passiveBleState.liveReading?.takeIf {
                session.hasActiveSession &&
                    !isOnboardingVisible &&
                    !isInitialSetupVisible &&
                    !BleScaleSheetCoordinator.isManualSheetOpen &&
                    !it.isWeightRemoved &&
                    (passiveBleState.scanState == BleScaleScanState.MEASURING ||
                        passiveBleState.scanState == BleScaleScanState.STABILIZED)
            }
            LaunchedEffect(
                passiveReadingDetected?.weightKg,
                passiveBleState.scanState,
                isOnboardingVisible,
                isInitialSetupVisible
            ) {
                if (passiveReadingDetected != null && !isAutoBlePopupOpen) {
                    val nowMs = currentTimeMillis()
                    if (nowMs - lastAutoBleDismissOrSaveMs > 12_000L) {
                        isAutoBlePopupOpen = true
                    }
                }
            }

            DisposableEffect(lifecycleOwner, session.hasActiveSession, session.isAuthenticated, session.userId, isOnboardingVisible, isProfileScreenOpen, isInitialSetupVisible) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE) {
                        passiveBleScaleBridge.stopScan()
                    } else if (event == Lifecycle.Event.ON_RESUME) {
                        startPassiveScaleIfReady()
                        evaluateWidgetReminderAndSync()
                        if (session.hasActiveSession && session.userId != 0) {
                            if (session.isAuthenticated) {
                                coroutineScope.launch {
                                    syncFcmTokenIfNeeded()
                                    checkFriendUpdatesAndNotify(force = true)
                                }
                            }
                            if (tokenStorage.isAutoDeviceSyncEnabled()) {
                                val nowMs = currentTimeMillis()
                                val minResumeIntervalMs = 5 * 60 * 1000L
                                if (nowMs - tokenStorage.getLastDeviceSyncMs() >= minResumeIntervalMs) {
                                    deviceHealthBridge.syncSilentlyIfAuthorized(days = 7) { result ->
                                        result.onSuccess { snapshot ->
                                            coroutineScope.launch {
                                                deviceHealthSyncManager.mergeSnapshot(
                                                    snapshot,
                                                    syncToCloud = session.isAuthenticated
                                                )
                                            }
                                        }
                                    }
                                }
                                if (session.isAuthenticated &&
                                    tokenStorage.isHuaweiConnected() &&
                                    nowMs - tokenStorage.getLastHuaweiSyncMs() >= minResumeIntervalMs
                                ) {
                                    coroutineScope.launch { syncHuaweiCloudIfReady() }
                                }
                            }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val hasNotificationPermission = dailyReminderNotifier.hasPermission
            LaunchedEffect(
                session.hasActiveSession,
                session.userId,
                isOnboardingVisible,
                isInitialSetupVisible,
                isProfileScreenOpen,
                isPremiumScreenOpen,
                isGuestAuthOpen,
                gamificationSectionOpen,
                updateStatus,
                hasNotificationPermission
            ) {
                evaluateWidgetReminderAndSync()
                val canShowNotifPrompt = session.hasActiveSession &&
                    session.userId != 0 &&
                    !isOnboardingVisible &&
                    !isInitialSetupVisible &&
                    !isProfileScreenOpen &&
                    !isPremiumScreenOpen &&
                    !isGuestAuthOpen &&
                    gamificationSectionOpen == null &&
                    updateStatus == UpdateStatus.UpToDate &&
                    !isWidgetPromoDialogVisible &&
                    !hasNotificationPermission
                if (canShowNotifPrompt) {
                    val nowMs = currentTimeMillis()
                    val cooldownMs = 3L * 24 * 60 * 60 * 1000L
                    if (nowMs - tokenStorage.getLastNotifPermPromptMs() >= cooldownMs) {
                        tokenStorage.setLastNotifPermPromptMs(nowMs)
                        isNotifPermDialogVisible = true
                    }
                }
            }

            LaunchedEffect(session.hasActiveSession, session.isAuthenticated, session.userId) {
                if (session.hasActiveSession && session.userId != 0) {
                    if (session.isAuthenticated) {
                        Napier.d("SYNC: Oturum açık, uygulama açılışında otomatik sync tetikleniyor (userId=${session.userId})...")
                        withContext(Dispatchers.Default) {
                            launch {
                                apiClient.fetchSubscriptionStatus().onSuccess { subStatus ->
                                    tokenStorage.setPremium(subStatus.isPremium, subStatus.premiumUntil)
                                }
                            }
                            launch {
                                apiClient.getHuaweiStatus().onSuccess { status ->
                                    tokenStorage.setHuaweiConnected(status.connected)
                                    if (tokenStorage.isAutoDeviceSyncEnabled() && status.connected && !status.needsReauth) {
                                        syncHuaweiCloudIfReady()
                                    }
                                }
                            }
                            syncEngine.sync()
                        }
                    } else {
                        Napier.d("GUEST: Misafir modu aktif (userId=${session.userId}), bulut sync atlanıyor.")
                    }
                    if (tokenStorage.isAutoDeviceSyncEnabled()) {
                        deviceHealthBridge.syncSilentlyIfAuthorized(days = 30) { result ->
                            result.onSuccess { snapshot ->
                                coroutineScope.launch(Dispatchers.Default) {
                                    deviceHealthSyncManager.mergeSnapshot(
                                        snapshot,
                                        syncToCloud = session.isAuthenticated
                                    )
                                }
                            }
                        }
                    }
                    if (database.getWeights(session.userId).isNotEmpty()) {
                        isInitialSetupVisible = false
                        isProfileScreenOpen = false
                    }
                } else if (!session.hasActiveSession) {
                    Napier.d("SCREEN: AuthScreen açıldı")
                }
            }

            LaunchedEffect(selectedTab, session.hasActiveSession) {
                if (session.hasActiveSession) {
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
                    BackHandler(enabled = session.hasActiveSession) {
                        tokenStorage.setOnboardingSeen(true)
                        isOnboardingVisible = false
                    }
                    OnboardingScreenRoot(
                        onFinish = {
                            tokenStorage.setOnboardingSeen(true)
                            isOnboardingVisible = false
                            if (session.isAuthenticated && !tokenStorage.isPremium()) {
                                premiumOfferType = "welcome"
                                isPremiumScreenOpen = true
                            }
                        },
                        onLanguageSelected = { newLang ->
                            LocalizationManager.setLanguage(newLang)
                        }
                    )
                } else if (!session.hasActiveSession) {
                    AuthScreenRoot(
                        onLoginSuccess = {
                            // User logged in or started guest session
                        },
                        onShowToast = showToast
                    )
                } else if (isGuestAuthOpen && session.isGuest) {
                    @Suppress("DEPRECATION")
                    @OptIn(ExperimentalComposeUiApi::class)
                    BackHandler(enabled = true) {
                        isGuestAuthOpen = false
                    }
                    AuthScreenRoot(
                        isGuestUpgrade = true,
                        onBackFromGuestUpgrade = { isGuestAuthOpen = false },
                        onLoginSuccess = {
                            isGuestAuthOpen = false
                            isProfileScreenOpen = false
                        },
                        onShowToast = showToast
                    )
                } else if (isInitialSetupVisible) {
                    InitialProfileSetupScreenRoot(
                        onComplete = { isInitialSetupVisible = false },
                        onShowToast = showToast
                    )
                } else if (isPremiumScreenOpen) {
                    @Suppress("DEPRECATION")
                    @OptIn(ExperimentalComposeUiApi::class)
                    BackHandler(enabled = true) {
                        isPremiumScreenOpen = false
                        premiumOfferType = null
                    }
                    PremiumScreenRoot(
                        offerType = premiumOfferType,
                        onNavigateBack = {
                            isPremiumScreenOpen = false
                            premiumOfferType = null
                        },
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
                        onOpenPremium = {
                            premiumOfferType = null
                            isPremiumScreenOpen = true
                        },
                        onOpenGuestAuth = {
                            isProfileScreenOpen = false
                            isGuestAuthOpen = true
                        },
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
                            isGuestAuthOpen = false
                            gamificationSectionOpen = null
                            gamificationManager.clearAllCelebrations()
                            friendActivityMonitor.clearAll()
                            coroutineScope.launch {
                                lastRegisteredFcmKey = null
                                if (session.isAuthenticated) {
                                    tokenStorage.getFcmToken()?.let { token ->
                                        runCatching { apiClient.unregisterFcmToken(token) }
                                    }
                                }
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
                        snackbarHost = { HealthSnackbarHost(snackbarHostState) },
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
                                                            showToast(
                                                                trEn(
                                                                    "Planora uygulaması cihazınızda yüklü değil.",
                                                                    "Planora app is not installed on your device.",
                                                                    currentLanguage
                                                                )
                                                            )
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

                                        Text(
                                            text = if (session.isGuest) {
                                                str("Misafir", "Guest")
                                            } else if (session.name.isNotBlank()) {
                                                session.name
                                            } else {
                                                "@${session.username}"
                                            },
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                                    // Lider Tablosu (Genel & Arkadaşlar) — Misafir modunda gizli
                                    if (!session.isGuest) {
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
                                beyondViewportPageCount = 0
                            ) { page ->
                                when (HealthTab.entries[page]) {
                                    HealthTab.FOOD -> FoodScreenRoot(
                                        onNavigateToWeight = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(HealthTab.WEIGHT.ordinal)
                                            }
                                        },
                                        onOpenPremiumOffer = {
                                            premiumOfferType = "quota_exceeded"
                                            isPremiumScreenOpen = true
                                        },
                                        onShowToast = showToast
                                    )
                                    HealthTab.WORKOUT -> WorkoutScreenRoot(
                                        onOpenPremiumOffer = {
                                            premiumOfferType = "quota_exceeded"
                                            isPremiumScreenOpen = true
                                        },
                                        onShowToast = showToast
                                    )
                                    HealthTab.WEIGHT -> WeightScreenRoot(onShowToast = showToast)
                                    HealthTab.ANALYTICS -> AnalyticsScreenRoot(onShowToast = showToast)
                                }
                            }
                        }
                    }
                }
                }

                if (!session.hasActiveSession || isGuestAuthOpen || isInitialSetupVisible || isPremiumScreenOpen || isProfileScreenOpen || gamificationSectionOpen != null) {
                    HealthSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(bottom = 8.dp)
                    )
                }

                // Top In-App Friend Activity Push Popup (Launch-restored & live) — hidden in Guest Mode
                if (!session.isGuest) {
                    FriendActivityBannerHost(
                        event = activeFriendBanner,
                        onOpenLeaderboard = {
                            isProfileScreenOpen = false
                            isPremiumScreenOpen = false
                            gamificationSectionOpen = GamificationSection.LEADERBOARD
                        },
                        onDismiss = { friendActivityMonitor.dismissCurrentBanner() },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // Global Auto-Detected BLE Scale Popup (same Vazgeç / Ölçümü Kaydet dialog across all screens)
                if (isAutoBlePopupOpen && !BleScaleSheetCoordinator.isManualSheetOpen) {
                    val userHeight = tokenStorage.getUserHeight().takeIf { it > 50.0 } ?: 175.0
                    val userAgeVal = tokenStorage.getUserAge().takeIf { it > 0 } ?: 26
                    val userIsMaleVal = !tokenStorage.getUserGender().equals("FEMALE", ignoreCase = true)

                    BleScaleMeasureDialogContent(
                        bleState = passiveBleState,
                        userHeightCm = userHeight,
                        userAge = userAgeVal,
                        userIsMale = userIsMaleVal,
                        onRetryScan = { startPassiveScaleIfReady() },
                        onSaveReading = { reading ->
                            isAutoBlePopupOpen = false
                            lastAutoBleDismissOrSaveMs = currentTimeMillis()
                            passiveBleScaleBridge.stopScan()

                            coroutineScope.launch {
                                val nowMs = currentTimeMillis()
                                val todayIso = getTodayIso()
                                val snapshot = DeviceHealthSnapshot(
                                    weights = listOf(
                                        DeviceWeightRecord(
                                            externalId = "ble_auto_$nowMs",
                                            date = todayIso,
                                            weightKg = reading.weightKg,
                                            bodyFatPercent = reading.bodyFatPercent,
                                            leanBodyMassKg = reading.leanBodyMassKg,
                                            boneMassKg = reading.boneMassKg,
                                            muscleMassKg = reading.muscleMassKg,
                                            musclePercent = reading.musclePercent,
                                            impedanceOhm = reading.impedanceOhm,
                                            measuredAtMillis = nowMs,
                                            sourcePackage = "ble.scale.auto",
                                            sourceName = reading.scaleModel
                                        )
                                    ),
                                    platformName = "Bluetooth (${reading.scaleModel})"
                                )
                                deviceHealthSyncManager.mergeSnapshot(
                                    snapshot,
                                    syncToCloud = session.isAuthenticated
                                )
                                tokenStorage.setUserWeight(reading.weightKg)

                                val fatPart = reading.bodyFatPercent?.let {
                                    " • %$it ${trEn("Yağ", "Fat", currentLanguage)}"
                                }.orEmpty()
                                val musclePart = reading.muscleMassKg?.let {
                                    " • $it kg ${trEn("Kas", "Muscle", currentLanguage)}"
                                }.orEmpty()
                                val msg = trEn(
                                    "Tartı ölçümü başarıyla kaydedildi: ${reading.weightKg} kg$fatPart$musclePart",
                                    "Scale reading saved: ${reading.weightKg} kg$fatPart$musclePart",
                                    currentLanguage
                                )
                                autoScaleSuccessMessage = msg
                                kotlinx.coroutines.delay(4500L)
                                if (autoScaleSuccessMessage == msg) {
                                    autoScaleSuccessMessage = null
                                }
                                kotlinx.coroutines.delay(5500L)
                                startPassiveScaleIfReady()
                            }
                        },
                        onDismiss = {
                            isAutoBlePopupOpen = false
                            lastAutoBleDismissOrSaveMs = currentTimeMillis()
                            passiveBleScaleBridge.stopScan()
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(12_000L)
                                startPassiveScaleIfReady()
                            }
                        }
                    )
                }

                // Global BLE Scale Saved Confirmation Banner
                val showLiveScaleBanner = session.hasActiveSession && autoScaleSuccessMessage != null
                AnimatedVisibility(
                    visible = showLiveScaleBanner,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    val savedMsg = autoScaleSuccessMessage
                    val accent = Color(0xFF10B981)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) Color(0xF0121A2A) else Color(0xF5FFFFFF),
                        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.55f)),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoScaleSuccessMessage = null }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = trEn("Ölçüm Başarıyla Kaydedildi ✓", "Measurement Saved Successfully ✓", currentLanguage),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (savedMsg != null) {
                                    Text(
                                        text = savedMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
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

                if (isNotifPermDialogVisible) {
                    NotificationPermissionDialog(
                        onAllowClick = {
                            isNotifPermDialogVisible = false
                            dailyReminderNotifier.requestPermission { granted ->
                                if (granted) {
                                    coroutineScope.launch { syncFcmTokenIfNeeded(force = true) }
                                }
                                showToast(
                                    if (granted) {
                                        trEn("Bildirimler açıldı!", "Notifications enabled!", currentLanguage)
                                    } else {
                                        trEn(
                                            "Bildirim izni verilmedi. Profil > Bildirim & Tema'dan açabilirsiniz.",
                                            "Notifications declined. You can enable them in Profile > Alerts & Theme.",
                                            currentLanguage
                                        )
                                    }
                                )
                            }
                        },
                        onLaterClick = { isNotifPermDialogVisible = false }
                    )
                }

                // Daily Widget Reminder Popup (Max 1x/day if widget is not yet added)
                if (isWidgetPromoDialogVisible) {
                    val promoSnapshot = widgetSnapshot ?: remember(session.userId) {
                        widgetSnapshotBuilder.buildTodaySnapshot(session.userId)
                    }
                    WidgetPromoDialog(
                        snapshot = promoSnapshot.copy(
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
