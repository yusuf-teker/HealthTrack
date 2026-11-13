package com.yusufteker.healthtrack.feature.premium.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.model.PaymentMethod
import com.yusufteker.healthtrack.core.model.RestorePurchaseRequest
import com.yusufteker.healthtrack.core.model.StoreBillingClient
import com.yusufteker.healthtrack.core.model.VerifyPurchaseRequest
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.getPlatform
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PremiumViewModel(
    private val tokenStorage: TokenStorage,
    private val apiClient: HealthApiClient,
    private val storeBillingClient: StoreBillingClient
) : ViewModel() {

    private val isIos = getPlatform().name.contains("iOS", ignoreCase = true)

    private val _state = MutableStateFlow(
        PremiumState(
            isPremium = tokenStorage.isPremium(),
            premiumUntil = tokenStorage.getPremiumUntil(),
            isIosPlatform = isIos,
            selectedPaymentMethod = if (isIos) PaymentMethod.APPLE_PAY else PaymentMethod.GOOGLE_PLAY,
            isStoreConnected = storeBillingClient.isStoreConnected
        )
    )
    val state: StateFlow<PremiumState> = _state.asStateFlow()

    private val _effects = Channel<PremiumEffect>(Channel.BUFFERED)
    val effects: Flow<PremiumEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            tokenStorage.isPremiumFlow.collect { isPrem ->
                _state.update { it.copy(isPremium = isPrem) }
            }
        }
        viewModelScope.launch {
            tokenStorage.premiumUntilFlow.collect { until ->
                _state.update { it.copy(premiumUntil = until) }
            }
        }
        loadPlansAndStatus()
    }

    fun onIntent(intent: PremiumIntent) {
        when (intent) {
            is PremiumIntent.OnPlanSelected -> {
                _state.update { it.copy(selectedPlan = intent.plan) }
            }
            is PremiumIntent.OnPaymentMethodSelected -> {
                _state.update { it.copy(selectedPaymentMethod = intent.method) }
            }
            is PremiumIntent.OnStartPurchaseClicked -> {
                _state.update { it.copy(showPaymentSheet = true, errorMessage = null, infoMessage = null) }
            }
            is PremiumIntent.OnDismissPaymentSheet -> {
                _state.update { it.copy(showPaymentSheet = false) }
            }
            is PremiumIntent.OnConfirmPurchaseClicked -> confirmPurchase()
            is PremiumIntent.OnRestorePurchasesClicked -> restorePurchases()
            is PremiumIntent.OnRefreshStatus -> loadPlansAndStatus()
            is PremiumIntent.OnDismissMessages -> {
                _state.update { it.copy(errorMessage = null, infoMessage = null) }
            }
            is PremiumIntent.OnBackClicked -> {
                viewModelScope.launch {
                    _effects.send(PremiumEffect.NavigateBack)
                }
            }
        }
    }

    private fun loadPlansAndStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }

            apiClient.fetchSubscriptionPlans().onSuccess { plans ->
                if (plans.isNotEmpty()) {
                    _state.update { current ->
                        val matchingSelected = plans.firstOrNull { it.id == current.selectedPlan.id } ?: plans.first()
                        current.copy(availablePlans = plans, selectedPlan = matchingSelected)
                    }
                }
            }

            if (!tokenStorage.getAccessToken().isNullOrBlank()) {
                apiClient.fetchSubscriptionStatus().onSuccess { status ->
                    tokenStorage.setPremium(status.isPremium, status.premiumUntil)
                    _state.update {
                        it.copy(
                            isPremium = status.isPremium,
                            premiumUntil = status.premiumUntil,
                            activePlanId = status.activePlanId,
                            provider = status.provider
                        )
                    }
                }
            }

            _state.update { it.copy(isSyncing = false) }
        }
    }

    private fun confirmPurchase() {
        val current = _state.value
        val plan = current.selectedPlan
        val provider = when (current.selectedPaymentMethod) {
            PaymentMethod.APPLE_PAY -> "APPLE_APP_STORE"
            PaymentMethod.GOOGLE_PLAY -> "GOOGLE_PLAY"
            PaymentMethod.CREDIT_CARD -> "CREDIT_CARD"
        }

        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true, errorMessage = null, infoMessage = null) }

            val purchaseResult = storeBillingClient.purchase(plan)
            if (!purchaseResult.isSuccess) {
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        showPaymentSheet = false,
                        infoMessage = purchaseResult.errorMessage
                            ?: "Mağaza abonelik SDK'sı henüz bağlanmadı."
                    )
                }
                return@launch
            }

            apiClient.verifySubscriptionPurchase(
                VerifyPurchaseRequest(
                    appId = "healthtrack",
                    planId = plan.id,
                    provider = provider,
                    purchaseToken = purchaseResult.purchaseToken,
                    transactionId = purchaseResult.transactionId
                )
            ).onSuccess { verifyResp ->
                if (verifyResp.success && verifyResp.isPremium) {
                    tokenStorage.setPremium(true, verifyResp.premiumUntil)
                    _state.update {
                        it.copy(
                            isPurchasing = false,
                            showPaymentSheet = false,
                            isPremium = true,
                            premiumUntil = verifyResp.premiumUntil,
                            activePlanId = verifyResp.planId,
                            provider = verifyResp.provider,
                            infoMessage = verifyResp.message
                        )
                    }
                    _effects.send(PremiumEffect.ShowToast("HealthTrack Premium aktif edildi!"))
                } else {
                    _state.update {
                        it.copy(
                            isPurchasing = false,
                            showPaymentSheet = false,
                            infoMessage = verifyResp.message.ifBlank { "Abonelik doğrulaması bekleniyor." }
                        )
                    }
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        showPaymentSheet = false,
                        errorMessage = err.message ?: "Satın alma doğrulaması başarısız."
                    )
                }
            }
        }
    }

    private fun restorePurchases() {
        val provider = if (isIos) "APPLE_APP_STORE" else "GOOGLE_PLAY"
        viewModelScope.launch {
            _state.update { it.copy(isRestoring = true, errorMessage = null, infoMessage = null) }

            val storeRestore = if (storeBillingClient.isStoreConnected) {
                storeBillingClient.restore()
            } else {
                null
            }

            apiClient.restoreSubscriptionPurchases(
                RestorePurchaseRequest(
                    appId = "healthtrack",
                    provider = provider,
                    purchaseToken = storeRestore?.purchaseToken,
                    transactionId = storeRestore?.transactionId
                )
            ).onSuccess { status ->
                tokenStorage.setPremium(status.isPremium, status.premiumUntil)
                _state.update {
                    it.copy(
                        isRestoring = false,
                        isPremium = status.isPremium,
                        premiumUntil = status.premiumUntil,
                        activePlanId = status.activePlanId,
                        provider = status.provider,
                        infoMessage = if (status.isPremium) {
                            "Premium üyeliğiniz başarıyla geri yüklendi."
                        } else {
                            "Aktif mağaza aboneliği bulunamadı."
                        }
                    )
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isRestoring = false,
                        errorMessage = err.message ?: "Geri yükleme başarısız."
                    )
                }
            }
        }
    }
}

