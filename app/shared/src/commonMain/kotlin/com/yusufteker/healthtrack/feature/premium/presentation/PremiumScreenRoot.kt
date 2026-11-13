package com.yusufteker.healthtrack.feature.premium.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PremiumScreenRoot(
    viewModel: PremiumViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onShowToast: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PremiumEffect.NavigateBack -> onNavigateBack()
                is PremiumEffect.ShowToast -> onShowToast(effect.message)
            }
        }
    }

    PremiumScreen(
        state = state,
        onIntent = viewModel::onIntent
    )
}

