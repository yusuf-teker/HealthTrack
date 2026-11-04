package com.yusufteker.healthtrack

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.sync.SyncState
import com.yusufteker.healthtrack.di.appModules
import com.yusufteker.healthtrack.feature.auth.presentation.AuthScreenRoot
import com.yusufteker.healthtrack.feature.food.presentation.FoodScreenRoot
import com.yusufteker.healthtrack.feature.health.presentation.AppointmentScreenRoot
import com.yusufteker.healthtrack.feature.weight.presentation.WeightScreenRoot
import com.yusufteker.healthtrack.feature.workout.presentation.WorkoutScreenRoot
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.HealthTrackTheme
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.bounceClick
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

enum class HealthTab(val title: String) {
    FOOD("Yemek"),
    WORKOUT("Spor"),
    WEIGHT("Kilo"),
    HEALTH("Randevu")
}

@OptIn(ExperimentalMaterial3Api::class)
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

        HealthTrackTheme(darkTheme = isDark) {
            val tokenStorage: TokenStorage = koinInject()
            val syncEngine: SyncEngine = koinInject()
            val session by tokenStorage.sessionFlow.collectAsState()
            val syncState by syncEngine.syncState.collectAsState()
            val coroutineScope = rememberCoroutineScope()

            var selectedTab by remember { mutableStateOf(HealthTab.FOOD) }

            if (!session.isLoggedIn) {
                AuthScreenRoot(
                    onLoginSuccess = {
                        // User logged in
                    }
                )
            } else {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Brand Mark "H" matching Planora's Brand Mark "P"
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
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
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black
                                            )
                                        )
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
                                // Dark Mode Toggle Button
                                IconButton(
                                    onClick = {
                                        val newMode = !isDark
                                        isDark = newMode
                                        settings.putBoolean("dark_mode", newMode)
                                    },
                                    modifier = Modifier.bounceClick()
                                ) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                        contentDescription = if (isDark) "Açık Tema" else "Koyu Tema",
                                        tint = if (isDark) HealthColors.Tertiary else MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Sync indicator & button
                                val isSyncing = syncState is SyncState.Syncing
                                IconButton(
                                    onClick = { coroutineScope.launch { syncEngine.sync() } },
                                    enabled = !isSyncing,
                                    modifier = Modifier.bounceClick()
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Şimdi Senkronize Et",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Logout Button
                                IconButton(
                                    onClick = { tokenStorage.clearSession() },
                                    modifier = Modifier.bounceClick()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Çıkış Yap",
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
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
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
                                selected = selectedTab == HealthTab.HEALTH,
                                onClick = { selectedTab = HealthTab.HEALTH },
                                icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Randevu") },
                                label = { Text(HealthTab.HEALTH.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (selectedTab) {
                            HealthTab.FOOD -> FoodScreenRoot()
                            HealthTab.WORKOUT -> WorkoutScreenRoot()
                            HealthTab.WEIGHT -> WeightScreenRoot()
                            HealthTab.HEALTH -> AppointmentScreenRoot()
                        }
                    }
                }
            }
        }
    }
}
