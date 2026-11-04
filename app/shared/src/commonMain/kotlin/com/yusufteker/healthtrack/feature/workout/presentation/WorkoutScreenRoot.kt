package com.yusufteker.healthtrack.feature.workout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkoutScreenRoot(
    viewModel: WorkoutViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WorkoutEffect.ShowToast -> {}
            }
        }
    }

    WorkoutScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}
