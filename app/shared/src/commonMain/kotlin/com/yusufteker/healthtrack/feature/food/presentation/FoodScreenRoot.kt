package com.yusufteker.healthtrack.feature.food.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FoodScreenRoot(
    onNavigateToWeight: (() -> Unit)? = null,
    viewModel: FoodViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FoodEffect.ShowToast -> {}
            }
        }
    }

    FoodScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateToWeight = onNavigateToWeight,
        modifier = modifier
    )
}
