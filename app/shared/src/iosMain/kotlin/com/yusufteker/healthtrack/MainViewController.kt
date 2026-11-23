package com.yusufteker.healthtrack

import androidx.compose.ui.window.ComposeUIViewController
import com.yusufteker.healthtrack.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
        Napier.base(DebugAntilog())
    }
) {
    App()
}