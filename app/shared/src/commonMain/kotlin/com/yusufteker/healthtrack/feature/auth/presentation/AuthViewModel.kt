package com.yusufteker.healthtrack.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.AuthResponse
import com.yusufteker.healthtrack.core.model.LoginRequest
import com.yusufteker.healthtrack.core.model.RegisterRequest
import com.yusufteker.healthtrack.core.model.SendRegisterCodeRequest
import com.yusufteker.healthtrack.core.model.UserSession
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

    fun setGuestUpgradeMode(enabled: Boolean) {
        val hasGuestData = enabled && database.hasDataForUser(UserSession.GUEST_USER_ID)
        _state.update {
            it.copy(
                isGuestUpgrade = enabled,
                hasGuestDataToTransfer = hasGuestData,
                transferGuestData = true,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.OnToggleAuthMode -> _state.update {
                it.copy(isRegisterMode = intent.isRegister, errorMessage = null, infoMessage = null)
            }
            is AuthIntent.OnIdentifierChanged -> _state.update { it.copy(identifier = intent.value, errorMessage = null) }
            is AuthIntent.OnPasswordChanged -> _state.update { it.copy(password = intent.value, errorMessage = null) }
            is AuthIntent.OnRegisterNameChanged -> _state.update { it.copy(registerName = intent.value, errorMessage = null) }
            is AuthIntent.OnRegisterUsernameChanged -> _state.update { it.copy(registerUsername = intent.value, errorMessage = null) }
            is AuthIntent.OnRegisterEmailChanged -> _state.update { it.copy(registerEmail = intent.value, errorMessage = null) }
            is AuthIntent.OnRegisterPasswordChanged -> _state.update { it.copy(registerPassword = intent.value, errorMessage = null) }
            is AuthIntent.OnRegisterCodeChanged -> _state.update { it.copy(registerCode = intent.value.filter { ch -> ch.isDigit() }.take(6), errorMessage = null) }
            is AuthIntent.OnLanguageChanged -> LocalizationManager.setLanguage(intent.language, settings)
            is AuthIntent.OnTransferGuestDataChanged -> _state.update { it.copy(transferGuestData = intent.transfer) }
            is AuthIntent.OnDismissError -> _state.update { it.copy(errorMessage = null, infoMessage = null) }
            is AuthIntent.OnLoginClicked -> performLogin()
            is AuthIntent.OnSendRegisterCodeClicked -> sendRegisterCode()
            is AuthIntent.OnRegisterClicked -> performRegister()
            is AuthIntent.OnEditRegisterInfoClicked -> _state.update {
                it.copy(isRegisterCodeSent = false, registerCode = "", errorMessage = null, infoMessage = null)
            }
            is AuthIntent.OnContinueAsGuestClicked -> continueAsGuest()
            is AuthIntent.OnCancelGuestUpgradeClicked -> {
                // Handled by AuthScreenRoot callback
            }
            is AuthIntent.OnLogoutClicked -> performLogout()
        }
    }

    private fun continueAsGuest() {
        viewModelScope.launch {
            val lang = _state.value.language
            tokenStorage.startGuestSession(
                guestName = trEn("Misafir", "Guest", lang)
            )
            _effects.send(AuthEffect.NavigateToHome)
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
            _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

            val result = apiClient.login(LoginRequest(identifier, password))
            result.onSuccess { response ->
                handleAuthSuccess(response, isRegister = false)
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

    private fun sendRegisterCode() {
        val name = _state.value.registerName.trim()
        val username = _state.value.registerUsername.trim()
        val email = _state.value.registerEmail.trim()
        val password = _state.value.registerPassword.trim()
        val lang = _state.value.language

        if (name.isBlank() || username.length < 3 || !email.contains("@") || password.length < 6) {
            _state.update {
                it.copy(
                    errorMessage = trEn(
                        "Lütfen ad soyad, en az 3 karakterli kullanıcı adı, geçerli e-posta ve en az 6 karakterli şifre giriniz.",
                        "Please enter your name, a username (min 3 chars), valid email, and password (min 6 chars).",
                        lang
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = apiClient.sendRegisterCode(
                SendRegisterCodeRequest(email = email, username = username)
            )
            result.onSuccess {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRegisterCodeSent = true,
                        infoMessage = trEn(
                            "Doğrulama kodu $email adresine gönderildi.",
                            "Verification code sent to $email.",
                            lang
                        )
                    )
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = formatActionableApiError(
                            error = err,
                            operationTr = "Doğrulama kodu",
                            operationEn = "Verification code",
                            isCredentialAuth = false,
                            language = _state.value.language
                        )
                    )
                }
            }
        }
    }

    private fun performRegister() {
        val name = _state.value.registerName.trim()
        val username = _state.value.registerUsername.trim()
        val email = _state.value.registerEmail.trim()
        val password = _state.value.registerPassword.trim()
        val code = _state.value.registerCode.trim()
        val lang = _state.value.language

        if (code.length < 6) {
            _state.update {
                it.copy(
                    errorMessage = trEn(
                        "Lütfen e-postanıza gelen 6 haneli doğrulama kodunu giriniz.",
                        "Please enter the 6-digit verification code sent to your email.",
                        lang
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = apiClient.register(
                RegisterRequest(
                    name = name,
                    username = username,
                    email = email,
                    password = password,
                    code = code
                )
            )
            result.onSuccess { response ->
                handleAuthSuccess(response, isRegister = true)
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = formatActionableApiError(
                            error = err,
                            operationTr = "Kayıt",
                            operationEn = "Registration",
                            isCredentialAuth = false,
                            language = _state.value.language
                        )
                    )
                }
            }
        }
    }

    private suspend fun handleAuthSuccess(response: AuthResponse, isRegister: Boolean) {
        val wasGuest = tokenStorage.isGuest()
        val shouldMigrateGuest = wasGuest && _state.value.transferGuestData
        var migratedCount = 0

        if (shouldMigrateGuest) {
            val migration = database.migrateGuestDataToUser(
                guestUserId = UserSession.GUEST_USER_ID,
                targetUserId = response.userId,
                preferGuestGoal = isRegister
            )
            migratedCount = migration.totalRecords
        } else if (wasGuest) {
            database.clearUserData(UserSession.GUEST_USER_ID)
        }

        tokenStorage.saveSession(
            userId = response.userId,
            name = response.name,
            username = response.username,
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            avatarId = response.avatarId,
            profileImageUrl = response.profileImageUrl,
            isPremium = response.isPremium,
            preserveGuestPreferences = shouldMigrateGuest
        )
        tokenStorage.setPremium(response.isPremium, response.premiumUntil)

        // Await initial sync so that migrated guest records are pushed and existing
        // cloud weights, goals, workouts, and foods are restored BEFORE navigating to Home.
        try {
            syncEngine.sync(forceFullPull = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("Giriş/Kayıt sonrası senkronizasyon hatası: ${e.message}", tag = "AUTH_LOG")
        }

        if (shouldMigrateGuest && migratedCount > 0) {
            _effects.send(
                AuthEffect.ShowToast(
                    trEn(
                        "Misafir verilerin ($migratedCount kayıt) hesabına aktarıldı!",
                        "Your guest data ($migratedCount records) has been transferred to your account!",
                        _state.value.language
                    )
                )
            )
        }

        _state.update {
            it.copy(
                isLoading = false,
                isGuestUpgrade = false,
                hasGuestDataToTransfer = false
            )
        }
        _effects.send(AuthEffect.NavigateToHome)
    }

    private fun performLogout() {
        viewModelScope.launch {
            database.clearAll()
            tokenStorage.clearSession()
            _effects.trySend(AuthEffect.ShowToast(trEn("Çıkış yapıldı.", "Signed out.", _state.value.language)))
        }
    }
}
