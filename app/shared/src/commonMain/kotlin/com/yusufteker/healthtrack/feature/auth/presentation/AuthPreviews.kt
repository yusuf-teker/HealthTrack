package com.yusufteker.healthtrack.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.theme.HealthTrackTheme

@Preview
@Composable
fun AuthScreenDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        AuthScreen(
            state = AuthState(
                identifier = "demo@healthtrack.com",
                password = "SecretPassword123"
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun AuthScreenLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        AuthScreen(
            state = AuthState(
                identifier = "demo@healthtrack.com",
                password = ""
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun AuthScreenLoadingPreview() {
    HealthTrackTheme(darkTheme = true) {
        AuthScreen(
            state = AuthState(
                identifier = "demo@healthtrack.com",
                password = "••••••••",
                isLoading = true
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
fun AuthScreenEnglishPreview() {
    HealthTrackTheme(darkTheme = true, language = AppLanguage.EN) {
        AuthScreen(
            state = AuthState(
                identifier = "demo@healthtrack.com",
                password = "SecretPassword123",
                language = AppLanguage.EN
            ),
            onIntent = {}
        )
    }
}
