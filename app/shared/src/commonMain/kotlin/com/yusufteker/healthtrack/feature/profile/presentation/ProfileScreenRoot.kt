package com.yusufteker.healthtrack.feature.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.yusufteker.healthtrack.theme.AppThemeColor
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreenRoot(
    onBack: () -> Unit,
    onThemeAccentChanged: (AppThemeColor) -> Unit,
    onDarkModeChanged: (Boolean) -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.themeAccent) {
        onThemeAccentChanged(state.themeAccent)
    }

    LaunchedEffect(state.isDarkMode) {
        onDarkModeChanged(state.isDarkMode)
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(ProfileIntent.OnScreenAppeared)
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.NavigateBack -> onBack()
                is ProfileEffect.LoggedOut -> onLogout()
                is ProfileEffect.ShowToast -> {
                    // Handled inside Screen / messages
                }
            }
        }
    }

    ProfileScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onLogout = onLogout,
        modifier = modifier
    )
}

