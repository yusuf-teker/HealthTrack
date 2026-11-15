package com.yusufteker.healthtrack.feature.auth.presentation

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.model.UserSession

data class AuthState(
    val isRegisterMode: Boolean = false,
    val identifier: String = "",
    val password: String = "",
    val registerName: String = "",
    val registerUsername: String = "",
    val registerEmail: String = "",
    val registerPassword: String = "",
    val registerCode: String = "",
    val isRegisterCodeSent: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val session: UserSession = UserSession(0, "", "", "", "", null, false),
    val language: AppLanguage = LocalizationManager.currentLanguage
)

sealed interface AuthIntent {
    data class OnToggleAuthMode(val isRegister: Boolean) : AuthIntent
    data class OnIdentifierChanged(val value: String) : AuthIntent
    data class OnPasswordChanged(val value: String) : AuthIntent
    data class OnRegisterNameChanged(val value: String) : AuthIntent
    data class OnRegisterUsernameChanged(val value: String) : AuthIntent
    data class OnRegisterEmailChanged(val value: String) : AuthIntent
    data class OnRegisterPasswordChanged(val value: String) : AuthIntent
    data class OnRegisterCodeChanged(val value: String) : AuthIntent
    data class OnLanguageChanged(val language: AppLanguage) : AuthIntent
    data object OnLoginClicked : AuthIntent
    data object OnSendRegisterCodeClicked : AuthIntent
    data object OnRegisterClicked : AuthIntent
    data object OnEditRegisterInfoClicked : AuthIntent
    data object OnLogoutClicked : AuthIntent
    data object OnDismissError : AuthIntent
}

sealed interface AuthEffect {
    data class ShowToast(val message: String) : AuthEffect
    data object NavigateToHome : AuthEffect
}
