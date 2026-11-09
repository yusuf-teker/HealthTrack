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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.utils.ExternalApp
import com.yusufteker.healthtrack.core.utils.rememberExternalAppLauncher
import com.yusufteker.healthtrack.di.appModules
import com.yusufteker.healthtrack.feature.analytics.presentation.AnalyticsScreenRoot
import com.yusufteker.healthtrack.feature.auth.presentation.AuthScreenRoot
import com.yusufteker.healthtrack.feature.food.presentation.FoodScreenRoot
import com.yusufteker.healthtrack.feature.profile.presentation.ProfileScreenRoot
import com.yusufteker.healthtrack.feature.weight.presentation.WeightScreenRoot
import com.yusufteker.healthtrack.feature.workout.presentation.WorkoutScreenRoot
import com.yusufteker.healthtrack.theme.AppThemeColor
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.HealthTrackTheme
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.HealthBrandLogo
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class HealthTab(val title: String) {
    FOOD("Yemek"),
    WORKOUT("Spor"),
    WEIGHT("Kilo"),
    ANALYTICS("Analiz")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalUuidApi::class)
@Composable
fun App() {
    KoinApplication(application = {
        modules(appModules)
    }) {
        val settings: Settings = koinInject()
        val systemDark = isSystemInDarkTheme()
        var isDark by remember {
            mutableStateOf(settings.getBoolean("dark_mode", systemDark))
        }
        var currentThemeColor by remember {
            mutableStateOf(AppThemeColor.fromId(settings.getString("theme_accent", AppThemeColor.TEAL.id)))
        }

        HealthTrackTheme(darkTheme = isDark, themeAccent = currentThemeColor) {
            val tokenStorage: TokenStorage = koinInject()
            val database: HealthDatabase = koinInject()
            val syncEngine: SyncEngine = koinInject()
            val session by tokenStorage.sessionFlow.collectAsState()

            var isProfileScreenOpen by remember(session.isLoggedIn) {
                mutableStateOf(session.isLoggedIn && !tokenStorage.isProfileConfigured() && database.getWeights(session.userId).isEmpty())
            }

            var selectedTab by remember { mutableStateOf(HealthTab.FOOD) }

            val appLauncher = rememberExternalAppLauncher()
            var isEcoSwitcherOpen by remember { mutableStateOf(false) }
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                try {
                    Napier.base(DebugAntilog())
                } catch (_: Exception) {}
            }

            LaunchedEffect(session.isLoggedIn, session.userId) {
                if (session.isLoggedIn && session.userId > 0) {
                    Napier.d("SYNC: Oturum açık, uygulama açılışında otomatik sync tetikleniyor (userId=${session.userId})...")
                    syncEngine.sync()
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

            if (!session.isLoggedIn) {
                AuthScreenRoot(
                    onLoginSuccess = {
                        // User logged in
                    }
                )
            } else if (isProfileScreenOpen) {
                ProfileScreenRoot(
                    onBack = { isProfileScreenOpen = false },
                    onThemeAccentChanged = { currentThemeColor = it },
                    onDarkModeChanged = { isDark = it },
                    onLogout = {
                        isProfileScreenOpen = false
                        coroutineScope.launch {
                            database.clearAll()
                            tokenStorage.clearSession()
                        }
                    }
                )
            } else {
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
                                                            snackbarHostState.showSnackbar("Planora uygulaması cihazınızda yüklü değil.")
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
                                // Sadece Profil Butonu
                                IconButton(
                                    onClick = { isProfileScreenOpen = true },
                                    modifier = Modifier.bounceClick()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profil & Ayarlar",
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
                            visible = imeBottom == 0,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    thickness = 0.5.dp
                                )
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 4.dp
                                ) {
                                    NavigationBarItem(
                                        selected = selectedTab == HealthTab.FOOD,
                                        onClick = { selectedTab = HealthTab.FOOD },
                                        icon = { Icon(Icons.Default.Restaurant, contentDescription = "Yemek") },
                                        label = { Text(HealthTab.FOOD.title) },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == HealthTab.WORKOUT,
                                        onClick = { selectedTab = HealthTab.WORKOUT },
                                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Spor") },
                                        label = { Text(HealthTab.WORKOUT.title) },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == HealthTab.WEIGHT,
                                        onClick = { selectedTab = HealthTab.WEIGHT },
                                        icon = { Icon(Icons.Default.MonitorWeight, contentDescription = "Kilo") },
                                        label = { Text(HealthTab.WEIGHT.title) },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == HealthTab.ANALYTICS,
                                        onClick = { selectedTab = HealthTab.ANALYTICS },
                                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Analiz & Rapor") },
                                        label = { Text(HealthTab.ANALYTICS.title) },
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
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                            .imePadding()
                            .clearFocusOnTap()
                    ) {
                        when (selectedTab) {
                            HealthTab.FOOD -> FoodScreenRoot(
                                onNavigateToWeight = { selectedTab = HealthTab.WEIGHT }
                            )
                            HealthTab.WORKOUT -> WorkoutScreenRoot()
                            HealthTab.WEIGHT -> WeightScreenRoot()
                            HealthTab.ANALYTICS -> AnalyticsScreenRoot()
                        }
                    }
                }
            }
        }
    }
}
