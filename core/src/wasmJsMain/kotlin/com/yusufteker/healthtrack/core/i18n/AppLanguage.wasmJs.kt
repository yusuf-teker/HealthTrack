package com.yusufteker.healthtrack.core.i18n

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
private fun browserLanguage(): String =
    js("(typeof navigator !== 'undefined' && navigator.language) ? navigator.language : 'en'")

actual fun detectSystemLanguage(): AppLanguage {
    val lang = try {
        browserLanguage()
    } catch (_: Throwable) {
        "en"
    }
    return AppLanguage.fromCode(lang) ?: AppLanguage.EN
}
