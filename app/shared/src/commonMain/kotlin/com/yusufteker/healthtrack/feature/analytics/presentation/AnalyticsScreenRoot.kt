package com.yusufteker.healthtrack.feature.analytics.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalyticsScreenRoot(
    viewModel: AnalyticsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    AnalyticsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

