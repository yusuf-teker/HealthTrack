package com.yusufteker.healthtrack.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.LoginRequest
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.SyncEngine
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(
    private val apiClient: HealthApiClient,
    private val tokenStorage: TokenStorage,
    private val syncEngine: SyncEngine,
    private val database: HealthDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects: Flow<AuthEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow.collect { session ->
                _state.update { it.copy(session = session) }
            }
        }
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.OnIdentifierChanged -> _state.update { it.copy(identifier = intent.value, errorMessage = null) }
            is AuthIntent.OnPasswordChanged -> _state.update { it.copy(password = intent.value, errorMessage = null) }
            is AuthIntent.OnDismissError -> _state.update { it.copy(errorMessage = null) }
            is AuthIntent.OnLoginClicked -> performLogin()
            is AuthIntent.OnLogoutClicked -> performLogout()
        }
    }

    private fun performLogin() {
        val identifier = _state.value.identifier.trim()
        val password = _state.value.password.trim()

        if (identifier.isBlank() || password.isBlank()) {
            _state.update { it.copy(errorMessage = "Lütfen kullanıcı adı/e-posta ve şifrenizi giriniz.") }
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
                } catch (e: Exception) {
                    Napier.e("Giriş sonrası senkronizasyon hatası: ${e.message}", tag = "AUTH_LOG")
                }

                _state.update { it.copy(isLoading = false) }
                _effects.send(AuthEffect.NavigateToHome)
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Giriş yapılamadı. Kullanıcı adı veya şifrenizi kontrol ediniz."
                    )
                }
            }
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            database.clearAll()
            tokenStorage.clearSession()
            _effects.trySend(AuthEffect.ShowToast("Çıkış yapıldı."))
        }
    }
}
