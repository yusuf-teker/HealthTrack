package com.yusufteker.healthtrack.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.LoginRequest
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.network.formatActionableApiError
import com.yusufteker.healthtrack.core.sync.SyncEngine
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(
    private val apiClient: HealthApiClient,
    private val tokenStorage: TokenStorage,
    private val syncEngine: SyncEngine,
    private val database: HealthDatabase,
    private val settings: Settings
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState(language = LocalizationManager.currentLanguage))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects: Flow<AuthEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow.collect { session ->
                _state.update { it.copy(session = session) }
            }
        }
        viewModelScope.launch {
            LocalizationManager.languageFlow.collect { lang ->
                _state.update { it.copy(language = lang) }
            }
        }
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.OnIdentifierChanged -> _state.update { it.copy(identifier = intent.value, errorMessage = null) }
            is AuthIntent.OnPasswordChanged -> _state.update { it.copy(password = intent.value, errorMessage = null) }
            is AuthIntent.OnLanguageChanged -> LocalizationManager.setLanguage(intent.language, settings)
            is AuthIntent.OnDismissError -> _state.update { it.copy(errorMessage = null) }
            is AuthIntent.OnLoginClicked -> performLogin()
            is AuthIntent.OnLogoutClicked -> performLogout()
        }
    }

    private fun performLogin() {
        val identifier = _state.value.identifier.trim()
        val password = _state.value.password.trim()
        val lang = _state.value.language

        if (identifier.isBlank() || password.isBlank()) {
            _state.update {
                it.copy(
                    errorMessage = trEn(
                        "Lütfen kullanıcı adı/e-posta ve şifrenizi giriniz.",
                        "Please enter your username/email and password.",
                        lang
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = apiClient.login(LoginRequest(identifier, password))
            result.onSuccess { response ->
                tokenStorage.saveSession(
                    userId = response.userId,
                    name = response.name,
                    username = response.username,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    avatarId = response.avatarId,
                    isPremium = response.isPremium
                )
                tokenStorage.setPremium(response.isPremium, response.premiumUntil)

                // Await initial sync so that weights, goals, workouts, and foods are restored
                // BEFORE navigating to Home and before checking profile configuration.
                try {
                    syncEngine.sync(forceFullPull = true)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.e("Giriş sonrası senkronizasyon hatası: ${e.message}", tag = "AUTH_LOG")
                }

                _state.update { it.copy(isLoading = false) }
                _effects.send(AuthEffect.NavigateToHome)
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = formatActionableApiError(
                            error = err,
                            operationTr = "Giriş",
                            operationEn = "Sign in",
                            isCredentialAuth = true,
                            language = _state.value.language
                        )
                    )
                }
            }
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            database.clearAll()
            tokenStorage.clearSession()
            _effects.trySend(AuthEffect.ShowToast(trEn("Çıkış yapıldı.", "Signed out.", _state.value.language)))
        }
    }
}
