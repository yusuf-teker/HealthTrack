package com.yusufteker.healthtrack.feature.food.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FoodScreenRoot(
    onNavigateToWeight: (() -> Unit)? = null,
    onShowToast: (String) -> Unit = {},
    viewModel: FoodViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FoodEffect.ShowToast -> onShowToast(effect.message)
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
