package com.yusufteker.healthtrack.feature.premium.presentation

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.PaymentMethod
import com.yusufteker.healthtrack.core.model.PremiumFeatureItem
import com.yusufteker.healthtrack.core.model.SubscriptionPeriod
import com.yusufteker.healthtrack.core.model.SubscriptionPlan

fun localizedHealthTrackPlans(language: AppLanguage = LocalizationManager.currentLanguage): List<SubscriptionPlan> = listOf(
    SubscriptionPlan(
        id = "healthtrack_premium_annual",
        name = trEn("Yıllık Plan", "Annual Plan", language),
        priceText = trEn("799.99 ₺ / Yıl", "799.99 ₺ / Year", language),
        priceAmount = 799.99,
        period = SubscriptionPeriod.ANNUAL,
        discountPercentage = 33,
        isPopular = true,
        description = trEn("Aylık sadece 66.60 ₺ (%33 Tasarruf)", "Only 66.60 ₺/mo (Save 33%)", language)
    ),
    SubscriptionPlan(
        id = "healthtrack_premium_monthly",
        name = trEn("Aylık Plan", "Monthly Plan", language),
        priceText = trEn("99.99 ₺ / Ay", "99.99 ₺ / Month", language),
        priceAmount = 99.99,
        period = SubscriptionPeriod.MONTHLY,
        discountPercentage = 0,
        isPopular = false,
        description = trEn("İstediğiniz zaman iptal edebilirsiniz", "Cancel anytime", language)
    )
)

fun localizedHealthTrackFeatures(language: AppLanguage = LocalizationManager.currentLanguage): List<PremiumFeatureItem> = listOf(
    PremiumFeatureItem(
        id = "ai_food_vision",
        title = trEn("Sınırsız AI Öğün & Fotoğraf Analizi", "Unlimited AI Meal & Photo Analysis", language),
        description = trEn(
            "Tabak fotoğrafından veya metinden anında makro ve mikro besin analizi.",
            "Instant macro & micronutrient analysis from a plate photo or text.",
            language
        ),
        iconName = "auto_awesome"
    ),
    PremiumFeatureItem(
        id = "ai_meal_plan",
        title = trEn("Kişisel AI Günlük Öğün Planlayıcı", "Personal AI Daily Meal Planner", language),
        description = trEn(
            "Hedef kalorine ve protein ihtiyacına göre akıllı öğün önerileri.",
            "Smart meal recommendations tailored to your calorie and protein goals.",
            language
        ),
        iconName = "restaurant"
    ),
    PremiumFeatureItem(
        id = "weekly_bio_report",
        title = trEn("Haftalık Bio-Telemetri & WhatsApp Raporu", "Weekly Bio-Telemetry & WhatsApp Report", language),
        description = trEn(
            "Kilo, antrenman ve beslenme trendlerinin otomatik AI özeti.",
            "Automated AI summary of your weight, workout, and nutrition trends.",
            language
        ),
        iconName = "insights"
    ),
    PremiumFeatureItem(
        id = "cloud_sync",
        title = trEn("Çoklu Cihaz Bulut Senkronizasyonu", "Multi-Device Cloud Sync", language),
        description = trEn(
            "Tüm sağlık verilerin Planora ekosistemiyle anlık yedeklenir.",
            "All your health data backs up automatically across the Planora ecosystem.",
            language
        ),
        iconName = "cloud_done"
    ),
    PremiumFeatureItem(
        id = "premium_themes",
        title = trEn("Özel Bio-Telemetri Temaları", "Custom Bio-Telemetry Themes", language),
        description = trEn(
            "Obsidian, Emerald, Solar ve Crimson vurgu renklerine tam erişim.",
            "Full access to Obsidian, Emerald, Solar, and Crimson accent palettes.",
            language
        ),
        iconName = "palette"
    )
)

val defaultHealthTrackPlans: List<SubscriptionPlan> get() = localizedHealthTrackPlans()
val defaultHealthTrackFeatures: List<PremiumFeatureItem> get() = localizedHealthTrackFeatures()

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

