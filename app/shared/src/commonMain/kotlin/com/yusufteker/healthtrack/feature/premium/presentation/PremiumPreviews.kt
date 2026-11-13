package com.yusufteker.healthtrack.feature.premium.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.theme.HealthTrackTheme

@Preview
@Composable
fun PremiumScreenDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        PremiumScreen(
            state = PremiumState(isPremium = false),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun PremiumScreenLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        PremiumScreen(
            state = PremiumState(isPremium = false),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun PremiumScreenActivePreview() {
    HealthTrackTheme(darkTheme = true) {
        PremiumScreen(
            state = PremiumState(
                isPremium = true,
                premiumUntil = "2027-01-15T00:00:00Z"
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun PremiumScreenEnglishPreview() {
    val plans = localizedHealthTrackPlans(AppLanguage.EN)
    HealthTrackTheme(darkTheme = true, language = AppLanguage.EN) {
        PremiumScreen(
            state = PremiumState(
                isPremium = false,
                availablePlans = plans,
                selectedPlan = plans.first(),
                features = localizedHealthTrackFeatures(AppLanguage.EN)
            ),
            onIntent = {}
        )
    }
}
