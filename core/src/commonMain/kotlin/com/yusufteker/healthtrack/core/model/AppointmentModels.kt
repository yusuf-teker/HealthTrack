package com.yusufteker.healthtrack.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AttachmentType {
    PDF,
    IMAGE
}

@Serializable
data class AttachmentMetadata(
    val id: String,
    val appointmentId: String,
    val fileName: String,
    val fileType: AttachmentType,
    val localFilePath: String,
    val fileSizeBytes: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Serializable
data class AppointmentEntry(
    val id: String,
    val userId: Int,
    val dateTime: Long, // Epoch millis
    val doctorName: String,
    val hospitalName: String,
    val department: String? = null,
    val notes: String? = null,
    val reminderMinutes: Int = 60,
    val planoraTaskId: String? = null,
    val attachments: List<AttachmentMetadata> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
