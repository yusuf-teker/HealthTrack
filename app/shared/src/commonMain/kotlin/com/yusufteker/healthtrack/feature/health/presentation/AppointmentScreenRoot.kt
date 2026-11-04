package com.yusufteker.healthtrack.feature.health.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppointmentScreenRoot(
    viewModel: AppointmentViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AppointmentEffect.ShowToast -> {}
            }
        }
    }

    AppointmentScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}
