package com.yusufteker.healthtrack.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreenRoot(
    viewModel: AuthViewModel = koinViewModel(),
    isGuestUpgrade: Boolean = false,
    onBackFromGuestUpgrade: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onShowToast: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)

    LaunchedEffect(isGuestUpgrade) {
        viewModel.setGuestUpgradeMode(isGuestUpgrade)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToHome -> onLoginSuccess()
                is AuthEffect.ShowToast -> onShowToast(effect.message)
            }
        }
    }

    AuthScreen(
        state = state,
        onIntent = { intent ->
            if (intent is AuthIntent.OnCancelGuestUpgradeClicked) {
                onBackFromGuestUpgrade()
            } else {
                viewModel.onIntent(intent)
            }
        }
    )
}
