package com.yusufteker.healthtrack.core.i18n

import java.util.Locale

actual fun detectSystemLanguage(): AppLanguage {
    return if (Locale.getDefault().language.equals("tr", ignoreCase = true)) {
        AppLanguage.TR
    } else {
        AppLanguage.EN
    }
}
