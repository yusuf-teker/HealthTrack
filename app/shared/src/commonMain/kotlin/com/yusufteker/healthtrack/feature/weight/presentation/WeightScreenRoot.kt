package com.yusufteker.healthtrack.feature.weight.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.utils.rememberDailyReminderNotifier
import kotlinx.coroutines.Dispatchers
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WeightScreenRoot(
    onShowToast: (String) -> Unit = {},
    viewModel: WeightViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    val dailyReminderNotifier = rememberDailyReminderNotifier()

    val latestWeightTimestamp = state.entries.maxOfOrNull { it.updatedAt }
    LaunchedEffect(state.reminderFrequency, state.reminderHour, state.reminderMinute, latestWeightTimestamp) {
        if (state.reminderFrequency == WeightReminderFrequency.OFF) {
            dailyReminderNotifier.cancelMorningWeightReminder()
        } else {
            dailyReminderNotifier.scheduleMorningWeightReminder(
                hour = state.reminderHour,
                minute = state.reminderMinute,
                frequency = state.reminderFrequency,
                lastWeightTimestamp = latestWeightTimestamp
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WeightEffect.ShowToast -> onShowToast(effect.message)
            }
        }
    }

    WeightScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}
