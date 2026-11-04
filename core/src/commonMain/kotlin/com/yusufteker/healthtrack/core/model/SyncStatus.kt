package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SyncStatus {
    PENDING,
    SYNCED
}
