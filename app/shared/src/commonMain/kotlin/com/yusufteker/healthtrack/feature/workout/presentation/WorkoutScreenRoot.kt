package com.yusufteker.healthtrack.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency
import com.yusufteker.healthtrack.core.utils.rememberDailyReminderNotifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkoutScreenRoot(
    onShowToast: (String) -> Unit = {},
    viewModel: WorkoutViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dailyReminderNotifier = rememberDailyReminderNotifier()

    LaunchedEffect(state.reminderFrequency) {
        if (state.reminderFrequency == WorkoutReminderFrequency.OFF) {
            dailyReminderNotifier.cancelEveningWorkoutReminder()
        } else {
            dailyReminderNotifier.scheduleEveningWorkoutReminder(hour = 18, minute = 0)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WorkoutEffect.ShowToast -> onShowToast(effect.message)
            }
        }
    }

    WorkoutScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}
