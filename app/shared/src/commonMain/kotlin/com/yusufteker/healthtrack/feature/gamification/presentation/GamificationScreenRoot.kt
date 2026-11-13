package com.yusufteker.healthtrack.feature.gamification.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GamificationScreenRoot(
    initialSection: GamificationSection = GamificationSection.OVERVIEW,
    onBack: () -> Unit,
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GamificationViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(initialSection) {
        viewModel.setInitialSection(initialSection)
    }

    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let {
            onShowToast(it)
            viewModel.onIntent(GamificationIntent.OnDismissInfoMessage)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            onShowToast(it)
            viewModel.onIntent(GamificationIntent.OnDismissErrorMessage)
        }
    }

    GamificationScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier
    )
}

