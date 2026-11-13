package com.yusufteker.healthtrack.core.i18n

private fun browserLanguage(): String =
    js("(typeof navigator !== 'undefined' && navigator.language) ? navigator.language : 'en'").unsafeCast<String>()

actual fun detectSystemLanguage(): AppLanguage {
    val lang = try {
        browserLanguage()
    } catch (_: Throwable) {
        "en"
    }
    return AppLanguage.fromCode(lang) ?: AppLanguage.EN
}
