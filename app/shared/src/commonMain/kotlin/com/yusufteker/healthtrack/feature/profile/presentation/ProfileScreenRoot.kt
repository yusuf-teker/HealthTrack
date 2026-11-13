package com.yusufteker.healthtrack.feature.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency
import com.yusufteker.healthtrack.core.utils.rememberDailyReminderNotifier
import com.yusufteker.healthtrack.theme.AppThemeColor
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreenRoot(
    onBack: () -> Unit,
    onThemeAccentChanged: (AppThemeColor) -> Unit,
    onDarkModeChanged: (Boolean) -> Unit,
    onOpenPremium: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onOpenWidgetPromo: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dailyReminderNotifier = rememberDailyReminderNotifier()

    LaunchedEffect(state.workoutReminderFrequency) {
        if (state.workoutReminderFrequency == WorkoutReminderFrequency.OFF) {
            dailyReminderNotifier.cancelEveningWorkoutReminder()
        } else {
            dailyReminderNotifier.scheduleEveningWorkoutReminder(hour = 18, minute = 0)
        }
    }

    LaunchedEffect(state.weightReminderFrequency) {
        if (state.weightReminderFrequency == WeightReminderFrequency.OFF) {
            dailyReminderNotifier.cancelMorningWeightReminder()
        } else {
            dailyReminderNotifier.scheduleMorningWeightReminder(hour = 9, minute = 0)
        }
    }

    LaunchedEffect(state.themeAccent) {
        onThemeAccentChanged(state.themeAccent)
    }

    LaunchedEffect(state.isDarkMode) {
        onDarkModeChanged(state.isDarkMode)
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(ProfileIntent.OnScreenAppeared)
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.NavigateBack -> onBack()
                is ProfileEffect.LoggedOut -> onLogout()
                is ProfileEffect.ShowToast -> onShowToast(effect.message)
            }
        }
    }

    ProfileScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onOpenPremium = onOpenPremium,
        onReplayOnboarding = onReplayOnboarding,
        onOpenWidgetPromo = onOpenWidgetPromo,
        onLogout = onLogout,
        modifier = modifier
    )
}

