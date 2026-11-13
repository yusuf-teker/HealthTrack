package com.yusufteker.healthtrack.feature.premium.presentation

import com.yusufteker.healthtrack.core.model.PaymentMethod
import com.yusufteker.healthtrack.core.model.PremiumFeatureItem
import com.yusufteker.healthtrack.core.model.SubscriptionPeriod
import com.yusufteker.healthtrack.core.model.SubscriptionPlan

val defaultHealthTrackPlans = listOf(
    SubscriptionPlan(
        id = "healthtrack_premium_annual",
        name = "Yıllık Plan",
        priceText = "799.99 ₺ / Yıl",
        priceAmount = 799.99,
        period = SubscriptionPeriod.ANNUAL,
        discountPercentage = 33,
        isPopular = true,
        description = "Aylık sadece 66.60 ₺ (%33 Tasarruf)"
    ),
    SubscriptionPlan(
        id = "healthtrack_premium_monthly",
        name = "Aylık Plan",
        priceText = "99.99 ₺ / Ay",
        priceAmount = 99.99,
        period = SubscriptionPeriod.MONTHLY,
        discountPercentage = 0,
        isPopular = false,
        description = "İstediğiniz zaman iptal edebilirsiniz"
    )
)

val defaultHealthTrackFeatures = listOf(
    PremiumFeatureItem(
        id = "ai_food_vision",
        title = "Sınırsız AI Öğün & Fotoğraf Analizi",
        description = "Tabak fotoğrafından veya metinden anında makro ve mikro besin analizi.",
        iconName = "auto_awesome"
    ),
    PremiumFeatureItem(
        id = "ai_meal_plan",
        title = "Kişisel AI Günlük Öğün Planlayıcı",
        description = "Hedef kalorine ve protein ihtiyacına göre akıllı öğün önerileri.",
        iconName = "restaurant"
    ),
    PremiumFeatureItem(
        id = "weekly_bio_report",
        title = "Haftalık Bio-Telemetri & WhatsApp Raporu",
        description = "Kilo, antrenman ve beslenme trendlerinin otomatik AI özeti.",
        iconName = "insights"
    ),
    PremiumFeatureItem(
        id = "cloud_sync",
        title = "Çoklu Cihaz Bulut Senkronizasyonu",
        description = "Tüm sağlık verilerin Planora ekosistemiyle anlık yedeklenir.",
        iconName = "cloud_done"
    ),
    PremiumFeatureItem(
        id = "premium_themes",
        title = "Özel Bio-Telemetri Temaları",
        description = "Obsidian, Emerald, Solar ve Crimson vurgu renklerine tam erişim.",
        iconName = "palette"
    )
)

data class PremiumState(
    val isPremium: Boolean = false,
    val premiumUntil: String? = null,
    val activePlanId: String? = null,
    val provider: String? = null,
    val availablePlans: List<SubscriptionPlan> = defaultHealthTrackPlans,
    val selectedPlan: SubscriptionPlan = defaultHealthTrackPlans.first(),
    val features: List<PremiumFeatureItem> = defaultHealthTrackFeatures,
    val isIosPlatform: Boolean = false,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.GOOGLE_PLAY,
    val isStoreConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val showPaymentSheet: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface PremiumIntent {
    data class OnPlanSelected(val plan: SubscriptionPlan) : PremiumIntent
    data class OnPaymentMethodSelected(val method: PaymentMethod) : PremiumIntent
    data object OnStartPurchaseClicked : PremiumIntent
    data object OnConfirmPurchaseClicked : PremiumIntent
    data object OnDismissPaymentSheet : PremiumIntent
    data object OnRestorePurchasesClicked : PremiumIntent
    data object OnRefreshStatus : PremiumIntent
    data object OnBackClicked : PremiumIntent
    data object OnDismissMessages : PremiumIntent
}

sealed interface PremiumEffect {
    data object NavigateBack : PremiumEffect
    data class ShowToast(val message: String) : PremiumEffect
}

