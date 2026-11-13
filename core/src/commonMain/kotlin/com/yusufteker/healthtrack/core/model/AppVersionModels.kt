package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AppVersionConfigDto(
    val minSupportedVersion: String = "1.0.0",
    val latestVersion: String = "1.0.0",
    val forceUpdateTitleTr: String? = null,
    val forceUpdateTitleEn: String? = null,
    val forceUpdateMessageTr: String? = null,
    val forceUpdateMessageEn: String? = null,
    val storeUrlAndroid: String = "",
    val storeUrlIos: String = ""
)

sealed interface UpdateStatus {
    data object UpToDate : UpdateStatus
    data class OptionalUpdateAvailable(
        val config: AppVersionConfigDto,
        val currentVersion: String
    ) : UpdateStatus
    data class ForceUpdateRequired(
        val config: AppVersionConfigDto,
        val currentVersion: String
    ) : UpdateStatus
}

class CheckAppVersionUseCase {
    fun evaluate(currentVersion: String, config: AppVersionConfigDto): UpdateStatus {
        return when {
            isVersionLower(currentVersion, config.minSupportedVersion) -> {
                UpdateStatus.ForceUpdateRequired(config = config, currentVersion = currentVersion)
            }
            isVersionLower(currentVersion, config.latestVersion) -> {
                UpdateStatus.OptionalUpdateAvailable(config = config, currentVersion = currentVersion)
            }
            else -> UpdateStatus.UpToDate
        }
    }

    fun isVersionLower(current: String, target: String): Boolean {
        val currentParts = current.split("-").first().split(".").mapNotNull { it.toIntOrNull() }
        val targetParts = target.split("-").first().split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(currentParts.size, targetParts.size)
        for (i in 0 until maxLength) {
            val curr = currentParts.getOrElse(i) { 0 }
            val tgt = targetParts.getOrElse(i) { 0 }
            if (curr < tgt) return true
            if (curr > tgt) return false
        }
        return false
    }
}

