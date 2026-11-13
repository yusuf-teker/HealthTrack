package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.serialization.Serializable

@Serializable
enum class SubscriptionPeriod {
    MONTHLY,
    ANNUAL
}

@Serializable
enum class PaymentMethod {
    GOOGLE_PLAY,
    APPLE_PAY,
    CREDIT_CARD
}

@Serializable
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val priceText: String,
    val priceAmount: Double,
    val currency: String = "₺",
    val period: SubscriptionPeriod,
    val discountPercentage: Int = 0,
    val isPopular: Boolean = false,
    val description: String = ""
)

@Serializable
data class PurchaseResult(
    val isSuccess: Boolean,
    val transactionId: String? = null,
    val purchaseToken: String? = null,
    val planId: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val errorMessage: String? = null,
    val isSimulation: Boolean = true
)

@Serializable
data class PremiumFeatureItem(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String
)

@Serializable
data class VerifyPurchaseRequest(
    val appId: String = "healthtrack",
    val planId: String,
    val provider: String,
    val purchaseToken: String? = null,
    val transactionId: String? = null,
    val receiptData: String? = null
)

@Serializable
data class VerifyPurchaseResponse(
    val success: Boolean,
    val isPremium: Boolean = false,
    val premiumUntil: String? = null,
    val planId: String? = null,
    val provider: String? = null,
    val status: String? = null,
    val message: String = "",
    val billingConfigured: Boolean = false
)

@Serializable
data class SubscriptionStatusResponse(
    val isPremium: Boolean = false,
    val premiumUntil: String? = null,
    val activePlanId: String? = null,
    val provider: String? = null,
    val status: String = "NONE",
    val autoRenew: Boolean = false,
    val billingConfigured: Boolean = false
)

@Serializable
data class RestorePurchaseRequest(
    val appId: String = "healthtrack",
    val provider: String,
    val purchaseToken: String? = null,
    val transactionId: String? = null
)

/**
 * Platform store billing bridge (Google Play Billing on Android, StoreKit 2 on iOS).
 * Implement and bind via Koin when store product IDs and merchant accounts are ready.
 */
interface StoreBillingClient {
    val isStoreConnected: Boolean
    suspend fun purchase(plan: SubscriptionPlan): PurchaseResult
    suspend fun restore(): PurchaseResult
}

/**
 * Default placeholder implementation until Google Play Billing / StoreKit 2 SDK is wired.
 */
class PendingStoreBillingClient : StoreBillingClient {
    override val isStoreConnected: Boolean = false

    override suspend fun purchase(plan: SubscriptionPlan): PurchaseResult {
        return PurchaseResult(
            isSuccess = false,
            planId = plan.id,
            errorMessage = trEn(
                "Mağaza abonelik SDK'sı henüz bağlanmadı. Google Play / App Store ürün ID'leri eklendikten sonra aktif olacaktır.",
                "Store billing SDK is not connected yet. It will become active once Google Play / App Store product IDs are configured."
            ),
            isSimulation = true
        )
    }

    override suspend fun restore(): PurchaseResult {
        return PurchaseResult(
            isSuccess = false,
            errorMessage = trEn(
                "Mağaza abonelik SDK'sı henüz bağlanmadı.",
                "Store billing SDK is not connected yet."
            ),
            isSimulation = true
        )
    }
}

