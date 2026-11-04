package com.yusufteker.healthtrack.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreenRoot(
    viewModel: AuthViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToHome -> onLoginSuccess()
                is AuthEffect.ShowToast -> {}
            }
        }
    }

    AuthScreen(
        state = state,
        onIntent = viewModel::onIntent
    )
}
