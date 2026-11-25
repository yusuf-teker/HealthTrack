package com.yusufteker.healthtrack.feature.premium.presentation

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.PaymentMethod
import com.yusufteker.healthtrack.core.model.PremiumFeatureItem
import com.yusufteker.healthtrack.core.model.SubscriptionPeriod
import com.yusufteker.healthtrack.core.model.SubscriptionPlan

fun localizedHealthTrackPlans(
    language: AppLanguage = LocalizationManager.currentLanguage,
    isSpecialOffer: Boolean = false
): List<SubscriptionPlan> {
    val annualPlan = if (isSpecialOffer) {
        SubscriptionPlan(
            id = "healthtrack_premium_annual_offer",
            name = trEn("Özel Yıllık Fırsat", "Special Annual Offer", language),
            priceText = trEn("249.99 ₺ / Yıl", "249.99 ₺ / Year", language),
            originalPriceText = "299.99 ₺",
            priceAmount = 249.99,
            period = SubscriptionPeriod.ANNUAL,
            discountPercentage = 48,
            isPopular = true,
            isLimitedTimeOffer = true,
            offerBadgeText = trEn("🔥 SINIRLI SÜRELİ FIRSAT", "🔥 LIMITED TIME OFFER", language),
            storeOfferId = "limited_annual_offer",
            description = trEn("Aylık sadece 20.80 ₺ (%48 Tasarruf)", "Only 20.80 ₺/mo (Save 48%)", language)
        )
    } else {
        SubscriptionPlan(
            id = "healthtrack_premium_annual",
            name = trEn("Yıllık Plan", "Annual Plan", language),
            priceText = trEn("299.99 ₺ / Yıl", "299.99 ₺ / Year", language),
            originalPriceText = null,
            priceAmount = 299.99,
            period = SubscriptionPeriod.ANNUAL,
            discountPercentage = 37,
            isPopular = true,
            isLimitedTimeOffer = false,
            offerBadgeText = null,
            description = trEn("Aylık sadece 25.00 ₺ (%37 Tasarruf)", "Only 25.00 ₺/mo (Save 37%)", language)
        )
    }

    val monthlyPlan = SubscriptionPlan(
        id = "healthtrack_premium_monthly",
        name = trEn("Aylık Plan", "Monthly Plan", language),
        priceText = trEn("39.99 ₺ / Ay", "39.99 ₺ / Month", language),
        priceAmount = 39.99,
        period = SubscriptionPeriod.MONTHLY,
        discountPercentage = 0,
        isPopular = false,
        isLimitedTimeOffer = false,
        description = trEn("İstediğiniz zaman iptal edebilirsiniz", "Cancel anytime", language)
    )

    return listOf(annualPlan, monthlyPlan)
}

fun localizedHealthTrackFeatures(language: AppLanguage = LocalizationManager.currentLanguage): List<PremiumFeatureItem> = listOf(
    PremiumFeatureItem(
        id = "ai_food_vision",
        title = trEn("Genişletilmiş AI Öğün & Fotoğraf Kotası", "Expanded AI Meal & Photo Quota", language),
        description = trEn(
            "Günlük 3 hak sınırına takılmadan metinden, sesten ve tabak/etiket fotoğrafından anında makro & mikro besin analizi.",
            "Instant macro & micronutrient analysis from text, voice, or plate/label photos without the 3/day free limit.",
            language
        ),
        iconName = "auto_awesome"
    ),
    PremiumFeatureItem(
        id = "ai_workout",
        title = trEn("Sınırsız AI Spor & Kalori Hesaplama", "Unlimited AI Workout & Calorie Analysis", language),
        description = trEn(
            "Antrenmanlarını serbest metinle yaz, kilona göre yakılan kaloriyi ve süreyi anında hesapla.",
            "Describe your workouts in plain text and instantly calculate calories burned based on your weight.",
            language
        ),
        iconName = "fitness_center"
    ),
    PremiumFeatureItem(
        id = "ai_recipes_inventory",
        title = trEn("AI Özel Tarif & Ürün Etiketi Analizi", "AI Custom Recipes & Label Analysis", language),
        description = trEn(
            "Kalan kalorine göre yapay zeka tarif önerileri oluştur ve ürün etiketlerini tek dokunuşla envanterine ekle.",
            "Generate AI meal recipes for your remaining calories and scan product labels straight into your inventory.",
            language
        ),
        iconName = "restaurant"
    ),
    PremiumFeatureItem(
        id = "core_always_free",
        title = trEn("Diğer Tüm Özellikler Herkese Ücretsiz", "All Other Features Stay Free", language),
        description = trEn(
            "Bulut yedekleme, haftalık raporlar, öğün planlayıcı, akıllı saat/tartı senkronu ve tüm temalar ücretsiz hesaplarda da açıktır.",
            "Cloud sync, weekly reports, meal planner, watch/scale sync, and all themes are included free for all accounts.",
            language
        ),
        iconName = "check_circle"
    )
)

val defaultHealthTrackPlans: List<SubscriptionPlan> get() = localizedHealthTrackPlans()
val defaultHealthTrackFeatures: List<PremiumFeatureItem> get() = localizedHealthTrackFeatures()

data class PremiumState(
    val isPremium: Boolean = false,
    val premiumUntil: String? = null,
    val activePlanId: String? = null,
    val provider: String? = null,
    val offerType: String? = null,
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
) {
    val isSpecialOffer: Boolean get() = !offerType.isNullOrBlank()
}

sealed interface PremiumIntent {
    data class SetOfferType(val offerType: String?) : PremiumIntent
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

