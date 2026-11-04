package com.yusufteker.healthtrack.feature.auth.presentation

import com.yusufteker.healthtrack.core.model.UserSession

data class AuthState(
    val identifier: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val session: UserSession = UserSession(0, "", "", "", "", null, false)
)

sealed interface AuthIntent {
    data class OnIdentifierChanged(val value: String) : AuthIntent
    data class OnPasswordChanged(val value: String) : AuthIntent
    data object OnLoginClicked : AuthIntent
    data object OnLogoutClicked : AuthIntent
    data object OnDismissError : AuthIntent
}

sealed interface AuthEffect {
    data class ShowToast(val message: String) : AuthEffect
    data object NavigateToHome : AuthEffect
}
