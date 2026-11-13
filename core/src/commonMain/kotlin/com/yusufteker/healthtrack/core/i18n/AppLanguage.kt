package com.yusufteker.healthtrack.core.i18n

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported languages across HealthTrack (KMP App & Web).
 */
enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val shortLabel: String,
    val bcp47Tag: String
) {
    TR(code = "tr", nativeLabel = "Türkçe", shortLabel = "TR", bcp47Tag = "tr-TR"),
    EN(code = "en", nativeLabel = "English", shortLabel = "EN", bcp47Tag = "en-US");

    companion object {
        fun fromCode(code: String?): AppLanguage? {
            if (code.isNullOrBlank()) return null
            val normalized = code.trim().lowercase()
            return when {
                normalized.startsWith("tr") -> TR
                normalized.startsWith("en") -> EN
                else -> null
            }
        }
    }
}

/**
 * Detects the host OS / browser language. Returns [AppLanguage.TR] for Turkish,
 * and [AppLanguage.EN] for all other locales.
 */
expect fun detectSystemLanguage(): AppLanguage

/**
 * Central reactive language state for both `core` engines and Compose UI.
 */
object LocalizationManager {
    const val KEY_APP_LANGUAGE = "ht_app_language"

    private val _languageFlow = MutableStateFlow(AppLanguage.TR)
    val languageFlow: StateFlow<AppLanguage> = _languageFlow.asStateFlow()

    val currentLanguage: AppLanguage
        get() = _languageFlow.value

    fun init(settings: Settings): AppLanguage {
        val saved = AppLanguage.fromCode(settings.getStringOrNull(KEY_APP_LANGUAGE))
        val resolved = saved ?: detectSystemLanguage()
        _languageFlow.value = resolved
        return resolved
    }

    fun setLanguage(language: AppLanguage, settings: Settings? = null) {
        _languageFlow.value = language
        settings?.putString(KEY_APP_LANGUAGE, language.code)
    }

    fun isTurkish(lang: AppLanguage = currentLanguage): Boolean = lang == AppLanguage.TR
}

fun trEn(tr: String, en: String, lang: AppLanguage = LocalizationManager.currentLanguage): String =
    if (lang == AppLanguage.TR) tr else en
