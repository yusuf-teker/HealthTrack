package com.yusufteker.healthtrack.feature.analytics.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.healthtrack.core.utils.rememberExternalAppLauncher
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalyticsScreenRoot(
    viewModel: AnalyticsViewModel = koinViewModel(),
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appLauncher = rememberExternalAppLauncher()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AnalyticsEffect.ShareToWhatsApp -> {
                    val launched = appLauncher.shareToWhatsApp(effect.message, effect.phoneNumber)
                    if (launched) {
                        viewModel.onIntent(AnalyticsIntent.OnReportSentToWhatsApp(effect.reportId))
                    } else {
                        onShowToast(com.yusufteker.healthtrack.core.i18n.trEn("WhatsApp açılamadı.", "Could not open WhatsApp."))
                    }
                }
            }
        }
    }

    AnalyticsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

