package com.yusufteker.healthtrack.core.i18n

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun detectSystemLanguage(): AppLanguage {
    val preferred = NSLocale.preferredLanguages.firstOrNull()?.toString()
    return AppLanguage.fromCode(preferred) ?: AppLanguage.EN
}
